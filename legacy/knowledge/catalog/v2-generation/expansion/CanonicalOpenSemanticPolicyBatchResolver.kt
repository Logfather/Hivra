package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchResult

class CanonicalOpenSemanticPolicyBatchResolver {

    fun resolve(
        openBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        category: String,

        axis:
        CanonicalProductFamilyVariantAxis
    ): CanonicalSemanticPolicyImplementationBatch {
        require(openBatches.valid)
        require(category.isNotBlank())

        val matches =
            openBatches.batches
                .filter {
                    it.category == category &&
                            it.axis == axis
                }

        require(matches.size == 1) {
            "Expected exactly one open implementation batch for " +
                    "category '$category' and axis '${axis.name}', " +
                    "but found ${matches.size}: " +
                    matches
                        .map {
                            it.batchKey
                        }
                        .sorted()
                        .joinToString()
        }

        val result =
            matches.single()

        require(result.complete) {
            "Open implementation batch '${result.batchKey}' is incomplete."
        }

        return result
    }
}