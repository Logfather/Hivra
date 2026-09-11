"""Pinned, local-only XLM-R and Point-13 multi-head runtime for HIM.

This module defines the shared encoder representation and deterministic task
heads.  It deliberately does not define losses, optimization, training, or
checkpointing.
"""

from __future__ import annotations

import hashlib
import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Mapping

import torch
from safetensors.torch import safe_open
from torch import Tensor, nn

from .execution_device_v1 import (
    HimExecutionDeviceError,
    resolve_him_execution_device_v1,
)
from torch.nn import functional as F

from him_trainer.protocol_v1 import (
    HEAD_INITIALIZATION_BINDING_CONTRACT_ID_V1,
    HEAD_INITIALIZATION_BINDING_VERSION_V1,
    HEAD_INITIALIZATION_BIAS_V1,
    HEAD_INITIALIZATION_MEAN_V1,
    HEAD_INITIALIZATION_ORDER_V1 as PROTOCOL_HEAD_INITIALIZATION_ORDER_V1,
    HEAD_INITIALIZATION_RNG_V1,
    HEAD_INITIALIZATION_SCHEME_V1 as PROTOCOL_HEAD_INITIALIZATION_SCHEME_V1,
    HEAD_INITIALIZATION_STD_V1,
    ModelExecutionBinding,
    head_initialization_binding_logical_digest_v1,
    model_execution_binding_logical_digest_v1,
)


MODEL_REVISION = "e73636d4f797dec63c3081bb6ed5c7b0bb3f2089"
MODEL_ROOT = (
    Path(__file__).resolve().parents[2]
    / "models"
    / "xlm-roberta-base"
    / MODEL_REVISION
)
CONFIG_FILENAME = "config.json"
WEIGHTS_FILENAME = "model.safetensors"
CONFIG_SHA256 = "d66ed8cd4f2a93b358c245e50736fa389ed4f35c0bae7aad0b32abb20c62b579"
WEIGHTS_SHA256 = "6fd4797bc397c3b8b55d6bb5740366b57e6a3ce91c04c77f22aafc0c128e6feb"
TOKENIZER_SHA256 = "a898ea75433890f6610f4e470b8ebeb0c21dce5c8dd61f892eb09eb5919d2e2c"
POINT12_MAX_SEQUENCE_LENGTH = 128
EXPECTED_SOURCE_WEIGHT_DTYPE = torch.float32
UNUSED_SOURCE_WEIGHT_KEYS = frozenset(
    {
        "lm_head.bias",
        "lm_head.dense.bias",
        "lm_head.dense.weight",
        "lm_head.layer_norm.bias",
        "lm_head.layer_norm.weight",
        "roberta.pooler.dense.bias",
        "roberta.pooler.dense.weight",
    }
)


class HimModelArchitectureError(ValueError):
    """Raised when the pinned model configuration is not supported exactly."""


class HimModelLoadError(ValueError):
    """Raised when a pinned local model cannot be loaded fail-closed."""


class HimHeadContractError(ValueError):
    """Raised when the Point-13 task-head contract is invalid."""


@dataclass(frozen=True)
class HimXlmRobertaConfigV1:
    model_type: str
    hidden_size: int
    num_hidden_layers: int
    num_attention_heads: int
    intermediate_size: int
    vocab_size: int
    max_position_embeddings: int
    type_vocab_size: int
    pad_token_id: int
    bos_token_id: int
    eos_token_id: int
    hidden_dropout_prob: float
    attention_probs_dropout_prob: float
    layer_norm_eps: float
    hidden_act: str
    initializer_range: float

    @classmethod
    def from_mapping(cls, value: Mapping[str, Any]) -> "HimXlmRobertaConfigV1":
        required = {
            "model_type",
            "hidden_size",
            "num_hidden_layers",
            "num_attention_heads",
            "intermediate_size",
            "vocab_size",
            "max_position_embeddings",
            "type_vocab_size",
            "pad_token_id",
            "bos_token_id",
            "eos_token_id",
            "hidden_dropout_prob",
            "attention_probs_dropout_prob",
            "layer_norm_eps",
            "hidden_act",
            "initializer_range",
        }
        missing = sorted(required.difference(value))
        if missing:
            raise HimModelArchitectureError(f"CONFIG_REQUIRED_FIELD_MISSING:{','.join(missing)}")
        try:
            config = cls(
                model_type=str(value["model_type"]),
                hidden_size=int(value["hidden_size"]),
                num_hidden_layers=int(value["num_hidden_layers"]),
                num_attention_heads=int(value["num_attention_heads"]),
                intermediate_size=int(value["intermediate_size"]),
                vocab_size=int(value["vocab_size"]),
                max_position_embeddings=int(value["max_position_embeddings"]),
                type_vocab_size=int(value["type_vocab_size"]),
                pad_token_id=int(value["pad_token_id"]),
                bos_token_id=int(value["bos_token_id"]),
                eos_token_id=int(value["eos_token_id"]),
                hidden_dropout_prob=float(value["hidden_dropout_prob"]),
                attention_probs_dropout_prob=float(value["attention_probs_dropout_prob"]),
                layer_norm_eps=float(value["layer_norm_eps"]),
                hidden_act=str(value["hidden_act"]),
                initializer_range=float(value["initializer_range"]),
            )
        except (TypeError, ValueError, OverflowError) as error:
            raise HimModelArchitectureError("CONFIG_VALUE_INVALID") from error
        validate_model_config_v1(config)
        return config


@dataclass(frozen=True)
class HimEncoderOutputV1:
    last_hidden_state: Tensor
    representation: Tensor


PRIMARY_TARGET_KIND_ORDER_V1 = (
    "EXISTING_CANONICAL",
    "IDENTITY",
    "VARIANT",
    "ALIAS",
    "NEW_CANONICAL",
)
PRIMARY_TARGET_KIND_CODE_BY_NAME_V1 = {
    "EXISTING_CANONICAL": 1,
    "IDENTITY": 2,
    "VARIANT": 3,
    "ALIAS": 4,
    "NEW_CANONICAL": 5,
}
SECONDARY_COMPATIBILITY_ORDER_V1 = ("COMPATIBLE", "REJECT")
SECONDARY_COMPATIBILITY_CODE_BY_NAME_V1 = {"COMPATIBLE": 0, "REJECT": 1}
HIM_MULTI_HEAD_CONTRACT_ID_V1 = "HIM_POINT_13_MULTI_HEAD_MODEL_AND_INITIALIZATION_CONTRACT_V1"
HIM_MULTI_HEAD_CONTRACT_VERSION_V1 = 1
HEAD_INITIALIZATION_SEED_SOURCE_V1 = "training-configuration.seed"
HEAD_INITIALIZATION_SCHEME_V1 = "NORMAL_ZERO_BIAS"
HEAD_INITIALIZATION_ORDER_V1 = ("PRIMARY", "SECONDARY")
HEAD_DROPOUT_PROBABILITY_V1 = 0.0


@dataclass(frozen=True)
class HimMultiHeadContractV1:
    """Deterministic authority for the two Point-13 task heads."""

    representation_width: int
    primary_class_order: tuple[str, ...]
    primary_target_codes: tuple[int, ...]
    secondary_class_order: tuple[str, ...]
    secondary_target_codes: tuple[int, ...]
    primary_bias: bool
    secondary_bias: bool
    head_dropout_probability: float
    initialization_scheme: str
    initializer_range: float
    initialization_seed: int
    initialization_order: tuple[str, ...]
    contract_id: str = HIM_MULTI_HEAD_CONTRACT_ID_V1
    version: int = HIM_MULTI_HEAD_CONTRACT_VERSION_V1

    @property
    def primary_class_count(self) -> int:
        return len(self.primary_class_order)

    @property
    def secondary_class_count(self) -> int:
        return len(self.secondary_class_order)

    def identity_payload(self) -> dict[str, object]:
        return {
            "contractId": self.contract_id,
            "version": self.version,
            "representationWidth": self.representation_width,
            "primaryClassOrder": list(self.primary_class_order),
            "primaryTargetCodes": list(self.primary_target_codes),
            "secondaryClassOrder": list(self.secondary_class_order),
            "secondaryTargetCodes": list(self.secondary_target_codes),
            "primaryBias": self.primary_bias,
            "secondaryBias": self.secondary_bias,
            "headDropoutProbability": self.head_dropout_probability,
            "initializationScheme": self.initialization_scheme,
            "initializerRange": self.initializer_range,
            "initializationSeed": self.initialization_seed,
            "initializationSeedSource": HEAD_INITIALIZATION_SEED_SOURCE_V1,
            "initializationOrder": list(self.initialization_order),
        }

    @property
    def logical_digest(self) -> str:
        encoded = json.dumps(self.identity_payload(), ensure_ascii=False, separators=(",", ":"), sort_keys=True)
        return hashlib.sha256(encoded.encode("utf-8")).hexdigest()

    @property
    def reference(self) -> str:
        return f"him-head-architecture:v1:{self.logical_digest}"


@dataclass(frozen=True)
class HimMultiHeadForwardOutputV1:
    encoder_output: HimEncoderOutputV1
    representation: Tensor
    primary_logits: Tensor
    secondary_logits: Tensor


def build_him_multi_head_contract_v1(config: HimXlmRobertaConfigV1, initialization_seed: int) -> HimMultiHeadContractV1:
    """Build the exact V1 head authority from the pinned encoder config."""

    validate_model_config_v1(config)
    if isinstance(initialization_seed, bool) or not isinstance(initialization_seed, int) or initialization_seed < 0:
        raise HimHeadContractError("HEAD_INITIALIZATION_SEED_INVALID")
    contract = HimMultiHeadContractV1(
        representation_width=config.hidden_size,
        primary_class_order=PRIMARY_TARGET_KIND_ORDER_V1,
        primary_target_codes=tuple(PRIMARY_TARGET_KIND_CODE_BY_NAME_V1[name] for name in PRIMARY_TARGET_KIND_ORDER_V1),
        secondary_class_order=SECONDARY_COMPATIBILITY_ORDER_V1,
        secondary_target_codes=tuple(SECONDARY_COMPATIBILITY_CODE_BY_NAME_V1[name] for name in SECONDARY_COMPATIBILITY_ORDER_V1),
        primary_bias=True,
        secondary_bias=True,
        head_dropout_probability=HEAD_DROPOUT_PROBABILITY_V1,
        initialization_scheme=HEAD_INITIALIZATION_SCHEME_V1,
        initializer_range=config.initializer_range,
        initialization_seed=initialization_seed,
        initialization_order=HEAD_INITIALIZATION_ORDER_V1,
    )
    validate_him_multi_head_contract_v1(contract, config)
    return contract


def validate_him_multi_head_contract_v1(contract: HimMultiHeadContractV1, config: HimXlmRobertaConfigV1) -> None:
    """Fail closed on class-domain, architecture, or initialization drift."""

    validate_model_config_v1(config)
    if contract.contract_id != HIM_MULTI_HEAD_CONTRACT_ID_V1 or contract.version != HIM_MULTI_HEAD_CONTRACT_VERSION_V1:
        raise HimHeadContractError("HEAD_CONTRACT_ID_OR_VERSION_INVALID")
    if contract.representation_width != config.hidden_size:
        raise HimHeadContractError("HEAD_REPRESENTATION_WIDTH_MISMATCH")
    if contract.primary_class_order != PRIMARY_TARGET_KIND_ORDER_V1:
        raise HimHeadContractError("PRIMARY_CLASS_ORDER_INVALID")
    if contract.primary_target_codes != tuple(PRIMARY_TARGET_KIND_CODE_BY_NAME_V1[name] for name in PRIMARY_TARGET_KIND_ORDER_V1):
        raise HimHeadContractError("PRIMARY_TARGET_CODE_ORDER_INVALID")
    if contract.secondary_class_order != SECONDARY_COMPATIBILITY_ORDER_V1:
        raise HimHeadContractError("SECONDARY_CLASS_ORDER_INVALID")
    if contract.secondary_target_codes != tuple(SECONDARY_COMPATIBILITY_CODE_BY_NAME_V1[name] for name in SECONDARY_COMPATIBILITY_ORDER_V1):
        raise HimHeadContractError("SECONDARY_TARGET_CODE_ORDER_INVALID")
    if not contract.primary_bias or not contract.secondary_bias:
        raise HimHeadContractError("HEAD_BIAS_POLICY_INVALID")
    if contract.head_dropout_probability != HEAD_DROPOUT_PROBABILITY_V1:
        raise HimHeadContractError("HEAD_DROPOUT_POLICY_INVALID")
    if contract.initialization_scheme != HEAD_INITIALIZATION_SCHEME_V1:
        raise HimHeadContractError("HEAD_INITIALIZATION_SCHEME_INVALID")
    if contract.initializer_range != config.initializer_range:
        raise HimHeadContractError("HEAD_INITIALIZER_RANGE_MISMATCH")
    if isinstance(contract.initialization_seed, bool) or contract.initialization_seed < 0:
        raise HimHeadContractError("HEAD_INITIALIZATION_SEED_INVALID")
    if contract.initialization_order != HEAD_INITIALIZATION_ORDER_V1:
        raise HimHeadContractError("HEAD_INITIALIZATION_ORDER_INVALID")


def primary_logit_index_v1(target_kind: str) -> int:
    try:
        return PRIMARY_TARGET_KIND_ORDER_V1.index(target_kind)
    except ValueError as error:
        raise HimHeadContractError("PRIMARY_TARGET_KIND_UNSUPPORTED") from error


def secondary_logit_index_v1(compatibility: str) -> int:
    try:
        return SECONDARY_COMPATIBILITY_ORDER_V1.index(compatibility)
    except ValueError as error:
        raise HimHeadContractError("SECONDARY_COMPATIBILITY_UNSUPPORTED") from error


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _require_file(path: Path, code: str) -> None:
    if not path.is_file() or path.is_symlink():
        raise HimModelLoadError(code)


def _require_digest(path: Path, expected: str, code: str) -> None:
    if _sha256(path) != expected:
        raise HimModelLoadError(code)


def validate_model_config_v1(config: HimXlmRobertaConfigV1) -> None:
    if config.model_type != "xlm-roberta":
        raise HimModelArchitectureError("MODEL_TYPE_INVALID")
    positive = (
        config.hidden_size,
        config.num_hidden_layers,
        config.num_attention_heads,
        config.intermediate_size,
        config.vocab_size,
        config.max_position_embeddings,
        config.type_vocab_size,
    )
    if any(value <= 0 for value in positive):
        raise HimModelArchitectureError("CONFIG_DIMENSION_INVALID")
    if config.hidden_size % config.num_attention_heads != 0:
        raise HimModelArchitectureError("ATTENTION_HEAD_DIVISIBILITY_INVALID")
    if not 0.0 <= config.hidden_dropout_prob < 1.0:
        raise HimModelArchitectureError("HIDDEN_DROPOUT_INVALID")
    if not 0.0 <= config.attention_probs_dropout_prob < 1.0:
        raise HimModelArchitectureError("ATTENTION_DROPOUT_INVALID")
    if config.layer_norm_eps <= 0.0 or config.initializer_range <= 0.0:
        raise HimModelArchitectureError("NUMERICAL_CONFIG_INVALID")
    if config.hidden_act != "gelu":
        raise HimModelArchitectureError("HIDDEN_ACT_UNSUPPORTED")
    if config.type_vocab_size != 1:
        raise HimModelArchitectureError("TYPE_VOCAB_SIZE_UNSUPPORTED")
    if config.pad_token_id < 0 or config.pad_token_id >= config.vocab_size:
        raise HimModelArchitectureError("PAD_TOKEN_ID_INVALID")
    if config.bos_token_id < 0 or config.eos_token_id < 0:
        raise HimModelArchitectureError("SPECIAL_TOKEN_ID_INVALID")
    if config.pad_token_id >= config.max_position_embeddings:
        raise HimModelArchitectureError("PAD_POSITION_ID_INVALID")
    if POINT12_MAX_SEQUENCE_LENGTH + config.pad_token_id + 1 > config.max_position_embeddings:
        raise HimModelArchitectureError("POINT12_POSITION_CAPACITY_INVALID")


def load_pinned_model_config_v1() -> HimXlmRobertaConfigV1:
    """Read the only authorized local config after its digest gate."""

    config_path = MODEL_ROOT / CONFIG_FILENAME
    _require_file(config_path, "CONFIG_ARTIFACT_INVALID")
    _require_digest(config_path, CONFIG_SHA256, "CONFIG_DIGEST_MISMATCH")
    try:
        with config_path.open("r", encoding="utf-8") as handle:
            value = json.load(handle)
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as error:
        raise HimModelLoadError("CONFIG_PARSE_FAILED") from error
    if not isinstance(value, dict):
        raise HimModelLoadError("CONFIG_ROOT_INVALID")
    try:
        return HimXlmRobertaConfigV1.from_mapping(value)
    except HimModelArchitectureError as error:
        raise HimModelLoadError(str(error)) from error


def create_xlm_roberta_position_ids_v1(input_ids: Tensor, padding_idx: int) -> Tensor:
    """Create XLM-R/RoBERTa padding-aware positions, not naive 0..L-1."""

    if not isinstance(input_ids, Tensor) or input_ids.ndim != 2 or input_ids.dtype != torch.int64:
        raise HimModelArchitectureError("POSITION_INPUT_INVALID")
    if padding_idx < 0:
        raise HimModelArchitectureError("POSITION_PADDING_INDEX_INVALID")
    mask = input_ids.ne(padding_idx).to(dtype=torch.int64)
    incremental = torch.cumsum(mask, dim=1) * mask
    return incremental + padding_idx


def validate_attention_mask_v1(attention_mask: Tensor, input_ids: Tensor) -> None:
    if not isinstance(attention_mask, Tensor) or attention_mask.dtype != torch.int64:
        raise HimModelArchitectureError("ATTENTION_MASK_DTYPE_INVALID")
    if not isinstance(input_ids, Tensor) or input_ids.ndim != 2:
        raise HimModelArchitectureError("INPUT_IDS_SHAPE_INVALID")
    if attention_mask.ndim != 2 or tuple(attention_mask.shape) != tuple(input_ids.shape):
        raise HimModelArchitectureError("ATTENTION_MASK_SHAPE_INVALID")
    if not bool(torch.all((attention_mask == 0) | (attention_mask == 1)).item()):
        raise HimModelArchitectureError("ATTENTION_MASK_BINARY_INVALID")
    if not bool(torch.all(attention_mask.sum(dim=1) > 0).item()):
        raise HimModelArchitectureError("ATTENTION_MASK_ALL_ZERO")


def additive_attention_mask_v1(attention_mask: Tensor, dtype: torch.dtype) -> Tensor:
    if dtype not in (torch.float32, torch.float64):
        raise HimModelArchitectureError("ATTENTION_MASK_DTYPE_UNSUPPORTED")
    return torch.where(
        attention_mask[:, None, None, :] == 1,
        torch.zeros((), dtype=dtype, device=attention_mask.device),
        torch.full((), torch.finfo(dtype).min, dtype=dtype, device=attention_mask.device),
    )


class HimXlmRobertaEmbeddingsV1(nn.Module):
    def __init__(self, config: HimXlmRobertaConfigV1) -> None:
        super().__init__()
        self.word_embeddings = nn.Embedding(config.vocab_size, config.hidden_size, padding_idx=config.pad_token_id)
        self.position_embeddings = nn.Embedding(config.max_position_embeddings, config.hidden_size, padding_idx=config.pad_token_id)
        self.token_type_embeddings = nn.Embedding(config.type_vocab_size, config.hidden_size)
        self.LayerNorm = nn.LayerNorm(config.hidden_size, eps=config.layer_norm_eps)
        self.dropout = nn.Dropout(config.hidden_dropout_prob)
        self.padding_idx = config.pad_token_id

    def forward(self, input_ids: Tensor) -> Tensor:
        position_ids = create_xlm_roberta_position_ids_v1(input_ids, self.padding_idx)
        token_type_ids = torch.zeros_like(input_ids)
        embeddings = self.word_embeddings(input_ids)
        embeddings = embeddings + self.position_embeddings(position_ids)
        embeddings = embeddings + self.token_type_embeddings(token_type_ids)
        embeddings = self.LayerNorm(embeddings)
        return self.dropout(embeddings)


class HimXlmRobertaSelfAttentionV1(nn.Module):
    def __init__(self, config: HimXlmRobertaConfigV1) -> None:
        super().__init__()
        self.num_attention_heads = config.num_attention_heads
        self.attention_head_size = config.hidden_size // config.num_attention_heads
        self.all_head_size = config.hidden_size
        self.query = nn.Linear(config.hidden_size, config.hidden_size)
        self.key = nn.Linear(config.hidden_size, config.hidden_size)
        self.value = nn.Linear(config.hidden_size, config.hidden_size)
        self.dropout = nn.Dropout(config.attention_probs_dropout_prob)

    def transpose_for_scores(self, value: Tensor) -> Tensor:
        shape = value.size()[:-1] + (self.num_attention_heads, self.attention_head_size)
        return value.view(*shape).permute(0, 2, 1, 3)

    def forward(self, hidden_states: Tensor, additive_mask: Tensor) -> Tensor:
        query_layer = self.transpose_for_scores(self.query(hidden_states))
        key_layer = self.transpose_for_scores(self.key(hidden_states))
        value_layer = self.transpose_for_scores(self.value(hidden_states))
        scores = torch.matmul(query_layer, key_layer.transpose(-1, -2))
        scores = scores / math.sqrt(self.attention_head_size)
        scores = scores + additive_mask
        probabilities = F.softmax(scores, dim=-1)
        probabilities = self.dropout(probabilities)
        context = torch.matmul(probabilities, value_layer)
        context = context.permute(0, 2, 1, 3).contiguous()
        return context.view(context.size(0), context.size(1), self.all_head_size)


class HimXlmRobertaSelfOutputV1(nn.Module):
    def __init__(self, config: HimXlmRobertaConfigV1) -> None:
        super().__init__()
        self.dense = nn.Linear(config.hidden_size, config.hidden_size)
        self.dropout = nn.Dropout(config.hidden_dropout_prob)
        self.LayerNorm = nn.LayerNorm(config.hidden_size, eps=config.layer_norm_eps)

    def forward(self, hidden_states: Tensor, input_tensor: Tensor) -> Tensor:
        hidden_states = self.dense(hidden_states)
        hidden_states = self.dropout(hidden_states)
        return self.LayerNorm(hidden_states + input_tensor)


class HimXlmRobertaAttentionV1(nn.Module):
    def __init__(self, config: HimXlmRobertaConfigV1) -> None:
        super().__init__()
        self.self = HimXlmRobertaSelfAttentionV1(config)
        self.output = HimXlmRobertaSelfOutputV1(config)

    def forward(self, hidden_states: Tensor, additive_mask: Tensor) -> Tensor:
        self_output = self.self(hidden_states, additive_mask)
        return self.output(self_output, hidden_states)


class HimXlmRobertaIntermediateV1(nn.Module):
    def __init__(self, config: HimXlmRobertaConfigV1) -> None:
        super().__init__()
        self.dense = nn.Linear(config.hidden_size, config.intermediate_size)

    def forward(self, hidden_states: Tensor) -> Tensor:
        return F.gelu(self.dense(hidden_states), approximate="none")


class HimXlmRobertaOutputV1(nn.Module):
    def __init__(self, config: HimXlmRobertaConfigV1) -> None:
        super().__init__()
        self.dense = nn.Linear(config.intermediate_size, config.hidden_size)
        self.dropout = nn.Dropout(config.hidden_dropout_prob)
        self.LayerNorm = nn.LayerNorm(config.hidden_size, eps=config.layer_norm_eps)

    def forward(self, hidden_states: Tensor, input_tensor: Tensor) -> Tensor:
        hidden_states = self.dense(hidden_states)
        hidden_states = self.dropout(hidden_states)
        return self.LayerNorm(hidden_states + input_tensor)


class HimXlmRobertaLayerV1(nn.Module):
    def __init__(self, config: HimXlmRobertaConfigV1) -> None:
        super().__init__()
        self.attention = HimXlmRobertaAttentionV1(config)
        self.intermediate = HimXlmRobertaIntermediateV1(config)
        self.output = HimXlmRobertaOutputV1(config)

    def forward(self, hidden_states: Tensor, additive_mask: Tensor) -> Tensor:
        attention_output = self.attention(hidden_states, additive_mask)
        intermediate_output = self.intermediate(attention_output)
        return self.output(intermediate_output, attention_output)


class HimXlmRobertaEncoderV1(nn.Module):
    def __init__(self, config: HimXlmRobertaConfigV1) -> None:
        super().__init__()
        self.layer = nn.ModuleList(HimXlmRobertaLayerV1(config) for _ in range(config.num_hidden_layers))

    def forward(self, hidden_states: Tensor, additive_mask: Tensor) -> Tensor:
        for layer_module in self.layer:
            hidden_states = layer_module(hidden_states, additive_mask)
        return hidden_states


class HimXlmRobertaBackboneV1(nn.Module):
    def __init__(self, config: HimXlmRobertaConfigV1) -> None:
        super().__init__()
        self.embeddings = HimXlmRobertaEmbeddingsV1(config)
        self.encoder = HimXlmRobertaEncoderV1(config)

    def forward(self, input_ids: Tensor, additive_mask: Tensor) -> Tensor:
        return self.encoder(self.embeddings(input_ids), additive_mask)


class HimXlmRobertaBaseModelV1(nn.Module):
    """HIM base wrapper: exactly input_ids + attention_mask, no task heads."""

    def __init__(self, config: HimXlmRobertaConfigV1, execution_device: str | torch.device = "cpu") -> None:
        super().__init__()
        self.config = config
        self.execution_device = _resolve_model_execution_device(execution_device)
        self.roberta = HimXlmRobertaBackboneV1(config)

    def forward(self, input_ids: Tensor, attention_mask: Tensor) -> HimEncoderOutputV1:
        _validate_model_inputs(input_ids, attention_mask, self.config, self.execution_device)
        additive_mask = additive_attention_mask_v1(attention_mask, self.roberta.embeddings.word_embeddings.weight.dtype)
        last_hidden_state = self.roberta(input_ids, additive_mask)
        representation = last_hidden_state[:, 0, :]
        return HimEncoderOutputV1(last_hidden_state=last_hidden_state, representation=representation)


class HimPrimaryTargetKindHeadV1(nn.Module):
    """Independent 768-to-5 raw-logit target-kind classifier."""

    def __init__(self, contract: HimMultiHeadContractV1) -> None:
        super().__init__()
        self.classifier = _new_linear_without_global_rng(
            contract.representation_width,
            contract.primary_class_count,
            bias=contract.primary_bias,
        )

    def forward(self, representation: Tensor) -> Tensor:
        return self.classifier(representation)


class HimSecondaryCompatibilityHeadV1(nn.Module):
    """Independent 768-to-2 raw-logit compatibility classifier."""

    def __init__(self, contract: HimMultiHeadContractV1) -> None:
        super().__init__()
        self.classifier = _new_linear_without_global_rng(
            contract.representation_width,
            contract.secondary_class_count,
            bias=contract.secondary_bias,
        )

    def forward(self, representation: Tensor) -> Tensor:
        return self.classifier(representation)


def _new_linear_without_global_rng(input_width: int, output_width: int, *, bias: bool) -> nn.Linear:
    """Construct a Linear without leaking its framework-default RNG draw."""

    with torch.random.fork_rng(devices=[]):
        return nn.Linear(input_width, output_width, bias=bias)


def _initialize_head_linear_v1(linear: nn.Linear, generator: torch.Generator, initializer_range: float) -> None:
    if linear.weight.device.type != "cpu" or linear.weight.dtype != torch.float32:
        raise HimHeadContractError("HEAD_PARAMETER_DEVICE_OR_DTYPE_INVALID")
    with torch.no_grad():
        weight = torch.randn(
            linear.weight.shape,
            generator=generator,
            dtype=linear.weight.dtype,
            device=linear.weight.device,
        ) * initializer_range
        linear.weight.copy_(weight)
        if linear.bias is None:
            raise HimHeadContractError("HEAD_BIAS_MISSING")
        linear.bias.zero_()


def initialize_him_heads_v1(
    primary_head: HimPrimaryTargetKindHeadV1,
    secondary_head: HimSecondaryCompatibilityHeadV1,
    contract: HimMultiHeadContractV1,
    config: HimXlmRobertaConfigV1,
) -> None:
    """Initialize only task heads with an isolated, explicit CPU generator."""

    validate_him_multi_head_contract_v1(contract, config)
    generator = torch.Generator(device="cpu")
    generator.manual_seed(contract.initialization_seed)
    heads = {"PRIMARY": primary_head.classifier, "SECONDARY": secondary_head.classifier}
    for name in contract.initialization_order:
        _initialize_head_linear_v1(heads[name], generator, contract.initializer_range)


class HimMultiHeadModelV1(nn.Module):
    """Pinned XLM-R encoder plus the two independent Point-13 task heads."""

    def __init__(
        self,
        base_model: HimXlmRobertaBaseModelV1,
        contract: HimMultiHeadContractV1,
        execution_binding: ModelExecutionBinding,
        expected_base_model_binding_logical_digest: str,
        expected_training_configuration_seed: int,
        execution_device: str | torch.device = "cpu",
    ) -> None:
        super().__init__()
        if not isinstance(base_model, HimXlmRobertaBaseModelV1):
            raise HimHeadContractError("BASE_MODEL_TYPE_INVALID")
        validate_him_multi_head_contract_v1(contract, base_model.config)
        if any(parameter.device.type != "cpu" or parameter.dtype != torch.float32 for parameter in base_model.parameters()):
            raise HimHeadContractError("BASE_MODEL_DEVICE_OR_DTYPE_INVALID")
        validate_him_model_execution_binding_v1(
            execution_binding,
            base_model.config,
            contract,
            expected_base_model_binding_logical_digest,
            expected_training_configuration_seed,
        )
        self.base_model = base_model
        self.execution_device = _resolve_model_execution_device(execution_device)
        self.base_model.execution_device = self.execution_device
        self.contract = contract
        self.execution_binding = execution_binding
        self.primary_head = HimPrimaryTargetKindHeadV1(contract)
        self.secondary_head = HimSecondaryCompatibilityHeadV1(contract)
        initialize_him_heads_v1(self.primary_head, self.secondary_head, contract, base_model.config)
        if self.execution_device != torch.device("cpu"):
            self.to(self.execution_device)
        if any(parameter.device != self.execution_device or parameter.dtype != torch.float32 for parameter in self.parameters()):
            raise HimHeadContractError("MODEL_PARAMETER_DEVICE_OR_DTYPE_INVALID")

    def forward(self, input_ids: Tensor, attention_mask: Tensor) -> HimMultiHeadForwardOutputV1:
        encoder_output = self.base_model(input_ids, attention_mask)
        representation = encoder_output.representation
        validate_shared_representation_v1(representation, self.contract, self.execution_device)
        primary_logits = self.primary_head(representation)
        secondary_logits = self.secondary_head(representation)
        if tuple(primary_logits.shape) != (representation.shape[0], self.contract.primary_class_count):
            raise HimHeadContractError("PRIMARY_LOGITS_SHAPE_INVALID")
        if tuple(secondary_logits.shape) != (representation.shape[0], self.contract.secondary_class_count):
            raise HimHeadContractError("SECONDARY_LOGITS_SHAPE_INVALID")
        if primary_logits.dtype != representation.dtype or secondary_logits.dtype != representation.dtype:
            raise HimHeadContractError("HEAD_LOGITS_DTYPE_INVALID")
        if primary_logits.device != self.execution_device or secondary_logits.device != self.execution_device:
            raise HimHeadContractError("HEAD_LOGITS_DEVICE_INVALID")
        return HimMultiHeadForwardOutputV1(
            encoder_output=encoder_output,
            representation=representation,
            primary_logits=primary_logits,
            secondary_logits=secondary_logits,
        )


def validate_shared_representation_v1(
    representation: Tensor,
    contract: HimMultiHeadContractV1,
    execution_device: torch.device | None = None,
) -> None:
    if not isinstance(representation, Tensor) or representation.ndim != 2:
        raise HimHeadContractError("SHARED_REPRESENTATION_RANK_INVALID")
    if representation.shape[0] == 0:
        raise HimHeadContractError("SHARED_REPRESENTATION_BATCH_EMPTY")
    if representation.shape[1] != contract.representation_width:
        raise HimHeadContractError("SHARED_REPRESENTATION_WIDTH_INVALID")
    if representation.dtype != torch.float32 or (execution_device is not None and representation.device != execution_device):
        raise HimHeadContractError("SHARED_REPRESENTATION_DEVICE_OR_DTYPE_INVALID")


def build_him_multi_head_model_v1(
    base_model: HimXlmRobertaBaseModelV1,
    execution_binding: ModelExecutionBinding,
    expected_base_model_binding_logical_digest: str,
    expected_training_configuration_seed: int,
    execution_device: str | torch.device = "cpu",
) -> HimMultiHeadModelV1:
    contract = build_him_multi_head_contract_v1(
        base_model.config,
        execution_binding.head_initialization_binding.seed,
    )
    return HimMultiHeadModelV1(base_model, contract, execution_binding, expected_base_model_binding_logical_digest, expected_training_configuration_seed, execution_device)


def load_pinned_him_multi_head_model_v1(
    execution_binding: ModelExecutionBinding,
    expected_base_model_binding_logical_digest: str,
    expected_training_configuration_seed: int,
    execution_device: str | torch.device = "cpu",
) -> HimMultiHeadModelV1:
    """Load the pinned base only under an explicit model-execution authority."""

    base_model = load_pinned_him_xlm_roberta_base_v1()
    model = build_him_multi_head_model_v1(base_model, execution_binding, expected_base_model_binding_logical_digest, expected_training_configuration_seed, execution_device)
    model.eval()
    return model


def full_him_initial_state_logical_digest_v1(
    contract: HimMultiHeadContractV1,
    *,
    config_digest: str = CONFIG_SHA256,
    weights_digest: str = WEIGHTS_SHA256,
    tokenizer_digest: str = TOKENIZER_SHA256,
) -> str:
    """Identity for the pinned base plus deterministic task-head authority."""

    payload = {
        "baseConfigDigest": config_digest,
        "baseWeightsDigest": weights_digest,
        "tokenizerDigest": tokenizer_digest,
        "headContractDigest": contract.logical_digest,
    }
    encoded = json.dumps(payload, separators=(",", ":"), sort_keys=True)
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()


def build_him_model_execution_binding_v1(
    config: HimXlmRobertaConfigV1,
    initialization_seed: int,
    base_model_binding_logical_digest: str,
) -> ModelExecutionBinding:
    """Create the single explicit authority required before model construction."""

    contract = build_him_multi_head_contract_v1(config, initialization_seed)
    initialization_digest = head_initialization_binding_logical_digest_v1(
        PROTOCOL_HEAD_INITIALIZATION_SCHEME_V1,
        HEAD_INITIALIZATION_MEAN_V1,
        f"{config.initializer_range:.2f}",
        HEAD_INITIALIZATION_BIAS_V1,
        initialization_seed,
        PROTOCOL_HEAD_INITIALIZATION_ORDER_V1,
        HEAD_INITIALIZATION_RNG_V1,
    )
    initialization_reference = f"him-head-initialization:v1:{initialization_digest}"
    full_digest = full_him_initial_state_logical_digest_v1(contract)
    full_reference = f"him-full-initial-state:v1:{full_digest}"
    logical_digest = model_execution_binding_logical_digest_v1(
        base_model_binding_logical_digest,
        contract.logical_digest,
        contract.reference,
        initialization_digest,
        initialization_reference,
        full_digest,
        full_reference,
    )
    from him_trainer.protocol_v1 import HeadInitializationBinding

    initialization = HeadInitializationBinding(
        contract_id=HEAD_INITIALIZATION_BINDING_CONTRACT_ID_V1,
        version=HEAD_INITIALIZATION_BINDING_VERSION_V1,
        initialization_scheme=PROTOCOL_HEAD_INITIALIZATION_SCHEME_V1,
        mean=HEAD_INITIALIZATION_MEAN_V1,
        std=f"{config.initializer_range:.2f}",
        bias=HEAD_INITIALIZATION_BIAS_V1,
        seed=initialization_seed,
        initialization_order=PROTOCOL_HEAD_INITIALIZATION_ORDER_V1,
        rng=HEAD_INITIALIZATION_RNG_V1,
        logical_digest=initialization_digest,
        reference=initialization_reference,
    )
    return ModelExecutionBinding(
        contract_id="HIM_MODEL_EXECUTION_BINDING_V1",
        version="1",
        base_model_binding_logical_digest=base_model_binding_logical_digest,
        head_contract_logical_digest=contract.logical_digest,
        head_contract_reference=contract.reference,
        head_initialization_binding=initialization,
        full_initial_state_logical_digest=full_digest,
        full_initial_state_reference=full_reference,
        logical_digest=logical_digest,
        reference=f"him-model-execution-binding:v1:{logical_digest}",
    )


def validate_him_model_execution_binding_v1(
    binding: ModelExecutionBinding,
    config: HimXlmRobertaConfigV1,
    contract: HimMultiHeadContractV1,
    expected_base_model_binding_logical_digest: str,
    expected_training_configuration_seed: int,
) -> None:
    """Cross-validate wire authority against the frozen local runtime contract."""

    if not isinstance(binding, ModelExecutionBinding):
        raise HimHeadContractError("MODEL_EXECUTION_BINDING_TYPE_INVALID")
    validate_him_multi_head_contract_v1(contract, config)
    if binding.base_model_binding_logical_digest != expected_base_model_binding_logical_digest:
        raise HimHeadContractError("MODEL_EXECUTION_BASE_BINDING_MISMATCH")
    if binding.head_contract_logical_digest != contract.logical_digest or binding.head_contract_reference != contract.reference:
        raise HimHeadContractError("MODEL_EXECUTION_HEAD_BINDING_MISMATCH")
    initialization = binding.head_initialization_binding
    if (
        initialization.contract_id != HEAD_INITIALIZATION_BINDING_CONTRACT_ID_V1
        or initialization.version != HEAD_INITIALIZATION_BINDING_VERSION_V1
        or initialization.initialization_scheme != PROTOCOL_HEAD_INITIALIZATION_SCHEME_V1
        or initialization.mean != HEAD_INITIALIZATION_MEAN_V1
        or initialization.std != f"{config.initializer_range:.2f}"
        or initialization.bias != HEAD_INITIALIZATION_BIAS_V1
        or initialization.initialization_order != PROTOCOL_HEAD_INITIALIZATION_ORDER_V1
        or initialization.rng != HEAD_INITIALIZATION_RNG_V1
        or initialization.seed != contract.initialization_seed
        or initialization.seed != expected_training_configuration_seed
    ):
        raise HimHeadContractError("MODEL_EXECUTION_INITIALIZATION_BINDING_MISMATCH")
    expected_initialization_digest = head_initialization_binding_logical_digest_v1(
        initialization.initialization_scheme,
        initialization.mean,
        initialization.std,
        initialization.bias,
        initialization.seed,
        initialization.initialization_order,
        initialization.rng,
    )
    if initialization.logical_digest != expected_initialization_digest or initialization.reference != f"him-head-initialization:v1:{expected_initialization_digest}":
        raise HimHeadContractError("MODEL_EXECUTION_INITIALIZATION_DIGEST_MISMATCH")
    expected_full_digest = full_him_initial_state_logical_digest_v1(contract)
    if binding.full_initial_state_logical_digest != expected_full_digest or binding.full_initial_state_reference != f"him-full-initial-state:v1:{expected_full_digest}":
        raise HimHeadContractError("MODEL_EXECUTION_FULL_STATE_MISMATCH")
    expected_binding_digest = model_execution_binding_logical_digest_v1(
        binding.base_model_binding_logical_digest,
        binding.head_contract_logical_digest,
        binding.head_contract_reference,
        initialization.logical_digest,
        initialization.reference,
        binding.full_initial_state_logical_digest,
        binding.full_initial_state_reference,
    )
    if binding.logical_digest != expected_binding_digest or binding.reference != f"him-model-execution-binding:v1:{expected_binding_digest}":
        raise HimHeadContractError("MODEL_EXECUTION_BINDING_DIGEST_MISMATCH")


def _resolve_model_execution_device(value: str | torch.device) -> torch.device:
    if isinstance(value, str):
        if value == "cpu":
            return torch.device("cpu")
        if value == "cuda:0":
            try:
                return resolve_him_execution_device_v1("CUDA", 0, torch_module=torch)
            except HimExecutionDeviceError as error:
                raise HimModelArchitectureError(str(error)) from error
        raise HimModelArchitectureError("UNKNOWN_EXECUTION_DEVICE")
    if not isinstance(value, torch.device):
        raise HimModelArchitectureError("EXECUTION_DEVICE_INVALID")
    if value.type == "cpu" and value.index is None:
        return value
    if value.type == "cuda" and value.index == 0:
        try:
            resolve_him_execution_device_v1("CUDA", 0, torch_module=torch)
        except HimExecutionDeviceError as error:
            raise HimModelArchitectureError(str(error)) from error
        return value
    raise HimModelArchitectureError("EXECUTION_DEVICE_INVALID")


def _validate_model_inputs(
    input_ids: Tensor,
    attention_mask: Tensor,
    config: HimXlmRobertaConfigV1,
    execution_device: torch.device = torch.device("cpu"),
) -> None:
    if not isinstance(input_ids, Tensor) or input_ids.dtype != torch.int64 or input_ids.ndim != 2:
        raise HimModelArchitectureError("INPUT_IDS_INVALID")
    if input_ids.device != execution_device or not input_ids.is_contiguous():
        raise HimModelArchitectureError("INPUT_IDS_DEVICE_OR_LAYOUT_INVALID")
    if input_ids.shape[0] == 0 or input_ids.shape[1] == 0 or input_ids.shape[1] > config.max_position_embeddings:
        raise HimModelArchitectureError("INPUT_SEQUENCE_LENGTH_INVALID")
    if bool(torch.any(input_ids < 0).item()) or bool(torch.any(input_ids >= config.vocab_size).item()):
        raise HimModelArchitectureError("INPUT_TOKEN_ID_INVALID")
    validate_attention_mask_v1(attention_mask, input_ids)
    if attention_mask.device != input_ids.device or not attention_mask.is_contiguous():
        raise HimModelArchitectureError("ATTENTION_MASK_DEVICE_OR_LAYOUT_INVALID")
    expected_padding = input_ids.eq(config.pad_token_id)
    actual_padding = attention_mask.eq(0)
    if not bool(torch.equal(expected_padding, actual_padding)):
        raise HimModelArchitectureError("PAD_MASK_MISMATCH")
    if bool(torch.any(input_ids[:, 0] != config.bos_token_id).item()):
        raise HimModelArchitectureError("NATIVE_START_TOKEN_REQUIRED")


def build_xlm_roberta_model_from_config_v1(config: HimXlmRobertaConfigV1) -> HimXlmRobertaBaseModelV1:
    validate_model_config_v1(config)
    return HimXlmRobertaBaseModelV1(config)


def _validate_weight_coverage(source_keys: set[str], runtime_keys: set[str]) -> None:
    missing = runtime_keys.difference(source_keys)
    unexpected = source_keys.difference(runtime_keys)
    if missing:
        raise HimModelLoadError(f"MISSING_REQUIRED_WEIGHT:{','.join(sorted(missing))}")
    if unexpected.difference(UNUSED_SOURCE_WEIGHT_KEYS):
        raise HimModelLoadError(f"UNEXPECTED_SOURCE_WEIGHT:{','.join(sorted(unexpected.difference(UNUSED_SOURCE_WEIGHT_KEYS)))}")
    if unexpected != set(UNUSED_SOURCE_WEIGHT_KEYS):
        raise HimModelLoadError("UNUSED_SOURCE_WEIGHT_ALLOWLIST_MISMATCH")


def _load_exact_state_from_safetensors(model: HimXlmRobertaBaseModelV1, handle: Any) -> None:
    runtime_keys = set(model.state_dict().keys())
    source_keys = set(handle.keys())
    _validate_weight_coverage(source_keys, runtime_keys)
    state: dict[str, Tensor] = {}
    for runtime_key in sorted(runtime_keys):
        source_key = runtime_key
        tensor = handle.get_tensor(source_key)
        if tensor.dtype != EXPECTED_SOURCE_WEIGHT_DTYPE:
            raise HimModelLoadError(f"SOURCE_WEIGHT_DTYPE_INVALID:{source_key}")
        expected_shape = tuple(model.state_dict()[runtime_key].shape)
        if tuple(tensor.shape) != expected_shape:
            raise HimModelLoadError(f"SOURCE_WEIGHT_SHAPE_INVALID:{source_key}")
        state[runtime_key] = tensor
    try:
        model.load_state_dict(state, strict=True)
    except RuntimeError as error:
        raise HimModelLoadError("MODEL_PARAMETER_LOAD_FAILED") from error
    if any(parameter.dtype != torch.float32 for parameter in model.parameters()):
        raise HimModelLoadError("MODEL_PARAMETER_DTYPE_INVALID")


def load_pinned_him_xlm_roberta_base_v1() -> HimXlmRobertaBaseModelV1:
    """Load only the digest-pinned local model; no arbitrary path is accepted."""

    root = MODEL_ROOT
    if not root.is_dir() or root.is_symlink():
        raise HimModelLoadError("MODEL_ROOT_INVALID")
    config_path = root / CONFIG_FILENAME
    weights_path = root / WEIGHTS_FILENAME
    _require_file(config_path, "CONFIG_ARTIFACT_INVALID")
    _require_file(weights_path, "WEIGHTS_ARTIFACT_INVALID")
    _require_digest(config_path, CONFIG_SHA256, "CONFIG_DIGEST_MISMATCH")
    _require_digest(weights_path, WEIGHTS_SHA256, "WEIGHTS_DIGEST_MISMATCH")
    config = load_pinned_model_config_v1()
    try:
        with safe_open(str(weights_path), framework="pt", device="cpu") as handle:
            source_keys = set(handle.keys())
            if not source_keys:
                raise HimModelLoadError("WEIGHT_KEY_INVENTORY_EMPTY")
            model = build_xlm_roberta_model_from_config_v1(config)
            _load_exact_state_from_safetensors(model, handle)
    except HimModelLoadError:
        raise
    except Exception as error:
        raise HimModelLoadError("SAFETENSORS_OPEN_OR_MAPPING_FAILED") from error
    model.eval()
    return model


def load_pinned_him_xlm_roberta_base_from_root_v1(root: Path) -> HimXlmRobertaBaseModelV1:
    """Load the same pinned model from an explicit deployment root.

    This is the packet/deployment entrypoint.  The original V1 function keeps
    its repository-local contract; this separate API prevents V2 from using
    that implicit fallback.
    """

    if not root.is_dir() or root.is_symlink():
        raise HimModelLoadError("MODEL_ROOT_INVALID")
    config_path = root / CONFIG_FILENAME
    weights_path = root / WEIGHTS_FILENAME
    _require_file(config_path, "CONFIG_ARTIFACT_INVALID")
    _require_file(weights_path, "WEIGHTS_ARTIFACT_INVALID")
    _require_digest(config_path, CONFIG_SHA256, "CONFIG_DIGEST_MISMATCH")
    _require_digest(weights_path, WEIGHTS_SHA256, "WEIGHTS_DIGEST_MISMATCH")
    try:
        with config_path.open("r", encoding="utf-8") as handle:
            value = json.load(handle)
        config = HimXlmRobertaConfigV1.from_mapping(value)
        with safe_open(str(weights_path), framework="pt", device="cpu") as handle:
            source_keys = set(handle.keys())
            if not source_keys:
                raise HimModelLoadError("WEIGHT_KEY_INVENTORY_EMPTY")
            model = build_xlm_roberta_model_from_config_v1(config)
            _load_exact_state_from_safetensors(model, handle)
    except HimModelLoadError:
        raise
    except Exception as error:
        raise HimModelLoadError("EXPLICIT_MODEL_ROOT_LOAD_FAILED") from error
    model.eval()
    return model


def load_pinned_model_config_from_root_v1(root: Path) -> HimXlmRobertaConfigV1:
    """Read and digest-check config from an explicit deployment root."""

    if not root.is_dir() or root.is_symlink():
        raise HimModelLoadError("MODEL_ROOT_INVALID")
    config_path = root / CONFIG_FILENAME
    _require_file(config_path, "CONFIG_ARTIFACT_INVALID")
    _require_digest(config_path, CONFIG_SHA256, "CONFIG_DIGEST_MISMATCH")
    try:
        with config_path.open("r", encoding="utf-8") as handle:
            value = json.load(handle)
        return HimXlmRobertaConfigV1.from_mapping(value)
    except HimModelLoadError:
        raise
    except Exception as error:
        raise HimModelLoadError("EXPLICIT_CONFIG_ROOT_LOAD_FAILED") from error


def load_pinned_him_multi_head_model_from_root_v1(
    execution_binding: ModelExecutionBinding,
    expected_base_model_binding_logical_digest: str,
    expected_training_configuration_seed: int,
    model_root: Path,
    execution_device: str | torch.device = "cpu",
) -> HimMultiHeadModelV1:
    """Build the proven multi-head model without a repository path fallback."""

    base_model = load_pinned_him_xlm_roberta_base_from_root_v1(model_root)
    model = build_him_multi_head_model_v1(base_model, execution_binding, expected_base_model_binding_logical_digest, expected_training_configuration_seed, execution_device)
    model.eval()
    return model


def expected_runtime_weight_keys_v1(config: HimXlmRobertaConfigV1) -> frozenset[str]:
    """Return keys from the explicit runtime architecture, for audit tests."""

    return frozenset(build_xlm_roberta_model_from_config_v1(config).state_dict().keys())
