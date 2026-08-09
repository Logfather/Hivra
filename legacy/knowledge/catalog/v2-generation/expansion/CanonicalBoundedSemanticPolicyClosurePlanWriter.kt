package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalBoundedSemanticPolicyClosurePlanWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        plan:
        CanonicalBoundedSemanticPolicyClosurePlan,

        outputFile:
        File
    ) {
        require(plan.valid)

        outputFile.parentFile?.mkdirs()

        val temporaryFile =
            File(
                outputFile.parentFile,
                "${outputFile.name}.tmp"
            )

        temporaryFile.writeText(
            gson.toJson(plan) +
                    System.lineSeparator(),

            Charsets.UTF_8
        )

        check(
            temporaryFile.renameTo(
                outputFile
            )
        ) {
            "Could not atomically persist bounded closure plan to: " +
                    outputFile.absolutePath
        }
    }
}