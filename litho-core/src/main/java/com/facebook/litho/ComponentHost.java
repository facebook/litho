/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.AccessibilityUtils.isAccessibilityEnabled;
import static com.facebook.litho.ComponentHostUtils.maybeInvalidateAccessibilityState;
import static com.facebook.litho.MountItem.isTouchableDisabled;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.proguard.annotations.DoNotStrip;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ViewGroup} that can host the mounted state of a {@link Component}. This is used
 * by {@link MountState} to wrap mounted drawables to handle click events and update drawable
 * states accordingly.
 */
@DoNotStrip
public class ComponentHost extends ViewGroup {

  private final SparseArrayCompat<MountItem> mMountItems = new SparseArrayCompat<>();
  private SparseArrayCompat<MountItem> mScrapMountItemsArray;

  private final SparseArrayCompat<MountItem> mViewMountItems = new SparseArrayCompat<>();
  private SparseArrayCompat<MountItem> mScrapViewMountItemsArray;

  private final SparseArrayCompat<MountItem> mDrawableMountItems = new SparseArrayCompat<>();
  private SparseArrayCompat<MountItem> mScrapDrawableMountItems;

  private final ArrayList<MountItem> mDisappearingItems = new ArrayList<>();

  private CharSequence mContentDescription;
  private Object mViewTag;
  private SparseArray<Object> mViewTags;

  private boolean mWasInvalidatedWhileSuppressed;
  private boolean mWasInvalidatedForAccessibilityWhileSuppressed;
  private boolean mSuppressInvalidations;

  private final InterleavedDispatchDraw mDispatchDraw = new InterleavedDispatchDraw();

  private final @Nullable List<ComponentHost> mScrapHosts;

  private int[] mChildDrawingOrder = new int[0];
  private boolean mIsChildDrawingOrderDirty;

  private long mParentHostMarker;
  private boolean mInLayout;

  @Nullable private ComponentAccessibilityDelegate mComponentAccessibilityDelegate;
  private boolean mIsComponentAccessibilityDelegateSet = false;

  private ComponentClickListener mOnClickListener;
  private ComponentLongClickListener mOnLongClickListener;
  private ComponentFocusChangeListener mOnFocusChangeListener;
  private ComponentTouchListener mOnTouchListener;
  private EventHandler<InterceptTouchEvent> mOnInterceptTouchEventHandler;

  private TouchExpansionDelegate mTouchExpansionDelegate;

  public ComponentHost(Context context) {
    this(context, null);
  }

  public ComponentHost(Context context, AttributeSet attrs) {
    this(new ComponentContext(context), attrs);
  }

  public ComponentHost(ComponentContext context) {
    this(context, null);
  }

  public ComponentHost(ComponentContext context, AttributeSet attrs) {
    super(context, attrs);
    setWillNotDraw(false);
    setChildrenDrawingOrderEnabled(true);

    if (!ComponentsConfiguration.lazyInitializeComponentAccessibilityDelegate) {
      mComponentAccessibilityDelegate = new ComponentAccessibilityDelegate(this);
    }
    refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(context));
    mScrapHosts =
        ComponentsConfiguration.scrapHostRecyclingForComponentHosts
            ? new ArrayList<ComponentHost>(3)
            : null;
  }

  /**
   * Sets the parent host marker for this host.
   * @param parentHostMarker marker that indicates which {@link ComponentHost} hosts this host.
   */
  void setParentHostMarker(long parentHostMarker) {
    mParentHostMarker = parentHostMarker;
  }

  /**
   * @return an id indicating which {@link ComponentHost} hosts this host.
   */
  long getParentHostMarker() {
    return mParentHostMarker;
  }

  /**
   * Mounts the given {@link MountItem} with unique index.
   * @param index index of the {@link MountItem}. Guaranteed to be the same index as is passed for
   * the corresponding {@code unmount(index, mountItem)} call.
   * @param mountItem item to be mounted into the host.
   * @param bounds the bounds of the item that is to be mounted into the host
   */
  public void mount(int index, MountItem mountItem, Rect bounds) {
    final Object content = mountItem.getContent();
    if (content instanceof Drawable) {
      mountDrawable(index, mountItem, bounds);
    } else if (content instanceof View) {
      mViewMountItems.put(index, mountItem);
      mountView((View) content, mountItem.getFlags());
      maybeRegisterTouchExpansion(index, mountItem);
    }

    mMountItems.put(index, mountItem);

    maybeInvalidateAccessibilityState(mountItem);
  }

  void unmount(MountItem item) {
    final int index = mMountItems.keyAt(mMountItems.indexOfValue(item));
    unmount(index, item);
  }

  /**
   * Unmounts the given {@link MountItem} with unique index.
   * @param index index of the {@link MountItem}. Guaranteed to be the same index as was passed for
   * the corresponding {@code mount(index, mountItem)} call.
   * @param mountItem item to be unmounted from the host.
   */
  public void unmount(int index, MountItem mountItem) {
    final Object content = mountItem.getContent();
    if (content instanceof Drawable) {
      unmountDrawable(index, mountItem);
    } else if (content instanceof View) {
      unmountView((View) content);
      ComponentHostUtils.removeItem(index, mViewMountItems, mScrapViewMountItemsArray);
      mIsChildDrawingOrderDirty = true;
      maybeUnregisterTouchExpansion(index, mountItem);
    }

    ComponentHostUtils.removeItem(index, mMountItems, mScrapMountItemsArray);
    releaseScrapDataStructuresIfNeeded();
    maybeInvalidateAccessibilityState(mountItem);
  }

  void startUnmountDisappearingItem(int index, MountItem mountItem) {
    final Object content = mountItem.getContent();
    if (!(content instanceof View)) {
      throw new RuntimeException("Cannot unmount non-view item");
    }
    mIsChildDrawingOrderDirty = true;

    maybeUnregisterTouchExpansion(index, mountItem);
    ComponentHostUtils.removeItem(index, mViewMountItems, mScrapViewMountItemsArray);
    ComponentHostUtils.removeItem(index, mMountItems, mScrapMountItemsArray);
    releaseScrapDataStructuresIfNeeded();
    mDisappearingItems.add(mountItem);
  }

  void unmountDisappearingItem(MountItem disappearingItem) {
    if (!mDisappearingItems.remove(disappearingItem)) {
      final String key = (disappearingItem.getViewNodeInfo() != null) ?
          disappearingItem.getViewNodeInfo().getTransitionKey() :
          null;
      throw new RuntimeException(
          "Tried to remove non-existent disappearing item, transitionKey: " + key);
    }

    final View content = (View) disappearingItem.getContent();

    unmountView(content);
    maybeInvalidateAccessibilityState(disappearingItem);
  }

  boolean hasDisappearingItems() {
    return mDisappearingItems.size() > 0;
  }

  List<String> getDisappearingItemKeys() {
    if (!hasDisappearingItems()) {
      return null;
    }
    final List<String> keys = new ArrayList<>();
    for (int i = 0, size = mDisappearingItems.size(); i < size; i++) {
      keys.add(mDisappearingItems.get(i).getViewNodeInfo().getTransitionKey());
    }

    return keys;
  }

  private void maybeMoveTouchExpansionIndexes(MountItem item, int oldIndex, int newIndex) {
    final ViewNodeInfo viewNodeInfo = item.getViewNodeInfo();
    if (viewNodeInfo == null) {
      return;
    }

    final Rect expandedTouchBounds = viewNodeInfo.getExpandedTouchBounds();
    if (expandedTouchBounds == null || mTouchExpansionDelegate == null) {
      return;
    }

    mTouchExpansionDelegate.moveTouchExpansionIndexes(
        oldIndex,
        newIndex);
  }

  void maybeRegisterTouchExpansion(int index, MountItem mountItem) {
    final ViewNodeInfo viewNodeInfo = mountItem.getViewNodeInfo();
    if (viewNodeInfo == null) {
      return;
    }

    final Rect expandedTouchBounds = viewNodeInfo.getExpandedTouchBounds();
    if (expandedTouchBounds == null) {
      return;
    }

    if (mTouchExpansionDelegate == null) {
      mTouchExpansionDelegate = new TouchExpansionDelegate(this);
      setTouchDelegate(mTouchExpansionDelegate);
    }

    mTouchExpansionDelegate.registerTouchExpansion(
        index,
        (View) mountItem.getContent(),
        expandedTouchBounds);
  }

  void maybeUnregisterTouchExpansion(int index, MountItem mountItem) {
    final ViewNodeInfo viewNodeInfo = mountItem.getViewNodeInfo();
    if (viewNodeInfo == null) {
      return;
    }

    if (mTouchExpansionDelegate == null || viewNodeInfo.getExpandedTouchBounds() == null) {
      return;
    }

    mTouchExpansionDelegate.unregisterTouchExpansion(index);
  }

  /**
   * Tries to recycle a scrap host attached to this host.
   * @return The host view to be recycled.
   */
  ComponentHost recycleHost() {
    if (mScrapHosts == null) {
      return null;
    }

    if (mScrapHosts.size() > 0) {
      final ComponentHost host = mScrapHosts.remove(0);

      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        // We are bringing the re-used host to the front because before API 17, Android doesn't
        // take into account the children drawing order when dispatching ViewGroup touch events,
        // but it just traverses its children list backwards.
        bringChildToFront(host);
      }

      // The recycled host is immediately re-mounted in mountView(), therefore setting
      // the flag here is redundant, but future proof.
      mIsChildDrawingOrderDirty = true;

      return host;
    }

    return null;
  }

  /**
   * @return number of {@link MountItem}s that are currently mounted in the host.
   */
  int getMountItemCount() {
    return mMountItems.size();
  }

  /**
   * @return the {@link MountItem} that was mounted with the given index.
   */
  MountItem getMountItemAt(int index) {
    return mMountItems.valueAt(index);
  }

  /**
   * Hosts are guaranteed to have only one accessible component
   * in them due to the way the view hierarchy is constructed in {@link LayoutState}.
   * There might be other non-accessible components in the same hosts such as
   * a background/foreground component though. This is why this method iterates over
   * all mount items in order to find the accessible one.
   */
  MountItem getAccessibleMountItem() {
    for (int i = 0; i < getMountItemCount(); i++) {
      MountItem item = getMountItemAt(i);
      if (item.isAccessible()) {
        return item;
      }
    }

    return null;
  }

  /**
   * @return list of drawables that are mounted on this host.
   */
  public List<Drawable> getDrawables() {
    final List<Drawable> drawables = new ArrayList<>(mDrawableMountItems.size());
    for (int i = 0, size = mDrawableMountItems.size(); i < size; i++) {
      Drawable drawable = (Drawable) mDrawableMountItems.valueAt(i).getContent();
      drawables.add(drawable);
    }

    return drawables;
  }

  /**
   * @return the text content that is mounted on this host.
   */
  @DoNotStrip
  public TextContent getTextContent() {
    return ComponentHostUtils.extractTextContent(
        ComponentHostUtils.extractContent(mMountItems));
  }

  /**
   * @return the image content that is mounted on this host.
   */
  public ImageContent getImageContent() {
    return ComponentHostUtils.extractImageContent(
        ComponentHostUtils.extractContent(mMountItems));
  }

  /**
   * @return the content descriptons that are set on content mounted on this host
   */
  @Override
  public CharSequence getContentDescription() {
    return mContentDescription;
  }

  /**
   * Host views implement their own content description handling instead of
   * just delegating to the underlying view framework for performance reasons as
   * the framework sets/resets content description very frequently on host views
   * and the underlying accessibility notifications might cause performance issues.
   * This is safe to do because the framework owns the accessibility state and
   * knows how to update it efficiently.
   */
  @Override
  public void setContentDescription(CharSequence contentDescription) {
    mContentDescription = contentDescription;
    invalidateAccessibilityState();
  }

  @Override
  public void setImportantForAccessibility(int mode) {
    if (mode != ViewCompat.getImportantForAccessibility(this)) {
      super.setImportantForAccessibility(mode);
    }
  }

  @Override
  public void setTag(int key, Object tag) {
    super.setTag(key, tag);
    if (key == R.id.component_node_info && tag != null) {
      refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(getContext()));

      if (mComponentAccessibilityDelegate != null) {
        mComponentAccessibilityDelegate.setNodeInfo((NodeInfo) tag);
      }
    }
  }

  /**
   * Moves the MountItem associated to oldIndex in the newIndex position. This happens when a
   * LithoView needs to re-arrange the internal order of its items. If an item is already
   * present in newIndex the item is guaranteed to be either unmounted or moved to a different index
   * by subsequent calls to either {@link ComponentHost#unmount(int, MountItem)} or
   * {@link ComponentHost#moveItem(MountItem, int, int)}.
   *
   * @param item The item that has been moved.
   * @param oldIndex The current index of the MountItem.
   * @param newIndex The new index of the MountItem.
   */
  void moveItem(MountItem item, int oldIndex, int newIndex) {
    if (item == null && mScrapMountItemsArray != null) {
      item = mScrapMountItemsArray.get(oldIndex);
    }

    if (item == null) {
      return;
    }
    maybeMoveTouchExpansionIndexes(item, oldIndex, newIndex);

    final Object content = item.getContent();
    if (content instanceof Drawable) {
      moveDrawableItem(item, oldIndex, newIndex);
    } else if (content instanceof View) {
      mIsChildDrawingOrderDirty = true;

      startTemporaryDetach(((View) content));

      if (mViewMountItems.get(newIndex) != null) {
        ensureScrapViewMountItemsArray();

        ComponentHostUtils.scrapItemAt(newIndex, mViewMountItems, mScrapViewMountItemsArray);
      }

      ComponentHostUtils.moveItem(oldIndex, newIndex, mViewMountItems, mScrapViewMountItemsArray);
    }

    if (mMountItems.get(newIndex) != null) {
      ensureScrapMountItemsArray();

      ComponentHostUtils.scrapItemAt(newIndex, mMountItems, mScrapMountItemsArray);
    }

    ComponentHostUtils.moveItem(oldIndex, newIndex, mMountItems, mScrapMountItemsArray);

    releaseScrapDataStructuresIfNeeded();

    if (content instanceof View) {
      finishTemporaryDetach(((View) content));
    }
  }

  /**
   * Sets view tag on this host.
   * @param viewTag the object to set as tag.
   */
  public void setViewTag(Object viewTag) {
    mViewTag = viewTag;
  }

  /**
   * Sets view tags on this host.
   * @param viewTags the map containing the tags by id.
   */
  public void setViewTags(SparseArray<Object> viewTags) {
    mViewTags = viewTags;
  }

  /**
   * Sets a click listener on this host.
   * @param listener The listener to set on this host.
   */
  void setComponentClickListener(ComponentClickListener listener) {
    mOnClickListener = listener;
    this.setOnClickListener(listener);
  }

  /**
   * @return The previously set click listener
   */
  ComponentClickListener getComponentClickListener() {
    return mOnClickListener;
  }

  /**
   * Sets a long click listener on this host.
   * @param listener The listener to set on this host.
   */
  void setComponentLongClickListener(ComponentLongClickListener listener) {
    mOnLongClickListener = listener;
    this.setOnLongClickListener(listener);
  }

  /**
   * @return The previously set long click listener
   */
  ComponentLongClickListener getComponentLongClickListener() {
    return mOnLongClickListener;
  }

  /**
   * Sets a focus change listener on this host.
   * @param listener The listener to set on this host.
   */
  void setComponentFocusChangeListener(ComponentFocusChangeListener listener) {
    mOnFocusChangeListener = listener;
    this.setOnFocusChangeListener(listener);
  }

  /**
   * @return The previously set focus change listener
   */
  ComponentFocusChangeListener getComponentFocusChangeListener() {
    return mOnFocusChangeListener;
  }

  /**
   * Sets a touch listener on this host.
   * @param listener The listener to set on this host.
   */
  void setComponentTouchListener(ComponentTouchListener listener) {
    mOnTouchListener = listener;
    setOnTouchListener(listener);
  }

  /**
   * Sets an {@link EventHandler} that will be invoked when
   * {@link ComponentHost#onInterceptTouchEvent} is called.
   * @param interceptTouchEventHandler the handler to be set on this host.
   */
  void setInterceptTouchEventHandler(EventHandler<InterceptTouchEvent> interceptTouchEventHandler) {
    mOnInterceptTouchEventHandler = interceptTouchEventHandler;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (mOnInterceptTouchEventHandler != null) {
      return EventDispatcherUtils.dispatchOnInterceptTouch(mOnInterceptTouchEventHandler, ev);
    }

    return super.onInterceptTouchEvent(ev);
  }

  /**
   * @return The previous set touch listener.
   */
  public ComponentTouchListener getComponentTouchListener() {
    return mOnTouchListener;
  }

  /**
   * This is used to collapse all invalidation calls on hosts during mount.
   * While invalidations are suppressed, the hosts will simply bail on
   * invalidations. Once the suppression is turned off, a single invalidation
   * will be triggered on the affected hosts.
   */
  void suppressInvalidations(boolean suppressInvalidations) {
    if (mSuppressInvalidations == suppressInvalidations) {
      return;
    }

    mSuppressInvalidations = suppressInvalidations;

    if (!mSuppressInvalidations) {
      if (mWasInvalidatedWhileSuppressed) {
        this.invalidate();
        mWasInvalidatedWhileSuppressed = false;
      }

      if (mWasInvalidatedForAccessibilityWhileSuppressed) {
        this.invalidateAccessibilityState();
        mWasInvalidatedForAccessibilityWhileSuppressed = false;
      }
    }
  }

  /**
   * Invalidates the accessibility node tree in this host.
   */
  void invalidateAccessibilityState() {
    if (!mIsComponentAccessibilityDelegateSet) {
      return;
    }

    if (mSuppressInvalidations) {
      mWasInvalidatedForAccessibilityWhileSuppressed = true;
      return;
    }

    if (mComponentAccessibilityDelegate != null && implementsVirtualViews()) {
      mComponentAccessibilityDelegate.invalidateRoot();
    }
  }

  @Override
  public boolean dispatchHoverEvent(MotionEvent event) {
    return (mComponentAccessibilityDelegate != null
      && implementsVirtualViews()
      && mComponentAccessibilityDelegate.dispatchHoverEvent(event))
      || super.dispatchHoverEvent(event);
  }

  private boolean implementsVirtualViews() {
    MountItem item = getAccessibleMountItem();
    return item != null
      && item.getComponent().getLifecycle().implementsExtraAccessibilityNodes();
  }

  public List<CharSequence> getContentDescriptions() {
    final List<CharSequence> contentDescriptions = new ArrayList<>();
    for (int i = 0, size = mDrawableMountItems.size(); i < size; i++) {
      final NodeInfo nodeInfo = mDrawableMountItems.valueAt(i).getNodeInfo();
      if (nodeInfo == null) {
        continue;
      }

      final CharSequence contentDescription = nodeInfo.getContentDescription();
      if (contentDescription != null) {
        contentDescriptions.add(contentDescription);
      }
    }
    final CharSequence hostContentDescription = getContentDescription();
    if (hostContentDescription != null) {
      contentDescriptions.add(hostContentDescription);
    }

    return contentDescriptions;
  }

  private void mountView(View view, int flags) {
    view.setDuplicateParentStateEnabled(MountItem.isDuplicateParentState(flags));

    mIsChildDrawingOrderDirty = true;

    // A host has been recycled and is already attached.
    if (view instanceof ComponentHost && view.getParent() == this) {
      finishTemporaryDetach(view);
      view.setVisibility(VISIBLE);
      return;
    }

    LayoutParams lp = view.getLayoutParams();
    if (lp == null) {
      lp = generateDefaultLayoutParams();
      view.setLayoutParams(lp);
    }

    if (mInLayout) {
      super.addViewInLayout(view, -1, view.getLayoutParams(), true);
    } else {
      super.addView(view, -1, view.getLayoutParams());
    }
  }

  private void unmountView(View view) {
    mIsChildDrawingOrderDirty = true;

    if (mScrapHosts != null && view instanceof ComponentHost) {
      final ComponentHost componentHost = (ComponentHost) view;

      view.setVisibility(GONE);

      // In Gingerbread the View system doesn't invalidate
      // the parent if a child become invisible.
      invalidate();

      startTemporaryDetach(componentHost);

      mScrapHosts.add(componentHost);
    } else if (mInLayout) {
      super.removeViewInLayout(view);
    } else {
      super.removeView(view);
    }
  }

  TouchExpansionDelegate getTouchExpansionDelegate() {
    return mTouchExpansionDelegate;
  }

  @Override
  public void dispatchDraw(Canvas canvas) {
    mDispatchDraw.start(canvas);

    super.dispatchDraw(canvas);

    // Cover the case where the host has no child views, in which case
    // getChildDrawingOrder() will not be called and the draw index will not
    // be incremented. This will also cover the case where drawables must be
    // painted after the last child view in the host.
    if (mDispatchDraw.isRunning()) {
      mDispatchDraw.drawNext();
    }

    mDispatchDraw.end();

    DebugDraw.draw(this, canvas);
  }

  @Override
  protected int getChildDrawingOrder(int childCount, int i) {
    updateChildDrawingOrderIfNeeded();

    // This method is called in very different contexts within a ViewGroup
    // e.g. when handling input events, drawing, etc. We only want to call
    // the draw methods if the InterleavedDispatchDraw is active.
    if (mDispatchDraw.isRunning()) {
      mDispatchDraw.drawNext();
    }

    return mChildDrawingOrder[i];
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    assertMainThread();

    boolean handled = false;

    if (isEnabled()) {
      // Iterate drawable from last to first to respect drawing order.
      for (int i = mDrawableMountItems.size() - 1; i >= 0; i--) {
        final MountItem item = mDrawableMountItems.valueAt(i);

        if (item.getContent() instanceof Touchable && !isTouchableDisabled(item.getFlags())) {
          final Touchable t = (Touchable) item.getContent();
          if (t.shouldHandleTouchEvent(event) && t.onTouchEvent(event, this)) {
            handled = true;
            break;
          }
        }
      }
    }

    if (!handled) {
      handled = super.onTouchEvent(event);
    }

    return handled;
  }

  void performLayout(boolean changed, int l, int t, int r, int b) {
  }

  @Override
  protected final void onLayout(boolean changed, int l, int t, int r, int b) {
    mInLayout = true;
    performLayout(changed, l, t, r, b);
    mInLayout = false;
  }

  @Override
  public void requestLayout() {
    // Don't request a layout if it will be blocked by any parent. Requesting a layout that is
    // then ignored by an ancestor means that this host will remain in a state where it thinks that
    // it has requested layout, and will therefore ignore future layout requests. This will lead to
    // problems if a child (e.g. a ViewPager) requests a layout later on, since the request will be
    // wrongly ignored by this host.
    ViewParent parent = this;
    while (parent instanceof ComponentHost) {
      final ComponentHost host = (ComponentHost) parent;
      if (!host.shouldRequestLayout()) {
        return;
      }

      parent = parent.getParent();
    }

    super.requestLayout();
  }

  protected boolean shouldRequestLayout() {
    // Don't bubble during layout.
    return !mInLayout;
  }

  @Override
  @SuppressLint("MissingSuperCall")
  protected boolean verifyDrawable(Drawable who) {
    return true;
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();

    for (int i = 0, size = mDrawableMountItems.size(); i < size; i++) {
      final MountItem mountItem = mDrawableMountItems.valueAt(i);
      ComponentHostUtils.maybeSetDrawableState(
          this,
          (Drawable) mountItem.getContent(),
          mountItem.getFlags(),
          mountItem.getNodeInfo());
    }
  }

  @Override
  public void jumpDrawablesToCurrentState() {
    super.jumpDrawablesToCurrentState();
    for (int i = 0, size = mDrawableMountItems.size(); i < size; i++) {
      final Drawable drawable = (Drawable) mDrawableMountItems.valueAt(i).getContent();
      DrawableCompat.jumpToCurrentState(drawable);
    }
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);

    for (int i = 0, size = mDrawableMountItems.size(); i < size; i++) {
      final Drawable drawable = (Drawable) mDrawableMountItems.valueAt(i).getContent();
      drawable.setVisible(visibility == View.VISIBLE, false);
    }
  }

  @DoNotStrip
  @Override
  public Object getTag() {
    if (mViewTag != null) {
      return mViewTag;
    }

    return super.getTag();
  }

  @Override
  public Object getTag(int key) {
    if (mViewTags != null) {
      final Object value = mViewTags.get(key);
      if (value != null) {
        return value;
      }
    }

    return super.getTag(key);
  }

  @Override
  public void invalidate(Rect dirty) {
    if (mSuppressInvalidations) {
      mWasInvalidatedWhileSuppressed = true;
      return;
    }

    super.invalidate(dirty);
  }

  @Override
  public void invalidate(int l, int t, int r, int b) {
    if (mSuppressInvalidations) {
      mWasInvalidatedWhileSuppressed = true;
      return;
    }

    super.invalidate(l, t, r, b);
  }

  @Override
  public void invalidate() {
    if (mSuppressInvalidations) {
      mWasInvalidatedWhileSuppressed = true;
      return;
    }

    super.invalidate();
  }

  protected void refreshAccessibilityDelegatesIfNeeded(boolean isAccessibilityEnabled) {
    if (isAccessibilityEnabled == mIsComponentAccessibilityDelegateSet) {
      return;
    }

    if (isAccessibilityEnabled && mComponentAccessibilityDelegate == null) {
      mComponentAccessibilityDelegate = new ComponentAccessibilityDelegate(this);
    }

    ViewCompat.setAccessibilityDelegate(
        this,
        isAccessibilityEnabled ? mComponentAccessibilityDelegate : null);
    mIsComponentAccessibilityDelegateSet = isAccessibilityEnabled;

    if (ComponentsConfiguration.lazyInitializeComponentAccessibilityDelegate
        && !isAccessibilityEnabled) {
      return;
    }

    for (int i = 0, size = getChildCount(); i < size; i++) {
      final View child = getChildAt(i);
      if (child instanceof ComponentHost) {
        ((ComponentHost) child).refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled);
      } else {
        final NodeInfo nodeInfo =
            (NodeInfo) child.getTag(R.id.component_node_info);
        if (nodeInfo != null) {
          ViewCompat.setAccessibilityDelegate(
              child,
              isAccessibilityEnabled ? new ComponentAccessibilityDelegate(child, nodeInfo) : null);
        }
      }
    }
  }

  @Override
  public void setAccessibilityDelegate(View.AccessibilityDelegate accessibilityDelegate) {
    super.setAccessibilityDelegate(accessibilityDelegate);

    // We cannot compare against mComponentAccessibilityDelegate directly, since it is not the
    // delegate that we receive here. Instead, we'll set this to true at the point that we set that
    // delegate explicitly.
    mIsComponentAccessibilityDelegateSet = false;
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  public void addView(View child) {
    throw new UnsupportedOperationException(
        "Adding Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  public void addView(View child, int index) {
    throw new UnsupportedOperationException(
        "Adding Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    throw new UnsupportedOperationException(
        "Adding Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  protected boolean addViewInLayout(
      View child, int index, ViewGroup.LayoutParams params, boolean preventRequestLayout) {
    throw new UnsupportedOperationException(
        "Adding Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  protected void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
    throw new UnsupportedOperationException(
        "Adding Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  public void removeView(View view) {
    throw new UnsupportedOperationException(
        "Removing Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  public void removeViewInLayout(View view) {
    throw new UnsupportedOperationException(
        "Removing Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  public void removeViewsInLayout(int start, int count) {
    throw new UnsupportedOperationException(
        "Removing Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  public void removeViewAt(int index) {
    throw new UnsupportedOperationException(
        "Removing Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  public void removeViews(int start, int count) {
    throw new UnsupportedOperationException(
        "Removing Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  public void removeAllViewsInLayout() {
    throw new UnsupportedOperationException(
        "Removing Views manually within LithoViews is not supported");
  }

  /**
   * Litho handles adding/removing views automatically using mount/unmount calls. Manually adding/
   * removing views will mess up Litho's bookkeeping of added views and cause weird crashes down the
   * line.
   */
  @Deprecated
  @Override
  protected void removeDetachedView(View child, boolean animate) {
    throw new UnsupportedOperationException(
        "Removing Views manually within LithoViews is not supported");
  }

  /**
   * Manually adds a View as a child of this ComponentHost for the purposes of testing. **This
   * should only be used for tests as this is not safe and will likely cause weird crashes if used
   * in a production environment**.
   */
  @VisibleForTesting
  public void addViewForTest(View view) {
    final LayoutParams params =
        view.getLayoutParams() == null ? generateDefaultLayoutParams() : view.getLayoutParams();
    super.addView(view, -1, params);
  }

  /**
   * Returns the Drawable associated with this ComponentHost for animations, for example the
   * background Drawable, or the drawable that otherwise has a transitionKey on it that has caused
   * it to be hosted in this ComponentHost.
   *
   * <p>The core purpose of exposing this drawable is so that when animating the bounds of this
   * ComponentHost, we also properly animate the bounds of this main Drawable at the same time.
   */
  public @Nullable Drawable getLinkedDrawableForAnimation() {
    for (int i = 0, size = mDrawableMountItems.size(); i < size; i++) {
      final MountItem mountItem = mDrawableMountItems.valueAt(i);
      if ((mountItem.getFlags() & MountItem.FLAG_IS_TRANSITION_KEY_SET) != 0) {
        return (Drawable) mountItem.getContent();
      }
    }
    return null;
  }

  private void updateChildDrawingOrderIfNeeded() {
    if (!mIsChildDrawingOrderDirty) {
      return;
    }

    final int childCount = getChildCount();
    if (mChildDrawingOrder.length < childCount) {
      mChildDrawingOrder = new int[childCount + 5];
    }

    int index = 0;
    final int viewMountItemCount = mViewMountItems.size();
    for (int i = 0, size = viewMountItemCount; i < size; i++) {
      final View child = (View) mViewMountItems.valueAt(i).getContent();
      mChildDrawingOrder[index++] = indexOfChild(child);
    }

    // Draw disappearing items on top of mounted views.
    for (int i = 0, size = mDisappearingItems.size(); i < size; i++) {
      final View child = (View) mDisappearingItems.get(i).getContent();
      mChildDrawingOrder[index++] = indexOfChild(child);
    }

    if (mScrapHosts != null) {
      for (int i = 0, size = mScrapHosts.size(); i < size; i++) {
        final View child = mScrapHosts.get(i);
        mChildDrawingOrder[index++] = indexOfChild(child);
      }
    }

    mIsChildDrawingOrderDirty = false;
  }

  private void ensureScrapViewMountItemsArray() {
    if (mScrapViewMountItemsArray == null) {
      mScrapViewMountItemsArray = ComponentsPools.acquireScrapMountItemsArray();
    }
  }

  private void ensureScrapMountItemsArray() {
    if (mScrapMountItemsArray == null) {
      mScrapMountItemsArray = ComponentsPools.acquireScrapMountItemsArray();
    }
  }

  private void releaseScrapDataStructuresIfNeeded() {
    if (mScrapMountItemsArray != null && mScrapMountItemsArray.size() == 0) {
      ComponentsPools.releaseScrapMountItemsArray(mScrapMountItemsArray);
      mScrapMountItemsArray = null;
    }

    if (mScrapViewMountItemsArray != null && mScrapViewMountItemsArray.size() == 0) {
      ComponentsPools.releaseScrapMountItemsArray(mScrapViewMountItemsArray);
      mScrapViewMountItemsArray = null;
    }
  }

  private void mountDrawable(int index, MountItem mountItem, Rect bounds) {
    assertMainThread();

    mDrawableMountItems.put(index, mountItem);
    final Drawable drawable = (Drawable) mountItem.getContent();
    final DisplayListDrawable displayListDrawable = mountItem.getDisplayListDrawable();

    ComponentHostUtils.mountDrawable(
        this,
        displayListDrawable != null ? displayListDrawable : drawable,
        bounds,
        mountItem.getFlags(),
        mountItem.getNodeInfo());
  }

  private void unmountDrawable(int index, MountItem mountItem) {
    assertMainThread();

    final Drawable contentDrawable = (Drawable) mountItem.getContent();
    final Drawable drawable = mountItem.getDisplayListDrawable() == null
        ? contentDrawable
        : mountItem.getDisplayListDrawable();

    if (ComponentHostUtils.existsScrapItemAt(index, mScrapDrawableMountItems)) {
      mScrapDrawableMountItems.remove(index);
    } else {
      mDrawableMountItems.remove(index);
    }

    drawable.setCallback(null);

    this.invalidate(drawable.getBounds());

    releaseScrapDataStructuresIfNeeded();
  }

  private void moveDrawableItem(MountItem item, int oldIndex, int newIndex) {
    assertMainThread();

    // When something is already present in newIndex position we need to keep track of it.
    if (mDrawableMountItems.get(newIndex) != null) {
      ensureScrapDrawableMountItemsArray();

      ComponentHostUtils.scrapItemAt(newIndex, mDrawableMountItems, mScrapDrawableMountItems);
    }

    // Move the MountItem in the new position.
    ComponentHostUtils.moveItem(oldIndex, newIndex, mDrawableMountItems, mScrapDrawableMountItems);

    // Drawing order changed, invalidate the whole view.
    this.invalidate();

    releaseScrapDataStructuresIfNeeded();
  }

  private void ensureScrapDrawableMountItemsArray() {
    if (mScrapDrawableMountItems == null) {
      mScrapDrawableMountItems = ComponentsPools.acquireScrapMountItemsArray();
    }
  }

  private static void startTemporaryDetach(View view) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      // Cancel any pending clicks.
      view.cancelPendingInputEvents();
    }
    // The ComponentHost's parent will send an ACTION_CANCEL if it's going to receive
    // other motion events for the recycled child.
    ViewCompat.dispatchStartTemporaryDetach(view);
  }

  private static void finishTemporaryDetach(View view) {
    ViewCompat.dispatchFinishTemporaryDetach(view);
  }

  /**
   * Encapsulates the logic for drawing a set of views and drawables respecting
   * their drawing order withing the component host i.e. allow interleaved views
   * and drawables to be drawn with the correct z-index.
   */
  private class InterleavedDispatchDraw {

    private Canvas mCanvas;
    private int mDrawIndex;
    private int mItemsToDraw;

    private InterleavedDispatchDraw() {
    }

    private void start(Canvas canvas) {
      mCanvas = canvas;
      mDrawIndex = 0;
      mItemsToDraw = mMountItems.size();
    }

    private boolean isRunning() {
      return (mCanvas != null && mDrawIndex < mItemsToDraw);
    }

    private void drawNext() {
      if (mCanvas == null) {
        return;
      }

      for (int i = mDrawIndex, size = mMountItems.size(); i < size; i++) {
        final MountItem mountItem = mMountItems.valueAt(i);

        final Object content = mountItem.getDisplayListDrawable() != null ?
            mountItem.getDisplayListDrawable() :
            mountItem.getContent();

        // During a ViewGroup's dispatchDraw() call with children drawing order enabled,
        // getChildDrawingOrder() will be called before each child view is drawn. This
        // method will only draw the drawables "between" the child views and the let
        // the host draw its children as usual. This is why views are skipped here.
        if (content instanceof View) {
          mDrawIndex = i + 1;
          return;
        }

        final boolean isTracing = ComponentsSystrace.isTracing();
        if (isTracing) {
          ComponentsSystrace.beginSection(getTraceName(mountItem));
        }
        ((Drawable) content).draw(mCanvas);
        if (isTracing) {
          ComponentsSystrace.endSection();
        }
      }

      mDrawIndex = mItemsToDraw;
    }

    private void end() {
      mCanvas = null;
    }
  }

  private static String getTraceName(MountItem mountItem) {
    String traceName = "draw: " + mountItem.getComponent().getSimpleName();
    final DisplayListDrawable displayListDrawable = mountItem.getDisplayListDrawable();
    if (displayListDrawable != null && displayListDrawable.willDrawDisplayList()) {
      traceName += "DL";
    }
    return traceName;
  }

  @Override
  public boolean performAccessibilityAction(int action, Bundle arguments) {
    // The view framework requires that a contentDescription be set for the
    // getIterableTextForAccessibility method to work.  If one isn't set, all text granularity
    // actions will be ignored.
    if (action == AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
        || action == AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY) {
      CharSequence contentDesc = null;
      if (!TextUtils.isEmpty(getContentDescription())) {
        contentDesc = getContentDescription();
      } else if (getContentDescriptions().size() != 0) {
        contentDesc = TextUtils.join(", ", getContentDescriptions());
      } else if (getTextContent().getTextItems().size() != 0) {
        contentDesc = TextUtils.join(", ", getTextContent().getTextItems());
      }

      if (contentDesc == null) {
        return false;
      }

      mContentDescription = contentDesc;
      super.setContentDescription(mContentDescription);
    }

    return super.performAccessibilityAction(action, arguments);
  }
}
