/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseArray;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.internal.ArraySet;
import com.facebook.yoga.YogaConfig;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaNode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.concurrent.GuardedBy;

/**
 * Pools of recycled resources.
 *
 * <p>FUTURE: Consider customizing the pool implementation such that we can match buffer sizes.
 * Without this we will tend to expand all buffers to the largest size needed.
 */
public class ComponentsPools {

  private static final int SCRAP_ARRAY_INITIAL_SIZE = 4;

  private static volatile YogaConfig sYogaConfig;

  private ComponentsPools() {}

  private static final Object sMountContentLock = new Object();
  private static final Object sYogaConfigLock = new Object();

  static final RecyclePool<LayoutState> sLayoutStatePool =
      new RecyclePool<>("LayoutState", PoolsConfig.sLayoutStateSize, true);

  static final RecyclePool<InternalNode> sInternalNodePool =
      new RecyclePool<>("InternalNode", PoolsConfig.sInternalNodeSize, true);

  static final RecyclePool<NodeInfo> sNodeInfoPool =
      new RecyclePool<>("NodeInfo", PoolsConfig.sNodeInfoSize, true);

  static final RecyclePool<ViewNodeInfo> sViewNodeInfoPool =
      new RecyclePool<>("ViewNodeInfo", 64, true);

  static final RecyclePool<YogaNode> sYogaNodePool =
      new RecyclePool<>("YogaNode", PoolsConfig.sYogaNodeSize, true);

  static final RecyclePool<MountItem> sMountItemPool = new RecyclePool<>("MountItem", 256, true);

  static final RecyclePool<LayoutOutput> sLayoutOutputPool =
      new RecyclePool<>("LayoutOutput", PoolsConfig.sLayoutOutputSize, true);

  @GuardedBy("sMountContentLock")
  private static final Map<Context, SparseArray<MountContentPool>> sMountContentPoolsByContext =
      new HashMap<>(4);

  static final RecyclePool<DisplayListContainer> sDisplayListContainerPool =
      new RecyclePool<>("DisplayListContainer", PoolsConfig.sDisplayListContainerSize, true);

  static final RecyclePool<VisibilityOutput> sVisibilityOutputPool =
      new RecyclePool<>("VisibilityOutput", 64, true);

  // These are lazily initialized as they are only needed when we're in a test environment.
  static RecyclePool<TestOutput> sTestOutputPool = null;
  static RecyclePool<TestItem> sTestItemPool = null;

  static final RecyclePool<VisibilityItem> sVisibilityItemPool =
      new RecyclePool<>("VisibilityItem", 64, true);

  static final RecyclePool<Output<?>> sOutputPool = new RecyclePool<>("Output", 20, true);

  static final RecyclePool<DiffNode> sDiffNodePool =
      new RecyclePool<>("DiffNode", PoolsConfig.sDiffNodeSize, true);

  static final RecyclePool<Diff<?>> sDiffPool = new RecyclePool<>("Diff", 20, true);

  static final RecyclePool<ComponentTree.Builder> sComponentTreeBuilderPool =
      new RecyclePool<>("ComponentTree.Builder", 2, true);

  static final RecyclePool<StateHandler> sStateHandlerPool =
      new RecyclePool<>("StateHandler", 10, true);

  static final RecyclePool<SparseArrayCompat<MountItem>> sMountItemScrapArrayPool =
      new RecyclePool<>("MountItemScrapArray", 8, false);

  static final RecyclePool<RectF> sRectFPool = new RecyclePool<>("RectF", 4, true);

  static final RecyclePool<Rect> sRectPool = new RecyclePool<>("Rect", 30, true);

  static final RecyclePool<Edges> sEdgesPool = new RecyclePool<>("Edges", 30, true);

  static final RecyclePool<DisplayListDrawable> sDisplayListDrawablePool =
      new RecyclePool<>("DisplayListDrawable", 10, false);

  static final RecyclePool<ArraySet> sArraySetPool = new RecyclePool<>("ArraySet", 10, true);

  static final RecyclePool<ArrayDeque> sArrayDequePool = new RecyclePool<>("ArrayDeque", 10, true);

  static final RecyclePool<RenderState> sRenderStatePool =
      new RecyclePool<>("RenderState", 4, true);

  static final RecyclePool<ArrayList<LithoView>> sLithoViewArrayListPool =
      new RecyclePool<>("LithoViewArrayList", 4, false);

  // Lazily initialized when acquired first time, as this is not a common use case.
  static RecyclePool<BorderColorDrawable> sBorderColorDrawablePool = null;

  // This Map is used as a set and the values are ignored.
  @GuardedBy("sMountContentLock")
  private static final WeakHashMap<Context, Boolean> sDestroyedRootContexts = new WeakHashMap<>();

  @GuardedBy("sMountContentLock")
  private static PoolsActivityCallback sActivityCallbacks;

  /**
   * To support Gingerbread (where the registerActivityLifecycleCallbacks API doesn't exist), we
   * allow apps to explicitly invoke activity callbacks. If this is enabled we'll throw if we are
   * passed a context for which we have no record.
   */
  static boolean sIsManualCallbacks;

  static LayoutState acquireLayoutState(ComponentContext context) {
    LayoutState state = sLayoutStatePool.acquire();
    if (state == null) {
      state = new LayoutState();
    }
    state.init(context);

    return state;
  }

  static YogaNode acquireYogaNode() {
    initYogaConfigIfNecessary();
    YogaNode node = sYogaNodePool.acquire();
    if (node == null) {
      node =
          PoolsConfig.sYogaNodeFactory != null
              ? PoolsConfig.sYogaNodeFactory.create(sYogaConfig)
              : new YogaNode(sYogaConfig);
    }

    return node;
  }

  static InternalNode acquireInternalNode(ComponentContext componentContext) {
    InternalNode node = sInternalNodePool.acquire();
    if (node == null) {
      node =
          PoolsConfig.sInternalNodeFactory != null
              ? PoolsConfig.sInternalNodeFactory.create()
              : new InternalNode();
    }

    node.init(acquireYogaNode(), componentContext);
    return node;
  }

  static NodeInfo acquireNodeInfo() {
    NodeInfo nodeInfo = sNodeInfoPool.acquire();
    if (nodeInfo == null) {
      nodeInfo = new NodeInfo();
    }

    return nodeInfo;
  }

  static ViewNodeInfo acquireViewNodeInfo() {
    ViewNodeInfo viewNodeInfo = sViewNodeInfoPool.acquire();
    if (viewNodeInfo == null) {
      viewNodeInfo = new ViewNodeInfo();
    }

    return viewNodeInfo;
  }

  static MountItem acquireRootHostMountItem(
      Component component, ComponentHost host, Object content) {
    MountItem item = sMountItemPool.acquire();
    if (item == null) {
      item = new MountItem();
    }

    final ViewNodeInfo viewNodeInfo = ViewNodeInfo.acquire();
    viewNodeInfo.setLayoutDirection(YogaDirection.INHERIT);

    item.init(
        component,
        host,
        content,
        null,
        viewNodeInfo,
        null,
        0,
        IMPORTANT_FOR_ACCESSIBILITY_AUTO,
        null);
    return item;
  }

  private static @Nullable DisplayListDrawable wrapDrawableIfPossible(
      Component component, Object content, LayoutOutput layoutOutput) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
        || ComponentsConfiguration.forceNotToCacheDisplayLists) {
      return null;
    }

    if (component == null || !component.shouldUseDisplayList()) {
      return null;
    }

    if (content == null
        || content instanceof DisplayListDrawable
        || !(content instanceof Drawable)) {
      return null;
    }

    return acquireDisplayListDrawable((Drawable) content, null);
  }

  static MountItem acquireMountItem(
      Component component, ComponentHost host, Object content, LayoutOutput layoutOutput) {
    MountItem item = sMountItemPool.acquire();
    if (item == null) {
      item = new MountItem();
    }

    item.init(
        component,
        host,
        content,
        layoutOutput,
        wrapDrawableIfPossible(component, content, layoutOutput));
    return item;
  }

  static LayoutOutput acquireLayoutOutput() {
    LayoutOutput output = sLayoutOutputPool.acquire();
    if (output == null) {
      output = new LayoutOutput();
    }
    output.acquire();

    return output;
  }

  static DisplayListContainer acquireDisplayListContainer() {
    DisplayListContainer displayListContainer = sDisplayListContainerPool.acquire();
    if (displayListContainer == null) {
      displayListContainer = new DisplayListContainer();
    }

    return displayListContainer;
  }

  static VisibilityOutput acquireVisibilityOutput() {
    VisibilityOutput output = sVisibilityOutputPool.acquire();
    if (output == null) {
      output = new VisibilityOutput();
    }

    return output;
  }

  static VisibilityItem acquireVisibilityItem(
      String globalKey,
      EventHandler<InvisibleEvent> invisibleHandler,
      EventHandler<UnfocusedVisibleEvent> unfocusedHandler,
      @Nullable EventHandler<VisibilityChangedEvent> visibilityChangedHandler) {
    VisibilityItem item = sVisibilityItemPool.acquire();
    if (item == null) {
      item = new VisibilityItem();
    }

    item.setGlobalKey(globalKey);
    item.setInvisibleHandler(invisibleHandler);
    item.setUnfocusedHandler(unfocusedHandler);
    item.setVisibilityChangedHandler(visibilityChangedHandler);

    return item;
  }

  static TestOutput acquireTestOutput() {
    if (sTestOutputPool == null) {
      sTestOutputPool = new RecyclePool<>("TestOutput", 64, true);
    }
    TestOutput output = sTestOutputPool.acquire();
    if (output == null) {
      output = new TestOutput();
    }

    return output;
  }

  static TestItem acquireTestItem() {
    if (sTestItemPool == null) {
      sTestItemPool = new RecyclePool<>("TestItem", 64, true);
    }
    TestItem item = sTestItemPool.acquire();
    if (item == null) {
      item = new TestItem();
    }
    item.setAcquired();

    return item;
  }

  static Output acquireOutput() {
    Output output = sOutputPool.acquire();
    if (output == null) {
      output = new Output();
    }

    return output;
  }

  static DiffNode acquireDiffNode() {
    DiffNode node = sDiffNodePool.acquire();
    if (node == null) {
      node = new DiffNode();
    }

    return node;
  }

  public static <T> Diff acquireDiff(T previous, T next) {
    Diff diff = sDiffPool.acquire();
    if (diff == null) {
      diff = new Diff();
    }
    diff.init(previous, next);

    return diff;
  }

  static ComponentTree.Builder acquireComponentTreeBuilder(ComponentContext c, Component root) {
    ComponentTree.Builder componentTreeBuilder = sComponentTreeBuilderPool.acquire();
    if (componentTreeBuilder == null) {
      componentTreeBuilder = new ComponentTree.Builder();
    }

    componentTreeBuilder.init(c, root);

    return componentTreeBuilder;
  }

  static StateHandler acquireStateHandler(@Nullable StateHandler fromStateHandler) {
    StateHandler stateHandler = sStateHandlerPool.acquire();
    if (stateHandler == null) {
      stateHandler = new StateHandler();
    }

    stateHandler.init(fromStateHandler);

    return stateHandler;
  }

  @Nullable
  static StateHandler acquireStateHandler() {
    return acquireStateHandler(null);
  }

  @ThreadSafe(enableChecks = false)
  static void release(ComponentTree.Builder componentTreeBuilder) {
    componentTreeBuilder.release();
    sComponentTreeBuilderPool.release(componentTreeBuilder);
  }

  @ThreadSafe(enableChecks = false)
  static void release(StateHandler stateHandler) {
    stateHandler.release();
    sStateHandlerPool.release(stateHandler);
  }

  @ThreadSafe(enableChecks = false)
  static void release(LayoutState state) {
    sLayoutStatePool.release(state);
  }

  @ThreadSafe(enableChecks = false)
  static void release(YogaNode node) {
    node.reset();
    sYogaNodePool.release(node);
  }

  @ThreadSafe(enableChecks = false)
  static void release(InternalNode node) {
    sInternalNodePool.release(node);
  }

  @ThreadSafe(enableChecks = false)
  static void release(NodeInfo nodeInfo) {
    sNodeInfoPool.release(nodeInfo);
  }

  @ThreadSafe(enableChecks = false)
  static void release(ViewNodeInfo viewNodeInfo) {
    sViewNodeInfoPool.release(viewNodeInfo);
  }

  @ThreadSafe(enableChecks = false)
  static void release(ComponentContext context, MountItem item) {
    item.release(context);
    sMountItemPool.release(item);
  }

  @ThreadSafe(enableChecks = false)
  static void release(LayoutOutput output) {
    sLayoutOutputPool.release(output);
  }

  static void release(DisplayListContainer displayListContainer) {
    displayListContainer.release();
    sDisplayListContainerPool.release(displayListContainer);
  }

  @ThreadSafe(enableChecks = false)
  static void release(VisibilityOutput output) {
    output.release();
    sVisibilityOutputPool.release(output);
  }

  @ThreadSafe(enableChecks = false)
  static void release(VisibilityItem item) {
    item.release();
    sVisibilityItemPool.release(item);
  }

  @ThreadSafe(enableChecks = false)
  static void release(TestOutput testOutput) {
    testOutput.release();
    sTestOutputPool.release(testOutput);
  }

  @ThreadSafe(enableChecks = false)
  static void release(TestItem testItem) {
    testItem.release();
    sTestItemPool.release(testItem);
  }

  @ThreadSafe(enableChecks = false)
  static void release(DiffNode node) {
    node.release();
    sDiffNodePool.release(node);
  }

  @ThreadSafe(enableChecks = false)
  static void release(Output output) {
    output.release();
    sOutputPool.release(output);
  }

  @ThreadSafe(enableChecks = false)
  public static void release(Diff diff) {
    diff.release();
    sDiffPool.release(diff);
  }

  static Object acquireMountContent(ComponentContext context, ComponentLifecycle lifecycle) {
    final MountContentPool pool = getMountContentPool(context, lifecycle);
    if (pool == null) {
      return lifecycle.createMountContent(context);
    }

    return pool.acquire(context, lifecycle);
  }

  static void release(ComponentContext context, ComponentLifecycle lifecycle, Object mountContent) {
    final MountContentPool pool = getMountContentPool(context, lifecycle);
    if (pool != null) {
      pool.release(mountContent);
    }
  }

  /**
   * Pre-allocates mount content for this component type within the pool for this context unless the
   * pre-allocation limit has been hit in which case we do nothing.
   */
  public static void maybePreallocateContent(
      ComponentContext context, ComponentLifecycle lifecycle) {
    final MountContentPool pool = getMountContentPool(context, lifecycle);
    if (pool != null) {
      pool.maybePreallocateContent(context, lifecycle);
    }
  }

  private static @Nullable MountContentPool getMountContentPool(
      ComponentContext wrappedContext, ComponentLifecycle lifecycle) {
    if (lifecycle.poolSize() == 0) {
      return null;
    }

    final Context context = getContextForMountPool(wrappedContext);

    synchronized (sMountContentLock) {
      SparseArray<MountContentPool> poolsArray = sMountContentPoolsByContext.get(context);
      if (poolsArray == null) {
        final Context rootContext = ContextUtils.getRootContext(context);
        if (sDestroyedRootContexts.containsKey(rootContext)) {
          return null;
        }

        ensureActivityCallbacks(context);
        poolsArray = new SparseArray<>();
        sMountContentPoolsByContext.put(context, poolsArray);
      }

      MountContentPool pool = poolsArray.get(lifecycle.getTypeId());
      if (pool == null) {
        pool = lifecycle.onCreateMountContentPool();
        poolsArray.put(lifecycle.getTypeId(), pool);
      }

      return pool;
    }
  }

  private static Context getContextForMountPool(ComponentContext wrappedContext) {
    final Context innerContext = wrappedContext.getBaseContext();
    if (innerContext instanceof ComponentContext) {
      throw new IllegalStateException("Double wrapped ComponentContext.");
    }

    return innerContext;
  }

  @GuardedBy("sMountContentLock")
  private static void ensureActivityCallbacks(Context context) {
    if (sActivityCallbacks == null && !sIsManualCallbacks) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        throw new RuntimeException(
            "Activity callbacks must be invoked manually below ICS (API level 14)");
      }
      sActivityCallbacks = new PoolsActivityCallback();
      ((Application) context.getApplicationContext())
          .registerActivityLifecycleCallbacks(sActivityCallbacks);
    }
  }

  static SparseArrayCompat<MountItem> acquireScrapMountItemsArray() {
    SparseArrayCompat<MountItem> sparseArray = sMountItemScrapArrayPool.acquire();
    if (sparseArray == null) {
      sparseArray = new SparseArrayCompat<>(SCRAP_ARRAY_INITIAL_SIZE);
    }

    return sparseArray;
  }

  @ThreadSafe(enableChecks = false)
  static void releaseScrapMountItemsArray(SparseArrayCompat<MountItem> sparseArray) {
    sMountItemScrapArrayPool.release(sparseArray);
  }

  static RectF acquireRectF() {
    RectF rect = sRectFPool.acquire();
    if (rect == null) {
      rect = new RectF();
    }

    return rect;
  }

  @ThreadSafe(enableChecks = false)
  static void releaseRectF(RectF rect) {
    rect.setEmpty();
    sRectFPool.release(rect);
  }

  static Rect acquireRect() {
    Rect rect = sRectPool.acquire();
    if (rect == null) {
      rect = new Rect();
    }

    return rect;
  }

  @ThreadSafe(enableChecks = false)
  static void release(Rect rect) {
    rect.setEmpty();
    sRectPool.release(rect);
  }

  static Edges acquireEdges() {
    Edges spacing = sEdgesPool.acquire();
    if (spacing == null) {
      spacing = new Edges();
    }

    return spacing;
  }

  @ThreadSafe(enableChecks = false)
  static void release(Edges edges) {
    edges.reset();
    sEdgesPool.release(edges);
  }

  /** Empty implementation of the {@link Application.ActivityLifecycleCallbacks} interface */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static class PoolsActivityCallback implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      ComponentsPools.onContextCreated(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivityResumed(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivityPaused(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivityStopped(Activity activity) {
      // Do nothing.
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
      // Do nothing.
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
      ComponentsPools.onContextDestroyed(activity);
    }
  }

  static void onContextCreated(Context context) {
    synchronized (sMountContentLock) {
      if (sMountContentPoolsByContext.containsKey(context)) {
        throw new IllegalStateException(
            "The MountContentPools has a reference to an activity that has just been created");
      }
    }
  }

  static void onContextDestroyed(Context context) {
    synchronized (sMountContentLock) {
      sMountContentPoolsByContext.remove(context);

      // Clear any context wrappers holding a reference to this activity.
      final Iterator<Map.Entry<Context, SparseArray<MountContentPool>>> it =
          sMountContentPoolsByContext.entrySet().iterator();

      while (it.hasNext()) {
        final Context contextKey = it.next().getKey();
        if (isContextWrapper(contextKey, context)) {
          it.remove();
        }
      }

      sDestroyedRootContexts.put(ContextUtils.getRootContext(context), true);
    }
  }

  /** Call from tests to clear external references. */
  public static void clearMountContentPools() {
    synchronized (sMountContentLock) {
      sMountContentPoolsByContext.clear();
    }
  }

  /** Clear pools for all the internal util objects, excluding mount content. */
  public static void clearInternalUtilPools() {
    sLayoutStatePool.clear();
    sYogaNodePool.clear();
    sInternalNodePool.clear();
    sNodeInfoPool.clear();
    sViewNodeInfoPool.clear();
    sMountItemPool.clear();
    sLayoutOutputPool.clear();
    sDisplayListContainerPool.clear();
    sVisibilityOutputPool.clear();
    sVisibilityItemPool.clear();
    if (sTestOutputPool != null) {
      sTestOutputPool.clear();
    }
    if (sTestItemPool != null) {
      sTestItemPool.clear();
    }
    sOutputPool.clear();
    sDiffNodePool.clear();
    sDiffPool.clear();
    sComponentTreeBuilderPool.clear();
    sStateHandlerPool.clear();
    sMountItemScrapArrayPool.clear();
    sRectFPool.clear();
    sEdgesPool.clear();
    sDisplayListDrawablePool.clear();
    if (sBorderColorDrawablePool != null) {
      sBorderColorDrawablePool.clear();
    }
    sArraySetPool.clear();
    sArrayDequePool.clear();
    sRenderStatePool.clear();
    sLithoViewArrayListPool.clear();
  }

  /** Check whether contextWrapper is a wrapper of baseContext */
  private static boolean isContextWrapper(Context contextWrapper, Context baseContext) {
    Context currentContext = contextWrapper;
    while (currentContext instanceof ContextWrapper) {
      currentContext = ((ContextWrapper) currentContext).getBaseContext();

      if (currentContext == baseContext) {
        return true;
      }
    }

    return false;
  }

  public static DisplayListDrawable acquireDisplayListDrawable(
      Drawable content, DisplayListContainer displayListContainer) {

    // When we are wrapping drawable with DisplayListDrawable we need to make sure that
    // wrapped DisplayListDrawable has the same view callback as original one had for correct
    // view invalidations.
    final Drawable.Callback callback = content.getCallback();

    DisplayListDrawable displayListDrawable = sDisplayListDrawablePool.acquire();
    if (displayListDrawable == null) {
      displayListDrawable = new DisplayListDrawable(content, displayListContainer);
    } else {
      displayListDrawable.setWrappedDrawable(content, displayListContainer);
    }
    displayListDrawable.setCallback(callback);

    return displayListDrawable;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(DisplayListDrawable displayListDrawable) {
    displayListDrawable.release();
    sDisplayListDrawablePool.release(displayListDrawable);
  }

  public static BorderColorDrawable acquireBorderColorDrawable() {
    if (sBorderColorDrawablePool == null) {
      sBorderColorDrawablePool = new RecyclePool<>("BorderColorDrawable", 10, true);
    }
    BorderColorDrawable drawable = sBorderColorDrawablePool.acquire();
    if (drawable == null) {
      drawable = new BorderColorDrawable();
    }

    return drawable;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(BorderColorDrawable borderColorDrawable) {
    borderColorDrawable.reset();
    sBorderColorDrawablePool.release(borderColorDrawable);
  }

  public static <E> ArraySet<E> acquireArraySet() {
    ArraySet<E> set = sArraySetPool.acquire();
    if (set == null) {
      set = new ArraySet<>();
    }
    return set;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(ArraySet set) {
    set.clear();
    sArraySetPool.release(set);
  }

  public static <E> ArrayDeque<E> acquireArrayDeque() {
    ArrayDeque<E> deque = sArrayDequePool.acquire();
    if (deque == null) {
      deque = new ArrayDeque<>();
    }
    return deque;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(ArrayDeque deque) {
    deque.clear();
    sArrayDequePool.release(deque);
  }

  public static RenderState acquireRenderState() {
    RenderState renderState = sRenderStatePool.acquire();
    if (renderState == null) {
      renderState = new RenderState();
    }
    return renderState;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(RenderState renderState) {
    renderState.reset();
    sRenderStatePool.release(renderState);
  }

  public static ArrayList<LithoView> acquireLithoViewArrayList() {
    ArrayList<LithoView> arrayList = sLithoViewArrayListPool.acquire();
    if (arrayList == null) {
      arrayList = new ArrayList<>(5);
    }

    return arrayList;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(ArrayList<LithoView> arrayList) {
    arrayList.clear();
    sLithoViewArrayListPool.release(arrayList);
  }

  static List<MountContentPool> getMountContentPools() {
    final ArrayList<MountContentPool> pools = new ArrayList<>();
    synchronized (sMountContentLock) {
      for (SparseArray<MountContentPool> contentPools :
          ComponentsPools.sMountContentPoolsByContext.values()) {
        for (int i = 0, count = contentPools.size(); i < count; i++) {
          pools.add(contentPools.valueAt(i));
        }
      }
    }
    return pools;
  }

  /**
   * Toggles a Yoga setting on whether to print debug logs to adb.
   *
   * @param enable whether to print logs or not
   */
  public static void setPrintYogaDebugLogs(boolean enable) {
    initYogaConfigIfNecessary();
    synchronized (sYogaConfigLock) {
      sYogaConfig.setPrintTreeFlag(enable);
    }
  }

  private static void initYogaConfigIfNecessary() {
    if (sYogaConfig == null) {
      synchronized (sYogaConfigLock) {
        if (sYogaConfig == null) {
          sYogaConfig = new YogaConfig();
          sYogaConfig.setUseWebDefaults(true);
        }
      }
    }
  }

  @VisibleForTesting
  @GuardedBy("sMountContentLock")
  static void clearActivityCallbacks() {
    sActivityCallbacks = null;
  }
}
