import unittest
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
 def test_execute_requires_explicit_primitives(self):
  from him_trainer.productive_retraining_v3 import execute_productive_retraining_v3
  with self.assertRaises(RuntimeError): execute_productive_retraining_v3(execute=True)
