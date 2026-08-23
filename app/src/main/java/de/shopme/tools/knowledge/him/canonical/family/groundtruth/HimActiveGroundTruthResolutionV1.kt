package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.security.MessageDigest

object HimActiveGroundTruthResolutionContractV1 {

    const val VERSION =
        "HIM_ACTIVE_GROUND_TRUTH_RESOLUTION_V1"
}

data class HimActiveGroundTruthArtifactsV1(
    val releaseReference:
    HimGroundTruthReleaseIdentityV1,
    val releaseDirectory:
    File,
    val authorityFile:
    File,
    val activeRegistryFile:
    File,
    val retiredRegistryFile:
    File,
    val mutationLedgerFile:
    File,
    val fingerprintIndexFile:
    File,
    val releaseFile:
    File,
    val authorityBytes:
    ByteArray,
    val activeRegistryBytes:
    ByteArray,
    val retiredRegistryBytes:
    ByteArray,
    val mutationLedgerBytes:
    ByteArray,
    val fingerprintIndexBytes:
    ByteArray,
    val releaseBytes:
    ByteArray,
) {

    override fun equals(
        other: Any?,
    ): Boolean {
        if (this === other) {
            return true
        }

        if (
            other !is
                    HimActiveGroundTruthArtifactsV1
        ) {
            return false
        }

        return releaseReference ==
                other.releaseReference &&
                releaseDirectory ==
                other.releaseDirectory &&
                authorityFile ==
                other.authorityFile &&
                activeRegistryFile ==
                other.activeRegistryFile &&
                retiredRegistryFile ==
                other.retiredRegistryFile &&
                mutationLedgerFile ==
                other.mutationLedgerFile &&
                fingerprintIndexFile ==
                other.fingerprintIndexFile &&
                releaseFile ==
                other.releaseFile &&
                authorityBytes.contentEquals(
                    other.authorityBytes
                ) &&
                activeRegistryBytes.contentEquals(
                    other.activeRegistryBytes
                ) &&
                retiredRegistryBytes.contentEquals(
                    other.retiredRegistryBytes
                ) &&
                mutationLedgerBytes.contentEquals(
                    other.mutationLedgerBytes
                ) &&
                fingerprintIndexBytes.contentEquals(
                    other.fingerprintIndexBytes
                ) &&
                releaseBytes.contentEquals(
                    other.releaseBytes
                )
    }

    override fun hashCode(): Int {
        var result =
            releaseReference.hashCode()

        result =
            31 * result +
                    releaseDirectory.hashCode()

        result =
            31 * result +
                    authorityFile.hashCode()

        result =
            31 * result +
                    activeRegistryFile.hashCode()

        result =
            31 * result +
                    retiredRegistryFile.hashCode()

        result =
            31 * result +
                    mutationLedgerFile.hashCode()

        result =
            31 * result +
                    fingerprintIndexFile.hashCode()

        result =
            31 * result +
                    releaseFile.hashCode()

        result =
            31 * result +
                    authorityBytes.contentHashCode()

        result =
            31 * result +
                    activeRegistryBytes.contentHashCode()

        result =
            31 * result +
                    retiredRegistryBytes.contentHashCode()

        result =
            31 * result +
                    mutationLedgerBytes.contentHashCode()

        result =
            31 * result +
                    fingerprintIndexBytes.contentHashCode()

        result =
            31 * result +
                    releaseBytes.contentHashCode()

        return result
    }
}

class HimActiveGroundTruthResolutionV1(
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create(),
) {

    fun resolve(
        projectRoot: File,
    ): HimActiveGroundTruthArtifactsV1 {
        val groundTruthRoot =
            projectRoot.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .GROUND_TRUTH_DIRECTORY
            )

        val currentFile =
            groundTruthRoot.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .CURRENT_POINTER_FILE_NAME
            )

        require(
            currentFile.isFile
        ) {
            "Active Ground-Truth current pointer is missing: " +
                    currentFile.absolutePath
        }

        val current =
            requireNotNull(
                gson.fromJson(
                    currentFile.readText(),
                    HimGroundTruthCurrentPointerV1::class.java,
                )
            ) {
                "Active Ground-Truth current pointer is invalid JSON."
            }

        require(
            current.contractVersion ==
                    HimTransactionalGroundTruthPublicationContractV1
                        .CURRENT_POINTER_VERSION
        ) {
            "Unsupported Active Ground-Truth pointer contract: " +
                    current.contractVersion
        }

        val releaseDirectory =
            resolveReleaseDirectory(
                groundTruthRoot =
                    groundTruthRoot,
                current =
                    current,
            )

        val authorityFile =
            releaseDirectory.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .AUTHORITY_FILE_NAME
            )

        val activeRegistryFile =
            releaseDirectory.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .ACTIVE_REGISTRY_FILE_NAME
            )

        val retiredRegistryFile =
            releaseDirectory.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .RETIRED_REGISTRY_FILE_NAME
            )

        val mutationLedgerFile =
            releaseDirectory.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .MUTATION_LEDGER_FILE_NAME
            )

        val fingerprintIndexFile =
            releaseDirectory.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .FINGERPRINT_INDEX_FILE_NAME
            )

        val releaseFile =
            releaseDirectory.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .RELEASE_FILE_NAME
            )

        val expectedFiles =
            listOf(
                authorityFile,
                activeRegistryFile,
                retiredRegistryFile,
                mutationLedgerFile,
                fingerprintIndexFile,
                releaseFile,
            )

        expectedFiles.forEach { file ->
            require(
                file.isFile
            ) {
                "Active Ground-Truth artifact is missing: " +
                        file.absolutePath
            }

            require(
                file.length() > 0L
            ) {
                "Active Ground-Truth artifact is empty: " +
                        file.absolutePath
            }
        }

        val actualFileNames =
            releaseDirectory
                .listFiles()
                .orEmpty()
                .filter {
                    it.isFile
                }
                .map {
                    it.name
                }
                .toSet()

        val expectedFileNames =
            expectedFiles
                .map {
                    it.name
                }
                .toSet()

        require(
            actualFileNames ==
                    expectedFileNames
        ) {
            "Active Ground-Truth release artifact set mismatch."
        }

        val authorityBytes =
            authorityFile.readBytes()

        val activeRegistryBytes =
            activeRegistryFile.readBytes()

        val retiredRegistryBytes =
            retiredRegistryFile.readBytes()

        val mutationLedgerBytes =
            mutationLedgerFile.readBytes()

        val fingerprintIndexBytes =
            fingerprintIndexFile.readBytes()

        val releaseBytes =
            releaseFile.readBytes()

        require(
            sha256(
                releaseBytes
            ) ==
                    current.releaseArtifactSha256.value
        ) {
            "Active Ground-Truth release SHA-256 does not match current pointer."
        }

        val reconstructedIdentity =
            HimGroundTruthReleaseIdentityV1Factory.compute(
                HimTransactionalGroundTruthPublicationInputV1(
                    authorityBytes =
                        authorityBytes,
                    activeRegistryBytes =
                        activeRegistryBytes,
                    retiredRegistryBytes =
                        retiredRegistryBytes,
                    mutationLedgerBytes =
                        mutationLedgerBytes,
                    fingerprintIndexBytes =
                        fingerprintIndexBytes,
                    releaseBytes =
                        releaseBytes,
                )
            )

        require(
            reconstructedIdentity ==
                    current.releaseReference
        ) {
            "Active Ground-Truth release identity does not match current pointer."
        }

        require(
            releaseDirectory.name ==
                    current.releaseReference.value
                        .removePrefix(
                            "release:v1:"
                        )
        ) {
            "Active Ground-Truth release directory name does not match release identity."
        }

        return HimActiveGroundTruthArtifactsV1(
            releaseReference =
                current.releaseReference,
            releaseDirectory =
                releaseDirectory,
            authorityFile =
                authorityFile,
            activeRegistryFile =
                activeRegistryFile,
            retiredRegistryFile =
                retiredRegistryFile,
            mutationLedgerFile =
                mutationLedgerFile,
            fingerprintIndexFile =
                fingerprintIndexFile,
            releaseFile =
                releaseFile,
            authorityBytes =
                authorityBytes,
            activeRegistryBytes =
                activeRegistryBytes,
            retiredRegistryBytes =
                retiredRegistryBytes,
            mutationLedgerBytes =
                mutationLedgerBytes,
            fingerprintIndexBytes =
                fingerprintIndexBytes,
            releaseBytes =
                releaseBytes,
        )
    }

    private fun resolveReleaseDirectory(
        groundTruthRoot: File,
        current:
        HimGroundTruthCurrentPointerV1,
    ): File {
        val rootPath =
            groundTruthRoot
                .canonicalFile
                .toPath()

        val releaseDirectory =
            groundTruthRoot
                .resolve(
                    current.releaseDirectory
                )
                .canonicalFile

        val releasePath =
            releaseDirectory.toPath()

        require(
            releasePath.startsWith(
                rootPath
            )
        ) {
            "Active Ground-Truth release directory escapes Ground-Truth root."
        }

        require(
            releaseDirectory.parentFile
                ?.canonicalFile ==
                    groundTruthRoot
                        .resolve(
                            HimTransactionalGroundTruthPublicationContractV1
                                .RELEASES_DIRECTORY_NAME
                        )
                        .canonicalFile
        ) {
            "Active Ground-Truth release directory is outside releases root."
        }

        require(
            releaseDirectory.isDirectory
        ) {
            "Active Ground-Truth release directory is missing: " +
                    releaseDirectory.absolutePath
        }

        require(
            !releaseDirectory.name.endsWith(
                HimTransactionalGroundTruthPublicationContractV1
                    .STAGING_SUFFIX
            )
        ) {
            "Active Ground-Truth pointer must not reference staging."
        }

        return releaseDirectory
    }

    private fun sha256(
        bytes: ByteArray,
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte ->
                "%02x".format(
                    byte.toInt() and 0xff
                )
            }
}