/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import com.facebook.rendercore.RenderCoreExtensionHost;
import com.facebook.rendercore.transitions.AnimatedRootHost;

public abstract class BaseMountingView extends ComponentHost
    implements RenderCoreExtensionHost, AnimatedRootHost {

  protected int mAnimatedWidth = -1;
  protected int mAnimatedHeight = -1;

  public BaseMountingView(Context context) {
    super(context);
  }

  public BaseMountingView(ComponentContext context) {
    super(context);
  }

  public BaseMountingView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Sets the width that the LithoView should take on the next measure pass and then requests a
   * layout. This should be called from animation-driving code on each frame to animate the size of
   * the LithoView.
   */
  @Override
  public void setAnimatedWidth(int width) {
    mAnimatedWidth = width;
    requestLayout();
  }

  /**
   * Sets the height that the LithoView should take on the next measure pass and then requests a
   * layout. This should be called from animation-driving code on each frame to animate the size of
   * the LithoView.
   */
  @Override
  public void setAnimatedHeight(int height) {
    mAnimatedHeight = height;
    requestLayout();
  }

  @Override
  public abstract void notifyVisibleBoundsChanged(
      Rect visibleRect, boolean processVisibilityOutputs);

  @Override
  public abstract void notifyVisibleBoundsChanged();
}
