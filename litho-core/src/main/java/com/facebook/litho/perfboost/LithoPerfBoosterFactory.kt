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

package com.facebook.litho.perfboost

/** Factory for creating a [LithoPerfBooster]. */
abstract class LithoPerfBoosterFactory {

  private var booster: LithoPerfBooster? = null

  fun acquireInstance(): LithoPerfBooster {
    val booster = this.booster
    if (booster != null) {
      return booster
    }
    return create().also { this.booster = it }
  }

  protected abstract fun create(): LithoPerfBooster
}
