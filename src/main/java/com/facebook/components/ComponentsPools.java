/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.util.Pools;
import android.support.v4.util.SparseArrayCompat;
import android.util.SparseArray;

import com.facebook.components.config.ComponentsConfiguration;
import com.facebook.components.displaylist.DisplayList;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.yoga.YogaConfig;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaExperimentalFeature;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaNodeAPI;
import com.facebook.yoga.CSSNodeDEPRECATED;
import com.facebook.yoga.Spacing;

import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;

/**
 * Pools of recycled resources.
 *
 * FUTURE: Consider customizing the pool implementation such that we can match buffer sizes. Without
 * this we will tend to expand all buffers to the largest size needed.
 */
public class ComponentsPools {

  private static YogaConfig sYogaConfig;

  private static final int SCRAP_ARRAY_INITIAL_SIZE = 4;

  private ComponentsPools() {
  }

  // FUTURE: tune pool max sizes

  private static final Object mountContentLock = new Object();

  private static final Pools.SynchronizedPool<LayoutState> sLayoutStatePool =
      new Pools.SynchronizedPool<>(64);

  private static final Pools.SynchronizedPool<InternalNode> sInternalNodePool =
      new Pools.SynchronizedPool<>(256);

  private static final Pools.SynchronizedPool<NodeInfo> sNodeInfoPool =
      new Pools.SynchronizedPool<>(256);

  private static final Pools.SynchronizedPool<ViewNodeInfo> sViewNodeInfoPool =
      new Pools.SynchronizedPool<>(64);

  private static final Pools.SynchronizedPool<YogaNodeAPI> sYogaNodePool =
      new Pools.SynchronizedPool<>(256);

  private static final Pools.SynchronizedPool<MountItem> sMountItemPool =
      new Pools.SynchronizedPool<>(256);

  private static final Map<Context, SparseArray<PoolWithCount>>
      sMountContentPoolsByContext = new ConcurrentHashMap<>(4);

  private static final Pools.SynchronizedPool<LayoutOutput> sLayoutOutputPool =
      new Pools.SynchronizedPool<>(256);

  private static final Pools.SynchronizedPool<VisibilityOutput> sVisibilityOutputPool =
      new Pools.SynchronizedPool<>(64);

  // These are lazily initialized as they are only needed when we're in a test environment.
  private static Pools.SynchronizedPool<TestOutput> sTestOutputPool = null;
  private static Pools.SynchronizedPool<TestItem> sTestItemPool = null;

  private static final Pools.SynchronizedPool<VisibilityItem> sVisibilityItemPool =
      new Pools.SynchronizedPool<>(64);

  private static final Pools.SynchronizedPool<Output<?>> sOutputPool =
      new Pools.SynchronizedPool<>(20);

  private static final Pools.SynchronizedPool<DiffNode> sDiffNodePool =
      new Pools.SynchronizedPool<>(256);

  private static final Pools.SynchronizedPool<Diff<?>> sDiffPool =
      new Pools.SynchronizedPool<>(20);

  private static final Pools.SynchronizedPool<ComponentTree.Builder> sComponentTreeBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private static final Pools.SynchronizedPool<StateHandler> sStateHandlerPool =
      new Pools.SynchronizedPool<>(10);

  private static final Pools.SimplePool<SparseArrayCompat<MountItem>> sMountItemScrapArrayPool =
      new Pools.SimplePool<>(8);

  private static final Pools.SimplePool<SparseArrayCompat<Touchable>> sTouchableScrapArrayPool =
      new Pools.SimplePool<>(4);

  private static final Pools.SynchronizedPool<RectF> sRectFPool =
      new Pools.SynchronizedPool<>(4);

  private static final Pools.SynchronizedPool<Rect> sRectPool =
      new Pools.SynchronizedPool<>(30);

  private static final Pools.SynchronizedPool<Spacing> sSpacingPool =
      new Pools.SynchronizedPool<>(30);

  private static final Pools.SynchronizedPool<TransitionContext> sTransitionContextPool =
      new Pools.SynchronizedPool<>(2);

  private static final Pools.SimplePool<TransitionManager> sTransitionManagerPool =
      new Pools.SimplePool<>(2);

  static final Pools.Pool<DisplayListDrawable> sDisplayListDrawablePool =
      new Pools.SimplePool<>(10);

  private static final Pools.SynchronizedPool<TreeProps> sTreePropsMapPool =
      new Pools.SynchronizedPool<>(10);

  // Lazily initialized when acquired first time, as this is not a common use case.
  private static Pools.Pool<BorderColorDrawable> sBorderColorDrawablePool = null;

  private static PoolsActivityCallback sActivityCallbacks;

  /**
   * To support Gingerbread (where the registerActivityLifecycleCallbacks API
   * doesn't exist), we allow apps to explicitly invoke activity callbacks. If
   * this is enabled we'll throw if we are passed a context for which we have
   * no record.
   */
  static boolean sIsManualCallbacks;

  /**
   * Local cache of ComponentsConfiguration.shouldUseCSSNodeJNI which ensures
   * the value is only read once.
   * Once any InternalNode uses any of CSSNodeDEPRECATED or
   * YogaNode all future InternalNodes must do the same as to not mix and match.
   */
  private static Boolean sShouldUseCSSNodeJNI = null;

  static LayoutState acquireLayoutState(ComponentContext context) {
    LayoutState state = sLayoutStatePool.acquire();
    if (state == null) {
      state = new LayoutState();
    }
    state.init(context);

    return state;
  }

  static synchronized YogaNodeAPI acquireYogaNode() {
    YogaNodeAPI node = sYogaNodePool.acquire();
    if (node == null) {
      if (sShouldUseCSSNodeJNI == null) {
        sShouldUseCSSNodeJNI = ComponentsConfiguration.shouldUseCSSNodeJNI;
      }

      if (sShouldUseCSSNodeJNI) {
        if (sYogaConfig == null) {
          sYogaConfig = new YogaConfig();
          sYogaConfig.setExperimentalFeatureEnabled(YogaExperimentalFeature.ROUNDING, true);
        }
        node = new YogaNode(sYogaConfig);
      } else {
        node = new CSSNodeDEPRECATED();
      }
    }

    return node;
  }

  static synchronized InternalNode acquireInternalNode(
      ComponentContext componentContext,
      Resources resources) {
    InternalNode node = sInternalNodePool.acquire();
    if (node == null) {
      node = new InternalNode();
    }

    node.init(acquireYogaNode(), componentContext, resources);
    return node;
  }

  static synchronized NodeInfo acquireNodeInfo() {
    NodeInfo nodeInfo = sNodeInfoPool.acquire();
    if (nodeInfo == null) {
      nodeInfo = new NodeInfo();
    }

    return nodeInfo;
  }

  static synchronized ViewNodeInfo acquireViewNodeInfo() {
    ViewNodeInfo viewNodeInfo = sViewNodeInfoPool.acquire();
    if (viewNodeInfo == null) {
      viewNodeInfo = new ViewNodeInfo();
    }

    return viewNodeInfo;
  }

  static MountItem acquireRootHostMountItem(
      Component<?> component,
      ComponentHost host,
      Object content) {
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
        IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    return item;
  }

  static MountItem acquireMountItem(
      Component<?> component,
      ComponentHost host,
      Object content,
      LayoutOutput layoutOutput) {
    MountItem item = sMountItemPool.acquire();
    if (item == null) {
      item = new MountItem();
    }

    item.init(component, host, content, layoutOutput, null);
    return item;
  }

  static Object acquireMountContent(Context context, int componentId) {

    if (context instanceof ComponentContext) {
      context = ((ComponentContext) context).getBaseContext();

      if (context instanceof ComponentContext) {
        throw new IllegalStateException("Double wrapped ComponentContext.");
      }
    }

    final Pools.SynchronizedPool<Object> pool;

    synchronized (mountContentLock) {

      if (sActivityCallbacks == null && !sIsManualCallbacks) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
          throw new RuntimeException(
              "Activity callbacks must be invoked manually below ICS (API level 14)");
        }
        sActivityCallbacks = new PoolsActivityCallback();
        ((Application) context.getApplicationContext())
            .registerActivityLifecycleCallbacks(sActivityCallbacks);
      }

      SparseArray<PoolWithCount> poolsArray =
          sMountContentPoolsByContext.get(context);

      if (poolsArray == null) {
        // The context is created here because we are sure the Activity is alive at this point in
        // contrast of the release call where the Activity might by gone.
        sMountContentPoolsByContext.put(context, new SparseArray<PoolWithCount>());
        return null;
      }

      pool = poolsArray.get(componentId);
      if (pool == null) {
        return null;
      }
    }

    return pool.acquire();
  }

  static LayoutOutput acquireLayoutOutput() {
    LayoutOutput output = sLayoutOutputPool.acquire();
    if (output == null) {
      output = new LayoutOutput();
    }

    return output;
  }

  static VisibilityOutput acquireVisibilityOutput() {
    VisibilityOutput output = sVisibilityOutputPool.acquire();
    if (output == null) {
      output = new VisibilityOutput();
    }

    return output;
  }

  static VisibilityItem acquireVisibilityItem(EventHandler invisibleHandler) {
    VisibilityItem item = sVisibilityItemPool.acquire();
    if (item == null) {
      item = new VisibilityItem();
    }

    item.setInvisibleHandler(invisibleHandler);

    return item;
  }

  static TestOutput acquireTestOutput() {
    if (sTestOutputPool == null) {
      sTestOutputPool = new Pools.SynchronizedPool<>(64);
    }
    TestOutput output = sTestOutputPool.acquire();
    if (output == null) {
      output = new TestOutput();
    }

    return output;
  }

  static TestItem acquireTestItem() {
    if (sTestItemPool == null) {
      sTestItemPool = new Pools.SynchronizedPool<>(64);
    }
    TestItem item = sTestItemPool.acquire();
    if (item == null) {
      item = new TestItem();
    }

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

  static Diff acquireDiff() {
    Diff diff = sDiffPool.acquire();
    if (diff == null) {
      diff = new Diff();
    }

    return diff;
  }

  static ComponentTree.Builder acquireComponentTreeBuilder(ComponentContext c, Component<?> root) {
    ComponentTree.Builder componentTreeBuilder = sComponentTreeBuilderPool.acquire();
    if (componentTreeBuilder == null) {
      componentTreeBuilder = new ComponentTree.Builder();
    }

    componentTreeBuilder.init(c, root);

    return componentTreeBuilder;
  }

  static StateHandler acquireStateHandler(StateHandler fromStateHandler) {
    StateHandler stateHandler = sStateHandlerPool.acquire();
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
    TransitionContext transitionContext = sTransitionContextPool.acquire();
    if (transitionContext == null) {
      transitionContext = new TransitionContext();
    }

    return transitionContext;
  }

  static TransitionManager acquireTransitionManager() {
    TransitionManager transitionManager = sTransitionManagerPool.acquire();
    if (transitionManager == null) {
      transitionManager = new TransitionManager();
    }

    return transitionManager;
  }

  public static TreeProps acquireTreeProps() {
    TreeProps treeProps = sTreePropsMapPool.acquire();
    if (treeProps == null) {
      treeProps = new TreeProps();
    }

    return treeProps;
  }

  //TODO t16407516 shb: change all "enableChecks = false" here to @TakesOwnership
  @ThreadSafe(enableChecks = false)
  public static void release(TreeProps treeProps) {
    treeProps.reset();
    sTreePropsMapPool.release(treeProps);
  }

  @ThreadSafe(enableChecks = false)
  static void release(TransitionManager transitionManager) {
    transitionManager.reset();
    sTransitionManagerPool.release(transitionManager);
  }

  @ThreadSafe(enableChecks = false)
  static void release(TransitionContext transitionContext) {
    transitionContext.reset();
    sTransitionContextPool.release(transitionContext);
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
  static void release(YogaNodeAPI node) {
    node.reset();
    sYogaNodePool.release(node);
  }

  @ThreadSafe(enableChecks = false)
  static void release(InternalNode node) {
    node.release();
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
    item.release(context);
    sMountItemPool.release(item);
  }

  @ThreadSafe(enableChecks = false)
  static void release(LayoutOutput output) {
    output.release();
    sLayoutOutputPool.release(output);
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
  static void release(Diff diff) {
    diff.release();
    sDiffPool.release(diff);
  }

  @ThreadSafe(enableChecks = false)
  static void release(Context context, ComponentLifecycle lifecycle, Object mountContent) {

    if (context instanceof ComponentContext) {
      context = ((ComponentContext) context).getBaseContext();

      if (context instanceof ComponentContext) {
        throw new IllegalStateException("Double wrapped ComponentContext.");
      }
    }

    PoolWithCount pool = null;

    synchronized (mountContentLock) {
      SparseArray<PoolWithCount> poolsArray =
          sMountContentPoolsByContext.get(context);
      if (poolsArray != null) {
        pool = poolsArray.get(lifecycle.getId());
        if (pool == null) {
          pool = new PoolWithCount(lifecycle.poolSize());
          poolsArray.put(lifecycle.getId(), pool);
        }
      }

      if (pool != null) {
        pool.release(mountContent);
      }
    }
  }

  static boolean canAddMountContentToPool(Context context, ComponentLifecycle lifecycle) {
    if (lifecycle.poolSize() == 0) {
      return false;
    }

    final SparseArray<PoolWithCount> poolsArray =
        sMountContentPoolsByContext.get(context);

    if (poolsArray == null) {
      return true;
    }

    final PoolWithCount pool = poolsArray.get(lifecycle.getId());
    return pool == null || !pool.isFull();
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

  static SparseArrayCompat<Touchable> acquireScrapTouchablesArray() {
    SparseArrayCompat<Touchable> sparseArray = sTouchableScrapArrayPool.acquire();
    if (sparseArray == null) {
      sparseArray = new SparseArrayCompat<>(SCRAP_ARRAY_INITIAL_SIZE);
    }

    return sparseArray;
  }

  @ThreadSafe(enableChecks = false)
  static void releaseScrapTouchablesArray(SparseArrayCompat<Touchable> sparseArray) {
    sTouchableScrapArrayPool.release(sparseArray);
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

  static Spacing acquireSpacing() {
    Spacing spacing = sSpacingPool.acquire();
    if (spacing == null) {
      spacing = new Spacing();
    }

    return spacing;
  }

  @ThreadSafe(enableChecks = false)
  static void release(Spacing spacing) {
    spacing.reset();
    sSpacingPool.release(spacing);
  }

  /**
   * Empty implementation of the {@link Application.ActivityLifecycleCallbacks} interface
   */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static class PoolsActivityCallback
      implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
      ComponentsPools.onActivityCreated(activity, savedInstanceState);
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
