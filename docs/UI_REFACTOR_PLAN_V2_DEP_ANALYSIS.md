# UI_REFACTOR_PLAN_V2 Dependency Analysis

## The Core Problem

**Batch 2 adds `BottomNavigationBar` to `HomeScreen`, but `NavGraph.kt` changes are in Batch 8.**

Without NavGraph supporting bottom nav routes, the bottom nav bar cannot work — tabs won't navigate anywhere.

## Current Architecture (No Bottom Nav)

```
Screen.kt:     Home | Terminal | Sftp | Monitor | Settings | AddConnection | EditConnection
NavGraph.kt:   Standard composable routes, NO bottom navigation state
HomeScreen.kt: TopAppBar with icon buttons (backup/restore/settings)
```

**Key finding**: Current `NavGraph.kt` uses flat composable routes with `navController.navigate()`. There is no bottom navigation state management, no `BottomNavItem`, no backstack handling for tabs.

---

## Q1: Can BottomNavigationBar work without NavGraph changes?

**No.** Here's what happens:

| What Batch 2 Adds | What's Missing | Result |
|-------------------|----------------|--------|
| `BottomNavigationBar` composable with 3 tabs (连接/工具/我的) | Routes for each tab in `Screen.kt` | Tabs render but have no navigation targets |
| Tab click handlers | `NavGraph.kt` with bottom nav state | `navController.navigate()` calls undefined routes |
| Tab selection state | Backstack management for tabs | App crashes or does nothing on tab tap |
| Glass Server Cube decoration | `BottomNavItem` sealed class | No route matching possible |

**Concrete failure**: If Batch 2 adds a bottom nav bar calling `navController.navigate("tools")`, but `Screen.kt` only defines `Screen.Terminal.route = "terminal/{connectionId}"`, the navigation will throw an exception or silently fail.

---

## Q2: Should we merge Batch 8 into Batch 2? Or split?

**Recommended: Split NavGraph into two parts.**

### Option A: Merge Batch 8 into Batch 2 ❌
- Makes Batch 2 too large (Home + NavGraph + Screen changes)
- Batch 2 becomes a 3-4 hour task instead of 2
- High risk: mixing UI and navigation changes

### Option B: Split NavGraph ✅ (Recommended)
- **Batch 2**: Add bottom nav UI + basic route definitions
- **Batch 8**: Advanced features (deep links, transitions, animations)

| Batch | NavGraph Scope | Effort |
|-------|----------------|--------|
| Batch 2 (revised) | Add `BottomNavItem` sealed class, basic tab routes, simple nav state | +30 min |
| Batch 8 (revised) | Deep links, transitions, animations, advanced backstack | Same |

---

## Q3: Other Dependency Issues

### Issue 1: Batch 8 says "depends on Batch 2-7" but Batch 2 needs Batch 8
- Section 10 states: "Batch 8 depends on Batch 2-7"
- But Batch 2 adds bottom nav that requires NavGraph changes
- **Circular dependency**

### Issue 2: Batch 8 includes `Screen.kt` changes
- Batch 8 adds `BottomNavItem` sealed class to `Screen.kt`
- But `BottomNavItem` is needed by Batch 2's `BottomNavigationBar`
- **Must split Screen.kt changes**

### Issue 3: Current `HomeScreen.kt` uses TopAppBar, not BottomAppBar
- Batch 2 needs to change `Scaffold` from `topBar` to include `bottomBar`
- This is a structural change that requires NavGraph to handle the new navigation pattern
- Without NavGraph changes, the old TopAppBar navigation callbacks (`onConnect`, `onSettings`, etc.) won't work with bottom nav

### Issue 4: Batch 5 (Monitor) depends on connectionId routes
- Monitor screen needs `connectionId` parameter
- Bottom nav tabs for connected state (终端/文件/监控) need to pass `connectionId`
- This requires NavGraph to handle tab navigation with parameters
- **Should be addressed in the NavGraph split**

---

## Q4: Recommended Revised Batch Plan

### Revised Batch 2: Home Screen + Basic Bottom Nav (P0)

**Files**: `HomeScreen.kt`, `HomeViewModel.kt`, `GlassServerCube.kt`, **`Screen.kt`**, **`NavGraph.kt`**

**Additions to original Batch 2:**

1. **Screen.kt** — Add basic bottom nav routes:
```kotlin
sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Connections : BottomNavItem("connections", "连接", Icons.Default.Computer)
    data object Tools : BottomNavItem("tools", "工具", Icons.Default.Build)
    data object Profile : BottomNavItem("profile", "我的", Icons.Default.Person)
}
```

2. **NavGraph.kt** — Add minimal bottom nav state:
```kotlin
// Add to NavGraph composable:
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = navBackStackEntry?.destination?.route

// Add composable routes for bottom nav tabs:
composable("connections") { HomeScreen(...) }
composable("tools") { ToolsScreen() }  // placeholder
composable("profile") { ProfileScreen() }  // placeholder
```

3. **HomeScreen.kt** — Add BottomNavigationBar:
```kotlin
Scaffold(
    topBar = { ... },
    bottomBar = {
        NavigationBar {
            // 3 tabs with navigation
        }
    }
) { paddingValues -> ... }
```

### Revised Batch 8: Advanced Navigation (P1)

**Files**: `NavGraph.kt`, `Screen.kt`

**Scope reduced to:**
- Deep link support
- Navigation transitions/animations
- Advanced backstack management
- Connected state bottom nav (终端/文件/监控) with connectionId passing
- Screen transition animations

---

## Revised Batch Plan Summary

| Batch | Name | Changes | Dependency |
|-------|------|---------|------------|
| 1 | Theme & Glass Foundation | Color.kt, Theme.kt, GlassEffects.kt | None |
| **2** | **Home + Basic Bottom Nav** | HomeScreen.kt, GlassServerCube.kt, **Screen.kt (basic routes)**, **NavGraph.kt (basic state)** | Batch 1 |
| 3 | Terminal Screen | TerminalScreen.kt, GlassKeyboardButton.kt | Batch 1 |
| 4 | File Management | SftpScreen.kt, GlassFolderIcon.kt | Batch 1 |
| 5 | System Monitor | MonitorScreen.kt, GlassMonitorCard.kt | Batch 1 |
| 6 | Settings | SettingsScreen.kt | Batch 1 |
| 7 | Connection Form | AddConnectionScreen.kt | Batch 1 |
| **8** | **Advanced Navigation** | NavGraph.kt (deep links, transitions), Screen.kt (advanced routes) | Batch 2-7 |

**Critical Path**: Batch 1 → Batch 2 → Batch 5 → Batch 8

**Key Change**: Batch 2 now includes minimal NavGraph changes to make bottom nav functional. Batch 8 focuses on advanced features only.

---

## Verification After Revised Batch 2

- [ ] Bottom nav bar renders with 3 tabs
- [ ] Tapping "连接" tab shows HomeScreen
- [ ] Tapping "工具" tab shows placeholder
- [ ] Tapping "我的" tab shows placeholder
- [ ] Tab selection state updates correctly
- [ ] Back button returns to previous tab
- [ ] Glass Server Cube renders on Home screen
- [ ] Navigation doesn't crash on tab switch
