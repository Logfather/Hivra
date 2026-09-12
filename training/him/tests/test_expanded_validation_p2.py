import copy
import contextlib
import json
import pathlib
import io
import sys
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from him_trainer.expanded_validation_p2 import (
    EXPECTED_AUTHORITY_REFERENCES,
    EXPECTED_CANDIDATE_REFERENCE,
    EXPECTED_CHECKPOINT_DIGEST,
    EXPECTED_CHECKPOINT_REFERENCE,
    EXPECTED_RUNTIME_BINDING,
    EXPECTED_TOTAL,
    ExpandedEvaluationModelInput,
    ExpandedValidationContractError,
    EvaluationExecutionGuards,
    build_aggregate_evidence,
    build_dry_run,
    build_evaluation_model_input,
    build_per_example_evidence,
    build_packet_manifest,
    execute_evaluation,
    extract_evaluation_predictions,
    load_authorities,
    _load_model_state_read_only,
    resolve_expanded_examples,
    packet_secret_scan,
    run_cli,
    validate_holdout_exclusion,
    validate_evaluation_execution_binding,
    validate_historical_leakage,
    validate_packet_manifest,
)


ROOT = pathlib.Path(__file__).resolve().parents[3]
AUTHORITY_ROOT = ROOT / "data/knowledge/him/training/expansions/p2/evaluation-expansion/v1"
CANDIDATE = "p2-trained-candidate:v1:f3c4105735ee742c53638784bf07fb19db11df52456a11d5918176030c61ee00"
CHECKPOINT = "him-training-checkpoint:v2:29312dac2a6524d1ca2e20563831d19346ae5cd831610db5a2bf160229437622"
PACKET_ROOT = ROOT / "data/knowledge/him/training/a100-transfer/p2-expanded-validation/v1/46553e60e94908c2d73585ee514758b96fc2d33b18d39027dc989ad7a974044d-adapter-v1/packet"
HISTORICAL_PACKET_ROOT = ROOT / "data/knowledge/him/training/a100-transfer/p2-expanded-validation/v1/46553e60e94908c2d73585ee514758b96fc2d33b18d39027dc989ad7a974044d/packet"


class ExpandedValidationP2Test(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.bundle = load_authorities(AUTHORITY_ROOT)
        cls.examples = resolve_expanded_examples(cls.bundle)

    def test_exact_input_counts(self):
        self.assertEqual(len(self.examples), 19)
        self.assertEqual(sum(x.primary_active for x in self.examples), 13)
        self.assertEqual(sum(x.secondary_only for x in self.examples), 6)
        self.assertEqual(sum(x.secondary_active for x in self.examples), 19)
        self.assertEqual(sum(x.primary_target == "IDENTITY" for x in self.examples), 6)
        self.assertEqual(sum(x.primary_target == "VARIANT" for x in self.examples), 7)
        self.assertEqual(sum(x.secondary_target == "COMPATIBLE" for x in self.examples), 13)
        self.assertEqual(sum(x.secondary_target == "REJECT" for x in self.examples), 6)

    def test_authority_references_and_order_are_frozen(self):
        for name, reference in EXPECTED_AUTHORITY_REFERENCES.items():
            self.assertEqual(self.bundle.as_dict()[name]["reference"], reference)
        first = [x.evaluation_example_reference for x in self.examples]
        second = [x.evaluation_example_reference for x in resolve_expanded_examples(self.bundle)]
        self.assertEqual(first, second)
        self.assertEqual([x.ordering_index for x in self.examples], list(range(19)))

    def test_secondary_only_masks(self):
        rejects = [x for x in self.examples if x.secondary_target == "REJECT"]
        self.assertEqual(len(rejects), 6)
        self.assertTrue(all(x.primary_mask == 0 and x.secondary_mask == 1 and x.primary_target is None for x in rejects))
        self.assertEqual(sum(x.primary_target is not None for x in rejects), 0)

    def test_holdout_isolation(self):
        self.assertEqual(validate_holdout_exclusion(self.examples, self.bundle.holdout), {"example": 0, "sourceRecord": 0, "family": 0})
        holdout = copy.deepcopy(self.bundle.holdout)
        holdout["records"][0]["familyReference"] = self.examples[0].family_reference
        with self.assertRaises(ExpandedValidationContractError):
            validate_holdout_exclusion(self.examples, holdout)

    def test_historical_leakage_isolation(self):
        self.assertTrue(all(v == 0 for v in validate_historical_leakage(self.examples).values()))
        with self.assertRaises(ExpandedValidationContractError):
            validate_historical_leakage(self.examples, train_examples=[self.examples[0].evaluation_example_reference])

    def test_evidence_skeleton_has_no_predictions_or_logits(self):
        records = build_per_example_evidence(self.examples, candidate_reference=CANDIDATE, checkpoint_reference=CHECKPOINT, expanded_authority_reference=self.bundle.expanded["reference"], limitations_reference=self.bundle.limitations["reference"], execution_reference="p2-expanded-validation-execution:v1:NOT_EXECUTED")
        self.assertEqual(len(records), EXPECTED_TOTAL)
        self.assertTrue(all(r["secondaryPrediction"] == "NOT_EXECUTED" for r in records))
        self.assertFalse(any("primaryLogits" in r or "secondaryLogits" in r for r in records))

    def test_aggregate_confusion_contract(self):
        aggregate = build_aggregate_evidence(self.examples, candidate_reference=CANDIDATE, checkpoint_reference=CHECKPOINT, expanded_authority_reference=self.bundle.expanded["reference"], limitations_reference=self.bundle.limitations["reference"], execution_reference="p2-expanded-validation-execution:v1:NOT_EXECUTED", leakage={"all": 0}, holdout_overlap={"all": 0})
        self.assertEqual(aggregate["primaryMetrics"]["denominator"], 13)
        self.assertEqual(aggregate["secondaryMetrics"]["denominator"], 19)
        self.assertEqual(set(aggregate["secondaryConfusion"]), {"COMPATIBLE", "REJECT"})
        self.assertEqual(set(aggregate["primaryConfusion"]), {"IDENTITY", "VARIANT"})
        self.assertEqual(aggregate["unseenRejectAnalysis"]["boundaryDiversity"], "2/3")

    def test_dry_run_round_trip_and_zero_execution(self):
        report = build_dry_run(authority_root=AUTHORITY_ROOT, candidate_reference=CANDIDATE, checkpoint_reference=CHECKPOINT, checkpoint_digest="a3a152b77b3ca51f356b262267d6230533f8b9ea9031f48e0e813e415a7e599e", execution_reference="p2-expanded-validation-execution:v1:NOT_EXECUTED")
        self.assertEqual(report["state"], "DRY_RUN_COMPLETE")
        self.assertEqual(report["counts"]["total"], 19)
        self.assertEqual(report["executionCounters"], {"modelDeserialization": 0, "forward": 0, "backward": 0, "optimizer": 0, "gpu": 0, "runpod": 0})
        encoded = json.dumps({k: report[k] for k in report if k not in ("logicalDigest", "reference")}, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
        self.assertEqual(report["logicalDigest"], __import__("hashlib").sha256(encoded).hexdigest())

    def test_guards_reject_mutation_paths(self):
        guards = EvaluationExecutionGuards(backward_count=1)
        with self.assertRaises(ExpandedValidationContractError):
            guards.require_evaluation_only()

    def test_packet_offline_reload(self):
        manifest = json.loads((PACKET_ROOT / "packet-manifest.v1.json").read_text(encoding="utf-8"))
        validate_packet_manifest(PACKET_ROOT, manifest)
        self.assertEqual(manifest["fileCount"], 17)
        self.assertEqual(packet_secret_scan(PACKET_ROOT), 0)
        self.assertTrue((PACKET_ROOT / "runtime/him_trainer/expanded_validation_p2.py").is_file())
        self.assertEqual(
            (PACKET_ROOT / "runtime/him_trainer/expanded_validation_p2.py").read_bytes(),
            pathlib.Path(ROOT / "training/him/src/him_trainer/expanded_validation_p2.py").read_bytes(),
        )
        packet_authority = json.loads((PACKET_ROOT / "authority/evaluation-authorities.v1.json").read_text(encoding="utf-8"))
        self.assertEqual(len(packet_authority["authorities"]), 7)
        self.assertFalse(packet_authority["holdoutExecutableInputIncluded"])
        packet_input = json.loads((PACKET_ROOT / "evaluation/expanded-validation-input.v1.json").read_text(encoding="utf-8"))
        self.assertEqual(packet_input["recordCount"], 19)
        self.assertEqual([record["orderingIndex"] for record in packet_input["records"]], list(range(19)))
        self.assertEqual(json.loads((PACKET_ROOT / "holdout/exclusion-binding.v1.json").read_text(encoding="utf-8"))["executableInputIncluded"], False)
        self.assertEqual(json.loads((PACKET_ROOT / "packet-readiness.v1.json").read_text(encoding="utf-8"))["state"], "READY")
        historical_manifest = json.loads((HISTORICAL_PACKET_ROOT / "packet-manifest.v1.json").read_text(encoding="utf-8"))
        validate_packet_manifest(HISTORICAL_PACKET_ROOT, historical_manifest)
        self.assertNotEqual(
            (HISTORICAL_PACKET_ROOT / "runtime/him_trainer/expanded_validation_p2.py").read_bytes(),
            pathlib.Path(ROOT / "training/him/src/him_trainer/expanded_validation_p2.py").read_bytes(),
        )

    def test_cli_dry_run_entrypoint_is_model_free(self):
        stdout = io.StringIO()
        with tempfile.TemporaryDirectory() as directory, contextlib.redirect_stdout(stdout):
            result = run_cli(["--packet-root", str(PACKET_ROOT), "--output-root", directory])
            output = json.loads((pathlib.Path(directory) / "expanded-validation-dry-run.v1.json").read_text(encoding="utf-8"))
        self.assertEqual(result, 0)
        self.assertEqual(output["state"], "DRY_RUN_COMPLETE")
        self.assertEqual(output["counts"]["total"], 19)
        self.assertEqual(output["executionCounters"], {"modelDeserialization": 0, "forward": 0, "backward": 0, "optimizer": 0, "gpu": 0, "runpod": 0})
        self.assertNotIn("torch", sys.modules)

    def test_cli_required_arguments_and_unknown_arguments_fail_closed(self):
        with self.assertRaises(SystemExit) as missing:
            run_cli([])
        self.assertEqual(missing.exception.code, 2)
        with self.assertRaises(SystemExit) as unknown:
            run_cli(["--packet-root", str(PACKET_ROOT), "--output-root", "/tmp/him-output", "--unknown"])
        self.assertEqual(unknown.exception.code, 2)

    def test_cli_invalid_packet_and_output_roots_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            output_root = pathlib.Path(directory) / "output"
            self.assertEqual(run_cli(["--packet-root", str(pathlib.Path(directory) / "missing"), "--output-root", str(output_root)]), 1)
            invalid_output = pathlib.Path(directory) / "file"
            invalid_output.write_text("occupied", encoding="utf-8")
            self.assertEqual(run_cli(["--packet-root", str(PACKET_ROOT), "--output-root", str(invalid_output)]), 1)

    def test_cli_execute_flag_reaches_evaluation_adapter_path(self):
        counters = {
            "model_deserialization_count": 1,
            "forward_count": 1,
            "backward_count": 0,
            "optimizer_step_count": 0,
            "holdout_forward_count": 0,
        }
        with tempfile.TemporaryDirectory() as directory:
            with patch("him_trainer.expanded_validation_p2.execute_evaluation", return_value={"counters": counters}) as execute:
                result = run_cli(["--packet-root", str(PACKET_ROOT), "--output-root", directory, "--execute"])
        self.assertEqual(result, 0)
        execute.assert_called_once()
        self.assertFalse((pathlib.Path(directory) / "expanded-validation-dry-run.v1.json").exists())

    def test_evaluation_execution_binding_accepts_frozen_candidate_checkpoint_and_runtime(self):
        candidate = json.loads((PACKET_ROOT / "candidate/frozen-candidate-checkpoint-binding.v1.json").read_text(encoding="utf-8"))
        runtime = json.loads((PACKET_ROOT / "runtime/runtime-binding.v1.json").read_text(encoding="utf-8"))
        validate_evaluation_execution_binding(candidate, runtime)
        self.assertEqual(candidate["candidateReference"], EXPECTED_CANDIDATE_REFERENCE)
        self.assertEqual(candidate["checkpointReference"], EXPECTED_CHECKPOINT_REFERENCE)
        self.assertEqual(candidate["checkpointLogicalDigest"], EXPECTED_CHECKPOINT_DIGEST)
        for field, expected in EXPECTED_RUNTIME_BINDING.items():
            self.assertEqual(runtime[field], expected)
        self.assertEqual(runtime["evaluatorSource"], "training/him/src/him_trainer/expanded_validation_p2.py")

    def test_evaluation_execution_binding_rejects_candidate_checkpoint_digest_and_runtime_mutations(self):
        candidate = json.loads((PACKET_ROOT / "candidate/frozen-candidate-checkpoint-binding.v1.json").read_text(encoding="utf-8"))
        runtime = json.loads((PACKET_ROOT / "runtime/runtime-binding.v1.json").read_text(encoding="utf-8"))
        for field, value in (
            ("candidateReference", "wrong-candidate"),
            ("checkpointReference", "wrong-checkpoint"),
            ("checkpointLogicalDigest", "0" * 64),
            ("modelDeserializationThisMission", True),
        ):
            broken = copy.deepcopy(candidate)
            broken[field] = value
            with self.subTest(field=field):
                with self.assertRaises(ExpandedValidationContractError):
                    validate_evaluation_execution_binding(broken, runtime)
        broken_runtime = copy.deepcopy(runtime)
        broken_runtime["pytorch"] = "wrong"
        with self.assertRaises(ExpandedValidationContractError):
            validate_evaluation_execution_binding(candidate, broken_runtime)

    def test_evaluation_model_input_projection_is_deterministic(self):
        from him_trainer.point12_token_tensor_builder_v1 import load_pinned_xlm_r_tokenizer_v1

        tokenizer = load_pinned_xlm_r_tokenizer_v1()
        first = build_evaluation_model_input(self.examples, tokenizer)
        second = build_evaluation_model_input(self.examples, tokenizer)
        import torch

        self.assertTrue(torch.equal(first.input_ids, second.input_ids))
        self.assertTrue(torch.equal(first.attention_mask, second.attention_mask))
        self.assertEqual(first.examples, second.examples)

    def test_evaluation_input_matches_training_tensor_semantics_for_identity_variant_and_secondary_only_reject(self):
        from him_trainer.point12_token_tensor_builder_v1 import load_pinned_xlm_r_tokenizer_v1
        from him_trainer.productive_training_p2_real import PRIMARY_ENCODING, P2Example, _tensor_view
        import torch

        tokenizer = load_pinned_xlm_r_tokenizer_v1()
        training_items = tuple(
            P2Example(
                reference=item.evaluation_example_reference,
                family=item.family_reference,
                candidate_id=item.canonical_id,
                candidate_name=item.canonical_name,
                observed_term=item.observed_term,
                target_kind=item.primary_target,
                relation=item.relation,
                compatibility=item.secondary_target,
                primary_target=PRIMARY_ENCODING[item.primary_target] if item.primary_active else 0,
                secondary_target=1 if item.secondary_target == "COMPATIBLE" else 0,
                primary_mask=float(item.primary_mask),
                secondary_mask=float(item.secondary_mask),
                source_record_reference=item.source_record_reference,
                source_evidence_reference=item.source_evidence_reference,
            )
            for item in self.examples
        )
        training_view = _tensor_view(training_items, tokenizer)
        evaluation_view = build_evaluation_model_input(self.examples, tokenizer)
        self.assertTrue(torch.equal(training_view.input_ids, evaluation_view.input_ids))
        self.assertTrue(torch.equal(training_view.attention_mask, evaluation_view.attention_mask))
        for label, item in (
            ("identity", next(x for x in self.examples if x.primary_target == "IDENTITY")),
            ("variant", next(x for x in self.examples if x.primary_target == "VARIANT")),
            ("secondary-only reject", next(x for x in self.examples if x.secondary_target == "REJECT")),
        ):
            with self.subTest(label=label):
                index = item.ordering_index
                self.assertTrue(torch.equal(training_view.input_ids[index], evaluation_view.input_ids[index]))
                self.assertTrue(torch.equal(training_view.attention_mask[index], evaluation_view.attention_mask[index]))

    def test_read_only_model_state_loader_uses_no_optimizer_or_persistence(self):
        import hashlib
        import torch

        class EvalModel:
            training = False

            def __init__(self):
                self.loaded_state = None

            def load_state_dict(self, state, strict=True):
                self.loaded_state = (state, strict)

        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            manifest_path = root / "checkpoint-manifest.json"
            model_path = root / "model-state.pt"
            optimizer_path = root / "optimizer-state.pt"
            model_path.write_bytes(b"model-state-placeholder")
            optimizer_path.write_bytes(b"optimizer-state-placeholder")
            manifest = {
                "checkpointReference": EXPECTED_CHECKPOINT_REFERENCE,
                "checkpointLogicalDigest": EXPECTED_CHECKPOINT_DIGEST,
                "authorityBindings": {"runtimeIdentity": "runtime", "optimizerIdentity": "optimizer"},
                "optimizerStep": 12,
            }
            manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
            candidate = {
                "checkpointManifestSha256": hashlib.sha256(manifest_path.read_bytes()).hexdigest(),
                "modelStatePath": str(model_path),
            }
            model = EvalModel()
            with patch("him_trainer.checkpoint_v2._strict_manifest_gate", return_value=(manifest, model_path, optimizer_path)) as gate:
                with patch.object(torch, "load", return_value={"weight": torch.tensor([1.0])}) as load:
                    loaded = _load_model_state_read_only(model, manifest_path, candidate)
            self.assertIs(loaded, model)
            gate.assert_called_once()
            load.assert_called_once_with(model_path, map_location="cpu", weights_only=True)
            self.assertEqual(model.loaded_state[1], True)
            self.assertFalse(hasattr(model, "optimizer"))
            self.assertEqual(optimizer_path.read_bytes(), b"optimizer-state-placeholder")

    def test_evaluation_predictions_preserve_raw_two_head_logits_and_shapes(self):
        import torch

        primary_logits = torch.arange(EXPECTED_TOTAL * 5, dtype=torch.float32).reshape(EXPECTED_TOTAL, 5)
        secondary_logits = torch.arange(EXPECTED_TOTAL * 2, dtype=torch.float32).reshape(EXPECTED_TOTAL, 2)

        class EvalModel:
            training = False
            execution_device = torch.device("cpu")

            def __call__(self, input_ids, attention_mask):
                self.seen_input_ids = input_ids
                self.seen_attention_mask = attention_mask
                return SimpleNamespace(primary_logits=primary_logits, secondary_logits=secondary_logits)

        model_input = ExpandedEvaluationModelInput(
            input_ids=torch.zeros((EXPECTED_TOTAL, 4), dtype=torch.int64),
            attention_mask=torch.ones((EXPECTED_TOTAL, 4), dtype=torch.int64),
            examples=self.examples,
        )
        result = extract_evaluation_predictions(EvalModel(), model_input)
        self.assertIs(result["primaryLogits"], primary_logits)
        self.assertIs(result["secondaryLogits"], secondary_logits)
        self.assertEqual(tuple(result["primaryLogits"].shape), (19, 5))
        self.assertEqual(tuple(result["secondaryLogits"].shape), (19, 2))
        self.assertTrue(torch.equal(result["primaryPredictions"], primary_logits.argmax(dim=1) + 1))
        self.assertTrue(torch.equal(result["secondaryPredictions"], secondary_logits.argmax(dim=1)))

    def test_evaluation_execution_is_eval_only_and_keeps_backward_optimizer_and_holdout_zero(self):
        class EvalModel:
            training = False

        guards = EvaluationExecutionGuards()
        predictions = {"primaryLogits": "raw", "secondaryLogits": "raw"}
        result = execute_evaluation(
            examples=self.examples,
            model_loader=EvalModel,
            input_builder=lambda items: (items, "model-input"),
            prediction_fn=lambda model, model_input: predictions,
            guards=guards,
        )
        self.assertIs(result["predictions"], predictions)
        self.assertEqual(result["counters"]["backward_count"], 0)
        self.assertEqual(result["counters"]["optimizer_step_count"], 0)
        self.assertEqual(result["counters"]["holdout_forward_count"], 0)
        self.assertEqual(result["counters"]["holdout_model_exposure_count"], 0)

    def test_cli_output_preserves_holdout_exclusion_and_existing_evaluator_contract(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual(run_cli(["--packet-root", str(PACKET_ROOT), "--output-root", directory]), 0)
            output = json.loads((pathlib.Path(directory) / "expanded-validation-dry-run.v1.json").read_text(encoding="utf-8"))
        self.assertEqual(output["holdout"], {"allowed": False, "overlap": {"example": 0, "sourceRecord": 0, "family": 0}})
        self.assertEqual(output["counts"], {"total": 19, "primaryActive": 13, "primaryInactive": 6, "secondaryActive": 19, "identity": 6, "variant": 7, "compatible": 13, "reject": 6})

    def test_fail_closed_count_and_binding_mutations(self):
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"] = expanded["records"][:-1]
        broken = type(self.bundle)(self.bundle.relation, self.bundle.primary, self.bundle.compatibility, self.bundle.boundary, expanded, self.bundle.holdout, self.bundle.limitations)
        with self.assertRaises(ExpandedValidationContractError):
            resolve_expanded_examples(broken)

    def test_negative_matrix_60_fail_closed_cases(self):
        def bundle_with(**changes):
            values = {name: copy.deepcopy(getattr(self.bundle, name)) for name in ("relation", "primary", "compatibility", "boundary", "expanded", "holdout", "limitations")}
            values.update(changes)
            return type(self.bundle)(**values)

        def expect_resolve(label, bundle):
            with self.subTest(label=label):
                with self.assertRaises(ExpandedValidationContractError):
                    resolve_expanded_examples(bundle)

        def expect_load(label, authority_name, mutate):
            with self.subTest(label=label):
                with tempfile.TemporaryDirectory() as directory:
                    root = pathlib.Path(directory)
                    for name, filename in {
                        "relation": "p2-final-relation-authority.v1.json",
                        "primary": "p2-final-primary-target-authority.v1.json",
                        "compatibility": "p2-final-candidate-compatibility-authority.v1.json",
                        "boundary": "p2-final-negative-boundary-authority.v1.json",
                        "expanded": "p2-expanded-validation-authority.v1.json",
                        "holdout": "p2-family-isolated-holdout-authority.v1.json",
                        "limitations": "p2-evaluation-limitations-authority.v1.json",
                    }.items():
                        value = copy.deepcopy(getattr(self.bundle, name))
                        if name == authority_name:
                            mutate(value)
                        (root / filename).write_text(json.dumps(value), encoding="utf-8")
                    with self.assertRaises(ExpandedValidationContractError):
                        load_authorities(root)

        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"] = expanded["records"][:-1]
        expect_resolve("01_total_18", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"].append(copy.deepcopy(expanded["records"][0]))
        expect_resolve("02_total_20", bundle_with(expanded=expanded))
        expect_load("03_wrong_identity_count", "expanded", lambda x: x.__setitem__("identityCount", 5))
        expect_load("04_wrong_variant_count", "expanded", lambda x: x.__setitem__("variantCount", 8))
        expect_load("05_wrong_reject_count", "expanded", lambda x: x.__setitem__("rejectCount", 5))
        expect_load("06_wrong_expanded_record_count", "expanded", lambda x: x.__setitem__("recordCount", 18))
        expect_load("07_wrong_holdout_record_count", "holdout", lambda x: x.__setitem__("recordCount", 5))
        expect_load("08_wrong_holdout_family_count", "holdout", lambda x: x.__setitem__("familyCount", 2))
        expect_load("09_holdout_not_frozen", "holdout", lambda x: x.__setitem__("frozen", False))
        expect_load("10_holdout_exposed", "holdout", lambda x: x.__setitem__("modelExposureCount", 1))
        expect_load("11_wrong_boundary_diversity", "limitations", lambda x: x.__setitem__("rejectBoundaryDiversity", "3/3"))
        expect_load("12_wrong_holdout_reject_coverage", "limitations", lambda x: x.__setitem__("holdoutRejectCoverage", 1))
        expect_load("13_wrong_boundary_count", "boundary", lambda x: x.__setitem__("boundaryTypeCount", 3))
        expect_load("14_fabricated_boundary", "boundary", lambda x: x.__setitem__("fabricatedCount", 1))
        for number, name in enumerate(("relation", "primary", "compatibility", "boundary", "expanded", "holdout", "limitations"), 15):
            expect_load(f"{number:02d}_wrong_{name}_reference", name, lambda x: x.__setitem__("reference", "wrong"))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][5]["primaryTarget"] = "IDENTITY"
        expect_resolve("22_reject_fabricated_primary", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][5]["primaryActive"] = True
        expect_resolve("23_reject_primary_mask_active", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["secondaryActive"] = False
        expect_resolve("24_secondary_mask_inactive", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["primaryActive"] = False
        expect_resolve("25_identity_primary_mask_inactive", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][17]["primaryActive"] = False
        expect_resolve("26_variant_primary_mask_inactive", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["relation"] = "IDENTITY_OF"
        expect_resolve("27_wrong_relation", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["relationAuthorityRecordReference"] = "wrong"
        expect_resolve("28_wrong_relation_authority", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["candidateCompatibilityAuthorityRecordReference"] = "wrong"
        expect_resolve("29_wrong_cc_authority", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["primaryTargetAuthorityRecordReference"] = "wrong"
        expect_resolve("30_wrong_primary_authority", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["negativeBoundaryAuthorityRecordReference"] = "wrong"
        expect_resolve("31_wrong_boundary_authority", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["sourceRecordReference"] = expanded["records"][1]["sourceRecordReference"]
        expect_resolve("32_duplicate_source_record", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][1]["recordReference"] = expanded["records"][0]["recordReference"]
        expect_resolve("33_duplicate_example_reference", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["candidateCompatibility"] = "UNKNOWN"
        expect_resolve("34_invalid_secondary_target", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["primaryTarget"] = "EXISTING_CANONICAL"
        expect_resolve("35_unexpected_primary_target", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["sourceEvidenceReference"] = ""
        expect_resolve("36_missing_source_evidence", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["reviewUnitReference"] = ""
        expect_resolve("37_missing_review_unit", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["familyReference"] = ""
        expect_resolve("38_missing_family", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["canonicalName"] = ""
        expect_resolve("39_missing_canonical_name", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["observedTerm"] = ""
        expect_resolve("40_missing_observed_term", bundle_with(expanded=expanded))
        holdout = copy.deepcopy(self.bundle.holdout)
        holdout["records"][0]["recordReference"] = self.examples[0].evaluation_example_reference
        with self.assertRaises(ExpandedValidationContractError):
            validate_holdout_exclusion(self.examples, holdout)
        holdout = copy.deepcopy(self.bundle.holdout)
        holdout["records"][0]["sourceRecordReference"] = self.examples[0].source_record_reference
        with self.assertRaises(ExpandedValidationContractError):
            validate_holdout_exclusion(self.examples, holdout)
        for number, field in enumerate(("train", "validation"), 43):
            with self.subTest(label=f"{number:02d}_historical_{field}_example"):
                with self.assertRaises(ExpandedValidationContractError):
                    validate_historical_leakage(self.examples, **{field + "_examples": [self.examples[0].evaluation_example_reference]})
        for number, field in enumerate(("train", "validation"), 45):
            with self.subTest(label=f"{number:02d}_historical_{field}_family"):
                with self.assertRaises(ExpandedValidationContractError):
                    validate_historical_leakage(self.examples, **{field + "_families": [self.examples[0].family_reference]})
        for number, kwargs in enumerate(({"candidate_reference": "wrong"}, {"checkpoint_reference": "wrong"}, {"checkpoint_digest": "wrong"}), 47):
            with self.subTest(label=f"{number:02d}_wrong_dry_run_binding"):
                args = dict(authority_root=AUTHORITY_ROOT, candidate_reference=CANDIDATE, checkpoint_reference=CHECKPOINT, checkpoint_digest="a3a152b77b3ca51f356b262267d6230533f8b9ea9031f48e0e813e415a7e599e", execution_reference="not-executed")
                args.update(kwargs)
                with self.assertRaises(ExpandedValidationContractError):
                    build_dry_run(**args)
        for number, guards in enumerate((
            {"backward_count": 1}, {"optimizer_step_count": 1}, {"parameter_mutation_count": 1}, {"checkpoint_mutation_count": 1}, {"holdout_model_exposure_count": 1},
        ), 50):
            with self.subTest(label=f"{number:02d}_forbidden_execution_path"):
                with self.assertRaises(ExpandedValidationContractError):
                    EvaluationExecutionGuards(**guards).require_evaluation_only()
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            (root / "entrypoint.txt").write_text("safe", encoding="utf-8")
            manifest = build_packet_manifest(root)
            self.assertEqual(manifest["fileCount"], 1)
            (root / "entrypoint.txt").write_text("RUNPOD_API_KEY", encoding="utf-8")
            with self.subTest(label="55_secret_scan"):
                self.assertGreater(packet_secret_scan(root), 0)
        with self.assertRaises(ExpandedValidationContractError):
            validate_holdout_exclusion(self.examples, {"records": self.bundle.holdout["records"], "modelExposureCount": 1})
        with self.assertRaises(ExpandedValidationContractError):
            load_authorities(AUTHORITY_ROOT / "missing")
        self.assertEqual(len(build_per_example_evidence(self.examples, candidate_reference=CANDIDATE, checkpoint_reference=CHECKPOINT, expanded_authority_reference=self.bundle.expanded["reference"], limitations_reference=self.bundle.limitations["reference"], execution_reference="not-executed")), 19)
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][0]["evaluationBucket"] = "FROZEN_HOLDOUT"
        expect_resolve("59_wrong_evaluation_bucket", bundle_with(expanded=expanded))
        expanded = copy.deepcopy(self.bundle.expanded)
        expanded["records"][1]["recordReference"] = expanded["records"][0]["recordReference"]
        expect_resolve("60_duplicate_evaluation_reference", bundle_with(expanded=expanded))
        compatibility = copy.deepcopy(self.bundle.compatibility)
        compatibility["records"][0]["candidateCompatibility"] = "REJECT"
        broken = type(self.bundle)(self.bundle.relation, self.bundle.primary, compatibility, self.bundle.boundary, self.bundle.expanded, self.bundle.holdout, self.bundle.limitations)
        with self.assertRaises(ExpandedValidationContractError):
            resolve_expanded_examples(broken)


if __name__ == "__main__":
    unittest.main()
