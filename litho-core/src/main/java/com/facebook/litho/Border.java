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

import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import com.facebook.yoga.YogaEdge;

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

  PathEffect mPathEffect;

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

  /**
   * @param values values pertaining to {@link YogaEdge}s
   * @return whether the values are equal for each edge
   */
  static boolean equalValues(int[] values) {
    if (values.length != EDGE_COUNT) {
      throw new IllegalArgumentException("Given wrongly sized array");
    }
    int lastValue = values[0];
    for (int i = 1, length = values.length; i < length; ++i) {
      if (lastValue != values[i]) {
        return false;
      }
    }
    return true;
  }

  private static void setEdgeValue(int[] edges, YogaEdge edge, int value) {
    switch (edge) {
      case ALL:
        for (int i = 0; i < EDGE_COUNT; ++i) {
          edges[i] = value;
        }
        break;
      case VERTICAL:
        edges[EDGE_TOP] = value;
        edges[EDGE_BOTTOM] = value;
        break;
      case HORIZONTAL:
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
    private static final int MAX_PATH_EFFECTS = 2;
    private final Border mBorder;
    private @Nullable ResourceResolver mResourceResolver;
    private PathEffect[] mPathEffects = new PathEffect[MAX_PATH_EFFECTS];
    private int mNumPathEffects;

    Builder(ComponentContext context) {
      mResourceResolver = new ResourceResolver();
      mResourceResolver.init(context, context.getResourceCache());
      mBorder = new Border();
    }

    /**
     * Specifies a width for a specific edge
     *
     * <p>Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The {@link YogaEdge} that will have its width modified
     * @param width The desired width in raw pixels
     */
    public Builder widthPx(YogaEdge edge, @Px int width) {
      checkNotBuilt();
      mBorder.setEdgeWidth(edge, width);
      return this;
    }

    /**
     * Specifies a width for a specific edge
     *
     * <p>Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The {@link YogaEdge} that will have its width modified
     * @param width The desired width in density independent pixels
     */
    public Builder widthDip(YogaEdge edge, @Dimension(unit = DP) int width) {
      checkNotBuilt();
      return widthPx(edge, mResourceResolver.dipsToPixels(width));
    }

    /**
     * Specifies a width for a specific edge
     *
     * <p>Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The {@link YogaEdge} that will have its width modified
     * @param widthRes The desired width resource to resolve
     */
    public Builder widthRes(YogaEdge edge, @DimenRes int widthRes) {
      checkNotBuilt();
      return widthPx(edge, mResourceResolver.resolveDimenSizeRes(widthRes));
    }

    /**
     * Specifies a width for a specific edge
     *
     * <p>Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The {@link YogaEdge} that will have its width modified
     * @param attrId The attribute to resolve a width value from
     */
    public Builder widthAttr(YogaEdge edge, @AttrRes int attrId) {
      checkNotBuilt();
      return widthAttr(edge, attrId, 0);
    }

    /**
     * Specifies a width for a specific edge
     *
     * <p>Note: Having a border effect with varying widths per edge is currently not supported
     *
     * @param edge The {@link YogaEdge} that will have its width modified
     * @param attrId The attribute to resolve a width value from
     * @param defaultResId Default resource value to utilize if the attribute is not set
     */
    public Builder widthAttr(YogaEdge edge, @AttrRes int attrId, @DimenRes int defaultResId) {
      checkNotBuilt();
      return widthPx(edge, mResourceResolver.resolveDimenSizeAttr(attrId, defaultResId));
    }

    /**
     * Specifies a color for a specific edge
     *
     * @param edge The {@link YogaEdge} that will have its color modified
     * @param color The raw color value to use
     */
    public Builder color(YogaEdge edge, @ColorInt int color) {
      checkNotBuilt();
      mBorder.setEdgeColor(edge, color);
      return this;
    }

    /**
     * Specifies a color for a specific edge
     *
     * @param edge The {@link YogaEdge} that will have its color modified
     * @param colorRes The color resource to use
     */
    public Builder colorRes(YogaEdge edge, @ColorRes int colorRes) {
      checkNotBuilt();
      return color(edge, mResourceResolver.resolveColorRes(colorRes));
    }

    /**
     * Applies a dash effect to the border
     *
     * <p>Specifying two effects will compose them where the first specified effect acts as the
     * outer effect and the second acts as the inner path effect, e.g. outer(inner(path))
     *
     * @param intervals Must be even-sized >= 2. Even indices specify "on" intervals and odd indices
     *     specify "off" intervals
     * @param phase Offset into the given intervals
     */
    public Builder dashEffect(float[] intervals, float phase) {
      checkNotBuilt();
      checkEffectCount();
      mPathEffects[mNumPathEffects++] = new DashPathEffect(intervals, phase);
      return this;
    }

    /**
     * Applies a corner effect to the border
     *
     * <p>Specifying two effects will compose them where the first specified effect acts as the
     * outer effect and the second acts as the inner path effect, e.g. outer(inner(path))
     *
     * @param radius The amount to round sharp angles when drawing the border
     */
    public Builder cornerEffect(float radius) {
      checkNotBuilt();
      checkEffectCount();
      mPathEffects[mNumPathEffects++] = new CornerPathEffect(radius);
      return this;
    }

    /**
     * Applies a discrete effect to the border
     *
     * <p>Specifying two effects will compose them where the first specified effect acts as the
     * outer effect and the second acts as the inner path effect, e.g. outer(inner(path))
     *
     * @param segmentLength Length of line segments
     * @param deviation Maximum amount of deviation. Utilized value is random in the range
     *     [-deviation, deviation]
     */
    public Builder discreteEffect(float segmentLength, float deviation) {
      checkNotBuilt();
      checkEffectCount();
      mPathEffects[mNumPathEffects++] = new DiscretePathEffect(segmentLength, deviation);
      return this;
    }

    /**
     * Applies a path dash effect to the border
     *
     * <p>Specifying two effects will compose them where the first specified effect acts as the
     * outer effect and the second acts as the inner path effect, e.g. outer(inner(path))
     *
     * @param shape The path to stamp along
     * @param advance The spacing between each stamp
     * @param phase Amount to offset before the first stamp
     * @param style How to transform the shape at each position
     */
    public Builder pathDashEffect(
        Path shape, float advance, float phase, PathDashPathEffect.Style style) {
      checkNotBuilt();
      checkEffectCount();
      mPathEffects[mNumPathEffects++] = new PathDashPathEffect(shape, advance, phase, style);
      return this;
    }

    public Border build() {
      checkNotBuilt();
      mResourceResolver.release();
      mResourceResolver = null;

      if (mNumPathEffects == MAX_PATH_EFFECTS) {
        mBorder.mPathEffect = new ComposePathEffect(mPathEffects[0], mPathEffects[1]);
      } else if (mNumPathEffects > 0) {
        mBorder.mPathEffect = mPathEffects[0];
      }

      if (mBorder.mPathEffect != null && !Border.equalValues(mBorder.mEdgeWidths)) {
        throw new IllegalArgumentException(
            "Borders do not currently support different widths with a path effect");
      }
      return mBorder;
    }

    private void checkNotBuilt() {
      if (mResourceResolver == null) {
        throw new IllegalStateException("This builder has already been disposed / built!");
      }
    }

    private void checkEffectCount() {
      if (mNumPathEffects >= MAX_PATH_EFFECTS) {
        throw new IllegalArgumentException("You cannot specify more than 2 effects to compose");
      }
    }
  }
}
