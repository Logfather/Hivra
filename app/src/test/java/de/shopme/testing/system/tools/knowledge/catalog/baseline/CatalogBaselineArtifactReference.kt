package de.shopme.testing.system.tools.knowledge.catalog.baseline

data class CatalogBaselineArtifactReference(
    val relativePath: String,
    val byteCount: Long,
    val sha256: String
) {

    init {
        require(relativePath.isNotBlank())
        require(!relativePath.startsWith("/")) {
            "Baseline artifact paths must be repository-relative."
        }

        require(byteCount > 0L)

        require(
            SHA_256_REGEX.matches(sha256)
        ) {
            "sha256 must contain exactly 64 lowercase hexadecimal characters."
        }
    }

    private companion object {
        val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}