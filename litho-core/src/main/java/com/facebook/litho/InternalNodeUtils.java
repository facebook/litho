/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.Component.MountType.NONE;
import static com.facebook.litho.Component.isMountable;
import static com.facebook.litho.Component.isPrimitive;
import static com.facebook.litho.LithoLayoutResult.willMountView;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_DISABLE_TOUCHABLE;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_HAS_TOUCH_EVENT_HANDLERS;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
import static com.facebook.litho.NodeInfo.CLICKABLE_SET_TRUE;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.SparseArray;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.drawable.BorderColorDrawable;
import com.facebook.rendercore.Mountable;
import com.facebook.rendercore.primitives.Primitive;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import java.util.List;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class InternalNodeUtils {

  /** Creates a {@link LithoRenderUnit} for the content output iff the result mounts content. */
  static @Nullable LithoRenderUnit createContentRenderUnit(
      LithoLayoutResult result, final LayoutState layoutState) {
    final LithoNode node = result.getNode();
    final Component component = node.getTailComponent();

    if (component.getMountType() == NONE) {
      return null;
    }

    final String componentKey = node.getTailComponentKey();
    final ComponentContext context = result.getContext();
    final @Nullable DiffNode diffNode = result.getDiffNode();
    long previousId = -1;

    if (diffNode != null) {
      final LithoRenderUnit contentOutput = diffNode.getContentOutput();
      if (contentOutput != null) {
        previousId = contentOutput.getId();
      }
    }

    final long id = context.calculateLayoutOutputId(componentKey, OutputUnitType.CONTENT);

    return createRenderUnit(
        id,
        component,
        context,
        layoutState,
        result,
        node,
        node.getImportantForAccessibility(),
        previousId != id
            ? LithoRenderUnit.STATE_UNKNOWN
            : result.areCachedMeasuresValid()
                ? LithoRenderUnit.STATE_UPDATED
                : LithoRenderUnit.STATE_DIRTY,
        layoutState.getCurrentShouldDuplicateParentState(),
        false,
        needsHostView(result, layoutState),
        willMountView(result));
  }

  /** Creates a {@link LithoRenderUnit} for the host output iff the result needs a host view. */
  static @Nullable LithoRenderUnit createHostRenderUnit(
      LithoLayoutResult result, final LayoutState layoutState, final boolean isRoot) {
    final LithoNode node = result.getNode();

    if (!isRoot && !needsHostView(result, layoutState)) {
      return null;
    }

    final HostComponent hostComponent = HostComponent.create();

    // We need to pass common dynamic props to the host component, as they only could be applied to
    // views, so we'll need to set them up, when binding HostComponent to ComponentHost. At the same
    // time, we don't remove them from the current component, as we may calculate multiple
    // LayoutStates using same Components
    hostComponent.setCommonDynamicProps(mergeCommonDynamicProps(node.getScopedComponentInfos()));

    final long id;
    final @LithoRenderUnit.UpdateState int updateState;
    if (isRoot) {
      // The root host (LithoView) always has ID 0 and is unconditionally
      // set as dirty i.e. no need to use shouldComponentUpdate().
      id = ROOT_HOST_ID;
      updateState = LithoRenderUnit.STATE_DIRTY;
    } else {
      id =
          result
              .getContext()
              .calculateLayoutOutputId(node.getTailComponentKey(), OutputUnitType.HOST);

      updateState = LithoRenderUnit.STATE_UNKNOWN;
    }

    return createRenderUnit(
        id,
        hostComponent,
        null,
        layoutState,
        result,
        node,
        node.getImportantForAccessibility(),
        updateState,
        node.isDuplicateParentStateEnabled(),
        node.isDuplicateChildrenStatesEnabled(),
        false,
        true);
  }

  /**
   * Creates a {@link LithoRenderUnit} for the background output iff the result has a background.
   */
  static @Nullable LithoRenderUnit createBackgroundRenderUnit(
      LithoLayoutResult result, final LayoutState layoutState) {
    final Drawable background = result.getBackground();

    // Only create a background output when the component does not mount a View because
    // the background will get set in the output of the component.
    if (background != null && !willMountView(result)) {
      return createDrawableRenderUnit(result, background, OutputUnitType.BACKGROUND, layoutState);
    }

    return null;
  }

  /**
   * Creates a {@link LithoRenderUnit} for the foreground output iff the result has a foreground.
   */
  static @Nullable LithoRenderUnit createForegroundRenderUnit(
      LithoLayoutResult result, final LayoutState layoutState) {
    final LithoNode node = result.getNode();
    final Drawable foreground = node.getForeground();

    /// Only create a foreground output when the component does not mount a View because
    // the foreground has already been set in the output of the component.
    if (foreground != null && (!willMountView(result) || SDK_INT < M)) {
      return createDrawableRenderUnit(result, foreground, OutputUnitType.FOREGROUND, layoutState);
    }

    return null;
  }

  /** Creates a {@link LithoRenderUnit} for the border output iff the result has borders. */
  static @Nullable LithoRenderUnit createBorderRenderUnit(
      LithoLayoutResult result, final LayoutState layoutState) {
    if (result.shouldDrawBorders()) {
      final Drawable border = getBorderColorDrawable(result);
      return createDrawableRenderUnit(result, border, OutputUnitType.BORDER, layoutState);
    }

    return null;
  }

  /**
   * Common method to create the {@link LithoRenderUnit} for backgrounds, foregrounds, and border.
   * The method uses the {@param outputType} to decide between the options. This method will call
   * the shouldupdate, and {@link SpecGeneratedComponent#onBoundsDefined(ComponentContext,
   * ComponentLayout, InterStagePropsContainer)} for the {@link DrawableComponent}.
   */
  static LithoRenderUnit createDrawableRenderUnit(
      final LithoLayoutResult result,
      final Drawable drawable,
      final @OutputUnitType int outputType,
      final LayoutState layoutState) {

    final Component component = DrawableComponent.create(drawable);
    final LithoNode node = result.getNode();
    final String componentKey = node.getTailComponentKey();
    final @Nullable DiffNode diffNode = result.getDiffNode();

    final @Nullable LithoRenderUnit recycle;

    if (diffNode != null) {
      switch (outputType) {
        case OutputUnitType.BACKGROUND:
          recycle = diffNode.getBackgroundOutput();
          break;
        case OutputUnitType.FOREGROUND:
          recycle = diffNode.getForegroundOutput();
          break;
        case OutputUnitType.BORDER:
          recycle = diffNode.getBorderOutput();
          break;
        case OutputUnitType.CONTENT:
        case OutputUnitType.HOST:
        default:
          throw new IllegalArgumentException("OutputUnitType " + outputType + " not supported");
      }
    } else {
      recycle = null;
    }

    boolean isCachedOutputUpdated;
    if (recycle != null) {
      try {
        isCachedOutputUpdated =
            !component.shouldComponentUpdate(null, recycle.getComponent(), null, component);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(result.getContext(), component, e);
        isCachedOutputUpdated = false;
      }
    } else {
      isCachedOutputUpdated = false;
    }

    final long previousId = recycle != null ? recycle.getId() : -1;
    final long id = result.getContext().calculateLayoutOutputId(componentKey, outputType);

    /* Call onBoundsDefined for the DrawableComponent */
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("onBoundsDefined:" + component.getSimpleName());
    }

    try {
      if (component instanceof SpecGeneratedComponent) {
        ((SpecGeneratedComponent) component).onBoundsDefined(result.getContext(), result, null);
      }
    } catch (Exception e) {
      ComponentUtils.handleWithHierarchy(result.getContext(), component, e);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    return createRenderUnit(
        id,
        component,
        null,
        layoutState,
        result,
        node,
        IMPORTANT_FOR_ACCESSIBILITY_NO,
        previousId != id
            ? LithoRenderUnit.STATE_UNKNOWN
            : isCachedOutputUpdated ? LithoRenderUnit.STATE_UPDATED : LithoRenderUnit.STATE_DIRTY,
        layoutState.getCurrentShouldDuplicateParentState(),
        false,
        needsHostView(result, layoutState),
        false);
  }

  /** Generic method to create a {@link LithoRenderUnit}. */
  static LithoRenderUnit createRenderUnit(
      long id,
      Component component,
      @Nullable ComponentContext context,
      LayoutState layoutState,
      LithoLayoutResult result,
      LithoNode node,
      int importantForAccessibility,
      @LithoRenderUnit.UpdateState int updateState,
      boolean duplicateParentState,
      boolean duplicateChildrenStates,
      boolean hasHostView,
      boolean isMountViewSpec) {

    int flags = 0;

    final NodeInfo layoutOutputNodeInfo;

    final NodeInfo nodeInfo = node.getNodeInfo();

    // View mount specs are able to set their own attributes when they're mounted.
    // Non-view specs (drawable and layout) always transfer their view attributes
    // to their respective hosts.
    // Moreover, if the component mounts a view, then we apply padding to the view itself later on.
    // Otherwise, apply the padding to the bounds of the layout output.
    if (isMountViewSpec) {
      layoutOutputNodeInfo = nodeInfo;
    } else {
      if (nodeInfo != null && nodeInfo.getEnabledState() == ENABLED_SET_FALSE) {
        flags |= LAYOUT_FLAG_DISABLE_TOUCHABLE;
      }
      layoutOutputNodeInfo = null;
    }

    if (duplicateParentState) {
      flags |= LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
    }

    if (duplicateChildrenStates) {
      flags |= LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES;
    }

    if (hasHostView) {
      flags |= LAYOUT_FLAG_MATCH_HOST_BOUNDS;
    }

    if (layoutState.mShouldDisableDrawableOutputs) {
      flags |= LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED;
    }
    if (nodeInfo != null && nodeInfo.hasTouchEventHandlers()) {
      flags |= LAYOUT_FLAG_HAS_TOUCH_EVENT_HANDLERS;
    }

    Mountable<?> mountable = node.getMountable();
    Rect touchBoundsExpansion = getExpandedTouchBounds(result);
    if (mountable != null && isMountable(component)) {
      return MountableLithoRenderUnit.create(
          component,
          context,
          layoutOutputNodeInfo,
          touchBoundsExpansion,
          flags,
          importantForAccessibility,
          updateState,
          mountable);
    }

    Primitive<?> primitive = node.getPrimitive();
    if (primitive != null && isPrimitive(component)) {
      return PrimitiveLithoRenderUnit.create(
          component,
          context,
          layoutOutputNodeInfo,
          touchBoundsExpansion,
          flags,
          importantForAccessibility,
          updateState,
          primitive.getRenderUnit());
    }

    return MountSpecLithoRenderUnit.create(
        id,
        component,
        context,
        layoutOutputNodeInfo,
        touchBoundsExpansion,
        flags,
        importantForAccessibility,
        updateState);
  }

  @Nullable
  private static Rect getExpandedTouchBounds(final LithoLayoutResult result) {
    if (!result.getNode().hasTouchExpansion()) {
      return null;
    }

    final int left = result.getTouchExpansionLeft();
    final int top = result.getTouchExpansionTop();
    final int right = result.getTouchExpansionRight();
    final int bottom = result.getTouchExpansionBottom();
    if (left == 0 && top == 0 && right == 0 && bottom == 0) {
      return null;
    }

    return new Rect(left, top, right, bottom);
  }

  private static SparseArray<DynamicValue<?>> mergeCommonDynamicProps(
      List<ScopedComponentInfo> infos) {
    final SparseArray<DynamicValue<?>> mergedDynamicProps = new SparseArray<>();
    for (ScopedComponentInfo info : infos) {
      final SparseArray<DynamicValue<?>> commonDynamicProps =
          info.getComponent().getCommonDynamicProps();
      if (commonDynamicProps == null) {
        continue;
      }
      for (int i = 0; i < commonDynamicProps.size(); i++) {
        final int key = commonDynamicProps.keyAt(i);
        final DynamicValue<?> commonDynamicProp = commonDynamicProps.get(key);
        if (commonDynamicProp != null) {
          mergedDynamicProps.append(key, commonDynamicProp);
        }
      }
    }

    return mergedDynamicProps;
  }

  private static Drawable getBorderColorDrawable(LithoLayoutResult result) {
    if (!result.shouldDrawBorders()) {
      throw new RuntimeException("This result does not support drawing border color");
    }

    final LithoNode node = result.getNode();
    final boolean isRtl = result.recursivelyResolveLayoutDirection() == YogaDirection.RTL;
    final float[] borderRadius = node.getBorderRadius();
    final int[] borderColors = node.getBorderColors();
    final YogaEdge leftEdge = isRtl ? YogaEdge.RIGHT : YogaEdge.LEFT;
    final YogaEdge rightEdge = isRtl ? YogaEdge.LEFT : YogaEdge.RIGHT;

    return new BorderColorDrawable.Builder()
        .pathEffect(node.getBorderPathEffect())
        .borderLeftColor(Border.getEdgeColor(borderColors, leftEdge))
        .borderTopColor(Border.getEdgeColor(borderColors, YogaEdge.TOP))
        .borderRightColor(Border.getEdgeColor(borderColors, rightEdge))
        .borderBottomColor(Border.getEdgeColor(borderColors, YogaEdge.BOTTOM))
        .borderLeftWidth(result.getLayoutBorder(leftEdge))
        .borderTopWidth(result.getLayoutBorder(YogaEdge.TOP))
        .borderRightWidth(result.getLayoutBorder(rightEdge))
        .borderBottomWidth(result.getLayoutBorder(YogaEdge.BOTTOM))
        .borderRadius(borderRadius)
        .build();
  }

  /**
   * Returns true if this is the root node (which always generates a matching layout output), if the
   * node has view attributes e.g. tags, content description, etc, or if the node has explicitly
   * been forced to be wrapped in a view.
   */
  static boolean needsHostView(final LithoLayoutResult result, final LayoutState layoutState) {
    final LithoNode node = result.getNode();

    if (willMountView(result)) {
      // Component already represents a View.
      return false;
    }

    if (node.isForceViewWrapping()) {
      // Wrapping into a View requested.
      return true;
    }

    if (hasViewContent(node, layoutState)) {
      // Has View content (e.g. Accessibility content, Focus change listener, shadow, view tag etc)
      // thus needs a host View.
      return true;
    }

    if (needsHostViewForCommonDynamicProps(node)) {
      return true;
    }

    if (needsHostViewForTransition(result)) {
      return true;
    }

    if (hasSelectedStateWhenDisablingDrawableOutputs(layoutState, result)) {
      return true;
    }

    return false;
  }

  /**
   * Determine if a given {@link LithoNode} within the context of a given {@link LayoutState}
   * requires to be wrapped inside a view.
   *
   * @see #needsHostView(LithoLayoutResult, LayoutState)
   */
  private static boolean hasViewContent(final LithoNode node, final LayoutState layoutState) {
    final Component component = node.getTailComponent();
    final NodeInfo nodeInfo = node.getNodeInfo();

    final boolean implementsAccessibility =
        (nodeInfo != null && nodeInfo.needsAccessibilityDelegate())
            || (component instanceof SpecGeneratedComponent
                && ((SpecGeneratedComponent) component).implementsAccessibility());

    final int importantForAccessibility = node.getImportantForAccessibility();

    // A component has accessibility content if:
    //   1. Accessibility is currently enabled.
    //   2. Accessibility hasn't been explicitly disabled on it
    //      i.e. IMPORTANT_FOR_ACCESSIBILITY_NO.
    //   3. Any of these conditions are true:
    //      - It implements accessibility support.
    //      - It has a content description.
    //      - It has importantForAccessibility set as either IMPORTANT_FOR_ACCESSIBILITY_YES
    //        or IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS.
    // IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS should trigger an inner host
    // so that such flag is applied in the resulting view hierarchy after the component
    // tree is mounted. Click handling is also considered accessibility content but
    // this is already covered separately i.e. click handler is not null.
    final boolean hasBackgroundOrForeground =
        layoutState.mShouldDisableDrawableOutputs
            && (node.getBackground() != null || node.getForeground() != null);
    final boolean hasAccessibilityContent =
        layoutState.isAccessibilityEnabled()
            && importantForAccessibility != IMPORTANT_FOR_ACCESSIBILITY_NO
            && (implementsAccessibility
                || (nodeInfo != null && !TextUtils.isEmpty(nodeInfo.getContentDescription()))
                || importantForAccessibility != IMPORTANT_FOR_ACCESSIBILITY_AUTO);

    return hasBackgroundOrForeground
        || hasAccessibilityContent
        || node.isDuplicateChildrenStatesEnabled()
        || hasViewAttributes(nodeInfo)
        || node.getLayerType() != LayerType.LAYER_TYPE_NOT_SET;
  }

  static boolean hasViewAttributes(@Nullable NodeInfo nodeInfo) {
    if (nodeInfo == null) {
      return false;
    }

    final boolean hasFocusChangeHandler = nodeInfo.hasFocusChangeHandler();
    final boolean hasEnabledTouchEventHandlers =
        nodeInfo.hasTouchEventHandlers() && nodeInfo.getEnabledState() != ENABLED_SET_FALSE;
    final boolean hasViewId = nodeInfo.hasViewId();
    final boolean hasViewTag = nodeInfo.getViewTag() != null;
    final boolean hasViewTags = nodeInfo.getViewTags() != null;
    final boolean hasShadowElevation = nodeInfo.getShadowElevation() != 0;
    final boolean hasAmbientShadowColor = nodeInfo.getAmbientShadowColor() != Color.BLACK;
    final boolean hasSpotShadowColor = nodeInfo.getSpotShadowColor() != Color.BLACK;
    final boolean hasOutlineProvider = nodeInfo.getOutlineProvider() != null;
    final boolean hasClipToOutline = nodeInfo.getClipToOutline();
    final boolean isFocusableSetTrue = nodeInfo.getFocusState() == FOCUS_SET_TRUE;
    final boolean isClickableSetTrue = nodeInfo.getClickableState() == CLICKABLE_SET_TRUE;
    final boolean hasClipChildrenSet = nodeInfo.isClipChildrenSet();
    final boolean hasTransitionName = nodeInfo.getTransitionName() != null;

    return hasFocusChangeHandler
        || hasEnabledTouchEventHandlers
        || hasViewId
        || hasViewTag
        || hasViewTags
        || hasShadowElevation
        || hasAmbientShadowColor
        || hasSpotShadowColor
        || hasOutlineProvider
        || hasClipToOutline
        || hasClipChildrenSet
        || isFocusableSetTrue
        || isClickableSetTrue
        || hasTransitionName;
  }

  /**
   * Similar to {@link InternalNodeUtils#needsHostView(LithoLayoutResult, LayoutState)} but without
   * dependency to {@link LayoutState} instance. This will be used for debugging tools to indicate
   * whether the mountable output is a wrapped View or View MountSpec. Unlike {@link
   * InternalNodeUtils#needsHostView(LithoLayoutResult, LayoutState)} this does not consider
   * accessibility also does not consider root component, but this approximation is good enough for
   * debugging purposes.
   */
  static boolean hasViewOutput(LithoLayoutResult result) {
    final LithoNode node = result.getNode();
    return node.isForceViewWrapping()
        || willMountView(result)
        || InternalNodeUtils.hasViewAttributes(node.getNodeInfo())
        || InternalNodeUtils.needsHostViewForCommonDynamicProps(node)
        || InternalNodeUtils.needsHostViewForTransition(result);
  }

  private static boolean hasSelectedStateWhenDisablingDrawableOutputs(
      final LayoutState layoutState, final LithoLayoutResult result) {
    final LithoNode node = result.getNode();
    return layoutState.mShouldAddHostViewForRootComponent
        && !willMountView(result)
        && node.getNodeInfo() != null
        && node.getNodeInfo().getSelectedState() != NodeInfo.SELECTED_UNSET;
  }

  static boolean needsHostViewForCommonDynamicProps(final LithoNode node) {
    final List<ScopedComponentInfo> infos = node.getScopedComponentInfos();
    for (ScopedComponentInfo info : infos) {
      if (info != null && info.getComponent().hasCommonDynamicProps()) {
        // Need a host View to apply the dynamic props to
        return true;
      }
    }
    return false;
  }

  static boolean needsHostViewForTransition(final LithoLayoutResult result) {
    final LithoNode node = result.getNode();
    return !TextUtils.isEmpty(node.getTransitionKey()) && !willMountView(result);
  }
}
