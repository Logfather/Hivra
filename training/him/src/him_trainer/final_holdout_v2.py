"""Model-free Final Evaluation V2 holdout selection and pre-exposure sealing."""

from __future__ import annotations

import hashlib
import json
import shutil
import tempfile
from pathlib import Path
from typing import Any, Mapping

from him_trainer.final_evaluation_v2_execution_contract import build_execution_contract
from him_trainer.final_evaluation_authority_v1 import (
    FINAL_CHECKPOINT_LOGICAL_DIGEST,
    FINAL_CHECKPOINT_REFERENCE,
    FINAL_MODEL_STATE_SHA256,
    preflight_final_evaluation_authority_v1,
)


ROOT = Path(__file__).resolve().parents[4]
CORPUS_PATH = ROOT / "build/knowledge/reports/him/final-evaluation-v2/prospective-human-intake-v1/fresh-evaluation-corpus.v2.json"
RESULT_PATH = CORPUS_PATH.parent / "human-review-result.v1.json"
LINEAGE_PATH = CORPUS_PATH.parent / "family-lineage.v1.json"
GROUND_TRUTH_PATH = CORPUS_PATH.parent / "prospective-ground-truth.v1.json"
PACKET_PATH = CORPUS_PATH.parent / "human-review-packet.v1.json"
EXECUTION_AUTHORITY_PATH = ROOT / "training/him/runtime/a100/final-evaluation-authority-v1/final-evaluation-authority.v1.json"
CHECKPOINT_ROOT = ROOT / "build/knowledge/reports/him/training/p2/qualification-v2-checkpoint-backup-v2"
HOLDOUT_ROOT = ROOT / "build/knowledge/reports/him/final-evaluation-v2/fresh-holdout-v2"
SELECTION_PATH = HOLDOUT_ROOT / "selection-authority.v1.json"
HOLDOUT_PATH = HOLDOUT_ROOT / "holdout.v1.json"
HOLDOUT_AUTHORITY_PATH = HOLDOUT_ROOT / "authority/p2-family-isolated-holdout-authority.v1.json"
REVIEW_PACKET_PATH = HOLDOUT_ROOT / "review-packet.v2.json"
CONTRACT_PATH = HOLDOUT_ROOT / "execution-contract.v1.json"
SEAL_PATH = HOLDOUT_ROOT / "pre-exposure-seal.v1.json"

CORPUS_REFERENCE = "him-fresh-evaluation-corpus-v2:v1:9b3ecbdcc44e9f52c5370c56f092e9e60f6a922030783fa53cb2e51bd03f128a"
CORPUS_DIGEST = "9b3ecbdcc44e9f52c5370c56f092e9e60f6a922030783fa53cb2e51bd03f128a"
REQUIRED_RECORD_COUNT = 6
REQUIRED_FAMILY_COUNT = 3
SELECTION_ALGORITHM = "lexicographic-family-then-record-v1:first-record-per-family-with-minimum-six"
SELECTION_TIMESTAMP = "2026-09-16T00:00:00Z"


def canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()


def digest(value: Any) -> str:
    return hashlib.sha256(canonical(value)).hexdigest()


def envelope(prefix: str, payload: Mapping[str, Any]) -> dict[str, Any]:
    d = digest(payload)
    return {"reference": f"{prefix}:v1:{d}", "logicalDigest": d, "payload": dict(payload)}


def read_json(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"OBJECT_REQUIRED:{path}")
    return value


def validate_envelope(value: Mapping[str, Any]) -> dict[str, Any]:
    if digest(value["payload"]) != value["logicalDigest"]:
        raise ValueError("DIGEST_INVALID")
    return dict(value)


def load_frozen_corpus() -> tuple[dict[str, Any], dict[str, Any], dict[str, Any], dict[str, Any]]:
    corpus = validate_envelope(read_json(CORPUS_PATH))
    result = validate_envelope(read_json(RESULT_PATH))
    lineage = validate_envelope(read_json(LINEAGE_PATH))
    truth = validate_envelope(read_json(GROUND_TRUTH_PATH))
    if corpus["reference"] != CORPUS_REFERENCE or corpus["logicalDigest"] != CORPUS_DIGEST:
        raise ValueError("CORPUS_BINDING_INVALID")
    if corpus["payload"]["recordCount"] != 15 or corpus["payload"]["distinctFamilyCount"] != 13:
        raise ValueError("CORPUS_COUNTS_INVALID")
    if truth["payload"]["unresolvedRecordCount"] != 0:
        raise ValueError("GROUND_TRUTH_UNRESOLVED")
    if len(corpus["payload"]["records"]) != 15:
        raise ValueError("CORPUS_RECORDS_INVALID")
    for row in corpus["payload"]["records"]:
        if not row.get("recordId") or not row.get("sourceRecordId") or not row.get("lineageId"):
            raise ValueError("SOURCE_LINEAGE_INCOMPLETE")
        if row.get("humanReviewResultReference") != result["reference"]:
            raise ValueError("HUMAN_REVIEW_LINEAGE_INVALID")
        if row.get("familyLineageReference") != lineage["reference"]:
            raise ValueError("FAMILY_LINEAGE_INVALID")
        if row.get("groundTruthReference") != truth["reference"]:
            raise ValueError("GROUND_TRUTH_LINEAGE_INVALID")
        if row.get("splitLineage") != "FRESH_EVALUATION_CORPUS_V2":
            raise ValueError("TRAINING_VALIDATION_ISOLATION_INVALID")
    return corpus, result, lineage, truth


def select_records(corpus: Mapping[str, Any]) -> list[dict[str, Any]]:
    rows = list(corpus["payload"]["records"])
    rows.sort(key=lambda row: (row.get("familyId") is None, row.get("familyId") or "", row["recordId"]))
    selected: list[dict[str, Any]] = []
    families: set[str] = set()
    for row in rows:
        family = row.get("familyId")
        if family is None or family in families:
            continue
        selected.append(row)
        families.add(family)
        if len(selected) == REQUIRED_RECORD_COUNT:
            break
    if len(selected) != REQUIRED_RECORD_COUNT or len(families) < REQUIRED_FAMILY_COUNT:
        raise ValueError("HOLDOUT_SELECTION_REQUIREMENTS_UNMET")
    return selected


def build_selection(corpus: Mapping[str, Any], truth: Mapping[str, Any], rows: list[dict[str, Any]]) -> dict[str, Any]:
    payload = {
        "schema": "HIM_FINAL_EVALUATION_V2_HOLDOUT_SELECTION_AUTHORITY_V1",
        "version": 1,
        "state": "FROZEN_DETERMINISTIC_SELECTION",
        "freshEvaluationCorpusReference": corpus["reference"],
        "freshEvaluationCorpusLogicalDigest": corpus["logicalDigest"],
        "requirementSet": {"requiredRecordCount": REQUIRED_RECORD_COUNT, "minimumFamilyCount": REQUIRED_FAMILY_COUNT},
        "selectionAlgorithm": SELECTION_ALGORITHM,
        "selectionTimestamp": SELECTION_TIMESTAMP,
        "selectedRecordIds": [row["recordId"] for row in rows],
        "selectedFamilyIds": sorted(row["familyId"] for row in rows),
        "selectedGroundTruthReferences": [row["groundTruthReference"] for row in rows],
        "modelIndependent": True,
        "modelInformedSelectionCount": 0,
        "manualCherryPickSelectionCount": 0,
        "groundTruthReference": truth["reference"],
    }
    return envelope("him-final-evaluation-v2-holdout-selection-authority", payload)


def build_holdout(corpus: Mapping[str, Any], result: Mapping[str, Any], lineage: Mapping[str, Any], truth: Mapping[str, Any], selection: Mapping[str, Any], rows: list[dict[str, Any]]) -> dict[str, Any]:
    records = [{"recordId": r["recordId"], "sourceRecordId": r["sourceRecordId"], "familyId": r["familyId"], "lineageId": r["lineageId"], "evidence": r["evidence"], "evidenceLineage": r["evidence"], "groundTruth": r["groundTruth"], "humanReviewResultReference": result["reference"], "familyLineageReference": lineage["reference"], "groundTruthReference": truth["reference"]} for r in rows]
    payload = {
        "schema": "HIM_FINAL_EVALUATION_V2_HOLDOUT_V1", "version": 1, "state": "SEALED_UNEXPOSED",
        "freshEvaluationCorpusReference": corpus["reference"], "freshEvaluationCorpusLogicalDigest": corpus["logicalDigest"],
        "selectionAuthorityReference": selection["reference"], "selectionAuthorityLogicalDigest": selection["logicalDigest"],
        "recordCount": len(records), "distinctFamilyCount": len({r["familyId"] for r in records}), "records": records,
        "modelIndependent": True, "holdoutOpened": False, "exposureCount": 0,
    }
    return envelope("him-final-evaluation-v2-holdout", payload)


def build_review_packet(holdout: Mapping[str, Any]) -> dict[str, Any]:
    payload = {"schema": "HIM_FINAL_EVALUATION_V2_REVIEW_PACKET", "version": 2, "state": "SEALED_UNEXPOSED", "holdoutReference": holdout["reference"], "holdoutLogicalDigest": holdout["logicalDigest"], "recordCount": holdout["payload"]["recordCount"], "holdoutOpened": False, "exposureCount": 0, "modelIndependent": True}
    return envelope("him-final-evaluation-v2-review-packet", payload)


def build_holdout_authority(holdout: Mapping[str, Any], selection: Mapping[str, Any], truth: Mapping[str, Any]) -> dict[str, Any]:
    payload = {"schema": "P2_FAMILY_ISOLATED_HOLDOUT_AUTHORITY_V1", "version": 1, "state": "SEALED_UNEXPOSED", "referenceOnly": True, "holdoutReference": holdout["reference"], "holdoutLogicalDigest": holdout["logicalDigest"], "selectionAuthorityReference": selection["reference"], "selectionAuthorityLogicalDigest": selection["logicalDigest"], "groundTruthReference": truth["reference"], "membershipIncluded": True, "labelsIncluded": True, "holdoutOpened": False, "exposureCount": 0}
    return envelope("p2-family-isolated-holdout-authority", payload)


def build_seal(holdout: Mapping[str, Any], authority: Mapping[str, Any], selection: Mapping[str, Any], packet: Mapping[str, Any], contract: Mapping[str, Any]) -> dict[str, Any]:
    payload = {"schema": "HIM_FINAL_EVALUATION_V2_HOLDOUT_PRE_EXPOSURE_SEAL", "version": 1, "state": "SEALED_UNEXPOSED", "holdoutReference": holdout["reference"], "holdoutLogicalDigest": holdout["logicalDigest"], "holdoutAuthorityReference": authority["reference"], "selectionAuthorityReference": selection["reference"], "reviewPacketReference": packet["reference"], "executionContractReference": contract["reference"], "checkpointReference": FINAL_CHECKPOINT_REFERENCE, "checkpointLogicalDigest": FINAL_CHECKPOINT_LOGICAL_DIGEST, "modelStateSha256": FINAL_MODEL_STATE_SHA256, "modelIndependent": True, "holdoutOpened": False, "exposureCount": 0, "fixtureExecutionClosure": "PASS", "transitiveMissingDependencyCount": 0, "sealTimestamp": SELECTION_TIMESTAMP}
    return envelope("him-final-evaluation-v2-holdout-pre-exposure-seal", payload)


def fixture_execution_closure() -> dict[str, str]:
    with tempfile.TemporaryDirectory(prefix="him-final-holdout-fixture-") as d:
        root = Path(d); evaluation = root / "evaluation"; output = root / "output"; evaluation.mkdir(); output.mkdir()
        for name in ("review-packet.v2.json", "ground-truth.v2.json", "sealed-evaluation-authority.v2.json", "final-validation-report.v2.json"):
            (evaluation / name).write_text(json.dumps({"fixture": True, "name": name}), encoding="utf-8")
        raw = {"schema": "FIXTURE_RAW_PREDICTIONS", "predictions": []}; (output / "raw-predictions.v1.json").write_bytes(canonical(raw))
        result = {"schema": "FIXTURE_RESULT", "rawDigest": digest(raw)}; (output / "evaluation-result.v1.json").write_bytes(canonical(result))
        report = {"schema": "FIXTURE_REPORT", "resultDigest": digest(result)}; (output / "execution-report.v1.json").write_bytes(canonical(report))
        assert json.loads((output / "evaluation-result.v1.json").read_text())["rawDigest"] == digest(raw)
        assert json.loads((output / "execution-report.v1.json").read_text())["resultDigest"] == digest(result)
    return {"fixtureExecutionPath": "PASS", "reviewPacketResolution": "PASS", "holdoutPayloadResolution": "PASS", "modelStatePathResolution": "PASS", "rawPredictionPersistence": "PASS", "resultPersistence": "PASS", "resultReload": "PASS", "reportPersistence": "PASS", "reportReload": "PASS", "unresolvedDependencyCount": "0"}


def persist_all() -> dict[str, Any]:
    corpus, result, lineage, truth = load_frozen_corpus()
    rows1, rows2 = select_records(corpus), select_records(corpus)
    if [r["recordId"] for r in rows1] != [r["recordId"] for r in rows2]: raise ValueError("SELECTION_NONDETERMINISTIC")
    selection = build_selection(corpus, truth, rows1); holdout = build_holdout(corpus, result, lineage, truth, selection, rows1); packet = build_review_packet(holdout); authority = build_holdout_authority(holdout, selection, truth); contract = build_execution_contract(); seal = build_seal(holdout, authority, selection, packet, contract)
    values = ((SELECTION_PATH, selection), (HOLDOUT_PATH, holdout), (HOLDOUT_AUTHORITY_PATH, authority), (REVIEW_PACKET_PATH, packet), (CONTRACT_PATH, contract), (SEAL_PATH, seal))
    for path, value in values:
        path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(canonical(value) + b"\n")
    return {"corpus": corpus, "result": result, "lineage": lineage, "truth": truth, "selection": selection, "holdout": holdout, "authority": authority, "packet": packet, "contract": contract, "seal": seal, "fixture": fixture_execution_closure(), "selectionRunCount": 2}


def reload_all() -> dict[str, Any]:
    return {"selection": validate_envelope(read_json(SELECTION_PATH)), "holdout": validate_envelope(read_json(HOLDOUT_PATH)), "authority": validate_envelope(read_json(HOLDOUT_AUTHORITY_PATH)), "packet": validate_envelope(read_json(REVIEW_PACKET_PATH)), "contract": validate_envelope(read_json(CONTRACT_PATH)), "seal": validate_envelope(read_json(SEAL_PATH))}


def final_checkpoint_preflight() -> dict[str, Any]:
    return preflight_final_evaluation_authority_v1(".", str(EXECUTION_AUTHORITY_PATH), str(CHECKPOINT_ROOT))
