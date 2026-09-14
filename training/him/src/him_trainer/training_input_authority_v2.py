"""Authoritative, model-free P2 V2 training-input binding.

This module consumes only the immutable Corpus V2 and Partition V2 artifacts.
It derives membership from partition records, reconstructs the persisted V2
serialization, and keeps supervision outside the model-facing input.  No
holdout content is loaded and no model/tokenizer is deserialized.
"""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
from pathlib import Path
from typing import Any, Mapping

from .corpus_assembly_v2 import parse_model_input_v2
from .sequence_length_authority_v2 import sequence_length_authority_v2


CORPUS_REFERENCE = "him-p2-corpus:v2:a822a387a4368448190b1743a232b5bedb2ee5617835fad0151235f98060e5f8"
PARTITION_REFERENCE = "him-p2-partition-authority:v2:0a159f9a66a1d3bf283cd41bee3cea4794e526616758a6b8c27db74b39a4e3b8"
LEAKAGE_REFERENCE = "him-p2-leakage-validation:v2:737935048cd73f2d82e321ec293cd16ca9ad6fbe640151aec2672d524f50e682"
BATCH_REFERENCE = "him-p2-batch-authority:v2:d23cf032f23d83d456654d3ee841a951961ce4350e4f3419ca9446312371682e"
RUNTIME_REFERENCE = "him-p2-training-runtime-authority:v2:fec1a68e178f31e8d19a033ad765bcd04e1da2fd389e15eb9dd245c9d5c915d0"
SEQUENCE_REFERENCE = "sequence-length-authority:v2:97130457decd4f283492da2d09a0faddb4bb99fc70f7d79732fbd390794cc509"
INPUT_ID = "HIM_INPUT_REPRESENTATION_V2"
EVIDENCE_ID = "HIM_EVIDENCE_PROJECTION_V2"
MAX_SEQUENCE_LENGTH = 256
TRAIN_SPLIT = "TRAIN"
VALIDATION_SPLIT = "VALIDATION"
HOLDOUT_SPLIT = "HOLDOUT"
DEFAULT_ROOT = Path(__file__).resolve().parents[3]
CORPUS_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v2/corpus-assembly-v2/corpus.v2.json")
PARTITION_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v2/corpus-assembly-v2/partition.v2.json")
LEAKAGE_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v2/corpus-assembly-v2/leakage-validation.v2.json")
COVERAGE_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v2/corpus-assembly-v2/coverage.v2.json")
BATCH_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v2/runtime-authority/batch-authority.v2.json")
RUNTIME_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v2/runtime-authority/training-runtime-authority.v2.json")
AUTHORITY_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v2/runtime-authority/training-input-authority.v2.json")
INVENTORY_RELATIVE = Path("data/knowledge/him/training/p2/canonical-catalog-expansion/v2/runtime-authority/training-input-inventory.v2.json")


class TrainingInputV2Error(ValueError):
    """Raised when a V2 input binding is absent, altered, or unsafe."""


def _canonical(value: object) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")


def logical_digest(value: object) -> str:
    return hashlib.sha256(_canonical(value)).hexdigest()


def _fail(condition: bool, code: str) -> None:
    if not condition:
        raise TrainingInputV2Error(code)


def _load(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except Exception as error:
        raise TrainingInputV2Error(f"AUTHORITY_LOAD_FAILED:{path}") from error
    _fail(isinstance(value, dict), f"AUTHORITY_OBJECT_REQUIRED:{path}")
    return value


def _verify_artifact(path: Path, reference: str, contract_id: str | None = None, *, recompute: bool = True) -> dict[str, Any]:
    value = _load(path)
    expected_digest = reference.rsplit(":", 1)[-1]
    _fail(value.get("reference") == reference, f"REFERENCE_MISMATCH:{path}")
    _fail(value.get("logicalDigest") == expected_digest, f"DIGEST_FIELD_MISMATCH:{path}")
    if recompute:
        core = {key: item for key, item in value.items() if key not in {"reference", "logicalDigest"}}
        _fail(logical_digest(core) == expected_digest, f"DIGEST_RECOMPUTATION_MISMATCH:{path}")
    if contract_id is not None:
        _fail(value.get("contractId") == contract_id, f"CONTRACT_MISMATCH:{path}")
    return value


def _entry_digest(serialized: str) -> str:
    return hashlib.sha256(serialized.encode("utf-8")).hexdigest()


def validate_v2_authority_identity(*, corpus_reference: str, partition_reference: str, sequence_reference: str) -> None:
    """Reject every historical authority family before any input is read."""

    _fail(corpus_reference == CORPUS_REFERENCE, "V1_OR_UNAUTHORIZED_CORPUS")
    _fail(partition_reference == PARTITION_REFERENCE, "V1_OR_UNAUTHORIZED_PARTITION")
    _fail(sequence_reference == SEQUENCE_REFERENCE, "SEQUENCE_256_AUTHORITY_REQUIRED")


def _partition_entries(partition: Mapping[str, Any]) -> tuple[dict[str, Any], ...]:
    members = partition.get("members")
    _fail(isinstance(members, list), "PARTITION_MEMBERS_MISSING")
    result = tuple(dict(item) for item in members)
    _fail(len(result) == 40, "PARTITION_MEMBER_COUNT_INVALID")
    _fail(len({item.get("exampleReference") for item in result}) == 40, "PARTITION_MEMBER_DUPLICATE")
    _fail(all(item.get("partition") in {TRAIN_SPLIT, VALIDATION_SPLIT} for item in result), "HOLDOUT_MEMBER_PRESENT")
    return result


def _inventory_entry(example: Mapping[str, Any], split: str) -> dict[str, Any]:
    representation = example.get("inputRepresentation")
    _fail(isinstance(representation, Mapping), "INPUT_REPRESENTATION_MISSING")
    _fail(representation.get("id") == INPUT_ID and representation.get("version") == "2", "INPUT_REPRESENTATION_AUTHORITY_MISMATCH")
    serialized = representation.get("serialized")
    _fail(isinstance(serialized, str), "INPUT_SERIALIZATION_MISSING")
    parsed = parse_model_input_v2(serialized)
    _fail(parsed.serialize() == serialized, "INPUT_SERIALIZATION_NOT_CANONICAL")
    projection = example.get("evidenceProjection")
    _fail(isinstance(projection, Mapping), "EVIDENCE_PROJECTION_MISSING")
    _fail(projection.get("id") == EVIDENCE_ID and projection.get("version") == "2", "EVIDENCE_PROJECTION_AUTHORITY_MISMATCH")
    projection_reference = example.get("evidenceProjectionReference")
    _fail(projection.get("reference") == projection_reference, "EVIDENCE_PROJECTION_REFERENCE_MISMATCH")
    sequence_length = example.get("sequenceLength")
    _fail(isinstance(sequence_length, int) and not isinstance(sequence_length, bool), "SEQUENCE_LENGTH_INVALID")
    _fail(example.get("sequenceLengthAuthority") == SEQUENCE_REFERENCE, "SEQUENCE_AUTHORITY_MISMATCH")
    sequence_length_authority_v2().validate_token_length(sequence_length)
    primary_mask = example.get("primaryMask")
    secondary_mask = example.get("secondaryMask")
    _fail(primary_mask in {0, 1} and secondary_mask in {0, 1}, "TARGET_MASK_INVALID")
    _fail(not (primary_mask == 0 and secondary_mask == 0), "BOTH_OBJECTIVES_INACTIVE")
    if primary_mask:
        _fail(example.get("primaryTarget") in {"IDENTITY", "VARIANT"}, "PRIMARY_TARGET_INVALID")
    else:
        _fail(example.get("primaryTarget") is None, "INACTIVE_PRIMARY_TARGET_PRESENT")
    if secondary_mask:
        _fail(example.get("secondaryTarget") in {"COMPATIBLE", "REJECT"}, "SECONDARY_TARGET_INVALID")
    else:
        _fail(example.get("secondaryTarget") is None, "INACTIVE_SECONDARY_TARGET_PRESENT")
    return {
        "exampleReference": example["exampleReference"],
        "partition": split,
        "familyReference": example["familyReference"],
        "sourceRecordReference": example["sourceRecordReference"],
        "candidateReference": example["candidateReference"],
        "inputRepresentationReference": f"him-input-representation:v2:{_entry_digest(serialized)}",
        "inputRepresentationDigest": _entry_digest(serialized),
        "evidenceProjectionReference": projection_reference,
        "evidenceProjectionDigest": projection_reference.rsplit(":", 1)[-1],
        "sequenceLength": sequence_length,
        "primaryTarget": example.get("primaryTarget"),
        "primaryActive": primary_mask,
        "secondaryTarget": example.get("secondaryTarget"),
        "secondaryActive": secondary_mask,
    }


def load_v2_input_bundle(root: str | Path = DEFAULT_ROOT) -> dict[str, Any]:
    """Reload every persisted V2 authority and reconstruct exactly 32/8 inputs."""

    repository = Path(root)
    validate_v2_authority_identity(corpus_reference=CORPUS_REFERENCE, partition_reference=PARTITION_REFERENCE, sequence_reference=SEQUENCE_REFERENCE)
    corpus = _verify_artifact(repository / CORPUS_RELATIVE, CORPUS_REFERENCE, "HIM_P2_CORPUS_V2")
    partition = _verify_artifact(repository / PARTITION_RELATIVE, PARTITION_REFERENCE, "HIM_P2_PARTITION_AUTHORITY_V2", recompute=False)
    leakage = _verify_artifact(repository / LEAKAGE_RELATIVE, LEAKAGE_REFERENCE, "HIM_P2_LEAKAGE_VALIDATION_AUTHORITY_V2", recompute=False)
    batch = _verify_artifact(repository / BATCH_RELATIVE, BATCH_REFERENCE, "HIM_P2_BATCH_SIZE_AUTHORITY_V2", recompute=False)
    runtime = _verify_artifact(repository / RUNTIME_RELATIVE, RUNTIME_REFERENCE, "HIM_P2_TRAINING_RUNTIME_AUTHORITY_V2", recompute=False)
    coverage = _load(repository / COVERAGE_RELATIVE)
    _fail(corpus.get("exampleCount") == 40 and len(corpus.get("examples", [])) == 40, "CORPUS_COUNT_INVALID")
    _fail(partition.get("corpusReference") == CORPUS_REFERENCE and partition.get("corpusLogicalDigest") == CORPUS_REFERENCE.rsplit(":", 1)[-1], "PARTITION_CORPUS_BINDING_INVALID")
    _fail(leakage.get("partitionReference") == PARTITION_REFERENCE and leakage.get("status") == "PASS", "LEAKAGE_AUTHORITY_INVALID")
    _fail(batch.get("physicalBatchSize") == 8 and batch.get("gradientAccumulationSteps") == 1 and batch.get("validationBatchSize") == 8, "BATCH_AUTHORITY_INVALID")
    _fail(batch.get("batchSizeAuthorized") is False, "UNEXPECTED_OLD_BATCH_AUTHORIZATION")
    _fail(runtime.get("batchAuthorityReference") == BATCH_REFERENCE and runtime.get("sequenceReference") == SEQUENCE_REFERENCE, "RUNTIME_BINDING_INVALID")
    _fail(coverage.get("train", {}).get("example_count") == 32 and coverage.get("validation", {}).get("example_count") == 8, "COVERAGE_COUNTS_INVALID")
    examples = {item["exampleReference"]: item for item in corpus["examples"]}
    members = _partition_entries(partition)
    _fail(set(examples) == {item["exampleReference"] for item in members}, "CORPUS_PARTITION_MEMBER_SET_MISMATCH")
    by_split: dict[str, list[dict[str, Any]]] = {TRAIN_SPLIT: [], VALIDATION_SPLIT: []}
    for member in members:
        by_split[member["partition"]].append(_inventory_entry(examples[member["exampleReference"]], member["partition"]))
    _fail(len(by_split[TRAIN_SPLIT]) == 32 and len(by_split[VALIDATION_SPLIT]) == 8, "V2_SPLIT_COUNTS_INVALID")
    _fail(not set(item["exampleReference"] for item in by_split[TRAIN_SPLIT]) & set(item["exampleReference"] for item in by_split[VALIDATION_SPLIT]), "TRAIN_VALIDATION_OVERLAP")
    return {"corpus": corpus, "partition": partition, "leakage": leakage, "batch": batch, "runtime": runtime, "coverage": coverage, "train": tuple(by_split[TRAIN_SPLIT]), "validation": tuple(by_split[VALIDATION_SPLIT]), "holdout": ()}


def build_training_input_inventory(bundle: Mapping[str, Any]) -> dict[str, Any]:
    entries = list(bundle["train"]) + list(bundle["validation"])
    def counts(values: list[Mapping[str, Any]]) -> dict[str, int]:
        return {
            "primaryActive": sum(item["primaryActive"] for item in values),
            "secondaryActive": sum(item["secondaryActive"] for item in values),
            "identity": sum(item["primaryTarget"] == "IDENTITY" for item in values),
            "variant": sum(item["primaryTarget"] == "VARIANT" for item in values),
            "compatible": sum(item["secondaryTarget"] == "COMPATIBLE" for item in values),
            "reject": sum(item["secondaryTarget"] == "REJECT" for item in values),
        }
    core = {
        "contractId": "HIM_P2_TRAINING_INPUT_INVENTORY_V2",
        "version": "2",
        "corpusReference": CORPUS_REFERENCE,
        "partitionReference": PARTITION_REFERENCE,
        "sequenceReference": SEQUENCE_REFERENCE,
        "entries": entries,
        "splitCounts": {"TRAIN": len(bundle["train"]), "VALIDATION": len(bundle["validation"]), "HOLDOUT": 0},
        "targetCounts": {"TRAIN": counts(list(bundle["train"])), "VALIDATION": counts(list(bundle["validation"]))},
        "holdoutIncluded": False,
        "ordering": "PARTITION_MEMBER_ORDER",
    }
    digest = logical_digest(core)
    return {**core, "logicalDigest": digest, "reference": f"him-p2-training-input-inventory:v2:{digest}"}


def build_training_input_authority(bundle: Mapping[str, Any], inventory: Mapping[str, Any]) -> dict[str, Any]:
    _fail(inventory.get("reference", "").startswith("him-p2-training-input-inventory:v2:"), "INVENTORY_REFERENCE_INVALID")
    inventory_core = {key: value for key, value in inventory.items() if key not in {"logicalDigest", "reference"}}
    _fail(inventory.get("logicalDigest") == logical_digest(inventory_core), "INVENTORY_DIGEST_MISMATCH")
    _fail(inventory.get("reference") == f"him-p2-training-input-inventory:v2:{inventory['logicalDigest']}", "INVENTORY_REFERENCE_MISMATCH")
    core = {
        "contractId": "HIM_P2_TRAINING_INPUT_AUTHORITY_V2",
        "version": "2",
        "corpusReference": CORPUS_REFERENCE,
        "partitionReference": PARTITION_REFERENCE,
        "leakageReference": LEAKAGE_REFERENCE,
        "inputRepresentation": {"id": INPUT_ID, "version": "2"},
        "evidenceProjection": {"id": EVIDENCE_ID, "version": "2"},
        "sequenceReference": SEQUENCE_REFERENCE,
        "maxSequenceLength": MAX_SEQUENCE_LENGTH,
        "truncationAllowed": False,
        "paddingPolicy": "DYNAMIC_PAD_TO_BATCH_MAX",
        "batchAuthorityReference": BATCH_REFERENCE,
        "inventoryReference": inventory["reference"],
        "inventoryLogicalDigest": inventory["logicalDigest"],
        "trainTotal": len(bundle["train"]),
        "validationTotal": len(bundle["validation"]),
        "holdoutTotal": 0,
        "candidateSemantics": "PERSISTED_CANDIDATE_CONDITIONING_V2",
        "targetSemantics": "PRIMARY_AND_SECONDARY_MASKED_TARGETS_OUTSIDE_MODEL_INPUT",
        "status": "AUTHORIZED",
    }
    digest = logical_digest(core)
    return {**core, "logicalDigest": digest, "reference": f"him-p2-training-input-authority:v2:{digest}"}


def _persist_immutable(path: Path, value: Mapping[str, Any]) -> None:
    data = _canonical(value) + b"\n"
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        _fail(path.read_bytes() == data, f"IMMUTABLE_ARTIFACT_COLLISION:{path}")
        return
    fd, temporary = tempfile.mkstemp(prefix=f".{path.name}.", suffix=".tmp", dir=str(path.parent))
    try:
        with os.fdopen(fd, "wb") as handle:
            handle.write(data); handle.flush(); os.fsync(handle.fileno())
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def persist_training_input_authority(root: str | Path = DEFAULT_ROOT) -> dict[str, Any]:
    repository = Path(root)
    bundle = load_v2_input_bundle(repository)
    inventory = build_training_input_inventory(bundle)
    authority = build_training_input_authority(bundle, inventory)
    _persist_immutable(repository / INVENTORY_RELATIVE, inventory)
    _persist_immutable(repository / AUTHORITY_RELATIVE, authority)
    _fail(_load(repository / INVENTORY_RELATIVE) == inventory, "INVENTORY_RELOAD_MISMATCH")
    _fail(_load(repository / AUTHORITY_RELATIVE) == authority, "AUTHORITY_RELOAD_MISMATCH")
    return {"bundle": bundle, "inventory": inventory, "authority": authority}


def validate_training_input_authority(value: Mapping[str, Any], bundle: Mapping[str, Any], inventory: Mapping[str, Any]) -> None:
    expected = build_training_input_authority(bundle, inventory)
    _fail(dict(value) == expected, "TRAINING_INPUT_AUTHORITY_MISMATCH")


__all__ = [
    "AUTHORITY_RELATIVE", "BATCH_REFERENCE", "CORPUS_REFERENCE", "DEFAULT_ROOT", "EVIDENCE_ID", "INVENTORY_RELATIVE",
    "LEAKAGE_REFERENCE", "MAX_SEQUENCE_LENGTH", "PARTITION_REFERENCE", "RUNTIME_REFERENCE", "SEQUENCE_REFERENCE",
    "TrainingInputV2Error", "build_training_input_authority", "build_training_input_inventory", "load_v2_input_bundle",
    "logical_digest", "persist_training_input_authority", "validate_training_input_authority",
]
