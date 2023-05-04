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
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static com.facebook.litho.Component.MountType.NONE;
import static com.facebook.litho.Component.isMountable;
import static com.facebook.litho.Component.isPrimitive;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_DISABLE_TOUCHABLE;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_DUPLICATE_CHILDREN_STATES;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_DUPLICATE_PARENT_STATE;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_HAS_TOUCH_EVENT_HANDLERS;
import static com.facebook.litho.LithoRenderUnit.LAYOUT_FLAG_MATCH_HOST_BOUNDS;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;

import android.graphics.drawable.Drawable;
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
  static @Nullable LithoRenderUnit createContentRenderUnit(LithoLayoutResult result) {
    final LithoNode node = result.getNode();
    final Component component = node.getTailComponent();

    if (component.getMountType() == NONE) {
      return null;
    }

    final String componentKey = node.getTailComponentKey();
    final ComponentContext context = node.getTailComponentContext();
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
        node,
        node.getImportantForAccessibility(),
        previousId != id
            ? MountSpecLithoRenderUnit.STATE_UNKNOWN
            : result.areCachedMeasuresValid()
                ? MountSpecLithoRenderUnit.STATE_UPDATED
                : MountSpecLithoRenderUnit.STATE_DIRTY,
        node.isDuplicateParentStateEnabled(),
        false,
        node.needsHostView(),
        node.willMountView());
  }

  /** Creates a {@link LithoRenderUnit} for the host output iff the result needs a host view. */
  static @Nullable LithoRenderUnit createHostRenderUnit(
      LithoLayoutResult result, final boolean isRoot) {
    final LithoNode node = result.getNode();

    if (!isRoot && !node.needsHostView()) {
      return null;
    }

    final HostComponent hostComponent = HostComponent.create();

    // We need to pass common dynamic props to the host component, as they only could be applied to
    // views, so we'll need to set them up, when binding HostComponent to ComponentHost. At the same
    // time, we don't remove them from the current component, as we may calculate multiple
    // LayoutStates using same Components
    hostComponent.setCommonDynamicProps(mergeCommonDynamicProps(node.getScopedComponentInfos()));

    final long id;
    final @MountSpecLithoRenderUnit.UpdateState int updateState;
    if (isRoot) {
      // The root host (LithoView) always has ID 0 and is unconditionally
      // set as dirty i.e. no need to use shouldComponentUpdate().
      id = ROOT_HOST_ID;
      updateState = MountSpecLithoRenderUnit.STATE_DIRTY;
    } else {
      id =
          node.getTailComponentContext()
              .calculateLayoutOutputId(node.getTailComponentKey(), OutputUnitType.HOST);

      updateState = MountSpecLithoRenderUnit.STATE_UNKNOWN;
    }

    return createRenderUnit(
        id,
        hostComponent,
        null,
        node,
        node.getImportantForAccessibility(),
        updateState,
        node.isHostDuplicateParentState(),
        node.isDuplicateChildrenStatesEnabled(),
        false,
        true);
  }

  /**
   * Creates a {@link LithoRenderUnit} for the background output iff the result has a background.
   */
  static @Nullable LithoRenderUnit createBackgroundRenderUnit(LithoLayoutResult result) {
    final LithoNode node = result.getNode();
    final Drawable background = node.getBackground();

    // Only create a background output when the component does not mount a View because
    // the background will get set in the output of the component.
    if (background != null && !node.willMountView()) {
      return createDrawableRenderUnit(result, background, OutputUnitType.BACKGROUND);
    }

    return null;
  }

  /**
   * Creates a {@link LithoRenderUnit} for the foreground output iff the result has a foreground.
   */
  static @Nullable LithoRenderUnit createForegroundRenderUnit(LithoLayoutResult result) {
    final LithoNode node = result.getNode();
    final Drawable foreground = node.getForeground();

    /// Only create a foreground output when the component does not mount a View because
    // the foreground has already been set in the output of the component.
    if (foreground != null && (!node.willMountView() || SDK_INT < M)) {
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
   * the shouldupdate, and {@link SpecGeneratedComponent#onBoundsDefined(ComponentContext,
   * ComponentLayout, InterStagePropsContainer)} for the {@link DrawableComponent}.
   */
  static LithoRenderUnit createDrawableRenderUnit(
      final LithoLayoutResult result,
      final Drawable drawable,
      final @OutputUnitType int outputType) {

    final Component component = DrawableComponent.create(drawable);
    final LithoNode node = result.getNode();
    final ComponentContext context = node.getTailComponentContext();
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
        ComponentUtils.handleWithHierarchy(context, component, e);
        isCachedOutputUpdated = false;
      }
    } else {
      isCachedOutputUpdated = false;
    }

    final long previousId = recycle != null ? recycle.getId() : -1;
    final long id = context.calculateLayoutOutputId(componentKey, outputType);

    /* Call onBoundsDefined for the DrawableComponent */
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("onBoundsDefined:" + component.getSimpleName());
    }

    try {
      if (component instanceof SpecGeneratedComponent) {
        ((SpecGeneratedComponent) component).onBoundsDefined(context, result, null);
      }
    } catch (Exception e) {
      ComponentUtils.handleWithHierarchy(context, component, e);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }

    return createRenderUnit(
        id,
        component,
        null,
        node,
        IMPORTANT_FOR_ACCESSIBILITY_NO,
        previousId != id
            ? MountSpecLithoRenderUnit.STATE_UNKNOWN
            : isCachedOutputUpdated
                ? MountSpecLithoRenderUnit.STATE_UPDATED
                : MountSpecLithoRenderUnit.STATE_DIRTY,
        node.isDuplicateParentStateEnabled(),
        false,
        node.needsHostView(),
        false);
  }

  /** Generic method to create a {@link LithoRenderUnit}. */
  static LithoRenderUnit createRenderUnit(
      long id,
      Component component,
      @Nullable ComponentContext context,
      LithoNode node,
      int importantForAccessibility,
      @MountSpecLithoRenderUnit.UpdateState int updateState,
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

    if (ComponentContext.getComponentsConfig(node.getHeadComponentContext())
        .isShouldDisableBgFgOutputs()) {
      flags |= LAYOUT_FLAG_DRAWABLE_OUTPUTS_DISABLED;
    }
    if (nodeInfo != null && nodeInfo.hasTouchEventHandlers()) {
      flags |= LAYOUT_FLAG_HAS_TOUCH_EVENT_HANDLERS;
    }

    Mountable<?> mountable = node.getMountable();
    if (mountable != null && isMountable(component)) {
      return MountableLithoRenderUnit.create(
          component, context, layoutOutputNodeInfo, flags, importantForAccessibility, mountable);
    }

    Primitive primitive = node.getPrimitive();
    if (primitive != null && isPrimitive(component)) {
      return PrimitiveLithoRenderUnit.create(
          component,
          context,
          layoutOutputNodeInfo,
          flags,
          importantForAccessibility,
          primitive.getRenderUnit());
    }

    return MountSpecLithoRenderUnit.create(
        id,
        component,
        context,
        layoutOutputNodeInfo,
        flags,
        importantForAccessibility,
        updateState);
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
}
