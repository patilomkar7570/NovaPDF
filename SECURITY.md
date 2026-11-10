# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Privacy & Security Model

NOVA PDF and NOVA PDF+ operate on a **zero-trust, privacy-first architecture**:

- **Zero Network Egress:** The Android application declares no internet permissions (`android.permission.INTERNET`). It physically cannot transmit document data, telemetry, or user inputs to external servers.
- **In-Memory Web Processing:** NOVA PDF+ executes 100% client-side in the user's local web browser via WebAssembly and HTML5 Canvas. Documents and OCR processing never leave the host machine.
- **Strict Input Sanitization:** All document metadata and file properties undergo rigorous escaping and DOM encapsulation to prevent injection and XSS attacks.
- **No Analytics / No Tracking:** Zero analytics SDKs, trackers, third-party beacons, or telemetry libraries are bundled.

## Reporting a Vulnerability

If you discover a security vulnerability or bug in NOVA PDF, please report it responsibly:

1. Open a private issue or advisory via GitHub's [Security Advisory](https://github.com/patilomkar7570/NovaPDF/security/advisories) tab.
2. Provide a detailed summary of the issue, affected versions, and steps to reproduce.
3. Vulnerabilities will be addressed and patched promptly in the next release.
