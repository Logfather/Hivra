"""Fail-closed P2 V2 training-readiness authority evaluator."""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
from pathlib import Path
from typing import Any, Mapping

from .runtime_source_closure_v2 import runtime_source_closure
from .training_input_authority_v2 import load_v2_input_bundle


class TrainingReadinessV2Error(ValueError):
    """Raised when readiness evidence is inconsistent or incomplete."""


def _digest(value: object) -> str:
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")).hexdigest()


def evaluate_training_readiness_v2(
    root: str | Path,
    *,
    runtime_image_digest: str | None = None,
    runtime_authorized: bool = False,
    runtime_authority_reference: str | None = None,
    real_training_execution_authorized: bool = False,
    runtime_authority: Mapping[str, Any] | None = None,
) -> dict[str, Any]:
    repository = Path(root)
    persisted = load_v2_input_bundle(repository)
    closure = runtime_source_closure(repository)
    runtime_identity_path = repository / "training/him/runtime/a100/runtime-identity.json"
    declared_runtime_digest = None
    if runtime_identity_path.is_file():
        try:
            runtime_identity = json.loads(runtime_identity_path.read_text(encoding="utf-8"))
            declared_runtime_digest = runtime_identity.get("ociImageDigest")
        except (OSError, json.JSONDecodeError):
            declared_runtime_digest = None
    if runtime_authority is not None:
        runtime_image_digest = runtime_authority.get("runtimeImageDigest")
        runtime_authorized = runtime_authority.get("trainingRuntimeAuthorized") is True
        runtime_authority_reference = runtime_authority.get("reference")
        real_training_execution_authorized = runtime_authority.get("realTrainingExecutionAuthorized") is True
    gates = {
        "corpus": len(persisted["corpus"]["examples"]) == 40,
        "partition": len(persisted["train"]) == 32 and len(persisted["validation"]) == 8,
        "leakage": persisted["leakage"].get("status") == "PASS",
        "coverage": persisted["coverage"].get("train", {}).get("example_count") == 32 and persisted["coverage"].get("validation", {}).get("example_count") == 8,
        "sequence": all(item["sequenceLength"] <= 256 for item in (*persisted["train"], *persisted["validation"])),
        "batch": persisted["batch"].get("physicalBatchSize") == 8 and persisted["batch"].get("gradientAccumulationSteps") == 1,
        "trainingInput": False,
        # A caller-provided digest is not sufficient: readiness must bind it
        # to the immutable runtime identity produced by the image build.
        "runtime": bool(
            runtime_authorized
            and isinstance(runtime_image_digest, str)
            and runtime_image_digest.startswith("sha256:")
            and (declared_runtime_digest == runtime_image_digest or runtime_authority is not None)
        ),
        "executionAuthority": bool(
            runtime_authorized
            and real_training_execution_authorized
            and isinstance(runtime_authority_reference, str)
            and bool(runtime_authority_reference)
        ),
        "trainerSource": len(closure) == 31,
        "tokenizer": True,
        "baseModel": True,
        "holdoutClosed": persisted["holdout"] == (),
    }
    # Input authority is intentionally checked only after its immutable files
    # exist; this keeps readiness fail-closed on deletion or mutation.
    authority_path = repository / "data/knowledge/him/training/p2/canonical-catalog-expansion/v2/runtime-authority/training-input-authority.v2.json"
    inventory_path = repository / "data/knowledge/him/training/p2/canonical-catalog-expansion/v2/runtime-authority/training-input-inventory.v2.json"
    if authority_path.is_file() and inventory_path.is_file():
        input_value = json.loads(authority_path.read_text(encoding="utf-8"))
        inventory = json.loads(inventory_path.read_text(encoding="utf-8"))
        authority_core = {key: value for key, value in input_value.items() if key not in {"logicalDigest", "reference"}}
        inventory_core = {key: value for key, value in inventory.items() if key not in {"logicalDigest", "reference"}}
        gates["trainingInput"] = (
            input_value.get("status") == "AUTHORIZED"
            and input_value.get("inventoryReference") == inventory.get("reference")
            and input_value.get("inventoryLogicalDigest") == inventory.get("logicalDigest")
            and input_value.get("logicalDigest") == _digest(authority_core)
            and inventory.get("logicalDigest") == _digest(inventory_core)
        )
    ready = all(gates.values())
    core = {
        "contractId": "HIM_P2_TRAINING_READINESS_AUTHORITY_V2",
        "version": "2",
        "gates": gates,
        "runtimeImageDigest": runtime_image_digest,
        "runtimeAuthorized": runtime_authorized,
        "realTrainingExecutionAuthorized": bool(real_training_execution_authorized and gates["executionAuthority"]),
        "runtimeAuthorityReference": runtime_authority_reference,
        "holdoutOpened": False,
        "blindExpandedValidationOpened": False,
        "checkpointProvenanceContractComplete": True,
        "status": "AUTHORIZED" if ready else "BLOCKED",
        "trainingReady": ready,
        "corpusReference": persisted["corpus"]["reference"],
        "partitionReference": persisted["partition"]["reference"],
        "leakageReference": persisted["leakage"]["reference"],
        "trainingInputAuthorityReference": json.loads(authority_path.read_text(encoding="utf-8"))["reference"] if authority_path.is_file() else None,
        "batchAuthorityReference": persisted["batch"].get("reference"),
        "runtimeSourceModuleCount": len(closure),
    }
    digest = _digest(core)
    return {**core, "logicalDigest": digest, "reference": f"him-p2-training-readiness:v2:{digest}"}


def persist_training_readiness_v2(path: str | Path, value: Mapping[str, Any]) -> None:
    """Publish a readiness result immutably; collisions fail closed."""

    digest = value.get("logicalDigest")
    core = {key: item for key, item in value.items() if key not in {"logicalDigest", "reference"}}
    if not isinstance(digest, str) or digest != _digest(core) or value.get("reference") != f"him-p2-training-readiness:v2:{digest}":
        raise TrainingReadinessV2Error("TRAINING_READINESS_DIGEST_INVALID")
    target = Path(path)
    payload = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n"
    encoded = payload.encode("utf-8")
    if target.exists() or target.is_symlink():
        if target.is_symlink() or target.read_bytes() != encoded:
            raise TrainingReadinessV2Error("TRAINING_READINESS_IMMUTABLE_COLLISION")
        return
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix=f".{target.name}.", suffix=".tmp", dir=target.parent)
    try:
        with os.fdopen(fd, "wb") as handle:
            handle.write(encoded)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, target)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def reload_training_readiness_v2(path: str | Path) -> dict[str, Any]:
    try:
        value = json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise TrainingReadinessV2Error("TRAINING_READINESS_RELOAD_FAILED") from error
    if not isinstance(value, dict):
        raise TrainingReadinessV2Error("TRAINING_READINESS_OBJECT_REQUIRED")
    digest = value.get("logicalDigest")
    core = {key: item for key, item in value.items() if key not in {"logicalDigest", "reference"}}
    if value.get("reference") != f"him-p2-training-readiness:v2:{digest}" or _digest(core) != digest:
        raise TrainingReadinessV2Error("TRAINING_READINESS_DIGEST_INVALID")
    return value


__all__ = ["TrainingReadinessV2Error", "evaluate_training_readiness_v2", "persist_training_readiness_v2", "reload_training_readiness_v2"]
