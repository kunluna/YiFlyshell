---
topic: "逸飞项目架构冗余与功能叠加问题全盘审查"
problem_type: "deterministic"
total_ideas: 35
dimensions:
  - 终端文本处理管线
  - UI组件/图标体系
  - 数据层/连接管理
  - 颜色/设计系统
  - 功能重叠/死代码
  - 架构层级违规
---

# 想法池：逸飞项目架构冗余与功能叠加问题全盘审查

## P1 自由发散（20 个）

### 终端文本处理管线
1. **AnsiParser 与 AnsiParserOptimized 功能完全重叠**：两者都做 ANSI CSI/SGR 解析→AnnotatedString，AnsiParserOptimized 是后来加的状态机版本（支持256色），但 AnsiParser（正则版本）没有被删除 `[P1]`
2. **VirtualTerminal 是中间产物未被清理**：VirtualTerminal 维护一个 Cell[][] 缓冲区，把 ANSI 解析为带样式单元格再重新输出带 ANSI 码的字符串——它既不做最终渲染也不是终端模拟器的主路径，是早期"双层架构"的残留 `[P1]`
3. **TerminalEmulator 是最终统一方案但旧方案仍在**：TerminalEmulator 直接从输入字节流→Cell[]→AnnotatedString，是正确的一层架构。但 AnsiParser/Optimized/VirtualTerminal 三个文件都还在代码库中 `[P1]`
4. **copySelectedText 与 copyPlainText 两套复制逻辑**：copySelectedText 用 offset 截取 AnnotatedString（存在 ANSI 映射 bug），copyPlainText 直接接收 UI 提取的纯文本。前者已被审查确认有 bug 且无调用方，是死代码 `[P1]`
5. **终端输出读取链路：TerminalViewModel.outputReader 依赖 SshManager.readOutput，但输出缓冲刚加上**：之前没有缓冲，现在加了 outputBuffer 但只有 resumeSession 时读取——正常运行时输出不经过缓冲，历史输出和实时输出可能不一致 `[P1]`

### UI组件/图标体系
6. **GlassCard.kt 的 Modifier.glassCard() 从未被调用**：定义在 components 包但全项目无引用，是早期玻璃效果实验残留 `[P1]`
7. **HomeScreen 内 private GlassCard 与 GlassEffects.kt 的 whiteGlassCard 并存**：两者都做白色玻璃卡片效果，GlassCard 是 HomeScreen 私有实现，whiteGlassCard 是全局实现被 AddConnectionScreen/SettingsScreen 使用 `[P1]`
8. **GlassServerCube 未被任何 Composable 调用**：Canvas 绘制的3D立方体图标，但 HomeScreen 用的是 GlassServerIcon（PNG 资源版）。GlassServerCube 是纯 Canvas 方案的残留 `[P1]`
9. **GlassSmallServerIcon 未被任何 Composable 调用**：同上，PNG 资源版本已替代 Canvas 版本，但旧 Canvas 版本文件仍在 `[P1]`
10. **GlassActionCubeIcon 仅被 GlassServerIcon 的 showPlus 分支间接引用**：实际上 GlassServerIcon 在 useTerminal=true 时不用 showPlus，所以 GlassActionCubeIcon 也是死代码 `[P1]`
11. **GlassFolder 未被任何 Composable 调用**：Canvas 绘制的文件夹图标，但项目中没有使用文件夹功能的界面 `[P1]`

### 数据层/连接管理
12. **ConnectionExporter 与 FullBackupManager 功能高度重叠**：两者都做连接配置的 JSON 导出/导入，都写入外部存储，都用相同的字段集。ConnectionExporter 是简单版本，FullBackupManager 增加了 id/lastConnected/createdAt 字段和恢复功能 `[P1]`
13. **MonitorViewModel 绕过 SshManager 直接操作 Connection**：MonitorViewModel.executeCommand 直接调用 connection.openSession() 执行监控命令，而不是用 SshManager.executeSingleCommand。这绕过了连接池管理、keep-alive、输出缓冲等所有基础设施 `[P1]`
14. **SshManager.executeSingleCommand 在已有连接上开新 session**：每次执行单条命令都 openSession + close，频繁创建/销毁 SSH channel。对于 Monitor 的 3 秒轮询场景，这意味着每 3 秒开一个新 channel `[P1]`

### 颜色/设计系统
15. **品牌蓝存在 0xFF4D8DFF 和 0xFF007AFF 两个值**：Color.kt 中 PrimaryBlue/ConnectionBlue/DesignBlue 用 0xFF4D8DFF，但 GlassServerCube.kt 用 0xFF007AFF。两个蓝色肉眼可辨差异 `[P1]`
16. **GlassServerCube 内部硬编码颜色映射**：直接在 Canvas 代码里写 when(color)→Color(0xFF...)，而不是引用 Color.kt 的常量 `[P1]`

### 架构层级违规
17. **TerminalViewModel 直接依赖 appContext 操作剪贴板**：剪贴板操作是平台副作用，应该封装在一个 ClipboardManager wrapper 里而不是直接在 ViewModel 里 getSystemService `[P1]`
18. **HomeViewModel 同时依赖 SshManager 和 ConnectionRepository**：HomeViewModel 直接调 SshManager.disconnect() 和 SshManager.activeConnections，又通过 ConnectionRepository 读连接列表。两个数据源可能在状态同步上不一致 `[P1]`

### 功能重叠/死代码
19. **GlassEffects.kt 的 blueAcrylicGlass() 仅被 SftpScreen 使用一次**：考虑是否值得维护一个独立的蓝色玻璃修饰符，还是可以简化为通用卡片样式 `[P1]`
20. **Settings 页面引用 whiteGlassCard 但 HomeScreen 用自己的 private GlassCard**：同一个应用的卡片样式不统一，设置页和首页视觉风格可能不一致 `[P1]`

## P2a SCAMPER 补盲（10 个）

### S Substitute
21. **TerminalEmulator 可以完全替代 AnsiParser + AnsiParserOptimized + VirtualTerminal**：TerminalEmulator 已经包含了完整的 ANSI 状态机解析 + Cell 缓冲 + AnnotatedString 输出。三个旧文件可以全部删除 `[P2a-S]`
22. **FullBackupManager 可以替代 ConnectionExporter**：FullBackupManager 是 ConnectionExporter 的超集（多了 id/timestamps/restore），保留 FullBackupManager 删除 ConnectionExporter 即可 `[P2a-S]`

### C Combine
23. **HomeScreen 的 private GlassCard 应合并到 GlassEffects.kt 的 whiteGlassCard**：统一为一个全局卡片样式修饰符，消除重复实现 `[P2a-C]`
24. **MonitorViewModel 应合并到 SshManager 的 executeSingleCommand 路径**：监控命令也应通过 SshManager 执行，而不是绕过它 `[P2a-C]`

### A Adapt
25. **GlassServerCube 的 Canvas 绘制逻辑可以废弃**：既然已经用 PNG 资源图标，Canvas 绘制方案不需要保留。如果未来需要动态颜色，可以用 ColorFilter/tint 而非 Canvas 重绘 `[P2a-A]`

### M Modify
26. **终端文本处理管线可以简化为单层**：TerminalEmulator 作为唯一解析器，TerminalViewModel 直接使用它，不再经过 AnsiParser/VirtualTerminal 的中间转换 `[P2a-M]`
27. **品牌蓝应统一为单一常量**：所有引用 0xFF007AFF 的地方改为 0xFF4D8DFF，或者反过来——但必须只保留一个值 `[P2a-M]`

### E Eliminate
28. **删除 GlassCard.kt（Modifier.glassCard() 死代码）** `[P2a-E]`
29. **删除 GlassServerCube.kt、GlassFolder.kt、GlassSmallServerIcon（三个未调用的 Canvas 组件）** `[P2a-E]`
30. **删除 copySelectedText（死代码，已被 copyPlainText 替代）** `[P2a-E]`

### R Reverse
31. **从"每个功能加新文件"反转为"每个功能只修改一个文件"**：建立架构约束——新功能必须在现有文件中找到归属，而不是新建文件。终端相关的全归 TerminalEmulator，连接相关的全归 SshManager `[P2a-R]`

## P2b 视角轮换（4 个）

### 👤 用户/终端使用者视角
32. **用户不会看到代码冗余，但会感受到 APK 体积增大和潜在 bug**：死代码虽然不执行，但增加编译时间、APK 体积，更危险的是可能被误引用导致行为不一致 `[P2b-用户]`

### 🛠 技术/实现者视角
33. **当前架构的"新增文件"模式导致代码膨胀**：每次加功能新建文件，旧文件不清理。项目才几十个文件就有 6 个死代码文件、3 套终端解析器、2 套导出工具——如果项目继续发展，冗余会指数增长 `[P2b-技术]`

### ⚔ 竞争者/对手视角
34. **同类 SSH 客户端（Termius、JuiceSSH）的终端处理都是单层架构**：一个终端模拟器类负责所有 ANSI 解析和渲染，不会有 Parser + VirtualTerminal + Emulator 三层。这是行业最佳实践 `[P2b-竞争]`

### 🌪 极端/边缘视角
35. **如果 ANSI 解析器有 bug，用户可能在终端看到乱码**：两套解析器可能在不同代码路径被调用（虽然现在只有一套被调用），但未来开发者可能误用旧解析器导致输出不一致 `[P2b-边缘]`
