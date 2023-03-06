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

import kotlin.jvm.JvmField

/**
 * Implemented by the class used to store state within both Components and Sections to store state.
 */
abstract class StateContainer : Cloneable {

  abstract fun applyStateUpdate(stateUpdate: StateUpdate)

  public override fun clone(): StateContainer =
      try {
        super.clone() as StateContainer
      } catch (ex: CloneNotSupportedException) {
        // This should never happen
        throw RuntimeException(ex)
      }

  class StateUpdate(@JvmField val type: Int, vararg params: Any) {
    @JvmField val params: Array<Any>

    init {
      // Use spread operator(*) to get a writable array here,
      // [doc](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs)
      this.params = arrayOf(*params)
    }
  }
}
