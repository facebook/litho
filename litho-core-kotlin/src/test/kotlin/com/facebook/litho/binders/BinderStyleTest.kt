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

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import com.facebook.litho.binders.drawableBinder
import com.facebook.litho.binders.viewBinder
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.TextDrawable
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.ViewAllocator
import java.util.LinkedList
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class BinderStyleTest {

  @get:Rule val lithoViewRule = LithoViewRule()

  @Test
  fun `binder around MountSpec invoked on mount and unmount`() {
    val testBinder = DrawableTestBinder()

    val lithoView =
        lithoViewRule.render {
          ComponentWithMountSpec(textStyle = Style.drawableBinder(testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, TextDrawable::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, TextDrawable::class.java)
    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, TextDrawable::class.java)
  }

  @Test
  fun `binder invoked on re-mount when shouldUpdate is true`() {
    val testBinder = DrawableTestBinder(shouldUpdate = true)

    val lithoView =
        lithoViewRule.render {
          ComponentWithMountSpec(textStyle = Style.drawableBinder(testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, TextDrawable::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoViewRule.act(lithoView) { clickOnText("Click me") }

    testBinder.assertNumOfBindInvocations(2)
    testBinder.assertNumOfUnbindInvocations(1)

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(2)
    testBinder.assertBoundContentType(0, TextDrawable::class.java)
    testBinder.assertNumOfUnbindInvocations(2)
    testBinder.assertUnboundContentType(0, TextDrawable::class.java)
  }

  @Test
  fun `binder not invoked on re-mount when shouldUpdate is false`() {
    val testBinder = DrawableTestBinder(shouldUpdate = false)

    val lithoView =
        lithoViewRule.render {
          ComponentWithMountSpec(textStyle = Style.drawableBinder(testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, TextDrawable::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoViewRule.act(lithoView) { clickOnText("Click me") }

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertNumOfUnbindInvocations(0)

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, TextDrawable::class.java)
    testBinder.assertNumOfUnbindInvocations(1)
    testBinder.assertUnboundContentType(0, TextDrawable::class.java)
  }

  @Test
  fun `binder around Primitive invoked on mount and unmount`() {
    val binder = ViewTestBinder()

    val lithoView =
        lithoViewRule.render { ComponentWithPrimitive(style = Style.viewBinder(binder)) }
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
  fun `binder around Mountable invoked on mount and unmount`() {
    val binder = ViewTestBinder()

    val lithoView =
        lithoViewRule.render { HelloMountableComponent(style = Style.viewBinder(binder)) }
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
  fun `usage of viewBinder around mount binds into wrapping component host`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoViewRule.render { ComponentWithMountSpec(textStyle = Style.viewBinder(testBinder)) }
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
  fun `when setting two binders of the same type binder only the last will be used`() {
    val overridenBinder = DrawableTestBinder()
    val usedBinder = DrawableTestBinder()

    val lithoView =
        lithoViewRule.render {
          ComponentWithMountSpec(
              textStyle = Style.drawableBinder(overridenBinder).drawableBinder(usedBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    overridenBinder.assertNoContentHasBeenBound()
    overridenBinder.assertNoContentHasBeenUnbound()

    usedBinder.assertNumOfBindInvocations(1)
    usedBinder.assertBoundContentType(0, TextDrawable::class.java)
    usedBinder.assertNoContentHasBeenUnbound()

    lithoView.lithoView.notifyVisibleBoundsChanged(Rect(0, 2040, 1080, 3060), true)

    overridenBinder.assertNoContentHasBeenBound()
    overridenBinder.assertNoContentHasBeenUnbound()

    usedBinder.assertNumOfBindInvocations(1)
    usedBinder.assertBoundContentType(0, TextDrawable::class.java)

    usedBinder.assertNumOfUnbindInvocations(1)
    usedBinder.assertUnboundContentType(0, TextDrawable::class.java)
  }

  @Test
  fun `setting a binder in a layout component which has no view properties that elevates it to ComponentHost works correctly`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoViewRule.render { ComponentWithMountSpec(rowStyle = Style.viewBinder(testBinder)) }
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
  fun `setting a binder in a layout component with view properties that require host view works correctly`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoViewRule.render {
          ComponentWithMountSpec(rowStyle = Style.viewTag("test-tag").viewBinder(testBinder))
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
  fun `setting a drawable binder in a view mount component should throw an exception`() {
    val testBinder = DrawableTestBinder()

    val throwable =
        Assertions.catchThrowable {
          lithoViewRule.render {
            ComponentWithMountSpec(rowStyle = Style.viewTag("test-tag").drawableBinder(testBinder))
          }
        }

    Assertions.assertThat(throwable).isNotNull
  }

  private class ComponentWithMountSpec(
      private val rowStyle: Style = Style,
      private val textStyle: Style = Style,
  ) : KComponent() {

    override fun ComponentScope.render(): Component {
      val dummyState = useState { 0 }
      return Column {
        child(Row(style = rowStyle) { child(Text("Hello", style = textStyle)) })
        child(Text("Click me", Style.onClick { dummyState.update { it + 1 } }))
      }
    }
  }

  private class ComponentWithPrimitive(private val style: Style) : KComponent() {

    override fun ComponentScope.render(): Component {
      return Column { child(HelloPrimitiveComponent(style)) }
    }
  }

  private class ViewTestBinder(
      shouldUpdate: Boolean = false,
  ) : TestBinder<View>(shouldUpdate, { view -> view.javaClass })

  private class DrawableTestBinder(shouldUpdate: Boolean = false) :
      TestBinder<Drawable>(shouldUpdate, { drawable -> drawable.javaClass })

  private abstract class TestBinder<T>(
      private val shouldUpdate: Boolean = false,
      private val extractor: (T) -> Class<*>
  ) : RenderUnit.Binder<Any?, T, Any?> {

    private val bindContent: MutableList<Class<*>> = LinkedList()
    private var numBindInvocations = 0

    private val unbindContent: MutableList<Class<*>> = LinkedList()
    private var numUnbindInvocations = 0

    override fun shouldUpdate(
        currentModel: Any?,
        newModel: Any?,
        currentLayoutData: Any?,
        nextLayoutData: Any?
    ): Boolean = shouldUpdate

    override fun bind(context: Context?, content: T, model: Any?, layoutData: Any?): Any {
      numBindInvocations++
      bindContent.add(extractor(content))
      return Unit
    }

    override fun unbind(
        context: Context?,
        content: T,
        model: Any?,
        layoutData: Any?,
        bindData: Any?
    ) {
      numUnbindInvocations++
      unbindContent.add(extractor(content))
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

  private class HelloPrimitiveComponent(private val style: Style) : PrimitiveComponent() {

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

  private class HelloMountableComponent(
      private val style: Style,
  ) : MountableComponent() {

    override fun MountableComponentScope.render(): MountableRenderResult {
      return MountableRenderResult(SimpleTextMountable("Hello"), style = style)
    }
  }

  private class SimpleTextMountable(private val text: String) :
      SimpleMountable<TextView>(RenderType.VIEW) {

    override fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult =
        withEqualSize(widthSpec, heightSpec)

    override fun mount(c: Context, content: TextView, layoutData: Any?) {
      content.text = text
    }

    override fun unmount(c: Context, content: TextView, layoutData: Any?) {
      content.text = null
    }

    override fun createContent(context: Context): TextView = TextView(context)
  }
}
