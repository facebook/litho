/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;
import android.view.View;

import com.facebook.stetho.common.Accumulator;

class HitTestAccumulator implements Accumulator<Object> {
  private final Rect mHitTestRect = new Rect();

  private final Rect mOutBounds;
  private final int mX;
  private final int mY;
  private Object mElementToHighlight;

  HitTestAccumulator(Rect outBounds, int x, int y) {
    mOutBounds = outBounds;
    mX = x;
    mY = y;
  }

  @Override
  public void store(Object obj) {
    if (obj instanceof StethoInternalNode) {
      final StethoInternalNode stethoNode = (StethoInternalNode) obj;
      mHitTestRect.set(
          stethoNode.node.getX(),
          stethoNode.node.getY(),
          stethoNode.node.getX() + stethoNode.node.getWidth(),
          stethoNode.node.getY() + stethoNode.node.getHeight());
      if (mHitTestRect.contains(mX, mY)) {
        mOutBounds.set(mHitTestRect);
        mElementToHighlight = stethoNode;
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
