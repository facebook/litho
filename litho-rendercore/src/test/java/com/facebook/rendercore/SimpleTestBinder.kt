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

internal class SimpleTestBinder(private val mountRunnable: Runnable) :
    RenderUnit.Binder<Any?, Any, Any> {

  override fun shouldUpdate(
      currentModel: Any?,
      newModel: Any?,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean = true

  override fun bind(context: Context, content: Any, model: Any?, layoutData: Any?): Any? {
    mountRunnable.run()
    return null
  }

  override fun unbind(
      context: Context,
      content: Any,
      model: Any?,
      layoutData: Any?,
      bindData: Any?
  ) = Unit

  override val description: String
    get() = super.description
}
