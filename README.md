# NOVA PDF — Private, Offline PDF Reader for Android & PC

<p align="center">
  <img src="assets/branding/nova-logo-black.png" alt="NOVA PDF Logo" width="128" height="128" />
</p>

<p align="center">
  <strong>A modern, distraction-free, privacy-first document reader for Android phones, tablets, and PC browsers.</strong><br>
  Zero ads. Zero tracking. Zero cloud uploads. 100% offline and local.
</p>

<p align="center">
  <a href="https://github.com/patilomkar7570/NovaPDF/actions/workflows/build-apk.yml"><img src="https://github.com/patilomkar7570/NovaPDF/actions/workflows/build-apk.yml/badge.svg" alt="CI Build Status" /></a>
  <a href="https://github.com/patilomkar7570/NovaPDF/releases/tag/v1.0.0"><img src="https://img.shields.io/github/v/release/patilomkar7570/NovaPDF?color=3267E8&label=Release" alt="Latest Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License: MIT" /></a>
  <a href="#edition-comparison"><img src="https://img.shields.io/badge/Platforms-Android%20%7C%20PC%20Web-3267E8.svg" alt="Platforms" /></a>
  <a href="SECURITY.md"><img src="https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg" alt="100% Offline" /></a>
</p>

---

## Quick Links

- [Overview](#overview)
- [Edition Comparison](#edition-comparison)
- [NOVA PDF (Android Edition)](#nova-pdf--android-edition)
- [NOVA PDF+ (PC Browser Edition)](#nova-pdf--pc-edition-browser-app)
- [Installation & Downloads](#installation--downloads)
- [Keyboard Shortcuts Reference](#keyboard-shortcuts-reference)
- [Technical Specifications](#technical-specifications)
- [Building from Source](#building-from-source)
- [Privacy & Security](#privacy-policy)
- [License](#license)

---

## Overview

**NOVA PDF** is an independent, native document reader engineered for speed, privacy, and readability across all devices.

Whether on a compact smartphone, a foldable device, a large Android tablet, or a desktop computer, NOVA PDF provides a responsive, distraction-free reading experience without telemetry, user accounts, or cloud dependencies.

All document rendering, text extraction, and OCR run strictly on your local device.

---

## Edition Comparison

| Feature | 📱 NOVA PDF (Android) | 💻 NOVA PDF+ (PC Browser) |
| :--- | :---: | :---: |
| **Target Devices** | Phones, Tablets, Foldables (Android 6.0+) | Windows, macOS, Linux, ChromeOS |
| **Installation** | Signed APK install | **Zero-Install** (Single `.html` file) |
| **Privacy & Storage** | 100% Offline & Local | 100% Client-Side In-Memory |
| **Hardware Accelerated Rendering** | ✅ Android `PdfRenderer` | ✅ WebAssembly / HTML5 Canvas |
| **Full Text Search** | ✅ Embedded PDFBox-Android | ✅ Fast In-Document Engine |
| **Smart Text (OCR Engine)** | Native digital text | ✅ **Tesseract.js OCR for scanned PDFs** |
| **Deep Document Inspector** | Standard Document Info | ✅ **10 Hidden Technical Properties** |
| **Adaptive Layouts** | Single & Two-Column responsive | Responsive PC Desktop Layout |
| **Light & Dark Themes** | ✅ High-contrast dual modes | ✅ High-contrast dual modes |
| **Bookmarks & History** | ✅ Local SharedPreferences | ✅ LocalStorage (Sanitized) |
| **Keyboard Navigation** | ✅ Hardware keyboard shortcuts | ✅ Full desktop hotkeys |

---

## NOVA PDF — Android Edition

### Key Capabilities
- **⚡ Fast Native Rendering:** Powered by Android's hardware-accelerated `PdfRenderer` for sharp, high-DPI rendering without bloat.
- **📱 Phone & Tablet Adaptive Layouts:**
  - **Compact (<600dp):** Streamlined single-column reading experience with clean controls and easy thumb navigation.
  - **Medium (600–839dp):** Two-column document library (Recent Documents and Bookmarks side-by-side).
  - **Expanded (840dp+):** Collapsible thumbnail navigation sidebar, dual-column library, and spacious reading canvas.
- **🔍 Full Text Search:** In-document text extraction and searching powered by PDFBox-Android with match stepping.
- **📑 Page Thumbnails Panel:** Collapsible thumbnail rail for rapid visual scanning across pages.
- **★ Bookmarks & Reading History:** Bookmark important pages and resume reading exactly where you left off.
- **🛡️ Memory & OOM Protection:** Bounded canvas dimensions and automatic fallback for high-ratio architectural drawings and receipts.
- **🔒 Absolute Privacy:** Zero trackers, zero ads, zero analytics, zero network permissions.

---

## NOVA PDF+ — PC Edition (Browser App)

**NOVA PDF+** is the standalone desktop edition designed specifically for PC and Mac users. It runs entirely inside any modern web browser (Edge, Chrome, Firefox, Safari, Brave, Opera) as a lightweight, single-file application (`NovaPDF-Plus.html`) with zero installation or local server required.

### 🌟 Exclusive Features for PC
- **🔤 Smart Text (OCR Engine):** Convert image-based, photographed, and scanned PDFs into clickable, selectable, and copyable text layers on demand. Perfect for scanned books, historical documents, receipts, and photographed handouts.
- **ℹ️ Deep Document Inspector ("More Information"):** Uncover technical document properties and hidden structural attributes not exposed by standard Windows File Explorer or common PDF viewers:
  - Exact bounding box dimensions in typographic points (`pt`) and millimeters (`mm`)
  - PDF specification version (1.4 through 2.0)
  - Linearization status (fast web stream vs. standard object table)
  - PDF producer, creation application, and author toolchains
  - High-precision UTC creation and modification timestamps
  - Encryption and document rights / copy restrictions
  - Document title and total page count
- **📑 Adaptive Reading Workspace:** Resizable thumbnail sidebar, zoom presets (Fit Width, Fit Page, 75%–200%), in-page text search, reading history, and local bookmarks.
- **🔒 100% Offline & Private:** Zero document data or images are ever uploaded. All rendering and OCR run client-side in your browser via WebAssembly and HTML5 Canvas.

---

## Installation & Downloads

### 📱 Android Phone & Tablet (APK)

Download the official signed release APK directly from the [GitHub Releases](https://github.com/patilomkar7570/NovaPDF/releases) page:

- **File Name:** `NOVA-PDF-Android-v1.0.apk`
- **Version:** `1.0` (Release `1.0.0`)
- **Requirements:** Android 6.0 Marshmallow (API 23+) or higher

#### Android Installation Steps:
1. Download `NOVA-PDF-Android-v1.0.apk` to your Android device.
2. Open your device's file manager or browser downloads and tap the APK.
3. If prompted, allow "Install unknown apps" from your file manager or browser.
4. Tap **Install** (or **Update**).
5. All your existing recent documents, bookmarks, and preferences are automatically preserved!

### 💻 PC Desktop Edition (Standalone HTML)

- **File Name:** `NovaPDF-Plus.html`
- **Supported Browsers:** Google Chrome, Microsoft Edge, Mozilla Firefox, Apple Safari, Brave, Opera.
- **How to Use:**
  1. Download `NovaPDF-Plus.html` from the [GitHub Releases](https://github.com/patilomkar7570/NovaPDF/releases) page.
  2. Double-click `NovaPDF-Plus.html` to open it in your default browser.
  3. Drag and drop any PDF file into the window to read, search, OCR, or inspect it.

---

## Keyboard Shortcuts Reference

Both the Android edition (with hardware keyboard) and the PC Edition support full keyboard navigation:

| Action | PC Shortcut | Android Shortcut |
| :--- | :---: | :---: |
| **Next Page** | `→` / `Page Down` / `Space` | `→` / `Page Down` / `Space` |
| **Previous Page** | `←` / `Page Up` | `←` / `Page Up` |
| **Zoom In (+25%)** | `+` or `=` | `Ctrl + Plus` / `Ctrl + =` |
| **Zoom Out (-25%)** | `-` | `Ctrl + Minus` |
| **Reset Zoom (100%)** | `0` | Tap Zoom Indicator |
| **Search Document** | `Ctrl + F` / `Cmd + F` | `Ctrl + F` |
| **Next Search Match** | `Enter` (in search) | `Next` Button |
| **Toggle Bookmark** | `Ctrl + B` / `Cmd + B` | `Ctrl + B` |
| **Close Modals / Drawers**| `Escape` | `Escape` / `Back` |

---

## Technical Specifications

| Parameter | Android Edition | PC Edition (NOVA PDF+) |
| :--- | :--- | :--- |
| **Application ID / Format** | `app.stillpdf.reader` | Single-File HTML5 / JavaScript |
| **Version** | `1.0` (VersionCode `3`) | `1.0` |
| **Runtime Target** | Android API 23 to API 35 | Evergreen Web Browsers |
| **Language** | Native Java 17 | HTML5, CSS3, ES2022 |
| **PDF Rendering Engine** | Android `PdfRenderer` | PDF.js 3.11.174 |
| **Text Extraction Engine** | PDFBox-Android 2.0.27.0 | PDF.js TextLayer API |
| **OCR Engine** | N/A | Tesseract.js 5 (Wasm) |
| **Network Permissions** | **None** (`android.permission.INTERNET` omitted) | **None** (Local client-side execution) |
| **Distribution Artifact** | `NOVA-PDF-Android-v1.0.apk` | `NovaPDF-Plus.html` / `index.html` |

---

## Building from Source

### Android APK Build

#### Prerequisites:
- JDK 17
- Android SDK (Build Tools 35.0.0, Platform API 35)
- Gradle 8.9

#### Build Debug APK:
```bash
gradle assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Build Signed Release APK:
```bash
export ANDROID_KEYSTORE_FILE="/path/to/keystore.jks"
export ANDROID_KEYSTORE_PASSWORD="your-keystore-password"
export ANDROID_KEY_ALIAS="your-key-alias"
export ANDROID_KEY_PASSWORD="your-key-password"

gradle assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

---

## Continuous Integration (GitHub Actions)

The repository includes an automated CI/CD release workflow located at:
`.github/workflows/build-apk.yml`

On every push to `main` or manual `workflow_dispatch`, GitHub Actions:
1. Sets up JDK 17, Gradle 8.9, and Android SDK Build Tools 35.0.0.
2. Decodes the signing keystore securely from repository secrets (`ANDROID_KEYSTORE_BASE64`).
3. Compiles the release build with `gradle assembleRelease`.
4. Validates the signature scheme with `apksigner verify --verbose` and prints certificate details with `keytool`.
5. Uploads `NOVA-PDF-Android-v1.0.apk` as a workflow artifact.

---

## Privacy Policy

NOVA PDF and NOVA PDF+ do not collect, transmit, store, or share any personal information, usage metrics, or document contents.
All PDF reading, search, and OCR operations are performed entirely in-memory and on your local device.

For more details, review our [Security Policy](SECURITY.md).

---

## License

This project is licensed under the [MIT License](LICENSE).
Third-party component licenses are documented in [NOTICE.md](NOTICE.md).
