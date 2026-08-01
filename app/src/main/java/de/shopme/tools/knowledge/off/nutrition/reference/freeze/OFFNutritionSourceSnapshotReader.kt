package de.shopme.tools.knowledge.off.nutrition.reference.freeze

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets

class OFFNutritionSourceSnapshotReader {

    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()

    fun read(
        file: File
    ): OFFNutritionSourceSnapshot {

        require(file.isFile) {
            "OFF Nutrition source snapshot does not exist: " +
                    file.absolutePath
        }

        require(file.length() > 0L) {
            "OFF Nutrition source snapshot is empty: " +
                    file.absolutePath
        }

        return file
            .reader(
                StandardCharsets.UTF_8
            )
            .use { reader ->
                gson.fromJson(
                    reader,
                    OFFNutritionSourceSnapshot::class.java
                )
            }
            ?: error(
                "Could not deserialize OFF Nutrition source snapshot: " +
                        file.absolutePath
            )
    }
}