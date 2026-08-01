package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy

enum class ResultingNutritionConflictPolicyAction {

    /**
     * Konflikt bleibt als diagnostische Evidenz erhalten.
     * Der Runtime-Eintrag wird nicht verändert.
     */
    RETAIN_AND_REPORT,

    /**
     * Der Datensatz darf nicht automatisch korrigiert werden.
     * Die Ursache kann später separat untersucht werden.
     */
    RETAIN_AND_REVIEW,

    /**
     * Nur für zukünftige Policy-Versionen vorgesehen.
     */
    REJECT_BUILD
}