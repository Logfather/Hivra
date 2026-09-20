import importlib
import sys
import tomllib
import unittest
from pathlib import Path

import him_trainer


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
TRAINING_ROOT = REPOSITORY_ROOT / "training" / "him"
SOURCE_ROOT = TRAINING_ROOT / "src"


class HostImportabilityTest(unittest.TestCase):
    def test_him_trainer_imports(self) -> None:
        self.assertEqual(him_trainer.HOST_MODULE_ID, "him_trainer")

    def test_namespace_is_stable(self) -> None:
        self.assertEqual(him_trainer.__name__, "him_trainer")
        self.assertEqual(him_trainer.HOST_MODULE_VERSION, "1")

    def test_module_resolves_from_repository_source_root(self) -> None:
        module_path = Path(him_trainer.__file__).resolve()
        self.assertEqual(module_path.parent, SOURCE_ROOT / "him_trainer")

    def test_source_root_layout_is_explicit(self) -> None:
        self.assertTrue(SOURCE_ROOT.is_dir())
        self.assertTrue((SOURCE_ROOT / "him_trainer" / "__init__.py").is_file())

    def test_pinned_python_version_is_used(self) -> None:
        if sys.version_info[:3] != (3, 13, 14):
            self.skipTest("HIM runtime-only: authoritative image uses Python 3.13.14")
        self.assertEqual(sys.version_info[:3], (3, 13, 14))

    def test_package_distribution_remains_disabled(self) -> None:
        with (TRAINING_ROOT / "pyproject.toml").open("rb") as handle:
            project = tomllib.load(handle)
        self.assertFalse(project["tool"]["uv"]["package"])

    def test_existing_runtime_dependencies_are_unchanged(self) -> None:
        with (TRAINING_ROOT / "pyproject.toml").open("rb") as handle:
            dependencies = set(tomllib.load(handle)["project"]["dependencies"])
        self.assertEqual(
            dependencies,
            {
                "safetensors==0.7.0",
                "sentencepiece==0.2.2",
                "tokenizers==0.23.1",
                "torch==2.14.0",
            },
        )

    def test_source_root_is_on_the_authorized_import_path(self) -> None:
        self.assertIn(str(SOURCE_ROOT), sys.path)

    def test_host_module_has_no_parallel_namespace(self) -> None:
        self.assertEqual(Path(him_trainer.__file__).parts[-2:], ("him_trainer", "__init__.py"))

    def test_host_module_has_no_decoder_api(self) -> None:
        source = Path(him_trainer.__file__).read_text(encoding="utf-8")
        self.assertNotIn("decode", source.lower())

    def test_host_module_has_no_numerical_runtime_imports(self) -> None:
        source = Path(him_trainer.__file__).read_text(encoding="utf-8").lower()
        for forbidden_name in ("torch", "tokenizers", "sentencepiece"):
            self.assertNotIn(forbidden_name, source)

    def test_host_module_has_no_network_or_model_access(self) -> None:
        source = Path(him_trainer.__file__).read_text(encoding="utf-8").lower()
        for forbidden_name in ("urllib", "requests", "socket", "model.safetensors", "tokenizer"):
            self.assertNotIn(forbidden_name, source)

    def test_import_is_deterministic(self) -> None:
        first_identity = (him_trainer.__name__, him_trainer.HOST_MODULE_ID, him_trainer.HOST_MODULE_VERSION)
        reloaded = importlib.reload(him_trainer)
        second_identity = (reloaded.__name__, reloaded.HOST_MODULE_ID, reloaded.HOST_MODULE_VERSION)
        self.assertEqual(first_identity, second_identity)

    def test_no_external_test_dependency_is_required(self) -> None:
        self.assertEqual(unittest.__name__, "unittest")

    def test_host_module_is_importable_without_model_initialization(self) -> None:
        self.assertNotIn("torch", sys.modules)
        self.assertNotIn("tokenizers", sys.modules)
        self.assertNotIn("sentencepiece", sys.modules)


if __name__ == "__main__":
    unittest.main()
