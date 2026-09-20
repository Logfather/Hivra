import tempfile, unittest
from pathlib import Path
from him_trainer.retraining_authority_materialization_v1 import materialize_model_tokenizer

class MaterializationTest(unittest.TestCase):
    def test_model_and_tokenizer_materialize_idempotently(self):
        with tempfile.TemporaryDirectory() as d:
            target = Path(d) / "workspace" / "him" / "retraining-v3-authority"
            first = materialize_model_tokenizer(".", target)
            second = materialize_model_tokenizer(".", target)
            self.assertEqual(first, second)
            self.assertTrue((target / "model" / "model.safetensors").is_file())
            self.assertTrue((target / "tokenizer" / "tokenizer.json").is_file())

if __name__ == "__main__": unittest.main()
