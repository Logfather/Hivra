"""Source-faithful, model-blind Human Review packet for prospective intake V1."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Mapping

from him_trainer.corpus_coverage_v2 import DEFINED_NEGATIVE_BOUNDARY_TYPES_V1
from him_trainer.prospective_evaluation_intake_v1 import validate_authority
from him_trainer.prospective_record_admission_v1 import logical_digest
from him_trainer.prospective_visual_evidence_supplement_v1 import (
    PARENT_REFERENCE,
    validate_updated_ledger,
)
from him_trainer.protocol_v1 import CandidateCompatibility, TargetKind


REPOSITORY_ROOT = Path(__file__).resolve().parents[4]
SOURCE_AUTHORITY_PATH = REPOSITORY_ROOT / "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/prospective-source-authority.v1.json"
UPDATED_LEDGER_PATH = REPOSITORY_ROOT / "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/intake-ledger.supplemented.v1.json"
VISUAL_ROOT = REPOSITORY_ROOT / "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/raw-evidence/visual"
PACKET_PATH = REPOSITORY_ROOT / "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/human-review-packet.v1.json"
PACKET_PREFIX = "him-final-evaluation-v2-prospective-human-review-packet"
SOURCE_REFERENCE = "him-final-evaluation-v2-prospective-human-intake-source:v1:eb7621f98868ffe3bac7dbd532bb09e055473c6d0220ab24005a85b0bd193653"
SOURCE_LINEAGE = "PROSPECTIVE_SOURCE_AUTHORITY_TO_VISUAL_SUPPLEMENT_TO_INTAKE_LEDGER"
TARGET_KINDS = tuple(kind.value for kind in TargetKind)
COMPATIBILITIES = tuple(value.value for value in CandidateCompatibility)


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _envelope(payload: Mapping[str, Any]) -> dict[str, Any]:
    digest = logical_digest(payload)
    return {"reference": f"{PACKET_PREFIX}:v1:{digest}", "logicalDigest": digest, "payload": dict(payload)}


def build_packet(ledger: Mapping[str, Any], supplement: Mapping[str, Any]) -> dict[str, Any]:
    source = json.loads(SOURCE_AUTHORITY_PATH.read_text(encoding="utf-8"))
    validate_authority(source)
    validate_updated_ledger(ledger)
    if ledger.get("payload", {}).get("evidenceSupplementLogicalDigest") != supplement.get("logicalDigest"):
        raise ValueError("PACKET_SUPPLEMENT_BINDING_INVALID")
    visual = {row["observationKey"]: row for row in supplement["payload"]["records"]}
    units = []
    for row in ledger["payload"]["records"]:
        ev = visual[row["observationKey"]]
        units.append({
            "reviewUnitId": f"prospective-human-review-unit:v1:{row['intakeRecordId'].rsplit(':', 1)[-1]}",
            "observationKey": row["observationKey"],
            "intakeRecordId": row["intakeRecordId"],
            "rawObservation": row["rawObservation"],
            "intakeCreatedAtUtc": row["intakeCreatedAtUtc"],
            "sourceLineage": {"sourceAuthorityReference": SOURCE_REFERENCE, "intakeLedgerReference": ledger["reference"], "lineage": SOURCE_LINEAGE},
            "visualEvidence": {"reference": ev["visualEvidenceReference"] if "visualEvidenceReference" in ev else supplement["reference"], "filename": ev["imageFilename"], "path": ev["durableEvidencePath"], "sha256": ev["sha256"], "description": ev["visualDescription"], "humanSupplied": True},
            "isolation": {"trainCollision": row["trainCollision"], "trainCollisionReference": row["trainCollisionReference"], "validationCollision": row["validationCollision"], "validationCollisionReference": row["validationCollisionReference"]},
            "familyReview": {"resolutionMode": "HUMAN_ADJUDICATION_REQUIRED", "familyState": "PENDING_HUMAN_ADJUDICATION", "familyId": None, "decision": None},
            "relationshipReview": {
                "candidateInputRequired": True, "targetReferenceInputRequired": True,
                "allowedTargetKindValues": list(TARGET_KINDS), "allowedCandidateCompatibilityValues": list(COMPATIBILITIES),
                "allowedNegativeBoundaryValues": list(DEFINED_NEGATIVE_BOUNDARY_TYPES_V1),
                "targetKindDecisionRequired": True, "candidateCompatibilityDecisionRequired": True,
                "targetReferenceDecisionRequired": True, "negativeBoundaryDecisionRequired": True,
                "targetKindDecision": None, "candidateCompatibilityDecision": None,
                "targetReferenceDecision": None, "negativeBoundaryDecision": None,
            },
            "reviewerDecision": None, "reviewRationale": None, "reviewCompleteness": "PENDING_HUMAN_REVIEW",
        })
    payload = {
        "schema": "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_HUMAN_REVIEW_PACKET_V1", "version": 1,
        "state": "SOURCE_FAITHFUL_MODEL_BLIND_HUMAN_REVIEW_REQUIRED", "sourceAuthorityReference": SOURCE_REFERENCE,
        "intakeLedgerReference": ledger["reference"], "intakeLedgerLogicalDigest": ledger["logicalDigest"],
        "visualSupplementReference": supplement["reference"], "visualSupplementLogicalDigest": supplement["logicalDigest"],
        "recordCount": len(units), "familyAssignmentCount": 0, "groundTruthAssignmentCount": 0,
        "modelIndependent": True, "modelOutputVisible": False, "modelConfidenceVisible": False,
        "familyResolutionModeCounts": {"DETERMINISTIC_AUTHORITY": 0, "HUMAN_ADJUDICATION_REQUIRED": len(units)},
        "reviewPolicy": {"humanRequired": True, "beforeModelExposure": True, "codexScientificDecisionCount": 0},
        "units": units,
    }
    return _envelope(payload)


def validate_packet(value: Mapping[str, Any]) -> dict[str, Any]:
    payload = value.get("payload")
    if not isinstance(payload, Mapping) or logical_digest(payload) != value.get("logicalDigest"):
        raise ValueError("HUMAN_REVIEW_PACKET_DIGEST_INVALID")
    if value.get("reference") != f"{PACKET_PREFIX}:v1:{value['logicalDigest']}":
        raise ValueError("HUMAN_REVIEW_PACKET_REFERENCE_INVALID")
    units = payload.get("units")
    if payload.get("recordCount") != 15 or not isinstance(units, list) or len(units) != 15:
        raise ValueError("HUMAN_REVIEW_PACKET_RECORD_COUNT_INVALID")
    if any(u.get("familyReview", {}).get("decision") is not None or u.get("relationshipReview", {}).get("targetKindDecision") is not None for u in units):
        raise ValueError("HUMAN_REVIEW_PACKET_CONTAINS_SCIENTIFIC_DECISION")
    if payload.get("familyAssignmentCount") != 0 or payload.get("groundTruthAssignmentCount") != 0 or payload.get("modelIndependent") is not True:
        raise ValueError("HUMAN_REVIEW_PACKET_ISOLATION_INVALID")
    return dict(value)


def persist_and_reload(
    ledger_path: str | Path = UPDATED_LEDGER_PATH,
    supplement_path: str | Path | None = None,
    packet_path: str | Path = PACKET_PATH,
) -> dict[str, Any]:
    ledger = validate_updated_ledger(json.loads(Path(ledger_path).read_text(encoding="utf-8")))
    supplement_path = Path(supplement_path or Path(ledger_path).parent / "raw-evidence/visual-evidence-supplement.v1.json")
    supplement = json.loads(supplement_path.read_text(encoding="utf-8"))
    packet = build_packet(ledger, supplement)
    destination = Path(packet_path); destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(_canonical(packet) + b"\n")
    return validate_packet(json.loads(destination.read_text(encoding="utf-8")))

