# OpenSpot - Parking & Campus Navigation for WPI
**CS 528: Mobile & Ubiquitous Computing | Worcester Polytechnic Institute | Spring 2026**

**Team:** Glenn D'souza | Ishaan Parekh | Shreelaxmi Malawade

---

## Folder Structure

```
Dsouza_Parekh_Malawade_final_project/
├── slides/         — Final presentation slides (.pptx)
├── video/          — Demo video of the app in action
├── paper/          — Final project paper (.docx)
└── code/           — Full Android source code + this README
    ├── app/
    │   └── src/main/java/com/wpi/openspot/
    │       ├── ui/
    │       │   ├── home/           — Map screen, ViewModel, bottom sheet
    │       │   ├── lots/           — Lots list screen, adapter, ViewModel
    │       │   ├── profile/        — Profile + Edit Profile screens
    │       │   ├── settings/       — Settings screen
    │       │   └── onboarding/     — Splash, Sign In, Register, Scan ID, Confirm
    │       ├── domain/model/       — ParkingLot, LotStatus data classes
    │       └── service/            — LocationService, GeofenceManager, BroadcastReceiver
    ├── app/src/main/res/           — Layouts, navigation graph, drawables, strings
    ├── app/build.gradle.kts        — Dependencies and build config
    └── app/src/main/AndroidManifest.xml
```

---

## Prerequisites

Before building, make sure you have the following installed:

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Hedgehog (2023.1.1) or newer | https://developer.android.com/studio |
| Android SDK | API Level 35 | Via Android Studio SDK Manager |
| JDK | 17 (bundled with Android Studio) | Bundled |
| Kotlin | 2.0.21 | Bundled with Android Studio |

---

## Firebase Setup (Required)

OpenSpot uses Firebase for authentication and real-time data. You need to add the `google-services.json` file before building.

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Open the **OpenSpot** project (or create a new one)
3. Go to **Project Settings** → **Your apps** → **Android app**
4. Download `google-services.json`
5. Place it at: `app/google-services.json`

The Firebase project must have the following enabled:
- **Firebase Authentication** — Email/Password and Google Sign-In providers
- **Firestore Database** — with the following security rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /lots/{lotId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

### Firestore Lot Documents

The `lots` collection must contain the following two documents:

**Document ID:** `lot_park_avenue_garage`
```
name: "Park Avenue Garage"
latitude: 42.2753950640153
longitude: -71.8101986028752
capacity: 534
occupancy: 0
status: "AVAILABLE"
permitTypes: ["Student Commuter"]
lastUpdatedAt: (server timestamp)
```

**Document ID:** `lot_boynton_north`
```
name: "Boynton North"
latitude: 42.27432952097102
longitude: -71.8057836028753
capacity: 50
occupancy: 0
status: "AVAILABLE"
permitTypes: ["Student Commuter"]
lastUpdatedAt: (server timestamp)
```

---

## Google Maps API Key (Required)

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Enable the **Maps SDK for Android**
3. Create an API key and restrict it to your app's SHA-1 certificate
4. Add to `local.properties` in the root project folder:

```
MAPS_API_KEY=YOUR_API_KEY_HERE
```

> Note: `local.properties` is not committed to git. Each developer must add their own key.

---

## How to Build and Run

### Option 1 — Android Studio (Recommended)

1. Open Android Studio
2. Select **File → Open** and navigate to the `code/` folder
3. Wait for Gradle sync to complete
4. Place `google-services.json` in the `app/` folder
5. Add `MAPS_API_KEY` to `local.properties`
6. Connect your Android device via USB
7. Enable **USB Debugging** on your device:
   - Settings → About Phone → tap Build Number 7 times
   - Settings → Developer Options → Enable USB Debugging
8. Press the **Run ▶** button in Android Studio
9. Select your connected device and click OK

### Option 2 — Command Line

```bash
# Navigate to project root
cd code/

# Clean build cache (if needed)
./gradlew clean

# Build debug APK
./gradlew :app:assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Windows:**
```powershell
# Set JAVA_HOME if needed
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

.\gradlew.bat clean
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## Required Device Permissions

After installing, grant the following permissions for full functionality:

| Permission | Why Needed |
|-----------|-----------|
| Location — Allow all the time | Required for background GeoFencing |
| Camera | Required for WPI ID card scanning |

To set location to "Allow all the time":
Settings → Apps → OpenSpot → Permissions → Location → Allow all the time

> Without "Allow all the time" location, GeoFencing will not fire when the app is backgrounded.

Also disable battery optimization for reliable background operation:
Settings → Battery → App battery usage → OpenSpot → Unrestricted

---

## App Flow

```
First launch:
  Splash → Sign In → Register → Scan WPI ID → Confirm Details → Map

Returning user:
  Splash (auth check) → Map directly
```

**Sign up note:** Only @wpi.edu email addresses are accepted during registration.

---

## Key Features

- **Real-time parking map** — Google Maps with color-coded markers (green/yellow/red)
- **Passive GeoFencing** — occupancy auto-updates when users enter/exit lot boundaries
- **Live Firestore sync** — map updates within 2 seconds of any occupancy change
- **MLKit ID scan** — scans WPI ID card during onboarding, extracts name and 9-digit ID
- **Edit profile** — users can update their name, WPI ID, and permit type at any time
- **Lot detail bottom sheet** — tap any marker for occupancy bar, permit types, directions

---

## Architecture

The app follows MVVM (Model-View-ViewModel) architecture:

```
UI Layer (Fragments)
    ↓ observes StateFlow
ViewModel Layer (HomeViewModel, LotsViewModel, etc.)
    ↓ reads/writes
Data Layer (FirestoreDataSource, Room DB, Repository)
    ↓ triggers
Service Layer (LocationService, GeofenceManager, GeofenceBroadcastReceiver)
    ↓ writes atomically
Firebase Backend (Firestore + Firebase Auth)
```

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Google Maps SDK | via BOM | Campus map display |
| Firebase BOM | 32.7.4 | Auth + Firestore |
| FusedLocation | via BOM | GPS + GeoFencing |
| MLKit Text Recognition | 16.0.0 | WPI ID card scan |
| CameraX | 1.3.2 | Camera pipeline |
| Room DB | via KSP | Local caching |
| Kotlin Coroutines | via BOM | Async/StateFlow |
| Navigation Component | via BOM | Fragment navigation |

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Maps not showing | Check `MAPS_API_KEY` in `local.properties` and rebuild |
| `google-services.json` missing | Download from Firebase console and place in `app/` folder |
| GeoFence not triggering | Grant "Allow all the time" location and disable battery optimization |
| Build fails with KSP error | Run `./gradlew clean` then build again |
| INSTALL_FAILED_UPDATE_INCOMPATIBLE | Run `adb uninstall com.wpi.openspot` then reinstall |

---

## Team Contributions

- **Glenn D'souza** — Architecture, Firebase backend, GeoFencing pipeline
- **Ishaan Parekh** — UI screens, Maps integration, MLKit ID scan, app polish
- **Shreelaxmi Malawade** — Testing, Firebase rules, Lots list, Profile screens

---

*CS 528 Mobile & Ubiquitous Computing - Worcester Polytechnic Institute - Spring 2026*
