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

/** A object pool that has debug info for display in tools like Stetho. */
interface PoolWithDebugInfo {

  /** @return a human-readable name for the pool. */
  val name: String

  /** @return the max number of objects this pool will hold. */
  val maxSize: Int

  /** @return the number of objects currently in the pool. */
  val currentSize: Int
}
