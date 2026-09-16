from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from him_trainer.final_evaluation_authority_v1 import (
    FINAL_CHECKPOINT_LOGICAL_DIGEST,
    FINAL_CHECKPOINT_MANIFEST_RELATIVE_PATH,
    FINAL_CHECKPOINT_REFERENCE,
    FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST,
    FINAL_HOLDOUT_AUTHORITY_REFERENCE,
    FINAL_MODEL_STATE_RELATIVE_PATH,
    FINAL_MODEL_STATE_SHA256,
    RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT,
    FINAL_HOLDOUT_AUTHORITY_FILENAME,
    FINAL_HOLDOUT_CUDA_COMPAT_PREFIX,
    REQUIRED_MODEL_ROOT_FILES,
    REQUIRED_RUNTIME_MODULES,
    build_final_evaluation_authority_v1,
    _load_checkpoint_manifest_for_path_binding,
    persist_final_evaluation_authority_v1,
    preflight_final_evaluation_authority_v1,
    reload_final_evaluation_authority_v1,
    validate_final_evaluation_authority_v1,
    verify_final_holdout_execution_bindings_v1,
    verify_final_holdout_execution_path_v1,
    validate_deployment_time_oci_binding_v1,
)
from him_trainer.final_evaluation_authority_v1 import FinalEvaluationAuthorityError


ROOT = Path(__file__).resolve().parents[3]
RUNTIME_AUTHORITY_ROOT = ROOT / "training/him/runtime/a100"


def _write_execution_fixture(root: Path) -> None:
    manifest = root / FINAL_CHECKPOINT_MANIFEST_RELATIVE_PATH
    model_state = root / FINAL_MODEL_STATE_RELATIVE_PATH
    manifest.parent.mkdir(parents=True, exist_ok=True)
    manifest.write_text(
        '{"checkpointReference":"' + FINAL_CHECKPOINT_REFERENCE + '",'
        '"checkpointLogicalDigest":"' + FINAL_CHECKPOINT_LOGICAL_DIGEST + '",'
        '"modelState":{"relativePath":"checkpoint/model-state.pt","sha256":"' + FINAL_MODEL_STATE_SHA256 + '"}}\n',
        encoding="utf-8",
    )
    model_state.write_bytes(b"fixture-model-state")


def _write_holdout_authority_fixture(root: Path) -> Path:
    root.mkdir(parents=True, exist_ok=True)
    path = root / FINAL_HOLDOUT_AUTHORITY_FILENAME
    path.write_text('{"reference":"' + FINAL_HOLDOUT_AUTHORITY_REFERENCE + '","logicalDigest":"' + FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST + '"}\n', encoding="utf-8")
    return path


def _write_model_root_fixture(root: Path) -> Path:
    root.mkdir(parents=True, exist_ok=True)
    for name in REQUIRED_MODEL_ROOT_FILES:
        (root / name).write_text(f"fixture {name}\n", encoding="utf-8")
    return root


def _write_runtime_authority_fixture(root: Path, authority_value: dict[str, object]) -> Path:
    for source in (
        "final-training-runtime-authority.v2.json",
        "final-training-authority-packet-v1/final-training-readiness.v2.json",
        "final-training-authority-packet-v1/final-training-input-authority.v2.json",
        "final-training-authority-packet-v1/final-training-partition.v2.json",
        "final-training-authority-packet-v1/final-training-leakage-validation.v2.json",
    ):
        destination = root / "training/him/runtime/a100" / source
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes((RUNTIME_AUTHORITY_ROOT / source).read_bytes())
    authority_path = root / "training/him/runtime/a100/final-evaluation-authority-v1/final-evaluation-authority.v1.json"
    persist_final_evaluation_authority_v1(authority_path, authority_value)
    return authority_path


def _write_deployed_runtime_fixture(root: Path, authority_value: dict[str, object]) -> Path:
    runtime_package = root / "runtime/lib/python3.13/site-packages/him_trainer"
    runtime_package.mkdir(parents=True, exist_ok=True)
    for module in REQUIRED_RUNTIME_MODULES:
        (runtime_package / module).write_text(f"# fixture for {module}\n", encoding="utf-8")
    runtime_identity = root / "runtime/runtime-identity.json"
    runtime_identity.parent.mkdir(parents=True, exist_ok=True)
    runtime_identity.write_text('{"identitySchema":"fixture"}\n', encoding="utf-8")
    return _write_runtime_authority_fixture(root, authority_value)


def _write_build_context_fixture(root: Path, authority_value: dict[str, object]) -> Path:
    runtime_package = root / "trainer/him_trainer"
    runtime_package.mkdir(parents=True, exist_ok=True)
    for module in REQUIRED_RUNTIME_MODULES:
        (runtime_package / module).write_text(f"# fixture for {module}\n", encoding="utf-8")
    (root / "runtime-identity.json").write_text('{"identitySchema":"fixture"}\n', encoding="utf-8")
    return _write_runtime_authority_fixture(root, authority_value)


class FinalEvaluationAuthorityV1Test(unittest.TestCase):
    def test_checkpoint_manifest_binding_uses_single_path_argument(self) -> None:
        with tempfile.TemporaryDirectory(prefix="him-final-checkpoint-binding-") as directory:
            manifest_path = Path(directory) / FINAL_CHECKPOINT_MANIFEST_RELATIVE_PATH
            _write_execution_fixture(Path(directory))
            manifest = _load_checkpoint_manifest_for_path_binding(manifest_path)
            self.assertEqual(FINAL_CHECKPOINT_REFERENCE, manifest["checkpointReference"])
            with self.assertRaises(FinalEvaluationAuthorityError):
                _load_checkpoint_manifest_for_path_binding(manifest_path.with_name("missing-checkpoint-manifest.json"))
            invalid_reference = dict(manifest)
            invalid_reference["checkpointReference"] = "wrong-reference"
            manifest_path.write_text(json.dumps(invalid_reference), encoding="utf-8")
            with self.assertRaises(ValueError):
                _load_checkpoint_manifest_for_path_binding(manifest_path)
            invalid_digest = dict(manifest)
            invalid_digest["checkpointLogicalDigest"] = "wrong-digest"
            manifest_path.write_text(json.dumps(invalid_digest), encoding="utf-8")
            with self.assertRaises(ValueError):
                _load_checkpoint_manifest_for_path_binding(manifest_path)
            manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
            with self.assertRaises(TypeError):
                _load_checkpoint_manifest_for_path_binding(manifest_path, manifest)  # type: ignore[call-arg]
            with self.assertRaises(TypeError):
                _load_checkpoint_manifest_for_path_binding()  # type: ignore[call-arg]

    def test_authority_binds_final_checkpoint_and_opaque_holdout_only(self) -> None:
        value = build_final_evaluation_authority_v1()
        validate_final_evaluation_authority_v1(value)
        payload = value["authorityPayload"]
        self.assertEqual(FINAL_CHECKPOINT_REFERENCE, payload["evaluatedCheckpoint"]["checkpointReference"])
        self.assertEqual(FINAL_CHECKPOINT_LOGICAL_DIGEST, payload["evaluatedCheckpoint"]["checkpointLogicalDigest"])
        self.assertEqual(FINAL_MODEL_STATE_SHA256, payload["evaluatedCheckpoint"]["modelStateSha256"])
        self.assertIsNone(payload["evaluatedCheckpoint"]["runtimeOciDigest"])
        self.assertEqual(
            RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT,
            payload["evaluatedCheckpoint"]["runtimeImageDigestBindingStage"],
        )
        self.assertEqual(FINAL_HOLDOUT_AUTHORITY_REFERENCE, payload["holdoutAuthority"]["reference"])
        self.assertEqual(FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST, payload["holdoutAuthority"]["logicalDigest"])
        self.assertFalse(payload["holdoutAuthority"]["membershipIncluded"])
        self.assertFalse(payload["holdoutAuthority"]["labelsIncluded"])
        self.assertNotIn("records", payload["holdoutAuthority"])
        self.assertNotIn("examples", payload["holdoutAuthority"])
        self.assertEqual(0, payload["holdoutAccess"]["holdoutFileOpenCount"])
        self.assertEqual(0, payload["holdoutAccess"]["holdoutContentReadCount"])
        self.assertEqual(0, payload["holdoutAccess"]["holdoutDeserializationCount"])
        self.assertEqual(0, payload["holdoutAccess"]["holdoutExposureCount"])
        self.assertFalse(payload["holdoutAccess"]["holdoutOpened"])

    def test_rejects_any_new_semantic_component(self) -> None:
        value = build_final_evaluation_authority_v1()
        value["authorityPayload"]["componentReuse"]["newSemanticComponentRequiredCount"] = 1
        with self.assertRaises(ValueError):
            validate_final_evaluation_authority_v1(value)

    def test_deployment_time_oci_binding_is_required_and_external(self) -> None:
        value = build_final_evaluation_authority_v1()
        with tempfile.TemporaryDirectory(prefix="him-final-deployment-oci-") as directory:
            authority_path = Path(directory) / "authority.json"
            persist_final_evaluation_authority_v1(authority_path, value)
            digest = "sha256:" + "a" * 64
            self.assertEqual(digest, validate_deployment_time_oci_binding_v1(authority_path, digest))
            with self.assertRaises(ValueError):
                validate_deployment_time_oci_binding_v1(authority_path, None)

    def test_persist_reload_and_model_free_preflight(self) -> None:
        value = build_final_evaluation_authority_v1()
        with tempfile.TemporaryDirectory(prefix="him-final-evaluation-authority-") as directory:
            root = Path(directory)
            path = root / "final-evaluation-authority.v1.json"
            execution_root = root / "workspace"
            _write_execution_fixture(execution_root)
            persist_final_evaluation_authority_v1(path, value)
            self.assertEqual(value, reload_final_evaluation_authority_v1(path))
            result = preflight_final_evaluation_authority_v1(ROOT, path, execution_root)
            self.assertEqual("FINAL_EVALUATION_PREFLIGHT_PASS", result["state"])
            self.assertEqual(0, result["missingInputCount"])
            self.assertEqual(0, result["modelDeserializationCount"])
            self.assertEqual(0, result["forwardCount"])
            self.assertEqual(0, result["inferenceCount"])
            self.assertEqual(0, result["trainingCount"])
            self.assertFalse(result["holdoutOpened"])
            self.assertEqual(0, result["holdoutExposureCount"])

    def test_deployed_runtime_root_passes_with_workspace_execution_root(self) -> None:
        value = build_final_evaluation_authority_v1()
        with tempfile.TemporaryDirectory(prefix="him-final-deployed-runtime-") as directory:
            root = Path(directory)
            runtime_root = root / "opt-him"
            execution_root = root / "workspace"
            _write_execution_fixture(execution_root)
            authority_path = _write_deployed_runtime_fixture(runtime_root, value)
            result = preflight_final_evaluation_authority_v1(runtime_root, authority_path, execution_root)
            self.assertEqual("FINAL_EVALUATION_PREFLIGHT_PASS", result["state"])
            self.assertEqual(0, result["unresolvedDependencyCount"])
            self.assertIn("runtime/lib/python3.13/site-packages", result["resolvedPaths"]["modulePaths"]["final_evaluation_authority_v1.py"])
            binding = verify_final_holdout_execution_bindings_v1(runtime_root, authority_path, execution_root)
            self.assertEqual("FINAL_HOLDOUT_EXECUTION_BINDINGS_PASS", binding["state"])
            self.assertEqual(0, binding["modelDeserializationCount"])
            self.assertEqual(0, binding["holdoutExposureCount"])

    def test_generated_build_context_root_passes_without_repo_source_paths(self) -> None:
        value = build_final_evaluation_authority_v1()
        with tempfile.TemporaryDirectory(prefix="him-final-build-context-") as directory:
            root = Path(directory)
            runtime_root = root / "context"
            execution_root = root / "workspace"
            _write_execution_fixture(execution_root)
            authority_path = _write_build_context_fixture(runtime_root, value)
            result = preflight_final_evaluation_authority_v1(runtime_root, authority_path, execution_root)
            self.assertEqual("FINAL_EVALUATION_PREFLIGHT_PASS", result["state"])
            self.assertEqual(0, result["unresolvedDependencyCount"])
            self.assertIn("trainer/him_trainer", result["resolvedPaths"]["modulePaths"]["final_evaluation_v2_execution_contract.py"])

    def test_final_holdout_execution_path_is_resolved_without_opening_holdout(self) -> None:
        value = build_final_evaluation_authority_v1()
        with tempfile.TemporaryDirectory(prefix="him-final-holdout-path-") as directory:
            root = Path(directory)
            runtime_root = root / "opt-him"
            execution_root = root / "workspace"
            holdout_root = root / "holdout-authority"
            model_root = root / "model-root"
            output_root = root / "final-evaluation-output"
            _write_execution_fixture(execution_root)
            _write_holdout_authority_fixture(holdout_root)
            _write_model_root_fixture(model_root)
            authority_path = _write_deployed_runtime_fixture(runtime_root, value)
            result = verify_final_holdout_execution_path_v1(
                runtime_root,
                authority_path,
                execution_root,
                holdout_authority_root=holdout_root,
                checkpoint_manifest=execution_root / FINAL_CHECKPOINT_MANIFEST_RELATIVE_PATH,
                model_root=model_root,
                tokenizer_path=model_root / "tokenizer.json",
                output_root=output_root,
            )
            self.assertEqual("FINAL_HOLDOUT_EXECUTION_PATH_RESOLVED", result["state"])
            self.assertTrue(result["finalHoldoutExecutionPathResolved"])
            self.assertTrue(result["finalHoldoutRawPredictionPersistencePathResolved"])
            self.assertTrue(result["finalHoldoutResultPersistencePathResolved"])
            self.assertTrue(result["finalHoldoutResultReloadPathResolved"])
            self.assertTrue(result["finalHoldoutReportPersistencePathResolved"])
            self.assertTrue(result["finalHoldoutReportReloadPathResolved"])
            self.assertEqual(0, result["finalPostHoldoutDeterministicIntegrationGapCount"])
            self.assertEqual(str((execution_root / FINAL_MODEL_STATE_RELATIVE_PATH).resolve()), result["modelStatePath"]["path"])
            self.assertNotIn("checkpoint/checkpoint/model-state.pt", result["modelStatePath"]["path"])
            self.assertEqual(0, result["holdoutFileOpenCount"])
            self.assertEqual(0, result["holdoutContentReadCount"])
            self.assertEqual(0, result["holdoutDeserializationCount"])
            self.assertFalse(result["holdoutOpened"])
            self.assertEqual(0, result["modelDeserializationCount"])
            self.assertEqual(0, result["forwardCount"])
            self.assertEqual(0, result["trainingCount"])
            self.assertIn("--execute-final-holdout", result["executionCommand"])
            command = result["executionCommand"]
            self.assertEqual("env", command[0])
            self.assertNotIn("HIM_CUDA_COMPAT_REQUIRED=YES", command)
            self.assertNotIn(f"HIM_CUDA_COMPAT_PREFIX={FINAL_HOLDOUT_CUDA_COMPAT_PREFIX}", command)
            self.assertNotIn("LD_LIBRARY_PATH=" + FINAL_HOLDOUT_CUDA_COMPAT_PREFIX, command)
            self.assertEqual(str(runtime_root / "runtime/bin/python"), command[1])
            self.assertIn("--evaluation-root", command)
            self.assertIn("--runtime-authority", command)

    def test_workspace_is_not_accepted_as_authority_root(self) -> None:
        value = build_final_evaluation_authority_v1()
        with tempfile.TemporaryDirectory(prefix="him-final-workspace-root-") as directory:
            root = Path(directory)
            workspace_root = root / "workspace"
            runtime_root = root / "opt-him"
            _write_execution_fixture(workspace_root)
            authority_path = _write_deployed_runtime_fixture(runtime_root, value)
            result = preflight_final_evaluation_authority_v1(workspace_root, authority_path, workspace_root)
            self.assertEqual("FINAL_EVALUATION_PREFLIGHT_INVALID_AUTHORITY_ROOT", result["state"])
            self.assertEqual("INVALID_BY_CONTRACT", result["workspaceRootAsAuthorityRoot"])
            self.assertEqual(0, result["missingInputCount"])
            self.assertEqual(0, result["unresolvedDependencyCount"])


if __name__ == "__main__":
    unittest.main()
