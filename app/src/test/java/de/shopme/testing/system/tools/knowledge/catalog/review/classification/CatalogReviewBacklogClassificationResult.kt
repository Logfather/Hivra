package de.shopme.testing.system.tools.knowledge.catalog.review.classification

data class CatalogReviewBacklogClassificationResult(
    val version: Int,

    val sourceBacklogEntryCount: Int,
    val classifiedEntryCount: Int,

    val potentiallyDeterministicCount: Int,
    val manualReviewRequiredCount: Int,
    val conflictingEvidenceCount: Int,
    val splitRequiredCount: Int,

    val entriesWithUniqueMergeTargetCount: Int,
    val entriesWithMultipleConflictReasonsCount: Int,

    val countsByClassification:
    Map<CatalogReviewBacklogClassification, Int>,

    val countsByAutomationAssessment:
    Map<CatalogReviewAutomationAssessment, Int>,

    val countsByCategory:
    Map<String, Int>,

    val entries:
    List<ClassifiedCatalogReviewBacklogEntry>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(sourceBacklogEntryCount >= 0)
        require(classifiedEntryCount >= 0)

        require(potentiallyDeterministicCount >= 0)
        require(manualReviewRequiredCount >= 0)
        require(conflictingEvidenceCount >= 0)
        require(splitRequiredCount >= 0)

        require(entriesWithUniqueMergeTargetCount >= 0)
        require(entriesWithMultipleConflictReasonsCount >= 0)

        require(
            classifiedEntryCount == entries.size
        ) {
            "classifiedEntryCount must equal entries size."
        }

        require(
            sourceBacklogEntryCount ==
                    classifiedEntryCount
        ) {
            "All supplied unresolved backlog entries must be classified."
        }

        require(
            potentiallyDeterministicCount +
                    manualReviewRequiredCount +
                    conflictingEvidenceCount +
                    splitRequiredCount ==
                    classifiedEntryCount
        ) {
            "Automation assessment counts must cover all entries."
        }

        require(
            countsByAutomationAssessment.values.sum() ==
                    classifiedEntryCount
        ) {
            "countsByAutomationAssessment must cover all entries."
        }

        require(
            countsByClassification.values.sum() ==
                    classifiedEntryCount
        ) {
            "Primary classification counts must cover all entries."
        }

        require(
            countsByCategory.values.sum() ==
                    classifiedEntryCount
        ) {
            "countsByCategory must cover all entries."
        }

        require(
            entriesWithUniqueMergeTargetCount ==
                    entries.count {
                        it.hasUniqueMergeTarget
                    }
        ) {
            "entriesWithUniqueMergeTargetCount is inconsistent."
        }

        require(
            entriesWithMultipleConflictReasonsCount ==
                    entries.count {
                        it.hasMultipleConflictReasons
                    }
        ) {
            "entriesWithMultipleConflictReasonsCount is inconsistent."
        }

        require(
            entries.map { it.sourceIndex }
                .distinct()
                .size ==
                    entries.size
        ) {
            "Classified entries must have unique sourceIndex values."
        }

        require(
            entries ==
                    entries.sortedBy { it.sourceIndex }
        ) {
            "Classified entries must be sorted by sourceIndex."
        }

        require(valid) {
            "Review backlog classification result must be valid."
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}