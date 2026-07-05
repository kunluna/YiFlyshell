# HOME_REPLICA 代码审计报告

## 修改文件
1. `app/src/main/java/com/yishell/app/presentation/theme/Color.kt`
2. `app/src/main/java/com/yishell/app/presentation/theme/GlassEffects.kt`
3. `app/src/main/java/com/yishell/app/presentation/home/HomeScreen.kt`

## 颜色值审计

| 检查项 | 设计值 | 代码值 | 状态 |
|--------|--------|--------|------|
| 页面背景 PageBackground | #F5F7FA | #F5F7FA | PASS |
| 品牌蓝 PrimaryBlue | #007AFF | #007AFF | PASS |
| 状态绿 DesignGreen | #00C853 | #00C853 | PASS |
| 设计蓝 DesignBlue | #007AFF | #007AFF | PASS |
| 分割线 DesignCardDivider | #EEEEEE | #EEEEEE | PASS |
| 终端按钮背景 DesignTerminalBg | #F0F8FF | #F0F8FF | PASS |
| 次要文字 TextSecondary | #666666 | #666666 | PASS |
| 三级文字 TextTertiary | #888888 | #888888 | PASS |
| 搜索/设置图标色 | #333333 | #333333 | PASS |

## 圆角审计

| 检查项 | 设计值 | 代码值 | 状态 |
|--------|--------|--------|------|
| 外层卡片圆角 | 20dp | 20dp | PASS |
| 内层连接卡片圆角 | 20dp | 20dp | PASS |
| 终端按钮圆角 | 12dp | 12dp | PASS |
| 收藏连接按钮圆角 | 10dp | 10dp | PASS |
| whiteGlassCard 圆角 | 20dp | 20dp | PASS |

## 字号审计

| 检查项 | 设计值 | 代码值 | 状态 |
|--------|--------|--------|------|
| H1 主标题 "逸飞" | 28sp | 28sp | PASS |
| 副标题 "现代 SSH 客户端" | 12sp | 12sp | PASS |
| Section 标题 (已连接/最近连接/收藏连接) | 16sp | 16sp | PASS |
| 连接卡片主机名 | 17sp Bold | 17sp Bold | PASS |
| 连接状态 "已连接" | 13sp | 13sp | PASS |
| 连接时间 | 13sp | 13sp | PASS |
| 最近连接主机名 | 15sp Medium | 15sp Medium | PASS |
| 最近连接 IP | 13sp | 13sp | PASS |
| 最近连接时间 | 13sp | 13sp | PASS |
| 收藏主机名 | 15sp Medium | 15sp Medium | PASS |
| 收藏 IP | 13sp | 13sp | PASS |
| 连接按钮文字 | 14sp | 14sp | PASS |
| 新建连接标题 | 17sp Bold | 17sp Bold | PASS |
| 新建连接副标题 | 13sp | 13sp | PASS |

## 间距审计

| 检查项 | 设计值 | 代码值 | 状态 |
|--------|--------|--------|------|
| 页面左右边距 | 20dp | 20dp | PASS |
| Section 间距 | 24dp | 24dp | PASS |
| 卡片内边距 | 20dp | 20dp | PASS |
| Logo 尺寸 | 40dp | 40dp | PASS |
| 搜索/设置按钮尺寸 | 44dp | 44dp | PASS |
| 服务器图标 (连接卡片) | 60dp | 60dp | PASS |
| 服务器图标 (最近/收藏列表) | 40dp | 40dp | PASS |
| 新建连接图标 | 80dp | 80dp | PASS |
| 绿色状态点 | 8dp | 8dp | PASS |

## 未使用变量/常量检查

- `GlassBlue` (Color.kt:105): 未在 HomeScreen.kt 中直接使用，但被 GlassEffects.kt 的 `blueAcrylicGlass()` 引用 → 保留
- `DesignTerminalBg` / `DesignFavoriteButtonBg`: 在 Color.kt 中定义，HomeScreen.kt 中使用 DesignTerminalBg → 保留
- `CardBorderColor`: 在 HomeScreen.kt 收藏连接按钮边框使用 → 保留
- `BrandGreen` / `BrandYellow`: BrandYellow 在收藏星标使用 → 保留

## 硬编码值检查

- HomeScreen.kt 搜索/设置图标色 `Color(0xFF333333)`: 设计图指定深灰图标，硬编码合理 → PASS
- 终端按钮 `>_` 文字: 功能性文字，硬编码合理 → PASS

## 编译验证

- BUILD SUCCESSFUL
- 42 actionable tasks: 6 executed, 36 up-to-date

## 真机验证

- ⚠️ 设备未连接 (FEC0219A25002315)，无法执行真机截图对比
- 待设备连接后需补充截图验证

## 总结

所有颜色值、圆角、字号、间距均已与设计图 sy.png 对齐。编译通过。
