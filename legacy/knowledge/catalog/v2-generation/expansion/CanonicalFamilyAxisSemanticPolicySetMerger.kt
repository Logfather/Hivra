package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy

import java.security.MessageDigest

class CanonicalFamilyAxisSemanticPolicySetMerger {

    fun merge(
        source:
        CanonicalFamilyAxisSemanticPolicySet,

        additions:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ): CanonicalFamilyAxisSemanticPolicySet {
        require(source.valid)
        require(additions.isNotEmpty())

        require(
            additions.map {
                it.identityKey
            }.distinct().size ==
                    additions.size
        ) {
            "Added policies must have unique identities."
        }

        val sourceByIdentity =
            source.entries
                .associateBy {
                    it.identityKey
                }
                .toMutableMap()

        additions.forEach { addition ->
            require(
                addition.identityKey !in sourceByIdentity
            ) {
                "Policy '${addition.identityKey}' already exists."
            }

            sourceByIdentity[
                addition.identityKey
            ] =
                addition
        }

        val entries =
            sourceByIdentity
                .values
                .sortedWith(
                    compareBy<
                            CanonicalFamilyAxisSemanticPolicyEntry
                            > {
                        it.familyKey
                    }.thenBy {
                        it.axis.name
                    }
                )

        val completeCount =
            entries.count {
                it.complete
            }

        val policySetId =
            createPolicySetId(
                sourceBaselineId =
                    source.sourceBaselineId,
                entries =
                    entries
            )

        return CanonicalFamilyAxisSemanticPolicySet(
            version =
                CanonicalFamilyAxisSemanticPolicySet
                    .CURRENT_VERSION,

            policySetId =
                policySetId,

            sourceBaselineId =
                source.sourceBaselineId,

            sourceBaselineCatalogSha256 =
                source.sourceBaselineCatalogSha256,

            policyCount =
                entries.size,

            activePolicyCount =
                entries.count {
                    it.active
                },

            completePolicyCount =
                completeCount,

            incompletePolicyCount =
                entries.size -
                        completeCount,

            closedIdentityPolicyCount =
                entries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .CLOSED_IDENTITY
                },

            curatedAllowedValuesPolicyCount =
                entries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .CURATED_ALLOWED_VALUES
                },

            notApplicablePolicyCount =
                entries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .NOT_APPLICABLE
                },

            reviewRequiredPolicyCount =
                entries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .REVIEW_REQUIRED
                },

            coveredFamilyCount =
                entries
                    .map {
                        it.familyKey
                    }
                    .distinct()
                    .size,

            coveredAxisCount =
                entries
                    .map {
                        it.axis
                    }
                    .distinct()
                    .size,

            entries =
                entries,

            valid =
                entries.isNotEmpty() &&
                        entries.all {
                            it.active &&
                                    it.complete
                        }
        )
    }

    private fun createPolicySetId(
        sourceBaselineId: String,
        entries:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ): String {
        val canonicalPayload =
            buildString {
                append(sourceBaselineId)
                append('\n')

                entries.forEach { entry ->
                    append(entry.familyKey)
                    append('|')
                    append(entry.axis.name)
                    append('|')
                    append(entry.policyType.name)
                    append('|')
                    append(
                        entry.allowedValues
                            .joinToString(",")
                    )
                    append('\n')
                }
            }

        val hash =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    canonicalPayload
                        .toByteArray(Charsets.UTF_8)
                )
                .joinToString("") {
                    "%02x".format(it)
                }
                .take(16)

        return "canonical-family-axis-semantic-policy-v1-$hash"
    }
}