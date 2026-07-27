package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

class OFFNutritionRetrievalTermExpander {

    fun expand(
        terms: List<String>
    ): List<String> {

        return terms
            .asSequence()
            .flatMap { term ->
                expandTerm(
                    term = term
                ).asSequence()
            }
            .map(
                OFFNutritionRetrievalTextNormalizer::normalize
            )
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
            .toList()
    }

    private fun expandTerm(
        term: String
    ): Set<String> {

        val normalized =
            OFFNutritionRetrievalTextNormalizer
                .normalize(term)

        if (normalized.isBlank()) {
            return emptySet()
        }

        val result =
            linkedSetOf(
                normalized
            )

        singularize(normalized)
            ?.let(result::add)

        pluralize(normalized)
            ?.let(result::add)

        val tokens =
            normalized
                .split(' ')
                .filter(String::isNotBlank)

        if (tokens.size > 1) {
            val lastToken =
                tokens.last()

            singularize(lastToken)
                ?.let { singularLastToken ->
                    result +=
                        (
                                tokens.dropLast(1) +
                                        singularLastToken
                                )
                            .joinToString(" ")
                }

            pluralize(lastToken)
                ?.let { pluralLastToken ->
                    result +=
                        (
                                tokens.dropLast(1) +
                                        pluralLastToken
                                )
                            .joinToString(" ")
                }
        }

        return result
    }

    private fun singularize(
        value: String
    ): String? {

        return when {
            value.endsWith("ies") &&
                    value.length > 3 -> {
                value.dropLast(3) + "y"
            }

            value.endsWith("oes") &&
                    value.length > 3 -> {
                value.dropLast(2)
            }

            value.endsWith("ses") &&
                    value.length > 3 -> {
                value.dropLast(2)
            }

            value.endsWith("xes") &&
                    value.length > 3 -> {
                value.dropLast(2)
            }

            value.endsWith("ches") &&
                    value.length > 4 -> {
                value.dropLast(2)
            }

            value.endsWith("shes") &&
                    value.length > 4 -> {
                value.dropLast(2)
            }

            value.endsWith("s") &&
                    !value.endsWith("ss") &&
                    value.length > 2 -> {
                value.dropLast(1)
            }

            else -> {
                null
            }
        }
    }

    private fun pluralize(
        value: String
    ): String? {

        return when {
            value.endsWith("s") -> {
                null
            }

            value.endsWith("y") &&
                    value.length > 1 &&
                    value[value.lastIndex - 1] !in VOWELS -> {
                value.dropLast(1) + "ies"
            }

            value.endsWith("ch") ||
                    value.endsWith("sh") ||
                    value.endsWith("x") ||
                    value.endsWith("s") -> {
                value + "es"
            }

            value.endsWith("o") -> {
                value + "es"
            }

            else -> {
                value + "s"
            }
        }
    }

    companion object {

        private val VOWELS =
            setOf(
                'a',
                'e',
                'i',
                'o',
                'u'
            )
    }
}