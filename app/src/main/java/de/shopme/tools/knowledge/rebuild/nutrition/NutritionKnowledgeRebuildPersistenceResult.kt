package de.shopme.tools.knowledge.rebuild.nutrition

data class NutritionKnowledgeRebuildPersistenceResult(
    val existingMappingCount: Int,
    val addedMappingCount: Int,
    val removedMappingCount: Int,
    val unchangedMappingCount: Int,
    val conflictCount: Int,
    val finalMappingCount: Int
) {

    init {
        require(existingMappingCount >= 0) {
            "Existing mapping count must not be negative."
        }

        require(addedMappingCount >= 0) {
            "Added mapping count must not be negative."
        }

        require(removedMappingCount >= 0) {
            "Removed mapping count must not be negative."
        }

        require(unchangedMappingCount >= 0) {
            "Unchanged mapping count must not be negative."
        }

        require(conflictCount >= 0) {
            "Conflict count must not be negative."
        }

        require(finalMappingCount >= 0) {
            "Final mapping count must not be negative."
        }

        require(
            existingMappingCount ==
                    unchangedMappingCount +
                    removedMappingCount
        ) {
            "Existing mapping count must equal unchanged plus removed: " +
                    "existing=$existingMappingCount, " +
                    "unchanged=$unchangedMappingCount, " +
                    "removed=$removedMappingCount."
        }

        require(
            finalMappingCount ==
                    unchangedMappingCount +
                    addedMappingCount
        ) {
            "Final mapping count must equal unchanged plus added: " +
                    "final=$finalMappingCount, " +
                    "unchanged=$unchangedMappingCount, " +
                    "added=$addedMappingCount."
        }

        require(
            finalMappingCount ==
                    existingMappingCount +
                    addedMappingCount -
                    removedMappingCount
        ) {
            "Final mapping count must equal existing plus added minus removed: " +
                    "existing=$existingMappingCount, " +
                    "added=$addedMappingCount, " +
                    "removed=$removedMappingCount, " +
                    "final=$finalMappingCount."
        }
    }
}