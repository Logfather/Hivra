# Nutrition Feature Optimization Evaluation

## Baseline

- Feature count: 18
- Test precision: 0.400000
- Test recall: 0.876404
- Test F1: 0.549296
- Test balanced accuracy: 0.857624

## Classification summary

- Required: 0
- Neutral: 4
- Harmful: 3

## Required features

None.

## Neutral features

| Feature | Δ Precision | Δ Recall | Δ F1 | Δ Balanced Accuracy |
|---|---:|---:|---:|---:|
| `domain_diet_or_substitute_difference_count` | +0.000000 | +0.000000 | +0.000000 | +0.000000 |
| `domain_non_semantic_token_difference_count` | +0.002062 | +0.000000 | +0.001941 | +0.000689 |
| `domain_region_or_style_difference_count` | +0.002062 | +0.000000 | +0.001941 | +0.000689 |
| `domain_same_domain_different_entity_count` | +0.002062 | +0.000000 | +0.001941 | +0.000689 |

## Harmful features

| Feature | Δ Precision | Δ Recall | Δ F1 | Δ Balanced Accuracy |
|---|---:|---:|---:|---:|
| `domain_form_or_processing_difference_count` | +0.008377 | +0.000000 | +0.007847 | +0.002755 |
| `domain_modifier_difference_count` | +0.004145 | +0.000000 | +0.003896 | +0.001377 |
| `domain_unknown_mismatch_count` | +0.004145 | +0.000000 | +0.003896 | +0.001377 |

## Recommended feature contract

- Recommended feature count: 15
- Removed feature count: 3

- `diagnostic_score`
- `reciprocal_candidate_rank`
- `reciprocal_candidate_count`
- `shared_token_count`
- `shared_token_ratio`
- `token_jaccard`
- `catalog_token_coverage`
- `server_token_coverage`
- `token_count_similarity`
- `character_length_similarity`
- `exact_normalized_match`
- `domain_diet_or_substitute_difference_count`
- `domain_same_domain_different_entity_count`
- `domain_region_or_style_difference_count`
- `domain_non_semantic_token_difference_count`

## Removed features

- `domain_form_or_processing_difference_count`
- `domain_modifier_difference_count`
- `domain_unknown_mismatch_count`
