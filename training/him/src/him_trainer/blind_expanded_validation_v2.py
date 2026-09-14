"""Model-blind V2 evaluator for the sealed 21-example validation set.

The V1 evaluator is intentionally left unchanged.  This adapter owns only the
new sealed-authority bindings and reuses the existing V2 input representation,
Point-13 model, and the same immutable result pattern.  Synthetic execution
uses exactly the same prediction, scoring, persistence, and reload functions
as real execution, without importing torch or loading a model.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import os
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping, Sequence


CONTRACT_ID = "HIM_P2_V2_BLIND_EXPANDED_VALIDATION_EVALUATOR"
VERSION = 2
EXPECTED_TOTAL = 21
EXPECTED_PRIMARY_ACTIVE = 13
EXPECTED_PRIMARY_IDENTITY = 6
EXPECTED_PRIMARY_VARIANT = 7
EXPECTED_SECONDARY_ACTIVE = 21
EXPECTED_SECONDARY_COMPATIBLE = 13
EXPECTED_SECONDARY_REJECT = 8
MAX_SEQUENCE_LENGTH_V2 = 256

EXPECTED_CHECKPOINT_REFERENCE = (
    "him-training-checkpoint:v2:"
    "4897d7957c60ebdee88d3192c0e40fb16d40a2d0e388b304d5c5ad9de3b6094f"
)
EXPECTED_CHECKPOINT_DIGEST = "a751b9ee9cf2304bc2adf05f1af58971fca583ebcf6a0633929f750a252bcf72"

PRIMARY_CODES = {"IDENTITY": 2, "VARIANT": 3}
PRIMARY_NAMES = {1: "OTHER", 2: "IDENTITY", 3: "VARIANT", 4: "OTHER", 5: "OTHER"}
SECONDARY_CODES = {"COMPATIBLE": 0, "REJECT": 1}
SECONDARY_NAMES = {0: "COMPATIBLE", 1: "REJECT"}

RESULT_FILENAME = "evaluation-result.v2.json"
RAW_PREDICTIONS_FILENAME = "raw-predictions.v2.json"
DIGEST_SCHEME = "sha256(canonical UTF-8 JSON; ensure_ascii=false, sort_keys=true, compact separators)"


class BlindExpandedV2Error(ValueError):
    """Raised for any failed V2 authority or execution gate."""


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise BlindExpandedV2Error(message)


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def _digest(value: Any) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def _read_json(path: Path) -> dict[str, Any]:
    _require(path.is_file() and not path.is_symlink(), f"FILE_INVALID:{path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise BlindExpandedV2Error(f"JSON_INVALID:{path}") from error
    _require(isinstance(value, dict), f"OBJECT_REQUIRED:{path}")
    return value


def _validate_outer(path: Path, payload_key: str) -> dict[str, Any]:
    value = _read_json(path)
    payload = value.get(payload_key)
    _require(isinstance(payload, Mapping), f"PAYLOAD_INVALID:{path.name}")
    actual = _digest(payload)
    _require(value.get("logicalDigest") == actual, f"DIGEST_MISMATCH:{path.name}")
    reference = value.get("authorityReference", value.get("reportReference"))
    _require(isinstance(reference, str) and reference.endswith(f":{actual}"), f"REFERENCE_MISMATCH:{path.name}")
    return value


@dataclass(frozen=True)
class SealedV2Set:
    root: Path
    packet: Mapping[str, Any]
    ground_truth: Mapping[str, Any]
    sealed_authority: Mapping[str, Any]
    final_report: Mapping[str, Any]
    examples: tuple[Mapping[str, Any], ...]

    @property
    def packet_reference(self) -> str:
        return str(self.packet["authorityReference"])

    @property
    def packet_digest(self) -> str:
        return str(self.packet["logicalDigest"])

    @property
    def ground_truth_reference(self) -> str:
        return str(self.ground_truth["authorityReference"])

    @property
    def ground_truth_digest(self) -> str:
        return str(self.ground_truth["logicalDigest"])

    @property
    def sealed_authority_reference(self) -> str:
        return str(self.sealed_authority["authorityReference"])

    @property
    def sealed_authority_digest(self) -> str:
        return str(self.sealed_authority["logicalDigest"])


def load_sealed_v2_set(root: str | Path) -> SealedV2Set:
    """Reload and cross-bind all four persisted model-blind V2 artifacts."""

    directory = Path(root)
    packet = _validate_outer(directory / "review-packet.v2.json", "authorityPayload")
    ground_truth = _validate_outer(directory / "ground-truth.v2.json", "authorityPayload")
    sealed_authority = _validate_outer(directory / "sealed-evaluation-authority.v2.json", "authorityPayload")
    final_report = _validate_outer(directory / "final-validation-report.v2.json", "reportPayload")

    packet_payload = packet["authorityPayload"]
    truth_payload = ground_truth["authorityPayload"]
    sealed_payload = sealed_authority["authorityPayload"]
    report_payload = final_report["reportPayload"]
    _require(packet_payload.get("state") == "AWAITING_HUMAN_ADJUDICATION", "PACKET_STATE_INVALID")
    _require(truth_payload.get("state") == "SEALED_IMMUTABLE", "GROUND_TRUTH_STATE_INVALID")
    _require(sealed_payload.get("state") == "SEALED" and sealed_payload.get("immutable") is True, "SEALED_AUTHORITY_STATE_INVALID")
    _require(truth_payload.get("reviewPacketReference") == packet["authorityReference"], "GROUND_TRUTH_PACKET_BINDING_INVALID")
    _require(truth_payload.get("reviewPacketLogicalDigest") == packet["logicalDigest"], "GROUND_TRUTH_PACKET_DIGEST_INVALID")
    _require(sealed_payload.get("groundTruthReference") == ground_truth["authorityReference"], "SEALED_AUTHORITY_TRUTH_BINDING_INVALID")
    _require(sealed_payload.get("groundTruthLogicalDigest") == ground_truth["logicalDigest"], "SEALED_AUTHORITY_TRUTH_DIGEST_INVALID")
    _require(sealed_payload.get("reviewPacketReference") == packet["authorityReference"], "SEALED_AUTHORITY_PACKET_BINDING_INVALID")
    _require(sealed_payload.get("reviewPacketLogicalDigest") == packet["logicalDigest"], "SEALED_AUTHORITY_PACKET_DIGEST_INVALID")
    _require(report_payload.get("groundTruthReference") == ground_truth["authorityReference"], "REPORT_TRUTH_BINDING_INVALID")
    _require(report_payload.get("groundTruthLogicalDigest") == ground_truth["logicalDigest"], "REPORT_TRUTH_DIGEST_INVALID")
    _require(report_payload.get("sealedAuthorityReference") == sealed_authority["authorityReference"], "REPORT_AUTHORITY_BINDING_INVALID")
    _require(report_payload.get("sealedAuthorityLogicalDigest") == sealed_authority["logicalDigest"], "REPORT_AUTHORITY_DIGEST_INVALID")

    examples = truth_payload.get("evaluationExamples")
    packet_units = packet_payload.get("units")
    _require(isinstance(examples, list) and isinstance(packet_units, list), "SEALED_MEMBERSHIP_INVALID")
    _require(len(examples) == EXPECTED_TOTAL and len(packet_units) == EXPECTED_TOTAL, "SEALED_EXAMPLE_COUNT_INVALID")
    _require(truth_payload.get("humanDecisionCount") == EXPECTED_TOTAL, "GROUND_TRUTH_DECISION_COUNT_INVALID")
    packet_by_unit = {u.get("reviewUnitId"): u for u in packet_units if isinstance(u, Mapping)}
    _require(len(packet_by_unit) == EXPECTED_TOTAL, "PACKET_UNIT_ID_INVALID")
    seen: set[str] = set()
    for example in examples:
        _require(isinstance(example, Mapping), "GROUND_TRUTH_EXAMPLE_INVALID")
        ref = example.get("evaluationExampleReference")
        unit = example.get("reviewUnitReference")
        _require(isinstance(ref, str) and ref not in seen, "GROUND_TRUTH_EXAMPLE_REFERENCE_INVALID")
        _require(isinstance(unit, str) and unit in packet_by_unit, "GROUND_TRUTH_UNIT_BINDING_INVALID")
        seen.add(ref)
        packet_unit = packet_by_unit[unit]
        for field in ("sourceRecordReference", "sourceEvidenceReference", "observedTerm", "source", "decisionContextReference"):
            _require(example.get(field) == packet_unit.get(field), f"PACKET_BINDING_INVALID:{field}")
        model_input = example.get("modelInput")
        _require(isinstance(model_input, Mapping) and model_input.get("serialized") == packet_unit.get("inputRepresentation", {}).get("serialized"), "INPUT_BINDING_INVALID")
        _require(example.get("reviewPacketLogicalDigest") == packet["logicalDigest"], "EXAMPLE_PACKET_DIGEST_INVALID")
    _require(len(seen) == EXPECTED_TOTAL, "GROUND_TRUTH_EXAMPLE_REFERENCE_DUPLICATE")

    coverage = truth_payload.get("coverage")
    _require(isinstance(coverage, Mapping), "GROUND_TRUTH_COVERAGE_INVALID")
    expected_coverage = {
        "evaluationExampleCount": EXPECTED_TOTAL,
        "primaryIdentityCount": EXPECTED_PRIMARY_IDENTITY,
        "primaryVariantCount": EXPECTED_PRIMARY_VARIANT,
        "primaryActiveCount": EXPECTED_PRIMARY_ACTIVE,
        "secondaryCompatibleCount": EXPECTED_SECONDARY_COMPATIBLE,
        "secondaryRejectCount": EXPECTED_SECONDARY_REJECT,
        "secondaryActiveCount": EXPECTED_SECONDARY_ACTIVE,
    }
    for field, expected in expected_coverage.items():
        _require(coverage.get(field) == expected, f"SEALED_COVERAGE_INVALID:{field}")
    _require(all(example.get("primaryTarget") in {"IDENTITY", "VARIANT", "NOT_APPLICABLE"} for example in examples), "PRIMARY_TARGET_INVALID")
    _require(sum(example.get("primaryTarget") == "IDENTITY" for example in examples) == EXPECTED_PRIMARY_IDENTITY, "IDENTITY_COUNT_INVALID")
    _require(sum(example.get("primaryTarget") == "VARIANT" for example in examples) == EXPECTED_PRIMARY_VARIANT, "VARIANT_COUNT_INVALID")
    _require(sum(example.get("primaryTarget") == "NOT_APPLICABLE" for example in examples) == EXPECTED_TOTAL - EXPECTED_PRIMARY_ACTIVE, "PRIMARY_INACTIVE_COUNT_INVALID")
    _require(sum(example.get("candidateCompatibility") == "COMPATIBLE" for example in examples) == EXPECTED_SECONDARY_COMPATIBLE, "COMPATIBLE_COUNT_INVALID")
    _require(sum(example.get("candidateCompatibility") == "REJECT" for example in examples) == EXPECTED_SECONDARY_REJECT, "REJECT_COUNT_INVALID")
    _require(len({example.get("familyReference") for example in examples}) == 9, "FAMILY_COUNT_INVALID")
    _require(sum(example.get("primaryMask") == 1 for example in examples) == EXPECTED_PRIMARY_ACTIVE, "PRIMARY_MASK_COUNT_INVALID")
    _require(sum(example.get("secondaryMask") == 1 for example in examples) == EXPECTED_SECONDARY_ACTIVE, "SECONDARY_MASK_COUNT_INVALID")
    _require(sum(1 for example in examples if example.get("relation") == "UNRESOLVED_MIXED_RELATION") == 0, "UNRESOLVED_GROUND_TRUTH")
    return SealedV2Set(directory, packet, ground_truth, sealed_authority, final_report, tuple(examples))


def reconstruct_v2_inputs(sealed: SealedV2Set, tokenizer: Any | None = None) -> dict[str, Any]:
    """Parse exactly the frozen V2 strings; optionally validate tokenizer lengths."""

    from .corpus_assembly_v2 import parse_model_input_v2

    failure_count = 0
    truncation_count = 0
    reconstructed: list[dict[str, Any]] = []
    for example in sealed.examples:
        serialized = example.get("modelInput", {}).get("serialized")
        try:
            parsed = parse_model_input_v2(serialized)
            _require(parsed.serialize() == serialized, "INPUT_RECONSTRUCTION_NOT_IDENTITY")
            declared_length = example.get("tokenLength")
            _require(isinstance(declared_length, int) and declared_length <= MAX_SEQUENCE_LENGTH_V2, "SEQUENCE_LENGTH_AUTHORITY_INVALID")
            if tokenizer is not None:
                actual_length = len(tokenizer.encode(serialized, add_special_tokens=True).ids)
                _require(actual_length == declared_length, "SEQUENCE_LENGTH_MISMATCH")
                _require(actual_length <= MAX_SEQUENCE_LENGTH_V2, "SEQUENCE_TOO_LONG")
            reconstructed.append({"exampleReference": example["evaluationExampleReference"], "serialized": serialized, "tokenLength": declared_length})
        except (BlindExpandedV2Error, ValueError, TypeError):
            failure_count += 1
    _require(failure_count == 0, "INPUT_RECONSTRUCTION_FAILED")
    return {"inputs": tuple(reconstructed), "count": len(reconstructed), "failureCount": failure_count, "truncationCount": truncation_count}


def _validate_prediction_rows(rows: Any, *, count: int, width: int, code: str) -> list[list[float]]:
    _require(isinstance(rows, list) and len(rows) == count, f"{code}_COUNT_INVALID")
    result: list[list[float]] = []
    for row in rows:
        _require(isinstance(row, list) and len(row) == width, f"{code}_WIDTH_INVALID")
        values = [float(item) for item in row]
        _require(all(math.isfinite(item) for item in values), f"{code}_NON_FINITE")
        result.append(values)
    return result


def _validate_prediction_vectors(predictions: Mapping[str, Any], sealed: SealedV2Set) -> list[dict[str, Any]]:
    rows = predictions.get("predictions")
    _require(isinstance(rows, list), "PREDICTIONS_MISSING")
    _require(len(rows) == EXPECTED_TOTAL, "WRONG_EXAMPLE_COUNT")
    expected_ids = [str(example["evaluationExampleReference"]) for example in sealed.examples]
    actual_ids = [row.get("evaluationExampleReference") for row in rows if isinstance(row, Mapping)]
    _require(len(actual_ids) == EXPECTED_TOTAL, "PREDICTION_ROW_INVALID")
    _require(len(set(actual_ids)) == EXPECTED_TOTAL, "DUPLICATE_PREDICTION_ID")
    _require(actual_ids == expected_ids, "MISSING_OR_REORDERED_PREDICTION")
    for row in rows:
        _require(isinstance(row, Mapping), "PREDICTION_ROW_INVALID")
        _validate_prediction_rows(row.get("primaryLogits"), count=1, width=5, code="PRIMARY_LOGITS")
        _validate_prediction_rows(row.get("secondaryLogits"), count=1, width=2, code="SECONDARY_LOGITS")
        pp = row.get("primaryPrediction")
        sp = row.get("secondaryPrediction")
        _require(isinstance(pp, int) and pp in PRIMARY_NAMES, "PRIMARY_PREDICTION_INVALID")
        _require(isinstance(sp, int) and sp in SECONDARY_NAMES, "SECONDARY_PREDICTION_INVALID")
    return [dict(row) for row in rows]


def build_synthetic_predictions(sealed: SealedV2Set) -> dict[str, Any]:
    """Build deterministic perfect predictions without importing torch."""

    rows = []
    for example in sealed.examples:
        primary_code = PRIMARY_CODES.get(str(example["primaryTarget"]), 1)
        secondary_code = SECONDARY_CODES[str(example["candidateCompatibility"])]
        primary_logits = [-1.0] * 5
        primary_logits[primary_code - 1] = 1.0
        secondary_logits = [-1.0, -1.0]
        secondary_logits[secondary_code] = 1.0
        rows.append({
            "evaluationExampleReference": example["evaluationExampleReference"],
            "primaryLogits": [primary_logits],
            "secondaryLogits": [secondary_logits],
            "primaryPrediction": primary_code,
            "secondaryPrediction": secondary_code,
        })
    payload = {
        "schema": "HIM_P2_V2_BLIND_EXPANDED_RAW_PREDICTIONS",
        "version": VERSION,
        "state": "FROZEN_BEFORE_SCORING",
        "evaluationExampleCount": EXPECTED_TOTAL,
        "rawPredictionCount": EXPECTED_TOTAL,
        "reviewPacketLogicalDigest": sealed.packet_digest,
        "groundTruthLogicalDigest": sealed.ground_truth_digest,
        "predictions": rows,
    }
    prediction_digest = _digest(payload)
    return {"predictionReference": f"him-p2-blind-expanded-v2-predictions:{prediction_digest}", "logicalDigest": prediction_digest, "digestScheme": DIGEST_SCHEME, "payload": payload}


def persist_raw_predictions(output_root: str | Path, predictions: Mapping[str, Any]) -> Path:
    root = Path(output_root)
    root.mkdir(parents=True, exist_ok=True)
    destination = root / RAW_PREDICTIONS_FILENAME
    _require(not destination.exists(), "RAW_PREDICTIONS_ALREADY_EXIST")
    content = _canonical(predictions) + b"\n"
    destination.write_bytes(content)
    return destination


def reload_raw_predictions(path: str | Path, sealed: SealedV2Set) -> dict[str, Any]:
    value = _read_json(Path(path))
    payload = value.get("payload")
    _require(isinstance(payload, Mapping), "RAW_PREDICTIONS_PAYLOAD_INVALID")
    actual = _digest(payload)
    _require(value.get("logicalDigest") == actual, "RAW_PREDICTIONS_DIGEST_MISMATCH")
    _require(payload.get("state") == "FROZEN_BEFORE_SCORING", "RAW_PREDICTIONS_NOT_FROZEN")
    _require(payload.get("reviewPacketLogicalDigest") == sealed.packet_digest, "RAW_PACKET_BINDING_MISMATCH")
    _require(payload.get("groundTruthLogicalDigest") == sealed.ground_truth_digest, "RAW_TRUTH_BINDING_MISMATCH")
    _require(payload.get("rawPredictionCount") == EXPECTED_TOTAL, "RAW_PREDICTION_COUNT_INVALID")
    _validate_prediction_vectors(payload, sealed)
    return value


def _family_results(per_example: Sequence[Mapping[str, Any]]) -> list[dict[str, Any]]:
    groups: dict[str, list[Mapping[str, Any]]] = {}
    for row in per_example:
        groups.setdefault(str(row["family"]), []).append(row)
    result = []
    for family in sorted(groups):
        rows = groups[family]
        primary = [row for row in rows if row["primaryActive"]]
        secondary = rows
        result.append({
            "familyId": family,
            "exampleCount": len(rows),
            "primaryEvaluated": len(primary),
            "primaryCorrect": sum(bool(row["primaryCorrect"]) for row in primary),
            "secondaryEvaluated": len(secondary),
            "secondaryCorrect": sum(bool(row["secondaryCorrect"]) for row in secondary),
            "primaryResults": [
                {"evaluationExampleReference": row["evaluationExampleReference"], "predicted": row["predictedPrimary"], "correct": row["primaryCorrect"]}
                for row in primary
            ],
            "secondaryResults": [
                {"evaluationExampleReference": row["evaluationExampleReference"], "predicted": row["predictedSecondary"], "correct": row["secondaryCorrect"]}
                for row in secondary
            ],
        })
    return result


def _matrix(per_example: Sequence[Mapping[str, Any]], gold_key: str, pred_key: str, classes: Sequence[str], active_key: str | None = None) -> dict[str, dict[str, int]]:
    matrix = {gold: {pred: 0 for pred in classes} for gold in classes}
    for row in per_example:
        if active_key is not None and not row[active_key]:
            continue
        gold = str(row[gold_key])
        predicted = str(row[pred_key])
        matrix.setdefault(gold, {pred: 0 for pred in classes})
        if predicted not in classes:
            predicted = "OTHER"
        matrix[gold][predicted] += 1
    return matrix


def build_scored_result(*, sealed: SealedV2Set, raw_predictions: Mapping[str, Any], checkpoint_reference: str, checkpoint_digest: str, runtime_binding: Mapping[str, Any], reconstruction: Mapping[str, Any], counters: Mapping[str, int]) -> dict[str, Any]:
    _require(checkpoint_reference == EXPECTED_CHECKPOINT_REFERENCE, "CHECKPOINT_REFERENCE_GATE_FAILED")
    _require(checkpoint_digest == EXPECTED_CHECKPOINT_DIGEST, "CHECKPOINT_DIGEST_GATE_FAILED")
    predictions = _validate_prediction_vectors(raw_predictions["payload"], sealed)
    by_id = {row["evaluationExampleReference"]: row for row in predictions}
    per_example: list[dict[str, Any]] = []
    for example in sealed.examples:
        ref = str(example["evaluationExampleReference"])
        prediction = by_id[ref]
        gold_primary = example["primaryTarget"] if example["primaryTarget"] != "NOT_APPLICABLE" else None
        predicted_primary_name = PRIMARY_NAMES[prediction["primaryPrediction"]]
        primary_correct = None if gold_primary is None else predicted_primary_name == gold_primary
        gold_secondary = str(example["candidateCompatibility"])
        predicted_secondary_name = SECONDARY_NAMES[prediction["secondaryPrediction"]]
        secondary_correct = predicted_secondary_name == gold_secondary
        per_example.append({
            "evaluationExampleReference": ref,
            "unitId": example["reviewUnitReference"],
            "observedTerm": example["observedTerm"],
            "candidate": example["candidate"]["canonicalName"],
            "family": example["familyReference"],
            "goldRelation": example["relation"],
            "goldPrimary": gold_primary,
            "predictedPrimary": None if gold_primary is None else predicted_primary_name,
            "primaryActive": gold_primary is not None,
            "primaryCorrect": primary_correct,
            "goldSecondary": gold_secondary,
            "predictedSecondary": predicted_secondary_name,
            "secondaryCorrect": secondary_correct,
            "negativeBoundary": example["negativeBoundary"],
        })
    primary_rows = [row for row in per_example if row["primaryActive"]]
    primary_correct = sum(bool(row["primaryCorrect"]) for row in primary_rows)
    secondary_correct = sum(bool(row["secondaryCorrect"]) for row in per_example)
    identity_rows = [row for row in primary_rows if row["goldPrimary"] == "IDENTITY"]
    variant_rows = [row for row in primary_rows if row["goldPrimary"] == "VARIANT"]
    reject_rows = [row for row in per_example if row["goldSecondary"] == "REJECT"]
    compatible_rows = [row for row in per_example if row["goldSecondary"] == "COMPATIBLE"]
    boundary_rows = {name: [row for row in reject_rows if row["negativeBoundary"] == name] for name in ("WRONG_RELATION_LEVEL", "WRONG_SCOPE")}
    primary_matrix = _matrix(primary_rows, "goldPrimary", "predictedPrimary", ("IDENTITY", "VARIANT", "OTHER"))
    secondary_matrix = _matrix(per_example, "goldSecondary", "predictedSecondary", ("COMPATIBLE", "REJECT", "OTHER"))
    error_rows = [row for row in per_example if row["primaryCorrect"] is False or row["secondaryCorrect"] is False]
    identity_pred_variant = sum(row["predictedPrimary"] == "VARIANT" for row in identity_rows)
    reject_pred_compatible = sum(row["predictedSecondary"] == "COMPATIBLE" for row in reject_rows)
    identity_failure = "SYSTEMATIC_COLLAPSE" if identity_pred_variant == len(identity_rows) else ("SUBSTANTIALLY_CORRECTED" if all(row["primaryCorrect"] for row in identity_rows) else "PARTIAL_IMPROVEMENT")
    reject_failure = "SYSTEMATIC_COLLAPSE" if reject_pred_compatible == len(reject_rows) else ("SUBSTANTIALLY_CORRECTED" if all(row["secondaryCorrect"] for row in reject_rows) else "PARTIAL_IMPROVEMENT")
    result_payload = {
        "contractId": "HIM_P2_V2_BLIND_EXPANDED_VALIDATION_RESULT",
        "version": VERSION,
        "state": "REAL_OR_SYNTHETIC_EVALUATION_COMPLETE",
        "checkpointReference": checkpoint_reference,
        "checkpointLogicalDigest": checkpoint_digest,
        "sealedEvaluationAuthorityReference": sealed.sealed_authority_reference,
        "sealedEvaluationAuthorityDigest": sealed.sealed_authority_digest,
        "groundTruthReference": sealed.ground_truth_reference,
        "groundTruthLogicalDigest": sealed.ground_truth_digest,
        "reviewPacketReference": sealed.packet_reference,
        "reviewPacketLogicalDigest": sealed.packet_digest,
        "runtimeBinding": dict(runtime_binding),
        "evaluationExampleCount": EXPECTED_TOTAL,
        "rawPredictionReference": raw_predictions["predictionReference"],
        "rawPredictionLogicalDigest": raw_predictions["logicalDigest"],
        "reconstruction": dict(reconstruction),
        "primary": {
            "evaluated": len(primary_rows), "correct": primary_correct, "incorrect": len(primary_rows) - primary_correct, "accuracy": primary_correct / len(primary_rows),
            "goldIdentity": len(identity_rows), "goldVariant": len(variant_rows), "confusionMatrix": primary_matrix,
        },
        "secondary": {
            "evaluated": len(per_example), "correct": secondary_correct, "incorrect": len(per_example) - secondary_correct, "accuracy": secondary_correct / len(per_example),
            "goldCompatible": len(compatible_rows), "goldReject": len(reject_rows), "confusionMatrix": secondary_matrix,
        },
        "failureModes": {
            "identityTotal": len(identity_rows), "identityCorrect": sum(bool(row["primaryCorrect"]) for row in identity_rows), "identityPredictedVariant": identity_pred_variant,
            "identityToVariantErrorRate": identity_pred_variant / len(identity_rows), "identitySystematicCollapsePersists": identity_failure == "SYSTEMATIC_COLLAPSE",
            "rejectTotal": len(reject_rows), "rejectCorrect": sum(bool(row["secondaryCorrect"]) for row in reject_rows), "rejectPredictedCompatible": reject_pred_compatible,
            "rejectToCompatibleErrorRate": reject_pred_compatible / len(reject_rows), "rejectSystematicCollapsePersists": reject_failure == "SYSTEMATIC_COLLAPSE",
        },
        "negativeBoundary": {name: {"total": len(rows), "correct": sum(bool(row["secondaryCorrect"]) for row in rows), "accuracy": sum(bool(row["secondaryCorrect"]) for row in rows) / len(rows)} for name, rows in boundary_rows.items()},
        "diagnostics": {
            "GOLD_IDENTITY_PRED_IDENTITY": primary_matrix["IDENTITY"]["IDENTITY"],
            "GOLD_IDENTITY_PRED_VARIANT": primary_matrix["IDENTITY"]["VARIANT"],
            "GOLD_IDENTITY_PRED_OTHER": primary_matrix["IDENTITY"]["OTHER"],
            "GOLD_VARIANT_PRED_IDENTITY": primary_matrix["VARIANT"]["IDENTITY"],
            "GOLD_VARIANT_PRED_VARIANT": primary_matrix["VARIANT"]["VARIANT"],
            "GOLD_VARIANT_PRED_OTHER": primary_matrix["VARIANT"]["OTHER"],
            "GOLD_COMPATIBLE_PRED_COMPATIBLE": secondary_matrix["COMPATIBLE"]["COMPATIBLE"],
            "GOLD_COMPATIBLE_PRED_REJECT": secondary_matrix["COMPATIBLE"]["REJECT"],
            "GOLD_COMPATIBLE_PRED_OTHER": secondary_matrix["COMPATIBLE"]["OTHER"],
            "GOLD_REJECT_PRED_COMPATIBLE": secondary_matrix["REJECT"]["COMPATIBLE"],
            "GOLD_REJECT_PRED_REJECT": secondary_matrix["REJECT"]["REJECT"],
            "GOLD_REJECT_PRED_OTHER": secondary_matrix["REJECT"]["OTHER"],
            "V2_IDENTITY_TOTAL": len(identity_rows),
            "V2_IDENTITY_CORRECT": sum(bool(row["primaryCorrect"]) for row in identity_rows),
            "V2_IDENTITY_PREDICTED_VARIANT": identity_pred_variant,
            "IDENTITY_TO_VARIANT_ERROR_RATE": identity_pred_variant / len(identity_rows),
            "IDENTITY_SYSTEMATIC_COLLAPSE_PERSISTS": identity_failure == "SYSTEMATIC_COLLAPSE",
            "V2_REJECT_TOTAL": len(reject_rows),
            "V2_REJECT_CORRECT": sum(bool(row["secondaryCorrect"]) for row in reject_rows),
            "V2_REJECT_PREDICTED_COMPATIBLE": reject_pred_compatible,
            "REJECT_TO_COMPATIBLE_ERROR_RATE": reject_pred_compatible / len(reject_rows),
            "REJECT_SYSTEMATIC_COLLAPSE_PERSISTS": reject_failure == "SYSTEMATIC_COLLAPSE",
            "WRONG_RELATION_LEVEL_TOTAL": len(boundary_rows["WRONG_RELATION_LEVEL"]),
            "WRONG_RELATION_LEVEL_CORRECT": sum(bool(row["secondaryCorrect"]) for row in boundary_rows["WRONG_RELATION_LEVEL"]),
            "WRONG_SCOPE_TOTAL": len(boundary_rows["WRONG_SCOPE"]),
            "WRONG_SCOPE_CORRECT": sum(bool(row["secondaryCorrect"]) for row in boundary_rows["WRONG_SCOPE"]),
        },
        "perFamily": _family_results(per_example),
        "perUnitErrors": error_rows,
        "executionCounters": dict(counters),
        "modelOutputFrozenBeforeScoring": True,
        "holdout": {"opened": False, "exposureCount": 0},
        "scientificClassification": {
            "primaryGeneralizationStatus": "SUBSTANTIALLY_CORRECTED" if primary_correct == len(primary_rows) else "PARTIAL_IMPROVEMENT",
            "secondaryGeneralizationStatus": "SUBSTANTIALLY_CORRECTED" if secondary_correct == len(per_example) else "PARTIAL_IMPROVEMENT",
            "identityFailureModeStatus": identity_failure,
            "rejectFailureModeStatus": reject_failure,
            "qualificationStatus": "SYNTHETIC_CAPABILITY_ONLY",
        },
    }
    result_digest = _digest(result_payload)
    return {"resultReference": f"him-p2-blind-expanded-validation-result:v2:{result_digest}", "logicalDigest": result_digest, "digestScheme": DIGEST_SCHEME, "resultPayload": result_payload}


def persist_result(output_root: str | Path, result: Mapping[str, Any]) -> Path:
    root = Path(output_root)
    root.mkdir(parents=True, exist_ok=True)
    destination = root / RESULT_FILENAME
    _require(not destination.exists(), "RESULT_ALREADY_EXIST")
    destination.write_bytes(_canonical(result) + b"\n")
    return destination


def reload_result(path: str | Path, sealed: SealedV2Set, raw_predictions: Mapping[str, Any], checkpoint_reference: str, checkpoint_digest: str) -> dict[str, Any]:
    value = _read_json(Path(path))
    payload = value.get("resultPayload")
    _require(isinstance(payload, Mapping), "RESULT_PAYLOAD_INVALID")
    actual = _digest(payload)
    _require(value.get("logicalDigest") == actual, "RESULT_DIGEST_MISMATCH")
    _require(value.get("resultReference") == f"him-p2-blind-expanded-validation-result:v2:{actual}", "RESULT_REFERENCE_INVALID")
    _require(payload.get("checkpointReference") == checkpoint_reference and payload.get("checkpointLogicalDigest") == checkpoint_digest, "RESULT_CHECKPOINT_BINDING_INVALID")
    _require(payload.get("sealedEvaluationAuthorityReference") == sealed.sealed_authority_reference and payload.get("sealedEvaluationAuthorityDigest") == sealed.sealed_authority_digest, "RESULT_AUTHORITY_BINDING_INVALID")
    _require(payload.get("groundTruthReference") == sealed.ground_truth_reference and payload.get("groundTruthLogicalDigest") == sealed.ground_truth_digest, "RESULT_TRUTH_BINDING_INVALID")
    _require(payload.get("rawPredictionReference") == raw_predictions["predictionReference"] and payload.get("rawPredictionLogicalDigest") == raw_predictions["logicalDigest"], "RESULT_PREDICTION_BINDING_INVALID")
    _require(payload.get("evaluationExampleCount") == EXPECTED_TOTAL, "RESULT_EXAMPLE_COUNT_INVALID")
    return value


def validate_checkpoint_manifest(path: str | Path, *, expected_reference: str = EXPECTED_CHECKPOINT_REFERENCE, expected_digest: str = EXPECTED_CHECKPOINT_DIGEST) -> dict[str, Any]:
    manifest = _read_json(Path(path))
    _require(manifest.get("checkpointReference") == expected_reference, "CHECKPOINT_REFERENCE_GATE_FAILED")
    _require(manifest.get("checkpointLogicalDigest") == expected_digest, "CHECKPOINT_DIGEST_GATE_FAILED")
    return manifest


def synthetic_end_to_end(root: str | Path, output_root: str | Path | None = None) -> dict[str, Any]:
    sealed = load_sealed_v2_set(root)
    reconstruction = reconstruct_v2_inputs(sealed)
    _require(reconstruction["count"] == EXPECTED_TOTAL and reconstruction["failureCount"] == 0, "SYNTHETIC_RECONSTRUCTION_FAILED")
    raw = build_synthetic_predictions(sealed)
    owned_output = output_root is None
    output = Path(output_root) if output_root is not None else Path(tempfile.mkdtemp(prefix="him-p2-v2-evaluation-"))
    try:
        raw_path = persist_raw_predictions(output, raw)
    except Exception:
        if owned_output:
            _remove_temporary_output(output)
        raise
    reloaded_raw = reload_raw_predictions(raw_path, sealed)
    counters = {"modelDeserializationCount": 0, "forwardCount": 0, "predictionCount": EXPECTED_TOTAL, "logitCount": EXPECTED_TOTAL, "trainingCount": 0, "backwardCount": 0, "optimizerCreatedCount": 0, "optimizerStepCount": 0, "holdoutExposureCount": 0}
    result = build_scored_result(sealed=sealed, raw_predictions=reloaded_raw, checkpoint_reference=EXPECTED_CHECKPOINT_REFERENCE, checkpoint_digest=EXPECTED_CHECKPOINT_DIGEST, runtime_binding={"mode": "SYNTHETIC_MODEL_FREE", "evaluator": "him_trainer.blind_expanded_validation_v2"}, reconstruction=reconstruction, counters=counters)
    result_path = persist_result(output, result)
    reloaded_result = reload_result(result_path, sealed, reloaded_raw, EXPECTED_CHECKPOINT_REFERENCE, EXPECTED_CHECKPOINT_DIGEST)
    return {"sealed": sealed, "reconstruction": reconstruction, "rawPath": raw_path, "resultPath": result_path, "outputRoot": output, "raw": reloaded_raw, "result": reloaded_result}


def _remove_temporary_output(path: Path) -> None:
    """Remove only a temporary directory created by synthetic execution."""

    import shutil

    if path.name.startswith("him-p2-v2-evaluation-") and path.is_dir() and not path.is_symlink():
        shutil.rmtree(path)


def _runtime_binding(path: str | Path | None) -> dict[str, Any]:
    _require(path is not None, "RUNTIME_AUTHORITY_REQUIRED")
    value = _read_json(Path(path))
    _require(value.get("status") == "AUTHORIZED", "RUNTIME_AUTHORITY_NOT_AUTHORIZED")
    _require(value.get("trainingRuntimeAuthorized") is True, "RUNTIME_AUTHORITY_NOT_AUTHORIZED")
    _require(value.get("realTrainingExecutionAuthorized") is True, "REAL_EXECUTION_NOT_AUTHORIZED")
    _require(value.get("holdoutOpened") is False, "RUNTIME_HOLDOUT_OPEN")
    trainer = value.get("trainer")
    _require(isinstance(trainer, Mapping), "RUNTIME_TRAINER_BINDING_INVALID")
    _require(trainer.get("runnerModule") == "him_trainer.productive_training_p2_v2", "RUNTIME_TRAINER_BINDING_INVALID")
    _require(trainer.get("supportsRealExecution") is True, "RUNTIME_REAL_EXECUTION_UNSUPPORTED")
    identity = {key: item for key, item in value.items() if key not in {"logicalDigest", "reference"}}
    digest = _digest(identity)
    _require(value.get("logicalDigest") == digest, "RUNTIME_AUTHORITY_DIGEST_MISMATCH")
    _require(value.get("reference") == f"him-p2-training-runtime-authority-execution-enabled:v2:{digest}", "RUNTIME_AUTHORITY_REFERENCE_MISMATCH")
    return value


def _checkpoint_model_state_path(manifest_path: Path, manifest: Mapping[str, Any]) -> Path:
    model_state = manifest.get("modelState")
    _require(isinstance(model_state, Mapping), "CHECKPOINT_MODEL_STATE_METADATA_INVALID")
    relative = model_state.get("relativePath")
    _require(isinstance(relative, str) and relative and not Path(relative).is_absolute(), "CHECKPOINT_MODEL_STATE_PATH_INVALID")
    root = manifest_path.parent.parent
    path = (root / relative).resolve()
    _require(path.is_file() and not path.is_symlink() and path.is_relative_to(root.resolve()), "CHECKPOINT_MODEL_STATE_INVALID")
    expected_sha = model_state.get("sha256")
    _require(isinstance(expected_sha, str) and len(expected_sha) == 64, "CHECKPOINT_MODEL_STATE_DIGEST_INVALID")
    _require(hashlib.sha256(path.read_bytes()).hexdigest() == expected_sha, "CHECKPOINT_MODEL_STATE_DIGEST_MISMATCH")
    return path


def _build_real_input_tensors(sealed: SealedV2Set, tokenizer: Any) -> tuple[Any, Any, dict[str, Any]]:
    import torch

    rows = reconstruct_v2_inputs(sealed, tokenizer)
    lengths = [int(row["tokenLength"]) for row in rows["inputs"]]
    _require(lengths and max(lengths) <= MAX_SEQUENCE_LENGTH_V2, "SEQUENCE_TOO_LONG")
    target_length = max(lengths)
    encoded = [tokenizer.encode(row["serialized"], add_special_tokens=True).ids for row in rows["inputs"]]
    input_ids = torch.tensor([ids + [1] * (target_length - len(ids)) for ids in encoded], dtype=torch.int64, device="cuda:0")
    attention = torch.tensor([[1] * len(ids) + [0] * (target_length - len(ids)) for ids in encoded], dtype=torch.int64, device="cuda:0")
    return input_ids, attention, rows


def execute_real_v2(*, sealed_root: str | Path, output_root: str | Path, checkpoint_manifest_path: str | Path, runtime_authority_path: str | Path, model_root: str | Path, tokenizer_path: str | Path) -> dict[str, Any]:
    """Execute one exact V2 evaluation after all model-blind gates have passed."""

    import torch

    sealed = load_sealed_v2_set(sealed_root)
    runtime = _runtime_binding(runtime_authority_path)
    manifest_path = Path(checkpoint_manifest_path)
    manifest = validate_checkpoint_manifest(manifest_path)
    model_state_path = _checkpoint_model_state_path(manifest_path, manifest)
    model_binding_digest = manifest.get("identity", {}).get("modelBindingDigest")
    _require(isinstance(model_binding_digest, str) and len(model_binding_digest) == 64, "CHECKPOINT_MODEL_BINDING_INVALID")

    from .point12_token_tensor_builder_v1 import load_pinned_xlm_r_tokenizer_v1
    from .point13_model_forward_v1 import (
        build_him_model_execution_binding_v1,
        load_pinned_him_multi_head_model_from_root_v1,
        load_pinned_model_config_from_root_v1,
    )

    tokenizer = load_pinned_xlm_r_tokenizer_v1(tokenizer_path)
    input_ids, attention, reconstruction = _build_real_input_tensors(sealed, tokenizer)
    config = load_pinned_model_config_from_root_v1(Path(model_root))
    binding = build_him_model_execution_binding_v1(config, 7, model_binding_digest)
    model = load_pinned_him_multi_head_model_from_root_v1(binding, model_binding_digest, 7, Path(model_root), "cuda:0")
    try:
        state = torch.load(model_state_path, map_location="cpu", weights_only=True)
        _require(isinstance(state, Mapping), "CHECKPOINT_MODEL_STATE_INVALID")
        model.load_state_dict(state, strict=True)
        model.eval()
    except BlindExpandedV2Error:
        raise
    except Exception as error:
        raise BlindExpandedV2Error("CHECKPOINT_MODEL_STATE_RELOAD_FAILED") from error
    with torch.no_grad():
        output = model(input_ids, attention)
    primary_logits = getattr(output, "primary_logits", None)
    secondary_logits = getattr(output, "secondary_logits", None)
    _require(isinstance(primary_logits, torch.Tensor) and isinstance(secondary_logits, torch.Tensor), "MODEL_HEAD_OUTPUT_INVALID")
    _require(tuple(primary_logits.shape) == (EXPECTED_TOTAL, 5), "PRIMARY_HEAD_OUTPUT_SHAPE_INVALID")
    _require(tuple(secondary_logits.shape) == (EXPECTED_TOTAL, 2), "SECONDARY_HEAD_OUTPUT_SHAPE_INVALID")
    primary_predictions = (primary_logits.argmax(dim=1) + 1).tolist()
    secondary_predictions = secondary_logits.argmax(dim=1).tolist()
    rows = []
    for index, example in enumerate(sealed.examples):
        rows.append({
            "evaluationExampleReference": example["evaluationExampleReference"],
            "primaryLogits": [float(value) for value in primary_logits[index].detach().cpu().tolist()],
            "secondaryLogits": [float(value) for value in secondary_logits[index].detach().cpu().tolist()],
            "primaryPrediction": int(primary_predictions[index]),
            "secondaryPrediction": int(secondary_predictions[index]),
        })
    payload = {
        "schema": "HIM_P2_V2_BLIND_EXPANDED_RAW_PREDICTIONS",
        "version": VERSION,
        "state": "FROZEN_BEFORE_SCORING",
        "evaluationExampleCount": EXPECTED_TOTAL,
        "rawPredictionCount": EXPECTED_TOTAL,
        "reviewPacketLogicalDigest": sealed.packet_digest,
        "groundTruthLogicalDigest": sealed.ground_truth_digest,
        "predictions": rows,
    }
    prediction_digest = _digest(payload)
    raw = {"predictionReference": f"him-p2-blind-expanded-v2-predictions:{prediction_digest}", "logicalDigest": prediction_digest, "digestScheme": DIGEST_SCHEME, "payload": payload}
    raw_path = persist_raw_predictions(output_root, raw)
    reloaded_raw = reload_raw_predictions(raw_path, sealed)
    counters = {"modelDeserializationCount": 1, "forwardCount": 1, "predictionCount": EXPECTED_TOTAL, "logitCount": EXPECTED_TOTAL, "trainingCount": 0, "backwardCount": 0, "optimizerCreatedCount": 0, "optimizerStepCount": 0, "holdoutExposureCount": 0}
    result = build_scored_result(sealed=sealed, raw_predictions=reloaded_raw, checkpoint_reference=EXPECTED_CHECKPOINT_REFERENCE, checkpoint_digest=EXPECTED_CHECKPOINT_DIGEST, runtime_binding=runtime, reconstruction=reconstruction, counters=counters)
    result_path = persist_result(output_root, result)
    reloaded_result = reload_result(result_path, sealed, reloaded_raw, EXPECTED_CHECKPOINT_REFERENCE, EXPECTED_CHECKPOINT_DIGEST)
    return {"sealed": sealed, "rawPath": raw_path, "resultPath": result_path, "raw": reloaded_raw, "result": reloaded_result}


def run_cli(arguments: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog="him_trainer.blind_expanded_validation_v2")
    parser.add_argument("--evaluation-root", required=True)
    parser.add_argument("--output-root", required=True)
    parser.add_argument("--checkpoint-manifest")
    parser.add_argument("--runtime-authority")
    parser.add_argument("--model-root")
    parser.add_argument("--tokenizer-path")
    parser.add_argument("--synthetic", action="store_true")
    args = parser.parse_args(list(arguments) if arguments is not None else None)
    try:
        if args.synthetic:
            result = synthetic_end_to_end(args.evaluation_root, args.output_root)
            output = result["result"]["resultPayload"]
            print(json.dumps({"state": output["state"], "resultReference": result["result"]["resultReference"], "output": str(result["resultPath"]), "modelDeserializationCount": 0, "forwardCount": 0}, sort_keys=True))
            return 0
        _require(args.checkpoint_manifest is not None, "CHECKPOINT_MANIFEST_REQUIRED")
        for option, message in ((args.runtime_authority, "RUNTIME_AUTHORITY_REQUIRED"), (args.model_root, "MODEL_ROOT_REQUIRED"), (args.tokenizer_path, "TOKENIZER_PATH_REQUIRED")):
            _require(option is not None, message)
        result = execute_real_v2(sealed_root=args.evaluation_root, output_root=args.output_root, checkpoint_manifest_path=args.checkpoint_manifest, runtime_authority_path=args.runtime_authority, model_root=args.model_root, tokenizer_path=args.tokenizer_path)
        output = result["result"]["resultPayload"]
        print(json.dumps({"state": output["state"], "resultReference": result["result"]["resultReference"], "output": str(result["resultPath"]), "modelDeserializationCount": 1, "forwardCount": 1}, sort_keys=True))
        return 0
    except (BlindExpandedV2Error, OSError, UnicodeError, ValueError) as error:
        print(f"{type(error).__name__}: {error}", file=os.sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(run_cli())
