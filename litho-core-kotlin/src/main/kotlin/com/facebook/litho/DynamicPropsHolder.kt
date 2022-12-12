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

/**
 * Class responsible for setting the dynamic value on the content through the [DynamicValuesBinder]
 * that is register as [RenderUnit.DelegateBinder.extension]
 *
 * @param dynamicValue value that will be set to the content in bind and onValueChange
 * @param defaultValue value that will be set to the content after unbind call
 * @param valueSetter function or function reference that will set the dynamic value on the content
 */
internal class DynamicPropsHolder<ContentT, T>(
    private val dynamicValue: DynamicValue<T>,
    private val defaultValue: T,
    private val valueSetter: (ContentT, T) -> Unit
) : DynamicValue.OnValueChangeListener<T> {

  private var content: ContentT? = null

  override fun onValueChange(value: DynamicValue<T>) {
    content?.let {
      ThreadUtils.assertMainThread()
      valueSetter(it, value.get())
    }
  }

  fun bind(context: Context?, content: ContentT, model: Mountable<*>?, layoutData: Any?) {
    this.content = content
    dynamicValue.attachListener(this)
    valueSetter(content, dynamicValue.get())
  }

  fun unbind(context: Context?, content: ContentT, model: Mountable<*>?, layoutData: Any?) {
    valueSetter(content, defaultValue)
    dynamicValue.detach(this)
  }
}
