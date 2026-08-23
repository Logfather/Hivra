package de.shopme.tools.knowledge.him.training.corpus

import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference

object HimTrainingExampleLeakageValidatorV1 {
    private val forbiddenInputFieldNames = setOf(
        "classification",
        "target",
        "targetEntityId",
        "validationReference",
        "promotionReference",
        "mutationReference",
        "groundTruthReleaseReference",
        "promotedEntityId",
        "newEntityId",
        "promotionType",
        "mutationType",
    )

    fun validateModelInput(input: Any) {
        require(input is HimTrainingInputV1) {
            "HIM model input must be the dedicated target-free HimTrainingInputV1 type."
        }
        val actualFields = HimTrainingInputV1::class.java.declaredFields.map { it.name }.toSet()
        require(actualFields.intersect(forbiddenInputFieldNames).isEmpty()) {
            "HIM training input contains an answer or infrastructure field."
        }
    }
}

object HimTrainingExampleValidatorV1 {
    fun validate(example: HimTrainingExampleV1) {
        require(example.taskType == HimTrainingTaskTypeV1.FOOD_IDENTITY_CLASSIFICATION) {
            "Unsupported HIM training task type."
        }
        HimTrainingExampleLeakageValidatorV1.validateModelInput(example.input)
        validateTarget(example.target)
        validateProvenance(example)
        val expected = HimTrainingExampleIdentityV1.example(
            taskType = example.taskType,
            input = example.input,
            target = example.target,
            provenance = example.provenance,
        )
        require(example.exampleReference == expected) {
            "Training example reference is not the deterministic identity of its content."
        }
    }

    private fun validateTarget(target: HimTrainingTargetV1) {
        when (target) {
            is HimTrainingTargetV1.ExistingCanonical -> Unit
            is HimTrainingTargetV1.Identity -> Unit
            is HimTrainingTargetV1.Variant -> validateScope(target.scope)
            is HimTrainingTargetV1.Alias -> validateScope(target.equivalentEntity)
            is HimTrainingTargetV1.NewCanonical -> {
                // Deliberately no Entity ID is required or accepted by this target shape.
                require(target.proposedCanonicalName == null || target.proposedCanonicalName.isNotBlank())
            }
        }
    }

    private fun validateScope(scope: HimFamilyEntityReference) {
        require(scope.canonicalId.value.isNotBlank())
        if (scope is HimFamilyEntityReference.Identity) {
            require(scope.identityId.value.isNotBlank())
        }
    }

    private fun validateProvenance(example: HimTrainingExampleV1) {
        val provenance = example.provenance
        val inputEvidence = example.input.evidence.map { it.reference }.toSet()
        require(inputEvidence.all { it in provenance.sourceEvidenceReferences.toSet() }) {
            "Every evidence item exposed to the model must remain traceable in provenance."
        }
        val expectedType = when (example.target) {
            is HimTrainingTargetV1.ExistingCanonical -> HimEntityType.CANONICAL
            is HimTrainingTargetV1.Identity -> HimEntityType.IDENTITY
            is HimTrainingTargetV1.Variant -> HimEntityType.VARIANT
            is HimTrainingTargetV1.Alias -> HimEntityType.ALIAS
            is HimTrainingTargetV1.NewCanonical -> null
        }
        require(provenance.promotedEntityType == null || expectedType == null || provenance.promotedEntityType == expectedType) {
            "Promoted Entity type is inconsistent with the semantic target."
        }
    }
}
