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
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import com.facebook.litho.binders.onBindViewWithDescription
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Progress
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.wrapInView
import com.facebook.litho.widget.ProgressView
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.BindFunc
import com.facebook.rendercore.primitives.BindScope
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.UnbindFunc
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import java.util.LinkedList
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class BinderStyleTest {

  @get:Rule val lithoTestRule = LithoTestRule()

  @Test
  fun `when Style onBind is set on a component then mount callbacks should be invoked`() {
    var wasBound = false
    var wasUnBound = false
    var view: View? = null

    val handle =
        lithoTestRule.render {
          ComponentWithTextViewPrimitive(
              style =
                  Style.onBindViewWithDescription({ "test-binder" }, Unit) { content ->
                    wasBound = true
                    content.tag = "test-tag"
                    view = content
                    onUnbind {
                      wasUnBound = true
                      content.tag = null
                    }
                  })
        }

    assertThat(wasBound).isTrue
    assertThat(wasUnBound).isFalse
    assertThat(view).isNotNull
    assertThat(view?.tag).isEqualTo("test-tag")

    handle.lithoView.unmountAllItems()

    assertThat(wasUnBound).isTrue
    assertThat(view?.tag).isNull()
  }

  @Test
  fun `when multiple Style onBind is set on a component then mount callbacks should be invoked`() {
    var wasBound1 = false
    var wasBound2 = false
    var wasUnBound1 = false
    var wasUnBound2 = false
    var view: View? = null

    val handle =
        lithoTestRule.render {
          ComponentWithTextViewPrimitive(
              style =
                  Style.onBindViewWithDescription({ "test-binder-1" }, Unit) { content ->
                        wasBound1 = true
                        content.tag = "test-tag-1"
                        view = content
                        onUnbind {
                          wasUnBound1 = true
                          content.tag = null
                        }
                      }
                      .onBindViewWithDescription({ "test-binder-2" }, Unit) { content ->
                        wasBound2 = true
                        content.tag = "test-tag-2"
                        view = content
                        onUnbind {
                          wasUnBound2 = true
                          content.tag = null
                        }
                      })
        }

    assertThat(wasBound1).isTrue
    assertThat(wasBound2).isTrue
    assertThat(wasUnBound1).isFalse
    assertThat(wasUnBound2).isFalse
    assertThat(view).isNotNull
    assertThat(view?.tag).isEqualTo("test-tag-2")

    handle.lithoView.unmountAllItems()

    assertThat(wasUnBound1).isTrue
    assertThat(wasUnBound2).isTrue
    assertThat(view?.tag).isNull()
  }

  @Test
  fun `when Style onBind with deps is set on a component then mount callbacks should be invoked when they change`() {
    var bindCalls = 0
    var unbindCalls = 0

    class TestComponent(val deps: () -> Array<out Any?>) : KComponent() {
      override fun ComponentScope.render(): Component {
        val dummyState = useState { 0 }
        return Column {
          child(
              Text(
                  "Hello",
                  style =
                      Style.onBindViewWithDescription({ "test-binder" }, *deps()) {
                        bindCalls++
                        onUnbind { unbindCalls++ }
                      },
              ),
          )
          child(Text("Click me", Style.onClick { dummyState.update { it + 1 } }))
        }
      }
    }

    var deps: Array<out Any?> = arrayOf(Unit)
    val handle = lithoTestRule.render { TestComponent { deps } }

    assertThat(bindCalls).isEqualTo(1)
    assertThat(unbindCalls).isEqualTo(0)

    lithoTestRule.act(handle) { clickOnText("Click me") }

    assertThat(bindCalls).isEqualTo(1)
    assertThat(unbindCalls).isEqualTo(0)

    deps = arrayOf(Any())

    lithoTestRule.act(handle) { clickOnText("Click me") }

    assertThat(bindCalls).isEqualTo(2)
    assertThat(unbindCalls).isEqualTo(1)
  }

  @Test
  fun `when Style onBind with should update is set on a component then mount callbacks should be invoked when they change`() {
    var bindCalls = 0
    var unbindCalls = 0

    class TestComponent(val shouldUpdateFunc: (Any?, Any?) -> Boolean) : KComponent() {
      override fun ComponentScope.render(): Component {
        val dummyState = useState { 0 }
        return Column {
          child(
              Text(
                  "Hello",
                  style =
                      Style.onBindViewWithDescription(
                          { "test-binder" },
                          Unit,
                          func =
                              object : BindFunc<View> {
                                override fun BindScope.bind(content: View): UnbindFunc {
                                  bindCalls++
                                  return onUnbind { unbindCalls++ }
                                }

                                override fun shouldUpdate(
                                    currentModel: Any?,
                                    newModel: Any?,
                                    currentLayoutData: Any?,
                                    nextLayoutData: Any?,
                                ): Boolean {
                                  return shouldUpdateFunc(currentModel, newModel)
                                }
                              }),
              ),
          )
          child(Text("Click me", Style.onClick { dummyState.update { it + 1 } }))
        }
      }
    }

    var shouldUpdate = false
    val shouldUpdateFunc: (Any?, Any?) -> Boolean = { _, _ -> shouldUpdate }
    val handle = lithoTestRule.render { TestComponent(shouldUpdateFunc = shouldUpdateFunc) }

    assertThat(bindCalls).isEqualTo(1)
    assertThat(unbindCalls).isEqualTo(0)

    lithoTestRule.act(handle) { clickOnText("Click me") }

    assertThat(bindCalls).isEqualTo(1)
    assertThat(unbindCalls).isEqualTo(0)

    shouldUpdate = true

    lithoTestRule.act(handle) { clickOnText("Click me") }

    assertThat(bindCalls).isEqualTo(2)
    assertThat(unbindCalls).isEqualTo(1)
  }

  @Test
  fun `MountSpec - binder invoked on mount and unmount`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithDrawableMountSpec(
              textStyle =
                  Style.onBindViewWithDescription({ "test-binder" }, Unit, func = testBinder))
        }

    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, ComponentHost::class.java)
  }

  @Test
  fun `Primitives - binder invoked on mount and unmount`() {
    val binder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithTextViewPrimitive(
              style = Style.onBindViewWithDescription({ "test-binder" }, Unit, func = binder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    binder.assertNumOfBindInvocations(1)
    binder.assertBoundContentType(0, TextView::class.java)
    binder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    binder.assertNumOfBindInvocations(1)
    binder.assertBoundContentType(0, TextView::class.java)

    binder.assertNumOfUnbindInvocations(1)
    binder.assertUnboundContentType(0, TextView::class.java)
  }

  @Test
  fun `binder invoked on re-mount when shouldUpdate is true`() {
    val testBinder = ViewTestBinder { _, _ -> true }

    val lithoView =
        lithoTestRule.render {
          ComponentWithDrawableMountSpec(
              textStyle =
                  Style.onBindViewWithDescription({ "test-binder" }, Any(), func = testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoTestRule.act(lithoView) { clickOnText("Click me") }

    testBinder.assertNumOfBindInvocations(2)
    testBinder.assertNumOfUnbindInvocations(1)

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(2)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNumOfUnbindInvocations(2)
    testBinder.assertUnboundContentType(0, ComponentHost::class.java)
  }

  @Test
  fun `binder not invoked on re-mount when shouldUpdate is false`() {
    val testBinder = ViewTestBinder { _, _ -> false }

    val lithoView =
        lithoTestRule.render {
          ComponentWithDrawableMountSpec(
              textStyle =
                  Style.onBindViewWithDescription({ "test-binder" }, Unit, func = testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoTestRule.act(lithoView) { clickOnText("Click me") }

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertNumOfUnbindInvocations(0)

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, ComponentHost::class.java)
  }

  @Test
  fun `Mount Spec - when setting two binders of the same type binder only the last will be used`() {
    val overridenBinder = ViewTestBinder()
    val usedBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithDrawableMountSpec(
              textStyle =
                  Style.onBindViewWithDescription({ "test-binder-1" }, Unit, func = overridenBinder)
                      .onBindViewWithDescription({ "test-binder-2" }, Unit, func = usedBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    overridenBinder.assertNoContentHasBeenBound()
    overridenBinder.assertNoContentHasBeenUnbound()

    usedBinder.assertNumOfBindInvocations(1)
    usedBinder.assertBoundContentType(0, ComponentHost::class.java)
    usedBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    overridenBinder.assertNoContentHasBeenBound()
    overridenBinder.assertNoContentHasBeenUnbound()

    usedBinder.assertNumOfBindInvocations(1)
    usedBinder.assertBoundContentType(0, ComponentHost::class.java)

    usedBinder.assertNumOfUnbindInvocations(1)
    usedBinder.assertUnboundContentType(0, ComponentHost::class.java)
  }

  @Test
  fun `Primitive - when setting two binders of the same type binder only the last will be used`() {
    val overridenBinder = ViewTestBinder()
    val usedBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithTextViewPrimitive(
              style =
                  Style.onBindViewWithDescription({ "test-binder" }, Unit, func = overridenBinder)
                      .onBindViewWithDescription({ "test-binder" }, Unit, func = usedBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    overridenBinder.assertNoContentHasBeenBound()
    overridenBinder.assertNoContentHasBeenUnbound()

    usedBinder.assertNumOfBindInvocations(1)
    usedBinder.assertBoundContentType(0, TextView::class.java)
    usedBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    overridenBinder.assertNoContentHasBeenBound()
    overridenBinder.assertNoContentHasBeenUnbound()

    usedBinder.assertNumOfBindInvocations(1)
    usedBinder.assertBoundContentType(0, TextView::class.java)

    usedBinder.assertNumOfUnbindInvocations(1)
    usedBinder.assertUnboundContentType(0, TextView::class.java)
  }

  @Test
  fun `MountSpec - setting a binder in a drawable mount which has no view properties that elevates it to ComponentHost works correctly`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithDrawableMountSpec(
              textStyle =
                  Style.onBindViewWithDescription({ "test-binder" }, Unit, func = testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)

    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, ComponentHost::class.java)
  }

  @Test
  fun `Primitive - setting a binder in a drawable mount which has no view properties that elevates it to ComponentHost works correctly`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithDrawablePrimitive(
              style = Style.onBindViewWithDescription({ "test-binder" }, Unit, func = testBinder))
        }

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)

    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, ComponentHost::class.java)
  }

  @Test
  fun `MountSpec - setting a binder in a drawable mount which requires an host view will set the binder in the host`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithDrawableMountSpec(
              textStyle =
                  Style.wrapInView()
                      .onBindViewWithDescription({ "test-binder" }, Unit, func = testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)

    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, ComponentHost::class.java)
  }

  @Test
  fun `Primitives - setting a binder in a drawable mount which requires an host view will set the binder in the host`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithDrawablePrimitive(
              style =
                  Style.wrapInView()
                      .onBindViewWithDescription({ "test-binder" }, Unit, func = testBinder))
        }

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)

    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, ComponentHost::class.java)
  }

  @Test
  fun `MountSpec - setting a binder in a view mount which requires an host view will set the binder in the view mount`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithViewMountSpec(
              style =
                  Style.wrapInView()
                      .onBindViewWithDescription({ "test-binder" }, Unit, func = testBinder))
        }

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ProgressView::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ProgressView::class.java)

    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, ProgressView::class.java)
  }

  @Test
  fun `Primitives - setting a binder in a view mount which requires an host view will set the binder in the view mount`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithTextViewPrimitive(
              style =
                  Style.wrapInView()
                      .onBindViewWithDescription({ "test-binder" }, Unit, func = testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, TextView::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, TextView::class.java)

    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, TextView::class.java)
  }

  @Test
  fun `setting a binder in the root host works correctly`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoTestRule.render {
          ComponentWithDrawableMountSpec(
              rootStyle =
                  Style.onBindViewWithDescription({ "test-binder" }, Unit, func = testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, LithoView::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, LithoView::class.java)

    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, LithoView::class.java)
  }

  private class ComponentWithDrawableMountSpec(
      private val rootStyle: Style = Style,
      private val textStyle: Style = Style,
  ) : KComponent() {

    override fun ComponentScope.render(): Component {
      val dummyState = useState { 0 }
      return Column(style = rootStyle) {
        child(Row { child(Text("Hello", style = textStyle)) })
        child(Text("Click me", Style.onClick { dummyState.update { it + 1 } }))
      }
    }
  }

  private class ComponentWithViewMountSpec(private val style: Style = Style) : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column { child(Progress(style = style.width(100.dp).height(100.dp))) }
    }
  }

  private class ComponentWithTextViewPrimitive(private val style: Style) : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column { child(TextPrimitiveComponent(style)) }
    }
  }

  private class ComponentWithDrawablePrimitive(private val style: Style) : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column { child(ColorDrawablePrimitiveComponent(style)) }
    }
  }

  fun ViewTestBinder(
      shouldUpdateFunc: (Any?, Any?) -> Boolean = { _, _ -> false },
  ): TestBinder<View> {
    return TestBinder(shouldUpdateFunc)
  }

  class TestBinder<T : Any>(
      private val shouldUpdateFunc: (Any?, Any?) -> Boolean = { _, _ -> false },
      private val extractor: (T) -> Class<*> = { view -> view.javaClass }
  ) : BindFunc<T> {

    private val bindContent: MutableList<Class<*>> = LinkedList()
    private var numBindInvocations = 0

    private val unbindContent: MutableList<Class<*>> = LinkedList()
    private var numUnbindInvocations = 0

    override fun BindScope.bind(content: T): UnbindFunc {
      numBindInvocations++
      bindContent.add(extractor(content))
      return onUnbind {
        numUnbindInvocations++
        unbindContent.add(extractor(content))
      }
    }

    override fun shouldUpdate(
        currentModel: Any?,
        newModel: Any?,
        currentLayoutData: Any?,
        nextLayoutData: Any?,
    ): Boolean {
      return shouldUpdateFunc(currentModel, newModel)
    }

    fun assertNoContentHasBeenBound() {
      Assertions.assertThat(bindContent).isEmpty()
    }

    fun assertNoContentHasBeenUnbound() {
      Assertions.assertThat(unbindContent).isEmpty()
    }

    fun assertNumOfBindInvocations(num: Int) {
      Assertions.assertThat(numBindInvocations).isEqualTo(num)
    }

    fun assertNumOfUnbindInvocations(num: Int) {
      Assertions.assertThat(numUnbindInvocations).isEqualTo(num)
    }

    fun assertBoundContentType(index: Int, type: Class<*>) {
      Assertions.assertThat(bindContent[index]).isNotNull
      Assertions.assertThat(bindContent[index]).isEqualTo(type)
    }

    fun assertUnboundContentType(index: Int, type: Class<*>) {
      Assertions.assertThat(unbindContent[index]).isNotNull
      Assertions.assertThat(unbindContent[index]).isEqualTo(type)
    }
  }

  private class TextPrimitiveComponent(private val style: Style) : PrimitiveComponent() {

    @Suppress("TestFunctionName")
    private fun PrimitiveComponentScope.TextPrimitive(
        text: String,
        tag: String? = null
    ): Primitive {
      return Primitive(
          layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
          mountBehavior =
              MountBehavior(ViewAllocator { context -> TextView(context) }) {
                text.bindTo(TextView::setText, "")
                tag.bindTo(TextView::setTag)
                tag.bindTo(TextView::setContentDescription)
              })
    }

    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      return LithoPrimitive(TextPrimitive(text = "Hello"), style = style)
    }
  }

  private class ColorDrawablePrimitiveComponent(private val style: Style) : PrimitiveComponent() {

    @Suppress("TestFunctionName")
    private fun PrimitiveComponentScope.ColorDrawablePrimitive(): Primitive {
      return Primitive(
          layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
          mountBehavior = MountBehavior(DrawableAllocator { ColorDrawable(Color.RED) }) {})
    }

    override fun PrimitiveComponentScope.render(): LithoPrimitive {
      return LithoPrimitive(ColorDrawablePrimitive(), style = style)
    }
  }
}
