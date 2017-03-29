/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentView;

import static android.view.ViewGroup.LayoutParams;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static com.facebook.litho.SizeSpec.getMode;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;

/**
 * A component binder for {@link RecyclerView}.
 */
public abstract class RecyclerComponentBinder<L extends RecyclerView.LayoutManager,
    R extends RecyclerComponentBinder.RecyclerComponentWorkingRangeController>
    extends BaseBinder<RecyclerView, R> {

  private final L mLayoutManager;
  private final InternalAdapter mAdapter;
  private final RecyclerView.OnScrollListener mOnScrollListener;

  private RecyclerView mRecyclerView;

  public RecyclerComponentBinder(Context context, L layoutManager, R rangeController) {
    this(context, layoutManager, rangeController, null);
  }

  public RecyclerComponentBinder(
      Context context,
      L layoutManager,
      R rangeController,
      Looper layoutLooper) {
    super(context, layoutLooper, rangeController);

    mLayoutManager = layoutManager;
    mAdapter = new InternalAdapter(context, this);
    mOnScrollListener = new InternalOnScrollListener(this);

    setListener(mAdapter);
  }

  /**
   * Enables sticky header if the binder implements {@link HasStickyHeader} interface
   */
  private void maybeEnableStickyHeader() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      // Sticky header needs some APIs like 'setTranslationY()' which are not available Gingerbread
      // and below
      return;
    }
    if (mRecyclerView == null) {
      return;
    }
    RecyclerViewWrapper recyclerViewWrapper = RecyclerViewWrapper.getParentWrapper(mRecyclerView);
    if (recyclerViewWrapper == null) {
      return;
    }
    if (this instanceof HasStickyHeader) {
      RecyclerView.OnScrollListener onScrollListener =
          new StickyHeaderAwareScrollListener(
              new ComponentContext(mRecyclerView.getContext()),
              this,
              recyclerViewWrapper);
      mRecyclerView.addOnScrollListener(onScrollListener);
    }
  }

  /**
   * Return the stable ID for the component at <code>position</code>. If {@link #hasStableIds()}
   * would return false this method should return {@link RecyclerView#NO_ID}. The default
   * implementation of this method returns {@link RecyclerView#NO_ID}.
   *
   * @param position Binder position to query
   * @return the stable ID of the component at position
   */
  public long getComponentId(int position) {
    return RecyclerView.NO_ID;
  }

  /**
   * Indicates whether each component in the data set can be represented with a unique identifier of
   * type {@link java.lang.Long}.
   *
   * @param hasStableIds Whether components in data set have unique identifiers or not.
   * @see #hasStableIds()
   * @see #getComponentId(int)
   */
  public void setHasStableIds(boolean hasStableIds) {
    mAdapter.setHasStableIds(hasStableIds);
  }

  /**
   * Returns true if this binder publishes a unique <code>long</code> value that can act as a key
   * for the component at a given position in the data set. If that component is relocated in the
   * data set, the ID returned for that component should be the same.
   *
   * @return true if this binder's component have stable IDs
   */
  public final boolean hasStableIds() {
    return mAdapter.hasStableIds();
  }

  // TODO(12986103): Remove onBoundsDefined once the experiment proved to be ok.
  @Override
  public void onBoundsDefined() {
    updateRange(0, getCount(), URFLAG_REFRESH_IN_RANGE | URFLAG_RELEASE_OUTSIDE_RANGE);
  }

  @Override
  public void onMount(RecyclerView recyclerView) {
    mRecyclerView = recyclerView;

    if (recyclerView.getLayoutManager() == null) {
      recyclerView.setLayoutManager(mLayoutManager);
    } else if (recyclerView.getLayoutManager() != mLayoutManager) {
      throw new IllegalStateException("The LayoutManager used in the Binder constructor must be " +
          "the same one assigned to the RecyclerView associated to that Binder.");
    }

    recyclerView.setAdapter(mAdapter);
    recyclerView.addOnScrollListener(mOnScrollListener);

    maybeEnableStickyHeader();
  }

  @Override
  public void onBind(RecyclerView recyclerView) {
  }

  @Override
  public void onUnbind(RecyclerView recyclerView) {
  }

  @Override
  public void onUnmount(RecyclerView recyclerView) {
    mRecyclerView.clearOnScrollListeners();
    mRecyclerView.setLayoutManager(null);
    mRecyclerView.setAdapter(null);
    mRecyclerView = null;
  }

  protected L getLayoutManager() {
    return mLayoutManager;
  }

  protected void onScrolled(RecyclerView recyclerView, int dx, int dy) {
  }

  private static class InternalAdapter
      extends Adapter<ComponentViewHolder> implements BaseBinder.Listener {

    private final Context mContext;
    private final RecyclerComponentBinder mBinder;

    InternalAdapter(Context context, RecyclerComponentBinder binder) {
      mContext = context;
      mBinder = binder;
    }

    @Override
    public ComponentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new ComponentViewHolder(new ComponentView(mContext));
    }

    @Override
    public void onBindViewHolder(ComponentViewHolder holder, int position) {
      final ComponentView componentView = (ComponentView) holder.itemView;
      final LayoutParams lp = componentView.getLayoutParams();
      final int width = getMode(mBinder.getWidthSpec(position)) == UNSPECIFIED
          ? WRAP_CONTENT
          : MATCH_PARENT;
      final int height = getMode(mBinder.getHeightSpec(position)) == UNSPECIFIED
          ? WRAP_CONTENT
          : MATCH_PARENT;
      if (lp != null) {
        lp.width = width;
        lp.height = height;
      } else {
        componentView.setLayoutParams(new RecyclerView.LayoutParams(width, height));
      }
      componentView.setComponent(mBinder.getComponentAt(position));
    }

    @Override
    public int getItemCount() {
      return mBinder.getCount();
    }

    @Override
    public long getItemId(int position) {
      return mBinder.getComponentId(position);
    }

    @Override
    public void onDataSetChanged() {
      notifyDataSetChanged();
    }

    @Override
    public void onItemInserted(int position) {
      notifyItemInserted(position);
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
      notifyItemRangeInserted(positionStart, itemCount);
    }

    @Override
    public void onItemChanged(int position) {
      notifyItemChanged(position);
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
      notifyItemRangeChanged(positionStart, itemCount);
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
      notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemRemoved(int position) {
      notifyItemRemoved(position);
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
      notifyItemRangeRemoved(positionStart, itemCount);
    }
  }

  private static class ComponentViewHolder extends RecyclerView.ViewHolder {

    public ComponentViewHolder(View view) {
      super(view);
    }
  }

  private static class InternalOnScrollListener extends RecyclerView.OnScrollListener {

    private final RecyclerComponentBinder mRecyclerComponentBinder;

    InternalOnScrollListener(RecyclerComponentBinder recyclerComponentBinder) {
      mRecyclerComponentBinder = recyclerComponentBinder;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
      mRecyclerComponentBinder.onScrolled(recyclerView, dx, dy);
    }
  }

  /**
   * The default, static WorkingRangeController for the {@link RecyclerComponentBinder}. Any custom
   * WorkingRangeControllers for subclasses of the {@link RecyclerComponentBinder} must extend this
   * class.
   */
  public static class RecyclerComponentWorkingRangeController extends WorkingRangeController {

    /**
     * The size of a range measured in viewports (i.e. the range will consist of
     * RANGE_SIZE * N rows/columns, where N is the number of rows/columns that are currently
     * visible).
     */
    static final int RANGE_SIZE = 3;
    private static final int EMPTY_INDEX = -1;

    private int mPreviousFirstVisiblePosition = EMPTY_INDEX;
    private int mPreviousVisibleItemCount = 0;

    /**
     * Method called after each scroll event. It decides whether or not the visible range has
     * changed since the last time it was called and if it should update the working range.
     * @param currentFirstVisiblePosition the index of the first visible item
     * @param currentVisibleItemCount the number of visible items
     */
    public void notifyOnScroll(int currentFirstVisiblePosition, int currentVisibleItemCount) {
      validateCurrentPosition(currentFirstVisiblePosition, currentVisibleItemCount);

      // Do not take any action if the same items are visible on the screen.
      if (mPreviousFirstVisiblePosition == currentFirstVisiblePosition &&
          mPreviousVisibleItemCount == currentVisibleItemCount) {
        return;
      }

      mPreviousFirstVisiblePosition = currentFirstVisiblePosition;
      mPreviousVisibleItemCount = currentVisibleItemCount;

      // Determine the number of items that should be included in the range before and after the
      // view port. We will load RANGE_SIZE * currentVisibleItemCount items in total, including
      // the ones within the view port. This means that (RANGE_SIZE-1) * currentVisibleItemCount
      // items, besides the ones within the view port, will be included in the range. They will be
      // split equally before and after the view port.
      final int notVisibleItemsToPrepareCount = (RANGE_SIZE - 1) * currentVisibleItemCount;
      final int rangeItemsBeforeViewPortCount = notVisibleItemsToPrepareCount / 2;
      final int rangeItemsAfterViewPortCount = notVisibleItemsToPrepareCount -
          rangeItemsBeforeViewPortCount;

      // Set range position.
      final int start = Math.max(0, currentFirstVisiblePosition - rangeItemsBeforeViewPortCount);
      final int end = Math.min(
          currentFirstVisiblePosition + (currentVisibleItemCount - 1) +
              rangeItemsAfterViewPortCount,
          getBinder().getCount() - 1);
      final int count = end - start + 1;

      updateWorkingRange(start, count);
    }

    private void validateCurrentPosition(int firstVisiblePosition, int itemCount) {
      if (firstVisiblePosition < 0 || firstVisiblePosition >= getBinder().getCount()) {
        throw new IllegalStateException(firstVisiblePosition + " is not a valid value for the " +
            "first visible item position.");
      }

      if (itemCount < 0 || itemCount > getBinder().getCount()) {
        throw new IllegalStateException(itemCount + " is not a valid value for the item count.");
      }
    }
  }

  /**
   * This NoOp WorkingRangeController, as the name suggests, is not operating on the range leaving
   * whatever initialized, untouched.
   */
  public static class NoOpRecyclerComponentWorkingRangeController
      extends RecyclerComponentWorkingRangeController {

    @Override
    public void notifyOnScroll(int currentFirstVisiblePosition, int currentVisibleItemCount) {
      // no-op.
    }
  }
}
