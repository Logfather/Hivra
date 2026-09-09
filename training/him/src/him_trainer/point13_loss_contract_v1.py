"""Framework-neutral Point-13 masked multi-objective loss authority.

This module deliberately has no PyTorch import.  The authority is part of
the serialized training-request identity, while numerical execution lives in
``point13_loss_v1``.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass


LOSS_CONTRACT_ID_V1 = "HIM_MASKED_MULTI_OBJECTIVE_LOSS_CONTRACT_V1"
LOSS_CONTRACT_VERSION_V1 = "1"
LOSS_CONTRACT_STATE_V1 = "MASKED_MULTI_OBJECTIVE_LOSS_DEFINED"
LOSS_CONTRACT_REFERENCE_PREFIX_V1 = "masked-multi-objective-loss:v1:"
MULTI_OBJECTIVE_WEIGHTING_AUTHORITY_DIGEST_V1 = (
    "c8b45847159c5ce711c27fbb0b3e6811ed2cdfaefabef6fc1aa474e0c89dbcca"
)


class HimLossContractError(ValueError):
    """Raised when loss authority is absent or has drifted."""


@dataclass(frozen=True)
class HimMaskedMultiObjectiveLossContractV1:
    """Complete deterministic authority for Point-13 loss evaluation."""

    contract_id: str
    version: str
    state: str
    primary_loss_function: str
    secondary_loss_function: str
    primary_class_order: tuple[str, ...]
    primary_target_codes: tuple[int, ...]
    primary_target_to_loss_index_mapping: str
    secondary_class_order: tuple[str, ...]
    secondary_target_codes: tuple[int, ...]
    secondary_target_to_loss_index_mapping: str
    primary_loss_reduction: str
    secondary_loss_reduction: str
    objective_mask_semantics: str
    objective_mask_dtype: str
    objective_mask_allowed_values: tuple[float, ...]
    masked_loss_normalization: str
    zero_active_objective_policy: str
    per_example_both_masks_zero_policy: str
    weight_application_order: str
    total_loss_formula: str
    objective_weight_renormalization: str
    primary_weight: str
    secondary_weight: str
    weighting_authority_digest: str
    logical_digest: str
    reference: str

    def identity_payload(self) -> dict[str, object]:
        return {
            "contractId": self.contract_id,
            "version": self.version,
            "state": self.state,
            "primaryLossFunction": self.primary_loss_function,
            "secondaryLossFunction": self.secondary_loss_function,
            "primaryClassOrder": list(self.primary_class_order),
            "primaryTargetCodes": list(self.primary_target_codes),
            "primaryTargetToLossIndexMapping": self.primary_target_to_loss_index_mapping,
            "secondaryClassOrder": list(self.secondary_class_order),
            "secondaryTargetCodes": list(self.secondary_target_codes),
            "secondaryTargetToLossIndexMapping": self.secondary_target_to_loss_index_mapping,
            "primaryLossReduction": self.primary_loss_reduction,
            "secondaryLossReduction": self.secondary_loss_reduction,
            "objectiveMaskSemantics": self.objective_mask_semantics,
            "objectiveMaskDtype": self.objective_mask_dtype,
            "objectiveMaskAllowedValues": list(self.objective_mask_allowed_values),
            "maskedLossNormalization": self.masked_loss_normalization,
            "zeroActiveObjectivePolicy": self.zero_active_objective_policy,
            "perExampleBothMasksZeroPolicy": self.per_example_both_masks_zero_policy,
            "weightApplicationOrder": self.weight_application_order,
            "totalLossFormula": self.total_loss_formula,
            "objectiveWeightRenormalization": self.objective_weight_renormalization,
            "primaryWeight": self.primary_weight,
            "secondaryWeight": self.secondary_weight,
            "weightingAuthorityDigest": self.weighting_authority_digest,
        }


def build_him_masked_multi_objective_loss_contract_v1() -> HimMaskedMultiObjectiveLossContractV1:
    """Return the sole frozen V1 loss authority."""

    unsigned = HimMaskedMultiObjectiveLossContractV1(
        contract_id=LOSS_CONTRACT_ID_V1,
        version=LOSS_CONTRACT_VERSION_V1,
        state=LOSS_CONTRACT_STATE_V1,
        primary_loss_function="CROSS_ENTROPY_RAW_LOGITS",
        secondary_loss_function="CROSS_ENTROPY_RAW_LOGITS",
        primary_class_order=("EXISTING_CANONICAL", "IDENTITY", "VARIANT", "ALIAS", "NEW_CANONICAL"),
        primary_target_codes=(1, 2, 3, 4, 5),
        primary_target_to_loss_index_mapping="SEMANTIC_CODE_MINUS_ONE_1_TO_5",
        secondary_class_order=("COMPATIBLE", "REJECT"),
        secondary_target_codes=(0, 1),
        secondary_target_to_loss_index_mapping="IDENTITY_0_TO_1",
        primary_loss_reduction="NONE",
        secondary_loss_reduction="NONE",
        objective_mask_semantics="BINARY_FLOAT32_0_OR_1",
        objective_mask_dtype="FLOAT32",
        objective_mask_allowed_values=(0.0, 1.0),
        masked_loss_normalization="ACTIVE_MEAN",
        zero_active_objective_policy="ZERO_CONTRIBUTION_AUTOGRAD_SAFE",
        per_example_both_masks_zero_policy="FAIL_CLOSED",
        weight_application_order="AFTER_MASKED_REDUCTION",
        total_loss_formula="PRIMARY_WEIGHTED_PLUS_SECONDARY_WEIGHTED",
        objective_weight_renormalization="NO",
        primary_weight="1.0",
        secondary_weight="1.0",
        weighting_authority_digest=MULTI_OBJECTIVE_WEIGHTING_AUTHORITY_DIGEST_V1,
        logical_digest="0" * 64,
        reference="",
    )
    logical_digest = hashlib.sha256(
        json.dumps(unsigned.identity_payload(), ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")
    ).hexdigest()
    return HimMaskedMultiObjectiveLossContractV1(
        **{
            **unsigned.__dict__,
            "logical_digest": logical_digest,
            "reference": f"{LOSS_CONTRACT_REFERENCE_PREFIX_V1}{logical_digest}",
        }
    )


def validate_him_masked_multi_objective_loss_contract_v1(
    contract: HimMaskedMultiObjectiveLossContractV1,
) -> None:
    """Fail closed before any numerical loss operation is allowed."""

    expected = build_him_masked_multi_objective_loss_contract_v1()
    if contract != expected:
        raise HimLossContractError("LOSS_CONTRACT_AUTHORITY_MISMATCH")


def decode_him_masked_multi_objective_loss_contract_v1(
    value: object,
) -> HimMaskedMultiObjectiveLossContractV1:
    """Decode the exact serialized authority without importing PyTorch."""

    if not isinstance(value, dict):
        raise HimLossContractError("LOSS_AUTHORITY_OBJECT_REQUIRED")
    required = {
        "contractId", "version", "state", "primaryLossFunction", "secondaryLossFunction",
        "primaryClassOrder", "primaryTargetCodes", "primaryTargetToLossIndexMapping",
        "secondaryClassOrder", "secondaryTargetCodes", "secondaryTargetToLossIndexMapping",
        "primaryLossReduction", "secondaryLossReduction", "objectiveMaskSemantics",
        "objectiveMaskDtype", "objectiveMaskAllowedValues", "maskedLossNormalization",
        "zeroActiveObjectivePolicy", "perExampleBothMasksZeroPolicy", "weightApplicationOrder",
        "totalLossFormula", "objectiveWeightRenormalization", "primaryWeight", "secondaryWeight",
        "weightingAuthorityDigest", "logicalDigest", "reference",
    }
    if set(value) != required:
        raise HimLossContractError("LOSS_AUTHORITY_FIELDS_INVALID")
    try:
        contract = HimMaskedMultiObjectiveLossContractV1(
            contract_id=value["contractId"],
            version=value["version"],
            state=value["state"],
            primary_loss_function=value["primaryLossFunction"],
            secondary_loss_function=value["secondaryLossFunction"],
            primary_class_order=tuple(value["primaryClassOrder"]),
            primary_target_codes=tuple(value["primaryTargetCodes"]),
            primary_target_to_loss_index_mapping=value["primaryTargetToLossIndexMapping"],
            secondary_class_order=tuple(value["secondaryClassOrder"]),
            secondary_target_codes=tuple(value["secondaryTargetCodes"]),
            secondary_target_to_loss_index_mapping=value["secondaryTargetToLossIndexMapping"],
            primary_loss_reduction=value["primaryLossReduction"],
            secondary_loss_reduction=value["secondaryLossReduction"],
            objective_mask_semantics=value["objectiveMaskSemantics"],
            objective_mask_dtype=value["objectiveMaskDtype"],
            objective_mask_allowed_values=tuple(value["objectiveMaskAllowedValues"]),
            masked_loss_normalization=value["maskedLossNormalization"],
            zero_active_objective_policy=value["zeroActiveObjectivePolicy"],
            per_example_both_masks_zero_policy=value["perExampleBothMasksZeroPolicy"],
            weight_application_order=value["weightApplicationOrder"],
            total_loss_formula=value["totalLossFormula"],
            objective_weight_renormalization=value["objectiveWeightRenormalization"],
            primary_weight=value["primaryWeight"],
            secondary_weight=value["secondaryWeight"],
            weighting_authority_digest=value["weightingAuthorityDigest"],
            logical_digest=value["logicalDigest"],
            reference=value["reference"],
        )
    except (TypeError, ValueError, KeyError) as error:
        raise HimLossContractError("LOSS_AUTHORITY_VALUE_INVALID") from error
    validate_him_masked_multi_objective_loss_contract_v1(contract)
    return contract
