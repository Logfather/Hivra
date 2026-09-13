"""Target-independent, source-faithful HIM input representation V2.

The representation in this module is deliberately separate from supervision
and from the historical V1 sequence construction.  It contains only values
that are available while constructing an inference request: the observed
term, source-faithful evidence, and candidate content.

No target, relation, compatibility, review, family, source identity, or
partition value is accepted as an input to the model-facing serializer.
"""

from __future__ import annotations

import re
import unicodedata
from dataclasses import dataclass
from typing import Any, Iterable, Mapping, Sequence


CONTRACT_ID = "HIM_INPUT_REPRESENTATION_V2"
CONTRACT_VERSION = "2"
MISSING_VALUE = "UNKNOWN"
MAX_SEQUENCE_LENGTH = 128

FIELD_ORDER = ("O", "L", "I", "K", "G", "T", "C", "N", "X")
MANDATORY_FIELDS = ("O", "L", "I", "C", "N", "X")
OPTIONAL_FIELDS = ("K", "G", "T")

CANDIDATE_CONDITIONING_V2_FIELDS = (
    "TERM",
    "NORMALIZED_TERM",
    "TAXONOMY_PATHS",
    "OPTIONAL_EXPLICIT_ALIASES",
)
OBSERVED_CONDITIONING_V2_FIELDS = (
    "TERM",
    "OPTIONAL_LANGUAGE",
    "SOURCE_FAITHFUL_LABEL",
    "COMPOSITION",
    "CATEGORIES",
    "FOOD_GROUPS",
    "PRODUCT_TYPE",
    "OPTIONAL_DESCRIPTION",
)

# These names are intentionally not used as serialized field names.  The
# scanner is also used by permanent tests as a fail-closed corpus invariant.
FORBIDDEN_MODEL_INPUT_FIELDS = (
    "TARGET_KIND",
    "CANDIDATE_COMPATIBILITY",
    "NEGATIVE_BOUNDARY",
    "REVIEW_VERDICT",
    "VALIDATION_TRUTH",
    "GOLD_RELATION",
    "EXPECTED_RELATION",
    "HUMAN_RESOLUTION",
    "APPROVED_LABEL",
    "EVALUATION_BUCKET",
    "EVALUATION_ROLE",
    "SECONDARY_ONLY",
    "RELATION",
    "TARGET_REFERENCE",
    "FAMILY_ID",
    "CANONICAL_ID",
    "CANDIDATE_ID",
    "SOURCE_ID",
    "SOURCE_RECORD_ID",
    "AUTHORITY_ID",
    "EVALUATION_ID",
)
FORBIDDEN_MODEL_INPUT_TOKENS = FORBIDDEN_MODEL_INPUT_FIELDS
FORBIDDEN_SOURCE_SHORTCUT_MARKERS = ("OPEN_FOOD_FACTS", "CIQUAL", "AGRIBALYSE")
_OPAQUE_REFERENCE = re.compile(
    r"(?:^|[^A-Za-z0-9_])(?:family:v1:|canonical-family:v1:|"
    r"(?:off|ciqual|agribalyse):|authority:v1:|p2-[A-Za-z0-9-]+:v1:)",
    re.IGNORECASE,
)


class InputRepresentationV2Error(ValueError):
    """Raised when an inference-constructible V2 input is invalid."""


def _normalise_text(value: object, *, required: bool, field: str) -> str:
    if not isinstance(value, str):
        if required:
            raise InputRepresentationV2Error(f"{field}_MUST_BE_TEXT")
        return MISSING_VALUE
    result = unicodedata.normalize("NFC", value).strip()
    if not result:
        if required:
            raise InputRepresentationV2Error(f"{field}_MUST_NOT_BE_EMPTY")
        return MISSING_VALUE
    return result


def _normalise_values(value: object, *, field: str) -> tuple[str, ...]:
    if value is None:
        return ()
    values: Iterable[object]
    if isinstance(value, str):
        values = (value,)
    elif isinstance(value, Sequence) and not isinstance(value, (bytes, bytearray)):
        values = value
    else:
        raise InputRepresentationV2Error(f"{field}_MUST_BE_A_SEQUENCE")
    normalised = tuple(_normalise_text(item, required=True, field=field) for item in values)
    return tuple(sorted(set(normalised)))


def _normalise_paths(value: object) -> tuple[tuple[str, ...], ...]:
    if not isinstance(value, Sequence) or isinstance(value, (str, bytes, bytearray)):
        raise InputRepresentationV2Error("TAXONOMY_PATHS_MUST_BE_A_SEQUENCE")
    paths: list[tuple[str, ...]] = []
    for path in value:
        if not isinstance(path, Sequence) or isinstance(path, (str, bytes, bytearray)):
            raise InputRepresentationV2Error("TAXONOMY_PATH_MUST_BE_A_SEQUENCE")
        segments = tuple(_normalise_text(item, required=True, field="TAXONOMY_SEGMENT") for item in path)
        if not segments:
            raise InputRepresentationV2Error("TAXONOMY_PATH_MUST_NOT_BE_EMPTY")
        paths.append(segments)
    result = tuple(sorted(set(paths)))
    if not result:
        raise InputRepresentationV2Error("TAXONOMY_PATHS_MUST_NOT_BE_EMPTY")
    return result


def _escape(value: str) -> str:
    """Keep the line-oriented contract unambiguous without changing normal text."""

    return (
        value.replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("|", "\\|")
    )


def _list_value(values: Sequence[str]) -> str:
    return MISSING_VALUE if not values else "|".join(_escape(value) for value in values)


def _mapping_value(mapping: Mapping[str, Any], *keys: str) -> object:
    for key in keys:
        if key in mapping and mapping[key] not in (None, ""):
            return mapping[key]
    return None


def _evidence_views(evidence: Mapping[str, Any]) -> tuple[Mapping[str, Any], ...]:
    views: list[Mapping[str, Any]] = [evidence]
    nested = evidence.get("evidence")
    projection = evidence.get("projection")
    if isinstance(nested, Mapping):
        views.append(nested)
    if isinstance(projection, Mapping):
        views.append(projection)
        identity = projection.get("identity")
        taxonomy = projection.get("taxonomy")
        if isinstance(identity, Mapping):
            views.append(identity)
        if isinstance(taxonomy, Mapping):
            views.append(taxonomy)
    return tuple(views)


def _first_evidence_text(evidence: Mapping[str, Any], *keys: str) -> str:
    for view in _evidence_views(evidence):
        value = _mapping_value(view, *keys)
        if isinstance(value, str) and value.strip():
            return value
    return MISSING_VALUE


def _first_evidence_list(evidence: Mapping[str, Any], *keys: str) -> tuple[str, ...]:
    for view in _evidence_views(evidence):
        for key in keys:
            if key in view and view[key] not in (None, [], ""):
                return _normalise_values(view[key], field=key)
    return ()


@dataclass(frozen=True)
class CandidateConditioningV2:
    """Candidate content available without any target or supervision."""

    term: str
    normalized_term: str
    taxonomy_paths: tuple[tuple[str, ...], ...]
    explicit_aliases: tuple[str, ...] = ()

    def __post_init__(self) -> None:
        object.__setattr__(self, "term", _normalise_text(self.term, required=True, field="CANDIDATE_TERM"))
        object.__setattr__(self, "normalized_term", _normalise_text(self.normalized_term, required=True, field="NORMALIZED_TERM"))
        object.__setattr__(self, "taxonomy_paths", _normalise_paths(self.taxonomy_paths))
        object.__setattr__(self, "explicit_aliases", _normalise_values(self.explicit_aliases, field="ALIASES"))

    @classmethod
    def from_mapping(cls, value: Mapping[str, Any]) -> "CandidateConditioningV2":
        term = _mapping_value(value, "itemname", "canonicalName", "candidateCanonicalTerm", "term")
        normalised = _mapping_value(value, "normalized", "candidateNormalizedTerm", "normalizedTerm")
        paths = _mapping_value(value, "taxonomyPaths", "candidateTaxonomyPaths")
        aliases = _mapping_value(value, "aliases", "explicitAliases") or ()
        return cls(
            term=_normalise_text(term, required=True, field="CANDIDATE_TERM"),
            normalized_term=_normalise_text(normalised, required=True, field="NORMALIZED_TERM"),
            taxonomy_paths=_normalise_paths(paths),
            explicit_aliases=_normalise_values(aliases, field="ALIASES"),
        )


@dataclass(frozen=True)
class ObservedConditioningV2:
    """Source-faithful observed evidence; missing optional evidence is explicit."""

    term: str
    source_faithful_label: str = MISSING_VALUE
    composition: str = MISSING_VALUE
    categories: tuple[str, ...] = ()
    food_groups: tuple[str, ...] = ()
    product_type: str = MISSING_VALUE
    language: str = MISSING_VALUE
    description: str = MISSING_VALUE

    def __post_init__(self) -> None:
        object.__setattr__(self, "term", _normalise_text(self.term, required=True, field="OBSERVED_TERM"))
        for name in ("source_faithful_label", "composition", "product_type", "language", "description"):
            object.__setattr__(self, name, _normalise_text(getattr(self, name), required=False, field=name.upper()))
        object.__setattr__(self, "categories", _normalise_values(self.categories, field="CATEGORIES"))
        object.__setattr__(self, "food_groups", _normalise_values(self.food_groups, field="FOOD_GROUPS"))

    @classmethod
    def from_mapping(cls, value: Mapping[str, Any]) -> "ObservedConditioningV2":
        return cls(
            term=_mapping_value(value, "observedTerm", "term"),
            source_faithful_label=_first_evidence_text(value, "sourceFaithfulLabel", "productName", "nameEn", "nameFr"),
            composition=_first_evidence_text(value, "ingredientText", "composition", "ingredients"),
            categories=_first_evidence_list(value, "categories"),
            food_groups=_first_evidence_list(value, "foodGroups"),
            product_type=_first_evidence_text(value, "productType"),
            language=_first_evidence_text(value, "language"),
            description=_first_evidence_text(value, "description", "genericName"),
        )


@dataclass(frozen=True)
class HimInputRepresentationV2:
    """The complete model-facing V2 input, with no supervision fields."""

    observed: ObservedConditioningV2
    candidate: CandidateConditioningV2

    def serialize(self) -> str:
        values = {
            "O": self.observed.term,
            "L": self.observed.source_faithful_label,
            "I": self.observed.composition,
            "K": _list_value(self.observed.categories),
            "G": _list_value(self.observed.food_groups),
            "T": self.observed.product_type,
            "C": self.candidate.term,
            "N": self.candidate.normalized_term,
            "X": _list_value(tuple("/".join(path) for path in self.candidate.taxonomy_paths)),
        }
        return "\n".join(("<HIMV2>", *(f"{field}={_escape(values[field])}" for field in FIELD_ORDER), "</HIMV2>"))

    def utf8(self) -> bytes:
        return self.serialize().encode("utf-8")

    @classmethod
    def from_record(
        cls,
        record: Mapping[str, Any],
        candidate: CandidateConditioningV2 | Mapping[str, Any],
    ) -> "HimInputRepresentationV2":
        """Build from a source/example record while ignoring supervision metadata."""

        candidate_value = candidate if isinstance(candidate, CandidateConditioningV2) else CandidateConditioningV2.from_mapping(candidate)
        evidence = record.get("sourceEvidence")
        if not isinstance(evidence, Mapping):
            evidence = record
        observed = ObservedConditioningV2(
            term=record.get("observedTerm", record.get("term")),
            source_faithful_label=_first_evidence_text(
                evidence,
                "sourceFaithfulLabel",
                "productName",
                "productNameEnglish",
                "productNameGerman",
                "nameEn",
                "nameFr",
            ),
            composition=_first_evidence_text(evidence, "ingredientText", "composition", "ingredients"),
            categories=_first_evidence_list(evidence, "categories"),
            food_groups=_first_evidence_list(evidence, "foodGroups"),
            product_type=_first_evidence_text(evidence, "productType"),
            language=_first_evidence_text(evidence, "language"),
            description=_first_evidence_text(evidence, "description", "genericName"),
        )
        return cls(observed=observed, candidate=candidate_value)


def build_input_representation_v2(
    observed_term: str,
    candidate: CandidateConditioningV2 | Mapping[str, Any],
    source_evidence: Mapping[str, Any],
) -> HimInputRepresentationV2:
    """Inference-shaped constructor; it has no supervision argument."""

    record = {"observedTerm": observed_term, "sourceEvidence": source_evidence}
    return HimInputRepresentationV2.from_record(record, candidate)


def validate_no_conflicting_targets(
    representations_and_targets: Iterable[tuple[bytes | str, object]],
) -> int:
    """Return conflicting representation groups; callers must fail closed on non-zero."""

    groups: dict[bytes, set[object]] = {}
    for representation, target in representations_and_targets:
        key = representation.encode("utf-8") if isinstance(representation, str) else bytes(representation)
        groups.setdefault(key, set()).add(target)
    return sum(len(targets) > 1 for targets in groups.values())


def assert_no_conflicting_targets(representations_and_targets: Iterable[tuple[bytes | str, object]]) -> None:
    conflicts = validate_no_conflicting_targets(representations_and_targets)
    if conflicts:
        raise InputRepresentationV2Error(f"CONFLICTING_TARGET_GROUPS:{conflicts}")


def scan_model_input(representations: Iterable[HimInputRepresentationV2]) -> dict[str, int]:
    """Scan only generated text for forbidden technical or opaque shortcuts."""

    texts = tuple(item.serialize() for item in representations)
    technical = sum(sum(token in text for token in FORBIDDEN_MODEL_INPUT_TOKENS) for text in texts)
    source_markers = sum(sum(marker in text for marker in FORBIDDEN_SOURCE_SHORTCUT_MARKERS) for text in texts)
    opaque = sum(bool(_OPAQUE_REFERENCE.search(text)) for text in texts)
    return {
        "MODEL_INPUT_TECHNICAL_TARGET_TOKEN_LEAK_COUNT": technical,
        "SOURCE_ID_SHORTCUT_MARKER_COUNT": source_markers,
        "OPAQUE_SHORTCUT_IDENTIFIER_COUNT": opaque,
    }


FUTURE_CORPUS_REQUIREMENTS = {
    "IDENTITY_REQUIRES_CLOSE_VARIANT_CONTRASTS": True,
    "VARIANT_REQUIRES_CLOSE_IDENTITY_CONTRASTS": True,
    "COMPATIBLE_REQUIRES_HARD_REJECT_CONTRASTS": True,
    "REJECT_REQUIRES_RELATED_BUT_INVALID_CONTRASTS": True,
    "REJECT_SUPERVISION_MUST_SPAN_MULTIPLE_FAMILIES": True,
    "REJECT_SUPERVISION_MUST_SPAN_MULTIPLE_BOUNDARY_TYPES": True,
    "VALIDATION_REJECT_COUNT_MUST_BE_NONZERO": True,
}


def token_budget(lengths: Iterable[int]) -> dict[str, int]:
    ordered = sorted(int(length) for length in lengths)
    if not ordered:
        raise InputRepresentationV2Error("TOKEN_BUDGET_EMPTY")
    p90_index = max(0, min(len(ordered) - 1, (9 * len(ordered) + 9) // 10 - 1))
    return {
        "min": ordered[0],
        "median": ordered[(len(ordered) - 1) // 2],
        "p90": ordered[p90_index],
        "max": ordered[-1],
        "over128": sum(length > MAX_SEQUENCE_LENGTH for length in ordered),
    }


__all__ = [
    "CANDIDATE_CONDITIONING_V2_FIELDS",
    "CONTRACT_ID",
    "CONTRACT_VERSION",
    "FIELD_ORDER",
    "FORBIDDEN_MODEL_INPUT_FIELDS",
    "FORBIDDEN_MODEL_INPUT_TOKENS",
    "FUTURE_CORPUS_REQUIREMENTS",
    "HimInputRepresentationV2",
    "InputRepresentationV2Error",
    "MANDATORY_FIELDS",
    "MAX_SEQUENCE_LENGTH",
    "MISSING_VALUE",
    "OBSERVED_CONDITIONING_V2_FIELDS",
    "OPTIONAL_FIELDS",
    "CandidateConditioningV2",
    "ObservedConditioningV2",
    "assert_no_conflicting_targets",
    "build_input_representation_v2",
    "scan_model_input",
    "token_budget",
    "validate_no_conflicting_targets",
]
