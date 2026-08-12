package de.shopme.tools.knowledge.mapping.catalog.retrieval

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

data class CanonicalCatalogRetrievalIdentity(
    val catalogKey: String,
    val retrievalTerms: List<String>
)

class CanonicalCatalogRetrievalIdentityReader {

    fun read(
        file: File
    ): List<CanonicalCatalogRetrievalIdentity> {

        require(
            file.isFile
        ) {
            "Canonical catalog does not exist: " +
                    file.absolutePath
        }

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(
            root.isJsonArray
        ) {
            "Canonical catalog root must be an array."
        }

        val identities =
            root
                .asJsonArray
                .map { element ->

                    val json =
                        element.asJsonObject

                    val catalogKey =
                        json.requiredString(
                            "normalized"
                        )

                    val retrievalTerms =
                        buildSet {

                            /*
                             * Authority bleibt immer an erster Stelle.
                             */
                            add(
                                catalogKey
                            )

                            json.optionalString(
                                "itemname"
                            )
                                ?.let(::add)

                            /*
                             * Wichtigster neue Recall-Hebel:
                             * Der finale Katalog besitzt bereits
                             * hochwertige englische Identitäten.
                             */
                            json.optionalString(
                                "normalizedEnglish"
                            )
                                ?.let(::add)

                            json.optionalString(
                                "plural"
                            )
                                ?.let(::add)

                            json.stringArray(
                                "colloquial"
                            )
                                .filter(
                                    ::isSafeCanonicalAlias
                                )
                                .forEach(
                                    ::add
                                )

                            /*
                             * Nur vollständige/ausreichend spezifische
                             * Autocomplete-Ausdrücke übernehmen.
                             *
                             * Einzelne generische Varianten wie
                             * "standard", "fresh", "cooked" etc.
                             * dürfen keinen Retrieval-Recall erzeugen.
                             */
                            json.stringArray(
                                "autocompleteTokens"
                            )
                                .filter { token ->
                                    isSafeAutocompleteTerm(
                                        token =
                                            token,
                                        catalogKey =
                                            catalogKey
                                    )
                                }
                                .forEach(
                                    ::add
                                )
                        }
                            .asSequence()
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .toList()

                    CanonicalCatalogRetrievalIdentity(
                        catalogKey =
                            catalogKey,
                        retrievalTerms =
                            retrievalTerms
                    )
                }
                .sortedBy {
                    it.catalogKey
                }

        require(
            identities
                .map {
                    it.catalogKey
                }
                .distinct()
                .size ==
                    identities.size
        ) {
            "Canonical catalog contains duplicate normalized keys."
        }

        return identities
    }

    private fun JsonObject.requiredString(
        key: String
    ): String =
        optionalString(
            key
        )
            ?: error(
                "Missing or blank '$key' in canonical catalog."
            )

    private fun JsonObject.optionalString(
        key: String
    ): String? =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive
            }
            ?.asString
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )

    private fun JsonObject.stringArray(
        key: String
    ): List<String> =
        get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?.mapNotNull { element ->

                element
                    .takeIf {
                        it.isJsonPrimitive
                    }
                    ?.asString
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    )
            }
            .orEmpty()

    private fun isSafeCanonicalAlias(
        value: String
    ): Boolean {

        val normalized =
            value
                .trim()
                .lowercase()

        if (
            normalized.length <
            MINIMUM_ALIAS_LENGTH
        ) {
            return false
        }

        return normalized !in
                GENERIC_ALIAS_TERMS
    }

    private fun isSafeAutocompleteTerm(
        token: String,
        catalogKey: String
    ): Boolean {

        val normalized =
            token
                .trim()
                .lowercase()

        if (
            normalized ==
            catalogKey.lowercase()
        ) {
            return true
        }

        if (
            normalized.length <
            MINIMUM_ALIAS_LENGTH
        ) {
            return false
        }

        if (
            normalized in
            GENERIC_ALIAS_TERMS
        ) {
            return false
        }

        /*
         * Zusammengesetzte Begriffe bzw. mehrteilige Ausdrücke sind
         * wesentlich sicherer als einzelne Variant-Wörter.
         */
        return normalized.contains("-") ||
                normalized.contains(" ") ||
                normalized.length >=
                MINIMUM_SAFE_SINGLE_TOKEN_LENGTH
    }

    companion object {

        private const val MINIMUM_ALIAS_LENGTH =
            4

        private const val MINIMUM_SAFE_SINGLE_TOKEN_LENGTH =
            8

        private val GENERIC_ALIAS_TERMS =
            setOf(
                "standard",
                "classic",
                "klassisch",
                "fresh",
                "frisch",
                "frozen",
                "gefroren",
                "tiefgekuehlt",
                "raw",
                "roh",
                "cooked",
                "gekocht",
                "dried",
                "getrocknet",
                "bio",
                "vegan",
                "vegetarian",
                "vegetarisch",
                "food",
                "product",
                "meal",
                "dish"
            )
    }
}