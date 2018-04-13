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

package com.facebook.litho.sections.widget;

import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.widget.RecyclerEventsController;
import java.lang.ref.WeakReference;

/**
 * An controller that can be passed as {@link com.facebook.litho.annotations.Prop} to a
 * RecyclerCollection component to trigger events from outside the component hierarchy.
 */
public class RecyclerCollectionEventsController extends RecyclerEventsController {

  private WeakReference<SectionTree> mSectionTree;
  private int mFirstCompletelyVisibleItemPosition = 0;

  /**
   * Sent the RecyclerCollection a request to refresh it's backing data.
   */
  public void requestRefresh() {
    requestRefresh(false);
  }

  /**
   * Sent the RecyclerCollection a request to refresh it's backing data. If showSpinner is
   * true, then refresh spinner is shown.
   * @param showSpinner
   */
  public void requestRefresh(boolean showSpinner) {
    if (mSectionTree != null && mSectionTree.get() != null) {
      if (showSpinner) {
        showRefreshing();
      }

      mSectionTree.get().refresh();
    }
  }

  /**
   * Send the Recycler a request to scroll the content to the next item in the binder.
   * @param animated if animated is set to true the scroll will happen with an animation.
   */
  public void requestScrollToNextPosition(boolean animated) {
    requestScrollToRelativePosition(animated, true);
  }

  public void requestScrollToPreviousPosition(boolean animated) {
    requestScrollToRelativePosition(animated, false);
  }

  private void requestScrollToRelativePosition(boolean animated, boolean forward) {
    requestScrollToPosition(
        Math.max(0, mFirstCompletelyVisibleItemPosition + (forward ? 1 : -1)), animated);
  }

  void setSectionTree(SectionTree sectionTree) {
    mSectionTree = new WeakReference<>(sectionTree);
  }

  /**
   * A package private method for {@link RecyclerCollectionComponentSpec} to let this controller
   * know about the current scroll position.
   */
  void setFirstCompletelyVisibleItemPosition(int firstCompletelyVisibleItemPosition) {
    mFirstCompletelyVisibleItemPosition = firstCompletelyVisibleItemPosition;
  }
}
