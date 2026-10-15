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

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.facebook.litho.accessibility.ImportantForAccessibility
import com.facebook.litho.accessibility.importantForAccessibility
import com.facebook.litho.core.height
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.border
import com.facebook.litho.layout.RenderWithConstraints
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.background
import com.facebook.litho.view.duplicateParentState
import com.facebook.litho.view.enabled
import com.facebook.litho.view.testKey
import com.facebook.litho.view.wrapInView
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a component laid out with its size hands to the content it resolves, and what that content
 * hands on to its children. Pins the values every field ends with, so moving where the transfer
 * happens cannot change any of them.
 */
@RunWith(LithoTestRunner::class)
class DeferredPropsTransferOrderTest {

  @JvmField @Rule val lithoTestRule: LithoTestRule = LithoTestRule()

  @Test
  fun `a deferred component's props reach its content and its content's children unchanged`() {
    val view = lithoTestRule.render(widthPx = 300, heightPx = 300) { Holder() }
    val root = checkNotNull(view.committedLayoutState?.rootLayoutResult) as LithoLayoutResult
    val holder = checkNotNull(firstDeferred(root)) { "no deferred result" }
    val content = checkNotNull(holder.result) { "deferred content not laid out" }

    assertThat(snapshot(content.node, "content") + snapshot(content.node.getChildAt(0), "child"))
        .isEqualTo(EXPECTED)
  }

  private fun snapshot(node: LithoNode, label: String): List<String> = listOf(
      "$label.importantForAccessibility=${node.importantForAccessibility}",
      "$label.duplicateParentState=${node.isDuplicateParentStateEnabled}",
      "$label.hostDuplicateParentState=${node.isHostDuplicateParentStateEnabled}",
      "$label.needsHostView=${node.needsHostView()}",
      "$label.enabledState=${node.nodeInfo?.enabledState}",
      "$label.testKey=${node.testKey}",
      "$label.hasBackground=${node.background != null}",
      "$label.paddingFromBackground=${node.paddingFromBackground}",
  )

  private fun firstDeferred(result: LithoLayoutResult): DeferredLithoLayoutResult? {
    if (result is DeferredLithoLayoutResult) return result
    for (i in 0 until result.childCount) {
      firstDeferred(result.getChildAt(i))?.let {
        return it
      }
    }
    return null
  }

  private class Holder : KComponent() {
    override fun ComponentScope.render(): Component =
        Column(style = Style.width(300.px)) {
          child(
              RenderWithConstraints(
                  style =
                      Style.wrapInView()
                          .duplicateParentState(true)
                          .importantForAccessibility(ImportantForAccessibility.NO_HIDE_DESCENDANTS)
                          .enabled(false)
                          .background(PaddedColorDrawable())
                          .border(width = 2.px, color = Color.BLACK)
                          .padding(all = 3.px)
                          .testKey("holder"),
              ) { _ ->
                Column(style = Style.width(200.px)) {
                  child(Row(style = Style.width(10.px).height(10.px)))
                }
              },
          )
        }
  }

  /** A background that pads its content, as a nine-patch does. */
  private class PaddedColorDrawable : ColorDrawable(Color.RED) {
    override fun getPadding(padding: android.graphics.Rect): Boolean {
      padding.set(4, 5, 6, 7)
      return true
    }
  }

  private companion object {
    val EXPECTED: List<String> = listOf(
        "content.importantForAccessibility=4",
        "content.duplicateParentState=true",
        "content.hostDuplicateParentState=false",
        "content.needsHostView=true",
        "content.enabledState=2",
        "content.testKey=holder",
        "content.hasBackground=true",
        "content.paddingFromBackground=Rect(4, 5 - 6, 7)",
        "child.importantForAccessibility=0",
        "child.duplicateParentState=false",
        "child.hostDuplicateParentState=false",
        "child.needsHostView=false",
        "child.enabledState=null",
        "child.testKey=null",
        "child.hasBackground=false",
        "child.paddingFromBackground=null",
    )
  }
}
