package de.shopme.tools.knowledge.compiler

object CreateFoodKnowledge {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {
        build()
    }

    fun build() {
        FoodKnowledgeBuildCompiler()
            .build()
    }
}