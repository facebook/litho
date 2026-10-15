/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

/** An interface for {@link SpecModel}s that can declare a CSS Flexbox §4.5 intrinsic size. */
public interface HasIntrinsicSize {

  /**
   * Whether this spec contributes a real min-content size to CSS Flexbox §4.5 automatic minimum
   * sizing. When false (the default) the component contributes 0 and Yoga short-circuits the
   * auto-min probe for this node.
   */
  boolean hasIntrinsicSize();
}
