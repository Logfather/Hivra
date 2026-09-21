import unittest
import tempfile
import torch
from him_trainer.productive_retraining_v3 import *
class ProductiveRetrainingV3Test(unittest.TestCase):
 @classmethod
 def setUpClass(cls): cls.a=prepare_retraining_inputs()
 def test_authorities(self): self.assertEqual((len(self.a['train']),len(self.a['development'])),(328,6))
 def test_single_serializer_and_fields(self):
  for part in ('train','development'):
   for x in self.a[part]: self.assertTrue(x['serializedInput'].startswith('<HIMV2V3>'))
 def test_steps_namespace(self):
  self.assertEqual(training_count_contract()['totalOptimizerSteps'],123)
  self.assertNotEqual(new_run_namespace()['runId'],HISTORICAL_RUN_ID)
 def test_pomelos_context(self):
  xs=[x for x in self.a['train'] if x['record'].get('observedTerm')=='Pomelos']
  self.assertGreaterEqual(len(xs),2); self.assertEqual(len({x['serializedInput'] for x in xs}),len(xs))
if __name__=='__main__': unittest.main()

class OrchestrationContractTest(unittest.TestCase):
 def test_preflight_is_non_mutating(self):
  from him_trainer.productive_retraining_v3 import preflight_productive_retraining_v3, execute_productive_retraining_v3
  p=preflight_productive_retraining_v3(); self.assertEqual(p['state'],'PREFLIGHT_PASS'); r=execute_productive_retraining_v3(); self.assertEqual(r['state'],'PRE_FIRST_FORWARD_READY'); self.assertEqual(r['optimizerStepCount'],0)
 def test_execute_requires_explicit_authorization(self):
  from him_trainer.productive_retraining_v3 import execute_productive_retraining_v3
  import inspect
  self.assertIn("execute", inspect.signature(execute_productive_retraining_v3).parameters)

class RuntimeIdentityBindingRegressionTest(unittest.TestCase):
 def test_repaired_runtime_identity_roundtrip(self):
  from him_trainer.checkpoint_v2 import persist_checkpoint, reload_checkpoint
  from him_trainer.productive_retraining_v3 import _load_deployed_runtime_identity
  identity = _load_deployed_runtime_identity()
  model = torch.nn.Linear(2, 2)
  optimizer = torch.optim.AdamW(model.parameters(), lr=0.001)
  optimizer_identity = {"optimizerId":"adamw:v1"}
  bindings = {"serializer": "him_trainer.input_representation_v3.serialize_contextual_input_v3", "runtimeIdentity": identity, "optimizerIdentity": optimizer_identity}
  with tempfile.TemporaryDirectory() as directory:
   result = persist_checkpoint(directory, model=model, optimizer=optimizer, run_reference="v3-binding-test", optimizer_step=0, authority_bindings=bindings, runtime_identity=identity, optimizer_identity=optimizer_identity)
   reload_checkpoint(result.manifest_path, model=model, optimizer=optimizer, expected_bindings=bindings, expected_optimizer_step=0)
   self.assertTrue(result.manifest_path.is_file())

 def test_old_device_only_shape_is_rejected(self):
  from him_trainer.checkpoint_v2 import CheckpointRuntimeError, persist_checkpoint
  model = torch.nn.Linear(2, 2)
  optimizer = torch.optim.AdamW(model.parameters(), lr=0.001)
  with tempfile.TemporaryDirectory() as directory:
   with self.assertRaisesRegex(CheckpointRuntimeError, "CHECKPOINT_RUNTIME_IDENTITY_MISMATCH"):
    persist_checkpoint(directory, model=model, optimizer=optimizer, run_reference="v3-old-binding-test", optimizer_step=0, authority_bindings={"serializer":"him_trainer.input_representation_v3.serialize_contextual_input_v3"}, runtime_identity={"device":"cuda:0"}, optimizer_identity={"optimizerId":"adamw:v1"})
