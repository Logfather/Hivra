package de.shopme.testing.system.tools.knowledge.him.training.scaling

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonical
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateCanonicalContext
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateInputRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateRunReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingInputV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionPolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingProvenanceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTaskTypeV1
import de.shopme.tools.knowledge.him.training.scaling.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.MessageDigest

class RunHimCanonicalGroundTruthScalingV1Test {

    @Test
    fun `deterministic family inventory has one item per catalog Canonical`() {
        val plan = planner().plan(input())

        assertEquals(3, plan.diagnostics.canonicalFamilyCount)
        assertEquals(listOf("Abc123", "OzlByp", "Qwe456"), plan.families.map { it.canonicalId.value })
        assertEquals(3, plan.families.map { it.canonicalId }.distinct().size)
    }

    @Test
    fun `catalog Canonical missing from Authority hard fails`() {
        val authority = authority().copy(families = authority().families.drop(1))
        assertFails { planner().plan(input(authority = authority)) }
    }

    @Test
    fun `duplicate Canonical ID hard fails`() {
        val base = authority().families
        val duplicate = base[0].copy(canonicalName = "Other duplicate")
        assertFails { planner().plan(input(authority = authority().copy(families = base + duplicate))) }
    }

    @Test
    fun `projectable Variant is present`() {
        val hering = family(HERRING_ID, "Hering", variantId = HERRING_VARIANT_ID)
        val plan = planner().plan(input(authority = authority(families = listOf(hering)), catalog = catalog(listOf(record("Hering", "hering"))), childLineage = listOf(heringLineage())))
        val coverage = plan.families.single().coverage(HimTrainingClassificationV1.VARIANT)

        assertEquals(HimCanonicalGroundTruthScalingCoverageStateV1.PRESENT, coverage.state)
        assertEquals(1, coverage.projectablePositiveCount)
        assertEquals(1, plan.diagnostics.variantCoverageCount)
    }

    @Test
    fun `Authority child without lineage is explicitly blocked`() {
        val blockedId = HimEntityId("Blk001")
        val blockedFamily = family(blockedId, "Blocked fish", variantId = HimEntityId("Blk002"))
        val plan = planner().plan(input(
            catalog = catalog(listOf(record("Blocked fish", "blocked fish"))),
            authority = authority(families = listOf(blockedFamily)),
            childLineage = emptyList(),
        ))
        val coverage = plan.families.single().coverage(HimTrainingClassificationV1.VARIANT)

        assertEquals(HimCanonicalGroundTruthScalingCoverageStateV1.NOT_YET_PROJECTABLE, coverage.state)
        assertEquals(HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_LINEAGE, coverage.projectability)
        assertEquals(1, coverage.blockedByLineageCount)
    }

    @Test
    fun `real Hering coverage is discovered from the supplied F3 8b lineage`() {
        val plan = planner().plan(input(
            catalog = catalog(listOf(record("Hering", "hering"))),
            authority = authority(families = listOf(family(HERRING_ID, "Hering", HERRING_VARIANT_ID))),
            childLineage = listOf(heringLineage()),
        ))
        val family = plan.families.single()

        assertEquals(HERRING_ID, family.canonicalId)
        assertEquals("Hering", family.canonicalName)
        assertEquals(HimTrainingPartitionV1.VALIDATION, family.partition)
        assertEquals(HimCanonicalGroundTruthScalingCoverageStateV1.PRESENT, family.coverage(HimTrainingClassificationV1.VARIANT).state)
        assertEquals(HimCanonicalGroundTruthScalingCoverageStateV1.NOT_YET_PROJECTABLE, family.coverage(HimTrainingClassificationV1.EXISTING_CANONICAL).state)
        assertEquals(HimCanonicalGroundTruthScalingCoverageStateV1.NOT_YET_PROJECTABLE, family.coverage(HimTrainingClassificationV1.NEW_CANONICAL).state)
        assertEquals(
            HimNegativeTrainingExamplePolicyV1.derive(heringExample()).size,
            family.derivedNegativeExampleCount,
        )
        assertTrue(family.derivedNegativeBoundaryTypes.isNotEmpty())
        assertEquals(HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_CONTRACT, family.coverage(HimTrainingClassificationV1.NEW_CANONICAL).projectability)
    }

    @Test
    fun `negative coverage is bounded to F3 8c derivation`() {
        val plan = planner().plan(input(
            catalog = catalog(listOf(record("Hering", "hering"))),
            authority = authority(families = listOf(family(HERRING_ID, "Hering", HERRING_VARIANT_ID))),
            childLineage = listOf(heringLineage()),
        ))

        assertEquals(1, plan.diagnostics.totalProjectablePositiveExamples)
        assertEquals(1, plan.families.single().projectablePositiveExampleReferences.size)
        assertEquals(1, plan.diagnostics.familiesWithProjectablePositives)
        assertEquals(4, plan.diagnostics.totalDerivedNegativeExamples)
    }

    @Test
    fun `no Catalog-wide wrong-scope combinations are generated`() {
        val plan = planner().plan(input())

        assertEquals(1, plan.diagnostics.totalProjectablePositiveExamples)
        assertEquals(HimNegativeTrainingExamplePolicyV1.derive(heringExample()).size, plan.diagnostics.totalDerivedNegativeExamples)
        assertTrue(plan.families.all { it.derivedNegativeExampleCount <= it.projectablePositiveExampleReferences.size * 5 })
    }

    @Test
    fun `work items retain F3 8d family partition including HOLDOUT`() {
        val holdoutId = listOf("Aaa333", "Bbb555")
            .map(::HimEntityId)
            .first { id -> HimTrainingPartitionPolicyV1.partitionForGroup(de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1.canonical(id)) == HimTrainingPartitionV1.HOLDOUT }
        val plan = planner().plan(input(
            catalog = catalog(listOf(record("Holdout fish", "holdout fish"))),
            authority = authority(families = listOf(family(holdoutId, "Holdout fish"))),
            childLineage = emptyList(),
        ))
        val workItem = plan.workItems.single()

        assertEquals(HimTrainingPartitionV1.HOLDOUT, plan.families.single().partition)
        assertEquals(HimTrainingPartitionV1.HOLDOUT, workItem.partition)
    }

    @Test
    fun `input order does not change plan or work item identity`() {
        val first = planner().plan(input())
        val second = planner().plan(input(
            catalog = catalog(listOf(record("Other fish", "other fish"), record("Hering", "hering"), record("Apple", "apple"))),
            authority = authority(families = authority().families.reversed()),
            childLineage = listOf(heringLineage()).reversed(),
        ))

        assertEquals(first, second)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.workItems.map { it.reference }, second.workItems.map { it.reference })
    }

    @Test
    fun `release and Catalog bindings change plan identity`() {
        val first = planner().plan(input())
        val changedRelease = planner().plan(input(groundTruthReleaseReference = RELEASE_B))
        val changedCatalog = planner().plan(input(
            catalog = catalog(contentSha256 = "b".repeat(64)),
            authority = authority(sourceCatalogSha256 = "b".repeat(64)),
            childLineage = listOf(heringLineage()),
        ))

        assertNotEquals(first.logicalDigest, changedRelease.logicalDigest)
        assertNotEquals(first.logicalDigest, changedCatalog.logicalDigest)
        assertNotEquals(
            first.workItems.first { it.canonicalId == HERRING_ID }.reference,
            changedRelease.workItems.first { it.canonicalId == HERRING_ID }.reference,
        )
    }

    @Test
    fun `same missing coverage produces deterministic work item`() {
        val first = planner().plan(input())
        val second = planner().plan(input())

        assertEquals(first.workItems, second.workItems)
        assertEquals(HimCanonicalGroundTruthScalingWorkStatusV1.TEACHER_WORK_REQUIRED, first.families.first { it.canonicalId == HERRING_ID }.workStatus)
    }

    @Test
    fun `family with no projectable positive is visible in diagnostics`() {
        val plan = planner().plan(input(
            catalog = catalog(listOf(record("Apple", "apple"))),
            authority = authority(families = listOf(family(APPLE_ID, "Apple"))),
            childLineage = emptyList(),
        ))
        val family = plan.families.single()

        assertEquals(0, plan.diagnostics.familiesWithProjectablePositives)
        assertEquals(1, plan.diagnostics.familiesWithNoProjectablePositives)
        assertEquals(HimCanonicalGroundTruthScalingCoverageStateV1.MISSING, family.coverage(HimTrainingClassificationV1.VARIANT).state)
        assertEquals(HimCanonicalGroundTruthScalingCoverageStateV1.NOT_YET_PROJECTABLE, family.coverage(HimTrainingClassificationV1.EXISTING_CANONICAL).state)
        assertEquals(1, plan.diagnostics.teacherWorkFamilyCount)
    }

    @Test
    fun `partition reassignment and duplicate work item hard fail validation`() {
        val plan = planner().plan(input())
        val workItem = plan.workItems.first { it.canonicalId == HERRING_ID }
        val reassigned = workItem.copy(
            partition = when (workItem.partition) {
                HimTrainingPartitionV1.TRAIN -> HimTrainingPartitionV1.HOLDOUT
                HimTrainingPartitionV1.VALIDATION -> HimTrainingPartitionV1.TRAIN
                HimTrainingPartitionV1.HOLDOUT -> HimTrainingPartitionV1.VALIDATION
            },
        )
        assertFails { HimCanonicalGroundTruthScalingPlanValidatorV1.validateOrThrow(plan.copy(workItems = listOf(reassigned))) }
        assertFails { HimCanonicalGroundTruthScalingPlanValidatorV1.validateOrThrow(plan.copy(workItems = listOf(workItem, workItem))) }
    }

    @Test
    fun `plan diagnostics match family contents`() {
        val plan = planner().plan(input())

        assertEquals(plan.families.size, plan.diagnostics.canonicalFamilyCount)
        assertEquals(plan.families.sumOf { it.projectablePositiveExampleReferences.size }, plan.diagnostics.totalProjectablePositiveExamples)
        assertEquals(plan.families.sumOf { it.derivedNegativeExampleCount }, plan.diagnostics.totalDerivedNegativeExamples)
        assertEquals(plan.families.count { it.partition == HimTrainingPartitionV1.TRAIN }, plan.diagnostics.trainFamilyCount)
        assertEquals(plan.families.count { it.partition == HimTrainingPartitionV1.VALIDATION }, plan.diagnostics.validationFamilyCount)
        assertEquals(plan.families.count { it.partition == HimTrainingPartitionV1.HOLDOUT }, plan.diagnostics.holdoutFamilyCount)
        assertTrue(HimCanonicalGroundTruthScalingPlanValidatorV1.validate(plan).valid)
    }

    @Test
    fun `identity scoped Variant remains owned by parent Canonical`() {
        val family = family(HERRING_ID, "Hering", HERRING_VARIANT_ID)
        val lineage = heringLineage().copy(
            projectedExample = heringExample(target = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Identity(HERRING_ID, HimEntityId("Iden01")))),
        )
        assertFails { planner().plan(input(authority = authority(families = listOf(family)), catalog = catalog(listOf(record("Hering", "hering"))), childLineage = listOf(lineage))) }
    }

    private fun planner() = HimCanonicalGroundTruthScalingPlannerV1()

    private fun input(
        catalog: HimProductOnlyCanonicalMaster = catalog(),
        authority: HimCanonicalFamilyAuthority = authority(),
        groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1 = RELEASE_A,
        childLineage: List<HimCanonicalGroundTruthScalingChildLineageV1> = listOf(heringLineage(groundTruthReleaseReference)),
    ) = HimCanonicalGroundTruthScalingPlannerInputV1(
        catalog = catalog,
        authority = authority,
        groundTruthReleaseReference = groundTruthReleaseReference,
        candidateDatasetBinding = HimCandidateDatasetBindingV1(
            path = "data/knowledge/him/candidates/master/candidate-dataset.v2.json",
            digest = HimSha256("c".repeat(64)),
        ),
        childLineage = childLineage,
    )

    private fun catalog(
        records: List<HimProductOnlyCanonical> = listOf(
            record("Hering", "hering"),
            record("Apple", "apple"),
            record("Other fish", "other fish"),
        ),
        contentSha256: String = CATALOG_SHA,
    ) = HimProductOnlyCanonicalMaster(
        path = CATALOG_PATH,
        contentSha256 = contentSha256,
        records = records,
    )

    private fun authority(
        families: List<HimCanonicalFamily> = listOf(
            family(HERRING_ID, "Hering", HERRING_VARIANT_ID),
            family(APPLE_ID, "Apple"),
            family(OTHER_ID, "Other fish"),
        ),
        sourceCatalogSha256: String = CATALOG_SHA,
    ) = HimCanonicalFamilyAuthority(
        schemaVersion = "1",
        sourceCatalog = HimCanonicalFamilySourceCatalog(CATALOG_PATH, sourceCatalogSha256, families.size),
        families = families,
    )

    private fun family(
        id: HimEntityId,
        name: String,
        variantId: HimEntityId? = null,
    ) = HimCanonicalFamily(
        canonicalId = id,
        canonicalName = name,
        normalizedName = name.lowercase(),
        taxonomyPaths = listOf(listOf("food", name.lowercase())),
        lifecycleStatus = HimLifecycleStatus.ACTIVE,
        identities = emptyList(),
        variants = listOfNotNull(variantId?.let { HimCanonicalVariant(it, "$name eingelegt", "$name eingelegt".lowercase(), HimLifecycleStatus.ACTIVE) }),
        aliases = emptyList(),
    )

    private fun record(name: String, normalized: String) = HimProductOnlyCanonical(
        itemname = name,
        normalized = normalized,
        taxonomyPaths = listOf(listOf("food", normalized)),
    )

    private fun heringLineage(release: HimGroundTruthReleaseIdentityV1 = RELEASE_A) = HimCanonicalGroundTruthScalingChildLineageV1(
        canonicalId = HERRING_ID,
        childId = HERRING_VARIANT_ID,
        classification = HimTrainingClassificationV1.VARIANT,
        projectability = HimCanonicalGroundTruthScalingProjectabilityV1.PROJECTABLE,
        projectedExample = heringExample(release = release),
    )

    private fun heringExample(
        target: HimTrainingTargetV1 = HimTrainingTargetV1.Variant(HimFamilyEntityReference.Canonical(HERRING_ID)),
        release: HimGroundTruthReleaseIdentityV1 = RELEASE_A,
    ) = example(
        term = "Hering eingelegt",
        canonicalId = HERRING_ID,
        promotedEntityId = HERRING_VARIANT_ID,
        target = target,
        contextIds = listOf(HERRING_ID, OTHER_ID),
        seed = "hering",
        release = release,
    )

    private fun example(
        term: String,
        canonicalId: HimEntityId,
        promotedEntityId: HimEntityId,
        target: HimTrainingTargetV1,
        contextIds: List<HimEntityId>,
        seed: String,
        release: HimGroundTruthReleaseIdentityV1,
    ) = HimTrainingExampleV1.create(
        taskType = HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION,
        input = HimTrainingInputV1(
            observedTerm = term,
            normalizedObservedTerm = term.lowercase(),
            canonicalContext = contextIds.mapIndexed { index, id ->
                HimCandidateCanonicalContext(index + 1, id, if (id == canonicalId) "Hering" else "Other fish", null)
            },
        ),
        target = target,
        provenance = HimTrainingProvenanceV1(
            candidateReference = HimCandidateReference("candidate:v1:${hex(seed + "candidate")}"),
            generationRunReference = HimCandidateRunReference("run:v2:${hex(seed + "run")}"),
            inputRunReference = HimCandidateInputRunReference("input-run:v1:${hex(seed + "input")}"),
            validationReference = HimCandidateValidationDecisionReference("validation:v1:${hex(seed + "validation")}"),
            promotionReference = HimCandidatePromotionReference("promotion:v1:${hex(seed + "promotion")}"),
            mutationReference = HimMutationReference("mutation:v1:${hex(seed + "mutation")}"),
            groundTruthReleaseReference = release,
            promotedEntityId = promotedEntityId,
            promotedEntityType = HimEntityType.VARIANT,
        ),
    )

    private fun hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            fail("Expected hard failure")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private companion object {
        const val CATALOG_PATH = "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json"
        val CATALOG_SHA = "a".repeat(64)
        val RELEASE_A = HimGroundTruthReleaseIdentityV1("release:v1:${"d".repeat(64)}")
        val RELEASE_B = HimGroundTruthReleaseIdentityV1("release:v1:${"e".repeat(64)}")
        val HERRING_ID = HimEntityId("OzlByp")
        val HERRING_VARIANT_ID = HimEntityId("bvoJLp")
        val APPLE_ID = HimEntityId("Abc123")
        val OTHER_ID = HimEntityId("Qwe456")
    }
}
