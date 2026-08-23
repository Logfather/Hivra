package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunHimSqliteFts5RuntimeVerificationTest {

    @Test
    fun verifiesSqliteFts5ThroughTheJVMRuntime() {
        Class.forName("org.sqlite.JDBC")

        val temporaryDirectory = Files.createTempDirectory("him-sqlite-fts5-")
        val database = temporaryDirectory.resolve("runtime-verification.sqlite")

        try {
            connect(database).use { connection ->
                val sqliteVersion = sqliteVersion(connection)
                assertTrue(sqliteVersion.isNotBlank())
                println("SQLite runtime version: $sqliteVersion")
                createSchema(connection)
                insertSyntheticEvidence(connection)

                assertEquals(
                    listOf("off:braeburn", "off:braeburn-cultivar"),
                    queryReferences(connection, "braeburn", limit = 2),
                )
                assertEquals(
                    listOf("off:braeburn", "off:braeburn-cultivar"),
                    queryReferences(connection, "braeb*", limit = 2),
                )
                assertEquals(listOf("off:aepfel"), queryReferences(connection, "Äpfel"))
                assertEquals(listOf("off:creme-fraiche"), queryReferences(connection, "\"Crème fraîche\""))
                val englishResults = queryReferences(connection, "apple")
                assertTrue(englishResults.size <= 10)
                assertTrue("off:apple" in englishResults)
                println("ENABLE_FTS5 visible: ${fts5CompileOptionVisible(connection)}")
            }

            connect(database).use { reopened ->
                assertEquals(
                    listOf("off:braeburn", "off:braeburn-cultivar"),
                    queryReferences(reopened, "braeb*", limit = 2),
                )
                reopened.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA integrity_check").use { result ->
                        assertTrue(result.next())
                        assertEquals("ok", result.getString(1))
                    }
                }
            }
        } finally {
            deleteTemporaryDatabase(database)
            temporaryDirectory.deleteIfExists()
        }
    }

    private fun connect(database: Path): Connection =
        DriverManager.getConnection("jdbc:sqlite:${database.toAbsolutePath()}")

    private fun sqliteVersion(connection: Connection): String =
        connection.createStatement().use { statement ->
            statement.executeQuery("select sqlite_version()").use { result ->
                assertTrue(result.next())
                result.getString(1)
            }
        }

    private fun fts5CompileOptionVisible(connection: Connection): Boolean =
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA compile_options").use { result ->
                generateSequence { if (result.next()) result.getString(1) else null }
                    .any { it == "ENABLE_FTS5" }
            }
        }

    private fun createSchema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE evidence_records (
                    id INTEGER PRIMARY KEY,
                    source_reference TEXT NOT NULL UNIQUE,
                    payload TEXT NOT NULL
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE VIRTUAL TABLE evidence_search USING fts5(
                    name,
                    description,
                    tokenize='unicode61'
                )
                """.trimIndent()
            )
        }
    }

    private fun insertSyntheticEvidence(connection: Connection) {
        val rows =
            listOf(
                SyntheticEvidence("off:braeburn", "Braeburn", "apple cultivar"),
                SyntheticEvidence("off:braeburn-cultivar", "Braeburn apple cultivar", "food description"),
                SyntheticEvidence("off:granny-smith", "Granny Smith apple", "green apple cultivar"),
                SyntheticEvidence("off:herring", "Atlantic herring", "fish"),
                SyntheticEvidence("off:matjes", "Matjes", "herring preparation"),
                SyntheticEvidence("off:dinkel", "Dinkelvollkornbrot", "German bread"),
                SyntheticEvidence("off:aepfel", "Äpfel", "German Unicode food name"),
                SyntheticEvidence("off:creme-fraiche", "Crème fraîche", "French accented food name"),
                SyntheticEvidence("off:pomme", "pomme", "French food name"),
                SyntheticEvidence("off:apple", "apple", "English food name"),
            )

        connection.autoCommit = false
        try {
            connection.prepareStatement(
                "INSERT INTO evidence_records(id, source_reference, payload) VALUES (?, ?, ?)"
            ).use { records ->
                connection.prepareStatement(
                    "INSERT INTO evidence_search(rowid, name, description) VALUES (?, ?, ?)"
                ).use { search ->
                    rows.forEachIndexed { index, row ->
                        val id = index + 1
                        records.setInt(1, id)
                        records.setString(2, row.sourceReference)
                        records.setString(3, "source-faithful-payload-$id")
                        records.addBatch()

                        search.setInt(1, id)
                        search.setString(2, row.name)
                        search.setString(3, row.description)
                        search.addBatch()
                    }
                    records.executeBatch()
                    search.executeBatch()
                }
            }
            connection.commit()
        } catch (failure: Throwable) {
            connection.rollback()
            throw failure
        } finally {
            connection.autoCommit = true
        }
    }

    private fun queryReferences(
        connection: Connection,
        query: String,
        limit: Int = 10,
    ): List<String> =
        connection.prepareStatement(
            """
            SELECT records.source_reference, bm25(evidence_search) AS rank
            FROM evidence_search
            JOIN evidence_records AS records ON records.id = evidence_search.rowid
            WHERE evidence_search MATCH ?
            ORDER BY rank, records.source_reference
            LIMIT ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, query)
            statement.setInt(2, limit)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        result.getDouble("rank")
                        add(result.getString("source_reference"))
                    }
                }
            }
        }

    private fun deleteTemporaryDatabase(database: Path) {
        listOf("-journal", "-wal", "-shm").forEach { suffix ->
            database.resolveSibling(database.fileName.toString() + suffix).deleteIfExists()
        }
        database.deleteIfExists()
    }

    private data class SyntheticEvidence(
        val sourceReference: String,
        val name: String,
        val description: String,
    )
}
