# BATCH 9 AUDIT REPORT

**Date**: 2026-06-28
**Build**: `./gradlew assembleDebug` — **SUCCESS** (42 tasks, 10 executed)

---

## 1. Code Review Checklist (7 Items)

### 1.1 Trace the Data Flow
- **Status**: PASS
- `lightGlassButton()` is used as a `Modifier` extension — called on button modifiers. No consumer dependency issues.
- `HomeScreen` composable functions are all `private` except `HomeScreen` itself, which is called from navigation. No orphan functions.

### 1.2 Search for References
- **Status**: PASS
- `TerminalButtonBg` defined in `Color.kt:144`, imported via `presentation.theme.*`
- `CardBorderColor` defined in `Color.kt:145`, imported via `presentation.theme.*`
- `glassCard` imported from `presentation.components` (line 27)
- `GlassServerCube` imported from `presentation.components` (line 28)
- All referenced theme colors (`PrimaryBlue`, `TextPrimary`, `TextSecondary`, `DesignGreen`, `Danger`, `Warning`, `DesignCardDivider`, `PageBackground`) available via `presentation.theme.*`

### 1.3 End-to-End Test
- **Status**: PASS
- Full build successful: input (source) → compile → APK output
- No unresolved references in modified files

### 1.4 Check Existing Behavior
- **Status**: PASS
- All changes are value-level (alpha, dp, sp, FontWeight) — no structural or behavioral changes
- Backward compatibility maintained: no API signature changes

### 1.5 Bold = Brightness
- **Status**: N/A
- No SGR code handling in this batch

### 1.6 Hidden / Inverse
- **Status**: N/A
- No SGR code handling in this batch

### 1.7 256-Color and True-Color
- **Status**: N/A
- No ANSI color handling in this batch

---

## 2. Code Audit

### 2.1 Unused Variables/Constants
- **Status**: PASS
- All modified references are actively used in composable rendering. No dead code introduced.

### 2.2 Color Values vs Design Document

| Line | Color | Value | Expected | Status |
|------|-------|-------|----------|--------|
| 257 | Connected section title | `Color(0xFF333333)` | #333333 | PASS |
| 284 | ConnectedCard background | `Color.White.copy(alpha = 0.92f)` | White 0.92 | PASS |
| 368 | Terminal button bg | `TerminalButtonBg` → `Color(0xFFF0F5FF)` | #F0F5FF | PASS |
| 608 | FavoriteItem border | `CardBorderColor` → `Color(0xFFE0E0E0)` | #E0E0E0 | PASS |
| 53 | lightGlassButton bg | `Color.White.copy(alpha = 0.82f)` | White 0.82 | PASS |

### 2.3 Corner Radius / Blur Values vs Design Document

| Location | Value | Expected | Status |
|----------|-------|----------|--------|
| ConnectedSection Card (line 279,283,285,286) | 20.dp | 服务器 20dp | PASS |
| ConnectedCard terminal button (line 367) | 16.dp | 终端 16dp | PASS |
| FavoriteItem connect button (line 607,608) | 12.dp | 连接 12dp | PASS |
| NewConnectionCard (line 639,641) | 24.dp | Section 24dp | PASS |
| RecentSection/FavoriteSection cards (line 402,517) | 24.dp | Section 24dp | PASS |

### 2.4 Hardcoded Values vs Design Tokens
- **Status**: PASS
- `Color(0xFF333333)` at line 257: hardcoded as requested per spec
- `TerminalButtonBg` at line 368: references theme token
- `CardBorderColor` at line 608: references theme token
- All other values use existing theme constants (`TextPrimary`, `TextSecondary`, `DesignGreen`, `PrimaryBlue`, `Danger`, `Warning`, `DesignCardDivider`, `PageBackground`)

### 2.5 Glass Effects Implementation
- **Status**: PASS
- `lightGlassButton()` alpha updated to 0.82f (line 53 GlassEffects.kt)
- ConnectedCard uses `.background(Color.White.copy(alpha = 0.92f))` (line 284)
- RecentSection and FavoriteSection use `.glassCard()` modifier (lines 401, 516)
- NewConnectionCard uses `.whiteGlassCard()` modifier (line 638)

---

## 3. Issue Resolution Summary

| Issue | Description | File:Line | Status |
|-------|-------------|-----------|--------|
| 1 | CardShape undefined → RoundedCornerShape(24.dp) | HomeScreen.kt:639,641 | FIXED |
| 2 | Section spacing 16dp → 24dp | HomeScreen.kt:63 | FIXED |
| 3 | ConnectedCard shape 16dp → 20dp | HomeScreen.kt:279,283,285,286 | FIXED |
| 4 | ConnectedCard padding 12dp → 20dp | HomeScreen.kt:322 | FIXED |
| 5 | ConnectedCard bg opaque → alpha 0.92 | HomeScreen.kt:284 | FIXED |
| 6 | Terminal button style (shape, bg, padding) | HomeScreen.kt:367-370 | FIXED |
| 7 | Server name Bold → SemiBold | HomeScreen.kt:331 | FIXED |
| 8 | "已连接" fontSize 12sp → 15sp | HomeScreen.kt:354 | FIXED |
| 9 | Connected section title TextPrimary → #333333 | HomeScreen.kt:257 | FIXED |
| 10 | FavoriteItem connect btn shape/border | HomeScreen.kt:607-608 | FIXED |
| 11 | FavoriteItem server name Bold → SemiBold | HomeScreen.kt:592 | FIXED |
| 12 | FavoriteItem "连接" fontSize 12sp → 13sp | HomeScreen.kt:616 | FIXED |
| 13 | ConnectedCard more btn 24dp → 20dp | HomeScreen.kt:386 | FIXED |
| 14 | FavoriteItem more btn 24dp → 20dp | HomeScreen.kt:626 | FIXED |
| 15 | RecentItem arrow 20dp → 16dp | HomeScreen.kt:501 | FIXED |
| 16 | NewConnectionCard padding 16dp → 20dp | HomeScreen.kt:647 | FIXED |
| 17 | NewConnectionCard server cube 48dp → 64dp | HomeScreen.kt:650 | FIXED |

**Bonus fix**: `GlassFolder.kt` — fixed pre-existing `RoundRect` unresolved reference by adding `import androidx.compose.ui.geometry.RoundRect` and removing fully-qualified `androidx.compose.ui.graphics.RoundRect` references.

---

## 4. Files Modified

1. `app/src/main/java/com/yishell/app/presentation/theme/GlassEffects.kt` — 1 change
2. `app/src/main/java/com/yishell/app/presentation/home/HomeScreen.kt` — 17 changes
3. `app/src/main/java/com/yishell/app/presentation/components/GlassFolder.kt` — bonus pre-existing fix
