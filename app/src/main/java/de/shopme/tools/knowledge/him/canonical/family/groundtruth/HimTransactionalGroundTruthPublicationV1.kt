package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimTransactionalGroundTruthPublicationContractV1 {

    const val VERSION =
        "HIM_TRANSACTIONAL_GROUND_TRUTH_PUBLICATION_V1"

    const val CURRENT_POINTER_VERSION =
        "HIM_GROUND_TRUTH_CURRENT_POINTER_V1"

    const val RELEASE_IDENTITY_CONTRACT =
        "HIM_GROUND_TRUTH_RELEASE_IDENTITY_V1"

    const val GROUND_TRUTH_DIRECTORY =
        "data/knowledge/him/canonical-family/groundtruth"

    const val RELEASES_DIRECTORY_NAME =
        "releases"

    const val CURRENT_POINTER_FILE_NAME =
        "current.v1.json"

    const val AUTHORITY_FILE_NAME =
        "canonical-family-authority.v1.json"

    const val ACTIVE_REGISTRY_FILE_NAME =
        "him-entity-id-registry.v1.json"

    const val RETIRED_REGISTRY_FILE_NAME =
        "retired-entity-id-registry.v1.json"

    const val MUTATION_LEDGER_FILE_NAME =
        "canonical-family-mutation-ledger.v1.json"

    const val FINGERPRINT_INDEX_FILE_NAME =
        "him-entity-fingerprint-index.v1.json"

    const val RELEASE_FILE_NAME =
        "ground-truth-release.v1.json"

    const val STAGING_SUFFIX =
        ".staging"
}

data class HimGroundTruthReleaseIdentityV1(
    val value: String,
) {
    init {
        require(
            value.matches(
                Regex("release:v1:[0-9a-f]{64}")
            )
        ) {
            "Invalid Ground-Truth release identity: $value"
        }
    }
}

data class HimGroundTruthCurrentPointerV1(
    val contractVersion: String =
        HimTransactionalGroundTruthPublicationContractV1
            .CURRENT_POINTER_VERSION,
    val releaseReference:
    HimGroundTruthReleaseIdentityV1,
    val releaseDirectory: String,
    val releaseArtifactSha256:
    HimSha256,
) {
    init {
        require(
            contractVersion ==
                    HimTransactionalGroundTruthPublicationContractV1
                        .CURRENT_POINTER_VERSION
        ) {
            "Unsupported Ground-Truth current-pointer contract: $contractVersion"
        }

        require(
            releaseDirectory.isNotBlank()
        ) {
            "Ground-Truth release directory must not be blank."
        }
    }
}

data class HimTransactionalGroundTruthPublicationInputV1(
    val authorityBytes: ByteArray,
    val activeRegistryBytes: ByteArray,
    val retiredRegistryBytes: ByteArray,
    val mutationLedgerBytes: ByteArray,
    val fingerprintIndexBytes: ByteArray,
    val releaseBytes: ByteArray,
) {
    init {
        require(authorityBytes.isNotEmpty())
        require(activeRegistryBytes.isNotEmpty())
        require(retiredRegistryBytes.isNotEmpty())
        require(mutationLedgerBytes.isNotEmpty())
        require(fingerprintIndexBytes.isNotEmpty())
        require(releaseBytes.isNotEmpty())
    }

    override fun equals(
        other: Any?,
    ): Boolean {
        if (this === other) {
            return true
        }

        if (
            other !is
                    HimTransactionalGroundTruthPublicationInputV1
        ) {
            return false
        }

        return authorityBytes.contentEquals(
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

data class HimTransactionalGroundTruthPublicationResultV1(
    val releaseReference:
    HimGroundTruthReleaseIdentityV1,
    val releaseDirectory:
    File,
    val currentPointer:
    HimGroundTruthCurrentPointerV1,
    val created:
    Boolean,
)

object HimGroundTruthReleaseIdentityV1Factory {

    fun compute(
        input:
        HimTransactionalGroundTruthPublicationInputV1,
    ): HimGroundTruthReleaseIdentityV1 {
        val canonical =
            buildString {
                appendLine(
                    "contract=" +
                            HimTransactionalGroundTruthPublicationContractV1
                                .RELEASE_IDENTITY_CONTRACT
                )

                appendLine(
                    "authority=" +
                            sha256(
                                input.authorityBytes
                            )
                )

                appendLine(
                    "active-registry=" +
                            sha256(
                                input.activeRegistryBytes
                            )
                )

                appendLine(
                    "retired-registry=" +
                            sha256(
                                input.retiredRegistryBytes
                            )
                )

                appendLine(
                    "mutation-ledger=" +
                            sha256(
                                input.mutationLedgerBytes
                            )
                )

                appendLine(
                    "fingerprint-index=" +
                            sha256(
                                input.fingerprintIndexBytes
                            )
                )

                appendLine(
                    "release=" +
                            sha256(
                                input.releaseBytes
                            )
                )
            }

        return HimGroundTruthReleaseIdentityV1(
            value =
                "release:v1:" +
                        sha256(
                            canonical.toByteArray(
                                Charsets.UTF_8
                            )
                        )
        )
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

class HimTransactionalGroundTruthPublicationV1(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create(),
) {

    fun publish(
        projectRoot: File,
        input:
        HimTransactionalGroundTruthPublicationInputV1,
        validate:
            (File) -> Unit,
    ): HimTransactionalGroundTruthPublicationResultV1 {

        val groundTruthRoot =
            projectRoot.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .GROUND_TRUTH_DIRECTORY
            )

        val releasesRoot =
            groundTruthRoot.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .RELEASES_DIRECTORY_NAME
            )

        require(
            releasesRoot.exists() ||
                    releasesRoot.mkdirs()
        ) {
            "Could not create Ground-Truth releases directory: " +
                    releasesRoot.absolutePath
        }

        val releaseReference =
            HimGroundTruthReleaseIdentityV1Factory.compute(
                input
            )

        val releaseDirectoryName =
            releaseReference.value.removePrefix(
                "release:v1:"
            )

        val finalReleaseDirectory =
            releasesRoot.resolve(
                releaseDirectoryName
            )

        val stagingDirectory =
            releasesRoot.resolve(
                releaseDirectoryName +
                        HimTransactionalGroundTruthPublicationContractV1
                            .STAGING_SUFFIX
            )

        if (finalReleaseDirectory.exists()) {
            require(
                !stagingDirectory.exists()
            ) {
                "Stale Ground-Truth staging directory exists: " +
                        stagingDirectory.absolutePath
            }

            verifyExistingRelease(
                releaseDirectory =
                    finalReleaseDirectory,
                input =
                    input,
            )

            validate(
                finalReleaseDirectory
            )

            val pointer =
                currentPointer(
                    groundTruthRoot =
                        groundTruthRoot,
                    releaseReference =
                        releaseReference,
                    releaseDirectory =
                        finalReleaseDirectory,
                    releaseBytes =
                        input.releaseBytes,
                )

            publishCurrentPointer(
                groundTruthRoot =
                    groundTruthRoot,
                pointer =
                    pointer,
            )

            return HimTransactionalGroundTruthPublicationResultV1(
                releaseReference =
                    releaseReference,
                releaseDirectory =
                    finalReleaseDirectory,
                currentPointer =
                    pointer,
                created =
                    false,
            )
        }

        require(
            !stagingDirectory.exists()
        ) {
            "Stale Ground-Truth staging directory exists: " +
                    stagingDirectory.absolutePath
        }

        require(
            stagingDirectory.mkdir()
        ) {
            "Could not create Ground-Truth staging directory: " +
                    stagingDirectory.absolutePath
        }

        var releaseCommitted =
            false

        try {
            writeReleaseArtifacts(
                directory =
                    stagingDirectory,
                input =
                    input,
            )

            verifyExistingRelease(
                releaseDirectory =
                    stagingDirectory,
                input =
                    input,
            )

            validate(
                stagingDirectory
            )

            moveDirectoryAtomically(
                source =
                    stagingDirectory,
                target =
                    finalReleaseDirectory,
            )

            releaseCommitted =
                true

            verifyExistingRelease(
                releaseDirectory =
                    finalReleaseDirectory,
                input =
                    input,
            )

            validate(
                finalReleaseDirectory
            )

            val pointer =
                currentPointer(
                    groundTruthRoot =
                        groundTruthRoot,
                    releaseReference =
                        releaseReference,
                    releaseDirectory =
                        finalReleaseDirectory,
                    releaseBytes =
                        input.releaseBytes,
                )

            publishCurrentPointer(
                groundTruthRoot =
                    groundTruthRoot,
                pointer =
                    pointer,
            )

            return HimTransactionalGroundTruthPublicationResultV1(
                releaseReference =
                    releaseReference,
                releaseDirectory =
                    finalReleaseDirectory,
                currentPointer =
                    pointer,
                created =
                    true,
            )
        } finally {
            if (
                !releaseCommitted &&
                stagingDirectory.exists()
            ) {
                stagingDirectory.deleteRecursively()
            }
        }
    }

    fun readCurrent(
        projectRoot: File,
    ): HimGroundTruthCurrentPointerV1? {
        val file =
            projectRoot
                .resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .GROUND_TRUTH_DIRECTORY
                )
                .resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .CURRENT_POINTER_FILE_NAME
                )

        if (!file.isFile) {
            return null
        }

        return requireNotNull(
            gson.fromJson(
                file.readText(),
                HimGroundTruthCurrentPointerV1::class.java,
            )
        ) {
            "Ground-Truth current pointer is invalid JSON: " +
                    file.absolutePath
        }
    }

    fun serializeCurrent(
        pointer:
        HimGroundTruthCurrentPointerV1,
    ): ByteArray =
        (
                gson.toJson(pointer) +
                        "\n"
                ).toByteArray(
                Charsets.UTF_8
            )

    private fun writeReleaseArtifacts(
        directory: File,
        input:
        HimTransactionalGroundTruthPublicationInputV1,
    ) {
        writeExact(
            file =
                directory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .AUTHORITY_FILE_NAME
                ),
            bytes =
                input.authorityBytes,
        )

        writeExact(
            file =
                directory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .ACTIVE_REGISTRY_FILE_NAME
                ),
            bytes =
                input.activeRegistryBytes,
        )

        writeExact(
            file =
                directory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .RETIRED_REGISTRY_FILE_NAME
                ),
            bytes =
                input.retiredRegistryBytes,
        )

        writeExact(
            file =
                directory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .MUTATION_LEDGER_FILE_NAME
                ),
            bytes =
                input.mutationLedgerBytes,
        )

        writeExact(
            file =
                directory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .FINGERPRINT_INDEX_FILE_NAME
                ),
            bytes =
                input.fingerprintIndexBytes,
        )

        writeExact(
            file =
                directory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .RELEASE_FILE_NAME
                ),
            bytes =
                input.releaseBytes,
        )
    }

    private fun verifyExistingRelease(
        releaseDirectory: File,
        input:
        HimTransactionalGroundTruthPublicationInputV1,
    ) {
        require(
            releaseDirectory.isDirectory
        ) {
            "Ground-Truth release directory is missing: " +
                    releaseDirectory.absolutePath
        }

        val expected =
            linkedMapOf(
                HimTransactionalGroundTruthPublicationContractV1
                    .AUTHORITY_FILE_NAME to
                        input.authorityBytes,

                HimTransactionalGroundTruthPublicationContractV1
                    .ACTIVE_REGISTRY_FILE_NAME to
                        input.activeRegistryBytes,

                HimTransactionalGroundTruthPublicationContractV1
                    .RETIRED_REGISTRY_FILE_NAME to
                        input.retiredRegistryBytes,

                HimTransactionalGroundTruthPublicationContractV1
                    .MUTATION_LEDGER_FILE_NAME to
                        input.mutationLedgerBytes,

                HimTransactionalGroundTruthPublicationContractV1
                    .FINGERPRINT_INDEX_FILE_NAME to
                        input.fingerprintIndexBytes,

                HimTransactionalGroundTruthPublicationContractV1
                    .RELEASE_FILE_NAME to
                        input.releaseBytes,
            )

        val actualNames =
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

        require(
            actualNames ==
                    expected.keys.toSet()
        ) {
            "Ground-Truth release artifact set mismatch."
        }

        expected.forEach { (name, bytes) ->
            val file =
                releaseDirectory.resolve(
                    name
                )

            require(
                file.isFile
            ) {
                "Ground-Truth release artifact is missing: " +
                        file.absolutePath
            }

            require(
                file.readBytes()
                    .contentEquals(bytes)
            ) {
                "Ground-Truth release artifact differs from expected bytes: " +
                        file.absolutePath
            }
        }
    }

    private fun currentPointer(
        groundTruthRoot: File,
        releaseReference:
        HimGroundTruthReleaseIdentityV1,
        releaseDirectory:
        File,
        releaseBytes:
        ByteArray,
    ): HimGroundTruthCurrentPointerV1 {
        val relative =
            groundTruthRoot
                .toPath()
                .relativize(
                    releaseDirectory.toPath()
                )
                .toString()

        return HimGroundTruthCurrentPointerV1(
            releaseReference =
                releaseReference,
            releaseDirectory =
                relative,
            releaseArtifactSha256 =
                HimSha256(
                    sha256(
                        releaseBytes
                    )
                ),
        )
    }

    private fun publishCurrentPointer(
        groundTruthRoot: File,
        pointer:
        HimGroundTruthCurrentPointerV1,
    ) {
        require(
            groundTruthRoot.exists() ||
                    groundTruthRoot.mkdirs()
        ) {
            "Could not create Ground-Truth root directory: " +
                    groundTruthRoot.absolutePath
        }

        val current =
            groundTruthRoot.resolve(
                HimTransactionalGroundTruthPublicationContractV1
                    .CURRENT_POINTER_FILE_NAME
            )

        val temporary =
            groundTruthRoot.resolve(
                "." +
                        HimTransactionalGroundTruthPublicationContractV1
                            .CURRENT_POINTER_FILE_NAME +
                        ".publishing"
            )

        require(
            !temporary.exists()
        ) {
            "Stale Ground-Truth current-pointer publication temporary exists: " +
                    temporary.absolutePath
        }

        try {
            Files.write(
                temporary.toPath(),
                serializeCurrent(
                    pointer
                ),
            )

            try {
                Files.move(
                    temporary.toPath(),
                    current.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (
                _: AtomicMoveNotSupportedException
            ) {
                throw IllegalStateException(
                    "Atomic publication of Ground-Truth current pointer " +
                            "is not supported by this filesystem."
                )
            }
        } finally {
            Files.deleteIfExists(
                temporary.toPath()
            )
        }
    }

    private fun writeExact(
        file: File,
        bytes: ByteArray,
    ) {
        require(
            !file.exists()
        ) {
            "Ground-Truth staging artifact already exists: " +
                    file.absolutePath
        }

        Files.write(
            file.toPath(),
            bytes,
        )
    }

    private fun moveDirectoryAtomically(
        source: File,
        target: File,
    ) {
        require(
            !target.exists()
        ) {
            "Ground-Truth release already exists: " +
                    target.absolutePath
        }

        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (
            _: AtomicMoveNotSupportedException
        ) {
            throw IllegalStateException(
                "Atomic Ground-Truth release publication is not " +
                        "supported by this filesystem."
            )
        }
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