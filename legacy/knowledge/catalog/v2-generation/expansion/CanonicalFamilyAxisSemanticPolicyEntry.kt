package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

data class CanonicalFamilyAxisSemanticPolicyEntry(
    val familyKey: String,

    val axis:
    CanonicalProductFamilyVariantAxis,

    val policyType:
    CanonicalFamilyAxisSemanticPolicyType,

    val allowedValues:
    List<String>,

    val rationale: String,

    val source: String,

    val active: Boolean
) {

    init {
        require(CANONICAL_KEY_REGEX.matches(familyKey)) {
            "Invalid family key '$familyKey'."
        }

        require(
            allowedValues ==
                    allowedValues
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "Allowed values must be normalized, unique and sorted for " +
                    "'$familyKey × $axis'."
        }

        when (policyType) {
            CanonicalFamilyAxisSemanticPolicyType
                .CURATED_ALLOWED_VALUES -> {
            require(allowedValues.isNotEmpty()) {
                "Curated policy must define at least one allowed value."
            }
        }

            CanonicalFamilyAxisSemanticPolicyType
                .CLOSED_IDENTITY -> {
            require(allowedValues.size == 1) {
                "Closed identity policy must define exactly one value."
            }
        }

            CanonicalFamilyAxisSemanticPolicyType
                .NOT_APPLICABLE -> {
            require(allowedValues.isEmpty()) {
                "Not-applicable policy must not define allowed values."
            }
        }

            CanonicalFamilyAxisSemanticPolicyType
                .REVIEW_REQUIRED -> {
            require(allowedValues.isEmpty()) {
                "Review-required policy must not define allowed values."
            }
        }
        }

        require(rationale.isNotBlank())
        require(rationale == rationale.trim())

        require(source.isNotBlank())
        require(source == source.trim())
    }

    val identityKey: String
        get() =
            "$familyKey::${axis.name}"

    val complete: Boolean
        get() =
            active &&
                    when (policyType) {
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES ->
                            allowedValues.isNotEmpty()

                        CanonicalFamilyAxisSemanticPolicyType
                            .CLOSED_IDENTITY ->
                            allowedValues.size == 1

                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE ->
                            allowedValues.isEmpty()

                        CanonicalFamilyAxisSemanticPolicyType
                            .REVIEW_REQUIRED ->
                            false
                    }

    private companion object {
        val CANONICAL_KEY_REGEX =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
    }
}