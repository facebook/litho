/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.widget;

import androidx.annotation.VisibleForTesting;
import androidx.collection.LruCache;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentsReporter;
import com.facebook.litho.LithoHandler;
import com.facebook.litho.Size;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.Nullable;

/**
 * Pool for pre-computing and storing ComponentTrees. Can be used to pre-compute and store
 * ComponentTrees before they are inserted in a RecyclerBinder. The RecyclerBinder will fetch the
 * precomputed ComponentTree from the pool if it's available instead of computing a layout
 * calculation when the item needs to be computed.
 */
public class ComponentWarmer {

  public static final String COMPONENT_WARMER_TAG = "component_warmer_tag";
  public static final String COMPONENT_WARMER_PREPARE_HANDLER = "component_warmer_prepare_handler";
  public static final int DEFAULT_MAX_SIZE = 10;
  private static final String COMPONENT_WARMER_LOG_TAG = "ComponentWarmer";

  public interface ComponentTreeHolderPreparer {

    /**
     * Create a ComponentTreeHolder instance from an existing render info which will be used as an
     * item in the underlying adapter of the RecyclerBinder
     */
    ComponentTreeHolder create(ComponentRenderInfo renderInfo);

    /**
     * Triggers a synchronous layout calculation for the ComponentTree held by the provided
     * ComponentTreeHolder.
     */
    void prepareSync(ComponentTreeHolder holder, @Nullable Size size);

    /**
     * Triggers an asynchronous layout calculation for the ComponentTree held by the provided
     * ComponentTreeHolder.
     */
    void prepareAsync(ComponentTreeHolder holder);
  }

  public class ComponentTreeHolderPreparerWithSizeImpl implements ComponentTreeHolderPreparer {

    private final int mHeightSpec;
    private final int mWidthSpec;
    private final ComponentContext mComponentContext;

    public ComponentTreeHolderPreparerWithSizeImpl(
        ComponentContext c, int widthSpec, int heightSpec) {
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;
      mComponentContext = c;
    }

    @Override
    public ComponentTreeHolder create(ComponentRenderInfo renderInfo) {
      return ComponentTreeHolder.create().renderInfo(renderInfo).build();
    }

    @Override
    public void prepareSync(ComponentTreeHolder holder, @Nullable Size size) {
      holder.computeLayoutSync(mComponentContext, mWidthSpec, mHeightSpec, size);
    }

    @Override
    public void prepareAsync(ComponentTreeHolder holder) {
      holder.computeLayoutAsync(mComponentContext, mWidthSpec, mHeightSpec);
    }
  }

  public interface ComponentWarmerReadyListener {

    /**
     * Called from a RecyclerBinder when a ComponentWarmer instance associated with it can be used
     * to prepare items because the RecyclerBinder has been measured.
     */
    void onInstanceReadyToPrepare();
  }

  public interface Cache {
    @Nullable
    ComponentTreeHolder remove(String tag);

    void put(String tag, ComponentTreeHolder holder);

    @Nullable
    ComponentTreeHolder get(String tag);

    void evictAll();
  }

  private static class DefaultCache implements Cache {
    private final LruCache<String, ComponentTreeHolder> cache;

    DefaultCache(int maxSize) {
      cache = new LruCache<>(maxSize);
    }

    @Override
    public @Nullable ComponentTreeHolder remove(String tag) {
      return cache.remove(tag);
    }

    @Override
    public void put(String tag, ComponentTreeHolder holder) {
      cache.put(tag, holder);
    }

    @Override
    @Nullable
    public ComponentTreeHolder get(String tag) {
      return cache.get(tag);
    }

    @Override
    public void evictAll() {
      cache.evictAll();
    }
  }

  private Cache mCache;
  private @Nullable ComponentTreeHolderPreparer mFactory;
  private boolean mIsReady;
  private @Nullable ComponentWarmerReadyListener mReadyListener;
  private BlockingQueue<ComponentRenderInfo> mPendingRenderInfos;

  /**
   * Sets up a {@link ComponentTreeHolderPreparerWithSizeImpl} as the {@link
   * ComponentTreeHolderPreparer} of this instance. All prepare calls will use the provided width
   * and height specs for preparing, until this instance is configured on a RecyclerBinder and a new
   * {@link ComponentTreeHolderPreparer} created by the RecyclerBinder is used, which uses the
   * RecyclerBinder's measurements.
   */
  public ComponentWarmer(ComponentContext c, int widthSpec, int heightSpec) {
    init(new ComponentTreeHolderPreparerWithSizeImpl(c, widthSpec, heightSpec), null);
  }

  /**
   * Creates a ComponentWarmer instance which is not ready to prepare items yet. If trying to
   * prepare an item before the ComponentWarmer is ready, the requests will be enqueued and will
   * only be executed once this instance is bound to a ComponentTreeHolderPreparer.
   *
   * <p>Pass in a {@link ComponentWarmerReadyListener} instance to be notified when the instance is
   * ready. Uses a {@link LruCache} to manage the internal cache.
   */
  public ComponentWarmer() {
    init(null, null);
  }

  public ComponentWarmer(Cache cache) {
    init(null, cache);
  }

  /**
   * Creates a ComponentWarmer for this RecyclerBinder. This ComponentWarmer instance will use the
   * same ComponentTree factory as the RecyclerBinder. The RecyclerBinder will query the
   * ComponentWarmer for cached items before creating new ComponentTrees. Uses a {@link LruCache} to
   * manage the internal cache.
   */
  public ComponentWarmer(RecyclerBinder recyclerBinder) {
    this(recyclerBinder, null);
  }

  /**
   * Same as {@link #ComponentWarmer(RecyclerBinder)} but uses the passed in Cache instance to
   * manage the internal cache.
   */
  public ComponentWarmer(RecyclerBinder recyclerBinder, @Nullable Cache cache) {
    this(recyclerBinder.getComponentTreeHolderPreparer(), cache);
    recyclerBinder.setComponentWarmer(this);
  }

  /**
   * Creates a ComponentWarmer which will use the provided ComponentTreeHolderPreparer instance to
   * create ComponentTreeHolder instances for preparing and caching items. Uses a {@link LruCache}
   * to manage the internal cache.
   */
  public ComponentWarmer(ComponentTreeHolderPreparer factory) {
    this(factory, null);
  }

  /**
   * Same as {@link #ComponentWarmer(ComponentTreeHolderPreparer)} but uses the passed in Cache
   * instance to manage the internal cache.
   */
  public ComponentWarmer(ComponentTreeHolderPreparer factory, @Nullable Cache cache) {
    if (factory == null) {
      throw new NullPointerException("factory == null");
    }

    init(factory, cache);
  }

  public void setComponentWarmerReadyListener(ComponentWarmerReadyListener listener) {
    mReadyListener = listener;
  }

  public synchronized boolean isReady() {
    return mIsReady;
  }

  private void init(@Nullable ComponentTreeHolderPreparer factory, @Nullable Cache cache) {
    mCache = cache == null ? new DefaultCache(DEFAULT_MAX_SIZE) : cache;

    if (factory != null) {
      mIsReady = true;
      setComponentTreeHolderFactory(factory);
    }
  }

  void setComponentTreeHolderFactory(ComponentTreeHolderPreparer factory) {
    if (factory == null) {
      throw new NullPointerException("factory == null");
    }

    mFactory = factory;

    if (!isReady()) {
      if (mReadyListener != null) {
        mReadyListener.onInstanceReadyToPrepare();
      }

      executePending();
      synchronized (this) {
        mIsReady = true;
      }
    }
  }

  /**
   * Synchronously post preparing the ComponentTree for the given ComponentRenderInfo to the
   * handler.
   */
  public void prepare(
      String tag,
      ComponentRenderInfo componentRenderInfo,
      @Nullable Size size,
      LithoHandler handler) {
    if (!isReady()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          COMPONENT_WARMER_LOG_TAG,
          "ComponentWarmer not ready: unable to prepare sync. This will be executed asynchronously when the ComponentWarmer is ready.");

      addToPending(tag, componentRenderInfo, handler);

      return;
    }

    executePrepare(tag, componentRenderInfo, size, false, handler);
  }

  /**
   * Synchronously prepare the ComponentTree for the given ComponentRenderInfo.
   *
   * @param tag Set a tag on the prepared ComponentTree so it can be retrieved from cache.
   * @param componentRenderInfo to be prepared.
   * @param size if not null, it will have the size result at the end of computing the layout.
   *     Prepare calls which require a size result to be computed cannot be cancelled (@see {@link
   *     #cancelPrepare(String)}). Prepare calls which are not immediately executed because the
   *     ComponentWarmer is not ready will not set a size.
   */
  public void prepare(String tag, ComponentRenderInfo componentRenderInfo, @Nullable Size size) {
    prepare(tag, componentRenderInfo, size, null);
  }

  /**
   * Asynchronously prepare the ComponentTree for the given ComponentRenderInfo.
   *
   * <p>The thread on which this ComponentRenderInfo is prepared is the background thread that the
   * associated RecyclerBinder uses. To change it, you can implement a {@link
   * ComponentTreeHolderPreparer} and configure the layout handler when creating the ComponentTree.
   *
   * <p>Alternatively you can use {@link #prepare(String, ComponentRenderInfo, Size, LithoHandler)}
   * to synchronously post the prepare call to a custom handler.
   */
  public void prepareAsync(String tag, ComponentRenderInfo componentRenderInfo) {
    if (!isReady()) {
      addToPending(tag, componentRenderInfo, null);

      return;
    }

    executePrepare(tag, componentRenderInfo, null, true, null);
  }

  private void executePrepare(
      String tag,
      ComponentRenderInfo renderInfo,
      @Nullable final Size size,
      boolean isAsync,
      @Nullable LithoHandler handler) {
    if (mFactory == null) {
      throw new IllegalStateException(
          "ComponentWarmer: trying to execute prepare but ComponentWarmer is not ready.");
    }
    renderInfo.addCustomAttribute(COMPONENT_WARMER_TAG, tag);

    final ComponentTreeHolder holder = mFactory.create(renderInfo);
    mCache.put(tag, holder);

    if (isAsync) {
      mFactory.prepareAsync(holder);
    } else {
      if (handler != null) {
        handler.post(
            new Runnable() {
              @Override
              public void run() {
                mFactory.prepareSync(holder, size);
              }
            },
            "prepare");
      } else {
        mFactory.prepareSync(holder, size);
      }
    }
  }

  private void addToPending(
      String tag, ComponentRenderInfo componentRenderInfo, @Nullable LithoHandler handler) {
    ensurePendingQueue();

    componentRenderInfo.addCustomAttribute(COMPONENT_WARMER_TAG, tag);

    if (handler != null) {
      componentRenderInfo.addCustomAttribute(COMPONENT_WARMER_PREPARE_HANDLER, handler);
    }

    mPendingRenderInfos.offer(componentRenderInfo);
  }

  private void executePending() {
    synchronized (this) {
      if (mPendingRenderInfos == null) {
        mIsReady = true;
        return;
      }
    }

    while (!mPendingRenderInfos.isEmpty()) {
      final ComponentRenderInfo renderInfo = mPendingRenderInfos.poll();

      final Object customAttrTag = renderInfo.getCustomAttribute(COMPONENT_WARMER_TAG);
      if (customAttrTag == null) {
        continue;
      }

      final String tag = (String) customAttrTag;

      if (renderInfo.getCustomAttribute(COMPONENT_WARMER_PREPARE_HANDLER) != null) {
        final LithoHandler handler =
            (LithoHandler) renderInfo.getCustomAttribute(COMPONENT_WARMER_PREPARE_HANDLER);
        executePrepare(tag, renderInfo, null, false, handler);
      } else {
        executePrepare(tag, renderInfo, null, true, null);
      }

      // Sync around mPendingRenderInfos.isEmpty() because otherwise this can happen:
      // 1. T1: check if is ready in prepare(), get false
      // 2. T2: here, check mPendingRenderInfos.isEmpty(), get true
      // 3. T1: add item to pending list
      // 4. T2: make mIsReady true
      // 5. T1: do step 1 again, read mIsReady true, execute layout before T2 loops. Prepare calls
      // are executed out of order.
      synchronized (this) {
        if (mPendingRenderInfos.isEmpty()) {
          mIsReady = true;
        }
      }
    }
  }

  public void evictAll() {
    mCache.evictAll();
  }

  public void remove(String tag) {
    mCache.remove(tag);
  }

  /**
   * If it exists, it returns the cached ComponentTreeHolder for this tag and removes it from cache.
   */
  @Nullable
  public ComponentTreeHolder consume(String tag) {
    return mCache.remove(tag);
  }

  /**
   * Cancels the prepare execution for the item with the given tag if it's currently running and it
   * removes the item from the cache.
   */
  public void cancelPrepare(String tag) {
    final ComponentTreeHolder holder = mCache.remove(tag);
    if (holder == null || holder.getComponentTree() == null) {
      return;
    }

    holder.getComponentTree().cancelLayoutAndReleaseTree();
  }

  @VisibleForTesting
  @Nullable
  ComponentTreeHolderPreparer getFactory() {
    return mFactory;
  }

  private synchronized void ensurePendingQueue() {
    if (mPendingRenderInfos == null) {
      mPendingRenderInfos = new LinkedBlockingQueue<>(10);
    }
  }

  @VisibleForTesting
  BlockingQueue<ComponentRenderInfo> getPending() {
    return mPendingRenderInfos;
  }

  @VisibleForTesting
  Cache getCache() {
    return mCache;
  }

  @VisibleForTesting
  ComponentTreeHolderPreparer getPrepareImpl() {
    return mFactory;
  }
}
