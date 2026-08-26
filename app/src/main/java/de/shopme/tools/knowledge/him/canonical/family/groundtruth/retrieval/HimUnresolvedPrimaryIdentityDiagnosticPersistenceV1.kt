package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1 {
    private val gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()

    fun writeMission(file: File, mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1) {
        mission.validate()
        writeJson(file, gson.toJson(mission))
    }

    fun readMission(file: File): HimUnresolvedPrimaryIdentityDiagnosticMissionV1 =
        gson.fromJson(file.readText(Charsets.UTF_8), HimUnresolvedPrimaryIdentityDiagnosticMissionV1::class.java).also { it.validate() }

    fun writeShard(
        file: File,
        mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
        result: HimUnresolvedPrimaryIdentityDiagnosticShardResultV1,
    ) {
        result.validateAgainst(mission)
        writeJson(file, gson.toJson(result))
    }

    fun readShard(
        file: File,
        mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
    ): HimUnresolvedPrimaryIdentityDiagnosticShardResultV1 =
        gson.fromJson(file.readText(Charsets.UTF_8), HimUnresolvedPrimaryIdentityDiagnosticShardResultV1::class.java).also {
            it.validateAgainst(mission)
        }

    fun writeAggregate(
        jsonFile: File,
        textFile: File,
        mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
        aggregate: HimUnresolvedPrimaryIdentityDiagnosticAggregateV1,
    ) {
        aggregate.validateAgainst(mission)
        val json = gson.toJson(aggregate)
        val text = text(aggregate)
        require(!jsonFile.exists() || jsonFile.readText(Charsets.UTF_8) == json) { "AGGREGATE_ALREADY_EXISTS" }
        require(!textFile.exists() || textFile.readText(Charsets.UTF_8) == text) { "AGGREGATE_TEXT_ALREADY_EXISTS" }
        if (jsonFile.exists() && textFile.exists()) return
        val jsonTemp = temporary(jsonFile)
        val textTemp = temporary(textFile)
        try {
            jsonTemp.writeText(json, Charsets.UTF_8)
            textTemp.writeText(text, Charsets.UTF_8)
            if (!jsonFile.exists()) moveNoOverwrite(jsonTemp, jsonFile) else jsonTemp.delete()
            if (!textFile.exists()) moveNoOverwrite(textTemp, textFile) else textTemp.delete()
        } finally {
            jsonTemp.delete()
            textTemp.delete()
        }
    }

    fun readAggregate(
        file: File,
        mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
    ): HimUnresolvedPrimaryIdentityDiagnosticAggregateV1 =
        gson.fromJson(file.readText(Charsets.UTF_8), HimUnresolvedPrimaryIdentityDiagnosticAggregateV1::class.java).also {
            it.validateAgainst(mission)
        }

    fun logicalDigest(mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1): String =
        sha256(gson.toJson(mission.copy(logicalDigest = "")))

    fun logicalDigest(result: HimUnresolvedPrimaryIdentityDiagnosticShardResultV1): String =
        sha256(gson.toJson(result.copy(logicalDigest = "")))

    fun logicalDigest(aggregate: HimUnresolvedPrimaryIdentityDiagnosticAggregateV1): String =
        sha256(gson.toJson(aggregate.copy(logicalDigest = "")))

    fun bindingDigest(binding: HimUnresolvedPrimaryIdentityDiagnosticInputBindingV1): String =
        sha256(gson.toJson(binding.copy(bindingDigest = "")))

    fun sha256(value: String): String = sha256(value.toByteArray(Charsets.UTF_8))

    fun sha256(file: File): String {
        FileInputStream(file).use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
            return digest.digest().toHex()
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun writeJson(file: File, json: String) {
        if (file.exists()) {
            require(file.readText(Charsets.UTF_8) == json) { "DIAGNOSTIC_RESULT_ALREADY_EXISTS" }
            return
        }
        val temp = temporary(file)
        try {
            temp.writeText(json, Charsets.UTF_8)
            moveNoOverwrite(temp, file)
        } finally {
            temp.delete()
        }
    }

    private fun moveNoOverwrite(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        Files.move(
            source.toPath(),
            destination.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    private fun temporary(file: File): File {
        file.parentFile?.mkdirs()
        return Files.createTempFile(file.parentFile?.toPath(), file.name, ".tmp").toFile()
    }

    private fun text(aggregate: HimUnresolvedPrimaryIdentityDiagnosticAggregateV1): String = buildString {
        appendLine("contract=HIM_UNRESOLVED_PRIMARY_IDENTITY_DIAGNOSTIC_V1")
        appendLine("state=${aggregate.state.name}")
        appendLine("missionDigest=${aggregate.missionDigest}")
        appendLine("unresolvedOccurrences=${aggregate.counters.unresolvedOccurrences}")
        appendLine("canonicalTargets=${aggregate.counters.canonicalTargets}")
        appendLine("uniqueEvidenceReferences=${aggregate.counters.uniqueEvidenceReferences}")
        appendLine("exactFetches=${aggregate.counters.exactFetches}")
        appendLine("recordsLoaded=${aggregate.counters.recordsLoaded}")
        appendLine("technicalErrors=${aggregate.counters.technicalErrors}")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
