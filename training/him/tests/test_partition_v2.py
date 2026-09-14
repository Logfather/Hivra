import unittest
from dataclasses import replace

from him_trainer.corpus_coverage_v2 import CorpusExampleV2
from him_trainer.evidence_projection_v2 import HimEvidenceProjectionV2
from him_trainer.input_representation_v2 import build_input_representation_v2
from him_trainer.partition_v2 import _component_reference, assign_group_then_stratify


def example(ref, family, primary=None, secondary=None, boundary=None, partition="UNASSIGNED"):
    projection = HimEvidenceProjectionV2(source_faithful_label=ref, product_type="food")
    model_input = build_input_representation_v2(
        ref, {"itemname": "Candidate", "normalized": "candidate", "taxonomyPaths": (("food",),)}, projection.model_input_mapping()
    )
    return CorpusExampleV2(ref, family, "source:" + ref, projection.reference, model_input, "candidate:Candidate", "OPEN_FOOD_FACTS", partition, primary, secondary, boundary, sequence_length=8)


class PartitionV2Test(unittest.TestCase):
    def test_complete_components_are_never_split(self):
        values = [
            example("i", "family:a", primary="IDENTITY"),
            example("v", "family:a", primary="VARIANT"),
            example("c", "family:b", secondary="COMPATIBLE"),
            example("r", "family:b", secondary="REJECT", boundary="WRONG_SCOPE"),
        ]
        assigned = assign_group_then_stratify(values)
        self.assertEqual({item.partition for item in assigned if item.family_reference == "family:a"}, {"VALIDATION"})
        self.assertEqual({item.partition for item in assigned if item.family_reference == "family:b"}, {"VALIDATION"})
        self.assertEqual(len(assigned), 4)

    def test_target_changes_do_not_change_component_assignment_inputs(self):
        first = example("same", "family:same", primary="IDENTITY")
        second = replace(first, primary_target="VARIANT")
        self.assertEqual(first.isolation_example.decision_context_reference, second.isolation_example.decision_context_reference)
        self.assertEqual(_component_reference((first,)), _component_reference((second,)))


if __name__ == "__main__":
    unittest.main()
