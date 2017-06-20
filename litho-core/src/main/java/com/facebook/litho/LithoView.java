/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.ref.WeakReference;
import java.util.Deque;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.support.v4.view.accessibility.AccessibilityManagerCompat.AccessibilityStateChangeListenerCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.proguard.annotations.DoNotStrip;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.facebook.litho.AccessibilityUtils.isAccessibilityEnabled;

/**
 * A {@link ViewGroup} that can host the mounted state of a {@link Component}.
 */
public class LithoView extends ComponentHost {
  private ComponentTree mComponentTree;
  private final MountState mMountState;
  private boolean mIsAttached;
  private final Rect mPreviousMountBounds = new Rect();
  private final boolean mIncrementalMountOnOffsetOrTranslationChange;

  private boolean mForceLayout;
  private boolean mSuppressMeasureComponentTree;

  private final AccessibilityManager mAccessibilityManager;

  private final AccessibilityStateChangeListener mAccessibilityStateChangeListener =
      new AccessibilityStateChangeListener(this);

  private static final int[] sLayoutSize = new int[2];

  // Keep ComponentTree when detached from this view in case the ComponentTree is shared between
  // sticky header and RecyclerView's binder
  // TODO T14859077 Replace with proper solution
  private ComponentTree mTemporaryDetachedComponent;
  private int mTransientStateCount;

  /**
   * Create a new {@link LithoView} instance and initialize it
   * with the given {@link Component} root.
   *
   * @param context Android {@link Context}.
   * @param component The root component to draw.
   * @return {@link LithoView} able to render a {@link Component} hierarchy.
   */
  public static LithoView create(Context context, Component component) {
    return create(new ComponentContext(context), component);
  }

  /**
   * Create a new {@link LithoView} instance and initialize it
   * with the given {@link Component} root.
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

  public LithoView(ComponentContext context, AttributeSet attrs) {
    this(context, attrs, false);
  }

  public LithoView(
      ComponentContext context,
      AttributeSet attrs,
      boolean incrementalMountOnOffsetOrTranslationChange) {
    super(context, attrs);

    mMountState = new MountState(this);
    mAccessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
    mIncrementalMountOnOffsetOrTranslationChange = incrementalMountOnOffsetOrTranslationChange;
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

  private void onAttach() {
    if (!mIsAttached) {
      mIsAttached = true;

      if (mComponentTree != null) {
        mComponentTree.attach();
      }

      refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(getContext()));

      AccessibilityManagerCompat.addAccessibilityStateChangeListener(
          mAccessibilityManager,
          mAccessibilityStateChangeListener);
    }
  }

  private void onDetach() {
    if (mIsAttached) {
      mIsAttached = false;

      if (mComponentTree != null) {
        mMountState.detach();

        mComponentTree.detach();
      }

      AccessibilityManagerCompat.removeAccessibilityStateChangeListener(
          mAccessibilityManager,
          mAccessibilityStateChangeListener);

      mSuppressMeasureComponentTree = false;
    }
  }

  /**
   * If set to true, the onMeasure(..) call won't measure the ComponentTree with the given
   * measure specs, but it will just use them as measured dimensions.
   */
  public void suppressMeasureComponentTree(boolean suppress) {
    mSuppressMeasureComponentTree = suppress;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (mTemporaryDetachedComponent != null && mComponentTree == null) {
      setComponentTree(mTemporaryDetachedComponent);
      mTemporaryDetachedComponent = null;
    }

    if (mComponentTree != null && !mSuppressMeasureComponentTree) {
      boolean forceRelayout = mForceLayout;
      mForceLayout = false;
      mComponentTree.measure(widthMeasureSpec, heightMeasureSpec, sLayoutSize, forceRelayout);

      width = sLayoutSize[0];
      height = sLayoutSize[1];
    }

    setMeasuredDimension(width, height);
  }

  @Override
  protected void performLayout(boolean changed, int left, int top, int right, int bottom) {

    if (mComponentTree != null) {
      boolean wasMountTriggered = mComponentTree.layout();

      final boolean isRectSame = mPreviousMountBounds != null
          && mPreviousMountBounds.left == left
          && mPreviousMountBounds.top == top
          && mPreviousMountBounds.right == right
          && mPreviousMountBounds.bottom == bottom;

      // If this happens the LithoView might have moved on Screen without a scroll event
      // triggering incremental mount. We trigger one here to be sure all the content is visible.
      if (!wasMountTriggered
          && !isRectSame
          && isIncrementalMountEnabled()) {
        performIncrementalMount();
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

  /**
   * Indicates if the children of this view should be laid regardless to a mount step being
   * triggered on layout. This step can be important when some of the children in the hierarchy
   * are changed (e.g. resized) but the parent wasn't.
   *
   * Since the framework doesn't expect its children to resize after being mounted, this should be
   * used only for extreme cases where the underline views are complex and need this behavior.
   *
   * @return boolean Returns true if the children of this view should be laid out even when a mount
   *    step was not needed.
   */
  protected boolean shouldAlwaysLayoutChildren() {
    return false;
  }

  /**
   * @return {@link ComponentContext} associated with this LithoView. It's a wrapper on the
   * {@link Context} originally used to create this LithoView itself.
   */
  public ComponentContext getComponentContext() {
    return (ComponentContext) getContext();
  }

  @Override
  protected boolean shouldRequestLayout() {
    // Don't bubble up layout requests while mounting.
    if (mComponentTree != null && mComponentTree.isMounting()) {
      return false;
    }

    return super.shouldRequestLayout();
  }

  public ComponentTree getComponentTree() {
    return mComponentTree;
  }

  public void setComponentTree(ComponentTree componentTree) {
    mTemporaryDetachedComponent = null;
    if (mComponentTree == componentTree) {
      if (mIsAttached) {
        rebind();
      }
      return;
    }

    setMountStateDirty();

    if (mComponentTree != null) {
      if (mIsAttached) {
        mComponentTree.detach();
      }

      mComponentTree.clearLithoView();
    }

    mComponentTree = componentTree;

    if (mComponentTree != null) {
      mComponentTree.setLithoView(this);

      if (mIsAttached) {
        mComponentTree.attach();
      } else {
        requestLayout();
      }
    }
  }

  /**
   * Change the root component synchronously.
   */
  public void setComponent(Component component) {
    if (mComponentTree == null) {
      setComponentTree(ComponentTree.create(getComponentContext(), component).build());
    } else {
      mComponentTree.setRoot(component);
    }
  }

  /**
   * Change the root component measuring it on a background thread before updating the UI.
   * If this {@link LithoView} doesn't have a ComponentTree initialized, the root will be
   * computed synchronously.
   */
  public void setComponentAsync(Component component) {
    if (mComponentTree == null) {
      setComponentTree(ComponentTree.create(getComponentContext(), component).build());
    } else {
      mComponentTree.setRootAsync(component);
    }
  }

  public void rebind() {
    mMountState.rebind();
  }

  /**
   * To be called this when the LithoView is about to become inactive. This means that either
   * the view is about to be recycled or moved off-screen.
   */
  public void unbind() {
    mMountState.unbind();
  }

  /**
   * Called from the ComponentTree when a new view want to use the same ComponentTree.
   */
  void clearComponentTree() {
    if (mIsAttached) {
      throw new IllegalStateException("Trying to clear the ComponentTree while attached.");
    }

    mComponentTree = null;
  }

  @Override
  public void setHasTransientState(boolean hasTransientState) {
    if (hasTransientState) {
      if (mTransientStateCount == 0 && isIncrementalMountEnabled()) {
        performIncrementalMount(null);
      }
      mTransientStateCount++;
    } else {
      mTransientStateCount--;
      if (mTransientStateCount < 0) {
        mTransientStateCount = 0;
      }
    }

    super.setHasTransientState(hasTransientState);
  }

  @Override
  public void offsetTopAndBottom(int offset) {
    super.offsetTopAndBottom(offset);

    maybePerformIncrementalMountOnView();
  }

  @Override
  public void offsetLeftAndRight(int offset) {
    super.offsetLeftAndRight(offset);

    maybePerformIncrementalMountOnView();
  }

  @Override
  public void setTranslationX(float translationX) {
    super.setTranslationX(translationX);

    maybePerformIncrementalMountOnView();
  }

  @Override
  public void setTranslationY(float translationY) {
    super.setTranslationY(translationY);

    maybePerformIncrementalMountOnView();
  }

  private void maybePerformIncrementalMountOnView() {
    if (!mIncrementalMountOnOffsetOrTranslationChange &&
        !ComponentsConfiguration.isIncrementalMountOnOffsetOrTranslationChangeEnabled) {
      return;
    }

    if (!isIncrementalMountEnabled() || !(getParent() instanceof View)) {
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

    if (left >= 0 &&
        top >= 0 &&
        right <= parentWidth &&
        bottom <= parentHeight &&
        mPreviousMountBounds.width() == getWidth() &&
        mPreviousMountBounds.height() == getHeight()) {
      // View is fully visible, and has already been completely mounted.
      return;
    }

    final Rect rect = ComponentsPools.acquireRect();
    rect.set(
        Math.max(0, -left),
        Math.max(0, -top),
        Math.min(right, parentWidth) - left,
        Math.min(bottom, parentHeight) - top);

    if (rect.isEmpty()) {
      // View is not visible at all, nothing to do.
      ComponentsPools.release(rect);
      return;
    }

    performIncrementalMount(rect);

    ComponentsPools.release(rect);
  }

  public void performIncrementalMount(Rect visibleRect) {
    if (mComponentTree == null) {
      return;
    }

    if (mComponentTree.isIncrementalMountEnabled()) {
      mComponentTree.mountComponent(visibleRect);
    } else {
      throw new IllegalStateException("To perform incremental mounting, you need first to enable" +
          " it when creating the ComponentTree.");
    }
  }

  public void performIncrementalMount() {
    if (mComponentTree == null) {
      return;
    }

    if (mComponentTree.isIncrementalMountEnabled()) {
      mComponentTree.incrementalMountComponent();
    } else {
      throw new IllegalStateException("To perform incremental mounting, you need first to enable" +
          " it when creating the ComponentTree.");
    }
  }

  public boolean isIncrementalMountEnabled() {
    return (mComponentTree != null && mComponentTree.isIncrementalMountEnabled());
  }

  public void release() {
    if (mComponentTree != null) {
      mComponentTree.release();
      mComponentTree = null;
    }
  }

  void mount(LayoutState layoutState, Rect currentVisibleArea) {
    if (mTransientStateCount > 0 && isIncrementalMountEnabled()) {
      // If transient state is set but the MountState is dirty we want to re-mount everything.
      // Otherwise, we don't need to do anything as the entire LithoView was mounted when the
      // transient state was set.
      if (!mMountState.isDirty()) {
        return;
      } else {
        currentVisibleArea = null;
      }
    }

    if (currentVisibleArea == null) {
      mPreviousMountBounds.setEmpty();
    } else {
      mPreviousMountBounds.set(currentVisibleArea);
    }

    mMountState.mount(layoutState, currentVisibleArea);
  }

  public Rect getPreviousMountBounds() {
    return mPreviousMountBounds;
  }

  void setMountStateDirty() {
    mMountState.setDirty();
    mPreviousMountBounds.setEmpty();
  }

  boolean isMountStateDirty() {
    return mMountState.isDirty();
  }

  MountState getMountState() {
    return mMountState;
  }

  @DoNotStrip
  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  Deque<TestItem> findTestItems(String testKey) {
    return mMountState.findTestItems(testKey);
  }

  private static class AccessibilityStateChangeListener extends
      AccessibilityStateChangeListenerCompat {
    private final WeakReference<LithoView> mLithoView;

    private AccessibilityStateChangeListener(LithoView lithoView) {
      mLithoView = new WeakReference<>(lithoView);
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
      final LithoView lithoView = mLithoView.get();
      if (lithoView == null) {
        return;
      }

      lithoView.refreshAccessibilityDelegatesIfNeeded(enabled);

      lithoView.requestLayout();
    }
  }
}
