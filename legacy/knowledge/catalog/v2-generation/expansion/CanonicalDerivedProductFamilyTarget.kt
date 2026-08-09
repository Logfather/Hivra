package de.shopme.testing.system.tools.knowledge.catalog.expansion.target

data class CanonicalDerivedProductFamilyTarget(
    val familyKey: String,
    val category: String,
    val displayName: String,

    val sourceAllocatedTargetEntryCount: Int,
    val additionalDerivedEntryCount: Int,
    val derivedTargetEntryCount: Int,

    val requiredAxisCount: Int,
    val recommendedAxisCount: Int,
    val optionalAxisCount: Int,

    val recommendedVariantValueCoverageCount: Int,
    val crossAxisCombinationAllowedCount: Int,

    val complexityWeight: Long,

    val additionalAllocationShare: Double
) {

    init {
        require(familyKey.isNotBlank())
        require(category.isNotBlank())
        require(displayName.isNotBlank())

        require(sourceAllocatedTargetEntryCount > 0)
        require(additionalDerivedEntryCount >= 0)

        require(
            derivedTargetEntryCount ==
                    sourceAllocatedTargetEntryCount +
                    additionalDerivedEntryCount
        )

        require(requiredAxisCount >= 0)
        require(recommendedAxisCount >= 0)
        require(optionalAxisCount >= 0)

        require(recommendedVariantValueCoverageCount > 0)
        require(crossAxisCombinationAllowedCount >= 0)

        require(complexityWeight > 0L)

        require(additionalAllocationShare in 0.0..1.0)
    }
}