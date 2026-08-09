package de.shopme.testing.system.tools.knowledge.catalog.category.migration

import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import java.text.Normalizer
import java.util.Locale

class CatalogLegacyCategoryMigrator {

    fun migrate(
        item: CatalogFoodItem,
        registry: CanonicalFoodCategoryRegistry
    ): CatalogCategoryMigrationResult {
        val originalCategory =
            item.category
                ?.trim()
                ?.takeIf(String::isNotBlank)

        if (originalCategory == null) {
            return unresolved(
                originalCategory = null
            )
        }

        val normalizedCategory =
            normalizeComparisonText(
                originalCategory
            )

        /*
         * Bekannte Legacy-Kategorien müssen vor der allgemeinen Registry-
         * Prüfung ausgewertet werden.
         *
         * Hintergrund:
         * Die Registry kann historische Schreibweisen oder Display-Namen wie
         * "Snacks" als bekannt akzeptieren. Für den kanonischen Katalog muss
         * daraus dennoch der lowercase Key "snacks" werden.
         */
        val directMigrationRule =
            DIRECT_MIGRATIONS[
                normalizedCategory
            ]

        if (directMigrationRule != null) {
            val target =
                resolveExistingTarget(
                    registry = registry,
                    candidates =
                        directMigrationRule
                            .targetCandidates
                )

            if (target != null) {
                /*
                 * Der Eintrag verwendet bereits exakt den kanonischen Key.
                 *
                 * Beispiel:
                 * originalCategory = "snacks"
                 * target           = "snacks"
                 */
                if (originalCategory == target) {
                    return CatalogCategoryMigrationResult(
                        originalCategory =
                            originalCategory,

                        resultingCategory =
                            originalCategory,

                        status =
                            CatalogCategoryMigrationStatus
                                .ALREADY_CANONICAL,

                        ruleId = null,
                        confidence = 1.0
                    )
                }

                /*
                 * Bekannte Legacy- oder Display-Schreibweise.
                 *
                 * Beispiel:
                 * originalCategory = "Snacks"
                 * target           = "snacks"
                 */
                return CatalogCategoryMigrationResult(
                    originalCategory =
                        originalCategory,

                    resultingCategory =
                        target,

                    status =
                        CatalogCategoryMigrationStatus
                            .MIGRATED_DIRECTLY,

                    ruleId =
                        directMigrationRule.ruleId,

                    confidence =
                        directMigrationRule.confidence
                )
            }
        }

        /*
         * Erst nachdem bekannte Legacy-Mappings ausgeschlossen wurden, darf
         * eine Kategorie aufgrund der Registry als bereits kanonisch gelten.
         */
        if (registry.contains(originalCategory)) {
            return CatalogCategoryMigrationResult(
                originalCategory =
                    originalCategory,

                resultingCategory =
                    originalCategory,

                status =
                    CatalogCategoryMigrationStatus
                        .ALREADY_CANONICAL,

                ruleId = null,
                confidence = 1.0
            )
        }

        return migrateContextDependentCategory(
            item = item,
            originalCategory = originalCategory,
            normalizedCategory = normalizedCategory,
            registry = registry
        )
    }

    private fun migrateContextDependentCategory(
        item: CatalogFoodItem,
        originalCategory: String,
        normalizedCategory: String,
        registry: CanonicalFoodCategoryRegistry
    ): CatalogCategoryMigrationResult {
        val tokens = productTokens(item)

        val decision = when (normalizedCategory) {
            "vegan",
            "vegetarisch" ->
                resolveDietOverlayCategory(
                    tokens = tokens,
                    registry = registry
                )

            "tiefkuehlprodukte",
            "tiefkuehl",
            "tiefkuhlprodukte",
            "tiefkuhl" ->
                resolveFrozenCategory(
                    tokens = tokens,
                    registry = registry
                )

            "getreideprodukte" ->
                resolveGrainProductCategory(
                    tokens = tokens,
                    registry = registry
                )

            "milchalternativen",
            "milchmalternativen",
            "pflanzliche alternativen" ->
                resolvePlantBasedAlternativeCategory(
                    tokens = tokens,
                    registry = registry
                )

            else -> null
        }

        return if (decision != null) {
            CatalogCategoryMigrationResult(
                originalCategory = originalCategory,
                resultingCategory = decision.targetCategory,
                status =
                    CatalogCategoryMigrationStatus.MIGRATED_BY_PRODUCT_RULE,
                ruleId = decision.ruleId,
                confidence = decision.confidence
            )
        } else {
            unresolved(originalCategory)
        }
    }

    private fun resolveDietOverlayCategory(
        tokens: Set<String>,
        registry: CanonicalFoodCategoryRegistry
    ): ProductRuleDecision? =
        firstMatchingRule(
            tokens = tokens,
            registry = registry,
            rules = listOf(
                productRule(
                    id = "diet-overlay-plant-drink",
                    requiredAny = setOf(
                        "haferdrink",
                        "hafermilch",
                        "mandeldrink",
                        "mandelmilch",
                        "sojadrink",
                        "sojamilch",
                        "reisdrink",
                        "reismilch",
                        "milchalternative",
                        "pflanzendrink"
                    ),
                    targets = listOf(
                        "plant-based-drinks",
                        "plant-based-alternatives"
                    ),
                    confidence = 0.99
                ),
                productRule(
                    id = "diet-overlay-cheese-alternative",
                    requiredAny = setOf(
                        "kaesealternative",
                        "käsealternative",
                        "veganerkaese",
                        "veganerkäse",
                        "kaeseersatz",
                        "käseersatz"
                    ),
                    targets = listOf(
                        "plant-based-cheese",
                        "plant-based-alternatives"
                    ),
                    confidence = 0.98
                ),
                productRule(
                    id = "diet-overlay-butter-alternative",
                    requiredAny = setOf(
                        "vegane butter",
                        "pflanzenbutter",
                        "margarine"
                    ),
                    targets = listOf(
                        "plant-based-butter",
                        "plant-based-alternatives",
                        "spreads"
                    ),
                    confidence = 0.98
                ),
                productRule(
                    id = "diet-overlay-cream-alternative",
                    requiredAny = setOf(
                        "sahnealternative",
                        "schlagcreme",
                        "kochcreme",
                        "vegane sahne"
                    ),
                    targets = listOf(
                        "plant-based-cream",
                        "plant-based-alternatives"
                    ),
                    confidence = 0.98
                ),
                productRule(
                    id = "diet-overlay-yogurt-alternative",
                    requiredAny = setOf(
                        "joghurtalternative",
                        "sojajoghurt",
                        "kokosjoghurt",
                        "pflanzenjoghurt"
                    ),
                    targets = listOf(
                        "plant-based-yogurt",
                        "plant-based-alternatives"
                    ),
                    confidence = 0.98
                ),
                productRule(
                    id = "diet-overlay-sauce",
                    requiredAny = setOf(
                        "mayonnaise",
                        "mayo",
                        "ketchup",
                        "sauce",
                        "dressing",
                        "pesto",
                        "senf"
                    ),
                    targets = listOf("sauces"),
                    confidence = 0.97
                ),
                productRule(
                    id = "diet-overlay-sausage",
                    requiredAny = setOf(
                        "wurst",
                        "bratwurst",
                        "aufschnitt",
                        "salami",
                        "wuerstchen",
                        "würstchen"
                    ),
                    targets = listOf(
                        "plant-based-meat",
                        "sausage",
                        "plant-based-alternatives"
                    ),
                    confidence = 0.96
                ),
                productRule(
                    id = "diet-overlay-meat-alternative",
                    requiredAny = setOf(
                        "schnitzel",
                        "burger",
                        "hack",
                        "frikadelle",
                        "steak",
                        "filet",
                        "nuggets",
                        "gyros",
                        "geschnetzeltes",
                        "fleischalternative"
                    ),
                    targets = listOf(
                        "plant-based-meat",
                        "plant-based-alternatives",
                        "ready-meals"
                    ),
                    confidence = 0.95
                ),
                productRule(
                    id = "diet-overlay-spread",
                    requiredAny = setOf(
                        "brotaufstrich",
                        "aufstrich",
                        "streichcreme"
                    ),
                    targets = listOf(
                        "spreads",
                        "plant-based-alternatives"
                    ),
                    confidence = 0.96
                ),
                productRule(
                    id = "diet-overlay-ready-meal",
                    requiredAny = setOf(
                        "pizza",
                        "lasagne",
                        "curry",
                        "eintopf",
                        "gericht",
                        "pfanne",
                        "auflauf",
                        "suppe",
                        "falafel"
                    ),
                    targets = listOf("ready-meals"),
                    confidence = 0.93
                ),
                productRule(
                    id = "diet-overlay-confectionery",
                    requiredAny = setOf(
                        "schokolade",
                        "praline",
                        "bonbon",
                        "gummibaerchen",
                        "gummibärchen",
                        "keks",
                        "cookie"
                    ),
                    targets = listOf("confectionery"),
                    confidence = 0.94
                ),
                productRule(
                    id = "diet-overlay-snack",
                    requiredAny = setOf(
                        "chips",
                        "cracker",
                        "snack",
                        "popcorn"
                    ),
                    targets = listOf("snacks"),
                    confidence = 0.94
                )
            )
        ) ?: resolveExistingTarget(
            registry = registry,
            candidates = listOf("plant-based-alternatives")
        )?.let { target ->
            ProductRuleDecision(
                targetCategory = target,
                ruleId = "diet-overlay-generic-plant-based-alternative",
                confidence = 0.82
            )
        }

    private fun resolveFrozenCategory(
        tokens: Set<String>,
        registry: CanonicalFoodCategoryRegistry
    ): ProductRuleDecision? =
        firstMatchingRule(
            tokens = tokens,
            registry = registry,
            rules = listOf(
                productRule(
                    id = "frozen-vegetables",
                    requiredAny = setOf(
                        "gemuese",
                        "gemüse",
                        "spinat",
                        "brokkoli",
                        "blumenkohl",
                        "erbsen",
                        "bohnen",
                        "mais",
                        "paprika",
                        "gemuesemischung",
                        "gemüsemischung"
                    ),
                    targets = listOf("vegetables"),
                    confidence = 0.98
                ),
                productRule(
                    id = "frozen-fruit",
                    requiredAny = setOf(
                        "beeren",
                        "erdbeeren",
                        "himbeeren",
                        "heidelbeeren",
                        "obst",
                        "fruechte",
                        "früchte",
                        "mango",
                        "kirschen"
                    ),
                    targets = listOf("fruit"),
                    confidence = 0.98
                ),
                productRule(
                    id = "frozen-fish",
                    requiredAny = setOf(
                        "fisch",
                        "lachs",
                        "kabeljau",
                        "seelachs",
                        "forelle",
                        "garnelen",
                        "fischstaebchen",
                        "fischstäbchen"
                    ),
                    targets = listOf("fish"),
                    confidence = 0.97
                ),
                productRule(
                    id = "frozen-meat",
                    requiredAny = setOf(
                        "fleisch",
                        "huhn",
                        "haehnchen",
                        "hähnchen",
                        "rind",
                        "schwein",
                        "pute",
                        "schnitzel"
                    ),
                    targets = listOf("meat"),
                    confidence = 0.96
                ),
                productRule(
                    id = "frozen-bakery",
                    requiredAny = setOf(
                        "brot",
                        "broetchen",
                        "brötchen",
                        "croissant",
                        "baguette",
                        "kuchen",
                        "torte",
                        "teig"
                    ),
                    targets = listOf("bakery"),
                    confidence = 0.96
                ),
                productRule(
                    id = "frozen-snacks",
                    requiredAny = setOf(
                        "pommes",
                        "kroketten",
                        "wedges",
                        "roesti",
                        "rösti"
                    ),
                    targets = listOf(
                        "snacks",
                        "ready-meals"
                    ),
                    confidence = 0.94
                ),
                productRule(
                    id = "frozen-ready-meal",
                    requiredAny = setOf(
                        "pizza",
                        "lasagne",
                        "gericht",
                        "pfanne",
                        "auflauf",
                        "suppe",
                        "eintopf"
                    ),
                    targets = listOf("ready-meals"),
                    confidence = 0.96
                )
            )
        ) ?: resolveExistingTarget(
            registry = registry,
            candidates = listOf("ready-meals")
        )?.let { target ->
            ProductRuleDecision(
                targetCategory = target,
                ruleId = "frozen-generic-ready-meal",
                confidence = 0.80
            )
        }

    private fun resolveGrainProductCategory(
        tokens: Set<String>,
        registry: CanonicalFoodCategoryRegistry
    ): ProductRuleDecision? {
        /*
         * Die historische Kategorie "Getreideprodukte" enthält nicht nur
         * Getreide. Sie wurde ebenfalls für Hülsenfrüchte, Mehle, Stärke,
         * Backzutaten, Frühstückscerealien und Reisprodukte verwendet.
         *
         * Die Regeln sind absichtlich von spezifisch nach allgemein sortiert.
         * Dadurch gewinnt beispielsweise "Kichererbsenmehl" die Mehlregel,
         * bevor eine allgemeine Hülsenfruchtregel greifen kann.
         */

        return firstMatchingRule(
            tokens = tokens,
            registry = registry,
            rules = listOf(
                /*
                 * Backtriebmittel und Hefe
                 */
                productRule(
                    id = "grain-product-baking-yeast",
                    requiredAny = setOf(
                        "hefe",
                        "hefe frisch",
                        "hefeflocken",
                        "hefewuerfel",
                        "hefewürfel",
                        "trockenhefe",
                        "backhefe"
                    ),
                    targets = listOf(
                        "baking-ingredients",
                        "baking",
                        "bakery",
                        "grains"
                    ),
                    confidence = 0.99
                ),

                /*
                 * Mehle
                 */
                productRule(
                    id = "grain-product-flour",
                    requiredAny = setOf(
                        "mehl",
                        "mehlmischung",
                        "vollkornmehl",
                        "glutenfreies mehl",
                        "glutenfreie mehlmischung",
                        "gruenkernmehl",
                        "grünkernmehl",
                        "kamutmehl",
                        "kartoffelmehl",
                        "kichererbsenmehl",
                        "linsenmehl",
                        "lupinenmehl",
                        "maismehl",
                        "mandelmehl",
                        "reismehl",
                        "teffmehl"
                    ),
                    targets = listOf(
                        "flours",
                        "flour",
                        "baking-ingredients",
                        "grains"
                    ),
                    confidence = 0.99
                ),

                /*
                 * Stärke und stärkebasierte Produkte
                 */
                productRule(
                    id = "grain-product-starch",
                    requiredAny = setOf(
                        "staerke",
                        "stärke",
                        "maisstaerke",
                        "maisstärke",
                        "maizena",
                        "tapioka",
                        "kartoffelstaerke",
                        "kartoffelstärke"
                    ),
                    targets = listOf(
                        "starches",
                        "baking-ingredients",
                        "grains"
                    ),
                    confidence = 0.99
                ),

                /*
                 * Semmelbrösel und Panierprodukte
                 */
                productRule(
                    id = "grain-product-breadcrumbs",
                    requiredAny = setOf(
                        "semmelbroesel",
                        "semmelbrösel",
                        "paniermehl",
                        "brotbroesel",
                        "brotbrösel"
                    ),
                    targets = listOf(
                        "baking-ingredients",
                        "bakery",
                        "grains"
                    ),
                    confidence = 0.98
                ),

                /*
                 * Frühstückscerealien und Flocken.
                 *
                 * Reisflocken und Maisflocken werden hier bewusst als
                 * Frühstücksprodukte priorisiert.
                 */
                productRule(
                    id = "grain-product-breakfast-cereal",
                    requiredAny = setOf(
                        "fruehstueckscerealien",
                        "frühstückscerealien",
                        "cerealien",
                        "cornflakes",
                        "maisflocken",
                        "reisflocken",
                        "haferflocken",
                        "muesli",
                        "müsli",
                        "granola",
                        "porridge"
                    ),
                    targets = listOf(
                        "breakfast",
                        "cereals",
                        "grains"
                    ),
                    confidence = 0.98
                ),

                /*
                 * Reis und Reisprodukte
                 */
                productRule(
                    id = "grain-product-rice",
                    requiredAny = setOf(
                        "reis",
                        "milchreis",
                        "reisvollkorn",
                        "basmati",
                        "jasminreis",
                        "risottoreis",
                        "wildreis"
                    ),
                    targets = listOf(
                        "rice",
                        "grains"
                    ),
                    confidence = 0.99
                ),

                /*
                 * Pasta
                 */
                productRule(
                    id = "grain-product-pasta",
                    requiredAny = setOf(
                        "nudeln",
                        "pasta",
                        "spaghetti",
                        "penne",
                        "fusilli",
                        "tagliatelle",
                        "lasagneplatten",
                        "kamut nudeln",
                        "kamutnudeln"
                    ),
                    targets = listOf(
                        "pasta",
                        "grains"
                    ),
                    confidence = 0.99
                ),

                /*
                 * Hülsenfrüchte.
                 *
                 * Diese Regel steht nach Mehl und Flocken, damit beispielsweise
                 * "Kichererbsenmehl" nicht lediglich als Hülsenfrucht landet.
                 */
                productRule(
                    id = "grain-product-legume",
                    requiredAny = setOf(
                        "linse",
                        "linsen",
                        "braune linsen",
                        "gruene linsen",
                        "grüne linsen",
                        "rote linse",
                        "rote linsen",
                        "schwarze linsen",
                        "erbsen getrocknet",
                        "getrocknete erbsen",
                        "gruene erbsen getrocknet",
                        "grüne erbsen getrocknet",
                        "kichererbsen getrocknet",
                        "kidneybohnen getrocknet",
                        "bohnen getrocknet",
                        "lupinen"
                    ),
                    targets = listOf(
                        "legumes",
                        "pulses",
                        "vegetables",
                        "grains"
                    ),
                    confidence = 0.98
                ),

                /*
                 * Couscous, Polenta, Grieß und Graupen
                 */
                productRule(
                    id = "grain-product-processed-grain",
                    requiredAny = setOf(
                        "kuszkus",
                        "couscous",
                        "polenta",
                        "graupen",
                        "gries",
                        "grieß",
                        "maisschrot",
                        "schrot",
                        "bulgur"
                    ),
                    targets = listOf(
                        "grains",
                        "cereals"
                    ),
                    confidence = 0.98
                ),

                /*
                 * Getreide und Pseudogetreide
                 */
                productRule(
                    id = "grain-product-grain",
                    requiredAny = setOf(
                        "amaranth",
                        "kamut",
                        "gruenkern",
                        "grünkern",
                        "sorghum",
                        "teff",
                        "hafer",
                        "weizen",
                        "roggen",
                        "dinkel",
                        "gerste",
                        "hirse",
                        "quinoa",
                        "buchweizen",
                        "mais"
                    ),
                    targets = listOf(
                        "grains",
                        "cereals"
                    ),
                    confidence = 0.98
                )
            )
        ) ?: resolveExistingTarget(
            registry = registry,
            candidates = listOf(
                "grains",
                "cereals"
            )
        )?.let { targetCategory ->
            /*
             * Die historische Kategorie selbst ist bereits ein starker
             * fachlicher Hinweis auf ein trockenes Getreide-, Mehl-,
             * Hülsenfrucht- oder Backgrundprodukt.
             *
             * Unbekannt bleiben darf sie nicht. Für noch nicht durch eine
             * spezifische Regel erfasste Einträge verwenden wir daher den
             * breitesten bestehenden kanonischen Getreide-Key.
             */
            ProductRuleDecision(
                targetCategory = targetCategory,
                ruleId = "grain-product-generic-fallback",
                confidence = 0.85
            )
        }
    }

    private fun resolvePlantBasedAlternativeCategory(
        tokens: Set<String>,
        registry: CanonicalFoodCategoryRegistry
    ): ProductRuleDecision? =
        resolveDietOverlayCategory(
            tokens = tokens,
            registry = registry
        ) ?: resolveExistingTarget(
            registry = registry,
            candidates = listOf("plant-based-alternatives")
        )?.let { target ->
            ProductRuleDecision(
                targetCategory = target,
                ruleId = "legacy-plant-based-alternative",
                confidence = 0.90
            )
        }

    private fun firstMatchingRule(
        tokens: Set<String>,
        registry: CanonicalFoodCategoryRegistry,
        rules: List<ProductRule>
    ): ProductRuleDecision? {
        rules.forEach { rule ->
            val matches = rule.requiredAny.any { requiredTerm ->
                matchesRequiredTerm(
                    requiredTerm = requiredTerm,
                    productTokens = tokens
                )
            }

            if (!matches) {
                return@forEach
            }

            val target = resolveExistingTarget(
                registry = registry,
                candidates = rule.targetCandidates
            ) ?: return@forEach

            return ProductRuleDecision(
                targetCategory = target,
                ruleId = rule.ruleId,
                confidence = rule.confidence
            )
        }

        return null
    }

    private fun matchesRequiredTerm(
        requiredTerm: String,
        productTokens: Set<String>
    ): Boolean {
        if (requiredTerm.isBlank()) {
            return false
        }

        if (requiredTerm in productTokens) {
            return true
        }

        if (' ' in requiredTerm) {
            val compactRequiredTerm =
                requiredTerm.replace(" ", "")

            return compactRequiredTerm in productTokens
        }

        if (requiredTerm.length < MINIMUM_COMPOUND_MATCH_LENGTH) {
            return false
        }

        return productTokens.any { productToken ->
            productToken.length >= requiredTerm.length &&
                    productToken.contains(requiredTerm)
        }
    }

    private fun productTokens(
        item: CatalogFoodItem
    ): Set<String> =
        buildSet {
            addAll(tokenize(item.itemname))
            addAll(tokenize(item.normalized.orEmpty()))
            addAll(
                item.colloquial.flatMap(::tokenize)
            )
            addAll(
                item.autocompleteTokens.flatMap(::tokenize)
            )
        }

    private fun tokenize(
        value: String
    ): List<String> {
        val normalized = normalizeComparisonText(value)

        if (normalized.isBlank()) {
            return emptyList()
        }

        val splitTokens = normalized
            .split(MULTIPLE_WHITESPACE_REGEX)
            .filter(String::isNotBlank)

        return buildList {
            addAll(splitTokens)

            if (splitTokens.size > 1) {
                add(splitTokens.joinToString(" "))
                add(splitTokens.joinToString(""))
            }
        }
    }

    private fun normalizeComparisonText(
        value: String
    ): String =
        Normalizer.normalize(
            transliterateGermanCharacters(value),
            Normalizer.Form.NFKD
        )
            .lowercase(Locale.ROOT)
            .replace(COMBINING_MARKS_REGEX, "")
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun transliterateGermanCharacters(
        value: String
    ): String =
        value
            .replace("Ä", "Ae")
            .replace("Ö", "Oe")
            .replace("Ü", "Ue")
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ẞ", "SS")
            .replace("ß", "ss")

    private fun resolveExistingTarget(
        registry: CanonicalFoodCategoryRegistry,
        candidates: List<String>
    ): String? =
        candidates.firstOrNull(registry::contains)

    private fun unresolved(
        originalCategory: String?
    ): CatalogCategoryMigrationResult =
        CatalogCategoryMigrationResult(
            originalCategory = originalCategory,
            resultingCategory = null,
            status = CatalogCategoryMigrationStatus.UNRESOLVED,
            ruleId = null,
            confidence = 0.0
        )

    private fun productRule(
        id: String,
        requiredAny: Set<String>,
        targets: List<String>,
        confidence: Double
    ): ProductRule =
        ProductRule(
            ruleId = id,
            requiredAny = requiredAny
                .map(::normalizeComparisonText)
                .toSet(),
            targetCandidates = targets,
            confidence = confidence
        )

    private data class DirectMigrationRule(
        val ruleId: String,
        val targetCandidates: List<String>,
        val confidence: Double
    )

    private data class ProductRule(
        val ruleId: String,
        val requiredAny: Set<String>,
        val targetCandidates: List<String>,
        val confidence: Double
    )

    private data class ProductRuleDecision(
        val targetCategory: String,
        val ruleId: String,
        val confidence: Double
    )

    private companion object {

        val COMBINING_MARKS_REGEX =
            Regex("\\p{M}+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        const val MINIMUM_COMPOUND_MATCH_LENGTH = 5

        val DIRECT_MIGRATIONS: Map<String, DirectMigrationRule> =
            listOf(
                Triple("Backwaren", "bakery", "legacy-backwaren"),
                Triple("Getränke", "beverages", "legacy-getraenke"),
                Triple("Konserven", "canned-food", "legacy-konserven"),
                Triple("Süßwaren", "confectionery", "legacy-suesswaren"),
                Triple("Milchprodukte", "dairy", "legacy-milchprodukte"),
                Triple("Fisch", "fish", "legacy-fisch"),
                Triple("Obst", "fruit", "legacy-obst"),
                Triple("Fleisch", "meat", "legacy-fleisch"),
                Triple("Öle", "oils", "legacy-oele"),
                Triple("Nudeln", "pasta", "legacy-nudeln"),
                Triple("Fertiggerichte", "ready-meals", "legacy-fertiggerichte"),
                Triple("Reis", "rice", "legacy-reis"),
                Triple("Saucen", "sauces", "legacy-saucen"),
                Triple("Wurst", "sausage", "legacy-wurst"),
                Triple("Snacks", "snacks", "legacy-snacks"),
                Triple("Gewürze", "spices", "legacy-gewuerze"),
                Triple("Gemüse", "vegetables", "legacy-gemuese")
            )
                .associate { (source, target, ruleId) ->
                    normalizeStatic(source) to
                            DirectMigrationRule(
                                ruleId = ruleId,
                                targetCandidates = listOf(target),
                                confidence = 1.0
                            )
                }

        fun normalizeStatic(
            value: String
        ): String =
            Normalizer.normalize(
                value
                    .replace("Ä", "Ae")
                    .replace("Ö", "Oe")
                    .replace("Ü", "Ue")
                    .replace("ä", "ae")
                    .replace("ö", "oe")
                    .replace("ü", "ue")
                    .replace("ẞ", "SS")
                    .replace("ß", "ss"),
                Normalizer.Form.NFKD
            )
                .lowercase(Locale.ROOT)
                .replace(Regex("\\p{M}+"), "")
                .replace(Regex("[^a-z0-9]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
    }
}