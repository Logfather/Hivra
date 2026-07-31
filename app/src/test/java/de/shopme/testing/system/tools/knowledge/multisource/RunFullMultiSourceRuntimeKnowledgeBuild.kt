package de.shopme.tools.knowledge.runtime

import de.shopme.tools.knowledge.ai.builder.runtime.MultiSourceRuntimeKnowledgeBuild
import java.io.File

object RunFullMultiSourceRuntimeKnowledgeBuild {

    @JvmStatic
    fun main(args: Array<String>) {

        val result =
            MultiSourceRuntimeKnowledgeBuild()
                .build(
                    offFile =
                        File(
                            "../data/generated/openfoodfacts/" +
                                    "openfoodfacts-products.slim.jsonl.gz"
                        ),
                    offNutritionAggregateFile =
                        File(
                            "../data/generated/knowledge/references/off/" +
                                    "off-nutrition-reference-aggregates.json"
                        ),
                    agribalyseFile =
                        File(
                            "../data/generated/agribalyse/" +
                                    "agribalyse-foods.slim.tsv"
                        ),
                    outputDir =
                        File(
                            "../data/generated/runtime"
                        ),
                    maxOffCandidates =
                        null,
                    maxOffNutritionAggregates =
                        null
                )

        println(
            """
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            FULL MULTI SOURCE BUILD DONE
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            OFF candidates=${result.offCandidateCount}
            Input=${result.inputCandidateCount}
            Normalized=${result.normalizedCandidateCount}
            Merged=${result.mergedCandidateCount}
            Conflicts=${result.conflictCount}
            OFF raw candidates=${'$'}{result.offCandidateCount}
            OFF nutrition aggregates=${'$'}{result.offNutritionAggregateCount}
            
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            
            """.trimIndent()
        )
    }
}