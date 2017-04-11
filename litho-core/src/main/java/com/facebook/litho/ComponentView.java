/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

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

import com.facebook.proguard.annotations.DoNotStrip;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static com.facebook.litho.AccessibilityUtils.isAccessibilityEnabled;

/**
 * A {@link ViewGroup} that can host the mounted state of a {@link Component}.
 */
public class ComponentView extends ComponentHost {
  private ComponentTree mComponentTree;
  private final MountState mMountState;
  private boolean mIsAttached;
  private final Rect mPreviousMountBounds = new Rect();
  private final boolean mIncrementalMountOnOffsetOrTranslationChange;

  private boolean mForceLayout;

  private final AccessibilityManager mAccessibilityManager;

  private final AccessibilityStateChangeListenerCompat mAccessibilityStateChangeListener =
      new AccessibilityStateChangeListenerCompat() {
        @Override
        public void onAccessibilityStateChanged(boolean enabled) {
          refreshAccessibilityDelegatesIfNeeded(enabled);

          requestLayout();
        }
      };

  private static final int[] sLayoutSize = new int[2];

  // Keep ComponentTree when detached from this view in case the ComponentTree is shared between
  // sticky header and RecyclerView's binder
  // TODO T14859077 Replace with proper solution
  private ComponentTree mTemporaryDetachedComponent;

  /**
   * Create a new {@link ComponentView} instance and initialize it
   * with the given {@link Component} root.
   *
   * @param context Android {@link Context}.
   * @param component The root component to draw.
   * @return {@link ComponentView} able to render a {@link Component} hierarchy.
   */
  public static ComponentView create(Context context, Component component) {
    return create(new ComponentContext(context), component);
  }

  /**
   * Create a new {@link ComponentView} instance and initialize it
   * with the given {@link Component} root.
   *
   * @param context {@link ComponentContext}.
   * @param component The root component to draw.
   * @return {@link ComponentView} able to render a {@link Component} hierarchy.
   */
  public static ComponentView create(ComponentContext context, Component component) {
    final ComponentView componentView = new ComponentView(context);
    componentView.setComponentTree(ComponentTree.create(context, component).build());

    return componentView;
  }

  public ComponentView(Context context) {
    this(context, null);
  }

  public ComponentView(Context context, AttributeSet attrs) {
    this(new ComponentContext(context), attrs);
  }

  public ComponentView(ComponentContext context) {
    this(context, null);
  }

  public ComponentView(ComponentContext context, AttributeSet attrs) {
    this(context, attrs, false);
  }

  public ComponentView(
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

  void forceRelayout() {
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
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (mTemporaryDetachedComponent != null && mComponentTree == null) {
      setComponentTree(mTemporaryDetachedComponent);
      mTemporaryDetachedComponent = null;
    }

    if (mComponentTree != null) {
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

      // If this happens the ComponentView might have moved on Screen without a scroll event
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
   * CONSULT THE COMPONENTS TEAM BEFORE USING THIS.
   *
   * Indicates if the children of this view should be laid regardless to a mount step being
   * triggered on layout. This step can be important when some of the children in the hierarchy
   * are changed (e.g. resized) but the parent wasn't.
   *
   * Since the framework doesn't expect its children to resize after being mounted, this should be
   * used only for extreme cases where the underline views are complex and need this behavior.
   * The {@link com.facebook.feedplugins.video.RichVideoAttachmentView}, used in
   * OlderRichVideoAttachmentComponentSpec is
   * the only view that currently needs this. Once it fixed - this method should be removed.
   *
   * @return boolean Returns true if the children of this view should be laid out even when a mount
   *    step was not needed.
   */
  protected boolean shouldAlwaysLayoutChildren() {
    return false;
  }

  /**
   * @return {@link ComponentContext} associated with this ComponentView. It's a wrapper on the
   * {@link Context} originally used to create this ComponentView itself.
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

      mComponentTree.clearComponentView();
    }

    mComponentTree = componentTree;

    if (mComponentTree != null) {
      mComponentTree.setComponentView(this);

      if (mIsAttached) {
        mComponentTree.attach();
      }
    }
  }

  /**
   * Change the root component synchronously.
   */
  public void setComponent(Component component) {
    if (mComponentTree == null) {
      throw new IllegalStateException("No ComponentTree initialized. Use the static " +
          ComponentView.class.getSimpleName() + ".create(..) method to create an instance of a " +
          ComponentView.class.getSimpleName() + " or manually set a ComponentTree.");
    }

    mComponentTree.setRoot(component);
  }

  /**
   * Change the root component measuring it on a background thread before updating the UI.
   */
  public void setComponentAsync(Component component) {
    if (mComponentTree == null) {
      throw new IllegalStateException("No ComponentTree initialized. Use the static " +
          ComponentView.class.getSimpleName() + ".create(..) method to create an instance of a " +
          ComponentView.class.getSimpleName() + " or manually set a ComponentTree.");
    }

    mComponentTree.setRootAsync(component);
  }

  public void rebind() {
    mMountState.rebind();
  }

  /**
   * To be called this when the ComponentView is about to become inactive. This means that either
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
  public void offsetTopAndBottom(int offset) {
    super.offsetTopAndBottom(offset);

    if (mIncrementalMountOnOffsetOrTranslationChange) {
      maybePerformIncrementalMountOnView();
    }
  }

  @Override
  public void offsetLeftAndRight(int offset) {
    super.offsetLeftAndRight(offset);

    if (mIncrementalMountOnOffsetOrTranslationChange) {
      maybePerformIncrementalMountOnView();
    }
  }

  @Override
  public void setTranslationX(float translationX) {
    super.setTranslationX(translationX);

    if (mIncrementalMountOnOffsetOrTranslationChange) {
      maybePerformIncrementalMountOnView();
    }
  }

  @Override
  public void setTranslationY(float translationY) {
    super.setTranslationY(translationY);

    if (mIncrementalMountOnOffsetOrTranslationChange) {
      maybePerformIncrementalMountOnView();
    }
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
}
