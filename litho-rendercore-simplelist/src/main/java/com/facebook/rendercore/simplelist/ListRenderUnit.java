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

import static com.facebook.rendercore.RenderUnit.DelegateBinder.createDelegateBinder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Parcelable;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.ContentAllocator;
import com.facebook.rendercore.RenderType;
import com.facebook.rendercore.RenderUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ListRenderUnit extends RenderUnit<RCRecyclerView>
    implements ContentAllocator<RCRecyclerView> {

  public enum LayoutType {
    LIST,
    GRID,
    STAGGERED_GRID
  }

  public abstract static class LayoutManagerConfig {
    private final @RecyclerView.Orientation int mOrientation;

    public LayoutManagerConfig(@RecyclerView.Orientation int orientation) {
      mOrientation = orientation;
    }

    @RecyclerView.Orientation
    public int getOrientation() {
      return mOrientation;
    }

    protected abstract boolean isEquivalentTo(LayoutManagerConfig other);

    protected abstract RecyclerView.LayoutManager getLayoutManager(Context context);
  }

  private final long mId;
  private final LayoutManagerConfig mLayoutManagerConfig;
  private final SimpleListAdapter mAdapter;
  private @Nullable List<RecyclerView.OnScrollListener> mOnScrollListeners;
  private @Nullable SnapHelper mSnapHelper;
  private @Nullable List<RecyclerView.ItemDecoration> mItemDecorations;
  private @Nullable RecyclerView.ItemAnimator mItemAnimator;
  private @ColorInt int mBackgroundColor = Color.TRANSPARENT;
  private int mOverScrollMode = View.OVER_SCROLL_ALWAYS;

  private @Nullable ListState mListState;
  private int mStartingIndex;
  private int mFadingEdgeLength;
  private @Nullable Rect mPadding;
  private boolean mClipToPadding = true;
  private boolean mIsScrollEnabled = true;
  private boolean mIsNestedScrollEnabled = true;
  private boolean mScrollBarEnabled = true;

  public ListRenderUnit(
      final long id,
      final SimpleListAdapter adapter,
      final LayoutManagerConfig layoutManagerConfig) {
    this(id, adapter, layoutManagerConfig, false);
  }

  public ListRenderUnit(
      final long id,
      final SimpleListAdapter adapter,
      final LayoutManagerConfig layoutManagerConfig,
      final boolean excludeLayoutDataBinder) {
    super();
    mId = id;
    mAdapter = adapter;
    mLayoutManagerConfig = layoutManagerConfig;

    addOptionalMountBinders(
        createDelegateBinder(this, sLayoutManagerBinder),
        createDelegateBinder(this, sAdapterBinder),
        createDelegateBinder(this, sScrollListenersBinder),
        createDelegateBinder(this, sSnapHelperBinder),
        createDelegateBinder(this, sItemDecorationBinder),
        createDelegateBinder(this, sItemAnimatorBinder));
    if (!excludeLayoutDataBinder) {
      addOptionalMountBinder(createDelegateBinder(this, sLayoutDataBinder));
    }
    addOptionalMountBinders(
        createDelegateBinder(this, sBackgroundColorBinder),
        createDelegateBinder(this, sOverscrollBinder),
        createDelegateBinder(this, sFadingEdgeBinder),
        createDelegateBinder(this, sPaddingBinder),
        createDelegateBinder(this, sClipToPaddingBinder),
        createDelegateBinder(this, sIsScrollEnabledBinder),
        createDelegateBinder(this, sNestedIsScrollEnabledBinder),
        createDelegateBinder(this, sScrollBarEnabledBinder));
  }

  @Override
  public RCRecyclerView createContent(final Context c) {
    return new RCRecyclerView(c);
  }

  @Override
  public ContentAllocator<RCRecyclerView> getContentAllocator() {
    return this;
  }

  @Override
  public RenderType getRenderType() {
    return RenderType.VIEW;
  }

  @Override
  public long getId() {
    return mId;
  }

  @Override
  public boolean doesMountRenderTreeHosts() {
    return true;
  }

  public void addOnScrollListener(final RecyclerView.OnScrollListener listener) {
    if (mOnScrollListeners == null) {
      mOnScrollListeners = new ArrayList<>(4);
    }
    mOnScrollListeners.add(listener);
  }

  public void setSnapHelper(final @Nullable SnapHelper snapHelper) {
    mSnapHelper = snapHelper;
  }

  public void addItemDecoration(final RecyclerView.ItemDecoration itemDecoration) {
    if (mItemDecorations == null) {
      mItemDecorations = new ArrayList<>();
    }
    mItemDecorations.add(itemDecoration);
  }

  public void setItemAnimator(final @Nullable RecyclerView.ItemAnimator itemAnimator) {
    mItemAnimator = itemAnimator;
  }

  public void setBackgroundColor(final int backgroundColor) {
    mBackgroundColor = backgroundColor;
  }

  /** See {@link View#setOverScrollMode(int)}. */
  public void setOverScrollMode(final int overScrollMode) {
    this.mOverScrollMode = overScrollMode;
  }

  public void setPadding(final @Nullable Rect padding) {
    mPadding = padding;
  }

  public void setClipToPadding(final boolean clipToPadding) {
    mClipToPadding = clipToPadding;
  }

  public void setIsScrollEnabled(final boolean isScrollEnabled) {
    this.mIsScrollEnabled = isScrollEnabled;
  }

  public void setIsNestedScrollEnabled(final boolean isNestedScrollEnabled) {
    this.mIsNestedScrollEnabled = isNestedScrollEnabled;
  }

  public void setScrollBarEnabled(final boolean enabled) {
    this.mScrollBarEnabled = enabled;
  }

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sAdapterBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {

        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return currentValue.mAdapter != newValue.mAdapter;
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          recyclerView.setAdapter(listRenderUnit.mAdapter);
          recyclerView.setItemAnimator(null);
          if (listRenderUnit.mListState != null
              && listRenderUnit.mListState.instanceState != null) {
            Objects.requireNonNull(recyclerView.getLayoutManager())
                .onRestoreInstanceState(listRenderUnit.mListState.instanceState);
          } else if (listRenderUnit.mStartingIndex >= 0) {
            recyclerView.scrollToPosition(listRenderUnit.mStartingIndex);
          }
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {
          if (listRenderUnit.mListState != null) {
            listRenderUnit.mListState.instanceState =
                Objects.requireNonNull(recyclerView.getLayoutManager()).onSaveInstanceState();
          }
          recyclerView.setAdapter(null);
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sLayoutManagerBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {

        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return !currentValue.mLayoutManagerConfig.isEquivalentTo(newValue.mLayoutManagerConfig);
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          final RecyclerView.LayoutManager layoutManager =
              listRenderUnit.mLayoutManagerConfig.getLayoutManager(context);
          recyclerView.setLayoutManager(layoutManager);
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {
          recyclerView.setLayoutManager(null);
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sScrollListenersBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {

        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          List<RecyclerView.OnScrollListener> currentListeners = currentValue.mOnScrollListeners;
          List<RecyclerView.OnScrollListener> newListeners = newValue.mOnScrollListeners;
          if (currentListeners == newListeners) {
            return false;
          }

          if (currentListeners == null
              || newListeners == null
              || currentListeners.size() != newListeners.size()) {
            return true;
          }

          return !currentListeners.equals(newListeners);
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          List<RecyclerView.OnScrollListener> listeners = listRenderUnit.mOnScrollListeners;
          if (listeners != null) {
            for (int i = 0, size = listeners.size(); i < size; i++) {
              recyclerView.addOnScrollListener(listeners.get(i));
            }
          }
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {
          List<RecyclerView.OnScrollListener> listeners = listRenderUnit.mOnScrollListeners;
          if (listeners != null) {
            for (int i = 0, size = listeners.size(); i < size; i++) {
              recyclerView.removeOnScrollListener(listeners.get(i));
            }
          }
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sSnapHelperBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {

        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            @Nullable Object currentLayoutData,
            @Nullable Object nextLayoutData) {
          final SnapHelper current = currentValue.mSnapHelper;
          final SnapHelper next = newValue.mSnapHelper;

          return !(Objects.equals(current, next));
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          if (listRenderUnit.mSnapHelper != null) {
            listRenderUnit.mSnapHelper.attachToRecyclerView(recyclerView);
          }
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {
          if (listRenderUnit.mSnapHelper != null) {
            listRenderUnit.mSnapHelper.attachToRecyclerView(null);
          }
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sItemDecorationBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {

        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          final List<RecyclerView.ItemDecoration> current = currentValue.mItemDecorations;
          final List<RecyclerView.ItemDecoration> next = newValue.mItemDecorations;

          return !(Objects.equals(current, next));
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          if (listRenderUnit.mItemDecorations != null) {
            for (RecyclerView.ItemDecoration decoration : listRenderUnit.mItemDecorations) {
              recyclerView.addItemDecoration(decoration);
            }
          }
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {
          if (listRenderUnit.mItemDecorations != null) {
            for (RecyclerView.ItemDecoration decoration : listRenderUnit.mItemDecorations) {
              recyclerView.removeItemDecoration(decoration);
            }
          }
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sItemAnimatorBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {

        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return currentValue.mItemAnimator != newValue.mItemAnimator;
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          recyclerView.setItemAnimator(listRenderUnit.mItemAnimator);
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {
          recyclerView.setItemAnimator(null);
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sLayoutDataBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {

        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          // Layout data should always be available.
          if (currentLayoutData == null || nextLayoutData == null) {
            return false;
          }

          final ListLayoutData currentResult = (ListLayoutData) currentLayoutData;
          final ListLayoutData newResult = (ListLayoutData) nextLayoutData;
          return currentResult.measuredWidth != newResult.measuredWidth
              || currentResult.measuredHeight != newResult.measuredHeight
              || !currentResult.items.equals(newResult.items);
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          if (layoutData == null) {
            throw new IllegalStateException("List data was not computed during layout");
          }
          listRenderUnit.mAdapter.setLayoutData((ListLayoutData) layoutData);
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {}
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sBackgroundColorBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {

        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            @Nullable Object currentLayoutData,
            @Nullable Object nextLayoutData) {
          return currentValue.mBackgroundColor != newValue.mBackgroundColor;
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          // See https://code.google.com/p/android/issues/detail?id=25266 for more information
          recyclerView.setBackgroundColor(listRenderUnit.mBackgroundColor);
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {}
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sOverscrollBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {

        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return currentValue.mOverScrollMode != newValue.mOverScrollMode;
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          recyclerView.setOverScrollMode(listRenderUnit.mOverScrollMode);
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {}
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sFadingEdgeBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {
        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return currentValue.mFadingEdgeLength != newValue.mFadingEdgeLength;
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          if (listRenderUnit.mFadingEdgeLength > 0) {
            final int orientation = listRenderUnit.mLayoutManagerConfig.mOrientation;
            if (orientation == RecyclerView.VERTICAL) {
              recyclerView.setVerticalFadingEdgeEnabled(true);
            } else {
              recyclerView.setHorizontalFadingEdgeEnabled(true);
            }
            recyclerView.setFadingEdgeLength(listRenderUnit.mFadingEdgeLength);
          }
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {
          recyclerView.setVerticalFadingEdgeEnabled(false);
          recyclerView.setHorizontalFadingEdgeEnabled(false);
          recyclerView.setFadingEdgeLength(0);
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sPaddingBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {
        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return !(Objects.equals(currentValue.mPadding, newValue.mPadding));
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          if (listRenderUnit.mPadding == null) {
            return null;
          }
          int left = listRenderUnit.mPadding.left;
          int top = listRenderUnit.mPadding.top;
          int right = listRenderUnit.mPadding.right;
          int bottom = listRenderUnit.mPadding.bottom;
          recyclerView.setPadding(left, top, right, bottom);
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {
          recyclerView.setPadding(0, 0, 0, 0);
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sClipToPaddingBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {
        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return currentValue.mClipToPadding != newValue.mClipToPadding;
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          recyclerView.setClipToPadding(listRenderUnit.mClipToPadding);
          return null;
        }

        @Override
        public void unbind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData,
            final Void bindData) {
          recyclerView.setClipToPadding(true);
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sIsScrollEnabledBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {
        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return currentValue.mIsScrollEnabled != newValue.mIsScrollEnabled;
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          recyclerView.setIsScrollEnabled(listRenderUnit.mIsScrollEnabled);
          return null;
        }

        @Override
        public void unbind(
            Context context,
            RCRecyclerView recyclerView,
            ListRenderUnit listRenderUnit,
            @Nullable Object layoutData,
            Void bindData) {
          // Restore scrollability.
          recyclerView.setIsScrollEnabled(true);
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sNestedIsScrollEnabledBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {
        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return currentValue.mIsNestedScrollEnabled != newValue.mIsNestedScrollEnabled;
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          recyclerView.setNestedScrollingEnabled(listRenderUnit.mIsNestedScrollEnabled);
          return null;
        }

        @Override
        public void unbind(
            Context context,
            RCRecyclerView recyclerView,
            ListRenderUnit listRenderUnit,
            @Nullable Object layoutData,
            Void bindData) {
          // Restore nested scrollability.
          recyclerView.setNestedScrollingEnabled(true);
        }
      };

  private static final Binder<ListRenderUnit, RCRecyclerView, Void> sScrollBarEnabledBinder =
      new Binder<ListRenderUnit, RCRecyclerView, Void>() {
        @Override
        public boolean shouldUpdate(
            final ListRenderUnit currentValue,
            final ListRenderUnit newValue,
            final @Nullable Object currentLayoutData,
            final @Nullable Object nextLayoutData) {
          return currentValue.mScrollBarEnabled != newValue.mScrollBarEnabled;
        }

        @Override
        public Void bind(
            final Context context,
            final RCRecyclerView recyclerView,
            final ListRenderUnit listRenderUnit,
            final @Nullable Object layoutData) {
          recyclerView.setVerticalScrollBarEnabled(listRenderUnit.mScrollBarEnabled);
          recyclerView.setHorizontalScrollBarEnabled(listRenderUnit.mScrollBarEnabled);
          return null;
        }

        @Override
        public void unbind(
            Context context,
            RCRecyclerView recyclerView,
            ListRenderUnit listRenderUnit,
            @Nullable Object layoutData,
            Void bindData) {
          // Restore default scrollbar visibility.
          recyclerView.setVerticalScrollBarEnabled(true);
          recyclerView.setHorizontalScrollBarEnabled(true);
        }
      };

  public void setFadingEdgeLength(final int length) {
    mFadingEdgeLength = length;
  }

  public void setListState(final ListState listState) {
    mListState = listState;
  }

  public void setStartingIndex(final int startingIndex) {
    mStartingIndex = startingIndex;
  }

  public static class ListState {
    @Nullable private Parcelable instanceState;
  }
}
