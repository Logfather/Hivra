package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ResultingNutritionConflictPolicyReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        policy: ResultingNutritionConflictPolicy,
        decision: ResultingNutritionConflictPolicyDecision,
        outputFile: File
    ): File {

        outputFile.parentFile?.let { parent ->
            require(
                parent.mkdirs() ||
                        parent.isDirectory
            ) {
                "Could not create Nutrition conflict policy report " +
                        "directory: ${parent.absolutePath}"
            }
        }

        val report =
            ResultingNutritionConflictPolicyReport(
                version =
                    ResultingNutritionConflictPolicyReport
                        .CURRENT_VERSION,
                policy =
                    policy,
                decision =
                    decision
            )

        val temporaryFile =
            outputFile.resolveSibling(
                outputFile.name +
                        ".tmp"
            )

        temporaryFile.writeText(
            gson.toJson(
                report
            ) + "\n",
            StandardCharsets.UTF_8
        )

        runCatching {
            Files.move(
                temporaryFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }
            .getOrElse {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }

        return outputFile
    }
}

data class ResultingNutritionConflictPolicyReport(
    val version: Int,
    val policy: ResultingNutritionConflictPolicy,
    val decision: ResultingNutritionConflictPolicyDecision
) {

    companion object {

        const val CURRENT_VERSION =
            1
    }
}