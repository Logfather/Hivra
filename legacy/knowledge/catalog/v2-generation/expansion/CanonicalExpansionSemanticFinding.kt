package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

data class CanonicalExpansionSemanticFinding(
    val ruleType: CanonicalExpansionSemanticRuleType,

    val decision: CanonicalExpansionSemanticDecision,

    val ruleKey: String,

    val message: String
) {

    init {
        require(RULE_KEY_REGEX.matches(ruleKey)) {
            "Invalid semantic rule key: '$ruleKey'."
        }

        require(message.isNotBlank())
        require(message == message.trim())
    }

    private companion object {
        val RULE_KEY_REGEX =
            Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
    }
}