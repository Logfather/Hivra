"""Production V3 prediction adapter for the frozen multi-head model."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import torch
from tokenizers import Tokenizer

from .point13_model_forward_v1 import (
    HimMultiHeadModelV1,
    PRIMARY_TARGET_KIND_ORDER_V1,
    SECONDARY_COMPATIBILITY_ORDER_V1,
)


@dataclass(frozen=True)
class V3PredictionV1:
    target_kind: str
    candidate_compatibility: str
    adapter_id: str = "HIM_V3_PREDICTION_ADAPTER_V1"

    def as_dict(self) -> dict[str, str]:
        return {"targetKind": self.target_kind, "candidateCompatibility": self.candidate_compatibility}


def predict_v3_v1(*, serialized_input: str, model: HimMultiHeadModelV1,
                  tokenizer: Tokenizer, checkpoint: Any | None = None) -> V3PredictionV1:
    """Run one deterministic, inference-only V3 prediction.

    ``checkpoint`` is accepted for the evaluator protocol; the caller supplies
    a model already restored from that checkpoint.  No external callback or
    model monkey-patching is involved.
    """
    if not isinstance(model, HimMultiHeadModelV1):
        raise TypeError("V3_MODEL_TYPE_INVALID")
    enc = tokenizer.encode(serialized_input, add_special_tokens=True)
    ids = enc.ids
    if not ids or len(ids) > 256:
        raise ValueError("V3_SEQUENCE_LENGTH_INVALID")
    device = model.execution_device
    input_ids = torch.tensor([ids], dtype=torch.int64, device=device)
    attention_mask = torch.ones((1, len(ids)), dtype=torch.int64, device=device)
    was_training = model.training
    model.eval()
    with torch.no_grad():
        output = model(input_ids, attention_mask)
        compatibility = SECONDARY_COMPATIBILITY_ORDER_V1[int(output.secondary_logits.argmax(dim=1).item())]
        # The frozen masking contract makes TARGET_KIND non-applicable for a
        # rejected candidate; otherwise decode the primary frozen class order.
        target = "NOT_APPLICABLE" if compatibility == "REJECT" else PRIMARY_TARGET_KIND_ORDER_V1[int(output.primary_logits.argmax(dim=1).item())]
    if was_training:
        model.train()
    return V3PredictionV1(target, compatibility)


__all__ = ["V3PredictionV1", "predict_v3_v1"]
