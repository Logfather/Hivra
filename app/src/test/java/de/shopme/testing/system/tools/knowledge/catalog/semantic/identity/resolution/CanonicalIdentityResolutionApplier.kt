package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

import com.google.gson.JsonObject

data class CanonicalIdentityResolutionApplicationResult(
    val inputEntryCount: Int,
    val removedEntryCount: Int,
    val outputEntryCount: Int,
    val removedNormalizedKeys: List<String>,
    val entries: List<JsonObject>
)

class CanonicalIdentityResolutionApplier {

    fun apply(
        baseline: List<JsonObject>,
        regeneratedExpansion: List<JsonObject>,
        resolutions: List<CanonicalIdentityResolution>
    ): CanonicalIdentityResolutionApplicationResult {

        val input =
            buildList {
                addAll(baseline)
                addAll(regeneratedExpansion)
            }

        val removedNormalizedKeys =
            resolutions
                .filter {
                    it.decision ==
                            CanonicalIdentityResolutionDecision.MERGE
                }
                .flatMap {
                    it.merged
                }
                .map {
                    it.normalized
                }
                .toSet()

        val output =
            input
                .filter { entry ->
                    entry
                        .get("normalized")
                        .asString !in
                            removedNormalizedKeys
                }
                .sortedWith(
                    compareBy<JsonObject>(
                        {
                            it
                                .get("category")
                                .asString
                        },
                        {
                            it
                                .get("normalized")
                                .asString
                        }
                    )
                )

        require(
            output.size ==
                    input.size -
                    removedNormalizedKeys.size
        ) {
            "Canonical identity resolution arithmetic mismatch."
        }

        return CanonicalIdentityResolutionApplicationResult(
            inputEntryCount =
                input.size,

            removedEntryCount =
                removedNormalizedKeys.size,

            outputEntryCount =
                output.size,

            removedNormalizedKeys =
                removedNormalizedKeys.sorted(),

            entries =
                output
        )
    }
}