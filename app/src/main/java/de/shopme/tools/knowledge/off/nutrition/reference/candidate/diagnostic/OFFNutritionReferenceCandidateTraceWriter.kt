package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic

import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionReferenceCandidateTraceWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        traces: List<OFFNutritionReferenceCandidateTrace>,
        outputFile: File
    ) {

        val duplicateSourceProductIds =
            traces
                .groupingBy(
                    OFFNutritionReferenceCandidateTrace::sourceProductId
                )
                .eachCount()
                .filterValues { it > 1 }
                .keys
                .sorted()

        require(
            duplicateSourceProductIds.isEmpty()
        ) {
            "Duplicate trace sourceProductIds: " +
                    duplicateSourceProductIds.joinToString()
        }

        val deterministicTraces =
            traces.sortedWith(
                compareBy<OFFNutritionReferenceCandidateTrace>(
                    { it.sourceProductId },
                    { it.productName },
                    {
                        it.normalizedProductIdentities
                            .joinToString("\u0000")
                    }
                )
            )

        outputFile.parentFile?.mkdirs()

        require(
            outputFile.parentFile?.isDirectory == true
        ) {
            "Could not create trace directory: " +
                    outputFile.parentFile?.absolutePath
        }

        outputFile.writeText(
            gson.toJson(
                TraceFile(
                    version =
                        1,
                    traceCount =
                        deterministicTraces.size,
                    traces =
                        deterministicTraces
                )
            ) + "\n",
            Charsets.UTF_8
        )
    }

    private data class TraceFile(
        val version: Int,
        val traceCount: Int,
        val traces: List<OFFNutritionReferenceCandidateTrace>
    )
}