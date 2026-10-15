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

package com.facebook.rendercore

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Extension properties for creating [Duration] values using a concise syntax.
 *
 * These extensions provide a convenient way to express time durations in animations and
 * transitions.
 *
 * Example usage:
 * ```
 * val animationDuration = 300.ms   // 300 milliseconds
 * val delayDuration = 1.s          // 1 second
 * val preciseDuration = 1.5.s      // 1.5 seconds
 * ```
 *
 * @see Duration
 */

/** Creates a [Duration] with the given value in milliseconds. */
inline val Int.ms: Duration
  get() = this.milliseconds

/** Creates a [Duration] with the given value in milliseconds. */
inline val Long.ms: Duration
  get() = this.milliseconds

/** Creates a [Duration] with the given value in milliseconds. */
inline val Double.ms: Duration
  get() = this.milliseconds

/** Creates a [Duration] with the given value in seconds. */
inline val Int.s: Duration
  get() = this.seconds

/** Creates a [Duration] with the given value in seconds. */
inline val Long.s: Duration
  get() = this.seconds

/** Creates a [Duration] with the given value in seconds. */
inline val Double.s: Duration
  get() = this.seconds
