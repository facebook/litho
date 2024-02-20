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
import android.view.View

open class TestBinder<MODEL> : RenderUnit.Binder<MODEL, View, Any> {

  class TestBinder1 : TestBinder<RenderUnit<*>> {
    constructor() : super()

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>
    ) : super(bindOrder, unbindOrder)
  }

  class TestBinder2 : TestBinder<RenderUnit<*>> {
    constructor() : super()

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>
    ) : super(bindOrder, unbindOrder)
  }

  class TestBinder3 : TestBinder<RenderUnit<*>> {
    constructor() : super()

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>
    ) : super(bindOrder, unbindOrder)
  }

  private val bindOrder: MutableList<Any?>
  private val unbindOrder: MutableList<Any?>
  var wasBound = false
  var wasUnbound = false

  constructor() {
    bindOrder = ArrayList()
    unbindOrder = ArrayList()
  }

  constructor(bindOrder: MutableList<Any?>, unbindOrder: MutableList<Any?>) {
    this.bindOrder = bindOrder
    this.unbindOrder = unbindOrder
  }

  override fun shouldUpdate(
      currentModel: MODEL,
      newModel: MODEL,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    return currentLayoutData != nextLayoutData
  }

  override fun bind(context: Context, content: View, model: MODEL, layoutData: Any?): Any? {
    bindOrder.add(this)
    wasBound = true
    return null
  }

  override fun unbind(
      context: Context,
      content: View,
      model: MODEL,
      layoutData: Any?,
      bindData: Any?
  ) {
    unbindOrder.add(this)
    wasUnbound = true
  }
}
