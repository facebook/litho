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
    LayoutState state = ComponentsConfiguration.disablePools ? null : sLayoutStatePool.acquire();
    if (state == null) {
      state = new LayoutState();
    }
    state.init(context);

    return state;
  }

  static YogaNode acquireYogaNode() {
    initYogaConfigIfNecessary();
    YogaNode node = ComponentsConfiguration.disablePools ? null : sYogaNodePool.acquire();
    if (node == null) {
      node =
          PoolsConfig.sYogaNodeFactory != null
              ? PoolsConfig.sYogaNodeFactory.create(sYogaConfig)
              : new YogaNode(sYogaConfig);
    }

    return node;
  }

  static InternalNode acquireInternalNode(ComponentContext componentContext) {
    InternalNode node = ComponentsConfiguration.disablePools ? null : sInternalNodePool.acquire();
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
    NodeInfo nodeInfo = ComponentsConfiguration.disablePools ? null : sNodeInfoPool.acquire();
    if (nodeInfo == null) {
      nodeInfo = new NodeInfo();
    }

    return nodeInfo;
  }

  static ViewNodeInfo acquireViewNodeInfo() {
    ViewNodeInfo viewNodeInfo =
        ComponentsConfiguration.disablePools ? null : sViewNodeInfoPool.acquire();
    if (viewNodeInfo == null) {
      viewNodeInfo = new ViewNodeInfo();
    }

    return viewNodeInfo;
  }

  static MountItem acquireRootHostMountItem(
      Component component, ComponentHost host, Object content) {
    MountItem item = ComponentsConfiguration.disablePools ? null : sMountItemPool.acquire();
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
        0,
        IMPORTANT_FOR_ACCESSIBILITY_AUTO,
        host.getContext().getResources().getConfiguration().orientation,
        null);
    return item;
  }

  static MountItem acquireMountItem(
      Component component, ComponentHost host, Object content, LayoutOutput layoutOutput) {
    MountItem item = ComponentsConfiguration.disablePools ? null : sMountItemPool.acquire();
    if (item == null) {
      item = new MountItem();
    }

    item.init(component, host, content, layoutOutput);
    return item;
  }

  static LayoutOutput acquireLayoutOutput() {
    LayoutOutput output = ComponentsConfiguration.disablePools ? null : sLayoutOutputPool.acquire();
    if (output == null) {
      output = new LayoutOutput();
    }
    output.acquire();

    return output;
  }

  static VisibilityOutput acquireVisibilityOutput() {
    VisibilityOutput output =
        ComponentsConfiguration.disablePools ? null : sVisibilityOutputPool.acquire();
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
    VisibilityItem item =
        ComponentsConfiguration.disablePools ? null : sVisibilityItemPool.acquire();
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
    TestOutput output = ComponentsConfiguration.disablePools ? null : sTestOutputPool.acquire();
    if (output == null) {
      output = new TestOutput();
    }

    return output;
  }

  static TestItem acquireTestItem() {
    if (sTestItemPool == null) {
      sTestItemPool = new RecyclePool<>("TestItem", 64, true);
    }
    TestItem item = ComponentsConfiguration.disablePools ? null : sTestItemPool.acquire();
    if (item == null) {
      item = new TestItem();
    }
    item.setAcquired();

    return item;
  }

  static Output acquireOutput() {
    Output output = ComponentsConfiguration.disablePools ? null : sOutputPool.acquire();
    if (output == null) {
      output = new Output();
    }

    return output;
  }

  static DiffNode acquireDiffNode() {
    DiffNode node = ComponentsConfiguration.disablePools ? null : sDiffNodePool.acquire();
    if (node == null) {
      node = new DiffNode();
    }

    return node;
  }

  public static <T> Diff acquireDiff(T previous, T next) {
    Diff diff = ComponentsConfiguration.disablePools ? null : sDiffPool.acquire();
    if (diff == null) {
      diff = new Diff();
    }
    diff.init(previous, next);

    return diff;
  }

  static ComponentTree.Builder acquireComponentTreeBuilder(ComponentContext c, Component root) {
    ComponentTree.Builder componentTreeBuilder =
        ComponentsConfiguration.disablePools ? null : sComponentTreeBuilderPool.acquire();
    if (componentTreeBuilder == null) {
      componentTreeBuilder = new ComponentTree.Builder();
    }

    componentTreeBuilder.init(c, root);

    return componentTreeBuilder;
  }

  static StateHandler acquireStateHandler(@Nullable StateHandler fromStateHandler) {
    StateHandler stateHandler =
        ComponentsConfiguration.disablePools ? null : sStateHandlerPool.acquire();
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
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    componentTreeBuilder.release();
    sComponentTreeBuilderPool.release(componentTreeBuilder);
  }

  @ThreadSafe(enableChecks = false)
  static void release(StateHandler stateHandler) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    stateHandler.release();
    sStateHandlerPool.release(stateHandler);
  }

  @ThreadSafe(enableChecks = false)
  static void release(LayoutState state) {
    sLayoutStatePool.release(state);
  }

  @ThreadSafe(enableChecks = false)
  static void release(YogaNode node) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
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
  static void release(Context context, MountItem item) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    item.release(context);
    sMountItemPool.release(item);
  }

  @ThreadSafe(enableChecks = false)
  static void release(LayoutOutput output) {
    sLayoutOutputPool.release(output);
  }

  @ThreadSafe(enableChecks = false)
  static void release(VisibilityOutput output) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    output.release();
    sVisibilityOutputPool.release(output);
  }

  @ThreadSafe(enableChecks = false)
  static void release(VisibilityItem item) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    item.release();
    sVisibilityItemPool.release(item);
  }

  @ThreadSafe(enableChecks = false)
  static void release(TestOutput testOutput) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    testOutput.release();
    sTestOutputPool.release(testOutput);
  }

  @ThreadSafe(enableChecks = false)
  static void release(TestItem testItem) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    testItem.release();
    sTestItemPool.release(testItem);
  }

  @ThreadSafe(enableChecks = false)
  static void release(DiffNode node) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    node.release();
    sDiffNodePool.release(node);
  }

  @ThreadSafe(enableChecks = false)
  static void release(Output output) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    output.release();
    sOutputPool.release(output);
  }

  @ThreadSafe(enableChecks = false)
  public static void release(Diff diff) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    diff.release();
    sDiffPool.release(diff);
  }

  static Object acquireMountContent(Context context, ComponentLifecycle lifecycle) {
    final MountContentPool pool = getMountContentPool(context, lifecycle);
    if (pool == null) {
      return lifecycle.createMountContent(context);
    }

    return pool.acquire(context, lifecycle);
  }

  static void release(Context context, ComponentLifecycle lifecycle, Object mountContent) {
    final MountContentPool pool = getMountContentPool(context, lifecycle);
    if (pool != null) {
      pool.release(mountContent);
    }
  }

  /**
   * Pre-allocates mount content for this component type within the pool for this context unless the
   * pre-allocation limit has been hit in which case we do nothing.
   */
  public static void maybePreallocateContent(Context context, ComponentLifecycle lifecycle) {
    final MountContentPool pool = getMountContentPool(context, lifecycle);
    if (pool != null) {
      pool.maybePreallocateContent(context, lifecycle);
    }
  }

  private static @Nullable MountContentPool getMountContentPool(
      Context context, ComponentLifecycle lifecycle) {
    if (lifecycle.poolSize() == 0) {
      return null;
    }

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
    RectF rect = ComponentsConfiguration.disablePools ? null : sRectFPool.acquire();
    if (rect == null) {
      rect = new RectF();
    }

    return rect;
  }

  @ThreadSafe(enableChecks = false)
  static void releaseRectF(RectF rect) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    rect.setEmpty();
    sRectFPool.release(rect);
  }

  static Rect acquireRect() {
    Rect rect = ComponentsConfiguration.disablePools ? null : sRectPool.acquire();
    if (rect == null) {
      rect = new Rect();
    }

    return rect;
  }

  @ThreadSafe(enableChecks = false)
  static void release(Rect rect) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    rect.setEmpty();
    sRectPool.release(rect);
  }

  static Edges acquireEdges() {
    Edges spacing = ComponentsConfiguration.disablePools ? null : sEdgesPool.acquire();
    if (spacing == null) {
      spacing = new Edges();
    }

    return spacing;
  }

  @ThreadSafe(enableChecks = false)
  static void release(Edges edges) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
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

  public static DisplayListDrawable acquireDisplayListDrawable(Drawable content) {
    // When we are wrapping drawable with DisplayListDrawable we need to make sure that
    // wrapped DisplayListDrawable has the same view callback as original one had for correct
    // view invalidations.
    final Drawable.Callback callback = content.getCallback();

    DisplayListDrawable displayListDrawable =
        ComponentsConfiguration.disablePools ? null : sDisplayListDrawablePool.acquire();
    if (displayListDrawable == null) {
      displayListDrawable = new DisplayListDrawable(content);
    } else {
      displayListDrawable.setWrappedDrawable(content);
    }
    displayListDrawable.setCallback(callback);

    return displayListDrawable;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(DisplayListDrawable displayListDrawable) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    displayListDrawable.release();
    sDisplayListDrawablePool.release(displayListDrawable);
  }

  public static BorderColorDrawable acquireBorderColorDrawable() {
    if (sBorderColorDrawablePool == null && !ComponentsConfiguration.disablePools) {
      sBorderColorDrawablePool = new RecyclePool<>("BorderColorDrawable", 10, true);
    }
    BorderColorDrawable drawable =
        ComponentsConfiguration.disablePools ? null : sBorderColorDrawablePool.acquire();
    if (drawable == null) {
      drawable = new BorderColorDrawable();
    }

    return drawable;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(BorderColorDrawable borderColorDrawable) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    borderColorDrawable.reset();
    sBorderColorDrawablePool.release(borderColorDrawable);
  }

  public static <E> ArraySet<E> acquireArraySet() {
    ArraySet<E> set = ComponentsConfiguration.disablePools ? null : sArraySetPool.acquire();
    if (set == null) {
      set = new ArraySet<>();
    }
    return set;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(ArraySet set) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    set.clear();
    sArraySetPool.release(set);
  }

  public static <E> ArrayDeque<E> acquireArrayDeque() {
    ArrayDeque<E> deque = ComponentsConfiguration.disablePools ? null : sArrayDequePool.acquire();
    if (deque == null) {
      deque = new ArrayDeque<>();
    }
    return deque;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(ArrayDeque deque) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    deque.clear();
    sArrayDequePool.release(deque);
  }

  public static RenderState acquireRenderState() {
    RenderState renderState =
        ComponentsConfiguration.disablePools ? null : sRenderStatePool.acquire();
    if (renderState == null) {
      renderState = new RenderState();
    }
    return renderState;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(RenderState renderState) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
    renderState.reset();
    sRenderStatePool.release(renderState);
  }

  public static ArrayList<LithoView> acquireLithoViewArrayList() {
    ArrayList<LithoView> arrayList =
        ComponentsConfiguration.disablePools ? null : sLithoViewArrayListPool.acquire();
    if (arrayList == null) {
      arrayList = new ArrayList<>(5);
    }

    return arrayList;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(ArrayList<LithoView> arrayList) {
    if (ComponentsConfiguration.disablePools) {
      return;
    }
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
