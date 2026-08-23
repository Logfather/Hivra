package de.shopme.tools.knowledge.him.canonical.family

import java.security.SecureRandom

class HimEntityIdGenerator(
    private val secureRandom: SecureRandom = SecureRandom(),
) {

    fun generate(assignedIds: MutableSet<HimEntityId>): HimEntityId {
        while (true) {
            val value =
                buildString(HimEntityIdLength) {
                    repeat(HimEntityIdLength) {
                        append(ALPHABET[secureRandom.nextInt(ALPHABET.length)])
                    }
                }

            val candidate = HimEntityId(value)
            if (assignedIds.add(candidate)) {
                return candidate
            }
        }
    }

    private companion object {
        const val HimEntityIdLength = 6
        const val ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }
}
