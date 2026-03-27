# 🗺️ Smart Travel App

A production-ready Android travel companion app built with **Kotlin**, **MVVM**, and **100% free APIs**.

![Android](https://img.shields.io/badge/Android-API%2024%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.10-purple)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 📱 Features

| Feature | API Used | Status |
|---|---|---|
| 📍 Live GPS Tracking | FusedLocationProvider | ✅ |
| 🗺️ Interactive Map | MapLibre + OpenStreetMap | ✅ |
| 🛣️ Route Calculation | OSRM (free) | ✅ |
| 🏨 Nearby Places | Overpass API (free) | ✅ |
| 🔍 Location Search | Nominatim (free) | ✅ |
| ⛽ Fuel Cost Calculator | Local calculation | ✅ |
| 📊 Travel Analytics | Room DB + MPAndroidChart | ✅ |
| 📌 Location Tagging | Room DB + Geofencing | ✅ |
| 🔔 Smart Alerts | NotificationManager | ✅ |
| 📤 CSV Export | FileProvider | ✅ |

---

## 🏗️ Architecture

```
app/
├── data/
│   ├── local/          # Room DB (entities, DAOs, database)
│   └── remote/         # Retrofit APIs + response models
├── di/                 # Hilt dependency injection
├── repository/         # Single source of truth for data
├── ui/                 # Fragments + Activity (View layer)
├── utils/              # Helpers: polyline, CSV, fuel calc
└── viewmodel/          # MVVM ViewModels (state management)
```

**Stack:** Kotlin • MVVM • Hilt DI • Room • Retrofit • Coroutines • MapLibre • LiveData • Navigation Component

---

## 🚀 Quick Start

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK API 33

### 1. Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/SmartTravelApp.git
cd SmartTravelApp
```

### 2. Open in Android Studio
```
File → Open → Select the SmartTravelApp folder
Wait for Gradle sync to complete
```

### 3. Run the app
```
Select a device (emulator or physical)
Click ▶ Run (or Shift+F10)
```

> ⚠️ **GPS Note:** Use a physical device or configure mock location on emulator for GPS features.

---

## 🔄 GitHub Actions CI/CD

The pipeline runs automatically on every push:

```
Push to any branch
    │
    ├─ ✅ Unit Tests (JUnit + Mockito)
    ├─ ✅ Lint Check
    │
    └─ Build
        ├─ Debug APK  → artifact on all branches
        └─ Release APK → artifact on main branch only
                └─ GitHub Release → on version tags (v1.0.0)
```

### Setup GitHub Secrets for signed release APK

Go to your repo → **Settings → Secrets → Actions** and add:

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | `base64 -i your-keystore.jks` |
| `KEY_ALIAS` | Your key alias |
| `KEY_PASSWORD` | Your key password |
| `STORE_PASSWORD` | Your keystore password |

### Create a release
```bash
git tag v1.0.0
git push origin v1.0.0
# → Triggers Release APK build + GitHub Release creation
```

---

## 🆓 Free APIs Used

| API | Purpose | Rate Limit | Docs |
|---|---|---|---|
| [Nominatim](https://nominatim.openstreetmap.org) | Geocoding & search | 1 req/sec | [docs](https://nominatim.org/release-docs/develop/api/Overview/) |
| [OSRM](https://router.project-osrm.org) | Route calculation | Unlimited* | [docs](http://project-osrm.org/docs/v5.24.0/api/) |
| [Overpass API](https://overpass-api.de) | Nearby places (OSM data) | Fair use | [docs](https://wiki.openstreetmap.org/wiki/Overpass_API) |
| [MapLibre](https://maplibre.org) | Map rendering | Unlimited | [docs](https://maplibre.org/maplibre-gl-js/docs/) |

> *OSRM demo server is for development only. For production, self-host OSRM or use a hosted provider.

---

## 📂 Project Structure

```
SmartTravelApp/
├── .github/
│   └── workflows/
│       └── android.yml          # CI/CD pipeline
├── app/
│   ├── build.gradle             # Dependencies
│   ├── proguard-rules.pro       # Release optimisation
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/smarttravel/app/
│       │   │   ├── SmartTravelApplication.kt
│       │   │   ├── data/local/  # Room DB
│       │   │   ├── data/remote/ # Retrofit APIs
│       │   │   ├── di/          # Hilt modules
│       │   │   ├── repository/  # Data repositories
│       │   │   ├── ui/          # Fragments & Activity
│       │   │   ├── utils/       # Helpers
│       │   │   └── viewmodel/   # ViewModels
│       │   └── res/
│       │       ├── drawable/    # Vector icons
│       │       ├── layout/      # XML layouts
│       │       ├── menu/        # Bottom nav menu
│       │       ├── navigation/  # Nav graph
│       │       ├── values/      # Strings, colors, themes
│       │       └── xml/         # FileProvider paths
│       ├── test/                # Unit tests
│       └── androidTest/         # Instrumentation tests
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradle.properties
└── gradlew
```

---

## 🔧 Configuration

### Change map tile style
In `MapFragment.kt`, replace the style URL:
```kotlin
// Default (demo tiles)
map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json"))

// OpenFreeMap (recommended for production)
map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty"))

// Maptiler (free tier available)
map.setStyle(Style.Builder().fromUri("https://api.maptiler.com/maps/streets/style.json?key=YOUR_KEY"))
```

### Adjust location update frequency
In `LocationTrackingService.kt`:
```kotlin
val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L) // interval ms
    .setMinUpdateIntervalMillis(2000L)  // fastest update
    .setMinUpdateDistanceMeters(10f)    // min movement to trigger update
    .build()
```

### Adjust nearby search radius
In `NearbyRepository.kt`:
```kotlin
viewModel.loadNearby(lat, lon, category, radiusMeters = 5000) // 5km radius
```

---

## 🧪 Running Tests

```bash
# Unit tests
./gradlew test

# Instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# All checks
./gradlew check
```

---

## 🛣️ Roadmap

- [ ] Offline map tiles caching
- [ ] Turn-by-turn navigation voice guidance
- [ ] Trip sharing via deep links
- [ ] Widget for quick location tagging
- [ ] Dark mode support
- [ ] Google Play Store release

---

## 📄 License

```
MIT License — free to use, modify, and distribute.
```

---

## 🙏 Credits

- [OpenStreetMap](https://www.openstreetmap.org) contributors
- [MapLibre](https://maplibre.org) — open-source map SDK
- [OSRM](http://project-osrm.org) — routing engine
- [Nominatim](https://nominatim.org) — geocoding
- [Overpass API](https://overpass-api.de) — OSM data queries
