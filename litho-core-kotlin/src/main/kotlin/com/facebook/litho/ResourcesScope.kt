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

import android.content.Context

/**
 * The receiver for [KComponent] methods which need to access resources and widgets like [Row],
 * [Column], and [Text]. This class exposes the ability to access functions defined in [Resources]
 * like [stringRes]/[drawableRes] etc.
 */
interface ResourcesScope {
  val context: ComponentContext
  val androidContext: Context
    get() = context.androidContext
  val resourceResolver: ResourceResolver
    get() = context.resourceResolver

  fun Dimen.toPixels(): Int = this.toPixels(resourceResolver)
}
