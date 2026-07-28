package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic

enum class MissingOFFNutritionReferenceCandidateStage {

    /**
     * Ein Rohprodukt wurde anhand des Katalogbegriffs gefunden, aber durch
     * die produktive Identitätsprüfung nicht als passende Referenz erkannt.
     */
    IDENTITY_REJECTED,

    /**
     * Die Produktidentität war geeignet, die produktive Nutrition-Prüfung
     * hat das Produkt jedoch abgelehnt.
     */
    NUTRITION_REJECTED,

    /**
     * Identität und Nutrition waren grundsätzlich geeignet, das Produkt
     * erfüllte aber weitere Referenzanforderungen nicht.
     */
    REFERENCE_ELIGIBILITY_REJECTED,

    /**
     * Mindestens ein Produkt war vollständig geeignet, aber es wurde kein
     * Referenzkandidat erzeugt.
     */
    CANDIDATE_NOT_CREATED,

    /**
     * Ein Referenzkandidat wurde erzeugt, ist aber im persistierten
     * Candidate-Artefakt nicht vorhanden.
     */
    CANDIDATE_NOT_PERSISTED,

    /**
     * Ein persistierter Referenzkandidat existiert.
     *
     * Bei einem ursprünglich fehlenden Retrieval-Request deutet dies auf
     * eine unvollständige oder veraltete Source-Coverage-Diagnose hin.
     */
    NONE
}