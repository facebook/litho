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

package com.facebook.litho;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.facebook.litho.AccessibilityUtils.isAccessibilityEnabled;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityManagerCompat;
import androidx.core.view.accessibility.AccessibilityManagerCompat.AccessibilityStateChangeListenerCompat;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.rendercore.MountDelegate.MountDelegateTarget;
import com.facebook.rendercore.visibility.VisibilityOutput;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** A {@link ViewGroup} that can host the mounted state of a {@link Component}. */
public class LithoView extends Host {

  public static final String ZERO_HEIGHT_LOG = "LithoView:0-height";
  public static final String SET_ALREADY_ATTACHED_COMPONENT_TREE =
      "LithoView:SetAlreadyAttachedComponentTree";
  private static final int TOO_BIG_TEXTURE_SIZE = 4096;
  private static final String TAG = LithoView.class.getSimpleName();
  private final boolean mDisableTransitionsExtension;
  private boolean mIsMountStateDirty;
  private final boolean mUseExtensions;
  private final boolean mDelegateToRenderCore;
  private final @Nullable MountDelegateTarget mMountDelegateTarget;
  private @Nullable LithoRenderUnitFactory mLithoRenderUnitFactory;

  public interface OnDirtyMountListener {
    /**
     * Called when finishing a mount where the mount state was dirty. This indicates that there were
     * new props/state in the tree, or the LithoView was mounting a new ComponentTree
     */
    void onDirtyMount(LithoView view);
  }

  public interface OnPostDrawListener {
    void onPostDraw();
  }

  @Nullable private ComponentTree mComponentTree;
  private final @Nullable MountState mMountState;
  private final ComponentContext mComponentContext;
  private boolean mIsAttached;
  // The bounds of the visible rect that was used for the previous incremental mount.
  private final Rect mPreviousMountVisibleRectBounds = new Rect();

  private boolean mForceLayout;
  private boolean mSuppressMeasureComponentTree;
  private boolean mIsMeasuring = false;
  private boolean mHasNewComponentTree = false;
  private int mAnimatedWidth = -1;
  private int mAnimatedHeight = -1;
  private OnDirtyMountListener mOnDirtyMountListener = null;
  private final Rect mRect = new Rect();
  @Nullable private OnPostDrawListener mOnPostDrawListener = null;

  private final AccessibilityManager mAccessibilityManager;

  private final AccessibilityStateChangeListener mAccessibilityStateChangeListener =
      new AccessibilityStateChangeListener(this);

  private static final int[] sLayoutSize = new int[2];

  // Keep ComponentTree when detached from this view in case the ComponentTree is shared between
  // sticky header and RecyclerView's binder
  // TODO T14859077 Replace with proper solution
  private ComponentTree mTemporaryDetachedComponent;
  private int mTransientStateCount;
  private boolean mDoMeasureInLayout;
  @Nullable private Map<String, ComponentLogParams> mInvalidStateLogParams;
  @Nullable private String mPreviousComponentSimpleName;
  @Nullable private String mNullComponentCause;
  @Nullable private MountStartupLoggingInfo mMountStartupLoggingInfo;
  @Nullable private LithoHostListenerCoordinator mLithoHostListenerCoordinator;

  /**
   * Create a new {@link LithoView} instance and initialize it with the given {@link Component}
   * root.
   *
   * @param context Android {@link Context}.
   * @param component The root component to draw.
   * @param isReconciliationEnabled should enable reconciliation.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   * @deprecated Use {@link #create(Context, Component)} instead and set config explicitly on the
   *     {@link ComponentTree} using {@link ComponentTree.Builder#isReconciliationEnabled(boolean)}.
   */
  @Deprecated
  public static LithoView create(
      Context context, Component component, boolean isReconciliationEnabled) {
    return create(new ComponentContext(context), component, isReconciliationEnabled);
  }

  /**
   * Create a new {@link LithoView} instance and initialize it with the given {@link Component}
   * root.
   *
   * @param context Android {@link Context}.
   * @param component The root component to draw.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   */
  public static LithoView create(Context context, Component component) {
    return create(new ComponentContext(context), component);
  }
  /**
   * Create a new {@link LithoView} instance and initialize it with the given {@link Component}
   * root.
   *
   * @param context {@link ComponentContext}.
   * @param component The root component to draw.
   * @param isReconciliationEnabled should enable reconciliation.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   * @deprecated Use {@link #create(Context, Component)} instead and set config explicitly on the
   *     {@link ComponentTree} using {@link ComponentTree.Builder#isReconciliationEnabled(boolean)}.
   */
  @Deprecated
  public static LithoView create(
      ComponentContext context, Component component, boolean isReconciliationEnabled) {
    final LithoView lithoView = new LithoView(context);
    lithoView.setComponentTree(
        ComponentTree.create(context, component)
            .isReconciliationEnabled(isReconciliationEnabled)
            .build());

    return lithoView;
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

  public LithoView(Context context) {
    this(context, null);
  }

  public LithoView(Context context, AttributeSet attrs) {
    this(new ComponentContext(context), attrs);
  }

  public LithoView(ComponentContext context) {
    this(context, null);
  }

  public LithoView(ComponentContext context, boolean useExtensions, boolean delegateToRenderCore) {
    this(context, null, useExtensions, delegateToRenderCore);
  }

  public LithoView(ComponentContext context, AttributeSet attrs) {
    this(
        context,
        attrs,
        ComponentsConfiguration.useExtensionsWithMountDelegate,
        ComponentsConfiguration.delegateToRenderCoreMount);
  }

  public LithoView(
      ComponentContext context,
      AttributeSet attrs,
      final boolean useExtensions,
      final boolean delegateToRenderCore) {
    super(context, attrs);
    mComponentContext = context;

    mUseExtensions = useExtensions;
    mDelegateToRenderCore = delegateToRenderCore;

    if (mUseExtensions) {
      if (mDelegateToRenderCore) {
        mMountDelegateTarget = new com.facebook.rendercore.MountState(this);
      } else {
        mMountDelegateTarget = new MountState(this);
      }
      mMountState = null;
    } else {
      mMountDelegateTarget = null;
      mMountState = new MountState(this);
    }

    mAccessibilityManager =
        (AccessibilityManager) context.getAndroidContext().getSystemService(ACCESSIBILITY_SERVICE);
    mDisableTransitionsExtension =
        ComponentsConfiguration.disableTransitionsExtensionForMountDelegate;
  }

  private static void performLayoutOnChildrenIfNecessary(ComponentHost host) {
    for (int i = 0, count = host.getChildCount(); i < count; i++) {
      final View child = host.getChildAt(i);

      if (child.isLayoutRequested()) {
        // The hosting view doesn't allow children to change sizes dynamically as
        // this would conflict with the component's own layout calculations.
        child.measure(
            MeasureSpec.makeMeasureSpec(child.getWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(child.getHeight(), MeasureSpec.EXACTLY));
        child.layout(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
      }

      if (child instanceof ComponentHost) {
        performLayoutOnChildrenIfNecessary((ComponentHost) child);
      }
    }
  }

  protected void forceRelayout() {
    mForceLayout = true;
    requestLayout();
  }

  public void startTemporaryDetach() {
    mTemporaryDetachedComponent = mComponentTree;
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    onAttach();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    onDetach();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void onAttachedToWindowForTest() {
    onAttachedToWindow();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void onDetachedFromWindowForTest() {
    onDetachedFromWindow();
  }

  @Override
  public void onStartTemporaryDetach() {
    super.onStartTemporaryDetach();
    onDetach();
  }

  @Override
  public void onFinishTemporaryDetach() {
    super.onFinishTemporaryDetach();
    onAttach();
  }

  boolean usingExtensionsWithMountDelegate() {
    return mUseExtensions;
  }

  private void onAttach() {
    if (!mIsAttached) {
      mIsAttached = true;

      if (mComponentTree != null) {
        mComponentTree.attach();
      }

      refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(getContext()));

      AccessibilityManagerCompat.addAccessibilityStateChangeListener(
          mAccessibilityManager, mAccessibilityStateChangeListener);
    }
  }

  private void onDetach() {
    if (mIsAttached) {
      mIsAttached = false;

      if (mUseExtensions) {
        mMountDelegateTarget.detach();
        if (mLithoHostListenerCoordinator != null) {
          mLithoHostListenerCoordinator.onUnbind();
        }
      } else {
        mMountState.detach();
      }

      if (mComponentTree != null) {
        mComponentTree.detach();
      }

      AccessibilityManagerCompat.removeAccessibilityStateChangeListener(
          mAccessibilityManager, mAccessibilityStateChangeListener);

      mSuppressMeasureComponentTree = false;
    }
  }

  /**
   * If set to true, the onMeasure(..) call won't measure the ComponentTree with the given measure
   * specs, but it will just use them as measured dimensions.
   */
  public void suppressMeasureComponentTree(boolean suppress) {
    mSuppressMeasureComponentTree = suppress;
  }

  /**
   * Sets the width that the LithoView should take on the next measure pass and then requests a
   * layout. This should be called from animation-driving code on each frame to animate the size of
   * the LithoView.
   */
  public void setAnimatedWidth(int width) {
    mAnimatedWidth = width;
    requestLayout();
  }

  /**
   * Sets the height that the LithoView should take on the next measure pass and then requests a
   * layout. This should be called from animation-driving code on each frame to animate the size of
   * the LithoView.
   */
  public void setAnimatedHeight(int height) {
    mAnimatedHeight = height;
    requestLayout();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    widthMeasureSpec =
        DoubleMeasureFixUtil.correctWidthSpecForAndroidDoubleMeasureBug(
            getResources(), getContext().getPackageManager(), widthMeasureSpec);

    // mAnimatedWidth/mAnimatedHeight >= 0 if something is driving a width/height animation.
    final boolean animating = mAnimatedWidth != -1 || mAnimatedHeight != -1;
    // up to date view sizes, taking into account running animations
    final int upToDateWidth = (mAnimatedWidth != -1) ? mAnimatedWidth : getWidth();
    final int upToDateHeight = (mAnimatedHeight != -1) ? mAnimatedHeight : getHeight();
    mAnimatedWidth = -1;
    mAnimatedHeight = -1;

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
      if (overrideWidthSpec != LayoutManagerOverrideParams.UNINITIALIZED) {
        widthMeasureSpec = overrideWidthSpec;
      }
      final int overrideHeightSpec = layoutManagerOverrideParams.getHeightMeasureSpec();
      if (overrideHeightSpec != LayoutManagerOverrideParams.UNINITIALIZED) {
        heightMeasureSpec = overrideHeightSpec;
      }
    }

    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (mTemporaryDetachedComponent != null && mComponentTree == null) {
      setComponentTree(mTemporaryDetachedComponent);
      mTemporaryDetachedComponent = null;
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

    if (height == 0) {
      maybeLogInvalidZeroHeight();
    }

    final boolean canAnimateRootBounds =
        !mSuppressMeasureComponentTree
            && mComponentTree != null
            && (!mHasNewComponentTree || !mComponentTree.hasMounted());

    if (canAnimateRootBounds) {
      // We might need to collect transitions before mount to know whether this LithoView has
      // width or height animation.
      mComponentTree.maybeCollectTransitions();

      final int initialAnimatedWidth =
          mComponentTree.getInitialAnimatedLithoViewWidth(upToDateWidth, mHasNewComponentTree);
      if (initialAnimatedWidth != -1) {
        width = initialAnimatedWidth;
      }

      final int initialAnimatedHeight =
          mComponentTree.getInitialAnimatedLithoViewHeight(upToDateHeight, mHasNewComponentTree);
      if (initialAnimatedHeight != -1) {
        height = initialAnimatedHeight;
      }
    }
    setMeasuredDimension(width, height);

    mHasNewComponentTree = false;
    mIsMeasuring = false;
  }

  void maybeCollectAllTransitions(LayoutState layoutState, ComponentTree componentTree) {
    if (mUseExtensions) {
      if (mIsMountStateDirty) {
        // TODO: can this be a generic callback?
        mLithoHostListenerCoordinator.collectAllTransitions(layoutState, componentTree);
      }
    } else {
      if (mMountState.isDirty()) {
        mMountState.collectAllTransitions(layoutState, componentTree);
      }
    }
  }

  @Override
  protected void performLayout(boolean changed, int left, int top, int right, int bottom) {
    if (mComponentTree != null) {
      if (mComponentTree.isReleased()) {
        throw new IllegalStateException(
            "Trying to layout a LithoView holding onto a released ComponentTree");
      }

      if (bottom - top >= TOO_BIG_TEXTURE_SIZE || right - left >= TOO_BIG_TEXTURE_SIZE) {
        if (isDeviceThatCantHandleTooBigTextures()) {
          ComponentsReporter.emitMessage(
              ComponentsReporter.LogLevel.ERROR,
              "TextureTooBig",
              "LithoView has measured greater than "
                  + TOO_BIG_TEXTURE_SIZE
                  + " in one dimension. Size: "
                  + (right - left)
                  + "x"
                  + (bottom - top)
                  + ", component: "
                  + (mComponentTree.getRoot() != null
                      ? mComponentTree.getRoot().getSimpleName()
                      : null),
              100);
        }
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

      boolean wasMountTriggered = mComponentTree.layout();

      // If this happens the LithoView might have moved on Screen without a scroll event
      // triggering incremental mount. We trigger one here to be sure all the content is visible.
      if (!wasMountTriggered) {
        notifyVisibleBoundsChanged();
      }

      if (!wasMountTriggered || shouldAlwaysLayoutChildren()) {
        // If the layout() call on the component didn't trigger a mount step,
        // we might need to perform an inner layout traversal on children that
        // requested it as certain complex child views (e.g. ViewPager,
        // RecyclerView, etc) rely on that.
        performLayoutOnChildrenIfNecessary(this);
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
   * Indicates if the children of this view should be laid regardless to a mount step being
   * triggered on layout. This step can be important when some of the children in the hierarchy are
   * changed (e.g. resized) but the parent wasn't.
   *
   * <p>Since the framework doesn't expect its children to resize after being mounted, this should
   * be used only for extreme cases where the underline views are complex and need this behavior.
   *
   * @return boolean Returns true if the children of this view should be laid out even when a mount
   *     step was not needed.
   */
  protected boolean shouldAlwaysLayoutChildren() {
    return false;
  }

  /**
   * @return {@link ComponentContext} associated with this LithoView. It's a wrapper on the {@link
   *     Context} originally used to create this LithoView itself.
   */
  public ComponentContext getComponentContext() {
    return mComponentContext;
  }

  @Override
  protected boolean shouldRequestLayout() {
    // Don't bubble up layout requests while mounting.
    if (mComponentTree != null && mComponentTree.isMounting()) {
      return false;
    }

    return super.shouldRequestLayout();
  }

  void assertNotInMeasure() {
    if (mIsMeasuring) {
      // If the ComponentTree is updated during measure, the following .layout() call will not run
      // on the ComponentTree that was prepared in measure.
      throw new RuntimeException("Cannot update ComponentTree while in the middle of measure");
    }
  }

  @Nullable
  public ComponentTree getComponentTree() {
    return mComponentTree;
  }

  public void setOnDirtyMountListener(OnDirtyMountListener onDirtyMountListener) {
    mOnDirtyMountListener = onDirtyMountListener;
  }

  public void setOnPostDrawListener(@Nullable OnPostDrawListener onPostDrawListener) {
    mOnPostDrawListener = onPostDrawListener;
  }

  void onDirtyMountComplete() {
    if (mOnDirtyMountListener != null) {
      mOnDirtyMountListener.onDirtyMount(this);
    }
  }

  public void setComponentTree(@Nullable ComponentTree componentTree) {
    assertMainThread();
    assertNotInMeasure();

    mTemporaryDetachedComponent = null;
    if (mComponentTree == componentTree) {
      if (mIsAttached) {
        rebind();
      }
      return;
    }

    mHasNewComponentTree =
        mComponentTree == null || componentTree == null || mComponentTree.mId != componentTree.mId;
    setMountStateDirty();

    if (mComponentTree != null) {
      if (ComponentsConfiguration.unmountAllWhenComponentTreeSetToNull && componentTree == null) {
        unmountAllItems();
      } else {
        clearVisibilityItems();
      }

      if (mInvalidStateLogParams != null) {
        mPreviousComponentSimpleName = mComponentTree.getSimpleName();
      }
      if (componentTree != null
          && componentTree.getLithoView() != null
          && mInvalidStateLogParams != null
          && mInvalidStateLogParams.containsKey(SET_ALREADY_ATTACHED_COMPONENT_TREE)) {
        logSetAlreadyAttachedComponentTree(
            mComponentTree,
            componentTree,
            mInvalidStateLogParams.get(SET_ALREADY_ATTACHED_COMPONENT_TREE));
      }
      if (mIsAttached) {
        mComponentTree.detach();
      }

      mComponentTree.clearLithoView();
    }

    if (componentTree != null && !mUseExtensions) {
      mMountState.setRecyclingMode(componentTree.getRecyclingMode());
    }

    mComponentTree = componentTree;

    if (mHasNewComponentTree && mUseExtensions) {
      setupMountExtensions(mComponentTree);
    }

    if (mComponentTree != null) {
      if (mComponentTree.isReleased()) {
        throw new IllegalStateException(
            "Setting a released ComponentTree to a LithoView, "
                + "released component was: "
                + mComponentTree.getReleasedComponent());
      }
      mComponentTree.setLithoView(this);

      if (mIsAttached) {
        mComponentTree.attach();
      } else {
        requestLayout();
      }
    }
    mNullComponentCause = mComponentTree == null ? "set_CT" : null;
  }

  private void setupMountExtensions(ComponentTree componentTree) {
    if (!mUseExtensions) {
      throw new IllegalStateException("Using mount extensions is disabled on this LithoView.");
    }

    if (mLithoHostListenerCoordinator == null) {
      mLithoHostListenerCoordinator = new LithoHostListenerCoordinator();

      mLithoHostListenerCoordinator.enableVisibilityProcessing(this);

      if (mMountDelegateTarget == null) {
        throw new IllegalStateException(
            "Cannot enable transitions extension or incremental mount extension without a MountDelegateTarget.");
      }

      if (componentTree != null && componentTree.isIncrementalMountEnabled()) {
        mLithoHostListenerCoordinator.enableIncrementalMount(this, mMountDelegateTarget);
      }

      if (!mDisableTransitionsExtension) {
        mLithoHostListenerCoordinator.enableTransitions(this, mMountDelegateTarget);
      }

      if (ComponentsConfiguration.isEndToEndTestRun) {
        mLithoHostListenerCoordinator.enableEndToEndTestProcessing(mMountDelegateTarget);
      }

      mLithoHostListenerCoordinator.enableDynamicProps();
      mLithoRenderUnitFactory = mLithoHostListenerCoordinator.getLithoRenderUnitFactory();
    }
  }

  LithoRenderUnitFactory getLithoRenderUnitFactory() {
    return mLithoRenderUnitFactory;
  }

  @Override
  boolean isInTransientState() {
    return hasTransientState();
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
   * Change the root component synchronously. Creates a new component tree with reconciliation
   * disabled if required. <b>DO NOT USE</b> this method; it was added only to deprecate the current
   * usages of {@link #setComponent(Component)}.
   *
   * @deprecated Use {@link #getComponentTree()} and {@link ComponentTree#setRoot(Component)}
   *     instead; set the config explicitly on the {@link ComponentTree} using {@link
   *     ComponentTree.Builder#isReconciliationEnabled(boolean)}.
   */
  @Deprecated
  public void setComponentWithoutReconciliation(Component component) {
    if (mComponentTree == null) {
      setComponentTree(
          ComponentTree.create(getComponentContext(), component)
              .isReconciliationEnabled(false)
              .build());
    } else {
      mComponentTree.setRoot(component);
    }
  }

  /**
   * Change the root component measuring it on a background thread before updating the UI. If this
   * {@link LithoView} doesn't have a ComponentTree initialized, the root will be computed
   * synchronously.
   */
  public void setComponentAsync(Component component) {
    if (mComponentTree == null) {
      setComponentTree(ComponentTree.create(getComponentContext(), component).build());
    } else {
      mComponentTree.setRootAsync(component);
    }
  }

  /**
   * Change the root component measuring it on a background thread before updating the UI. If this
   * {@link LithoView} doesn't have a ComponentTree initialized, the root will be computed
   * synchronously with reconciliation disabled. <b>DO NOT USE</b> this method; it was added only to
   * deprecate the current usages of {@link #setComponentAsync(Component)}.
   *
   * @deprecated Use {@link #getComponentTree()} and {@link ComponentTree#setRootAsync(Component)}
   *     instead; set the config explicitly on the {@link ComponentTree} using {@link
   *     ComponentTree.Builder#isReconciliationEnabled(boolean)}.
   */
  @Deprecated
  public void setComponentAsyncWithoutReconciliation(Component component) {
    if (mComponentTree == null) {
      setComponentTree(
          ComponentTree.create(getComponentContext(), component)
              .isReconciliationEnabled(false)
              .build());
    } else {
      mComponentTree.setRootAsync(component);
    }
  }

  public void rebind() {
    if (mUseExtensions) {
      mMountDelegateTarget.attach();
    } else {
      mMountState.rebind();
    }
  }

  /**
   * To be called this when the LithoView is about to become inactive. This means that either the
   * view is about to be recycled or moved off-screen.
   */
  public void unbind() {
    if (mUseExtensions) {
      mMountDelegateTarget.detach();
      if (mLithoHostListenerCoordinator != null) {
        mLithoHostListenerCoordinator.onUnbind();
      }
    } else {
      mMountState.unbind();
    }
  }

  /**
   * Call this to tell the LithoView whether it is visible or not. In general, you shouldn't require
   * this as the system will do this for you. However, when a new activity/fragment is added on top
   * of the one hosting this view, the LithoView remains in the backstack but receives no callback
   * to indicate that it is no longer visible.
   *
   * @param isVisible if true, this will find the current visible rect and process visibility
   *     outputs using it. If false, any invisible and unfocused events will be called.
   */
  public void setVisibilityHint(boolean isVisible) {
    assertMainThread();

    if (mComponentTree == null) {
      return;
    }

    if (isVisible) {
      if (getLocalVisibleRect(mRect)) {
        processVisibilityOutputs(mRect);
        recursivelySetVisibleHint(true);
      }
      // if false: no-op, doesn't have visible area, is not ready or not attached
    } else {
      recursivelySetVisibleHint(false);
      clearVisibilityItems();
    }
  }

  private void clearVisibilityItems() {
    if (mUseExtensions && mLithoHostListenerCoordinator != null) {
      final VisibilityOutputsExtension visibilityOutputsExtension =
          mLithoHostListenerCoordinator.getVisibilityOutputsExtension();
      if (visibilityOutputsExtension != null) {
        visibilityOutputsExtension.clearVisibilityItems();
      }
    } else {
      mMountState.clearVisibilityItems();
    }
  }

  private void recursivelySetVisibleHint(boolean isVisible) {
    final List<LithoView> childLithoViews = getChildLithoViewsFromCurrentlyMountedItems();
    for (int i = childLithoViews.size() - 1; i >= 0; i--) {
      final LithoView lithoView = childLithoViews.get(i);
      lithoView.setVisibilityHint(isVisible);
    }
  }

  @Override
  public void setHasTransientState(boolean hasTransientState) {
    super.setHasTransientState(hasTransientState);

    if (hasTransientState) {
      if (mTransientStateCount == 0 && mComponentTree != null) {
        notifyVisibleBoundsChanged(new Rect(0, 0, getWidth(), getHeight()), false);
      }
      mTransientStateCount++;
    } else {
      mTransientStateCount--;
      if (mTransientStateCount == 0 && mComponentTree != null) {
        // We mounted everything when the transient state was set on this view. We need to do this
        // partly to unmount content that is not visible but mostly to get the correct visibility
        // events to be fired.
        notifyVisibleBoundsChanged();
      }
      if (mTransientStateCount < 0) {
        mTransientStateCount = 0;
      }
    }
  }

  @Override
  public void offsetTopAndBottom(int offset) {
    super.offsetTopAndBottom(offset);

    onOffsetOrTranslationChange();
  }

  @Override
  public void offsetLeftAndRight(int offset) {
    super.offsetLeftAndRight(offset);

    onOffsetOrTranslationChange();
  }

  @Override
  public void setTranslationX(float translationX) {
    if (translationX == getTranslationX()) {
      return;
    }
    super.setTranslationX(translationX);

    onOffsetOrTranslationChange();
  }

  @Override
  public void setTranslationY(float translationY) {
    if (translationY == getTranslationY()) {
      return;
    }
    super.setTranslationY(translationY);

    onOffsetOrTranslationChange();
  }

  @Override
  public void draw(Canvas canvas) {
    try {
      canvas.translate(getPaddingLeft(), getPaddingTop());
      super.draw(canvas);
    } catch (Throwable t) {
      if (mComponentTree != null && mComponentTree.getRoot() != null) {
        throw new ComponentsChainException(
            "Component root of the crashing hierarchy:", mComponentTree.getRoot(), t);
      }

      throw t;
    }

    if (mOnPostDrawListener != null) {
      mOnPostDrawListener.onPostDraw();
    }
  }

  private void onOffsetOrTranslationChange() {
    if (mComponentTree == null || !(getParent() instanceof View)) {
      return;
    }

    int parentWidth = ((View) getParent()).getWidth();
    int parentHeight = ((View) getParent()).getHeight();

    final int translationX = (int) getTranslationX();
    final int translationY = (int) getTranslationY();
    final int top = getTop() + translationY;
    final int bottom = getBottom() + translationY;
    final int left = getLeft() + translationX;
    final int right = getRight() + translationX;
    final Rect previousRect = mPreviousMountVisibleRectBounds;

    if (left >= 0
        && top >= 0
        && right <= parentWidth
        && bottom <= parentHeight
        && previousRect.left >= 0
        && previousRect.top >= 0
        && previousRect.right <= parentWidth
        && previousRect.bottom <= parentHeight
        && previousRect.width() == getWidth()
        && previousRect.height() == getHeight()) {
      // View is fully visible, and has already been completely mounted.
      return;
    }

    final Rect rect = new Rect();
    if (!getLocalVisibleRect(rect)) {
      // View is not visible at all, nothing to do.
      return;
    }

    notifyVisibleBoundsChanged(rect, true);
  }

  /**
   * Checks to make sure the main thread layout state is valid (and throws if it's both invalid and
   * there's no layout requested to make it valid).
   *
   * @return whether the main thread layout state is ok to use
   */
  private boolean checkMainThreadLayoutStateForIncrementalMount() {
    if (mComponentTree.getMainThreadLayoutState() != null) {
      return true;
    }

    if (mComponentTree.isIncrementalMountEnabled() && !isLayoutRequested()) {
      throw new RuntimeException(
          "Trying to incrementally mount a component with a null main thread LayoutState on a "
              + "LithoView that hasn't requested layout!");
    }

    return false;
  }

  public void notifyVisibleBoundsChanged(Rect visibleRect, boolean processVisibilityOutputs) {
    if (mComponentTree == null || !checkMainThreadLayoutStateForIncrementalMount()) {
      return;
    }

    if (mComponentTree.isIncrementalMountEnabled()) {
      mComponentTree.mountComponent(visibleRect, processVisibilityOutputs);
    } else if (processVisibilityOutputs) {
      processVisibilityOutputs(visibleRect);
    }
  }

  public void notifyVisibleBoundsChanged() {
    if (mComponentTree == null || mComponentTree.getMainThreadLayoutState() == null) {
      return;
    }

    if (mComponentTree.isIncrementalMountEnabled()) {
      mComponentTree.incrementalMountComponent();
    } else {
      processVisibilityOutputs();
    }
  }

  public boolean isIncrementalMountEnabled() {
    return (mComponentTree != null && mComponentTree.isIncrementalMountEnabled());
  }

  public void release() {
    assertMainThread();

    if (mComponentTree != null) {
      mComponentTree.release();
      mComponentTree = null;
      mNullComponentCause = "release_CT";
    }
  }

  void mount(
      LayoutState layoutState,
      @Nullable Rect currentVisibleArea,
      boolean processVisibilityOutputs) {

    if (mTransientStateCount > 0
        && mComponentTree != null
        && mComponentTree.isIncrementalMountEnabled()) {
      // If transient state is set but the MountState is dirty we want to re-mount everything.
      // Otherwise, we don't need to do anything as the entire LithoView was mounted when the
      // transient state was set.
      if (!isMountStateDirty()) {
        return;
      } else {
        currentVisibleArea = new Rect(0, 0, getWidth(), getHeight());
        processVisibilityOutputs = false;
      }
    }

    if (currentVisibleArea == null) {
      mPreviousMountVisibleRectBounds.setEmpty();
    } else {
      mPreviousMountVisibleRectBounds.set(currentVisibleArea);
    }

    final boolean loggedFirstMount =
        MountStartupLoggingInfo.maybeLogFirstMountStart(mMountStartupLoggingInfo);
    final boolean loggedLastMount =
        MountStartupLoggingInfo.maybeLogLastMountStart(mMountStartupLoggingInfo, this);

    if (mUseExtensions) {
      mountWithMountDelegateTarget(layoutState, currentVisibleArea);
    } else {
      mMountState.mount(layoutState, currentVisibleArea, processVisibilityOutputs);
    }

    mIsMountStateDirty = false;

    if (loggedFirstMount) {
      MountStartupLoggingInfo.logFirstMountEnd(mMountStartupLoggingInfo);
    }
    if (loggedLastMount) {
      MountStartupLoggingInfo.logLastMountEnd(mMountStartupLoggingInfo);
    }
  }

  private void mountWithMountDelegateTarget(
      LayoutState layoutState, @Nullable Rect currentVisibleArea) {
    final boolean needsMount = isMountStateDirty() || mountStateNeedsRemount();
    if (currentVisibleArea != null && !needsMount) {
      mLithoHostListenerCoordinator.onVisibleBoundsChanged(currentVisibleArea);
    } else {
      if (mLithoHostListenerCoordinator != null) {
        mLithoHostListenerCoordinator.beforeMount(layoutState, currentVisibleArea);
      }
      mMountDelegateTarget.mount(layoutState.toRenderTree());
      if (mLithoHostListenerCoordinator != null) {
        mLithoHostListenerCoordinator.afterMount();
      }
    }
  }

  /**
   * Dispatch a visibility events to all the components hosted in this LithoView.
   *
   * <p>Marked as @Deprecated to indicate this method is experimental and should not be widely used.
   *
   * <p>NOTE: Can only be used when Incremental Mount is disabled! Call this method when the
   * LithoView is considered eligible for the visibility event (i.e. only dispatch VisibleEvent when
   * the LithoView is visible in its container).
   *
   * @param visibilityEventType The class type of the visibility event to dispatch. Supported:
   *     VisibleEvent.class, InvisibleEvent.class, FocusedVisibleEvent.class,
   *     UnfocusedVisibleEvent.class, FullImpressionVisibleEvent.class.
   */
  @Deprecated
  public void dispatchVisibilityEvent(Class<?> visibilityEventType) {
    if (isIncrementalMountEnabled()) {
      throw new IllegalStateException(
          "dispatchVisibilityEvent - "
              + "Can't manually trigger visibility events when incremental mount is enabled");
    }

    LayoutState layoutState =
        mComponentTree == null ? null : mComponentTree.getMainThreadLayoutState();

    if (layoutState != null && visibilityEventType != null) {
      for (int i = 0; i < layoutState.getVisibilityOutputCount(); i++) {
        dispatchVisibilityEvent(layoutState.getVisibilityOutputAt(i), visibilityEventType);
      }

      List<LithoView> childViews = getChildLithoViewsFromCurrentlyMountedItems();
      for (LithoView lithoView : childViews) {
        lithoView.dispatchVisibilityEvent(visibilityEventType);
      }
    }
  }

  private List<LithoView> getChildLithoViewsFromCurrentlyMountedItems() {
    if (mUseExtensions) {
      return getChildLithoViewsFromCurrentlyMountedItems(mMountDelegateTarget);
    }

    return mMountState.getChildLithoViewsFromCurrentlyMountedItems();
  }

  private static List<LithoView> getChildLithoViewsFromCurrentlyMountedItems(
      MountDelegateTarget mountDelegateTarget) {
    final ArrayList<LithoView> childLithoViews = new ArrayList<>();

    for (int i = 0, size = mountDelegateTarget.getContentCount(); i < size; i++) {
      final Object content = mountDelegateTarget.getContentAt(i);
      if (content instanceof HasLithoViewChildren) {
        ((HasLithoViewChildren) content).obtainLithoViewChildren(childLithoViews);
      }
    }

    return childLithoViews;
  }

  private void dispatchVisibilityEvent(
      VisibilityOutput visibilityOutput, Class<?> visibilityEventType) {
    if (visibilityEventType == VisibleEvent.class) {
      if (visibilityOutput.getVisibleEventHandler() != null) {
        EventDispatcherUtils.dispatchOnVisible(visibilityOutput.getVisibleEventHandler());
      }
    } else if (visibilityEventType == InvisibleEvent.class) {
      if (visibilityOutput.getInvisibleEventHandler() != null) {
        EventDispatcherUtils.dispatchOnInvisible(visibilityOutput.getInvisibleEventHandler());
      }
    } else if (visibilityEventType == FocusedVisibleEvent.class) {
      if (visibilityOutput.getFocusedEventHandler() != null) {
        EventDispatcherUtils.dispatchOnFocused(visibilityOutput.getFocusedEventHandler());
      }
    } else if (visibilityEventType == UnfocusedVisibleEvent.class) {
      if (visibilityOutput.getUnfocusedEventHandler() != null) {
        EventDispatcherUtils.dispatchOnUnfocused(visibilityOutput.getUnfocusedEventHandler());
      }
    } else if (visibilityEventType == FullImpressionVisibleEvent.class) {
      if (visibilityOutput.getFullImpressionEventHandler() != null) {
        EventDispatcherUtils.dispatchOnFullImpression(
            visibilityOutput.getFullImpressionEventHandler());
      }
    }
  }

  // This only gets called if extensions are disabled.
  private void processVisibilityOutputs() {
    final Rect currentVisibleArea = new Rect();
    final boolean visible = getLocalVisibleRect(currentVisibleArea);
    if (!visible) {
      currentVisibleArea.setEmpty();
    }

    processVisibilityOutputs(currentVisibleArea);
  }

  @VisibleForTesting
  void processVisibilityOutputs(Rect currentVisibleArea) {
    if (mComponentTree == null || !mComponentTree.isVisibilityProcessingEnabled()) {
      return;
    }

    if (mLithoHostListenerCoordinator != null) {
      mLithoHostListenerCoordinator.onVisibleBoundsChanged(currentVisibleArea);
    } else {
      final LayoutState layoutState = mComponentTree.getMainThreadLayoutState();

      if (layoutState == null) {
        Log.w(TAG, "Main Thread Layout state is not found");
        return;
      }

      mMountState.processVisibilityOutputs(
          layoutState,
          currentVisibleArea,
          mPreviousMountVisibleRectBounds,
          isMountStateDirty(),
          null);
    }

    mPreviousMountVisibleRectBounds.set(currentVisibleArea);
  }

  public void unmountAllItems() {
    if (mUseExtensions) {
      mMountDelegateTarget.unmountAllItems();
      if (mLithoHostListenerCoordinator != null) {
        mLithoHostListenerCoordinator.onUnmount();
      }
    } else {
      mMountState.unmountAllItems();
    }

    mPreviousMountVisibleRectBounds.setEmpty();
  }

  public Rect getPreviousMountBounds() {
    return mPreviousMountVisibleRectBounds;
  }

  void setMountStateDirty() {
    if (mUseExtensions) {
      mIsMountStateDirty = true;
    } else {
      mMountState.setDirty();
    }

    mPreviousMountVisibleRectBounds.setEmpty();
  }

  boolean isMountStateDirty() {
    if (mUseExtensions) {
      return mIsMountStateDirty;
    }

    return mMountState.isDirty();
  }

  boolean mountStateNeedsRemount() {
    if (mUseExtensions) {
      return mMountDelegateTarget.needsRemount();
    }

    return mMountState.needsRemount();
  }

  MountState getMountState() {
    return mUseExtensions && !mDelegateToRenderCore
        ? (MountState) mMountDelegateTarget
        : mMountState;
  }

  // Used for Transitions - When using extensions, TransitionsExtension gets this information
  // from the LayoutState directly.
  void setIsFirstMountOfComponentTree() {
    if (!mUseExtensions) {
      mMountState.setIsFirstMountOfComponentTree();
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

  /** Register for particular invalid state logs. */
  public void setInvalidStateLogParamsList(@Nullable List<ComponentLogParams> logParamsList) {
    if (logParamsList == null) {
      mInvalidStateLogParams = null;
    } else {
      mInvalidStateLogParams = new HashMap<>();
      for (int i = 0, size = logParamsList.size(); i < size; i++) {
        final ComponentLogParams logParams = logParamsList.get(i);
        mInvalidStateLogParams.put(logParams.logType, logParams);
      }
    }
  }

  private void maybeLogInvalidZeroHeight() {
    if (mComponentTree != null
        && mComponentTree.getMainThreadLayoutState() != null
        && mComponentTree.getMainThreadLayoutState().mLayoutRoot == null) {
      // Valid case for 0-height, onCreateLayout of root component returned null.
      return;
    }

    final ComponentLogParams logParams =
        mInvalidStateLogParams == null ? null : mInvalidStateLogParams.get(ZERO_HEIGHT_LOG);
    if (logParams == null) {
      // surface didn't subscribe for this type of logging.
      return;
    }

    final LayoutParams layoutParams = getLayoutParams();
    final boolean isViewBeingRemovedInPreLayoutOfPredictiveAnim =
        layoutParams instanceof LayoutManagerOverrideParams
            && ((LayoutManagerOverrideParams) layoutParams).hasValidAdapterPosition();

    if (isViewBeingRemovedInPreLayoutOfPredictiveAnim) {
      return;
    }

    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(logParams.logProductId);
    messageBuilder.append("-");
    messageBuilder.append(ZERO_HEIGHT_LOG);
    messageBuilder.append(", current=");
    messageBuilder.append(
        (mComponentTree == null ? "null_" + mNullComponentCause : mComponentTree.getSimpleName()));
    messageBuilder.append(", previous=");
    messageBuilder.append(mPreviousComponentSimpleName);
    messageBuilder.append(", view=");
    messageBuilder.append(LithoViewTestHelper.toDebugString(this));
    logError(messageBuilder.toString(), ZERO_HEIGHT_LOG, logParams);
  }

  private void logSetAlreadyAttachedComponentTree(
      ComponentTree currentComponentTree,
      ComponentTree newComponentTree,
      ComponentLogParams logParams) {
    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(logParams.logProductId);
    messageBuilder.append("-");
    messageBuilder.append(SET_ALREADY_ATTACHED_COMPONENT_TREE);
    messageBuilder.append(", currentView=");
    messageBuilder.append(LithoViewTestHelper.toDebugString(currentComponentTree.getLithoView()));
    messageBuilder.append(", newComponent.LV=");
    messageBuilder.append(LithoViewTestHelper.toDebugString(newComponentTree.getLithoView()));
    messageBuilder.append(", currentComponent=");
    messageBuilder.append(currentComponentTree.getSimpleName());
    messageBuilder.append(", newComponent=");
    messageBuilder.append(newComponentTree.getSimpleName());
    logError(messageBuilder.toString(), SET_ALREADY_ATTACHED_COMPONENT_TREE, logParams);
  }

  private static void logError(String message, String categoryKey, ComponentLogParams logParams) {
    final ComponentsReporter.LogLevel logLevel =
        logParams.failHarder
            ? ComponentsReporter.LogLevel.FATAL
            : ComponentsReporter.LogLevel.ERROR;
    ComponentsReporter.emitMessage(logLevel, categoryKey, message, logParams.samplingFrequency);
  }

  @DoNotStrip
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  Deque<TestItem> findTestItems(String testKey) {
    if (mUseExtensions) {
      if (mLithoHostListenerCoordinator == null) {
        return new LinkedList<>();
      }

      if (mLithoHostListenerCoordinator.getEndToEndTestingExtension() == null) {
        throw new IllegalStateException(
            "Trying to access TestItems while "
                + "ComponentsConfiguration.isEndToEndTestRun is false.");
      }

      return mLithoHostListenerCoordinator.getEndToEndTestingExtension().findTestItems(testKey);
    } else {
      return mMountState.findTestItems(testKey);
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

  private static boolean isDeviceThatCantHandleTooBigTextures() {
    switch (Build.MODEL) {
      case "SM-J610F":
      case "SM-J415F":
      case "SM-J415FN":
      case "SM-J610G":
      case "SM-J610FN":
      case "SM-J415G":
        return true;
      default:
        return false;
    }
  }

  /**
   * LayoutParams that override the LayoutManager.
   *
   * <p>If you set LayoutParams on a LithoView that implements this interface, the view will
   * completely ignore the layout specs given to it by its LayoutManager and use these specs
   * instead. To use, set the LayoutParams height and width to {@link
   * ViewGroup.LayoutParams#WRAP_CONTENT} and then provide a width and height measure spec though
   * this interface.
   *
   * <p>This is helpful for implementing {@link View.MeasureSpec#AT_MOST} support since Android
   * LayoutManagers don't support an AT_MOST concept as part of {@link ViewGroup.LayoutParams}'s
   * special values.
   */
  public interface LayoutManagerOverrideParams {

    int UNINITIALIZED = -1;

    int getWidthMeasureSpec();

    int getHeightMeasureSpec();

    // TODO T30527513 Remove after fixing 0 height issues.
    boolean hasValidAdapterPosition();
  }

  @Override
  public String toString() {
    // dump this view and include litho internal UI data
    return super.toString() + LithoViewTestHelper.viewToString(this, true);
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
}
