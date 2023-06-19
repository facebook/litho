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

import android.R
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.MountSpecLithoRenderUnit.Companion.STATE_DIRTY
import com.facebook.litho.MountSpecLithoRenderUnit.Companion.STATE_UNKNOWN
import com.facebook.litho.MountSpecLithoRenderUnit.Companion.STATE_UPDATED
import com.facebook.litho.SizeSpec.makeSizeSpec
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.drawable.ComparableColorDrawable
import com.facebook.litho.testing.TestDrawableComponent
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LegacyTreeDiffingTest {

  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false)
    context = ComponentContext(ApplicationProvider.getApplicationContext<Context>())
    redDrawable = ComparableColorDrawable.create(Color.RED)
    blackDrawable = ComparableColorDrawable.create(Color.BLACK)
    transparentDrawable = ComparableColorDrawable.create(Color.TRANSPARENT)
  }

  @Test
  fun testLayoutOutputUpdateStateWithBackground() {
    val component1 = TestLayoutSpecBgState(false)
    val component2 = TestLayoutSpecBgState(false)
    val component3 = TestLayoutSpecBgState(true)
    val lithoView = LithoView(context)
    val componentTree = ComponentTree.create(context, component1).build()
    lithoView.componentTree = componentTree
    lithoView.onAttachedToWindow()
    componentTree.setRootAndSizeSpecSync(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY))
    val state = requireNotNull(componentTree.mainThreadLayoutState)
    assertOutputsState(state, MountSpecLithoRenderUnit.STATE_UNKNOWN)
    componentTree.root = component2
    val secondState = requireNotNull(componentTree.mainThreadLayoutState)
    assertThat(5).isEqualTo(secondState.mountableOutputCount)
    assertOutputsState(secondState, MountSpecLithoRenderUnit.STATE_UPDATED)
    componentTree.root = component3
    val thirdState = requireNotNull(componentTree.mainThreadLayoutState)
    assertThat(5).isEqualTo(thirdState.mountableOutputCount)
    assertThat(
            MountSpecLithoRenderUnit.getUpdateState(
                requireNotNull(thirdState.getMountableOutputAt(1))))
        .isEqualTo(STATE_DIRTY)
    assertThat(
            MountSpecLithoRenderUnit.getUpdateState(
                requireNotNull(thirdState.getMountableOutputAt(2))))
        .isEqualTo(STATE_UPDATED)
    assertThat(
            MountSpecLithoRenderUnit.getUpdateState(
                requireNotNull(thirdState.getMountableOutputAt(3))))
        .isEqualTo(STATE_UPDATED)
    assertThat(
            MountSpecLithoRenderUnit.getUpdateState(
                requireNotNull(thirdState.getMountableOutputAt(4))))
        .isEqualTo(STATE_UPDATED)
  }

  // This test covers the same case with the foreground since the code path is the same!
  @Test
  fun testLayoutOutputUpdateStateWithBackgroundInWithLayout() {
    val component1 = TestLayoutSpecInnerState(false)
    val component2 = TestLayoutSpecInnerState(false)
    val component3 = TestLayoutSpecInnerState(true)
    val lithoView = LithoView(context)
    val componentTree = ComponentTree.create(context, component1).build()
    lithoView.componentTree = componentTree
    lithoView.onAttachedToWindow()
    componentTree.setRootAndSizeSpecSync(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY))
    val state = requireNotNull(componentTree.mainThreadLayoutState)
    assertThat(
            MountSpecLithoRenderUnit.getUpdateState(requireNotNull(state.getMountableOutputAt(2))))
        .isEqualTo(STATE_UNKNOWN)
    componentTree.root = component2
    val secondState = requireNotNull(componentTree.mainThreadLayoutState)
    assertThat(
            MountSpecLithoRenderUnit.getUpdateState(
                requireNotNull(secondState.getMountableOutputAt(2))))
        .isEqualTo(STATE_UPDATED)
    componentTree.root = component3
    val thirdState = requireNotNull(componentTree.mainThreadLayoutState)
    assertThat(
            MountSpecLithoRenderUnit.getUpdateState(
                requireNotNull(thirdState.getMountableOutputAt(2))))
        .isEqualTo(STATE_DIRTY)
  }

  private class TestLayoutSpecBgState(private val changeBg: Boolean) : InlineLayoutSpec() {
    override fun onCreateLayout(c: ComponentContext): Component? =
        Column.create(c)
            .background(if (changeBg) blackDrawable else redDrawable)
            .foreground(transparentDrawable)
            .child(TestDrawableComponent.create(c))
            .child(Column.create(c).child(TestDrawableComponent.create(c)))
            .build()
  }

  private class TestLayoutSpecInnerState(private val changeChildDrawable: Boolean) :
      InlineLayoutSpec() {
    override fun onCreateLayout(c: ComponentContext): Component? =
        Column.create(c)
            .background(redDrawable)
            .foregroundRes(R.drawable.btn_default)
            .child(
                TestDrawableComponent.create(c)
                    .background(if (changeChildDrawable) redDrawable else blackDrawable))
            .child(Column.create(c).child(TestDrawableComponent.create(c)))
            .build()
  }

  @After
  fun restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  companion object {
    private lateinit var redDrawable: Drawable
    private lateinit var blackDrawable: Drawable
    private lateinit var transparentDrawable: Drawable

    private fun assertOutputsState(
        layoutState: LayoutState,
        @MountSpecLithoRenderUnit.UpdateState state: Int
    ) {
      assertThat(STATE_DIRTY)
          .isEqualTo(MountSpecLithoRenderUnit.getUpdateState(layoutState.getMountableOutputAt(0)))
      for (i in 1 until layoutState.mountableOutputCount) {
        assertThat(state)
            .isEqualTo(MountSpecLithoRenderUnit.getUpdateState(layoutState.getMountableOutputAt(i)))
      }
    }
  }
}
