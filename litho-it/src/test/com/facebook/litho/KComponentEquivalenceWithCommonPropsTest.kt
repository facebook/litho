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

import android.annotation.SuppressLint
import com.facebook.litho.core.heightPercent
import com.facebook.litho.core.widthPercent
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.flexbox.flex
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.alpha
import com.facebook.litho.view.onClick
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.visibility.VisibleEvent
import com.facebook.yoga.YogaAlign
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for common props equivalence of KComponent classes. */
@SuppressLint("KComponentJavaApiDetector")
@RunWith(LithoTestRunner::class)
class KComponentEquivalenceWithCommonPropsTest {

  @Rule @JvmField val lithoTestRule = LithoTestRule()

  @Test
  fun kcomponentWithCommonProps_isEquivalentTo_checksAllFields() {
    class ComponentWithStyleProp(val style: Style) : KComponent() {
      override fun ComponentScope.render(): Component =
          Row.create(context).kotlinStyle(style).build()
    }

    val onClick: (ClickEvent) -> Unit = {}

    assertThat(
        ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))
            .isEquivalentTo(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))),
    )
        .isTrue
    assertThat(
        ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))
            .isEquivalentTo(ComponentWithStyleProp(Style.alpha(1f).onClick(action = onClick))),
    )
        .isFalse
    assertThat(
        ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))
            .isEquivalentTo(ComponentWithStyleProp(Style.alpha(.5f).onClick {})),
    )
        .isFalse
  }

  @Test
  fun kcomponentComponentProp_isEquivalentTo_checksAllFields() {
    class ComponentWithComponentProp(val component: Component) : KComponent() {
      override fun ComponentScope.render(): Component = component
    }

    class ComponentWithStyleProp(val style: Style) : KComponent() {
      override fun ComponentScope.render(): Component =
          Row.create(context).kotlinStyle(style).build()
    }

    val onClick: (ClickEvent) -> Unit = {}

    assertThat(
        ComponentWithComponentProp(
            ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)),
        )
            .isEquivalentTo(
                ComponentWithComponentProp(
                    ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)),
                ),
            ),
    )
        .isTrue
    assertThat(
        ComponentWithComponentProp(
            ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)),
        )
            .isEquivalentTo(
                ComponentWithComponentProp(
                    ComponentWithStyleProp(Style.alpha(1f).onClick(action = onClick)),
                ),
            ),
    )
        .isFalse
    assertThat(
        ComponentWithComponentProp(
            ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)),
        )
            .isEquivalentTo(
                ComponentWithComponentProp(ComponentWithStyleProp(Style.alpha(.5f).onClick {})),
            ),
    )
        .isFalse
  }

  @Test
  fun kcomponentInRow_isEquivalentToWithCommonProps_checksAllFields() {
    class ComponentWithStyleProp(val style: Style) : KComponent() {
      override fun ComponentScope.render(): Component =
          Row.create(context).kotlinStyle(style).build()
    }

    val onClick: (ClickEvent) -> Unit = {}

    with(ComponentScope(lithoTestRule.context)) {
      assertThat(
          Row { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
              .isEquivalentTo(
                  Row {
                    child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)))
                  },
              ),
      )
          .isTrue
      assertThat(
          Row { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
              .isEquivalentTo(
                  Row {
                    child(ComponentWithStyleProp(Style.alpha(.1f).onClick(action = onClick)))
                  },
              ),
      )
          .isFalse
      assertThat(
          Row { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
              .isEquivalentTo(
                  Row { child(ComponentWithStyleProp(Style.alpha(.5f).onClick {})) },
              ),
      )
          .isFalse
    }
  }

  @Test
  fun rowWithCommonProps_isEquivalentToWithCommonProps_checksAllFields() {
    val onClick: (ClickEvent) -> Unit = {}

    with(ComponentScope(lithoTestRule.context)) {
      assertThat(
          Row(style = Style.alpha(.5f).onClick(action = onClick)) {}
              .isEquivalentTo(Row(style = Style.alpha(.5f).onClick(action = onClick)) {}),
      )
          .isTrue
      assertThat(
          Row(style = Style.alpha(.5f).onClick(action = onClick)) {}
              .isEquivalentTo(Row(style = Style.alpha(.1f).onClick(action = onClick)) {}),
      )
          .isFalse
      assertThat(
          Row(style = Style.alpha(.5f).onClick(action = onClick)) {}
              .isEquivalentTo(Row(style = Style.alpha(.5f).onClick {}) {}),
      )
          .isFalse
    }
  }

  @Test
  fun rowWithValueStyles_isEquivalentToSameStylesBuiltAgain() {
    val onVisible: (VisibleEvent) -> Unit = {}
    val styles: Map<String, () -> Style> = mapOf(
        "widthPercent" to { Style.widthPercent(100f) },
        "heightPercent" to { Style.heightPercent(50f) },
        "flex" to { Style.flex(grow = 1f, shrink = 0f) },
        "alignSelf" to { Style.alignSelf(YogaAlign.CENTER) },
        "onVisible ratio" to { Style.onVisible(visibleHeightRatio = 0.5f, callback = onVisible) },
    )

    with(ComponentScope(lithoTestRule.context)) {
      SoftAssertions.assertSoftly { softly ->
        styles.forEach { (name, style) ->
          softly
              .assertThat(Row(style = style()) {}.isEquivalentTo(Row(style = style()) {}))
              .describedAs("a Row with $name built twice with the same values")
              .isTrue
        }
      }
      assertThat(
          Row(style = Style.widthPercent(100f)) {}
              .isEquivalentTo(Row(style = Style.widthPercent(50f)) {}),
      )
          .isFalse
    }
  }

  @Test
  fun kcomponentInColumn_isEquivalentToWithCommonProps_checksAllFields() {
    class ComponentWithStyleProp(val style: Style) : KComponent() {
      override fun ComponentScope.render(): Component =
          Row.create(context).kotlinStyle(style).build()
    }

    val onClick: (ClickEvent) -> Unit = {}

    with(ComponentScope(lithoTestRule.context)) {
      assertThat(
          Column { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
              .isEquivalentTo(
                  Column {
                    child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)))
                  },
              ),
      )
          .isTrue
      assertThat(
          Column { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
              .isEquivalentTo(
                  Column {
                    child(ComponentWithStyleProp(Style.alpha(.1f).onClick(action = onClick)))
                  },
              ),
      )
          .isFalse
      assertThat(
          Column { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
              .isEquivalentTo(
                  Column { child(ComponentWithStyleProp(Style.alpha(.5f).onClick {})) },
              ),
      )
          .isFalse
    }
  }

  @Test
  fun columnWithCommonProps_isEquivalentToWithCommonProps_checksAllFields() {
    val onClick: (ClickEvent) -> Unit = {}

    with(ComponentScope(lithoTestRule.context)) {
      assertThat(
          Column(style = Style.alpha(.5f).onClick(action = onClick)) {}
              .isEquivalentTo(Column(style = Style.alpha(.5f).onClick(action = onClick)) {}),
      )
          .isTrue
      assertThat(
          Column(style = Style.alpha(.5f).onClick(action = onClick)) {}
              .isEquivalentTo(Column(style = Style.alpha(.1f).onClick(action = onClick)) {}),
      )
          .isFalse
      assertThat(
          Column(style = Style.alpha(.5f).onClick(action = onClick)) {}
              .isEquivalentTo(Column(style = Style.alpha(.5f).onClick {}) {}),
      )
          .isFalse
    }
  }

  @Test
  fun kcomponentInWrapper_isEquivalentToWithCommonProps_checksAllFields() {
    class ComponentWithStyleProp(val style: Style) : KComponent() {
      override fun ComponentScope.render(): Component =
          Row.create(context).kotlinStyle(style).build()
    }

    val onClick: (ClickEvent) -> Unit = {}

    fun Wrapper(style: Style = Style, delegate: () -> Component?): Component =
        Wrapper.create(lithoTestRule.context).delegate(delegate()).kotlinStyle(style).build()

    assertThat(
        Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)) }
            .isEquivalentTo(
                Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)) },
            ),
    )
        .isTrue
    assertThat(
        Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)) }
            .isEquivalentTo(
                Wrapper { ComponentWithStyleProp(Style.alpha(.1f).onClick(action = onClick)) },
            ),
    )
        .isFalse
    assertThat(
        Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)) }
            .isEquivalentTo(Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick {}) }),
    )
        .isFalse
  }

  @Test
  fun kcomponentWithWrappedCommonProps_isEquivalentTo_checksAllFields() {
    val onClick: (ClickEvent) -> Unit = {}

    fun Wrapper(style: Style = Style, delegate: () -> Component?): Component =
        Wrapper.create(lithoTestRule.context).delegate(delegate()).kotlinStyle(style).build()

    assertThat(
        Wrapper(Style.alpha(.5f).onClick(action = onClick)) { null }
            .isEquivalentTo(Wrapper(Style.alpha(.5f).onClick(action = onClick)) { null }),
    )
        .isTrue
    assertThat(
        Wrapper(Style.alpha(.5f).onClick(action = onClick)) { null }
            .isEquivalentTo(Wrapper(Style.alpha(.1f).onClick(action = onClick)) { null }),
    )
        .isFalse
    assertThat(
        Wrapper(Style.alpha(.5f).onClick(action = onClick)) { null }
            .isEquivalentTo(Wrapper(Style.alpha(.5f).onClick {}) { null }),
    )
        .isFalse
  }
}
