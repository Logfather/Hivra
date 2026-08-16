package de.shopme.testing.system.tools.knowledge.multisource

import de.shopme.tools.knowledge.agribalyse.parser.AgribalyseRawSourceReducer
import de.shopme.tools.knowledge.ai.builder.runtime.MultiSourceRuntimeKnowledgeBuild
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultiSourceKnowledgeCandidateMergeTest {

    @Test
    fun mergeOpenFoodFactsAgribalyseAndCiqualCandidates() {

        val paths =
            KnowledgeBuildPaths.default()

        val agribalyseSourceFile =
            paths.agribalyseSourceRoot.resolve(
                "AGRIBALYSE3.2_Tableur produits alimentaires_PublieAOUT25.xlsx"
            )

        val outputDirectory =
            paths.projectRoot.resolve(
                "build/knowledge/test/multisource-runtime"
            )

        if (outputDirectory.exists()) {
            require(
                outputDirectory.deleteRecursively()
            ) {
                "Could not reset MultiSource Runtime Knowledge test directory: " +
                        outputDirectory.absolutePath
            }
        }

        require(
            outputDirectory.mkdirs()
        ) {
            "Could not create MultiSource Runtime Knowledge test directory: " +
                    outputDirectory.absolutePath
        }

        require(
            paths.openFoodFactsProducts.isFile
        ) {
            "Open Food Facts source does not exist: " +
                    paths.openFoodFactsProducts.absolutePath
        }

        require(
            paths.offNutritionReferenceAggregates.isFile
        ) {
            "OFF Nutrition aggregate source does not exist: " +
                    paths.offNutritionReferenceAggregates.absolutePath
        }

        require(
            agribalyseSourceFile.isFile
        ) {
            "Agribalyse source does not exist: " +
                    agribalyseSourceFile.absolutePath
        }

        require(
            paths.ciqualSourceRoot.isDirectory
        ) {
            "CIQUAL source directory does not exist: " +
                    paths.ciqualSourceRoot.absolutePath
        }

        val agribalyseReferenceFile =
            outputDirectory.resolve(
                "agribalyse-foods.slim.tsv"
            )

        AgribalyseRawSourceReducer()
            .reduce(
                input =
                    agribalyseSourceFile,
                output =
                    agribalyseReferenceFile,
                sheetName =
                    "Synthese"
            )

        require(
            agribalyseReferenceFile.isFile
        ) {
            "Agribalyse reference file was not generated: " +
                    agribalyseReferenceFile.absolutePath
        }

        require(
            agribalyseReferenceFile.length() > 0L
        ) {
            "Agribalyse reference file is empty: " +
                    agribalyseReferenceFile.absolutePath
        }

        val result =
            MultiSourceRuntimeKnowledgeBuild()
                .build(
                    offNutritionAggregateFile =
                        paths.offNutritionReferenceAggregates,
                    offFile =
                        paths.openFoodFactsProducts,
                    agribalyseFile =
                        agribalyseReferenceFile,
                    ciqualDirectory =
                        paths.ciqualSourceRoot,
                    outputDir =
                        outputDirectory,
                    maxOffCandidates =
                        50_000,
                    maxOffNutritionAggregates =
                        50_000
                )

        printBlockedFanoutKeys(
            result.blockedHighFanoutKeys
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("MULTI SOURCE RUNTIME KNOWLEDGE BUILD")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "OFF candidates=" +
                    result.offCandidateCount
        )
        println(
            "OFF nutrition aggregates=" +
                    result.offNutritionAggregateCount
        )
        println(
            "Agribalyse candidates=" +
                    result.agribalyseCandidateCount
        )
        println(
            "CIQUAL candidates=" +
                    result.ciqualCandidateCount
        )
        println(
            "Input candidates=" +
                    result.inputCandidateCount
        )
        println(
            "Normalized=" +
                    result.normalizedCandidateCount
        )
        println(
            "Merged=" +
                    result.mergedCandidateCount
        )
        println(
            "Conflicts=" +
                    result.conflictCount
        )
        println(
            "Multi dimension=" +
                    result.multiDimensionCandidateCount
        )

        println()
        println("ACTIVE KNOWLEDGE DIMENSIONS")
        println()

        println(
            "Nutrition candidates=" +
                    result.nutritionCandidateCount
        )
        println(
            "Nutrition artifact entries=" +
                    result.nutritionArtifactEntryCount
        )
        println(
            "Nutrition artifact=" +
                    result.nutritionArtifactFile.path
        )

        println(
            "Environmental candidates=" +
                    result.environmentalImpactCandidateCount
        )
        println(
            "Environmental artifact entries=" +
                    result.environmentalImpactArtifactEntryCount
        )
        println(
            "Environmental artifact=" +
                    result.environmentalImpactArtifactFile.path
        )

        println(
            "Allergens candidates=" +
                    result.allergensCandidateCount
        )
        println(
            "Allergen artifact entries=" +
                    result.allergenArtifactEntryCount
        )
        println(
            "Allergen artifact=" +
                    result.allergenArtifactFile.path
        )

        println(
            "Taxonomy candidates=" +
                    result.taxonomyCandidateCount
        )
        println(
            "Taxonomy artifact entries=" +
                    result.taxonomyArtifactEntryCount
        )
        println(
            "Taxonomy artifact=" +
                    result.taxonomyArtifactFile.path
        )

        println(
            "Processing candidates=" +
                    result.processingCandidateCount
        )
        println(
            "Processing artifact entries=" +
                    result.processingArtifactEntryCount
        )
        println(
            "Processing artifact=" +
                    result.processingArtifactFile.path
        )

        println(
            "Water candidates=" +
                    result.waterCandidateCount
        )
        println(
            "Water artifact entries=" +
                    result.waterArtifactEntryCount
        )
        println(
            "Water artifact=" +
                    result.waterArtifactFile.path
        )

        println(
            "Water Stress candidates=" +
                    result.waterStressCandidateCount
        )
        println(
            "Water Stress artifact entries=" +
                    result.waterStressArtifactEntryCount
        )
        println(
            "Water Stress artifact=" +
                    result.waterStressArtifactFile.path
        )

        println(
            "Pesticides candidates=" +
                    result.pesticidesCandidateCount
        )
        println(
            "Pesticides artifact entries=" +
                    result.pesticidesArtifactEntryCount
        )
        println(
            "Pesticides artifact=" +
                    result.pesticidesArtifactFile.path
        )

        println(
            "Food Miles candidates=" +
                    result.foodMilesCandidateCount
        )
        println(
            "Food Miles artifact entries=" +
                    result.foodMilesArtifactEntryCount
        )
        println(
            "Food Miles artifact=" +
                    result.foodMilesArtifactFile.path
        )

        println(
            "Nutri Score candidates=" +
                    result.nutriScoreCandidateCount
        )
        println(
            "Nutri Score artifact entries=" +
                    result.nutriScoreArtifactEntryCount
        )
        println(
            "Nutri Score artifact=" +
                    result.nutriScoreArtifactFile.path
        )

        println(
            "Diet candidates=" +
                    result.dietCandidateCount
        )
        println(
            "Diet artifact entries=" +
                    result.dietArtifactEntryCount
        )
        println(
            "Diet artifact=" +
                    result.dietArtifactFile.path
        )

        println(
            "Animal Welfare candidates=" +
                    result.animalWelfareCandidateCount
        )
        println(
            "Animal Welfare artifact entries=" +
                    result.animalWelfareArtifactEntryCount
        )
        println(
            "Animal Welfare artifact=" +
                    result.animalWelfareArtifactFile.path
        )

        assertTrue(
            result.offCandidateCount > 0,
            "Open Food Facts must produce candidates."
        )

        assertTrue(
            result.agribalyseCandidateCount > 0,
            "Agribalyse must produce candidates."
        )

        assertTrue(
            result.ciqualCandidateCount > 0,
            "CIQUAL must produce nutrition candidates."
        )

        val expectedInputCandidateCount =
            result.offCandidateCount +
                    result.offNutritionAggregateCount +
                    result.agribalyseCandidateCount +
                    result.ciqualCandidateCount

        assertEquals(
            expectedInputCandidateCount,
            result.inputCandidateCount,
            "Input candidate count must equal the sum of raw OFF, " +
                    "OFF nutrition aggregate, Agribalyse and CIQUAL candidates."
        )

        assertTrue(
            result.offNutritionAggregateCount > 0,
            "Expected OFF nutrition aggregates to be included in the build."
        )

        assertTrue(
            result.normalizedCandidateCount > 0,
            "The combined source candidates must produce normalized candidates."
        )

        assertTrue(
            result.normalizedCandidateCount <=
                    result.inputCandidateCount,
            "Normalization must not increase the number of source candidates."
        )

        assertTrue(
            result.mergedCandidateCount > 0,
            "The combined source candidates must produce merged candidates."
        )

        assertTrue(
            result.mergedCandidateCount <=
                    result.normalizedCandidateCount,
            "Merging must not increase the number of normalized candidates."
        )

        assertTrue(
            result.multiDimensionCandidateCount > 0
        )

        assertTrue(
            result.nutritionCandidateCount > 0
        )
        assertTrue(
            result.nutritionArtifactEntryCount > 0
        )
        assertTrue(
            result.nutritionArtifactFile.isFile
        )

        assertTrue(
            result.environmentalImpactCandidateCount > 0
        )
        assertTrue(
            result.environmentalImpactArtifactEntryCount > 0
        )
        assertTrue(
            result.environmentalImpactArtifactFile.isFile
        )

        assertTrue(
            result.allergensCandidateCount > 0
        )
        assertTrue(
            result.allergenArtifactEntryCount > 0
        )
        assertTrue(
            result.allergenArtifactFile.isFile
        )

        assertTrue(
            result.taxonomyCandidateCount > 0
        )
        assertTrue(
            result.taxonomyArtifactEntryCount > 0
        )
        assertTrue(
            result.taxonomyArtifactFile.isFile
        )

        assertTrue(
            result.processingCandidateCount > 0
        )
        assertTrue(
            result.processingArtifactEntryCount > 0
        )
        assertTrue(
            result.processingArtifactFile.isFile
        )

        assertTrue(
            result.waterCandidateCount > 0
        )
        assertTrue(
            result.waterArtifactEntryCount > 0
        )
        assertTrue(
            result.waterArtifactFile.isFile
        )

        assertTrue(
            result.waterStressCandidateCount > 0
        )
        assertTrue(
            result.waterStressArtifactEntryCount > 0
        )
        assertTrue(
            result.waterStressArtifactFile.isFile
        )

        assertTrue(
            result.pesticidesCandidateCount > 0
        )
        assertTrue(
            result.pesticidesArtifactEntryCount > 0
        )
        assertTrue(
            result.pesticidesArtifactFile.isFile
        )

        assertTrue(
            result.foodMilesCandidateCount > 0
        )
        assertTrue(
            result.foodMilesArtifactEntryCount > 0
        )
        assertTrue(
            result.foodMilesArtifactFile.isFile
        )

        assertTrue(
            result.nutriScoreCandidateCount > 0
        )
        assertTrue(
            result.nutriScoreArtifactEntryCount > 0
        )
        assertTrue(
            result.nutriScoreArtifactFile.isFile
        )

        assertTrue(
            result.dietCandidateCount > 0
        )
        assertTrue(
            result.dietArtifactEntryCount > 0
        )
        assertTrue(
            result.dietArtifactFile.isFile
        )

        assertTrue(
            result.animalWelfareCandidateCount > 0
        )
        assertTrue(
            result.animalWelfareArtifactEntryCount > 0
        )
        assertTrue(
            result.animalWelfareArtifactFile.isFile
        )
    }

    private fun printBlockedFanoutKeys(
        keys: Map<String, Int>
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("BLOCKED HIGH FANOUT MATCH KEYS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        keys
            .entries
            .sortedByDescending {
                it.value
            }
            .take(
                30
            )
            .forEach {
                println(
                    "${it.key}=${it.value}"
                )
            }
    }
}