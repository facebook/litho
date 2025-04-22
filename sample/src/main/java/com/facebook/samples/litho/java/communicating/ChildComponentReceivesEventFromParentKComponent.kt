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

package com.facebook.samples.litho.java.communicating

import android.widget.Toast
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect
import com.facebook.rendercore.dp

class ChildComponentReceivesEventFromParentKComponent(
    private val controller: ParentToChildEventController,
    private val textFromParent: String,
) : KComponent() {

  override fun ComponentScope.render(): Component? {
    useEffect(controller) {
      controller.setTriggerShowToastListener { triggerShowToast(context, it) }
      onCleanup { controller.reset() }
    }

    return Column {
      child(Text(text = "ChildComponent", textSize = 20.dp))
      child(Text(text = "Text received from parent: $textFromParent", textSize = 15.dp))
    }
  }

  private fun triggerShowToast(c: ComponentContext, message: String) {
    Toast.makeText(c.androidContext, message, Toast.LENGTH_SHORT).show()
  }
}
