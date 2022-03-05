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

package com.facebook.samples.litho.kotlin.lithography.components

import com.facebook.litho.Column
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.kotlin.widget.Card
import com.facebook.samples.litho.kotlin.lithography.data.Artist

class FeedItemCard(val artist: Artist) : KComponent() {
  override fun ComponentScope.render() =
      Column(style = Style.padding(horizontal = 16.dp, vertical = 8.dp)) {
        child(Card { FeedItemComponent(artist = artist) })
      }
}
