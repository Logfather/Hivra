"""Model-independent visual evidence supplementation for prospective intake V1.

This module binds human-supplied local image bytes to the existing immutable
prospective intake ledger.  It deliberately records only conservative visual
observations; it performs no OCR, lookup, canonicalization, family assignment,
ground-truth assignment, or model access.
"""

from __future__ import annotations

import copy
import hashlib
import json
import shutil
from pathlib import Path
from typing import Any, Mapping

from him_trainer.prospective_record_admission_v1 import (
    INTAKE_CREATED_AT_UTC,
    INTAKE_SOURCE_REFERENCE,
    logical_digest,
)


REPOSITORY_ROOT = Path(__file__).resolve().parents[4]
VISUAL_SOURCE_ROOT = REPOSITORY_ROOT / "artifacts/him/final-evaluation-v2/prospective-human-intake-v1/visual-evidence"
DURABLE_EVIDENCE_ROOT = REPOSITORY_ROOT / "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/raw-evidence/visual"
ORIGINAL_LEDGER_PATH = REPOSITORY_ROOT / "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/intake-ledger.v1.json"
VISUAL_SOURCE_RELATIVE_ROOT = "artifacts/him/final-evaluation-v2/prospective-human-intake-v1/visual-evidence"
DURABLE_EVIDENCE_RELATIVE_ROOT = "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/raw-evidence/visual"
PARENT_REFERENCE = "him-final-evaluation-v2-prospective-intake-ledger:v1:13721e323716015bca8e2ced0f47cd7d5fdf67aa0bffc2a94a61bd22f970e656"
SUPPLEMENT_SCHEMA = "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_VISUAL_EVIDENCE_SUPPLEMENT_V1"
UPDATED_LEDGER_SCHEMA = "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_INTAKE_LEDGER_V1_SUPPLEMENTED"


VISUAL_DESCRIPTIONS = (
    "Stack of thin, crisp, rectangular baked bread slices with a perforated surface.",
    "A mound of finely ground brown spice powder on a white background.",
    "Several whole white button mushrooms, including visible caps and stems.",
    "A red-pink apple with a visible Pink Lady label.",
    "A round slice of salami with visible pale fat speckles.",
    "A white, soft spreadable dairy product in a bowl, with bread in the background.",
    "Pieces of fish in a white creamy sauce with visible green herb pieces.",
    "A white yogurt product in a retail cup visibly labelled in German as Greek-style yogurt.",
    "A glass of amber beer with a dense white head, surrounded by grain and hops.",
    "A retail bottle visibly labelled Southern Comfort Original.",
    "A sealed transparent bag containing multiple peeled shrimp pieces.",
    "Two cooked chicken leg portions on a plate.",
    "A bowl containing dark purple-black olives.",
    "A red-orange pepper spread or paste in a white bowl.",
    "Multiple small ring-shaped filled pasta pieces on a wooden surface.",
)


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _envelope(schema: str, payload: Mapping[str, Any], prefix: str) -> dict[str, Any]:
    digest = logical_digest(payload)
    return {"reference": f"{prefix}:v1:{digest}", "logicalDigest": digest, "payload": dict(payload)}


def _image_path(root: Path, index: int) -> Path:
    return root / f"pic_{index:02d}.jpg"


def build_supplement(source_root: str | Path = VISUAL_SOURCE_ROOT, durable_root: str | Path = DURABLE_EVIDENCE_ROOT) -> dict[str, Any]:
    source_root, durable_root = Path(source_root), Path(durable_root)
    records = []
    for index, description in enumerate(VISUAL_DESCRIPTIONS, 1):
        source = _image_path(source_root, index)
        if not source.is_file() or source.stat().st_size == 0:
            raise ValueError(f"VISUAL_EVIDENCE_MISSING:OBS-{index:03d}")
        data = source.read_bytes()
        digest = hashlib.sha256(data).hexdigest()
        destination = durable_root / source.name
        destination.parent.mkdir(parents=True, exist_ok=True)
        if destination.exists() and destination.read_bytes() != data:
            raise ValueError(f"VISUAL_EVIDENCE_DESTINATION_COLLISION:OBS-{index:03d}")
        if not destination.exists():
            shutil.copyfile(source, destination)
        if destination.read_bytes() != data:
            raise ValueError(f"VISUAL_EVIDENCE_BYTE_MISMATCH:OBS-{index:03d}")
        records.append({
            "observationKey": f"OBS-{index:03d}",
            "imageFilename": source.name,
            "mediaType": "image/jpeg",
            "sourcePath": f"{VISUAL_SOURCE_RELATIVE_ROOT}/{source.name}",
            "durableEvidencePath": f"{DURABLE_EVIDENCE_RELATIVE_ROOT}/{destination.name}",
            "sha256": digest,
            "byteCount": len(data),
            "humanSupplied": True,
            "visualDescription": description,
            "descriptionScope": "CONSERVATIVE_VISIBLE_APPEARANCE_ONLY",
            "ocrUsed": False,
            "externalEnrichmentUsed": False,
        })
    payload = {
        "schema": SUPPLEMENT_SCHEMA,
        "version": 1,
        "state": "PERSISTED_APPEND_ONLY_VISUAL_EVIDENCE",
        "parentLedgerReference": PARENT_REFERENCE,
        "sourceAuthorityReference": INTAKE_SOURCE_REFERENCE,
        "supplementCreatedAtUtc": INTAKE_CREATED_AT_UTC,
        "eventType": "HUMAN_SUPPLIED_VISUAL_EVIDENCE_SUPPLEMENT",
        "recordCount": len(records),
        "records": records,
        "scientificIsolation": {
            "modelUsed": False, "ocrUsed": False, "networkUsed": False,
            "canonicalAssignmentCount": 0, "familyAssignmentCount": 0,
            "groundTruthAssignmentCount": 0, "holdoutAccessCount": 0,
        },
    }
    return _envelope(SUPPLEMENT_SCHEMA, payload, "him-final-evaluation-v2-prospective-visual-evidence-supplement")


def validate_supplement(value: Mapping[str, Any]) -> dict[str, Any]:
    payload = value.get("payload")
    if not isinstance(payload, Mapping) or logical_digest(payload) != value.get("logicalDigest"):
        raise ValueError("VISUAL_SUPPLEMENT_DIGEST_INVALID")
    if value.get("reference") != f"him-final-evaluation-v2-prospective-visual-evidence-supplement:v1:{value['logicalDigest']}":
        raise ValueError("VISUAL_SUPPLEMENT_REFERENCE_INVALID")
    records = payload.get("records")
    if payload.get("recordCount") != 15 or not isinstance(records, list) or len(records) != 15:
        raise ValueError("VISUAL_SUPPLEMENT_RECORD_COUNT_INVALID")
    if [row.get("observationKey") for row in records] != [f"OBS-{i:03d}" for i in range(1, 16)]:
        raise ValueError("VISUAL_SUPPLEMENT_ORDER_INVALID")
    if any(row.get("ocrUsed") or row.get("externalEnrichmentUsed") for row in records):
        raise ValueError("VISUAL_SUPPLEMENT_UNSUPPORTED_ENRICHMENT")
    return dict(value)


def build_updated_ledger(original: Mapping[str, Any], supplement: Mapping[str, Any]) -> dict[str, Any]:
    if original.get("reference") != PARENT_REFERENCE or original.get("logicalDigest") != PARENT_REFERENCE.rsplit(":", 1)[1]:
        raise ValueError("ORIGINAL_LEDGER_PARENT_MISMATCH")
    validate_supplement(supplement)
    payload = copy.deepcopy(original["payload"])
    visual_by_key = {row["observationKey"]: row for row in supplement["payload"]["records"]}
    for row in payload["records"]:
        visual = visual_by_key[row["observationKey"]]
        row["visualEvidenceReference"] = supplement["reference"]
        row["visualEvidenceLogicalDigest"] = visual["sha256"]
        row["visualEvidenceFilename"] = visual["imageFilename"]
        row["visualEvidencePath"] = visual["durableEvidencePath"]
        row["visualEvidenceDescription"] = visual["visualDescription"]
        row["evidenceSufficiency"] = "SUFFICIENT_FOR_LATER_REVIEW"
        row["evidenceSufficiencyReason"] = "Raw observation plus human-supplied readable visual evidence is available; no unsupported enrichment was used."
        row["finalIntakeState"] = "READY_FOR_FUTURE_HUMAN_REVIEW"
    payload["schema"] = UPDATED_LEDGER_SCHEMA
    payload["state"] = "PRODUCTIVE_PRE_GROUND_TRUTH_INTAKE_WITH_VISUAL_EVIDENCE"
    payload["acceptedRecordCount"] = 15
    payload["pendingEvidenceRecordCount"] = 0
    payload["evidenceSupplementReference"] = supplement["reference"]
    payload["evidenceSupplementLogicalDigest"] = supplement["logicalDigest"]
    payload["parentLedgerReference"] = original["reference"]
    payload["supplementEventCount"] = 1
    return _envelope(UPDATED_LEDGER_SCHEMA, payload, "him-final-evaluation-v2-prospective-intake-ledger-supplemented")


def validate_updated_ledger(value: Mapping[str, Any]) -> dict[str, Any]:
    payload = value.get("payload")
    if not isinstance(payload, Mapping) or logical_digest(payload) != value.get("logicalDigest"):
        raise ValueError("UPDATED_LEDGER_DIGEST_INVALID")
    if value.get("reference") != f"him-final-evaluation-v2-prospective-intake-ledger-supplemented:v1:{value['logicalDigest']}":
        raise ValueError("UPDATED_LEDGER_REFERENCE_INVALID")
    if payload.get("recordCount") != 15 or payload.get("acceptedRecordCount") != 15 or payload.get("pendingEvidenceRecordCount") != 0:
        raise ValueError("UPDATED_LEDGER_COUNTS_INVALID")
    if payload.get("parentLedgerReference") != PARENT_REFERENCE or payload.get("familyAssignmentCount") != 0 or payload.get("groundTruthAssignmentCount") != 0:
        raise ValueError("UPDATED_LEDGER_ISOLATION_INVALID")
    if any(row.get("groundTruthState") != "NOT_ASSIGNED" or row.get("modelExposureCount") != 0 or row.get("trainCollision") or row.get("validationCollision") for row in payload.get("records", [])):
        raise ValueError("UPDATED_LEDGER_RECORD_ISOLATION_INVALID")
    return dict(value)


def pre_ground_truth_population(value: Mapping[str, Any]) -> dict[str, Any]:
    ledger = validate_updated_ledger(value)
    ids = [row["intakeRecordId"] for row in ledger["payload"]["records"] if row["evidenceSufficiency"] == "SUFFICIENT_FOR_LATER_REVIEW"]
    return {"recordCount": len(ids), "recordIds": ids, "identityDigest": logical_digest(ids), "familyAssignmentCount": 0, "groundTruthAssignmentCount": 0}


def persist_and_reload(
    source_root: str | Path = VISUAL_SOURCE_ROOT,
    durable_root: str | Path = DURABLE_EVIDENCE_ROOT,
    original_ledger_path: str | Path = ORIGINAL_LEDGER_PATH,
    supplement_path: str | Path | None = None,
    updated_ledger_path: str | Path | None = None,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    original = json.loads(Path(original_ledger_path).read_text(encoding="utf-8"))
    supplement = build_supplement(source_root, durable_root)
    validate_supplement(supplement)
    updated = build_updated_ledger(original, supplement)
    supplement_path = Path(supplement_path or Path(durable_root).parent / "visual-evidence-supplement.v1.json")
    updated_ledger_path = Path(updated_ledger_path or Path(original_ledger_path).with_name("intake-ledger.supplemented.v1.json"))
    supplement_path.parent.mkdir(parents=True, exist_ok=True)
    updated_ledger_path.parent.mkdir(parents=True, exist_ok=True)
    supplement_path.write_bytes(_canonical(supplement) + b"\n")
    updated_ledger_path.write_bytes(_canonical(updated) + b"\n")
    reloaded_supplement = validate_supplement(json.loads(supplement_path.read_text(encoding="utf-8")))
    reloaded_updated = validate_updated_ledger(json.loads(updated_ledger_path.read_text(encoding="utf-8")))
    return reloaded_supplement, reloaded_updated, pre_ground_truth_population(reloaded_updated)
