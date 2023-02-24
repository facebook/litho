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

import android.view.View
import com.facebook.litho.DebugComponentDescriptionHelper.ExtraDescription
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.ResType
import java.lang.Exception
import java.lang.StringBuilder
import java.util.HashSet
import kotlin.jvm.JvmStatic
import org.json.JSONObject

/**
 * Describes [DebugComponent]s for use in testing and debugging. Note that
 * [com.facebook.litho.config.ComponentsConfiguration#isEndToEndTestRun] must be enabled in order
 * for this data to be collected.
 */
object DebugComponentDescriptionHelper {

  /** Fields to ignore when dumping extra props */
  private val IGNORE_PROP_FIELDS =
      HashSet(
          listOf(
              "delegate",
              "feedPrefetcher",
              "parentFeedContextChain",
              "child",
              "children",
              "childComponent",
              "trackingCode",
              "eventsController",
              "itemAnimator",
              "onScrollListeners",
              "recyclerConfiguration",
              "threadTileViewData",
              "textColorStateList",
              "typeface",
              "text",
              "params"))

  @JvmStatic
  fun addViewDescription(
      debugComponent: DebugComponent,
      sb: StringBuilder,
      leftOffset: Int,
      topOffset: Int,
      embedded: Boolean,
      withProps: Boolean
  ) {
    addViewDescription(debugComponent, sb, leftOffset, topOffset, embedded, withProps, null)
  }

  /**
   * Appends a compact description of a [DebugComponent] for debugging purposes.
   *
   * @param debugComponent The [DebugComponent]
   * @param sb The [StringBuilder] to which the description is appended
   * @param leftOffset Offset of the parent component relative to litho view
   * @param topOffset Offset of the parent component relative to litho view
   * @param embedded Whether the call is embedded in "adb dumpsys activity"
   * @param extraDescription An interface for callsite to append extra description.
   */
  @JvmStatic
  /**
   * Appends a compact description of a [DebugComponent] for debugging purposes.
   *
   * @param debugComponent The [DebugComponent]
   * @param sb The [StringBuilder] to which the description is appended
   * @param leftOffset Offset of the parent component relative to litho view
   * @param topOffset Offset of the parent component relative to litho view
   * @param embedded Whether the call is embedded in "adb dumpsys activity"
   * @param extraDescription An interface for callsite to append extra description.
   */
  fun addViewDescription(
      debugComponent: DebugComponent,
      sb: StringBuilder,
      leftOffset: Int,
      topOffset: Int,
      embedded: Boolean,
      withProps: Boolean,
      extraDescription: ExtraDescription?
  ) {
    sb.append("litho.")
    sb.append(debugComponent.component.simpleName)
    sb.append('{')
    sb.append(Integer.toHexString(debugComponent.hashCode()))
    sb.append(' ')
    val lithoView = debugComponent.lithoView
    val layout = debugComponent.layoutNode
    sb.append(if (lithoView != null && lithoView.visibility == View.VISIBLE) "V" else ".")
    sb.append(if (layout != null && layout.focusable) "F" else ".")
    sb.append(if (lithoView != null && lithoView.isEnabled) "E" else ".")
    sb.append(".")
    sb.append(if (lithoView != null && lithoView.isHorizontalScrollBarEnabled) "H" else ".")
    sb.append(if (lithoView != null && lithoView.isVerticalScrollBarEnabled) "V" else ".")
    sb.append(if (layout?.clickHandler != null) "C" else ".")
    sb.append(". .. ")
    // using position relative to litho view host to handle relative position issues
    // the offset is for the parent component to create proper relative coordinates
    val bounds = debugComponent.boundsInLithoView
    sb.append(bounds.left - leftOffset)
    sb.append(",")
    sb.append(bounds.top - topOffset)
    sb.append("-")
    sb.append(bounds.right - leftOffset)
    sb.append(",")
    sb.append(bounds.bottom - topOffset)
    val testKey = debugComponent.testKey
    if (testKey != null && !testKey.isNullOrEmpty()) {
      sb.append(" litho:id/").append(testKey.replace(' ', '_'))
    }
    val textContent = debugComponent.textContent
    if (textContent != null && !textContent.isNullOrEmpty()) {
      sb.append(" text=\"").append(fixString(textContent, 200)).append("\"")
    }
    if (withProps) {
      addExtraProps(debugComponent.component, sb)
    }
    extraDescription?.applyExtraDescription(debugComponent, sb)
    if (!embedded && layout?.clickHandler != null) {
      sb.append(" [clickable]")
    }
    sb.append('}')
  }

  private fun addExtraProps(node: Any, sb: StringBuilder) {
    val props = getExtraProps(node)
    if (props.length() > 0) {
      sb.append(" props=\"").append(props.toString()).append("\"")
    }
  }

  @JvmStatic
  fun getExtraProps(node: Any): JSONObject {
    val props = JSONObject()
    for (field in node.javaClass.declaredFields) {
      try {
        if (IGNORE_PROP_FIELDS.contains(field.name)) {
          continue
        }
        val annotation = field.getAnnotation(Prop::class.java) ?: continue
        field.isAccessible = true
        when (annotation.resType) {
          ResType.COLOR,
          ResType.DRAWABLE,
          ResType.DIMEN_SIZE,
          ResType.DIMEN_OFFSET -> {}
          ResType.STRING -> {
            val strValue = fixString(field[node], 50)
            if (strValue.isNotEmpty()) {
              props.put(field.name, strValue)
            }
          }
          else -> {
            val value = field[node]
            if (value != null) {
              props.put(field.name, value)
            }
          }
        }
      } catch (e: Exception) {
        try {
          props.put("DUMP-ERROR", fixString(e.message, 50))
        } catch (ex: Exception) {
          // ignore
        }
      }
    }
    return props
  }

  private fun fixString(str: Any?, maxLength: Int): String {
    if (str == null) {
      return ""
    }
    var fixed = str.toString().replace(" \n", " ").replace("\n", " ").replace("\"", "")
    if (fixed.length > maxLength) {
      fixed = "${fixed.substring(0, maxLength)}..."
    }
    return fixed
  }

  /**
   * An fun interface for callsite to append extra description into [StringBuilder] by given
   * [DebugComponent].
   */
  fun interface ExtraDescription {
    fun applyExtraDescription(debugComponent: DebugComponent, sb: StringBuilder)
  }
}
