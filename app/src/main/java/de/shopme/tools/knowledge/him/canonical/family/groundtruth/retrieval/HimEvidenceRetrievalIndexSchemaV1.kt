package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

object HimEvidenceRetrievalIndexSchemaV1 {
    const val SCHEMA_VERSION = "HIM_EVIDENCE_RETRIEVAL_INDEX_SCHEMA_V1"
    const val BUILD_POLICY_VERSION = "HIM_EVIDENCE_RETRIEVAL_INDEX_BUILD_V1"
    const val PROJECTION_POLICY_VERSION = "HIM_EVIDENCE_PROJECTION_V1"
    const val SQLITE_USER_VERSION = 1

    const val CREATE_METADATA = """
        CREATE TABLE index_metadata (
            singleton_id INTEGER PRIMARY KEY CHECK (singleton_id = 1),
            schema_version TEXT NOT NULL,
            source TEXT NOT NULL,
            source_artifact_path TEXT NOT NULL,
            source_artifact_sha256 TEXT NOT NULL,
            source_record_count INTEGER NOT NULL CHECK (source_record_count >= 0),
            logical_record_counts_json TEXT NOT NULL,
            index_build_policy_version TEXT NOT NULL,
            evidence_projection_policy_version TEXT NOT NULL,
            indexed_record_count INTEGER NOT NULL CHECK (indexed_record_count >= 0),
            evidence_record_count INTEGER NOT NULL CHECK (evidence_record_count >= 0),
            fts_row_count INTEGER NOT NULL CHECK (fts_row_count >= 0),
            logical_content_sha256 TEXT NOT NULL,
            build_state TEXT NOT NULL CHECK (build_state IN ('BUILDING', 'VALIDATED')),
            sqlite_runtime_version TEXT,
            sqlite_file_sha256 TEXT
        )
    """

    const val CREATE_EVIDENCE_RECORDS = """
        CREATE TABLE evidence_records (
            internal_record_key INTEGER PRIMARY KEY CHECK (internal_record_key > 0),
            source_record_reference TEXT NOT NULL UNIQUE,
            record_kind TEXT NOT NULL,
            source_native_identifiers_json TEXT NOT NULL,
            evidence_projection_json TEXT NOT NULL
        )
    """

    const val CREATE_EVIDENCE_SEARCH = """
        CREATE VIRTUAL TABLE evidence_search USING fts5(
            primary_name,
            secondary_names,
            taxonomy_text,
            ingredient_text,
            context_text,
            content='',
            tokenize='unicode61'
        )
    """

    const val EXACT_FETCH_SQL = """
        SELECT internal_record_key, source_record_reference, record_kind,
               source_native_identifiers_json, evidence_projection_json
        FROM evidence_records
        WHERE source_record_reference = ?
    """

    const val SEARCH_SQL = """
        SELECT records.internal_record_key, records.source_record_reference,
               records.record_kind, records.evidence_projection_json,
               bm25(evidence_search) AS technical_rank
        FROM evidence_search
        JOIN evidence_records AS records
          ON records.internal_record_key = evidence_search.rowid
        WHERE evidence_search MATCH ?
        ORDER BY technical_rank, records.source_record_reference
        LIMIT ?
    """

    val creationStatements: List<String> = listOf(
        "PRAGMA user_version = $SQLITE_USER_VERSION",
        CREATE_METADATA,
        CREATE_EVIDENCE_RECORDS,
        CREATE_EVIDENCE_SEARCH,
    )
}
