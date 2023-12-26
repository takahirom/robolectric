package org.robolectric.integration.roborazzi

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build.VERSION_CODES.S
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Integration Test for Roborazzi
 *
 * This test is not intended to obstruct the release of Robolectric. In the event that issues are
 * detected which do not stem from Robolectric, the test can be temporarily disabled, and an issue
 * can be reported on the Roborazzi repository.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [S])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalRoborazziApi::class)
class RoborazziCaptureTest {
  @get:Rule
  val roborazziRule = RoborazziRule(
    options = RoborazziRule.Options(
      outputDirectoryPath = "src/screenshots",
    )
  )

  @Test
  // For reducing repository size, we use small size
  @Config(qualifiers = "w50dp-h40dp")
  fun checkViewWithElevationRendering() {
    hardwareRendererEnvironment {
      registerActivityToPackageManager(RoborazziViewWithElevationTestActivity::class.java.name)
      ActivityScenario.launch(RoborazziViewWithElevationTestActivity::class.java)

      captureScreenWithRoborazzi()
    }
  }

  @Test
  // For reducing repository size, we use small size
  @Config(qualifiers = "w110dp-h120dp")
  fun checkDialogRendering() {
    hardwareRendererEnvironment {
      registerActivityToPackageManager(RoborazziDialogTestActivity::class.java.name)
      ActivityScenario.launch(RoborazziDialogTestActivity::class.java)

      captureScreenWithRoborazzi()
    }
  }

  private fun captureScreenWithRoborazzi() {
    try {
      captureScreenRoboImage(
        roborazziOptions = RoborazziOptions(
          recordOptions = RoborazziOptions.RecordOptions(
            resizeScale = 0.5,
          )
        )
      )
    } catch (e: AssertionError) {
      throw AssertionError(
        "Image changes are detected by Roborazzi. Please run `./gradlew :integration_tests:roborazzi:testDebugUnitTest` " +
          "and commit images in integration_tests/roborazzi/src/screenshots", e
      )
    }
  }

  companion object {
    const val USE_HARDWARE_RENDERER_NATIVE_ENV = "robolectric.screenshot.hwrdr.native"
  }
}

private fun registerActivityToPackageManager(activity: String) {
  val appContext: Application =
    InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
  Shadows.shadowOf(appContext.packageManager)
    .addActivityIfNotPresent(
      ComponentName(
        appContext.packageName,
        activity,
      )
    )
}

private fun hardwareRendererEnvironment(block: () -> Unit) {
  val originalHwrdrOption =
    System.getProperty(RoborazziCaptureTest.USE_HARDWARE_RENDERER_NATIVE_ENV, null)
  // This cause ClassNotFoundException: java.nio.NioUtils
  // System.setProperty(USE_HARDWARE_RENDERER_NATIVE_ENV, "true")
  try {
    block()
  } finally {
    if (originalHwrdrOption == null) {
      System.clearProperty(RoborazziCaptureTest.USE_HARDWARE_RENDERER_NATIVE_ENV)
    } else {
      System.setProperty(RoborazziCaptureTest.USE_HARDWARE_RENDERER_NATIVE_ENV, originalHwrdrOption)
    }
  }
}

private class RoborazziViewWithElevationTestActivity : Activity() {

  var view: FrameLayout? = null
  val expectedViewBackgroundColor: Int = Color.MAGENTA
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(android.R.style.Theme_Light_NoTitleBar)
    super.onCreate(savedInstanceState)
    setContentView(
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        fun Int.toDp(): Int = (this * resources.displayMetrics.density).toInt()

        // View with elevation
        addView(
          FrameLayout(this@RoborazziViewWithElevationTestActivity)
            .apply {
              background = ColorDrawable(expectedViewBackgroundColor)
              elevation = 10f
              addView(TextView(this.context).apply { text = "Txt" })
            }
            .also { view = it },
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
          )
            .apply { setMargins(10.toDp(), 10.toDp(), 10.toDp(), 10.toDp()) }
        )
      }
    )
  }
}

private class RoborazziDialogTestActivity : Activity() {
  var dialogContentView: View? = null
  val expectedDialogContentBackgroundColor: Int = Color.MAGENTA
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(android.R.style.Theme_DeviceDefault_Light_NoActionBar)
    super.onCreate(savedInstanceState)
    setContentView(
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        fun Int.toDp(): Int = (this * resources.displayMetrics.density).toInt()

        // View with elevation
        addView(
          TextView(this.context).apply { text = "Under the dialog" },
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
          )
            .apply { setMargins(10.toDp(), 10.toDp(), 10.toDp(), 10.toDp()) }
        )
      }
    )
    AlertDialog.Builder(this)
      .setTitle("Dlg")
      .setPositiveButton("OK") { _, _ -> }
      .show()
  }
}
