"""Design authority for a future fresh Final Evaluation V2 corpus.

This is policy and construction metadata only. It contains no evaluation
records, labels, Holdout content, model output, or selection result.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any, Mapping


SCHEMA = "HIM_FRESH_EVALUATION_CORPUS_DESIGN_AUTHORITY_V2"
VERSION = 1
EXECUTION_CONTRACT_DIGEST = "57488a3f2f85d3970e6f338d31076497b76434c36ef84087094c6539cf062ded"
REQUIREMENT_SET_DIGEST = "69200af2fb579ace4d0f260f28d3965871f18dae1181ea333d3081ee8a17aff4"
CHECKPOINT_REFERENCE = "him-training-checkpoint:v2:beba3ba4ba75ba1d117f3ed13482e8389b2f428406dd92d4cc41d1b476140197"
CHECKPOINT_DIGEST = "923d0a479847b21e57ff4b1c5649c1a38f07bb3a431b8e06ea9bec50bcd54632"
MODEL_STATE_SHA256 = "6fd984f5375ef780ab81187e053bf468158ddb849932553ea4062a745796443e"


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def logical_digest(value: Any) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def build_design_authority() -> dict[str, Any]:
    payload = {
        "schema": SCHEMA,
        "version": VERSION,
        "state": "DESIGN_AUTHORITY_CLOSED_BEFORE_CORPUS_GENERATION",
        "executionContractLogicalDigest": EXECUTION_CONTRACT_DIGEST,
        "requirementSetLogicalDigest": REQUIREMENT_SET_DIGEST,
        "admissibleSourceTypes": [
            "INDEPENDENT_RAW_SOURCE_SNAPSHOT_WITH_STABLE_LINEAGE",
            "INDEPENDENT_HUMAN_REVIEW_QUEUE_WITH_SOURCE_BINDING",
            "POST_TRAINING_SOURCE_ADDITION_WITH_PROVENANCE",
            "UNSPLIT_EVIDENCE_BACKED_CANDIDATE_POPULATION",
            "INDEPENDENT_AUTHORITY_SUPERVISION_EXPORT",
        ],
        "admissibilityRules": [
            "source snapshot predates selection and is independently versioned",
            "stable source and source-record identity is present",
            "exactly one deterministic family identity is derivable",
            "record/family/lineage is absent from TRAIN, VALIDATION, and consumed Holdout",
            "ground truth is independently reviewable and model-independent",
        ],
        "forbiddenSourceTypes": [
            "TRAINING_DERIVED",
            "VALIDATION_DERIVED",
            "CONSUMED_HOLDOUT_DERIVED",
            "MODEL_OUTPUT_SYNTHETIC",
            "MODEL_FAILURE_SELECTED",
            "CONSUMED_HOLDOUT_REWRITTEN",
            "UNRESOLVED_SOURCE_IDENTITY",
            "UNRESOLVED_FAMILY_IDENTITY",
            "MODEL_DEPENDENT_GROUND_TRUTH",
        ],
        "identityAuthority": {
            "recordId": "sha256(canonical source-record identity + family + lineage)",
            "sourceId": "immutable source snapshot reference",
            "sourceRecordId": "source-native stable record identity",
            "familyId": "deterministic canonical-family identity from independent authority",
            "lineageId": "sha256(source snapshot + source-record + evidence provenance)",
        },
        "familyAuthority": {"modelIndependent": True, "deterministic": True, "reproducible": True, "rule": "exactly one authoritative family binding or fail closed"},
        "lineageAuthority": {"required": True, "complete": True, "opaqueHandAuthoredRecordsAllowed": False, "multiSource": "explicit ordered provenance tuple"},
        "groundTruthAuthority": {"modelIndependent": True, "reviewable": True, "provenanceRequired": True, "allowed": ["independent human adjudication", "pre-existing authoritative supervision", "frozen authoritative rule"], "forbidden": ["model prediction", "model confidence", "consumed Holdout result"]},
        "humanReviewPolicy": {"defined": True, "unitIdentityRequired": True, "sourceFaithfulEvidenceRequired": True, "decisionsPersistedWithDigest": True, "unresolvedState": "UNRESOLVED_NOT_ELIGIBLE"},
        "outputAuthority": {"primary": "TARGET_KIND", "secondary": "CANDIDATE_COMPATIBILITY", "semanticChangeCount": 0, "newOutputHeadCount": 0, "newTargetLabelCount": 0, "inputRepresentation": "V2"},
        "exclusionFramework": {
            "training": ["exact_input", "source_record", "family", "lineage", "candidate_evidence_lineage"],
            "validation": ["exact_input", "source_record", "family", "lineage", "candidate_evidence_lineage"],
            "consumedHoldout": ["record_id", "source_record", "family", "lineage"],
            "unresolved": ["unresolved_adjudication", "invalid_evidence", "schema_invalid", "ambiguous_family"],
            "proofRequiresConsumedHoldoutContent": False,
        },
        "splitLineage": {"state": "FRESH_EVALUATION_CORPUS_V2", "distinctFrom": ["TRAIN", "VALIDATION", "CONSUMED_HOLDOUT"]},
        "constructionContract": {"finite": True, "modelIndependent": True, "reproducible": True, "enumeration": "sorted stable source-record identity", "deduplication": "exact source-record and lineage identity; collisions fail closed", "ordering": "canonical UTF-8 identity ascending", "readiness": "DISCOVERED -> IDENTIFIED -> LINEAGE_COMPLETE -> ELIGIBLE -> REVIEW_REQUIRED -> GROUND_TRUTH_RESOLVED -> READY_FOR_EVALUATION_UNIVERSE"},
        "futureCorpusSchema": {"required": ["authority", "sourceSnapshot", "recordId", "sourceRecordId", "familyId", "lineageId", "evidence", "groundTruth", "splitLineage", "inputRepresentationV2"], "recordIdentityCoverage": "100_PERCENT"},
        "selection": {"finalHoldoutSelectionInThisAuthority": False, "recordCount": 0, "familyCount": 0},
        "checkpointBinding": {"reference": CHECKPOINT_REFERENCE, "logicalDigest": CHECKPOINT_DIGEST, "modelStateSha256": MODEL_STATE_SHA256},
        "scientificIndependence": {"usesModelOutput": False, "usesModelConfidence": False, "usesModelErrors": False, "usesConsumedHoldoutContent": False},
    }
    digest = logical_digest(payload)
    return {"reference": f"him-fresh-evaluation-corpus-design-authority:v2:{digest}", "logicalDigest": digest, "payload": payload}


def validate_design_authority(value: Mapping[str, Any]) -> dict[str, Any]:
    payload = value.get("payload")
    if not isinstance(payload, Mapping) or logical_digest(payload) != value.get("logicalDigest"):
        raise ValueError("DESIGN_AUTHORITY_DIGEST_INVALID")
    if value.get("reference") != f"him-fresh-evaluation-corpus-design-authority:v2:{value['logicalDigest']}":
        raise ValueError("DESIGN_AUTHORITY_REFERENCE_INVALID")
    if payload.get("schema") != SCHEMA or payload.get("state") != "DESIGN_AUTHORITY_CLOSED_BEFORE_CORPUS_GENERATION":
        raise ValueError("DESIGN_AUTHORITY_STATE_INVALID")
    if payload.get("selection", {}).get("recordCount") != 0 or payload.get("selection", {}).get("familyCount") != 0:
        raise ValueError("SELECTION_MUST_REMAIN_EMPTY")
    if payload.get("scientificIndependence", {}).get("usesConsumedHoldoutContent") is not False:
        raise ValueError("CONSUMED_HOLDOUT_CONTENT_FORBIDDEN")
    return dict(value)


def persist_and_reload(path: str | Path) -> dict[str, Any]:
    value = build_design_authority()
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(_canonical(value) + b"\n")
    reloaded = json.loads(destination.read_text(encoding="utf-8"))
    return validate_design_authority(reloaded)

