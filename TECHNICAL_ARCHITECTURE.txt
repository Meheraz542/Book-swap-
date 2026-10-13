# TECHNICAL ARCHITECTURE SPECIFICATION
## Application: Polytechnic Book Exchange (`BookSet`)

---

## 1. System Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│  MainActivity  ├── HorizontalPager 5-Tab Coordinator (Nav)             │
│                ├── AuthScreen (Sign In / Sign Up / Gmail OTP)          │
│                ├── HomeScreen / SearchScreen / SellScreen              │
│                ├── InboxScreen / ChatScreen                            │
│                ├── ProfileScreen (Profile & "develop by Mh Nirob")     │
│                ├── SetDetailsScreen / NotificationScreen               │
│                └── AdminPanelScreen (Telemetry / Moderation)           │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ StateFlow / SharedFlow
┌────────────────────────────────────▼───────────────────────────────────┐
│                           BUSINESS LOGIC LAYER                         │
│  • MarketplaceRepository (Singleton Reactive Store)                    │
│  • AuthViewModel (Jetpack Credential Manager & Google Sign-In)         │
│  • EmailOtpManager (6-digit generation, expiration, notifications)     │
│  • ImageStorageManager (Internal photo persistence)                    │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ Room Coroutines / Flow
┌────────────────────────────────────▼───────────────────────────────────┐
│                             DATA ACCESS LAYER                          │
│  • AppDatabase (Room SQLite v1: polytechnic_marketplace.db)            │
│  • DAOs: UserDao, BookSetDao, MegaDealDao, ChatDao, NotificationDao    │
│  • Entities: 7 Room Entities with Foreign Keys and Relations          │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Room Database Schema & Entity Specification

### 2.1 `UserEntity` (Table: `users`)
- `id: String` (Primary Key, e.g. `user_a1b2c3d4`, `admin_master_1`, `google_<uid>`)
- `name: String`
- `email: String` (Unique student email)
- `passwordHash: String` (SHA-256 hashed password)
- `phone: String` (Contact phone e.g. `01712-345678`)
- `college: String` (e.g. `Feni Polytechnic Institute`)
- `technologyId: String` (`civil`, `electrical`, `mechanical`, `power`, `cse`, `aidt`, `telecom`)
- `semesterId: String` (`1` through `7`)
- `profilePhotoRes: Int?` (Drawable avatar resource)
- `profilePhotoUri: String?` (Local URI from camera or gallery)
- `role: String` (`USER` or `ADMIN`)
- `accountStatus: String` (`ACTIVE`, `SUSPENDED`, `BANNED`)
- `isCurrentSession: Boolean` (Active login flag)

### 2.2 `BookSetEntity` (Table: `book_sets`)
- `id: String` (Primary Key, e.g. `set_civil_4th_1`)
- `sellerId: String` (Foreign link to user)
- `sellerName: String`
- `sellerCollege: String`
- `sellerTech: String`
- `sellerSem: String`
- `sellerPhone: String`
- `sellerEmail: String`
- `title: String`
- `subject: String`
- `technologyId: String`
- `semesterId: String`
- `college: String`
- `totalPrice: Int` (Price in Bangladeshi Taka ৳)
- `overallCondition: String` (`Like New`, `Good`, `Used`, `Heavily Used`)
- `description: String`
- `coverDrawableRes: Int?`
- `imageUrlsString: String` (Comma-separated local URIs)
- `status: String` (`AVAILABLE`, `SOLD`, `REMOVED`)
- `isSaved: Boolean`
- `createdAt: Long`

### 2.3 `IncludedBookEntity` (Table: `included_books`)
- `id: String` (Primary Key)
- `setId: String` (Foreign Key referencing `book_sets.id` on delete CASCADE)
- `bookName: String`
- `subject: String`
- `author: String`
- `edition: String`
- `bookCode: String`
- `condition: String`

### 2.4 `MegaDealEntity` (Table: `mega_deals`)
- `dealId: String` (Primary Key)
- `setId: String`
- `setTitle: String`
- `buyerId: String`
- `buyerName: String`
- `sellerId: String`
- `sellerName: String`
- `finalPrice: Int` (Agreed price in BDT)
- `location: String` (Meetup location on campus)
- `date: String`
- `time: String`
- `status: String` (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`)
- `createdAt: Long`

### 2.5 `ConversationEntity` & `ChatMessageEntity` (Tables: `conversations`, `chat_messages`)
- Conversation metadata with last message, unread count, participant IDs.
- Chat message with sender ID, recipient ID, text, timestamp, and read status.

### 2.6 `NotificationEntity` (Table: `notifications`)
- `id: String` (Primary Key)
- `type: String` (`SECURITY`, `MEGA_DEAL_CONFIRMED`, `SET_SOLD`, `WELCOME`, `INFO`)
- `title: String`
- `message: String`
- `timeAgo: String`
- `isRead: Boolean`
- `relatedSetId: String?`
- `relatedDealId: String?`
- `timestamp: Long`

---

## 3. Service Layer Specifications

### 3.1 `EmailOtpManager` (`com.example.data.auth.EmailOtpManager`)
- **6-Digit Generation:** Cryptographically secure random integer `100000..999999`.
- **Pending Registration Cache:** Stores pending `name`, `email`, `password`, `phone`, `college`, `technology`, `semester`, `otpCode`, `expiresAt`.
- **Expiration Policy:** 5 minutes (300 seconds).
- **System Notification Dispatch:**
  - Android `NotificationChannel` with ID `gmail_otp_channel`, importance `HIGH`.
  - Dispatches `NotificationCompat.Builder` with title "Gmail • পলিটেকনিক বুক এক্সচেঞ্জ" and OTP content text.
- **In-App Dispatch:** Adds notification to `MarketplaceRepository.notifications` StateFlow and inserts into Room DB.
- **Verification Logic:**
  - Checks expiration timestamp.
  - Matches entered code with expected code.
  - On match, consumes pending registration and executes `AuthManager.signUp(...)`.

### 3.2 `AuthViewModel` & Google Sign-In
- Uses Android Jetpack `CredentialManager` API with `GetSignInWithGoogleOption`.
- Extracts `GoogleIdTokenCredential` and authenticates against `FirebaseAuth`.
- Fallback mechanism: In environments without Google Play Services (e.g. cloud streaming emulator), falls back gracefully to a verified student session.

---

## 4. Build System & Gradle Configuration

- **Gradle Version:** 9.3.1 (configured in `gradle-wrapper.properties`).
- **Android Gradle Plugin (AGP):** Compatible with Kotlin Compose Plugin.
- **Java Toolchain:** JDK 17 (Temurin).
- **Key Files in Repository Root:**
  - `gradlew` (Executable shell script, permission 755)
  - `gradlew.bat` (Windows batch script)
  - `gradle/wrapper/gradle-wrapper.jar` (Wrapper launcher JAR)
  - `gradle/wrapper/gradle-wrapper.properties` (Distribution URL pointing to Gradle 9.3.1)
  - `settings.gradle.kts` (`rootProject.name = "BookSet"`, `include(":app")`)
  - `app/build.gradle.kts`
  - `.github/workflows/android-apk.yml` (CI/CD pipeline for automated APK builds)
