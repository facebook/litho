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
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.StyleRes;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderState.LayoutContext;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaWrap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Internal class representing a {@link ComponentLayout}. */
@ThreadConfined(ThreadConfined.ANY)
public interface InternalNode extends Node<LithoRenderContext> {

  void addChildAt(InternalNode child, int index);

  void addComponentNeedingPreviousRenderData(String globalKey, Component component);

  void addTransition(Transition transition);

  void addWorkingRanges(List<WorkingRangeContainer.Registration> registrations);

  void addAttachable(Attachable attachable);

  InternalNode alignContent(YogaAlign alignContent);

  InternalNode alignItems(YogaAlign alignItems);

  void appendComponent(
      Component component, String key, @Nullable ScopedComponentInfo scopedComponentInfo);

  void appendUnresolvedComponent(Component component);

  InternalNode background(@Nullable Drawable background);

  InternalNode backgroundColor(@ColorInt int backgroundColor);

  InternalNode backgroundRes(@DrawableRes int resId);

  InternalNode border(Border border);

  void border(int[] widths, int[] colors, float[] radii, @Nullable PathEffect effect);

  LithoLayoutResult calculateLayout(
      LayoutContext<LithoRenderContext> c, int widthSpec, int heightSpec);

  InternalNode child(
      LayoutStateContext layoutContext, ComponentContext c, @Nullable Component child);

  InternalNode child(@Nullable InternalNode child);

  InternalNode duplicateParentState(boolean duplicateParentState);

  InternalNode duplicateChildrenStates(boolean duplicateChildState);

  InternalNode flexDirection(YogaFlexDirection direction);

  InternalNode focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler);

  InternalNode foreground(@Nullable Drawable foreground);

  InternalNode foregroundColor(@ColorInt int foregroundColor);

  InternalNode foregroundRes(@DrawableRes int resId);

  InternalNode layerType(@LayerType int layoutType, Paint layerPaint);

  InternalNode fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler);

  void setNodeInfo(NodeInfo nodeInfo);

  NodeInfo getOrCreateNodeInfo();

  InternalNode importantForAccessibility(int importantForAccessibility);

  InternalNode invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler);

  InternalNode justifyContent(YogaJustify justifyContent);

  InternalNode removeChildAt(int index);

  void setMeasureFunction(YogaMeasureFunction measureFunction);

  InternalNode stateListAnimator(@Nullable StateListAnimator stateListAnimator);

  InternalNode stateListAnimatorRes(@DrawableRes int resId);

  InternalNode testKey(@Nullable String testKey);

  InternalNode touchExpansionPx(YogaEdge edge, @Px int touchExpansion);

  InternalNode transitionKey(@Nullable String key, @Nullable String ownerKey);

  InternalNode transitionKeyType(@Nullable Transition.TransitionKeyType type);

  InternalNode unfocusedHandler(@Nullable EventHandler<UnfocusedVisibleEvent> unfocusedHandler);

  InternalNode visibilityChangedHandler(
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler);

  InternalNode visibleHandler(@Nullable EventHandler<VisibleEvent> visibleHandler);

  InternalNode visibleHeightRatio(float visibleHeightRatio);

  InternalNode visibleWidthRatio(float visibleWidthRatio);

  InternalNode wrap(YogaWrap wrap);

  InternalNode wrapInView();

  void applyAttributes(Context c, @AttrRes int defStyleAttr, @StyleRes int defStyleRes);

  /* InternalNode related APIs */

  Context getAndroidContext();

  String getSimpleName();

  InternalNode getChildAt(int index);

  int getChildCount();

  int getChildIndex(InternalNode child);

  void assertContextSpecificStyleNotSet();

  boolean isClone();

  /**
   * Reconcile returns a new InternalNode tree where only mutated sub-trees are recreated and all
   * other sub-trees are copied. The returned InternalNode tree represents the updated layout tree.
   *
   * @param layoutStateContext
   * @param c The new ComponentContext.
   * @param next The new component to reconcile against.
   * @return The reconciled InternalNode which represents {@param next}.
   */
  InternalNode reconcile(
      LayoutStateContext layoutStateContext,
      ComponentContext c,
      Component next,
      @Nullable ScopedComponentInfo nextScopedComponentInfo,
      @Nullable String nextKey);

  InternalNode deepClone();

  /* Component related APIs */

  /**
   * Return the list of components contributing to this InternalNode. This exists in both debug and
   * production mode.
   */
  List<Component> getComponents();

  /**
   * Return the list of keys of components contributing to this InternalNode. This exists in both
   * debug and production mode.
   */
  List<String> getComponentKeys();

  @Nullable
  Component getHeadComponent();

  @Nullable
  String getHeadComponentKey();

  @Nullable
  Component getTailComponent();

  @Nullable
  String getTailComponentKey();

  @Nullable
  List<Attachable> getAttachables();

  @Nullable
  List<Component> getUnresolvedComponents();

  @Nullable
  Map<String, Component> getComponentsNeedingPreviousRenderData();

  @Nullable
  ArrayList<WorkingRangeContainer.Registration> getWorkingRangeRegistrations();

  /* Visibility related APIs */

  boolean hasVisibilityHandlers();

  @Nullable
  EventHandler<VisibleEvent> getVisibleHandler();

  @Nullable
  EventHandler<InvisibleEvent> getInvisibleHandler();

  @Nullable
  EventHandler<FocusedVisibleEvent> getFocusedHandler();

  @Nullable
  EventHandler<UnfocusedVisibleEvent> getUnfocusedHandler();

  @Nullable
  EventHandler<VisibilityChangedEvent> getVisibilityChangedHandler();

  @Nullable
  EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler();

  float getVisibleHeightRatio();

  float getVisibleWidthRatio();

  /* Transitions related APIs */

  boolean hasTransitionKey();

  @Nullable
  String getTransitionKey();

  @Nullable
  String getTransitionOwnerKey();

  @Nullable
  String getTransitionGlobalKey();

  @Nullable
  Transition.TransitionKeyType getTransitionKeyType();

  @Nullable
  ArrayList<Transition> getTransitions();

  /* Output related APIs */

  @Nullable
  NodeInfo getNodeInfo();

  @Nullable
  Drawable getBackground();

  @Nullable
  Drawable getForeground();

  boolean hasStateListAnimatorResSet();

  @Nullable
  StateListAnimator getStateListAnimator();

  @DrawableRes
  int getStateListAnimatorRes();

  boolean hasBorderColor();

  int[] getBorderColors();

  float[] getBorderRadius();

  @Nullable
  PathEffect getBorderPathEffect();

  boolean hasTouchExpansion();

  @Nullable
  Edges getTouchExpansion();

  boolean isDuplicateParentStateEnabled();

  boolean isDuplicateChildrenStatesEnabled();

  boolean isForceViewWrapping();

  boolean isImportantForAccessibilityIsSet();

  int getImportantForAccessibility();

  @LayerType
  int getLayerType();

  @Nullable
  Paint getLayerPaint();

  /* Layout and measurement related APIs */

  void layoutDirection(YogaDirection direction);

  boolean isLayoutDirectionInherit();

  void setNestedPadding(@Nullable Edges padding, @Nullable boolean[] isPercentage);

  /* Testing a nd debugging related APIs */

  LayoutProps getDebugLayoutEditor();

  /**
   * A unique identifier which may be set for retrieving a component and its bounds when testing.
   */
  @Nullable
  String getTestKey();

  void registerDebugComponent(DebugComponent debugComponent);

  @Deprecated
  boolean implementsLayoutDiffing();

  /**
   * The API for the nested tree holder, which is used to hold the partial results of an unresolved
   * node which will be resolved after layout calculation for components which implement {@link
   * OnCreateLayoutWithSizeSpec}.
   */
  interface NestedTreeHolder extends InternalNode, Copyable<InternalNode> {

    /**
     * The {@link TreeProps} to to be set into the new {@link ComponentContext} before resolving the
     * nested tree.
     */
    @Nullable
    TreeProps getPendingTreeProps();
  }

  @IntDef({
    ReconciliationMode.REUSE,
    ReconciliationMode.COPY,
    ReconciliationMode.RECONCILE,
    ReconciliationMode.RECREATE
  })
  @interface ReconciliationMode {
    /** Used only for InternalNode's without satatless components */
    @Deprecated int COPY = 0;

    int RECONCILE = 1;
    int RECREATE = 2;
    int REUSE = 3;
  }
}
