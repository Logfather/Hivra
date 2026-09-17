import json
import sqlite3

import pytest
import hashlib
import torch

from him_trainer.source_bound_multicandidate_v1 import (
    AuthorityError, RetrievalConfig, is_final_holdout_eligible,
    materialize_with_exclusion, resolve_candidates_for_base,
)
from him_trainer.input_representation_v2 import build_input_representation_v2
from him_trainer.point12_token_tensor_builder_v1 import load_pinned_xlm_r_tokenizer_v1


def fixture_index(tmp_path):
    p = tmp_path / "index.sqlite"
    c = sqlite3.connect(p)
    c.executescript("CREATE TABLE evidence_records(internal_record_key INTEGER PRIMARY KEY, source_record_reference TEXT, source_native_identifiers_json TEXT, evidence_projection_json TEXT); CREATE VIRTUAL TABLE evidence_search USING fts5(primaryName, content='');")
    for i, code in enumerate(("A", "B", "B"), 1):
        c.execute("insert into evidence_records values(?,?,?,?)", (i, f"off:{code}:{i}", json.dumps({"code": code}), json.dumps({"primaryName": code})))
        c.execute("insert into evidence_search(rowid,primaryName) values(?,?)", (i, "apple"))
    c.commit(); c.close(); return str(p)


def test_bounded_deduplicated_and_bound(fixture_index):
    src = {"sourceSha256": "s", "authority": "source"}; idx = {"sourceSha256": "s", "authority": "index"}
    rows = resolve_candidates_for_base({"baseId": "base:1", "term": "apple"}, index_path=fixture_index, source_authority=src, index_authority=idx)
    assert [r["candidateId"] for r in rows] == ["A", "B"]
    assert all(r["groundTruthState"] == "UNRESOLVED" for r in rows)


def test_materialization_is_atomic_semantic_and_excludes():
    rec = {"sourceRecordReference": "off:A", "sourceAuthority": {"sourceSha256": "s"}, "indexAuthority": {"logicalDigest": "i"}}
    materialized, exclusion = materialize_with_exclusion(rec, excluded=set(), materialization_id="m1")
    assert materialized["finalHoldoutEligible"] is False
    assert not is_final_holdout_eligible("off:A", [exclusion])
    with pytest.raises(AuthorityError): materialize_with_exclusion(rec, excluded={"off:A"}, materialization_id="m1")


def test_binding_and_bounds_fail_closed(fixture_index):
    with pytest.raises(AuthorityError): RetrievalConfig(max_query_results=1000)
    with pytest.raises(AuthorityError): resolve_candidates_for_base({"baseId":"b","term":"x"}, index_path=fixture_index, source_authority={"sourceSha256":"a"}, index_authority={"sourceSha256":"b"})


def test_authoritative_model_input_distinguishes_candidates():
    common = {"productName": "apple", "ingredients": "apple", "categories": ["fruit"]}
    candidates = [
        {"term": "Braeburn apple", "normalizedTerm": "braeburn apple", "taxonomyPaths": (("fruit", "apple"),)},
        {"term": "Granny Smith apple", "normalizedTerm": "granny smith apple", "taxonomyPaths": (("fruit", "apple"),)},
    ]
    rendered = [build_input_representation_v2("apple", c, common).serialize() for c in candidates]
    assert rendered[0] != rendered[1]
    assert hashlib.sha256(rendered[0].encode()).hexdigest() != hashlib.sha256(rendered[1].encode()).hexdigest()
    tokenizer = load_pinned_xlm_r_tokenizer_v1()
    ids = [torch.tensor(tokenizer.encode(value, add_special_tokens=True).ids, dtype=torch.int64) for value in rendered]
    assert not torch.equal(ids[0], ids[1])
    masks = [torch.ones_like(value) for value in ids]
    assert not torch.equal(torch.nn.utils.rnn.pad_sequence(ids, batch_first=True, padding_value=1)[0], torch.nn.utils.rnn.pad_sequence(ids, batch_first=True, padding_value=1)[1])
    assert torch.equal(masks[0], masks[1]) is False or ids[0].numel() != ids[1].numel()
