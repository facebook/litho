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

package com.facebook.litho.testing

import com.facebook.litho.Component
import com.facebook.litho.DebugComponent
import com.facebook.litho.LithoViewTestHelper

object TestCollectionDescriptionHelper {

  private const val DEPTH_3_LEADING_SPACE = "      "
  private const val ITEM_STARTING_DEPTH = 3

  fun collectionToString(testCollection: TestCollection): String {
    var hierarchyString = testCollection.recyclerView.toString()
    testCollection.items.forEach { item ->
      hierarchyString = "${hierarchyString}${itemToString(item)}"
    }
    return hierarchyString
  }

  private fun itemToString(item: TestCollectionItem): String {
    val testCollectionItemHeader =
        "\n└── index ${item.index}: Collection Item (id: ${item.id}, isVisible: ${item.isVisible})"
    // TODO: support views, in addition to components
    return "$testCollectionItemHeader${getComponentInfo(item)}"
  }

  private fun getComponentInfo(item: TestCollectionItem): String {
    val debugComponent =
        DebugComponent.getRootInstance(item.componentTree)
            ?: return fallbackComponentPrintInfo(item.component)

    return LithoViewTestHelper.rootInstanceToString(debugComponent, false, ITEM_STARTING_DEPTH)
  }

  private fun fallbackComponentPrintInfo(component: Component): String {
    // This can be null when working with a mock, so we fallback in that case to the class name info
    val componentString = component.toString() ?: component::class.java.simpleName
    return "\n$DEPTH_3_LEADING_SPACE$componentString{${component.hashCode()}}\n"
  }
}
