"""Explicit, model-free execution contract for a future Final Evaluation V2.

This module contains paths and bindings only.  It deliberately contains no
Holdout records, labels, model weights, or consumed-Holdout content.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any, Mapping


SCHEMA = "HIM_FINAL_EVALUATION_V2_EXECUTION_CONTRACT"
VERSION = 1
DIGEST_SCHEME = "sha256(canonical UTF-8 JSON; ensure_ascii=false, sort_keys=true, compact separators)"
ENTRYPOINT = "him_trainer.final_evaluation_authority_v1"
FIXTURE_NAMESPACE = "HIM_FINAL_EVALUATION_V2_FIXTURE_ONLY"


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def logical_digest(value: Any) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def build_execution_contract() -> dict[str, Any]:
    inputs = [
        {"id": "runtime_identity", "type": "json", "path": "runtime/runtime-identity.json", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "deployment runtime identity validator", "consumedBy": ENTRYPOINT},
        {"id": "deployment_oci_digest", "type": "environment", "binding": "HIM_RUNTIME_IMAGE_DIGEST", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "validate_deployment_time_oci_binding_v1", "consumedBy": ENTRYPOINT},
        {"id": "evaluation_authority", "type": "json", "path": "training/him/runtime/a100/final-evaluation-authority-v1/final-evaluation-authority.v1.json", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "reload_final_evaluation_authority_v1", "consumedBy": ENTRYPOINT},
        {"id": "holdout_authority", "type": "opaque-authority", "path": "<holdout-authority-root>/p2-family-isolated-holdout-authority.v1.json", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "final holdout authority binding", "consumedBy": ENTRYPOINT},
        {"id": "sealed_review_packet", "type": "json", "path": "<evaluation-root>/review-packet.v2.json", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "load_sealed_v2_set", "consumedBy": "blind_expanded_validation_v2.execute_real_v2"},
        {"id": "sealed_ground_truth", "type": "json", "path": "<evaluation-root>/ground-truth.v2.json", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "load_sealed_v2_set", "consumedBy": "blind_expanded_validation_v2.execute_real_v2"},
        {"id": "sealed_evaluation_authority", "type": "json", "path": "<evaluation-root>/sealed-evaluation-authority.v2.json", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "load_sealed_v2_set", "consumedBy": "blind_expanded_validation_v2.execute_real_v2"},
        {"id": "sealed_final_validation_report", "type": "json", "path": "<evaluation-root>/final-validation-report.v2.json", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "load_sealed_v2_set", "consumedBy": "blind_expanded_validation_v2.execute_real_v2"},
        {"id": "checkpoint_manifest", "type": "json", "path": "<execution-root>/him-final/final-training-v1/.../checkpoint/checkpoint-manifest.json", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "validate_checkpoint_manifest", "consumedBy": ENTRYPOINT},
        {"id": "model_state", "type": "binary", "path": "<execution-root>/him-final/final-training-v1/.../checkpoint/model-state.pt", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "checkpoint model-state SHA-256 validator", "consumedBy": ENTRYPOINT},
        {"id": "input_representation", "type": "sealed-field", "path": "<evaluation-root>/ground-truth.v2.json:modelInput.serialized", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "reconstruct_v2_inputs", "consumedBy": "_build_real_input_tensors"},
        {"id": "model_config", "type": "json", "path": "<model-root>/config.json", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "pinned model config loader", "consumedBy": ENTRYPOINT},
        {"id": "model_weights", "type": "safetensors", "path": "<model-root>/model.safetensors", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "pinned model loader", "consumedBy": ENTRYPOINT},
        {"id": "tokenizer", "type": "json", "path": "<tokenizer-path>", "requiredBeforeExecution": True, "requiredBeforeExposure": True, "validatedBy": "pinned tokenizer loader", "consumedBy": ENTRYPOINT},
        {"id": "raw_prediction_output", "type": "json-output", "path": "<output-root>/raw-predictions.v1.json", "requiredBeforeExecution": False, "requiredBeforeExposure": False, "validatedBy": "persist_raw_predictions/reload_raw_predictions", "consumedBy": "scoring"},
        {"id": "result_output", "type": "json-output", "path": "<output-root>/evaluation-result.v1.json", "requiredBeforeExecution": False, "requiredBeforeExposure": False, "validatedBy": "persist_result/reload_result", "consumedBy": "report"},
        {"id": "report_output", "type": "json-output", "path": "<output-root>/execution-report.v1.json", "requiredBeforeExecution": False, "requiredBeforeExposure": False, "validatedBy": "final report persistence/reload", "consumedBy": "final execution caller"},
    ]
    outputs = [
        {"id": "raw_prediction_output", "path": "<output-root>/raw-predictions.v1.json", "state": "FROZEN_BEFORE_SCORING"},
        {"id": "result_output", "path": "<output-root>/evaluation-result.v1.json", "state": "DIGEST_VERIFIED_AFTER_RELOAD"},
        {"id": "report_output", "path": "<output-root>/execution-report.v1.json", "state": "DIGEST_VERIFIED_AFTER_RELOAD"},
    ]
    payload = {
        "schema": SCHEMA,
        "version": VERSION,
        "fixtureNamespace": FIXTURE_NAMESPACE,
        "productiveEntrypoint": ENTRYPOINT,
        "inputs": inputs,
        "outputs": outputs,
        "environmentBindings": [{"name": "HIM_RUNTIME_IMAGE_DIGEST", "required": True, "immutable": True, "stage": "DEPLOYMENT_TIME_IMMUTABLE_OCI"}],
        "preExposure": {"holdoutOpened": False, "allInputsResolved": True, "reviewPacketRequired": True, "failClosed": True},
        "counters": {"modelDeserialization": 0, "forward": 0, "training": 0, "holdoutExposure": 0},
        "digestScheme": DIGEST_SCHEME,
    }
    digest = logical_digest(payload)
    return {"reference": f"him-final-evaluation-v2-execution-contract:v1:{digest}", "logicalDigest": digest, "payload": payload}


def build_future_holdout_requirement_set() -> dict[str, Any]:
    contract = build_execution_contract()
    payload = {"schema": "HIM_FINAL_EVALUATION_V2_FUTURE_REAL_HOLDOUT_REQUIREMENTS", "version": 1, "executionContractReference": contract["reference"], "executionContractLogicalDigest": contract["logicalDigest"], "requirements": contract["payload"]["inputs"], "sealedRootFiles": ["review-packet.v2.json", "ground-truth.v2.json", "sealed-evaluation-authority.v2.json", "final-validation-report.v2.json"], "noContentInRequirementSet": True}
    digest = logical_digest(payload)
    return {"reference": f"him-final-evaluation-v2-future-holdout-requirements:v1:{digest}", "logicalDigest": digest, "payload": payload}


def validate_fixture_preseal(root: str | Path) -> dict[str, Any]:
    directory = Path(root)
    required = build_execution_contract()["payload"]["inputs"][4:8]
    missing = [item["path"] for item in required if not (directory / item["path"].replace("<evaluation-root>/", "")).is_file()]
    return {"requiredInputCount": len(required), "missingInputCount": len(missing), "missingInputs": missing, "state": "PASS" if not missing else "FAIL", "holdoutOpened": False, "exposureCount": 0}

