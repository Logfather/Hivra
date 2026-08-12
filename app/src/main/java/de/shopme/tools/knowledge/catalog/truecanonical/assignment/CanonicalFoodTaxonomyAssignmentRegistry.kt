package de.shopme.tools.knowledge.catalog.truecanonical.assignment

import de.shopme.tools.knowledge.catalog.truecanonical.TrueCanonicalFoodNameNormalizer
import de.shopme.tools.knowledge.catalog.truecanonical.assignment.model.CanonicalFoodTaxonomyAssignment

object CanonicalFoodTaxonomyAssignmentRegistry {

    private val normalizer =
        TrueCanonicalFoodNameNormalizer()

    val assignments:
            Map<String, CanonicalFoodTaxonomyAssignment> =
        buildList {

            /*
             * ──────────────────────────────────────────
             * OBST
             * ──────────────────────────────────────────
             */

            assign(
                "fruit",
                "pome-fruit",
                "Äpfel",
                "Birnen"
            )

            assign(
                "fruit",
                "stone-fruit",
                "Aprikosen",
                "Kirschen",
                "Nektarinen",
                "Pfirsiche",
                "Pflaumen"
            )

            assign(
                "fruit",
                "berries",
                "Beerenmischung",
                "Blaubeeren",
                "Brombeeren",
                "Cranberries",
                "Erdbeeren",
                "Heidelbeeren",
                "Himbeeren",
                "Preiselbeeren",
                "Stachelbeeren"
            )

            assign(
                "fruit",
                "citrus-fruit",
                "Clementinen",
                "Grapefruits",
                "Limetten",
                "Orangen",
                "Zitronen"
            )

            assign(
                "fruit",
                "tropical-fruit",
                "Ananas",
                "Avocados",
                "Bananen",
                "Granatäpfel",
                "Kakis",
                "Kiwis",
                "Mangos",
                "Papayas"
            )

            assign(
                "fruit",
                "melons",
                "Honigmelonen",
                "Melonen",
                "Wassermelonen"
            )

            assign(
                "fruit",
                "grapes",
                "Trauben"
            )

            assign(
                "fruit",
                "other-fruit",
                "Datteln",
                "Feigen"
            )

            /*
             * ──────────────────────────────────────────
             * GEMÜSE
             * ──────────────────────────────────────────
             */

            assign(
                "vegetables",
                "leafy-vegetables",
                "Mangold",
                "Spinat"
            )

            assign(
                "vegetables",
                "cabbage-vegetables",
                "Blumenkohl",
                "Brokkoli",
                "Chinakohl",
                "Grünkohl",
                "Rosenkohl",
                "Rotkohl",
                "Spitzkohl",
                "Weißkohl",
                "Wirsing"
            )

            assign(
                "vegetables",
                "root-vegetables",
                "Karotten",
                "Pastinaken",
                "Radieschen",
                "Rettich"
            )

            assign(
                "vegetables",
                "tuber-vegetables",
                "Kartoffeln",
                "Kohlrabi",
                "Sellerie"
            )

            assign(
                "vegetables",
                "pod-vegetables",
                "Grüne Bohnen"
            )

            assign(
                "vegetables",
                "fruiting-vegetables",
                "Auberginen",
                "Cherrytomaten",
                "Gurken",
                "Kürbis",
                "Paprika",
                "Tomaten",
                "Zucchini"
            )

            assign(
                "vegetables",
                "allium-vegetables",
                "Frühlingszwiebeln",
                "Knoblauch",
                "Lauch",
                "Zwiebeln"
            )

            assign(
                "vegetables",
                "stem-vegetables",
                "Artischocken",
                "Fenchel",
                "Spargel"
            )

            assign(
                "vegetables",
                "salads",
                "Chicorée",
                "Eisbergsalat",
                "Endiviensalat",
                "Feldsalat",
                "Rucola"
            )

            assign(
                "vegetables",
                "mushrooms",
                "Champignons"
            )

            /*
             * ──────────────────────────────────────────
             * HÜLSENFRÜCHTE / GETREIDE / REIS
             * ──────────────────────────────────────────
             */

            assign(
                "grains-rice-and-legumes",
                "lentils",
                "Belugalinsen",
                "Gelbe Linsen",
                "Linsen",
                "Rote Linsen"
            )

            assign(
                "grains-rice-and-legumes",
                "beans",
                "Kidneybohnen",
                "Weiße Bohnen"
            )

            assign(
                "grains-rice-and-legumes",
                "peas",
                "Erbsen"
            )

            assign(
                "grains-rice-and-legumes",
                "chickpeas",
                "Kichererbsen"
            )

            assign(
                "grains-rice-and-legumes",
                "rice",
                "Basmatireis",
                "Jasminreis",
                "Langkornreis",
                "Milchreis",
                "Risottoreis",
                "Vollkornreis"
            )

            assign(
                "grains-rice-and-legumes",
                "spelt",
                "Dinkel"
            )

            assign(
                "grains-rice-and-legumes",
                "oats",
                "Hafer",
                "Haferflocken"
            )

            assign(
                "grains-rice-and-legumes",
                "barley",
                "Gerste"
            )

            assign(
                "grains-rice-and-legumes",
                "millet",
                "Hirse"
            )

            assign(
                "grains-rice-and-legumes",
                "corn",
                "Mais"
            )

            assign(
                "grains-rice-and-legumes",
                "pseudocereals",
                "Amaranth",
                "Buchweizen",
                "Quinoa"
            )

            assign(
                "grains-rice-and-legumes",
                "couscous",
                "Couscous"
            )

            assign(
                "grains-rice-and-legumes",
                "bulgur",
                "Bulgur",
                "Grünkern"
            )

            /*
             * ──────────────────────────────────────────
             * FLEISCH
             * ──────────────────────────────────────────
             */

            assign(
                "meat",
                "beef",
                "Rindfleisch",
                "Rinderbraten",
                "Rinderfilet",
                "Rindergulasch",
                "Rinderrouladen",
                "Rindersteak"
            )

            assign(
                "meat",
                "pork",
                "Schweinefleisch",
                "Schweinebraten",
                "Schweinefilet",
                "Schweineschnitzel"
            )

            assign(
                "meat",
                "veal",
                "Kalbfleisch"
            )

            assign(
                "meat",
                "lamb-and-mutton",
                "Lammfleisch"
            )

            assign(
                "meat",
                "chicken",
                "Hähnchenfleisch",
                "Hähnchenbrustfilets",
                "Hähnchenkeulen",
                "Hähnchenschenkel"
            )

            assign(
                "meat",
                "turkey",
                "Putenfleisch"
            )

            assign(
                "meat",
                "duck-and-goose",
                "Entenfleisch",
                "Gänsefleisch"
            )

            assign(
                "meat",
                "game",
                "Hirschfleisch",
                "Kaninchenfleisch"
            )

            assign(
                "meat",
                "minced-meat",
                "Geflügelhackfleisch",
                "Hackfleisch gemischt",
                "Rinderhackfleisch",
                "Schweinehackfleisch"
            )

            /*
             * ──────────────────────────────────────────
             * WURST & SCHINKEN
             * ──────────────────────────────────────────
             */

            assign(
                "sausage-and-ham",
                "raw-sausage",
                "Cabanossi",
                "Cervelat",
                "Chorizo",
                "Mettwurst",
                "Salami",
                "Teewurst"
            )

            assign(
                "sausage-and-ham",
                "boiled-sausage",
                "Bockwürste",
                "Fleischwurst",
                "Gelbwurst",
                "Jagdwurst",
                "Wiener Würstchen"
            )

            assign(
                "sausage-and-ham",
                "cooked-sausage",
                "Blutwurst",
                "Leberwurst"
            )

            assign(
                "sausage-and-ham",
                "fried-sausage",
                "Bratwürste"
            )

            assign(
                "sausage-and-ham",
                "ham",
                "Kochschinken",
                "Schinken"
            )

            /*
             * ──────────────────────────────────────────
             * FISCH & MEERESFRÜCHTE
             * ──────────────────────────────────────────
             */

            assign(
                "fish-and-seafood",
                "freshwater-fish",
                "Aal",
                "Forellen",
                "Karpfen"
            )

            assign(
                "fish-and-seafood",
                "saltwater-fish",
                "Heilbutt",
                "Hering",
                "Kabeljau",
                "Lachs",
                "Makrelen",
                "Rotbarsch",
                "Sardinen",
                "Seelachs",
                "Thunfisch"
            )

            assign(
                "fish-and-seafood",
                "crustaceans",
                "Garnelen",
                "Hummer"
            )

            assign(
                "fish-and-seafood",
                "molluscs",
                "Calamari"
            )

            assign(
                "fish-and-seafood",
                "shellfish",
                "Austern",
                "Jakobsmuscheln",
                "Miesmuscheln"
            )

            /*
             * ──────────────────────────────────────────
             * MOLKEREI & EIER
             * ──────────────────────────────────────────
             */

            assign(
                "dairy-and-eggs",
                "milk",
                "Fettarme Milch",
                "Frischmilch",
                "H-Milch",
                "Milch",
                "Vollmilch"
            )

            assign(
                "dairy-and-eggs",
                "cultured-milk-products",
                "Buttermilch",
                "Kefir"
            )

            assign(
                "dairy-and-eggs",
                "yogurt",
                "Fruchtjoghurt",
                "Griechischer Joghurt",
                "Naturjoghurt"
            )

            assign(
                "dairy-and-eggs",
                "quark-and-skyr",
                "Magerquark",
                "Quark",
                "Skyr"
            )

            assign(
                "dairy-and-eggs",
                "cream-products",
                "Crème fraîche",
                "Saure Sahne",
                "Sahne",
                "Schmand"
            )

            assign(
                "dairy-and-eggs",
                "butter",
                "Butter"
            )

            assign(
                "dairy-and-eggs",
                "fresh-cheese",
                "Frischkäse",
                "Hüttenkäse"
            )

            assign(
                "dairy-and-eggs",
                "soft-cheese",
                "Brie",
                "Camembert"
            )

            assign(
                "dairy-and-eggs",
                "semi-hard-cheese",
                "Edamer",
                "Gouda"
            )

            assign(
                "dairy-and-eggs",
                "hard-cheese",
                "Bergkäse",
                "Cheddar",
                "Emmentaler",
                "Parmesan"
            )

            assign(
                "dairy-and-eggs",
                "brined-cheese",
                "Büffelmozzarella",
                "Feta",
                "Halloumi",
                "Mozzarella"
            )

            assign(
                "dairy-and-eggs",
                "eggs",
                "Eier"
            )

            /*
             * ──────────────────────────────────────────
             * BROT & BACKWAREN
             * ──────────────────────────────────────────
             */

            assign(
                "bakery",
                "bread",
                "Bauernbrot",
                "Dinkelbrot",
                "Mischbrot",
                "Roggenbrot",
                "Vollkornbrot",
                "Weißbrot",
                "Weizenbrot"
            )

            assign(
                "bakery",
                "bread-rolls",
                "Brötchen",
                "Burgerbrötchen",
                "Körnerbrötchen"
            )

            assign(
                "bakery",
                "toast-and-sandwich-bread",
                "Toastbrot"
            )

            assign(
                "bakery",
                "flatbread",
                "Fladenbrot",
                "Pitabrot"
            )

            assign(
                "bakery",
                "crispbread-and-rusks",
                "Knäckebrot"
            )

            assign(
                "bakery",
                "small-baked-goods",
                "Baguette",
                "Brezeln"
            )

            assign(
                "bakery",
                "pastries",
                "Ciabatta",
                "Croissants"
            )

            /*
             * ──────────────────────────────────────────
             * PASTA
             * ──────────────────────────────────────────
             */

            assign(
                "pasta",
                "dried-pasta",
                "Bandnudeln",
                "Farfalle",
                "Fettuccine",
                "Fusilli",
                "Makkaroni",
                "Penne",
                "Rigatoni",
                "Spaghetti",
                "Tagliatelle"
            )

            assign(
                "pasta",
                "dumpling-pasta",
                "Gnocchi"
            )

            assign(
                "pasta",
                "dried-pasta",
                "Lasagneplatten"
            )

            /*
             * ──────────────────────────────────────────
             * MEHL & BACKZUTATEN
             * ──────────────────────────────────────────
             */

            assign(
                "flour-and-baking",
                "flour",
                "Buchweizenmehl",
                "Dinkelmehl",
                "Kichererbsenmehl",
                "Roggenmehl",
                "Vollkornmehl",
                "Weizenmehl"
            )

            assign(
                "flour-and-baking",
                "starch",
                "Speisestärke"
            )

            assign(
                "flour-and-baking",
                "raising-agents",
                "Backpulver"
            )

            assign(
                "flour-and-baking",
                "yeast",
                "Hefe",
                "Trockenhefe"
            )

            assign(
                "flour-and-baking",
                "gelling-agents",
                "Gelatine"
            )

            /*
             * ──────────────────────────────────────────
             * ÖLE & FETTE
             * ──────────────────────────────────────────
             */

            assign(
                "oils-and-fats",
                "olive-oil",
                "Olivenöl"
            )

            assign(
                "oils-and-fats",
                "vegetable-oils",
                "Avocadoöl",
                "Kokosöl",
                "Rapsöl",
                "Sonnenblumenöl"
            )

            assign(
                "oils-and-fats",
                "nut-and-seed-oils",
                "Erdnussöl",
                "Leinöl",
                "Sesamöl",
                "Walnussöl"
            )

            assign(
                "oils-and-fats",
                "cooking-fats",
                "Ghee"
            )

            /*
             * ──────────────────────────────────────────
             * GETRÄNKE
             * ──────────────────────────────────────────
             */

            assign(
                "beverages",
                "fruit-juices",
                "Apfelsaft",
                "Birnensaft",
                "Cranberrysaft",
                "Granatapfelsaft",
                "Grapefruitsaft",
                "Kirschsaft",
                "Multivitaminsaft",
                "Orangensaft",
                "Traubensaft"
            )

            assign(
                "beverages",
                "vegetable-juices",
                "Karottensaft",
                "Tomatensaft"
            )

            assign(
                "beverages",
                "spritzers",
                "Apfelschorle"
            )

            assign(
                "beverages",
                "soft-drinks",
                "Cola",
                "Limonade"
            )

            assign(
                "beverages",
                "water",
                "Mineralwasser",
                "Tafelwasser"
            )

            assign(
                "beverages",
                "coffee",
                "Espresso",
                "Kaffee"
            )

            assign(
                "beverages",
                "tea",
                "Eistee",
                "Früchtetee",
                "Grüner Tee",
                "Kamillentee",
                "Kräutertee",
                "Pfefferminztee",
                "Schwarzer Tee"
            )

            assign(
                "beverages",
                "beer",
                "Bier"
            )

            assign(
                "beverages",
                "wine",
                "Apfelwein",
                "Wein"
            )

            /*
             * ──────────────────────────────────────────
             * PFLANZLICHE ALTERNATIVEN
             * ──────────────────────────────────────────
             */

            assign(
                "plant-based-foods",
                "plant-drinks",
                "Haferdrink",
                "Kokosdrink",
                "Mandeldrink",
                "Reisdrink",
                "Sojadrink"
            )

            assign(
                "plant-based-foods",
                "plant-yogurt-alternatives",
                "Cashewjoghurt",
                "Haferjoghurt",
                "Kokosjoghurt",
                "Sojajoghurt"
            )

            assign(
                "plant-based-foods",
                "tofu",
                "Tofu"
            )

            assign(
                "plant-based-foods",
                "tempeh",
                "Tempeh"
            )

            /*
             * ──────────────────────────────────────────
             * SAUCEN
             * ──────────────────────────────────────────
             */

            assign(
                "sauces-and-dressings",
                "ketchup",
                "Ketchup",
                "Tomatenketchup"
            )

            assign(
                "sauces-and-dressings",
                "mayonnaise",
                "Mayonnaise"
            )

            assign(
                "sauces-and-dressings",
                "mustard",
                "Senf"
            )

            assign(
                "sauces-and-dressings",
                "barbecue-sauces",
                "Barbecuesoße"
            )

            assign(
                "sauces-and-dressings",
                "cooking-sauces",
                "Bratensoße",
                "Chilisoße",
                "Currysoße"
            )

            assign(
                "sauces-and-dressings",
                "asian-sauces",
                "Fischsauce",
                "Sojasauce"
            )

            assign(
                "sauces-and-dressings",
                "vinegar",
                "Apfelessig",
                "Balsamicoessig"
            )

            assign(
                "sauces-and-dressings",
                "dips-and-seasoning-pastes",
                "Ajvar",
                "Guacamole"
            )

            /*
             * ──────────────────────────────────────────
             * BROTAUFSTRICHE
             * ──────────────────────────────────────────
             */

            assign(
                "breakfast-and-spreads",
                "nut-spreads",
                "Erdnussmus",
                "Haselnussmus",
                "Mandelmus"
            )

            /*
             * ──────────────────────────────────────────
             * READY MEALS
             * ──────────────────────────────────────────
             */

            assign(
                "ready-meals",
                "stews",
                "Chili con Carne",
                "Gulasch"
            )

            assign(
                "ready-meals",
                "meat-dishes",
                "Currywurst",
                "Hühnerfrikassee"
            )

            assign(
                "ready-meals",
                "vegetable-dishes",
                "Falafel",
                "Gemüseauflauf",
                "Gemüsecurry",
                "Gemüselasagne",
                "Gemüsepfanne",
                "Ratatouille"
            )

            assign(
                "ready-meals",
                "soups",
                "Gemüsesuppe",
                "Kartoffelsuppe"
            )

            assign(
                "ready-meals",
                "potato-dishes",
                "Kartoffelgratin"
            )

            assign(
                "ready-meals",
                "pasta-dishes",
                "Lasagne"
            )

            assign(
                "ready-meals",
                "rice-dishes",
                "Risotto"
            )

            assign(
                "ready-meals",
                "pizza-and-flatbread-dishes",
                "Pizza"
            )

            assign(
                "ready-meals",
                "vegetable-dishes",
                "Moussaka"
            )

            /*
             * ──────────────────────────────────────────
             * SNACKS
             * ──────────────────────────────────────────
             */

            assign(
                "snacks",
                "potato-chips",
                "Kartoffelchips"
            )

            assign(
                "snacks",
                "vegetable-chips",
                "Gemüsechips"
            )

            assign(
                "snacks",
                "flips",
                "Erdnussflips"
            )

            assign(
                "snacks",
                "salted-snacks",
                "Salzstangen"
            )

            assign(
                "snacks",
                "crackers",
                "Cracker"
            )

            assign(
                "snacks",
                "popcorn",
                "Popcorn"
            )

            /*
             * ──────────────────────────────────────────
             * SÜSSWAREN
             * ──────────────────────────────────────────
             */

            assign(
                "confectionery",
                "chocolate",
                "Milchschokolade",
                "Weiße Schokolade",
                "Zartbitterschokolade"
            )

            assign(
                "confectionery",
                "candy",
                "Bonbons"
            )

            assign(
                "confectionery",
                "fruit-gums",
                "Fruchtgummi",
                "Gummibärchen"
            )

            assign(
                "confectionery",
                "marzipan-and-nougat",
                "Marzipan",
                "Nougat"
            )

            /*
             * Kekse gehören in unsere eigene
             * Bakery-/Biscuit-Abteilung.
             */

            assign(
                "biscuits-and-sweet-baked-goods",
                "biscuits",
                "Kekse"
            )

            /*
             * ──────────────────────────────────────────
             * GEWÜRZE
             * ──────────────────────────────────────────
             */

            assign(
                "spices-and-seasonings",
                "salt",
                "Salz"
            )

            assign(
                "spices-and-seasonings",
                "pepper",
                "Pfeffer"
            )

            assign(
                "spices-and-seasonings",
                "single-spices",
                "Anis",
                "Cayennepfeffer",
                "Chili",
                "Fenchelsamen",
                "Ingwer",
                "Kardamom",
                "Koriander",
                "Kreuzkümmel",
                "Kurkuma",
                "Muskatnuss",
                "Paprikapulver",
                "Vanille",
                "Zimt"
            )

            assign(
                "spices-and-seasonings",
                "dried-herbs",
                "Basilikum",
                "Dill",
                "Estragon",
                "Kerbel",
                "Majoran",
                "Oregano",
                "Petersilie",
                "Rosmarin",
                "Thymian"
            )

        }
            .associateBy {
                it.normalized
            }

    private fun MutableList<CanonicalFoodTaxonomyAssignment>.assign(
        departmentId: String,
        groupId: String,
        vararg itemnames: String
    ) {

        itemnames.forEach { itemname ->

            val normalized =
                normalizer.normalize(
                    itemname
                )

            add(
                CanonicalFoodTaxonomyAssignment(
                    normalized =
                        normalized,
                    departmentId =
                        departmentId,
                    groupId =
                        groupId
                )
            )
        }
    }
}