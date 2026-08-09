package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateAnalyzer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidateFamilyRepairedCanonicalIdentityTest {

    @Test
    fun validateFamilyRepairedCanonicalIdentities() {

        val projectRoot =
            resolveProjectRoot()

        val inputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-family-identity-resolved.json"
            )

        require(inputFile.isFile) {
            "Family repaired catalog not found: " +
                    inputFile.absolutePath
        }

        val entries =
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
            actual = entries.size,
            message =
                "Family-repaired canonical catalog arithmetic " +
                        "changed unexpectedly."
        )

        val report =
            CanonicalIdentityDuplicateAnalyzer()
                .analyze(
                    baseline = entries,
                    regeneratedExpansion = emptyList()
                )

        println()
        println("Family Repaired Canonical Identity Validation")
        println("---------------------------------------------")
        println(
            "Catalog entries : " +
                    entries.size
        )

        println()
        println("Duplicate groups")
        println(
            "  ERROR  : " +
                    report.errorGroupCount
        )
        println(
            "  REVIEW : " +
                    report.reviewGroupCount
        )
        println(
            "  TOTAL  : " +
                    report.duplicateGroupCount
        )

        if (
            report.groups.isNotEmpty()
        ) {

            println()
            println("Remaining groups:")

            report
                .groups
                .forEach { group ->

                    println(
                        "  ${group.severity.name} " +
                                "${group.reason.name} " +
                                "[${group.fingerprint}]"
                    )

                    group
                        .entries
                        .forEach { entry ->

                            println(
                                "      " +
                                        "${entry.normalized.padEnd(36)} " +
                                        entry.itemName
                            )
                        }
                }
        }

        /*
         * Assertions bewusst erst NACH der Diagnoseausgabe.
         *
         * Dadurch sehen wir bei neuen kanonischen
         * Identity-Transformationen die tatsächlich entstandenen
         * Konflikte, bevor der Test terminiert.
         */
        assertEquals(
            expected = 0,
            actual = report.errorGroupCount,
            message =
                "Family identity repair introduced hard canonical " +
                        "identity conflicts."
        )

        assertEquals(
            expected = 0,
            actual = report.reviewGroupCount,
            message =
                "Family identity repair introduced unresolved " +
                        "canonical identity candidates."
        )

        assertEquals(
            expected = 0,
            actual = report.duplicateGroupCount,
            message =
                "Family-repaired canonical catalog must remain " +
                        "duplicate-free."
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