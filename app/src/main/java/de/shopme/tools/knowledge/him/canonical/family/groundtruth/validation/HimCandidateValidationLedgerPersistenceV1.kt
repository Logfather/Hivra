package de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object HimCandidateValidationLedgerPersistenceV1 {

    const val ROOT =
        "data/knowledge/him/validation"

    const val MASTER_ROOT =
        "$ROOT/master"

    const val LEDGER_PATH =
        "$MASTER_ROOT/candidate-validation-ledger.v1.json"

    private val gson: Gson =
        GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create()

    fun addValidation(
        ledger: HimCandidateValidationLedgerV1,
        validation: HimCandidateValidationRecord,
    ): HimCandidateValidationLedgerV1 {
        val existingByCandidate =
            ledger.validations.find {
                it.candidateReference ==
                        validation.candidateReference
            }

        if (existingByCandidate != null) {
            require(
                existingByCandidate == validation
            ) {
                "Immutable Candidate validation already exists for candidate: " +
                        validation.candidateReference.value
            }

            return ledger
        }

        val existingByReference =
            ledger.validations.find {
                it.validationReference ==
                        validation.validationReference
            }

        require(
            existingByReference == null
        ) {
            "Validation reference collision."
        }

        val validations =
            (
                    ledger.validations +
                            validation
                    )
                .sortedBy {
                    it.validationReference.value
                }

        return HimCandidateValidationLedgerV1(
            validations = validations
        )
    }

    fun serialize(
        ledger: HimCandidateValidationLedgerV1,
    ): ByteArray =
        (
                gson.toJson(ledger) +
                        "\n"
                )
            .toByteArray(Charsets.UTF_8)

    fun read(
        file: File,
    ): HimCandidateValidationLedgerV1 =
        if (!file.isFile) {
            HimCandidateValidationLedgerV1()
        } else {
            requireNotNull(
                gson.fromJson(
                    file.readText(),
                    HimCandidateValidationLedgerV1::class.java,
                )
            )
        }

    fun write(
        file: File,
        ledger: HimCandidateValidationLedgerV1,
    ) {
        atomicWrite(
            file = file,
            bytes = serialize(ledger),
        )
    }

    fun defaultLedgerFile(
        root: File,
    ): File =
        root.resolve(LEDGER_PATH)

    private fun atomicWrite(
        file: File,
        bytes: ByteArray,
    ) {
        val parent =
            requireNotNull(file.parentFile)

        require(
            parent.exists() ||
                    parent.mkdirs()
        )

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

            Files.move(
                temporary,
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            Files.deleteIfExists(
                temporary
            )
        }
    }
}