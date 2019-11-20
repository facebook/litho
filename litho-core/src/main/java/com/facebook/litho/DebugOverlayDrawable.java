/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.VisibleForTesting;
import com.facebook.litho.drawable.ComparableDrawable;
import java.util.List;

/**
 * Based on the content of the {@link #mainThreadCalculations} draws a row of columns starting from
 * the top left corner with text representing the number of columns. Add overlay color within
 * drawing bounds.
 */
public class DebugOverlayDrawable extends Drawable implements ComparableDrawable {

  @VisibleForTesting static final int COLOR_RED_SEMITRANSPARENT = Color.parseColor("#22FF0000");
  @VisibleForTesting static final int BOX_WIDTH_PX = 16;
  private static final int COLOR_GREEN_SEMITRANSPARENT = Color.parseColor("#2200FF00");
  private static final int COLOR_RED_OPAQUE = Color.parseColor("#CCFF0000");
  private static final int COLOR_GREEN_OPAQUE = Color.parseColor("#CC00FF00");
  private static final float TEXT_SIZE_PX = 80f;
  private static final int BOX_HEIGHT_PX = 100;
  private static final int SHOW_TEXT_THRESHOLD_CNT = 3;

  private final Paint textPaint = new Paint();
  private final Paint colorPaint = new Paint();

  private final List<Boolean> isLayoutCalculatedOnMainThread;

  // Drawing params.
  @VisibleForTesting final String text;
  @VisibleForTesting final int overlayColor;

  /**
   * @param isLayoutCalculatedOnMainThread will be used to draw on the Canvas. It should be
   *     guaranteed by the caller to not modify this list.
   */
  public DebugOverlayDrawable(List<Boolean> isLayoutCalculatedOnMainThread) {
    textPaint.setColor(Color.BLACK);
    textPaint.setAntiAlias(true);
    textPaint.setStyle(Paint.Style.FILL);
    textPaint.setTextSize(TEXT_SIZE_PX);
    textPaint.setTextAlign(Paint.Align.LEFT);

    this.isLayoutCalculatedOnMainThread = isLayoutCalculatedOnMainThread;
    int size = isLayoutCalculatedOnMainThread.size();
    if (size > 0) {
      text = size + "x";
      overlayColor =
          isLayoutCalculatedOnMainThread.get(size - 1)
              ? COLOR_RED_SEMITRANSPARENT
              : COLOR_GREEN_SEMITRANSPARENT;
    } else {
      text = "";
      overlayColor = Color.TRANSPARENT;
    }
  }

  @Override
  public void draw(Canvas canvas) {
    colorPaint.setColor(overlayColor);
    final Rect bounds = getBounds();
    canvas.drawRect(bounds, colorPaint);

    final int count = isLayoutCalculatedOnMainThread.size();
    final int leftEnd = bounds.left, rightEnd = bounds.right;
    final int top = bounds.top, bottom = Math.min(top + BOX_HEIGHT_PX, bounds.bottom);
    for (int i = 0, left, right; i < count; i++) {
      left = leftEnd + i * 20;
      right = left + BOX_WIDTH_PX;
      if (right < rightEnd) {
        if (isLayoutCalculatedOnMainThread.get(i)) {
          colorPaint.setColor(COLOR_RED_OPAQUE);
        } else {
          colorPaint.setColor(COLOR_GREEN_OPAQUE);
        }
        canvas.drawRect(left, top, right, bottom, colorPaint);
      } else {
        break;
      }
    }

    if (count > SHOW_TEXT_THRESHOLD_CNT) {
      canvas.drawText(text, leftEnd, top + TEXT_SIZE_PX, textPaint);
    }
  }

  @Override
  public void setAlpha(int alpha) {}

  @Override
  public void setColorFilter(ColorFilter cf) {}

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public int hashCode() {
    return isLayoutCalculatedOnMainThread.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    DebugOverlayDrawable other = (DebugOverlayDrawable) obj;
    // isLayoutCalculatedOnMainThread is the only important field here but we want to stop earlier
    return overlayColor == other.overlayColor
        && text.equals(other.text)
        && isLayoutCalculatedOnMainThread.equals(other.isLayoutCalculatedOnMainThread);
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    return this.equals(other);
  }
}
