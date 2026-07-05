# Batch 2 Audit Reconciliation

## Finding 1: BottomNavItem.kt line 3 - unused `background` import

**CONFIRMED**

- Line 3: `import androidx.compose.foundation.background`
- Grep for `\.background\(` in BottomNavItem.kt returned **0 matches** in code body
- The file uses `containerColor = GlassSurface` (line 39) and `color = GlassBorder` (line 45) for styling, never `Modifier.background()`
- **Action**: Remove unused import

## Finding 2: YiFeiNavHost.kt - unused PaddingValues, padding, dp imports

**CONFIRMED**

- Line 4: `import androidx.compose.foundation.layout.PaddingValues` — **never used** in code body
- Line 6: `import androidx.compose.foundation.layout.padding` — **never used** in code body (grep found only the import line itself)
- Line 10: `import androidx.compose.ui.unit.dp` — **never used** in code body
- The file's modifiers are `Modifier.fillMaxSize()` (lines 34, 38) and bare `Modifier` (line 176) — no padding or dp values
- **Action**: Remove all three unused imports

## Finding 3: HomeScreen.kt line 25 - unused TextAlign import

**CONFIRMED**

- Line 25: `import androidx.compose.ui.text.style.TextAlign`
- Grep for `TextAlign` found **only the import line**, zero usages in code body
- No `TextAlign.Center`, `TextAlign.Start`, etc. anywhere in the file
- **Action**: Remove unused import

## Finding 4: Routes.Terminal.route and Routes.Monitor.route are dead code

**CONFIRMED — Intentional placeholder, not dead code**

- `Routes.Terminal.route = "bottom_terminal"` (Routes.kt:22)
- `Routes.Monitor.route = "bottom_monitor"` (Routes.kt:28)
- In YiFeiNavHost.kt lines 159-167, clicking Terminal or Monitor in the bottom nav navigates to `Screen.Home.route` instead:

```kotlin
Routes.Terminal -> {
    navController.navigate(Screen.Home.route) {
        popUpTo(Screen.Home.route) { inclusive = true }
    }
}
Routes.Monitor -> {
    navController.navigate(Screen.Home.route) {
        popUpTo(Screen.Home.route) { inclusive = true }
    }
}
```

- The "bottom_terminal" and "bottom_monitor" route strings are **never registered as composable destinations** in the NavHost
- These are placeholder routes for future terminal/monitor tabs — currently the bottom nav reuses HomeScreen for all tabs except Settings
- **Action**: Not a bug. Mark with TODO comment if desired, or leave as-is for future implementation

## Finding 5: HomeScreen.kt uses Color.White/Black/Gray instead of design tokens

**CONFIRMED — Acceptable but not ideal**

Usages found:
- `Color.White`: lines 70, 96, 219, 269, 275 (icon tints on primary backgrounds)
- `Color.Black`: lines 81, 108, 338 (text on light background)
- `Color.Gray`: lines 124, 331, 354 (secondary text)

Available design tokens in Color.kt:
- `LightOnBackground = Color(0xFF121212)` ≈ black
- `LightOnSurface = Color(0xFF121212)` ≈ black  
- `LightOnSurfaceVariant = Color(0xFF616161)` ≈ gray
- `LightSurface = Color(0xFFFFFFFF)` ≈ white

**Assessment**: These are standard UI colors (white icons on blue circles, black text on white background). Using built-in `Color.White/Black/Gray` is acceptable for:
- Icon tints on solid color backgrounds (PrimaryBlue circles)
- Simple text-on-background where tokens would be over-abstracted

**Recommended**: Replace `Color.Black` → `LightOnBackground` and `Color.Gray` → `LightOnSurfaceVariant` for consistency, but not critical. `Color.White` on PrimaryBlue backgrounds is fine as-is.

**Action**: Low priority cleanup, not blocking
