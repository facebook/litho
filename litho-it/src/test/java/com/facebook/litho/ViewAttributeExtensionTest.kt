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
import android.graphics.Rect
import android.view.View
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.SolidColor
import com.facebook.litho.kotlin.widget.TextInput
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.view.wrapInView
import com.facebook.litho.visibility.Visibility
import com.facebook.litho.visibility.Visibility.Companion.Invisible
import com.facebook.litho.visibility.Visibility.Companion.Visible
import com.facebook.litho.visibility.visibility
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class ViewAttributeExtensionTest {

  @JvmField @Rule var lithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()
  @JvmField @Rule var lithoTestRule = LithoTestRule()

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

  @Test
  fun `when view attributes are set on a drawable no view attributes should be created`() {
    lithoViewRule.render {
      Column {
        child(
            SolidColor(
                color = Color.BLACK,
                style = Style.width(10.px).height(10.px).viewTag("test-tag"),
            ))
      }
    }

    val viewAttributes = lithoViewRule.committedLayoutState!!.viewAttributes

    assertThat(viewAttributes.size).isEqualTo(2)
    assertThat(viewAttributes.values).doesNotContainNull()
  }

  @Test
  fun `when component is force wrapped then no duplicate view attributes should be created`() {
    lithoViewRule.render {
      Column { child(TextInput(initialText = "hello world", style = Style.wrapInView())) }
    }

    val viewAttributes = lithoViewRule.committedLayoutState!!.viewAttributes

    assertThat(viewAttributes.size).isEqualTo(2)
    assertThat(viewAttributes.values).doesNotContainNull()
  }

  @Test
  fun `when visibility is set on a component then view should be follow it`() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        val drawableVisibility: State<Visibility> = useState { Visible }
        val viewVisibility: State<Visibility> = useState { Visible }
        return Column {
          child(
              SolidColor(
                  color = Color.BLACK,
                  style =
                      Style.width(10.px)
                          .height(10.px)
                          .viewTag("test-tag-0")
                          .onClick {
                            drawableVisibility.updateSync {
                              if (it == Visible) Invisible else Visible
                            }
                          }
                          .visibility(drawableVisibility.value),
              ),
          )

          child(
              TextInput(
                  initialText = "hello world",
                  style =
                      Style.width(10.px)
                          .height(10.px)
                          .viewTag("test-tag-1")
                          .onClick {
                            viewVisibility.updateSync { if (it == Visible) Invisible else Visible }
                          }
                          .visibility(viewVisibility.value),
              ),
          )
        }
      }
    }

    val handle = lithoTestRule.render { TestComponent() }

    val drawableHost = handle.findViewWithTag("test-tag-0")

    assertThat(drawableHost.visibility).isEqualTo(View.VISIBLE)
    drawableHost.callOnClick()
    assertThat(drawableHost.visibility).isEqualTo(View.INVISIBLE)
    drawableHost.callOnClick()
    assertThat(drawableHost.visibility).isEqualTo(View.VISIBLE)

    val view = handle.findViewWithTag("test-tag-1")

    assertThat(view.visibility).isEqualTo(View.VISIBLE)
    view.callOnClick()
    assertThat(view.visibility).isEqualTo(View.INVISIBLE)
    view.callOnClick()
    assertThat(view.visibility).isEqualTo(View.VISIBLE)
  }
}
