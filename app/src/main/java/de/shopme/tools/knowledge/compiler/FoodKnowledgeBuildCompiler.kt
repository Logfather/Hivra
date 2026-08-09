package de.shopme.tools.knowledge.compiler

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.compiler.candidate.CatalogImportWorkflow
import de.shopme.tools.knowledge.compiler.candidate.CatalogImportWriter
import de.shopme.tools.knowledge.foods.DefaultFoodLookup
import de.shopme.tools.knowledge.foods.FoodsKnowledgeGenerator
import de.shopme.tools.knowledge.foods.FoodsKnowledgeWriter
import de.shopme.tools.knowledge.foods.loader.FileFoodsKnowledgeLoader
import de.shopme.tools.knowledge.foods.report.FoodsKnowledgeCoverageAnalyzer
import de.shopme.tools.knowledge.foods.report.FoodsKnowledgeCoveragePrinter
import de.shopme.tools.knowledge.foods.runtime.NutritionRuntimeArtifactGenerator
import de.shopme.tools.knowledge.nutrition.NutritionKnowledgeJsonWriter
import de.shopme.tools.knowledge.publisher.KnowledgeArtifactPublisher
import de.shopme.tools.knowledge.reader.ResourceCatalogReader
import java.io.File

class FoodKnowledgeBuildCompiler {

    fun build(
        importFile: File? = null
    ) {

        val paths =
            KnowledgeBuildPaths.default()

        paths.ensureBuildDirectories()

        val foodsRuntimeFile =
            paths.runtimeArtifact(
                "foods.json"
            )

        val nutritionRuntimeFile =
            paths.runtimeArtifact(
                "nutrition.json"
            )

        val proposedCatalogFile =
            paths.intermediateArtifact(
                "catalog/foods.proposed.json"
            )

        val reader =
            ResourceCatalogReader()

        var catalog =
            reader.read()

        if (importFile != null) {

            val importResult =
                CatalogImportWorkflow()
                    .import(
                        existingItems =
                            catalog,
                        importFile =
                            importFile
                    )

            if (!importResult.isSuccess) {
                error(
                    "Catalog import failed:\n" +
                            importResult.errors
                                .joinToString("\n")
                )
            }

            val mergeResult =
                importResult.mergeResult
                    ?: error(
                        "Catalog import did not produce merge result"
                    )

            catalog =
                mergeResult.items

            CatalogImportWriter()
                .write(
                    items =
                        catalog,
                    file =
                        proposedCatalogFile
                )

            println()
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("🧠 CATALOG IMPORT")
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println(
                "Imported : " +
                        mergeResult.summary.importedItems
            )
            println(
                "Added    : " +
                        mergeResult.summary.addedItems
            )
            println(
                "Updated  : " +
                        mergeResult.summary.updatedItems
            )
            println(
                "Merged   : " +
                        mergeResult.summary.mergedItems
            )
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println()
        }

        require(foodsRuntimeFile.isFile) {
            "Bootstrap foods knowledge does not exist: " +
                    foodsRuntimeFile.absolutePath
        }

        val bootstrapFoodsKnowledge =
            FileFoodsKnowledgeLoader(
                foodsRuntimeFile
            ).load()

        val foodLookup =
            DefaultFoodLookup(
                bootstrapFoodsKnowledge
            )

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