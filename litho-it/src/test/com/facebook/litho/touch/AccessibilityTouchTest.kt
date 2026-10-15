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

package com.facebook.litho.touch

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.facebook.litho.AccessibilityRole
import com.facebook.litho.AccessibilityUtils
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.accessibility.ImportantForAccessibility
import com.facebook.litho.accessibility.accessibilityRole
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.accessibility.importantForAccessibility
import com.facebook.litho.child
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.AccessibilityProbe
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.accessibilityProbe
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.useState
import com.facebook.litho.view.onClick
import com.facebook.litho.view.onLongClick
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAccessibilityManager

/**
 * Characterizes the coupling between touch handlers and accessibility.
 *
 * Accessibility is a second input path that never produces a `MotionEvent`: a screen reader calls
 * `performAccessibilityAction`. Both paths converge on the same click listener, and on the
 * `isClickable` flag that installing it sets, so a change to touch dispatch can move screen-reader
 * behavior with no gesture test noticing.
 *
 * Targets are resolved through [AccessibilityProbe], which reads nodes the way a service does.
 * Nothing here reaches for a `ComponentHost`: where a node comes from — a mounted View today, a
 * virtual node once components stop mounting one — is the implementation detail these tests exist
 * to be insulated from.
 */
@RunWith(LithoTestRunner::class)
class AccessibilityTouchTest {

  @get:Rule val lithoTestRule = LithoTestRule(attachToRealWindow = true)

  private lateinit var shadowManager: ShadowAccessibilityManager

  @Before
  fun setUp() {
    val manager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    shadowManager = Shadows.shadowOf(manager)
    shadowManager.setEnabled(true)
    shadowManager.setTouchExplorationEnabled(true)
    // The result is memoised, so flipping the shadow alone has no effect.
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @After
  fun tearDown() {
    shadowManager.setEnabled(false)
    shadowManager.setTouchExplorationEnabled(false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
    InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
  }

  private fun probe(component: ComponentScope.() -> Component): AccessibilityProbe =
      lithoTestRule.render { component() }.accessibilityProbe()

  private fun labelled(label: String): (AccessibilityNodeInfo) -> Boolean = {
    it.contentDescription == label
  }

  @Test
  fun `a click handler is announced as clickable`() {
    val probe = probe {
      Column {
        child(
            Column(
                style = Style.width(SIZE.px).height(SIZE.px).contentDescription(LABEL).onClick {},
            ),
        )
      }
    }

    assertThat(probe.require(labelled(LABEL)).info().isClickable).isTrue()
  }

  @Test
  fun `a component with no click handler is not announced as clickable`() {
    val probe = probe {
      Column {
        child(Column(style = Style.width(SIZE.px).height(SIZE.px).contentDescription(LABEL)))
      }
    }

    assertThat(probe.require(labelled(LABEL)).info().isClickable).isFalse()
  }

  @Test
  fun `a long click handler is announced as long clickable`() {
    val probe = probe {
      Column {
        child(
            Column(
                style =
                    Style.width(SIZE.px).height(SIZE.px).contentDescription(LABEL).onLongClick {
                      true
                    },
            ),
        )
      }
    }

    assertThat(probe.require(labelled(LABEL)).info().isLongClickable).isTrue()
  }

  @Test
  fun `the click action is offered on the node`() {
    val probe = probe {
      Column {
        child(
            Column(
                style = Style.width(SIZE.px).height(SIZE.px).contentDescription(LABEL).onClick {},
            ),
        )
      }
    }

    assertThat(probe.require(labelled(LABEL)).info().actionList.map { it.id })
        .contains(AccessibilityNodeInfo.ACTION_CLICK)
  }

  /**
   * A screen reader's tap. It produces no MotionEvent, so no gesture test covers this route to the
   * same handler.
   */
  @Test
  fun `performing the click action reaches the click handler`() {
    var clicks = 0
    val probe = probe {
      Column {
        child(
            Column(
                style =
                    Style.width(SIZE.px).height(SIZE.px).contentDescription(LABEL).onClick {
                      clicks++
                    },
            ),
        )
      }
    }

    val handled = probe.require(labelled(LABEL)).performAction(AccessibilityNodeInfo.ACTION_CLICK)

    assertThat(handled).isTrue()
    assertThat(clicks)
        .describedAs("a screen-reader tap reaches the Litho click handler")
        .isEqualTo(1)
  }

  @Test
  fun `performing the long click action reaches the long click handler`() {
    var longClicks = 0
    val probe = probe {
      Column {
        child(
            Column(
                style =
                    Style.width(SIZE.px).height(SIZE.px).contentDescription(LABEL).onLongClick {
                      longClicks++
                      true
                    },
            ),
        )
      }
    }

    val handled =
        probe.require(labelled(LABEL)).performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)

    assertThat(handled).isTrue()
    assertThat(longClicks).isEqualTo(1)
  }

  /** Without a handler there is nothing to perform, and the action is refused. */
  @Test
  fun `the click action is refused when there is no click handler`() {
    val probe = probe {
      Column {
        child(Column(style = Style.width(SIZE.px).height(SIZE.px).contentDescription(LABEL)))
      }
    }

    val handled = probe.require(labelled(LABEL)).performAction(AccessibilityNodeInfo.ACTION_CLICK)

    assertThat(handled).isFalse()
  }

  /** The role is announced as the node's className, which is how a screen reader states a type. */
  @Test
  fun `the role is announced as the node class name`() {
    val probe = probe {
      Column {
        child(
            Column(
                style =
                    Style.width(SIZE.px)
                        .height(SIZE.px)
                        .contentDescription(LABEL)
                        .accessibilityRole(AccessibilityRole.BUTTON)
                        .onClick {},
            ),
        )
      }
    }

    assertThat(probe.require(labelled(LABEL)).info().className).isEqualTo(AccessibilityRole.BUTTON)
  }

  @Test
  fun `importantForAccessibility NO removes the component from the tree a service walks`() {
    val probe = probe {
      Column {
        child(
            Column(
                style =
                    Style.width(SIZE.px)
                        .height(SIZE.px)
                        .contentDescription(LABEL)
                        .importantForAccessibility(ImportantForAccessibility.NO)
                        .onClick {},
            ),
        )
      }
    }

    assertThat(probe.find(labelled(LABEL)))
        .describedAs("a service reaches nothing announcing the label")
        .isNull()
  }

  /** NO hides the component itself. Its descendants are a separate question, answered below. */
  @Test
  fun `importantForAccessibility NO on a parent leaves its child reachable`() {
    val probe = probe {
      Column {
        child(
            Column(
                style =
                    Style.width(SIZE.px)
                        .height(SIZE.px)
                        .contentDescription(PARENT)
                        .importantForAccessibility(ImportantForAccessibility.NO)
                        .onClick {},
            ) {
              child(
                  Column(
                      style =
                          Style.width(HALF.px).height(HALF.px).contentDescription(CHILD).onClick {},
                  ),
              )
            },
        )
      }
    }

    assertThat(probe.find(labelled(PARENT))).isNull()
    assertThat(probe.find(labelled(CHILD)))
        .describedAs("hiding a parent does not hide what is inside it")
        .isNotNull()
  }

  @Test
  fun `importantForAccessibility NO_HIDE_DESCENDANTS on a parent removes its child too`() {
    val probe = probe {
      Column {
        child(
            Column(
                style =
                    Style.width(SIZE.px)
                        .height(SIZE.px)
                        .contentDescription(PARENT)
                        .importantForAccessibility(ImportantForAccessibility.NO_HIDE_DESCENDANTS)
                        .onClick {},
            ) {
              child(
                  Column(
                      style =
                          Style.width(HALF.px).height(HALF.px).contentDescription(CHILD).onClick {},
                  ),
              )
            },
        )
      }
    }

    assertThat(probe.find(labelled(PARENT))).isNull()
    assertThat(probe.find(labelled(CHILD))).isNull()
  }

  @Test
  fun `a clickable child inside a clickable parent is announced as its own node`() {
    val probe = probe {
      Column {
        child(
            Column(
                style = Style.width(SIZE.px).height(SIZE.px).contentDescription(PARENT).onClick {},
            ) {
              child(
                  Column(
                      style =
                          Style.width(HALF.px).height(HALF.px).contentDescription(CHILD).onClick {},
                  ),
              )
            },
        )
      }
    }

    assertThat(probe.require(labelled(PARENT)).info().isClickable).isTrue()
    assertThat(probe.require(labelled(CHILD)).info().isClickable).isTrue()
  }

  /**
   * Nesting is where the two input paths diverge most: a gesture is claimed by one target, but a
   * service addresses a node directly and the enclosing node must not also fire.
   */
  @Test
  fun `activating a child does not activate its clickable parent`() {
    var parentClicks = 0
    var childClicks = 0
    val probe = probe {
      Column {
        child(
            Column(
                style =
                    Style.width(SIZE.px).height(SIZE.px).contentDescription(PARENT).onClick {
                      parentClicks++
                    },
            ) {
              child(
                  Column(
                      style =
                          Style.width(HALF.px).height(HALF.px).contentDescription(CHILD).onClick {
                            childClicks++
                          },
                  ),
              )
            },
        )
      }
    }

    probe.require(labelled(CHILD)).performAction(AccessibilityNodeInfo.ACTION_CLICK)

    assertThat(childClicks).isEqualTo(1)
    assertThat(parentClicks).describedAs("the enclosing node is not activated too").isEqualTo(0)
  }

  private class Toggling : KComponent() {
    override fun ComponentScope.render(): Component {
      val expanded = useState { false }
      return Column {
        child(
            Column(
                style =
                    Style.width(SIZE.px)
                        .height(SIZE.px)
                        .contentDescription(if (expanded.value) COLLAPSE else EXPAND)
                        .onClick { expanded.update(true) },
            ),
        )
      }
    }
  }

  /**
   * A service reads the tree again after every change, so what it reads has to follow a re-render —
   * including one the service triggered itself.
   */
  @Test
  fun `the tree a service reads follows a state update`() {
    val testLithoView = lithoTestRule.render { Toggling() }
    val probe = testLithoView.accessibilityProbe()

    probe.require(labelled(EXPAND)).performAction(AccessibilityNodeInfo.ACTION_CLICK)
    lithoTestRule.idle()

    assertThat(probe.find(labelled(EXPAND))).describedAs("the old label is gone").isNull()
    assertThat(probe.find(labelled(COLLAPSE))).describedAs("the new label is read").isNotNull()
  }

  /** A KComponent implements no extra a11y nodes, so it contributes nothing virtual. */
  @Test
  fun `a KComponent contributes no virtual nodes`() {
    val probe = probe {
      Column {
        child(
            Column(
                style = Style.width(SIZE.px).height(SIZE.px).contentDescription(LABEL).onClick {},
            ),
        )
      }
    }

    assertThat(probe.nodes().filter { it.isVirtual }).isEmpty()
  }

  private fun spannedWithClick(onSpanClick: () -> Unit): SpannableString =
      SpannableString("tap here").apply {
        setSpan(
            object : ClickableSpan() {
              override fun onClick(widget: View) = onSpanClick()
            },
            0,
            3,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }

  /**
   * `Text` with `accessibleClickableSpans` is the production path that does implement extra nodes,
   * letting a screen reader reach each span. It is the existing mechanism for exposing content that
   * has no View of its own, and so the closest analogue to what a root dispatcher would have to
   * reproduce for every component.
   */
  @Test
  fun `each clickable span is reachable as a virtual node`() {
    val probe = probe {
      Column {
        child(
            Text(
                text = spannedWithClick {},
                accessibleClickableSpans = true,
                style = Style.width(SIZE.px).height(SIZE.px),
            ),
        )
      }
    }

    assertThat(probe.nodes().filter { it.isVirtual })
        .describedAs("one virtual node per clickable span")
        .hasSize(1)
  }

  /**
   * `ACTION_ACCESSIBILITY_FOCUS` is handled by `ExploreByTouchHelper` itself, so it lands on a span
   * without Litho's own gate below. It is the control for that test: virtual-node actions do reach
   * the provider.
   */
  @Test
  fun `a span's virtual node accepts accessibility focus`() {
    val probe = probe {
      Column {
        child(
            Text(
                text = spannedWithClick {},
                accessibleClickableSpans = true,
                style = Style.width(SIZE.px).height(SIZE.px),
            ),
        )
      }
    }
    val span = probe.nodes().first { it.isVirtual }

    assertThat(span.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)).isTrue()
  }

  /**
   * Recording a defect, not endorsing it. `ComponentAccessibilityDelegate` refuses a virtual-node
   * action unless `findFocus(FOCUS_INPUT)` returns a node, which needs the host View focused — and
   * nothing in the Litho API makes a `Text` host focusable. So a screen reader can read a clickable
   * span but not activate it. Pinned because a dispatcher change flipping this to true is a
   * behavior change that wants a deliberate decision, welcome as the fix would be.
   */
  @Test
  fun `activating a span's virtual node is refused, because nothing focuses its host`() {
    InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
    var spanClicks = 0
    val probe = probe {
      Column {
        child(
            Text(
                text = spannedWithClick { spanClicks++ },
                accessibleClickableSpans = true,
                style = Style.width(SIZE.px).height(SIZE.px),
            ),
        )
      }
    }
    val span = probe.nodes().first { it.isVirtual }

    assertThat(span.requestKeyboardFocus())
        .describedAs("the host is not focusable, so focus cannot be moved to it")
        .isFalse()
    assertThat(span.performAction(AccessibilityNodeInfo.ACTION_CLICK)).isFalse()
    assertThat(spanClicks).isZero()
  }

  companion object {
    private const val SIZE = 100
    private const val HALF = SIZE / 2
    private const val LABEL = "target"
    private const val PARENT = "parent"
    private const val CHILD = "child"
    private const val EXPAND = "Expand"
    private const val COLLAPSE = "Collapse"
  }
}
