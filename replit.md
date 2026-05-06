# EntglDb.Kotlin — Collaborative Shopping List

Kotlin/Android implementation of EntglDb (P2P sync middleware), transformed into a **Collaborative Shopping List** app.

## Project Type

Android multi-module library + sample app (Gradle). No web frontend or backend — deploys as an APK on Android devices.

## Architecture

Gradle multi-module project:

| Module | Description |
|---|---|
| `:core` | Core engine: VectorClock, HLC, Oplog, Conflict Resolution (scaffold) |
| `:persistence-sqlite` | SQLite storage adapter (scaffold) |
| `:network` | P2P networking layer (Ktor, TCP + UDP) — stub `EntglDbService` present |
| `:protocol` | Protocol Buffers v5 (protobuf-kotlin-lite, auto-compiled) |
| `:app` | Shopping List Android app (Jetpack Compose + Material3) |

## App Source Layout

```
app/src/main/kotlin/com/entgldb/app/
├── EntglDbApplication.kt          - Application class
├── MainActivity.kt                - Entry point, edge-to-edge Compose host
├── models/
│   └── ShoppingListItem.kt        - Data model (id, name, quantity, category, checked)
├── db/
│   └── ShoppingRepository.kt      - In-memory repo (replace with real EntglDb API)
└── ui/
    ├── ShoppingListViewModel.kt   - ViewModel using StateFlow
    ├── ShoppingListScreen.kt      - Full Compose UI + Connection Badge + Add dialog
    └── theme/
        ├── Color.kt
        └── Theme.kt               - Material3 light color scheme
```

## Key Features

- **Connection Badge** — animates green (Synced) / amber (Syncing) / red (Offline)
- **Category grouping** — items grouped and sorted by category with headers
- **Add item dialog** — name, quantity, and category dropdown (8 preset categories)
- **Check/uncheck** — strikethrough + animated card elevation
- **Delete** — red trash icon per item
- **Empty state** — friendly prompt when list is empty
- **ShoppingListItem model** — `id`, `name`, `quantity`, `category`, `checked`

## EntglDb Integration Hook

`ShoppingRepository.kt` is the only file to update when `:core`/`:persistence-sqlite`/`:network` are implemented. Replace the in-memory list with real `EntglDb` API calls there.

## Technologies

- Kotlin 2.3.0, AGP 9.0, Gradle 9.1.0
- Jetpack Compose + Material3 + Navigation
- Kotlinx Coroutines + StateFlow
- Protocol Buffers (protobuf-kotlin-lite 4.33.4)
- Ktor 3.4.0 (network layer)
- Android SDK 36, minSdk 24

## Build

```bash
JAVA_HOME=/nix/store/k95pqfzyvrna93hc9a4cg5csl7l4fh0d-openjdk-21.0.7+6 \
PATH=/nix/store/k95pqfzyvrna93hc9a4cg5csl7l4fh0d-openjdk-21.0.7+6/bin:$PATH \
./gradlew build
```

Requires OpenJDK 21 (not GraalVM) due to Android SDK 36 + jlink compatibility. Android SDK is at `/home/runner/android-sdk` (installed via command-line tools).

## Environment Notes

- Android SDK 36 + build-tools 36.0.0 at `/home/runner/android-sdk`
- `local.properties` points to `sdk.dir=/home/runner/android-sdk`
- JDK: OpenJDK 21.0.7 (nixpkgs `jdk21`)
- GraalVM CE 22.3.1 is also present but NOT used (jlink incompatibility with AGP 9)
