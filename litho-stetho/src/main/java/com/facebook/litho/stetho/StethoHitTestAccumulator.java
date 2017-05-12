/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.stetho;

import android.graphics.Rect;
import android.view.View;

import com.facebook.litho.DebugComponent;
import com.facebook.stetho.common.Accumulator;

class StethoHitTestAccumulator implements Accumulator<Object> {
  private final Rect mHitTestRect = new Rect();

  private final Rect mOutBounds;
  private final int mX;
  private final int mY;
  private Object mElementToHighlight;

  StethoHitTestAccumulator(Rect outBounds, int x, int y) {
    mOutBounds = outBounds;
    mX = x;
    mY = y;
  }

  @Override
  public void store(Object obj) {
    if (obj instanceof DebugComponent) {
      final DebugComponent debugComponent = (DebugComponent) obj;
      mHitTestRect.set(debugComponent.getBounds());
      if (mHitTestRect.contains(mX, mY)) {
        mOutBounds.set(mHitTestRect);
        mElementToHighlight = debugComponent;
      }
    } else if (obj instanceof View) {
      final View view = (View) obj;
      view.getHitRect(mHitTestRect);
      if (mHitTestRect.contains(mX, mY)) {
        mOutBounds.set(mHitTestRect);
        mElementToHighlight = view;
      }
    }
  }

  public Object getElement() {
    return mElementToHighlight;
  }
}
