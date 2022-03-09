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

package com.facebook.samples.litho.onboarding

import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.dp
import com.facebook.litho.px
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.widget.collection.CrossAxisWrapMode
import com.facebook.litho.widget.collection.LazyList
import com.facebook.samples.litho.onboarding.model.Post
import com.facebook.samples.litho.onboarding.model.User

// start_example
class UserFeedWithStoriesKComponent(
    private val posts: List<Post>,
    private val usersWithStories: List<User>
) : KComponent() {
  override fun ComponentScope.render(): Component {
    return LazyList {
      child(
          LazyList(
              orientation = HORIZONTAL,
              crossAxisWrapMode = CrossAxisWrapMode.MatchFirstChild,
              startPadding = 4.dp,
              topPadding = 4.dp,
              style = Style.padding(vertical = 6.dp)) {
            usersWithStories.forEach { user -> child(StoryKComponent(user = user)) }
          })

      child(Row(style = Style.height(1.px).backgroundColor(0x22888888)))

      posts.forEach { post ->
        child(id = post.id, component = PostWithActionsKComponent(post = post))
      }
    }
  }
}
// end_example
