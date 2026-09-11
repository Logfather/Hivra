"""Deterministic Point-13 AdamW execution authority.

This module defines optimizer semantics and parameter-group authority.  It
does not call backward, step, scheduler.step, or any training loop.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, replace
from typing import Any


OPTIMIZER_EXECUTION_CONTRACT_ID_V1 = "HIM_OPTIMIZER_EXECUTION_POLICY_V1"
OPTIMIZER_EXECUTION_CONTRACT_VERSION_V1 = "1"
OPTIMIZER_EXECUTION_CONTRACT_STATE_V1 = "OPTIMIZER_EXECUTION_POLICY_VALIDATED"
OPTIMIZER_ID_ADAMW_V1 = "optimizer:adamw:v1"
OPTIMIZER_TYPE_ADAMW_V1 = "ADAMW"
PARAMETER_GROUP_POLICY_STANDARD_NO_DECAY_V1 = "STANDARD_BIAS_AND_LAYERNORM_NO_DECAY"
NO_DECAY_PARAMETER_POLICY_V1 = "BIAS_AND_LAYERNORM_WEIGHT"
ZERO_GRAD_POLICY_SET_TO_NONE_V1 = "SET_TO_NONE"
GRADIENT_ACCUMULATION_LOSS_SCALING_MEAN_V1 = "MEAN_LOSS_PER_ACCUMULATION_WINDOW"
GRADIENT_CLIPPING_NONE_V1 = "NONE"
LR_SCHEDULER_NONE_V1 = "NONE"
WARMUP_NONE_V1 = "NONE"
PRECISION_FP32_V1 = "FP32"
DEVICE_CPU_V1 = "CPU"
DEVICE_CUDA_V1 = "CUDA"


class HimOptimizerExecutionPolicyError(ValueError):
    """Raised when optimizer authority is absent or internally inconsistent."""


def _digest(value: dict[str, object]) -> str:
    encoded = json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()


@dataclass(frozen=True)
class HimOptimizerExecutionPolicyV1:
    contract_id: str
    version: str
    state: str
    optimizer_id: str
    optimizer_type: str
    learning_rate: str
    beta1: str
    beta2: str
    epsilon: str
    weight_decay: str
    decoupled_weight_decay: bool
    bias_correction: bool
    amsgrad: bool
    maximize: bool
    foreach: bool
    fused: bool
    capturable: bool
    differentiable: bool
    parameter_group_policy: str
    no_decay_parameter_policy: str
    zero_grad_policy: str
    gradient_accumulation_steps: int
    gradient_accumulation_loss_scaling: str
    gradient_clipping_policy: str
    gradient_clip_threshold: str | None
    lr_scheduler_policy: str
    warmup_policy: str
    precision_policy: str
    amp_enabled: bool
    grad_scaler_enabled: bool
    device_policy: str
    optimizer_step_train_split_only: bool
    logical_digest: str
    reference: str

    def identity_payload(self) -> dict[str, object]:
        return {
            "contractId": self.contract_id,
            "version": self.version,
            "state": self.state,
            "optimizerId": self.optimizer_id,
            "optimizerType": self.optimizer_type,
            "learningRate": self.learning_rate,
            "beta1": self.beta1,
            "beta2": self.beta2,
            "epsilon": self.epsilon,
            "weightDecay": self.weight_decay,
            "decoupledWeightDecay": self.decoupled_weight_decay,
            "biasCorrection": self.bias_correction,
            "amsgrad": self.amsgrad,
            "maximize": self.maximize,
            "foreach": self.foreach,
            "fused": self.fused,
            "capturable": self.capturable,
            "differentiable": self.differentiable,
            "parameterGroupPolicy": self.parameter_group_policy,
            "noDecayParameterPolicy": self.no_decay_parameter_policy,
            "zeroGradPolicy": self.zero_grad_policy,
            "gradientAccumulationSteps": self.gradient_accumulation_steps,
            "gradientAccumulationLossScaling": self.gradient_accumulation_loss_scaling,
            "gradientClippingPolicy": self.gradient_clipping_policy,
            "gradientClipThreshold": self.gradient_clip_threshold,
            "lrSchedulerPolicy": self.lr_scheduler_policy,
            "warmupPolicy": self.warmup_policy,
            "precisionPolicy": self.precision_policy,
            "ampEnabled": self.amp_enabled,
            "gradScalerEnabled": self.grad_scaler_enabled,
            "devicePolicy": self.device_policy,
            "optimizerStepTrainSplitOnly": self.optimizer_step_train_split_only,
        }


def _finalize(policy: HimOptimizerExecutionPolicyV1) -> HimOptimizerExecutionPolicyV1:
    logical_digest = _digest(policy.identity_payload())
    return replace(
        policy,
        logical_digest=logical_digest,
        reference=f"him-optimizer-execution-policy:v1:{logical_digest}",
    )


def build_him_optimizer_execution_policy_v1(
    *,
    gradient_accumulation_steps: int = 1,
    device_policy: str = DEVICE_CPU_V1,
) -> HimOptimizerExecutionPolicyV1:
    """Build the selected, fully explicit CPU/CUDA FP32 AdamW authority."""

    if device_policy not in {DEVICE_CPU_V1, DEVICE_CUDA_V1}:
        raise HimOptimizerExecutionPolicyError("DEVICE_POLICY_UNSUPPORTED")

    return _finalize(
        HimOptimizerExecutionPolicyV1(
            contract_id=OPTIMIZER_EXECUTION_CONTRACT_ID_V1,
            version=OPTIMIZER_EXECUTION_CONTRACT_VERSION_V1,
            state=OPTIMIZER_EXECUTION_CONTRACT_STATE_V1,
            optimizer_id=OPTIMIZER_ID_ADAMW_V1,
            optimizer_type=OPTIMIZER_TYPE_ADAMW_V1,
            learning_rate="0.0001",
            beta1="0.9",
            beta2="0.999",
            epsilon="0.00000001",
            weight_decay="0.01",
            decoupled_weight_decay=True,
            bias_correction=True,
            amsgrad=False,
            maximize=False,
            foreach=False,
            fused=False,
            capturable=False,
            differentiable=False,
            parameter_group_policy=PARAMETER_GROUP_POLICY_STANDARD_NO_DECAY_V1,
            no_decay_parameter_policy=NO_DECAY_PARAMETER_POLICY_V1,
            zero_grad_policy=ZERO_GRAD_POLICY_SET_TO_NONE_V1,
            gradient_accumulation_steps=gradient_accumulation_steps,
            gradient_accumulation_loss_scaling=GRADIENT_ACCUMULATION_LOSS_SCALING_MEAN_V1,
            gradient_clipping_policy=GRADIENT_CLIPPING_NONE_V1,
            gradient_clip_threshold=None,
            lr_scheduler_policy=LR_SCHEDULER_NONE_V1,
            warmup_policy=WARMUP_NONE_V1,
            precision_policy=PRECISION_FP32_V1,
            amp_enabled=False,
            grad_scaler_enabled=False,
            device_policy=device_policy,
            optimizer_step_train_split_only=True,
            logical_digest="0" * 64,
            reference="",
        ),
    )


def rebind_him_optimizer_execution_policy_v1(
    policy: HimOptimizerExecutionPolicyV1,
    **changes: object,
) -> HimOptimizerExecutionPolicyV1:
    """Create a deterministic test/authority variant with a recomputed digest."""

    return _finalize(replace(policy, **changes, logical_digest="0" * 64, reference=""))


def validate_him_optimizer_execution_policy_v1(
    policy: HimOptimizerExecutionPolicyV1,
) -> None:
    if policy.contract_id != OPTIMIZER_EXECUTION_CONTRACT_ID_V1:
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_POLICY_CONTRACT_MISMATCH")
    if policy.version != OPTIMIZER_EXECUTION_CONTRACT_VERSION_V1:
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_POLICY_VERSION_MISMATCH")
    if policy.state != OPTIMIZER_EXECUTION_CONTRACT_STATE_V1:
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_POLICY_STATE_MISMATCH")
    if policy.optimizer_id != OPTIMIZER_ID_ADAMW_V1 or policy.optimizer_type != OPTIMIZER_TYPE_ADAMW_V1:
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_TYPE_MISMATCH")
    if policy.learning_rate != "0.0001":
        raise HimOptimizerExecutionPolicyError("LEARNING_RATE_AUTHORITY_MISMATCH")
    if (policy.beta1, policy.beta2, policy.epsilon, policy.weight_decay) != ("0.9", "0.999", "0.00000001", "0.01"):
        raise HimOptimizerExecutionPolicyError("ADAMW_HYPERPARAMETER_AUTHORITY_MISMATCH")
    if not policy.decoupled_weight_decay or not policy.bias_correction:
        raise HimOptimizerExecutionPolicyError("ADAMW_SEMANTICS_MISMATCH")
    if any((policy.amsgrad, policy.maximize, policy.foreach, policy.fused, policy.capturable, policy.differentiable)):
        raise HimOptimizerExecutionPolicyError("ADAMW_EXECUTION_FLAG_MISMATCH")
    if policy.parameter_group_policy != PARAMETER_GROUP_POLICY_STANDARD_NO_DECAY_V1:
        raise HimOptimizerExecutionPolicyError("PARAMETER_GROUP_POLICY_MISMATCH")
    if policy.no_decay_parameter_policy != NO_DECAY_PARAMETER_POLICY_V1:
        raise HimOptimizerExecutionPolicyError("NO_DECAY_POLICY_MISMATCH")
    if policy.zero_grad_policy != ZERO_GRAD_POLICY_SET_TO_NONE_V1:
        raise HimOptimizerExecutionPolicyError("ZERO_GRAD_POLICY_MISMATCH")
    if policy.gradient_accumulation_steps < 1:
        raise HimOptimizerExecutionPolicyError("GRADIENT_ACCUMULATION_INVALID")
    if policy.gradient_accumulation_loss_scaling != GRADIENT_ACCUMULATION_LOSS_SCALING_MEAN_V1:
        raise HimOptimizerExecutionPolicyError("GRADIENT_ACCUMULATION_SCALING_MISMATCH")
    if policy.gradient_clipping_policy != GRADIENT_CLIPPING_NONE_V1 or policy.gradient_clip_threshold is not None:
        raise HimOptimizerExecutionPolicyError("GRADIENT_CLIPPING_POLICY_MISMATCH")
    if policy.lr_scheduler_policy != LR_SCHEDULER_NONE_V1 or policy.warmup_policy != WARMUP_NONE_V1:
        raise HimOptimizerExecutionPolicyError("SCHEDULER_POLICY_MISMATCH")
    if policy.precision_policy != PRECISION_FP32_V1 or policy.amp_enabled or policy.grad_scaler_enabled:
        raise HimOptimizerExecutionPolicyError("PRECISION_POLICY_MISMATCH")
    if policy.device_policy not in {DEVICE_CPU_V1, DEVICE_CUDA_V1} or not policy.optimizer_step_train_split_only:
        raise HimOptimizerExecutionPolicyError("DEVICE_OR_SPLIT_POLICY_MISMATCH")
    if policy.logical_digest != _digest(policy.identity_payload()):
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_POLICY_DIGEST_MISMATCH")
    if policy.reference != f"him-optimizer-execution-policy:v1:{policy.logical_digest}":
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_POLICY_REFERENCE_MISMATCH")


def optimizer_execution_policy_wire_v1(policy: HimOptimizerExecutionPolicyV1) -> dict[str, object]:
    validate_him_optimizer_execution_policy_v1(policy)
    return {**policy.identity_payload(), "logicalDigest": policy.logical_digest, "reference": policy.reference}


def decode_him_optimizer_execution_policy_v1(value: object) -> HimOptimizerExecutionPolicyV1:
    if not isinstance(value, dict):
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_POLICY_OBJECT_REQUIRED")
    required = set(build_him_optimizer_execution_policy_v1().identity_payload()) | {"logicalDigest", "reference"}
    if set(value) != required:
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_POLICY_FIELDS_INVALID")
    try:
        policy = HimOptimizerExecutionPolicyV1(
            contract_id=value["contractId"], version=value["version"], state=value["state"],
            optimizer_id=value["optimizerId"], optimizer_type=value["optimizerType"],
            learning_rate=value["learningRate"], beta1=value["beta1"], beta2=value["beta2"],
            epsilon=value["epsilon"], weight_decay=value["weightDecay"],
            decoupled_weight_decay=value["decoupledWeightDecay"], bias_correction=value["biasCorrection"],
            amsgrad=value["amsgrad"], maximize=value["maximize"], foreach=value["foreach"],
            fused=value["fused"], capturable=value["capturable"], differentiable=value["differentiable"],
            parameter_group_policy=value["parameterGroupPolicy"], no_decay_parameter_policy=value["noDecayParameterPolicy"],
            zero_grad_policy=value["zeroGradPolicy"], gradient_accumulation_steps=value["gradientAccumulationSteps"],
            gradient_accumulation_loss_scaling=value["gradientAccumulationLossScaling"],
            gradient_clipping_policy=value["gradientClippingPolicy"], gradient_clip_threshold=value["gradientClipThreshold"],
            lr_scheduler_policy=value["lrSchedulerPolicy"], warmup_policy=value["warmupPolicy"],
            precision_policy=value["precisionPolicy"], amp_enabled=value["ampEnabled"],
            grad_scaler_enabled=value["gradScalerEnabled"], device_policy=value["devicePolicy"],
            optimizer_step_train_split_only=value["optimizerStepTrainSplitOnly"],
            logical_digest=value["logicalDigest"], reference=value["reference"],
        )
    except (KeyError, TypeError, ValueError) as error:
        raise HimOptimizerExecutionPolicyError("OPTIMIZER_POLICY_VALUES_INVALID") from error
    validate_him_optimizer_execution_policy_v1(policy)
    return policy
