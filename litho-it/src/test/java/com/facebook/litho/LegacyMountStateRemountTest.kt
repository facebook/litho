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
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.drawable.ComparableDrawable
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.helper.ComponentTestHelper
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.EditText
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LegacyMountStateRemountTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false)
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
  }

  @Test
  fun testRemountOnNodeInfoLayoutChanges() {
    val oldComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(Text.create(context).textSizeSp(12f).text("label:"))
            .child(
                EditText.create(context)
                    .text("Hello World")
                    .textSizeSp(12f)
                    .viewTag("Alpha")
                    .enabled(true))
            .build()
    val lithoView = LithoView(context)
    val componentTree =
        ComponentTree.create(context, oldComponent)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build()
    ComponentTestHelper.mountComponent(lithoView, componentTree, exactly(400), exactly(400))
    val oldView = lithoView.getChildAt(0)
    val oldTag = oldView.tag
    val oldIsEnabled = oldView.isEnabled
    val newComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(Text.create(context).textSizeSp(12f).text("label:"))
            .child(
                EditText.create(context)
                    .text("Hello World")
                    .textSizeSp(12f)
                    .viewTag("Beta")
                    .enabled(false))
            .build()
    componentTree.setRootAndSizeSpecSync(newComponent, exactly(400), exactly(400))
    componentTree.setSizeSpec(exactly(400), exactly(400))
    val newView = lithoView.getChildAt(0)
    assertThat(newView).isSameAs(oldView)
    val newTag = newView.tag
    val newIsEnabled = newView.isEnabled
    assertThat(newTag).isNotEqualTo(oldTag)
    assertThat(newIsEnabled).isNotEqualTo(oldIsEnabled)
  }

  @Test
  fun testRemountOnNoLayoutChanges() {
    val oldComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(
                EditText.create(context)
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.CYAN)
                    .text("Hello World")
                    .viewTag("Alpha")
                    .contentDescription("some description"))
            .build()
    val lithoView = LithoView(context)
    val componentTree =
        ComponentTree.create(context, oldComponent)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build()
    ComponentTestHelper.mountComponent(lithoView, componentTree, exactly(400), exactly(400))
    val oldView = lithoView.getChildAt(0)
    val oldTag = oldView.tag
    val oldContentDescription = oldView.contentDescription.toString()
    val oldBackground = oldView.background
    val newComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(
                EditText.create(context)
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.CYAN)
                    .text("Hello World")
                    .viewTag("Alpha")
                    .contentDescription("some description"))
            .build()
    componentTree.setRootAndSizeSpecSync(newComponent, exactly(400), exactly(400))
    componentTree.setSizeSpec(exactly(400), exactly(400))
    val newView = lithoView.getChildAt(0)
    assertThat(newView).isSameAs(oldView)
    val newTag = newView.tag
    val newContentDescription = newView.contentDescription.toString()
    val newBackground = newView.background

    // Check that props were not set again
    assertThat(newTag).isSameAs(oldTag)
    assertThat(newContentDescription).isSameAs(oldContentDescription)
    assertThat(oldBackground).isSameAs(newBackground)
  }

  @Test
  fun testRemountOnViewNodeInfoLayoutChanges() {
    val oldComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(Text.create(context).textSizeSp(12f).text("label:"))
            .child(
                EditText.create(context)
                    .text("Hello World")
                    .textSizeSp(12f)
                    .backgroundColor(Color.RED))
            .build()
    val lithoView = LithoView(context)
    val componentTree =
        ComponentTree.create(context, oldComponent)
            .incrementalMount(false)
            .layoutDiffing(true)
            .build()
    ComponentTestHelper.mountComponent(lithoView, componentTree, exactly(400), exactly(400))
    val oldView = lithoView.getChildAt(0)
    val oldDrawable = oldView.background as ComparableDrawable
    val newComponent =
        Column.create(context)
            .backgroundColor(Color.WHITE)
            .child(Text.create(context).textSizeSp(12f).text("label:"))
            .child(
                EditText.create(context)
                    .text("Hello World")
                    .textSizeSp(12f)
                    .backgroundColor(Color.CYAN))
            .build()
    componentTree.setRootAndSizeSpecSync(newComponent, exactly(400), exactly(400))
    componentTree.setSizeSpec(exactly(400), exactly(400))
    val newView = lithoView.getChildAt(0)
    assertThat(newView).isSameAs(oldView)
    val newDrawable = newView.background as ComparableDrawable
    assertThat(oldDrawable.isEquivalentTo(newDrawable)).isFalse
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }
}
