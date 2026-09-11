"""Framework-neutral Point-13 base-encoder trainability authority."""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from typing import Any


TRAINABILITY_CONTRACT_ID_V1 = "HIM_BASE_ENCODER_TRAINABILITY_POLICY_V1"
TRAINABILITY_CONTRACT_VERSION_V1 = "1"
TRAINABILITY_CONTRACT_STATE_V1 = "BASE_ENCODER_TRAINABILITY_POLICY_VALIDATED"
TRAINABILITY_POLICY_KIND_FULL_FINE_TUNE_V1 = "FULL_FINE_TUNE"
TRAINABILITY_POLICY_KIND_BASE_ENCODER_FROZEN_V1 = "BASE_ENCODER_FROZEN"
TRAINABILITY_BASE_ENCODER_SCOPE_V1 = "ALL_BASE_ENCODER_PARAMETERS"
TRAINABILITY_HEAD_SCOPE_PRIMARY_V1 = "primary_head.**"
TRAINABILITY_HEAD_SCOPE_SECONDARY_V1 = "secondary_head.**"
TRAINABILITY_BASE_SCOPE_V1 = "base_model.**"


class HimTrainabilityPolicyError(ValueError):
    """Raised when trainability authority is absent or inconsistent."""


def _digest(value: dict[str, object]) -> str:
    encoded = json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()


@dataclass(frozen=True)
class HimBaseEncoderTrainabilityPolicyV1:
    """Complete, deterministic Point-13 trainability policy."""

    contract_id: str
    version: str
    state: str
    policy_kind: str
    base_encoder_trainable: bool
    base_encoder_scope: str
    trainable_parameter_scopes: tuple[str, ...]
    frozen_parameter_scopes: tuple[str, ...]
    embeddings_trainable: bool
    unused_pooler_policy: str
    unused_lm_head_policy: str
    primary_head_trainable: bool
    secondary_head_trainable: bool
    staged_unfreeze_schedule: str
    logical_digest: str
    reference: str

    def identity_payload(self) -> dict[str, object]:
        return {
            "contractId": self.contract_id,
            "version": self.version,
            "state": self.state,
            "policyKind": self.policy_kind,
            "baseEncoderTrainable": self.base_encoder_trainable,
            "baseEncoderScope": self.base_encoder_scope,
            "trainableParameterScopes": list(self.trainable_parameter_scopes),
            "frozenParameterScopes": list(self.frozen_parameter_scopes),
            "embeddingsTrainable": self.embeddings_trainable,
            "unusedPoolerPolicy": self.unused_pooler_policy,
            "unusedLmHeadPolicy": self.unused_lm_head_policy,
            "primaryHeadTrainable": self.primary_head_trainable,
            "secondaryHeadTrainable": self.secondary_head_trainable,
            "stagedUnfreezeSchedule": self.staged_unfreeze_schedule,
        }


def build_him_base_encoder_trainability_policy_v1() -> HimBaseEncoderTrainabilityPolicyV1:
    """Return the sole frozen V1 policy: full encoder fine-tuning plus trainable heads."""

    unsigned = HimBaseEncoderTrainabilityPolicyV1(
        contract_id=TRAINABILITY_CONTRACT_ID_V1,
        version=TRAINABILITY_CONTRACT_VERSION_V1,
        state=TRAINABILITY_CONTRACT_STATE_V1,
        policy_kind=TRAINABILITY_POLICY_KIND_FULL_FINE_TUNE_V1,
        base_encoder_trainable=True,
        base_encoder_scope=TRAINABILITY_BASE_ENCODER_SCOPE_V1,
        trainable_parameter_scopes=(TRAINABILITY_BASE_SCOPE_V1, TRAINABILITY_HEAD_SCOPE_PRIMARY_V1, TRAINABILITY_HEAD_SCOPE_SECONDARY_V1),
        frozen_parameter_scopes=(),
        embeddings_trainable=True,
        unused_pooler_policy="NOT_PRESENT",
        unused_lm_head_policy="NOT_PRESENT",
        primary_head_trainable=True,
        secondary_head_trainable=True,
        staged_unfreeze_schedule="NONE",
        logical_digest="0" * 64,
        reference="",
    )
    logical_digest = _digest(unsigned.identity_payload())
    return HimBaseEncoderTrainabilityPolicyV1(
        **{**unsigned.__dict__, "logical_digest": logical_digest, "reference": f"him-base-encoder-trainability:v1:{logical_digest}"},
    )


def validate_him_base_encoder_trainability_policy_v1(
    policy: HimBaseEncoderTrainabilityPolicyV1,
) -> None:
    """Validate exact V1 semantics before runtime projection."""

    if policy.contract_id != TRAINABILITY_CONTRACT_ID_V1:
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_CONTRACT_MISMATCH")
    if policy.version != TRAINABILITY_CONTRACT_VERSION_V1:
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_VERSION_MISMATCH")
    if policy.state != TRAINABILITY_CONTRACT_STATE_V1:
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_STATE_MISMATCH")
    if policy.policy_kind == TRAINABILITY_POLICY_KIND_FULL_FINE_TUNE_V1:
        expected = (
            True, TRAINABILITY_BASE_ENCODER_SCOPE_V1,
            (TRAINABILITY_BASE_SCOPE_V1, TRAINABILITY_HEAD_SCOPE_PRIMARY_V1, TRAINABILITY_HEAD_SCOPE_SECONDARY_V1),
            (), True,
        )
    elif policy.policy_kind == TRAINABILITY_POLICY_KIND_BASE_ENCODER_FROZEN_V1:
        expected = (
            False, TRAINABILITY_BASE_ENCODER_SCOPE_V1,
            (TRAINABILITY_HEAD_SCOPE_PRIMARY_V1, TRAINABILITY_HEAD_SCOPE_SECONDARY_V1),
            (TRAINABILITY_BASE_SCOPE_V1,), False,
        )
    else:
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_KIND_UNSUPPORTED")
    if (
        policy.base_encoder_trainable,
        policy.base_encoder_scope,
        policy.trainable_parameter_scopes,
        policy.frozen_parameter_scopes,
        policy.embeddings_trainable,
    ) != expected:
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_SCOPE_MISMATCH")
    if (
        policy.unused_pooler_policy,
        policy.unused_lm_head_policy,
        policy.primary_head_trainable,
        policy.secondary_head_trainable,
        policy.staged_unfreeze_schedule,
    ) != ("NOT_PRESENT", "NOT_PRESENT", True, True, "NONE"):
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_HEAD_OR_SCHEDULE_MISMATCH")
    expected_digest = _digest(policy.identity_payload())
    if policy.logical_digest != expected_digest:
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_DIGEST_MISMATCH")
    if policy.reference != f"him-base-encoder-trainability:v1:{policy.logical_digest}":
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_REFERENCE_MISMATCH")


def trainability_policy_wire_v1(policy: HimBaseEncoderTrainabilityPolicyV1) -> dict[str, object]:
    validate_him_base_encoder_trainability_policy_v1(policy)
    return {**policy.identity_payload(), "logicalDigest": policy.logical_digest, "reference": policy.reference}


def decode_him_base_encoder_trainability_policy_v1(value: object) -> HimBaseEncoderTrainabilityPolicyV1:
    if not isinstance(value, dict):
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_OBJECT_REQUIRED")
    required = {
        "contractId", "version", "state", "policyKind", "baseEncoderTrainable", "baseEncoderScope",
        "trainableParameterScopes", "frozenParameterScopes", "embeddingsTrainable", "unusedPoolerPolicy",
        "unusedLmHeadPolicy", "primaryHeadTrainable", "secondaryHeadTrainable", "stagedUnfreezeSchedule",
        "logicalDigest", "reference",
    }
    if set(value) != required:
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_FIELDS_INVALID")
    try:
        scopes = tuple(value["trainableParameterScopes"])
        frozen_scopes = tuple(value["frozenParameterScopes"])
        if not all(isinstance(item, str) for item in scopes + frozen_scopes):
            raise TypeError
        policy = HimBaseEncoderTrainabilityPolicyV1(
            contract_id=value["contractId"], version=value["version"], state=value["state"],
            policy_kind=value["policyKind"], base_encoder_trainable=value["baseEncoderTrainable"],
            base_encoder_scope=value["baseEncoderScope"], trainable_parameter_scopes=scopes,
            frozen_parameter_scopes=frozen_scopes, embeddings_trainable=value["embeddingsTrainable"],
            unused_pooler_policy=value["unusedPoolerPolicy"], unused_lm_head_policy=value["unusedLmHeadPolicy"],
            primary_head_trainable=value["primaryHeadTrainable"], secondary_head_trainable=value["secondaryHeadTrainable"],
            staged_unfreeze_schedule=value["stagedUnfreezeSchedule"], logical_digest=value["logicalDigest"],
            reference=value["reference"],
        )
    except (KeyError, TypeError, ValueError) as error:
        raise HimTrainabilityPolicyError("TRAINABILITY_POLICY_VALUES_INVALID") from error
    validate_him_base_encoder_trainability_policy_v1(policy)
    return policy
