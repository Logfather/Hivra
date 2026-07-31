package de.shopme.tools.knowledge.off.nutrition.reference.aggregation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.MalformedJsonException
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Liest das persistierte OFF-Nutrition-Aggregate-Dataset
 * speicherbegrenzt.
 *
 * Das Dataset muss ein deterministisch nach canonicalId sortiertes
 * JSON-Array enthalten.
 *
 * Während des Lesens wird immer nur ein Aggregate gleichzeitig
 * deserialisiert.
 */
class OFFNutritionReferenceAggregateDatasetReader(
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()
) {

    /**
     * Liest das Dataset streamingbasiert und übergibt jedes Aggregate
     * unmittelbar an den Consumer.
     *
     * @param inputFile Persistiertes Aggregate-Dataset als JSON-Array.
     * @param maxAggregates Optionales Verarbeitungslimit.
     * @param consumer Consumer für jedes gelesene Aggregate.
     *
     * @return Anzahl der tatsächlich an den Consumer übergebenen Aggregate.
     */
    fun forEachAggregate(
        inputFile: File,
        maxAggregates: Int? = null,
        consumer:
            (CanonicalOFFNutritionReferenceAggregate) -> Unit
    ): Int {

        validateInput(
            inputFile =
                inputFile,
            maxAggregates =
                maxAggregates
        )

        var aggregateCount =
            0

        var previousCanonicalId: String? =
            null

        JsonReader(
            inputFile.bufferedReader(
                StandardCharsets.UTF_8
            )
        ).use { reader ->

            require(
                reader.peek() ==
                        JsonToken.BEGIN_ARRAY
            ) {
                "OFF nutrition aggregate dataset must be a JSON array: " +
                        inputFile.absolutePath
            }

            reader.beginArray()

            while (
                reader.hasNext() &&
                (
                        maxAggregates == null ||
                                aggregateCount < maxAggregates
                        )
            ) {
                val aggregate =
                    readAggregate(
                        reader =
                            reader
                    )

                validateGlobalOrder(
                    previousCanonicalId =
                        previousCanonicalId,
                    currentCanonicalId =
                        aggregate.canonicalId
                )

                consumer(
                    aggregate
                )

                aggregateCount++

                previousCanonicalId =
                    aggregate.canonicalId
            }

            if (
                maxAggregates == null ||
                !reader.hasNext()
            ) {
                reader.endArray()

                validateEndOfDocument(
                    reader =
                        reader,
                    inputFile =
                        inputFile
                )
            }
        }

        return aggregateCount
    }

    private fun validateEndOfDocument(
        reader: JsonReader,
        inputFile: File
    ) {

        val errorMessage =
            "Unexpected content after OFF nutrition " +
                    "aggregate dataset JSON array: " +
                    inputFile.absolutePath

        try {
            require(
                reader.peek() ==
                        JsonToken.END_DOCUMENT
            ) {
                errorMessage
            }
        } catch (
            exception: MalformedJsonException
        ) {
            throw IllegalArgumentException(
                errorMessage,
                exception
            )
        }
    }

    /**
     * Komfortfunktion für kleine Datasets und Unit-Tests.
     *
     * Für produktive vollständige Datasets sollte forEachAggregate
     * verwendet werden.
     */
    fun read(
        inputFile: File,
        maxAggregates: Int? = null
    ): List<CanonicalOFFNutritionReferenceAggregate> {

        val aggregates =
            mutableListOf<
                    CanonicalOFFNutritionReferenceAggregate
                    >()

        forEachAggregate(
            inputFile =
                inputFile,
            maxAggregates =
                maxAggregates
        ) { aggregate ->
            aggregates +=
                aggregate
        }

        return aggregates
    }

    private fun readAggregate(
        reader: JsonReader
    ): CanonicalOFFNutritionReferenceAggregate {

        require(
            reader.peek() !=
                    JsonToken.NULL
        ) {
            "OFF nutrition aggregate dataset contains a null entry."
        }

        return requireNotNull(
            gson.fromJson(
                reader,
                CanonicalOFFNutritionReferenceAggregate::class.java
            )
        ) {
            "OFF nutrition aggregate dataset contains a null entry."
        }
    }

    private fun validateInput(
        inputFile: File,
        maxAggregates: Int?
    ) {

        require(inputFile.isFile) {
            "OFF nutrition aggregate dataset does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L) {
            "OFF nutrition aggregate dataset is empty: " +
                    inputFile.absolutePath
        }

        require(
            maxAggregates == null ||
                    maxAggregates >= 0
        ) {
            "maxAggregates must not be negative."
        }
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

                    "OFF nutrition aggregate dataset contains " +
                            "duplicate canonicalId: " +
                            currentCanonicalId
                }

                else -> {
                    "OFF nutrition aggregate dataset is not " +
                            "deterministically sorted: " +
                            "previous=$previousCanonicalId, " +
                            "current=$currentCanonicalId"
                }
            }
        }
    }
}