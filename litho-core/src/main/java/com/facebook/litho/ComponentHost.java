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

import static com.facebook.litho.AccessibilityUtils.isAccessibilityEnabled;
import static com.facebook.litho.ComponentHostUtils.maybeInvalidateAccessibilityState;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.LayoutOutput.isTouchableDisabled;
import static com.facebook.litho.ThreadUtils.assertMainThread;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.SparseArrayCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import com.facebook.proguard.annotations.DoNotStrip;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link ViewGroup} that can host the mounted state of a {@link Component}. This is used by
 * {@link MountState} to wrap mounted drawables to handle click events and update drawable states
 * accordingly.
 */
@DoNotStrip
public class ComponentHost extends Host {

  private static final int SCRAP_ARRAY_INITIAL_SIZE = 4;

  private SparseArrayCompat<MountItem> mMountItems;
  private SparseArrayCompat<MountItem> mScrapMountItemsArray;

  private SparseArrayCompat<MountItem> mViewMountItems;
  private SparseArrayCompat<MountItem> mScrapViewMountItemsArray;

  private SparseArrayCompat<MountItem> mDrawableMountItems;
  private SparseArrayCompat<MountItem> mScrapDrawableMountItems;

  private ArrayList<MountItem> mDisappearingItems;

  private CharSequence mContentDescription;
  private Object mViewTag;
  private SparseArray<Object> mViewTags;

  private boolean mDisallowIntercept;

  private final InterleavedDispatchDraw mDispatchDraw = new InterleavedDispatchDraw();

  private int[] mChildDrawingOrder = new int[0];
  private boolean mIsChildDrawingOrderDirty;

  private boolean mInLayout;

  @Nullable private ComponentAccessibilityDelegate mComponentAccessibilityDelegate;
  private boolean mIsComponentAccessibilityDelegateSet = false;

  private ComponentClickListener mOnClickListener;
  private ComponentLongClickListener mOnLongClickListener;
  private ComponentFocusChangeListener mOnFocusChangeListener;
  private ComponentTouchListener mOnTouchListener;
  private EventHandler<InterceptTouchEvent> mOnInterceptTouchEventHandler;

  private TouchExpansionDelegate mTouchExpansionDelegate;

  /**
   * {@link ViewGroup#getClipChildren()} was only added in API 18, will need to keep track of this
   * flag ourselves on the lower versions
   */
  private boolean mClipChildren = true;

  private boolean mClippingTemporaryDisabled = false;
  private boolean mClippingToRestore = false;

  public ComponentHost(Context context) {
    this(context, null);
  }

  public ComponentHost(Context context, AttributeSet attrs) {
    this(new ComponentContext(context), attrs);
  }

  @Override
  public void mount(int index, MountItem mountItem) {
    Rect bounds = new Rect();
    mountItem.getRenderTreeNode().getMountBounds(bounds);
    mount(index, mountItem, bounds);
  }

  public ComponentHost(ComponentContext context) {
    this(context, null);
  }

  public ComponentHost(ComponentContext context, AttributeSet attrs) {
    super(context.getAndroidContext(), attrs);
    setWillNotDraw(false);
    setChildrenDrawingOrderEnabled(true);
    refreshAccessibilityDelegatesIfNeeded(isAccessibilityEnabled(context.getAndroidContext()));

    mMountItems = new SparseArrayCompat<>();
    mViewMountItems = new SparseArrayCompat<>();
    mDrawableMountItems = new SparseArrayCompat<>();
    mDisappearingItems = new ArrayList<>();
  }

  /**
   * Mounts the given {@link MountItem} with unique index.
   *
   * @param index index of the {@link MountItem}. Guaranteed to be the same index as is passed for
   *     the corresponding {@code unmount(index, mountItem)} call.
   * @param mountItem item to be mounted into the host.
   * @param bounds the bounds of the item that is to be mounted into the host
   */
  public void mount(int index, MountItem mountItem, Rect bounds) {
    final Object content = mountItem.getContent();
    final LayoutOutput output = getLayoutOutput(mountItem);
    if (content instanceof Drawable) {
      mountDrawable(index, mountItem, bounds);
    } else if (content instanceof View) {
      ensureViewMountItems();
      mViewMountItems.put(index, mountItem);
      mountView((View) content, output.getFlags());
    }

    ensureMountItems();
    mMountItems.put(index, mountItem);
  }

  private void ensureMountItems() {
    if (mMountItems == null) {
      mMountItems = new SparseArrayCompat<>();
    }
  }

  private void ensureViewMountItems() {
    if (mViewMountItems == null) {
      mViewMountItems = new SparseArrayCompat<>();
    }
  }

  private void ensureDrawableMountItems() {
    if (mDrawableMountItems == null) {
      mDrawableMountItems = new SparseArrayCompat<>();
    }
  }

  private void ensureDisappearingItems() {
    if (mDisappearingItems == null) {
      mDisappearingItems = new ArrayList<>();
    }
  }

  @Override
  public void unmount(MountItem item) {
    ensureMountItems();
    final int index = mMountItems.keyAt(mMountItems.indexOfValue(item));
    unmount(index, item);
  }

  /**
   * Unmounts the given {@link MountItem} with unique index.
   *
   * @param index index of the {@link MountItem}. Guaranteed to be the same index as was passed for
   *     the corresponding {@code mount(index, mountItem)} call.
   * @param mountItem item to be unmounted from the host.
   */
  @Override
  public void unmount(int index, MountItem mountItem) {
    final Object content = mountItem.getContent();
    final LayoutOutput output = getLayoutOutput(mountItem);
    if (content instanceof Drawable) {
      ensureDrawableMountItems();

      unmountDrawable(mountItem);
      ComponentHostUtils.removeItem(index, mDrawableMountItems, mScrapDrawableMountItems);
    } else if (content instanceof View) {
      unmountView((View) content);

      ensureViewMountItems();
      ComponentHostUtils.removeItem(index, mViewMountItems, mScrapViewMountItemsArray);
      mIsChildDrawingOrderDirty = true;
    }

    ensureMountItems();
    ComponentHostUtils.removeItem(index, mMountItems, mScrapMountItemsArray);
    releaseScrapDataStructuresIfNeeded();
  }

  void startUnmountDisappearingItem(int index, MountItem mountItem) {
    final Object content = mountItem.getContent();

    if (content instanceof Drawable) {
      ensureDrawableMountItems();

      ComponentHostUtils.removeItem(index, mDrawableMountItems, mScrapDrawableMountItems);
    } else if (content instanceof View) {
      ensureViewMountItems();
      ComponentHostUtils.removeItem(index, mViewMountItems, mScrapViewMountItemsArray);
      mIsChildDrawingOrderDirty = true;
      maybeUnregisterTouchExpansion(index, getLayoutOutput(mountItem), content);
    }
    ensureMountItems();
    ComponentHostUtils.removeItem(index, mMountItems, mScrapMountItemsArray);
    releaseScrapDataStructuresIfNeeded();
    ensureDisappearingItems();
    mDisappearingItems.add(mountItem);
  }

  void unmountDisappearingItem(MountItem disappearingItem) {
    ensureDisappearingItems();
    if (!mDisappearingItems.remove(disappearingItem)) {
      final TransitionId transitionId = getLayoutOutput(disappearingItem).getTransitionId();
      throw new RuntimeException(
          "Tried to remove non-existent disappearing item, transitionId: " + transitionId);
    }

    final Object content = disappearingItem.getContent();
    if (content instanceof Drawable) {
      unmountDrawable(disappearingItem);
    } else if (content instanceof View) {
      unmountView((View) content);
    }

    maybeInvalidateAccessibilityState(getLayoutOutput(disappearingItem), this);
  }

  boolean hasDisappearingItems() {
    return mDisappearingItems != null && !mDisappearingItems.isEmpty();
  }

  @Nullable
  List<TransitionId> getDisappearingItemTransitionIds() {
    if (!hasDisappearingItems()) {
      return null;
    }
    final List<TransitionId> ids = new ArrayList<>();
    for (int i = 0, size = mDisappearingItems.size(); i < size; i++) {
      ids.add(getLayoutOutput(mDisappearingItems.get(i)).getTransitionId());
    }

    return ids;
  }

  private void maybeMoveTouchExpansionIndexes(MountItem item, int oldIndex, int newIndex) {
    final ViewNodeInfo viewNodeInfo = getLayoutOutput(item).getViewNodeInfo();
    if (viewNodeInfo == null) {
      return;
    }

    final Rect expandedTouchBounds = viewNodeInfo.getExpandedTouchBounds();
    if (expandedTouchBounds == null || mTouchExpansionDelegate == null) {
      return;
    }

    mTouchExpansionDelegate.moveTouchExpansionIndexes(oldIndex, newIndex);
  }

  void maybeRegisterTouchExpansion(int index, LayoutOutput output, Object content) {
    final ViewNodeInfo viewNodeInfo = output.getViewNodeInfo();
    if (viewNodeInfo == null) {
      return;
    }

    final Rect expandedTouchBounds = viewNodeInfo.getExpandedTouchBounds();
    if (expandedTouchBounds == null) {
      return;
    }

    if (this.equals(content)) {
      // Don't delegate to ourselves or we'll cause a StackOverflowError
      return;
    }

    if (mTouchExpansionDelegate == null) {
      mTouchExpansionDelegate = new TouchExpansionDelegate(this);
      setTouchDelegate(mTouchExpansionDelegate);
    }

    mTouchExpansionDelegate.registerTouchExpansion(index, (View) content, expandedTouchBounds);
  }

  void maybeUnregisterTouchExpansion(int index, LayoutOutput output, Object content) {
    final ViewNodeInfo viewNodeInfo = output.getViewNodeInfo();
    if (viewNodeInfo == null) {
      return;
    }

    if (mTouchExpansionDelegate == null || viewNodeInfo.getExpandedTouchBounds() == null) {
      return;
    }

    if (this.equals(content)) {
      // Recursive delegation is never registered
      return;
    }

    mTouchExpansionDelegate.unregisterTouchExpansion(index);
  }

  /** @return number of {@link MountItem}s that are currently mounted in the host. */
  @Override
  public int getMountItemCount() {
    return mMountItems == null ? 0 : mMountItems.size();
  }

  /** @return the {@link MountItem} that was mounted with the given index. */
  @Override
  public MountItem getMountItemAt(int index) {
    return mMountItems.valueAt(index);
  }

  /**
   * Hosts are guaranteed to have only one accessible component in them due to the way the view
   * hierarchy is constructed in {@link LayoutState}. There might be other non-accessible components
   * in the same hosts such as a background/foreground component though. This is why this method
   * iterates over all mount items in order to find the accessible one.
   */
  @Nullable
  MountItem getAccessibleMountItem() {
    for (int i = 0; i < getMountItemCount(); i++) {
      MountItem item = getMountItemAt(i);
      // For inexplicable reason, item is null sometimes.
      if (item != null && getLayoutOutput(item).isAccessible()) {
        return item;
      }
    }

    return null;
  }

  /** @return list of drawables that are mounted on this host. */
  public List<Drawable> getDrawables() {
    if (mDrawableMountItems == null || mDrawableMountItems.size() == 0) {
      return Collections.emptyList();
    }

    final List<Drawable> drawables = new ArrayList<>(mDrawableMountItems.size());
    for (int i = 0, size = mDrawableMountItems.size(); i < size; i++) {
      Drawable drawable = (Drawable) mDrawableMountItems.valueAt(i).getContent();
      drawables.add(drawable);
    }

    return drawables;
  }

  /** @return list of names of content mounted on this host. */
  public List<String> getContentNames() {
    if (mMountItems == null || mMountItems.size() == 0) {
      return Collections.emptyList();
    }

    final int contentSize = mMountItems.size();
    final List<String> contentNames = new ArrayList<>(contentSize);
    for (int i = 0; i < contentSize; i++) {
      contentNames.add(getMountItemName(getMountItemAt(i)));
    }

    return contentNames;
  }

  /** @return the text content that is mounted on this host. */
  @DoNotStrip
  public TextContent getTextContent() {
    ensureMountItems();
    return ComponentHostUtils.extractTextContent(ComponentHostUtils.extractContent(mMountItems));
  }

  /** @return the image content that is mounted on this host. */
  public ImageContent getImageContent() {
    ensureMountItems();
    return ComponentHostUtils.extractImageContent(ComponentHostUtils.extractContent(mMountItems));
  }

  /** @return the content descriptons that are set on content mounted on this host */
  @Override
  public CharSequence getContentDescription() {
    return mContentDescription;
  }

  /**
   * Host views implement their own content description handling instead of just delegating to the
   * underlying view framework for performance reasons as the framework sets/resets content
   * description very frequently on host views and the underlying accessibility notifications might
   * cause performance issues. This is safe to do because the framework owns the accessibility state
   * and knows how to update it efficiently.
   */
  @Override
  public void setContentDescription(CharSequence contentDescription) {
    mContentDescription = contentDescription;

    if (!TextUtils.isEmpty(contentDescription)
        && ViewCompat.getImportantForAccessibility(this)
            == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
      ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    invalidateAccessibilityState();
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
   * LithoView needs to re-arrange the internal order of its items. If an item is already present in
   * newIndex the item is guaranteed to be either unmounted or moved to a different index by
   * subsequent calls to either {@link ComponentHost#unmount(int, MountItem)} or {@link
   * ComponentHost#moveItem(MountItem, int, int)}.
   *
   * @param item The item that has been moved.
   * @param oldIndex The current index of the MountItem.
   * @param newIndex The new index of the MountItem.
   */
  @Override
  public void moveItem(MountItem item, int oldIndex, int newIndex) {
    if (item == null && mScrapMountItemsArray != null) {
      item = mScrapMountItemsArray.get(oldIndex);
    }

    if (item == null) {
      return;
    }
    maybeMoveTouchExpansionIndexes(item, oldIndex, newIndex);

    final Object content = item.getContent();

    ensureViewMountItems();

    if (content instanceof Drawable) {
      moveDrawableItem(item, oldIndex, newIndex);
    } else if (content instanceof View) {
      mIsChildDrawingOrderDirty = true;

      if (!mDisallowIntercept) {
        startTemporaryDetach(((View) content));
      }

      if (mViewMountItems.get(newIndex) != null) {
        ensureScrapViewMountItemsArray();

        ComponentHostUtils.scrapItemAt(newIndex, mViewMountItems, mScrapViewMountItemsArray);
      }

      ComponentHostUtils.moveItem(oldIndex, newIndex, mViewMountItems, mScrapViewMountItemsArray);
    }

    ensureMountItems();
    if (mMountItems.get(newIndex) != null) {
      ensureScrapMountItemsArray();

      ComponentHostUtils.scrapItemAt(newIndex, mMountItems, mScrapMountItemsArray);
    }

    ComponentHostUtils.moveItem(oldIndex, newIndex, mMountItems, mScrapMountItemsArray);

    releaseScrapDataStructuresIfNeeded();

    if (!mDisallowIntercept && content instanceof View) {
      finishTemporaryDetach(((View) content));
    }
  }

  /**
   * Sets view tag on this host.
   *
   * @param viewTag the object to set as tag.
   */
  public void setViewTag(Object viewTag) {
    mViewTag = viewTag;
  }

  /**
   * Sets view tags on this host.
   *
   * @param viewTags the map containing the tags by id.
   */
  public void setViewTags(SparseArray<Object> viewTags) {
    mViewTags = viewTags;
  }

  /**
   * Sets a click listener on this host.
   *
   * @param listener The listener to set on this host.
   */
  void setComponentClickListener(ComponentClickListener listener) {
    mOnClickListener = listener;
    this.setOnClickListener(listener);
  }

  /** @return The previously set click listener */
  ComponentClickListener getComponentClickListener() {
    return mOnClickListener;
  }

  /**
   * Sets a long click listener on this host.
   *
   * @param listener The listener to set on this host.
   */
  void setComponentLongClickListener(ComponentLongClickListener listener) {
    mOnLongClickListener = listener;
    this.setOnLongClickListener(listener);
  }

  /** @return The previously set long click listener */
  ComponentLongClickListener getComponentLongClickListener() {
    return mOnLongClickListener;
  }

  /**
   * Sets a focus change listener on this host.
   *
   * @param listener The listener to set on this host.
   */
  void setComponentFocusChangeListener(ComponentFocusChangeListener listener) {
    mOnFocusChangeListener = listener;
    this.setOnFocusChangeListener(listener);
  }

  /** @return The previously set focus change listener */
  ComponentFocusChangeListener getComponentFocusChangeListener() {
    return mOnFocusChangeListener;
  }

  /**
   * Sets a touch listener on this host.
   *
   * @param listener The listener to set on this host.
   */
  void setComponentTouchListener(ComponentTouchListener listener) {
    mOnTouchListener = listener;
    setOnTouchListener(listener);
  }

  /**
   * Sets an {@link EventHandler} that will be invoked when {@link
   * ComponentHost#onInterceptTouchEvent} is called.
   *
   * @param interceptTouchEventHandler the handler to be set on this host.
   */
  void setInterceptTouchEventHandler(EventHandler<InterceptTouchEvent> interceptTouchEventHandler) {
    mOnInterceptTouchEventHandler = interceptTouchEventHandler;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    int action = ev.getAction();
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      mDisallowIntercept = false;
    }

    if (mOnInterceptTouchEventHandler != null) {
      return EventDispatcherUtils.dispatchOnInterceptTouch(mOnInterceptTouchEventHandler, this, ev);
    }

    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    super.requestDisallowInterceptTouchEvent(disallowIntercept);
    this.mDisallowIntercept = disallowIntercept;
  }

  /** @return The previous set touch listener. */
  public ComponentTouchListener getComponentTouchListener() {
    return mOnTouchListener;
  }

  /** Invalidates the accessibility node tree in this host. */
  void invalidateAccessibilityState() {
    if (!mIsComponentAccessibilityDelegateSet) {
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
    return item != null && getLayoutOutput(item).getComponent().implementsExtraAccessibilityNodes();
  }

  public List<CharSequence> getContentDescriptions() {
    final List<CharSequence> contentDescriptions = new ArrayList<>();
    for (int i = 0, size = mDrawableMountItems == null ? 0 : mDrawableMountItems.size();
        i < size;
        i++) {
      final NodeInfo nodeInfo = getLayoutOutput(mDrawableMountItems.valueAt(i)).getNodeInfo();
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
    view.setDuplicateParentStateEnabled(LayoutOutput.isDuplicateParentState(flags));

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

    if (mInLayout) {
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

    // Everything from mMountItems was drawn at this point. Then ViewGroup took care of drawing
    // disappearing views, as they still added as children. Thus the only thing left to draw is
    // disappearing drawables
    for (int index = 0, size = mDisappearingItems == null ? 0 : mDisappearingItems.size();
        index < size;
        ++index) {
      final Object content = mDisappearingItems.get(index).getContent();
      if (content instanceof Drawable) {
        ((Drawable) content).draw(canvas);
      }
    }

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
  public boolean shouldDelayChildPressedState() {
    return false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    assertMainThread();

    boolean handled = false;

    if (isEnabled()) {
      // Iterate drawable from last to first to respect drawing order.
      for (int i = ((mDrawableMountItems == null) ? 0 : mDrawableMountItems.size()) - 1;
          i >= 0;
          i--) {
        final MountItem item = mDrawableMountItems.valueAt(i);

        if (item.getContent() instanceof Touchable
            && !isTouchableDisabled(getLayoutOutput(item).getFlags())) {
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

  void performLayout(boolean changed, int l, int t, int r, int b) {}

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

    for (int i = 0, size = (mDrawableMountItems == null) ? 0 : mDrawableMountItems.size();
        i < size;
        i++) {
      final MountItem mountItem = mDrawableMountItems.valueAt(i);
      final LayoutOutput output = getLayoutOutput(mountItem);
      ComponentHostUtils.maybeSetDrawableState(
          this, (Drawable) mountItem.getContent(), output.getFlags(), output.getNodeInfo());
    }
  }

  @Override
  public void jumpDrawablesToCurrentState() {
    super.jumpDrawablesToCurrentState();

    for (int i = 0, size = (mDrawableMountItems == null) ? 0 : mDrawableMountItems.size();
        i < size;
        i++) {
      final Drawable drawable = (Drawable) mDrawableMountItems.valueAt(i).getContent();
      DrawableCompat.jumpToCurrentState(drawable);
    }
  }

  @Override
  public void setVisibility(int visibility) {
    assertMainThread();
    super.setVisibility(visibility);

    for (int i = 0, size = (mDrawableMountItems == null) ? 0 : mDrawableMountItems.size();
        i < size;
        i++) {
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

  protected void refreshAccessibilityDelegatesIfNeeded(boolean isAccessibilityEnabled) {
    if (isAccessibilityEnabled == mIsComponentAccessibilityDelegateSet) {
      return;
    }

    if (isAccessibilityEnabled && mComponentAccessibilityDelegate == null) {
      mComponentAccessibilityDelegate =
          new ComponentAccessibilityDelegate(
              this, this.isFocusable(), ViewCompat.getImportantForAccessibility(this));
    }

    ViewCompat.setAccessibilityDelegate(
        this, isAccessibilityEnabled ? mComponentAccessibilityDelegate : null);
    mIsComponentAccessibilityDelegateSet = isAccessibilityEnabled;

    if (!isAccessibilityEnabled) {
      return;
    }

    for (int i = 0, size = getChildCount(); i < size; i++) {
      final View child = getChildAt(i);
      if (child instanceof ComponentHost) {
        ((ComponentHost) child).refreshAccessibilityDelegatesIfNeeded(true);
      } else {
        final NodeInfo nodeInfo = (NodeInfo) child.getTag(R.id.component_node_info);
        if (nodeInfo != null) {
          ViewCompat.setAccessibilityDelegate(
              child,
              new ComponentAccessibilityDelegate(
                  child,
                  nodeInfo,
                  child.isFocusable(),
                  ViewCompat.getImportantForAccessibility(child)));
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

  @Override
  public void setClipChildren(boolean clipChildren) {
    if (mClippingTemporaryDisabled) {
      mClippingToRestore = clipChildren;
      return;
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
      // There is no ViewGroup.getClipChildren() method on API < 18, will keep track this way
      mClipChildren = clipChildren;
    }
    super.setClipChildren(clipChildren);
  }

  @Override
  public boolean getClipChildren() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
      // There is no ViewGroup.getClipChildren() method on API < 18
      return mClipChildren;
    } else {
      return super.getClipChildren();
    }
  }

  /**
   * Temporary disables child clipping, the previous state could be restored by calling {@link
   * #restoreChildClipping()}. While clipping is disabled calling {@link #setClipChildren(boolean)}
   * would have no immediate effect, but the restored state would reflect the last set value
   */
  void temporaryDisableChildClipping() {
    if (mClippingTemporaryDisabled) {
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      mClippingToRestore = getClipChildren();
    } else {
      mClippingToRestore = mClipChildren;
    }

    // The order here is crucial, we first need to set clipping then update
    // mClippingTemporaryDisabled flag
    setClipChildren(false);

    mClippingTemporaryDisabled = true;
  }

  /**
   * Restores child clipping to the state it was in when {@link #temporaryDisableChildClipping()}
   * was called, unless there were attempts to set a new value, while the clipping was disabled,
   * then would be restored to the last set value
   */
  void restoreChildClipping() {
    if (!mClippingTemporaryDisabled) {
      return;
    }

    // The order here is crucial, we first need to update mClippingTemporaryDisabled flag then set
    // clipping
    mClippingTemporaryDisabled = false;

    setClipChildren(mClippingToRestore);
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
   * Returns the Drawables associated with this ComponentHost for animations, for example the
   * background Drawable and/or the drawable that otherwise has a transitionKey on it that has
   * caused it to be hosted in this ComponentHost.
   *
   * <p>The core purpose of exposing these drawables is so that when animating the bounds of this
   * ComponentHost, we also properly animate the bounds of its contained Drawables at the same time.
   */
  public @Nullable List<Drawable> getLinkedDrawablesForAnimation() {
    List<Drawable> drawables = null;

    for (int i = 0, size = (mDrawableMountItems == null) ? 0 : mDrawableMountItems.size();
        i < size;
        i++) {
      final MountItem mountItem = mDrawableMountItems.valueAt(i);
      if ((getLayoutOutput(mountItem).getFlags() & LayoutOutput.LAYOUT_FLAG_MATCH_HOST_BOUNDS)
          != 0) {
        if (drawables == null) {
          drawables = new ArrayList<>();
        }
        drawables.add((Drawable) mountItem.getContent());
      }
    }
    return drawables;
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
    final int viewMountItemCount = mViewMountItems == null ? 0 : mViewMountItems.size();
    for (int i = 0; i < viewMountItemCount; i++) {
      final View child = (View) mViewMountItems.valueAt(i).getContent();
      mChildDrawingOrder[index++] = indexOfChild(child);
    }

    // Draw disappearing items on top of mounted views.
    for (int i = 0, size = mDisappearingItems == null ? 0 : mDisappearingItems.size();
        i < size;
        i++) {
      final Object child = mDisappearingItems.get(i).getContent();
      if (child instanceof View) {
        mChildDrawingOrder[index++] = indexOfChild((View) child);
      }
    }

    mIsChildDrawingOrderDirty = false;
  }

  private void ensureScrapViewMountItemsArray() {
    if (mScrapViewMountItemsArray == null) {
      mScrapViewMountItemsArray = new SparseArrayCompat<>(SCRAP_ARRAY_INITIAL_SIZE);
    }
  }

  private void ensureScrapMountItemsArray() {
    if (mScrapMountItemsArray == null) {
      mScrapMountItemsArray = new SparseArrayCompat<>(SCRAP_ARRAY_INITIAL_SIZE);
    }
  }

  private void releaseScrapDataStructuresIfNeeded() {
    if (mScrapMountItemsArray != null && mScrapMountItemsArray.size() == 0) {
      mScrapMountItemsArray = null;
    }

    if (mScrapViewMountItemsArray != null && mScrapViewMountItemsArray.size() == 0) {
      mScrapViewMountItemsArray = null;
    }
  }

  private void mountDrawable(int index, MountItem mountItem, Rect bounds) {
    assertMainThread();

    ensureDrawableMountItems();
    mDrawableMountItems.put(index, mountItem);
    final Drawable drawable = (Drawable) mountItem.getContent();

    final LayoutOutput output = getLayoutOutput(mountItem);
    ComponentHostUtils.mountDrawable(
        this, drawable, bounds, output.getFlags(), output.getNodeInfo());
  }

  private void unmountDrawable(MountItem mountItem) {
    assertMainThread();

    final Drawable drawable = (Drawable) mountItem.getContent();
    drawable.setCallback(null);
    invalidate(drawable.getBounds());

    releaseScrapDataStructuresIfNeeded();
  }

  private void moveDrawableItem(MountItem item, int oldIndex, int newIndex) {
    assertMainThread();

    // When something is already present in newIndex position we need to keep track of it.
    ensureDrawableMountItems();

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
      mScrapDrawableMountItems = new SparseArrayCompat<>(SCRAP_ARRAY_INITIAL_SIZE);
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
   * Encapsulates the logic for drawing a set of views and drawables respecting their drawing order
   * withing the component host i.e. allow interleaved views and drawables to be drawn with the
   * correct z-index.
   */
  private class InterleavedDispatchDraw {

    private Canvas mCanvas;
    private int mDrawIndex;
    private int mItemsToDraw;

    private InterleavedDispatchDraw() {}

    private void start(Canvas canvas) {
      mCanvas = canvas;
      mDrawIndex = 0;
      mItemsToDraw = mMountItems == null ? 0 : mMountItems.size();
    }

    private boolean isRunning() {
      return (mCanvas != null && mDrawIndex < mItemsToDraw);
    }

    private void drawNext() {
      if (mCanvas == null) {
        return;
      }

      for (int i = mDrawIndex, size = (mMountItems == null) ? 0 : mMountItems.size();
          i < size;
          i++) {
        final MountItem mountItem = mMountItems.valueAt(i);
        final Object content = mountItem.getContent();

        // During a ViewGroup's dispatchDraw() call with children drawing order enabled,
        // getChildDrawingOrder() will be called before each child view is drawn. This
        // method will only draw the drawables "between" the child views and the let
        // the host draw its children as usual. This is why views are skipped here.
        if (content instanceof View) {
          mDrawIndex = i + 1;
          return;
        }

        if (!mountItem.isBound()) {
          continue;
        }

        final boolean isTracing = ComponentsSystrace.isTracing();
        if (isTracing) {
          ComponentsSystrace.beginSection("draw: " + getMountItemName(mountItem));
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

  private static String getMountItemName(MountItem mountItem) {
    return getLayoutOutput(mountItem).getComponent().getSimpleName();
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
      } else if (!getContentDescriptions().isEmpty()) {
        contentDesc = TextUtils.join(", ", getContentDescriptions());
      } else if (!getTextContent().getTextItems().isEmpty()) {
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
