/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.LithoView;

/**
 * Controller that handles sticky header logic. Depending on where the sticky item is located in the
 * list, we might either use first child as sticky header or use {@link SectionsRecyclerView}'s
 * sticky header.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class StickyHeaderControllerImpl extends RecyclerView.OnScrollListener
    implements StickyHeaderController {

  static final String RECYCLER_ARGUMENT_NULL = "Cannot initialize with null SectionsRecyclerView.";
  static final String RECYCLER_ALREADY_INITIALIZED =
      "SectionsRecyclerView has already been initialized but never reset.";
  static final String RECYCLER_NOT_INITIALIZED = "SectionsRecyclerView has not been set yet.";
  static final String LAYOUTMANAGER_NOT_INITIALIZED =
      "LayoutManager of RecyclerView is not initialized yet.";

  private final HasStickyHeader mHasStickyHeader;

  private @Nullable SectionsRecyclerView mSectionsRecyclerView;
  private @Nullable RecyclerView.LayoutManager mLayoutManager;
  private @Nullable View lastTranslatedView;
  private int previousStickyHeaderPosition = RecyclerView.NO_POSITION;

  StickyHeaderControllerImpl(HasStickyHeader hasStickyHeader) {
    mHasStickyHeader = hasStickyHeader;
  }

  @Override
  public void init(SectionsRecyclerView SectionsRecyclerView) {
    if (SectionsRecyclerView == null) {
      throw new RuntimeException(RECYCLER_ARGUMENT_NULL);
    }

    if (mSectionsRecyclerView != null) {
      throw new RuntimeException(RECYCLER_ALREADY_INITIALIZED);
    }

    mSectionsRecyclerView = SectionsRecyclerView;
    mSectionsRecyclerView.hideStickyHeader();
    mLayoutManager = SectionsRecyclerView.getRecyclerView().getLayoutManager();
    if (mLayoutManager == null) {
      throw new RuntimeException(LAYOUTMANAGER_NOT_INITIALIZED);
    }

    mSectionsRecyclerView.getRecyclerView().addOnScrollListener(this);
  }

  @Override
  public void reset() {
    if (mSectionsRecyclerView == null) {
      throw new IllegalStateException(RECYCLER_NOT_INITIALIZED);
    }

    mSectionsRecyclerView.getRecyclerView().removeOnScrollListener(this);
    mLayoutManager = null;
    mSectionsRecyclerView = null;
  }

  @Override
  public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
    final int firstVisiblePosition = mHasStickyHeader.findFirstVisibleItemPosition();

    if (firstVisiblePosition == RecyclerView.NO_POSITION) {
      return;
    }

    final int stickyHeaderPosition = findStickyHeaderPosition(firstVisiblePosition);
    final ComponentTree firstVisibleItemComponentTree =
        mHasStickyHeader.getComponentForStickyHeaderAt(firstVisiblePosition);

    if (lastTranslatedView != null
        && firstVisibleItemComponentTree != null
        && lastTranslatedView != firstVisibleItemComponentTree.getLithoView()) {
      // Reset previously modified view
      lastTranslatedView.setTranslationY(0);
      lastTranslatedView = null;
    }

    if (stickyHeaderPosition == RecyclerView.NO_POSITION || firstVisibleItemComponentTree == null) {
      // no sticky header above first visible position, reset the state
      mSectionsRecyclerView.hideStickyHeader();
      previousStickyHeaderPosition = RecyclerView.NO_POSITION;
      return;
    }

    if (firstVisiblePosition == stickyHeaderPosition) {

      final LithoView firstVisibleView = firstVisibleItemComponentTree.getLithoView();

      if (firstVisibleView == null) {
        // When RV has pending updates adapter position and layout position might not match
        // and firstVisibleView might be null.
        final ComponentsLogger logger = firstVisibleItemComponentTree.getContext().getLogger();
        if (logger != null) {
          logger.emitMessage(
              ComponentsLogger.LogLevel.ERROR,
              "First visible sticky header item is null"
                  + ", RV.hasPendingAdapterUpdates: "
                  + mSectionsRecyclerView.getRecyclerView().hasPendingAdapterUpdates()
                  + ", first visible component: "
                  + firstVisibleItemComponentTree.getSimpleName()
                  + ", hasMounted: "
                  + firstVisibleItemComponentTree.hasMounted()
                  + ", isReleased: "
                  + firstVisibleItemComponentTree.isReleased());
        }
      } else {

        // Translate first child, no need for sticky header.
        //
        // NOTE: Translate only if the next item is not also sticky header. If two sticky items are
        // stacked we don't want to translate the first one, as it would hide the second one under
        // the first one which is undesirable.
        if (!mHasStickyHeader.isValidPosition(stickyHeaderPosition + 1)
            || !mHasStickyHeader.isSticky(stickyHeaderPosition + 1)) {
          firstVisibleView.setTranslationY(-firstVisibleView.getTop());
        }
      }

      lastTranslatedView = firstVisibleView;
      mSectionsRecyclerView.hideStickyHeader();
      previousStickyHeaderPosition = RecyclerView.NO_POSITION;
    } else {

      if (mSectionsRecyclerView.isStickyHeaderHidden()
          || stickyHeaderPosition != previousStickyHeaderPosition) {
        initStickyHeader(stickyHeaderPosition);
        mSectionsRecyclerView.showStickyHeader();
      }

      // Translate sticky header
      final int lastVisiblePosition = mHasStickyHeader.findLastVisibleItemPosition();
      int translationY = 0;
      for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
        if (mHasStickyHeader.isSticky(i)) {
          final View nextStickyHeader = mLayoutManager.findViewByPosition(i);
          final int offsetBetweenStickyHeaders =
              nextStickyHeader.getTop()
                  - mSectionsRecyclerView.getStickyHeader().getBottom()
                  + mSectionsRecyclerView.getPaddingTop();
          translationY = Math.min(offsetBetweenStickyHeaders, 0);
          break;
        }
      }
      mSectionsRecyclerView.setStickyHeaderVerticalOffset(translationY);
      previousStickyHeaderPosition = stickyHeaderPosition;
    }
  }

  private void initStickyHeader(int stickyHeaderPosition) {
    final ComponentTree componentTree =
        mHasStickyHeader.getComponentForStickyHeaderAt(stickyHeaderPosition);
    // RecyclerView might not have yet detached the view that this componentTree bound to,
    // so detach it if that is the case.
    detachLithoViewIfNeeded(componentTree.getLithoView());
    mSectionsRecyclerView.setStickyComponent(componentTree);
  }

  private static void detachLithoViewIfNeeded(LithoView view) {
    if (view == null) {
      return;
    }
    // This is equivalent of calling view.isAttachedToWindow(),
    // however, that method is available only from API19
    final boolean isAttachedToWindow = view.getWindowToken() != null;
    if (isAttachedToWindow) {
      view.onStartTemporaryDetach();
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  int findStickyHeaderPosition(int currentFirstVisiblePosition) {
    for (int i = currentFirstVisiblePosition; i >= 0; i--) {
      if (mHasStickyHeader.isSticky(i)) {
        return i;
      }
    }
    return RecyclerView.NO_POSITION;
  }
}
