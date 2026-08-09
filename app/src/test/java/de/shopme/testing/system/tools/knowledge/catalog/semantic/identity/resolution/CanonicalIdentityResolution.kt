package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateReason
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentitySourceLayer

enum class CanonicalIdentityResolutionDecision {
    MERGE,
    REVIEW
}

enum class CanonicalIdentityResolutionReason {

    /**
     * Eine regenerierte Expansion erzeugt dieselbe Produktidentität
     * wie ein bereits vorhandener Baseline-Eintrag.
     */
    BASELINE_PREFERRED_OVER_EXPANSION,

    /**
     * Singular ist die kanonische Produktidentität.
     */
    SINGULAR_PREFERRED_OVER_PLURAL,

    /**
     * Deutsche Zusammenschreibung ist gegenüber Getrennt- oder
     * Bindestrichschreibung kanonisch.
     *
     * Beispiele:
     * Apfelschorle > Apfel Schorle
     * Balsamicoessig > Balsamico Essig
     * Cashewdrink > Cashew-Drink
     */
    GERMAN_COMPOUND_PREFERRED,

    /**
     * Standardorthographie wird gegenüber fehlerhafter oder
     * künstlicher Schreibweise bevorzugt.
     */
    STANDARD_ORTHOGRAPHY_PREFERRED,

    /**
     * Im Deutschen steht ein beschreibender Modifier vor
     * der eigentlichen Produktidentität.
     *
     * Beispiele:
     * Grüner Tee > Tee Grüner
     * Bio Eier > Eier Bio
     */
    NATURAL_GERMAN_WORD_ORDER_PREFERRED,

    /**
     * Produktidentität steht vor Lager-/Vertriebszustand.
     *
     * Beispiele:
     * Beerenmix TK > TK-Beerenmix
     * Pizza Margherita (TK) > TK-Pizza Margherita
     */
    PRODUCT_BEFORE_STATE_PREFERRED,

    /**
     * Konkrete Speise steht vor einer generischen
     * Produkt-/Gerichtsklasse.
     *
     * Beispiele:
     * Gemüsecurry Fertiggericht > Fertiggericht Gemüsecurry
     */
    SPECIFIC_DISH_BEFORE_GENERIC_CLASS_PREFERRED,

    /**
     * Keine hinreichend sichere deterministische Auflösung.
     */
    NO_DETERMINISTIC_RESOLUTION
}

data class CanonicalIdentityResolutionCandidate(
    val itemName: String,
    val normalized: String,
    val category: String,
    val sourceLayer: CanonicalIdentitySourceLayer
)

data class CanonicalIdentityResolution(
    val duplicateReason: CanonicalIdentityDuplicateReason,
    val fingerprint: String,

    val decision: CanonicalIdentityResolutionDecision,
    val reason: CanonicalIdentityResolutionReason,

    val winner: CanonicalIdentityResolutionCandidate?,
    val merged: List<CanonicalIdentityResolutionCandidate>
)

data class CanonicalIdentityResolutionReport(
    val schemaVersion: Int,

    val inputEntryCount: Int,
    val conflictGroupCount: Int,

    val resolvedGroupCount: Int,
    val reviewGroupCount: Int,

    val removedEntryCount: Int,
    val resolvedEntryCount: Int,

    val countsByReason: Map<String, Int>,

    val resolutions: List<CanonicalIdentityResolution>
)