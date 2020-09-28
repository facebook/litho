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

package com.facebook.litho.dataflow.springs;

/** This code was taken from the facebook/rebound repository. */
public interface SpringListener {

  /**
   * called whenever the spring is updated
   *
   * @param spring the Spring sending the update
   */
  void onSpringUpdate(Spring spring);

  /**
   * called whenever the spring achieves a resting state
   *
   * @param spring the spring that's now resting
   */
  void onSpringAtRest(Spring spring);

  /**
   * called whenever the spring leaves its resting state
   *
   * @param spring the spring that has left its resting state
   */
  void onSpringActivate(Spring spring);

  /**
   * called whenever the spring notifies of displacement state changes
   *
   * @param spring the spring whose end state has changed
   */
  void onSpringEndStateChange(Spring spring);
}
