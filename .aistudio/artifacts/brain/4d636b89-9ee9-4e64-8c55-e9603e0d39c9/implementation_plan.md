# Implementation Plan: Universal Real-Time Book Photo Visibility & Robust Caching

Ensure that real-time book listings and photos uploaded by any student or seller are immediately, consistently, and dynamically visible to every user across all devices without render failures, "Photo temporarily unavailable" errors, or account-type limitations.

## User Review & Critical Decisions

> [!IMPORTANT]
> - **Codebase Framework Alignment**: The application is an Android Kotlin codebase built with Jetpack Compose, Material 3, Room, and Coil (with OkHttp caching engine). We are enhancing this exact pipeline to provide cross-user real-time visibility, persistent image caching, and pinch-to-zoom full-screen preview.
> - **Firebase Storage Access Rules**: In accordance with your confirmed preference, `storage.rules` is configured with **Public read for all book photos** (`allow read: if true;`) and authenticated uploads (`allow write: if request.auth != null;`). This ensures User B, User C, and all devices can download and render User A's uploaded book photos without cross-user permission denials.
> - **Direct Download URLs & Synchronous Upload Flow**: In accordance with your choice, `Firebase Storage direct download URLs only` will be stored in Firestore, with synchronous upload in `SellScreen` before Firestore publication so no local `file://` URIs are published to other users.

---

## 1. Overview & Core Concept

- **The Problem**: When User A uploaded a book set, the app used an asynchronous background task that initially published local `file:///...` URIs or un-synced URLs to Firestore. When User B or User C loaded the listing, `file:///` URLs were invalid on their physical devices, causing Coil to trigger the fallback "Photo temporarily unavailable" error.
- **The Solution**:
  1. Synchronous Firebase Storage upload during publishing: Upload all cover and included book images to Firebase Storage first, obtain public `https://firebasestorage.googleapis.com/...` download URLs, and save these verified URLs directly to Firestore.
  2. Public read access in `storage.rules` so every user can access uploaded images.
  3. Coil image caching with `OkHttpClient` connection retries, disk cache (150 MB), and memory cache (30%).
  4. Interactive full-screen photo dialog with pinch-to-zoom, pan gestures, and instant retry.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Seller Publishes Listing (`SellScreen`)**:
   - The user selects a cover photo and included book photos.
   - Upon tapping **"Publish Book Set"**, a progress dialog with clear status ("Uploading photo 1 of N to Firebase Storage...") displays while photos upload.
   - Once uploaded, verified remote HTTPS URLs are published to Firestore (`/bookSets/{id}`).
   - The user is notified and routed to their new active listing.

2. **Buyer / Other Users Browse (`HomeScreen` & `SearchScreen`)**:
   - Firestore's real-time snapshot listener (`listenAllBookSets`) receives the listing with remote HTTPS URLs.
   - Coil fetches the image from Firebase Storage and caches it to disk (`marketplace_image_cache`).
   - Cards display clean loading shimmer/progress, followed by the original high-resolution book photo.

3. **Full-Screen Photo Experience (`SetDetailsScreen`)**:
   - Tapping the hero cover image or any book thumbnail opens the full-screen photo dialog.
   - Supports pinch-to-zoom (up to 3x) and drag-to-pan, photo counter ("Photo 1 of 4"), previous/next arrows, and an interactive **Retry** button if network drops.

---

## 3. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   SELLER DEVICE                        │
│ 1. User picks book photos (Photo Picker)              │
│ 2. Compress/Stream original bytes to Firebase Storage  │
│ 3. Obtain Download URL: https://firebasestorage...     │
│ 4. Save BookSet document with remote HTTPS URLs        │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│            FIREBASE CLOUD BACKEND                      │
│ - Storage: /book_images/{setId}_{index}_{time}.jpg     │
│   Rules: allow read: if true; allow write: if auth     │
│ - Firestore: /bookSets/{setId}                         │
│   Field: imageUrls = ["https://firebasestorage..."]    │
└──────────────────────────┬─────────────────────────────┘
                           │ Real-Time Snapshot Listener
                           ▼
┌────────────────────────────────────────────────────────┐
│             BUYER / OTHER DEVICES (User B, C)          │
│ 1. Firestore listener emits updated BookSet            │
│ 2. Coil ImageLoader (30s timeout, auto-retry, cache)  │
│ 3. DiskCache (150MB) + MemoryCache (30%)               │
│ 4. Jetpack Compose AsyncImage renders cover & books    │
│ 5. Full-Screen Dialog with Pinch-to-Zoom & Pan         │
└────────────────────────────────────────────────────────┘
```

---

## 4. Implementation Steps

1. **Storage Rules (`storage.rules`)**:
   - Set public read on `/book_images/**` and authenticated write permissions.
   - Deploy/configure via Firebase project configuration.

2. **Publish Pipeline (`SellScreen.kt` & `MarketplaceRepository.kt`)**:
   - Update `SellScreen` to display an upload progress modal during publication.
   - Await `FirebaseStorageManager.uploadOriginalBookImage` for all photos so only valid remote `https://` URLs are written to Firestore.

3. **Coil Image Loader Engine (`MarketplaceApplication.kt`)**:
   - Ensure `Coil.setImageLoader` registers the custom `ImageLoader` as the global singleton on app launch.
   - Configure cache headers, redirect policies, and network timeout resilience.

4. **Full-Screen Interactive Zoom (`SetDetailsScreen.kt`)**:
   - Implement `Modifier.pointerInput` with `detectTransformGestures` for smooth pinch-to-zoom and pan.
   - Retain carousel thumbnails, photo counters, and reload error recovery.

5. **Verification**:
   - Verify compilation with `compile_applet`.
   - Run unit tests to validate image URL parsing and model persistence.
