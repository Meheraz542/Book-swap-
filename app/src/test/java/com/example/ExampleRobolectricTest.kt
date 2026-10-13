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
    assertEquals("BookSwipe", appName)
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
}

