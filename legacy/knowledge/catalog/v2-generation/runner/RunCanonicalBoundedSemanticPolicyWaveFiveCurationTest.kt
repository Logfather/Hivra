package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure.CanonicalBoundedSemanticPolicyClosurePlanReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure.CanonicalBoundedSemanticPolicyOverrideRegistry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure.CanonicalBoundedSemanticPolicyWaveFiveFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure.CanonicalConcreteFamilyAxisValueIndexFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestMerger
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalBoundedSemanticPolicyWaveFiveCurationTest {

    @Test
    fun curateBoundedSemanticPolicyWaveFive() {
        val projectDirectory =
            resolveProjectDirectory()

        val manifestFile =
            File(
                projectDirectory,
                CURATED_MANIFEST_PATH
            )

        val sourceManifest =
            CanonicalCuratedSemanticPolicyBatchManifestReader()
                .read(manifestFile)

        val closurePlan =
            CanonicalBoundedSemanticPolicyClosurePlanReader()
                .read(
                    File(
                        projectDirectory,
                        CLOSURE_PLAN_PATH
                    )
                )

        val concreteValueCoverage =
            CanonicalConcreteVariantValueCoverageReader()
                .read(
                    File(
                        projectDirectory,
                        CONCRETE_VALUE_COVERAGE_PATH
                    )
                )

        val concreteValueIndex =
            CanonicalConcreteFamilyAxisValueIndexFactory()
                .create(
                    coverage =
                        concreteValueCoverage
                )

        val additions =
            CanonicalBoundedSemanticPolicyWaveFiveFactory()
                .createBatches(
                    closurePlan =
                        closurePlan,

                    concreteValueIndex =
                        concreteValueIndex
                )

        val wavePolicies =
            additions.flatMap { batch ->
                batch.policies
            }

        val expectedWave =
            closurePlan.waves
                .single { wave ->
                    wave.waveNumber ==
                            CanonicalBoundedSemanticPolicyWaveFiveFactory
                                .WAVE_NUMBER
                }

        assertEquals(
            expectedWave.assignedBatchCount,
            additions.size
        )

        assertEquals(
            expectedWave.assignedGapCount,
            wavePolicies.size
        )

        val vocabularyValidation =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(
                    entries =
                        wavePolicies
                )

        assertTrue(
            vocabularyValidation.valid,
            vocabularyValidation.issues.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        val existingByBatchKey =
            sourceManifest.batches
                .associateBy { batch ->
                    batch.sourceImplementationBatchKey
                }

        additions.forEach { addition ->
            val existing =
                existingByBatchKey[
                    addition.sourceImplementationBatchKey
                ]

            if (existing != null) {
                assertEquals(
                    addition,
                    existing,
                    "Persisted Wave-5 batch diverged: " +
                            addition.sourceImplementationBatchKey
                )
            }
        }

        val newBatches =
            additions.filter { addition ->
                addition.sourceImplementationBatchKey !in
                        existingByBatchKey
            }

        val result =
            CanonicalCuratedSemanticPolicyBatchManifestMerger()
                .merge(
                    source =
                        sourceManifest,

                    additions =
                        additions
                )

        assertTrue(result.valid)

        assertEquals(
            sourceManifest.batchCount +
                    newBatches.size,
            result.batchCount
        )

        assertEquals(
            sourceManifest.policyCount +
                    newBatches.sumOf { batch ->
                        batch.policyCount
                    },
            result.policyCount
        )

        CanonicalCuratedSemanticPolicyBatchManifestWriter()
            .write(
                manifest =
                    result,

                outputFile =
                    manifestFile
            )

        val persisted =
            CanonicalCuratedSemanticPolicyBatchManifestReader()
                .read(manifestFile)

        assertEquals(
            result,
            persisted
        )

        additions.forEach { addition ->
            assertEquals(
                addition,
                persisted.batches.single { persistedBatch ->
                    persistedBatch.sourceImplementationBatchKey ==
                            addition.sourceImplementationBatchKey
                }
            )
        }

        val curatedPolicyCount =
            wavePolicies.count { policy ->
                policy.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES
            }

        val notApplicablePolicyCount =
            wavePolicies.count { policy ->
                policy.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }

        val overridePolicyCount =
            wavePolicies.count { policy ->
                policy.source ==
                        CanonicalBoundedSemanticPolicyOverrideRegistry
                            .OVERRIDE_SOURCE
            }

        println(
            buildString {
                appendLine(
                    "Bounded semantic policy Wave 5 curation"
                )
                appendLine(
                    "---------------------------------------"
                )
                appendLine(
                    "Previous manifest: " +
                            sourceManifest.manifestId
                )
                appendLine(
                    "Updated manifest: " +
                            persisted.manifestId
                )
                appendLine(
                    "Wave source batches: " +
                            additions.size
                )
                appendLine(
                    "Wave policies: " +
                            wavePolicies.size
                )
                appendLine(
                    "Curated policies: " +
                            curatedPolicyCount
                )
                appendLine(
                    "Not-applicable policies: " +
                            notApplicablePolicyCount
                )
                appendLine(
                    "Explicit override policies: " +
                            overridePolicyCount
                )
                appendLine(
                    "Newly curated batches: " +
                            newBatches.size
                )
                appendLine(
                    "Newly curated policies: " +
                            newBatches.sumOf { batch ->
                                batch.policyCount
                            }
                )
                append(
                    "Manifest valid: " +
                            persisted.valid
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
            REQUIRED_PATHS.all { relativePath ->
                File(
                    workingDirectory,
                    relativePath
                ).isFile
            } ->
                workingDirectory

            workingDirectory.name ==
                    "app" ->
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

        const val CURATED_MANIFEST_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-curation/" +
                    "canonical-curated-semantic-policy-batches.json"

        const val CLOSURE_PLAN_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-closure/" +
                    "canonical-bounded-semantic-policy-closure-plan.json"

        const val CONCRETE_VALUE_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-concrete-variant-value-coverage.json"

        val REQUIRED_PATHS =
            listOf(
                CURATED_MANIFEST_PATH,
                CLOSURE_PLAN_PATH,
                CONCRETE_VALUE_COVERAGE_PATH
            )
    }
}