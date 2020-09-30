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

import android.animation.StateListAnimator;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Copyable;
import com.facebook.rendercore.RenderState.LayoutContext;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.visibility.VisibilityOutput;
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
import java.util.Collections;
import java.util.List;

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
  public @Nullable InternalNode alignContent(YogaAlign alignContent) {
    return null;
  }

  @Override
  public @Nullable InternalNode alignItems(YogaAlign alignItems) {
    return null;
  }

  @Override
  public void appendComponent(Component component, String key) {}

  @Override
  public void appendUnresolvedComponent(Component component) {}

  @Override
  public boolean areCachedMeasuresValid() {
    return false;
  }

  @Override
  public @Nullable InternalNode background(@Nullable Drawable background) {
    return null;
  }

  @Override
  public @Nullable InternalNode backgroundColor(int backgroundColor) {
    return null;
  }

  @Override
  public @Nullable InternalNode backgroundRes(int resId) {
    return null;
  }

  @Override
  public @Nullable InternalNode border(Border border) {
    return null;
  }

  @Override
  public void border(Edges width, int[] colors, float[] radii) {}

  @Override
  public void calculateLayout(float width, float height) {}

  @Override
  public void calculateLayout() {}

  @Override
  public @Nullable InternalNode child(Component child) {
    return null;
  }

  @Override
  public @Nullable InternalNode child(Component.Builder<?> child) {
    return null;
  }

  @Override
  public @Nullable InternalNode child(InternalNode child) {
    return null;
  }

  @Override
  public void copyInto(InternalNode target) {}

  @Override
  public @Nullable InternalNode duplicateParentState(boolean duplicateParentState) {
    return null;
  }

  @Override
  public @Nullable InternalNode duplicateChildrenStates(boolean duplicateChildState) {
    return null;
  }

  @Override
  public @Nullable InternalNode flexBasisAuto() {
    return null;
  }

  @Override
  public @Nullable InternalNode flexDirection(YogaFlexDirection direction) {
    return null;
  }

  @Override
  public @Nullable InternalNode focusedHandler(
      @Nullable EventHandler<FocusedVisibleEvent> focusedHandler) {
    return null;
  }

  @Override
  public @Nullable InternalNode foreground(@Nullable Drawable foreground) {
    return null;
  }

  @Override
  public @Nullable InternalNode foregroundColor(int foregroundColor) {
    return null;
  }

  @Override
  public @Nullable InternalNode foregroundRes(int resId) {
    return null;
  }

  @Override
  public InternalNode layerType(@LayerType int layoutType, Paint layerPaint) {
    return null;
  }

  @Override
  public @LayerType int getLayerType() {
    return LayerType.LAYER_TYPE_NOT_SET;
  }

  @Override
  public @Nullable Paint getLayerPaint() {
    return null;
  }

  @Override
  public @Nullable InternalNode fullImpressionHandler(
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

  @Override
  public RenderUnit getRenderUnit() {
    return null;
  }

  @Override
  public int getChildrenCount() {
    return 0;
  }

  @Nullable
  @Override
  public InternalNode getChildAt(int index) {
    return null;
  }

  @Override
  public int getXForChildAtIndex(int index) {
    return 0;
  }

  @Override
  public int getYForChildAtIndex(int index) {
    return 0;
  }

  @Override
  public int getHeightSpec() {
    return 0;
  }

  @Override
  public int getWidthSpec() {
    return 0;
  }

  @Override
  public Object getLayoutData() {
    return null;
  }

  @Nullable
  @Override
  public Component getComponent() {
    return null;
  }

  @Override
  public void setComponent(@Nullable Component component) {}

  @Override
  public int getChildCount() {
    return 0;
  }

  @Override
  public int getChildIndex(InternalNode child) {
    return 0;
  }

  @Override
  public @Nullable List<Component> getComponents() {
    return null;
  }

  @Override
  @Nullable
  public List<String> getComponentKeys() {
    return null;
  }

  @Nullable
  @Override
  public List<Component> getUnresolvedComponents() {
    return null;
  }

  @Nullable
  @Override
  public ArrayList<Component> getComponentsNeedingPreviousRenderData() {
    return null;
  }

  @Override
  public @Nullable ComponentContext getContext() {
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

  @Override
  public @Nullable Drawable getBackground() {
    return null;
  }

  @Override
  public @Nullable YogaDirection getResolvedLayoutDirection() {
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
  public Drawable getForeground() {
    return null;
  }

  @Nullable
  @Override
  public EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler() {
    return null;
  }

  @Nullable
  @Override
  public Component getHeadComponent() {
    return null;
  }

  @javax.annotation.Nullable
  @Override
  public String getHeadComponentKey() {
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
  public List<DiffNode> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public void addChild(DiffNode node) {}

  @Nullable
  @Override
  public LayoutOutput getContentOutput() {
    return null;
  }

  @Override
  public void setContentOutput(@Nullable LayoutOutput content) {}

  @Nullable
  @Override
  public VisibilityOutput getVisibilityOutput() {
    return null;
  }

  @Override
  public void setVisibilityOutput(@Nullable VisibilityOutput visibilityOutput) {}

  @Nullable
  @Override
  public LayoutOutput getBackgroundOutput() {
    return null;
  }

  @Override
  public void setBackgroundOutput(@Nullable LayoutOutput background) {}

  @Nullable
  @Override
  public LayoutOutput getForegroundOutput() {
    return null;
  }

  @Override
  public void setForegroundOutput(@Nullable LayoutOutput foreground) {}

  @Nullable
  @Override
  public LayoutOutput getBorderOutput() {
    return null;
  }

  @Override
  public void setBorderOutput(@Nullable LayoutOutput border) {}

  @Nullable
  @Override
  public LayoutOutput getHostOutput() {
    return null;
  }

  @Override
  public void setHostOutput(@Nullable LayoutOutput host) {}

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
  public @Nullable NestedTreeProps getOrCreateNestedTreeProps() {
    return null;
  }

  @Override
  public @Nullable NodeInfo getOrCreateNodeInfo() {
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
  public Component getTailComponent() {
    return null;
  }

  @Nullable
  @Override
  public String getTailComponentKey() {
    return null;
  }

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
  public @Nullable YogaDirection getStyleDirection() {
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
  public String getTransitionOwnerKey() {
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
  public @Nullable YogaNode getYogaNode() {
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
  public @Nullable InternalNode heightAuto() {
    return null;
  }

  @Override
  public @Nullable InternalNode importantForAccessibility(int importantForAccessibility) {
    return null;
  }

  @Override
  public @Nullable InternalNode invisibleHandler(
      @Nullable EventHandler<InvisibleEvent> invisibleHandler) {
    return null;
  }

  @Override
  public boolean isDuplicateParentStateEnabled() {
    return false;
  }

  @Override
  public boolean isDuplicateChildrenStatesEnabled() {
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
  public boolean isLayoutDirectionInherit() {
    return false;
  }

  @Override
  public boolean isNestedTreeHolder() {
    return false;
  }

  @Override
  public @Nullable InternalNode justifyContent(YogaJustify justifyContent) {
    return null;
  }

  @Override
  public void markIsNestedTreeHolder(@Nullable TreeProps currentTreeProps) {}

  @Override
  public void markLayoutSeen() {}

  @Override
  public @Nullable YogaDirection recursivelyResolveLayoutDirection() {
    return null;
  }

  @Override
  public void registerDebugComponent(DebugComponent debugComponent) {}

  @Override
  public @Nullable InternalNode removeChildAt(int index) {
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
  public @Nullable InternalNode stateListAnimator(@Nullable StateListAnimator stateListAnimator) {
    return null;
  }

  @Override
  public @Nullable InternalNode stateListAnimatorRes(int resId) {
    return null;
  }

  @Override
  public @Nullable InternalNode testKey(@Nullable String testKey) {
    return null;
  }

  @Override
  public @Nullable InternalNode touchExpansionPx(YogaEdge edge, int touchExpansion) {
    return null;
  }

  @Override
  public @Nullable InternalNode transitionKey(@Nullable String key, @Nullable String ownerKey) {
    return null;
  }

  @Override
  public @Nullable InternalNode transitionKeyType(@Nullable Transition.TransitionKeyType type) {
    return null;
  }

  @Override
  public @Nullable String getTransitionGlobalKey() {
    return null;
  }

  @Override
  public @Nullable InternalNode unfocusedHandler(
      @Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    return null;
  }

  @Override
  public @Nullable InternalNode visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    return null;
  }

  @Override
  public @Nullable InternalNode visibleHandler(
      @Nullable EventHandler<VisibleEvent> visibleHandler) {
    return null;
  }

  @Override
  public @Nullable InternalNode visibleHeightRatio(float visibleHeightRatio) {
    return null;
  }

  @Override
  public @Nullable InternalNode visibleWidthRatio(float visibleWidthRatio) {
    return null;
  }

  @Override
  public @Nullable InternalNode widthAuto() {
    return null;
  }

  @Override
  public void widthPx(int width) {}

  @Override
  public void widthPercent(float percent) {}

  @Override
  public void minWidthPx(int minWidth) {}

  @Override
  public void maxWidthPx(int maxWidth) {}

  @Override
  public void minWidthPercent(float percent) {}

  @Override
  public void maxWidthPercent(float percent) {}

  @Override
  public void heightPx(int height) {}

  @Override
  public void heightPercent(float percent) {}

  @Override
  public void minHeightPx(int minHeight) {}

  @Override
  public void maxHeightPx(int maxHeight) {}

  @Override
  public void minHeightPercent(float percent) {}

  @Override
  public void maxHeightPercent(float percent) {}

  @Override
  public void layoutDirection(YogaDirection direction) {}

  @Override
  public void alignSelf(YogaAlign alignSelf) {}

  @Override
  public void flex(float flex) {}

  @Override
  public void flexGrow(float flexGrow) {}

  @Override
  public void flexShrink(float flexShrink) {}

  @Override
  public void flexBasisPx(int flexBasis) {}

  @Override
  public void flexBasisPercent(float percent) {}

  @Override
  public void aspectRatio(float aspectRatio) {}

  @Override
  public void positionType(@Nullable YogaPositionType positionType) {}

  @Override
  public void positionPx(YogaEdge edge, int position) {}

  @Override
  public void positionPercent(YogaEdge edge, float percent) {}

  @Override
  public void paddingPx(YogaEdge edge, int padding) {}

  @Override
  public void paddingPercent(YogaEdge edge, float percent) {}

  @Override
  public void marginPx(YogaEdge edge, int margin) {}

  @Override
  public void marginPercent(YogaEdge edge, float percent) {}

  @Override
  public void marginAuto(YogaEdge edge) {}

  @Override
  public void isReferenceBaseline(boolean isReferenceBaseline) {}

  @Override
  public void useHeightAsBaseline(boolean useHeightAsBaseline) {}

  @Override
  public @Nullable InternalNode wrap(YogaWrap wrap) {
    return null;
  }

  @Override
  public @Nullable InternalNode wrapInView() {
    return null;
  }

  @Override
  public void applyAttributes(TypedArray a) {}

  @Override
  public void assertContextSpecificStyleNotSet() {}

  @Override
  public InternalNode deepClone() {
    throw new UnsupportedOperationException("NoOpInternalNode.deepClone not implemented.");
  }

  @Override
  public String getSimpleName() {
    return "NoOpInternalNode";
  }

  @Override
  public InternalNode reconcile(
      LayoutStateContext layoutStateContext, ComponentContext c, Component next, @Nullable String nextKey) {
    return this;
  }

  @Override
  public LayoutResult calculateLayout(LayoutContext context, int widthSpec, int heightSpec) {
    return null;
  }

  @Override
  public Copyable getLayoutParams() {
    return null;
  }

  @Override
  public Copyable makeCopy() {
    throw new UnsupportedOperationException("NoOpInternalNode.makeCopy not implemented.");
  }
}
