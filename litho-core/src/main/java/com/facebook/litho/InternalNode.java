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
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.Px;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNode.Inputs;
import com.facebook.yoga.YogaWrap;
import java.util.List;
import javax.annotation.Nullable;

/** Internal class representing a {@link ComponentLayout}. */
@ThreadConfined(ThreadConfined.ANY)
public interface InternalNode extends Inputs, LithoLayoutResult, LayoutProps {

  void addChildAt(InternalNode child, int index);

  void addComponentNeedingPreviousRenderData(String globalKey, Component component);

  void addTransition(Transition transition);

  void addWorkingRanges(List<WorkingRangeContainer.Registration> registrations);

  void addAttachable(Attachable attachable);

  InternalNode alignContent(YogaAlign alignContent);

  InternalNode alignItems(YogaAlign alignItems);

  void appendComponent(Component component, String key);

  void appendUnresolvedComponent(Component component);

  InternalNode background(@Nullable Drawable background);

  InternalNode backgroundColor(@ColorInt int backgroundColor);

  InternalNode backgroundRes(@DrawableRes int resId);

  InternalNode border(Border border);

  void border(Edges width, int[] colors, float[] radii);

  LithoLayoutResult calculateLayout(float width, float height);

  void calculateLayout();

  InternalNode child(Component child);

  InternalNode child(Component.Builder<?> child);

  InternalNode child(InternalNode child);

  InternalNode duplicateParentState(boolean duplicateParentState);

  InternalNode duplicateChildrenStates(boolean duplicateChildState);

  /** Used by stetho to re-set auto value */
  InternalNode flexBasisAuto();

  InternalNode flexDirection(YogaFlexDirection direction);

  InternalNode focusedHandler(@Nullable EventHandler<FocusedVisibleEvent> focusedHandler);

  InternalNode foreground(@Nullable Drawable foreground);

  InternalNode foregroundColor(@ColorInt int foregroundColor);

  InternalNode foregroundRes(@DrawableRes int resId);

  InternalNode layerType(@LayerType int layoutType, Paint layerPaint);

  InternalNode fullImpressionHandler(
      @Nullable EventHandler<FullImpressionVisibleEvent> fullImpressionHandler);

  void setDiffNode(@Nullable DiffNode diffNode);

  void setNodeInfo(NodeInfo nodeInfo);

  NodeInfo getOrCreateNodeInfo();

  /** Used by stetho to re-set auto value */
  InternalNode heightAuto();

  InternalNode importantForAccessibility(int importantForAccessibility);

  InternalNode invisibleHandler(@Nullable EventHandler<InvisibleEvent> invisibleHandler);

  InternalNode justifyContent(YogaJustify justifyContent);

  void registerDebugComponent(DebugComponent debugComponent);

  InternalNode removeChildAt(int index);

  /** This method marks all resolved layout property values to undefined. */
  void resetResolvedLayoutProperties();

  void setBorderWidth(YogaEdge edge, @Px int borderWidth);

  void setCachedMeasuresValid(boolean valid);

  void setMeasureFunction(YogaMeasureFunction measureFunction);

  void setStyleHeightFromSpec(int heightSpec);

  void setStyleWidthFromSpec(int widthSpec);

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

  InternalNode deepClone();

  /**
   * Reconcile returns a new InternalNode tree where only mutated sub-trees are recreated and all
   * other sub-trees are copied. The returned InternalNode tree represents the updated layout tree.
   *
   * @param layoutStateContext
   * @param c The new ComponentContext.
   * @param next The new component to reconcile against.
   * @return The reconciled InternalNode which represents {@param next}.
   */
  InternalNode reconcile(ComponentContext c, Component next, @Nullable String nextKey);

  /**
   * The API for the nested tree holder, which is used to hold the partial results of an unresolved
   * node which will be resolved after layout calculation for components which implement {@link
   * OnCreateLayoutWithSizeSpec}.
   */
  interface NestedTreeHolder extends InternalNode, Copyable<InternalNode> {}
}
