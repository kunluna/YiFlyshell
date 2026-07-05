# Batch 1 Audit Report — Theme & Glass Foundation

**Date**: 2026-06-25
**Scope**: Color.kt, Theme.kt, GlassEffects.kt

---

## Audit Results

| # | Check Item | Result | Line Numbers | Notes |
|---|-----------|--------|--------------|-------|
| 1 | No unused imports | **PASS** | — | All imports in 3 files are referenced |
| 2 | No unused variables/constants | **FAIL** | Color.kt | 20 unused constants (see detail below) |
| 3 | No hardcoded colors in GlassEffects.kt | **PASS** | — | All colors reference Color.kt constants or Color.Black/Transparent |
| 4 | Color hex values match spec | **PASS** | Color.kt:83,93,96,98,111-113 | All 7 spec tokens match exact hex values |
| 5 | Glass corner radius (cards=24dp, buttons=12dp) | **PASS** | GlassEffects.kt:32,37,58,61 | Cards=24dp, buttons=12dp |
| 6 | Glass blur values 10-30px | **FAIL** | GlassEffects.kt:60 | lightGlassButton uses 8px (below min 10) |
| 7 | Theme default isDarkTheme=false | **PASS** | Theme.kt:45 | `isDarkTheme: Boolean = false` |
| 8 | BUILD SUCCESSFUL | **PASS** | — | `./gradlew assembleDebug` completed successfully |
| 9 | GlassBlue pre-applied alpha (double-alpha) | **FAIL** | Color.kt:105, GlassEffects.kt:33-38 | GlassBlue has alpha=0.25; GlassEffects.kt applies additional .copy(alpha=) producing wrong final alphas |
| 10 | GlassEdgeHighlight used anywhere | **FAIL** | GlassEffects.kt:67 | Only defined, never imported or called outside GlassEffects.kt |

---

## Detail: Unused Constants (Check #2)

**20 constants** defined in Color.kt with only 1 file match (the definition itself):

| Constant | Line | Reason |
|----------|------|--------|
| Green700 | 78 | No references outside Color.kt |
| PrimaryBlueDark | 84 | No references outside Color.kt |
| LightBackground | 93 | No references outside Color.kt |
| Warning | 112 | No references outside Color.kt |
| Danger | 113 | No references outside Color.kt |
| TerminalCursor | 118 | No references outside Color.kt |
| TerminalSelection | 119 | No references outside Color.kt |
| AnsiBlack | 122 | No references outside Color.kt |
| AnsiGreen | 124 | No references outside Color.kt |
| AnsiBlue | 126 | No references outside Color.kt |
| AnsiCyan | 128 | No references outside Color.kt |
| AnsiWhite | 129 | No references outside Color.kt |
| AnsiBrightBlack | 130 | No references outside Color.kt |
| AnsiBrightRed | 131 | No references outside Color.kt |
| AnsiBrightGreen | 132 | No references outside Color.kt |
| AnsiBrightYellow | 133 | No references outside Color.kt |
| AnsiBrightBlue | 134 | No references outside Color.kt |
| AnsiBrightMagenta | 135 | No references outside Color.kt |
| AnsiBrightCyan | 136 | No references outside Color.kt |
| AnsiBrightWhite | 137 | No references outside Color.kt |

**Note**: Some of these (e.g., TerminalCursor/Selection, ANSI colors) may be reserved for future use by the ANSI parser. If intentional, add a `// Used by AnsiParser` comment. If not, remove.

---

## Detail: GlassBlue Double-Alpha (Check #9)

GlassBlue defined with pre-applied alpha:
```kotlin
val GlassBlue = Color(0xFF4A90FF).copy(alpha = 0.25f)  // line 105
```

Applied with additional alpha in GlassEffects.kt:
| Usage | Line | Effective Alpha |
|-------|------|----------------|
| `GlassBlue.copy(alpha = 0.15f)` (shadow ambient) | 33 | 0.15 (ignores 0.25) |
| `GlassBlue.copy(alpha = 0.25f)` (shadow spot) | 34 | 0.25 (ignores 0.25) |
| `GlassBlue.copy(alpha = 0.4f)` (background) | 38 | 0.4 (ignores 0.25) |
| `GlassBlue.copy(alpha = 0.3f)` (border) | 39 | 0.3 (ignores 0.25) |

**Impact**: `.copy(alpha=x)` replaces the alpha, so the 0.25 in GlassBlue is effectively dead. No visual bug but misleading. Fix: remove `.copy(alpha = 0.25f)` from GlassBlue definition, or stop using `.copy(alpha=)` in GlassEffects.kt.

---

## Summary

| Metric | Count |
|--------|-------|
| Total checks | 10 |
| PASS | 6 |
| FAIL | 4 |
| Unused constants | 20 |
| Blur below range | 1 (lightGlassButton: 8px) |

## Verdict

**FAIL — 4 issues require resolution before Batch 1 is clean.**

Critical: Check #2 (20 unused constants — dead code). Medium: Check #6 (blur 8px < 10px min), Check #9 (GlassBlue double-alpha), Check #10 (GlassEdgeHighlight dead code).
