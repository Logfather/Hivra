import json
import tempfile
import unittest
from pathlib import Path

from him_trainer.corpus_assembly_v2 import (
    CORPUS_OUTPUT_RELATIVE,
    assemble_eligible_examples,
    logical_digest,
    materialize,
    reload_corpus_examples_v2,
)
from him_trainer.corpus_coverage_v2 import build_coverage_report
from him_trainer.partition_leakage_v2 import REQUIRED_ISOLATION_UNITS_V2, validate_partition_assignments


ROOT = Path(__file__).resolve().parents[3]


class CorpusAssemblyV2Test(unittest.TestCase):
    def test_authoritative_assembly_is_deterministic_and_excludes_unit5(self):
        first, first_records, meta = assemble_eligible_examples(ROOT)
        second, second_records, _ = assemble_eligible_examples(ROOT)
        self.assertEqual([item.example_reference for item in first], [item.example_reference for item in second])
        self.assertEqual(first_records, second_records)
        self.assertEqual(len(first), 40)
        self.assertTrue(meta["unit5Excluded"])
        self.assertEqual(meta["unit5TokenLength"], 259)
        self.assertEqual(sum(item.secondary_target == "REJECT" for item in first), 11)
        self.assertEqual(sum(item.secondary_target == "COMPATIBLE" for item in first), 25)

    def test_corn_replacement_is_present_without_special_case_labeling(self):
        examples, _, _ = assemble_eligible_examples(ROOT)
        self.assertEqual(sum(item.source_record_identity == "off:product:row:18180:code:0021130111824" for item in examples), 1)

    def test_materialized_artifacts_reload_with_same_logical_identity(self):
        result = materialize(ROOT)
        output = ROOT / CORPUS_OUTPUT_RELATIVE
        corpus = json.loads((output / "corpus.v2.json").read_text())
        core = {key: value for key, value in corpus.items() if key not in {"reference", "logicalDigest"}}
        self.assertEqual(logical_digest(core), corpus["logicalDigest"])
        self.assertEqual(corpus["exampleCount"], 40)
        self.assertEqual(result["leakage"]["status"], "PASS")

    def test_reload_recomputes_coverage_and_leakage_from_persisted_input_bytes(self):
        materialize(ROOT)
        output = ROOT / CORPUS_OUTPUT_RELATIVE
        reloaded = reload_corpus_examples_v2(output / "corpus.v2.json")
        report = build_coverage_report(reloaded)
        self.assertEqual(report.example_count, 40)
        self.assertEqual(report.primary_counts, {"IDENTITY": 7, "VARIANT": 20})
        self.assertEqual(report.secondary_counts, {"COMPATIBLE": 25, "REJECT": 11})
        partition = json.loads((output / "partition.v2.json").read_text())
        memberships = {item["exampleReference"]: item["partition"] for item in partition["members"]}
        assigned = [type(item)(**{**item.__dict__, "partition": memberships[item.example_reference]}) for item in reloaded]
        validation = [item for item in assigned if item.partition == "VALIDATION"]
        self.assertEqual(build_coverage_report(validation).primary_counts, {"IDENTITY": 1, "VARIANT": 5})
        self.assertTrue(validate_partition_assignments(tuple(item.isolation_example for item in assigned), REQUIRED_ISOLATION_UNITS_V2).valid)
        by_input = {}
        for item in assigned:
            by_input.setdefault(item.model_input.serialize(), set()).add(item.partition)
        self.assertFalse(any(len(splits) > 1 for splits in by_input.values()))


if __name__ == "__main__":
    unittest.main()
