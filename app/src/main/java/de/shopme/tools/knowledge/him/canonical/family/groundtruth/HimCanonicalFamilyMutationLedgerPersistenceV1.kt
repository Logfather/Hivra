package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.io.File
import java.lang.reflect.Type
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object HimCanonicalFamilyMutationLedgerContractV1 {

    const val SCHEMA_VERSION =
        "HIM_CANONICAL_FAMILY_MUTATION_LEDGER_V1"

    const val DEFAULT_PATH =
        "data/knowledge/him/canonical-family/groundtruth/master/" +
                "canonical-family-mutation-ledger.v1.json"
}

class HimCanonicalFamilyMutationLedgerPersistenceV1(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(
                HimFamilyEntityReference::class.java,
                HimFamilyEntityReferenceDeserializer,
            )
            .create(),
) {

    fun emptyLedger(): HimCanonicalFamilyMutationLedger =
        HimCanonicalFamilyMutationLedger(
            schemaVersion =
                HimCanonicalFamilyMutationLedgerContractV1.SCHEMA_VERSION,
            entries = emptyList(),
        )

    fun addEntry(
        ledger: HimCanonicalFamilyMutationLedger,
        entry: HimCanonicalFamilyMutationLedgerEntry,
    ): HimCanonicalFamilyMutationLedger {
        validateLedger(ledger)

        val existingByReference =
            ledger.entries.find {
                it.mutationReference == entry.mutationReference
            }

        if (existingByReference != null) {
            require(existingByReference == entry) {
                "Mutation reference collision between different ledger entries."
            }

            return ledger
        }

        /*
         * A validation must not silently produce multiple different
         * mutations in the same immutable ledger.
         */
        val existingByValidation =
            ledger.entries.find {
                it.validationReference == entry.validationReference
            }

        require(existingByValidation == null) {
            "Validation reference already has a different mutation."
        }

        val result =
            HimCanonicalFamilyMutationLedger(
                schemaVersion =
                    HimCanonicalFamilyMutationLedgerContractV1.SCHEMA_VERSION,
                entries =
                    (ledger.entries + entry)
                        .sortedBy {
                            it.mutationReference.value
                        },
            )

        validateLedger(result)

        return result
    }

    fun serialize(
        ledger: HimCanonicalFamilyMutationLedger,
    ): ByteArray {
        validateLedger(ledger)

        return (
                gson.toJson(ledger) +
                        "\n"
                ).toByteArray(Charsets.UTF_8)
    }

    fun read(
        file: File,
    ): HimCanonicalFamilyMutationLedger {
        if (!file.isFile) {
            return emptyLedger()
        }

        val ledger =
            requireNotNull(
                gson.fromJson(
                    file.readText(),
                    HimCanonicalFamilyMutationLedger::class.java,
                )
            ) {
                "Mutation ledger is invalid JSON: ${file.absolutePath}"
            }

        validateLedger(ledger)

        return ledger
    }

    fun write(
        file: File,
        ledger: HimCanonicalFamilyMutationLedger,
    ) {
        atomicWrite(
            file = file,
            bytes = serialize(ledger),
        )
    }

    fun defaultLedgerFile(
        projectRoot: File,
    ): File =
        projectRoot.resolve(
            HimCanonicalFamilyMutationLedgerContractV1.DEFAULT_PATH
        )

    private fun validateLedger(
        ledger: HimCanonicalFamilyMutationLedger,
    ) {
        require(
            ledger.schemaVersion ==
                    HimCanonicalFamilyMutationLedgerContractV1.SCHEMA_VERSION
        ) {
            "Unsupported mutation ledger schema: ${ledger.schemaVersion}"
        }

        require(
            ledger.entries
                .map { it.mutationReference }
                .distinct()
                .size ==
                    ledger.entries.size
        ) {
            "Duplicate mutation references in ledger."
        }

        require(
            ledger.entries
                .map { it.validationReference }
                .distinct()
                .size ==
                    ledger.entries.size
        ) {
            "Duplicate validation references in mutation ledger."
        }

        require(
            ledger.entries.map {
                it.mutationReference.value
            } ==
                    ledger.entries.map {
                        it.mutationReference.value
                    }.sorted()
        ) {
            "Mutation ledger entries are not deterministically ordered."
        }
    }

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
            "Could not create mutation ledger directory: ${parent.absolutePath}"
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

private object HimFamilyEntityReferenceDeserializer :
    JsonDeserializer<HimFamilyEntityReference> {

    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext,
    ): HimFamilyEntityReference {
        val value = json.asJsonObject

        return when {
            value.has("identityId") ->
                context.deserialize(
                    value,
                    HimFamilyEntityReference.Identity::class.java,
                )

            value.has("canonicalId") ->
                context.deserialize(
                    value,
                    HimFamilyEntityReference.Canonical::class.java,
                )

            else ->
                throw JsonParseException(
                    "Unknown HimFamilyEntityReference representation."
                )
        }
    }
}