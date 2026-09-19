import json
import pathlib
import tempfile
import unittest

from him_trainer.evaluation_authorities.productive_v2 import (
    AUTHORITY_VERSION,
    CHECKPOINT_DIGEST,
    CHECKPOINT_REFERENCE,
    prepare_productive_evaluation,
    ProductiveEvaluationAuthorityError,
)
from him_trainer import expanded_validation_p2 as historical


ROOT = pathlib.Path(__file__).resolve().parents[3]
AUTHORITY_ROOT = ROOT / "data/knowledge/him/training/a100-transfer/p2-expanded-validation/v1/46553e60e94908c2d73585ee514758b96fc2d33b18d39027dc989ad7a974044d/packet/authority"
AUTHORITY_FILE = ROOT / "training/him/src/him_trainer/evaluation_authorities/productive-v2-authority.json"
CHECKPOINT = ROOT / "training/him/artifacts/productive-v2/5af227d4-7ed1-4367-aec4-f42d2b83042d/p2-v2-training-output-3/checkpoint/checkpoint-manifest.json"


class ProductiveEvaluationAuthorityV2Test(unittest.TestCase):
    def test_pre_forward_binding_and_population(self):
        prepared = prepare_productive_evaluation(
            authority_root=AUTHORITY_ROOT,
            authority_path=AUTHORITY_FILE,
            checkpoint_manifest_path=CHECKPOINT,
        )
        self.assertEqual(prepared["authority"]["authorityVersion"], AUTHORITY_VERSION)
        self.assertEqual(prepared["manifest"]["checkpointReference"], CHECKPOINT_REFERENCE)
        self.assertEqual(prepared["manifest"]["checkpointLogicalDigest"], CHECKPOINT_DIGEST)
        self.assertEqual(len(prepared["examples"]), 19)
        self.assertEqual(prepared["modelForwardCount"], 0)
        self.assertEqual(prepared["holdoutAccessCount"], 0)
        self.assertIsNone(prepared["modelInput"])

    def test_historical_evaluator_is_unchanged(self):
        self.assertEqual(historical.EXPECTED_CHECKPOINT_REFERENCE, "him-training-checkpoint:v2:29312dac2a6524d1ca2e20563831d19346ae5cd831610db5a2bf160229437622")
        self.assertEqual(historical.EXPECTED_CHECKPOINT_DIGEST, "a3a152b77b3ca51f356b262267d6230533f8b9ea9031f48e0e813e415a7e599e")

    def test_checkpoint_binding_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / "authority.json"
            value = json.loads(AUTHORITY_FILE.read_text(encoding="utf-8"))
            value["checkpointDigest"] = "wrong"
            path.write_text(json.dumps(value), encoding="utf-8")
            with self.assertRaises(ProductiveEvaluationAuthorityError):
                prepare_productive_evaluation(authority_root=AUTHORITY_ROOT, authority_path=path, checkpoint_manifest_path=CHECKPOINT)


if __name__ == "__main__":
    unittest.main()
