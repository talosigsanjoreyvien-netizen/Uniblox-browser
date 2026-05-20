# Uniblox Browser

Uniblox browser is a fast, smooth, and homemade product for UNIBLOX. Built with modern Android technologies and verified on the Uniblox App Store.

## Features

- ⚡ Fast web browsing performance
- 🎨 Material Design UI
- 🔐 Uniblox App Store verified (UPK token protected)
- 📱 Built for Android 21+
- 🛡️ Anti-hacking verification system

## Building with AndroidIDE / AIDE (Android)

1. **Clone the project**
   - Use your IDE's clone feature (e.g. Git → Clone in AndroidIDE).
   - URL: `https://github.com/talosigsanjoreyvien-netizen/Uniblox-browser`

2. **Gradle Sync**
   - Wait for the IDE to finish the Gradle sync process. The IDE will download and install Gradle automatically. Do NOT manually create a `gradlew` script.

3. **Build and Run**
   - Click the **Play** or **Run** (Shift + F10) button.
   - For AIDE: Click **Build** → **Run**.
   - For terminal builds: `gradle assembleDebug`

## Building with Android Studio (Desktop)

1. Clone the repository
2. Open in Android Studio
3. Wait for Gradle sync (IDE uses system/embedded Gradle)
4. Click **Run** (Shift + F10)

## Project Structure

```
Uniblox-browser/
├── app/
│   ├── src/main/
│   │   ├── java/            # Java source code
│   │   ├── res/             # Resources (layouts, drawables)
│   │   └── AndroidManifest.xml
│   ├── build.gradle         # App-level build config
│   └── proguard-rules.pro   # ProGuard obfuscation rules
├── gradle/
│   └── libs.versions.toml   # Dependency version catalog
├── build.gradle             # Project-level build config
├── settings.gradle          # Gradle settings
├── executable.upk.txt       # Uniblox verification token
└── README.md
```

## Configuration

### Environment Variables
- Create `.env` file in project root if needed
- Key signing requires `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`

### API Keys
- Set Gemini API key in `.env` if using AI features
- Format: `GEMINI_API_KEY=your_key_here`

## Troubleshooting

### "gradle: command not found"
- **Cause:** Gradle is not in your system PATH or not installed in the IDE's terminal.
- **Fix:** Use the IDE's built-in **Build** button instead of the terminal. In Termux, run `pkg install gradle`.

### AIDE Gradle Sync Issues
- If AIDE fails to sync, ensure you have a stable internet connection for the IDE to download the Gradle distribution.
- Use **Build** → **Clean Project** to reset the build state.

### "Could not find or load main class" Error
- **Cause:** Java/JDK not configured properly.
- **Fix:** 
  1. Go to IDE Settings → SDK Manager.
  2. Ensure Java 11 or 17 is selected.
  3. Click **Build** → **Clean** → **Build** again.

## Uniblox App Store Verification

This app includes an `executable.upk.txt` file containing a verification token:
```
uniblox-app-verification-token: "jj-hhhgeffcggggg&+8-++jjjjjjjughkguhiygirvhfyt=="
```

This token:
- ✅ Verifies the app on Uniblox App Store
- ✅ Prevents unauthorized/hacked versions
- ✅ Ensures app integrity
- **Do NOT remove or modify this file**

## Release Build

To build a release APK:

1. Configure signing key in `app/build.gradle`
2. Set environment variables:
   ```bash
   export KEYSTORE_PATH=/path/to/keystore
   export STORE_PASSWORD=your_password
   export KEY_PASSWORD=your_key_password
   ```
3. Run: `gradle assembleRelease`

## Dependencies

- AndroidX AppCompat
- Material Design
- Retrofit & OkHttp
- Firebase (optional)
- Room Database
- Moshi JSON

## Support

For issues or feature requests, open an issue on GitHub.

## License

This project is part of the Uniblox ecosystem. All rights reserved.

---

**Built with ❤️ for Uniblox**
