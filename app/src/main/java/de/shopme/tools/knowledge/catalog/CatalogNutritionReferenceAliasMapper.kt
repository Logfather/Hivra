package de.shopme.tools.knowledge.catalog

class CatalogNutritionReferenceAliasMapper(
    aliases: Map<String, String> = emptyMap()
) {

    private val aliasesByNormalizedKey: Map<String, String> =
        aliases
            .mapKeys { (alias, _) ->
                normalizeKey(alias)
            }
            .mapValues { (_, reference) ->
                normalizeKey(reference)
            }
            .toSortedMap()

    fun hasAlias(
        value: String
    ): Boolean =
        normalizeKey(value) in aliasesByNormalizedKey

    fun map(
        value: String
    ): String {
        val normalizedValue = normalizeKey(value)

        return aliasesByNormalizedKey[normalizedValue]
            ?: normalizedValue
                .takeIf(String::isNotBlank)
            ?: UNKNOWN_REFERENCE
    }

    private fun normalizeKey(
        value: String
    ): String =
        value
            .trim()
            .lowercase()
            .replace(UMLAUT_A_REGEX, "ae")
            .replace(UMLAUT_O_REGEX, "oe")
            .replace(UMLAUT_U_REGEX, "ue")
            .replace("ß", "ss")
            .replace(NON_ALPHANUMERIC_REGEX, "-")
            .replace(MULTIPLE_HYPHEN_REGEX, "-")
            .trim('-')

    private companion object {

        const val UNKNOWN_REFERENCE = "unknown"

        val UMLAUT_A_REGEX = Regex("[äÄ]")
        val UMLAUT_O_REGEX = Regex("[öÖ]")
        val UMLAUT_U_REGEX = Regex("[üÜ]")
        val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]+")
        val MULTIPLE_HYPHEN_REGEX = Regex("-{2,}")
    }
}