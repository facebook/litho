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

package com.facebook.litho.codelab.textinput

import android.text.InputType
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.VisibleEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Prop
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextInput
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import java.util.Calendar
import java.util.TimeZone

@Suppress("MagicNumber")
@LayoutSpec
object TimeConverterSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop textInputKey: String): Component {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/London"))

    val londonTimeStr = timeStr(calendar)

    val londonTime = timeMillis(calendar)

    calendar.setTimeZone(TimeZone.getTimeZone("America/New_York"))

    val newYorkTime = timeMillis(calendar)

    calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))

    val sanFranciscoTime = timeMillis(calendar)

    return Row.create(c)
        .child(
            Column.create(c)
                .child(
                    Clock.create(c).timeMillis(londonTime).radius(210).marginDip(YogaEdge.ALL, 24f))
                .child(
                    Clock.create(c)
                        .timeMillis(newYorkTime)
                        .radius(210)
                        .marginDip(YogaEdge.ALL, 24f))
                .child(
                    Clock.create(c)
                        .timeMillis(sanFranciscoTime)
                        .radius(210)
                        .marginDip(YogaEdge.ALL, 24f))
                .justifyContent(YogaJustify.SPACE_AROUND))
        .child(
            Column.create(c)
                .child(
                    Column.create(c)
                        .child(Text.create(c).text("London").textSizeDip(24f))
                        .child(
                            TextInput.create(c)
                                .key(textInputKey)
                                .inputType(InputType.TYPE_CLASS_DATETIME)
                                .textSizeDip(24f)
                                .initialText(londonTimeStr)
                                .visibleHandler(TimeConverter.onVisibleEvent(c))))
                .child(Text.create(c).text("New York").textSizeDip(24f))
                .child(Text.create(c).text("San Francisco").textSizeDip(24f))
                .justifyContent(YogaJustify.SPACE_AROUND))
        .build()
  }

  private fun timeMillis(calendar: Calendar): Long {
    var timeOfDay: Long = 0
    calendar.apply {
      timeOfDay += get(Calendar.HOUR)
      timeOfDay *= 60
      timeOfDay += get(Calendar.MINUTE)
      timeOfDay *= 60
      timeOfDay += get(Calendar.SECOND)
      timeOfDay *= 1000
      timeOfDay += get(Calendar.MILLISECOND)
    }
    return timeOfDay
  }

  @OnEvent(VisibleEvent::class)
  fun onVisibleEvent(c: ComponentContext, @Prop textInputKey: String) {
    TextInput.requestFocus(c, textInputKey)
  }

  private fun timeStr(calendar: Calendar) =
      "${calendar.get(Calendar.HOUR_OF_DAY)} : ${calendar.get(Calendar.MINUTE)}"
}
