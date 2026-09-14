"""Explicit source closure for the P2 V2 training runtime."""

from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Any


REQUIRED_RUNTIME_MODULES = (
    "__init__.py", "__main__.py", "a100_validation_v1.py", "batch_size_authority_v2.py",
    "checkpoint_v2.py", "corpus_assembly_v2.py", "corpus_coverage_v2.py", "evidence_projection_v2.py",
    "execution_device_v1.py", "input_representation_v2.py", "partition_leakage_v2.py", "partition_v2.py",
    "point12_protocol_v1.py", "point12_token_tensor_builder_v1.py", "point13_forward_rng_contract_v1.py",
    "point13_loss_contract_v1.py", "point13_loss_v1.py", "point13_model_forward_v1.py",
    "point13_optimizer_construction_v1.py", "point13_optimizer_execution_policy_v1.py",
    "point13_trainability_policy_v1.py", "point13_trainability_projection_v1.py", "post_review_eligibility_v2.py",
    "productive_training_p2_v2.py", "protocol_v1.py", "runtime_source_closure_v2.py", "sequence_length_authority_v2.py",
    "training_input_authority_v2.py", "training_readiness_authority_v2.py", "training_runtime_authority_v2.py",
)


class RuntimeSourceClosureV2Error(ValueError):
    """Raised when the declared runtime closure is incomplete or drifting."""


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def runtime_source_closure(root: str | Path) -> tuple[dict[str, Any], ...]:
    source_root = Path(root) / "training/him/src/him_trainer"
    entries = []
    for module in REQUIRED_RUNTIME_MODULES:
        path = source_root / module
        if not path.is_file() or path.is_symlink():
            raise RuntimeSourceClosureV2Error(f"REQUIRED_RUNTIME_MODULE_MISSING:{module}")
        entries.append({"module": module, "sourcePath": f"training/him/src/him_trainer/{module}", "bytes": path.stat().st_size, "sha256": _sha256(path), "destination": f"trainer/him_trainer/{module}"})
    return tuple(entries)


__all__ = ["REQUIRED_RUNTIME_MODULES", "RuntimeSourceClosureV2Error", "runtime_source_closure"]
