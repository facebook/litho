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
import androidx.core.util.Pair

open class TestBinderWithBindData<MODEL> : RenderUnit.Binder<MODEL, View, Any> {

  class TestBinderWithBindData1 : TestBinderWithBindData<RenderUnit<*>?> {
    constructor() : super()

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>
    ) : super(bindOrder, unbindOrder)

    constructor(bindData: Any?) : super(bindData)

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>,
        bindData: Any?
    ) : super(bindOrder, unbindOrder, bindData)
  }

  class TestBinderWithBindData2 : TestBinderWithBindData<RenderUnit<*>?> {
    constructor() : super()

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>
    ) : super(bindOrder, unbindOrder)

    constructor(bindData: Any?) : super(bindData)

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>,
        bindData: Any?
    ) : super(bindOrder, unbindOrder, bindData)
  }

  class TestBinderWithBindData3 : TestBinderWithBindData<RenderUnit<*>?> {
    constructor() : super()

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>
    ) : super(bindOrder, unbindOrder)

    constructor(bindData: Any?) : super(bindData)

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>,
        bindData: Any?
    ) : super(bindOrder, unbindOrder, bindData)
  }

  class TestBinderWithBindData4 : TestBinderWithBindData<RenderUnit<*>?> {
    constructor() : super()

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>
    ) : super(bindOrder, unbindOrder)

    constructor(bindData: Any?) : super(bindData)

    constructor(
        bindOrder: MutableList<Any?>,
        unbindOrder: MutableList<Any?>,
        bindData: Any?
    ) : super(bindOrder, unbindOrder, bindData)
  }

  private val bindOrder: MutableList<Any?>
  private val unbindOrder: MutableList<Any?>
  private val bindData: Any?
  var wasBound = false
  var wasUnbound = false

  constructor() {
    bindOrder = ArrayList()
    unbindOrder = ArrayList()
    bindData = Any()
  }

  constructor(bindData: Any?) {
    bindOrder = ArrayList()
    unbindOrder = ArrayList()
    this.bindData = bindData
  }

  constructor(bindOrder: MutableList<Any?>, unbindOrder: MutableList<Any?>) {
    this.bindOrder = bindOrder
    this.unbindOrder = unbindOrder
    bindData = Any()
  }

  constructor(bindOrder: MutableList<Any?>, unbindOrder: MutableList<Any?>, bindData: Any?) {
    this.bindOrder = bindOrder
    this.unbindOrder = unbindOrder
    this.bindData = bindData
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
    bindOrder.add(Pair<Any?, Any?>(this, bindData))
    wasBound = true
    return bindData
  }

  override fun unbind(
      context: Context,
      content: View,
      model: MODEL,
      layoutData: Any?,
      bindData: Any?
  ) {
    unbindOrder.add(Pair<Any?, Any?>(this, bindData))
    wasUnbound = true
  }
}
