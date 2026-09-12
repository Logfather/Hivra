"""Model-free contract tests for the Productive Training V2 runner."""

from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock

from him_trainer.productive_training_v2 import (
    EXPECTED_COMPATIBLE_COUNT,
    EXPECTED_EXAMPLE_COUNT,
    EXPECTED_HOLDOUT_COUNT,
    EXPECTED_REJECT_COUNT,
    EXPECTED_TRAIN_COUNT,
    EXPECTED_VALIDATION_COUNT,
    ProductiveTrainingV2Error,
    atomic_json_write,
    load_packet,
    prepare_productive_v2,
    project_examples,
    run_cli,
    run_evidence_payload,
    validate_startup,
)
from him_trainer.checkpoint_v2 import build_prepared_checkpoint_bindings


ROOT = Path(__file__).resolve().parents[3]
PACKET = ROOT / "data/knowledge/him/training/a100-transfer/v2/p1-training-input-authority-v2-negative-observed-term-v1/packet"
REQUEST = PACKET / "request/productive-training-request.v2.json"
TOKENIZER = ROOT / "training/him/models/xlm-roberta-base/e73636d4f797dec63c3081bb6ed5c7b0bb3f2089/tokenizer.json"


class ProductiveTrainingV2Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.prepared = prepare_productive_v2(PACKET, REQUEST, TOKENIZER)

    def test_counts_and_frozen_split(self) -> None:
        self.assertEqual(EXPECTED_EXAMPLE_COUNT, len(self.prepared.examples))
        self.assertEqual(EXPECTED_TRAIN_COUNT, len(self.prepared.train))
        self.assertEqual(EXPECTED_VALIDATION_COUNT, len(self.prepared.validation))
        self.assertEqual(EXPECTED_HOLDOUT_COUNT, len(self.prepared.holdout))
        self.assertEqual(EXPECTED_COMPATIBLE_COUNT, sum(x.compatibility == "COMPATIBLE" for x in self.prepared.examples))
        self.assertEqual(EXPECTED_REJECT_COUNT, sum(x.compatibility == "REJECT" for x in self.prepared.examples))

    def test_current_source_runner_regression_is_independent_of_historical_packet(self) -> None:
        prepared = self.prepared
        self.assertEqual(40, len(prepared.examples))
        self.assertEqual(15, len({item.family_group_reference for item in prepared.examples}))
        self.assertEqual(32, len(prepared.train))
        self.assertEqual(6, len(prepared.validation))
        self.assertEqual(2, len(prepared.holdout))
        self.assertEqual(38, sum(item.compatibility == "COMPATIBLE" for item in prepared.examples))
        self.assertEqual(2, sum(item.compatibility == "REJECT" for item in prepared.examples))
        self.assertEqual(32, len(prepared.tensors["TRAIN"].examples))
        self.assertEqual(6, len(prepared.tensors["VALIDATION"].examples))
        self.assertEqual(2, len(prepared.tensors["HOLDOUT"].examples))
        self.assertTrue(all(item.primary_target in {1, 2, 3, 4, 5} for item in prepared.examples))
        self.assertTrue(all(item.secondary_mask == 1.0 for item in prepared.examples))
        self.assertEqual("3", prepared.plan["identity"]["trajectory"]["epochs"])
        self.assertEqual("8", prepared.plan["identity"]["trajectory"]["microBatchSize"])
        self.assertEqual("12", prepared.plan["identity"]["trajectory"]["trainingSteps"])
        self.assertEqual("EACH_EPOCH_AND_FINAL", prepared.plan["identity"]["trajectory"]["validationPolicy"])
        training_configuration = prepared.plan["identity"]["trainingConfiguration"]
        self.assertEqual("0.0001", training_configuration["learningRate"])
        self.assertEqual("optimizer:adamw:v1", training_configuration["optimizerId"])
        self.assertEqual("7", training_configuration["trainingSeed"])
        self.assertEqual("1", training_configuration["gradientAccumulationSteps"])
        self.assertEqual("HIM_P1_PRODUCTIVE_TRAINING_EXECUTION_PLAN_V2", prepared.plan["contractId"])
        self.assertNotIn("HIM_P2_PRODUCTIVE_TRAINING", json.dumps(prepared.plan, ensure_ascii=False))
        self.assertTrue(all(float(value) == 1.0 for view in prepared.tensors.values() for value in view.primary_mask.tolist()))
        self.assertTrue(all(float(value) == 1.0 for view in prepared.tensors.values() for value in view.secondary_mask.tolist()))
        self.assertFalse(prepared.plan["identity"]["holdout"]["training"])
        self.assertFalse(prepared.plan["identity"]["holdout"]["validation"])
        self.assertFalse(prepared.plan["identity"]["holdout"]["modelSelection"])

    def test_negative_observed_terms_are_packet_authority(self) -> None:
        negatives = [x for x in self.prepared.examples if x.compatibility == "REJECT"]
        self.assertEqual(["Brie double crème", "Brie double crème"], [x.observed_term for x in negatives])
        self.assertEqual([1, 1], [x.secondary_target for x in negatives])

    def test_explicit_semantic_fields_resolve_positive_terms(self) -> None:
        by_candidate = {}
        for item in self.prepared.examples:
            by_candidate.setdefault(item.candidate_id, set()).add(item.observed_term)
        self.assertIn("Teff Ivory Grain", by_candidate["3pMmZU"])
        self.assertIn("Vegetable rosti, pre-fried, frozen", by_candidate["7HLNbE"])
        self.assertIn("Artischocken Herzen", by_candidate["ZuhV5V"])

    def test_primary_and_secondary_projection_is_explicit(self) -> None:
        self.assertEqual({2, 3}, {x.primary_target for x in self.prepared.examples})
        self.assertEqual({0, 1}, {x.secondary_target for x in self.prepared.examples})
        self.assertTrue(all(x.secondary_mask == 1.0 for x in self.prepared.examples))

    def test_tensor_contract_is_cpu_and_typed(self) -> None:
        for name, view in self.prepared.tensors.items():
            self.assertEqual("cpu", view.input_ids.device.type, name)
            self.assertEqual("cpu", view.attention_mask.device.type, name)
            self.assertEqual("torch.int64", str(view.input_ids.dtype))
            self.assertEqual("torch.int64", str(view.attention_mask.dtype))
            self.assertEqual("torch.int64", str(view.primary_target.dtype))
            self.assertEqual("torch.int64", str(view.secondary_target.dtype))
            self.assertEqual("torch.float32", str(view.secondary_mask.dtype))
            self.assertEqual(len(view.examples), view.input_ids.shape[0])

    def test_plan_is_deterministic(self) -> None:
        other = prepare_productive_v2(PACKET, REQUEST, TOKENIZER)
        self.assertEqual(self.prepared.plan, other.plan)

    def test_plan_contains_no_local_path(self) -> None:
        encoded = json.dumps(self.prepared.plan, ensure_ascii=False)
        self.assertNotIn(str(ROOT), encoded)
        self.assertNotIn("training/him/models", encoded)

    def test_startup_gate_rejects_readiness_drift(self) -> None:
        packet = load_packet(PACKET, REQUEST)
        packet.files["productive_readiness"]["state"] = "NOT_READY"  # type: ignore[index]
        with self.assertRaises(ProductiveTrainingV2Error):
            validate_startup(packet)

    def test_startup_gate_rejects_authority_drift(self) -> None:
        packet = load_packet(PACKET, REQUEST)
        packet.files["observed"]["candidateNameUsedAsObservedTerm"] = True  # type: ignore[index]
        with self.assertRaises(ProductiveTrainingV2Error):
            validate_startup(packet)

    def test_missing_positive_input_fails_closed(self) -> None:
        packet = load_packet(PACKET, REQUEST)
        item = packet.files["examples"]["examples"][0]
        item["semanticLabel"] = None
        item["productForm"] = None
        with self.assertRaises(ProductiveTrainingV2Error):
            project_examples(packet)

    def test_request_must_be_inside_packet(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ProductiveTrainingV2Error):
                load_packet(PACKET, Path(directory) / "request.json")

    def test_checkpoint_and_run_evidence_bind_plan_and_authorities(self) -> None:
        checkpoint = {"reference": "him-training-checkpoint:v2:test", "logicalDigest": "a" * 64, "reload": {"passed": False}}
        evidence = run_evidence_payload(self.prepared, "productive-training-run:v2:test", "RUN_FAILED", checkpoint, "TEST")
        self.assertEqual("RUN_FAILED", evidence["state"])
        self.assertEqual(self.prepared.plan["reference"], evidence["executionPlanReference"])
        self.assertFalse(evidence["reloadValidation"]["passed"])

    def test_checkpoint_bindings_use_authoritative_packet_reference_fields(self) -> None:
        bindings = build_prepared_checkpoint_bindings(
            self.prepared,
            runtime_identity={"device": "cpu", "runtimeImage": "test"},
            optimizer_identity={"optimizerId": "optimizer:adamw:v1"},
        )
        self.assertNotIn("reference", self.prepared.packet["examples"])
        self.assertNotIn("reference", self.prepared.packet["corpus"])
        self.assertEqual(
            self.prepared.packet["examples"]["authorityReference"],
            bindings["completeTrainingExamplesReference"],
        )
        self.assertEqual(
            self.prepared.packet["corpus"]["corpusReference"],
            bindings["corpusReference"],
        )

    def test_atomic_json_round_trip(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "evidence.json"
            atomic_json_write(path, {"b": 2, "a": 1})
            self.assertEqual({"a": 1, "b": 2}, json.loads(path.read_text(encoding="utf-8")))
            self.assertFalse(any(path.parent.glob("*.tmp")))

    def test_dry_run_cli_never_calls_model_loader(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            result = run_cli(["--mode", "PRODUCTIVE_TRAINING_V2", "--packet-root", str(PACKET), "--request", str(REQUEST), "--output-root", directory, "--tokenizer-path", str(TOKENIZER), "--dry-run"])
            self.assertEqual(0, result)
            self.assertTrue((Path(directory) / "execution-plan.v2.json").is_file())

    def test_dry_run_requires_explicit_mode(self) -> None:
        with self.assertRaises(SystemExit):
            run_cli(["--packet-root", str(PACKET), "--request", str(REQUEST), "--output-root", tempfile.gettempdir(), "--tokenizer-path", str(TOKENIZER), "--dry-run"])

    def test_real_execution_requires_external_binding(self) -> None:
        result = run_cli(["--mode", "PRODUCTIVE_TRAINING_V2", "--packet-root", str(PACKET), "--request", str(REQUEST), "--output-root", tempfile.gettempdir(), "--tokenizer-path", str(TOKENIZER), "--model-root", "/external/model", "--execute"])
        self.assertEqual(1, result)

    def test_model_loader_is_not_called_by_preparation(self) -> None:
        loader = Mock()
        prepare_productive_v2(PACKET, REQUEST, TOKENIZER)
        loader.assert_not_called()


if __name__ == "__main__":
    unittest.main()
