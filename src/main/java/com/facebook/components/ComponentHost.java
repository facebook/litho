// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.facebook.R;

import static com.facebook.components.AccessibilityUtils.isAccessibilityEnabled;
import static com.facebook.components.ComponentHostUtils.maybeInvalidateAccessibilityState;

/**
 * A {@link ViewGroup} that can host the mounted state of a {@link Component}. This is used
 * by {@link MountState} to wrap mounted drawables to handle click events and update drawable
 * states accordingly.
 */
public class ComponentHost extends ViewGroup {

  private final SparseArrayCompat<MountItem> mMountItems = new SparseArrayCompat<>();
  private SparseArrayCompat<MountItem> mScrapMountItemsArray;

  private final SparseArrayCompat<MountItem> mViewMountItems = new SparseArrayCompat<>();
  private SparseArrayCompat<MountItem> mScrapViewMountItemsArray;

  private final SparseArrayCompat<MountItem> mDrawableMountItems = new SparseArrayCompat<>();
  private SparseArrayCompat<MountItem> mScrapDrawableMountItems;

  private final SparseArrayCompat<Touchable> mTouchables = new SparseArrayCompat<>();
  private SparseArrayCompat<Touchable> mScrapTouchables;

  private CharSequence mContentDescription;
  private Object mViewTag;
  private SparseArray<Object> mViewTags;

  private boolean mWasInvalidatedWhileSuppressed;
  private boolean mWasInvalidatedForAccessibilityWhileSuppressed;
  private boolean mSuppressInvalidations;

  private final InterleavedDispatchDraw mDispatchDraw = new InterleavedDispatchDraw();

  private final List<ComponentHost> mScrapHosts = new ArrayList<>(3);
  private final ComponentsLogger mLogger;

  private int[] mChildDrawingOrder = new int[0];
  private boolean mIsChildDrawingOrderDirty;

  private long mParentHostMarker;
  private boolean mInLayout;

  private ComponentAccessibilityDelegate mComponentAccessibilityDelegate;
  private boolean mIsComponentAccessibilityDelegateSet = false;

  private ComponentClickListener mOnClickListener;
  private ComponentLongClickListener mOnLongClickListener;
  private ComponentTouchListener mOnTouchListener;

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

    mLogger = context.getLogger();
    mComponentAccessibilityDelegate = new ComponentAccessibilityDelegate(this);
    refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(context));
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
      maybeUnregisterTouchExpansion(index, mountItem);
    }

    ComponentHostUtils.removeItem(index, mMountItems, mScrapMountItemsArray);
    releaseScrapDataStructuresIfNeeded();
    maybeInvalidateAccessibilityState(mountItem);
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

  private void maybeRegisterTouchExpansion(int index, MountItem mountItem) {
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

  private void maybeUnregisterTouchExpansion(int index, MountItem mountItem) {
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
      mComponentAccessibilityDelegate.setNodeInfo((NodeInfo) tag);
      refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(getContext()));
    }
  }

  /**
   * Moves the MountItem associated to oldIndex in the newIndex position. This happens when a
   * ComponentView needs to re-arrange the internal order of its items. If an item is already
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
   * Sets a touch listener on this host.
   * @param listener The listener to set on this host.
   */
  void setComponentTouchListener(ComponentTouchListener listener) {
    mOnTouchListener = listener;
    setOnTouchListener(listener);
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

    if (mComponentAccessibilityDelegate != null) {
      mComponentAccessibilityDelegate.invalidateRoot();
    }
  }

  @Override
  public boolean dispatchHoverEvent(MotionEvent event) {
    return (mComponentAccessibilityDelegate != null
        && mComponentAccessibilityDelegate.dispatchHoverEvent(event))
        || super.dispatchHoverEvent(event);
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
      addViewInLayout(view, -1, view.getLayoutParams(), true);
    } else {
      addView(view, -1, view.getLayoutParams());
    }
  }

  private void unmountView(View view) {
    mIsChildDrawingOrderDirty = true;

    if (view instanceof ComponentHost) {
      final ComponentHost componentHost = (ComponentHost) view;

      view.setVisibility(GONE);

      // In Gingerbread the View system doesn't invalidate
      // the parent if a child become invisible.
      invalidate();

      startTemporaryDetach(componentHost);

      mScrapHosts.add(componentHost);
    } else if (mInLayout) {
      removeViewInLayout(view);
    } else {
      removeView(view);
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
    boolean handled = false;

    // Iterate drawable from last to first to respect drawing order.
    for (int size = mTouchables.size(), i = size - 1; i >= 0; i--) {
      final Touchable t = mTouchables.valueAt(i);
      if (t.shouldHandleTouchEvent(event) && t.onTouchEvent(event, this)) {
        handled = true;
        break;
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

    ViewCompat.setAccessibilityDelegate(
        this,
        isAccessibilityEnabled ? mComponentAccessibilityDelegate : null);
    mIsComponentAccessibilityDelegateSet = isAccessibilityEnabled;

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
  public void setAccessibilityDelegate(AccessibilityDelegate accessibilityDelegate) {
    super.setAccessibilityDelegate(accessibilityDelegate);

    // We cannot compare against mComponentAccessibilityDelegate directly, since it is not the
    // delegate that we receive here. Instead, we'll set this to true at the point that we set that
    // delegate explicitly.
    mIsComponentAccessibilityDelegateSet = false;
  }

  private void updateChildDrawingOrderIfNeeded() {
    if (!mIsChildDrawingOrderDirty) {
      return;
    }

    final int childCount = getChildCount();
    if (mChildDrawingOrder.length < childCount) {
      mChildDrawingOrder = new int[childCount + 5];
    }

    final int viewMountItemCount = mViewMountItems.size();
    for (int i = 0, size = viewMountItemCount; i < size; i++) {
      final View child = (View) mViewMountItems.valueAt(i).getContent();
      mChildDrawingOrder[i] = indexOfChild(child);
    }

    for (int i = 0, size = mScrapHosts.size(); i < size; i++) {
      final View child = (View) mScrapHosts.get(i);
      mChildDrawingOrder[i + viewMountItemCount] = indexOfChild(child);
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
    mDrawableMountItems.put(index, mountItem);
    final Drawable drawable = (Drawable) mountItem.getContent();
    final DisplayListDrawable displayListDrawable = mountItem.getDisplayListDrawable();

    ComponentHostUtils.mountDrawable(
        this,
        displayListDrawable != null ? displayListDrawable : drawable,
        bounds,
        mountItem.getFlags(),
        mountItem.getNodeInfo());

    if (drawable instanceof Touchable) {
      mTouchables.put(index, (Touchable) drawable);
    }
  }

  private void unmountDrawable(int index, MountItem mountItem) {
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

    if (contentDrawable instanceof Touchable) {
      if (ComponentHostUtils.existsScrapItemAt(index, mScrapTouchables)) {
        mScrapTouchables.remove(index);
      } else {
        mTouchables.remove(index);
      }
    }

    this.invalidate(drawable.getBounds());

    releaseScrapDataStructuresIfNeeded();
  }

  private void moveDrawableItem(MountItem item, int oldIndex, int newIndex) {
    // When something is already present in newIndex position we need to keep track of it.
    if (mDrawableMountItems.get(newIndex) != null) {
      ensureScrapDrawableMountItemsArray();

      ComponentHostUtils.scrapItemAt(newIndex, mDrawableMountItems, mScrapDrawableMountItems);
    }

    if (mTouchables.get(newIndex) != null) {
      ensureScrapTouchablesArray();

      ComponentHostUtils.scrapItemAt(newIndex, mTouchables, mScrapTouchables);
    }

    // Move the MountItem in the new position. If the mount item was a Touchable we need to reflect
    // this change also in the Touchables SparseArray.
    ComponentHostUtils.moveItem(oldIndex, newIndex, mDrawableMountItems, mScrapDrawableMountItems);
    if (item.getContent() instanceof Touchable) {
      ComponentHostUtils.moveItem(oldIndex, newIndex, mTouchables, mScrapTouchables);
    }

    // Drawing order changed, invalidate the whole view.
    this.invalidate();

    releaseScrapDataStructuresIfNeeded();
  }

  private void ensureScrapDrawableMountItemsArray() {
    if (mScrapDrawableMountItems == null) {
      mScrapDrawableMountItems = ComponentsPools.acquireScrapMountItemsArray();
    }
  }

  private void ensureScrapTouchablesArray() {
    if (mScrapTouchables == null) {
      mScrapTouchables = ComponentsPools.acquireScrapTouchablesArray();
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

    private InterleavedDispatchDraw() {

    }

    private void start(Canvas canvas) {
      mCanvas = canvas;
      mDrawIndex = 0;
    }

    private boolean isRunning() {
      return (mCanvas != null && mDrawIndex < mMountItems.size());
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

        ComponentsSystrace.beginSection(mountItem.getComponent().getSimpleName());
        ((Drawable) content).draw(mCanvas);
        ComponentsSystrace.endSection();
      }

      mDrawIndex = mMountItems.size();
    }

    private void end() {
      mCanvas = null;
    }
  }
}
