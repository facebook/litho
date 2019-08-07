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
package com.facebook.litho;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.Nullable;

public interface LithoTooltip {

  /**
   * Do not call this method directly! It should only be called by the Litho framework to ensure
   * that the correct parameters are passed. Use the methods exposed by {@link
   * LithoTooltipController} instead.
   *
   * <p>Display the tooltip on an anchor component with the given bounds relative to the view that
   * contains it.
   *
   * @param container View that wraps the Component that is used as anchor
   * @param anchorBounds Rect with the bounds of the component relative to host view
   * @param xOffset move tooltip with this horizontal offset from default position
   * @param yOffset move tooltip with this vertical offset from default position
   */
  void showLithoTooltip(@Nullable View container, Rect anchorBounds, int xOffset, int yOffset);
}
