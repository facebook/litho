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

/**
 * A class storing data that was created in [RenderUnit.Binder.bind] and will be passed to
 * [RenderUnit.Binder.unbind].
 */
class BindData {
  var fixedBindersBindData: Array<Any?>? = null
    private set

  var optionalMountBindersBindData: MutableMap<Class<*>, Any?>? = null
    private set

  var attachBindersBindData: MutableMap<Class<*>, Any?>? = null
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
    return fixedBindersBindData?.let {
      val bindData = it[index]
      it[index] = null
      bindData
    }
  }

  /**
   * Sets the bindData at specified [index] to the provided value. If [optionalMountBindersBindData]
   * doesn't exist, it creates it and then sets the bindData value.
   */
  fun setOptionalMountBindersBindData(
      bindData: Any?,
      key: Class<*>,
      optionalMountBindersSize: Int
  ) {
    if (bindData == null) {
      return
    }

    val bindersBindData: MutableMap<Class<*>, Any?> =
        optionalMountBindersBindData ?: LinkedHashMap(optionalMountBindersSize)
    bindersBindData[key] = bindData
    if (optionalMountBindersBindData == null) {
      optionalMountBindersBindData = bindersBindData
    }
  }

  /**
   * Removes the bindData for specified [key] and returns it if it was present or `null` if it
   * wasn't present or [optionalMountBindersBindData] map was `null`.
   */
  fun removeOptionalMountBindersBindData(key: Class<*>): Any? {
    return optionalMountBindersBindData?.remove(key)
  }

  /**
   * Sets the bindData at specified [index] to the provided value. If [attachBindersBindData]
   * doesn't exist, it creates it and then sets the bindData value.
   */
  fun setAttachBindersBindData(bindData: Any?, key: Class<*>, attachBindersSize: Int) {
    if (bindData == null) {
      return
    }

    val bindersBindData: MutableMap<Class<*>, Any?> =
        attachBindersBindData ?: LinkedHashMap(attachBindersSize)
    bindersBindData[key] = bindData
    if (attachBindersBindData == null) {
      attachBindersBindData = bindersBindData
    }
  }

  /**
   * Removes the bindData for specified [key] and returns it if it was present or `null` if it
   * wasn't present or [attachBindersBindData] map was `null`.
   */
  fun removeAttachBindersBindData(key: Class<*>): Any? {
    return attachBindersBindData?.remove(key)
  }
}
