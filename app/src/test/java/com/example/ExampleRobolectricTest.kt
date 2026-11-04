package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.model.primaryCoverUrl

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BookSwap", appName)
  }

  @Test
  fun `verify repository initial data and logout purge`() = kotlinx.coroutines.test.runTest {
    com.example.data.MarketplaceRepository.logoutAsync()
    // Startup check: unauthenticated on initial clean state
    org.junit.Assert.assertFalse(com.example.data.MarketplaceRepository.isLoggedIn.value)
    val sets = com.example.data.MarketplaceRepository.bookSets.value
    org.junit.Assert.assertNotNull(sets)

    // User logs in with admin credentials
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.data.auth.AuthManager(context)
    val res = authManager.signIn("mdmaheraz65@gmail.com", "meheraz12@#$")
    org.junit.Assert.assertTrue(res is com.example.data.auth.AuthResult.Success)
    val adminUser = (res as com.example.data.auth.AuthResult.Success).user
    org.junit.Assert.assertTrue(adminUser.name == "Super Admin" || adminUser.name == "MH NIROB")
    assertEquals("mdmaheraz65@gmail.com", adminUser.email)
    assertEquals("ADMIN", adminUser.role)

    // Logging out purges session
    com.example.data.MarketplaceRepository.logoutAsync()
    org.junit.Assert.assertFalse(com.example.data.MarketplaceRepository.isLoggedIn.value)
    org.junit.Assert.assertEquals("", com.example.data.MarketplaceRepository.currentUser.value.id)
  }

  @Test
  fun `launch MainActivity without crash`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
    val activity = controller.get()
    org.junit.Assert.assertNotNull(activity)
  }

  @Test
  fun `reject blank password and short password on sign in`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.data.auth.AuthManager(context)

    val blankPassResult = authManager.signIn("test@example.com", "   ")
    org.junit.Assert.assertTrue(blankPassResult is com.example.data.auth.AuthResult.Error)

    val shortPassResult = authManager.signIn("test@example.com", "123")
    org.junit.Assert.assertTrue(shortPassResult is com.example.data.auth.AuthResult.Error)
  }

  @Test
  fun `reject unregistered email on sign in and require sign up`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.data.auth.AuthManager(context)

    // Attempting to sign in with an unregistered account must fail
    val unregResult = authManager.signIn("random_stranger@example.com", "validPass123")
    org.junit.Assert.assertTrue(unregResult is com.example.data.auth.AuthResult.Error)
    val errorMsg = (unregResult as com.example.data.auth.AuthResult.Error).message
    org.junit.Assert.assertTrue(errorMsg.contains("No registered account found", ignoreCase = true))

    // Legitimate registration
    val signUpResult = authManager.signUp(
        name = "Tanvir Ahmed",
        email = "tanvir@fpi.edu.bd",
        password = "studentSecure123",
        phone = "01812345678",
        college = "Feni Polytechnic Institute",
        technology = com.example.model.Technology.CIVIL,
        semester = com.example.model.Semester.SEM_3
    )
    org.junit.Assert.assertTrue(signUpResult is com.example.data.auth.AuthResult.Success)

    // Wrong password for registered user fails
    val wrongPass = authManager.signIn("tanvir@fpi.edu.bd", "wrongPassword123")
    org.junit.Assert.assertTrue(wrongPass is com.example.data.auth.AuthResult.Error)

    // Correct password succeeds
    val correctSignIn = authManager.signIn("tanvir@fpi.edu.bd", "studentSecure123")
    org.junit.Assert.assertTrue(correctSignIn is com.example.data.auth.AuthResult.Success)
    val user = (correctSignIn as com.example.data.auth.AuthResult.Success).user
    assertEquals("Tanvir Ahmed", user.name)
    assertEquals("tanvir@fpi.edu.bd", user.email)
    assertEquals("USER", user.role)
  }

  @Test
  fun `rbac prevents third party viewer from deleting listing`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.MarketplaceRepository.initialize(context)

    // Create a listing belonging to seller_1
    val published = com.example.data.MarketplaceRepository.publishSet(
        title = "Surveying & Leveling Practical",
        subject = "Surveying",
        technology = com.example.model.Technology.CIVIL,
        semester = com.example.model.Semester.SEM_2,
        college = "Feni Polytechnic Institute",
        totalPrice = 500,
        overallCondition = com.example.model.SetCondition.GOOD,
        description = "Full semester book set",
        includedBooks = emptyList()
    )

    // Unauthorized delete attempt by a random third-party user must be blocked
    val setsBefore = com.example.data.MarketplaceRepository.bookSets.value
    com.example.data.MarketplaceRepository.deleteSet(
        setId = published.id,
        requestingUserId = "random_intruder_999",
        requestingUserRole = "USER"
    )
    val setsAfter = com.example.data.MarketplaceRepository.bookSets.value
    org.junit.Assert.assertEquals(setsBefore.size, setsAfter.size)
    org.junit.Assert.assertTrue(setsAfter.any { it.id == published.id })

    // Admin delete succeeds
    com.example.data.MarketplaceRepository.deleteSet(
        setId = published.id,
        requestingUserId = "admin_master_1",
        requestingUserRole = "ADMIN"
    )
    val setsAfterAdmin = com.example.data.MarketplaceRepository.bookSets.value
    org.junit.Assert.assertFalse(setsAfterAdmin.any { it.id == published.id })
  }

  @Test
  fun `mark as sold immediately removes book listing to conserve storage`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.MarketplaceRepository.initialize(context)

    val published = com.example.data.MarketplaceRepository.publishSet(
        title = "Civil Engineering Drawing",
        subject = "Civil",
        technology = com.example.model.Technology.CIVIL,
        semester = com.example.model.Semester.SEM_1,
        college = "Feni Polytechnic Institute",
        totalPrice = 350,
        overallCondition = com.example.model.SetCondition.LIKE_NEW,
        description = "Drawing book set",
        includedBooks = emptyList()
    )

    org.junit.Assert.assertTrue(com.example.data.MarketplaceRepository.bookSets.value.any { it.id == published.id })

    // When marked as sold, it is immediately removed from the active listings to conserve storage
    com.example.data.MarketplaceRepository.markSetAsSold(null, published.id)
    org.junit.Assert.assertFalse(com.example.data.MarketplaceRepository.bookSets.value.any { it.id == published.id })
  }

  @Test
  fun `seller uploaded book images map correctly to buyer UI and persist across app restart`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.MarketplaceRepository.initialize(context)

    val testCoverUrl = "https://firebasestorage.googleapis.com/v0/b/app/o/book_sets%2Fset_101%2Fcover.jpg?alt=media"
    val testBook1Photo = "https://firebasestorage.googleapis.com/v0/b/app/o/book_sets%2Fset_101%2Fbook_0.jpg?alt=media"

    val book1 = com.example.model.IncludedBook(
        id = "b_1",
        setId = "set_101",
        bookName = "Data Structures",
        subject = "CSE",
        author = "Tanenbaum",
        edition = "3rd",
        condition = "Like New",
        imageUris = listOf(testBook1Photo)
    )

    // Seller publishes book set with custom uploaded cover photo and book photos
    val published = com.example.data.MarketplaceRepository.publishSet(
        title = "Complete 3rd Semester CSE Book Set",
        subject = "Computer Science",
        technology = com.example.model.Technology.CSE,
        semester = com.example.model.Semester.SEM_3,
        college = "Feni Computer Institute",
        totalPrice = 550,
        overallCondition = com.example.model.SetCondition.LIKE_NEW,
        description = "All books in mint condition with seller original photos.",
        includedBooks = listOf(book1),
        imageUrls = listOf(testCoverUrl)
    )

    // Verify buyer card mapping
    org.junit.Assert.assertEquals(testCoverUrl, published.primaryCoverUrl)
    org.junit.Assert.assertEquals(1, published.imageUrls.size)
    org.junit.Assert.assertEquals(testCoverUrl, published.imageUrls.first())
    org.junit.Assert.assertEquals(testBook1Photo, published.includedBooks.first().imageUris.first())

    // Verify persistence across restart
    val db = com.example.data.local.AppDatabase.getInstance(context)
    val setWithBooks = db.bookSetDao().getBookSetWithBooksById(published.id)
    org.junit.Assert.assertNotNull(setWithBooks)
    org.junit.Assert.assertEquals(published.id, setWithBooks?.bookSet?.id)
    org.junit.Assert.assertTrue(setWithBooks?.bookSet?.imageUrlsString?.contains("cover.jpg") == true)
    org.junit.Assert.assertEquals(1, setWithBooks?.books?.size)
    org.junit.Assert.assertEquals(testBook1Photo, setWithBooks?.books?.first()?.image1Uri)
  }
}

