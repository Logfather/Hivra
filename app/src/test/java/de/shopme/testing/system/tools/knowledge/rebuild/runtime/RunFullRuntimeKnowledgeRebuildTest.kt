package de.shopme.testing.system.tools.knowledge.rebuild.runtime

import de.shopme.tools.knowledge.rebuild.runtime.runner.RunFullRuntimeKnowledgeRebuild
import org.junit.Test

class RunFullRuntimeKnowledgeRebuildTest {

    @Test
    fun runFullRuntimeKnowledgeRebuildWithoutPublishing() {

        RunFullRuntimeKnowledgeRebuild.main(
            arrayOf("--no-publish")
        )
    }
}