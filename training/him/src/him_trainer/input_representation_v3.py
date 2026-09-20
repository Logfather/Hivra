"""Versioned contextual model input for future HIM V2 retraining.

The historical V2 serializers remain unchanged.  V3 adds only the
source-faithful context proven necessary to separate the Pomelos examples.
No supervision or authority labels are accepted by this serializer.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable

CONTRACT_ID = "HIM_V2_INPUT_REPRESENTATION_V3"
CONTRACT_VERSION = "3"
MISSING_VALUE = "UNKNOWN"
FIELD_ORDER = ("O", "C", "L", "K", "G")


def _text(value: str | None) -> str:
    value = MISSING_VALUE if value is None else str(value).strip()
    return value or MISSING_VALUE


def _values(values: Iterable[str] | None) -> tuple[str, ...]:
    if values is None:
        return ()
    return tuple(sorted({_text(value) for value in values if _text(value) != MISSING_VALUE}))


def _list_value(values: tuple[str, ...]) -> str:
    return "|".join(values) if values else MISSING_VALUE


def _escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("\n", "\\n").replace("|", "\\|")


@dataclass(frozen=True)
class ContextualInputV3:
    observed_term: str
    candidate_term: str
    source_faithful_label: str | None = None
    category_context: tuple[str, ...] = ()
    food_group_context: tuple[str, ...] = ()

    def serialize(self) -> str:
        values = {
            "O": _text(self.observed_term),
            "C": _text(self.candidate_term),
            "L": _text(self.source_faithful_label),
            "K": _list_value(_values(self.category_context)),
            "G": _list_value(_values(self.food_group_context)),
        }
        return "\n".join(
            ("<HIMV2V3>", *(f"{field}={_escape(values[field])}" for field in FIELD_ORDER), "</HIMV2V3>")
        )

    @classmethod
    def from_mapping(cls, record: dict[str, object]) -> "ContextualInputV3":
        return cls(
            observed_term=str(record.get("observedTerm", record.get("term", ""))),
            candidate_term=str(record.get("candidateName", record.get("candidateTerm", ""))),
            source_faithful_label=record.get("sourceFaithfulLabel"),
            category_context=tuple(str(value) for value in record.get("categories", ()) or ()),
            food_group_context=tuple(str(value) for value in record.get("foodGroups", ()) or ()),
        )


def serialize_contextual_input_v3(record: dict[str, object]) -> str:
    """Single serializer entry point shared by future training and evaluation."""
    return ContextualInputV3.from_mapping(record).serialize()
