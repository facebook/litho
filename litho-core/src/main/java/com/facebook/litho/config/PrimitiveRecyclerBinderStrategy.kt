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

package com.facebook.litho.config

enum class PrimitiveRecyclerBinderStrategy {
  /**
   * In this strategy we attempt to break the mount behavior with three different binders.
   *
   * @see [com.facebook.litho.widget.ExperimentalRecycler.SplitBindersMountBehavior]
   */
  SPLIT_BINDERS,

  /** In this strategy we only remove the item decoration binding from the original approach. */
  RECYCLER_SPEC_EQUIVALENT_AND_ITEM_DECORATION
}
