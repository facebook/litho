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

package com.facebook.rendercore.incrementalmount;

/**
 * This class contains mainly flags for features that are either used for development, or are not
 * ready for public consumption, or for use in experiments.
 *
 * <p>The current values are safe defaults and should not require manual changes.
 */
public class IncrementalMountExtensionConfigs {

  public static final String DEBUG_TAG = "IncrementalMount";

  /** Set this to true to enable debug logs for the incremental mount extension. */
  public static boolean isDebugLoggingEnabled = false;

  public static boolean shouldSkipBoundsInNegativeCoordinateSpace = false;
}
