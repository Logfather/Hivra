package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetMerger

class CanonicalFirstSemanticPolicyBatchApplier(
    private val factory:
    CanonicalFirstSemanticPolicyBatchFactory =
        CanonicalFirstSemanticPolicyBatchFactory(),

    private val vocabularyValidator:
    CanonicalSemanticPolicyBatchVocabularyValidator =
        CanonicalSemanticPolicyBatchVocabularyValidator(),

    private val merger:
    CanonicalFamilyAxisSemanticPolicySetMerger =
        CanonicalFamilyAxisSemanticPolicySetMerger()
) {

    fun apply(
        sourcePolicySet:
        CanonicalFamilyAxisSemanticPolicySet,

        batchResult:
        CanonicalSemanticPolicyImplementationBatchResult
    ): CanonicalFamilyAxisSemanticPolicySet {
        require(sourcePolicySet.valid)
        require(batchResult.valid)

        val additions =
            factory.createEntries()

        validateVocabulary(
            additions = additions
        )

        val sourceEntriesByIdentity =
            sourcePolicySet.entries
                .associateBy {
                    it.identityKey
                }

        val existingEntries =
            additions.mapNotNull { addition ->
                sourceEntriesByIdentity[
                    addition.identityKey
                ]
            }

        return when {
            existingEntries.isEmpty() ->
                applyPendingBatch(
                    sourcePolicySet =
                        sourcePolicySet,

                    batchResult =
                        batchResult,

                    additions =
                        additions
                )

            existingEntries.size ==
                    additions.size -> {
                require(
                    existingEntriesByIdentity(
                        entries = existingEntries
                    ) ==
                            existingEntriesByIdentity(
                                entries = additions
                            )
                ) {
                    "First semantic policy batch is already present, " +
                            "but one or more persisted policies differ from " +
                            "the canonical batch definition."
                }

                /*
                 * Der Batch wurde bereits vollständig und identisch
                 * angewendet. Der erneute Aufruf ist ein No-op.
                 */
                sourcePolicySet
            }

            else ->
                error(
                    "First semantic policy batch is only partially applied. " +
                            "existing=${existingEntries.size}, " +
                            "expected=${additions.size}."
                )
        }
    }

    private fun applyPendingBatch(
        sourcePolicySet:
        CanonicalFamilyAxisSemanticPolicySet,

        batchResult:
        CanonicalSemanticPolicyImplementationBatchResult,

        additions:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ): CanonicalFamilyAxisSemanticPolicySet {
        val firstBatch =
            requireNotNull(
                batchResult.batches.firstOrNull {
                    it.batchKey ==
                            FIRST_BATCH_KEY
                }
            ) {
                "Expected first semantic policy batch is missing, " +
                        "although its policies have not yet been applied."
            }

        require(
            firstBatch.category ==
                    EXPECTED_CATEGORY
        ) {
            "Unexpected first-batch category: " +
                    firstBatch.category
        }

        require(
            firstBatch.axis.name ==
                    EXPECTED_AXIS_NAME
        ) {
            "Unexpected first-batch axis: " +
                    firstBatch.axis.name
        }

        require(
            firstBatch.gapCount ==
                    EXPECTED_GAP_COUNT
        ) {
            "Unexpected first-batch gap count: " +
                    firstBatch.gapCount
        }

        require(
            additions
                .map {
                    it.identityKey
                }
                .sorted() ==
                    firstBatch.gaps
                        .map {
                            it.gapKey
                        }
                        .sorted()
        ) {
            "Curated policy entries do not exactly cover the first batch."
        }

        return merger.merge(
            source =
                sourcePolicySet,

            additions =
                additions
        )
    }

    private fun validateVocabulary(
        additions:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ) {
        val result =
            vocabularyValidator.validate(
                entries =
                    additions
            )

        require(result.valid) {
            result.issues.joinToString(
                separator =
                    System.lineSeparator()
            )
        }
    }

    private fun existingEntriesByIdentity(
        entries:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ): Map<String, CanonicalFamilyAxisSemanticPolicyEntry> =
        entries
            .associateBy {
                it.identityKey
            }
            .toSortedMap()

    private companion object {
        const val FIRST_BATCH_KEY =
            "semantic-policy-batch-vegetables-plant-species-001"

        const val EXPECTED_CATEGORY =
            "vegetables"

        const val EXPECTED_AXIS_NAME =
            "PLANT_SPECIES"

        const val EXPECTED_GAP_COUNT =
            12
    }
}