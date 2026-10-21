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

// Declared in RecyclerView's package to reach ItemAnimator.setListener, which RecyclerView uses to
// learn that an item's animation finished.
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

/** Records every holder an [RecyclerView.ItemAnimator] reports as finished, in order. */
class ItemAnimatorFinishRecorder(
    animator: RecyclerView.ItemAnimator,
    private val onFinished: (RecyclerView.ViewHolder) -> Unit = {},
) {
  val finished = mutableListOf<RecyclerView.ViewHolder>()

  init {
    animator.setListener { holder ->
      finished.add(holder)
      onFinished(holder)
    }
  }
}
