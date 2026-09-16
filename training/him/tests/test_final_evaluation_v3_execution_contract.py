import json
from pathlib import Path


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
