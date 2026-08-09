package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.application

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifest
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetMerger

class CanonicalSemanticPolicyBatchManifestApplier(
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

        openImplementationBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        curatedManifest:
        CanonicalCuratedSemanticPolicyBatchManifest
    ): CanonicalSemanticPolicyBatchApplicationResult {
        require(sourcePolicySet.valid)
        require(openImplementationBatches.valid)
        require(curatedManifest.valid)

        val sourceByIdentity =
            sourcePolicySet.entries
                .associateBy {
                    it.identityKey
                }

        val blockers =
            mutableListOf<String>()

        val additions =
            mutableListOf<
                    CanonicalFamilyAxisSemanticPolicyEntry
                    >()

        var newlyAppliedBatchCount =
            0

        var alreadyAppliedBatchCount =
            0

        var newlyAppliedPolicyCount =
            0

        var alreadyAppliedPolicyCount =
            0

        curatedManifest.batches.forEach { curatedBatch ->
            val batchState =
                evaluateBatchState(
                    curatedBatch =
                        curatedBatch,

                    sourceByIdentity =
                        sourceByIdentity
                )

            when (batchState) {
                CuratedBatchState.NOT_APPLIED -> {
                    validateOpenBatchCoverage(
                        curatedBatch =
                            curatedBatch,

                        openImplementationBatches =
                            openImplementationBatches,

                        blockers =
                            blockers
                    )

                    additions +=
                        curatedBatch.policies

                    newlyAppliedBatchCount +=
                        1

                    newlyAppliedPolicyCount +=
                        curatedBatch.policyCount
                }

                CuratedBatchState.ALREADY_APPLIED -> {
                    alreadyAppliedBatchCount +=
                        1

                    alreadyAppliedPolicyCount +=
                        curatedBatch.policyCount
                }

                CuratedBatchState.PARTIALLY_APPLIED ->
                    blockers +=
                        "Curated batch '${curatedBatch.sourceImplementationBatchKey}' " +
                                "is only partially applied."

                CuratedBatchState.DIVERGENT ->
                    blockers +=
                        "Persisted policies for curated batch " +
                                "'${curatedBatch.sourceImplementationBatchKey}' " +
                                "differ from the curated definition."
            }
        }

        if (additions.isNotEmpty()) {
            val vocabularyValidation =
                vocabularyValidator.validate(
                    entries =
                        additions
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
                )

            blockers +=
                vocabularyValidation.issues
        }

        val sortedBlockers =
            blockers
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val resultingPolicySet =
            if (
                sortedBlockers.isEmpty() &&
                additions.isNotEmpty()
            ) {
                merger.merge(
                    source =
                        sourcePolicySet,

                    additions =
                        additions
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
                )
            } else {
                sourcePolicySet
            }

        return CanonicalSemanticPolicyBatchApplicationResult(
            version =
                CanonicalSemanticPolicyBatchApplicationResult
                    .CURRENT_VERSION,

            manifestId =
                curatedManifest.manifestId,

            sourcePolicySetId =
                sourcePolicySet.policySetId,

            resultingPolicySetId =
                resultingPolicySet.policySetId,

            manifestBatchCount =
                curatedManifest.batchCount,

            manifestPolicyCount =
                curatedManifest.policyCount,

            newlyAppliedBatchCount =
                newlyAppliedBatchCount,

            alreadyAppliedBatchCount =
                alreadyAppliedBatchCount,

            newlyAppliedPolicyCount =
                newlyAppliedPolicyCount,

            alreadyAppliedPolicyCount =
                alreadyAppliedPolicyCount,

            resultingPolicySet =
                resultingPolicySet,

            idempotent =
                newlyAppliedPolicyCount == 0 &&
                        alreadyAppliedPolicyCount ==
                        curatedManifest.policyCount,

            blockers =
                sortedBlockers,

            valid =
                sortedBlockers.isEmpty() &&
                        resultingPolicySet.valid
        )
    }

    private fun evaluateBatchState(
        curatedBatch:
        CanonicalCuratedSemanticPolicyBatch,

        sourceByIdentity:
        Map<String, CanonicalFamilyAxisSemanticPolicyEntry>
    ): CuratedBatchState {
        val existingEntries =
            curatedBatch.policies
                .mapNotNull { expected ->
                    sourceByIdentity[
                        expected.identityKey
                    ]
                }

        return when {
            existingEntries.isEmpty() ->
                CuratedBatchState.NOT_APPLIED

            existingEntries.size <
                    curatedBatch.policyCount ->
                CuratedBatchState.PARTIALLY_APPLIED

            existingEntries
                .associateBy {
                    it.identityKey
                } ==
                    curatedBatch.policies
                        .associateBy {
                            it.identityKey
                        } ->
                CuratedBatchState.ALREADY_APPLIED

            else ->
                CuratedBatchState.DIVERGENT
        }
    }

    private fun resolveOpenBatch(
        curatedBatch:
        CanonicalCuratedSemanticPolicyBatch,

        openImplementationBatches:
        CanonicalSemanticPolicyImplementationBatchResult
    ): CanonicalSemanticPolicyImplementationBatch? {
        val directMatch =
            openImplementationBatches.batches
                .firstOrNull { batch ->
                    batch.batchKey ==
                            curatedBatch.sourceImplementationBatchKey
                }

        if (directMatch != null) {
            return directMatch
        }

        val curatedPolicyKeys =
            curatedBatch.policies
                .map { policy ->
                    policy.identityKey
                }
                .distinct()
                .sorted()

        val semanticMatches =
            openImplementationBatches.batches
                .filter { batch ->
                    val openGapKeys =
                        batch.gaps
                            .map { gap ->
                                gap.gapKey
                            }
                            .distinct()
                            .sorted()

                    batch.category ==
                            curatedBatch.category &&
                            batch.axis.name ==
                            curatedBatch.axis &&
                            openGapKeys ==
                            curatedPolicyKeys
                }

        require(
            semanticMatches.size <= 1
        ) {
            "Multiple open implementation batches semantically match " +
                    "curated batch " +
                    "'${curatedBatch.sourceImplementationBatchKey}': " +
                    semanticMatches
                        .map { batch ->
                            batch.batchKey
                        }
                        .sorted()
        }

        return semanticMatches.singleOrNull()
    }

    private fun validateOpenBatchCoverage(
        curatedBatch:
        CanonicalCuratedSemanticPolicyBatch,

        openImplementationBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        blockers:
        MutableList<String>
    ) {
        val openBatch =
            resolveOpenBatch(
                curatedBatch =
                    curatedBatch,

                openImplementationBatches =
                    openImplementationBatches
            )

        if (openBatch == null) {
            blockers +=
                "No open implementation batch semantically matches " +
                        "unapplied curated batch " +
                        "'${curatedBatch.sourceImplementationBatchKey}'."

            return
        }

        if (
            openBatch.category !=
            curatedBatch.category
        ) {
            blockers +=
                "Category mismatch for curated batch " +
                        "'${curatedBatch.sourceImplementationBatchKey}': " +
                        "expected='${curatedBatch.category}', " +
                        "actual='${openBatch.category}'."
        }

        if (
            openBatch.axis.name !=
            curatedBatch.axis
        ) {
            blockers +=
                "Axis mismatch for curated batch " +
                        "'${curatedBatch.sourceImplementationBatchKey}': " +
                        "expected='${curatedBatch.axis}', " +
                        "actual='${openBatch.axis.name}'."
        }

        val curatedPolicyKeys =
            curatedBatch.policies
                .map { policy ->
                    policy.identityKey
                }
                .distinct()
                .sorted()

        val openGapKeys =
            openBatch.gaps
                .map { gap ->
                    gap.gapKey
                }
                .distinct()
                .sorted()

        if (
            curatedPolicyKeys !=
            openGapKeys
        ) {
            blockers +=
                "Curated policies do not exactly cover resolved " +
                        "implementation batch '${openBatch.batchKey}' " +
                        "for historical batch " +
                        "'${curatedBatch.sourceImplementationBatchKey}'."
        }
    }

    private enum class CuratedBatchState {
        NOT_APPLIED,
        ALREADY_APPLIED,
        PARTIALLY_APPLIED,
        DIVERGENT
    }
}