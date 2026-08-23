package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndex
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object HimGroundTruthReleaseContractV1 {

    const val RELEASE_VERSION =
        "HIM_GROUND_TRUTH_RELEASE_V1"

    const val DEFAULT_PATH =
        "data/knowledge/him/canonical-family/groundtruth/master/" +
                "ground-truth-release.v1.json"
}

data class HimGroundTruthReleaseBuildInputV1(
    val authority:
    HimCanonicalFamilyAuthority,
    val activeEntityIdRegistry:
    HimEntityIdRegistry,
    val retiredEntityIdRegistry:
    HimRetiredEntityIdRegistry,
    val mutationLedger:
    HimCanonicalFamilyMutationLedger,
    val entityFingerprintIndex:
    HimEntityFingerprintIndex,
    val authorityArtifact:
    HimGroundTruthReleaseArtifactReference,
    val activeEntityIdRegistryArtifact:
    HimGroundTruthReleaseArtifactReference,
    val retiredEntityIdRegistryArtifact:
    HimGroundTruthReleaseArtifactReference,
    val mutationLedgerArtifact:
    HimGroundTruthReleaseArtifactReference,
    val entityFingerprintIndexArtifact:
    HimGroundTruthReleaseArtifactReference,
)

class HimGroundTruthReleaseBuilderV1 {

    fun build(
        input: HimGroundTruthReleaseBuildInputV1,
    ): HimGroundTruthRelease {
        val release =
            HimGroundTruthRelease(
                releaseVersion =
                    HimGroundTruthReleaseContractV1.RELEASE_VERSION,
                sources =
                    HimGroundTruthReleaseSources(
                        canonicalFamilyAuthority =
                            input.authorityArtifact,
                        activeEntityIdRegistry =
                            input.activeEntityIdRegistryArtifact,
                        retiredEntityIdRegistry =
                            input.retiredEntityIdRegistryArtifact,
                        mutationLedger =
                            input.mutationLedgerArtifact,
                        entityFingerprintIndex =
                            input.entityFingerprintIndexArtifact,
                    ),
                counts =
                    counts(input),
                state =
                    HimGroundTruthReleaseState.RELEASED,
            )

        HimGroundTruthReleaseValidatorV1()
            .validate(
                release = release,
                input = input,
            )

        return release
    }

    private fun counts(
        input: HimGroundTruthReleaseBuildInputV1,
    ): HimGroundTruthReleaseCounts {
        val identities =
            input.authority.families.sumOf {
                it.identities.size
            }

        val canonicalVariants =
            input.authority.families.sumOf {
                it.variants.size
            }

        val identityVariants =
            input.authority.families.sumOf { family ->
                family.identities.sumOf {
                    it.variants.size
                }
            }

        val canonicalAliases =
            input.authority.families.sumOf {
                it.aliases.size
            }

        val identityAliases =
            input.authority.families.sumOf { family ->
                family.identities.sumOf {
                    it.aliases.size
                }
            }

        return HimGroundTruthReleaseCounts(
            canonicals =
                input.authority.families.size,
            identities =
                identities,
            variants =
                canonicalVariants +
                        identityVariants,
            aliases =
                canonicalAliases +
                        identityAliases,
            activeEntityIds =
                input.activeEntityIdRegistry.entries.size,
            retiredEntityIds =
                input.retiredEntityIdRegistry.entries.size,
            mutations =
                input.mutationLedger.entries.size,
        )
    }
}

data class HimGroundTruthReleaseValidationResultV1(
    val canonicals: Int,
    val identities: Int,
    val variants: Int,
    val aliases: Int,
    val activeEntityIds: Int,
    val retiredEntityIds: Int,
    val mutations: Int,
    val fingerprintEntries: Int,
)

class HimGroundTruthReleaseValidatorV1 {

    fun validate(
        release: HimGroundTruthRelease,
        input: HimGroundTruthReleaseBuildInputV1,
    ): HimGroundTruthReleaseValidationResultV1 {
        require(
            release.releaseVersion ==
                    HimGroundTruthReleaseContractV1.RELEASE_VERSION
        ) {
            "Unsupported Ground-Truth release version: ${release.releaseVersion}"
        }

        require(
            release.state ==
                    HimGroundTruthReleaseState.RELEASED
        ) {
            "Ground-Truth release is not RELEASED."
        }

        validateArtifactReferences(
            release = release,
            input = input,
        )

        val expectedCounts =
            HimGroundTruthReleaseBuilderV1()
                .let {
                    /*
                     * Build count semantics directly here rather than
                     * recursively building another release.
                     */
                    expectedCounts(input)
                }

        require(
            release.counts ==
                    expectedCounts
        ) {
            "Ground-Truth release counts do not match materialized state."
        }

        require(
            release.sources
                .canonicalFamilyAuthority
                .recordCount ==
                    input.authority.families.size
        ) {
            "Authority artifact record count mismatch."
        }

        require(
            release.sources
                .activeEntityIdRegistry
                .recordCount ==
                    input.activeEntityIdRegistry.entries.size
        ) {
            "Active Entity-ID Registry artifact record count mismatch."
        }

        require(
            release.sources
                .retiredEntityIdRegistry
                .recordCount ==
                    input.retiredEntityIdRegistry.entries.size
        ) {
            "Retired Entity-ID Registry artifact record count mismatch."
        }

        require(
            release.sources
                .mutationLedger
                .recordCount ==
                    input.mutationLedger.entries.size
        ) {
            "Mutation Ledger artifact record count mismatch."
        }

        require(
            release.sources
                .entityFingerprintIndex
                .recordCount ==
                    input.entityFingerprintIndex.entryCount
        ) {
            "Entity Fingerprint Index artifact record count mismatch."
        }

        require(
            input.entityFingerprintIndex.entryCount ==
                    input.entityFingerprintIndex.entries.size
        ) {
            "Entity Fingerprint Index entryCount mismatch."
        }

        require(
            input.entityFingerprintIndex
                .sourceAuthorities
                .entityIdRegistrySha256 ==
                    input.activeEntityIdRegistryArtifact.sha256.value
        ) {
            "Fingerprint Index Registry SHA-256 binding mismatch."
        }

        require(
            input.entityFingerprintIndex
                .sourceAuthorities
                .canonicalFamilyAuthoritySha256 ==
                    input.authorityArtifact.sha256.value
        ) {
            "Fingerprint Index Authority SHA-256 binding mismatch."
        }

        require(
            input.activeEntityIdRegistry.entries
                .map { it.entityId }
                .distinct()
                .size ==
                    input.activeEntityIdRegistry.entries.size
        ) {
            "Duplicate active Entity IDs."
        }

        require(
            input.retiredEntityIdRegistry.entries
                .map { it.entityId }
                .distinct()
                .size ==
                    input.retiredEntityIdRegistry.entries.size
        ) {
            "Duplicate retired Entity IDs."
        }

        val activeIds =
            input.activeEntityIdRegistry.entries
                .map { it.entityId }
                .toSet()

        val retiredIds =
            input.retiredEntityIdRegistry.entries
                .map { it.entityId }
                .toSet()

        require(
            activeIds.intersect(retiredIds).isEmpty()
        ) {
            "Entity ID is simultaneously active and retired."
        }

        return HimGroundTruthReleaseValidationResultV1(
            canonicals =
                release.counts.canonicals,
            identities =
                release.counts.identities,
            variants =
                release.counts.variants,
            aliases =
                release.counts.aliases,
            activeEntityIds =
                release.counts.activeEntityIds,
            retiredEntityIds =
                release.counts.retiredEntityIds,
            mutations =
                release.counts.mutations,
            fingerprintEntries =
                input.entityFingerprintIndex.entryCount,
        )
    }

    private fun validateArtifactReferences(
        release: HimGroundTruthRelease,
        input: HimGroundTruthReleaseBuildInputV1,
    ) {
        require(
            release.sources.canonicalFamilyAuthority ==
                    input.authorityArtifact
        ) {
            "Authority artifact binding mismatch."
        }

        require(
            release.sources.activeEntityIdRegistry ==
                    input.activeEntityIdRegistryArtifact
        ) {
            "Active Registry artifact binding mismatch."
        }

        require(
            release.sources.retiredEntityIdRegistry ==
                    input.retiredEntityIdRegistryArtifact
        ) {
            "Retired Registry artifact binding mismatch."
        }

        require(
            release.sources.mutationLedger ==
                    input.mutationLedgerArtifact
        ) {
            "Mutation Ledger artifact binding mismatch."
        }

        require(
            release.sources.entityFingerprintIndex ==
                    input.entityFingerprintIndexArtifact
        ) {
            "Fingerprint Index artifact binding mismatch."
        }
    }

    private fun expectedCounts(
        input: HimGroundTruthReleaseBuildInputV1,
    ): HimGroundTruthReleaseCounts {
        val identities =
            input.authority.families.sumOf {
                it.identities.size
            }

        val variants =
            input.authority.families.sumOf { family ->
                family.variants.size +
                        family.identities.sumOf {
                            it.variants.size
                        }
            }

        val aliases =
            input.authority.families.sumOf { family ->
                family.aliases.size +
                        family.identities.sumOf {
                            it.aliases.size
                        }
            }

        return HimGroundTruthReleaseCounts(
            canonicals =
                input.authority.families.size,
            identities =
                identities,
            variants =
                variants,
            aliases =
                aliases,
            activeEntityIds =
                input.activeEntityIdRegistry.entries.size,
            retiredEntityIds =
                input.retiredEntityIdRegistry.entries.size,
            mutations =
                input.mutationLedger.entries.size,
        )
    }
}

class HimGroundTruthReleasePersistenceV1(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create(),
) {

    fun serialize(
        release: HimGroundTruthRelease,
    ): ByteArray =
        (
                gson.toJson(release) +
                        "\n"
                ).toByteArray(Charsets.UTF_8)

    fun read(
        file: File,
    ): HimGroundTruthRelease {
        require(file.isFile) {
            "Ground-Truth release is missing: ${file.absolutePath}"
        }

        return requireNotNull(
            gson.fromJson(
                file.readText(),
                HimGroundTruthRelease::class.java,
            )
        ) {
            "Ground-Truth release is invalid JSON: ${file.absolutePath}"
        }
    }

    fun write(
        file: File,
        release: HimGroundTruthRelease,
    ) {
        atomicWrite(
            file = file,
            bytes = serialize(release),
        )
    }

    fun defaultReleaseFile(
        projectRoot: File,
    ): File =
        projectRoot.resolve(
            HimGroundTruthReleaseContractV1.DEFAULT_PATH
        )

    private fun atomicWrite(
        file: File,
        bytes: ByteArray,
    ) {
        val parent =
            requireNotNull(file.parentFile)

        require(
            parent.exists() ||
                    parent.mkdirs()
        ) {
            "Could not create Ground-Truth release directory: ${parent.absolutePath}"
        }

        val temporary =
            Files.createTempFile(
                parent.toPath(),
                ".${file.name}.",
                ".tmp",
            )

        try {
            Files.write(
                temporary,
                bytes,
            )

            try {
                Files.move(
                    temporary,
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary,
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } finally {
            Files.deleteIfExists(
                temporary
            )
        }
    }
}