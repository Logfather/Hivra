package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy

data class CanonicalFamilyAxisSemanticPolicySet(
    val version: Int,

    val policySetId: String,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val policyCount: Int,
    val activePolicyCount: Int,
    val completePolicyCount: Int,
    val incompletePolicyCount: Int,

    val closedIdentityPolicyCount: Int,
    val curatedAllowedValuesPolicyCount: Int,
    val reviewRequiredPolicyCount: Int,
    val notApplicablePolicyCount: Int,

    val coveredFamilyCount: Int,
    val coveredAxisCount: Int,

    val entries:
    List<CanonicalFamilyAxisSemanticPolicyEntry>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(POLICY_SET_ID_REGEX.matches(policySetId)) {
            "Invalid policy set id '$policySetId'."
        }

        require(sourceBaselineId.isNotBlank())

        require(
            SHA_256_REGEX.matches(
                sourceBaselineCatalogSha256
            )
        )

        require(policyCount == entries.size)

        require(
            entries ==
                    entries.sortedWith(
                        compareBy<
                                CanonicalFamilyAxisSemanticPolicyEntry
                                > {
                            it.familyKey
                        }.thenBy {
                            it.axis.name
                        }
                    )
        ) {
            "Semantic policy entries must be deterministically sorted."
        }

        require(
            entries.map {
                it.identityKey
            }.distinct().size ==
                    entries.size
        ) {
            "Semantic policies must have unique family-axis identities."
        }

        require(
            activePolicyCount ==
                    entries.count {
                        it.active
                    }
        )

        require(
            completePolicyCount ==
                    entries.count {
                        it.complete
                    }
        )

        require(
            incompletePolicyCount ==
                    entries.count {
                        !it.complete
                    }
        )

        require(
            policyCount ==
                    completePolicyCount +
                    incompletePolicyCount
        )

        require(
            closedIdentityPolicyCount ==
                    entries.count {
                        it.policyType ==
                                CanonicalFamilyAxisSemanticPolicyType
                                    .CLOSED_IDENTITY
                    }
        )

        require(
            curatedAllowedValuesPolicyCount ==
                    entries.count {
                        it.policyType ==
                                CanonicalFamilyAxisSemanticPolicyType
                                    .CURATED_ALLOWED_VALUES
                    }
        )

        require(
            notApplicablePolicyCount ==
                    entries.count {
                        it.policyType ==
                                CanonicalFamilyAxisSemanticPolicyType
                                    .NOT_APPLICABLE
                    }
        )

        require(
            reviewRequiredPolicyCount ==
                    entries.count {
                        it.policyType ==
                                CanonicalFamilyAxisSemanticPolicyType
                                    .REVIEW_REQUIRED
                    }
        )

        require(
            policyCount ==
                    closedIdentityPolicyCount +
                    curatedAllowedValuesPolicyCount +
                    notApplicablePolicyCount +
                    reviewRequiredPolicyCount
        )

        require(
            coveredFamilyCount ==
                    entries
                        .map {
                            it.familyKey
                        }
                        .distinct()
                        .size
        )

        require(
            coveredAxisCount ==
                    entries
                        .map {
                            it.axis
                        }
                        .distinct()
                        .size
        )

        require(
            valid ==
                    (
                            entries.isNotEmpty() &&
                                    incompletePolicyCount == 0 &&
                                    entries.all {
                                        it.active
                                    }
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val POLICY_SET_ID_REGEX =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}