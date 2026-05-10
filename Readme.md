# RideSync

RideSync is an Android app for real-time group trip coordination. Users can sign in with Google, create or join a travel team, share live locations, let the team leader set a shared destination, and view a Mapbox route with ETA and distance.

The repository is named `TravelTrip`, while the Android app namespace and root Gradle project are `RideSync`.

## Current Status

Project status: In progress.

Completed phases:

| Phase | Feature Area | Status |
| --- | --- | --- |
| 1 | Project setup and architecture foundation | Complete |
| 2 | Firebase setup and configuration | Complete |
| 3 | Google authentication | Complete |
| 4 | Firestore user data management | Complete |
| 5 | Mapbox map integration | Complete |
| 6 | Real-time location tracking | Complete |
| 7 | Team creation and joining | Complete |
| 8 | Real-time team markers on map | Complete |
| 9 | Leader destination selection | Complete |
| 10 | Route drawing and ETA | Complete |
| 11 | Background location service | Not started |
| 12 | Push notifications | Not started |
| 13 | Error handling and offline support | Not started |
| 14 | UI polish, tests, and deployment | Not started |

## Features

- Google Sign-In through Firebase Authentication and Android Credential Manager.
- Firestore-backed user profiles with live profile observation.
- Team creation with a generated 6-character join code.
- Team joining, leaving, and leader disband flow.
- Real-time member list and team member location sync.
- Mapbox map screen with user location puck and live latitude/longitude chip.
- Team member map markers with member names.
- Leader-only long-press destination selection on the map.
- Reverse geocoded destination address stored in Firestore.
- Shared destination marker visible to all team members.
- Mapbox Directions API route from current user location to destination.
- Route polyline overlay, ETA, and distance summary.
- Rerouting when the user moves significantly.

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, repository interfaces, domain/data separation |
| Dependency Injection | Hilt |
| Async | Kotlin Coroutines and Flow |
| Auth | Firebase Authentication, Google ID |
| Database | Firebase Firestore |
| Messaging dependency | Firebase Cloud Messaging dependency is included for later phases |
| Maps | Mapbox Maps SDK v11 with Compose extension |
| Routes | Mapbox Directions API |
| Device Location | FusedLocationProviderClient |
| Images | Coil |
| Build | Gradle Kotlin DSL, version catalog |

## Architecture

The app follows a lightweight clean architecture layout:

```text
app/src/main/java/com/ragnar/RideSync/
  data/
    model/          Firestore DTOs and mappers
    repository/     Firebase, location, and Mapbox implementations
  domain/
    model/          App domain models
    repository/     Repository contracts
  di/               Hilt modules
  ui/
    navigation/     Compose navigation graph and routes
    screens/        Auth, home, profile, team, and map screens
    theme/          Compose theme and color definitions
  utils/            Constants and debug logging helpers
```

Important runtime flows:

- Auth flow: `LoginScreen` -> `AuthViewModel` -> `AuthRepository` -> Firebase Auth.
- Profile flow: `UserViewModel` observes `UserRepository.observeUser`.
- Team flow: `TeamViewModel` creates, joins, leaves, disbands, and observes team state.
- Location flow: `LocationViewModel` observes fused location updates and writes latest user location to Firestore.
- Map team flow: `TeamMapViewModel` resolves the active team, observes members and team destination, and writes leader-selected destinations.
- Navigation route flow: `NavigationViewModel` calls `DirectionsRepository`, which is implemented by `MapboxDirectionsRepository`.

## Firestore Data Model

The app currently uses these collections:

```text
users/{userId}
  displayName: String?
  email: String?
  photoUrl: String?
  currentTeamId: String?
  fcmToken: String?
  createdAt: Long?
  updatedAt: Long?
  lastLocation:
    lat: Double?
    lng: Double?
    updatedAt: Long?

teams/{teamId}
  name: String
  code: String
  leaderId: String
  createdAt: Long
  destinationLat: Double?
  destinationLng: Double?
  destinationAddress: String?

teams/{teamId}/members/{userId}
  displayName: String?
  photoUrl: String?
  joinedAt: Long?
  lastLocation:
    lat: Double?
    lng: Double?
    updatedAt: Long?
```

## Requirements

- Android Studio or compatible Gradle/JDK setup.
- JDK 17.
- Android SDK with compile SDK 35.
- Firebase project configured for Android package `com.ragnar.RideSync`.
- Google Sign-In Web Client ID.
- Mapbox public access token.
- Mapbox secret token with `DOWNLOADS:READ` scope for resolving the Mapbox Maven repository.

## Local Setup

1. Clone or open the project.

```bash
cd /home/ragnar/AndroidStudioProjects/TravelTrip
```

2. Add Firebase config.

Place your Firebase Android config at:

```text
app/google-services.json
```

This file is intentionally ignored by git.

3. Add local secrets.

Create or update `local.properties` in the repository root:

```properties
sdk.dir=/path/to/Android/Sdk
GOOGLE_WEB_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
MAPBOX_ACCESS_TOKEN=pk.your-public-mapbox-token
MAPBOX_SECRET_TOKEN=sk.your-mapbox-downloads-read-token
```

`MAPBOX_ACCESS_TOKEN` is injected into `BuildConfig` and used at runtime. `MAPBOX_SECRET_TOKEN` is used only by Gradle to resolve Mapbox SDK artifacts from the Mapbox Maven repository.

4. Sync Gradle.

Open the project in Android Studio and run Gradle sync, or use:

```bash
./gradlew projects
```

## Build And Run

Debug build:

```bash
./gradlew :app:assembleDebug
```

Install on a connected device or emulator:

```bash
./gradlew :app:installDebug
```

Kotlin-only compile check:

```bash
./gradlew :app:compileDebugKotlin
```

If resource processing is blocked by the local environment, a Kotlin-only check can be run by excluding Android resource tasks:

```bash
./gradlew :app:compileDebugKotlin -x :app:processDebugResources -x :app:mergeDebugResources -x :app:packageDebugResources
```

## NixOS Build Note

On this machine, full Android builds currently fail before Kotlin compilation because Android `aapt2` is a dynamically linked Linux binary that is not patched for NixOS:

```text
NixOS cannot run dynamically linked executables intended for generic linux environments out of the box.
```

This is an environment issue, not a Kotlin source issue. The Kotlin source compile was verified successfully with resource tasks excluded.

## App Usage

1. Launch the app and sign in with Google.
2. Open `My Team`.
3. Create a team or join an existing team using a 6-character code.
4. Share the team code with other users.
5. Open the map.
6. Grant location permission when prompted.
7. Team members appear as map markers once their location is available.
8. If you are the team leader, long-press on the map to set or update the shared destination.
9. All team members see the destination marker.
10. When the current user location and destination are available, the app draws a route and displays ETA and distance.

## Permissions

The app declares:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Background location is not implemented yet. That is planned for Phase 11.

## Key Files

| File | Purpose |
| --- | --- |
| `app/src/main/java/com/ragnar/RideSync/RideSyncApplication.kt` | Hilt application class and Mapbox token initialization |
| `app/src/main/java/com/ragnar/RideSync/ui/MainActivity.kt` | Compose host activity |
| `app/src/main/java/com/ragnar/RideSync/ui/navigation/NavGraph.kt` | App navigation graph |
| `app/src/main/java/com/ragnar/RideSync/ui/screens/auth/AuthViewModel.kt` | Google/Firebase auth state |
| `app/src/main/java/com/ragnar/RideSync/ui/screens/team/TeamViewModel.kt` | Team create/join/leave/disband state |
| `app/src/main/java/com/ragnar/RideSync/ui/screens/map/LocationViewModel.kt` | Live device location tracking |
| `app/src/main/java/com/ragnar/RideSync/ui/screens/map/TeamMapViewModel.kt` | Team context, members, and destination state for map |
| `app/src/main/java/com/ragnar/RideSync/ui/screens/map/NavigationViewModel.kt` | Route fetching and reroute state |
| `app/src/main/java/com/ragnar/RideSync/ui/screens/map/MapScreen.kt` | Mapbox UI, markers, destination, route, and ETA card |
| `app/src/main/java/com/ragnar/RideSync/data/repository/FirestoreTeamRepository.kt` | Firestore team and destination persistence |
| `app/src/main/java/com/ragnar/RideSync/data/repository/MapboxDirectionsRepository.kt` | Mapbox Directions API client |

## Development Documents

Internal planning files are stored in `Documents/`:

- `DevelopmentPlan.md`: phase-by-phase implementation plan.
- `ProjectTracker.md`: phase status and file log.
- `PaidVsFreeAnalysis.md`: service cost analysis.

The `Documents/` folder is ignored by git in this repository.

## Known Limitations

- Background location updates are not implemented yet.
- Push notifications for destination changes are not implemented yet.
- Firestore security rules are not included in this repository.
- Automated unit/UI tests are not yet implemented.
- Offline handling and retry strategy are limited.
- Route requests require a valid Mapbox access token and network connectivity.
- Reverse geocoding currently uses Android `Geocoder`, which may vary by device/service availability.

## Roadmap

Next planned phases:

- Phase 11: Background location service.
- Phase 12: Firebase Cloud Messaging notifications.
- Phase 13: Error handling, edge cases, and offline support.
- Phase 14: UI/UX polish, testing, and deployment preparation.
