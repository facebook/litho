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

package com.facebook.samples.litho.kotlin.animations.messages

import com.facebook.litho.widget.ComponentRenderInfo
import com.facebook.litho.widget.RenderInfo
import com.facebook.samples.litho.kotlin.animations.expandableelement.ExpandableElementMe
import com.facebook.samples.litho.kotlin.animations.expandableelement.ExpandableElementOther

class Message(
    private val isMe: Boolean,
    private val message: String,
    private val seen: Boolean,
    private val timestamp: String
) {

  fun createComponent(): RenderInfo {
    val component =
        if (isMe) {
          ExpandableElementMe(messageText = message, timestamp = timestamp, seen = seen)
        } else {
          ExpandableElementOther(messageText = message, timestamp = timestamp, seen = seen)
        }

    return ComponentRenderInfo.create().component(component).build()
  }

  companion object {
    val MESSAGES =
        listOf(
            Message(
                true,
                "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque " +
                    "laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi " +
                    "architecto beatae vitae dicta sunt explicabo",
                true,
                "DEC 25 AT 9:55"),
            Message(
                false,
                "Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit",
                true,
                "DEC 25 AT 9:58"),
            Message(
                false,
                "sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.",
                true,
                "DEC 25 AT 9:59"),
            Message(true, "Neque porro quisquam est", true, "DEC 25 AT 9:59"),
            Message(false, "qui dolorem ipsum quia dolor sit amet", true, "DEC 25 AT 10:01"),
            Message(true, "consectetur, adipisci velit", true, "DEC 25 AT 10:02"),
            Message(
                true,
                "sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam " +
                    "quaerat voluptatem",
                true,
                "DEC 25 AT 10:07"),
            Message(
                true,
                "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit " +
                    "laboriosam",
                true,
                "DEC 25 AT 10:11"),
            Message(false, "nisi ut aliquid ex ea commodi consequatur?", true, "DEC 25 AT 10:16"),
            Message(
                true,
                "Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil" +
                    " molestiae consequatur",
                true,
                "DEC 25 AT 10:21"),
            Message(
                false,
                "vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?",
                false,
                "DEC 25 AT 10:25"),
            Message(
                false,
                "At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis " +
                    "praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias " +
                    "excepturi sint occaecati cupiditate non provident, similique sunt in culpa qui " +
                    "officia deserunt mollitia animi, id est laborum et dolorum fuga.",
                false,
                "DEC 25 AT 10:29"))
  }
}
