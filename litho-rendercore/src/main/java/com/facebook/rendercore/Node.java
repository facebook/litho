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

package com.facebook.rendercore;

import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.facebook.rendercore.RenderState.LayoutContext;

/**
 * Represents a single node in a RenderCore Tree. A Node has children, base layout information, and
 * whether it needs to be rendered.
 */
public interface Node<RenderContext> extends Copyable {

  /**
   * Implementations of Node are responsible to calculate a layout based on the width/height
   * constraints provided. A Node could decide to implement its own layout function or to delegate
   * to its RenderUnit measure.
   *
   * <ul>
   *   The general contract is:
   *   <li>- A Node must call calculateLayout on each child (if it has any) at least once, even if
   *       the Node is going to assign that child an exact size -- this gives a chance for children
   *       to lay out their own children and produce artifacts like text layouts.
   *   <li>- If a Node calls layout on a child with flexible specs (UNSPECIFIED or AT_MOST) to get
   *       sizing information, but ultimately decides to assign that child a different size than the
   *       child returned, the Node must call calculateLayout again on that child with a mode of
   *       EXACTLY to enforce the assigned size.
   * </ul>
   *
   * @param context The LayoutContext associated with this layout calculation {@link LayoutContext}
   * @param widthSpec a measure spec for the width in the format of {@link View.MeasureSpec}
   * @param heightSpec a measure spec for the height in the format of {@link View.MeasureSpec}
   */
  LayoutResult calculateLayout(LayoutContext<RenderContext> context, int widthSpec, int heightSpec);

  /**
   * Returns the layout params of this Node. The layout params are useful to inform the parent of
   * this node, about how this Node should be laid-out. The layout params implement {@link
   * Copyable}, and can be copied to be reused but another node.
   *
   * @return the layout params for this Node.
   */
  Copyable getLayoutParams();

  /**
   * Represents the result of a Layout pass. A LayoutResult has a reference to its originating Node
   * and all the layout information needed to position the content of such Node.
   */
  interface LayoutResult<T> {

    /** @return the RenderUnit that should be rendered by this layout result. */
    @Nullable
    RenderUnit getRenderUnit();

    /**
     * @return layout specific data that was generated during the layout pass that created this
     *     LayoutResult.
     */
    @Nullable
    T getLayoutData();

    /** @return the number of children of this LayoutResult. */
    int getChildrenCount();

    /** @return the LayoutResult for the given child index */
    LayoutResult getChildAt(int index);

    /** @return the resolved X position for the Node */
    @Px
    int getXForChildAtIndex(int index);

    /** @return the resolved Y position for the Node */
    @Px
    int getYForChildAtIndex(int index);

    /** @return the resolved width for the Node */
    @Px
    int getWidth();

    /** @return the resolved height for the Node */
    @Px
    int getHeight();

    /** @return the resolved top padding for the Node */
    @Px
    int getPaddingTop();

    /** @return the resolved right padding for the Node */
    @Px
    int getPaddingRight();

    /** @return the resolved bottom padding for the Node */
    @Px
    int getPaddingBottom();

    /** @return the resolved left padding for the Node */
    @Px
    int getPaddingLeft();

    /** @return the width measurement that generated this LayoutResult */
    int getWidthSpec();

    /** @return the height measurement that generated this LayoutResult */
    int getHeightSpec();
  }
}
