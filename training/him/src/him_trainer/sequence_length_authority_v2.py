"""Versioned, target-independent sequence-length authority for HIM V2.

The authority is deliberately separate from the V1 Point-12 tensor builder.
It validates the number of IDs produced by the pinned tokenizer for one
serialized ``HIM_INPUT_REPRESENTATION_V2`` value before batch padding.  It
does not truncate, shorten, inspect targets, or depend on a partition.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import asdict, dataclass


SEQUENCE_LENGTH_AUTHORITY_ID = "HIM_SEQUENCE_LENGTH_AUTHORITY_V2"
SEQUENCE_LENGTH_AUTHORITY_VERSION = "2"
INPUT_REPRESENTATION_ID = "HIM_INPUT_REPRESENTATION_V2"
INPUT_REPRESENTATION_VERSION = "2"

MODEL_ID = "FacebookAI/xlm-roberta-base"
MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
MODEL_MAX_POSITION_EMBEDDINGS = 514
MODEL_POSITIONAL_LIMIT = 512
SPECIAL_POSITION_OFFSET = 2

TOKENIZER_ID = "xlm-roberta-base-tokenizer"
TOKENIZER_REVISION = MODEL_REVISION
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"
BOS_TOKEN_ID = 0
PAD_TOKEN_ID = 1
EOS_TOKEN_ID = 2
V2_SPECIAL_TOKEN_COUNT = 2
POINT12_PAIR_SPECIAL_TOKEN_COUNT = 4

SEQUENCE_LENGTH_V1 = 128
SEQUENCE_LENGTH_V2 = 256
TRUNCATION_POLICY = "NO_TRUNCATION_FAIL_IF_TOO_LONG"
PADDING_POLICY = "RIGHT_BATCH_LONGEST_WITH_HARD_UPPER_BOUND"
PADDING_SIDE = "RIGHT"
OVERFLOW_POLICY = "FAIL_CLOSED"
OVERFLOW_ERROR_CLASS = "HimSequenceLengthV2Error"
OVERFLOW_ERROR_MARKER = "INPUT_SEQUENCE_TOO_LONG"
SPECIAL_TOKEN_POLICY = "TOKENIZER_ADDS_BOS_EOS_BEFORE_PADDING"
LENGTH_MEASUREMENT = (
    "TOKENIZER_IDS_FROM_HIMV2_SERIALIZATION_WITH_SPECIAL_TOKENS_BEFORE_PADDING"
)


class HimSequenceLengthV2Error(ValueError):
    """Raised when a V2 sequence cannot be consumed without truncation."""


@dataclass(frozen=True)
class SequenceLengthAuthorityV2:
    """Complete immutable identity and policy for the V2 length authority."""

    authority_id: str = SEQUENCE_LENGTH_AUTHORITY_ID
    authority_version: str = SEQUENCE_LENGTH_AUTHORITY_VERSION
    input_representation_id: str = INPUT_REPRESENTATION_ID
    input_representation_version: str = INPUT_REPRESENTATION_VERSION
    tokenizer_id: str = TOKENIZER_ID
    tokenizer_revision: str = TOKENIZER_REVISION
    tokenizer_sha256: str = TOKENIZER_SHA256
    model_id: str = MODEL_ID
    model_revision: str = MODEL_REVISION
    model_max_position_embeddings: int = MODEL_MAX_POSITION_EMBEDDINGS
    model_usable_sequence_limit: int = MODEL_POSITIONAL_LIMIT
    special_position_offset: int = SPECIAL_POSITION_OFFSET
    tokenizer_special_token_count: int = V2_SPECIAL_TOKEN_COUNT
    max_sequence_length: int = SEQUENCE_LENGTH_V2
    truncation_policy: str = TRUNCATION_POLICY
    padding_policy: str = PADDING_POLICY
    padding_side: str = PADDING_SIDE
    overflow_policy: str = OVERFLOW_POLICY
    overflow_error_class: str = OVERFLOW_ERROR_CLASS
    overflow_error_marker: str = OVERFLOW_ERROR_MARKER
    special_token_policy: str = SPECIAL_TOKEN_POLICY
    length_measurement: str = LENGTH_MEASUREMENT

    def __post_init__(self) -> None:
        if self.max_sequence_length <= 0:
            raise ValueError("MAX_SEQUENCE_LENGTH_MUST_BE_POSITIVE")
        if self.model_usable_sequence_limit <= 0:
            raise ValueError("MODEL_USABLE_SEQUENCE_LIMIT_MUST_BE_POSITIVE")
        if self.model_usable_sequence_limit > self.model_max_position_embeddings:
            raise ValueError("MODEL_USABLE_LIMIT_EXCEEDS_POSITION_EMBEDDINGS")
        if self.max_sequence_length > self.model_usable_sequence_limit:
            raise ValueError("MAX_SEQUENCE_LENGTH_EXCEEDS_MODEL_CAPACITY")
        if self.truncation_policy != TRUNCATION_POLICY:
            raise ValueError("TRUNCATION_POLICY_MUST_REMAIN_FAIL_CLOSED")
        if self.padding_policy != PADDING_POLICY or self.padding_side != PADDING_SIDE:
            raise ValueError("PADDING_POLICY_MISMATCH")
        if self.overflow_policy != OVERFLOW_POLICY:
            raise ValueError("OVERFLOW_POLICY_MUST_REMAIN_FAIL_CLOSED")
        if self.tokenizer_special_token_count != V2_SPECIAL_TOKEN_COUNT:
            raise ValueError("V2_SPECIAL_TOKEN_COUNT_MISMATCH")

    @property
    def authority_reference(self) -> str:
        return f"sequence-length-authority:v2:{self.logical_digest}"

    @property
    def logical_digest(self) -> str:
        encoded = json.dumps(
            asdict(self), sort_keys=True, separators=(",", ":"), ensure_ascii=False
        ).encode("utf-8")
        return hashlib.sha256(encoded).hexdigest()

    def validate_token_length(self, token_length: int) -> None:
        """Accept an unpadded length or fail closed; never truncate."""

        if isinstance(token_length, bool) or not isinstance(token_length, int):
            raise HimSequenceLengthV2Error("TOKEN_LENGTH_MUST_BE_INTEGER")
        if token_length < 0:
            raise HimSequenceLengthV2Error("TOKEN_LENGTH_MUST_NOT_BE_NEGATIVE")
        if token_length > self.max_sequence_length:
            raise HimSequenceLengthV2Error(
                f"{self.overflow_error_marker}:actual={token_length}:"
                f"authorized={self.max_sequence_length}:"
                f"input={self.input_representation_id}:"
                f"tokenizer={self.tokenizer_id}"
            )

    def validate_model_position_capacity(self) -> None:
        """Confirm that this authority remains safe for the pinned model."""

        if self.max_sequence_length > self.model_usable_sequence_limit:
            raise HimSequenceLengthV2Error("MODEL_POSITIONAL_CAPACITY_EXCEEDED")


def sequence_length_authority_v2() -> SequenceLengthAuthorityV2:
    """Return the immutable current V2 authority."""

    return SequenceLengthAuthorityV2()


def validate_train_eval_authority_match(
    train_authority: SequenceLengthAuthorityV2,
    evaluation_authority: SequenceLengthAuthorityV2,
) -> None:
    """Require one identical authority for training and evaluation."""

    if train_authority.authority_reference != evaluation_authority.authority_reference:
        raise HimSequenceLengthV2Error("TRAIN_EVAL_SEQUENCE_AUTHORITY_MISMATCH")


__all__ = [
    "BOS_TOKEN_ID",
    "EOS_TOKEN_ID",
    "HimSequenceLengthV2Error",
    "INPUT_REPRESENTATION_ID",
    "INPUT_REPRESENTATION_VERSION",
    "MODEL_ID",
    "MODEL_MAX_POSITION_EMBEDDINGS",
    "MODEL_POSITIONAL_LIMIT",
    "MODEL_REVISION",
    "PADDING_POLICY",
    "PADDING_SIDE",
    "PAD_TOKEN_ID",
    "POINT12_PAIR_SPECIAL_TOKEN_COUNT",
    "SEQUENCE_LENGTH_AUTHORITY_ID",
    "SEQUENCE_LENGTH_AUTHORITY_VERSION",
    "SEQUENCE_LENGTH_V1",
    "SEQUENCE_LENGTH_V2",
    "SPECIAL_POSITION_OFFSET",
    "TOKENIZER_ID",
    "TOKENIZER_REVISION",
    "TOKENIZER_SHA256",
    "TRUNCATION_POLICY",
    "SequenceLengthAuthorityV2",
    "sequence_length_authority_v2",
    "validate_train_eval_authority_match",
]
