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

import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LayoutOnlyPassRenderStateTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  private val defaultLayoutOnlyPassUsesResolveRenderState =
      ComponentsConfiguration.layoutOnlyPassUsesResolveRenderState

  @After
  fun tearDown() {
    ComponentsConfiguration.layoutOnlyPassUsesResolveRenderState =
        defaultLayoutOnlyPassUsesResolveRenderState
  }

  @Test
  fun `layout-only pass takes render state from the resolve snapshot`() {
    ComponentsConfiguration.layoutOnlyPassUsesResolveRenderState = true
    val mounted = renderAndMount()

    val treeState = TreeState.forLayoutOnlyPass(mounted.treeState, mounted.resolveResult.treeState)

    assertThat(treeState.getPreviousLayoutStateId())
        .isEqualTo(mounted.resolveResult.treeState.getPreviousLayoutStateId())
        .isNotEqualTo(mounted.id)
  }

  @Test
  fun `layout-only pass takes render state from the committed layout when disabled`() {
    ComponentsConfiguration.layoutOnlyPassUsesResolveRenderState = false
    val mounted = renderAndMount()

    val treeState = TreeState.forLayoutOnlyPass(mounted.treeState, mounted.resolveResult.treeState)

    assertThat(treeState.getPreviousLayoutStateId()).isEqualTo(mounted.id)
  }

  /** Mount records the layout's render data into its own [TreeState] on the main thread. */
  private fun renderAndMount(): LayoutState {
    val testView = lithoTestRule.render(widthPx = 400, heightPx = 200) { Text(text = "hello") }
    lithoTestRule.idle()
    val mounted = requireNotNull(testView.componentTree.mainThreadLayoutState)
    assertThat(mounted.treeState.getPreviousLayoutStateId()).isEqualTo(mounted.id)
    return mounted
  }
}
