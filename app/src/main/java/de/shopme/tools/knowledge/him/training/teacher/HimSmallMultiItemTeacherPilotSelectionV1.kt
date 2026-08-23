package de.shopme.tools.knowledge.him.training.teacher

import de.shopme.tools.knowledge.him.training.scaling.HimTeacherGroundTruthWorkItemV1

object HimSmallMultiItemTeacherPilotContractV1 {
    const val VERSION = "HIM_SMALL_MULTI_ITEM_TEACHER_PAID_PILOT_V1"
    const val ITEM_COUNT = 3
}

/** Technical-only selection; it does not inspect or predict semantic outputs. */
object HimSmallMultiItemTeacherPilotSelectionV1 {
    fun select(
        workItems: List<HimTeacherGroundTruthWorkItemV1>,
        excludedWorkItemReferences: Set<String>,
    ): List<HimTeacherGroundTruthWorkItemV1> {
        val eligible = workItems
            .filterNot { it.reference in excludedWorkItemReferences }
            .sortedBy { it.reference }
        require(eligible.map { it.reference }.distinct().size == eligible.size) {
            "F3.8e work-item references are not unique"
        }
        val selected = linkedMapOf<String, HimTeacherGroundTruthWorkItemV1>()
        eligible.groupBy { it.partition }
            .toSortedMap(compareBy { it.ordinal })
            .values
            .map { items -> items.first() }
            .forEach { selected[it.reference] = it }
        eligible.forEach { if (selected.size < HimSmallMultiItemTeacherPilotContractV1.ITEM_COUNT) selected[it.reference] = it }
        require(selected.size >= HimSmallMultiItemTeacherPilotContractV1.ITEM_COUNT) {
            "F3.8e worklist has fewer than three eligible work items"
        }
        return selected.values.take(HimSmallMultiItemTeacherPilotContractV1.ITEM_COUNT)
    }
}
