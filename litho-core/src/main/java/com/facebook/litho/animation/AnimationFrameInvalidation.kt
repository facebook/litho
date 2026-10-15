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

package com.facebook.litho.animation

import com.facebook.litho.state.StateId

/**
 * A stable descriptor of an animated value's invalidation, recorded each frame the value changes.
 *
 * This is a framework-internal contract between `AnimatedValue` and
 * `StateUpdater.applyAnimationFrameUpdates`. Its fields are all fixed for the lifetime of the
 * producing `AnimatedValue`, so a single instance is cached on the value and reused every frame
 * rather than allocated per frame. It carries only what is needed to invalidate the readers of
 * [stateId]; the animated value itself stays owned by the `AnimatedValue`, so this is an
 * invalidation key, not a state mutation.
 *
 * @property stateId the animated value's state id, used to locate its read-tracked consumers.
 * @property isLayoutState whether the producing hook was created in a layout context; threaded into
 *   the hook-state update path exactly as the existing animated path does.
 * @property attribution debug attribution string for tracing.
 */
data class AnimationFrameInvalidation(
    val stateId: StateId,
    val isLayoutState: Boolean,
    val attribution: String?,
)
