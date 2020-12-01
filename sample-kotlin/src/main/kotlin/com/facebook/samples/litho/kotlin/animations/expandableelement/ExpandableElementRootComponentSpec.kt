/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.DataDiffSection
import com.facebook.litho.sections.common.RenderEvent
import com.facebook.litho.sections.widget.NotAnimatedItemAnimator
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.RenderInfo
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextAlignment
import com.facebook.samples.litho.kotlin.animations.messages.Message
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaEdge

@LayoutSpec
object ExpandableElementRootComponentSpec {

  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      messages: StateValue<List<Message>>,
      counter: StateValue<Int>,
      @Prop initialMessages: List<Message>
  ) {
    messages.set(initialMessages)
    counter.set(1)
  }

  @OnCreateLayout
  internal fun onCreateLayout(
      c: ComponentContext,
      @State messages: List<Message>,
      @State counter: Int
  ): Component =
      Column.create(c)
          .child(
              Row.create(c)
                  .backgroundColor(Color.LTGRAY)
                  .child(
                      Text.create(c)
                          .paddingDip(YogaEdge.ALL, 10f)
                          .text("INSERT")
                          .textSizeSp(20f)
                          .flexGrow(1f)
                          .alignSelf(YogaAlign.CENTER)
                          .testKey("INSERT")
                          .alignment(TextAlignment.CENTER)
                          .clickHandler(ExpandableElementRootComponent.onClick(c, true)))
                  .child(
                      Text.create(c)
                          .paddingDip(YogaEdge.ALL, 10f)
                          .text("DELETE")
                          .textSizeSp(20f)
                          .flexGrow(1f)
                          .alignSelf(YogaAlign.CENTER)
                          .alignment(TextAlignment.CENTER)
                          .clickHandler(ExpandableElementRootComponent.onClick(c, false))))
          .child(
              RecyclerCollectionComponent.create(c)
                  .flexGrow(1f)
                  .disablePTR(true)
                  .itemAnimator(NotAnimatedItemAnimator())
                  .section(
                      DataDiffSection.create<Message>(SectionContext(c))
                          .data(messages)
                          .renderEventHandler(ExpandableElementRootComponent.onRender(c))
                          .build())
                  .paddingDip(YogaEdge.TOP, 8f))
          .build()

  @OnEvent(ClickEvent::class)
  fun onClick(c: ComponentContext, @Prop initialMessages: List<Message>, @Param adding: Boolean) {
    ExpandableElementRootComponent.onUpdateList(c, adding, initialMessages.size)
  }

  @OnUpdateState
  fun onUpdateList(
      messages: StateValue<List<Message>>,
      counter: StateValue<Int>,
      @Param adding: Boolean,
      @Param initialMessagesSize: Int
  ) {
    val updatedMessageList = messages.get()!!.toMutableList()

    val counterValue = counter.get()
    if (adding) {
      updatedMessageList.add(1, Message(true, "Just Added #$counterValue", true, "Recently", true))
      counter.set(counterValue!! + 1)
    } else if (initialMessagesSize < updatedMessageList.size) {
      updatedMessageList.removeAt(1)
    }
    messages.set(updatedMessageList)
  }

  @OnEvent(RenderEvent::class)
  fun onRender(c: ComponentContext, @FromEvent model: Message): RenderInfo =
      model.createComponent(c)
}
