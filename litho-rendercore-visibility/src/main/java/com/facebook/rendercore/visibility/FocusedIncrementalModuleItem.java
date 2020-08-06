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

package com.facebook.rendercore.visibility;

import android.graphics.Rect;
import android.view.View;

public final class FocusedIncrementalModuleItem implements IncrementalModuleItem {

  private final VisibilityOutput mVisibilityOutput;

  FocusedIncrementalModuleItem(VisibilityOutput visibilityOutput) {
    mVisibilityOutput = visibilityOutput;
  }

  @Override
  public String getId() {
    return "f_" + mVisibilityOutput.getId();
  }

  @Override
  public Rect getBounds() {
    return mVisibilityOutput.getBounds();
  }

  @Override
  public float getEnterRangeTop() {
    return mVisibilityOutput.getFocusedTop();
  }

  @Override
  public float getEnterRangeBottom() {
    return mVisibilityOutput.getFocusedBottom();
  }

  @Override
  public void onEnterVisibleRange() {
    VisibilityModuleInput.processFocused(mVisibilityOutput);
  }

  @Override
  public void onExitVisibleRange() {
    VisibilityModuleInput.processUnfocused(mVisibilityOutput);
  }

  public void onLithoViewAvailable(View view) {
    final View parent = (View) view.getParent();
    if (parent == null) {
      return;
    }

    final int halfViewportArea = parent.getWidth() * parent.getHeight() / 2;

    if (mVisibilityOutput.getComponentArea() >= halfViewportArea) {
      float ratio = 0.5f * halfViewportArea / halfViewportArea;
      mVisibilityOutput.setFocusedRatio(ratio);
    } else {
      mVisibilityOutput.setFocusedRatio(1.0f);
    }
  }
}
