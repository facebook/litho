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

package com.facebook.rendercore.incrementalmount

import android.content.Context
import com.facebook.rendercore.RenderUnit

/**
 * This Binder is only used to hold the information indicating whether the Mountable Component skips
 * Incremental Mount. If this is true then the Component will not be involved in Incremental Mount.
 */
class ExcludeFromIncrementalMountBinder private constructor() :
    RenderUnit.Binder<RenderUnit<*>?, Any?, Any> {
  override fun shouldUpdate(
      currentModel: RenderUnit<*>?,
      newModel: RenderUnit<*>?,
      currentLayoutData: Any?,
      nextLayoutData: Any?,
  ): Boolean {
    return false
  }

  override fun bind(
      context: Context,
      content: Any?,
      model: RenderUnit<*>?,
      layoutData: Any?,
  ): Any? {
    return null
  }

  override fun unbind(
      context: Context,
      content: Any?,
      model: RenderUnit<*>?,
      layoutData: Any?,
      bindData: Any?,
  ) {}

  companion object {
    val INSTANCE: ExcludeFromIncrementalMountBinder = ExcludeFromIncrementalMountBinder()
  }
}
