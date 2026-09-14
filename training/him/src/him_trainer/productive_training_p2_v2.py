"""Model-free P2 V2 runner binding and future execution CLI.

The preflight path is executable now; the real training path is intentionally
not invoked by this mission.  Membership always comes from Partition V2.
"""

from __future__ import annotations

import argparse
import json
from typing import Any, Mapping

from .training_input_authority_v2 import (
    BATCH_REFERENCE,
    MAX_SEQUENCE_LENGTH,
    TrainingInputV2Error,
    load_v2_input_bundle,
    persist_training_input_authority,
)


RUNNER_MODULE = "him_trainer.productive_training_p2_v2"
TRAIN_COUNT = 32
VALIDATION_COUNT = 8
PHYSICAL_BATCH_SIZE = 8
GRADIENT_ACCUMULATION_STEPS = 1
EPOCHS = 3
SHUFFLE = False
DROP_LAST = False


def build_partition_bound_batches(entries: tuple[Mapping[str, Any], ...], batch_size: int = PHYSICAL_BATCH_SIZE) -> tuple[tuple[Mapping[str, Any], ...], ...]:
    if batch_size != PHYSICAL_BATCH_SIZE:
        raise TrainingInputV2Error("UNAUTHORIZED_BATCH_CHANGE")
    if not entries or len(entries) % batch_size:
        raise TrainingInputV2Error("BATCH_MEMBERSHIP_INVALID")
    return tuple(tuple(entries[index:index + batch_size]) for index in range(0, len(entries), batch_size))


def build_tensor_shape_contract(entries: tuple[Mapping[str, Any], ...]) -> dict[str, tuple[int, ...]]:
    if not entries:
        raise TrainingInputV2Error("EMPTY_INPUT_SPLIT")
    width = max(int(item["sequenceLength"]) for item in entries)
    if width > MAX_SEQUENCE_LENGTH:
        raise TrainingInputV2Error("INPUT_SEQUENCE_TOO_LONG")
    batch = len(entries)
    return {
        "input_ids": (batch, width),
        "attention_mask": (batch, width),
        "primary_target": (batch,),
        "primary_active": (batch,),
        "secondary_target": (batch,),
        "secondary_active": (batch,),
    }


def build_batch_tensor_shape_contract(entries: tuple[Mapping[str, Any], ...]) -> tuple[dict[str, tuple[int, ...]], ...]:
    """Return dynamic-pad shapes for the physical batches in authority order."""

    return tuple(build_tensor_shape_contract(batch) for batch in build_partition_bound_batches(entries))


def preflight(root: str) -> dict[str, Any]:
    persisted = persist_training_input_authority(root)
    bundle = persisted["bundle"]
    train = tuple(bundle["train"])
    validation = tuple(bundle["validation"])
    train_batches = build_partition_bound_batches(train)
    validation_batches = build_partition_bound_batches(validation)
    return {
        "state": "P2_V2_INPUT_PREFLIGHT_PASS",
        "runnerModule": RUNNER_MODULE,
        "train": len(train),
        "validation": len(validation),
        "holdout": 0,
        "trainBatches": [len(item) for item in train_batches],
        "validationBatches": [len(item) for item in validation_batches],
        "trainShape": build_tensor_shape_contract(train),
        "validationShape": build_tensor_shape_contract(validation),
        "trainBatchShapes": build_batch_tensor_shape_contract(train),
        "validationBatchShapes": build_batch_tensor_shape_contract(validation),
        "sequenceLimit": MAX_SEQUENCE_LENGTH,
        "batchAuthority": BATCH_REFERENCE,
        "trainingInputAuthority": persisted["authority"]["reference"],
        "modelDeserializationCount": 0,
        "trainingCount": 0,
    }


def future_training_command() -> tuple[str, ...]:
    return (
        "python", "-m", RUNNER_MODULE, "--execute",
        "--corpus-v2", "<authoritative corpus.v2.json>",
        "--partition-v2", "<authoritative partition.v2.json>",
        "--batch-authority", "<updated authorized batch authority>",
        "--training-input-authority", "<training-input-authority.v2.json>",
        "--runtime-authority", "<updated training-runtime-authority.v2.json>",
        "--output-root", "<fresh immutable p2-v2 qualification output root>",
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(prog=RUNNER_MODULE)
    parser.add_argument("--root", default=".")
    parser.add_argument("--preflight", action="store_true")
    parser.add_argument("--execute", action="store_true")
    args = parser.parse_args(argv)
    if args.execute:
        parser.error("REAL_P2_V2_TRAINING_NOT_AUTHORIZED_IN_THIS_MISSION")
    if not args.preflight:
        parser.error("--preflight_required")
    print(json.dumps(preflight(args.root), ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())


__all__ = ["RUNNER_MODULE", "build_batch_tensor_shape_contract", "build_partition_bound_batches", "build_tensor_shape_contract", "future_training_command", "preflight"]
