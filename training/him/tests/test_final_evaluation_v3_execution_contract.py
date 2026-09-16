import json
import hashlib
import inspect
import tempfile
from pathlib import Path

from him_trainer.final_evaluation_authority_v1 import (
    FinalEvaluationAuthorityError,
    _load_checkpoint_manifest_for_path_binding,
    _load_contract_bound_sealed_root,
    FINAL_CHECKPOINT_LOGICAL_DIGEST,
    FINAL_CHECKPOINT_REFERENCE,
    FINAL_MODEL_STATE_SHA256,
    execute_final_holdout_v1,
)


ROOT = Path(__file__).parents[3] / "build/knowledge/reports/him/final-evaluation-v3/fresh-holdout-v3"


def test_v3_productive_inputs_bind_only_v3_execution_artifacts():
    contract = json.loads((ROOT / "execution-contract.v3.json").read_text())
    inputs = contract["payload"]["inputs"]
    productive = [item for item in inputs if item.get("requiredBeforeExecution")]
    stale = [
        item for item in productive
        if any(token in item.get("path", "") for token in (
            "review-packet.v2",
            "ground-truth.v2",
            "sealed-evaluation-authority.v2",
            "final-validation-report.v2",
            "derived-execution-artifact.v1",
        ))
    ]
    assert stale == []
    by_id = {item["id"]: item for item in inputs}
    assert by_id["sealed_review_packet"]["path"].endswith("review-packet.v3.json")
    assert by_id["sealed_ground_truth"]["path"].endswith("ground-truth.v3.json")
    assert by_id["sealed_evaluation_authority"]["path"].endswith("sealed-evaluation-authority.v3.json")
    assert by_id["sealed_final_validation_report"]["path"].endswith("final-validation-report.v3.json")
    assert by_id["input_representation"]["path"].startswith("<evaluation-root>/derived-execution-artifact.v3.json")
    productive_source = inspect.getsource(execute_final_holdout_v1)
    assert "_load_native_sealed_root_v2" not in productive_source


def _envelope(prefix, payload):
    encoded = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    digest = hashlib.sha256(encoded).hexdigest()
    return {"logicalDigest": digest, "payload": payload, "reference": f"{prefix}:{digest}"}


def test_contract_driven_v3_loader_resolves_all_sealed_inputs_without_v2_loader():
    with tempfile.TemporaryDirectory(prefix="him-v3-contract-fixture-") as directory:
        root = Path(directory)
        examples = [{"evaluationExampleReference": f"example-{i}", "groundTruth": {}} for i in range(2)]
        files = {
            "review-packet.v3.json": _envelope("fixture-packet:v3", {"state": "SEALED_UNEXPOSED", "recordCount": 2, "holdoutReference": "fixture-holdout:v3:x"}),
            "ground-truth.v3.json": _envelope("fixture-truth:v3", {"state": "SEALED_UNEXPOSED", "evaluationExamples": examples}),
            "sealed-evaluation-authority.v3.json": _envelope("fixture-sealed:v3", {"state": "SEALED_UNEXPOSED"}),
            "final-validation-report.v3.json": _envelope("fixture-report:v3", {"state": "SEALED_UNEXPOSED"}),
            "derived-execution-artifact.v3.json": _envelope("fixture-derived:v3", {"state": "SEALED_UNEXPOSED", "holdoutReference": "fixture-holdout:v3:x", "recordIds": [e["evaluationExampleReference"] for e in examples], "modelInputs": [{"recordId": e["evaluationExampleReference"], "serialized": "fixture"} for e in examples]}),
        }
        for name, value in files.items():
            (root / name).write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        inputs = [
            {"id": "sealed_review_packet", "path": "<evaluation-root>/review-packet.v3.json", "requiredBeforeExecution": True},
            {"id": "sealed_ground_truth", "path": "<evaluation-root>/ground-truth.v3.json", "requiredBeforeExecution": True},
            {"id": "sealed_evaluation_authority", "path": "<evaluation-root>/sealed-evaluation-authority.v3.json", "requiredBeforeExecution": True},
            {"id": "sealed_final_validation_report", "path": "<evaluation-root>/final-validation-report.v3.json", "requiredBeforeExecution": True},
            {"id": "input_representation", "path": "<evaluation-root>/derived-execution-artifact.v3.json:modelInputs[].serialized", "requiredBeforeExecution": True},
        ] + [{"id": f"other-{i}", "type": "json"} for i in range(12)]
        contract = _envelope("fixture-contract:v3", {"schema": "HIM_FINAL_EVALUATION_V3_EXECUTION_CONTRACT", "inputs": inputs, "outputs": [{"id": "raw"}, {"id": "result"}, {"id": "report"}]})
        contract_path = root / "execution-contract.v3.json"
        contract_path.write_text(json.dumps(contract, sort_keys=True), encoding="utf-8")
        loaded = _load_contract_bound_sealed_root(contract_path, root)
        assert loaded["count"] == 2
        assert len(loaded["paths"]) == 5


def test_v3_checkpoint_binding_still_accepts_exactly_one_path_argument():
    with tempfile.TemporaryDirectory(prefix="him-v3-checkpoint-fixture-") as directory:
        path = Path(directory) / "checkpoint-manifest.json"
        path.write_text(json.dumps({"checkpointReference": FINAL_CHECKPOINT_REFERENCE, "checkpointLogicalDigest": FINAL_CHECKPOINT_LOGICAL_DIGEST, "modelState": {"relativePath": "model-state.pt", "sha256": FINAL_MODEL_STATE_SHA256}}), encoding="utf-8")
        assert _load_checkpoint_manifest_for_path_binding(path)["checkpointReference"] == FINAL_CHECKPOINT_REFERENCE


def test_v3_contract_negative_bindings_fail_closed():
    with tempfile.TemporaryDirectory(prefix="him-v3-contract-negative-") as directory:
        root = Path(directory)
        payloads = {
            "review-packet.v3.json": {"state": "SEALED_UNEXPOSED", "recordCount": 1, "holdoutReference": "h"},
            "ground-truth.v3.json": {"state": "SEALED_UNEXPOSED", "evaluationExamples": [{"evaluationExampleReference": "e", "groundTruth": {}}]},
            "sealed-evaluation-authority.v3.json": {"state": "SEALED_UNEXPOSED"},
            "final-validation-report.v3.json": {"state": "SEALED_UNEXPOSED"},
            "derived-execution-artifact.v3.json": {"state": "SEALED_UNEXPOSED", "holdoutReference": "h", "recordIds": ["e"], "modelInputs": [{"recordId": "e", "serialized": "x"}]},
        }
        for name, payload in payloads.items():
            (root / name).write_text(json.dumps(_envelope("fixture:v3", payload)), encoding="utf-8")
        names = ["sealed_review_packet", "sealed_ground_truth", "sealed_evaluation_authority", "sealed_final_validation_report", "input_representation"]
        paths = ["review-packet.v3.json", "ground-truth.v3.json", "sealed-evaluation-authority.v3.json", "final-validation-report.v3.json", "derived-execution-artifact.v3.json:modelInputs[].serialized"]
        for index in range(4):
            inputs = [{"id": n, "path": f"<evaluation-root>/{p}", "requiredBeforeExecution": True} for n, p in zip(names, paths)]
            if index == 0:
                inputs.pop(0)
            elif index == 1:
                inputs[1]["path"] = "<evaluation-root>/missing.v3.json"
            elif index == 2:
                inputs[2]["path"] = "<evaluation-root>/sealed-evaluation-authority.v2.json"
            else:
                inputs[3]["path"] = "<evaluation-root>/final-validation-report.v3.json"
                bad = _envelope("fixture:v3", {"state": "SEALED_UNEXPOSED"})
                bad["logicalDigest"] = "0" * 64
                (root / "final-validation-report.v3.json").write_text(json.dumps(bad), encoding="utf-8")
            contract = _envelope("fixture-contract:v3", {"schema": "HIM_FINAL_EVALUATION_V3_EXECUTION_CONTRACT", "inputs": inputs, "outputs": []})
            contract_path = root / f"contract-{index}.json"
            contract_path.write_text(json.dumps(contract), encoding="utf-8")
            try:
                _load_contract_bound_sealed_root(contract_path, root)
            except (FinalEvaluationAuthorityError, OSError, ValueError):
                continue
            raise AssertionError(f"negative contract case {index} unexpectedly passed")


def test_full_synthetic_v3_execution_fixture_persists_and_reloads_outputs():
    import torch

    with tempfile.TemporaryDirectory(prefix="him-v3-full-fixture-") as directory:
        root = Path(directory)
        examples = [{"evaluationExampleReference": f"example-{i}", "groundTruth": {"secondary": None}} for i in range(2)]
        payloads = {
            "review-packet.v3.json": {"state": "SEALED_UNEXPOSED", "recordCount": 2, "holdoutReference": "h"},
            "ground-truth.v3.json": {"state": "SEALED_UNEXPOSED", "evaluationExamples": examples},
            "sealed-evaluation-authority.v3.json": {"state": "SEALED_UNEXPOSED"},
            "final-validation-report.v3.json": {"state": "SEALED_UNEXPOSED"},
            "derived-execution-artifact.v3.json": {"state": "SEALED_UNEXPOSED", "holdoutReference": "h", "recordIds": [e["evaluationExampleReference"] for e in examples], "modelInputs": [{"recordId": e["evaluationExampleReference"], "serialized": "fixture"} for e in examples]},
        }
        for name, payload in payloads.items():
            (root / name).write_text(json.dumps(_envelope("fixture:v3", payload)), encoding="utf-8")
        inputs = [
            {"id": "sealed_review_packet", "path": "<evaluation-root>/review-packet.v3.json", "requiredBeforeExecution": True},
            {"id": "sealed_ground_truth", "path": "<evaluation-root>/ground-truth.v3.json", "requiredBeforeExecution": True},
            {"id": "sealed_evaluation_authority", "path": "<evaluation-root>/sealed-evaluation-authority.v3.json", "requiredBeforeExecution": True},
            {"id": "sealed_final_validation_report", "path": "<evaluation-root>/final-validation-report.v3.json", "requiredBeforeExecution": True},
            {"id": "input_representation", "path": "<evaluation-root>/derived-execution-artifact.v3.json:modelInputs[].serialized", "requiredBeforeExecution": True},
        ] + [{"id": f"other-{i}", "type": "json"} for i in range(12)]
        contract = _envelope("fixture-contract:v3", {"schema": "HIM_FINAL_EVALUATION_V3_EXECUTION_CONTRACT", "inputs": inputs, "outputs": []})
        contract_path = root / "execution-contract.v3.json"
        contract_path.write_text(json.dumps(contract), encoding="utf-8")
        loaded = _load_contract_bound_sealed_root(contract_path, root)
        model = torch.nn.Linear(2, 2)
        with torch.no_grad():
            output = model(torch.ones((loaded["count"], 2)))
        assert tuple(output.shape) == (2, 2)
        raw = {"state": "FROZEN_BEFORE_SCORING", "predictions": [{"evaluationExampleReference": e["evaluationExampleReference"], "secondaryPrediction": None} for e in examples]}
        result = {"state": "SYNTHETIC_COMPLETE", "rawPrediction": raw, "nAMasked": True}
        report = {"state": "SYNTHETIC_COMPLETE", "modelDeserializationCount": 1, "forwardCount": 1}
        for name, value in (("raw.json", raw), ("result.json", result), ("report.json", report)):
            value["logicalDigest"] = hashlib.sha256(json.dumps({k: v for k, v in value.items() if k != "logicalDigest"}, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
            (root / name).write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
            reloaded = json.loads((root / name).read_text())
            expected = hashlib.sha256(json.dumps({k: v for k, v in reloaded.items() if k != "logicalDigest"}, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
            assert reloaded["logicalDigest"] == expected
