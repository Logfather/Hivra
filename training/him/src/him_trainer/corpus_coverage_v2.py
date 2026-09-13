"""Scientific coverage authority for the HIM P2 corpus V2.

This module describes *why* a corpus is sufficient for the next qualification
experiment.  It does not create labels, inspect a model, or materialize a
corpus.  All contrast groups are explicit or exact candidate/family bindings;
there is no fuzzy, lexical, embedding, or target-aware grouping.
"""

from __future__ import annotations

import hashlib
import json
from collections import Counter, defaultdict
from dataclasses import asdict, dataclass, field
from typing import Iterable, Mapping, Sequence

from .evidence_projection_v2 import (
    EVIDENCE_PROJECTION_V2_ID,
    EVIDENCE_PROJECTION_V2_VERSION,
)
from .input_representation_v2 import (
    CONTRACT_ID as INPUT_REPRESENTATION_V2_ID,
    CONTRACT_VERSION as INPUT_REPRESENTATION_V2_VERSION,
    HimInputRepresentationV2,
    scan_model_input,
)
from .partition_leakage_v2 import (
    IsolationUnitV2,
    PartitionLeakageExampleV2,
    REQUIRED_ISOLATION_UNITS_V2,
    candidate_semantic_signature_v2,
    decision_context_reference_v2,
    validate_partition_assignments,
)
from .sequence_length_authority_v2 import (
    SEQUENCE_LENGTH_AUTHORITY_ID,
    SEQUENCE_LENGTH_AUTHORITY_VERSION,
    SEQUENCE_LENGTH_V2,
    sequence_length_authority_v2,
)


CORPUS_COVERAGE_V2_ID = "HIM_P2_CORPUS_COVERAGE_V2"
CORPUS_COVERAGE_V2_VERSION = "2"
CORPUS_COVERAGE_REFERENCE_PREFIX = "him-p2-corpus-coverage:v2:"
CONTRASTIVE_GROUPING_POLICY_V2 = "EXPLICIT_OR_EXACT_CANDIDATE_FAMILY_ONLY"
TARGET_LEAKAGE_POLICY_V2 = "GOLD_SUPERVISION_IS_NOT_MODEL_VISIBLE"
NO_HEURISTIC_TARGET_CREATION_V2 = True

PRIMARY_TARGETS_V2 = ("IDENTITY", "VARIANT")
SECONDARY_TARGETS_V2 = ("COMPATIBLE", "REJECT")
DEFINED_NEGATIVE_BOUNDARY_TYPES_V1 = (
    "CANONICAL_VS_CHILD_BOUNDARY",
    "IDENTITY_VS_VARIANT_BOUNDARY",
    "ALIAS_VS_SEMANTIC_CHILD_BOUNDARY",
    "WRONG_RELATION_LEVEL",
    "WRONG_SCOPE",
    "WRONG_CLASSIFICATION",
    "CROSS_CANONICAL_CLASSIFICATION_BOUNDARY",
)


class CorpusCoverageV2Error(ValueError):
    """Raised when coverage input is not authoritative or fail-closed."""


def _text(value: object, field_name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise CorpusCoverageV2Error(f"{field_name}_MUST_BE_NON_EMPTY_TEXT")
    return value


def _digest(value: object) -> str:
    encoded = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


@dataclass(frozen=True)
class CorpusExampleV2:
    """An authority-bound example used for coverage diagnostics.

    The target and boundary fields are supervision-side only.  They are never
    passed to the V2 input serializer.  ``contrast_group_reference`` is an
    optional, already-authorized grouping identity; when absent, diagnostics
    use the exact candidate semantic identity together with its family.
    """

    example_reference: str
    family_reference: str
    source_record_identity: str
    evidence_projection_reference: str
    model_input: HimInputRepresentationV2
    candidate_reference: str
    source: str
    partition: str = "UNASSIGNED"
    primary_target: str | None = None
    secondary_target: str | None = None
    boundary_type: str | None = None
    contrast_group_reference: str | None = None
    sequence_length: int | None = None

    def __post_init__(self) -> None:
        for name in (
            "example_reference",
            "family_reference",
            "source_record_identity",
            "evidence_projection_reference",
            "candidate_reference",
            "source",
            "partition",
        ):
            _text(getattr(self, name), name.upper())
        if not isinstance(self.model_input, HimInputRepresentationV2):
            raise CorpusCoverageV2Error("MODEL_INPUT_MUST_BE_INPUT_REPRESENTATION_V2")
        if self.primary_target not in (None, *PRIMARY_TARGETS_V2):
            raise CorpusCoverageV2Error("PRIMARY_TARGET_INVALID")
        if self.secondary_target not in (None, *SECONDARY_TARGETS_V2):
            raise CorpusCoverageV2Error("SECONDARY_TARGET_INVALID")
        if self.secondary_target != "REJECT" and self.boundary_type is not None:
            raise CorpusCoverageV2Error("BOUNDARY_REQUIRES_REJECT_TARGET")
        if self.boundary_type is not None and self.boundary_type not in DEFINED_NEGATIVE_BOUNDARY_TYPES_V1:
            raise CorpusCoverageV2Error("BOUNDARY_TYPE_UNDEFINED")
        if self.contrast_group_reference is not None:
            _text(self.contrast_group_reference, "CONTRAST_GROUP_REFERENCE")
        if self.sequence_length is not None:
            if isinstance(self.sequence_length, bool) or not isinstance(self.sequence_length, int):
                raise CorpusCoverageV2Error("SEQUENCE_LENGTH_MUST_BE_INTEGER")
            if self.sequence_length < 0:
                raise CorpusCoverageV2Error("SEQUENCE_LENGTH_MUST_NOT_BE_NEGATIVE")

    @property
    def decision_context_reference(self) -> str:
        return decision_context_reference_v2(self.model_input)

    @property
    def candidate_semantic_signature(self) -> str:
        return candidate_semantic_signature_v2(self.model_input)

    @property
    def isolation_example(self) -> PartitionLeakageExampleV2:
        return PartitionLeakageExampleV2(
            record_reference=self.example_reference,
            partition=self.partition,
            family_reference=self.family_reference,
            source_record_identity=self.source_record_identity,
            evidence_projection_reference=self.evidence_projection_reference,
            model_input=self.model_input,
            candidate_reference=self.candidate_reference,
            target=self.primary_target or self.secondary_target,
        )

    @property
    def contrast_group(self) -> str:
        if self.contrast_group_reference is not None:
            return self.contrast_group_reference
        return f"candidate-family:{self.candidate_semantic_signature}:{self.family_reference}"


@dataclass(frozen=True)
class ContrastiveGroupV2:
    reference: str
    example_references: tuple[str, ...]
    families: tuple[str, ...]
    sources: tuple[str, ...]
    primary_targets: tuple[str, ...]
    secondary_targets: tuple[str, ...]

    @property
    def has_primary_contrast(self) -> bool:
        return set(self.primary_targets) == set(PRIMARY_TARGETS_V2)

    @property
    def has_secondary_contrast(self) -> bool:
        return set(self.secondary_targets) == set(SECONDARY_TARGETS_V2)


@dataclass(frozen=True)
class BoundaryCoverageV2:
    boundary_type: str
    example_count: int
    family_count: int
    source_count: int
    constructible: bool


@dataclass(frozen=True)
class CoverageReportV2:
    example_count: int
    primary_active_count: int
    primary_counts: Mapping[str, int]
    secondary_active_count: int
    secondary_counts: Mapping[str, int]
    family_counts: Mapping[str, int]
    decision_context_counts: Mapping[str, int]
    candidate_counts: Mapping[str, int]
    source_counts: Mapping[str, int]
    target_family_counts: Mapping[str, int]
    target_decision_context_counts: Mapping[str, int]
    target_source_counts: Mapping[str, int]
    target_candidate_counts: Mapping[str, int]
    target_candidate_example_counts: Mapping[str, Mapping[str, int]]
    boundary_coverage: tuple[BoundaryCoverageV2, ...]
    primary_contrastive_groups: tuple[ContrastiveGroupV2, ...]
    secondary_contrastive_groups: tuple[ContrastiveGroupV2, ...]
    primary_conflicting_target_group_count: int
    secondary_conflicting_target_group_count: int
    input_scan: Mapping[str, int]
    sequence_lengths: tuple[int, ...]

    @property
    def primary_contrastive_group_count(self) -> int:
        return len(self.primary_contrastive_groups)

    @property
    def secondary_contrastive_group_count(self) -> int:
        return len(self.secondary_contrastive_groups)

    @property
    def reject_example_count(self) -> int:
        return self.secondary_counts.get("REJECT", 0)

    @property
    def reject_family_count(self) -> int:
        return self.target_family_counts.get("REJECT", 0)


@dataclass(frozen=True)
class CoverageAssessmentV2:
    report: CoverageReportV2
    validation_report: CoverageReportV2 | None
    validation_measurable: bool
    target_presence_valid: bool
    contrastive_coverage_valid: bool
    sequence_valid: bool
    leakage_valid: bool
    quality_status: str


@dataclass(frozen=True)
class CorpusCoverageContractV2:
    contract_id: str = CORPUS_COVERAGE_V2_ID
    version: str = CORPUS_COVERAGE_V2_VERSION
    input_representation_id: str = INPUT_REPRESENTATION_V2_ID
    input_representation_version: str = INPUT_REPRESENTATION_V2_VERSION
    evidence_projection_id: str = EVIDENCE_PROJECTION_V2_ID
    evidence_projection_version: str = EVIDENCE_PROJECTION_V2_VERSION
    sequence_authority_id: str = SEQUENCE_LENGTH_AUTHORITY_ID
    sequence_authority_version: str = SEQUENCE_LENGTH_AUTHORITY_VERSION
    partition_leakage_id: str = "HIM_P2_PARTITION_LEAKAGE_V2"
    partition_leakage_version: str = "2"
    required_isolation_units: tuple[str, ...] = tuple(unit.value for unit in REQUIRED_ISOLATION_UNITS_V2)
    primary_targets: tuple[str, ...] = PRIMARY_TARGETS_V2
    secondary_targets: tuple[str, ...] = SECONDARY_TARGETS_V2
    negative_boundary_types: tuple[str, ...] = DEFINED_NEGATIVE_BOUNDARY_TYPES_V1
    grouping_policy: str = CONTRASTIVE_GROUPING_POLICY_V2
    target_leakage_policy: str = TARGET_LEAKAGE_POLICY_V2
    heuristic_target_creation: bool = NO_HEURISTIC_TARGET_CREATION_V2
    validation_requires_all_targets: bool = True
    same_boundary_type_across_splits_allowed: bool = True
    same_decision_context_across_splits_allowed: bool = False

    @property
    def logical_digest(self) -> str:
        return _digest(asdict(self))

    @property
    def reference(self) -> str:
        return f"{CORPUS_COVERAGE_REFERENCE_PREFIX}{self.logical_digest}"


def corpus_coverage_contract_v2() -> CorpusCoverageContractV2:
    """Return the immutable V2 coverage authority."""

    return CorpusCoverageContractV2()


def _groups(examples: Sequence[CorpusExampleV2]) -> tuple[ContrastiveGroupV2, ...]:
    grouped: dict[str, list[CorpusExampleV2]] = defaultdict(list)
    for example in examples:
        grouped[example.contrast_group].append(example)
    result: list[ContrastiveGroupV2] = []
    for reference, values in sorted(grouped.items()):
        result.append(
            ContrastiveGroupV2(
                reference=reference,
                example_references=tuple(sorted(item.example_reference for item in values)),
                families=tuple(sorted({item.family_reference for item in values})),
                sources=tuple(sorted({item.source for item in values})),
                primary_targets=tuple(sorted({item.primary_target for item in values if item.primary_target is not None})),
                secondary_targets=tuple(sorted({item.secondary_target for item in values if item.secondary_target is not None})),
            )
        )
    return tuple(result)


def build_coverage_report(examples: Iterable[CorpusExampleV2]) -> CoverageReportV2:
    values = tuple(examples)
    primary = Counter(item.primary_target for item in values if item.primary_target is not None)
    secondary = Counter(item.secondary_target for item in values if item.secondary_target is not None)
    families = Counter(item.family_reference for item in values)
    contexts = Counter(item.decision_context_reference for item in values)
    candidates = Counter(item.candidate_reference for item in values)
    sources = Counter(item.source for item in values)
    target_families: dict[str, set[str]] = defaultdict(set)
    target_contexts: dict[str, set[str]] = defaultdict(set)
    target_sources: dict[str, set[str]] = defaultdict(set)
    target_candidates: dict[str, set[str]] = defaultdict(set)
    target_candidate_examples: dict[str, Counter[str]] = defaultdict(Counter)
    for item in values:
        for target in (item.primary_target, item.secondary_target):
            if target is not None:
                target_families[target].add(item.family_reference)
                target_contexts[target].add(item.decision_context_reference)
                target_sources[target].add(item.source)
                target_candidates[target].add(item.candidate_reference)
                target_candidate_examples[target][item.candidate_reference] += 1

    boundary_coverage = tuple(
        BoundaryCoverageV2(
            boundary_type=boundary,
            example_count=sum(item.boundary_type == boundary for item in values),
            family_count=len({item.family_reference for item in values if item.boundary_type == boundary}),
            source_count=len({item.source for item in values if item.boundary_type == boundary}),
            constructible=any(item.boundary_type == boundary for item in values),
        )
        for boundary in DEFINED_NEGATIVE_BOUNDARY_TYPES_V1
    )

    primary_targets_by_context: dict[str, set[str]] = defaultdict(set)
    secondary_targets_by_context: dict[str, set[str]] = defaultdict(set)
    for item in values:
        if item.primary_target is not None:
            primary_targets_by_context[item.decision_context_reference].add(item.primary_target)
        if item.secondary_target is not None:
            secondary_targets_by_context[item.decision_context_reference].add(item.secondary_target)

    sequence_lengths = tuple(sorted(item.sequence_length for item in values if item.sequence_length is not None))
    return CoverageReportV2(
        example_count=len(values),
        primary_active_count=sum(primary.values()),
        primary_counts=dict(sorted(primary.items())),
        secondary_active_count=sum(secondary.values()),
        secondary_counts=dict(sorted(secondary.items())),
        family_counts=dict(sorted(families.items())),
        decision_context_counts=dict(sorted(contexts.items())),
        candidate_counts=dict(sorted(candidates.items())),
        source_counts=dict(sorted(sources.items())),
        target_family_counts={key: len(value) for key, value in sorted(target_families.items())},
        target_decision_context_counts={key: len(value) for key, value in sorted(target_contexts.items())},
        target_source_counts={key: len(value) for key, value in sorted(target_sources.items())},
        target_candidate_counts={key: len(value) for key, value in sorted(target_candidates.items())},
        target_candidate_example_counts={
            key: dict(sorted(value.items())) for key, value in sorted(target_candidate_examples.items())
        },
        boundary_coverage=boundary_coverage,
        primary_contrastive_groups=tuple(group for group in _groups(values) if group.has_primary_contrast),
        secondary_contrastive_groups=tuple(group for group in _groups(values) if group.has_secondary_contrast),
        primary_conflicting_target_group_count=sum(len(targets) > 1 for targets in primary_targets_by_context.values()),
        secondary_conflicting_target_group_count=sum(len(targets) > 1 for targets in secondary_targets_by_context.values()),
        input_scan=scan_model_input(item.model_input for item in values),
        sequence_lengths=sequence_lengths,
    )


def validate_target_presence(report: CoverageReportV2) -> bool:
    return all(report.primary_counts.get(target, 0) > 0 for target in PRIMARY_TARGETS_V2) and all(
        report.secondary_counts.get(target, 0) > 0 for target in SECONDARY_TARGETS_V2
    )


def validate_validation_measurability(report: CoverageReportV2) -> bool:
    return validate_target_presence(report)


def validate_sequence_lengths(report: CoverageReportV2, max_length: int = SEQUENCE_LENGTH_V2) -> bool:
    return all(length <= max_length for length in report.sequence_lengths)


def _leakage_valid(examples: Sequence[CorpusExampleV2]) -> bool:
    result = validate_partition_assignments(
        tuple(item.isolation_example for item in examples),
        REQUIRED_ISOLATION_UNITS_V2,
    )
    return result.valid


def assess_coverage(
    examples: Iterable[CorpusExampleV2],
    *,
    validation_examples: Iterable[CorpusExampleV2] | None = None,
) -> CoverageAssessmentV2:
    values = tuple(examples)
    report = build_coverage_report(values)
    validation_report = build_coverage_report(tuple(validation_examples)) if validation_examples is not None else None
    target_presence_valid = validate_target_presence(report)
    contrastive_coverage_valid = bool(report.primary_contrastive_groups and report.secondary_contrastive_groups)
    sequence_valid = validate_sequence_lengths(report)
    leakage_valid = _leakage_valid(values) if values else False
    validation_measurable = validation_report is not None and validate_validation_measurability(validation_report)
    model_input_clean = all(value == 0 for value in report.input_scan.values())

    if not sequence_valid:
        quality_status = "BLOCKED_BY_SEQUENCE_LENGTH"
    elif not leakage_valid:
        quality_status = "BLOCKED_BY_LEAKAGE"
    elif not model_input_clean:
        quality_status = "BLOCKED_BY_TARGET_LEAKAGE"
    elif report.primary_conflicting_target_group_count or report.secondary_conflicting_target_group_count:
        quality_status = "BLOCKED_BY_CONFLICTING_INPUT"
    elif not target_presence_valid or not contrastive_coverage_valid or not validation_measurable:
        quality_status = "REQUIRES_ADDITIONAL_REVIEWED_EXAMPLES"
    else:
        quality_status = "READY_AFTER_MATERIALIZATION"

    return CoverageAssessmentV2(
        report=report,
        validation_report=validation_report,
        validation_measurable=validation_measurable,
        target_presence_valid=target_presence_valid,
        contrastive_coverage_valid=contrastive_coverage_valid,
        sequence_valid=sequence_valid,
        leakage_valid=leakage_valid,
        quality_status=quality_status,
    )


def validate_contract_bindings_v2(contract: CorpusCoverageContractV2 | None = None) -> None:
    value = contract or corpus_coverage_contract_v2()
    if value.contract_id != CORPUS_COVERAGE_V2_ID or value.version != CORPUS_COVERAGE_V2_VERSION:
        raise CorpusCoverageV2Error("COVERAGE_CONTRACT_IDENTITY_MISMATCH")
    if value.input_representation_id != INPUT_REPRESENTATION_V2_ID or value.input_representation_version != INPUT_REPRESENTATION_V2_VERSION:
        raise CorpusCoverageV2Error("INPUT_V2_BINDING_MISMATCH")
    if value.evidence_projection_id != EVIDENCE_PROJECTION_V2_ID or value.evidence_projection_version != EVIDENCE_PROJECTION_V2_VERSION:
        raise CorpusCoverageV2Error("EVIDENCE_PROJECTION_V2_BINDING_MISMATCH")
    if value.sequence_authority_id != SEQUENCE_LENGTH_AUTHORITY_ID or value.sequence_authority_version != SEQUENCE_LENGTH_AUTHORITY_VERSION:
        raise CorpusCoverageV2Error("SEQUENCE_AUTHORITY_V2_BINDING_MISMATCH")
    if tuple(value.required_isolation_units) != tuple(unit.value for unit in REQUIRED_ISOLATION_UNITS_V2):
        raise CorpusCoverageV2Error("PARTITION_LEAKAGE_V2_BINDING_MISMATCH")
    if value.same_decision_context_across_splits_allowed:
        raise CorpusCoverageV2Error("DECISION_CONTEXT_CROSS_SPLIT_MUST_BE_FORBIDDEN")
    if not value.heuristic_target_creation:
        raise CorpusCoverageV2Error("HEURISTIC_TARGET_CREATION_MUST_BE_DISABLED")
    sequence_length_authority_v2().validate_model_position_capacity()


def candidate_shortcut_diagnostic(report: CoverageReportV2) -> str:
    reject_count = report.secondary_counts.get("REJECT", 0)
    if reject_count == 0:
        return "NOT_APPLICABLE"
    reject_candidates = report.target_candidate_example_counts.get("REJECT", {})
    largest = max(reject_candidates.values()) if reject_candidates else 0
    share = largest / reject_count
    if len(report.candidate_counts) <= 2 and share >= 0.5:
        return "HIGH"
    if share >= 0.5:
        return "MEDIUM"
    return "LOW"


__all__ = [
    "CONTRASTIVE_GROUPING_POLICY_V2",
    "CORPUS_COVERAGE_V2_ID",
    "CORPUS_COVERAGE_V2_VERSION",
    "CorpusCoverageContractV2",
    "CorpusCoverageV2Error",
    "CorpusExampleV2",
    "CoverageAssessmentV2",
    "CoverageReportV2",
    "DEFINED_NEGATIVE_BOUNDARY_TYPES_V1",
    "PRIMARY_TARGETS_V2",
    "SECONDARY_TARGETS_V2",
    "assess_coverage",
    "build_coverage_report",
    "candidate_shortcut_diagnostic",
    "corpus_coverage_contract_v2",
    "validate_contract_bindings_v2",
    "validate_sequence_lengths",
    "validate_target_presence",
    "validate_validation_measurability",
]
