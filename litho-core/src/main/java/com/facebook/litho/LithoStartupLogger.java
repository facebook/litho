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

package com.facebook.litho;

/** Logger for tracking Litho events happening during startup. */
public interface LithoStartupLogger {
  String LITHO_PREFIX = "litho";

  String CHANGESET_CALCULATION = "_changeset";
  String INIT_RANGE = "_initrange";
  String FIRST_MOUNT = "_firstmount";
  String LAST_MOUNT = "_lastmount";

  String START = "_start";
  String END = "_end";

  /** @return whether this logger is active. */
  boolean isEnabled();

  /**
   * Set attribution to the rendered events like the network query name, data source (network/cache)
   * etc.
   */
  void setDataAttribution(String attribution);

  /**
   * @return attribution to the rendered events like the network query name, data source
   *     (network/cache) etc.
   */
  String getLatestDataAttribution();

  /**
   * Mark the event with given name and stage (start/end). It will use currently assigned data
   * attribution.
   */
  void markPoint(String eventName, String stage);

  /** Mark the event with given name, stage (start/end), and given data attribution. */
  void markPoint(String eventName, String stage, String dataAttribution);
}
