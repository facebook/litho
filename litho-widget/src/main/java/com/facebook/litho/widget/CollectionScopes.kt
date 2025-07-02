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

package com.facebook.litho.widget

import com.facebook.litho.widget.collection.CrossAxisWrapMode
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints

/**
 * Class that provides scope and context information for item size constraint calculations in
 * collection layouts. This class encapsulates the essential parameters needed to determine how
 * individual items should be sized within a collection.
 *
 * @param layoutInfo Information about the layout configuration and properties
 * @param collectionConstraints Size constraints applied to the entire collection
 * @param collectionSize The actual size of the collection, null if not yet determined
 * @param isVertical Whether the collection is oriented vertically (true) or horizontally (false)
 * @param isDynamicSize Whether the collection supports dynamic size
 */
sealed class ItemSizeConstraintsProviderScope(
    val layoutInfo: LayoutInfo,
    val collectionConstraints: SizeConstraints,
    val collectionSize: Size?,
    val isVertical: Boolean,
    val isDynamicSize: Boolean
)

class CollectionLayoutData(
    layoutInfo: LayoutInfo,
    collectionConstraints: SizeConstraints,
    collectionSize: Size?,
    isVertical: Boolean,
    isDynamicSize: Boolean
) :
    ItemSizeConstraintsProviderScope(
        layoutInfo = layoutInfo,
        collectionConstraints = collectionConstraints,
        collectionSize = collectionSize,
        isVertical = isVertical,
        isDynamicSize = isDynamicSize)

class CollectionLayoutScope(
    layoutInfo: LayoutInfo,
    collectionConstraints: SizeConstraints,
    collectionSize: Size?,
    isVertical: Boolean,
    val wrapInMainAxis: Boolean,
    val crossAxisWrapMode: CrossAxisWrapMode,
) :
    ItemSizeConstraintsProviderScope(
        layoutInfo = layoutInfo,
        collectionConstraints = collectionConstraints,
        collectionSize = collectionSize,
        isVertical = isVertical,
        isDynamicSize = crossAxisWrapMode == CrossAxisWrapMode.Dynamic)
