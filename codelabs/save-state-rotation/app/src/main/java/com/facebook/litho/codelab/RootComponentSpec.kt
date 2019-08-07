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
package com.facebook.litho.codelab

import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.State
import com.facebook.litho.ClickEvent
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.widget.Text

@Suppress("MagicNumber")
@LayoutSpec
object RootComponentSpec {

    @OnCreateLayout
    fun onCreateLayout(c: ComponentContext, @State counter: Int): Component {
        return Column.create(c)
            .child(
                Text.create(c)
                    .textSizeSp(20f)
                    .text("Click to increment")
                    .clickHandler(RootComponent.onClick(c)))
            .child(Text.create(c).textSizeSp(20f).text(counter.toString()))
            .build()
    }

    @OnEvent(ClickEvent::class)
    fun onClick(c: ComponentContext) {
        RootComponent.increment(c)
    }

    @OnUpdateState
    fun increment(counter: StateValue<Int>) {
        val counterVal = counter.get()!!
        counter.set(counterVal + 1)
    }
}
