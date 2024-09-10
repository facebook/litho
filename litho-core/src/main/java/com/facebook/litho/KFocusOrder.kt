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

import com.facebook.litho.annotations.Hook

/**
 * This hook creates a set of references, one for each focusable component that can be used for
 * changing the default focus order. Use with [Style.focusOrder]
 */
@Hook
fun ComponentScope.useFocusOrder(): FocusOrderModelProvider =
    FocusOrderModelProvider(context.globalKey)

class FocusOrderModel(val key: String) {
  var previous: FocusOrderModel? = null
    internal set

  var next: FocusOrderModel? = null
    internal set
}

/** Create multiple [FocusOrderModel]s, which can to be used to specify a focus traversal order. */
class FocusOrderModelProvider(private val key: String) {
  private val models: MutableMap<Int, FocusOrderModel> = mutableMapOf()

  private fun getOrCreate(index: Int): FocusOrderModel =
      models.getOrPut(index) {
        FocusOrderModel(key = "$key:$index").also { current ->
          val previous: FocusOrderModel? = getPrevious(index)
          current.previous = previous
          previous?.let { it.next = current }
        }
      }

  private fun getPrevious(index: Int) =
      if (index > 0) {
        getOrCreate(index - 1)
      } else {
        models[0]
      }

  operator fun component1(): FocusOrderModel = getOrCreate(0)

  operator fun component2(): FocusOrderModel = getOrCreate(1)

  operator fun component3(): FocusOrderModel = getOrCreate(2)

  operator fun component4(): FocusOrderModel = getOrCreate(3)

  operator fun component5(): FocusOrderModel = getOrCreate(4)

  operator fun component6(): FocusOrderModel = getOrCreate(5)

  operator fun component7(): FocusOrderModel = getOrCreate(6)

  operator fun component8(): FocusOrderModel = getOrCreate(7)

  operator fun component9(): FocusOrderModel = getOrCreate(8)

  operator fun component10(): FocusOrderModel = getOrCreate(9)

  operator fun component11(): FocusOrderModel = getOrCreate(10)

  operator fun component12(): FocusOrderModel = getOrCreate(11)

  operator fun component13(): FocusOrderModel = getOrCreate(12)

  operator fun component14(): FocusOrderModel = getOrCreate(13)

  operator fun component15(): FocusOrderModel = getOrCreate(14)
}
