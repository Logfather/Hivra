"""Model-free P2 productive-runner authority for the frozen 26/6/0 partition.

This module is versioned separately from the P1 productive runner.  It validates
the P2 authority, derives the deterministic trajectory, and provides a packet
dry-run without importing a model, CUDA, or RunPod.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping


class P2ProductiveTrainingError(ValueError):
    """Fail-closed P2 authority or partition error."""


def _canonical(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")


def _digest(value: object) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def _require(condition: bool, code: str) -> None:
    if not condition:
        raise P2ProductiveTrainingError(code)


@dataclass(frozen=True)
class P2TrainingConfiguration:
    lineage_reference: str
    corpus_logical_digest: str
    partition_logical_digest: str
    epochs: int = 3
    micro_batch_size: int = 8
    gradient_accumulation_steps: int = 1
    learning_rate: str = "0.0001"
    weight_decay: str = "0.01"
    seed: int = 7
    precision: str = "FP32"
    sequence_length: int = 128
    optimizer_id: str = "optimizer:adamw:v1"
    validation_policy: str = "EACH_EPOCH_AND_FINAL"

    def __post_init__(self) -> None:
        _require(self.epochs > 0 and self.micro_batch_size > 0 and self.gradient_accumulation_steps > 0, "NUMERICAL_CONFIGURATION_INVALID")
        _require(float(self.learning_rate) > 0 and math.isfinite(float(self.learning_rate)), "LEARNING_RATE_INVALID")
        _require(float(self.weight_decay) >= 0 and math.isfinite(float(self.weight_decay)), "WEIGHT_DECAY_INVALID")
        _require(isinstance(self.seed, int) and not isinstance(self.seed, bool), "SEED_INVALID")
        _require(self.precision == "FP32", "PRECISION_UNSUPPORTED")
        _require(self.sequence_length > 0, "SEQUENCE_LENGTH_INVALID")

    @classmethod
    def current(cls) -> "P2TrainingConfiguration":
        return cls(
            lineage_reference="p2-lineage:v1:53a946c4aaefe1a27e5dfdc78dea7aad678e7fd212c2d66d40ec9a72bc9fa110",
            corpus_logical_digest="fd65bcdfcf5811dd497722a80e68bb3bf0b1819e1c771f0ef9e9a01eabbfc8ca",
            partition_logical_digest="5f31d055ad9f05f9a978f25011a6dfc8abc292fad5b9612e328896a617713d9d",
        )

    def identity(self) -> dict[str, Any]:
        return {
            "contractId": "HIM_P2_PRODUCTIVE_TRAINING_CONFIGURATION_V1",
            "version": 1,
            "lineageReference": self.lineage_reference,
            "corpusLogicalDigest": self.corpus_logical_digest,
            "partitionLogicalDigest": self.partition_logical_digest,
            "epochs": self.epochs,
            "microBatchSize": self.micro_batch_size,
            "gradientAccumulationSteps": self.gradient_accumulation_steps,
            "learningRate": self.learning_rate,
            "weightDecay": self.weight_decay,
            "seed": self.seed,
            "precision": self.precision,
            "sequenceLength": self.sequence_length,
            "optimizerId": self.optimizer_id,
            "validationPolicy": self.validation_policy,
            "checkpointContract": "HIM_TRAINING_CHECKPOINT_CONTRACT_V1",
            "checkpointReloadGate": "MODEL_STATE_RELOAD_EQUIVALENCE,OPTIMIZER_STATE_RELOAD_EQUIVALENCE,STEP_RELOAD_EQUIVALENCE",
            "runEvidenceContract": "HIM_P2_PRODUCTIVE_TRAINING_RUN_EVIDENCE_V1",
            "validationEvidenceContract": "HIM_P2_VALIDATION_EVIDENCE_V1",
            "holdoutEvidenceArtifactRequired": "NO",
            "configurationRationale": "Explicit P2 pilot decision: reuse P1 numerical continuity by new P2 authority; FP32 is retained and no AMP mode is introduced.",
        }

    @property
    def logical_digest(self) -> str:
        return _digest(self.identity())

    @property
    def reference(self) -> str:
        return f"p2-training-configuration:v1:{self.logical_digest}"

    @property
    def batches_per_epoch(self) -> int:
        return math.ceil(26 / self.micro_batch_size)

    @property
    def total_train_batches(self) -> int:
        return self.epochs * self.batches_per_epoch

    @property
    def optimizer_steps(self) -> int:
        return math.ceil(self.total_train_batches / self.gradient_accumulation_steps)

    def trajectory(self) -> dict[str, int]:
        return {
            "trainExamplesPerEpoch": 26,
            "batchesPerEpoch": self.batches_per_epoch,
            "optimizerStepsPerEpoch": math.ceil(self.batches_per_epoch / self.gradient_accumulation_steps),
            "epochs": self.epochs,
            "totalTrainBatches": self.total_train_batches,
            "totalOptimizerSteps": self.optimizer_steps,
        }


@dataclass(frozen=True)
class P2DryRunResult:
    counts: Mapping[str, int]
    trajectory: Mapping[str, int]
    validation_batch_count: int
    expected_backward_count: int
    expected_validation_forward_count: int
    expected_holdout_forward_count: int

    def to_dict(self) -> dict[str, Any]:
        return {"counts": dict(self.counts), "trajectory": dict(self.trajectory), "validationBatchCount": self.validation_batch_count, "expectedBackwardCount": self.expected_backward_count, "expectedValidationForwardCount": self.expected_validation_forward_count, "expectedHoldoutForwardCount": self.expected_holdout_forward_count, "modelDeserializationCount": 0}


def _assignment_counts(corpus: Mapping[str, Any], partition: Mapping[str, Any]) -> dict[str, Any]:
    examples = corpus.get("examples")
    assignments = partition.get("exampleAssignments")
    _require(isinstance(examples, list) and len(examples) == 32, "TOTAL_COUNT_INVALID")
    _require(isinstance(assignments, list) and len(assignments) == 32, "ASSIGNMENT_COUNT_INVALID")
    by_reference = {item.get("exampleReference"): item for item in examples}
    _require(len(by_reference) == 32 and all(reference for reference in by_reference), "EXAMPLE_REFERENCE_INVALID")
    seen: set[str] = set()
    split: dict[str, list[dict[str, Any]]] = {"TRAIN": [], "VALIDATION": [], "HOLDOUT": []}
    families: dict[str, set[str]] = {name: set() for name in split}
    for assignment in assignments:
        reference = assignment.get("exampleReference")
        bucket = assignment.get("partition")
        _require(reference in by_reference and reference not in seen and bucket in split, "PARTITION_ASSIGNMENT_INVALID")
        seen.add(reference)
        example = by_reference[reference]
        family = assignment.get("familyGroupReference")
        _require(isinstance(family, str) and family == example.get("family", {}).get("familyGroupReference"), "FAMILY_BINDING_INVALID")
        families[bucket].add(family)
        split[bucket].append(assignment)
    _require(len(seen) == 32 and not (families["TRAIN"] & families["VALIDATION"]) and not (families["TRAIN"] & families["HOLDOUT"]) and not (families["VALIDATION"] & families["HOLDOUT"]), "FAMILY_LEAKAGE")
    def counts(items: list[dict[str, Any]]) -> dict[str, int]:
        return {
            "total": len(items),
            "primaryActive": sum(item.get("primaryMask") == 1 for item in items),
            "secondaryActive": sum(item.get("secondaryMask") == 1 for item in items),
            "secondaryOnly": sum(item.get("role") == "SECONDARY_ONLY" for item in items),
            "compatible": sum(item.get("candidateCompatibility") == "COMPATIBLE" for item in items),
            "reject": sum(item.get("candidateCompatibility") == "REJECT" for item in items),
            "identity": sum(item.get("targetKind") == "IDENTITY" for item in items),
            "variant": sum(item.get("targetKind") == "VARIANT" for item in items),
            "families": len({item["familyGroupReference"] for item in items}),
        }
    result = {name: counts(items) for name, items in split.items()}
    for item in assignments:
        if item.get("role") == "SECONDARY_ONLY":
            _require(item.get("candidateCompatibility") == "REJECT" and item.get("primaryMask") == 0 and item.get("secondaryMask") == 1 and item.get("targetKind") is None, "SECONDARY_ONLY_SEMANTICS_INVALID")
        else:
            _require(item.get("role") == "PRIMARY_AND_SECONDARY" and item.get("primaryMask") == 1 and item.get("secondaryMask") == 1, "PRIMARY_SECONDARY_SEMANTICS_INVALID")
    return {"splits": result, "totalFamilies": len(set().union(*families.values()))}


def validate_p2_inputs(corpus: Mapping[str, Any], partition: Mapping[str, Any]) -> dict[str, Any]:
    result = _assignment_counts(corpus, partition)
    train, validation, holdout = (result["splits"][name] for name in ("TRAIN", "VALIDATION", "HOLDOUT"))
    _require(result["totalFamilies"] == 12, "FAMILY_COUNT_INVALID")
    _require(train == {"total": 26, "primaryActive": 17, "secondaryActive": 26, "secondaryOnly": 9, "compatible": 17, "reject": 9, "identity": 3, "variant": 14, "families": 9}, "TRAIN_AUTHORITY_INVALID")
    _require(validation == {"total": 6, "primaryActive": 6, "secondaryActive": 6, "secondaryOnly": 0, "compatible": 6, "reject": 0, "identity": 2, "variant": 4, "families": 3}, "VALIDATION_AUTHORITY_INVALID")
    _require(holdout == {"total": 0, "primaryActive": 0, "secondaryActive": 0, "secondaryOnly": 0, "compatible": 0, "reject": 0, "identity": 0, "variant": 0, "families": 0}, "HOLDOUT_AUTHORITY_INVALID")
    return result


def build_p2_dry_run(corpus: Mapping[str, Any], partition: Mapping[str, Any], configuration: P2TrainingConfiguration | None = None) -> P2DryRunResult:
    config = configuration or P2TrainingConfiguration.current()
    split = validate_p2_inputs(corpus, partition)["splits"]
    return P2DryRunResult(split["TRAIN"], config.trajectory(), 1, config.optimizer_steps, 1, 0)


def run_cli(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--corpus", required=True)
    parser.add_argument("--partition", required=True)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)
    _require(args.dry_run, "P2_REAL_EXECUTION_NOT_AVAILABLE_IN_AUTHORITY_CLOSURE")
    corpus = json.loads(Path(args.corpus).read_text(encoding="utf-8"))
    partition = json.loads(Path(args.partition).read_text(encoding="utf-8"))
    print(json.dumps(build_p2_dry_run(corpus, partition).to_dict(), sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(run_cli())
