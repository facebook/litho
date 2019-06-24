/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.codelab.textinput

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Row
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge
import com.facebook.yoga.YogaJustify
import java.util.Calendar

@Suppress("MagicNumber")
@LayoutSpec
object TimeConverterSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext): Component {
    val calendar = Calendar.getInstance()

    val londonTime = calendar.timeInMillis

    val newYourTime = londonTime - 5 * ClockDrawable.ONE_HOUR

    val sanFranciscoTime = londonTime - 8 * ClockDrawable.ONE_HOUR

    return Row.create(c)
        .child(
            Column.create(c)
                .child(
                    Clock.create(c)
                        .timeMillis(londonTime)
                        .radius(210)
                        .marginDip(YogaEdge.ALL, 24f)
                )
                .child(
                    Clock.create(c)
                        .timeMillis(newYourTime)
                        .radius(210)
                        .marginDip(YogaEdge.ALL, 24f)
                )
                .child(
                    Clock.create(c)
                        .timeMillis(sanFranciscoTime)
                        .radius(210)
                        .marginDip(YogaEdge.ALL, 24f)
                )
                .justifyContent(YogaJustify.SPACE_AROUND)
        )
        .child(
            Column.create(c)
                .child(
                    Text.create(c)
                        .text("London")
                        .textSizeDip(24f)
                )
                .child(
                    Text.create(c)
                        .text("New York")
                        .textSizeDip(24f)
                )
                .child(
                    Text.create(c)
                        .text("San Francisco")
                        .textSizeDip(24f)
                )
                .justifyContent(YogaJustify.SPACE_AROUND)
        )
        .build()
  }
}
