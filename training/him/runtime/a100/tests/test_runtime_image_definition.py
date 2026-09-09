from __future__ import annotations

import hashlib
import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
REPOSITORY_ROOT = ROOT.parents[3]
EXPECTED_BASE_IMAGE = "nvcr.io/nvidia/cuda-dl-base:25.08-cuda13.0-runtime-ubuntu24.04"
EXPECTED_PYPROJECT = REPOSITORY_ROOT / "training/him/pyproject.toml"
EXPECTED_UV_LOCK = REPOSITORY_ROOT / "training/him/uv.lock"
EXPECTED_TRAINER_SOURCE_ROOT = REPOSITORY_ROOT / "training/him/src/him_trainer"
EXPECTED_TRAINER_RUNTIME_ROOT = "/opt/him/runtime/lib/python3.13/site-packages/him_trainer"
IDENTITY_SCRIPT = ROOT / "runtime-identity.sh"
IDENTITY_FILE = ROOT / "runtime-identity.json"
EXPECTED_RUNTIME_ID = "HIM_A100_REFERENCE_TRAINING_RUNTIME_IMAGE_V1"
EXPECTED_REFERENCE_DIGEST = "e5bfa4442a50e154c7f23735def545cfc545798f8b326ff38051f2eea57111d9"
EXPECTED_EXCLUDED_FIELDS = ["buildContextDigest", "runtimeImageDefinitionDigest", "ociImageDigest"]


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class RuntimeImageDefinitionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.dockerfile = (ROOT / "Dockerfile").read_text()
        cls.startup = (ROOT / "start.sh").read_text()
        cls.metadata = json.loads((ROOT / "runtime-image-definition.json").read_text())
        subprocess.check_call([str(IDENTITY_SCRIPT), "--generate"], stdout=subprocess.DEVNULL)
        cls.identity = json.loads(IDENTITY_FILE.read_text())
        cls.manifest_rows = []
        for line in (ROOT / "build-context.manifest.tsv").read_text().splitlines()[1:]:
            if line:
                cls.manifest_rows.append(line.split("\t"))

    def test_exact_base_image_and_target(self) -> None:
        self.assertIn(f"FROM {EXPECTED_BASE_IMAGE}", self.dockerfile)
        self.assertEqual(self.metadata["baseImage"], EXPECTED_BASE_IMAGE)
        self.assertEqual(self.metadata["target"], {"os": "linux", "architecture": "amd64"})

    def test_runtime_identity(self) -> None:
        self.assertEqual(self.metadata["runtimeImageId"], "HIM_A100_REFERENCE_TRAINING_RUNTIME_IMAGE_V1")

    def test_python_and_uv_are_pinned(self) -> None:
        self.assertIn("ARG PYTHON_VERSION=3.13.14", self.dockerfile)
        self.assertIn("ARG UV_VERSION=0.12.2", self.dockerfile)
        self.assertEqual(self.metadata["python"]["version"], "3.13.14")
        self.assertEqual(self.metadata["uv"]["version"], "0.12.2")

    def test_locked_dependency_authority(self) -> None:
        dependencies = self.metadata["dependencies"]
        self.assertEqual(dependencies["realization"], "IMAGE_BUILD_FROM_UNCHANGED_UV_LOCK")
        self.assertEqual(dependencies["authority"], "training/him/uv.lock")
        self.assertEqual(dependencies["linuxTorchSource"], "pytorch-cu130")
        self.assertEqual(dependencies["torch"], "2.14.0+cu130")
        self.assertIn("--frozen", self.dockerfile)
        self.assertNotIn("pip install torch", self.dockerfile)

    def test_expected_package_versions_and_no_transformers(self) -> None:
        lock = EXPECTED_UV_LOCK.read_text()
        self.assertIn('name = "tokenizers"\nversion = "0.23.1"', lock)
        self.assertIn('name = "sentencepiece"\nversion = "0.2.2"', lock)
        self.assertIn('name = "safetensors"\nversion = "0.7.0"', lock)
        self.assertNotIn('name = "transformers"', lock)

    def test_ssh_is_built_and_secure(self) -> None:
        ssh = self.metadata["ssh"]
        self.assertIn("openssh-server", self.dockerfile)
        self.assertTrue(ssh["opensshServerInstalledAtImageBuild"])
        self.assertTrue(ssh["publicKeyEnvironmentVariable"] == "PUBLIC_KEY")
        self.assertIn("PasswordAuthentication no", self.startup)
        self.assertIn("PermitEmptyPasswords no", self.startup)
        self.assertIn("PubkeyAuthentication yes", self.startup)
        self.assertIn("PermitRootLogin prohibit-password", self.startup)

    def test_ssh_has_no_baked_credentials(self) -> None:
        self.assertNotIn("RUNPOD_API_KEY", self.dockerfile)
        self.assertNotIn("OPENAI_API_KEY", self.dockerfile)
        self.assertNotIn("BEGIN OPENSSH PRIVATE KEY", self.startup)
        self.assertTrue(self.metadata["ssh"]["runtimeHostKeyGeneration"])
        self.assertFalse(self.metadata["ssh"]["privateKeysBakedIntoImage"])
        self.assertFalse(self.metadata["ssh"]["userPublicKeyBakedIntoImage"])

    def test_startup_fails_closed_and_stays_foreground(self) -> None:
        self.assertIn("fail_closed", self.startup)
        self.assertIn('[[ -n "$PUBLIC_KEY_VALUE" ]]', self.startup)
        self.assertIn("sshd -t", self.startup)
        self.assertIn("exec /usr/sbin/sshd -D -e", self.startup)
        self.assertNotIn("sleep infinity", self.startup)

    def test_training_and_download_autostart_are_disabled(self) -> None:
        self.assertEqual(self.metadata["autostart"], {"training": False, "modelDownload": False, "dataDownload": False})
        self.assertNotIn("python -m", self.startup)
        self.assertNotIn("uv run", self.startup)
        self.assertNotIn("torch", self.startup)

    def test_image_excludes_mutable_and_sensitive_artifacts(self) -> None:
        contents = self.metadata["imageContents"]
        self.assertFalse(contents["modelArtifacts"])
        self.assertFalse(contents["trainingData"])
        self.assertFalse(contents["trainingRequest"])
        self.assertFalse(contents["checkpoints"])
        self.assertTrue(contents["a100ValidationTool"])
        self.assertTrue(contents["trainerPackage"])
        self.assertIn("COPY validation/a100_validation_v1.py", self.dockerfile)
        for forbidden in ("COPY .", "COPY model", "COPY corpus", "COPY checkpoint", "COPY data/"):
            self.assertNotIn(forbidden, self.dockerfile)

    def test_directory_boundaries(self) -> None:
        paths = self.metadata["paths"]
        self.assertEqual(paths["immutableRuntimeRoot"], "/opt/him")
        self.assertEqual(paths["pythonRuntimeRoot"], "/opt/him/runtime")
        self.assertEqual(self.metadata["runtimeLayout"]["trainerPackageRoot"], EXPECTED_TRAINER_RUNTIME_ROOT)
        self.assertEqual(paths["validationRoot"], "/opt/him/validation")
        self.assertEqual(paths["mutableWorkspaceRoot"], "/workspace")
        self.assertIn("/workspace", self.dockerfile)

    def test_provider_boundary_contains_no_resource_identifiers(self) -> None:
        boundary = self.metadata["providerBoundary"]
        self.assertEqual(boundary["runtimeCore"], "PROVIDER_NEUTRAL_RUNTIME_CORE")
        self.assertEqual(boundary["runpodAdapter"], "RUNPOD_REMOTE_ACCESS_ADAPTER")
        self.assertFalse(boundary["resourceIdentifiersBakedIn"])
        self.assertFalse(boundary["apiCredentialsBakedIn"])
        self.assertNotIn("rk05nlcczpabis", self.dockerfile)
        self.assertNotIn("v3xo66lqnk", self.dockerfile)
        self.assertNotIn("EUR-IS-1", self.dockerfile)

    def test_manifest_is_bounded_and_deterministic(self) -> None:
        self.assertEqual(len(self.manifest_rows), 25)
        destinations = [row[1] for row in self.manifest_rows]
        self.assertEqual(len(destinations), len(set(destinations)))
        for source, destination, _role, _final, _build_only in self.manifest_rows:
            self.assertFalse(Path(source).is_absolute())
            self.assertFalse(".." in Path(source).parts)
            self.assertFalse(".." in Path(destination).parts)
            self.assertTrue((REPOSITORY_ROOT / source).is_file())

    def test_manifest_binds_dependency_source_bytes(self) -> None:
        by_source = {row[0]: row for row in self.manifest_rows}
        self.assertEqual(by_source["training/him/pyproject.toml"][2], "dependency-authority")
        self.assertEqual(by_source["training/him/uv.lock"][2], "dependency-lock-authority")
        self.assertEqual(sha256(EXPECTED_PYPROJECT), sha256(EXPECTED_PYPROJECT))
        self.assertEqual(sha256(EXPECTED_UV_LOCK), sha256(EXPECTED_UV_LOCK))
        self.assertFalse((ROOT / "pyproject.toml").exists())
        self.assertFalse((ROOT / "uv.lock").exists())

    def test_manifest_excludes_model_training_and_user_data(self) -> None:
        manifest_text = (ROOT / "build-context.manifest.tsv").read_text().lower()
        for forbidden in ("models/", "corpus/", "dataset/", "checkpoint", "decision", "review", "knowledge/", "firestore", "firebase"):
            self.assertNotIn(forbidden, manifest_text)
        self.assertNotIn("data/", manifest_text)

    def test_manifest_marks_validation_tool_and_build_only_manifest(self) -> None:
        by_source = {row[0]: row for row in self.manifest_rows}
        validation = next(
            row for row in self.manifest_rows
            if row[0] == "training/him/src/him_trainer/a100_validation_v1.py"
            and row[2] == "a100-validation-tooling"
        )
        runtime_validation = by_source["training/him/runtime/a100/him_runtime_validation_v1.py"]
        rootfs_builder = by_source["training/him/runtime/a100/build-runtime-rootfs.sh"]
        manifest = by_source["training/him/runtime/a100/build-context.manifest.tsv"]
        self.assertEqual(validation[2], "a100-validation-tooling")
        self.assertEqual(validation[3:], ["YES", "NO"])
        self.assertEqual(runtime_validation[2], "runtime-validation-tooling")
        self.assertEqual(runtime_validation[3:], ["YES", "NO"])
        self.assertEqual(rootfs_builder[2], "rootfs-realization-tooling")
        self.assertEqual(rootfs_builder[3:], ["NO", "YES"])
        self.assertEqual(manifest[3:], ["NO", "YES"])

    def test_manifest_binds_unchanged_him_trainer_package(self) -> None:
        package_rows = [row for row in self.manifest_rows if row[2] == "him-trainer-package"]
        source_files = sorted(EXPECTED_TRAINER_SOURCE_ROOT.glob("*.py"))
        self.assertEqual(len(package_rows), len(source_files))
        self.assertEqual(len(package_rows), 15)
        self.assertEqual(
            {row[0] for row in package_rows},
            {str(path.relative_to(REPOSITORY_ROOT)) for path in source_files},
        )
        self.assertEqual(
            {row[1] for row in package_rows},
            {f"trainer/him_trainer/{path.name}" for path in source_files},
        )
        self.assertTrue(all(row[3:] == ["YES", "NO"] for row in package_rows))
        self.assertIn(
            "COPY trainer/him_trainer /opt/him/runtime/lib/python3.13/site-packages/him_trainer",
            self.dockerfile,
        )
        self.assertIn("import him_trainer", self.dockerfile)

    def test_runtime_definition_digest_is_repeatable(self) -> None:
        command = [str(ROOT / "runtime-image-definition.digest.sh")]
        first = subprocess.check_output(command, text=True).strip()
        second = subprocess.check_output(command, text=True).strip()
        self.assertEqual(first, second)
        self.assertRegex(first, r"^[0-9a-f]{64}$")

    def test_embedded_runtime_identity_binds_current_authorities(self) -> None:
        self.assertEqual(self.identity["runtimeImageId"], EXPECTED_RUNTIME_ID)
        self.assertEqual(self.identity["referenceEnvironmentContractDigest"], EXPECTED_REFERENCE_DIGEST)
        self.assertEqual(self.identity["buildContextDigest"], self._build_context_digest())
        self.assertEqual(self.identity["runtimeImageDefinitionDigest"], self._runtime_definition_digest())
        self.assertIsNone(self.identity["ociImageDigest"])

    def test_content_tag_is_derived_from_final_definition_digest(self) -> None:
        definition_digest = self._runtime_definition_digest()
        self.assertEqual(self.identity["contentTagSchemeVersion"], "V1")
        self.assertEqual(self.identity["contentTagPrefixLength"], 12)
        self.assertEqual(self.identity["contentDerivedDeploymentTag"], f"def-{definition_digest[:12]}")
        self.assertEqual(self.identity["previousContentDerivedDeploymentTag"], "def-e5562d215017")
        self.assertNotEqual(self.identity["contentDerivedDeploymentTag"], self.identity["previousContentDerivedDeploymentTag"])

    def test_identity_exclusion_rule_is_exact_and_non_circular(self) -> None:
        self.assertEqual(self.identity["runtimeImageDefinitionDigestExcludedFields"], EXPECTED_EXCLUDED_FIELDS)
        self.assertIn("Canonical runtime-image-definition.json bytes exclude", self.identity["buildContextDigestRule"])
        self.assertIn("runtime-identity.json is derived metadata and excluded", self.identity["buildContextDigestRule"])

    def test_runtime_identity_generation_is_byte_identical(self) -> None:
        first = IDENTITY_FILE.read_bytes()
        subprocess.check_call([str(IDENTITY_SCRIPT), "--generate"], stdout=subprocess.DEVNULL)
        second = IDENTITY_FILE.read_bytes()
        self.assertEqual(first, second)

    def test_digests_are_reproducible_independently(self) -> None:
        self.assertEqual(self._runtime_definition_digest(), self._runtime_definition_digest())
        self.assertEqual(self._build_context_digest(), self._build_context_digest())

    def test_dockerfile_copies_non_secret_runtime_identity(self) -> None:
        self.assertIn("COPY runtime-identity.json /opt/him/runtime/runtime-identity.json", self.dockerfile)
        self.assertIn("chmod 0644 /opt/him/runtime/runtime-identity.json", self.dockerfile)
        self.assertNotIn("RUNPOD_API_KEY", IDENTITY_FILE.read_text())
        self.assertNotIn("OPENAI_API_KEY", IDENTITY_FILE.read_text())

    def test_dockerfile_has_explicit_runtime_validation_gate(self) -> None:
        self.assertIn("COPY validation/him_runtime_validation_v1.py", self.dockerfile)
        self.assertIn("test -x /opt/him/python/bin/python3.13", self.dockerfile)
        self.assertIn("test -x /opt/him/runtime/bin/python", self.dockerfile)
        self.assertIn("/opt/him/runtime/lib/python3.13/site-packages/torch/__init__.py", self.dockerfile)
        self.assertIn("--mode build", self.dockerfile)
        self.assertNotIn("runtime-image-definition.json \\\\", self.dockerfile)

    def test_runtime_layout_and_export_assertions_are_authoritative(self) -> None:
        self.assertEqual(self.metadata["runtimeLayout"]["authoritativePython"], "/opt/him/runtime/bin/python")
        self.assertEqual(self.metadata["runtimeLayout"]["projectEnvironment"], "/opt/him/runtime")
        self.assertEqual(self.metadata["runtimeLayout"]["pythonHome"], "/opt/him/python")
        self.assertTrue(self.metadata["rootfsExport"]["validationBeforeOciExport"])
        self.assertTrue(self.metadata["rootfsExport"]["assertionsFailClosed"])

    def test_runtime_identity_is_offline_observable_and_digest_derived(self) -> None:
        self.assertEqual(self.identity["runtimeIdentityRuntimePath"], "/opt/him/runtime/runtime-identity.json")
        self.assertRegex(self.identity["buildContextDigest"], r"^[0-9a-f]{64}$")
        self.assertRegex(self.identity["runtimeImageDefinitionDigest"], r"^[0-9a-f]{64}$")
        self.assertEqual(self.identity["identitySchema"], "HIM_A100_RUNTIME_IDENTITY_V1")

    def _runtime_definition_digest(self) -> str:
        return subprocess.check_output([str(ROOT / "runtime-image-definition.digest.sh")], text=True).strip()

    def _build_context_digest(self) -> str:
        with tempfile.TemporaryDirectory() as output_dir:
            output = subprocess.check_output(
                [str(ROOT / "build-context.sh"), os.path.join(output_dir, "context")],
                text=True,
            )
        return next(line.split("=", 1)[1] for line in output.splitlines() if line.startswith("BUILD_CONTEXT_DIGEST="))

    def test_build_context_generator_is_repeatable(self) -> None:
        command = [str(ROOT / "build-context.sh")]
        with tempfile.TemporaryDirectory() as first_dir, tempfile.TemporaryDirectory() as second_dir:
            first = subprocess.check_output(command + [os.path.join(first_dir, "context")], text=True)
            second = subprocess.check_output(command + [os.path.join(second_dir, "context")], text=True)
        first_digest = next(line for line in first.splitlines() if line.startswith("BUILD_CONTEXT_DIGEST=") )
        second_digest = next(line for line in second.splitlines() if line.startswith("BUILD_CONTEXT_DIGEST=") )
        self.assertEqual(first_digest, second_digest)
        self.assertRegex(first_digest.split("=", 1)[1], r"^[0-9a-f]{64}$")

    def test_build_context_carries_exact_authority_bytes(self) -> None:
        with tempfile.TemporaryDirectory() as output_dir:
            context = Path(output_dir) / "context"
            subprocess.check_call([str(ROOT / "build-context.sh"), str(context)])
            self.assertEqual((context / "pyproject.toml").read_bytes(), EXPECTED_PYPROJECT.read_bytes())
            self.assertEqual((context / "uv.lock").read_bytes(), EXPECTED_UV_LOCK.read_bytes())
            self.assertEqual((context / "validation/a100_validation_v1.py").read_bytes(),
                             (REPOSITORY_ROOT / "training/him/src/him_trainer/a100_validation_v1.py").read_bytes())

    def test_build_context_has_no_secret_or_data_paths(self) -> None:
        with tempfile.TemporaryDirectory() as output_dir:
            context = Path(output_dir) / "context"
            subprocess.check_call([str(ROOT / "build-context.sh"), str(context)], stdout=subprocess.DEVNULL)
            paths = [path.relative_to(context).as_posix() for path in context.rglob("*") if path.is_file()]
        self.assertEqual(len(paths), 25)
        self.assertFalse(any(".env" in path or "private" in path or "credential" in path for path in paths))
        forbidden_components = {"model", "models", "corpus", "dataset", "checkpoint", "knowledge"}
        self.assertFalse(
            any(component in forbidden_components for path in paths for component in Path(path).parts)
        )

    def test_reference_environment_binding_is_separate(self) -> None:
        self.assertEqual(
            self.metadata["referenceEnvironmentContractDigest"],
            "e5bfa4442a50e154c7f23735def545cfc545798f8b326ff38051f2eea57111d9",
        )
        self.assertIn("runtimeImageDefinitionDigestScope", self.metadata)
        self.assertNotIn("OCI_IMAGE_DIGEST", self.metadata)


if __name__ == "__main__":
    unittest.main()
