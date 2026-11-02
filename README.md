# StillPDF — private, ad-free PDF reader

StillPDF is a small Android PDF reader designed for personal use. It has no ads, analytics, account system, internet permission, or tracking SDK.

## Features

- Open PDF files using Android's system document picker.
- Render PDF pages offline using Android `PdfRenderer`.
- Search for words/phrases in selectable PDF text using PDFBox-Android.
- Save and remove page bookmarks.
- Remember the last page for each recent document.
- Recent documents and bookmarks saved on-device.
- Light and dark app themes.
- Accept PDF files opened/shared from other apps.

### Known limitations

- Search only works on text embedded in the PDF. Scanned/image-only PDFs require OCR; OCR is not included.
- This is a first version, not yet device-tested in this environment.
- Password-protected, damaged, or unusual PDFs may fail to open.
- The first time you build the app, Gradle downloads the Android build plugin and PDFBox dependency.

## Build an APK in Android Studio

1. Install Android Studio on a computer and let it install a compatible Android SDK.
2. Extract the `StillPDF` folder from this ZIP.
3. In Android Studio, choose **Open** and select the extracted `StillPDF` folder.
4. Allow Gradle sync to finish. If prompted, install Android SDK Platform 35 and Java 17.
5. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
6. Android Studio will show a link to the debug APK. The expected output path is:

   `app/build/outputs/apk/debug/app-debug.apk`

7. Transfer that APK to your Android phone, tap it, and allow installation for the file app you used to open it. Only install APKs you built or otherwise trust.

The debug APK is intended for personal use. It is not a Play Store release build.

## Build using GitHub Actions

The included `.github/workflows/build-apk.yml` builds a debug APK on a push to the `main` or `master` branch, or when manually started from the repository's **Actions** tab. After a successful run, open the workflow run and download the `StillPDF-debug-apk` artifact. GitHub keeps the artifact for 14 days.

## Technical details

- Package: `app.stillpdf.reader`
- Version: `1.0.0`
- Minimum Android version: Android 6.0 (API 23)
- Compile SDK: 35
- Target SDK: 34
- PDF rendering: Android `PdfRenderer`
- Text extraction/search: PDFBox-Android `2.0.27.0`

All app-owned preferences and cached document copies are stored in private app storage. The app does not request Internet access. Removing the app also removes its private local data/cache.
