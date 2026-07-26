package de.shopme.tools.knowledge.ciqual.parser

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.InputStream
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

internal class CiqualXmlRowReader {

    fun readRows(
        file: File,
        acceptedRowNames: Set<String>,
        consumer: (Map<String, String>) -> Unit
    ) {
        require(file.isFile) {
            "CIQUAL XML file not found: ${file.absolutePath}"
        }

        file.inputStream()
            .buffered()
            .use { input ->
                readRows(
                    input = input,
                    acceptedRowNames = acceptedRowNames,
                    consumer = consumer
                )
            }
    }

    fun readRows(
        input: InputStream,
        acceptedRowNames: Set<String>,
        consumer: (Map<String, String>) -> Unit
    ) {
        require(acceptedRowNames.isNotEmpty()) {
            "At least one CIQUAL XML row name must be accepted."
        }

        val normalizedRowNames =
            acceptedRowNames
                .map(::normalizeElementName)
                .toSet()

        val parserFactory =
            SAXParserFactory.newInstance().apply {
                isNamespaceAware = true
                isValidating = false

                setFeatureSafely(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true
                )

                setFeatureSafely(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
                )

                setFeatureSafely(
                    "http://xml.org/sax/features/external-general-entities",
                    false
                )

                setFeatureSafely(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
                )

                setFeatureSafely(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false
                )
            }

        val parser =
            parserFactory.newSAXParser()

        val handler =
            RowHandler(
                acceptedRowNames = normalizedRowNames,
                consumer = consumer
            )

        parser.parse(
            InputSource(input),
            handler
        )
    }

    private fun SAXParserFactory.setFeatureSafely(
        name: String,
        value: Boolean
    ) {
        runCatching {
            setFeature(name, value)
        }
    }

    private fun normalizeElementName(
        value: String
    ): String =
        value
            .substringAfterLast(':')
            .trim()
            .lowercase()
            .replace('-', '_')

    private inner class RowHandler(
        private val acceptedRowNames: Set<String>,
        private val consumer: (Map<String, String>) -> Unit
    ) : DefaultHandler() {

        private var currentRowName: String? = null
        private var currentFieldName: String? = null

        private val currentRow =
            linkedMapOf<String, String>()

        private val currentValue =
            StringBuilder()

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?
        ) {
            val elementName =
                normalizedName(
                    localName = localName,
                    qualifiedName = qName
                )

            if (currentRowName == null) {
                if (elementName in acceptedRowNames) {
                    currentRowName = elementName
                    currentRow.clear()
                    currentFieldName = null
                    currentValue.setLength(0)
                }

                return
            }

            currentFieldName = elementName
            currentValue.setLength(0)
        }

        override fun characters(
            ch: CharArray,
            start: Int,
            length: Int
        ) {
            if (currentRowName != null && currentFieldName != null) {
                currentValue.append(
                    ch,
                    start,
                    length
                )
            }
        }

        override fun endElement(
            uri: String?,
            localName: String?,
            qName: String?
        ) {
            val elementName =
                normalizedName(
                    localName = localName,
                    qualifiedName = qName
                )

            val rowName =
                currentRowName
                    ?: return

            if (elementName == rowName) {
                if (currentRow.isNotEmpty()) {
                    consumer(
                        currentRow.toMap()
                    )
                }

                currentRowName = null
                currentFieldName = null
                currentRow.clear()
                currentValue.setLength(0)

                return
            }

            val fieldName =
                currentFieldName

            if (fieldName != null && elementName == fieldName) {
                val value =
                    currentValue
                        .toString()
                        .trim()

                if (value.isNotEmpty()) {
                    currentRow[fieldName] = value
                }

                currentFieldName = null
                currentValue.setLength(0)
            }
        }

        override fun error(
            exception: SAXParseException
        ) {
            throw exception
        }

        override fun fatalError(
            exception: SAXParseException
        ) {
            throw exception
        }

        private fun normalizedName(
            localName: String?,
            qualifiedName: String?
        ): String {
            val value =
                localName
                    ?.takeIf { it.isNotBlank() }
                    ?: qualifiedName.orEmpty()

            return normalizeElementName(value)
        }
    }
}