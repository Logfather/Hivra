"""Additive P2 mixed-supervision contract for Human-authorized rejects.

This boundary keeps the semantic relation and authority provenance intact while
making Primary-target absence explicit.  ``primaryTarget=0`` exists only in
the numerical tensor representation as an out-of-domain absence marker; it is
never a Primary class and is accepted only where ``primaryMask=0``.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from typing import Any, Mapping


CONTRACT_ID_V1 = "HIM_P2_SECONDARY_ONLY_NEGATIVE_TRAINING_CONTRACT_V1"
VERSION_V1 = "1"
ROLE_PRIMARY_AND_SECONDARY = "PRIMARY_AND_SECONDARY"
ROLE_SECONDARY_ONLY = "SECONDARY_ONLY"
ROLES = (ROLE_PRIMARY_AND_SECONDARY, ROLE_SECONDARY_ONLY)
PRIMARY_TARGET_KINDS = ("EXISTING_CANONICAL", "IDENTITY", "VARIANT", "ALIAS", "NEW_CANONICAL")
SECONDARY_TARGETS = ("COMPATIBLE", "REJECT")
PRIMARY_ABSENCE_TENSOR_CODE = 0
PRIMARY_MASK_ACTIVE = 1.0
PRIMARY_MASK_INACTIVE = 0.0
SECONDARY_MASK_ACTIVE = 1.0
SECONDARY_MASK_INACTIVE = 0.0


class SecondaryOnlyContractError(ValueError):
    """Raised for an invalid mixed-supervision contract state."""


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise SecondaryOnlyContractError(message)


def _text(value: Any, message: str) -> str:
    _require(isinstance(value, str) and bool(value.strip()), message)
    return value


@dataclass(frozen=True)
class HimP2PrimaryTargetV1:
    kind: str
    reference: str

    def __post_init__(self) -> None:
        _require(self.kind in PRIMARY_TARGET_KINDS, "PRIMARY_TARGET_KIND_INVALID")
        _text(self.reference, "PRIMARY_TARGET_REFERENCE_INVALID")

    def to_dict(self) -> dict[str, str]:
        return {"kind": self.kind, "reference": self.reference}


@dataclass(frozen=True)
class HimP2TrainingExampleV1:
    example_reference: str
    observed_term: str
    candidate_canonical_id: str
    candidate_canonical_name: str
    primary_target: HimP2PrimaryTargetV1 | None
    primary_mask: float
    secondary_target: str
    secondary_mask: float
    training_role: str
    relation: str
    relation_authority_reference: str
    candidate_compatibility_authority_reference: str
    source_evidence_references: tuple[str, ...]
    family_reference: str
    lineage_reference: str
    catalog_reference: str

    def __post_init__(self) -> None:
        for value, message in (
            (self.example_reference, "EXAMPLE_REFERENCE_INVALID"),
            (self.observed_term, "OBSERVED_TERM_INVALID"),
            (self.candidate_canonical_id, "CANDIDATE_ID_INVALID"),
            (self.candidate_canonical_name, "CANDIDATE_NAME_INVALID"),
            (self.relation, "RELATION_INVALID"),
            (self.relation_authority_reference, "RELATION_AUTHORITY_REFERENCE_INVALID"),
            (self.candidate_compatibility_authority_reference, "CC_AUTHORITY_REFERENCE_INVALID"),
            (self.family_reference, "FAMILY_REFERENCE_INVALID"),
            (self.lineage_reference, "LINEAGE_REFERENCE_INVALID"),
            (self.catalog_reference, "CATALOG_REFERENCE_INVALID"),
        ):
            _text(value, message)
        _require(self.training_role in ROLES, "TRAINING_ROLE_INVALID")
        _require(self.secondary_target in SECONDARY_TARGETS, "SECONDARY_TARGET_INVALID")
        _require(self.primary_mask in (PRIMARY_MASK_ACTIVE, PRIMARY_MASK_INACTIVE), "PRIMARY_MASK_INVALID")
        _require(self.secondary_mask == SECONDARY_MASK_ACTIVE, "SECONDARY_MASK_INVALID")
        _require(self.source_evidence_references and all(isinstance(item, str) and item.strip() for item in self.source_evidence_references), "SOURCE_EVIDENCE_INVALID")
        if self.training_role == ROLE_SECONDARY_ONLY:
            _require(self.primary_target is None, "SECONDARY_ONLY_PRIMARY_TARGET_PRESENT")
            _require(self.primary_mask == PRIMARY_MASK_INACTIVE, "SECONDARY_ONLY_PRIMARY_MASK_ACTIVE")
            _require(self.secondary_target == "REJECT", "SECONDARY_ONLY_REJECT_REQUIRED")
        else:
            _require(self.primary_target is not None, "PRIMARY_TARGET_REQUIRED")
            _require(isinstance(self.primary_target, HimP2PrimaryTargetV1), "PRIMARY_TARGET_INVALID")
            _require(self.primary_mask == PRIMARY_MASK_ACTIVE, "PRIMARY_AND_SECONDARY_PRIMARY_MASK_INVALID")

    @property
    def candidate_conditioned_input(self) -> tuple[str, str]:
        return self.observed_term, f"CANDIDATE {self.candidate_canonical_name}"

    @property
    def primary_target_absent(self) -> bool:
        return self.primary_target is None

    def identity_payload(self) -> dict[str, Any]:
        return {
            "contractId": CONTRACT_ID_V1,
            "version": VERSION_V1,
            "exampleReference": self.example_reference,
            "observedTerm": self.observed_term,
            "candidateCanonicalId": self.candidate_canonical_id,
            "candidateCanonicalName": self.candidate_canonical_name,
            "primaryTarget": None if self.primary_target is None else self.primary_target.to_dict(),
            "primaryMask": self.primary_mask,
            "secondaryTarget": self.secondary_target,
            "secondaryMask": self.secondary_mask,
            "trainingRole": self.training_role,
            "relation": self.relation,
            "relationAuthorityReference": self.relation_authority_reference,
            "candidateCompatibilityAuthorityReference": self.candidate_compatibility_authority_reference,
            "sourceEvidenceReferences": list(self.source_evidence_references),
            "familyReference": self.family_reference,
            "lineageReference": self.lineage_reference,
            "catalogReference": self.catalog_reference,
        }

    @property
    def logical_digest(self) -> str:
        return hashlib.sha256(json.dumps(self.identity_payload(), ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")).hexdigest()

    @property
    def reference(self) -> str:
        return f"p2-training-example:v1:{self.logical_digest}"

    def to_dict(self) -> dict[str, Any]:
        return {**self.identity_payload(), "logicalDigest": self.logical_digest, "reference": self.reference}

    def serialize(self) -> bytes:
        return (json.dumps(self.to_dict(), ensure_ascii=False, separators=(",", ":"), sort_keys=True) + "\n").encode("utf-8")

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> "HimP2TrainingExampleV1":
        _require(isinstance(value, Mapping), "TRAINING_EXAMPLE_OBJECT_INVALID")
        expected = set(cls._serialized_keys())
        _require(set(value) == expected, "TRAINING_EXAMPLE_FIELDS_INVALID")
        _require(value["contractId"] == CONTRACT_ID_V1, "TRAINING_EXAMPLE_CONTRACT_INVALID")
        _require(value["version"] == VERSION_V1, "TRAINING_EXAMPLE_VERSION_INVALID")
        raw_target = value["primaryTarget"]
        if raw_target is not None:
            _require(isinstance(raw_target, Mapping), "PRIMARY_TARGET_OBJECT_INVALID")
            _require(set(raw_target) == {"kind", "reference"}, "PRIMARY_TARGET_FIELDS_INVALID")
            target = HimP2PrimaryTargetV1(str(raw_target["kind"]), str(raw_target["reference"]))
        else:
            target = None
        example = cls(
            str(value["exampleReference"]), str(value["observedTerm"]), str(value["candidateCanonicalId"]), str(value["candidateCanonicalName"]),
            target, float(value["primaryMask"]), str(value["secondaryTarget"]), float(value["secondaryMask"]), str(value["trainingRole"]),
            str(value["relation"]), str(value["relationAuthorityReference"]), str(value["candidateCompatibilityAuthorityReference"]),
            tuple(value["sourceEvidenceReferences"]), str(value["familyReference"]), str(value["lineageReference"]), str(value["catalogReference"]),
        )
        _require(value["logicalDigest"] == example.logical_digest, "TRAINING_EXAMPLE_DIGEST_INVALID")
        _require(value["reference"] == example.reference, "TRAINING_EXAMPLE_REFERENCE_INVALID")
        return example

    @staticmethod
    def _serialized_keys() -> tuple[str, ...]:
        return (
            "contractId", "version", "exampleReference", "observedTerm", "candidateCanonicalId", "candidateCanonicalName",
            "primaryTarget", "primaryMask", "secondaryTarget", "secondaryMask", "trainingRole", "relation",
            "relationAuthorityReference", "candidateCompatibilityAuthorityReference", "sourceEvidenceReferences",
            "familyReference", "lineageReference", "catalogReference", "logicalDigest", "reference",
        )


@dataclass(frozen=True)
class HimP2TensorBatchV1:
    input_ids: Any
    attention_mask: Any
    primary_target: Any
    secondary_target: Any
    primary_mask: Any
    secondary_mask: Any
    examples: tuple[HimP2TrainingExampleV1, ...]

    @property
    def secondary_only_count(self) -> int:
        return sum(item.training_role == ROLE_SECONDARY_ONLY for item in self.examples)


def build_him_p2_tensor_batch_v1(examples: tuple[HimP2TrainingExampleV1, ...], input_ids: tuple[tuple[int, ...], ...]) -> HimP2TensorBatchV1:
    import torch

    _require(examples and len(examples) == len(input_ids), "BATCH_INPUT_COUNT_INVALID")
    _require(all(ids and all(isinstance(item, int) and item >= 0 and item != 1 for item in ids) for ids in input_ids), "BATCH_INPUT_IDS_INVALID")
    target_length = max(len(ids) for ids in input_ids)
    padded = [list(ids) + [1] * (target_length - len(ids)) for ids in input_ids]
    attention = [[1] * len(ids) + [0] * (target_length - len(ids)) for ids in input_ids]
    return HimP2TensorBatchV1(
        torch.tensor(padded, dtype=torch.int64), torch.tensor(attention, dtype=torch.int64),
        torch.tensor([{"EXISTING_CANONICAL": 1, "IDENTITY": 2, "VARIANT": 3, "ALIAS": 4, "NEW_CANONICAL": 5}[item.primary_target.kind] if item.primary_target else PRIMARY_ABSENCE_TENSOR_CODE for item in examples], dtype=torch.int64),
        torch.tensor([0 if item.secondary_target == "COMPATIBLE" else 1 for item in examples], dtype=torch.int64),
        torch.tensor([item.primary_mask for item in examples], dtype=torch.float32), torch.tensor([item.secondary_mask for item in examples], dtype=torch.float32), examples,
    )


def compute_him_p2_masked_loss_v1(*, primary_logits: Any, secondary_logits: Any, batch: HimP2TensorBatchV1, loss_contract: Any) -> Any:
    from .point13_loss_v1 import compute_him_masked_multi_objective_loss_v1

    return compute_him_masked_multi_objective_loss_v1(
        primary_logits=primary_logits, secondary_logits=secondary_logits, primary_target=batch.primary_target,
        secondary_target=batch.secondary_target, primary_mask=batch.primary_mask, secondary_mask=batch.secondary_mask,
        loss_contract=loss_contract, allow_primary_target_absence=True,
    )


def supervision_counts(examples: tuple[HimP2TrainingExampleV1, ...]) -> dict[str, int]:
    return {
        "total": len(examples),
        "primaryActive": sum(item.primary_mask == 1.0 for item in examples),
        "secondaryActive": sum(item.secondary_mask == 1.0 for item in examples),
        "secondaryOnly": sum(item.training_role == ROLE_SECONDARY_ONLY for item in examples),
        "compatible": sum(item.secondary_target == "COMPATIBLE" for item in examples),
        "reject": sum(item.secondary_target == "REJECT" for item in examples),
    }


def validate_secondary_only_readiness(examples: tuple[HimP2TrainingExampleV1, ...]) -> None:
    _require(examples, "EXAMPLES_EMPTY")
    for item in examples:
        item.__post_init__()
    _require(len({item.example_reference for item in examples}) == len(examples), "DUPLICATE_EXAMPLE_IDENTITY")
    _require(all(item.training_role != ROLE_SECONDARY_ONLY or item.secondary_target == "REJECT" for item in examples), "SECONDARY_ONLY_REJECT_MISSING")


def evaluation_counts(examples: tuple[HimP2TrainingExampleV1, ...]) -> dict[str, int]:
    counts = supervision_counts(examples)
    counts.update({"primaryMetricActive": counts["primaryActive"], "secondaryMetricActive": counts["secondaryActive"]})
    return counts


def build_execution_plan_v1(examples: tuple[HimP2TrainingExampleV1, ...]) -> dict[str, Any]:
    validate_secondary_only_readiness(examples)
    counts = supervision_counts(examples)
    payload = {"contractId": CONTRACT_ID_V1, "version": VERSION_V1, "counts": counts, "familyBound": True}
    digest = hashlib.sha256(json.dumps(payload, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    return {**payload, "logicalDigest": digest, "reference": f"p2-secondary-only-execution-plan:v1:{digest}"}


def build_run_evidence_v1(examples: tuple[HimP2TrainingExampleV1, ...]) -> dict[str, Any]:
    validate_secondary_only_readiness(examples)
    return {"contractId": CONTRACT_ID_V1, "version": VERSION_V1, "supervision": evaluation_counts(examples), "primaryMetricExcludesSecondaryOnly": True, "secondaryMetricIncludesSecondaryOnly": True}


def build_evaluation_evidence_v1(examples: tuple[HimP2TrainingExampleV1, ...]) -> tuple[dict[str, Any], ...]:
    """Expose per-example supervision state without requiring predictions."""

    validate_secondary_only_readiness(examples)
    return tuple(
        {
            "exampleReference": item.example_reference,
            "trainingRole": item.training_role,
            "primaryTargetPresent": item.primary_target is not None,
            "primaryMask": item.primary_mask,
            "primaryMetricAuthoritative": item.primary_mask == PRIMARY_MASK_ACTIVE,
            "secondaryTarget": item.secondary_target,
            "secondaryMask": item.secondary_mask,
            "secondaryMetricAuthoritative": item.secondary_mask == SECONDARY_MASK_ACTIVE,
            "relation": item.relation,
            "relationAuthorityReference": item.relation_authority_reference,
            "candidateCompatibilityAuthorityReference": item.candidate_compatibility_authority_reference,
            "sourceEvidenceReferences": list(item.source_evidence_references),
        }
        for item in examples
    )


def build_dry_run_report_v1(examples: tuple[HimP2TrainingExampleV1, ...]) -> dict[str, Any]:
    """Validate/report mixed supervision without loading a model."""

    validate_secondary_only_readiness(examples)
    counts = supervision_counts(examples)
    return {
        "contractId": CONTRACT_ID_V1,
        "version": VERSION_V1,
        "counts": counts,
        "missingMasks": 0,
        "fakePrimaryTargets": 0,
        "invalidRoleCombinations": 0,
        "familyBound": all(bool(item.family_reference) for item in examples),
    }
