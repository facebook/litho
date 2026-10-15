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
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.accessibility.accessibilityHeading
import com.facebook.litho.accessibility.containerTitle
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.accessibility.focusOrder
import com.facebook.litho.accessibility.labeledBy
import com.facebook.litho.accessibility.stateDescription
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.accessibilityProbe
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAccessibilityManager

/** Accessibility relations and container semantics that a service reads off a host's node. */
@RunWith(LithoTestRunner::class)
class AccessibilityRelationsTest {

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
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @After
  fun tearDown() {
    shadowManager.setEnabled(false)
    shadowManager.setTouchExplorationEnabled(false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  private fun labelled(label: String): (AccessibilityNodeInfo) -> Boolean = {
    it.contentDescription == label
  }

  @Test
  fun `focus order reaches a card whose own content is not accessible`() {
    // A clickable card holding only a Text: the Text is hosted by its own View, so none of the
    // card host's direct mount items is accessible. The card's own node info must still apply.
    val probe = lithoTestRule.render { TwoCards() }.accessibilityProbe()

    // Nodes compare by source, so this names the second card's View whatever fields the
    // traversal target was populated with.
    val before =
        AccessibilityNodeInfoCompat.wrap(probe.require(labelled(FIRST)).info()).traversalBefore
    assertThat(before)
        .isEqualTo(AccessibilityNodeInfoCompat.wrap(probe.require(labelled(SECOND)).info()))
  }

  @Test
  fun `a heading set on a container reaches a node`() {
    val probe =
        lithoTestRule
            .render {
              // Nested: the root always has a host, so its props reach a View regardless.
              Column {
                child(
                    Column(style = Style.accessibilityHeading(true)) {
                      child(Text(text = "Photos"))
                    },
                )
              }
            }
            .accessibilityProbe()

    assertThat(probe.find { AccessibilityNodeInfoCompat.wrap(it).isHeading }).isNotNull
  }

  @Test
  fun `a container title and state description set on a container reach a node`() {
    val probe =
        lithoTestRule
            .render {
              Column {
                child(
                    Column(style = Style.containerTitle(TITLE).stateDescription(STATE)) {
                      child(Text(text = "a"))
                    },
                )
              }
            }
            .accessibilityProbe()

    // Compat reads the title from extras below API 34, where the delegate also writes it.
    assertThat(probe.find { AccessibilityNodeInfoCompat.wrap(it).containerTitle == TITLE })
        .isNotNull
    assertThat(probe.find { AccessibilityNodeInfoCompat.wrap(it).stateDescription == STATE })
        .isNotNull
  }

  @Test
  fun `focus order reaches a clickable host with no content at all`() {
    // A clickable Row with no children mounts no item of its own, so there is no mount item to take
    // a context from; the relation is looked up from the tree's root regardless.
    val probe = lithoTestRule.render { EmptyThenCard() }.accessibilityProbe()

    val before =
        AccessibilityNodeInfoCompat.wrap(probe.require(labelled(FIRST)).info()).traversalBefore
    assertThat(before)
        .isEqualTo(AccessibilityNodeInfoCompat.wrap(probe.require(labelled(SECOND)).info()))
  }

  @Test
  fun `focus order still applies where the host has an accessible mount item`() {
    // Two Texts that carry the focus order themselves: each is the accessible item of its own host,
    // the case that worked before and must keep working.
    val probe = lithoTestRule.render { TwoTexts() }.accessibilityProbe()

    val before =
        AccessibilityNodeInfoCompat.wrap(probe.require(labelled(FIRST)).info()).traversalBefore
    assertThat(before)
        .isEqualTo(AccessibilityNodeInfoCompat.wrap(probe.require(labelled(SECOND)).info()))
  }

  @Test
  fun `labeled-by reaches a card whose own content is not accessible`() {
    val testView = lithoTestRule.render {
      Column {
        child(Text(text = "Name", style = Style.viewTag(LABEL_TAG)))
        child(
            Row(
                style =
                    Style.width(SIZE.px)
                        .height(SIZE.px)
                        .contentDescription(FIRST)
                        .onClick {}
                        .labeledBy(LABEL_TAG),
            ) {
              child(Text(text = "a"))
            },
        )
      }
    }
    val label = testView.lithoView.findViewWithTag<View>(LABEL_TAG)
    val card = checkNotNull(findView(testView.lithoView) { it.contentDescription == FIRST })

    // Robolectric does not record setLabeledBy(View), so the call itself is what is checked.
    val info = spy(AccessibilityNodeInfo.obtain())
    card.onInitializeAccessibilityNodeInfo(info)

    verify(info).setLabeledBy(label)
  }

  private fun findView(root: View, matching: (View) -> Boolean): View? {
    if (matching(root)) return root
    if (root is ViewGroup) {
      for (i in 0 until root.childCount) {
        findView(root.getChildAt(i), matching)?.let {
          return it
        }
      }
    }
    return null
  }

  private class EmptyThenCard : KComponent() {
    override fun ComponentScope.render(): Component {
      val (first, second) = useFocusOrder()
      return Column {
        child(
            Row(
                style =
                    Style.width(SIZE.px)
                        .height(SIZE.px)
                        .contentDescription(FIRST)
                        .onClick {}
                        .focusOrder(first),
            ),
        )
        child(
            Row(
                style =
                    Style.width(SIZE.px)
                        .height(SIZE.px)
                        .contentDescription(SECOND)
                        .onClick {}
                        .focusOrder(second),
            ) {
              child(Text(text = "b"))
            },
        )
      }
    }
  }

  private class TwoTexts : KComponent() {
    override fun ComponentScope.render(): Component {
      val (first, second) = useFocusOrder()
      return Column {
        child(Text(text = "a", style = Style.contentDescription(FIRST).focusOrder(first)))
        child(Text(text = "b", style = Style.contentDescription(SECOND).focusOrder(second)))
      }
    }
  }

  @Test
  fun `a heading set to false or an empty state description adds no host`() {
    // Design-system props set heading on nearly every component, false for almost all of them. Only
    // what a service would read may create a host.
    val plain = hostCount { Column(style = Style.width(SIZE.px).height(SIZE.px)) }
    val notAHeading = hostCount {
      Column(style = Style.width(SIZE.px).height(SIZE.px).accessibilityHeading(false))
    }
    val emptyState = hostCount {
      Column(style = Style.width(SIZE.px).height(SIZE.px).stateDescription(""))
    }

    assertThat(notAHeading).describedAs("heading set to false").isEqualTo(plain)
    assertThat(emptyState).describedAs("empty state description").isEqualTo(plain)
  }

  @Test
  fun `with no accessibility service running a heading adds no host`() {
    shadowManager.setEnabled(false)
    shadowManager.setTouchExplorationEnabled(false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()

    val plain = hostCount { Column(style = Style.width(SIZE.px).height(SIZE.px)) }
    val heading = hostCount {
      Column(style = Style.width(SIZE.px).height(SIZE.px).accessibilityHeading(true))
    }

    assertThat(heading).isEqualTo(plain)
  }

  /** Hosts under the rendered tree when [child] is a container nested below the root. */
  private fun hostCount(child: ResourcesScope.() -> Component): Int {
    val testView = lithoTestRule.render { Column { child(child()) } }
    fun count(view: View): Int =
        (if (view is ComponentHost) 1 else 0) +
            if (view is ViewGroup) (0 until view.childCount).sumOf { count(view.getChildAt(it)) }
            else 0
    return count(testView.lithoView)
  }

  @Test
  fun `turning a heading off on a mounted component reaches its node`() {
    val heading = java.util.concurrent.atomic.AtomicReference<State<Boolean>?>()

    class Section : KComponent() {
      override fun ComponentScope.render(): Component {
        val isHeading = useState { true }.also { heading.set(it) }
        return Row(
            style =
                Style.width(SIZE.px)
                    .height(SIZE.px)
                    .contentDescription(FIRST)
                    .onClick {}
                    .accessibilityHeading(isHeading.value),
        )
      }
    }

    val probe = lithoTestRule.render { Column { child(Section()) } }.accessibilityProbe()
    assertThat(AccessibilityNodeInfoCompat.wrap(probe.require(labelled(FIRST)).info()).isHeading)
        .describedAs("a heading at first")
        .isTrue()

    checkNotNull(heading.get()).updateSync(false)
    lithoTestRule.idle()

    // Equivalence ignored the heading, so the host kept the node info it was first bound with.
    assertThat(AccessibilityNodeInfoCompat.wrap(probe.require(labelled(FIRST)).info()).isHeading)
        .describedAs("no longer a heading")
        .isFalse()
  }

  private class TwoCards : KComponent() {
    override fun ComponentScope.render(): Component {
      val (first, second) = useFocusOrder()
      return Column {
        child(card(FIRST, "a", Style.focusOrder(first)))
        child(card(SECOND, "b", Style.focusOrder(second)))
      }
    }

    private fun ResourcesScope.card(label: String, text: String, extra: Style): Component = Row(
        style = Style.width(SIZE.px).height(SIZE.px).contentDescription(label).onClick {} + extra,
    ) {
      child(Text(text = text))
    }
  }

  private companion object {
    const val SIZE = 100
    const val FIRST = "first"
    const val SECOND = "second"
    const val TITLE = "Photos"
    const val STATE = "expanded"
    const val LABEL_TAG = "label"
  }
}
