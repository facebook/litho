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
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.drawable.BorderColorDrawable;
import com.facebook.rendercore.Mountable;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.primitives.Primitive;
import java.util.List;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class LithoNodeUtils {

  /** Creates a {@link LithoRenderUnit} for the content output iff the result mounts content. */
  static @Nullable LithoRenderUnit createContentRenderUnit(
      final LithoNode node,
      final boolean areCachedMeasuresValid,
      final @Nullable DiffNode diffNode) {
    final Component component = node.getTailComponent();

    // We need to merge dynamic props from all scoped component infos in order to cover cases where
    // a non-SpecGeneratedComponent such as Primitive is wrapped in a Wrapper. If we don't merge
    // then DynamicCommonProps added through the Wrapper will be lost.
    final SparseArray<DynamicValue<?>> commonDynamicProps =
        mergeCommonDynamicProps(node.getScopedComponentInfos());

    if (component.getMountType() == NONE) {
      return null;
    }

    final String componentKey = node.getTailComponentKey();
    final ComponentContext context = node.getTailComponentContext();
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
        commonDynamicProps,
        context,
        node,
        node.getImportantForAccessibility(),
        previousId != id
            ? MountSpecLithoRenderUnit.STATE_UNKNOWN
            : areCachedMeasuresValid
                ? MountSpecLithoRenderUnit.STATE_UPDATED
                : MountSpecLithoRenderUnit.STATE_DIRTY,
        node.isDuplicateParentStateEnabled(),
        false,
        node.needsHostView(),
        node.willMountView(),
        (node.needsHostView() || node.getMountable() != null || node.getPrimitive() != null)
            ? null
            : node.getCustomBindersForMountSpec(),
        getLithoNodeDebugKey(node, OutputUnitType.CONTENT));
  }

  /** Creates a {@link LithoRenderUnit} for the host output iff the result needs a host view. */
  static @Nullable LithoRenderUnit createHostRenderUnit(final LithoNode node) {
    if (!node.needsHostView()) {
      return null;
    }

    final HostComponent hostComponent = HostComponent.create();

    // We need to pass common dynamic props to the host component, as they only could be applied to
    // views, so we'll need to set them up, when binding HostComponent to ComponentHost. At the same
    // time, we don't remove them from the current component, as we may calculate multiple
    // LayoutStates using same Components
    SparseArray<DynamicValue<?>> commonDynamicProps =
        mergeCommonDynamicProps(node.getScopedComponentInfos());
    hostComponent.setCommonDynamicProps(commonDynamicProps);

    final long id =
        node.getTailComponentContext()
            .calculateLayoutOutputId(node.getTailComponentKey(), OutputUnitType.HOST);

    return createRenderUnit(
        id,
        hostComponent,
        commonDynamicProps,
        null,
        node,
        node.getImportantForAccessibility(),
        MountSpecLithoRenderUnit.STATE_UNKNOWN,
        node.isHostDuplicateParentState(),
        node.isDuplicateChildrenStatesEnabled(),
        false,
        true,
        node.needsHostView() ? node.getCustomBindersForMountSpec() : null,
        getLithoNodeDebugKey(node, OutputUnitType.HOST));
  }

  /** Creates a {@link LithoRenderUnit} for the root host */
  static LithoRenderUnit createRootHostRenderUnit(LithoNode node) {

    final HostComponent hostComponent = HostComponent.create();

    // We need to pass common dynamic props to the host component, as they only could be applied to
    // views, so we'll need to set them up, when binding HostComponent to ComponentHost. At the same
    // time, we don't remove them from the current component, as we may calculate multiple
    // LayoutStates using same Components
    SparseArray<DynamicValue<?>> commonDynamicProps =
        mergeCommonDynamicProps(node.getScopedComponentInfos());
    hostComponent.setCommonDynamicProps(commonDynamicProps);

    return createRenderUnit(
        ROOT_HOST_ID, // The root host (LithoView) always has ID 0
        hostComponent,
        commonDynamicProps,
        null,
        node,
        node.getImportantForAccessibility(),
        MountSpecLithoRenderUnit.STATE_DIRTY, // set as dirty to skip shouldComponentUpdate()
        node.isHostDuplicateParentState(),
        node.isDuplicateChildrenStatesEnabled(),
        false,
        true,
        node.willMountView() ? null : node.getCustomBindersForMountSpec(),
        getLithoNodeDebugKey(node, OutputUnitType.HOST));
  }

  /**
   * Creates a {@link LithoRenderUnit} for the background output iff the result has a background.
   */
  static @Nullable LithoRenderUnit createBackgroundRenderUnit(
      final LithoNode node, final int width, final int height, final @Nullable DiffNode diffNode) {
    final Drawable background = node.getBackground();

    // Only create a background output when the component does not mount a View because
    // the background will get set in the output of the component.
    if (background != null && !node.willMountView()) {
      return createDrawableRenderUnit(
          node, background, width, height, OutputUnitType.BACKGROUND, diffNode);
    }

    return null;
  }

  /**
   * Creates a {@link LithoRenderUnit} for the foreground output iff the result has a foreground.
   */
  static @Nullable LithoRenderUnit createForegroundRenderUnit(
      final LithoNode node, final int width, final int height, final @Nullable DiffNode diffNode) {
    final Drawable foreground = node.getForeground();

    /// Only create a foreground output when the component does not mount a View because
    // the foreground has already been set in the output of the component.
    if (foreground != null && (!node.willMountView() || SDK_INT < M)) {
      return createDrawableRenderUnit(
          node, foreground, width, height, OutputUnitType.FOREGROUND, diffNode);
    }

    return null;
  }

  /** Creates a {@link LithoRenderUnit} for the border output iff the result has borders. */
  static LithoRenderUnit createBorderRenderUnit(
      final LithoNode node,
      final BorderColorDrawable border,
      final int width,
      final int height,
      final @Nullable DiffNode diffNode) {
    return createDrawableRenderUnit(node, border, width, height, OutputUnitType.BORDER, diffNode);
  }

  /**
   * Common method to create the {@link LithoRenderUnit} for backgrounds, foregrounds, and border.
   * The method uses the {@param outputType} to decide between the options. This method will call
   * the shouldupdate, and {@link SpecGeneratedComponent#onBoundsDefined(ComponentContext,
   * ComponentLayout, InterStagePropsContainer)} for the {@link DrawableComponent}.
   */
  static LithoRenderUnit createDrawableRenderUnit(
      final LithoNode node,
      final Drawable drawable,
      final int width,
      final int height,
      final @OutputUnitType int outputType,
      final @Nullable DiffNode diffNode) {

    final DrawableComponent<?> component = DrawableComponent.create(drawable, width, height);
    final ComponentContext context = node.getTailComponentContext();
    final String componentKey = node.getTailComponentKey();

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

    return createRenderUnit(
        id,
        component,
        null /* Drawables don't bind dynamic props */,
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
        false,
        null,
        getLithoNodeDebugKey(node, outputType));
  }

  /** Generic method to create a {@link LithoRenderUnit}. */
  static LithoRenderUnit createRenderUnit(
      long id,
      Component component,
      @Nullable SparseArray<DynamicValue<?>> commonDynamicProps,
      @Nullable ComponentContext context,
      LithoNode node,
      int importantForAccessibility,
      @MountSpecLithoRenderUnit.UpdateState int updateState,
      boolean duplicateParentState,
      boolean duplicateChildrenStates,
      boolean hasHostView,
      boolean isMountViewSpec,
      @Nullable Map<Class<?>, RenderUnit.Binder<Object, Object, Object>> customBindersForMountSpec,
      @Nullable String debugKey) {

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
          component,
          commonDynamicProps,
          context,
          layoutOutputNodeInfo,
          flags,
          importantForAccessibility,
          mountable,
          debugKey);
    }

    Primitive primitive = node.getPrimitive();
    if (primitive != null && isPrimitive(component)) {
      return PrimitiveLithoRenderUnit.create(
          component,
          commonDynamicProps,
          context,
          layoutOutputNodeInfo,
          flags,
          importantForAccessibility,
          primitive.getRenderUnit(),
          debugKey);
    }

    LithoRenderUnit renderUnit =
        MountSpecLithoRenderUnit.create(
            id,
            component,
            (SparseArray) commonDynamicProps,
            context,
            layoutOutputNodeInfo,
            flags,
            importantForAccessibility,
            updateState,
            debugKey);

    if (customBindersForMountSpec != null) {
      for (RenderUnit.Binder<Object, Object, Object> binder : customBindersForMountSpec.values()) {
        renderUnit.addOptionalMountBinder(
            RenderUnit.DelegateBinder.createDelegateBinder(renderUnit, binder));
      }
    }

    return renderUnit;
  }

  private static SparseArray<DynamicValue<?>> mergeCommonDynamicProps(
      List<ScopedComponentInfo> infos) {
    final SparseArray<DynamicValue<?>> mergedDynamicProps = new SparseArray<>();
    for (ScopedComponentInfo info : infos) {
      final @Nullable CommonProps commonProps = info.getCommonProps();
      final @Nullable SparseArray<DynamicValue<?>> commonDynamicProps =
          (commonProps != null) ? commonProps.getCommonDynamicProps() : null;
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

  @Nullable
  private static String getLithoNodeDebugKey(LithoNode node, @OutputUnitType int outputUnitType) {
    return getDebugKey(node.getTailComponentKey(), outputUnitType);
  }

  @Nullable
  public static String getDebugKey(
      @Nullable String componentKey, @OutputUnitType int outputUnitType) {
    if (ComponentsConfiguration.isDebugModeEnabled) {
      return null;
    }

    switch (outputUnitType) {
      case OutputUnitType.BACKGROUND:
        return componentKey + "$background";
      case OutputUnitType.BORDER:
        return componentKey + "$border";
      case OutputUnitType.FOREGROUND:
        return componentKey + "$foreground";
      case OutputUnitType.CONTENT:
        return componentKey;
      case OutputUnitType.HOST:
        return componentKey + "$host";
      default:
        return null;
    }
  }
}
