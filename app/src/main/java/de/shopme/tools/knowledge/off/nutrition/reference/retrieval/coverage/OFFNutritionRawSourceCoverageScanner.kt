package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequest
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionRetrievalTextNormalizer
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.zip.GZIPInputStream

class OFFNutritionRawSourceCoverageScanner {

    fun scan(
        inputFile: File,
        missingRequests: List<CatalogOFFNutritionRetrievalRequest>
    ): OFFNutritionRawSourceCoverage {

        require(inputFile.isFile) {
            "OFF slim dump not found: ${inputFile.absolutePath}"
        }

        val targets =
            missingRequests
                .sortedBy { request ->
                    request.catalogIndex
                }
                .associate { request ->
                    request.catalogIndex to
                            Target(
                                catalogIndex =
                                    request.catalogIndex,
                                normalizedAliases =
                                    buildSet {
                                        add(request.catalogKey)
                                        add(request.normalizedEnglish)
                                        addAll(request.catalogTerms)
                                    }
                                        .asSequence()
                                        .map(
                                            OFFNutritionRetrievalTextNormalizer::normalize
                                        )
                                        .filter(String::isNotBlank)
                                        .toSortedSet()
                            )
                }

        val accumulators =
            targets.mapValues { (_, target) ->
                Accumulator(
                    target = target
                )
            }

        var scannedProductCount =
            0L

        GZIPInputStream(
            BufferedInputStream(
                FileInputStream(inputFile)
            )
        )
            .bufferedReader(Charsets.UTF_8)
            .use { reader ->

                var lineNumber =
                    0L

                while (true) {
                    val line =
                        reader.readLine()
                            ?: break

                    lineNumber++

                    if (line.isBlank()) {
                        continue
                    }

                    val product =
                        parseProduct(
                            line = line,
                            inputFile = inputFile,
                            lineNumber = lineNumber
                        )

                    scannedProductCount++

                    inspectProduct(
                        product = product,
                        accumulators = accumulators
                    )
                }
            }

        return OFFNutritionRawSourceCoverage(
            scannedProductCount =
                scannedProductCount,
            entriesByCatalogIndex =
                accumulators
                    .mapValues { (_, accumulator) ->
                        accumulator.toEntry()
                    }
                    .toSortedMap()
        )
    }

    private fun parseProduct(
        line: String,
        inputFile: File,
        lineNumber: Long
    ): JsonObject {

        val element =
            try {
                JsonParser.parseString(line)
            } catch (exception: RuntimeException) {
                throw IllegalStateException(
                    "Invalid JSONL record in " +
                            "${inputFile.absolutePath} at line $lineNumber.",
                    exception
                )
            }

        require(element.isJsonObject) {
            "Expected JSON object at line $lineNumber."
        }

        return element.asJsonObject
    }

    private fun inspectProduct(
        product: JsonObject,
        accumulators: Map<Int, Accumulator>
    ) {

        val identityValues =
            IDENTITY_FIELDS
                .mapNotNull { fieldName ->
                    product.string(fieldName)
                }
                .map(
                    OFFNutritionRetrievalTextNormalizer::normalize
                )
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        if (identityValues.isEmpty()) {
            return
        }

        val matchedTargets =
            accumulators.values
                .filter { accumulator ->
                    accumulator.matches(
                        identityValues = identityValues
                    )
                }

        if (matchedTargets.isEmpty()) {
            return
        }

        val nutrition =
            inspectNutrition(
                product = product
            )

        val representativeName =
            IDENTITY_FIELDS
                .asSequence()
                .mapNotNull { fieldName ->
                    product.string(fieldName)
                }
                .firstOrNull()
                ?: identityValues.first()

        matchedTargets.forEach { accumulator ->
            accumulator.record(
                representativeName =
                    representativeName,
                hasAnyNutrition =
                    nutrition.hasAnyNutrition,
                hasUsableNutrition =
                    nutrition.hasUsableNutrition
            )
        }
    }

    private fun inspectNutrition(
        product: JsonObject
    ): NutritionPresence {

        val nutriments =
            product
                .get("nutriments")
                ?.takeIf(JsonElement::isJsonObject)
                ?.asJsonObject

        val valueSource =
            nutriments
                ?: product

        val presentDimensions =
            CORE_NUTRIENT_FIELDS
                .filterValues { fieldNames ->
                    fieldNames.any { fieldName ->
                        valueSource.hasFiniteNumber(
                            fieldName = fieldName
                        )
                    }
                }
                .keys

        val hasAnyNutrition =
            presentDimensions.isNotEmpty()

        val hasUsableNutrition =
            REQUIRED_CORE_DIMENSIONS.all(
                presentDimensions::contains
            )

        return NutritionPresence(
            hasAnyNutrition =
                hasAnyNutrition,
            hasUsableNutrition =
                hasUsableNutrition
        )
    }

    private fun JsonObject.hasFiniteNumber(
        fieldName: String
    ): Boolean {

        val element =
            get(fieldName)
                ?: return false

        val value =
            when {
                element.isJsonPrimitive &&
                        element.asJsonPrimitive.isNumber -> {
                    runCatching {
                        element.asDouble
                    }.getOrNull()
                }

                element.isJsonPrimitive &&
                        element.asJsonPrimitive.isString -> {
                    element
                        .asString
                        .trim()
                        .replace(',', '.')
                        .toDoubleOrNull()
                }

                else -> {
                    null
                }
            }

        return value?.isFinite() == true
    }

    private fun JsonObject.string(
        fieldName: String
    ): String? {

        val element =
            get(fieldName)
                ?: return null

        if (
            !element.isJsonPrimitive ||
            !element.asJsonPrimitive.isString
        ) {
            return null
        }

        return element
            .asString
            .trim()
            .takeIf(String::isNotBlank)
    }

    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean {

        if (
            text.isBlank() ||
            phrase.isBlank()
        ) {
            return false
        }

        if (text == phrase) {
            return true
        }

        return " $text "
            .contains(
                " $phrase "
            )
    }

    private inner class Accumulator(
        private val target: Target
    ) {

        private var productMatchCount =
            0

        private var productWithAnyNutritionCount =
            0

        private var productWithUsableNutritionCount =
            0

        private val matchedProductNames =
            sortedSetOf<String>()

        fun matches(
            identityValues: List<String>
        ): Boolean {

            return target.normalizedAliases.any { alias ->
                identityValues.any { identityValue ->
                    containsPhrase(
                        text = identityValue,
                        phrase = alias
                    )
                }
            }
        }

        fun record(
            representativeName: String,
            hasAnyNutrition: Boolean,
            hasUsableNutrition: Boolean
        ) {
            productMatchCount++

            if (hasAnyNutrition) {
                productWithAnyNutritionCount++
            }

            if (hasUsableNutrition) {
                productWithUsableNutritionCount++
            }

            if (
                matchedProductNames.size <
                MAXIMUM_SAMPLE_COUNT
            ) {
                matchedProductNames +=
                    representativeName.trim()
            }
        }

        fun toEntry(): OFFNutritionRawSourceCoverageEntry {

            return OFFNutritionRawSourceCoverageEntry(
                productMatchCount =
                    productMatchCount,
                productWithAnyNutritionCount =
                    productWithAnyNutritionCount,
                productWithUsableNutritionCount =
                    productWithUsableNutritionCount,
                matchedProductNames =
                    matchedProductNames.toList()
            )
        }
    }

    private data class Target(
        val catalogIndex: Int,
        val normalizedAliases: Set<String>
    )

    private data class NutritionPresence(
        val hasAnyNutrition: Boolean,
        val hasUsableNutrition: Boolean
    )

    companion object {

        private const val MAXIMUM_SAMPLE_COUNT =
            20

        private val IDENTITY_FIELDS =
            listOf(
                "product_name",
                "product_name_en",
                "product_name_de",
                "generic_name",
                "generic_name_en",
                "generic_name_de"
            )

        private val REQUIRED_CORE_DIMENSIONS =
            setOf(
                "energy",
                "fat",
                "carbohydrates",
                "proteins"
            )

        private val CORE_NUTRIENT_FIELDS =
            mapOf(
                "energy" to
                        setOf(
                            "energy-kcal_100g",
                            "energy_kcal_100g",
                            "energy_100g"
                        ),
                "fat" to
                        setOf(
                            "fat_100g"
                        ),
                "carbohydrates" to
                        setOf(
                            "carbohydrates_100g"
                        ),
                "proteins" to
                        setOf(
                            "proteins_100g"
                        ),
                "salt" to
                        setOf(
                            "salt_100g",
                            "sodium_100g"
                        )
            )
    }
}