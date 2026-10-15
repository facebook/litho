/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.rendercore.simplelist;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderCoreSystrace;
import com.facebook.rendercore.RenderResultFuture;
import com.facebook.rendercore.RenderTreeHost;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SimpleListAdapter<T extends Node>
    extends RecyclerView.Adapter<SimpleListAdapter.SimpleViewHolder<ListItem<T>>> {

  private int mMeasuredWidth;
  private int mMeasuredHeight;

  public interface ChildLayoutParamsProvider<T extends Node> {
    @Nullable
    ViewGroup.LayoutParams getChildLayoutParams(
        RecyclerView.LayoutManager layoutManager,
        ListItem<T> item,
        int measuredWidth,
        int measuredHeight);
  }

  public interface AreContentsSameDiffingCallback<T extends Node> {
    boolean areContentsTheSame(ListItem<T> oldItem, ListItem<T> newItem);
  }

  private static final int UNSET = -1;

  private final @Nullable ChildLayoutParamsProvider<T> mChildLayoutParamsProvider;

  private List<ListItem<T>> mItems = new ArrayList<>();
  private @Nullable RecyclerView mRecyclerView;

  public SimpleListAdapter() {
    this(null);
  }

  public SimpleListAdapter(final @Nullable ChildLayoutParamsProvider<T> childLayoutParamsProvider) {
    mChildLayoutParamsProvider = childLayoutParamsProvider;
    setHasStableIds(true);
  }

  void clearLayoutData() {
    mItems = Collections.emptyList();
  }

  public void setLayoutData(ListLayoutData layoutData) {
    final boolean heightChanged = (mMeasuredHeight != layoutData.measuredHeight);
    final boolean widthChanged = (mMeasuredWidth != layoutData.measuredWidth);
    boolean nonScrollingSizeChanged = false;

    @Nullable RecyclerView.LayoutManager layoutManager = getLayoutManager();

    if (layoutManager != null) {
      nonScrollingSizeChanged =
          ((layoutManager.canScrollHorizontally() && heightChanged)
              || (layoutManager.canScrollVertically() && widthChanged));
    }

    mMeasuredWidth = layoutData.measuredWidth;
    mMeasuredHeight = layoutData.measuredHeight;
    final List<ListItem<T>> oldData = mItems;
    mItems = layoutData.items;

    if (nonScrollingSizeChanged) {
      notifyDataSetChanged();
    } else {
      RenderCoreSystrace.beginSection("diffingItems");
      final DiffUtil.DiffResult diffingResult =
          DiffUtil.calculateDiff(
              new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                  return oldData.size();
                }

                @Override
                public int getNewListSize() {
                  return mItems.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                  return oldData.get(oldItemPosition).getId()
                      == mItems.get(newItemPosition).getId();
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                  final ListItem<T> oldItem = oldData.get(oldItemPosition);
                  final ListItem<T> newItem = mItems.get(newItemPosition);
                  @Nullable
                  final AreContentsSameDiffingCallback<T> callback =
                      getAreContentsSameDiffingCallback();
                  if (callback != null) {
                    return callback.areContentsTheSame(oldItem, newItem);
                  }
                  return oldItem.isEquivalentTo(newItem);
                }
              });
      RenderCoreSystrace.endSection();
      diffingResult.dispatchUpdatesTo(this);
    }
  }

  @Nullable
  private RecyclerView.LayoutManager getLayoutManager() {
    if (mRecyclerView != null) {
      return mRecyclerView.getLayoutManager();
    }

    return null;
  }

  protected abstract RenderTreeHost createRootHost(Context c);

  @Override
  public long getItemId(int position) {
    return mItems.get(position).getId();
  }

  @Override
  public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    RenderTreeHost rootView = createRootHost(parent.getContext());
    return new SimpleViewHolder((View) rootView);
  }

  @Override
  public void onBindViewHolder(SimpleViewHolder viewHolder, int i) {
    ViewGroup.LayoutParams overrideLayoutParams = null;
    @Nullable RecyclerView.LayoutManager layoutManager = getLayoutManager();
    if (layoutManager != null) {
      if (mChildLayoutParamsProvider != null && mMeasuredWidth != UNSET) {
        overrideLayoutParams =
            mChildLayoutParamsProvider.getChildLayoutParams(
                layoutManager, mItems.get(i), mMeasuredWidth, mMeasuredHeight);
        if (overrideLayoutParams != null) {
          viewHolder.itemView.setLayoutParams(overrideLayoutParams);
        }
      }
      if (viewHolder.hasOverriddenLayoutParams && overrideLayoutParams == null) {
        viewHolder.itemView.setLayoutParams(layoutManager.generateDefaultLayoutParams());
      }
    }
    viewHolder.hasOverriddenLayoutParams = overrideLayoutParams != null;

    final ListItem<T> item = mItems.get(i);

    final RenderResultFuture<?, ?> renderResultFuture = item.getRenderResultFuture();
    ((RenderTreeHost) viewHolder.itemView)
        .setMountInput(renderResultFuture.runAndGet().getMountInput());
    viewHolder.item = mItems.get(i);
  }

  @Override
  public void onViewRecycled(SimpleViewHolder holder) {
    super.onViewRecycled(holder);
    final RenderTreeHost rootView = (RenderTreeHost) holder.itemView;
    rootView.setMountInput(null);
    holder.item = null;
  }

  @Override
  public int getItemCount() {
    return mItems.size();
  }

  @Override
  public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    mRecyclerView = recyclerView;
  }

  @Override
  public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
    mRecyclerView = null;
  }

  public List<ListItem<T>> getItems() {
    return mItems;
  }

  protected @Nullable AreContentsSameDiffingCallback<T> getAreContentsSameDiffingCallback() {
    return null;
  }

  public static class SimpleViewHolder<T> extends RecyclerView.ViewHolder {

    private boolean hasOverriddenLayoutParams = false;
    public T item;

    public SimpleViewHolder(View itemView) {
      super(itemView);
    }
  }

  protected int getMeasuredHeight() {
    return mMeasuredHeight;
  }

  protected int getMeasuredWidth() {
    return mMeasuredWidth;
  }
}
