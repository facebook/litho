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

import androidx.annotation.IntDef;

/**
 * An implementation of this interface will provide both the {@link
 * androidx.recyclerview.widget.RecyclerView}'s current visible views position and the total number
 * of items in its {@link androidx.recyclerview.widget.RecyclerView.Adapter}.
 */
public interface ViewportInfo {

  /**
   * @return the adapter position of the first visible view.
   */
  int findFirstVisibleItemPosition();

  /**
   * @return the adapter position of the last visible view.
   */
  int findLastVisibleItemPosition();

  /**
   * @return the adapter position of the first fully visible view.
   */
  int findFirstFullyVisibleItemPosition();

  /**
   * @return the adapter position of the last fully visible view.
   */
  int findLastFullyVisibleItemPosition();

  /**
   * @return total number of items in the adapter
   */
  int getItemCount();

  /**
   * Implement this interface to be notified of Viewport changes from the {@link Binder}
   */
  interface ViewportChanged {
    void viewportChanged(
        int firstVisibleIndex,
        int lastVisibleIndex,
        int firstFullyVisibleIndex,
        int lastFullyVisibleIndex,
        @State int state);
  }

  @IntDef({State.SCROLLING, State.DATA_CHANGES})
  @interface State {
    int SCROLLING = 0;
    int DATA_CHANGES = 1;
  }
}
