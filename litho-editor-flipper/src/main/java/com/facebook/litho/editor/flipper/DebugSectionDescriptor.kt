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

package com.facebook.litho.editor.flipper

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.ViewCompat
import com.facebook.flipper.core.ErrorReportingRunnable
import com.facebook.flipper.core.FlipperDynamic
import com.facebook.flipper.core.FlipperObject
import com.facebook.flipper.plugins.inspector.HighlightedOverlay
import com.facebook.flipper.plugins.inspector.Named
import com.facebook.flipper.plugins.inspector.NodeDescriptor
import com.facebook.flipper.plugins.inspector.SetDataOperations.FlipperValueHint
import com.facebook.flipper.plugins.inspector.Touch
import com.facebook.litho.editor.flipper.DataUtils.getPropData
import com.facebook.litho.editor.flipper.DataUtils.getStateData
import com.facebook.litho.sections.debug.DebugSection

@Suppress("KotlinGenericsCast")
class DebugSectionDescriptor : NodeDescriptor<DebugSection>() {
  override fun invalidate(debugSection: DebugSection?) {
    super.invalidate(debugSection)

    object : ErrorReportingRunnable(mConnection) {
          @Throws(Exception::class)
          override fun runOrThrow() {
            for (i in 0..<getChildCount(debugSection)) {
              val child = getChildAt(debugSection, i)
              if (child is DebugSection) {
                invalidate(child)
              }
            }
          }
        }
        .run()
  }

  @Throws(Exception::class) override fun init(node: DebugSection) = Unit

  @Throws(Exception::class)
  override fun getId(node: DebugSection?): String? {
    return node?.globalKey
  }

  @Throws(Exception::class)
  override fun getName(node: DebugSection?): String? {
    return node?.name
  }

  @Throws(Exception::class)
  override fun getChildCount(node: DebugSection?): Int {
    return node?.sectionChildren?.size ?: 0
  }

  @Throws(Exception::class)
  override fun getChildAt(node: DebugSection?, index: Int): Any {
    return checkNotNull(node).sectionChildren[index]!!
  }

  @Throws(Exception::class)
  override fun getData(node: DebugSection?): List<Named<FlipperObject>> {
    // TODO T39526148 add changeset info
    val data: MutableList<Named<FlipperObject>> = ArrayList()

    node ?: return data

    val propData = getPropData(node)
    if (propData != null) {
      data.addAll(propData)
    }

    val stateData = getStateData(node)
    if (stateData != null) {
      data.add(Named("State", stateData))
    }

    return data
  }

  @Throws(Exception::class)
  override fun setValue(
      node: DebugSection?,
      path: Array<String>,
      kind: FlipperValueHint?,
      value: FlipperDynamic
  ) {
    // TODO T39526148
  }

  @Throws(Exception::class)
  override fun getAttributes(node: DebugSection?): List<Named<String>> {
    // TODO T39526148
    val attrs: List<Named<String>> = ArrayList()
    return attrs
  }

  override fun getExtraInfo(node: DebugSection?): FlipperObject {
    val extraInfo = FlipperObject.Builder()

    val metaData = FlipperObject.Builder()
    metaData.put("className", node?.section?.javaClass?.name)
    metaData.put("framework", "LITHO")

    extraInfo.put("metaData", metaData)

    return extraInfo.build()
  }

  @Throws(Exception::class)
  override fun setHighlighted(node: DebugSection?, selected: Boolean, isAlignmentMode: Boolean) {
    val childCount = getChildCount(node)

    if (node?.isDiffSectionSpec == true) {
      for (i in 0..<childCount) {
        val view = getChildAt(node, i) as View
        highlightChildView(view, selected)
      }
    } else {
      for (i in 0..<childCount) {
        val child = getChildAt(node, i)
        val descriptor = descriptorForClass(child.javaClass) as NodeDescriptor<Any?>?
        checkNotNull(descriptor).setHighlighted(child, selected, isAlignmentMode)
      }
    }
  }

  @Throws(Exception::class)
  override fun getSnapshot(node: DebugSection?, includeChildren: Boolean): Bitmap? {
    return null
  }

  // This is similar to the implementation in ViewDescriptor but doesn't
  // target the parent view.
  private fun highlightChildView(node: View, selected: Boolean) {
    if (!selected) {
      HighlightedOverlay.removeHighlight(node)
      return
    }

    val padding =
        Rect(
            ViewCompat.getPaddingStart(node),
            node.paddingTop,
            ViewCompat.getPaddingEnd(node),
            node.paddingBottom)

    val margin: Rect
    val params = node.layoutParams
    if (params is ViewGroup.MarginLayoutParams) {
      val marginParams = params
      margin =
          Rect(
              MarginLayoutParamsCompat.getMarginStart(marginParams),
              marginParams.topMargin,
              MarginLayoutParamsCompat.getMarginEnd(marginParams),
              marginParams.bottomMargin)
    } else {
      margin = Rect()
    }

    val left = node.left
    val top = node.top

    val contentBounds = Rect(left, top, left + node.width, top + node.height)

    contentBounds.offset(-left, -top)

    HighlightedOverlay.setHighlighted(node, margin, padding, contentBounds, false)
  }

  @Throws(Exception::class)
  override fun hitTest(node: DebugSection?, touch: Touch) {
    val childCount = getChildCount(node)

    // For a DiffSectionSpec, check if child view to see if the touch is in its bounds.
    // For a GroupSectionSpec, check the bounds of the entire section.
    var finish = true
    if (node?.isDiffSectionSpec == true) {
      for (i in 0..<childCount) {
        val child = getChildAt(node, i) as View
        val left = child.left + child.translationX.toInt()
        val top = (child.top + child.translationY.toInt())
        val right = (child.right + child.translationX.toInt())
        val bottom = (child.bottom + child.translationY.toInt())

        val hit = touch.containedIn(left, top, right, bottom)
        if (hit) {
          touch.continueWithOffset(i, left, top)
          finish = false
        }
      }
    } else {
      for (i in 0..<childCount) {
        val child = getChildAt(node, i) as DebugSection
        val bounds = child.bounds
        bounds?.let {
          val hit = touch.containedIn(bounds.left, bounds.top, bounds.right, bounds.bottom)
          if (hit) {
            touch.continueWithOffset(i, 0, 0)
            finish = false
          }
        }
      }
    }
    if (finish) touch.finish()
  }

  @Throws(Exception::class)
  override fun getDecoration(node: DebugSection?): String? {
    // TODO T39526148
    return null
  }

  @Throws(Exception::class)
  override fun matches(query: String, node: DebugSection?): Boolean {
    val descriptor = descriptorForClass(Any::class.java) as NodeDescriptor<Any?>?
    return checkNotNull(descriptor).matches(query, node)
  }

  override fun getAXChildCount(node: DebugSection?): Int {
    return 0
  }

  companion object {
    @Throws(Exception::class)
    private fun getPropData(node: DebugSection): List<Named<FlipperObject>> {
      val section = node.section
      return getPropData(section)
    }

    private fun getStateData(node: DebugSection): FlipperObject? {
      return getStateData(node.stateContainer)
    }
  }
}
