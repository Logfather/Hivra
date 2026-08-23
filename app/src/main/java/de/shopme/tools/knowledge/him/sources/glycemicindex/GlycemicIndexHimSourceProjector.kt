package de.shopme.tools.knowledge.him.sources.glycemicindex

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs

class GlycemicIndexHimSourceProjector {

    fun project(sourceFile: File): GlycemicIndexHimSourceArtifact {
        require(sourceFile.isFile && sourceFile.length() == EXPECTED_SIZE) {
            "Unexpected Glycemic Index raw source file: ${sourceFile.absolutePath}"
        }
        require(sha256(sourceFile) == EXPECTED_SHA256) {
            "Glycemic Index raw source SHA-256 mismatch."
        }

        return Loader.loadPDF(sourceFile).use { document ->
            require(document.numberOfPages == EXPECTED_PAGES)
            project(extract(document))
        }
    }

    private fun project(extraction: Extraction): GlycemicIndexHimSourceArtifact {
        val lines = geometryLines(extraction.glyphs)
        val majorCategories = extraction.pageTexts.getValue(1).lineSequence().map(String::trim).mapNotNull { line ->
            TOC.matchEntire(line)?.groupValues?.get(1)
        }.filter { it != "FOOTNOTES" }.toList()
        require(majorCategories.size == EXPECTED_MAJOR_CATEGORIES)

        val bodyLines = lines.filter { it.page in 2..138 && it.y in BODY_TOP..BODY_BOTTOM }
        val anchors = anchors(bodyLines)
        val majorHeadings = lines.filter { it.page in 2..138 }.mapNotNull { line ->
            majorCategories.firstOrNull { it.equals(line.text, ignoreCase = true) }?.let {
                Heading(line.page, line.y, line.minX, it, HeadingKind.MAJOR)
            }
        }
        val subordinateLines = lines.filter { line ->
            val leftText = joinGlyphs(line.glyphs.filter { it.x < FOOD_COLUMN_END })
                line.page in 2..138 && leftText.isNotBlank() &&
                line.glyphs.none { it.x >= FOOD_COLUMN_END } &&
                anchors.none { it.page == line.page && abs(it.y - line.y) <= LINE_TOLERANCE } &&
                "Bold" in line.style && majorCategories.none { it.equals(line.text, ignoreCase = true) }
        }
        val subordinateHeadings = mergeLines(subordinateLines).map {
            Heading(it.page, it.y, it.minX, it.text, HeadingKind.SUBCATEGORY)
        }
        val headings = (majorHeadings + subordinateHeadings).sortedWith(SOURCE_POSITION_COMPARATOR)
        val contexts = contexts(headings)

        val italicSummaryGroups = mergeLines(
            lines.filter { line -> line.page in 2..138 && line.glyphs.none { it.x >= FOOD_COLUMN_END } && "Italic" in line.style }
        ).filter { SUMMARY.containsMatchIn(it.text) }
        val summaryLines = lines.filter { it.page in 2..138 && SUMMARY.containsMatchIn(it.text) }.map { anchor ->
            italicSummaryGroups.firstOrNull { group -> group.page == anchor.page && anchor.y in group.physicalYs } ?: anchor
        }.distinctBy { it.page to it.y }.sortedWith(SOURCE_POSITION_COMPARATOR)
        require(summaryLines.size == EXPECTED_SUMMARIES) {
            "Unexpected mean summary count: ${summaryLines.size}; samples=${summaryLines.take(10).map { it.page to it.text }}"
        }

        val noteLines = lines.filter { it.page in 2..138 && NOTE_START.containsMatchIn(it.text) }
        require(noteLines.size == EXPECTED_NOTES)

        val excluded = buildSet {
            addAll(headings.map { it.page to quantize(it.y) })
            addAll(summaryLines.flatMap { line -> line.physicalYs.map { line.page to quantize(it) } })
            addAll(noteLines.map { it.page to quantize(it.y) })
        }
        val measurementLines = assignableLines(bodyLines, anchors)
        val linesByAnchor = measurementLines.groupBy { line ->
            anchors.filter { it.page == line.page }.minWithOrNull(compareBy<Anchor> { abs(it.y - line.y) }.thenBy { it.number })
        }

        val yearDiagnostics = mutableListOf<Triple<Int, Int, String>>()
        val measurements = anchors.map { anchor ->
            val cells = COLUMNS.associate { column -> column.name to cell(linesByAnchor[anchor].orEmpty(), column, excluded) }.toMutableMap()
            cells["foodItem"] = cells.getValue("foodItem").replaceFirst(Regex("^${anchor.number}\\s*"), "").trim()
            cells["year"] = cells.getValue("year").replace(Regex("(\\d{4}-)\\s+(\\d{2,4})"), "$1$2")
            cells["referenceCode"] = referenceCode(bodyLines, anchor).ifBlank { cells.getValue("referenceCode") }
            if (!YEAR.matches(cells.getValue("year"))) yearDiagnostics += Triple(anchor.number, anchor.page, cells.getValue("year"))
            val giSem = GI_SEM.find(cells.getValue("giSem"))

            GlycemicIndexMeasurement(
                foodNumber = anchor.number,
                pageNumber = anchor.page,
                sourceContext = contextAt(anchor.page, anchor.y, contexts),
                foodItem = sourceField("foodItem", cells.getValue("foodItem")),
                country = sourceField("country", cells.getValue("country")),
                year = sourceField("year", cells.getValue("year")),
                gi = sourceField("gi", giSem?.groupValues?.get(1).orEmpty()),
                sem = sourceField("sem", giSem?.groupValues?.get(2).orEmpty()),
                gl = sourceField("gl", cells.getValue("gl")),
                subjects = sourceField("subjects", cells.getValue("subjects")),
                availableCarbohydrate = sourceField("availableCarbohydrate", cells.getValue("availableCarbohydrate")),
                testPortion = sourceField("testPortion", cells.getValue("testPortion")),
                referenceFoodTime = sourceField("referenceFoodTime", cells.getValue("referenceFoodTime")),
                timepoints = sourceField("timepoints", cells.getValue("timepoints")),
                sampleCollection = sourceField("sampleCollection", cells.getValue("sampleCollection")),
                analysisMethod = sourceField("analysisMethod", cells.getValue("analysisMethod")),
                referenceCode = sourceField("referenceCode", cells.getValue("referenceCode")),
            )
        }
        require(yearDiagnostics.size == 7) { "Year extraction diagnostics: $yearDiagnostics" }

        val meanSummaries = summaryLines.sortedWith(SOURCE_POSITION_COMPARATOR).map { line ->
            GlycemicIndexMeanSummary(
                pageNumber = line.page,
                sourceContext = contextAt(line.page, line.y, contexts),
                lexicalText = present(line.text),
            )
        }
        val categoryNotes = noteLines.sortedWith(SOURCE_POSITION_COMPARATOR).map { line ->
            GlycemicIndexCategoryNote(
                pageNumber = line.page,
                sourceContext = contextAt(line.page, line.y, contexts),
                lexicalText = present(line.text),
            )
        }
        val footnotes = footnotes(lines.filter { it.page == 139 }).map { footnote ->
            GlycemicIndexFootnote(
                pageNumber = footnote.page,
                identifier = present(footnote.identifier),
                lexicalText = present(footnote.text),
            )
        }

        return GlycemicIndexHimSourceArtifact(
            measurements = measurements,
            meanSummaries = meanSummaries,
            categoryNotes = categoryNotes,
            footnotes = footnotes,
        )
    }

    private fun extract(document: PDDocument): Extraction {
        val glyphs = mutableListOf<Glyph>()
        val pageTexts = sortedMapOf<Int, String>()
        val stripper = object : PDFTextStripper() {
            init {
                sortByPosition = true
            }

            override fun processTextPosition(text: TextPosition) {
                glyphs += Glyph(
                    page = currentPageNo,
                    x = text.xDirAdj,
                    y = text.yDirAdj,
                    width = text.widthDirAdj,
                    height = text.heightDir,
                    font = text.font?.name.orEmpty(),
                    text = text.unicode,
                )
                super.processTextPosition(text)
            }
        }
        (1..document.numberOfPages).forEach { page ->
            stripper.startPage = page
            stripper.endPage = page
            pageTexts[page] = stripper.getText(document).replace("\r\n", "\n").replace('\r', '\n')
        }
        return Extraction(glyphs, pageTexts)
    }

    private fun anchors(lines: List<GeoLine>): List<Anchor> {
        val result = lines.mapNotNull { line ->
            joinGlyphs(line.glyphs.filter { it.x in 45f..<70f }).toIntOrNull()
                ?.takeIf { it in 1..EXPECTED_MEASUREMENTS }
                ?.let { Anchor(it, line.page, line.y) }
        }.distinctBy { it.number }.sortedBy { it.number }
        require(result.map { it.number } == (1..EXPECTED_MEASUREMENTS).toList())
        return result
    }

    private fun assignableLines(
        bodyLines: List<GeoLine>,
        anchors: List<Anchor>,
    ): List<GeoLine> {
        val anchorsByPage = anchors.groupBy { it.page }
        return bodyLines.filter { line ->
            val pageAnchors = anchorsByPage[line.page].orEmpty()
            val first = pageAnchors.minOfOrNull { it.y }
            val last = pageAnchors.maxOfOrNull { it.y }
            first != null && last != null &&
                line.y >= first - LINE_TOLERANCE && line.y <= last + 13f &&
                (line.y >= 108f || pageAnchors.any { abs(it.y - line.y) <= LINE_TOLERANCE }) &&
                "nominal GL" !in line.text &&
                !NOTE_START.containsMatchIn(line.text)
        }
    }

    private fun contexts(headings: List<Heading>): List<ContextEvent> {
        var major: String? = null
        var subcategory: String? = null
        return headings.map { heading ->
            when (heading.kind) {
                HeadingKind.MAJOR -> {
                    major = heading.text
                    subcategory = null
                }
                HeadingKind.SUBCATEGORY -> subcategory = heading.text
            }
            ContextEvent(heading.page, heading.y, requireNotNull(major), subcategory)
        }
    }

    private fun contextAt(page: Int, y: Float, events: List<ContextEvent>): GlycemicIndexSourceContext {
        val event = requireNotNull(events.lastOrNull { it.page < page || it.page == page && it.y <= y })
        return GlycemicIndexSourceContext(
            majorCategory = event.majorCategory,
            subcategory = event.subcategory,
            deeperHeading = null,
        )
    }

    private fun footnotes(lines: List<GeoLine>): List<Footnote> {
        val relevant = lines.filter { it.y > 55f && it.y < 410f && it.minX > 40f && it.text.isNotBlank() }
        val starts = relevant.mapIndexedNotNull { index, line ->
            FOOTNOTE_START.matchEntire(line.text)?.let { Triple(index, line, it) }
        }
        val result = starts.mapIndexed { position, (index, line, match) ->
            val end = starts.getOrNull(position + 1)?.first ?: relevant.size
            val text = buildList {
                add(match.groupValues[2])
                addAll(relevant.subList(index + 1, end).map { it.text })
            }.joinToString(" ").replace(WHITESPACE, " ").trim()
            Footnote(line.page, line.y, match.groupValues[1], text)
        }.sortedWith(SOURCE_POSITION_COMPARATOR)
        require(result.map { it.identifier } == (1..EXPECTED_FOOTNOTES).map(Int::toString))
        return result
    }

    private fun sourceField(field: String, lexical: String): GlycemicSourceField {
        if (lexical == "NS") {
            return GlycemicSourceField(lexical, GlycemicSourceFieldStatus.SOURCE_MISSING)
        }
        val present = when (field) {
            "gi", "sem", "gl", "availableCarbohydrate", "testPortion" -> lexical.toDoubleOrNull() != null
            "year" -> YEAR.matches(lexical)
            else -> lexical.isNotBlank()
        }
        return if (present) present(lexical) else GlycemicSourceField(null, GlycemicSourceFieldStatus.UNRESOLVED)
    }

    private fun present(lexical: String) = GlycemicSourceField(lexical, GlycemicSourceFieldStatus.PRESENT)

    private fun referenceCode(lines: List<GeoLine>, anchor: Anchor): String {
        val column = COLUMNS.last()
        val glyphs = lines.filter { it.page == anchor.page && abs(it.y - anchor.y) <= 5f }
            .flatMap { it.glyphs }
            .filter { glyph -> glyph.x < column.end && glyph.x + glyph.width / 2f >= column.start }
        return joinGlyphs(glyphs).replace(Regex("\\bUO\\s+(\\d+)"), "UO$1")
    }

    private fun cell(lines: List<GeoLine>, column: Column, excluded: Set<Pair<Int, Int>>): String = lines.mapNotNull { line ->
        if (column.name == "foodItem" && (line.page to quantize(line.y)) in excluded) return@mapNotNull null
        val selected = line.glyphs.filter { glyph -> glyph.x < column.end && glyph.x + glyph.width / 2f >= column.start }
        selected.takeIf(List<Glyph>::isNotEmpty)?.let(::joinGlyphs)
    }.let { physical ->
        if (column.name == "referenceCode") {
            val glyphs = lines.flatMap { it.glyphs }.filter { glyph -> glyph.x < column.end && glyph.x + glyph.width / 2f >= column.start }
            joinGlyphs(glyphs)
        } else {
            physical.joinToString(" ").replace(WHITESPACE, " ").trim()
        }
    }

    private fun mergeLines(lines: List<GeoLine>): List<GeoLine> {
        val result = mutableListOf<GeoLine>()
        lines.sortedWith(SOURCE_POSITION_COMPARATOR).forEach { line ->
            val previous = result.lastOrNull()
            if (previous != null && previous.page == line.page && line.y - previous.y <= 12f &&
                abs(previous.minX - line.minX) <= 1f && previous.style == line.style) {
                result[result.lastIndex] = previous.copy(
                    text = "${previous.text} ${line.text}".replace(WHITESPACE, " ").trim(),
                    glyphs = previous.glyphs + line.glyphs,
                    physicalYs = previous.physicalYs + line.y,
                )
            } else {
                result += line
            }
        }
        return result
    }

    private fun geometryLines(glyphs: List<Glyph>): List<GeoLine> = glyphs.groupBy { it.page }.toSortedMap().flatMap { (page, pageGlyphs) ->
        val groups = mutableListOf<MutableList<Glyph>>()
        pageGlyphs.sortedWith(compareBy<Glyph> { it.y }.thenBy { it.x }).forEach { glyph ->
            val group = groups.lastOrNull()?.takeIf { abs(it.first().y - glyph.y) <= LINE_TOLERANCE }
            if (group == null) groups += mutableListOf(glyph) else group += glyph
        }
        groups.map { group ->
            GeoLine(
                page = page,
                y = group.minOf { it.y },
                minX = group.minOf { it.x },
                text = joinGlyphs(group),
                glyphs = group.sortedBy { it.x },
                style = group.groupingBy { "${it.font}@${format(it.height)}" }.eachCount().maxBy { it.value }.key,
                physicalYs = listOf(group.minOf { it.y }),
            )
        }
    }

    private fun joinGlyphs(glyphs: List<Glyph>): String {
        val sorted = glyphs.sortedBy { it.x }
        return buildString {
            var end: Float? = null
            sorted.forEach { glyph ->
                if (end != null && glyph.x - end!! > maxOf(1.5f, glyph.height * 0.28f) && isNotEmpty() && last() != ' ') append(' ')
                append(glyph.text)
                end = maxOf(end ?: glyph.x, glyph.x + glyph.width)
            }
        }.replace(WHITESPACE, " ").trim()
    }

    private fun sha256(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    private fun quantize(value: Float) = (value * 10).toInt()
    private fun format(value: Float) = String.format(Locale.ROOT, "%.2f", value)

    private companion object {
        const val EXPECTED_SIZE = 2_216_220L
        const val EXPECTED_SHA256 = "13a2f85fb781bc8d8f9ce194a88f944b885377d59d8d340902ad1bd0d610e2d8"
        const val EXPECTED_PAGES = 139
        const val EXPECTED_MEASUREMENTS = 2_091
        const val EXPECTED_SUMMARIES = 122
        const val EXPECTED_NOTES = 15
        const val EXPECTED_FOOTNOTES = 26
        const val EXPECTED_MAJOR_CATEGORIES = 20
        const val BODY_TOP = 104.5f
        const val BODY_BOTTOM = 552f
        const val FOOD_COLUMN_END = 289f
        const val LINE_TOLERANCE = 0.75f
        val WHITESPACE = Regex("\\s+")
        val TOC = Regex("^([A-Z][A-Z &/,-]+):\\s+pages?\\s+(.+)$")
        val SUMMARY = Regex("mean of", RegexOption.IGNORE_CASE)
        val NOTE_START = Regex("^Average available carbohydrate portion", RegexOption.IGNORE_CASE)
        val FOOTNOTE_START = Regex("^(\\d+)\\.\\s*(.*)$")
        val GI_SEM = Regex("(-?\\d+(?:\\.\\d+)?)\\s*±\\s*(\\d+(?:\\.\\d+)?)")
        val YEAR = Regex("(?:19|20)\\d{2}(?:\\*|-(?:\\d{2}|(?:19|20)\\d{2}))?")
        val COLUMNS = listOf(
            Column("foodItem", 50f, 289f), Column("country", 289f, 337f),
            Column("year", 337f, 382f), Column("giSem", 382f, 422f),
            Column("gl", 422f, 449f), Column("subjects", 449f, 505f),
            Column("availableCarbohydrate", 505f, 530f), Column("testPortion", 530f, 558f),
            Column("referenceFoodTime", 558f, 624f), Column("timepoints", 624f, 678f),
            Column("sampleCollection", 678f, 732f), Column("analysisMethod", 732f, 786f),
            Column("referenceCode", 786f, 821f),
        )
        val SOURCE_POSITION_COMPARATOR = compareBy<SourcePosition> { it.page }.thenBy { it.y }.thenBy { it.x }
    }

    private interface SourcePosition { val page: Int; val y: Float; val x: Float }
    private data class Column(val name: String, val start: Float, val end: Float)
    private data class Glyph(val page: Int, val x: Float, val y: Float, val width: Float, val height: Float, val font: String, val text: String)
    private data class Extraction(val glyphs: List<Glyph>, val pageTexts: Map<Int, String>)
    private data class GeoLine(override val page: Int, override val y: Float, val minX: Float, val text: String, val glyphs: List<Glyph>, val style: String, val physicalYs: List<Float>, override val x: Float = minX) : SourcePosition
    private data class Anchor(val number: Int, val page: Int, val y: Float)
    private enum class HeadingKind { MAJOR, SUBCATEGORY }
    private data class Heading(override val page: Int, override val y: Float, override val x: Float, val text: String, val kind: HeadingKind) : SourcePosition
    private data class ContextEvent(val page: Int, val y: Float, val majorCategory: String, val subcategory: String?)
    private data class Footnote(override val page: Int, override val y: Float, val identifier: String, val text: String, override val x: Float = 0f) : SourcePosition
}
