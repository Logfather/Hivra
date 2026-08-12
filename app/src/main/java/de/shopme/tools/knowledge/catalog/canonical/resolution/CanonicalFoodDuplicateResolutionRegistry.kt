package de.shopme.tools.knowledge.catalog.canonical.resolution

enum class DuplicateResolutionType {
    MERGE,
    REJECT_GROUP
}

data class CanonicalDuplicateResolution(
    val type: DuplicateResolutionType,
    val canonicalItemname: String?,
    val canonicalNormalized: String?,
    val canonicalCategory: String?
)

object CanonicalFoodDuplicateResolutionRegistry {

    private val resolutions =
        mapOf(

            key(
                "Apfelsaft Schorle",
                "Apfelschorle"
            ) to
                    merge(
                        itemname =
                            "Apfelschorle",
                        normalized =
                            "apfelschorle",
                        category =
                            "beverages"
                    ),

            key(
                "Basmati Reis",
                "Basmatireis"
            ) to
                    merge(
                        itemname =
                            "Basmatireis",
                        normalized =
                            "basmatireis",
                        category =
                            "rice"
                    ),

            /*
             * Kein sinnvoller kanonischer Oberbegriff.
             */
            key(
                "Bohnen-Mix",
                "Bohnenmix"
            ) to
                    CanonicalDuplicateResolution(
                        type =
                            DuplicateResolutionType.REJECT_GROUP,
                        canonicalItemname =
                            null,
                        canonicalNormalized =
                            null,
                        canonicalCategory =
                            null
                    ),

            key(
                "Bratwurst",
                "Bratwürste"
            ) to
                    merge(
                        itemname =
                            "Bratwürste",
                        normalized =
                            "bratwuerste",
                        category =
                            "sausage"
                    ),

            key(
                "Ciabatta",
                "Ciabatta Brot"
            ) to
                    merge(
                        itemname =
                            "Ciabatta",
                        normalized =
                            "ciabatta",
                        category =
                            "bakery"
                    ),

            key(
                "Cranberry",
                "Cranberries"
            ) to
                    merge(
                        itemname =
                            "Cranberries",
                        normalized =
                            "cranberries",
                        category =
                            "fruit"
                    ),

            key(
                "Croissant",
                "Croissants"
            ) to
                    merge(
                        itemname =
                            "Croissants",
                        normalized =
                            "croissants",
                        category =
                            "bakery"
                    ),

            key(
                "Dattel",
                "Datteln"
            ) to
                    merge(
                        itemname =
                            "Datteln",
                        normalized =
                            "datteln",
                        category =
                            "fruit"
                    ),

            key(
                "Erdbeere",
                "Erdbeeren"
            ) to
                    merge(
                        itemname =
                            "Erdbeeren",
                        normalized =
                            "erdbeeren",
                        category =
                            "fruit"
                    ),

            key(
                "Feige",
                "Feigen"
            ) to
                    merge(
                        itemname =
                            "Feigen",
                        normalized =
                            "feigen",
                        category =
                            "fruit"
                    ),

            key(
                "Gemüse Lasagne",
                "Gemüselasagne"
            ) to
                    merge(
                        itemname =
                            "Gemüselasagne",
                        normalized =
                            "gemueselasagne",
                        category =
                            "ready-meals"
                    ),

            key(
                "Heidelbeere",
                "Heidelbeeren"
            ) to
                    merge(
                        itemname =
                            "Heidelbeeren",
                        normalized =
                            "heidelbeeren",
                        category =
                            "fruit"
                    ),

            key(
                "Himbeere",
                "Himbeeren"
            ) to
                    merge(
                        itemname =
                            "Himbeeren",
                        normalized =
                            "himbeeren",
                        category =
                            "fruit"
                    ),

            key(
                "Kirsche",
                "Kirschen"
            ) to
                    merge(
                        itemname =
                            "Kirschen",
                        normalized =
                            "kirschen",
                        category =
                            "fruit"
                    ),

            key(
                "Lachs Scheiben",
                "Lachsscheiben"
            ) to
                    merge(
                        itemname =
                            "Lachsscheiben",
                        normalized =
                            "lachsscheiben",
                        category =
                            "fish"
                    ),

            key(
                "Soja Joghurt",
                "Sojajoghurt"
            ) to
                    merge(
                        itemname =
                            "Sojajoghurt",
                        normalized =
                            "sojajoghurt",
                        category =
                            "plant-based-alternatives"
                    ),

            key(
                "Tomaten Ketchup",
                "Tomatenketchup"
            ) to
                    merge(
                        itemname =
                            "Tomatenketchup",
                        normalized =
                            "tomatenketchup",
                        category =
                            "sauces"
                    )
        )

    fun find(
        itemnames: Collection<String>
    ): CanonicalDuplicateResolution? =
        resolutions[
            itemnames
                .map(
                    String::trim
                )
                .sorted()
                .joinToString(
                    separator =
                        "\u0000"
                )
        ]

    private fun key(
        vararg names: String
    ): String =
        names
            .map(
                String::trim
            )
            .sorted()
            .joinToString(
                separator =
                    "\u0000"
            )

    private fun merge(
        itemname: String,
        normalized: String,
        category: String
    ): CanonicalDuplicateResolution =
        CanonicalDuplicateResolution(
            type =
                DuplicateResolutionType.MERGE,
            canonicalItemname =
                itemname,
            canonicalNormalized =
                normalized,
            canonicalCategory =
                category
        )
}