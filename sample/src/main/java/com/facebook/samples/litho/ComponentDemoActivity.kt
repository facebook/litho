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

package com.facebook.samples.litho

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import com.facebook.litho.*
import kotlinx.coroutines.Dispatchers

class ComponentDemoActivity : NavigatableDemoActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val composition = initialise()

    composition {
      Column {  }
    }
  }

  private fun initialise(): (@Composable () -> Unit)->Unit {
    val activity = ComponentContext(this)
    val root = RootComponent()
    val info = ScopedComponentInfo(
        ComponentContext.withComponentScope(
            activity,
            root,
            ComponentKeyUtils.generateGlobalKey(activity, root),
        )
    )

    val node = LithoNode()
    node.appendComponent(info)

    return fun(content : @Composable ()-> Unit) {
      Composition(
          applier = LithoNodeApplier(root = node),
          parent = Recomposer(Dispatchers.Main)
      ).setContent {
        CompositionLocalProvider(LocalParentContext provides info.context) {
          content()
        }
      }
    }
  }
}
