package de.shopme.tools.knowledge.ki_candidates.partition

import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class KnowledgeCandidatePartitioner(
    val partitionCount: Int =
        DEFAULT_PARTITION_COUNT
) {

    init {
        require(partitionCount > 0) {
            "partitionCount must be greater than zero."
        }

        require(
            partitionCount and
                    (partitionCount - 1) ==
                    0
        ) {
            "partitionCount must be a power of two: $partitionCount"
        }
    }

    fun partitionIndex(
        candidate: CanonicalKnowledgeCandidate
    ): Int =
        partitionIndex(
            partitionKey(
                candidate
            )
        )

    fun partitionIndex(
        canonicalId: String
    ): Int {
        val normalizedKey =
            canonicalId
                .trim()
                .lowercase()

        require(normalizedKey.isNotEmpty()) {
            "Partition key must not be blank."
        }

        val digest =
            MessageDigest
                .getInstance(HASH_ALGORITHM)
                .digest(
                    normalizedKey.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

        val hash =
            ByteBuffer
                .wrap(
                    digest,
                    0,
                    Int.SIZE_BYTES
                )
                .int

        return hash and
                (partitionCount - 1)
    }

    private fun partitionKey(
        candidate: CanonicalKnowledgeCandidate
    ): String {
        val canonicalId =
            candidate.canonicalId
                .trim()

        if (canonicalId.isNotEmpty()) {
            return canonicalId
        }

        /*
         * Legacy- und Testkandidaten können vor dem Merge noch keine
         * canonicalId besitzen. Für die Partitionierung wird deshalb
         * ein stabiler Identitätsschlüssel aus den verfügbaren
         * Kandidatenfeldern erzeugt.
         *
         * Die Kandidatendaten selbst werden dabei nicht verändert.
         */
        val fallbackKey =
            buildString {
                append(FALLBACK_PREFIX)
                append('|')

                append(
                    candidate.aliases
                        .asSequence()
                        .map { alias ->
                            alias.trim().lowercase()
                        }
                        .filter { alias ->
                            alias.isNotEmpty()
                        }
                        .distinct()
                        .sorted()
                        .joinToString(
                            separator = "|"
                        )
                )

                append('|')

                append(
                    candidate.matchAliases
                        .asSequence()
                        .map { alias ->
                            alias.trim().lowercase()
                        }
                        .filter { alias ->
                            alias.isNotEmpty()
                        }
                        .distinct()
                        .sorted()
                        .joinToString(
                            separator = "|"
                        )
                )

                append('|')

                append(
                    candidate.dimensions
                        .asSequence()
                        .map { dimension ->
                            dimension.dimension.name
                        }
                        .distinct()
                        .sorted()
                        .joinToString(
                            separator = "|"
                        )
                )
            }

        require(
            fallbackKey !=
                    EMPTY_FALLBACK_KEY
        ) {
            "Candidate has neither canonicalId nor usable identity data."
        }

        return fallbackKey
    }

    companion object {

        const val DEFAULT_PARTITION_COUNT =
            256

        private const val HASH_ALGORITHM =
            "SHA-256"

        private const val FALLBACK_PREFIX =
            "candidate"

        private const val EMPTY_FALLBACK_KEY =
            "candidate|||"
    }
}