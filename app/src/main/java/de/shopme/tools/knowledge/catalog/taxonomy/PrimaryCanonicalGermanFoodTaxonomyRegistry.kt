package de.shopme.tools.knowledge.catalog.taxonomy

import de.shopme.tools.knowledge.catalog.taxonomy.model.PrimaryFoodDepartment
import de.shopme.tools.knowledge.catalog.taxonomy.model.PrimaryFoodGroup
import de.shopme.tools.knowledge.catalog.taxonomy.model.PrimaryFoodTaxonomy
import de.shopme.tools.knowledge.catalog.taxonomy.model.PrimaryFoodTaxonomyDomain

object PrimaryCanonicalGermanFoodTaxonomyRegistry {

    val taxonomy =
        PrimaryFoodTaxonomy(
            version =
                1,

            domain =
                PrimaryFoodTaxonomyDomain(
                    id =
                        "food",
                    name =
                        "Food"
                ),

            departments =
                listOf(

                    department(
                        id =
                            "fruit",
                        name =
                            "Obst",

                        groups =
                            listOf(
                                group(
                                    "pome-fruit",
                                    "Kernobst"
                                ),
                                group(
                                    "stone-fruit",
                                    "Steinobst"
                                ),
                                group(
                                    "berries",
                                    "Beerenobst"
                                ),
                                group(
                                    "citrus-fruit",
                                    "Zitrusfrüchte"
                                ),
                                group(
                                    "tropical-fruit",
                                    "Tropenfrüchte"
                                ),
                                group(
                                    "melons",
                                    "Melonen"
                                ),
                                group(
                                    "grapes",
                                    "Trauben"
                                ),
                                group(
                                    "other-fruit",
                                    "Sonstiges Obst"
                                )
                            )
                    ),

                    department(
                        id =
                            "vegetables",
                        name =
                            "Gemüse",

                        groups =
                            listOf(
                                group(
                                    "leafy-vegetables",
                                    "Blattgemüse"
                                ),
                                group(
                                    "cabbage-vegetables",
                                    "Kohlgemüse"
                                ),
                                group(
                                    "root-vegetables",
                                    "Wurzelgemüse"
                                ),
                                group(
                                    "tuber-vegetables",
                                    "Knollengemüse"
                                ),
                                group(
                                    "fruiting-vegetables",
                                    "Fruchtgemüse"
                                ),
                                group(
                                    "pod-vegetables",
                                    "Hülsengemüse"
                                ),
                                group(
                                    "allium-vegetables",
                                    "Zwiebelgemüse"
                                ),
                                group(
                                    "stem-vegetables",
                                    "Stängelgemüse"
                                ),
                                group(
                                    "salads",
                                    "Salate"
                                ),
                                group(
                                    "mushrooms",
                                    "Pilze"
                                ),
                                group(
                                    "culinary-herbs",
                                    "Frische Kräuter"
                                ),
                                group(
                                    "other-vegetables",
                                    "Sonstiges Gemüse"
                                )
                            )
                    ),

                    department(
                        id =
                            "bakery",
                        name =
                            "Brot & Backwaren",

                        groups =
                            listOf(
                                group(
                                    "bread",
                                    "Brot"
                                ),
                                group(
                                    "bread-rolls",
                                    "Brötchen"
                                ),
                                group(
                                    "toast-and-sandwich-bread",
                                    "Toast- & Sandwichbrot"
                                ),
                                group(
                                    "flatbread",
                                    "Fladenbrot"
                                ),
                                group(
                                    "crispbread-and-rusks",
                                    "Knäckebrot & Zwieback"
                                ),
                                group(
                                    "small-baked-goods",
                                    "Kleingebäck"
                                ),
                                group(
                                    "pastries",
                                    "Feingebäck"
                                ),
                                group(
                                    "cakes-and-tarts",
                                    "Kuchen & Torten"
                                )
                            )
                    ),

                    department(
                        id =
                            "dairy-and-eggs",
                        name =
                            "Molkereiprodukte & Eier",

                        groups =
                            listOf(
                                group(
                                    "milk",
                                    "Milch"
                                ),
                                group(
                                    "cultured-milk-products",
                                    "Sauermilchprodukte"
                                ),
                                group(
                                    "yogurt",
                                    "Joghurt"
                                ),
                                group(
                                    "quark-and-skyr",
                                    "Quark & Skyr"
                                ),
                                group(
                                    "cream-products",
                                    "Sahne & Sauerrahmprodukte"
                                ),
                                group(
                                    "butter",
                                    "Butter"
                                ),
                                group(
                                    "fresh-cheese",
                                    "Frischkäse"
                                ),
                                group(
                                    "soft-cheese",
                                    "Weichkäse"
                                ),
                                group(
                                    "semi-hard-cheese",
                                    "Schnittkäse"
                                ),
                                group(
                                    "hard-cheese",
                                    "Hartkäse"
                                ),
                                group(
                                    "brined-cheese",
                                    "Salzlakenkäse"
                                ),
                                group(
                                    "dairy-desserts",
                                    "Molkereidesserts"
                                ),
                                group(
                                    "eggs",
                                    "Eier"
                                )
                            )
                    ),

                    department(
                        id =
                            "meat",
                        name =
                            "Fleisch & Geflügel",

                        groups =
                            listOf(
                                group(
                                    "beef",
                                    "Rindfleisch"
                                ),
                                group(
                                    "pork",
                                    "Schweinefleisch"
                                ),
                                group(
                                    "veal",
                                    "Kalbfleisch"
                                ),
                                group(
                                    "lamb-and-mutton",
                                    "Lamm- & Schaffleisch"
                                ),
                                group(
                                    "chicken",
                                    "Hähnchenfleisch"
                                ),
                                group(
                                    "turkey",
                                    "Putenfleisch"
                                ),
                                group(
                                    "duck-and-goose",
                                    "Enten- & Gänsefleisch"
                                ),
                                group(
                                    "game",
                                    "Wildfleisch"
                                ),
                                group(
                                    "minced-meat",
                                    "Hackfleisch"
                                ),
                                group(
                                    "other-meat",
                                    "Sonstiges Fleisch"
                                )
                            )
                    ),

                    department(
                        id =
                            "sausage-and-ham",
                        name =
                            "Wurst & Schinken",

                        groups =
                            listOf(
                                group(
                                    "raw-sausage",
                                    "Rohwurst"
                                ),
                                group(
                                    "boiled-sausage",
                                    "Brühwurst"
                                ),
                                group(
                                    "cooked-sausage",
                                    "Kochwurst"
                                ),
                                group(
                                    "fried-sausage",
                                    "Bratwurst"
                                ),
                                group(
                                    "ham",
                                    "Schinken"
                                ),
                                group(
                                    "cold-cuts",
                                    "Aufschnitt"
                                ),
                                group(
                                    "other-sausage",
                                    "Sonstige Wurstwaren"
                                )
                            )
                    ),

                    department(
                        id =
                            "fish-and-seafood",
                        name =
                            "Fisch & Meeresfrüchte",

                        groups =
                            listOf(
                                group(
                                    "freshwater-fish",
                                    "Süßwasserfische"
                                ),
                                group(
                                    "saltwater-fish",
                                    "Meeresfische"
                                ),
                                group(
                                    "fish-fillets",
                                    "Fischfilets"
                                ),
                                group(
                                    "crustaceans",
                                    "Krustentiere"
                                ),
                                group(
                                    "molluscs",
                                    "Weichtiere"
                                ),
                                group(
                                    "shellfish",
                                    "Muscheln"
                                ),
                                group(
                                    "other-seafood",
                                    "Sonstige Meeresfrüchte"
                                )
                            )
                    ),

                    department(
                        id =
                            "plant-based-foods",
                        name =
                            "Pflanzliche Lebensmittelalternativen",

                        groups =
                            listOf(
                                group(
                                    "plant-drinks",
                                    "Pflanzendrinks"
                                ),
                                group(
                                    "plant-yogurt-alternatives",
                                    "Pflanzliche Joghurtalternativen"
                                ),
                                group(
                                    "plant-cheese-alternatives",
                                    "Pflanzliche Käsealternativen"
                                ),
                                group(
                                    "plant-meat-alternatives",
                                    "Pflanzliche Fleischalternativen"
                                ),
                                group(
                                    "tofu",
                                    "Tofu"
                                ),
                                group(
                                    "tempeh",
                                    "Tempeh"
                                ),
                                group(
                                    "plant-spreads",
                                    "Pflanzliche Brotaufstriche"
                                )
                            )
                    ),

                    department(
                        id =
                            "beverages",
                        name =
                            "Getränke",

                        groups =
                            listOf(
                                group(
                                    "water",
                                    "Wasser"
                                ),
                                group(
                                    "fruit-juices",
                                    "Fruchtsäfte"
                                ),
                                group(
                                    "vegetable-juices",
                                    "Gemüsesäfte"
                                ),
                                group(
                                    "nectars",
                                    "Nektare"
                                ),
                                group(
                                    "spritzers",
                                    "Schorlen"
                                ),
                                group(
                                    "soft-drinks",
                                    "Erfrischungsgetränke"
                                ),
                                group(
                                    "energy-and-sports-drinks",
                                    "Energy- & Sportgetränke"
                                ),
                                group(
                                    "coffee",
                                    "Kaffee"
                                ),
                                group(
                                    "tea",
                                    "Tee"
                                ),
                                group(
                                    "cocoa-drinks",
                                    "Kakaogetränke"
                                ),
                                group(
                                    "beer",
                                    "Bier"
                                ),
                                group(
                                    "wine",
                                    "Wein"
                                ),
                                group(
                                    "sparkling-wine",
                                    "Sekt & Schaumwein"
                                ),
                                group(
                                    "spirits",
                                    "Spirituosen"
                                )
                            )
                    ),

                    department(
                        id =
                            "grains-rice-and-legumes",
                        name =
                            "Getreide, Reis & Hülsenfrüchte",

                        groups =
                            listOf(
                                group(
                                    "rice",
                                    "Reis"
                                ),
                                group(
                                    "wheat",
                                    "Weizen"
                                ),
                                group(
                                    "rye",
                                    "Roggen"
                                ),
                                group(
                                    "spelt",
                                    "Dinkel"
                                ),
                                group(
                                    "oats",
                                    "Hafer"
                                ),
                                group(
                                    "barley",
                                    "Gerste"
                                ),
                                group(
                                    "millet",
                                    "Hirse"
                                ),
                                group(
                                    "corn",
                                    "Mais"
                                ),
                                group(
                                    "pseudocereals",
                                    "Pseudogetreide"
                                ),
                                group(
                                    "lentils",
                                    "Linsen"
                                ),
                                group(
                                    "beans",
                                    "Bohnen"
                                ),
                                group(
                                    "peas",
                                    "Erbsen"
                                ),
                                group(
                                    "chickpeas",
                                    "Kichererbsen"
                                ),
                                group(
                                    "couscous",
                                    "Couscous"
                                ),
                                group(
                                    "bulgur",
                                    "Bulgur"
                                )
                            )
                    ),

                    department(
                        id =
                            "pasta",
                        name =
                            "Nudeln & Pasta",

                        groups =
                            listOf(
                                group(
                                    "dried-pasta",
                                    "Trockennudeln"
                                ),
                                group(
                                    "egg-pasta",
                                    "Eiernudeln"
                                ),
                                group(
                                    "fresh-pasta",
                                    "Frische Pasta"
                                ),
                                group(
                                    "filled-pasta",
                                    "Gefüllte Pasta"
                                ),
                                group(
                                    "asian-noodles",
                                    "Asiatische Nudeln"
                                ),
                                group(
                                    "dumpling-pasta",
                                    "Gnocchi & ähnliche Teigwaren"
                                )
                            )
                    ),

                    department(
                        id =
                            "flour-and-baking",
                        name =
                            "Mehl & Backzutaten",

                        groups =
                            listOf(
                                group(
                                    "flour",
                                    "Mehl"
                                ),
                                group(
                                    "starch",
                                    "Stärke"
                                ),
                                group(
                                    "sugar",
                                    "Zucker"
                                ),
                                group(
                                    "sweeteners",
                                    "Süßungsmittel"
                                ),
                                group(
                                    "raising-agents",
                                    "Backtriebmittel"
                                ),
                                group(
                                    "yeast",
                                    "Hefe"
                                ),
                                group(
                                    "gelling-agents",
                                    "Geliermittel"
                                ),
                                group(
                                    "baking-flavours",
                                    "Backaromen"
                                ),
                                group(
                                    "baking-decoration",
                                    "Backdekoration"
                                )
                            )
                    ),

                    department(
                        id =
                            "oils-and-fats",
                        name =
                            "Öle & Fette",

                        groups =
                            listOf(
                                group(
                                    "olive-oil",
                                    "Olivenöl"
                                ),
                                group(
                                    "vegetable-oils",
                                    "Pflanzenöle"
                                ),
                                group(
                                    "nut-and-seed-oils",
                                    "Nuss- & Samenöle"
                                ),
                                group(
                                    "animal-fats",
                                    "Tierische Fette"
                                ),
                                group(
                                    "margarine",
                                    "Margarine"
                                ),
                                group(
                                    "cooking-fats",
                                    "Brat- & Kochfette"
                                )
                            )
                    ),

                    department(
                        id =
                            "spices-and-seasonings",
                        name =
                            "Gewürze & Würzmittel",

                        groups =
                            listOf(
                                group(
                                    "salt",
                                    "Salz"
                                ),
                                group(
                                    "pepper",
                                    "Pfeffer"
                                ),
                                group(
                                    "single-spices",
                                    "Einzelgewürze"
                                ),
                                group(
                                    "spice-blends",
                                    "Gewürzmischungen"
                                ),
                                group(
                                    "dried-herbs",
                                    "Getrocknete Kräuter"
                                ),
                                group(
                                    "seasoning-pastes",
                                    "Würzpasten"
                                ),
                                group(
                                    "broths-and-stocks",
                                    "Brühen & Fonds"
                                )
                            )
                    ),

                    department(
                        id =
                            "sauces-and-dressings",
                        name =
                            "Saucen, Dressings & Essig",

                        groups =
                            listOf(
                                group(
                                    "ketchup",
                                    "Ketchup"
                                ),
                                group(
                                    "mayonnaise",
                                    "Mayonnaise"
                                ),
                                group(
                                    "mustard",
                                    "Senf"
                                ),
                                group(
                                    "barbecue-sauces",
                                    "Grillsaucen"
                                ),
                                group(
                                    "cooking-sauces",
                                    "Kochsaucen"
                                ),
                                group(
                                    "asian-sauces",
                                    "Asiatische Saucen"
                                ),
                                group(
                                    "dressings",
                                    "Salatdressings"
                                ),
                                group(
                                    "dips-and-seasoning-pastes",
                                    "Dips & Würzpasten"
                                ),
                                group(
                                    "vinegar",
                                    "Essig"
                                )
                            )
                    ),

                    department(
                        id =
                            "breakfast-and-spreads",
                        name =
                            "Frühstück & Brotaufstriche",

                        groups =
                            listOf(
                                group(
                                    "breakfast-cereals",
                                    "Frühstückscerealien"
                                ),
                                group(
                                    "muesli",
                                    "Müsli"
                                ),
                                group(
                                    "porridge-and-flakes",
                                    "Flocken & Porridge"
                                ),
                                group(
                                    "fruit-spreads",
                                    "Konfitüren & Fruchtaufstriche"
                                ),
                                group(
                                    "honey",
                                    "Honig"
                                ),
                                group(
                                    "nut-spreads",
                                    "Nussaufstriche"
                                ),
                                group(
                                    "savoury-spreads",
                                    "Herzhafte Brotaufstriche"
                                )
                            )
                    ),

                    department(
                        id =
                            "nuts-seeds-and-dried-fruit",
                        name =
                            "Nüsse, Samen & Trockenfrüchte",

                        groups =
                            listOf(
                                group(
                                    "nuts",
                                    "Nüsse"
                                ),
                                group(
                                    "seeds",
                                    "Samen & Kerne"
                                ),
                                group(
                                    "dried-fruit",
                                    "Trockenfrüchte"
                                ),
                                group(
                                    "nut-and-fruit-mixes",
                                    "Nuss- & Fruchtmischungen"
                                )
                            )
                    ),

                    department(
                        id =
                            "snacks",
                        name =
                            "Snacks & Knabberartikel",

                        groups =
                            listOf(
                                group(
                                    "potato-chips",
                                    "Kartoffelchips"
                                ),
                                group(
                                    "vegetable-chips",
                                    "Gemüsechips"
                                ),
                                group(
                                    "flips",
                                    "Flips"
                                ),
                                group(
                                    "salted-snacks",
                                    "Salzgebäck"
                                ),
                                group(
                                    "crackers",
                                    "Cracker"
                                ),
                                group(
                                    "popcorn",
                                    "Popcorn"
                                )
                            )
                    ),

                    department(
                        id =
                            "confectionery",
                        name =
                            "Süßwaren",

                        groups =
                            listOf(
                                group(
                                    "chocolate",
                                    "Schokolade"
                                ),
                                group(
                                    "pralines",
                                    "Pralinen"
                                ),
                                group(
                                    "candy",
                                    "Bonbons"
                                ),
                                group(
                                    "fruit-gums",
                                    "Fruchtgummi"
                                ),
                                group(
                                    "liquorice",
                                    "Lakritz"
                                ),
                                group(
                                    "marzipan-and-nougat",
                                    "Marzipan & Nougat"
                                ),
                                group(
                                    "bars",
                                    "Riegel"
                                ),
                                group(
                                    "chewing-gum",
                                    "Kaugummi"
                                )
                            )
                    ),

                    department(
                        id =
                            "biscuits-and-sweet-baked-goods",
                        name =
                            "Kekse & süßes Gebäck",

                        groups =
                            listOf(
                                group(
                                    "biscuits",
                                    "Kekse"
                                ),
                                group(
                                    "wafers",
                                    "Waffeln"
                                ),
                                group(
                                    "gingerbread",
                                    "Lebkuchen"
                                ),
                                group(
                                    "sweet-pastries",
                                    "Süßgebäck"
                                )
                            )
                    ),

                    department(
                        id =
                            "ready-meals",
                        name =
                            "Fertiggerichte & zubereitete Speisen",

                        groups =
                            listOf(
                                group(
                                    "soups",
                                    "Suppen"
                                ),
                                group(
                                    "stews",
                                    "Eintöpfe"
                                ),
                                group(
                                    "pasta-dishes",
                                    "Nudelgerichte"
                                ),
                                group(
                                    "rice-dishes",
                                    "Reisgerichte"
                                ),
                                group(
                                    "potato-dishes",
                                    "Kartoffelgerichte"
                                ),
                                group(
                                    "meat-dishes",
                                    "Fleischgerichte"
                                ),
                                group(
                                    "fish-dishes",
                                    "Fischgerichte"
                                ),
                                group(
                                    "vegetable-dishes",
                                    "Gemüsegerichte"
                                ),
                                group(
                                    "pizza-and-flatbread-dishes",
                                    "Pizza & belegte Fladenbrote"
                                ),
                                group(
                                    "salads-and-cold-meals",
                                    "Salate & kalte Mahlzeiten"
                                )
                            )
                    ),

                    department(
                        id =
                            "ice-cream-and-desserts",
                        name =
                            "Eis & Desserts",

                        groups =
                            listOf(
                                group(
                                    "ice-cream",
                                    "Speiseeis"
                                ),
                                group(
                                    "sorbets",
                                    "Sorbet"
                                ),
                                group(
                                    "puddings",
                                    "Pudding"
                                ),
                                group(
                                    "dessert-creams",
                                    "Dessertcremes"
                                ),
                                group(
                                    "other-desserts",
                                    "Sonstige Desserts"
                                )
                            )
                    ),

                    department(
                        id =
                            "baby-and-toddler-food",
                        name =
                            "Baby- & Kleinkindnahrung",

                        groups =
                            listOf(
                                group(
                                    "infant-formula",
                                    "Säuglingsnahrung"
                                ),
                                group(
                                    "baby-porridge",
                                    "Babybreie"
                                ),
                                group(
                                    "baby-meals",
                                    "Babymenüs"
                                ),
                                group(
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
        groups: List<PrimaryFoodGroup>
    ): PrimaryFoodDepartment =
        PrimaryFoodDepartment(
            id =
                id,
            name =
                name,
            groups =
                groups.sortedBy {
                    it.id
                }
        )

    private fun group(
        id: String,
        name: String
    ): PrimaryFoodGroup =
        PrimaryFoodGroup(
            id =
                id,
            name =
                name
        )
}