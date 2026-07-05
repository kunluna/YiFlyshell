# YiShell Project Rules

## Code Review Checklist ( mandatory )

Every code change MUST pass this checklist before reporting "done":

### 1. Trace the Data Flow
After writing any parser/processor/handler, answer these questions:
- Who calls me? Where does my input come from?
- Who consumes my output? Is the format correct?
- What position does my component occupy in the pipeline?

### 2. Search for References
After writing a new file or function:
- Grep the class name / function name
- Confirm it is actually imported and called
- If not called → the change is ineffective

### 3. End-to-End Test
Do NOT just unit-test the component in isolation.
Test the FULL chain: `input → my component → next component → final output`

### 4. Check Existing Behavior
Before modifying any component:
- Read its current responsibilities
- Understand its current input/output contract
- Ensure backward compatibility

### 5. Bold = Brightness
When handling SGR codes:
- Bold (code 1) should map standard colors (30-37) to bright variants (90-97)
- Do NOT just set a boolean flag — apply the mapping at render time

### 6. Hidden / Inverse
- `hidden` (code 8): characters should NOT appear in output
- `inverse` (code 7): foreground and background colors should swap

### 7. 256-Color and True-Color
- 256-color: `ESC[38;5;N` — N is an INDEX, not a raw ANSI code
- True-color: `ESC[38;2;R;G;B` — store as (0xFF << 24) | (R << 16) | (G << 8) | B
- Ensure the color value is compatible with the consumer's color lookup

## Build Rules
- Build command: `./gradlew assembleDebug`
- After build: `./gradlew --stop` and `pkill -f "kotlin.daemon"` to free memory
- Device: Windows ADB path `/mnt/c/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe`
- Device ID: `FEC0219A25002315` (TAS-AN00)

## Lessons Learned (from actual bugs)

### Bug 1: New code never executed
- Wrote AnsiParserOptimized (271 lines) but TerminalScreen still called old AnsiParser
- Grep for class name would have caught this immediately

### Bug 2: Output format mismatch
- VirtualTerminal.getOutput() returned `buffer.joinToString("\n")` — pure text
- Added color state tracking but never modified the output format
- Consumer expected plain text, so colors were silently lost

### Bug 3: Bold didn't affect brightness
- Set `bold = true` flag but never mapped standard colors → bright colors
- Bold (SGR 1) should turn color 31 → 91 (red → bright red)

### Rule: Before reporting "done"
1. grep the class/function name → confirm it's imported and called
2. Trace the full chain: input → your code → consumer
3. Test with real data, not just unit tests

## Architecture
- SSH: `com.trilead.ssh2` via local JAR `sshlib-2.2.48.jar`
- Terminal: VirtualTerminal.kt (state machine) + AnsiParser/AnsiParserOptimized (color rendering)
- UI: Jetpack Compose
- DI: Hilt
- Database: Room

## Design-to-Compose Skill（图片转代码）

### 图片分析5维度（生成代码前必须分析）
1. **页面类型**：列表页、详情页、表单页、仪表盘、设置页、登录页、搜索页
2. **布局结构**：单列、双列、网格、抽屉、底部/顶部导航、Tab 切换
3. **配色方案**：主色、辅色、背景色、文字色、强调色、错误色
4. **组件类型**：卡片、按钮、输入框、列表项、图标、图片、分隔线
5. **间距规律**：padding、margin、gap 数值（通常 4/8/16/24/32dp）

### 三参数系统
| 参数 | 作用 | 默认值 |
|------|------|--------|
| LAYOUT_VARIANCE | 布局方差（1-10） | 5 |
| COMPONENT_DENSITY | 组件密度（1-10） | 5 |
| STYLE_VARIANCE | 风格方差（1-10） | 5 |

**参数推断规则**：
- 列表页：LAYOUT_VARIANCE=3-5, COMPONENT_DENSITY=5-7, STYLE_VARIANCE=3-5
- 详情页：LAYOUT_VARIANCE=4-6, COMPONENT_DENSITY=3-5, STYLE_VARIANCE=4-6
- 表单页：LAYOUT_VARIANCE=2-4, COMPONENT_DENSITY=6-8, STYLE_VARIANCE=2-4
- 仪表盘：LAYOUT_VARIANCE=6-8, COMPONENT_DENSITY=7-9, STYLE_VARIANCE=5-7

### 设计系统映射
- **Material 3（默认）**：使用 MaterialTheme.colorScheme、标准组件
- **自定义主题**：lightColorScheme/darkColorScheme 定义品牌色
- **深色模式（必须）**：isSystemInDarkTheme() 切换，确保对比度符合 WCAG AA

### PRE-FLIGHT CHECKLIST（6大类）
1. **Material 3 合规**：使用 M3 组件、MaterialTheme 颜色/字体
2. **深色模式**：定义 light/darkScheme，所有颜色双版本
3. **无障碍支持**：contentDescription、对比度 4.5:1
4. **响应式布局**：dp 单位、fillMaxWidth/height、weight 分配
5. **代码质量**：函数名清晰、StateFlow 状态管理、Hilt 注入、Preview 函数
6. **间距规范**：8dp 网格系统、标准间距值

### 反默认原则
- ❌ 禁止：所有卡片16dp圆角、按钮紫色、列表LazyColumn、导航BottomNavigation
- ✅ 正确：根据设计图选择圆角、按钮颜色、列表类型、导航类型

### 失败模式与恢复
| 场景 | 修复策略 |
|------|----------|
| 设计图分析失败 | 请求更清晰图片或基于常见模式生成默认布局 |
| 三参数推断冲突 | 使用规则表默认值，必要时让用户指定 |
| 设计系统不匹配 | 创建自定义主题，保留 M3 组件结构 |
| 深色模式颜色冲突 | 调整亮度/饱和度，确保 WCAG AA 标准 |
| 组件代码生成错误 | 检查 import、参数类型，简化组件实现 |
| 无障碍支持缺失 | 回溯检查可交互元素，补充语义标签 |

**处理原则**：先修复后报告、保留上下文、渐进式降级、透明沟通
