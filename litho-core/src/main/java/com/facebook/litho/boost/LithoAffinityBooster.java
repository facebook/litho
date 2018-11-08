/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.boost;

/**
 * Provides methods for boosting critical operations so that they are executed more efficiently. One
 * example is moving the operation to a more powerful core.
 */
public interface LithoAffinityBooster {

  boolean isSupported();

  /**
   * Start boosting.
   *
   * @return false if the operation cannot be boosted at this time.
   */
  boolean request();

  /** End boosting. */
  void release();

  String getIdentifier();
}
