package de.shopme.testing.system.tools.knowledge.him.training.teacher

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.training.scaling.HimCandidateDatasetBindingV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerInputV1
import de.shopme.tools.knowledge.him.training.scaling.HimCanonicalGroundTruthScalingPlannerV1
import de.shopme.tools.knowledge.him.training.teacher.HimSmallMultiItemTeacherPilotSelectionV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RunHimSmallMultiItemTeacherPilotSelectionV1Test {
    @Test
    fun selectsExactlyThreeDeterministicExistingItemsOffline() {
        val root = projectRoot()
        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
        val catalog = HimProductOnlyCanonicalMasterReader().read(HimCanonicalFamilyPaths(root))
        val plan = HimCanonicalGroundTruthScalingPlannerV1().plan(
            HimCanonicalGroundTruthScalingPlannerInputV1(
                catalog,
                authority,
                active.releaseReference,
                candidateBinding(root),
                emptyList(),
            ),
        )
        val selected = HimSmallMultiItemTeacherPilotSelectionV1.select(
            plan.workItems,
            setOf(PREVIOUS_WORK_ITEM),
        )
        val replay = HimSmallMultiItemTeacherPilotSelectionV1.select(
            plan.workItems,
            setOf(PREVIOUS_WORK_ITEM),
        )
        assertEquals(3, selected.size)
        assertEquals(selected.map { it.reference }, selected.map { it.reference }.distinct())
        assertEquals(selected.map { it.reference }, replay.map { it.reference })
        assertFalse(selected.any { it.reference == PREVIOUS_WORK_ITEM })
        selected.forEach { item ->
            assertEquals(plan.families.single { family -> family.canonicalId == item.canonicalId }.partition, item.partition)
        }
    }

    private fun candidateBinding(root: File): HimCandidateDatasetBindingV1? {
        val file = root.resolve("${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json")
        if (!file.isFile) return null
        return HimCandidateDatasetBindingV1(
            "${HimCandidateDatasetContractV2.MASTER_ROOT}/candidate-dataset.v2.json",
            de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1.datasetDigest(HimCandidateDatasetPersistenceV2.readDataset(file)),
        )
    }

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("user.dir unavailable")).canonicalFile
        while (current.parentFile != null && !current.resolve("settings.gradle").isFile && !current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    companion object {
        private const val PREVIOUS_WORK_ITEM = "teacher-work:v1:0012490f1d38f892a294aad1e771999a90d44a2bbbd9c38b3193af75f8fa792a"
    }
}
