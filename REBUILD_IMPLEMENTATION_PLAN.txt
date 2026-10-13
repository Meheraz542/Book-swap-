# REBUILD IMPLEMENTATION PLAN
## Application: Polytechnic Book Exchange (`BookSet`)
### Step-by-Step Blueprint for 100% Faithful Reconstruction

---

## 1. Project Initialization & Toolchain Setup
- **Step 1.1:** Initialize an Android project targeting SDK 36, minSdk 24 with Kotlin and Gradle Kotlin DSL (`build.gradle.kts`).
- **Step 1.2:** Configure `settings.gradle.kts` with `rootProject.name = "BookSet"`.
- **Step 1.3:** Setup Gradle Wrapper with Gradle 9.3.1 (`gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`).
- **Step 1.4:** Add dependencies in `gradle/libs.versions.toml`:
  - AndroidX Compose BOM (Material 3, Foundation, UI, Animation, Icons Extended)
  - AndroidX Lifecycle (ViewModel, Runtime Compose)
  - Room Database (Runtime, KTX, KSP compiler)
  - AndroidX Credentials & Google Identity (`credentials`, `googleid`)
  - Firebase Auth & Google Services Plugin

---

## 2. Data Layer & Room Architecture
- **Step 2.1:** Implement domain models in `com.example.model.Models.kt` (Enums: `Technology`, `Semester`, `SetCondition`, `SetStatus`, `MegaDealStatus`).
- **Step 2.2:** Define 7 Room entities in `com.example.data.local.entities.Entities.kt`:
  - `UserEntity`, `BookSetEntity`, `IncludedBookEntity`, `MegaDealEntity`, `ConversationEntity`, `ChatMessageEntity`, `NotificationEntity`.
- **Step 2.3:** Implement Room DAOs in `com.example.data.local.dao.Daos.kt` with reactive `Flow` query observers.
- **Step 2.4:** Build `AppDatabase` singleton with `fallbackToDestructiveMigration()`.

---

## 3. Business Logic, Authentication & Services
- **Step 3.1:** Implement `AuthManager` with password hashing, session switcher, and user seeding.
- **Step 3.2:** Build `EmailOtpManager` with 6-digit random code generation, expiration timestamps, and system notification dispatch.
- **Step 3.3:** Implement `MarketplaceRepository` singleton coordinating all reactive flows (`currentUser`, `isLoggedIn`, `bookSets`, `conversations`, `megaDeals`, `notifications`, `trafficStats`).
- **Step 3.4:** Build `AuthViewModel` with Jetpack Credential Manager Google Sign-In and emulator fallback.

---

## 4. UI System & Design Tokens
- **Step 4.1:** Setup `Color.kt` and `Theme.kt` with M3 color schemes and custom palette (`PrimaryBlue`, `PrimaryPurple`, `BgCanvas`).
- **Step 4.2:** Build `GlassComponents.kt` with the `Modifier.liquidGlass` modifier and frosted button composables.
- **Step 4.3:** Implement `FloatingBottomNavBar` with iOS-grade spring animation.

---

## 5. Screen Development Sequence
- **Step 5.1:** `AuthScreen` with Tab Switcher, Clean Sign In, and the **Gmail OTP Verification Card** (PIN boxes, auto-fill, countdown, and Bengali messages).
- **Step 5.2:** `HomeScreen` with Campus hero card, department pills, semester selector, and book sets grid.
- **Step 5.3:** `SetDetailsScreen` with included book itemization and deal/chat actions.
- **Step 5.4:** `SearchScreen` with multi-filter chips and price ranges.
- **Step 5.5:** `SellScreen` with dynamic multi-book set publisher.
- **Step 5.6:** `InboxScreen` and `ChatScreen` with in-chat Mega Deal proposition and negotiation banners.
- **Step 5.7:** `ProfileScreen` with student card, listing tabs, Super Admin trigger, Logout confirmation, and **`develop by Mh Nirob`** credit text.
- **Step 5.8:** `AdminPanelScreen` with telemetry metrics, student moderation, and listing moderation.
- **Step 5.9:** `NotificationScreen` for real-time security OTP and deal alert logs.

---

## 6. Build & CI/CD Pipeline
- **Step 6.1:** Add `.github/workflows/android-apk.yml` with automated debug keystore generation and `gradle assembleDebug` APK compilation.
- **Step 6.2:** Verify compilation using `gradle assembleDebug` or `compile_applet`.
