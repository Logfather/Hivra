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
from dataclasses import dataclass
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


@dataclass(frozen=True)
class TrainingInputArtifactSet:
    """Concrete file bindings for one immutable P2 V2 training-input authority."""

    corpus: Path
    partition: Path
    leakage: Path
    coverage: Path
    batch: Path
    runtime: Path | None
    authority: Path | None
    inventory: Path | None
    corpus_reference: str | None = None
    partition_reference: str | None = None
    leakage_reference: str | None = None
    batch_reference: str | None = None
    runtime_reference: str | None = None


def _default_artifact_set(repository: Path) -> TrainingInputArtifactSet:
    return TrainingInputArtifactSet(
        corpus=repository / CORPUS_RELATIVE,
        partition=repository / PARTITION_RELATIVE,
        leakage=repository / LEAKAGE_RELATIVE,
        coverage=repository / COVERAGE_RELATIVE,
        batch=repository / BATCH_RELATIVE,
        runtime=repository / RUNTIME_RELATIVE,
        authority=repository / AUTHORITY_RELATIVE,
        inventory=repository / INVENTORY_RELATIVE,
        corpus_reference=CORPUS_REFERENCE,
        partition_reference=PARTITION_REFERENCE,
        leakage_reference=LEAKAGE_REFERENCE,
        batch_reference=BATCH_REFERENCE,
        runtime_reference=RUNTIME_REFERENCE,
    )


def artifact_set_from_directory(directory: str | Path) -> TrainingInputArtifactSet:
    """Bind a versioned assembly directory without changing semantic contents."""

    base = Path(directory)
    if not (base / "corpus.v2.json").is_file() and (base / "final-training-corpus.v2.json").is_file():
        return TrainingInputArtifactSet(
            corpus=base / "final-training-corpus.v2.json",
            partition=base / "final-training-partition.v2.json",
            leakage=base / "final-training-leakage-validation.v2.json",
            coverage=base / "final-training-coverage.v2.json",
            batch=base / "final-training-batch-authority.v2.json",
            runtime=None,
            authority=base / "final-training-input-authority.v2.json",
            inventory=base / "final-training-input-inventory.v2.json",
        )
    return TrainingInputArtifactSet(
        corpus=base / "corpus.v2.json",
        partition=base / "partition.v2.json",
        leakage=base / "leakage-validation.v2.json",
        coverage=base / "coverage.v2.json",
        batch=base / "batch-authority.v2.json",
        runtime=None,
        authority=base / "training-input-authority.v2.json",
        inventory=base / "training-input-inventory.v2.json",
    )


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


def _verify_self_describing_artifact(path: Path, contract_id: str | None = None, *, recompute: bool = True) -> dict[str, Any]:
    value = _load(path)
    reference = value.get("reference")
    digest = value.get("logicalDigest")
    _fail(isinstance(reference, str) and isinstance(digest, str), f"REFERENCE_OR_DIGEST_MISSING:{path}")
    _fail(reference.endswith(f":{digest}"), f"REFERENCE_MISMATCH:{path}")
    if recompute:
        core = {key: item for key, item in value.items() if key not in {"reference", "logicalDigest"}}
        _fail(logical_digest(core) == digest, f"DIGEST_RECOMPUTATION_MISMATCH:{path}")
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


def _partition_entries(partition: Mapping[str, Any], expected_count: int) -> tuple[dict[str, Any], ...]:
    members = partition.get("members")
    _fail(isinstance(members, list), "PARTITION_MEMBERS_MISSING")
    result = tuple(dict(item) for item in members)
    _fail(len(result) == expected_count, "PARTITION_MEMBER_COUNT_INVALID")
    _fail(len({item.get("exampleReference") for item in result}) == expected_count, "PARTITION_MEMBER_DUPLICATE")
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


def _load_artifact_set(repository: Path, artifact_set: TrainingInputArtifactSet) -> dict[str, Any]:
    if artifact_set.corpus_reference is None:
        corpus = _verify_self_describing_artifact(artifact_set.corpus, "HIM_P2_CORPUS_V2")
        partition = _verify_self_describing_artifact(artifact_set.partition, "HIM_P2_PARTITION_AUTHORITY_V2")
        leakage = _verify_self_describing_artifact(artifact_set.leakage, recompute=True)
        batch = _verify_self_describing_artifact(artifact_set.batch, "HIM_P2_BATCH_SIZE_AUTHORITY_V2")
    else:
        validate_v2_authority_identity(corpus_reference=artifact_set.corpus_reference, partition_reference=artifact_set.partition_reference or "", sequence_reference=SEQUENCE_REFERENCE)
        corpus = _verify_artifact(artifact_set.corpus, artifact_set.corpus_reference, "HIM_P2_CORPUS_V2")
        partition = _verify_artifact(artifact_set.partition, artifact_set.partition_reference or "", "HIM_P2_PARTITION_AUTHORITY_V2", recompute=False)
        leakage = _verify_artifact(artifact_set.leakage, artifact_set.leakage_reference or "", "HIM_P2_LEAKAGE_VALIDATION_AUTHORITY_V2", recompute=False)
        batch = _verify_artifact(artifact_set.batch, artifact_set.batch_reference or "", "HIM_P2_BATCH_SIZE_AUTHORITY_V2", recompute=False)
    runtime = _verify_artifact(artifact_set.runtime, artifact_set.runtime_reference or "", "HIM_P2_TRAINING_RUNTIME_AUTHORITY_V2", recompute=False) if artifact_set.runtime is not None else None
    coverage = _load(artifact_set.coverage)
    inventory = _verify_self_describing_artifact(artifact_set.inventory, "HIM_P2_TRAINING_INPUT_INVENTORY_V2") if artifact_set.inventory is not None and artifact_set.inventory.is_file() else None
    authority = _verify_self_describing_artifact(artifact_set.authority, "HIM_P2_TRAINING_INPUT_AUTHORITY_V2") if artifact_set.authority is not None and artifact_set.authority.is_file() else None
    return {"corpus": corpus, "partition": partition, "leakage": leakage, "batch": batch, "runtime": runtime, "coverage": coverage, "inventory": inventory, "authority": authority}


def _coverage_count(coverage: Mapping[str, Any], split: str) -> int | None:
    section = coverage.get(split.lower())
    if isinstance(section, Mapping):
        value = section.get("example_count", section.get("exampleCount"))
        if isinstance(value, int) and not isinstance(value, bool):
            return value
    return None


def load_v2_input_bundle(root: str | Path = DEFAULT_ROOT, *, artifact_directory: str | Path | None = None) -> dict[str, Any]:
    """Reload persisted V2 authority bindings and reconstruct partitioned inputs."""

    repository = Path(root)
    artifacts = _load_artifact_set(repository, artifact_set_from_directory(artifact_directory) if artifact_directory is not None else _default_artifact_set(repository))
    corpus = artifacts["corpus"]
    partition = artifacts["partition"]
    leakage = artifacts["leakage"]
    batch = artifacts["batch"]
    runtime = artifacts["runtime"]
    coverage = artifacts["coverage"]
    expected_total = corpus.get("exampleCount")
    _fail(isinstance(expected_total, int) and not isinstance(expected_total, bool), "CORPUS_COUNT_INVALID")
    _fail(len(corpus.get("examples", [])) == expected_total, "CORPUS_COUNT_INVALID")
    _fail(partition.get("corpusReference") == corpus.get("reference") and partition.get("corpusLogicalDigest") == corpus.get("logicalDigest"), "PARTITION_CORPUS_BINDING_INVALID")
    _fail(leakage.get("partitionReference") == partition.get("reference") and leakage.get("status") == "PASS", "LEAKAGE_AUTHORITY_INVALID")
    _fail(batch.get("physicalBatchSize") == 8 and batch.get("gradientAccumulationSteps") == 1 and batch.get("validationBatchSize") == 8, "BATCH_AUTHORITY_INVALID")
    if runtime is not None:
        _fail(runtime.get("batchAuthorityReference") == batch.get("reference") and runtime.get("sequenceReference") == SEQUENCE_REFERENCE, "RUNTIME_BINDING_INVALID")
    examples = {item["exampleReference"]: item for item in corpus["examples"]}
    members = _partition_entries(partition, expected_total)
    _fail(set(examples) == {item["exampleReference"] for item in members}, "CORPUS_PARTITION_MEMBER_SET_MISMATCH")
    by_split: dict[str, list[dict[str, Any]]] = {TRAIN_SPLIT: [], VALIDATION_SPLIT: []}
    for member in members:
        by_split[member["partition"]].append(_inventory_entry(examples[member["exampleReference"]], member["partition"]))
    train_count = len(by_split[TRAIN_SPLIT])
    validation_count = len(by_split[VALIDATION_SPLIT])
    _fail(train_count > 0 and validation_count > 0 and train_count + validation_count == expected_total, "V2_SPLIT_COUNTS_INVALID")
    _fail(_coverage_count(coverage, TRAIN_SPLIT) == train_count and _coverage_count(coverage, VALIDATION_SPLIT) == validation_count, "COVERAGE_COUNTS_INVALID")
    if batch.get("trainExampleCount") is not None:
        _fail(batch.get("trainExampleCount") == train_count and batch.get("validationExampleCount") == validation_count, "BATCH_COUNT_BINDING_INVALID")
    if artifacts["inventory"] is not None:
        inventory = artifacts["inventory"]
        _fail(inventory.get("corpusReference") == corpus.get("reference") and inventory.get("partitionReference") == partition.get("reference"), "INVENTORY_BINDING_INVALID")
        _fail(inventory.get("splitCounts") == {"TRAIN": train_count, "VALIDATION": validation_count, "HOLDOUT": 0}, "INVENTORY_SPLIT_COUNT_INVALID")
    if artifacts["authority"] is not None:
        authority = artifacts["authority"]
        _fail(authority.get("corpusReference") == corpus.get("reference") and authority.get("partitionReference") == partition.get("reference"), "TRAINING_INPUT_AUTHORITY_BINDING_INVALID")
        if artifacts["inventory"] is not None:
            _fail(authority.get("inventoryReference") == artifacts["inventory"].get("reference"), "TRAINING_INPUT_AUTHORITY_INVENTORY_MISMATCH")
    _fail(not set(item["exampleReference"] for item in by_split[TRAIN_SPLIT]) & set(item["exampleReference"] for item in by_split[VALIDATION_SPLIT]), "TRAIN_VALIDATION_OVERLAP")
    return {"corpus": corpus, "partition": partition, "leakage": leakage, "batch": batch, "runtime": runtime, "coverage": coverage, "inventory": artifacts["inventory"], "authority": artifacts["authority"], "train": tuple(by_split[TRAIN_SPLIT]), "validation": tuple(by_split[VALIDATION_SPLIT]), "holdout": ()}


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
    "TrainingInputV2Error", "artifact_set_from_directory", "build_training_input_authority", "build_training_input_inventory", "load_v2_input_bundle",
    "logical_digest", "persist_training_input_authority", "validate_training_input_authority",
]
