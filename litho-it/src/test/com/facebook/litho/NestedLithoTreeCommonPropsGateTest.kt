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

import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.alpha
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests common-prop comparison when deciding whether a nested tree must re-resolve. */
@RunWith(LithoTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NestedLithoTreeCommonPropsGateTest {

  @JvmField @Rule val lithoTestRule: LithoTestRule = LithoTestRule()

  @Test
  fun `re-resolves when only common props change`() {
    val first = resolve(alpha = 0.5f, current = null, version = 1)
    val second = resolve(alpha = 0.1f, current = first, version = 2)

    assertThat(second).isNotSameAs(first)
  }

  @Test
  fun `re-resolves when a non-common prop changes`() {
    val first = resolve(alpha = 0.5f, current = null, version = 1)
    val second = resolve(alpha = 0.5f, current = first, version = 2, text = "changed")

    assertThat(second).isNotSameAs(first)
  }

  /** Builds a root whose only [alpha] difference lands in the child's common props. */
  private fun resolve(
      alpha: Float,
      current: ResolveResult?,
      version: Int,
      text: String = "hello",
  ): ResolveResult {
    val context = ComponentTree.create(lithoTestRule.context).build().context
    val root =
        with(ComponentScope(lithoTestRule.context)) {
          Row {
            child(
                Text.create(context).text(text).kotlinStyle(Style.alpha(alpha)).build(),
            )
          }
        }
    return NestedLithoTree.resolve(
        id = TREE_ID,
        context = context,
        root = root,
        treeProps = null,
        state = TreeState(),
        current = current,
        version = version,
    )
  }

  companion object {
    private const val TREE_ID = 1
  }
}
