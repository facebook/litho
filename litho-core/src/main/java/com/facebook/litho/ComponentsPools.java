/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
 * FUTURE: Consider customizing the pool implementation such that we can match buffer sizes. Without
 * this we will tend to expand all buffers to the largest size needed.
 */
public class ComponentsPools {

  private static final int SCRAP_ARRAY_INITIAL_SIZE = 4;

  @GuardedBy("sYogaConfigLock")
  private static YogaConfig sYogaConfig;

  private ComponentsPools() {
  }

  // FUTURE: tune pool max sizes

  private static final Object sMountContentLock = new Object();
  private static final Object sYogaConfigLock = new Object();

  static final RecyclePool<LayoutState> sLayoutStatePool =
      new RecyclePool<>("LayoutState", 64, true);

  static final RecyclePool<InternalNode> sInternalNodePool =
      new RecyclePool<>("InternalNode", 256, true);

  static final RecyclePool<NodeInfo> sNodeInfoPool =
      new RecyclePool<>("NodeInfo", 256, true);

  static final RecyclePool<ViewNodeInfo> sViewNodeInfoPool =
      new RecyclePool<>("ViewNodeInfo", 64, true);

  static final RecyclePool<YogaNode> sYogaNodePool =
      new RecyclePool<>("YogaNode", 256, true);

  static final RecyclePool<MountItem> sMountItemPool =
      new RecyclePool<>("MountItem", 256, true);

  @GuardedBy("sMountContentLock")
  private static final Map<Context, SparseArray<MountContentPool>> sMountContentPoolsByContext =
      new HashMap<>(4);

  static final RecyclePool<LayoutOutput> sLayoutOutputPool =
      new RecyclePool<>("LayoutOutput", 256, true);

  static final RecyclePool<DisplayListContainer> sDisplayListContainerPool =
      new RecyclePool<>("DisplayListContainer", 64, true);

  static final RecyclePool<VisibilityOutput> sVisibilityOutputPool =
      new RecyclePool<>("VisibilityOutput", 64, true);

  // These are lazily initialized as they are only needed when we're in a test environment.
  static RecyclePool<TestOutput> sTestOutputPool = null;
  static RecyclePool<TestItem> sTestItemPool = null;

  static final RecyclePool<VisibilityItem> sVisibilityItemPool =
      new RecyclePool<>("VisibilityItem", 64, true);

  static final RecyclePool<Output<?>> sOutputPool =
      new RecyclePool<>("Output", 20, true);

  static final RecyclePool<DiffNode> sDiffNodePool =
      new RecyclePool<>("DiffNode", 256, true);

  static final RecyclePool<Diff<?>> sDiffPool =
      new RecyclePool<>("Diff", 20, true);

  static final RecyclePool<ComponentTree.Builder> sComponentTreeBuilderPool =
      new RecyclePool<>("ComponentTree.Builder", 2, true);

  static final RecyclePool<StateHandler> sStateHandlerPool =
      new RecyclePool<>("StateHandler", 10, true);

  static final RecyclePool<SparseArrayCompat<MountItem>> sMountItemScrapArrayPool =
      new RecyclePool<>("MountItemScrapArray", 8, false);

  static final RecyclePool<RectF> sRectFPool =
      new RecyclePool<>("RectF", 4, true);

  static final RecyclePool<Rect> sRectPool =
      new RecyclePool<>("Rect", 30, true);

  static final RecyclePool<Edges> sEdgesPool =
      new RecyclePool<>("Edges", 30, true);

  static final RecyclePool<TransitionContext> sTransitionContextPool =
      new RecyclePool<>("TransitionContext", 2, true);

  static final RecyclePool<DisplayListDrawable> sDisplayListDrawablePool =
      new RecyclePool<>("DisplayListDrawable", 10, false);

  static final RecyclePool<TreeProps> sTreePropsMapPool =
      new RecyclePool<>("TreeProps", 10, true);

  static final RecyclePool<ArraySet> sArraySetPool =
      new RecyclePool<>("ArraySet", 10, true);

  static final RecyclePool<ArrayDeque> sArrayDequePool =
      new RecyclePool<>("ArrayDeque", 10, true);

  static final RecyclePool<LogEvent> sLogEventPool =
      new RecyclePool<>("LogEvent", 10, true);

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
   * To support Gingerbread (where the registerActivityLifecycleCallbacks API
   * doesn't exist), we allow apps to explicitly invoke activity callbacks. If
   * this is enabled we'll throw if we are passed a context for which we have
   * no record.
   */
  static boolean sIsManualCallbacks;

  static LayoutState acquireLayoutState(ComponentContext context) {
    LayoutState state = ComponentsConfiguration.usePooling ? sLayoutStatePool.acquire() : null;
    if (state == null) {
      state = new LayoutState();
    }
    state.init(context);

    return state;
  }

  static YogaNode acquireYogaNode() {
    if (sYogaConfig == null) {
      synchronized (sYogaConfigLock) {
        if (sYogaConfig == null) {
          sYogaConfig = new YogaConfig();
          sYogaConfig.setUseWebDefaults(true);
          sYogaConfig.setUseLegacyStretchBehaviour(true);
        }
      }
    }

    if (sYogaConfig.getLogger() != ComponentsConfiguration.YOGA_LOGGER) {
      synchronized (sYogaConfigLock) {
        sYogaConfig.setLogger(ComponentsConfiguration.YOGA_LOGGER);
      }
    }

    YogaNode node = ComponentsConfiguration.usePooling ? sYogaNodePool.acquire() : null;
    if (node == null) {
      node = new YogaNode(sYogaConfig);
    }

    return node;
  }

  static InternalNode acquireInternalNode(ComponentContext componentContext) {
    InternalNode node = ComponentsConfiguration.usePooling ? sInternalNodePool.acquire() : null;
    if (node == null) {
      node = new InternalNode();
    }

    node.init(acquireYogaNode(), componentContext);
    return node;
  }

  static NodeInfo acquireNodeInfo() {
    NodeInfo nodeInfo = ComponentsConfiguration.usePooling ? sNodeInfoPool.acquire() : null;
    if (nodeInfo == null) {
      nodeInfo = new NodeInfo();
    }

    return nodeInfo;
  }

  static ViewNodeInfo acquireViewNodeInfo() {
    ViewNodeInfo viewNodeInfo =
        ComponentsConfiguration.usePooling ? sViewNodeInfoPool.acquire() : null;
    if (viewNodeInfo == null) {
      viewNodeInfo = new ViewNodeInfo();
    }

    return viewNodeInfo;
  }

  static MountItem acquireRootHostMountItem(
      Component<?> component,
      ComponentHost host,
      Object content) {
    MountItem item = ComponentsConfiguration.usePooling ? sMountItemPool.acquire() : null;
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
        null,
        0,
        IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    return item;
  }

  private static @Nullable DisplayListDrawable wrapDrawableIfPossible(
      Component<?> component, Object content, LayoutOutput layoutOutput) {
    if (!LayoutState.isEligibleForCreatingDisplayLists()) {
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

    return acquireDisplayListDrawable((Drawable) content, layoutOutput.getDisplayListContainer());
  }

  static MountItem acquireMountItem(
      Component<?> component,
      ComponentHost host,
      Object content,
      LayoutOutput layoutOutput) {
    MountItem item = ComponentsConfiguration.usePooling ? sMountItemPool.acquire() : null;
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
    LayoutOutput output = ComponentsConfiguration.usePooling ? sLayoutOutputPool.acquire() : null;
    if (output == null) {
      output = new LayoutOutput();
    }
    output.acquire();

    return output;
  }

  static DisplayListContainer acquireDisplayListContainer() {
    DisplayListContainer displayListContainer =
        ComponentsConfiguration.usePooling ? sDisplayListContainerPool.acquire() : null;
    if (displayListContainer == null) {
      displayListContainer = new DisplayListContainer();
    }

    return displayListContainer;
  }

  static VisibilityOutput acquireVisibilityOutput() {
    VisibilityOutput output =
        ComponentsConfiguration.usePooling ? sVisibilityOutputPool.acquire() : null;
    if (output == null) {
      output = new VisibilityOutput();
    }

    return output;
  }

  static VisibilityItem acquireVisibilityItem(
      String globalKey,
      EventHandler<InvisibleEvent> invisibleHandler,
      EventHandler<UnfocusedVisibleEvent> unfocusedHandler) {
    VisibilityItem item = ComponentsConfiguration.usePooling ? sVisibilityItemPool.acquire() : null;
    if (item == null) {
      item = new VisibilityItem();
    }

    item.setGlobalKey(globalKey);
    item.setInvisibleHandler(invisibleHandler);
    item.setUnfocusedHandler(unfocusedHandler);

    return item;
  }

  static TestOutput acquireTestOutput() {
    if (sTestOutputPool == null) {
      sTestOutputPool = new RecyclePool<>("TestOutput", 64, true);
    }
    TestOutput output = ComponentsConfiguration.usePooling ? sTestOutputPool.acquire() : null;
    if (output == null) {
      output = new TestOutput();
    }

    return output;
  }

  static TestItem acquireTestItem() {
    if (sTestItemPool == null) {
      sTestItemPool = new RecyclePool<>("TestItem", 64, true);
    }
    TestItem item = ComponentsConfiguration.usePooling ? sTestItemPool.acquire() : null;
    if (item == null) {
      item = new TestItem();
    }
    item.setAcquired();

    return item;
  }

  static Output acquireOutput() {
    Output output = ComponentsConfiguration.usePooling ? sOutputPool.acquire() : null;
    if (output == null) {
      output = new Output();
    }

    return output;
  }

  static DiffNode acquireDiffNode() {
    DiffNode node = ComponentsConfiguration.usePooling ? sDiffNodePool.acquire() : null;
    if (node == null) {
      node = new DiffNode();
    }

    return node;
  }

  public static <T> Diff acquireDiff(T previous, T next) {
    Diff diff = ComponentsConfiguration.usePooling ? sDiffPool.acquire() : null;
    if (diff == null) {
      diff = new Diff();
    }
    diff.init(previous, next);

    return diff;
  }

  static ComponentTree.Builder acquireComponentTreeBuilder(ComponentContext c, Component<?> root) {
    ComponentTree.Builder componentTreeBuilder =
        ComponentsConfiguration.usePooling ? sComponentTreeBuilderPool.acquire() : null;
    if (componentTreeBuilder == null) {
      componentTreeBuilder = new ComponentTree.Builder();
    }

    componentTreeBuilder.init(c, root);

    return componentTreeBuilder;
  }

  static StateHandler acquireStateHandler(StateHandler fromStateHandler) {
    StateHandler stateHandler =
        ComponentsConfiguration.usePooling ? sStateHandlerPool.acquire() : null;
    if (stateHandler == null) {
      stateHandler = new StateHandler();
    }

    stateHandler.init(fromStateHandler);

    return stateHandler;
  }

  static StateHandler acquireStateHandler() {
    return acquireStateHandler(null);
  }

  static TransitionContext acquireTransitionContext() {
    TransitionContext transitionContext =
        ComponentsConfiguration.usePooling ? sTransitionContextPool.acquire() : null;
    if (transitionContext == null) {
      transitionContext = new TransitionContext();
    }

    return transitionContext;
  }

  public static TreeProps acquireTreeProps() {
    TreeProps treeProps = ComponentsConfiguration.usePooling ? sTreePropsMapPool.acquire() : null;
    if (treeProps == null) {
      treeProps = new TreeProps();
    }

    return treeProps;
  }

  public static LogEvent acquireLogEvent(int eventId) {
    LogEvent event = ComponentsConfiguration.usePooling ? sLogEventPool.acquire() : null;
    if (event == null) {
      event = new LogEvent();
    }

    event.setEventId(eventId);
    return event;
  }

  //TODO t16407516 shb: change all "enableChecks = false" here to @TakesOwnership
  @ThreadSafe(enableChecks = false)
  public static void release(TreeProps treeProps) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    treeProps.reset();
    sTreePropsMapPool.release(treeProps);
  }

  @ThreadSafe(enableChecks = false)
  static void release(TransitionContext transitionContext) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    transitionContext.reset();
    sTransitionContextPool.release(transitionContext);
  }

  @ThreadSafe(enableChecks = false)
  static void release(ComponentTree.Builder componentTreeBuilder) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    componentTreeBuilder.release();
    sComponentTreeBuilderPool.release(componentTreeBuilder);
  }

  @ThreadSafe(enableChecks = false)
  static void release(StateHandler stateHandler) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    stateHandler.release();
    sStateHandlerPool.release(stateHandler);
  }

  @ThreadSafe(enableChecks = false)
  static void release(LayoutState state) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    sLayoutStatePool.release(state);
  }

  @ThreadSafe(enableChecks = false)
  static void release(YogaNode node) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    node.reset();
    sYogaNodePool.release(node);
  }

  @ThreadSafe(enableChecks = false)
  static void release(InternalNode node) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    sInternalNodePool.release(node);
  }

  @ThreadSafe(enableChecks = false)
  static void release(NodeInfo nodeInfo) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    sNodeInfoPool.release(nodeInfo);
  }

  @ThreadSafe(enableChecks = false)
  static void release(ViewNodeInfo viewNodeInfo) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    sViewNodeInfoPool.release(viewNodeInfo);
  }

  @ThreadSafe(enableChecks = false)
  static void release(Context context, MountItem item) {
    item.release(context);
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    sMountItemPool.release(item);
  }

  @ThreadSafe(enableChecks = false)
  static void release(LayoutOutput output) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    sLayoutOutputPool.release(output);
  }

  static void release(DisplayListContainer displayListContainer) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    displayListContainer.release();
    sDisplayListContainerPool.release(displayListContainer);
  }

  @ThreadSafe(enableChecks = false)
  static void release(VisibilityOutput output) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    output.release();
    sVisibilityOutputPool.release(output);
  }

  @ThreadSafe(enableChecks = false)
  static void release(VisibilityItem item) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    item.release();
    sVisibilityItemPool.release(item);
  }

  @ThreadSafe(enableChecks = false)
  static void release(TestOutput testOutput) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    testOutput.release();
    sTestOutputPool.release(testOutput);
  }

  @ThreadSafe(enableChecks = false)
  static void release(TestItem testItem) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    testItem.release();
    sTestItemPool.release(testItem);
  }

  @ThreadSafe(enableChecks = false)
  static void release(DiffNode node) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    node.release();
    sDiffNodePool.release(node);
  }

  @ThreadSafe(enableChecks = false)
  static void release(Output output) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    output.release();
    sOutputPool.release(output);
  }

  @ThreadSafe(enableChecks = false)
  public static void release(Diff diff) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    diff.release();
    sDiffPool.release(diff);
  }

  static @Nullable Object acquireMountContent(Context context, int componentTypeId) {
    final MountContentPool pool = getMountContentPoolForAcquire(context, componentTypeId);
    if (pool == null) {
      return null;
    }

    return pool.acquire();
  }

  static void release(Context context, ComponentLifecycle lifecycle, Object mountContent) {
    final MountContentPool pool = getMountContentPoolForRelease(context, lifecycle);
    if (pool != null) {
      pool.release(mountContent);
    }
  }

  private static @Nullable MountContentPool getMountContentPoolForAcquire(
      Context wrappedContext, int typeId) {
    final Context context = getContextForMountPool(wrappedContext);

    synchronized (sMountContentLock) {
      SparseArray<MountContentPool> poolsArray = sMountContentPoolsByContext.get(context);
      if (poolsArray == null) {
        return null;
      }

      return poolsArray.get(typeId);
    }
  }

  private static @Nullable MountContentPool getMountContentPoolForRelease(
      Context wrappedContext, ComponentLifecycle lifecycle) {
    final Context context = getContextForMountPool(wrappedContext);
    final Context rootContext = ContextUtils.getRootContext(context);

    synchronized (sMountContentLock) {
      if (sDestroyedRootContexts.containsKey(rootContext)) {
        return null;
      }

      SparseArray<MountContentPool> poolsArray = sMountContentPoolsByContext.get(context);
      if (poolsArray == null) {
        ensureActivityCallbacks(context);
        poolsArray = new SparseArray<>();
        sMountContentPoolsByContext.put(context, poolsArray);
      }

      MountContentPool pool = poolsArray.get(lifecycle.getTypeId());
      if (pool == null) {
        pool =
            new MountContentPool(lifecycle.getClass().getSimpleName(), lifecycle.poolSize(), true);
        poolsArray.put(lifecycle.getTypeId(), pool);
      }

      return pool;
    }
  }

  private static Context getContextForMountPool(Context wrappedContext) {
    if (!(wrappedContext instanceof ComponentContext)) {
      return wrappedContext;
    }

    final Context innerContext = ((ComponentContext) wrappedContext).getBaseContext();
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

  @ThreadSafe(enableChecks = false)
  static boolean canAddMountContentToPool(Context context, ComponentLifecycle lifecycle) {
    if (context instanceof ComponentContext) {
      context = ((ComponentContext) context).getBaseContext();

      if (context instanceof ComponentContext) {
        throw new IllegalStateException("Double wrapped ComponentContext.");
      }
    }

    synchronized (sMountContentLock) {
      if (lifecycle.poolSize() == 0) {
        return false;
      }

      final SparseArray<MountContentPool> poolsArray = sMountContentPoolsByContext.get(context);

      if (poolsArray == null) {
        return true;
      }

      final RecyclePool pool = poolsArray.get(lifecycle.getTypeId());
      return pool == null || !pool.isFull();
    }
  }

  static SparseArrayCompat<MountItem> acquireScrapMountItemsArray() {
    SparseArrayCompat<MountItem> sparseArray =
        ComponentsConfiguration.usePooling ? sMountItemScrapArrayPool.acquire() : null;
    if (sparseArray == null) {
      sparseArray = new SparseArrayCompat<>(SCRAP_ARRAY_INITIAL_SIZE);
    }

    return sparseArray;
  }

  @ThreadSafe(enableChecks = false)
  static void releaseScrapMountItemsArray(SparseArrayCompat<MountItem> sparseArray) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    sMountItemScrapArrayPool.release(sparseArray);
  }

  static RectF acquireRectF() {
    RectF rect = ComponentsConfiguration.usePooling ? sRectFPool.acquire() : null;
    if (rect == null) {
      rect = new RectF();
    }

    return rect;
  }

  @ThreadSafe(enableChecks = false)
  static void releaseRectF(RectF rect) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    rect.setEmpty();
    sRectFPool.release(rect);
  }

  static Rect acquireRect() {
    Rect rect = ComponentsConfiguration.usePooling ? sRectPool.acquire() : null;
    if (rect == null) {
      rect = new Rect();
    }

    return rect;
  }

  @ThreadSafe(enableChecks = false)
  static void release(Rect rect) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    rect.setEmpty();
    sRectPool.release(rect);
  }

  static Edges acquireEdges() {
    Edges spacing = ComponentsConfiguration.usePooling ? sEdgesPool.acquire() : null;
    if (spacing == null) {
      spacing = new Edges();
    }

    return spacing;
  }

  @ThreadSafe(enableChecks = false)
  static void release(Edges edges) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    edges.reset();
    sEdgesPool.release(edges);
  }

  /**
   * Empty implementation of the {@link Application.ActivityLifecycleCallbacks} interface
   */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static class PoolsActivityCallback
      implements Application.ActivityLifecycleCallbacks {

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

  /**
   * Call from tests to clear external references.
   */
  public static void clearMountContentPools() {
    synchronized (sMountContentLock) {
      sMountContentPoolsByContext.clear();
    }
  }

  /**
   * Clear pools for all the internal util objects, excluding mount content.
   */
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
    sTransitionContextPool.clear();
    sTreePropsMapPool.clear();
    sLogEventPool.clear();
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

  /**
   * Check whether contextWrapper is a wrapper of baseContext
   */
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
      Drawable content,
      DisplayListContainer displayListContainer) {

    // When we are wrapping drawable with DisplayListDrawable we need to make sure that
    // wrapped DisplayListDrawable has the same view callback as original one had for correct
    // view invalidations.
    final Drawable.Callback callback = content.getCallback();

    DisplayListDrawable displayListDrawable =
        ComponentsConfiguration.usePooling ? sDisplayListDrawablePool.acquire() : null;
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
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    displayListDrawable.release();
    sDisplayListDrawablePool.release(displayListDrawable);
  }

  public static BorderColorDrawable acquireBorderColorDrawable() {
    if (sBorderColorDrawablePool == null) {
      sBorderColorDrawablePool = new RecyclePool<>("BorderColorDrawable", 10, true);
    }
    BorderColorDrawable drawable =
        ComponentsConfiguration.usePooling ? sBorderColorDrawablePool.acquire() : null;
    if (drawable == null) {
      drawable = new BorderColorDrawable();
    }

    return drawable;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(BorderColorDrawable borderColorDrawable) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    sBorderColorDrawablePool.release(borderColorDrawable);
  }

  public static <E> ArraySet<E> acquireArraySet() {
    ArraySet<E> set = ComponentsConfiguration.usePooling ? sArraySetPool.acquire() : null;
    if (set == null) {
      set = new ArraySet<>();
    }
    return set;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(ArraySet set) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    set.clear();
    sArraySetPool.release(set);
  }

  public static <E> ArrayDeque<E> acquireArrayDeque() {
    ArrayDeque<E> deque = ComponentsConfiguration.usePooling ? sArrayDequePool.acquire() : null;
    if (deque == null) {
      deque = new ArrayDeque<>();
    }
    return deque;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(ArrayDeque deque) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    deque.clear();
    sArrayDequePool.release(deque);
  }

  @ThreadSafe(enableChecks = false)
  public static void release(LogEvent event) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }
    event.reset();
    sLogEventPool.release(event);
  }

  public static RenderState acquireRenderState() {
    RenderState renderState =
        ComponentsConfiguration.usePooling ? sRenderStatePool.acquire() : null;
    if (renderState == null) {
      renderState = new RenderState();
    }
    return renderState;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(RenderState renderState) {
    if (!ComponentsConfiguration.usePooling) {
      return;
    }

    renderState.reset();
    sRenderStatePool.release(renderState);
  }

  public static ArrayList<LithoView> acquireLithoViewArrayList() {
    ArrayList<LithoView> arrayList =
        ComponentsConfiguration.usePooling ? sLithoViewArrayListPool.acquire() : null;
    if (arrayList == null) {
      arrayList = new ArrayList<>(5);
    }

    return arrayList;
  }

  @ThreadSafe(enableChecks = false)
  public static void release(ArrayList<LithoView> arrayList) {
    if (!ComponentsConfiguration.usePooling) {
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

  @VisibleForTesting
  static void clearActivityCallbacks() {
    sActivityCallbacks = null;
  }
}
