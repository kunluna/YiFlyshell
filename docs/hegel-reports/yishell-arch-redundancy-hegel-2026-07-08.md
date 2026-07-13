---
signals:
  terminal_status: "converged"
  system_type: "deterministic"
---

# 收敛分析报告：逸飞项目架构冗余与功能叠加问题

## 收敛摘要
| 指标 | 值 |
|------|-----|
| 分析轮次 | 2 |
| 终态 | converged |
| 总发现数 | 12 |
| 按类型收敛 | deterministic: 12/12 |

## 确认的问题（按严重度排序）

### 🔴 Critical F1：终端 ANSI 解析三套机制并存
**问题**：项目中存在 AnsiParser、AnsiParserOptimized、VirtualTerminal、TerminalEmulator 四个文件，构成三套独立的 ANSI 解析/终端模拟方案。TerminalEmulator 是最终统一方案（直接从字节流→Cell[]→AnnotatedString），但旧三套文件仍在代码库中。

**证据**：
- TerminalEmulator.kt 包含完整的 ANSI 状态机解析 + Cell 缓冲 + AnnotatedString 输出
- AnsiParser（正则版本）和 AnsiParserOptimized（状态机版本）功能完全重叠
- VirtualTerminal 维护 Cell[][] 缓冲区再重新输出带 ANSI 码字符串——既不做最终渲染也不是主路径

**修复方案**：删除 AnsiParser、AnsiParserOptimized、VirtualTerminal 三个文件。TerminalEmulator 作为唯一终端解析器。

**置信度**：0.98（代码直接验证，无调用方引用旧文件）

---

### 🔴 Critical F2：连接导出/备份功能完全重叠
**问题**：ConnectionExporter 和 FullBackupManager 功能高度重叠——两者都做连接配置的 JSON 导出/导入，写入相同的外部存储目录，使用几乎相同的字段集。FullBackupManager 是 ConnectionExporter 的超集（多了 id/lastConnected/createdAt 字段和恢复功能）。

**证据**：
- ConnectionExporter.exportToJson() 和 FullBackupManager.createFullBackup() 产出相同结构的 JSON
- 两者都写入 Environment.DIRECTORY_DOCUMENTS/YiShell 目录
- 两者都有相同的 MAX_NAME_LENGTH=128、MAX_HOST_LENGTH=255 常量
- FullBackupManager 额外有 listBackups() 和 restoreFromFile() 方法

**修复方案**：删除 ConnectionExporter，统一使用 FullBackupManager。HomeViewModel 中的 exportConnections/importConnections 改为调用 FullBackupManager。

**置信度**：0.95

---

### 🟡 Major F3：GlassCard 三套实现并存
**问题**：存在三个 GlassCard 实现：
1. `GlassCard.kt` 的 `Modifier.glassCard()` —— **从未被调用**（死代码）
2. `HomeScreen.kt` 的 `private fun GlassCard()` —— HomeScreen 专用
3. `GlassEffects.kt` 的 `Modifier.whiteGlassCard()` —— AddConnectionScreen/SettingsScreen 使用

**证据**：
- `Get-ChildItem -Pattern "glassCard\(\)"` 返回零调用
- HomeScreen 的 GlassCard 用 Card + shadow + clip + white background
- whiteGlassCard 用 shadow + clip + background + border，支持暗色模式和玻璃效果开关

**修复方案**：
1. 删除 GlassCard.kt（死代码）
2. 将 HomeScreen 的 private GlassCard 合并到 whiteGlassCard，统一全局卡片样式
3. HomeScreen 改用 Modifier.whiteGlassCard()

**置信度**：0.97

---

### 🟡 Major F4：四个未调用的 Canvas 图标组件
**问题**：以下四个 Canvas 绘制的图标组件均未被任何 Composable 调用：
1. `GlassServerCube` —— Canvas 3D 立方体，已被 PNG 资源版 GlassServerIcon 替代
2. `GlassSmallServerIcon` —— PNG 资源版的小图标，同上
3. `GlassActionCubeIcon` —— 仅被 GlassServerIcon 的 showPlus 分支引用，但该分支已无调用方
4. `GlassFolder` —— Canvas 文件夹图标，项目中无文件夹功能界面

**证据**：
- `Select-String -Pattern "GlassServerCube|GlassSmallServerIcon|GlassFolder"` 在非定义/非 import 行返回零结果
- `Select-String -Pattern "GlassActionCubeIcon"` 同样无调用

**修复方案**：删除 GlassServerCube.kt、GlassFolder.kt，移除 GlassIcons.kt 中的 GlassSmallServerIcon 和 GlassActionCubeIcon 函数。

**置信度**：0.99

---

### 🟡 Major F5：copySelectedText 死代码
**问题**：TerminalViewModel.copySelectedText(start, end) 方法存在已知的 offset 映射 bug（在 parse()/stripAllAnsi() 之间映射不一致，hidden 段被跳过）。经对抗性审查后新增了 copyPlainText(text: String) 作为替代方案。但 copySelectedText 未被删除，且无调用方。

**证据**：
- `Select-String -Pattern "copySelectedText"` 仅在 TerminalViewModel.kt:523 行定义处出现，无调用
- 之前的审查记录明确指出 copySelectedText 的 offset 映射问题

**修复方案**：删除 copySelectedText 方法。

**置信度**：1.0

---

### 🟡 Major F6：MonitorViewModel 绕过 SshManager
**问题**：MonitorViewModel.executeCommand() 直接调用 `connection.openSession()` 执行监控命令，而不是使用 SshManager.executeSingleCommand()。这绕过了连接池管理、keep-alive、输出缓冲等所有基础设施。Monitor 每 3 秒轮询一次，意味着每 3 秒直接在 Connection 对象上创建/销毁一个 SSH channel。

**证据**：
- MonitorViewModel.kt:272 `private fun executeCommand(connection: Connection, command: String)` 直接操作 Connection
- SshManager.kt:266 已有 `executeSingleCommand(connectionId, command)` 方法
- MonitorViewModel.executeCpuStatSample 和 executeNetSample 都调用 executeCommand

**修复方案**：MonitorViewModel 改为调用 SshManager.executeSingleCommand()，移除直接对 Connection 的依赖。

**置信度**：0.92

---

### 🟡 Major F7：品牌蓝颜色不统一
**问题**：项目中存在两个不同的品牌蓝色值：
- `0xFF4D8DFF` —— Color.kt 中的 PrimaryBlue/ConnectionBlue/DesignBlue，TerminalScreen 的 handleColor，GlassIcons.kt 的 BrandBlue
- `0xFF007AFF` —— GlassServerCube.kt 中的 DEFAULT/BLUE 映射色和 plus badge 色

两个蓝色肉眼可辨差异（#4D8DFF 偏亮紫蓝，#007AFF 偏纯蓝）。

**证据**：
- `Select-String -Pattern "0xFF4D8DFF|0xFF007AFF"` 返回 12 处引用，两个值并存
- 设计规范中品牌蓝应统一为 #4D8DFF（之前的 hegel 收敛已确认）

**修复方案**：GlassServerCube.kt 中的 0xFF007AFF 改为 0xFF4D8DFF。但因 GlassServerCube 本身是死代码（F4），修复 F4 后此问题自动消失。

**置信度**：0.95

---

### 🟢 Minor F8：HomeViewModel 双数据源
**问题**：HomeViewModel 同时依赖 SshManager（读取 activeConnections、调用 disconnect）和 ConnectionRepository（读取连接列表）。两个数据源可能在状态同步上不一致——SshManager 的 activeConnections 是内存状态，ConnectionRepository 是数据库状态。

**证据**：
- HomeViewModel 构造函数注入 SshManager 和 ConnectionRepository
- disconnect() 调用 sshManager.disconnect()，但连接列表来自 connectionRepository.getAllConnections()
- 之前的修复已加入 HomeViewModel 监听数据库变更自动同步 SSOT，但基本架构仍是双源

**修复方案**：中期考虑让 SshManager 也通过 Repository 读写状态，统一 SSOT。短期可维持现状但需文档标注"两源同步靠 Flow 自动合并"。

**置信度**：0.75

---

### 🟢 Minor F9：GlassEffects.kt 的 blueAcrylicGlass() 仅一处使用
**问题**：blueAcrylicGlass() 修饰符仅被 SftpScreen.kt:524 使用一次。考虑是否值得维护独立的蓝色玻璃修饰符。

**证据**：
- `Select-String -Pattern "blueAcrylicGlass"` 返回 1 处调用（SftpScreen）

**修复方案**：保留。单点使用但 SftpScreen 确实需要不同的视觉风格（蓝色玻璃 vs 白色玻璃），删除反而降低表达力。

**置信度**：0.70

---

### 🟢 Minor F10：TerminalViewModel 直接操作剪贴板
**问题**：TerminalViewModel 直接通过 `appContext.getSystemService(Context.CLIPBOARD_SERVICE)` 操作剪贴板，这是平台副作用，理论上应封装在独立的 ClipboardManager wrapper 中。

**证据**：
- TerminalViewModel.kt 中 copyPlainText/pasteFromClipboard 直接操作 ClipboardManager

**修复方案**：低优先级。当前实现功能正确，封装为 wrapper 是架构洁癖层面的改进。可暂不处理。

**置信度**：0.60

---

### 🟢 Minor F11：终端输出缓冲仅在 resumeSession 时读取
**问题**：SshManager 新增了 64KB outputBuffer 环形缓冲，但仅在 resumeSession() 时通过 getBufferedOutput() 读取。正常运行时输出不经过缓冲——startOutputReader() 直接从 readAvailableOutputInternal 读取并更新 _terminalOutput，同时写入 outputBuffer。这意味着缓冲数据和历史数据可能不完全一致。

**证据**：
- SshManager.kt: appendToOutputBuffer 在 readAvailableOutputInternal 中同步写入
- getBufferedOutput 仅在 resumeSession 路径调用

**修复方案**：当前行为是正确的——正常使用时实时输出直接显示，仅在断开重连时用缓冲恢复历史。不需要修改，但需文档标注设计意图。

**置信度**：0.80

---

### 🟢 Minor F12：SshManager.executeSingleCommand 每次创建新 session
**问题**：executeSingleCommand 在已有连接上每次调用都 openSession + close，频繁创建/销毁 SSH channel。对于 Monitor 的 3 秒轮询场景，每 3 秒开一个新 channel。

**证据**：
- SshManager.kt:266-283 executeSingleCommand 每次创建新 Session 并 close

**修复方案**：中期可考虑为 Monitor 场景维护一个持久 exec channel。短期影响不大——SSH channel 创建开销约 50-100ms，3 秒轮询可接受。

**置信度**：0.65

## 零假设检验

| # | 零假设断言 | 验证结果 | 证据 |
|---|-----------|---------|------|
| H1 | 如果项目架构合理，不应该存在从未被调用的组件 | ❌ 未通过 | GlassCard.kt、GlassServerCube、GlassSmallServerIcon、GlassActionCubeIcon、GlassFolder 共 5 个死代码组件 |
| H2 | 如果终端处理设计正确，应该只有一个 ANSI 解析器 | ❌ 未通过 | 存在 AnsiParser + AnsiParserOptimized + VirtualTerminal + TerminalEmulator 四个文件 |
| H3 | 如果连接管理统一，所有功能模块应该通过 SshManager 操作 SSH 连接 | ❌ 未通过 | MonitorViewModel 绕过 SshManager 直接操作 Connection |
| H4 | 如果设计规范执行到位，品牌蓝应该只有一个值 | ❌ 未通过 | 0xFF4D8DFF 和 0xFF007AFF 并存 |

**零假设结论**：0/4 条零假设通过 → 项目存在系统性架构冗余问题，不是个别遗漏。

## 已排除的问题

无。所有 12 个发现均通过收敛判定。

## 审计日志

| 轮次 | 操作 | 结果 |
|------|------|------|
| 1 | 代码全量扫描 + 35 个发散想法 | 识别 12 个架构问题 |
| 2 | 收敛判定 | 12/12 确认，置信度均 ≥ 0.60 |

## 修复优先级排序

| 优先级 | 问题 | 修复动作 | 预估工作量 |
|--------|------|---------|-----------|
| P0 | F1 终端解析三套机制 | 删除 3 个旧文件 | 10 分钟 |
| P0 | F2 导出/备份重叠 | 删除 ConnectionExporter，改 HomeViewModel | 30 分钟 |
| P0 | F3 GlassCard 三套 | 删除 GlassCard.kt，合并 HomeScreen GlassCard 到 whiteGlassCard | 20 分钟 |
| P0 | F4 四个死 Canvas 组件 | 删除 2 个文件 + 移除 2 个函数 | 10 分钟 |
| P0 | F5 copySelectedText 死代码 | 删除该方法 | 2 分钟 |
| P1 | F6 MonitorViewModel 绕过 SshManager | 改为调用 executeSingleCommand | 45 分钟 |
| P1 | F7 品牌蓝不统一 | 修复 F4 后自动消失 | 0 分钟 |
| P2 | F8 HomeViewModel 双数据源 | 中期架构改进 | 2 小时 |
| P2 | F10 剪贴板封装 | 低优先级架构改进 | 30 分钟 |
| P2 | F12 executeSingleCommand channel 复用 | 中期性能优化 | 1 小时 |
| 保留 | F9 blueAcrylicGlass 单点使用 | 保留现状 | 0 |
| 保留 | F11 输出缓冲设计 | 文档标注即可 | 5 分钟 |
