/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.litho.config.ComponentsConfiguration;

import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.Component.isMountViewSpec;

/**
 * Draw operations used in developer options.
 */
class DebugDraw {

  private static final int INTERACTIVE_VIEW_COLOR = 0x66C29BFF;
  private static final int TOUCH_DELEGATE_COLOR = 0x44D3FFCE;

  private static final int MOUNT_BORDER_COLOR = 0x99FF0000;
  private static final int MOUNT_BORDER_COLOR_HOST = 0x99FF00FF;
  private static final int MOUNT_CORNER_COLOR = 0xFF0000FF;
  private static final int MOUNT_CORNER_COLOR_HOST = 0xFF00FFFF;

  private static Paint sInteractiveViewPaint;
  private static Paint sTouchDelegatePaint;

  private static Rect sMountBoundsRect;
  private static Paint sMountBoundsBorderPaint;
  private static Paint sMountBoundsCornerPaint;

  static void draw(ComponentHost host, Canvas canvas) {
    if (ComponentsConfiguration.debugHighlightInteractiveBounds) {
      highlightInteractiveBounds(host, canvas);
    }

    if (ComponentsConfiguration.debugHighlightMountBounds) {
