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

package com.facebook.rendercore.sample.data

import com.facebook.rendercore.Node
import com.facebook.rendercore.sample.layoutadapter.GsonLayoutAdapter
import com.google.gson.JsonParser
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay

object LayoutRepository {

  private val layout =
      """
{
  "type": "Flexbox",
  "direction": "column",
  "padding": 32,
  "children": [
    {
      "type": "Text",
      "text": "Hello World!",
      "textSize": 64,
      "textColor": "#000000"
    },
    {
      "type": "Text",
      "flex-grow": 1,
      "textAlign": "center",
      "text": "This text is centered.",
      "textSize": 48,
      "textColor": "#323232"
    }
  ]
}
"""
          .trimIndent()

  suspend fun getLayout(): Node<Any?> {
    delay(1500L)
    return GsonLayoutAdapter.from(JsonParser.parseString(layout).asJsonObject)
  }

  private val idGenerator = AtomicLong(1)
  private val ids = mutableMapOf<Any, Long>()

  // The render unit id should be unique within a tree
  fun getRenderUnitId(key: Any): Long {
    return ids.getOrPut(key) { idGenerator.incrementAndGet() }
  }
}
