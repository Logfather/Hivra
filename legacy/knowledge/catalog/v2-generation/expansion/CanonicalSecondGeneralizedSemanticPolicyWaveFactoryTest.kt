package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalSecondGeneralizedSemanticPolicyWaveFactoryTest {

    @Test
    fun createCompleteSecondGeneralizedWave() {
        val batches =
            CanonicalSecondGeneralizedSemanticPolicyWaveFactory()
                .createBatches()

        assertEquals(
            EXPECTED_BATCH_COUNT,
            batches.size
        )

        assertEquals(
            EXPECTED_POLICY_COUNT,
            batches.sumOf {
                it.policyCount
            }
        )

        assertEquals(
            EXPECTED_BATCH_KEYS,
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
            EXPECTED_POLICY_COUNT,
            policies
                .map {
                    it.identityKey
                }
                .distinct()
                .size
        )

        assertEquals(
            EXPECTED_CURATED_POLICY_COUNT,
            policies.count {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES
            }
        )

        assertEquals(
            EXPECTED_NOT_APPLICABLE_POLICY_COUNT,
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
            CanonicalSecondGeneralizedSemanticPolicyWaveFactory()
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
    fun markBakeryPhysicalFormAsNotApplicable() {
        val batch =
            CanonicalSecondGeneralizedSemanticPolicyWaveFactory()
                .createBatches()
                .single {
                    it.sourceImplementationBatchKey ==
                            BAKERY_PHYSICAL_FORM_BATCH_KEY
                }

        assertEquals(
            10,
            batch.policyCount
        )

        assertTrue(
            batch.policies.all {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }
        )
    }

    @Test
    fun curateBakeryProcessingMethodsExplicitly() {
        val batch =
            CanonicalSecondGeneralizedSemanticPolicyWaveFactory()
                .createBatches()
                .single {
                    it.sourceImplementationBatchKey ==
                            BAKERY_PROCESSING_METHOD_BATCH_KEY
                }

        assertEquals(
            10,
            batch.policyCount
        )

        assertTrue(
            batch.policies.all {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES &&
                        it.allowedValues.isNotEmpty()
            }
        )

        val breadPolicy =
            requireNotNull(
                batch.policies.singleOrNull {
                    it.familyKey ==
                            "bread"
                }
            )

        assertEquals(
            listOf(
                "baked",
                "fermented"
            ),
            breadPolicy.allowedValues
        )
    }

    @Test
    fun createDeterministically() {
        val factory =
            CanonicalSecondGeneralizedSemanticPolicyWaveFactory()

        assertEquals(
            factory.createBatches(),
            factory.createBatches()
        )
    }

    private companion object {
        const val EXPECTED_BATCH_COUNT =
            3

        const val EXPECTED_POLICY_COUNT =
            32

        const val EXPECTED_CURATED_POLICY_COUNT =
            10

        const val EXPECTED_NOT_APPLICABLE_POLICY_COUNT =
            22

        const val BAKERY_PHYSICAL_FORM_BATCH_KEY =
            "semantic-policy-batch-bakery-physical-form-002"

        const val BAKERY_PROCESSING_METHOD_BATCH_KEY =
            "semantic-policy-batch-bakery-processing-method-008"

        val EXPECTED_BATCH_KEYS =
            listOf(
                "semantic-policy-batch-bakery-physical-form-002",
                "semantic-policy-batch-bakery-processing-method-008",
                "semantic-policy-batch-ready-meals-food-type-009"
            )
    }
}