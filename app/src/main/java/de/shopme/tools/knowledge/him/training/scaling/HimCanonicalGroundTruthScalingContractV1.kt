package de.shopme.tools.knowledge.him.training.scaling

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetContractV2
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeBoundaryTypeV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionV1
import java.security.MessageDigest

object HimCanonicalGroundTruthScalingContractV1 {
    const val VERSION = "HIM_CANONICAL_GROUND_TRUTH_SCALING_V1"
    const val POLICY_VERSION = "HIM_CANONICAL_GROUND_TRUTH_SCALING_POLICY_V1"
    const val PLAN_IDENTITY_CONTRACT = "HIM_CANONICAL_GROUND_TRUTH_SCALING_PLAN_IDENTITY_V1"
    const val WORK_ITEM_REFERENCE_CONTRACT = "HIM_TEACHER_GROUND_TRUTH_WORK_ITEM_REFERENCE_V1"
}

enum class HimCanonicalGroundTruthScalingCoverageStateV1 {
    PRESENT,
    MISSING,
    NOT_YET_PROJECTABLE,
    NOT_APPLICABLE,
}

enum class HimCanonicalGroundTruthScalingProjectabilityV1 {
    PROJECTABLE,
    BLOCKED_BY_LINEAGE,
    BLOCKED_BY_CONTRACT,
}

enum class HimCanonicalGroundTruthScalingWorkStatusV1 {
    NO_TEACHER_WORK_REQUIRED,
    TEACHER_WORK_REQUIRED,
    BLOCKED_BY_LINEAGE,
    BLOCKED_BY_CONTRACT,
}

enum class HimCanonicalGroundTruthScalingWorkReasonV1 {
    MISSING_SEMANTIC_COVERAGE,
}

data class HimCanonicalCatalogBindingV1(
    val path: String,
    val contentSha256: HimSha256,
    val recordCount: Int,
) {
    init {
        require(path.isNotBlank())
        require(recordCount >= 0)
    }

    companion object {
        fun from(master: HimProductOnlyCanonicalMaster) =
            HimCanonicalCatalogBindingV1(
                path = master.path,
                contentSha256 = HimSha256(master.contentSha256),
                recordCount = master.records.size,
            )
    }
}

data class HimCandidateDatasetBindingV1(
    val path: String,
    val digest: HimSha256,
    val schemaVersion: String = HimCandidateDatasetContractV2.SCHEMA_VERSION,
) {
    init {
        require(path.isNotBlank())
        require(schemaVersion == HimCandidateDatasetContractV2.SCHEMA_VERSION)
    }
}

data class HimCanonicalGroundTruthScalingChildLineageV1(
    val canonicalId: HimEntityId,
    val childId: HimEntityId,
    val classification: HimTrainingClassificationV1,
    val projectability: HimCanonicalGroundTruthScalingProjectabilityV1,
    val projectedExample: HimTrainingExampleV1? = null,
    val reason: String? = null,
) {
    init {
        require(classification in setOf(
            HimTrainingClassificationV1.IDENTITY,
            HimTrainingClassificationV1.VARIANT,
            HimTrainingClassificationV1.ALIAS,
        ))
        when (projectability) {
            HimCanonicalGroundTruthScalingProjectabilityV1.PROJECTABLE -> {
                require(projectedExample != null)
                require(reason == null)
            }
            HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_LINEAGE,
            HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_CONTRACT -> {
                require(projectedExample == null)
                require(!reason.isNullOrBlank())
            }
        }
    }
}

data class HimCanonicalGroundTruthScalingClassificationCoverageV1(
    val classification: HimTrainingClassificationV1,
    val state: HimCanonicalGroundTruthScalingCoverageStateV1,
    val projectability: HimCanonicalGroundTruthScalingProjectabilityV1?,
    val authorityEntityCount: Int,
    val projectablePositiveCount: Int,
    val blockedByLineageCount: Int,
    val blockedByContractCount: Int,
    val positiveExampleReferences: List<HimTrainingExampleReference>,
) {
    init {
        require(authorityEntityCount >= 0)
        require(projectablePositiveCount >= 0)
        require(blockedByLineageCount >= 0)
        require(blockedByContractCount >= 0)
        require(positiveExampleReferences.distinct().size == positiveExampleReferences.size)
        require(
            when (state) {
                HimCanonicalGroundTruthScalingCoverageStateV1.PRESENT ->
                    projectability == HimCanonicalGroundTruthScalingProjectabilityV1.PROJECTABLE &&
                        projectablePositiveCount > 0
                HimCanonicalGroundTruthScalingCoverageStateV1.NOT_YET_PROJECTABLE ->
                    projectability != null && projectablePositiveCount == 0
                HimCanonicalGroundTruthScalingCoverageStateV1.MISSING,
                HimCanonicalGroundTruthScalingCoverageStateV1.NOT_APPLICABLE ->
                    projectability == null && projectablePositiveCount == 0
            },
        )
    }
}

data class HimCanonicalTrainingCoverageV1(
    val canonicalId: HimEntityId,
    val canonicalName: String,
    val partition: HimTrainingPartitionV1,
    val classificationCoverage: List<HimCanonicalGroundTruthScalingClassificationCoverageV1>,
    val projectablePositiveExampleReferences: List<HimTrainingExampleReference>,
    val derivedNegativeExampleCount: Int,
    val derivedNegativeBoundaryTypes: List<HimNegativeBoundaryTypeV1>,
    val promotedIdentityCount: Int,
    val promotedVariantCount: Int,
    val promotedAliasCount: Int,
    val workStatus: HimCanonicalGroundTruthScalingWorkStatusV1,
    val workItemReferences: List<String>,
) {
    init {
        require(canonicalName.isNotBlank())
        require(classificationCoverage.map { it.classification }.toSet() == HimTrainingClassificationV1.entries.toSet())
        require(classificationCoverage.map { it.classification } == HimTrainingClassificationV1.entries.toList())
        require(projectablePositiveExampleReferences.distinct().size == projectablePositiveExampleReferences.size)
        require(derivedNegativeExampleCount >= 0)
        require(derivedNegativeBoundaryTypes == derivedNegativeBoundaryTypes.distinct().sortedBy { it.name })
        require(listOf(promotedIdentityCount, promotedVariantCount, promotedAliasCount).all { it >= 0 })
        require(workItemReferences == workItemReferences.distinct().sorted())
    }

    fun coverage(classification: HimTrainingClassificationV1) =
        classificationCoverage.first { it.classification == classification }
}

data class HimTeacherGroundTruthWorkItemV1(
    val reference: String,
    val canonicalId: HimEntityId,
    val partition: HimTrainingPartitionV1,
    val missingCoverage: List<HimTrainingClassificationV1>,
    val reason: HimCanonicalGroundTruthScalingWorkReasonV1,
    val groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1,
) {
    init {
        require(reference.matches(Regex("teacher-work:v1:[0-9a-f]{64}")))
        require(missingCoverage.isNotEmpty())
        require(missingCoverage == missingCoverage.distinct().sortedBy { it.name })
    }
}

data class HimCanonicalGroundTruthScalingDiagnosticsV1(
    val canonicalFamilyCount: Int,
    val familiesWithProjectablePositives: Int,
    val familiesWithNoProjectablePositives: Int,
    val totalProjectablePositiveExamples: Int,
    val totalDerivedNegativeExamples: Int,
    val identityCoverageCount: Int,
    val variantCoverageCount: Int,
    val aliasCoverageCount: Int,
    val existingCanonicalProjectableCount: Int,
    val newCanonicalProjectableCount: Int,
    val teacherWorkFamilyCount: Int,
    val lineageBlockedFamilyCount: Int,
    val contractBlockedFamilyCount: Int,
    val trainFamilyCount: Int,
    val validationFamilyCount: Int,
    val holdoutFamilyCount: Int,
) {
    init {
        require(listOf(
            canonicalFamilyCount,
            familiesWithProjectablePositives,
            familiesWithNoProjectablePositives,
            totalProjectablePositiveExamples,
            totalDerivedNegativeExamples,
            identityCoverageCount,
            variantCoverageCount,
            aliasCoverageCount,
            existingCanonicalProjectableCount,
            newCanonicalProjectableCount,
            teacherWorkFamilyCount,
            lineageBlockedFamilyCount,
            contractBlockedFamilyCount,
            trainFamilyCount,
            validationFamilyCount,
            holdoutFamilyCount,
        ).all { it >= 0 })
        require(familiesWithProjectablePositives + familiesWithNoProjectablePositives == canonicalFamilyCount)
        require(trainFamilyCount + validationFamilyCount + holdoutFamilyCount == canonicalFamilyCount)
    }

    companion object {
        fun from(
            families: List<HimCanonicalTrainingCoverageV1>,
            workItems: List<HimTeacherGroundTruthWorkItemV1>,
        ) = HimCanonicalGroundTruthScalingDiagnosticsV1(
            canonicalFamilyCount = families.size,
            familiesWithProjectablePositives = families.count { it.projectablePositiveExampleReferences.isNotEmpty() },
            familiesWithNoProjectablePositives = families.count { it.projectablePositiveExampleReferences.isEmpty() },
            totalProjectablePositiveExamples = families.sumOf { it.projectablePositiveExampleReferences.size },
            totalDerivedNegativeExamples = families.sumOf { it.derivedNegativeExampleCount },
            identityCoverageCount = families.sumOf { it.coverage(HimTrainingClassificationV1.IDENTITY).projectablePositiveCount },
            variantCoverageCount = families.sumOf { it.coverage(HimTrainingClassificationV1.VARIANT).projectablePositiveCount },
            aliasCoverageCount = families.sumOf { it.coverage(HimTrainingClassificationV1.ALIAS).projectablePositiveCount },
            existingCanonicalProjectableCount = families.sumOf { it.coverage(HimTrainingClassificationV1.EXISTING_CANONICAL).projectablePositiveCount },
            newCanonicalProjectableCount = families.sumOf { it.coverage(HimTrainingClassificationV1.NEW_CANONICAL).projectablePositiveCount },
            teacherWorkFamilyCount = workItems.map { it.canonicalId }.distinct().size,
            lineageBlockedFamilyCount = families.count {
                it.classificationCoverage.any { coverage ->
                    coverage.projectability == HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_LINEAGE
                }
            },
            contractBlockedFamilyCount = families.count {
                it.classificationCoverage.any { coverage ->
                    coverage.projectability == HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_CONTRACT
                }
            },
            trainFamilyCount = families.count { it.partition == HimTrainingPartitionV1.TRAIN },
            validationFamilyCount = families.count { it.partition == HimTrainingPartitionV1.VALIDATION },
            holdoutFamilyCount = families.count { it.partition == HimTrainingPartitionV1.HOLDOUT },
        )
    }
}

data class HimCanonicalGroundTruthScalingPlanV1(
    val contractVersion: String,
    val policyVersion: String,
    val catalogBinding: HimCanonicalCatalogBindingV1,
    val groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1,
    val candidateDatasetBinding: HimCandidateDatasetBindingV1?,
    val families: List<HimCanonicalTrainingCoverageV1>,
    val workItems: List<HimTeacherGroundTruthWorkItemV1>,
    val diagnostics: HimCanonicalGroundTruthScalingDiagnosticsV1,
    val logicalDigest: HimSha256,
) {
    init {
        require(contractVersion == HimCanonicalGroundTruthScalingContractV1.VERSION)
        require(policyVersion == HimCanonicalGroundTruthScalingContractV1.POLICY_VERSION)
    }
}

object HimTeacherGroundTruthWorkItemIdentityV1 {
    fun reference(
        canonicalId: HimEntityId,
        missingCoverage: List<HimTrainingClassificationV1>,
        reason: HimCanonicalGroundTruthScalingWorkReasonV1,
        catalogBinding: HimCanonicalCatalogBindingV1,
        groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1,
        candidateDatasetBinding: HimCandidateDatasetBindingV1?,
    ): String {
        val canonical = buildString {
            appendLine("contract=${HimCanonicalGroundTruthScalingContractV1.WORK_ITEM_REFERENCE_CONTRACT}")
            appendLine("policy=${HimCanonicalGroundTruthScalingContractV1.POLICY_VERSION}")
            appendLine("canonical-id=${canonicalId.value}")
            appendLine("missing=${missingCoverage.sortedBy { it.name }.joinToString(",") { it.name }}")
            appendLine("reason=${reason.name}")
            appendLine("catalog-path=${catalogBinding.path}")
            appendLine("catalog-sha256=${catalogBinding.contentSha256.value}")
            appendLine("catalog-record-count=${catalogBinding.recordCount}")
            appendLine("ground-truth-release=${groundTruthReleaseReference.value}")
            appendLine("candidate-dataset-path=${candidateDatasetBinding?.path.orEmpty()}")
            appendLine("candidate-dataset-digest=${candidateDatasetBinding?.digest?.value.orEmpty()}")
        }
        return "teacher-work:v1:${sha256(canonical)}"
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

object HimCanonicalGroundTruthScalingPlanIdentityV1 {
    fun digest(plan: HimCanonicalGroundTruthScalingPlanV1): HimSha256 =
        HimSha256(sha256(canonical(plan)))

    fun canonical(plan: HimCanonicalGroundTruthScalingPlanV1): String = buildString {
        appendLine("contract=${HimCanonicalGroundTruthScalingContractV1.PLAN_IDENTITY_CONTRACT}")
        appendLine("policy=${plan.policyVersion}")
        appendLine("catalog=${plan.catalogBinding.path}|${plan.catalogBinding.contentSha256.value}|${plan.catalogBinding.recordCount}")
        appendLine("ground-truth-release=${plan.groundTruthReleaseReference.value}")
        appendLine("candidate-dataset=${plan.candidateDatasetBinding?.path.orEmpty()}|${plan.candidateDatasetBinding?.digest?.value.orEmpty()}")
        plan.families.sortedBy { it.canonicalId.value }.forEach { family ->
            appendLine("family=${family.canonicalId.value}|${family.canonicalName}|${family.partition.name}|${family.workStatus.name}")
            family.classificationCoverage.forEach { coverage ->
                appendLine(
                    "coverage=${coverage.classification.name}|${coverage.state.name}|" +
                        "${coverage.projectability?.name.orEmpty()}|${coverage.authorityEntityCount}|" +
                        "${coverage.projectablePositiveCount}|${coverage.blockedByLineageCount}|" +
                        "${coverage.blockedByContractCount}|${coverage.positiveExampleReferences.joinToString(",") { it.value }}",
                )
            }
            appendLine("positives=${family.projectablePositiveExampleReferences.joinToString(",") { it.value }}")
            appendLine("negatives=${family.derivedNegativeExampleCount}|${family.derivedNegativeBoundaryTypes.joinToString(",") { it.name }}")
            appendLine("work-refs=${family.workItemReferences.joinToString(",")}")
        }
        plan.workItems.sortedBy { it.reference }.forEach { workItem ->
            appendLine(
                "work=${workItem.reference}|${workItem.canonicalId.value}|${workItem.partition.name}|" +
                    "${workItem.missingCoverage.joinToString(",") { it.name }}|${workItem.reason.name}|" +
                    workItem.groundTruthReleaseReference.value,
            )
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
