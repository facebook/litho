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
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.config.TempComponentsConfigurations
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.Whitebox
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.DynamicPropsComponentTester
import com.facebook.litho.widget.Image
import com.facebook.litho.widget.MountSpecWithMountUnmountAssertion
import com.facebook.litho.widget.MountSpecWithMountUnmountAssertionSpec
import com.facebook.litho.widget.Progress
import com.facebook.litho.widget.SolidColor
import com.facebook.litho.widget.Text
import com.facebook.litho.widget.TextInput
import com.facebook.rendercore.MountState
import com.facebook.yoga.YogaEdge
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class MountStateTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()
  private lateinit var context: ComponentContext

  @Before
  fun setup() {
    context = legacyLithoViewRule.context
  }

  @Test
  fun testDetachLithoView_unbindComponentFromContent() {
    val child1 = DynamicPropsComponentTester.create(context).dynamicPropValue(1).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val dynamicPropsManager = legacyLithoViewRule.lithoView.dynamicPropsManager
    assertThat(dynamicPropsManager).isNotNull
    assertThat(dynamicPropsManager?.hasCachedContent(child1)).isTrue
    legacyLithoViewRule.detachFromWindow()
    assertThat(dynamicPropsManager?.hasCachedContent(child1)).isFalse
  }

  @Test
  fun testUnbindMountItem_unbindComponentFromContent() {
    val child1 = DynamicPropsComponentTester.create(context).dynamicPropValue(1).build()
    val root =
        Column.create(context)
            .child(Wrapper.create(context).delegate(child1).widthPx(10).heightPx(10))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val dynamicPropsManager = legacyLithoViewRule.lithoView.dynamicPropsManager
    assertThat(dynamicPropsManager?.hasCachedContent(child1)).isTrue
    legacyLithoViewRule.setRoot(Column.create(context).build())
    assertThat(dynamicPropsManager?.hasCachedContent(child1)).isFalse
  }

  @Test
  fun onSetRootWithNoOutputsWithRenderCore_shouldSuccessfullyCompleteMount() {
    val root =
        Wrapper.create(context)
            .delegate(SolidColor.create(context).color(Color.BLACK).build())
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val emptyRoot: Component = Wrapper.create(context).delegate(null).build()
    legacyLithoViewRule.setRoot(emptyRoot)
  }

  @Test
  fun onSetRootWithSimilarComponent_MountContentShouldUsePools() {
    val root =
        Column.create(context)
            .child(TextInput.create(context).widthDip(100f).heightDip(100f))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val view = legacyLithoViewRule.lithoView.getChildAt(0)
    val newRoot =
        Row.create(context)
            .child(TextInput.create(context).initialText("testing").widthDip(120f).heightDip(120f))
            .build()
    legacyLithoViewRule.setRoot(newRoot).setSizeSpecs(exactly(1_000), exactly(1_000))
    val newView = legacyLithoViewRule.lithoView.getChildAt(0)
    assertThat(newView).isSameAs(view)
  }

  @Test
  fun onSetRootWithDifferentComponent_MountContentPoolsShouldNoCollide() {
    val root =
        Column.create(context)
            .child(TextInput.create(context).widthDip(100f).heightDip(100f))
            .build()
    legacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val newRoot =
        Column.create(context)
            .child(Progress.create(context).widthDip(100f).heightDip(100f))
            .build()
    legacyLithoViewRule.setRoot(newRoot).setSizeSpecs(exactly(1_000), exactly(1_000))
  }

  @Test
  fun onSetRootWithNullComponentWithStatelessness_shouldMountWithoutCrashing() {
    legacyLithoViewRule
        .attachToWindow()
        .setRoot(EmptyComponent())
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    assertThat(legacyLithoViewRule.currentRootNode).isNull()
    assertThat(legacyLithoViewRule.lithoView.childCount).isEqualTo(0)
    val tree = legacyLithoViewRule.committedLayoutState?.toRenderTree()
    assertThat(tree?.mountableOutputCount).isEqualTo(1)
    assertThat(tree?.root).isSameAs(tree?.getRenderTreeNodeAtIndex(0))
    assertThat(tree?.getRenderTreeNodeIndex(MountState.ROOT_HOST_ID)).isEqualTo(0)
  }

  @Test
  fun mountingChildForUnmountedParentInRenderCore_shouldMountWithoutCrashing() {
    val root =
        Row.create(context)
            .backgroundColor(Color.BLUE)
            .widthPx(20)
            .heightPx(20)
            .viewTag("root")
            .child(
                Row.create(context) // Parent that will be unmounted
                    .backgroundColor(Color.RED)
                    .widthPx(20)
                    .heightPx(20)
                    .viewTag("parent")
                    .border(
                        Border.create(context) // Drawable to be mounted after parent unmounts
                            .widthPx(YogaEdge.ALL, 2)
                            .color(YogaEdge.ALL, Color.YELLOW)
                            .build()))
            .build()
    legacyLithoViewRule
        .useComponentTree(
            ComponentTree.create(context)
                .componentsConfiguration(
                    ComponentsConfiguration.create()
                        .shouldAddHostViewForRootComponent(true)
                        .build())
                .build())
        .attachToWindow()
        .setRoot(root)
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val parentOfParent = legacyLithoViewRule.findViewWithTagOrNull("root") as ComponentHost
    val parentNode = parentOfParent.getMountItemAt(0).renderTreeNode
    val parentId = parentNode.renderUnit.id
    val childId = parentNode.getChildAt(0).renderUnit.id

    // Unmount the parent
    legacyLithoViewRule.lithoView.mountDelegateTarget.notifyUnmount(parentId)

    // Attempt to mount the child (border drawable)
    // If there is a problem, a crash will occur here.
    legacyLithoViewRule.lithoView.mountDelegateTarget.notifyMount(childId)
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent()
  }

  @Test
  fun shouldUnregisterAllExtensions_whenUnmountAllItems() {
    val root =
        Row.create(context)
            .backgroundColor(Color.BLUE)
            .child(
                Image.create(context).drawable(ColorDrawable(Color.RED)).heightPx(100).widthPx(200))
            .build()
    legacyLithoViewRule
        .attachToWindow()
        .setRoot(root)
        .setSizeSpecs(exactly(1_000), exactly(1_000))
        .measure()
        .layout()
    val lithoView = legacyLithoViewRule.lithoView
    val mountDelegate = lithoView.mountDelegateTarget.mountDelegate
    var coordinator =
        Whitebox.getInternalState<LithoHostListenerCoordinator>(
            lithoView, "mLithoHostListenerCoordinator")
    assertThat(coordinator).isNotNull
    assertThat(coordinator.visibilityExtensionState).isNotNull
    assertThat(coordinator.incrementalMountExtensionState).isNotNull
    assertThat(mountDelegate).isNotNull
    assertThat(mountDelegate?.extensionStates).isNotEmpty

    // Unmount the parent
    legacyLithoViewRule.lithoView.unmountAllItems()
    coordinator =
        Whitebox.getInternalState<LithoHostListenerCoordinator>(
            lithoView, "mLithoHostListenerCoordinator")
    assertThat(coordinator).isNull()
    assertThat(mountDelegate).isNotNull
    assertThat(mountDelegate?.extensionStates).isEmpty()
  }

  /**
   * This test case captures the scenario where unmount gets called on a component which was moved
   * to a different location during prepare mount which causes the wrong item to be unmounted, which
   * can lead to crashes. 1. A layout is mounted. 2. The next layout update cause an item to be
   * moved to a different position, but at that position it gets unmounted because it is outside the
   * visible rect. 3. This causes the wrong item to be unmounted.
   */
  @Test
  fun whenItemsAreMovedThenUnmountedInTheNextMountLoop_shouldUnmountTheCorrectItem() {

    // TODO(T118124771): Test failure because of incorrect visible bounds
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return
    }
    val c = legacyLithoViewRule.context
    val initialComponent =
        Column.create(c)
            .heightPx(800)
            .child(
                Column.create(c)
                    .wrapInView()
                    .heightPx(800)
                    .child(TextInput.create(c).key("#0").initialText("0").heightPx(100))
                    .child(
                        MountSpecWithMountUnmountAssertion.create(c)
                            .key("test")
                            .container(MountSpecWithMountUnmountAssertionSpec.Container())
                            .heightPx(100))
                    .child(TextInput.create(c).key("#2").initialText("2").heightPx(100))
                    .child(TextInput.create(c).key("#3").initialText("3").heightPx(100))
                    .child(TextInput.create(c).key("#4").initialText("4").heightPx(100))
                    .child(TextInput.create(c).key("#5").initialText("5").heightPx(100))
                    .child(TextInput.create(c).key("#6").initialText("6").heightPx(100)))
            .build()
    val initialComponentTree = ComponentTree.create(c, initialComponent).build()
    val lithoView = LithoView(c.androidContext)

    // Mount a layout with the component.
    lithoView.componentTree = initialComponentTree
    lithoView.measure(exactly(100), exactly(800))
    lithoView.layout(0, 0, 100, 800)

    // Assert that the view is mounted
    assertThat(lithoView.childCount).isEqualTo(1)
    assertThat((lithoView.getChildAt(0) as ComponentHost).childCount).isEqualTo(7)
    val newComponent =
        Column.create(c)
            .heightPx(800)
            .child(
                Column.create(c)
                    .wrapInView()
                    .heightPx(800)
                    .child(TextInput.create(c).key("#0").initialText("0").heightPx(100))
                    .child(Text.create(c).key("#1").text("1").heightPx(100))
                    .child(TextInput.create(c).key("#2").initialText("2").heightPx(100))
                    .child(TextInput.create(c).key("#3").initialText("3").heightPx(100))
                    .child(TextInput.create(c).key("#4").initialText("4").heightPx(100))
                    .child(
                        MountSpecWithMountUnmountAssertion.create(c)
                            .key("test")
                            .container(MountSpecWithMountUnmountAssertionSpec.Container())
                            .heightPx(100))
                    .child(TextInput.create(c).key("#6").initialText("6").heightPx(100)))
            .build()
    lithoView.setComponent(newComponent)

    // Mount a new layout, but with a shorter height, to make the item unmount
    lithoView.measure(exactly(100), exactly(95))
    lithoView.layout(0, 0, 100, 95)

    // Assert that the items is unmounted.
    assertThat(lithoView.childCount).isEqualTo(1)
    assertThat((lithoView.getChildAt(0) as ComponentHost).childCount).isEqualTo(1)
    lithoView.unmountAllItems()
  }
}
