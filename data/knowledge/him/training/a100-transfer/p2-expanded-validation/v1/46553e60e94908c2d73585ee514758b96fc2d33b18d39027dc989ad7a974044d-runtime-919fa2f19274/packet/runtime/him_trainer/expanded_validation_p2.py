"""Authority-bound, evaluation-only control plane for P2 Expanded Validation.

This module deliberately keeps model execution behind ``execute_evaluation``.
Loading authorities, constructing tensors, and producing dry-run evidence are
model-free and never import torch.  The Expanded Validation authority is the
sole source of membership and labels; the historical training packet is used
only for leakage checks.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Mapping, Sequence


CONTRACT_ID = "HIM_P2_EXPANDED_VALIDATION_EVALUATOR_V1"
VERSION = 1
EXPECTED_TOTAL = 19
EXPECTED_PRIMARY_ACTIVE = 13
EXPECTED_PRIMARY_INACTIVE = 6
EXPECTED_SECONDARY_ACTIVE = 19
EXPECTED_IDENTITY = 6
EXPECTED_VARIANT = 7
EXPECTED_COMPATIBLE = 13
EXPECTED_REJECT = 6
EXPECTED_REJECT_BOUNDARY_DIVERSITY = "2/3"
EXPECTED_CANDIDATE_REFERENCE = "p2-trained-candidate:v1:f3c4105735ee742c53638784bf07fb19db11df52456a11d5918176030c61ee00"
EXPECTED_CHECKPOINT_REFERENCE = "him-training-checkpoint:v2:29312dac2a6524d1ca2e20563831d19346ae5cd831610db5a2bf160229437622"
EXPECTED_CHECKPOINT_DIGEST = "a3a152b77b3ca51f356b262267d6230533f8b9ea9031f48e0e813e415a7e599e"
PRIMARY_UNIVERSE = ("EXISTING_CANONICAL", "IDENTITY", "VARIANT", "ALIAS", "NEW_CANONICAL")
SECONDARY_UNIVERSE = ("COMPATIBLE", "REJECT")

AUTHORITY_FILES = {
    "relation": "p2-final-relation-authority.v1.json",
    "primary": "p2-final-primary-target-authority.v1.json",
    "compatibility": "p2-final-candidate-compatibility-authority.v1.json",
    "boundary": "p2-final-negative-boundary-authority.v1.json",
    "expanded": "p2-expanded-validation-authority.v1.json",
    "holdout": "p2-family-isolated-holdout-authority.v1.json",
    "limitations": "p2-evaluation-limitations-authority.v1.json",
}
EXPECTED_AUTHORITY_REFERENCES = {
    "relation": "p2-evaluation-relation-authority:v1:ba2025505c6c82fd2f9f169a8fbb77c0651961204982eaedf9866f5b7fa21e7f",
    "primary": "p2-evaluation-primary-target-authority:v1:797eddea0a7e2dafcccf3eb84e4c1c2067d706c8bad59a75f7285181e948902d",
    "compatibility": "p2-evaluation-candidate-compatibility-authority:v1:9863774999927b44afe0ade907475d2e7f9f9312c8fd770ab91d3a20614ad1b8",
    "boundary": "p2-evaluation-negative-boundary-authority:v1:02243b70a4eec7bcac8f6afb353b9d5f81e2c517a653e6604fd20e7083c5ac9f",
    "expanded": "p2-expanded-validation-authority:v1:46553e60e94908c2d73585ee514758b96fc2d33b18d39027dc989ad7a974044d",
    "holdout": "p2-family-isolated-holdout-authority:v1:dc19557950bb3737ea51fc94d04616f2f2c96233868be294773bd120509b7c5a",
    "limitations": "p2-evaluation-limitations-authority:v1:dd63d332d8c6e152b5a0c431213f4806853559f69f73f68f836050c8bdea4de1",
}
EXPECTED_RUNTIME_BINDING = {
    "cuda": "13.0",
    "python": "3.13.14",
    "pytorch": "2.14.0+cu130",
    "sentencepiece": "0.2.2",
    "tokenizers": "0.23.1",
    "transformers": "ABSENT",
    "ociImage": "ghcr.io/logfather/him-a100-reference-runtime@sha256:87b74e2b58b3918840890209c42f1a0bf468135cc37156e00d2d3591dd20723a",
    "runtimeImageDefinitionDigest": "b016c8805c52f2fab3d4883dcae3f859fda40332c3c4ce17507f09a6cc541b4e",
}
PRODUCTIVE_V2_RUNTIME_BINDING = {
    **EXPECTED_RUNTIME_BINDING,
    "ociImage": "ghcr.io/logfather/him-a100-reference-runtime@sha256:919fa2f19274a4b009405fb71f0e899dc4d08eb97996f20875c7c9fbbde267ec",
    "runtimeImageDefinitionDigest": "7819f4b0de5c83a52dbe2a9e9caf7f2deba5e803f1d642a0c02598202b53c774",
}
PACKET_DRY_RUN_OUTPUT_NAME = "expanded-validation-dry-run.v1.json"
REAL_EVALUATION_RESULT_FILENAME = "expanded-validation-result.v1.json"
REAL_EVALUATION_RESULT_CONTRACT_ID = "HIM_P2_EXPANDED_VALIDATION_RESULT_V1"
REAL_EVALUATION_RESULT_SCHEMA_VERSION = 1
EVALUATOR_ENTRYPOINT = "him_trainer.expanded_validation_p2"
RUNTIME_REQUIRED_BASE_MODEL_FILES = ("config.json", "model.safetensors", "tokenizer.json")
TOKENIZER_FILENAME = "tokenizer.json"
PRIMARY_CLASS_ORDER = ("EC", "ID", "VAR", "ALIAS", "NEW_CANONICAL")
PRIMARY_CLASS_CODES = {
    "EXISTING_CANONICAL": 1,
    "IDENTITY": 2,
    "VARIANT": 3,
    "ALIAS": 4,
    "NEW_CANONICAL": 5,
}
SECONDARY_CLASS_ORDER = ("COMPATIBLE", "REJECT")
SECONDARY_CLASS_CODES = {"COMPATIBLE": 0, "REJECT": 1}


class ExpandedValidationContractError(ValueError):
    """Raised whenever an evaluation authority or guard is invalid."""


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise ExpandedValidationContractError(message)


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def _json_value(value: Any) -> Any:
    """Convert a tensor-like value to deterministic JSON-compatible values."""

    detached = value.detach() if hasattr(value, "detach") else value
    cpu_value = detached.cpu() if hasattr(detached, "cpu") else detached
    result = cpu_value.tolist() if hasattr(cpu_value, "tolist") else cpu_value
    if isinstance(result, list):
        return [_json_value(item) for item in result]
    if isinstance(result, tuple):
        return [_json_value(item) for item in result]
    if isinstance(result, float):
        _require(math.isfinite(result), "RESULT_NON_FINITE_VALUE")
    return result


def _read_json(path: Path) -> dict[str, Any]:
    _require(path.is_file() and not path.is_symlink(), f"AUTHORITY_FILE_INVALID:{path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except Exception as error:
        raise ExpandedValidationContractError(f"AUTHORITY_JSON_INVALID:{path}") from error
    _require(isinstance(value, dict), f"AUTHORITY_OBJECT_INVALID:{path}")
    return value


@dataclass(frozen=True)
class AuthorityBundle:
    relation: Mapping[str, Any]
    primary: Mapping[str, Any]
    compatibility: Mapping[str, Any]
    boundary: Mapping[str, Any]
    expanded: Mapping[str, Any]
    holdout: Mapping[str, Any]
    limitations: Mapping[str, Any]

    def as_dict(self) -> dict[str, Mapping[str, Any]]:
        return {name: getattr(self, name) for name in AUTHORITY_FILES}


def load_authorities(authority_root: str | Path) -> AuthorityBundle:
    root = Path(authority_root)
    values = {name: _read_json(root / filename) for name, filename in AUTHORITY_FILES.items()}
    for name, value in values.items():
        _require(value.get("reference") == EXPECTED_AUTHORITY_REFERENCES[name], f"AUTHORITY_REFERENCE_MISMATCH:{name}")
        _require(value.get("logicalDigest") == EXPECTED_AUTHORITY_REFERENCES[name].rsplit(":", 1)[-1], f"AUTHORITY_DIGEST_REFERENCE_MISMATCH:{name}")
        _require(value.get("state", "").endswith("FINALIZED"), f"AUTHORITY_NOT_FINALIZED:{name}")
    bundle = AuthorityBundle(**values)
    expanded = bundle.expanded
    _require(expanded.get("recordCount") == EXPECTED_TOTAL, "EXPANDED_COUNT_INVALID")
    _require(expanded.get("identityCount") == EXPECTED_IDENTITY, "EXPANDED_IDENTITY_COUNT_INVALID")
    _require(expanded.get("variantCount") == EXPECTED_VARIANT, "EXPANDED_VARIANT_COUNT_INVALID")
    _require(expanded.get("rejectCount") == EXPECTED_REJECT, "EXPANDED_REJECT_COUNT_INVALID")
    _require(bundle.holdout.get("recordCount") == 6 and bundle.holdout.get("familyCount") == 3, "HOLDOUT_COUNT_INVALID")
    _require(bundle.holdout.get("frozen") is True and bundle.holdout.get("executed") is False, "HOLDOUT_STATE_INVALID")
    _require(bundle.holdout.get("modelExposureCount") == 0, "HOLDOUT_EXPOSURE_INVALID")
    _require(bundle.limitations.get("rejectBoundaryDiversity") == EXPECTED_REJECT_BOUNDARY_DIVERSITY, "LIMITATION_BOUNDARY_INVALID")
    _require(bundle.limitations.get("holdoutRejectCoverage") == 0, "LIMITATION_HOLDOUT_REJECT_INVALID")
    _require(bundle.boundary.get("boundaryTypeCount") == 2 and bundle.boundary.get("fabricatedCount") == 0, "BOUNDARY_AUTHORITY_INVALID")
    _require(bundle.expanded.get("familyMultiBucketAssignmentCount") == 0, "EXPANDED_FAMILY_ASSIGNMENT_INVALID")
    return bundle


@dataclass(frozen=True)
class ExpandedEvaluationExample:
    evaluation_example_reference: str
    human_review_unit_reference: str
    canonical_id: str
    canonical_name: str
    family_reference: str
    source: str
    source_record_reference: str
    source_evidence_reference: str
    source_evidence_authority_reference: str | None
    observed_term: str
    relation: str
    relation_authority_reference: str
    primary_target_authority_reference: str
    candidate_compatibility_authority_reference: str
    negative_boundary: str
    evaluation_bucket: str
    target_reference: str
    primary_target: str | None
    secondary_target: str
    primary_mask: int
    secondary_mask: int
    ordering_index: int

    @property
    def primary_active(self) -> bool:
        return self.primary_mask == 1

    @property
    def secondary_active(self) -> bool:
        return self.secondary_mask == 1

    @property
    def secondary_only(self) -> bool:
        return not self.primary_active

    def structural_identity(self) -> dict[str, Any]:
        return {
            "evaluationExampleReference": self.evaluation_example_reference,
            "humanReviewUnitReference": self.human_review_unit_reference,
            "canonicalId": self.canonical_id,
            "canonicalName": self.canonical_name,
            "familyReference": self.family_reference,
            "source": self.source,
            "sourceRecordReference": self.source_record_reference,
            "sourceEvidenceReference": self.source_evidence_reference,
            "sourceEvidenceAuthorityReference": self.source_evidence_authority_reference,
            "observedTerm": self.observed_term,
            "relation": self.relation,
            "relationAuthorityReference": self.relation_authority_reference,
            "primaryTargetAuthorityReference": self.primary_target_authority_reference,
            "candidateCompatibilityAuthorityReference": self.candidate_compatibility_authority_reference,
            "negativeBoundary": self.negative_boundary,
            "evaluationBucket": self.evaluation_bucket,
            "targetReference": self.target_reference,
            "primaryTarget": self.primary_target,
            "secondaryTarget": self.secondary_target,
            "primaryMask": self.primary_mask,
            "secondaryMask": self.secondary_mask,
            "orderingIndex": self.ordering_index,
        }


@dataclass(frozen=True)
class ExpandedEvaluationModelInput:
    """The target-free, model-visible batch for Expanded Validation."""

    input_ids: Any
    attention_mask: Any
    examples: tuple[ExpandedEvaluationExample, ...]


def _validate_evaluation_examples_for_execution(examples: Sequence[ExpandedEvaluationExample]) -> tuple[ExpandedEvaluationExample, ...]:
    resolved = tuple(examples)
    _require(len(resolved) == EXPECTED_TOTAL, "EXPANDED_EXECUTION_EXAMPLE_COUNT_INVALID")
    _require([item.ordering_index for item in resolved] == list(range(EXPECTED_TOTAL)), "EXPANDED_EXECUTION_ORDER_INVALID")
    _require(all(item.evaluation_bucket.startswith("EXPANDED_VALIDATION_") for item in resolved), "NON_EXPANDED_EXECUTION_RECORD")
    _require(all(item.secondary_active and item.secondary_mask == 1 for item in resolved), "SECONDARY_EXECUTION_MASK_INVALID")
    _require(all(item.primary_mask in (0, 1) for item in resolved), "PRIMARY_EXECUTION_MASK_INVALID")
    _require(all(item.primary_active or item.primary_target is None for item in resolved), "SECONDARY_ONLY_PRIMARY_TARGET_INVALID")
    _require(not any(item.evaluation_bucket == "FROZEN_HOLDOUT" for item in resolved), "HOLDOUT_EXECUTION_RECORD")
    return resolved


def build_evaluation_model_input(
    examples: Sequence[ExpandedEvaluationExample],
    tokenizer: Any,
) -> ExpandedEvaluationModelInput:
    """Project frozen evaluation examples using the training input semantics.

    This intentionally emits only input ids and attention masks. Targets and
    masks remain evaluator-owned and are never placed in the model input.
    """

    import torch

    resolved = _validate_evaluation_examples_for_execution(examples)
    encoded: list[list[int]] = []
    for item in resolved:
        result = tokenizer.encode(
            item.observed_term,
            f"CANDIDATE {item.canonical_name}",
            add_special_tokens=True,
        )
        ids = [int(value) for value in result.ids]
        _require(ids and len(ids) <= 128, "EVALUATION_TOKEN_SEQUENCE_INVALID")
        _require(ids[0] == 0 and ids[-1] == 2, "EVALUATION_SPECIAL_TOKEN_LAYOUT_INVALID")
        _require(1 not in ids, "EVALUATION_PADDING_BEFORE_COLLATION")
        encoded.append(ids)
    target_length = max(len(ids) for ids in encoded)
    input_ids = [ids + [1] * (target_length - len(ids)) for ids in encoded]
    attention_mask = [[1] * len(ids) + [0] * (target_length - len(ids)) for ids in encoded]
    return ExpandedEvaluationModelInput(
        input_ids=torch.tensor(input_ids, dtype=torch.int64),
        attention_mask=torch.tensor(attention_mask, dtype=torch.int64),
        examples=resolved,
    )


def _resolve_explicit_path(value: str | Path) -> Path:
    path = Path(value)
    return path if path.is_absolute() else Path.cwd() / path


def resolve_pinned_tokenizer_path_for_model_root(model_root: str | Path | None = None) -> Path:
    """Resolve the tokenizer beside the selected frozen model root.

    An explicit deployment root is authoritative for execution.  The
    repository-local Point-13 root is used only when no root is supplied,
    preserving the existing local entrypoint semantics.
    """

    if model_root is not None:
        return Path(model_root) / TOKENIZER_FILENAME
    from .point13_model_forward_v1 import MODEL_ROOT

    return MODEL_ROOT / TOKENIZER_FILENAME


def validate_evaluation_execution_binding(
    candidate_binding: Mapping[str, Any],
    runtime_binding: Mapping[str, Any],
) -> None:
    """Validate the packet's frozen candidate/checkpoint/runtime identity."""

    _require(candidate_binding.get("candidateReference") == EXPECTED_CANDIDATE_REFERENCE, "CANDIDATE_EXECUTION_BINDING_MISMATCH")
    _require(candidate_binding.get("checkpointReference") == EXPECTED_CHECKPOINT_REFERENCE, "CHECKPOINT_EXECUTION_BINDING_MISMATCH")
    _require(candidate_binding.get("checkpointLogicalDigest") == EXPECTED_CHECKPOINT_DIGEST, "CHECKPOINT_EXECUTION_DIGEST_MISMATCH")
    _require(candidate_binding.get("modelDeserializationThisMission") is False, "MODEL_DESERIALIZATION_AUTHORITY_INVALID")
    for field, expected in EXPECTED_RUNTIME_BINDING.items():
        _require(runtime_binding.get(field) == expected, f"RUNTIME_EXECUTION_BINDING_MISMATCH:{field}")


def _load_model_state_read_only(
    model: Any,
    manifest_path: Path,
    candidate_binding: Mapping[str, Any],
    guards: "EvaluationExecutionGuards | None" = None,
) -> Any:
    """Load only the verified model state; never constructs or loads an optimizer."""

    import torch

    manifest_sha = _sha256(manifest_path.read_bytes())
    _require(manifest_sha == candidate_binding.get("checkpointManifestSha256"), "CHECKPOINT_MANIFEST_DIGEST_MISMATCH")
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except Exception as error:
        raise ExpandedValidationContractError("CHECKPOINT_MANIFEST_JSON_INVALID") from error
    _require(isinstance(manifest, dict), "CHECKPOINT_MANIFEST_OBJECT_INVALID")
    _require(manifest.get("checkpointReference") == EXPECTED_CHECKPOINT_REFERENCE, "CHECKPOINT_REFERENCE_MISMATCH")
    _require(manifest.get("checkpointLogicalDigest") == EXPECTED_CHECKPOINT_DIGEST, "CHECKPOINT_DIGEST_MISMATCH")
    authority_bindings = manifest.get("authorityBindings")
    _require(isinstance(authority_bindings, Mapping), "CHECKPOINT_AUTHORITY_BINDINGS_INVALID")

    # Reuse the existing strict manifest and artifact-digest gate. It also
    # verifies optimizer metadata/files but does not deserialize optimizer
    # state; this adapter deliberately consumes only its model-path result.
    from .checkpoint_v2 import _strict_manifest_gate

    _, model_state_path, _optimizer_state_path = _strict_manifest_gate(
        manifest_path,
        authority_bindings,
        int(manifest.get("optimizerStep")),
        torch,
    )
    bound_model_path = _resolve_explicit_path(str(candidate_binding.get("modelStatePath")))
    _require(model_state_path.resolve() == bound_model_path.resolve(), "CHECKPOINT_MODEL_STATE_PATH_MISMATCH")
    try:
        model_state = torch.load(model_state_path, map_location="cpu", weights_only=True)
    except Exception as error:
        raise ExpandedValidationContractError("CHECKPOINT_MODEL_STATE_DESERIALIZATION_FAILED") from error
    _require(isinstance(model_state, Mapping), "CHECKPOINT_MODEL_STATE_INVALID")
    try:
        model.load_state_dict(model_state, strict=True)
    except Exception as error:
        raise ExpandedValidationContractError("CHECKPOINT_MODEL_STATE_RELOAD_FAILED") from error
    _require(not getattr(model, "training", True), "MODEL_EVAL_MODE_REQUIRED")
    if guards is not None:
        guards.checkpoint_model_state_load_count += 1
    return model


def load_frozen_evaluation_model(
    *,
    packet_root: str | Path,
    candidate_binding: Mapping[str, Any],
    runtime_binding: Mapping[str, Any],
    model_root: str | Path | None = None,
    checkpoint_manifest_path: str | Path | None = None,
    guards: "EvaluationExecutionGuards | None" = None,
) -> Any:
    """Load the exact frozen candidate model for evaluation-only execution."""

    validate_evaluation_execution_binding(candidate_binding, runtime_binding)
    import torch  # noqa: F401 - runtime guard is intentionally execution-only
    from .point13_model_forward_v1 import (
        MODEL_ROOT,
        build_him_model_execution_binding_v1,
        load_pinned_him_multi_head_model_from_root_v1,
        load_pinned_model_config_from_root_v1,
    )

    packet_root_path = Path(packet_root)
    _require(packet_root_path.is_dir() and not packet_root_path.is_symlink(), "PACKET_ROOT_INVALID")
    manifest_path = _resolve_explicit_path(
        checkpoint_manifest_path or str(candidate_binding.get("checkpointManifestPath")),
    )
    _require(manifest_path.is_file() and not manifest_path.is_symlink(), "CHECKPOINT_MANIFEST_INVALID")
    selected_model_root = Path(model_root) if model_root is not None else MODEL_ROOT
    config = load_pinned_model_config_from_root_v1(selected_model_root)
    model_binding = build_him_model_execution_binding_v1(config, 7, "7eb19b673b8176e7c54ee0ebaac565b61781af60daac696ccaf76ef9adacb6bb")
    model = load_pinned_him_multi_head_model_from_root_v1(
        model_binding,
        "7eb19b673b8176e7c54ee0ebaac565b61781af60daac696ccaf76ef9adacb6bb",
        7,
        selected_model_root,
        "cuda:0",
    )
    return _load_model_state_read_only(model, manifest_path, candidate_binding, guards)


def extract_evaluation_predictions(model: Any, model_input: ExpandedEvaluationModelInput) -> Mapping[str, Any]:
    """Call the existing two-head forward and expose its raw outputs."""

    import torch

    _require(not getattr(model, "training", True), "MODEL_EVAL_MODE_REQUIRED")
    device = getattr(model, "execution_device", None)
    if device is None:
        try:
            device = next(model.parameters()).device
        except (AttributeError, StopIteration) as error:
            raise ExpandedValidationContractError("MODEL_EXECUTION_DEVICE_UNAVAILABLE") from error
    output = model(model_input.input_ids.to(device), model_input.attention_mask.to(device))
    primary_logits = getattr(output, "primary_logits", None)
    secondary_logits = getattr(output, "secondary_logits", None)
    _require(isinstance(primary_logits, torch.Tensor) and isinstance(secondary_logits, torch.Tensor), "MODEL_HEAD_OUTPUT_INVALID")
    _require(tuple(primary_logits.shape) == (EXPECTED_TOTAL, 5), "PRIMARY_HEAD_OUTPUT_SHAPE_INVALID")
    _require(tuple(secondary_logits.shape) == (EXPECTED_TOTAL, 2), "SECONDARY_HEAD_OUTPUT_SHAPE_INVALID")
    return {
        "primaryLogits": primary_logits,
        "secondaryLogits": secondary_logits,
        "primaryPredictions": primary_logits.argmax(dim=1) + 1,
        "secondaryPredictions": secondary_logits.argmax(dim=1),
    }


def build_evaluation_authority_binding(bundle: AuthorityBundle) -> dict[str, dict[str, str]]:
    """Return the seven finalized authority identities used by the result."""

    return {
        name: {
            "reference": str(value["reference"]),
            "logicalDigest": str(value["logicalDigest"]),
        }
        for name, value in bundle.as_dict().items()
    }


def build_packet_binding(packet_root: str | Path) -> dict[str, Any]:
    """Bind a result to the exact packet manifest bytes that produced it."""

    root = Path(packet_root)
    manifest_path = root / "packet-manifest.v1.json"
    manifest = _packet_json(root, "packet-manifest.v1.json")
    return {
        "manifestReference": str(manifest["reference"]),
        "manifestLogicalDigest": str(manifest["logicalDigest"]),
        "manifestSha256": _sha256(manifest_path.read_bytes()),
        "fileCount": int(manifest["fileCount"]),
        "totalBytes": int(manifest["totalBytes"]),
    }


def _checkpoint_binding(candidate_binding: Mapping[str, Any]) -> dict[str, Any]:
    fields = (
        "checkpointReference",
        "checkpointLogicalDigest",
        "checkpointManifestPath",
        "checkpointManifestSha256",
        "modelStatePath",
    )
    return {field: candidate_binding[field] for field in fields if field in candidate_binding}


def _prediction_rows(value: Any, *, expected_rows: int, expected_width: int, code: str) -> list[list[float]]:
    rows = _json_value(value)
    _require(isinstance(rows, list) and len(rows) == expected_rows, f"{code}_ROW_COUNT_INVALID")
    for row in rows:
        _require(isinstance(row, list) and len(row) == expected_width, f"{code}_DIMENSION_INVALID")
        _require(all(isinstance(item, (int, float)) and not isinstance(item, bool) and math.isfinite(float(item)) for item in row), f"{code}_VALUE_INVALID")
    return rows


def _prediction_vector(value: Any, *, expected_rows: int, code: str) -> list[int]:
    values = _json_value(value)
    _require(isinstance(values, list) and len(values) == expected_rows, f"{code}_COUNT_INVALID")
    result: list[int] = []
    for item in values:
        _require(isinstance(item, int) and not isinstance(item, bool), f"{code}_VALUE_INVALID")
        result.append(int(item))
    return result


def _confusion_matrix(expected: Sequence[int], predicted: Sequence[int], class_codes: Mapping[str, int], class_order: Sequence[str]) -> dict[str, Any]:
    matrix = {target: {prediction: 0 for prediction in class_order} for target in class_order}
    if class_codes is PRIMARY_CLASS_CODES:
        code_to_name = dict(zip(range(1, len(class_order) + 1), class_order))
    else:
        code_to_name = dict(zip(range(len(class_order)), class_order))
    for actual, guess in zip(expected, predicted):
        _require(actual in code_to_name and guess in code_to_name, "RESULT_PREDICTION_CODE_INVALID")
        matrix[code_to_name[actual]][code_to_name[guess]] += 1
    return {"classOrder": list(class_order), "matrix": matrix}


def compute_evaluation_metrics(per_example_results: Sequence[Mapping[str, Any]]) -> dict[str, Any]:
    """Compute exact masked metrics from persisted per-example predictions."""

    primary = [item for item in per_example_results if item.get("primaryActive") is True]
    secondary = list(per_example_results)
    _require(len(primary) == EXPECTED_PRIMARY_ACTIVE, "PRIMARY_DENOMINATOR_INVALID")
    _require(len(secondary) == EXPECTED_SECONDARY_ACTIVE, "SECONDARY_DENOMINATOR_INVALID")

    primary_expected = [int(item["primaryExpected"]) for item in primary]
    primary_predicted = [int(item["primaryPredicted"]) for item in primary]
    secondary_expected = [int(item["secondaryExpected"]) for item in secondary]
    secondary_predicted = [int(item["secondaryPredicted"]) for item in secondary]
    primary_correct = sum(actual == guess for actual, guess in zip(primary_expected, primary_predicted))
    secondary_correct = sum(actual == guess for actual, guess in zip(secondary_expected, secondary_predicted))
    return {
        "primary": {
            "total": len(primary),
            "correct": primary_correct,
            "incorrect": len(primary) - primary_correct,
            "accuracy": primary_correct / len(primary),
            "confusionMatrix": _confusion_matrix(primary_expected, primary_predicted, PRIMARY_CLASS_CODES, PRIMARY_CLASS_ORDER),
        },
        "secondary": {
            "total": len(secondary),
            "correct": secondary_correct,
            "incorrect": len(secondary) - secondary_correct,
            "accuracy": secondary_correct / len(secondary),
            "confusionMatrix": _confusion_matrix(secondary_expected, secondary_predicted, SECONDARY_CLASS_CODES, SECONDARY_CLASS_ORDER),
        },
    }


def _result_execution_counters(counters: Mapping[str, Any]) -> dict[str, int]:
    names = (
        "modelDeserializationCount", "checkpointModelStateLoadCount", "checkpointOptimizerStateLoadCount",
        "forwardCount", "realExpandedValidationForwardCount", "predictionCount", "logitCount",
        "trainingCount", "retrainingCount", "backwardCount", "optimizerCreatedCount", "optimizerStateLoadCount",
        "optimizerStepCount", "schedulerStepCount", "parameterMutationCount", "checkpointMutationCount",
        "holdoutExposureCount",
    )
    source_names = {name: {
        "modelDeserializationCount": "model_deserialization_count", "checkpointModelStateLoadCount": "checkpoint_model_state_load_count",
        "checkpointOptimizerStateLoadCount": "checkpoint_optimizer_state_load_count", "forwardCount": "forward_count",
        "realExpandedValidationForwardCount": "real_expanded_validation_forward_count", "predictionCount": "prediction_count",
        "logitCount": "logit_count", "trainingCount": "training_count", "retrainingCount": "retraining_count",
        "backwardCount": "backward_count", "optimizerCreatedCount": "optimizer_created_count", "optimizerStateLoadCount": "optimizer_state_load_count",
        "optimizerStepCount": "optimizer_step_count", "schedulerStepCount": "scheduler_step_count", "parameterMutationCount": "parameter_mutation_count",
        "checkpointMutationCount": "checkpoint_mutation_count", "holdoutExposureCount": "holdout_model_exposure_count",
    }.get(name, "") for name in names}
    return {name: int(counters.get(source_names[name], 0)) for name in names}


def build_real_evaluation_result(
    *,
    examples: Sequence[ExpandedEvaluationExample],
    predictions: Mapping[str, Any],
    candidate_binding: Mapping[str, Any],
    runtime_binding: Mapping[str, Any],
    authority_binding: Mapping[str, Any],
    packet_binding: Mapping[str, Any],
    execution_counters: Mapping[str, Any],
) -> dict[str, Any]:
    """Build the immutable, target-bound result from evaluator outputs."""

    resolved = _validate_evaluation_examples_for_execution(examples)
    primary_logits = _prediction_rows(predictions.get("primaryLogits"), expected_rows=EXPECTED_TOTAL, expected_width=5, code="PRIMARY_LOGITS")
    secondary_logits = _prediction_rows(predictions.get("secondaryLogits"), expected_rows=EXPECTED_TOTAL, expected_width=2, code="SECONDARY_LOGITS")
    primary_predicted = _prediction_vector(predictions.get("primaryPredictions"), expected_rows=EXPECTED_TOTAL, code="PRIMARY_PREDICTION")
    secondary_predicted = _prediction_vector(predictions.get("secondaryPredictions"), expected_rows=EXPECTED_TOTAL, code="SECONDARY_PREDICTION")
    per_example: list[dict[str, Any]] = []
    for index, item in enumerate(resolved):
        primary_expected = None if not item.primary_active else PRIMARY_CLASS_CODES[item.primary_target]
        primary_guess = None if not item.primary_active else primary_predicted[index]
        per_example.append({
            "exampleId": item.evaluation_example_reference,
            "family": item.family_reference,
            "relation": item.relation,
            "targetReference": item.target_reference,
            "primaryTarget": item.primary_target,
            "primaryActive": item.primary_active,
            "primaryExpected": primary_expected,
            "primaryPredicted": primary_guess,
            "primaryLogits": primary_logits[index],
            "primaryCorrect": None if not item.primary_active else primary_guess == primary_expected,
            "secondaryExpected": SECONDARY_CLASS_CODES[item.secondary_target],
            "secondaryPredicted": secondary_predicted[index],
            "secondaryLogits": secondary_logits[index],
            "secondaryCorrect": secondary_predicted[index] == SECONDARY_CLASS_CODES[item.secondary_target],
            "candidateReference": candidate_binding["candidateReference"],
            "relationAuthorityReference": item.relation_authority_reference,
            "primaryTargetAuthorityReference": item.primary_target_authority_reference,
            "candidateCompatibilityAuthorityReference": item.candidate_compatibility_authority_reference,
            "expandedValidationAuthorityReference": authority_binding["expanded"]["reference"],
        })
    metrics = compute_evaluation_metrics(per_example)
    result = {
        "contractId": REAL_EVALUATION_RESULT_CONTRACT_ID,
        "schemaVersion": REAL_EVALUATION_RESULT_SCHEMA_VERSION,
        "state": "REAL_EVALUATION_COMPLETE",
        "createdFromPacket": dict(packet_binding),
        "candidateBinding": dict(candidate_binding),
        "checkpointBinding": _checkpoint_binding(candidate_binding),
        "runtimeBinding": dict(runtime_binding),
        "evaluationAuthorityBinding": dict(authority_binding),
        "packetBinding": dict(packet_binding),
        "exampleCount": len(per_example),
        "primaryEvaluatedCount": sum(item["primaryActive"] for item in per_example),
        "primaryInactiveCount": sum(not item["primaryActive"] for item in per_example),
        "secondaryEvaluatedCount": len(per_example),
        "perExampleResults": per_example,
        "metrics": metrics,
        "executionCounters": _result_execution_counters(execution_counters),
        "holdoutCounters": {
            "inputConstructionCount": 0, "tokenizationCount": 0, "modelDeserializationCount": 0,
            "forwardCount": 0, "predictionCount": 0, "logitCount": 0, "metricsCount": 0, "exposureCount": 0,
        },
    }
    semantic = _canonical(result)
    digest = _sha256(semantic)
    result["logicalDigest"] = digest
    result["resultIdentity"] = f"expanded-validation-result:v1:{digest}"
    return result


def _require_result_bindings(result: Mapping[str, Any], *, candidate_binding: Mapping[str, Any], runtime_binding: Mapping[str, Any], authority_binding: Mapping[str, Any], packet_binding: Mapping[str, Any]) -> None:
    _require(result.get("candidateBinding") == dict(candidate_binding), "RESULT_CANDIDATE_BINDING_MISMATCH")
    _require(result.get("checkpointBinding") == _checkpoint_binding(candidate_binding), "RESULT_CHECKPOINT_BINDING_MISMATCH")
    _require(result.get("runtimeBinding") == dict(runtime_binding), "RESULT_RUNTIME_BINDING_MISMATCH")
    _require(result.get("evaluationAuthorityBinding") == dict(authority_binding), "RESULT_AUTHORITY_BINDING_MISMATCH")
    _require(result.get("packetBinding") == dict(packet_binding), "RESULT_PACKET_BINDING_MISMATCH")
    _require(result.get("createdFromPacket") == dict(packet_binding), "RESULT_CREATED_FROM_PACKET_MISMATCH")


def validate_real_evaluation_result(
    result: Mapping[str, Any],
    *,
    examples: Sequence[ExpandedEvaluationExample],
    candidate_binding: Mapping[str, Any],
    runtime_binding: Mapping[str, Any],
    authority_binding: Mapping[str, Any],
    packet_binding: Mapping[str, Any],
) -> dict[str, Any]:
    """Fail-closed validation and metric recomputation for a persisted result."""

    _require(result.get("contractId") == REAL_EVALUATION_RESULT_CONTRACT_ID, "RESULT_SCHEMA_INVALID")
    _require(result.get("schemaVersion") == REAL_EVALUATION_RESULT_SCHEMA_VERSION, "RESULT_SCHEMA_VERSION_INVALID")
    _require(result.get("state") == "REAL_EVALUATION_COMPLETE", "RESULT_STATE_INVALID")
    _require_result_bindings(result, candidate_binding=candidate_binding, runtime_binding=runtime_binding, authority_binding=authority_binding, packet_binding=packet_binding)
    resolved = tuple(examples)
    records = result.get("perExampleResults")
    _require(isinstance(records, list), "RESULT_PER_EXAMPLE_INVALID")
    _require(result.get("exampleCount") == EXPECTED_TOTAL and len(records) == EXPECTED_TOTAL, "RESULT_EXAMPLE_COUNT_INVALID")
    expected_ids = [item.evaluation_example_reference for item in resolved]
    actual_ids = [item.get("exampleId") for item in records if isinstance(item, Mapping)]
    _require(len(actual_ids) == EXPECTED_TOTAL, "RESULT_PER_EXAMPLE_RECORD_INVALID")
    _require(len(set(actual_ids)) == EXPECTED_TOTAL, "RESULT_DUPLICATE_EXAMPLE")
    _require(actual_ids == expected_ids, "RESULT_EXAMPLE_MEMBERSHIP_INVALID")
    by_id = {item.evaluation_example_reference: item for item in resolved}
    for record in records:
        item = by_id[record["exampleId"]]
        active = bool(item.primary_active)
        _require(record.get("family") == item.family_reference and record.get("relation") == item.relation and record.get("targetReference") == item.target_reference, "RESULT_EXAMPLE_IDENTITY_MISMATCH")
        _require(record.get("primaryActive") is active, "RESULT_PRIMARY_MASK_MISMATCH")
        expected_primary = None if not active else PRIMARY_CLASS_CODES[item.primary_target]
        _require(record.get("primaryExpected") == expected_primary, "RESULT_PRIMARY_EXPECTATION_MISMATCH")
        _require(record.get("primaryPredicted") is None if not active else isinstance(record.get("primaryPredicted"), int), "RESULT_PRIMARY_PREDICTION_INVALID")
        _prediction_rows([record.get("primaryLogits")], expected_rows=1, expected_width=5, code="PRIMARY_LOGITS")
        _require(record.get("primaryCorrect") is None if not active else record.get("primaryCorrect") == (record.get("primaryPredicted") == expected_primary), "RESULT_PRIMARY_CORRECTNESS_INVALID")
        expected_secondary = SECONDARY_CLASS_CODES[item.secondary_target]
        _require(record.get("secondaryExpected") == expected_secondary and isinstance(record.get("secondaryPredicted"), int), "RESULT_SECONDARY_PREDICTION_INVALID")
        _prediction_rows([record.get("secondaryLogits")], expected_rows=1, expected_width=2, code="SECONDARY_LOGITS")
        _require(record.get("secondaryCorrect") == (record.get("secondaryPredicted") == expected_secondary), "RESULT_SECONDARY_CORRECTNESS_INVALID")
        _require(record.get("candidateReference") == candidate_binding["candidateReference"], "RESULT_EXAMPLE_CANDIDATE_BINDING_MISMATCH")
    _require(result.get("primaryEvaluatedCount") == EXPECTED_PRIMARY_ACTIVE, "RESULT_PRIMARY_COUNT_INVALID")
    _require(result.get("primaryInactiveCount") == EXPECTED_PRIMARY_INACTIVE, "RESULT_PRIMARY_INACTIVE_COUNT_INVALID")
    _require(result.get("secondaryEvaluatedCount") == EXPECTED_SECONDARY_ACTIVE, "RESULT_SECONDARY_COUNT_INVALID")
    counters = result.get("executionCounters")
    holdout = result.get("holdoutCounters")
    _require(isinstance(counters, Mapping) and isinstance(holdout, Mapping), "RESULT_COUNTERS_INVALID")
    _require(counters.get("modelDeserializationCount", 0) > 0, "RESULT_MODEL_NOT_DESERIALIZED")
    _require(counters.get("checkpointModelStateLoadCount", 0) > 0, "RESULT_CHECKPOINT_MODEL_STATE_NOT_LOADED")
    _require(counters.get("forwardCount", 0) > 0 and counters.get("realExpandedValidationForwardCount", 0) > 0, "RESULT_FORWARD_NOT_EXECUTED")
    _require(counters.get("predictionCount") == EXPECTED_TOTAL and counters.get("logitCount") == EXPECTED_TOTAL, "RESULT_PREDICTION_LOGIT_COUNT_INVALID")
    for name in ("checkpointOptimizerStateLoadCount", "trainingCount", "retrainingCount", "backwardCount", "optimizerCreatedCount", "optimizerStateLoadCount", "optimizerStepCount", "schedulerStepCount", "parameterMutationCount", "checkpointMutationCount", "holdoutExposureCount"):
        _require(counters.get(name) == 0, f"RESULT_NONZERO_SAFETY_COUNTER:{name}")
    for name in ("inputConstructionCount", "tokenizationCount", "modelDeserializationCount", "forwardCount", "predictionCount", "logitCount", "metricsCount", "exposureCount"):
        _require(holdout.get(name) == 0, f"RESULT_NONZERO_HOLDOUT_COUNTER:{name}")
    recomputed = compute_evaluation_metrics(records)
    _require(result.get("metrics") == recomputed, "RESULT_METRICS_MISMATCH")
    semantic = {key: value for key, value in result.items() if key not in ("logicalDigest", "resultIdentity")}
    digest = _sha256(_canonical(semantic))
    _require(result.get("logicalDigest") == digest and result.get("resultIdentity") == f"expanded-validation-result:v1:{digest}", "RESULT_IDENTITY_MISMATCH")
    return dict(result)


def persist_real_evaluation_result(output_root: str | Path, result: Mapping[str, Any]) -> Path:
    """Persist one immutable result file using the project's atomic writer."""

    root = _prepare_output_root(output_root)
    destination = root / REAL_EVALUATION_RESULT_FILENAME
    payload = _canonical(result) + b"\n"
    temporary: str | None = None
    try:
        fd, temporary = tempfile.mkstemp(prefix=f".{destination.name}.", suffix=".tmp", dir=str(root))
        with os.fdopen(fd, "wb") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, destination)
        temporary = None
    finally:
        if temporary is not None:
            try:
                os.unlink(temporary)
            except OSError:
                pass
    return destination


def reload_real_evaluation_result(path: str | Path, **validation: Any) -> dict[str, Any]:
    destination = Path(path)
    _require(destination.name == REAL_EVALUATION_RESULT_FILENAME and destination.is_file() and not destination.is_symlink(), "RESULT_FILE_INVALID")
    return validate_real_evaluation_result(_read_json(destination), **validation)


def _source_map(authority: Mapping[str, Any]) -> dict[str, Mapping[str, Any]]:
    records = authority.get("records")
    _require(isinstance(records, list), "AUTHORITY_RECORDS_INVALID")
    result: dict[str, Mapping[str, Any]] = {}
    for record in records:
        _require(isinstance(record, Mapping), "AUTHORITY_RECORD_INVALID")
        key = record.get("sourceRecordReference")
        _require(isinstance(key, str) and key and key not in result, "AUTHORITY_SOURCE_RECORD_DUPLICATE")
        result[key] = record
    return result


def resolve_expanded_examples(bundle: AuthorityBundle) -> tuple[ExpandedEvaluationExample, ...]:
    expanded_records = bundle.expanded.get("records")
    _require(isinstance(expanded_records, list) and len(expanded_records) == EXPECTED_TOTAL, "EXPANDED_RECORD_RESOLUTION_INVALID")
    relation_by_source = _source_map(bundle.relation)
    primary_by_source = _source_map(bundle.primary)
    compatibility_by_source = _source_map(bundle.compatibility)
    boundary_by_source = _source_map(bundle.boundary)
    examples: list[ExpandedEvaluationExample] = []
    seen_sources: set[str] = set()
    seen_examples: set[str] = set()
    for index, raw in enumerate(expanded_records):
        _require(isinstance(raw, Mapping), "EXPANDED_RECORD_INVALID")
        source_record = raw.get("sourceRecordReference")
        _require(isinstance(source_record, str) and source_record not in seen_sources, "EXPANDED_SOURCE_RECORD_DUPLICATE")
        seen_sources.add(source_record)
        evaluation_reference = raw.get("recordReference")
        _require(isinstance(evaluation_reference, str) and evaluation_reference not in seen_examples, "EXPANDED_EXAMPLE_REFERENCE_DUPLICATE")
        seen_examples.add(evaluation_reference)
        for field in ("reviewUnitReference", "canonicalId", "canonicalName", "familyReference", "source", "sourceEvidenceReference", "observedTerm"):
            _require(isinstance(raw.get(field), str) and bool(raw[field].strip()), f"EXPANDED_FIELD_INVALID:{field}")
        for mapping in (relation_by_source, primary_by_source, compatibility_by_source, boundary_by_source):
            _require(source_record in mapping, "PARENT_AUTHORITY_RECORD_MISSING")
        relation = relation_by_source[source_record]
        primary = primary_by_source[source_record]
        compatibility = compatibility_by_source[source_record]
        boundary = boundary_by_source[source_record]
        _require(raw.get("relation") == relation.get("relation"), "RELATION_BINDING_MISMATCH")
        _require(raw.get("relationAuthorityRecordReference") == relation.get("recordReference"), "RELATION_AUTHORITY_RECORD_BINDING_MISMATCH")
        _require(raw.get("candidateCompatibility") == compatibility.get("candidateCompatibility"), "COMPATIBILITY_BINDING_MISMATCH")
        _require(raw.get("candidateCompatibilityAuthorityRecordReference") == compatibility.get("recordReference"), "COMPATIBILITY_AUTHORITY_RECORD_BINDING_MISMATCH")
        _require(raw.get("negativeBoundary") == boundary.get("negativeBoundary"), "BOUNDARY_BINDING_MISMATCH")
        _require(raw.get("negativeBoundaryAuthorityRecordReference") == boundary.get("recordReference"), "BOUNDARY_AUTHORITY_RECORD_BINDING_MISMATCH")
        _require(raw.get("primaryTarget") == primary.get("primaryTarget"), "PRIMARY_BINDING_MISMATCH")
        _require(raw.get("primaryTargetAuthorityRecordReference") == primary.get("recordReference"), "PRIMARY_AUTHORITY_RECORD_BINDING_MISMATCH")
        _require(raw.get("secondaryActive", True) is True, "SECONDARY_ACTIVITY_INVALID")
        primary_active = bool(raw.get("primaryActive"))
        secondary_target = str(raw.get("candidateCompatibility"))
        _require(secondary_target in SECONDARY_UNIVERSE, "SECONDARY_TARGET_INVALID")
        _require(int(primary_active) == (1 if raw.get("primaryTarget") else 0), "PRIMARY_ACTIVITY_INVALID")
        if primary_active:
            _require(raw.get("primaryTarget") in ("IDENTITY", "VARIANT"), "EXPANDED_PRIMARY_UNIVERSE_INVALID")
            _require(raw.get("evaluationBucket") == f"EXPANDED_VALIDATION_{raw.get('primaryTarget')}", "EXPANDED_BUCKET_INVALID")
        else:
            _require(secondary_target == "REJECT" and raw.get("primaryTarget") is None, "SECONDARY_ONLY_TARGET_INVALID")
            _require(raw.get("evaluationBucket") == "EXPANDED_VALIDATION_REJECT", "EXPANDED_BUCKET_INVALID")
        examples.append(ExpandedEvaluationExample(
            evaluation_example_reference=evaluation_reference,
            human_review_unit_reference=str(raw["reviewUnitReference"]),
            canonical_id=str(raw["canonicalId"]),
            canonical_name=str(raw["canonicalName"]),
            family_reference=str(raw["familyReference"]),
            source=str(raw["source"]),
            source_record_reference=source_record,
            source_evidence_reference=str(raw["sourceEvidenceReference"]),
            source_evidence_authority_reference=None if raw.get("sourceEvidenceAuthorityReference") is None else str(raw["sourceEvidenceAuthorityReference"]),
            observed_term=str(raw["observedTerm"]),
            relation=str(raw["relation"]),
            relation_authority_reference=str(raw["relationAuthorityRecordReference"]),
            primary_target_authority_reference=str(raw["primaryTargetAuthorityRecordReference"]),
            candidate_compatibility_authority_reference=str(raw["candidateCompatibilityAuthorityRecordReference"]),
            negative_boundary=str(raw["negativeBoundary"]),
            evaluation_bucket=str(raw["evaluationBucket"]),
            target_reference=str(raw.get("targetReference") or raw.get("canonicalReference")),
            primary_target=None if raw.get("primaryTarget") is None else str(raw["primaryTarget"]),
            secondary_target=secondary_target,
            primary_mask=1 if primary_active else 0,
            secondary_mask=1,
            ordering_index=index,
        ))
    counts = {
        "total": len(examples),
        "primaryActive": sum(item.primary_active for item in examples),
        "primaryInactive": sum(item.secondary_only for item in examples),
        "secondaryActive": sum(item.secondary_active for item in examples),
        "identity": sum(item.primary_target == "IDENTITY" for item in examples),
        "variant": sum(item.primary_target == "VARIANT" for item in examples),
        "compatible": sum(item.secondary_target == "COMPATIBLE" for item in examples),
        "reject": sum(item.secondary_target == "REJECT" for item in examples),
    }
    _require(counts == {"total": 19, "primaryActive": 13, "primaryInactive": 6, "secondaryActive": 19, "identity": 6, "variant": 7, "compatible": 13, "reject": 6}, "EXPANDED_INPUT_COUNTS_INVALID")
    _require(len({item.evaluation_example_reference for item in examples}) == EXPECTED_TOTAL, "EXPANDED_EXAMPLE_REFERENCE_DUPLICATE")
    return tuple(examples)


def validate_holdout_exclusion(examples: Sequence[ExpandedEvaluationExample], holdout: Mapping[str, Any]) -> dict[str, int]:
    holdout_records = holdout.get("records")
    _require(isinstance(holdout_records, list), "HOLDOUT_RECORDS_INVALID")
    expanded_examples = {item.evaluation_example_reference for item in examples}
    expanded_sources = {item.source_record_reference for item in examples}
    expanded_families = {item.family_reference for item in examples}
    holdout_examples = {str(item.get("recordReference")) for item in holdout_records}
    holdout_sources = {str(item.get("sourceRecordReference")) for item in holdout_records}
    holdout_families = {str(item.get("familyReference")) for item in holdout_records}
    overlap = {
        "example": len(expanded_examples & holdout_examples),
        "sourceRecord": len(expanded_sources & holdout_sources),
        "family": len(expanded_families & holdout_families),
    }
    _require(not any(overlap.values()), "HOLDOUT_OVERLAP")
    _require(holdout.get("modelExposureCount") == 0, "HOLDOUT_MODEL_EXPOSURE")
    return overlap


def validate_historical_leakage(examples: Sequence[ExpandedEvaluationExample], *, train_examples: Sequence[str] = (), validation_examples: Sequence[str] = (), train_families: Sequence[str] = (), validation_families: Sequence[str] = (), train_source_records: Sequence[str] = (), validation_source_records: Sequence[str] = (), train_review_units: Sequence[str] = (), validation_review_units: Sequence[str] = ()) -> dict[str, int]:
    expanded_refs = {item.evaluation_example_reference for item in examples}
    expanded_sources = {item.source_record_reference for item in examples}
    expanded_families = {item.family_reference for item in examples}
    expanded_review_units = {item.human_review_unit_reference for item in examples}
    expanded_source_records = {item.source_record_reference for item in examples}
    train_refs, validation_refs = set(train_examples), set(validation_examples)
    train_family_set, validation_family_set = set(train_families), set(validation_families)
    result = {
        "historicalTrainExpandedExample": len(expanded_refs & train_refs),
        "historicalValidationExpandedExample": len(expanded_refs & validation_refs),
        "historicalTrainExpandedFamily": len(expanded_families & train_family_set),
        "historicalValidationExpandedFamily": len(expanded_families & validation_family_set),
        "historicalTrainExpandedSource": len(expanded_source_records & set(train_source_records)),
        "historicalValidationExpandedSource": len(expanded_source_records & set(validation_source_records)),
        "historicalTrainExpandedReviewUnit": len(expanded_review_units & set(train_review_units)),
        "historicalValidationExpandedReviewUnit": len(expanded_review_units & set(validation_review_units)),
    }
    _require(not any(result.values()), "HISTORICAL_EXPANDED_OVERLAP")
    return result


def _counts(examples: Sequence[ExpandedEvaluationExample]) -> dict[str, int]:
    return {
        "total": len(examples),
        "primaryActive": sum(item.primary_active for item in examples),
        "primaryInactive": sum(item.secondary_only for item in examples),
        "secondaryActive": sum(item.secondary_active for item in examples),
        "identity": sum(item.primary_target == "IDENTITY" for item in examples),
        "variant": sum(item.primary_target == "VARIANT" for item in examples),
        "compatible": sum(item.secondary_target == "COMPATIBLE" for item in examples),
        "reject": sum(item.secondary_target == "REJECT" for item in examples),
    }


def build_per_example_evidence(examples: Sequence[ExpandedEvaluationExample], *, candidate_reference: str, checkpoint_reference: str, expanded_authority_reference: str, limitations_reference: str, execution_reference: str) -> list[dict[str, Any]]:
    return [{
        **item.structural_identity(),
        "candidateReference": candidate_reference,
        "checkpointReference": checkpoint_reference,
        "expandedValidationAuthorityReference": expanded_authority_reference,
        "evaluationLimitationsAuthorityReference": limitations_reference,
        "executionReference": execution_reference,
        "primaryPrediction": "NOT_EXECUTED",
        "secondaryPrediction": "NOT_EXECUTED",
        "primaryCorrect": "NOT_EXECUTED" if item.primary_active else "NOT_APPLICABLE",
        "secondaryCorrect": "NOT_EXECUTED",
    } for item in examples]


def build_aggregate_evidence(examples: Sequence[ExpandedEvaluationExample], *, candidate_reference: str, checkpoint_reference: str, expanded_authority_reference: str, limitations_reference: str, execution_reference: str, leakage: Mapping[str, int], holdout_overlap: Mapping[str, int]) -> dict[str, Any]:
    def grouped(key: str) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for item in examples:
            group = getattr(item, key)
            value = result.setdefault(group, {"exampleCount": 0, "primaryActiveCount": 0, "primaryCorrectCount": "NOT_EXECUTED", "secondaryActiveCount": 0, "secondaryCorrectCount": "NOT_EXECUTED"})
            value["exampleCount"] += 1
            value["primaryActiveCount"] += int(item.primary_active)
            value["secondaryActiveCount"] += int(item.secondary_active)
        return result

    return {
        "contractId": "HIM_P2_EXPANDED_VALIDATION_EVIDENCE_V1",
        "version": 1,
        "state": "NOT_EXECUTED",
        "candidateReference": candidate_reference,
        "checkpointReference": checkpoint_reference,
        "expandedValidationAuthorityReference": expanded_authority_reference,
        "evaluationLimitationsAuthorityReference": limitations_reference,
        "executionReference": execution_reference,
        "counts": _counts(examples),
        "primaryMetrics": {"denominator": 13, "correct": "NOT_EXECUTED", "incorrect": "NOT_EXECUTED", "accuracy": "NOT_EXECUTED"},
        "primaryConfusion": {target: {prediction: "NOT_EXECUTED" for prediction in PRIMARY_UNIVERSE} for target in ("IDENTITY", "VARIANT")},
        "secondaryMetrics": {"denominator": 19, "correct": "NOT_EXECUTED", "incorrect": "NOT_EXECUTED", "accuracy": "NOT_EXECUTED"},
        "secondaryConfusion": {target: {prediction: "NOT_EXECUTED" for prediction in SECONDARY_UNIVERSE} for target in SECONDARY_UNIVERSE},
        "identityAnalysis": {"total": 6, "correct": "NOT_EXECUTED", "incorrect": "NOT_EXECUTED", "accuracy": "NOT_EXECUTED"},
        "variantAnalysis": {"total": 7, "correct": "NOT_EXECUTED", "incorrect": "NOT_EXECUTED", "accuracy": "NOT_EXECUTED"},
        "unseenRejectAnalysis": {"total": 6, "correct": "NOT_EXECUTED", "incorrect": "NOT_EXECUTED", "boundaryDiversity": "2/3", "thirdBoundaryFabricated": False},
        "familyEvidence": grouped("family_reference"),
        "sourceEvidence": grouped("source"),
        "leakage": dict(leakage),
        "holdoutOverlap": dict(holdout_overlap),
        "numericalValidation": {"modelDeserializationCount": 0, "forwardCount": 0, "gradientsEnabled": False},
    }


def build_dry_run(*, authority_root: str | Path, candidate_reference: str, checkpoint_reference: str, checkpoint_digest: str, execution_reference: str, historical_train_examples: Sequence[str] = (), historical_validation_examples: Sequence[str] = (), historical_train_families: Sequence[str] = (), historical_validation_families: Sequence[str] = (), historical_train_source_records: Sequence[str] = (), historical_validation_source_records: Sequence[str] = (), historical_train_review_units: Sequence[str] = (), historical_validation_review_units: Sequence[str] = ()) -> dict[str, Any]:
    bundle = load_authorities(authority_root)
    examples = resolve_expanded_examples(bundle)
    _require(candidate_reference == EXPECTED_CANDIDATE_REFERENCE, "CANDIDATE_REFERENCE_MISMATCH")
    _require(checkpoint_reference == EXPECTED_CHECKPOINT_REFERENCE, "CHECKPOINT_REFERENCE_MISMATCH")
    _require(checkpoint_digest == EXPECTED_CHECKPOINT_DIGEST, "CHECKPOINT_DIGEST_MISMATCH")
    holdout_overlap = validate_holdout_exclusion(examples, bundle.holdout)
    leakage = validate_historical_leakage(examples, train_examples=historical_train_examples, validation_examples=historical_validation_examples, train_families=historical_train_families, validation_families=historical_validation_families, train_source_records=historical_train_source_records, validation_source_records=historical_validation_source_records, train_review_units=historical_train_review_units, validation_review_units=historical_validation_review_units)
    per_example = build_per_example_evidence(examples, candidate_reference=candidate_reference, checkpoint_reference=checkpoint_reference, expanded_authority_reference=str(bundle.expanded["reference"]), limitations_reference=str(bundle.limitations["reference"]), execution_reference=execution_reference)
    aggregate = build_aggregate_evidence(examples, candidate_reference=candidate_reference, checkpoint_reference=checkpoint_reference, expanded_authority_reference=str(bundle.expanded["reference"]), limitations_reference=str(bundle.limitations["reference"]), execution_reference=execution_reference, leakage=leakage, holdout_overlap=holdout_overlap)
    payload = {"contractId": CONTRACT_ID, "version": VERSION, "state": "DRY_RUN_COMPLETE", "counts": _counts(examples), "orderedExampleReferences": [item.evaluation_example_reference for item in examples], "maskSemantics": {"secondaryOnly": 6, "fakePrimaryTargets": 0, "primaryActive": 13, "secondaryActive": 19}, "holdout": {"allowed": False, "overlap": holdout_overlap}, "leakage": leakage, "perExampleEvidence": per_example, "aggregateEvidence": aggregate, "executionCounters": {"modelDeserialization": 0, "forward": 0, "backward": 0, "optimizer": 0, "gpu": 0, "runpod": 0}}
    encoded = _canonical(payload)
    return {**payload, "logicalDigest": _sha256(encoded), "reference": f"p2-expanded-validation-dry-run:v1:{_sha256(encoded)}"}


def _packet_json(packet_root: Path, relative_path: str) -> dict[str, Any]:
    path = packet_root / relative_path
    _require(path.is_file() and not path.is_symlink(), f"PACKET_FILE_INVALID:{relative_path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except Exception as error:
        raise ExpandedValidationContractError(f"PACKET_JSON_INVALID:{relative_path}") from error
    _require(isinstance(value, dict), f"PACKET_OBJECT_INVALID:{relative_path}")
    return value


def _load_packet_context(packet_root: str | Path) -> tuple[AuthorityBundle, tuple[ExpandedEvaluationExample, ...], dict[str, Any], dict[str, Any]]:
    root = Path(packet_root)
    _require(root.is_dir() and not root.is_symlink(), "PACKET_ROOT_INVALID")
    manifest = _packet_json(root, "packet-manifest.v1.json")
    validate_packet_manifest(root, manifest)
    _require(packet_secret_scan(root) == 0, "PACKET_SECRET_SCAN_FAILED")

    readiness = _packet_json(root, "packet-readiness.v1.json")
    _require(readiness.get("state") == "READY", "PACKET_READINESS_INVALID")
    _require(readiness.get("packetPurpose") == "EVALUATION_ONLY", "PACKET_PURPOSE_INVALID")
    _require(readiness.get("evaluatorEntrypoint") == EVALUATOR_ENTRYPOINT, "PACKET_EVALUATOR_ENTRYPOINT_INVALID")
    _require(readiness.get("exampleCount") == EXPECTED_TOTAL, "PACKET_EXAMPLE_COUNT_INVALID")
    _require(readiness.get("executableHoldoutInputCount") == 0, "PACKET_HOLDOUT_INPUT_INVALID")
    _require(readiness.get("secretCount") == 0, "PACKET_SECRET_COUNT_INVALID")
    for field in ("authorityBindingReady", "candidateCheckpointBindingReady", "holdoutExclusionReady", "limitationsBindingReady", "runtimeBindingReady", "evaluatorReady"):
        _require(readiness.get(field) is True, f"PACKET_READINESS_FIELD_INVALID:{field}")

    bundle = load_authorities(root / "authority")
    examples = resolve_expanded_examples(bundle)
    candidate_binding = _packet_json(root, "candidate/frozen-candidate-checkpoint-binding.v1.json")
    _require(candidate_binding.get("candidateReference") == EXPECTED_CANDIDATE_REFERENCE, "CANDIDATE_BINDING_INVALID")
    _require(candidate_binding.get("checkpointReference") == EXPECTED_CHECKPOINT_REFERENCE, "CHECKPOINT_BINDING_INVALID")
    _require(candidate_binding.get("checkpointLogicalDigest") == EXPECTED_CHECKPOINT_DIGEST, "CHECKPOINT_DIGEST_BINDING_INVALID")
    _require(candidate_binding.get("modelDeserializationThisMission") is False, "MODEL_DESERIALIZATION_AUTHORITY_INVALID")

    runtime = _packet_json(root, "runtime/runtime-binding.v1.json")
    expected_runtime = PRODUCTIVE_V2_RUNTIME_BINDING if runtime.get("contentTag") == "ctx-eb0654be5f39" else EXPECTED_RUNTIME_BINDING
    for field, expected in expected_runtime.items():
        _require(runtime.get(field) == expected, f"RUNTIME_BINDING_INVALID:{field}")
    _require(runtime.get("evaluatorSource") == "training/him/src/him_trainer/expanded_validation_p2.py", "RUNTIME_EVALUATOR_SOURCE_INVALID")

    holdout_exclusion = _packet_json(root, "holdout/exclusion-binding.v1.json")
    _require(holdout_exclusion.get("frozen") is True, "HOLDOUT_FREEZE_INVALID")
    _require(holdout_exclusion.get("executableInputIncluded") is False, "HOLDOUT_EXECUTABLE_INPUT_INVALID")
    _require(holdout_exclusion.get("holdoutAuthorityReference") == EXPECTED_AUTHORITY_REFERENCES["holdout"], "HOLDOUT_AUTHORITY_BINDING_INVALID")
    _require(holdout_exclusion.get("holdoutLogicalDigest") == EXPECTED_AUTHORITY_REFERENCES["holdout"].rsplit(":", 1)[-1], "HOLDOUT_DIGEST_INVALID")
    validate_holdout_exclusion(examples, bundle.holdout)

    packet_input = _packet_json(root, "evaluation/expanded-validation-input.v1.json")
    _require(packet_input.get("authorityReference") == EXPECTED_AUTHORITY_REFERENCES["expanded"], "EVALUATION_AUTHORITY_BINDING_INVALID")
    _require(packet_input.get("recordCount") == EXPECTED_TOTAL, "EVALUATION_INPUT_COUNT_INVALID")
    packet_records = packet_input.get("records")
    _require(isinstance(packet_records, list), "EVALUATION_INPUT_RECORDS_INVALID")
    packet_sources = [record.get("sourceRecordReference") for record in packet_records if isinstance(record, Mapping)]
    _require(len(packet_sources) == len(packet_records), "EVALUATION_INPUT_RECORD_INVALID")
    _require(packet_sources == [item.source_record_reference for item in examples], "EVALUATION_INPUT_ORDER_INVALID")

    entrypoint = root / "runtime/ENTRYPOINT.txt"
    _require(entrypoint.is_file() and not entrypoint.is_symlink(), "PACKET_ENTRYPOINT_INVALID")
    _require(entrypoint.read_text(encoding="utf-8").splitlines()[:2] == [EVALUATOR_ENTRYPOINT, "mode=EVALUATION_ONLY"], "PACKET_ENTRYPOINT_BINDING_INVALID")
    execution = _packet_json(root, "execution/execution-command.v1.json")
    command = execution.get("command", "")
    _require(execution.get("inputAuthority") == EXPECTED_AUTHORITY_REFERENCES["expanded"], "EXECUTION_AUTHORITY_BINDING_INVALID")
    _require(execution.get("candidateReference") == EXPECTED_CANDIDATE_REFERENCE, "EXECUTION_CANDIDATE_BINDING_INVALID")
    _require(execution.get("checkpointReference") == EXPECTED_CHECKPOINT_REFERENCE, "EXECUTION_CHECKPOINT_BINDING_INVALID")
    for token in (f"-m {EVALUATOR_ENTRYPOINT}", "--packet-root", "--output-root", "--execute"):
        _require(token in command, f"EXECUTION_COMMAND_INVALID:{token}")
    return bundle, examples, candidate_binding, runtime


def _prepare_output_root(output_root: str | Path) -> Path:
    root = Path(output_root)
    _require(not root.is_symlink(), "OUTPUT_ROOT_SYMLINK")
    if root.exists():
        _require(root.is_dir(), "OUTPUT_ROOT_NOT_DIRECTORY")
        _require(not any(root.iterdir()), "OUTPUT_ROOT_NOT_EMPTY")
    else:
        root.mkdir(parents=True)
    return root


def _write_dry_run_output(output_root: Path, report: Mapping[str, Any]) -> Path:
    destination = output_root / PACKET_DRY_RUN_OUTPUT_NAME
    destination.write_bytes(_canonical(report) + b"\n")
    return destination


def run_cli(arguments: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="him_trainer.expanded_validation_p2")
    parser.add_argument("--packet-root", required=True)
    parser.add_argument("--output-root", required=True)
    parser.add_argument("--model-root")
    parser.add_argument("--checkpoint-manifest")
    parser.add_argument("--execute", action="store_true")
    args = parser.parse_args(list(arguments) if arguments is not None else None)
    try:
        bundle, examples, candidate_binding, runtime = _load_packet_context(args.packet_root)
        output_root = _prepare_output_root(args.output_root)
        if args.execute:
            guards = EvaluationExecutionGuards()
            tokenizer = None
            tokenizer_path = resolve_pinned_tokenizer_path_for_model_root(args.model_root)

            def input_builder(items: Sequence[ExpandedEvaluationExample]) -> ExpandedEvaluationModelInput:
                nonlocal tokenizer
                if tokenizer is None:
                    from .point12_token_tensor_builder_v1 import load_pinned_xlm_r_tokenizer_v1

                    tokenizer = load_pinned_xlm_r_tokenizer_v1(tokenizer_path)
                return build_evaluation_model_input(items, tokenizer)

            result = execute_evaluation(
                examples=examples,
                model_loader=lambda: load_frozen_evaluation_model(
                    packet_root=args.packet_root,
                    candidate_binding=candidate_binding,
                    runtime_binding=runtime,
                    model_root=args.model_root,
                    checkpoint_manifest_path=args.checkpoint_manifest,
                    guards=guards,
                ),
                input_builder=input_builder,
                prediction_fn=extract_evaluation_predictions,
                guards=guards,
            )
            packet_binding = build_packet_binding(args.packet_root)
            authority_binding = build_evaluation_authority_binding(bundle)
            execution_reference = f"p2-expanded-validation-execution:v1:{packet_binding['manifestLogicalDigest']}"
            report = build_real_evaluation_result(
                examples=examples,
                predictions=result["predictions"],
                candidate_binding=candidate_binding,
                runtime_binding=runtime,
                authority_binding=authority_binding,
                packet_binding=packet_binding,
                execution_counters=result["counters"],
            )
            destination = persist_real_evaluation_result(output_root, report)
            reload_real_evaluation_result(
                destination,
                examples=examples,
                candidate_binding=candidate_binding,
                runtime_binding=runtime,
                authority_binding=authority_binding,
                packet_binding=packet_binding,
            )
            sys.stdout.write(json.dumps({"state": report["state"], "resultIdentity": report["resultIdentity"], "output": str(destination), "counters": report["executionCounters"]}, sort_keys=True) + "\n")
            return 0
        report = build_dry_run(
            authority_root=Path(args.packet_root) / "authority",
            candidate_reference=str(candidate_binding["candidateReference"]),
            checkpoint_reference=str(candidate_binding["checkpointReference"]),
            checkpoint_digest=str(candidate_binding["checkpointLogicalDigest"]),
            execution_reference="p2-expanded-validation-execution:v1:CLI_DRY_RUN",
        )
        destination = _write_dry_run_output(output_root, report)
        sys.stdout.write(json.dumps({"state": report["state"], "reference": report["reference"], "output": str(destination)}, ensure_ascii=False, sort_keys=True) + "\n")
        return 0
    except (ExpandedValidationContractError, OSError, UnicodeError, ValueError) as error:
        sys.stderr.write(f"{type(error).__name__}: {error}\n")
        return 1


@dataclass
class EvaluationExecutionGuards:
    model_deserialization_count: int = 0
    checkpoint_model_state_load_count: int = 0
    checkpoint_optimizer_state_load_count: int = 0
    forward_count: int = 0
    real_expanded_validation_forward_count: int = 0
    prediction_count: int = 0
    logit_count: int = 0
    training_count: int = 0
    retraining_count: int = 0
    backward_count: int = 0
    optimizer_created_count: int = 0
    optimizer_state_load_count: int = 0
    optimizer_step_count: int = 0
    scheduler_step_count: int = 0
    training_state_advance_count: int = 0
    parameter_mutation_count: int = 0
    checkpoint_mutation_count: int = 0
    gpu_execution_count: int = 0
    holdout_model_input_construction_count: int = 0
    holdout_tokenization_count: int = 0
    holdout_forward_count: int = 0
    holdout_prediction_count: int = 0
    holdout_metric_count: int = 0
    holdout_model_exposure_count: int = 0

    def require_evaluation_only(self) -> None:
        _require(self.backward_count == 0, "BACKWARD_FORBIDDEN")
        _require(self.optimizer_step_count == 0, "OPTIMIZER_FORBIDDEN")
        _require(self.scheduler_step_count == 0, "SCHEDULER_FORBIDDEN")
        _require(self.training_state_advance_count == 0, "TRAINING_STATE_ADVANCE_FORBIDDEN")
        _require(self.parameter_mutation_count == 0, "PARAMETER_MUTATION_FORBIDDEN")
        _require(self.checkpoint_mutation_count == 0, "CHECKPOINT_MUTATION_FORBIDDEN")
        _require(self.gpu_execution_count == 0, "GPU_EXECUTION_FORBIDDEN")
        _require(self.holdout_model_input_construction_count == 0, "HOLDOUT_INPUT_CONSTRUCTION_FORBIDDEN")
        _require(self.holdout_tokenization_count == 0, "HOLDOUT_TOKENIZATION_FORBIDDEN")
        _require(self.holdout_forward_count == 0, "HOLDOUT_FORWARD_FORBIDDEN")
        _require(self.holdout_prediction_count == 0, "HOLDOUT_PREDICTION_FORBIDDEN")
        _require(self.holdout_metric_count == 0, "HOLDOUT_METRIC_FORBIDDEN")
        _require(self.holdout_model_exposure_count == 0, "HOLDOUT_EXPOSURE_FORBIDDEN")


def execute_evaluation(*, examples: Sequence[ExpandedEvaluationExample], model_loader: Callable[[], Any], input_builder: Callable[[Sequence[ExpandedEvaluationExample]], Any], prediction_fn: Callable[[Any, Any], Mapping[str, Any]], guards: EvaluationExecutionGuards | None = None) -> Mapping[str, Any]:
    """Future A100-only execution hook; never used by the local dry-run."""
    import torch

    state = guards or EvaluationExecutionGuards()
    state.require_evaluation_only()
    model = model_loader()
    state.model_deserialization_count += 1
    _require(not getattr(model, "training", True), "MODEL_EVAL_MODE_REQUIRED")
    model_input = input_builder(examples)
    with torch.no_grad():
        predictions = prediction_fn(model, model_input)
    state.forward_count += 1
    state.real_expanded_validation_forward_count += 1
    state.prediction_count += len(examples)
    state.logit_count += len(examples)
    state.require_evaluation_only()
    return {"predictions": predictions, "counters": state.__dict__.copy()}


def build_packet_manifest(packet_root: str | Path) -> dict[str, Any]:
    """Create a content manifest without reading or loading model weights."""
    root = Path(packet_root)
    _require(root.is_dir() and not root.is_symlink(), "PACKET_ROOT_INVALID")
    files = []
    for path in sorted(p for p in root.rglob("*") if p.is_file() and p.name != "packet-manifest.v1.json"):
        relative = path.relative_to(root).as_posix()
        data = path.read_bytes()
        files.append({"path": relative, "size": len(data), "sha256": _sha256(data)})
    payload = {"contractId": "HIM_P2_EXPANDED_VALIDATION_PACKET_MANIFEST_V1", "version": 1, "files": files}
    digest = _sha256(_canonical(payload))
    return {**payload, "fileCount": len(files), "totalBytes": sum(item["size"] for item in files), "logicalDigest": digest, "reference": f"p2-expanded-validation-packet-manifest:v1:{digest}"}


def validate_packet_manifest(packet_root: str | Path, manifest: Mapping[str, Any]) -> None:
    expected = build_packet_manifest(packet_root)
    _require(dict(manifest) == expected, "PACKET_MANIFEST_MISMATCH")


def packet_secret_scan(packet_root: str | Path) -> int:
    """Return secret-pattern matches; values are never printed."""
    patterns = (b"RUNPOD_" + b"API_KEY", b"HIM_REGISTRY_" + b"TOKEN", b"BEGIN " + b"PRIVATE KEY", b"gh" + b"p_")
    count = 0
    for path in Path(packet_root).rglob("*"):
        if path.is_file() and path.name != "packet-manifest.v1.json":
            data = path.read_bytes()
            count += sum(data.count(pattern) for pattern in patterns)
    return count


if __name__ == "__main__":
    raise SystemExit(run_cli())
