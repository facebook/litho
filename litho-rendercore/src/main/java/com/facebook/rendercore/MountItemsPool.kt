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

package com.facebook.rendercore

import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.util.Pools
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.facebook.rendercore.utils.ThreadUtils.runOnUiThread
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.GuardedBy

/**
 * Pools of recycled resources.
 *
 * FUTURE: Consider customizing the pool implementation such that we can match buffer sizes. Without
 * this we will tend to expand all buffers to the largest size needed.
 */
object MountContentPools {

  private var mountItemPoolsReleaseValidator: MountItemPoolsReleaseValidator? = null

  /** A factory used to create [MountContentPools.ContentPool]s. */
  fun interface Factory {

    /** Creates an ContentPool for the mountable content. */
    fun createMountContentPool(): ContentPool
  }

  private val mountContentLock: Any = Any()

  @GuardedBy("mountContentLock")
  private val mountContentPoolsByContext: MutableMap<Context, ContextContentPools> = HashMap(4)

  // This Map is used as a set and the values are ignored.
  @GuardedBy("mountContentLock")
  private val destroyedRootContexts: WeakHashMap<Context, Boolean> = WeakHashMap<Context, Boolean>()

  @GuardedBy("mountContentLock")
  private val contextsWithLifecycleObservers: WeakHashMap<Context, Boolean> =
      WeakHashMap<Context, Boolean>()

  private var activityCallbacks: PoolsActivityCallback? = null

  /**
   * To support Gingerbread (where the registerActivityLifecycleCallbacks API doesn't exist), we
   * allow apps to explicitly invoke activity callbacks. If this is enabled we'll throw if we are
   * passed a context for which we have no record.
   */
  @JvmField var isManualCallbacks: Boolean = false

  /** Should be used to disable pooling entirely for debugging, testing, and other use cases. */
  @JvmField var isPoolingDisabled: Boolean = false

  /** Can be used to return a custom Pool implementation for testing. */
  private val mountContentPoolFactory: ThreadLocal<Factory> = ThreadLocal<Factory>()

  private var hasMountContentPoolFactory = false

  @JvmStatic
  fun acquireMountContent(
      context: Context,
      poolableMountContent: ContentAllocator<*>,
      poolScope: PoolScope = PoolScope.None
  ): Any {
    val contentFromPool =
        if (poolableMountContent.poolingPolicy.canAcquireContent) {
          val pool =
              getOrCreateMountContentPool(
                  context = context, allocator = poolableMountContent, poolScope = poolScope)
          pool?.acquire(poolableMountContent)
        } else {
          null
        }

    return if (contentFromPool != null) {
          contentFromPool
        } else {
          val isTracing = RenderCoreSystrace.isTracing()
          if (isTracing) {
            RenderCoreSystrace.beginSection(
                "MountContentPools:createMountContent ${poolableMountContent.poolKeyTypeName}")
          }
          val newContent = poolableMountContent.createContent(context)
          if (isTracing) {
            RenderCoreSystrace.endSection()
          }

          newContent
        }
        .also { content ->
          if (content is View) {
            mountItemPoolsReleaseValidator?.registerAcquiredViewState(content)
          }
        }
  }

  @JvmStatic
  fun release(
      context: Context,
      poolableMountContent: ContentAllocator<*>,
      mountContent: Any,
      poolScope: PoolScope = PoolScope.None
  ) {
    if (RenderCoreConfig.removeComponentHostListeners) {
      if (mountContent is Host) {
        mountContent.removeViewListeners()
      }
    }
    val pool =
        if (poolableMountContent.poolingPolicy.canReleaseContent) {
          getOrCreateMountContentPool(
              context = context, allocator = poolableMountContent, poolScope = poolScope)
        } else {
          null
        }

    if (pool == null) {
      // There is no pool, call onContentDiscarded to allow for releasing resources if needed.
      poolableMountContent.onContentDiscarded?.invoke(mountContent)
      return
    }

    if (mountItemPoolsReleaseValidator != null && mountContent is View) {
      mountItemPoolsReleaseValidator?.assertValidRelease(
          mountContent, listOf(poolableMountContent.poolKeyTypeName))
    }

    val releasedToThePool = pool.release(mountContent)

    if (!releasedToThePool) {
      // Content was not released to the pool. Call onContentDiscarded to allow for releasing
      // resources if needed.
      poolableMountContent.onContentDiscarded?.invoke(mountContent)
    }
  }

  @JvmStatic
  fun maybePreallocateContent(
      context: Context,
      poolableMountContent: ContentAllocator<*>,
      poolScope: PoolScope = PoolScope.None
  ): Boolean {
    val pool =
        getOrCreateMountContentPool(
            context = context, allocator = poolableMountContent, poolScope = poolScope)
    return pool?.maybePreallocateContent(context, poolableMountContent) ?: false
  }

  /**
   * Fills up a mount content pool for the specified [poolableMountContent] allocator. If a pool
   * doesn't exist for a [ContentAllocator], a default one will be created with the specified size.
   * The specified [poolSize] will only be respected if the [poolableMountContent] does not provide
   * a custom Pool implementation.
   */
  @JvmStatic
  fun prefillMountContentPool(
      context: Context,
      poolSize: Int,
      poolableMountContent: ContentAllocator<*>,
      poolScope: PoolScope = PoolScope.None
  ) {
    if (poolSize == 0) {
      return
    }

    val pool =
        getOrCreateMountContentPool(
            context = context,
            allocator = poolableMountContent,
            poolScope = poolScope,
            poolSize = poolSize)

    if (pool != null) {
      for (i in 0 until poolSize) {
        if (!pool.release(poolableMountContent.createContent(context))) {
          break
        }
      }
    }
  }

  /**
   * Retrieves a recycling pool for the given [ContentAllocator] if it exists. Else it will default
   * to creating a new pool with the [allocator]'s pool size if no size is specified
   */
  private fun getOrCreateMountContentPool(
      context: Context,
      allocator: ContentAllocator<*>,
      poolScope: PoolScope,
      poolSize: Int = allocator.poolSize()
  ): ContentPool? {
    if (isPoolingDisabled || poolSize <= 0) {
      return null
    }

    synchronized(mountContentLock) {
      var contextContentPools = mountContentPoolsByContext[context]
      if (contextContentPools == null) {
        val rootContext = getRootContext(context)
        if (destroyedRootContexts.containsKey(rootContext)) {
          return null
        }
        ensureLifecycleCallbacks(rootContext)
        contextContentPools = ContextContentPools()
        mountContentPoolsByContext[context] = contextContentPools
      }
      val scopedPools = contextContentPools.getPools(poolScope)

      val poolKey = allocator.getPoolKey()
      var pool = scopedPools[poolKey]

      if (pool == null && hasMountContentPoolFactory) {
        pool = mountContentPoolFactory.get()?.createMountContentPool()
      }

      if (pool == null) {
        pool = allocator.onCreateMountContentPool(poolSize) ?: DefaultContentPool(poolKey, poolSize)
        pool.setOnClearedListener(allocator.onContentDiscarded)
      }

      scopedPools[poolKey] = pool
      return pool
    }
  }

  @JvmStatic
  @VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE)
  fun clear() {
    synchronized(mountContentLock) {
      // Clear pools and dispatch OnClearedListeners before clearing the maps
      mountContentPoolsByContext.values.forEach { contextContentPool -> contextContentPool.clear() }

      mountContentPoolsByContext.clear()
      destroyedRootContexts.clear()
      contextsWithLifecycleObservers.clear()
    }
  }

  @JvmStatic
  fun releaseScope(poolScope: PoolScope) {
    synchronized(mountContentLock) {
      mountContentPoolsByContext.values.forEach { it.releaseScope(poolScope) }
    }
  }

  @JvmStatic
  @VisibleForTesting
  fun setMountContentPoolFactory(factory: Factory?) {
    mountContentPoolFactory.set(factory)
    hasMountContentPoolFactory = factory != null
  }

  /**
   * @return the "most base" Context of this Context, i.e. the Activity, Application, or Service
   *   backing this Context and all its ContextWrappers. In some cases, e.g. instrumentation tests
   *   or other places we don't wrap a standard Context, this root Context may instead be a raw
   *   ContextImpl.
   */
  private fun getRootContext(context: Context): Context {
    var currentContext = context
    while (currentContext is ContextWrapper &&
        currentContext !is Activity &&
        currentContext !is Application &&
        currentContext !is Service) {
      currentContext = currentContext.baseContext
    }
    return currentContext
  }

  private fun ensureLifecycleCallbacks(context: Context) {
    if (!isManualCallbacks) {
      if (context is LifecycleOwner) {
        synchronized(mountContentLock) {
          if (!contextsWithLifecycleObservers.containsKey(context)) {
            contextsWithLifecycleObservers[context] = true
            runOnUiThread {
              context.lifecycle.addObserver(PoolsLifecycleObserver(context.lifecycle.currentState))
            }
          }
        }
      } else if (activityCallbacks == null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
          throw RuntimeException(
              "Activity callbacks must be invoked manually below ICS (API level 14)")
        }
        activityCallbacks = PoolsActivityCallback()
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(
            activityCallbacks)
      }
    }
  }

  @JvmStatic
  fun onContextCreated(context: Context) {
    synchronized(mountContentLock) {
      check(!mountContentPoolsByContext.containsKey(context)) {
        "The MountContentPools has a reference to an activity that has just been created"
      }
    }
  }

  @JvmStatic
  fun onContextDestroyed(context: Context) {
    synchronized(mountContentLock) {
      clearMatchingContexts(context)
      destroyedRootContexts.put(getRootContext(context), true)
    }
  }

  @GuardedBy("mountContentLock")
  private fun clearMatchingContexts(
      context: Context,
  ) {
    val removedContextContentPool = mountContentPoolsByContext.remove(context)
    removedContextContentPool?.clear()

    // Clear any context wrappers holding a reference to this activity.
    val iterator: MutableIterator<Map.Entry<Context, ContextContentPools>> =
        mountContentPoolsByContext.entries.iterator()
    while (iterator.hasNext()) {
      val (contextKey, contextContentPool) = iterator.next()
      if (isContextWrapper(contextKey, context)) {
        iterator.remove()
        contextContentPool.clear()
      }
    }
  }

  @get:VisibleForTesting
  val mountContentPools: List<ContentPool>
    get() {
      val result: MutableList<ContentPool> = ArrayList()
      for (itemPools in mountContentPoolsByContext.values) {
        itemPools.scopedPools.values.flatMap { it.values }.forEach { pool -> result.add(pool) }
        itemPools.unscopedPools.values.forEach { pool -> result.add(pool) }
      }
      return result
    }

  /** Check whether contextWrapper is a wrapper of baseContext */
  private fun isContextWrapper(contextWrapper: Context, baseContext: Context): Boolean {
    var baseCtx = baseContext
    while (baseCtx is ContextWrapper) {
      baseCtx = baseCtx.baseContext
    }
    var currentContext = contextWrapper
    while (currentContext is ContextWrapper) {
      currentContext = currentContext.baseContext
    }
    return currentContext === baseCtx
  }

  private val ContentAllocator<*>.poolKeyTypeName: String
    get() {
      val poolKey: Any = getPoolKey()
      return (poolKey as? Class<*>)?.simpleName ?: poolKey.toString()
    }

  /** Empty implementation of the [Application.ActivityLifecycleCallbacks] interface */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private class PoolsActivityCallback : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
      onContextCreated(activity)
    }

    override fun onActivityStarted(activity: Activity) {
      // Do nothing.
    }

    override fun onActivityResumed(activity: Activity) {
      // Do nothing.
    }

    override fun onActivityPaused(activity: Activity) {
      // Do nothing.
    }

    override fun onActivityStopped(activity: Activity) {
      // Do nothing.
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
      // Do nothing.
    }

    override fun onActivityDestroyed(activity: Activity) {
      onContextDestroyed(activity)
    }
  }

  private class PoolsLifecycleObserver(private val initialState: Lifecycle.State) :
      DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
      // When the lifecycle observer is attached to an owner that has already been created, the
      // onCreate method will be invoked but we want to call onContextCreated only when the owner
      // switches to created for the first time.
      if (!initialState.isAtLeast(Lifecycle.State.CREATED)) {
        // we know owner is a Context because we checked this when adding the lifecycle observer
        onContextCreated(owner as Context)
      }
    }

    override fun onDestroy(owner: LifecycleOwner) {
      // we know owner is a Context because we checked this when adding the lifecycle observer
      val context = owner as Context
      onContextDestroyed(context)
      owner.lifecycle.removeObserver(this)
      synchronized(mountContentLock) { contextsWithLifecycleObservers.remove(context) }
    }
  }

  /**
   * Enables the validation step which verifies if the content is released with listeners to the
   * pool. This should only be used for debugging and logging purposes.
   */
  fun enableItemsReleaseValidation(
      failOnDetection: Boolean,
      excludedPatterns: Set<Regex> = emptySet(),
      extraFields: List<MountItemPoolsReleaseValidator.FieldExtractionDefinition> = emptyList(),
      onInvalidRelease: (exception: InvalidReleaseToMountPoolException) -> Unit
  ) {
    mountItemPoolsReleaseValidator =
        MountItemPoolsReleaseValidator(
            failOnDetection = failOnDetection,
            excludedPatterns = excludedPatterns,
            onInvalidRelease = onInvalidRelease,
            extraFields = extraFields)
  }

  /** Content pool that RenderCore uses to recycle content (such as Views) */
  interface ContentPool {

    /** Interface definition for a callback to be invoked when the pool is cleared. */
    fun interface OnClearedListener {
      /**
       * Called when the pool is cleared.
       *
       * @param item The content that has been removed from the pool when pool has been cleared.
       */
      fun onCleared(item: Any)
    }

    /**
     * Acquire a pooled content item from the pool
     *
     * @param contentAllocator the content allocator used.
     * @return a pooled content item
     */
    fun acquire(contentAllocator: ContentAllocator<*>): Any?

    /**
     * Called when an item is released and can return to the pool
     *
     * @param item the item to release to the pool
     * @return `true` if the {@param item} is released to the pool.
     */
    fun release(item: Any): Boolean

    /**
     * This method can be called to allocate mount content to a Pool ahead of time.
     *
     * @param c the android context
     * @param contentAllocator the content allocator used.
     * @return `` true if the mount content created by {@param contentAllocator} is released into
     *   the pool.
     */
    fun maybePreallocateContent(c: Context, contentAllocator: ContentAllocator<*>): Boolean

    /**
     * Register a callback to be invoked when this pool is cleared.
     *
     * @param listener The callback that will run
     */
    fun setOnClearedListener(listener: OnClearedListener?): Unit = Unit

    /** Clears the pool and dispatches [OnClearedListener] if present. */
    fun clear(): Unit = Unit
  }

  open class DefaultContentPool(poolKey: Any, private val maxPoolSize: Int) : ContentPool {

    private val pool: Pools.SynchronizedPool<Any> = Pools.SynchronizedPool(maxPoolSize)

    private val debugIdentifier: String = (poolKey as? Class<*>)?.name ?: poolKey.toString()

    private val currentPoolSize: AtomicInteger = AtomicInteger(0)

    private var onClearedListener: ContentPool.OnClearedListener? = null

    override fun acquire(contentAllocator: ContentAllocator<*>): Any? {
      val content = pool.acquire()
      if (content != null) {
        currentPoolSize.decrementAndGet()
      }
      return content
    }

    override fun release(item: Any): Boolean {
      return try {
        val releasedIntoPool = pool.release(item)
        if (releasedIntoPool) {
          currentPoolSize.incrementAndGet()
        }
        releasedIntoPool
      } catch (e: IllegalStateException) {
        val metadata = "Failed to release item to DefaultContentPool: $debugIdentifier"
        throw IllegalStateException(metadata, e)
      }
    }

    override fun maybePreallocateContent(
        c: Context,
        contentAllocator: ContentAllocator<*>
    ): Boolean {
      return if (currentPoolSize.get() < maxPoolSize) {
        release(contentAllocator.createContent(c))
      } else false
    }

    override fun setOnClearedListener(listener: ContentPool.OnClearedListener?) {
      onClearedListener = listener
    }

    override fun clear() {
      // clear is currently used only in order to invoke the onClearedListener
      // so if the listener isn't present, there is no need to do the additional work
      if (onClearedListener == null || currentPoolSize.get() == 0) {
        return
      }

      do {
        val content = pool.acquire()
        if (content != null) {
          currentPoolSize.decrementAndGet()
          onClearedListener?.onCleared(content)
        }
      } while (content != null)
    }
  }
}

/**
 * Represents a scope of a custom scoped pool.
 *
 * Use [LifecycleAware] if you want the scope to be automatically released when the [Lifecyle]
 * transitions to destroyed state or [ManuallyManaged] if you want to manually control when the
 * scope is released.
 */
sealed interface PoolScope {

  /** An object representing none scope. Used as a default [PoolScope] value. */
  object None : PoolScope

  /**
   * A pool scope that automatically releases the pool when the given [lifecycle] transitions to
   * destroyed state
   */
  class LifecycleAware(private val lifecycle: Lifecycle) : PoolScope {
    init {
      lifecycle.addObserver(
          object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
              lifecycle.removeObserver(this)
              MountContentPools.releaseScope(this@LifecycleAware)
            }
          })
    }
  }

  /** A pool scope that needs to be manually released by calling [releaseScope] method on it. */
  class ManuallyManaged : PoolScope {

    /** Clears the pool associated with this [PoolScope]. */
    fun releaseScope() {
      MountContentPools.releaseScope(this)
    }
  }
}

/**
 * A class that holds content pools.
 *
 * [scopedPools] contains pools scoped to [PoolScope] [unscopedPools] contains pools scoped to the
 * [Context]
 */
private class ContextContentPools {

  val scopedPools: MutableMap<PoolScope, MutableMap<Any, MountContentPools.ContentPool>> = HashMap()

  val unscopedPools: MutableMap<Any, MountContentPools.ContentPool> = HashMap()

  /** Returns pools associated with [poolScope] or [unscopedPools] if [poolScope] is null. */
  fun getPools(
      poolScope: PoolScope = PoolScope.None
  ): MutableMap<Any, MountContentPools.ContentPool> {
    if (poolScope == PoolScope.None) {
      return unscopedPools
    }
    return scopedPools.getOrPut(poolScope) { HashMap() }
  }

  /**
   * Clears the pool associated with [poolScope] and dispatches
   * [MountContentPools.ContentPool.OnClearedListener]s if present.
   */
  fun releaseScope(poolScope: PoolScope) {
    val removedPool = scopedPools.remove(poolScope)
    removedPool?.values?.forEach { contentPool -> clearContentPool(contentPool) }
  }

  /**
   * Clears all pools and dispatches [MountContentPools.ContentPool.OnClearedListener]s if present.
   */
  fun clear() {
    scopedPools.values
        .flatMap { poolKeyToContentPoolMap -> poolKeyToContentPoolMap.values }
        .forEach { contentPool -> clearContentPool(contentPool) }
    scopedPools.clear()

    unscopedPools.values.forEach { contentPool -> clearContentPool(contentPool) }
    unscopedPools.clear()
  }

  private fun clearContentPool(contentPool: MountContentPools.ContentPool) {
    contentPool.clear()
    contentPool.setOnClearedListener(null)
  }
}
