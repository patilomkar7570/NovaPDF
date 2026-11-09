# StillPDF — private, ad-free PDF reader

StillPDF is a lightweight Android PDF reader designed around privacy and offline use. It has no advertisements, analytics, account system, internet permission, or tracking SDK.

**By NOVA** · An independent app in the NOVA ecosystem.

## Features

- Open PDF files using Android's system document picker.
- Render PDF pages offline using Android `PdfRenderer`.
- Search for words and phrases in selectable PDF text using PDFBox-Android.
- Save and remove page bookmarks.
- Remember the last page for each document.
- View recently opened documents and saved bookmarks.
- Switch between light and dark themes.
- Open or share PDF files from other apps.

## Privacy

- No ads, accounts, analytics, or tracking.
- No Internet permission is requested.
- Documents are processed locally on your device.
- App preferences and cached document copies are stored in private app storage.
- Uninstalling the app removes its private local data and cache.

## Known limitations

- Search works only with text embedded in the PDF. Scanned or image-only PDFs require OCR; OCR is not included.
- Password-protected, damaged, or unusual PDFs may fail to open.

## Download

The latest public release is **StillPDF v1.0.0**.

- [Download StillPDF v1.0.0 APK](https://github.com/patilomkar7570/StillPDF/releases/download/v1.0.0/Still.PDF.apk)
- [View all releases](https://github.com/patilomkar7570/StillPDF/releases)

Download the APK to your Android device and install it. Android may ask you to allow installation from the browser or file manager you used. Only install APKs from sources you trust.

**Minimum Android version:** Android 6.0 (API 23).

## Build an APK in Android Studio

1. Install Android Studio and a compatible Android SDK.
2. Open the `StillPDF` project folder in Android Studio.
3. Allow Gradle sync to finish. If prompted, install Android SDK Platform 35 and Java 17.
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Android Studio will provide the debug APK. The expected output path is:

   `app/build/outputs/apk/debug/app-debug.apk`

The debug APK is intended for development and testing. It is not the signed public release APK.

## Build using GitHub Actions

The workflow at `.github/workflows/build-apk.yml` builds a **signed release APK** when code is pushed to the `main` or `master` branch, or when the workflow is started manually from the repository's **Actions** tab.

After a successful run:

1. Open the completed **Build StillPDF APK** workflow run.
2. Find the `StillPDF-signed-release-apk` artifact.
3. Download the artifact ZIP and extract `app-release.apk`.

The workflow uses GitHub Actions repository secrets for the Android release-signing keystore and passwords. These signing credentials must be configured by the repository maintainer. Workflow artifacts are retained for 30 days.

## Technical details

- Package: `app.stillpdf.reader`
- Current version: `1.0.0`
- Minimum Android version: Android 6.0 (API 23)
- Compile SDK: 35
- Target SDK: 34
- Java: 17
- PDF rendering: Android `PdfRenderer`
- Text extraction and search: PDFBox-Android `2.0.27.0`

StillPDF is an offline-first project. It does not request Internet access.
