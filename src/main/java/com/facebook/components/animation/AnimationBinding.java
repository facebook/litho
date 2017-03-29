// Copyright 2004-present Facebook. All Rights Reserved.

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
}
