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
import android.content.Context;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class representing an empty InternalNode with a null ComponentLayout. All methods have been
 * overridden so no actions are performed, and no exceptions are thrown.
 */
class NoOpInternalNode implements InternalNode {

  NoOpInternalNode() {}

  @Override
  public void addChildAt(InternalNode child, int index) {}

  @Override
  public void addComponentNeedingPreviousRenderData(String key, Component component) {}

  @Override
  public void addTransition(Transition transition) {}

  @Override
  public void addWorkingRanges(List<WorkingRangeContainer.Registration> registrations) {}

  @Override
  public void addAttachable(Attachable attachable) {}

  @Override
  public @Nullable List<Attachable> getAttachables() {
    return null;
  }

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
  public void setNestedPadding(@Nullable Edges padding, @Nullable boolean[] isPercentage) {}

  @Override
  public LayoutProps getDebugLayoutEditor() {
    return null;
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
  public void border(int[] widths, int[] colors, float[] radii, @Nullable PathEffect effect) {}

  @Override
  public LithoLayoutResult calculateLayout(ComponentContext c, int widthSpec, int heightSpec) {
    return NullLayoutResult.INSTANCE;
  }

  @Override
  public @Nullable InternalNode child(ComponentContext c, Component child) {
    return null;
  }

  @Override
  public @Nullable InternalNode child(InternalNode child) {
    return null;
  }

  @Override
  public @Nullable InternalNode duplicateParentState(boolean duplicateParentState) {
    return null;
  }

  @Override
  public @Nullable InternalNode duplicateChildrenStates(boolean duplicateChildState) {
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

  @Nullable
  @Override
  public NoOpInternalNode getChildAt(int index) {
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
  public Map<String, Component> getComponentsNeedingPreviousRenderData() {
    return null;
  }

  @Override
  public @Nullable Context getAndroidContext() {
    return null;
  }

  @Override
  public @Nullable Drawable getBackground() {
    return null;
  }

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

  @Nullable
  @Override
  public NodeInfo getNodeInfo() {
    return null;
  }

  @Override
  public void setNodeInfo(NodeInfo nodeInfo) {}

  @Override
  public @Nullable NodeInfo getOrCreateNodeInfo() {
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
  public boolean hasBorderColor() {
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
  public boolean isLayoutDirectionInherit() {
    return false;
  }

  @Override
  public @Nullable InternalNode justifyContent(YogaJustify justifyContent) {
    return null;
  }

  @Override
  public InternalNode removeChildAt(int index) {
    return null;
  }

  @Override
  public void registerDebugComponent(DebugComponent debugComponent) {}

  @Deprecated
  @Override
  public boolean implementsLayoutDiffing() {
    return false;
  }

  @Override
  public void setMeasureFunction(YogaMeasureFunction measureFunction) {}

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
  public void layoutDirection(YogaDirection direction) {}

  @Override
  public @Nullable InternalNode wrap(YogaWrap wrap) {
    return null;
  }

  @Override
  public @Nullable InternalNode wrapInView() {
    return null;
  }

  @Override
  public void applyAttributes(Context c, int defStyleAttr, int defStyleRes) {}

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
  public InternalNode reconcile(ComponentContext c, Component next, @Nullable String nextKey) {
    return this;
  }

  @Override
  public void freeze(LayoutStateContext c, YogaNode node, @Nullable YogaNode parent) {}
}
