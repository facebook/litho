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

public enum LifecycleStep {
  ON_CREATE_INITIAL_STATE,
  ON_CREATE_LAYOUT,
  ON_PREPARE,
  ON_MEASURE,
  ON_BOUNDS_DEFINED,
  ON_CREATE_MOUNT_CONTENT,
  ON_MOUNT,
  ON_UNMOUNT,
  ON_BIND,
  ON_UNBIND,
  ON_ATTACHED,
  ON_BIND_DYNAMIC_VALUE,
  ON_CALCULATE_CACHED_VALUE,
  ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
  ON_CREATE_MOUNT_CONTENT_POOL,
  ON_CREATE_TRANSITION,
  ON_CREATE_TREE_PROP,
  ON_ENTERED_RANGE,
  ON_ERROR,
  ON_EVENT,
  ON_EXITED_RANGE,
  ON_LOAD_STYLE,
  ON_MEASURE_BASELINE,
  ON_POPULATE_ACCESSIBILITY_NODE,
  ON_POPULATE_EXTRA_ACCESSIBILITY_NODE,
  ON_REGISTER_RANGES,
  ON_SHOULD_CREATE_LAYOUT_WITH_NEW_SIZE_SPEC,
  ON_TRIGGER,
  ON_UPDATE_STATE,
  ON_UPDATE_STATE_WITH_TRANSITION,
  ON_DETACHED;

  public static class StepInfo {
    public final LifecycleStep step;

    public StepInfo(LifecycleStep step) {
      this.step = step;
    }
  }
}
