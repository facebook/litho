// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.examples

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.px
import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.ComponentConditions.typeIs
import com.facebook.litho.testing.assertj.LithoAssertions.assertThat
import com.facebook.litho.testing.assertj.LithoViewAssert
import com.facebook.litho.testing.setRoot
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.visibility.onVisible
import com.facebook.litho.widget.Text
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Examples of LithoViewRule usage */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class LithoViewRuleExampleTest {
  // start_example
  @Rule @JvmField val lithoViewRule = LithoViewRule()
  // end_example

  @Test
  fun `verify InnerComponent appears when TestComponent is clicked`() {
    class InnerComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.height(100.dp).width(100.dp)) { child(Text(text = "some_text")) }
      }
    }

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val showChild = useState { false }

        return Row(
            style =
                Style.width(100.px).height(100.px).viewTag("test_view").onClick {
                  showChild.update(true)
                }) {
          child(Text("some_text2"))
          if (showChild.value) {
            child(InnerComponent())
          }
        }
      }
    }

    lithoViewRule.render { TestComponent() }
    assertThat(lithoViewRule.lithoView).isNotNull
    /** can use all of the assertions from: [LithoViewAssert] class */
    val nullComponent = lithoViewRule.findComponent(InnerComponent::class)
    assertThat(nullComponent).isNull()
    // act_example_start
    lithoViewRule.act { clickOnTag("test_view") }
    // act_example_end

    val component = lithoViewRule.findComponent(InnerComponent::class)
    assertThat(component).isNotNull()
    assertThat(lithoViewRule.lithoView).hasVisibleText("some_text")
  }

  @Test
  fun `verify InnerComponent appears when TestComponent is visible`() {
    class InnerComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.height(100.dp).width(100.dp)) { child(Text(text = "some_text")) }
      }
    }

    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val showChild = useState { false }

        return Row(
            style = Style.width(100.px).height(100.px).onVisible { showChild.update(true) }) {
          child(Text("some_other_text"))
          if (showChild.value) {
            child(InnerComponent())
          }
        }
      }
    }

    // visibility_test_start
    lithoViewRule.setRoot(TestComponent()).attachToWindow().measure()
    /** Before the onVisible is called */
    assertThat(lithoViewRule.lithoView).doesNotContainComponents(InnerComponent::class)
    /** Layout component and idle, triggering visibility event and any async updates */
    lithoViewRule.layout().idle()
    /** After the onVisible is called */
    assertThat(lithoViewRule.lithoView).hasExactlyOne(InnerComponent::class)
    // visibility_test_end
    assertThat(lithoViewRule.lithoView).hasVisibleText("some_text")
  }

  @Test
  fun `verify Components existence with ListAssert`() {
    class DeepComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return Row(style = Style.height(100.dp).width(100.dp))
      }
    }
    class InnerComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return DeepComponent()
      }
    }
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return Column {
          child(InnerComponent())
          child(InnerComponent())
          child(DeepComponent())
        }
      }
    }
    with(lithoViewRule) {
      render { TestComponent() }
      assertThat(findAllComponents(InnerComponent::class, DeepComponent::class))
          .isNotEmpty
          .haveExactly(2, typeIs(InnerComponent::class))
          .haveExactly(3, typeIs(DeepComponent::class))
    }
  }

  @Test
  fun `verify subcomponents existence with contains`() {
    class DeepComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return Row(style = Style.height(100.dp).width(100.dp))
      }
    }
    class InnerComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return DeepComponent()
      }
    }
    class InnerSecondComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return DeepComponent()
      }
    }
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return Column {
          child(InnerComponent())
          child(InnerComponent())
          child(DeepComponent())
          child(InnerSecondComponent())
        }
      }
    }
    // contains_components_start
    lithoViewRule.render { TestComponent() }
    assertThat(lithoViewRule.lithoView)
        .hasExactly(2, InnerComponent::class)
        .containsComponents(DeepComponent::class, InnerSecondComponent::class)
    // contains_components_end
  }

  @Test
  fun `verify InnerComponent has given props`() {
    // has_props_start
    lithoViewRule.render { TestComponent() }
    val component = lithoViewRule.findComponent(InnerComponent::class)
    assertThat(component).isNotNull()

    assertThat(component)
        .hasProps(
            InnerComponent::value to "some_value",
            InnerComponent::style to Style.height(100.dp).width(100.dp))
        .hasPropsMatching(InnerComponent::value to IsInstanceOf.instanceOf(String::class.java))
    // has_props_end
  }

  @Test
  fun `verify lithoView is not null`() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row { child(Text(text = "some_text")) }
      }
    }
    // start_render_example
    lithoViewRule.render { TestComponent() }
    // end_render_example
    assertThat(lithoViewRule.lithoView).isNotNull()
    /** can use all of the assertions from: [LithoViewAssert] class */
  }

  @Test
  fun `verify children of the component`() {
    // find_direct_component_start
    lithoViewRule.render { TestComponent() }
    val innerComponent = lithoViewRule.findDirectComponent(InnerComponent::class)
    assertThat(innerComponent).isNotNull()
    // find_direct_component_end
    // find_non_direct_component_start
    val textComponent = lithoViewRule.findComponent(Text::class)
    assertThat(textComponent).isNotNull()
    // find_non_direct_component_end
  }

  @Test
  fun `verify assertions on lithoview `() {
    // lithoview_assertion_start
    lithoViewRule.render { TestComponent() }
    assertThat(lithoViewRule.lithoView).hasVisibleText("some_value")
    /** can use all of the assertions from: [LithoViewAssert] class */
    // lithoview_assertion_end
  }

  @Test
  fun `verify click on component`() {
    class TestClickComponent(private val clickHandler: TestClickHandler) : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row(style = Style.onClick { clickHandler.onClick(androidContext) }) {
          child(Text(text = "Text"))
        }
      }
    }

    val mockClickHandler: TestClickHandler = mock()
    lithoViewRule.render { TestClickComponent(mockClickHandler) }
    lithoViewRule.act { clickOnRootView() }

    verify(mockClickHandler).onClick(any())
  }

  @Test(expected = IllegalStateException::class)
  fun `verify runtime exception thrown when clicking on a non-clickable view`() {
    lithoViewRule.render { Row() }
    lithoViewRule.act { clickOnRootView() }
  }

  @Test
  fun `verify interactions on lithoView`() {
    // component_for_action_start
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        val showText = useState { false }
        return Row(
            style =
                Style.width(100.px)
                    .height(100.px)
                    .onClick { showText.update(!showText.value) }
                    .contentDescription("row")) {
          if (showText.value) {
            child(Text(text = "Text"))
          }
        }
      }
    }
    // component_for_action_end

    // test_interaction_start
    lithoViewRule.render { TestComponent() }
    LithoStats.resetAllCounters()
    /** Find [Component] based on the text or [Component] class */
    assertThat(lithoViewRule.findViewWithTextOrNull("Text")).isNull()
    assertThat(lithoViewRule.findComponent(Text::class)).isNull()

    /** perform interaction defined in [LithoViewRule] */
    lithoViewRule.act { clickOnContentDescription("row") }

    /** check number of state updates */
    assertThat(LithoStats.getComponentTriggeredAsyncStateUpdateCount()).isEqualTo(1)

    /** Find [Component] based on the text or [Component] class */
    assertThat(lithoViewRule.findViewWithTextOrNull("Text")).isNotNull()
    assertThat(lithoViewRule.findComponent(Text::class)).isNotNull()
    // test_interaction_end
  }

  @Test
  fun `verify the visibility event changed the state`() {
    lateinit var stateRef: AtomicReference<Boolean>

    class TestComponent : KComponent() {
      // idle_component_start
      override fun ComponentScope.render(): Component? {
        val visibilityEventCalled = useState { false }
        stateRef = AtomicReference(visibilityEventCalled.value)
        return Column(
            style =
                Style.width(10.dp).height(10.dp).onVisible { visibilityEventCalled.update(true) })
      }
    }

    lithoViewRule.render { TestComponent() }
    Assertions.assertThat(stateRef.get()).isEqualTo(false)
    lithoViewRule.idle()
    Assertions.assertThat(stateRef.get()).isEqualTo(true)
    // idle_component_end
  }

  // test_component_start
  class TestComponent() : KComponent() {
    override fun ComponentScope.render(): Component {
      return InnerComponent()
    }
  }
}

class InnerComponent(
    @VisibleForTesting val style: Style = Style.height(100.dp).width(100.dp),
    @VisibleForTesting val value: String = "some_value"
) : KComponent() {
  override fun ComponentScope.render(): Component {
    return Row(style = style) { child(Text(text = value)) }
  }
}
// test_component_end

private interface TestClickHandler {
  fun onClick(context: Context)
}
