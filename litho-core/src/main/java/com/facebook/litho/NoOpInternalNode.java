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

import android.animation.StateListAnimator;
import android.content.res.TypedArray;
import android.graphics.PathEffect;
import android.graphics.drawable.Drawable;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class representing an empty InternalNode with a null ComponentLayout. All methods have been
 * overridden so no actions are performed, and no exceptions are thrown.
 */
class NoOpInternalNode implements InternalNode {

  NoOpInternalNode() {}

  @Override
  public void addChildAt(InternalNode child, int index) {}

  @Override
  public void addComponentNeedingPreviousRenderData(Component component) {}

  @Override
  public void addTransition(Transition transition) {}

  @Override
  public void addWorkingRanges(List<WorkingRangeContainer.Registration> registrations) {}

  @Override
  public InternalNode alignContent(YogaAlign alignContent) {
    return null;
  }

  @Override
  public InternalNode alignItems(YogaAlign alignItems) {
    return null;
  }

  @Override
  public InternalNode alignSelf(YogaAlign alignSelf) {
    return null;
  }

  @Override
  public void appendComponent(Component component) {}

  @Override
  public boolean areCachedMeasuresValid() {
    return false;
  }

  @Override
  public InternalNode aspectRatio(float aspectRatio) {
    return null;
  }

  @Override
  public InternalNode background(@Nullable ComparableDrawable background) {
    return null;
  }

  @Override
  public InternalNode background(@Nullable Drawable background) {
    return null;
  }

  @Override
  public InternalNode backgroundColor(int backgroundColor) {
    return null;
  }

  @Override
  public InternalNode backgroundRes(int resId) {
    return null;
  }

  @Override
  public InternalNode border(Border border) {
    return null;
  }

  @Override
  public void border(Edges width, int[] colors, float[] radii) {}

  @Override
  public void calculateLayout(float width, float height) {}

  @Override
  public void calculateLayout() {}

  @Override
  public InternalNode child(Component child) {
    return null;
  }

  @Override
  public InternalNode child(Component.Builder<?> child) {
    return null;
  }

  @Override
  public InternalNode child(InternalNode child) {
    return null;
  }

  @Override
  public InternalNode duplicateParentState(boolean duplicateParentState) {
    return null;
  }

  @Override
  public InternalNode flex(float flex) {
    return null;
  }

  @Override
  public InternalNode flexBasisAuto() {
    return null;
  }

  @Override
  public InternalNode flexBasisPercent(float percent) {
    return null;
  }

  @Override
  public InternalNode flexBasisPx(int flexBasis) {
    return null;
  }

  @Override
  public InternalNode flexDirection(YogaFlexDirection direction) {
    return null;
  }

  @Override
  public InternalNode flexGrow(float flexGrow) {
    return null;
  }

  @Override
  public InternalNode flexShrink(float flexShrink) {
    return null;
  }

  @Override
  public InternalNode focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
    return null;
  }

  @Override
  public InternalNode foreground(@Nullable Drawable foreground) {
    return null;
  }

  @Override
  public InternalNode foreground(@Nullable ComparableDrawable foreground) {
    return null;
  }

  @Override
  public InternalNode foregroundColor(int foregroundColor) {
    return null;
  }

  @Override
  public InternalNode foregroundRes(int resId) {
    return null;
  }

  @Override
  public InternalNode fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler) {
    return null;
  }

  @Override
  public int[] getBorderColors() {
    return new int[0];
  }

  @Nullable
  @Override
  public PathEffect getBorderPathEffect() {
    return null;
  }

  @Override
  public float[] getBorderRadius() {
    return new float[0];
  }

  @Nullable
  @Override
  public InternalNode getChildAt(int index) {
    return null;
  }

  @Override
  public int getChildCount() {
    return 0;
  }

  @Override
  public int getChildIndex(InternalNode child) {
    return 0;
  }

  @Override
  public List<Component> getComponents() {
    return null;
  }

  @Nullable
  @Override
  public ArrayList<Component> getComponentsNeedingPreviousRenderData() {
    return null;
  }

  @Override
  public ComponentContext getContext() {
    return null;
  }

  @Nullable
  @Override
  public DiffNode getDiffNode() {
    return null;
  }

  @Override
  public int getX() {
    return 0;
  }

  @Override
  public int getY() {
    return 0;
  }

  @Override
  public int getWidth() {
    return 0;
  }

  @Override
  public int getHeight() {
    return 0;
  }

  @Override
  public int getPaddingTop() {
    return 0;
  }

  @Override
  public int getPaddingRight() {
    return 0;
  }

  @Override
  public int getPaddingBottom() {
    return 0;
  }

  @Override
  public int getPaddingLeft() {
    return 0;
  }

  @Override
  public boolean isPaddingSet() {
    return false;
  }

  @androidx.annotation.Nullable
  @Override
  public ComparableDrawable getBackground() {
    return null;
  }

  @Override
  public YogaDirection getResolvedLayoutDirection() {
    return null;
  }

  @Override
  public void setDiffNode(@Nullable DiffNode diffNode) {}

  @Nullable
  @Override
  public EventHandler<FocusedVisibleEvent> getFocusedHandler() {
    return null;
  }

  @Nullable
  @Override
  public ComparableDrawable getForeground() {
    return null;
  }

  @Nullable
  @Override
  public EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler() {
    return null;
  }

  @Override
  public int getImportantForAccessibility() {
    return 0;
  }

  @Nullable
  @Override
  public EventHandler<InvisibleEvent> getInvisibleHandler() {
    return null;
  }

  @Override
  public int getLastHeightSpec() {
    return 0;
  }

  @Override
  public void setLastHeightSpec(int heightSpec) {}

  @Override
  public float getLastMeasuredHeight() {
    return 0;
  }

  @Override
  public void setLastMeasuredHeight(float lastMeasuredHeight) {}

  @Override
  public float getLastMeasuredWidth() {
    return 0;
  }

  @Override
  public void setLastMeasuredWidth(float lastMeasuredWidth) {}

  @Override
  public int getLastWidthSpec() {
    return 0;
  }

  @Override
  public void setLastWidthSpec(int widthSpec) {}

  @Override
  public int getLayoutBorder(YogaEdge edge) {
    return 0;
  }

  @Override
  public float getMaxHeight() {
    return 0;
  }

  @Override
  public float getMaxWidth() {
    return 0;
  }

  @Override
  public float getMinHeight() {
    return 0;
  }

  @Override
  public float getMinWidth() {
    return 0;
  }

  @Nullable
  @Override
  public InternalNode getNestedTree() {
    return null;
  }

  @Override
  public void setNestedTree(InternalNode nestedTree) {}

  @Nullable
  @Override
  public InternalNode getNestedTreeHolder() {
    return null;
  }

  @Nullable
  @Override
  public NodeInfo getNodeInfo() {
    return null;
  }

  @Override
  public void setNodeInfo(NodeInfo nodeInfo) {}

  @Override
  public NestedTreeProps getOrCreateNestedTreeProps() {
    return null;
  }

  @Override
  public NodeInfo getOrCreateNodeInfo() {
    return null;
  }

  @Nullable
  @Override
  public InternalNode getParent() {
    return null;
  }

  @Nullable
  @Override
  public TreeProps getPendingTreeProps() {
    return null;
  }

  @Nullable
  @Override
  public Component getRootComponent() {
    return null;
  }

  @Override
  public void setRootComponent(Component component) {}

  @Nullable
  @Override
  public StateListAnimator getStateListAnimator() {
    return null;
  }

  @Override
  public int getStateListAnimatorRes() {
    return 0;
  }

  @Override
  public YogaDirection getStyleDirection() {
    return null;
  }

  @Override
  public float getStyleHeight() {
    return 0;
  }

  @Override
  public float getStyleWidth() {
    return 0;
  }

  @Nullable
  @Override
  public String getTestKey() {
    return null;
  }

  @Nullable
  @Override
  public Edges getTouchExpansion() {
    return null;
  }

  @Override
  public int getTouchExpansionBottom() {
    return 0;
  }

  @Override
  public int getTouchExpansionLeft() {
    return 0;
  }

  @Override
  public int getTouchExpansionRight() {
    return 0;
  }

  @Override
  public int getTouchExpansionTop() {
    return 0;
  }

  @Nullable
  @Override
  public String getTransitionKey() {
    return null;
  }

  @Nullable
  @Override
  public Transition.TransitionKeyType getTransitionKeyType() {
    return null;
  }

  @Nullable
  @Override
  public ArrayList<Transition> getTransitions() {
    return null;
  }

  @Nullable
  @Override
  public EventHandler<UnfocusedVisibleEvent> getUnfocusedHandler() {
    return null;
  }

  @Nullable
  @Override
  public EventHandler<VisibilityChangedEvent> getVisibilityChangedHandler() {
    return null;
  }

  @Nullable
  @Override
  public EventHandler<VisibleEvent> getVisibleHandler() {
    return null;
  }

  @Override
  public float getVisibleHeightRatio() {
    return 0;
  }

  @Override
  public float getVisibleWidthRatio() {
    return 0;
  }

  @Nullable
  @Override
  public ArrayList<WorkingRangeContainer.Registration> getWorkingRangeRegistrations() {
    return null;
  }

  @Override
  public YogaNode getYogaNode() {
    return null;
  }

  @Override
  public boolean hasBorderColor() {
    return false;
  }

  @Override
  public boolean hasNestedTree() {
    return false;
  }

  @Override
  public boolean hasNewLayout() {
    return false;
  }

  @Override
  public boolean hasStateListAnimatorResSet() {
    return false;
  }

  @Override
  public boolean hasTouchExpansion() {
    return false;
  }

  @Override
  public boolean hasTransitionKey() {
    return false;
  }

  @Override
  public boolean hasVisibilityHandlers() {
    return false;
  }

  @Override
  public InternalNode heightAuto() {
    return null;
  }

  @Override
  public InternalNode heightPercent(float percent) {
    return null;
  }

  @Override
  public InternalNode heightPx(int height) {
    return null;
  }

  @Override
  public InternalNode importantForAccessibility(int importantForAccessibility) {
    return null;
  }

  @Override
  public InternalNode invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler) {
    return null;
  }

  @Override
  public boolean isDuplicateParentStateEnabled() {
    return false;
  }

  @Override
  public boolean isForceViewWrapping() {
    return false;
  }

  @Override
  public boolean isImportantForAccessibilityIsSet() {
    return false;
  }

  @Override
  public boolean isInitialized() {
    return false;
  }

  @Override
  public boolean isLayoutDirectionSet() {
    return false;
  }

  @Override
  public boolean isNestedTreeHolder() {
    return false;
  }

  @Override
  public boolean isPaddingPercent(YogaEdge edge) {
    return false;
  }

  @Override
  public InternalNode isReferenceBaseline(boolean isReferenceBaseline) {
    return null;
  }

  @Override
  public InternalNode justifyContent(YogaJustify justifyContent) {
    return null;
  }

  @Override
  public InternalNode layoutDirection(YogaDirection direction) {
    return null;
  }

  @Override
  public InternalNode marginAuto(YogaEdge edge) {
    return null;
  }

  @Override
  public InternalNode marginPercent(YogaEdge edge, float percent) {
    return null;
  }

  @Override
  public InternalNode marginPx(YogaEdge edge, int margin) {
    return null;
  }

  @Override
  public void markIsNestedTreeHolder(TreeProps currentTreeProps) {}

  @Override
  public void markLayoutSeen() {}

  @Override
  public InternalNode maxHeightPercent(float percent) {
    return null;
  }

  @Override
  public InternalNode maxHeightPx(int maxHeight) {
    return null;
  }

  @Override
  public InternalNode maxWidthPercent(float percent) {
    return null;
  }

  @Override
  public InternalNode maxWidthPx(int maxWidth) {
    return null;
  }

  @Override
  public InternalNode minHeightPercent(float percent) {
    return null;
  }

  @Override
  public InternalNode minHeightPx(int minHeight) {
    return null;
  }

  @Override
  public InternalNode minWidthPercent(float percent) {
    return null;
  }

  @Override
  public InternalNode minWidthPx(int minWidth) {
    return null;
  }

  @Override
  public void padding(Edges padding, @Nullable InternalNode holder) {}

  @Override
  public InternalNode paddingPercent(YogaEdge edge, float percent) {
    return null;
  }

  @Override
  public InternalNode paddingPx(YogaEdge edge, int padding) {
    return null;
  }

  @Override
  public InternalNode positionPercent(YogaEdge edge, float percent) {
    return null;
  }

  @Override
  public InternalNode positionPx(YogaEdge edge, int position) {
    return null;
  }

  @Override
  public InternalNode positionType(YogaPositionType positionType) {
    return null;
  }

  @Override
  public YogaDirection recursivelyResolveLayoutDirection() {
    return null;
  }

  @Override
  public void registerDebugComponent(DebugComponent debugComponent) {}

  @Override
  public InternalNode removeChildAt(int index) {
    return null;
  }

  @Override
  public void resetResolvedLayoutProperties() {}

  @Override
  public void setBorderWidth(YogaEdge edge, int borderWidth) {}

  @Override
  public void setCachedMeasuresValid(boolean valid) {}

  @Override
  public void setMeasureFunction(YogaMeasureFunction measureFunction) {}

  @Override
  public void setStyleHeightFromSpec(int heightSpec) {}

  @Override
  public void setStyleWidthFromSpec(int widthSpec) {}

  @Override
  public boolean shouldDrawBorders() {
    return false;
  }

  @Override
  public InternalNode stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
    return null;
  }

  @Override
  public InternalNode stateListAnimatorRes(int resId) {
    return null;
  }

  @Override
  public InternalNode testKey(@Nullable String testKey) {
    return null;
  }

  @Override
  public InternalNode touchExpansionPx(YogaEdge edge, int touchExpansion) {
    return null;
  }

  @Override
  public InternalNode transitionKey(@Nullable String key) {
    return null;
  }

  @Override
  public InternalNode transitionKeyType(@Nullable Transition.TransitionKeyType type) {
    return null;
  }

  @Override
  public InternalNode unfocusedHandler(
      @Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    return null;
  }

  @Override
  public void useHeightAsBaselineFunction(boolean useHeightAsBaselineFunction) {}

  @Override
  public InternalNode visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    return null;
  }

  @Override
  public InternalNode visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler) {
    return null;
  }

  @Override
  public InternalNode visibleHeightRatio(float visibleHeightRatio) {
    return null;
  }

  @Override
  public InternalNode visibleWidthRatio(float visibleWidthRatio) {
    return null;
  }

  @Override
  public InternalNode widthAuto() {
    return null;
  }

  @Override
  public InternalNode widthPercent(float percent) {
    return null;
  }

  @Override
  public InternalNode widthPx(int width) {
    return null;
  }

  @Override
  public InternalNode wrap(YogaWrap wrap) {
    return null;
  }

  @Override
  public InternalNode wrapInView() {
    return null;
  }

  @Override
  public void copyInto(InternalNode target) {}

  @Override
  public void applyAttributes(TypedArray a) {}
}
