package de.shopme.testing.system.tools.knowledge.catalog.semantic.variant

import java.text.Normalizer

object SemanticVariantTaxonomyRegistry {

    private val COMBINING_MARKS_REGEX =
        Regex("\\p{M}+")

    private val NON_ALPHANUMERIC_REGEX =
        Regex("[^a-z0-9]+")

    private val WHITESPACE_REGEX =
        Regex("\\s+")

    private val definitions: List<SemanticVariantDefinition> =
        listOf(

            // ---------------------------------------------------------
            // INGREDIENT
            // ---------------------------------------------------------

            definition(
                canonicalKey = "amaranth",
                displayName = "Amaranth",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "buckwheat",
                displayName = "Buchweizen",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "einkorn",
                displayName = "Einkorn",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "barley",
                displayName = "Gerste",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "durum-wheat",
                displayName = "Hartweizen",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "corn",
                displayName = "Mais",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "almond",
                displayName = "Mandel",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "apple",
                displayName = "Apfel",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "bean",
                displayName = "Bohne",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "carrot",
                displayName = "Karotte",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "beef",
                displayName = "Rind",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "veal",
                displayName = "Kalb",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "venison",
                displayName = "Hirsch",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "chicken",
                displayName = "Hähnchen",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "apricot",
                displayName = "Aprikose",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "banana",
                displayName = "Banane",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "artichoke",
                displayName = "Artischocke",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "bamboo-shoots",
                displayName = "Bambussprosse",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "asparagus",
                displayName = "Spargel",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "alfalfa",
                displayName = "Alfalfa",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "bean-sprouts",
                displayName = "Bohnensprossen",
                type = SemanticVariantType.INGREDIENT
            ),

            definition(
                canonicalKey = "honey",
                displayName = "Mit Honig",
                type = SemanticVariantType.INGREDIENT,
                aliases = setOf(
                    "Honig"
                )
            ),

            // ---------------------------------------------------------
            // PROCESSING
            // ---------------------------------------------------------

            definition(
                canonicalKey = "fermented",
                displayName = "Fermentiert",
                type = SemanticVariantType.PROCESSING
            ),

            definition(
                canonicalKey = "smoked",
                displayName = "Geräuchert",
                type = SemanticVariantType.PROCESSING
            ),

            definition(
                canonicalKey = "dried",
                displayName = "Getrocknet",
                type = SemanticVariantType.PROCESSING
            ),

            definition(
                canonicalKey = "freeze-dried",
                displayName = "Gefriergetrocknet",
                type = SemanticVariantType.PROCESSING
            ),

            definition(
                canonicalKey = "fortified",
                displayName = "Angereichert",
                type = SemanticVariantType.PROCESSING
            ),

            definition(
                canonicalKey = "instant",
                displayName = "Instant",
                type = SemanticVariantType.PROCESSING
            ),

            definition(
                canonicalKey = "preserved",
                displayName = "Konserviert",
                type = SemanticVariantType.PROCESSING
            ),

            // ---------------------------------------------------------
            // PREPARATION
            // ---------------------------------------------------------

            definition(
                canonicalKey = "cooked",
                displayName = "Gegart",
                type = SemanticVariantType.PREPARATION
            ),

            definition(
                canonicalKey = "baked",
                displayName = "Gebacken",
                type = SemanticVariantType.PREPARATION
            ),

            definition(
                canonicalKey = "boiled",
                displayName = "Gekocht",
                type = SemanticVariantType.PREPARATION
            ),

            definition(
                canonicalKey = "deep-fried",
                displayName = "Frittiert",
                type = SemanticVariantType.PREPARATION
            ),

            definition(
                canonicalKey = "fried",
                displayName = "Gebraten",
                type = SemanticVariantType.PREPARATION
            ),

            definition(
                canonicalKey = "grilled",
                displayName = "Gegrillt",
                type = SemanticVariantType.PREPARATION
            ),

            // ---------------------------------------------------------
            // MATURATION
            // ---------------------------------------------------------

            definition(
                canonicalKey = "aged",
                displayName = "Lang gereift",
                type = SemanticVariantType.MATURATION,
                aliases = setOf(
                    "Gereift"
                )
            ),

            definition(
                canonicalKey = "extra-aged",
                displayName = "Extra lang gereift",
                type = SemanticVariantType.MATURATION
            ),

            definition(
                canonicalKey = "cave-aged",
                displayName = "Höhlengereift",
                type = SemanticVariantType.MATURATION
            ),

            definition(
                canonicalKey = "brine-ripened",
                displayName = "Salzlakegereift",
                type = SemanticVariantType.MATURATION
            ),

            // ---------------------------------------------------------
            // FORM
            // ---------------------------------------------------------

            definition(
                canonicalKey = "cubes",
                displayName = "Würfel",
                type = SemanticVariantType.FORM
            ),

            definition(
                canonicalKey = "sliced",
                displayName = "Geschnitten",
                type = SemanticVariantType.FORM
            ),

            definition(
                canonicalKey = "diced",
                displayName = "Gewürfelt",
                type = SemanticVariantType.FORM
            ),

            definition(
                canonicalKey = "grated",
                displayName = "Geraspelt",
                type = SemanticVariantType.FORM
            ),

            definition(
                canonicalKey = "flakes",
                displayName = "Flocken",
                type = SemanticVariantType.FORM
            ),

            definition(
                canonicalKey = "granules",
                displayName = "Granulat",
                type = SemanticVariantType.FORM
            ),

            definition(
                canonicalKey = "powder",
                displayName = "Pulver",
                type = SemanticVariantType.FORM
            ),

            definition(
                canonicalKey = "concentrate",
                displayName = "Konzentrat",
                type = SemanticVariantType.FORM
            ),

            // ---------------------------------------------------------
            // CUT
            // ---------------------------------------------------------

            definition(
                canonicalKey = "schnitzel-cut",
                displayName = "Schnitzel",
                type = SemanticVariantType.CUT
            ),

            definition(
                canonicalKey = "fillet",
                displayName = "Filet",
                type = SemanticVariantType.CUT
            ),

            definition(
                canonicalKey = "chop",
                displayName = "Kotelett",
                type = SemanticVariantType.CUT
            ),

            // ---------------------------------------------------------
            // TEXTURE
            // ---------------------------------------------------------

            definition(
                canonicalKey = "creamy",
                displayName = "Cremig",
                type = SemanticVariantType.TEXTURE
            ),

            definition(
                canonicalKey = "crispy",
                displayName = "Knusprig",
                type = SemanticVariantType.TEXTURE
            ),

            // ---------------------------------------------------------
            // FLAVOR
            // ---------------------------------------------------------

            definition(
                canonicalKey = "bitter",
                displayName = "Bitter",
                type = SemanticVariantType.FLAVOR
            ),

            definition(
                canonicalKey = "buttery",
                displayName = "Buttrig",
                type = SemanticVariantType.FLAVOR
            ),

            definition(
                canonicalKey = "citrus-like",
                displayName = "Zitrusartig",
                type = SemanticVariantType.FLAVOR
            ),

            definition(
                canonicalKey = "nutty",
                displayName = "Nussig",
                type = SemanticVariantType.FLAVOR
            ),

            definition(
                canonicalKey = "spicy",
                displayName = "Würzig",
                type = SemanticVariantType.FLAVOR
            ),

            definition(
                canonicalKey = "mild",
                displayName = "Mild",
                type = SemanticVariantType.FLAVOR
            ),

            // ---------------------------------------------------------
            // COMPOSITION
            // ---------------------------------------------------------

            definition(
                canonicalKey = "double-cream",
                displayName = "Doppelrahmstufe",
                type = SemanticVariantType.COMPOSITION
            ),

            definition(
                canonicalKey = "full-fat",
                displayName = "Vollfett",
                type = SemanticVariantType.COMPOSITION
            ),

            definition(
                canonicalKey = "high-fat",
                displayName = "Fettreich",
                type = SemanticVariantType.COMPOSITION
            ),

            definition(
                canonicalKey = "reduced-fat",
                displayName = "Fettreduziert",
                type = SemanticVariantType.COMPOSITION
            ),

            // ---------------------------------------------------------
            // NUTRITION_CLAIM
            // ---------------------------------------------------------

            definition(
                canonicalKey = "reduced-energy",
                displayName = "Energiereduziert",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            definition(
                canonicalKey = "high-fiber",
                displayName = "Ballaststoffreich",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            definition(
                canonicalKey = "reduced-carbohydrate",
                displayName = "Kohlenhydratreduziert",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            definition(
                canonicalKey = "no-added-sugar",
                displayName = "Ohne Zuckerzusatz",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            definition(
                canonicalKey = "reduced-sugar",
                displayName = "Zuckerreduziert",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            definition(
                canonicalKey = "fat-free",
                displayName = "Fettfrei",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            definition(
                canonicalKey = "high-protein",
                displayName = "Proteinreich",
                type = SemanticVariantType.NUTRITION_CLAIM,
                aliases = setOf(
                    "Eiweißreich",
                    "Eiweissreich"
                )
            ),

            definition(
                canonicalKey = "low-fat",
                displayName = "Fettarm",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            definition(
                canonicalKey = "sugar-free",
                displayName = "Zuckerfrei",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            definition(
                canonicalKey = "low-sugar",
                displayName = "Zuckerarm",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            definition(
                canonicalKey = "low-salt",
                displayName = "Salzarm",
                type = SemanticVariantType.NUTRITION_CLAIM
            ),

            // ---------------------------------------------------------
            // ALLERGEN_CLAIM
            // ---------------------------------------------------------

            definition(
                canonicalKey = "egg-free",
                displayName = "Eifrei",
                type = SemanticVariantType.ALLERGEN_CLAIM
            ),

            definition(
                canonicalKey = "gluten-free",
                displayName = "Glutenfrei",
                type = SemanticVariantType.ALLERGEN_CLAIM
            ),

            definition(
                canonicalKey = "lactose-free",
                displayName = "Laktosefrei",
                type = SemanticVariantType.ALLERGEN_CLAIM
            ),

            definition(
                canonicalKey = "celery-free",
                displayName = "Selleriefrei",
                type = SemanticVariantType.ALLERGEN_CLAIM
            ),

            // ---------------------------------------------------------
            // DIET_CLAIM
            // ---------------------------------------------------------

            definition(
                canonicalKey = "vegan",
                displayName = "Vegan",
                type = SemanticVariantType.DIET_CLAIM
            ),

            definition(
                canonicalKey = "vegetarian",
                displayName = "Vegetarisch",
                type = SemanticVariantType.DIET_CLAIM
            ),

            // ---------------------------------------------------------
            // PRODUCTION_METHOD
            // ---------------------------------------------------------

            definition(
                canonicalKey = "organic",
                displayName = "Bio",
                type = SemanticVariantType.PRODUCTION_METHOD,
                aliases = setOf(
                    "Biologisch"
                )
            ),

            definition(
                canonicalKey = "conventional",
                displayName = "Konventionell",
                type = SemanticVariantType.PRODUCTION_METHOD
            ),

            // ---------------------------------------------------------
            // STORAGE_STATE
            // ---------------------------------------------------------

            definition(
                canonicalKey = "chilled",
                displayName = "Gekühlt",
                type = SemanticVariantType.STORAGE_STATE
            ),

            definition(
                canonicalKey = "frozen",
                displayName = "Tiefgekühlt",
                type = SemanticVariantType.STORAGE_STATE,
                aliases = setOf(
                    "Tiefkühl",
                    "TK"
                )
            ),

            definition(
                canonicalKey = "fresh",
                displayName = "Frisch",
                type = SemanticVariantType.STORAGE_STATE
            ),

            // ---------------------------------------------------------
            // STYLE
            // ---------------------------------------------------------

            definition(
                canonicalKey = "classic",
                displayName = "Klassisch",
                type = SemanticVariantType.STYLE
            ),

            definition(
                canonicalKey = "traditional",
                displayName = "Traditionell",
                type = SemanticVariantType.STYLE
            ),

            // ---------------------------------------------------------
            // GENERIC_PLACEHOLDER
            // ---------------------------------------------------------

            definition(
                canonicalKey = "fish-based",
                displayName = "Fischbasiert",
                type = SemanticVariantType.GENERIC_PLACEHOLDER
            ),

            definition(
                canonicalKey = "cheese-based",
                displayName = "Käsebasiert",
                type = SemanticVariantType.GENERIC_PLACEHOLDER
            ),

            definition(
                canonicalKey = "fruit-based",
                displayName = "Fruchtbasiert",
                type = SemanticVariantType.GENERIC_PLACEHOLDER
            ),

            definition(
                canonicalKey = "cultured-food",
                displayName = "Kultiviertes Lebensmittel",
                type = SemanticVariantType.GENERIC_PLACEHOLDER
            ),

            definition(
                canonicalKey = "coated-food",
                displayName = "Überzogenes Lebensmittel",
                type = SemanticVariantType.GENERIC_PLACEHOLDER
            )
        )

    private val byLookupKey: Map<String, SemanticVariantDefinition> =
        buildLookup()

    private val byCanonicalKey: Map<String, SemanticVariantDefinition> =
        definitions
            .associateBy { definition ->
                definition.canonicalKey
            }

    init {
        require(
            byCanonicalKey.size == definitions.size
        ) {
            "Duplicate semantic variant canonicalKey detected."
        }
    }

    fun all(): List<SemanticVariantDefinition> =
        definitions
            .sortedBy { it.canonicalKey }

    fun definitionFor(
        rawValue: String
    ): SemanticVariantDefinition? =
        byLookupKey[normalizeLookupKey(rawValue)]

    fun definitionForCanonicalKey(
        canonicalKey: String
    ): SemanticVariantDefinition? =
        byCanonicalKey[canonicalKey]

    fun normalizeLookupKey(
        rawValue: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                rawValue.trim().lowercase(),
                Normalizer.Form.NFD
            )

        return decomposed
            .replace(COMBINING_MARKS_REGEX, "")
            .replace("ß", "ss")
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private fun buildLookup():
            Map<String, SemanticVariantDefinition> {

        val result =
            linkedMapOf<String, SemanticVariantDefinition>()

        definitions.forEach { definition ->

            val lookupValues =
                buildSet {
                    add(definition.displayName)
                    add(definition.canonicalKey)
                    addAll(definition.aliases)
                }

            lookupValues.forEach { rawValue ->

                val lookupKey =
                    normalizeLookupKey(rawValue)

                val previous =
                    result.put(
                        lookupKey,
                        definition
                    )

                require(
                    previous == null ||
                            previous.canonicalKey ==
                            definition.canonicalKey
                ) {
                    "Semantic variant lookup collision for '$rawValue': " +
                            "${previous?.canonicalKey} vs " +
                            definition.canonicalKey
                }
            }
        }

        return result.toMap()
    }

    private fun definition(
        canonicalKey: String,
        displayName: String,
        type: SemanticVariantType,
        aliases: Set<String> = emptySet()
    ) =
        SemanticVariantDefinition(
            canonicalKey = canonicalKey,
            displayName = displayName,
            type = type,
            aliases = aliases
        )
}