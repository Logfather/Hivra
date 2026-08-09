package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetMerger

class CanonicalThirdSemanticPolicyBatchApplier(
    private val factory:
    CanonicalThirdSemanticPolicyBatchFactory =
        CanonicalThirdSemanticPolicyBatchFactory(),

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

        val vocabularyValidation =
            vocabularyValidator.validate(
                entries = additions
            )

        require(vocabularyValidation.valid) {
            vocabularyValidation.issues.joinToString(
                separator = System.lineSeparator()
            )
        }

        val sourceByIdentity =
            sourcePolicySet.entries
                .associateBy { it.identityKey }

        val existingEntries =
            additions.mapNotNull { addition ->
                sourceByIdentity[addition.identityKey]
            }

        return when {
            existingEntries.isEmpty() ->
                applyPendingBatch(
                    sourcePolicySet = sourcePolicySet,
                    batchResult = batchResult,
                    additions = additions
                )

            existingEntries.size ==
                    additions.size -> {
                require(
                    byIdentity(existingEntries) ==
                            byIdentity(additions)
                ) {
                    "Third semantic policy batch is already present, " +
                            "but persisted policies differ from the canonical " +
                            "batch definition."
                }

                sourcePolicySet
            }

            else ->
                error(
                    "Third semantic policy batch is only partially applied. " +
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
        val selectedBatch =
            requireNotNull(
                batchResult.batches.firstOrNull {
                    it.batchKey ==
                            THIRD_BATCH_KEY
                }
            ) {
                "Expected third semantic policy batch is missing, " +
                        "although its policies have not yet been applied."
            }

        require(
            selectedBatch.category ==
                    EXPECTED_CATEGORY
        )

        require(
            selectedBatch.axis.name ==
                    EXPECTED_AXIS_NAME
        )

        require(
            selectedBatch.gapCount ==
                    EXPECTED_GAP_COUNT
        )

        require(
            additions
                .map { it.identityKey }
                .sorted() ==
                    selectedBatch.gaps
                        .map { it.gapKey }
                        .sorted()
        ) {
            "Curated policies do not exactly cover the selected third batch."
        }

        return merger.merge(
            source = sourcePolicySet,
            additions = additions
        )
    }

    private fun byIdentity(
        entries:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ): Map<String, CanonicalFamilyAxisSemanticPolicyEntry> =
        entries
            .associateBy { it.identityKey }
            .toSortedMap()

    private companion object {
        const val THIRD_BATCH_KEY =
            "semantic-policy-batch-fruit-plant-species-001"

        const val EXPECTED_CATEGORY =
            "fruit"

        const val EXPECTED_AXIS_NAME =
            "PLANT_SPECIES"

        const val EXPECTED_GAP_COUNT =
            8
    }
}