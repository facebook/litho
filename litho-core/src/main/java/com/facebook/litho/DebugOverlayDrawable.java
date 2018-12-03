/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import com.facebook.litho.drawable.ComparableDrawable;
import java.util.List;

public class DebugOverlayDrawable extends ComparableDrawable {

  private static final int COLOR_RED_TRANSPARENT = Color.parseColor("#22FF0000");
  private static final int COLOR_GREEN_TRANSPARENT = Color.parseColor("#2200FF00");
  private static final int COLOR_RED_OPAQUE = Color.parseColor("#CCFF0000");
  private static final int COLOR_GREEN_OPAQUE = Color.parseColor("#CC00FF00");
  private static final float TEXT_SIZE = 80f;
  private static final int BOX_HEIGHT = 100;
  private static final int TEXT_THRESHOLD = 3;

  private final Paint textPaint = new Paint();
  private final Paint colorPaint = new Paint();

  private final List<Boolean> mainThreadCalculations;

  // Drawing params. Package-private for testing purpose only.
  final String text;
  final int overlayColor;

  /**
   * @param mainThreadCalculations will be used to draw on the Canvas. It should be guaranteed by
   *     the caller to not modify this list.
   */
  public DebugOverlayDrawable(List<Boolean> mainThreadCalculations) {
    textPaint.setColor(Color.BLACK);
    textPaint.setAntiAlias(true);
    textPaint.setStyle(Paint.Style.FILL);
    textPaint.setTextSize(TEXT_SIZE);
    textPaint.setTextAlign(Paint.Align.LEFT);

    this.mainThreadCalculations = mainThreadCalculations;
    int size = mainThreadCalculations.size();
    if (size > 0) {
      text = size + "x";
      overlayColor =
          mainThreadCalculations.get(size - 1) ? COLOR_RED_TRANSPARENT : COLOR_GREEN_TRANSPARENT;
    } else {
      text = "";
      overlayColor = Color.TRANSPARENT;
    }
  }

  @Override
  public void draw(Canvas canvas) {
    colorPaint.setColor(overlayColor);
    canvas.drawRect(getBounds(), colorPaint);

    int count = mainThreadCalculations.size();
    for (int i = 0, width = canvas.getWidth(), l, r; i < count; i++) {
      l = i * 20;
      r = l + 16;
      if (r < width) {
        if (mainThreadCalculations.get(i)) {
          colorPaint.setColor(COLOR_RED_OPAQUE);
        } else {
          colorPaint.setColor(COLOR_GREEN_OPAQUE);
        }
        canvas.drawRect(l, 0, r, BOX_HEIGHT, colorPaint);
      } else {
        break;
      }
    }

    if (count > TEXT_THRESHOLD) {
      canvas.drawText(text, 0, TEXT_SIZE, textPaint);
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
    return mainThreadCalculations.hashCode();
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
    // mainThreadCalculations is the only important field here but we want to stop earlier
    return overlayColor == other.overlayColor
        && text.equals(other.text)
        && mainThreadCalculations.equals(other.mainThreadCalculations);
  }

  @Override
  public boolean isEquivalentTo(ComparableDrawable other) {
    return this.equals(other);
  }
}
