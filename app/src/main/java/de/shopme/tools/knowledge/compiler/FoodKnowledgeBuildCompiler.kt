package de.shopme.tools.knowledge.compiler

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.foods.EmptyFoodLookup
import de.shopme.tools.knowledge.foods.FoodsKnowledgeGenerator
import de.shopme.tools.knowledge.foods.FoodsKnowledgeWriter
import de.shopme.tools.knowledge.foods.report.FoodsKnowledgeCoverageAnalyzer
import de.shopme.tools.knowledge.foods.report.FoodsKnowledgeCoveragePrinter
import de.shopme.tools.knowledge.foods.runtime.NutritionRuntimeArtifactGenerator
import de.shopme.tools.knowledge.nutrition.NutritionKnowledgeJsonWriter
import de.shopme.tools.knowledge.publisher.KnowledgeArtifactPublisher
import de.shopme.tools.knowledge.reader.ResourceCatalogReader

class FoodKnowledgeBuildCompiler {

    fun build() {

        val paths =
            KnowledgeBuildPaths.default()

        paths.ensureBuildDirectories()

        paths.validateCanonicalCatalogAuthority()

        val foodsRuntimeFile =
            paths.runtimeArtifact(
                "foods.json"
            )

        val nutritionRuntimeFile =
            paths.runtimeArtifact(
                "nutrition.json"
            )

        val reader =
            ResourceCatalogReader()

        var catalog =
            reader.read()

        val foodLookup =
            EmptyFoodLookup

        val resolvers =
            BuildKnowledgeResolversFactory(
                runtimeDirectory =
                    paths.runtimeRoot
            ).create()

        val compiler =
            FullFoodKnowledgeBuildCompilerFactory
                .create(
                    foodLookup =
                        foodLookup,
                    resolvers =
                        resolvers
                )

        val knowledge =
            catalog
                .map(
                    compiler::compile
                )
                .sortedBy {
                    it.normalizedName
                }

        val foodsKnowledge =
            FoodsKnowledgeGenerator()
                .generate(
                    entries =
                        knowledge
                )

        FoodsKnowledgeWriter()
            .write(
                knowledge =
                    foodsKnowledge,
                outputFile =
                    foodsRuntimeFile
            )

        val nutritionRuntimeArtifact =
            NutritionRuntimeArtifactGenerator()
                .generate(
                    foodsKnowledge
                )

        NutritionKnowledgeJsonWriter()
            .write(
                knowledge =
                    nutritionRuntimeArtifact,
                output =
                    nutritionRuntimeFile
            )

        KnowledgeArtifactPublisher(
            generatedDirectory =
                paths.runtimeRoot,
            knowledgeDirectory =
                paths.publishedRuntimeRoot
        ).publish(
            "foods.json"
        )

        val canonicalFoodsCoverageReport =
            FoodsKnowledgeCoverageAnalyzer()
                .analyze(
                    foodsKnowledge
                )

        FoodsKnowledgeCoveragePrinter()
            .print(
                canonicalFoodsCoverageReport
            )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🧠 FOOD KNOWLEDGE BUILD")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "📊 Entries : ${knowledge.size}"
        )
        println(
            "Runtime    : ${paths.runtimeRoot.path}"
        )
        println(
            "Published  : ${paths.publishedRuntimeRoot.path}"
        )
        println("🏁 FINISHED")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }
}