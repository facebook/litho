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

import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.widget.collection.CrossAxisWrapMode
import com.facebook.rendercore.FastMath.round
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import com.facebook.rendercore.utils.MeasureSpecUtils
import kotlin.math.max
import kotlin.math.min

/**
 * A manager for measurement strategies of collection components.
 *
 * This class handles the measurement of [CollectionRecyclerComponent] and its children, supporting
 * different wrapping behaviors in both main and cross axes.
 */
@ExperimentalLithoApi
object CollectionLayoutManager {

  /** Calculates the size constraints for the child of the Recycler. */
  fun getChildSizeConstraints(
      layoutInfo: LayoutInfo,
      collectionSizeConstraints: SizeConstraints,
      collectionSize: Size?,
      child: CollectionItem<*>,
      isVertical: Boolean,
      isDynamicSize: Boolean,
  ): SizeConstraints {

    val (widthSpec, heightSpec) =
        if (collectionSize != null && !isDynamicSize) {
          // The size of the collection is determined so we can use it to measure the child for the
          // second pass.
          val measuredSize: Size = collectionSize

          val resolvedWidthSpec =
              if (isMatchingParentSize(child.renderInfo.parentWidthPercent)) {
                MeasureSpecUtils.exactly(
                    round(measuredSize.width * child.renderInfo.parentWidthPercent / 100))
              } else {
                layoutInfo.getChildWidthSpec(
                    MeasureSpecUtils.exactly(measuredSize.width), child.renderInfo)
              }
          val resolvedHeightSpec =
              if (isMatchingParentSize(child.renderInfo.parentHeightPercent)) {
                MeasureSpecUtils.exactly(
                    round(measuredSize.height * child.renderInfo.parentHeightPercent / 100))
              } else {
                layoutInfo.getChildHeightSpec(
                    MeasureSpecUtils.exactly(measuredSize.height), child.renderInfo)
              }
          resolvedWidthSpec to resolvedHeightSpec
        } else {
          // We haven't measured the collection yet, so we need to use the parent constraints to
          // measure the child for the first pass.
          val resolvedWidthSpec =
              if (isDynamicSize && isVertical) {
                child.size?.let {
                  MeasureSpecUtils.exactly(max(collectionSize?.width ?: 0, it.width))
                } ?: MeasureSpecUtils.unspecified()
              } else {
                layoutInfo.getChildWidthSpec(
                    collectionSizeConstraints.toWidthSpec(), child.renderInfo)
              }
          val resolvedHeightSpec =
              if (isDynamicSize && !isVertical) {
                child.size?.let {
                  MeasureSpecUtils.exactly(max(collectionSize?.height ?: 0, it.height))
                } ?: MeasureSpecUtils.unspecified()
              } else {
                layoutInfo.getChildHeightSpec(
                    collectionSizeConstraints.toHeightSpec(), child.renderInfo)
              }

          resolvedWidthSpec to resolvedHeightSpec
        }

    return SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec)
  }

  /** Validates the size constraints for the Recycler. */
  fun validateSizeConstraint(
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

  /** Measures the size of the Recycler. */
  fun measure(
      layoutInfo: LayoutInfo,
      items: List<CollectionItem<*>>,
      collectionSizeConstraints: SizeConstraints,
      collectionSize: Size?,
      isVertical: Boolean,
      wrapInMainAxis: Boolean,
      crossAxisWrapMode: CrossAxisWrapMode,
  ): Size {

    if (collectionSizeConstraints.hasExactWidth && collectionSizeConstraints.hasExactHeight) {
      // The recycler has a fixed size, so we don't need to measure it again.
      return Size(collectionSizeConstraints.maxWidth, collectionSizeConstraints.maxHeight)
    }

    val sizeInMainAxis: Int
    val sizeInCrossAxis: Int
    if (collectionSize != null) {
      // We've measured the recycler once and size never changed after that, so we can check if we
      // can reuse the size from the last measurement.
      val measuredSize: Size = collectionSize

      if (crossAxisWrapMode == CrossAxisWrapMode.MatchFirstChild) {
        val wasFirstChildSizeChanged =
            if (items.isNotEmpty()) {
              if (isVertical) {
                val width = items[0].size?.width ?: 0
                measuredSize.width != width
              } else {
                val height = items[0].size?.height ?: 0
                measuredSize.height != height
              }
            } else {
              true
            }
        if (!wrapInMainAxis && !wasFirstChildSizeChanged) {
          return Size(measuredSize.width, measuredSize.height)
        }
      }
      sizeInMainAxis = if (isVertical) measuredSize.height else measuredSize.width
      sizeInCrossAxis = if (isVertical) measuredSize.width else measuredSize.height
    } else {
      // We haven't measured the recycler yet, so we need to fill the viewport to get the actual
      // size. TODO: how about the entire list has changed and the size is becoming smaller?
      val filler: LayoutInfo.ViewportFiller? =
          layoutInfo.createViewportFiller(
              collectionSizeConstraints.maxWidth, collectionSizeConstraints.maxHeight)

      if (filler != null) {
        // The viewport info might not be reliable because user could be scrolling the list at the
        // same time, we should double check when commiting the changeset.
        var maxChildWidthInTheViewport = 0
        var maxChildHeightInTheViewport = 0
        var index: Int = max(0, layoutInfo.findFirstVisibleItemPosition())
        while (filler.wantsMore() && index < items.size) {
          val item = items[index]
          val output = IntArray(2)
          item.measure(
              getChildSizeConstraints(
                  layoutInfo = layoutInfo,
                  collectionSizeConstraints = collectionSizeConstraints,
                  collectionSize = collectionSize,
                  child = item,
                  isVertical = isVertical,
                  isDynamicSize = crossAxisWrapMode == CrossAxisWrapMode.Dynamic),
              output)
          val width = output[0]
          val height = output[1]
          filler.add(item.renderInfo, width, height)
          maxChildWidthInTheViewport = max(maxChildWidthInTheViewport, width)
          maxChildHeightInTheViewport = max(maxChildHeightInTheViewport, height)
          index++
        }
        sizeInMainAxis = filler.getFill()
        // This is only used for dynamic size, so we can use the max child size in the viewport.
        sizeInCrossAxis =
            if (isVertical) maxChildWidthInTheViewport else maxChildHeightInTheViewport
      } else {
        sizeInMainAxis =
            if (isVertical) collectionSizeConstraints.maxHeight
            else collectionSizeConstraints.maxWidth
        sizeInCrossAxis =
            if (isVertical) collectionSizeConstraints.maxWidth
            else collectionSizeConstraints.maxHeight
      }
    }

    val width: Int
    val height: Int
    if (isVertical) {
      width =
          when (crossAxisWrapMode) {
            CrossAxisWrapMode.NoWrap -> collectionSizeConstraints.maxWidth
            CrossAxisWrapMode.MatchFirstChild ->
                if (items.isNotEmpty()) {
                  items[0].size?.width ?: 0
                } else 0
            CrossAxisWrapMode.Dynamic -> sizeInCrossAxis
          }
      height =
          if (wrapInMainAxis) {
            min(sizeInMainAxis, collectionSizeConstraints.maxHeight)
          } else {
            collectionSizeConstraints.maxHeight
          }
    } else {
      width =
          if (wrapInMainAxis) {
            min(sizeInMainAxis, collectionSizeConstraints.maxWidth)
          } else {
            collectionSizeConstraints.maxWidth
          }
      height =
          when (crossAxisWrapMode) {
            CrossAxisWrapMode.NoWrap -> collectionSizeConstraints.maxHeight
            CrossAxisWrapMode.MatchFirstChild ->
                if (items.isNotEmpty()) {
                  items[0].size?.height ?: 0
                } else 0
            CrossAxisWrapMode.Dynamic -> sizeInCrossAxis
          }
    }
    return Size(width, height)
  }

  private fun isMatchingParentSize(percent: Float): Boolean {
    return percent in 0.0..100.0
  }
}
