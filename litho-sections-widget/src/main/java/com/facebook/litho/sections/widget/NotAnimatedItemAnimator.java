/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.widget;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;

/**
 * An animator that disables all animations in a {@link RecyclerView}
 *
 * From java/com/facebook/fbreact/views/recyclerview/NotAnimatedItemAnimator.java
 */
public class NotAnimatedItemAnimator extends SimpleItemAnimator {

  public NotAnimatedItemAnimator() {
    super();
    setSupportsChangeAnimations(false);
  }

  @Override
  public void runPendingAnimations() {
    // nothing
  }

  @Override
  public boolean animateRemove(RecyclerView.ViewHolder holder) {
    dispatchRemoveStarting(holder);
    dispatchRemoveFinished(holder);
    return true;
  }

  @Override
  public boolean animateAdd(RecyclerView.ViewHolder holder) {
    dispatchAddStarting(holder);
    dispatchAddFinished(holder);
    return true;
  }

  @Override
  public boolean animateMove(
      RecyclerView.ViewHolder holder,
      int fromX,
      int fromY,
      int toX,
      int toY) {
    dispatchMoveStarting(holder);
    dispatchMoveFinished(holder);
    return true;
  }

  @Override
  public boolean animateChange(
      RecyclerView.ViewHolder oldHolder,
      RecyclerView.ViewHolder newHolder,
      int fromLeft,
      int fromTop,
      int toLeft,
      int toTop) {
    // Avoid calling animation finish for the item twice,
    // preventing "isRecyclable decremented below 0" error
    if (oldHolder != newHolder) {
      dispatchChangeStarting(oldHolder, true);
      dispatchChangeFinished(oldHolder, true);
    }
    dispatchChangeStarting(newHolder, false);
    dispatchChangeFinished(newHolder, false);
    return true;
  }

  @Override
  public void endAnimation(RecyclerView.ViewHolder item) {
  }

  @Override
  public void endAnimations() {
  }

  @Override
  public boolean isRunning() {
    return false;
  }
}
