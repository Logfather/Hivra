package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

data class CanonicalConcreteFamilyAxisValueIndex(
    val entries:
    Map<String, List<String>>
) {

    init {
        require(entries.isNotEmpty())

        entries.forEach { (identityKey, values) ->
            require(identityKey.isNotBlank())

            require(
                values ==
                        values
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .sorted()
            )
        }
    }

    fun contains(
        familyKey: String,
        axis: String
    ): Boolean =
        identityKey(
            familyKey = familyKey,
            axis = axis
        ) in entries

    fun values(
        familyKey: String,
        axis: String
    ): List<String>? =
        entries[
            identityKey(
                familyKey = familyKey,
                axis = axis
            )
        ]

    companion object {

        fun identityKey(
            familyKey: String,
            axis: String
        ): String =
            "${familyKey.trim()}::${axis.trim()}"
    }
}