package de.shopme.tools.knowledge.off.nutrition.reference.quality.policy

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets

class OFFNutritionReferenceQualityPolicyReader {

    private val gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()

    fun read(
        inputFile: File
    ): OFFNutritionReferenceQualityPolicy {

        val canonicalInputFile =
            inputFile.canonicalFile

        require(canonicalInputFile.isFile) {
            "OFF nutrition quality policy does not exist: " +
                    canonicalInputFile.absolutePath
        }

        val content =
            canonicalInputFile.readText(
                charset =
                    StandardCharsets.UTF_8
            )

        val policy:
                OFFNutritionReferenceQualityPolicy =
            requireNotNull(
                gson.fromJson(
                    content,
                    OFFNutritionReferenceQualityPolicy::class.java
                )
            ) {
                "OFF nutrition quality policy is empty."
            }

        require(
            policy.version ==
                    OFFNutritionReferenceQualityPolicy.CURRENT_VERSION
        ) {
            "Unsupported OFF nutrition quality policy version: " +
                    policy.version
        }

        return policy
    }
}