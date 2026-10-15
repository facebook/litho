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

/**
 * A specialized functional interface that returns a primitive `boolean` in bytecode, avoiding the
 * boxing overhead of Kotlin's `() -> Boolean` (which erases to `Function0<Boolean>` and forces
 * every return through `java.lang.Boolean`).
 *
 * Use this instead of `() -> Boolean` when the lambda is passed through a virtual (open/abstract)
 * method boundary and cannot be inlined.
 */
fun interface BooleanFunc {
  fun invoke(): Boolean
}
