package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1Factory
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationV1
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RunHimTransactionalGroundTruthPublicationV1Test {

    @Test
    fun `validated release publishes atomically and becomes current`() {
        val root =
            temporaryRoot()

        try {
            val publisher =
                HimTransactionalGroundTruthPublicationV1()

            assertNull(
                publisher.readCurrent(root)
            )

            val input =
                input("A")

            var validations = 0

            val result =
                publisher.publish(
                    projectRoot = root,
                    input = input,
                ) { releaseDirectory ->
                    validations += 1
                    assertCompleteRelease(
                        releaseDirectory,
                        input,
                    )
                }

            assertTrue(result.created)
            assertEquals(2, validations)

            assertTrue(
                result.releaseDirectory.isDirectory
            )

            val current =
                requireNotNull(
                    publisher.readCurrent(root)
                )

            assertEquals(
                result.releaseReference,
                current.releaseReference,
            )

            assertEquals(
                result.currentPointer,
                current,
            )

            assertCompleteRelease(
                result.releaseDirectory,
                input,
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `failed staging validation does not change current pointer`() {
        val root =
            temporaryRoot()

        try {
            val publisher =
                HimTransactionalGroundTruthPublicationV1()

            val first =
                publisher.publish(
                    projectRoot = root,
                    input = input("A"),
                ) {
                    // valid
                }

            val currentBefore =
                requireNotNull(
                    publisher.readCurrent(root)
                )

            val failure =
                runCatching {
                    publisher.publish(
                        projectRoot = root,
                        input = input("B"),
                    ) {
                        throw IllegalArgumentException(
                            "Synthetic validation failure."
                        )
                    }
                }.exceptionOrNull()

            assertTrue(
                failure is IllegalArgumentException
            )

            val currentAfter =
                requireNotNull(
                    publisher.readCurrent(root)
                )

            assertEquals(
                currentBefore,
                currentAfter,
            )

            assertEquals(
                first.releaseReference,
                currentAfter.releaseReference,
            )

            val releases =
                releasesRoot(root)

            assertFalse(
                releases
                    .listFiles()
                    .orEmpty()
                    .any {
                        it.name.endsWith(
                            HimTransactionalGroundTruthPublicationContractV1
                                .STAGING_SUFFIX
                        )
                    }
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `identical publication reuses immutable release deterministically`() {
        val root =
            temporaryRoot()

        try {
            val publisher =
                HimTransactionalGroundTruthPublicationV1()

            val input =
                input("A")

            val first =
                publisher.publish(
                    projectRoot = root,
                    input = input,
                ) {}

            val releaseFilesBefore =
                snapshot(
                    first.releaseDirectory
                )

            val second =
                publisher.publish(
                    projectRoot = root,
                    input = input,
                ) {}

            assertFalse(second.created)

            assertEquals(
                first.releaseReference,
                second.releaseReference,
            )

            assertEquals(
                first.releaseDirectory,
                second.releaseDirectory,
            )

            assertEquals(
                releaseFilesBefore,
                snapshot(
                    second.releaseDirectory
                ),
            )

            assertEquals(
                second.currentPointer,
                publisher.readCurrent(root),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `different release state produces different release identity`() {
        val first =
            HimGroundTruthReleaseIdentityV1Factory.compute(
                input("A")
            )

        val second =
            HimGroundTruthReleaseIdentityV1Factory.compute(
                input("B")
            )

        assertNotEquals(
            first,
            second,
        )
    }

    @Test
    fun `stale staging directory fails closed`() {
        val root =
            temporaryRoot()

        try {
            val publisher =
                HimTransactionalGroundTruthPublicationV1()

            val input =
                input("A")

            val reference =
                HimGroundTruthReleaseIdentityV1Factory.compute(
                    input
                )

            val staging =
                releasesRoot(root)
                    .resolve(
                        reference.value
                            .removePrefix(
                                "release:v1:"
                            ) +
                                HimTransactionalGroundTruthPublicationContractV1
                                    .STAGING_SUFFIX
                    )

            assertTrue(
                staging.mkdirs()
            )

            val failure =
                runCatching {
                    publisher.publish(
                        projectRoot = root,
                        input = input,
                    ) {}
                }.exceptionOrNull()

            assertTrue(
                failure is IllegalArgumentException
            )

            assertNull(
                publisher.readCurrent(root)
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `existing immutable release with different bytes fails closed`() {
        val root =
            temporaryRoot()

        try {
            val publisher =
                HimTransactionalGroundTruthPublicationV1()

            val input =
                input("A")

            val first =
                publisher.publish(
                    projectRoot = root,
                    input = input,
                ) {}

            val authority =
                first.releaseDirectory.resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .AUTHORITY_FILE_NAME
                )

            authority.writeText(
                "tampered\n"
            )

            val currentBefore =
                requireNotNull(
                    publisher.readCurrent(root)
                )

            val failure =
                runCatching {
                    publisher.publish(
                        projectRoot = root,
                        input = input,
                    ) {}
                }.exceptionOrNull()

            assertTrue(
                failure is IllegalArgumentException
            )

            assertEquals(
                currentBefore,
                publisher.readCurrent(root),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `current pointer serialization is deterministic`() {
        val root =
            temporaryRoot()

        try {
            val publisher =
                HimTransactionalGroundTruthPublicationV1()

            val result =
                publisher.publish(
                    projectRoot = root,
                    input = input("A"),
                ) {}

            val first =
                publisher.serializeCurrent(
                    result.currentPointer
                )

            val second =
                publisher.serializeCurrent(
                    result.currentPointer
                )

            assertArrayEquals(
                first,
                second,
            )

            val currentFile =
                root
                    .resolve(
                        HimTransactionalGroundTruthPublicationContractV1
                            .GROUND_TRUTH_DIRECTORY
                    )
                    .resolve(
                        HimTransactionalGroundTruthPublicationContractV1
                            .CURRENT_POINTER_FILE_NAME
                    )

            assertArrayEquals(
                first,
                currentFile.readBytes(),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    private fun input(
        marker: String,
    ) =
        HimTransactionalGroundTruthPublicationInputV1(
            authorityBytes =
                bytes(
                    "authority-$marker"
                ),
            activeRegistryBytes =
                bytes(
                    "active-registry-$marker"
                ),
            retiredRegistryBytes =
                bytes(
                    "retired-registry-$marker"
                ),
            mutationLedgerBytes =
                bytes(
                    "mutation-ledger-$marker"
                ),
            fingerprintIndexBytes =
                bytes(
                    "fingerprint-index-$marker"
                ),
            releaseBytes =
                bytes(
                    "release-$marker"
                ),
        )

    private fun bytes(
        value: String,
    ) =
        "$value\n".toByteArray(
            Charsets.UTF_8
        )

    private fun temporaryRoot() =
        Files.createTempDirectory(
            "him-ground-truth-publication-v1-"
        ).toFile()

    private fun releasesRoot(
        root: java.io.File,
    ): java.io.File {
        val releases =
            root
                .resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .GROUND_TRUTH_DIRECTORY
                )
                .resolve(
                    HimTransactionalGroundTruthPublicationContractV1
                        .RELEASES_DIRECTORY_NAME
                )

        require(
            releases.exists() ||
                    releases.mkdirs()
        )

        return releases
    }

    private fun assertCompleteRelease(
        directory: java.io.File,
        input:
        HimTransactionalGroundTruthPublicationInputV1,
    ) {
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

        assertEquals(
            expected.keys,
            directory
                .listFiles()
                .orEmpty()
                .filter {
                    it.isFile
                }
                .map {
                    it.name
                }
                .toSet(),
        )

        expected.forEach { (name, bytes) ->
            assertArrayEquals(
                bytes,
                directory
                    .resolve(name)
                    .readBytes(),
            )
        }
    }

    private fun snapshot(
        directory: java.io.File,
    ): Map<String, List<Byte>> =
        directory
            .listFiles()
            .orEmpty()
            .filter {
                it.isFile
            }
            .associate {
                it.name to
                        it.readBytes()
                            .toList()
            }
}