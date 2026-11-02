# Comprehensive Admin Panel & App Management System

A production-grade Admin Console and real-time App Management System (Maintenance Mode & Force Update) connected directly to Cloud Firestore at `/app_config/global`, featuring responsive sidebar navigation, user portfolio views with photo moderation, instant session revocation for suspended accounts, and live Firebase quota monitoring.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during the interactive interview and govern this implementation:

- **Confirmed Decision 1 (Admin Panel Navigation Layout)**: Responsive collapsible sidebar navigation that smoothly adapts across screen sizes, functioning as a persistent multi-pane sidebar on tablets and wide screens, and a sleek frosted-glass drawer on handheld devices.
- **Confirmed Decision 2 (Maintenance Mode Admin Bypass)**: Admins (`role == "ADMIN"` and `mdmaheraz65@gmail.com`) automatically bypass the Maintenance Mode blocker with a visible admin badge, ensuring developer access while public students are safely gated.
- **Confirmed Decision 3 (App Controls Firestore Node)**: The App Management System will synchronize in real-time via a dedicated Firestore document at `/app_config/global`, propagating toggles and messages instantly to all connected user apps.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Comprehensive Admin Panel**: A central command console featuring 9 core sections: Dashboard, Users, Books, Reports, Colleges, App Controls, Firebase Usage, Settings, and Logout.
  2. **User Portfolio View**: Deep-dive user profile inspector displaying student credentials, published listings, contact details, and a shared photo gallery with admin preview, download, and delete actions.
  3. **Instant Account Revocation**: Real-time Firestore account listener that immediately kicks out any active user whose account status changes to `SUSPENDED`, `BLOCKED`, or `BANNED`, displaying an explanatory modal and preventing subsequent login.
  4. **App Management System (Maintenance Mode & Force Update)**:
     - **Maintenance Mode**: Remote toggle and custom broadcast message that immediately displays a non-dismissible blocking screen for regular students while allowing authorized admins to bypass.
     - **Force Update**: Remote toggle, target version string, APK download link, and update release notes that blocks outdated clients and directs users to download the latest APK.
- **Target Audience**: Campus administrators, platform moderators, and developers managing the BookSwap student marketplace.
- **Key Value**: Complete operational control over app availability, client versioning, user safety, and database resources without requiring new APK deployments.

---

## 2. User Experience & Visual Design

### Key User Flows

#### Flow 1: Admin Panel Navigation & Responsive Sidebar
- **Wide Screens (Tablets / Foldables / Desktops)**: A left-anchored frosted-glass sidebar (width 260dp) displays navigation icons with labels, active pill indicators, and live badge counters (e.g. pending reports).
- **Compact Screens (Handheld Devices)**: A top action bar with a hamburger menu opens a modal navigation drawer with identical tabs and quick transitions.
- **Navigation Tabs**:
  1. **Dashboard**: Metrics grid (total users, active book listings, pending reports, app status) + daily visitor analytics.
  2. **Users**: Searchable and filterable student directory; tapping a student card opens the User Portfolio View.
  3. **Books**: Marketplace listings moderation, search, delete, and restore actions.
  4. **Reports**: Listing dispute and scam report triaging with resolve/dismiss actions.
  5. **Colleges**: Campus traffic share and listing breakdown (FPI vs FCI).
  6. **App Controls**: Maintenance mode switch, message editor, force update toggle, target version, and APK link.
  7. **Firebase Usage**: Real-time quota and live usage meters for Firestore document reads/writes, Storage bandwidth, and active chats.
  8. **Settings**: Admin profile, database backup triggers, and telemetry toggles.
  9. **Logout**: Secure session termination returning to authentication screen.

#### Flow 2: User Portfolio & Photo Moderation
- Tapping any user in the Users tab displays their detailed User Portfolio Page:
  - Header with profile avatar, verified student badge, contact buttons (phone/email), and account status badge.
  - Moderation action row: **Activate**, **Suspend**, **Block**, and **Delete User**.
  - **Published Book Sets**: Cards showing each book set authored by this student.
  - **Shared Portfolio Photos**: Grid of all photos uploaded by this student across listings and avatar. Admin can tap any photo to view full-resolution, download to local storage, or permanently delete inappropriate photos.

#### Flow 3: Maintenance Mode Wall & Admin Bypass
- When `is_maintenance_mode` is set to `true`:
  - Regular students immediately see a full-screen, non-dismissible frosted maintenance wall with animated gear iconography, the custom maintenance message, and a "Retry Connection" button.
  - Logged-in Admins bypass the wall automatically, displaying a floating pill: "⚡ Maintenance Mode Active (Admin Bypass)".

#### Flow 4: Force Update Wall
- When `is_force_update` is set to `true` and the installed version (`1.0`) is lower than `latest_app_version`:
  - An un-dismissible update wall informs the student that an update is required to continue.
  - Displays release notes and an "Update Now" action button that opens the direct APK download URL via the system browser.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Real-Time Firestore Synchronization for `/app_config/global`**
  - *Chosen Approach*: Stream updates via `addSnapshotListener` on `/app_config/global` within `MarketplaceRepository`.
  - *Why*: Instantaneous propagation across all connected devices within sub-second latency, avoiding polling battery drain.
  - *Fallback*: Default offline configuration is bundled in-app if network is unavailable.

- **Decision 2: Global Root Composition for Maintenance & Update Gating**
  - *Chosen Approach*: Gate screens at the root of `MainActivity.kt` (`MainApp` level) so that no background tabs, chat listeners, or marketplace views can be accessed while maintenance or force update is active.
  - *Why*: Complete security isolation; unauthorized users cannot interact with or leak campus listing data during critical migrations.

- **Decision 3: Integrated Dual Navigation (Sidebar + Drawer)**
  - *Chosen Approach*: Use `BoxWithConstraints` to automatically switch between a persistent side navigation rail/bar (width >= 600dp) and a modal navigation drawer (compact handheld).
  - *Why*: Follows Material Design 3 adaptive guidelines for foldables, tablets, and mobile devices without code duplication.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                        Cloud Firestore                         │
│  • /app_config/global (isMaintenanceMode, forceUpdate, etc.)   │
│  • /users/{userId} (accountStatus: ACTIVE/SUSPENDED/BLOCKED)   │
│  • /bookSets/{setId} (active listings & photos)                │
└───────────────────────────────┬────────────────────────────────┘
                                │
               Real-Time Snapshot Listeners (StateFlow)
                                │
                                ▼
┌────────────────────────────────────────────────────────────────┐
│                     MarketplaceRepository                      │
│  • appConfig: StateFlow<AppConfig>                             │
│  • currentUser: StateFlow<UserProfile>                         │
│  • allUsers: StateFlow<List<UserProfile>>                      │
│  • bookSets: StateFlow<List<BookSet>>                          │
│  • updateAppConfig(config: AppConfig)                          │
│  • updateUserAccountStatus(userId, status)                     │
└───────────────────────────────┬────────────────────────────────┘
                                │
        ┌───────────────────────┴───────────────────────┐
        ▼                                               ▼
┌──────────────────────────────┐        ┌──────────────────────────────┐
│         MainActivity         │        │      AdminPanelScreen        │
│  (Root App Gating)           │        │  (Responsive Sidebar & Rail) │
│                              │        │                              │
│  • Maintenance Mode Wall     │        │  1. Dashboard Overview       │
│    (Admin auto-bypasses)     │        │  2. Users & Portfolio Modal  │
│  • Force Update Wall         │        │  3. Books Moderation         │
│    (Version comparison check)│        │  4. Reports & Dispute Desk   │
│  • Live Account Status Gate  │        │  5. Colleges Breakdown       │
│    (Revocation on suspend)   │        │  6. App Controls (Toggles)   │
│  • Main Marketplace Content  │        │  7. Firebase Live Telemetry  │
│                              │        │  8. Settings & 9. Logout     │
└──────────────────────────────┘        └──────────────────────────────┘
```

### Data Models & Schemas

```kotlin
data class AppConfig(
    val isMaintenanceMode: Boolean = false,
    val maintenanceMessage: String = "The app is currently undergoing scheduled campus maintenance. Please check back shortly.",
    val isForceUpdate: Boolean = false,
    val latestAppVersion: String = "1.0",
    val apkDownloadUrl: String = "https://github.com/mdmaheraz65/BookSwap/releases",
    val updateNotes: String = "Bug fixes, performance enhancements, and new campus marketplace features."
)
```

---

## 5. Verification & Testing Plan

1. **Compilation Check**: Run `compile_applet` to ensure zero compilation or syntax errors across new components and state flows.
2. **Unit & Robolectric Tests**: Run `gradle :app:testDebugUnitTest` to verify repository updates, account status changes, and configuration mutations.
3. **Interactive CUJ Verification**:
   - *App Controls Toggle*: Toggle Maintenance Mode in Admin Panel -> verify Firestore document update -> verify regular user app immediately displays maintenance screen while Admin user retains access.
   - *Force Update Trigger*: Set `latest_app_version = "2.0.0"` with `is_force_update = true` -> verify user app displays non-dismissible update modal with "Update Now" browser link.
   - *User Moderation & Revocation*: Suspend a user in Users tab -> verify user's session is instantly terminated with an explanatory dialogue.
   - *User Portfolio*: View student profile -> inspect published listings -> preview and delete portfolio photos.
