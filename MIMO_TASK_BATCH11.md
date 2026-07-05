## 任务：将三个 Section 内容包裹在白色圆角卡片中，微调 ConnectedCard 图标尺寸

### 需要修改的文件
app/src/main/java/com/yishell/app/presentation/home/HomeScreen.kt

### 设计图参考
/home/openclaw/.openclaw/workspace/yishell/sy.png

### 设计图分析（已确认）
- 已连接、最近连接、收藏连接三个分区各自被一个白色圆角卡片（~16dp圆角）包裹，带浅灰色边框（0.5dp）和轻微阴影
- ConnectedCard 中的服务器图标尺寸约 56dp（当前代码是 60dp）
- 整体分区间距 16dp，各 section 内容 horizontal padding 24dp

### 修改内容

#### 1. ConnectedSection 函数（约第282行）
当前根布局是 Column + padding(horizontal=24dp)。需要在外层包一个 Card：
- Card 圆角 16dp，白色背景，0.5dp 浅灰色边框（DesignCardDivider），轻微阴影（elevation=2.dp）
- 内部 Column 的 horizontal padding 改为 16dp（外层 Card 已有 padding(horizontal=24.dp)，内部改为 16dp）
- vertical padding 也改为 16dp

实现：
```kotlin
@Composable
private fun ConnectedSection(
    sessions: List<HomeViewModel.ConnectedSession>,
    onDisconnect: (HomeViewModel.ConnectedSession) -> Unit,
    onDisconnectAll: () -> Unit,
    onOpenTerminal: (String) -> Unit,
    onMenuClick: (HomeViewModel.ConnectedSession) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, DesignCardDivider, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // ... 保持现有 Column 内容不变（Row 标题、sessions 列表等）...
        }
    }
}
```

#### 2. RecentSection 函数（约第456行）
同样的包装方式：
```kotlin
@Composable
private fun RecentSection(
    recentList: List<ConnectionConfig>,
    onConnect: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, DesignCardDivider, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // ... 保持现有 Column 内容不变 ...
        }
    }
}
```

#### 3. FavoriteSection 函数（约第570行）
同样的包装方式：
```kotlin
@Composable
private fun FavoriteSection(
    favoriteList: List<ConnectionConfig>,
    onConnect: (String) -> Unit,
    onMenuClick: (ConnectionConfig) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, DesignCardDivider, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // ... 保持现有 Column 内容不变 ...
        }
    }
}
```

#### 4. ConnectedCard 函数（约第375行）
将图标尺寸从 60.dp 改为 56.dp：
```kotlin
// 第375行: Modifier.size(60.dp) → Modifier.size(56.dp)
Image(
    painter = painterResource(glassServerIconRes(session.config.color)),
    contentDescription = null,
    modifier = Modifier.size(56.dp)
)
```

### 注意事项
- 使用已有的 DesignCardDivider 颜色作为边框色（Color.kt 已定义）
- Card 导入（Card、CardDefaults）已在文件顶部 import
- shadow elevation 用 2.dp（轻微阴影）
- **不要修改 NewConnectionCard**，它已经有 glass 效果
- **不要修改 TopNavBar**
- **不要修改 ConnectedCard 的其他内容**，只改图标尺寸
- 确保 Card 的 modifier 顺序：fillMaxWidth → padding → shadow → clip → border

### 完成后必须执行以下步骤再报告 done：
1. ./gradlew assembleDebug 编译验证
2. ./gradlew --stop && pkill -f "kotlin.daemon" 释放内存
3. 按 AGENTS.md 的 7 条 Code Review Checklist 逐条自检：
   - 追踪数据流：确认修改的三个 Section 被 HomeScreen 正确调用
   - 搜索引用：grep 确认 ConnectedSection、RecentSection、FavoriteSection 被使用
   - 端到端测试：确认 HomeScreen 整体流程正确
   - 检查现有行为：确保不破坏现有功能
4. 代码审计（必须在同一个进程内完成）：
   - 检查未使用变量/常量
   - 检查颜色值与设计文档是否一致
   - 检查圆角值与设计文档是否一致（16dp）
   - 检查是否有硬编码值应该引用设计 token
5. 输出审计报告（每条检查项的通过/不通过状态 + 具体行号）
6. 报告写入项目的 docs/BATCH11_AUDIT.md 文件（覆盖已有内容）

⚠️ 审计是任务的一部分，必须在代码修改和编译验证之后、报告 done 之前完成。
⚠️ 不要只报告"编译通过"就退出，审计报告是必须产出物。
