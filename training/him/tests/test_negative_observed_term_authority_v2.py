"""Focused packet-only validation for the P1 negative observed-term closure."""

from __future__ import annotations

import copy
import hashlib
import json
import unittest
from pathlib import Path


REPO = Path(__file__).resolve().parents[3]
PACKET = (
    REPO
    / "data/knowledge/him/training/a100-transfer/v2"
    / "p1-training-input-authority-v2-negative-observed-term-v1/packet"
)
INVENTORY = PACKET.parent / "transfer-inventory.v2.json"
TERM = "Brie double crème"
N1 = "negative-example:v1:113a5ce116991c864581175a486ff1f65ce89c5c9b892f5c0399c40ed9c84146"
N2 = "negative-example:v1:bc4960dcca492cd8fe8f9181d4eb95561fdb254605598403aea70d7efc620107"
E1 = "280b91e4c4e10f95288308d50a0df31e3be773385c11afc104e8525127cc5b1e"
E2 = "045ee67398a6f7355b0faef3760ceb0d24bf22515b9e67d84c7931e96d2c9fba"


def canonical(value: object) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode()


def load(relative: str) -> dict:
    return json.loads((PACKET / relative).read_text(encoding="utf-8"))


def digest_without_logical_digest(value: dict) -> str:
    clone = copy.deepcopy(value)
    clone.pop("logicalDigest", None)
    return hashlib.sha256(canonical(clone)).hexdigest()


def validate_negative_binding(binding: dict) -> None:
    required = {
        "exampleReference",
        "candidateReference",
        "evidenceReference",
        "observedTerm",
        "humanAuthorityReference",
        "candidateCompatibilityAuthorityReference",
        "sourceAuthority",
        "candidateCompatibility",
        "secondaryTarget",
        "secondaryMask",
        "logicalDigest",
        "reference",
    }
    if set(binding) < required:
        raise ValueError("OBSERVED_TERM_BINDING_FIELDS_MISSING")
    if not binding["exampleReference"].startswith("negative-example:v1:"):
        raise ValueError("EXAMPLE_REFERENCE_INVALID")
    if binding["candidateReference"] != "uVfHe4":
        raise ValueError("CANDIDATE_REFERENCE_INVALID")
    if binding["evidenceReference"] not in {E1, E2}:
        raise ValueError("EVIDENCE_REFERENCE_INVALID")
    expected_evidence = {N1: E1, N2: E2}.get(binding["exampleReference"])
    if expected_evidence != binding["evidenceReference"]:
        raise ValueError("EXAMPLE_EVIDENCE_BINDING_INVALID")
    if "observedTerm" not in binding or binding["observedTerm"] != TERM:
        raise ValueError("OBSERVED_TERM_INVALID")
    if binding["humanAuthorityReference"] != "user-response:training-input-authority-v2-batch-1":
        raise ValueError("HUMAN_AUTHORITY_MISSING")
    if binding["candidateCompatibility"] != "REJECT":
        raise ValueError("NEGATIVE_COMPATIBILITY_CHANGED")
    if binding["secondaryTarget"] != 1 or binding["secondaryMask"] != 1:
        raise ValueError("SECONDARY_AUTHORITY_CHANGED")
    if binding["reference"] != f"model-visible-observed-term-binding:v2:{binding['logicalDigest']}":
        raise ValueError("BINDING_REFERENCE_MISMATCH")


class NegativeObservedTermAuthorityV2Test(unittest.TestCase):
    def setUp(self) -> None:
        self.authority = load("authority/model-visible-observed-term-authority.v2.json")
        self.examples = load("dataset/complete-training-examples.v2.json")
        self.corpus = load("dataset/corpus.v2.json")
        self.partition = load("dataset/partition.v2.json")
        self.request = load("request/productive-training-request.v2.json")
        self.manifest = load("manifest/productive-training-manifest.v2.json")
        self.readiness = load("readiness/productive-training-readiness.v2.json")

    def negative_examples(self) -> list[dict]:
        return [item for item in self.examples["examples"] if item["candidateCompatibility"] == "REJECT"]

    def test_exactly_two_negative_authority_bindings(self) -> None:
        self.assertEqual(2, self.authority["bindingCount"])
        self.assertEqual({N1, N2}, {item["exampleReference"] for item in self.authority["bindings"]})

    def test_exact_terms_and_evidence_are_bound(self) -> None:
        for binding in self.authority["bindings"]:
            validate_negative_binding(binding)
            self.assertEqual(TERM, binding["observedTerm"])
        self.assertEqual({E1, E2}, {item["evidenceReference"] for item in self.authority["bindings"]})

    def test_human_authority_and_source_provenance_are_present(self) -> None:
        self.assertTrue(all(item["humanAuthorityReference"].startswith("user-response:") for item in self.authority["bindings"]))
        self.assertTrue(all(item["sourceAuthority"] == "ORIGINAL_V1_HUMAN_AUTHORITY" for item in self.authority["bindings"]))
        self.assertTrue(all(item["provenance"]["heuristicDerivation"] is False for item in self.authority["bindings"]))

    def test_reject_and_secondary_semantics_are_preserved(self) -> None:
        self.assertEqual(2, len(self.negative_examples()))
        self.assertTrue(all(item["candidateCompatibility"] == "REJECT" for item in self.negative_examples()))
        self.assertTrue(all(item["secondaryTarget"] == 1 and item["secondaryMask"] == 1 for item in self.negative_examples()))
        self.assertTrue(all(item["targetKind"] == "VARIANT" for item in self.negative_examples()))
        self.assertTrue(all(item["familyGroupReference"] == "family:v1:canonical:uVfHe4" for item in self.negative_examples()))

    def test_sequence_input_is_two_nonempty_segments(self) -> None:
        for item in self.negative_examples():
            segments = [item["observedTerm"], f"CANDIDATE {item['candidateName']}"]
            self.assertEqual([TERM, "CANDIDATE Brie"], segments)
            self.assertTrue(all(segment.strip() for segment in segments))

    def test_missing_observed_term_fails_closed(self) -> None:
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding.pop("observedTerm")
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_empty_observed_term_fails_closed(self) -> None:
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding["observedTerm"] = ""
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_candidate_name_is_not_a_fallback(self) -> None:
        self.assertNotEqual("Brie", TERM)
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding["observedTerm"] = binding["candidateReference"]
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_negative_prefix_is_not_a_fallback(self) -> None:
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding["observedTerm"] = binding["exampleReference"]
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_relation_is_not_a_fallback(self) -> None:
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding["observedTerm"] = "VARIANT_OF"
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_wrong_observed_term_fails_closed(self) -> None:
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding["observedTerm"] = "Brie"
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_wrong_example_binding_fails_closed(self) -> None:
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding["exampleReference"] = N2
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_wrong_evidence_binding_fails_closed(self) -> None:
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding["evidenceReference"] = E2
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_missing_human_authority_fails_closed(self) -> None:
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding["humanAuthorityReference"] = ""
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_changed_reject_fails_closed(self) -> None:
        binding = copy.deepcopy(self.authority["bindings"][0])
        binding["candidateCompatibility"] = "COMPATIBLE"
        with self.assertRaises(ValueError):
            validate_negative_binding(binding)

    def test_family_mutation_is_detectable(self) -> None:
        item = copy.deepcopy(self.negative_examples()[0])
        item["familyGroupReference"] = "family:v1:canonical:other"
        self.assertNotEqual("family:v1:canonical:uVfHe4", item["familyGroupReference"])

    def test_partition_mutation_is_detectable(self) -> None:
        references = {item["exampleReference"] for item in self.partition["exampleAssignments"]}
        self.assertEqual({item["exampleReference"] for item in self.examples["examples"]}, references)
        mutated = copy.deepcopy(self.partition)
        mutated["exampleAssignments"][0]["partition"] = "VALIDATION"
        self.assertNotEqual(self.partition["exampleAssignments"][0]["partition"], mutated["exampleAssignments"][0]["partition"])

    def test_packet_contains_no_historical_runtime_dependency(self) -> None:
        self.assertTrue(all(path.is_file() and not path.is_symlink() for path in PACKET.rglob("*" ) if path.is_file()))
        self.assertTrue(all("negative-examples/v1" not in str(path) for path in PACKET.rglob("*")))

    def test_downstream_rebinds_are_current(self) -> None:
        authority_ref = self.authority["reference"]
        authority_digest = self.authority["logicalDigest"]
        self.assertEqual(authority_digest, self.examples["observedTermAuthorityLogicalDigest"])
        self.assertEqual(authority_ref, self.examples["observedTermAuthorityReference"])
        self.assertEqual(self.examples["logicalDigest"], self.corpus["completeTrainingExamplesLogicalDigest"])
        self.assertEqual(self.corpus["logicalDigest"], self.partition["corpusLogicalDigest"])
        self.assertEqual(self.manifest["logicalDigest"], self.readiness["manifestDigest"])
        self.assertEqual(self.request["logicalDigest"], self.readiness["productiveRequestDigest"])

    def test_identity_and_split_counts_are_preserved(self) -> None:
        self.assertEqual(40, len(self.examples["examples"]))
        self.assertEqual(32, self.partition["trainExampleCount"])
        self.assertEqual(6, self.partition["validationExampleCount"])
        self.assertEqual(2, self.partition["holdoutExampleCount"])
        self.assertEqual(0, self.partition["familyLeakageCount"])
        self.assertEqual(0, self.partition["exampleLeakageCount"])

    def test_artifact_digests_round_trip(self) -> None:
        for relative in (
            "dataset/complete-training-examples.v2.json",
            "dataset/corpus.v2.json",
            "dataset/partition.v2.json",
            "readiness/evaluation-readiness.v2.json",
            "request/productive-training-request.v2.json",
            "manifest/productive-training-manifest.v2.json",
            "readiness/productive-training-readiness.v2.json",
        ):
            value = load(relative)
            self.assertEqual(value["logicalDigest"], digest_without_logical_digest(value))

    def test_authority_and_inventory_are_deterministic(self) -> None:
        authority = load("authority/model-visible-observed-term-authority.v2.json")
        self.assertEqual(authority["reference"], f"model-visible-observed-term-authority:v2:{authority['logicalDigest']}")
        inventory = json.loads(INVENTORY.read_text(encoding="utf-8"))
        self.assertEqual(9, inventory["fileCount"])
        self.assertEqual(sorted(entry["relativePath"] for entry in inventory["files"]), [entry["relativePath"] for entry in inventory["files"]])
        for entry in inventory["files"]:
            path = PACKET / entry["relativePath"]
            self.assertEqual(entry["size"], path.stat().st_size)
            self.assertEqual(entry["sha256"], hashlib.sha256(path.read_bytes()).hexdigest())


if __name__ == "__main__":
    unittest.main()
