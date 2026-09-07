# Country terminology packs (Phase 5 / atlas §7)

This directory is the landing place for per-country terminology packs. It is
intentionally almost empty: the atlas rule is that a language pack launches
only when a competent speaker has reviewed its translated legal terminology,
moderation templates, and source labels. Machine-translated packs must never
ship here.

## What a pack looks like

One JSON file per country, named by ISO-3166 alpha-2 code, e.g. `fr.json`,
`jp.json`, `bd.json`. Each pack keeps the atlas's four separated layers:

```json
{
  "iso2": "FR",
  "interfaceLanguage": "fr-FR",
  "reviewedBy": null,
  "reviewedAt": null,
  "layers": {
    "interface": {
      "purpose": "Everyday UI labels",
      "terms": { "evidence": "Preuve", "disputed": "Contesté" }
    },
    "legal_terminology": {
      "purpose": "Terms with legal weight — reviewed wording only",
      "terms": { "energy_certificate": "Diagnostic de performance énergétique (DPE)" }
    },
    "user_content": {
      "purpose": "Labels for content users supply themselves",
      "terms": { "self_declared": "Déclaré par l'utilisateur" }
    },
    "machine_assisted": {
      "purpose": "Explanation shown when text is machine-assisted, never legal wording",
      "terms": { "summary_hint": "Résumé généré automatiquement — vérifiez les originaux" }
    }
  }
}
```

## Gating rule (non-negotiable)

- A pack is inactive until `reviewedBy` and `reviewedAt` are set by a
  competent human speaker. `CountryRegistry` treats `null` as "not launched".
- `legal_terminology` must never be machine-translated; if a term is
  unreviewed, the UI falls back to English rather than guessing.
- Countries whose profile confidence is below `REGIONAL_EVIDENCE` never get a
  pack, regardless of translation readiness.
- Interface translations live in `res/values-{locale}/strings.xml` once a pack
  is reviewed; this directory holds the terminology data the strings cannot
  express (source labels, legal term mapping, review state).
