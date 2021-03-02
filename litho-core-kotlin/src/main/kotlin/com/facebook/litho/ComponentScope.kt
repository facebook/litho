/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import android.content.Context

class ComponentScope(val context: ComponentContext) {
  val androidContext: Context
    get() = context.androidContext
  val resourceResolver: ResourceResolver
    get() = context.resourceResolver

  // TODO: Extract into more generic container to track hooks when needed
  internal var useStateIndex = 0

  inline fun Dimen.toPixels(): Int = this.toPixels(resourceResolver)

  /*inline*/ fun Component.applyStyle(style: Style?) {
    style?.applyToComponent(resourceResolver, this)
  }
}
