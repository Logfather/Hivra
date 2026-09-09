"""Authoritative Point-12 tokenizer and CPU tensor builder.

This module starts only after the strict Point-12 wire decoder.  It owns
tokenizer execution and numerical tensor construction, but never loads a
model or performs a forward, loss, optimizer, or training operation.
"""

from __future__ import annotations

import hashlib
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import torch
from tokenizers import Encoding, Tokenizer

from .point12_protocol_v1 import (
    Point12Batch,
    Point12ProtocolError,
    _objective_weighting_authority_digest,
    _sequence_digest,
)


MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
MODEL_FAMILY_ID = "XLM-R"
BASE_MODEL_ID = "FacebookAI/xlm-roberta-base"
TOKENIZER_ID = "xlm-roberta-base-tokenizer"
TOKENIZER_REVISION = MODEL_REVISION
TOKENIZER_FILENAME = "tokenizer.json"
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"
DEFAULT_TOKENIZER_PATH = (
    Path(__file__).resolve().parents[2]
    / "models"
    / "xlm-roberta-base"
    / MODEL_REVISION
    / TOKENIZER_FILENAME
)

MAX_SEQUENCE_LENGTH = 128
PAD_TOKEN_ID = 1
REAL_TOKEN_MASK = 1
PAD_MASK = 0
SPECIAL_TOKEN_COUNT = 4


class Point12TensorBuildError(Point12ProtocolError):
    """Raised when an authorized Point-12 payload cannot build safely."""


@dataclass(frozen=True)
class Point12TensorBatchV1:
    """The six and only six model/training tensors plus non-model audit data."""

    input_ids: torch.Tensor
    attention_mask: torch.Tensor
    primary_target: torch.Tensor
    secondary_target: torch.Tensor
    primary_mask: torch.Tensor
    secondary_mask: torch.Tensor
    split: str
    partition_id: str
    partition_digest: str
    batch_plan_id: str
    batch_plan_digest: str
    member_references: tuple[str, ...]
    family_group_references: tuple[str, ...]
    raw_sequence_lengths: tuple[int, ...]
    target_length: int

    def __post_init__(self) -> None:
        tensors = (
            self.input_ids,
            self.attention_mask,
            self.primary_target,
            self.secondary_target,
            self.primary_mask,
            self.secondary_mask,
        )
        _fail_if(not all(isinstance(value, torch.Tensor) for value in tensors), "TENSOR_TYPE_INVALID")
        _fail_if(any(value.device.type != "cpu" for value in tensors), "TENSOR_DEVICE_INVALID")
        _fail_if(any(not value.is_contiguous() for value in tensors), "TENSOR_CONTIGUITY_INVALID")
        batch_size = len(self.member_references)
        _fail_if(self.input_ids.dtype != torch.int64, "INPUT_IDS_DTYPE_INVALID")
        _fail_if(self.attention_mask.dtype != torch.int64, "ATTENTION_MASK_DTYPE_INVALID")
        _fail_if(self.primary_target.dtype != torch.int64, "PRIMARY_TARGET_DTYPE_INVALID")
        _fail_if(self.secondary_target.dtype != torch.int64, "SECONDARY_TARGET_DTYPE_INVALID")
        _fail_if(self.primary_mask.dtype != torch.float32, "PRIMARY_MASK_DTYPE_INVALID")
        _fail_if(self.secondary_mask.dtype != torch.float32, "SECONDARY_MASK_DTYPE_INVALID")
        _fail_if(tuple(self.input_ids.shape) != (batch_size, self.target_length), "INPUT_IDS_SHAPE_INVALID")
        _fail_if(tuple(self.attention_mask.shape) != (batch_size, self.target_length), "ATTENTION_MASK_SHAPE_INVALID")
        for tensor, name in (
            (self.primary_target, "PRIMARY_TARGET"),
            (self.secondary_target, "SECONDARY_TARGET"),
            (self.primary_mask, "PRIMARY_MASK"),
            (self.secondary_mask, "SECONDARY_MASK"),
        ):
            _fail_if(tuple(tensor.shape) != (batch_size,), f"{name}_SHAPE_INVALID")


def load_pinned_xlm_r_tokenizer_v1(path: str | Path = DEFAULT_TOKENIZER_PATH) -> Tokenizer:
    """Load only the digest-pinned local tokenizer artifact."""

    tokenizer_path = Path(path)
    _fail_if(not tokenizer_path.is_file() or tokenizer_path.is_symlink(), "TOKENIZER_ARTIFACT_INVALID")
    _fail_if(tokenizer_path.name != TOKENIZER_FILENAME, "TOKENIZER_FILENAME_INVALID")
    actual_digest = hashlib.sha256(tokenizer_path.read_bytes()).hexdigest()
    _equal_or_fail(actual_digest, TOKENIZER_SHA256, "TOKENIZER_DIGEST_MISMATCH")
    try:
        tokenizer = Tokenizer.from_file(str(tokenizer_path))
        tokenizer.no_truncation()
    except Exception as error:  # tokenizers exposes several exception types
        raise Point12TensorBuildError("TOKENIZER_LOAD_FAILED") from error
    _equal_or_fail(tokenizer.token_to_id("<pad>"), PAD_TOKEN_ID, "PAD_TOKEN_ID_MISMATCH")
    _equal_or_fail(tokenizer.token_to_id("<s>"), 0, "START_TOKEN_ID_MISMATCH")
    _equal_or_fail(tokenizer.token_to_id("</s>"), 2, "END_TOKEN_ID_MISMATCH")
    return tokenizer


def build_point12_numerical_tensors_v1(
    batch: Point12Batch,
    tokenizer: Tokenizer | None = None,
    tokenizer_path: str | Path = DEFAULT_TOKENIZER_PATH,
) -> Point12TensorBatchV1:
    """Build one validated Point-12 batch on CPU from decoded semantic input."""

    _validate_batch_authority(batch)
    active_tokenizer = tokenizer if tokenizer is not None else load_pinned_xlm_r_tokenizer_v1(tokenizer_path)
    if tokenizer is not None:
        _validate_tokenizer_instance(active_tokenizer)
    _validate_runtime_binding(batch, tokenizer_path if tokenizer is None else None)
    _fail_if(batch.member_count != len(batch.members) or batch.member_count == 0, "EMPTY_EXECUTABLE_BATCH")
    raw_lengths: list[int] = []
    raw_ids: list[list[int]] = []
    for member in batch.members:
        encoding = _encode_authoritative_sequence(active_tokenizer, member.sequence)
        ids = [int(value) for value in encoding.ids]
        _fail_if(not ids, "EMPTY_TOKEN_INPUT")
        _fail_if(len(ids) > MAX_SEQUENCE_LENGTH, "HIM_V1_MAX_SEQUENCE_LENGTH_EXCEEDED")
        _fail_if(PAD_TOKEN_ID in ids, "UNEXPECTED_PADDING_BEFORE_COLLATION")
        _fail_if(any(value < 0 for value in ids), "NEGATIVE_INPUT_ID")
        raw_lengths.append(len(ids))
        raw_ids.append(ids)

    target_length = max(raw_lengths)
    _fail_if(target_length != batch.target_length, "BATCH_TARGET_LENGTH_MISMATCH")
    _fail_if(target_length > MAX_SEQUENCE_LENGTH, "HIM_V1_MAX_SEQUENCE_LENGTH_EXCEEDED")
    padded_ids = [ids + [PAD_TOKEN_ID] * (target_length - len(ids)) for ids in raw_ids]
    attention = [[REAL_TOKEN_MASK] * length + [PAD_MASK] * (target_length - length) for length in raw_lengths]
    _validate_padding(padded_ids, attention, raw_lengths)

    result = Point12TensorBatchV1(
        input_ids=torch.tensor(padded_ids, dtype=torch.int64, device="cpu"),
        attention_mask=torch.tensor(attention, dtype=torch.int64, device="cpu"),
        primary_target=torch.tensor([member.primary_target for member in batch.members], dtype=torch.int64, device="cpu"),
        secondary_target=torch.tensor([member.secondary_target for member in batch.members], dtype=torch.int64, device="cpu"),
        primary_mask=torch.tensor([member.primary_mask for member in batch.members], dtype=torch.float32, device="cpu"),
        secondary_mask=torch.tensor([member.secondary_mask for member in batch.members], dtype=torch.float32, device="cpu"),
        split=batch.split,
        partition_id=batch.partition_id,
        partition_digest=batch.partition_digest,
        batch_plan_id=batch.batch_plan_id,
        batch_plan_digest=batch.batch_plan_digest,
        member_references=tuple(member.example_reference for member in batch.members),
        family_group_references=tuple(member.family_group_reference for member in batch.members),
        raw_sequence_lengths=tuple(raw_lengths),
        target_length=target_length,
    )
    _validate_tensor_contract(result)
    return result


def _validate_batch_authority(batch: Point12Batch) -> None:
    _fail_if(batch.split not in {"TRAIN", "HOLDOUT"}, "SPLIT_INVALID")
    policy = batch.policy
    _equal_or_fail(policy.sequence_contract_id, "HIM_TRAINING_SEQUENCE_CONSTRUCTION_CONTRACT_V1", "SEQUENCE_CONTRACT_MISMATCH")
    _equal_or_fail(policy.sequence_contract_version, "1", "SEQUENCE_CONTRACT_MISMATCH")
    _equal_or_fail(policy.length_contract_id, "HIM_TRAINING_SEQUENCE_LENGTH_PADDING_TRUNCATION_CONTRACT_V1", "LENGTH_CONTRACT_MISMATCH")
    _equal_or_fail(policy.length_contract_version, "1", "LENGTH_CONTRACT_MISMATCH")
    _equal_or_fail(policy.tensor_contract_id, "HIM_TRAINING_TENSOR_CONSTRUCTION_CONTRACT_V1", "TENSOR_CONTRACT_MISMATCH")
    _equal_or_fail(policy.tensor_contract_version, "1", "TENSOR_CONTRACT_MISMATCH")
    _equal_or_fail(policy.batch_collation_contract_id, "HIM_P1_POINT_12_BATCH_COLLATION_CONTRACT_V1", "BATCH_COLLATION_CONTRACT_MISMATCH")
    _equal_or_fail(policy.batch_collation_contract_version, "1", "BATCH_COLLATION_CONTRACT_MISMATCH")
    _equal_or_fail(policy.objective_weighting_authority_contract_id, "HIM_MULTI_OBJECTIVE_WEIGHTING_AUTHORITY_V1", "OBJECTIVE_WEIGHTING_AUTHORITY_MISMATCH")
    _equal_or_fail(policy.objective_weighting_authority_contract_version, "1", "OBJECTIVE_WEIGHTING_AUTHORITY_MISMATCH")
    _equal_or_fail(policy.objective_weighting_authority_digest, _objective_weighting_authority_digest(), "OBJECTIVE_WEIGHTING_AUTHORITY_DIGEST_MISMATCH")
    _equal_or_fail(policy.max_sequence_length, MAX_SEQUENCE_LENGTH, "MAX_LENGTH_POLICY_MISMATCH")
    _equal_or_fail(policy.padding_strategy, "BATCH_LONGEST_WITH_HARD_UPPER_BOUND", "PADDING_POLICY_MISMATCH")
    _equal_or_fail(policy.padding_side, "RIGHT", "PADDING_SIDE_MISMATCH")
    _equal_or_fail(policy.pad_token_id, PAD_TOKEN_ID, "PAD_TOKEN_MISMATCH")
    _equal_or_fail(policy.truncation_policy, "NO_TRUNCATION_FAIL_IF_TOO_LONG", "TRUNCATION_POLICY_MISMATCH")
    _equal_or_fail(policy.attention_mask_real_token_value, REAL_TOKEN_MASK, "ATTENTION_MASK_POLICY_MISMATCH")
    _equal_or_fail(policy.attention_mask_pad_value, PAD_MASK, "ATTENTION_MASK_POLICY_MISMATCH")
    _fail_if(policy.token_type_ids_required, "TOKEN_TYPE_IDS_FORBIDDEN")
    _equal_or_fail(policy.primary_target_encoding, (("EXISTING_CANONICAL", 1), ("IDENTITY", 2), ("VARIANT", 3), ("ALIAS", 4), ("NEW_CANONICAL", 5)), "PRIMARY_ENCODING_MISMATCH")
    _equal_or_fail(policy.secondary_target_encoding, (("COMPATIBLE", 0), ("REJECT", 1)), "SECONDARY_ENCODING_MISMATCH")
    _equal_or_fail((policy.primary_mask_active, policy.primary_mask_inactive, policy.secondary_mask_active, policy.secondary_mask_inactive), (1.0, 0.0, 1.0, 0.0), "MASK_POLICY_MISMATCH")
    _equal_or_fail((policy.input_ids_dtype, policy.attention_mask_dtype, policy.target_dtype, policy.objective_mask_dtype), ("INT64", "INT64", "INT64", "FLOAT32"), "DTYPE_POLICY_MISMATCH")
    _equal_or_fail(policy.shape_policy, "[B,L]_INPUTS_AND_MASKS_[B]_TARGETS_AND_MASKS", "SHAPE_POLICY_MISMATCH")
    _fail_if(any(member.primary_mask == 0.0 and member.secondary_mask == 0.0 for member in batch.members), "ZERO_ACTIVE_OBJECTIVES")


def _validate_runtime_binding(batch: Point12Batch, tokenizer_path: str | Path | None) -> None:
    binding = batch.model_tokenizer_binding
    _equal_or_fail(binding.model_family_id, MODEL_FAMILY_ID, "MODEL_FAMILY_MISMATCH")
    _equal_or_fail(binding.base_model_id, BASE_MODEL_ID, "BASE_MODEL_MISMATCH")
    _equal_or_fail(binding.model_revision, MODEL_REVISION, "MODEL_REVISION_MISMATCH")
    _equal_or_fail(binding.tokenizer_id, TOKENIZER_ID, "TOKENIZER_ID_MISMATCH")
    _equal_or_fail(binding.tokenizer_revision, TOKENIZER_REVISION, "TOKENIZER_REVISION_MISMATCH")
    if tokenizer_path is not None:
        actual_digest = hashlib.sha256(Path(tokenizer_path).read_bytes()).hexdigest()
        _equal_or_fail(binding.tokenizer_artifact_digest, actual_digest, "TOKENIZER_BINDING_DIGEST_MISMATCH")


def _encode_authoritative_sequence(tokenizer: Tokenizer, sequence: Any) -> Encoding:
    _equal_or_fail(sequence.contract_id, "HIM_TRAINING_SEQUENCE_CONSTRUCTION_CONTRACT_V1", "SEQUENCE_CONTRACT_MISMATCH")
    _equal_or_fail(sequence.version, "1", "SEQUENCE_VERSION_MISMATCH")
    _equal_or_fail(sequence.state, "TRAINING_SEQUENCE_CONSTRUCTION_DEFINED", "SEQUENCE_STATE_MISMATCH")
    _fail_if(len(sequence.segments) != 2, "SEQUENCE_SEGMENT_COUNT_INVALID")
    _equal_or_fail(sequence.segments[0][0], "OBSERVED_TERM", "SEQUENCE_SEGMENTS_INVALID")
    _equal_or_fail(sequence.segments[0][1], sequence.observed_term, "SEQUENCE_SEGMENTS_INVALID")
    _equal_or_fail(sequence.segments[1][0], "COMPATIBILITY_SUBJECT", "SEQUENCE_SEGMENTS_INVALID")
    _equal_or_fail(sequence.segments[1][1], f"{sequence.subject_kind} {sequence.subject_term}", "SEQUENCE_SEGMENTS_INVALID")
    expected_digest = _sequence_digest(sequence.subject_kind, sequence.segments)
    _equal_or_fail(sequence.logical_digest, expected_digest, "SEQUENCE_DIGEST_MISMATCH")
    _equal_or_fail(sequence.sequence_reference, f"training-sequence:v1:{sequence.logical_digest}", "SEQUENCE_REFERENCE_MISMATCH")
    try:
        encoding = tokenizer.encode(sequence.segments[0][1], sequence.segments[1][1], add_special_tokens=True)
    except Exception as error:
        raise Point12TensorBuildError("TOKENIZATION_FAILED") from error
    _fail_if(len(encoding.ids) != len(encoding.type_ids), "TOKEN_TYPE_IDS_SHAPE_INVALID")
    _fail_if(any(value != 0 for value in encoding.type_ids), "TOKEN_TYPE_IDS_NONZERO")
    _fail_if(any(value != 1 for value in encoding.attention_mask), "TOKENIZER_ATTENTION_MASK_INVALID")
    special_positions = [index for index, value in enumerate(encoding.special_tokens_mask) if value == 1]
    _fail_if(len(special_positions) != SPECIAL_TOKEN_COUNT, "SPECIAL_TOKEN_LAYOUT_INVALID")
    _fail_if(special_positions[0] != 0 or special_positions[-1] != len(encoding.ids) - 1, "SPECIAL_TOKEN_LAYOUT_INVALID")
    _fail_if(special_positions[2] != special_positions[1] + 1, "SPECIAL_TOKEN_LAYOUT_INVALID")
    _fail_if(encoding.ids[0] != 0 or encoding.ids[-1] != 2, "SPECIAL_TOKEN_LAYOUT_INVALID")
    return encoding


def _validate_tokenizer_instance(tokenizer: Tokenizer) -> None:
    _equal_or_fail(tokenizer.token_to_id("<pad>"), PAD_TOKEN_ID, "PAD_TOKEN_ID_MISMATCH")
    _equal_or_fail(tokenizer.token_to_id("<s>"), 0, "START_TOKEN_ID_MISMATCH")
    _equal_or_fail(tokenizer.token_to_id("</s>"), 2, "END_TOKEN_ID_MISMATCH")
    tokenizer.no_truncation()


def _validate_padding(input_ids: list[list[int]], attention_mask: list[list[int]], raw_lengths: list[int]) -> None:
    for ids, mask, raw_length in zip(input_ids, attention_mask, raw_lengths):
        _fail_if(len(ids) != len(mask), "INPUT_MASK_LENGTH_MISMATCH")
        _fail_if(sum(mask) != raw_length, "ATTENTION_MASK_SUM_INVALID")
        padding_started = False
        for token_id, mask_value in zip(ids, mask):
            is_pad = token_id == PAD_TOKEN_ID
            is_masked = mask_value == PAD_MASK
            _fail_if(is_pad != is_masked, "PAD_MASK_MISMATCH")
            if is_pad:
                padding_started = True
            _fail_if(not is_pad and padding_started, "INTERNAL_PADDING")
        _fail_if(not any(value == REAL_TOKEN_MASK for value in mask), "ALL_PADDING_INPUT")


def _validate_tensor_contract(batch: Point12TensorBatchV1) -> None:
    _fail_if(torch.any(batch.input_ids == PAD_TOKEN_ID).item() and not torch.all((batch.input_ids == PAD_TOKEN_ID) == (batch.attention_mask == PAD_MASK)).item(), "PAD_MASK_MISMATCH")
    _fail_if(torch.any((batch.primary_mask == 0.0) & (batch.secondary_mask == 0.0)).item(), "ZERO_ACTIVE_OBJECTIVES")


def _equal_or_fail(actual: Any, expected: Any, message: str) -> None:
    if actual != expected:
        raise Point12TensorBuildError(message)


def _fail_if(condition: bool, message: str) -> None:
    if condition:
        raise Point12TensorBuildError(message)
