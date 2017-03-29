/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.dataflow.springs;

/**
 * This code was taken from the facebook/rebound repository.
 */
public interface SpringListener {

  /**
   * called whenever the spring is updated
   * @param spring the Spring sending the update
   */
  void onSpringUpdate(Spring spring);

  /**
   * called whenever the spring achieves a resting state
   * @param spring the spring that's now resting
   */
  void onSpringAtRest(Spring spring);

  /**
   * called whenever the spring leaves its resting state
   * @param spring the spring that has left its resting state
   */
  void onSpringActivate(Spring spring);

  /**
   * called whenever the spring notifies of displacement state changes
   * @param spring the spring whose end state has changed
   */
  void onSpringEndStateChange(Spring spring);
}
