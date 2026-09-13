"""Source-faithful HIM Evidence Projection V2.

This module is the narrow boundary between a retrieved source record and the
model-facing Input Representation V2.  It only copies source-native semantic
fields, applies representation-only normalization, and keeps audit lineage in
a separate object.  It never receives or emits supervision, target, family,
partition, authority, or source-record-kind values for model input.
"""

from __future__ import annotations

import hashlib
import json
import re
import unicodedata
from dataclasses import dataclass
from typing import Any, Mapping, Sequence


EVIDENCE_PROJECTION_V2_ID = "HIM_EVIDENCE_PROJECTION_V2"
EVIDENCE_PROJECTION_V2_VERSION = "2"
MISSING_VALUE = "UNKNOWN"
MAX_RAW_TEXT_CHARACTERS = 1_048_576
_SOURCE_NAMES = {"OPEN_FOOD_FACTS", "AGRIBALYSE", "CIQUAL", "GLYCEMIC_INDEX"}
_PROHIBITED_MODEL_KEYS = frozenset(
    {
        "target",
        "targetkind",
        "targetreference",
        "relation",
        "candidatecompatibility",
        "negativeboundary",
        "reviewverdict",
        "validationtruth",
        "familyid",
        "familyreference",
        "partition",
        "authorityid",
        "evaluationrole",
        "evaluationbucket",
        "sourcerecordkind",
        "sourceid",
        "sourcerecordid",
    }
)
_WHITESPACE = re.compile(r"[\t\r\n ]+")


class EvidenceProjectionV2Error(ValueError):
    """Raised when source evidence cannot be projected without invention."""


def _normalise_text(value: object, *, field: str, required: bool = False) -> str:
    if value is None:
        if required:
            raise EvidenceProjectionV2Error(f"{field}_MISSING")
        return MISSING_VALUE
    if not isinstance(value, str):
        raise EvidenceProjectionV2Error(f"{field}_MUST_BE_TEXT")
    if len(value) > MAX_RAW_TEXT_CHARACTERS:
        raise EvidenceProjectionV2Error(f"{field}_OVERSIZED")
    if "\x00" in value:
        raise EvidenceProjectionV2Error(f"{field}_UNSAFE_BINARY_CONTENT")
    # Line-ending and Unicode normalization are representation-only changes;
    # internal spacing is retained as source content, apart from line endings.
    result = unicodedata.normalize("NFC", value.replace("\r\n", "\n").replace("\r", "\n")).strip()
    if not result:
        if required:
            raise EvidenceProjectionV2Error(f"{field}_MUST_NOT_BE_EMPTY")
        return MISSING_VALUE
    return result


def _normalise_values(value: object, *, field: str) -> tuple[str, ...]:
    if value is None:
        return ()
    if isinstance(value, str):
        raw_values: Sequence[object] = (value,)
    elif isinstance(value, Sequence) and not isinstance(value, (bytes, bytearray)):
        raw_values = value
    else:
        raise EvidenceProjectionV2Error(f"{field}_MUST_BE_TEXT_OR_SEQUENCE")
    values = [_normalise_text(item, field=field, required=True) for item in raw_values]
    return tuple(sorted(set(values)))


def _mapping(value: object, *, field: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise EvidenceProjectionV2Error(f"{field}_MUST_BE_OBJECT")
    return value


def _optional_text(mapping: Mapping[str, Any], *keys: str, field: str) -> tuple[str, str | None]:
    for key in keys:
        if key in mapping and mapping[key] is not None:
            return _normalise_text(mapping[key], field=field), key
    return MISSING_VALUE, None


def _optional_list(mapping: Mapping[str, Any], *keys: str, field: str) -> tuple[tuple[str, ...], str | None]:
    for key in keys:
        if key in mapping and mapping[key] is not None:
            return _normalise_values(mapping[key], field=field), key
    return (), None


def _lexical_value(value: object, *, field: str) -> str:
    """Read the explicit lexical/status wrapper used by GI source values."""

    if value is None:
        return MISSING_VALUE
    wrapper = _mapping(value, field=field)
    status = wrapper.get("status")
    lexical = wrapper.get("lexicalValue")
    if status in (None, "MISSING", "ABSENT") and lexical in (None, "", " "):
        return MISSING_VALUE
    if status not in (None, "PRESENT", "MISSING", "ABSENT"):
        raise EvidenceProjectionV2Error(f"{field}_STATUS_INVALID")
    return _normalise_text(lexical, field=field)


def _reject_metadata_keys(record: Mapping[str, Any]) -> None:
    for key in record:
        compact = "".join(character for character in str(key).lower() if character.isalnum())
        if compact in _PROHIBITED_MODEL_KEYS:
            raise EvidenceProjectionV2Error(f"MODEL_METADATA_FIELD_REJECTED:{key}")


@dataclass(frozen=True)
class EvidenceProjectionProvenanceV2:
    """Non-model-visible source lineage for one projection."""

    source: str
    source_record_identity: str
    source_artifact: str
    source_field_paths: tuple[str, ...]
    projection_id: str = EVIDENCE_PROJECTION_V2_ID
    projection_version: str = EVIDENCE_PROJECTION_V2_VERSION


@dataclass(frozen=True)
class HimEvidenceProjectionV2:
    """Immutable semantic evidence fields accepted by Input V2."""

    source_faithful_label: str = MISSING_VALUE
    ingredient_or_composition: str = MISSING_VALUE
    categories: tuple[str, ...] = ()
    food_groups: tuple[str, ...] = ()
    product_type: str = MISSING_VALUE
    provenance: EvidenceProjectionProvenanceV2 | None = None

    def __post_init__(self) -> None:
        object.__setattr__(self, "source_faithful_label", _normalise_text(self.source_faithful_label, field="L"))
        object.__setattr__(self, "ingredient_or_composition", _normalise_text(self.ingredient_or_composition, field="I"))
        object.__setattr__(self, "categories", _normalise_values(self.categories, field="K"))
        object.__setattr__(self, "food_groups", _normalise_values(self.food_groups, field="G"))
        object.__setattr__(self, "product_type", _normalise_text(self.product_type, field="T"))

    @property
    def logical_digest(self) -> str:
        payload = {
            "projectionId": EVIDENCE_PROJECTION_V2_ID,
            "projectionVersion": EVIDENCE_PROJECTION_V2_VERSION,
            "sourceFaithfulLabel": self.source_faithful_label,
            "ingredientOrComposition": self.ingredient_or_composition,
            "categories": list(self.categories),
            "foodGroups": list(self.food_groups),
            "productType": self.product_type,
        }
        encoded = json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
        return hashlib.sha256(encoded).hexdigest()

    @property
    def reference(self) -> str:
        return f"him-evidence-projection:v2:{self.logical_digest}"

    def model_input_mapping(self) -> dict[str, object]:
        """Return only fields accepted by ``HimInputRepresentationV2``."""

        return {
            "sourceFaithfulLabel": self.source_faithful_label,
            "ingredientText": self.ingredient_or_composition,
            "categories": list(self.categories),
            "foodGroups": list(self.food_groups),
            "productType": self.product_type,
        }


def _provenance(
    source: str,
    record: Mapping[str, Any],
    paths: list[str],
    source_record_identity: str | None,
) -> EvidenceProjectionProvenanceV2:
    identity = source_record_identity
    if identity is None:
        identity_value = record.get("sourceRecordIdentity")
        if identity_value is not None and not isinstance(identity_value, str):
            raise EvidenceProjectionV2Error("SOURCE_RECORD_IDENTITY_MUST_BE_TEXT")
        identity = identity_value or "UNAVAILABLE"
    return EvidenceProjectionProvenanceV2(
        source=source,
        source_record_identity=identity,
        source_artifact=f"source-artifact:{source}:optimized",
        source_field_paths=tuple(sorted(set(paths))),
    )


def _project_open_food_facts(record: Mapping[str, Any], identity: str | None) -> HimEvidenceProjectionV2:
    source_identity = _mapping(record.get("identity", {}), field="identity")
    taxonomy = _mapping(record.get("taxonomy", {}), field="taxonomy")
    ingredients = _mapping(record.get("ingredients", {}), field="ingredients")
    label, label_key = _optional_text(source_identity, "genericName", "productName", field="L")
    composition, composition_key = _optional_text(ingredients, "text", field="I")
    categories, category_key = _optional_list(taxonomy, "categories", field="K")
    groups, group_key = _optional_list(taxonomy, "foodGroups", "pnnsGroups", field="G")
    product_type, type_key = _optional_text(source_identity, "productType", field="T")
    paths = [
        f"identity.{label_key}" if label_key else "",
        f"ingredients.{composition_key}" if composition_key else "",
        f"taxonomy.{category_key}" if category_key else "",
        f"taxonomy.{group_key}" if group_key else "",
        f"identity.{type_key}" if type_key else "",
    ]
    return HimEvidenceProjectionV2(label, composition, categories, groups, product_type, _provenance("OPEN_FOOD_FACTS", record, [p for p in paths if p], identity))


def _project_agribalyse(record: Mapping[str, Any], identity: str | None) -> HimEvidenceProjectionV2:
    label, label_key = _optional_text(record, "productNameFr", "lciName", field="L")
    groups = _normalise_values(tuple(value for value in (record.get("foodGroup"), record.get("foodSubgroup")) if value is not None), field="G")
    for key in ("foodGroup", "foodSubgroup"):
        if key in record and record[key] is not None and not isinstance(record[key], str):
            raise EvidenceProjectionV2Error(f"G_{key}_MUST_BE_TEXT")
    paths = [f"{label_key}" if label_key else ""] + [key for key in ("foodGroup", "foodSubgroup") if key in record and record[key] is not None]
    return HimEvidenceProjectionV2(label, MISSING_VALUE, (), groups, MISSING_VALUE, _provenance("AGRIBALYSE", record, [p for p in paths if p], identity))


def _project_ciqual(record: Mapping[str, Any], identity: str | None) -> HimEvidenceProjectionV2:
    label, label_key = _optional_text(record, "nameEn", "nameFr", field="L")
    groups = _normalise_values(
        tuple(value for value in (record.get("groupNameEn"), record.get("subgroupNameEn"), record.get("subSubgroupNameEn")) if value not in (None, "-")),
        field="G",
    )
    for key in ("groupNameEn", "subgroupNameEn", "subSubgroupNameEn"):
        if key in record and record[key] is not None and not isinstance(record[key], str):
            raise EvidenceProjectionV2Error(f"G_{key}_MUST_BE_TEXT")
    paths = [f"{label_key}" if label_key else ""] + [key for key in ("groupNameEn", "subgroupNameEn", "subSubgroupNameEn") if key in record and record[key] not in (None, "-")]
    return HimEvidenceProjectionV2(label, MISSING_VALUE, (), groups, MISSING_VALUE, _provenance("CIQUAL", record, [p for p in paths if p], identity))


def _project_glycemic_index(record: Mapping[str, Any], identity: str | None) -> HimEvidenceProjectionV2:
    label_key = "foodItem" if "foodItem" in record else "foodName"
    if label_key == "foodItem":
        label = _lexical_value(record.get(label_key), field="L")
    else:
        label, _ = _optional_text(record, "foodName", field="L")
    context = _mapping(record.get("sourceContext", {}), field="sourceContext")
    group_values = tuple(value for value in (context.get("majorCategory"), context.get("subcategory"), context.get("deeperHeading")) if value is not None)
    groups = _normalise_values(group_values, field="G")
    paths = [label_key] + [f"sourceContext.{key}" for key in ("majorCategory", "subcategory", "deeperHeading") if context.get(key) is not None]
    return HimEvidenceProjectionV2(label, MISSING_VALUE, (), groups, MISSING_VALUE, _provenance("GLYCEMIC_INDEX", record, paths, identity))


def project_evidence_v2(
    source: str,
    record: Mapping[str, Any],
    *,
    source_record_identity: str | None = None,
) -> HimEvidenceProjectionV2:
    """Project one optimized source record without semantic enrichment."""

    if source not in _SOURCE_NAMES:
        raise EvidenceProjectionV2Error("UNSUPPORTED_SOURCE")
    record = _mapping(record, field="SOURCE_RECORD")
    _reject_metadata_keys(record)
    if source == "OPEN_FOOD_FACTS":
        return _project_open_food_facts(record, source_record_identity)
    if source == "AGRIBALYSE":
        return _project_agribalyse(record, source_record_identity)
    if source == "CIQUAL":
        return _project_ciqual(record, source_record_identity)
    return _project_glycemic_index(record, source_record_identity)


def project_to_input_v2(
    observed_term: str,
    candidate: Mapping[str, Any] | Any,
    projection: HimEvidenceProjectionV2,
) -> Any:
    """Bind a projection explicitly to Input V2 without adding provenance."""

    from .input_representation_v2 import build_input_representation_v2

    return build_input_representation_v2(observed_term, candidate, projection.model_input_mapping())


__all__ = [
    "EVIDENCE_PROJECTION_V2_ID",
    "EVIDENCE_PROJECTION_V2_VERSION",
    "EvidenceProjectionProvenanceV2",
    "EvidenceProjectionV2Error",
    "HimEvidenceProjectionV2",
    "MISSING_VALUE",
    "MAX_RAW_TEXT_CHARACTERS",
    "project_evidence_v2",
    "project_to_input_v2",
]
