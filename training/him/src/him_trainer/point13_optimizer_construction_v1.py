"""Deterministic structural AdamW construction for Point 13.

Construction is intentionally the last operation in this module.  No
backward, gradient clipping, scheduler step, optimizer step, or parameter
update is performed here.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from typing import Any

import torch

from .point13_optimizer_execution_policy_v1 import (
    HimOptimizerExecutionPolicyV1,
    HimOptimizerExecutionPolicyError,
    validate_him_optimizer_execution_policy_v1,
)
from .point13_trainability_projection_v1 import HimTrainabilityProjectionV1


@dataclass(frozen=True)
class HimOptimizerParameterGroupV1:
    group_id: str
    parameter_names: tuple[str, ...]
    parameter_tensor_count: int
    parameter_element_count: int
    learning_rate: str
    weight_decay: str
    logical_digest: str


@dataclass(frozen=True)
class HimOptimizerConstructionV1:
    policy_digest: str
    groups: tuple[HimOptimizerParameterGroupV1, ...]
    parameter_group_map_digest: str
    initial_state_entry_count: int
    initial_structure_digest: str


def _digest(value: object) -> str:
    encoded = json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()


def _is_no_decay_parameter(name: str) -> bool:
    """Use exact bias and LayerNorm suffixes; never a broad ``norm`` match."""

    return name.endswith(".bias") or name.endswith(".LayerNorm.weight")


def _group_digest(policy: HimOptimizerExecutionPolicyV1, group_id: str, names: tuple[str, ...], weight_decay: str) -> str:
    return _digest({
        "policyDigest": policy.logical_digest,
        "groupId": group_id,
        "parameterNames": list(names),
        "learningRate": policy.learning_rate,
        "weightDecay": weight_decay,
    })


def _group(
    policy: HimOptimizerExecutionPolicyV1,
    group_id: str,
    names: tuple[str, ...],
    parameters: dict[str, torch.nn.Parameter],
    weight_decay: str,
) -> HimOptimizerParameterGroupV1:
    return HimOptimizerParameterGroupV1(
        group_id=group_id,
        parameter_names=names,
        parameter_tensor_count=len(names),
        parameter_element_count=sum(parameters[name].numel() for name in names),
        learning_rate=policy.learning_rate,
        weight_decay=weight_decay,
        logical_digest=_group_digest(policy, group_id, names, weight_decay),
    )


def build_him_optimizer_parameter_groups_v1(
    model: Any,
    policy: HimOptimizerExecutionPolicyV1,
    projection: HimTrainabilityProjectionV1,
) -> tuple[HimOptimizerParameterGroupV1, ...]:
    """Build the ordered DECAY/NO_DECAY map from authorized trainable params."""

    validate_him_optimizer_execution_policy_v1(policy)
    parameters = dict(model.named_parameters())
    states = dict(projection.parameter_states)
    if len(states) != len(projection.parameter_states):
        raise HimOptimizerExecutionPolicyError("TRAINABILITY_PARAMETER_NAMES_DUPLICATED")
    authorized = {name for name, state in states.items() if state == "TRAINABLE"}
    runtime_trainable = {name for name, parameter in parameters.items() if parameter.requires_grad}
    if authorized != runtime_trainable:
        raise HimOptimizerExecutionPolicyError("AUTHORIZED_TRAINABLE_PARAMETER_SET_MISMATCH")
    if any(state not in {"TRAINABLE", "FROZEN"} for state in states.values()):
        raise HimOptimizerExecutionPolicyError("TRAINABILITY_STATE_INVALID")
    if set(parameters) != set(states):
        raise HimOptimizerExecutionPolicyError("TRAINABILITY_PARAMETER_COVERAGE_MISMATCH")

    decay_names = tuple(sorted(name for name in authorized if not _is_no_decay_parameter(name)))
    no_decay_names = tuple(sorted(name for name in authorized if _is_no_decay_parameter(name)))
    if set(decay_names) & set(no_decay_names) or set(decay_names) | set(no_decay_names) != authorized:
        raise HimOptimizerExecutionPolicyError("PARAMETER_GROUP_COVERAGE_INVALID")
    groups = (
        _group(policy, "DECAY", decay_names, parameters, policy.weight_decay),
        _group(policy, "NO_DECAY", no_decay_names, parameters, "0"),
    )
    if any(not group.parameter_names for group in groups):
        raise HimOptimizerExecutionPolicyError("PARAMETER_GROUP_EMPTY")
    if len(set().union(*(set(group.parameter_names) for group in groups))) != len(authorized):
        raise HimOptimizerExecutionPolicyError("PARAMETER_GROUP_DUPLICATE_OR_MISSING")
    return groups


def _construction_digest(
    policy: HimOptimizerExecutionPolicyV1,
    groups: tuple[HimOptimizerParameterGroupV1, ...],
    initial_state_entry_count: int,
) -> str:
    return _digest({
        "policyDigest": policy.logical_digest,
        "groups": [
            {
                "groupId": group.group_id,
                "parameterNames": list(group.parameter_names),
                "parameterTensorCount": group.parameter_tensor_count,
                "parameterElementCount": group.parameter_element_count,
                "learningRate": group.learning_rate,
                "weightDecay": group.weight_decay,
                "logicalDigest": group.logical_digest,
            }
            for group in groups
        ],
        "initialStateEntryCount": initial_state_entry_count,
    })


def construct_him_adamw_v1(
    model: Any,
    policy: HimOptimizerExecutionPolicyV1,
    projection: HimTrainabilityProjectionV1,
) -> tuple[torch.optim.AdamW, HimOptimizerConstructionV1]:
    """Construct one explicit AdamW instance without executing it."""

    groups = build_him_optimizer_parameter_groups_v1(model, policy, projection)
    named_parameters = dict(model.named_parameters())
    requires_grad_before = {name: parameter.requires_grad for name, parameter in named_parameters.items()}
    optimizer = torch.optim.AdamW(
        [
            {
                "params": [named_parameters[name] for name in group.parameter_names],
                "lr": float(group.learning_rate),
                "weight_decay": float(group.weight_decay),
            }
            for group in groups
        ],
        lr=float(policy.learning_rate),
        betas=(float(policy.beta1), float(policy.beta2)),
        eps=float(policy.epsilon),
        amsgrad=policy.amsgrad,
        maximize=policy.maximize,
        foreach=policy.foreach,
        capturable=policy.capturable,
        differentiable=policy.differentiable,
        fused=policy.fused,
    )
    requires_grad_after = {name: parameter.requires_grad for name, parameter in named_parameters.items()}
    if requires_grad_before != requires_grad_after:
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_CONSTRUCTION_CHANGED_TRAINABILITY")
    initial_state_entry_count = len(optimizer.state)
    if initial_state_entry_count != 0:
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_INITIAL_STATE_NOT_EMPTY")
    parameter_group_map_digest = _digest({
        "policyDigest": policy.logical_digest,
        "groups": [group.logical_digest for group in groups],
    })
    construction = HimOptimizerConstructionV1(
        policy_digest=policy.logical_digest,
        groups=groups,
        parameter_group_map_digest=parameter_group_map_digest,
        initial_state_entry_count=initial_state_entry_count,
        initial_structure_digest=_construction_digest(policy, groups, initial_state_entry_count),
    )
    return optimizer, construction


def validate_train_split_execution_gate_v1(partition: str, policy: HimOptimizerExecutionPolicyV1) -> None:
    validate_him_optimizer_execution_policy_v1(policy)
    if policy.optimizer_step_train_split_only and partition != "TRAIN_ONLY":
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_STEP_TRAIN_SPLIT_REQUIRED")
