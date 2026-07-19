### 📂 Smart File Viewer App
![Status](https://img.shields.io/badge/Code-181717?style=for-the-badge&logo=github&logoColor=white) ![AI](https://img.shields.io/badge/Google_Gemini-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white) ![Security](https://img.shields.io/badge/Encrypted_Vault-607D8B?style=for-the-badge&logo=android&logoColor=white) ![Media](https://img.shields.io/badge/Media3_ExoPlayer-E64A19?style=for-the-badge&logo=youtube&logoColor=white)

> A premium, high-performance file management and viewing experience for Android, built with modern tools and a stunning Neon & Glassmorphism aesthetic.

| Subsystem | Technical Execution Architecture |
| :--- | :--- |
| 🚀 **Smart Explorer** | Seamless navigation across Internal Storage, Downloads, and Recent structures with a blazing-fast query engine. |
| 🧠 **Gemini Intelligence** | Context-aware AI bridge leveraging Gemini 3.5-Flash for real-time document summarization, natural language file querying, and automated data extraction. |
| 🛡️ **Secure Vault** | Isolated filesystem layers wrapping biometric prompt access constraints (fingerprint/face unlock) to encrypt and hide sensitive states. |
| 🎥 **All-in-One Viewer** | Immersive media rendering engine integrating Android Media3 (ExoPlayer) pipelines alongside custom high-fidelity PDF, image, and text layouts. |
| 📄 **Document Support** | Low-latency IO processing engines managing native rendering extensions for plain Text, JSON, XML, and Office formats (Docx, Xlsx). |
| 🎨 **Design Philosophy** | Complete edge-to-edge system interfaces engineered using frosted-glass canvas rules and vibrant neon palettes matching dynamic system dark modes. |
| 🔧 **State Preservation** | Architecture configured over strict MVVM and Clean boundaries using Hilt DI and Room caching to preserve state across rotation vectors. |

<details>
<summary><b>✨ View Interface Design (Click to Expand)</b></summary>
<br/>
<table width="100%">
 <tr>
  <td width="33.3%" align="center">
    <img src="https://github.com/user-attachments/assets/82606a97-ce28-4131-8164-315217ac2a7b" width="100%" alt="Screen 1" />
    <br />
    <b>Screen 1: Home Screen</b> 
  </td>
  <td width="33.3%" align="center">
    <img src="https://github.com/user-attachments/assets/b45857ca-262c-4c76-bdbf-c184bb9ca9d6" width="100%" alt="Screen 2" />
    <br />
    <b>Screen 2: Document Summarization</b> <!-- Yahan apna text likhein -->
  </td>
  <td width="33.3%" align="center">
    <img src="https://github.com/user-attachments/assets/faf8bd6e-04d3-452a-8ed1-dd79fd66643c" width="100%" alt="Screen 3" />
    <br />
    <b>Screen 3: Video Screen</b> <!-- Yahan apna text likhein -->
  </td>
</tr>

<!-- Row 2 -->
<tr>
  <td width="33.3%" align="center">
    <img src="https://github.com/user-attachments/assets/456d56d2-b7e8-4c93-af27-e5163d7d2757" width="100%" alt="Screen 4" />
    <br />
    <b>Screen 4: Video Summarization</b> <!-- Yahan apna text likhein -->
  </td>
  <td width="33.3%" align="center">
    <img src="https://github.com/user-attachments/assets/3a85ff3b-c010-4080-956a-a394e025c098" width="100%" alt="Screen 5" />
    <br />
    <b>Screen 5: Audio Screen</b> <!-- Yahan apna text likhein -->
  </td>
  <td width="33.3%" align="center">
    <img src="https://github.com/user-attachments/assets/b5db7d14-8f29-4267-9e52-9a90b57f718e" width="100%" alt="Screen 6" />
    <br />
    <b>Screen 6: Audio Summarization</b> <!-- Yahan apna text likhein -->
  </td>
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
- Android Studio Ladybug (or newer)
- A Google Gemini API Key
- Firebase Project setup
