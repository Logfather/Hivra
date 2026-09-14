"""Model-free eligibility gates after P2 V2 human adjudication.

This module validates authority identity, supervision preservation, protected
material exclusion, model-input hygiene, and the committed V2 sequence
authority.  It does not materialize a corpus or compute a partition.
"""

from __future__ import annotations

from dataclasses import dataclass

from .sequence_length_authority_v2 import sequence_length_authority_v2


POST_REVIEW_ELIGIBILITY_V2_ID = "HIM_P2_POST_REVIEW_ELIGIBILITY_V2"
PRIMARY_DECISIONS_V2 = ("ID", "VAR")
SECONDARY_DECISIONS_V2 = ("COMPATIBLE", "REJECT")


class PostReviewEligibilityV2Error(ValueError):
    """Raised when a post-review eligibility gate fails closed."""


@dataclass(frozen=True)
class HumanAdjudicationBindingV2:
    review_packet_reference: str
    review_packet_logical_digest: str
    adjudication_reference: str
    adjudication_logical_digest: str
    adjudication_review_packet_reference: str
    adjudication_review_packet_logical_digest: str
    packet_review_unit_id: str
    adjudication_review_unit_id: str


def validate_human_adjudication_binding_v2(binding: HumanAdjudicationBindingV2) -> None:
    """Require packet, adjudication, and exact review-unit identity to match."""

    if not isinstance(binding, HumanAdjudicationBindingV2):
        raise PostReviewEligibilityV2Error("HUMAN_ADJUDICATION_BINDING_TYPE_INVALID")
    if binding.review_packet_reference != binding.adjudication_review_packet_reference:
        raise PostReviewEligibilityV2Error("REVIEW_PACKET_REFERENCE_MISMATCH")
    if binding.review_packet_logical_digest != binding.adjudication_review_packet_logical_digest:
        raise PostReviewEligibilityV2Error("REVIEW_PACKET_DIGEST_MISMATCH")
    if binding.packet_review_unit_id != binding.adjudication_review_unit_id:
        raise PostReviewEligibilityV2Error("REVIEW_UNIT_IDENTITY_MISMATCH")


@dataclass(frozen=True)
class PostReviewUnitV2:
    """One fully bound adjudicated unit, before corpus materialization."""

    binding: HumanAdjudicationBindingV2
    human_review_final: bool
    independent_validation_satisfied: bool
    source_record_bound: bool
    family_bound: bool
    evidence_projection_bound: bool
    decision_context_bound: bool
    input_v2_constructible: bool
    target_leakage: bool
    protected_review_collision: bool
    historical_expanded_collision: bool
    partition_leakage: bool
    sequence_length: int
    primary: str | None = None
    secondary: str | None = None
    negative_boundary: str | None = None


@dataclass(frozen=True)
class PostReviewEligibilityResultV2:
    eligible: bool
    failure_reasons: tuple[str, ...]
    primary: str | None
    secondary: str | None
    negative_boundary: str | None


def human_supervision_identity_v2(unit: PostReviewUnitV2) -> tuple[str | None, str | None, str | None]:
    """Return human targets exactly as authorized; never normalize or infer."""

    return unit.primary, unit.secondary, unit.negative_boundary


def evaluate_post_review_unit_v2(unit: PostReviewUnitV2) -> PostReviewEligibilityResultV2:
    """Evaluate all pre-corpus gates for one adjudicated unit."""

    validate_human_adjudication_binding_v2(unit.binding)
    failures: list[str] = []
    if not unit.human_review_final:
        failures.append("HUMAN_REVIEW_NOT_FINAL")
    if not unit.independent_validation_satisfied:
        failures.append("INDEPENDENT_VALIDATION_UNSATISFIED")
    for value, reason in (
        (unit.source_record_bound, "SOURCE_RECORD_UNBOUND"),
        (unit.family_bound, "FAMILY_UNBOUND"),
        (unit.evidence_projection_bound, "EVIDENCE_PROJECTION_UNBOUND"),
        (unit.decision_context_bound, "DECISION_CONTEXT_UNBOUND"),
        (unit.input_v2_constructible, "INPUT_V2_NOT_CONSTRUCTIBLE"),
    ):
        if not value:
            failures.append(reason)
    if unit.target_leakage:
        failures.append("TARGET_LEAKAGE")
    if unit.protected_review_collision:
        failures.append("PROTECTED_REVIEW_COLLISION")
    if unit.historical_expanded_collision:
        failures.append("HISTORICAL_EXPANDED_COLLISION")
    if unit.partition_leakage:
        failures.append("PARTITION_LEAKAGE")
    if unit.primary not in (None, *PRIMARY_DECISIONS_V2):
        failures.append("PRIMARY_DECISION_INVALID")
    if unit.secondary not in (None, *SECONDARY_DECISIONS_V2):
        failures.append("SECONDARY_DECISION_INVALID")
    if unit.secondary == "REJECT" and not unit.negative_boundary:
        failures.append("REJECT_BOUNDARY_MISSING")
    if unit.secondary != "REJECT" and unit.negative_boundary is not None:
        failures.append("BOUNDARY_WITHOUT_REJECT")
    try:
        sequence_length_authority_v2().validate_token_length(unit.sequence_length)
    except ValueError:
        failures.append("SEQUENCE_LENGTH_OVERFLOW")
    return PostReviewEligibilityResultV2(
        eligible=not failures,
        failure_reasons=tuple(failures),
        primary=unit.primary,
        secondary=unit.secondary,
        negative_boundary=unit.negative_boundary,
    )


def require_corpus_eligible_v2(unit: PostReviewUnitV2) -> None:
    result = evaluate_post_review_unit_v2(unit)
    if not result.eligible:
        raise PostReviewEligibilityV2Error("CORPUS_UNIT_NOT_ELIGIBLE:" + ",".join(result.failure_reasons))


__all__ = [
    "HumanAdjudicationBindingV2",
    "POST_REVIEW_ELIGIBILITY_V2_ID",
    "PostReviewEligibilityResultV2",
    "PostReviewEligibilityV2Error",
    "PostReviewUnitV2",
    "evaluate_post_review_unit_v2",
    "human_supervision_identity_v2",
    "require_corpus_eligible_v2",
    "validate_human_adjudication_binding_v2",
]
