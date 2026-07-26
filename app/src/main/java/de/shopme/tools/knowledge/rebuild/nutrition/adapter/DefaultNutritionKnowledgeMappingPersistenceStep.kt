package de.shopme.tools.knowledge.rebuild.nutrition.adapter

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeMappingPersistenceStep
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildPersistenceResult
import java.io.File

class DefaultNutritionKnowledgeMappingPersistenceStep(
    private val outputMappingFile: File,
    private val representativeValidationFile: File,
    private val persistMappings: () -> Unit,
    private val validateProductiveLowConfidenceMatches:
    (() -> Unit)? =
        null,
    private val representativeMappingMerger:
    RepresentativeNutritionMappingMerger =
        RepresentativeNutritionMappingMerger()
) : NutritionKnowledgeMappingPersistenceStep {

    override fun run():
            NutritionKnowledgeRebuildPersistenceResult {

        val beforeMappings =
            readMappingIdentities(
                file =
                    outputMappingFile
            )

        /*
         * Schreibt die aktuell akzeptierten regulären Decisions.
         *
         * Dieser Schritt darf die vorhandene Datei vollständig
         * ersetzen. Dadurch dürfen auch veraltete Mappings aus
         * früheren Request- oder Decision-Batches entfallen.
         *
         * Unmittelbar danach werden die aktuell validierten
         * Representative-Mappings wieder deterministisch ergänzt.
         */
        persistMappings()

        require(
            outputMappingFile.isFile
        ) {
            "Catalog-server mapping file was not created: " +
                    outputMappingFile.absolutePath
        }

        val regularMappings =
            readMappingIdentities(
                file =
                    outputMappingFile
            )

        /*
         * Produktive MATCH-Decisions unterhalb der regulären
         * Persistenzschwelle werden deterministisch als mögliche
         * Representative-Mappings nachvalidiert.
         *
         * Im OFFLINE-Modus ist diese Funktion nicht konfiguriert.
         */
        validateProductiveLowConfidenceMatches
            ?.invoke()

        val representativeResult =
            representativeMappingMerger.merge(
                representativeValidationFile =
                    representativeValidationFile,
                mappingFile =
                    outputMappingFile
            )

        val finalMappings =
            readMappingIdentities(
                file =
                    outputMappingFile
            )

        require(
            representativeResult.existingMappingCount ==
                    regularMappings.size
        ) {
            "Representative merger read a different regular " +
                    "mapping count: regular=${regularMappings.size}, " +
                    "mergeExisting=" +
                    "${representativeResult.existingMappingCount}."
        }

        require(
            finalMappings.size ==
                    representativeResult.finalMappingCount
        ) {
            "Persisted mapping count differs from representative " +
                    "merge result: persisted=${finalMappings.size}, " +
                    "merge=${representativeResult.finalMappingCount}."
        }

        /*
         * Das Result-Modell unterscheidet:
         *
         * - bestehende Mappings, die nach dem Rebuild weiterhin
         *   vorhanden sind,
         * - neu hinzugekommene Mappings,
         * - den finalen Mapping-Bestand.
         *
         * Veraltete und entfernte Mappings zählen ausdrücklich
         * nicht als weiterhin bestehende Mappings.
         */
        val unchangedMappings =
            beforeMappings
                .intersect(
                    finalMappings
                )

        val addedMappings =
            finalMappings
                .minus(
                    beforeMappings
                )

        val removedMappings =
            beforeMappings
                .minus(
                    finalMappings
                )

        require(
            unchangedMappings.size +
                    addedMappings.size ==
                    finalMappings.size
        ) {
            "Final nutrition mappings are not completely covered " +
                    "by unchanged and added mappings: " +
                    "unchanged=${unchangedMappings.size}, " +
                    "added=${addedMappings.size}, " +
                    "removed=${removedMappings.size}, " +
                    "final=${finalMappings.size}."
        }

        return NutritionKnowledgeRebuildPersistenceResult(
            existingMappingCount =
                beforeMappings.size,
            addedMappingCount =
                addedMappings.size,
            removedMappingCount =
                removedMappings.size,
            unchangedMappingCount =
                unchangedMappings.size,
            conflictCount =
                0,
            finalMappingCount =
                finalMappings.size
        )
    }

    private fun readMappingIdentities(
        file: File
    ): Set<String> {

        if (!file.isFile) {
            return emptySet()
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(
            root.isJsonObject
        ) {
            "Catalog-server mapping file must contain a JSON " +
                    "object: " +
                    file.absolutePath
        }

        val mappings =
            root.asJsonObject["mappings"]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: error(
                    "Catalog-server mapping file contains no " +
                            "'mappings' array: " +
                            file.absolutePath
                )

        val identities =
            mappings
                .map {
                    createMappingIdentity(
                        mapping =
                            it,
                        file =
                            file
                    )
                }

        require(
            identities.size ==
                    identities.toSet().size
        ) {
            "Catalog-server mapping file contains duplicate " +
                    "mappings: " +
                    file.absolutePath
        }

        return identities.toSet()
    }

    private fun createMappingIdentity(
        mapping: JsonElement,
        file: File
    ): String {

        require(
            mapping.isJsonObject
        ) {
            "Catalog-server mapping entry must be a JSON object: " +
                    file.absolutePath
        }

        /*
         * Die vollständige kanonische JSON-Repräsentation wird als
         * Identität verwendet.
         *
         * Eine Änderung des Ziel-Keys oder eines anderen
         * persistierten Mapping-Feldes wird dadurch korrekt als
         * Entfernung des alten und Hinzufügung des neuen Mappings
         * klassifiziert.
         *
         * Voraussetzung ist, dass der Mapping-Writer die Properties
         * deterministisch serialisiert. Das ist für die generierten
         * Knowledge-Artefakte ohnehin erforderlich.
         */
        return mapping.toString()
    }
}