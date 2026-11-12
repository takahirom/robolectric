package org.robolectric.shadows;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import kotlinx.coroutines.debug.DebugProbes;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * Repro: after a density-scaled bitmap decode in NATIVE graphics mode, the JVM no longer accepts
 * Attach API connections, so DebugProbes.install() fails with "Target VM did not respond".
 *
 * <p>scaled_icon.png is a 72x72 solid-colour PNG that exists only in res/drawable-xxhdpi, so
 * drawing it under the "hdpi" qualifier decodes it with density scaling. With qualifiers =
 * "xxhdpi" (no scaling), or with -XX:+StartAttachListener, the test passes.
 */
@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "hdpi", sdk = 34)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DebugProbesAfterScaledImageTest {
  @Test
  public void a_drawImage() {
    Context context = ApplicationProvider.getApplicationContext();
    Drawable drawable = context.getDrawable(R.drawable.scaled_icon);
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    drawable.setBounds(0, 0, 100, 100);
    drawable.draw(new Canvas(bitmap));
  }

  @Test
  public void b_installDebugProbes() {
    DebugProbes.INSTANCE.install(); // fails: "Target VM did not respond"
    DebugProbes.INSTANCE.uninstall();
  }
}
