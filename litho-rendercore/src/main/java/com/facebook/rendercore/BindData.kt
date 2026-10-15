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

import androidx.collection.MutableScatterMap

/**
 * A class storing data that was created in [RenderUnit.Binder.bind] and will be passed to
 * [RenderUnit.Binder.unbind].
 */
class BindData {
  var fixedBindersBindData: Array<Any?>? = null
    private set

  var optionalMountBindersBindData: MutableScatterMap<BinderKey, Any?>? = null
    private set

  var attachBindersBindData: MutableScatterMap<BinderKey, Any?>? = null
    private set

  /**
   * Sets the bindData at specified [index] to the provided value. If [fixedBindersBindData] doesn't
   * exist, it creates it and then sets the bindData value.
   */
  fun setFixedBindersBindData(bindData: Any?, index: Int, fixedMountBindersSize: Int) {
    if (bindData == null) {
      return
    }

    val bindersBindData: Array<Any?> = fixedBindersBindData ?: arrayOfNulls(fixedMountBindersSize)
    bindersBindData[index] = bindData
    if (fixedBindersBindData == null) {
      fixedBindersBindData = bindersBindData
    }
  }

  /**
   * Removes the bindData at specified [index] and returns it if it was present or `null` if it
   * wasn't present or [fixedBindersBindData] array was `null`.
   */
  fun removeFixedBinderBindData(index: Int): Any? {
    return fixedBindersBindData?.let { array ->
      val bindData = array[index]
      array[index] = null
      bindData
    }
  }

  /**
   * Sets the bindData at specified [index] to the provided value. If the appropriate optional mount
   * binders bind data map doesn't exist, it creates it and then sets the bindData value.
   */
  fun setOptionalMountBindersBindData(
      bindData: Any?,
      key: BinderKey,
      optionalMountBindersSize: Int,
  ) {
    if (bindData == null) {
      return
    }

    val bindersBindData: MutableScatterMap<BinderKey, Any?> =
        optionalMountBindersBindData ?: MutableScatterMap(optionalMountBindersSize)
    bindersBindData[key] = bindData
    if (optionalMountBindersBindData == null) {
      optionalMountBindersBindData = bindersBindData
    }
  }

  /**
   * Removes the bindData for specified [key] and returns it if it was present or `null` if it
   * wasn't present or the appropriate optional mount binders bind data map was `null`.
   */
  fun removeOptionalMountBindersBindData(key: BinderKey): Any? {
    return optionalMountBindersBindData?.remove(key)
  }

  /**
   * Sets the bindData at specified [index] to the provided value. If the appropriate attach binders
   * bind data map doesn't exist, it creates it and then sets the bindData value.
   */
  fun setAttachBindersBindData(bindData: Any?, key: BinderKey, attachBindersSize: Int) {
    if (bindData == null) {
      return
    }

    val bindersBindData: MutableScatterMap<BinderKey, Any?> =
        attachBindersBindData ?: MutableScatterMap(attachBindersSize)
    bindersBindData[key] = bindData
    if (attachBindersBindData == null) {
      attachBindersBindData = bindersBindData
    }
  }

  /**
   * Removes the bindData for specified [key] and returns it if it was present or `null` if it
   * wasn't present or the appropriate attach binders bind data map was `null`.
   */
  fun removeAttachBindersBindData(key: BinderKey): Any? {
    return attachBindersBindData?.remove(key)
  }
}
