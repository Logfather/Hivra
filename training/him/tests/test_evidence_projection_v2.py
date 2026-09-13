import unittest

from him_trainer.evidence_projection_v2 import (
    EvidenceProjectionV2Error,
    MISSING_VALUE,
    project_evidence_v2,
    project_to_input_v2,
)


class EvidenceProjectionV2Test(unittest.TestCase):
    def test_open_food_facts_source_mapping(self) -> None:
        projection = project_evidence_v2(
            "OPEN_FOOD_FACTS",
            {
                "identity": {"genericName": "  Generic label  ", "productName": "Product", "productType": "food"},
                "taxonomy": {"categories": ["en:z", "en:a", "en:a"], "foodGroups": ["group"]},
                "ingredients": {"text": "Milk\r\nSugar"},
                "source": {"code": "redacted-from-model"},
            },
            source_record_identity="off:product:example",
        )
        self.assertEqual(projection.source_faithful_label, "Generic label")
        self.assertEqual(projection.ingredient_or_composition, "Milk\nSugar")
        self.assertEqual(projection.categories, ("en:a", "en:z"))
        self.assertEqual(projection.food_groups, ("group",))
        self.assertEqual(projection.product_type, "food")
        self.assertEqual(projection.provenance.source, "OPEN_FOOD_FACTS")

    def test_agribalyse_source_mapping(self) -> None:
        projection = project_evidence_v2(
            "AGRIBALYSE",
            {"productNameFr": "Produit", "lciName": "LCI", "foodGroup": "group", "foodSubgroup": "subgroup"},
        )
        self.assertEqual(projection.source_faithful_label, "Produit")
        self.assertEqual(projection.food_groups, ("group", "subgroup"))
        self.assertEqual(projection.ingredient_or_composition, MISSING_VALUE)
        self.assertEqual(projection.product_type, MISSING_VALUE)

    def test_ciqual_and_glycemic_index_source_mapping(self) -> None:
        ciqual = project_evidence_v2(
            "CIQUAL",
            {"nameEn": "Food", "nameFr": "Aliment", "groupNameEn": "Group", "subgroupNameEn": "Subgroup"},
        )
        gi = project_evidence_v2(
            "GLYCEMIC_INDEX",
            {"foodItem": {"lexicalValue": "Food item", "status": "PRESENT"}, "sourceContext": {"majorCategory": "Category"}, "gi": "55"},
        )
        self.assertEqual(ciqual.source_faithful_label, "Food")
        self.assertEqual(ciqual.food_groups, ("Group", "Subgroup"))
        self.assertEqual(gi.source_faithful_label, "Food item")
        self.assertEqual(gi.food_groups, ("Category",))

    def test_projection_is_immutable_and_digest_excludes_provenance(self) -> None:
        first = project_evidence_v2("OPEN_FOOD_FACTS", {"identity": {"productName": "A"}}, source_record_identity="one")
        second = project_evidence_v2("OPEN_FOOD_FACTS", {"identity": {"productName": "A"}}, source_record_identity="two")
        self.assertEqual(first.logical_digest, second.logical_digest)
        self.assertEqual(first.reference, second.reference)
        with self.assertRaises((AttributeError, TypeError)):
            first.source_faithful_label = "changed"  # type: ignore[misc]

    def test_target_and_supervision_metadata_are_rejected(self) -> None:
        for key in ("targetKind", "candidateCompatibility", "negativeBoundary", "familyReference", "sourceRecordKind", "authorityId"):
            with self.assertRaisesRegex(EvidenceProjectionV2Error, "MODEL_METADATA_FIELD_REJECTED"):
                project_evidence_v2("OPEN_FOOD_FACTS", {key: "forbidden", "identity": {"productName": "A"}})

    def test_projection_is_unchanged_when_only_either_target_changes(self) -> None:
        source = {"identity": {"productName": "A"}, "taxonomy": {"categories": ["category"]}}
        primary_zero = project_evidence_v2("OPEN_FOOD_FACTS", source)
        primary_one = project_evidence_v2("OPEN_FOOD_FACTS", source)
        secondary_zero = project_evidence_v2("OPEN_FOOD_FACTS", source)
        secondary_one = project_evidence_v2("OPEN_FOOD_FACTS", source)
        self.assertEqual(primary_zero.reference, primary_one.reference)
        self.assertEqual(secondary_zero.reference, secondary_one.reference)

    def test_wrong_types_and_unsafe_values_fail_closed(self) -> None:
        with self.assertRaisesRegex(EvidenceProjectionV2Error, "MUST_BE_TEXT"):
            project_evidence_v2("OPEN_FOOD_FACTS", {"identity": {"productName": {"not": "text"}}})
        with self.assertRaisesRegex(EvidenceProjectionV2Error, "K_MUST_BE"):
            project_evidence_v2("OPEN_FOOD_FACTS", {"taxonomy": {"categories": {"bad": "shape"}}})
        with self.assertRaisesRegex(EvidenceProjectionV2Error, "UNSAFE_BINARY"):
            project_evidence_v2("OPEN_FOOD_FACTS", {"identity": {"productName": "bad\x00value"}})

    def test_missing_and_extreme_valid_values_are_not_truncated(self) -> None:
        missing = project_evidence_v2("AGRIBALYSE", {})
        self.assertEqual(missing.source_faithful_label, MISSING_VALUE)
        long_label = "x" * 4096
        projection = project_evidence_v2("OPEN_FOOD_FACTS", {"identity": {"productName": long_label}})
        self.assertEqual(projection.source_faithful_label, long_label)
        with self.assertRaisesRegex(EvidenceProjectionV2Error, "OVERSIZED"):
            project_evidence_v2("OPEN_FOOD_FACTS", {"identity": {"productName": "x" * (1_048_576 + 1)}})

    def test_input_v2_binding_exposes_semantics_without_provenance(self) -> None:
        projection = project_evidence_v2(
            "OPEN_FOOD_FACTS",
            {"identity": {"productName": "Pomelo"}, "taxonomy": {"categories": ["en:fresh-pomelos"]}},
        )
        candidate = {"itemname": "Pomelos", "normalized": "pomelos", "taxonomyPaths": [["fruit"]]}
        representation = project_to_input_v2("Pomelo", candidate, projection)
        serialized = representation.serialize()
        self.assertIn("L=Pomelo", serialized)
        self.assertIn("K=en:fresh-pomelos", serialized)
        self.assertNotIn("source-record", serialized)
        self.assertNotIn("OPEN_FOOD_FACTS", serialized)
        self.assertNotIn("authority", serialized.lower())


if __name__ == "__main__":
    unittest.main()
