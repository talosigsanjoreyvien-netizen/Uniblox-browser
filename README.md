# Uniblox Browser

Uniblox browser is a fast, smooth, and homemade product for UNIBLOX. Built with modern Android technologies and verified on the Uniblox App Store.

## Features

- ⚡ Fast web browsing performance
- 🎨 Material Design UI
- 🔐 Uniblox App Store verified (UPK token protected)
- 📱 Built for Android 21+
- 🛡️ Anti-hacking verification system

## Building with AIDE

### Prerequisites
- AIDE IDE installed on your Android tablet
- Android SDK 21 or higher
- Java 11+

### Steps

1. **Clone the project in AIDE**
   - Open AIDE
   - Select "Open Project"
   - Paste: `https://github.com/talosigsanjoreyvien-netizen/Uniblox-browser`

2. **Build the app**
   - Click **Build** button
   - Wait for compilation to complete
   - APK will be generated in `app/build/outputs/apk/`

3. **Run on device**
   - Click **Run** button
   - Select your device
   - App will install and launch

## Building with Android Studio (Desktop)

1. Clone the repository
2. Open in Android Studio
3. Wait for Gradle sync
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
├── build.gradle             # Project-level build config
├── settings.gradle          # Gradle settings
├── executable.upk.txt       # Uniblox verification token
└── README.md
```

## Configuration

### Environment Variables
- Create `.env` file in project root if needed
- See `.env.example` for reference

### API Keys
- Set Gemini API key in `.env` if using AI features
- Format: `GEMINI_API_KEY=your_key_here`

## Troubleshooting

### "Could not find or load main class" Error
- **Cause:** Java/JDK not configured in AIDE
- **Fix:** 
  1. Go to AIDE Settings → SDK Manager
  2. Install Android SDK and Java 11+
  3. Restart AIDE
  4. Click **Build** → **Clean** → **Build** again

### Build fails with Gradle error
- Click **Build** → **Clean Project**
- Then **Build** → **Build Project** again
- Restart AIDE if issue persists

### APK not installing
- Make sure "Unknown Sources" is enabled in Android Settings
- Try uninstalling previous version first

## Uniblox App Store Verification

This app includes an `executable.upk.txt` file containing a verification token:
```
[!uniblox-app-verification-token={upk.string=[jj-hhhgeffcggggg&+8-++jjjjjjjughkguhiygirvhfyt==]}]
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
