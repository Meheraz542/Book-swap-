# UI/UX DESIGN SPECIFICATION
## Application: Polytechnic Book Exchange (`BookSet`)

---

## 1. Design System & Visual Philosophy

The visual design is an iOS-grade **Liquid Glassmorphism** blended with Android **Material Design 3 (M3)**.

### 1.1 Color Tokens & Palette (`Color.kt`)
- **Primary Blue:** `#2563EB` (Primary brand color for buttons, active tabs, highlights)
- **Primary Blue Glow:** `#3B82F6` (Ambient glow and focus borders)
- **Primary Purple:** `#7C3AED` (Accent gradients, deals, admin badges)
- **Secondary Teal:** `#0D9488` (Status badges, campus success tags)
- **Background Canvas:** `#F8FAFC` (Light neutral background canvas)
- **Surface Clean:** `#FFFFFF` (Card surfaces, elevated components)
- **Glass Tint:** `#F1F5F9` (Frosted glass secondary fill)
- **Text Primary:** `#0F172A` (Headings, primary labels, price tags)
- **Text Secondary:** `#475569` (Subtitles, department text, metadata)
- **Text Muted:** `#94A3B8` (Hints, timestamps, inactive borders)
- **Success Green:** `#16A34A` / Light: `#F0FDF4` (Sold status, OTP verified)
- **Warning Amber:** `#D97706` / Light: `#FFFBEB` (Pending deals, like new condition)
- **Destructive Red:** `#DC2626` / Light: `#FEF2F2` (Suspended accounts, error banners, Gmail badge)

### 1.2 Glassmorphism Implementation (`GlassComponents.kt`)
- **Modifier Extension:** `Modifier.liquidGlass(...)`
  - Shape: Configurable (default `RoundedCornerShape(20.dp)`)
  - Background: `Color.White.copy(alpha = 0.85f)` to `0.92f`
  - Border Brush: Linear gradient from `Color.White.copy(alpha = 0.70f)` to `Color.White.copy(alpha = 0.20f)`
  - Elevation: 4.dp to 8.dp
  - Top Sheen: Specular linear gradient highlight along top rim
  - Inner Highlight: White sheen `Color.White.copy(alpha = 0.60f)`

---

## 2. Typography Hierarchy (`Type.kt`)

Using Roboto / System Sans with proportional typography:
- **Display Large (28sp, Bold, -0.5sp letter spacing):** Auth greeting, hero headers.
- **Title Large (20sp, Bold):** Screen titles, dialog titles, book set titles.
- **Title Medium (16sp, SemiBold):** Section headers, seller names, price tags.
- **Body Large (14sp, Regular):** Book descriptions, chat message body.
- **Body Medium (13sp, Medium):** Filter chips, tab labels, department chips.
- **Caption / Label (11sp - 12sp, Medium/SemiBold):** Timestamps, badges, unread counters, condition tags.
- **Developer Credit (13sp, SemiBold, 0.4sp letter spacing):** `develop by Mh Nirob`.

---

## 3. Screen-by-Screen Layout Specifications

### 3.1 Authentication Screen (`AuthScreen.kt`)
- **Backdrop:** `LiquidGlassBackdrop` with subtle multi-color blurred gradient spheres.
- **Top Brand Emblem:** 76.dp circular frosted badge with book icon (`Icons.Outlined.AutoStories`) and blue glow.
- **Card Container:** Liquid glass card with 24.dp corner radius.
- **Tab Switcher:** Pill switcher with sliding animated thumb (`animateDpAsState` spring physics) between "Sign In" and "Sign Up".
- **Sign In Tab:**
  - Email field (Outlined rounded 14.dp)
  - Password field with show/hide eye toggle
  - "Sign In" filled glass button
  - Divider with "OR"
  - "Sign in with Google" surface card with Google "G" logo
- **Sign Up Tab:**
  - Form fields: Name, Email, Password, Phone, Polytechnic Dropdown, Technology Dropdown, Semester Dropdown
  - "Create Account" primary button
- **Gmail OTP Verification View:**
  - Gmail circular emblem (64.dp, soft red `#FEE2E2`, border `#FCA5A5`, icon `Icons.Outlined.MarkEmailRead`)
  - Title: "Gmail OTP Verification"
  - Email pill chip with edit icon to return to form
  - Simulated incoming Gmail notification box with 1-tap "Auto-fill" chip button
  - 6 individual PIN boxes (44.dp x 52.dp, rounded 12.dp) with glowing blue border on active box
  - Invisible `BasicTextField` capturing keyboard input
  - 60-second live countdown timer and "Resend OTP" button
  - "Verify OTP & Log In" primary button
  - Bengali error and success banners

### 3.2 Floating Bottom Navigation Bar (`FloatingBottomNavBar.kt`)
- **Docking:** Floats 16.dp above screen bottom with horizontal padding 20.dp.
- **Height:** 66.dp
- **Shape:** Rounded pill `RoundedCornerShape(32.dp)`
- **Glass Effect:** Elevated liquid glass with specular sheen and frosted blur.
- **Tabs (5 items):**
  1. Home: `Icons.Outlined.Home` (Selected: Filled `Home`)
  2. Search: `Icons.Outlined.Search`
  3. Sell: Center elevated FAB pill with `Icons.Outlined.AddCircle` or plus emblem
  4. Inbox: `Icons.Outlined.ChatBubbleOutline` with reactive red unread counter badge
  5. Profile: `Icons.Outlined.Person`
- **Active State:** Animated pill indicator behind active icon with smooth spring slide.

### 3.3 Home Screen (`HomeScreen.kt`)
- **Header Row:** App logo badge, Polytechnic title, search icon button, notification bell with red badge.
- **Hero Card:** Curved banner with diploma graduation iconography, greeting, campus tag, and statistics.
- **Technology Filter Bar:** Horizontal scrollable row of rounded pills with icon and short name (Civil, Computer, Electrical, etc.).
- **Book Set Feed Grid:** Staggered or 2-column cards featuring:
  - 16:10 aspect ratio book cover photo with condition badge overlaid (e.g. "Like New", "Good")
  - Book set title (max 2 lines)
  - Technology and Semester badge
  - Price in BDT (`৳550`)
  - Seller avatar and campus abbreviation
  - Bookmark favorite heart toggle

### 3.4 Set Details Screen (`SetDetailsScreen.kt`)
- **Top Bar:** Back button, share button, bookmark button.
- **Hero Image Carousel / Image Viewer:** Full width with rounded bottom corners.
- **Price & Title Banner:** Large price tag in BDT with "Negotiable" or "Fixed" tag.
- **Included Books List:** Itemized breakdown cards showing individual textbooks (Book Name, Subject Code, Edition, Condition).
- **Seller Profile Card:** Seller student photo, name, college, technology, semester, member since.
- **Bottom Fixed Action Bar:** Two buttons:
  - "Chat with Seller" (Frosted Glass Button)
  - "Make Mega Deal" (Primary Blue Glass Button)

### 3.5 Chat Screen (`ChatScreen.kt`)
- **Header:** Back button, seller photo, student name, online green status indicator, current book thumbnail.
- **Deal Proposition Banner:** If active deal exists, sticky card displaying agreed price, meetup place, and status badge.
- **Message List:** Chronological chat bubbles. Sent messages (blue right aligned), received messages (white left aligned).
- **Input Bar:** Attachment icon, text field with placeholder "Type message...", Send button.

### 3.6 Profile Screen (`ProfileScreen.kt`)
- **Student ID Card:** Frosted card showing user photo, full name, college, technology, semester, phone, email.
- **Listing Tabs:** "My Listings" vs "Saved Book Sets".
- **Admin Panel Entry:** Rendered only for `ADMIN` role (`mdmaheraz65@gmail.com`).
- **Logout Button:** Full width button with red icon.
- **Developer Credit:** Center aligned below logout: **`develop by Mh Nirob`** (13sp, `#94A3B8`).
