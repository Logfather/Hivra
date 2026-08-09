package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalFamilyAxisSemanticPolicySetReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalFamilyAxisSemanticPolicySet {
        require(inputFile.isFile) {
            "Semantic policy file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L) {
            "Semantic policy file is empty: " +
                    inputFile.absolutePath
        }

        val result =
            requireNotNull(
                inputFile.reader(Charsets.UTF_8)
                    .use { reader ->
                        gson.fromJson(
                            reader,
                            CanonicalFamilyAxisSemanticPolicySet::
                            class.java
                        )
                    }
            ) {
                "Could not parse semantic policy set."
            }

        require(result.valid) {
            "Semantic policy set is invalid."
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