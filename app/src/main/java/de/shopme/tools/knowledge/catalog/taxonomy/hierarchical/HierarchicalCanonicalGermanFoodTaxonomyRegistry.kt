package de.shopme.tools.knowledge.catalog.taxonomy.hierarchical

import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.model.HierarchicalFoodDepartment
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.model.HierarchicalFoodDomain
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.model.HierarchicalFoodTaxonomyNode
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.model.HierarchicalPrimaryFoodTaxonomy

object HierarchicalCanonicalGermanFoodTaxonomyRegistry {

    val taxonomy =
        HierarchicalPrimaryFoodTaxonomy(
            version =
                1,

            domain =
                HierarchicalFoodDomain(
                    id =
                        "food",
                    name =
                        "Food"
                ),

            departments =
                listOf(

                    /*
                     * ─────────────────────────────────────
                     * OBST
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "fruit",
                        name =
                            "Obst",

                        children =
                            listOf(
                                leaf(
                                    "pome-fruit",
                                    "Kernobst"
                                ),
                                leaf(
                                    "stone-fruit",
                                    "Steinobst"
                                ),
                                leaf(
                                    "berries",
                                    "Beerenobst"
                                ),
                                leaf(
                                    "citrus-fruit",
                                    "Zitrusfrüchte"
                                ),
                                leaf(
                                    "tropical-fruit",
                                    "Tropenfrüchte"
                                ),
                                leaf(
                                    "melons",
                                    "Melonen"
                                ),
                                leaf(
                                    "grapes",
                                    "Trauben"
                                ),
                                leaf(
                                    "other-fruit",
                                    "Sonstiges Obst"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * GEMÜSE
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "vegetables",
                        name =
                            "Gemüse",

                        children =
                            listOf(

                                leaf(
                                    "leafy-vegetables",
                                    "Blattgemüse"
                                ),

                                leaf(
                                    "cabbage-vegetables",
                                    "Kohlgemüse"
                                ),

                                leaf(
                                    "root-vegetables",
                                    "Wurzelgemüse"
                                ),

                                leaf(
                                    "tuber-vegetables",
                                    "Knollengemüse"
                                ),

                                leaf(
                                    "pod-vegetables",
                                    "Hülsengemüse"
                                ),

                                leaf(
                                    "allium-vegetables",
                                    "Zwiebelgemüse"
                                ),

                                /*
                                 * Verbraucherverständliche
                                 * Sortimentsgruppe.
                                 */
                                node(
                                    id =
                                        "mediterranean-vegetables",
                                    name =
                                        "Mediterranes Gemüse",

                                    children =
                                        listOf(
                                            leaf(
                                                "tomatoes",
                                                "Tomaten"
                                            ),
                                            leaf(
                                                "peppers",
                                                "Paprika"
                                            ),
                                            leaf(
                                                "zucchini",
                                                "Zucchini"
                                            ),
                                            leaf(
                                                "aubergines",
                                                "Auberginen"
                                            ),
                                            leaf(
                                                "fennel",
                                                "Fenchel"
                                            ),
                                            leaf(
                                                "artichokes",
                                                "Artischocken"
                                            ),
                                            leaf(
                                                "avocados",
                                                "Avocados"
                                            )
                                        )
                                ),

                                leaf(
                                    "salads",
                                    "Salate"
                                ),

                                leaf(
                                    "mushrooms",
                                    "Pilze"
                                ),

                                node(
                                    id =
                                        "herbs",
                                    name =
                                        "Kräuter",

                                    children =
                                        listOf(
                                            leaf(
                                                "fresh-herbs",
                                                "Frisch"
                                            )
                                        )
                                ),

                                leaf(
                                    "other-vegetables",
                                    "Sonstiges Gemüse"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * BROT & BACKWAREN
                     *
                     * Frische-/Backstationssortiment.
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "bakery",
                        name =
                            "Brot & Backwaren",

                        children =
                            listOf(

                                leaf(
                                    "bread",
                                    "Brot"
                                ),

                                leaf(
                                    "bread-rolls",
                                    "Brötchen"
                                ),

                                leaf(
                                    "toast-and-sandwich-bread",
                                    "Toast- & Sandwichbrot"
                                ),

                                leaf(
                                    "flatbread",
                                    "Fladenbrot"
                                ),

                                leaf(
                                    "crispbread-and-rusks",
                                    "Knäckebrot & Zwieback"
                                ),

                                leaf(
                                    "small-baked-goods",
                                    "Kleingebäck"
                                ),

                                leaf(
                                    "pastries",
                                    "Feingebäck"
                                ),

                                leaf(
                                    "cakes-and-tarts",
                                    "Kuchen & Torten"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * KEKSE & SÜSSES GEBÄCK
                     *
                     * Haltbare verpackte Regalware.
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "biscuits-and-sweet-baked-goods",
                        name =
                            "Kekse & süßes Gebäck",

                        children =
                            listOf(
                                leaf(
                                    "biscuits",
                                    "Kekse"
                                ),
                                leaf(
                                    "wafers",
                                    "Waffeln"
                                ),
                                leaf(
                                    "gingerbread",
                                    "Lebkuchen"
                                ),
                                leaf(
                                    "sweet-pastries",
                                    "Süßgebäck"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * MOLKEREIPRODUKTE & EIER
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "dairy-and-eggs",
                        name =
                            "Molkereiprodukte & Eier",

                        children =
                            listOf(

                                node(
                                    id =
                                        "dairy-products",
                                    name =
                                        "Milchprodukte",

                                    children =
                                        listOf(

                                            node(
                                                id =
                                                    "milk",
                                                name =
                                                    "Milch",

                                                children =
                                                    listOf(
                                                        leaf(
                                                            "whole-milk",
                                                            "Vollmilch"
                                                        ),
                                                        leaf(
                                                            "low-fat-milk",
                                                            "Fettarme Milch"
                                                        ),
                                                        leaf(
                                                            "fresh-milk",
                                                            "Frischmilch"
                                                        ),
                                                        leaf(
                                                            "uht-milk",
                                                            "H-Milch"
                                                        )
                                                    )
                                            ),

                                            leaf(
                                                "cultured-milk-products",
                                                "Sauermilchprodukte"
                                            ),

                                            leaf(
                                                "yogurt",
                                                "Joghurt"
                                            ),

                                            node(
                                                id =
                                                    "quark",
                                                name =
                                                    "Quark",

                                                children =
                                                    listOf(
                                                        leaf(
                                                            "low-fat-quark",
                                                            "Magerquark"
                                                        )
                                                    )
                                            ),

                                            leaf(
                                                "skyr",
                                                "Skyr"
                                            ),

                                            leaf(
                                                "cream-products",
                                                "Sahne & Sauerrahmprodukte"
                                            ),

                                            leaf(
                                                "butter",
                                                "Butter"
                                            ),

                                            node(
                                                id =
                                                    "cheese",
                                                name =
                                                    "Käse",

                                                children =
                                                    listOf(
                                                        leaf(
                                                            "fresh-cheese",
                                                            "Frischkäse"
                                                        ),
                                                        leaf(
                                                            "soft-cheese",
                                                            "Weichkäse"
                                                        ),
                                                        leaf(
                                                            "semi-hard-cheese",
                                                            "Schnittkäse"
                                                        ),
                                                        leaf(
                                                            "hard-cheese",
                                                            "Hartkäse"
                                                        ),
                                                        leaf(
                                                            "brined-cheese",
                                                            "Salzlakenkäse"
                                                        )
                                                    )
                                            ),

                                            leaf(
                                                "dairy-desserts",
                                                "Molkereidesserts"
                                            )
                                        )
                                ),

                                leaf(
                                    "eggs",
                                    "Eier"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * FLEISCH & GEFLÜGEL
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "meat",
                        name =
                            "Fleisch & Geflügel",

                        children =
                            listOf(

                                leaf(
                                    "beef",
                                    "Rindfleisch"
                                ),

                                leaf(
                                    "pork",
                                    "Schweinefleisch"
                                ),

                                leaf(
                                    "veal",
                                    "Kalbfleisch"
                                ),

                                leaf(
                                    "lamb-and-mutton",
                                    "Lamm- & Schaffleisch"
                                ),

                                leaf(
                                    "chicken",
                                    "Hähnchenfleisch"
                                ),

                                leaf(
                                    "turkey",
                                    "Putenfleisch"
                                ),

                                leaf(
                                    "duck-and-goose",
                                    "Enten- & Gänsefleisch"
                                ),

                                leaf(
                                    "game",
                                    "Wildfleisch"
                                ),

                                node(
                                    id =
                                        "minced-meat",
                                    name =
                                        "Hackfleisch",

                                    children =
                                        listOf(
                                            leaf(
                                                "beef-minced-meat",
                                                "Rinderhackfleisch"
                                            ),
                                            leaf(
                                                "pork-minced-meat",
                                                "Schweinehackfleisch"
                                            ),
                                            leaf(
                                                "mixed-minced-meat",
                                                "Hackfleisch gemischt"
                                            ),
                                            leaf(
                                                "poultry-minced-meat",
                                                "Geflügelhackfleisch"
                                            )
                                        )
                                ),

                                leaf(
                                    "other-meat",
                                    "Sonstiges Fleisch"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * WURST & SCHINKEN
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "sausage-and-ham",
                        name =
                            "Wurst & Schinken",

                        children =
                            listOf(
                                leaf(
                                    "raw-sausage",
                                    "Rohwurst"
                                ),
                                leaf(
                                    "boiled-sausage",
                                    "Brühwurst"
                                ),
                                leaf(
                                    "cooked-sausage",
                                    "Kochwurst"
                                ),
                                leaf(
                                    "fried-sausage",
                                    "Bratwurst"
                                ),
                                leaf(
                                    "ham",
                                    "Schinken"
                                ),
                                leaf(
                                    "cold-cuts",
                                    "Aufschnitt"
                                ),
                                leaf(
                                    "other-sausage",
                                    "Sonstige Wurstwaren"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * FISCH & MEERESFRÜCHTE
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "fish-and-seafood",
                        name =
                            "Fisch & Meeresfrüchte",

                        children =
                            listOf(

                                leaf(
                                    "freshwater-fish",
                                    "Süßwasserfische"
                                ),

                                leaf(
                                    "saltwater-fish",
                                    "Meeresfische"
                                ),

                                leaf(
                                    "fish-fillets",
                                    "Fischfilets"
                                ),

                                leaf(
                                    "crustaceans",
                                    "Krustentiere"
                                ),

                                leaf(
                                    "molluscs",
                                    "Weichtiere"
                                ),

                                leaf(
                                    "shellfish",
                                    "Muscheln"
                                ),

                                leaf(
                                    "other-seafood",
                                    "Sonstige Meeresfrüchte"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * GETRÄNKE
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "beverages",
                        name =
                            "Getränke",

                        children =
                            listOf(

                                leaf(
                                    "water",
                                    "Wasser"
                                ),

                                leaf(
                                    "fruit-juices",
                                    "Fruchtsäfte"
                                ),

                                leaf(
                                    "vegetable-juices",
                                    "Gemüsesäfte"
                                ),

                                leaf(
                                    "nectars",
                                    "Nektare"
                                ),

                                leaf(
                                    "spritzers",
                                    "Schorlen"
                                ),

                                leaf(
                                    "soft-drinks",
                                    "Erfrischungsgetränke"
                                ),

                                leaf(
                                    "energy-and-sports-drinks",
                                    "Energy- & Sportgetränke"
                                ),

                                leaf(
                                    "coffee",
                                    "Kaffee"
                                ),

                                node(
                                    id =
                                        "tea",
                                    name =
                                        "Tee",

                                    children =
                                        listOf(
                                            leaf(
                                                "green-tea",
                                                "Grüner Tee"
                                            ),
                                            leaf(
                                                "black-tea",
                                                "Schwarzer Tee"
                                            ),
                                            leaf(
                                                "herbal-tea",
                                                "Kräutertee"
                                            ),
                                            leaf(
                                                "fruit-tea",
                                                "Früchtetee"
                                            )
                                        )
                                ),

                                leaf(
                                    "cocoa-drinks",
                                    "Kakaogetränke"
                                ),

                                leaf(
                                    "beer",
                                    "Bier"
                                ),

                                leaf(
                                    "wine",
                                    "Wein"
                                ),

                                leaf(
                                    "sparkling-wine",
                                    "Sekt & Schaumwein"
                                ),

                                leaf(
                                    "spirits",
                                    "Spirituosen"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * GETREIDE, REIS & HÜLSENFRÜCHTE
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "grains-rice-and-legumes",
                        name =
                            "Getreide, Reis & Hülsenfrüchte",

                        children =
                            listOf(

                                node(
                                    id =
                                        "grains",
                                    name =
                                        "Getreide",

                                    children =
                                        listOf(
                                            leaf(
                                                "wheat",
                                                "Weizen"
                                            ),
                                            leaf(
                                                "rye",
                                                "Roggen"
                                            ),
                                            leaf(
                                                "spelt",
                                                "Dinkel"
                                            ),
                                            leaf(
                                                "green-spelt",
                                                "Grünkern"
                                            ),
                                            leaf(
                                                "oats",
                                                "Hafer"
                                            ),
                                            leaf(
                                                "barley",
                                                "Gerste"
                                            ),
                                            leaf(
                                                "millet",
                                                "Hirse"
                                            ),
                                            leaf(
                                                "corn",
                                                "Mais"
                                            )
                                        )
                                ),

                                leaf(
                                    "pseudocereals",
                                    "Pseudogetreide"
                                ),

                                leaf(
                                    "rice",
                                    "Reis"
                                ),

                                node(
                                    id =
                                        "legumes",
                                    name =
                                        "Hülsenfrüchte",

                                    children =
                                        listOf(

                                            node(
                                                id =
                                                    "lentils",
                                                name =
                                                    "Linsen",

                                                children =
                                                    listOf(
                                                        leaf(
                                                            "red-lentils",
                                                            "Rote Linsen"
                                                        ),
                                                        leaf(
                                                            "yellow-lentils",
                                                            "Gelbe Linsen"
                                                        ),
                                                        leaf(
                                                            "beluga-lentils",
                                                            "Belugalinsen"
                                                        )
                                                    )
                                            ),

                                            node(
                                                id =
                                                    "beans",
                                                name =
                                                    "Bohnen",

                                                children =
                                                    listOf(
                                                        leaf(
                                                            "kidney-beans",
                                                            "Kidneybohnen"
                                                        ),
                                                        leaf(
                                                            "white-beans",
                                                            "Weiße Bohnen"
                                                        )
                                                    )
                                            ),

                                            leaf(
                                                "peas",
                                                "Erbsen"
                                            ),

                                            leaf(
                                                "chickpeas",
                                                "Kichererbsen"
                                            )
                                        )
                                ),

                                leaf(
                                    "couscous",
                                    "Couscous"
                                ),

                                leaf(
                                    "bulgur",
                                    "Bulgur"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * NUDELN & PASTA
                     *
                     * bewusst kein "Trockennudeln"-Root.
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "pasta",
                        name =
                            "Nudeln & Pasta",

                        children =
                            listOf(
                                leaf(
                                    "pasta",
                                    "Pasta"
                                ),
                                leaf(
                                    "egg-pasta",
                                    "Eiernudeln"
                                ),
                                leaf(
                                    "filled-pasta",
                                    "Gefüllte Pasta"
                                ),
                                leaf(
                                    "asian-noodles",
                                    "Asiatische Nudeln"
                                ),
                                leaf(
                                    "dumpling-pasta",
                                    "Gnocchi & ähnliche Teigwaren"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * MEHL & BACKZUTATEN
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "flour-and-baking",
                        name =
                            "Mehl & Backzutaten",

                        children =
                            listOf(
                                leaf(
                                    "flour",
                                    "Mehl"
                                ),
                                leaf(
                                    "starch",
                                    "Stärke"
                                ),
                                leaf(
                                    "sugar",
                                    "Zucker"
                                ),
                                leaf(
                                    "sweeteners",
                                    "Süßungsmittel"
                                ),
                                leaf(
                                    "raising-agents",
                                    "Backtriebmittel"
                                ),
                                leaf(
                                    "yeast",
                                    "Hefe"
                                ),
                                leaf(
                                    "gelling-agents",
                                    "Geliermittel"
                                ),
                                leaf(
                                    "baking-flavours",
                                    "Backaromen"
                                ),
                                leaf(
                                    "baking-decoration",
                                    "Backdekoration"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * ÖLE & FETTE
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "oils-and-fats",
                        name =
                            "Öle & Fette",

                        children =
                            listOf(
                                leaf(
                                    "olive-oil",
                                    "Olivenöl"
                                ),
                                leaf(
                                    "vegetable-oils",
                                    "Pflanzenöle"
                                ),
                                leaf(
                                    "nut-and-seed-oils",
                                    "Nuss- & Samenöle"
                                ),
                                leaf(
                                    "animal-fats",
                                    "Tierische Fette"
                                ),
                                leaf(
                                    "margarine",
                                    "Margarine"
                                ),
                                leaf(
                                    "cooking-fats",
                                    "Brat- & Kochfette"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * GEWÜRZE & WÜRZMITTEL
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "spices-and-seasonings",
                        name =
                            "Gewürze & Würzmittel",

                        children =
                            listOf(
                                leaf(
                                    "salt",
                                    "Salz"
                                ),
                                leaf(
                                    "pepper",
                                    "Pfeffer"
                                ),
                                leaf(
                                    "single-spices",
                                    "Einzelgewürze"
                                ),
                                leaf(
                                    "spice-blends",
                                    "Gewürzmischungen"
                                ),

                                node(
                                    id =
                                        "herbs",
                                    name =
                                        "Kräuter",

                                    children =
                                        listOf(
                                            leaf(
                                                "dried-herbs",
                                                "Getrocknet"
                                            )
                                        )
                                ),

                                leaf(
                                    "seasoning-pastes",
                                    "Würzpasten"
                                ),
                                leaf(
                                    "broths-and-stocks",
                                    "Brühen & Fonds"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * SAUCEN
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "sauces-and-dressings",
                        name =
                            "Saucen, Dressings & Essig",

                        children =
                            listOf(
                                leaf(
                                    "ketchup",
                                    "Ketchup"
                                ),
                                leaf(
                                    "mayonnaise",
                                    "Mayonnaise"
                                ),
                                leaf(
                                    "mustard",
                                    "Senf"
                                ),
                                leaf(
                                    "barbecue-sauces",
                                    "Grillsaucen"
                                ),
                                leaf(
                                    "cooking-sauces",
                                    "Kochsaucen"
                                ),
                                leaf(
                                    "asian-sauces",
                                    "Asiatische Saucen"
                                ),
                                leaf(
                                    "dressings",
                                    "Salatdressings"
                                ),
                                leaf(
                                    "dips-and-seasoning-pastes",
                                    "Dips & Würzpasten"
                                ),
                                leaf(
                                    "vinegar",
                                    "Essig"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * FRÜHSTÜCK
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "breakfast-and-spreads",
                        name =
                            "Frühstück & Brotaufstriche",

                        children =
                            listOf(
                                leaf(
                                    "breakfast-cereals",
                                    "Frühstückscerealien"
                                ),
                                leaf(
                                    "muesli",
                                    "Müsli"
                                ),
                                leaf(
                                    "porridge-and-flakes",
                                    "Flocken & Porridge"
                                ),
                                leaf(
                                    "fruit-spreads",
                                    "Konfitüren & Fruchtaufstriche"
                                ),
                                leaf(
                                    "honey",
                                    "Honig"
                                ),
                                leaf(
                                    "nut-spreads",
                                    "Nussaufstriche"
                                ),
                                leaf(
                                    "savoury-spreads",
                                    "Herzhafte Brotaufstriche"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * NÜSSE / SAMEN / TROCKENFRÜCHTE
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "nuts-seeds-and-dried-fruit",
                        name =
                            "Nüsse, Samen & Trockenfrüchte",

                        children =
                            listOf(
                                leaf(
                                    "nuts",
                                    "Nüsse"
                                ),
                                leaf(
                                    "seeds",
                                    "Samen & Kerne"
                                ),
                                leaf(
                                    "dried-fruit",
                                    "Trockenfrüchte"
                                ),
                                leaf(
                                    "nut-and-fruit-mixes",
                                    "Nuss- & Fruchtmischungen"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * SNACKS
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "snacks",
                        name =
                            "Snacks & Knabberartikel",

                        children =
                            listOf(
                                leaf(
                                    "potato-chips",
                                    "Kartoffelchips"
                                ),
                                leaf(
                                    "vegetable-chips",
                                    "Gemüsechips"
                                ),
                                leaf(
                                    "flips",
                                    "Flips"
                                ),
                                leaf(
                                    "salted-snacks",
                                    "Salzgebäck"
                                ),
                                leaf(
                                    "crackers",
                                    "Cracker"
                                ),
                                leaf(
                                    "popcorn",
                                    "Popcorn"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * SÜSSWAREN
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "confectionery",
                        name =
                            "Süßwaren",

                        children =
                            listOf(

                                node(
                                    id =
                                        "chocolate",
                                    name =
                                        "Schokolade",

                                    children =
                                        listOf(
                                            leaf(
                                                "milk-chocolate",
                                                "Milchschokolade"
                                            ),
                                            leaf(
                                                "white-chocolate",
                                                "Weiße Schokolade"
                                            ),
                                            leaf(
                                                "dark-chocolate",
                                                "Zartbitterschokolade"
                                            )
                                        )
                                ),

                                leaf(
                                    "pralines",
                                    "Pralinen"
                                ),
                                leaf(
                                    "candy",
                                    "Bonbons"
                                ),
                                leaf(
                                    "fruit-gums",
                                    "Fruchtgummi"
                                ),
                                leaf(
                                    "liquorice",
                                    "Lakritz"
                                ),
                                leaf(
                                    "marzipan-and-nougat",
                                    "Marzipan & Nougat"
                                ),
                                leaf(
                                    "bars",
                                    "Riegel"
                                ),
                                leaf(
                                    "chewing-gum",
                                    "Kaugummi"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * EIS & DESSERTS
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "ice-cream-and-desserts",
                        name =
                            "Eis & Desserts",

                        children =
                            listOf(
                                leaf(
                                    "ice-cream",
                                    "Speiseeis"
                                ),
                                leaf(
                                    "sorbets",
                                    "Sorbet"
                                ),
                                leaf(
                                    "puddings",
                                    "Pudding"
                                ),
                                leaf(
                                    "dessert-creams",
                                    "Dessertcremes"
                                ),
                                leaf(
                                    "other-desserts",
                                    "Sonstige Desserts"
                                )
                            )
                    ),

                    /*
                     * ─────────────────────────────────────
                     * BABY- & KLEINKINDNAHRUNG
                     *
                     * echte eigene Fachabteilung.
                     * ─────────────────────────────────────
                     */

                    department(
                        id =
                            "baby-and-toddler-food",
                        name =
                            "Baby- & Kleinkindnahrung",

                        children =
                            listOf(
                                leaf(
                                    "infant-formula",
                                    "Säuglingsnahrung"
                                ),
                                leaf(
                                    "baby-porridge",
                                    "Babybreie"
                                ),
                                leaf(
                                    "baby-meals",
                                    "Babymenüs"
                                ),
                                leaf(
                                    "baby-snacks",
                                    "Babysnacks"
                                )
                            )
                    )
                )
                    .sortedBy {
                        it.id
                    }
        )

    private fun department(
        id: String,
        name: String,
        children: List<HierarchicalFoodTaxonomyNode>
    ): HierarchicalFoodDepartment =
        HierarchicalFoodDepartment(
            id =
                id,
            name =
                name,
            children =
                children.sortedBy {
                    it.id
                }
        )

    private fun node(
        id: String,
        name: String,
        children: List<HierarchicalFoodTaxonomyNode>
    ): HierarchicalFoodTaxonomyNode =
        HierarchicalFoodTaxonomyNode(
            id =
                id,
            name =
                name,
            children =
                children.sortedBy {
                    it.id
                }
        )

    private fun leaf(
        id: String,
        name: String
    ): HierarchicalFoodTaxonomyNode =
        HierarchicalFoodTaxonomyNode(
            id =
                id,
            name =
                name,
            children =
                emptyList()
        )
}