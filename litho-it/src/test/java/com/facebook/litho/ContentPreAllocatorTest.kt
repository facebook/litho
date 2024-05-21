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

package com.facebook.litho

import android.content.Context
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.RunnableHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ContentPreAllocatorTest {

  @Test
  fun preAllocateMountContent_createsNewAllocation_whenInControlState() {
    var allocations = 0
    val preAllocator = getContentPreAllocator(false) { allocations++ }
    assertThat(allocations).isEqualTo(0)

    preAllocator.executeSync()
    assertThat(allocations).isEqualTo(1)

    preAllocator.executeSync()
    preAllocator.executeSync()
    assertThat(allocations).isEqualTo(3) // Extra allocation here
  }

  @Test
  fun preAllocateMountContent_avoidsNewAllocation_whenPreviouslyPreAllocated() {
    var allocations = 0
    val preAllocator = getContentPreAllocator(true) { allocations++ }
    assertThat(allocations).isEqualTo(0)

    preAllocator.executeSync()
    assertThat(allocations).isEqualTo(1)

    preAllocator.executeSync()
    preAllocator.executeSync()
    assertThat(allocations).isEqualTo(1) // Extra allocation avoided
  }

  private fun getContentPreAllocator(
      avoidRedundantAllocation: Boolean,
      onPreAllocate: () -> Unit
  ): ContentPreAllocator {
    val componentTree = getComponentTree()
    val mountHandler = RunnableHandler.DefaultHandler(ComponentTree.getDefaultLayoutThreadLooper())

    return ContentPreAllocator(
        treeId = componentTree.id,
        componentContext = componentTree.context,
        mountContentHandler = mountHandler,
        avoidRedundantPreAllocations = avoidRedundantAllocation,
        logger = null,
        nodeSupplier = { componentTree.mainThreadLayoutState?.mountableOutputs.orEmpty() },
        preAllocator = { _, _ ->
          onPreAllocate()
          true
        })
  }
}

private fun getComponentTree(): ComponentTree {
  class SimpleText : KComponent() {
    override fun ComponentScope.render() = Text("Hello world")
  }

  fun ComponentTree.layout() {
    val lithoView = LithoView(context)
    lithoView.componentTree = this
    lithoView.measure(makeMeasureSpec(20, EXACTLY), makeMeasureSpec(20, EXACTLY))
    lithoView.layout(0, 0, lithoView.measuredWidth, lithoView.measuredHeight)
  }

  return ComponentTree.create(
          ComponentContext(ApplicationProvider.getApplicationContext<Context>()), SimpleText())
      .build()
      .also(ComponentTree::layout)
}
