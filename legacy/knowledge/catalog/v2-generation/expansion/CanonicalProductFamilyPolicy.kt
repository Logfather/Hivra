package de.shopme.testing.system.tools.knowledge.catalog.expansion.family

object CanonicalProductFamilyPolicy {

    private fun defaultAxes() =
        axes(
            CanonicalProductFamilyVariantAxis.FOOD_TYPE,
            CanonicalProductFamilyVariantAxis.PRIMARY_INGREDIENT,
            CanonicalProductFamilyVariantAxis.RECIPE_TYPE,
            CanonicalProductFamilyVariantAxis.PROCESSING_METHOD,
            CanonicalProductFamilyVariantAxis.PRESERVATION_METHOD,
            CanonicalProductFamilyVariantAxis.PREPARATION_STATE,
            CanonicalProductFamilyVariantAxis.PHYSICAL_FORM,
            CanonicalProductFamilyVariantAxis.FLAVOR_PROFILE,
            CanonicalProductFamilyVariantAxis.DIETARY_FORM,
            CanonicalProductFamilyVariantAxis.ALLERGEN_RELEVANT_VARIANT,
            CanonicalProductFamilyVariantAxis.NUTRITIONALLY_RELEVANT_VARIANT
        )

    val FAMILIES: List<CanonicalProductFamily> =
        buildList<CanonicalProductFamily> {

            addAll(
                category(
                    category = "bakery",
                    axes = axes(
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        CanonicalProductFamilyVariantAxis.RECIPE_TYPE,
                        CanonicalProductFamilyVariantAxis.PROCESSING_METHOD,
                        CanonicalProductFamilyVariantAxis.PHYSICAL_FORM
                    ),
                    families = listOf(
                        family("bread", "Brot", 28),
                        family("bread-rolls", "Brötchen", 16),
                        family("toast-bread", "Toastbrot", 8),
                        family("crispbread", "Knäckebrot", 6),
                        family("flatbread", "Fladenbrot", 7),
                        family("baguette", "Baguette", 7),
                        family("pastries", "Feingebäck", 10),
                        family("cakes", "Kuchen", 8),
                        family("pies-and-tarts", "Torten und Tartes", 5),
                        family("sweet-rolls", "Süße Kleingebäcke", 5)
                    )
                )
            )

            addAll(
                category(
                    category = "baking-ingredients",
                    axes = axes(
                        CanonicalProductFamilyVariantAxis.FOOD_TYPE,
                        CanonicalProductFamilyVariantAxis.PRIMARY_INGREDIENT,
                        CanonicalProductFamilyVariantAxis.PHYSICAL_FORM
                    ),
                    families = listOf(
                        family("raising-agents", "Backtriebmittel", 14),
                        family("baking-flavors", "Backaromen", 10),
                        family("cake-decorations", "Backdekoration", 12),
                        family("baking-chocolate", "Backschokolade", 14),
                        family("cocoa-products", "Kakaoerzeugnisse", 14),
                        family("baking-nuts", "Backnüsse", 12),
                        family("baking-fruit", "Backfrüchte", 10),
                        family("gelling-agents", "Geliermittel", 8),
                        family("dessert-mixes", "Dessert- und Backmischungen", 6)
                    )
                )
            )

            addAll(
                category(
                    category = "beverages",
                    axes = axes(
                        CanonicalProductFamilyVariantAxis.FOOD_TYPE,
                        CanonicalProductFamilyVariantAxis.PRIMARY_INGREDIENT,
                        CanonicalProductFamilyVariantAxis.FLAVOR_PROFILE,
                        CanonicalProductFamilyVariantAxis.SWEETENING_TYPE,
                        CanonicalProductFamilyVariantAxis
                            .NUTRITIONALLY_RELEVANT_VARIANT
                    ),
                    families = listOf(
                        family("mineral-water", "Mineralwasser", 14),
                        family("table-water", "Tafelwasser", 4),
                        family("fruit-juice", "Fruchtsaft", 14),
                        family("vegetable-juice", "Gemüsesaft", 5),
                        family("fruit-nectar", "Fruchtnektar", 8),
                        family("fruit-drinks", "Fruchtgetränke", 7),
                        family("lemonade", "Limonade", 9),
                        family("cola", "Colagetränke", 6),
                        family("iced-tea", "Eistee", 7),
                        family("energy-drinks", "Energydrinks", 5),
                        family("sports-drinks", "Sportgetränke", 4),
                        family("coffee", "Kaffee", 8),
                        family("tea", "Tee", 8),
                        family("cocoa-drinks", "Kakaogetränke", 3),
                        family("malt-drinks", "Malzgetränke", 3)
                    )
                )
            )

            addAll(
                category(
                    category = "breakfast",
                    axes = defaultAxes(),
                    families = listOf(
                        family("muesli", "Müsli", 22),
                        family("breakfast-cereals", "Frühstückscerealien", 18),
                        family("porridge", "Porridge", 13),
                        family("oat-meals", "Hafermahlzeiten", 9),
                        family("breakfast-bars", "Frühstücksriegel", 8),
                        family("breakfast-mixes", "Frühstücksmischungen", 8),
                        family("sweet-breakfast", "Süße Frühstücksprodukte", 12),
                        family("savory-breakfast", "Herzhafte Frühstücksprodukte", 10)
                    )
                )
            )
            addAll(
                category(
                    category = "canned-food",
                    axes = defaultAxes(),
                    families = listOf(
                        family("canned-vegetables", "Gemüsekonserven", 22),
                        family("canned-legumes", "Hülsenfruchtkonserven", 14),
                        family("canned-fruit", "Obstkonserven", 12),
                        family("canned-fish", "Fischkonserven", 16),
                        family("canned-meat", "Fleischkonserven", 8),
                        family("canned-soups", "Suppenkonserven", 8),
                        family("canned-meals", "Fertiggerichtkonserven", 10),
                        family("pickled-food", "Eingelegte Lebensmittel", 10)
                    )
                )
            )

            addAll(
                category(
                    category = "confectionery",
                    axes = defaultAxes(),
                    families = listOf(
                        family("milk-chocolate", "Milchschokolade", 14),
                        family("dark-chocolate", "Dunkle Schokolade", 12),
                        family("white-chocolate", "Weiße Schokolade", 7),
                        family("filled-chocolate", "Gefüllte Schokolade", 10),
                        family("pralines", "Pralinen", 10),
                        family("chocolate-bars", "Schokoladenriegel", 10),
                        family("hard-candy", "Hartkaramellen", 7),
                        family("soft-candy", "Weichkaramellen", 6),
                        family("fruit-gums", "Fruchtgummi", 8),
                        family("liquorice", "Lakritz", 5),
                        family("marzipan", "Marzipan", 5),
                        family("chewing-gum", "Kaugummi", 6)
                    )
                )
            )

            addAll(
                category(
                    category = "dairy",
                    axes = axes(
                        CanonicalProductFamilyVariantAxis.FOOD_TYPE,
                        CanonicalProductFamilyVariantAxis.FAT_LEVEL,
                        CanonicalProductFamilyVariantAxis.PROCESSING_METHOD,
                        CanonicalProductFamilyVariantAxis.RIPENING_OR_AGING,
                        CanonicalProductFamilyVariantAxis.FLAVOR_PROFILE
                    ),
                    families = listOf(
                        family("milk", "Milch", 15),
                        family("cream", "Sahne", 8),
                        family("yogurt", "Joghurt", 14),
                        family("quark", "Quark", 8),
                        family("fresh-cheese", "Frischkäse", 8),
                        family("soft-cheese", "Weichkäse", 8),
                        family("semi-hard-cheese", "Schnittkäse", 10),
                        family("hard-cheese", "Hartkäse", 8),
                        family("blue-cheese", "Blauschimmelkäse", 5),
                        family("butter", "Butter", 6),
                        family("cultured-dairy", "Sauermilchprodukte", 6),
                        family("dairy-desserts", "Milchdesserts", 4)
                    )
                )
            )

            addAll(
                category(
                    category = "fish",
                    axes = defaultAxes(),
                    families = listOf(
                        family("fresh-fish", "Frischfisch", 24),
                        family("fish-fillets", "Fischfilets", 18),
                        family("smoked-fish", "Räucherfisch", 10),
                        family("salted-fish", "Gesalzener Fisch", 5),
                        family("crustaceans", "Krustentiere", 12),
                        family("molluscs", "Weichtiere", 10),
                        family("fish-products", "Fischerzeugnisse", 11),
                        family("seafood-mixtures", "Meeresfrüchtemischungen", 10)
                    )
                )
            )

            addAll(
                category(
                    category = "flour",
                    axes = axes(
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        CanonicalProductFamilyVariantAxis.PROCESSING_METHOD,
                        CanonicalProductFamilyVariantAxis.PHYSICAL_FORM
                    ),
                    families = listOf(
                        family("wheat-flour", "Weizenmehl", 24),
                        family("rye-flour", "Roggenmehl", 16),
                        family("spelt-flour", "Dinkelmehl", 16),
                        family("oat-flour", "Hafermehl", 8),
                        family("corn-flour", "Maismehl", 8),
                        family("rice-flour", "Reismehl", 8),
                        family("legume-flour", "Hülsenfruchtmehl", 10),
                        family("nut-flour", "Nussmehl", 6),
                        family("specialty-flour", "Spezialmehle", 4)
                    )
                )
            )

            addAll(
                category(
                    category = "fruit",
                    axes = axes(
                        CanonicalProductFamilyVariantAxis.PLANT_SPECIES,
                        CanonicalProductFamilyVariantAxis.PREPARATION_STATE,
                        CanonicalProductFamilyVariantAxis.PRESERVATION_METHOD,
                        CanonicalProductFamilyVariantAxis.PHYSICAL_FORM
                    ),
                    families = listOf(
                        family("pome-fruit", "Kernobst", 15),
                        family("stone-fruit", "Steinobst", 15),
                        family("berries", "Beeren", 17),
                        family("citrus-fruit", "Zitrusfrüchte", 12),
                        family("tropical-fruit", "Tropische Früchte", 14),
                        family("melons", "Melonen", 6),
                        family("grapes", "Trauben", 5),
                        family("dried-fruit", "Trockenfrüchte", 9),
                        family("prepared-fruit", "Zubereitetes Obst", 7)
                    )
                )
            )

            addAll(
                category(
                    category = "grains",
                    axes = defaultAxes(),
                    families = listOf(
                        family("wheat-grains", "Weizenkörner", 15),
                        family("rye-grains", "Roggenkörner", 10),
                        family("spelt-grains", "Dinkelkörner", 10),
                        family("oats", "Hafer", 14),
                        family("barley", "Gerste", 10),
                        family("millet", "Hirse", 9),
                        family("corn-grains", "Maisprodukte", 10),
                        family("pseudo-cereals", "Pseudogetreide", 12),
                        family("grain-mixes", "Getreidemischungen", 10)
                    )
                )
            )

            addAll(
                category(
                    category = "legumes",
                    axes = defaultAxes(),
                    families = listOf(
                        family("beans", "Bohnen", 24),
                        family("lentils", "Linsen", 22),
                        family("peas", "Erbsen", 16),
                        family("chickpeas", "Kichererbsen", 14),
                        family("soybeans", "Sojabohnen", 10),
                        family("lupins", "Lupinen", 6),
                        family("legume-mixes", "Hülsenfruchtmischungen", 8)
                    )
                )
            )

            addAll(
                category(
                    category = "meat",
                    axes = axes(
                        CanonicalProductFamilyVariantAxis.ANIMAL_SPECIES,
                        CanonicalProductFamilyVariantAxis.CUT_FORM,
                        CanonicalProductFamilyVariantAxis.PROCESSING_METHOD,
                        CanonicalProductFamilyVariantAxis.PREPARATION_STATE
                    ),
                    families = listOf(
                        family("beef", "Rindfleisch", 22),
                        family("pork", "Schweinefleisch", 22),
                        family("veal", "Kalbfleisch", 8),
                        family("lamb", "Lammfleisch", 8),
                        family("chicken", "Hähnchenfleisch", 14),
                        family("turkey", "Putenfleisch", 10),
                        family("duck-and-goose", "Enten- und Gänsefleisch", 5),
                        family("game", "Wildfleisch", 6),
                        family("offal", "Innereien", 5)
                    )
                )
            )

            addAll(
                category(
                    category = "oils",
                    axes = defaultAxes(),
                    families = listOf(
                        family("olive-oil", "Olivenöl", 20),
                        family("rapeseed-oil", "Rapsöl", 15),
                        family("sunflower-oil", "Sonnenblumenöl", 13),
                        family("seed-oils", "Saatenöle", 18),
                        family("nut-oils", "Nussöle", 14),
                        family("coconut-oil", "Kokosöl", 8),
                        family("blended-oils", "Mischöle", 7),
                        family("frying-fats", "Brat- und Frittierfette", 5)
                    )
                )
            )

            addAll(
                category(
                    category = "pasta",
                    axes = defaultAxes(),
                    families = listOf(
                        family("wheat-pasta", "Weizennudeln", 25),
                        family("egg-pasta", "Eiernudeln", 16),
                        family("wholegrain-pasta", "Vollkornnudeln", 12),
                        family("spelt-pasta", "Dinkelnudeln", 9),
                        family("legume-pasta", "Hülsenfruchtnudeln", 10),
                        family("gluten-free-pasta", "Glutenfreie Nudeln", 10),
                        family("filled-pasta", "Gefüllte Pasta", 12),
                        family("asian-noodles", "Asiatische Nudeln", 6)
                    )
                )
            )

            addAll(
                category(
                    category = "plant-based-alternatives",
                    axes = defaultAxes(),
                    families = listOf(
                        family("meat-alternatives", "Fleischalternativen", 25),
                        family("sausage-alternatives", "Wurstalternativen", 13),
                        family("cheese-alternatives", "Käsealternativen", 12),
                        family("yogurt-alternatives", "Joghurtalternativen", 10),
                        family("cream-alternatives", "Sahnealternativen", 7),
                        family("butter-alternatives", "Butteralternativen", 7),
                        family("egg-alternatives", "Eialternativen", 6),
                        family("tofu", "Tofu", 12),
                        family("tempeh", "Tempeh", 5),
                        family("seitan", "Seitan", 3)
                    )
                )
            )

            addAll(
                category(
                    category = "plant-based-drinks",
                    axes = defaultAxes(),
                    families = listOf(
                        family("oat-drinks", "Haferdrinks", 22),
                        family("soy-drinks", "Sojadrinks", 18),
                        family("almond-drinks", "Mandeldrinks", 13),
                        family("rice-drinks", "Reisdrinks", 11),
                        family("coconut-drinks", "Kokosdrinks", 9),
                        family("pea-drinks", "Erbsendrinks", 8),
                        family("nut-drinks", "Nussdrinks", 10),
                        family("grain-drinks", "Getreidedrinks", 9)
                    )
                )
            )

            addAll(
                category(
                    category = "ready-meals",
                    axes = defaultAxes(),
                    families = listOf(
                        family("pizza", "Pizza", 11),
                        family("pasta-meals", "Nudelgerichte", 10),
                        family("rice-meals", "Reisgerichte", 9),
                        family("potato-meals", "Kartoffelgerichte", 8),
                        family("meat-meals", "Fleischgerichte", 10),
                        family("fish-meals", "Fischgerichte", 6),
                        family("vegetable-meals", "Gemüsegerichte", 8),
                        family("vegetarian-meals", "Vegetarische Gerichte", 8),
                        family("vegan-meals", "Vegane Gerichte", 8),
                        family("soups", "Suppen", 7),
                        family("stews", "Eintöpfe", 7),
                        family("international-meals", "Internationale Gerichte", 8)
                    )
                )
            )

            addAll(
                category(
                    category = "rice",
                    axes = defaultAxes(),
                    families = listOf(
                        family("long-grain-rice", "Langkornreis", 18),
                        family("short-grain-rice", "Rundkornreis", 14),
                        family("basmati-rice", "Basmatireis", 16),
                        family("jasmine-rice", "Jasminreis", 12),
                        family("brown-rice", "Naturreis", 14),
                        family("risotto-rice", "Risottoreis", 10),
                        family("wild-rice", "Wildreis", 7),
                        family("rice-mixtures", "Reismischungen", 9)
                    )
                )
            )

            addAll(
                category(
                    category = "sauces",
                    axes = defaultAxes(),
                    families = listOf(
                        family("tomato-sauces", "Tomatensaucen", 14),
                        family("cream-sauces", "Sahnesaucen", 9),
                        family("cheese-sauces", "Käsesaucen", 8),
                        family("barbecue-sauces", "Barbecuesaucen", 8),
                        family("chili-sauces", "Chilisaucen", 8),
                        family("soy-sauces", "Sojasaucen", 7),
                        family("asian-sauces", "Asiatische Saucen", 10),
                        family("salad-dressings", "Salatdressings", 11),
                        family("mayonnaise-sauces", "Mayonnaisebasierte Saucen", 8),
                        family("mustard-sauces", "Senfsaucen", 7),
                        family("dessert-sauces", "Dessertsaucen", 4),
                        family("cooking-sauces", "Kochsaucen", 6)
                    )
                )
            )

            addAll(
                category(
                    category = "sausage",
                    axes = defaultAxes(),
                    families = listOf(
                        family("boiled-sausage", "Brühwurst", 17),
                        family("raw-sausage", "Rohwurst", 15),
                        family("cooked-sausage", "Kochwurst", 12),
                        family("spreadable-sausage", "Streichwurst", 10),
                        family("ham", "Schinken", 14),
                        family("salami", "Salami", 12),
                        family("poultry-sausage", "Geflügelwurst", 8),
                        family("regional-sausage", "Regionale Wurstspezialitäten", 7),
                        family("sausage-products", "Wursterzeugnisse", 5)
                    )
                )
            )

            addAll(
                category(
                    category = "snacks",
                    axes = defaultAxes(),
                    families = listOf(
                        family("potato-chips", "Kartoffelchips", 14),
                        family("tortilla-chips", "Tortillachips", 8),
                        family("vegetable-chips", "Gemüsechips", 7),
                        family("crackers", "Cracker", 12),
                        family("pretzel-snacks", "Laugengebäck-Snacks", 10),
                        family("popcorn", "Popcorn", 7),
                        family("nuts", "Nüsse", 12),
                        family("seed-snacks", "Kerne und Saaten", 8),
                        family("snack-mixes", "Snackmischungen", 8),
                        family("rice-snacks", "Reissnacks", 6),
                        family("corn-snacks", "Maissnacks", 8)
                    )
                )
            )

            addAll(
                category(
                    category = "spices",
                    axes = defaultAxes(),
                    families = listOf(
                        family("single-spices", "Einzelgewürze", 34),
                        family("herbs", "Kräuter", 18),
                        family("spice-blends", "Gewürzmischungen", 18),
                        family("seasoning-salts", "Gewürzsalze", 8),
                        family("pepper-products", "Pfefferprodukte", 8),
                        family("chili-products", "Chiliprodukte", 8),
                        family("seasoning-pastes", "Würzpasten", 6)
                    )
                )
            )

            addAll(
                category(
                    category = "spreads",
                    axes = defaultAxes(),
                    families = listOf(
                        family("fruit-spreads", "Fruchtaufstriche", 18),
                        family("nut-spreads", "Nussaufstriche", 15),
                        family("chocolate-spreads", "Schokoladenaufstriche", 12),
                        family("honey", "Honig", 10),
                        family("vegetable-spreads", "Gemüseaufstriche", 12),
                        family("legume-spreads", "Hülsenfruchtaufstriche", 10),
                        family("cheese-spreads", "Käseaufstriche", 9),
                        family("meat-spreads", "Fleischaufstriche", 7),
                        family("yeast-spreads", "Hefeaufstriche", 3),
                        family("mixed-spreads", "Gemischte Aufstriche", 4)
                    )
                )
            )

            addAll(
                category(
                    category = "vegetables",
                    axes = axes(
                        CanonicalProductFamilyVariantAxis.PLANT_SPECIES,
                        CanonicalProductFamilyVariantAxis.PREPARATION_STATE,
                        CanonicalProductFamilyVariantAxis.PRESERVATION_METHOD,
                        CanonicalProductFamilyVariantAxis.CUT_FORM,
                        CanonicalProductFamilyVariantAxis.PROCESSING_METHOD
                    ),
                    families = listOf(
                        family("leafy-vegetables", "Blattgemüse", 13),
                        family("root-vegetables", "Wurzelgemüse", 13),
                        family("tuber-vegetables", "Knollengemüse", 10),
                        family("cabbage", "Kohlgemüse", 12),
                        family("fruit-vegetables", "Fruchtgemüse", 13),
                        family("onion-vegetables", "Zwiebelgemüse", 8),
                        family("stem-vegetables", "Stängelgemüse", 7),
                        family("flower-vegetables", "Blütengemüse", 7),
                        family("mushrooms", "Pilze", 7),
                        family("sprouts", "Sprossen und Keimlinge", 4),
                        family("sea-vegetables", "Meeresgemüse", 2),
                        family("vegetable-mixtures", "Gemüsemischungen", 4)
                    )
                )
            )
        }
            .sortedWith(
                compareBy<CanonicalProductFamily> {
                    it.category
                }.thenBy {
                    it.key
                }
            )

    init {
        require(FAMILIES.isNotEmpty())

        require(
            FAMILIES.map {
                it.category to it.key
            }.distinct().size ==
                    FAMILIES.size
        ) {
            "Canonical product-family keys must be unique per category."
        }

        require(
            FAMILIES.groupingBy {
                it.key
            }.eachCount()
                .none { (_, count) ->
                    count > 1
                }
        ) {
            "Canonical product-family keys must be globally unique."
        }

        require(
            FAMILIES.map {
                it.category
            }.toSet() ==
                    requiredCategories()
        ) {
            "Product-family policy must cover all canonical categories."
        }
    }

    fun familiesFor(
        category: String
    ): List<CanonicalProductFamily> =
        FAMILIES.filter {
            it.category == category
        }

    fun categories(): Set<String> =
        FAMILIES
            .map {
                it.category
            }
            .toSortedSet()

    private fun category(
        category: String,
        axes: List<CanonicalProductFamilyVariantAxis>,
        families: List<FamilyDefinition>
    ): List<CanonicalProductFamily> =
        families.map { definition ->
            CanonicalProductFamily(
                key = definition.key,
                category = category,
                displayName = definition.displayName,
                allocationWeight = definition.weight,
                allowedVariantAxes = axes,
                rationale =
                    "Family '${definition.displayName}' represents a " +
                            "canonical food family within category '$category'. " +
                            "Retail brand, EAN, package size, retailer and price " +
                            "do not create separate canonical entries."
            )
        }

    private fun family(
        key: String,
        displayName: String,
        weight: Int
    ): FamilyDefinition =
        FamilyDefinition(
            key = key,
            displayName = displayName,
            weight = weight
        )

    private fun axes(
        vararg axes:
        CanonicalProductFamilyVariantAxis
    ): List<CanonicalProductFamilyVariantAxis> =
        axes
            .distinct()
            .sortedBy {
                it.name
            }

    private data class FamilyDefinition(
        val key: String,
        val displayName: String,
        val weight: Int
    )

    private fun requiredCategories(): Set<String> =
        sortedSetOf(
            "bakery",
            "baking-ingredients",
            "beverages",
            "breakfast",
            "canned-food",
            "confectionery",
            "dairy",
            "fish",
            "flour",
            "fruit",
            "grains",
            "legumes",
            "meat",
            "oils",
            "pasta",
            "plant-based-alternatives",
            "plant-based-drinks",
            "ready-meals",
            "rice",
            "sauces",
            "sausage",
            "snacks",
            "spices",
            "spreads",
            "vegetables"
        )
}