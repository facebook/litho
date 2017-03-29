/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;

/**
 * Created by pasqualea on 2/20/17.
 */
public interface BinderOperations<V extends ViewGroup> {
  /**
   * Notifies this Binder that the data has changed and all the {@link ComponentTree} need to be
   * regenerated.
   * This is very inefficient and the more granular notify methods should be preferred instead.
   */
  void notifyDataSetChanged();

  /**
   * Notifies this Binder that the {@link ComponentTree} representing the item with index position
   * needs to be regenerated as the data changed.
   */
  void notifyItemChanged(int position);

  /**
   * Notifies this Binder that the {@link ComponentTree}s representing the items with index starting
   * at position and ending at position + itemCount need to be regenerated as the data changed.
   */
  void notifyItemRangeChanged(int positionStart, int itemCount);

  /**
   * Notifies this Binder that an item was inserted at index position. If an item had index position
   * before notifyItemInserted was called it is assumed that it will have index position + 1 after
   * calling this method.
   */
  void notifyItemInserted(int position);

  /**
   * Notifies this Binder that a range of items were inserted at index position.
   * If an item had index position before notifyItemRangeInserted was called it is assumed that it
   * will have index position + itemCount after calling this method.
   */
  void notifyItemRangeInserted(int positionStart, int itemCount);

  /**
   * Notifies this Binder that an item moved from index fromPosition to index toPosition.
   */
  void notifyItemMoved(int fromPosition, int toPosition);

  /**
   * Notifies this Binder that an item was removed from index position. The items at index
   * position + i (with i >= 1) before notifyItemRemoved was called will have index position + i - 1
   * after calling this method.
   */
  void notifyItemRemoved(int position);

  /**
   * Notifies this Binder that a range of items were removed at index position.
   * The items at index position + itemCount + i (with i >= 0) before notifyRangeRemoved was called
   * will have index position + i after calling this method.
   */
  void notifyItemRangeRemoved(int positionStart, int itemCount);

  /**
   * Called before the {@link View} is mounted.
   *
   * @param view The {@link View} to mount.
   */
  abstract void onMount(V view);

  /**
   * Called when the {@link View} bound to this {@link Binder} has been laid out.
   *
   * @param view The {@link View} to bind to.
   */
  abstract void onBind(V view);

  /**
   * Called when this {@link Binder} is unbound from a {@link View}.
   *
   * @param view The {@link View} to unbind from.
   */
  abstract void onUnbind(V view);

  /**
   * Called when this {@link Binder} is unmounted from a {@link View}.
   *
   * @param view The {@link View} to unmount from.
   */
  abstract void onUnmount(V view);

  /**
   * Called when the bounds are defined for this binder.
   */
  abstract void onBoundsDefined();

  /**
   * Release all the componentTrees in this binder if is not going to be used anymore.
   */
  void release();

  /**
   * Depending on the returning value, this binder will try or not to measure the components
   * in a background thread.
   *
   * @return True to enable Async Layout, False otherwise.
   */
  abstract boolean isAsyncLayoutEnabled();

  /**
   * Whether or not incremental should be enabled on components
   * created by this {@link Binder}.
   */
  boolean isIncrementalMountEnabled();

  /**
   * Create a {@link Component} for the given position.
   *
   *
   * @param c An Android context for creating components
   * @param position Position of the {@link Component} that it's required.
   * @return The {@link Component} at the given position
   */
  Component<?> createComponent(ComponentContext c, int position);
}
