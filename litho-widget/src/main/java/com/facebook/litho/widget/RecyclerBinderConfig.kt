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

package com.facebook.litho.widget

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.Mode
import com.facebook.litho.ErrorEventHandler
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.LayoutThreadPoolConfiguration
import com.facebook.rendercore.RunnableHandler

/**
 * This configuration is meant to be used in the context of [RecyclerBinder]. It allows you to
 * define to define specific behavior changes to the default behaviour of the RecyclerBinder.
 *
 * At this point, we are still in a transition phase where a lot of configs still live in the
 * [RecyclerBinder.Builder], but we aim to move all of them here.
 */
@DataClassGenerate(toString = Mode.OMIT, equalsHashCode = Mode.KEEP)
data class RecyclerBinderConfig(
    /**
     * Whether the underlying RecyclerBinder will have a circular behaviour. Defaults to false.
     * Note: circular lists DO NOT support any operation that changes the size of items like insert,
     * remove, insert range, remove range
     */
    @JvmField val isCircular: Boolean = false,
    /**
     * The factory that will be used to create the nested [com.facebook.litho.LithoView] inside
     * Section/LazyCollection.
     */
    @JvmField val lithoViewFactory: LithoViewFactory? = null,
    /**
     * Experimental. Configuration to change the behavior of HScroll's when they are nested within a
     * vertical scroll. With this mode, the hscroll will attempt to compute all layouts in the
     * background before mounting so that no layouts are computed on the main thread. All subsequent
     * insertions will be treated with LAYOUT_BEFORE_INSERT policy to ensure those layouts also do
     * not happen on the main thread.
     */
    @JvmField val hScrollAsyncMode: Boolean = false,
    /**
     * Enable pre-mounting for pre-fetched items, which requires to turn on RecyclerView's item
     * prefetching first.
     *
     * @see recyclerViewItemPrefetch
     */
    @JvmField val requestMountForPrefetchedItems: Boolean = false,
    /**
     * Set whether item prefetch should be enabled on the underlying RecyclerView.LayoutManager.
     * Defaults to false.
     *
     * <p>ItemPrefetching feature of RecyclerView clashes with RecyclerBinder's compute range
     * optimization and in certain scenarios (like sticky header) it might reset ComponentTree of
     * LithoView while it is still on screen making it render blank or zero height.
     *
     * <p>As ItemPrefetching is built on top of item view cache, please do remember to set a proper
     * cache size if you want to enable this feature. Otherwise, prefetched item will be thrown into
     * the recycler pool immediately. See [RecyclerBinder.Builder#setItemViewCacheSize]¬
     */
    @JvmField val recyclerViewItemPrefetch: Boolean = false,
    /**
     * Set the number of offscreen views to retain before adding them to the potentially shared
     * recycled view pool.
     *
     * <p>The offscreen view cache stays aware of changes in the attached adapter, allowing a
     * LayoutManager to reuse those views unmodified without needing to return to the adapter to
     * rebind them.
     */
    @JvmField val itemViewCacheSize: Int = 0,
    /**
     * Set pool for pre-computing and storing [ComponentTree], which can be used to pre-compute and
     * store ComponentTrees before they are inserted in a [RecyclerBinder].
     *
     * @see [ComponentWarmer]
     */
    @JvmField val componentWarmer: ComponentWarmer? = null,
    /**
     * This is used in very specific cases on critical performance paths where measuring the first
     * item cannot be relied on to estimate the viewport count. It should not be used in the common
     * case, use with caution.
     */
    @JvmField val estimatedViewportCount: Int? = null,
    /**
     * Do not enable this. This is an experimental feature and your Section surface will take a perf
     * hit if you use it.
     *
     * <p>Whether the items of this RecyclerBinder can change height after the initial measure. Only
     * applicable to horizontally scrolling RecyclerBinders. If true, the children of this h-scroll
     * are all measured with unspecified height. When the ComponentTree of a child is remeasured,
     * this will cause the RecyclerBinder to remeasure in case the height of the child changed and
     * the RecyclerView needs to have a different height to account for it. This only supports
     * changing the height of the item that triggered the remeasuring, not the height of all items
     * in the h-scroll.
     */
    @JvmField val hasDynamicItemHeight: Boolean = false,
    /**
     * RecyclerBinder will use this [LayoutThreadPoolConfiguration] to create
     * [com.facebook.litho.ThreadPoolLayoutHandler] this will create a new separate thread pool
     * which might negatively affect the app's [RecyclerBinder.Builder.layoutHandlerFactory] is
     * provided, the handler created by the factory will be used instead of the one that would have
     * been created by this config.
     */
    @JvmField val threadPoolConfig: LayoutThreadPoolConfiguration? = null,
    @JvmField val errorEventHandler: ErrorEventHandler? = null,
    @JvmField val reconciliationEnabled: Boolean = ComponentsConfiguration.isReconciliationEnabled,
    /**
     * Whether the [Recycler] children should preallocate mount content after being generated. This
     * will only work if the root [com.facebook.litho.ComponentTree] has set a preallocation
     * handler, since it will try to use it to run the preallocation.
     *
     * @see [com.facebook.litho.ComponentTree.Builder.useDefaultHandlerForContentPreallocation]
     * @see [com.facebook.litho.ComponentTree.Builder.shouldPreallocateMountContentPerMountSpec]
     */
    @JvmField val preallocateMountContent: Boolean = false,
    /**
     * Define a different [com.facebook.RunnableHandler] to run the preallocation process. This
     * handler is only used if [RecyclerBinderConfig.preallocateMountContent] is enabled.
     */
    @JvmField val preallocateMountContentHandler: RunnableHandler? = null
) {

  init {
    if (estimatedViewportCount != null) {
      require(estimatedViewportCount > 0) {
        "Estimated viewport count must be > 0: $estimatedViewportCount"
      }
    }
  }

  companion object {
    private val default: RecyclerBinderConfig = RecyclerBinderConfig()

    @JvmStatic
    fun create(configuration: RecyclerBinderConfig): RecyclerBinderConfigBuilder {
      return RecyclerBinderConfigBuilder(configuration)
    }

    @JvmStatic
    fun create(): RecyclerBinderConfigBuilder {
      return RecyclerBinderConfigBuilder(default)
    }
  }
}

/**
 * This builder is just a helper class for Java clients.
 *
 * It allows the configuration of a builder in a fluent way:
 * ```
 * val recyclerBinderConfig = RecyclerBinderConfig.create()
 *    .isCircular(true)
 *    .build();
 * ```
 */
class RecyclerBinderConfigBuilder internal constructor(configuration: RecyclerBinderConfig) {

  private var isCircular = configuration.isCircular
  private var hScrollAsyncMode = configuration.hScrollAsyncMode
  private var lithoViewFactory = configuration.lithoViewFactory
  private var componentWarmer = configuration.componentWarmer
  private var estimatedViewportCount = configuration.estimatedViewportCount
  private var requestMountForPrefetchedItems = configuration.requestMountForPrefetchedItems
  private var recyclerViewItemPrefetch = configuration.recyclerViewItemPrefetch
  private var itemViewCacheSize = configuration.itemViewCacheSize
  private var hasDynamicItemHeight = configuration.hasDynamicItemHeight
  private var threadPoolConfig = configuration.threadPoolConfig
  private var errorEventHandler = configuration.errorEventHandler
  private var reconciliationEnabled = configuration.reconciliationEnabled
  private var preallocateMountContent = configuration.preallocateMountContent
  private var preallocateMountContentHandler = configuration.preallocateMountContentHandler

  fun isCircular(isCircular: Boolean): RecyclerBinderConfigBuilder = also {
    this.isCircular = isCircular
  }

  fun lithoViewFactory(lithoViewFactory: LithoViewFactory?): RecyclerBinderConfigBuilder = also {
    this.lithoViewFactory = lithoViewFactory
  }

  fun hScrollAsyncMode(hScrollAsyncMode: Boolean): RecyclerBinderConfigBuilder = also {
    this.hScrollAsyncMode = hScrollAsyncMode
  }

  fun requestMountForPrefetchedItems(
      requestMountForPrefetchedItems: Boolean
  ): RecyclerBinderConfigBuilder = also {
    this.requestMountForPrefetchedItems = requestMountForPrefetchedItems
  }

  fun itemViewPrefetch(enabled: Boolean): RecyclerBinderConfigBuilder = also {
    recyclerViewItemPrefetch = enabled
  }

  fun itemViewCacheSize(size: Int): RecyclerBinderConfigBuilder = also { itemViewCacheSize = size }

  fun estimatedViewportCount(estimatedViewportCount: Int?): RecyclerBinderConfigBuilder = also {
    this.estimatedViewportCount = estimatedViewportCount
  }

  fun componentWarmer(componentWarmer: ComponentWarmer?): RecyclerBinderConfigBuilder = also {
    this.componentWarmer = componentWarmer
  }

  fun hasDynamicItemHeight(hasDynamicItemHeight: Boolean): RecyclerBinderConfigBuilder = also {
    this.hasDynamicItemHeight = hasDynamicItemHeight
  }

  fun threadPoolConfig(
      threadPoolConfig: LayoutThreadPoolConfiguration?
  ): RecyclerBinderConfigBuilder = also { this.threadPoolConfig = threadPoolConfig }

  fun errorEventHandler(errorEventHandler: ErrorEventHandler?): RecyclerBinderConfigBuilder = also {
    this.errorEventHandler = errorEventHandler
  }

  fun reconciliationEnabled(enabled: Boolean): RecyclerBinderConfigBuilder = also {
    this.reconciliationEnabled = enabled
  }

  fun preallocateMountContent(enabled: Boolean): RecyclerBinderConfigBuilder = also {
    preallocateMountContent = enabled
  }

  fun preallocateMountContentHandler(handler: RunnableHandler?): RecyclerBinderConfigBuilder =
      also {
        preallocateMountContentHandler = handler
      }

  fun build(): RecyclerBinderConfig {
    return RecyclerBinderConfig(
        isCircular = isCircular,
        lithoViewFactory = lithoViewFactory,
        hScrollAsyncMode = hScrollAsyncMode,
        requestMountForPrefetchedItems = requestMountForPrefetchedItems,
        recyclerViewItemPrefetch = recyclerViewItemPrefetch,
        itemViewCacheSize = itemViewCacheSize,
        componentWarmer = componentWarmer,
        estimatedViewportCount = estimatedViewportCount,
        hasDynamicItemHeight = hasDynamicItemHeight,
        threadPoolConfig = threadPoolConfig,
        errorEventHandler = errorEventHandler,
        reconciliationEnabled = reconciliationEnabled,
        preallocateMountContent = preallocateMountContent,
        preallocateMountContentHandler = preallocateMountContentHandler)
  }
}
