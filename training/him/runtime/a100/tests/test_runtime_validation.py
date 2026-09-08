from __future__ import annotations

import importlib.util
import subprocess
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
VALIDATOR = ROOT / "him_runtime_validation_v1.py"
ROOTFS_BUILDER = ROOT / "build-runtime-rootfs.sh"


def load_validator():
    spec = importlib.util.spec_from_file_location("him_runtime_validation_v1", VALIDATOR)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FakeCuda:
    def __init__(self, available: bool = True, count: int = 1, name: str = "NVIDIA A100-SXM4-80GB") -> None:
        self.available = available
        self.count = count
        self.name = name

    def is_available(self) -> bool:
        return self.available

    def device_count(self) -> int:
        return self.count

    def get_device_name(self, index: int) -> str:
        return self.name

    def synchronize(self, index: int) -> None:
        return None


class FakeFinite:
    def all(self) -> "FakeFinite":
        return self

    def item(self) -> bool:
        return True


class FakeTensor:
    def add(self, value: float) -> "FakeTensor":
        return self


class FakeTorch:
    __version__ = "2.14.0+cu130"
    version = type("Version", (), {"cuda": "13.0"})()
    cuda = FakeCuda()
    float32 = object()

    @staticmethod
    def device(kind: str, index: int) -> tuple[str, int]:
        return kind, index

    @staticmethod
    def ones(shape: tuple[int, int], device: object, dtype: object) -> FakeTensor:
        return FakeTensor()

    @staticmethod
    def isfinite(value: FakeTensor) -> FakeFinite:
        return FakeFinite()


class RuntimeValidationTest(unittest.TestCase):
    def test_build_mode_validates_exact_runtime_without_gpu(self) -> None:
        validator = load_validator()
        facts = validator.validate_runtime(
            torch_module=FakeTorch,
            executable="/opt/him/runtime/bin/python",
            python_version="3.13.14",
            require_a100=False,
        )
        self.assertEqual(facts["torchVersion"], "2.14.0+cu130")
        self.assertEqual(facts["torchCudaBuild"], "13.0")

    def test_a100_mode_requires_single_a100_and_cuda_smoke(self) -> None:
        validator = load_validator()
        facts = validator.validate_runtime(
            torch_module=FakeTorch,
            executable="/opt/him/runtime/bin/python",
            python_version="3.13.14",
            require_a100=True,
        )
        self.assertEqual(facts["gpuCount"], 1)
        self.assertEqual(facts["cudaTensorSmoke"], "PASS")

    def test_wrong_interpreter_fails_closed(self) -> None:
        validator = load_validator()
        with self.assertRaises(validator.RuntimeValidationError):
            validator.validate_runtime(
                torch_module=FakeTorch,
                executable="/root/.local/bin/python3.13",
                python_version="3.13.14",
                require_a100=False,
            )

    def test_rootfs_builder_is_bounded_and_fail_closed(self) -> None:
        result = subprocess.run(["bash", "-n", str(ROOTFS_BUILDER)], check=False)
        self.assertEqual(result.returncode, 0)
        text = ROOTFS_BUILDER.read_text()
        self.assertIn("chroot", text)
        self.assertIn("apt-get install --yes --no-install-recommends ca-certificates curl openssh-server", text)
        self.assertIn("uv sync --frozen --no-dev", text)
        self.assertIn("/opt/him/runtime/bin/python", text)
        self.assertIn("build-lineage.json", text)
        self.assertNotIn("rm -rf /;", text)

    def test_validator_metadata_is_deterministic_and_non_secret(self) -> None:
        validator = load_validator()
        self.assertEqual(validator.CONTRACT_ID, "HIM_A100_RUNTIME_VALIDATION_V1")
        self.assertEqual(validator.AUTHORIZED_TORCH_VERSION, "2.14.0+cu130")
        self.assertNotIn("RUNPOD_API_KEY", VALIDATOR.read_text())
        self.assertNotIn("OPENAI_API_KEY", VALIDATOR.read_text())


if __name__ == "__main__":
    unittest.main()
