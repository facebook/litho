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

package com.facebook.samples.litho.kotlin.visibility

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.Style
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.visibility.onInvisible
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.dp
import com.facebook.samples.litho.NavigatableDemoActivity

const val LITHOVIEW_INVISIBLE: String = "Set LithoView visibility to INVISIBLE"
const val LITHOVIEW_VISIBLE: String = "Set LithoView visibility to VISIBLE"
const val PARENT_INVISIBLE: String = "Set Parent visibility to INVISIBLE"
const val PARENT_VISIBLE: String = "Set Parent visibility to VISIBLE"

class SetVisibilityActivity : NavigatableDemoActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val containerLayout = LinearLayout(this)
    containerLayout.orientation = LinearLayout.VERTICAL
    val actionButtons = createButtons()
    val parentContainer = FrameLayout(this)
    val lithoView = setupLithoView(parentContainer)
    setupButton1(actionButtons[0], lithoView)
    setupButton2(actionButtons[1], parentContainer)

    containerLayout.apply {
      actionButtons.forEach { addView(it) }
      addView(parentContainer)
    }

    setContentView(containerLayout)
  }

  private fun createButtons(): List<Button> {
    val button1 = Button(this)
    val button2 = Button(this)
    return listOf(button1, button2)
  }

  private fun setupLithoView(parent: FrameLayout): LithoView {
    val lithoView = LithoView.create(ComponentContext(this), VisibilityComponent())
    parent.addView(lithoView)
    return lithoView
  }

  private fun setupButton1(button: Button, lithoView: LithoView) {
    button.text = LITHOVIEW_INVISIBLE
    button.setOnClickListener {
      // it works for both INVISIBLE and GONE
      lithoView.visibility = if (lithoView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
      button.text =
          if (lithoView.visibility == View.VISIBLE) LITHOVIEW_VISIBLE else LITHOVIEW_INVISIBLE
    }
  }

  private fun setupButton2(button: Button, parent: FrameLayout) {
    button.text = PARENT_INVISIBLE
    button.setOnClickListener {
      // it works for both INVISIBLE and GONE
      parent.visibility = if (parent.visibility == View.VISIBLE) View.GONE else View.VISIBLE
      button.text = if (parent.visibility == View.VISIBLE) PARENT_VISIBLE else PARENT_INVISIBLE
    }
  }

  private class VisibilityComponent : KComponent() {
    override fun ComponentScope.render(): Component? {
      return Column(
          style =
              Style.width(200.dp)
                  .width(200.dp)
                  .onVisible { Log.d("VisibilityComponent", "onVisible") }
                  .onInvisible { Log.d("VisibilityComponent", "onInvisible") }) {
            child(Text("Hello World"))
          }
    }
  }
}
