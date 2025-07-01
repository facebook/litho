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

/**
 * A MountContext is a context object that is passed to the [RenderUnit] when interacting with
 * binders. It's a container for the parameters required to execute a successful
 * mount/unmount/attach/detach/update operation as required.
 *
 * @param androidContext The Android [Context].
 * @param tracer The [Systracer] that may be used to trace execution of code blocks.
 * @param binderObserver The [BinderObserver] that can observe the bind and unbind operations of a
 *   binder.
 * @see RenderUnit
 */
class MountContext
internal constructor(
    override val androidContext: Context,
    val tracer: Systracer,
    val binderObserver: BinderObserver? = null
) : BinderScope {

  private var _binderId: BinderId? = null
  private var _model: Any? = null

  internal inline fun <T> withBinder(
      binderId: BinderId,
      model: Any?,
      func: BinderScope.() -> T
  ): T {
    try {
      _binderId = binderId
      _model = model
      return func()
    } finally {
      _binderId = null
      _model = null
    }
  }

  /**
   * Returns the [BinderId] of the binder currently associated with the [BinderScope]. This is only
   * valid when called within the scope of [withBinder] and will throw an exception otherwise.
   */
  override val binderId: BinderId
    get() = requireNotNull(_binderId)

  /**
   * Returns the model of the binder currently associated with the [BinderScope]. This is only valid
   * when called within the scope of [withBinder] and will throw an exception otherwise.
   */
  override val binderModel: Any?
    get() = _model
}
