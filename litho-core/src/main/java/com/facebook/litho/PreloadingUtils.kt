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

@file:JvmName("PreloadingUtils")

package com.facebook.litho

import android.os.Handler
import com.facebook.rendercore.FastMath
import com.facebook.rendercore.LruResourceCache
import com.facebook.rendercore.MountItemsPool
import com.facebook.rendercore.MountState
import com.facebook.rendercore.ResourceCache
import com.facebook.rendercore.ResourceResolver
import com.facebook.rendercore.RunnableHandler
import com.facebook.rendercore.primitives.Allocator
import com.facebook.rendercore.primitives.AspectRatioLayoutBehavior
import com.facebook.rendercore.primitives.BindScope
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.EqualDimensionsLayoutBehavior
import com.facebook.rendercore.primitives.Equivalence
import com.facebook.rendercore.primitives.ExactSizeConstraintsLayoutBehavior
import com.facebook.rendercore.primitives.FillLayoutBehavior
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.MountBehavior
import com.facebook.rendercore.primitives.MountConfigurationScope
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.PrimitiveRenderUnit
import com.facebook.rendercore.primitives.ViewAllocator

private val preloadClassHandler: Handler =
    RunnableHandler.DefaultHandler(ComponentTree.getDefaultLayoutThreadLooper())

/**
 * Utility for preloading classes that should be loaded early but may not be accessible outside of
 * Litho.
 */
fun preloadLithoClasses(additionalClasses: (() -> Set<Class<*>>)? = null) {
  val r = Runnable {
    val classes: MutableSet<Class<*>> =
        listOf(
                SpecGeneratedComponent::class.java,
                Component::class.java,
                EventDispatcher::class.java,
                HasEventDispatcher::class.java,
                Equivalence::class.java,
                EventTriggerTarget::class.java,
                HasEventTrigger::class.java,
                LithoYogaMeasureFunction::class.java,
                Size::class.java,
                ComponentTree::class.java,
                LithoLifecycleListener::class.java,
                DefaultErrorEventHandler::class.java,
                ErrorEventHandler::class.java,
                EventHandler::class.java,
                LayoutState::class.java,
                HostComponent::class.java,
                DrawableComponent::class.java,
                StateContainer::class.java,
                CommonProps::class.java,
                LayoutProps::class.java,
                NodeInfo::class.java,
                DelegatingEventHandler::class.java,
                StateValue::class.java,
                Output::class.java,
                LithoLifecycleProviderDelegate::class.java,
                LithoLifecycleProvider::class.java,
                Diff::class.java,
                LayoutThreadPoolConfigurationImpl::class.java,
                WorkingRange::class.java,
                LithoNode::class.java,
                LithoLayoutResult::class.java,
                ComponentLayout::class.java,
                NodeConfig::class.java,
                LithoRenderContext::class.java,
                DefaultDiffNode::class.java,
                LithoRenderUnit::class.java,
                LithoLayoutData::class.java,
                Column::class.java,
                Edges::class.java,
                Row::class.java,
                InterStagePropsContainer::class.java,
                Border::class.java,
                FastMath::class.java,
                LithoView::class.java,
                ComponentHost::class.java,
                MountState::class.java,
                DynamicPropsManager::class.java,
                AnimationsDebug::class.java,
                Wrapper::class.java,
                MountItemsPool.DefaultItemPool::class.java,
                LayoutSpecAttachable::class.java,
                Attachable::class.java,
                Touchable::class.java,
                ImageContent::class.java,
                AttachDetachHandler::class.java,
                ComponentClickListener::class.java,
                CommonUtils::class.java,
                MatrixDrawable::class.java,
                VisibleEvent::class.java,
                FullImpressionVisibleEvent::class.java,
                HasLithoViewChildren::class.java,
                ComponentLongClickListener::class.java,
                FocusedVisibleEvent::class.java,
                RenderState::class.java,
                ViewAttributes::class.java,
                LithoViewAttributesExtension::class.java,
                DynamicPropsExtension::class.java,
                VisibilityChangedEvent::class.java,
                InvisibleEvent::class.java,
                TouchEvent::class.java,
                ComponentsSystrace::class.java,
                WorkContinuationInstrumenter::class.java,
                EventDispatcherInstrumenter::class.java,
                ComponentContext::class.java,
                ResourceCache::class.java,
                LruResourceCache::class.java,
                ResourceResolver::class.java,
                DynamicValue::class.java,
                InitialStateContainer::class.java,
                RenderUnitIdGenerator::class.java,
                EventHandlersController::class.java,
                EventTriggersContainer::class.java,
                WorkingRangeStatusHandler::class.java,
                StateHandler::class.java,
                IncrementalMountHelper::class.java,
                DefaultComponentsSystrace::class.java,
                LithoLayoutContext::class.java,
                AccessibilityUtils::class.java,
                Layout::class.java,
                TreeProps::class.java,
                ThreadUtils::class.java,
                RenderResult::class.java,
                ComponentKeyUtils::class.java,
                ThreadPoolLayoutHandler::class.java,
                LayoutThreadPoolExecutor::class.java,
                LayoutThreadFactory::class.java,
                LithoNodeUtils::class.java,
                SizeSpec::class.java,
                TransitionId::class.java,
                OutputUnitsAffinityGroup::class.java,
                LithoAnimtableItem::class.java,
                WorkingRangeContainer::class.java,
                MeasureComparisonUtils::class.java,
                Handle::class.java,
                DrawableMatrix::class.java,
                LithoMountData::class.java,
                TransitionsExtension::class.java,
                ContextUtils::class.java,
                ComponentUtils::class.java,
                MountItemsPool::class.java,
                LogTreePopulator::class.java,
                TouchExpansionDelegate::class.java,
                ComponentTouchListener::class.java,
                EventTrigger::class.java,
                Transition::class.java,
                ComponentHostUtils::class.java,
                DelayTransitionSet::class.java,
                ParallelTransitionSet::class.java,
                TransitionManager::class.java,
                TransitionIdMap::class.java,
                ThreadTracingRunnable::class.java,
                TransitionSet::class.java,
                ReThrownException::class.java,
                TextContent::class.java,
                LithoGestureDetector::class.java,
                LithoHostListenerCoordinator::class.java,
                EndToEndTestingExtension::class.java,
                ComponentAccessibilityDelegate::class.java,
                ScopedComponentInfo::class.java,
                KStateContainer::class.java,
                EventDispatcherUtils::class.java,
                LithoViewTestHelper::class.java,
                DebugComponent::class.java,
                UnfocusedVisibleEvent::class.java,
                LithoTooltip::class.java,
                ComponentsReporter::class.java,
                InterceptTouchEvent::class.java,
                ComponentFocusChangeListener::class.java,
                FocusChangedEvent::class.java,
                ClickEvent::class.java,
                ComponentTreeDumpingHelper::class.java,
                BoundaryWorkingRange::class.java,
                RenderCompleteEvent::class.java,
                InterceptTouchEvent::class.java,
                Primitive::class.java,
                MountConfigurationScope::class.java,
                BindScope::class.java,
                MountBehavior::class.java,
                PrimitiveRenderUnit::class.java,
                LayoutScope::class.java,
                LayoutBehavior::class.java,
                PrimitiveLayoutResult::class.java,
                ExactSizeConstraintsLayoutBehavior::class.java,
                FillLayoutBehavior::class.java,
                FixedSizeLayoutBehavior::class.java,
                AspectRatioLayoutBehavior::class.java,
                EqualDimensionsLayoutBehavior::class.java,
                Equivalence::class.java,
                Allocator::class.java,
                ViewAllocator::class.java,
                DrawableAllocator::class.java)
            .toHashSet()
    if (additionalClasses != null) {
      classes.addAll(additionalClasses())
    }
    for (clazz in classes) {
      clazz.hashCode()
    }
  }
  preloadClassHandler.post(r)
}

fun preloadYogaConfig() {
  NodeConfig.createYogaNode()
}
