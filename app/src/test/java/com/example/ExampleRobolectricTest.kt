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
  fun `verify repository initial data`() {
    val user = com.example.data.MarketplaceRepository.currentUser.value
    assertEquals("Super Admin", user.name)
    assertEquals("mdmaheraz65@gmail.com", user.email)
    assertEquals("ADMIN", user.role)
    val sets = com.example.data.MarketplaceRepository.bookSets.value
    org.junit.Assert.assertNotNull(sets)
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
  fun `reject incorrect password for admin user`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authManager = com.example.data.auth.AuthManager(context)

    val wrongAdminResult = authManager.signIn("mdmaheraz65@gmail.com", "wrongpassword123")
    org.junit.Assert.assertTrue(wrongAdminResult is com.example.data.auth.AuthResult.Error)
    val errorMsg = (wrongAdminResult as com.example.data.auth.AuthResult.Error).message
    org.junit.Assert.assertTrue(errorMsg.contains("Incorrect password", ignoreCase = true))
  }
}

