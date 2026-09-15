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
FINAL_EVALUATION_AUTHORITY_RELATIVE_PATH = Path(
    "training/him/runtime/a100/final-evaluation-authority-v1/final-evaluation-authority.v1.json"
)
FINAL_TRAINING_RUNTIME_AUTHORITY_RELATIVE_PATH = Path(
    "training/him/runtime/a100/final-training-runtime-authority.v2.json"
)
FINAL_TRAINING_READINESS_RELATIVE_PATH = Path(
    "training/him/runtime/a100/final-training-authority-packet-v1/final-training-readiness.v2.json"
)
FINAL_TRAINING_INPUT_AUTHORITY_RELATIVE_PATH = Path(
    "training/him/runtime/a100/final-training-authority-packet-v1/final-training-input-authority.v2.json"
)
FINAL_TRAINING_PARTITION_RELATIVE_PATH = Path(
    "training/him/runtime/a100/final-training-authority-packet-v1/final-training-partition.v2.json"
)
FINAL_TRAINING_LEAKAGE_RELATIVE_PATH = Path(
    "training/him/runtime/a100/final-training-authority-packet-v1/final-training-leakage-validation.v2.json"
)
REPOSITORY_TRAINER_ROOT = Path("training/him/src/him_trainer")
BUILD_CONTEXT_TRAINER_ROOT = Path("trainer/him_trainer")
DEPLOYED_TRAINER_ROOT = Path("runtime/lib/python3.13/site-packages/him_trainer")
REPOSITORY_RUNTIME_IDENTITY_RELATIVE_PATH = Path("training/him/runtime/a100/runtime-identity.json")
BUILD_CONTEXT_RUNTIME_IDENTITY_RELATIVE_PATH = Path("runtime-identity.json")
DEPLOYED_RUNTIME_IDENTITY_RELATIVE_PATH = Path("runtime/runtime-identity.json")
FINAL_CHECKPOINT_RELATIVE_ROOT = Path(
    "him-final/final-training-v1/20260915T162921Z-545510e7-final/checkpoint"
)
FINAL_MODEL_STATE_RELATIVE_PATH = FINAL_CHECKPOINT_RELATIVE_ROOT / "model-state.pt"
FINAL_CHECKPOINT_MANIFEST_RELATIVE_PATH = FINAL_CHECKPOINT_RELATIVE_ROOT / "checkpoint-manifest.json"
FINAL_EVALUATION_OUTPUT_RELATIVE_ROOT = Path("him-final/final-evaluation/v1")
FINAL_RAW_PREDICTION_OUTPUT_RELATIVE_PATH = FINAL_EVALUATION_OUTPUT_RELATIVE_ROOT / "raw-predictions.v1.json"
FINAL_SCORED_RESULT_OUTPUT_RELATIVE_PATH = FINAL_EVALUATION_OUTPUT_RELATIVE_ROOT / "evaluation-result.v1.json"
FINAL_EXECUTION_REPORT_OUTPUT_RELATIVE_PATH = FINAL_EVALUATION_OUTPUT_RELATIVE_ROOT / "execution-report.v1.json"
REQUIRED_RUNTIME_MODULES = (
    "final_evaluation_authority_v1.py",
    "blind_expanded_validation_v2.py",
    "corpus_assembly_v2.py",
    "point12_token_tensor_builder_v1.py",
    "point13_model_forward_v1.py",
)


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
        *(base / REPOSITORY_TRAINER_ROOT / module for module in REQUIRED_RUNTIME_MODULES),
        base / FINAL_TRAINING_RUNTIME_AUTHORITY_RELATIVE_PATH,
        base / FINAL_TRAINING_READINESS_RELATIVE_PATH,
        base / FINAL_TRAINING_INPUT_AUTHORITY_RELATIVE_PATH,
        base / FINAL_TRAINING_PARTITION_RELATIVE_PATH,
        base / FINAL_TRAINING_LEAKAGE_RELATIVE_PATH,
    )


def _runtime_module_candidates(root: Path, module: str) -> tuple[Path, ...]:
    return (
        root / DEPLOYED_TRAINER_ROOT / module,
        root / BUILD_CONTEXT_TRAINER_ROOT / module,
        root / REPOSITORY_TRAINER_ROOT / module,
    )


def _runtime_identity_candidates(root: Path) -> tuple[Path, ...]:
    return (
        root / DEPLOYED_RUNTIME_IDENTITY_RELATIVE_PATH,
        root / BUILD_CONTEXT_RUNTIME_IDENTITY_RELATIVE_PATH,
        root / REPOSITORY_RUNTIME_IDENTITY_RELATIVE_PATH,
    )


def _first_existing_file(candidates: Sequence[Path]) -> Path | None:
    for candidate in candidates:
        if candidate.is_file() and not candidate.is_symlink():
            return candidate
    return None


def _is_workspace_execution_root(root: Path) -> bool:
    resolved = root.resolve()
    return resolved == Path("/workspace") or resolved.name == "workspace"


def _is_runtime_authority_root(root: Path) -> bool:
    if _first_existing_file(_runtime_identity_candidates(root)) is not None:
        return True
    if all(_first_existing_file(_runtime_module_candidates(root, module)) is not None for module in REQUIRED_RUNTIME_MODULES):
        return True
    if (root / FINAL_TRAINING_RUNTIME_AUTHORITY_RELATIVE_PATH).is_file():
        return True
    return False


def default_execution_root_for_runtime_root(root: str | Path) -> Path:
    base = Path(root)
    if (base / DEPLOYED_TRAINER_ROOT).is_dir() or (base / DEPLOYED_RUNTIME_IDENTITY_RELATIVE_PATH).is_file():
        return Path("/workspace")
    return base


def resolve_final_evaluation_runtime_paths(
    runtime_root: str | Path,
    execution_root: str | Path | None = None,
) -> dict[str, Any]:
    runtime_base = Path(runtime_root)
    execution_base = Path(execution_root) if execution_root is not None else default_execution_root_for_runtime_root(runtime_base)
    module_paths: dict[str, str | None] = {}
    unresolved: list[dict[str, Any]] = []

    for module in REQUIRED_RUNTIME_MODULES:
        candidates = _runtime_module_candidates(runtime_base, module)
        resolved = _first_existing_file(candidates)
        module_paths[module] = str(resolved) if resolved else None
        if resolved is None:
            unresolved.append(
                {
                    "role": f"runtime-module:{module}",
                    "classification": "A_ALREADY_EXISTS_WRONG_RUNTIME_PATH_BINDING",
                    "expectedCandidates": [str(candidate) for candidate in candidates],
                    "actualRuntimePath": str(runtime_base / DEPLOYED_TRAINER_ROOT / module),
                }
            )

    immutable_inputs = {
        "finalTrainingRuntimeAuthority": runtime_base / FINAL_TRAINING_RUNTIME_AUTHORITY_RELATIVE_PATH,
        "finalTrainingReadiness": runtime_base / FINAL_TRAINING_READINESS_RELATIVE_PATH,
        "finalTrainingInputAuthority": runtime_base / FINAL_TRAINING_INPUT_AUTHORITY_RELATIVE_PATH,
        "finalTrainingPartition": runtime_base / FINAL_TRAINING_PARTITION_RELATIVE_PATH,
        "finalTrainingLeakageValidation": runtime_base / FINAL_TRAINING_LEAKAGE_RELATIVE_PATH,
    }
    resolved_immutable_inputs: dict[str, str] = {}
    for role, input_path in immutable_inputs.items():
        if input_path.is_file() and not input_path.is_symlink():
            resolved_immutable_inputs[role] = str(input_path)
        else:
            unresolved.append(
                {
                    "role": role,
                    "classification": "C_ALREADY_EXISTS_WRONG_ROOT_ASSUMPTION",
                    "expectedPath": str(input_path),
                }
            )

    runtime_identity = _first_existing_file(_runtime_identity_candidates(runtime_base))
    if runtime_identity is None:
        unresolved.append(
            {
                "role": "runtimeIdentity",
                "classification": "A_ALREADY_EXISTS_WRONG_RUNTIME_PATH_BINDING",
                "expectedCandidates": [str(candidate) for candidate in _runtime_identity_candidates(runtime_base)],
                "actualRuntimePath": str(runtime_base / DEPLOYED_RUNTIME_IDENTITY_RELATIVE_PATH),
            }
        )

    persistent_inputs = {
        "finalCheckpointManifest": execution_base / FINAL_CHECKPOINT_MANIFEST_RELATIVE_PATH,
        "finalModelState": execution_base / FINAL_MODEL_STATE_RELATIVE_PATH,
    }
    resolved_persistent_inputs: dict[str, str] = {}
    for role, input_path in persistent_inputs.items():
        if input_path.is_file() and not input_path.is_symlink():
            resolved_persistent_inputs[role] = str(input_path)
        else:
            unresolved.append(
                {
                    "role": role,
                    "classification": "PERSISTENT_EXECUTION_ARTIFACT_MISSING",
                    "expectedPath": str(input_path),
                }
            )

    planned_outputs = {
        "rawPredictionOutput": execution_base / FINAL_RAW_PREDICTION_OUTPUT_RELATIVE_PATH,
        "scoredResultOutput": execution_base / FINAL_SCORED_RESULT_OUTPUT_RELATIVE_PATH,
        "executionReportOutput": execution_base / FINAL_EXECUTION_REPORT_OUTPUT_RELATIVE_PATH,
    }
    output_status: dict[str, dict[str, Any]] = {}
    for role, output_path in planned_outputs.items():
        parent = output_path.parent
        output_status[role] = {
            "path": str(output_path),
            "classification": "D_RESULT_OUTPUT_DIRECTORY_BINDING_MISSING",
            "requiredBeforeExecution": False,
            "createOnExecution": True,
            "parentExists": parent.exists(),
            "parentIsDirectoryOrCreatable": parent.is_dir() or not parent.exists(),
            "preexistingOutputForbidden": True,
            "preexistingOutputPresent": output_path.exists(),
        }

    return {
        "runtimeRoot": str(runtime_base),
        "executionRoot": str(execution_base),
        "modulePaths": module_paths,
        "immutableAuthorityInputs": resolved_immutable_inputs,
        "runtimeIdentityPath": str(runtime_identity) if runtime_identity else None,
        "persistentExecutionInputs": resolved_persistent_inputs,
        "plannedCreateOnExecutionOutputs": output_status,
        "unresolvedDependencies": unresolved,
        "unresolvedDependencyCount": len(unresolved),
        "newSemanticDependencyCount": 0,
        "holdoutAuthorityPath": "NOT_A_FILE_PRE_HOLDOUT_OPAQUE_AUTHORITY_REFERENCE",
        "holdoutAuthorityReference": FINAL_HOLDOUT_AUTHORITY_REFERENCE,
        "holdoutAuthorityLogicalDigest": FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST,
    }


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


def preflight_final_evaluation_authority_v1(
    root: str | Path,
    authority_path: str | Path,
    execution_root: str | Path | None = None,
) -> dict[str, Any]:
    authority = reload_final_evaluation_authority_v1(authority_path)
    runtime_root = Path(root)
    if _is_workspace_execution_root(runtime_root) and not _is_runtime_authority_root(runtime_root):
        return {
            "state": "FINAL_EVALUATION_PREFLIGHT_INVALID_AUTHORITY_ROOT",
            "authorityReference": authority["authorityReference"],
            "authorityLogicalDigest": authority["logicalDigest"],
            "runtimeRoot": str(runtime_root),
            "workspaceRootAsAuthorityRoot": "INVALID_BY_CONTRACT",
            "requiredInputCount": 0,
            "missingInputCount": 0,
            "missingInputs": [],
            "unresolvedDependencyCount": 0,
            "unresolvedDependencies": [],
            "newSemanticDependencyCount": 0,
            "holdoutOpened": False,
            "holdoutExposureCount": 0,
            "modelDeserializationCount": 0,
            "forwardCount": 0,
            "inferenceCount": 0,
            "trainingCount": 0,
        }

    resolved = resolve_final_evaluation_runtime_paths(runtime_root, execution_root)
    missing = [item.get("expectedPath") or item.get("actualRuntimePath") for item in resolved["unresolvedDependencies"]]
    return {
        "state": "FINAL_EVALUATION_PREFLIGHT_PASS" if not missing else "FINAL_EVALUATION_PREFLIGHT_BLOCKED",
        "authorityReference": authority["authorityReference"],
        "authorityLogicalDigest": authority["logicalDigest"],
        "runtimeRoot": resolved["runtimeRoot"],
        "executionRoot": resolved["executionRoot"],
        "workspaceRootAsAuthorityRoot": "NO",
        "requiredInputCount": 13,
        "missingInputCount": len(missing),
        "missingInputs": [str(item) for item in missing if item is not None],
        "resolvedPaths": {
            "modulePaths": resolved["modulePaths"],
            "immutableAuthorityInputs": resolved["immutableAuthorityInputs"],
            "runtimeIdentityPath": resolved["runtimeIdentityPath"],
            "persistentExecutionInputs": resolved["persistentExecutionInputs"],
            "plannedCreateOnExecutionOutputs": resolved["plannedCreateOnExecutionOutputs"],
            "holdoutAuthorityPath": resolved["holdoutAuthorityPath"],
        },
        "unresolvedDependencyCount": resolved["unresolvedDependencyCount"],
        "unresolvedDependencies": resolved["unresolvedDependencies"],
        "newSemanticDependencyCount": resolved["newSemanticDependencyCount"],
        "holdoutOpened": False,
        "holdoutExposureCount": 0,
        "modelDeserializationCount": 0,
        "forwardCount": 0,
        "inferenceCount": 0,
        "trainingCount": 0,
    }


def verify_final_holdout_execution_bindings_v1(
    runtime_root: str | Path,
    authority_path: str | Path,
    execution_root: str | Path | None = None,
) -> dict[str, Any]:
    preflight = preflight_final_evaluation_authority_v1(runtime_root, authority_path, execution_root)
    output_paths = preflight.get("resolvedPaths", {}).get("plannedCreateOnExecutionOutputs", {})
    output_binding_pass = all(
        item.get("createOnExecution") is True and item.get("parentIsDirectoryOrCreatable") is True
        for item in output_paths.values()
    )
    state = "FINAL_HOLDOUT_EXECUTION_BINDINGS_PASS" if preflight["state"] == "FINAL_EVALUATION_PREFLIGHT_PASS" and output_binding_pass else "FINAL_HOLDOUT_EXECUTION_BINDINGS_BLOCKED"
    return {
        "state": state,
        "preflightState": preflight["state"],
        "transitiveRequiredDependencyCount": preflight["requiredInputCount"],
        "transitiveUnresolvedDependencyCount": preflight["unresolvedDependencyCount"],
        "runtimeAuthorityV2Match": preflight["state"] == "FINAL_EVALUATION_PREFLIGHT_PASS",
        "trainingReadinessV2Match": preflight["state"] == "FINAL_EVALUATION_PREFLIGHT_PASS",
        "resultOutputBindingPass": output_binding_pass,
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
    parser.add_argument("--execution-root")
    parser.add_argument("--write-authority", action="store_true")
    parser.add_argument("--preflight", action="store_true")
    args = parser.parse_args(list(arguments) if arguments is not None else None)
    try:
        if args.write_authority:
            persist_final_evaluation_authority_v1(args.authority, build_final_evaluation_authority_v1())
        if args.preflight:
            print(json.dumps(preflight_final_evaluation_authority_v1(args.root, args.authority, args.execution_root), sort_keys=True))
        else:
            value = reload_final_evaluation_authority_v1(args.authority)
            print(json.dumps({"state": "FINAL_EVALUATION_AUTHORITY_RELOAD_PASS", "authorityReference": value["authorityReference"], "logicalDigest": value["logicalDigest"]}, sort_keys=True))
        return 0
    except (FinalEvaluationAuthorityError, OSError, UnicodeError, ValueError) as error:
        print(f"{type(error).__name__}: {error}", file=os.sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(run_cli())
