package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1 {
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .create()

    fun writeMission(file: File, mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1) {
        mission.validate()
        writeNoOverwrite(file, gson.toJson(mission).toByteArray(StandardCharsets.UTF_8))
    }

    fun readMission(file: File): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1 =
        gson.fromJson(file.readText(StandardCharsets.UTF_8), HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1::class.java)
            .also { it.validate() }

    fun writeShard(
        file: File,
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        result: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1,
    ) {
        result.validateAgainst(mission)
        writeNoOverwrite(file, gson.toJson(result).toByteArray(StandardCharsets.UTF_8))
    }

    fun readShard(
        file: File,
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1 =
        gson.fromJson(file.readText(StandardCharsets.UTF_8), HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1::class.java)
            .also { it.validateAgainst(mission) }

    fun writeAggregate(
        file: File,
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        aggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1,
    ) {
        aggregate.validateAgainst(mission)
        writeNoOverwrite(file, gson.toJson(aggregate).toByteArray(StandardCharsets.UTF_8))
    }

    fun readAggregate(
        file: File,
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1 =
        gson.fromJson(file.readText(StandardCharsets.UTF_8), HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1::class.java)
            .also { it.validateAgainst(mission) }

    fun logicalDigest(mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1): String =
        sha256(gson.toJson(mission.copy(logicalDigest = "")))

    fun logicalDigest(result: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1): String =
        sha256(gson.toJson(result.copy(logicalDigest = "")))

    fun logicalDigest(aggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1): String =
        sha256(gson.toJson(aggregate.copy(logicalDigest = "")))

    fun shardBindingDigest(
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        shard: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardPlanV1,
    ): String = sha256(
        gson.toJson(
            listOf(
                mission.logicalDigest,
                shard.shardId,
                shard.startInclusive,
                shard.endExclusive,
                shard.canonicalCount,
                shard.canonicalOrderDigest,
                shard.canonicalEntityIds,
            ),
        ),
    )

    fun sha256(value: String): String = sha256(value.toByteArray(StandardCharsets.UTF_8))

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(value: ByteArray): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value)
        .joinToString("") { "%02x".format(it) }

    private fun writeNoOverwrite(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        if (file.exists()) {
            require(file.readBytes().contentEquals(bytes)) { "RESULT_ALREADY_EXISTS" }
            return
        }
        val parent = requireNotNull(file.parentFile)
        val temporary = Files.createTempFile(parent.toPath(), file.name, ".pending")
        try {
            Files.write(temporary, bytes)
            try {
                Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, file.toPath())
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
