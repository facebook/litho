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

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.util.SparseArray;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;

/**
 * Class representing an empty InternalNode with a null ComponentLayout. All methods have been
 * overridden so no actions are performed, and no exceptions are thrown.
 */
class NoOpInternalNode extends InternalNode {

  protected NoOpInternalNode() {
    super(null, null);
  }

  @Override
  void appendComponent(Component component) {
  }

  @Px
  @Override
  public int getX() {
    return 0;
  }

  @Px
  @Override
  public int getY() {
    return 0;
  }

  @Px
  @Override
  public int getWidth() {
    return 0;
  }

  @Px
  @Override
  public int getHeight() {
    return 0;
  }

  @Px
  @Override
  public int getPaddingLeft() {
    return 0;
  }

  @Px
  @Override
  public int getPaddingTop() {
    return 0;
  }

  @Px
  @Override
  public int getPaddingRight() {
    return 0;
  }

  @Px
  @Override
  public int getPaddingBottom() {
    return 0;
  }

  @Override
  public void setCachedMeasuresValid(boolean valid) {}

  @Override
  public int getLastWidthSpec() {
    return 0;
  }

  @Override
  public void setLastWidthSpec(int widthSpec) {}

  @Override
  public int getLastHeightSpec() {
    return 0;
  }

  @Override
  public void setLastHeightSpec(int heightSpec) {}

  @Override
  void setLastMeasuredWidth(float lastMeasuredWidth) {}

  @Override
  void setLastMeasuredHeight(float lastMeasuredHeight) {}

  @Override
  void setDiffNode(DiffNode diffNode) {}

  @Override
  public InternalNode layoutDirection(YogaDirection direction) {
    return this;
  }

  @Override
  public InternalNode flexDirection(YogaFlexDirection direction) {
    return this;
  }

  @Override
  public InternalNode wrap(YogaWrap wrap) {
    return this;
  }

  @Override
  public InternalNode justifyContent(YogaJustify justifyContent) {
    return this;
  }

  @Override
  public InternalNode alignItems(YogaAlign alignItems) {
    return this;
  }

  @Override
  public InternalNode alignContent(YogaAlign alignContent) {
    return this;
  }

  @Override
  public InternalNode alignSelf(YogaAlign alignSelf) {
    return this;
  }

  @Override
  public InternalNode positionType(YogaPositionType positionType) {
    return this;
  }

  @Override
  public InternalNode flex(float flex) {
    return this;
  }

  @Override
  public InternalNode flexGrow(float flexGrow) {
    return this;
  }

  @Override
  public InternalNode flexShrink(float flexShrink) {
    return this;
  }

  @Override
  public InternalNode flexBasisPx(@Px int flexBasis) {
    return this;
  }

  @Override
  public InternalNode flexBasisPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode importantForAccessibility(int importantForAccessibility) {
    return this;
  }

  @Override
  public InternalNode duplicateParentState(boolean duplicateParentState) {
    return this;
  }

  @Override
  public InternalNode marginPx(YogaEdge edge, @Px int margin) {
    return this;
  }

  @Override
  public InternalNode marginPercent(YogaEdge edge, float percent) {
    return this;
  }

  @Override
  public InternalNode marginAuto(YogaEdge edge) {
    return this;
  }

  @Override
  public InternalNode border(Border border) {
    return this;
  }

  @Override
  public InternalNode paddingPx(YogaEdge edge, @Px int padding) {
    return this;
  }

  @Override
  public InternalNode paddingPercent(YogaEdge edge, float percent) {
    return this;
  }

  @Override
  public InternalNode positionPx(YogaEdge edge, @Px int position) {
    return this;
  }

  @Override
  public InternalNode positionPercent(YogaEdge edge, float percent) {
    return this;
  }

  @Override
  public InternalNode widthPx(@Px int width) {
    return this;
  }

  @Override
  public InternalNode widthPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode minWidthPx(@Px int minWidth) {
    return this;
  }

  @Override
  public InternalNode minWidthPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode maxWidthPx(@Px int maxWidth) {
    return this;
  }

  @Override
  public InternalNode maxWidthPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode heightPx(@Px int height) {
    return this;
  }

  @Override
  public InternalNode heightPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode minHeightPx(@Px int minHeight) {
    return this;
  }

  @Override
  public InternalNode minHeightPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode maxHeightPx(@Px int maxHeight) {
    return this;
  }

  @Override
  public InternalNode maxHeightPercent(float percent) {
    return this;
  }

  @Override
  public InternalNode child(Component child) {
    return this;
  }

  @Override
  public InternalNode child(Component.Builder<?> child) {
    return this;
  }

  @Override
  public InternalNode background(Reference<? extends Drawable> builder) {
    return this;
  }

  @Override
  InternalNode background(@Nullable ComparableDrawable background) {
    return this;
  }

  @Override
  public InternalNode foreground(@Nullable ComparableDrawable foreground) {
    return this;
  }

  @Override
  public InternalNode foreground(@Nullable Drawable foreground) {
    return this;
  }

  @Override
  public InternalNode wrapInView() {
    return this;
  }

  @Override
  public InternalNode visibleHandler(EventHandler<VisibleEvent> visibleHandler) {
    return this;
  }

  @Override
  public InternalNode focusedHandler(EventHandler<FocusedVisibleEvent> focusedHandler) {
    return this;
  }

  @Override
  public InternalNode fullImpressionHandler(
      EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    return this;
  }

  @Override
  public InternalNode invisibleHandler(EventHandler<InvisibleEvent> invisibleHandler) {
    return this;
  }

  @Override
  public InternalNode unfocusedHandler(EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    return this;
  }

  @Override
  public InternalNode transitionKey(String key) {
    return this;
  }

  @Override
  public InternalNode isReferenceBaseline(boolean isReferenceBaseline) {
    return this;
  }

  @Override
  void setNestedTree(InternalNode nestedTree) {}

  @Override
  void copyInto(InternalNode node) {}

  @Override
  void setStyleWidthFromSpec(int widthSpec) {}

  @Override
  void setStyleHeightFromSpec(int heightSpec) {}

  @Override
  void applyAttributes(TypedArray a) {}
}
