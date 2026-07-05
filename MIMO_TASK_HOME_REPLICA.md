# MIMO Task: 首页 1:1 复刻 sy.png 设计图（Compose 模式）

## 设计图
- **文件路径**: `/home/openclaw/.openclaw/workspace/yishell/sy.png`
- **分辨率**: 853x1844px
- **⚠️ 你必须先用 image 工具读设计图 sy.png，逐项分析后再写代码**

## 当前代码
- **文件**: `app/src/main/java/com/yishell/app/presentation/home/HomeScreen.kt`
- **包名**: `com.yishell.app.presentation.home`

## 设计图关键规格（基于 image 工具分析）

### 全局
| 属性 | 值 |
|------|-----|
| 页面背景色 | `#F5F7FA`（极淡灰蓝） |
| 卡片背景色 | `#FFFFFF`（纯白） |
| 卡片圆角 | **20dp** |
| 卡片内边距 | **20dp** |
| 页面左右边距 | **20dp** |
| Section 间距 | **24dp** |

### 颜色
| 用途 | 颜色 | 值 |
|------|------|-----|
| 主标题文字 | 深黑 | `#1A1A1A` |
| 次要文字 | 中灰 | `#666666` |
| 三级文字/IP | 浅灰 | `#888888` |
| 品牌蓝 | 苹果蓝 | `#007AFF` |
| 状态绿 | 亮绿 | `#00C853` |
| 终端按钮背景 | 极淡蓝 | `#F0F8FF` |

### 字体大小
| 层级 | 字号 |
|------|------|
| H1 主标题"逸飞" | **28sp** |
| H2 Section 标题 | **16sp** |
| Body Bold 卡片标题 | **17sp** |
| Body Regular 列表标题 | **15sp** |
| Caption IP/副标题 | **13sp** |
| Small 时间/操作 | **12-14sp** |

### 各 Section 详细规格

#### 1. 顶部导航栏
- Logo: 蓝色纸飞机图标，40x40dp
- 标题"逸飞": 28sp, 加粗, `#1A1A1A`
- 副标题"现代 SSH 客户端": 12sp, `#666666`
- 搜索按钮: 白色圆形背景 44dp, 深灰图标 `#333333`
- 设置按钮: 同上
- 右侧间距: 16dp

#### 2. 已连接 Section
- 标题行: 绿色圆点 8dp + "已连接 (2)" 16sp 加粗 `#1A1A1A` + "全部断开" 14sp `#007AFF`
- 连接卡片: 白色背景, 圆角 20dp, 内边距 20dp
- 服务器图标: 3D方块 60x60dp
- 主机名: 17sp 加粗 `#1A1A1A`
- IP地址: 13sp `#888888`
- 状态行: 绿点8dp + "已连接" 13sp `#00C853` + 时间 13sp `#888888`
- 终端按钮: 圆角12dp, 背景 `#F0F8FF`, 文字 `#007AFF`, 60x50dp
- 菜单按钮: 三点竖排, 右上角 16dp

#### 3. 最近连接 Section
- 标题行: 时钟图标 16dp + "最近连接" 16sp 加粗 + "更多 >" 14sp `#666666`
- 列表项: 图标 40x40dp, 主机名 15sp 中等, IP 13sp `#888888`
- 右侧: 时间 13sp `#888888`, 箭头 12dp 灰色
- 分割线: 1dp `#EEEEEE`

#### 4. 收藏连接 Section
- 标题行: 黄星 16dp + "收藏连接" 16sp 加粗 + "管理 >" 14sp `#666666`
- 列表项: 图标 40x40dp + 小黄星 12x12dp 叠加
- "连接"按钮: 白色背景, 边框 1dp `#E0E0E0`, 圆角 10dp, 14sp `#007AFF`, 60x32dp

#### 5. 新建连接卡片
- 大图标: 半透明蓝色方块 + 加号圆圈, 80x80dp
- 标题: "新建 SSH 连接" 17sp 加粗 `#1A1A1A`
- 副标题: "添加一台新的服务器" 13sp `#888888`
- 间距: 图标与文本 16dp, 标题与副标题 6dp

## 修改范围
**本次修改 HomeScreen.kt 一个文件**

## 必须遵守的规则
1. **先用 image 工具读 sy.png**，逐项分析设计图
2. 基于读图结果修改代码，**不是凭记忆**
3. 所有颜色值、字号、圆角、间距必须与设计图一致
4. 保持所有现有功能不变（回调、状态管理、对话框等）
5. 只调整 UI 视觉层

## 完成后必须执行以下步骤再报告 done

### 1. 编译验证
```bash
# 确保没有旧 Gradle 进程
./gradlew --stop
./gradlew assembleDebug
```
确认 BUILD SUCCESSFUL

### 2. 真机安装 + 截图验证
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
sleep 3
adb shell screencap -p /sdcard/home_verify.png
adb pull /sdcard/home_verify.png ./sy_verify.png
```

### 3. 对比验证
用 image 工具读取 sy.png 和 sy_verify.png，逐项对比：
- 页面背景色
- 卡片圆角（20dp）
- 所有字体大小
- 所有颜色值
- 所有间距
- 图标尺寸

### 4. 代码审计
- 检查未使用变量/常量
- 检查颜色值与设计图是否一致
- 检查圆角/间距与设计图是否一致
- 检查是否有硬编码值应该引用设计 token

### 5. 输出报告
将审计报告写入 `docs/BATCH_HOME_REPLICA_AUDIT.md`

## ⚠️ 关键提醒
- 圆角是 **20dp**（不是 24dp）
- 页面背景是 **`#F5F7FA`**（不是 `#F8FAFC`）
- 主标题是 **28sp**（不是 18sp）
- 品牌蓝是 **`#007AFF`**（不是 `#3B82F6`）
- 状态绿是 **`#00C853`**（不是 `#10B981`）
- 这些都是**设计图实测值**，不要猜
