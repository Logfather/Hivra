"""Framework-neutral Point-13 train-forward RNG authority.

The authority is separate from the initial model state and from loss
semantics.  It describes the deterministic runtime stream that the Python
trainer initializes once per run and advances across successive forwards.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass


FORWARD_RNG_CONTRACT_ID_V1 = "HIM_TRAIN_FORWARD_RNG_CONTRACT_V1"
FORWARD_RNG_CONTRACT_VERSION_V1 = "1"
FORWARD_RNG_STATE_V1 = "TRAIN_FORWARD_RNG_AUTHORITY_DEFINED"
FORWARD_RNG_ROOT_SEED_SOURCE_V1 = "training-configuration.seed"
FORWARD_RNG_DOMAIN_SEPARATOR_V1 = "HIM_TRAIN_FORWARD_RNG_V1:MODEL_FORWARD"
FORWARD_RNG_DEVICE_V1 = "cpu"
FORWARD_RNG_INITIALIZATION_V1 = "ONCE_PER_RUN"
FORWARD_RNG_RESET_POLICY_V1 = "NO_RESET_BEFORE_BATCH"
FORWARD_RNG_STREAM_SEMANTICS_V1 = "SEQUENTIAL_STATE_ADVANCE"
FORWARD_RNG_GLOBAL_STATE_POLICY_V1 = "FORKED_CPU_RNG_STATE_RESTORED"
FORWARD_RNG_REFERENCE_PREFIX_V1 = "train-forward-rng:v1:"
FORWARD_RNG_MAX_SEED_V1 = (1 << 63) - 1


class HimForwardRngContractError(ValueError):
    """Raised when train-forward RNG authority is absent or invalid."""


def _canonical_digest(value: dict[str, object]) -> str:
    encoded = json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
    return hashlib.sha256(encoded.encode("utf-8")).hexdigest()


def derive_him_train_forward_rng_seed_v1(
    root_seed: int,
    domain_separator: str = FORWARD_RNG_DOMAIN_SEPARATOR_V1,
) -> int:
    """Derive a stable PyTorch-compatible seed without Python hash state."""

    if isinstance(root_seed, bool) or not isinstance(root_seed, int) or root_seed < 0:
        raise HimForwardRngContractError("FORWARD_RNG_ROOT_SEED_INVALID")
    if not isinstance(domain_separator, str) or not domain_separator:
        raise HimForwardRngContractError("FORWARD_RNG_DOMAIN_SEPARATOR_INVALID")
    payload = {
        "contractId": FORWARD_RNG_CONTRACT_ID_V1,
        "version": FORWARD_RNG_CONTRACT_VERSION_V1,
        "rootSeedSource": FORWARD_RNG_ROOT_SEED_SOURCE_V1,
        "rootSeed": root_seed,
        "domainSeparator": domain_separator,
    }
    digest = hashlib.sha256(
        json.dumps(payload, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")
    ).digest()
    return int.from_bytes(digest[:8], byteorder="big", signed=False) % (FORWARD_RNG_MAX_SEED_V1 + 1)


@dataclass(frozen=True)
class HimTrainForwardRngContractV1:
    """Complete deterministic authority for the Point-13 train RNG stream."""

    contract_id: str
    version: str
    state: str
    root_seed_source: str
    root_seed: int
    domain_separator: str
    derived_forward_seed: int
    device: str
    initialization: str
    reset_policy: str
    stream_semantics: str
    global_state_policy: str
    logical_digest: str
    reference: str

    def identity_payload(self) -> dict[str, object]:
        return {
            "contractId": self.contract_id,
            "version": self.version,
            "state": self.state,
            "rootSeedSource": self.root_seed_source,
            "rootSeed": self.root_seed,
            "domainSeparator": self.domain_separator,
            "derivedForwardSeed": self.derived_forward_seed,
            "device": self.device,
            "initialization": self.initialization,
            "resetPolicy": self.reset_policy,
            "streamSemantics": self.stream_semantics,
            "globalStatePolicy": self.global_state_policy,
        }


def build_him_train_forward_rng_contract_v1(
    root_seed: int,
    domain_separator: str = FORWARD_RNG_DOMAIN_SEPARATOR_V1,
) -> HimTrainForwardRngContractV1:
    derived_seed = derive_him_train_forward_rng_seed_v1(root_seed, domain_separator)
    unsigned = HimTrainForwardRngContractV1(
        contract_id=FORWARD_RNG_CONTRACT_ID_V1,
        version=FORWARD_RNG_CONTRACT_VERSION_V1,
        state=FORWARD_RNG_STATE_V1,
        root_seed_source=FORWARD_RNG_ROOT_SEED_SOURCE_V1,
        root_seed=root_seed,
        domain_separator=domain_separator,
        derived_forward_seed=derived_seed,
        device=FORWARD_RNG_DEVICE_V1,
        initialization=FORWARD_RNG_INITIALIZATION_V1,
        reset_policy=FORWARD_RNG_RESET_POLICY_V1,
        stream_semantics=FORWARD_RNG_STREAM_SEMANTICS_V1,
        global_state_policy=FORWARD_RNG_GLOBAL_STATE_POLICY_V1,
        logical_digest="0" * 64,
        reference="",
    )
    logical_digest = _canonical_digest(unsigned.identity_payload())
    return HimTrainForwardRngContractV1(
        **{
            **unsigned.__dict__,
            "logical_digest": logical_digest,
            "reference": f"{FORWARD_RNG_REFERENCE_PREFIX_V1}{logical_digest}",
        }
    )


def validate_him_train_forward_rng_contract_v1(
    contract: HimTrainForwardRngContractV1,
    *,
    expected_root_seed: int | None = None,
) -> None:
    """Fail closed before a train-mode forward can consume RNG."""

    if not isinstance(contract, HimTrainForwardRngContractV1):
        raise HimForwardRngContractError("FORWARD_RNG_AUTHORITY_TYPE_INVALID")
    if contract.contract_id != FORWARD_RNG_CONTRACT_ID_V1 or contract.version != FORWARD_RNG_CONTRACT_VERSION_V1:
        raise HimForwardRngContractError("FORWARD_RNG_CONTRACT_ID_OR_VERSION_INVALID")
    if contract.state != FORWARD_RNG_STATE_V1:
        raise HimForwardRngContractError("FORWARD_RNG_STATE_INVALID")
    if contract.root_seed_source != FORWARD_RNG_ROOT_SEED_SOURCE_V1:
        raise HimForwardRngContractError("FORWARD_RNG_ROOT_SEED_SOURCE_INVALID")
    if expected_root_seed is not None and contract.root_seed != expected_root_seed:
        raise HimForwardRngContractError("FORWARD_RNG_ROOT_SEED_MISMATCH")
    if not isinstance(contract.root_seed, int) or isinstance(contract.root_seed, bool) or contract.root_seed < 0:
        raise HimForwardRngContractError("FORWARD_RNG_ROOT_SEED_INVALID")
    if not isinstance(contract.domain_separator, str) or not contract.domain_separator:
        raise HimForwardRngContractError("FORWARD_RNG_DOMAIN_SEPARATOR_INVALID")
    expected_seed = derive_him_train_forward_rng_seed_v1(contract.root_seed, contract.domain_separator)
    if contract.derived_forward_seed != expected_seed or not 0 <= contract.derived_forward_seed <= FORWARD_RNG_MAX_SEED_V1:
        raise HimForwardRngContractError("FORWARD_RNG_DERIVED_SEED_INVALID")
    if contract.device != FORWARD_RNG_DEVICE_V1:
        raise HimForwardRngContractError("FORWARD_RNG_DEVICE_UNSUPPORTED")
    if contract.initialization != FORWARD_RNG_INITIALIZATION_V1:
        raise HimForwardRngContractError("FORWARD_RNG_INITIALIZATION_POLICY_INVALID")
    if contract.reset_policy != FORWARD_RNG_RESET_POLICY_V1:
        raise HimForwardRngContractError("FORWARD_RNG_RESET_POLICY_INVALID")
    if contract.stream_semantics != FORWARD_RNG_STREAM_SEMANTICS_V1:
        raise HimForwardRngContractError("FORWARD_RNG_STREAM_SEMANTICS_INVALID")
    if contract.global_state_policy != FORWARD_RNG_GLOBAL_STATE_POLICY_V1:
        raise HimForwardRngContractError("FORWARD_RNG_GLOBAL_STATE_POLICY_INVALID")
    expected_digest = _canonical_digest(contract.identity_payload())
    if contract.logical_digest != expected_digest:
        raise HimForwardRngContractError("FORWARD_RNG_DIGEST_MISMATCH")
    if contract.reference != f"{FORWARD_RNG_REFERENCE_PREFIX_V1}{contract.logical_digest}":
        raise HimForwardRngContractError("FORWARD_RNG_REFERENCE_MISMATCH")


def decode_him_train_forward_rng_contract_v1(value: object) -> HimTrainForwardRngContractV1:
    """Decode the exact serialized authority without importing PyTorch."""

    if not isinstance(value, dict):
        raise HimForwardRngContractError("FORWARD_RNG_AUTHORITY_OBJECT_REQUIRED")
    required = {
        "contractId", "version", "state", "rootSeedSource", "rootSeed", "domainSeparator",
        "derivedForwardSeed", "device", "initialization", "resetPolicy", "streamSemantics",
        "globalStatePolicy", "logicalDigest", "reference",
    }
    if set(value) != required:
        raise HimForwardRngContractError("FORWARD_RNG_AUTHORITY_FIELDS_INVALID")
    try:
        contract = HimTrainForwardRngContractV1(
            contract_id=value["contractId"],
            version=value["version"],
            state=value["state"],
            root_seed_source=value["rootSeedSource"],
            root_seed=value["rootSeed"],
            domain_separator=value["domainSeparator"],
            derived_forward_seed=value["derivedForwardSeed"],
            device=value["device"],
            initialization=value["initialization"],
            reset_policy=value["resetPolicy"],
            stream_semantics=value["streamSemantics"],
            global_state_policy=value["globalStatePolicy"],
            logical_digest=value["logicalDigest"],
            reference=value["reference"],
        )
    except (TypeError, ValueError, KeyError) as error:
        raise HimForwardRngContractError("FORWARD_RNG_AUTHORITY_VALUE_INVALID") from error
    validate_him_train_forward_rng_contract_v1(contract)
    return contract


def forward_rng_contract_wire_v1(contract: HimTrainForwardRngContractV1) -> dict[str, object]:
    """Return the canonical wire representation used by protocol fixtures."""

    validate_him_train_forward_rng_contract_v1(contract)
    return {
        "contractId": contract.contract_id,
        "version": contract.version,
        "state": contract.state,
        "rootSeedSource": contract.root_seed_source,
        "rootSeed": contract.root_seed,
        "domainSeparator": contract.domain_separator,
        "derivedForwardSeed": contract.derived_forward_seed,
        "device": contract.device,
        "initialization": contract.initialization,
        "resetPolicy": contract.reset_policy,
        "streamSemantics": contract.stream_semantics,
        "globalStatePolicy": contract.global_state_policy,
        "logicalDigest": contract.logical_digest,
        "reference": contract.reference,
    }
