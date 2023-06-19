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
import com.facebook.litho.widget.ComponentTreeHolder
import com.facebook.litho.widget.RecyclerBinder

/**
 * A wrapper class that exposes methods specific to testing collection children.
 *
 * @property componentTreeHolder The [ComponentTreeHolder] that holds information about the child.
 * @property index The item's index in the collection.
 * @property component The [Component] contained in this child.
 * @property id The id of the child in the collection.
 * @property isSticky True if the component can stick to the top of the collection.
 * @property isFullSpan True if the component spans the whole grid
 * @property spanSize Number of columns the component spans in a grid
 */
data class TestCollectionItem(
    private val testCollection: TestCollection,
    private val componentTreeHolder: ComponentTreeHolder,
    val index: Int,
) {

  internal val recyclerView
    get() = testCollection.recyclerView

  internal val renderInfo
    get() = componentTreeHolder.renderInfo

  internal val componentTree
    get() = componentTreeHolder.componentTree

  val id
    get() = renderInfo.getCustomAttribute(RecyclerBinder.ID_CUSTOM_ATTR_KEY)

  val component
    get() = renderInfo.component

  val isVisible
    get() = index in with(testCollection) { firstVisibleIndex..lastVisibleIndex }

  val isFullyVisible
    get() = index in with(testCollection) { firstFullyVisibleIndex..lastFullyVisibleIndex }

  val isSticky
    get() = renderInfo.isSticky

  val isFullSpan: Boolean
    get() = renderInfo.isFullSpan

  val spanSize: Int
    get() = renderInfo.spanSize
}
