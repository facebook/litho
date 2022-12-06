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

package com.facebook.litho.widget

import android.content.Context
import android.widget.FrameLayout
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoView
import com.facebook.litho.MeasureScope
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Size
import com.facebook.litho.useState
import com.facebook.rendercore.MeasureResult

/** Renders the given component in a nested LithoView. */
class SimpleNestedTreeComponent(private val contentComponent: Component) : MountableComponent() {
  override fun MountableComponentScope.render(): MountableRenderResult {
    val componentTree = useState {
      ComponentTree.create(ComponentContext.makeCopyForNestedTree(context))
          .incrementalMount(false)
          .build()
    }
    return MountableRenderResult(
        SimpleNestedTreeMountable(contentComponent, componentTree.value), null)
  }
}

private class SimpleNestedTreeMountable(
    private val contentComponent: Component,
    private val componentTree: ComponentTree
) : SimpleMountable<FrameLayout>(RenderType.VIEW) {
  override fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult {
    val size = Size()
    componentTree.setRootAndSizeSpecSync(contentComponent, widthSpec, heightSpec, size)
    return MeasureResult(size.width, size.height)
  }

  override fun mount(c: Context, content: FrameLayout, layoutData: Any?) {
    val lithoView = content.getChildAt(0) as LithoView
    lithoView.componentTree = componentTree
  }

  override fun unmount(c: Context, content: FrameLayout, layoutData: Any?) {
    val lithoView = content.getChildAt(0) as LithoView
    lithoView.componentTree = null
  }

  // FrameLayout is used here due to some bugs around having a LithoView as the direct content type.
  override fun createContent(context: Context): FrameLayout {
    val lithoView = LithoView(context)
    val frameLayout = FrameLayout(context)
    frameLayout.addView(
        lithoView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
    return frameLayout
  }
}
