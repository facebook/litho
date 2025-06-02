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

package com.facebook.rendercore.sample.layoutadapter

import android.graphics.Color
import com.facebook.rendercore.DefaultTextNode
import com.facebook.rendercore.DefaultYogaNode
import com.facebook.rendercore.Node
import com.facebook.rendercore.YogaProps
import com.facebook.rendercore.sample.data.LayoutRepository
import com.facebook.rendercore.sample.layoutadapter.FlexboxAdapter.getYogaProps
import com.facebook.rendercore.text.TextAlignment
import com.facebook.rendercore.text.TextRenderUnit
import com.facebook.rendercore.text.TextStyle
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaFlexDirection
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object GsonLayoutAdapter {

  fun from(input: JsonObject): Node<Any?> {
    return resolve(input)
  }

  internal fun from(input: JsonArray): List<Node<Any?>> {
    return input.map { resolve(it.asJsonObject) }
  }

  private fun resolve(input: JsonObject): Node<Any?> {
    val type = input.getAsJsonPrimitive("type").asString
    return when (type) {
      "Flexbox" -> FlexboxAdapter.resolve(input)
      "Text" -> {
        // Generate a unique id for this Text component's RenderUnit.
        // RenderUnits within a tree with the same id are considered same.
        // When a new RenderTree is mounted, RenderUnits with the same id are updated.
        val id = LayoutRepository.getRenderUnitId(input)
        return TextAdapter.resolve(id = id, input = input)
      }
      else -> throw IllegalArgumentException("Unknown type $type")
    }
  }
}

internal object FlexboxAdapter {

  fun resolve(input: JsonObject): Node<Any?> {

    // Build the Node for a FlexBox container component
    val node = DefaultYogaNode(input.getYogaProps())

    // Build the Node for each of its children, and add them to the parent
    input.getChildren().forEach { child -> node.addChild(child) }
    return node
  }

  private fun JsonObject.getChildren(): List<Node<Any?>> {
    return GsonLayoutAdapter.from(getAsJsonArray("children"))
  }

  internal fun JsonObject.getYogaProps(): YogaProps {
    // Read all the Yoga layout attributes to create the YogaProps
    val props = YogaProps()
    this.entrySet().forEach {
      when (it.key) {
        "direction" -> props.flexDirection(it.value.getFlexDirection())
        "flex-grow" -> props.flexGrow(it.value.asFloat)
        "padding" -> props.paddingPx(YogaEdge.ALL, it.value.asInt)
      }
    }

    return props
  }

  private fun JsonElement.getFlexDirection(): YogaFlexDirection {
    return when {
      isJsonPrimitive && asString == "column" -> YogaFlexDirection.COLUMN
      isJsonPrimitive && asString == "row" -> YogaFlexDirection.ROW
      else -> throw IllegalArgumentException("Expected row or column value but as $asString")
    }
  }
}

internal object TextAdapter {

  fun resolve(id: Long, input: JsonObject): DefaultTextNode {
    return DefaultTextNode(
        input.getYogaProps(),
        input.getText(),
        TextRenderUnit(id),
        input.getTextStyle(),
    )
  }

  private fun JsonObject.getText(): String {
    return getAsJsonPrimitive("text").asString
  }

  private fun JsonObject.getTextStyle(): TextStyle {
    val style = TextStyle()
    this.entrySet().forEach {
      when (it.key) {
        "textSize" -> style.textSize = it.value.asInt
        "textColor" -> style.textColor = Color.parseColor(it.value.asString)
        "textAlign" -> style.alignment = it.value.getTextAlignment()
      }
    }

    return style
  }

  private fun JsonElement.getTextAlignment(): TextAlignment {
    return when {
      isJsonPrimitive && asString == "left" -> TextAlignment.LEFT
      isJsonPrimitive && asString == "right" -> TextAlignment.RIGHT
      isJsonPrimitive && asString == "center" -> TextAlignment.CENTER
      else -> throw IllegalArgumentException("Expected left, right or center value but as $this")
    }
  }
}
