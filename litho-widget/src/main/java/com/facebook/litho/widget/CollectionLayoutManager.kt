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

import androidx.annotation.UiThread
import androidx.recyclerview.widget.OrientationHelper
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.widget.collection.CrossAxisWrapMode
import com.facebook.rendercore.FastMath.round
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import com.facebook.rendercore.utils.MeasureSpecUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

/**
 * A layout manager responsible for measuring and laying out collection components.
 *
 * This class handles the measurement of [CollectionRecyclerComponent] and its children, supporting
 * different wrapping behaviors in both main and cross axes.
 *
 * @param layoutInfo Information about the layout configuration
 * @param wrapInMainAxis Whether the collection should wrap its content in the main scrolling axis
 * @param wrapInCrossAxis The wrapping behavior for the cross axis (perpendicular to scrolling)
 */
@ExperimentalLithoApi
class CollectionLayoutManager(
    val layoutInfo: LayoutInfo,
    private val wrapInMainAxis: Boolean = false,
    private val wrapInCrossAxis: CrossAxisWrapMode = CrossAxisWrapMode.NoWrap
) {

  private var lastSizeConstraints: SizeConstraints = SizeConstraints()
  private val isMeasured = AtomicBoolean(false)
  @Volatile private var lastMeasuredSize: Size = Size(0, 0)
  private val isVertical: Boolean = layoutInfo.getScrollDirection() == OrientationHelper.VERTICAL
  private val isDynamicSize: Boolean = (wrapInCrossAxis == CrossAxisWrapMode.Dynamic)

  /**
   * To avoid any side effects when measuring the Recycler with a preview list, we'll have to commit
   * the measured size along with the submission of changesets, and re-measure the Recycler when the
   * size is no longer valid.
   */
  @UiThread
  fun commitMeasureResult(size: Size) {
    lastMeasuredSize = size
    isMeasured.compareAndSet(false, true)
  }

  /** Returns true if we found a larger size of children since the last measurement. */
  fun isDynamicSizeChanged(width: Int, height: Int): Boolean {
    if (wrapInCrossAxis != CrossAxisWrapMode.Dynamic) {
      return false
    }
    val measuredSize = lastMeasuredSize
    return isMeasured.get() &&
        ((isVertical && width > measuredSize.width) ||
            (!isVertical && height > measuredSize.height))
  }

  /** Submit a larger size of children since the last measurement. */
  fun submitDynamicSize(width: Int, height: Int) {
    if (wrapInCrossAxis != CrossAxisWrapMode.Dynamic) {
      return
    }
    val measuredSize = lastMeasuredSize
    if (isVertical) {
      commitMeasureResult(Size(max(width, measuredSize.width), measuredSize.height))
    } else {
      commitMeasureResult(Size(measuredSize.width, max(height, measuredSize.height)))
    }
  }

  // Indicates that we have measured the collection and don't need to re-measure it anymore.
  private fun isMeasured(): Boolean =
      isMeasured.get() && (wrapInCrossAxis != CrossAxisWrapMode.Dynamic)

  private fun isMatchingParentSize(percent: Float): Boolean {
    return percent in 0.0..100.0
  }

  fun getChildSizeConstraints(
      child: CollectionItem<*>,
      sizeConstraints: SizeConstraints = lastSizeConstraints,
  ): SizeConstraints {

    val (widthSpec, heightSpec) =
        if (isMeasured()) {
          // The size of the collection is determined so we can use it to measure the child for the
          // second pass.
          val measuredSize = lastMeasuredSize

          val resolvedWidthSpec =
              if (isMatchingParentSize(child.renderInfo.parentWidthPercent)) {
                MeasureSpecUtils.exactly(
                    round(measuredSize.width * child.renderInfo.parentWidthPercent / 100))
              } else if (isDynamicSize && isVertical) {
                MeasureSpecUtils.unspecified()
              } else {
                layoutInfo.getChildWidthSpec(
                    MeasureSpecUtils.exactly(measuredSize.width), child.renderInfo)
              }
          val resolvedHeightSpec =
              if (isMatchingParentSize(child.renderInfo.parentHeightPercent)) {
                MeasureSpecUtils.exactly(
                    round(measuredSize.height * child.renderInfo.parentHeightPercent / 100))
              } else if (isDynamicSize && !isVertical) {
                MeasureSpecUtils.unspecified()
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
                child.size?.let { MeasureSpecUtils.exactly(max(lastMeasuredSize.width, it.width)) }
                    ?: MeasureSpecUtils.unspecified()
              } else {
                layoutInfo.getChildWidthSpec(sizeConstraints.toWidthSpec(), child.renderInfo)
              }
          val resolvedHeightSpec =
              if (isDynamicSize && !isVertical) {
                child.size?.let {
                  MeasureSpecUtils.exactly(max(lastMeasuredSize.height, it.height))
                } ?: MeasureSpecUtils.unspecified()
              } else {
                layoutInfo.getChildHeightSpec(sizeConstraints.toHeightSpec(), child.renderInfo)
              }

          resolvedWidthSpec to resolvedHeightSpec
        }

    return SizeConstraints.fromMeasureSpecs(widthSpec, heightSpec)
  }

  private fun validateSizeConstraint(sizeConstraints: SizeConstraints) {
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
    if (wrapInCrossAxis == CrossAxisWrapMode.NoWrap && noBoundedSizeInCrossAxis) {
      throw IllegalArgumentException(
          "Can't use Unspecified ${if (isVertical) "width" else "height"} on a scrolling " +
              "RecyclerView if dynamic measurement is not allowed, please use " +
              "[CrossAxisWrapMode.MatchFirstChild] or [CrossAxisWrapMode.Dynamic] instead.")
    }
  }

  /** Measures the size of the Recycler. */
  fun measure(
      items: List<CollectionItem<*>>,
      parentSizeConstraints: SizeConstraints = lastSizeConstraints
  ): Size {

    validateSizeConstraint(parentSizeConstraints)

    synchronized(this) {
      if (lastSizeConstraints != parentSizeConstraints) {
        lastSizeConstraints = parentSizeConstraints
      }
    }

    if (parentSizeConstraints.hasExactWidth && parentSizeConstraints.hasExactHeight) {
      // The recycler has a fixed size, so we don't need to measure it again.
      return Size(parentSizeConstraints.maxWidth, parentSizeConstraints.maxHeight)
    }

    val sizeInMainAxis: Int
    val sizeInCrossAxis: Int
    if (isMeasured()) {
      // We've measured the recycler once and size never changed after that, so we can check if we
      // can reuse the size from the last measurement.
      val measuredSize = lastMeasuredSize

      if (wrapInCrossAxis == CrossAxisWrapMode.MatchFirstChild) {
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
              parentSizeConstraints.maxWidth, parentSizeConstraints.maxHeight)

      if (filler != null) {
        // The viewport info might not be reliable because user could be scrolling the list at the
        // same time, we should double check when commiting the changeset.
        var maxChildWidthInTheViewport = 0
        var maxChildHeightInTheViewport = 0
        var index: Int = max(0, layoutInfo.findFirstVisibleItemPosition())
        while (filler.wantsMore() && index < items.size) {
          val item = items[index]
          val output = IntArray(2)
          item.prepareSync(
              getChildSizeConstraints(child = item, sizeConstraints = parentSizeConstraints),
              output)
          val size = Size(output[0], output[1])
          filler.add(item.renderInfo, size.width, size.height)
          maxChildWidthInTheViewport = max(maxChildWidthInTheViewport, size.width)
          maxChildHeightInTheViewport = max(maxChildHeightInTheViewport, size.height)
          index++
        }
        sizeInMainAxis = filler.getFill()
        // This is only used for dynamic size, so we can use the max child size in the viewport.
        sizeInCrossAxis =
            if (isVertical) maxChildWidthInTheViewport else maxChildHeightInTheViewport
      } else {
        sizeInMainAxis =
            if (isVertical) parentSizeConstraints.maxHeight else parentSizeConstraints.maxWidth
        sizeInCrossAxis =
            if (isVertical) parentSizeConstraints.maxWidth else parentSizeConstraints.maxHeight
      }
    }

    val width: Int
    val height: Int
    if (isVertical) {
      width =
          when (wrapInCrossAxis) {
            CrossAxisWrapMode.NoWrap -> parentSizeConstraints.maxWidth
            CrossAxisWrapMode.MatchFirstChild ->
                if (items.isNotEmpty()) {
                  items[0].size?.width ?: 0
                } else 0
            CrossAxisWrapMode.Dynamic -> sizeInCrossAxis
          }
      height =
          if (wrapInMainAxis) {
            min(sizeInMainAxis, parentSizeConstraints.maxHeight)
          } else {
            parentSizeConstraints.maxHeight
          }
    } else {
      width =
          if (wrapInMainAxis) {
            min(sizeInMainAxis, parentSizeConstraints.maxWidth)
          } else {
            parentSizeConstraints.maxWidth
          }
      height =
          when (wrapInCrossAxis) {
            CrossAxisWrapMode.NoWrap -> parentSizeConstraints.maxHeight
            CrossAxisWrapMode.MatchFirstChild ->
                if (items.isNotEmpty()) {
                  items[0].size?.height ?: 0
                } else 0
            CrossAxisWrapMode.Dynamic -> sizeInCrossAxis
          }
    }
    return Size(width, height)
  }
}
