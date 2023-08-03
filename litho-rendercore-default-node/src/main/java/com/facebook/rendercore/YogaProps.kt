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

import androidx.annotation.Px
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaConstants
import com.facebook.yoga.YogaDirection
import com.facebook.yoga.YogaDisplay
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaNode
import com.facebook.yoga.YogaPositionType
import com.facebook.yoga.YogaWrap
import java.util.ArrayList

class YogaProps : YogaRootLayoutParams, Cloneable {

  private var privateFlags: Long = 0

  @Px private var _widthPx = 0
  private var _widthPercent = 0f

  @Px private var _minWidthPx = 0
  private var _minWidthPercent = 0f

  @Px private var _maxWidthPx = 0
  private var _maxWidthPercent = 0f

  @Px private var _heightPx = 0
  private var _heightPercent = 0f

  @Px private var _minHeightPx = 0
  private var _minHeightPercent = 0f

  @Px private var _maxHeightPx = 0
  private var _maxHeightPercent = 0f
  private var _flex = 0f
  private var _flexGrow = 0f
  private var _flexShrink = 0f

  @Px private var _flexBasisPx = 0
  private var _flexBasisPercent = 0f
  private var _aspectRatio = 0f
  private var _layoutDirection: YogaDirection? = null
  private var _alignSelf: YogaAlign? = null
  private var _positionType: YogaPositionType? = null
  private var positions: Edges? = null
  private var margins: Edges? = null
  private var marginPercents: Edges? = null
  private var marginAutos: MutableList<YogaEdge>? = null
  private var paddings: Edges? = null
  private var paddingPercents: Edges? = null
  private var positionPercents: Edges? = null
  private var _flexDirection: YogaFlexDirection? = null
  private var _alignItems: YogaAlign? = null
  private var _alignContent: YogaAlign? = null
  private var _justifyContent: YogaJustify? = null
  private var _wrap: YogaWrap? = null
  private var display: YogaDisplay? = null
  private var _isReferenceBaseline = false
  private var _useHeightAsBaseline = false
  private var _usePercentDimensAtRoot = false

  fun flexDirection(flexDirection: YogaFlexDirection?) {
    privateFlags = privateFlags or PFLAG_FLEX_DIRECTION_IS_SET
    _flexDirection = flexDirection
  }

  fun widthPx(@Px width: Int) {
    privateFlags = privateFlags or PFLAG_WIDTH_IS_SET
    _widthPx = width
  }

  fun widthPercent(percent: Float) {
    privateFlags = privateFlags or PFLAG_WIDTH_PERCENT_IS_SET
    _widthPercent = percent
  }

  fun widthAuto() {
    privateFlags = privateFlags or PFLAG_WIDTH_AUTO
  }

  fun minWidthPx(@Px minWidth: Int) {
    privateFlags = privateFlags or PFLAG_MIN_WIDTH_IS_SET
    _minWidthPx = minWidth
  }

  fun maxWidthPx(@Px maxWidth: Int) {
    privateFlags = privateFlags or PFLAG_MAX_WIDTH_IS_SET
    _maxWidthPx = maxWidth
  }

  fun minWidthPercent(percent: Float) {
    privateFlags = privateFlags or PFLAG_MIN_WIDTH_PERCENT_IS_SET
    _minWidthPercent = percent
  }

  fun maxWidthPercent(percent: Float) {
    privateFlags = privateFlags or PFLAG_MAX_WIDTH_PERCENT_IS_SET
    _maxWidthPercent = percent
  }

  fun heightPx(@Px height: Int) {
    privateFlags = privateFlags or PFLAG_HEIGHT_IS_SET
    _heightPx = height
  }

  fun heightPercent(percent: Float) {
    privateFlags = privateFlags or PFLAG_HEIGHT_PERCENT_IS_SET
    _heightPercent = percent
  }

  fun heightAuto() {
    privateFlags = privateFlags or PFLAG_HEIGHT_AUTO
  }

  fun minHeightPx(@Px minHeight: Int) {
    privateFlags = privateFlags or PFLAG_MIN_HEIGHT_IS_SET
    _minHeightPx = minHeight
  }

  fun maxHeightPx(@Px maxHeight: Int) {
    privateFlags = privateFlags or PFLAG_MAX_HEIGHT_IS_SET
    _maxHeightPx = maxHeight
  }

  fun minHeightPercent(percent: Float) {
    privateFlags = privateFlags or PFLAG_MIN_HEIGHT_PERCENT_IS_SET
    _minHeightPercent = percent
  }

  fun maxHeightPercent(percent: Float) {
    privateFlags = privateFlags or PFLAG_MAX_HEIGHT_PERCENT_IS_SET
    _maxHeightPercent = percent
  }

  fun layoutDirection(direction: YogaDirection?) {
    privateFlags = privateFlags or PFLAG_LAYOUT_DIRECTION_IS_SET
    _layoutDirection = direction
  }

  fun alignSelf(alignSelf: YogaAlign?) {
    privateFlags = privateFlags or PFLAG_ALIGN_SELF_IS_SET
    _alignSelf = alignSelf
  }

  fun flex(flex: Float) {
    privateFlags = privateFlags or PFLAG_FLEX_IS_SET
    _flex = flex
  }

  fun flexGrow(flexGrow: Float) {
    privateFlags = privateFlags or PFLAG_FLEX_GROW_IS_SET
    _flexGrow = flexGrow
  }

  fun flexShrink(flexShrink: Float) {
    privateFlags = privateFlags or PFLAG_FLEX_SHRINK_IS_SET
    _flexShrink = flexShrink
  }

  fun flexBasisPx(@Px flexBasis: Int) {
    privateFlags = privateFlags or PFLAG_FLEX_BASIS_IS_SET
    _flexBasisPx = flexBasis
  }

  fun flexBasisPercent(percent: Float) {
    privateFlags = privateFlags or PFLAG_FLEX_BASIS_PERCENT_IS_SET
    _flexBasisPercent = percent
  }

  fun aspectRatio(aspectRatio: Float) {
    privateFlags = privateFlags or PFLAG_ASPECT_RATIO_IS_SET
    _aspectRatio = aspectRatio
  }

  fun positionType(positionType: YogaPositionType?) {
    privateFlags = privateFlags or PFLAG_POSITION_TYPE_IS_SET
    _positionType = positionType
  }

  fun positionPx(edge: YogaEdge?, @Px position: Int) {
    privateFlags = privateFlags or PFLAG_POSITION_IS_SET
    if (positions == null) {
      positions = Edges()
    }
    positions!![edge] = position.toFloat()
  }

  fun positionPercent(edge: YogaEdge?, percent: Float) {
    privateFlags = privateFlags or PFLAG_POSITION_PERCENT_IS_SET
    if (positionPercents == null) {
      positionPercents = Edges()
    }
    positionPercents!![edge] = percent
  }

  fun paddingPx(edge: YogaEdge?, @Px padding: Int) {
    privateFlags = privateFlags or PFLAG_PADDING_IS_SET
    if (paddings == null) {
      paddings = Edges()
    }
    paddings!![edge] = padding.toFloat()
  }

  fun paddingPercent(edge: YogaEdge?, percent: Float) {
    privateFlags = privateFlags or PFLAG_PADDING_PERCENT_IS_SET
    if (paddingPercents == null) {
      paddingPercents = Edges()
    }
    paddingPercents!![edge] = percent
  }

  fun marginPx(edge: YogaEdge?, @Px margin: Int) {
    privateFlags = privateFlags or PFLAG_MARGIN_IS_SET
    if (margins == null) {
      margins = Edges()
    }
    margins!![edge] = margin.toFloat()
  }

  fun marginPercent(edge: YogaEdge?, percent: Float) {
    privateFlags = privateFlags or PFLAG_MARGIN_PERCENT_IS_SET
    if (marginPercents == null) {
      marginPercents = Edges()
    }
    marginPercents!![edge] = percent
  }

  fun marginAuto(edge: YogaEdge) {
    privateFlags = privateFlags or PFLAG_MARGIN_AUTO_IS_SET
    if (marginAutos == null) {
      marginAutos = ArrayList(2)
    }
    marginAutos!!.add(edge)
  }

  fun isReferenceBaseline(isReferenceBaseline: Boolean) {
    privateFlags = privateFlags or PFLAG_IS_REFERENCE_BASELINE_IS_SET
    _isReferenceBaseline = isReferenceBaseline
  }

  fun useHeightAsBaseline(useHeightAsBaseline: Boolean) {
    privateFlags = privateFlags or PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET
    _useHeightAsBaseline = useHeightAsBaseline
  }

  fun justifyContent(justifyContent: YogaJustify?) {
    privateFlags = privateFlags or PFLAG_JUSTIFY_CONTENT_IS_SET
    _justifyContent = justifyContent
  }

  fun alignItems(alignItems: YogaAlign?) {
    privateFlags = privateFlags or PFLAG_ALIGN_ITEMS_IS_SET
    _alignItems = alignItems
  }

  fun alignContent(alignContent: YogaAlign?) {
    privateFlags = privateFlags or PFLAG_ALIGN_CONTENT_IS_SET
    _alignContent = alignContent
  }

  fun wrap(flexWrap: YogaWrap?) {
    privateFlags = privateFlags or PFLAG_WRAP_IS_SET
    _wrap = flexWrap
  }

  fun setDisplay(yogaDisplay: YogaDisplay?) {
    display = yogaDisplay
  }

  fun setUsePercentDimensAtRoot() {
    _usePercentDimensAtRoot = true
  }

  fun applyToNode(context: LayoutContext<*>?, yogaNode: YogaNode) {
    if (privateFlags and PFLAG_WIDTH_IS_SET != 0L) {
      yogaNode.setWidth(_widthPx.toFloat())
    }
    if (privateFlags and PFLAG_WIDTH_PERCENT_IS_SET != 0L) {
      yogaNode.setWidthPercent(_widthPercent)
    }
    if (privateFlags and PFLAG_WIDTH_AUTO != 0L) {
      yogaNode.setWidthAuto()
    }
    if (privateFlags and PFLAG_MIN_WIDTH_IS_SET != 0L) {
      yogaNode.setMinWidth(_minWidthPx.toFloat())
    }
    if (privateFlags and PFLAG_MIN_WIDTH_PERCENT_IS_SET != 0L) {
      yogaNode.setMinWidthPercent(_minWidthPercent)
    }
    if (privateFlags and PFLAG_MAX_WIDTH_IS_SET != 0L) {
      yogaNode.setMaxWidth(_maxWidthPx.toFloat())
    }
    if (privateFlags and PFLAG_MAX_WIDTH_PERCENT_IS_SET != 0L) {
      yogaNode.setMaxWidthPercent(_maxWidthPercent)
    }
    if (privateFlags and PFLAG_HEIGHT_IS_SET != 0L) {
      yogaNode.setHeight(_heightPx.toFloat())
    }
    if (privateFlags and PFLAG_HEIGHT_PERCENT_IS_SET != 0L) {
      yogaNode.setHeightPercent(_heightPercent)
    }
    if (privateFlags and PFLAG_HEIGHT_AUTO != 0L) {
      yogaNode.setHeightAuto()
    }
    if (privateFlags and PFLAG_MIN_HEIGHT_IS_SET != 0L) {
      yogaNode.setMinHeight(_minHeightPx.toFloat())
    }
    if (privateFlags and PFLAG_MIN_HEIGHT_PERCENT_IS_SET != 0L) {
      yogaNode.setMinHeightPercent(_minHeightPercent)
    }
    if (privateFlags and PFLAG_MAX_HEIGHT_IS_SET != 0L) {
      yogaNode.setMaxHeight(_maxHeightPx.toFloat())
    }
    if (privateFlags and PFLAG_MAX_HEIGHT_PERCENT_IS_SET != 0L) {
      yogaNode.setMaxHeightPercent(_maxHeightPercent)
    }
    if (privateFlags and PFLAG_LAYOUT_DIRECTION_IS_SET != 0L) {
      yogaNode.setDirection(_layoutDirection)
    }
    if (privateFlags and PFLAG_ALIGN_SELF_IS_SET != 0L) {
      yogaNode.alignSelf = _alignSelf
    }
    if (privateFlags and PFLAG_FLEX_IS_SET != 0L) {
      yogaNode.flex = _flex
    }
    if (privateFlags and PFLAG_FLEX_GROW_IS_SET != 0L) {
      yogaNode.flexGrow = _flexGrow
    }
    if (privateFlags and PFLAG_FLEX_SHRINK_IS_SET != 0L) {
      yogaNode.flexShrink = _flexShrink
    }
    if (privateFlags and PFLAG_FLEX_BASIS_IS_SET != 0L) {
      yogaNode.setFlexBasis(_flexBasisPx.toFloat())
    }
    if (privateFlags and PFLAG_FLEX_BASIS_PERCENT_IS_SET != 0L) {
      yogaNode.setFlexBasisPercent(_flexBasisPercent)
    }
    if (privateFlags and PFLAG_ASPECT_RATIO_IS_SET != 0L) {
      yogaNode.aspectRatio = _aspectRatio
    }
    if (privateFlags and PFLAG_POSITION_TYPE_IS_SET != 0L) {
      yogaNode.positionType = _positionType
    }
    if (privateFlags and PFLAG_POSITION_IS_SET != 0L) {
      for (i in 0 until Edges.EDGES_LENGTH) {
        val value = positions!!.getRaw(i)
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setPosition(YogaEdge.fromInt(i), value.toInt().toFloat())
        }
      }
    }
    if (privateFlags and PFLAG_POSITION_PERCENT_IS_SET != 0L) {
      for (i in 0 until Edges.EDGES_LENGTH) {
        val value = positionPercents!!.getRaw(i)
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setPositionPercent(YogaEdge.fromInt(i), value)
        }
      }
    }
    if (privateFlags and PFLAG_PADDING_IS_SET != 0L) {
      for (i in 0 until Edges.EDGES_LENGTH) {
        val value = paddings!!.getRaw(i)
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setPadding(YogaEdge.fromInt(i), value.toInt().toFloat())
        }
      }
    }
    if (privateFlags and PFLAG_PADDING_PERCENT_IS_SET != 0L) {
      for (i in 0 until Edges.EDGES_LENGTH) {
        val value = paddingPercents!!.getRaw(i)
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setPaddingPercent(YogaEdge.fromInt(i), value)
        }
      }
    }
    if (privateFlags and PFLAG_MARGIN_IS_SET != 0L) {
      for (i in 0 until Edges.EDGES_LENGTH) {
        val value = margins!!.getRaw(i)
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setMargin(YogaEdge.fromInt(i), value.toInt().toFloat())
        }
      }
    }
    if (privateFlags and PFLAG_MARGIN_PERCENT_IS_SET != 0L) {
      for (i in 0 until Edges.EDGES_LENGTH) {
        val value = marginPercents!!.getRaw(i)
        if (!YogaConstants.isUndefined(value)) {
          yogaNode.setMarginPercent(YogaEdge.fromInt(i), value)
        }
      }
    }
    if (privateFlags and PFLAG_MARGIN_AUTO_IS_SET != 0L) {
      for (edge in marginAutos!!) {
        yogaNode.setMarginAuto(edge)
      }
    }
    if (privateFlags and PFLAG_IS_REFERENCE_BASELINE_IS_SET != 0L) {
      yogaNode.setIsReferenceBaseline(_isReferenceBaseline)
    }
    if (privateFlags and PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET != 0L && _useHeightAsBaseline) {
      yogaNode.setBaselineFunction { _, _, height -> height }
    }
    if (privateFlags and PFLAG_FLEX_DIRECTION_IS_SET != 0L) {
      yogaNode.flexDirection = _flexDirection
    }
    if (privateFlags and PFLAG_ALIGN_ITEMS_IS_SET != 0L) {
      yogaNode.alignItems = _alignItems
    }
    if (privateFlags and PFLAG_ALIGN_CONTENT_IS_SET != 0L) {
      yogaNode.alignContent = _alignContent
    }
    if (_justifyContent != null) {
      yogaNode.justifyContent = _justifyContent
    }
    if (privateFlags and PFLAG_WRAP_IS_SET != 0L) {
      yogaNode.wrap = _wrap
    }
    if (display != null) {
      yogaNode.display = display
    }
  }

  override fun usePercentDimensAtRoot(): Boolean = _usePercentDimensAtRoot

  override fun hasPercentWidth(): Boolean = privateFlags and PFLAG_WIDTH_PERCENT_IS_SET != 0L

  override fun hasPercentHeight(): Boolean = privateFlags and PFLAG_HEIGHT_PERCENT_IS_SET != 0L

  override fun getWidthPercent(): Float = _widthPercent

  override fun getHeightPercent(): Float = _heightPercent

  companion object {
    private const val PFLAG_WIDTH_IS_SET = 1L shl 0
    private const val PFLAG_WIDTH_PERCENT_IS_SET = 1L shl 1
    private const val PFLAG_MIN_WIDTH_IS_SET = 1L shl 2
    private const val PFLAG_MIN_WIDTH_PERCENT_IS_SET = 1L shl 3
    private const val PFLAG_MAX_WIDTH_IS_SET = 1L shl 4
    private const val PFLAG_MAX_WIDTH_PERCENT_IS_SET = 1L shl 5
    private const val PFLAG_HEIGHT_IS_SET = 1L shl 6
    private const val PFLAG_HEIGHT_PERCENT_IS_SET = 1L shl 7
    private const val PFLAG_MIN_HEIGHT_IS_SET = 1L shl 8
    private const val PFLAG_MIN_HEIGHT_PERCENT_IS_SET = 1L shl 9
    private const val PFLAG_MAX_HEIGHT_IS_SET = 1L shl 10
    private const val PFLAG_MAX_HEIGHT_PERCENT_IS_SET = 1L shl 11
    private const val PFLAG_LAYOUT_DIRECTION_IS_SET = 1L shl 12
    private const val PFLAG_ALIGN_SELF_IS_SET = 1L shl 13
    private const val PFLAG_FLEX_IS_SET = 1L shl 14
    private const val PFLAG_FLEX_GROW_IS_SET = 1L shl 15
    private const val PFLAG_FLEX_SHRINK_IS_SET = 1L shl 16
    private const val PFLAG_FLEX_BASIS_IS_SET = 1L shl 17
    private const val PFLAG_FLEX_BASIS_PERCENT_IS_SET = 1L shl 18
    private const val PFLAG_ASPECT_RATIO_IS_SET = 1L shl 19
    private const val PFLAG_POSITION_TYPE_IS_SET = 1L shl 20
    private const val PFLAG_POSITION_IS_SET = 1L shl 21
    private const val PFLAG_POSITION_PERCENT_IS_SET = 1L shl 22
    private const val PFLAG_PADDING_IS_SET = 1L shl 23
    private const val PFLAG_PADDING_PERCENT_IS_SET = 1L shl 24
    private const val PFLAG_MARGIN_IS_SET = 1L shl 25
    private const val PFLAG_MARGIN_PERCENT_IS_SET = 1L shl 26
    private const val PFLAG_MARGIN_AUTO_IS_SET = 1L shl 27
    private const val PFLAG_IS_REFERENCE_BASELINE_IS_SET = 1L shl 28
    private const val PFLAG_USE_HEIGHT_AS_BASELINE_IS_SET = 1L shl 29
    private const val PFLAG_FLEX_DIRECTION_IS_SET = 1L shl 30
    private const val PFLAG_ALIGN_ITEMS_IS_SET = 1L shl 31
    private const val PFLAG_ALIGN_CONTENT_IS_SET = 1L shl 32
    private const val PFLAG_JUSTIFY_CONTENT_IS_SET = 1L shl 33
    private const val PFLAG_WRAP_IS_SET = 1L shl 34
    private const val PFLAG_WIDTH_AUTO = 1L shl 35
    private const val PFLAG_HEIGHT_AUTO = 1L shl 36
  }
}
