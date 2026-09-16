from __future__ import annotations

import shutil
import tempfile
import unittest
from pathlib import Path

from him_trainer import blind_expanded_validation_v2 as evaluator
from him_trainer.final_evaluation_authority_v1 import execute_synthetic_fixture_v2
from him_trainer.final_evaluation_authority_v1 import (
    build_final_evaluation_authority_v1,
    FINAL_HOLDOUT_AUTHORITY_FILENAME,
    FINAL_HOLDOUT_AUTHORITY_REFERENCE,
    FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST,
    FINAL_CHECKPOINT_MANIFEST_RELATIVE_PATH,
    FINAL_MODEL_STATE_RELATIVE_PATH,
    REQUIRED_MODEL_ROOT_FILES,
    persist_final_evaluation_authority_v1,
    verify_final_holdout_execution_path_v1,
)
from him_trainer.final_evaluation_v2_execution_contract import (
    build_execution_contract,
    build_future_holdout_requirement_set,
    logical_digest,
    validate_fixture_preseal,
)


ROOT = Path(__file__).resolve().parents[3]
SEALED_ROOT = ROOT / "build/knowledge/reports/him/training/p2/corpus-v2/blind-expanded-validation-v2"


class FinalEvaluationV2ExecutionContractTest(unittest.TestCase):
    def test_contract_and_future_requirements_are_self_describing(self) -> None:
        contract = build_execution_contract()
        self.assertEqual(contract["logicalDigest"], logical_digest(contract["payload"]))
        self.assertEqual(contract["payload"]["productiveEntrypoint"], "him_trainer.final_evaluation_authority_v1")
        self.assertEqual(len(contract["payload"]["inputs"]), 17)
        self.assertEqual(len(contract["payload"]["outputs"]), 3)
        requirements = build_future_holdout_requirement_set()
        self.assertEqual(requirements["logicalDigest"], logical_digest(requirements["payload"]))
        self.assertEqual(len(requirements["payload"]["requirements"]), 17)

    def test_negative_review_packet_fails_before_exposure_and_restore_passes(self) -> None:
        with tempfile.TemporaryDirectory(prefix="him-final-evaluation-v2-fixture-") as directory:
            fixture = Path(directory)
            shutil.copytree(SEALED_ROOT, fixture, dirs_exist_ok=True)
            packet = fixture / "review-packet.v2.json"
            packet.unlink()
            negative = validate_fixture_preseal(fixture)
            self.assertEqual(negative["state"], "FAIL")
            self.assertEqual(negative["missingInputCount"], 1)
            self.assertFalse(negative["holdoutOpened"])
            self.assertEqual(negative["exposureCount"], 0)
            shutil.copy2(SEALED_ROOT / "review-packet.v2.json", packet)
            positive = validate_fixture_preseal(fixture)
            self.assertEqual(positive["state"], "PASS")
            self.assertEqual(positive["missingInputCount"], 0)

    def test_final_entrypoint_fixture_crosses_review_packet_and_persists_roundtrip(self) -> None:
        with tempfile.TemporaryDirectory(prefix="him-final-evaluation-v2-execution-") as directory:
            root = Path(directory)
            evaluation = root / "evaluation"
            output = root / "output"
            shutil.copytree(SEALED_ROOT, evaluation)
            result = execute_synthetic_fixture_v2(evaluation_root=evaluation, output_root=output)
            self.assertEqual(result["result"]["result"]["resultPayload"]["evaluationExampleCount"], 21)
            self.assertEqual(result["result"]["result"]["resultPayload"]["executionCounters"]["modelDeserializationCount"], 0)
            self.assertEqual(result["report"]["reportPayload"]["fixtureNamespace"], "HIM_FINAL_EVALUATION_V2_FIXTURE_ONLY")
            self.assertTrue(result["result"]["rawPath"].is_file())
            self.assertTrue(result["result"]["resultPath"].is_file())
            self.assertTrue(result["reportPath"].is_file())

    def test_productive_preflight_rejects_missing_review_packet(self) -> None:
        with tempfile.TemporaryDirectory(prefix="him-final-evaluation-v2-preflight-") as directory:
            root = Path(directory)
            runtime = root / "runtime"
            execution = root / "workspace"
            evaluation = root / "sealed"
            holdout_authority = root / "holdout-authority"
            model = root / "model"
            output = root / "output"
            shutil.copytree(SEALED_ROOT, evaluation)
            (evaluation / "review-packet.v2.json").unlink()
            manifest = execution / FINAL_CHECKPOINT_MANIFEST_RELATIVE_PATH
            manifest.parent.mkdir(parents=True, exist_ok=True)
            manifest.write_text('{"checkpointReference":"him-training-checkpoint:v2:beba3ba4ba75ba1d117f3ed13482e8389b2f428406dd92d4cc41d1b476140197","checkpointLogicalDigest":"923d0a479847b21e57ff4b1c5649c1a38f07bb3a431b8e06ea9bec50bcd54632","modelState":{"relativePath":"checkpoint/model-state.pt"}}', encoding="utf-8")
            (execution / FINAL_MODEL_STATE_RELATIVE_PATH).write_bytes(b"fixture")
            holdout_authority.mkdir(parents=True)
            (holdout_authority / FINAL_HOLDOUT_AUTHORITY_FILENAME).write_text('{"reference":"' + FINAL_HOLDOUT_AUTHORITY_REFERENCE + '","logicalDigest":"' + FINAL_HOLDOUT_AUTHORITY_LOGICAL_DIGEST + '"}', encoding="utf-8")
            model.mkdir()
            for filename in REQUIRED_MODEL_ROOT_FILES:
                (model / filename).write_text("fixture", encoding="utf-8")
            authority = runtime / "training/him/runtime/a100/final-evaluation-authority-v1/final-evaluation-authority.v1.json"
            persist_final_evaluation_authority_v1(authority, build_final_evaluation_authority_v1())
            package = runtime / "trainer/him_trainer"
            package.mkdir(parents=True)
            for name in ("final_evaluation_authority_v1.py", "blind_expanded_validation_v2.py", "corpus_assembly_v2.py", "point12_token_tensor_builder_v1.py", "point13_model_forward_v1.py"):
                (package / name).write_text("# fixture", encoding="utf-8")
            (runtime / "runtime-identity.json").write_text("{}", encoding="utf-8")
            plan = verify_final_holdout_execution_path_v1(runtime, authority, execution, holdout_authority_root=holdout_authority, checkpoint_manifest=manifest, model_root=model, tokenizer_path=model / "tokenizer.json", output_root=output, evaluation_root=evaluation)
            self.assertEqual(plan["state"], "FINAL_HOLDOUT_EXECUTION_PATH_BLOCKED")
            self.assertEqual(plan["holdoutOpened"], False)
            self.assertEqual(plan["modelDeserializationCount"], 0)
            self.assertTrue(any(gap["role"] == "sealedRoot:review-packet.v2.json" for gap in plan["gaps"]))


if __name__ == "__main__":
    unittest.main()
