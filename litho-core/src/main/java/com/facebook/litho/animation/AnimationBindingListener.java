/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.GraphBinding;

/**
 * Listener that receives events when a {@link GraphBinding} is activated or is finished.
 */
public interface AnimationBindingListener {

  /**
   * Called when {@link GraphBinding#activate} is called on the relevant binding.
   */
  void onWillStart(AnimationBinding binding);

  /**
   * Called when a {@link GraphBinding} is finished, meaning all of its nodes are finished.
   */
  void onFinish(AnimationBinding binding);

  /**
   * Called when a listener (including this one) returns false from {@link #shouldStart}
   */
  void onCanceledBeforeStart(AnimationBinding binding);

  /**
   * Return 'false' to cancel this animation and keep it from starting.
   *
   * @return shouldStart whether to start this animation (i.e. return false to cancel it and prevent
   * it from running)
   */
  boolean shouldStart(AnimationBinding binding);
}
