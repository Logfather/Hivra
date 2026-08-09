package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair.CanonicalCatalogLinguisticMetadataRepairer
import de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair.CanonicalCatalogLinguisticRepairValidator
import de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair.CanonicalCatalogLinguisticRepairWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepairCanonicalCatalogLinguisticMetadataTest {

    @Test
    fun repairCanonicalCatalogLinguisticMetadata() {

        val projectRoot =
            resolveProjectRoot()

        val inputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-family-identity-resolved.json"
            )

        require(inputFile.isFile) {
            "Identity-resolved canonical catalog not found: " +
                    inputFile.absolutePath
        }

        val source =
            JsonParser
                .parseString(
                    inputFile.readText()
                )
                .asJsonArray
                .mapNotNull { element ->
                    element
                        .takeIf {
                            it.isJsonObject
                        }
                        ?.asJsonObject
                }

        assertEquals(
            expected = 4596,
            actual = source.size,
            message =
                "Resolved canonical identity arithmetic changed."
        )

        val repairer =
            CanonicalCatalogLinguisticMetadataRepairer()

        val first =
            repairer
                .repair(source)

        /*
         * Vollständiger zweiter Lauf gegen dieselbe Eingabe.
         * Ergebnis muss byte-semantisch identisch sein.
         */
        val second =
            repairer
                .repair(source)

        val deterministic =
            first.first == second.first &&
                    first.second == second.second

        val report =
            CanonicalCatalogLinguisticRepairValidator()
                .validate(
                    catalog =
                        first.first,
                    stats =
                        first.second,
                    deterministic =
                        deterministic
                )

        assertEquals(
            expected = 4596,
            actual =
                report.outputEntryCount
        )

        assertEquals(
            expected = 4596,
            actual =
                report.uniqueNormalizedKeyCount
        )

        assertEquals(
            expected = 4596,
            actual =
                report.uniqueItemNameCount
        )

        assertEquals(
            expected = 0,
            actual =
                report.blankPluralCount
        )

        assertEquals(
            expected = 0,
            actual =
                report.variantLeakedIntoColloquialCount
        )

        assertEquals(
            expected = 0,
            actual =
                report.duplicateColloquialValueCount
        )

        assertEquals(
            expected = 0,
            actual =
                report.emptyAutocompleteEntryCount
        )

        assertTrue(
            report.deterministic
        )

        assertTrue(
            report.valid
        )

        val outputDirectory =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration"
            )

        val catalogFile =
            File(
                outputDirectory,
                "canonical-food-catalog-linguistic-repaired.json"
            )

        val reportFile =
            File(
                outputDirectory,
                "canonical-food-catalog-linguistic-repair-report.json"
            )

        CanonicalCatalogLinguisticRepairWriter()
            .writeCatalog(
                catalog =
                    first.first,
                outputFile =
                    catalogFile
            )

        CanonicalCatalogLinguisticRepairWriter()
            .writeReport(
                report =
                    report,
                outputFile =
                    reportFile
            )

        println()
        println("Canonical Catalog Linguistic Repair")
        println("-----------------------------------")
        println(
            "Input entries                   : " +
                    report.inputEntryCount
        )
        println(
            "Output entries                  : " +
                    report.outputEntryCount
        )

        println()
        println("Changes:")
        println(
            "  Plural changed                : " +
                    report.stats.pluralChangedCount
        )
        println(
            "  Colloquial changed            : " +
                    report.stats.colloquialChangedCount
        )
        println(
            "  Phonetic tokens changed       : " +
                    report.stats.phoneticTokensChangedCount
        )
        println(
            "  Autocomplete tokens changed   : " +
                    report.stats.autocompleteTokensChangedCount
        )

        println()
        println("Colloquial:")
        println(
            "  Removed variant aliases       : " +
                    report.stats.removedVariantColloquialCount
        )
        println(
            "  Preserved aliases             : " +
                    report.stats.preservedColloquialAliasCount
        )

        println()
        println("Plural:")
        println(
            "  Family plural resolved        : " +
                    report.stats.familyPluralResolvedCount
        )
        println(
            "  Family plural fallback        : " +
                    report.stats.familyPluralFallbackCount
        )

        println()
        println("Validation:")
        println(
            "  Unique normalized keys        : " +
                    report.uniqueNormalizedKeyCount
        )
        println(
            "  Unique canonical names        : " +
                    report.uniqueItemNameCount
        )
        println(
            "  Blank plurals                 : " +
                    report.blankPluralCount
        )
        println(
            "  Variant colloquial leaks      : " +
                    report.variantLeakedIntoColloquialCount
        )
        println(
            "  Duplicate colloquial values   : " +
                    report.duplicateColloquialValueCount
        )
        println(
            "  Empty autocomplete entries    : " +
                    report.emptyAutocompleteEntryCount
        )
        println(
            "  Deterministic                 : " +
                    report.deterministic
        )
        println(
            "  Valid                         : " +
                    report.valid
        )

        println()
        println(
            "Catalog: data/generated/knowledge/catalog/" +
                    "regeneration/" +
                    "canonical-food-catalog-linguistic-repaired.json"
        )

        println(
            "Report : data/generated/knowledge/catalog/" +
                    "regeneration/" +
                    "canonical-food-catalog-linguistic-repair-report.json"
        )
    }

    private fun resolveProjectRoot(): File {

        val current =
            File(".")
                .canonicalFile

        return listOfNotNull(
            current,
            current.parentFile
        )
            .firstOrNull { candidate ->
                File(
                    candidate,
                    "data/generated/knowledge/catalog/regeneration"
                ).isDirectory
            }
            ?: error(
                "Could not resolve ShopMe project root."
            )
    }
}