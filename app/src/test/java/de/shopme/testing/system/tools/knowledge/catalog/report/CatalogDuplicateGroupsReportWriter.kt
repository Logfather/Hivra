package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateCandidate
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.CatalogDuplicateGroup
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

class CatalogDuplicateGroupsReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        groups: List<CatalogDuplicateGroup>,
        outputFile: File
    ) {
        require(outputFile.name.isNotBlank()) {
            "Duplicate-groups report output file must have a name."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Duplicate-groups report output path is not a file: " +
                    outputFile.path
        }

        validateGroups(groups)

        val normalizedGroups = groups
            .map(::normalizeGroup)
            .sortedWith(GROUP_COMPARATOR)

        val report = CatalogDuplicateGroupsReport(
            version = CURRENT_VERSION,
            groupCount = normalizedGroups.size,
            duplicateEntryCount = normalizedGroups
                .sumOf { it.members.size },
            mergeAutomaticallyCount = normalizedGroups.count {
                it.recommendation.name == "MERGE_AUTOMATICALLY"
            },
            mergeAfterReviewCount = normalizedGroups.count {
                it.recommendation.name == "MERGE_AFTER_REVIEW"
            },
            reviewCount = normalizedGroups.count {
                it.recommendation.name == "REVIEW"
            },
            keepSeparateCount = normalizedGroups.count {
                it.recommendation.name == "KEEP_SEPARATE"
            },
            groups = normalizedGroups
        )

        validateReport(report)

        val outputDirectory = outputFile.absoluteFile.parentFile

        requireNotNull(outputDirectory) {
            "Duplicate-groups report output file has no parent directory: " +
                    outputFile.path
        }

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Failed to create duplicate-groups report directory: " +
                        outputDirectory.path
            }
        }

        require(outputDirectory.isDirectory) {
            "Duplicate-groups report parent is not a directory: " +
                    outputDirectory.path
        }

        val json = gson.toJson(report)
            .trimEnd() + System.lineSeparator()

        writeAtomically(
            outputFile = outputFile,
            content = json
        )
    }

    private fun normalizeGroup(
        group: CatalogDuplicateGroup
    ): CatalogDuplicateGroup =
        group.copy(
            members = group.members
                .map(::normalizeMember)
                .sortedWith(MEMBER_COMPARATOR),
            confidence = normalizeConfidence(group.confidence)
        )

    private fun normalizeMember(
        member: CatalogDuplicateCandidate
    ): CatalogDuplicateCandidate =
        member.copy(
            matchScore = normalizeConfidence(member.matchScore),
            reasons = member.reasons
                .toSortedSet(compareBy { it.name })
        )

    private fun validateGroups(
        groups: List<CatalogDuplicateGroup>
    ) {
        val duplicateGroupIds = groups
            .groupBy { it.groupId }
            .filterValues { it.size > 1 }
            .keys
            .sorted()

        require(duplicateGroupIds.isEmpty()) {
            "Duplicate-groups report contains duplicate group IDs: " +
                    duplicateGroupIds.joinToString(", ")
        }

        val duplicateSourceIndicesAcrossGroups = groups
            .flatMap { group ->
                group.members.map { member ->
                    member.sourceIndex to group.groupId
                }
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .filterValues { groupIds ->
                groupIds.distinct().size > 1
            }
            .toSortedMap()

        require(duplicateSourceIndicesAcrossGroups.isEmpty()) {
            "Catalog entries belong to multiple duplicate groups: " +
                    duplicateSourceIndicesAcrossGroups.entries.joinToString("; ") {
                            (sourceIndex, groupIds) ->
                        "$sourceIndex -> " +
                                groupIds.distinct().sorted().joinToString(", ")
                    }
        }

        groups.forEach { group ->
            validateGroup(group)
        }
    }

    private fun validateGroup(
        group: CatalogDuplicateGroup
    ) {
        require(group.groupId.isNotBlank()) {
            "Duplicate group ID must not be blank."
        }

        require(group.canonicalCandidateSourceIndex >= 0) {
            "Duplicate group '${group.groupId}' contains a negative " +
                    "canonicalCandidateSourceIndex."
        }

        require(group.canonicalCandidateName.isNotBlank()) {
            "Duplicate group '${group.groupId}' has a blank canonical name."
        }

        require(group.members.size >= MINIMUM_GROUP_MEMBER_COUNT) {
            "Duplicate group '${group.groupId}' must contain at least " +
                    "$MINIMUM_GROUP_MEMBER_COUNT members."
        }

        require(group.confidence.isFinite()) {
            "Duplicate group '${group.groupId}' has a non-finite confidence."
        }

        require(group.confidence in MINIMUM_CONFIDENCE..MAXIMUM_CONFIDENCE) {
            "Duplicate group '${group.groupId}' confidence must be between " +
                    "$MINIMUM_CONFIDENCE and $MAXIMUM_CONFIDENCE, but was " +
                    "${group.confidence}."
        }

        val duplicateMemberIndices = group.members
            .groupBy { it.sourceIndex }
            .filterValues { it.size > 1 }
            .keys
            .sorted()

        require(duplicateMemberIndices.isEmpty()) {
            "Duplicate group '${group.groupId}' contains repeated source " +
                    "indices: ${duplicateMemberIndices.joinToString(", ")}"
        }

        val canonicalMembers = group.members.filter {
            it.sourceIndex == group.canonicalCandidateSourceIndex
        }

        require(canonicalMembers.size == 1) {
            "Duplicate group '${group.groupId}' must contain exactly one " +
                    "member matching canonicalCandidateSourceIndex " +
                    "${group.canonicalCandidateSourceIndex}."
        }

        val canonicalMember = canonicalMembers.single()

        require(canonicalMember.itemName.isNotBlank()) {
            "Canonical member of duplicate group '${group.groupId}' has a " +
                    "blank item name."
        }

        group.members.forEach { member ->
            validateMember(
                groupId = group.groupId,
                member = member
            )
        }
    }

    private fun validateMember(
        groupId: String,
        member: CatalogDuplicateCandidate
    ) {
        require(member.sourceIndex >= 0) {
            "Duplicate group '$groupId' contains a member with negative " +
                    "sourceIndex."
        }

        require(member.itemName.isNotBlank()) {
            "Duplicate group '$groupId' contains a member with blank " +
                    "itemName at sourceIndex ${member.sourceIndex}."
        }

        require(member.normalizedKey.isNotBlank()) {
            "Duplicate group '$groupId' contains a member with blank " +
                    "normalizedKey at sourceIndex ${member.sourceIndex}."
        }

        require(member.matchScore.isFinite()) {
            "Duplicate group '$groupId' contains a non-finite matchScore " +
                    "at sourceIndex ${member.sourceIndex}."
        }

        require(
            member.matchScore in
                    MINIMUM_CONFIDENCE..MAXIMUM_CONFIDENCE
        ) {
            "Duplicate group '$groupId' contains matchScore " +
                    "${member.matchScore} outside the allowed range at " +
                    "sourceIndex ${member.sourceIndex}."
        }

        require(member.reasons.isNotEmpty()) {
            "Duplicate group '$groupId' member at sourceIndex " +
                    "${member.sourceIndex} must contain at least one reason."
        }
    }

    private fun validateReport(
        report: CatalogDuplicateGroupsReport
    ) {
        require(report.version > 0) {
            "Duplicate-groups report version must be greater than zero."
        }

        require(report.groupCount == report.groups.size) {
            "Duplicate-groups report groupCount must equal groups size."
        }

        require(
            report.duplicateEntryCount ==
                    report.groups.sumOf { it.members.size }
        ) {
            "Duplicate-groups report duplicateEntryCount is inconsistent."
        }

        val recommendationCount =
            report.mergeAutomaticallyCount +
                    report.mergeAfterReviewCount +
                    report.reviewCount +
                    report.keepSeparateCount

        require(recommendationCount == report.groupCount) {
            "Duplicate-group recommendation counts must sum to groupCount."
        }

        require(
            report.groups == report.groups.sortedWith(GROUP_COMPARATOR)
        ) {
            "Duplicate groups must be deterministically sorted."
        }

        report.groups.forEach { group ->
            require(
                group.members ==
                        group.members.sortedWith(MEMBER_COMPARATOR)
            ) {
                "Members of duplicate group '${group.groupId}' must be " +
                        "deterministically sorted."
            }
        }
    }

    private fun writeAtomically(
        outputFile: File,
        content: String
    ) {
        val parentDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        )

        val temporaryFile = File(
            parentDirectory,
            ".${outputFile.name}.tmp"
        )

        try {
            temporaryFile.outputStream().buffered().use { output ->
                output.write(
                    content.toByteArray(StandardCharsets.UTF_8)
                )
                output.flush()
            }

            try {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
        }
    }

    private fun normalizeConfidence(
        value: Double
    ): Double =
        String.format(
            Locale.ROOT,
            CONFIDENCE_FORMAT,
            value
        ).toDouble()

    private data class CatalogDuplicateGroupsReport(
        val version: Int,
        val groupCount: Int,
        val duplicateEntryCount: Int,
        val mergeAutomaticallyCount: Int,
        val mergeAfterReviewCount: Int,
        val reviewCount: Int,
        val keepSeparateCount: Int,
        val groups: List<CatalogDuplicateGroup>
    )

    private companion object {

        const val CURRENT_VERSION = 1
        const val MINIMUM_GROUP_MEMBER_COUNT = 2

        const val MINIMUM_CONFIDENCE = 0.0
        const val MAXIMUM_CONFIDENCE = 1.0

        const val CONFIDENCE_FORMAT = "%.6f"

        val GROUP_COMPARATOR =
            compareByDescending<CatalogDuplicateGroup> {
                it.confidence
            }
                .thenBy {
                    it.canonicalCandidateName.lowercase(Locale.ROOT)
                }
                .thenBy {
                    it.canonicalCandidateSourceIndex
                }
                .thenBy {
                    it.groupId
                }

        val MEMBER_COMPARATOR =
            compareByDescending<CatalogDuplicateCandidate> {
                it.matchScore
            }
                .thenBy {
                    it.itemName.lowercase(Locale.ROOT)
                }
                .thenBy {
                    it.normalizedKey
                }
                .thenBy {
                    it.sourceIndex
                }

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
    }
}