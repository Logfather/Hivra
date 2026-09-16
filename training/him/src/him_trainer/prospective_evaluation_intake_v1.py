"""Frozen, empty prospective intake source for Final Evaluation V2.

The authority is an append boundary, not an evaluation corpus. Productive
records are admitted later through a separate ledger and must be created at
or after the frozen intake timestamp.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any, Mapping


SCHEMA = "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_HUMAN_INTAKE_SOURCE_V1"
DESIGN_AUTHORITY_REFERENCE = "him-fresh-evaluation-corpus-design-authority:v2:e41a000ac0cc8f8c7533f29c86180abfa94167c04d08b895b45c99e555eea5c2"
CREATED_AT_UTC = "2026-09-16T09:02:22Z"
HOLDOUT_EXISTENCE_ANCHOR = "2026-09-15T19:26:26+02:00"
HOLDOUT_REFERENCE = "p2-family-isolated-holdout-authority:v1:dc19557950bb3737ea51fc94d04616f2f2c96233868be294773bd120509b7c5a"


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def logical_digest(value: Any) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def build_authority() -> dict[str, Any]:
    payload = {
        "schema": SCHEMA,
        "version": 1,
        "state": "FROZEN_EMPTY_PROSPECTIVE_SOURCE",
        "sourceSemanticIdentity": "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_HUMAN_INTAKE_V1",
        "sourceClass": "INDEPENDENT_HUMAN_REVIEW_INTAKE",
        "designAuthorityReference": DESIGN_AUTHORITY_REFERENCE,
        "sourceCreatedAtUtc": CREATED_AT_UTC,
        "sourceIntakeNotBeforeUtc": CREATED_AT_UTC,
        "repositoryBinding": {"mode": "COMMIT_AUTHORED_NON_SELF_REFERENTIAL", "reference": "feature/shared-lists@d11e6b2f085667943bcd335e87271b0036ef8f26"},
        "initialState": {"recordCount": 0, "familyCount": 0, "evidenceRecordCount": 0, "groundTruthCount": 0},
        "admissionPolicy": {
            "prospectiveRecordCreationClaim": True,
            "realWorldProductCreationClaim": False,
            "intakeTimestampMustBeAtOrAfterSourceIntakeNotBefore": True,
            "requiredFields": ["intakeSourceReference", "intakeRecordId", "intakeCreatedAtUtc", "rawObservation", "sourceProvenance"],
            "optionalLaterFields": ["externalSourceId", "rawEvidenceReference", "familyLineage", "groundTruthLineage"],
        },
        "recordIdentityPolicy": {
            "scheme": "sha256(canonical intake source reference + intake event identity + raw observation identity)",
            "modelIndependent": True,
            "groundTruthIndependent": True,
            "distinctFrom": ["GTIN", "OFF_CODE", "sourceRecordId", "familyId", "groundTruth"],
        },
        "duplicatePolicy": {
            "sameIntakeRecord": "same immutable intakeRecordId is rejected",
            "sameExternalProduct": "external identifier is provenance and requires explicit observation comparison",
            "differentObservationSameProduct": "distinct intake record allowed only with distinct intake event identity",
            "differentCandidateOrRelation": "does not authorize scientific equivalence; requires independent review",
            "modelDecision": False,
        },
        "evidencePolicy": {"sourceFaithfulEvidenceRequired": True, "rawObservationRequired": True, "sourceContextRequired": True, "reviewSufficiencyRequired": True},
        "isolationPolicy": {
            "trainCollisionScreenRequired": True,
            "trainCollisionAllowed": False,
            "validationCollisionScreenRequired": True,
            "validationCollisionAllowed": False,
            "consumedHoldoutSourceLevelTemporalSeparation": True,
            "consumedHoldoutRecordLevelIsolation": "PENDING_FUTURE_RECORD_ADMISSION",
            "consumedHoldoutAuthorityReference": HOLDOUT_REFERENCE,
            "consumedHoldoutContentAccessRequired": False,
        },
        "modelPolicy": {"accessBeforeGroundTruthFreezeAllowed": False, "inferenceDuringIntakeAllowed": False, "inferenceDuringHumanReviewAllowed": False, "groundTruthModelIndependence": "FROZEN"},
        "humanReviewPolicy": {"required": True, "beforeModelExposure": True, "authorityPattern": "existing independent human adjudication with source binding", "groundTruthLabelsDefinedHere": False},
        "familyPolicy": {"pattern": "existing deterministic canonical family lineage", "assignmentAtSourceFreeze": False},
        "futurePersistence": {
            "intakeLedgerPattern": "append-only immutable intake ledger bound to frozen source authority",
            "intakeLedgerStoragePath": "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/intake-ledger.v1.jsonl",
            "intakeLedgerIdentityScheme": "intakeRecordId + intakeCreatedAtUtc + source authority reference",
            "rawEvidenceStoragePolicy": "source-faithful immutable evidence sidecar bound by SHA-256",
            "rawEvidenceStoragePath": "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/raw-evidence/",
            "rawEvidenceDigestScheme": "sha256(canonical UTF-8 evidence bytes)",
            "humanReviewAuthorityPattern": "existing human adjudication authority with logical digest",
            "humanReviewPacketPattern": "existing source-faithful review packet with source-record binding",
            "humanReviewResultPattern": "existing persisted human decision/result with logical digest",
            "familyLineagePattern": "existing canonical family lineage authority",
            "trainIsolationReference": "him-final-training-partition:v1:b8f48b6b253c633965ed1bb7afebd6325b25d62f4f125b3055150b881b45e27f",
            "validationIsolationReference": "him-final-training-partition:v1:b8f48b6b253c633965ed1bb7afebd6325b25d62f4f125b3055150b881b45e27f",
            "futureCorpusToHoldoutPattern": "existing Final Evaluation V2 corpus freeze -> family-isolated Holdout selection/seal",
        },
        "minimumFutureEvaluation": {"recordCount": 6, "familyCount": 3, "artificialTrimmingAllowed": False, "artificialRecordCreationAllowed": False},
        "freshDeltaBinding": None,
        "scientificIndependence": {"usesModelOutput": False, "usesConsumedHoldoutContent": False, "usesFinalModel": False},
    }
    digest = logical_digest(payload)
    return {"reference": f"him-final-evaluation-v2-prospective-human-intake-source:v1:{digest}", "logicalDigest": digest, "payload": payload}


def validate_authority(value: Mapping[str, Any]) -> dict[str, Any]:
    payload = value.get("payload")
    if not isinstance(payload, Mapping) or logical_digest(payload) != value.get("logicalDigest"):
        raise ValueError("PROSPECTIVE_AUTHORITY_DIGEST_INVALID")
    if value.get("reference") != f"him-final-evaluation-v2-prospective-human-intake-source:v1:{value['logicalDigest']}":
        raise ValueError("PROSPECTIVE_AUTHORITY_REFERENCE_INVALID")
    if payload.get("schema") != SCHEMA or payload.get("state") != "FROZEN_EMPTY_PROSPECTIVE_SOURCE":
        raise ValueError("PROSPECTIVE_AUTHORITY_STATE_INVALID")
    if payload.get("initialState") != {"recordCount": 0, "familyCount": 0, "evidenceRecordCount": 0, "groundTruthCount": 0}:
        raise ValueError("PROSPECTIVE_SOURCE_NOT_EMPTY")
    if payload.get("sourceIntakeNotBeforeUtc") != payload.get("sourceCreatedAtUtc"):
        raise ValueError("PROSPECTIVE_SOURCE_TIME_BOUNDARY_INVALID")
    if payload.get("freshDeltaBinding") is not None:
        raise ValueError("FRESH_DELTA_MUST_NOT_BE_BOUND")
    return dict(value)


def persist_and_reload(path: str | Path) -> dict[str, Any]:
    value = build_authority()
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(_canonical(value) + b"\n")
    return validate_authority(json.loads(destination.read_text(encoding="utf-8")))
