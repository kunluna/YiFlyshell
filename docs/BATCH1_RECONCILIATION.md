# BATCH1 Reconciliation Report

Human review findings vs. actual code verification.

---

## Finding 1: Green700 and PrimaryBlueDark are truly unused

**VERDICT: CONFIRMED**

- `Green700` — grep across all `.kt` files returns only 1 hit: `Color.kt:78` (its definition).
- `PrimaryBlueDark` — grep across all `.kt` files returns only 1 hit: `Color.kt:84` (its definition).

Neither constant is imported or referenced anywhere else in the project. They are dead code.

---

## Finding 2: lightGlassButton uses glassBlur(8), below 10px minimum

**VERDICT: CONFIRMED**

- `GlassEffects.kt:60`: `.glassBlur(8)` — verified.
- Design spec (`docs/BATCH1_AUDIT.md`, `BATCH1_AUDIT_V2.md`, `BATCH1_AUDIT_V3.md`) all document the 10–30px blur range.
- `lightGlassButton` is the only glass modifier using 8px; the others use 12px and 20px.
- `lightGlassButton` itself is also unused — grep shows only 1 match (its definition at `GlassEffects.kt:55`), meaning this is a double issue: wrong blur value AND dead code.

---

## Finding 3: GlassBlue alpha 0.25f is dead because .copy(alpha) replaces

**VERDICT: CONFIRMED**

- `Color.kt:105`: `val GlassBlue = Color(0xFF4A90FF).copy(alpha = 0.25f)`
- Every usage in `GlassEffects.kt` calls `GlassBlue.copy(alpha = ...)` with a new alpha value:
  - Line 33: `.copy(alpha = 0.15f)`
  - Line 34: `.copy(alpha = 0.25f)`
  - Line 38: `.copy(alpha = 0.4f)`
  - Line 39: `.copy(alpha = 0.3f)`
  - Line 62: `.copy(alpha = 0.15f)`

**Kotlin semantics**: `Color.copy(alpha = x)` **replaces** the alpha component, it does **not** multiply. So `GlassBlue` (alpha=0.25) with `.copy(alpha=0.4f)` produces alpha=0.4, not 0.1. The 0.25 in the definition is never effective — it's dead.

---

## Finding 4: GlassEdgeHighlight is defined but never called

**VERDICT: CONFIRMED**

- `GlassEffects.kt:67`: `fun GlassEdgeHighlight(modifier: Modifier = Modifier)` — definition only.
- Grep for `GlassEdgeHighlight` across all `.kt` files returns exactly 1 match: the definition.
- No file imports or calls it. It is dead code.

---

## Finding 5: ANSI colors referenced only in Color.kt — runtime mapping instead?

**VERDICT: PARTIALLY CONFIRMED — Nuanced**

The finding is correct that the ANSI parser runtime does NOT reference the named `Ansi*` constants. But some ANSI colors ARE used directly in UI code.

### Runtime parsers define their own inline colors

- `AnsiParser.kt:31-48`: `ANSI_COLORS` map uses raw hex values (e.g., `0xFF1A1A1A`), NOT `AnsiBlack`.
- `AnsiParserOptimized.kt:17-22`: `ANSI_COLORS` intArray uses the same raw hex values.

So the `Ansi*` named constants from Color.kt are **not** used by the parsers at all. The parsers duplicate the color values inline.

### Which ANSI colors ARE referenced outside Color.kt

| Constant | Color.kt | Theme.kt | TerminalScreen | SftpScreen | MonitorScreen |
|----------|----------|----------|----------------|------------|---------------|
| AnsiBlack | def | — | — | — | — |
| AnsiRed | def | ✓ (line 22) | ✓ (line 491) | ✓ (lines 166,172,492) | ✓ (lines 87,142,156,170,436,464,505) |
| AnsiGreen | def | — | — | — | — |
| AnsiYellow | def | — | — | ✓ (line 412) | ✓ (lines 143,157,171,436,443) |
| AnsiBlue | def | — | — | — | — |
| AnsiMagenta | def | — | — | — | ✓ (line 172) |
| AnsiCyan | def | — | — | — | — |
| AnsiWhite | def | — | — | — | — |

### All AnsiBright* colors — completely unused

`AnsiBrightBlack`, `AnsiBrightRed`, `AnsiBrightGreen`, `AnsiBrightYellow`, `AnsiBrightBlue`, `AnsiBrightMagenta`, `AnsiBrightCyan`, `AnsiBrightWhite` — all 8 have **zero** references outside their definitions in Color.kt.

### Summary for Finding 5

- **Truly unused (no references outside Color.kt):** AnsiBlack, AnsiGreen, AnsiBlue, AnsiCyan, AnsiWhite, + all 8 AnsiBright* = **13 constants**
- **Used directly in UI code (not via runtime mapping):** AnsiRed, AnsiYellow, AnsiMagenta = **3 constants**
- **No runtime mapping exists** — parsers define their own color arrays inline and never import the named constants
