# 学习笔记：3D 等距立方体 Compose 实现

> 来源：dev.to Canvas 教程 + 官方文档 + Compose Canvas API 研究

---

## 1. Compose Canvas 基础 API

**来源**: [dev.to/tkuenneth/drawing-and-painting-in-jetpack-compose-1-2okl](https://dev.to/tkuenneth/drawing-and-painting-in-jetpack-compose-1-2okl)

### 核心组件

```kotlin
@Composable
fun SimpleCanvas() {
    Canvas(
        modifier = Modifier.fillMaxWidth().preferredHeight(128.dp),
        onDraw = {
            // drawLine: 画线
            drawLine(Color.Black, Offset(0f, 0f), Offset(size.width - 1, size.height - 1))
            
            // drawCircle: 画圆
            drawCircle(Color.Red, 64f, Offset(size.width / 2, size.height / 2))
            
            // drawPath: 画自定义路径
            drawPath(path, color)
            
            // drawPoints: 画点集
            drawPoints(points, strokeWidth = 4f, pointMode = PointMode.Points, color = Color.Blue)
        }
    )
}
```

### 关键 API 说明

| API | 用途 |
|-----|------|
| `Canvas(onDraw = {...})` | 创建绘图画布 |
| `drawLine(color, start, end)` | 画线段 |
| `drawCircle(color, radius, center)` | 画圆 |
| `drawRect(color, topLeft, size)` | 画矩形 |
| `drawPath(path, color)` | 画自定义路径 |
| `drawArc(color, startAngle, sweepAngle, ...)` | 画弧形 |
| `drawPoints(points, pointMode)` | 画点集 |
| `rotate(degrees) { ... }` | 旋转后绘制 |
| `translate(left, top) { ... }` | 平移后绘制 |
| `scale(sx, sy) { ... }` | 缩放后绘制 |

### 渐变支持

```kotlin
// 线性渐变
val gradient = LinearGradient(
    listOf(Color.Blue, Color.Black),
    startX = ..., startY = ...,
    endX = ..., endY = ...,
    tileMode = TileMode.Clamp
)

// 径向渐变
val gradient = RadialGradient(
    listOf(Color.Black, Color.Blue),
    centerX = center.x, centerY = center.y,
    radius = 64f
)
```

### 虚线效果（新版 API）

```kotlin
drawCircle(
    Color.Red, 64f,
    Offset(size.width / 2, size.height / 2),
    style = Stroke(width = 8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
)
```

---

## 2. 3D 等距投影原理

### 等距变换公式

等距投影（Isometric Projection）将 3D 坐标 (x, y, z) 映射到 2D 屏幕坐标：

```
screenX = (x - z) * cos(30°) * scale
screenY = (y + (x + z) * sin(30°)) * scale
```

简化后（cos30° ≈ 0.866, sin30° = 0.5）：

```
screenX = (x - z) * 0.866 * scale
screenY = (x + z) * 0.5 * scale - y * scale
```

### 立方体的 3 个可见面

等距视角下，立方体只显示 3 个面（正面、顶部、右侧）：

```
        (0,0,1)---------(1,0,1)
           /|            /|
          / |           / |
   (0,0,0)---------(1,0,0) |
         |  |         |  |
         | (0,1,1)----|--(1,1,1)
         | /          | /
         |/           |/
   (0,1,0)---------(1,1,0)
```

### 实现立方体的顶点计算

```kotlin
data class Point3D(val x: Float, val y: Float, val z: Float)

fun toIso(point: Point3D, offsetX: Float, offsetY: Float, scale: Float): Offset {
    val screenX = (point.x - point.z) * 0.866f * scale + offsetX
    val screenY = (point.x + point.z) * 0.5f * scale - point.y * scale + offsetY
    return Offset(screenX, screenY)
}

// 立方体 8 个顶点
val cubeVertices = listOf(
    Point3D(0f, 0f, 0f), Point3D(1f, 0f, 0f), Point3D(1f, 1f, 0f), Point3D(0f, 1f, 0f),  // 后面
    Point3D(0f, 0f, 1f), Point3D(1f, 0f, 1f), Point3D(1f, 1f, 1f), Point3D(0f, 1f, 1f)   // 前面
)
```

### 等距立方体的 3 个面

```kotlin
// 顶面（Top）- 4 个顶点
val topFace = listOf(
    Point3D(0f, 0f, 1f), Point3D(1f, 0f, 1f),
    Point3D(1f, 0f, 0f), Point3D(0f, 0f, 0f)
)

// 左面（Left）- 4 个顶点
val leftFace = listOf(
    Point3D(0f, 0f, 1f), Point3D(0f, 1f, 1f),
    Point3D(0f, 1f, 0f), Point3D(0f, 0f, 0f)
)

// 右面（Right）- 4 个顶点
val rightFace = listOf(
    Point3D(1f, 0f, 1f), Point3D(1f, 1f, 1f),
    Point3D(1f, 1f, 0f), Point3D(1f, 0f, 0f)
)
```

---

## 3. Compose 等距立方体完整实现

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

data class IsoPoint(val x: Float, val y: Float, val z: Float)

fun isoToScreen(
    point: IsoPoint,
    origin: Offset,
    scale: Float
): Offset {
    val screenX = (point.x - point.z) * 0.866f * scale + origin.x
    val screenY = (point.x + point.z) * 0.5f * scale - point.y * scale + origin.y
    return Offset(screenX, screenY)
}

fun isoPath(
    points: List<IsoPoint>,
    origin: Offset,
    scale: Float
): Path {
    val path = Path()
    points.forEachIndexed { index, point ->
        val screenPoint = isoToScreen(point, origin, scale)
        if (index == 0) {
            path.moveTo(screenPoint.x, screenPoint.y)
        } else {
            path.lineTo(screenPoint.x, screenPoint.y)
        }
    }
    path.close()
    return path
}

@Composable
fun IsometricCube(
    modifier: Modifier = Modifier,
    size: Float = 100f,
    topColor: Color = Color(0xFF4CAF50),     // 绿色顶面
    leftColor: Color = Color(0xFF388E3C),     // 深绿左面
    rightColor: Color = Color(0xFF2E7D32)     // 更深绿右面
) {
    Canvas(modifier = modifier) {
        val origin = Offset(size.width / 2, size.height / 2)
        val scale = size.width / 2

        // 顶面
        val topPath = isoPath(
            listOf(
                IsoPoint(0f, 0f, 1f),
                IsoPoint(1f, 0f, 1f),
                IsoPoint(1f, 0f, 0f),
                IsoPoint(0f, 0f, 0f)
            ),
            origin, scale
        )
        drawPath(topPath, topColor)

        // 左面
        val leftPath = isoPath(
            listOf(
                IsoPoint(0f, 0f, 1f),
                IsoPoint(0f, 1f, 1f),
                IsoPoint(0f, 1f, 0f),
                IsoPoint(0f, 0f, 0f)
            ),
            origin, scale
        )
        drawPath(leftPath, leftColor)

        // 右面
        val rightPath = isoPath(
            listOf(
                IsoPoint(1f, 0f, 1f),
                IsoPoint(1f, 1f, 1f),
                IsoPoint(1f, 1f, 0f),
                IsoPoint(1f, 0f, 0f)
            ),
            origin, scale
        )
        drawPath(rightPath, rightColor)
    }
}
```

---

## 4. 多立方体网格（Tiled Isometric）

```kotlin
@Composable
fun IsometricGrid(
    modifier: Modifier = Modifier,
    gridSize: Int = 3,
    tileSize: Float = 50f,
    colors: List<Color> = listOf(
        Color(0xFF4CAF50), Color(0xFF66BB6A), Color(0xFF81C784)
    )
) {
    Canvas(modifier = modifier) {
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val origin = Offset(
                    size.width / 2 + (col - row) * tileSize * 0.866f,
                    size.height / 2 + (col + row) * tileSize * 0.5f
                )
                val path = isoPath(
                    listOf(
                        IsoPoint(0f, 0f, 1f),
                        IsoPoint(1f, 0f, 1f),
                        IsoPoint(1f, 0f, 0f),
                        IsoPoint(0f, 0f, 0f)
                    ),
                    origin, tileSize
                )
                drawPath(path, colors[(row + col) % colors.size])
            }
        }
    }
}
```

---

## 5. DrawScope 变换 API

Compose Canvas 的 `DrawScope` 支持三种变换：

| 方法 | 说明 |
|------|------|
| `rotate(degrees) { drawXxx() }` | 旋转绘制内容 |
| `translate(left, top) { drawXxx() }` | 平移绘制内容 |
| `scale(sx, sy) { drawXxx() }` | 缩放绘制内容 |
| `transformMatrix(matrix) { drawXxx() }` | 应用 4x4 变换矩阵（3D） |

### 变换矩阵实现 3D 旋转

```kotlin
// 在 DrawScope 中使用 transformMatrix 做 3D 变换
drawContext.transform.transformMatrix(
    android.graphics.Matrix().apply {
        // 构建等距投影矩阵
        preScale(0.866f, 0.5f)
        preRotate(45f)
    }
)
```

---

## 6. 注意事项

1. **路径闭合**: `Path` 绘制完成后必须调用 `close()`，否则面不会填充
2. **绘制顺序**: 先画顶面，再画侧面，避免遮挡问题
3. **性能**: 复杂 3D 场景考虑使用 `Modifier.graphicsLayer` 做硬件加速
4. **动画**: 使用 `animateFloatAsState` 或 `infiniteTransition` 控制旋转角度
5. **hit testing**: 等距场景需要自定义点击检测，将屏幕坐标转换回等距坐标

---

## 7. 参考资源

- [dev.to: Drawing and painting in Jetpack Compose #1](https://dev.to/tkuenneth/drawing-and-painting-in-jetpack-compose-1-2okl)
- [Android Developer: DrawScope API](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/drawscope/DrawScope)
- [Android Developer: Canvas API](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/Canvas)
- [Android Developer: Path API](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/Path)
- [Isometric Projection Math](https://en.wikipedia.org/wiki/Isometric_video_game_graphics)

---

*最后更新: 2026-06-28*
