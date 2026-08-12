package de.shopme.testing.system.tools.knowledge.catalog.nutrition.group

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HighConfidenceNutritionSourceVariantReviewPolicyTest {

    @Test
    fun resolvesSafeMapleSyrupTyposButKeepsWeakAliasesInReview() {

        val paths =
            KnowledgeBuildPaths.default()

        val file =
            paths.projectRoot.resolve(
                "build/knowledge/source-variant-groups/" +
                        "nutrition.source-variant-membership.resolved.json"
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

        val ahornsirup =
            root["groups"]
                .asJsonArray
                .map {
                    it.asJsonObject
                }
                .single {
                    it["catalogKey"]
                        .asString ==
                            "ahornsirup"
                }

        val pureMapleTypo =
            findVariant(
                group =
                    ahornsirup,
                serverKey =
                    "100% pure canadian marple syrup"
            )

        assertNotNull(
            pureMapleTypo
        )

        assertEquals(
            "ACCEPT",
            pureMapleTypo[
                "decision"
            ].asString
        )

        assertEquals(
            "HIGH_CONFIDENCE_TYPO_IDENTITY",
            pureMapleTypo[
                "reason"
            ].asString
        )

        val compactMaple =
            findVariant(
                group =
                    ahornsirup,
                serverKey =
                    "100% puremaple syrup"
            )

        assertNotNull(
            compactMaple
        )

        assertEquals(
            "ACCEPT",
            compactMaple[
                "decision"
            ].asString
        )

        assertEquals(
            "HIGH_CONFIDENCE_COMPACT_IDENTITY",
            compactMaple[
                "reason"
            ].asString
        )

        val weakAlias =
            findVariant(
                group =
                    ahornsirup,
                serverKey =
                    "100% pure grade a amber color"
            )

        assertNotNull(
            weakAlias
        )

        assertEquals(
            "REVIEW",
            weakAlias[
                "decision"
            ].asString
        )
    }

    private fun findVariant(
        group: JsonObject,
        serverKey: String
    ): JsonObject? {

        return sequenceOf(
            "acceptedSourceVariants",
            "rejectedSourceVariants",
            "reviewSourceVariants"
        )
            .flatMap { field ->

                group[
                    field
                ]
                    .asJsonArray
                    .asSequence()
            }
            .map {
                it.asJsonObject
            }
            .singleOrNull {
                it["serverKey"]
                    .asString ==
                        serverKey
            }
    }
}