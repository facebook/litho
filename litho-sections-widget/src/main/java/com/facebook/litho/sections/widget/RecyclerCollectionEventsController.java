/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import com.facebook.litho.sections.Section;
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
   * Update the section tree with a new root.
   */
  public void requestNewRootSection(Section section) {
    if (mSectionTree != null && mSectionTree.get() != null) {
      mSectionTree.get().setRoot(section);
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
