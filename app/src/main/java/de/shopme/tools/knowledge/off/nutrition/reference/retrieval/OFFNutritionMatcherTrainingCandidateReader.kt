package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.shopme.tools.knowledge.off.nutrition.reference.training.OFFNutritionMatcherTrainingCandidate
import java.io.File

class OFFNutritionMatcherTrainingCandidateReader(
    private val gson: Gson =
        Gson()
) {

    fun read(
        inputFile: File
    ): List<OFFNutritionMatcherTrainingCandidate> {

        require(inputFile.isFile) {
            "OFF matcher candidate file not found: " +
                    inputFile.absolutePath
        }

        val type =
            object :
                TypeToken<List<OFFNutritionMatcherTrainingCandidate>>() {
            }.type

        val candidates:
                List<OFFNutritionMatcherTrainingCandidate> =
            inputFile
                .reader()
                .use { reader ->
                    gson.fromJson(reader, type)
                }

        require(
            candidates ==
                    candidates.sortedBy {
                        it.serverKey
                    }
        ) {
            "OFF matcher candidates must be sorted by serverKey."
        }

        require(
            candidates.map { it.serverKey }.distinct().size ==
                    candidates.size
        ) {
            "OFF matcher candidates contain duplicate server keys."
        }

        return candidates
    }
}