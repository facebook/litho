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

package com.facebook.samples.litho.kotlin.gettingstarted

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useCallback
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.widget.collection.LazyList
import com.facebook.rendercore.dp

// start_list_clicking_example
private val ONE_TO_TEN =
    listOf("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten")

class BasicList : KComponent() {
  override fun ComponentScope.render(): Component {
    val clickItem = useState { "Nothing" }
    val callBack = useCallback { index: Int -> clickItem.update(ONE_TO_TEN[index]) }

    return LazyList {
      child(Text(text = "Click us!"))
      ONE_TO_TEN.forEachIndexed { index, str ->
        child(
            id = str,
            component =
                Text(
                    str,
                    style = Style.onClick { callBack(index) }.padding(all = 16.dp),
                ))
      }
      child(Text(text = "${clickItem.value} was clicked"))
    }
  }
}
// end_list_clicking_example
