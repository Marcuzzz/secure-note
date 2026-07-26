# SecureNote

An Android app for storing notes and credentials, encrypted end-to-end on device with a master password, with optional biometric unlock and a built-in random password generator.

## Features

- **Master password** protects everything. Key is derived with PBKDF2-HMAC-SHA256 (200,000 iterations).
- **SQLCipher-encrypted Room database.** The whole DB file is encrypted with AES-256; the master password never leaves the device.
- **Biometric unlock.** After enrolling, the raw key is wrapped by an Android Keystore-backed AES-GCM key that requires biometric authentication to release.
- **Notes & credentials.** Title, URL, username, password, and free-form body per entry. Search across all fields.
- **Password generator.** Configurable length (6–64), character classes, ambiguous-character avoidance, strength meter (bits of entropy). Uses `SecureRandom`.
- **Copy protections.** Password copy is flagged sensitive on Android 13+ so it doesn't show in the clipboard preview.
- **Screenshot & recents protection.** `FLAG_SECURE` is set on the window.
- **Auto-lock.** Vault locks whenever the app moves to the background.
- **No backups.** Cloud backup and device-transfer of app data are disabled.

## Requirements

- Android Studio Iguana (or newer)
- JDK 17
- Android SDK 34
- `minSdk` 26 (Android 8.0)

## Build locally

```bash
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or run/debug on a device from Android Studio.

## Build and host on GitHub

A GitHub Actions workflow is included at [.github/workflows/build.yml](.github/workflows/build.yml).

Every push or PR:
- builds the debug APK
- uploads it as a workflow artifact named `SecureNote-debug-apk` (downloadable from the Actions run page)

Every tag matching `v*` (e.g. `v1.0.0`):
- creates a GitHub Release
- attaches `SecureNote-debug.apk` to it

To publish a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

After the workflow finishes, the APK is available at `https://github.com/<user>/<repo>/releases/tag/v1.0.0`.

### Notes on the CI build
- The workflow uses Temurin JDK 17 and Gradle 8.7.
- It auto-generates `gradle/wrapper/gradle-wrapper.jar` if it's missing so you don't have to commit the binary. Commit it if you'd rather pin the wrapper explicitly.
- The output is a **debug-signed** APK. Debug builds are fine for personal side-loading but Play Store distribution requires a release keystore. To add release signing, create a keystore, set it in `app/build.gradle.kts` under `signingConfigs`, and store the credentials as GitHub Actions secrets. Ask if you want that wired up.

## Security architecture

```
Master password
     │  PBKDF2-HMAC-SHA256 (200k iter, 16-byte salt)
     ▼
32-byte raw key ─────────────────────────► SQLCipher passphrase (encrypts entire DB)
     │
     │  wrapped by AES-GCM
     │  (Keystore, requires biometric auth)
     ▼
biometric_wrapped blob in EncryptedSharedPreferences
```

- The raw key exists in RAM only while the vault is unlocked. `VaultSession.lock()` zeros the byte array and closes the DB.
- A **verifier hash** (SHA-256 of the derived key + a fixed constant) is stored so the app can detect a wrong password without persisting the key itself.
- Losing the master password means losing the notes — there's no recovery path by design.

## Project structure

```
app/src/main/java/com/example/securenote/
├── SecureNoteApp.kt              # Application class + DI container
├── data/                         # Room entities, DAO, encrypted DB, repository, session
├── security/                     # CryptoUtils, VaultKeyManager, KeystoreCipher
├── util/PasswordGenerator.kt     # Generator + entropy estimator
└── ui/
    ├── MainActivity.kt
    ├── theme/Theme.kt
    ├── nav/SecureNoteNavHost.kt
    ├── vm/                       # UnlockViewModel, NoteListViewModel, NoteEditViewModel
    └── screens/                  # UnlockScreen, NoteListScreen, NoteEditScreen, PasswordGeneratorScreen
```

## Known limitations / next steps

- Biometric **enrollment** UI is not yet wired into the unlock flow — the pieces exist in `VaultKeyManager` (`cipherForBiometricEnroll`, `enrollBiometric`). Add a settings screen to prompt for it after first unlock.
- No autofill service integration.
- No encrypted export/import.
- Release signing not configured.
