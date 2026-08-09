package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyBatchVocabularyValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatchManifestReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyType
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value
.CanonicalConcreteVariantValueCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBoundedSemanticPolicyWaveSixFactoryTest {

    @Test
    fun curateEveryWaveSixGapOrPreserveCompletedWave() {
        val openBatches =
            readOpenBatches()

        val batches =
            createOrReadWaveSixBatches()

        val policies =
            batches.flatMap { batch ->
                batch.policies
            }

        if (
            openBatches.missingPolicyGapCount >
            0
        ) {
            assertEquals(
                openBatches.batchCount,
                batches.size
            )

            assertEquals(
                openBatches.missingPolicyGapCount,
                policies.size
            )
        } else {
            assertEquals(
                EXPECTED_PERSISTED_WAVE_SIX_BATCH_COUNT,
                batches.size
            )

            assertEquals(
                EXPECTED_PERSISTED_WAVE_SIX_POLICY_COUNT,
                policies.size
            )
        }

        assertEquals(
            policies.size,
            policies
                .map { policy ->
                    policy.identityKey
                }
                .distinct()
                .size
        )

        assertTrue(
            batches.all { batch ->
                batch.valid &&
                        batch.complete
            }
        )

        assertTrue(
            policies.all { policy ->
                policy.active &&
                        policy.complete
            }
        )
    }

    @Test
    fun preserveEveryWaveSixPolicyIdentityExactly() {
        val openBatches =
            readOpenBatches()

        val actualPolicyIdentities =
            createOrReadWaveSixBatches()
                .flatMap { batch ->
                    batch.policies
                }
                .map { policy ->
                    policy.identityKey
                }
                .sorted()

        if (
            openBatches.missingPolicyGapCount >
            0
        ) {
            val expectedGapIdentities =
                openBatches.batches
                    .flatMap { batch ->
                        batch.gaps
                    }
                    .map { gap ->
                        gap.gapKey
                    }
                    .sorted()

            assertEquals(
                expectedGapIdentities,
                actualPolicyIdentities
            )
        } else {
            assertEquals(
                EXPECTED_PERSISTED_WAVE_SIX_POLICY_COUNT,
                actualPolicyIdentities.size
            )

            assertEquals(
                actualPolicyIdentities.size,
                actualPolicyIdentities
                    .distinct()
                    .size
            )
        }
    }

    @Test
    fun useOnlyClosedPolicyTypes() {
        val policies =
            createOrReadWaveSixBatches()
                .flatMap { batch ->
                    batch.policies
                }

        assertTrue(
            policies.all { policy ->
                policy.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES ||
                        policy.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }
        )

        assertTrue(
            policies
                .filter { policy ->
                    policy.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .CURATED_ALLOWED_VALUES
                }
                .all { policy ->
                    policy.allowedValues.isNotEmpty()
                }
        )

        assertTrue(
            policies
                .filter { policy ->
                    policy.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .NOT_APPLICABLE
                }
                .all { policy ->
                    policy.allowedValues.isEmpty()
                }
        )
    }

    @Test
    fun referenceOnlyCanonicalVariantValues() {
        val policies =
            createOrReadWaveSixBatches()
                .flatMap { batch ->
                    batch.policies
                }

        val validation =
            CanonicalSemanticPolicyBatchVocabularyValidator()
                .validate(
                    entries =
                        policies
                )

        assertTrue(
            validation.valid,
            validation.issues.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        assertEquals(
            0,
            validation.issueCount
        )
    }

    @Test
    fun createDeterministically() {
        assertEquals(
            createOrReadWaveSixBatches(),
            createOrReadWaveSixBatches()
        )
    }

    private fun createOrReadWaveSixBatches():
            List<CanonicalCuratedSemanticPolicyBatch> {
        val projectDirectory =
            resolveProjectDirectory()

        val openBatches =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(
                    File(
                        projectDirectory,
                        OPEN_BATCH_PATH
                    )
                )

        if (
            openBatches.missingPolicyGapCount ==
            0
        ) {
            val manifest =
                CanonicalCuratedSemanticPolicyBatchManifestReader()
                    .read(
                        File(
                            projectDirectory,
                            CURATED_MANIFEST_PATH
                        )
                    )

            val persistedWaveSixBatches =
                manifest.batches
                    .filter { batch ->
                        batch.curationId.startsWith(
                            CanonicalBoundedSemanticPolicyWaveSixFactory
                                .CURATION_ID_PREFIX
                        )
                    }
                    .sortedBy { batch ->
                        batch.sourceImplementationBatchKey
                    }

            require(
                persistedWaveSixBatches.size ==
                        EXPECTED_PERSISTED_WAVE_SIX_BATCH_COUNT
            ) {
                "Expected $EXPECTED_PERSISTED_WAVE_SIX_BATCH_COUNT " +
                        "persisted Wave-6 batches, but found " +
                        "${persistedWaveSixBatches.size}."
            }

            require(
                persistedWaveSixBatches.sumOf { batch ->
                    batch.policyCount
                } ==
                        EXPECTED_PERSISTED_WAVE_SIX_POLICY_COUNT
            ) {
                "Expected $EXPECTED_PERSISTED_WAVE_SIX_POLICY_COUNT " +
                        "persisted Wave-6 policies, but found " +
                        persistedWaveSixBatches.sumOf { batch ->
                            batch.policyCount
                        } +
                        "."
            }

            require(
                persistedWaveSixBatches.all { batch ->
                    batch.valid &&
                            batch.complete
                }
            ) {
                "Persisted Wave 6 contains an invalid or incomplete batch."
            }

            return persistedWaveSixBatches
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

        return CanonicalBoundedSemanticPolicyWaveSixFactory()
            .createBatches(
                openImplementationBatches =
                    openBatches,

                concreteValueIndex =
                    concreteValueIndex
            )
    }

    private fun readOpenBatches() =
        CanonicalSemanticPolicyImplementationBatchReader()
            .read(
                File(
                    resolveProjectDirectory(),
                    OPEN_BATCH_PATH
                )
            )

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty(
                        "user.dir"
                    )
                ) {
                    "System property 'user.dir' is not available."
                }
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

        const val OPEN_BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"

        const val CONCRETE_VALUE_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-concrete-variant-value-coverage.json"

        const val CURATED_MANIFEST_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-curation/" +
                    "canonical-curated-semantic-policy-batches.json"

        const val EXPECTED_PERSISTED_WAVE_SIX_BATCH_COUNT =
            2

        const val EXPECTED_PERSISTED_WAVE_SIX_POLICY_COUNT =
            14

        val REQUIRED_PATHS =
            listOf(
                OPEN_BATCH_PATH,
                CONCRETE_VALUE_COVERAGE_PATH,
                CURATED_MANIFEST_PATH
            )
    }
}