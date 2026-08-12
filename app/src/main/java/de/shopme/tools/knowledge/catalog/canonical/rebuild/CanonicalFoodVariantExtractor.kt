package de.shopme.tools.knowledge.catalog.canonical.rebuild

class CanonicalFoodVariantExtractor {

    fun extract(
        sourceNames: Collection<String>
    ): List<String> {

        val variants =
            linkedSetOf<String>()

        sourceNames
            .forEach { name ->

                VARIANTS
                    .forEach { rule ->

                        if (
                            rule.pattern
                                .containsMatchIn(
                                    name
                                )
                        ) {
                            variants +=
                                rule.variant
                        }
                    }
            }

        return variants
            .sorted()
    }

    private data class VariantRule(
        val variant: String,
        val pattern: Regex
    )

    companion object {

        private val VARIANTS =
            listOf(
                VariantRule(
                    variant =
                        "tiefgekühlt",
                    pattern =
                        Regex(
                            """(?i)\b(tk|tiefkühl|tiefgekühlt|tiefgekuehlt|gefroren)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        "geräuchert",
                    pattern =
                        Regex(
                            """(?i)\b(geräuchert|geraeuchert|rauch)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        "getrocknet",
                    pattern =
                        Regex(
                            """(?i)\b(getrocknet|trocken|dried)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        "konserviert",
                    pattern =
                        Regex(
                            """(?i)\b(konserviert|konserve|dose|dosen)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        "naturtrüb",
                    pattern =
                        Regex(
                            """(?i)\b(naturtrüb|naturtrueb|cloudy)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        "aus Konzentrat",
                    pattern =
                        Regex(
                            """(?i)\b(aus konzentrat|from concentrate)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        "gemahlen",
                    pattern =
                        Regex(
                            """(?i)\b(gemahlen|ground)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        "geschnitten",
                    pattern =
                        Regex(
                            """(?i)\b(geschnitten|sliced)\b"""
                        )
                )
            )
    }
}