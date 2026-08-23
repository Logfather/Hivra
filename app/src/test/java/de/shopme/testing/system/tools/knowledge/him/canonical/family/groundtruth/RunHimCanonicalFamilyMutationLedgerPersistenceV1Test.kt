package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerEntry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthMutationType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimValidationReference
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RunHimCanonicalFamilyMutationLedgerPersistenceV1Test {

    @Test
    fun `mutation ledger persists reloads and replays deterministically`() {
        val persistence =
            HimCanonicalFamilyMutationLedgerPersistenceV1()

        val first =
            variantEntry(
                mutationDigit = "1",
                validationDigit = "2",
                entityId = "AbCd12",
                authorityBefore = "a",
                authorityAfter = "b",
                registryBefore = "c",
                registryAfter = "d",
            )

        val second =
            variantEntry(
                mutationDigit = "3",
                validationDigit = "4",
                entityId = "EfGh34",
                authorityBefore = "b",
                authorityAfter = "e",
                registryBefore = "d",
                registryAfter = "f",
            )

        val initial =
            persistence.emptyLedger()

        val afterFirst =
            persistence.addEntry(
                ledger = initial,
                entry = first,
            )

        val combined =
            persistence.addEntry(
                ledger = afterFirst,
                entry = second,
            )

        assertEquals(
            HimCanonicalFamilyMutationLedgerContractV1.SCHEMA_VERSION,
            combined.schemaVersion,
        )

        assertEquals(
            2,
            combined.entries.size,
        )

        assertEquals(
            combined.entries
                .map { it.mutationReference.value }
                .sorted(),
            combined.entries
                .map { it.mutationReference.value },
        )

        val serialized =
            persistence.serialize(combined)

        val temporaryDirectory =
            Files.createTempDirectory(
                "him-mutation-ledger-v1-"
            ).toFile()

        try {
            val ledgerFile =
                temporaryDirectory.resolve(
                    "canonical-family-mutation-ledger.v1.json"
                )

            persistence.write(
                file = ledgerFile,
                ledger = combined,
            )

            assertTrue(
                ledgerFile.isFile
            )

            assertArrayEquals(
                serialized,
                ledgerFile.readBytes(),
            )

            val reloaded =
                persistence.read(ledgerFile)

            assertEquals(
                combined,
                reloaded,
            )

            assertArrayEquals(
                serialized,
                persistence.serialize(reloaded),
            )

            val replayed =
                persistence.addEntry(
                    ledger = reloaded,
                    entry = first,
                )

            assertEquals(
                reloaded,
                replayed,
            )

            assertArrayEquals(
                serialized,
                persistence.serialize(replayed),
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    @Test
    fun `same mutation reference cannot represent different mutation`() {
        val persistence =
            HimCanonicalFamilyMutationLedgerPersistenceV1()

        val first =
            variantEntry(
                mutationDigit = "5",
                validationDigit = "6",
                entityId = "IjKl56",
                authorityBefore = "1",
                authorityAfter = "2",
                registryBefore = "3",
                registryAfter = "4",
            )

        val conflicting =
            first.copy(
                newEntityId =
                    HimEntityId("MnOp78")
            )

        val ledger =
            persistence.addEntry(
                ledger =
                    persistence.emptyLedger(),
                entry =
                    first,
            )

        val failure =
            runCatching {
                persistence.addEntry(
                    ledger =
                        ledger,
                    entry =
                        conflicting,
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )

        assertEquals(
            "Mutation reference collision between different ledger entries.",
            failure?.message,
        )
    }

    @Test
    fun `same validation cannot produce second different mutation`() {
        val persistence =
            HimCanonicalFamilyMutationLedgerPersistenceV1()

        val first =
            variantEntry(
                mutationDigit = "7",
                validationDigit = "8",
                entityId = "QrSt90",
                authorityBefore = "5",
                authorityAfter = "6",
                registryBefore = "7",
                registryAfter = "8",
            )

        val conflicting =
            variantEntry(
                mutationDigit = "9",
                validationDigit = "8",
                entityId = "UvWx12",
                authorityBefore = "5",
                authorityAfter = "9",
                registryBefore = "7",
                registryAfter = "a",
            )

        val ledger =
            persistence.addEntry(
                ledger =
                    persistence.emptyLedger(),
                entry =
                    first,
            )

        val failure =
            runCatching {
                persistence.addEntry(
                    ledger =
                        ledger,
                    entry =
                        conflicting,
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )

        assertEquals(
            "Validation reference already has a different mutation.",
            failure?.message,
        )
    }

    @Test
    fun `missing ledger file reads as deterministic empty ledger`() {
        val persistence =
            HimCanonicalFamilyMutationLedgerPersistenceV1()

        val temporaryDirectory =
            Files.createTempDirectory(
                "him-empty-mutation-ledger-v1-"
            ).toFile()

        try {
            val missing =
                temporaryDirectory.resolve(
                    "missing.json"
                )

            val ledger =
                persistence.read(missing)

            assertEquals(
                HimCanonicalFamilyMutationLedgerContractV1.SCHEMA_VERSION,
                ledger.schemaVersion,
            )

            assertTrue(
                ledger.entries.isEmpty()
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    @Test
    fun `ledger rejects non deterministic ordering`() {
        val persistence =
            HimCanonicalFamilyMutationLedgerPersistenceV1()

        val first =
            variantEntry(
                mutationDigit = "b",
                validationDigit = "c",
                entityId = "YzAb34",
                authorityBefore = "b",
                authorityAfter = "c",
                registryBefore = "d",
                registryAfter = "e",
            )

        val second =
            variantEntry(
                mutationDigit = "a",
                validationDigit = "d",
                entityId = "CdEf56",
                authorityBefore = "c",
                authorityAfter = "d",
                registryBefore = "e",
                registryAfter = "f",
            )

        val invalid =
            de.shopme.tools.knowledge.him.canonical.family.groundtruth
                .HimCanonicalFamilyMutationLedger(
                    schemaVersion =
                        HimCanonicalFamilyMutationLedgerContractV1.SCHEMA_VERSION,
                    entries =
                        listOf(
                            first,
                            second,
                        ),
                )

        val failure =
            runCatching {
                persistence.serialize(invalid)
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    private fun variantEntry(
        mutationDigit: String,
        validationDigit: String,
        entityId: String,
        authorityBefore: String,
        authorityAfter: String,
        registryBefore: String,
        registryAfter: String,
    ) =
        HimCanonicalFamilyMutationLedgerEntry(
            mutationReference =
                HimMutationReference(
                    "mutation:v1:" +
                            mutationDigit.repeat(64)
                ),
            validationReference =
                HimValidationReference(
                    "validation:v1:" +
                            validationDigit.repeat(64)
                ),
            mutationType =
                HimGroundTruthMutationType.ADD_VARIANT,
            entityType =
                HimEntityType.VARIANT,
            newEntityId =
                HimEntityId(entityId),
            parent =
                HimFamilyEntityReference.Canonical(
                    canonicalId =
                        HimEntityId("OzlByp")
                ),
            canonicalFamilyAuthoritySha256Before =
                HimSha256(
                    authorityBefore.repeat(64)
                ),
            canonicalFamilyAuthoritySha256After =
                HimSha256(
                    authorityAfter.repeat(64)
                ),
            entityIdRegistrySha256Before =
                HimSha256(
                    registryBefore.repeat(64)
                ),
            entityIdRegistrySha256After =
                HimSha256(
                    registryAfter.repeat(64)
                ),
            contractVersion =
                "HIM_CANONICAL_FAMILY_CHILD_MUTATION_V1",
        )
}