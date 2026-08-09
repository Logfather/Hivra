package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

object CanonicalFamilyAxisSemanticPolicyTestFactory {

    fun create(
        vararg entries:
        CanonicalFamilyAxisSemanticPolicyEntry
    ): CanonicalFamilyAxisSemanticPolicySet {
        val sortedEntries =
            entries
                .distinctBy {
                    it.identityKey
                }
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
            sortedEntries.count {
                it.complete
            }

        return CanonicalFamilyAxisSemanticPolicySet(
            version =
                CanonicalFamilyAxisSemanticPolicySet
                    .CURRENT_VERSION,

            policySetId =
                "canonical-family-axis-semantic-policy-test",

            sourceBaselineId =
                "canonical-food-catalog-test",

            sourceBaselineCatalogSha256 =
                "0".repeat(64),

            policyCount =
                sortedEntries.size,

            activePolicyCount =
                sortedEntries.count {
                    it.active
                },

            completePolicyCount =
                completeCount,

            incompletePolicyCount =
                sortedEntries.size -
                        completeCount,

            closedIdentityPolicyCount =
                sortedEntries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .CLOSED_IDENTITY
                },

            curatedAllowedValuesPolicyCount =
                sortedEntries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .CURATED_ALLOWED_VALUES
                },

            notApplicablePolicyCount =
                sortedEntries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .NOT_APPLICABLE
                },

            reviewRequiredPolicyCount =
                sortedEntries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .REVIEW_REQUIRED
                },

            coveredFamilyCount =
                sortedEntries
                    .map {
                        it.familyKey
                    }
                    .distinct()
                    .size,

            coveredAxisCount =
                sortedEntries
                    .map {
                        it.axis
                    }
                    .distinct()
                    .size,

            entries =
                sortedEntries,

            valid =
                sortedEntries.isNotEmpty() &&
                        sortedEntries.all {
                            it.complete &&
                                    it.active
                        }
        )
    }

    fun curated(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis,
        allowedValues: List<String>
    ): CanonicalFamilyAxisSemanticPolicyEntry =
        CanonicalFamilyAxisSemanticPolicyEntry(
            familyKey =
                familyKey,

            axis =
                axis,

            policyType =
                CanonicalFamilyAxisSemanticPolicyType
                    .CURATED_ALLOWED_VALUES,

            allowedValues =
                allowedValues
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),

            rationale =
                "Test semantic policy.",

            source =
                "Test fixture",

            active =
                true
        )

    fun closedIdentity(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis,
        allowedValue: String
    ): CanonicalFamilyAxisSemanticPolicyEntry =
        CanonicalFamilyAxisSemanticPolicyEntry(
            familyKey =
                familyKey,

            axis =
                axis,

            policyType =
                CanonicalFamilyAxisSemanticPolicyType
                    .CLOSED_IDENTITY,

            allowedValues =
                listOf(
                    allowedValue.trim()
                ),

            rationale =
                "Test closed identity policy.",

            source =
                "Test fixture",

            active =
                true
        )

    fun notApplicable(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): CanonicalFamilyAxisSemanticPolicyEntry =
        CanonicalFamilyAxisSemanticPolicyEntry(
            familyKey =
                familyKey,

            axis =
                axis,

            policyType =
                CanonicalFamilyAxisSemanticPolicyType
                    .NOT_APPLICABLE,

            allowedValues =
                emptyList(),

            rationale =
                "Test not-applicable policy.",

            source =
                "Test fixture",

            active =
                true
        )
}