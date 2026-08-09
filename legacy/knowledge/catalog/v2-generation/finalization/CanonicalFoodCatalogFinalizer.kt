package de.shopme.testing.system.tools.knowledge.catalog.finalization

import de.shopme.testing.system.tools.knowledge.catalog.category
.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalApprovedCatalogExpansionResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalExpandedCatalogItemOrder
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact
.CanonicalSemanticPolicyBatchImpactAnalysis
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization
.CanonicalFoodKeyNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.validation
.NormalizedCatalogValidator
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class CanonicalFoodCatalogFinalizer(
    private val categoryRegistry:
    CanonicalFoodCategoryRegistry =
        CanonicalFoodCategoryRegistry(),

    private val keyNormalizer:
    CanonicalFoodKeyNormalizer =
        CanonicalFoodKeyNormalizer()
) {

    fun finalize(
        approvedExpansion:
        CanonicalApprovedCatalogExpansionResult,

        terminalPolicyBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        waveSixImpact:
        CanonicalSemanticPolicyBatchImpactAnalysis,

        outputCatalogFile: File
    ): CanonicalFoodCatalogFinalizationResult {
        require(approvedExpansion.valid) {
            "Approved catalog expansion must be valid before finalization."
        }

        require(terminalPolicyBatches.valid) {
            "Terminal semantic-policy batch result must be valid."
        }

        require(waveSixImpact.valid) {
            "Frozen Wave-6 impact must be valid."
        }

        val finalItems =
            approvedExpansion.expandedCatalogItems

        require(finalItems.isNotEmpty()) {
            "Approved expansion contains no final catalog items."
        }

        val blockers =
            mutableListOf<String>()

        val terminalPolicyClosureReached =
            terminalPolicyBatches.missingPolicyGapCount == 0 &&
                    terminalPolicyBatches.batchCount == 0 &&
                    terminalPolicyBatches.gaps.isEmpty() &&
                    terminalPolicyBatches.batches.isEmpty() &&
                    terminalPolicyBatches.completeGapCoverage &&
                    terminalPolicyBatches.deterministicOrderValid &&
                    terminalPolicyBatches.blockers.isEmpty()

        if (!terminalPolicyClosureReached) {
            blockers +=
                "Semantic-policy closure has not reached the valid " +
                        "terminal state."
        }

        if (waveSixImpact.missingPolicyGapsAfter != 0) {
            blockers +=
                "Wave-6 impact still reports " +
                        "${waveSixImpact.missingPolicyGapsAfter} open " +
                        "semantic-policy gaps."
        }

        val normalizedCatalogValidation =
            NormalizedCatalogValidator(
                keyNormalizer =
                    keyNormalizer
            )
                .validate(
                    items =
                        finalItems,

                    categoryRegistry =
                        categoryRegistry
                )

        if (!normalizedCatalogValidation.valid) {
            normalizedCatalogValidation.issues
                .map { issue ->
                    issue.toString()
                }
                .forEach(blockers::add)
        }

        val normalizedNames =
            finalItems
                .map { item ->
                    normalizeName(
                        item.itemname
                    )
                }

        val normalizedKeys =
            finalItems
                .map { item ->
                    requireNotNull(
                        item.normalized
                    ) {
                        "Final catalog item '${item.itemname}' has no " +
                                "normalized key."
                    }
                }

        val uniqueCanonicalNameCount =
            normalizedNames
                .distinct()
                .size

        val uniqueNormalizedKeyCount =
            normalizedKeys
                .distinct()
                .size

        val uniqueCanonicalNames =
            uniqueCanonicalNameCount ==
                    finalItems.size

        val uniqueNormalizedKeys =
            uniqueNormalizedKeyCount ==
                    finalItems.size

        if (!uniqueCanonicalNames) {
            normalizedNames
                .groupingBy(String::toString)
                .eachCount()
                .filterValues { count ->
                    count > 1
                }
                .keys
                .sorted()
                .forEach { duplicateName ->
                    blockers +=
                        "Final catalog contains duplicate canonical name " +
                                "'$duplicateName'."
                }
        }

        if (!uniqueNormalizedKeys) {
            normalizedKeys
                .groupingBy(String::toString)
                .eachCount()
                .filterValues { count ->
                    count > 1
                }
                .keys
                .sorted()
                .forEach { duplicateKey ->
                    blockers +=
                        "Final catalog contains duplicate normalized key " +
                                "'$duplicateKey'."
                }
        }

        val deterministicOrderValid =
            CanonicalExpandedCatalogItemOrder
                .isSorted(
                    finalItems
                )

        if (!deterministicOrderValid) {
            blockers +=
                "Final canonical food catalog order is not deterministic."
        }

        val exactCatalogArithmeticValid =
            finalItems.size ==
                    approvedExpansion.baselineEntryCount +
                    approvedExpansion.materializedEntryCount &&
                    finalItems.size ==
                    approvedExpansion.expandedCatalogEntryCount

        if (!exactCatalogArithmeticValid) {
            blockers +=
                "Final catalog arithmetic is inconsistent: " +
                        "baseline=${approvedExpansion.baselineEntryCount}, " +
                        "materialized=${approvedExpansion.materializedEntryCount}, " +
                        "expanded=${approvedExpansion.expandedCatalogEntryCount}, " +
                        "actual=${finalItems.size}."
        }

        val prePersistenceBlockers =
            normalizedBlockers(
                blockers
            )

        require(prePersistenceBlockers.isEmpty()) {
            prePersistenceBlockers.joinToString(
                separator =
                    System.lineSeparator()
            )
        }

        CanonicalFinalCatalogWriter()
            .write(
                items =
                    finalItems,

                outputFile =
                    outputCatalogFile
            )

        val persistedItems =
            readPersistedCatalog(
                outputCatalogFile
            )

        val persistedCatalogRoundtripValid =
            persistedItems ==
                    finalItems

        if (!persistedCatalogRoundtripValid) {
            blockers +=
                "Persisted final catalog does not equal the approved " +
                        "expanded catalog."
        }

        val finalCatalogSha256 =
            sha256(
                outputCatalogFile
            )

        val sortedBlockers =
            normalizedBlockers(
                blockers
            )

        val valid =
            sortedBlockers.isEmpty() &&
                    approvedExpansion.valid &&
                    waveSixImpact.valid &&
                    terminalPolicyClosureReached &&
                    normalizedCatalogValidation.valid &&
                    uniqueCanonicalNames &&
                    uniqueNormalizedKeys &&
                    deterministicOrderValid &&
                    exactCatalogArithmeticValid &&
                    persistedCatalogRoundtripValid

        return CanonicalFoodCatalogFinalizationResult(
            version =
                CanonicalFoodCatalogFinalizationResult
                    .CURRENT_VERSION,

            finalizationId =
                "canonical-food-catalog-final-v1-" +
                        finalCatalogSha256.take(16),

            sourceBaselineId =
                approvedExpansion.sourceBaselineId,

            sourceBaselineCatalogSha256 =
                approvedExpansion.sourceBaselineCatalogSha256,

            sourcePolicySetId =
                terminalPolicyBatches.sourcePolicySetId,

            sourceWaveSixImpactKey =
                waveSixImpact.batchKey,

            baselineEntryCount =
                approvedExpansion.baselineEntryCount,

            materializedEntryCount =
                approvedExpansion.materializedEntryCount,

            finalCatalogEntryCount =
                finalItems.size,

            semanticAcceptedCandidateCount =
                approvedExpansion
                    .semanticallyAcceptedCandidateCount,

            semanticRejectedCandidateCount =
                approvedExpansion
                    .semanticallyRejectedCandidateCount,

            semanticReviewRequiredCandidateCount =
                approvedExpansion
                    .reviewRequiredCandidateCount,

            categoryCount =
                finalItems
                    .mapNotNull(CatalogFoodItem::category)
                    .distinct()
                    .size,

            uniqueCanonicalNameCount =
                uniqueCanonicalNameCount,

            uniqueNormalizedKeyCount =
                uniqueNormalizedKeyCount,

            openImplementationBatchCount =
                terminalPolicyBatches.batchCount,

            openPolicyGapCount =
                terminalPolicyBatches.missingPolicyGapCount,

            approvedExpansionValid =
                approvedExpansion.valid,

            waveSixImpactValid =
                waveSixImpact.valid,

            terminalPolicyClosureReached =
                terminalPolicyClosureReached,

            normalizedCatalogValidationValid =
                normalizedCatalogValidation.valid,

            uniqueCanonicalNames =
                uniqueCanonicalNames,

            uniqueNormalizedKeys =
                uniqueNormalizedKeys,

            deterministicOrderValid =
                deterministicOrderValid,

            exactCatalogArithmeticValid =
                exactCatalogArithmeticValid,

            persistedCatalogRoundtripValid =
                persistedCatalogRoundtripValid,

            finalCatalogSha256 =
                finalCatalogSha256,

            blockers =
                sortedBlockers,

            valid =
                valid
        )
    }

    private fun readPersistedCatalog(
        file: File
    ): List<CatalogFoodItem> =
        com.google.gson.GsonBuilder()
            .create()
            .fromJson(
                file.readText(
                    Charsets.UTF_8
                ),

                Array<CatalogFoodItem>::class.java
            )
            .toList()

    private fun normalizedBlockers(
        blockers: List<String>
    ): List<String> =
        blockers
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    private fun normalizeName(
        value: String
    ): String =
        value
            .trim()
            .lowercase(
                Locale.ROOT
            )
            .replace(
                MULTIPLE_WHITESPACE_REGEX,
                " "
            )

    private fun sha256(
        file: File
    ): String {
        val digest =
            MessageDigest.getInstance(
                "SHA-256"
            )

        file.inputStream()
            .buffered()
            .use { input ->
                val buffer =
                    ByteArray(
                        BUFFER_SIZE
                    )

                while (true) {
                    val count =
                        input.read(
                            buffer
                        )

                    if (count < 0) {
                        break
                    }

                    digest.update(
                        buffer,
                        0,
                        count
                    )
                }
            }

        return digest
            .digest()
            .joinToString(
                separator =
                    ""
            ) { byte ->
                "%02x".format(
                    byte
                )
            }
    }

    private companion object {

        const val BUFFER_SIZE =
            16 * 1024

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")
    }
}