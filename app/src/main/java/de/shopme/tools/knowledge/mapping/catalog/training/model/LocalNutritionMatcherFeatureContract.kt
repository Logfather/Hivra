package de.shopme.tools.knowledge.mapping.catalog.training.model

object LocalNutritionMatcherFeatureContract {

    val ACTIVE_DOMAIN_FEATURE_NAMES: List<String> =
        listOf(
            "domain_diet_or_substitute_difference_count",
            "domain_same_domain_different_entity_count",
            "domain_region_or_style_difference_count",
            "domain_non_semantic_token_difference_count",
        )

    /**
     * Deterministischer Basis-Featurevertrag.
     *
     * Die Reihenfolge muss exakt mit der Reihenfolge übereinstimmen,
     * in der LocalNutritionMatcherFeatureExtractor die Basiswerte
     * in den Featurevektor schreibt.
     */
    val BASE_FEATURE_NAMES: List<String> =
        listOf(
            "diagnostic_score",
            "reciprocal_candidate_rank",
            "reciprocal_candidate_count",
            "shared_token_count",
            "shared_token_ratio",
            "token_jaccard",
            "catalog_token_coverage",
            "server_token_coverage",
            "token_count_similarity",
            "character_length_similarity",
            "exact_normalized_match",
        )

    /**
     * Vollständiger deterministischer Domain-Mismatch-Featurevertrag.
     *
     * Diese Reihenfolge muss exakt mit der Reihenfolge übereinstimmen,
     * in der LocalNutritionMatcherFeatureExtractor die Domain-Werte
     * an den Basisvektor anhängt.
     *
     * Diese vollständige Menge bleibt für:
     *
     * - historische Modellvergleiche,
     * - Extraktor-Tests,
     * - Leakage-Tests,
     * - Kompatibilitätsprüfungen
     *
     * erhalten.
     */
    val ALL_DOMAIN_FEATURE_NAMES: List<String> =
        listOf(
            "domain_observation_count",
            "domain_diet_or_substitute_difference_count",
            "domain_cross_domain_mismatch_count",
            "domain_same_domain_different_entity_count",
            "domain_form_or_processing_difference_count",
            "domain_region_or_style_difference_count",
            "domain_compatible_relationship_count",
            "domain_unknown_token_involved_count",
            "domain_non_semantic_token_difference_count",
            "domain_unknown_mismatch_count",
            "domain_identity_conflict_count",
            "domain_modifier_difference_count",
            "domain_known_semantic_observation_count",
            "domain_unknown_semantic_observation_count",
        )

    /**
     * Aggregierte Beobachtungszähler.
     *
     * Diese Features bleiben vollständig extrahierbar, gehören aber
     * nicht zur produktiven Optimierungsbasis.
     */
    val AGGREGATE_DOMAIN_FEATURE_NAMES: List<String> =
        listOf(
            "domain_observation_count",
            "domain_known_semantic_observation_count",
            "domain_unknown_semantic_observation_count",
        )

    /**
     * Domain-Features, die bereits vor der aktuellen
     * Leave-one-out-Optimierung aus dem produktiven Featurevertrag
     * ausgeschlossen waren.
     */
    val PREVIOUSLY_EXCLUDED_DOMAIN_FEATURE_NAMES: List<String> =
        listOf(
            "domain_cross_domain_mismatch_count",
            "domain_compatible_relationship_count",
            "domain_unknown_token_involved_count",
            "domain_identity_conflict_count",
        )

    /**
     * Die sieben Domain-Features, gegen die die aktuelle
     * deterministische Leave-one-out-Optimierung ausgeführt wurde.
     */
    val OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES: List<String> =
        ALL_DOMAIN_FEATURE_NAMES
            .filterNot { featureName ->
                featureName in AGGREGATE_DOMAIN_FEATURE_NAMES ||
                        featureName in
                        PREVIOUSLY_EXCLUDED_DOMAIN_FEATURE_NAMES
            }

    /**
     * In der aktuellen deterministischen Auswertung als schädlich
     * klassifizierte Domain-Features.
     */
    val HARMFUL_DOMAIN_FEATURE_NAMES: List<String> =
        listOf(
            "domain_form_or_processing_difference_count",
            "domain_modifier_difference_count",
            "domain_unknown_mismatch_count",
        )

    val OPTIMIZED_DOMAIN_FEATURE_NAMES: List<String> =
        OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
            .filterNot { featureName ->
                featureName in
                        HARMFUL_DOMAIN_FEATURE_NAMES
            }

    val OPTIMIZED_FEATURE_NAMES: List<String> =
        BASE_FEATURE_NAMES +
                OPTIMIZED_DOMAIN_FEATURE_NAMES

    /**
     * Vollständiger historischer Featurevertrag:
     *
     * 11 Basis-Features + 14 Domain-Features = 25 Features.
     */
    val ALL_FEATURE_NAMES: List<String> =
        BASE_FEATURE_NAMES +
                ALL_DOMAIN_FEATURE_NAMES

    /**
     * Aktiver produktiver Featurevertrag:
     *
     * 11 Basis-Features + 4 aktive Domain-Features = 15 Features.
     */
    val ACTIVE_FEATURE_NAMES: List<String> =
        OPTIMIZED_FEATURE_NAMES

    /**
     * Domain-Features, die in zukünftigen Optimierungsläufen noch
     * einzeln untersucht beziehungsweise abgetragen werden dürfen.
     */
    val OPTIMIZABLE_FEATURE_NAMES: List<String> =
        ACTIVE_DOMAIN_FEATURE_NAMES

    val BASE_FEATURE_COUNT: Int
        get() =
            BASE_FEATURE_NAMES.size

    val ALL_DOMAIN_FEATURE_COUNT: Int
        get() =
            ALL_DOMAIN_FEATURE_NAMES.size

    val OPTIMIZATION_BASELINE_DOMAIN_FEATURE_COUNT: Int
        get() =
            OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES.size

    val HARMFUL_DOMAIN_FEATURE_COUNT: Int
        get() =
            HARMFUL_DOMAIN_FEATURE_NAMES.size

    val ACTIVE_DOMAIN_FEATURE_COUNT: Int
        get() =
            ACTIVE_DOMAIN_FEATURE_NAMES.size

    val ALL_FEATURE_COUNT: Int
        get() =
            ALL_FEATURE_NAMES.size

    val ACTIVE_FEATURE_COUNT: Int
        get() =
            ACTIVE_FEATURE_NAMES.size

    /**
     * Validiert ein für Training oder Modellvergleich verwendetes
     * produktiv zulässiges Feature-Subset.
     *
     * Erlaubt sind:
     *
     * - der vollständige Basis-Featurepräfix,
     * - gefolgt von null bis vier aktiven Domain-Features,
     * - in kanonischer Reihenfolge.
     *
     * Historische 25-Feature-Vergleiche sind bewusst nicht über diese
     * Methode zulässig.
     */
    fun validateTrainingFeatureSubset(
        featureNames: List<String>,
    ) {
        require(featureNames.isNotEmpty()) {
            "Local nutrition matcher feature subset must not be empty."
        }

        require(
            featureNames.distinct().size ==
                    featureNames.size,
        ) {
            "Local nutrition matcher feature subset must be unique: " +
                    featureNames.joinToString()
        }

        require(
            featureNames.size >=
                    BASE_FEATURE_COUNT,
        ) {
            "Local nutrition matcher feature subset must contain the " +
                    "complete base-feature prefix. Expected at least " +
                    "$BASE_FEATURE_COUNT features, but contains " +
                    "${featureNames.size}."
        }

        val actualBaseFeatures =
            featureNames.take(
                BASE_FEATURE_COUNT,
            )

        require(
            actualBaseFeatures ==
                    BASE_FEATURE_NAMES,
        ) {
            "Local nutrition matcher feature subset must preserve " +
                    "the complete base-feature prefix. Expected: " +
                    BASE_FEATURE_NAMES.joinToString() +
                    "; actual: " +
                    actualBaseFeatures.joinToString()
        }

        val actualDomainFeatures =
            featureNames.drop(
                BASE_FEATURE_COUNT,
            )

        val unsupportedDomainFeatures =
            actualDomainFeatures
                .filterNot { featureName ->
                    featureName in
                            ACTIVE_DOMAIN_FEATURE_NAMES
                }

        require(
            unsupportedDomainFeatures.isEmpty(),
        ) {
            "Unsupported active Domain-Mismatch features: " +
                    unsupportedDomainFeatures.joinToString()
        }

        val expectedDomainFeatureOrder =
            ACTIVE_DOMAIN_FEATURE_NAMES
                .filter { featureName ->
                    featureName in actualDomainFeatures
                }

        require(
            actualDomainFeatures ==
                    expectedDomainFeatureOrder,
        ) {
            "Local nutrition matcher Domain-Mismatch features must " +
                    "preserve canonical order. Expected: " +
                    expectedDomainFeatureOrder.joinToString() +
                    "; actual: " +
                    actualDomainFeatures.joinToString()
        }
    }

    /**
     * Validiert ein Feature-Subset für deterministische
     * Leave-one-feature-out-Optimierungen.
     *
     * Erlaubt sind:
     *
     * - der vollständige Basis-Featurepräfix,
     * - gefolgt von null bis sieben Features aus der unabhängigen
     *   Optimierungsbaseline,
     * - in kanonischer Reihenfolge.
     *
     * Diese Methode ist bewusst breiter als
     * validateTrainingFeatureSubset(...), weil für Ablationen auch bereits
     * als schädlich klassifizierte Features erneut trainierbar sein müssen.
     */
    fun validateOptimizationFeatureSubset(
        featureNames: List<String>,
    ) {
        require(featureNames.isNotEmpty()) {
            "Local nutrition matcher optimization feature subset must " +
                    "not be empty."
        }

        require(
            featureNames.distinct().size ==
                    featureNames.size,
        ) {
            "Local nutrition matcher optimization feature subset must " +
                    "be unique: " +
                    featureNames.joinToString()
        }

        require(
            featureNames.size >=
                    BASE_FEATURE_COUNT,
        ) {
            "Local nutrition matcher optimization feature subset must " +
                    "contain the complete base-feature prefix. Expected " +
                    "at least $BASE_FEATURE_COUNT features, but contains " +
                    "${featureNames.size}."
        }

        val actualBaseFeatures =
            featureNames.take(
                BASE_FEATURE_COUNT,
            )

        require(
            actualBaseFeatures ==
                    BASE_FEATURE_NAMES,
        ) {
            "Local nutrition matcher optimization feature subset must " +
                    "preserve the complete base-feature prefix. Expected: " +
                    BASE_FEATURE_NAMES.joinToString() +
                    "; actual: " +
                    actualBaseFeatures.joinToString()
        }

        val actualDomainFeatures =
            featureNames.drop(
                BASE_FEATURE_COUNT,
            )

        val unsupportedDomainFeatures =
            actualDomainFeatures
                .filterNot { featureName ->
                    featureName in
                            OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
                }

        require(
            unsupportedDomainFeatures.isEmpty(),
        ) {
            "Unsupported optimization-baseline Domain-Mismatch features: " +
                    unsupportedDomainFeatures.joinToString()
        }

        val expectedDomainFeatureOrder =
            OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
                .filter { featureName ->
                    featureName in
                            actualDomainFeatures
                }

        require(
            actualDomainFeatures ==
                    expectedDomainFeatureOrder,
        ) {
            "Local nutrition matcher optimization Domain-Mismatch " +
                    "features must preserve canonical order. Expected: " +
                    expectedDomainFeatureOrder.joinToString() +
                    "; actual: " +
                    actualDomainFeatures.joinToString()
        }
    }

    /**
     * Validiert den exakten produktiven Featurevertrag.
     */
    fun validateProductionFeatureContract(
        featureNames: List<String>,
    ) {
        require(
            featureNames ==
                    ACTIVE_FEATURE_NAMES,
        ) {
            "Local nutrition matcher does not use the active " +
                    "production feature contract. Expected: " +
                    ACTIVE_FEATURE_NAMES.joinToString() +
                    "; actual: " +
                    featureNames.joinToString()
        }
    }

    /**
     * Validiert den vollständigen historischen 25-Featurevertrag.
     *
     * Diese Methode ist ausschließlich für Extraktor-, Leakage- und
     * historische Vergleichstests vorgesehen.
     */
    fun validateCompleteFeatureContract(
        featureNames: List<String>,
    ) {
        require(
            featureNames ==
                    ALL_FEATURE_NAMES,
        ) {
            "Local nutrition matcher does not use the complete " +
                    "historical feature contract. Expected: " +
                    ALL_FEATURE_NAMES.joinToString() +
                    "; actual: " +
                    featureNames.joinToString()
        }
    }

    init {
        require(
            BASE_FEATURE_NAMES.isNotEmpty(),
        ) {
            "Nutrition base-feature contract must not be empty."
        }

        require(
            BASE_FEATURE_NAMES.distinct().size ==
                    BASE_FEATURE_NAMES.size,
        ) {
            "Nutrition base-feature names must be unique."
        }

        require(
            ALL_DOMAIN_FEATURE_NAMES.isNotEmpty(),
        ) {
            "Complete nutrition domain-feature contract must not be empty."
        }

        require(
            ALL_DOMAIN_FEATURE_NAMES.distinct().size ==
                    ALL_DOMAIN_FEATURE_NAMES.size,
        ) {
            "Complete nutrition domain-feature names must be unique."
        }

        require(
            BASE_FEATURE_NAMES.none { featureName ->
                featureName in ALL_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Nutrition base and domain feature contracts must be disjoint."
        }

        require(
            AGGREGATE_DOMAIN_FEATURE_NAMES.distinct().size ==
                    AGGREGATE_DOMAIN_FEATURE_NAMES.size,
        ) {
            "Aggregate nutrition domain feature names must be unique."
        }

        require(
            AGGREGATE_DOMAIN_FEATURE_NAMES.all { featureName ->
                featureName in ALL_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Every aggregate nutrition domain feature must belong to " +
                    "the complete domain feature contract."
        }

        require(
            PREVIOUSLY_EXCLUDED_DOMAIN_FEATURE_NAMES.distinct().size ==
                    PREVIOUSLY_EXCLUDED_DOMAIN_FEATURE_NAMES.size,
        ) {
            "Previously excluded nutrition domain feature names " +
                    "must be unique."
        }

        require(
            PREVIOUSLY_EXCLUDED_DOMAIN_FEATURE_NAMES.all { featureName ->
                featureName in ALL_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Every previously excluded nutrition domain feature must " +
                    "belong to the complete domain feature contract."
        }

        require(
            AGGREGATE_DOMAIN_FEATURE_NAMES.none { featureName ->
                featureName in
                        PREVIOUSLY_EXCLUDED_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Aggregate and previously excluded nutrition domain " +
                    "feature groups must be disjoint."
        }

        require(
            OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES.distinct().size ==
                    OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES.size,
        ) {
            "Nutrition optimization-baseline domain features must " +
                    "be unique."
        }

        require(
            OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES.all { featureName ->
                featureName in ALL_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Every nutrition optimization-baseline feature must " +
                    "belong to the complete domain feature contract."
        }

        require(
            OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES.none { featureName ->
                featureName in AGGREGATE_DOMAIN_FEATURE_NAMES ||
                        featureName in
                        PREVIOUSLY_EXCLUDED_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Nutrition optimization-baseline features must not contain " +
                    "aggregate or previously excluded domain features."
        }

        require(
            OPTIMIZATION_BASELINE_DOMAIN_FEATURE_COUNT == 7,
        ) {
            "Nutrition optimization baseline must contain seven domain " +
                    "features, but contains " +
                    "$OPTIMIZATION_BASELINE_DOMAIN_FEATURE_COUNT."
        }

        require(
            HARMFUL_DOMAIN_FEATURE_NAMES.distinct().size ==
                    HARMFUL_DOMAIN_FEATURE_NAMES.size,
        ) {
            "Harmful nutrition domain feature names must be unique."
        }

        require(
            HARMFUL_DOMAIN_FEATURE_NAMES.all { featureName ->
                featureName in
                        OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Every harmful nutrition domain feature must belong to " +
                    "the optimization-baseline domain feature contract."
        }

        require(
            ACTIVE_DOMAIN_FEATURE_NAMES.distinct().size ==
                    ACTIVE_DOMAIN_FEATURE_NAMES.size,
        ) {
            "Active nutrition domain feature names must be unique."
        }

        require(
            ACTIVE_DOMAIN_FEATURE_NAMES.all { featureName ->
                featureName in
                        OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Every active nutrition domain feature must belong to the " +
                    "optimization-baseline domain feature contract."
        }

        require(
            ACTIVE_DOMAIN_FEATURE_NAMES.none { featureName ->
                featureName in HARMFUL_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Active nutrition domain features must not contain " +
                    "harmful features."
        }

        require(
            ACTIVE_DOMAIN_FEATURE_NAMES ==
                    OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
                        .filterNot { featureName ->
                            featureName in
                                    HARMFUL_DOMAIN_FEATURE_NAMES
                        },
        ) {
            "Active nutrition domain features must equal the " +
                    "optimization baseline without harmful features."
        }

        require(
            ALL_FEATURE_NAMES ==
                    BASE_FEATURE_NAMES +
                    ALL_DOMAIN_FEATURE_NAMES,
        ) {
            "Complete nutrition feature contract must consist of the " +
                    "base features followed by all domain features."
        }

        require(
            ACTIVE_FEATURE_NAMES ==
                    BASE_FEATURE_NAMES +
                    ACTIVE_DOMAIN_FEATURE_NAMES,
        ) {
            "Active nutrition feature contract must consist of the " +
                    "base features followed by the active domain features."
        }

        require(
            ALL_FEATURE_NAMES.distinct().size ==
                    ALL_FEATURE_NAMES.size,
        ) {
            "Complete nutrition feature names must be unique."
        }

        require(
            ACTIVE_FEATURE_NAMES.distinct().size ==
                    ACTIVE_FEATURE_NAMES.size,
        ) {
            "Active nutrition feature names must be unique."
        }

        require(
            ALL_FEATURE_NAMES.take(BASE_FEATURE_COUNT) ==
                    BASE_FEATURE_NAMES,
        ) {
            "Complete nutrition feature contract must preserve the " +
                    "complete base-feature prefix."
        }

        require(
            ACTIVE_FEATURE_NAMES.take(BASE_FEATURE_COUNT) ==
                    BASE_FEATURE_NAMES,
        ) {
            "Active nutrition feature contract must preserve the " +
                    "complete base-feature prefix."
        }

        require(
            OPTIMIZABLE_FEATURE_NAMES ==
                    ACTIVE_DOMAIN_FEATURE_NAMES,
        ) {
            "Optimizable nutrition features must equal the active " +
                    "Domain-Mismatch feature contract."
        }

        require(
            BASE_FEATURE_COUNT == 11,
        ) {
            "Nutrition base-feature contract must contain 11 features, " +
                    "but contains $BASE_FEATURE_COUNT."
        }

        require(
            ALL_DOMAIN_FEATURE_COUNT == 14,
        ) {
            "Complete nutrition domain-feature contract must contain " +
                    "14 features, but contains $ALL_DOMAIN_FEATURE_COUNT."
        }

        require(
            HARMFUL_DOMAIN_FEATURE_COUNT == 3,
        ) {
            "Harmful nutrition domain-feature contract must contain " +
                    "three features, but contains " +
                    "$HARMFUL_DOMAIN_FEATURE_COUNT."
        }

        require(
            ACTIVE_DOMAIN_FEATURE_COUNT == 4,
        ) {
            "Optimized nutrition feature contract must contain four " +
                    "active domain features, but contains " +
                    "$ACTIVE_DOMAIN_FEATURE_COUNT."
        }

        require(
            ALL_FEATURE_COUNT == 25,
        ) {
            "Complete historical nutrition feature contract must " +
                    "contain 25 features, but contains $ALL_FEATURE_COUNT."
        }

        require(
            ACTIVE_FEATURE_COUNT == 15,
        ) {
            "Optimized nutrition production feature contract must " +
                    "contain 15 features, but contains " +
                    "$ACTIVE_FEATURE_COUNT."
        }

        require(
            HARMFUL_DOMAIN_FEATURE_NAMES.distinct().size ==
                    HARMFUL_DOMAIN_FEATURE_NAMES.size,
        ) {
            "Harmful nutrition domain-feature names must be unique."
        }

        require(
            HARMFUL_DOMAIN_FEATURE_NAMES.all { featureName ->
                featureName in
                        OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Every harmful nutrition domain feature must belong to the " +
                    "optimization baseline."
        }

        require(
            OPTIMIZED_DOMAIN_FEATURE_NAMES.size ==
                    OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES.size -
                    HARMFUL_DOMAIN_FEATURE_NAMES.size,
        ) {
            "Unexpected optimized nutrition domain-feature count."
        }

        require(
            OPTIMIZED_FEATURE_NAMES.take(
                BASE_FEATURE_COUNT,
            ) ==
                    BASE_FEATURE_NAMES,
        ) {
            "Optimized nutrition feature contract must preserve the complete " +
                    "base-feature prefix."
        }

        require(
            OPTIMIZED_FEATURE_NAMES.size ==
                    15,
        ) {
            "Unexpected optimized nutrition feature count: " +
                    OPTIMIZED_FEATURE_NAMES.size
        }

        require(
            ACTIVE_FEATURE_NAMES ==
                    OPTIMIZED_FEATURE_NAMES,
        ) {
            "Active nutrition matcher feature contract must use the " +
                    "deterministically optimized feature set."
        }

        require(
            ACTIVE_FEATURE_NAMES.size ==
                    15,
        ) {
            "Unexpected active nutrition matcher feature count: " +
                    ACTIVE_FEATURE_NAMES.size
        }
    }
}