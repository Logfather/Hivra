package de.shopme.tools.knowledge.off.nutrition.reference.validation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Validiert das persistierte finale OFF-Nutrition-Aggregate-Dataset
 * speicherbegrenzt.
 *
 * Es wird immer nur ein Aggregate gleichzeitig deserialisiert.
 *
 * Fehlerfreie Aggregate werden nicht im Result gehalten. Persistiert werden
 * ausschließlich Warning- und Rejected-Einträge sowie globale Zähler.
 */
class OFFNutritionReferenceAggregateDatasetValidator(
    private val aggregateValidator:
    OFFNutritionReferenceAggregateValidator =
        OFFNutritionReferenceAggregateValidator(),
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()
) {

    fun validate(
        inputFile: File
    ): OFFNutritionReferenceAggregateDatasetValidationResult {

        require(inputFile.isFile) {
            "OFF nutrition aggregate dataset does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L) {
            "OFF nutrition aggregate dataset is empty: " +
                    inputFile.absolutePath
        }

        var inputAggregateCount =
            0

        var acceptedAggregateCount =
            0

        var warningAggregateCount =
            0

        var rejectedAggregateCount =
            0

        var totalProfileCount =
            0L

        var totalWarningCount =
            0

        var totalErrorCount =
            0

        var previousCanonicalId: String? =
            null

        val issueCountsByType =
            sortedMapOf<
                    OFFNutritionReferenceAggregateIssueType,
                    Int
                    >(
                compareBy { type ->
                    type.name
                }
            )

        val warningEntries =
            mutableListOf<
                    OFFNutritionReferenceAggregateValidationEntry
                    >()

        val rejectedEntries =
            mutableListOf<
                    OFFNutritionReferenceAggregateValidationEntry
                    >()

        JsonReader(
            inputFile.bufferedReader(
                StandardCharsets.UTF_8
            )
        ).use { reader ->

            require(
                reader.peek() ==
                        JsonToken.BEGIN_ARRAY
            ) {
                "OFF nutrition aggregate dataset must be a JSON array."
            }

            reader.beginArray()

            while (reader.hasNext()) {
                val aggregate:
                        CanonicalOFFNutritionReferenceAggregate =
                    requireNotNull(
                        gson.fromJson(
                            reader,
                            CanonicalOFFNutritionReferenceAggregate::class.java
                        )
                    ) {
                        "OFF nutrition aggregate dataset contains a null entry."
                    }

                validateGlobalOrder(
                    previousCanonicalId =
                        previousCanonicalId,
                    currentCanonicalId =
                        aggregate.canonicalId
                )

                val entry =
                    aggregateValidator.validateAggregate(
                        aggregate =
                            aggregate
                    )

                inputAggregateCount++

                totalProfileCount +=
                    aggregate.profileCount.toLong()

                totalWarningCount +=
                    entry.warningCount

                totalErrorCount +=
                    entry.errorCount

                entry.issues.forEach { issue ->
                    issueCountsByType[issue.type] =
                        issueCountsByType.getOrDefault(
                            issue.type,
                            0
                        ) + 1
                }

                when (entry.status) {
                    OFFNutritionReferenceAggregateValidationStatus.ACCEPTED -> {
                        acceptedAggregateCount++
                    }

                    OFFNutritionReferenceAggregateValidationStatus.WARNING -> {
                        warningAggregateCount++

                        warningEntries +=
                            entry
                    }

                    OFFNutritionReferenceAggregateValidationStatus.REJECTED -> {
                        rejectedAggregateCount++

                        rejectedEntries +=
                            entry
                    }
                }

                previousCanonicalId =
                    aggregate.canonicalId
            }

            reader.endArray()

            require(
                reader.peek() ==
                        JsonToken.END_DOCUMENT
            ) {
                "Unexpected content after aggregate dataset JSON array."
            }
        }

        return OFFNutritionReferenceAggregateDatasetValidationResult(
            inputAggregateCount =
                inputAggregateCount,
            acceptedAggregateCount =
                acceptedAggregateCount,
            warningAggregateCount =
                warningAggregateCount,
            rejectedAggregateCount =
                rejectedAggregateCount,
            totalProfileCount =
                totalProfileCount,
            totalWarningCount =
                totalWarningCount,
            totalErrorCount =
                totalErrorCount,
            issueCountsByType =
                issueCountsByType,
            warningEntries =
                warningEntries,
            rejectedEntries =
                rejectedEntries
        )
    }

    private fun validateGlobalOrder(
        previousCanonicalId: String?,
        currentCanonicalId: String
    ) {
        if (previousCanonicalId == null) {
            return
        }

        require(
            previousCanonicalId <
                    currentCanonicalId
        ) {
            when {
                previousCanonicalId ==
                        currentCanonicalId -> {

                    "Aggregate dataset contains duplicate canonicalId: " +
                            currentCanonicalId
                }

                else -> {
                    "Aggregate dataset is not deterministically sorted: " +
                            "previous=$previousCanonicalId, " +
                            "current=$currentCanonicalId"
                }
            }
        }
    }
}