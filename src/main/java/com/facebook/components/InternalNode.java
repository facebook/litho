/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.SparseArray;

import com.facebook.R;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.reference.ColorDrawableReference;
import com.facebook.litho.reference.Reference;
import com.facebook.litho.reference.ResourceDrawableReference;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaNodeAPI;
import com.facebook.yoga.YogaOverflow;
import com.facebook.yoga.Spacing;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.support.annotation.Dimension.DP;
import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.END;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.START;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaEdge.VERTICAL;

/**
 * Internal class representing both a {@link ComponentLayout} and a
 * {@link com.facebook.litho.ComponentLayout.ContainerBuilder}.
 */
@ThreadConfined(ThreadConfined.ANY)
class InternalNode implements ComponentLayout, ComponentLayout.ContainerBuilder {

  // Used to check whether or not the framework can use style IDs for
  // paddingStart/paddingEnd due to a bug in some Android devices.
  private static final boolean SUPPORTS_RTL = (SDK_INT >= JELLY_BEAN_MR1);

  // When this flag is set, layoutDirection style was explicitly set on this node.
  private static final long PFLAG_LAYOUT_DIRECTION_IS_SET = 1L << 0;
  // When this flag is set, alignSelf was explicitly set on this node.
  private static final long PFLAG_ALIGN_SELF_IS_SET = 1L << 1;
  // When this flag is set, position type was explicitly set on this node.
  private static final long PFLAG_POSITION_TYPE_IS_SET = 1L << 2;
  // When this flag is set, flex was explicitly set on this node.
  private static final long PFLAG_FLEX_IS_SET = 1L << 3;
  // When this flag is set, flex grow was explicitly set on this node.
  private static final long PFLAG_FLEX_GROW_IS_SET = 1L << 4;
  // When this flag is set, flex shrink was explicitly set on this node.
  private static final long PFLAG_FLEX_SHRINK_IS_SET = 1L << 5;
  // When this flag is set, flex basis was explicitly set on this node.
  private static final long PFLAG_FLEX_BASIS_IS_SET = 1L << 6;
  // When this flag is set, importantForAccessibility was explicitly set on this node.
  private static final long PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET = 1L << 7;
  // When this flag is set, duplicateParentState was explicitly set on this node.
  private static final long PFLAG_DUPLICATE_PARENT_STATE_IS_SET = 1L << 8;
  // When this flag is set, margin was explicitly set on this node.
  private static final long PFLAG_MARGIN_IS_SET = 1L << 9;
  // When this flag is set, padding was explicitly set on this node.
  private static final long PFLAG_PADDING_IS_SET = 1L << 10;
  // When this flag is set, position was explicitly set on this node.
  private static final long PFLAG_POSITION_IS_SET = 1L << 11;
  // When this flag is set, width was explicitly set on this node.
  private static final long PFLAG_WIDTH_IS_SET = 1L << 12;
  // When this flag is set, minWidth was explicitly set on this node.
  private static final long PFLAG_MIN_WIDTH_IS_SET = 1L << 13;
  // When this flag is set, maxWidth was explicitly set on this node.
  private static final long PFLAG_MAX_WIDTH_IS_SET = 1L << 14;
  // When this flag is set, height was explicitly set on this node.
  private static final long PFLAG_HEIGHT_IS_SET = 1L << 15;
  // When this flag is set, minHeight was explicitly set on this node.
  private static final long PFLAG_MIN_HEIGHT_IS_SET = 1L << 16;
  // When this flag is set, maxHeight was explicitly set on this node.
  private static final long PFLAG_MAX_HEIGHT_IS_SET = 1L << 17;
