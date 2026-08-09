package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

import java.text.Normalizer
import java.util.Locale

object CanonicalExpansionCandidateNaming {

    fun candidateKey(
        category: String,
        familyKey: String,
        variantValues:
        List<CanonicalExpansionCandidateVariantValue>
    ): String =
        normalizeKey(
            buildString {
                append(category)
                append("-")
                append(familyKey)

                variantValues
                    .sortedBy {
                        it.axis.name
                    }
                    .forEach { value ->
                        append("-")
                        append(value.valueKey)
                    }
            }
        )

    fun proposedCanonicalName(
        familyDisplayName: String,
        variantValues:
        List<CanonicalExpansionCandidateVariantValue>
    ): String =
        buildString {
            append(familyDisplayName.trim())

            variantValues
                .sortedBy {
                    it.axis.name
                }
                .forEach { value ->
                    append(" – ")
                    append(value.displayName.trim())
                }
        }

    fun proposedNormalizedKey(
        familyKey: String,
        variantValues:
        List<CanonicalExpansionCandidateVariantValue>
    ): String =
        normalizeKey(
            buildString {
                append(familyKey)

                variantValues
                    .sortedBy {
                        it.axis.name
                    }
                    .forEach { value ->
                        append("-")
                        append(value.valueKey)
                    }
            }
        )

    private fun normalizeKey(
        value: String
    ): String =
        Normalizer
            .normalize(
                value.lowercase(Locale.ROOT),
                Normalizer.Form.NFD
            )
            .replace(
                Regex("\\p{M}+"),
                ""
            )
            .replace("ß", "ss")
            .replace(
                Regex("[^a-z0-9]+"),
                "-"
            )
            .trim('-')
            .replace(
                Regex("-+"),
                "-"
            )
}