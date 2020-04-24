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

package com.facebook.rendercore;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

public abstract class Host extends ViewGroup {

  private boolean mWasInvalidatedWhileSuppressed;
  private boolean mWasRequestedFocusWhileSuppressed;
  private boolean mSuppressInvalidations;

  public Host(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Mounts the given {@link MountItem} with unique index.
   *
   * @param index index of the {@link MountItem}. Guaranteed to be the same index as is passed for
   *     the corresponding {@code unmount(index, mountItem)} call.
   * @param mountItem item to be mounted into the host.
   */
  public abstract void mount(int index, MountItem mountItem);

  /**
   * Unmounts the given {@link MountItem}
   *
   * @param mountItem item to be unmounted from the host.
   */
  public abstract void unmount(MountItem mountItem);

  /**
   * Unmounts the given {@link MountItem} with unique index.
   *
   * @param index index of the {@link MountItem}. Guaranteed to be the same index as was passed for
   *     the corresponding {@code mount(index, mountItem)} call.
   * @param mountItem item to be unmounted from the host.
   */
  public abstract void unmount(int index, MountItem mountItem);

  /** @return number of {@link MountItem}s that are currently mounted in the host. */
  public abstract int getMountItemCount();

  /** @return the {@link MountItem} that was mounted with the given index. */
  public abstract MountItem getMountItemAt(int index);

  /**
   * Moves the MountItem associated to oldIndex in the newIndex position. This happens when a
   * RootHostView needs to re-arrange the internal order of its items. If an item is already present
   * in newIndex the item is guaranteed to be either unmounted or moved to a different index by
   * subsequent calls to either {@link HostView#unmount(int, MountItem)} or {@link
   * HostView#moveItem(MountItem, int, int)}.
   *
   * @param item The item that has been moved.
   * @param oldIndex The current index of the MountItem.
   * @param newIndex The new index of the MountItem.
   */
  public abstract void moveItem(MountItem item, int oldIndex, int newIndex);

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

  @Override
  public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
    // Arguments for equivalent call to View.requestFocus().
    final boolean nullArgumentsRequestFocusCall =
        (direction == View.FOCUS_DOWN && previouslyFocusedRect == null);
    if (nullArgumentsRequestFocusCall && mSuppressInvalidations) {
      mWasRequestedFocusWhileSuppressed = true;
      return false;
    }

    return super.requestFocus(direction, previouslyFocusedRect);
  }

  /**
   * This is used to collapse all invalidation calls on hosts during mount. While invalidations are
   * suppressed, the hosts will simply bail on invalidations. Once the suppression is turned off, a
   * single invalidation will be triggered on the affected hosts.
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

      if (mWasRequestedFocusWhileSuppressed) {
        final View root = getRootView();
        if (root != null) {
          root.requestFocus();
        }
        mWasRequestedFocusWhileSuppressed = false;
      }
    }
  }

  @VisibleForTesting
  public boolean getSuppressInvalidations() {
    return mSuppressInvalidations;
  }
}
