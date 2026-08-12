package de.shopme.tools.knowledge.rebuild.runtime.runner

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.rebuild.runtime.FullRuntimeKnowledgeRebuild

object RunFullRuntimeKnowledgeRebuild {

    @JvmStatic
    fun main(
        args: Array<String>
    ) {

        require(
            args.all {
                it == "--no-publish"
            }
        ) {
            "RunFullRuntimeKnowledgeRebuild accepts only " +
                    "the optional argument --no-publish."
        }

        require(
            args.distinct().size ==
                    args.size
        ) {
            "Duplicate arguments are not allowed."
        }

        val publish =
            "--no-publish" !in args

        val paths =
            KnowledgeBuildPaths.default()

        FullRuntimeKnowledgeRebuild()
            .rebuild(
                paths =
                    paths,
                publish =
                    publish
            )
    }
}