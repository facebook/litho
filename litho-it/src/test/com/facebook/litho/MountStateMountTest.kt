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
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.enabled
import com.facebook.litho.view.wrapInView
import com.facebook.litho.visibility.onVisible
import com.facebook.litho.widget.SolidColor
import com.facebook.litho.widget.Text
import com.facebook.rendercore.Function
import com.facebook.rendercore.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class MountStateMountTest {

  @JvmField @Rule val lithoTestRule: LithoTestRule = LithoTestRule()
  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = lithoTestRule.context
  }

  @Test
  fun unmountAll_mountStateNeedsRemount() {
    val root = Column.create(context).child(CardHeaderComponent(title = "Title")).build()
    val testLithoView = lithoTestRule.render { root }
    val mountDelegateTarget = testLithoView.lithoView.mountDelegateTarget
    assertThat(mountDelegateTarget.needsRemount()).isFalse
    testLithoView.lithoView.unmountAllItems()
    assertThat(mountDelegateTarget.needsRemount()).isTrue
  }

  private class CardActionsComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      val props = getTreeProp<ItemCardComponent.TreeProps>()
      val isEnabledState = useState<Boolean> { true }
      return Column(
          style =
              Style.backgroundColor(Color.GRAY)
                  .enabled(props == null || !props.areCardToolsDisabled),
      ) {
        child(Text.create(context).text("controls").build())
        child(
            Row(
                style =
                    Style.enabled(isEnabledState.value).wrapInView().onVisible {
                      props?.onCardActionViewVisible?.call(it.content)
                    },
            ) {
              child(SolidColor(color = Color.RED, style = Style.width(25.dp).height(25.dp)))
              child(SolidColor(color = Color.GREEN, style = Style.width(25.dp).height(25.dp)))
              child(SolidColor(color = Color.BLUE, style = Style.width(25.dp).height(25.dp)))
            },
        )
      }
    }
  }

  private class CardToolsComponent : KComponent() {
    override fun ComponentScope.render(): Component {
      return Row {
        child(Text.create(context).text("toolbar: ").build())
        child(CardActionsComponent())
      }
    }
  }

  private class CardHeaderComponent(private val title: String) : KComponent() {
    override fun ComponentScope.render(): Component {
      return Column {
        child(
            Row {
              child(SolidColor(color = Color.MAGENTA, style = Style.width(16.dp).height(16.dp)))
              child(Text.create(context).text(title).build())
            },
        )
        child(CardToolsComponent())
      }
    }
  }

  private class ItemCardComponent {
    class TreeProps {
      @JvmField var onCardActionsTouched: Function<Unit>? = null
      @JvmField var onCardActionViewVisible: Function<Unit>? = null
      @JvmField var areCardToolsDisabled: Boolean = false
    }
  }
}
