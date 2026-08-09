package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalVariantValuePolicy

class CanonicalSemanticPolicyBatchVocabularyValidator {

    fun validate(
        entries:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ): CanonicalSemanticPolicyBatchVocabularyValidationResult {
        require(entries.isNotEmpty())

        val issues =
            entries
                .flatMap { entry ->
                    val knownValues =
                        CanonicalVariantValuePolicy
                            .valuesFor(entry.axis)
                            .map {
                                it.key
                            }
                            .toSet()

                    when (entry.policyType) {
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES,

                        CanonicalFamilyAxisSemanticPolicyType
                            .CLOSED_IDENTITY ->
                            entry.allowedValues
                                .filterNot {
                                    it in knownValues
                                }
                                .map { unknownValue ->
                                    "Policy '${entry.identityKey}' references " +
                                            "unknown value '$unknownValue'."
                                }

                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE,

                        CanonicalFamilyAxisSemanticPolicyType
                            .REVIEW_REQUIRED ->
                            emptyList()
                    }
                }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        return CanonicalSemanticPolicyBatchVocabularyValidationResult(
            entryCount =
                entries.size,
            referencedValueCount =
                entries.sumOf {
                    it.allowedValues.size
                },
            issueCount =
                issues.size,
            issues =
                issues,
            valid =
                issues.isEmpty()
        )
    }
}