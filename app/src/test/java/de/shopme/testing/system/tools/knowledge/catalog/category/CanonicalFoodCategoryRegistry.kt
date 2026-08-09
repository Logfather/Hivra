package de.shopme.testing.system.tools.knowledge.catalog.category

import java.util.Locale

class CanonicalFoodCategoryRegistry {

    private val categoryDefinitions: List<CatalogCategoryDefinition> by lazy {
        buildDefinitions()
            .also(::validateDefinitions)
    }

    private val definitionsByKey: Map<String, CatalogCategoryDefinition> by lazy {
        categoryDefinitions.associateBy { it.key }
    }

    fun definitions(): List<CatalogCategoryDefinition> =
        categoryDefinitions

    fun definition(key: String): CatalogCategoryDefinition? =
        definitionsByKey[normalizeKey(key)]

    fun requireDefinition(key: String): CatalogCategoryDefinition =
        requireNotNull(definition(key)) {
            "Unknown canonical food category key: '$key'."
        }

    fun contains(key: String): Boolean =
        normalizeKey(key) in definitionsByKey

    fun rootCategories(): List<CatalogCategoryDefinition> =
        categoryDefinitions
            .filter { it.parentKey == null }
            .sortedWith(CATEGORY_COMPARATOR)

    fun children(parentKey: String): List<CatalogCategoryDefinition> {
        val normalizedParentKey = normalizeKey(parentKey)

        require(normalizedParentKey in definitionsByKey) {
            "Cannot resolve children for unknown category key: '$parentKey'."
        }

        return categoryDefinitions
            .filter { it.parentKey == normalizedParentKey }
            .sortedWith(CATEGORY_COMPARATOR)
    }

    fun descendants(parentKey: String): List<CatalogCategoryDefinition> {
        val normalizedParentKey = normalizeKey(parentKey)

        require(normalizedParentKey in definitionsByKey) {
            "Cannot resolve descendants for unknown category key: '$parentKey'."
        }

        val descendants = mutableListOf<CatalogCategoryDefinition>()
        val pendingKeys = ArrayDeque<String>()

        children(normalizedParentKey)
            .mapTo(pendingKeys) { it.key }

        while (pendingKeys.isNotEmpty()) {
            val currentKey = pendingKeys.removeFirst()
            val currentDefinition = definitionsByKey.getValue(currentKey)

            descendants += currentDefinition

            children(currentKey)
                .mapTo(pendingKeys) { it.key }
        }

        return descendants
            .distinctBy { it.key }
            .sortedWith(CATEGORY_COMPARATOR)
    }

    fun ancestors(categoryKey: String): List<CatalogCategoryDefinition> {
        val normalizedCategoryKey = normalizeKey(categoryKey)
        val startDefinition = requireNotNull(definitionsByKey[normalizedCategoryKey]) {
            "Cannot resolve ancestors for unknown category key: '$categoryKey'."
        }

        val result = mutableListOf<CatalogCategoryDefinition>()
        var currentParentKey = startDefinition.parentKey

        while (currentParentKey != null) {
            val parentDefinition = requireNotNull(definitionsByKey[currentParentKey]) {
                "Category '${startDefinition.key}' references unknown parent '$currentParentKey'."
            }

            result += parentDefinition
            currentParentKey = parentDefinition.parentKey
        }

        return result
    }

    fun lineage(categoryKey: String): List<CatalogCategoryDefinition> {
        val definition = requireDefinition(categoryKey)

        return (ancestors(definition.key).reversed() + definition)
    }

    fun topLevelCategory(categoryKey: String): CatalogCategoryDefinition {
        val lineage = lineage(categoryKey)

        return lineage.first()
    }

    fun isRootCategory(categoryKey: String): Boolean =
        requireDefinition(categoryKey).parentKey == null

    fun isDescendantOf(
        categoryKey: String,
        potentialAncestorKey: String
    ): Boolean {
        val normalizedAncestorKey = normalizeKey(potentialAncestorKey)

        require(normalizedAncestorKey in definitionsByKey) {
            "Unknown potential ancestor category key: '$potentialAncestorKey'."
        }

        return ancestors(categoryKey)
            .any { it.key == normalizedAncestorKey }
    }

    fun depth(categoryKey: String): Int =
        ancestors(categoryKey).size

    fun leafCategories(): List<CatalogCategoryDefinition> {
        val parentKeys = categoryDefinitions
            .mapNotNull { it.parentKey }
            .toSet()

        return categoryDefinitions
            .filterNot { it.key in parentKeys }
            .sortedWith(CATEGORY_COMPARATOR)
    }

    fun categoryPath(categoryKey: String): String =
        lineage(categoryKey)
            .joinToString(PATH_SEPARATOR) { it.displayName }

    fun keys(): Set<String> =
        categoryDefinitions
            .mapTo(sortedSetOf()) { it.key }

    fun displayNames(): Set<String> =
        categoryDefinitions
            .mapTo(sortedSetOf(String.CASE_INSENSITIVE_ORDER)) {
                it.displayName
            }

    private fun buildDefinitions(): List<CatalogCategoryDefinition> {
        val specifications = listOf(
            root(
                key = "fruit",
                displayName = "Obst"
            ),
            child(
                key = "pome-fruit",
                displayName = "Kernobst",
                parentKey = "fruit"
            ),
            child(
                key = "stone-fruit",
                displayName = "Steinobst",
                parentKey = "fruit"
            ),
            child(
                key = "berries",
                displayName = "Beerenobst",
                parentKey = "fruit"
            ),
            child(
                key = "citrus-fruit",
                displayName = "Zitrusfrüchte",
                parentKey = "fruit"
            ),
            child(
                key = "tropical-fruit",
                displayName = "Tropische Früchte",
                parentKey = "fruit"
            ),
            child(
                key = "melons",
                displayName = "Melonen",
                parentKey = "fruit"
            ),
            child(
                key = "grapes",
                displayName = "Trauben",
                parentKey = "fruit"
            ),
            child(
                key = "dried-fruit",
                displayName = "Trockenfrüchte",
                parentKey = "fruit"
            ),
            child(
                key = "processed-fruit",
                displayName = "Verarbeitetes Obst",
                parentKey = "fruit"
            ),

            root(
                key = "vegetables",
                displayName = "Gemüse"
            ),
            child(
                key = "root-vegetables",
                displayName = "Wurzelgemüse",
                parentKey = "vegetables"
            ),
            child(
                key = "tuber-vegetables",
                displayName = "Knollengemüse",
                parentKey = "vegetables"
            ),
            child(
                key = "leafy-vegetables",
                displayName = "Blattgemüse",
                parentKey = "vegetables"
            ),
            child(
                key = "cabbage-vegetables",
                displayName = "Kohlgemüse",
                parentKey = "vegetables"
            ),
            child(
                key = "fruiting-vegetables",
                displayName = "Fruchtgemüse",
                parentKey = "vegetables"
            ),
            child(
                key = "stem-vegetables",
                displayName = "Stängelgemüse",
                parentKey = "vegetables"
            ),
            child(
                key = "flower-vegetables",
                displayName = "Blütengemüse",
                parentKey = "vegetables"
            ),
            child(
                key = "onion-vegetables",
                displayName = "Zwiebelgemüse",
                parentKey = "vegetables"
            ),
            child(
                key = "sprouts",
                displayName = "Sprossen und Keimlinge",
                parentKey = "vegetables"
            ),
            child(
                key = "sea-vegetables",
                displayName = "Algen und Meeresgemüse",
                parentKey = "vegetables"
            ),
            child(
                key = "processed-vegetables",
                displayName = "Verarbeitetes Gemüse",
                parentKey = "vegetables"
            ),

            root(
                key = "herbs",
                displayName = "Kräuter"
            ),
            child(
                key = "fresh-herbs",
                displayName = "Frische Kräuter",
                parentKey = "herbs"
            ),
            child(
                key = "dried-herbs",
                displayName = "Getrocknete Kräuter",
                parentKey = "herbs"
            ),
            child(
                key = "wild-herbs",
                displayName = "Wildkräuter",
                parentKey = "herbs"
            ),
            child(
                key = "culinary-herb-mixtures",
                displayName = "Kräutermischungen",
                parentKey = "herbs"
            ),

            root(
                key = "mushrooms",
                displayName = "Pilze"
            ),
            child(
                key = "cultivated-mushrooms",
                displayName = "Kulturpilze",
                parentKey = "mushrooms"
            ),
            child(
                key = "wild-mushrooms",
                displayName = "Wildpilze",
                parentKey = "mushrooms"
            ),
            child(
                key = "dried-mushrooms",
                displayName = "Getrocknete Pilze",
                parentKey = "mushrooms"
            ),
            child(
                key = "processed-mushrooms",
                displayName = "Verarbeitete Pilze",
                parentKey = "mushrooms"
            ),

            root(
                key = "meat",
                displayName = "Fleisch"
            ),
            child(
                key = "beef",
                displayName = "Rindfleisch",
                parentKey = "meat"
            ),
            child(
                key = "veal",
                displayName = "Kalbfleisch",
                parentKey = "meat"
            ),
            child(
                key = "pork",
                displayName = "Schweinefleisch",
                parentKey = "meat"
            ),
            child(
                key = "lamb",
                displayName = "Lammfleisch",
                parentKey = "meat"
            ),
            child(
                key = "mutton",
                displayName = "Schaffleisch",
                parentKey = "meat"
            ),
            child(
                key = "goat-meat",
                displayName = "Ziegenfleisch",
                parentKey = "meat"
            ),
            child(
                key = "game-meat",
                displayName = "Wildfleisch",
                parentKey = "meat"
            ),
            child(
                key = "rabbit-meat",
                displayName = "Kaninchenfleisch",
                parentKey = "meat"
            ),
            child(
                key = "offal",
                displayName = "Innereien",
                parentKey = "meat"
            ),
            child(
                key = "minced-meat",
                displayName = "Hackfleisch",
                parentKey = "meat"
            ),
            child(
                key = "meat-cuts",
                displayName = "Fleischzuschnitte",
                parentKey = "meat"
            ),

            root(
                key = "poultry",
                displayName = "Geflügel"
            ),
            child(
                key = "chicken",
                displayName = "Hähnchen",
                parentKey = "poultry"
            ),
            child(
                key = "turkey",
                displayName = "Pute",
                parentKey = "poultry"
            ),
            child(
                key = "duck",
                displayName = "Ente",
                parentKey = "poultry"
            ),
            child(
                key = "goose",
                displayName = "Gans",
                parentKey = "poultry"
            ),
            child(
                key = "quail",
                displayName = "Wachtel",
                parentKey = "poultry"
            ),
            child(
                key = "other-poultry",
                displayName = "Sonstiges Geflügel",
                parentKey = "poultry"
            ),
            child(
                key = "poultry-offal",
                displayName = "Geflügelinnereien",
                parentKey = "poultry"
            ),

            root(
                key = "sausage",
                displayName = "Wurst"
            ),
            child(
                key = "boiled-sausage",
                displayName = "Brühwurst",
                parentKey = "sausage"
            ),
            child(
                key = "cooked-sausage",
                displayName = "Kochwurst",
                parentKey = "sausage"
            ),
            child(
                key = "raw-sausage",
                displayName = "Rohwurst",
                parentKey = "sausage"
            ),
            child(
                key = "spreadable-sausage",
                displayName = "Streichwurst",
                parentKey = "sausage"
            ),
            child(
                key = "ham",
                displayName = "Schinken",
                parentKey = "sausage"
            ),
            child(
                key = "bacon",
                displayName = "Speck",
                parentKey = "sausage"
            ),
            child(
                key = "poultry-sausage",
                displayName = "Geflügelwurst",
                parentKey = "sausage"
            ),
            child(
                key = "meat-products",
                displayName = "Fleischerzeugnisse",
                parentKey = "sausage"
            ),

            root(
                key = "fish",
                displayName = "Fisch"
            ),
            child(
                key = "freshwater-fish",
                displayName = "Süßwasserfisch",
                parentKey = "fish"
            ),
            child(
                key = "saltwater-fish",
                displayName = "Salzwasserfisch",
                parentKey = "fish"
            ),
            child(
                key = "fatty-fish",
                displayName = "Fettreicher Fisch",
                parentKey = "fish"
            ),
            child(
                key = "lean-fish",
                displayName = "Magerfisch",
                parentKey = "fish"
            ),
            child(
                key = "smoked-fish",
                displayName = "Räucherfisch",
                parentKey = "fish"
            ),
            child(
                key = "dried-fish",
                displayName = "Trockenfisch",
                parentKey = "fish"
            ),
            child(
                key = "fish-products",
                displayName = "Fischerzeugnisse",
                parentKey = "fish"
            ),

            root(
                key = "seafood",
                displayName = "Meeresfrüchte"
            ),
            child(
                key = "crustaceans",
                displayName = "Krebstiere",
                parentKey = "seafood"
            ),
            child(
                key = "molluscs",
                displayName = "Weichtiere",
                parentKey = "seafood"
            ),
            child(
                key = "cephalopods",
                displayName = "Kopffüßer",
                parentKey = "seafood"
            ),
            child(
                key = "shellfish-products",
                displayName = "Meeresfrüchteerzeugnisse",
                parentKey = "seafood"
            ),

            root(
                key = "dairy",
                displayName = "Milchprodukte"
            ),
            child(
                key = "milk",
                displayName = "Milch",
                parentKey = "dairy"
            ),
            child(
                key = "cream",
                displayName = "Sahne und Rahm",
                parentKey = "dairy"
            ),
            child(
                key = "yogurt",
                displayName = "Joghurt",
                parentKey = "dairy"
            ),
            child(
                key = "quark",
                displayName = "Quark",
                parentKey = "dairy"
            ),
            child(
                key = "buttermilk",
                displayName = "Buttermilch",
                parentKey = "dairy"
            ),
            child(
                key = "kefir",
                displayName = "Kefir",
                parentKey = "dairy"
            ),
            child(
                key = "soured-milk-products",
                displayName = "Sauermilchprodukte",
                parentKey = "dairy"
            ),
            child(
                key = "milk-desserts",
                displayName = "Milchdesserts",
                parentKey = "dairy"
            ),
            child(
                key = "butter",
                displayName = "Butter",
                parentKey = "dairy"
            ),
            child(
                key = "clarified-butter",
                displayName = "Butterschmalz",
                parentKey = "dairy"
            ),
            child(
                key = "milk-powder",
                displayName = "Milchpulver",
                parentKey = "dairy"
            ),
            child(
                key = "condensed-milk",
                displayName = "Kondensmilch",
                parentKey = "dairy"
            ),

            root(
                key = "cheese",
                displayName = "Käse"
            ),
            child(
                key = "fresh-cheese",
                displayName = "Frischkäse",
                parentKey = "cheese"
            ),
            child(
                key = "soft-cheese",
                displayName = "Weichkäse",
                parentKey = "cheese"
            ),
            child(
                key = "semi-hard-cheese",
                displayName = "Schnittkäse",
                parentKey = "cheese"
            ),
            child(
                key = "hard-cheese",
                displayName = "Hartkäse",
                parentKey = "cheese"
            ),
            child(
                key = "blue-cheese",
                displayName = "Blauschimmelkäse",
                parentKey = "cheese"
            ),
            child(
                key = "brined-cheese",
                displayName = "Salzlakenkäse",
                parentKey = "cheese"
            ),
            child(
                key = "processed-cheese",
                displayName = "Schmelzkäse",
                parentKey = "cheese"
            ),
            child(
                key = "goat-cheese",
                displayName = "Ziegenkäse",
                parentKey = "cheese"
            ),
            child(
                key = "sheep-cheese",
                displayName = "Schafskäse",
                parentKey = "cheese"
            ),
            child(
                key = "mixed-milk-cheese",
                displayName = "Mischmilchkäse",
                parentKey = "cheese"
            ),

            root(
                key = "eggs",
                displayName = "Eier"
            ),
            child(
                key = "chicken-eggs",
                displayName = "Hühnereier",
                parentKey = "eggs"
            ),
            child(
                key = "quail-eggs",
                displayName = "Wachteleier",
                parentKey = "eggs"
            ),
            child(
                key = "duck-eggs",
                displayName = "Enteneier",
                parentKey = "eggs"
            ),
            child(
                key = "egg-products",
                displayName = "Eiprodukte",
                parentKey = "eggs"
            ),

            root(
                key = "plant-based-alternatives",
                displayName = "Pflanzliche Alternativen"
            ),
            child(
                key = "plant-based-milk-alternatives",
                displayName = "Pflanzliche Milchalternativen",
                parentKey = "plant-based-alternatives"
            ),
            child(
                key = "plant-based-yogurt-alternatives",
                displayName = "Pflanzliche Joghurtalternativen",
                parentKey = "plant-based-alternatives"
            ),
            child(
                key = "plant-based-cheese-alternatives",
                displayName = "Pflanzliche Käsealternativen",
                parentKey = "plant-based-alternatives"
            ),
            child(
                key = "plant-based-meat-alternatives",
                displayName = "Pflanzliche Fleischalternativen",
                parentKey = "plant-based-alternatives"
            ),
            child(
                key = "plant-based-fish-alternatives",
                displayName = "Pflanzliche Fischalternativen",
                parentKey = "plant-based-alternatives"
            ),
            child(
                key = "tofu",
                displayName = "Tofu",
                parentKey = "plant-based-alternatives"
            ),
            child(
                key = "tempeh",
                displayName = "Tempeh",
                parentKey = "plant-based-alternatives"
            ),
            child(
                key = "seitan",
                displayName = "Seitan",
                parentKey = "plant-based-alternatives"
            ),
            child(
                key = "legume-based-alternatives",
                displayName = "Hülsenfruchtbasierte Alternativen",
                parentKey = "plant-based-alternatives"
            ),

            root(
                key = "bread",
                displayName = "Brot"
            ),
            child(
                key = "wheat-bread",
                displayName = "Weizenbrot",
                parentKey = "bread"
            ),
            child(
                key = "rye-bread",
                displayName = "Roggenbrot",
                parentKey = "bread"
            ),
            child(
                key = "mixed-bread",
                displayName = "Mischbrot",
                parentKey = "bread"
            ),
            child(
                key = "wholegrain-bread",
                displayName = "Vollkornbrot",
                parentKey = "bread"
            ),
            child(
                key = "sourdough-bread",
                displayName = "Sauerteigbrot",
                parentKey = "bread"
            ),
            child(
                key = "crispbread",
                displayName = "Knäckebrot",
                parentKey = "bread"
            ),
            child(
                key = "flatbread",
                displayName = "Fladenbrot",
                parentKey = "bread"
            ),
            child(
                key = "toast-bread",
                displayName = "Toastbrot",
                parentKey = "bread"
            ),
            child(
                key = "gluten-free-bread",
                displayName = "Glutenfreies Brot",
                parentKey = "bread"
            ),
            child(
                key = "speciality-bread",
                displayName = "Brotspezialitäten",
                parentKey = "bread"
            ),

            root(
                key = "bakery",
                displayName = "Backwaren"
            ),
            child(
                key = "bread-rolls",
                displayName = "Brötchen",
                parentKey = "bakery"
            ),
            child(
                key = "croissants",
                displayName = "Croissants",
                parentKey = "bakery"
            ),
            child(
                key = "pastries",
                displayName = "Feingebäck",
                parentKey = "bakery"
            ),
            child(
                key = "cakes",
                displayName = "Kuchen",
                parentKey = "bakery"
            ),
            child(
                key = "tarts",
                displayName = "Torten",
                parentKey = "bakery"
            ),
            child(
                key = "cookies",
                displayName = "Kekse",
                parentKey = "bakery"
            ),
            child(
                key = "waffles",
                displayName = "Waffeln",
                parentKey = "bakery"
            ),
            child(
                key = "sweet-yeast-bakery",
                displayName = "Süßes Hefegebäck",
                parentKey = "bakery"
            ),
            child(
                key = "savoury-bakery",
                displayName = "Herzhafte Backwaren",
                parentKey = "bakery"
            ),
            child(
                key = "gluten-free-bakery",
                displayName = "Glutenfreie Backwaren",
                parentKey = "bakery"
            ),

            root(
                key = "baking-ingredients",
                displayName = "Backzutaten"
            ),
            child(
                key = "flour",
                displayName = "Mehl",
                parentKey = "baking-ingredients"
            ),
            child(
                key = "starch",
                displayName = "Stärke",
                parentKey = "baking-ingredients"
            ),
            child(
                key = "baking-agents",
                displayName = "Backtriebmittel",
                parentKey = "baking-ingredients"
            ),
            child(
                key = "yeast",
                displayName = "Hefe",
                parentKey = "baking-ingredients"
            ),
            child(
                key = "baking-decorations",
                displayName = "Backdekoration",
                parentKey = "baking-ingredients"
            ),
            child(
                key = "baking-flavours",
                displayName = "Backaromen",
                parentKey = "baking-ingredients"
            ),
            child(
                key = "baking-mixes",
                displayName = "Backmischungen",
                parentKey = "baking-ingredients"
            ),
            child(
                key = "gelatin-and-gelling-agents",
                displayName = "Geliermittel",
                parentKey = "baking-ingredients"
            ),
            child(
                key = "cocoa-baking-products",
                displayName = "Kakaobasierte Backzutaten",
                parentKey = "baking-ingredients"
            ),

            root(
                key = "grains",
                displayName = "Getreide"
            ),
            child(
                key = "wheat",
                displayName = "Weizen",
                parentKey = "grains"
            ),
            child(
                key = "rye",
                displayName = "Roggen",
                parentKey = "grains"
            ),
            child(
                key = "barley",
                displayName = "Gerste",
                parentKey = "grains"
            ),
            child(
                key = "oats",
                displayName = "Hafer",
                parentKey = "grains"
            ),
            child(
                key = "spelt",
                displayName = "Dinkel",
                parentKey = "grains"
            ),
            child(
                key = "millet",
                displayName = "Hirse",
                parentKey = "grains"
            ),
            child(
                key = "corn",
                displayName = "Mais",
                parentKey = "grains"
            ),
            child(
                key = "ancient-grains",
                displayName = "Urgetreide",
                parentKey = "grains"
            ),
            child(
                key = "pseudocereals",
                displayName = "Pseudogetreide",
                parentKey = "grains"
            ),
            child(
                key = "grain-products",
                displayName = "Getreideerzeugnisse",
                parentKey = "grains"
            ),

            root(
                key = "rice",
                displayName = "Reis"
            ),
            child(
                key = "long-grain-rice",
                displayName = "Langkornreis",
                parentKey = "rice"
            ),
            child(
                key = "short-grain-rice",
                displayName = "Rundkornreis",
                parentKey = "rice"
            ),
            child(
                key = "medium-grain-rice",
                displayName = "Mittelkornreis",
                parentKey = "rice"
            ),
            child(
                key = "brown-rice",
                displayName = "Vollkornreis",
                parentKey = "rice"
            ),
            child(
                key = "parboiled-rice",
                displayName = "Parboiled-Reis",
                parentKey = "rice"
            ),
            child(
                key = "aromatic-rice",
                displayName = "Duftreis",
                parentKey = "rice"
            ),
            child(
                key = "risotto-rice",
                displayName = "Risottoreis",
                parentKey = "rice"
            ),
            child(
                key = "sushi-rice",
                displayName = "Sushireis",
                parentKey = "rice"
            ),
            child(
                key = "wild-rice",
                displayName = "Wildreis",
                parentKey = "rice"
            ),
            child(
                key = "rice-products",
                displayName = "Reiserzeugnisse",
                parentKey = "rice"
            ),

            root(
                key = "pasta",
                displayName = "Nudeln"
            ),
            child(
                key = "wheat-pasta",
                displayName = "Weizennudeln",
                parentKey = "pasta"
            ),
            child(
                key = "durum-wheat-pasta",
                displayName = "Hartweizennudeln",
                parentKey = "pasta"
            ),
            child(
                key = "egg-pasta",
                displayName = "Eiernudeln",
                parentKey = "pasta"
            ),
            child(
                key = "wholegrain-pasta",
                displayName = "Vollkornnudeln",
                parentKey = "pasta"
            ),
            child(
                key = "gluten-free-pasta",
                displayName = "Glutenfreie Nudeln",
                parentKey = "pasta"
            ),
            child(
                key = "legume-pasta",
                displayName = "Hülsenfruchtnudeln",
                parentKey = "pasta"
            ),
            child(
                key = "fresh-pasta",
                displayName = "Frische Nudeln",
                parentKey = "pasta"
            ),
            child(
                key = "filled-pasta",
                displayName = "Gefüllte Nudeln",
                parentKey = "pasta"
            ),
            child(
                key = "asian-noodles",
                displayName = "Asiatische Nudeln",
                parentKey = "pasta"
            ),
            child(
                key = "pasta-specialities",
                displayName = "Nudelspezialitäten",
                parentKey = "pasta"
            ),

            root(
                key = "legumes",
                displayName = "Hülsenfrüchte"
            ),
            child(
                key = "beans",
                displayName = "Bohnen",
                parentKey = "legumes"
            ),
            child(
                key = "peas",
                displayName = "Erbsen",
                parentKey = "legumes"
            ),
            child(
                key = "lentils",
                displayName = "Linsen",
                parentKey = "legumes"
            ),
            child(
                key = "chickpeas",
                displayName = "Kichererbsen",
                parentKey = "legumes"
            ),
            child(
                key = "soybeans",
                displayName = "Sojabohnen",
                parentKey = "legumes"
            ),
            child(
                key = "lupins",
                displayName = "Lupinen",
                parentKey = "legumes"
            ),
            child(
                key = "peanuts",
                displayName = "Erdnüsse",
                parentKey = "legumes"
            ),
            child(
                key = "processed-legumes",
                displayName = "Verarbeitete Hülsenfrüchte",
                parentKey = "legumes"
            ),

            root(
                key = "canned-food",
                displayName = "Konserven"
            ),
            child(
                key = "canned-fruit",
                displayName = "Obstkonserven",
                parentKey = "canned-food"
            ),
            child(
                key = "canned-vegetables",
                displayName = "Gemüsekonserven",
                parentKey = "canned-food"
            ),
            child(
                key = "canned-legumes",
                displayName = "Hülsenfruchtkonserven",
                parentKey = "canned-food"
            ),
            child(
                key = "canned-fish",
                displayName = "Fischkonserven",
                parentKey = "canned-food"
            ),
            child(
                key = "canned-meat",
                displayName = "Fleischkonserven",
                parentKey = "canned-food"
            ),
            child(
                key = "canned-soups",
                displayName = "Suppenkonserven",
                parentKey = "canned-food"
            ),
            child(
                key = "pickled-food",
                displayName = "Eingelegte Lebensmittel",
                parentKey = "canned-food"
            ),
            child(
                key = "preserved-ready-meals",
                displayName = "Konservierte Fertiggerichte",
                parentKey = "canned-food"
            ),

            root(
                key = "frozen-food",
                displayName = "Tiefkühlkost"
            ),
            child(
                key = "frozen-fruit",
                displayName = "Tiefkühlobst",
                parentKey = "frozen-food"
            ),
            child(
                key = "frozen-vegetables",
                displayName = "Tiefkühlgemüse",
                parentKey = "frozen-food"
            ),
            child(
                key = "frozen-herbs",
                displayName = "Tiefkühlkräuter",
                parentKey = "frozen-food"
            ),
            child(
                key = "frozen-fish",
                displayName = "Tiefkühlfisch",
                parentKey = "frozen-food"
            ),
            child(
                key = "frozen-seafood",
                displayName = "Tiefkühl-Meeresfrüchte",
                parentKey = "frozen-food"
            ),
            child(
                key = "frozen-meat",
                displayName = "Tiefkühlfleisch",
                parentKey = "frozen-food"
            ),
            child(
                key = "frozen-bakery",
                displayName = "Tiefkühlbackwaren",
                parentKey = "frozen-food"
            ),
            child(
                key = "frozen-ready-meals",
                displayName = "Tiefkühl-Fertiggerichte",
                parentKey = "frozen-food"
            ),
            child(
                key = "ice-cream",
                displayName = "Speiseeis",
                parentKey = "frozen-food"
            ),

            root(
                key = "ready-meals",
                displayName = "Fertiggerichte"
            ),
            child(
                key = "pasta-ready-meals",
                displayName = "Nudelgerichte",
                parentKey = "ready-meals"
            ),
            child(
                key = "rice-ready-meals",
                displayName = "Reisgerichte",
                parentKey = "ready-meals"
            ),
            child(
                key = "potato-ready-meals",
                displayName = "Kartoffelgerichte",
                parentKey = "ready-meals"
            ),
            child(
                key = "meat-ready-meals",
                displayName = "Fleischgerichte",
                parentKey = "ready-meals"
            ),
            child(
                key = "fish-ready-meals",
                displayName = "Fischgerichte",
                parentKey = "ready-meals"
            ),
            child(
                key = "vegetarian-ready-meals",
                displayName = "Vegetarische Fertiggerichte",
                parentKey = "ready-meals"
            ),
            child(
                key = "vegan-ready-meals",
                displayName = "Vegane Fertiggerichte",
                parentKey = "ready-meals"
            ),
            child(
                key = "pizza",
                displayName = "Pizza",
                parentKey = "ready-meals"
            ),
            child(
                key = "filled-dough-products",
                displayName = "Gefüllte Teigwaren",
                parentKey = "ready-meals"
            ),
            child(
                key = "meal-components",
                displayName = "Mahlzeitenkomponenten",
                parentKey = "ready-meals"
            ),

            root(
                key = "soups",
                displayName = "Suppen"
            ),
            child(
                key = "clear-soups",
                displayName = "Klare Suppen",
                parentKey = "soups"
            ),
            child(
                key = "cream-soups",
                displayName = "Cremesuppen",
                parentKey = "soups"
            ),
            child(
                key = "vegetable-soups",
                displayName = "Gemüsesuppen",
                parentKey = "soups"
            ),
            child(
                key = "legume-soups",
                displayName = "Hülsenfruchtsuppen",
                parentKey = "soups"
            ),
            child(
                key = "meat-soups",
                displayName = "Fleischsuppen",
                parentKey = "soups"
            ),
            child(
                key = "fish-soups",
                displayName = "Fischsuppen",
                parentKey = "soups"
            ),
            child(
                key = "stews",
                displayName = "Eintöpfe",
                parentKey = "soups"
            ),
            child(
                key = "instant-soups",
                displayName = "Instant-Suppen",
                parentKey = "soups"
            ),
            child(
                key = "broths-and-stocks",
                displayName = "Brühen und Fonds",
                parentKey = "soups"
            ),

            root(
                key = "spices",
                displayName = "Gewürze"
            ),
            child(
                key = "single-spices",
                displayName = "Einzelgewürze",
                parentKey = "spices"
            ),
            child(
                key = "spice-mixtures",
                displayName = "Gewürzmischungen",
                parentKey = "spices"
            ),
            child(
                key = "pepper-spices",
                displayName = "Pfeffer",
                parentKey = "spices"
            ),
            child(
                key = "chilli-spices",
                displayName = "Chili",
                parentKey = "spices"
            ),
            child(
                key = "salt",
                displayName = "Salz",
                parentKey = "spices"
            ),
            child(
                key = "seasoning-pastes",
                displayName = "Würzpasten",
                parentKey = "spices"
            ),
            child(
                key = "seasoning-powders",
                displayName = "Würzpulver",
                parentKey = "spices"
            ),

            root(
                key = "oils",
                displayName = "Öle"
            ),
            child(
                key = "olive-oil",
                displayName = "Olivenöl",
                parentKey = "oils"
            ),
            child(
                key = "rapeseed-oil",
                displayName = "Rapsöl",
                parentKey = "oils"
            ),
            child(
                key = "sunflower-oil",
                displayName = "Sonnenblumenöl",
                parentKey = "oils"
            ),
            child(
                key = "seed-oils",
                displayName = "Samenöle",
                parentKey = "oils"
            ),
            child(
                key = "nut-oils",
                displayName = "Nussöle",
                parentKey = "oils"
            ),
            child(
                key = "speciality-oils",
                displayName = "Spezialöle",
                parentKey = "oils"
            ),
            child(
                key = "frying-oils",
                displayName = "Brat- und Frittieröle",
                parentKey = "oils"
            ),
            child(
                key = "blended-oils",
                displayName = "Ölmischungen",
                parentKey = "oils"
            ),

            root(
                key = "vinegar",
                displayName = "Essig"
            ),
            child(
                key = "wine-vinegar",
                displayName = "Weinessig",
                parentKey = "vinegar"
            ),
            child(
                key = "apple-vinegar",
                displayName = "Apfelessig",
                parentKey = "vinegar"
            ),
            child(
                key = "balsamic-vinegar",
                displayName = "Balsamessig",
                parentKey = "vinegar"
            ),
            child(
                key = "spirit-vinegar",
                displayName = "Branntweinessig",
                parentKey = "vinegar"
            ),
            child(
                key = "rice-vinegar",
                displayName = "Reisessig",
                parentKey = "vinegar"
            ),
            child(
                key = "fruit-vinegar",
                displayName = "Fruchtessig",
                parentKey = "vinegar"
            ),
            child(
                key = "seasoned-vinegar",
                displayName = "Gewürzter Essig",
                parentKey = "vinegar"
            ),

            root(
                key = "sauces",
                displayName = "Saucen"
            ),
            child(
                key = "tomato-sauces",
                displayName = "Tomatensaucen",
                parentKey = "sauces"
            ),
            child(
                key = "cream-sauces",
                displayName = "Sahnesaucen",
                parentKey = "sauces"
            ),
            child(
                key = "gravy",
                displayName = "Bratensaucen",
                parentKey = "sauces"
            ),
            child(
                key = "pesto",
                displayName = "Pesto",
                parentKey = "sauces"
            ),
            child(
                key = "mustard",
                displayName = "Senf",
                parentKey = "sauces"
            ),
            child(
                key = "ketchup",
                displayName = "Ketchup",
                parentKey = "sauces"
            ),
            child(
                key = "mayonnaise",
                displayName = "Mayonnaise",
                parentKey = "sauces"
            ),
            child(
                key = "salad-dressings",
                displayName = "Salatdressings",
                parentKey = "sauces"
            ),
            child(
                key = "asian-sauces",
                displayName = "Asiatische Saucen",
                parentKey = "sauces"
            ),
            child(
                key = "hot-sauces",
                displayName = "Scharfe Saucen",
                parentKey = "sauces"
            ),
            child(
                key = "dips",
                displayName = "Dips",
                parentKey = "sauces"
            ),

            root(
                key = "breakfast",
                displayName = "Frühstück"
            ),
            child(
                key = "porridge",
                displayName = "Porridge",
                parentKey = "breakfast"
            ),
            child(
                key = "breakfast-cereals",
                displayName = "Frühstückscerealien",
                parentKey = "breakfast"
            ),
            child(
                key = "pancake-products",
                displayName = "Pfannkuchenprodukte",
                parentKey = "breakfast"
            ),
            child(
                key = "breakfast-bakery",
                displayName = "Frühstücksbackwaren",
                parentKey = "breakfast"
            ),
            child(
                key = "breakfast-meat",
                displayName = "Frühstücksfleisch",
                parentKey = "breakfast"
            ),
            child(
                key = "breakfast-convenience",
                displayName = "Frühstücks-Convenience",
                parentKey = "breakfast"
            ),

            root(
                key = "muesli",
                displayName = "Müsli"
            ),
            child(
                key = "classic-muesli",
                displayName = "Klassisches Müsli",
                parentKey = "muesli"
            ),
            child(
                key = "fruit-muesli",
                displayName = "Früchtemüsli",
                parentKey = "muesli"
            ),
            child(
                key = "nut-muesli",
                displayName = "Nussmüsli",
                parentKey = "muesli"
            ),
            child(
                key = "chocolate-muesli",
                displayName = "Schokomüsli",
                parentKey = "muesli"
            ),
            child(
                key = "crunchy-muesli",
                displayName = "Knuspermüsli",
                parentKey = "muesli"
            ),
            child(
                key = "granola",
                displayName = "Granola",
                parentKey = "muesli"
            ),
            child(
                key = "protein-muesli",
                displayName = "Proteinmüsli",
                parentKey = "muesli"
            ),
            child(
                key = "gluten-free-muesli",
                displayName = "Glutenfreies Müsli",
                parentKey = "muesli"
            ),

            root(
                key = "spreads",
                displayName = "Brotaufstriche"
            ),
            child(
                key = "fruit-spreads",
                displayName = "Fruchtaufstriche",
                parentKey = "spreads"
            ),
            child(
                key = "nut-spreads",
                displayName = "Nussaufstriche",
                parentKey = "spreads"
            ),
            child(
                key = "chocolate-spreads",
                displayName = "Schokoaufstriche",
                parentKey = "spreads"
            ),
            child(
                key = "honey",
                displayName = "Honig",
                parentKey = "spreads"
            ),
            child(
                key = "syrup-spreads",
                displayName = "Sirupbasierte Aufstriche",
                parentKey = "spreads"
            ),
            child(
                key = "vegetable-spreads",
                displayName = "Gemüseaufstriche",
                parentKey = "spreads"
            ),
            child(
                key = "legume-spreads",
                displayName = "Hülsenfruchtaufstriche",
                parentKey = "spreads"
            ),
            child(
                key = "cheese-spreads",
                displayName = "Käseaufstriche",
                parentKey = "spreads"
            ),
            child(
                key = "meat-spreads",
                displayName = "Fleischaufstriche",
                parentKey = "spreads"
            ),
            child(
                key = "fish-spreads",
                displayName = "Fischaufstriche",
                parentKey = "spreads"
            ),

            root(
                key = "confectionery",
                displayName = "Süßwaren"
            ),
            child(
                key = "chocolate",
                displayName = "Schokolade",
                parentKey = "confectionery"
            ),
            child(
                key = "pralines",
                displayName = "Pralinen",
                parentKey = "confectionery"
            ),
            child(
                key = "candy",
                displayName = "Bonbons",
                parentKey = "confectionery"
            ),
            child(
                key = "gummies",
                displayName = "Fruchtgummi",
                parentKey = "confectionery"
            ),
            child(
                key = "liquorice",
                displayName = "Lakritz",
                parentKey = "confectionery"
            ),
            child(
                key = "marzipan",
                displayName = "Marzipan",
                parentKey = "confectionery"
            ),
            child(
                key = "nougat",
                displayName = "Nougat",
                parentKey = "confectionery"
            ),
            child(
                key = "chewing-gum",
                displayName = "Kaugummi",
                parentKey = "confectionery"
            ),
            child(
                key = "sugar-confectionery",
                displayName = "Zuckerwaren",
                parentKey = "confectionery"
            ),
            child(
                key = "seasonal-confectionery",
                displayName = "Saisonale Süßwaren",
                parentKey = "confectionery"
            ),

            root(
                key = "snacks",
                displayName = "Snacks"
            ),
            child(
                key = "potato-snacks",
                displayName = "Kartoffelsnacks",
                parentKey = "snacks"
            ),
            child(
                key = "corn-snacks",
                displayName = "Maissnacks",
                parentKey = "snacks"
            ),
            child(
                key = "rice-snacks",
                displayName = "Reissnacks",
                parentKey = "snacks"
            ),
            child(
                key = "cracker-snacks",
                displayName = "Cracker",
                parentKey = "snacks"
            ),
            child(
                key = "pretzel-snacks",
                displayName = "Laugengebäck-Snacks",
                parentKey = "snacks"
            ),
            child(
                key = "popcorn",
                displayName = "Popcorn",
                parentKey = "snacks"
            ),
            child(
                key = "vegetable-snacks",
                displayName = "Gemüsesnacks",
                parentKey = "snacks"
            ),
            child(
                key = "protein-snacks",
                displayName = "Proteinsnacks",
                parentKey = "snacks"
            ),
            child(
                key = "snack-mixes",
                displayName = "Snackmischungen",
                parentKey = "snacks"
            ),
            child(
                key = "bars",
                displayName = "Riegel",
                parentKey = "snacks"
            ),

            root(
                key = "nuts",
                displayName = "Nüsse"
            ),
            child(
                key = "tree-nuts",
                displayName = "Baumnüsse",
                parentKey = "nuts"
            ),
            child(
                key = "nut-mixtures",
                displayName = "Nussmischungen",
                parentKey = "nuts"
            ),
            child(
                key = "roasted-nuts",
                displayName = "Geröstete Nüsse",
                parentKey = "nuts"
            ),
            child(
                key = "seasoned-nuts",
                displayName = "Gewürzte Nüsse",
                parentKey = "nuts"
            ),
            child(
                key = "ground-nuts",
                displayName = "Gemahlene Nüsse",
                parentKey = "nuts"
            ),
            child(
                key = "nut-flours",
                displayName = "Nussmehle",
                parentKey = "nuts"
            ),

            root(
                key = "seeds",
                displayName = "Saaten"
            ),
            child(
                key = "sunflower-seeds",
                displayName = "Sonnenblumenkerne",
                parentKey = "seeds"
            ),
            child(
                key = "pumpkin-seeds",
                displayName = "Kürbiskerne",
                parentKey = "seeds"
            ),
            child(
                key = "sesame-seeds",
                displayName = "Sesam",
                parentKey = "seeds"
            ),
            child(
                key = "linseeds",
                displayName = "Leinsamen",
                parentKey = "seeds"
            ),
            child(
                key = "chia-seeds",
                displayName = "Chiasamen",
                parentKey = "seeds"
            ),
            child(
                key = "poppy-seeds",
                displayName = "Mohn",
                parentKey = "seeds"
            ),
            child(
                key = "hemp-seeds",
                displayName = "Hanfsamen",
                parentKey = "seeds"
            ),
            child(
                key = "seed-mixtures",
                displayName = "Saatenmischungen",
                parentKey = "seeds"
            ),
            child(
                key = "seed-pastes",
                displayName = "Saatenpasten",
                parentKey = "seeds"
            ),

            root(
                key = "beverages",
                displayName = "Getränke"
            ),
            child(
                key = "water",
                displayName = "Wasser",
                parentKey = "beverages"
            ),
            child(
                key = "fruit-juices",
                displayName = "Fruchtsäfte",
                parentKey = "beverages"
            ),
            child(
                key = "vegetable-juices",
                displayName = "Gemüsesäfte",
                parentKey = "beverages"
            ),
            child(
                key = "nectars",
                displayName = "Nektare",
                parentKey = "beverages"
            ),
            child(
                key = "soft-drinks",
                displayName = "Erfrischungsgetränke",
                parentKey = "beverages"
            ),
            child(
                key = "syrups",
                displayName = "Getränkesirupe",
                parentKey = "beverages"
            ),
            child(
                key = "tea",
                displayName = "Tee",
                parentKey = "beverages"
            ),
            child(
                key = "coffee",
                displayName = "Kaffee",
                parentKey = "beverages"
            ),
            child(
                key = "cocoa-drinks",
                displayName = "Kakaogetränke",
                parentKey = "beverages"
            ),
            child(
                key = "energy-drinks",
                displayName = "Energy-Drinks",
                parentKey = "beverages"
            ),
            child(
                key = "sports-drinks",
                displayName = "Sportgetränke",
                parentKey = "beverages"
            ),
            child(
                key = "plant-based-drinks",
                displayName = "Pflanzendrinks",
                parentKey = "beverages"
            ),
            child(
                key = "milk-mixed-drinks",
                displayName = "Milchmischgetränke",
                parentKey = "beverages"
            ),
            child(
                key = "fermented-drinks",
                displayName = "Fermentierte Getränke",
                parentKey = "beverages"
            ),
            child(
                key = "non-alcoholic-beer-and-wine",
                displayName = "Alkoholfreies Bier und alkoholfreier Wein",
                parentKey = "beverages"
            ),

            root(
                key = "international-food",
                displayName = "Internationale Küche"
            ),
            child(
                key = "italian-food",
                displayName = "Italienische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "french-food",
                displayName = "Französische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "spanish-food",
                displayName = "Spanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "greek-food",
                displayName = "Griechische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "turkish-food",
                displayName = "Türkische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "balkan-food",
                displayName = "Balkanküche",
                parentKey = "international-food"
            ),
            child(
                key = "middle-eastern-food",
                displayName = "Nahöstliche Küche",
                parentKey = "international-food"
            ),
            child(
                key = "north-african-food",
                displayName = "Nordafrikanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "west-african-food",
                displayName = "Westafrikanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "east-african-food",
                displayName = "Ostafrikanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "indian-food",
                displayName = "Indische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "pakistani-food",
                displayName = "Pakistanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "chinese-food",
                displayName = "Chinesische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "japanese-food",
                displayName = "Japanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "korean-food",
                displayName = "Koreanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "thai-food",
                displayName = "Thailändische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "vietnamese-food",
                displayName = "Vietnamesische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "indonesian-food",
                displayName = "Indonesische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "mexican-food",
                displayName = "Mexikanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "latin-american-food",
                displayName = "Lateinamerikanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "north-american-food",
                displayName = "Nordamerikanische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "caribbean-food",
                displayName = "Karibische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "eastern-european-food",
                displayName = "Osteuropäische Küche",
                parentKey = "international-food"
            ),
            child(
                key = "scandinavian-food",
                displayName = "Skandinavische Küche",
                parentKey = "international-food"
            ),

            root(
                key = "sugar-and-sweeteners",
                displayName = "Zucker und Süßungsmittel"
            ),
            child(
                key = "granulated-sugar",
                displayName = "Kristallzucker",
                parentKey = "sugar-and-sweeteners"
            ),
            child(
                key = "brown-sugar",
                displayName = "Brauner Zucker",
                parentKey = "sugar-and-sweeteners"
            ),
            child(
                key = "powdered-sugar",
                displayName = "Puderzucker",
                parentKey = "sugar-and-sweeteners"
            ),
            child(
                key = "sugar-specialities",
                displayName = "Zuckerspezialitäten",
                parentKey = "sugar-and-sweeteners"
            ),
            child(
                key = "natural-sweeteners",
                displayName = "Natürliche Süßungsmittel",
                parentKey = "sugar-and-sweeteners"
            ),
            child(
                key = "sugar-substitutes",
                displayName = "Zuckeraustauschstoffe",
                parentKey = "sugar-and-sweeteners"
            ),
            child(
                key = "intense-sweeteners",
                displayName = "Intensivsüßstoffe",
                parentKey = "sugar-and-sweeteners"
            ),

            root(
                key = "potato-products",
                displayName = "Kartoffelprodukte"
            ),
            child(
                key = "fresh-potatoes",
                displayName = "Frische Kartoffeln",
                parentKey = "potato-products"
            ),
            child(
                key = "potato-dough-products",
                displayName = "Kartoffelteigprodukte",
                parentKey = "potato-products"
            ),
            child(
                key = "mashed-potato-products",
                displayName = "Kartoffelpüreeprodukte",
                parentKey = "potato-products"
            ),
            child(
                key = "fried-potato-products",
                displayName = "Frittierte Kartoffelprodukte",
                parentKey = "potato-products"
            ),
            child(
                key = "potato-side-dishes",
                displayName = "Kartoffelbeilagen",
                parentKey = "potato-products"
            ),

            root(
                key = "desserts",
                displayName = "Desserts"
            ),
            child(
                key = "puddings",
                displayName = "Pudding",
                parentKey = "desserts"
            ),
            child(
                key = "creams-and-mousses",
                displayName = "Cremes und Mousses",
                parentKey = "desserts"
            ),
            child(
                key = "fruit-desserts",
                displayName = "Fruchtdesserts",
                parentKey = "desserts"
            ),
            child(
                key = "gelled-desserts",
                displayName = "Geleedesserts",
                parentKey = "desserts"
            ),
            child(
                key = "rice-desserts",
                displayName = "Reisdesserts",
                parentKey = "desserts"
            ),
            child(
                key = "semolina-desserts",
                displayName = "Grießdesserts",
                parentKey = "desserts"
            ),
            child(
                key = "plant-based-desserts",
                displayName = "Pflanzliche Desserts",
                parentKey = "desserts"
            ),

            root(
                key = "salads",
                displayName = "Salate"
            ),
            child(
                key = "leaf-salads",
                displayName = "Blattsalate",
                parentKey = "salads"
            ),
            child(
                key = "vegetable-salads",
                displayName = "Gemüsesalate",
                parentKey = "salads"
            ),
            child(
                key = "potato-salads",
                displayName = "Kartoffelsalate",
                parentKey = "salads"
            ),
            child(
                key = "pasta-salads",
                displayName = "Nudelsalate",
                parentKey = "salads"
            ),
            child(
                key = "grain-salads",
                displayName = "Getreidesalate",
                parentKey = "salads"
            ),
            child(
                key = "legume-salads",
                displayName = "Hülsenfruchtsalate",
                parentKey = "salads"
            ),
            child(
                key = "meat-salads",
                displayName = "Fleischsalate",
                parentKey = "salads"
            ),
            child(
                key = "fish-salads",
                displayName = "Fischsalate",
                parentKey = "salads"
            ),

            root(
                key = "dough-and-bases",
                displayName = "Teige und Böden"
            ),
            child(
                key = "pizza-dough",
                displayName = "Pizzateig",
                parentKey = "dough-and-bases"
            ),
            child(
                key = "puff-pastry",
                displayName = "Blätterteig",
                parentKey = "dough-and-bases"
            ),
            child(
                key = "shortcrust-pastry",
                displayName = "Mürbeteig",
                parentKey = "dough-and-bases"
            ),
            child(
                key = "yeast-dough",
                displayName = "Hefeteig",
                parentKey = "dough-and-bases"
            ),
            child(
                key = "strudel-dough",
                displayName = "Strudelteig",
                parentKey = "dough-and-bases"
            ),
            child(
                key = "wraps-and-tortillas",
                displayName = "Wraps und Tortillas",
                parentKey = "dough-and-bases"
            ),
            child(
                key = "cake-bases",
                displayName = "Torten- und Kuchenböden",
                parentKey = "dough-and-bases"
            ),

            root(
                key = "fermented-food",
                displayName = "Fermentierte Lebensmittel"
            ),
            child(
                key = "fermented-vegetables",
                displayName = "Fermentiertes Gemüse",
                parentKey = "fermented-food"
            ),
            child(
                key = "fermented-legumes",
                displayName = "Fermentierte Hülsenfrüchte",
                parentKey = "fermented-food"
            ),
            child(
                key = "fermented-grains",
                displayName = "Fermentiertes Getreide",
                parentKey = "fermented-food"
            ),
            child(
                key = "fermented-dairy",
                displayName = "Fermentierte Milchprodukte",
                parentKey = "fermented-food"
            ),
            child(
                key = "fermented-sauces",
                displayName = "Fermentierte Saucen",
                parentKey = "fermented-food"
            ),

            root(
                key = "baby-food",
                displayName = "Babynahrung"
            ),
            child(
                key = "baby-porridge",
                displayName = "Babybrei",
                parentKey = "baby-food"
            ),
            child(
                key = "baby-meals",
                displayName = "Babymenüs",
                parentKey = "baby-food"
            ),
            child(
                key = "baby-snacks",
                displayName = "Babysnacks",
                parentKey = "baby-food"
            ),
            child(
                key = "infant-formula",
                displayName = "Säuglingsnahrung",
                parentKey = "baby-food"
            ),
            child(
                key = "follow-on-formula",
                displayName = "Folgemilch",
                parentKey = "baby-food"
            ),
            child(
                key = "children-food",
                displayName = "Kleinkindnahrung",
                parentKey = "baby-food"
            )
        )

        val childKeysByParent = specifications
            .filter { it.parentKey != null }
            .groupBy { requireNotNull(it.parentKey) }
            .mapValues { (_, children) ->
                children
                    .mapTo(sortedSetOf()) { it.key }
            }

        return specifications
            .map { specification ->
                CatalogCategoryDefinition(
                    key = specification.key,
                    displayName = specification.displayName,
                    parentKey = specification.parentKey,
                    allowedChildKeys = childKeysByParent[specification.key]
                        ?: emptySet(),
                    foodOnly = specification.foodOnly
                )
            }
            .sortedWith(CATEGORY_COMPARATOR)
    }

    private fun validateDefinitions(
        definitions: List<CatalogCategoryDefinition>
    ) {
        require(definitions.isNotEmpty()) {
            "Canonical food category registry must not be empty."
        }

        validateKeys(definitions)
        validateDisplayNames(definitions)
        validateParentReferences(definitions)
        validateAllowedChildReferences(definitions)
        validateParentChildConsistency(definitions)
        validateFoodOnlyInvariant(definitions)
        validateCycles(definitions)
        validateReachability(definitions)
    }

    private fun validateKeys(
        definitions: List<CatalogCategoryDefinition>
    ) {
        definitions.forEach { definition ->
            require(definition.key.isNotBlank()) {
                "Canonical food category key must not be blank."
            }

            require(definition.key == normalizeKey(definition.key)) {
                "Canonical food category key '${definition.key}' is not normalized."
            }

            require(CATEGORY_KEY_REGEX.matches(definition.key)) {
                "Canonical food category key '${definition.key}' contains invalid characters."
            }
        }

        val duplicateKeys = definitions
            .groupBy { it.key }
            .filterValues { it.size > 1 }
            .keys
            .sorted()

        require(duplicateKeys.isEmpty()) {
            "Canonical food category registry contains duplicate keys: " +
                    duplicateKeys.joinToString(", ")
        }
    }

    private fun validateDisplayNames(
        definitions: List<CatalogCategoryDefinition>
    ) {
        definitions.forEach { definition ->
            require(definition.displayName.isNotBlank()) {
                "Display name for category '${definition.key}' must not be blank."
            }

            require(definition.displayName == definition.displayName.trim()) {
                "Display name for category '${definition.key}' contains surrounding whitespace."
            }
        }

        val duplicateDisplayNames = definitions
            .groupBy {
                it.displayName
                    .lowercase(Locale.ROOT)
                    .trim()
            }
            .filterValues { it.size > 1 }
            .mapValues { (_, values) ->
                values.map { it.key }.sorted()
            }

        require(duplicateDisplayNames.isEmpty()) {
            "Canonical food category registry contains duplicate display names: " +
                    duplicateDisplayNames.entries
                        .sortedBy { it.key }
                        .joinToString("; ") { (displayName, keys) ->
                            "'$displayName' -> ${keys.joinToString(", ")}"
                        }
        }
    }

    private fun validateParentReferences(
        definitions: List<CatalogCategoryDefinition>
    ) {
        val keys = definitions.mapTo(hashSetOf()) { it.key }

        val unknownParentReferences = definitions
            .mapNotNull { definition ->
                definition.parentKey
                    ?.takeUnless { it in keys }
                    ?.let { unknownParent ->
                        "${definition.key} -> $unknownParent"
                    }
            }
            .sorted()

        require(unknownParentReferences.isEmpty()) {
            "Canonical food category registry contains unknown parent references: " +
                    unknownParentReferences.joinToString(", ")
        }

        val selfReferences = definitions
            .filter { it.parentKey == it.key }
            .map { it.key }
            .sorted()

        require(selfReferences.isEmpty()) {
            "Canonical food categories must not reference themselves as parent: " +
                    selfReferences.joinToString(", ")
        }
    }

    private fun validateAllowedChildReferences(
        definitions: List<CatalogCategoryDefinition>
    ) {
        val keys = definitions.mapTo(hashSetOf()) { it.key }

        val unknownChildReferences = definitions
            .flatMap { definition ->
                definition.allowedChildKeys
                    .filterNot { it in keys }
                    .map { unknownChild ->
                        "${definition.key} -> $unknownChild"
                    }
            }
            .sorted()

        require(unknownChildReferences.isEmpty()) {
            "Canonical food category registry contains unknown child references: " +
                    unknownChildReferences.joinToString(", ")
        }

        val selfChildReferences = definitions
            .filter { it.key in it.allowedChildKeys }
            .map { it.key }
            .sorted()

        require(selfChildReferences.isEmpty()) {
            "Canonical food categories must not contain themselves as allowed child: " +
                    selfChildReferences.joinToString(", ")
        }
    }

    private fun validateParentChildConsistency(
        definitions: List<CatalogCategoryDefinition>
    ) {
        val byKey = definitions.associateBy { it.key }

        val inconsistentParentAssignments = definitions
            .mapNotNull { child ->
                val parentKey = child.parentKey ?: return@mapNotNull null
                val parent = byKey.getValue(parentKey)

                if (child.key !in parent.allowedChildKeys) {
                    "${child.key} declares parent $parentKey, " +
                            "but $parentKey does not declare ${child.key} as child"
                } else {
                    null
                }
            }
            .sorted()

        require(inconsistentParentAssignments.isEmpty()) {
            "Canonical food category registry contains inconsistent parent assignments: " +
                    inconsistentParentAssignments.joinToString("; ")
        }

        val inconsistentChildAssignments = definitions
            .flatMap { parent ->
                parent.allowedChildKeys.mapNotNull { childKey ->
                    val child = byKey.getValue(childKey)

                    if (child.parentKey != parent.key) {
                        "${parent.key} declares child $childKey, " +
                                "but $childKey declares parent ${child.parentKey}"
                    } else {
                        null
                    }
                }
            }
            .sorted()

        require(inconsistentChildAssignments.isEmpty()) {
            "Canonical food category registry contains inconsistent child assignments: " +
                    inconsistentChildAssignments.joinToString("; ")
        }
    }

    private fun validateFoodOnlyInvariant(
        definitions: List<CatalogCategoryDefinition>
    ) {
        val nonFoodDefinitions = definitions
            .filterNot { it.foodOnly }
            .map { it.key }
            .sorted()

        require(nonFoodDefinitions.isEmpty()) {
            "Canonical food category registry must contain only food categories. " +
                    "Invalid categories: ${nonFoodDefinitions.joinToString(", ")}"
        }
    }

    private fun validateCycles(
        definitions: List<CatalogCategoryDefinition>
    ) {
        val byKey = definitions.associateBy { it.key }
        val visited = mutableSetOf<String>()
        val active = mutableSetOf<String>()

        fun visit(key: String) {
            if (key in active) {
                val cycle = (active + key)
                    .sorted()
                    .joinToString(" -> ")

                error(
                    "Canonical food category registry contains a category cycle: $cycle"
                )
            }

            if (!visited.add(key)) {
                return
            }

            active += key

            byKey.getValue(key)
                .allowedChildKeys
                .sorted()
                .forEach(::visit)

            active -= key
        }

        definitions
            .filter { it.parentKey == null }
            .map { it.key }
            .sorted()
            .forEach(::visit)

        require(visited.size == definitions.size) {
            val unvisited = definitions
                .map { it.key }
                .filterNot { it in visited }
                .sorted()

            "Canonical food category registry contains unreachable or cyclic categories: " +
                    unvisited.joinToString(", ")
        }
    }

    private fun validateReachability(
        definitions: List<CatalogCategoryDefinition>
    ) {
        val roots = definitions
            .filter { it.parentKey == null }

        require(roots.isNotEmpty()) {
            "Canonical food category registry must contain at least one root category."
        }

        val byKey = definitions.associateBy { it.key }
        val reachable = mutableSetOf<String>()
        val pending = ArrayDeque<String>()

        roots
            .map { it.key }
            .sorted()
            .forEach(pending::addLast)

        while (pending.isNotEmpty()) {
            val key = pending.removeFirst()

            if (!reachable.add(key)) {
                continue
            }

            byKey.getValue(key)
                .allowedChildKeys
                .sorted()
                .forEach(pending::addLast)
        }

        val unreachable = definitions
            .map { it.key }
            .filterNot { it in reachable }
            .sorted()

        require(unreachable.isEmpty()) {
            "Canonical food category registry contains unreachable categories: " +
                    unreachable.joinToString(", ")
        }
    }

    private fun root(
        key: String,
        displayName: String
    ): CategorySpecification =
        CategorySpecification(
            key = normalizeKey(key),
            displayName = displayName,
            parentKey = null,
            foodOnly = true
        )

    private fun child(
        key: String,
        displayName: String,
        parentKey: String
    ): CategorySpecification =
        CategorySpecification(
            key = normalizeKey(key),
            displayName = displayName,
            parentKey = normalizeKey(parentKey),
            foodOnly = true
        )

    private fun normalizeKey(value: String): String =
        value
            .trim()
            .lowercase(Locale.ROOT)
            .replace(UNDERSCORE_OR_WHITESPACE_REGEX, "-")
            .replace(MULTIPLE_HYPHEN_REGEX, "-")
            .trim('-')

    private data class CategorySpecification(
        val key: String,
        val displayName: String,
        val parentKey: String?,
        val foodOnly: Boolean
    )

    private companion object {

        const val PATH_SEPARATOR = " > "

        val CATEGORY_KEY_REGEX = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")
        val UNDERSCORE_OR_WHITESPACE_REGEX = Regex("[_\\s]+")
        val MULTIPLE_HYPHEN_REGEX = Regex("-+")

        val CATEGORY_COMPARATOR =
            compareBy<CatalogCategoryDefinition>(
                { it.parentKey ?: "" },
                { it.displayName.lowercase(Locale.ROOT) },
                { it.key }
            )
    }
}