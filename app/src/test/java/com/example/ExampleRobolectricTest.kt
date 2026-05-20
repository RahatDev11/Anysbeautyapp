package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.BeautyAppContent
import com.example.ui.BeautyViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Any's Beauty Corner", appName)
  }

  @Test
  fun `test whole app composition without crashes`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BeautyViewModel(app)
    
    composeTestRule.setContent {
      MyApplicationTheme {
        BeautyAppContent(viewModel = viewModel)
      }
    }
    
    // Wait for compose to idle to verify it successfully renders the first frame
    composeTestRule.waitForIdle()
    composeTestRule.onRoot().assertExists()
  }
}
