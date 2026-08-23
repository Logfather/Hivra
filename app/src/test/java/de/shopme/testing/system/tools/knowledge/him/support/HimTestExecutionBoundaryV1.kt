package de.shopme.testing.system.tools.knowledge.him.support

import org.junit.Assume.assumeTrue

/**
 * Explicit opt-in boundaries for tests that can touch source artifacts, local immutable
 * master inputs such as the Canonical Catalog, or paid transports.
 */
object HimTestExecutionBoundaryV1 {
    const val SOURCE_INTEGRATION_PROPERTY = "him.sourceIntegration.enabled"
    const val PAID_NETWORK_PROPERTY = "him.paidNetwork.enabled"
    const val PAID_NETWORK_CONFIRMATION_PROPERTY = "him.paidNetwork.confirmation"
    const val PAID_NETWORK_CONFIRMATION = "AUTHORIZED_PAID_INFERENCE"

    internal fun sourceIntegrationEnabled(property: String?): Boolean = property == "true"

    internal fun paidNetworkEnabled(enabledProperty: String?, confirmationProperty: String?): Boolean =
        enabledProperty == "true" && confirmationProperty == PAID_NETWORK_CONFIRMATION

    fun requireSourceIntegrationEnabled() {
        assumeTrue(sourceIntegrationEnabled(System.getProperty(SOURCE_INTEGRATION_PROPERTY)))
    }

    fun requirePaidNetworkEnabled() {
        assumeTrue(
            paidNetworkEnabled(
                System.getProperty(PAID_NETWORK_PROPERTY),
                System.getProperty(PAID_NETWORK_CONFIRMATION_PROPERTY),
            ),
        )
    }
}
