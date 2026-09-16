"""Model-independent persistence of explicitly confirmed prospective V2 review."""

from __future__ import annotations

import copy
import json
from pathlib import Path
from typing import Any, Mapping

from him_trainer.prospective_human_review_packet_v1 import validate_packet
from him_trainer.prospective_record_admission_v1 import logical_digest
from him_trainer.prospective_visual_evidence_supplement_v1 import validate_updated_ledger


ROOT = Path(__file__).resolve().parents[4]
BASE = ROOT / "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1"
PACKET_PATH = BASE / "human-review-packet.v1.json"
LEDGER_PATH = BASE / "intake-ledger.supplemented.v1.json"
SUPPLEMENT_PATH = BASE / "raw-evidence/visual-evidence-supplement.v1.json"
FAMILY_AUTHORITY_PATH = ROOT / "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json"
RESULT_PATH = BASE / "human-review-result.v1.json"
LINEAGE_PATH = BASE / "family-lineage.v1.json"
GROUND_TRUTH_PATH = BASE / "prospective-ground-truth.v1.json"
CORPUS_PATH = BASE / "fresh-evaluation-corpus.v2.json"
PACKET_REFERENCE = "him-final-evaluation-v2-prospective-human-review-packet:v1:91c2616cdee380064d5ebc4f577047050133db9ba69e7dfa44dac94e929af472"
REVIEWER = "human-reviewer:christian-glatschke:v1"
FAMILY_AUTHORITY_REFERENCE = "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json@sha256:86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184"


DECISIONS = {
    "OBS-001": ("family:v1:canonical:i1ntgb", "canonical-family:v1:canonical:i1ntgb", "EXISTING_CANONICAL", "COMPATIBLE", "NOT_APPLICABLE", "Die Beobachtung Knäckebrot entspricht exakt dem bestehenden Canonical Knäckebrot; eine Spezialisierung ist nicht autorisiert."),
    "OBS-002": ("family:v1:canonical:k2RoSD", "canonical-family:v1:canonical:k2RoSD", "VARIANT", "COMPATIBLE", "NOT_APPLICABLE", "Muskatnuss gemahlen gehört zur bestehenden Muskatnuss-Familie; gemahlen bezeichnet eine Verarbeitungs- bzw. Produktform."),
    "OBS-003": ("family:v1:canonical:vs2zry", "canonical-family:v1:canonical:vs2zry", "IDENTITY", "COMPATIBLE", "NOT_APPLICABLE", "Frische Champignons bezeichnet dieselbe Lebensmittelidentität wie Champignons; frisch begründet keine eigenständige Variante."),
    "OBS-004": ("family:v1:canonical:eejBZ7", "canonical-family:v1:canonical:eejBZ7", "VARIANT", "COMPATIBLE", "NOT_APPLICABLE", "Pink Lady spezifiziert eine konkrete Apfelsorte innerhalb der Äpfel-Familie."),
    "OBS-005": ("family:v1:canonical:J9V3UB", "canonical-family:v1:canonical:J9V3UB", "EXISTING_CANONICAL", "COMPATIBLE", "NOT_APPLICABLE", "Salami entspricht exakt dem bestehenden Canonical Salami."),
    "OBS-006": ("family:v1:canonical:VsPlZM", "canonical-family:v1:canonical:VsPlZM", "EXISTING_CANONICAL", "COMPATIBLE", "NOT_APPLICABLE", "Frischkäse entspricht exakt dem bestehenden Canonical Frischkäse."),
    "OBS-007": (None, None, "NEW_CANONICAL", "NOT_APPLICABLE", "NOT_APPLICABLE", "Kein bestehender Canonical oder bestehende Family wurde gefunden; die Beobachtung bleibt als NEW_CANONICAL autorisiert."),
    "OBS-008": ("family:v1:canonical:hVJHg0", "canonical-family:v1:canonical:hVJHg0", "EXISTING_CANONICAL", "COMPATIBLE", "NOT_APPLICABLE", "Griechischer Joghurt entspricht dem präziseren bestehenden Canonical Griechischer Joghurt."),
    "OBS-009": ("family:v1:canonical:2fRaJS", "canonical-family:v1:canonical:2fRaJS", "EXISTING_CANONICAL", "COMPATIBLE", "NOT_APPLICABLE", "Weizenbier entspricht exakt dem bestehenden Canonical Weizenbier."),
    "OBS-010": (None, None, "NEW_CANONICAL", "NOT_APPLICABLE", "NOT_APPLICABLE", "Kein bestehender Canonical oder bestehende Family wurde gefunden; die Beobachtung bleibt als NEW_CANONICAL autorisiert."),
    "OBS-011": ("family:v1:canonical:9axGGb", "canonical-family:v1:canonical:9axGGb", "IDENTITY", "COMPATIBLE", "NOT_APPLICABLE", "Shrimps bezeichnet dieselbe Lebensmittelidentität wie Garnelen; gefroren ist eine Zustandsbeschreibung."),
    "OBS-012": ("family:v1:canonical:xgFB2C", "canonical-family:v1:canonical:xgFB2C", "EXISTING_CANONICAL", "COMPATIBLE", "NOT_APPLICABLE", "Hähnchenschenkel entspricht exakt dem bestehenden Canonical Hähnchenschenkel."),
    "OBS-013": ("family:v1:canonical:rTjhFq", "canonical-family:v1:canonical:rTjhFq", "VARIANT", "COMPATIBLE", "NOT_APPLICABLE", "Kalamata bezeichnet eine spezifische Olivensorte innerhalb der Oliven-Familie."),
    "OBS-014": ("family:v1:canonical:3owfJH", "canonical-family:v1:canonical:3owfJH", "EXISTING_CANONICAL", "COMPATIBLE", "NOT_APPLICABLE", "Ajvar entspricht exakt dem bestehenden Canonical Ajvar."),
    "OBS-015": ("family:v1:canonical:Klq8bN", "canonical-family:v1:canonical:Klq8bN", "EXISTING_CANONICAL", "COMPATIBLE", "NOT_APPLICABLE", "Tortellini entspricht exakt dem bestehenden Canonical Tortellini."),
}


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()


def _envelope(schema: str, payload: Mapping[str, Any], prefix: str) -> dict[str, Any]:
    digest = logical_digest(payload)
    return {"reference": f"{prefix}:v1:{digest}", "logicalDigest": digest, "payload": dict(payload)}


def _inputs() -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    packet = validate_packet(json.loads(PACKET_PATH.read_text()))
    ledger = validate_updated_ledger(json.loads(LEDGER_PATH.read_text()))
    supplement = json.loads(SUPPLEMENT_PATH.read_text())
    if packet["reference"] != PACKET_REFERENCE or packet["payload"]["intakeLedgerReference"] != ledger["reference"]:
        raise ValueError("HUMAN_INPUT_BINDING_INVALID")
    return packet, ledger, supplement


def build_result() -> dict[str, Any]:
    packet, ledger, supplement = _inputs()
    by_key = {u["observationKey"]: u for u in packet["payload"]["units"]}
    records = []
    for row in ledger["payload"]["records"]:
        key = row["observationKey"]; unit = by_key[key]; family, target, kind, compatibility, boundary, rationale = DECISIONS[key]
        records.append({"observationKey": key, "reviewUnitId": unit["reviewUnitId"], "intakeRecordId": row["intakeRecordId"], "family": family, "targetReference": target, "targetKind": kind, "candidateCompatibility": compatibility, "negativeBoundary": boundary, "rationale": rationale, "sourceLineage": unit["sourceLineage"], "visualEvidence": unit["visualEvidence"]})
    payload = {"schema": "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_HUMAN_REVIEW_RESULT_V1", "version": 1, "state": "FINAL_HUMAN_ADJUDICATION", "reviewer": REVIEWER, "reviewPacketReference": packet["reference"], "reviewPacketLogicalDigest": packet["logicalDigest"], "visualSupplementReference": supplement["reference"], "recordCount": 15, "humanConfirmedRecordCount": 15, "modelIndependent": True, "records": records}
    return _envelope("HIM_FINAL_EVALUATION_V2_PROSPECTIVE_HUMAN_REVIEW_RESULT_V1", payload, "him-final-evaluation-v2-prospective-human-review-result")


def validate_result(value: Mapping[str, Any]) -> dict[str, Any]:
    payload = value.get("payload")
    if not isinstance(payload, Mapping) or logical_digest(payload) != value.get("logicalDigest"):
        raise ValueError("HUMAN_RESULT_DIGEST_INVALID")
    if payload.get("recordCount") != 15 or payload.get("humanConfirmedRecordCount") != 15 or payload.get("modelIndependent") is not True:
        raise ValueError("HUMAN_RESULT_STATE_INVALID")
    if payload.get("reviewPacketReference") != PACKET_REFERENCE:
        raise ValueError("HUMAN_RESULT_PACKET_BINDING_INVALID")
    if len(payload.get("records", [])) != 15 or len({r["reviewUnitId"] for r in payload["records"]}) != 15:
        raise ValueError("HUMAN_RESULT_UNITS_INVALID")
    return dict(value)


def build_lineage(result: Mapping[str, Any]) -> dict[str, Any]:
    rows = []
    for r in result["payload"]["records"]:
        rows.append({"intakeRecordId": r["intakeRecordId"], "observationKey": r["observationKey"], "familyId": r["family"], "resolutionMode": "HUMAN_ADJUDICATION" if r["family"] else "NEW_CANONICAL_NO_EXISTING_FAMILY", "authorityOrReviewReference": result["reference"], "targetReference": r["targetReference"]})
    payload = {"schema": "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_FAMILY_LINEAGE_V1", "version": 1, "state": "FROZEN_FROM_HUMAN_REVIEW", "familyAuthorityReference": FAMILY_AUTHORITY_REFERENCE, "humanReviewResultReference": result["reference"], "recordCount": 15, "existingFamilyBoundRecordCount": 13, "newCanonicalNoExistingFamilyRecordCount": 2, "familyAssignmentCount": 15, "records": rows}
    return _envelope("HIM_FINAL_EVALUATION_V2_PROSPECTIVE_FAMILY_LINEAGE_V1", payload, "him-final-evaluation-v2-prospective-family-lineage")


def build_ground_truth(result: Mapping[str, Any], lineage: Mapping[str, Any]) -> dict[str, Any]:
    payload = {"schema": "HIM_FINAL_EVALUATION_V2_PROSPECTIVE_GROUND_TRUTH_V1", "version": 1, "state": "FROZEN_IMMUTABLE", "modelIndependent": True, "humanReviewResultReference": result["reference"], "familyLineageReference": lineage["reference"], "recordCount": 15, "resolvedRecordCount": 15, "unresolvedRecordCount": 0, "records": copy.deepcopy(result["payload"]["records"])}
    return _envelope("HIM_FINAL_EVALUATION_V2_PROSPECTIVE_GROUND_TRUTH_V1", payload, "him-final-evaluation-v2-prospective-ground-truth")


def build_corpus(result: Mapping[str, Any], lineage: Mapping[str, Any], truth: Mapping[str, Any]) -> dict[str, Any]:
    records = []
    for r in result["payload"]["records"]:
        records.append({"recordId": r["intakeRecordId"], "sourceRecordId": r["intakeRecordId"], "familyId": r["family"], "lineageId": logical_digest({"source": r["sourceLineage"], "evidence": r["visualEvidence"]}), "evidence": r["visualEvidence"], "groundTruth": {"targetReference": r["targetReference"], "targetKind": r["targetKind"], "candidateCompatibility": r["candidateCompatibility"], "negativeBoundary": r["negativeBoundary"]}, "humanReviewResultReference": result["reference"], "familyLineageReference": lineage["reference"], "groundTruthReference": truth["reference"], "splitLineage": "FRESH_EVALUATION_CORPUS_V2", "inputRepresentationV2": {"observedTerm": r["observationKey"], "sourceFaithfulEvidence": r["visualEvidence"]["description"]}})
    payload = {"schema": "HIM_FRESH_EVALUATION_CORPUS_V2", "version": 2, "state": "FROZEN_EVALUATION_UNIVERSE", "modelIndependent": True, "artificiallyTrimmed": False, "humanReviewResultReference": result["reference"], "familyLineageReference": lineage["reference"], "groundTruthReference": truth["reference"], "recordCount": 15, "distinctFamilyCount": 13, "records": records, "finalHoldoutSelectionCount": 0}
    return _envelope("HIM_FRESH_EVALUATION_CORPUS_V2", payload, "him-fresh-evaluation-corpus-v2")


def persist_all() -> dict[str, Any]:
    result = validate_result(build_result()); lineage = build_lineage(result); truth = build_ground_truth(result, lineage); corpus = build_corpus(result, lineage, truth)
    for path, value in ((RESULT_PATH, result), (LINEAGE_PATH, lineage), (GROUND_TRUTH_PATH, truth), (CORPUS_PATH, corpus)):
        path.write_bytes(_canonical(value) + b"\n")
    return {"result": validate_result(json.loads(RESULT_PATH.read_text())), "lineage": json.loads(LINEAGE_PATH.read_text()), "groundTruth": json.loads(GROUND_TRUTH_PATH.read_text()), "corpus": json.loads(CORPUS_PATH.read_text())}
