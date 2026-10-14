# Real-Time Firebase Authentication & Cloud Firestore Synchronization Plan

## Problem Diagnosis & Root Causes
1. **Local-Only Persistence:** Currently, user registrations, book sets, conversations, and deals write to an on-device local Room SQLite database (`polytechnic_marketplace.db`). This is why an admin on Phone A cannot see users or books created on Phone B.
2. **Demo Credential Exposure:** A "⚡ Quick Fill Demo" shortcut on the login screen fills admin credentials, which could allow unauthorized student users to access the admin portal.
3. **Admin Privilege Isolation:** Admin status was inferred client-side without strict server-enforced role checks and multi-factor PIN verification.

---

## Architectural Implementation Plan

### Step 1: Clean Authentication & Remove Demo Shortcuts
- **Target File:** `app/src/main/java/com/example/ui/screens/AuthScreen.kt`
- Remove the "⚡ Quick Fill Demo" button completely from the login screen.
- Ensure the login screen directs first-time and unauthenticated users to create an account or sign in with their own credentials.
- Retain clean, completely blank email and password inputs with floating labels.

### Step 2: Implement Real Firebase Authentication (Email/Password & Google Sign-In)
- **Target Files:** `app/src/main/java/com/example/data/auth/AuthManager.kt`, `app/src/main/java/com/example/ui/screens/AuthViewModel.kt`
- **Email/Password Registration:** Call `FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)`.
  - On auth success, write the new student profile document to Cloud Firestore: `/users/{uid}` with fields: `id`, `name`, `email`, `phone`, `college`, `technologyId`, `semesterId`, `role: "USER"`, `accountStatus: "ACTIVE"`, and server timestamp `createdAt`.
- **Email/Password Login:** Call `FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)`.
  - Fetch user profile from `/users/{uid}`.
- **Google Sign-In:** Authenticate with `FirebaseAuth.signInWithCredential()` using the Google ID Token via Credential Manager. Automatically sync/create student profile in `/users/{uid}` with `role: "USER"`.
- Handle failure cases, network retries, and clear error surfacing without swallowing exceptions.

### Step 3: Dual-Gate Admin Portal Security (Email + Private PIN)
- **Target Files:** `app/src/main/java/com/example/ui/screens/ProfileScreen.kt`, `app/src/main/java/com/example/ui/screens/AdminPanelScreen.kt`
- Admin privileges are restricted exclusively to `mdmaheraz65@gmail.com`.
- Even when logged in as `mdmaheraz65@gmail.com`, tapping the Admin Portal button triggers a **Secure Master Admin PIN Dialog**.
- Access is granted only upon entering the private PIN (`8426`), preventing accidental or unauthorized access on shared devices.

### Step 4: Real-Time Cloud Firestore Data Layer (`FirebaseFirestoreManager`)
- **Target File:** `app/src/main/java/com/example/data/MarketplaceRepository.kt` & new `FirebaseFirestoreManager.kt`
- Connect to the provisioned Firestore database (`ai-studio-android-bookset-4d636b89-9ee9-4e64-8c55-e9603e0d39c9`).
- **Users Collection (`/users`):**
  - Implement `addSnapshotListener` for the Admin Dashboard to stream real-time user registrations across all devices.
  - When Phone B registers, Phone A's admin user list updates instantly.
- **Book Listings Collection (`/bookSets`):**
  - Publish listings directly to Firestore with itemized books.
  - Listen via `addSnapshotListener` with status filtering (`status == "AVAILABLE"`), college, semester, and technology filters.
  - Distinguish between a genuine empty state (0 matching books) and loading/error states to fix the "No Books Found" bug.
- **Real-Time Chat & Negotiation (`/conversations` and `/conversations/{id}/messages`):**
  - Real-time Firestore snapshot listeners on active messages ordered by `timestamp ASC`.
  - Messages sent from Phone B deliver in real time to Phone A without manual screen refresh.
- **Mega Deals Collection (`/megaDeals`):**
  - Propose, accept, and update campus meetup deal status in real time.

### Step 5: Security Rules & Verification
- **Target File:** `firestore.rules`
- Audit and deploy Firestore security rules ensuring:
  - Users can only read/write their own profiles and messages in their conversations.
  - Only authenticated admin (`request.auth.token.email == "mdmaheraz65@gmail.com"`) can view all users and moderate listings.
- Run `compile_applet` and test suite to verify full build and real-time connectivity.
