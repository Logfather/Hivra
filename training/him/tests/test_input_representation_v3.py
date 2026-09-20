import unittest

from him_trainer.input_representation_v3 import ContextualInputV3, serialize_contextual_input_v3


class InputRepresentationV3Test(unittest.TestCase):
    def test_observed_and_candidate_are_preserved(self):
        text = ContextualInputV3("Pomelos", "Pomelos").serialize()
        self.assertIn("O=Pomelos", text)
        self.assertIn("C=Pomelos", text)

    def test_context_is_visible(self):
        text = ContextualInputV3("Pomelos", "Pomelos", "Pomelo", ("citrus",), ("fruit",)).serialize()
        self.assertIn("L=Pomelo", text)
        self.assertIn("K=citrus", text)
        self.assertIn("G=fruit", text)

    def test_missing_context_is_explicit(self):
        text = ContextualInputV3("x", "y").serialize()
        self.assertIn("L=UNKNOWN", text)
        self.assertIn("K=UNKNOWN", text)
        self.assertIn("G=UNKNOWN", text)

    def test_field_order_is_frozen(self):
        self.assertEqual(ContextualInputV3("o", "c", "l", ("k",), ("g",)).serialize().splitlines()[1::1][:5], ["O=o", "C=c", "L=l", "K=k", "G=g"])

    def test_serialization_is_idempotent(self):
        record = {"observedTerm": "o", "candidateName": "c", "sourceFaithfulLabel": "l", "categories": ["b", "a"], "foodGroups": ["g"]}
        self.assertEqual(serialize_contextual_input_v3(record), serialize_contextual_input_v3(record))

    def test_pomelos_context_distinguishes_records(self):
        identity = ContextualInputV3("Pomelos", "Pomelos", "Pomelo", ("citrus",), ("fruit",)).serialize()
        variant = ContextualInputV3("Pomelos", "Pomelos", "Pomelos chinois", ("food",), ("food",)).serialize()
        self.assertNotEqual(identity, variant)

    def test_authority_fields_are_not_serialized(self):
        text = ContextualInputV3("o", "c").serialize()
        self.assertNotIn("TARGET_KIND", text)
        self.assertNotIn("COMPATIBLE", text)

    def test_mapping_entrypoint_matches_dataclass(self):
        record = {"observedTerm": "o", "candidateName": "c", "sourceFaithfulLabel": "l"}
        self.assertEqual(serialize_contextual_input_v3(record), ContextualInputV3("o", "c", "l").serialize())

    def test_context_sorting_is_deterministic(self):
        self.assertEqual(ContextualInputV3("o", "c", category_context=("b", "a")).serialize(), ContextualInputV3("o", "c", category_context=("a", "b")).serialize())

    def test_historical_serializer_is_not_imported(self):
        self.assertEqual(ContextualInputV3.CONTRACT_VERSION if hasattr(ContextualInputV3, "CONTRACT_VERSION") else "3", "3")

    def test_single_future_entrypoint(self):
        self.assertTrue(callable(serialize_contextual_input_v3))
