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
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import com.facebook.litho.InternalNode.NestedTreeHolder;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** The {@link LayoutResult} class for Litho */
public interface LithoLayoutResult extends ComponentLayout {

  /* InternalNode related APIs */

  ComponentContext getContext();

  YogaNode getYogaNode();

  String getSimpleName();

  @Nullable
  InternalNode getParent();

  @Nullable
  InternalNode getChildAt(int index);

  int getChildCount();

  int getChildIndex(InternalNode child);

  /**
   * For testing and debugging purposes only where initialization may have not occurred. For any
   * production use, this should never be necessary.
   */
  boolean isInitialized();

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
  @Nullable
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
  Drawable getForeground();

  boolean hasStateListAnimatorResSet();

  @Nullable
  StateListAnimator getStateListAnimator();

  @DrawableRes
  int getStateListAnimatorRes();

  boolean shouldDrawBorders();

  boolean hasBorderColor();

  int[] getBorderColors();

  float[] getBorderRadius();

  int getLayoutBorder(YogaEdge edge);

  @Nullable
  PathEffect getBorderPathEffect();

  boolean hasTouchExpansion();

  int getTouchExpansionBottom();

  int getTouchExpansionLeft();

  int getTouchExpansionRight();

  int getTouchExpansionTop();

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

  float getMaxHeight();

  float getMaxWidth();

  float getMinHeight();

  float getMinWidth();

  float getStyleHeight();

  float getStyleWidth();

  boolean isLayoutDirectionInherit();

  YogaDirection getStyleDirection();

  /** Continually walks the node hierarchy until a node returns a non inherited layout direction */
  YogaDirection recursivelyResolveLayoutDirection();

  boolean areCachedMeasuresValid();

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * height. This is used together with {@link InternalNode#getLastHeightSpec()} to implement
   * measure caching.
   */
  float getLastMeasuredHeight();

  /**
   * The last value the measure funcion associated with this node {@link Component} returned for the
   * width. This is used together with {@link InternalNode#getLastWidthSpec()} to implement measure
   * caching.
   */
  float getLastMeasuredWidth();

  int getLastHeightSpec();

  int getLastWidthSpec();

  @Nullable
  DiffNode getDiffNode();

  void assertContextSpecificStyleNotSet();

  /* Test related APIs */

  /**
   * A unique identifier which may be set for retrieving a component and its bounds when testing.
   */
  @Nullable
  String getTestKey();

  /* Measurement related APIs for mutating the result */

  void setLastWidthSpec(int widthSpec);

  void setLastHeightSpec(int heightSpec);

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the height.
   */
  void setLastMeasuredHeight(float lastMeasuredHeight);

  /**
   * Sets the last value the measure funcion associated with this node {@link Component} returned
   * for the width.
   */
  void setLastMeasuredWidth(float lastMeasuredWidth);

  /** Holds the {@link LithoLayoutResult} for {@link NestedTreeHolder} */
  interface NestedTreeHolderResult extends LithoLayoutResult, Copyable<InternalNode> {

    /**
     * The {@link TreeProps} to to be set into the new {@link ComponentContext} before resolving the
     * nested tree.
     */
    @Nullable
    TreeProps getPendingTreeProps();

    @Nullable
    LithoLayoutResult getNestedResult();

    void setNestedResult(@Nullable LithoLayoutResult tree);
  }
}
