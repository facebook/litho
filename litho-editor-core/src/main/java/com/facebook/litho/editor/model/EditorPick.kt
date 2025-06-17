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

class EditorPick(otherValues: Set<String>, selected: String) : EditorValue() {
  @JvmField val values: Set<String>
  @JvmField val selected: String

  init {
    val values = HashSet<String>(otherValues.size + 1)
    values.addAll(otherValues)
    values.add(selected)
    this.values = values
    this.selected = selected
  }

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }
    if (o == null || javaClass != o.javaClass) {
      return false
    }

    val that = o as EditorPick

    if (values != that.values) {
      return false
    }
    return selected == that.selected
  }

  override fun hashCode(): Int {
    var result = values.hashCode()
    result = 31 * result + selected.hashCode()
    return result
  }

  override fun toString(): String {
    return selected
  }

  override fun <R> `when`(visitor: EditorVisitor<R>): R {
    return visitor.isPick(this)
  }
}
