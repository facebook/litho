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

import static com.facebook.litho.CommonUtils.addOrCreateList;

import android.text.TextUtils;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import java.util.List;

public class YogaLayoutProps implements LayoutProps {

  private static final String INVALID_LAYOUT_PROPS = "YogaLayoutProps:ContextSpecificStyleSet";
  private static final long PFLAG_ALIGN_SELF_IS_SET = 1L << 1;
  private static final long PFLAG_POSITION_TYPE_IS_SET = 1L << 2;
  private static final long PFLAG_FLEX_IS_SET = 1L << 3;
  private static final long PFLAG_FLEX_GROW_IS_SET = 1L << 4;
  private static final long PFLAG_MARGIN_IS_SET = 1L << 5;

  private final YogaNode node;

  boolean isPaddingSet;
  private long mPrivateFlags;

  public YogaLayoutProps(YogaNode node) {
    this.node = node;
  }

  @Override
  public void widthPx(int width) {
    node.setWidth(width);
  }

  @Override
  public void widthPercent(float percent) {
    node.setWidthPercent(percent);
  }

  @Override
  public void minWidthPx(int minWidth) {
    node.setMinWidth(minWidth);
  }

  @Override
  public void maxWidthPx(int maxWidth) {
    node.setMaxWidth(maxWidth);
  }

  @Override
  public void minWidthPercent(float percent) {
    node.setMinWidthPercent(percent);
  }

  @Override
  public void maxWidthPercent(float percent) {
    node.setMaxWidthPercent(percent);
  }

  @Override
  public void heightPx(int height) {
    node.setHeight(height);
  }

  @Override
  public void heightPercent(float percent) {
    node.setHeightPercent(percent);
  }

  @Override
  public void minHeightPx(int minHeight) {
    node.setMinHeight(minHeight);
  }

  @Override
  public void maxHeightPx(int maxHeight) {
    node.setMaxHeight(maxHeight);
  }

  @Override
  public void minHeightPercent(float percent) {
    node.setMinHeightPercent(percent);
  }

  @Override
  public void maxHeightPercent(float percent) {
    node.setMaxHeightPercent(percent);
  }

  @Override
  public void layoutDirection(YogaDirection direction) {
    node.setDirection(direction);
  }

  @Override
  public void alignSelf(YogaAlign alignSelf) {
    mPrivateFlags |= PFLAG_ALIGN_SELF_IS_SET;
    node.setAlignSelf(alignSelf);
  }

  @Override
  public void flex(float flex) {
    mPrivateFlags |= PFLAG_FLEX_IS_SET;
    node.setFlex(flex);
  }

  @Override
  public void flexGrow(float flexGrow) {
    mPrivateFlags |= PFLAG_FLEX_GROW_IS_SET;
    node.setFlexGrow(flexGrow);
  }

  @Override
  public void flexShrink(float flexShrink) {
    node.setFlexShrink(flexShrink);
  }

  @Override
  public void flexBasisPx(int flexBasis) {
    node.setFlexBasis(flexBasis);
  }

  @Override
  public void flexBasisPercent(float percent) {
    node.setFlexBasisPercent(percent);
  }

  @Override
  public void aspectRatio(float aspectRatio) {
    node.setAspectRatio(aspectRatio);
  }

  @Override
  public void positionType(YogaPositionType positionType) {
    mPrivateFlags |= PFLAG_POSITION_TYPE_IS_SET;
    node.setPositionType(positionType);
  }

  @Override
  public void positionPx(YogaEdge edge, int position) {
    node.setPosition(edge, position);
  }

  @Override
  public void positionPercent(YogaEdge edge, float percent) {
    node.setPositionPercent(edge, percent);
  }

  @Override
  public void paddingPx(YogaEdge edge, int padding) {
    isPaddingSet = true;
    node.setPadding(edge, padding);
  }

  @Override
  public void paddingPercent(YogaEdge edge, float percent) {
    isPaddingSet = true;
    node.setPaddingPercent(edge, percent);
  }

  @Override
  public void marginPx(YogaEdge edge, int margin) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    node.setMargin(edge, margin);
  }

  @Override
  public void marginPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    node.setMarginPercent(edge, percent);
  }

  @Override
  public void marginAuto(YogaEdge edge) {
    mPrivateFlags |= PFLAG_MARGIN_IS_SET;
    node.setMarginAuto(edge);
  }

  @Override
  public void isReferenceBaseline(boolean isReferenceBaseline) {
    node.setIsReferenceBaseline(isReferenceBaseline);
  }

  @Override
  public void useHeightAsBaseline(boolean useHeightAsBaseline) {
    if (useHeightAsBaseline) {
      node.setBaselineFunction(
          new YogaBaselineFunction() {
            @Override
            public float baseline(YogaNode yogaNode, float width, float height) {
              return height;
            }
          });
    }
  }

  @Override
  public void heightAuto() {
    node.setHeightAuto();
  }

  @Override
  public void widthAuto() {
    node.setWidthAuto();
  }

  @Override
  public void flexBasisAuto() {
    node.setFlexBasisAuto();
  }

  @Override
  public void setBorderWidth(YogaEdge edge, float borderWidth) {
    node.setBorder(edge, borderWidth);
  }

  public void flexDirection(YogaFlexDirection direction) {
    node.setFlexDirection(direction);
  }

  public void wrap(YogaWrap wrap) {
    node.setWrap(wrap);
  }

  public void justifyContent(YogaJustify justify) {
    node.setJustifyContent(justify);
  }

  public void alignItems(YogaAlign align) {
    node.setAlignItems(align);
  }

  public void validateLayoutPropsForRoot() {
    List<CharSequence> error = null;
    if ((mPrivateFlags & PFLAG_ALIGN_SELF_IS_SET) != 0L) {
      error = addOrCreateList(error, "alignSelf");
    }
    if ((mPrivateFlags & PFLAG_POSITION_TYPE_IS_SET) != 0L) {
      error = addOrCreateList(error, "positionType");
    }
    if ((mPrivateFlags & PFLAG_FLEX_IS_SET) != 0L) {
      error = addOrCreateList(error, "flex");
    }
    if ((mPrivateFlags & PFLAG_FLEX_GROW_IS_SET) != 0L) {
      error = addOrCreateList(error, "flexGrow");
    }
    if ((mPrivateFlags & PFLAG_MARGIN_IS_SET) != 0L) {
      error = addOrCreateList(error, "margin");
    }

    if (error != null) {
      final CharSequence props = TextUtils.join(", ", error);
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          INVALID_LAYOUT_PROPS,
          "You should not set " + props + " to a root layout.");
    }
  }
}
