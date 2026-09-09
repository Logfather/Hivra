"""Isolated CPU train-forward RNG execution for Point 13.

Standard PyTorch Dropout remains in the model.  This runtime owns the
sequence by restoring its private CPU RNG state inside ``fork_rng`` for each
forward, then retaining the advanced state for the next forward.  The caller's
global RNG state is restored when the forward returns.
"""

from __future__ import annotations

from typing import Any

import torch
from torch import Tensor, nn

from .point13_forward_rng_contract_v1 import (
    HimTrainForwardRngContractV1,
    validate_him_train_forward_rng_contract_v1,
)


class HimTrainForwardRngError(ValueError):
    """Raised when train-forward RNG execution is not authorized."""


class HimTrainForwardRngV1:
    """One run-scoped, sequential CPU RNG stream for train-mode forwards."""

    def __init__(self, contract: HimTrainForwardRngContractV1) -> None:
        validate_him_train_forward_rng_contract_v1(contract)
        self.contract = contract
        generator = torch.Generator(device="cpu")
        generator.manual_seed(contract.derived_forward_seed)
        self._initial_state = generator.get_state().clone()
        self._state = self._initial_state.clone()
        self._forward_count = 0

    @property
    def forward_count(self) -> int:
        return self._forward_count

    @property
    def state(self) -> Tensor:
        """Return a defensive copy of the current runtime-only RNG state."""

        return self._state.clone()

    def reset(self) -> None:
        """Reset to the single run-start state, never automatically per batch."""

        self._state = self._initial_state.clone()
        self._forward_count = 0

    def restore_state(self, state: Tensor) -> None:
        if not isinstance(state, Tensor) or state.device.type != "cpu" or state.dtype != torch.uint8:
            raise HimTrainForwardRngError("FORWARD_RNG_STATE_INVALID")
        self._state = state.clone()

    def forward(
        self,
        model: nn.Module,
        input_ids: Tensor,
        attention_mask: Tensor,
    ) -> Any:
        """Execute one train-mode forward under the owned isolated stream."""

        if not isinstance(model, nn.Module) or not model.training:
            raise HimTrainForwardRngError("TRAIN_MODE_REQUIRED")
        if input_ids.device.type != "cpu" or attention_mask.device.type != "cpu":
            raise HimTrainForwardRngError("FORWARD_RNG_CPU_INPUTS_REQUIRED")
        with torch.random.fork_rng(devices=[]):
            torch.set_rng_state(self._state)
            with torch.no_grad():
                result = model(input_ids, attention_mask)
            self._state = torch.get_rng_state().clone()
        self._forward_count += 1
        return result


def initialize_him_train_forward_rng_v1(
    contract: HimTrainForwardRngContractV1,
) -> HimTrainForwardRngV1:
    """Initialize the forward stream exactly once for a run."""

    return HimTrainForwardRngV1(contract)
