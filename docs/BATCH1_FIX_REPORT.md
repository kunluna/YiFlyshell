# Batch 1 Reconciliation Fix Report

Date: 2026-06-25
Status: ✅ BUILD SUCCESSFUL

## Changes

### Color.kt — Deleted Unused Constants

| Constant | Line (before) | Reason |
|---|---|---|
| `Green700` | 78 | No references outside Color.kt |
| `PrimaryBlueDark` | 84 | No references outside Color.kt |
| `AnsiBlack` | 122 | No references outside Color.kt |
| `AnsiGreen` | 124 | No references outside Color.kt |
| `AnsiBlue` | 126 | No references outside Color.kt |
| `AnsiCyan` | 128 | No references outside Color.kt |
| `AnsiWhite` | 129 | No references outside Color.kt |
| `AnsiBrightBlack` | 130 | No references outside Color.kt |
| `AnsiBrightRed` | 131 | No references outside Color.kt |
| `AnsiBrightGreen` | 132 | No references outside Color.kt |
| `AnsiBrightYellow` | 133 | No references outside Color.kt |
| `AnsiBrightBlue` | 134 | No references outside Color.kt |
| `AnsiBrightMagenta` | 135 | No references outside Color.kt |
| `AnsiBrightCyan` | 136 | No references outside Color.kt |
| `AnsiBrightWhite` | 137 | No references outside Color.kt |

### Color.kt — Kept (Verified Referenced)

- `AnsiRed` — used in TerminalScreen, SftpScreen, MonitorScreen
- `AnsiYellow` — used in TerminalScreen, SftpScreen, MonitorScreen
- `AnsiMagenta` — used in TerminalScreen, SftpScreen, MonitorScreen

### Color.kt — Fixed GlassBlue

| Item | Before | After |
|---|---|---|
| `GlassBlue` definition | `Color(0xFF4A90FF).copy(alpha = 0.25f)` | `Color(0xFF4A90FF)` |

Alpha is controlled at usage sites in GlassEffects.kt (`.copy(alpha = 0.4f)`, `.copy(alpha = 0.15f)`, etc.), so the definition should be pure.

### GlassEffects.kt — Fixed Blur Value

| Item | Before | After |
|---|---|---|
| `lightGlassButton` blur | `.glassBlur(8)` | `.glassBlur(10)` |

Matches the design specification for light glass button blur radius.

## Build Verification

```
BUILD SUCCESSFUL in 27s
41 actionable tasks: 6 executed, 35 up-to-date
```

No compilation errors. All remaining references verified intact.
