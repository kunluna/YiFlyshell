# Batch 8: Settings 子页面拆分 + 多会话管理

## 任务 1: SettingsSubPages.kt — 从 SettingsScreen.kt 提取子页面

文件: app/src/main/java/com/yishell/app/presentation/settings/SettingsSubPages.kt (新建)

从 SettingsScreen.kt 中提取以下 Composable 到新文件：
- AppearanceSettingsScreen (行 140-210)
- TerminalSettingsScreen (行 212-303)
- ConnectionSettingsScreen (行 305-378)

新文件 SettingsSubPages.kt 需要的 import：
- 所有这些 composable 使用的 Material3、compose.foundation、compose.ui 等
- SettingsViewModel (hiltViewModel)
- TerminalThemes, whiteGlassCard (从 presentation.theme)
- TerminalColorScheme (从 data.local)
- KeyboardLayoutEditor, TerminalThemePicker (这两个保留在 SettingsScreen.kt 中，但 SettingsSubPages 中的 TerminalSettingsScreen 需要引用它们)

注意：KeyboardLayoutEditor 和 TerminalThemePicker 保留在 SettingsScreen.kt 中。SettingsSubPages.kt 中的 TerminalSettingsScreen 需要引用它们（同包 com.yishell.app.presentation.settings，不需要额外 import）。

从 SettingsScreen.kt 删除提取的三个 composable 函数（AppearanceSettingsScreen, TerminalSettingsScreen, ConnectionSettingsScreen）。

## 任务 2: TerminalViewModel.kt — 添加会话管理

在 TerminalViewModel.kt 中添加：

1. 数据类（放在 ConnectionState sealed class 旁边，文件末尾）：

```kotlin
data class SessionInfo(
    val name: String,
    val connectionId: String,
    val isActive: Boolean = false
)
```

2. 在 TerminalViewModel 类中添加状态（放在 _connectionName 之后）：

```kotlin
private val _sessions = MutableStateFlow<List<SessionInfo>>(emptyList())
val sessions: StateFlow<List<SessionInfo>> = _sessions.asStateFlow()

private val _currentSessionIndex = MutableStateFlow(0)
val currentSessionIndex: StateFlow<Int> = _currentSessionIndex.asStateFlow()
```

3. 添加方法（放在 disconnect() 方法之后）：

switchSession(index: Int) — 切换到指定会话，更新 currentSessionIndex
closeSession(index: Int) — 关闭指定会话，从列表中移除，如果关闭的是当前会话则调整索引
addSession(name: String, connectionId: String) — 添加新会话到列表
getCurrentSession(): SessionInfo? — 返回当前会话

4. 在 init 块中，在 if (connectionId.isNotEmpty()) 之前，初始化第一个会话：

```kotlin
_sessions.value = listOf(SessionInfo(name = "会话 1", connectionId = connectionId, isActive = true))
```

## 任务 3: TerminalScreen.kt — 添加多会话标签栏

在 TerminalScreen.kt 的 TopAppBar 下方（Column 内部，Box 之前）添加标签栏代码：

```kotlin
// 会话标签栏
val sessions by viewModel.sessions.collectAsState()
val currentSessionIndex by viewModel.currentSessionIndex.collectAsState()

Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(40.dp)
        .background(TerminalBackground),
    verticalAlignment = Alignment.CenterVertically
) {
    LazyRow(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        itemsIndexed(sessions) { index, session ->
            SessionTab(
                session = session,
                isSelected = index == currentSessionIndex,
                onClick = { viewModel.switchSession(index) },
                onClose = { viewModel.closeSession(index) }
            )
        }
    }
    IconButton(onClick = { viewModel.addSession("新会话", "") }) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "新会话",
            tint = PrimaryBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}
```

添加 SessionTab Composable（放在 KeyboardButton 旁边）：

```kotlin
@Composable
fun SessionTab(
    session: SessionInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        color = if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = session.name,
                color = if (isSelected) PrimaryBlue else TerminalForeground.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            if (sessions.size > 1) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭会话",
                    tint = if (isSelected) PrimaryBlue.copy(alpha = 0.7f) else TerminalForeground.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onClose() }
                )
            }
        }
    }
}
```

需要在 TerminalScreen.kt 顶部添加 import：
- import com.yishell.app.presentation.theme.PrimaryBlue
- import androidx.compose.foundation.lazy.LazyRow
- import androidx.compose.foundation.lazy.itemsIndexed
- import androidx.compose.material.icons.filled.Add

## 重要注意事项
- SettingsSubPages.kt 的 package 必须是 com.yishell.app.presentation.settings
- TerminalScreen.kt 中的 SessionTab 需要 import SessionInfo（从同一个 terminal 包，不需要额外 import）
- 颜色使用 PrimaryBlue（Color.kt 中已定义为 #4A90FF）
- 标签栏高度 40dp，使用 TerminalBackground 背景色
- 活动标签使用 PrimaryBlue 文字色 + PrimaryBlue.copy(alpha=0.15f) 背景
- 非活动标签使用 TerminalForeground.copy(alpha=0.7f) 文字色
