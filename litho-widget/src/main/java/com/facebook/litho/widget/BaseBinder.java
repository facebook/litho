/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Looper;
import android.support.v4.util.Pools;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.infer.annotation.ThreadSafe;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.ThreadUtils.assertDoesntHoldLock;
import static com.facebook.litho.ThreadUtils.assertMainThread;

public abstract class BaseBinder<
    V extends ViewGroup,
    R extends WorkingRangeController> implements Binder<V>, BinderOperations<V> {

  private static final Pools.SynchronizedPool<List> sListPool =
      new Pools.SynchronizedPool<>(8);

  protected static final int URFLAG_REFRESH_IN_RANGE = 0x1;
  protected static final int URFLAG_RELEASE_OUTSIDE_RANGE = 0x2;

  protected interface Listener {
    void onDataSetChanged();

    void onItemInserted(int position);
    void onItemRangeInserted(int positionStart, int itemCount);

    void onItemChanged(int position);
    void onItemRangeChanged(int positionStart, int itemCount);

    void onItemMoved(int fromPosition, int toPosition);

    void onItemRemoved(int position);
    void onItemRangeRemoved(int positionStart, int itemCount);
  }

  private final ComponentContext mContext;
  private final BinderTreeCollection mComponentTrees;
  private final Looper mLayoutLooper;

  private int mContentWidthSpec = SizeSpec.makeSizeSpec(0, UNSPECIFIED);
  private int mContentHeightSpec = SizeSpec.makeSizeSpec(0, UNSPECIFIED);
  private Listener mListener;
  private R mRangeController;
  private V mView;

  public BaseBinder(Context context, R rangeController) {
    this(context, null, rangeController);
  }

  public BaseBinder(Context context, Looper layoutLooper, R rangeController) {
    mContext = new ComponentContext(context);
    mComponentTrees = new BinderTreeCollection();

    setRangeController(rangeController);

    mLayoutLooper = layoutLooper;

    if (mLayoutLooper != null && mLayoutLooper == Looper.getMainLooper()) {
      throw new IllegalStateException("If you want to compute the layout of the " +
          "Binder's elements in the Main Thread you shouldn't set the MainLooper here but" +
          "override isAsyncLayoutEnabled() and return false.");
    }
  }

  @Override
  public void measure(Size outSize, int widthSpec, int heightSpec) {
    throw new IllegalStateException("Recycler must have sizes spec set " +
        "when using the old binder implementation.");
  }

  @Override
  public final void notifyDataSetChanged() {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    // TODO(12986103): Once the experiment proved to be ok, remove the updateRange code.
    if (ComponentsConfiguration.bootstrapBinderItems) {
      initializeRange(0);
    } else {
      updateRange(0, getCount(), URFLAG_REFRESH_IN_RANGE | URFLAG_RELEASE_OUTSIDE_RANGE);
    }

    if (mListener != null) {
      mListener.onDataSetChanged();
    }
  }

  @Override
  public final void notifyItemChanged(int position) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    if (isInRange(position)) {
      updateRange(position, 1, URFLAG_REFRESH_IN_RANGE);
    }

    if (mListener != null) {
      mListener.onItemChanged(position);
    }
  }

  @Override
  public void notifyItemRangeChanged(int positionStart, int itemCount) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    if (isInRange(positionStart, itemCount)) {
      updateRange(positionStart, itemCount, URFLAG_REFRESH_IN_RANGE);
    }

    if (mListener != null) {
      mListener.onItemRangeChanged(positionStart, itemCount);
    }
  }

  @Override
  public void notifyItemInserted(int position) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    boolean shouldInsertItem = true;

    synchronized (this) {
      // Check if the inserted item is inside the range or adjacent to it.
      if (!isInRange(position) &&
          position != mComponentTrees.getFirstPosition() + mComponentTrees.size()) {
        // If the new item is inserted before the current range, than shift right the range.
        if (position < mComponentTrees.getFirstPosition()) {
          mComponentTrees.shiftAllRight(1);
        }

        shouldInsertItem = false;
      }
    }

    if (shouldInsertItem) {
      Component component = createComponent(mContext, position);

      synchronized (this) {
        // We need to check again because we exited the critical section and since the last check
        // the result might have changed.
        if (isInRange(position) ||
            position == mComponentTrees.getFirstPosition() + mComponentTrees.size()) {

          final ComponentTree componentTree = buildComponentTree(component);

          if (isAsyncLayoutEnabled()) {
            componentTree.setSizeSpecAsync(getWidthSpec(position), getHeightSpec(position));
          } else {
            componentTree.setSizeSpec(getWidthSpec(position), getHeightSpec(position));
          }

          // Move right the successive ComponentTree positions and insert the new item.
          mComponentTrees.insert(position, componentTree);
        }
      }
    }

    // There might be a componentTree outside the range now but for simplicity we just keep it,
    // it will be released in a successive updateRange call.

    if (mListener != null) {
      mListener.onItemInserted(position);
    }
  }

  @Override
  public void notifyItemRangeInserted(int positionStart, int itemCount) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    List<Component<?>> componentList;
    boolean shouldInsertItems = true;

    synchronized (this) {
      if (positionStart < mComponentTrees.getFirstPosition()) {
        // If the new items are inserted before the current range, than shift right the range.
        mComponentTrees.shiftAllRight(itemCount);

        shouldInsertItems = false;
      }

      // Do nothing if the items are inserted after the current range.
      if (positionStart > mComponentTrees.getFirstPosition() + mComponentTrees.size()) {
        shouldInsertItems = false;
      }
    }

    if (shouldInsertItems) {
      componentList = acquireList(itemCount);
      // This must remain outside the synchronized block to maintain thread safety.
      for (int i = positionStart, size = positionStart + itemCount; i < size; i++) {
        componentList.add(createComponent(mContext, i));
      }

      synchronized (this) {
        // We need to check again because we exited the synchronized block and since the last check
        // the result might have changed.
        if (positionStart >= mComponentTrees.getFirstPosition() &&
            positionStart <= mComponentTrees.getFirstPosition() + mComponentTrees.size()) {

          for (int i = 0; i < itemCount; i++) {
            final ComponentTree componentTree = buildComponentTree(componentList.get(i));

            if (isAsyncLayoutEnabled()) {
              componentTree.setSizeSpecAsync(getWidthSpec(i), getHeightSpec(i));
            } else {
              componentTree.setSizeSpec(getWidthSpec(i), getHeightSpec(i));
            }

            mComponentTrees.insert(i + positionStart, componentTree);
          }
        }
      }

      if (componentList != null) {
        releaseList(componentList);
      }
    }

    // There might be componentTrees outside the range now but for simplicity we just keep them,
    // they will be released in a successive updateRange call.

    if (mListener != null) {
      mListener.onItemRangeInserted(positionStart, itemCount);
    }
  }

  @Override
  public void notifyItemMoved(int fromPosition, int toPosition) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    Component component = createComponent(mContext, toPosition);

    synchronized (this) {
      boolean isFromInRange = isInRange(fromPosition);
      boolean isToInRange = isInRange(toPosition);

      if (!isFromInRange && !isToInRange) {
        // If the item is moved from after the range, before the range or the other way round, then
        // shift the range right, or left, accordingly.
        final int firstPosition = mComponentTrees.getFirstPosition();
        final int lastPosition = firstPosition + mComponentTrees.size() - 1;

        if (toPosition < firstPosition && fromPosition > lastPosition) {
          mComponentTrees.shiftAllRight(1);
        } else if (toPosition > lastPosition && fromPosition < firstPosition) {
          mComponentTrees.shiftAllLeft(1);
        }
      } else if (isFromInRange && !isToInRange) {
        mComponentTrees.removeShiftingLeft(fromPosition);

      } else if (!isFromInRange && isToInRange) {
        final ComponentTree componentTree = buildComponentTree(component);

        if (isAsyncLayoutEnabled()) {
          componentTree.setSizeSpecAsync(getWidthSpec(toPosition), getHeightSpec(toPosition));
        } else {
          componentTree.setSizeSpec(getWidthSpec(toPosition), getHeightSpec(toPosition));
        }

        // If fromPosition is before the range, the elements preceding toPosition need to be shifted
        // left.
        if (fromPosition < mComponentTrees.getFirstPosition()) {
          mComponentTrees.insertShiftingLeft(toPosition, componentTree);
        } else {
          mComponentTrees.insert(toPosition, componentTree);
        }

        // Both From and To positions are in range.
      } else {
        mComponentTrees.move(fromPosition, toPosition);
      }
    }

    if (mListener != null) {
      mListener.onItemMoved(fromPosition, toPosition);
    }
  }

  @Override
  public void notifyItemRemoved(int position) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    ComponentTree componentTreeToRecycle = null;

    synchronized (this) {
      // If the removed item is before the current range, then shift the range left.
      if (position < mComponentTrees.getFirstPosition()) {
        mComponentTrees.shiftAllLeft(1);
      } else if (isInRange(position)) {
        componentTreeToRecycle = mComponentTrees.get(position);
        mComponentTrees.removeShiftingLeft(position);
      }
    }

    if (componentTreeToRecycle != null) {
      componentTreeToRecycle.release();
    }

    if (mListener != null) {
      mListener.onItemRemoved(position);
    }
  }

  @Override
  public void notifyItemRangeRemoved(int positionStart, int itemCount) {
    assertMainThread();

    if (!hasContentSize()) {
      return;
    }

    boolean shouldRemoveItems = true;

    synchronized (this) {
      // If the removed items are all before the current range, then shift the range left.
      if (positionStart + itemCount - 1 < mComponentTrees.getFirstPosition()) {
        mComponentTrees.shiftAllLeft(itemCount);
        shouldRemoveItems = false;
      }
    }

    if (shouldRemoveItems) {
      List<ComponentTree> treesToRelease = acquireList(itemCount);

      synchronized (this) {
        if (isInRange(positionStart, itemCount)) {

          for (int i = positionStart, size = positionStart + itemCount; i < size; i++) {
            ComponentTree componentTree = getComponentAt(i);

            if (componentTree != null) {
              treesToRelease.add(componentTree);
            }
          }

          mComponentTrees.removeShiftingLeft(positionStart, itemCount);
        }
      }

      for (int i = 0, size = treesToRelease.size(); i < size; i++) {
        treesToRelease.get(i).release();
      }
      releaseList(treesToRelease);
    }

    if (mListener != null) {
      mListener.onItemRangeRemoved(positionStart, itemCount);
    }
  }

  @Override
  public void setSize(int width, int height) {
    final int newWidthSpec = SizeSpec.makeSizeSpec(width, EXACTLY);
    final int newHeightSpec = SizeSpec.makeSizeSpec(height, EXACTLY);

    synchronized (this) {
      if (mContentHeightSpec == newHeightSpec && mContentWidthSpec == newWidthSpec) {
        return;
      }

      mContentWidthSpec = newWidthSpec;
      mContentHeightSpec = newHeightSpec;
    }

    // TODO(12986103): Remove onBoundsDefined once the experiment proved to be ok.
    if (ComponentsConfiguration.bootstrapBinderItems) {
      initializeRange(getInitializeStartPosition());
    } else {
      onBoundsDefined();
    }
  }

  /**
   * Call this method before the {@link View} is mounted, i.e. within
   * {@link com.facebook.litho.ComponentLifecycle#onMount(Context, Object, Component)}
   */
  @Override
  public final void mount(V view) {
    assertMainThread();

    if (mView != null) {
      // If this binder is being mounted on a new view before it was unmounted from the previous
      // one, then unmount now.
      onUnmount(mView);
    }

    mView = view;

    onMount(mView);
  }

  /**
   * Bind this {@link Binder} to a {@link View}. Remember to call
   * {@link #notifyDataSetChanged()} when your {@link Component}s are
   * ready to be used.
   */
  @Override
  public final void bind(V view) {
    assertMainThread();

    if (mView != view) {
      unbind(mView);
      unmount(mView);
      mView = null;
      mount(view);
    }

    onBind(mView);
  }

  @Override
  public final void unbind(V view) {
    assertMainThread();
    if (view != mView) {
      return;
    }

    onUnbind(mView);
  }

  @Override
  public final void unmount(V view) {
    assertMainThread();

    if (view != mView) {
      // This binder has already been mounted on another view.
      return;
    }

    onUnmount(mView);

    mView = null;
  }

  /**
   * Returns the height of the {@link View} corresponding to the binder.
   */
  public int getHeight() {
    return SizeSpec.getSize(mContentHeightSpec);
  }

  /**
   * Returns the width of the {@link View} corresponding to the binder.
   */
  public int getWidth() {
    return SizeSpec.getSize(mContentWidthSpec);
  }

  @Override
  public final void release() {
    List<ComponentTree> componentTreesToRelease;

    synchronized (this) {
      componentTreesToRelease = acquireList(mComponentTrees.size());
      mComponentTrees.addAllTo(componentTreesToRelease);
      mComponentTrees.clear();
    }

    for (int i = 0, size = componentTreesToRelease.size(); i < size; i++) {
      componentTreesToRelease.get(i).release();
    }
    releaseList(componentTreesToRelease);
  }

  @Override
  public final ComponentTree getComponentAt(int position) {
    synchronized (this) {
      return mComponentTrees.get(position);
    }
  }

  @Override
  public boolean isIncrementalMountEnabled() {
    return false;
  }

  protected int getInitializeStartPosition() {
    return 0;
  }

  int getComponentPosition(ComponentTree componentTree) {
    synchronized (this) {
      return mComponentTrees.getPositionOf(componentTree);
    }
  }

  protected final void setListener(Listener listener) {
    mListener = listener;
  }

  /**
   * Returns the total number of component props in the data set hold by the binder.
   *
   * @return The total number of component props in this binder.
   */
  protected abstract int getCount();

  @Override
  public void onBoundsDefined() {
    updateRange(0, getCount(), URFLAG_REFRESH_IN_RANGE);
  }

  @Override
  public boolean isAsyncLayoutEnabled() {
    return false;
  }

  /**
   * Returns the width spec to be used for the component in the
   * given position.
   */
  protected int getWidthSpec(int position) {
    return mContentWidthSpec;
  }

  /**
   * Returns the height spec to be used for the component in the
   * given position.
   */
  protected int getHeightSpec(int position) {
    return mContentHeightSpec;
  }

  /**
   * Returns the {@link WorkingRangeController} associated with the binder.
   */
  public R getRangeController() {
    return mRangeController;
  }

  /**
   * Sets a {@link WorkingRangeController} for the binder.
   */
  public void setRangeController(R rangeController) {
    if (rangeController == null) {
      throw new IllegalStateException("The range controller should not be null.");
    }

    if (mRangeController != null) {
      mRangeController.setBinder(null);
    }

    mRangeController = rangeController;
    mRangeController.setBinder(this);
  }

  /**
   * Update the range of ComponentTrees held by this binder.
   *
   * @param start Starting position of the new range.
   * @param count Item count of the new range.
   * @param flags {@link #URFLAG_REFRESH_IN_RANGE} to refresh existing componentTrees
   *              still in range. {@link #URFLAG_RELEASE_OUTSIDE_RANGE} to release existing
   *              componentTrees outside the new range.
   */
  protected void updateRange(int start, int count, int flags) {
    assertDoesntHoldLock(this);

    final boolean isRefreshingInRange = (flags & URFLAG_REFRESH_IN_RANGE) != 0;
    final boolean isReleasingOutsideRange = (flags & URFLAG_RELEASE_OUTSIDE_RANGE) != 0;
    final List<ComponentTree> componentTreesToRelease = acquireList(getCount());
    final List<Component<?>> componentList = acquireList(getCount());

    // This must remain outside the synchronized block to maintain thread safety
    for (int i = start, size = start + count; i < size; i++) {
      componentList.add(createComponent(mContext, i));
    }

    synchronized (this) {
      final int currentStart = mComponentTrees.getFirstPosition();
      final int currentCount = mComponentTrees.size();
      final int minPositionToProcess = Math.max(0, Math.min(start, currentStart));
      final int maxPositionToProcess = Math.max(start + count, currentStart + currentCount);

      for (int i = minPositionToProcess; i < maxPositionToProcess; i++) {
        ComponentTree componentTree = mComponentTrees.get(i);

        // ComponentTree outside the range: check if it needs to be released.
        if (componentTree != null
            && isReleasingOutsideRange
            && (i < start || i >= start + count)) {
          componentTreesToRelease.add(componentTree);
          mComponentTrees.remove(i);

          // If we are within the new range.
        } else if (i >= start && i < start + count) {
          final Component<?> component = componentList.get(i - start);

          if (componentTree == null) {
            // Create a new ComponentTree if we don't have it at the given position.
            componentTree = buildComponentTree(component);
            if (isAsyncLayoutEnabled()) {
              componentTree.setSizeSpecAsync(getWidthSpec(i), getHeightSpec(i));
            } else {
              componentTree.setSizeSpec(getWidthSpec(i), getHeightSpec(i));
            }

            mComponentTrees.put(i, componentTree);

          } else if (isRefreshingInRange) {
            // Or just change component in the ComponentTree we already have.
            if (isAsyncLayoutEnabled()) {
              componentTree.setRootAndSizeSpecAsync(
                  component,
                  getWidthSpec(i),
                  getHeightSpec(i));
            } else {
              componentTree.setRootAndSizeSpec(
                  component,
                  getWidthSpec(i),
                  getHeightSpec(i));
            }
          }
        }
      }
    }
    releaseList(componentList);

    for (int i = 0, size = componentTreesToRelease.size(); i < size; i++) {
      componentTreesToRelease.get(i).release();
    }
    releaseList(componentTreesToRelease);
  }

  private ComponentTree buildComponentTree(Component<?> component) {
    return ComponentTree.create(mContext, component)
        .incrementalMount(isIncrementalMountEnabled())
        .layoutThreadLooper(mLayoutLooper)
        .build();
  }

