"""PyTorch execution for the Point-13 masked multi-objective loss.

The model remains responsible only for raw logits.  This module consumes
those logits and the already validated Point-12 supervision tensors.  It
performs no optimizer, backward, parameter update, persistence, or training
operation.
"""

from __future__ import annotations

from dataclasses import dataclass

import torch
from torch import Tensor
from torch.nn import functional as F

from .point13_loss_contract_v1 import (
    HimLossContractError,
    HimMaskedMultiObjectiveLossContractV1,
    validate_him_masked_multi_objective_loss_contract_v1,
)


class HimMaskedMultiObjectiveLossError(ValueError):
    """Raised when logits, supervision, device, or loss authority is invalid."""


@dataclass(frozen=True)
class HimMaskedMultiObjectiveLossResultV1:
    """Typed result retaining differentiable scalar and per-example tensors."""

    primary_raw_loss: Tensor
    secondary_raw_loss: Tensor
    primary_loss_index: Tensor
    secondary_loss_index: Tensor
    primary_active_count: int
    secondary_active_count: int
    primary_loss: Tensor
    secondary_loss: Tensor
    primary_weight: float
    secondary_weight: float
    total_loss: Tensor
    loss_contract_logical_digest: str


def compute_him_masked_multi_objective_loss_v1(
    *,
    primary_logits: Tensor,
    secondary_logits: Tensor,
    primary_target: Tensor,
    secondary_target: Tensor,
    primary_mask: Tensor,
    secondary_mask: Tensor,
    loss_contract: HimMaskedMultiObjectiveLossContractV1,
    selected_execution_device: torch.device | None = None,
) -> HimMaskedMultiObjectiveLossResultV1:
    """Compute two independently masked CE objectives and their weighted sum."""

    try:
        validate_him_masked_multi_objective_loss_contract_v1(loss_contract)
    except HimLossContractError as error:
        raise HimMaskedMultiObjectiveLossError(str(error)) from error

    tensors = (primary_logits, secondary_logits, primary_target, secondary_target, primary_mask, secondary_mask)
    if not all(isinstance(value, Tensor) for value in tensors):
        raise HimMaskedMultiObjectiveLossError("LOSS_TENSOR_TYPE_INVALID")
    if not tensors:
        raise HimMaskedMultiObjectiveLossError("LOSS_BATCH_EMPTY")
    devices = {value.device for value in tensors}
    if len(devices) != 1:
        raise HimMaskedMultiObjectiveLossError("LOSS_DEVICE_MISMATCH")
    if selected_execution_device is not None and primary_logits.device != selected_execution_device:
        raise HimMaskedMultiObjectiveLossError("LOSS_DEVICE_MISMATCH")
    if primary_logits.device != secondary_logits.device:
        raise HimMaskedMultiObjectiveLossError("LOSS_DEVICE_MISMATCH")
    if primary_logits.dtype != torch.float32 or secondary_logits.dtype != torch.float32:
        raise HimMaskedMultiObjectiveLossError("LOGITS_DTYPE_INVALID")
    if primary_logits.ndim != 2 or primary_logits.shape[1] != 5:
        raise HimMaskedMultiObjectiveLossError("PRIMARY_LOGITS_SHAPE_INVALID")
    if secondary_logits.ndim != 2 or secondary_logits.shape[1] != 2:
        raise HimMaskedMultiObjectiveLossError("SECONDARY_LOGITS_SHAPE_INVALID")
    batch_size = int(primary_logits.shape[0])
    if batch_size <= 0 or int(secondary_logits.shape[0]) != batch_size:
        raise HimMaskedMultiObjectiveLossError("LOGIT_BATCH_DIMENSION_MISMATCH")
    _validate_vector(primary_target, batch_size, torch.int64, "PRIMARY_TARGET")
    _validate_vector(secondary_target, batch_size, torch.int64, "SECONDARY_TARGET")
    _validate_vector(primary_mask, batch_size, torch.float32, "PRIMARY_MASK")
    _validate_vector(secondary_mask, batch_size, torch.float32, "SECONDARY_MASK")
    if not bool(torch.isfinite(primary_logits).all().item()) or not bool(torch.isfinite(secondary_logits).all().item()):
        raise HimMaskedMultiObjectiveLossError("LOGITS_NON_FINITE")
    _validate_binary_mask(primary_mask, "PRIMARY_MASK")
    _validate_binary_mask(secondary_mask, "SECONDARY_MASK")
    if bool(((primary_mask == 0.0) & (secondary_mask == 0.0)).any().item()):
        raise HimMaskedMultiObjectiveLossError("BOTH_OBJECTIVES_INACTIVE_FOR_EXAMPLE")
    if bool((primary_target < 1).any().item()) or bool((primary_target > 5).any().item()):
        raise HimMaskedMultiObjectiveLossError("PRIMARY_TARGET_OUT_OF_DOMAIN")
    if bool((secondary_target < 0).any().item()) or bool((secondary_target > 1).any().item()):
        raise HimMaskedMultiObjectiveLossError("SECONDARY_TARGET_OUT_OF_DOMAIN")

    # Mapping is deliberately non-mutating: Point-12 semantic code 1..5
    # becomes the CrossEntropy index 0..4 in a new INT64 tensor.
    primary_loss_index = primary_target - 1
    secondary_loss_index = secondary_target
    primary_raw_loss = F.cross_entropy(primary_logits, primary_loss_index, reduction="none")
    secondary_raw_loss = F.cross_entropy(secondary_logits, secondary_loss_index, reduction="none")
    if not bool(torch.isfinite(primary_raw_loss).all().item()) or not bool(torch.isfinite(secondary_raw_loss).all().item()):
        raise HimMaskedMultiObjectiveLossError("RAW_LOSS_NON_FINITE")

    primary_active_count = int(primary_mask.sum().item())
    secondary_active_count = int(secondary_mask.sum().item())
    primary_loss = _masked_active_mean(primary_raw_loss, primary_mask, primary_active_count)
    secondary_loss = _masked_active_mean(secondary_raw_loss, secondary_mask, secondary_active_count)
    primary_weight = float(loss_contract.primary_weight)
    secondary_weight = float(loss_contract.secondary_weight)
    total_loss = primary_loss * primary_weight + secondary_loss * secondary_weight
    if not bool(torch.isfinite(primary_loss).item()) or not bool(torch.isfinite(secondary_loss).item()) or not bool(torch.isfinite(total_loss).item()):
        raise HimMaskedMultiObjectiveLossError("LOSS_NON_FINITE")
    if bool((primary_loss < 0).item()) or bool((secondary_loss < 0).item()) or bool((total_loss < 0).item()):
        raise HimMaskedMultiObjectiveLossError("LOSS_NEGATIVE")
    return HimMaskedMultiObjectiveLossResultV1(
        primary_raw_loss=primary_raw_loss,
        secondary_raw_loss=secondary_raw_loss,
        primary_loss_index=primary_loss_index,
        secondary_loss_index=secondary_loss_index,
        primary_active_count=primary_active_count,
        secondary_active_count=secondary_active_count,
        primary_loss=primary_loss,
        secondary_loss=secondary_loss,
        primary_weight=primary_weight,
        secondary_weight=secondary_weight,
        total_loss=total_loss,
        loss_contract_logical_digest=loss_contract.logical_digest,
    )


def _validate_vector(value: Tensor, batch_size: int, dtype: torch.dtype, name: str) -> None:
    if value.ndim != 1 or int(value.shape[0]) != batch_size:
        raise HimMaskedMultiObjectiveLossError(f"{name}_SHAPE_INVALID")
    if value.dtype != dtype:
        raise HimMaskedMultiObjectiveLossError(f"{name}_DTYPE_INVALID")


def _validate_binary_mask(value: Tensor, name: str) -> None:
    if not bool(torch.isfinite(value).all().item()):
        raise HimMaskedMultiObjectiveLossError(f"{name}_NON_FINITE")
    if not bool(((value == 0.0) | (value == 1.0)).all().item()):
        raise HimMaskedMultiObjectiveLossError(f"{name}_NOT_BINARY")


def _masked_active_mean(raw_loss: Tensor, mask: Tensor, active_count: int) -> Tensor:
    if active_count == 0:
        # Preserve the graph to the corresponding head while contributing
        # exactly zero; no detached Python scalar is introduced.
        return raw_loss.sum() * 0.0
    return (raw_loss * mask).sum() / mask.sum()
