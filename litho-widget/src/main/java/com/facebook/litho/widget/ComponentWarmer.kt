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

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.collection.LruCache
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentsReporter
import com.facebook.litho.Size
import com.facebook.litho.ThreadUtils
import com.facebook.rendercore.RunnableHandler
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.Volatile

/**
 * Pool for pre-computing and storing ComponentTrees. Can be used to pre-compute and store
 * ComponentTrees before they are inserted in a RecyclerBinder. The RecyclerBinder will fetch the
 * precomputed ComponentTree from the pool if it's available instead of computing a layout
 * calculation when the item needs to be computed.
 */
class ComponentWarmer {

  interface ComponentTreeHolderPreparer {
    /**
     * Create a ComponentTreeHolder instance from an existing render info which will be used as an
     * item in the underlying adapter of the RecyclerBinder
     */
    fun create(renderInfo: ComponentRenderInfo): ComponentTreeHolder

    /**
     * Triggers a synchronous layout calculation for the ComponentTree held by the provided
     * ComponentTreeHolder.
     */
    fun prepareSync(holder: ComponentTreeHolder, size: Size?)

    /**
     * Triggers an asynchronous layout calculation for the ComponentTree held by the provided
     * ComponentTreeHolder.
     */
    fun prepareAsync(holder: ComponentTreeHolder)
  }

  inner class ComponentTreeHolderPreparerWithSizeImpl(
      private val componentContext: ComponentContext,
      private val widthSpec: Int,
      private val heightSpec: Int
  ) : ComponentTreeHolderPreparer {
    override fun create(renderInfo: ComponentRenderInfo): ComponentTreeHolder =
        ComponentTreeHolder.create(componentContext.lithoConfiguration.componentsConfig)
            .renderInfo(renderInfo)
            .build()

    override fun prepareSync(holder: ComponentTreeHolder, size: Size?) {
      holder.computeLayoutSync(componentContext, widthSpec, heightSpec, size)
    }

    override fun prepareAsync(holder: ComponentTreeHolder) {
      holder.computeLayoutAsync(componentContext, widthSpec, heightSpec)
    }
  }

  fun interface ComponentWarmerReadyListener {
    /**
     * Called from a RecyclerBinder when a ComponentWarmer instance associated with it can be used
     * to prepare items because the RecyclerBinder has been measured.
     */
    fun onInstanceReadyToPrepare()
  }

  fun interface CacheListener {
    fun onEntryEvicted(tag: String, holder: ComponentTreeHolder)
  }

  interface Cache {
    fun remove(tag: String): ComponentTreeHolder?

    fun put(tag: String, holder: ComponentTreeHolder)

    operator fun get(tag: String): ComponentTreeHolder?

    fun evictAll()

    fun setCacheListener(cacheListener: CacheListener?)
  }

  private class DefaultCache(maxSize: Int, private val cacheListener: CacheListener?) : Cache {
    val cache: LruCache<String, ComponentTreeHolder> =
        object : LruCache<String, ComponentTreeHolder>(maxSize) {
          override fun entryRemoved(
              evicted: Boolean,
              key: String,
              oldValue: ComponentTreeHolder,
              newValue: ComponentTreeHolder?
          ) {
            if (evicted && cacheListener != null) {
              cacheListener.onEntryEvicted(key, oldValue)
            }
          }
        }

    override fun remove(tag: String): ComponentTreeHolder? = this.cache.remove(tag)

    override fun put(tag: String, holder: ComponentTreeHolder) {
      this.cache.put(tag, holder)
    }

    override fun get(tag: String): ComponentTreeHolder? = this.cache[tag]

    override fun evictAll() {
      this.cache.evictAll()
    }

    override fun setCacheListener(cacheListener: CacheListener?) {
      // Already set from the constructor, so there is nothing to do here
    }
  }

  private val mainThreadHandler = Handler(Looper.getMainLooper())

  private var pendingRenderInfos: BlockingQueue<ComponentRenderInfo>? = null
  private var readyListener: ComponentWarmerReadyListener? = null
  @Volatile private var releaseEvictedEntries = false

  @get:VisibleForTesting
  lateinit var cache: Cache
    private set

  @get:VisibleForTesting
  var factory: ComponentTreeHolderPreparer? = null
    private set

  val prepareImpl: ComponentTreeHolderPreparer?
    @VisibleForTesting get() = factory

  @get:Synchronized
  var isReady: Boolean = false
    private set

  val pending: BlockingQueue<ComponentRenderInfo>?
    @VisibleForTesting get() = pendingRenderInfos

  /**
   * Sets up a [ComponentTreeHolderPreparerWithSizeImpl] as the [ComponentTreeHolderPreparer] of
   * this instance. All prepare calls will use the provided width and height specs for preparing,
   * until this instance is configured on a RecyclerBinder and a new [ComponentTreeHolderPreparer]
   * created by the RecyclerBinder is used, which uses the RecyclerBinder's measurements.
   */
  constructor(c: ComponentContext, widthSpec: Int, heightSpec: Int) {
    init(ComponentTreeHolderPreparerWithSizeImpl(c, widthSpec, heightSpec), null)
  }

  /**
   * Creates a ComponentWarmer instance which is not ready to prepare items yet. If trying to
   * prepare an item before the ComponentWarmer is ready, the requests will be enqueued and will
   * only be executed once this instance is bound to a ComponentTreeHolderPreparer.
   *
   * Pass in a [ComponentWarmerReadyListener] instance to be notified when the instance is ready.
   * Uses a [LruCache] to manage the internal cache.
   */
  constructor() {
    init(null, null)
  }

  constructor(cache: Cache?) {
    init(null, cache)
  }

  /**
   * Same as [ComponentWarmer] but uses the passed in Cache instance to manage the internal cache.
   */
  /**
   * Creates a ComponentWarmer for this RecyclerBinder. This ComponentWarmer instance will use the
   * same ComponentTree factory as the RecyclerBinder. The RecyclerBinder will query the
   * ComponentWarmer for cached items before creating new ComponentTrees. Uses a [LruCache] to
   * manage the internal cache.
   */
  @JvmOverloads
  constructor(
      recyclerBinder: RecyclerBinder,
      cache: Cache? = null
  ) : this(recyclerBinder.componentTreeHolderPreparer, cache) {
    recyclerBinder.componentWarmer = this
  }

  /**
   * Same as [ComponentWarmer] but uses the passed in Cache instance to manage the internal cache.
   */
  /**
   * Creates a ComponentWarmer which will use the provided ComponentTreeHolderPreparer instance to
   * create ComponentTreeHolder instances for preparing and caching items. Uses a [LruCache] to
   * manage the internal cache.
   */
  @JvmOverloads
  constructor(factory: ComponentTreeHolderPreparer, cache: Cache? = null) {
    init(factory, cache)
  }

  fun setComponentWarmerReadyListener(listener: ComponentWarmerReadyListener?) {
    readyListener = listener
  }

  fun setReleaseEvictedEntries(releaseEvictedEntries: Boolean) {
    this.releaseEvictedEntries = releaseEvictedEntries
  }

  private fun init(factory: ComponentTreeHolderPreparer?, cache: Cache?) {
    val cacheListener = CacheListener { _, holder ->
      if (releaseEvictedEntries) {
        if (ThreadUtils.isMainThread) {
          holder.releaseTree()
        } else {
          mainThreadHandler.post { holder.releaseTree() }
        }
      }
    }

    if (cache != null) {
      this.cache = cache
      cache.setCacheListener(cacheListener)
    } else {
      this.cache = DefaultCache(DEFAULT_MAX_SIZE, cacheListener)
    }

    if (factory != null) {
      isReady = true
      setComponentTreeHolderFactory(factory)
    }
  }

  fun setComponentTreeHolderFactory(factory: ComponentTreeHolderPreparer) {
    this.factory = factory

    if (!isReady) {
      readyListener?.onInstanceReadyToPrepare()

      executePending()
      synchronized(this) { isReady = true }
    }
  }

  /**
   * Synchronously post preparing the ComponentTree for the given ComponentRenderInfo to the
   * handler.
   */
  /**
   * Synchronously prepare the ComponentTree for the given ComponentRenderInfo.
   *
   * @param tag Set a tag on the prepared ComponentTree so it can be retrieved from cache.
   * @param componentRenderInfo to be prepared.
   * @param size if not null, it will have the size result at the end of computing the layout.
   *   Prepare calls which require a size result to be computed cannot be cancelled (@see
   *   [cancelPrepare]). Prepare calls which are not immediately executed because the
   *   ComponentWarmer is not ready will not set a size.
   */
  @JvmOverloads
  fun prepare(
      tag: String,
      componentRenderInfo: ComponentRenderInfo,
      size: Size?,
      handler: RunnableHandler? = null
  ) {
    if (!isReady) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          COMPONENT_WARMER_LOG_TAG,
          "ComponentWarmer not ready: unable to prepare sync. This will be executed asynchronously when the ComponentWarmer is ready.")

      addToPending(tag, componentRenderInfo, handler)

      return
    }

    executePrepare(tag, componentRenderInfo, size, false, handler)
  }

  /**
   * Asynchronously prepare the ComponentTree for the given ComponentRenderInfo.
   *
   * The thread on which this ComponentRenderInfo is prepared is the background thread that the
   * associated RecyclerBinder uses. To change it, you can implement a [ComponentTreeHolderPreparer]
   * and configure the layout handler when creating the ComponentTree.
   *
   * Alternatively you can use [prepare] to synchronously post the prepare call to a custom handler.
   */
  fun prepareAsync(tag: String, componentRenderInfo: ComponentRenderInfo) {
    if (!isReady) {
      addToPending(tag, componentRenderInfo, null)

      return
    }

    executePrepare(tag, componentRenderInfo, null, true, null)
  }

  private fun executePrepare(
      tag: String,
      renderInfo: ComponentRenderInfo,
      size: Size?,
      isAsync: Boolean,
      handler: RunnableHandler?
  ) {
    val prepareImpl =
        checkNotNull(factory) {
          "ComponentWarmer: trying to execute prepare but ComponentWarmer is not ready."
        }
    renderInfo.addCustomAttribute(COMPONENT_WARMER_TAG, tag)
    val holder = prepareImpl.create(renderInfo)
    cache.put(tag, holder)

    if (isAsync) {
      prepareImpl.prepareAsync(holder)
    } else {
      if (handler != null) {
        handler.post(Runnable { prepareImpl.prepareSync(holder, size) }, "prepare")
      } else {
        prepareImpl.prepareSync(holder, size)
      }
    }
  }

  private fun addToPending(
      tag: String,
      componentRenderInfo: ComponentRenderInfo,
      handler: RunnableHandler?
  ) {
    ensurePendingQueue()

    componentRenderInfo.addCustomAttribute(COMPONENT_WARMER_TAG, tag)

    if (handler != null) {
      componentRenderInfo.addCustomAttribute(COMPONENT_WARMER_PREPARE_HANDLER, handler)
    }

    checkNotNull(pendingRenderInfos).offer(componentRenderInfo)
  }

  private fun executePending() {
    val pending =
        synchronized(this) {
          pendingRenderInfos
              ?: run {
                isReady = true
                return
              }
        }
    while (!pending.isEmpty()) {
      val renderInfo = pending.poll()

      val customAttrTag = renderInfo.getCustomAttribute(COMPONENT_WARMER_TAG) ?: continue

      val tag = customAttrTag as String

      if (renderInfo.getCustomAttribute(COMPONENT_WARMER_PREPARE_HANDLER) != null) {
        val handler =
            renderInfo.getCustomAttribute(COMPONENT_WARMER_PREPARE_HANDLER) as RunnableHandler?
        executePrepare(tag, renderInfo, null, false, handler)
      } else {
        executePrepare(tag, renderInfo, null, true, null)
      }

      // Sync around pendingRenderInfos.isEmpty() because otherwise this can happen:
      // 1. T1: check if is ready in prepare(), get false
      // 2. T2: here, check pendingRenderInfos.isEmpty(), get true
      // 3. T1: add item to pending list
      // 4. T2: make isReady true
      // 5. T1: do step 1 again, read isReady true, execute layout before T2 loops. Prepare calls
      // are executed out of order.
      synchronized(this) {
        if (pending.isEmpty()) {
          isReady = true
        }
      }
    }
  }

  fun evictAll() {
    cache.evictAll()
  }

  fun remove(tag: String) {
    cache.remove(tag)
  }

  /**
   * If it exists, it returns the cached ComponentTreeHolder for this tag and removes it from cache.
   */
  fun consume(tag: String): ComponentTreeHolder? = cache.remove(tag)

  /**
   * Cancels the prepare execution for the item with the given tag if it's currently running and it
   * removes the item from the cache.
   */
  fun cancelPrepare(tag: String) {
    val holder = cache.remove(tag)
    holder?.componentTree?.cancelLayoutAndReleaseTree()
  }

  @Synchronized
  private fun ensurePendingQueue() {
    if (pendingRenderInfos == null) {
      pendingRenderInfos = LinkedBlockingQueue(10)
    }
  }

  companion object {
    const val COMPONENT_WARMER_TAG: String = "component_warmer_tag"
    const val COMPONENT_WARMER_PREPARE_HANDLER: String = "component_warmer_prepare_handler"
    const val DEFAULT_MAX_SIZE: Int = 10
    private const val COMPONENT_WARMER_LOG_TAG = "ComponentWarmer"
  }
}
