package de.shopme.tools.knowledge.rebuild.nutrition

interface NutritionKnowledgeSnapshotReader {

    fun read(
    ): NutritionKnowledgeRebuildSnapshot
}

interface NutritionKnowledgeRequestRebuilder {

    fun rebuild():
            NutritionKnowledgeRequestRebuildResult
}

data class NutritionKnowledgeRequestRebuildResult(
    val requestCount: Int,
    val requestFile: String
)

interface NutritionKnowledgeMatchingStep {

    fun run(
        mode: NutritionKnowledgeRebuildMode
    ): NutritionKnowledgeRebuildMatchingResult
}

interface NutritionKnowledgeMappingPersistenceStep {

    fun run():
            NutritionKnowledgeRebuildPersistenceResult
}

interface NutritionKnowledgeRuntimeRebuildStep {

    fun run()
}