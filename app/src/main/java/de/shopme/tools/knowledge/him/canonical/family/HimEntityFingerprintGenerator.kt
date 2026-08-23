package de.shopme.tools.knowledge.him.canonical.family

import java.security.MessageDigest

class HimEntityFingerprintGenerator {

    fun canonicalInput(
        canonicalId: HimEntityId,
        identityId: HimEntityId?,
        variantIds: List<HimEntityId>,
    ): String {
        val sortedVariantIds =
            variantIds
                .map { it.value }
                .sorted()
                .joinToString(",")

        return "canonicalId=${canonicalId.value}" +
                "|identityId=${identityId?.value.orEmpty()}" +
                "|variantIds=$sortedVariantIds"
    }

    fun generate(
        canonicalId: HimEntityId,
        identityId: HimEntityId?,
        variantIds: List<HimEntityId>,
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(
                canonicalInput(
                    canonicalId = canonicalId,
                    identityId = identityId,
                    variantIds = variantIds,
                ).toByteArray(Charsets.UTF_8)
            )
            .joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
}
