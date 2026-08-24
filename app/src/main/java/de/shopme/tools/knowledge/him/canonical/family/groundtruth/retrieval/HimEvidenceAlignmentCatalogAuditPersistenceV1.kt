package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

object HimEvidenceAlignmentCatalogAuditPersistenceV1 {
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .serializeNulls()
        .create()

    fun writeMission(file: File, plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1) {
        plan.validate()
        require(plan.missionDigest == logicalDigest(plan.copy(missionDigest = "")))
        writeNoOverwrite(file, gson.toJson(plan).toByteArray(StandardCharsets.UTF_8))
    }

    fun readMission(file: File): HimEvidenceAlignmentCatalogAuditMissionPlanV1 =
        gson.fromJson(file.readText(StandardCharsets.UTF_8), HimEvidenceAlignmentCatalogAuditMissionPlanV1::class.java)
            .also { it.validate() }

    fun writeShard(
        file: File,
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        result: HimEvidenceAlignmentCatalogAuditShardResultV1,
    ) {
        result.validateAgainst(plan)
        writeNoOverwrite(file, gson.toJson(result).toByteArray(StandardCharsets.UTF_8))
    }

    fun readShard(
        file: File,
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
    ): HimEvidenceAlignmentCatalogAuditShardResultV1 = gson
        .fromJson(file.readText(StandardCharsets.UTF_8), HimEvidenceAlignmentCatalogAuditShardResultV1::class.java)
        .also { it.validateAgainst(plan) }

    fun writeAggregate(
        file: File,
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        aggregate: HimEvidenceAlignmentCatalogAuditAggregateV1,
    ) {
        aggregate.validateAgainst(plan)
        writeNoOverwrite(file, gson.toJson(aggregate).toByteArray(StandardCharsets.UTF_8))
    }

    fun readAggregate(
        file: File,
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
    ): HimEvidenceAlignmentCatalogAuditAggregateV1 = gson
        .fromJson(file.readText(StandardCharsets.UTF_8), HimEvidenceAlignmentCatalogAuditAggregateV1::class.java)
        .also { it.validateAgainst(plan) }

    fun logicalDigest(plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1): String =
        sha256(gson.toJson(plan.copy(missionDigest = "")))

    fun logicalDigest(result: HimEvidenceAlignmentCatalogAuditShardResultV1): String =
        sha256(gson.toJson(result.copy(logicalDigest = "")))

    fun logicalDigest(aggregate: HimEvidenceAlignmentCatalogAuditAggregateV1): String =
        sha256(gson.toJson(aggregate.copy(logicalDigest = "")))

    fun shardBindingDigest(
        plan: HimEvidenceAlignmentCatalogAuditMissionPlanV1,
        shard: HimEvidenceAlignmentCatalogAuditShardV1,
    ): String = sha256(
        gson.toJson(
            listOf(
                plan.missionDigest,
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

    private fun sha256(value: ByteArray): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value)
        .joinToString("") { "%02x".format(it) }

    private fun writeNoOverwrite(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        if (file.exists()) {
            require(file.readBytes().contentEquals(bytes)) {
                "Refusing to overwrite an existing catalog-audit artifact."
            }
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
