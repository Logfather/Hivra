package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInferenceSchema
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticInstructionPolicyV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HimSemanticInstructionPolicyV22Test {

    @Test
    fun `instruction policy v2_2 preserves canonical identity across variant states`() {
        assertEquals(
            "HIM_SEMANTIC_INFERENCE_INSTRUCTION_POLICY_V2_2",
            HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
        )

        assertEquals(
            HimSemanticInferenceSchema.INSTRUCTION_POLICY_VERSION,
            HimSemanticInstructionPolicyV2.VERSION,
        )

        val policy = HimSemanticInstructionPolicyV2.TEXT

        assertContains(
            policy,
            "Preserve canonical identity when the underlying food identity remains unchanged.",
        )

        assertContains(
            policy,
            "processing",
        )

        assertContains(
            policy,
            "preparation",
        )

        assertContains(
            policy,
            "preservation",
        )

        assertContains(
            policy,
            "presentation",
        )

        assertContains(
            policy,
            "represent such a distinction as a VARIANT within the appropriate existing semantic scope.",
        )

        assertContains(
            policy,
            "Source-specific categorization or the existence of a distinct source record does not by itself establish a new canonical identity.",
        )

        assertContains(
            policy,
            "CREATE_NEW_CANONICAL requires a genuinely distinct underlying food identity",
        )

        assertContains(
            policy,
            "must not be used solely because a source represents a processing, preparation, preservation, or presentation state as a separate category or record.",
        )
    }

    private fun assertContains(
        policy: String,
        expected: String,
    ) {
        assertTrue(
            "Expected instruction policy to contain: $expected",
            policy.contains(expected),
        )
    }
}