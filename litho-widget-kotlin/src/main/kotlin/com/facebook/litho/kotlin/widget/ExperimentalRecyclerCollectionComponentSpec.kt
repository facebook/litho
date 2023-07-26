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

package com.facebook.litho.kotlin.widget

import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.SnapHelper
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.Component.ContainerBuilder
import com.facebook.litho.ComponentContext
import com.facebook.litho.EventHandler
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.StateValue
import com.facebook.litho.Style
import com.facebook.litho.TouchEvent
import com.facebook.litho.Wrapper
import com.facebook.litho.annotations.FromTrigger
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.State
import com.facebook.litho.flexbox.flex
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.sections.BaseLoadEventsHandler
import com.facebook.litho.sections.LoadEventsHandler
import com.facebook.litho.sections.Section
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.SectionTree
import com.facebook.litho.sections.SectionTree.Target.DynamicConfig
import com.facebook.litho.sections.widget.ClearRefreshingEvent
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.NoUpdateItemAnimator
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController
import com.facebook.litho.sections.widget.RecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerDynamicConfigEvent
import com.facebook.litho.sections.widget.ScrollEvent
import com.facebook.litho.sections.widget.SectionBinderTarget
import com.facebook.litho.view.onTouch
import com.facebook.litho.widget.Binder
import com.facebook.litho.widget.LayoutInfo
import com.facebook.litho.widget.LithoRecyclerView.TouchInterceptor
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.RecyclerBinder.CommitPolicy
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.SectionsRecyclerView.SectionsRecyclerViewLogger
import com.facebook.litho.widget.StickyHeaderControllerFactory
import com.facebook.litho.widget.ViewportInfo.ViewportChanged
import com.facebook.rendercore.dp
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaPositionType
import main.kotlin.com.facebook.litho.kotlin.widget.ExperimentalRecyclerWrapper

/**
 * Experimental implementation of {@link RecyclerCollectionComponentSpec} that wraps a {@link
 * ExperimentalRecycler} instead of {@link RecyclerSpec}
 */
@LayoutSpec
object ExperimentalRecyclerCollectionComponentSpec {

  @get:PropDefault
  val recyclerConfiguration: RecyclerConfiguration = ListRecyclerConfiguration.create().build()
  @get:PropDefault val nestedScrollingEnabled: Boolean = true
  @get:PropDefault val scrollBarStyle: Int = View.SCROLLBARS_INSIDE_OVERLAY
  @get:PropDefault val recyclerViewId: Int = View.NO_ID
  @get:PropDefault val overScrollMode: Int = View.OVER_SCROLL_ALWAYS
  @get:PropDefault internal val asyncStateUpdates: Boolean = false
  @get:PropDefault val itemAnimator: ItemAnimator = NoUpdateItemAnimator()
  @get:PropDefault internal val asyncPropUpdates: Boolean = false
  @get:PropDefault internal val setRootAsync: Boolean = false
  @get:PropDefault val clipToPadding: Boolean = true
  @get:PropDefault val clipChildren: Boolean = true
  @get:PropDefault val incrementalMount: Boolean = true
  @get:PropDefault val refreshProgressBarColor: Int = -0xbd984e // blue
  @get:PropDefault val useTwoBindersRecycler: Boolean = false
  @get:PropDefault val enableSeparateAnimatorBinder: Boolean = false

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext?,
      @Prop section: Section?,
      @Prop(optional = true) loadingComponent: Component?,
      @Prop(optional = true) emptyComponent: Component?,
      @Prop(optional = true) errorComponent: Component?,
      @Prop(optional = true, varArg = "onScrollListener")
      onScrollListeners: List<OnScrollListener?>?,
      @Prop(optional = true) loadEventsHandler: LoadEventsHandler?,
      @Prop(optional = true) clipToPadding: Boolean,
      @Prop(optional = true) clipChildren: Boolean,
      @Prop(optional = true) nestedScrollingEnabled: Boolean,
      @Prop(optional = true) scrollBarStyle: Int,
      @Prop(optional = true) itemDecoration: ItemDecoration?,
      @Prop(optional = true) itemAnimator: ItemAnimator?,
      @Prop(optional = true) ignoreLoadingUpdates: Boolean,
      @Prop(optional = true) @IdRes recyclerViewId: Int,
      @Prop(optional = true) overScrollMode: Int,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) leftPadding: Int,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) rightPadding: Int,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) topPadding: Int,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) bottomPadding: Int,
      @Prop(optional = true) recyclerTouchEventHandler: EventHandler<TouchEvent?>?,
      @Prop(optional = true) horizontalFadingEdgeEnabled: Boolean,
      @Prop(optional = true) verticalFadingEdgeEnabled: Boolean,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) fadingEdgeLength: Int,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarBackgroundColor: Int?,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarColor: Int,
      @Prop(optional = true) touchInterceptor: TouchInterceptor?,
      @Prop(optional = true) itemTouchListener: OnItemTouchListener?,
      @Prop(optional = true) setRootAsync: Boolean,
      @Prop(optional = true) disablePTR: Boolean,
      @Prop(optional = true) recyclerConfiguration: RecyclerConfiguration,
      @Prop(optional = true) sectionsViewLogger: SectionsRecyclerViewLogger?,
      @Prop(optional = true) useTwoBindersRecycler: Boolean,
      @Prop(optional = true) enableSeparateAnimatorBinder: Boolean,
      @State(canUpdateLazily = true) hasSetSectionTreeRoot: Boolean,
      @State internalEventsController: RecyclerCollectionEventsController,
      @State layoutInfo: LayoutInfo,
      @State loadingState: LoadingState,
      @State binder: Binder<RecyclerView>,
      @State sectionTree: SectionTree,
      @State recyclerCollectionLoadEventsHandler: RecyclerCollectionLoadEventsHandler,
      @State snapHelper: SnapHelper?
  ): Component? {

    // This is a side effect from OnCreateLayout, so it's inherently prone to race conditions:
    recyclerCollectionLoadEventsHandler.setLoadEventsHandler(loadEventsHandler)

    // More side effects in OnCreateLayout. Watch out:
    if (hasSetSectionTreeRoot && setRootAsync) {
      sectionTree.setRootAsync(section)
    } else {
      ExperimentalRecyclerCollectionComponent.lazyUpdateHasSetSectionTreeRoot(c, true)
      sectionTree.setRoot(section)
    }
    val isErrorButNoErrorComponent = loadingState == LoadingState.ERROR && errorComponent == null
    val isEmptyButNoEmptyComponent = loadingState == LoadingState.EMPTY && emptyComponent == null
    val shouldHideComponent = isEmptyButNoEmptyComponent || isErrorButNoErrorComponent
    if (shouldHideComponent) {
      return null
    }
    val canPTR = recyclerConfiguration.orientation != OrientationHelper.HORIZONTAL && !disablePTR
    val onScrolledListener =
        object : RecyclerView.OnScrollListener() {
          override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            internalEventsController.updateFirstLastFullyVisibleItemPositions(layoutInfo)
          }
        }
    val onScrollListenersList =
        if (onScrollListeners != null) onScrollListeners.toMutableList() else mutableListOf()
    onScrollListenersList.add(onScrolledListener)
    val touchStyle =
        if (recyclerTouchEventHandler != null)
            Style.onTouch {
              recyclerTouchEventHandler?.dispatchEvent(it)
              true
            }
        else null
    val positionStyle =
        if (!binder.canMeasure() &&
            !recyclerConfiguration.recyclerBinderConfiguration.isWrapContent) {
          Style.positionType(YogaPositionType.ABSOLUTE).position(all = 0.dp)
        } else null
    val recycler =
        ExperimentalRecyclerWrapper(
            style = Style.flex(shrink = 0f) + touchStyle + positionStyle,
            isClipToPaddingEnabled = clipToPadding,
            isClipChildrenEnabled = clipChildren,
            nestedScrollingEnabled = nestedScrollingEnabled,
            scrollBarStyle = scrollBarStyle,
            recyclerViewId = recyclerViewId,
            overScrollMode = overScrollMode,
            recyclerEventsController = internalEventsController,
            onRefresh =
                if (!canPTR) null
                else {
                  { sectionTree.refresh() }
                },
            pullToRefresh = canPTR,
            itemDecoration = itemDecoration,
            horizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled,
            verticalFadingEdgeEnabled = verticalFadingEdgeEnabled,
            fadingEdgeLength = fadingEdgeLength.dp,
            onScrollListeners = onScrollListenersList,
            refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
            refreshProgressBarColor = refreshProgressBarColor,
            snapHelper = snapHelper,
            touchInterceptor = touchInterceptor,
            onItemTouchListener = itemTouchListener,
            binder = binder,
            itemAnimator = itemAnimator,
            sectionsViewLogger = sectionsViewLogger,
            useTwoBindersRecycler = useTwoBindersRecycler,
            enableSeparateAnimatorBinder = enableSeparateAnimatorBinder,
            leftPadding = leftPadding,
            topPadding = topPadding,
            rightPadding = rightPadding,
            bottomPadding = bottomPadding)
    val containerBuilder: ContainerBuilder<*> =
        Column.create(c).flexShrink(0f).alignContent(YogaAlign.FLEX_START).child(recycler)
    if (loadingState == LoadingState.LOADING && loadingComponent != null) {
      containerBuilder.child(
          Wrapper.create(c)
              .delegate(loadingComponent)
              .flexShrink(0f)
              .positionType(YogaPositionType.ABSOLUTE)
              .positionPx(YogaEdge.ALL, 0))
    } else if (loadingState == LoadingState.EMPTY) {
      containerBuilder.child(
          Wrapper.create(c)
              .delegate(emptyComponent)
              .flexShrink(0f)
              .positionType(YogaPositionType.ABSOLUTE)
              .positionPx(YogaEdge.ALL, 0))
    } else if (loadingState == LoadingState.ERROR) {
      containerBuilder.child(
          Wrapper.create(c)
              .delegate(errorComponent)
              .flexShrink(0f)
              .positionType(YogaPositionType.ABSOLUTE)
              .positionPx(YogaEdge.ALL, 0))
    }
    return containerBuilder.build()
  }

  @OnCreateInitialState
  fun createInitialState(
      c: ComponentContext,
      snapHelper: StateValue<SnapHelper?>,
      sectionTree: StateValue<SectionTree?>,
      recyclerCollectionLoadEventsHandler: StateValue<RecyclerCollectionLoadEventsHandler?>,
      binder: StateValue<Binder<RecyclerView>?>,
      loadingState: StateValue<LoadingState?>,
      internalEventsController: StateValue<RecyclerCollectionEventsController?>,
      layoutInfo: StateValue<LayoutInfo?>,
      @Prop section: Section,
      @Prop(optional = true) recyclerConfiguration: RecyclerConfiguration,
      @Prop(optional = true) eventsController: RecyclerCollectionEventsController?,
      @Prop(optional = true) asyncPropUpdates: Boolean,
      @Prop(optional = true)
      asyncStateUpdates:
          Boolean, // NB: This is a *workaround* for sections that use non-threadsafe models, e.g.
      // models that
      // may be updated from the main thread while background changesets would be calculated. It has
      // negative performance implications since it forces all changesets to be calculated on the
      // main thread!
      @Prop(optional = true) forceSyncStateUpdates: Boolean, // Caution: ignoreLoadingUpdates breaks
      // loadingComponent/errorComponent/emptyComponent.
      // It's intended to be a temporary workaround, not something you should use often.
      @Prop(optional = true) ignoreLoadingUpdates: Boolean,
      @Prop(optional = true) sectionTreeTag: String?,
      @Prop(optional = true)
      canMeasureRecycler:
          Boolean, // Don't use this. If false, off incremental mount for all subviews of this
      // Recycler.
      @Prop(optional = true) incrementalMount: Boolean,
      @Prop(optional = true) startupLogger: LithoStartupLogger?,
      @Prop(optional = true) stickyHeaderControllerFactory: StickyHeaderControllerFactory?
  ) {
    val binderConfiguration = recyclerConfiguration.recyclerBinderConfiguration
    val newLayoutInfo = recyclerConfiguration.getLayoutInfo(c)
    layoutInfo.set(newLayoutInfo)
    val recyclerBinderBuilder =
        RecyclerBinder.Builder()
            .layoutInfo(newLayoutInfo)
            .rangeRatio(binderConfiguration.rangeRatio)
            .layoutHandlerFactory(binderConfiguration.layoutHandlerFactory)
            .wrapContent(binderConfiguration.isWrapContent)
            .enableStableIds(binderConfiguration.enableStableIds)
            .invalidStateLogParamsList(binderConfiguration.invalidStateLogParamsList)
            .threadPoolConfig(binderConfiguration.threadPoolConfiguration)
            .hscrollAsyncMode(binderConfiguration.hScrollAsyncMode)
            .isCircular(binderConfiguration.isCircular)
            .hasDynamicItemHeight(binderConfiguration.hasDynamicItemHeight())
            .incrementalMount(incrementalMount)
            .stickyHeaderControllerFactory(stickyHeaderControllerFactory)
            .componentsConfiguration(binderConfiguration.componentsConfiguration)
            .isReconciliationEnabled(binderConfiguration.isReconciliationEnabled)
            .isLayoutDiffingEnabled(binderConfiguration.isLayoutDiffingEnabled)
            .componentWarmer(binderConfiguration.componentWarmer)
            .lithoViewFactory(binderConfiguration.lithoViewFactory)
            .errorEventHandler(binderConfiguration.errorEventHandler)
            .recyclerViewItemPrefetch(binderConfiguration.enableItemPrefetch)
            .startupLogger(startupLogger)
    if (binderConfiguration.estimatedViewportCount != RecyclerBinderConfiguration.Builder.UNSET) {
      recyclerBinderBuilder.estimatedViewportCount(binderConfiguration.estimatedViewportCount)
    }
    val recyclerBinder = recyclerBinderBuilder.build(c)
    val targetBinder =
        SectionBinderTarget(recyclerBinder, binderConfiguration.useBackgroundChangeSets)
    val sectionContext = SectionContext(c)
    binder.set(targetBinder)
    snapHelper.set(recyclerConfiguration.snapHelper)
    val sectionTreeInstance =
        SectionTree.create(sectionContext, targetBinder)
            .tag(
                if (sectionTreeTag == null || sectionTreeTag == "") section.simpleName
                else sectionTreeTag)
            .asyncPropUpdates(asyncPropUpdates)
            .asyncStateUpdates(asyncStateUpdates)
            .forceSyncStateUpdates(forceSyncStateUpdates)
            .changeSetThreadHandler(binderConfiguration.changeSetThreadHandler)
            .postToFrontOfQueueForFirstChangeset(
                binderConfiguration.isPostToFrontOfQueueForFirstChangeset)
            .build()
    sectionTree.set(sectionTreeInstance)
    val internalEventsControllerInstance = eventsController ?: RecyclerCollectionEventsController()
    internalEventsControllerInstance.setSectionTree(sectionTreeInstance)
    internalEventsControllerInstance.setSnapMode(recyclerConfiguration.snapMode)
    internalEventsController.set(internalEventsControllerInstance)
    val recyclerCollectionLoadEventsHandlerInstance =
        RecyclerCollectionLoadEventsHandler(
            c, internalEventsControllerInstance, ignoreLoadingUpdates)
    recyclerCollectionLoadEventsHandler.set(recyclerCollectionLoadEventsHandlerInstance)
    sectionTreeInstance.setLoadEventsHandler(recyclerCollectionLoadEventsHandlerInstance)
    val viewPortChanged =
        ViewportChanged {
            firstVisibleIndex,
            lastVisibleIndex,
            firstFullyVisibleIndex,
            lastFullyVisibleIndex,
            state ->
          sectionTreeInstance.viewPortChanged(
              firstVisibleIndex,
              lastVisibleIndex,
              firstFullyVisibleIndex,
              lastFullyVisibleIndex,
              state)
        }
    targetBinder.setViewportChangedListener(viewPortChanged)
    targetBinder.setCanMeasure(canMeasureRecycler)
    if (ignoreLoadingUpdates) {
      loadingState.set(LoadingState.LOADED)
    } else {
      loadingState.set(LoadingState.LOADING)
    }
  }

  @OnUpdateState
  fun updateLoadingState(
      loadingState: StateValue<LoadingState?>,
      @Param currentLoadingState: LoadingState?
  ) {
    loadingState.set(currentLoadingState)
  }

  @OnTrigger(ScrollEvent::class)
  fun onScroll(
      c: ComponentContext?,
      @FromTrigger position: Int,
      @FromTrigger animate: Boolean,
      @State sectionTree: SectionTree,
      @State internalEventsController: RecyclerCollectionEventsController?
  ) {
    sectionTree.requestFocusOnRoot(position)
  }

  @OnTrigger(RecyclerDynamicConfigEvent::class)
  fun onRecyclerConfigChanged(
      c: ComponentContext?,
      @FromTrigger @CommitPolicy commitPolicy: Int,
      @State sectionTree: SectionTree
  ) {
    sectionTree.setTargetConfig(DynamicConfig(commitPolicy))
  }

  @OnTrigger(ClearRefreshingEvent::class)
  fun onClearRefreshing(
      c: ComponentContext?,
      @State internalEventsController: RecyclerCollectionEventsController
  ) {
    internalEventsController.clearRefreshing()
  }

  @OnDetached
  fun onDetached(c: ComponentContext?, @State binder: Binder<RecyclerView>) {
    binder.detach()
  }

  @VisibleForTesting
  enum class LoadingState {
    /** We're loading but don't have any content yet. */
    LOADING,

    /** A load completed and we have content. */
    LOADED,

    /** A load completed, but the content is empty. */
    EMPTY,

    /** A load failed with an error. */
    ERROR
  }

  class RecyclerCollectionLoadEventsHandler(
      private val componentContext: ComponentContext,
      private val recyclerEventsController: RecyclerEventsController,
      private val ignoreLoadingUpdates: Boolean
  ) : BaseLoadEventsHandler() {
    private var delegate: LoadEventsHandler? = null
    private var lastState = LoadingState.LOADING

    /** May be called from any thread (in OnCreateLayout). (Does this need synchronization?) */
    fun setLoadEventsHandler(delegate: LoadEventsHandler?) {
      this.delegate = delegate
    }

    /**
     * One would hope this is only called from one thread, since onLoadSucceeded could arrive before
     * onLoadStarted if you post them on different threads. But use synchronized to defend against
     * bad clients.
     *
     * This method exists to avoid thrashing Litho with state updates as we do a bunch of load
     * operations. In theory you could call updateLoadingStateAsync every single time and get the
     * same result, but it's more efficient to avoid all the unnecessary updates.
     */
    @Synchronized
    private fun updateState(newState: LoadingState) {
      if (ignoreLoadingUpdates) {
        return
      }
      if (lastState != newState) {
        lastState = newState
        ExperimentalRecyclerCollectionComponent.updateLoadingStateAsync(componentContext, newState)
      }
    }

    override fun onLoadStarted(empty: Boolean) {
      updateState(if (empty) LoadingState.LOADING else LoadingState.LOADED)
      val delegate = delegate
      delegate?.onLoadStarted(empty)
    }

    override fun onLoadSucceeded(empty: Boolean) {
      updateState(if (empty) LoadingState.EMPTY else LoadingState.LOADED)
      recyclerEventsController.clearRefreshing()
      val delegate = delegate
      delegate?.onLoadSucceeded(empty)
    }

    override fun onLoadFailed(empty: Boolean) {
      updateState(if (empty) LoadingState.ERROR else LoadingState.LOADED)
      recyclerEventsController.clearRefreshing()
      val delegate = delegate
      delegate?.onLoadFailed(empty)
    }

    override fun onInitialLoad() {
      val delegate = delegate
      delegate?.onInitialLoad()
    }
  }
}
