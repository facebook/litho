// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.litho.animation;

import com.facebook.litho.dataflow.GraphBinding;

/**
 * Listener that receives events when a {@link GraphBinding} is activated or is finished.
 */
public interface AnimationBindingListener {

  /**
   * Called when {@link GraphBinding#activate} is called on the relevant binding.
   */
  void onStart(AnimationBinding binding);

  /**
   * Called when a {@link GraphBinding} is finished, meaning all of its nodes are finished.
   */
  void onFinish(AnimationBinding binding);
}
