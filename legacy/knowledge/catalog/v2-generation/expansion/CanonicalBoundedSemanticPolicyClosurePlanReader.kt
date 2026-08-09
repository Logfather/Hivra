package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import com.google.gson.Gson
import java.io.File

class CanonicalBoundedSemanticPolicyClosurePlanReader(
    private val gson: Gson =
        Gson()
) {

    fun read(
        inputFile:
        File
    ): CanonicalBoundedSemanticPolicyClosurePlan {
        require(inputFile.isFile) {
            "Bounded semantic-policy closure plan is missing: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L) {
            "Bounded semantic-policy closure plan is empty: " +
                    inputFile.absolutePath
        }

        val result =
            inputFile
                .reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalBoundedSemanticPolicyClosurePlan::class.java
                    )
                }

        require(result.valid) {
            "Bounded semantic-policy closure plan is invalid."
        }

        return result
    }
}