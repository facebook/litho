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
import com.facebook.rendercore.FastMath.round
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.areCompatible
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import com.facebook.rendercore.utils.MeasureSpecUtils
import kotlin.math.max
import kotlin.math.min

/** Calculates the size constraints for the child of the Recycler. */
fun ItemSizeConstraintsProviderScope.getChildSizeConstraints(
    item: CollectionItem<*>
): SizeConstraints {

  val (widthSpec, heightSpec) =
      if (collectionSize != null && !isDynamicSize) {
        // The size of the collection is determined so we can use it to measure the child for the
        // second pass.
        val measuredSize: Size = collectionSize

        val resolvedWidthSpec =
            if (isMatchingParentSize(item.renderInfo.parentWidthPercent)) {
              MeasureSpecUtils.exactly(
                  round(measuredSize.width * item.renderInfo.parentWidthPercent / 100))
            } else {
              layoutInfo.getChildWidthSpec(
                  MeasureSpecUtils.exactly(measuredSize.width), item.renderInfo)
            }
        val resolvedHeightSpec =
            if (isMatchingParentSize(item.renderInfo.parentHeightPercent)) {
              MeasureSpecUtils.exactly(
                  round(measuredSize.height * item.renderInfo.parentHeightPercent / 100))
            } else {
              layoutInfo.getChildHeightSpec(
                  MeasureSpecUtils.exactly(measuredSize.height), item.renderInfo)
            }
        resolvedWidthSpec to resolvedHeightSpec
      } else {
        // 1) We haven't measured the collection yet, delegating to the layout info to measure the
        // child for the first pass.
        // 2) We've measured the collection but the wrap mode is dynamic, so we need to ensure the
        // child is aligned with the size of the collection.
        val shouldMeasureWidthDynamically = (isDynamicSize && isVertical)
        val size = item.size
        val resolvedWidthSpec =
            if (shouldMeasureWidthDynamically) {
              if (size == null) {
                // We haven't measured the child yet, so we need to measure its actual size.
                MeasureSpecUtils.unspecified()
              } else {
                // We've known the size of the child, aligning with the size of the collection.
                MeasureSpecUtils.exactly(max(collectionSize?.width ?: 0, size.width))
              }
            } else {
              layoutInfo.getChildWidthSpec(collectionConstraints.toWidthSpec(), item.renderInfo)
            }
        val shouldMeasureHeightDynamically = (isDynamicSize && !isVertical)
        val resolvedHeightSpec =
            if (shouldMeasureHeightDynamically) {
              if (size == null) {
                // We haven't measured the child yet, so we need to measure its actual size.
                MeasureSpecUtils.unspecified()
              } else {
                // We've known the size of the child, aligning with the size of the collection.
                MeasureSpecUtils.exactly(max(collectionSize?.height ?: 0, size.height))
              }
            } else {
              layoutInfo.getChildHeightSpec(collectionConstraints.toHeightSpec(), item.renderInfo)
            }
        resolvedWidthSpec to resolvedHeightSpec
      }

  return SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec)
}

/** Validates the size constraints for the Recycler. */
private fun validateSizeConstraint(
    sizeConstraints: SizeConstraints,
    isVertical: Boolean,
    wrapInMainAxis: Boolean,
    crossAxisWrapMode: CrossAxisWrapMode
) {
  val noBoundedSizeInMainAxis =
      if (isVertical) {
        !sizeConstraints.hasBoundedHeight
      } else {
        !sizeConstraints.hasBoundedWidth
      }
  val noBoundedSizeInCrossAxis =
      if (isVertical) {
        !sizeConstraints.hasBoundedWidth
      } else {
        !sizeConstraints.hasBoundedHeight
      }
  if (!wrapInMainAxis && noBoundedSizeInMainAxis) {
    throw IllegalArgumentException(
        "${if (isVertical) "Height" else "Width"}[main axis] has to be EXACTLY OR AT MOST " +
            "for a scrolling RecyclerView.")
  }
  if (crossAxisWrapMode == CrossAxisWrapMode.NoWrap && noBoundedSizeInCrossAxis) {
    throw IllegalArgumentException(
        "Can't use Unspecified ${if (isVertical) "width" else "height"} on a scrolling " +
            "RecyclerView if dynamic measurement is not allowed, please use " +
            "[CrossAxisWrapMode.MatchFirstChild] or [CrossAxisWrapMode.Dynamic] instead.")
  }
}

/** Measures the size of the Collection. */
fun CollectionLayoutScope.calculateLayout(items: List<CollectionItem<*>>): Size {

  validateSizeConstraint(collectionConstraints, isVertical, wrapInMainAxis, crossAxisWrapMode)

  if (collectionConstraints.hasExactWidth && collectionConstraints.hasExactHeight) {
    // The collection has a fixed size, so we don't need to measure it again.
    return Size(collectionConstraints.maxWidth, collectionConstraints.maxHeight)
  }

  var sizeInMainAxis: Int =
      if (isVertical) {
        collectionConstraints.maxHeight
      } else {
        collectionConstraints.maxWidth
      }
  var sizeInCrossAxis: Int =
      if (isVertical) {
        collectionConstraints.maxWidth
      } else {
        collectionConstraints.maxHeight
      }

  if (wrapInMainAxis || crossAxisWrapMode == CrossAxisWrapMode.Dynamic) {
    // We need to measure items in the viewport to get the size of the collection.
    val filler: LayoutInfo.ViewportFiller? =
        layoutInfo.createViewportFiller(
            collectionConstraints.maxWidth, collectionConstraints.maxHeight)

    if (filler != null) {
      // The viewport info might not be reliable because user could be scrolling the list at the
      // same time, we should double check when commiting the changeset.
      var maxChildWidthInTheViewport = 0
      var maxChildHeightInTheViewport = 0
      var index: Int = max(0, layoutInfo.findFirstVisibleItemPosition())

      while (filler.wantsMore() && index < items.size) {
        val item = items[index]
        val size = getMeasuredSize(item)
        filler.add(item.renderInfo, size.width, size.height)
        maxChildWidthInTheViewport = max(maxChildWidthInTheViewport, size.width)
        maxChildHeightInTheViewport = max(maxChildHeightInTheViewport, size.height)
        index++
      }
      if (wrapInMainAxis) {
        sizeInMainAxis = min(sizeInMainAxis, filler.getFill())
      }

      if (crossAxisWrapMode == CrossAxisWrapMode.Dynamic) {
        sizeInCrossAxis =
            if (isVertical) maxChildWidthInTheViewport else maxChildHeightInTheViewport
      }
    }
  }

  if (crossAxisWrapMode == CrossAxisWrapMode.MatchFirstChild) {
    // MatchFirstChild should always respect the first child's size.
    sizeInCrossAxis =
        if (items.isNotEmpty()) {
          val size = getMeasuredSize(items[0])
          if (isVertical) {
            size.width
          } else {
            size.height
          }
        } else {
          0
        }
  }

  return if (isVertical) {
    Size(sizeInCrossAxis, sizeInMainAxis)
  } else {
    Size(sizeInMainAxis, sizeInCrossAxis)
  }
}

/** Gets the size of a collection item, either from its cached size or by measuring it. */
private fun CollectionLayoutScope.getMeasuredSize(item: CollectionItem<*>): Size {
  val constraints = getChildSizeConstraints(item)
  val itemSize =
      if (item.areSizeConstraintsCompatible(constraints)) {
        item.size
      } else {
        null
      }
  return itemSize
      ?: run {
        val output = IntArray(2)
        item.measure(constraints, output)
        Size(output[0], output[1])
      }
}

/**
 * Checks if the provided size constraints are compatible with this CollectionItem's current size
 * constraints and measured size.
 *
 * @param constraints The size constraints to compare against this item's constraints
 * @return true if the constraints are compatible with this item's size and constraints, false
 *   otherwise
 */
fun CollectionItem<*>.areSizeConstraintsCompatible(constraints: SizeConstraints): Boolean {
  val size = size ?: return false
  val localConstraints = sizeConstraints ?: return false
  return constraints.areCompatible(localConstraints, size)
}

private fun isMatchingParentSize(percent: Float): Boolean {
  return percent in 0.0..100.0
}
