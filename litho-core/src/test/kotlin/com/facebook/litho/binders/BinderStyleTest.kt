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
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import com.facebook.litho.binders.viewBinder
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Progress
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.assertj.LithoAssertions
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.wrapInView
import com.facebook.litho.widget.ProgressView
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import java.util.LinkedList
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class BinderStyleTest {

  @get:Rule val lithoViewRule = LithoViewRule()

  @Test
  fun `MountSpec - binder invoked on mount and unmount`() {
    val testBinder = ViewTestBinder()

    val lithoView =
        lithoViewRule.render {
          ComponentWithDrawableMountSpec(textStyle = Style.viewBinder(testBinder))
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
        lithoViewRule.render { ComponentWithTextViewPrimitive(style = Style.viewBinder(binder)) }
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
    val testBinder = ViewTestBinder(shouldUpdate = true)

    val lithoView =
        lithoViewRule.render {
          ComponentWithDrawableMountSpec(textStyle = Style.viewBinder(testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoViewRule.act(lithoView) { clickOnText("Click me") }

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
    val testBinder = ViewTestBinder(shouldUpdate = false)

    val lithoView =
        lithoViewRule.render {
          ComponentWithDrawableMountSpec(textStyle = Style.viewBinder(testBinder))
        }
    LithoAssertions.assertThat(lithoView).hasVisibleText("Hello")

    testBinder.assertNumOfBindInvocations(1)
    testBinder.assertBoundContentType(0, ComponentHost::class.java)
    testBinder.assertNoContentHasBeenUnbound()

    lithoViewRule.act(lithoView) { clickOnText("Click me") }

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
        lithoViewRule.render {
          ComponentWithDrawableMountSpec(
              textStyle = Style.viewBinder(overridenBinder).viewBinder(usedBinder))
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
        lithoViewRule.render {
          ComponentWithTextViewPrimitive(
              style = Style.viewBinder(overridenBinder).viewBinder(usedBinder))
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
        lithoViewRule.render {
          ComponentWithDrawableMountSpec(textStyle = Style.viewBinder(testBinder))
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
        lithoViewRule.render {
          ComponentWithDrawablePrimitive(style = Style.viewBinder(testBinder))
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
        lithoViewRule.render {
          ComponentWithDrawableMountSpec(textStyle = Style.wrapInView().viewBinder(testBinder))
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
        lithoViewRule.render {
          ComponentWithDrawablePrimitive(style = Style.wrapInView().viewBinder(testBinder))
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
        lithoViewRule.render {
          ComponentWithViewMountSpec(style = Style.wrapInView().viewBinder(testBinder))
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
        lithoViewRule.render {
          ComponentWithTextViewPrimitive(style = Style.wrapInView().viewBinder(testBinder))
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
        lithoViewRule.render {
          ComponentWithDrawableMountSpec(rootStyle = Style.viewBinder(testBinder))
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

  private class ViewTestBinder(
      shouldUpdate: Boolean = false,
  ) : TestBinder<View>(shouldUpdate, { view -> view.javaClass })

  private abstract class TestBinder<T>(
      private val shouldUpdate: Boolean = false,
      private val extractor: (T) -> Class<*>
  ) : RenderUnit.Binder<Unit, T, Any> {

    private val bindContent: MutableList<Class<*>> = LinkedList()
    private var numBindInvocations = 0

    private val unbindContent: MutableList<Class<*>> = LinkedList()
    private var numUnbindInvocations = 0

    override fun shouldUpdate(
        currentModel: Unit,
        newModel: Unit,
        currentLayoutData: Any?,
        nextLayoutData: Any?
    ): Boolean = shouldUpdate

    override fun bind(context: Context, content: T, model: Unit, layoutData: Any?): Any {
      numBindInvocations++
      bindContent.add(extractor(content))
      return Unit
    }

    override fun unbind(
        context: Context,
        content: T,
        model: Unit,
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
