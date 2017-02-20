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
public abstract class Binder<V extends ViewGroup> {

  private V mView;

  /**
   * Notifies this Binder that the data has changed and all the {@link ComponentTree} need to be
   * regenerated.
   * This is very inefficient and the more granular notify methods should be preferred instead.
   */
  public abstract void notifyDataSetChanged();

  /**
   * Notifies this Binder that the {@link ComponentTree} representing the item with index position
   * needs to be regenerated as the data changed.
   */
  public abstract void notifyItemChanged(int position);

  /**
   * Notifies this Binder that the {@link ComponentTree}s representing the items with index starting
   * at position and ending at position + itemCount need to be regenerated as the data changed.
   */
  public abstract void notifyItemRangeChanged(int positionStart, int itemCount);

  /**
   * Notifies this Binder that an item was inserted at index position. If an item had index position
   * before notifyItemInserted was called it is assumed that it will have index position + 1 after
   * calling this method.
   */
  public abstract void notifyItemInserted(int position);

  /**
   * Notifies this Binder that a range of items were inserted at index position.
   * If an item had index position before notifyItemRangeInserted was called it is assumed that it
   * will have index position + itemCount after calling this method.
   */
  public abstract void notifyItemRangeInserted(int positionStart, int itemCount);

  /**
   * Notifies this Binder that an item moved from index fromPosition to index toPosition.
   */
  public abstract void notifyItemMoved(int fromPosition, int toPosition);

  /**
   * Notifies this Binder that an item was removed from index position. The items at index
   * position + i (with i >= 1) before notifyItemRemoved was called will have index position + i - 1
   * after calling this method.
   */
  public abstract void notifyItemRemoved(int position);

  /**
   * Notifies this Binder that a range of items were removed at index position.
   * The items at index position + itemCount + i (with i >= 0) before notifyRangeRemoved was called
   * will have index position + i after calling this method.
   */
  public abstract void notifyItemRangeRemoved(int positionStart, int itemCount);

  /**
   * Set the width and height of the {@link View} that will be passed to the subsequent
   * {@link #mount(ViewGroup)}, {@link #bind(ViewGroup)} and {@link #unmount(ViewGroup)} calls.
   * Can be called by any thread.
   *
   * @param width Usually the view width minus horizontal padding.
   * @param height Usually the view height minus vertical padding.
   */
  public abstract void setSize(int width, int height);

  /**
   * Release all the componentTrees in this binder if is not going to be used anymore.
   */
  public abstract void release();

  /**
   * Returns the component at the given position in the binder.
   */
  public abstract ComponentTree getComponentAt(int position);

  /**
   * Create a {@link Component} for the given position.
   *
   * TODO: 9924056 figure out a way to make this protected again
   *
   * @param c An Android context for creating components
   * @param position Position of the {@link Component} that it's required.
   * @return The {@link Component} at the given position
   */
  public abstract Component<?> createComponent(ComponentContext c, int position);

  /**
   * Called when the bounds are defined for this binder.
   */
  // TODO(12986103): Remove onBoundsDefined once the experiment proved to be ok.
  protected abstract void onBoundsDefined();

  /**
   * Called before the {@link View} is mounted.
   *
   * @param view The {@link View} to mount.
   */
  protected abstract void onMount(V view);

  /**
   * Called when the {@link View} bound to this {@link Binder} has been laid out.
   *
   * @param view The {@link View} to bind to.
   */
  protected abstract void onBind(V view);

  /**
   * Called when this {@link Binder} is unbound from a {@link View}.
   *
   * @param view The {@link View} to unbind from.
   */
  protected abstract void onUnbind(V view);

  /**
   * Called when this {@link Binder} is unmounted from a {@link View}.
   *
   * @param view The {@link View} to unmount from.
   */
  protected abstract void onUnmount(V view);

  /**
   * Depending on the returning value, this binder will try or not to measure the components
   * in a background thread.
   *
   * @return True to enable Async Layout, False otherwise.
   */
  protected abstract boolean isAsyncLayoutEnabled();

  /**
   * Whether or not incremental should be enabled on components
   * created by this {@link Binder}.
   */
  protected boolean isIncrementalMountEnabled() {
    return false;
  }

  /**
   * Call this method before the {@link View} is mounted, i.e. within
   * {@link com.facebook.components.ComponentLifecycle#onMount(Context, Object, Component)}
   */
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

  /**
   * Call this method when the view is unbound.
   * @param view the view being unbound.
   */
  public final void unbind(V view) {
    assertMainThread();
    if (view != mView) {
      return;
    }

    onUnbind(mView);
  }

  /**
   * Call this method when the view is unmounted.
   * @param view the view being unmounted.
   */
  public final void unmount(V view) {
    assertMainThread();

    if (view != mView) {
      // This binder has already been mounted on another view.
      return;
    }

    onUnmount(mView);

    mView = null;
  }
}
