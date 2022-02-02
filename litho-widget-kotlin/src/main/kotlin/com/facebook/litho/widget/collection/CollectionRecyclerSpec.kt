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

package com.facebook.litho.widget.collection

import androidx.annotation.IdRes
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.EventHandler
import com.facebook.litho.Handle
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.StateValue
import com.facebook.litho.TouchEvent
import com.facebook.litho.annotations.FromTrigger
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.State
import com.facebook.litho.sections.Section
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.SectionTree
import com.facebook.litho.sections.widget.ClearRefreshingEvent
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration
import com.facebook.litho.sections.widget.RecyclerConfiguration
import com.facebook.litho.sections.widget.ScrollEvent
import com.facebook.litho.sections.widget.ScrollToHandle
import com.facebook.litho.sections.widget.SectionBinderTarget
import com.facebook.litho.sections.widget.SmoothScrollEvent
import com.facebook.litho.sections.widget.SmoothScrollToHandleEvent
import com.facebook.litho.widget.Binder
import com.facebook.litho.widget.LithoRecyclerView.TouchInterceptor
import com.facebook.litho.widget.PTRRefreshEvent
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.SectionsRecyclerView.SectionsRecylerViewLogger
import com.facebook.litho.widget.SmoothScrollAlignmentType
import com.facebook.litho.widget.ViewportInfo.ViewportChanged

@LayoutSpec(events = [PTRRefreshEvent::class])
object CollectionRecyclerSpec {

  @get:PropDefault
  val recyclerConfiguration: RecyclerConfiguration = ListRecyclerConfiguration.create().build()

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop section: Section?,
      @Prop(optional = true, varArg = "onScrollListener")
      onScrollListeners: List<RecyclerView.OnScrollListener?>?,
      @Prop(optional = true) clipToPadding: Boolean?,
      @Prop(optional = true) clipChildren: Boolean?,
      @Prop(optional = true) nestedScrollingEnabled: Boolean?,
      @Prop(optional = true) scrollBarStyle: Int?,
      @Prop(optional = true) itemDecoration: RecyclerView.ItemDecoration?,
      @Prop(optional = true) itemAnimator: RecyclerView.ItemAnimator?,
      @Prop(optional = true) @IdRes recyclerViewId: Int?,
      @Prop(optional = true) overScrollMode: Int?,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) startPadding: Int,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) endPadding: Int,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) topPadding: Int,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) bottomPadding: Int,
      @Prop(optional = true) recyclerTouchEventHandler: EventHandler<TouchEvent?>?,
      @Prop(optional = true) horizontalFadingEdgeEnabled: Boolean,
      @Prop(optional = true) verticalFadingEdgeEnabled: Boolean,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) fadingEdgeLength: Int,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarBackgroundColor: Int?,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarColor: Int?,
      @Prop(optional = true) touchInterceptor: TouchInterceptor?,
      @Prop(optional = true) itemTouchListener: RecyclerView.OnItemTouchListener?,
      @Prop(optional = true) pullToRefreshEnabled: Boolean,
      @Prop(optional = true) recyclerConfiguration: RecyclerConfiguration,
      @Prop(optional = true) sectionsViewLogger: SectionsRecylerViewLogger?,
      @State internalRecyclerEventsController: RecyclerEventsController?,
      @State binder: Binder<RecyclerView>,
      @State sectionTree: SectionTree
  ): Component {
    sectionTree.setRoot(section)
    val internalPullToRefreshEnabled =
        (recyclerConfiguration.orientation != OrientationHelper.HORIZONTAL && pullToRefreshEnabled)
    val recycler =
        Recycler.create(c)
            .leftPadding(startPadding)
            .rightPadding(endPadding)
            .topPadding(topPadding)
            .bottomPadding(bottomPadding)
            .recyclerEventsController(internalRecyclerEventsController)
            .refreshHandler(if (!pullToRefreshEnabled) null else CollectionRecycler.onRefresh(c))
            .pullToRefresh(internalPullToRefreshEnabled)
            .itemDecoration(itemDecoration)
            .horizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled)
            .verticalFadingEdgeEnabled(verticalFadingEdgeEnabled)
            .fadingEdgeLengthDip(fadingEdgeLength.toFloat())
            .onScrollListeners(onScrollListeners)
            .refreshProgressBarBackgroundColor(refreshProgressBarBackgroundColor)
            .snapHelper(recyclerConfiguration.snapHelper)
            .touchInterceptor(touchInterceptor)
            .onItemTouchListener(itemTouchListener)
            .binder(binder)
            .touchHandler(recyclerTouchEventHandler)
            .sectionsViewLogger(sectionsViewLogger)
    if (clipToPadding != null) {
      recycler.clipToPadding(clipToPadding)
    }
    if (clipChildren != null) {
      recycler.clipChildren(clipChildren)
    }
    if (nestedScrollingEnabled != null) {
      recycler.nestedScrollingEnabled(nestedScrollingEnabled)
    }
    if (scrollBarStyle != null) {
      recycler.scrollBarStyle(scrollBarStyle)
    }
    if (recyclerViewId != null) {
      recycler.recyclerViewId(recyclerViewId)
    }
    if (overScrollMode != null) {
      recycler.overScrollMode(overScrollMode)
    }
    if (refreshProgressBarColor != null) {
      recycler.refreshProgressBarColor(refreshProgressBarColor)
    }
    if (itemAnimator != null) {
      recycler.itemAnimator(itemAnimator)
    }
    return recycler.build()
  }

  @JvmStatic
  @OnCreateInitialState
  fun createInitialState(
      c: ComponentContext,
      sectionTree: StateValue<SectionTree?>,
      binder: StateValue<Binder<RecyclerView>>,
      internalRecyclerEventsController: StateValue<RecyclerEventsController?>,
      @Prop section: Section,
      @Prop(optional = true) recyclerConfiguration: RecyclerConfiguration,
      @Prop(optional = true) sectionTreeTag: String?,
      @Prop(optional = true) canMeasureRecycler: Boolean,
      @Prop(optional = true) startupLogger: LithoStartupLogger?,
      @Prop(optional = true) recyclerEventsController: RecyclerEventsController?
  ) {
    val binderConfiguration = recyclerConfiguration.recyclerBinderConfiguration
    val recyclerBinderBuilder =
        RecyclerBinder.Builder()
            .layoutInfo(recyclerConfiguration.getLayoutInfo(c))
            .rangeRatio(binderConfiguration.rangeRatio)
            .layoutHandlerFactory(binderConfiguration.layoutHandlerFactory)
            .wrapContent(binderConfiguration.isWrapContent)
            .enableStableIds(binderConfiguration.enableStableIds)
            .invalidStateLogParamsList(binderConfiguration.invalidStateLogParamsList)
            .threadPoolConfig(binderConfiguration.threadPoolConfiguration)
            .hscrollAsyncMode(binderConfiguration.hScrollAsyncMode)
            .isCircular(binderConfiguration.isCircular)
            .hasDynamicItemHeight(binderConfiguration.hasDynamicItemHeight())
            .componentsConfiguration(binderConfiguration.componentsConfiguration)
            .canInterruptAndMoveLayoutsBetweenThreads(
                binderConfiguration.moveLayoutsBetweenThreads())
            .isReconciliationEnabled(binderConfiguration.isReconciliationEnabled)
            .recyclingMode(binderConfiguration.recyclingMode)
            .isLayoutDiffingEnabled(binderConfiguration.isLayoutDiffingEnabled)
            .componentWarmer(binderConfiguration.componentWarmer)
            .lithoViewFactory(binderConfiguration.lithoViewFactory)
            .errorEventHandler(binderConfiguration.errorEventHandler)
            .startupLogger(startupLogger)
    if (binderConfiguration.estimatedViewportCount != RecyclerBinderConfiguration.Builder.UNSET) {
      recyclerBinderBuilder.estimatedViewportCount(binderConfiguration.estimatedViewportCount)
    }
    val recyclerBinder = recyclerBinderBuilder.build(c)
    val targetBinder =
        SectionBinderTarget(recyclerBinder, binderConfiguration.useBackgroundChangeSets)
    binder.set(targetBinder)
    val sectionTreeInstance =
        SectionTree.create(SectionContext(c), targetBinder)
            .tag(
                if (sectionTreeTag == null || sectionTreeTag == "") section.simpleName
                else sectionTreeTag)
            .changeSetThreadHandler(binderConfiguration.changeSetThreadHandler)
            .postToFrontOfQueueForFirstChangeset(
                binderConfiguration.isPostToFrontOfQueueForFirstChangeset)
            .build()
    sectionTree.set(sectionTreeInstance)
    internalRecyclerEventsController.set(recyclerEventsController ?: RecyclerEventsController())
    val viewPortChanged =
        ViewportChanged {
        firstVisibleIndex,
        lastVisibleIndex,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex,
        state ->
      sectionTreeInstance.viewPortChanged(
          firstVisibleIndex, lastVisibleIndex, firstFullyVisibleIndex, lastFullyVisibleIndex, state)
    }
    targetBinder.setViewportChangedListener(viewPortChanged)
    targetBinder.setCanMeasure(canMeasureRecycler)
  }

  @JvmStatic
  @OnEvent(PTRRefreshEvent::class)
  fun onRefresh(c: ComponentContext, @State sectionTree: SectionTree): Boolean {
    val ptrEventHandler = CollectionRecycler.getPTRRefreshEventHandler(c)
    if (ptrEventHandler == null) {
      sectionTree.refresh()
      return true
    }
    val isHandled: Boolean = CollectionRecycler.dispatchPTRRefreshEvent(ptrEventHandler)
    if (!isHandled) {
      sectionTree.refresh()
    }
    return true
  }

  @JvmStatic
  @OnTrigger(ScrollEvent::class)
  fun onScroll(c: ComponentContext, @FromTrigger position: Int, @State sectionTree: SectionTree) {
    sectionTree.requestFocusOnRoot(position)
  }

  @JvmStatic
  @OnTrigger(ScrollToHandle::class)
  fun onScrollToHandle(
      c: ComponentContext,
      @FromTrigger target: Handle?,
      @FromTrigger offset: Int,
      @State sectionTree: SectionTree
  ) {
    sectionTree.requestFocusOnRoot(target, offset)
  }

  @JvmStatic
  @OnTrigger(SmoothScrollEvent::class)
  fun onSmoothScroll(
      c: ComponentContext,
      @FromTrigger index: Int,
      @FromTrigger offset: Int,
      @FromTrigger type: SmoothScrollAlignmentType?,
      @State sectionTree: SectionTree
  ) {
    sectionTree.requestSmoothFocusOnRoot(index, offset, type)
  }

  @JvmStatic
  @OnTrigger(SmoothScrollToHandleEvent::class)
  fun onSmoothScrollToHandle(
      c: ComponentContext,
      @FromTrigger target: Handle?,
      @FromTrigger offset: Int,
      @FromTrigger type: SmoothScrollAlignmentType?,
      @State sectionTree: SectionTree
  ) {
    sectionTree.requestSmoothFocusOnRoot(target, offset, type)
  }

  @JvmStatic
  @OnTrigger(ClearRefreshingEvent::class)
  fun onClearRefreshing(
      c: ComponentContext,
      @State internalRecyclerEventsController: RecyclerEventsController
  ) {
    internalRecyclerEventsController.clearRefreshing()
  }

  @JvmStatic
  @OnDetached
  fun onDetached(c: ComponentContext, @State binder: Binder<RecyclerView>) {
    binder.detach()
  }
}
