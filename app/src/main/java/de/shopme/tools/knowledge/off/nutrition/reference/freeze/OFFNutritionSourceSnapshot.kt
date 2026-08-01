package de.shopme.tools.knowledge.off.nutrition.reference.freeze

data class OFFNutritionSourceSnapshot(
    val version: Int,
    val status: OFFNutritionSourceSnapshotStatus,
    val source: String,
    val sourceVersion: String,
    val sourceAggregateFile: String,
    val frozenAggregateFile: String,
    val sourceFileSizeBytes: Long,
    val frozenFileSizeBytes: Long,
    val sourceSha256: String,
    val frozenSha256: String,
    val aggregateEntryCount: Long,
    val nutritionValidationApproved: Boolean,
    val nutritionConflictPolicyVersion: Int,
    val nutritionConflictPolicyApproved: Boolean
) {

    init {
        require(version > 0) {
            "OFF Nutrition source snapshot version must be positive."
        }

        require(
            status ==
                    OFFNutritionSourceSnapshotStatus.APPROVED
        ) {
            "Frozen OFF Nutrition source snapshot must be approved."
        }

        require(source.isNotBlank()) {
            "OFF Nutrition snapshot source must not be blank."
        }

        require(sourceVersion.isNotBlank()) {
            "OFF Nutrition snapshot sourceVersion must not be blank."
        }

        require(sourceAggregateFile.isNotBlank()) {
            "OFF Nutrition snapshot source aggregate path must not be blank."
        }

        require(frozenAggregateFile.isNotBlank()) {
            "OFF Nutrition snapshot frozen aggregate path must not be blank."
        }

        require(sourceFileSizeBytes > 0L) {
            "OFF Nutrition source aggregate file must not be empty."
        }

        require(frozenFileSizeBytes > 0L) {
            "Frozen OFF Nutrition aggregate file must not be empty."
        }

        require(
            sourceFileSizeBytes ==
                    frozenFileSizeBytes
        ) {
            "Source and frozen OFF Nutrition files must have identical size."
        }

        require(
            SHA_256_REGEX.matches(
                sourceSha256
            )
        ) {
            "OFF Nutrition source SHA-256 must contain 64 lowercase " +
                    "hexadecimal characters."
        }

        require(
            SHA_256_REGEX.matches(
                frozenSha256
            )
        ) {
            "Frozen OFF Nutrition SHA-256 must contain 64 lowercase " +
                    "hexadecimal characters."
        }

        require(
            sourceSha256 ==
                    frozenSha256
        ) {
            "Source and frozen OFF Nutrition SHA-256 must be identical."
        }

        require(aggregateEntryCount > 0L) {
            "Frozen OFF Nutrition aggregate entry count must be positive."
        }

        require(nutritionValidationApproved) {
            "OFF Nutrition source may only be frozen after successful " +
                    "resulting Nutrition validation."
        }

        require(nutritionConflictPolicyVersion > 0) {
            "Nutrition conflict policy version must be positive."
        }

        require(nutritionConflictPolicyApproved) {
            "OFF Nutrition source may only be frozen after approval by " +
                    "the Nutrition conflict policy."
        }
    }

    companion object {

        const val CURRENT_VERSION =
            1

        const val SOURCE =
            "open_food_facts"

        const val SOURCE_VERSION =
            "1"

        private val SHA_256_REGEX =
            Regex(
                "^[0-9a-f]{64}$"
            )
    }
}