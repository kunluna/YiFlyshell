# YiFly 🚀

<div align="center">

**[🇺🇸 English](README.md) | [🇨🇳 中文](README_zh.md)**

**A modern Android SSH client built with Jetpack Compose**

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.08.01-green?logo=android)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-007AFF)](https://apilevels.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-007AFF)](https://apilevels.com)

[Features](#-features) • [Screenshots](#-screenshots) • [Installation](#-installation) • [Contributing](#-contributing)

</div>

---

## 📱 Features

YiFly is a powerful, modern SSH client for Android, designed with Material Design 3 and built entirely with Jetpack Compose.

### Core Features

- 🔐 **Secure SSH Connection**
  - Password and SSH key authentication
  - Host key verification
  - Connection history and quick reconnect

- 💻 **Built-in Terminal**
  - Full-featured terminal emulator
  - Customizable font size and color schemes
  - Quick command shortcuts
  - Command history with search

- 📁 **SFTP File Manager**
  - Browse remote file system
  - Upload/download files
  - Create/delete/rename files and folders
  - File preview support

- 📊 **Server Monitoring**
  - Real-time CPU, memory, and disk usage
  - Process list with resource usage
  - Network traffic monitoring
  - Customizable refresh interval

- 🎨 **Modern UI**
  - Material Design 3
  - Dark theme support
  - Responsive layout
  - Smooth animations

---

## 📸 Screenshots

<div align="center">

| Home Screen | Terminal | File Manager |
|-------------|----------|--------------|
| *Coming soon* | *Coming soon* | *Coming soon* |

</div>

---

## 🛠️ Tech Stack

- **Language**: Kotlin 2.0.0
- **UI Framework**: Jetpack Compose 2024.08.01
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Navigation**: Compose Navigation
- **Database**: Room
- **Networking**: JSch (SSH library)
- **Async**: Kotlin Coroutines + Flow
- **Build System**: Gradle Kotlin DSL

---

## 📦 Installation

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or later
- JDK 17
- Android SDK 26+

### Build from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/kunluna/YiFlyshell.git
   cd YiFlyshell
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

### Download Release

*Coming soon: Download pre-built APK from the [Releases](https://github.com/kunluna/YiFlyshell/releases) page.*

---

## ⚙️ Configuration

### SSH Connection

1. Launch YiFly
2. Tap the "+" button to add a new connection
3. Fill in the connection details:
   - Host: Your server IP or domain
   - Port: SSH port (default: 22)
   - Username: SSH username
   - Password or SSH key
4. Tap "Connect"

### Quick Commands

You can customize quick commands for frequently used operations:

1. Connect to a server
2. Open the terminal
3. Tap the quick command icon
4. Add your custom commands

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### How to Contribute

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Reporting Bugs

Please use the [GitHub Issues](https://github.com/kunluna/YiFlyshell/issues) page to report bugs.

---

## 📝 License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [JSch](http://www.jcraft.com/jsch/) - Java Secure Channel library
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Material Design 3](https://m3.material.io/) - Design system

---

## 📧 Contact

- **Author**: YiFly
- **GitHub**: [@kunluna](https://github.com/kunluna)
- **Project Link**: [https://github.com/kunluna/YiFlyshell](https://github.com/kunluna/YiFlyshell)

---

<div align="center">

**⭐ Star this repository if you find it helpful!**

Made with ❤️ by YiFly

</div>
