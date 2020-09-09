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

package com.facebook.rendercore.utils;

import static android.view.View.MeasureSpec.makeMeasureSpec;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.RenderTreeNode;

public class BoundsUtils {

  public static void applyBoundsToMountContent(
      RenderTreeNode renderTreeNode, Object content, boolean force) {
    final Rect bounds = renderTreeNode.getBounds();
    BoundsUtils.applyBoundsToMountContent(
        bounds, renderTreeNode.getResolvedPadding(), content, force);
  }

  /**
   * Sets the bounds on the given content if the content doesn't already have those bounds (or if
   * 'force' * is supplied).
   *
   * @param bounds
   * @param padding
   * @param content
   * @param force
   */
  public static void applyBoundsToMountContent(
      final Rect bounds, @Nullable final Rect padding, Object content, boolean force) {
    applyBoundsToMountContent(
        bounds.left, bounds.top, bounds.right, bounds.bottom, padding, content, force);
  }

  /**
   * Sets the bounds on the given content if the content doesn't already have those bounds (or if
   * 'force' * is supplied).
   */
  public static void applyBoundsToMountContent(
      int left,
      int top,
      int right,
      int bottom,
      @Nullable final Rect padding,
      Object content,
      boolean force) {
    if (content instanceof View) {
      applyBoundsToView((View) content, left, top, right, bottom, padding, force);
    } else if (content instanceof Drawable) {
      if (padding != null) {
        left += padding.left;
        top += padding.top;
        right -= padding.right;
        bottom -= padding.bottom;
      }
      ((Drawable) content).setBounds(left, top, right, bottom);
    } else {
      throw new IllegalStateException("Unsupported mounted content " + content);
    }
  }

  /**
   * Sets the bounds on the given view if the view doesn't already have those bounds (or if 'force'
   * is supplied).
   */
  private static void applyBoundsToView(
      View view, int left, int top, int right, int bottom, @Nullable Rect padding, boolean force) {
    final int width = right - left;
    final int height = bottom - top;

    if (padding != null && !(view instanceof Host)) {
      view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
    }

    if (force || view.getMeasuredHeight() != height || view.getMeasuredWidth() != width) {
      view.measure(
          makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
    }

    if (force
        || view.getLeft() != left
        || view.getTop() != top
        || view.getRight() != right
        || view.getBottom() != bottom) {
      view.layout(left, top, right, bottom);
    }
  }
}
