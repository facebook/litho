/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

/**
 * An animation or set of animations using {@link com.facebook.litho.dataflow.GraphBinding}s.
 * This interface adds on top of {@link com.facebook.litho.dataflow.GraphBinding} the ability
 * to define {@link PendingNode}s which will be resolved to concrete ValueNodes when the
 * animation is ready to start. These are used to allow developers to define animations when the
 * concrete view or view property doesn't exist yet.
 */
public interface AnimationBinding {

  /**
   * Begins this animation.
   */
  void start();

  /**
   * Stops this animation.
   */
  void stop();

  /**
   * @return whether this animation is running
   */
  boolean isActive();

  /**
   * Resolves {@link PendingNode}s with the given resolver. Should be called before
   * {@link #start()}.
   */
  void resolvePendingNodes(PendingNodeResolver resolver);

  /**
   * Adds a {@link AnimationBindingListener}.
   */
  void addListener(AnimationBindingListener animationBindingListener);

  /**
   * Removes a previously added {@link AnimationBindingListener}.
   */
  void removeListener(AnimationBindingListener animationBindingListener);
}
