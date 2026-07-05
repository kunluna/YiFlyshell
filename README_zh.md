# YiFly 🚀

<div align="center">

**基于 Jetpack Compose 构建的现代 Android SSH 客户端**

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.08.01-green?logo=android)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-007AFF)](https://apilevels.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-007AFF)](https://apilevels.com)

[功能特性](#-功能特性) • [截图](#-截图) • [安装](#-安装) • [贡献](#-贡献)

</div>

---

## 📱 功能特性

YiFly 是一款强大、现代的 Android SSH 客户端，采用 Material Design 3 设计，完全使用 Jetpack Compose 构建。

### 核心功能

- 🔐 **安全 SSH 连接**
  - 密码和 SSH 密钥认证
  - 主机密钥验证
  - 连接历史和快速重连

- 💻 **内置终端**
  - 功能完整的终端模拟器
  - 可自定义字体大小和配色方案
  - 快捷命令
  - 命令历史记录与搜索

- 📁 **SFTP 文件管理器**
  - 浏览远程文件系统
  - 上传/下载文件
  - 创建/删除/重命名文件和文件夹
  - 文件预览支持

- 📊 **服务器监控**
  - 实时 CPU、内存和磁盘使用率
  - 进程列表和资源使用情况
  - 网络流量监控
  - 可自定义刷新间隔

- 🎨 **现代 UI**
  - Material Design 3
  - 深色主题支持
  - 响应式布局
  - 流畅的动画效果

---

## 📸 截图

<div align="center">

| 主界面 | 终端 | 文件管理器 |
|-------|------|-----------|
| *即将推出* | *即将推出* | *即将推出* |

</div>

---

## 🛠️ 技术栈

- **语言**: Kotlin 2.0.0
- **UI 框架**: Jetpack Compose 2024.08.01
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **导航**: Compose Navigation
- **数据库**: Room
- **网络**: JSch (SSH 库)
- **异步**: Kotlin Coroutines + Flow
- **构建系统**: Gradle Kotlin DSL

---

## 📦 安装

### 前提条件

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK 26+

### 从源码构建

1. **克隆仓库**
   ```bash
   git clone https://github.com/kunluna/YiFlyshell.git
   cd YiFlyshell
   ```

2. **在 Android Studio 中打开**
   - 启动 Android Studio
   - 选择"打开现有项目"
   - 导航到克隆的目录

3. **构建项目**
   ```bash
   ./gradlew assembleDebug
   ```

4. **安装到设备**
   ```bash
   ./gradlew installDebug
   ```

### 下载发布版

*即将推出：从 [Releases](https://github.com/kunluna/YiFlyshell/releases) 页面下载预构建的 APK。*

---

## ⚙️ 配置

### SSH 连接

1. 启动 YiFly
2. 点击"+"按钮添加新连接
3. 填写连接详情：
   - 主机: 服务器 IP 或域名
   - 端口: SSH 端口（默认: 22）
   - 用户名: SSH 用户名
   - 密码或 SSH 密钥
4. 点击"连接"

### 快捷命令

您可以自定义常用操作的快捷命令：

1. 连接到服务器
2. 打开终端
3. 点击快捷命令图标
4. 添加您的自定义命令

---

## 🤝 贡献

欢迎贡献！请随时提交 Pull Request。

### 如何贡献

1. Fork 本仓库
2. 创建您的功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

### 报告 Bug

请使用 [GitHub Issues](https://github.com/kunluna/YiFlyshell/issues) 页面报告 bug。

---

## 📝 许可证

本项目采用 Apache License, Version 2.0 许可证 - 详见 [LICENSE](LICENSE) 文件。

---

## 🙏 致谢

- [JSch](http://www.jcraft.com/jsch/) - Java Secure Channel 库
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代 Android UI 工具包
- [Material Design 3](https://m3.material.io/) - 设计系统

---

## 📧 联系

- **作者**: YiFly
- **GitHub**: [@kunluna](https://github.com/kunluna)
- **项目链接**: [https://github.com/kunluna/YiFlyshell](https://github.com/kunluna/YiFlyshell)

---

<div align="center">

**⭐ 如果这个项目对您有帮助，请给它一个星标！**

Made with ❤️ by YiFly

</div>
