"""Deterministic, model-independent Final Evaluation V2 execution support.

This module is deliberately separate from the frozen scientific artifacts.  It
projects an already selected holdout into the same HIMV2 representation used
by training, owns write-once output schemas, and keeps exposure state explicit.
It does not select records, alter labels, or discover candidates.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping, Sequence

from .input_representation_v2 import build_input_representation_v2


SCHEMA = "HIM_FINAL_EVALUATION_V2_ENGINE"
VERSION = 1
PRIMARY_ORDER = ("EXISTING_CANONICAL", "IDENTITY", "VARIANT", "ALIAS", "NEW_CANONICAL")
SECONDARY_ORDER = ("COMPATIBLE", "REJECT")
PRIMARY_CODES = {name: index for index, name in enumerate(PRIMARY_ORDER)}
SECONDARY_CODES = {name: index for index, name in enumerate(SECONDARY_ORDER)}


class FinalEvaluationV2Error(ValueError):
    pass


def canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def logical_digest(value: Any) -> str:
    return hashlib.sha256(canonical(value)).hexdigest()


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise FinalEvaluationV2Error(message)


def envelope(prefix: str, payload: Mapping[str, Any]) -> dict[str, Any]:
    digest = logical_digest(payload)
    return {"reference": f"{prefix}:v1:{digest}", "logicalDigest": digest, "payload": dict(payload)}


def validate_envelope(value: Mapping[str, Any], *, prefix: str | None = None) -> dict[str, Any]:
    payload = value.get("payload")
    digest = value.get("logicalDigest")
    _require(isinstance(payload, Mapping), "PAYLOAD_INVALID")
    _require(isinstance(digest, str) and logical_digest(payload) == digest, "DIGEST_INVALID")
    reference = value.get("reference")
    _require(reference == f"{prefix}:v1:{digest}" if prefix else isinstance(reference, str) and reference.endswith(f":{digest}"), "REFERENCE_INVALID")
    return dict(value)


def _evidence_mapping(value: Any) -> dict[str, Any]:
    if isinstance(value, Mapping):
        return dict(value)
    return {"sourceFaithfulLabel": str(value)}


def _candidate_for_family(family: Mapping[str, Any]) -> dict[str, Any]:
    canonical_id = family.get("canonicalId")
    if isinstance(canonical_id, Mapping):
        canonical_id = canonical_id.get("value")
    name = family.get("canonicalName") or family.get("itemname")
    normalized = family.get("normalizedName") or family.get("normalizedTerm")
    paths = family.get("taxonomyPaths")
    _require(isinstance(name, str) and isinstance(normalized, str), "CANDIDATE_AUTHORITY_INVALID")
    _require(isinstance(paths, Sequence) and not isinstance(paths, (str, bytes)), "CANDIDATE_TAXONOMY_INVALID")
    return {"canonicalName": name, "normalizedName": normalized, "taxonomyPaths": paths, "canonicalId": canonical_id}


def project_model_input(record: Mapping[str, Any], candidate: Mapping[str, Any]) -> dict[str, Any]:
    """Build an execution input through the exact training constructor."""

    input_view = record.get("inputRepresentationV2")
    _require(isinstance(input_view, Mapping), "OBSERVED_INPUT_VIEW_MISSING")
    observed = input_view.get("observedTerm")
    evidence = _evidence_mapping(input_view.get("sourceFaithfulEvidence"))
    candidate_input = {
        "itemname": candidate.get("canonicalName", candidate.get("itemname")),
        "normalized": candidate.get("normalizedName", candidate.get("normalizedTerm", candidate.get("normalized"))),
        "taxonomyPaths": candidate.get("taxonomyPaths", candidate.get("candidateTaxonomyPaths")),
        "aliases": candidate.get("aliases", candidate.get("explicitAliases", ())),
    }
    value = build_input_representation_v2(observed, candidate_input, evidence)
    serialized = value.serialize()
    return {
        "recordId": record.get("recordId"),
        "sourceRecordId": record.get("sourceRecordId"),
        "serialized": serialized,
        "modelInputSha256": hashlib.sha256(serialized.encode("utf-8")).hexdigest(),
        "candidateConditioning": {
            "term": value.candidate.term,
            "normalizedTerm": value.candidate.normalized_term,
            "taxonomyPaths": [list(path) for path in value.candidate.taxonomy_paths],
            "source": "FROZEN_CANONICAL_FAMILY_AUTHORITY",
        },
    }


def build_model_input_projection_authority(parent_reference: str, parent_digest: str, rows: Sequence[Mapping[str, Any]]) -> dict[str, Any]:
    payload = {
        "schema": "HIM_FINAL_EVALUATION_V2_MODEL_INPUT_PROJECTION_AUTHORITY",
        "version": VERSION,
        "state": "FROZEN_DERIVATION_AUTHORITY",
        "parentReference": parent_reference,
        "parentLogicalDigest": parent_digest,
        "builder": "him_trainer.input_representation_v2.build_input_representation_v2",
        "inputRepresentationId": "HIM_INPUT_REPRESENTATION_V2",
        "fieldOrder": ["O", "L", "I", "K", "G", "T", "C", "N", "X"],
        "modelIndependent": True,
        "groundTruthIndependent": True,
        "records": [{"recordId": row["recordId"], "modelInputSha256": row["modelInputSha256"]} for row in rows],
    }
    return envelope("him-final-evaluation-v2-model-input-projection-authority", payload)


def build_derived_execution_artifact(
    holdout: Mapping[str, Any],
    projection_authority: Mapping[str, Any],
    projected_rows: Sequence[Mapping[str, Any]],
) -> dict[str, Any]:
    holdout_payload = holdout["payload"]
    records = holdout_payload["records"]
    _require([row["recordId"] for row in records] == [row["recordId"] for row in projected_rows], "HOLDOUT_ORDER_CHANGED")
    payload = {
        "schema": "HIM_FINAL_EVALUATION_V2_DERIVED_EXECUTION_ARTIFACT_V1",
        "version": VERSION,
        "state": "SEALED_UNEXPOSED",
        "modelIndependent": True,
        "holdoutReference": holdout["reference"],
        "holdoutLogicalDigest": holdout["logicalDigest"],
        "recordCount": len(records),
        "recordIds": [row["recordId"] for row in records],
        "groundTruthReference": holdout_payload["records"][0]["groundTruthReference"],
        "humanReviewResultReference": holdout_payload["records"][0]["humanReviewResultReference"],
        "familyLineageReference": holdout_payload["records"][0]["familyLineageReference"],
        "projectionAuthorityReference": projection_authority["reference"],
        "projectionAuthorityLogicalDigest": projection_authority["logicalDigest"],
        "derivation": "frozen-lineage-v1",
        "modelInputs": list(projected_rows),
        "holdoutOpened": False,
        "exposureCount": 0,
    }
    return envelope("him-final-evaluation-v2-derived-execution-artifact", payload)


def mask_secondary(target: str | None) -> bool:
    if target is None or target == "NOT_APPLICABLE":
        return False
    _require(target in SECONDARY_CODES, "SECONDARY_TARGET_INVALID")
    return True


def score_predictions(rows: Sequence[Mapping[str, Any]], predictions: Sequence[Mapping[str, Any]]) -> dict[str, Any]:
    _require(len(rows) == len(predictions), "PREDICTION_COUNT_MISMATCH")
    primary_matrix = {actual: {guess: 0 for guess in PRIMARY_ORDER} for actual in PRIMARY_ORDER}
    secondary_matrix = {actual: {guess: 0 for guess in SECONDARY_ORDER} for actual in SECONDARY_ORDER}
    primary_active = primary_correct = secondary_active = secondary_correct = 0
    per_record: list[dict[str, Any]] = []
    for row, prediction in zip(rows, predictions):
        truth = row.get("groundTruth", {})
        primary = truth.get("targetKind")
        secondary = truth.get("candidateCompatibility")
        pp = prediction.get("primaryPrediction")
        sp = prediction.get("secondaryPrediction")
        if isinstance(pp, int) and 0 <= pp < len(PRIMARY_ORDER):
            pp = PRIMARY_ORDER[pp]
        if isinstance(sp, int) and 0 <= sp < len(SECONDARY_ORDER):
            sp = SECONDARY_ORDER[sp]
        record = {"recordId": row.get("recordId"), "primaryActive": primary is not None, "secondaryActive": mask_secondary(secondary), "primaryPrediction": pp, "secondaryPrediction": sp}
        if primary in PRIMARY_CODES:
            primary_active += 1
            primary_matrix[primary][pp] = primary_matrix[primary].get(pp, 0) + 1
            record["primaryCorrect"] = pp == primary
            primary_correct += int(pp == primary)
        else:
            record["primaryCorrect"] = None
        if mask_secondary(secondary):
            secondary_active += 1
            secondary_matrix[secondary][sp] = secondary_matrix[secondary].get(sp, 0) + 1
            record["secondaryCorrect"] = sp == secondary
            secondary_correct += int(sp == secondary)
        else:
            record["secondaryCorrect"] = None
        per_record.append(record)
    return {"primaryActiveCount": primary_active, "primaryCorrectCount": primary_correct, "primaryAccuracy": primary_correct / primary_active if primary_active else None, "primaryConfusionMatrix": primary_matrix, "secondaryActiveCount": secondary_active, "secondaryCorrectCount": secondary_correct, "secondaryAccuracy": secondary_correct / secondary_active if secondary_active else None, "secondaryConfusionMatrix": secondary_matrix, "perRecord": per_record, "passFailThreshold": "NOT_DEFINED"}


@dataclass
class ExactlyOnceState:
    productive_invocation_count: int = 0
    exposure_count: int = 0
    model_deserialization_count: int = 0
    forward_count: int = 0
    state: str = "SEALED_UNEXPOSED"

    def authorize_invocation(self) -> None:
        _require(self.productive_invocation_count == 0 and self.exposure_count == 0, "SECOND_PRODUCTIVE_INVOCATION_FAILS_CLOSED")
        self.productive_invocation_count = 1

    def expose_once(self) -> None:
        _require(self.productive_invocation_count == 1 and self.exposure_count == 0, "EXPOSURE_TRANSITION_INVALID")
        self.exposure_count = 1
        self.state = "EXPOSED_CONSUMED"

    def persist(self, path: str | Path) -> None:
        destination = Path(path)
        _require(not destination.exists(), "EXECUTION_STATE_ALREADY_EXISTS")
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(canonical({"schema": "HIM_FINAL_EVALUATION_V2_EXACTLY_ONCE_STATE_V1", "version": 1, **self.__dict__}) + b"\n")


def persist_write_once(path: str | Path, prefix: str, payload: Mapping[str, Any]) -> dict[str, Any]:
    destination = Path(path)
    _require(not destination.exists(), f"OUTPUT_ALREADY_EXISTS:{destination.name}")
    value = envelope(prefix, payload)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(canonical(value) + b"\n")
    reloaded = json.loads(destination.read_text(encoding="utf-8"))
    validate_envelope(reloaded)
    return reloaded


__all__ = ["ExactlyOnceState", "FinalEvaluationV2Error", "PRIMARY_CODES", "PRIMARY_ORDER", "SECONDARY_CODES", "SECONDARY_ORDER", "build_derived_execution_artifact", "build_model_input_projection_authority", "logical_digest", "mask_secondary", "persist_write_once", "project_model_input", "score_predictions", "validate_envelope"]
