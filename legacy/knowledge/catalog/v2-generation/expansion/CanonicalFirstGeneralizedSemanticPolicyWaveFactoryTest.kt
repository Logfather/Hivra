package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFirstGeneralizedSemanticPolicyWaveFactoryTest {

    @Test
    fun createCompleteFirstGeneralizedWave() {
        val batches =
            CanonicalFirstGeneralizedSemanticPolicyWaveFactory()
                .createBatches()

        assertEquals(
            3,
            batches.size
        )

        assertEquals(
            33,
            batches.sumOf {
                it.policyCount
            }
        )

        assertEquals(
            listOf(
                "semantic-policy-batch-bakery-grain-type-003",
                "semantic-policy-batch-dairy-food-type-001",
                "semantic-policy-batch-ready-meals-" +
                        "allergen-relevant-variant-007"
            ),
            batches.map {
                it.sourceImplementationBatchKey
            }
        )

        assertTrue(
            batches.all {
                it.valid &&
                        it.complete
            }
        )

        val policies =
            batches.flatMap {
                it.policies
            }

        assertEquals(
            33,
            policies
                .map {
                    it.identityKey
                }
                .distinct()
                .size
        )

        assertEquals(
            29,
            policies.count {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES
            }
        )

        assertEquals(
            4,
            policies.count {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }
        )
    }

    @Test
    fun referenceOnlyCanonicalVariantValues() {
        val policies =
            CanonicalFirstGeneralizedSemanticPolicyWaveFactory()
                .createBatches()
                .flatMap {
                    it.policies
                }

        val result =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(
                    entries =
                        policies
                )

        assertTrue(
            result.valid,
            result.issues.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        assertEquals(
            0,
            result.issueCount
        )
    }

    @Test
    fun createDeterministically() {
        val factory =
            CanonicalFirstGeneralizedSemanticPolicyWaveFactory()

        assertEquals(
            factory.createBatches(),
            factory.createBatches()
        )
    }
}