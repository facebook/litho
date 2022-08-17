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

import android.os.Handler;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.MountState;
import com.facebook.rendercore.RunnableHandler.DefaultHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Utility for preloading classes that should be loaded early but may not be accessible outside of
 * Litho.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class PreloadingUtils {
  private static final Handler preloadClassHandler =
      new DefaultHandler(ComponentTree.getDefaultLayoutThreadLooper());

  public static void preloadLithoClasses(final @Nullable Set<Class> additionalClasses) {
    final Runnable r =
        new Runnable() {
          @Override
          public void run() {
            final Set<Class> classes =
                new HashSet<Class>(
                    Arrays.asList(
                        SpecGeneratedComponent.class,
                        Component.class,
                        EventDispatcher.class,
                        HasEventDispatcher.class,
                        Equivalence.class,
                        EventTriggerTarget.class,
                        HasEventTrigger.class,
                        LithoYogaMeasureFunction.class,
                        Size.class,
                        ComponentTree.class,
                        LithoLifecycleListener.class,
                        DefaultErrorEventHandler.class,
                        ErrorEventHandler.class,
                        EventHandler.class,
                        LayoutState.class,
                        HostComponent.class,
                        DrawableComponent.class,
                        StateContainer.class,
                        CommonProps.class,
                        LayoutProps.class,
                        NodeInfo.class,
                        DelegatingEventHandler.class,
                        StateValue.class,
                        Output.class,
                        LithoLifecycleProviderDelegate.class,
                        LithoLifecycleProvider.class,
                        Diff.class,
                        LayoutThreadPoolConfigurationImpl.class,
                        WorkingRange.class,
                        LithoNode.class,
                        LithoLayoutResult.class,
                        ComponentLayout.class,
                        NodeConfig.class,
                        LithoRenderContext.class,
                        DefaultDiffNode.class,
                        ViewNodeInfo.class,
                        LithoRenderUnit.class,
                        LithoLayoutData.class,
                        Column.class,
                        Edges.class,
                        Row.class,
                        InterStagePropsContainer.class,
                        Border.class,
                        FastMath.class,
                        LithoView.class,
                        ComponentHost.class,
                        MountState.class,
                        DynamicPropsManager.class,
                        AnimationsDebug.class,
                        Wrapper.class,
                        MountContentPool.class,
                        DefaultMountContentPool.class,
                        LayoutSpecAttachable.class,
                        Attachable.class,
                        Touchable.class,
                        ImageContent.class,
                        AttachDetachHandler.class,
                        DisabledMountContentPool.class,
                        ComponentClickListener.class,
                        CommonUtils.class,
                        MatrixDrawable.class,
                        VisibleEvent.class,
                        FullImpressionVisibleEvent.class,
                        HasLithoViewChildren.class,
                        ComponentLongClickListener.class,
                        FocusedVisibleEvent.class,
                        RenderState.class,
                        LithoViewAttributesExtension.class,
                        DynamicPropsExtension.class,
                        VisibilityChangedEvent.class,
                        InvisibleEvent.class,
                        TouchEvent.class,
                        ComponentsSystrace.class,
                        WorkContinuationInstrumenter.class,
                        EventDispatcherInstrumenter.class,
                        ComponentContext.class,
                        ResourceCache.class,
                        LruResourceCache.class,
                        ResourceResolver.class,
                        DynamicValue.class,
                        InitialStateContainer.class,
                        RenderUnitIdGenerator.class,
                        EventHandlersController.class,
                        EventTriggersContainer.class,
                        WorkingRangeStatusHandler.class,
                        StateHandler.class,
                        IncrementalMountHelper.class,
                        DefaultComponentsSystrace.class,
                        LayoutStateContext.class,
                        AccessibilityUtils.class,
                        Layout.class,
                        TreeProps.class,
                        ThreadUtils.class,
                        RenderResult.class,
                        ComponentKeyUtils.class,
                        ThreadPoolLayoutHandler.class,
                        LayoutThreadPoolExecutor.class,
                        LayoutThreadFactory.class,
                        InternalNodeUtils.class,
                        SizeSpec.class,
                        TransitionId.class,
                        LayoutOutput.class,
                        OutputUnitsAffinityGroup.class,
                        LithoAnimtableItem.class,
                        WorkingRangeContainer.class,
                        MeasureComparisonUtils.class,
                        Handle.class,
                        DrawableMatrix.class,
                        LithoMountData.class,
                        TransitionsExtension.class,
                        ContextUtils.class,
                        ComponentUtils.class,
                        MountItemsPool.class,
                        LogTreePopulator.class,
                        DoubleMeasureFixUtil.class,
                        TouchExpansionDelegate.class,
                        ComponentTouchListener.class,
                        EventTrigger.class,
                        Transition.class,
                        ComponentHostUtils.class,
                        DelayTransitionSet.class,
                        ParallelTransitionSet.class,
                        TransitionManager.class,
                        TransitionIdMap.class,
                        ThreadTracingRunnable.class,
                        TransitionSet.class,
                        ReThrownException.class,
                        TextContent.class,
                        LithoGestureDetector.class,
                        LithoHostListenerCoordinator.class,
                        EndToEndTestingExtension.class,
                        ComponentAccessibilityDelegate.class,
                        ScopedComponentInfo.class,
                        KStateContainer.class,
                        EventDispatcherUtils.class,
                        LithoViewTestHelper.class,
                        DebugComponent.class,
                        UnfocusedVisibleEvent.class,
                        LithoTooltip.class,
                        ComponentsReporter.class,
                        InterceptTouchEvent.class,
                        ComponentFocusChangeListener.class,
                        FocusChangedEvent.class,
                        ClickEvent.class,
                        ComponentTreeDumpingHelper.class,
                        BoundaryWorkingRange.class,
                        RenderCompleteEvent.class,
                        InterceptTouchEvent.class));
            if (additionalClasses != null) {
              classes.addAll(additionalClasses);
            }

            for (Class clazz : classes) {
              clazz.hashCode();
            }
          }
        };
    preloadClassHandler.post(r);
  }

  public static void preloadLithoClasses() {
    preloadLithoClasses(null);
  }
}
