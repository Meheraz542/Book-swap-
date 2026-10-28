# Authentication Flow, Blank Auth Inputs & Custom Logout Confirmation

A comprehensive implementation plan to enforce a strict startup authentication gate, remove all placeholder hint texts from authentication forms, and implement a custom animated logout confirmation dialog matching the 'Delete Book Listing' UI with full session purge.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed by the user in Phase 1:
> - **Strict Authentication Wall**: Unauthenticated users are strictly barred from browsing and must sign in or register before accessing the home feed and book listings.
> - **Full Session Purge**: Triggering logout completely clears all local credentials, active session flags, and user states, ensuring subsequent launches route strictly to the authentication screen.

---

## 1. Overview & Core Concept

- **Startup Auth Verification**: On application boot, the app checks for an active authenticated session (via Firebase Authentication and local Room session). Active sessions bypass the auth screen immediately into `MainAppContent`, while unauthenticated users are directed strictly to `AuthScreen`.
- **Pristine Blank Inputs**: All text fields across Sign-In and Sign-Up forms will have empty string values and zero placeholder hints (e.g. removing "e.g. student@fpi.edu.bd", "e.g. Shakil Ahmed", "01712-345678", and "Enter your password"), keeping inputs completely blank until the student types.
- **Delete-Style Logout Dialog**: Replacing the default dialog with a custom `PopAnimatedConfirmationDialog` featuring the bouncy spring pop animation, soft glassmorphism card, rounded red-tinted icon header, a "Cancel" button, and a prominent red "Log Out" button.
- **Session Purge Lifecycle**: On confirmation, `MarketplaceRepository.logout()` signs out from Firebase Auth, purges the active session in Room database, and resets the repository to an unauthenticated state, navigating securely to `AuthScreen`.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Cold Launch (Existing Session)**:
   - App launches with splash screen -> Startup auth check detects authenticated user -> Smooth crossfade straight to `HomeScreen`.
2. **Cold Launch (New / Logged-Out User)**:
   - App launches with splash screen -> Startup auth check detects unauthenticated state -> Directed strictly to `AuthScreen`.
   - Clean, blank form inputs with floating labels, waiting for user input.
   - Successful Sign-In or Sign-Up sets the active session and transitions immediately to `HomeScreen`.
3. **Log Out Flow**:
   - User navigates to `ProfileScreen` and taps "Log Out".
   - A custom `PopAnimatedConfirmationDialog` pops onto the screen with a bouncy spring animation.
   - Includes a circular red logout icon, "Log Out?" title, and descriptive warning text.
   - User taps "Cancel" to dismiss or the red "Log Out" button to proceed.
   - Upon confirmation, local session is purged, and the app instantly navigates to `AuthScreen`.

### Visual Theme & Styling
- **Dialog Appearance**: Rounded 26.dp white card with subtle 1.dp border (`#E2E8F0`), 24.dp elevation shadow, and semi-transparent dim backdrop.
- **Icon Header**: 56.dp circular container with red tint (`#EF4444` at 12% opacity) hosting `Icons.AutoMirrored.Outlined.Logout`.
- **Action Buttons**:
  - Cancel: `FrostedGlassButton` with muted gray text (`#64748B`).
  - Log Out: `GlassButton` filled with warning red (`#EF4444`) and bold white typography.
- **Auth Inputs**: Clean floating labels with modern soft focus borders, removing all placeholder strings.

---

## 3. Key Product Decisions & Trade-Offs

- **Disabling Auto-Login Seed**:
  - *Chosen Approach*: `seedDefaultUsersIfEmpty()` in `AuthManager.kt` will create the demo admin account with `isCurrentSession = false`.
  - *Why*: Previously, seeding forced `isCurrentSession = true` on every fresh start, which bypassed login automatically. Disabling this flag ensures new users are strictly met with the login screen.
- **Removing Guest Mode**:
  - *Chosen Approach*: Remove `isGuestMode` bypass from `MainActivity.kt` and `AuthScreen.kt`.
  - *Why*: Ensures strict compliance with user decision: "Strict authentication wall requiring sign-in before viewing anything".
- **Component Reusability**:
  - *Chosen Approach*: Elevate `PopAnimatedConfirmationDialog` into `com.example.ui.components` or share between `SetDetailsScreen.kt` and `ProfileScreen.kt`.
  - *Why*: Guarantees 100% design consistency between the 'Delete Book Listing' dialog and the 'Log Out' dialog.

---

## 4. Technical Architecture & Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                       MainActivity                          │
│                                                             │
│   Startup Auth Check (MarketplaceRepository.isLoggedIn)     │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
     [Is Authenticated]             [Is Unauthenticated]
               │                              │
               ▼                              ▼
      ┌──────────────────┐          ┌───────────────────┐
      │  MainAppContent  │          │    AuthScreen     │
      │  (Home Feed &    │          │  (Blank Inputs,   │
      │   Marketplace)   │          │   Sign-In / Up)   │
      └────────┬─────────┘          └─────────▲─────────┘
               │                              │
       [Taps Log Out]                         │
               │                              │
               ▼                              │
┌──────────────────────────────┐              │
│ PopAnimatedConfirmationDialog│              │
│ (Custom Delete-Style Modal)  │              │
│ - Cancel                     │              │
│ - Red "Log Out" Button       ├──────────────┘
└──────────────────────────────┘   Purges Session:
                                   - FirebaseAuth.signOut()
                                   - userDao.clearCurrentSession()
                                   - _currentUser = emptyProfile
                                   - _isLoggedIn = false
```

### Planned Code Changes

1. **`AuthManager.kt`**:
   - In `seedDefaultUsersIfEmpty()`, set `isCurrentSession = false` so default seed accounts do not hijack the startup session.
   - Ensure `signOut()` clears Firebase Auth and executes `userDao.clearCurrentSession()`.

2. **`MarketplaceRepository.kt`**:
   - Change default initial `_currentUser` state from `defaultAdminProfile` to `emptyGuestProfile`.
   - Update `logout()` to immediately set `_isLoggedIn.value = false`, set `_currentUser.value = emptyGuestProfile`, clear cached tokens, and invoke `authManager.signOut()`.
   - Ensure `init()` accurately reflects whether a real session exists on startup.

3. **`MainActivity.kt`**:
   - Remove `isGuestMode` and `onDismiss` bypass logic in `MainApp`.
   - Direct strictly to `AuthScreen` whenever `!isLoggedIn`.
   - When user logs in/signs up, update `isLoggedIn` to route to `MainAppContent`.

4. **`AuthScreen.kt`**:
   - Remove all placeholder text arguments (`placeholder = { Text(...) }`) across all input fields (Sign-In email, Sign-In password, Sign-Up full name, email, phone number, etc.).
   - Verify all input fields start strictly with `""` and display clean floating labels without example text.

5. **`ProfileScreen.kt`**:
   - Replace the basic `AlertDialog` for logout with `PopAnimatedConfirmationDialog`.
   - Configure title "Log Out?", description "Are you sure you want to log out of your student account? You will need to sign in again to access the marketplace.", icon `Icons.AutoMirrored.Outlined.Logout`, and red confirmation button "Log Out" (`#EF4444`).
   - Trigger `MarketplaceRepository.logout()`, ensuring immediate navigation back to `AuthScreen`.

---

## 5. Verification Plan

1. **Automated Compilation**: Run `compile_applet` to verify clean build without syntax or reference errors.
2. **Automated Unit Tests**: Execute `gradle :app:testDebugUnitTest` to verify repository and authentication unit tests pass.
3. **Session Verification**:
   - Verify unauthenticated startup routes directly to `AuthScreen`.
   - Verify `AuthScreen` inputs are completely blank without example placeholders.
   - Verify successful sign-in transitions to `HomeScreen`.
   - Verify tapping "Log Out" opens the animated delete-style confirmation popup.
   - Verify confirming logout clears the session and returns to `AuthScreen`.
