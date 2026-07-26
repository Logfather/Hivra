package de.shopme.tools.knowledge.off.nutrition.reference.deduplication

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate

data class OFFNutritionReferenceDeduplicationResult(
    val inputCandidateCount: Int,
    val outputCandidateCount: Int,
    val removedDuplicateCount: Int,
    val duplicateGroupCount: Int,
    val candidates: List<CanonicalOFFNutritionReferenceCandidate>,
    val duplicateGroups: List<OFFNutritionReferenceDuplicateGroup>
) {

    init {
        require(inputCandidateCount >= 0) {
            "inputCandidateCount must not be negative."
        }

        require(outputCandidateCount >= 0) {
            "outputCandidateCount must not be negative."
        }

        require(removedDuplicateCount >= 0) {
            "removedDuplicateCount must not be negative."
        }

        require(duplicateGroupCount >= 0) {
            "duplicateGroupCount must not be negative."
        }

        require(
            inputCandidateCount ==
                    outputCandidateCount +
                    removedDuplicateCount
        ) {
            "Deduplication counts do not cover all input candidates."
        }

        require(outputCandidateCount == candidates.size) {
            "outputCandidateCount must equal candidates.size."
        }

        require(duplicateGroupCount == duplicateGroups.size) {
            "duplicateGroupCount must equal duplicateGroups.size."
        }

        val expectedRemovedDuplicateCount =
            duplicateGroups.sumOf { group ->
                group.mergedSourceIds.size - 1
            }

        require(
            removedDuplicateCount ==
                    expectedRemovedDuplicateCount
        ) {
            "removedDuplicateCount does not match duplicate groups: " +
                    "expected=$expectedRemovedDuplicateCount, " +
                    "actual=$removedDuplicateCount."
        }

        require(
            candidates ==
                    candidates.sortedWith(CANDIDATE_COMPARATOR)
        ) {
            "Deduplicated OFF nutrition candidates must be sorted."
        }

        require(
            duplicateGroups ==
                    duplicateGroups.sortedWith(GROUP_COMPARATOR)
        ) {
            "Duplicate groups must be sorted deterministically."
        }
    }

    private companion object {

        val CANDIDATE_COMPARATOR =
            compareBy<CanonicalOFFNutritionReferenceCandidate>(
                { it.canonicalId },
                { it.sourceId }
            )

        val GROUP_COMPARATOR =
            compareBy<OFFNutritionReferenceDuplicateGroup>(
                { it.canonicalId },
                { it.nutritionFingerprint },
                { it.representativeSourceId }
            )
    }
}