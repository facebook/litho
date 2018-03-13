/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.facebook.litho.AccessibilityUtils.isAccessibilityEnabled;
import static com.facebook.litho.ThreadUtils.assertMainThread;

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
import java.lang.ref.WeakReference;
import java.util.Deque;
import javax.annotation.Nullable;

/**
 * A {@link ViewGroup} that can host the mounted state of a {@link Component}.
 */
public class LithoView extends ComponentHost {

  public interface OnDirtyMountListener {
    /**
     * Called when finishing a mount where the mount state was dirty. This indicates that there were
     * new props/state in the tree, or the LithoView was mounting a new ComponentTree
     */
    void onDirtyMount(LithoView view);
  }

  private ComponentTree mComponentTree;
  private final MountState mMountState;
  private boolean mIsAttached;
  private final Rect mPreviousMountBounds = new Rect();

  private boolean mForceLayout;
  private boolean mSuppressMeasureComponentTree;
  private boolean mIsMeasuring = false;
  private boolean mHasNewComponentTree = false;
  private int mAnimatedWidth = -1;
  private int mAnimatedHeight = -1;
  private OnDirtyMountListener mOnDirtyMountListener = null;

  private final AccessibilityManager mAccessibilityManager;

  private final AccessibilityStateChangeListener mAccessibilityStateChangeListener =
      new AccessibilityStateChangeListener(this);

  private static final int[] sLayoutSize = new int[2];

  // Keep ComponentTree when detached from this view in case the ComponentTree is shared between
  // sticky header and RecyclerView's binder
  // TODO T14859077 Replace with proper solution
  private ComponentTree mTemporaryDetachedComponent;
  private int mTransientStateCount;
  private boolean mDoesOwnIncrementalMount;

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
    super(context, attrs);

    mMountState = new MountState(this);
    mAccessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
  }

  private static void performLayoutOnChildrenIfNecessary(ComponentHost host) {
    for (int i = 0, count = host.getChildCount(); i < count; i++) {
      final View child = host.getChildAt(i);

      if (child.isLayoutRequested()) {
        // The hosting view doesn't allow children to change sizes dynamically as
        // this would conflict with the component's own layout calculations.
        child.measure(
            SizeSpec.makeSizeSpec(child.getWidth(), SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(child.getHeight(), SizeSpec.EXACTLY));
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
      mMountState.detach();

      if (mComponentTree != null) {
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
    // mAnimatedWidth/mAnimatedHeight >= 0 if something is driving a width/height animation.
    if (mAnimatedWidth != -1 || mAnimatedHeight != -1) {
      final int nextWidth = (mAnimatedWidth != -1) ? mAnimatedWidth : getWidth();
      final int nextHeight = (mAnimatedHeight != -1) ? mAnimatedHeight : getHeight();
      mAnimatedWidth = -1;
      mAnimatedHeight = -1;

      // If the mount state is dirty, we want to ignore the current animation and calculate the
      // new LayoutState as normal below. That LayoutState has the opportunity to define its own
      // transition to a new width/height from the current height of the LithoView, or if not we
      // will jump straight to that width/height.
      if (!isMountStateDirty()) {
        setMeasuredDimension(nextWidth, nextHeight);
        return;
      }
    }

    LayoutParams layoutParams = getLayoutParams();
    if (layoutParams instanceof LayoutManagerOverrideParams) {
      LayoutManagerOverrideParams layoutManagerOverrideParams =
          (LayoutManagerOverrideParams) layoutParams;
      widthMeasureSpec = layoutManagerOverrideParams.getWidthMeasureSpec();
      heightMeasureSpec = layoutManagerOverrideParams.getHeightMeasureSpec();
    }

    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (mTemporaryDetachedComponent != null && mComponentTree == null) {
      setComponentTree(mTemporaryDetachedComponent);
      mTemporaryDetachedComponent = null;
    }

    mIsMeasuring = true;

    if (mComponentTree != null && !mSuppressMeasureComponentTree) {
      boolean forceRelayout = mForceLayout;
      mForceLayout = false;
      mComponentTree.measure(widthMeasureSpec, heightMeasureSpec, sLayoutSize, forceRelayout);

      width = sLayoutSize[0];
      height = sLayoutSize[1];
    }

    // If we're mounting a new ComponentTree, it probably has a different width/height but we don't
    // want to animate it.
    if (!mHasNewComponentTree && mComponentTree != null) {
      final boolean isExpectingWidthAnimation =
          width != getWidth() && mComponentTree.hasLithoViewWidthAnimation();
      if (isExpectingWidthAnimation) {
        width = getWidth();
      }
      final boolean isExpectingHeightAnimation =
          height != getHeight() && mComponentTree.hasLithoViewHeightAnimation();
      if (isExpectingHeightAnimation) {
        height = getHeight();
      }
    }
    setMeasuredDimension(width, height);

    mHasNewComponentTree = false;
    mIsMeasuring = false;
  }

  @Override
  protected void performLayout(boolean changed, int left, int top, int right, int bottom) {

    if (mComponentTree != null) {
      if (mComponentTree.isReleased()) {
        throw new IllegalStateException(
            "Trying to layout a LithoView holding onto a released ComponentTree");
      }

      if (!ComponentsConfiguration.IS_INTERNAL_BUILD
          && mComponentTree.getMainThreadLayoutState() == null) {
        // Call measure so that we get a layout state that we can use for layout.
        mComponentTree.measure(
            SizeSpec.makeSizeSpec(right - left, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(bottom - top, SizeSpec.EXACTLY),
            new int[2],
            false);
      }

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

  void onDirtyMountComplete() {
    if (mOnDirtyMountListener != null) {
      mOnDirtyMountListener.onDirtyMount(this);
    }
  }

  public void setComponentTree(ComponentTree componentTree) {
    assertMainThread();
    assertNotInMeasure();

    mTemporaryDetachedComponent = null;
    if (mComponentTree == componentTree) {
      if (mIsAttached) {
        rebind();
      }
      return;
    }

    mHasNewComponentTree = true;
    setMountStateDirty();

    if (mComponentTree != null) {
      if (ComponentsConfiguration.unmountAllWhenComponentTreeSetToNull && componentTree == null) {
        unmountAllItems();
      }

      if (mIsAttached) {
        mComponentTree.detach();
      }

      mComponentTree.clearLithoView();
    }

    mComponentTree = componentTree;

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
    assertMainThread();

    if (mIsAttached) {
      throw new IllegalStateException("Trying to clear the ComponentTree while attached.");
    }

    mComponentTree = null;
  }

  @Override
  public void setHasTransientState(boolean hasTransientState) {
    if (hasTransientState) {
      if (mTransientStateCount == 0 && isIncrementalMountEnabled()) {
        final Rect rect = ComponentsPools.acquireRect();
        rect.set(0, 0, getWidth(), getHeight());
        performIncrementalMount(rect, false);
        ComponentsPools.release(rect);
      }
      mTransientStateCount++;
    } else {
      mTransientStateCount--;
      if (mTransientStateCount == 0 && isIncrementalMountEnabled()) {
        // We mounted everything when the transient state was set on this view. We need to do this
        // partly to unmount content that is not visible but mostly to get the correct visibility
        // events to be fired.
        performIncrementalMount();
      }
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
    final boolean isEmpty;
    if (ComponentsConfiguration.lithoViewIncrementalMountUsesLocalVisibleBounds) {
      isEmpty = !getLocalVisibleRect(rect);
    } else {
      rect.set(
          Math.max(0, -left),
          Math.max(0, -top),
          Math.min(right, parentWidth) - left,
          Math.min(bottom, parentHeight) - top);
      isEmpty = rect.isEmpty();
    }

    if (isEmpty) {
      // View is not visible at all, nothing to do.
      ComponentsPools.release(rect);
      return;
    }

    performIncrementalMount(rect, true);

    ComponentsPools.release(rect);
  }

  /**
   * Checks to make sure the main thread layout state is valid (and throws if it's both invalid and
   * there's no layout requested to make it valid).
   * @return whether the main thread layout state is ok to use
   */
  private boolean checkMainThreadLayoutStateForIncrementalMount() {
    if (mComponentTree.getMainThreadLayoutState() != null) {
      return true;
    }

    if (!isLayoutRequested()) {
      throw new RuntimeException(
          "Trying to incrementally mount a component with a null main thread LayoutState on a " +
              "LithoView that hasn't requested layout!");
    }

    return false;
  }

  public void performIncrementalMount(Rect visibleRect, boolean processVisibilityOutputs) {
    if (mComponentTree == null || !checkMainThreadLayoutStateForIncrementalMount()) {
      return;
    }

    if (mComponentTree.isIncrementalMountEnabled()) {
      mComponentTree.mountComponent(visibleRect, processVisibilityOutputs);
    } else {
      throw new IllegalStateException("To perform incremental mounting, you need first to enable" +
          " it when creating the ComponentTree.");
    }
  }

  public void performIncrementalMount() {
    if (mComponentTree == null || !checkMainThreadLayoutStateForIncrementalMount()) {
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
    assertMainThread();

    if (mComponentTree != null) {
      mComponentTree.release();
      mComponentTree = null;
    }
  }

  void mount(LayoutState layoutState, Rect currentVisibleArea, boolean processVisibilityOutputs) {
    boolean rectNeedsRelease = false;
    if (mTransientStateCount > 0 && isIncrementalMountEnabled()) {
      // If transient state is set but the MountState is dirty we want to re-mount everything.
      // Otherwise, we don't need to do anything as the entire LithoView was mounted when the
      // transient state was set.
      if (!mMountState.isDirty()) {
        return;
      } else {
        currentVisibleArea = ComponentsPools.acquireRect();
        currentVisibleArea.set(0, 0, getWidth(), getHeight());
        rectNeedsRelease = true;
        processVisibilityOutputs = false;
      }
    }

    if (currentVisibleArea == null) {
      mPreviousMountBounds.setEmpty();
    } else {
      mPreviousMountBounds.set(currentVisibleArea);
    }

    mMountState.mount(layoutState, currentVisibleArea, processVisibilityOutputs);

    if (rectNeedsRelease) {
      ComponentsPools.release(currentVisibleArea);
    }
  }

  public void unmountAllItems() {
    mMountState.unmountAllItems();
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

  boolean doesOwnIncrementalMount() {
    return mDoesOwnIncrementalMount;
  }

  @Deprecated
  /**
   * @deprecated This is being temporarily added while we experiment with other solutions for
   *     incremental mount (see {@link
   *     ComponentsConfiguration#incrementalMountUsesLocalVisibleBounds} and {@link
   *     ComponentsConfiguration#lithoViewIncrementalMountUsesLocalVisibleBounds}.
   */
  public void setDoesOwnIncrementalMount(boolean doesOwnIncrementalMount) {
    mDoesOwnIncrementalMount = doesOwnIncrementalMount;

    setDoesOwnIncrementalMountOnChildren(this, doesOwnIncrementalMount);
  }

  private void setDoesOwnIncrementalMountOnChildren(
      ViewGroup viewGroup, boolean doesOwnIncrementalMount) {
    for (int i = 0, size = viewGroup.getChildCount(); i < size; i++) {
      final View child = viewGroup.getChildAt(i);

      if (child instanceof LithoView) {
        ((LithoView) child).setDoesOwnIncrementalMount(doesOwnIncrementalMount);
      }

      if (child instanceof ViewGroup) {
        setDoesOwnIncrementalMountOnChildren((ViewGroup) child, doesOwnIncrementalMount);
      }
    }
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
    int getWidthMeasureSpec();

    int getHeightMeasureSpec();
  }

  @Override
  public String toString() {
    return LithoViewTestHelper.viewToString(this, true);
  }
}
