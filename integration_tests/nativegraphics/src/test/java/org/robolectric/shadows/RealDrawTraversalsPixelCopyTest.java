package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.CINNAMON_BUN;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.FrameLayout;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.junit.runners.MethodSorters;

/**
 * Regression test for stale PixelCopy frames when real draw traversals are enabled (the default on
 * SDK >= 37 / CINNAMON_BUN).
 *
 * <p>Each test launches an Activity whose initial frame is RED, then mutates the background to
 * BLUE after the initial traversal, idles the main looper, and captures via PixelCopy. The first
 * test in the JVM process sees BLUE, but every subsequent test sees the stale initial (RED) frame:
 * draw passes after the first traversal never reach the window surface once a previous test has
 * run in the same process.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = CINNAMON_BUN)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RealDrawTraversalsPixelCopyTest {

  @Test
  public void pixelCopy_afterContentChange_firstTestInProcess() throws Exception {
    launchMutateAndAssertBlue();
  }

  @Test
  public void pixelCopy_afterContentChange_secondTestInProcess() throws Exception {
    launchMutateAndAssertBlue();
  }

  private static void launchMutateAndAssertBlue() throws Exception {
    RedActivity activity = Robolectric.setupActivity(RedActivity.class);

    // Mutate the UI after the initial traversal, then propagate the change.
    activity.frameLayout.setBackgroundColor(Color.BLUE);
    shadowOf(Looper.getMainLooper()).idle();

    Window window = activity.getWindow();
    Bitmap bitmap =
        Bitmap.createBitmap(
            window.getDecorView().getWidth(),
            window.getDecorView().getHeight(),
            Bitmap.Config.ARGB_8888);
    CountDownLatch latch = new CountDownLatch(1);
    PixelCopy.request(
        window, bitmap, copyResult -> latch.countDown(), new Handler(Looper.getMainLooper()));
    shadowOf(Looper.getMainLooper()).idle();
    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();

    assertThat(Integer.toHexString(bitmap.getPixel(100, 100)))
        .isEqualTo(Integer.toHexString(Color.BLUE));
  }

  static class RedActivity extends Activity {
    FrameLayout frameLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      frameLayout = new FrameLayout(this);
      frameLayout.setLayoutParams(
          new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
      frameLayout.setBackgroundColor(Color.RED);
      setContentView(frameLayout);
    }
  }

  /**
   * Same scenario on SDK 36 with real draw traversals explicitly opted in, to check whether the
   * staleness is specific to SDK 37 or to the real-draw-traversals code path itself.
   */
  @RunWith(RobolectricTestRunner.class)
  @Config(sdk = android.os.Build.VERSION_CODES.BAKLAVA)
  @FixMethodOrder(MethodSorters.NAME_ASCENDING)
  public static class OnSdk36WithOptInTest {
    @org.junit.Rule
    public org.robolectric.junit.rules.SetSystemPropertyRule setSystemPropertyRule =
        new org.robolectric.junit.rules.SetSystemPropertyRule();

    @Test
    public void pixelCopy_afterContentChange_firstTestInProcess() throws Exception {
      setSystemPropertyRule.set("robolectric.useRealDrawTraversals", "true");
      assertThat(ShadowView.areRealDrawTraversalsEnabled()).isTrue();
      launchMutateAndAssertBlue();
    }

    @Test
    public void pixelCopy_afterContentChange_secondTestInProcess() throws Exception {
      setSystemPropertyRule.set("robolectric.useRealDrawTraversals", "true");
      assertThat(ShadowView.areRealDrawTraversalsEnabled()).isTrue();
      launchMutateAndAssertBlue();
    }
  }
}
