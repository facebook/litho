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

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.visibility.VisibilityOutput;

public class LithoVisibilityOutputFactory implements VisibilityOutput.Factory<InternalNode> {

  @Override
  public @Nullable VisibilityOutput createVisibilityOutput(InternalNode node, Rect absoluteBounds) {
    if (node.hasVisibilityHandlers()) {
      final Component component = node.getTailComponent();
      return new VisibilityOutput(
          component != null ? component.getGlobalKey() : "null",
          component != null ? component.getSimpleName() : "Unknown",
          absoluteBounds,
          node.getVisibleHeightRatio(),
          node.getVisibleWidthRatio(),
          node.getVisibleHandler(),
          node.getInvisibleHandler(),
          node.getFocusedHandler(),
          node.getUnfocusedHandler(),
          node.getFullImpressionHandler(),
          node.getVisibilityChangedHandler());
    } else {
      return null;
    }
  }
}
