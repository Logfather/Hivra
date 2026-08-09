package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry

data class CanonicalCuratedSemanticPolicyBatch(
    val version: Int,

    val curationId: String,

    val sourceImplementationBatchKey: String,

    val category: String,

    val axis: String,

    val policyCount: Int,

    val policies:
    List<CanonicalFamilyAxisSemanticPolicyEntry>,

    val complete: Boolean,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(curationId.isNotBlank())
        require(sourceImplementationBatchKey.isNotBlank())
        require(category.isNotBlank())
        require(axis.isNotBlank())

        require(policyCount == policies.size)
        require(policyCount > 0)

        require(
            policies ==
                    policies.sortedWith(
                        compareBy<
                                CanonicalFamilyAxisSemanticPolicyEntry
                                > {
                            it.familyKey
                        }.thenBy {
                            it.axis.name
                        }
                    )
        ) {
            "Curated semantic policies must be deterministically sorted."
        }

        require(
            policies
                .map {
                    it.identityKey
                }
                .distinct()
                .size ==
                    policies.size
        ) {
            "Curated semantic policies must have unique identities."
        }

        require(
            policies.all {
                it.axis.name == axis
            }
        ) {
            "Every curated policy must use axis '$axis'."
        }

        require(
            complete ==
                    policies.all {
                        it.active &&
                                it.complete
                    }
        )

        require(
            valid ==
                    (
                            complete &&
                                    policies.isNotEmpty()
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}