# LessOfIt

LessOfIt is an Android app for reducing household food waste. It tracks the
ingredients you own, along with how much of each and when it expires, then
recommends recipes that make best use of what is already in your kitchen —
prioritising items closest to expiry so they get cooked instead of thrown
away.

The project began as a university final-year project (2022) and has since
been recovered and repaired: the build toolchain restored, core data
integrity and user flows fixed, and regression coverage added.

## How it works

The app has three tabs — **Recipes**, **Inventory**, and **Shopping List** —
backed by a local [Room](https://developer.android.com/training/data-storage/room)
database and a live integration with
[TheMealDB](https://www.themealdb.com/api.php) recipe API:

1. You add ingredients to your **inventory** with a quantity (whole units)
   and one or more expiry dates.
2. You search recipes **by ingredient** (TheMealDB `filter.php` /
   `lookup.php`). Matching recipes are imported into the local database with
   their ingredients and instructions.
3. A deterministic ranking algorithm scores every recipe against your
   inventory: a recipe is *makeable* only when all required ingredients are
   owned in sufficient quantity; makeable recipes are ranked so that ones
   using soon-expiring stock come first. Recipes that require missing
   ingredients are still listed, behind the makeable ones.
4. When you cook, **Finished cooking** deducts the consumed units and removes
   the matching expiry records. When something is missing, **Add missing
   ingredients** puts exactly the shortfall on the shopping list; marking a
   shopping-list item as purchased moves it into the inventory with a new
   expiry date.

Ingredient names are normalised (trimmed, locale-independent upper case)
consistently on storage and matching, so imported names like "Tomato" match
inventory entries like "TOMATO".

## Features

- Inventory management with per-unit expiry-date tracking.
- Recipe import from TheMealDB by ingredient search, with duplicate-import
  protection enforced at the database level: external meal IDs are persisted
  under a unique index and every import commits in one transaction, so even
  overlapping searches cannot import the same recipe twice.
- Expiry-aware, inventory-aware deterministic recipe ranking
  ("By Recommended" and "By Name" sort options).
- Cooking completion updates inventory quantities and expiry records
  consistently (soonest-expiring units consumed first).
- Custom recipe management (create, edit, delete; optional image stored in
  app-private storage).
- Shopping list with autocomplete, quantity handling, and a purchased →
  inventory transfer flow.
- Local persistence via Room, including an explicit non-destructive
  database migration from the legacy schema.

Known feature limitations are listed under [Current
limitations](#current-limitations).

## Technology stack

- **Language:** Java (source/target compatibility 1.8)
- **UI:** Android Views (XML layouts, fragments, RecyclerView), AppCompat
  1.4.1, Material Components 1.5.0, ConstraintLayout 2.1.3
- **Build:** Gradle wrapper 9.0.0, Android Gradle Plugin 8.13.0
- **SDK:** `compileSdk` 36 (revision 36.1), `minSdk` 26, `targetSdk` 34,
  Build Tools 36.0.0
- **Persistence:** Room 2.4.0 (schema version 7)
- **Networking/API:** TheMealDB v1 over `HttpURLConnection`, JSON parsed
  with Gson 2.9.0
- **Testing:** JUnit 4.13.2 (unit), AndroidX Test `ext:junit` 1.2.1,
  Espresso 3.7.0 (instrumented), Room/SQLite framework on the test
  classpath for migration tests (Espresso 3.7.0+ is required to inject
  input on API 36 devices)

## Prerequisites

- **JDK 17 or newer.** All verification runs used OpenJDK 21.
- **Android SDK** with:
  - Android SDK Platform 36 (revision **36.1** — the build targets the
    36.1 minor platform),
  - Android SDK Build-Tools **36.0.0**.

  Android Studio will prompt to install missing components once its SDK
  licenses are accepted; alternatively install them from the SDK Manager.
- **Android Studio:** a current stable release is recommended. The project
  uses AGP 8.13.0; older Android Studio versions may fail to sync it. The
  command-line workflow below does not depend on any specific IDE — the
  repository's Gradle wrapper downloads Gradle 9.0.0 itself, so a separate
  Gradle installation is not needed.
- **A device or emulator running API 26 (Android 8.0) or newer** for
  runtime verification. The test suite was verified on an API 36 emulator.

## Setup

1. Clone the repository:

   ```bash
   git clone git@github.com:Xodez/FYP.git
   cd FYP
   ```

2. Open the project folder in Android Studio and let it sync (it will use
   the bundled JDK and the Gradle wrapper). For command-line-only use,
   make sure `JAVA_HOME` points at a JDK 17+.

3. Point Gradle at your Android SDK. In Android Studio this is handled
   automatically by writing `local.properties` (already gitignored). For
   command-line builds without an IDE, create `local.properties` in the
   project root:

   ```properties
   sdk.dir=/path/to/your/Android/sdk
   ```

4. Install the SDK components listed in Prerequisites if prompted.

5. Build and run the `app` configuration on an emulator or device, or use
   the command-line checks below.

## Build and test commands

All commands run from the repository root.

Build the debug APK:

```bash
./gradlew --no-daemon assembleDebug
```

Run the unit tests (JVM only — no device needed):

```bash
./gradlew --no-daemon test
```

Run static analysis:

```bash
./gradlew --no-daemon lint
```

Run the instrumented tests (requires a connected emulator or device running
API 26+; Espresso 3.7.0 is required for API 36 devices):

```bash
./gradlew --no-daemon connectedDebugAndroidTest
```

Verified results at the current configuration: `assembleDebug` succeeds;
43/43 unit tests pass; 26/26 instrumented tests pass on an API 36
emulator; `lint` reports 0 errors and 97 warnings (all pre-existing
style/hard-coding suggestions, listed under Limitations).

## API and configuration notes

- The app calls TheMealDB's public v1 API using the documented
  **developer test key `1`**, embedded in the base URL in
  `app/src/main/java/com/example/fyp/MealApi.java`. No secrets are stored
  in the repository and no build-time configuration is required to run
  recipe searches.
- Per TheMealDB's terms of use, the test key is intended for development
  and educational use; publishing the app to an app store requires a paid
  supporter (production) key. If you plan store distribution, obtain a key
  from TheMealDB and replace the `1` path segment in `MealApi` accordingly.
- Recipe API requests and remote image loads all run on bounded
  background pools with explicit 5-second connect and read timeouts.
  Connection failures, non-2xx responses, empty bodies, and malformed
  JSON are handled with user-facing messages instead of crashes, and a
  recipe with an unreachable image still opens (a placeholder is shown).
  Beyond importing new recipes, the only other network dependency is
  fetching imported recipes' thumbnails from the MealDB image host when
  they are displayed; everything else works offline.

## Data storage and privacy

- All user data — inventory items, quantities, expiry dates, custom and
  imported recipes, shopping-list entries — is stored in a local Room
  database (`RecipeApp`, schema version 7) in the app's private internal
  storage on the device. The data is not encrypted at rest and is not
  synced to any account or server.
- Recipe images chosen for custom recipes are copied into the app's
  external files directory (`Android/data/<package>/files/LessOfIt/Images`),
  which is removed on uninstall. Images imported from TheMealDB stay remote
  URLs; the app downloads them for display when the recipe is viewed.
- Clearing the app's data or uninstalling removes all stored data. Because
  `android:allowBackup` is enabled, the platform may include app data in
  the user's normal device-backup behaviour; no custom backup logic is
  implemented.
- Browsing imported recipes, the inventory, and the shopping list works
  offline; only importing new recipes from TheMealDB requires a network
  connection.

## Testing

- **Unit tests** (`app/src/test/java/com/example/fyp/`): the recommendation
  ranking algorithm (`RecipeAlgorithmTest`, 20 tests), MealDB response
  parsing with offline fixtures (`MealApiTest`, 10 tests), the remote-image
  fetch boundary (`RemoteImageLoaderTest`, 5 tests), and the Room
  ingredient-identity converters (`ConvertersIngredientIdentityTest`,
  7 tests). Run with `./gradlew --no-daemon test`.
- **Instrumented tests** (`app/src/androidTest/java/com/example/fyp/`):
  Room persistence, expiry-consistency, and concurrent-import deduplication
  behaviour; the schema 5→6 and 6→7 migrations against real legacy
  databases (data preservation, duplicate reconciliation, and a full
  chained 5→7 Room open); and Espresso end-to-end tests of the core flows
  (cook-and-decrement, unit removal with shared expiry dates,
  shopping-list purchase round trip, editing a recipe with a remote image,
  invalid and cancelled edits preserving the original, unreachable-image
  and mid-request-teardown lifecycle cases). Run with
  `./gradlew --no-daemon connectedDebugAndroidTest` on a connected
  emulator or device.

## Current limitations

- **Ingredient quantities are whole units.** TheMealDB's measure strings
  ("200g", "to taste") do not map onto the app's discrete unit model, so
  imported recipes require exactly **one unit per ingredient**. There is no
  measurement conversion.
- **`targetSdk` is 34.** Android 15 (API 35) enforces edge-to-edge display,
  which would require inset handling across the legacy XML screens; that
  work is intentionally deferred. Android 13/14 behaviour changes do not
  affect this app (no notifications, services, receivers, or PendingIntents).
- **Recipe thumbnails are not cached:** imported recipes re-fetch their image
  from the MealDB image host each time the list renders, bounded to two
  background loader threads with 5-second timeouts.
- **Recommendations are deterministic**, not machine-learned: score-based
  ranking over owned quantities and expiry urgency only. Recipes without
  ingredient requirements are excluded from ranked results; ingredients with
  missing or malformed expiry data rank behind comparable valid data; expired
  items get maximum urgency (waste-reduction heuristic, not a food-safety
  judgement).
- **Architecture is dated:** Room is used with main-thread queries from
  fragments; there is no ViewModel/LiveData layer. Background work for
  imports runs on a plain thread with UI updates posted back.
- **Manual-verification gaps:** gallery-based image selection and the
  splash-screen entry flow are not covered by automated tests.
- **Lint:** 97 pre-existing warnings remain (hard-coded strings, layout
  suggestions, unused resources); no lint errors.
- No licence file is included; add one before publishing if you want
  reuse terms stated.
