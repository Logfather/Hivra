package de.shopme.tools.knowledge.off.nutrition.reference.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import java.io.BufferedInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

class OFFAcceptedNutritionReferenceReader(
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()
) {

    fun forEachReference(
        inputFile: File,
        consumer:
            (CanonicalOFFNutritionReferenceCandidate) -> Unit
    ): Long {

        require(inputFile.isFile) {
            "OFF accepted nutrition reference file does not exist: " +
                    inputFile.absolutePath
        }

        var referenceCount =
            0L

        GZIPInputStream(
            BufferedInputStream(
                inputFile.inputStream()
            )
        ).bufferedReader(
            StandardCharsets.UTF_8
        ).useLines { lines ->

            lines.forEachIndexed { index, line ->

                require(line.isNotBlank()) {
                    "Blank JSONL record at line ${index + 1}."
                }

                val reference =
                    requireNotNull(
                        gson.fromJson(
                            line,
                            CanonicalOFFNutritionReferenceCandidate::class.java
                        )
                    ) {
                        "Empty reference at line ${index + 1}."
                    }

                consumer(reference)
                referenceCount++
            }
        }

        return referenceCount
    }
}