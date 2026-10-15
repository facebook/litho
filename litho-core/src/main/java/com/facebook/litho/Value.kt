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

package com.facebook.litho

/**
 * A readonly observable value interface.
 *
 * `Value<T>` represents a value that can be read but not directly mutated through this interface.
 * It serves as the foundational abstraction for all observable values in Litho, enabling:
 * - **Type-safe readonly access**: Functions that only need to read can accept `Value<T>`
 * - **Specialized implementations**: Different value types with different behaviors
 * - **Deferred reads**: Style APIs can store `Value<T>` and read during appropriate phases
 * - **Automatic optimization**: Phase-based read tracking enables targeted updates
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Reading a value
 * val count: Value<Int> = ...
 * Text("Count: ${count.value}")
 *
 * // Passing to Style APIs (deferred read)
 * val alpha: Value<Float> = useAnimatedFloat(1f)
 * Style.alpha(alpha)  // Read deferred to bind phase
 *
 * // Functions accepting readonly values
 * fun formatValue(value: Value<Int>): String {
 *     return "Value is ${value.value}"
 * }
 * ```
 *
 * @see State for an updatable Value implementation
 */
interface Value<out T> {

  /**
   * The current value.
   *
   * Reading this property may trigger state read tracking depending on the current execution
   * context. When read during:
   * - Component resolve → tracked for re-resolve on change
   * - Layout calculation → tracked for re-layout on change
   * - Binder execution → tracked for rebind-only on change
   * - Draw scope execution → tracked for redraw-only on change
   *
   * ## Thread Safety
   *
   * This property is safe to read from any thread. Implementations must ensure thread-safe access
   * to the underlying value.
   *
   * ## Performance Note
   *
   * It may be more efficient to defer reading this [value] by passing the `Value<T>` object around
   * instead. This allows the value to be read when it's actually needed, rather than eagerly during
   * a resolve, which can significantly improve performance.
   */
  val value: T
}
