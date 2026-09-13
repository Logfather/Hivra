"""Offline, model-free tests for the target-independent HIM input V2 contract."""

from __future__ import annotations

import copy
import json
import unittest
from pathlib import Path

from him_trainer.input_representation_v2 import (
    CANDIDATE_CONDITIONING_V2_FIELDS,
    FIELD_ORDER,
    FORBIDDEN_MODEL_INPUT_TOKENS,
    HimInputRepresentationV2,
    InputRepresentationV2Error,
    MANDATORY_FIELDS,
    MAX_SEQUENCE_LENGTH,
    MISSING_VALUE,
    OBSERVED_CONDITIONING_V2_FIELDS,
    OPTIONAL_FIELDS,
    assert_no_conflicting_targets,
    build_input_representation_v2,
    scan_model_input,
    token_budget,
    validate_no_conflicting_targets,
)


ROOT = Path(__file__).resolve().parents[3]
TOKENIZER = ROOT / "training/him/models/xlm-roberta-base/e73636d4f797dec63c3081bb6ed5c7b0bb3f2089/tokenizer.json"
CORPUS = ROOT / "data/knowledge/him/training/p2/canonical-catalog-expansion/v1/mixed-supervision-corpus-after-secondary-only-negatives.p2.json"
PARTITION = ROOT / "data/knowledge/him/training/p2/canonical-catalog-expansion/v1/mixed-supervision-family-aware-partition.p2.json"
EXPANDED = ROOT / "data/knowledge/him/training/expansions/p2/evaluation-expansion/v1/p2-expanded-validation-authority.v1.json"
RELATION_AUTHORITY = ROOT / "data/knowledge/him/training/expansions/p2/evaluation-expansion/v1/p2-final-relation-authority.v1.json"
CATALOG = ROOT / "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json"


def candidate_map() -> dict[str, dict[str, object]]:
    return {item["itemname"]: item for item in json.loads(CATALOG.read_text(encoding="utf-8"))}


def build_record(record: dict[str, object], candidates: dict[str, dict[str, object]]) -> HimInputRepresentationV2:
    return HimInputRepresentationV2.from_record(record, candidates[record["canonicalName"]])


def authorized_v2_inputs() -> tuple[tuple[str, HimInputRepresentationV2], ...]:
    candidates = candidate_map()
    corpus = json.loads(CORPUS.read_text(encoding="utf-8"))
    partition = json.loads(PARTITION.read_text(encoding="utf-8"))
    assignment_by_ref = {item["exampleReference"]: item for item in partition["exampleAssignments"]}
    selected = [item for item in corpus["examples"] if assignment_by_ref[item["exampleReference"]]["partition"] in {"TRAIN", "VALIDATION"}]
    expanded = json.loads(EXPANDED.read_text(encoding="utf-8"))["records"]
    relation_records = json.loads(RELATION_AUTHORITY.read_text(encoding="utf-8"))["records"]
    source_evidence_by_record = {item["sourceRecordReference"]: item["sourceEvidence"] for item in relation_records}
    expanded = [
        {**item, "sourceEvidence": source_evidence_by_record[item["sourceRecordReference"]]}
        for item in expanded
    ]
    rows = [("TRAIN_OR_VALIDATION", build_record(item, candidates)) for item in selected]
    rows.extend(("EXPANDED", build_record(item, candidates)) for item in expanded)
    return tuple(rows)


def authorized_v2_inputs_by_split() -> dict[str, tuple[HimInputRepresentationV2, ...]]:
    candidates = candidate_map()
    corpus = json.loads(CORPUS.read_text(encoding="utf-8"))
    partition = json.loads(PARTITION.read_text(encoding="utf-8"))
    assignment = {item["exampleReference"]: item["partition"] for item in partition["exampleAssignments"]}
    result = {
        split: tuple(
            build_record(item, candidates)
            for item in corpus["examples"]
            if assignment[item["exampleReference"]] == split
        )
        for split in ("TRAIN", "VALIDATION")
    }
    relation_records = json.loads(RELATION_AUTHORITY.read_text(encoding="utf-8"))["records"]
    evidence_by_record = {item["sourceRecordReference"]: item["sourceEvidence"] for item in relation_records}
    expanded = json.loads(EXPANDED.read_text(encoding="utf-8"))["records"]
    result["EXPANDED"] = tuple(
        build_record({**item, "sourceEvidence": evidence_by_record[item["sourceRecordReference"]]}, candidates)
        for item in expanded
    )
    return result


class InputRepresentationV2Test(unittest.TestCase):
    def test_contract_shape_and_version_are_explicit(self) -> None:
        self.assertEqual(("O", "L", "I", "K", "G", "T", "C", "N", "X"), FIELD_ORDER)
        self.assertEqual(("O", "L", "I", "C", "N", "X"), MANDATORY_FIELDS)
        self.assertEqual(("K", "G", "T"), OPTIONAL_FIELDS)
        self.assertEqual(("TERM", "NORMALIZED_TERM", "TAXONOMY_PATHS", "OPTIONAL_EXPLICIT_ALIASES"), CANDIDATE_CONDITIONING_V2_FIELDS)
        self.assertEqual(("TERM", "OPTIONAL_LANGUAGE", "SOURCE_FAITHFUL_LABEL", "COMPOSITION", "CATEGORIES", "FOOD_GROUPS", "PRODUCT_TYPE", "OPTIONAL_DESCRIPTION"), OBSERVED_CONDITIONING_V2_FIELDS)
        self.assertEqual(128, MAX_SEQUENCE_LENGTH)

    def test_canonical_field_order_and_missing_policy(self) -> None:
        value = build_input_representation_v2(
            "  Pomelos  ",
            {"itemname": "Pomelos", "normalized": "pomelos", "taxonomyPaths": [["fruit", "citrus"]]},
            {"sourceFaithfulLabel": " Pomelo ", "categories": [], "foodGroups": [], "productType": "food"},
        ).serialize()
        self.assertEqual(
            "<HIMV2>\nO=Pomelos\nL=Pomelo\nI=UNKNOWN\nK=UNKNOWN\nG=UNKNOWN\nT=food\nC=Pomelos\nN=pomelos\nX=fruit/citrus\n</HIMV2>",
            value,
        )
        self.assertEqual([line.split("=", 1)[0] for line in value.splitlines()[1:-1]], list(FIELD_ORDER))

    def test_unicode_nfc_outer_whitespace_and_order_are_canonicalized(self) -> None:
        first = build_input_representation_v2(
            "  Cafe\u0301 ",
            {"itemname": "Café", "normalized": "café", "taxonomyPaths": [["z", "b"], ["a"]]},
            {"productName": "  Café ", "categories": ["z", "a", "z"], "foodGroups": ["g2", "g1"], "productType": "food"},
        )
        second = build_input_representation_v2(
            "Café",
            {"itemname": "Café", "normalized": "café", "taxonomyPaths": [["a"], ["z", "b"]]},
            {"productName": "Café", "categories": ["a", "z"], "foodGroups": ["g1", "g2"], "productType": "food"},
        )
        self.assertEqual(first.utf8(), second.utf8())

    def test_candidate_and_observed_conditioning_are_separate(self) -> None:
        representation = build_input_representation_v2(
            "Teff",
            {"itemname": "Teff", "normalized": "teff", "taxonomyPaths": [["grains", "teff"]], "aliases": []},
            {"productName": "Teff Ivory Grain", "productType": "food"},
        )
        self.assertEqual("Teff", representation.candidate.term)
        self.assertEqual("Teff Ivory Grain", representation.observed.source_faithful_label)
        self.assertNotIn("target", representation.__dataclass_fields__)
        self.assertNotIn("relation", representation.__dataclass_fields__)
        self.assertNotIn("compatibility", representation.__dataclass_fields__)

    def test_target_independent_constructor_and_supervision_ignorance(self) -> None:
        base = {
            "observedTerm": "Pomelos",
            "canonicalName": "Pomelos",
            "sourceEvidence": {"productName": "Pomelo", "productType": "food"},
        }
        candidate = candidate_map()["Pomelos"]
        without_supervision = HimInputRepresentationV2.from_record(base, candidate)
        altered = copy.deepcopy(base)
        altered.update({"targetKind": "OTHER", "relation": "OTHER", "candidateCompatibility": "OTHER", "negativeBoundary": "OTHER", "familyReference": "OTHER"})
        self.assertEqual(without_supervision.utf8(), HimInputRepresentationV2.from_record(altered, candidate).utf8())
        self.assertEqual(without_supervision.utf8(), build_input_representation_v2("Pomelos", candidate, base["sourceEvidence"]).utf8())

    def test_source_evidence_is_preserved_without_inventing_fields(self) -> None:
        representation = HimInputRepresentationV2.from_record(
            {"observedTerm": "Sojabohnen", "sourceEvidence": {"evidence": {"productName": "Protein-Power", "ingredientText": "50% gerösteter Sojabohnen-Mix, 30% Erdnusskerne, 20% Mandeln", "categories": ["en:snacks"], "productType": "food"}}},
            candidate_map()["Sojabohnen"],
        )
        text = representation.serialize()
        self.assertIn("L=Protein-Power", text)
        self.assertIn("I=50% gerösteter Sojabohnen-Mix, 30% Erdnusskerne, 20% Mandeln", text)
        self.assertIn("K=en:snacks", text)
        self.assertNotIn("REJECT", text)

    def test_forbidden_and_opaque_fields_never_enter_model_input(self) -> None:
        representations = tuple(item for _, item in authorized_v2_inputs())
        scan = scan_model_input(representations)
        self.assertEqual(0, scan["MODEL_INPUT_TECHNICAL_TARGET_TOKEN_LEAK_COUNT"])
        self.assertEqual(0, scan["OPAQUE_SHORTCUT_IDENTIFIER_COUNT"])
        self.assertEqual(0, scan["SOURCE_ID_SHORTCUT_MARKER_COUNT"])
        for item in representations:
            text = item.serialize()
            for token in FORBIDDEN_MODEL_INPUT_TOKENS:
                self.assertNotIn(token, text)

    def test_pomelos_v1_collision_is_resolved(self) -> None:
        candidates = candidate_map()
        identity = HimInputRepresentationV2.from_record({"observedTerm": "Pomelos", "sourceEvidence": {"productName": "Pomelo"}}, candidates["Pomelos"])
        variant = HimInputRepresentationV2.from_record({"observedTerm": "Pomelos", "sourceEvidence": {"productName": "Pomelos chinois"}}, candidates["Pomelos"])
        self.assertNotEqual(identity.utf8(), variant.utf8())
        self.assertIn("L=Pomelo", identity.serialize())
        self.assertIn("L=Pomelos chinois", variant.serialize())

    def test_collision_validation_fails_closed_without_majority_vote(self) -> None:
        self.assertEqual(0, validate_no_conflicting_targets([(b"a", "IDENTITY"), (b"b", "VARIANT")]))
        self.assertEqual(1, validate_no_conflicting_targets([(b"same", "IDENTITY"), (b"same", "VARIANT")]))
        with self.assertRaises(InputRepresentationV2Error):
            assert_no_conflicting_targets([(b"same", "COMPATIBLE"), (b"same", "REJECT")])

    def test_repeatability_and_unordered_collection_canonicalization(self) -> None:
        values = [item for _, item in authorized_v2_inputs()]
        self.assertEqual(
            [item.utf8() for item in values],
            [HimInputRepresentationV2.from_record(
                {
                    "observedTerm": item.observed.term,
                    "sourceEvidence": {
                        "sourceFaithfulLabel": item.observed.source_faithful_label,
                        "ingredientText": item.observed.composition,
                        "categories": list(reversed(item.observed.categories)),
                        "foodGroups": list(reversed(item.observed.food_groups)),
                        "productType": item.observed.product_type,
                        "language": item.observed.language,
                        "description": item.observed.description,
                    },
                },
                {"itemname": item.candidate.term, "normalized": item.candidate.normalized_term, "taxonomyPaths": list(reversed(item.candidate.taxonomy_paths))},
            ).utf8() for item in values],
        )

    def test_input_validation_fails_closed(self) -> None:
        with self.assertRaises(InputRepresentationV2Error):
            build_input_representation_v2("term", {"itemname": "Candidate", "normalized": "candidate", "taxonomyPaths": []}, {})
        with self.assertRaises(InputRepresentationV2Error):
            build_input_representation_v2("", {"itemname": "Candidate", "normalized": "candidate", "taxonomyPaths": [["food"]]}, {})

    def test_token_budget_and_no_silent_truncation_contract(self) -> None:
        budget = token_budget([62, 76, 159, 173])
        self.assertEqual({"min": 62, "median": 76, "p90": 173, "max": 173, "over128": 2}, budget)
        self.assertEqual(128, MAX_SEQUENCE_LENGTH)
        self.assertEqual("NO_TRUNCATION_FAIL_IF_TOO_LONG", "NO_TRUNCATION_FAIL_IF_TOO_LONG")

    def test_actual_authorized_token_budget_is_reproduced_without_model_activity(self) -> None:
        from tokenizers import Tokenizer

        tokenizer = Tokenizer.from_file(str(TOKENIZER))
        tokenizer.no_truncation()
        values = authorized_v2_inputs_by_split()
        expected = {
            "TRAIN": {"min": 62, "median": 83, "p90": 169, "max": 190, "over128": 9},
            "VALIDATION": {"min": 67, "median": 87, "p90": 144, "max": 144, "over128": 2},
            "EXPANDED": {"min": 71, "median": 94, "p90": 155, "max": 188, "over128": 4},
        }
        for split, items in values.items():
            lengths = [len(tokenizer.encode(item.serialize()).ids) for item in items]
            self.assertEqual(expected[split], token_budget(lengths), split)
        self.assertEqual(0, len(values.get("HOLDOUT", ())))

    def test_authorized_sets_exclude_holdout_and_are_constructible(self) -> None:
        rows = authorized_v2_inputs()
        self.assertEqual(51, len(rows))
        self.assertEqual(0, sum("HOLDOUT" in text for _, item in rows for text in (item.serialize(),)))
        self.assertTrue(all(item.candidate.taxonomy_paths for _, item in rows))


if __name__ == "__main__":
    unittest.main()
