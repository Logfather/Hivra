package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1Test {
    @Test
    fun validMaterializedValueIsPersistedAndReloaded() {
        val root = tempRoot()
        try {
            val input = Fixture.materialized()
            val result = completed(persist(input, root))
            assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.PersistenceStatus.CREATED, result.status)
            assertEquals(input.materializationId, result.record.p1MaterializationId)
            assertEquals(input.negativeTrainingExample, result.record.genericNegativeExample)
            assertEquals(input.p1Lineage, result.record.p1Lineage)
            assertEquals(input.evidenceBindings, result.record.evidenceBindings)
            assertEquals(result.record, HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.readByMaterializationId(input.materializationId.value, root))
        } finally { root.deleteRecursively() }
    }

    @Test
    fun canonicalBytesHaveOneTrailingLfAndStableFieldOrder() {
        val root = tempRoot()
        try {
            val result = completed(persist(Fixture.materialized(), root))
            val bytes = Files.readAllBytes(result.path(root).toPath())
            assertEquals(1, bytes.count { it == '\n'.code.toByte() })
            assertTrue(bytes.last() == '\n'.code.toByte())
            assertTrue(!bytes.contains('\r'.code.toByte()))
            assertEquals(bytes.toList(), HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.serializeRecord(result.record).toList())
            val text = bytes.toString(StandardCharsets.UTF_8)
            assertTrue(text.indexOf("\"contractId\"") < text.indexOf("\"p1MaterializationId\""))
            assertTrue(text.indexOf("\"p1MaterializationId\"") < text.indexOf("\"genericNegativeExample\""))
        } finally { root.deleteRecursively() }
    }

    @Test
    fun secondWriteIsAlreadyPresentIdenticalAndByteIdentical() {
        val root = tempRoot()
        try {
            val input = Fixture.materialized()
            val first = completed(persist(input, root))
            val firstBytes = Files.readAllBytes(first.path(root).toPath())
            val second = completed(persist(input, root))
            assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.PersistenceStatus.ALREADY_PRESENT_IDENTICAL, second.status)
            assertEquals(firstBytes.toList(), Files.readAllBytes(second.path(root).toPath()).toList())
            assertEquals(first.sha256, second.sha256)
            assertEquals(first.byteSize, second.byteSize)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun genericNegativeExampleAndLineageAreBothBoundToTheMaterialization() {
        val materialized = Fixture.materialized()
        val record = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.recordFrom(materialized)
        assertEquals(materialized.negativeTrainingExample.reference, record.genericNegativeExample.reference)
        assertEquals(record.genericNegativeExample.positiveExample.exampleReference, record.p1Lineage.positiveTrainingExampleReference)
        assertEquals(record.genericNegativeExample.rejectedTarget, record.p1Lineage.rejectedTarget)
        assertEquals(record.genericNegativeExample.boundaryType, record.p1Lineage.boundaryType)
        assertEquals(materialized.evidenceBindings.map { it.persistedEvidenceReferenceId }.toSet(), record.p1Lineage.evidenceReferenceIds.toSet())
    }

    @Test
    fun sourceEvidenceIsPersistedInBothTrainingInputAndP1Bindings() {
        val record = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.recordFrom(Fixture.materialized())
        val input = record.genericNegativeExample.positiveExample.input.evidence.map { it.reference }.toSet()
        val provenance = record.genericNegativeExample.positiveExample.provenance.sourceEvidenceReferences.toSet()
        assertEquals(input, record.evidenceBindings.map { it.trainingEvidence.reference }.toSet())
        assertEquals(provenance, record.evidenceBindings.map { it.trainingEvidence.reference }.toSet())
    }

    @Test
    fun materializationIdAndNegativeReferenceAreDeterministic() {
        val first = Fixture.materialized()
        val second = Fixture.materialized()
        assertEquals(first.materializationId, second.materializationId)
        assertEquals(first.negativeTrainingExample.reference, second.negativeTrainingExample.reference)
        assertEquals(first.p1Lineage, second.p1Lineage)
    }

    @Test
    fun differentMaterializationIdsUseDifferentDirectPaths() {
        val first = Fixture.materialized("f")
        val second = Fixture.materialized("e")
        val root = tempRoot()
        try {
            assertNotEquals(
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.pathFor(first.materializationId, root),
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.pathFor(second.materializationId, root),
            )
        } finally { root.deleteRecursively() }
    }

    @Test
    fun disabledRequestDoesNotCreateAnArtifact() {
        val root = tempRoot()
        try {
            val result = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.execute(
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Request(Fixture.materialized(), root, enabled = false),
            )
            assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Result.Disabled>(result)
            assertEquals(emptyList<File>(), root.listFiles()?.toList().orEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun malformedExistingArtifactFailsClosedWithoutReplacement() {
        val root = tempRoot()
        try {
            val materialized = Fixture.materialized()
            val path = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.pathFor(materialized.materializationId, root)
            Files.write(path.toPath(), "not-json\n".toByteArray(StandardCharsets.UTF_8))
            val failure = failed(persist(materialized, root))
            assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.MALFORMED_EXISTING_ARTIFACT, failure.reason)
            assertEquals("not-json\n", Files.readAllBytes(path.toPath()).toString(StandardCharsets.UTF_8))
        } finally { root.deleteRecursively() }
    }

    @Test
    fun existingDifferentSemanticsFailClosedWithoutOverwrite() {
        val root = tempRoot()
        try {
            val input = Fixture.materialized()
            val baseline = completed(persist(input, root))
            val altered = baseline.record.copy(
                p1Lineage = baseline.record.p1Lineage.copy(originalReviewerRef = "reviewer:other"),
                recordLogicalDigest = "",
            ).let { it.copy(recordLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.recordLogicalDigest(it)) }
            val originalBytes = Files.readAllBytes(baseline.path(root).toPath())
            Files.write(baseline.path(root).toPath(), HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.serializeRecord(altered))
            val failure = failed(persist(input, root))
            assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.EXISTING_ARTIFACT_CONFLICT, failure.reason)
            assertNotEquals(originalBytes.toList(), Files.readAllBytes(baseline.path(root).toPath()).toList())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun temporaryCollisionFailsClosed() {
        val root = tempRoot()
        try {
            val materialized = Fixture.materialized()
            val path = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.pathFor(materialized.materializationId, root)
            Files.write(File(path.parentFile, ".${path.name}.tmp").toPath(), byteArrayOf(1))
            assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.TEMPORARY_FILE_CONFLICT, failed(persist(materialized, root)).reason)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun readerRejectsMissingRecordAndInvalidMaterializationId() {
        val root = tempRoot()
        try {
            val missing = assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.PersistenceFailure> {
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.readByMaterializationId(Fixture.materialized().materializationId.value, root)
            }
            assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.ARTIFACT_MISSING, missing.reason)
            val invalid = assertFailsWith<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.PersistenceFailure> {
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.readByMaterializationId("../escape", root)
            }
            assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.INVALID_MATERIALIZATION_ID, invalid.reason)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun malformedTrailingBytesUnknownAndDuplicateFieldsFailClosed() {
        val root = tempRoot()
        try {
            val materialized = Fixture.materialized()
            val result = completed(persist(materialized, root))
            val path = result.path(root)
            val original = Files.readAllBytes(path.toPath()).toString(StandardCharsets.UTF_8)
            val malformed = listOf(
                original.dropLast(1),
                original.replaceFirst("{", "{\"unknown\":\"x\",") ,
                original.replaceFirst("{", "{\"contractId\":\"duplicate\",") ,
                original.replaceFirst("\"state\":\"${HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.STATE}\",", ""),
            )
            malformed.forEach { candidate ->
                Files.write(path.toPath(), candidate.toByteArray(StandardCharsets.UTF_8))
                assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.MALFORMED_EXISTING_ARTIFACT, failed(persist(materialized, root)).reason)
            }
        } finally { root.deleteRecursively() }
    }

    @Test
    fun wrongContractVersionStateAndDigestFailClosed() {
        val root = tempRoot()
        try {
            val materialized = Fixture.materialized()
            val result = completed(persist(materialized, root))
            val path = result.path(root)
            val original = Files.readAllBytes(path.toPath()).toString(StandardCharsets.UTF_8)
            listOf(
                original.replaceFirst(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.CONTRACT_ID, "WRONG"),
                original.replaceFirst("\"version\":\"1\"", "\"version\":\"2\""),
                original.replaceFirst(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.STATE, "WRONG_STATE"),
                original.replaceFirst("\"recordLogicalDigest\":\"", "\"recordLogicalDigest\":\"f"),
            ).forEach { candidate ->
                Files.write(path.toPath(), candidate.toByteArray(StandardCharsets.UTF_8))
                assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.MALFORMED_EXISTING_ARTIFACT, failed(persist(materialized, root)).reason)
            }
        } finally { root.deleteRecursively() }
    }

    @Test
    fun inputMutationIsRejectedBeforePublication() {
        val materialized = Fixture.materialized()
        val altered = materialized.copy(p1Lineage = materialized.p1Lineage.copy(evidenceReferenceIds = listOf("foreign")))
        val root = tempRoot()
        try {
            val failure = failed(persist(altered, root))
            assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.INVALID_MATERIALIZED_INPUT, failure.reason)
            assertEquals(emptyList<File>(), root.listFiles()?.toList().orEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test
    fun validationRejectsNonCanonicalBindingOrderAndDigestTampering() {
        val materialized = Fixture.materialized()
        val record = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.recordFrom(materialized)
        val firstBinding = record.evidenceBindings.single()
        val secondBinding = firstBinding.copy(persistedEvidenceReferenceId = "negative-evidence-2")
        val canonicalRecord = record.copy(
            p1Lineage = record.p1Lineage.copy(evidenceReferenceIds = listOf("negative-evidence-1", "negative-evidence-2")),
            evidenceBindings = listOf(firstBinding, secondBinding),
            recordLogicalDigest = "",
        ).let { unsigned ->
            unsigned.copy(recordLogicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.recordLogicalDigest(unsigned))
        }
        val reversed = canonicalRecord.copy(evidenceBindings = canonicalRecord.evidenceBindings.reversed())
        assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.validateRecord(reversed) }
        val tampered = record.copy(recordLogicalDigest = "0".repeat(64))
        assertFailsWith<IllegalArgumentException> { HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.validateRecord(tampered) }
    }

    @Test
    fun pathIsDirectAndMaterializationIdCannotEscapeRoot() {
        val root = tempRoot()
        try {
            val path = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.pathFor(Fixture.materialized().materializationId, root)
            assertEquals(root.canonicalFile, path.parentFile.canonicalFile)
            assertTrue(path.name.endsWith(".negative-training-example.v1.json"))
            assertFailsWith<IllegalArgumentException> {
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.pathFor(
                    HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1("negative-materialization:v1:${"z".repeat(64)}"),
                    root,
                )
            }
        } finally { root.deleteRecursively() }
    }

    @Test
    fun twoMaterializationsMayShareGenericContentWithoutConflict() {
        val root = tempRoot()
        try {
            val first = Fixture.materialized("f")
            val second = Fixture.materialized("e")
            val firstResult = completed(persist(first, root))
            val secondResult = completed(persist(second, root))
            assertEquals(firstResult.record.genericNegativeExample, secondResult.record.genericNegativeExample)
            assertNotEquals(firstResult.record.p1MaterializationId, secondResult.record.p1MaterializationId)
            assertEquals(firstResult.record, HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.readByMaterializationId(first.materializationId.value, root))
            assertEquals(secondResult.record, HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.readByMaterializationId(second.materializationId.value, root))
            assertEquals(2, root.listFiles()?.count { it.isFile })
        } finally { root.deleteRecursively() }
    }

    @Test
    fun rootAndTargetSymlinksAreRejected() {
        val parent = tempRoot()
        val targetRoot = File(parent, "target-root")
        val rootLink = File(parent, "root-link")
        try {
            Files.createDirectory(targetRoot.toPath())
            Files.createSymbolicLink(rootLink.toPath(), targetRoot.toPath())
            assertEquals(
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.UNSAFE_PATH,
                failed(persist(Fixture.materialized(), rootLink)).reason,
            )

            val safeRoot = File(parent, "safe-root").also { it.mkdirs() }
            val materialized = Fixture.materialized()
            val target = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.pathFor(materialized.materializationId, safeRoot)
            val outside = File(parent, "outside.json").also { it.writeText("outside\n") }
            Files.createSymbolicLink(target.toPath(), outside.toPath())
            assertEquals(
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.FailureReason.UNSAFE_PATH,
                failed(persist(materialized, safeRoot)).reason,
            )
        } finally { parent.deleteRecursively() }
    }

    @Test
    fun persistenceSurfaceHasNoSourceOrInferenceDependency() {
        val names = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1::class.java.declaredMethods.map { it.name }.toSet()
        assertTrue(names.contains("execute"))
        assertTrue(names.contains("readByMaterializationId"))
        assertTrue(names.none { it.contains("search", ignoreCase = true) || it.contains("fetch", ignoreCase = true) || it.contains("inference", ignoreCase = true) })
    }

    private fun completed(result: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Result) =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Result.Completed>(result)

    private fun failed(result: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Result) =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Result.Failed>(result)

    private fun persist(materialized: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Materialized, root: File) =
        HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.execute(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Request(materialized, root),
        )

    private fun HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExamplePersistenceV1.Result.Completed.path(root: File): File =
        File(root, relativePath)

    private fun tempRoot(): File = Files.createTempDirectory("him-negative-example-persistence-").toFile().canonicalFile

    private object Fixture {
        private val canonicalId = HimEntityId("AbCd12")
        private val evidenceReference = HimEvidenceReference("OPEN_FOOD_FACTS", HimSha256("d".repeat(64)), "off:product:row:1")
        private val positive = HimTrainingExampleV1.create(
            taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
            input = HimTrainingInputV1(
                observedTerm = "unknown food",
                normalizedObservedTerm = "unknown food",
                canonicalContext = listOf(HimCandidateCanonicalContext(1, canonicalId, "Canonical", null)),
                evidence = listOf(HimTrainingEvidenceInputV1(evidenceReference, "FOOD", 1)),
            ),
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId)),
            provenance = HimTrainingProvenanceV1(sourceEvidenceReferences = listOf(evidenceReference)),
        )
        private val rejectedTarget = HimTrainingTargetV1.NewCanonical("new canonical")
        private val boundary = HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY

        fun materialized(recordPrefix: String = "f"): HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Materialized {
            val recordId = recordPrefix.repeat(64)
            val negative = HimNegativeTrainingExampleV1.create(positive, rejectedTarget, boundary)
            val lineage = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.P1Lineage(
                negativeSupervisionRecordId = recordId,
                negativeCandidateId = "a".repeat(64),
                readinessDecisionId = "b".repeat(64),
                readinessState = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
                eligibilityBatchId = "eligibility-batch",
                eligibilityInputBindingDigest = "c".repeat(64),
                eligibilityDecisionId = "c".repeat(64),
                eligibilityState = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE,
                validationBatchId = "validation-batch",
                validationBatchBindingDigest = "d".repeat(64),
                validationBatchLogicalDigest = "e".repeat(64),
                validationRecordId = "e".repeat(64),
                reviewUnitId = "review-unit",
                stableEntryId = "f".repeat(64),
                canonicalEntityId = canonicalId.value,
                originalReviewerRef = "reviewer:original",
                validatorReviewerRef = "reviewer:validator",
                validationRound = 1,
                validationRevision = 1,
                originalDecision = HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION,
                assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
                validationReasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION),
                evidenceReferenceIds = listOf("negative-evidence-1"),
                downstreamRoute = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
                candidateState = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
                rejectedTarget = rejectedTarget,
                boundaryType = boundary,
                occurrenceReference = "occurrence:v1:${"b".repeat(64)}",
                positiveTrainingExampleReference = positive.exampleReference,
                materializationDecisionId = "1".repeat(64),
                projectionInputBindingDigest = "2".repeat(64),
                proofBindingDigest = "3".repeat(64),
                proofLogicalDigest = "4".repeat(64),
            )
            val materializationId = "negative-materialization:v1:${sha256(buildString {
                appendLine("HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_TRAINING_EXAMPLE_MATERIALIZATION_ID_V1")
                appendLine("negativeSupervisionRecordId=$recordId")
                appendLine("negativeExampleReference=${negative.reference.value}")
            })}"
            return HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.Materialized(
                materializationId = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeTrainingExampleMaterializationV1.MaterializationReferenceV1(materializationId),
                negativeTrainingExample = negative,
                p1Lineage = lineage,
                evidenceBindings = listOf(
                    HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionToTrainingBindingContextContractV1.EvidenceBinding(
                        "negative-evidence-1", HimTrainingEvidenceInputV1(evidenceReference, "FOOD", 1),
                    ),
                ),
            )
        }

        private fun sha256(value: String): String = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
