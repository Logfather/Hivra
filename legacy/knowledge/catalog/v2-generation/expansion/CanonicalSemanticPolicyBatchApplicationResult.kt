package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.application

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet

data class CanonicalSemanticPolicyBatchApplicationResult(
    val version: Int,

    val manifestId: String,

    val sourcePolicySetId: String,
    val resultingPolicySetId: String,

    val manifestBatchCount: Int,
    val manifestPolicyCount: Int,

    val newlyAppliedBatchCount: Int,
    val alreadyAppliedBatchCount: Int,

    val newlyAppliedPolicyCount: Int,
    val alreadyAppliedPolicyCount: Int,

    val resultingPolicySet:
    CanonicalFamilyAxisSemanticPolicySet,

    val idempotent: Boolean,

    val blockers: List<String>,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(manifestId.isNotBlank())

        require(sourcePolicySetId.isNotBlank())
        require(resultingPolicySetId.isNotBlank())

        require(manifestBatchCount > 0)
        require(manifestPolicyCount > 0)

        require(
            manifestBatchCount ==
                    newlyAppliedBatchCount +
                    alreadyAppliedBatchCount
        )

        require(
            manifestPolicyCount ==
                    newlyAppliedPolicyCount +
                    alreadyAppliedPolicyCount
        )

        require(
            resultingPolicySetId ==
                    resultingPolicySet.policySetId
        )

        require(
            blockers ==
                    blockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            idempotent ==
                    (
                            newlyAppliedPolicyCount == 0 &&
                                    alreadyAppliedPolicyCount ==
                                    manifestPolicyCount
                            )
        )

        require(
            valid ==
                    (
                            blockers.isEmpty() &&
                                    resultingPolicySet.valid
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}