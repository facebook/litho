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
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import com.facebook.litho.accessibility.ImportantForAccessibility
import com.facebook.litho.accessibility.accessibilityRole
import com.facebook.litho.accessibility.accessibilityRoleDescription
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.accessibility.importantForAccessibility
import com.facebook.litho.accessibility.onInitializeAccessibilityNodeInfo
import com.facebook.litho.accessibility.onPopulateAccessibilityNode
import com.facebook.litho.animated.alpha
import com.facebook.litho.core.height
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.border
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Border
import com.facebook.litho.kotlin.widget.BorderEdge
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.match
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.focusable
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.litho.visibility.onInvisible
import com.facebook.litho.visibility.onVisible
import com.facebook.litho.widget.LithoScrollView
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.Size
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.DrawableAllocator
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.Primitive
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px
import com.facebook.rendercore.testing.ViewAssertions
import com.facebook.rendercore.utils.fillSpace
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class PrimitiveComponentsTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()
  @Rule @JvmField val expectedException = ExpectedException.none()

  @Test
  fun `should render primitive component`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()

    val testView =
        lithoViewRule.render {
          Column {
            child(
                TestViewPrimitiveComponent(
                    TextView(c.androidContext),
                    steps,
                    style =
                        Style.width(100.px)
                            .height(100.px)
                            .padding(all = 20.px)
                            .backgroundColor(Color.LTGRAY)
                            .border(
                                Border(edgeAll = BorderEdge(width = 5f.dp, color = Color.BLACK)))))
          }
        }
    testView.lithoView.unmountAllItems()

    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_UNMOUNT)
  }

  @Test
  fun `width, height, focusable, viewTag styles respected when set`() {
    val testComponent =
        TestViewPrimitiveComponent(
            view = TextView(lithoViewRule.context.androidContext),
            style = Style.width(667.px).height(668.px).focusable(true).viewTag("test_view_tag"))

    val testView = lithoViewRule.render { testComponent }

    assertThat(testView.lithoView.childCount).isEqualTo(1)
    val realTestView = testView.lithoView.getChildAt(0)

    ViewAssertions.assertThat(realTestView).matches(match<TextView> { bounds(0, 0, 667, 668) })

    assertThat(realTestView.isFocusable).isTrue()

    testView.findViewWithTag("test_view_tag")
  }

  @Test
  fun `width, height, focusable, viewTag styles respected when updated`() {

    val testView =
        lithoViewRule.render {
          TestViewPrimitiveComponent(
              view = TextView(lithoViewRule.context.androidContext),
              style = Style.width(667.px).height(668.px).focusable(true).viewTag("test_view_tag"),
          )
        }

    assertThat(testView.lithoView.childCount).isEqualTo(1)
    val realTestView = testView.lithoView.getChildAt(0)

    ViewAssertions.assertThat(realTestView).matches(match<TextView> { bounds(0, 0, 667, 668) })

    assertThat(realTestView.isFocusable).isTrue

    testView.findViewWithTag("test_view_tag")

    lithoViewRule.render(lithoView = testView.lithoView) {
      TestViewPrimitiveComponent(
          view = TextView(lithoViewRule.context.androidContext),
          style = Style.width(667.px).height(668.px).focusable(false).viewTag("new_test_view_tag"),
      )
    }

    assertThat(testView.lithoView.childCount).isEqualTo(1)
    val newRealTestView = testView.lithoView.getChildAt(0)

    assertThat(newRealTestView.isFocusable).isFalse

    testView.findViewWithTag("new_test_view_tag")
  }

  @Test
  fun `onClick event is dispatched when set`() {
    val wasClicked = AtomicBoolean(false)

    val testComponent =
        TestViewPrimitiveComponent(
            view = TextView(lithoViewRule.context.androidContext),
            style =
                Style.width(667.px).height(668.px).focusable(true).viewTag("click_me").onClick {
                  wasClicked.set(true)
                })

    val testView = lithoViewRule.render { testComponent }

    assertThat(wasClicked.get()).isFalse
    testView.findViewWithTag("click_me").performClick()
    assertThat(wasClicked.get()).isTrue
  }

  @Test
  fun `onVisible event is fired when set`() {
    val eventFired = AtomicBoolean(false)

    val testComponent =
        TestViewPrimitiveComponent(
            view = TextView(lithoViewRule.context.androidContext),
            style =
                Style.width(667.px).height(668.px).focusable(true).viewTag("click_me").onVisible {
                  eventFired.set(true)
                })

    lithoViewRule.render { testComponent }

    assertThat(eventFired.get()).isTrue
  }

  @Test
  fun `widthPercent and heightPercent is respected when set`() {
    val testComponent =
        TestViewPrimitiveComponent(
            view = TextView(lithoViewRule.context.androidContext),
            style = Style.heightPercent(50f).widthPercent(50f))

    val testView =
        lithoViewRule.render {
          Row(style = Style.width(100.px).height(100.px)) { child(testComponent) }
        }

    assertThat(testView.lithoView.childCount).isEqualTo(1)
    val realTestView = testView.lithoView.getChildAt(0)

    ViewAssertions.assertThat(realTestView).matches(match<TextView> { bounds(0, 0, 50, 50) })
  }

  @Test
  fun `dynamic alpha is respected when set`() {
    val alpha = 0.5f
    val alphaDV: DynamicValue<Float> = DynamicValue<Float>(alpha)

    val testComponent =
        TestViewPrimitiveComponent(
            view = TextView(lithoViewRule.context.androidContext),
            style = Style.width(100.px).height(100.px).alpha(alphaDV))

    val testView = lithoViewRule.render { testComponent }

    assertThat(testView.lithoView.alpha).isEqualTo(alpha)

    alphaDV.set(1f)
    assertThat(testView.lithoView.alpha).isEqualTo(1f)

    alphaDV.set(0.7f)
    assertThat(testView.lithoView.alpha).isEqualTo(0.7f)
  }

  @Test
  fun `updating the state in primitive takes effect`() {
    lateinit var stateRef: AtomicReference<String>

    class TestComponent(val view: View) : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val testState: State<String> = useState { "initial" }
        stateRef = AtomicReference(testState.value)
        return LithoPrimitive(
            ViewPrimitive(view = view, updateState = { testState.update { s -> s + "_" + it } }),
            style = null)
      }
    }

    lithoViewRule.render { TestComponent(TextView(lithoViewRule.context.androidContext)) }

    lithoViewRule.idle()

    assertThat(stateRef.get())
        .describedAs("String state is updated")
        .isEqualTo("initial_createContent_mount")
  }

  @Test
  fun `should not remeasure same primitive if size specs match`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val view = TextView(c.androidContext)
    val component =
        TestViewPrimitiveComponent(
            view = view,
            steps = steps,
            style = Style.width(100.px).height(100.px),
        )

    val testView = lithoViewRule.render { Column { child(component) } }

    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT)

    steps.clear()

    lithoViewRule.render(lithoView = testView.lithoView) { Column { child(component) } }

    assertThat(LifecycleStep.getSteps(steps)).containsExactly(LifecycleStep.RENDER)
  }

  @Test
  fun `should not remeasure same primitive if size specs match with non exact size`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val view = TextView(c.androidContext)
    val component =
        TestViewPrimitiveComponent(
            view = view,
            steps = steps,
            style = Style.width(100.px).flex(grow = 1f),
        )

    val testView = lithoViewRule.render { Column { child(component) } }

    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT)

    steps.clear()

    lithoViewRule.render(lithoView = testView.lithoView) { Column { child(component) } }

    assertThat(LifecycleStep.getSteps(steps)).containsExactly(LifecycleStep.RENDER)
  }

  @Test
  fun `should not remeasure primitive on state update if LayoutBehavior hasn't changed`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val view = TextView(c.androidContext)

    class TestComponent(val view: View) : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        val testState: State<String> = useState { "initial" }
        return LithoPrimitive(
            ViewPrimitive(view = view, steps = steps, str = testState.value),
            style = Style.viewTag("click_me").onClick { testState.update("updated") })
      }
    }

    val testView = lithoViewRule.render { TestComponent(view) }

    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.ON_MEASURE, LifecycleStep.ON_CREATE_MOUNT_CONTENT, LifecycleStep.ON_MOUNT)

    steps.clear()
    testView.findViewWithTag("click_me").performClick()

    lithoViewRule.render(lithoView = testView.lithoView) { TestComponent(view) }
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT)
  }

  @Test
  fun `should remeasure primitive if properties have changed`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val view = TextView(c.androidContext)

    val testView =
        lithoViewRule.render {
          Column {
            child(
                TestViewPrimitiveComponent(
                    identity = 0,
                    view = view,
                    steps = steps,
                    style = Style.width(100.px).flex(grow = 1f),
                ))
          }
        }

    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT)

    steps.clear()

    lithoViewRule.render(lithoView = testView.lithoView) {
      Column {
        child(
            TestViewPrimitiveComponent(
                identity = 1,
                view = view,
                steps = steps,
                style = Style.width(100.px).flex(grow = 1f),
            ))
      }
    }

    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT)
  }

  @Test
  fun `should remeasure primitive if size specs change`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val view = TextView(c.androidContext)
    val component = TestViewPrimitiveComponent(identity = 0, view = view, steps = steps)

    val testView = lithoViewRule.render(widthPx = 800, heightPx = 600) { component }

    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT)

    steps.clear()

    lithoViewRule.render(lithoView = testView.lithoView, widthPx = 1920, heightPx = 1080) {
      component
    }

    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT)
  }

  @Test
  fun `same instance should be equivalent`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val view = TextView(c.androidContext)
    val component = TestViewPrimitiveComponent(identity = 0, view = view, steps = steps)

    assertThat(component.isEquivalentTo(component)).isTrue
    assertThat(component.isEquivalentTo(component, true)).isTrue
  }

  @Test
  fun `components with same prop values should be equivalent`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val view = TextView(c.androidContext)
    val a = TestViewPrimitiveComponent(identity = 0, view = view, steps = steps)
    val b = TestViewPrimitiveComponent(identity = 0, view = view, steps = steps)
    assertThat(a.isEquivalentTo(b)).isTrue
    assertThat(a.isEquivalentTo(b, true)).isTrue
  }

  @Test
  fun `components with different prop values should not be equivalent`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val view = TextView(c.androidContext)
    val a = TestViewPrimitiveComponent(identity = 0, view = view, steps = steps)
    val b = TestViewPrimitiveComponent(identity = 1, view = view, steps = steps)

    assertThat(a.isEquivalentTo(b)).isFalse
    assertThat(a.isEquivalentTo(b, true)).isFalse
  }

  @Test
  fun `components with different style values should not be equivalent`() {
    val c = lithoViewRule.context
    val steps = mutableListOf<LifecycleStep.StepInfo>()
    val view = TextView(c.androidContext)
    val a =
        TestViewPrimitiveComponent(
            identity = 0,
            view = view,
            steps = steps,
            style = Style.width(100.px).height(100.px), /* 100 here */
        )

    val b =
        TestViewPrimitiveComponent(
            identity = 0,
            view = view,
            steps = steps,
            style = Style.width(200.px).height(200.px), /* 200 here */
        )

    assertThat(a.isEquivalentTo(b, true)).isFalse
  }

  @Test
  fun `when a11y props are set on style it should set them on the rendered content`() {
    val eventHandler: EventHandler<OnInitializeAccessibilityNodeInfoEvent> = mock()
    val onPopulateAccessibilityNodeHandler: EventHandler<OnPopulateAccessibilityNodeEvent> = mock()

    val component =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            style =
                Style.accessibilityRole(AccessibilityRole.EDIT_TEXT)
                    .accessibilityRoleDescription("Accessibility Test")
                    .contentDescription("Accessibility Test")
                    .importantForAccessibility(ImportantForAccessibility.YES)
                    .onInitializeAccessibilityNodeInfo { eventHandler }
                    .onPopulateAccessibilityNode { onPopulateAccessibilityNodeHandler })

    // verify that info is set on the LithoView where possible, otherwise on LithoNode
    val testView = lithoViewRule.render { component }
    val node = testView.currentRootNode?.node
    val nodeInfo = node?.nodeInfo
    assertThat(nodeInfo?.accessibilityRole).isEqualTo(AccessibilityRole.EDIT_TEXT)
    assertThat(nodeInfo?.accessibilityRoleDescription).isEqualTo("Accessibility Test")
    assertThat(testView.lithoView.getChildAt(0).contentDescription).isEqualTo("Accessibility Test")
    assertThat(testView.lithoView.getChildAt(0).importantForAccessibility)
        .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_YES)
    assertThat(nodeInfo?.onInitializeAccessibilityNodeInfoHandler).isNotNull
    assertThat(nodeInfo?.onPopulateAccessibilityNodeHandler).isNotNull
  }

  @Test
  fun `TestDrawablePrimitiveComponent has IMAGE accessibility role by default but overriding it works`() {
    val component1 =
        TestDrawablePrimitiveComponent(
            drawable = ColorDrawable(Color.RED), style = Style.width(100.px).height(100.px))

    val testView1 = lithoViewRule.render { component1 }
    val node1 = testView1.currentRootNode?.node
    val nodeInfo1 = node1?.nodeInfo
    assertThat(nodeInfo1?.accessibilityRole).isEqualTo(AccessibilityRole.IMAGE)

    val component2 =
        TestDrawablePrimitiveComponent(
            drawable = ColorDrawable(Color.RED),
            style =
                Style.width(100.px)
                    .height(100.px)
                    .accessibilityRole(AccessibilityRole.IMAGE_BUTTON))

    val testView2 = lithoViewRule.render { component2 }
    val node2 = testView2.currentRootNode?.node
    val nodeInfo2 = node2?.nodeInfo
    assertThat(nodeInfo2?.accessibilityRole).isEqualTo(AccessibilityRole.IMAGE_BUTTON)
  }

  @Test
  fun `when dynamic value is set it should update the content`() {
    val tag = DynamicValue<Any?>("0")
    val root =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            dynamicTag = tag,
            style = Style.width(100.px).height(100.px))

    val test = lithoViewRule.render { root }

    test.findViewWithTag("0")

    tag.set("1")

    test.findViewWithTag("1")
  }

  @Test
  fun `when component with dynamic value is unmounted it should unbind the dynamic value`() {
    val tag = DynamicValue<Any?>("0")
    val root =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            dynamicTag = tag,
            style = Style.width(100.px).height(100.px))

    val test = lithoViewRule.render { root }

    val view = test.findViewWithTag("0")

    test.lithoView.setComponentTree(null, true)

    assertThat(tag.numberOfListeners).isEqualTo(0)

    tag.set("1")

    // tag should be set to default value
    assertThat(view.tag).isEqualTo("default_value")
  }

  @Test
  fun `when new dynamic value is set it should unbind the old dynamic value`() {
    val tag1 = DynamicValue<Any?>("0")
    val root1 =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            dynamicTag = tag1,
            style = Style.width(100.px).height(100.px))

    val test = lithoViewRule.render { root1 }

    test.findViewWithTag("0")

    tag1.set("1")

    test.findViewWithTag("1")

    assertThat(tag1.numberOfListeners).isEqualTo(1)

    val tag2 = DynamicValue<Any?>("2")
    val root2 =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            dynamicTag = tag2,
            style = Style.width(100.px).height(100.px))

    test.setRoot(root2)

    assertThat(tag1.numberOfListeners).isEqualTo(0)

    // should have view with new tag
    val view = test.findViewWithTag("2")

    // set new tag using the old dynamic value
    tag1.set("3")

    // the above should not work, the tag should not change
    assertThat(view.tag).isEqualTo("2")

    // set the new tag using the new dynamic value
    tag2.set("3")

    // the above should work, the tag should change
    assertThat(view.tag).isEqualTo("3")

    assertThat(tag2.numberOfListeners).isEqualTo(1)
  }

  @Test
  fun `when new dynamic value is null it should unbind the old dynamic value`() {
    val tag1 = DynamicValue<Any?>("0")
    val root1 =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            dynamicTag = tag1,
            style = Style.width(100.px).height(100.px))

    val test = lithoViewRule.render { root1 }

    test.findViewWithTag("0")

    tag1.set("1")

    test.findViewWithTag("1")

    val root2 =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            dynamicTag = null,
            style = Style.width(100.px).height(100.px))

    test.setRoot(root2)

    assertThat(tag1.numberOfListeners).isEqualTo(0)
  }

  @Test
  fun `when new dynamic value is set and the old one was null it should bind the new dynamic value`() {
    val root1 =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            dynamicTag = null,
            style = Style.width(100.px).height(100.px))

    val test = lithoViewRule.render { root1 }

    val tag = DynamicValue<Any?>("1")
    val root2 =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            dynamicTag = tag,
            style = Style.width(100.px).height(100.px))

    test.setRoot(root2)

    assertThat(tag.numberOfListeners).isEqualTo(1)

    // should have view with new tag
    val view = test.findViewWithTag("1")

    // set new tag using the new dynamic value
    tag.set("2")

    // the above should work, the tag should change
    assertThat(view.tag).isEqualTo("2")

    assertThat(tag.numberOfListeners).isEqualTo(1)
  }

  @Test
  fun `when same dynamic value is used on different components it should update the content for all instances`() {
    val c = lithoViewRule.context
    val tag = DynamicValue<Any?>("0")

    val test =
        lithoViewRule.render {
          Column {
            child(
                TestViewPrimitiveComponent(
                    view = EditText(lithoViewRule.context.androidContext),
                    dynamicTag = tag,
                    style = Style.width(100.px).height(100.px)))
            child(
                TestViewPrimitiveComponent(
                    view = EditText(lithoViewRule.context.androidContext),
                    dynamicTag = tag,
                    style = Style.width(100.px).height(100.px)))
          }
        }

    val lithoView = test.lithoView
    val child0 = lithoView.getChildAt(0)
    val child1 = lithoView.getChildAt(1)

    assertThat(child0.tag).isEqualTo("0")
    assertThat(child1.tag).isEqualTo("0")

    tag.set("1")

    assertThat(child0.tag).isEqualTo("1")
    assertThat(child1.tag).isEqualTo("1")
  }

  @Test
  fun `when same component with dynamic value is used multiple times it should update the content for all instances`() {
    val c = lithoViewRule.context
    val tag = DynamicValue<Any?>("0")
    val component =
        TestViewPrimitiveComponent(
            view = EditText(lithoViewRule.context.androidContext),
            dynamicTag = tag,
            style = Style.width(100.px).height(100.px))

    val test =
        lithoViewRule.render {
          Column {
            child(component)
            child(component)
          }
        }

    val lithoView = test.lithoView
    val child0 = lithoView.getChildAt(0)
    val child1 = lithoView.getChildAt(1)

    assertThat(child0.tag).isEqualTo("0")
    assertThat(child1.tag).isEqualTo("0")

    tag.set("1")

    assertThat(child0.tag).isEqualTo("1")
    assertThat(child1.tag).isEqualTo("1")
  }

  @Test
  fun `Primitive that renders tree host notifies them about visibility bounds changes`() {
    val steps = mutableListOf<LifecycleStep.StepInfo>()

    val component =
        TestVerticalScrollPrimitiveComponent(
            child =
                TestViewPrimitiveComponent(
                    view = TextView(lithoViewRule.context.androidContext),
                    steps = steps,
                    style =
                        Style.width(100.px).height(100.px).margin(top = 50.px).onVisible {
                          steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_EVENT_VISIBLE))
                        }),
            style = Style.width(100.px).height(100.px),
        )

    // Create a FrameLayout 100x100
    val parent = FrameLayout(lithoViewRule.context.androidContext)
    parent.measure(exactly(1080), exactly(100))
    parent.layout(0, 0, 1080, 100)

    // Add a new LithoView to that FrameLayout
    val testView = lithoViewRule.createTestLithoView()
    parent.addView(testView.lithoView)

    // Render the component
    lithoViewRule.render(testView.lithoView) { component }

    // Since everything is in the view port everything should mount
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_EVENT_VISIBLE,
        )
    steps.clear()

    // should unmount if the component is outside of the visible rect due to the 55px offset
    testView.lithoView.offsetTopAndBottom(55) // 55 offset from top; the item is 50 offset from top
    assertThat(LifecycleStep.getSteps(steps)).containsExactly(LifecycleStep.ON_UNMOUNT)
    steps.clear()

    // should not do anything when no items cross over the view port
    testView.lithoView.offsetTopAndBottom(5) // 55 + 5 = 60 offset from top
    assertThat(LifecycleStep.getSteps(steps)).isEmpty()
    steps.clear()

    // should mount the inner item back when it is back in the view port
    testView.lithoView.offsetTopAndBottom(-20) // 60 - 20 = 40 offset from top
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_EVENT_VISIBLE,
        )
  }

  @Test
  fun `Primitive that is excluded from incremental mount is setting this value properly`() {
    val steps = mutableListOf<LifecycleStep.StepInfo>()

    val component =
        TestViewPrimitiveComponent(
            view = TextView(lithoViewRule.context.androidContext),
            shouldExcludeFromIncrementalMount = true,
            steps = steps,
            style =
                Style.width(100.px)
                    .height(100.px)
                    .margin(top = 50.px)
                    .onVisible { steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_EVENT_VISIBLE)) }
                    .onInvisible {
                      steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_EVENT_INVISIBLE))
                    })

    // Create a FrameLayout 100x100
    val parent = FrameLayout(lithoViewRule.context.androidContext)
    parent.measure(exactly(1080), exactly(100))
    parent.layout(0, 0, 1080, 100)

    // Add a new LithoView to that FrameLayout
    val testView = lithoViewRule.createTestLithoView()
    parent.addView(testView.lithoView)
    testView.lithoView
    // Render the component
    lithoViewRule.render(testView.lithoView) { component }

    // Since everything is in the view port everything should mount
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_EVENT_VISIBLE,
        )
    steps.clear()

    // should not unmount if the component is outside of the visible rect due to the 55px offset
    testView.lithoView.offsetTopAndBottom(55) // 55 offset from top; the item is 50 offset from top
    assertThat(LifecycleStep.getSteps(steps)).containsExactly(LifecycleStep.ON_EVENT_INVISIBLE)
    steps.clear()

    // should not do anything when no items cross over the view port
    testView.lithoView.offsetTopAndBottom(5) // 55 + 5 = 60 offset from top
    assertThat(LifecycleStep.getSteps(steps)).isEmpty()
    steps.clear()

    // should trigger visibility event when the component is back in the view port
    testView.lithoView.offsetTopAndBottom(-20) // 60 - 20 = 40 offset from top
    assertThat(LifecycleStep.getSteps(steps)).containsExactly(LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun `Primitive that is not excluded from incremental mount by default is setting this value properly`() {
    val steps = mutableListOf<LifecycleStep.StepInfo>()

    val component =
        TestViewPrimitiveComponent(
            view = TextView(lithoViewRule.context.androidContext),
            steps = steps,
            style =
                Style.width(100.px)
                    .height(100.px)
                    .margin(top = 50.px)
                    .onVisible { steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_EVENT_VISIBLE)) }
                    .onInvisible {
                      steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_EVENT_INVISIBLE))
                    })

    // Create a FrameLayout 100x100
    val parent = FrameLayout(lithoViewRule.context.androidContext)
    parent.measure(exactly(1080), exactly(100))
    parent.layout(0, 0, 1080, 100)

    // Add a new LithoView to that FrameLayout
    val testView = lithoViewRule.createTestLithoView()
    parent.addView(testView.lithoView)
    testView.lithoView
    // Render the component
    lithoViewRule.render(testView.lithoView) { component }

    // Since everything is in the view port everything should mount
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_EVENT_VISIBLE,
        )
    steps.clear()

    // should unmount if the component is outside of the visible rect due to the 55px offset
    testView.lithoView.offsetTopAndBottom(55) // 55 offset from top; the item is 50 offset from top
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_EVENT_INVISIBLE)
    steps.clear()

    // should not do anything when no items cross over the view port
    testView.lithoView.offsetTopAndBottom(5) // 55 + 5 = 60 offset from top
    assertThat(LifecycleStep.getSteps(steps)).isEmpty()
    steps.clear()

    // should mount back and trigger visibility event when the component is back in the view port
    testView.lithoView.offsetTopAndBottom(-20) // 60 - 20 = 40 offset from top
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(LifecycleStep.ON_MOUNT, LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun `Primitive that sets that it's not excluded from incremental mount is setting this value properly`() {
    val steps = mutableListOf<LifecycleStep.StepInfo>()

    val component =
        TestViewPrimitiveComponent(
            view = TextView(lithoViewRule.context.androidContext),
            shouldExcludeFromIncrementalMount = false,
            steps = steps,
            style =
                Style.width(100.px)
                    .height(100.px)
                    .margin(top = 50.px)
                    .onVisible { steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_EVENT_VISIBLE)) }
                    .onInvisible {
                      steps.add(LifecycleStep.StepInfo(LifecycleStep.ON_EVENT_INVISIBLE))
                    })

    // Create a FrameLayout 100x100
    val parent = FrameLayout(lithoViewRule.context.androidContext)
    parent.measure(exactly(1080), exactly(100))
    parent.layout(0, 0, 1080, 100)

    // Add a new LithoView to that FrameLayout
    val testView = lithoViewRule.createTestLithoView()
    parent.addView(testView.lithoView)
    testView.lithoView
    // Render the component
    lithoViewRule.render(testView.lithoView) { component }

    // Since everything is in the view port everything should mount
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(
            LifecycleStep.RENDER,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_EVENT_VISIBLE,
        )
    steps.clear()

    // should unmount if the component is outside of the visible rect due to the 55px offset
    testView.lithoView.offsetTopAndBottom(55) // 55 offset from top; the item is 50 offset from top
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_EVENT_INVISIBLE)
    steps.clear()

    // should not do anything when no items cross over the view port
    testView.lithoView.offsetTopAndBottom(5) // 55 + 5 = 60 offset from top
    assertThat(LifecycleStep.getSteps(steps)).isEmpty()
    steps.clear()

    // should mount back and trigger visibility event when the component is back in the view port
    testView.lithoView.offsetTopAndBottom(-20) // 60 - 20 = 40 offset from top
    assertThat(LifecycleStep.getSteps(steps))
        .containsExactly(LifecycleStep.ON_MOUNT, LifecycleStep.ON_EVENT_VISIBLE)
  }

  @Test
  fun `Primitive component's RenderUnit should have a correct description`() {
    var defaultRenderUnitDescription: String = ""
    val componentNoDescription =
        object : PrimitiveComponent() {
          override fun PrimitiveComponentScope.render(): LithoPrimitive {
            val primitive =
                Primitive(
                    layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
                    MountBehavior(ViewAllocator { context -> TextView(context) }) {})

            defaultRenderUnitDescription = primitive.renderUnit.description

            return LithoPrimitive(primitive, null)
          }
        }
    var testView = lithoViewRule.render { Column { child(componentNoDescription) } }

    assertThat(defaultRenderUnitDescription)
        .contains("com.facebook.rendercore.primitives.MountBehavior")

    testView.lithoView.unmountAllItems()

    var customRenderUnitDescription: String = ""
    val customDescription = "RenderUnit description"
    val componentWithDescription =
        object : PrimitiveComponent() {
          override fun PrimitiveComponentScope.render(): LithoPrimitive {
            val primitive =
                Primitive(
                    layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
                    MountBehavior(
                        customDescription, ViewAllocator { context -> TextView(context) }) {})

            customRenderUnitDescription = primitive.renderUnit.description

            return LithoPrimitive(primitive, null)
          }
        }

    testView = lithoViewRule.render { Column { child(componentWithDescription) } }

    assertThat(customRenderUnitDescription).isEqualTo(customDescription)

    testView.lithoView.unmountAllItems()
  }

  @Test
  fun `Primitive remounting with the same props should preserve 'unbind' lambdas returned from original 'bind' calls`() {
    val bindInfo = mutableListOf<String>()
    val lv = LithoView(lithoViewRule.context)
    // mount component
    var testView =
        lithoViewRule.render(lithoView = lv) {
          TestPrimitiveWithTwoBindersComponent(text = "foo", contentDescription = "bar", bindInfo)
        }
    // remount with the same props
    testView =
        lithoViewRule.render(lithoView = lv) {
          TestPrimitiveWithTwoBindersComponent(text = "foo", contentDescription = "bar", bindInfo)
        }
    // unmount component
    testView.lithoView.unmountAllItems()
    assertThat(bindInfo)
        .containsExactly(
            "BIND:text", "BIND:contentDescription", "UNBIND:contentDescription", "UNBIND:text")
  }

  @Test
  fun `Primitive binding and unbinding two binders individually should preserve 'unbind' lambdas returned from original 'bind' calls`() {
    val bindInfo = mutableListOf<String>()
    val lv = LithoView(lithoViewRule.context)
    // mount component
    var testView =
        lithoViewRule.render(lithoView = lv) {
          TestPrimitiveWithTwoBindersComponent(text = "foo", contentDescription = "bar", bindInfo)
        }
    assertThat(bindInfo).containsExactly("BIND:text", "BIND:contentDescription")

    // remount updating only first binder
    bindInfo.clear()
    testView =
        lithoViewRule.render(lithoView = lv) {
          TestPrimitiveWithTwoBindersComponent(text = "baz", contentDescription = "bar", bindInfo)
        }
    assertThat(bindInfo).containsExactly("UNBIND:text", "BIND:text")

    // remount updating only second binder
    bindInfo.clear()
    testView =
        lithoViewRule.render(lithoView = lv) {
          TestPrimitiveWithTwoBindersComponent(text = "baz", contentDescription = "qux", bindInfo)
        }
    assertThat(bindInfo).containsExactly("UNBIND:contentDescription", "BIND:contentDescription")

    // unmount component
    bindInfo.clear()
    testView.lithoView.unmountAllItems()
    assertThat(bindInfo).containsExactly("UNBIND:contentDescription", "UNBIND:text")
  }

  @Test
  fun `bindWithLayoutData with no deps should work the same as with Unit dep`() {
    val unitDepLayoutData = mutableListOf<TestPrimitiveLayoutData>()
    val noDepLayoutData = mutableListOf<TestPrimitiveLayoutData>()

    class TestComponent(private val style: Style) : PrimitiveComponent() {
      override fun PrimitiveComponentScope.render(): LithoPrimitive {
        return LithoPrimitive(
            layoutBehavior =
                object : LayoutBehavior {
                  override fun LayoutScope.layout(
                      sizeConstraints: SizeConstraints
                  ): PrimitiveLayoutResult {
                    val layoutData =
                        TestPrimitiveLayoutData(
                            width = sizeConstraints.maxWidth,
                            height = sizeConstraints.maxHeight,
                        )
                    return PrimitiveLayoutResult(
                        width = layoutData.width,
                        height = layoutData.height,
                        layoutData = layoutData)
                  }
                },
            mountBehavior =
                MountBehavior(ViewAllocator { context -> View(context) }) {
                  bindWithLayoutData<TestPrimitiveLayoutData>(Unit) { _, layoutData ->
                    unitDepLayoutData.add(layoutData)
                    onUnbind {}
                  }

                  bindWithLayoutData<TestPrimitiveLayoutData> { _, layoutData ->
                    noDepLayoutData.add(layoutData)
                    onUnbind {}
                  }
                },
            style = style)
      }
    }

    // initial render
    val testView =
        lithoViewRule.render { TestComponent(style = Style.width(100.px).height(100.px)) }

    assertThat(unitDepLayoutData).containsExactly(TestPrimitiveLayoutData(100, 100))
    assertThat(noDepLayoutData).isEqualTo(unitDepLayoutData)

    // re-render with the same size
    lithoViewRule.render(lithoView = testView.lithoView) {
      TestComponent(style = Style.width(100.px).height(100.px))
    }

    assertThat(unitDepLayoutData).containsExactly(TestPrimitiveLayoutData(100, 100))
    assertThat(noDepLayoutData).isEqualTo(unitDepLayoutData)

    // re-render with different size
    lithoViewRule.render(lithoView = testView.lithoView) {
      TestComponent(style = Style.width(200.px).height(200.px))
    }

    assertThat(unitDepLayoutData)
        .containsExactly(TestPrimitiveLayoutData(100, 100), TestPrimitiveLayoutData(200, 200))
    assertThat(noDepLayoutData).isEqualTo(unitDepLayoutData)

    testView.lithoView.unmountAllItems()
  }
}

class TestViewPrimitiveComponent(
    val view: View,
    val steps: MutableList<LifecycleStep.StepInfo>? = null,
    val identity: Int = 0,
    val dynamicTag: DynamicValue<Any?>? = null,
    val shouldExcludeFromIncrementalMount: Boolean? = null,
    val style: Style? = null
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {

    steps?.add(LifecycleStep.StepInfo(LifecycleStep.RENDER))

    shouldExcludeFromIncrementalMount =
        this@TestViewPrimitiveComponent.shouldExcludeFromIncrementalMount == true

    return LithoPrimitive(ViewPrimitive(identity, view, steps, dynamicTag), style)
  }
}

@Suppress("TestFunctionName")
private fun PrimitiveComponentScope.ViewPrimitive(
    id: Int = 0,
    view: View,
    steps: MutableList<LifecycleStep.StepInfo>? = null,
    dynamicTag: DynamicValue<Any?>? = null,
    updateState: ((String) -> Unit)? = null,
    str: String? = null,
): Primitive {

  class ViewPrimitiveLayoutBehavior(private val id: Int = 0) : LayoutBehavior {
    override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
      steps?.add(LifecycleStep.StepInfo(LifecycleStep.ON_MEASURE))
      val size = Size.fillSpace(sizeConstraints, 100, 100)
      return PrimitiveLayoutResult(
          size = size, layoutData = TestPrimitiveLayoutData(size.width, size.height))
    }
  }

  return Primitive(
      layoutBehavior = ViewPrimitiveLayoutBehavior(id),
      mountBehavior =
          MountBehavior(
              ViewAllocator {
                updateState?.invoke("createContent")
                steps?.add(LifecycleStep.StepInfo(LifecycleStep.ON_CREATE_MOUNT_CONTENT))
                view
              }) {
                // using tag for convenience of tests
                bindDynamic(dynamicTag, View::setTag, "default_value")

                bindWithLayoutData<TestPrimitiveLayoutData>(id, steps, updateState, str) { _, _ ->
                  updateState?.invoke("mount")
                  steps?.add(LifecycleStep.StepInfo(LifecycleStep.ON_MOUNT))

                  onUnbind { steps?.add(LifecycleStep.StepInfo(LifecycleStep.ON_UNMOUNT)) }
                }
              })
}

class TestDrawablePrimitiveComponent(val drawable: Drawable, val style: Style? = null) :
    PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior =
            object : LayoutBehavior {
              override fun LayoutScope.layout(
                  sizeConstraints: SizeConstraints
              ): PrimitiveLayoutResult {
                val width =
                    if (sizeConstraints.hasExactWidth) {
                      sizeConstraints.maxWidth
                    } else {
                      100
                    }

                val height =
                    if (sizeConstraints.hasExactHeight) {
                      sizeConstraints.maxHeight
                    } else {
                      100
                    }

                val result = MeasureResult(width, height, TestPrimitiveLayoutData(width, height))
                return PrimitiveLayoutResult(
                    result.width, result.height, layoutData = result.layoutData)
              }
            },
        mountBehavior =
            MountBehavior(DrawableAllocator { drawable }) {
              bindWithLayoutData<TestPrimitiveLayoutData>(Unit) { drawable, testPrimitiveLayoutData
                ->
                onUnbind {}
              }
            },
        Style.accessibilityRole(AccessibilityRole.IMAGE) + style)
  }
}

class TestVerticalScrollPrimitiveComponent(
    val child: Component,
    val style: Style? = null,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {

    val componentTree = useState { ComponentTree.createNestedComponentTree(context, child).build() }

    return LithoPrimitive(
        layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> LithoScrollView(context) }) {
              doesMountRenderTreeHosts = true

              bind(componentTree.value) { scrollView ->
                scrollView.isVerticalScrollBarEnabled = true
                scrollView.mount(
                    componentTree.value,
                    LithoScrollView.ScrollPosition(0),
                    null,
                )
                onUnbind {}
              }
            },
        style)
  }
}

class TestPrimitiveWithTwoBindersComponent(
    private val text: String,
    private val contentDescription: String,
    private val bindInfo: MutableList<String>
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> TextView(context) }) {
              bind(text) { content ->
                bindInfo.add("BIND:text")
                content.text = text
                onUnbind {
                  bindInfo.add("UNBIND:text")
                  content.text = null
                }
              }
              bindWithLayoutData<Any?>(contentDescription) { content, _ ->
                bindInfo.add("BIND:contentDescription")
                content.contentDescription = contentDescription
                onUnbind {
                  bindInfo.add("UNBIND:contentDescription")
                  content.contentDescription = null
                }
              }
            },
        style = null)
  }
}

data class TestPrimitiveLayoutData(val width: Int, val height: Int)
