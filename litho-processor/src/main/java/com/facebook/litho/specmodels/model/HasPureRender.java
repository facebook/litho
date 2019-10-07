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

package com.facebook.litho.specmodels.model;

/** An interface for {@link SpecModel}s that can be pure render. */
public interface HasPureRender {

  /**
   * Whether this spec is pure render or not. If a spec is pure render then when the ComponentTree
   * for this component is updated, if nothing changes then the measurements for this component can
   * be re-used.
   */
  boolean isPureRender();
}
