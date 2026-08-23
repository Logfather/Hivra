package de.shopme.tools.knowledge.him.training.scaling

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.training.corpus.HimNegativeTrainingExamplePolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingClassificationV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupReferenceV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolutionV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingFamilyGroupResolverV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleValidatorV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionBuildResultV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionPolicyV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionRecordV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingPartitionerV1
import de.shopme.tools.knowledge.him.training.corpus.HimTrainingTargetV1

data class HimCanonicalGroundTruthScalingPlannerInputV1(
    val catalog: HimProductOnlyCanonicalMaster,
    val authority: HimCanonicalFamilyAuthority,
    val groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1,
    val candidateDatasetBinding: HimCandidateDatasetBindingV1? = null,
    val childLineage: List<HimCanonicalGroundTruthScalingChildLineageV1> = emptyList(),
)

data class HimCanonicalGroundTruthScalingPlanValidationResultV1(
    val valid: Boolean,
    val diagnostics: List<String>,
) {
    init {
        require(valid == diagnostics.isEmpty())
    }
}

object HimCanonicalGroundTruthScalingPlanValidatorV1 {
    fun validate(plan: HimCanonicalGroundTruthScalingPlanV1): HimCanonicalGroundTruthScalingPlanValidationResultV1 {
        val diagnostics = mutableListOf<String>()
        val families = plan.families
        val expectedFamilyIds = families.map { it.canonicalId.value }
        if (expectedFamilyIds != expectedFamilyIds.sorted() || expectedFamilyIds.distinct().size != expectedFamilyIds.size) {
            diagnostics += "Families are not uniquely and deterministically ordered."
        }
        if (families.any { HimTrainingPartitionPolicyV1.partitionForGroup(HimTrainingFamilyGroupReferenceV1.canonical(it.canonicalId)) != it.partition }) {
            diagnostics += "A family is bound to a partition different from F3.8d."
        }
        if (families.any { family ->
                family.classificationCoverage.map { it.classification } != HimTrainingClassificationV1.entries.toList()
            }) {
            diagnostics += "Classification coverage is incomplete or not ordered."
        }
        if (plan.workItems.map { it.reference }.distinct().size != plan.workItems.size) {
            diagnostics += "Duplicate Teacher work-item references exist."
        }
        val familyById = families.associateBy { it.canonicalId }
        plan.workItems.forEach { workItem ->
            val family = familyById[workItem.canonicalId]
            if (family == null) {
                diagnostics += "Teacher work item references an unknown Canonical Family."
            } else {
                if (workItem.partition != family.partition) {
                    diagnostics += "Teacher work item partition does not match its Canonical Family."
                }
                val expectedReference = HimTeacherGroundTruthWorkItemIdentityV1.reference(
                    canonicalId = workItem.canonicalId,
                    missingCoverage = workItem.missingCoverage,
                    reason = workItem.reason,
                    catalogBinding = plan.catalogBinding,
                    groundTruthReleaseReference = plan.groundTruthReleaseReference,
                    candidateDatasetBinding = plan.candidateDatasetBinding,
                )
                if (workItem.reference != expectedReference) {
                    diagnostics += "Teacher work item reference is not deterministic."
                }
                if (workItem.reference !in family.workItemReferences) {
                    diagnostics += "Family does not retain its Teacher work-item reference."
                }
            }
            if (workItem.groundTruthReleaseReference != plan.groundTruthReleaseReference) {
                diagnostics += "Teacher work item is bound to a different Ground-Truth release."
            }
            if (workItem.partition != HimTrainingPartitionPolicyV1.partitionForGroup(
                    HimTrainingFamilyGroupReferenceV1.canonical(workItem.canonicalId),
                )
            ) {
                diagnostics += "Teacher work item has been reassigned away from its deterministic partition."
            }
        }
        val expectedDiagnostics = HimCanonicalGroundTruthScalingDiagnosticsV1.from(families, plan.workItems)
        if (plan.diagnostics != expectedDiagnostics) {
            diagnostics += "Plan diagnostics do not match family and work-item contents."
        }
        if (plan.logicalDigest != HimCanonicalGroundTruthScalingPlanIdentityV1.digest(plan.copy(logicalDigest = plan.logicalDigest))) {
            diagnostics += "Plan logical digest is not deterministic for its bindings and contents."
        }
        return HimCanonicalGroundTruthScalingPlanValidationResultV1(
            valid = diagnostics.isEmpty(),
            diagnostics = diagnostics.distinct(),
        )
    }

    fun validateOrThrow(plan: HimCanonicalGroundTruthScalingPlanV1) {
        val result = validate(plan)
        require(result.valid) { result.diagnostics.joinToString("; ") }
    }
}

class HimCanonicalGroundTruthScalingPlannerV1 {
    fun plan(input: HimCanonicalGroundTruthScalingPlannerInputV1): HimCanonicalGroundTruthScalingPlanV1 {
        val catalogBinding = HimCanonicalCatalogBindingV1.from(input.catalog)
        validateCatalogAuthority(input.catalog, input.authority)
        val authorityChildren = authorityChildren(input.authority)
        validateLineageRecords(input.childLineage, authorityChildren)

        val families = input.authority.families
            .sortedBy { it.canonicalId.value }
            .map { family ->
                buildFamilyCoverage(
                    family = family,
                    authorityChildren = authorityChildren.getValue(family.canonicalId),
                    lineageRecords = input.childLineage,
                    groundTruthReleaseReference = input.groundTruthReleaseReference,
                )
            }

        val workItems = families
            .filter { it.workStatus == HimCanonicalGroundTruthScalingWorkStatusV1.TEACHER_WORK_REQUIRED }
            .map { family ->
                val missing = family.classificationCoverage
                    .filter { it.state == HimCanonicalGroundTruthScalingCoverageStateV1.MISSING }
                    .map { it.classification }
                    .filter { it in TEACHER_WORK_CLASSIFICATIONS }
                    .sortedBy { it.name }
                val reason = HimCanonicalGroundTruthScalingWorkReasonV1.MISSING_SEMANTIC_COVERAGE
                HimTeacherGroundTruthWorkItemV1(
                    reference = HimTeacherGroundTruthWorkItemIdentityV1.reference(
                        canonicalId = family.canonicalId,
                        missingCoverage = missing,
                        reason = reason,
                        catalogBinding = catalogBinding,
                        groundTruthReleaseReference = input.groundTruthReleaseReference,
                        candidateDatasetBinding = input.candidateDatasetBinding,
                    ),
                    canonicalId = family.canonicalId,
                    partition = family.partition,
                    missingCoverage = missing,
                    reason = reason,
                    groundTruthReleaseReference = input.groundTruthReleaseReference,
                )
            }
            .sortedBy { it.reference }

        val workByCanonical = workItems.associateBy { it.canonicalId }
        val completedFamilies = families.map { family ->
            family.copy(workItemReferences = listOfNotNull(workByCanonical[family.canonicalId]?.reference))
        }
        val diagnostics = HimCanonicalGroundTruthScalingDiagnosticsV1.from(completedFamilies, workItems)
        val withoutDigest = HimCanonicalGroundTruthScalingPlanV1(
            contractVersion = HimCanonicalGroundTruthScalingContractV1.VERSION,
            policyVersion = HimCanonicalGroundTruthScalingContractV1.POLICY_VERSION,
            catalogBinding = catalogBinding,
            groundTruthReleaseReference = input.groundTruthReleaseReference,
            candidateDatasetBinding = input.candidateDatasetBinding,
            families = completedFamilies,
            workItems = workItems,
            diagnostics = diagnostics,
            logicalDigest = de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256("0".repeat(64)),
        )
        val plan = withoutDigest.copy(
            logicalDigest = HimCanonicalGroundTruthScalingPlanIdentityV1.digest(withoutDigest),
        )
        HimCanonicalGroundTruthScalingPlanValidatorV1.validateOrThrow(plan)
        return plan
    }

    private fun buildFamilyCoverage(
        family: HimCanonicalFamily,
        authorityChildren: List<AuthorityChildV1>,
        lineageRecords: List<HimCanonicalGroundTruthScalingChildLineageV1>,
        groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1,
    ): HimCanonicalTrainingCoverageV1 {
        val records = authorityChildren.map { child ->
            lineageRecords.firstOrNull { it.childId == child.childId }
                ?: HimCanonicalGroundTruthScalingChildLineageV1(
                    canonicalId = family.canonicalId,
                    childId = child.childId,
                    classification = child.classification,
                    projectability = HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_LINEAGE,
                    reason = "No validated F3.8b lineage was supplied for this active Authority child.",
                )
        }
        records.filter { it.projectability == HimCanonicalGroundTruthScalingProjectabilityV1.PROJECTABLE }
            .forEach { record ->
                validateProjectedExample(
                    record = record,
                    child = authorityChildren.first { it.childId == record.childId },
                    groundTruthReleaseReference = groundTruthReleaseReference,
                )
            }

        val positiveExamples = records.mapNotNull { it.projectedExample }.sortedBy { it.exampleReference.value }
        require(positiveExamples.map { it.exampleReference }.distinct().size == positiveExamples.size) {
            "Multiple Authority children resolve to the same projected training example."
        }
        val negatives = positiveExamples.flatMap { HimNegativeTrainingExamplePolicyV1.derive(it) }
        if (positiveExamples.isNotEmpty()) {
            val partitionResult = HimTrainingPartitionerV1().assign(
                positiveExamples.map { HimTrainingPartitionRecordV1.Positive(it) } +
                    negatives.map { HimTrainingPartitionRecordV1.Negative(it) },
            )
            require(partitionResult is HimTrainingPartitionBuildResultV1.Partitioned) {
                "Projectable family examples could not be partitioned by F3.8d."
            }
            require(partitionResult.manifest.assignments.map { it.partition }.distinct().size == 1) {
                "A Canonical Family received multiple partitions during scaling analysis."
            }
        }

        val classificationCoverage = HimTrainingClassificationV1.entries.map { classification ->
            when (classification) {
                HimTrainingClassificationV1.EXISTING_CANONICAL ->
                    coverage(
                        classification = classification,
                        state = HimCanonicalGroundTruthScalingCoverageStateV1.NOT_YET_PROJECTABLE,
                        projectability = HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_CONTRACT,
                        authorityEntityCount = 1,
                    )
                HimTrainingClassificationV1.NEW_CANONICAL ->
                    coverage(
                        classification = classification,
                        state = HimCanonicalGroundTruthScalingCoverageStateV1.NOT_YET_PROJECTABLE,
                        projectability = HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_CONTRACT,
                        authorityEntityCount = 0,
                    )
                HimTrainingClassificationV1.IDENTITY,
                HimTrainingClassificationV1.VARIANT,
                HimTrainingClassificationV1.ALIAS -> {
                    val matching = records.filter { it.classification == classification }
                    val projected = matching.mapNotNull { it.projectedExample }.sortedBy { it.exampleReference.value }
                    val blockedLineage = matching.count {
                        it.projectability == HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_LINEAGE
                    }
                    val blockedContract = matching.count {
                        it.projectability == HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_CONTRACT
                    }
                    when {
                        projected.isNotEmpty() -> coverage(
                            classification = classification,
                            state = HimCanonicalGroundTruthScalingCoverageStateV1.PRESENT,
                            projectability = HimCanonicalGroundTruthScalingProjectabilityV1.PROJECTABLE,
                            authorityEntityCount = matching.size,
                            projectablePositiveCount = projected.size,
                            positiveExampleReferences = projected.map { it.exampleReference },
                        )
                        blockedLineage > 0 -> coverage(
                            classification = classification,
                            state = HimCanonicalGroundTruthScalingCoverageStateV1.NOT_YET_PROJECTABLE,
                            projectability = HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_LINEAGE,
                            authorityEntityCount = matching.size,
                            blockedByLineageCount = blockedLineage,
                            blockedByContractCount = blockedContract,
                        )
                        blockedContract > 0 -> coverage(
                            classification = classification,
                            state = HimCanonicalGroundTruthScalingCoverageStateV1.NOT_YET_PROJECTABLE,
                            projectability = HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_CONTRACT,
                            authorityEntityCount = matching.size,
                            blockedByLineageCount = blockedLineage,
                            blockedByContractCount = blockedContract,
                        )
                        else -> coverage(
                            classification = classification,
                            state = HimCanonicalGroundTruthScalingCoverageStateV1.MISSING,
                            projectability = null,
                            authorityEntityCount = 0,
                        )
                    }
                }
            }
        }

        val missingTeacherCoverage = classificationCoverage
            .filter {
                it.state == HimCanonicalGroundTruthScalingCoverageStateV1.MISSING &&
                    it.classification in TEACHER_WORK_CLASSIFICATIONS
            }
            .map { it.classification }
        val workStatus = when {
            missingTeacherCoverage.isNotEmpty() -> HimCanonicalGroundTruthScalingWorkStatusV1.TEACHER_WORK_REQUIRED
            classificationCoverage.any {
                it.projectability == HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_LINEAGE
            } -> HimCanonicalGroundTruthScalingWorkStatusV1.BLOCKED_BY_LINEAGE
            classificationCoverage.any {
                it.projectability == HimCanonicalGroundTruthScalingProjectabilityV1.BLOCKED_BY_CONTRACT
            } -> HimCanonicalGroundTruthScalingWorkStatusV1.BLOCKED_BY_CONTRACT
            else -> HimCanonicalGroundTruthScalingWorkStatusV1.NO_TEACHER_WORK_REQUIRED
        }
        return HimCanonicalTrainingCoverageV1(
            canonicalId = family.canonicalId,
            canonicalName = family.canonicalName,
            partition = HimTrainingPartitionPolicyV1.partitionForGroup(
                HimTrainingFamilyGroupReferenceV1.canonical(family.canonicalId),
            ),
            classificationCoverage = classificationCoverage,
            projectablePositiveExampleReferences = positiveExamples.map { it.exampleReference },
            derivedNegativeExampleCount = negatives.size,
            derivedNegativeBoundaryTypes = negatives.map { it.boundaryType }.distinct().sortedBy { it.name },
            promotedIdentityCount = authorityChildren.count { it.classification == HimTrainingClassificationV1.IDENTITY },
            promotedVariantCount = authorityChildren.count { it.classification == HimTrainingClassificationV1.VARIANT },
            promotedAliasCount = authorityChildren.count { it.classification == HimTrainingClassificationV1.ALIAS },
            workStatus = workStatus,
            workItemReferences = emptyList(),
        )
    }

    private fun coverage(
        classification: HimTrainingClassificationV1,
        state: HimCanonicalGroundTruthScalingCoverageStateV1,
        projectability: HimCanonicalGroundTruthScalingProjectabilityV1?,
        authorityEntityCount: Int,
        projectablePositiveCount: Int = 0,
        blockedByLineageCount: Int = 0,
        blockedByContractCount: Int = 0,
        positiveExampleReferences: List<de.shopme.tools.knowledge.him.training.corpus.HimTrainingExampleReference> = emptyList(),
    ) = HimCanonicalGroundTruthScalingClassificationCoverageV1(
        classification = classification,
        state = state,
        projectability = projectability,
        authorityEntityCount = authorityEntityCount,
        projectablePositiveCount = projectablePositiveCount,
        blockedByLineageCount = blockedByLineageCount,
        blockedByContractCount = blockedByContractCount,
        positiveExampleReferences = positiveExampleReferences,
    )

    private fun validateProjectedExample(
        record: HimCanonicalGroundTruthScalingChildLineageV1,
        child: AuthorityChildV1,
        groundTruthReleaseReference: HimGroundTruthReleaseIdentityV1,
    ) {
        val example = requireNotNull(record.projectedExample)
        HimTrainingExampleValidatorV1.validate(example)
        require(example.target.classification == record.classification && record.classification == child.classification) {
            "Projected example classification does not match Authority child lineage."
        }
        require(example.provenance.promotedEntityId == record.childId && child.childId == record.childId) {
            "Projected example promoted Entity ID does not match Authority child lineage."
        }
        require(example.provenance.promotedEntityType == expectedEntityType(record.classification)) {
            "Projected example Entity type does not match Authority child lineage."
        }
        require(example.provenance.groundTruthReleaseReference == groundTruthReleaseReference) {
            "Projected example is bound to a different Ground-Truth release."
        }
        val resolution = HimTrainingFamilyGroupResolverV1.resolve(example)
        require(resolution is HimTrainingFamilyGroupResolutionV1.Resolved) {
            "Projected example has no resolvable Canonical Family."
        }
        require(resolution.groupReference == HimTrainingFamilyGroupReferenceV1.canonical(record.canonicalId)) {
            "Projected example family does not match Authority child lineage."
        }
        when (val target = example.target) {
            is HimTrainingTargetV1.Identity -> require(
                target.parentCanonicalId == child.canonicalId &&
                    child.expectedScope == HimFamilyEntityReference.Canonical(child.canonicalId),
            )
            is HimTrainingTargetV1.Variant -> require(target.scope == child.expectedScope)
            is HimTrainingTargetV1.Alias -> require(target.equivalentEntity == child.expectedScope)
            is HimTrainingTargetV1.ExistingCanonical,
            is HimTrainingTargetV1.NewCanonical -> error("Canonical positives are not Authority child lineage records.")
        }
    }

    private fun validateCatalogAuthority(
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
    ) {
        require(catalog.records.map { it.normalized }.distinct().size == catalog.records.size) {
            "Canonical Catalog contains duplicate normalized Canonical identities."
        }
        require(authority.families.map { it.canonicalId }.distinct().size == authority.families.size) {
            "Ground-Truth Authority contains duplicate Canonical IDs."
        }
        require(authority.sourceCatalog.path == catalog.path) {
            "Ground-Truth Authority is bound to a different Canonical Catalog path."
        }
        require(authority.sourceCatalog.contentSha256 == catalog.contentSha256) {
            "Ground-Truth Authority is bound to a different Canonical Catalog digest."
        }
        require(authority.sourceCatalog.recordCount == catalog.records.size) {
            "Ground-Truth Authority Catalog record count does not match the Catalog."
        }
        val familiesByNormalizedName = authority.families.groupBy { it.normalizedName }
        require(familiesByNormalizedName.values.all { it.size == 1 }) {
            "Ground-Truth Authority contains duplicate normalized Canonical names."
        }
        catalog.records.forEach { record ->
            val family = familiesByNormalizedName[record.normalized]?.singleOrNull()
            require(family != null) {
                "Canonical Catalog identity has no matching Ground-Truth Authority family: ${record.normalized}"
            }
            require(family.canonicalName == record.itemname && family.taxonomyPaths == record.taxonomyPaths) {
                "Canonical Catalog and Ground-Truth Authority identity mismatch: ${record.normalized}"
            }
        }
        require(authority.families.size == catalog.records.size) {
            "Ground-Truth Authority contains families absent from the Canonical Catalog."
        }
    }

    private fun authorityChildren(authority: HimCanonicalFamilyAuthority): Map<HimEntityId, List<AuthorityChildV1>> =
        authority.families.associate { family ->
            val children = buildList {
                family.identities.forEach { identity ->
                    add(AuthorityChildV1(family.canonicalId, identity.identityId, HimTrainingClassificationV1.IDENTITY, HimFamilyEntityReference.Canonical(family.canonicalId)))
                    identity.variants.forEach { add(AuthorityChildV1(family.canonicalId, it.variantId, HimTrainingClassificationV1.VARIANT, HimFamilyEntityReference.Identity(family.canonicalId, identity.identityId))) }
                    identity.aliases.forEach { add(AuthorityChildV1(family.canonicalId, it.aliasId, HimTrainingClassificationV1.ALIAS, HimFamilyEntityReference.Identity(family.canonicalId, identity.identityId))) }
                }
                family.variants.forEach { add(AuthorityChildV1(family.canonicalId, it.variantId, HimTrainingClassificationV1.VARIANT, HimFamilyEntityReference.Canonical(family.canonicalId))) }
                family.aliases.forEach { add(AuthorityChildV1(family.canonicalId, it.aliasId, HimTrainingClassificationV1.ALIAS, HimFamilyEntityReference.Canonical(family.canonicalId))) }
            }
            require(children.map { it.childId }.distinct().size == children.size) {
                "Ground-Truth Authority contains duplicate child Entity IDs in Canonical Family ${family.canonicalId.value}."
            }
            family.canonicalId to children
        }

    private fun validateLineageRecords(
        records: List<HimCanonicalGroundTruthScalingChildLineageV1>,
        childrenByFamily: Map<HimEntityId, List<AuthorityChildV1>>,
    ) {
        require(records.map { it.childId }.distinct().size == records.size) {
            "Scaling input contains duplicate child lineage records."
        }
        val known = childrenByFamily.values.flatten().associateBy { it.childId }
        records.forEach { record ->
            val child = known[record.childId]
            require(child != null) {
                "Scaling input contains lineage for an Authority child that does not exist."
            }
            require(child.canonicalId == record.canonicalId && child.classification == record.classification) {
                "Scaling child lineage does not match the active Authority child."
            }
        }
    }

    private fun expectedEntityType(classification: HimTrainingClassificationV1) = when (classification) {
        HimTrainingClassificationV1.IDENTITY -> HimEntityType.IDENTITY
        HimTrainingClassificationV1.VARIANT -> HimEntityType.VARIANT
        HimTrainingClassificationV1.ALIAS -> HimEntityType.ALIAS
        HimTrainingClassificationV1.EXISTING_CANONICAL,
        HimTrainingClassificationV1.NEW_CANONICAL -> error("Canonical positives are not child lineage records.")
    }

    private data class AuthorityChildV1(
        val canonicalId: HimEntityId,
        val childId: HimEntityId,
        val classification: HimTrainingClassificationV1,
        val expectedScope: HimFamilyEntityReference,
    )

    private companion object {
        val TEACHER_WORK_CLASSIFICATIONS = setOf(
            HimTrainingClassificationV1.IDENTITY,
            HimTrainingClassificationV1.VARIANT,
            HimTrainingClassificationV1.ALIAS,
        )
    }
}
