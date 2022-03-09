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

package com.facebook.samples.litho.kotlin.animations.expandableelement

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.eventHandlerWithReturn
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.widget.NotAnimatedItemAnimator
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.sp
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.litho.view.testKey
import com.facebook.litho.widget.TextAlignment
import com.facebook.samples.litho.kotlin.animations.messages.Message
import com.facebook.yoga.YogaAlign

class ExpandableElementRootKotlinKComponent(private val initialMessages: List<Message>) :
    KComponent() {

  override fun ComponentScope.render(): Component? {
    val messages = useState { initialMessages }
    val counter = useState { 1 }

    return Column {
      child(
          Row(style = Style.backgroundColor(Color.LTGRAY)) {
            child(
                Text(
                    style =
                        Style.padding(all = 10.dp)
                            .flex(grow = 1f)
                            .alignSelf(YogaAlign.CENTER)
                            .testKey("INSERT")
                            .onClick {
                              val updatedMessageList = messages.value.toMutableList()
                              updatedMessageList.add(
                                  1,
                                  Message(true, "Just Added #${counter.value}", true, "Recently"))
                              counter.update { prevValue -> prevValue + 1 }
                              messages.update(updatedMessageList)
                            },
                    text = "INSERT",
                    textSize = 20.sp,
                    alignment = TextAlignment.CENTER))
            child(
                Text(
                    style =
                        Style.padding(all = 10.dp)
                            .flex(grow = 1f)
                            .alignSelf(YogaAlign.CENTER)
                            .onClick {
                              val updatedMessageList = messages.value.toMutableList()
                              if (initialMessages.size < updatedMessageList.size) {
                                updatedMessageList.removeAt(1)
                                messages.update(updatedMessageList)
                              }
                            },
                    text = "DELETE",
                    textSize = 20.sp,
                    alignment = TextAlignment.CENTER))
          })
      child(
          RecyclerCollectionComponent(
              style = Style.flex(grow = 1f).padding(top = 8.dp),
              disablePTR = true,
              itemAnimator = NotAnimatedItemAnimator(),
              section =
                  DataDiffSection.create<Message>(SectionContext(context))
                      .data(messages.value)
                      .renderEventHandler(
                          eventHandlerWithReturn { event -> event.model.createComponent() })
                      .build()))
    }
  }
}
