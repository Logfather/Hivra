package de.shopme.tools.knowledge.him.sources.glycemicindex

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

class GlycemicIndexHimSourceValidator(
    private val gson: Gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create(),
) {

    fun validate(artifactFile: File, rawSourceFile: File): GlycemicIndexHimSourceValidationResult {
        require(rawSourceFile.isFile && rawSourceFile.length() == RAW_SIZE)
        require(sha256(rawSourceFile) == RAW_SHA256)
        require(artifactFile.isFile && artifactFile.length() > 0L)

        val compressedSha256 = sha256(artifactFile)
        val contentDigest = MessageDigest.getInstance("SHA-256")
        val artifact = readArtifact(artifactFile, contentDigest)
        val contentSha256 = contentDigest.digest().toHex()

        require(artifact.measurements.size == 2_091)
        require(artifact.meanSummaries.size == 122)
        require(artifact.categoryNotes.size == 15)
        require(artifact.footnotes.size == 26)
        val numbers = artifact.measurements.map(GlycemicIndexMeasurement::foodNumber)
        require(numbers == (1..2_091).toList())
        require(numbers.distinct().size == 2_091)

        val majorCategories = artifact.measurements.map { it.sourceContext.majorCategory }.toSet()
        val subcategories = buildSet {
            artifact.measurements.mapNotNullTo(this) { it.sourceContext.subcategory }
            artifact.meanSummaries.mapNotNullTo(this) { it.sourceContext.subcategory }
            artifact.categoryNotes.mapNotNullTo(this) { it.sourceContext.subcategory }
        }
        require(majorCategories.size == 20) { "Major category mismatch: $majorCategories" }
        require(subcategories.size == 3) { "Subcategory mismatch: $subcategories" }
        require(artifact.measurements.count { it.sourceContext.subcategory != null } == 210) {
            "Measurement subcategory count: ${artifact.measurements.count { it.sourceContext.subcategory != null }}"
        }
        require(artifact.meanSummaries.count { it.sourceContext.subcategory != null } == 12) {
            "Mean summary subcategory count: ${artifact.meanSummaries.count { it.sourceContext.subcategory != null }}"
        }
        require(artifact.measurements.all { it.sourceContext.majorCategory.isNotEmpty() && it.sourceContext.deeperHeading == null })
        require(artifact.meanSummaries.all { it.sourceContext.majorCategory.isNotEmpty() && it.sourceContext.deeperHeading == null })
        require(artifact.categoryNotes.all { it.sourceContext.majorCategory.isNotEmpty() && it.sourceContext.deeperHeading == null })

        artifact.measurements.forEach(::validateMeasurementFields)
        artifact.meanSummaries.forEach { validateField(it.lexicalText) }
        artifact.categoryNotes.forEach { validateField(it.lexicalText) }
        artifact.footnotes.forEach { validateField(it.identifier); validateField(it.lexicalText) }

        requireStatus(artifact.measurements.map { it.country }, 2_085, 6, 0, "country")
        requireStatus(artifact.measurements.map { it.year }, 2_084, 0, 7, "year")
        requireStatus(artifact.measurements.map { it.gi }, 2_091, 0, 0, "gi")
        requireStatus(artifact.measurements.map { it.sem }, 2_091, 0, 0, "sem")
        requireStatus(artifact.measurements.map { it.gl }, 2_090, 0, 1, "gl")
        requireStatus(artifact.measurements.map { it.subjects }, 2_091, 0, 0, "subjects")
        requireStatus(artifact.measurements.map { it.availableCarbohydrate }, 2_091, 0, 0, "availableCarbohydrate")
        requireStatus(artifact.measurements.map { it.testPortion }, 1_760, 325, 6, "testPortion")
        requireStatus(artifact.measurements.map { it.referenceFoodTime }, 2_091, 0, 0, "referenceFoodTime")
        requireStatus(artifact.measurements.map { it.timepoints }, 2_090, 1, 0, "timepoints")
        requireStatus(artifact.measurements.map { it.sampleCollection }, 2_091, 0, 0, "sampleCollection")
        requireStatus(artifact.measurements.map { it.analysisMethod }, 2_036, 55, 0, "analysisMethod")
        requireStatus(artifact.measurements.map { it.referenceCode }, 2_090, 0, 1, "referenceCode")

        val unresolved = artifact.measurements.sumOf { measurement ->
            measurementFields(measurement).count { it.status == GlycemicSourceFieldStatus.UNRESOLVED }
        }
        require(unresolved == 15)
        require(artifact.measurements.single { it.foodNumber == 1_841 }.gl.status == GlycemicSourceFieldStatus.UNRESOLVED)

        val gi = artifact.measurements.map { requireNotNull(it.gi.lexicalValue).toDouble() }
        val sem = artifact.measurements.map { requireNotNull(it.sem.lexicalValue).toDouble() }
        require(gi.size == 2_091 && sem.size == 2_091)
        require(gi.min() == 10.0 && gi.max() == 119.0)
        require(gi.count { it > 100.0 } == 17)
        require(gi.count { it == 100.0 } == 1)
        require(gi.none { it < 0.0 })

        require(artifact.meanSummaries.all { it.lexicalText.status == GlycemicSourceFieldStatus.PRESENT })
        require(artifact.categoryNotes.all { it.lexicalText.status == GlycemicSourceFieldStatus.PRESENT })
        require(artifact.categoryNotes.map { it.lexicalText.lexicalValue }.distinct().size == 8)
        require(artifact.categoryNotes.map { it.sourceContext.majorCategory }.distinct().size == 13)
        require(artifact.footnotes.map { it.identifier.lexicalValue } == (1..26).map(Int::toString))
        require(artifact.footnotes.all { it.pageNumber == 139 })

        val uoReferences = artifact.measurements.mapNotNull { it.referenceCode.lexicalValue }.filter { UO.matches(it) }
        require(uoReferences.size == 1_061) { "UO reference count=${uoReferences.size}, domain=${uoReferences.toSet()}" }
        require(uoReferences.toSet() == setOf("UO5", "UO7", "UO10", "UO14", "UO15", "UO18", "UO20", "UO23", "UO26")) {
            "UO reference domain=${uoReferences.toSet()}"
        }

        return GlycemicIndexHimSourceValidationResult(
            measurementCount = artifact.measurements.size,
            meanSummaryCount = artifact.meanSummaries.size,
            categoryNoteCount = artifact.categoryNotes.size,
            footnoteCount = artifact.footnotes.size,
            majorCategoryCount = majorCategories.size,
            subcategoryCount = subcategories.size,
            unresolvedCount = unresolved,
            giMinimum = gi.min(),
            giMaximum = gi.max(),
            giAbove100Count = gi.count { it > 100.0 },
            sizeBytes = artifactFile.length(),
            compressedSha256 = compressedSha256,
            contentSha256 = contentSha256,
        )
    }

    private fun readArtifact(file: File, contentDigest: MessageDigest): GlycemicIndexHimSourceArtifact {
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return DigestInputStream(GZIPInputStream(BufferedInputStream(file.inputStream())), contentDigest).use { input ->
            InputStreamReader(input, decoder).use { reader ->
                JsonReader(reader).use { jsonReader ->
                    jsonReader.isLenient = false
                    val artifact: GlycemicIndexHimSourceArtifact = requireNotNull(
                        gson.fromJson(jsonReader, GlycemicIndexHimSourceArtifact::class.java)
                    )
                    require(jsonReader.peek() == JsonToken.END_DOCUMENT)
                    artifact
                }
            }
        }
    }

    private fun validateMeasurementFields(measurement: GlycemicIndexMeasurement) {
        require(measurement.foodNumber in 1..2_091 && measurement.pageNumber in 2..138)
        measurementFields(measurement).forEach(::validateField)
    }

    private fun measurementFields(value: GlycemicIndexMeasurement) = listOf(
        value.foodItem, value.country, value.year, value.gi, value.sem, value.gl,
        value.subjects, value.availableCarbohydrate, value.testPortion,
        value.referenceFoodTime, value.timepoints, value.sampleCollection,
        value.analysisMethod, value.referenceCode,
    )

    private fun validateField(field: GlycemicSourceField) {
        when (field.status) {
            GlycemicSourceFieldStatus.PRESENT,
            GlycemicSourceFieldStatus.SOURCE_MISSING -> require(!field.lexicalValue.isNullOrEmpty())
            GlycemicSourceFieldStatus.UNRESOLVED -> require(field.lexicalValue == null)
        }
    }

    private fun requireStatus(
        fields: List<GlycemicSourceField>,
        present: Int,
        sourceMissing: Int,
        unresolved: Int,
        name: String,
    ) {
        val actual = fields.groupingBy(GlycemicSourceField::status).eachCount()
        require(actual[GlycemicSourceFieldStatus.PRESENT] ?: 0 == present) { "$name PRESENT status mismatch: $actual" }
        require(actual[GlycemicSourceFieldStatus.SOURCE_MISSING] ?: 0 == sourceMissing) { "$name SOURCE_MISSING status mismatch: $actual" }
        require(actual[GlycemicSourceFieldStatus.UNRESOLVED] ?: 0 == unresolved) { "$name UNRESOLVED status mismatch: $actual" }
    }

    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).toHex()
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private companion object {
        const val RAW_SIZE = 2_216_220L
        const val RAW_SHA256 = "13a2f85fb781bc8d8f9ce194a88f944b885377d59d8d340902ad1bd0d610e2d8"
        val UO = Regex("UO\\d+")
    }
}
