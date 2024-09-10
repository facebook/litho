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

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.annotations.Hook
import java.util.UUID

/**
 * This hook creates a set of references, one for each focusable component that can be used for
 * changing the default focus order. Use with [Style.focusOrder]
 */
@Hook
fun ComponentScope.useFocusOrder(): FocusOrderModelProvider {
  val uuidList = List(15) { UUID.randomUUID().toString() }
  return useCached { FocusOrderModelProvider(uuidList) }
}

@DataClassGenerate
data class FocusOrderModel
internal constructor(val key: String, val previousKey: String? = null, val nextKey: String? = null)

/** Create multiple [FocusOrderModel]s, which can to be used to specify a focus traversal order. */
class FocusOrderModelProvider(private val uuidList: List<String>) {

  operator fun component1(): FocusOrderModel =
      FocusOrderModel(key = uuidList[0], nextKey = uuidList[1])

  operator fun component2(): FocusOrderModel =
      FocusOrderModel(key = uuidList[1], nextKey = uuidList[2], previousKey = uuidList[0])

  operator fun component3(): FocusOrderModel =
      FocusOrderModel(key = uuidList[2], nextKey = uuidList[3], previousKey = uuidList[1])

  operator fun component4(): FocusOrderModel =
      FocusOrderModel(key = uuidList[3], nextKey = uuidList[4], previousKey = uuidList[2])

  operator fun component5(): FocusOrderModel =
      FocusOrderModel(key = uuidList[4], nextKey = uuidList[5], previousKey = uuidList[3])

  operator fun component6(): FocusOrderModel =
      FocusOrderModel(key = uuidList[5], nextKey = uuidList[6], previousKey = uuidList[4])

  operator fun component7(): FocusOrderModel =
      FocusOrderModel(key = uuidList[6], nextKey = uuidList[7], previousKey = uuidList[5])

  operator fun component8(): FocusOrderModel =
      FocusOrderModel(key = uuidList[7], nextKey = uuidList[8], previousKey = uuidList[6])

  operator fun component9(): FocusOrderModel =
      FocusOrderModel(key = uuidList[8], nextKey = uuidList[9], previousKey = uuidList[7])

  operator fun component10(): FocusOrderModel =
      FocusOrderModel(key = uuidList[9], nextKey = uuidList[10], previousKey = uuidList[8])

  operator fun component11(): FocusOrderModel =
      FocusOrderModel(key = uuidList[10], nextKey = uuidList[11], previousKey = uuidList[9])

  operator fun component12(): FocusOrderModel =
      FocusOrderModel(key = uuidList[11], nextKey = uuidList[12], previousKey = uuidList[10])

  operator fun component13(): FocusOrderModel =
      FocusOrderModel(key = uuidList[12], nextKey = uuidList[13], previousKey = uuidList[11])

  operator fun component14(): FocusOrderModel =
      FocusOrderModel(key = uuidList[13], nextKey = uuidList[14], previousKey = uuidList[12])

  operator fun component15(): FocusOrderModel =
      FocusOrderModel(key = uuidList[14], previousKey = uuidList[13])
}
