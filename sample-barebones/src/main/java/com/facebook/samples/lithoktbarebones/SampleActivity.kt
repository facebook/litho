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

package com.facebook.samples.lithoktbarebones

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.recyclerview.widget.OrientationHelper
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.LinearLayoutInfo
import com.facebook.litho.widget.Recycler
import com.facebook.litho.widget.RecyclerBinder

class SampleActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val context = ComponentContext(this)

    val recyclerBinder =
        RecyclerBinder.Builder()
            .layoutInfo(LinearLayoutInfo(this, OrientationHelper.VERTICAL, false))
            .build(context)

    val component = Recycler.create(context).binder(recyclerBinder).build()

    addContent(recyclerBinder, context)

    setContentView(LithoView.create(context, component))
  }

  private fun addContent(recyclerBinder: RecyclerBinder, context: ComponentContext) {
    for (i in 0 until 31) {
      recyclerBinder.insertItemAt(
          i,
          ComponentRenderInfo.create()
              .component(
                  ListItem.create(context)
                      .color(if (i % 2 == 0) Color.WHITE else Color.LTGRAY)
                      .title("Hello, world!")
                      .subtitle("Litho tutorial")
                      .build())
              .build())
    }
  }
}
