import unittest

from him_trainer.sequence_length_authority_v2 import (
    BOS_TOKEN_ID,
    EOS_TOKEN_ID,
    HimSequenceLengthV2Error,
    MODEL_MAX_POSITION_EMBEDDINGS,
    MODEL_POSITIONAL_LIMIT,
    PAD_TOKEN_ID,
    POINT12_PAIR_SPECIAL_TOKEN_COUNT,
    SEQUENCE_LENGTH_V1,
    SEQUENCE_LENGTH_V2,
    SequenceLengthAuthorityV2,
    sequence_length_authority_v2,
    validate_train_eval_authority_match,
)


class SequenceLengthAuthorityV2Test(unittest.TestCase):
    def setUp(self) -> None:
        self.authority = sequence_length_authority_v2()

    def test_pinned_model_and_special_token_contract(self) -> None:
        self.assertEqual(SEQUENCE_LENGTH_V1, 128)
        self.assertEqual(SEQUENCE_LENGTH_V2, 256)
        self.assertEqual(MODEL_MAX_POSITION_EMBEDDINGS, 514)
        self.assertEqual(MODEL_POSITIONAL_LIMIT, 512)
        self.assertEqual(self.authority.special_position_offset, 2)
        self.assertEqual((BOS_TOKEN_ID, PAD_TOKEN_ID, EOS_TOKEN_ID), (0, 1, 2))
        self.assertEqual(POINT12_PAIR_SPECIAL_TOKEN_COUNT, 4)

    def test_authority_identity_is_versioned_and_target_independent(self) -> None:
        self.assertEqual(self.authority.authority_id, "HIM_SEQUENCE_LENGTH_AUTHORITY_V2")
        self.assertEqual(self.authority.authority_version, "2")
        self.assertEqual(self.authority.input_representation_id, "HIM_INPUT_REPRESENTATION_V2")
        self.assertNotIn("target", self.authority.authority_reference.lower())
        self.assertNotIn("partition", self.authority.authority_reference.lower())
        self.assertTrue(self.authority.authority_reference.startswith("sequence-length-authority:v2:"))

    def test_v2_boundary_n_minus_1_n_and_n_plus_1(self) -> None:
        self.authority.validate_token_length(SEQUENCE_LENGTH_V2 - 1)
        self.authority.validate_token_length(SEQUENCE_LENGTH_V2)
        with self.assertRaisesRegex(HimSequenceLengthV2Error, "INPUT_SEQUENCE_TOO_LONG"):
            self.authority.validate_token_length(SEQUENCE_LENGTH_V2 + 1)

    def test_current_longest_authorized_partitions_are_accepted(self) -> None:
        for length in (190, 144, 188):
            self.authority.validate_token_length(length)

    def test_no_truncation_or_silent_truncation(self) -> None:
        self.assertEqual(self.authority.truncation_policy, "NO_TRUNCATION_FAIL_IF_TOO_LONG")
        self.assertEqual(self.authority.overflow_policy, "FAIL_CLOSED")
        with self.assertRaises(HimSequenceLengthV2Error):
            self.authority.validate_token_length(257)

    def test_invalid_lengths_fail_closed(self) -> None:
        for invalid in (-1, True, 1.0):
            with self.assertRaises(HimSequenceLengthV2Error):
                self.authority.validate_token_length(invalid)

    def test_model_capacity_and_train_eval_match(self) -> None:
        self.authority.validate_model_position_capacity()
        validate_train_eval_authority_match(self.authority, SequenceLengthAuthorityV2())
        different = SequenceLengthAuthorityV2(max_sequence_length=224)
        with self.assertRaisesRegex(HimSequenceLengthV2Error, "TRAIN_EVAL_SEQUENCE_AUTHORITY_MISMATCH"):
            validate_train_eval_authority_match(self.authority, different)


if __name__ == "__main__":
    unittest.main()
