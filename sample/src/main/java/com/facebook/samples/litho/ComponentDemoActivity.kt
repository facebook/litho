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
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.facebook.litho.*
import kotlinx.coroutines.*
import com.facebook.litho.widget.Text
import java.util.concurrent.Executors

class ComponentDemoActivity : NavigatableDemoActivity() {

  lateinit var composition: (component: @Composable  () -> Unit) -> Unit

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // initialise the composition
    composition = initialise()

    // Set a component on the composition
    composition {
      Column {
        Text(text = "hello world")
      }
    }
  }

  @Composable
  fun Text(text: String) {
    val parent = LocalParentContext.current
    val component = Text.create(parent).text(text).build()
    LithoComposeNode(scope = createScopedContext(component = component, parent = parent))
  }

  private fun initialise(): (@Composable () -> Unit)->Unit {

    // Hard coding a root node for the hack
    val node = createRootNode()

    // Use a dispatcher which uses a single background thread.
    val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    // Create Recomposer to watch
    val recomposer = Recomposer(dispatcher)

    // Watch the Composers current state
    watch(recomposer, node)

    return fun(content : @Composable ()-> Unit) {
      Composition(
          applier = LithoNodeApplier(root = node),
          parent = recomposer
      ).setContent {
        // This akin to a creating a tree prop
        // LocalParentContext.current will return the info.context
        // The implementation of Column
        CompositionLocalProvider(LocalParentContext provides node.tailComponentContext) {
          content()
        }
      }
    }
  }

  private fun watch(recomposer: Recomposer, node: LithoNode) {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        recomposer.currentState.collect {
          Log.d("recomposer", "current state is: $it.")
          val component = node.getChildAt(0).getChildAt(0).tailComponent
          Log.d("recomposer", "Component: $component")
        }
      }
    }
  }

  private fun createRootNode(): LithoNode {
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

    return node
  }
}
