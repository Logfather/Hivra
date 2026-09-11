"""Explicit PyTorch requires_grad projection for Point-13 trainability authority."""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from typing import Any

from .point13_trainability_policy_v1 import (
    HimBaseEncoderTrainabilityPolicyV1,
    HimTrainabilityPolicyError,
    validate_him_base_encoder_trainability_policy_v1,
)


@dataclass(frozen=True)
class HimTrainabilityProjectionV1:
    policy_digest: str
    parameter_states: tuple[tuple[str, str], ...]
    trainable_parameter_count: int
    frozen_parameter_count: int
    runtime_map_digest: str


def project_him_trainability_policy_v1(
    model: Any,
    policy: HimBaseEncoderTrainabilityPolicyV1,
) -> HimTrainabilityProjectionV1:
    """Classify every runtime parameter explicitly and apply the authority."""

    validate_him_base_encoder_trainability_policy_v1(policy)
    if hasattr(model, "roberta") or hasattr(model, "lm_head"):
        raise HimTrainabilityPolicyError("UNSUPPORTED_MODEL_ROOT")
    if hasattr(model, "base_model") is False or hasattr(model, "primary_head") is False or hasattr(model, "secondary_head") is False:
        raise HimTrainabilityPolicyError("MODEL_COMPONENTS_MISSING")
    if hasattr(model.base_model, "pooler") or hasattr(model.base_model, "lm_head"):
        raise HimTrainabilityPolicyError("UNUSED_PRETRAINED_COMPONENT_PRESENT")

    states: list[tuple[str, str]] = []
    for name, parameter in model.named_parameters():
        if name.startswith("base_model."):
            trainable = policy.base_encoder_trainable
        elif name.startswith("primary_head."):
            trainable = policy.primary_head_trainable
        elif name.startswith("secondary_head."):
            trainable = policy.secondary_head_trainable
        else:
            raise HimTrainabilityPolicyError(f"PARAMETER_SCOPE_UNCLASSIFIED:{name}")
        parameter.requires_grad_(trainable)
        states.append((name, "TRAINABLE" if trainable else "FROZEN"))

    if not states:
        raise HimTrainabilityPolicyError("MODEL_PARAMETERS_EMPTY")
    if len({name for name, _ in states}) != len(states):
        raise HimTrainabilityPolicyError("PARAMETER_SCOPE_DUPLICATED")
    encoded = json.dumps(
        [{"name": name, "state": state} for name, state in states],
        ensure_ascii=False, separators=(",", ":"),
    )
    digest = hashlib.sha256(encoded.encode("utf-8")).hexdigest()
    trainable_count = sum(parameter.numel() for parameter in model.parameters() if parameter.requires_grad)
    total_count = sum(parameter.numel() for parameter in model.parameters())
    return HimTrainabilityProjectionV1(
        policy_digest=policy.logical_digest,
        parameter_states=tuple(states),
        trainable_parameter_count=trainable_count,
        frozen_parameter_count=total_count - trainable_count,
        runtime_map_digest=digest,
    )
