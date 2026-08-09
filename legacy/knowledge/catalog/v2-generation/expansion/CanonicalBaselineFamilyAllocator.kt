package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

import de.shopme.testing.system.tools.knowledge.catalog.expansion.target
.CanonicalDerivedProductFamilyTarget

class CanonicalBaselineFamilyAllocator {

    fun allocate(
        categoryBaselineEntryCount: Int,
        families:
        List<CanonicalDerivedProductFamilyTarget>
    ): Map<String, Int> {
        require(categoryBaselineEntryCount >= 0)
        require(families.isNotEmpty())

        val sourceTargetEntryCount =
            families.sumOf {
                it.sourceAllocatedTargetEntryCount
            }

        require(sourceTargetEntryCount > 0)

        require(
            categoryBaselineEntryCount <=
                    sourceTargetEntryCount
        ) {
            "Category baseline count exceeds source family target."
        }

        if (categoryBaselineEntryCount == 0) {
            return families
                .associate {
                    it.familyKey to 0
                }
                .toSortedMap()
        }

        val rawAllocations =
            families.map { family ->
                val numerator =
                    categoryBaselineEntryCount.toLong() *
                            family.sourceAllocatedTargetEntryCount
                                .toLong()

                RawAllocation(
                    familyKey =
                        family.familyKey,

                    maximumAllocation =
                        family.sourceAllocatedTargetEntryCount,

                    baseAllocation =
                        (
                                numerator /
                                        sourceTargetEntryCount.toLong()
                                ).toInt(),

                    remainder =
                        numerator %
                                sourceTargetEntryCount.toLong()
                )
            }

        val baseAllocatedCount =
            rawAllocations.sumOf {
                it.baseAllocation
            }

        var remainingCount =
            categoryBaselineEntryCount -
                    baseAllocatedCount

        require(remainingCount >= 0)

        val result =
            rawAllocations.associate {
                it.familyKey to
                        it.baseAllocation
            }.toMutableMap()

        val orderedRecipients =
            rawAllocations.sortedWith(
                compareByDescending<RawAllocation> {
                    it.remainder
                }.thenBy {
                    it.familyKey
                }
            )

        while (remainingCount > 0) {
            var progress = false

            orderedRecipients.forEach { allocation ->
                if (remainingCount == 0) {
                    return@forEach
                }

                val current =
                    requireNotNull(
                        result[allocation.familyKey]
                    )

                if (
                    current <
                    allocation.maximumAllocation
                ) {
                    result[allocation.familyKey] =
                        current + 1

                    remainingCount -= 1
                    progress = true
                }
            }

            require(progress) {
                "Could not allocate complete category baseline."
            }
        }

        val sortedResult =
            result.toSortedMap()

        require(
            sortedResult.values.sum() ==
                    categoryBaselineEntryCount
        )

        require(
            families.all { family ->
                requireNotNull(
                    sortedResult[family.familyKey]
                ) <=
                        family.sourceAllocatedTargetEntryCount
            }
        )

        return sortedResult
    }

    private data class RawAllocation(
        val familyKey: String,
        val maximumAllocation: Int,
        val baseAllocation: Int,
        val remainder: Long
    )
}