package de.shopme.tools.knowledge.him.sources.ciqual

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

class CiqualHimSourceValidator(
    private val gson: Gson =
        GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create()
) {

    fun validate(
        artifactFile: File
    ): CiqualHimSourceValidationResult {
        require(artifactFile.exists()) {
            "CIQUAL HIM source artifact does not exist: ${artifactFile.absolutePath}"
        }
        require(artifactFile.isFile) {
            "CIQUAL HIM source artifact is not a regular file: ${artifactFile.absolutePath}"
        }
        require(artifactFile.length() > 0L) {
            "CIQUAL HIM source artifact is empty: ${artifactFile.absolutePath}"
        }

        val compressedSha256 =
            sha256(artifactFile)

        val contentDigest =
            MessageDigest.getInstance("SHA-256")

        val artifact =
            readArtifact(
                artifactFile = artifactFile,
                contentDigest = contentDigest
            )

        val contentSha256 =
            contentDigest.digest().toHex()

        require(artifact.foods.size == EXPECTED_FOODS) {
            "Unexpected CIQUAL food count: ${artifact.foods.size}."
        }
        require(artifact.taxonomy.size == EXPECTED_TAXONOMY) {
            "Unexpected CIQUAL taxonomy count: ${artifact.taxonomy.size}."
        }
        require(artifact.constituents.size == EXPECTED_CONSTITUENTS) {
            "Unexpected CIQUAL constituent count: ${artifact.constituents.size}."
        }
        require(artifact.sources.size == EXPECTED_SOURCES) {
            "Unexpected CIQUAL source count: ${artifact.sources.size}."
        }

        requireSorted(
            actual = artifact.foods.map(CiqualFood::alimCode),
            description = "foods by alimCode"
        )
        requireSorted(
            actual = artifact.constituents.map(CiqualConstituent::constCode),
            description = "constituents by constCode"
        )
        requireSorted(
            actual = artifact.sources.map(CiqualSource::sourceCode),
            description = "sources by sourceCode"
        )

        val taxonomyKeys =
            artifact.taxonomy.map {
                TaxonomyKey(
                    groupCode = it.groupCode,
                    subgroupCode = it.subgroupCode,
                    subSubgroupCode = it.subSubgroupCode
                )
            }

        require(
            taxonomyKeys ==
                    taxonomyKeys.sortedWith(TAXONOMY_KEY_COMPARATOR)
        ) {
            "CIQUAL taxonomy is not deterministically sorted."
        }

        val uniqueFoodCodes =
            requireUnique(
                values = artifact.foods.map(CiqualFood::alimCode),
                description = "alimCode"
            )

        val uniqueConstCodes =
            requireUnique(
                values = artifact.constituents.map(CiqualConstituent::constCode),
                description = "constCode"
            )

        val uniqueSourceCodes =
            requireUnique(
                values = artifact.sources.map(CiqualSource::sourceCode),
                description = "sourceCode"
            )

        requireUnique(
            values = taxonomyKeys,
            description = "taxonomy tuple"
        )

        val knownConstCodes =
            artifact.constituents
                .mapTo(HashSet()) {
                    it.constCode
                }

        val knownSourceCodes =
            artifact.sources
                .mapTo(HashSet()) {
                    it.sourceCode
                }

        val knownTaxonomyKeys =
            taxonomyKeys.toHashSet()

        val compositionKeys =
            HashSet<CompositionKey>()

        var compositionCount =
            0

        artifact.foods.forEach { food ->
            requireSorted(
                actual = food.compositions.map(CiqualComposition::constCode),
                description = "compositions for alimCode '${food.alimCode}'"
            )

            val foodTaxonomyKey =
                TaxonomyKey(
                    groupCode = food.groupCode,
                    subgroupCode = food.subgroupCode,
                    subSubgroupCode = food.subSubgroupCode
                )

            require(
                foodTaxonomyKey in knownTaxonomyKeys ||
                        foodTaxonomyKey == KNOWN_MISSING_TAXONOMY_KEY
            ) {
                "Unexpected CIQUAL food taxonomy orphan: $foodTaxonomyKey."
            }

            food.compositions.forEach { composition ->
                compositionCount++

                require(composition.constCode in knownConstCodes) {
                    "Composition references unknown constCode '${composition.constCode}'."
                }

                if (composition.sourceCode.missingAttributeValue == null) {
                    require(
                        composition.sourceCode.lexicalValue in knownSourceCodes
                    ) {
                        "Composition references unknown sourceCode " +
                                "'${composition.sourceCode.lexicalValue}'."
                    }
                }

                require(
                    compositionKeys.add(
                        CompositionKey(
                            alimCode = food.alimCode,
                            constCode = composition.constCode
                        )
                    )
                ) {
                    "Duplicate CIQUAL (alimCode, constCode) pair: " +
                            "'${food.alimCode}'/'${composition.constCode}'."
                }
            }
        }

        require(compositionCount == EXPECTED_COMPOSITIONS) {
            "Unexpected CIQUAL composition count: $compositionCount."
        }

        require(RETAINED_UNREFERENCED_TAXONOMY_KEY in knownTaxonomyKeys) {
            "Expected unreferenced CIQUAL taxonomy tuple is missing: " +
                    "$RETAINED_UNREFERENCED_TAXONOMY_KEY."
        }

        require(KNOWN_MISSING_TAXONOMY_KEY !in knownTaxonomyKeys) {
            "Known missing CIQUAL taxonomy tuple was synthesized: " +
                    "$KNOWN_MISSING_TAXONOMY_KEY."
        }

        require(
            artifact.foods.any { food ->
                food.compositions.any {
                    it.teneurLexical == "traces"
                }
            }
        ) {
            "Expected source lexical teneur value 'traces' was not retained."
        }

        require(
            artifact.foods.any { food ->
                food.compositions.any {
                    it.teneurLexical.startsWith("< ")
                }
            }
        ) {
            "Expected source lexical teneur value with '< ' prefix was not retained."
        }

        val missingAttributeValues =
            collectMissingAttributeValues(artifact)

        require(missingAttributeValues.isNotEmpty()) {
            "CIQUAL source missingness was not retained."
        }
        require(missingAttributeValues.size >= 2) {
            "Expected at least two distinct CIQUAL missing attribute values, " +
                    "found $missingAttributeValues."
        }

        require(hasDuplicateInfoodsCode(artifact)) {
            "Expected duplicate CIQUAL INFOODS codes were not retained."
        }

        require(hasDuplicateCitationAcrossSourceCodes(artifact)) {
            "Expected duplicate CIQUAL citations across source codes were not retained."
        }

        return CiqualHimSourceValidationResult(
            foodCount = artifact.foods.size,
            taxonomyCount = artifact.taxonomy.size,
            constituentCount = artifact.constituents.size,
            sourceCount = artifact.sources.size,
            compositionCount = compositionCount,
            uniqueFoodCodeCount = uniqueFoodCodes,
            uniqueConstCodeCount = uniqueConstCodes,
            uniqueSourceCodeCount = uniqueSourceCodes,
            sizeBytes = artifactFile.length(),
            compressedSha256 = compressedSha256,
            contentSha256 = contentSha256
        )
    }

    private fun readArtifact(
        artifactFile: File,
        contentDigest: MessageDigest
    ): CiqualHimSourceArtifact {
        val decoder =
            StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)

        return DigestInputStream(
            GZIPInputStream(
                BufferedInputStream(
                    artifactFile.inputStream()
                )
            ),
            contentDigest
        ).use { digestInput ->
            InputStreamReader(
                digestInput,
                decoder
            ).use { reader ->
                JsonReader(reader).use { jsonReader ->
                    jsonReader.isLenient = false

                    val artifact: CiqualHimSourceArtifact =
                        requireNotNull(
                            gson.fromJson(
                                jsonReader,
                                CiqualHimSourceArtifact::class.java
                            )
                        ) {
                            "CIQUAL HIM source JSON deserialized to null."
                        }

                    require(jsonReader.peek() == JsonToken.END_DOCUMENT) {
                        "CIQUAL HIM source JSON contains trailing content."
                    }

                    artifact
                }
            }
        }
    }

    private fun collectMissingAttributeValues(
        artifact: CiqualHimSourceArtifact
    ): Set<String> =
        buildSet {
            artifact.foods.forEach { food ->
                food.scientificName.missingAttributeValue?.let(::add)

                food.compositions.forEach { composition ->
                    composition.minimum.missingAttributeValue?.let(::add)
                    composition.maximum.missingAttributeValue?.let(::add)
                    composition.confidenceCode.missingAttributeValue?.let(::add)
                    composition.sourceCode.missingAttributeValue?.let(::add)
                }
            }

            artifact.constituents.forEach {
                it.infoodsCode.missingAttributeValue?.let(::add)
            }

            artifact.sources.forEach {
                it.citation.missingAttributeValue?.let(::add)
            }
        }

    private fun hasDuplicateInfoodsCode(
        artifact: CiqualHimSourceArtifact
    ): Boolean =
        artifact.constituents
            .filter {
                it.infoodsCode.missingAttributeValue == null
            }
            .groupingBy {
                it.infoodsCode.lexicalValue
            }
            .eachCount()
            .any {
                it.value > 1
            }

    private fun hasDuplicateCitationAcrossSourceCodes(
        artifact: CiqualHimSourceArtifact
    ): Boolean =
        artifact.sources
            .filter {
                it.citation.missingAttributeValue == null
            }
            .groupingBy {
                it.citation.lexicalValue
            }
            .eachCount()
            .any {
                it.value > 1
            }

    private fun <T> requireUnique(
        values: List<T>,
        description: String
    ): Int {
        val uniqueValues =
            values.toHashSet()

        require(uniqueValues.size == values.size) {
            "Duplicate CIQUAL $description values found."
        }

        return uniqueValues.size
    }

    private fun requireSorted(
        actual: List<String>,
        description: String
    ) {
        require(actual == actual.sorted()) {
            "CIQUAL $description are not deterministically sorted."
        }
    }

    private fun sha256(
        file: File
    ): String {
        val digest =
            MessageDigest.getInstance("SHA-256")

        file.inputStream()
            .buffered()
            .use { input ->
                val buffer =
                    ByteArray(DEFAULT_BUFFER_SIZE)

                while (true) {
                    val read =
                        input.read(buffer)

                    if (read < 0) {
                        break
                    }

                    digest.update(
                        buffer,
                        0,
                        read
                    )
                }
            }

        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") {
            "%02x".format(it)
        }

    private data class TaxonomyKey(
        val groupCode: String,
        val subgroupCode: String,
        val subSubgroupCode: String
    )

    private data class CompositionKey(
        val alimCode: String,
        val constCode: String
    )

    private companion object {
        const val EXPECTED_FOODS = 3484
        const val EXPECTED_TAXONOMY = 138
        const val EXPECTED_CONSTITUENTS = 74
        const val EXPECTED_SOURCES = 1978
        const val EXPECTED_COMPOSITIONS = 257816

        val KNOWN_MISSING_TAXONOMY_KEY =
            TaxonomyKey(
                groupCode = "00",
                subgroupCode = "0000",
                subSubgroupCode = "000000"
            )

        val RETAINED_UNREFERENCED_TAXONOMY_KEY =
            TaxonomyKey(
                groupCode = "04",
                subgroupCode = "0411",
                subSubgroupCode = "000000"
            )

        val TAXONOMY_KEY_COMPARATOR =
            compareBy(
                TaxonomyKey::groupCode,
                TaxonomyKey::subgroupCode,
                TaxonomyKey::subSubgroupCode
            )
    }
}

data class CiqualHimSourceValidationResult(
    val foodCount: Int,
    val taxonomyCount: Int,
    val constituentCount: Int,
    val sourceCount: Int,
    val compositionCount: Int,
    val uniqueFoodCodeCount: Int,
    val uniqueConstCodeCount: Int,
    val uniqueSourceCodeCount: Int,
    val sizeBytes: Long,
    val compressedSha256: String,
    val contentSha256: String
) {

    fun renderReport(
        deterministicContent: Boolean,
        deterministicCompressedArtifact: Boolean
    ): String =
        buildString {
            appendLine("CIQUAL HIM SOURCE VALIDATION")
            appendLine("============================")
            appendLine()
            appendLine("Foods              : $foodCount")
            appendLine("Taxonomy           : $taxonomyCount")
            appendLine("Constituents       : $constituentCount")
            appendLine("Sources            : $sourceCount")
            appendLine("Compositions       : $compositionCount")
            appendLine()
            appendLine("Unique food codes  : $uniqueFoodCodeCount")
            appendLine("Unique const codes : $uniqueConstCodeCount")
            appendLine("Unique source codes: $uniqueSourceCodeCount")
            appendLine()
            appendLine("Known taxonomy miss:")
            appendLine("00/0000/000000")
            appendLine()
            appendLine("Unreferenced taxonomy tuple retained:")
            appendLine("04/0411/000000")
            appendLine()
            appendLine("Size:")
            appendLine("$sizeBytes bytes")
            appendLine()
            appendLine("Compressed SHA-256:")
            appendLine(compressedSha256)
            appendLine()
            appendLine("Content SHA-256:")
            appendLine(contentSha256)
            appendLine()
            appendLine("Deterministic content:")
            appendLine(deterministicContent)
            appendLine()
            appendLine("Deterministic compressed artifact:")
            appendLine(deterministicCompressedArtifact)
            appendLine()
            appendLine("============================")
        }
}
