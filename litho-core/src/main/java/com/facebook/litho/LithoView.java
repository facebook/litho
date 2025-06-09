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

package com.facebook.litho;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityManagerCompat;
import androidx.core.view.accessibility.AccessibilityManagerCompat.AccessibilityStateChangeListenerCompat;
import androidx.lifecycle.LifecycleOwner;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.rendercore.utils.CommonUtils;
import com.facebook.rendercore.utils.MeasureSpecUtils;
import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/** A {@link ViewGroup} that can host the mounted state of a {@link Component}. */
public class LithoView extends BaseMountingView {
  private static final String LITHO_VISIBILITY_EVENTS_CONTROLLER_FOUND =
      "lithoView:LithoVisibilityEventsControllerFound";

  private @Nullable ComponentTree mComponentTree;
  private final ComponentContext mComponentContext;
  private boolean mIsAttachedForTest;
  private boolean mForceLayout;
  private boolean mSuppressMeasureComponentTree;
  private boolean mIsMeasuring = false;
  private boolean mWasMeasured = false;
  private boolean mHasNewComponentTree = false;
  private @Nullable OnPostDrawListener mOnPostDrawListener = null;

  private final AccessibilityManager mAccessibilityManager;

  private final AccessibilityStateChangeListener mAccessibilityStateChangeListener =
      new AccessibilityStateChangeListener(this);

  private static final int[] sLayoutSize = new int[2];

  // Keep ComponentTree when detached from this view in case the ComponentTree is shared between
  // sticky header and RecyclerView's binder
  // TODO T14859077 Replace with proper solution
  private @Nullable ComponentTree mTemporaryDetachedComponentTree;
  private boolean mDoMeasureInLayout;
  private @Nullable MountStartupLoggingInfo mMountStartupLoggingInfo;

  /**
   * Create a new {@link LithoView} instance and initialize it with the given {@link Component}
   * root.
   *
   * @param context Android {@link Context}.
   * @param component The root component to draw.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   */
  public static LithoView create(Context context, @Nullable Component component) {
    final LithoView lithoView = new LithoView(context);
    lithoView.setComponentTree(
        ComponentTree.create(new ComponentContext(context), component).build());
    return lithoView;
  }

  public static LithoView create(
      Context context, @Nullable Component component, LithoVisibilityEventsController controller) {
    return create(new ComponentContext(context), component, controller);
  }

  /**
   * Create a new {@link LithoView} instance and initialize it with the given {@link Component}
   * root.
   *
   * @param context {@link ComponentContext}.
   * @param component The root component to draw.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   */
  public static LithoView create(ComponentContext context, Component component) {
    final LithoView lithoView = new LithoView(context);
    lithoView.setComponentTree(ComponentTree.create(context, component).build());
    return lithoView;
  }

  /**
   * Creates a new LithoView and sets a new ComponentTree on it. The ComponentTree is subscribed to
   * the given LithoVisibilityEventsController instance.
   */
  public static LithoView create(
      ComponentContext context, Component component, LithoVisibilityEventsController controller) {
    final LithoView lithoView = new LithoView(context);
    lithoView.setComponentTree(ComponentTree.create(context, component, controller).build());
    return lithoView;
  }

  /**
   * Create a new {@link LithoView} instance and initialize it with a custom {@link ComponentTree}.
   */
  public static LithoView create(Context context, ComponentTree componentTree) {
    return create(new ComponentContext(context), componentTree);
  }

  /**
   * Create a new {@link LithoView} instance and initialize it with a custom {@link ComponentTree}.
   */
  public static LithoView create(ComponentContext context, ComponentTree componentTree) {
    final LithoView lithoView = new LithoView(context);
    lithoView.setComponentTree(componentTree);
    return lithoView;
  }

  public LithoView(Context context) {
    this(context, null);
  }

  public LithoView(Context context, @Nullable AttributeSet attrs) {
    this(new ComponentContext(context), attrs);
  }

  public LithoView(ComponentContext context) {
    this(context, null);
  }

  public LithoView(ComponentContext context, @Nullable AttributeSet attrs) {
    super(context.getAndroidContext(), attrs);
    mComponentContext = context;

    mAccessibilityManager =
        (AccessibilityManager) context.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE);
  }

  protected void forceRelayout() {
    mForceLayout = true;
    requestLayout();
  }

  public void setTemporaryDetachedComponentTree(@Nullable ComponentTree componentTree) {
    mTemporaryDetachedComponentTree = componentTree;
  }

  public boolean hasTemporaryDetachedComponentTree() {
    return mTemporaryDetachedComponentTree != null;
  }

  /**
   * Along with {@link #onDetachedFromWindowForTest} below, makes the LithoView think it's attached/
   * detached in a unit test environment. This also handles setting the same state for all LithoView
   * children.
   *
   * <p>Implementation Note: Ideally, we'd just attach the LithoView to a View hierarchy and let
   * AOSP handle all this for us. The reason we haven't is because attaching to an Activity while
   * also trying to make sure the full LithoView is always considered visible (for the purposes of
   * visibility events and incremental mount) proved difficult - if interested, see summary on the
   * blame diff for this comment for more info.
   */
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void onAttachedToWindowForTest() {
    if (mIsAttachedForTest) {
      return;
    }

    onAttachedToWindow();
    mIsAttachedForTest = true;

    dispatchAttachedForTestToChildren();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void onDetachedFromWindowForTest() {
    if (!mIsAttachedForTest) {
      return;
    }

    mIsAttachedForTest = false;
    onDetachedFromWindow();

    dispatchAttachedForTestToChildren();
  }

  /**
   * If set to true, the onMeasure(..) call won't measure the ComponentTree with the given measure
   * specs, but it will just use them as measured dimensions.
   */
  public void suppressMeasureComponentTree(boolean suppress) {
    mSuppressMeasureComponentTree = suppress;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    try {
      if (isTracing) {
        ComponentsSystrace.beginSection("LithoView.onMeasure");
      }
      onMeasureInternal(widthMeasureSpec, heightMeasureSpec);
      mWasMeasured = true;
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  private void onMeasureInternal(int widthMeasureSpec, int heightMeasureSpec) {
    // mAnimatedWidth/mAnimatedHeight >= 0 if something is driving a width/height animation.
    final boolean animating = animatedWidth != SIZE_UNSET || animatedHeight != SIZE_UNSET;
    // up to date view sizes, taking into account running animations
    final int upToDateWidth = (animatedWidth != SIZE_UNSET) ? animatedWidth : getWidth();
    final int upToDateHeight = (animatedHeight != SIZE_UNSET) ? animatedHeight : getHeight();
    animatedWidth = SIZE_UNSET;
    animatedHeight = SIZE_UNSET;

    if (animating) {
      // If the mount state is dirty, we want to ignore the current animation and calculate the
      // new LayoutState as normal below. That LayoutState has the opportunity to define its own
      // transition to a new width/height from the current height of the LithoView, or if not we
      // will jump straight to that width/height.
      if (!isMountStateDirty()) {
        setMeasuredDimension(upToDateWidth, upToDateHeight);
        return;
      }
    }

    LayoutParams layoutParams = getLayoutParams();
    if (layoutParams instanceof LayoutManagerOverrideParams) {
      LayoutManagerOverrideParams layoutManagerOverrideParams =
          (LayoutManagerOverrideParams) layoutParams;
      final int overrideWidthSpec = layoutManagerOverrideParams.getWidthMeasureSpec();
      if (overrideWidthSpec != MeasureSpecUtils.unspecified()) {
        widthMeasureSpec = overrideWidthSpec;
      }
      final int overrideHeightSpec = layoutManagerOverrideParams.getHeightMeasureSpec();
      if (overrideHeightSpec != MeasureSpecUtils.unspecified()) {
        heightMeasureSpec = overrideHeightSpec;
      }
    }

    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (mTemporaryDetachedComponentTree != null && mComponentTree == null) {
      setComponentTree(mTemporaryDetachedComponentTree);
      mTemporaryDetachedComponentTree = null;
    }

    if (!mForceLayout
        && SizeSpec.getMode(widthMeasureSpec) == SizeSpec.EXACTLY
        && SizeSpec.getMode(heightMeasureSpec) == SizeSpec.EXACTLY) {
      // If the measurements are exact, postpone LayoutState calculation from measure to layout.
      // This is part of the fix for android's double measure bug. Doing this means that if we get
      // remeasured with different exact measurements, we don't compute two layouts.
      mDoMeasureInLayout = true;
      setMeasuredDimension(width, height);
      return;
    }

    mIsMeasuring = true;

    if (mComponentTree != null && !mSuppressMeasureComponentTree) {
      boolean forceRelayout = mForceLayout;
      mForceLayout = false;
      mComponentTree.measure(
          adjustMeasureSpecForPadding(widthMeasureSpec, getPaddingRight() + getPaddingLeft()),
          adjustMeasureSpecForPadding(heightMeasureSpec, getPaddingTop() + getPaddingBottom()),
          sLayoutSize,
          forceRelayout);

      width = sLayoutSize[0];
      height = sLayoutSize[1];
      mDoMeasureInLayout = false;
    }

    final boolean canAnimateRootBounds =
        !mSuppressMeasureComponentTree
            && mComponentTree != null
            && (!mHasNewComponentTree || !hasMountedAtLeastOnce());

    if (canAnimateRootBounds) {
      // We might need to collect transitions before mount to know whether this LithoView has
      // width or height animation.
      maybeCollectAllTransitions();

      final int initialAnimatedWidth =
          getInitialAnimatedMountingViewWidth(upToDateWidth, mHasNewComponentTree);
      if (initialAnimatedWidth != SIZE_UNSET) {
        width = initialAnimatedWidth;
      }

      final int initialAnimatedHeight =
          getInitialAnimatedMountingViewHeight(upToDateHeight, mHasNewComponentTree);
      if (initialAnimatedHeight != SIZE_UNSET) {
        height = initialAnimatedHeight;
      }
    }
    setMeasuredDimension(width, height);

    mHasNewComponentTree = false;
    mIsMeasuring = false;
  }

  boolean hasMeasuredAtLeastOnce() {
    return mWasMeasured || getMeasuredHeight() != 0 || getMeasuredWidth() != 0;
  }

  @Override
  protected void onBeforeLayout(int left, int top, int right, int bottom) {
    super.onBeforeLayout(left, top, right, bottom);
    if (mComponentTree == null || mComponentTree.isReleased()) {
      throw new IllegalStateException(
          "Trying to layout a LithoView holding onto a released ComponentTree");
    }

    if (mDoMeasureInLayout || mComponentTree.getMainThreadLayoutState() == null) {
      final int widthWithoutPadding =
          Math.max(0, right - left - getPaddingRight() - getPaddingLeft());
      final int heightWithoutPadding =
          Math.max(0, bottom - top - getPaddingTop() - getPaddingBottom());

      // Call measure so that we get a layout state that we can use for layout.
      mComponentTree.measure(
          MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(heightWithoutPadding, MeasureSpec.EXACTLY),
          sLayoutSize,
          false);
      mHasNewComponentTree = false;
      mDoMeasureInLayout = false;
    }
  }

  @Override
  protected void onLifecycleOwnerChanged(
      @Nullable LifecycleOwner previousLifecycleOwner,
      @Nullable LifecycleOwner currentLifecycleOwner) {
    if (ComponentsConfiguration.defaultInstance
        .enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner) {
      if (mComponentTree != null && currentLifecycleOwner != null) {
        mComponentTree.setLifecycleOwnerTreeProp(currentLifecycleOwner);
      }
    }
  }

  private static int adjustMeasureSpecForPadding(int measureSpec, int padding) {
    final int mode = MeasureSpec.getMode(measureSpec);
    if (mode == MeasureSpec.UNSPECIFIED) {
      return measureSpec;
    }
    final int size = Math.max(0, MeasureSpec.getSize(measureSpec) - padding);
    return MeasureSpec.makeMeasureSpec(size, mode);
  }

  /**
   * @return {@link ComponentContext} associated with this LithoView. It's a wrapper on the {@link
   *     Context} originally used to create this LithoView itself.
   */
  public ComponentContext getComponentContext() {
    return mComponentContext;
  }

  void assertNotInMeasure() {
    if (mIsMeasuring) {
      // If the ComponentTree is updated during measure, the following .layout() call will not run
      // on the ComponentTree that was prepared in measure.
      throw new RuntimeException("Cannot update ComponentTree while in the middle of measure");
    }
  }

  public @Nullable ComponentTree getComponentTree() {
    return mComponentTree;
  }

  @Nullable
  LayoutState getMountedLayoutState() {
    final @Nullable ComponentTree tree = mComponentTree;
    return tree != null ? tree.getMainThreadLayoutState() : null;
  }

  @VisibleForTesting
  public @Nullable Component getRootComponent() {
    final @Nullable ComponentTree componentTree = mComponentTree;
    return componentTree != null ? componentTree.getRoot() : null;
  }

  public void setOnPostDrawListener(@Nullable OnPostDrawListener onPostDrawListener) {
    mOnPostDrawListener = onPostDrawListener;
  }

  public void setComponentTree(@Nullable ComponentTree componentTree) {
    setComponentTree(componentTree, true);
  }

  public void setComponentTree(
      @Nullable ComponentTree componentTree, boolean unmountAllWhenComponentTreeSetToNull) {
    assertMainThread();
    assertNotInMeasure();

    // Cannot set a released ComponentTree
    if (componentTree != null && componentTree.isReleased()) {
      return;
    }

    mTemporaryDetachedComponentTree = null;
    if (mComponentTree == componentTree) {
      if (ComponentsConfiguration.bindOnSameComponentTree && isAttached()) {
        rebind();
      }
      return;
    }

    mHasNewComponentTree =
        mComponentTree == null || componentTree == null || mComponentTree.mId != componentTree.mId;
    setMountStateDirty();

    if (mComponentTree != null) {
      if (componentTree == null && unmountAllWhenComponentTreeSetToNull) {
        unmountAllItems();
      } else if (componentTree != null) {
        onBeforeSettingNewTree();
      }

      if (mComponentTree.isAttached()) {
        mComponentTree.detach();
      }

      mComponentTree.clearLithoView();
    }

    mComponentTree = componentTree;

    setupMountExtensions();

    if (mComponentTree != null) {
      if (mComponentTree.isReleased()) {
        throw new IllegalStateException(
            "Setting a released ComponentTree to a LithoView, "
                + "released component was: "
                + mComponentTree.getReleasedComponent());
      }
      mComponentTree.setLithoView(this);

      if (isAttached()) {
        if (ComponentsConfiguration.defaultInstance
            .enableSetLifecycleOwnerTreePropViaDefaultLifecycleOwner) {
          if (getLifecycleOwner() != null) {
            mComponentTree.setLifecycleOwnerTreeProp(getLifecycleOwner());
          }
        }
        mComponentTree.attach();
      } else {
        requestLayout();
      }
    }
  }

  /** Change the root component synchronously. */
  public void setComponent(Component component) {
    if (mComponentTree == null) {
      setComponentTree(ComponentTree.create(getComponentContext(), component).build());
    } else {
      mComponentTree.setRoot(component);
    }
  }

  /**
   * Change the root component measuring it on a background thread before updating the UI. If this
   * {@link LithoView} doesn't have a ComponentTree initialized, the root will be computed
   * synchronously.
   */
  public void setComponentAsync(@Nullable Component component) {
    if (mComponentTree == null) {
      setComponentTree(ComponentTree.create(getComponentContext(), component).build());
    } else {
      mComponentTree.setRootAsync(component);
    }
  }

  /**
   * @return true if this LithoView has a ComponentTree attached and a
   *     LithoVisibilityEventsController is set on it, false otherwise.
   */
  public synchronized boolean componentTreeHasVisibilityEventsController() {
    return mComponentTree != null && mComponentTree.isSubscribedToVisibilityEventsController();
  }

  /**
   * If this LithoView has a ComponentTree attached to it, set a LithoVisibilityEventsController if
   * it doesn't already have one.
   *
   * @return true if the LithoView's ComponentTree was subscribed as listener to the given
   *     LithoVisibilityEventsController, false otherwise.
   */
  public synchronized boolean subscribeComponentTreeToVisibilityEventsController(
      LithoVisibilityEventsController controller) {
    if (mComponentTree == null) {
      return false;
    }

    if (mComponentTree.isSubscribedToVisibilityEventsController()) {
      return false;
    }

    mComponentTree.subscribeToVisibilityEventsController(controller);
    return true;
  }

  @Override
  public void setVisibilityHint(boolean isVisible, boolean skipMountingIfNotVisible) {
    if (componentTreeHasVisibilityEventsController()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          LITHO_VISIBILITY_EVENTS_CONTROLLER_FOUND,
          "Setting visibility hint but a LithoVisibilityEventsController was found, ignoring.");

      return;
    }
    super.setVisibilityHint(isVisible, skipMountingIfNotVisible);
  }

  @Override
  public void setVisibilityHint(boolean isVisible) {
    if (componentTreeHasVisibilityEventsController()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          LITHO_VISIBILITY_EVENTS_CONTROLLER_FOUND,
          "Setting visibility hint but a LithoVisibilityEventsController was found, ignoring.");

      return;
    }
    super.setVisibilityHint(isVisible);
  }

  @Override
  protected void onAttached() {
    // Not calling super intentionally as in the LithoView case we want ComponentTree to control the
    // rebind logic.
    if (mComponentTree != null) {
      mComponentTree.attach();
    }

    refreshAccessibilityDelegatesIfNeeded(true);

    AccessibilityManagerCompat.addAccessibilityStateChangeListener(
        mAccessibilityManager, mAccessibilityStateChangeListener);
  }

  @Override
  protected void onDetached() {
    super.onDetached();
    if (mComponentTree != null) {
      mComponentTree.detach();
    }

    AccessibilityManagerCompat.removeAccessibilityStateChangeListener(
        mAccessibilityManager, mAccessibilityStateChangeListener);

    mSuppressMeasureComponentTree = false;
  }

  @Override
  public Object onBeforeMount() {
    super.onBeforeMount();
    final boolean loggedFirstMount =
        LithoView.MountStartupLoggingInfo.maybeLogFirstMountStart(mMountStartupLoggingInfo);
    final boolean loggedLastMount =
        LithoView.MountStartupLoggingInfo.maybeLogLastMountStart(mMountStartupLoggingInfo, this);
    return (loggedFirstMount ? 1 : 0) | (loggedLastMount ? 2 : 0);
  }

  @Override
  public void onAfterMount(@Nullable Object fromOnBeforeMount) {
    super.onAfterMount(fromOnBeforeMount);
    if (fromOnBeforeMount == null) {
      throw new IllegalStateException(
          "Should have received wether firs and last mount should be logged");
    }
    final int fromBefore = (Integer) fromOnBeforeMount;
    if (mIsAttachedForTest) {
      dispatchAttachedForTestToChildren();
    }

    if ((fromBefore & 1) != 0) {
      LithoView.MountStartupLoggingInfo.logFirstMountEnd(mMountStartupLoggingInfo);
    }
    if ((fromBefore & 2) != 0) {
      LithoView.MountStartupLoggingInfo.logLastMountEnd(mMountStartupLoggingInfo);
    }
  }

  @Override
  public void draw(Canvas canvas) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    try {
      if (isTracing) {
        ComponentsSystrace.beginSection("LithoView.draw");
      }
      drawInternal(canvas);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  private void drawInternal(Canvas canvas) {
    try {
      canvas.translate(getPaddingLeft(), getPaddingTop());
      super.draw(canvas);
    } catch (Throwable t) {
      throw new LithoMetadataExceptionWrapper(mComponentTree, t);
    }

    if (mOnPostDrawListener != null) {
      mOnPostDrawListener.onPostDraw();
    }
  }

  @Nullable
  @Override
  public LayoutState getCurrentLayoutState() {
    return mComponentTree == null ? null : mComponentTree.getMainThreadLayoutState();
  }

  @Nullable
  @Override
  protected TreeState getTreeState() {
    return mComponentTree == null ? null : mComponentTree.getTreeState();
  }

  @Override
  protected boolean getHasTree() {
    return mComponentTree != null;
  }

  @Override
  protected String getTreeName() {
    return mComponentTree != null ? mComponentTree.getSimpleName() : null;
  }

  @Nullable
  @Override
  public ComponentsConfiguration getConfiguration() {
    return mComponentTree != null ? mComponentTree.getLithoConfiguration().componentsConfig : null;
  }

  @Override
  public boolean isIncrementalMountEnabled() {
    return (mComponentTree != null && mComponentTree.isIncrementalMountEnabled());
  }

  @Override
  protected boolean isVisibilityProcessingEnabled() {
    return (mComponentTree != null && mComponentTree.isVisibilityProcessingEnabled());
  }

  /** Deprecated: Consider subscribing the LithoView to a VisibilityEventsController instead. */
  @Deprecated
  public void release() {
    assertMainThread();
    if (componentTreeHasVisibilityEventsController()) {
      ComponentsReporter.emitMessage(
          ComponentsReporter.LogLevel.WARNING,
          LITHO_VISIBILITY_EVENTS_CONTROLLER_FOUND,
          "Trying to release a LithoView but a LithoVisibilityEventsController was found,"
              + " ignoring.");

      return;
    }

    final List<BaseMountingView> childrenLithoViews =
        getChildMountingViewsFromCurrentlyMountedItems();
    if (childrenLithoViews != null) {
      for (BaseMountingView child : childrenLithoViews) {
        if (child instanceof LithoView) {
          ((LithoView) child).release();
        }
      }
    }

    if (mComponentTree != null) {
      mComponentTree.release();
      clearDebugOverlay(this);
      mComponentTree = null;
    }
    mWasMeasured = false;
  }

  @Nullable
  @VisibleForTesting
  public DynamicPropsManager getDynamicPropsManager() {
    final LithoHostListenerCoordinator lithoHostListenerCoordinator =
        getLithoHostListenerCoordinator();
    if (lithoHostListenerCoordinator != null) {
      return lithoHostListenerCoordinator.getDynamicPropsManager();
    } else {
      return null;
    }
  }

  public void setMountStartupLoggingInfo(
      LithoStartupLogger startupLogger,
      String startupLoggerAttribution,
      boolean[] firstMountCalled,
      boolean[] lastMountCalled,
      boolean isLastAdapterItem,
      boolean isOrientationVertical) {

    mMountStartupLoggingInfo =
        new MountStartupLoggingInfo(
            startupLogger,
            startupLoggerAttribution,
            firstMountCalled,
            lastMountCalled,
            isLastAdapterItem,
            isOrientationVertical);
  }

  public void resetMountStartupLoggingInfo() {
    mMountStartupLoggingInfo = null;
  }

  @DoNotStrip
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  Deque<TestItem> findTestItems(String testKey) {
    final LithoHostListenerCoordinator lithoHostListenerCoordinator =
        getLithoHostListenerCoordinator();
    if (lithoHostListenerCoordinator == null) {
      return new LinkedList<>();
    }

    if (lithoHostListenerCoordinator.getEndToEndTestingExtension() == null) {
      throw new IllegalStateException(
          "Trying to access TestItems while "
              + "ComponentsConfiguration.isEndToEndTestRun is false.");
    }

    return lithoHostListenerCoordinator.getEndToEndTestingExtension().findTestItems(testKey);
  }

  private void dispatchAttachedForTestToChildren() {
    recursivelyDispatchedAttachedForTest(this, mIsAttachedForTest);
  }

  private static void recursivelyDispatchedAttachedForTest(
      ViewGroup viewGroup, boolean isAttachedForTest) {
    for (int i = 0; i < viewGroup.getChildCount(); i++) {
      View child = viewGroup.getChildAt(i);
      if (child instanceof LithoView) {
        if (isAttachedForTest) {
          ((LithoView) child).onAttachedToWindowForTest();
        } else {
          ((LithoView) child).onDetachedFromWindowForTest();
        }
      } else if (child instanceof ViewGroup) {
        recursivelyDispatchedAttachedForTest((ViewGroup) child, isAttachedForTest);
      }
    }
  }

  private static class AccessibilityStateChangeListener
      extends AccessibilityStateChangeListenerCompat {

    private final WeakReference<LithoView> mLithoView;

    private AccessibilityStateChangeListener(LithoView lithoView) {
      mLithoView = new WeakReference<>(lithoView);
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
      AccessibilityUtils.invalidateCachedIsAccessibilityEnabled();
      final LithoView lithoView = mLithoView.get();
      if (lithoView == null) {
        return;
      }

      lithoView.rerenderForAccessibility(enabled);
    }
  }

  public void rerenderForAccessibility(boolean enabled) {
    refreshAccessibilityDelegatesIfNeeded(enabled);
    // must force (not just request)
    forceRelayout();
  }

  @Override
  public void setVisibilityHintNonRecursive(boolean isVisible) {
    super.setVisibilityHintNonRecursive(isVisible);
  }

  @Override
  public void resetVisibilityHint() {
    super.resetVisibilityHint();
  }

  @Override
  public void performLayout(boolean changed, int l, int t, int r, int b) {
    super.performLayout(changed, l, t, r, b);
  }

  @Override
  public String toString() {
    // dump this view and include litho internal UI data
    return super.toString() + LithoViewTestHelper.viewToString(this, true);
  }

  /**
   * For Litho we will use the root as hint for which is the hierarchy backed by the {@link
   * com.facebook.rendercore.MountState}.
   */
  @Nullable
  @Override
  public String getHostHierarchyMountStateIdentifier() {
    Component root = mComponentTree == null ? null : mComponentTree.getRoot();
    return root != null ? CommonUtils.getSectionNameForTracing(root.getClass()) : null;
  }

  static class MountStartupLoggingInfo {

    private final LithoStartupLogger startupLogger;
    private final String startupLoggerAttribution;
    private final boolean[] firstMountLogged;
    private final boolean[] lastMountLogged;
    private final boolean isLastAdapterItem;
    private final boolean isOrientationVertical;

    MountStartupLoggingInfo(
        LithoStartupLogger startupLogger,
        String startupLoggerAttribution,
        boolean[] firstMountLogged,
        boolean[] lastMountLogged,
        boolean isLastAdapterItem,
        boolean isOrientationVertical) {
      this.startupLogger = startupLogger;
      this.startupLoggerAttribution = startupLoggerAttribution;
      this.firstMountLogged = firstMountLogged;
      this.lastMountLogged = lastMountLogged;
      this.isLastAdapterItem = isLastAdapterItem;
      this.isOrientationVertical = isOrientationVertical;
    }

    static boolean maybeLogFirstMountStart(@Nullable MountStartupLoggingInfo loggingInfo) {
      if (loggingInfo != null
          && LithoStartupLogger.isEnabled(loggingInfo.startupLogger)
          && loggingInfo.firstMountLogged != null
          && !loggingInfo.firstMountLogged[0]) {
        loggingInfo.startupLogger.markPoint(
            LithoStartupLogger.FIRST_MOUNT,
            LithoStartupLogger.START,
            loggingInfo.startupLoggerAttribution);
        return true;
      }
      return false;
    }

    static boolean maybeLogLastMountStart(
        @Nullable MountStartupLoggingInfo loggingInfo, LithoView lithoView) {
      if (loggingInfo != null
          && LithoStartupLogger.isEnabled(loggingInfo.startupLogger)
          && loggingInfo.firstMountLogged != null
          && loggingInfo.firstMountLogged[0]
          && loggingInfo.lastMountLogged != null
          && !loggingInfo.lastMountLogged[0]) {

        final ViewGroup parent = (ViewGroup) lithoView.getParent();
        if (parent == null) {
          return false;
        }

        if (loggingInfo.isLastAdapterItem
            || (loggingInfo.isOrientationVertical
                ? lithoView.getBottom() >= parent.getHeight() - parent.getPaddingBottom()
                : lithoView.getRight() >= parent.getWidth() - parent.getPaddingRight())) {
          loggingInfo.startupLogger.markPoint(
              LithoStartupLogger.LAST_MOUNT,
              LithoStartupLogger.START,
              loggingInfo.startupLoggerAttribution);
          return true;
        }
      }
      return false;
    }

    static void logFirstMountEnd(MountStartupLoggingInfo loggingInfo) {
      loggingInfo.startupLogger.markPoint(
          LithoStartupLogger.FIRST_MOUNT,
          LithoStartupLogger.END,
          loggingInfo.startupLoggerAttribution);
      loggingInfo.firstMountLogged[0] = true;
    }

    static void logLastMountEnd(MountStartupLoggingInfo loggingInfo) {
      loggingInfo.startupLogger.markPoint(
          LithoStartupLogger.LAST_MOUNT,
          LithoStartupLogger.END,
          loggingInfo.startupLoggerAttribution);
      loggingInfo.lastMountLogged[0] = true;
    }
  }

  public interface OnPostDrawListener {

    void onPostDraw();
  }
}
