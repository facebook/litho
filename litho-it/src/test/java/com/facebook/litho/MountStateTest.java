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

package com.facebook.litho;

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.rendercore.MountState.ROOT_HOST_ID;
import static com.facebook.rendercore.utils.MeasureSpecUtils.exactly;
import static org.assertj.core.api.Assertions.assertThat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.DynamicPropsComponentTester;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.MountSpecWithMountUnmountAssertion;
import com.facebook.litho.widget.MountSpecWithMountUnmountAssertionSpec;
import com.facebook.litho.widget.Progress;
import com.facebook.litho.widget.SolidColor;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.TextInput;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateTest {

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = mLegacyLithoViewRule.getContext();
  }

  @Test
  public void testDetachLithoView_unbindComponentFromContent() {
    final Component child1 =
        DynamicPropsComponentTester.create(mContext).dynamicPropValue(1).build();

    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .build();

    mLegacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final DynamicPropsManager dynamicPropsManager =
        mLegacyLithoViewRule.getLithoView().getDynamicPropsManager();

    assertThat(dynamicPropsManager).isNotNull();
    assertThat(dynamicPropsManager.hasCachedContent(child1)).isTrue();

    mLegacyLithoViewRule.detachFromWindow();
    assertThat(dynamicPropsManager.hasCachedContent(child1)).isFalse();
  }

  @Test
  public void testUnbindMountItem_unbindComponentFromContent() {
    final Component child1 =
        DynamicPropsComponentTester.create(mContext).dynamicPropValue(1).build();

    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .build();

    mLegacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final DynamicPropsManager dynamicPropsManager =
        mLegacyLithoViewRule.getLithoView().getDynamicPropsManager();
    assertThat(dynamicPropsManager.hasCachedContent(child1)).isTrue();

    mLegacyLithoViewRule.setRoot(Column.create(mContext).build());
    assertThat(dynamicPropsManager.hasCachedContent(child1)).isFalse();
  }

  @Test
  public void onSetRootWithNoOutputsWithRenderCore_shouldSuccessfullyCompleteMount() {
    final Component root =
        Wrapper.create(mContext)
            .delegate(SolidColor.create(mContext).color(Color.BLACK).build())
            .build();

    mLegacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final Component emptyRoot = Wrapper.create(mContext).delegate(null).build();

    mLegacyLithoViewRule.setRoot(emptyRoot);
  }

  @Test
  public void onSetRootWithSimilarComponent_MountContentShouldUsePools() {
    final Component root =
        Column.create(mContext)
            .child(TextInput.create(mContext).widthDip(100).heightDip(100))
            .build();

    mLegacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    View view = mLegacyLithoViewRule.getLithoView().getChildAt(0);

    final Component newRoot =
        Row.create(mContext)
            .child(TextInput.create(mContext).initialText("testing").widthDip(120).heightDip(120))
            .build();

    mLegacyLithoViewRule
        .setRoot(newRoot)
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY));

    View newView = mLegacyLithoViewRule.getLithoView().getChildAt(0);

    assertThat(newView).isSameAs(view);
  }

  @Test
  public void onSetRootWithDifferentComponent_MountContentPoolsShouldNoCollide() {
    final Component root =
        Column.create(mContext)
            .child(TextInput.create(mContext).widthDip(100).heightDip(100))
            .build();

    mLegacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final Component newRoot =
        Column.create(mContext)
            .child(Progress.create(mContext).widthDip(100).heightDip(100))
            .build();

    mLegacyLithoViewRule
        .setRoot(newRoot)
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY));
  }

  @Test
  public void onSetRootWithNullComponentWithStatelessness_shouldMountWithoutCrashing() {
    mLegacyLithoViewRule
        .attachToWindow()
        .setRoot(new EmptyComponent())
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    assertThat(mLegacyLithoViewRule.getCurrentRootNode()).isNull();
    assertThat(mLegacyLithoViewRule.getLithoView().getChildCount()).isEqualTo(0);

    final RenderTree tree = mLegacyLithoViewRule.getCommittedLayoutState().toRenderTree();

    assertThat(tree.getMountableOutputCount()).isEqualTo(1);
    assertThat(tree.getRoot()).isSameAs(tree.getRenderTreeNodeAtIndex(0));
    assertThat(tree.getRenderTreeNodeIndex(ROOT_HOST_ID)).isEqualTo(0);
  }

  @Test
  public void mountingChildForUnmountedParentInRenderCore_shouldMountWithoutCrashing() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);

    final Component root =
        Row.create(mContext)
            .backgroundColor(Color.BLUE)
            .widthPx(20)
            .heightPx(20)
            .viewTag("root")
            .child(
                Row.create(mContext) // Parent that will be unmounted
                    .backgroundColor(Color.RED)
                    .widthPx(20)
                    .heightPx(20)
                    .viewTag("parent")
                    .border(
                        Border.create(mContext) // Drawable to be mounted after parent unmounts
                            .widthPx(YogaEdge.ALL, 2)
                            .color(YogaEdge.ALL, Color.YELLOW)
                            .build()))
            .build();

    mLegacyLithoViewRule
        .attachToWindow()
        .setRoot(root)
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final ComponentHost parentOfParent =
        (ComponentHost) mLegacyLithoViewRule.findViewWithTagOrNull("root");

    final RenderTreeNode parentNode = parentOfParent.getMountItemAt(0).getRenderTreeNode();

    final long parentId = parentNode.getRenderUnit().getId();
    final long childId = parentNode.getChildAt(0).getRenderUnit().getId();

    // Unmount the parent
    mLegacyLithoViewRule.getLithoView().getMountDelegateTarget().notifyUnmount(parentId);

    // Attempt to mount the child (border drawable)
    // If there is a problem, a crash will occur here.
    mLegacyLithoViewRule.getLithoView().getMountDelegateTarget().notifyMount(childId);

    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }

  @Test
  public void shouldUnregisterAllExtensions_whenUnmountAllItems() {
    final Component root =
        Row.create(mContext)
            .backgroundColor(Color.BLUE)
            .child(
                Image.create(mContext)
                    .drawable(new ColorDrawable(Color.RED))
                    .heightPx(100)
                    .widthPx(200))
            .build();

    mLegacyLithoViewRule
        .attachToWindow()
        .setRoot(root)
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLegacyLithoViewRule.getLithoView();
    final MountDelegate mountDelegate = lithoView.getMountDelegateTarget().getMountDelegate();
    LithoHostListenerCoordinator coordinator;

    coordinator = Whitebox.getInternalState(lithoView, "mLithoHostListenerCoordinator");

    assertThat(coordinator).isNotNull();
    assertThat(coordinator.getVisibilityExtensionState()).isNotNull();
    assertThat(coordinator.getIncrementalMountExtensionState()).isNotNull();

    assertThat(mountDelegate).isNotNull();
    assertThat(mountDelegate.getExtensionStates()).isNotEmpty();

    // Unmount the parent
    mLegacyLithoViewRule.getLithoView().unmountAllItems();

    coordinator = Whitebox.getInternalState(lithoView, "mLithoHostListenerCoordinator");

    assertThat(coordinator).isNull();
    assertThat(mountDelegate).isNotNull();
    assertThat(mountDelegate.getExtensionStates()).isEmpty();
  }

  /**
   * This test case captures the scenario where unmount gets called on a component which was moved
   * to a different location during prepare mount which causes the wrong item to be unmounted, which
   * can lead to crashes. 1. A layout is mounted. 2. The next layout update cause an item to be
   * moved to a different position, but at that position it gets unmounted because it is outside the
   * visible rect. 3. This causes the wrong item to be unmounted.
   */
  @Test
  public void whenItemsAreMovedThenUnmountedInTheNextMountLoop_shouldUnmountTheCorrectItem() {

    // TODO(T118124771): Test failure because of incorrect visible bounds
    if (ComponentsConfiguration.lithoViewSelfManageViewPortChanges) {
      return;
    }

    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component initialComponent =
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
                            .container(new MountSpecWithMountUnmountAssertionSpec.Container())
                            .heightPx(100))
                    .child(TextInput.create(c).key("#2").initialText("2").heightPx(100))
                    .child(TextInput.create(c).key("#3").initialText("3").heightPx(100))
                    .child(TextInput.create(c).key("#4").initialText("4").heightPx(100))
                    .child(TextInput.create(c).key("#5").initialText("5").heightPx(100))
                    .child(TextInput.create(c).key("#6").initialText("6").heightPx(100)))
            .build();

    final ComponentTree initialComponentTree = ComponentTree.create(c, initialComponent).build();

    LithoView lithoView = new LithoView(c.getAndroidContext());

    // Mount a layout with the component.
    lithoView.setComponentTree(initialComponentTree);
    lithoView.measure(exactly(100), exactly(800));
    lithoView.layout(0, 0, 100, 800);

    // Assert that the view is mounted
    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(((ComponentHost) lithoView.getChildAt(0)).getChildCount()).isEqualTo(7);

    Component newComponent =
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
                            .container(new MountSpecWithMountUnmountAssertionSpec.Container())
                            .heightPx(100))
                    .child(TextInput.create(c).key("#6").initialText("6").heightPx(100)))
            .build();

    lithoView.setComponent(newComponent);

    // Mount a new layout, but with a shorter height, to make the item unmount
    lithoView.measure(exactly(100), exactly(95));
    lithoView.layout(0, 0, 100, 95);

    // Assert that the items is unmounted.
    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(((ComponentHost) lithoView.getChildAt(0)).getChildCount()).isEqualTo(1);

    lithoView.unmountAllItems();
  }
}
