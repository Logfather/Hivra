package de.shopme.testing.system.tools.knowledge.runtime

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import kotlin.test.Test
import kotlin.test.assertTrue

class ReportCatalogKnowledgeKeyReductionTest {

    private val gson =
        Gson()

    @Test
    fun reportCatalogKnowledgeKeyReduction() {

        val file =
            KnowledgeBuildPaths
                .default()
                .canonicalFoodCatalog

        assertTrue(
            file.isFile,
            "Catalog file missing: ${file.path}"
        )

        val type =
            object : TypeToken<List<JsonObject>>() {}.type

        val items =
            gson.fromJson<List<JsonObject>>(
                file.readText(),
                type
            )

        var normalizedEnglishCount =
            0

        var fallbackCount =
            0

        var missingNameCount =
            0

        val keys =
            mutableListOf<String>()

        items.forEach { item ->

            val normalizedEnglish =
                item.string(
                    "normalizedEnglish"
                )

            val fallback =
                item.string("name")
                    ?: item.string("productName")
                    ?: item.string("title")

            when {
                normalizedEnglish != null -> {
                    normalizedEnglishCount++

                    keys +=
                        normalizeKey(
                            normalizedEnglish
                        )
                }

                fallback != null -> {
                    fallbackCount++

                    keys +=
                        normalizeKey(
                            fallback
                        )
                }

                else -> {
                    missingNameCount++
                }
            }
        }

        val nonBlankKeys =
            keys.filter {
                it.isNotBlank()
            }

        val grouped =
            nonBlankKeys
                .groupingBy {
                    it
                }
                .eachCount()

        val duplicateGroups =
            grouped.filterValues {
                it > 1
            }

        val duplicateRows =
            duplicateGroups.values.sumOf {
                it - 1
            }

        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CATALOG KNOWLEDGE KEY REDUCTION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Catalog rows             = ${items.size}")
        println("normalizedEnglish rows   = $normalizedEnglishCount")
        println("Fallback-name rows       = $fallbackCount")
        println("Missing-name rows        = $missingNameCount")
        println("Non-blank keys           = ${nonBlankKeys.size}")
        println("Unique knowledge keys    = ${grouped.size}")
        println("Duplicate groups         = ${duplicateGroups.size}")
        println("Rows collapsed by dedupe = $duplicateRows")

        println()
        println("Top duplicate keys:")

        duplicateGroups
            .entries
            .sortedByDescending {
                it.value
            }
            .take(30)
            .forEach { entry ->
                println(
                    "${entry.key} -> ${entry.value}"
                )
            }
    }

    private fun JsonObject.string(
        key: String
    ): String? =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive
            }
            ?.asString
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }

    private fun normalizeKey(
        value: String
    ): String =
        value
            .trim()
            .lowercase()
            .replace("-", " ")
            .replace("_", " ")
            .collapseWhitespace()
            .trim()

    private fun String.collapseWhitespace(): String {

        val builder =
            StringBuilder(length)

        var previousWasWhitespace =
            false

        for (char in this) {
            if (char.isWhitespace()) {
                if (!previousWasWhitespace) {
                    builder.append(' ')
                    previousWasWhitespace = true
                }
            } else {
                builder.append(char)
                previousWasWhitespace = false
            }
        }

        return builder.toString()
    }
}