from __future__ import annotations

import json
import unittest
from pathlib import Path

from him_trainer.productive_training_p2_v2 import (
    build_batch_tensor_shape_contract,
    build_partition_bound_batches,
    build_tensor_shape_contract,
    preflight,
)
from him_trainer.training_input_authority_v2 import (
    AUTHORITY_RELATIVE,
    INVENTORY_RELATIVE,
    CORPUS_REFERENCE,
    PARTITION_REFERENCE,
    TrainingInputV2Error,
    load_v2_input_bundle,
    persist_training_input_authority,
    validate_v2_authority_identity,
)
from him_trainer.runtime_source_closure_v2 import REQUIRED_RUNTIME_MODULES, runtime_source_closure
from him_trainer.training_readiness_authority_v2 import evaluate_training_readiness_v2


ROOT = Path(__file__).resolve().parents[3]


class TrainingInputAuthorityV2Test(unittest.TestCase):
    def test_exact_partition_bound_counts_and_dynamic_shapes(self) -> None:
        result = preflight(str(ROOT))
        self.assertEqual((32, 8, 0), (result["train"], result["validation"], result["holdout"]))
        self.assertEqual([8, 8, 8, 8], result["trainBatches"])
        self.assertEqual([8], result["validationBatches"])
        self.assertEqual(4, len(result["trainBatchShapes"]))
        self.assertTrue(all(shape["input_ids"][0] == 8 and shape["input_ids"][1] <= 256 for shape in result["trainBatchShapes"]))
        self.assertLessEqual(result["trainShape"]["input_ids"][1], 256)

    def test_no_holdout_and_corn_replacement_are_bound(self) -> None:
        bundle = load_v2_input_bundle(ROOT)
        self.assertEqual((), bundle["holdout"])
        entries = list(bundle["train"]) + list(bundle["validation"])
        self.assertTrue(any(item["candidateReference"].endswith(":3pMmZU") and item["inputRepresentationDigest"] for item in entries))

    def test_unauthorized_batch_change_rejected(self) -> None:
        bundle = load_v2_input_bundle(ROOT)
        with self.assertRaisesRegex(TrainingInputV2Error, "UNAUTHORIZED_BATCH_CHANGE"):
            build_partition_bound_batches(tuple(bundle["train"]), 4)

    def test_v1_authorities_rejected(self) -> None:
        with self.assertRaisesRegex(TrainingInputV2Error, "REFERENCE_MISMATCH"):
            from him_trainer.training_input_authority_v2 import _verify_artifact
            _verify_artifact(ROOT / "data/knowledge/him/training/p2/canonical-catalog-expansion/v2/corpus-assembly-v2/corpus.v2.json", "p2-corpus:v1:deadbeef", None)
        with self.assertRaisesRegex(TrainingInputV2Error, "V1_OR_UNAUTHORIZED_PARTITION"):
            validate_v2_authority_identity(corpus_reference=CORPUS_REFERENCE, partition_reference="p2-partition:v1:old", sequence_reference="sequence-length-authority:v1:old")

    def test_sequence_128_authority_rejected(self) -> None:
        with self.assertRaisesRegex(TrainingInputV2Error, "SEQUENCE_256_AUTHORITY_REQUIRED"):
            validate_v2_authority_identity(corpus_reference=CORPUS_REFERENCE, partition_reference=PARTITION_REFERENCE, sequence_reference="sequence-length-authority:v2:128")

    def test_persistence_reload_and_digest(self) -> None:
        result = persist_training_input_authority(ROOT)
        self.assertEqual(result["inventory"], json.loads((ROOT / INVENTORY_RELATIVE).read_text()))
        self.assertEqual(result["authority"], json.loads((ROOT / AUTHORITY_RELATIVE).read_text()))

    def test_runtime_closure_is_explicit_and_readiness_fails_closed_without_image(self) -> None:
        self.assertEqual(32, len(REQUIRED_RUNTIME_MODULES))
        self.assertEqual(32, len(runtime_source_closure(ROOT)))
        readiness = evaluate_training_readiness_v2(ROOT)
        self.assertFalse(readiness["trainingReady"])
        self.assertFalse(readiness["gates"]["runtime"])
        self.assertTrue(readiness["gates"]["trainerSource"])

    def test_runtime_image_digest_drift_is_rejected(self) -> None:
        readiness = evaluate_training_readiness_v2(
            ROOT,
            runtime_image_digest="sha256:" + ("0" * 64),
            runtime_authorized=True,
        )
        self.assertFalse(readiness["gates"]["runtime"])
        self.assertFalse(readiness["trainingReady"])

    def test_input_mutation_invalidates_authority(self) -> None:
        result = persist_training_input_authority(ROOT)
        mutated = dict(result["inventory"])
        mutated["entries"] = list(mutated["entries"])
        mutated["entries"][0] = dict(mutated["entries"][0])
        mutated["entries"][0]["inputRepresentationDigest"] = "0" * 64
        from him_trainer.training_input_authority_v2 import validate_training_input_authority
        with self.assertRaises(TrainingInputV2Error):
            validate_training_input_authority(result["authority"], result["bundle"], mutated)


if __name__ == "__main__":
    unittest.main()
