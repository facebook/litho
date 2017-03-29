/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import java.lang.ref.WeakReference;

import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;
import android.text.Layout;

import com.facebook.fbui.textlayoutbuilder.util.LayoutMeasureUtil;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.os.Process.THREAD_PRIORITY_LOWEST;

/**
 * A class that schedules a background draw of a {@link Layout}. Drawing a {@link Layout} in the
 * background ensures that the glyph caches are warmed up and ready for drawing the same
 * {@link Layout} on a real {@link Canvas}. This will substantially reduce drawing times for big
 * chunks of text. On the other hand over-using text warming might rotate the glyphs cache too
 * quickly and diminish the optimization.
 */
public class GlyphWarmer {

  private static final String TAG = GlyphWarmer.class.getName();
