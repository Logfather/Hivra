package de.shopme.testing.system.tools.knowledge.him.training.corpus

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimEvidenceReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolutionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionAssignmentV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionBuildResultV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionContractV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionLeakageLevelV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionLeakageValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestIdentityV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionPolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionerV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingEvidenceInputV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RunHimTrainingPartitionContractV1Test {

    @Test
    fun `multiple positives from same family share one partition`() {
        val records = listOf(
            HimTrainingPartitionRecordV1.Positive(positiveRecord("Hering geräuchert", HERRING_ID)),
            HimTrainingPartitionRecordV1.Positive(positiveRecord("Hering in Öl", HERRING_ID)),
            HimTrainingPartitionRecordV1.Positive(positiveRecord("Hering eingelegt", HERRING_ID)),
        )
        val manifest = partition(records)
        assertEquals(1, manifest.assignments.map { it.groupReference }.distinct().size)
        assertEquals(1, manifest.assignments.map { it.partition }.distinct().size)
    }

    @Test
    fun `negative records inherit the positive family partition`() {
        val positive = heringPositive()
        val negativeRecords = HimTrainingPartitionerV1().let { partitioner ->
            val negatives = de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1.derive(positive)
            partitioner.assign(
                listOf(HimTrainingPartitionRecordV1.Positive(positive)) +
                    negatives.map { HimTrainingPartitionRecordV1.Negative(it) },
            )
        }
        val manifest = (negativeRecords as HimTrainingPartitionBuildResultV1.Partitioned).manifest
        assertEquals(1, manifest.assignments.map { it.partition }.distinct().size)
        val negativeCount = manifest.assignments.count {
            it.record is HimTrainingPartitionRecordV1.Negative
        }
        assertEquals(
            negativeCount,
            manifest.diagnostics.trainNegativeCount +
                manifest.diagnostics.validationNegativeCount +
                manifest.diagnostics.holdoutNegativeCount,
        )
        manifest.assignments.filter { it.record is HimTrainingPartitionRecordV1.Negative }.forEach { negative ->
            assertEquals(
                manifest.assignments.first { it.record is HimTrainingPartitionRecordV1.Positive }.partition,
                negative.partition,
            )
        }
    }

    @Test
    fun `real Hering positive and all supported boundaries share one partition`() {
        val positive = heringPositive()
        val negatives = de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1.derive(positive)
        val manifest = partition(
            listOf(HimTrainingPartitionRecordV1.Positive(positive)) + negatives.map { HimTrainingPartitionRecordV1.Negative(it) },
        )
        assertEquals(HimTrainingFamilyGroupReferenceV1.canonical(HERRING_ID), manifest.assignments.first().groupReference)
        assertEquals(1, manifest.assignments.map { it.partition }.distinct().size)
        assertEquals(1, manifest.diagnostics.familyGroupCount)
    }

    @Test
    fun `different families have independent deterministic assignments`() {
        val records = listOf(
            positiveRecord("Hering", HERRING_ID),
            positiveRecord("Makrele", HimEntityId("Abc123")),
            positiveRecord("Forelle", HimEntityId("Qwe456")),
        )
        val manifest = partition(records.map { HimTrainingPartitionRecordV1.Positive(it) })
        manifest.assignments.forEach { assignment ->
            assertEquals(HimTrainingPartitionPolicyV1.partitionForGroup(assignment.groupReference), assignment.partition)
        }
        assertEquals(3, manifest.diagnostics.familyGroupCount)
    }

    @Test
    fun `input order does not affect assignments or manifest identity`() {
        val records = listOf(
            HimTrainingPartitionRecordV1.Positive(positiveRecord("Hering", HERRING_ID)),
            HimTrainingPartitionRecordV1.Positive(positiveRecord("Makrele", HimEntityId("Abc123"))),
            HimTrainingPartitionRecordV1.Positive(positiveRecord("Forelle", HimEntityId("Qwe456"))),
        )
        val first = partition(records)
        val second = partition(records.reversed())
        assertEquals(first, second)
        assertEquals(first.logicalDigest, second.logicalDigest)
    }

    @Test
    fun `same group always has same hash bucket`() {
        val group = HimTrainingFamilyGroupReferenceV1.canonical(HERRING_ID)
        assertEquals(HimTrainingPartitionPolicyV1.bucketForGroup(group), HimTrainingPartitionPolicyV1.bucketForGroup(group))
        assertEquals(HimTrainingPartitionPolicyV1.partitionForGroup(group), HimTrainingPartitionPolicyV1.partitionForGroup(group))
    }

    @Test
    fun `policy bucket boundaries are exact`() {
        assertEquals(HimTrainingPartitionV1.TRAIN, HimTrainingPartitionPolicyV1.partitionForBucket(HimTrainingPartitionContractV1.TRAIN_LAST_BUCKET))
        assertEquals(HimTrainingPartitionV1.VALIDATION, HimTrainingPartitionPolicyV1.partitionForBucket(HimTrainingPartitionContractV1.VALIDATION_FIRST_BUCKET))
        assertEquals(HimTrainingPartitionV1.VALIDATION, HimTrainingPartitionPolicyV1.partitionForBucket(HimTrainingPartitionContractV1.VALIDATION_LAST_BUCKET))
        assertEquals(HimTrainingPartitionV1.HOLDOUT, HimTrainingPartitionPolicyV1.partitionForBucket(HimTrainingPartitionContractV1.HOLDOUT_FIRST_BUCKET))
    }

    @Test
    fun `exact duplicate positive reference hard fails`() {
        val positive = positiveRecord("Hering", HERRING_ID)
        assertFails {
            HimTrainingPartitionerV1().assign(
                listOf(HimTrainingPartitionRecordV1.Positive(positive), HimTrainingPartitionRecordV1.Positive(positive)),
            )
        }
    }

    @Test
    fun `exact duplicate negative reference hard fails`() {
        val positive = heringPositive()
        val negative = de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1.create(
            positive,
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        assertFails {
            HimTrainingPartitionerV1().assign(
                listOf(
                    HimTrainingPartitionRecordV1.Positive(positive),
                    HimTrainingPartitionRecordV1.Negative(negative),
                    HimTrainingPartitionRecordV1.Negative(negative),
                ),
            )
        }
    }

    @Test
    fun `missing positive for negative hard fails`() {
        val positive = heringPositive()
        val negative = de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1.create(
            positive,
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        assertFails { HimTrainingPartitionerV1().assign(listOf(HimTrainingPartitionRecordV1.Negative(negative))) }
    }

    @Test
    fun `manual exact example split is reported as leakage`() {
        val positive = positiveRecord("Hering", HERRING_ID)
        val group = HimTrainingFamilyGroupReferenceV1.canonical(HERRING_ID)
        val assignments = listOf(
            HimTrainingPartitionAssignmentV1(HimTrainingPartitionRecordV1.Positive(positive), group, HimTrainingPartitionV1.TRAIN),
            HimTrainingPartitionAssignmentV1(HimTrainingPartitionRecordV1.Positive(positive), group, HimTrainingPartitionV1.HOLDOUT),
        )
        val result = HimTrainingPartitionLeakageValidatorV1.validate(HimTrainingPartitionManifestV1.create(assignments))
        assertFalse(result.valid)
        assertTrue(result.diagnostics.any { it.level == HimTrainingPartitionLeakageLevelV1.EXACT_EXAMPLE_LEAKAGE })
    }

    @Test
    fun `manual derived negative split is reported as leakage`() {
        val positive = heringPositive()
        val negative = de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExampleV1.create(
            positive,
            HimTrainingTargetV1.NewCanonical(),
            HimNegativeBoundaryTypeV1.CANONICAL_VS_CHILD_BOUNDARY,
        )
        val group = HimTrainingFamilyGroupReferenceV1.canonical(HERRING_ID)
        val assignments = listOf(
            HimTrainingPartitionAssignmentV1(HimTrainingPartitionRecordV1.Positive(positive), group, HimTrainingPartitionV1.TRAIN),
            HimTrainingPartitionAssignmentV1(HimTrainingPartitionRecordV1.Negative(negative), group, HimTrainingPartitionV1.HOLDOUT),
        )
        val result = HimTrainingPartitionLeakageValidatorV1.validate(HimTrainingPartitionManifestV1.create(assignments))
        assertFalse(result.valid)
        assertTrue(result.diagnostics.any { it.level == HimTrainingPartitionLeakageLevelV1.DERIVED_NEGATIVE_LEAKAGE })
    }

    @Test
    fun `manual family split is reported as canonical family leakage`() {
        val first = positiveRecord("Hering", HERRING_ID)
        val second = positiveRecord("Hering eingelegt", HERRING_ID)
        val group = HimTrainingFamilyGroupReferenceV1.canonical(HERRING_ID)
        val assignments = listOf(
            HimTrainingPartitionAssignmentV1(HimTrainingPartitionRecordV1.Positive(first), group, HimTrainingPartitionV1.TRAIN),
            HimTrainingPartitionAssignmentV1(HimTrainingPartitionRecordV1.Positive(second), group, HimTrainingPartitionV1.HOLDOUT),
        )
        val result = HimTrainingPartitionLeakageValidatorV1.validate(HimTrainingPartitionManifestV1.create(assignments))
        assertFalse(result.valid)
        assertTrue(result.diagnostics.any { it.level == HimTrainingPartitionLeakageLevelV1.CANONICAL_FAMILY_LEAKAGE })
    }

    @Test
    fun `lineage split is reported when provenance crosses partitions`() {
        val first = heringPositive(observedTerm = "Hering A", normalizedTerm = "hering a")
        val second = positiveRecord("Makrele B", HimEntityId("Abc123"), provenance = first.provenance)
        val assignments = listOf(
            HimTrainingPartitionAssignmentV1(HimTrainingPartitionRecordV1.Positive(first), HimTrainingFamilyGroupReferenceV1.canonical(HERRING_ID), HimTrainingPartitionV1.TRAIN),
            HimTrainingPartitionAssignmentV1(HimTrainingPartitionRecordV1.Positive(second), HimTrainingFamilyGroupReferenceV1.canonical(HimEntityId("Abc123")), HimTrainingPartitionV1.HOLDOUT),
        )
        val result = HimTrainingPartitionLeakageValidatorV1.validate(HimTrainingPartitionManifestV1.create(assignments))
        assertFalse(result.valid)
        assertTrue(result.diagnostics.any { it.level == HimTrainingPartitionLeakageLevelV1.LINEAGE_LEAKAGE })
    }

    @Test
    fun `context-only cross-partition reference is diagnostic not family leakage`() {
        val firstId = HERRING_ID
        val secondId = listOf("Abc123", "Qwe456", "Zxc789", "Mno321").map(::HimEntityId)
            .first { HimTrainingPartitionPolicyV1.partitionForGroup(HimTrainingFamilyGroupReferenceV1.canonical(it)) != HimTrainingPartitionPolicyV1.partitionForGroup(HimTrainingFamilyGroupReferenceV1.canonical(firstId)) }
        val first = positiveRecord("Hering", firstId, contextIds = listOf(firstId, secondId))
        val second = positiveRecord("Other fish", secondId)
        val result = validate(
            listOf(
                HimTrainingPartitionRecordV1.Positive(first),
                HimTrainingPartitionRecordV1.Positive(second),
            ),
        )
        assertTrue(result.valid)
        assertTrue(result.diagnostics.any { it.level == HimTrainingPartitionLeakageLevelV1.CONTEXT_ONLY_CROSS_PARTITION_REFERENCE && !it.fatal })
        assertFalse(result.diagnostics.any { it.level == HimTrainingPartitionLeakageLevelV1.CANONICAL_FAMILY_LEAKAGE })
    }

    @Test
    fun `identity-scoped variant groups by parent Canonical`() {
        val positive = positiveRecord(
            term = "Braeburn geschält",
            canonicalId = HERRING_ID,
            target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Identity(HERRING_ID, HimEntityId("Iden01"))),
        )
        val resolution = de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolverV1.resolve(positive)
        assertEquals(HimTrainingFamilyGroupResolutionV1.Resolved(HimTrainingFamilyGroupReferenceV1.canonical(HERRING_ID)), resolution)
    }

    @Test
    fun `alias groups by equivalent Canonical`() {
        val positive = positiveRecord(
            term = "Heringsfisch",
            canonicalId = HERRING_ID,
            target = HimTrainingTargetV1.Alias(HimFamilyEntityReference.Canonical(HERRING_ID)),
        )
        val resolution = de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolverV1.resolve(positive)
        assertEquals(HimTrainingFamilyGroupResolutionV1.Resolved(HimTrainingFamilyGroupReferenceV1.canonical(HERRING_ID)), resolution)
    }

    @Test
    fun `NEW_CANONICAL is explicitly not yet groupable`() {
        val result = HimTrainingPartitionerV1().assign(
            listOf(HimTrainingPartitionRecordV1.Positive(positiveRecord("Unknown", HERRING_ID, HimTrainingTargetV1.NewCanonical())))
        )
        assertTrue(result is HimTrainingPartitionBuildResultV1.NotYetGroupable)
    }

    @Test
    fun `EXISTING_CANONICAL groups structurally when Canonical ID exists`() {
        val result = HimTrainingPartitionerV1().assign(
            listOf(HimTrainingPartitionRecordV1.Positive(positiveRecord("Hering", HERRING_ID, HimTrainingTargetV1.ExistingCanonical(HERRING_ID))))
        )
        assertTrue(result is HimTrainingPartitionBuildResultV1.Partitioned)
    }

    @Test
    fun `manifest identity is deterministic and valid`() {
        val manifest = partition(
            listOf(
                HimTrainingPartitionRecordV1.Positive(positiveRecord("Hering", HERRING_ID)),
                HimTrainingPartitionRecordV1.Positive(positiveRecord("Makrele", HimEntityId("Abc123"))),
            ),
        )
        assertEquals(2, manifest.diagnostics.totalRecordCount)
        assertEquals(manifest.logicalDigest, HimTrainingPartitionManifestIdentityV1.digest(manifest.policyVersion, manifest.assignments))
        assertTrue(HimTrainingPartitionLeakageValidatorV1.validate(manifest).valid)
    }

    @Test
    fun `Hering geräuchert in Öl and eingelegt cannot split into holdout`() {
        val manifest = partition(
            listOf(
                HimTrainingPartitionRecordV1.Positive(positiveRecord("Hering geräuchert", HERRING_ID)),
                HimTrainingPartitionRecordV1.Positive(positiveRecord("Hering in Öl", HERRING_ID)),
                HimTrainingPartitionRecordV1.Positive(positiveRecord("Hering eingelegt", HERRING_ID)),
            ),
        )
        assertEquals(1, manifest.assignments.map { it.partition }.distinct().size)
        assertEquals(1, manifest.assignments.map { it.groupReference }.distinct().size)
    }

    private fun partition(records: List<HimTrainingPartitionRecordV1>): de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionManifestV1 {
        return when (val result = HimTrainingPartitionerV1().assign(records)) {
            is HimTrainingPartitionBuildResultV1.Partitioned -> result.manifest
            is HimTrainingPartitionBuildResultV1.NotYetGroupable -> error(result.reason)
        }
    }

    private fun validate(records: List<HimTrainingPartitionRecordV1>) =
        HimTrainingPartitionLeakageValidatorV1.validate(partition(records))

    private fun positiveRecord(
        term: String,
        canonicalId: HimEntityId,
        target: HimTrainingTargetV1 = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(canonicalId)),
        contextIds: List<HimEntityId> = listOf(canonicalId),
        provenance: HimTrainingProvenanceV1 = HimTrainingProvenanceV1(),
    ): HimTrainingExampleV1 = HimTrainingExampleV1.create(
        taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
        input = HimTrainingInputV1(
            observedTerm = term,
            normalizedObservedTerm = term.lowercase(),
            canonicalContext = contextIds.mapIndexed { index, id ->
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(index + 1, id, "Canonical $id", null)
            },
        ),
        target = target,
        provenance = provenance,
    )

    private fun heringPositive(
        observedTerm: String = "Hering eingelegt",
        normalizedTerm: String = "hering eingelegt",
    ): HimTrainingExampleV1 = HimTrainingExampleV1.create(
        taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
        input = HimTrainingInputV1(
            observedTerm = observedTerm,
            normalizedObservedTerm = normalizedTerm,
            canonicalContext = listOf(
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(1, HERRING_ID, "Hering", null),
                de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext(2, OTHER_CANONICAL_ID, "Makrele", null),
            ),
            evidence = listOf(
                HimTrainingEvidenceInputV1(HimEvidenceReference("OPEN_FOOD_FACTS", ARTIFACT_SHA, "off:product:row:4530988:code:5701157480343"), "real-pilot-evidence", 1),
                HimTrainingEvidenceInputV1(HimEvidenceReference("AGRIBALYSE", ARTIFACT_SHA, "agribalyse:row:2171:agb:26010"), "real-pilot-evidence", 1),
                HimTrainingEvidenceInputV1(HimEvidenceReference("CIQUAL", ARTIFACT_SHA, "ciqual:food:26010"), "real-pilot-evidence", 1),
            ),
        ),
        target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HERRING_ID)),
        provenance = HimTrainingProvenanceV1(
            candidateReference = HimCandidateReference("candidate:v1:8df2826510366d3cacc78d81f1ad9b3d81d50ef61d7ff3b29195eaf1ac1fd2ef"),
            generationRunReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRunReference("run:v2:c1ab93c23dde76444a3585afd80b592038ecf9c14e39442846ddbaf7671a0823"),
            inputRunReference = de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputRunReference("input-run:v1:${"1".repeat(64)}"),
            validationReference = HimCandidateValidationDecisionReference("validation:v1:f0b328d718b10878cdbb4cf41ea462f4e8d27ec8b9197ac4ca101792d458cbb4"),
            promotionReference = HimCandidatePromotionReference("promotion:v1:3578a9afcbd7f5a3c2d774301ebf51c07a3835b9c0a81ed657ea084afa6b07c1"),
            mutationReference = HimMutationReference("mutation:v1:935c9e0a864b08cbb885f4abb9000090cd68011e30527cfb6335d2030f9eb576"),
            groundTruthReleaseReference = HimGroundTruthReleaseIdentityV1("release:v1:e877ccc673527e569520a9ee0932a79f6460ef3fabdb364a38200837e557c5f7"),
            promotedEntityId = HimEntityId("bvoJLp"),
            promotedEntityType = HimEntityType.VARIANT,
            sourceEvidenceReferences = listOf(
                HimEvidenceReference("OPEN_FOOD_FACTS", ARTIFACT_SHA, "off:product:row:4530988:code:5701157480343"),
                HimEvidenceReference("AGRIBALYSE", ARTIFACT_SHA, "agribalyse:row:2171:agb:26010"),
                HimEvidenceReference("CIQUAL", ARTIFACT_SHA, "ciqual:food:26010"),
            ),
            sourceArtifactDigests = listOf(ARTIFACT_SHA),
        ),
    )

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            fail("Expected hard failure")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private companion object {
        val HERRING_ID = HimEntityId("OzlByp")
        val OTHER_CANONICAL_ID = HimEntityId("Abc123")
        val ARTIFACT_SHA = HimSha256("1".repeat(64))
    }
}
