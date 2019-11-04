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
import android.graphics.PathEffect;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Px;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.drawable.ComparableDrawable;
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
import javax.annotation.Nullable;

/** Internal class representing a {@link ComponentLayout}. */
@ThreadConfined(ThreadConfined.ANY)
public interface InternalNode extends ComponentLayout, LayoutProps, Copyable<InternalNode> {

  void addChildAt(InternalNode child, int index);

  void addComponentNeedingPreviousRenderData(Component component);

  void addTransition(Transition transition);

  void addWorkingRanges(List<WorkingRangeContainer.Registration> registrations);

  InternalNode alignContent(YogaAlign alignContent);

  InternalNode alignItems(YogaAlign alignItems);

  void appendComponent(Component component);

  void appendUnresolvedComponent(Component component);

  boolean areCachedMeasuresValid();

  InternalNode background(@Nullable ComparableDrawable background);

  /**
   * @deprecated use {@link #background(ComparableDrawable)} more efficient diffing of drawables.
   */
  @Deprecated
  InternalNode background(@Nullable Drawable background);

  InternalNode backgroundColor(@ColorInt int backgroundColor);

  InternalNode backgroundRes(@DrawableRes int resId);

  InternalNode border(Border border);

  void border(Edges width, int[] colors, float[] radii);

  void calculateLayout(float width, float height);

  void calculateLayout();

  InternalNode child(Component child);

  InternalNode child(Component.Builder<?> child);

  InternalNode child(InternalNode child);

  InternalNode duplicateParentState(boolean duplicateParentState);

  /** Used by stetho to re-set auto value */
  InternalNode flexBasisAuto();

  InternalNode flexDirection(YogaFlexDirection direction);

  InternalNode focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler);

  /**
   * @deprecated use {@link #foreground(ComparableDrawable)} more efficient diffing of drawables.
   */
  @Deprecated
  InternalNode foreground(@Nullable Drawable foreground);

  InternalNode foreground(@Nullable ComparableDrawable foreground);

  InternalNode foregroundColor(@ColorInt int foregroundColor);

  InternalNode foregroundRes(@DrawableRes int resId);

  InternalNode fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler);

  int[] getBorderColors();

  @Nullable
  PathEffect getBorderPathEffect();

  float[] getBorderRadius();

  @Nullable
  InternalNode getChildAt(int index);

  int getChildCount();

  int getChildIndex(InternalNode child);

  /**
   * Return the list of components contributing to this InternalNode. This exists in both debug and
   * production mode.
   */
  List<Component> getComponents();

  @Nullable
  List<Component> getUnresolvedComponents();

  @Nullable
  ArrayList<Component> getComponentsNeedingPreviousRenderData();

  ComponentContext getContext();

  @Nullable
  DiffNode getDiffNode();

  void setDiffNode(@Nullable DiffNode diffNode);

  @Nullable
  EventHandler<FocusedVisibleEvent> getFocusedHandler();

  @Nullable
  ComparableDrawable getForeground();

  @Nullable
  EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler();

  @Nullable
  Component getHeadComponent();

  int getImportantForAccessibility();

  @Nullable
  EventHandler<InvisibleEvent> getInvisibleHandler();

  int getLastHeightSpec();

  void setLastHeightSpec(int heightSpec);

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * height. This is used together with {@link InternalNode#getLastHeightSpec()} to implement
   * measure caching.
   */
  float getLastMeasuredHeight();

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the height.
   */
  void setLastMeasuredHeight(float lastMeasuredHeight);

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * width. This is used together with {@link InternalNode#getLastWidthSpec()} to implement measure
   * caching.
   */
  float getLastMeasuredWidth();

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the width.
   */
  void setLastMeasuredWidth(float lastMeasuredWidth);

  int getLastWidthSpec();

  void setLastWidthSpec(int widthSpec);

  int getLayoutBorder(YogaEdge edge);

  float getMaxHeight();

  float getMaxWidth();

  float getMinHeight();

  float getMinWidth();

  @Nullable
  InternalNode getNestedTree();

  /**
   * Set the nested tree before measuring it in order to transfer over important information such as
   * layout direction needed during measurement.
   */
  void setNestedTree(InternalNode nestedTree);

  @Nullable
  InternalNode getNestedTreeHolder();

  @Nullable
  NodeInfo getNodeInfo();

  void setNodeInfo(NodeInfo nodeInfo);

  NestedTreeProps getOrCreateNestedTreeProps();

  NodeInfo getOrCreateNodeInfo();

  @Nullable
  InternalNode getParent();

  @Nullable
  TreeProps getPendingTreeProps();

  @Nullable
  Component getTailComponent();

  void setRootComponent(Component component);

  @Nullable
  StateListAnimator getStateListAnimator();

  @DrawableRes
  int getStateListAnimatorRes();

  YogaDirection getStyleDirection();

  float getStyleHeight();

  float getStyleWidth();

  /**
   * A unique identifier which may be set for retrieving a component and its bounds when testing.
   */
  @Nullable
  String getTestKey();

  @Nullable
  Edges getTouchExpansion();

  int getTouchExpansionBottom();

  int getTouchExpansionLeft();

  int getTouchExpansionRight();

  int getTouchExpansionTop();

  @Nullable
  String getTransitionKey();

  @Nullable
  String getTransitionOwnerKey();

  @Nullable
  Transition.TransitionKeyType getTransitionKeyType();

  @Nullable
  ArrayList<Transition> getTransitions();

  @Nullable
  EventHandler<UnfocusedVisibleEvent> getUnfocusedHandler();

  @Nullable
  EventHandler<VisibilityChangedEvent> getVisibilityChangedHandler();

  @Nullable
  EventHandler<VisibleEvent> getVisibleHandler();

  float getVisibleHeightRatio();

  float getVisibleWidthRatio();

  @Nullable
  ArrayList<WorkingRangeContainer.Registration> getWorkingRangeRegistrations();

  YogaNode getYogaNode();

  boolean hasBorderColor();

  boolean hasNestedTree();

  boolean hasNewLayout();

  boolean hasStateListAnimatorResSet();

  boolean hasTouchExpansion();

  boolean hasTransitionKey();

  boolean hasVisibilityHandlers();

  /** Used by stetho to re-set auto value */
  InternalNode heightAuto();

  InternalNode importantForAccessibility(int importantForAccessibility);

  InternalNode invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler);

  boolean isDuplicateParentStateEnabled();

  boolean isForceViewWrapping();

  boolean isImportantForAccessibilityIsSet();

  /**
   * For testing and debugging purposes only where initialization may have not occurred. For any
   * production use, this should never be necessary.
   */
  boolean isInitialized();

  boolean isLayoutDirectionInherit();

  /**
   * @return Whether this node is holding a nested tree or not. The decision was made during tree
   *     creation {@link LayoutState#createLayout(ComponentContext, Component, boolean)}.
   */
  boolean isNestedTreeHolder();

  InternalNode justifyContent(YogaJustify justifyContent);

  /** Mark this node as a nested tree root holder. */
  void markIsNestedTreeHolder(@Nullable TreeProps currentTreeProps);

  void markLayoutSeen();

  /** Continually walks the node hierarchy until a node returns a non inherited layout direction */
  YogaDirection recursivelyResolveLayoutDirection();

  void registerDebugComponent(DebugComponent debugComponent);

  InternalNode removeChildAt(int index);

  /** This method marks all resolved layout property values to undefined. */
  void resetResolvedLayoutProperties();

  void setBorderWidth(YogaEdge edge, @Px int borderWidth);

  void setCachedMeasuresValid(boolean valid);

  void setMeasureFunction(YogaMeasureFunction measureFunction);

  void setStyleHeightFromSpec(int heightSpec);

  void setStyleWidthFromSpec(int widthSpec);

  boolean shouldDrawBorders();

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

  // Used by stetho to re-set auto value
  InternalNode widthAuto();

  InternalNode wrap(YogaWrap wrap);

  InternalNode wrapInView();

  void applyAttributes(TypedArray a);

  void assertContextSpecificStyleNotSet();

  InternalNode deepClone();

  String getSimpleName();

  /**
   * Reconcile returns a new InternalNode tree where only mutated sub-trees are recreated and all
   * other sub-trees are copied. The returned InternalNode tree represents the updated layout tree.
   *
   * @param c The new ComponentContext.
   * @param next The new component to reconcile against.
   * @return The reconciled InternalNode which represents {@param next}.
   */
  InternalNode reconcile(ComponentContext c, Component next);

  class NestedTreeProps {
    boolean mIsNestedTreeHolder;
    @Nullable InternalNode mNestedTree;
    @Nullable InternalNode mNestedTreeHolder;
    @Nullable Edges mNestedTreePadding;
    @Nullable Edges mNestedTreeBorderWidth;
    @Nullable TreeProps mPendingTreeProps;
  }
}
