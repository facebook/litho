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

package com.facebook.samples.litho.kotlin.errors

import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.widget.Card

class KListRow(private val row: ListRow) : KComponent() {

  override fun ComponentScope.render(): Component? {
    return Column(style = Style.padding(vertical = 8.dp, horizontal = 32.dp)) {
      child(
          Card.create(context)
              .content(
                  Column(style = Style.margin(all = 32.dp)) {
                    child(TitleComponent.create(context).title(row.title).build())
                    child(
                        PossiblyCrashingSubTitleComponent.create(context)
                            .subtitle(row.subtitle)
                            .build())
                  })
              .build())
    }
  }
}
