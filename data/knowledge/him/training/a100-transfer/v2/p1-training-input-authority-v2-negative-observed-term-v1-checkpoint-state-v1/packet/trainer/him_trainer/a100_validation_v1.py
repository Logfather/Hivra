"""Offline-safe A100/CUDA validation boundary for the future reference host.

The pure validator consumes typed observations and never treats a local fake
fixture as hardware success.  ``probe_cuda`` is the only runtime probe and is
invoked only by a future CUDA host execution.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any


REQUIRED_GPU_COUNT = 1
REQUIRED_GPU_MEMORY_BYTES = 80_000_000_000
REQUIRED_COMPUTE_CAPABILITY_MAJOR = 8
AUTHORIZED_CUDA_VERSION = (13, 0)
AUTHORIZED_PYTORCH_CUDA_VARIANT = "cu130"
NVIDIA_DRIVER_MINIMUM_MAJOR = 580
NVIDIA_DRIVER_MINIMUM_VERSION = (580, 65, 6)
AUTHORIZED_BASE_IMAGE_PUBLISHER = "NVIDIA"
AUTHORIZED_BASE_IMAGE_FAMILY = "NVIDIA_CUDA_DL_BASE"
AUTHORIZED_BASE_IMAGE_TAG = "25.08-cuda13.0-runtime-ubuntu24.04"
AUTHORIZED_BASE_IMAGE_REGISTRY = "nvcr.io"
AUTHORIZED_BASE_IMAGE_REPOSITORY = "nvidia/cuda-dl-base"
AUTHORIZED_FULL_BASE_IMAGE_REFERENCE = (
    f"{AUTHORIZED_BASE_IMAGE_REGISTRY}/{AUTHORIZED_BASE_IMAGE_REPOSITORY}:"
    f"{AUTHORIZED_BASE_IMAGE_TAG}"
)
BASE_IMAGE_DIGEST_REQUIRED_FOR_PROVISIONING = False
BASE_IMAGE_DIGEST_AUTHORITY = "NOT_REQUIRED"


@dataclass(frozen=True)
class A100Facts:
    cuda_available: bool
    gpu_count: int
    device_name: str
    total_memory_bytes: int
    compute_capability: tuple[int, int]
    device_index: int
    cuda_tensor_smoke_passed: bool


@dataclass(frozen=True)
class A100Validation:
    passed: bool
    reason: str | None = None

    def __post_init__(self) -> None:
        if self.passed != (self.reason is None):
            raise ValueError("validation state and reason disagree")


@dataclass(frozen=True)
class CudaRuntimeValidation:
    passed: bool
    reason: str | None = None

    def __post_init__(self) -> None:
        if self.passed != (self.reason is None):
            raise ValueError("validation state and reason disagree")


@dataclass(frozen=True)
class DriverValidation:
    passed: bool
    reason: str | None = None

    def __post_init__(self) -> None:
        if self.passed != (self.reason is None):
            raise ValueError("validation state and reason disagree")


@dataclass(frozen=True)
class BaseImageValidation:
    passed: bool
    reason: str | None = None

    def __post_init__(self) -> None:
        if self.passed != (self.reason is None):
            raise ValueError("validation state and reason disagree")


def _parse_cuda_version(value: str | None) -> tuple[int, int] | None:
    if value is None:
        return None
    parts = value.split(".")
    if len(parts) != 2 or not all(part.isdigit() for part in parts):
        return None
    return int(parts[0]), int(parts[1])


def _parse_driver_version(value: str | None) -> tuple[int, int, int] | None:
    if value is None:
        return None
    parts = value.split(".")
    if len(parts) != 3 or not all(part.isdigit() for part in parts):
        return None
    return int(parts[0]), int(parts[1]), int(parts[2])


def validate_cuda_runtime(observed: str | None) -> CudaRuntimeValidation:
    if observed is None:
        return CudaRuntimeValidation(False, "MISSING")
    parsed = _parse_cuda_version(observed)
    if parsed is None:
        return CudaRuntimeValidation(False, "MALFORMED")
    if parsed != AUTHORIZED_CUDA_VERSION:
        return CudaRuntimeValidation(False, "VERSION_MISMATCH")
    return CudaRuntimeValidation(True)


def validate_nvidia_driver(observed: str | None) -> DriverValidation:
    if observed is None:
        return DriverValidation(False, "MISSING")
    parsed = _parse_driver_version(observed)
    if parsed is None:
        return DriverValidation(False, "MALFORMED")
    if parsed < NVIDIA_DRIVER_MINIMUM_VERSION:
        return DriverValidation(False, "BELOW_MINIMUM")
    return DriverValidation(True)


def validate_base_image(
    publisher: str,
    family: str,
    tag: str,
    *,
    registry: str = AUTHORIZED_BASE_IMAGE_REGISTRY,
    repository: str = AUTHORIZED_BASE_IMAGE_REPOSITORY,
) -> BaseImageValidation:
    if not registry or not repository or not tag:
        return BaseImageValidation(False, "MISSING_REFERENCE")
    if registry != AUTHORIZED_BASE_IMAGE_REGISTRY:
        return BaseImageValidation(False, "REGISTRY_MISMATCH")
    if repository != AUTHORIZED_BASE_IMAGE_REPOSITORY:
        return BaseImageValidation(False, "REPOSITORY_MISMATCH")
    if publisher != AUTHORIZED_BASE_IMAGE_PUBLISHER:
        return BaseImageValidation(False, "PUBLISHER_MISMATCH")
    if family != AUTHORIZED_BASE_IMAGE_FAMILY:
        return BaseImageValidation(False, "FAMILY_MISMATCH")
    if tag != AUTHORIZED_BASE_IMAGE_TAG:
        return BaseImageValidation(False, "TAG_MISMATCH")
    return BaseImageValidation(True)


def validate_base_image_reference(reference: str | None) -> BaseImageValidation:
    if reference is None or not reference:
        return BaseImageValidation(False, "MISSING_REFERENCE")
    if reference != reference.strip():
        return BaseImageValidation(False, "MALFORMED_REFERENCE")
    if reference.count(":") != 1:
        return BaseImageValidation(False, "MALFORMED_REFERENCE")
    registry_and_repository, tag = reference.split(":", 1)
    if "/" not in registry_and_repository:
        return BaseImageValidation(False, "MALFORMED_REFERENCE")
    registry, repository = registry_and_repository.split("/", 1)
    if not registry or not repository or not tag or any(char.isspace() for char in reference):
        return BaseImageValidation(False, "MALFORMED_REFERENCE")
    return validate_base_image(
        AUTHORIZED_BASE_IMAGE_PUBLISHER,
        AUTHORIZED_BASE_IMAGE_FAMILY,
        tag,
        registry=registry,
        repository=repository,
    )


def validate_a100_facts(facts: A100Facts) -> A100Validation:
    if not facts.cuda_available:
        return A100Validation(False, "CUDA_UNAVAILABLE")
    if facts.gpu_count != REQUIRED_GPU_COUNT:
        return A100Validation(False, "GPU_COUNT_MISMATCH")
    if facts.device_index != 0:
        return A100Validation(False, "DEVICE_INDEX_INVALID")
    if not facts.device_name.strip().upper().startswith("NVIDIA A100"):
        return A100Validation(False, "GPU_FAMILY_MISMATCH")
    if facts.total_memory_bytes < REQUIRED_GPU_MEMORY_BYTES:
        return A100Validation(False, "GPU_MEMORY_INSUFFICIENT")
    if facts.compute_capability[0] < REQUIRED_COMPUTE_CAPABILITY_MAJOR:
        return A100Validation(False, "COMPUTE_CAPABILITY_UNSUPPORTED")
    if not facts.cuda_tensor_smoke_passed:
        return A100Validation(False, "CUDA_TENSOR_SMOKE_FAILED")
    return A100Validation(True)


def probe_cuda(torch_module: Any) -> A100Facts:
    """Probe the active PyTorch CUDA device without changing training state."""

    cuda = torch_module.cuda
    available = bool(cuda.is_available())
    count = int(cuda.device_count()) if available else 0
    if not available or count == 0:
        return A100Facts(False, count, "", 0, (0, 0), 0, False)

    index = 0
    try:
        properties = cuda.get_device_properties(index)
        device = torch_module.device("cuda", index)
        value = torch_module.ones((2, 2), device=device, dtype=torch_module.float32)
        result = value.add(1.0)
        cuda.synchronize(index)
        tensor_smoke_passed = bool(torch_module.isfinite(result).all().item())
    except (RuntimeError, TypeError, ValueError):
        tensor_smoke_passed = False

    return A100Facts(
        cuda_available=True,
        gpu_count=count,
        device_name=str(properties.name),
        total_memory_bytes=int(properties.total_memory),
        compute_capability=(int(properties.major), int(properties.minor)),
        device_index=index,
        cuda_tensor_smoke_passed=tensor_smoke_passed,
    )
