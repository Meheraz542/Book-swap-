# FEATURE ACCEPTANCE CRITERIA
## Application: Polytechnic Book Exchange (`BookSet`)

---

## 1. Authentication & Security Acceptance Criteria

### 1.1 Sign In Flow
- [x] **AC-AUTH-01:** Submitting valid email and matching password successfully authenticates and transitions to `MainAppContent`.
- [x] **AC-AUTH-02:** Submitting blank email or password displays immediate inline validation error in red.
- [x] **AC-AUTH-03:** Submitting mismatched password displays `"Incorrect password. Please try again."`.
- [x] **AC-AUTH-04:** Super admin email `mdmaheraz65@gmail.com` with `polytechnic123` grants `ADMIN` role.
- [x] **AC-AUTH-05:** Tapping "Sign in with Google" triggers Jetpack Credential Manager Google ID option.

### 1.2 Sign Up & Gmail OTP Verification
- [x] **AC-OTP-01:** Sign Up form requires Full Name (>=2 chars), valid email regex, Password (>=4 chars), Phone (>=6 chars), College, Tech, Semester.
- [x] **AC-OTP-02:** Submitting valid Sign Up does NOT immediately log in; it generates a 6-digit cryptographic OTP and transitions to the OTP Verification Card.
- [x] **AC-OTP-03:** The OTP card displays the user's email with an edit icon that allows cancelling back to the form.
- [x] **AC-OTP-04:** A simulated incoming Gmail notification card is rendered on screen containing the generated OTP code and an "Auto-fill" chip button.
- [x] **AC-OTP-05:** Tapping "Auto-fill" immediately populates the 6 PIN boxes.
- [x] **AC-OTP-06:** Entering an incorrect 6-digit code and tapping "Verify" displays: `"ভুল ওটিপি কোড! অনুগ্রহ করে আপনার জিমেইলে আসা ৬-সংখ্যার কোডটি দিন।"`.
- [x] **AC-OTP-07:** Resend button is disabled during the 60-second countdown; once countdown reaches 0, it enables and issues a fresh OTP.
- [x] **AC-OTP-08:** Correct OTP verification persists user to Room DB, inserts welcome notification, and navigates into the main app.

---

## 2. Navigation & Shell Acceptance Criteria

- [x] **AC-NAV-01:** Floating bottom bar displays 5 tabs (Home, Search, Sell, Inbox, Profile).
- [x] **AC-NAV-02:** HorizontalPager swipe gestures or bottom bar clicks smoothly animate between tabs.
- [x] **AC-NAV-03:** Inbox tab displays a red numerical badge reflecting total unread messages across conversations.
- [x] **AC-NAV-04:** Bottom bar respects system navigation bar insets and floats above content with no overlap.

---

## 3. Feed & Search Acceptance Criteria

- [x] **AC-FEED-01:** Home screen displays technology chips (Civil, Computer, Electrical, Mechanical, Power, AIDT, Telecom). Selecting a chip filters listings reactively.
- [x] **AC-FEED-02:** Semester tabs (1st through 7th) filter listings by semester.
- [x] **AC-FEED-03:** Tapping a book set card opens `SetDetailsScreen` with full breakdown of included books.
- [x] **AC-FEED-04:** Heart icon toggles bookmark status in Room DB (`isSaved = true/false`).
- [x] **AC-SEARCH-01:** Search input queries book titles, subjects, and seller names in real time without lag.
- [x] **AC-SEARCH-02:** Empty search results display a clean illustration and a "Reset Filters" action.

---

## 4. Book Set Publishing (Sell) Acceptance Criteria

- [x] **AC-SELL-01:** Form validates that Title is non-empty, asking price is > 0 BDT, and at least 1 book is added.
- [x] **AC-SELL-02:** User can add multiple individual books with subject name, edition, and condition.
- [x] **AC-SELL-03:** Publishing writes both `BookSetEntity` and `IncludedBookEntity` rows to Room DB and resets the form.

---

## 5. Chat & Mega Deals Acceptance Criteria

- [x] **AC-CHAT-01:** Opening chat displays existing messages in chronological order.
- [x] **AC-CHAT-02:** Sending a text message persists `ChatMessageEntity` to Room and updates conversation timestamp.
- [x] **AC-DEAL-01:** Proposing a deal creates a `MegaDealEntity` in `PENDING` status.
- [x] **AC-DEAL-02:** Seller can accept or decline. Accepting changes status to `CONFIRMED`.
- [x] **AC-DEAL-03:** Marking exchange completed changes book set status to `SOLD` and deal status to `COMPLETED`.

---

## 6. Profile & Developer Attribution Acceptance Criteria

- [x] **AC-PROF-01:** Profile displays student photo, name, college, technology, and semester.
- [x] **AC-PROF-02:** "My Listings" tab shows only listings created by the logged-in student.
- [x] **AC-PROF-03:** "Saved Sets" tab displays bookmarked listings.
- [x] **AC-PROF-04:** Tapping Log Out displays confirmation dialog; confirming clears current session and resets `isLoggedIn = false`.
- [x] **AC-PROF-05 (CRITICAL):** Directly beneath the Log Out option, the attribution text **`develop by Mh Nirob`** is explicitly rendered.

---

## 7. Super Admin Portal Acceptance Criteria

- [x] **AC-ADMIN-01:** Admin portal option appears ONLY when logged-in user email is `mdmaheraz65@gmail.com` or role is `ADMIN`.
- [x] **AC-ADMIN-02:** Telemetry tab displays real-time daily active users, visits today, and campus traffic share.
- [x] **AC-ADMIN-03:** Admin can Suspend, Block, or Reactivate any student user.
- [x] **AC-ADMIN-04:** Suspended students are barred from logging in with a clear warning.
