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

package com.facebook.samples.litho.kotlin.animations.transitions

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.transition.CrossFade
import com.facebook.litho.transition.ExpandToReveal
import com.facebook.litho.useState
import com.facebook.litho.view.background
import com.facebook.litho.view.onClick
import com.facebook.samples.litho.kotlin.drawable.RoundedRect

class ContainersComponent : KComponent() {

  override fun ComponentScope.render(): Component? {

    val isExpanded = useState { true }
    val showSecondComponent = useState { false }

    val firstComponent =
        Column(
            style =
                Style.width(100.dp)
                    .height(50.dp)
                    .margin(all = 5.dp)
                    .background(RoundedRect(0xff6ab071, 8.dp)))
    val secondComponent =
        Column(
            style =
                Style.width(50.dp)
                    .height(50.dp)
                    .margin(all = 5.dp)
                    .background(RoundedRect(0xffbf678d, 8.dp)))

    return Column(style = Style.width(200.dp)) {
      child(
          Column(
              style =
                  Style.width(100.dp)
                      .height(50.dp)
                      .margin(all = 5.dp)
                      .background(RoundedRect(0xff6ab071, 8.dp))
                      .onClick { isExpanded.update(!isExpanded.value) }))

      child(
          ExpandToReveal(
              isExpanded.value,
              Column(
                  style =
                      Style.width(50.dp)
                          .height(50.dp)
                          .margin(all = 5.dp)
                          .background(RoundedRect(0xffbf678d, 8.dp)))))
      child(
          Column(
              style =
                  Style.width(150.dp)
                      .height(50.dp)
                      .margin(all = 5.dp)
                      .background(RoundedRect(0xffa4dece, 8.dp))
                      .onClick { showSecondComponent.update(!showSecondComponent.value) }))
      child(CrossFade(showSecondComponent.value, firstComponent, secondComponent))
    }
  }
}
