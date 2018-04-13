/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.os.Process.THREAD_PRIORITY_LOWEST;

import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;
import android.text.Layout;
import com.facebook.fbui.textlayoutbuilder.util.LayoutMeasureUtil;
import java.lang.ref.WeakReference;

/**
 * A class that schedules a background draw of a {@link Layout}. Drawing a {@link Layout} in the
 * background ensures that the glyph caches are warmed up and ready for drawing the same
 * {@link Layout} on a real {@link Canvas}. This will substantially reduce drawing times for big
 * chunks of text. On the other hand over-using text warming might rotate the glyphs cache too
 * quickly and diminish the optimization.
 */
public class GlyphWarmer {

  private static final String TAG = GlyphWarmer.class.getName();

  private static final int WARMER_THREAD_PRIORITY =
      (THREAD_PRIORITY_BACKGROUND + THREAD_PRIORITY_LOWEST) / 2;

  private static GlyphWarmer sInstance;
  private final WarmerHandler mHandler;

  /**
   * @return the global {@link GlyphWarmer} instance.
   */
  public static synchronized GlyphWarmer getInstance() {
    if (sInstance == null) {
      sInstance = new GlyphWarmer();
    }

    return sInstance;
  }

  private GlyphWarmer() {

    HandlerThread handlerThread = new HandlerThread(TAG, WARMER_THREAD_PRIORITY);
    handlerThread.start();

    mHandler = new WarmerHandler(handlerThread.getLooper());
  }

  @VisibleForTesting
  Looper getWarmerLooper() {
    return mHandler.getLooper();
  }

  /**
   * Schedules a {@link Layout} to be drawn in the background. This warms up the Glyph cache for
   * that {@link Layout}.
   */
  public void warmLayout(Layout layout) {
    mHandler.obtainMessage(WarmerHandler.WARM_LAYOUT, new WeakReference<>(layout)).sendToTarget();
  }

  private static final class WarmerHandler extends Handler {
    public static final int WARM_LAYOUT = 0;

    private final Picture mPicture;

    private WarmerHandler(Looper looper) {
      super(looper);

      Picture picture;
      try {
        picture = new Picture();
      } catch (RuntimeException e) {
        picture = null;
      }

      mPicture = picture;
    }

    @Override
    public void handleMessage(Message msg) {
      if (mPicture == null) {
        return;
      }

      try {
        final Layout layout = ((WeakReference<Layout>) msg.obj).get();

        if (layout == null) {
          return;
        }

        final Canvas canvas = mPicture.beginRecording(
            layout.getWidth(),
            LayoutMeasureUtil.getHeight(layout));

        layout.draw(canvas);
        mPicture.endRecording();
      } catch (Exception e) {
        // Nothing to do here. This is a best effort. No real problem if it fails.
      }
    }
  }
}
