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

import androidx.annotation.Nullable;
import androidx.annotation.Px;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaDisplay;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.List;

public class YogaProps implements Copyable, YogaPropsProvider, YogaRootLayoutParams {

  private static final long PFLAG_WIDTH_IS_SET = 1L << 0;
  private static final long PFLAG_WIDTH_PERCENT_IS_SET = 1L << 1;
  private static final long PFLAG_MIN_WIDTH_IS_SET = 1L << 2;
  private static final long PFLAG_MIN_WIDTH_PERCENT_IS_SET = 1L << 3;
  private static final long PFLAG_MAX_WIDTH_IS_SET = 1L << 4;
  private static final long PFLAG_MAX_WIDTH_PERCENT_IS_SET = 1L << 5;
  private static final long PFLAG_HEIGHT_IS_SET = 1L << 6;
  private static final long PFLAG_HEIGHT_PERCENT_IS_SET = 1L << 7;
  private static final long PFLAG_MIN_HEIGHT_IS_SET = 1L << 8;
  private static final long PFLAG_MIN_HEIGHT_PERCENT_IS_SET = 1L << 9;
  private static final long PFLAG_MAX_HEIGHT_IS_SET = 1L << 10;
  private static final long PFLAG_MAX_HEIGHT_PERCENT_IS_SET = 1L << 11;
  private static final long PFLAG_LAYOUT_DIRECTION_IS_SET = 1L << 12;
  private static final long PFLAG_ALIGN_SELF_IS_SET = 1L << 13;
  private static final long PFLAG_FLEX_IS_SET = 1L << 14;
  private static final long PFLAG_FLEX_GROW_IS_SET = 1L << 15;
  private static final long PFLAG_FLEX_SHRINK_IS_SET = 1L << 16;
  private static final long PFLAG_FLEX_BASIS_IS_SET = 1L << 17;
  private static final long PFLAG_FLEX_BASIS_PERCENT_IS_SET = 1L << 18;
  private static final long PFLAG_ASPECT_RATIO_IS_SET = 1L << 19;
  private static final long PFLAG_POSITION_TYPE_IS_SET = 1L << 20;
  private static final long PFLAG_POSITION_IS_SET = 1L << 21;
  private static final long PFLAG_POSITION_PERCENT_IS_SET = 1L << 22;
  private static final long PFLAG_PADDING_IS_SET = 1L << 23;
  private static final long PFLAG_PADDING_PERCENT_IS_SET = 1L << 24;
  private static final long PFLAG_MARGIN_IS_SET = 1L << 25;
  private static final long PFLAG_MARGIN_PERCENT_IS_SET = 1L << 26;
  private static final long PFLAG_MARGIN_AUTO_IS_SET = 1L << 27;
  private static final long PFLAG_IS_REFERENCE_BASELINE_IS_SET = 1L << 28;
  private static final long PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET = 1L << 29;
  private static final long PFLAG_FLEX_DIRECTION_IS_SET = 1L << 30;
  private static final long PFLAG_ALIGN_ITEMS_IS_SET = 1L << 31;
  private static final long PFLAG_ALIGN_CONTENT_IS_SET = 1L << 32;
  private static final long PFLAG_JUSTIFY_CONTENT_IS_SET = 1L << 33;
  private static final long PFLAG_WRAP_IS_SET = 1L << 34;
  private static final long PFLAG_WIDTH_AUTO = 1L << 35;
  private static final long PFLAG_HEIGHT_AUTO = 1L << 36;

  private long mPrivateFlags;

  @Px private int mWidthPx;
  private float mWidthPercent;
  @Px private int mMinWidthPx;
  private float mMinWidthPercent;
  @Px private int mMaxWidthPx;
  private float mMaxWidthPercent;
  @Px private int mHeightPx;
  private float mHeightPercent;
  @Px private int mMinHeightPx;
  private float mMinHeightPercent;
  @Px private int mMaxHeightPx;
  private float mMaxHeightPercent;
  private float mFlex;
  private float mFlexGrow;
  private float mFlexShrink;
  @Px private int mFlexBasisPx;
  private float mFlexBasisPercent;
  private float mAspectRatio;
  @Nullable private YogaDirection mLayoutDirection;
  @Nullable private YogaAlign mAlignSelf;
  @Nullable private YogaPositionType mPositionType;
  @Nullable private Edges mPositions;
  @Nullable private Edges mMargins;
  @Nullable private Edges mMarginPercents;
  @Nullable private List<YogaEdge> mMarginAutos;
  @Nullable private Edges mPaddings;
  @Nullable private Edges mPaddingPercents;
  @Nullable private Edges mPositionPercents;
  @Nullable private YogaFlexDirection mFlexDirection;
  @Nullable private YogaAlign mAlignItems;
  @Nullable private YogaAlign mAlignContent;
  @Nullable private YogaJustify mJustifyContent;
  @Nullable private YogaWrap mWrap;
  @Nullable private YogaDisplay mDisplay;

  private boolean mIsReferenceBaseline;
  private boolean mUseHeightAsBaseline;
  private boolean mUsePercentDimensAtRoot;

  public void flexDirection(YogaFlexDirection flexDirection) {
    mPrivateFlags |= PFLAG_FLEX_DIRECTION_IS_SET;
    mFlexDirection = flexDirection;
  }

  public void widthPx(@Px int width) {
    mPrivateFlags |= PFLAG_WIDTH_IS_SET;
    mWidthPx = width;
  }

  public void widthPercent(float percent) {
    mPrivateFlags |= PFLAG_WIDTH_PERCENT_IS_SET;
    mWidthPercent = percent;
  }

  public void widthAuto() {
    mPrivateFlags |= PFLAG_WIDTH_AUTO;
  }

  public void minWidthPx(@Px int minWidth) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_IS_SET;
    mMinWidthPx = minWidth;
  }

  public void maxWidthPx(@Px int maxWidth) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_IS_SET;
    mMaxWidthPx = maxWidth;
  }

  public void minWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_WIDTH_PERCENT_IS_SET;
    mMinWidthPercent = percent;
  }

  public void maxWidthPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_WIDTH_PERCENT_IS_SET;
    mMaxWidthPercent = percent;
  }

  public void heightPx(@Px int height) {
    mPrivateFlags |= PFLAG_HEIGHT_IS_SET;
    mHeightPx = height;
  }

  public void heightPercent(float percent) {
    mPrivateFlags |= PFLAG_HEIGHT_PERCENT_IS_SET;
    mHeightPercent = percent;
  }

  public void heightAuto() {
    mPrivateFlags |= PFLAG_HEIGHT_AUTO;
  }

  public void minHeightPx(@Px int minHeight) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_IS_SET;
    mMinHeightPx = minHeight;
  }

  public void maxHeightPx(@Px int maxHeight) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_IS_SET;
    mMaxHeightPx = maxHeight;
  }

  public void minHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MIN_HEIGHT_PERCENT_IS_SET;
    mMinHeightPercent = percent;
  }

  public void maxHeightPercent(float percent) {
    mPrivateFlags |= PFLAG_MAX_HEIGHT_PERCENT_IS_SET;
    mMaxHeightPercent = percent;
  }

  public void layoutDirection(YogaDirection direction) {
    mPrivateFlags |= PFLAG_LAYOUT_DIRECTION_IS_SET;
    mLayoutDirection = direction;
  }

  public void alignSelf(YogaAlign alignSelf) {
    mPrivateFlags |= PFLAG_ALIGN_SELF_IS_SET;
    mAlignSelf = alignSelf;
  }

  public void flex(float flex) {
    mPrivateFlags |= PFLAG_FLEX_IS_SET;
    mFlex = flex;
  }

  public void flexGrow(float flexGrow) {
    mPrivateFlags |= PFLAG_FLEX_GROW_IS_SET;
    mFlexGrow = flexGrow;
  }

  public void flexShrink(float flexShrink) {
    mPrivateFlags |= PFLAG_FLEX_SHRINK_IS_SET;
    mFlexShrink = flexShrink;
  }

  public void flexBasisPx(@Px int flexBasis) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_IS_SET;
    mFlexBasisPx = flexBasis;
  }

  public void flexBasisPercent(float percent) {
    mPrivateFlags |= PFLAG_FLEX_BASIS_PERCENT_IS_SET;
    mFlexBasisPercent = percent;
  }

  public void aspectRatio(float aspectRatio) {
    mPrivateFlags |= PFLAG_ASPECT_RATIO_IS_SET;
    mAspectRatio = aspectRatio;
  }

  public void positionType(@Nullable YogaPositionType positionType) {
    mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
    mPositionType = positionType;
  }

  public void positionPx(YogaEdge edge, @Px int position) {
    mPrivateFlags |= PFLAG_POSITION_IS_SET;
    if (mPositions == null) {
      mPositions = new Edges();
    }

    mPositions.set(edge, position);
  }

  public void positionPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_POSITION_PERCENT_IS_SET;
    if (mPositionPercents == null) {
      mPositionPercents = new Edges();
    }
    mPositionPercents.set(edge, percent);
  }

  public void paddingPx(YogaEdge edge, @Px int padding) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;
    if (mPaddings == null) {
      mPaddings = new Edges();
    }
    mPaddings.set(edge, padding);
  }

  public void paddingPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_PADDING_PERCENT_IS_SET;
    if (mPaddingPercents == null) {
      mPaddingPercents = new Edges();
    }
    mPaddingPercents.set(edge, percent);
  }

  public void marginPx(YogaEdge edge, @Px int margin) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;

    if (mMargins == null) {
      mMargins = new Edges();
    }
    mMargins.set(edge, margin);
  }

  public void marginPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_MARGIN_PERCENT_IS_SET;
    if (mMarginPercents == null) {
      mMarginPercents = new Edges();
    }
    mMarginPercents.set(edge, percent);
  }

  public void marginAuto(YogaEdge edge) {
    mPrivateFlags |= PFLAG_MARGIN_AUTO_IS_SET;
    if (mMarginAutos == null) {
      mMarginAutos = new ArrayList<>(2);
    }
    mMarginAutos.add(edge);
  }

  public void isReferenceBaseline(boolean isReferenceBaseline) {
    mPrivateFlags |= PFLAG_IS_REFERENCE_BASELINE_IS_SET;
    mIsReferenceBaseline = isReferenceBaseline;
  }

  public void useHeightAsBaseline(boolean useHeightAsBaseline) {
    mPrivateFlags |= PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET;
    mUseHeightAsBaseline = useHeightAsBaseline;
  }

  public void justifyContent(YogaJustify justifyContent) {
    mPrivateFlags |= PFLAG_JUSTIFY_CONTENT_IS_SET;
    mJustifyContent = justifyContent;
  }

  public void alignItems(YogaAlign alignItems) {
    mPrivateFlags |= PFLAG_ALIGN_ITEMS_IS_SET;
    mAlignItems = alignItems;
  }

  public void alignContent(YogaAlign alignContent) {
    mPrivateFlags |= PFLAG_ALIGN_CONTENT_IS_SET;
    mAlignContent = alignContent;
  }

  public void wrap(YogaWrap flexWrap) {
    mPrivateFlags |= PFLAG_WRAP_IS_SET;
    mWrap = flexWrap;
  }

  public void setDisplay(@Nullable YogaDisplay yogaDisplay) {
    mDisplay = yogaDisplay;
  }

  public void setUsePercentDimensAtRoot() {
    mUsePercentDimensAtRoot = true;
  }

  @Override
  public void applyToNode(RenderState.LayoutContext context, YogaNode yogaNode) {
    if ((mPrivateFlags & PFLAG_WIDTH_IS_SET) != 0L) {
      yogaNode.setWidth(mWidthPx);
    }
    if ((mPrivateFlags & PFLAG_WIDTH_PERCENT_IS_SET) != 0L) {
      yogaNode.setWidthPercent(mWidthPercent);
    }
    if ((mPrivateFlags & PFLAG_WIDTH_AUTO) != 0L) {
      yogaNode.setWidthAuto();
    }
    if ((mPrivateFlags & PFLAG_MIN_WIDTH_IS_SET) != 0L) {
      yogaNode.setMinWidth(mMinWidthPx);
    }
    if ((mPrivateFlags & PFLAG_MIN_WIDTH_PERCENT_IS_SET) != 0L) {
      yogaNode.setMinWidthPercent(mMinWidthPercent);
    }
    if ((mPrivateFlags & PFLAG_MAX_WIDTH_IS_SET) != 0L) {
      yogaNode.setMaxWidth(mMaxWidthPx);
    }
    if ((mPrivateFlags & PFLAG_MAX_WIDTH_PERCENT_IS_SET) != 0L) {
      yogaNode.setMaxWidthPercent(mMaxWidthPercent);
    }
    if ((mPrivateFlags & PFLAG_HEIGHT_IS_SET) != 0L) {
      yogaNode.setHeight(mHeightPx);
    }
    if ((mPrivateFlags & PFLAG_HEIGHT_PERCENT_IS_SET) != 0L) {
      yogaNode.setHeightPercent(mHeightPercent);
    }
    if ((mPrivateFlags & PFLAG_HEIGHT_AUTO) != 0L) {
      yogaNode.setHeightAuto();
    }
    if ((mPrivateFlags & PFLAG_MIN_HEIGHT_IS_SET) != 0L) {
      yogaNode.setMinHeight(mMinHeightPx);
    }
    if ((mPrivateFlags & PFLAG_MIN_HEIGHT_PERCENT_IS_SET) != 0L) {
      yogaNode.setMinHeightPercent(mMinHeightPercent);
    }
    if ((mPrivateFlags & PFLAG_MAX_HEIGHT_IS_SET) != 0L) {
      yogaNode.setMaxHeight(mMaxHeightPx);
    }
    if ((mPrivateFlags & PFLAG_MAX_HEIGHT_PERCENT_IS_SET) != 0L) {
      yogaNode.setMaxHeightPercent(mMaxHeightPercent);
    }
    if ((mPrivateFlags & PFLAG_LAYOUT_DIRECTION_IS_SET) != 0L) {
      yogaNode.setDirection(mLayoutDirection);
    }
    if ((mPrivateFlags & PFLAG_ALIGN_SELF_IS_SET) != 0L) {
      yogaNode.setAlignSelf(mAlignSelf);
    }
    if ((mPrivateFlags & PFLAG_FLEX_IS_SET) != 0L) {
      yogaNode.setFlex(mFlex);
    }
    if ((mPrivateFlags & PFLAG_FLEX_GROW_IS_SET) != 0L) {
      yogaNode.setFlexGrow(mFlexGrow);
    }
    if ((mPrivateFlags & PFLAG_FLEX_SHRINK_IS_SET) != 0L) {
      yogaNode.setFlexShrink(mFlexShrink);
    }
    if ((mPrivateFlags & PFLAG_FLEX_BASIS_IS_SET) != 0L) {
      yogaNode.setFlexBasis(mFlexBasisPx);
    }
    if ((mPrivateFlags & PFLAG_FLEX_BASIS_PERCENT_IS_SET) != 0L) {
      yogaNode.setFlexBasisPercent(mFlexBasisPercent);
    }
    if ((mPrivateFlags & PFLAG_ASPECT_RATIO_IS_SET) != 0L) {
      yogaNode.setAspectRatio(mAspectRatio);
    }
    if ((mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
      yogaNode.setPositionType(mPositionType);
    }
    if ((mPrivateFlags & PFLAG_POSITION_IS_SET) != 0L) {
      for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
        final float value = mPositions.getRaw(i);
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setPosition(YogaEdge.fromInt(i), (int) value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_POSITION_PERCENT_IS_SET) != 0L) {
      for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
        final float value = mPositionPercents.getRaw(i);
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setPositionPercent(YogaEdge.fromInt(i), value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L) {
      for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
        final float value = mPaddings.getRaw(i);
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setPadding(YogaEdge.fromInt(i), (int) value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_PADDING_PERCENT_IS_SET) != 0L) {
      for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
        final float value = mPaddingPercents.getRaw(i);
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setPaddingPercent(YogaEdge.fromInt(i), value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_MARGIN_IS_SET) != 0L) {
      for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
        final float value = mMargins.getRaw(i);
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setMargin(YogaEdge.fromInt(i), (int) value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_MARGIN_PERCENT_IS_SET) != 0L) {
      for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
        final float value = mMarginPercents.getRaw(i);
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setMarginPercent(YogaEdge.fromInt(i), value);
        }
      }
    }
    if ((mPrivateFlags & PFLAG_MARGIN_AUTO_IS_SET) != 0L) {
      for (YogaEdge edge : mMarginAutos) {
        yogaNode.setMarginAuto(edge);
      }
    }
    if ((mPrivateFlags & PFLAG_IS_REFERENCE_BASELINE_IS_SET) != 0L) {
      yogaNode.setIsReferenceBaseline(mIsReferenceBaseline);
    }
    if ((mPrivateFlags & PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET) != 0L && mUseHeightAsBaseline) {
      yogaNode.setBaselineFunction(
          new YogaBaselineFunction() {
            @Override
            public float baseline(YogaNode yogaNode, float width, float height) {
              return height;
            }
          });
    }

    if ((mPrivateFlags & PFLAG_FLEX_DIRECTION_IS_SET) != 0L) {
      yogaNode.setFlexDirection(mFlexDirection);
    }

    if ((mPrivateFlags & PFLAG_ALIGN_ITEMS_IS_SET) != 0L) {
      yogaNode.setAlignItems(mAlignItems);
    }

    if ((mPrivateFlags & PFLAG_ALIGN_CONTENT_IS_SET) != 0L) {
      yogaNode.setAlignContent(mAlignContent);
    }
    if (mJustifyContent != null) {
      yogaNode.setJustifyContent(mJustifyContent);
    }

    if ((mPrivateFlags & PFLAG_WRAP_IS_SET) != 0L) {
      yogaNode.setWrap(mWrap);
    }

    if (mDisplay != null) {
      yogaNode.setDisplay(mDisplay);
    }
  }

  @Override
  public YogaProps makeCopy() {
    try {
      return (YogaProps) clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean usePercentDimensAtRoot() {
    return mUsePercentDimensAtRoot;
  }

  @Override
  public boolean hasPercentWidth() {
    return (mPrivateFlags & PFLAG_WIDTH_PERCENT_IS_SET) != 0L;
  }

  @Override
  public boolean hasPercentHeight() {
    return (mPrivateFlags & PFLAG_HEIGHT_PERCENT_IS_SET) != 0L;
  }

  @Override
  public float getWidthPercent() {
    return mWidthPercent;
  }

  @Override
  public float getHeightPercent() {
    return mHeightPercent;
  }
}
