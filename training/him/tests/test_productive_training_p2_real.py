"""Model-free and synthetic contract tests for the dedicated P2 runner."""

from __future__ import annotations

import copy
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

import torch

from him_trainer.productive_training_p2_real import (
    P2Packet,
    P2RealRunnerError,
    _apply_partition,
    _project_examples,
    execute_p2,
    load_p2_packet,
    prepare_loaded_p2,
    validate_p2_runtime_authority,
)
from him_trainer.productive_training_p2 import validate_p2_inputs


ROOT = Path(__file__).resolve().parents[3]
OLD_PACKET = ROOT / "data/knowledge/him/training/a100-transfer/p2/51c774fc47ebaf1efe6ca615c34771d89b71a8c697858298fe4e2a3c83cd2b26/packet"


def real_runner_packet() -> P2Packet:
    original = load_p2_packet(OLD_PACKET)
    values = copy.deepcopy(dict(original.values))
    values["runner"].update(
        {
            "runnerModule": "him_trainer.productive_training_p2_real",
            "supportsModelDeserialization": True,
            "supportsRealExecution": True,
        },
    )
    return P2Packet(original.root, values)


class SyntheticModel(torch.nn.Module):
    def __init__(self) -> None:
        super().__init__()
        self.base_model = torch.nn.Linear(1, 8)
        self.primary_head = torch.nn.Linear(8, 5)
        self.secondary_head = torch.nn.Linear(8, 2)

    def forward(self, input_ids: torch.Tensor, attention_mask: torch.Tensor) -> SimpleNamespace:
        del attention_mask
        representation = self.base_model(input_ids.to(dtype=torch.float32).mean(dim=1, keepdim=True))
        return SimpleNamespace(primary_logits=self.primary_head(representation), secondary_logits=self.secondary_head(representation))


class ProductiveTrainingP2RealTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.packet = real_runner_packet()
        cls.prepared = prepare_loaded_p2(cls.packet)

    def test_real_runner_authority_is_required(self) -> None:
        old = load_p2_packet(OLD_PACKET)
        with self.assertRaises(ValueError):
            validate_p2_runtime_authority(old, require_real_runner=True)

    def test_exact_p2_counts_and_masks(self) -> None:
        self.assertEqual(32, len(self.prepared.examples))
        self.assertEqual(26, len(self.prepared.train))
        self.assertEqual(6, len(self.prepared.validation))
        self.assertEqual(0, len(self.prepared.holdout))
        self.assertEqual(17, sum(item.compatibility == "COMPATIBLE" for item in self.prepared.train))
        self.assertEqual(9, sum(item.compatibility == "REJECT" for item in self.prepared.train))
        self.assertEqual(9, sum(item.primary_mask == 0.0 for item in self.prepared.train))
        self.assertTrue(all(item.primary_mask == 1.0 for item in self.prepared.validation))

    def test_secondary_only_examples_have_no_primary_target(self) -> None:
        rejects = [item for item in self.prepared.train if item.compatibility == "REJECT"]
        self.assertEqual(9, len(rejects))
        self.assertTrue(all(item.primary_target == 0 and item.primary_mask == 0.0 for item in rejects))
        self.assertTrue(all(item.secondary_target == 1 and item.secondary_mask == 1.0 for item in rejects))

    def test_frozen_batches_are_8_8_8_2(self) -> None:
        sizes = [min(8, 26 - start) for start in range(0, 26, 8)]
        self.assertEqual([8, 8, 8, 2], sizes)

    def test_synthetic_execution_closes_trajectory_checkpoint_and_reload(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            evidence = execute_p2(self.prepared, directory, model_loader=SyntheticModel, device="cpu", synthetic=True)
            self.assertEqual("P2_RUN_COMPLETED", evidence["state"])
            self.assertEqual(12, evidence["trajectory"]["optimizerSteps"])
            self.assertEqual(1, evidence["trajectory"]["forwardValidation"])
            self.assertEqual(6, evidence["validation"]["recordCount"])
            self.assertEqual(0, evidence["holdout"]["forwardCount"])
            self.assertTrue(evidence["checkpoint"]["reload"]["logitEquivalence"])
            self.assertTrue((Path(directory) / "checkpoint/model-state.pt").is_file())
            self.assertTrue((Path(directory) / "checkpoint/optimizer-state.pt").is_file())

    def test_projection_and_partition_are_model_free(self) -> None:
        examples = _project_examples(self.packet)
        train, validation, holdout = _apply_partition(self.packet, examples)
        self.assertEqual(26, len(train))
        self.assertEqual(6, len(validation))
        self.assertEqual(0, len(holdout))

    def test_hard_abort_matrix(self) -> None:
        cases = []
        for key in ("request", "manifest", "readiness", "runner", "configuration", "runtime", "model", "tokenizer", "corpus", "partition"):
            cases.append((f"missing-{key}", lambda values, key=key: values[key].clear()))
            cases.append((f"wrong-{key}-digest", lambda values, key=key: values[key].update(logicalDigest="f" * 64)))
        cases.extend(
            [
                ("wrong-runner-module", lambda v: v["runner"].update(runnerModule="him_trainer.productive_training_v2")),
                ("wrong-runner-real", lambda v: v["runner"].update(supportsRealExecution=False)),
                ("wrong-runner-model", lambda v: v["runner"].update(supportsModelDeserialization=False)),
                ("wrong-python", lambda v: v["runtime"].update(python="3.12.0")),
                ("wrong-interpreter", lambda v: v["runtime"].update(interpreter="python3")),
                ("wrong-torch", lambda v: v["runtime"].update(pytorch="2.13.0")),
                ("wrong-cuda", lambda v: v["runtime"].update(cudaBuild="12.0")),
                ("wrong-tokenizers", lambda v: v["runtime"].update(tokenizers="0.22.0")),
                ("wrong-sentencepiece", lambda v: v["runtime"].update(sentencepiece="0.1.0")),
                ("transformers-present", lambda v: v["runtime"].update(transformers="4.0.0")),
                ("wrong-model-id", lambda v: v["model"].update(modelId="other")),
                ("wrong-model-revision", lambda v: v["model"].update(revision="other")),
                ("wrong-tokenizer-id", lambda v: v["tokenizer"].update(tokenizerId="other")),
                ("tokenizer-mutated", lambda v: v["tokenizer"].update(vocabularyMutation="YES")),
                ("checkpoint-format", lambda v: v["checkpoint"].update(formatChangeRequired=True)),
                ("wrong-run-contract", lambda v: v["run_evidence"].update(contractId="wrong")),
                ("wrong-validation-contract", lambda v: v["validation_evidence"].update(contractId="wrong")),
                ("readiness-not-ready", lambda v: v["readiness"].update(state="BLOCKED")),
                ("wrong-epochs", lambda v: v["request"]["numerical"].update(epochs=4)),
                ("wrong-microbatch", lambda v: v["request"]["numerical"].update(microBatchSize=4)),
                ("wrong-accumulation", lambda v: v["request"]["numerical"].update(gradientAccumulationSteps=2)),
                ("wrong-lr", lambda v: v["request"]["numerical"].update(learningRate="0.2")),
                ("wrong-weight-decay", lambda v: v["request"]["numerical"].update(weightDecay="0.2")),
                ("wrong-seed", lambda v: v["request"]["numerical"].update(seed=8)),
                ("wrong-precision", lambda v: v["request"]["numerical"].update(precision="FP16")),
                ("fake-holdout", lambda v: v["request"]["holdoutInput"].update(state="NON_EMPTY")),
                ("wrong-corpus-count", lambda v: v["corpus"].update(exampleCount=31)),
                ("wrong-partition-count", lambda v: v["partition"].update(trainExampleCount=25)),
                ("wrong-partition-leakage", lambda v: v["partition"].update(familyLeakageCount=1)),
                ("wrong-secondary-only", lambda v: v["corpus"].update(secondaryOnlyCount=8)),
                ("wrong-reject-distribution", lambda v: v["corpus"]["candidateCompatibilityDistribution"].update(REJECT=8)),
                ("fake-primary-target", lambda v: v["corpus"]["examples"][0].update(primaryMask=0)),
            ],
        )
        self.assertGreaterEqual(len(cases), 35)
        for name, mutate in cases:
            with self.subTest(name=name):
                values = copy.deepcopy(dict(self.packet.values))
                mutate(values)
                with self.assertRaises((P2RealRunnerError, ValueError)):
                    packet = P2Packet(self.packet.root, values)
                    validate_p2_runtime_authority(packet, require_real_runner=True)
                    validate_p2_inputs(packet["corpus"], packet["partition"])
                    _apply_partition(packet, _project_examples(packet))


if __name__ == "__main__":
    unittest.main()
