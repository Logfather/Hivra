package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File

object CanonicalSemanticPolicyTestFixtures {

    fun readPolicySet(
        projectDirectory: File
    ): CanonicalFamilyAxisSemanticPolicySet =
        CanonicalFamilyAxisSemanticPolicySetReader()
            .read(
                File(
                    projectDirectory,
                    POLICY_PATH
                )
            )

    private const val POLICY_PATH =
        "data/generated/knowledge/catalog/expansion/" +
                "canonical-family-axis-semantic-policies.json"
}