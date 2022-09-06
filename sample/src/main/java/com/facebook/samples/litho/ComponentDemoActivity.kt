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
import android.os.Handler
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.facebook.litho.*
import kotlinx.coroutines.*
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaFlexDirection
import java.util.concurrent.Executors

class ComponentDemoActivity : NavigatableDemoActivity() {

  lateinit var composition: (component: @Composable () -> Unit) -> Unit
  private val subscriber = Subscriber()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // initialise the composition
    composition = initialise()

    // Set a component on the composition
    composition {
      Column {
        Text(text = "hello world", subscriber = subscriber)
      }
    }

    setContent {
      androidx.compose.material.Text(text = "Hello World")
    }

    Handler().postDelayed({
      subscriber.update(1)
    }, 1000)
  }

  private fun watch(recomposer: Recomposer, node: LithoNode) {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        recomposer.currentState.collect {
          Log.d("jchack", "current state is: $it.")
          val textNode = node.getChildAt(0).getChildAt(0)
          val component = textNode.tailComponent
          Log.d("jchack", "Component: $component [${textNode.testKey}]")
        }
      }
    }
  }

  private fun initialise(): (@Composable () -> Unit) -> Unit {

    // Hard coding a root node for the hack
    val node = createRootNode()

    // Use a dispatcher which uses a single background thread.
    val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    // Create Recomposer to watch
    val recomposer = Recomposer(dispatcher)

    // Watch the Composers current state
    watch(recomposer, node)

    return fun(content: @Composable () -> Unit) {
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

@Composable
fun Column(content: @Composable () -> Unit) {
  val parent = LocalParentContext.current
  val component = Column.create(parent).build()
  val scope = createScopedContext(component = component, parent = parent)

  // Litho Compose Node is the bridge between LithoNode an Compose
  LithoComposeNode(scope = scope, content = content) {
    // This lambda receives the LithoNode
    flexDirection(YogaFlexDirection.COLUMN)
  }
}

@Composable
fun Text(text: String, subscriber: Subscriber? = null) {
  val parent = LocalParentContext.current
  val count = remember { mutableStateOf(0) }

  Log.d("jchack", "current count: ${count.value}")

  DisposableEffect(subscriber) {
    val listener = { c: Int ->
      Log.d("jchack", "update count: $c")
      count.value = c
    }
    subscriber?.register(listener)
    onDispose {
      Log.d("jchack", "on-dispose")
      subscriber?.unregister(listener)
    }
  }

  val component = Text.create(parent).text("$text and count: ${count.value}").build()
  LithoComposeNode(scope = createScopedContext(component = component, parent = parent)) {
    testKey("count: ${count.value}")
  }
}

class Subscriber {

  private val listeners = mutableSetOf<(Int) -> Unit>()

  fun register(listener: (Int) -> Unit) {
    listeners.add(listener)
  }

  fun unregister(listener: (Int) -> Unit) {
    listeners.remove(listener)
  }

  fun update(count: Int) {
    listeners.forEach {
      it.invoke(count)
    }
  }
}
