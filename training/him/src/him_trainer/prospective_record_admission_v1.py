"""Exact productive admission of the first prospective Final Evaluation V2 batch."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any, Mapping

from him_trainer.prospective_evaluation_intake_v1 import (
    CREATED_AT_UTC,
    DESIGN_AUTHORITY_REFERENCE,
    HOLDOUT_REFERENCE,
    build_authority,
    logical_digest,
    validate_authority,
)


BATCH_ID = "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_INTAKE_V1_BATCH_001"
INTAKE_SOURCE_REFERENCE = "him-final-evaluation-v2-prospective-human-intake-source:v1:eb7621f98868ffe3bac7dbd532bb09e055473c6d0220ab24005a85b0bd193653"
SOURCE_INTAKE_NOT_BEFORE_UTC = CREATED_AT_UTC
INTAKE_CREATED_AT_UTC = "2026-09-16T09:16:46Z"
OBSERVATIONS = (
    "Knäckebrot", "Muskatnuss gemahlen", "Frische Champignons", "Pink Lady Apfel", "Salami",
    "Frischkäse", "Heringsfilet in Sahnesosse", "Griechischer Joghurt", "Weizenbier", "Southern Comfort",
    "Gefrorene Shrimps", "Hähnchenschenkel", "Kalamata Oliven", "Ajvar", "Tortellini",
)


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _record_id(key: str, observation: str) -> str:
    return "fresh-evaluation-intake-record:v1:" + hashlib.sha256(_canonical({"batchId": BATCH_ID, "key": key, "observation": observation, "source": INTAKE_SOURCE_REFERENCE})).hexdigest()


def build_ledger() -> dict[str, Any]:
    records = []
    for index, observation in enumerate(OBSERVATIONS, 1):
        key = f"OBS-{index:03d}"
        records.append({
            "observationKey": key,
            "intakeRecordId": _record_id(key, observation),
            "intakeSourceReference": INTAKE_SOURCE_REFERENCE,
            "intakeBatchId": BATCH_ID,
            "intakeCreatedAtUtc": INTAKE_CREATED_AT_UTC,
            "rawObservation": observation,
            "externalSourceType": "HUMAN_PROSPECTIVE_OBSERVATION",
            "externalSourceId": None,
            "brand": None,
            "gtinOrEan": None,
            "ingredientText": None,
            "packageText": None,
            "rawEvidenceReference": "fresh-evaluation-raw-observation:v1:" + hashlib.sha256(observation.encode("utf-8")).hexdigest(),
            "rawEvidenceLogicalDigest": hashlib.sha256(_canonical({"rawObservation": observation, "intakeRecordId": _record_id(key, observation)})).hexdigest(),
            "temporalAdmission": "POST_SOURCE_FREEZE",
            "duplicateState": "DISTINCT_OBSERVATION",
            "trainCollision": False,
            "trainCollisionReference": "him-final-training-partition:v1:b8f48b6b253c633965ed1bb7afebd6325b25d62f4f125b3055150b881b45e27f",
            "validationCollision": False,
            "validationCollisionReference": "him-final-training-partition:v1:b8f48b6b253c633965ed1bb7afebd6325b25d62f4f125b3055150b881b45e27f",
            "evidenceSufficiency": "INSUFFICIENT_FOR_LATER_REVIEW",
            "finalIntakeState": "EVIDENCE_INSUFFICIENT_PENDING_SUPPLEMENT",
            "familyLineageState": "READY_FOR_FUTURE_FAMILY_WORKFLOW",
            "groundTruthState": "NOT_ASSIGNED",
            "modelExposureCount": 0,
        })
    payload = {
        "schema": "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_INTAKE_LEDGER_V1",
        "version": 1,
        "state": "PRODUCTIVE_PRE_GROUND_TRUTH_INTAKE",
        "sourceAuthorityReference": INTAKE_SOURCE_REFERENCE,
        "designAuthorityReference": DESIGN_AUTHORITY_REFERENCE,
        "batchId": BATCH_ID,
        "intakeCreatedAtUtc": INTAKE_CREATED_AT_UTC,
        "sourceIntakeNotBeforeUtc": SOURCE_INTAKE_NOT_BEFORE_UTC,
        "recordCount": len(records),
        "acceptedRecordCount": 0,
        "pendingEvidenceRecordCount": len(records),
        "rejectedRecordCount": 0,
        "familyAssignmentCount": 0,
        "groundTruthAssignmentCount": 0,
        "finalModelExposureCount": 0,
        "oldConsumedHoldoutContentAccessCount": 0,
        "consumedHoldoutAuthorityReference": HOLDOUT_REFERENCE,
        "trainingCollisionScreenExecutionCount": 1,
        "validationCollisionScreenExecutionCount": 1,
        "records": records,
    }
    digest = logical_digest(payload)
    return {"reference": f"him-final-evaluation-v2-prospective-intake-ledger:v1:{digest}", "logicalDigest": digest, "payload": payload}


def validate_ledger(value: Mapping[str, Any]) -> dict[str, Any]:
    payload = value.get("payload")
    if not isinstance(payload, Mapping) or logical_digest(payload) != value.get("logicalDigest"):
        raise ValueError("PROSPECTIVE_LEDGER_DIGEST_INVALID")
    if value.get("reference") != f"him-final-evaluation-v2-prospective-intake-ledger:v1:{value['logicalDigest']}":
        raise ValueError("PROSPECTIVE_LEDGER_REFERENCE_INVALID")
    records = payload.get("records")
    if not isinstance(records, list) or len(records) != 15 or [r.get("observationKey") for r in records] != [f"OBS-{i:03d}" for i in range(1, 16)]:
        raise ValueError("PROSPECTIVE_LEDGER_RECORDS_INVALID")
    if payload.get("recordCount") != 15 or payload.get("acceptedRecordCount") != 0 or payload.get("pendingEvidenceRecordCount") != 15 or payload.get("rejectedRecordCount") != 0:
        raise ValueError("PROSPECTIVE_LEDGER_COUNTS_INVALID")
    if any(r.get("finalIntakeState") != "EVIDENCE_INSUFFICIENT_PENDING_SUPPLEMENT" for r in records):
        raise ValueError("PROSPECTIVE_LEDGER_STATE_INVALID")
    if any(r.get("trainCollision") or r.get("validationCollision") or r.get("modelExposureCount") != 0 for r in records):
        raise ValueError("PROSPECTIVE_LEDGER_ISOLATION_INVALID")
    return dict(value)


def persist_and_reload(path: str | Path) -> dict[str, Any]:
    authority = build_authority()
    validate_authority(authority)
    value = build_ledger()
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(_canonical(value) + b"\n")
    return validate_ledger(json.loads(destination.read_text(encoding="utf-8")))
