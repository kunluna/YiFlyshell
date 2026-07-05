# Batch 8 补充任务 — 多会话标签栏 + 会话管理

## 前置条件
路由(Screen.kt)、导航(YiFeiNavHost.kt)、子页面(SettingsScreen.kt中的AppearanceSettingsScreen/TerminalSettingsScreen/ConnectionSettingsScreen)都已实现。

## 你需要做的

### 1. TerminalViewModel.kt — 添加会话管理

添加数据类:
```kotlin
data class SessionInfo(
    val id: String,
    val name: String,
    val connectionId: String
)
```

添加状态:
```kotlin
private val _sessions = MutableStateFlow<List<SessionInfo>>(emptyList())
val sessions: StateFlow<List<SessionInfo>> = _sessions.asStateFlow()

private val _currentSessionIndex = MutableStateFlow(0)
val currentSessionIndex: StateFlow<Int> = _currentSessionIndex.asStateFlow()
```

添加方法:
- switchSession(index: Int) — 切换到指定会话，更新 _currentSessionIndex
- closeSession(index: Int) — 关闭指定会话，不允许关闭最后一个，如果关闭的是当前会话则切换到前一个
- addSession(name: String, connectionId: String) — 添加新会话，自动生成UUID作为id

### 2. TerminalScreen.kt — 添加多会话标签栏

在 TopAppBar 下方、终端内容上方添加标签栏:

布局:
- 水平Row排列会话标签
- 每个标签: 文字(会话名) + x按钮(关闭)
- 最右侧: + 按钮(新建会话)
- 只在连接状态下显示（ConnectionState.Connected时）

样式:
- 高度: 48dp
- 背景色: 现有TopAppBar背景色
- 标签间距: 8dp
- 活动标签: PrimaryBlue文字 + PrimaryBlue底部2dp线
- 非活动标签: Color.LightGray文字 + 透明底部线
- x按钮: 小号(12sp)，Color.LightGray
- +按钮: PrimaryBlue

交互:
- 点击标签 -> viewModel.switchSession(index)
- 点击x -> viewModel.closeSession(index)
- 点击+ -> viewModel.addSession("会话N", currentConnectionId)

### 3. 颜色
- PrimaryBlue = #4A90FF (已在Color.kt定义)
- 不要硬编码颜色，用已有的主题色

### 4. 注意
- 保持现有TerminalScreen所有功能不变
- 会话切换只更新UI状态索引，不影响SSH连接

## 完成后
1. ./gradlew assembleDebug 编译验证
2. 代码审计: 检查未使用变量、颜色一致性、硬编码值
3. 审计报告写入 docs/BATCH8_AUDIT.md
4. 报告 done
