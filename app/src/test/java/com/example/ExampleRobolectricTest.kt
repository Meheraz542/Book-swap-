package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    assertEquals("Super Admin", adminUser.name)
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
}

