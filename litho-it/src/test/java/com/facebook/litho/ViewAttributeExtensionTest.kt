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

import android.graphics.Rect
import android.view.View
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecLifecycleTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ViewAttributeExtensionTest {

  @JvmField @Rule var lithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  @Test
  fun `when view attributes is set it should override attribute set by mount spec`() {

    val c: ComponentContext = lithoViewRule.context

    val root: Component =
        MountSpecLifecycleTester.create(c)
            .intrinsicSize(Size(100, 100))
            .lifecycleTracker(LifecycleTracker())
            .defaultAlpha(0.5f)
            .alpha(0.2f)
            .build()

    lithoViewRule.render { root }

    val content: View = lithoViewRule.lithoView.getChildAt(0)

    assertThat(content.alpha)
        .describedAs("alpha should be applied from the common props")
        .isEqualTo(0.2f)

    // unmount everything
    lithoViewRule.useComponentTree(null)

    assertThat(content.alpha)
        .describedAs("alpha should be restored to the initial value")
        .isEqualTo(0.5f)
  }

  @Test
  fun `when view attributes is set on LithoView should be unset when root is unmounted`() {

    val c: ComponentContext = lithoViewRule.context

    val root: Component =
        Column.create(c)
            .alpha(0.2f)
            .child(
                MountSpecLifecycleTester.create(c)
                    .intrinsicSize(Size(100, 100))
                    .lifecycleTracker(LifecycleTracker()))
            .build()

    lithoViewRule.render { root }

    assertThat(lithoViewRule.lithoView.alpha)
        .describedAs("alpha should be applied from the common props")
        .isEqualTo(0.2f)

    lithoViewRule.lithoView.mountComponent(Rect(0, -10, 1080, -5), true)

    assertThat(lithoViewRule.lithoView.alpha)
        .describedAs("alpha should be restored to the default value")
        .isEqualTo(1f)
  }
}
