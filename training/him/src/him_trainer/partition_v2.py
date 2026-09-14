"""Deterministic GROUP_THEN_STRATIFY partitioning for the HIM P2 corpus V2.

This module assigns complete target-independent isolation components.  It is
deliberately model-free and never uses Holdout material or semantic heuristics.
"""

from __future__ import annotations

from dataclasses import replace
from typing import Sequence

from .corpus_coverage_v2 import CorpusExampleV2
from .partition_leakage_v2 import (
    REQUIRED_ISOLATION_UNITS_V2,
    connected_components,
    isolation_group_reference_v2,
    validate_partition_assignments,
)


PARTITION_STRATEGY_V2 = "GROUP_THEN_STRATIFY"
PARTITION_SEED_V2 = "LEXICOGRAPHIC_DETERMINISTIC_V2"
TRAIN_SPLIT_V2 = "TRAIN"
VALIDATION_SPLIT_V2 = "VALIDATION"


def _component_reference(component: Sequence[object]) -> str:
    refs = sorted(isolation_group_reference_v2(item.isolation_example) for item in component)  # type: ignore[attr-defined]
    from hashlib import sha256
    import json

    encoded = json.dumps(refs, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return "him-p2-isolation-component:v2:" + sha256(encoded).hexdigest()


def assign_group_then_stratify(
    examples: Sequence[CorpusExampleV2],
    *,
    validation_fraction: float = 0.20,
) -> tuple[CorpusExampleV2, ...]:
    """Assign complete components, reserving measurable contrast groups.

    Complete primary and secondary contrast groups are selected in stable
    lexical order first.  Remaining whole components are added in stable
    component-reference order until the requested approximate fraction is
    reached.  No RNG or target-aware grouping is used.
    """

    if not examples:
        raise ValueError("CORPUS_EMPTY")
    if not 0.0 < validation_fraction < 1.0:
        raise ValueError("VALIDATION_FRACTION_INVALID")
    components = connected_components(
        tuple(item.isolation_example for item in examples),
        REQUIRED_ISOLATION_UNITS_V2,
    )
    by_record = {item.example_reference: item for item in examples}
    component_values: list[tuple[str, tuple[CorpusExampleV2, ...]]] = []
    for component in components:
        values = tuple(by_record[item.record_reference] for item in component)
        component_values.append((_component_reference(values), values))
    component_values.sort(key=lambda item: item[0])

    selected: set[str] = set()
    owner_by_group: dict[str, str] = {}
    for component_ref, values in component_values:
        for value in values:
            owner_by_group.setdefault(value.contrast_group, component_ref)
    primary_groups = sorted({value.contrast_group for value in examples if value.primary_target in {"IDENTITY", "VARIANT"}})
    secondary_groups = sorted({value.contrast_group for value in examples if value.secondary_target in {"COMPATIBLE", "REJECT"}})
    for group in (primary_groups, secondary_groups):
        complete = []
        for group_ref in group:
            values = [value for value in examples if value.contrast_group == group_ref]
            primaries = {value.primary_target for value in values if value.primary_target is not None}
            secondaries = {value.secondary_target for value in values if value.secondary_target is not None}
            if primaries == {"IDENTITY", "VARIANT"} or secondaries == {"COMPATIBLE", "REJECT"}:
                complete.append(group_ref)
        if complete:
            selected.add(owner_by_group[complete[0]])

    desired = max(1, round(len(examples) * validation_fraction))
    current = sum(len(values) for ref, values in component_values if ref in selected)
    for component_ref, values in component_values:
        if component_ref in selected:
            continue
        if current + len(values) <= desired:
            selected.add(component_ref)
            current += len(values)

    result = []
    for component_ref, values in component_values:
        split = VALIDATION_SPLIT_V2 if component_ref in selected else TRAIN_SPLIT_V2
        result.extend(replace(value, partition=split) for value in values)
    result.sort(key=lambda value: value.example_reference)
    validation = [value for value in result if value.partition == VALIDATION_SPLIT_V2]
    required = {value.primary_target for value in validation if value.primary_target} | {value.secondary_target for value in validation if value.secondary_target}
    if not {"IDENTITY", "VARIANT", "COMPATIBLE", "REJECT"}.issubset(required):
        raise ValueError("VALIDATION_MEASURABILITY_NOT_ACHIEVED")
    if any(value.partition not in {TRAIN_SPLIT_V2, VALIDATION_SPLIT_V2} for value in result):
        raise ValueError("UNSUPPORTED_SPLIT")
    if not validate_partition_assignments(tuple(value.isolation_example for value in result), REQUIRED_ISOLATION_UNITS_V2).valid:
        raise ValueError("PARTITION_LEAKAGE")
    return tuple(result)


__all__ = [
    "PARTITION_SEED_V2",
    "PARTITION_STRATEGY_V2",
    "TRAIN_SPLIT_V2",
    "VALIDATION_SPLIT_V2",
    "assign_group_then_stratify",
]
