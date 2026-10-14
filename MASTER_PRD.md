# MASTER PRODUCT REQUIREMENTS DOCUMENT (PRD)
## Project: BookSwipe (Polytechnic Book Exchange / ডিপ্লোমা বুকসেট মার্কেটপ্লেস)
### Application ID: `com.aistudio.bookset.market`
### Target Platform: Android 7.0+ (API 24 to 36, compileSdk 36.1) | Language: Kotlin | Framework: Jetpack Compose (Material 3)

---

# SECTION A: PRODUCT OVERVIEW

### 1. Actual Application Name and Purpose
- **Application Name (Launcher & Metadata):** `BookSwipe`
- **Full Brand Identity:** Polytechnic Book Exchange (পলিটেকনিক বুক এক্সচেঞ্জ / ডিপ্লোমা বুকসেট মার্কেটপেস)
- **Developer Credit:** `"develop by Mh Nirob"` (Explicitly placed below the authentication / logout action on the Student Profile screen).
- **Core Purpose:** A hyper-specialized peer-to-peer textbook marketplace and campus community platform tailored specifically for Diploma Engineering students enrolled in polytechnic institutes across Bangladesh (e.g., Feni Polytechnic Institute [FPI], Feni Computer Institute [FCI], and technical institutes nationwide).

### 2. Target Audience & Core Use Cases
- **Target Audience:**
  - Junior Diploma Engineering Students (Semesters 1 through 7) seeking affordable, complete semester syllabus book sets without buying expensive new copies.
  - Senior Diploma Engineering Students who have completed a semester and want to pass on or sell their idle semester textbooks directly to campus juniors.
  - Institute Faculty and Campus Community Members seeking verified student exchanges.
  - Super Administrator (`mdmaheraz65@gmail.com`) monitoring campus listing integrity, user behavior, and platform metrics.
- **Primary Use Cases:**
  1. Discovery of bundled semester book sets filtered by specific Polytechnic College, Technology (Department), and Semester.
  2. Multi-book listing publication with subject titles, editions, authors, and condition ratings.
  3. Direct student-to-student in-app messaging and real-time negotiation.
  4. "Mega Deal" campus handover scheduling with fixed meeting spot, agreed price, and exchange confirmation.
  5. Dual-mode authentication: 1-Tap Google Sign-In and secure Gmail OTP email verification.
  6. Moderation, user suspension, and analytics via the integrated Super Admin Portal.

### 3. Core User Problems Solved
1. **High Textbook Costs:** Each semester requires 6–8 distinct specialized engineering textbooks costing thousands of Taka if purchased new.
2. **Fragmentation & Lack of Domain Filtering:** General classifieds (e.g., Bikroy, Facebook groups) lack filtering by BTEB (Bangladesh Technical Education Board) Technology (Civil, Computer, Electrical, Mechanical, Power, AIDT, Telecom) and Semester (1st to 7th).
3. **Delivery Scams & Logistics Overhead:** Students study on localized campuses; BookSet eliminates courier fees and shipping risks by facilitating in-person campus meetups.
4. **Scattered Transactions:** Books are often bought and sold piecemeal; BookSet focuses on complete "Book Sets" (bundle deals) for the entire semester.

### 4. Product Boundaries (In-Scope vs. Out-of-Scope)
- **In-Scope:**
  - Campus-tailored textbook catalog and bundle search.
  - Granular multi-factor filtering (College, Technology, Semester, Price BDT, Condition).
  - Multi-book set publisher with dynamic book breakdown (Subject, Edition, Condition).
  - In-app chat, message persistence, and unread counts.
  - Mega Deal contract negotiation, campus rendezvous scheduling, and status lifecycle.
  - Gmail OTP verification (with simulated notification preview & auto-fill for testing/emulators) and Google Sign-In (Credential Manager).
  - Full local persistence via Android Room Database (SQLite) with reactive StateFlow streams.
  - Administrative telemetry, user account moderation (`ACTIVE`, `SUSPENDED`, `BANNED`), and listing management.
- **Out-of-Scope:**
  - Third-party payment gateways (bKash/Nagad/Cards in-app escrow) — payments are made physically during campus handover.
  - Delivery/courier tracking — handovers occur in person at designated campus landmarks.

### 5. Application Architecture & Technology Stack
- **Architecture:** Clean MVVM (Model-View-ViewModel) + Repository Pattern with Reactive StateFlow and Kotlin Coroutines.
- **Language:** Kotlin 100%
- **UI Toolkit:** Jetpack Compose with Material 3 (BOM `androidx.compose:compose-bom:2025.02.00`).
- **Styling Architecture:** Custom iOS-grade **Liquid Glassmorphism** (`LiquidGlassBackdrop`, frosted specular rims, chromatic ambient caustics, and neumorphic tactile elevations).
- **Persistence Layer:** Android Room Database v1 (`polytechnic_marketplace.db`) with SQLite, Foreign Keys, cascading deletions, and reactive Flows.
- **Authentication & Identity:** 
  - Dual Mode: Gmail OTP (6-digit PIN verification via `EmailOtpManager`) and Google Sign-In (`androidx.credentials:credentials:1.5.0-rc01`, `com.google.android.libraries.identity.googleid:googleid:1.1.1`).
  - Hashing: SHA-256 for local password storage.
- **Image Pipeline:** Coil (`coil-kt:coil-compose:2.7.0`) and internal app storage (`ImageStorageManager`).
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`), Android Gradle Plugin, minSdk 24 (Android 7.0), targetSdk 36 (Android 15), compileSdk 36.1.

---

# SECTION B: COMPLETE PAGE AND SCREEN INVENTORY

The application features a single-activity architecture (`MainActivity.kt`) managed by a 5-tab `HorizontalPager` navigation bar, with a type-safe modal/sub-screen destination stack (`AppDestination`).

### 1. Global Navigation Architecture
- **Root Container:** `MainActivity.kt` with `LiquidGlassBackdrop`.
- **Top-Level Tabs (HorizontalPager):**
  - Tab 0: `HomeScreen` (Route: `BottomNavTab.HOME`)
  - Tab 1: `SearchScreen` (Route: `BottomNavTab.SEARCH`)
  - Tab 2: `SellScreen` (Route: `BottomNavTab.SELL`)
  - Tab 3: `InboxScreen` (Route: `BottomNavTab.INBOX`)
  - Tab 4: `ProfileScreen` (Route: `BottomNavTab.PROFILE`)
- **Sub-Screen Navigation Stack (`AppDestination`):**
  - `AppDestination.Main`: Displays the 5-tab pager and floating bottom navigation bar.
  - `AppDestination.SetDetails(val set: BookSet)`: Detailed view of a specific book bundle.
  - `AppDestination.Chat(val conversationId: String)`: Direct student negotiation and Mega Deal dialogue.
  - `AppDestination.AdminPanel`: Super Admin portal (restricted to role `ADMIN`).
  - `AppDestination.Notifications`: Notification center with activity feed.
- **Back-Stack Handling:** Android `BackHandler` navigates from sub-screens back to `AppDestination.Main`. When on tabs 1–4, pressing back returns to Tab 0 (`HomeScreen`).

---

### 2. Screen Specifications

#### 2.1 Authentication Screen (`AuthScreen.kt`)
- **Entry Point:** Mandatory initial destination immediately upon opening the app (`MarketplaceRepository.isLoggedIn` defaults to `false` and `onDismiss = null`). Users must authenticate before accessing the home screen.
- **Layout Structure:**
  - Top: Header with app branding ("BookSwipe"), Bengali subtitle ("পলিটেকনিক বুক এক্সচেঞ্জ"), and campus community tag.
  - Segmented Tab Switcher: "লগইন" (Sign In) vs. "নতুন একাউন্ট" (Sign Up).
- **Sign In Tab:**
  - Input: Email (`testTag("input_signin_email")`) and Password (`testTag("input_signin_password")`) with visibility toggle. Blank by default without auto-fill or suggestions.
  - Action: "লগইন করুন" (Sign In) button (`testTag("btn_signin_submit")`).
  - Google Sign-In: 1-Tap Google button (`testTag("btn_google_signin")`) invoking Credential Manager with immediate responsive feedback.
- **Sign Up Tab (with Gmail OTP Verification Flow):**
  - Input Fields: Full Name, Email, Password, Mobile Number, Polytechnic Institute (Dropdown with 12+ institutes), Engineering Technology (Dropdown), Academic Semester (Dropdown 1–7).
  - Action: "ওটিপি কোড পাঠান" (Send OTP Code) (`testTag("btn_signup_submit")`).
  - Preconditions: Name >= 2 chars, valid email syntax, password >= 4 chars, phone >= 6 digits.
- **Gmail OTP Modal/Verification Sub-State (`isVerifyingOtp = true`):**
  - UI Header: Gmail logo badge, verification message with masked email (e.g. `m***5@gmail.com`).
  - Simulator Card: Displays the dispatched 6-digit OTP code with a 1-tap "অটো-ফিল করুন" (Auto-fill) button.
  - OTP Input: 6 individual segmented PIN boxes with highlighted active borders, backspace handling, and hidden numeric keyboard focus.
  - Countdown Timer: 60-second live ticker.
  - Resend Link: Active once countdown reaches 00:00.
  - Submit Action: "যাচাই করুন এবং লগইন" (Verify OTP & Log In) button.

#### 2.2 Home Screen (`HomeScreen.kt`)
- **Layout Structure:**
  - Top Bar: App logo, campus greeting, Notification bell icon with unread count badge (`testTag("btn_notifications")`).
  - Hero Campus Banner: Gradient glass banner highlighting student community stats (Active Book Sets, Verified Campus Deals).
  - Quick Department Pills (Horizontal Row): Civil, Computer, Electrical, Mechanical, Power, AIDT, Telecommunication. Tapping immediately pre-filters Search.
  - Semester Filter Tabs: Horizontal chips for Semester 1 to Semester 7.
  - Mega Deals Section: Featured carousel of active bundles with discounted prices and meetup locations.
  - Book Sets Grid: 2-column staggered/adaptive card grid displaying book covers, bundle titles, college, condition pill (Like New, Good, Used), price in BDT (৳), seller avatar, and favorite heart toggle.
  - Empty State: Illustrated placeholder if no book sets are listed for the selected semester.

#### 2.3 Search & Discovery Screen (`SearchScreen.kt`)
- **Layout Structure:**
  - Search Header: Back button, search input field with clear button (`testTag("input_search_query")`).
  - Filter Tray: Collapsible filter pills for Technology, Semester, College, and Price Range (Min/Max BDT).
  - Results List / Grid: Live filtered results updating in real-time as the user types or toggles filters.
  - Empty Results View: "কোনো বই পাওয়া যায়নি" (No books found) message with a 1-tap "ফিল্টার রিসেট করুন" (Reset Filters) button.

#### 2.4 Sell & Publish Screen (`SellScreen.kt`)
- **Layout Structure:**
  - Header: "নতুন বই সেট বিক্রি করুন" (List New Book Set).
  - Cover Image Picker: Preset diploma covers or custom image upload from device gallery (`ImageStorageManager`).
  - Bundle Details: Set Title, Description, College selector, Technology dropdown, Semester dropdown, Overall Condition selector (Like New, Good, Used, Heavily Used), Total Bundle Price in BDT (`৳`).
  - Included Books Breakdown: Dynamic itemized list where sellers add individual books belonging to the bundle:
    - Subject Name, Edition, Author (optional), Condition.
    - "+ আরেকটি বই যোগ করুন" (Add Another Book) action.
    - Delete book row action.
  - Seller Contact Info: Phone number and email confirmation.
  - Publish Action: "পোস্ট করুন" (Publish Listing) button (`testTag("btn_publish_set")`).
  - Validation: Enforces non-empty title, price > 0, at least 1 included book, valid phone.

#### 2.5 Inbox & Conversations Screen (`InboxScreen.kt`)
- **Layout Structure:**
  - Header: "মেসেজ" (Messages) with total unread badge count.
  - Conversation List: Vertical cards for active chats:
    - Other party's avatar, online green indicator, name, and polytechnic college badge.
    - Related Book Set title snippet and price.
    - Last message preview with timestamp.
    - Unread message counter badge.
  - Empty State: "কোনো বার্তা নেই" (No conversations yet) with a button to explore the Home marketplace.

#### 2.6 Chat & Deal Negotiation Screen (`ChatScreen.kt`)
- **Layout Structure:**
  - Header: Back arrow, seller/buyer avatar, student name, institute name, book thumbnail, and "প্রস্তাবিত মেগা ডিল" (Propose Mega Deal) button.
  - Mega Deal Banner (Conditional): Displays deal state (`PENDING`, `CONFIRMED`, `SOLD`, `CANCELLED`) with agreed price, meetup spot, and meeting time. If `PENDING` and viewer is seller: includes "Accept" and "Decline" buttons.
  - Message Bubbles List: Left-aligned (received) vs. Right-aligned (sent) bubbles with timestamps and frosted glass containers.
  - Propose Mega Deal Dialog: Modal form to specify:
    - Agreed Final Price in BDT (`৳`).
    - Campus Meetup Point (e.g., "FPI Main Gate", "Library Cafeteria", "Academic Building 2").
    - Scheduled Date and Time.
  - Bottom Chat Bar: Text input field and send icon button.

#### 2.7 Book Set Details Screen (`SetDetailsScreen.kt`)
- **Layout Structure:**
  - Hero Image Carousel / Cover Header: Full-width book cover with gradient scrim, back button, and save/favorite bookmark button.
  - Pricing & Title Card: Set title, price in BDT (৳), condition tag, publication date.
  - Seller Info Card: Seller name, polytechnic institute, department, semester, verified badge, and direct call/SMS button.
  - Itemized Book List: Expandable card showing every individual book included in the set (Subject, Edition, Condition).
  - Campus Handover Info: Campus meeting location and notes.
  - Bottom Action Bar:
    - If viewer is the seller: "বিক্রি সম্পন্ন হয়েছে (Mark as Sold)" button.
    - If viewer is a buyer: "মেসেজ পাঠান (Message Seller)" primary action button.

#### 2.8 Student Profile Screen (`ProfileScreen.kt`)
- **Layout Structure:**
  - Profile Card: Student avatar (supports custom photo upload or fallback placeholder), student name, polytechnic college, engineering technology, semester, phone, and email.
  - Tab Switcher: "আমার বিজ্ঞাপন" (My Listings) vs. "সংরক্ষিত বই" (Saved Sets).
  - Admin Panel Entry: Displayed only if `currentUser.role == "ADMIN"` (Vibrant gradient button to launch Super Admin Panel).
  - Logout Action: "লগআউট" (Logout) button with alert confirmation dialog.
  - Developer Credit: Centered attribution text: **`develop by Mh Nirob`** located directly below the logout section (`testTag("text_developer_credit")`).

#### 2.9 Super Admin Panel (`AdminPanelScreen.kt`)
- **Access Rule:** Strictly gated behind `role == "ADMIN"` (Master admin: `mdmaheraz65@gmail.com`).
- **Layout Structure:**
  - Header: Back button, "অ্যাডমিন ড্যাশবোর্ড" (Admin Dashboard), live status chip.
  - Live Telemetry Metrics:
    - Today's Visits, Daily Active Users (DAU), Inquiries, Active Deals.
    - Campus Traffic Share: Progress bar comparing Feni Polytechnic (FPI) vs. Feni Computer (FCI) vs. others.
  - Tab Switcher: "শিক্ষার্থী তালিকা" (Students Directory), "বই সেট সমূহ" (Book Listings), "মেগা ডিল" (Deals).
  - Student Moderation Actions:
    - View student profile, contact info, and listings count.
    - Change status: `ACTIVE`, `SUSPENDED`, or `BANNED`.
  - Listing Moderation Actions:
    - Remove flagged or fraudulent listings.
    - Toggle listing status (`AVAILABLE` vs. `SOLD`).
  - Direct Provisioning: Dialogs to manually add verified students or seed new book sets.

#### 2.10 Notification Screen (`NotificationScreen.kt`)
- **Layout Structure:**
  - Header: Back button, "বিজ্ঞপ্তি" (Notifications), "সব পঠিত হিসেবে চিহ্নিত করুন" (Mark All as Read) action.
  - Notification Feed: Grouped cards categorized into Deals, Messages, System Updates, and Campus Alerts.
  - Item Interaction: Tapping a deal notification routes directly to the relevant `SetDetails` or `Chat`.

---

# SECTION C: DETAILED UI/UX DESIGN SPECIFICATION

### 1. Visual Design Philosophy: Liquid Glassmorphism & Material 3
The design language combines iOS-grade translucent glassmorphism with tactile neumorphic elevation, using Android Material 3 foundations:
- **Glass Specular Sheen:** Top-to-bottom translucent specular gradient giving the appearance of high-refractive glass.
- **Frosted Rim Stroke:** 1.2dp border with dual-phase linear gradient (`GlassRimBrush`) simulating light reflecting off polished glass edges.
- **Tactile Neumorphic Elevation:** 3D extruded drop shadows (`SoftShadowColor`) that compress dynamically when pressed (`isPressed` state).
- **Physical Inner Shadows:** Top specular bevel and bottom inner shadow providing real 3D depth inside cards and inputs.

### 2. Measured Color Tokens (`Color.kt`)
```kotlin
// Canvas & Ambient
val BgCanvas = Color(0xFFF8FAFC)
val BgCanvasGradientStart = Color(0xFFF8FAFC)
val BgCanvasGradientEnd = Color(0xFFEFF6FF)

// Ambient Caustics & Chromatic Flares (Beta Blue & Cyan)
val CausticPurple = Color(0x353B82F6) // Vibrant Blue Bloom
val CausticCyan = Color(0x3038BDF8)
val CausticPeach = Color(0x2060A5FA)
val CausticBlue = Color(0x402563EB)

// Liquid Glass Surfaces
val CardGlass = Color(0xFFFFFFFF).copy(alpha = 0.85f)
val CardGlassBorder = Color.White.copy(alpha = 0.90f)
val CardGlassSoft = Color(0xFFFFFFFF).copy(alpha = 0.65f)
val GlassHighlightWhite = Color.White.copy(alpha = 0.92f)

// Primary Brand Colors (Beta Blue)
val PrimaryBlue = Color(0xFF2563EB)
val PrimaryBlueEnd = Color(0xFF3B82F6)
val PrimaryBlueLight = Color(0xFFEFF6FF)
val PrimaryBlueGlow = Color(0x502563EB)
val PrimaryBlueInnerShadow = Color(0x351E3A8A)

// Secondary Cyan / Sky
val SecondaryCyan = Color(0xFF0284C7)
val SecondaryCyanEnd = Color(0xFF38BDF8)
val SecondaryCyanGlow = Color(0x4D0284C7)

// Text Tokens
val TextPrimary = Color(0xFF0F172A)    // Slate 900
val TextSecondary = Color(0xFF475569)  // Slate 600
val TextMuted = Color(0xFF94A3B8)      // Slate 400
```

### 3. Typography & Sizing
- **Font Scale:** Native Material 3 Roboto with custom weights (`FontWeight.Medium`, `FontWeight.SemiBold`, `FontWeight.Bold`).
- **Headers:** 20.sp–24.sp, Bold, letterSpacing -0.5.sp.
- **Card Titles:** 15.sp–16.sp, SemiBold.
- **Body & Subtitles:** 13.sp–14.sp, Regular/Medium.
- **Captions & Tags:** 11.sp–12.sp, Medium/SemiBold.
- **Developer Credit:** 13.sp, SemiBold, letterSpacing 0.4.sp, color `TextMuted`.

### 4. Spacing, Geometry & Component Metrics
- **Grid System:** 8dp standard grid (8dp, 12dp, 16dp, 24dp padding).
- **Corner Radii:**
  - Large Surface Cards: `RoundedCornerShape(24.dp)` or `RoundedCornerShape(28.dp)`.
  - Buttons: `RoundedCornerShape(16.dp)` or `RoundedCornerShape(18.dp)`.
  - Input Fields: `RoundedCornerShape(16.dp)`.
  - Filter Pills / Tags: `CircleShape` or `RoundedCornerShape(12.dp)`.
- **Touch Targets:** Minimum 48dp x 48dp on all interactive elements.

### 5. Animation Physics
- **Tab Transitions:** `spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)`.
- **Sub-Screen Slide Transitions:** Horizontal slide with 35% offset and simultaneous 220ms alpha crossfade.
- **Floating Bottom Bar Visibility:** Vertical spring enter/exit (`dampingRatio = 0.82f`).

---

# SECTION D: COMPLETE FEATURE & INTERACTION INVENTORY

| Feature ID | Feature Name | User Roles | Preconditions | Trigger & Sequence | Expected Outcome | Error Handling |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **FEAT-01** | Google 1-Tap Sign-In | All | Google Play Services available | Tap "Google দিয়ে সাইন ইন করুন" | Authenticates via Credential Manager, matches/creates user in Room, transitions to MainApp. | Fallback error message if cancelled or network error. |
| **FEAT-02** | Gmail OTP Sign-Up | New Student | Unregistered email | Fill signup form -> Tap "ওটিপি কোড পাঠান" | Dispatches 6-digit code via `EmailOtpManager`, triggers notification, displays verification card. | Validates name >= 2, valid email format, phone >= 6 digits. |
| **FEAT-03** | OTP Verification | New Student | Code dispatched | Enter 6 digits in PIN boxes -> Tap "যাচাই করুন" | Validates code matches; inserts `UserEntity` into Room; marks current session active. | "ভুল ওটিপি কোড" if mismatched; "কোডটির মেয়াদ শেষ" after 5 mins. |
| **FEAT-04** | Semester & Tech Filtering | All Students | On Home or Search | Tap Tech pill (e.g. Civil) or Sem chip (e.g. Sem 3) | Dynamically filters `bookSets` Flow; updates card grid instantly. | Displays empty illustration if zero matches found. |
| **FEAT-05** | Publish Book Set | Logged-in Student | Active session | Fill Sell form with >= 1 book -> Tap "পোস্ট করুন" | Atomically inserts `BookSetEntity` and `IncludedBookEntity` items in Room. | Highlights missing required fields (title, price). |
| **FEAT-06** | Student In-App Chat | Logged-in Student | Active book set | Tap "মেসেজ পাঠান" on SetDetails | Opens or retrieves `ConversationEntity`; appends `ChatMessageEntity` on send. | Disables send if message text is blank. |
| **FEAT-07** | Propose Mega Deal | Student Buyer | Active conversation | Tap "প্রস্তাবিত মেগা ডিল" in Chat -> Enter price & meetup point | Creates `MegaDealEntity` in `PENDING` state; renders proposal card in chat. | Enforces positive price and specified location. |
| **FEAT-08** | Accept / Reject Deal | Student Seller | Pending Mega Deal | Tap "Accept" or "Decline" on Deal Banner | Updates `MegaDealEntity.status` to `CONFIRMED` or `CANCELLED`; dispatches notification. | Alerts both parties. |
| **FEAT-09** | Mark Set as SOLD | Seller / Admin | Active or Confirmed set | Tap "বিক্রি সম্পন্ন হয়েছে" on SetDetails or Chat | Updates `BookSetEntity.status = "SOLD"`; deal status = `SOLD`; generates notification. | Disables further negotiations for this set. |
| **FEAT-10** | Save / Bookmark Set | Logged-in Student | Any available set | Tap heart icon on card or SetDetails | Toggles ID in `savedSetIds`; updates `isSaved` in Room; reflects in Profile tab. | Instant optimistic state update. |
| **FEAT-11** | User Moderation | Super Admin | `role == "ADMIN"` | In Admin Panel -> Select student -> Tap Suspend/Ban | Updates `UserEntity.accountStatus`; persists in Room; bans user from publishing. | Immediate reflected telemetry. |
| **FEAT-12** | Developer Attribution | All | On Profile Screen | Scroll to bottom of Profile | Displays `"develop by Mh Nirob"` centered text. | Verified static placement. |

---

# SECTION E: USER JOURNEYS & STATE TRANSITIONS

```
                    ┌─────────────────────────┐
                    │       Launch App        │
                    └────────────┬────────────┘
                                 │
                     [ Is User Logged In? ]
                                 │
                 ┌───────────────┴───────────────┐
                 │ NO                            │ YES
                 ▼                               ▼
      ┌─────────────────────┐        ┌───────────────────────┐
      │     AuthScreen      │        │      MainAppContent   │
      │  (Login / Sign Up)  │        │  (HorizontalPager 5)  │
      └──────────┬──────────┘        └───────────┬───────────┘
                 │                               │
        [ User Signs Up ]               [ Select Bottom Tab ]
                 │                               │
                 ▼                               ├─► Home (Browse & Filter)
      ┌─────────────────────┐                    ├─► Search (Granular Query)
      │  Gmail OTP Request  │                    ├─► Sell (Publish Book Set)
      └──────────┬──────────┘                    ├─► Inbox (Student Chats)
                 │                               └─► Profile (Saved, Admin,
                 ▼                                            Logout, Credit)
      ┌─────────────────────┐
      │  Enter 6-Digit PIN  │
      └──────────┬──────────┘
                 │
          [ Verified? ]
                 │
                 ├─► Success: Persist User -> Auto-Login -> Enter MainApp
                 └─► Failure: Show Error -> Retry or Resend Code
```

---

# SECTION F: DATA & BACKEND SPECIFICATION

### 1. Database Implementation: Android Room (SQLite)
- **Database File:** `polytechnic_marketplace.db`
- **Database Class:** `AppDatabase.kt` (v1, `exportSchema = false`, `fallbackToDestructiveMigration()`).

### 2. Entity Schemas & Relations (`Entities.kt`)

#### `UserEntity` (Table: `users`)
- `id: String` (Primary Key)
- `name: String`
- `email: String`
- `passwordHash: String`
- `phone: String`
- `college: String`
- `technologyId: String`
- `semesterId: String`
- `profilePhotoRes: Int?`
- `profilePhotoUri: String?`
- `role: String` (Default: `"USER"`, Admin: `"ADMIN"`)
- `accountStatus: String` (Default: `"ACTIVE"`, `"SUSPENDED"`, `"BANNED"`)
- `isCurrentSession: Boolean` (Default: `false`)

#### `BookSetEntity` (Table: `book_sets`)
- `id: String` (Primary Key)
- `sellerId: String`
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
- `totalPrice: Int` (in BDT)
- `overallCondition: String` (`Like New`, `Good`, `Used`, `Heavily Used`)
- `description: String`
- `coverDrawableRes: Int?`
- `imageUrlsString: String`
- `status: String` (`AVAILABLE`, `SOLD`)
- `isSaved: Boolean`
- `createdAt: Long`

#### `IncludedBookEntity` (Table: `included_books`)
- `id: String` (Primary Key)
- `setId: String` (Foreign Key -> `book_sets.id`, Cascade Delete)
- `bookName: String`
- `subject: String`
- `author: String`
- `edition: String`
- `bookCode: String`
- `condition: String`

#### `MegaDealEntity` (Table: `mega_deals`)
- `dealId: String` (Primary Key)
- `setId: String`
- `setTitle: String`
- `buyerId: String`
- `buyerName: String`
- `sellerId: String`
- `sellerName: String`
- `finalPrice: Int`
- `location: String`
- `date: String`
- `time: String`
- `status: String` (`PENDING`, `CONFIRMED`, `SOLD`, `CANCELLED`)
- `createdAt: Long`

#### `ConversationEntity` (Table: `conversations`)
- `id: String` (Primary Key)
- `buyerId: String`
- `sellerId: String`
- `otherUserName: String`
- `otherUserCollege: String`
- `otherUserAvatarRes: Int?`
- `setId: String`
- `setTitle: String`
- `setPrice: Int`
- `lastMessage: String`
- `timestamp: String`
- `unreadCount: Int`
- `isOnline: Boolean`

#### `ChatMessageEntity` (Table: `chat_messages`)
- `id: String` (Primary Key)
- `conversationId: String` (Indexed)
- `senderId: String`
- `senderName: String`
- `text: String`
- `timestamp: String`
- `isMe: Boolean`

#### `NotificationEntity` (Table: `notifications`)
- `id: String` (Primary Key)
- `type: String` (`DEAL`, `CHAT`, `SYSTEM`, `ALERT`)
- `title: String`
- `message: String`
- `timeAgo: String`
- `isRead: Boolean`
- `relatedSetId: String?`
- `relatedDealId: String?`
- `timestamp: Long`

---

# SECTION G: TECHNICAL RECONSTRUCTION SPECIFICATION

To rebuild BookSet from scratch, execute the following steps:

1. **Project Setup & Dependencies:**
   - Configure `build.gradle.kts` with plugins: `android.application`, `kotlin.compose`, `google.devtools.ksp`, `secrets`.
   - Add Jetpack Compose BOM, Material 3, Room KSP (`androidx.room:room-compiler`), Coil Compose, and Google Credentials libraries.
   - Set `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36.1`.
2. **Design Tokens & Theme:**
   - Implement `Color.kt` with `BgCanvas`, `PrimaryBlue` (`0xFF2563EB`), `SecondaryCyan`, and frosted glass alpha tokens.
   - Implement `GlassComponents.kt` containing `Modifier.liquidGlass()` with specular gradient pass, rim stroke, and dynamic neumorphic elevation.
3. **Database & Persistence:**
   - Define all 7 Room entities and 5 DAOs (`UserDao`, `BookSetDao`, `MegaDealDao`, `ChatDao`, `NotificationDao`).
   - Implement `AppDatabase` with destructive migration fallback.
4. **Authentication & OTP:**
   - Implement `EmailOtpManager.kt` with 6-digit random code generation, expiration logic, and Android system notification channel (`gmail_otp_channel`).
   - Implement `AuthManager.kt` with SHA-256 password hashing.
5. **Repository Layer:**
   - Implement `MarketplaceRepository.kt` as a singleton holding `StateFlow` streams for `currentUser`, `isLoggedIn`, `bookSets`, `conversations`, `megaDeals`, and `notifications`.
6. **Navigation & Screens:**
   - Implement `MainActivity.kt` with `HorizontalPager(pageCount = 5)` and `AnimatedContent` for `AppDestination` stack.
   - Build `HomeScreen`, `SearchScreen`, `SellScreen`, `InboxScreen`, `SetDetailsScreen`, `ChatScreen`, `AdminPanelScreen`, `NotificationScreen`, and `ProfileScreen`.
   - Ensure `ProfileScreen.kt` renders `"develop by Mh Nirob"` directly under the logout action.

---

# SECTION H: ASSET & CONTENT INVENTORY

### 1. Vector & Drawable Assets (`app/src/main/res/drawable/`)
- `bookset_launcher_icon_1789677723796.jpg`: Primary launcher brand icon.
- `hero_banner_poly_1789735532124.jpg`: Campus hero banner art.
- `book_cover_civil_1789677775416.jpg`: Diploma Civil textbook set cover.
- `book_cover_cse_1789735549141.jpg`: Diploma Computer Technology textbook set cover.
- `book_cover_elec_1789677789123.jpg`: Diploma Electrical textbook set cover.
- `book_cover_mech_1789677808779.jpg`: Diploma Mechanical textbook set cover.
- `avatar_rahim_1789677826412.jpg`: Rahim student avatar.
- `avatar_nusrat_1789735573987.jpg`: Nusrat student avatar.

### 2. Colleges & Curriculums (`CollegeDirectory.kt`)
- Feni Polytechnic Institute (FPI)
- Feni Computer Institute (FCI)
- Dhaka Polytechnic Institute (DPI)
- Chittagong Polytechnic Institute (CPI)
- Comilla Polytechnic Institute
- Sylhet Polytechnic Institute
- Rajshahi Polytechnic Institute
- Bogura Polytechnic Institute
- Barishal Polytechnic Institute
- Rangpur Polytechnic Institute
- Mymensingh Polytechnic Institute
- Dhaka Mohila Polytechnic Institute

---

# SECTION I: SECURITY, RELIABILITY & QUALITY

1. **Password Security:** Student passwords are encrypted using SHA-256 before insertion into Room database (`AuthManager.kt`). Plaintext passwords are never persisted.
2. **OTP Anti-Abuse:** OTP codes are restricted to 5-minute validity, single-use invalidation upon verification, and a 60-second cooldown timer on resend requests.
3. **Zero-Crash Resilience:** All asynchronous database transactions run on `Dispatchers.IO` with `SupervisorJob()`. UI collectors utilize null-safe operators and default fallbacks.
4. **Offline First:** All core user interactions (browsing book sets, viewing saved listings, searching, creating drafts) execute against local SQLite via Room and work seamlessly with no internet connection.

---

# SECTION J: KNOWN GAPS & REBUILD RISKS

1. **SMTP vs. System OTP Notification:** The app uses an Android Notification Channel (`gmail_otp_channel`) and in-app simulation card for OTP delivery rather than a live external SMTP relay (e.g., SendGrid/Firebase Auth SMS), ensuring 100% offline and emulator testability without external API fees.
2. **Peer-to-Peer Chat Sync Across Multiple Physical Devices:** Real-time chat messages are stored locally in Room. For multi-device synchronization across two separate physical phones, a remote Firestore / WebSocket synchronization layer can be connected to the existing `ChatDao` repository triggers.
3. **In-Person Physical Payment:** There is deliberately no third-party payment gateway SDK (such as SSLCommerz or bKash API) embedded; payments are settled physically in cash or student-to-student transfer at the campus meetup point.
