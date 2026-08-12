package de.shopme.tools.knowledge.mapping.catalog.retrieval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File

data class SourceKnowledgeIdentityRecord(
    val serverKey: String,
    val aliases: List<String>,
    val matchAliases: List<String>,
    val artifacts: List<String>
)

class SourceKnowledgeIdentityIndexWriter(
    private val directory: File
) : Closeable {

    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()

    private val outputFile =
        directory.resolve(
            FILE_NAME
        )

    private val writer: BufferedWriter

    private var recordCount =
        0L

    init {

        if (directory.exists()) {
            require(
                directory.deleteRecursively()
            ) {
                "Could not reset source identity directory: " +
                        directory.absolutePath
            }
        }

        require(
            directory.mkdirs()
        ) {
            "Could not create source identity directory: " +
                    directory.absolutePath
        }

        writer =
            outputFile
                .bufferedWriter()
    }

    fun appendAll(
        candidates: Collection<CanonicalKnowledgeCandidate>
    ) {

        candidates.forEach(
            ::append
        )
    }

    fun append(
        candidate: CanonicalKnowledgeCandidate
    ) {

        val serverKey =
            candidate.canonicalId
                .trim()

        if (serverKey.isBlank()) {
            return
        }

        if (
            !isUsableServerKey(
                value =
                    serverKey
            )
        ) {
            return
        }

        val artifacts =
            candidate.dimensions
                .asSequence()
                .mapNotNull { dimension ->
                    artifactForDimension(
                        dimension.dimension.name
                    )
                }
                .distinct()
                .sorted()
                .toList()

        if (artifacts.isEmpty()) {
            return
        }

        val aliases =
            candidate.aliases
                .asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .filter(
                    ::isUsableAlias
                )
                .filterNot {
                    equivalentIdentity(
                        left =
                            serverKey,
                        right =
                            it
                    )
                }
                .distinct()
                .sorted()
                .toList()

        val matchAliases =
            candidate.matchAliases
                .asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .filter(
                    ::isUsableAlias
                )
                .filterNot {
                    equivalentIdentity(
                        left =
                            serverKey,
                        right =
                            it
                    )
                }
                .filterNot {
                    aliases.contains(
                        it
                    )
                }
                .distinct()
                .sorted()
                .toList()

        /*
         * Hat der Candidate keine zusätzliche Identity-Information,
         * muss er nicht in den Alias-Index.
         *
         * Der serverKey wird ohnehin direkt aus dem Server-Artefakt
         * ausgewertet.
         */
        if (
            aliases.isEmpty() &&
            matchAliases.isEmpty()
        ) {
            return
        }

        val record =
            SourceKnowledgeIdentityRecord(
                serverKey =
                    serverKey,
                aliases =
                    aliases,
                matchAliases =
                    matchAliases,
                artifacts =
                    artifacts
            )

        writer.write(
            gson.toJson(
                record
            )
        )

        writer.newLine()

        recordCount++
    }

    private fun isUsableServerKey(
        value: String
    ): Boolean {

        val normalized =
            normalize(
                value
            )

        if (
            normalized.isBlank()
        ) {
            return false
        }

        val lettersOrDigits =
            normalized.count {
                it.isLetterOrDigit()
            }

        if (
            lettersOrDigits <
            MINIMUM_SERVER_KEY_ALPHANUMERIC_COUNT
        ) {
            return false
        }

        return normalized.any {
            it.isLetter()
        }
    }

    private fun isUsableAlias(
        value: String
    ): Boolean {

        val normalized =
            normalize(
                value
            )

        if (
            normalized.isBlank()
        ) {
            return false
        }

        val lettersOrDigits =
            normalized.count {
                it.isLetterOrDigit()
            }

        return lettersOrDigits >=
                MINIMUM_ALIAS_ALPHANUMERIC_COUNT &&
                normalized.any {
                    it.isLetter()
                }
    }

    fun count(): Long =
        recordCount

    override fun close() {
        writer.close()
    }

    private fun equivalentIdentity(
        left: String,
        right: String
    ): Boolean =
        normalize(
            left
        ) ==
                normalize(
                    right
                )

    private fun normalize(
        value: String
    ): String =
        value
            .lowercase()
            .replace(
                NON_ALPHANUMERIC,
                " "
            )
            .replace(
                WHITESPACE,
                " "
            )
            .trim()

    companion object {

        private const val MINIMUM_ALIAS_ALPHANUMERIC_COUNT =
            2

        private const val MINIMUM_SERVER_KEY_ALPHANUMERIC_COUNT =
            2

        const val DIRECTORY_NAME =
            ".source-identities"

        const val FILE_NAME =
            "source-identities.jsonl"

        private val NON_ALPHANUMERIC =
            Regex(
                "[^\\p{L}\\p{N}]+"
            )

        private val WHITESPACE =
            Regex(
                "\\s+"
            )

        /*
         * String-basiert statt when(enum), damit dieser Index nicht
         * unnötig an zusätzliche/ältere Dimension-Enums gekoppelt ist.
         */
        private fun artifactForDimension(
            dimension: String
        ): String? =
            when (dimension) {

                "ALLERGENS" ->
                    "allergens.json"

                "ANIMAL_WELFARE" ->
                    "animal_welfare.json"

                "BIODIVERSITY" ->
                    "biodiversity.json"

                "DIET",
                "DIET_CLASSIFICATION" ->
                    "diet_classification.json"

                "CARBON",
                "CARBON_IMPACT",
                "ENVIRONMENTAL_IMPACT" ->
                    "environmental_impact.json"

                "FAIRTRADE",
                "FAIR_TRADE" ->
                    "fairtrade.json"

                "FOOD_MILES" ->
                    "food_miles.json"

                "TAXONOMY",
                "FOOD_TAXONOMY" ->
                    "food_taxonomy.json"

                "INGREDIENT_GRAPH" ->
                    "ingredient_graph.json"

                "INGREDIENTS" ->
                    "ingredients.json"

                "LOCALITY" ->
                    "locality.json"

                "NUTRI_SCORE",
                "NUTRISCORE" ->
                    "nutri_score.json"

                "NUTRITION" ->
                    "nutrition.json"

                "PACKAGING" ->
                    "packaging.json"

                "PESTICIDES" ->
                    "pesticides.json"

                "POLLINATOR" ->
                    "pollinator.json"

                "PROCESSING" ->
                    "processing.json"

                "PRODUCTION" ->
                    "production.json"

                "RECIPE_GRAPH" ->
                    "recipe_graph.json"

                "RECIPE",
                "RECIPES" ->
                    "recipes.json"

                "SEASONALITY" ->
                    "seasonality.json"

                "WATER",
                "WATER_FOOTPRINT" ->
                    "water_footprint.json"

                "WATER_STRESS" ->
                    "water_stress.json"

                else ->
                    null
            }
    }
}