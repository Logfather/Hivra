from __future__ import annotations

import gzip
import json
import tempfile
import unittest
from pathlib import Path

from him_trainer.fresh_off_code_delta_v2 import build_delta, logical_digest, persist_and_reload, validate_delta


def _write(path: Path, rows: list[object]) -> None:
    with gzip.open(path, "wt", encoding="utf-8") as stream:
        for row in rows:
            stream.write(json.dumps(row) + "\n")


class FreshOffCodeDeltaV2Test(unittest.TestCase):
    def test_uses_unique_non_empty_codes_and_excludes_invalid_rows(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            baseline = root / "baseline.jsonl.gz"
            fresh = root / "fresh.jsonl.gz"
            _write(baseline, [{"source": {"code": "a"}}, {"_id": "a"}, {"_id": "b"}, {"name": "missing"}])
            _write(fresh, [{"_id": "a"}, {"code": "c"}, {"_id": "c"}, {"code": ""}])
            value = build_delta(baseline, fresh)
            validate_delta(value)
            self.assertEqual(value["payload"]["baseline"]["uniqueCodeCount"], 2)
            self.assertEqual(value["payload"]["fresh"]["uniqueCodeCount"], 2)
            self.assertEqual(value["payload"]["delta"]["newCodes"], ["c"])
            self.assertEqual(value["payload"]["delta"]["removedCodeCount"], 1)

    def test_persist_reload_and_digest_are_deterministic(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            baseline = root / "baseline.jsonl.gz"
            fresh = root / "fresh.jsonl.gz"
            destination = root / "delta.json"
            _write(baseline, [{"_id": "b"}, {"_id": "a"}])
            _write(fresh, [{"_id": "a"}, {"_id": "c"}])
            first = persist_and_reload(baseline, fresh, destination)
            second = build_delta(baseline, fresh)
            self.assertEqual(first["logicalDigest"], logical_digest(first["payload"]))
            self.assertEqual(first["logicalDigest"], second["logicalDigest"])

    def test_rejects_digest_tampering(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            baseline = root / "baseline.jsonl.gz"
            fresh = root / "fresh.jsonl.gz"
            _write(baseline, [{"_id": "a"}])
            _write(fresh, [{"_id": "b"}])
            value = build_delta(baseline, fresh)
            value["payload"]["delta"]["newCodeCount"] = 99
            with self.assertRaises(ValueError):
                validate_delta(value)


if __name__ == "__main__":
    unittest.main()
