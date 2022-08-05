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

import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.alpha
import com.facebook.litho.view.onClick
import java.lang.Exception
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for common props equivalence of KComponent classes. */
@RunWith(LithoTestRunner::class)
class KComponentEquivalenceWithCommonPropsTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()

  @Before
  @Throws(Exception::class)
  fun setup() {
    TempComponentsConfigurations.setShouldCompareCommonPropsInIsEquivalentTo(true)
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldCompareCommonPropsInIsEquivalentTo()
  }

  @Test
  fun kcomponentWithCommonProps_isEquivalentTo_checksAllFields() {
    class ComponentWithStyleProp(val style: Style) : KComponent() {
      override fun ComponentScope.render(): Component =
          Row.create(context).kotlinStyle(style).build()
    }

    val onClick: (ClickEvent) -> Unit = {}

    assertThat(
            ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))
                .isEquivalentTo(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))))
        .isTrue
    assertThat(
            ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))
                .isEquivalentTo(ComponentWithStyleProp(Style.alpha(1f).onClick(action = onClick))))
        .isFalse
    assertThat(
            ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))
                .isEquivalentTo(ComponentWithStyleProp(Style.alpha(.5f).onClick {})))
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
                    ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)))
                .isEquivalentTo(
                    ComponentWithComponentProp(
                        ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)))))
        .isTrue
    assertThat(
            ComponentWithComponentProp(
                    ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)))
                .isEquivalentTo(
                    ComponentWithComponentProp(
                        ComponentWithStyleProp(Style.alpha(1f).onClick(action = onClick)))))
        .isFalse
    assertThat(
            ComponentWithComponentProp(
                    ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)))
                .isEquivalentTo(
                    ComponentWithComponentProp(
                        ComponentWithStyleProp(Style.alpha(.5f).onClick {}))))
        .isFalse
  }

  @Test
  fun kcomponentInRow_isEquivalentToWithCommonProps_checksAllFields() {
    class ComponentWithStyleProp(val style: Style) : KComponent() {
      override fun ComponentScope.render(): Component =
          Row.create(context).kotlinStyle(style).build()
    }

    val onClick: (ClickEvent) -> Unit = {}

    with(ComponentScope(lithoViewRule.context)) {
      assertThat(
              Row { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
                  .isEquivalentTo(
                      Row {
                        child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)))
                      }))
          .isTrue
      assertThat(
              Row { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
                  .isEquivalentTo(
                      Row {
                        child(ComponentWithStyleProp(Style.alpha(.1f).onClick(action = onClick)))
                      }))
          .isFalse
      assertThat(
              Row { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
                  .isEquivalentTo(
                      Row { child(ComponentWithStyleProp(Style.alpha(.5f).onClick {})) }))
          .isFalse
    }
  }

  @Test
  fun rowWithCommonProps_isEquivalentToWithCommonProps_checksAllFields() {
    val onClick: (ClickEvent) -> Unit = {}

    with(ComponentScope(lithoViewRule.context)) {
      assertThat(
              Row(style = Style.alpha(.5f).onClick(action = onClick)) {}
                  .isEquivalentTo(Row(style = Style.alpha(.5f).onClick(action = onClick)) {}))
          .isTrue
      assertThat(
              Row(style = Style.alpha(.5f).onClick(action = onClick)) {}
                  .isEquivalentTo(Row(style = Style.alpha(.1f).onClick(action = onClick)) {}))
          .isFalse
      assertThat(
              Row(style = Style.alpha(.5f).onClick(action = onClick)) {}
                  .isEquivalentTo(Row(style = Style.alpha(.5f).onClick {}) {}))
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

    with(ComponentScope(lithoViewRule.context)) {
      assertThat(
              Column { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
                  .isEquivalentTo(
                      Column {
                        child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)))
                      }))
          .isTrue
      assertThat(
              Column { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
                  .isEquivalentTo(
                      Column {
                        child(ComponentWithStyleProp(Style.alpha(.1f).onClick(action = onClick)))
                      }))
          .isFalse
      assertThat(
              Column { child(ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick))) }
                  .isEquivalentTo(
                      Column { child(ComponentWithStyleProp(Style.alpha(.5f).onClick {})) }))
          .isFalse
    }
  }

  @Test
  fun columnWithCommonProps_isEquivalentToWithCommonProps_checksAllFields() {
    val onClick: (ClickEvent) -> Unit = {}

    with(ComponentScope(lithoViewRule.context)) {
      assertThat(
              Column(style = Style.alpha(.5f).onClick(action = onClick)) {}
                  .isEquivalentTo(Column(style = Style.alpha(.5f).onClick(action = onClick)) {}))
          .isTrue
      assertThat(
              Column(style = Style.alpha(.5f).onClick(action = onClick)) {}
                  .isEquivalentTo(Column(style = Style.alpha(.1f).onClick(action = onClick)) {}))
          .isFalse
      assertThat(
              Column(style = Style.alpha(.5f).onClick(action = onClick)) {}
                  .isEquivalentTo(Column(style = Style.alpha(.5f).onClick {}) {}))
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

    fun Wrapper(style: Style? = null, delegate: () -> Component?): Component =
        Wrapper.create(lithoViewRule.context).delegate(delegate()).kotlinStyle(style).build()

    assertThat(
            Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)) }
                .isEquivalentTo(
                    Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)) }))
        .isTrue
    assertThat(
            Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)) }
                .isEquivalentTo(
                    Wrapper { ComponentWithStyleProp(Style.alpha(.1f).onClick(action = onClick)) }))
        .isFalse
    assertThat(
            Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick(action = onClick)) }
                .isEquivalentTo(Wrapper { ComponentWithStyleProp(Style.alpha(.5f).onClick {}) }))
        .isFalse
  }

  @Test
  fun kcomponentWithWrappedCommonProps_isEquivalentTo_checksAllFields() {
    val onClick: (ClickEvent) -> Unit = {}

    fun Wrapper(style: Style? = null, delegate: () -> Component?): Component =
        Wrapper.create(lithoViewRule.context).delegate(delegate()).kotlinStyle(style).build()

    assertThat(
            Wrapper(Style.alpha(.5f).onClick(action = onClick)) { null }
                .isEquivalentTo(Wrapper(Style.alpha(.5f).onClick(action = onClick)) { null }))
        .isTrue
    assertThat(
            Wrapper(Style.alpha(.5f).onClick(action = onClick)) { null }
                .isEquivalentTo(Wrapper(Style.alpha(.1f).onClick(action = onClick)) { null }))
        .isFalse
    assertThat(
            Wrapper(Style.alpha(.5f).onClick(action = onClick)) { null }
                .isEquivalentTo(Wrapper(Style.alpha(.5f).onClick {}) { null }))
        .isFalse
  }
}
