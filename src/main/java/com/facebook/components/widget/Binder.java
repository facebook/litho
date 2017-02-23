// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentTree;

import static com.facebook.components.ThreadUtils.assertMainThread;

/**
 * This binder class is used to asynchronously layout Components given a list of
 * {@link Component} and attaching them to a {@link ViewGroup} through the
 * {@link #bind(ViewGroup)} method.
 */
public interface Binder<V extends ViewGroup> {


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
   * Set the width and height of the {@link View} that will be passed to the subsequent
   * {@link #mount(ViewGroup)}, {@link #bind(ViewGroup)} and {@link #unmount(ViewGroup)} calls.
   * Can be called by any thread.
   *
   * @param width Usually the view width minus horizontal padding.
   * @param height Usually the view height minus vertical padding.
   */
  void setSize(int width, int height);

  /**
   * Release all the componentTrees in this binder if is not going to be used anymore.
   */
  void release();

  /**
   * Returns the component at the given position in the binder.
   */
  ComponentTree getComponentAt(int position);

  /**
   * Create a {@link Component} for the given position.
   *
   * TODO: 9924056 figure out a way to make this protected again
   *
   * @param c An Android context for creating components
   * @param position Position of the {@link Component} that it's required.
   * @return The {@link Component} at the given position
   */
  Component<?> createComponent(ComponentContext c, int position);

  /**
   * Called when the bounds are defined for this binder.
   */
  // TODO(12986103): Remove onBoundsDefined once the experiment proved to be ok.
  abstract void onBoundsDefined();

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
   * Call this method before the {@link View} is mounted, i.e. within
   * {@link com.facebook.components.ComponentLifecycle#onMount(Context, Object, Component)}
   */
  abstract void mount(V view);

  /**
   * Bind this {@link Binder} to a {@link View}. Remember to call
   * {@link #notifyDataSetChanged()} when your {@link Component}s are
   * ready to be used.
   */
  abstract void bind(V view);

  /**
   * Call this method when the view is unbound.
   * @param view the view being unbound.
   */
  abstract void unbind(V view);

  /**
   * Call this method when the view is unmounted.
   * @param view the view being unmounted.
   */
  abstract void unmount(V view);
}
