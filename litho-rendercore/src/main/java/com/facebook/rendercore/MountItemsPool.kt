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
import androidx.lifecycle.LifecycleOwner
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.concurrent.GuardedBy

/**
 * Pools of recycled resources.
 *
 * FUTURE: Consider customizing the pool implementation such that we can match buffer sizes. Without
 * this we will tend to expand all buffers to the largest size needed.
 */
object MountItemsPool {

  private var mountItemPoolsReleaseValidator: MountItemPoolsReleaseValidator? = null

  /** A factory used to create [MountItemsPool.ItemPool]s. */
  fun interface Factory {

    /** Creates an ItemPool for the mountable content. */
    fun createMountContentPool(): ItemPool
  }

  private val mountContentLock: Any = Any()

  @GuardedBy("mountContentLock")
  private val mountContentPoolsByContext: MutableMap<Context, MutableMap<Any, ItemPool>> =
      HashMap(4)

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
  fun acquireMountContent(context: Context, poolableMountContent: ContentAllocator<*>): Any {
    val content =
        if (poolableMountContent.poolingPolicy.canAcquireContent) {
          val pool = getOrCreateMountContentPool(context, poolableMountContent)
          pool?.acquire(poolableMountContent)
        } else {
          null
        }

    return if (content != null) {
          content
        } else {
          val isTracing = RenderCoreSystrace.isTracing()
          if (isTracing) {
            RenderCoreSystrace.beginSection(
                "MountItemsPool:createMountContent ${poolableMountContent.getPoolableContentType().simpleName}")
          }
          val content = poolableMountContent.createPoolableContent(context)
          if (isTracing) {
            RenderCoreSystrace.endSection()
          }

          content
        }
        .also { content ->
          if (content is View) {
            mountItemPoolsReleaseValidator?.registerAcquiredViewState(content)
          }
        }
  }

  @JvmStatic
  fun release(context: Context, poolableMountContent: ContentAllocator<*>, mountContent: Any) {
    if (RenderCoreConfig.removeComponentHostListeners) {
      if (mountContent is Host) {
        mountContent.removeViewListeners()
      }
    }
    val pool =
        if (poolableMountContent.poolingPolicy.canReleaseContent) {
          getOrCreateMountContentPool(context, poolableMountContent)
        } else {
          null
        }

    if (pool == null) {
      return
    }

    if (mountItemPoolsReleaseValidator != null && mountContent is View) {
      mountItemPoolsReleaseValidator?.assertValidRelease(
          mountContent, listOf(poolableMountContent.getPoolableContentType().name))
    }

    pool.release(mountContent)
  }

  @JvmStatic
  fun maybePreallocateContent(
      context: Context,
      poolableMountContent: ContentAllocator<*>
  ): Boolean {
    val pool = getOrCreateMountContentPool(context, poolableMountContent)
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
      poolableMountContent: ContentAllocator<*>
  ) {
    if (poolSize == 0) {
      return
    }

    val pool = getOrCreateMountContentPool(context, poolableMountContent, poolSize)

    if (pool != null) {
      for (i in 0 until poolSize) {
        if (!pool.release(poolableMountContent.createPoolableContent(context))) {
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
      poolSize: Int = allocator.poolSize()
  ): ItemPool? {
    if (isPoolingDisabled || poolSize <= 0) {
      return null
    }

    synchronized(mountContentLock) {
      var poolsMap = mountContentPoolsByContext[context]
      if (poolsMap == null) {
        val rootContext = getRootContext(context)
        if (destroyedRootContexts.containsKey(rootContext)) {
          return null
        }
        ensureLifecycleCallbacks(rootContext)
        poolsMap = HashMap()
        mountContentPoolsByContext[context] = poolsMap
      }

      val poolableContentType = allocator.getPoolableContentType()
      var pool = poolsMap[poolableContentType]

      if (pool == null && hasMountContentPoolFactory) {
        pool = mountContentPoolFactory.get()?.createMountContentPool()
      }

      if (pool == null) {
        pool =
            allocator.onCreateMountContentPool(poolSize)
                ?: DefaultItemPool(poolableContentType, poolSize)
      }

      poolsMap[poolableContentType] = pool
      return pool
    }
  }

  @JvmStatic
  @VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE)
  fun clear() {
    synchronized(mountContentLock) {
      mountContentPoolsByContext.clear()
      destroyedRootContexts.clear()
      contextsWithLifecycleObservers.clear()
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
      if (context is LifecycleOwner && RenderCoreConfig.useLifecycleObserverInMountPools) {
        synchronized(mountContentLock) {
          if (!contextsWithLifecycleObservers.containsKey(context)) {
            contextsWithLifecycleObservers[context] = true
            context.lifecycle.addObserver(PoolsLifecycleObserver())
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
      clearMatchingContexts(context, mountContentPoolsByContext)
      destroyedRootContexts.put(getRootContext(context), true)
    }
  }

  @GuardedBy("mountContentLock")
  private fun <T> clearMatchingContexts(context: Context, poolsMap: MutableMap<Context, T>) {
    poolsMap.remove(context)

    // Clear any context wrappers holding a reference to this activity.
    val it: MutableIterator<Map.Entry<Context, T>> = poolsMap.entries.iterator()
    while (it.hasNext()) {
      val contextKey = it.next().key
      if (isContextWrapper(contextKey, context)) {
        it.remove()
      }
    }
  }

  @get:VisibleForTesting
  val mountItemPools: List<ItemPool>
    get() {
      val result: MutableList<ItemPool> = ArrayList()
      for (poolMap in mountContentPoolsByContext.values) {
        for (pool in poolMap.values) {
          result.add(pool)
        }
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

  private class PoolsLifecycleObserver : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
      // we know owner is a Context because we checked this when adding the lifecycle observer
      onContextCreated(owner as Context)
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
      onInvalidRelease: (exception: InvalidReleaseToMountPoolException) -> Unit
  ) {
    mountItemPoolsReleaseValidator =
        MountItemPoolsReleaseValidator(
            failOnDetection = failOnDetection,
            excludedPatterns = excludedPatterns,
            onInvalidRelease = onInvalidRelease)
  }

  /** Content item pools that RenderCore uses to recycle content (such as Views) */
  interface ItemPool {

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
  }

  open class DefaultItemPool(poolableContentType: Class<*>, private val maxPoolSize: Int) :
      ItemPool {

    private val pool: Pools.SynchronizedPool<Any> = Pools.SynchronizedPool(maxPoolSize)

    private val debugIdentifier: String = poolableContentType.name

    private val currentPoolSize: AtomicInteger = AtomicInteger(0)

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
        val metadata = "Failed to release item to MountItemPool: $debugIdentifier"
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
  }
}
