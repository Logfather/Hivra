package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure
.CanonicalBoundedSemanticPolicyWaveSixFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure
.CanonicalConcreteFamilyAxisValueIndexFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatchManifestMerger
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatchManifestWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyType
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value
.CanonicalConcreteVariantValueCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalBoundedSemanticPolicyWaveSixCurationTest {

    @Test
    fun curateBoundedSemanticPolicyWaveSix() {
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

        val persistedWaveSixBatches =
            sourceManifest.batches
                .filter { batch ->
                    batch.curationId.startsWith(
                        CanonicalBoundedSemanticPolicyWaveSixFactory
                            .CURATION_ID_PREFIX
                    )
                }
                .sortedBy { batch ->
                    batch.sourceImplementationBatchKey
                }

        if (persistedWaveSixBatches.isNotEmpty()) {
            require(
                persistedWaveSixBatches.all { batch ->
                    batch.valid &&
                            batch.complete
                }
            ) {
                "Persisted Wave 6 contains an invalid curated batch."
            }

            println(
                buildString {
                    appendLine(
                        "Bounded semantic policy Wave 6 curation"
                    )
                    appendLine(
                        "---------------------------------------"
                    )
                    appendLine(
                        "Manifest: ${sourceManifest.manifestId}"
                    )
                    appendLine(
                        "Wave batches: ${persistedWaveSixBatches.size}"
                    )
                    appendLine(
                        "Wave policies: " +
                                persistedWaveSixBatches.sumOf { batch ->
                                    batch.policyCount
                                }
                    )
                    appendLine(
                        "Application mode: PRESERVED_ALREADY_CURATED"
                    )
                    append(
                        "Manifest valid: ${sourceManifest.valid}"
                    )
                }
            )

            return
        }

        val openBatches =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(
                    File(
                        projectDirectory,
                        OPEN_BATCH_PATH
                    )
                )

        require(
            openBatches.missingPolicyGapCount > 0
        ) {
            "No open semantic policy gaps remain for Wave 6."
        }

        val coverage =
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
                        coverage
                )

        val additions =
            CanonicalBoundedSemanticPolicyWaveSixFactory()
                .createBatches(
                    openImplementationBatches =
                        openBatches,

                    concreteValueIndex =
                        concreteValueIndex
                )

        val wavePolicies =
            additions.flatMap { batch ->
                batch.policies
            }

        assertEquals(
            openBatches.batchCount,
            additions.size
        )

        assertEquals(
            openBatches.missingPolicyGapCount,
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
                    additions.size,
            result.batchCount
        )

        assertEquals(
            sourceManifest.policyCount +
                    wavePolicies.size,
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

        println(
            buildString {
                appendLine(
                    "Bounded semantic policy Wave 6 curation"
                )
                appendLine(
                    "---------------------------------------"
                )
                appendLine(
                    "Previous manifest: ${sourceManifest.manifestId}"
                )
                appendLine(
                    "Updated manifest: ${persisted.manifestId}"
                )
                appendLine(
                    "Open policy gaps: " +
                            openBatches.missingPolicyGapCount
                )
                appendLine(
                    "Wave source batches: ${additions.size}"
                )
                appendLine(
                    "Wave policies: ${wavePolicies.size}"
                )
                appendLine(
                    "Curated policies: $curatedPolicyCount"
                )
                appendLine(
                    "Not-applicable policies: " +
                            notApplicablePolicyCount
                )
                appendLine(
                    "Newly curated batches: ${additions.size}"
                )
                appendLine(
                    "Newly curated policies: ${wavePolicies.size}"
                )
                append(
                    "Manifest valid: ${persisted.valid}"
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

        fun containsRequiredArtifacts(
            directory: File
        ): Boolean =
            REQUIRED_PATHS.all { relativePath ->
                File(
                    directory,
                    relativePath
                ).isFile
            }

        return when {
            containsRequiredArtifacts(
                workingDirectory
            ) ->
                workingDirectory

            workingDirectory.name ==
                    "app" &&
                    containsRequiredArtifacts(
                        requireNotNull(
                            workingDirectory.parentFile
                        )
                    ) ->
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

        const val OPEN_BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"

        const val CONCRETE_VALUE_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-concrete-variant-value-coverage.json"

        val REQUIRED_PATHS =
            listOf(
                CURATED_MANIFEST_PATH,
                OPEN_BATCH_PATH,
                CONCRETE_VALUE_COVERAGE_PATH
            )
    }
}