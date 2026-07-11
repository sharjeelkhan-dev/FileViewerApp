### 📂 File Viewer App
![Status](https://img.shields.io/badge/Code-181717?style=for-the-badge&logo=github&logoColor=white) ![Security](https://img.shields.io/badge/Encrypted_Vault-607D8B?style=for-the-badge&logo=android&logoColor=white) ![Media](https://img.shields.io/badge/Media3_ExoPlayer-E64A19?style=for-the-badge&logo=youtube&logoColor=white)

> A premium, high-performance file management and viewing experience for Android, built with modern tools and a stunning Neon & Glassmorphism aesthetic.

| Subsystem | Technical Execution Architecture |
| :--- | :--- |
| 🚀 **Smart Explorer** | Seamless navigation across Internal Storage, Downloads, and Recent structures with a blazing-fast query engine. |
| 🛡️ **Secure Vault** | Isolated filesystem layers wrapping biometric prompt access constraints (fingerprint/face unlock) to encrypt and hide sensitive states. |
| 🎥 **All-in-One Viewer** | Immersive media rendering engine integrating Android Media3 (ExoPlayer) pipelines alongside custom high-fidelity PDF, image, and text layouts. |
| 📄 **Document Support** | Low-latency IO processing engines managing native rendering extensions for plain Text, JSON, XML, and Office formats (Docx, Xlsx). |
| 🎨 **Design Philosophy** | Complete edge-to-edge system interfaces engineered using frosted-glass canvas rules and vibrant neon palettes matching dynamic system dark modes. |
| 🔧 **State Preservation** | Architecture configured over strict MVVM and Clean boundaries using Hilt DI and Room caching to preserve state across rotation vectors. |

<details>
<summary><b>✨ View Interface Design (Click to Expand)</b></summary>
<br/>
<table width="100%">
  <!-- Row 1 -->
  <tr>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/948bcd18-822b-4309-9e69-78a26e30fd9d" width="100%" alt="Screen 1" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/363073f7-95b1-4183-8147-5ae1da6b5194" width="100%" alt="Screen 2" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/e52bf06c-89dd-4a75-ba8e-68b4505802eb" width="100%" alt="Screen 3" /></td>
  </tr>
  <!-- Row 2 -->
  <tr>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/2558837f-c370-4a0b-8c23-8357cd12ea28" width="100%" alt="Screen 4" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/0184296a-007b-4040-9b6f-e9e0786c95a8" width="100%" alt="Screen 5" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/86885a65-c24f-4906-8dbd-135dc7fe31f5" width="100%" alt="Screen 6" /></td>
  </tr>
  <!-- Row 3 -->
  <tr>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/443d3b4a-00cf-4542-9754-759d1b972fcd" width="100%" alt="Screen 7" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/bf897bbe-7072-458d-b3cd-062824839ff6" width="100%" alt="Screen 8" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/e84d3437-9b78-435c-acbf-09af1c5f4904" width="100%" alt="Screen 9" /></td>
  </tr>
  <!-- Row 4 -->
  <tr>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/e187ed0d-b9ef-4172-aaa9-4dfb8a993414" width="100%" alt="Screen 10" /></td>
    <td width="33.3%" align="center"></td>
    <td width="33.3%" align="center"></td>
  </tr>
</table>
</details>

---

## 🏗️ Architecture Blueprint

The ecosystem follows standard **Clean Architecture** patterns to decoupled features into modular targets:
* **Data Layer:** Manages remote serialization frameworks, local Room components, and physical storage repositories.
* **Domain Layer:** Business use-cases, unified model definitions, and contract boundary interfaces.
* **Presentation Layer:** Reactive UI layouts composed natively using Jetpack Compose, managed via unidirectional StateFlow models.

---

## 🛠 Setup & Installation

### 📋 Prerequisites
* Android Studio Ladybug (or newer)
* JDK 17+
* Android SDK 28 (Android 9.0) or higher

### ⚙️ Compilation Steps
1. **Clone the repository**:
   ```bash
   git clone [https://github.com/sharjeelkhan111213-coder/fileviewerapp.git](https://github.com/sharjeelkhan111213-coder/fileviewerapp.git)
