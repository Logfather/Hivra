"""Executable, packet-only Productive Training V2 boundary.

The module is deliberately split into two phases.  ``prepare_productive_v2``
parses and validates the frozen packet, resolves the authoritative input
fields, and builds CPU tensors and an execution plan.  ``execute_productive_v2``
is the later A100-only phase and is never entered by dry-run mode.

No semantic value is inferred here.  Every model-visible term is read either
from the V2 observed-term authority (the two negative examples) or from the
explicit semantic-input authority fields bound to the example.  A missing or
ambiguous binding fails closed.
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
from typing import Any, Callable, Mapping

from .point13_loss_contract_v1 import build_him_masked_multi_objective_loss_contract_v1
from .checkpoint_v2 import (
    CHECKPOINT_CONTRACT_ID,
    CheckpointRuntimeError,
    build_prepared_checkpoint_bindings,
    persist_checkpoint,
)

BASE_MODEL_ID = "FacebookAI/xlm-roberta-base"
MODEL_FAMILY_ID = "XLM-R"
MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
TOKENIZER_ID = "xlm-roberta-base-tokenizer"
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"
MAX_SEQUENCE_LENGTH = 128
PAD_TOKEN_ID = 1


MODE_PRODUCTIVE_TRAINING_V2 = "PRODUCTIVE_TRAINING_V2"
EXPECTED_EXAMPLE_COUNT = 40
EXPECTED_FAMILY_COUNT = 15
EXPECTED_TRAIN_COUNT = 32
EXPECTED_VALIDATION_COUNT = 6
EXPECTED_HOLDOUT_COUNT = 2
EXPECTED_COMPATIBLE_COUNT = 38
EXPECTED_REJECT_COUNT = 2
PRIMARY_TARGET_ENCODING = {
    "EXISTING_CANONICAL": 1,
    "IDENTITY": 2,
    "VARIANT": 3,
    "ALIAS": 4,
    "NEW_CANONICAL": 5,
}
SECONDARY_TARGET_ENCODING = {"COMPATIBLE": 0, "REJECT": 1}
QUALIFIER_FIELDS = ("variantLabel", "processingForm", "preparationState", "productForm")
PACKET_RELATIVE_FILES = {
    "observed": "authority/model-visible-observed-term-authority.v2.json",
    "input_authority": "authority/training-input-authority.v2.json",
    "examples": "dataset/complete-training-examples.v2.json",
    "corpus": "dataset/corpus.v2.json",
    "partition": "dataset/partition.v2.json",
    "manifest": "manifest/productive-training-manifest.v2.json",
    "evaluation_readiness": "readiness/evaluation-readiness.v2.json",
    "productive_readiness": "readiness/productive-training-readiness.v2.json",
    "request": "request/productive-training-request.v2.json",
}


class ProductiveTrainingV2Error(ValueError):
    """Fail-closed error for packet, projection, or execution authority."""


def _canonical(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")


def _sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def _require(condition: bool, code: str) -> None:
    if not condition:
        raise ProductiveTrainingV2Error(code)


def _read_json(path: Path) -> dict[str, Any]:
    _require(not path.is_symlink() and path.is_file(), f"PACKET_FILE_INVALID:{path.name}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"), parse_constant=lambda _: (_ for _ in ()).throw(ValueError()))
    except (OSError, UnicodeError, json.JSONDecodeError, ValueError) as error:
        raise ProductiveTrainingV2Error(f"PACKET_JSON_INVALID:{path.name}") from error
    _require(isinstance(value, dict), f"PACKET_OBJECT_REQUIRED:{path.name}")
    return value


def _text(value: Any, code: str) -> str:
    _require(isinstance(value, str) and bool(value), code)
    return value


def _int(value: Any, code: str) -> int:
    _require(isinstance(value, int) and not isinstance(value, bool), code)
    return value


def _bool(value: Any, code: str) -> bool:
    _require(isinstance(value, bool), code)
    return value


@dataclass(frozen=True)
class Packet:
    root: Path
    files: Mapping[str, dict[str, Any]]

    def __getitem__(self, name: str) -> dict[str, Any]:
        return self.files[name]


@dataclass(frozen=True)
class ProjectedExample:
    example_reference: str
    family_group_reference: str
    partition: str
    candidate_id: str
    candidate_name: str
    target_reference: str
    target_kind: str
    compatibility: str
    primary_target: int
    secondary_target: int
    secondary_mask: float
    observed_term: str
    relation: str
    semantic_fields: Mapping[str, str]
    human_authority_reference: str
    evidence_reference: str | None
    source: str

    @property
    def sequence_segments(self) -> tuple[tuple[str, str], tuple[str, str]]:
        return (
            ("OBSERVED_TERM", self.observed_term),
            ("COMPATIBILITY_SUBJECT", f"CANDIDATE {self.candidate_name}"),
        )


@dataclass(frozen=True)
class TensorView:
    input_ids: Any
    attention_mask: Any
    primary_target: Any
    secondary_target: Any
    primary_mask: Any
    secondary_mask: Any
    examples: tuple[ProjectedExample, ...]
    raw_sequence_lengths: tuple[int, ...]


@dataclass(frozen=True)
class PreparedProductiveV2:
    packet: Packet
    examples: tuple[ProjectedExample, ...]
    train: tuple[ProjectedExample, ...]
    validation: tuple[ProjectedExample, ...]
    holdout: tuple[ProjectedExample, ...]
    tensors: Mapping[str, TensorView]
    plan: dict[str, Any]


def load_packet(packet_root: str | Path, request_path: str | Path) -> Packet:
    root = Path(packet_root)
    _require(not root.is_symlink() and root.is_dir(), "PACKET_ROOT_INVALID")
    request = Path(request_path)
    _require(request.is_file() and not request.is_symlink(), "REQUEST_PATH_INVALID")
    try:
        request.relative_to(root)
    except ValueError as error:
        raise ProductiveTrainingV2Error("REQUEST_MUST_BE_INSIDE_PACKET") from error
    files: dict[str, dict[str, Any]] = {}
    for name, relative in PACKET_RELATIVE_FILES.items():
        files[name] = _read_json(root / relative)
    _require(files["request"] == _read_json(request), "REQUEST_BINDING_MISMATCH")
    return Packet(root=root, files=files)


def validate_startup(packet: Packet) -> None:
    """Validate only cross-bound authority; no model or CUDA probe is used."""

    observed, authority, examples, corpus, partition = (packet[x] for x in ("observed", "input_authority", "examples", "corpus", "partition"))
    manifest, eval_ready, ready, request = (packet[x] for x in ("manifest", "evaluation_readiness", "productive_readiness", "request"))
    _require(examples.get("version") == "2" and corpus.get("version") == "2" and partition.get("version") == "2", "DATA_VERSION_INVALID")
    _require(ready.get("state") == "READY" and ready.get("datasetReady") is True, "READINESS_NOT_READY")
    _require(len(examples.get("examples", [])) == EXPECTED_EXAMPLE_COUNT, "EXAMPLE_COUNT_INVALID")
    _require(len(examples.get("familyGroups", [])) == EXPECTED_FAMILY_COUNT, "FAMILY_COUNT_INVALID")
    _require(examples.get("logicalDigest") == manifest.get("completeTrainingExamplesLogicalDigest") == request.get("completeTrainingExamplesLogicalDigest") == ready.get("completeTrainingExamplesDigest"), "EXAMPLES_BINDING_MISMATCH")
    _require(corpus.get("logicalDigest") == manifest.get("corpusLogicalDigest") == request.get("corpusLogicalDigest") == ready.get("corpusLogicalDigest"), "CORPUS_BINDING_MISMATCH")
    _require(partition.get("logicalDigest") == manifest.get("partitionLogicalDigest") == request.get("partitionLogicalDigest") == ready.get("partitionLogicalDigest"), "PARTITION_BINDING_MISMATCH")
    _require(eval_ready.get("logicalDigest") == manifest.get("evaluationReadinessLogicalDigest") == ready.get("evaluationReadinessLogicalDigest"), "EVALUATION_READINESS_BINDING_MISMATCH")
    _require(authority.get("logicalDigest") == examples.get("trainingInputAuthorityLogicalDigest") == request.get("trainingInputAuthorityLogicalDigest") == ready.get("trainingInputAuthorityDigest"), "INPUT_AUTHORITY_BINDING_MISMATCH")
    _require(observed.get("logicalDigest") == examples.get("observedTermAuthorityLogicalDigest") == request.get("observedTermAuthority", {}).get("authorityLogicalDigest") == ready.get("observedTermAuthorityLogicalDigest"), "OBSERVED_TERM_AUTHORITY_BINDING_MISMATCH")
    _require(observed.get("reference") == examples.get("observedTermAuthorityReference") == ready.get("observedTermAuthorityReference"), "OBSERVED_TERM_REFERENCE_MISMATCH")
    _require(partition.get("trainExampleCount") == EXPECTED_TRAIN_COUNT and partition.get("validationExampleCount") == EXPECTED_VALIDATION_COUNT and partition.get("holdoutExampleCount") == EXPECTED_HOLDOUT_COUNT, "PARTITION_COUNTS_INVALID")
    _require(partition.get("familyLeakageCount") == 0 and partition.get("exampleLeakageCount") == 0 and partition.get("rngUsed") is False, "PARTITION_LEAKAGE_OR_RNG_INVALID")
    _require(authority.get("candidateCompatibilityAuthority", {}).get("pairCount") == EXPECTED_EXAMPLE_COUNT, "PAIR_COUNT_INVALID")
    _require(authority.get("candidateCompatibilityAuthority", {}).get("compatibleCount") == EXPECTED_COMPATIBLE_COUNT and authority.get("candidateCompatibilityAuthority", {}).get("rejectCount") == EXPECTED_REJECT_COUNT and authority.get("candidateCompatibilityAuthority", {}).get("unresolvedCount") == 0, "PAIR_DISTRIBUTION_INVALID")
    _require(authority.get("semanticInputAuthority", {}).get("requiredUnresolvedCount") == 0 and authority.get("semanticInputAuthority", {}).get("heuristicFieldCount") == 0, "SEMANTIC_INPUT_AUTHORITY_INVALID")
    _require(observed.get("bindingCount") == 2 and observed.get("heuristicObservedTermDerivationCount") == 0 and observed.get("candidateNameUsedAsObservedTerm") is False, "OBSERVED_TERM_AUTHORITY_INVALID")
    _require(manifest.get("checkpointAuthority") == "HIM_TRAINING_CHECKPOINT_CONTRACT_V1" and manifest.get("checkpointContractVersion") == "1", "CHECKPOINT_AUTHORITY_MISSING")
    _require(manifest.get("holdoutPolicy") == "EXCLUDED_FROM_TRAINING_VALIDATION_MODEL_SELECTION" and not request.get("holdoutTrainingEnabled") and not request.get("holdoutValidationEnabled") and not request.get("holdoutModelSelectionEnabled"), "HOLDOUT_POLICY_INVALID")
    _require(request.get("modelId") == BASE_MODEL_ID and request.get("modelRevision") == MODEL_REVISION and request.get("tokenizerId") == TOKENIZER_ID and request.get("tokenizerArtifactSha256") == TOKENIZER_SHA256, "RUNTIME_BINDING_INVALID")
    _require(request.get("sequenceLength") == "128" and request.get("trainingSeed") == "7" and request.get("trainingSteps") == "12" and request.get("epochs") == "3", "TRAINING_TRAJECTORY_INVALID")
    _require(request.get("validationPolicy") == "EACH_EPOCH_AND_FINAL" and request.get("gradientAccumulationSteps") == "1", "TRAINING_CONFIGURATION_INVALID")
    _require(ready.get("checkpointAuthorityPass") is True and ready.get("modelAuthorityPass") is True and ready.get("tokenizerAuthorityPass") is True and ready.get("runtimeAuthorityPass") is True and ready.get("trainingConfigurationAuthorityPass") is True, "READINESS_AUTHORITY_INVALID")


def _semantic_field_map(authority: dict[str, Any]) -> dict[str, dict[str, str]]:
    records = authority.get("semanticInputAuthority", {}).get("records", [])
    result: dict[str, dict[str, str]] = {}
    for record in records:
        reference = _text(record.get("exampleReference"), "SEMANTIC_EXAMPLE_REFERENCE_INVALID")
        fields = record.get("fields")
        _require(isinstance(fields, dict), "SEMANTIC_FIELDS_INVALID")
        result[reference] = {str(k): _text(v, "SEMANTIC_FIELD_VALUE_INVALID") for k, v in fields.items()}
    return result


def resolve_observed_term(example: dict[str, Any], semantic_fields: Mapping[str, str], observed_bindings: Mapping[str, dict[str, Any]]) -> tuple[str, dict[str, str]]:
    reference = _text(example.get("exampleReference"), "EXAMPLE_REFERENCE_INVALID")
    if example.get("candidateCompatibility") == "REJECT":
        binding = observed_bindings.get(reference)
        _require(binding is not None, "NEGATIVE_OBSERVED_TERM_BINDING_MISSING")
        _require(binding.get("observedTerm") == "Brie double crème", "NEGATIVE_OBSERVED_TERM_INVALID")
        _require(binding.get("candidateCompatibility") == "REJECT" and binding.get("secondaryTarget") == 1 and binding.get("secondaryMask") == 1, "NEGATIVE_AUTHORITY_INVALID")
        return _text(binding.get("observedTerm"), "OBSERVED_TERM_INVALID"), {}
    fields = dict(semantic_fields)
    if not fields:
        pairs = (("semanticLabel", example.get("semanticLabel")),) + tuple((key, example.get(key)) for key in QUALIFIER_FIELDS)
        fields = {key: value for key, value in pairs if value is not None}
    semantic_label = fields.get("semanticLabel")
    _require(isinstance(semantic_label, str) and semantic_label, "POSITIVE_SEMANTIC_LABEL_MISSING")
    qualifiers = [fields[key] for key in QUALIFIER_FIELDS if fields.get(key)]
    _require(len(qualifiers) <= 1, "POSITIVE_SEMANTIC_FIELDS_AMBIGUOUS")
    # This is an authority-defined projection, not a lexical/semantic guess:
    # an explicit confirmed qualifier is the observed term; otherwise the
    # confirmed semantic label is the observed term.
    term = qualifiers[0] if qualifiers else semantic_label
    return _text(term, "OBSERVED_TERM_INVALID"), fields


def project_examples(packet: Packet) -> tuple[ProjectedExample, ...]:
    examples = packet["examples"].get("examples")
    _require(isinstance(examples, list), "EXAMPLES_ARRAY_INVALID")
    semantic = _semantic_field_map(packet["input_authority"])
    observed = {item["exampleReference"]: item for item in packet["observed"].get("bindings", [])}
    projected: list[ProjectedExample] = []
    for item in examples:
        _require(isinstance(item, dict), "EXAMPLE_OBJECT_INVALID")
        reference = _text(item.get("exampleReference"), "EXAMPLE_REFERENCE_INVALID")
        observed_term, fields = resolve_observed_term(item, semantic.get(reference, {}), observed)
        target_kind = _text(item.get("targetKind"), "TARGET_KIND_INVALID")
        compatibility = _text(item.get("candidateCompatibility"), "COMPATIBILITY_INVALID")
        _require(target_kind in PRIMARY_TARGET_ENCODING and compatibility in SECONDARY_TARGET_ENCODING, "TARGET_DOMAIN_INVALID")
        _require(item.get("secondaryTarget") == SECONDARY_TARGET_ENCODING[compatibility], "SECONDARY_TARGET_NOT_AUTHORIZED")
        _require(item.get("secondaryMask") == 1 and item.get("secondaryObjectiveActive") is True, "SECONDARY_MASK_NOT_AUTHORIZED")
        projected.append(ProjectedExample(
            example_reference=reference,
            family_group_reference=_text(item.get("familyGroupReference"), "FAMILY_REFERENCE_INVALID"),
            partition="",
            candidate_id=_text(item.get("candidateId"), "CANDIDATE_ID_INVALID"),
            candidate_name=_text(item.get("candidateName"), "CANDIDATE_NAME_INVALID"),
            target_reference=_text(item.get("targetCanonicalId"), "TARGET_REFERENCE_INVALID"),
            target_kind=target_kind,
            compatibility=compatibility,
            primary_target=PRIMARY_TARGET_ENCODING[target_kind],
            secondary_target=int(item["secondaryTarget"]),
            secondary_mask=float(item["secondaryMask"]),
            observed_term=observed_term,
            relation=_text(item.get("relation"), "RELATION_INVALID"),
            semantic_fields=fields,
            human_authority_reference=_text(item.get("humanTrainingInputAuthorityReference"), "HUMAN_AUTHORITY_REFERENCE_INVALID"),
            evidence_reference=item.get("evidenceReferenceId"),
            source=_text(item.get("source"), "SOURCE_INVALID"),
        ))
    _require(len(projected) == EXPECTED_EXAMPLE_COUNT, "PROJECTED_EXAMPLE_COUNT_INVALID")
    _require(sum(item.compatibility == "COMPATIBLE" for item in projected) == EXPECTED_COMPATIBLE_COUNT and sum(item.compatibility == "REJECT" for item in projected) == EXPECTED_REJECT_COUNT, "PROJECTED_COMPATIBILITY_INVALID")
    _require(sum(item.observed_term == "Brie double crème" for item in projected if item.compatibility == "REJECT") == 2, "PROJECTED_NEGATIVE_TERM_INVALID")
    return tuple(projected)


def apply_frozen_partition(packet: Packet, examples: tuple[ProjectedExample, ...]) -> tuple[tuple[ProjectedExample, ...], tuple[ProjectedExample, ...], tuple[ProjectedExample, ...]]:
    assignments = packet["partition"].get("exampleAssignments")
    _require(isinstance(assignments, list) and len(assignments) == EXPECTED_EXAMPLE_COUNT, "PARTITION_ASSIGNMENTS_INVALID")
    by_reference = {item.example_reference: item for item in examples}
    seen: set[str] = set()
    views: dict[str, list[ProjectedExample]] = {"TRAIN": [], "VALIDATION": [], "HOLDOUT": []}
    for assignment in assignments:
        reference = _text(assignment.get("exampleReference"), "PARTITION_EXAMPLE_REFERENCE_INVALID")
        partition = _text(assignment.get("partition"), "PARTITION_NAME_INVALID")
        _require(partition in views and reference in by_reference and reference not in seen, "PARTITION_ASSIGNMENT_MISMATCH")
        item = by_reference[reference]
        _require(assignment.get("familyGroupReference") == item.family_group_reference and assignment.get("candidateId") == item.candidate_id, "PARTITION_LINEAGE_MISMATCH")
        views[partition].append(ProjectedExample(**{**item.__dict__, "partition": partition}))
        seen.add(reference)
    _require(len(seen) == EXPECTED_EXAMPLE_COUNT and len(views["TRAIN"]) == EXPECTED_TRAIN_COUNT and len(views["VALIDATION"]) == EXPECTED_VALIDATION_COUNT and len(views["HOLDOUT"]) == EXPECTED_HOLDOUT_COUNT, "FROZEN_PARTITION_COUNTS_INVALID")
    return tuple(views["TRAIN"]), tuple(views["VALIDATION"]), tuple(views["HOLDOUT"])


def load_tokenizer(path: str | Path) -> Tokenizer:
    from tokenizers import Tokenizer

    tokenizer_path = Path(path)
    _require(not tokenizer_path.is_symlink() and tokenizer_path.is_file() and tokenizer_path.name == "tokenizer.json", "TOKENIZER_ARTIFACT_INVALID")
    _require(_sha256(tokenizer_path.read_bytes()) == TOKENIZER_SHA256, "TOKENIZER_DIGEST_MISMATCH")
    try:
        tokenizer = Tokenizer.from_file(str(tokenizer_path))
    except Exception as error:
        raise ProductiveTrainingV2Error("TOKENIZER_LOAD_FAILED") from error
    tokenizer.no_truncation()
    _require(tokenizer.token_to_id("<pad>") == PAD_TOKEN_ID and tokenizer.token_to_id("<s>") == 0 and tokenizer.token_to_id("</s>") == 2, "TOKENIZER_SPECIAL_TOKEN_INVALID")
    return tokenizer


def _tensor_view(items: tuple[ProjectedExample, ...], tokenizer: Tokenizer) -> TensorView:
    import torch

    _require(items, "EMPTY_SPLIT")
    encodings = []
    for item in items:
        try:
            encoding = tokenizer.encode(item.sequence_segments[0][1], item.sequence_segments[1][1], add_special_tokens=True)
        except Exception as error:
            raise ProductiveTrainingV2Error("TOKENIZATION_FAILED") from error
        ids = [int(value) for value in encoding.ids]
        _require(ids and len(ids) <= MAX_SEQUENCE_LENGTH and PAD_TOKEN_ID not in ids, "TOKEN_SEQUENCE_INVALID")
        _require(all(int(value) == 0 for value in encoding.type_ids) and all(int(value) == 1 for value in encoding.attention_mask), "TOKEN_SEQUENCE_AUTHORITY_INVALID")
        _require(ids[0] == 0 and ids[-1] == 2, "TOKEN_SPECIAL_TOKEN_LAYOUT_INVALID")
        encodings.append(ids)
    target_length = max(map(len, encodings))
    input_ids = [ids + [PAD_TOKEN_ID] * (target_length - len(ids)) for ids in encodings]
    attention = [[1] * len(ids) + [0] * (target_length - len(ids)) for ids in encodings]
    view = TensorView(
        input_ids=torch.tensor(input_ids, dtype=torch.int64, device="cpu"),
        attention_mask=torch.tensor(attention, dtype=torch.int64, device="cpu"),
        primary_target=torch.tensor([item.primary_target for item in items], dtype=torch.int64, device="cpu"),
        secondary_target=torch.tensor([item.secondary_target for item in items], dtype=torch.int64, device="cpu"),
        primary_mask=torch.ones((len(items),), dtype=torch.float32, device="cpu"),
        secondary_mask=torch.tensor([item.secondary_mask for item in items], dtype=torch.float32, device="cpu"),
        examples=items,
        raw_sequence_lengths=tuple(map(len, encodings)),
    )
    _require(tuple(view.input_ids.shape) == (len(items), target_length) and tuple(view.attention_mask.shape) == (len(items), target_length), "TENSOR_INPUT_SHAPE_INVALID")
    _require(tuple(view.primary_target.shape) == (len(items),) and tuple(view.secondary_target.shape) == (len(items),) and tuple(view.secondary_mask.shape) == (len(items),), "TENSOR_TARGET_SHAPE_INVALID")
    _require(view.input_ids.dtype == torch.int64 and view.attention_mask.dtype == torch.int64 and view.primary_target.dtype == torch.int64 and view.secondary_target.dtype == torch.int64 and view.primary_mask.dtype == torch.float32 and view.secondary_mask.dtype == torch.float32, "TENSOR_DTYPE_INVALID")
    return view


def build_execution_plan(packet: Packet, examples: tuple[ProjectedExample, ...], tensors: Mapping[str, TensorView]) -> dict[str, Any]:
    request = packet["request"]
    plan_identity = {
        "mode": MODE_PRODUCTIVE_TRAINING_V2,
        "request": {"reference": request.get("requestReference"), "logicalDigest": request.get("logicalDigest")},
        "manifest": {"reference": packet["manifest"].get("manifestReference"), "logicalDigest": packet["manifest"].get("logicalDigest")},
        "readiness": {"reference": packet["productive_readiness"].get("readinessReference"), "logicalDigest": packet["productive_readiness"].get("logicalDigest")},
        "trainingInputAuthority": {"reference": packet["input_authority"].get("authorityReference"), "logicalDigest": packet["input_authority"].get("logicalDigest")},
        "observedTermAuthority": {"reference": packet["observed"].get("reference"), "logicalDigest": packet["observed"].get("logicalDigest")},
        "datasets": {name: {"logicalDigest": packet[name].get("logicalDigest")} for name in ("examples", "corpus", "partition", "evaluation_readiness")},
        "runtime": {"modelId": request.get("modelId"), "modelRevision": request.get("modelRevision"), "tokenizerId": request.get("tokenizerId"), "tokenizerSha256": request.get("tokenizerArtifactSha256"), "device": request.get("cudaDevice")},
        "counts": {"examples": len(examples), "families": EXPECTED_FAMILY_COUNT, "train": len(tensors["TRAIN"].examples), "validation": len(tensors["VALIDATION"].examples), "holdout": len(tensors["HOLDOUT"].examples), "compatible": EXPECTED_COMPATIBLE_COUNT, "reject": EXPECTED_REJECT_COUNT},
        "tensorShapes": {name: {"inputIds": list(view.input_ids.shape), "attentionMask": list(view.attention_mask.shape), "primaryTarget": list(view.primary_target.shape), "secondaryTarget": list(view.secondary_target.shape), "secondaryMask": list(view.secondary_mask.shape), "device": "cpu", "dtypes": {"inputIds": str(view.input_ids.dtype), "attentionMask": str(view.attention_mask.dtype), "primaryTarget": str(view.primary_target.dtype), "secondaryTarget": str(view.secondary_target.dtype), "secondaryMask": str(view.secondary_mask.dtype)}} for name, view in tensors.items()},
        "trainingConfiguration": {key: request.get(key) for key in ("optimizerId", "learningRate", "gradientAccumulationSteps", "trainingSeed", "epochs", "trainingSteps", "microBatchSize", "validationPolicy")},
        "trajectory": {"epochs": request.get("epochs"), "trainingSteps": request.get("trainingSteps"), "microBatchSize": request.get("microBatchSize"), "validationPolicy": request.get("validationPolicy")},
        "checkpoint": {"contract": packet["manifest"].get("checkpointAuthority"), "version": packet["manifest"].get("checkpointContractVersion")},
        "checkpointRuntime": {"contract": CHECKPOINT_CONTRACT_ID, "version": "1", "stateSerialization": "PYTORCH_TORCH_SAVE_ZIP", "manifestLast": True, "dryRunCreatesState": False},
        "runEvidence": {"contract": "HIM_P1_PRODUCTIVE_TRAINING_RUN_EVIDENCE_V2", "version": "2"},
        "holdout": {"training": False, "validation": False, "modelSelection": False},
    }
    logical_digest = _sha256(_canonical(plan_identity))
    return {"contractId": "HIM_P1_PRODUCTIVE_TRAINING_EXECUTION_PLAN_V2", "version": "2", "state": "EXECUTION_PLAN_READY", "reference": f"productive-training-execution-plan:v2:{logical_digest}", "logicalDigest": logical_digest, "identity": plan_identity}


def prepare_productive_v2(packet_root: str | Path, request_path: str | Path, tokenizer_path: str | Path) -> PreparedProductiveV2:
    packet = load_packet(packet_root, request_path)
    validate_startup(packet)
    examples = project_examples(packet)
    train, validation, holdout = apply_frozen_partition(packet, examples)
    tokenizer = load_tokenizer(tokenizer_path)
    tensors = {name: _tensor_view(items, tokenizer) for name, items in (("TRAIN", train), ("VALIDATION", validation), ("HOLDOUT", holdout))}
    plan = build_execution_plan(packet, examples, tensors)
    return PreparedProductiveV2(packet, examples, train, validation, holdout, tensors, plan)


def _finite_tensors(values: Mapping[str, torch.Tensor]) -> None:
    import torch

    for name, value in values.items():
        _require(bool(torch.isfinite(value).all().item()), f"NON_FINITE:{name}")


def atomic_json_write(path: str | Path, value: object) -> None:
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    payload = _canonical(value) + b"\n"
    fd, temporary = tempfile.mkstemp(prefix=f".{destination.name}.", suffix=".tmp", dir=str(destination.parent))
    try:
        with os.fdopen(fd, "wb") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary, destination)
    except Exception:
        try:
            os.unlink(temporary)
        except OSError:
            pass
        raise


def reload_json(path: str | Path) -> dict[str, Any]:
    return _read_json(Path(path))


def run_evidence_payload(prepared: PreparedProductiveV2, run_reference: str, terminal_state: str, checkpoint: Mapping[str, Any] | None, failure: str | None = None, training_metrics: list[dict[str, Any]] | None = None) -> dict[str, Any]:
    checkpoint_reload = checkpoint.get("reload", {}) if checkpoint is not None else {"passed": False}
    payload = {"contractId": "HIM_P1_PRODUCTIVE_TRAINING_RUN_EVIDENCE_V2", "version": "2", "state": terminal_state, "runReference": run_reference, "requestDigest": prepared.packet["request"]["logicalDigest"], "manifestDigest": prepared.packet["manifest"]["logicalDigest"], "trainingInputAuthorityDigest": prepared.packet["input_authority"]["logicalDigest"], "observedTermAuthorityDigest": prepared.packet["observed"]["logicalDigest"], "examplesDigest": prepared.packet["examples"]["logicalDigest"], "corpusDigest": prepared.packet["corpus"]["logicalDigest"], "partitionDigest": prepared.packet["partition"]["logicalDigest"], "executionPlanReference": prepared.plan["reference"], "runtime": {"device": prepared.packet["request"]["cudaDevice"], "modelId": prepared.packet["request"]["modelId"], "modelRevision": prepared.packet["request"]["modelRevision"], "tokenizerId": prepared.packet["request"]["tokenizerId"]}, "trajectory": prepared.plan["identity"]["trajectory"], "holdoutAccess": {"training": False, "validation": False, "modelSelection": False}, "checkpoint": checkpoint, "reloadValidation": {"required": True, **checkpoint_reload}, "numericalStability": {"finitenessGuards": True}, "failure": failure}
    payload["trainingMetrics"] = training_metrics or []
    payload["logicalDigest"] = _sha256(_canonical(payload))
    payload["reference"] = f"productive-training-run-evidence:v2:{payload['logicalDigest']}"
    return payload


def execute_productive_v2(prepared: PreparedProductiveV2, output_root: str | Path, model_loader: Callable[[], Any] | None = None) -> dict[str, Any]:
    """Execute the real path only with explicit external model authority.

    The callable is intentionally injected: it prevents a repository-local
    model fallback and lets the A100 packet bind the immutable model root at
    the deployment boundary.  No caller can reach this function from dry-run.
    """
    _require(model_loader is not None, "EXPLICIT_MODEL_BINDING_REQUIRED")
    import torch
    from .a100_validation_v1 import probe_cuda, validate_a100_facts
    from .point13_loss_v1 import compute_him_masked_multi_objective_loss_v1
    from .point13_optimizer_construction_v1 import construct_him_adamw_v1
    from .point13_optimizer_execution_policy_v1 import build_him_optimizer_execution_policy_v1
    from .point13_trainability_policy_v1 import build_him_base_encoder_trainability_policy_v1
    from .point13_trainability_projection_v1 import project_him_trainability_policy_v1
    output = Path(output_root)
    run_reference = f"productive-training-run:v2:{prepared.plan['logicalDigest']}"
    checkpoint = None
    try:
        facts = probe_cuda(torch)
        validation = validate_a100_facts(facts)
        _require(validation.passed, f"A100_RUNTIME_INVALID:{validation.reason}")
        model = model_loader()
        _require(model is not None, "MODEL_LOAD_FAILED")
        model.train()
        policy = build_him_optimizer_execution_policy_v1(device_policy="CUDA", gradient_accumulation_steps=1)
        trainability = build_him_base_encoder_trainability_policy_v1()
        projection = project_him_trainability_policy_v1(model, trainability)
        optimizer, _ = construct_him_adamw_v1(model, policy, projection)
        loss_contract = build_him_masked_multi_objective_loss_contract_v1()
        optimizer_step = 0
        metrics: list[dict[str, Any]] = []

        def validate_epoch(epoch: int) -> None:
            model.eval()
            view = prepared.tensors["VALIDATION"]
            with torch.no_grad():
                forward = model(view.input_ids.to(device), view.attention_mask.to(device))
                losses = compute_him_masked_multi_objective_loss_v1(primary_logits=forward.primary_logits, secondary_logits=forward.secondary_logits, primary_target=view.primary_target.to(device), secondary_target=view.secondary_target.to(device), primary_mask=view.primary_mask.to(device), secondary_mask=view.secondary_mask.to(device), loss_contract=loss_contract, selected_execution_device=device)
                _finite_tensors({"validationLoss": losses.total_loss})
                metrics.append({"epoch": epoch, "validationExampleCount": len(view.examples), "validationLoss": float(losses.total_loss.detach().cpu().item())})
            model.train()

        for epoch in range(int(prepared.packet["request"]["epochs"])):
            for batch_start in range(0, len(prepared.train), int(prepared.packet["request"]["microBatchSize"])):
                batch_items = prepared.train[batch_start:batch_start + int(prepared.packet["request"]["microBatchSize"])]
                start, end = batch_start, batch_start + len(batch_items)
                full_batch = prepared.tensors["TRAIN"]
                device = torch.device("cuda:0")
                optimizer.zero_grad(set_to_none=True)
                forward = model(full_batch.input_ids[start:end].to(device), full_batch.attention_mask[start:end].to(device))
                losses = compute_him_masked_multi_objective_loss_v1(primary_logits=forward.primary_logits, secondary_logits=forward.secondary_logits, primary_target=full_batch.primary_target[start:end].to(device), secondary_target=full_batch.secondary_target[start:end].to(device), primary_mask=full_batch.primary_mask[start:end].to(device), secondary_mask=full_batch.secondary_mask[start:end].to(device), loss_contract=loss_contract, selected_execution_device=device)
                _finite_tensors({"loss": losses.total_loss})
                losses.total_loss.backward()
                optimizer.step()
                optimizer_step += 1
                metrics.append({"epoch": epoch + 1, "optimizerStep": optimizer_step, "loss": float(losses.total_loss.detach().cpu().item())})
            validate_epoch(epoch + 1)
        validate_epoch(int(prepared.packet["request"]["epochs"]))
        request = prepared.packet["request"]
        runtime_identity = {"device": "cuda:0", "runtimeImage": request["runtimeImage"], "modelId": request["modelId"], "modelRevision": request["modelRevision"], "tokenizerId": request["tokenizerId"], "tokenizerArtifactSha256": request["tokenizerArtifactSha256"]}
        optimizer_identity = {"optimizerId": request["optimizerId"], "learningRate": request["learningRate"], "gradientAccumulationSteps": request["gradientAccumulationSteps"], "trainingSeed": request["trainingSeed"]}
        authority_bindings = build_prepared_checkpoint_bindings(prepared, runtime_identity=runtime_identity, optimizer_identity=optimizer_identity)
        checkpoint_result = persist_checkpoint(output, model=model, optimizer=optimizer, run_reference=run_reference, optimizer_step=optimizer_step, authority_bindings=authority_bindings, runtime_identity=runtime_identity, optimizer_identity=optimizer_identity)
        checkpoint = checkpoint_result.evidence_fields()
        evidence = run_evidence_payload(prepared, run_reference, "RUN_COMPLETED", checkpoint, training_metrics=metrics)
        atomic_json_write(output / "run-evidence.v2.json", evidence)
        return evidence
    except Exception as error:
        failure = f"CHECKPOINT_STAGE:PERSIST_OR_RELOAD:{type(error).__name__}:{error}" if isinstance(error, CheckpointRuntimeError) else f"{type(error).__name__}:{error}"
        evidence = run_evidence_payload(prepared, run_reference, "RUN_FAILED", checkpoint, failure)
        atomic_json_write(output / "run-evidence.failed.v2.json", evidence)
        raise ProductiveTrainingV2Error("REAL_TRAINING_FAILED") from error


def _parse_args(arguments: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(prog="him_trainer")
    parser.add_argument("--mode", required=True, choices=(MODE_PRODUCTIVE_TRAINING_V2,))
    parser.add_argument("--packet-root", required=True)
    parser.add_argument("--request", required=True)
    parser.add_argument("--output-root", required=True)
    parser.add_argument("--tokenizer-path", required=True)
    parser.add_argument("--model-root")
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parsed = parser.parse_args(arguments)
    _require(not (parsed.execute and parsed.dry_run), "EXECUTION_FLAGS_CONFLICT")
    _require(parsed.dry_run or not parsed.execute or parsed.model_root is not None, "MODEL_ROOT_REQUIRED_FOR_EXECUTION")
    return parsed


def run_cli(arguments: list[str]) -> int:
    try:
        args = _parse_args(arguments)
        prepared = prepare_productive_v2(args.packet_root, args.request, args.tokenizer_path)
        if args.execute:
            from .point13_model_forward_v1 import (
                build_him_model_execution_binding_v1,
                load_pinned_him_multi_head_model_from_root_v1,
                load_pinned_model_config_from_root_v1,
            )
            request = prepared.packet["request"]
            model_root = Path(args.model_root)

            def load_external_model() -> Any:
                config = load_pinned_model_config_from_root_v1(model_root)
                binding = build_him_model_execution_binding_v1(config, int(request["trainingSeed"]), request["modelBindingDigest"])
                return load_pinned_him_multi_head_model_from_root_v1(binding, request["modelBindingDigest"], int(request["trainingSeed"]), model_root, "cuda:0")

            evidence = execute_productive_v2(
                prepared,
                args.output_root,
                model_loader=load_external_model,
            )
            sys.stdout.write(json.dumps({"state": evidence["state"], "reference": evidence["reference"]}, sort_keys=True) + "\n")
            return 0
        output = Path(args.output_root)
        atomic_json_write(output / "execution-plan.v2.json", prepared.plan)
        sys.stdout.write(json.dumps({"state": "DRY_RUN_PASS", "planReference": prepared.plan["reference"], "logicalDigest": prepared.plan["logicalDigest"], "counts": prepared.plan["identity"]["counts"]}, ensure_ascii=False, sort_keys=True) + "\n")
        return 0
    except (ProductiveTrainingV2Error, OSError, UnicodeError, ValueError) as error:
        sys.stderr.write(f"{type(error).__name__}: {error}\n")
        return 1
