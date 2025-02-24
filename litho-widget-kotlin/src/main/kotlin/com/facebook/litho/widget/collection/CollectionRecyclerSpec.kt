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

import android.graphics.Color
import android.view.View
import androidx.annotation.IdRes
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.EventHandler
import com.facebook.litho.LithoStartupLogger
import com.facebook.litho.StateValue
import com.facebook.litho.StyleCompat
import com.facebook.litho.TouchEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnDetached
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.State
import com.facebook.litho.config.PrimitiveRecyclerBinderStrategy
import com.facebook.litho.sections.Section
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.SectionTree
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.NoUpdateItemAnimator
import com.facebook.litho.sections.widget.RecyclerConfiguration
import com.facebook.litho.sections.widget.SectionBinderTarget
import com.facebook.litho.widget.Binder
import com.facebook.litho.widget.LithoRecyclerView.OnAfterLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.OnBeforeLayoutListener
import com.facebook.litho.widget.LithoRecyclerView.TouchInterceptor
import com.facebook.litho.widget.PTRRefreshEvent
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder
import com.facebook.litho.widget.RecyclerEventsController
import com.facebook.litho.widget.SectionsRecyclerView.SectionsRecyclerViewLogger
import com.facebook.litho.widget.ViewportInfo.ViewportChanged
import com.facebook.rendercore.PoolScope

@LayoutSpec(events = [PTRRefreshEvent::class])
object CollectionRecyclerSpec {

  @get:PropDefault
  val recyclerConfiguration: RecyclerConfiguration = ListRecyclerConfiguration.create().build()

  @PropDefault val itemAnimator: RecyclerView.ItemAnimator = NoUpdateItemAnimator()
  @PropDefault val isLeftFadingEnabled: Boolean = true
  @PropDefault val isRightFadingEnabled: Boolean = true
  @PropDefault val isTopFadingEnabled: Boolean = true
  @PropDefault val isBottomFadingEnabled: Boolean = true

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
      @Prop(optional = true) recyclerTouchEventHandler: EventHandler<TouchEvent>?,
      @Prop(optional = true) horizontalFadingEdgeEnabled: Boolean,
      @Prop(optional = true) verticalFadingEdgeEnabled: Boolean,
      @Prop(optional = true) isLeftFadingEnabled: Boolean,
      @Prop(optional = true) isRightFadingEnabled: Boolean,
      @Prop(optional = true) isTopFadingEnabled: Boolean,
      @Prop(optional = true) isBottomFadingEnabled: Boolean,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) fadingEdgeLength: Int,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarBackgroundColor: Int?,
      @Prop(optional = true, resType = ResType.COLOR) refreshProgressBarColor: Int?,
      @Prop(optional = true) touchInterceptor: TouchInterceptor?,
      @Prop(optional = true) itemTouchListener: RecyclerView.OnItemTouchListener?,
      @Prop(optional = true) pullToRefreshEnabled: Boolean,
      @Prop(optional = true) recyclerConfiguration: RecyclerConfiguration,
      @Prop(optional = true) sectionsViewLogger: SectionsRecyclerViewLogger?,
      @Prop(optional = true) shouldExcludeFromIncrementalMount: Boolean,
      @Prop(optional = true) onBeforeLayoutListener: OnBeforeLayoutListener?,
      @Prop(optional = true) onAfterLayoutListener: OnAfterLayoutListener?,
      @State internalRecyclerEventsController: RecyclerEventsController?,
      @State binder: Binder<RecyclerView>,
      @State sectionTree: SectionTree
  ): Component {
    sectionTree.setRoot(section)
    val internalPullToRefreshEnabled =
        (recyclerConfiguration.orientation != OrientationHelper.HORIZONTAL && pullToRefreshEnabled)

    val componentsConfiguration = c.lithoConfiguration.componentsConfig

    val primitiveRecyclerBinderStrategy =
        recyclerConfiguration.recyclerBinderConfiguration.primitiveRecyclerBinderStrategy
            ?: componentsConfiguration.primitiveRecyclerBinderStrategy

    /**
     * This is a temporary solution while we experiment with offering the same behavior regarding
     * the default item animators as in
     * [com.facebook.litho.sections.widget.RecyclerCollectionComponent].
     *
     * This is needed because we will have a crash if we re-use the same animator instance across
     * different RV instances. In this approach we identify if the client opted by using the
     * "default" animator, and if so, it will pass on a new instance of the same type, to avoid a
     * crash that happens due to re-using the same instances in different RVs.
     */
    val itemAnimatorToUse =
        when (itemAnimator) {
          CollectionRecyclerSpec.itemAnimator -> {
            if (c.lithoConfiguration.componentsConfig.useDefaultItemAnimatorInLazyCollections &&
                c.lithoConfiguration.componentsConfig.primitiveRecyclerBinderStrategy ==
                    PrimitiveRecyclerBinderStrategy.SPLIT_BINDERS) {
              NoUpdateItemAnimator()
            } else {
              null
            }
          }
          else -> itemAnimator
        }

    return Recycler(
        binderStrategy = primitiveRecyclerBinderStrategy,
        binder = binder,
        isClipToPaddingEnabled = clipToPadding ?: true,
        isClipChildrenEnabled = clipChildren ?: true,
        isNestedScrollingEnabled = nestedScrollingEnabled ?: true,
        scrollBarStyle = scrollBarStyle ?: View.SCROLLBARS_INSIDE_OVERLAY,
        leftPadding = startPadding,
        rightPadding = endPadding,
        topPadding = topPadding,
        bottomPadding = bottomPadding,
        recyclerEventsController = internalRecyclerEventsController,
        isPullToRefreshEnabled = internalPullToRefreshEnabled,
        onRefresh =
            if (internalPullToRefreshEnabled) {
              { refreshContent(c, sectionTree) }
            } else null,
        itemDecorations = itemDecoration?.let { listOf(it) },
        isHorizontalFadingEdgeEnabled = horizontalFadingEdgeEnabled,
        isVerticalFadingEdgeEnabled = verticalFadingEdgeEnabled,
        isLeftFadingEnabled = isLeftFadingEnabled,
        isRightFadingEnabled = isRightFadingEnabled,
        isTopFadingEnabled = isTopFadingEnabled,
        isBottomFadingEnabled = isBottomFadingEnabled,
        fadingEdgeLength = fadingEdgeLength,
        onScrollListeners = onScrollListeners?.filterNotNull(),
        refreshProgressBarBackgroundColor = refreshProgressBarBackgroundColor,
        snapHelper = recyclerConfiguration.snapHelper,
        touchInterceptor = touchInterceptor,
        onItemTouchListener = itemTouchListener,
        excludeFromIncrementalMount = shouldExcludeFromIncrementalMount,
        sectionsViewLogger = sectionsViewLogger,
        itemAnimator = itemAnimatorToUse,
        refreshProgressBarColor = refreshProgressBarColor ?: Color.BLACK,
        recyclerViewId = recyclerViewId ?: View.NO_ID,
        overScrollMode = overScrollMode ?: View.OVER_SCROLL_ALWAYS,
        onBeforeLayoutListener = onBeforeLayoutListener,
        onAfterLayoutListener = onAfterLayoutListener,
        style = StyleCompat.touchHandler(recyclerTouchEventHandler).build())
  }

  @JvmStatic
  @OnCreateInitialState
  fun createInitialState(
      c: ComponentContext,
      sectionTree: StateValue<SectionTree?>,
      binder: StateValue<Binder<RecyclerView>>,
      internalRecyclerEventsController: StateValue<RecyclerEventsController?>,
      collectionRecyclerPoolScope: StateValue<PoolScope.ManuallyManaged>,
      @Prop section: Section,
      @Prop(optional = true) recyclerConfiguration: RecyclerConfiguration,
      @Prop(optional = true) sectionTreeTag: String?,
      @Prop(optional = true) startupLogger: LithoStartupLogger?,
      @Prop(optional = true) lazyCollectionController: LazyCollectionController?
  ) {
    val binderConfiguration = recyclerConfiguration.recyclerBinderConfiguration

    val poolScope = PoolScope.ManuallyManaged()
    collectionRecyclerPoolScope.set(poolScope)

    val recyclerBinder =
        RecyclerBinder.Builder()
            .layoutInfo(recyclerConfiguration.getLayoutInfo(c))
            .startupLogger(startupLogger)
            .recyclerBinderConfig(binderConfiguration.recyclerBinderConfig)
            .poolScope(poolScope)
            .build(c)

    val targetBinder =
        SectionBinderTarget(recyclerBinder, binderConfiguration.useBackgroundChangeSets)
    binder.set(targetBinder)
    val sectionTreeInstance =
        SectionTree.create(SectionContext(c), targetBinder)
            .tag(if (sectionTreeTag.isNullOrEmpty()) section.simpleName else sectionTreeTag)
            .changeSetThreadHandler(binderConfiguration.changeSetThreadHandler)
            .postToFrontOfQueueForFirstChangeset(
                binderConfiguration.isPostToFrontOfQueueForFirstChangeset)
            .build()
    sectionTree.set(sectionTreeInstance)
    lazyCollectionController?.scrollerDelegate =
        ScrollerDelegate.SectionTreeScroller(sectionTreeInstance)
    val recyclerEventsController = RecyclerEventsController()
    lazyCollectionController?.recyclerEventsController = recyclerEventsController
    internalRecyclerEventsController.set(recyclerEventsController)
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
  }

  @JvmStatic
  @OnEvent(PTRRefreshEvent::class)
  fun onRefresh(c: ComponentContext, @State sectionTree: SectionTree): Boolean {
    return refreshContent(c, sectionTree)
  }

  private fun refreshContent(c: ComponentContext, sectionTree: SectionTree): Boolean {
    val ptrEventHandler = CollectionRecycler.getPTRRefreshEventHandler(c)
    if (ptrEventHandler == null) {
      sectionTree.refresh()
      return true
    }
    val isHandled = CollectionRecycler.dispatchPTRRefreshEvent(ptrEventHandler)
    if (!isHandled) {
      sectionTree.refresh()
    }
    return true
  }

  @JvmStatic
  @OnDetached
  fun onDetached(
      c: ComponentContext,
      @State binder: Binder<RecyclerView>,
      @State collectionRecyclerPoolScope: PoolScope.ManuallyManaged
  ): Unit {
    binder.detach()
    collectionRecyclerPoolScope.releaseScope()
  }
}
