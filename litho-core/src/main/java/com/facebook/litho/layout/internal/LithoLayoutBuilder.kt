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

package com.facebook.litho.layout.internal

import androidx.collection.MutableScatterSet
import com.facebook.litho.LithoLayoutResult
import com.facebook.litho.layout.BoxConstraints
import com.facebook.litho.state.StateId
import com.facebook.rendercore.IntSize

/**
 * Lays one node out for one pass, within the boxes its parent resolves for it: measured any number
 * of times, then either built once into its result or discarded if nothing placed it. Nothing can
 * be asked of it after either.
 */
internal sealed interface LithoLayoutBuilder {

  /**
   * Its border-box size within [box]. A layout that fails measures 0 × 0 inside its padding and
   * border; its failure rides on its result, reported only if the result reaches the final tree.
   */
  fun measure(box: BoxConstraints): IntSize

  /**
   * Its result within [box]. Under a `Layout`, [box] is one it was measured in; under Yoga, it's
   * the box of its final size, which Yoga may not have measured it in.
   */
  fun build(box: BoxConstraints): LithoLayoutResult

  /**
   * Nothing placed it: adds the state reads of what it made to [reads], null when reads aren't
   * tracked. It never reaches the final tree, so its parent's result carries them.
   */
  fun discard(reads: MutableScatterSet<StateId>?)
}
