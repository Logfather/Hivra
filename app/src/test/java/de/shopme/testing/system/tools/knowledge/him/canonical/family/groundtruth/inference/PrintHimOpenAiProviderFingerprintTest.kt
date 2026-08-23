package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.inference

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.provider.openai.HimOpenAiSemanticProviderConfiguration
import org.junit.Test

class PrintHimOpenAiProviderFingerprintTest {

    @Test
    fun `print current provider configuration fingerprint`() {
        val configuration = HimOpenAiSemanticProviderConfiguration()

        println(
            "CURRENT_HIM_OPENAI_PROVIDER_FINGERPRINT=" +
                    configuration.fingerprint().value
        )
    }
}