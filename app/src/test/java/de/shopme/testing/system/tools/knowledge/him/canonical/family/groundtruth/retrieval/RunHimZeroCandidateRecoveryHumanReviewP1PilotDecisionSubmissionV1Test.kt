package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidenceKindV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewReviewUnitV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1Test {
    @Test
    fun identityStateScopeAndReviewerAreFrozen() {
        val submission = submission()
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_SUBMISSION_V1",
            submission.contractId,
        )
        assertEquals("1", submission.version)
        assertEquals("HUMAN_REVIEWER_AUTHORIZED_UNPERSISTED", submission.state)
        assertEquals(
            "p1-artischocken-herzen-brie-double-creme-reviewer-logfather-r1-v1",
            submission.submissionId,
        )
        assertEquals("p1-artischocken-herzen-brie-double-creme-v1", submission.missionBinding.missionId)
        assertEquals(submission.missionBinding.missionId, submission.missionBinding.scopeId)
        assertEquals("reviewer:logfather:v1", submission.reviewerRef)
        assertEquals(1, submission.reviewRound)
        assertTrue(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.validate(submission).valid)
    }

    @Test
    fun fourDecisionsAndCountersAreExactlyTheHumanAuthorization() {
        val submission = submission()
        assertEquals(
            listOf(
                "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
                "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
                "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
                "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
            ),
            submission.selections.map { it.reviewUnitId },
        )
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            ),
            submission.selections.map { it.decision },
        )
        assertEquals(4, submission.counters.authorizedDecisionSelections)
        assertEquals(2, submission.counters.confirmAssociations)
        assertEquals(2, submission.counters.rejectAssociations)
        assertEquals(0, submission.counters.abstentions)
        assertEquals(0, submission.counters.escalations)
        assertEquals(12, submission.counters.selectedEvidenceReferences)
        assertEquals(12, submission.counters.uniqueEvidenceReferences)
        assertEquals(0, submission.counters.alternativeCanonicalProposals)
        assertEquals(1, submission.counters.reviewers)
        assertEquals(1, submission.counters.reviewRound)
        assertEquals(1, submission.counters.minimumRevision)
        assertEquals(1, submission.counters.maximumRevision)
    }

    @Test
    fun reasonAndEvidenceBindingsAreFrozenAndCanonicalized() {
        val submission = submission()
        assertEquals(
            listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED),
            submission.selections[0].reasonCodes,
        )
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH,
                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED,
            ),
            submission.selections[1].reasonCodes,
        )
        assertEquals(submission.selections[0].reasonCodes, submission.selections[2].reasonCodes)
        assertEquals(submission.selections[1].reasonCodes, submission.selections[3].reasonCodes)
        assertTrue(submission.selections.all { it.evidenceReferenceIds.size == 3 })
        assertEquals(
            12,
            submission.selections.flatMap { it.evidenceReferenceIds }.distinct().size,
        )
        assertTrue(submission.selections.all { it.evidenceReferenceIds == it.evidenceReferenceIds.sorted() })
        assertTrue(submission.selections.all { it.alternativeCanonicalProposal == null && it.reviewerNote == null })
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.FROZEN_SELECTIONS,
            submission.selections,
        )
    }

    @Test
    fun materializationProducesFourExistingValidDecisionRecordsInMissionOrder() {
        val result = materialize()
        val completed = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Completed>(result)
        assertEquals(4, completed.records.size)
        assertEquals(submission().selections.map { it.reviewUnitId }, completed.records.map { it.reviewUnit.reviewUnitId })
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION,
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
            ),
            completed.records.map { it.decision },
        )
        completed.records.forEach { record ->
            assertEquals("reviewer:logfather:v1", record.reviewerRef)
            assertEquals(1, record.reviewRound)
            assertEquals(1, record.revision)
            assertEquals(null, record.alternativeCanonicalProposal)
            assertEquals(null, record.reviewerNote)
            assertTrue(record.validate().valid)
        }
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.POSITIVE_GOLD_CANDIDATE_AFTER_INDEPENDENT_VALIDATION,
                HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.NEGATIVE_SUPERVISION_CANDIDATE_AFTER_INDEPENDENT_VALIDATION,
                HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.POSITIVE_GOLD_CANDIDATE_AFTER_INDEPENDENT_VALIDATION,
                HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.NEGATIVE_SUPERVISION_CANDIDATE_AFTER_INDEPENDENT_VALIDATION,
            ),
            completed.records.map { it.downstreamRoute },
        )
    }

    @Test
    fun bindingAndLogicalDigestsAreIndependentAndDeterministic() {
        val first = submission()
        val second = submission()
        assertEquals(first.submissionBindingDigest, second.submissionBindingDigest)
        assertEquals(first.submissionLogicalDigest, second.submissionLogicalDigest)
        assertEquals(first, second)
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.submissionBindingDigest(first),
            first.submissionBindingDigest,
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.submissionLogicalDigest(first),
            first.submissionLogicalDigest,
        )
        val changed = first.copy(reviewerRef = "reviewer:other:v1")
        val changedWithBindingDigest = changed.copy(
            submissionBindingDigest =
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.submissionBindingDigest(changed),
        )
        assertNotEquals(first.submissionBindingDigest, changedWithBindingDigest.submissionBindingDigest)
        assertNotEquals(
            first.submissionLogicalDigest,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.submissionLogicalDigest(changedWithBindingDigest),
        )
        assertFalse(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.validate(changedWithBindingDigest).valid)
    }

    @Test
    fun manipulatedSubmissionBindingsAndDecisionsFailClosed() {
        val original = submission()
        assertFailure(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.materialize(
                original.copy(corpusBindingDigest = "0".repeat(64)),
                supplement(),
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.INVALID_CORPUS_BINDING,
        )
        assertFailure(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.materialize(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.create(
                    original.selections.mapIndexed { index, selection ->
                        if (index == 0) selection.copy(decision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION) else selection
                    },
                ),
                supplement(),
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.DECISION_MISMATCH,
        )
        assertFailure(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.materialize(
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.create(
                    original.selections.mapIndexed { index, selection ->
                        if (index == 1) selection.copy(reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED)) else selection
                    },
                ),
                supplement(),
            ),
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1.REASON_CODES_MISMATCH,
        )
    }

    @Test
    fun missingExtraDuplicateCrossUnitAndWrongEvidenceSemanticsFailClosed() {
        val original = supplement()
        val firstBundle = original.bundles[0]
        val source = firstBundle.evidence.first { it.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION }
        val missing = original.copy(bundles = original.bundles.toMutableList().apply { this[0] = firstBundle.copy(evidence = firstBundle.evidence.drop(1)) })
        assertTrue(materialize(submission(), missing) is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Failed)
        val duplicate = original.copy(bundles = original.bundles.toMutableList().apply { this[0] = firstBundle.copy(evidence = firstBundle.evidence + source) })
        assertTrue(materialize(submission(), duplicate) is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Failed)
        val crossUnit = original.copy(bundles = original.bundles.toMutableList().apply {
            this[0] = firstBundle.copy(evidence = firstBundle.evidence.map { evidence ->
                if (evidence.kind == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION) evidence.copy(reviewUnitId = original.bundles[1].reviewUnit.reviewUnitId) else evidence
            })
        })
        assertTrue(materialize(submission(), crossUnit) is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Failed)
        val wrongKind = original.copy(bundles = original.bundles.toMutableList().apply {
            this[0] = firstBundle.copy(evidence = firstBundle.evidence.map { evidence ->
                if (evidence.evidenceReferenceId == "5912c6bde368803377441d627a04c74d1aecf08b43131b19d793196b32bf94f9") evidence.copy(kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD) else evidence
            })
        })
        assertTrue(materialize(submission(), wrongKind) is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Failed)
    }

    @Test
    fun reflectionShowsNoPersistenceRuntimeOrDownstreamAuthorizationFields() {
        val forbidden = setOf(
            "approval", "gold", "negativeSupervision", "training", "publication", "mutation",
            "timestamp", "clock", "random", "model", "recommendation", "decisionBatch",
        )
        val fields = listOf(
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1::class.java,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionCountersV1::class.java,
        ).flatMap { it.declaredFields.map { field -> field.name } }
        assertTrue(fields.none { name -> forbidden.any { token -> name.contains(token, ignoreCase = true) } })
        assertFalse(fields.contains("validationApproval"))
        assertFalse(fields.contains("trainingEligibility"))
        assertFalse(fields.contains("authorityMutation"))
    }

    @Test
    fun materializationIsRepeatableWithoutPersistenceOrRealInputs() {
        val first = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Completed>(materialize())
        val second = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Completed>(materialize())
        assertEquals(first.records, second.records)
        assertTrue(first.records.all { it.evidenceReferences.all { evidence -> !evidence.artifactReference.startsWith("/") } })
    }

    private fun submission() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.create()

    private fun materialize(
        submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1 = submission(),
        supplement: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 = supplement(),
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.materialize(submission, supplement)

    private fun assertFailure(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1,
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionFailureReasonV1,
    ) {
        assertEquals(expected, assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionMaterializationResultV1.Failed>(result).reason)
    }



    private fun supplement(): HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementV1 {
        val units = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.FROZEN_REVIEW_UNITS
        val bundles = units.mapIndexed { index, unit ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceBundleV1(
                unit,
                packetBinding(index),
                packetLogical(index),
                evidenceFor(unit.reviewUnitId),
                "",
                "",
            )
        }
        return HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.create(binding(), bundles)
    }

    private fun evidenceFor(
        unitId: String,
    ): List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1> = when (unitId) {
        "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e" -> {
            val fields0 = listOf(
            field("canonical.aliases", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
            field("canonical.entityId", "ZuhV5V", "45405269451baec97edcc9b6d58c120a8a8e677bcc2717a4e056bd7a6fa9bf72"),
            field("canonical.identities", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
            field("canonical.lifecycleStatus", "ACTIVE", "630c2f1c0ee1b8d7da57cf8936ae7e78274aba0bfd765fe10a20dfe580f9eecc"),
            field("canonical.name", "Artischocken", "d5bf10bffd48d29ed551d21155f939acffedb4025e9c5e93249fd2f4306d741b"),
            field("canonical.normalizedName", "artischocken", "66d6ef4b2c02afb5e6f50465cb2f089caf6d88ac60dcc56fe399c082c35089a7"),
            field("canonical.variants", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
        )
            val fields1 = listOf(
            field("projection.classification.labels[0]", "en:vegetarian", "f9b2ecb6ce0d2433b266da2a5346a4909efee62fa123c0094a47b9e506879126"),
            field("projection.classification.labels[1]", "en:vegan", "bf76b678e00b2efb5bf868e2774ddab4f14253a13b19c626d44869b3ffbd2469"),
            field("projection.classification.labels[2]", "en:no-lactose", "05fff9d9a8b45e6bf5627c9ae41f05347d58e06503026d33256a05d0777d59e3"),
            field("projection.classification.nova.tags[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.classification.nutriScore.grade", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.classification.nutriScore.version", "2023", "d398b29d3dbbb9bf201d4c7e1c19ff9d43c15fd45a0cec46fbe9885ec3f6e97f"),
            field("projection.environmentalEvidence.diagnostics.missingAgribalyseCategory", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingAgribalyseMatch", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingIngredients", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingLabels", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingOrigins", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingPackaging", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.status", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].origin", "en:unknown", "930e9cc4104236653b40c811ae2752246141fed58e65c4f00f2282c958dc769a"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].percent", "100.0", "43b87f618caab482ebe4976c92bcd6ad308b48055f1c27b4c574f3e31d7683e0"),
            field("projection.environmentalEvidence.origin.epiScore", "0.0", "8aed642bf5118b9d3c859bd4be35ecac75b6e873cce34e7b6f554b06f75550d7"),
            field("projection.environmentalEvidence.origin.epiValue", "-5.0", "d08da5e668569e4ba7d8e7f55d0dfb939bb31e645be387e05937306c5c39da0b"),
            field("projection.environmentalEvidence.origin.originsFromCategories[0]", "en:unknown", "930e9cc4104236653b40c811ae2752246141fed58e65c4f00f2282c958dc769a"),
            field("projection.environmentalEvidence.origin.originsFromSourceField[0]", "en:unknown", "930e9cc4104236653b40c811ae2752246141fed58e65c4f00f2282c958dc769a"),
            field("projection.environmentalEvidence.origin.warning", "origins_are_100_percent_unknown", "b02bd10bbb99ccad120f96e6c821d47744e2a12eb95134f3be037bbfa36d3037"),
            field("projection.environmentalEvidence.packaging.score", "81.0", "6380b14ebfde66dfec1aeb370d775d3c35cb61db2bfd988395924c2f55647bfb"),
            field("projection.environmentalEvidence.packaging.value", "-2.0", "060cf5e1089dc78b7234fdace727200a0105911e2867d17b7f15df8b963b855e"),
            field("projection.environmentalEvidence.packaging.warning", "unspecified_shape", "28fec6d498d293c3c2dc72d1035c1c118f45615594231d815fee9e576d75d82c"),
            field("projection.environmentalEvidence.productionSystem.value", "0.0", "8aed642bf5118b9d3c859bd4be35ecac75b6e873cce34e7b6f554b06f75550d7"),
            field("projection.environmentalEvidence.productionSystem.warning", "no_label", "2b069f022c225b12b341469c8dc78636faa659da27370256b7cc233338fc5daa"),
            field("projection.environmentalEvidence.score.grade", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.environmentalEvidence.score.tags[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.environmentalEvidence.threatenedSpecies.warning", "ingredients_missing", "0df20dd2821117dfa92a368f8f1a34401eff227c5b6a4a6b5957e3dd0bda8e12"),
            field("projection.geography.countries[0]", "en:germany", "67bddff782279ac98c3ba69d1d4c26feae3337a2307301d6dcd1013b2d6c5fa1"),
            field("projection.identity.brands[0]", "xx:feinkost-dittmann", "42203efa8ba7e72df1a4e90d4475e831a608a9bb35f7234b9df375b42012ae2c"),
            field("projection.identity.productName", "Artischocken Herzen", "116e55725814811c8cb79c29e853ebba8a1e4f281a738e6d2d9c15b0a91b7a82"),
            field("projection.identity.productNameEnglish", "Artischocken Herzen", "116e55725814811c8cb79c29e853ebba8a1e4f281a738e6d2d9c15b0a91b7a82"),
            field("projection.identity.productNameGerman", "Artischocken Herzen", "116e55725814811c8cb79c29e853ebba8a1e4f281a738e6d2d9c15b0a91b7a82"),
            field("projection.identity.productType", "food", "c1f026582fe6e8cb620d0c85a72fe421ddded756662a8ec00ed4c297ad10676b"),
            field("projection.identity.quantity", "330g", "25b8a66541facd737737e19187c1d4148f2a4c649890107b39ab18538fd7130d"),
            field("projection.packagingEvidence.hierarchy[0]", "en:glass", "0088bd98cf39d3c6e2dd970b7ba45b52c6998c03805415f498ff94515ed4cf52"),
            field("projection.packagingEvidence.items[0].foodContact", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.packagingEvidence.items[0].material", "en:glass", "0088bd98cf39d3c6e2dd970b7ba45b52c6998c03805415f498ff94515ed4cf52"),
            field("projection.packagingEvidence.materials[0]", "en:glass", "0088bd98cf39d3c6e2dd970b7ba45b52c6998c03805415f498ff94515ed4cf52"),
            field("projection.packagingEvidence.taxonomy[0]", "en:glass", "0088bd98cf39d3c6e2dd970b7ba45b52c6998c03805415f498ff94515ed4cf52"),
            field("projection.quality.complete", "false", "fcbcf165908dd18a9e49f7ff27810176db8e9f63b4352213741664245224f8aa"),
            field("projection.quality.completeness", "0.575", "c4668f664649a1d42d34f29f270b4ca65d4e5ac3b78f624cfb42bd3e27cc16ec"),
            field("projection.quality.info[0]", "en:packaging-data-incomplete", "3626d4a2a66fca14113e568a55af1fba5016b933b3369dcd4fbd095faf08db26"),
            field("projection.quality.info[1]", "en:food-groups-1-unknown", "c34697bdf06cef0af785c432e0be71b69c287af9c79f0d6fc8284daac194e474"),
            field("projection.quality.info[2]", "en:food-groups-2-unknown", "064c29a3a716a6ef52a62a8c8ec2186b2bcf2c4b49b444d7d240414bd5474853"),
            field("projection.quality.info[3]", "en:food-groups-3-unknown", "634d6b1264a8ab32857b25fc3ec1426b92f017d7318761da224ac06d5540c319"),
            field("projection.quality.scans", "2", "d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35"),
            field("projection.quality.uniqueScans", "2", "d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35"),
            field("projection.quality.warnings[0]", "en:environmental-score-origins-of-ingredients-origins-are-100-percent-unknown", "1b32ba6e1e7a2e7caf34c5139cc0358e1836e24ac0cfb6d11b5bc17585def684"),
            field("projection.quality.warnings[1]", "en:environmental-score-packaging-unspecified-shape", "ed62563531b4eb37b28f597aee6a5c8c5d9e3000ce38fae58ae4fc38da8c5681"),
            field("projection.quality.warnings[2]", "en:environmental-score-production-system-no-label", "5636a82167d716d3740742ab6562de4a85d3d271132e6ee0cdb5f644428b3615"),
            field("projection.quality.warnings[3]", "en:environmental-score-threatened-species-ingredients-missing", "b9b4cea74abda7ed0c89c895f30c35ec9927c9fec51063166674e3e20af0c92a"),
            field("projection.rowOrdinal", "431650", "5c6cfbf3e06a4e6f815a25c6f277136e4d3266f4263c6243fc16d0f302859895"),
            field("projection.source.code", "4002239680509", "2a7a6a389c88cb0b38c35eb9464ad2d1e9be37580eba5947f18076c70193c496"),
            field("projection.taxonomy.categories[0]", "de:artischocken-herzen", "a93dcecc358a48419fc24cfa31fb22b23661090cb1077be59084bd1227fee857"),
            field("projection.taxonomy.categoryHierarchy[0]", "de:artischocken herzen", "a7fa451e9552ffdd01cd80a3467cdd1a94164270648c6ddce23e43a96c601341"),
            field("projection.taxonomy.ciqualReferences[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.taxonomy.pnnsGroups[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
        )
            val fields2 = listOf(
            field("canonical.entityId", "ZuhV5V", "45405269451baec97edcc9b6d58c120a8a8e677bcc2717a4e056bd7a6fa9bf72"),
            field("canonical.name", "Artischocken", "d5bf10bffd48d29ed551d21155f939acffedb4025e9c5e93249fd2f4306d741b"),
            field("canonical.normalizedName", "artischocken", "66d6ef4b2c02afb5e6f50465cb2f089caf6d88ac60dcc56fe399c082c35089a7"),
            field("canonical.taxonomyPaths", "[[vegetables, mediterranean-vegetables, artichokes]]", "ae256fb3f0ae54511b4919a69dba9e1aff77acca5fa1152f97a59d5ec6be14f3"),
        )
            listOf(
                card(
                    unitId = "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
                    evidenceReferenceId = "49c134d1fd2b99ae2c164cffdc5e553a6364f2cabfea7f057bb58e613aaef9f4",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
                    artifact = artifact("data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json", 532398, "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184", "d4a26bf27de8768498bea8277cfb903f572900af00fe499bff9f63dcbc44ca7c"),
                    recordReference = "authority:ZuhV5V",
                    fields = fields0,
                    sourceProjection = null,
                ),
                card(
                    unitId = "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
                    evidenceReferenceId = "5912c6bde368803377441d627a04c74d1aecf08b43131b19d793196b32bf94f9",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
                    artifact = artifact("data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", 25551749120, "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df", "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"),
                    recordReference = "off:product:row:431650:code:4002239680509",
                    fields = fields1,
                    sourceProjection = projection(
                        source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                        recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
                        artifact = artifact("data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", 25551749120, "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df", "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"),
                        recordReference = "off:product:row:431650:code:4002239680509",
                        fields = fields1,
                        sourceOriginArtifact = artifact("data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", 0, "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236", null),
                    ),
                ),
                card(
                    unitId = "36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e",
                    evidenceReferenceId = "9ecfd9e8e601ffdb1f60620ff204882a32bbbfc355cd6b9b79f403866a40d9bd",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
                    artifact = artifact("data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json", 266953, "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f", "6ffd1f6c08d3f2f9d02aba427540d99a94d8461517bf6ea24e08f342fff5406e"),
                    recordReference = "catalog:ZuhV5V",
                    fields = fields2,
                    sourceProjection = null,
                ),
            )
        }
        "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7" -> {
            val fields0 = listOf(
            field("canonical.entityId", "rVnyq7", "f75957cd418023c9b49642b5390c4b46bdc244798eea0d6715c9148bf1e8ca42"),
            field("canonical.name", "Crème double", "8e1ea30e61a9677227a32087a8feb540ad7cbd56c2cf06dcdedfeab13024ae88"),
            field("canonical.normalizedName", "creme-double", "efa6e8e8e4f3b55370f349fd5c44b9193defd93154d4ca005abad1f82f0b8c0c"),
            field("canonical.taxonomyPaths", "[[dairy-and-eggs, dairy-products, cream, double-cream]]", "3eb00e2dfd39b26075033e8994b1221ee3b24402d5ce44347bc03a5c11c90b22"),
        )
            val fields1 = listOf(
            field("projection.classification.nova.tags[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.classification.nutriScore.grade", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.classification.nutriScore.version", "2021", "1bea20e1df19b12013976de2b5e0e3d1fb4ba088b59fe53642c324298b21ffd9"),
            field("projection.environmentalEvidence.score.legacyEcoScoreGrade", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.geography.countries[0]", "en:united-states", "5f26d8c0a65785d8930d9c2284abcf8495ad2c79a11f9515e7064f1365fcf097"),
            field("projection.identity.productName", "Brie double crème", "d1941ce29836e79fae1938a738e6516965eeb771286aee4dd92bbc0e520618ab"),
            field("projection.identity.productNameEnglish", "Brie double crème", "d1941ce29836e79fae1938a738e6516965eeb771286aee4dd92bbc0e520618ab"),
            field("projection.identity.productType", "food", "c1f026582fe6e8cb620d0c85a72fe421ddded756662a8ec00ed4c297ad10676b"),
            field("projection.identity.servingSize", "30.0g", "dc1b9f7327bd5404f81d943516314777882441d2dad3849fabca394bdb394402"),
            field("projection.nutrition.declared.energy-kcal.per100g", "366.6666666666667", "2dcdc15758806bf044e0061ed39c357456ecc33b8723c816894351650bdd56e6"),
            field("projection.nutrition.declared.energy-kcal.perServing", "110.0", "7ae77b1b2f78eca1f49787b534ce526dc308c6bb02a522d6530664cfa34456df"),
            field("projection.nutrition.declared.energy-kcal.unit", "kcal", "adda1147b4a391605db368c3fa19ad5a463b36810a8d7a31f876bd18eecb8a27"),
            field("projection.nutrition.declared.energy-kcal.value", "366.6666666666667", "2dcdc15758806bf044e0061ed39c357456ecc33b8723c816894351650bdd56e6"),
            field("projection.nutrition.declared.energy.per100g", "1534.0", "4204f4f64f81fe816e4d5689b1e60e6d348dccaa6412b3ecfa14597ecb04ad72"),
            field("projection.nutrition.declared.energy.perServing", "460.0", "3c21c07f663ffa9a65afd34906b142563464e0e76e032f3fbcf54f75a8f6da08"),
            field("projection.nutrition.declared.energy.unit", "kcal", "adda1147b4a391605db368c3fa19ad5a463b36810a8d7a31f876bd18eecb8a27"),
            field("projection.nutrition.declared.energy.value", "1534.0", "4204f4f64f81fe816e4d5689b1e60e6d348dccaa6412b3ecfa14597ecb04ad72"),
            field("projection.nutrition.declared.fat.per100g", "33.333333333333336", "6a6edf9052437595256afc0dfb1e87239762eaa053a99588bfd100b3c8049c99"),
            field("projection.nutrition.declared.fat.perServing", "10.0", "f1e42019aecc858ffbcca7fddec511b761b474916fde37b1a6ff321a9b459330"),
            field("projection.nutrition.declared.fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.declared.fat.value", "33.333333333333336", "6a6edf9052437595256afc0dfb1e87239762eaa053a99588bfd100b3c8049c99"),
            field("projection.nutrition.declared.proteins.per100g", "20.0", "585348dbd28810f9a34f57be46f000c5f9effab894d074713d93fe7ccfeb3b76"),
            field("projection.nutrition.declared.proteins.perServing", "6.0", "8ab31b5afaea56114427e1f01b81d001b079a0f59539f6db3f099816ca794055"),
            field("projection.nutrition.declared.proteins.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.declared.proteins.value", "20.0", "585348dbd28810f9a34f57be46f000c5f9effab894d074713d93fe7ccfeb3b76"),
            field("projection.nutrition.declared.salt.per100g", "1.75", "f4881c772c8950930750e103abbe15b6720b84168921e66850d5800500ea0865"),
            field("projection.nutrition.declared.salt.perServing", "0.525", "dd879c59c94f64f6944ecd473c7347b0859efcf3e184a0e3da402650b0c027f4"),
            field("projection.nutrition.declared.salt.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.declared.salt.value", "1.75", "f4881c772c8950930750e103abbe15b6720b84168921e66850d5800500ea0865"),
            field("projection.nutrition.declared.saturated-fat.per100g", "20.0", "585348dbd28810f9a34f57be46f000c5f9effab894d074713d93fe7ccfeb3b76"),
            field("projection.nutrition.declared.saturated-fat.perServing", "6.0", "8ab31b5afaea56114427e1f01b81d001b079a0f59539f6db3f099816ca794055"),
            field("projection.nutrition.declared.saturated-fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.declared.saturated-fat.value", "20.0", "585348dbd28810f9a34f57be46f000c5f9effab894d074713d93fe7ccfeb3b76"),
            field("projection.nutrition.declared.sodium.per100g", "0.7", "973b372a514f91db8219fef585cdf81b09196afd7948f5fb1e29c2016dd995a0"),
            field("projection.nutrition.declared.sodium.perServing", "0.21", "207e96f0842d64f3794482df77b130336a46bc7d923fa84ce4236d4dec94ad5e"),
            field("projection.nutrition.declared.sodium.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.declared.sodium.value", "0.7", "973b372a514f91db8219fef585cdf81b09196afd7948f5fb1e29c2016dd995a0"),
            field("projection.quality.complete", "false", "fcbcf165908dd18a9e49f7ff27810176db8e9f63b4352213741664245224f8aa"),
            field("projection.quality.completeness", "0.2", "44896b09365746b5f7167ee4d64988a38f7f4628803cbf86224e74eeb7c69e9d"),
            field("projection.quality.info[0]", "en:no-packaging-data", "85ba292351f2263f671eca3eb2a5277efc488337da2a4f5458d3892871bfecec"),
            field("projection.quality.info[1]", "en:ecoscore-extended-data-not-computed", "50a7115a7027c26b897186a83e41c26f17fde0b30e1d9ae248e8d93e5a3874bb"),
            field("projection.quality.info[2]", "en:food-groups-1-unknown", "c34697bdf06cef0af785c432e0be71b69c287af9c79f0d6fc8284daac194e474"),
            field("projection.quality.info[3]", "en:food-groups-2-unknown", "064c29a3a716a6ef52a62a8c8ec2186b2bcf2c4b49b444d7d240414bd5474853"),
            field("projection.quality.info[4]", "en:food-groups-3-unknown", "634d6b1264a8ab32857b25fc3ec1426b92f017d7318761da224ac06d5540c319"),
            field("projection.quality.scans", "1", "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b"),
            field("projection.quality.uniqueScans", "1", "6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b"),
            field("projection.quality.warnings[0]", "en:serving-quantity-defined-but-quantity-undefined", "8e82ae631a4d0eab74ead166e2178f8b339a1b1b1c8cce413ed15e9f4f887c61"),
            field("projection.quality.warnings[1]", "en:ecoscore-origins-of-ingredients-origins-are-100-percent-unknown", "eb26787e756575225e082661a3b4dbc7ea5d75676ac1f4d5483593a1a31933f4"),
            field("projection.quality.warnings[2]", "en:ecoscore-packaging-packaging-data-missing", "ff06c7d99a497816e335436aef49fd479f2ad733d1d9a1bcdf33e1f073a3ea77"),
            field("projection.quality.warnings[3]", "en:ecoscore-production-system-no-label", "90a17b2269f43822e835e47ecd8f5d543d69d1dfcd7c15545a8d7336d203eb8d"),
            field("projection.quality.warnings[4]", "en:ecoscore-threatened-species-ingredients-missing", "085950937133b0c0890e490b3b4a8dd34e0879491cdd3f66e4023c7ff99079ce"),
            field("projection.rowOrdinal", "3272579", "a2009b9346f165287f82772ccdfc217e27b10d40509050142467f8bc024a97bc"),
            field("projection.source.code", "0061483010917", "2ec8cd318daf51b3976aea8312eee1451ffbdf7ddf6e2d0f5b93fbf473f5676e"),
            field("projection.taxonomy.pnnsGroups[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
        )
            val fields2 = listOf(
            field("canonical.aliases", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
            field("canonical.entityId", "rVnyq7", "f75957cd418023c9b49642b5390c4b46bdc244798eea0d6715c9148bf1e8ca42"),
            field("canonical.identities", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
            field("canonical.lifecycleStatus", "ACTIVE", "630c2f1c0ee1b8d7da57cf8936ae7e78274aba0bfd765fe10a20dfe580f9eecc"),
            field("canonical.name", "Crème double", "8e1ea30e61a9677227a32087a8feb540ad7cbd56c2cf06dcdedfeab13024ae88"),
            field("canonical.normalizedName", "creme-double", "efa6e8e8e4f3b55370f349fd5c44b9193defd93154d4ca005abad1f82f0b8c0c"),
            field("canonical.variants", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
        )
            listOf(
                card(
                    unitId = "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
                    evidenceReferenceId = "280b91e4c4e10f95288308d50a0df31e3be773385c11afc104e8525127cc5b1e",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
                    artifact = artifact("data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json", 266953, "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f", "6ffd1f6c08d3f2f9d02aba427540d99a94d8461517bf6ea24e08f342fff5406e"),
                    recordReference = "catalog:rVnyq7",
                    fields = fields0,
                    sourceProjection = null,
                ),
                card(
                    unitId = "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
                    evidenceReferenceId = "8a9e75f184ab7e1e4dd81c0b6d805ddf252dce54d8d39244b892e53ad59e7bcd",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION,
                    artifact = artifact("data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", 25551749120, "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df", "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"),
                    recordReference = "off:product:row:3272579:code:0061483010917",
                    fields = fields1,
                    sourceProjection = projection(
                        source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                        recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
                        artifact = artifact("data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", 25551749120, "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df", "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"),
                        recordReference = "off:product:row:3272579:code:0061483010917",
                        fields = fields1,
                        sourceOriginArtifact = artifact("data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", 0, "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236", null),
                    ),
                ),
                card(
                    unitId = "4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7",
                    evidenceReferenceId = "b4a88e7492228b36d6919cbc75959520b86d8505cf1f85b93764f21538cdfe09",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
                    artifact = artifact("data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json", 532398, "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184", "d4a26bf27de8768498bea8277cfb903f572900af00fe499bff9f63dcbc44ca7c"),
                    recordReference = "authority:rVnyq7",
                    fields = fields2,
                    sourceProjection = null,
                ),
            )
        }
        "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70" -> {
            val fields0 = listOf(
            field("projection.classification.nova.tags[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.classification.nutriScore.grade", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.classification.nutriScore.version", "2023", "d398b29d3dbbb9bf201d4c7e1c19ff9d43c15fd45a0cec46fbe9885ec3f6e97f"),
            field("projection.environmentalEvidence.diagnostics.missingAgribalyseMatch", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingCategories", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingIngredients", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingKeyData", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingLabels", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingOrigins", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingPackaging", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.status", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].origin", "en:unknown", "930e9cc4104236653b40c811ae2752246141fed58e65c4f00f2282c958dc769a"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].percent", "100.0", "43b87f618caab482ebe4976c92bcd6ad308b48055f1c27b4c574f3e31d7683e0"),
            field("projection.environmentalEvidence.origin.epiScore", "0.0", "8aed642bf5118b9d3c859bd4be35ecac75b6e873cce34e7b6f554b06f75550d7"),
            field("projection.environmentalEvidence.origin.epiValue", "-5.0", "d08da5e668569e4ba7d8e7f55d0dfb939bb31e645be387e05937306c5c39da0b"),
            field("projection.environmentalEvidence.origin.originsFromCategories[0]", "en:unknown", "930e9cc4104236653b40c811ae2752246141fed58e65c4f00f2282c958dc769a"),
            field("projection.environmentalEvidence.origin.originsFromSourceField[0]", "en:unknown", "930e9cc4104236653b40c811ae2752246141fed58e65c4f00f2282c958dc769a"),
            field("projection.environmentalEvidence.origin.warning", "origins_are_100_percent_unknown", "b02bd10bbb99ccad120f96e6c821d47744e2a12eb95134f3be037bbfa36d3037"),
            field("projection.environmentalEvidence.packaging.value", "-15.0", "b63d2b235e273730eda06df31b5d8c0f4c73eec62deb0a3937bc3540384d6a26"),
            field("projection.environmentalEvidence.packaging.warning", "packaging_data_missing", "e032dba53bec00c1824cd8143247b0262cef402ec74052fd4340f84917b35233"),
            field("projection.environmentalEvidence.productionSystem.value", "0.0", "8aed642bf5118b9d3c859bd4be35ecac75b6e873cce34e7b6f554b06f75550d7"),
            field("projection.environmentalEvidence.productionSystem.warning", "no_label", "2b069f022c225b12b341469c8dc78636faa659da27370256b7cc233338fc5daa"),
            field("projection.environmentalEvidence.score.grade", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.environmentalEvidence.score.tags[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.environmentalEvidence.threatenedSpecies.warning", "ingredients_missing", "0df20dd2821117dfa92a368f8f1a34401eff227c5b6a4a6b5957e3dd0bda8e12"),
            field("projection.geography.countries[0]", "en:germany", "67bddff782279ac98c3ba69d1d4c26feae3337a2307301d6dcd1013b2d6c5fa1"),
            field("projection.identity.productName", "Artischocken Herzen", "116e55725814811c8cb79c29e853ebba8a1e4f281a738e6d2d9c15b0a91b7a82"),
            field("projection.identity.productNameGerman", "Artischocken Herzen", "116e55725814811c8cb79c29e853ebba8a1e4f281a738e6d2d9c15b0a91b7a82"),
            field("projection.identity.productType", "food", "c1f026582fe6e8cb620d0c85a72fe421ddded756662a8ec00ed4c297ad10676b"),
            field("projection.nutrition.structured.aggregated.carbohydrates.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.carbohydrates.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.carbohydrates.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.carbohydrates.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.carbohydrates.value", "6.5", "a22eea496bce0154f2e2dc5e652100cd8a0f9571b95ed89dd48599e76d339657"),
            field("projection.nutrition.structured.aggregated.energy-kcal.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.energy-kcal.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.energy-kcal.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.energy-kcal.unit", "kcal", "adda1147b4a391605db368c3fa19ad5a463b36810a8d7a31f876bd18eecb8a27"),
            field("projection.nutrition.structured.aggregated.energy-kcal.value", "38.0", "cc179cc6804e03b317abe487bbfd0651951ccd6f562d6db1fa9296da87076e7e"),
            field("projection.nutrition.structured.aggregated.energy-kj.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.energy-kj.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.energy-kj.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.energy-kj.unit", "kj", "46874106f9ac7f3b68a422895aa223e1ea5ed0db860f179f6362cb166c65f8ca"),
            field("projection.nutrition.structured.aggregated.energy-kj.value", "134.9", "41c1ded9cbdcf693a0e5b557d8c725988c54d27e8baf21f49a66f8d7f787593e"),
            field("projection.nutrition.structured.aggregated.energy.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.energy.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.energy.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.energy.unit", "kj", "46874106f9ac7f3b68a422895aa223e1ea5ed0db860f179f6362cb166c65f8ca"),
            field("projection.nutrition.structured.aggregated.energy.value", "134.9", "41c1ded9cbdcf693a0e5b557d8c725988c54d27e8baf21f49a66f8d7f787593e"),
            field("projection.nutrition.structured.aggregated.fat.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.fat.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.fat.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.fat.value", "0.2", "44896b09365746b5f7167ee4d64988a38f7f4628803cbf86224e74eeb7c69e9d"),
            field("projection.nutrition.structured.aggregated.proteins.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.proteins.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.proteins.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.proteins.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.proteins.value", "1.0", "d0ff5974b6aa52cf562bea5921840c032a860a91a3512f7fe8f768f6bbe005f6"),
            field("projection.nutrition.structured.aggregated.salt.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.salt.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.salt.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.salt.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.salt.value", "0.8", "1e9d7c27c8bbc8ddf0055c93e064a62fa995d177fee28cc8fa949bc8a4db06f4"),
            field("projection.nutrition.structured.aggregated.saturated-fat.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.saturated-fat.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.saturated-fat.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.saturated-fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.saturated-fat.value", "0.1", "14be4b45f18e0d8c67b4f719b5144eee88497e413709d11d85b096d8e2346310"),
            field("projection.nutrition.structured.aggregated.sodium.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.sodium.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.sodium.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.sodium.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.sodium.value", "0.32", "60bb8f6c52a2aca85b1a3ae08e71f97dd3fa8603b55abefb09ebb0edfe5294d1"),
            field("projection.nutrition.structured.aggregated.sugars.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.sugars.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.sugars.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.sugars.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.sugars.value", "1.2", "77ac319bfe1979e2d799d9e6987e65feb54f61511c03552ebae990826c208590"),
            field("projection.nutrition.structured.inputSets[0].nutrients.carbohydrates.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.carbohydrates.value", "6.5", "a22eea496bce0154f2e2dc5e652100cd8a0f9571b95ed89dd48599e76d339657"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kcal.unit", "kcal", "adda1147b4a391605db368c3fa19ad5a463b36810a8d7a31f876bd18eecb8a27"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kcal.value", "38.0", "cc179cc6804e03b317abe487bbfd0651951ccd6f562d6db1fa9296da87076e7e"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kj.unit", "kj", "46874106f9ac7f3b68a422895aa223e1ea5ed0db860f179f6362cb166c65f8ca"),
            field("projection.nutrition.structured.inputSets[0].nutrients.fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.fat.value", "0.2", "44896b09365746b5f7167ee4d64988a38f7f4628803cbf86224e74eeb7c69e9d"),
            field("projection.nutrition.structured.inputSets[0].nutrients.proteins.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.proteins.value", "1.0", "d0ff5974b6aa52cf562bea5921840c032a860a91a3512f7fe8f768f6bbe005f6"),
            field("projection.nutrition.structured.inputSets[0].nutrients.salt.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.salt.value", "0.8", "1e9d7c27c8bbc8ddf0055c93e064a62fa995d177fee28cc8fa949bc8a4db06f4"),
            field("projection.nutrition.structured.inputSets[0].nutrients.saturated-fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.saturated-fat.value", "0.1", "14be4b45f18e0d8c67b4f719b5144eee88497e413709d11d85b096d8e2346310"),
            field("projection.nutrition.structured.inputSets[0].nutrients.sodium.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.sodium.value", "0.32", "60bb8f6c52a2aca85b1a3ae08e71f97dd3fa8603b55abefb09ebb0edfe5294d1"),
            field("projection.nutrition.structured.inputSets[0].nutrients.sugars.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.sugars.value", "1.2", "77ac319bfe1979e2d799d9e6987e65feb54f61511c03552ebae990826c208590"),
            field("projection.nutrition.structured.inputSets[0].per", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.inputSets[0].preparation", "as_sold", "f7f6686107b4b1862a19030a9a255ca11db0b7178b1a162713e993936f43000f"),
            field("projection.nutrition.structured.inputSets[0].source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.quality.complete", "false", "fcbcf165908dd18a9e49f7ff27810176db8e9f63b4352213741664245224f8aa"),
            field("projection.quality.completeness", "0.275", "1bd77e962f3a0649428e75dc1cce6cff83d90cc553ea4106e98416a3b7e58a99"),
            field("projection.quality.info[0]", "en:no-packaging-data", "85ba292351f2263f671eca3eb2a5277efc488337da2a4f5458d3892871bfecec"),
            field("projection.quality.info[1]", "en:food-groups-1-unknown", "c34697bdf06cef0af785c432e0be71b69c287af9c79f0d6fc8284daac194e474"),
            field("projection.quality.info[2]", "en:food-groups-2-unknown", "064c29a3a716a6ef52a62a8c8ec2186b2bcf2c4b49b444d7d240414bd5474853"),
            field("projection.quality.info[3]", "en:food-groups-3-unknown", "634d6b1264a8ab32857b25fc3ec1426b92f017d7318761da224ac06d5540c319"),
            field("projection.quality.warnings[0]", "en:nutrition-energy-value-in-kcal-does-not-match-value-in-kj", "99e3a65e42780dccafd2e48459b1d3cf3ddb7ad0fe0e418151fd5dd806cd0a28"),
            field("projection.quality.warnings[1]", "en:environmental-score-origins-of-ingredients-origins-are-100-percent-unknown", "1b32ba6e1e7a2e7caf34c5139cc0358e1836e24ac0cfb6d11b5bc17585def684"),
            field("projection.quality.warnings[2]", "en:environmental-score-packaging-packaging-data-missing", "24067ac9c307269d001ee2b016fcfa23b1f33fc2e7f9ff9696dfe343deb956e8"),
            field("projection.quality.warnings[3]", "en:environmental-score-production-system-no-label", "5636a82167d716d3740742ab6562de4a85d3d271132e6ee0cdb5f644428b3615"),
            field("projection.quality.warnings[4]", "en:environmental-score-threatened-species-ingredients-missing", "b9b4cea74abda7ed0c89c895f30c35ec9927c9fec51063166674e3e20af0c92a"),
            field("projection.rowOrdinal", "1551407", "f3c36b561625a8e4a7b543cfed2bcd1d08763163ff32e1169c12e1d74681988e"),
            field("projection.source.code", "4013200552046", "4507adbe9bb735fa7d19002b2b6175808af795022437b5492166930921333959"),
            field("projection.taxonomy.pnnsGroups[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
        )
            val fields1 = listOf(
            field("canonical.entityId", "ZuhV5V", "45405269451baec97edcc9b6d58c120a8a8e677bcc2717a4e056bd7a6fa9bf72"),
            field("canonical.name", "Artischocken", "d5bf10bffd48d29ed551d21155f939acffedb4025e9c5e93249fd2f4306d741b"),
            field("canonical.normalizedName", "artischocken", "66d6ef4b2c02afb5e6f50465cb2f089caf6d88ac60dcc56fe399c082c35089a7"),
            field("canonical.taxonomyPaths", "[[vegetables, mediterranean-vegetables, artichokes]]", "ae256fb3f0ae54511b4919a69dba9e1aff77acca5fa1152f97a59d5ec6be14f3"),
        )
            val fields2 = listOf(
            field("canonical.aliases", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
            field("canonical.entityId", "ZuhV5V", "45405269451baec97edcc9b6d58c120a8a8e677bcc2717a4e056bd7a6fa9bf72"),
            field("canonical.identities", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
            field("canonical.lifecycleStatus", "ACTIVE", "630c2f1c0ee1b8d7da57cf8936ae7e78274aba0bfd765fe10a20dfe580f9eecc"),
            field("canonical.name", "Artischocken", "d5bf10bffd48d29ed551d21155f939acffedb4025e9c5e93249fd2f4306d741b"),
            field("canonical.normalizedName", "artischocken", "66d6ef4b2c02afb5e6f50465cb2f089caf6d88ac60dcc56fe399c082c35089a7"),
            field("canonical.variants", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
        )
            listOf(
                card(
                    unitId = "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
                    evidenceReferenceId = "4712bc9b78ecfc168ea1902518ff625f92eff4ee14a9d1938a5c66fc4dd8b39e",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
                    artifact = artifact("data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", 25551749120, "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df", "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"),
                    recordReference = "off:product:row:1551407:code:4013200552046",
                    fields = fields0,
                    sourceProjection = projection(
                        source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                        recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
                        artifact = artifact("data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", 25551749120, "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df", "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"),
                        recordReference = "off:product:row:1551407:code:4013200552046",
                        fields = fields0,
                        sourceOriginArtifact = artifact("data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", 0, "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236", null),
                    ),
                ),
                card(
                    unitId = "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
                    evidenceReferenceId = "896f30747f7fcfcb29f64e9170e9ca6798ad796a996e79d7b5432b146fde2020",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION,
                    artifact = artifact("data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json", 266953, "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f", "6ffd1f6c08d3f2f9d02aba427540d99a94d8461517bf6ea24e08f342fff5406e"),
                    recordReference = "catalog:ZuhV5V",
                    fields = fields1,
                    sourceProjection = null,
                ),
                card(
                    unitId = "9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70",
                    evidenceReferenceId = "d6f7e596663b3547c30bb4afcf386cdc97bc32d170860ed39486f23ec719dabf",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
                    artifact = artifact("data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json", 532398, "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184", "d4a26bf27de8768498bea8277cfb903f572900af00fe499bff9f63dcbc44ca7c"),
                    recordReference = "authority:ZuhV5V",
                    fields = fields2,
                    sourceProjection = null,
                ),
            )
        }
        "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50" -> {
            val fields0 = listOf(
            field("canonical.aliases", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
            field("canonical.entityId", "rVnyq7", "f75957cd418023c9b49642b5390c4b46bdc244798eea0d6715c9148bf1e8ca42"),
            field("canonical.identities", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
            field("canonical.lifecycleStatus", "ACTIVE", "630c2f1c0ee1b8d7da57cf8936ae7e78274aba0bfd765fe10a20dfe580f9eecc"),
            field("canonical.name", "Crème double", "8e1ea30e61a9677227a32087a8feb540ad7cbd56c2cf06dcdedfeab13024ae88"),
            field("canonical.normalizedName", "creme-double", "efa6e8e8e4f3b55370f349fd5c44b9193defd93154d4ca005abad1f82f0b8c0c"),
            field("canonical.variants", "[]", "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"),
        )
            val fields1 = listOf(
            field("projection.classification.nova.tags[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.classification.nutriScore.grade", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.classification.nutriScore.version", "2023", "d398b29d3dbbb9bf201d4c7e1c19ff9d43c15fd45a0cec46fbe9885ec3f6e97f"),
            field("projection.environmentalEvidence.diagnostics.missingAgribalyseMatch", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingCategories", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingIngredients", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingKeyData", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingLabels", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingOrigins", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.missingPackaging", "true", "b5bea41b6c623f7c09f1bf24dcae58ebab3c0cdd90ad966bc43a45b44867e12b"),
            field("projection.environmentalEvidence.diagnostics.status", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].origin", "en:unknown", "930e9cc4104236653b40c811ae2752246141fed58e65c4f00f2282c958dc769a"),
            field("projection.environmentalEvidence.origin.aggregatedOrigins[0].percent", "100.0", "43b87f618caab482ebe4976c92bcd6ad308b48055f1c27b4c574f3e31d7683e0"),
            field("projection.environmentalEvidence.origin.epiScore", "0.0", "8aed642bf5118b9d3c859bd4be35ecac75b6e873cce34e7b6f554b06f75550d7"),
            field("projection.environmentalEvidence.origin.epiValue", "-5.0", "d08da5e668569e4ba7d8e7f55d0dfb939bb31e645be387e05937306c5c39da0b"),
            field("projection.environmentalEvidence.origin.originsFromCategories[0]", "en:unknown", "930e9cc4104236653b40c811ae2752246141fed58e65c4f00f2282c958dc769a"),
            field("projection.environmentalEvidence.origin.originsFromSourceField[0]", "en:unknown", "930e9cc4104236653b40c811ae2752246141fed58e65c4f00f2282c958dc769a"),
            field("projection.environmentalEvidence.origin.warning", "origins_are_100_percent_unknown", "b02bd10bbb99ccad120f96e6c821d47744e2a12eb95134f3be037bbfa36d3037"),
            field("projection.environmentalEvidence.packaging.value", "-15.0", "b63d2b235e273730eda06df31b5d8c0f4c73eec62deb0a3937bc3540384d6a26"),
            field("projection.environmentalEvidence.packaging.warning", "packaging_data_missing", "e032dba53bec00c1824cd8143247b0262cef402ec74052fd4340f84917b35233"),
            field("projection.environmentalEvidence.productionSystem.value", "0.0", "8aed642bf5118b9d3c859bd4be35ecac75b6e873cce34e7b6f554b06f75550d7"),
            field("projection.environmentalEvidence.productionSystem.warning", "no_label", "2b069f022c225b12b341469c8dc78636faa659da27370256b7cc233338fc5daa"),
            field("projection.environmentalEvidence.score.grade", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.environmentalEvidence.score.tags[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
            field("projection.environmentalEvidence.threatenedSpecies.warning", "ingredients_missing", "0df20dd2821117dfa92a368f8f1a34401eff227c5b6a4a6b5957e3dd0bda8e12"),
            field("projection.geography.countries[0]", "en:ireland", "3aeb208667a2bc7a621b07bd0d1b087fa64a090d7e7105dc052a6ce97696d19e"),
            field("projection.identity.productName", "Brie double crème", "d1941ce29836e79fae1938a738e6516965eeb771286aee4dd92bbc0e520618ab"),
            field("projection.identity.productNameEnglish", "Brie double crème", "d1941ce29836e79fae1938a738e6516965eeb771286aee4dd92bbc0e520618ab"),
            field("projection.identity.productType", "food", "c1f026582fe6e8cb620d0c85a72fe421ddded756662a8ec00ed4c297ad10676b"),
            field("projection.identity.servingSize", "30.0g", "dc1b9f7327bd5404f81d943516314777882441d2dad3849fabca394bdb394402"),
            field("projection.nutrition.structured.aggregated.carbohydrates.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.carbohydrates.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.carbohydrates.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.carbohydrates.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.carbohydrates.value", "3.3333333333333335", "a4c4e89585d676e7ae3e6bd5554ae834803f6515d9abbc0b58d07c8f10f76e11"),
            field("projection.nutrition.structured.aggregated.energy-kcal.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.energy-kcal.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.energy-kcal.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.energy-kcal.unit", "kcal", "adda1147b4a391605db368c3fa19ad5a463b36810a8d7a31f876bd18eecb8a27"),
            field("projection.nutrition.structured.aggregated.energy-kcal.value", "333.33333333333337", "5bb4c24002c3d3771ae83023ffb07fddd0891ff1ff660272df5e78bc796df51c"),
            field("projection.nutrition.structured.aggregated.energy-kj.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.energy-kj.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.energy-kj.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.energy-kj.unit", "kj", "46874106f9ac7f3b68a422895aa223e1ea5ed0db860f179f6362cb166c65f8ca"),
            field("projection.nutrition.structured.aggregated.energy-kj.value", "1506.6666666666667", "8384bd5715492cb731fbc20b2a4589895a917ea27053f8fe195edd1935784caa"),
            field("projection.nutrition.structured.aggregated.energy.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.energy.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.energy.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.energy.unit", "kj", "46874106f9ac7f3b68a422895aa223e1ea5ed0db860f179f6362cb166c65f8ca"),
            field("projection.nutrition.structured.aggregated.energy.value", "1506.6666666666667", "8384bd5715492cb731fbc20b2a4589895a917ea27053f8fe195edd1935784caa"),
            field("projection.nutrition.structured.aggregated.fat.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.fat.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.fat.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.fat.value", "30.0", "26c9a96ce053a14dd88a71a4830c9cbed7e1fed7e3f3f8a0b0b6a58f3f0f02e6"),
            field("projection.nutrition.structured.aggregated.proteins.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.proteins.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.proteins.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.proteins.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.proteins.value", "20.0", "585348dbd28810f9a34f57be46f000c5f9effab894d074713d93fe7ccfeb3b76"),
            field("projection.nutrition.structured.aggregated.saturated-fat.source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.nutrition.structured.aggregated.saturated-fat.sourceIndex", "0", "5feceb66ffc86f38d952786c6d696c79c2dbc239dd4e91b46729d73a27fb57e9"),
            field("projection.nutrition.structured.aggregated.saturated-fat.sourcePer", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.aggregated.saturated-fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.aggregated.saturated-fat.value", "20.0", "585348dbd28810f9a34f57be46f000c5f9effab894d074713d93fe7ccfeb3b76"),
            field("projection.nutrition.structured.inputSets[0].nutrients.carbohydrates.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.carbohydrates.value", "3.3333333333333335", "a4c4e89585d676e7ae3e6bd5554ae834803f6515d9abbc0b58d07c8f10f76e11"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kcal.unit", "kcal", "adda1147b4a391605db368c3fa19ad5a463b36810a8d7a31f876bd18eecb8a27"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kcal.value", "333.33333333333337", "5bb4c24002c3d3771ae83023ffb07fddd0891ff1ff660272df5e78bc796df51c"),
            field("projection.nutrition.structured.inputSets[0].nutrients.energy-kj.unit", "kj", "46874106f9ac7f3b68a422895aa223e1ea5ed0db860f179f6362cb166c65f8ca"),
            field("projection.nutrition.structured.inputSets[0].nutrients.fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.fat.value", "30.0", "26c9a96ce053a14dd88a71a4830c9cbed7e1fed7e3f3f8a0b0b6a58f3f0f02e6"),
            field("projection.nutrition.structured.inputSets[0].nutrients.proteins.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.proteins.value", "20.0", "585348dbd28810f9a34f57be46f000c5f9effab894d074713d93fe7ccfeb3b76"),
            field("projection.nutrition.structured.inputSets[0].nutrients.saturated-fat.unit", "g", "cd0aa9856147b6c5b4ff2b7dfee5da20aa38253099ef1b4a64aced233c9afe29"),
            field("projection.nutrition.structured.inputSets[0].nutrients.saturated-fat.value", "20.0", "585348dbd28810f9a34f57be46f000c5f9effab894d074713d93fe7ccfeb3b76"),
            field("projection.nutrition.structured.inputSets[0].per", "100g", "7a038d35f154760ecdc199a14147831a2a52324c9cee3894b25c71010ae1a994"),
            field("projection.nutrition.structured.inputSets[0].preparation", "as_sold", "f7f6686107b4b1862a19030a9a255ca11db0b7178b1a162713e993936f43000f"),
            field("projection.nutrition.structured.inputSets[0].source", "packaging", "71669691cb0b0f1bf662939c0db0782fb1a71b103bba2564051bb58bf0e303e6"),
            field("projection.quality.complete", "false", "fcbcf165908dd18a9e49f7ff27810176db8e9f63b4352213741664245224f8aa"),
            field("projection.quality.completeness", "0.2", "44896b09365746b5f7167ee4d64988a38f7f4628803cbf86224e74eeb7c69e9d"),
            field("projection.quality.info[0]", "en:no-packaging-data", "85ba292351f2263f671eca3eb2a5277efc488337da2a4f5458d3892871bfecec"),
            field("projection.quality.info[1]", "en:food-groups-1-unknown", "c34697bdf06cef0af785c432e0be71b69c287af9c79f0d6fc8284daac194e474"),
            field("projection.quality.info[2]", "en:food-groups-2-unknown", "064c29a3a716a6ef52a62a8c8ec2186b2bcf2c4b49b444d7d240414bd5474853"),
            field("projection.quality.info[3]", "en:food-groups-3-unknown", "634d6b1264a8ab32857b25fc3ec1426b92f017d7318761da224ac06d5540c319"),
            field("projection.quality.warnings[0]", "en:serving-quantity-defined-but-quantity-undefined", "8e82ae631a4d0eab74ead166e2178f8b339a1b1b1c8cce413ed15e9f4f887c61"),
            field("projection.quality.warnings[1]", "en:environmental-score-origins-of-ingredients-origins-are-100-percent-unknown", "1b32ba6e1e7a2e7caf34c5139cc0358e1836e24ac0cfb6d11b5bc17585def684"),
            field("projection.quality.warnings[2]", "en:environmental-score-packaging-packaging-data-missing", "24067ac9c307269d001ee2b016fcfa23b1f33fc2e7f9ff9696dfe343deb956e8"),
            field("projection.quality.warnings[3]", "en:environmental-score-production-system-no-label", "5636a82167d716d3740742ab6562de4a85d3d271132e6ee0cdb5f644428b3615"),
            field("projection.quality.warnings[4]", "en:environmental-score-threatened-species-ingredients-missing", "b9b4cea74abda7ed0c89c895f30c35ec9927c9fec51063166674e3e20af0c92a"),
            field("projection.rowOrdinal", "3322623", "aa2a4fa9e1fcf18b36b05b8787e6eb282cfb9061aefd9d0a324be183bfccb6a0"),
            field("projection.source.code", "2026088009283", "085e2134364621dfff59e3e86510164c717e660f0de55935a1154e810b0f3783"),
            field("projection.taxonomy.pnnsGroups[0]", "unknown", "b23a6a8439c0dde5515893e7c90c1e3233b8616e634470f20dc4928bcf3609bc"),
        )
            val fields2 = listOf(
            field("canonical.entityId", "rVnyq7", "f75957cd418023c9b49642b5390c4b46bdc244798eea0d6715c9148bf1e8ca42"),
            field("canonical.name", "Crème double", "8e1ea30e61a9677227a32087a8feb540ad7cbd56c2cf06dcdedfeab13024ae88"),
            field("canonical.normalizedName", "creme-double", "efa6e8e8e4f3b55370f349fd5c44b9193defd93154d4ca005abad1f82f0b8c0c"),
            field("canonical.taxonomyPaths", "[[dairy-and-eggs, dairy-products, cream, double-cream]]", "3eb00e2dfd39b26075033e8994b1221ee3b24402d5ce44347bc03a5c11c90b22"),
        )
            listOf(
                card(
                    unitId = "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
                    evidenceReferenceId = "045ee67398a6f7355b0faef3760ceb0d24bf22515b9e67d84c7931e96d2c9fba",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
                    artifact = artifact("data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json", 532398, "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184", "d4a26bf27de8768498bea8277cfb903f572900af00fe499bff9f63dcbc44ca7c"),
                    recordReference = "authority:rVnyq7",
                    fields = fields0,
                    sourceProjection = null,
                ),
                card(
                    unitId = "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
                    evidenceReferenceId = "2b225f611ce2dc5eed79f4238b0a24d328318638549fb63f3a9611f1fc29e027",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION,
                    artifact = artifact("data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", 25551749120, "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df", "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"),
                    recordReference = "off:product:row:3322623:code:2026088009283",
                    fields = fields1,
                    sourceProjection = projection(
                        source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                        recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
                        artifact = artifact("data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite", 25551749120, "80c7da8c2b0a94ee0b12fb03e095f50429d2c0b300ce70dc22d0b5095aabf5df", "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"),
                        recordReference = "off:product:row:3322623:code:2026088009283",
                        fields = fields1,
                        sourceOriginArtifact = artifact("data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz", 0, "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236", null),
                    ),
                ),
                card(
                    unitId = "d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50",
                    evidenceReferenceId = "dbcacd77e3944b77661262caa8845e24b1a1cb2eb6b78726cb7595dc7ecf5d93",
                    kind = HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD,
                    directness = HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
                    position = HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY,
                    artifact = artifact("data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json", 266953, "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f", "6ffd1f6c08d3f2f9d02aba427540d99a94d8461517bf6ea24e08f342fff5406e"),
                    recordReference = "catalog:rVnyq7",
                    fields = fields2,
                    sourceProjection = null,
                ),
            )
        }
        else -> error("unknown review unit")
    }

    private fun projection(
        source: HimGroundTruthSource,
        recordKind: HimEvidenceRecordKind,
        artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
        recordReference: String,
        fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>,
        sourceOriginArtifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1(
        source,
        recordKind,
        artifact,
        recordReference,
        fields,
        sourceOriginArtifact,
    )

    private fun card(
        unitId: String,
        evidenceReferenceId: String,
        kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
        directness: HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1,
        position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
        artifact: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1,
        recordReference: String,
        fields: List<HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1>,
        sourceProjection: HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceProjectionV1?,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceCardV1(
        unitId,
        evidenceReferenceId,
        kind,
        directness,
        position,
        artifact,
        recordReference,
        fields,
        sourceProjection,
    )

    private fun binding() = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementBindingV1(
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.CONTRACT_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.VERSION,
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.MISSION_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.SCOPE_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotMissionContractV1.FROZEN_SELECTION_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.CONTRACT_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotReviewPacketContractV1.VERSION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_INPUT_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_LOGICAL_DIGEST,
        artifact(
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-review-corpus/v1/review-corpus.v1.json",
            2359985,
            "4c2dee0e378c62139dcc349c4f0992b49fb7d3fbf2afa408faec3a4b56fc8016",
            "3f1de89b5ea97bfa340ae3c61baa3e7f2a2e4ed0a7dad9bc7867118614d03d02",
        ),
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.CORPUS_BINDING_DIGEST,
        HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementContractV1.PACKET_IMPLEMENTATION_HEAD,
    )

    private fun artifact(
        path: String,
        size: Long,
        sha256: String,
        logicalDigest: String?,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceSupplementArtifactBindingV1(
        path,
        size,
        sha256,
        logicalDigest,
    )

    private fun field(
        reference: String,
        value: String,
        digest: String,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDirectEvidenceFieldV1(reference, value, digest)

    private fun packetBinding(index: Int) = listOf(
        "a61bd239809b266d8a8ca36ef98b8b635966b4d0e6f6716468b77ef284829f29",
        "0fb192a227076e28204d841ad8573cc33e486fcfd8991678fa24f7727937fb35",
        "2fef9dcff87ceeebfe0ae5ca2f9996ab5e8115c13c9949bb6bb0a1b1ed3e5287",
        "1528b8f9dfd01272762f10e1f22a7f7f4ecad05cb1f3ee59493b6c2eb6d91722",
    )[index]

    private fun packetLogical(index: Int) = listOf(
        "50af5a0589bc19f9154d840091cb5ccbc7d7250b2fca5a3a9a1d05967905c418",
        "7b26d34c7335cd0f03ffaf49d72bd5edff36a2e9df465ce3251acb9e8839a57c",
        "ef5008317a70136fa95b4f025cd060544af249db579551cc996694ec9fa35ab0",
        "dd78dbf8d17943f1d6f7645903f305e33f571f3efb70d4dff8a46e77dfb92d89",
    )[index]
}
