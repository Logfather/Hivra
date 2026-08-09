package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalSemanticPolicyBatchImpactBaselineReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalSemanticPolicyBatchImpactBaseline {
        require(inputFile.isFile) {
            "Impact baseline does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        val result =
            requireNotNull(
                inputFile.reader(Charsets.UTF_8)
                    .use { reader ->
                        gson.fromJson(
                            reader,
                            CanonicalSemanticPolicyBatchImpactBaseline::
                            class.java
                        )
                    }
            )

        require(result.valid)

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