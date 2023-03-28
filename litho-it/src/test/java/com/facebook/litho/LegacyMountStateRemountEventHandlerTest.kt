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
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.SimpleMountSpecTester
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LegacyMountStateRemountEventHandlerTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false)
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testReuseLongClickListenerOnSameView() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .longClickHandler(c.newEventHandler(1) as? EventHandler<LongClickEvent>)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    val longClickListener = LithoViewAttributesExtension.getComponentLongClickListener(lithoView)
    assertThat(longClickListener).isNotNull
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .longClickHandler(c.newEventHandler(1) as? EventHandler<LongClickEvent>)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    assertThat(
            longClickListener ===
                LithoViewAttributesExtension.getComponentLongClickListener(lithoView))
        .isTrue
  }

  @Test
  fun testReuseFocusChangeListenerListenerOnSameView() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .focusChangeHandler(c.newEventHandler(1) as? EventHandler<FocusChangedEvent>)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    val focusChangeListener =
        LithoViewAttributesExtension.getComponentFocusChangeListener(lithoView)
    assertThat(focusChangeListener).isNotNull
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .focusChangeHandler(c.newEventHandler(1) as? EventHandler<FocusChangedEvent>)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    assertThat(
            focusChangeListener ===
                LithoViewAttributesExtension.getComponentFocusChangeListener(lithoView))
        .isTrue
  }

  @Test
  fun testReuseTouchListenerOnSameView() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .touchHandler(c.newEventHandler(1) as? EventHandler<TouchEvent>)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    val touchListener = LithoViewAttributesExtension.getComponentTouchListener(lithoView)
    assertThat(touchListener).isNotNull
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .touchHandler(c.newEventHandler(2) as? EventHandler<TouchEvent>)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    assertThat(LithoViewAttributesExtension.getComponentTouchListener(lithoView))
        .isEqualTo(touchListener)
  }

  @Test
  fun testUnsetClickHandler() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .clickHandler(c.newEventHandler(1) as? EventHandler<ClickEvent>)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    assertThat(lithoView.isClickable).isTrue
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    assertThat(lithoView.isClickable).isFalse
  }

  @Test
  fun testUnsetLongClickHandler() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .longClickHandler(c.newEventHandler(1) as? EventHandler<LongClickEvent>)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    assertThat(LithoViewAttributesExtension.getComponentLongClickListener(lithoView)).isNotNull
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    val listener = LithoViewAttributesExtension.getComponentLongClickListener(lithoView)
    assertThat(listener).isNotNull
    assertThat(listener?.eventHandler).isNull()
  }

  @Test
  fun testUnsetFocusChangeHandler() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .focusChangeHandler(c.newEventHandler(1) as? EventHandler<FocusChangedEvent>)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    assertThat(LithoViewAttributesExtension.getComponentFocusChangeListener(lithoView)).isNotNull
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    val listener = LithoViewAttributesExtension.getComponentFocusChangeListener(lithoView)
    assertThat(listener).isNotNull
    assertThat(listener?.eventHandler).isNull()
  }

  @Test
  fun testUnsetTouchHandler() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .touchHandler(c.newEventHandler(1) as? EventHandler<TouchEvent>)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    val listener = LithoViewAttributesExtension.getComponentTouchListener(lithoView)
    assertThat(listener?.eventHandler).isNull()
  }

  @Test
  fun testSetLongClickHandler() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    assertThat(LithoViewAttributesExtension.getComponentLongClickListener(lithoView)).isNull()
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .longClickHandler(c.newEventHandler(1) as? EventHandler<LongClickEvent>)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    val listener = LithoViewAttributesExtension.getComponentLongClickListener(lithoView)
    assertThat(listener).isNotNull
    assertThat(listener?.eventHandler).isNotNull
  }

  @Test
  fun testSetFocusChangeHandler() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    assertThat(LithoViewAttributesExtension.getComponentFocusChangeListener(lithoView)).isNull()
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .focusChangeHandler(c.newEventHandler(1) as? EventHandler<FocusChangedEvent>)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    val listener = LithoViewAttributesExtension.getComponentFocusChangeListener(lithoView)
    assertThat(listener).isNotNull
    assertThat(listener?.eventHandler).isNotNull
  }

  @Test
  fun testSetTouchHandler() {
    val lithoView =
        ComponentTestHelper.mountComponent(
            context,
            object : InlineLayoutSpec() {
              override fun onCreateLayout(c: ComponentContext): Component =
                  Column.create(c)
                      .child(SimpleMountSpecTester.create(c))
                      .child(SimpleMountSpecTester.create(c))
                      .build()
            })
    assertThat(LithoViewAttributesExtension.getComponentTouchListener(lithoView)).isNull()
    lithoView.componentTree?.root =
        object : InlineLayoutSpec() {
          override fun onCreateLayout(c: ComponentContext): Component =
              Column.create(c)
                  .touchHandler(c.newEventHandler(1) as? EventHandler<TouchEvent>)
                  .child(SimpleMountSpecTester.create(c))
                  .child(SimpleMountSpecTester.create(c))
                  .build()
        }
    val listener = LithoViewAttributesExtension.getComponentTouchListener(lithoView)
    assertThat(listener).isNotNull
    assertThat(listener?.eventHandler).isNotNull
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }
}
