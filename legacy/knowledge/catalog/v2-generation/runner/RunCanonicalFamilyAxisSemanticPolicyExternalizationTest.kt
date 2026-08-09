package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalInitialFamilyAxisSemanticPolicyFactory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalFamilyAxisSemanticPolicyExternalizationTest {

    @Test
    fun externalizeCanonicalFamilyAxisSemanticPolicies() {
        val projectDirectory =
            resolveProjectDirectory()

        val baseline =
            CanonicalFoodCatalogBaselineReader()
                .read(
                    File(
                        projectDirectory,
                        BASELINE_PATH
                    )
                )

        val basePolicySet =
            CanonicalInitialFamilyAxisSemanticPolicyFactory()
                .create(
                    baseline = baseline
                )

        assertTrue(basePolicySet.valid)
        assertTrue(basePolicySet.policyCount > 0)

        assertEquals(
            basePolicySet.policyCount,
            basePolicySet.completePolicyCount
        )

        assertEquals(
            0,
            basePolicySet.incompletePolicyCount
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        val effectivePolicySet =
            if (outputFile.isFile && outputFile.length() > 0L) {
                val existingPolicySet =
                    CanonicalFamilyAxisSemanticPolicySetReader()
                        .read(outputFile)

                require(
                    existingPolicySet.sourceBaselineId ==
                            basePolicySet.sourceBaselineId
                ) {
                    "Existing semantic policy set belongs to another baseline. " +
                            "existing=${existingPolicySet.sourceBaselineId}, " +
                            "expected=${basePolicySet.sourceBaselineId}."
                }

                require(
                    existingPolicySet.sourceBaselineCatalogSha256 ==
                            basePolicySet.sourceBaselineCatalogSha256
                ) {
                    "Existing semantic policy set belongs to another catalog " +
                            "artifact."
                }

                val existingEntriesByIdentity =
                    existingPolicySet.entries
                        .associateBy {
                            it.identityKey
                        }

                basePolicySet.entries.forEach { baseEntry ->
                    require(
                        existingEntriesByIdentity[
                            baseEntry.identityKey
                        ] ==
                                baseEntry
                    ) {
                        "Existing semantic policy set does not preserve base " +
                                "policy '${baseEntry.identityKey}'."
                    }
                }

                require(
                    existingPolicySet.policyCount >=
                            basePolicySet.policyCount
                ) {
                    "Existing semantic policy set must not contain fewer " +
                            "policies than the canonical base policy set."
                }

                /*
                 * Ein bereits erweitertes Policy-Set darf durch die
                 * Externalisierung niemals auf den Basisstand zurückgesetzt
                 * werden.
                 */
                existingPolicySet
            } else {
                CanonicalFamilyAxisSemanticPolicySetWriter()
                    .write(
                        policySet =
                            basePolicySet,

                        outputFile =
                            outputFile
                    )

                basePolicySet
            }

        val persisted =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(outputFile)

        assertEquals(
            effectivePolicySet,
            persisted
        )

        val persistedEntriesByIdentity =
            persisted.entries
                .associateBy {
                    it.identityKey
                }

        basePolicySet.entries.forEach { baseEntry ->
            assertEquals(
                baseEntry,
                persistedEntriesByIdentity[
                    baseEntry.identityKey
                ],
                "Persisted policy set must preserve base policy " +
                        "'${baseEntry.identityKey}'."
            )
        }

        println(
            buildString {
                appendLine(
                    "Canonical family-axis semantic policy externalization"
                )
                appendLine(
                    "----------------------------------------------------"
                )
                appendLine(
                    "Base policy set ID: " +
                            basePolicySet.policySetId
                )
                appendLine(
                    "Effective policy set ID: " +
                            persisted.policySetId
                )
                appendLine(
                    "Base policies: " +
                            basePolicySet.policyCount
                )
                appendLine(
                    "Effective policies: " +
                            persisted.policyCount
                )
                appendLine(
                    "Preserved extension policies: " +
                            (
                                    persisted.policyCount -
                                            basePolicySet.policyCount
                                    )
                )
                appendLine(
                    "Effective policy set valid: " +
                            persisted.valid
                )
                append(
                    "Externalization mode: " +
                            if (
                                persisted.policyCount >
                                basePolicySet.policyCount
                            ) {
                                "PRESERVED_EXISTING_SUPERSET"
                            } else {
                                "BASE_POLICY_SET"
                            }
                )
            }
        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                )
            ).canonicalFile

        return when {
            File(
                workingDirectory,
                BASELINE_PATH
            ).isFile ->
                workingDirectory

            workingDirectory.name == "app" ->
                requireNotNull(
                    workingDirectory.parentFile
                )

            else ->
                error(
                    "Could not resolve ShopMe project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }

    private companion object {
        const val BASELINE_PATH =
            "data/generated/knowledge/catalog/baseline/" +
                    "canonical-food-catalog-baseline.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"
    }
}