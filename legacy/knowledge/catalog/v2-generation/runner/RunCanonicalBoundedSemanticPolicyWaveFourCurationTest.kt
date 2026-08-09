package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure.CanonicalBoundedSemanticPolicyClosurePlanReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure.CanonicalBoundedSemanticPolicyWaveFourFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure.CanonicalConcreteFamilyAxisValueIndexFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestMerger
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifestWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalBoundedSemanticPolicyWaveFourCurationTest {

    @Test
    fun curateBoundedSemanticPolicyWaveFour() {
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
            CanonicalBoundedSemanticPolicyWaveFourFactory()
                .createBatches(
                    closurePlan =
                        closurePlan,

                    concreteValueIndex =
                        concreteValueIndex
                )

        val vocabularyValidation =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(
                    entries =
                        additions.flatMap {
                            it.policies
                        }
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
                .associateBy {
                    it.sourceImplementationBatchKey
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
                    "Persisted Wave-4 batch diverged: " +
                            addition.sourceImplementationBatchKey
                )
            }
        }

        val newBatches =
            additions.filter {
                it.sourceImplementationBatchKey !in
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

        val wavePolicies =
            additions.flatMap {
                it.policies
            }

        println(
            buildString {
                appendLine(
                    "Bounded semantic policy Wave 4 curation"
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
                            wavePolicies.count {
                                it.policyType.name ==
                                        "CURATED_ALLOWED_VALUES"
                            }
                )
                appendLine(
                    "Not-applicable policies: " +
                            wavePolicies.count {
                                it.policyType.name ==
                                        "NOT_APPLICABLE"
                            }
                )
                appendLine(
                    "Newly curated batches: " +
                            newBatches.size
                )
                appendLine(
                    "Newly curated policies: " +
                            newBatches.sumOf {
                                it.policyCount
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