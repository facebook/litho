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

package com.facebook.samples.litho.integration.actionhandler

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.onLongClick
import com.facebook.rendercore.dp
import com.facebook.rendercore.sp

/**
 * Self-contained demo showing the action handler pattern. The component receives a
 * ScreenActionHandler and invokes its methods on user interaction -- never navigating directly.
 *
 * A log area at the bottom shows which handler methods were called, demonstrating that the
 * component delegates all side effects to the host.
 */
class ActionHandlerComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    val log = useState { listOf<String>() }

    val actionHandler =
        object : ScreenActionHandler {
          override fun onItemClick(itemId: Int) {
            log.update { it + "onItemClick(itemId=$itemId)" }
          }

          override fun onItemLongPress(itemId: Int) {
            log.update { it + "onItemLongPress(itemId=$itemId)" }
          }

          override fun onRetryClick() {
            log.update { it + "onRetryClick()" }
          }

          override fun onProfileClick(userId: String) {
            log.update { it + "onProfileClick(userId=$userId)" }
          }

          override fun onShareClick(itemId: Int) {
            log.update { it + "onShareClick(itemId=$itemId)" }
          }
        }

    return Column(style = Style.padding(all = 16.dp)) {
      child(
          Text(
              text = "Action Handler Pattern",
              textSize = 18.sp,
              style = Style.padding(bottom = 8.dp),
          ),
      )
      child(
          Text(
              text = "Components invoke handler methods, never navigate directly.",
              textSize = 14.sp,
              style = Style.padding(bottom = 12.dp),
          ),
      )
      child(ItemListComponent(actionHandler = actionHandler))
      child(
          Text(
              text = "Action Log:",
              textSize = 14.sp,
              style = Style.padding(top = 16.dp, bottom = 4.dp),
          ),
      )
      log.value.takeLast(5).forEach { entry ->
        child(Text(text = "> $entry", textSize = 12.sp, style = Style.padding(vertical = 2.dp)))
      }
    }
  }
}

// start_example
/**
 * A list component that receives a ScreenActionHandler and delegates all interactions to it. The
 * component has zero knowledge of what happens when the user taps -- that is the host's
 * responsibility.
 */
class ItemListComponent(private val actionHandler: ScreenActionHandler) : KComponent() {

  override fun ComponentScope.render(): Component {
    val items = useCached {
      listOf(
          SampleItem(1, "Photo Post", "user_alice"),
          SampleItem(2, "Video Reel", "user_bob"),
          SampleItem(3, "Story", "user_carol"),
      )
    }

    return Column {
      items.forEach { item ->
        child(
            Column(
                style =
                    Style.padding(vertical = 8.dp)
                        .onClick { actionHandler.onItemClick(item.id) }
                        .onLongClick {
                          actionHandler.onItemLongPress(item.id)
                          true
                        },
            ) {
              child(Text(text = item.title, textSize = 16.sp))
              child(
                  Text(
                      text = "by ${item.authorId}",
                      textSize = 12.sp,
                      style =
                          Style.padding(top = 2.dp).onClick {
                            actionHandler.onProfileClick(item.authorId)
                          },
                  ),
              )
              child(
                  Text(
                      text = "Share",
                      textSize = 12.sp,
                      style =
                          Style.padding(top = 4.dp).onClick { actionHandler.onShareClick(item.id) },
                  ),
              )
            },
        )
      }
    }
  }
}

private class SampleItem(val id: Int, val title: String, val authorId: String)
// end_example
