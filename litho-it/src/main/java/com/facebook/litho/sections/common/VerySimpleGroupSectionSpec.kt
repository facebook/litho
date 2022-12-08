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

package com.facebook.litho.sections.common

import android.graphics.drawable.ColorDrawable
import com.facebook.litho.ClickEvent
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.sections.Children
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.annotations.GroupSectionSpec
import com.facebook.litho.sections.annotations.OnCreateChildren
import com.facebook.litho.sections.annotations.OnDataBound
import com.facebook.litho.widget.Image
import com.facebook.litho.widget.Text

/** Dummy [GroupSectionSpec] to illustrate how to test sections. */
@GroupSectionSpec
object VerySimpleGroupSectionSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(c: SectionContext, extra: StateValue<Int>) {
    extra.set(0)
  }

  @JvmStatic
  @OnCreateChildren
  fun onCreateChildren(
      c: SectionContext,
      @State(canUpdateLazily = true) extra: Int,
      @Prop numberOfDummy: Int
  ): Children {
    val builder = Children.create()
    if (extra > 0) {
      builder.child(
          SingleComponentSection.create(c)
              .component(Image.create(c).drawable(ColorDrawable()).build()))
    }
    for (i in 0 until numberOfDummy + extra) {
      builder.child(
          SingleComponentSection.create(c)
              .component(Text.create(c).text("Lol hi $i").build())
              .key("key$i")
              .build())
    }
    return builder.build()
  }

  @JvmStatic
  @OnDataBound
  fun onDataBound(
      c: SectionContext,
      @Prop numberOfDummy: Int,
      @State(canUpdateLazily = true) extra: Int
  ) {
    VerySimpleGroupSection.lazyUpdateExtra(c, extra - numberOfDummy)
  }

  @JvmStatic
  @OnUpdateState
  fun onUpdateState(extra: StateValue<Int>, @Param newExtra: Int) {
    extra.set(newExtra)
  }

  @JvmStatic
  @OnEvent(ClickEvent::class)
  fun onImageClick(c: SectionContext) {
    VerySimpleGroupSection.onUpdateStateSync(c, 3)
  }
}
