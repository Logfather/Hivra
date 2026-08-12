package de.shopme.testing.system.tools.knowledge.catalog.nutrition.group

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CanonicalNutritionSourceVariantMembershipPolicyTest {

    @Test
    fun grannySmithAppleJuiceBelongsToAppleJuiceFamily() {

        val paths =
            KnowledgeBuildPaths.default()

        val file =
            paths.projectRoot.resolve(
                "build/knowledge/source-variant-groups/" +
                        "nutrition.source-variant-membership.json"
            )

        require(
            file.isFile
        )

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        val apfelsaft =
            root["groups"]
                .asJsonArray
                .map {
                    it.asJsonObject
                }
                .single {
                    it["catalogKey"]
                        .asString ==
                            "apfelsaft"
                }

        val decisions =
            sequenceOf(
                "acceptedSourceVariants",
                "rejectedSourceVariants",
                "reviewSourceVariants"
            )
                .flatMap { field ->

                    apfelsaft[
                        field
                    ]
                        .asJsonArray
                        .asSequence()
                }
                .map {
                    it.asJsonObject
                }
                .toList()

        val grannySmithJuice =
            decisions
                .singleOrNull {
                    it["serverKey"]
                        .asString
                        .contains(
                            "granny smith apple juice",
                            ignoreCase = true
                        )
                }

        assertNotNull(
            grannySmithJuice
        )

        assertEquals(
            "ACCEPT",
            grannySmithJuice[
                "decision"
            ].asString
        )
    }
}