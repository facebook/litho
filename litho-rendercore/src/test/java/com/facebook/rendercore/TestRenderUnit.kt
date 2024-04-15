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

import android.content.Context
import android.util.SparseArray
import android.view.View

internal class TestRenderUnit : RenderUnit<View>, ContentAllocator<View> {

  constructor() : super(RenderType.VIEW)

  constructor(
      fixedMountBinders: List<DelegateBinder<*, in View, *>>?
  ) : super(
      RenderType.VIEW,
      fixedMountBinders,
      emptyList<DelegateBinder<*, in View, *>>(),
      emptyList<DelegateBinder<*, in View, *>>())

  constructor(
      fixedMountBinders: List<DelegateBinder<*, in View, *>>?,
      optionalMountBinders: List<DelegateBinder<*, in View, *>>,
      attachBinder: List<DelegateBinder<*, in View, *>>
  ) : super(RenderType.VIEW, fixedMountBinders, optionalMountBinders, attachBinder)

  constructor(
      extras: SparseArray<Any?>?
  ) : super(
      RenderType.VIEW,
      emptyList<DelegateBinder<*, in View, *>>(),
      emptyList<DelegateBinder<*, in View, *>>(),
      emptyList<DelegateBinder<*, in View, *>>(),
      extras)

  override fun createContent(context: Context): View {
    return View(context)
  }

  override val contentAllocator: ContentAllocator<View>
    get() = this

  override val id: Long
    get() = 0
}
