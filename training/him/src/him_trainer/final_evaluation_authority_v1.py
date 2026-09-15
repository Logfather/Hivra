"""Pre-Holdout authority for the Final HIM evaluation.

This module deliberately does not load, parse, or validate Holdout membership.
It closes only the execution contract that must exist before the first Holdout
open: checkpoint binding, opaque Holdout authority identity, reusable evaluator
components, frozen input/metric semantics, prediction/result persistence, and a
model-free clean-environment preflight.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
from typing import Any, Mapping, Sequence


CONTRACT_ID = "HIM_FINAL_EVALUATION_AUTHORITY_V1"
VERSION = 1
DIGEST_SCHEME = "sha256(canonical UTF-8 JSON; ensure_ascii=false, sort_keys=true, compact separators)"

FINAL_CHECKPOINT_REFERENCE = "him-training-checkpoint:v2:beba3ba4ba75ba1d117f3ed13482e8389b2f428406dd92d4cc41d1b476140197"
FINAL_CHECKPOINT_LOGICAL_DIGEST = "923d0a479847b21e57ff4b1c5649c1a38f07bb3a431b8e06ea9bec50bcd54632"
FINAL_MODEL_STATE_SHA256 = "6fd984f5375ef780ab81187e053bf468158ddb849932553ea4062a745796443e"
FINAL_RUNTIME_OCI = "sha256:1ba9baae282dcd83c84d21ac0fef2d9583da45c0657cc2cb0aac5b88ed8a6f44"
SOURCE_HEAD = "545510e7547fd3b723ad17efca9a093390d381d6"

FINAL_HOLDOUT_AUTHORITY_REFERENCE = "p2-family-isolated-holdout-authority:v1:dc19557950bb3737ea51fc94d04616f2f2c96233868be294773bd120509b7c5a"
FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST = "dc19557950bb3737ea51fc94d04616f2f2c96233868be294773bd120509b7c5a"

FINAL_RUNTIME_AUTHORITY_REFERENCE = "him-final-training-runtime-authority:v1:b7e4640ddf2ae719a418c8e435fd2c20a3a18a51dc46396be4dee244ab8fb24f"
FINAL_READINESS_REFERENCE = "him-final-training-readiness:v1:f3f13bb47bd4382e073e05f43eab1efec6dca8619ffd06ac8784bf65389bd5b6"
FINAL_INPUT_AUTHORITY_REFERENCE = "him-final-training-input-authority:v1:9ceb2bb424978cdaf4ac5230eeba9c3fb3b64f319ec951d8f1e95f22fe045654"
FINAL_CORPUS_REFERENCE = "him-final-training-corpus:v1:4e3621d8ff82e92f57af2262e819beae98dd438166e1b06f7bf7d511131b7b75"
FINAL_PARTITION_REFERENCE = "him-final-training-partition:v1:b8f48b6b253c633965ed1bb7afebd6325b25d62f4f125b3055150b881b45e27f"
SEQUENCE_LENGTH_REFERENCE = "sequence-length-authority:v2:97130457decd4f283492da2d09a0faddb4bb99fc70f7d79732fbd390794cc509"
BASE_MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
TOKENIZER_REFERENCE = "xlm-roberta-base-tokenizer@a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"

AUTHORITY_FILENAME = "final-evaluation-authority.v1.json"


class FinalEvaluationAuthorityError(ValueError):
    """Raised when the Final Evaluation authority is invalid."""


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def logical_digest(value: Any) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise FinalEvaluationAuthorityError(message)


def _read_json(path: Path) -> dict[str, Any]:
    _require(path.is_file() and not path.is_symlink(), f"FILE_INVALID:{path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise FinalEvaluationAuthorityError(f"JSON_INVALID:{path}") from error
    _require(isinstance(value, dict), f"OBJECT_REQUIRED:{path}")
    return value


def existing_evaluation_components() -> tuple[str, ...]:
    return (
        "Blind Expanded V2 evaluator",
        "checkpoint loader",
        "Training Input V2 reconstruction",
        "prediction persistence",
        "confusion-matrix logic",
        "metric calculation",
        "result persistence",
        "result reload/digest verification",
    )


def required_clean_environment_inputs(root: str | Path) -> tuple[Path, ...]:
    base = Path(root)
    return (
        base / "training/him/src/him_trainer/final_evaluation_authority_v1.py",
        base / "training/him/src/him_trainer/blind_expanded_validation_v2.py",
        base / "training/him/src/him_trainer/corpus_assembly_v2.py",
        base / "training/him/src/him_trainer/point12_token_tensor_builder_v1.py",
        base / "training/him/src/him_trainer/point13_model_forward_v1.py",
        base / "training/him/runtime/a100/final-training-runtime-authority.v2.json",
        base / "training/him/runtime/a100/final-training-authority-packet-v1/final-training-readiness.v2.json",
        base / "training/him/runtime/a100/final-training-authority-packet-v1/final-training-input-authority.v2.json",
        base / "training/him/runtime/a100/final-training-authority-packet-v1/final-training-partition.v2.json",
        base / "training/him/runtime/a100/final-training-authority-packet-v1/final-training-leakage-validation.v2.json",
    )


def build_final_evaluation_authority_v1() -> dict[str, Any]:
    components = existing_evaluation_components()
    payload: dict[str, Any] = {
        "contractId": CONTRACT_ID,
        "version": VERSION,
        "state": "PRE_HOLDOUT_AUTHORITY_CLOSED",
        "holdoutAccess": {
            "holdoutFileOpenCount": 0,
            "holdoutContentReadCount": 0,
            "holdoutDeserializationCount": 0,
            "holdoutExposureCount": 0,
            "holdoutOpened": False,
            "contentAccessPolicy": "FORBIDDEN_IN_PRE_HOLDOUT_AUTHORITY_CLOSURE",
        },
        "componentReuse": {
            "existingEvaluationComponentCount": len(components),
            "reusableFinalEvaluationComponentCount": len(components),
            "newSemanticComponentRequiredCount": 0,
            "components": list(components),
            "semanticPolicy": "REUSE_EXISTING_BLIND_EXPANDED_V2_TARGET_KIND_AND_CANDIDATE_COMPATIBILITY_SEMANTICS",
        },
        "evaluatedCheckpoint": {
            "checkpointReference": FINAL_CHECKPOINT_REFERENCE,
            "checkpointLogicalDigest": FINAL_CHECKPOINT_LOGICAL_DIGEST,
            "modelStateSha256": FINAL_MODEL_STATE_SHA256,
            "sourceHead": SOURCE_HEAD,
            "runtimeOciDigest": FINAL_RUNTIME_OCI,
        },
        "holdoutAuthority": {
            "reference": FINAL_HOLDOUT_AUTHORITY_REFERENCE,
            "logicalDigest": FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST,
            "resolved": True,
            "opaqueMetadataOnly": True,
            "membershipIncluded": False,
            "labelsIncluded": False,
        },
        "inputContract": {
            "frozen": True,
            "inputRepresentation": "V2",
            "trainingInputAuthorityReference": FINAL_INPUT_AUTHORITY_REFERENCE,
            "sequenceLengthAuthorityReference": SEQUENCE_LENGTH_REFERENCE,
            "tokenizerReference": TOKENIZER_REFERENCE,
            "baseModelRevision": BASE_MODEL_REVISION,
            "candidateConditioning": "FROZEN_FROM_FINAL_TRAINING_INPUT_V2",
            "serialization": "FROZEN_V2",
            "padding": "FROZEN_BY_EXISTING_TOKEN_TENSOR_CONTRACT",
            "truncationPolicy": "TRUNCATION_FORBIDDEN",
            "tensorConstruction": "REUSE_EXISTING_POINT12_POINT13_PATH",
        },
        "outputs": {
            "primary": "TARGET_KIND",
            "secondary": "CANDIDATE_COMPATIBILITY",
            "newOutputHeadIntroduced": False,
        },
        "metricSet": {
            "frozen": True,
            "primaryMetrics": [
                "PRIMARY_ACTIVE_COUNT",
                "PRIMARY_CORRECT_COUNT",
                "PRIMARY_ACCURACY",
                "PRIMARY_CONFUSION_MATRIX",
            ],
            "secondaryMetrics": [
                "SECONDARY_ACTIVE_COUNT",
                "SECONDARY_CORRECT_COUNT",
                "SECONDARY_ACCURACY",
                "COMPATIBLE_REJECT_CONFUSION_MATRIX",
            ],
            "additionalExistingMetrics": [
                "PER_FAMILY_RESULTS",
                "PER_UNIT_ERRORS",
                "NEGATIVE_BOUNDARY_RESULTS",
            ],
            "metricMutationAfterHoldoutOpen": "FORBIDDEN",
        },
        "rawPredictionPolicy": {
            "rawPredictionsPersisted": True,
            "rawPredictionsFrozenBeforeScoring": True,
            "predictionMutationAfterPersist": "FORBIDDEN",
            "bindings": [
                "checkpoint",
                "model-state SHA256",
                "runtime OCI",
                "source HEAD",
                "Holdout authority",
                "evaluation authority",
                "input authority",
            ],
        },
        "artifactPolicy": {
            "rawPredictionArtifact": "Final Holdout raw prediction artifact",
            "scoredResultArtifact": "Final Holdout scored result artifact",
            "executionReportArtifact": "Final evaluation execution report",
            "persistence": "IMMUTABLE_WRITE_ONCE",
            "reloadDigestVerification": True,
        },
        "qualityDecision": {
            "thresholdAuthorityExists": False,
            "mode": "DESCRIPTIVE_ONLY_NO_PREEXISTING_THRESHOLD",
            "thresholdMutationAfterHoldoutOpen": "FORBIDDEN",
        },
        "authorityBindings": {
            "runtimeAuthorityReference": FINAL_RUNTIME_AUTHORITY_REFERENCE,
            "finalReadinessReference": FINAL_READINESS_REFERENCE,
            "trainingInputAuthorityReference": FINAL_INPUT_AUTHORITY_REFERENCE,
            "finalCorpusReference": FINAL_CORPUS_REFERENCE,
            "finalPartitionReference": FINAL_PARTITION_REFERENCE,
        },
        "executionEntrypoint": {
            "module": "him_trainer.final_evaluation_authority_v1",
            "futureEvaluationUses": "him_trainer.blind_expanded_validation_v2 primitives with final checkpoint binding",
            "preflightImportRequired": True,
            "modelDeserializationCount": 0,
            "forwardCount": 0,
            "inferenceCount": 0,
            "trainingCount": 0,
        },
    }
    digest = logical_digest(payload)
    return {
        "authorityReference": f"him-final-evaluation-authority:v1:{digest}",
        "logicalDigest": digest,
        "digestScheme": DIGEST_SCHEME,
        "authorityPayload": payload,
    }


def validate_final_evaluation_authority_v1(value: Mapping[str, Any]) -> dict[str, Any]:
    payload = value.get("authorityPayload")
    _require(isinstance(payload, Mapping), "AUTHORITY_PAYLOAD_INVALID")
    actual = logical_digest(payload)
    _require(value.get("logicalDigest") == actual, "AUTHORITY_DIGEST_MISMATCH")
    _require(value.get("authorityReference") == f"him-final-evaluation-authority:v1:{actual}", "AUTHORITY_REFERENCE_MISMATCH")
    _require(payload.get("contractId") == CONTRACT_ID, "CONTRACT_ID_INVALID")
    _require(payload.get("state") == "PRE_HOLDOUT_AUTHORITY_CLOSED", "STATE_INVALID")
    holdout = payload.get("holdoutAccess")
    _require(isinstance(holdout, Mapping), "HOLDOUT_ACCESS_INVALID")
    for field in ("holdoutFileOpenCount", "holdoutContentReadCount", "holdoutDeserializationCount", "holdoutExposureCount"):
        _require(holdout.get(field) == 0, f"HOLDOUT_COUNTER_NONZERO:{field}")
    _require(holdout.get("holdoutOpened") is False, "HOLDOUT_OPENED_INVALID")
    reuse = payload.get("componentReuse")
    _require(isinstance(reuse, Mapping), "COMPONENT_REUSE_INVALID")
    _require(reuse.get("newSemanticComponentRequiredCount") == 0, "NEW_SEMANTIC_COMPONENT_REQUIRED")
    evaluated = payload.get("evaluatedCheckpoint")
    _require(isinstance(evaluated, Mapping), "CHECKPOINT_BINDING_INVALID")
    _require(evaluated.get("checkpointReference") == FINAL_CHECKPOINT_REFERENCE, "CHECKPOINT_REFERENCE_INVALID")
    _require(evaluated.get("checkpointLogicalDigest") == FINAL_CHECKPOINT_LOGICAL_DIGEST, "CHECKPOINT_DIGEST_INVALID")
    _require(evaluated.get("modelStateSha256") == FINAL_MODEL_STATE_SHA256, "MODEL_STATE_DIGEST_INVALID")
    opaque = payload.get("holdoutAuthority")
    _require(isinstance(opaque, Mapping), "HOLDOUT_AUTHORITY_INVALID")
    _require(opaque.get("reference") == FINAL_HOLDOUT_AUTHORITY_REFERENCE, "HOLDOUT_REFERENCE_INVALID")
    _require(opaque.get("logicalDigest") == FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST, "HOLDOUT_DIGEST_INVALID")
    _require(opaque.get("membershipIncluded") is False and opaque.get("labelsIncluded") is False, "HOLDOUT_CONTENT_INCLUDED")
    input_contract = payload.get("inputContract")
    _require(isinstance(input_contract, Mapping) and input_contract.get("frozen") is True, "INPUT_CONTRACT_NOT_FROZEN")
    _require(input_contract.get("inputRepresentation") == "V2", "INPUT_REPRESENTATION_INVALID")
    outputs = payload.get("outputs")
    _require(isinstance(outputs, Mapping), "OUTPUTS_INVALID")
    _require(outputs.get("primary") == "TARGET_KIND", "PRIMARY_OUTPUT_INVALID")
    _require(outputs.get("secondary") == "CANDIDATE_COMPATIBILITY", "SECONDARY_OUTPUT_INVALID")
    metric_set = payload.get("metricSet")
    _require(isinstance(metric_set, Mapping) and metric_set.get("frozen") is True, "METRIC_SET_NOT_FROZEN")
    quality = payload.get("qualityDecision")
    _require(isinstance(quality, Mapping), "QUALITY_DECISION_INVALID")
    _require(quality.get("mode") == "DESCRIPTIVE_ONLY_NO_PREEXISTING_THRESHOLD", "QUALITY_MODE_INVALID")
    return dict(value)


def persist_final_evaluation_authority_v1(path: str | Path, value: Mapping[str, Any]) -> None:
    validate_final_evaluation_authority_v1(value)
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(_canonical(value) + b"\n")


def reload_final_evaluation_authority_v1(path: str | Path) -> dict[str, Any]:
    return validate_final_evaluation_authority_v1(_read_json(Path(path)))


def preflight_final_evaluation_authority_v1(root: str | Path, authority_path: str | Path) -> dict[str, Any]:
    authority = reload_final_evaluation_authority_v1(authority_path)
    required = required_clean_environment_inputs(root)
    missing = [str(path) for path in required if not path.is_file()]
    return {
        "state": "FINAL_EVALUATION_PREFLIGHT_PASS" if not missing else "FINAL_EVALUATION_PREFLIGHT_BLOCKED",
        "authorityReference": authority["authorityReference"],
        "authorityLogicalDigest": authority["logicalDigest"],
        "requiredInputCount": len(required),
        "missingInputCount": len(missing),
        "missingInputs": missing,
        "holdoutOpened": False,
        "holdoutExposureCount": 0,
        "modelDeserializationCount": 0,
        "forwardCount": 0,
        "inferenceCount": 0,
        "trainingCount": 0,
    }


def run_cli(arguments: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="him_trainer.final_evaluation_authority_v1")
    parser.add_argument("--root", default=".")
    parser.add_argument("--authority", required=True)
    parser.add_argument("--write-authority", action="store_true")
    parser.add_argument("--preflight", action="store_true")
    args = parser.parse_args(list(arguments) if arguments is not None else None)
    try:
        if args.write_authority:
            persist_final_evaluation_authority_v1(args.authority, build_final_evaluation_authority_v1())
        if args.preflight:
            print(json.dumps(preflight_final_evaluation_authority_v1(args.root, args.authority), sort_keys=True))
        else:
            value = reload_final_evaluation_authority_v1(args.authority)
            print(json.dumps({"state": "FINAL_EVALUATION_AUTHORITY_RELOAD_PASS", "authorityReference": value["authorityReference"], "logicalDigest": value["logicalDigest"]}, sort_keys=True))
        return 0
    except (FinalEvaluationAuthorityError, OSError, UnicodeError, ValueError) as error:
        print(f"{type(error).__name__}: {error}", file=os.sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(run_cli())
