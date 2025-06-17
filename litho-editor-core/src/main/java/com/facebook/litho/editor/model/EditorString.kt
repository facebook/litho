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

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate

/** Wraps over a String to make it an EditorValue */
@DataClassGenerate
data class EditorString(@JvmField val value: String) : EditorValue() {
  override fun toString(): String {
    return value
  }

  override fun <R> `when`(visitor: EditorVisitor<R>): R {
    return visitor.isString(this)
  }
}
