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

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.Prop
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent

@Suppress("MagicNumber")
@LayoutSpec
object LifecycleRootComponentSpec {

  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop lifecycleListener: LifecycleListener): Component {
    val zodiacs = listOf(
        Zodiac("Rat", 1),
        Zodiac("Ox", 2),
        Zodiac("Tiger", 3),
        Zodiac("Hare", 4),
        Zodiac("Dragon", 5),
        Zodiac("Snake", 6),
        Zodiac("Horse", 7),
        Zodiac("Sheep", 8),
        Zodiac("Monkey", 9),
        Zodiac("Rooster", 10),
        Zodiac("Dog", 11),
        Zodiac("Pig", 12)
    )
    return RecyclerCollectionComponent.create(c)
        .disablePTR(true)
        .section(
            LifecycleGroupSection.create(SectionContext(c))
                .zodiacs(zodiacs)
                .lifecycleListener(lifecycleListener)
                .build())
        .build()
  }
}
