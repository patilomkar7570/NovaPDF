# NOVA PDF — Private, Offline PDF Reader for Android

<p align="center">
  <img src="assets/branding/nova-logo-black.png" alt="NOVA PDF Logo" width="128" height="128" />
</p>

<p align="center">
  <strong>A modern, distraction-free, privacy-first PDF reader for Android phones, foldables, and tablets.</strong><br>
  No advertisements. No analytics. No account requirements. 100% offline and local.
</p>

---

## Overview

**NOVA PDF** is an independent, native Android document reader engineered for speed, privacy, and readability across all form factors.

Whether reading on a compact smartphone, a foldable device, or a large Android tablet, NOVA PDF adapts smoothly to your screen size with responsive multi-column layouts, page thumbnails, in-document text search, bookmarks, and last-read page restoration.

All processing occurs entirely on your device. The application does not request internet permission and never transmits your documents.

---

## Key Features

- **⚡ Fast Native Rendering:** Powered by Android's hardware-accelerated `PdfRenderer` for sharp, high-DPI rendering without bloat.
- **📱 Phone & Tablet Adaptive Layouts:**
  - **Compact (<600dp):** Streamlined single-column reading experience with clean controls and easy thumb navigation.
  - **Medium (600–839dp):** Two-column document library (Recent Documents and Bookmarks side-by-side) with expanded reading controls.
  - **Expanded (840dp+):** Collapsible thumbnail navigation sidebar, dual-column library, and spacious reading area.
- **🔍 Full Text Search:** In-document text extraction and searching powered by PDFBox-Android with match stepping and navigation.
- **📑 Page Thumbnails Panel:** Collapsible thumbnail rail for rapid visual scanning across pages.
- **★ Bookmarks & Reading History:** Bookmark important pages and resume reading exactly where you left off.
- **🌗 Light & Dark Themes:** High-contrast reading themes designed to minimize eye strain in any lighting condition.
- **🔍 Multi-Level Zoom & Jump Dialog:** Smooth zoom controls (50% to 300%) with reset button and direct page jump dialog.
- **⌨️ Keyboard & Hardware Support:** Full navigation support via arrow keys, Page Up/Down, Space, and shortcuts (`Ctrl + F`, `Ctrl + B`, `Ctrl + Plus`, `Ctrl + Minus`).
- **🔒 Absolute Privacy:** Zero trackers, zero ads, zero analytics, zero cloud connections.

---

## Technical Specifications

| Parameter | Value |
| :--- | :--- |
| **Application ID** | `app.stillpdf.reader` *(Preserved for 100% upgrade compatibility with previous installs)* |
| **Version Name** | `1.0` |
| **Version Code** | `3` |
| **Minimum SDK** | Android 6.0 Marshmallow (API 23) |
| **Target SDK** | Android 14 (API 34) |
| **Compile SDK** | Android 15 (API 35) |
| **Language** | Native Java 17 |
| **Rendering Engine** | Android `PdfRenderer` |
| **Search Engine** | PDFBox-Android 2.0.27.0 |
| **Distribution** | Signed Release APK (`NOVA-PDF-Android-v1.0.apk`) |

---

## Installation

### Download Signed Release APK

You can download the official signed release APK directly from the [GitHub Releases](https://github.com/patilomkar7570/NovaPDF/releases) page or workflow artifacts:

- **Artifact Name:** `NOVA-PDF-Android-v1.0.apk`

### Installation Steps

1. Transfer or download `NOVA-PDF-Android-v1.0.apk` to your Android device.
2. Open your device's file manager or browser downloads and tap the APK.
3. If prompted, allow "Install unknown apps" from your file manager or browser.
4. Tap **Install** (or **Update**).
5. All your existing recent documents, bookmarks, and preferences from previous versions are automatically preserved!

---

## Building from Source

### Prerequisites

- Java Development Kit (JDK) 17
- Android SDK (Build Tools 35.0.0, Platform API 35)
- Gradle 8.9

### Build Debug APK

```bash
gradle assembleDebug
```

The debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Build Signed Release APK

Provide your signing credentials as environment variables:

```bash
export ANDROID_KEYSTORE_FILE="/path/to/keystore.jks"
export ANDROID_KEYSTORE_PASSWORD="your-keystore-password"
export ANDROID_KEY_ALIAS="your-key-alias"
export ANDROID_KEY_PASSWORD="your-key-password"

gradle assembleRelease
```

The signed release APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## Continuous Integration (GitHub Actions)

The repository includes an automated CI/CD release workflow located at:
`.github/workflows/build-apk.yml`

On every push or manual `workflow_dispatch`, GitHub Actions:
1. Sets up JDK 17, Gradle 8.9, and Android SDK Build Tools 35.0.0.
2. Decodes the signing keystore securely from repository secrets (`ANDROID_KEYSTORE_BASE64`).
3. Compiles the release build with `gradle assembleRelease`.
4. Validates the signature scheme with `apksigner verify --verbose` and prints certificate details with `keytool`.
5. Uploads `NOVA-PDF-Android-v1.0.apk` as a workflow artifact.

---

## Privacy Policy

NOVA PDF does not collect, transmit, store, or share any personal information, usage metrics, or document contents.
All PDF reading and search operations are performed entirely in-memory and on-device.

---

## License

This project is licensed under the [MIT License](LICENSE).
Third-party licenses are documented in [NOTICE.md](NOTICE.md).
