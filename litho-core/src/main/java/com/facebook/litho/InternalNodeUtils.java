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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.Component.MountType.NONE;
import static com.facebook.litho.Component.isHostSpec;
import static com.facebook.litho.Component.isMountViewSpec;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DISABLE_TOUCHABLE;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.LayoutOutput.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
import static com.facebook.litho.NodeInfo.CLICKABLE_SET_TRUE;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.SparseArray;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;
import com.facebook.litho.drawable.BorderColorDrawable;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import java.util.List;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class InternalNodeUtils {

  static InternalNode create(ComponentContext context) {
    NodeConfig.InternalNodeFactory factory = NodeConfig.sInternalNodeFactory;
    if (factory != null) {
      return factory.create(context);
    } else {
      return context.isInputOnlyInternalNodeEnabled()
          ? new InputOnlyInternalNode<>(context)
          : new DefaultInternalNode(context);
    }
  }

  static InternalNode.NestedTreeHolder createNestedTreeHolder(
      final ComponentContext context, final @Nullable TreeProps props) {
    NodeConfig.InternalNodeFactory factory = NodeConfig.sInternalNodeFactory;
    if (factory != null) {
      return factory.createNestedTreeHolder(context, props);
    } else {
      return context.isInputOnlyInternalNodeEnabled()
          ? new InputOnlyNestedTreeHolder(context, props)
          : new DefaultNestedTreeHolder(context, props);
    }
  }

  /**
   * Check that the root of the nested tree we are going to use, has valid layout directions with
   * its main tree holder node.
   */
  static boolean hasValidLayoutDirectionInNestedTree(
      NestedTreeHolderResult holder, LithoLayoutResult nestedTree) {
    return nestedTree.getInternalNode().isLayoutDirectionInherit()
        || (nestedTree.getResolvedLayoutDirection() == holder.getResolvedLayoutDirection());
  }

  /** Creates a {@link LithoRenderUnit} for the content output iff the result mounts content. */
  static @Nullable LithoRenderUnit createContentRenderUnit(LithoLayoutResult result) {
    final InternalNode node = result.getInternalNode();
    final Component component = node.getTailComponent();

    if (component == null || component.getMountType() == NONE) {
      return null;
    }

    final String componentKey = node.getTailComponentKey();
    final ComponentContext context = result.getContext();
    final LayoutState layoutState =
        Preconditions.checkNotNull(result.getLayoutStateContext().getLayoutState());
    final @Nullable DiffNode diffNode = result.getDiffNode();
    long previousId = -1;

    if (diffNode != null) {
      final LithoRenderUnit contentOutput = diffNode.getContentOutput();
      if (contentOutput != null) {
        previousId = contentOutput.getId();
      }
    }

    final long id =
        layoutState.calculateLayoutOutputId(
            component,
            componentKey,
            layoutState.getCurrentLevel(),
            OutputUnitType.CONTENT,
            previousId);

    return createRenderUnit(
        id,
        component,
        context,
        layoutState,
        result,
        node,
        node.getImportantForAccessibility(),
        previousId != id
            ? LayoutOutput.STATE_UNKNOWN
            : result.areCachedMeasuresValid()
                ? LayoutOutput.STATE_UPDATED
                : LayoutOutput.STATE_DIRTY,
        layoutState.getCurrentShouldDuplicateParentState(),
        false,
        needsHostView(node, layoutState));
  }

  /** Creates a {@link LithoRenderUnit} for the host output iff the result needs a host view. */
  static @Nullable LithoRenderUnit createHostRenderUnit(LithoLayoutResult result) {
    final LayoutState layoutState =
        Preconditions.checkNotNull(result.getLayoutStateContext().getLayoutState());
    final InternalNode node = result.getInternalNode();
    final boolean isRoot =
        !layoutState.mShouldAddHostViewForRootComponent
            && (result.getParent() == null
                || (result.getParent() instanceof NestedTreeHolderResult
                    && result.getParent().getParent() == null));

    if (!isRoot && !needsHostView(node, layoutState)) {
      return null;
    }

    final HostComponent hostComponent = HostComponent.create();

    // We need to pass common dynamic props to the host component, as they only could be applied to
    // views, so we'll need to set them up, when binding HostComponent to ComponentHost. At the same
    // time, we don't remove them from the current component, as we may calculate multiple
    // LayoutStates using same Components
    hostComponent.setCommonDynamicProps(mergeCommonDynamicProps(node.getComponents()));

    final long id;
    final @LayoutOutput.UpdateState int updateState;
    if (isRoot) {
      // The root host (LithoView) always has ID 0 and is unconditionally
      // set as dirty i.e. no need to use shouldComponentUpdate().
      id = ROOT_HOST_ID;
      updateState = LayoutOutput.STATE_DIRTY;
    } else {
      id =
          layoutState.calculateLayoutOutputId(
              hostComponent,
              node.getTailComponentKey(),
              layoutState.getCurrentLevel(),
              OutputUnitType.HOST,
              -1);
      updateState = LayoutOutput.STATE_UNKNOWN;
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
        false);
  }

  /**
   * Creates a {@link LithoRenderUnit} for the background output iff the result has a background.
   */
  static @Nullable LithoRenderUnit createBackgroundRenderUnit(LithoLayoutResult result) {
    final InternalNode node = result.getInternalNode();
    final Component component = node.getTailComponent();
    final Drawable background = result.getBackground();

    // Only create a background output when the component does not mount a View because
    // the background will get set in the output of the component.
    if (background != null && !isMountViewSpec(component)) {
      return createDrawableRenderUnit(result, background, OutputUnitType.BACKGROUND);
    }

    return null;
  }

  /**
   * Creates a {@link LithoRenderUnit} for the foreground output iff the result has a foreground.
   */
  static @Nullable LithoRenderUnit createForegroundRenderUnit(LithoLayoutResult result) {
    final InternalNode node = result.getInternalNode();
    final Component component = node.getTailComponent();
    final Drawable foreground = node.getForeground();

    /// Only create a foreground output when the component does not mount a View because
    // the foreground has already been set in the output of the component.
    if (foreground != null && (!isMountViewSpec(component) || SDK_INT < M)) {
      return createDrawableRenderUnit(result, foreground, OutputUnitType.FOREGROUND);
    }

    return null;
  }

  /** Creates a {@link LithoRenderUnit} for the border output iff the result has borders. */
  static @Nullable LithoRenderUnit createBorderRenderUnit(LithoLayoutResult result) {
    if (result.shouldDrawBorders()) {
      final Drawable border = getBorderColorDrawable(result);
      return createDrawableRenderUnit(result, border, OutputUnitType.BORDER);
    }

    return null;
  }

  /**
   * Common method to create the {@link LithoRenderUnit} for backgrounds, foregrounds, and border.
   * The method uses the {@param outputType} to decide between the options. This method will call
   * the shouldupdate, and {@link Component#onBoundsDefined(ComponentContext, ComponentLayout)} for
   * the {@link DrawableComponent}.
   */
  static LithoRenderUnit createDrawableRenderUnit(
      final LithoLayoutResult result,
      final Drawable drawable,
      final @OutputUnitType int outputType) {

    final Component component = DrawableComponent.create(drawable);
    final LayoutState layoutState =
        Preconditions.checkNotNull(result.getLayoutStateContext().getLayoutState());
    final InternalNode node = result.getInternalNode();
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
            !component.shouldComponentUpdate(null, recycle.output.getComponent(), null, component);
      } catch (Exception e) {
        ComponentUtils.handleWithHierarchy(result.getContext(), component, e);
        isCachedOutputUpdated = false;
      }
    } else {
      isCachedOutputUpdated = false;
    }

    final long previousId = recycle != null ? recycle.getId() : -1;
    final long id =
        layoutState.calculateLayoutOutputId(
            component, componentKey, layoutState.getCurrentLevel(), outputType, previousId);

    /* Call onBoundsDefined for the DrawableComponent */
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("onBoundsDefined:" + node.getSimpleName());
    }

    try {
      component.onBoundsDefined(result.getContext(), result, null);
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
            ? LayoutOutput.STATE_UNKNOWN
            : isCachedOutputUpdated ? LayoutOutput.STATE_UPDATED : LayoutOutput.STATE_DIRTY,
        layoutState.getCurrentShouldDuplicateParentState(),
        false,
        needsHostView(node, layoutState));
  }

  /** Generic method to create a {@link LithoRenderUnit}. */
  static LithoRenderUnit createRenderUnit(
      long id,
      Component component,
      @Nullable ComponentContext context,
      LayoutState layoutState,
      LithoLayoutResult result,
      InternalNode node,
      int importantForAccessibility,
      @LayoutOutput.UpdateState int updateState,
      boolean duplicateParentState,
      boolean duplicateChildrenStates,
      boolean hasHostView) {

    final boolean isMountViewSpec = isMountViewSpec(component);

    int flags = 0;

    final NodeInfo layoutOutputNodeInfo;
    final ViewNodeInfo layoutOutputViewNodeInfo;

    final NodeInfo nodeInfo = node.getNodeInfo();

    // View mount specs are able to set their own attributes when they're mounted.
    // Non-view specs (drawable and layout) always transfer their view attributes
    // to their respective hosts.
    // Moreover, if the component mounts a view, then we apply padding to the view itself later on.
    // Otherwise, apply the padding to the bounds of the layout output.
    if (isMountViewSpec) {
      layoutOutputNodeInfo = nodeInfo;
      // Acquire a ViewNodeInfo, set it up and release it after passing it to the LayoutOutput.
      final ViewNodeInfo viewNodeInfo = new ViewNodeInfo();

      // The following only applies if bg/fg outputs are NOT disabled:
      // backgrounds and foregrounds should not be set for HostComponents
      // because those will either be set on the content output or explicit outputs
      // will be created for backgrounds and foreground.
      if (layoutState.mShouldDisableDrawableOutputs || !isHostSpec(component)) {
        viewNodeInfo.setBackground(result.getBackground());
        if (SDK_INT >= M) {
          viewNodeInfo.setForeground(node.getForeground());
        }
      }
      if (result.isPaddingSet()) {
        viewNodeInfo.setPadding(
            result.getPaddingLeft(),
            result.getPaddingTop(),
            result.getPaddingRight(),
            result.getPaddingBottom());
      }
      viewNodeInfo.setLayoutDirection(result.getResolvedLayoutDirection());
      if (node.hasTouchExpansion()) {
        viewNodeInfo.setExpandedTouchBounds(result);
      }
      viewNodeInfo.setLayerType(node.getLayerType(), node.getLayerPaint());
      layoutOutputViewNodeInfo = viewNodeInfo;
    } else {
      if (nodeInfo != null && nodeInfo.getEnabledState() == ENABLED_SET_FALSE) {
        flags |= LAYOUT_FLAG_DISABLE_TOUCHABLE;
      }
      layoutOutputNodeInfo = null;
      layoutOutputViewNodeInfo = null;
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

    return LithoRenderUnit.create(
        id,
        component,
        context,
        layoutOutputNodeInfo,
        layoutOutputViewNodeInfo,
        flags,
        importantForAccessibility,
        updateState);
  }

  private static SparseArray<DynamicValue<?>> mergeCommonDynamicProps(List<Component> components) {
    final SparseArray<DynamicValue<?>> mergedDynamicProps = new SparseArray<>();
    for (Component component : components) {
      final SparseArray<DynamicValue<?>> commonDynamicProps = component.getCommonDynamicProps();
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

    final InternalNode node = result.getInternalNode();
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
  static boolean needsHostView(final InternalNode node, final LayoutState layoutState) {
    final Component component = node.getTailComponent();

    if (isMountViewSpec(component)) {
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

    if (needsHostViewForTransition(node)) {
      return true;
    }

    if (hasSelectedStateWhenDisablingDrawableOutputs(layoutState, node)) {
      return true;
    }

    return false;
  }

  /**
   * Determine if a given {@link InternalNode} within the context of a given {@link LayoutState}
   * requires to be wrapped inside a view.
   *
   * @see #needsHostView(InternalNode, LayoutState)
   */
  private static boolean hasViewContent(final InternalNode node, final LayoutState layoutState) {
    final Component component = node.getTailComponent();
    final NodeInfo nodeInfo = node.getNodeInfo();

    final boolean implementsAccessibility =
        (nodeInfo != null && nodeInfo.needsAccessibilityDelegate())
            || (component != null && component.implementsAccessibility());

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
    final boolean hasViewTag = nodeInfo.getViewTag() != null;
    final boolean hasViewTags = nodeInfo.getViewTags() != null;
    final boolean hasShadowElevation = nodeInfo.getShadowElevation() != 0;
    final boolean hasOutlineProvider = nodeInfo.getOutlineProvider() != null;
    final boolean hasClipToOutline = nodeInfo.getClipToOutline();
    final boolean isFocusableSetTrue = nodeInfo.getFocusState() == FOCUS_SET_TRUE;
    final boolean isClickableSetTrue = nodeInfo.getClickableState() == CLICKABLE_SET_TRUE;
    final boolean hasClipChildrenSet = nodeInfo.isClipChildrenSet();
    final boolean hasTransitionName = nodeInfo.getTransitionName() != null;

    return hasFocusChangeHandler
        || hasEnabledTouchEventHandlers
        || hasViewTag
        || hasViewTags
        || hasShadowElevation
        || hasOutlineProvider
        || hasClipToOutline
        || hasClipChildrenSet
        || isFocusableSetTrue
        || isClickableSetTrue
        || hasTransitionName;
  }

  /**
   * Similar to {@link InternalNodeUtils#needsHostView(InternalNode, LayoutState)} but without
   * dependency to {@link LayoutState} instance. This will be used for debugging tools to indicate
   * whether the mountable output is a wrapped View or View MountSpec. Unlike {@link
   * InternalNodeUtils#needsHostView(InternalNode, LayoutState)} this does not consider
   * accessibility also does not consider root component, but this approximation is good enough for
   * debugging purposes.
   */
  static boolean hasViewOutput(InternalNode node) {
    return node.isForceViewWrapping()
        || isMountViewSpec(node.getTailComponent())
        || InternalNodeUtils.hasViewAttributes(node.getNodeInfo())
        || InternalNodeUtils.needsHostViewForCommonDynamicProps(node)
        || InternalNodeUtils.needsHostViewForTransition(node);
  }

  private static boolean hasSelectedStateWhenDisablingDrawableOutputs(
      final LayoutState layoutState, final InternalNode node) {
    return layoutState.mShouldAddHostViewForRootComponent
        && !isMountViewSpec(node.getTailComponent())
        && node.getNodeInfo() != null
        && node.getNodeInfo().getSelectedState() != NodeInfo.SELECTED_UNSET;
  }

  static boolean needsHostViewForCommonDynamicProps(final InternalNode node) {
    final List<Component> components = node.getComponents();
    for (Component comp : components) {
      if (comp != null && comp.hasCommonDynamicProps()) {
        // Need a host View to apply the dynamic props to
        return true;
      }
    }
    return false;
  }

  static boolean needsHostViewForTransition(final InternalNode node) {
    return !TextUtils.isEmpty(node.getTransitionKey()) && !isMountViewSpec(node.getTailComponent());
  }
}
