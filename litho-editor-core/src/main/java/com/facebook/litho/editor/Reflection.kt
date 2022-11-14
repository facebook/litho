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

package com.facebook.litho.editor

import java.lang.reflect.Field

object Reflection {
  fun <T> getValueUNSAFE(f: Field, node: Any?): T? {
    return try {
      val oldAccessibility = f.isAccessible
      f.isAccessible = true
      var value: T? = null
      f[node]?.let { v -> value = v as T }

      f.isAccessible = oldAccessibility
      value
    } catch (e: IllegalArgumentException) {
      throw RuntimeException(e)
    } catch (e: IllegalAccessException) {
      throw RuntimeException(e)
    }
  }

  fun <T> setValueUNSAFE(f: Field, node: Any?, value: T) {
    try {
      val oldAccessibility = f.isAccessible
      f.isAccessible = true
      f[node] = value
      f.isAccessible = oldAccessibility
    } catch (e: IllegalArgumentException) {
      throw RuntimeException(e)
    } catch (e: IllegalAccessException) {
      throw RuntimeException(e)
    } catch (e: SecurityException) {
      throw RuntimeException(e)
    }
  }

  fun geFieldUNSAFE(clazz: Class<*>, field: String): Field? {
    return try {
      clazz.getDeclaredField(field)
    } catch (e: SecurityException) {
      throw RuntimeException(e)
    } catch (e: NoSuchFieldException) {
      throw RuntimeException(e)
    }
  }
}
