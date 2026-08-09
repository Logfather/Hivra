package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalSemanticPolicyBatchImpactAnalysisReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalSemanticPolicyBatchImpactAnalysis {
        require(inputFile.isFile) {
            "Semantic policy batch impact analysis does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L) {
            "Semantic policy batch impact analysis is empty: " +
                    inputFile.absolutePath
        }

        val result =
            requireNotNull(
                inputFile.reader(Charsets.UTF_8)
                    .use { reader ->
                        gson.fromJson(
                            reader,
                            CanonicalSemanticPolicyBatchImpactAnalysis::
                            class.java
                        )
                    }
            )

        require(result.valid) {
            "Persisted semantic policy batch impact analysis is invalid: " +
                    result.blockers.joinToString()
        }

        return result
    }

    private companion object {

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
    }
}