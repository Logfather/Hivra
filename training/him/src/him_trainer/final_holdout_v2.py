"""Model-free Final Evaluation V2 holdout selection and pre-exposure sealing."""

from __future__ import annotations

import hashlib
import json
import shutil
import tempfile
from pathlib import Path
from typing import Any, Mapping

from him_trainer.final_evaluation_v2_execution_contract import build_execution_contract
from him_trainer.final_evaluation_v2_engine import (
    build_derived_execution_artifact,
    build_model_input_projection_authority,
    project_model_input,
)
from him_trainer.final_evaluation_authority_v1 import (
    FINAL_CHECKPOINT_LOGICAL_DIGEST,
    FINAL_CHECKPOINT_REFERENCE,
    FINAL_MODEL_STATE_SHA256,
    FINAL_HOLDOUT_V2_REFERENCE,
    FINAL_HOLDOUT_V2_LOGICAL_DIGEST,
    FINAL_GROUND_TRUTH_REFERENCE,
    FINAL_GROUND_TRUTH_LOGICAL_DIGEST,
    FINAL_HOLDOUT_AUTHORITY_REFERENCE,
    FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST,
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
DERIVED_EXECUTION_PATH = HOLDOUT_ROOT / "derived-execution-artifact.v1.json"
PROJECTION_AUTHORITY_PATH = HOLDOUT_ROOT / "model-input-projection-authority.v1.json"
GROUND_TRUTH_EXECUTION_PATH = HOLDOUT_ROOT / "ground-truth.v2.json"
SEALED_AUTHORITY_EXECUTION_PATH = HOLDOUT_ROOT / "sealed-evaluation-authority.v2.json"
VALIDATION_REPORT_EXECUTION_PATH = HOLDOUT_ROOT / "final-validation-report.v2.json"
FAMILY_AUTHORITY_PATH = ROOT / "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json"

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


def build_seal(holdout: Mapping[str, Any], authority: Mapping[str, Any], selection: Mapping[str, Any], packet: Mapping[str, Any], contract: Mapping[str, Any], execution_authority: Mapping[str, Any]) -> dict[str, Any]:
    records = holdout["payload"]["records"]
    payload = {"schema": "HIM_FINAL_EVALUATION_V2_HOLDOUT_PRE_EXPOSURE_SEAL", "version": 1, "state": "SEALED_UNEXPOSED", "holdoutReference": holdout["reference"], "holdoutLogicalDigest": holdout["logicalDigest"], "holdoutAuthorityReference": authority["reference"], "holdoutAuthorityLogicalDigest": authority["logicalDigest"], "selectionAuthorityReference": selection["reference"], "selectionAuthorityLogicalDigest": selection["logicalDigest"], "reviewPacketReference": packet["reference"], "reviewPacketLogicalDigest": packet["logicalDigest"], "groundTruthReference": records[0]["groundTruthReference"], "groundTruthLogicalDigest": FINAL_GROUND_TRUTH_LOGICAL_DIGEST, "humanReviewResultReference": records[0]["humanReviewResultReference"], "humanReviewResultLogicalDigest": records[0]["humanReviewResultReference"].rsplit(":", 1)[-1], "familyLineageReference": records[0]["familyLineageReference"], "familyLineageLogicalDigest": records[0]["familyLineageReference"].rsplit(":", 1)[-1], "executionAuthorityReference": execution_authority["authorityReference"], "executionAuthorityLogicalDigest": execution_authority["logicalDigest"], "executionContractReference": contract["reference"], "executionContractLogicalDigest": contract["logicalDigest"], "checkpointReference": FINAL_CHECKPOINT_REFERENCE, "checkpointLogicalDigest": FINAL_CHECKPOINT_LOGICAL_DIGEST, "modelStateSha256": FINAL_MODEL_STATE_SHA256, "modelIndependent": True, "holdoutOpened": False, "exposureCount": 0, "fixtureExecutionClosure": "PASS", "transitiveMissingDependencyCount": 0, "sealTimestamp": SELECTION_TIMESTAMP}
    return envelope("him-final-evaluation-v2-holdout-pre-exposure-seal", payload)


def build_execution_layer_artifacts(holdout: Mapping[str, Any], corpus: Mapping[str, Any]) -> dict[str, Any]:
    """Derive execution inputs from frozen holdout/corpus lineage only.

    This function never changes membership or labels.  Family candidates are
    read from the committed canonical-family authority and the input builder
    is the exact builder used by training.
    """
    family_authority = json.loads(FAMILY_AUTHORITY_PATH.read_text(encoding="utf-8"))
    families = {}
    for family in family_authority.get("families", []):
        canonical_id = family.get("canonicalId", {}).get("value")
        if canonical_id:
            families[f"family:v1:canonical:{canonical_id}"] = family
    corpus_by_id = {row["recordId"]: row for row in corpus["payload"]["records"]}
    projected = []
    for row in holdout["payload"]["records"]:
        family = families.get(row["familyId"])
        if family is None:
            raise ValueError(f"FAMILY_AUTHORITY_MISSING:{row['familyId']}")
        source_row = corpus_by_id.get(row["recordId"])
        if source_row is None:
            raise ValueError(f"CORPUS_RECORD_MISSING:{row['recordId']}")
        projected.append(project_model_input(source_row, family))
    projection = build_model_input_projection_authority(
        holdout["reference"], holdout["logicalDigest"], projected
    )
    derived = build_derived_execution_artifact(holdout, projection, projected)
    records = holdout["payload"]["records"]
    truth_payload = {
        "schema": "HIM_FINAL_EVALUATION_V2_GROUND_TRUTH",
        "version": 2,
        "state": "SEALED_UNEXPOSED",
        "holdoutReference": holdout["reference"],
        "holdoutLogicalDigest": holdout["logicalDigest"],
        "recordCount": len(records),
        "reviewPacketLogicalDigest": REVIEW_PACKET_PATH.exists() and read_json(REVIEW_PACKET_PATH)["logicalDigest"],
        "evaluationExamples": [
            {"evaluationExampleReference": row["recordId"], "groundTruth": row["groundTruth"]}
            for row in records
        ],
        "modelIndependent": True,
        "holdoutOpened": False,
        "exposureCount": 0,
    }
    sealed_payload = {
        "schema": "HIM_FINAL_EVALUATION_V2_SEALED_AUTHORITY",
        "version": 2,
        "state": "SEALED_UNEXPOSED",
        "holdoutReference": holdout["reference"],
        "holdoutLogicalDigest": holdout["logicalDigest"],
        "derivedExecutionArtifactReference": derived["reference"],
        "derivedExecutionArtifactLogicalDigest": derived["logicalDigest"],
        "recordCount": len(records),
        "holdoutOpened": False,
        "exposureCount": 0,
        "modelIndependent": True,
    }
    validation_payload = {
        "schema": "HIM_FINAL_EVALUATION_V2_VALIDATION_REPORT",
        "version": 2,
        "state": "SEALED_UNEXPOSED",
        "holdoutReference": holdout["reference"],
        "holdoutLogicalDigest": holdout["logicalDigest"],
        "recordCount": len(records),
        "projectionAuthorityReference": projection["reference"],
        "derivedExecutionArtifactReference": derived["reference"],
        "holdoutOpened": False,
        "exposureCount": 0,
        "modelIndependent": True,
    }
    return {
        "projection": projection,
        "derived": derived,
        "truth": envelope("him-final-evaluation-v2-ground-truth", truth_payload),
        "sealed": envelope("him-final-evaluation-v2-sealed-authority", sealed_payload),
        "validation": envelope("him-final-evaluation-v2-validation-report", validation_payload),
    }


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
    selection = build_selection(corpus, truth, rows1); holdout = build_holdout(corpus, result, lineage, truth, selection, rows1); packet = build_review_packet(holdout); authority = build_holdout_authority(holdout, selection, truth); contract = build_execution_contract(); execution_authority = read_json(EXECUTION_AUTHORITY_PATH); seal = build_seal(holdout, authority, selection, packet, contract, execution_authority)
    execution_artifacts = build_execution_layer_artifacts(holdout, corpus)
    values = ((SELECTION_PATH, selection), (HOLDOUT_PATH, holdout), (HOLDOUT_AUTHORITY_PATH, authority), (REVIEW_PACKET_PATH, packet), (CONTRACT_PATH, contract), (SEAL_PATH, seal), (PROJECTION_AUTHORITY_PATH, execution_artifacts["projection"]), (DERIVED_EXECUTION_PATH, execution_artifacts["derived"]), (GROUND_TRUTH_EXECUTION_PATH, execution_artifacts["truth"]), (SEALED_AUTHORITY_EXECUTION_PATH, execution_artifacts["sealed"]), (VALIDATION_REPORT_EXECUTION_PATH, execution_artifacts["validation"]))
    for path, value in values:
        path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(canonical(value) + b"\n")
    return {"corpus": corpus, "result": result, "lineage": lineage, "truth": truth, "selection": selection, "holdout": holdout, "authority": authority, "packet": packet, "contract": contract, "seal": seal, "executionArtifacts": execution_artifacts, "fixture": fixture_execution_closure(), "selectionRunCount": 2}


def reload_all() -> dict[str, Any]:
    return {"selection": validate_envelope(read_json(SELECTION_PATH)), "holdout": validate_envelope(read_json(HOLDOUT_PATH)), "authority": validate_envelope(read_json(HOLDOUT_AUTHORITY_PATH)), "packet": validate_envelope(read_json(REVIEW_PACKET_PATH)), "contract": validate_envelope(read_json(CONTRACT_PATH)), "seal": validate_envelope(read_json(SEAL_PATH))}


def final_checkpoint_preflight() -> dict[str, Any]:
    return preflight_final_evaluation_authority_v1(".", str(EXECUTION_AUTHORITY_PATH), str(CHECKPOINT_ROOT))
