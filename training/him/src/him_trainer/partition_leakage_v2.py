"""Target-independent partition leakage authority for HIM P2 V2.

The contract is deliberately independent from supervision and model execution.
It separates source provenance from model-visible semantic identity and makes
the selected isolation units explicit.  A future partitioner can compute
connected components with this module and assign complete components to
partitions before stratifying targets.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from enum import Enum
from typing import Any, Iterable, Mapping, Sequence

from .evidence_projection_v2 import (
    EVIDENCE_PROJECTION_V2_ID,
    EVIDENCE_PROJECTION_V2_VERSION,
)
from .input_representation_v2 import (
    CONTRACT_ID as INPUT_REPRESENTATION_V2_ID,
    CONTRACT_VERSION as INPUT_REPRESENTATION_V2_VERSION,
    HimInputRepresentationV2,
)
from .sequence_length_authority_v2 import (
    SEQUENCE_LENGTH_AUTHORITY_ID,
    SEQUENCE_LENGTH_AUTHORITY_VERSION,
)


PARTITION_LEAKAGE_V2_ID = "HIM_P2_PARTITION_LEAKAGE_V2"
PARTITION_LEAKAGE_V2_VERSION = "2"
PARTITION_AUTHORITY_V2_ID = "HIM_P2_PARTITION_AUTHORITY_V2"
LEAKAGE_VALIDATION_AUTHORITY_V2_ID = "HIM_P2_LEAKAGE_VALIDATION_AUTHORITY_V2"
PARTITION_STRATEGY_V2 = "GROUP_THEN_STRATIFY"
HEURISTIC_SEMANTIC_LEAKAGE_MATCHING = "FORBIDDEN"


class PartitionLeakageV2Error(ValueError):
    """Raised when a V2 identity or authority binding is invalid."""


class IsolationUnitV2(str, Enum):
    FAMILY = "FAMILY"
    SOURCE_RECORD = "SOURCE_RECORD"
    EVIDENCE_PROJECTION = "EVIDENCE_PROJECTION"
    DECISION_CONTEXT = "DECISION_CONTEXT"


REQUIRED_ISOLATION_UNITS_V2 = (
    IsolationUnitV2.FAMILY,
    IsolationUnitV2.SOURCE_RECORD,
    IsolationUnitV2.EVIDENCE_PROJECTION,
    IsolationUnitV2.DECISION_CONTEXT,
)


def _digest(payload: object) -> str:
    encoded = json.dumps(
        payload,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _text(value: object, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise PartitionLeakageV2Error(f"{field}_MUST_BE_NON_EMPTY_TEXT")
    return value


def _ordered_unique(values: Iterable[str]) -> tuple[str, ...]:
    return tuple(sorted(set(values)))


@dataclass(frozen=True)
class PartitionLeakageExampleV2:
    """One partition candidate with audit and model-facing identities.

    ``source_record_identity`` and ``evidence_projection_reference`` are
    audit-side values.  They are never serialized into ``model_input``.  The
    optional ``target`` is retained only for callers' post-group stratification
    bookkeeping and is intentionally excluded from every identity below.
    """

    record_reference: str
    partition: str
    family_reference: str
    source_record_identity: str
    evidence_projection_reference: str
    model_input: HimInputRepresentationV2
    candidate_reference: str | None = None
    target: object | None = None

    def __post_init__(self) -> None:
        for field in (
            "record_reference",
            "partition",
            "family_reference",
            "source_record_identity",
            "evidence_projection_reference",
        ):
            _text(getattr(self, field), field.upper())
        if not isinstance(self.model_input, HimInputRepresentationV2):
            raise PartitionLeakageV2Error("MODEL_INPUT_MUST_BE_INPUT_REPRESENTATION_V2")
        if self.candidate_reference is not None:
            _text(self.candidate_reference, "CANDIDATE_REFERENCE")

    @property
    def decision_context_reference(self) -> str:
        return decision_context_reference_v2(self.model_input)

    @property
    def candidate_semantic_signature(self) -> str:
        return candidate_semantic_signature_v2(self.model_input)

    @property
    def observed_semantic_signature(self) -> str:
        return observed_semantic_signature_v2(self.model_input)

    @property
    def observed_term(self) -> str:
        return self.model_input.observed.term


@dataclass(frozen=True)
class LeakageDiagnosticV2:
    isolation_unit: IsolationUnitV2 | str
    key: str
    record_references: tuple[str, ...]
    partitions: tuple[str, ...]
    fatal: bool = True

    def __post_init__(self) -> None:
        _text(str(self.isolation_unit), "ISOLATION_UNIT")
        _text(self.key, "KEY")
        if not self.record_references:
            raise PartitionLeakageV2Error("DIAGNOSTIC_RECORD_REFERENCES_EMPTY")
        if not self.partitions:
            raise PartitionLeakageV2Error("DIAGNOSTIC_PARTITIONS_EMPTY")


@dataclass(frozen=True)
class LeakageValidationResultV2:
    valid: bool
    diagnostics: tuple[LeakageDiagnosticV2, ...]

    def __post_init__(self) -> None:
        if self.valid != (not any(item.fatal for item in self.diagnostics)):
            raise PartitionLeakageV2Error("VALIDITY_DOES_NOT_MATCH_DIAGNOSTICS")


def _candidate_payload(value: HimInputRepresentationV2) -> dict[str, object]:
    candidate = value.candidate
    return {
        "C": candidate.term,
        "N": candidate.normalized_term,
        "X": [list(path) for path in candidate.taxonomy_paths],
    }


def _observed_payload(value: HimInputRepresentationV2) -> dict[str, object]:
    observed = value.observed
    return {
        "O": observed.term,
        "L": observed.source_faithful_label,
        "I": observed.composition,
        "K": list(observed.categories),
        "G": list(observed.food_groups),
        "T": observed.product_type,
    }


def candidate_semantic_signature_v2(value: HimInputRepresentationV2) -> str:
    """Return a digest of model-visible candidate fields C/N/X only."""

    return f"him-candidate-semantics:v2:{_digest(_candidate_payload(value))}"


def observed_semantic_signature_v2(value: HimInputRepresentationV2) -> str:
    """Return a digest of model-visible observed fields O/L/I/K/G/T only."""

    return f"him-observed-semantics:v2:{_digest(_observed_payload(value))}"


def decision_context_reference_v2(value: HimInputRepresentationV2) -> str:
    """Identify an exact model-facing V2 decision context.

    The canonical V2 serializer is the equality source, so target, family,
    source identity, authority and partition cannot affect this reference.
    """

    payload = {
        "contractId": INPUT_REPRESENTATION_V2_ID,
        "contractVersion": INPUT_REPRESENTATION_V2_VERSION,
        "observed": _observed_payload(value),
        "candidate": _candidate_payload(value),
    }
    return f"him-p2-decision-context:v2:{_digest(payload)}"


def isolation_key(example: PartitionLeakageExampleV2, unit: IsolationUnitV2) -> str:
    if unit is IsolationUnitV2.FAMILY:
        return example.family_reference
    if unit is IsolationUnitV2.SOURCE_RECORD:
        return example.source_record_identity
    if unit is IsolationUnitV2.EVIDENCE_PROJECTION:
        return example.evidence_projection_reference
    if unit is IsolationUnitV2.DECISION_CONTEXT:
        return example.decision_context_reference
    raise PartitionLeakageV2Error(f"UNSUPPORTED_ISOLATION_UNIT:{unit}")


def validate_partition_assignments(
    examples: Sequence[PartitionLeakageExampleV2],
    isolation_units: Sequence[IsolationUnitV2] = REQUIRED_ISOLATION_UNITS_V2,
) -> LeakageValidationResultV2:
    """Fail closed when any selected identity crosses partition boundaries."""

    selected = tuple(dict.fromkeys(isolation_units))
    if not selected:
        raise PartitionLeakageV2Error("ISOLATION_UNITS_EMPTY")
    if any(not isinstance(unit, IsolationUnitV2) for unit in selected):
        raise PartitionLeakageV2Error("ISOLATION_UNIT_INVALID")

    diagnostics: list[LeakageDiagnosticV2] = []
    by_record: dict[str, list[PartitionLeakageExampleV2]] = {}
    for example in examples:
        by_record.setdefault(example.record_reference, []).append(example)
    for key, values in sorted(by_record.items()):
        if len(values) > 1:
            diagnostics.append(
                LeakageDiagnosticV2(
                    isolation_unit="RECORD_REFERENCE",
                    key=key,
                    record_references=tuple(sorted(item.record_reference for item in values)),
                    partitions=_ordered_unique(item.partition for item in values),
                )
            )

    for unit in selected:
        by_key: dict[str, list[PartitionLeakageExampleV2]] = {}
        for example in examples:
            key = isolation_key(example, unit)
            if not key.strip():
                raise PartitionLeakageV2Error(f"{unit.value}_IDENTITY_MISSING")
            by_key.setdefault(key, []).append(example)
        for key, values in sorted(by_key.items()):
            partitions = _ordered_unique(item.partition for item in values)
            if len(partitions) > 1:
                diagnostics.append(
                    LeakageDiagnosticV2(
                        isolation_unit=unit,
                        key=key,
                        record_references=tuple(sorted(item.record_reference for item in values)),
                        partitions=partitions,
                    )
                )

    ordered = tuple(
        sorted(
            diagnostics,
            key=lambda item: (str(item.isolation_unit), item.key, item.record_references),
        )
    )
    return LeakageValidationResultV2(valid=not any(item.fatal for item in ordered), diagnostics=ordered)


def connected_components(
    examples: Sequence[PartitionLeakageExampleV2],
    isolation_units: Sequence[IsolationUnitV2] = REQUIRED_ISOLATION_UNITS_V2,
) -> tuple[tuple[PartitionLeakageExampleV2, ...], ...]:
    """Build deterministic transitive groups over the selected identities."""

    selected = tuple(dict.fromkeys(isolation_units))
    if not selected:
        raise PartitionLeakageV2Error("ISOLATION_UNITS_EMPTY")
    ordered = tuple(sorted(examples, key=lambda item: item.record_reference))
    parent = list(range(len(ordered)))

    def find(index: int) -> int:
        while parent[index] != index:
            parent[index] = parent[parent[index]]
            index = parent[index]
        return index

    def union(left: int, right: int) -> None:
        root_left, root_right = find(left), find(right)
        if root_left != root_right:
            parent[root_right] = root_left

    for unit in selected:
        first_by_key: dict[str, int] = {}
        for index, example in enumerate(ordered):
            key = isolation_key(example, unit)
            if not key.strip():
                raise PartitionLeakageV2Error(f"{unit.value}_IDENTITY_MISSING")
            previous = first_by_key.setdefault(key, index)
            union(previous, index)

    groups: dict[int, list[PartitionLeakageExampleV2]] = {}
    for index, example in enumerate(ordered):
        groups.setdefault(find(index), []).append(example)
    return tuple(
        sorted(
            (tuple(sorted(values, key=lambda item: item.record_reference)) for values in groups.values()),
            key=lambda group: group[0].record_reference,
        )
    )


def component_statistics(
    examples: Sequence[PartitionLeakageExampleV2],
    isolation_units: Sequence[IsolationUnitV2] = REQUIRED_ISOLATION_UNITS_V2,
) -> dict[str, int]:
    components = connected_components(examples, isolation_units)
    sizes = sorted(len(group) for group in components)
    return {
        "componentCount": len(sizes),
        "largestComponentSize": sizes[-1] if sizes else 0,
        "medianComponentSize": sizes[(len(sizes) - 1) // 2] if sizes else 0,
    }


def isolation_group_reference_v2(example: PartitionLeakageExampleV2) -> str:
    """Return a stable group identity without target or partition data."""

    payload = {
        "family": example.family_reference,
        "sourceRecord": example.source_record_identity,
        "projection": example.evidence_projection_reference,
        "decisionContext": example.decision_context_reference,
    }
    return f"him-p2-isolation-group:v2:{_digest(payload)}"


def expected_representation_binding_v2() -> dict[str, str]:
    return {
        "inputRepresentationId": INPUT_REPRESENTATION_V2_ID,
        "inputRepresentationVersion": INPUT_REPRESENTATION_V2_VERSION,
        "evidenceProjectionId": EVIDENCE_PROJECTION_V2_ID,
        "evidenceProjectionVersion": EVIDENCE_PROJECTION_V2_VERSION,
        "sequenceLengthAuthorityId": SEQUENCE_LENGTH_AUTHORITY_ID,
        "sequenceLengthAuthorityVersion": SEQUENCE_LENGTH_AUTHORITY_VERSION,
    }


def validate_representation_binding_v2(binding: Mapping[str, Any]) -> None:
    expected = expected_representation_binding_v2()
    for key, value in expected.items():
        if binding.get(key) != value:
            raise PartitionLeakageV2Error(f"REPRESENTATION_BINDING_MISMATCH:{key}")


@dataclass(frozen=True)
class PartitionAuthorityV2:
    """Reloadable future authority shape; no partition persistence is done here."""

    corpus_identity: str
    partition_strategy: str
    isolation_units: tuple[IsolationUnitV2, ...]
    split_membership: tuple[tuple[str, str], ...]
    grouping_identities: tuple[tuple[str, str], ...]
    representation_binding: tuple[tuple[str, str], ...]
    input_representation_id: str = INPUT_REPRESENTATION_V2_ID
    input_representation_version: str = INPUT_REPRESENTATION_V2_VERSION
    evidence_projection_id: str = EVIDENCE_PROJECTION_V2_ID
    evidence_projection_version: str = EVIDENCE_PROJECTION_V2_VERSION
    sequence_length_authority_id: str = SEQUENCE_LENGTH_AUTHORITY_ID
    sequence_length_authority_version: str = SEQUENCE_LENGTH_AUTHORITY_VERSION
    authority_id: str = PARTITION_AUTHORITY_V2_ID
    authority_version: str = PARTITION_LEAKAGE_V2_VERSION

    @property
    def logical_digest(self) -> str:
        payload = {
            "authorityId": self.authority_id,
            "authorityVersion": self.authority_version,
            "corpusIdentity": self.corpus_identity,
            "inputRepresentationId": self.input_representation_id,
            "inputRepresentationVersion": self.input_representation_version,
            "evidenceProjectionId": self.evidence_projection_id,
            "evidenceProjectionVersion": self.evidence_projection_version,
            "sequenceLengthAuthorityId": self.sequence_length_authority_id,
            "sequenceLengthAuthorityVersion": self.sequence_length_authority_version,
            "partitionStrategy": self.partition_strategy,
            "isolationUnits": [unit.value for unit in self.isolation_units],
            "splitMembership": [list(item) for item in self.split_membership],
            "groupingIdentities": [list(item) for item in self.grouping_identities],
            "representationBinding": [list(item) for item in self.representation_binding],
        }
        return _digest(payload)

    @property
    def reference(self) -> str:
        return f"him-p2-partition-authority:v2:{self.logical_digest}"


@dataclass(frozen=True)
class LeakageValidationAuthorityV2:
    """Reloadable deterministic result shape for a leakage validation run."""

    partition_identity: str
    isolation_units_checked: tuple[IsolationUnitV2, ...]
    collision_counts: tuple[tuple[str, int], ...]
    collision_references: tuple[tuple[str, tuple[str, ...]], ...]
    status: str
    authority_id: str = LEAKAGE_VALIDATION_AUTHORITY_V2_ID
    authority_version: str = PARTITION_LEAKAGE_V2_VERSION

    @property
    def logical_digest(self) -> str:
        return _digest(
            {
                "authorityId": self.authority_id,
                "authorityVersion": self.authority_version,
                "partitionIdentity": self.partition_identity,
                "isolationUnitsChecked": [unit.value for unit in self.isolation_units_checked],
                "collisionCounts": [list(item) for item in self.collision_counts],
                "collisionReferences": [[key, list(values)] for key, values in self.collision_references],
                "status": self.status,
            }
        )

    @property
    def reference(self) -> str:
        return f"him-p2-leakage-validation:v2:{self.logical_digest}"


__all__ = [
    "EVIDENCE_PROJECTION_V2_ID",
    "EVIDENCE_PROJECTION_V2_VERSION",
    "HEURISTIC_SEMANTIC_LEAKAGE_MATCHING",
    "IsolationUnitV2",
    "LEAKAGE_VALIDATION_AUTHORITY_V2_ID",
    "LeakageDiagnosticV2",
    "LeakageValidationAuthorityV2",
    "LeakageValidationResultV2",
    "PARTITION_AUTHORITY_V2_ID",
    "PARTITION_LEAKAGE_V2_ID",
    "PARTITION_LEAKAGE_V2_VERSION",
    "PARTITION_STRATEGY_V2",
    "PartitionAuthorityV2",
    "PartitionLeakageExampleV2",
    "PartitionLeakageV2Error",
    "REQUIRED_ISOLATION_UNITS_V2",
    "candidate_semantic_signature_v2",
    "component_statistics",
    "connected_components",
    "decision_context_reference_v2",
    "expected_representation_binding_v2",
    "isolation_group_reference_v2",
    "isolation_key",
    "observed_semantic_signature_v2",
    "validate_partition_assignments",
    "validate_representation_binding_v2",
]
