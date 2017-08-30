/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.support.annotation.Dimension.DP;

import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.Px;
import com.facebook.yoga.YogaEdge;
import javax.annotation.Nullable;

/**
 * Represents a collection of attributes that describe how a border should be applied to a layout
 */
public class Border {
  static final int EDGE_LEFT = 0;
  static final int EDGE_TOP = 1;
  static final int EDGE_RIGHT = 2;
  static final int EDGE_BOTTOM = 3;
  static final int EDGE_COUNT = 4;

  final int[] mEdgeWidths = new int[EDGE_COUNT];
  final int[] mEdgeColors = new int[EDGE_COUNT];

  public static Builder create(ComponentContext context) {
    return new Builder(context);
  }

  private Border() {}

  void setEdgeWidth(YogaEdge edge, int width) {
    if (width < 0) {
      throw new IllegalArgumentException(
          "Given negative border width value: " + width + " for edge " + edge.name());
    }
    setEdgeValue(mEdgeWidths, edge, width);
  }

  void setEdgeColor(YogaEdge edge, @ColorInt int color) {
    setEdgeValue(mEdgeColors, edge, color);
  }

  static int getEdgeColor(int[] colorArray, YogaEdge edge) {
    if (colorArray.length != EDGE_COUNT) {
      throw new IllegalArgumentException("Given wrongly sized array");
    }
    return colorArray[edgeIndex(edge)];
  }

  static YogaEdge edgeFromIndex(int i) {
    if (i < 0 || i >= EDGE_COUNT) {
      throw new IllegalArgumentException("Given index out of range of acceptable edges: " + i);
    }
    switch (i) {
      case EDGE_LEFT:
        return YogaEdge.LEFT;
      case EDGE_TOP:
        return YogaEdge.TOP;
      case EDGE_RIGHT:
        return YogaEdge.RIGHT;
      case EDGE_BOTTOM:
        return YogaEdge.BOTTOM;
    }
    throw new IllegalArgumentException("Given unknown edge index: " + i);
  }

  private static void setEdgeValue(int[] edges, YogaEdge edge, int value) {
    switch (edge) {
      case ALL:
        for (int i = 0; i < EDGE_COUNT; ++i) {
          edges[i] = value;
        }
        break;
      case HORIZONTAL:
        edges[EDGE_TOP] = value;
        edges[EDGE_BOTTOM] = value;
        break;
      case VERTICAL:
        edges[EDGE_LEFT] = value;
        edges[EDGE_RIGHT] = value;
        break;
      default:
        edges[edgeIndex(edge)] = value;
        break;
    }
  }

  private static int edgeIndex(YogaEdge edge) {
    switch (edge) {
      case START:
      case LEFT:
        return EDGE_LEFT;
      case TOP:
        return EDGE_TOP;
      case END:
      case RIGHT:
        return EDGE_RIGHT;
      case BOTTOM:
        return EDGE_BOTTOM;
    }
    throw new IllegalArgumentException("Given unsupported edge " + edge.name());
  }

  public static class Builder {
    private final Border mBorder;
    private @Nullable ResourceResolver mResourceResolver;

    Builder(ComponentContext context) {
      mResourceResolver = new ResourceResolver();
      mResourceResolver.init(context, context.getResourceCache());
      mBorder = new Border();
    }

    public Builder widthPx(YogaEdge edge, @Px int width) {
      checkNotBuilt();
      mBorder.setEdgeWidth(edge, width);
      return this;
    }

    public Builder widthDip(YogaEdge edge, @Dimension(unit = DP) int width) {
      checkNotBuilt();
      return widthPx(edge, mResourceResolver.dipsToPixels(width));
    }

    public Builder widthRes(YogaEdge edge, @DimenRes int borderWidthRes) {
      checkNotBuilt();
      return widthPx(edge, mResourceResolver.resolveDimenOffsetRes(borderWidthRes));
    }

    public Builder widthAttr(YogaEdge edge, @AttrRes int resId) {
      checkNotBuilt();
      return widthAttr(edge, resId, 0);
    }

    public Builder widthAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId) {
      checkNotBuilt();
      return widthPx(edge, mResourceResolver.resolveDimenOffsetAttr(resId, defaultResId));
    }

    public Builder color(YogaEdge edge, @ColorInt int color) {
      checkNotBuilt();
      mBorder.setEdgeColor(edge, color);
      return this;
    }

    public Builder colorRes(YogaEdge edge, @ColorRes int colorRes) {
      checkNotBuilt();
      return color(edge, mResourceResolver.resolveColorRes(colorRes));
    }

    public Border build() {
      checkNotBuilt();
      mResourceResolver.release();
      mResourceResolver = null;
      return mBorder;
    }

    private void checkNotBuilt() {
      if (mResourceResolver == null) {
        throw new IllegalStateException("This builder has already been disposed / built!");
      }
    }
  }
}
