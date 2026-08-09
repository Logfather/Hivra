package de.shopme.testing.system.tools.knowledge.catalog.review.reclassification

enum class CatalogMisclassifiedTypoRecommendation {

    /**
     * Quelle und Ziel müssen als getrennte Lebensmittel erhalten bleiben.
     */
    PRESERVE_AS_SEPARATE_FOODS,

    /**
     * Der Fall bleibt für eine spätere fachliche Review-Runde offen.
     */
    MANUAL_REVIEW_REQUIRED,

    /**
     * Die bisherige TYPO_VARIANT-Klassifikation soll im Review-Backlog
     * durch eine spezifischere Klasse ersetzt werden.
     */
    RECLASSIFY_REVIEW_ENTRY
}