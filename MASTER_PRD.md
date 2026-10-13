# MASTER PRODUCT REQUIREMENTS DOCUMENT (PRD)
## Project: Polytechnic Book Exchange (Diploma BookSet Market)
### Application ID: `com.aistudio.bookset.market`
### Target Platform: Android 7.0+ (API 24 to 36) | Language: Kotlin | Framework: Jetpack Compose (M3)

---

## 1. Product Overview & Executive Summary

### 1.1 Product Name & Branding
- **Official App Name:** BookSet (Polytechnic Book Exchange / পলিটেকনিক বুক এক্সচেঞ্জ)
- **Developer Credit:** "develop by Mh Nirob" (Displayed at the bottom of the User Profile)
- **Primary Institution Hubs:** Feni Polytechnic Institute (FPI), Feni Computer Institute (FCI), and other Technical & Polytechnic Institutes across Bangladesh.

### 1.2 Core Problem Statement
Polytechnic diploma engineering students in Bangladesh study technical curriculums structured into 7 active academic semesters across specialized technologies (Civil, Computer, Electrical, Mechanical, Power, AIDT, Telecom). 
1. New book sets are expensive, and each semester requires 6–8 specialized textbooks.
2. After passing a semester, seniors have idle book sets that junior students urgently need.
3. Generalized marketplaces lack domain-specific filtering by Technology, Semester, College, and Book Condition.
4. Students require safe campus-based transactions without courier fees or online payment scams.

### 1.3 Solution & Product Boundaries
**BookSet** is an Android-native academic marketplace and student community platform designed with an ultra-modern iOS-grade **Liquid Glassmorphic** UI and Material 3 design system.
- **In-Scope:**
  - Campus-tailored textbook exchange and syllabus book set discovery.
  - Granular filtering by Polytechnic Institution, Engineering Technology, and Semester.
  - Multi-book set bundle listings with itemized subject titles, editions, and conditions.
  - In-app student chat and negotiation.
  - "Mega Deal" scheduling with campus meetup points, agreed final price, and status tracking (Pending -> Confirmed -> Sold).
  - Secure Gmail OTP authentication and Google Sign-In with Credential Manager.
  - Local persistence via SQLite / Android Room Database with reactive flows.
  - Comprehensive Admin Panel featuring live traffic telemetry, user moderation (Suspend/Block/Activate), and listing management.
- **Out-of-Scope (Boundaries):**
  - Third-party payment gateway processing (transactions are settled physically on campus).
  - Courier / postal delivery logistics.

---

## 2. User Personas & Roles

| Role | Permissions & Capabilities |
| :--- | :--- |
| **Student Buyer (Junior)** | Browses book sets, filters by campus/tech/semester, bookmarks sets, chats with sellers, proposes Mega Deals, confirms in-person collection. |
| **Student Seller (Senior)** | Publishes bundled book sets with photos, prices, conditions, responds to inquiries in Chat, accepts/rejects Mega Deals, marks sets as SOLD. |
| **Super Admin** | Role: `ADMIN` (Master Account: `mdmaheraz65@gmail.com`). Accesses Admin Portal from Profile, monitors telemetry analytics, moderates listings, suspends/bans abusive accounts, adds verified students and sets manually. |

---

## 3. High-Level Architecture & Technical Stack

```
[ UI Layer: Jetpack Compose M3 + Liquid Glassmorphism ]
                 │
[ Navigation: HorizontalPager 5-Tab Bar + State Stack ]
                 │
[ Business Logic & ViewModel: AuthViewModel / Repository ]
                 │
[ Service Layer: EmailOtpManager + CredentialManager ]
                 │
[ Data Layer: Room Database (SQLite) + Entities + DAOs ]
```

- **Runtime:** JVM 17, Android minSdk 24, targetSdk 36, compileSdk 36.1.
- **UI Toolkit:** Jetpack Compose (BOM 2025.02.00) + Material 3.
- **Design System:** Custom iOS Liquid Glassmorphism (`LiquidGlassBackdrop`, specular highlight, frosted rim brush).
- **Local Persistence:** Room Database v1 (`polytechnic_marketplace.db`) with 7 entities.
- **Asynchronous Architecture:** Kotlin Coroutines + `StateFlow` + `SharedFlow`.
- **Identity & Security:** Dual-mode authentication (Gmail OTP 6-digit verification + Google Sign-In).

---

## 4. Feature Breakdown & Specifications

### 4.1 Authentication & Onboarding
1. **Sign In:**
   - Email & Password input.
   - 1-Tap Google Sign-In via Jetpack Credential Manager.
   - Master admin override (`mdmaheraz65@gmail.com` with `polytechnic123`).
2. **Sign Up with Gmail OTP Verification:**
   - Input fields: Full Name, Email, Password, Mobile Phone, Polytechnic College, Technology, Semester.
   - Validation: Full name >= 2 chars, valid email format, password >= 4 chars, phone >= 6 chars.
   - OTP Generation: Cryptographic 6-digit random code (valid for 5 minutes).
   - Dispatch: System notification (`gmail_otp_channel`) + In-app notification + On-screen simulation card.
   - Verification UI: 6 segmented PIN boxes, numeric keyboard, auto-advance, backspace handling, auto-fill chip, live 60-second countdown timer, resend button.
   - Account Activation: Persists verified `UserEntity` to Room and establishes active session.

### 4.2 Home Screen (`HomeScreen.kt`)
- **Top Header:** App logo emblem, campus title, unread notification bell icon with badge, search trigger.
- **Hero Campus Banner:** Dynamic greeting with college badge, academic quote, and quick stats.
- **Quick Technology Pills:** Horizontal scrollable row of engineering technologies (Civil, Computer, Electrical, Mechanical, Power, AIDT, Telecom).
- **Semester Selector:** Semester 1 through 7 quick tabs.
- **Mega Deals Carousel:** Prominent cards showcasing active discounted campus exchange bundles.
- **Book Sets Grid:** Reactive listing cards showing cover photo, title, department, condition tag, price in BDT (৳), seller avatar, and save/favorite button.

### 4.3 Search & Discovery (`SearchScreen.kt`)
- **Real-Time Query Field:** Searches across book titles, subjects, authors, and seller names.
- **Filter Chips:** Department / Technology pills, Semester selector, Price range filter (Min/Max BDT).
- **Institution Filter:** Feni Polytechnic Institute, Feni Computer Institute, etc.
- **Empty State:** Illustrated empty feedback when no matches are found with filter reset button.

### 4.4 Book Listing & Seller Publisher (`SellScreen.kt`)
- **Bundle Information:** Set title, description, overall bundle condition (Like New, Good, Used, Heavily Used), expected total price in BDT.
- **Dynamic Book Breakdown:** Ability to add multiple included individual books (Subject Name, Edition, Condition).
- **Cover Photo:** Preset diploma covers or custom image upload.
- **Validation:** Enforces title, price > 0, at least 1 book included, and seller contact.
- **Persistence:** Inserts `BookSetEntity` and related `IncludedBookEntity` records in Room atomically.

### 4.5 In-App Student Chat & Messaging (`InboxScreen.kt` & `ChatScreen.kt`)
- **Inbox List:** Active conversation cards with buyer/seller avatar, online indicator, last message snippet, timestamp, and unread count badge.
- **Chat Dialogue Screen:**
  - Header: Seller/buyer student name, college badge, online status dot, book set thumbnail, back button.
  - Message Bubbles: Left/right aligned message bubbles with frosted glass backdrop and timestamps.
  - "Propose Mega Deal" Action: Opens dialog to negotiate agreed final price (৳), meetup location (e.g. "Main Gate FPI", "Library Cafeteria"), and scheduled time.
  - Chat Input: Styled text field with send icon button.

### 4.6 Mega Deals Campus Handover Workflow
1. Buyer proposes deal inside Chat with price and meetup place.
2. A `MegaDealEntity` is created in status `PENDING`.
3. Seller receives in-chat notification banner with "Accept Deal" or "Decline" buttons.
4. When accepted, status updates to `CONFIRMED`.
5. Upon in-person handover on campus, seller marks "Exchange Completed -> Set SOLD".
6. The book set status changes to `SOLD` and notification is dispatched to both students.

### 4.7 Student Profile & Developer Attribution (`ProfileScreen.kt`)
- **Student ID Card:** Photo avatar, Full Name, Department / Technology, Current Semester, Campus, Mobile Phone, Email.
- **Tabs:** "My Listings" (active sets posted by user) and "Saved Sets" (bookmarked listings).
- **Admin Portal Trigger:** If logged-in user role is `ADMIN`, displays prominent red/purple "Open Admin Panel" entry.
- **Logout Action:** Styled button with confirmation dialog.
- **Developer Credit:** Centered attribution text: **`develop by Mh Nirob`** rendered below the logout button.

### 4.8 Super Admin Panel (`AdminPanelScreen.kt`)
- **Telemetry & Live Analytics:** Daily active users, visits today, book inquiries, chat queries, campus traffic share (FPI vs FCI progress bar), peak activity hours.
- **Student User Moderation:** Full directory of registered students with search, details, account status (`ACTIVE`, `SUSPENDED`, `BANNED`), and 1-tap moderation actions.
- **Listing Moderation:** Review all book sets, view seller details, remove flagged listings.
- **Direct Provisioning:** Add new student dialog, Add manual listing dialog.

---

## 5. Security, Reliability & Quality Assurance

1. **Local Privacy:** All sensitive student credentials (passwords) are SHA-256 hashed before storage in Room DB.
2. **Offline-First Resilience:** App functions completely offline without internet connection for browsing cached database listings.
3. **Emulator Friendly:** Gmail OTP service displays an on-screen simulation banner with 1-tap auto-fill for testing without real SMTP external connections.
4. **Zero Crashing Policy:** Fallback destructive Room migration, robust try-catch wrappers, coroutine supervisor jobs, and null-safe UI state collectors.
