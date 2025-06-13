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

import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoRenderTreeView
import com.facebook.litho.LithoRenderer
import com.facebook.litho.LithoTree
import com.facebook.litho.componentsConfig
import com.facebook.rendercore.SizeConstraints

/** A [CollectionItem] that renders a [LithoRenderTreeView]. */
class LithoCollectionItem(
    componentContext: ComponentContext,
    id: Int = LithoTree.generateComponentTreeId(),
    viewType: Int,
    renderInfo: RenderInfo,
) : CollectionItem<LithoRenderTreeView>(id, viewType, renderInfo) {

  private val renderer: LithoRenderer =
      LithoRenderer(
          context = componentContext.androidContext,
          id = id,
          componentsConfig = componentContext.componentsConfig,
          treePropContainer = componentContext.treePropContainerCopy,
          visibilityController = componentContext.lithoVisibilityEventsController)

  override fun prepare(sizeConstraints: SizeConstraints) {
    renderer.render(renderInfo.component, sizeConstraints)
  }

  override fun prepareSync(
      sizeConstraints: SizeConstraints,
      result: IntArray?,
      shouldCommit: Boolean
  ) {
    val layoutState = renderer.renderSync(renderInfo.component, sizeConstraints)
    result?.let {
      result[0] = layoutState?.width ?: 0
      result[1] = layoutState?.height ?: 0
    }
  }

  override fun onBindView(view: LithoRenderTreeView) {
    renderer.currentLayoutState?.let { layoutState ->
      view.setLayoutState(layoutState, layoutState.treeState)
    }
  }

  override fun onViewRecycled(view: LithoRenderTreeView) {
    view.clean()
  }

  override fun unprepare() {
    // todo
  }
}
