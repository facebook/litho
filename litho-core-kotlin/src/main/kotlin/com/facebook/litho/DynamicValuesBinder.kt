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
import com.facebook.rendercore.Mountable
import com.facebook.rendercore.RenderUnit

/**
 * Class that will be registered as MountUnmount [RenderUnit.DelegateBinder.createDelegateBinder].
 *
 * @param binders List of [DynamicPropsBinder] holding reference to DynamicValue and Content
 */
internal class DynamicValuesBinder<ContentT>(
    private val binders: ArrayList<DynamicPropsHolder<Any?, Mountable<*>>>
) : RenderUnit.Binder<Mountable<*>, ContentT, Any?> {

  override fun bind(
      context: Context?,
      content: ContentT,
      model: Mountable<*>?,
      layoutData: Any?
  ): Any? {
    binders.forEach { it.bind(context, content, model, layoutData) }
    return null
  }

  override fun unbind(
      context: Context?,
      content: ContentT,
      model: Mountable<*>?,
      layoutData: Any?,
      bindData: Any?
  ) {
    binders.forEach { it.unbind(context, content, model, layoutData) }
  }

  override fun shouldUpdate(
      currentModel: Mountable<*>?,
      newModel: Mountable<*>?,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean = true
}
