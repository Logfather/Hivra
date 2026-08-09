package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

object CanonicalVariantValuePolicy {

    val AXIS_VALUE_SETS:
            List<CanonicalVariantAxisValueSet> =
        listOf(
            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.FOOD_TYPE,

                values =
                    values(
                        value("single-food", "Einzellebensmittel"),
                        value("food-mixture", "Lebensmittelmischung"),
                        value("prepared-food", "Zubereitetes Lebensmittel"),
                        value("ingredient", "Lebensmittelzutat"),
                        value("ready-to-eat", "Verzehrfertiges Lebensmittel"),
                        value("ready-to-cook", "Kochfertiges Lebensmittel"),
                        value("concentrate", "Konzentrat"),
                        value("extract", "Extrakt"),
                        value("fermented-food", "Fermentiertes Lebensmittel"),
                        value("cultured-food", "Kultiviertes Lebensmittel"),
                        value("filled-food", "Gefülltes Lebensmittel"),
                        value("coated-food", "Überzogenes Lebensmittel")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.PRIMARY_INGREDIENT,

                values =
                    values(
                        value("wheat", "Weizen"),
                        value("rye", "Roggen"),
                        value("spelt", "Dinkel"),
                        value("oats", "Hafer"),
                        value("barley", "Gerste"),
                        value("corn", "Mais"),
                        value("rice", "Reis"),
                        value("potato", "Kartoffel"),
                        value("soy", "Soja"),
                        value("pea", "Erbse"),
                        value("chickpea", "Kichererbse"),
                        value("lentil", "Linse"),
                        value("bean", "Bohne"),
                        value("milk", "Milch"),
                        value("beef", "Rind"),
                        value("pork", "Schwein"),
                        value("chicken", "Hähnchen"),
                        value("turkey", "Pute"),
                        value("fish", "Fisch"),
                        value("apple", "Apfel"),
                        value("orange", "Orange"),
                        value("tomato", "Tomate"),
                        value("carrot", "Karotte"),
                        value("cocoa", "Kakao"),
                        value("almond", "Mandel"),
                        value("hazelnut", "Haselnuss"),
                        value("coconut", "Kokos"),
                        value("rapeseed", "Raps"),
                        value("sunflower", "Sonnenblume"),
                        value("olive", "Olive")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.SECONDARY_INGREDIENT,

                values =
                    values(
                        value("fruit", "Obst"),
                        value("vegetable", "Gemüse"),
                        value("herbs", "Kräuter"),
                        value("spices", "Gewürze"),
                        value("nuts", "Nüsse"),
                        value("seeds", "Saaten"),
                        value("cheese", "Käse"),
                        value("cream", "Sahne"),
                        value("chocolate", "Schokolade"),
                        value("cocoa", "Kakao"),
                        value("vanilla", "Vanille"),
                        value("honey", "Honig"),
                        value("egg", "Ei"),
                        value("meat", "Fleisch"),
                        value("fish", "Fisch"),
                        value("mushroom", "Pilz"),
                        value("tomato", "Tomate"),
                        value("onion", "Zwiebel"),
                        value("garlic", "Knoblauch"),
                        value("chili", "Chili")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.RECIPE_TYPE,

                values =
                    values(
                        value("plain", "Natur"),
                        value("classic", "Klassisch"),
                        value("traditional", "Traditionell"),
                        value("seasoned", "Gewürzt"),
                        value("herb", "Mit Kräutern"),
                        value("spicy", "Scharf"),
                        value("sweet", "Süß"),
                        value("savory", "Herzhaft"),
                        value("creamy", "Cremig"),
                        value("smoky", "Rauchig"),
                        value("fruit-based", "Fruchtbasiert"),
                        value("vegetable-based", "Gemüsebasiert"),
                        value("cheese-based", "Käsebasiert"),
                        value("meat-based", "Fleischbasiert"),
                        value("fish-based", "Fischbasiert"),
                        value("nut-based", "Nussbasiert"),
                        value("seed-based", "Saatenbasiert"),
                        value("grain-based", "Getreidebasiert"),
                        value("legume-based", "Hülsenfruchtbasiert"),
                        value("mixed-recipe", "Gemischte Rezeptur")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.FLAVOR_PROFILE,

                values =
                    values(
                        value("neutral", "Neutral"),
                        value("mild", "Mild"),

                        value("sweet", "Süß"),
                        value("salty", "Salzig"),
                        value("sour", "Sauer"),
                        value("bitter", "Bitter"),
                        value("umami", "Umami"),

                        value("savory", "Herzhaft"),
                        value("spicy", "Scharf"),
                        value("peppery", "Pfeffrig"),

                        value("herbal", "Kräuterig"),
                        value("floral", "Blumig"),
                        value("fruity", "Fruchtig"),
                        value("citrusy", "Zitrusartig"),

                        value("nutty", "Nussig"),
                        value("malty", "Malzig"),
                        value("roasted", "Geröstet"),
                        value("smoky", "Rauchig"),

                        value("creamy", "Cremig"),
                        value("buttery", "Buttrig"),

                        value("earthy", "Erdig"),
                        value("mineral", "Mineralisch")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.SWEETENING_TYPE,

                values =
                    values(
                        value("unsweetened", "Ungesüßt"),
                        value("sugar-sweetened", "Mit Zucker"),
                        value("no-added-sugar", "Ohne Zuckerzusatz"),
                        value("reduced-sugar", "Zuckerreduziert"),
                        value("sweetener-sweetened", "Mit Süßungsmittel"),
                        value("honey-sweetened", "Mit Honig")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.FAT_LEVEL,

                values =
                    values(
                        value("fat-free", "Fettfrei"),
                        value("very-low-fat", "Sehr fettarm"),
                        value("low-fat", "Fettarm"),
                        value("medium-fat", "Mittlere Fettstufe"),
                        value("full-fat", "Vollfett"),
                        value("high-fat", "Fettreich"),
                        value("double-cream", "Doppelrahmstufe"),
                        value("variable-fat", "Variable Fettstufe")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.PROTEIN_SOURCE,

                values =
                    values(
                        value("milk-protein", "Milchprotein"),
                        value("egg-protein", "Eiprotein"),
                        value("beef-protein", "Rindprotein"),
                        value("pork-protein", "Schweineprotein"),
                        value("poultry-protein", "Geflügelprotein"),
                        value("fish-protein", "Fischprotein"),
                        value("soy-protein", "Sojaprotein"),
                        value("pea-protein", "Erbsenprotein"),
                        value("wheat-protein", "Weizenprotein"),
                        value("lupin-protein", "Lupinenprotein"),
                        value("rice-protein", "Reisprotein"),
                        value("potato-protein", "Kartoffelprotein"),
                        value("chickpea-protein", "Kichererbsenprotein"),
                        value("mixed-plant-protein", "Gemischtes Pflanzenprotein"),
                        value("mixed-animal-protein", "Gemischtes Tierprotein"),
                        value("mixed-protein", "Gemischte Proteinquelle")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.GRAIN_TYPE,

                values =
                    values(
                        value("wheat", "Weizen"),
                        value("rye", "Roggen"),
                        value("spelt", "Dinkel"),
                        value("oats", "Hafer"),
                        value("barley", "Gerste"),
                        value("corn", "Mais"),
                        value("millet", "Hirse"),
                        value("rice", "Reis"),
                        value("buckwheat", "Buchweizen"),
                        value("quinoa", "Quinoa"),
                        value("amaranth", "Amaranth"),
                        value("einkorn", "Einkorn"),
                        value("emmer", "Emmer"),
                        value("durum-wheat", "Hartweizen"),
                        value("soft-wheat", "Weichweizen"),
                        value("teff", "Teff"),
                        value("sorghum", "Sorghum"),
                        value("mixed-grain", "Getreidemischung"),
                        value("wholegrain", "Vollkorn"),
                        value("refined-grain", "Ausgemahlenes Getreide")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.ANIMAL_SPECIES,

                values =
                    values(
                        value("cattle", "Rind"),
                        value("calf", "Kalb"),
                        value("pig", "Schwein"),
                        value("chicken", "Hähnchen"),
                        value("turkey", "Pute"),
                        value("duck", "Ente"),
                        value("goose", "Gans"),
                        value("sheep", "Schaf"),
                        value("lamb", "Lamm"),
                        value("goat", "Ziege"),
                        value("deer", "Hirsch"),
                        value("wild-boar", "Wildschwein"),
                        value("rabbit", "Kaninchen"),
                        value("horse", "Pferd"),
                        value("mixed-poultry", "Gemischtes Geflügel"),
                        value("mixed-meat", "Gemischte Fleischarten")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.PLANT_SPECIES,

                values =
                    values(
                        value(
                            key = "alfalfa",
                            displayName = "Alfalfa",
                            aliases = listOf(
                                "Luzerne"
                            )
                        ),
                        value("apple", "Apfel"),
                        value("apricot", "Aprikose"),
                        value("artichoke", "Artischocke"),
                        value("asparagus", "Spargel"),
                        value(
                            key = "bamboo-shoot",
                            displayName = "Bambussprosse",
                            aliases = listOf(
                                "Bambussprossen"
                            )
                        ),
                        value("banana", "Banane"),
                        value(
                            key = "bean-sprouts",
                            displayName = "Bohnensprossen",
                            aliases = listOf(
                                "Bohnensprosse"
                            )
                        ),
                        value(
                            key = "beetroot",
                            displayName = "Rote Bete",
                            aliases = listOf(
                                "Rande",
                                "Rote Beete",
                                "Rote Rübe"
                            )
                        ),
                        value("blackberry", "Brombeere"),
                        value("blueberry", "Blaubeere"),
                        value("broccoli", "Brokkoli"),
                        value(
                            key = "broccoli-sprouts",
                            displayName = "Brokkolisprossen",
                            aliases = listOf(
                                "Brokkolisprosse"
                            )
                        ),
                        value("brussels-sprouts", "Rosenkohl"),
                        value("carrot", "Karotte"),
                        value(
                            key = "cassava",
                            displayName = "Maniok",
                            aliases = listOf(
                                "Kassava",
                                "Yuca"
                            )
                        ),
                        value("cauliflower", "Blumenkohl"),
                        value(
                            key = "celeriac",
                            displayName = "Knollensellerie",
                            aliases = listOf(
                                "Sellerieknolle"
                            )
                        ),
                        value("celery", "Sellerie"),
                        value(
                            key = "chard",
                            displayName = "Mangold",
                            aliases = listOf(
                                "Blattmangold",
                                "Stielmangold"
                            )
                        ),
                        value("cherry", "Kirsche"),
                        value("cucumber", "Gurke"),
                        value("currant", "Johannisbeere"),
                        value(
                            key = "dulse",
                            displayName = "Lappentang",
                            aliases = listOf(
                                "Dulse"
                            )
                        ),
                        value("eggplant", "Aubergine"),
                        value("garlic", "Knoblauch"),
                        value("grape", "Traube"),
                        value("grapefruit", "Grapefruit"),
                        value(
                            key = "jerusalem-artichoke",
                            displayName = "Topinambur",
                            aliases = listOf(
                                "Erdbirne",
                                "Jerusalem-Artischocke"
                            )
                        ),
                        value("kale", "Grünkohl"),
                        value("kohlrabi", "Kohlrabi"),
                        value("kombu", "Kombu"),
                        value("leek", "Lauch"),
                        value("lemon", "Zitrone"),
                        value(
                            key = "lentil-sprouts",
                            displayName = "Linsensprossen",
                            aliases = listOf(
                                "Linsensprosse"
                            )
                        ),
                        value("lettuce", "Salat"),
                        value("lime", "Limette"),
                        value("mango", "Mango"),
                        value(
                            key = "mung-bean-sprouts",
                            displayName = "Mungbohnensprossen",
                            aliases = listOf(
                                "Mungbohnensprosse",
                                "Mungosprossen"
                            )
                        ),
                        value("mushroom", "Pilz"),
                        value(
                            key = "nori",
                            displayName = "Nori",
                            aliases = listOf(
                                "Nori-Alge"
                            )
                        ),
                        value("onion", "Zwiebel"),
                        value("orange", "Orange"),
                        value("papaya", "Papaya"),
                        value("parsnip", "Pastinake"),
                        value("peach", "Pfirsich"),
                        value("pear", "Birne"),
                        value("pepper", "Paprika"),
                        value("pineapple", "Ananas"),
                        value("plum", "Pflaume"),
                        value("potato", "Kartoffel"),
                        value("pumpkin", "Kürbis"),
                        value(
                            key = "radish",
                            displayName = "Radieschen",
                            aliases = listOf(
                                "Rettich"
                            )
                        ),
                        value("raspberry", "Himbeere"),
                        value("red-cabbage", "Rotkohl"),
                        value("rhubarb", "Rhabarber"),
                        value(
                            key = "rocket",
                            displayName = "Rucola",
                            aliases = listOf(
                                "Rauke"
                            )
                        ),
                        value(
                            key = "savoy-cabbage",
                            displayName = "Wirsing",
                            aliases = listOf(
                                "Wirsingkohl"
                            )
                        ),
                        value(
                            key = "sea-lettuce",
                            displayName = "Meersalat",
                            aliases = listOf(
                                "Grünalge",
                                "Ulva"
                            )
                        ),
                        value("shallot", "Schalotte"),
                        value("spinach", "Spinat"),
                        value(
                            key = "spring-onion",
                            displayName = "Frühlingszwiebel",
                            aliases = listOf(
                                "Lauchzwiebel"
                            )
                        ),
                        value("strawberry", "Erdbeere"),
                        value("sweet-potato", "Süßkartoffel"),
                        value("tomato", "Tomate"),
                        value(
                            key = "turnip",
                            displayName = "Speiserübe",
                            aliases = listOf(
                                "Herbstrübe",
                                "Mairübe",
                                "Weiße Rübe"
                            )
                        ),
                        value(
                            key = "wakame",
                            displayName = "Wakame",
                            aliases = listOf(
                                "Wakame-Alge"
                            )
                        ),
                        value("watermelon", "Wassermelone"),
                        value("white-cabbage", "Weißkohl"),
                        value(
                            key = "yam",
                            displayName = "Yamswurzel",
                            aliases = listOf(
                                "Yams"
                            )
                        ),
                        value("zucchini", "Zucchini")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.PROCESSING_METHOD,

                values =
                    values(
                        value("unprocessed", "Unverarbeitet"),
                        value("washed", "Gewaschen"),
                        value("peeled", "Geschält"),
                        value("cut", "Geschnitten"),
                        value("ground", "Gemahlen"),
                        value("pressed", "Gepresst"),
                        value("roasted", "Geröstet"),
                        value("baked", "Gebacken"),
                        value("boiled", "Gekocht"),
                        value("steamed", "Gedämpft"),
                        value("fried", "Gebraten"),
                        value("deep-fried", "Frittiert"),
                        value("fermented", "Fermentiert"),
                        value("smoked", "Geräuchert")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.PRESERVATION_METHOD,

                values =
                    values(
                        value("fresh", "Frisch"),
                        value("chilled", "Gekühlt"),
                        value("frozen", "Tiefgekühlt"),
                        value("dried", "Getrocknet"),
                        value("canned", "Konserviert"),
                        value("pickled", "Eingelegt"),
                        value("salted", "Gesalzen"),
                        value("smoked", "Geräuchert"),
                        value("vacuum-packed", "Vakuumverpackt"),
                        value("shelf-stable", "Ungekühlt haltbar")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.PREPARATION_STATE,

                values =
                    values(
                        value("raw", "Roh"),
                        value("partially-cooked", "Vorgegart"),
                        value("cooked", "Gegart"),
                        value("baked", "Gebacken"),
                        value("fried", "Gebraten"),
                        value("ready-to-eat", "Verzehrfertig"),
                        value("ready-to-cook", "Kochfertig"),
                        value("instant", "Instant"),
                        value("rehydrated", "Rehydriert"),
                        value("prepared", "Zubereitet")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.PHYSICAL_FORM,

                values =
                    values(
                        value("whole", "Ganz"),
                        value("pieces", "Stücke"),
                        value("sliced", "Geschnitten"),
                        value("diced", "Gewürfelt"),
                        value("grated", "Geraspelt"),
                        value("ground", "Gemahlen"),
                        value("powder", "Pulver"),
                        value("flakes", "Flocken"),
                        value("granules", "Granulat"),
                        value("paste", "Paste"),
                        value("liquid", "Flüssig"),
                        value("puree", "Püree")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.CUT_FORM,

                values =
                    values(
                        value("whole", "Ganz"),
                        value("fillet", "Filet"),
                        value("steak", "Steak"),
                        value("cutlet", "Schnitzel"),
                        value("chop", "Kotelett"),
                        value("strips", "Streifen"),
                        value("cubes", "Würfel"),
                        value("slices", "Scheiben"),
                        value("minced", "Gehackt"),
                        value("shredded", "Zerkleinert"),
                        value("rings", "Ringe"),
                        value("sticks", "Stifte")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.RIPENING_OR_AGING,

                values =
                    values(
                        value("unripened", "Ungereift"),
                        value("young", "Jung"),
                        value("matured", "Gereift"),
                        value("aged", "Lang gereift"),
                        value("extra-aged", "Extra lang gereift"),
                        value("surface-ripened", "Oberflächengereift"),
                        value("mold-ripened", "Schimmelgereift"),
                        value("brine-ripened", "Salzlakegereift"),
                        value("cave-aged", "Höhlengereift"),
                        value("variable-ripening", "Variable Reifung")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis.DIETARY_FORM,

                values =
                    values(
                        value("standard", "Standard"),
                        value("vegetarian", "Vegetarisch"),
                        value("vegan", "Vegan"),
                        value("gluten-free", "Glutenfrei"),
                        value("lactose-free", "Laktosefrei"),
                        value("low-carb", "Kohlenhydratreduziert"),
                        value("high-protein", "Proteinreich"),
                        value("reduced-salt", "Salzreduziert"),
                        value("reduced-sugar", "Zuckerreduziert"),
                        value("organic-recipe", "Bio-Rezeptur")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ALLERGEN_RELEVANT_VARIANT,

                values =
                    values(
                        value("standard-allergen-profile", "Standard-Allergenprofil"),
                        value("gluten-free", "Glutenfrei"),
                        value("lactose-free", "Laktosefrei"),
                        value("milk-free", "Milchfrei"),
                        value("egg-free", "Eifrei"),
                        value("soy-free", "Sojafrei"),
                        value("nut-free", "Nussfrei"),
                        value("peanut-free", "Erdnussfrei"),
                        value("celery-free", "Selleriefrei"),
                        value("mustard-free", "Senffrei")
                    )
            ),

            valueSet(
                axis =
                    CanonicalProductFamilyVariantAxis
                        .NUTRITIONALLY_RELEVANT_VARIANT,

                values =
                    values(
                        value("standard-nutrition", "Standard-Nährwertprofil"),
                        value("energy-reduced", "Energiereduziert"),
                        value("low-fat", "Fettreduziert"),
                        value("high-fat", "Fettreich"),
                        value("reduced-sugar", "Zuckerreduziert"),
                        value("sugar-free", "Zuckerfrei"),
                        value("reduced-salt", "Salzreduziert"),
                        value("high-protein", "Proteinreich"),
                        value("high-fiber", "Ballaststoffreich"),
                        value("wholegrain", "Vollkorn"),
                        value("fortified", "Angereichert"),
                        value("unsweetened", "Ungesüßt")
                    )
            )
        )
            .sortedBy {
                it.axis.name
            }

    init {
        require(
            AXIS_VALUE_SETS.map { it.axis }
                .toSet() ==
                    CanonicalProductFamilyVariantAxis.entries
                        .toSet()
        ) {
            "Concrete value policy must cover every variant axis."
        }

        require(
            AXIS_VALUE_SETS
                .flatMap { set ->
                    set.values.map {
                        set.axis to it.key
                    }
                }
                .distinct()
                .size ==
                    AXIS_VALUE_SETS.sumOf {
                        it.valueCount
                    }
        )
    }

    fun valuesFor(
        axis: CanonicalProductFamilyVariantAxis
    ): List<CanonicalVariantValue> =
        requireNotNull(
            AXIS_VALUE_SETS.firstOrNull {
                it.axis == axis
            }
        ) {
            "No concrete values exist for axis '$axis'."
        }.values

    private fun valueSet(
        axis: CanonicalProductFamilyVariantAxis,
        values: List<CanonicalVariantValue>
    ): CanonicalVariantAxisValueSet =
        CanonicalVariantAxisValueSet(
            axis = axis,
            values = values,
            valueCount = values.size,
            completeCanonicalVocabulary = true,
            rationale =
                "Canonical value vocabulary for axis '$axis'. Values " +
                        "represent food- and knowledge-relevant distinctions, " +
                        "not brands, retailers, EANs, prices or package sizes."
        )

    private fun values(
        vararg values: CanonicalVariantValue
    ): List<CanonicalVariantValue> =
        values
            .distinctBy {
                it.key
            }
            .sortedBy {
                it.key
            }

    private fun value(
        key: String,
        displayName: String,
        aliases: List<String> = emptyList(),
        identityRelevant: Boolean = true,
        knowledgeRelevant: Boolean = true
    ): CanonicalVariantValue =
        CanonicalVariantValue(
            key = key,
            displayName = displayName,
            aliases =
                aliases
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
            identityRelevant = identityRelevant,
            knowledgeRelevant = knowledgeRelevant
        )
}