"""Explicit execution-device authority for the HIM numerical runtime."""

from __future__ import annotations

from typing import Any


EXECUTION_DEVICE_CPU_V1 = "CPU"
EXECUTION_DEVICE_CUDA_V1 = "CUDA"
CUDA_DEVICE_INDEX_V1 = 0


class HimExecutionDeviceError(ValueError):
    """Raised when an execution device cannot be realized safely."""


def resolve_him_execution_device_v1(
    device: str,
    device_index: int,
    *,
    torch_module: Any,
) -> Any:
    """Resolve an already transported device authority without fallback."""

    if device == EXECUTION_DEVICE_CPU_V1:
        if device_index != CUDA_DEVICE_INDEX_V1:
            raise HimExecutionDeviceError("CPU_DEVICE_INDEX_INVALID")
        return torch_module.device("cpu")
    if device != EXECUTION_DEVICE_CUDA_V1:
        raise HimExecutionDeviceError("UNKNOWN_EXECUTION_DEVICE")
    if device_index != CUDA_DEVICE_INDEX_V1:
        raise HimExecutionDeviceError("CUDA_DEVICE_INDEX_INVALID")
    if not bool(torch_module.cuda.is_available()):
        raise HimExecutionDeviceError("CUDA_UNAVAILABLE")
    if int(torch_module.cuda.device_count()) <= device_index:
        raise HimExecutionDeviceError("CUDA_DEVICE_INDEX_UNAVAILABLE")
    return torch_module.device("cuda", device_index)


def validate_him_tensor_device_v1(value: Any, selected_device: Any, name: str) -> None:
    """Require an exact device match; never move or copy a tensor."""

    if getattr(value, "device", None) != selected_device:
        raise HimExecutionDeviceError(f"{name}_DEVICE_MISMATCH")
