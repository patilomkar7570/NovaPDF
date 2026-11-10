# NOVA PDF — Private, Offline Document Reader Ecosystem

<p align="center">
  <img src="assets/branding/nova-logo-black.png" alt="NOVA PDF Logo" width="128" height="128" />
</p>

<p align="center">
  <strong>A modern, privacy-first PDF reader ecosystem for Android and Windows Desktop.</strong><br>
  No advertisements. No analytics. No account requirements. 100% offline and local.
</p>

---

## Overview

**NOVA PDF** is an independent, distraction-free document reading application available across two dedicated platforms:

1. **NOVA PDF Android** — Native Android application designed for phones, foldables, and tablets, featuring responsive multi-column layouts, page thumbnail navigation, and offline text search.
2. **NOVA PDF Desktop** — Native Windows desktop application featuring fast PDF.js rendering, page thumbnails, bookmarks, document history, text search, print support, and full keyboard navigation.

Both editions share a unified visual identity, minimal aesthetic, and a strict local-first privacy standard: your files never leave your device.

---

## Key Features

| Capability | NOVA PDF Android | NOVA PDF Windows Desktop |
| :--- | :--- | :--- |
| **Engine** | Native Android `PdfRenderer` + PDFBox-Android | PDF.js + Electron + React + TypeScript |
| **Layouts** | Adaptive: Compact (<600dp), Medium (600–839dp), Expanded (840dp+) | Professional desktop window, resizable, collapsible sidebar |
| **Document Navigation** | Page switcher, direct page jump dialog, thumbnail rail | Prev/Next, direct page input, multi-page thumbnail sidebar |
| **Text Search** | In-document text extraction with match jumping | Real-time text search with occurrence counter and match stepping |
| **Bookmarks** | Per-document bookmark management | Instant bookmarking, persistent sidebar list, quick jump |
| **Reading State** | Automatically remembers last page read per document | Remembers last document and page position per document |
| **Themes** | High-contrast Light and Dark modes | High-contrast Light and Dark modes with instant toggle |
| **Display Zoom** | Multi-level zoom controls with reset & fit | Preset zoom levels, Fit to Width, Fit to Page, manual zoom |
| **Print Support** | System document intent integration | Native Windows print dialog integration (`Ctrl + P`) |
| **File Opening** | System document picker & Android Share Intent | Native Windows file picker (`Ctrl + O`) & Drag and Drop |
| **Privacy & Offline** | 0 trackers, 0 ads, 0 analytics, no internet permission | 100% offline, local persistence, isolated Electron sandbox |

---

## UI & Screenshots

```
+-------------------------------------------------------------------------------+
|  NOVA PDF Desktop                                          [ - ] [ # ] [ X ]  |
+-------------------------------------------------------------------------------+
| [NOVA] Open PDF (Ctrl+O) | [<<] Page [ 12 ] / 45 [>>] | Zoom [100% v] | [Search] [★] |
+------------------+------------------------------------------------------------+
| Thumbnails       |                                                            |
| +--------------+ |                       DOCUMENT CANVAS                      |
| |    Page 1    | |                                                            |
| +--------------+ |                                                            |
| +--------------+ |                                                            |
| |  [Page 12]   | |                                                            |
| +--------------+ |                                                            |
| +--------------+ |                                                            |
| |    Page 13   | |                                                            |
| +--------------+ |                                                            |
+------------------+------------------------------------------------------------+
```

---

## Current Versions & Compatibility

- **NOVA PDF Android:** `v1.1.0` (versionCode: `2`)
  - Application ID: `app.stillpdf.reader` *(preserved for seamless upgrade compatibility)*
  - Minimum Android Version: Android 6.0 (API 23)
  - Target SDK: Android 14 (API 34)
  - Compile SDK: Android 15 (API 35)
  - Java Version: Java 17
- **NOVA PDF Desktop:** `v1.0.0`
  - Platform: Windows 10 / Windows 11 (x64)
  - Distribution: Windows Installer (`NOVA-PDF-Windows-Setup-v1.0.0.exe`)

---

## Downloads

### 📱 Android Installation

Download the official signed release APK:
- [Download NOVA PDF Android v1.1.0 APK](https://github.com/patilomkar7570/StillPDF/releases) *(Artifact name: `NOVA-PDF-Android-v1.1.0.apk`)*

**Installation steps:**
1. Download the `.apk` file to your Android phone or tablet.
2. Tap the downloaded file. When prompted, allow installing applications from your browser or file manager.
3. Tap **Install**. If updating from v1.0.0, your recent documents, bookmarks, and preferences will be preserved automatically.

### 💻 Windows Desktop Installation

Download the official Windows installer:
- [Download NOVA PDF Windows Setup v1.0.0](https://github.com/patilomkar7570/StillPDF/releases) *(Artifact name: `NOVA-PDF-Windows-Setup-v1.0.0.exe`)*

**Installation steps:**
1. Download `NOVA-PDF-Windows-Setup-v1.0.0.exe`.
2. Double-click the installer and choose your preferred installation directory.
3. Launch NOVA PDF from your Desktop or Start Menu.

---

## Keyboard Shortcuts (Desktop)

| Shortcut | Action |
| :--- | :--- |
| **Ctrl + O** | Open PDF via native Windows file dialog |
| **Ctrl + F** | Open or toggle the text search bar |
| **Ctrl + P** | Open native Windows print dialog |
| **Ctrl + B** | Toggle collapsible sidebar (Thumbnails / Bookmarks / Recents) |
| **Ctrl + +** / **Ctrl + =** | Zoom in (+25%) |
| **Ctrl + -** | Zoom out (-25%) |
| **Ctrl + 0** | Reset zoom to 100% |
| **PageDown** / **Right Arrow** / **Space** | Next page |
| **PageUp** / **Left Arrow** | Previous page |
| **Home** | First page |
| **End** | Last page |
| **Escape** | Dismiss search bar or active dialog |

---

## Building from Source

### 1. Android Application (`app/`)

**Prerequisites:**
- JDK 17
- Android SDK Platform 35 & Build-Tools 35.0.0
- Gradle 8.9+

**Build commands:**

```bash
# Clone the repository
git clone https://github.com/patilomkar7570/StillPDF.git
cd StillPDF

# Build Debug APK
gradle assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk

# Build Release APK (requires signing environment variables)
export ANDROID_KEYSTORE_FILE="/path/to/keystore.jks"
export ANDROID_KEYSTORE_PASSWORD="your-keystore-password"
export ANDROID_KEY_ALIAS="stillpdf"
export ANDROID_KEY_PASSWORD="your-key-password"

gradle assembleRelease

# Output APK location:
# app/build/outputs/apk/release/app-release.apk
```

### 2. Windows Desktop Application (`desktop/`)

**Prerequisites:**
- Node.js LTS (v20+ or v22+)
- npm 10+
- Windows 10/11 x64 build environment

**Build commands:**

```bash
# Navigate to the desktop directory
cd desktop

# Install dependencies
npm ci

# Run development server with live reload
npm run dev

# Build production assets (Vite React + Electron TypeScript)
npm run build

# Package Windows NSIS x64 Installer
npm run dist

# Output installer location:
# desktop/release/NOVA-PDF-Windows-Setup-v1.0.0.exe
```

---

## GitHub Actions CI/CD Automation

This repository includes automated GitHub Actions workflows:

### Android Release Workflow (`.github/workflows/build-apk.yml`)
- Triggers on manual dispatch (`workflow_dispatch`) and pushes to `main`, `master`, and `feature/**`.
- Compiles with Java 17, Android SDK 35, and Gradle 8.9.
- Decodes the release keystore using repository secret `ANDROID_KEYSTORE_BASE64`.
- Signs the APK using `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`.
- Verifies APK signature with `apksigner` and `keytool`.
- Uploads the signed APK artifact as `NOVA-PDF-Android-v1.1.0.apk`.

### Windows Desktop Workflow (`.github/workflows/build-desktop.yml`)
- Triggers on manual dispatch (`workflow_dispatch`) and pushes with changes in `desktop/**`.
- Runs on `windows-latest` with Node.js LTS.
- Builds the React front-end and Electron TypeScript back-end.
- Packages the standalone installer using `electron-builder`.
- Uploads the Windows installer artifact as `NOVA-PDF-Windows-Setup-v1.0.0.exe`.

---

## Privacy Policy & Architecture

NOVA PDF is designed from the ground up as a private, offline tool:

- **Zero Cloud Dependence:** PDF parsing, rendering, text stripping, and page searching are performed entirely on the local CPU/GPU.
- **Zero Telemetry:** No tracking libraries, analytics SDKs, error reporting beacons, or advertisement networks are present in either project.
- **No Internet Permissions:** The Android app requests neither `android.permission.INTERNET` nor `ACCESS_NETWORK_STATE`.
- **Local Persistence:** Reading position, bookmarks, and UI preferences are stored in private local app storage (`SharedPreferences` on Android, local JSON in user data on Windows).

---

## License

This project is licensed under the [MIT License](LICENSE).
Third-party component notices and licenses are documented in [NOTICE.md](NOTICE.md).
