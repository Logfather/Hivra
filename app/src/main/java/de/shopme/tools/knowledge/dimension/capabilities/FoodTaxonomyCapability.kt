package de.shopme.tools.knowledge.dimension.capabilities

import de.shopme.domain.food.FoodKnowledgeEntry
import de.shopme.tools.knowledge.dimension.AbstractKnowledgeDimensionCapability
import de.shopme.tools.knowledge.dimension.KnowledgeDimensionId
import de.shopme.tools.knowledge.dimension.KnowledgeDimensionInfo
import de.shopme.tools.knowledge.dimension.KnowledgeDimensionInterpretation
import de.shopme.tools.knowledge.dimension.KnowledgeDimensionResult
import de.shopme.tools.knowledge.dimension.KnowledgeIndicator
import de.shopme.tools.knowledge.dimension.KnowledgeSection
import de.shopme.tools.report.CoverageDimension

class FoodTaxonomyCapability : AbstractKnowledgeDimensionCapability() {

    override val id = KnowledgeDimensionId.FOOD_TAXONOMY

    override val section =
        KnowledgeSection.CONTENT

    override val order =
        50

    override val coverageDimension =
        CoverageDimension.TAXONOMY

    override fun info() =
        KnowledgeDimensionInfo(
            title = "Lebensmittelklassifikation",
            description = "Diese Dimension beschreibt, wie ein Lebensmittel fachlich eingeordnet wird.",
            storedFacts = listOf("Taxonomiepfad", "Übergeordnete Lebensmittelgruppe"),
            evaluation = "Der Knowledge Compiler ordnet Lebensmittel in eine strukturierte Lebensmittel-Taxonomie ein.",
            interpretations = listOf(
                KnowledgeDimensionInterpretation("🟩", "Klassifiziert", "Das Lebensmittel besitzt eine bekannte Taxonomie-Zuordnung."),
                KnowledgeDimensionInterpretation("⚪", "Unbekannt", "Für das Lebensmittel liegt keine Taxonomie-Zuordnung vor.")
            )
        )

    override fun result(
        knowledge: FoodKnowledgeEntry
    ): KnowledgeDimensionResult {

        val paths =
            knowledge.taxonomyPaths

        if (paths.isEmpty()) {
            return unknownResult()
        }

        val summary =
            paths.joinToString(
                separator = " · "
            ) { path ->
                path.joinToString(" → ")
            }

        return KnowledgeDimensionResult(
            indicator =
                KnowledgeIndicator.LIGHTGREEN,
            summary =
                summary,
            recommendation =
                "Diese Klassifikation beschreibt die fachliche Einordnung und ist keine Qualitätsbewertung."
        )
    }
}