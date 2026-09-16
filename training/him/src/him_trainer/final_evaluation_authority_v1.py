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
import tempfile
import os
import re
from pathlib import Path
from typing import Any, Mapping, Sequence

from him_trainer.final_evaluation_v2_execution_contract import build_execution_contract


CONTRACT_ID = "HIM_FINAL_EVALUATION_AUTHORITY_V1"
VERSION = 1
DIGEST_SCHEME = "sha256(canonical UTF-8 JSON; ensure_ascii=false, sort_keys=true, compact separators)"

FINAL_CHECKPOINT_REFERENCE = "him-training-checkpoint:v2:beba3ba4ba75ba1d117f3ed13482e8389b2f428406dd92d4cc41d1b476140197"
FINAL_CHECKPOINT_LOGICAL_DIGEST = "923d0a479847b21e57ff4b1c5649c1a38f07bb3a431b8e06ea9bec50bcd54632"
FINAL_MODEL_STATE_SHA256 = "6fd984f5375ef780ab81187e053bf468158ddb849932553ea4062a745796443e"
RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT = "DEPLOYMENT_TIME_IMMUTABLE_OCI"
SOURCE_HEAD = "545510e7547fd3b723ad17efca9a093390d381d6"

FINAL_HOLDOUT_AUTHORITY_REFERENCE = "p2-family-isolated-holdout-authority:v1:5caa1788cfa7eff8d6d02d317aeebb6ef1b72ecba434ac211e3624f62a9e2a51"
FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST = "5caa1788cfa7eff8d6d02d317aeebb6ef1b72ecba434ac211e3624f62a9e2a51"
FINAL_HOLDOUT_V2_REFERENCE = "him-final-evaluation-v2-holdout:v1:28d61146b5181bc06020f35ec6eeb1b90262f75456b56fe1554135c3d052145b"
FINAL_HOLDOUT_V2_LOGICAL_DIGEST = "28d61146b5181bc06020f35ec6eeb1b90262f75456b56fe1554135c3d052145b"
FINAL_GROUND_TRUTH_REFERENCE = "him-final-evaluation-v2-prospective-ground-truth:v1:09084f0895a4d3c595996ed4194031f8a2210d581302b4f9fecc82d7b2b38958"
FINAL_GROUND_TRUTH_LOGICAL_DIGEST = "09084f0895a4d3c595996ed4194031f8a2210d581302b4f9fecc82d7b2b38958"

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
DEFAULT_FINAL_HOLDOUT_AUTHORITY_ROOT = Path("/workspace/p2-expanded-validation-packet/packet/authority")
FINAL_HOLDOUT_AUTHORITY_FILENAME = "p2-family-isolated-holdout-authority.v1.json"
FINAL_SEALED_ROOT_REQUIRED_FILES = (
    "review-packet.v2.json",
    "ground-truth.v2.json",
    "sealed-evaluation-authority.v2.json",
    "final-validation-report.v2.json",
)
DEFAULT_FINAL_MODEL_ROOT = Path("/workspace/models/xlm-roberta-base/e73636d4f797dec63c3081bb6ed5c7b0bb3f2089")
DEFAULT_FINAL_TOKENIZER_PATH = DEFAULT_FINAL_MODEL_ROOT / "tokenizer.json"
FINAL_HOLDOUT_EXECUTION_ENTRYPOINT = "him_trainer.final_evaluation_authority_v1"
FINAL_HOLDOUT_EXECUTION_COMMAND_MODE = "--execute-final-holdout"
FINAL_HOLDOUT_PREFLIGHT_COMMAND_MODE = "--final-holdout-execution-preflight"
FINAL_HOLDOUT_CUDA_COMPAT_PREFIX = "/usr/local/cuda-13.0/compat/lib.real:/usr/local/cuda-13.0/compat"
REQUIRED_MODEL_ROOT_FILES = ("config.json", "model.safetensors", "tokenizer.json")
REQUIRED_RUNTIME_MODULES = (
    "final_evaluation_authority_v1.py",
    "final_evaluation_v2_execution_contract.py",
    "input_representation_v2.py",
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
        "Native Final Evaluation V2 runner",
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
    execution_contract = build_execution_contract()
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
            "runtimeOciDigest": None,
            "runtimeImageDigestBindingStage": RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT,
        },
        "holdoutAuthority": {
            "reference": FINAL_HOLDOUT_AUTHORITY_REFERENCE,
            "logicalDigest": FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST,
            "resolved": True,
            "opaqueMetadataOnly": True,
            "membershipIncluded": False,
            "labelsIncluded": False,
            "holdoutV2Reference": FINAL_HOLDOUT_V2_REFERENCE,
            "holdoutV2LogicalDigest": FINAL_HOLDOUT_V2_LOGICAL_DIGEST,
            "groundTruthReference": FINAL_GROUND_TRUTH_REFERENCE,
            "groundTruthLogicalDigest": FINAL_GROUND_TRUTH_LOGICAL_DIGEST,
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
            "executionContractReference": execution_contract["reference"],
            "executionContractLogicalDigest": execution_contract["logicalDigest"],
            "baseModelAuthorityReference": f"FacebookAI/xlm-roberta-base@{BASE_MODEL_REVISION}",
            "baseModelRevision": BASE_MODEL_REVISION,
            "runtimeAuthorityReference": FINAL_RUNTIME_AUTHORITY_REFERENCE,
            "finalReadinessReference": FINAL_READINESS_REFERENCE,
            "trainingInputAuthorityReference": FINAL_INPUT_AUTHORITY_REFERENCE,
            "finalCorpusReference": FINAL_CORPUS_REFERENCE,
            "finalPartitionReference": FINAL_PARTITION_REFERENCE,
        },
        "executionEntrypoint": {
            "module": FINAL_HOLDOUT_EXECUTION_ENTRYPOINT,
            "executionAuthorized": True,
            "runnerBinding": "FINAL_EVALUATION_AUTHORITY_V1_TO_EXISTING_V2_EVALUATOR_PRIMITIVES",
            "futureEvaluationUses": "native Final Evaluation V2 runner with final checkpoint binding",
            "preflightImportRequired": True,
            "runtimeImageDigestBindingStage": RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT,
            "runtimeImageDigestRequiredAtExecution": True,
            "runtimeImageDigestSource": "HIM_RUNTIME_IMAGE_DIGEST",
            "runtimeImageDigestImmutable": True,
            "runtimeImageDigestVerifiedBeforeHoldoutOpen": True,
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
    _require(evaluated.get("runtimeOciDigest") is None, "STATIC_RUNTIME_OCI_DIGEST_FORBIDDEN")
    _require(
        evaluated.get("runtimeImageDigestBindingStage") == RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT,
        "RUNTIME_IMAGE_DIGEST_BINDING_STAGE_INVALID",
    )
    opaque = payload.get("holdoutAuthority")
    _require(isinstance(opaque, Mapping), "HOLDOUT_AUTHORITY_INVALID")
    _require(opaque.get("reference") == FINAL_HOLDOUT_AUTHORITY_REFERENCE, "HOLDOUT_REFERENCE_INVALID")
    _require(opaque.get("logicalDigest") == FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST, "HOLDOUT_DIGEST_INVALID")
    _require(opaque.get("membershipIncluded") is False and opaque.get("labelsIncluded") is False, "HOLDOUT_CONTENT_INCLUDED")
    _require(opaque.get("holdoutV2Reference") == FINAL_HOLDOUT_V2_REFERENCE, "HOLDOUT_V2_REFERENCE_INVALID")
    _require(opaque.get("holdoutV2LogicalDigest") == FINAL_HOLDOUT_V2_LOGICAL_DIGEST, "HOLDOUT_V2_DIGEST_INVALID")
    _require(opaque.get("groundTruthReference") == FINAL_GROUND_TRUTH_REFERENCE, "GROUND_TRUTH_REFERENCE_INVALID")
    _require(opaque.get("groundTruthLogicalDigest") == FINAL_GROUND_TRUTH_LOGICAL_DIGEST, "GROUND_TRUTH_DIGEST_INVALID")
    bindings = payload.get("authorityBindings")
    contract = build_execution_contract()
    _require(isinstance(bindings, Mapping), "AUTHORITY_BINDINGS_INVALID")
    _require(bindings.get("executionContractReference") == contract["reference"], "EXECUTION_CONTRACT_REFERENCE_INVALID")
    _require(bindings.get("executionContractLogicalDigest") == contract["logicalDigest"], "EXECUTION_CONTRACT_DIGEST_INVALID")
    input_contract = payload.get("inputContract")
    _require(isinstance(input_contract, Mapping) and input_contract.get("frozen") is True, "INPUT_CONTRACT_NOT_FROZEN")
    _require(input_contract.get("inputRepresentation") == "V2", "INPUT_REPRESENTATION_INVALID")
    outputs = payload.get("outputs")
    _require(isinstance(outputs, Mapping), "OUTPUTS_INVALID")
    _require(outputs.get("primary") == "TARGET_KIND", "PRIMARY_OUTPUT_INVALID")
    _require(outputs.get("secondary") == "CANDIDATE_COMPATIBILITY", "SECONDARY_OUTPUT_INVALID")
    entrypoint = payload.get("executionEntrypoint")
    _require(isinstance(entrypoint, Mapping), "EXECUTION_ENTRYPOINT_INVALID")
    _require(entrypoint.get("module") == FINAL_HOLDOUT_EXECUTION_ENTRYPOINT, "EXECUTION_ENTRYPOINT_MISMATCH")
    _require(entrypoint.get("executionAuthorized") is True, "EXECUTION_NOT_AUTHORIZED")
    _require(
        entrypoint.get("runtimeImageDigestBindingStage") == RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT
        and entrypoint.get("runtimeImageDigestRequiredAtExecution") is True
        and entrypoint.get("runtimeImageDigestSource") == "HIM_RUNTIME_IMAGE_DIGEST"
        and entrypoint.get("runtimeImageDigestImmutable") is True
        and entrypoint.get("runtimeImageDigestVerifiedBeforeHoldoutOpen") is True,
        "DEPLOYMENT_RUNTIME_OCI_BINDING_CONTRACT_INVALID",
    )
    _require(entrypoint.get("newSemanticComponentRequiredCount", 0) == 0, "NEW_SEMANTIC_COMPONENT_REQUIRED")
    _require(entrypoint.get("newMetricDefinitionCount", 0) == 0, "NEW_METRIC_DEFINITION_REQUIRED")
    _require(entrypoint.get("newOutputHeadCount", 0) == 0, "NEW_OUTPUT_HEAD_REQUIRED")
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


def validate_deployment_time_oci_binding_v1(
    authority_path: str | Path,
    runtime_image_digest: str | None,
    runtime_identity_path: str | Path | None = None,
) -> str:
    """Resolve the immutable OCI identity supplied by deployment, never build time."""

    authority = reload_final_evaluation_authority_v1(authority_path)
    evaluated = authority["authorityPayload"]["evaluatedCheckpoint"]
    _require(evaluated.get("runtimeOciDigest") is None, "STATIC_RUNTIME_OCI_DIGEST_FORBIDDEN")
    _require(
        evaluated.get("runtimeImageDigestBindingStage") == RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT,
        "RUNTIME_IMAGE_DIGEST_BINDING_STAGE_INVALID",
    )
    _require(
        isinstance(runtime_image_digest, str) and re.fullmatch(r"sha256:[0-9a-f]{64}", runtime_image_digest) is not None,
        "DEPLOYMENT_RUNTIME_OCI_DIGEST_REQUIRED_OR_INVALID",
    )
    if runtime_identity_path is not None:
        identity = _read_json(Path(runtime_identity_path))
        observed = identity.get("ociImageDigest")
        if observed is not None:
            _require(observed == runtime_image_digest, "DEPLOYMENT_RUNTIME_OCI_DIGEST_MISMATCH")
    return runtime_image_digest


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
        "requiredInputCount": 13 + len(FINAL_SEALED_ROOT_REQUIRED_FILES),
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


def _path_status(path: Path, *, must_exist: bool, create_on_execution: bool = False) -> dict[str, Any]:
    parent = path.parent
    exists = path.exists()
    return {
        "path": str(path),
        "exists": exists,
        "mustExistBeforeExecution": must_exist,
        "createOnExecution": create_on_execution,
        "parentExists": parent.exists(),
        "parentIsDirectoryOrCreatable": parent.is_dir() or not parent.exists(),
        "resolved": (path.is_file() and not path.is_symlink()) if must_exist else (parent.is_dir() or not parent.exists()),
    }


def _default_final_checkpoint_manifest(execution_root: str | Path) -> Path:
    return Path(execution_root) / FINAL_CHECKPOINT_MANIFEST_RELATIVE_PATH


def _default_final_output_root(execution_root: str | Path) -> Path:
    return Path(execution_root) / FINAL_EVALUATION_OUTPUT_RELATIVE_ROOT


def _load_checkpoint_manifest_for_path_binding(path: Path) -> dict[str, Any]:
    manifest = _read_json(path)
    _require(manifest.get("checkpointReference") == FINAL_CHECKPOINT_REFERENCE, "FINAL_CHECKPOINT_REFERENCE_MISMATCH")
    _require(manifest.get("checkpointLogicalDigest") == FINAL_CHECKPOINT_LOGICAL_DIGEST, "FINAL_CHECKPOINT_DIGEST_MISMATCH")
    model_state = manifest.get("modelState")
    _require(isinstance(model_state, Mapping), "FINAL_MODEL_STATE_METADATA_INVALID")
    relative = model_state.get("relativePath")
    _require(isinstance(relative, str) and relative and not Path(relative).is_absolute(), "FINAL_MODEL_STATE_RELATIVE_PATH_INVALID")
    return manifest


def _final_model_state_path_from_manifest(manifest_path: Path, manifest: Mapping[str, Any]) -> Path:
    relative = manifest["modelState"]["relativePath"]
    root = manifest_path.parent.parent
    resolved = (root / relative).resolve()
    _require(resolved.is_relative_to((root / "checkpoint").resolve()), "FINAL_MODEL_STATE_ESCAPES_CHECKPOINT_ROOT")
    return resolved


def final_holdout_execution_command_v1(
    *,
    runtime_root: str | Path = "/opt/him",
    execution_root: str | Path = "/workspace",
    output_root: str | Path | None = None,
    holdout_authority_root: str | Path | None = None,
    checkpoint_manifest: str | Path | None = None,
    model_root: str | Path | None = None,
    tokenizer_path: str | Path | None = None,
    evaluation_root: str | Path | None = None,
    runtime_authority: str | Path | None = None,
) -> tuple[str, ...]:
    execution = Path(execution_root)
    root = Path(runtime_root)
    authority = root / FINAL_EVALUATION_AUTHORITY_RELATIVE_PATH
    output = Path(output_root) if output_root is not None else _default_final_output_root(execution)
    holdout_root = Path(holdout_authority_root) if holdout_authority_root is not None else DEFAULT_FINAL_HOLDOUT_AUTHORITY_ROOT
    checkpoint = Path(checkpoint_manifest) if checkpoint_manifest is not None else _default_final_checkpoint_manifest(execution)
    model = Path(model_root) if model_root is not None else DEFAULT_FINAL_MODEL_ROOT
    tokenizer = Path(tokenizer_path) if tokenizer_path is not None else model / "tokenizer.json"
    evaluation = Path(evaluation_root) if evaluation_root is not None else execution / "final-holdout"
    runtime_authority_path = Path(runtime_authority) if runtime_authority is not None else root / FINAL_TRAINING_RUNTIME_AUTHORITY_RELATIVE_PATH
    return (
        "env",
        "HIM_CUDA_COMPAT_REQUIRED=YES",
        f"HIM_CUDA_COMPAT_PREFIX={FINAL_HOLDOUT_CUDA_COMPAT_PREFIX}",
        f"LD_LIBRARY_PATH={FINAL_HOLDOUT_CUDA_COMPAT_PREFIX}",
        str(root / "runtime/bin/python"),
        "-m",
        FINAL_HOLDOUT_EXECUTION_ENTRYPOINT,
        FINAL_HOLDOUT_EXECUTION_COMMAND_MODE,
        "--root",
        str(root),
        "--authority",
        str(authority),
        "--execution-root",
        str(execution),
        "--holdout-authority-root",
        str(holdout_root),
        "--checkpoint-manifest",
        str(checkpoint),
        "--model-root",
        str(model),
        "--tokenizer-path",
        str(tokenizer),
        "--output-root",
        str(output),
        "--evaluation-root",
        str(evaluation),
        "--runtime-authority",
        str(runtime_authority_path),
    )


def verify_final_holdout_execution_path_v1(
    runtime_root: str | Path,
    authority_path: str | Path,
    execution_root: str | Path | None = None,
    *,
    holdout_authority_root: str | Path | None = None,
    checkpoint_manifest: str | Path | None = None,
    model_root: str | Path | None = None,
    tokenizer_path: str | Path | None = None,
    output_root: str | Path | None = None,
    evaluation_root: str | Path | None = None,
    runtime_authority: str | Path | None = None,
) -> dict[str, Any]:
    execution = Path(execution_root) if execution_root is not None else default_execution_root_for_runtime_root(runtime_root)
    output = Path(output_root) if output_root is not None else _default_final_output_root(execution)
    holdout_root = Path(holdout_authority_root) if holdout_authority_root is not None else DEFAULT_FINAL_HOLDOUT_AUTHORITY_ROOT
    checkpoint_path = Path(checkpoint_manifest) if checkpoint_manifest is not None else _default_final_checkpoint_manifest(execution)
    model_root_path = Path(model_root) if model_root is not None else DEFAULT_FINAL_MODEL_ROOT
    tokenizer_file = Path(tokenizer_path) if tokenizer_path is not None else model_root_path / "tokenizer.json"

    binding = verify_final_holdout_execution_bindings_v1(runtime_root, authority_path, execution)
    gaps: list[dict[str, Any]] = []
    if binding["state"] != "FINAL_HOLDOUT_EXECUTION_BINDINGS_PASS":
        gaps.append({"role": "preHoldoutBinding", "state": binding["state"]})

    authority = reload_final_evaluation_authority_v1(authority_path)
    if authority["authorityReference"] != f"him-final-evaluation-authority:v1:{authority['logicalDigest']}":
        gaps.append({"role": "finalEvaluationAuthority", "state": "REFERENCE_INVALID"})

    holdout_authority_path = holdout_root / FINAL_HOLDOUT_AUTHORITY_FILENAME
    holdout_status = _path_status(holdout_authority_path, must_exist=True)
    if not holdout_status["resolved"]:
        gaps.append({"role": "holdoutAuthority", "state": "PATH_NOT_RESOLVED", "path": holdout_status["path"]})

    # The review packet is a required execution input, not an execution output.
    sealed_root = Path(evaluation_root) if evaluation_root is not None else None
    sealed_root_files: dict[str, dict[str, Any]] = {}
    if sealed_root is not None:
        for filename in FINAL_SEALED_ROOT_REQUIRED_FILES:
            status = _path_status(sealed_root / filename, must_exist=True)
            sealed_root_files[filename] = status
            if not status["resolved"]:
                gaps.append({"role": f"sealedRoot:{filename}", "state": "PATH_NOT_RESOLVED", "path": status["path"]})

    checkpoint_status = _path_status(checkpoint_path, must_exist=True)
    model_state_status: dict[str, Any] = {"path": "UNRESOLVED", "resolved": False}
    if checkpoint_status["resolved"]:
        manifest = _load_checkpoint_manifest_for_path_binding(checkpoint_path)
        model_state_path = _final_model_state_path_from_manifest(checkpoint_path, manifest)
        model_state_status = _path_status(model_state_path, must_exist=True)
        if not model_state_status["resolved"]:
            gaps.append({"role": "modelState", "state": "PATH_NOT_RESOLVED", "path": model_state_status["path"]})
    else:
        gaps.append({"role": "checkpointManifest", "state": "PATH_NOT_RESOLVED", "path": checkpoint_status["path"]})

    model_files = {name: _path_status(model_root_path / name, must_exist=True) for name in REQUIRED_MODEL_ROOT_FILES}
    for name, status in model_files.items():
        if not status["resolved"]:
            gaps.append({"role": f"modelRoot:{name}", "state": "PATH_NOT_RESOLVED", "path": status["path"]})
    tokenizer_status = _path_status(tokenizer_file, must_exist=True)
    if not tokenizer_status["resolved"]:
        gaps.append({"role": "tokenizer", "state": "PATH_NOT_RESOLVED", "path": tokenizer_status["path"]})

    raw_path = output / FINAL_RAW_PREDICTION_OUTPUT_RELATIVE_PATH.name
    result_path = output / FINAL_SCORED_RESULT_OUTPUT_RELATIVE_PATH.name
    report_path = output / FINAL_EXECUTION_REPORT_OUTPUT_RELATIVE_PATH.name
    raw_status = _path_status(raw_path, must_exist=False, create_on_execution=True)
    result_status = _path_status(result_path, must_exist=False, create_on_execution=True)
    report_status = _path_status(report_path, must_exist=False, create_on_execution=True)
    for role, status in (("rawPredictionOutput", raw_status), ("resultOutput", result_status), ("reportOutput", report_status)):
        if not status["resolved"]:
            gaps.append({"role": role, "state": "OUTPUT_PATH_NOT_RESOLVED", "path": status["path"]})

    command = final_holdout_execution_command_v1(
        runtime_root=runtime_root,
        execution_root=execution,
        output_root=output,
        holdout_authority_root=holdout_root,
        checkpoint_manifest=checkpoint_path,
        model_root=model_root_path,
        tokenizer_path=tokenizer_file,
        evaluation_root=evaluation_root,
        runtime_authority=runtime_authority,
    )
    resolved = len(gaps) == 0
    return {
        "state": "FINAL_HOLDOUT_EXECUTION_PATH_RESOLVED" if resolved else "FINAL_HOLDOUT_EXECUTION_PATH_BLOCKED",
        "finalHoldoutExecutionEntrypoint": FINAL_HOLDOUT_EXECUTION_ENTRYPOINT,
        "executionCommand": list(command),
        "finalEvaluationAuthorityReference": authority["authorityReference"],
        "finalHoldoutAuthorityReference": FINAL_HOLDOUT_AUTHORITY_REFERENCE,
        "finalCheckpointReference": FINAL_CHECKPOINT_REFERENCE,
        "finalModelStateSha256": FINAL_MODEL_STATE_SHA256,
        "holdoutAuthorityPath": holdout_status,
        "sealedEvaluationRoot": str(sealed_root) if sealed_root is not None else None,
        "sealedRootFiles": sealed_root_files,
        "checkpointManifestPath": checkpoint_status,
        "modelStatePath": model_state_status,
        "modelRoot": str(model_root_path),
        "modelRootFiles": model_files,
        "tokenizerPath": tokenizer_status,
        "rawPredictionOutputPath": raw_status,
        "resultOutputPath": result_status,
        "resultReloadPath": result_status,
        "reportOutputPath": report_status,
        "reportReloadPath": report_status,
        "finalHoldoutExecutionPathResolved": resolved,
        "finalHoldoutRawPredictionPersistencePathResolved": raw_status["resolved"],
        "finalHoldoutResultPersistencePathResolved": result_status["resolved"],
        "finalHoldoutResultReloadPathResolved": result_status["resolved"],
        "finalHoldoutReportPersistencePathResolved": report_status["resolved"],
        "finalHoldoutReportReloadPathResolved": report_status["resolved"],
        "finalPostHoldoutDeterministicIntegrationGapCount": len(gaps),
        "gaps": gaps,
        "newSemanticComponentRequiredCount": 0,
        "newMetricDefinitionCount": 0,
        "newOutputHeadCount": 0,
        "holdoutFileOpenCount": 0,
        "holdoutContentReadCount": 0,
        "holdoutDeserializationCount": 0,
        "holdoutExposureCount": 0,
        "holdoutOpened": False,
        "modelDeserializationCount": 0,
        "forwardCount": 0,
        "inferenceCount": 0,
        "trainingCount": 0,
        "backwardCount": 0,
        "optimizerStepCount": 0,
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
        "finalHoldoutExecutionAuthorized": True,
        "finalHoldoutExecutionRunnerBindingMatch": FINAL_HOLDOUT_EXECUTION_ENTRYPOINT == "him_trainer.final_evaluation_authority_v1",
        "resultOutputBindingPass": output_binding_pass,
        "holdoutOpened": False,
        "holdoutExposureCount": 0,
        "modelDeserializationCount": 0,
        "forwardCount": 0,
        "inferenceCount": 0,
        "trainingCount": 0,
    }


def execute_final_holdout_v1(
    *,
    runtime_root: str | Path,
    evaluation_root: str | Path,
    output_root: str | Path,
    authority_path: str | Path,
    checkpoint_manifest: str | Path,
    runtime_authority: str | Path,
    model_root: str | Path,
    tokenizer_path: str | Path,
    execution_root: str | Path,
    holdout_authority_root: str | Path,
) -> dict[str, Any]:
    """Run the native Final Evaluation V2 path after the binding gate.

    This function intentionally owns the productive orchestration.  The
    historical P2 evaluator is not part of this call graph: Final Evaluation
    V2 derives its count and input rows from its sealed V2 root and fails
    closed when the required model-input field is absent.
    """

    deployment_digest = validate_deployment_time_oci_binding_v1(
        authority_path,
        os.environ.get("HIM_RUNTIME_IMAGE_DIGEST"),
        Path(runtime_root) / DEPLOYED_RUNTIME_IDENTITY_RELATIVE_PATH,
    )
    plan = verify_final_holdout_execution_path_v1(
        runtime_root=runtime_root,
        authority_path=authority_path,
        execution_root=execution_root,
        holdout_authority_root=holdout_authority_root,
        checkpoint_manifest=checkpoint_manifest,
        model_root=model_root,
        tokenizer_path=tokenizer_path,
        output_root=output_root,
        evaluation_root=evaluation_root,
    )
    _require(plan["state"] == "FINAL_HOLDOUT_EXECUTION_PATH_RESOLVED", "FINAL_HOLDOUT_EXECUTION_PATH_NOT_RESOLVED")
    result = _execute_native_final_evaluation_v2(
        evaluation_root=evaluation_root,
        output_root=output_root,
        checkpoint_manifest=checkpoint_manifest,
        model_root=model_root,
        tokenizer_path=tokenizer_path,
        model_state_path=Path(plan["modelStatePath"]["path"]),
        deployment_digest=deployment_digest,
    )
    report_payload = {
        "contractId": CONTRACT_ID,
        "version": VERSION,
        "state": "FINAL_HOLDOUT_EXECUTION_COMPLETE",
        "authorityReference": plan["finalEvaluationAuthorityReference"],
        "checkpointReference": FINAL_CHECKPOINT_REFERENCE,
        "holdoutAuthorityReference": FINAL_HOLDOUT_AUTHORITY_REFERENCE,
        "runtimeImageDigest": deployment_digest,
        "runtimeImageDigestBindingStage": RUNTIME_IMAGE_DIGEST_BINDING_STAGE_DEPLOYMENT,
        "rawPredictionPath": str(result["rawPath"]),
        "resultPath": str(result["resultPath"]),
        "counters": {
            "modelDeserializationCount": result["counters"]["modelDeserializationCount"],
            "forwardCount": result["counters"]["forwardCount"],
            "trainingCount": 0,
            "backwardCount": 0,
            "optimizerStepCount": 0,
            "holdoutExposureCount": result["counters"]["holdoutExposureCount"],
        },
    }
    digest = logical_digest(report_payload)
    report = {"reportReference": f"him-final-evaluation-report:v1:{digest}", "logicalDigest": digest, "digestScheme": DIGEST_SCHEME, "reportPayload": report_payload}
    destination = Path(output_root) / FINAL_EXECUTION_REPORT_OUTPUT_RELATIVE_PATH.name
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(_canonical(report) + b"\n")
    reloaded = _read_json(destination)
    _require(reloaded.get("logicalDigest") == logical_digest(reloaded["reportPayload"]), "FINAL_EXECUTION_REPORT_DIGEST_MISMATCH")
    return {"result": result, "reportPath": destination, "report": reloaded}


def execute_synthetic_fixture_v2(*, evaluation_root: str | Path, output_root: str | Path) -> dict[str, Any]:
    """Exercise native V2 persistence with an isolated model-free fixture."""

    del evaluation_root
    result = _execute_native_synthetic_fixture_v2(Path(output_root))
    payload = {
        "contractId": CONTRACT_ID,
        "version": VERSION,
        "state": "SYNTHETIC_FIXTURE_COMPLETE",
        "fixtureNamespace": "HIM_FINAL_EVALUATION_V2_FIXTURE_ONLY",
        "notFinalEvaluation": True,
        "modelDeserializationCount": 0,
        "forwardCount": 0,
        "holdoutExposureCount": 0,
        "rawPredictionPath": str(result["rawPath"]),
        "resultPath": str(result["resultPath"]),
    }
    digest = logical_digest(payload)
    report = {"reportReference": f"him-final-evaluation-fixture-report:v2:{digest}", "logicalDigest": digest, "digestScheme": DIGEST_SCHEME, "reportPayload": payload}
    destination = Path(output_root) / FINAL_EXECUTION_REPORT_OUTPUT_RELATIVE_PATH.name
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(_canonical(report) + b"\n")
    reloaded = _read_json(destination)
    _require(reloaded["logicalDigest"] == logical_digest(reloaded["reportPayload"]), "FIXTURE_REPORT_DIGEST_MISMATCH")
    return {"result": result, "reportPath": destination, "report": reloaded}


def _validate_native_outer(path: Path, payload_key: str) -> dict[str, Any]:
    value = _read_json(path)
    payload = value.get(payload_key)
    _require(isinstance(payload, Mapping), f"NATIVE_PAYLOAD_INVALID:{path.name}")
    digest = logical_digest(payload)
    _require(value.get("logicalDigest") == digest, f"NATIVE_DIGEST_INVALID:{path.name}")
    reference = value.get("authorityReference", value.get("reportReference"))
    _require(isinstance(reference, str) and reference.endswith(f":{digest}"), f"NATIVE_REFERENCE_INVALID:{path.name}")
    return value


def _load_native_sealed_root_v2(root: Path) -> dict[str, Any]:
    """Load only the four sealed V2 execution inputs, without old P2 logic."""

    packet = _validate_native_outer(root / "review-packet.v2.json", "authorityPayload")
    ground_truth = _validate_native_outer(root / "ground-truth.v2.json", "authorityPayload")
    sealed = _validate_native_outer(root / "sealed-evaluation-authority.v2.json", "authorityPayload")
    report = _validate_native_outer(root / "final-validation-report.v2.json", "reportPayload")
    packet_payload = packet["authorityPayload"]
    truth_payload = ground_truth["authorityPayload"]
    sealed_payload = sealed["authorityPayload"]
    report_payload = report["reportPayload"]
    _require(packet_payload.get("state") == "SEALED_UNEXPOSED", "NATIVE_REVIEW_PACKET_NOT_SEALED")
    _require(truth_payload.get("state") == "SEALED_UNEXPOSED", "NATIVE_GROUND_TRUTH_NOT_SEALED")
    _require(sealed_payload.get("state") == "SEALED_UNEXPOSED", "NATIVE_SEALED_AUTHORITY_NOT_SEALED")
    _require(report_payload.get("state") == "SEALED_UNEXPOSED", "NATIVE_VALIDATION_REPORT_NOT_SEALED")
    count = packet_payload.get("recordCount")
    _require(isinstance(count, int) and count > 0, "NATIVE_SEALED_RECORD_COUNT_INVALID")
    examples = truth_payload.get("evaluationExamples")
    _require(isinstance(examples, list) and len(examples) == count, "NATIVE_GROUND_TRUTH_COUNT_INVALID")
    _require(truth_payload.get("reviewPacketLogicalDigest") in (None, packet["logicalDigest"]), "NATIVE_PACKET_BINDING_INVALID")
    return {"packet": packet, "groundTruth": ground_truth, "sealedAuthority": sealed, "validationReport": report, "examples": tuple(examples), "count": count}


def _native_model_input_rows(sealed: Mapping[str, Any]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for example in sealed["examples"]:
        _require(isinstance(example, Mapping), "NATIVE_EXAMPLE_INVALID")
        model_input = example.get("modelInput")
        _require(isinstance(model_input, Mapping) and isinstance(model_input.get("serialized"), str) and model_input["serialized"], "NATIVE_MODEL_INPUT_UNRESOLVED")
        rows.append({"evaluationExampleReference": str(example.get("evaluationExampleReference")), "serialized": model_input["serialized"]})
    return rows


def _write_native_output(path: Path, payload_key: str, payload: Mapping[str, Any], prefix: str) -> dict[str, Any]:
    digest = logical_digest(payload)
    value = {"reference": f"{prefix}:{digest}", "logicalDigest": digest, "digestScheme": DIGEST_SCHEME, payload_key: dict(payload)}
    path.parent.mkdir(parents=True, exist_ok=True)
    _require(not path.exists(), f"NATIVE_OUTPUT_ALREADY_EXISTS:{path.name}")
    path.write_bytes(_canonical(value) + b"\n")
    reloaded = _read_json(path)
    _require(reloaded["logicalDigest"] == logical_digest(reloaded[payload_key]), f"NATIVE_OUTPUT_RELOAD_DIGEST_INVALID:{path.name}")
    return reloaded


def _execute_native_synthetic_fixture_v2(output_root: Path) -> dict[str, Any]:
    """Use six local synthetic rows; no real sealed artifacts or model are read."""

    output_root.mkdir(parents=True, exist_ok=True)
    examples = tuple({"evaluationExampleReference": f"fixture-example-{index}", "modelInput": {"serialized": f"<HIMV2>\\nO=fixture-{index}\\n</HIMV2>"}} for index in range(6))
    packet_payload = {"schema": "HIM_FINAL_EVALUATION_V2_REVIEW_PACKET", "version": 2, "state": "SEALED_UNEXPOSED", "recordCount": len(examples), "modelIndependent": True}
    packet = {"authorityReference": f"fixture-packet:{logical_digest(packet_payload)}", "logicalDigest": logical_digest(packet_payload), "authorityPayload": packet_payload}
    truth_payload = {"schema": "HIM_FINAL_EVALUATION_V2_GROUND_TRUTH", "version": 2, "state": "SEALED_UNEXPOSED", "reviewPacketLogicalDigest": packet["logicalDigest"], "evaluationExamples": list(examples)}
    truth = {"authorityReference": f"fixture-truth:{logical_digest(truth_payload)}", "logicalDigest": logical_digest(truth_payload), "authorityPayload": truth_payload}
    sealed_payload = {"schema": "HIM_FINAL_EVALUATION_V2_SEALED_AUTHORITY", "version": 2, "state": "SEALED_UNEXPOSED"}
    sealed = {"authorityReference": f"fixture-sealed:{logical_digest(sealed_payload)}", "logicalDigest": logical_digest(sealed_payload), "authorityPayload": sealed_payload}
    report_payload = {"schema": "HIM_FINAL_EVALUATION_V2_VALIDATION_REPORT", "version": 2, "state": "SEALED_UNEXPOSED"}
    validation = {"reportReference": f"fixture-validation:{logical_digest(report_payload)}", "logicalDigest": logical_digest(report_payload), "reportPayload": report_payload}
    with tempfile.TemporaryDirectory(prefix="him-final-native-sealed-") as directory:
        root = Path(directory)
        for filename, value in (("review-packet.v2.json", packet), ("ground-truth.v2.json", truth), ("sealed-evaluation-authority.v2.json", sealed), ("final-validation-report.v2.json", validation)):
            (root / filename).write_bytes(_canonical(value) + b"\n")
        loaded = _load_native_sealed_root_v2(root)
        inputs = _native_model_input_rows(loaded)
    raw_payload = {"schema": "HIM_FINAL_EVALUATION_V2_RAW_PREDICTIONS", "version": 1, "state": "FROZEN_BEFORE_SCORING", "evaluationExampleCount": loaded["count"], "predictions": [{"evaluationExampleReference": row["evaluationExampleReference"], "primaryPrediction": 1, "secondaryPrediction": 0} for row in inputs]}
    raw = _write_native_output(output_root / FINAL_RAW_PREDICTION_OUTPUT_RELATIVE_PATH.name, "payload", raw_payload, "him-final-evaluation-v2-predictions")
    result_payload = {"schema": "HIM_FINAL_EVALUATION_V2_RESULT", "version": 1, "state": "SYNTHETIC_FIXTURE_COMPLETE", "evaluationExampleCount": loaded["count"], "rawPredictionLogicalDigest": raw["logicalDigest"], "counters": {"modelDeserializationCount": 0, "forwardCount": 0, "holdoutExposureCount": 0}}
    result = _write_native_output(output_root / FINAL_SCORED_RESULT_OUTPUT_RELATIVE_PATH.name, "payload", result_payload, "him-final-evaluation-v2-result")
    return {"rawPath": output_root / FINAL_RAW_PREDICTION_OUTPUT_RELATIVE_PATH.name, "resultPath": output_root / FINAL_SCORED_RESULT_OUTPUT_RELATIVE_PATH.name, "raw": raw, "result": result, "counters": result_payload["counters"], "count": loaded["count"]}


def _execute_native_final_evaluation_v2(*, evaluation_root: str | Path, output_root: str | Path, checkpoint_manifest: str | Path, model_root: str | Path, tokenizer_path: str | Path, model_state_path: Path, deployment_digest: str) -> dict[str, Any]:
    """Native productive skeleton; model execution starts only after sealed inputs resolve."""

    sealed = _load_native_sealed_root_v2(Path(evaluation_root))
    inputs = _native_model_input_rows(sealed)
    _require(Path(model_root).is_dir() and Path(tokenizer_path).is_file() and model_state_path.is_file(), "NATIVE_MODEL_INPUT_PATH_UNRESOLVED")
    _require(Path(checkpoint_manifest).is_file(), "NATIVE_CHECKPOINT_MANIFEST_UNRESOLVED")
    del output_root, deployment_digest
    raise FinalEvaluationAuthorityError("NATIVE_MODEL_EXECUTION_REQUIRES_EXPLICIT_MODEL_FORWARD_IMPLEMENTATION")


def run_cli(arguments: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="him_trainer.final_evaluation_authority_v1")
    parser.add_argument("--root", default=".")
    parser.add_argument("--authority", required=True)
    parser.add_argument("--execution-root")
    parser.add_argument("--holdout-authority-root")
    parser.add_argument("--checkpoint-manifest")
    parser.add_argument("--model-root")
    parser.add_argument("--tokenizer-path")
    parser.add_argument("--output-root")
    parser.add_argument("--evaluation-root")
    parser.add_argument("--runtime-authority")
    parser.add_argument("--write-authority", action="store_true")
    parser.add_argument("--preflight", action="store_true")
    parser.add_argument("--final-holdout-execution-preflight", action="store_true")
    parser.add_argument("--execute-final-holdout", action="store_true")
    args = parser.parse_args(list(arguments) if arguments is not None else None)
    try:
        if args.write_authority:
            persist_final_evaluation_authority_v1(args.authority, build_final_evaluation_authority_v1())
        if args.final_holdout_execution_preflight:
            print(json.dumps(verify_final_holdout_execution_path_v1(
                args.root,
                args.authority,
                args.execution_root,
                holdout_authority_root=args.holdout_authority_root,
                checkpoint_manifest=args.checkpoint_manifest,
                model_root=args.model_root,
                tokenizer_path=args.tokenizer_path,
                output_root=args.output_root,
            ), sort_keys=True))
        elif args.execute_final_holdout:
            for option, message in (
                (args.execution_root, "EXECUTION_ROOT_REQUIRED"),
                (args.holdout_authority_root, "HOLDOUT_AUTHORITY_ROOT_REQUIRED"),
                (args.checkpoint_manifest, "CHECKPOINT_MANIFEST_REQUIRED"),
                (args.runtime_authority, "RUNTIME_AUTHORITY_REQUIRED"),
                (args.model_root, "MODEL_ROOT_REQUIRED"),
                (args.tokenizer_path, "TOKENIZER_PATH_REQUIRED"),
                (args.output_root, "OUTPUT_ROOT_REQUIRED"),
                (args.evaluation_root, "EVALUATION_ROOT_REQUIRED"),
            ):
                _require(option is not None, message)
            execution = execute_final_holdout_v1(
                runtime_root=args.root,
                evaluation_root=args.evaluation_root,
                output_root=args.output_root,
                authority_path=args.authority,
                checkpoint_manifest=args.checkpoint_manifest,
                runtime_authority=args.runtime_authority,
                model_root=args.model_root,
                tokenizer_path=args.tokenizer_path,
                execution_root=args.execution_root,
                holdout_authority_root=args.holdout_authority_root,
            )
            print(json.dumps({"state": execution["report"]["reportPayload"]["state"], "report": str(execution["reportPath"])}, sort_keys=True))
        elif args.preflight:
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
