package de.shopme.tools.knowledge.ciqual.model

import java.io.File

data class CiqualSourceFiles(
    val foodsFile: File,
    val foodGroupsFile: File,
    val compositionsFile: File,
    val constituentsFile: File,
    val sourcesFile: File
) {

    fun validate() {
        val files =
            listOf(
                foodsFile,
                foodGroupsFile,
                compositionsFile,
                constituentsFile,
                sourcesFile
            )

        files.forEach { file ->
            require(file.isFile) {
                "CIQUAL source file not found: ${file.absolutePath}"
            }

            require(file.length() > 0L) {
                "CIQUAL source file is empty: ${file.absolutePath}"
            }
        }
    }

    companion object {

        fun fromDirectory(
            directory: File
        ): CiqualSourceFiles {
            require(directory.isDirectory) {
                "CIQUAL source directory not found: ${directory.absolutePath}"
            }

            return CiqualSourceFiles(
                foodsFile =
                    File(
                        directory,
                        "alim_2025_11_03.xml"
                    ),
                foodGroupsFile =
                    File(
                        directory,
                        "alim_grp_2025_11_03.xml"
                    ),
                compositionsFile =
                    File(
                        directory,
                        "compo_2025_11_03.xml"
                    ),
                constituentsFile =
                    File(
                        directory,
                        "const_2025_11_03.xml"
                    ),
                sourcesFile =
                    File(
                        directory,
                        "sources_2025_11_03.xml"
                    )
            )
        }
    }
}