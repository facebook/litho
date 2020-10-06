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

package com.facebook.rendercore.extensions;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.RenderTreeNode;

/**
 * The {@link LayoutResultVisitor} API is used by RenderCore to allow a {@link RenderCoreExtension}
 * to visit every {@link LayoutResult} during every layout pass. The visitor can collect any data
 * that it is interested in from every {@link LayoutResult}. A {@link LayoutResultVisitor} must not
 * mutate its {@link RenderCoreExtension}; it should be functional. Consequently, a {@link
 * LayoutResultVisitor} should only be used for one layout pass.
 *
 * @param <State> The state represents the data collected by the visitor for a layout pass.
 */
public interface LayoutResultVisitor<State> {

  /**
   * This API is called for every LayoutResult during a layout pass.
   *
   * @param parent
   * @param result The {@link LayoutResult} being visited.
   * @param bounds The bounds of this {@link LayoutResult} relative to its parent.
   * @param x The absolute x position.
   * @param y The absolute y position.
   * @param position The position of the layout result.
   * @param state The state the visitor can write to.
   */
  void visit(
      final @Nullable RenderTreeNode parent,
      final LayoutResult<?> result,
      final Rect bounds,
      final int x,
      final int y,
      int position,
      final @Nullable State state);
}
