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

package com.facebook.litho.editor.model

/** Wraps over a boolean to make it an EditorValue */
class EditorBool(@JvmField val value: Boolean) : EditorValue() {

  override fun equals(o: Any?): Boolean {
    if (o === this) {
      return true
    }
    if (o !is EditorBool) {
      return false
    }
    val thisValue: Any = this.value
    val otherValue: Any = o.value
    return thisValue == otherValue
  }

  override fun hashCode(): Int {
    val PRIME = 59
    var result = 1
    val objValue: Any = this.value
    result = result * PRIME + objValue.hashCode()
    return result
  }

  override fun toString(): String {
    return value.toString()
  }

  override fun <R> `when`(visitor: EditorVisitor<R>): R {
    return visitor.isBool(this)
  }
}
