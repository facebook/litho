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

package com.facebook.samples.litho.kotlin.collection

import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.EmptyComponent as MyComponent
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.eventHandler
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useCallback
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.collection.LazyList

// start_simple_group
class SimpleGroupMigration(private val title: String) : KComponent() {
  override fun ComponentScope.render(): Component {
    return LazyList {
      // Add SingleComponentSection components as children:
      child(Text(title))
      child(MyComponent())
    }
  }
}
// end_simple_group

class Model(val id: String, val field1: String, val field2: String)

// start_list
class ListMigration(private val models: List<Model>) : KComponent() {
  override fun ComponentScope.render(): Component {
    return LazyList {
      // Add DataDiffSection contents as children with ids
      children(items = models, id = { it.id }) { model ->
        // highlight-start
        Text("${model.field1} ${model.field2}")
        // highlight-end
      }
    }
  }
}
// end_list

// start_event_handler
class EventHandlerMigration : KComponent() {
  override fun ComponentScope.render(): Component {
    val onClick = useCallback { _: ClickEvent -> println("Hello World!") }

    return LazyList {
      // Using Style.onClick(..)
      child(Text("Say Hello", style = Style.onClick(action = onClick)))

      // Or using the Spec api with eventHandler(..)
      child(Text.create(context).text("Say Hello").clickHandler(eventHandler(onClick)).build())
    }
  }
}
// end_event_handler

// start_state
class StateMigration : KComponent() {
  override fun ComponentScope.render(): Component {
    val counter = useState { 0 }
    val onClick = useCallback { _: ClickEvent -> counter.update { it + 1 } }

    return LazyList {
      child(Text("Increment ${counter.value}", style = Style.onClick(action = onClick)))
    }
  }
}
// end_state
