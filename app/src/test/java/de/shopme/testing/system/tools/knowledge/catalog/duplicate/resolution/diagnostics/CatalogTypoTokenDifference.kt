package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics

data class CatalogTypoTokenDifference(
    val tokenIndex: Int,

    val sourceToken: String,
    val targetToken: String,

    val normalizedSourceToken: String,
    val normalizedTargetToken: String,

    val sourceLength: Int,
    val targetLength: Int,
    val lengthDifference: Int,

    val editDistance: Int,

    val germanOrthographyEquivalent: Boolean,
    val typTypeEquivalent: Boolean,
    val sauceSosseEquivalent: Boolean
) {

    init {
        require(tokenIndex >= 0)

        require(sourceToken.isNotBlank())
        require(targetToken.isNotBlank())

        require(normalizedSourceToken.isNotBlank())
        require(normalizedTargetToken.isNotBlank())

        require(sourceLength >= 0)
        require(targetLength >= 0)
        require(lengthDifference >= 0)
        require(editDistance >= 0)

        require(sourceLength == normalizedSourceToken.length)
        require(targetLength == normalizedTargetToken.length)

        require(
            lengthDifference ==
                    kotlin.math.abs(
                        sourceLength - targetLength
                    )
        )
    }
}