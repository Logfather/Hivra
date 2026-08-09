package de.shopme.tools.knowledge.catalog

import java.io.File

class CatalogNutritionReferenceNormalizer(
    private val inputFile: File,
    private val outputFile: File,
    private val aliasMapper: CatalogNutritionReferenceAliasMapper
) {

    fun normalize() {
        validateInputFile()
        prepareOutputDirectory()

        val json = inputFile.readText(
            Charsets.UTF_8
        )

        val objects = splitTopLevelObjects(json)

        require(objects.isNotEmpty() || json.trim() == "[]") {
            "Catalog does not contain readable top-level objects: " +
                    inputFile.absolutePath
        }

        val normalizedJson = objects
            .joinToString(
                separator = ",\n",
                prefix = "[\n",
                postfix = "\n]\n"
            ) { objectJson ->
                normalizeObject(objectJson)
                    .prependIndent("  ")
            }

        outputFile.writeText(
            text = normalizedJson,
            charset = Charsets.UTF_8
        )

        require(outputFile.isFile) {
            "Normalized catalog was not created: " +
                    outputFile.absolutePath
        }

        require(outputFile.length() > 0L || objects.isEmpty()) {
            "Normalized catalog output is unexpectedly empty: " +
                    outputFile.absolutePath
        }
    }

    private fun validateInputFile() {
        require(inputFile.path.isNotBlank()) {
            "Catalog input file path must not be blank."
        }

        require(inputFile.exists()) {
            "Catalog input file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.isFile) {
            "Catalog input path is not a file: " +
                    inputFile.absolutePath
        }

        require(inputFile.canRead()) {
            "Catalog input file is not readable: " +
                    inputFile.absolutePath
        }

        require(inputFile.canonicalFile != outputFile.canonicalFile) {
            "Input and output files must be different. Refusing to " +
                    "overwrite source catalog: ${inputFile.absolutePath}"
        }
    }

    private fun prepareOutputDirectory() {
        require(outputFile.path.isNotBlank()) {
            "Catalog output file path must not be blank."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Catalog output path is not a file: " +
                    outputFile.absolutePath
        }

        val parentDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        ) {
            "Catalog output file has no parent directory: " +
                    outputFile.absolutePath
        }

        if (!parentDirectory.exists()) {
            require(parentDirectory.mkdirs()) {
                "Could not create catalog output directory: " +
                        parentDirectory.absolutePath
            }
        }

        require(parentDirectory.isDirectory) {
            "Catalog output parent is not a directory: " +
                    parentDirectory.absolutePath
        }

        require(parentDirectory.canWrite()) {
            "Catalog output directory is not writable: " +
                    parentDirectory.absolutePath
        }
    }

    private fun normalizeObject(
        objectJson: String
    ): String {
        val normalizedName =
            Regex(
                """"normalized"\s*:\s*"([^"]+)""""
            )
                .find(objectJson)
                ?.groupValues
                ?.get(1)

        val legacyReference =
            Regex(
                """"nutritionReference"\s*:\s*"([^"]+)""""
            )
                .find(objectJson)
                ?.groupValues
                ?.get(1)

        val existingNutritionReference =
            Regex(
                """"nutrition"\s*:\s*\{[\s\S]*?"reference"\s*:\s*"([^"]+)""""
            )
                .find(objectJson)
                ?.groupValues
                ?.get(1)

        val reference = when {
            existingNutritionReference != null ->
                aliasMapper.map(existingNutritionReference)

            legacyReference != null ->
                aliasMapper.map(legacyReference)

            normalizedName != null &&
                    aliasMapper.hasAlias(normalizedName) ->
                aliasMapper.map(normalizedName)

            else ->
                "unknown"
        }

        val source =
            if (reference == "unknown") {
                "unknown"
            } else {
                "open_food_facts"
            }

        val withoutLegacy = objectJson.replace(
            Regex(
                """,?\s*"nutritionReference"\s*:\s*"[^"]+""""
            ),
            ""
        )

        return if (withoutLegacy.contains(""""knowledge"""")) {
            if (withoutLegacy.contains(""""nutrition"""")) {
                withoutLegacy.replace(
                    Regex(
                        """"nutrition"\s*:\s*\{[\s\S]*?\}"""
                    ),
                    """
                    "nutrition": {
                      "reference": "$reference",
                      "source": "$source"
                    }
                    """.trimIndent()
                )
            } else {
                withoutLegacy.replace(
                    Regex(
                        """"knowledge"\s*:\s*\{"""
                    ),
                    """
                    "knowledge": {
                      "nutrition": {
                        "reference": "$reference",
                        "source": "$source"
                      },
                    """.trimIndent()
                )
            }
        } else {
            withoutLegacy.replace(
                Regex(
                    """"autocomplete_tokens"\s*:\s*\[[\s\S]*?]"""
                )
            ) { match ->
                """
                ${match.value},
                "knowledge": {
                  "nutrition": {
                    "reference": "$reference",
                    "source": "$source"
                  }
                }
                """.trimIndent()
            }
        }
    }

    private fun splitTopLevelObjects(
        json: String
    ): List<String> {
        val objects = mutableListOf<String>()

        var depth = 0
        var startIndex = -1
        var insideString = false
        var escaped = false

        json.forEachIndexed { index, char ->
            if (escaped) {
                escaped = false
                return@forEachIndexed
            }

            if (char == '\\' && insideString) {
                escaped = true
                return@forEachIndexed
            }

            if (char == '"') {
                insideString = !insideString
                return@forEachIndexed
            }

            if (insideString) {
                return@forEachIndexed
            }

            when (char) {
                '{' -> {
                    if (depth == 0) {
                        startIndex = index
                    }

                    depth++
                }

                '}' -> {
                    depth--

                    require(depth >= 0) {
                        "Catalog JSON contains an unmatched closing brace " +
                                "at character index $index."
                    }

                    if (depth == 0 && startIndex >= 0) {
                        objects += json.substring(
                            startIndex,
                            index + 1
                        )

                        startIndex = -1
                    }
                }
            }
        }

        require(!insideString) {
            "Catalog JSON contains an unterminated string."
        }

        require(depth == 0) {
            "Catalog JSON contains unbalanced object braces."
        }

        return objects
    }
}