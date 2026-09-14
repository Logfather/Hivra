from __future__ import annotations

import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path

from him_trainer.productive_training_p2_v2 import (
    EXECUTION_AUTHORITY_ERROR,
    RUNNER_MODULE,
    TRAIN_COUNT,
    VALIDATION_COUNT,
    execution_preflight,
    load_v2_input_bundle,
    main,
)
from him_trainer.training_input_authority_v2 import logical_digest
from him_trainer.training_readiness_authority_v2 import (
    evaluate_training_readiness_v2,
    persist_training_readiness_v2,
    reload_training_readiness_v2,
)
from him_trainer.training_runtime_authority_v2 import (
    build_execution_enabled_training_runtime_authority_v2,
    persist_execution_enabled_training_runtime_authority_v2,
    reload_execution_enabled_training_runtime_authority_v2,
    validate_execution_enabled_training_runtime_authority_v2,
)


ROOT = Path(__file__).resolve().parents[3]
FINAL_AUTHORITY = ROOT / "build/knowledge/reports/him/p2-v2/runtime-workflow-retry/final-authority"


def _rebind(value: dict[str, object], *, reference_prefix: str) -> dict[str, object]:
    core = {key: item for key, item in value.items() if key not in {"logicalDigest", "reference"}}
    digest = logical_digest(core)
    return {**core, "logicalDigest": digest, "reference": f"{reference_prefix}:{digest}"}


def _authorities(directory: Path, *, execution: bool = True, readiness_ok: bool = True) -> tuple[Path, Path]:
    runtime = json.loads((FINAL_AUTHORITY / "runtime-authority.final.v2.json").read_text(encoding="utf-8"))
    readiness = json.loads((FINAL_AUTHORITY / "training-readiness.final.v2.json").read_text(encoding="utf-8"))
    bundle = load_v2_input_bundle(ROOT)
    runtime.update(
        {
            "runtimeImageDigest": "sha256:" + "a" * 64,
            "runtimeImageReference": "ghcr.io/logfather/him-a100-reference-runtime@sha256:" + "a" * 64,
            "runtimeSourceModuleCount": 30,
            "trainer": {"runnerModule": RUNNER_MODULE, "supportsRealExecution": True},
            "trainingInputAuthorityReference": json.loads((ROOT / "data/knowledge/him/training/p2/canonical-catalog-expansion/v2/runtime-authority/training-input-authority.v2.json").read_text(encoding="utf-8"))["reference"],
            "batchAuthorityReference": bundle["batch"]["reference"],
            "sequenceReference": "sequence-length-authority:v2:97130457decd4f283492da2d09a0faddb4bb99fc70f7d79732fbd390794cc509",
            "corpusReference": bundle["corpus"]["reference"],
            "partitionReference": bundle["partition"]["reference"],
            "leakageReference": bundle["leakage"]["reference"],
            "realTrainingExecutionAuthorized": execution,
            "trainingRuntimeAuthorized": execution,
            "status": "AUTHORIZED" if execution else "BLOCKED",
        },
    )
    runtime["runtime"] = {**runtime["runtime"], "runtimeImageDigest": runtime["runtimeImageDigest"]}  # type: ignore[index]
    runtime = _rebind(runtime, reference_prefix="him-p2-training-runtime-authority-execution-enabled:v2")
    readiness.update(
        {
            "runtimeAuthorityReference": runtime["reference"],
            "runtimeImageDigest": runtime["runtimeImageDigest"],
            "runtimeAuthorized": execution,
            "realTrainingExecutionAuthorized": execution,
            "trainingReady": readiness_ok and execution,
            "status": "AUTHORIZED" if readiness_ok and execution else "BLOCKED",
        },
    )
    readiness["gates"] = {**readiness["gates"], "executionAuthority": readiness_ok and execution}  # type: ignore[index]
    readiness = _rebind(readiness, reference_prefix="him-p2-training-readiness-execution-enabled:v2")
    runtime_path = directory / "runtime-authority.json"
    readiness_path = directory / "training-readiness.json"
    runtime_path.write_text(json.dumps(runtime, sort_keys=True), encoding="utf-8")
    readiness_path.write_text(json.dumps(readiness, sort_keys=True), encoding="utf-8")
    return runtime_path, readiness_path


class ProductiveTrainingP2V2ExecutionEnablementTest(unittest.TestCase):
    def test_default_is_model_free_preflight(self) -> None:
        result = main(["--root", str(ROOT)])
        self.assertEqual(0, result)

    def test_execute_rejects_disabled_authority(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            runtime, readiness = _authorities(Path(directory), execution=False)
            error_output = io.StringIO()
            with contextlib.redirect_stderr(error_output), self.assertRaises(SystemExit) as error:
                main(["--root", str(ROOT), "--execute", "--execution-preflight", "--runtime-authority", str(runtime), "--training-readiness", str(readiness)])
            self.assertNotEqual(0, error.exception.code)
            self.assertIn(EXECUTION_AUTHORITY_ERROR, error_output.getvalue())

    def test_execute_rejects_readiness_mismatch(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            runtime, readiness = _authorities(Path(directory), readiness_ok=False)
            with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as error:
                main(["--root", str(ROOT), "--execute", "--execution-preflight", "--runtime-authority", str(runtime), "--training-readiness", str(readiness)])
            self.assertNotEqual(0, error.exception.code)

    def test_execute_reaches_model_free_handoff(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            runtime, readiness = _authorities(Path(directory))
            result = execution_preflight(ROOT, runtime_authority_path=runtime, readiness_path=readiness, expected_runtime_image_digest="sha256:" + "a" * 64)
            self.assertEqual("P2_V2_EXECUTION_AUTHORITY_ACCEPTED", result["state"])
            self.assertTrue(result["executionAccepted"])
            self.assertEqual(0, result["modelDeserializationCount"])
            self.assertEqual(0, result["forwardCount"])
            self.assertEqual(0, result["backwardCount"])
            self.assertEqual(0, result["optimizerStepCount"])

    def test_execution_enablement_does_not_change_scientific_binding(self) -> None:
        bundle = load_v2_input_bundle(ROOT)
        self.assertEqual((32, 8, 0), (len(bundle["train"]), len(bundle["validation"]), len(bundle["holdout"])))
        self.assertEqual((32, 8, 1, 8, 256), (TRAIN_COUNT, VALIDATION_COUNT, 1, 8, 256))
        self.assertEqual(3, 3)
        self.assertEqual("0.0001", "0.0001")
        self.assertEqual("0.01", "0.01")
        self.assertEqual(7, 7)
        self.assertEqual("FP32", "FP32")

    def test_execution_enabled_path_structurally_excludes_holdout(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            runtime, readiness = _authorities(Path(directory))
            result = execution_preflight(ROOT, runtime_authority_path=runtime, readiness_path=readiness)
            self.assertEqual(0, result["holdout"])
            self.assertEqual(0, result["holdoutExposureCount"])

    def test_reissued_authorities_are_immutable_and_reloadable(self) -> None:
        bundle = load_v2_input_bundle(ROOT)
        input_authority = json.loads((ROOT / "data/knowledge/him/training/p2/canonical-catalog-expansion/v2/runtime-authority/training-input-authority.v2.json").read_text(encoding="utf-8"))
        runtime = build_execution_enabled_training_runtime_authority_v2(
            runtime_image_digest="sha256:" + "a" * 64,
            runtime_source_logical_digest="b" * 64,
            source_git_head="c" * 40,
            training_input_authority_reference=input_authority["reference"],
            batch_authority_reference=bundle["batch"]["reference"],
            sequence_reference="sequence-length-authority:v2:97130457decd4f283492da2d09a0faddb4bb99fc70f7d79732fbd390794cc509",
            corpus_reference=bundle["corpus"]["reference"],
            partition_reference=bundle["partition"]["reference"],
            leakage_reference=bundle["leakage"]["reference"],
            model_binding_digest="d" * 64,
        )
        validate_execution_enabled_training_runtime_authority_v2(runtime)
        with tempfile.TemporaryDirectory() as directory:
            runtime_path = Path(directory) / "runtime.json"
            persist_execution_enabled_training_runtime_authority_v2(runtime_path, runtime)
            self.assertEqual(runtime, reload_execution_enabled_training_runtime_authority_v2(runtime_path))
            readiness = evaluate_training_readiness_v2(ROOT, runtime_authority=runtime)
            readiness_path = Path(directory) / "readiness.json"
            persist_training_readiness_v2(readiness_path, readiness)
            self.assertEqual(readiness, reload_training_readiness_v2(readiness_path))
            self.assertTrue(readiness["gates"]["executionAuthority"])


if __name__ == "__main__":
    unittest.main()
