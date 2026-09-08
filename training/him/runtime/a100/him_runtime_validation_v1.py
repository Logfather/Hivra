#!/usr/bin/env python3
"""Fail-closed validation for the realized HIM Python/PyTorch runtime.

This validator is deliberately separate from the host-only A100 validator.
Build mode proves the interpreter and locked PyTorch runtime without requiring
GPU visibility.  A100 mode adds CUDA device and tensor-smoke requirements.
"""

from __future__ import annotations

import argparse
import json
import platform
import sys
from pathlib import Path
from typing import Any


CONTRACT_ID = "HIM_A100_RUNTIME_VALIDATION_V1"
VERSION = 1
AUTHORIZED_PYTHON_VERSION = "3.13.14"
AUTHORIZED_TORCH_VERSION = "2.14.0+cu130"
AUTHORIZED_CUDA_VERSION = "13.0"
AUTHORIZED_RUNTIME_PYTHON = "/opt/him/runtime/bin/python"
RUNTIME_IDENTITY_PATH = Path("/opt/him/runtime/runtime-identity.json")


class RuntimeValidationError(RuntimeError):
    pass


def _fail(reason: str) -> None:
    raise RuntimeValidationError(reason)


def _check(condition: bool, reason: str) -> None:
    if not condition:
        _fail(reason)


def validate_runtime(
    *,
    torch_module: Any,
    executable: str,
    python_version: str,
    require_a100: bool,
) -> dict[str, Any]:
    """Validate an already imported runtime and return deterministic facts."""

    _check(executable == AUTHORIZED_RUNTIME_PYTHON, "AUTHORITATIVE_PYTHON_PATH_MISMATCH")
    _check(python_version == AUTHORIZED_PYTHON_VERSION, "PYTHON_VERSION_MISMATCH")

    observed_torch_version = str(torch_module.__version__)
    _check(observed_torch_version == AUTHORIZED_TORCH_VERSION, "TORCH_VERSION_MISMATCH")

    observed_cuda_version = str(torch_module.version.cuda)
    _check(observed_cuda_version == AUTHORIZED_CUDA_VERSION, "TORCH_CUDA_BUILD_MISMATCH")

    facts: dict[str, Any] = {
        "contractId": CONTRACT_ID,
        "version": VERSION,
        "mode": "A100" if require_a100 else "BUILD",
        "pythonExecutable": executable,
        "pythonVersion": python_version,
        "torchVersion": observed_torch_version,
        "torchCudaBuild": observed_cuda_version,
    }

    if require_a100:
        cuda = torch_module.cuda
        _check(bool(cuda.is_available()), "CUDA_UNAVAILABLE")
        device_count = int(cuda.device_count())
        _check(device_count == 1, "GPU_COUNT_MISMATCH")
        device_name = str(cuda.get_device_name(0))
        _check(device_name.strip().upper().startswith("NVIDIA A100"), "GPU_FAMILY_MISMATCH")
        device = torch_module.device("cuda", 0)
        value = torch_module.ones((2, 2), device=device, dtype=torch_module.float32)
        result = value.add(1.0)
        cuda.synchronize(0)
        _check(bool(torch_module.isfinite(result).all().item()), "CUDA_TENSOR_SMOKE_FAILED")
        facts.update(
            {
                "cudaAvailable": True,
                "gpuCount": device_count,
                "deviceName": device_name,
                "cudaTensorSmoke": "PASS",
            }
        )

    return facts


def _write_lineage(path: Path, mode: str, facts: dict[str, Any]) -> None:
    identity: dict[str, Any] = {}
    if RUNTIME_IDENTITY_PATH.is_file():
        identity = json.loads(RUNTIME_IDENTITY_PATH.read_text())
    lineage = {
        "contractId": CONTRACT_ID,
        "version": VERSION,
        "runtimeImageId": identity.get("runtimeImageId", "HIM_A100_REFERENCE_TRAINING_RUNTIME_IMAGE_V1"),
        "pythonVersion": AUTHORIZED_PYTHON_VERSION,
        "uvVersion": "0.12.2",
        "torchVersion": AUTHORIZED_TORCH_VERSION,
        "torchCudaBuild": AUTHORIZED_CUDA_VERSION,
        "buildContextDigest": identity.get("buildContextDigest"),
        "runtimeImageDefinitionDigest": identity.get("runtimeImageDefinitionDigest"),
        "runtimeValidatorVersion": VERSION,
        "authoritativePython": AUTHORIZED_RUNTIME_PYTHON,
        "authoritativeEnvironment": "/opt/him/runtime",
        "validationMode": mode,
        "validationResult": "PASS",
        "facts": facts,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(lineage, ensure_ascii=False, sort_keys=True, indent=2) + "\n")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=("build", "a100"), required=True)
    parser.add_argument("--lineage-output", type=Path)
    args = parser.parse_args(argv)

    try:
        import torch

        facts = validate_runtime(
            torch_module=torch,
            executable=str(Path(sys.executable)),
            python_version=platform.python_version(),
            require_a100=args.mode == "a100",
        )
        if args.lineage_output is not None:
            _write_lineage(args.lineage_output, args.mode.upper(), facts)
    except (ImportError, OSError, RuntimeError, TypeError, ValueError, KeyError) as error:
        print(f"HIM_RUNTIME_VALIDATION=FAIL:{error}", file=sys.stderr)
        return 78

    print("HIM_RUNTIME_VALIDATION=PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
