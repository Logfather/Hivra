package de.shopme.tools.knowledge.him.canonical.family

data class HimEntityId(
    val value: String,
) {

    init {
        require(value.matches(VALID_VALUE)) {
            "HIM entity ID must contain exactly six ASCII alphanumeric characters."
        }
    }

    private companion object {
        val VALID_VALUE = Regex("[0-9A-Za-z]{6}")
    }
}

enum class HimEntityType {
    CANONICAL,
    IDENTITY,
    VARIANT,
    ALIAS,
}

enum class HimLifecycleStatus {
    ACTIVE,
    DEPRECATED,
}
