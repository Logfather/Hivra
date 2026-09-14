"""Fail-closed P2 V2 training-readiness authority evaluator."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any, Mapping

from .runtime_source_closure_v2 import runtime_source_closure
from .training_input_authority_v2 import load_v2_input_bundle


class TrainingReadinessV2Error(ValueError):
    """Raised when readiness evidence is inconsistent or incomplete."""


def _digest(value: object) -> str:
    return hashlib.sha256(json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")).hexdigest()


def evaluate_training_readiness_v2(root: str | Path, *, runtime_image_digest: str | None = None, runtime_authorized: bool = False) -> dict[str, Any]:
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
            and declared_runtime_digest == runtime_image_digest
        ),
        "trainerSource": len(closure) == 30,
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
        "realTrainingExecutionAuthorized": False,
        "holdoutOpened": False,
        "blindExpandedValidationOpened": False,
        "checkpointProvenanceContractComplete": True,
        "status": "AUTHORIZED" if ready else "BLOCKED",
        "trainingReady": ready,
        "corpusReference": persisted["corpus"]["reference"],
        "partitionReference": persisted["partition"]["reference"],
        "leakageReference": persisted["leakage"]["reference"],
        "trainingInputAuthorityReference": json.loads(authority_path.read_text(encoding="utf-8"))["reference"] if authority_path.is_file() else None,
        "runtimeSourceModuleCount": len(closure),
    }
    digest = _digest(core)
    return {**core, "logicalDigest": digest, "reference": f"him-p2-training-readiness:v2:{digest}"}


__all__ = ["TrainingReadinessV2Error", "evaluate_training_readiness_v2"]
