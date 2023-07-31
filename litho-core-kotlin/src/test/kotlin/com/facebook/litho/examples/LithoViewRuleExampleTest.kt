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
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.EmptyComponent
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.stats.LithoStats
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.ComponentConditions.typeIs
import com.facebook.litho.testing.assertj.LithoAssertions.assertThat
import com.facebook.litho.testing.assertj.LithoViewAssert
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.visibility.onVisible
import com.facebook.litho.widget.Text
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.LooperMode

/** Examples of LithoViewRule usage */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
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

    val testLithoView = lithoViewRule.render { TestComponent() }
    /** can use all of the assertions from: [LithoViewAssert] class */
    assertThat(testLithoView).willRenderContent().doesNotContainComponents(InnerComponent::class)
    // act_example_start
    lithoViewRule.act(testLithoView) { clickOnTag("test_view") }
    // act_example_end

    assertThat(testLithoView).containsExactlyOne(InnerComponent::class).hasVisibleText("some_text")
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
    val testLithoView =
        lithoViewRule.createTestLithoView { TestComponent() }.attachToWindow().measure()
    /** Before the onVisible is called */
    assertThat(testLithoView.findComponent(InnerComponent::class)).isNull()
    /** Layout component and idle, triggering visibility event and any async updates */
    testLithoView.layout()
    lithoViewRule.idle()
    /** After the onVisible is called */
    assertThat(testLithoView).containsExactlyOne(InnerComponent::class)
    // visibility_test_end
    assertThat(testLithoView).hasVisibleText("some_text")
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
    val testLithoView = lithoViewRule.render { TestComponent() }
    with(testLithoView) {
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
    val testLithoView = lithoViewRule.render { TestComponent() }
    assertThat(testLithoView)
        .containsExactly(2, InnerComponent::class)
        .containsComponents(DeepComponent::class, InnerSecondComponent::class)
    // contains_components_end
  }

  @Test
  fun `verify soft assertions when missing multiple components`() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return Column(style = Style.height(200.dp).width(200.dp))
      }
    }
    class MissingComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return Row(style = Style.height(10.dp).width(10.dp))
      }
    }
    class MissingComponentTwo : KComponent() {
      override fun ComponentScope.render(): Component {
        return MissingComponent()
      }
    }
    class MissingComponentThree : KComponent() {
      override fun ComponentScope.render(): Component {
        return MissingComponent()
      }
    }

    val testLithoView = lithoViewRule.render { TestComponent() }
    assertThatThrownBy {
          assertThat(testLithoView)
              .containsComponents(
                  MissingComponent::class.java,
                  MissingComponentTwo::class.java,
                  MissingComponentThree::class.java)
        }
        .hasMessageContaining("\$MissingComponent")
        .hasMessageContaining("\$MissingComponentTwo")
        .hasMessageContaining("\$MissingComponentThree")
  }

  @Test
  fun `verify better contains component message`() {
    val testLithoView = lithoViewRule.render { EmptyComponent() }
    assertThatThrownBy {
          assertThat(testLithoView)
              .containsComponents(Row::class.java, Column::class.java, Text::class.java)
        }
        .hasMessageContaining("Row in LithoView, but did not find one")
        .hasMessageContaining("Column in LithoView, but did not find one")
        .hasMessageContaining("Text in LithoView, but did not find one")
  }

  @Test
  fun `verify InnerComponent has given props`() {
    // has_props_start
    val testLithoView = lithoViewRule.render { TestComponent() }
    assertThat(testLithoView).willRenderContent()
    val component = testLithoView.findComponent(InnerComponent::class)

    assertThat(component)
        .hasProps(
            InnerComponent::value to "some_value",
            InnerComponent::style to Style.height(100.dp).width(100.dp))
        .hasPropsMatching(InnerComponent::value to IsInstanceOf.instanceOf(String::class.java))
    // has_props_end
  }

  @Test
  fun `verify InnerComponent has given props asserting on LithoView`() {
    val testLithoView = lithoViewRule.render { TestComponent() }
    assertThat(testLithoView)
        .willRenderContent()
        .hasDirectMatchingComponent(
            InnerComponent::class,
            InnerComponent::value to "some_value",
            InnerComponent::style to Style.height(100.dp).width(100.dp))
        .hasDirectMatchingComponentWithMatcher(
            InnerComponent::class,
            InnerComponent::value to IsInstanceOf.instanceOf(String::class.java))
  }

  @Test
  fun `verify InnerComponent has given props not on a direct child asserting on LithoView`() {
    class WrapperComponent(val delegate: Component, val tag: String = "") : KComponent() {
      override fun ComponentScope.render(): Component = delegate
    }
    class ParentTestComponent : KComponent() {
      override fun ComponentScope.render(): Component {
        return WrapperComponent(
            delegate =
                WrapperComponent(tag = "TAGGED", delegate = Column { child(TestComponent()) }))
      }
    }

    val testLithoView = lithoViewRule.render { ParentTestComponent() }
    assertThat(testLithoView).willRenderContent()
    // We want to catch that the component does not have props in direct component
    assertThatThrownBy {
          assertThat(testLithoView)
              .hasDirectMatchingComponent(
                  InnerComponent::class,
                  InnerComponent::value to "some_value",
                  InnerComponent::style to Style.height(100.dp).width(100.dp))
        }
        .isInstanceOf(AssertionError::class.java)

    // We want to catch that the component does not have matching props to th matcher in direct
    // component
    assertThatThrownBy {
          assertThat(testLithoView)
              .hasDirectMatchingComponentWithMatcher(
                  InnerComponent::class,
                  InnerComponent::value to IsInstanceOf.instanceOf(String::class.java))
        }
        .isInstanceOf(AssertionError::class.java)

    assertThat(testLithoView)
        .hasDirectMatchingComponent(WrapperComponent::class, WrapperComponent::tag to "TAGGED")
        .hasAnyMatchingComponent(
            InnerComponent::class,
            InnerComponent::value to "some_value",
            InnerComponent::style to Style.height(100.dp).width(100.dp))
        .hasAnyMatchingComponentWithMatcher(
            InnerComponent::class,
            InnerComponent::value to IsInstanceOf.instanceOf(String::class.java))
  }

  @Test
  fun `verify lithoView is not null`() {
    class TestComponent : KComponent() {
      override fun ComponentScope.render(): Component? {
        return Row { child(Text(text = "some_text")) }
      }
    }
    // start_render_example
    val testLithoView = lithoViewRule.render { TestComponent() }
    // end_render_example
    assertThat(testLithoView).willRenderContent()
    /** can use all of the assertions from: [LithoViewAssert] class */
  }

  @Test
  fun `verify children of the component`() {
    // find_direct_component_start
    val testLithoView = lithoViewRule.render { TestComponent() }
    val innerComponent = testLithoView.findDirectComponent(InnerComponent::class)
    assertThat(innerComponent).isNotNull()
    // find_direct_component_end
    // find_non_direct_component_start
    val textComponent = testLithoView.findComponent(Text::class)
    assertThat(textComponent).isNotNull()
    // find_non_direct_component_end
  }

  @Test
  fun `verify order for finding components`() {
    class HeadComponent(val childComponent: Component) : KComponent() {
      override fun ComponentScope.render(): Component = childComponent
    }
    class DelegateComponent(val delegate: Component) : KComponent() {
      override fun ComponentScope.render(): Component = delegate
    }
    class RowWithChildren : KComponent() {
      override fun ComponentScope.render(): Component = Row {
        child(Text(text = "Hello, I'm not a direct component"))
        child(InnerComponent())
      }
    }

    val rowWithChildren = RowWithChildren()
    val secondLayerDelegate = DelegateComponent(rowWithChildren)
    val firstLayerDelegate = DelegateComponent(secondLayerDelegate)
    lateinit var rootComponent: Component

    val testLithoView =
        lithoViewRule.render { HeadComponent(firstLayerDelegate).also { rootComponent = it } }

    // Ensure that we find direct components from the head component to the tail component
    assertThat(testLithoView.rootComponent).isSameAs(rootComponent)
    assertThat(testLithoView)
        .containsDirectComponents(HeadComponent::class, DelegateComponent::class)
    assertThat(testLithoView.findDirectComponent(DelegateComponent::class))
        .isNotNull
        .isSameAs(firstLayerDelegate)
    assertThat(testLithoView.findAllComponents(DelegateComponent::class))
        .containsExactly(firstLayerDelegate, secondLayerDelegate)

    // Ensure that direct component includes and stops at first flexbox
    assertThat(testLithoView)
        .containsDirectComponents(RowWithChildren::class, Row::class)
        .doesNotContainDirectComponents(Text::class, InnerComponent::class)
    assertThat(testLithoView.findDirectComponent(RowWithChildren::class))
        .isNotNull
        .isSameAs(rowWithChildren)
    assertThat(testLithoView).hasVisibleText("Hello, I'm not a direct component")

    // Ensure that we can find non-direct components
    assertThat(testLithoView.findComponent(Text::class)).isNotNull
    assertThat(testLithoView.findComponent(InnerComponent::class)).isNotNull
  }

  @Test
  fun `verify assertions on lithoview `() {
    // lithoview_assertion_start
    val testLithoView = lithoViewRule.render { TestComponent() }
    assertThat(testLithoView).hasVisibleText("some_value")
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
    val testLithoView = lithoViewRule.render { TestClickComponent(mockClickHandler) }
    lithoViewRule.act(testLithoView) { clickOnRootView() }

    verify(mockClickHandler).onClick(any())
  }

  @Test(expected = IllegalStateException::class)
  fun `verify runtime exception thrown when clicking on a non-clickable view`() {
    val testLithoView = lithoViewRule.render { Row() }
    lithoViewRule.act(testLithoView) { clickOnRootView() }
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
                    .onClick { showText.update { isTextShowing -> !isTextShowing } }
                    .contentDescription("row")) {
              if (showText.value) {
                child(Text(text = "Text"))
              }
            }
      }
    }
    // component_for_action_end

    // test_interaction_start
    val testLithoView = lithoViewRule.render { TestComponent() }
    LithoStats.resetAllCounters()
    /** Find [Component] based on the text or [Component] class */
    assertThat(testLithoView.findViewWithTextOrNull("Text")).isNull()
    assertThat(testLithoView.findComponent(Text::class)).isNull()

    /** perform interaction defined in [LithoViewRule] */
    lithoViewRule.act(testLithoView) { clickOnContentDescription("row") }

    /** check number of state updates */
    assertThat(LithoStats.getComponentTriggeredAsyncStateUpdateCount()).isEqualTo(1)

    /** Find [Component] based on the text or [Component] class */
    assertThat(testLithoView.findViewWithTextOrNull("Text")).isNotNull()
    assertThat(testLithoView.findComponent(Text::class)).isNotNull()
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
    assertThat(stateRef.get()).isEqualTo(false)
    lithoViewRule.idle()
    assertThat(stateRef.get()).isEqualTo(true)
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
