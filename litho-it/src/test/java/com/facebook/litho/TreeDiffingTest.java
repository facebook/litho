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

import static android.R.drawable.btn_default;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.TRANSPARENT;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutState.calculate;
import static com.facebook.litho.LithoRenderUnit.STATE_DIRTY;
import static com.facebook.litho.LithoRenderUnit.STATE_UNKNOWN;
import static com.facebook.litho.LithoRenderUnit.STATE_UPDATED;
import static com.facebook.litho.LithoRenderUnit.getRenderUnit;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.testing.Whitebox.getInternalState;
import static com.facebook.yoga.YogaAlign.FLEX_START;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.collection.SparseArrayCompat;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestSizeDependentComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.rendercore.MountItem;
import com.facebook.rendercore.RenderTreeNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@RunWith(LithoTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class TreeDiffingTest {

  private static Drawable sRedDrawable;
  private static Drawable sBlackDrawable;
  private static Drawable sTransparentDrawable;

  @Rule public final LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  private int mUnspecifiedSpec;

  @Before
  public void setup() throws Exception {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);
    mUnspecifiedSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    sRedDrawable = ComparableColorDrawable.create(Color.RED);
    sBlackDrawable = ComparableColorDrawable.create(Color.BLACK);
    sTransparentDrawable = ComparableColorDrawable.create(TRANSPARENT);
  }

  @Test
  public void testDiffTreeDisabled() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutState(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY));

    // Check diff tree is null.
    assertThat(layoutState.getDiffTree()).isNull();
  }

  @Test
  public void testDiffTreeEnabled() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    // Check diff tree is not null and consistent.
    DiffNode node = layoutState.getDiffTree();
    assertThat(node).isNotNull();
    assertThat(4).isEqualTo(countNodes(node));
  }

  private static int countNodes(DiffNode node) {
    int sum = 1;
    for (int i = 0; i < node.getChildCount(); i++) {
      sum += countNodes(node.getChildAt(i));
    }

    return sum;
  }

  @Test
  public void testCachedMeasureFunction() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component_0 = Text.create(c).text("hello-world").build();

    mLegacyLithoViewRule.attachToWindow().setRoot(component_0).layout().measure();

    final DiffNode diffNode = mLegacyLithoViewRule.getCommittedLayoutState().getDiffTree();

    final Component component_1 = Text.create(c).text("hello-world").build();

    mLegacyLithoViewRule.setRoot(component_1).layout().measure();

    final LithoLayoutResult result = mLegacyLithoViewRule.getCurrentRootNode();

    assertThat(result.getWidth()).isEqualTo((int) diffNode.getLastMeasuredWidth());
    assertThat(result.getHeight()).isEqualTo((int) diffNode.getLastMeasuredHeight());
  }

  @Test
  public void tesLastConstraints() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component_0 = Text.create(c).text("hello-world").build();

    mLegacyLithoViewRule.attachToWindow().setRoot(component_0).layout().measure();

    final DiffNode diffNode = mLegacyLithoViewRule.getCommittedLayoutState().getDiffTree();

    final Component component_1 = Text.create(c).text("hello-world").build();

    mLegacyLithoViewRule.setRoot(component_1).layout().measure();

    final LithoLayoutResult result = mLegacyLithoViewRule.getCurrentRootNode();

    assertThat(diffNode.getLastWidthSpec()).isEqualTo(result.getWidthSpec());
    assertThat(diffNode.getLastHeightSpec()).isEqualTo(result.getHeightSpec());
  }

  @Test
  public void testCachedMeasures() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component_0 =
        Column.create(c)
            .child(Text.create(c).text("hello-world").build())
            .child(Text.create(c).text("hello-world").build())
            .build();

    mLegacyLithoViewRule.attachToWindow().setRoot(component_0).layout().measure();

    final Component component_1 =
        Column.create(c)
            .child(Text.create(c).text("hello-world").build())
            .child(Text.create(c).text("hello-world").build())
            .build();

    mLegacyLithoViewRule.setRoot(component_1).layout().measure();

    final LithoLayoutResult result = mLegacyLithoViewRule.getCurrentRootNode();

    assertThat(result.getChildAt(0).areCachedMeasuresValid()).isTrue();
    assertThat(result.getChildAt(1).areCachedMeasuresValid()).isTrue();
  }

  @Test
  public void testPartiallyCachedMeasures() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component component_0 =
        Column.create(c)
            .child(Text.create(c).text("hello-world-1").build())
            .child(Text.create(c).text("hello-world-2").build())
            .build();

    mLegacyLithoViewRule.attachToWindow().setRoot(component_0).layout().measure();

    final Component component_1 =
        Column.create(c)
            .child(Text.create(c).text("hello-world-1").build())
            .child(Text.create(c).text("hello-world-3").build())
            .build();

    mLegacyLithoViewRule.setRoot(component_1).layout().measure();

    final LithoLayoutResult result = mLegacyLithoViewRule.getCurrentRootNode();

    assertThat(result.getChildAt(0).areCachedMeasuresValid()).isTrue();
    assertThat(result.getChildAt(1).areCachedMeasuresValid()).isFalse();
  }

  @Test
  public void testLayoutOutputReuse() {
    // Needed because of (legacy) usage two different InlineLayoutSpec that the test wants to treat
    // as the same component type.
    final int COMPONENT_IDENTITY = 12345;
    final Component component1 =
        new InlineLayoutSpec(COMPONENT_IDENTITY) {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    final Component component2 =
        new InlineLayoutSpec(COMPONENT_IDENTITY) {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component1,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component2,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    assertThat(layoutState.getMountableOutputCount())
        .isEqualTo(prevLayoutState.getMountableOutputCount());
    for (int i = 0, count = prevLayoutState.getMountableOutputCount(); i < count; i++) {
      assertThat(layoutState.getMountableOutputAt(i).getRenderUnit().getId())
          .isEqualTo(prevLayoutState.getMountableOutputAt(i).getRenderUnit().getId());
    }
  }

  @Test
  public void testLayoutOutputPartialReuse() {
    // Needed because of (legacy) usage two different InlineLayoutSpec that the test wants to treat
    // as the same component type.
    final int COMPONENT_IDENTITY = 12345;
    final Component component1 =
        new InlineLayoutSpec(COMPONENT_IDENTITY) {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(TestDrawableComponent.create(c))
                .child(Column.create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    final Component component2 =
        new InlineLayoutSpec(COMPONENT_IDENTITY) {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(TestDrawableComponent.create(c))
                .child(Column.create(c).child(TestDrawableComponent.create(c)))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        };

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component1,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            null);
    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component2,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    assertNotEquals(
        prevLayoutState.getMountableOutputCount(), layoutState.getMountableOutputCount());
    for (int i = 0, count = prevLayoutState.getMountableOutputCount(); i < count; i++) {
      assertThat(layoutState.getMountableOutputAt(i).getRenderUnit().getId())
          .describedAs("Output " + i)
          .isEqualTo(prevLayoutState.getMountableOutputAt(i).getRenderUnit().getId());
    }
  }

  private void assertCachedMeasurementsNotDefined(LithoLayoutResult node) {
    assertThat(node.areCachedMeasuresValid()).isFalse();
  }

  private void checkAllComponentsHaveMeasureCache(LithoLayoutResult node) {
    if (node.getNode().getTailComponent() != null
        && node.getNode().getTailComponent().canMeasure()) {
      assertCachedMeasurementsDefined(node);
    }
    int numChildren = node.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      checkAllComponentsHaveMeasureCache(node.getChildAt(i));
    }
  }

  @Test
  public void testComponentHostMoveItem() {
    ComponentContext c = mLegacyLithoViewRule.getComponentTree().getContext();
    ComponentHost hostHolder = new ComponentHost(c);

    MountItem mountItem = mock(MountItem.class);
    when(mountItem.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    MountItem mountItem1 = mock(MountItem.class);
    when(mountItem1.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    MountItem mountItem2 = mock(MountItem.class);
    when(mountItem2.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    hostHolder.mount(0, mountItem, new Rect());
    hostHolder.mount(1, mountItem1, new Rect());
    hostHolder.mount(2, mountItem2, new Rect());
    assertThat(mountItem).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(2));
    hostHolder.moveItem(mountItem, 0, 2);
    hostHolder.moveItem(mountItem2, 2, 0);
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));
    assertThat(mountItem).isEqualTo(hostHolder.getMountItemAt(2));
  }

  @Test
  public void testComponentHostMoveItemPartial() {
    ComponentContext c = mLegacyLithoViewRule.getComponentTree().getContext();
    ComponentHost hostHolder = new ComponentHost(c);

    MountItem mountItem = mock(MountItem.class);
    when(mountItem.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    MountItem mountItem1 = mock(MountItem.class);
    when(mountItem1.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    MountItem mountItem2 = mock(MountItem.class);
    when(mountItem2.getRenderTreeNode()).thenReturn(createNode(Column.create(c).build()));

    hostHolder.mount(0, mountItem, new Rect());
    hostHolder.mount(1, mountItem1, new Rect());
    hostHolder.mount(2, mountItem2, new Rect());
    assertThat(mountItem).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(2));
    hostHolder.moveItem(mountItem2, 2, 0);
    assertThat(mountItem2).isEqualTo(hostHolder.getMountItemAt(0));
    assertThat(mountItem1).isEqualTo(hostHolder.getMountItemAt(1));

    assertThat(1)
        .isEqualTo(
            ((SparseArrayCompat<MountItem>) getInternalState(hostHolder, "mScrapMountItemsArray"))
                .size());

    hostHolder.unmount(0, mountItem);

    assertThat(2)
        .isEqualTo(
            ((SparseArrayCompat<MountItem>) getInternalState(hostHolder, "mMountItems")).size());
    assertThat((Object) getInternalState(hostHolder, "mScrapMountItemsArray")).isNull();
  }

  @Test
  public void testLayoutOutputUpdateState() {
    ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component firstComponent = TestDrawableComponent.create(c).color(Color.BLACK).build();
    final Component secondComponent = TestDrawableComponent.create(c).color(Color.BLACK).build();
    final Component thirdComponent = TestDrawableComponent.create(c).color(Color.WHITE).build();

    mLegacyLithoViewRule.setRoot(firstComponent).attachToWindow();

    mLegacyLithoViewRule.setRootAndSizeSpecSync(
        firstComponent,
        SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(state, LithoRenderUnit.STATE_UNKNOWN);

    mLegacyLithoViewRule.setRoot(secondComponent);
    LayoutState secondState = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(secondState, LithoRenderUnit.STATE_UPDATED);

    mLegacyLithoViewRule.setRoot(thirdComponent);
    LayoutState thirdState = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(thirdState, LithoRenderUnit.STATE_DIRTY);
  }

  @Test
  public void testLayoutOutputUpdateStateWithBackground() {
    final Component component1 = new TestLayoutSpecBgState(false);
    final Component component2 = new TestLayoutSpecBgState(false);
    final Component component3 = new TestLayoutSpecBgState(true);

    mLegacyLithoViewRule.setRoot(component1).attachToWindow();

    mLegacyLithoViewRule.setRootAndSizeSpecSync(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(state, STATE_UNKNOWN);

    mLegacyLithoViewRule.setRoot(component2);
    LayoutState secondState = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(secondState.getMountableOutputCount()).isEqualTo(4);

    mLegacyLithoViewRule.setRoot(component3);
    LayoutState thirdState = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(thirdState.getMountableOutputCount()).isEqualTo(4);
    assertThat(getRenderUnit(thirdState.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UPDATED);
    assertThat(getRenderUnit(thirdState.getMountableOutputAt(3)).getUpdateState())
        .isEqualTo(STATE_UPDATED);
  }

  // This test covers the same case with the foreground since the code path is the same!
  @Test
  public void testLayoutOutputUpdateStateWithBackgroundInWithLayout() {
    final Component component1 = new TestLayoutSpecInnerState(false);
    final Component component2 = new TestLayoutSpecInnerState(false);
    final Component component3 = new TestLayoutSpecInnerState(true);

    mLegacyLithoViewRule.setRoot(component1).attachToWindow();

    mLegacyLithoViewRule.setRootAndSizeSpecSync(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(getRenderUnit(state.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UNKNOWN);

    mLegacyLithoViewRule.setRoot(component2);
    LayoutState secondState = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(getRenderUnit(secondState.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UNKNOWN);
    assertThat(getRenderUnit(secondState.getMountableOutputAt(3)).getUpdateState())
        .isEqualTo(STATE_UPDATED);

    mLegacyLithoViewRule.setRoot(component3);
    LayoutState thirdState = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(getRenderUnit(thirdState.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UNKNOWN);
    assertThat(getRenderUnit(thirdState.getMountableOutputAt(3)).getUpdateState())
        .isEqualTo(STATE_UPDATED);
  }

  @Test
  public void testLayoutOutputUpdateStateIdClash() {
    final Component component1 = new TestLayoutWithStateIdClash(false);
    final Component component2 = new TestLayoutWithStateIdClash(true);

    mLegacyLithoViewRule.setRoot(component1).attachToWindow();

    mLegacyLithoViewRule.setRootAndSizeSpecSync(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertOutputsState(state, STATE_UNKNOWN);

    mLegacyLithoViewRule.setRoot(component2);
    LayoutState secondState = mLegacyLithoViewRule.getComponentTree().getMainThreadLayoutState();

    assertThat(6).isEqualTo(secondState.getMountableOutputCount());
    assertThat(STATE_DIRTY)
        .isEqualTo(getRenderUnit(secondState.getMountableOutputAt(0)).getUpdateState());
    assertThat(STATE_UNKNOWN)
        .isEqualTo(getRenderUnit(secondState.getMountableOutputAt(1)).getUpdateState());
    assertThat(STATE_UPDATED)
        .isEqualTo(getRenderUnit(secondState.getMountableOutputAt(2)).getUpdateState());
    assertThat(STATE_UNKNOWN)
        .isEqualTo(getRenderUnit(secondState.getMountableOutputAt(3)).getUpdateState());
    assertThat(STATE_UNKNOWN)
        .isEqualTo(getRenderUnit(secondState.getMountableOutputAt(4)).getUpdateState());
    assertThat(STATE_UPDATED)
        .isEqualTo(getRenderUnit(secondState.getMountableOutputAt(5)).getUpdateState());
  }

  @Test
  public void testDiffTreeUsedIfRootMeasureSpecsAreDifferentButChildHasSame() {
    ComponentContext c = mLegacyLithoViewRule.getComponentTree().getContext();
    final TestComponent component = TestDrawableComponent.create(c).color(BLACK).build();

    final Component layoutComponent = new TestSimpleContainerLayout2(component);

    LayoutState firstLayoutState =
        calculateLayoutStateWithDiffing(
            c,
            layoutComponent,
            makeSizeSpec(100, SizeSpec.EXACTLY),
            makeSizeSpec(100, SizeSpec.EXACTLY),
            null);

    assertThat(component.wasMeasureCalled()).isTrue();

    final TestComponent secondComponent = TestDrawableComponent.create(c).color(BLACK).build();

    final Component secondLayoutComponent = new TestSimpleContainerLayout2(secondComponent);

    calculateLayoutStateWithDiffing(
        c,
        secondLayoutComponent,
        makeSizeSpec(100, SizeSpec.EXACTLY),
        makeSizeSpec(90, SizeSpec.EXACTLY),
        firstLayoutState);

    assertThat(secondComponent.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testDiffTreeUsedIfMeasureSpecsAreSame() {
    ComponentContext c = mLegacyLithoViewRule.getComponentTree().getContext();

    final TestComponent component = TestDrawableComponent.create(c).color(BLACK).build();

    final Component layoutComponent = new TestSimpleContainerLayout(component, 0);

    LayoutState firstLayoutState =
        calculateLayoutStateWithDiffing(
            c,
            layoutComponent,
            makeSizeSpec(100, SizeSpec.EXACTLY),
            makeSizeSpec(100, SizeSpec.EXACTLY),
            null);

    assertThat(component.wasMeasureCalled()).isTrue();

    final TestComponent secondComponent = TestDrawableComponent.create(c).color(BLACK).build();

    final Component secondLayoutComponent = new TestSimpleContainerLayout(secondComponent, 0);

    calculateLayoutStateWithDiffing(
        c,
        secondLayoutComponent,
        makeSizeSpec(100, SizeSpec.EXACTLY),
        makeSizeSpec(100, SizeSpec.EXACTLY),
        firstLayoutState);

    assertThat(secondComponent.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testCachedMeasuresForNestedTreeComponentDelegateWithUndefinedSize() {
    final Component component1 = new TestNestedTreeDelegateWithUndefinedSizeLayout();
    final Component component2 = new TestNestedTreeDelegateWithUndefinedSizeLayout();

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component1,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component2,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    // The nested root measure() was called in the first layout calculation.
    TestComponent prevNestedRoot =
        (TestComponent) getRenderUnit(prevLayoutState.getMountableOutputAt(2)).getComponent();
    assertThat(prevNestedRoot.wasMeasureCalled()).isTrue();

    TestComponent nestedRoot =
        (TestComponent) getRenderUnit(layoutState.getMountableOutputAt(2)).getComponent();
    assertThat(nestedRoot.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testCachedMeasuresForNestedTreeComponentWithUndefinedSize() {
    final Component component1 = new TestUndefinedSizeLayout();
    final Component component2 = new TestUndefinedSizeLayout();

    LayoutState prevLayoutState =
        calculateLayoutStateWithDiffing(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component1,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            null);

    LayoutState layoutState =
        calculateLayoutStateWithDiffing(
            mLegacyLithoViewRule.getComponentTree().getContext(),
            component2,
            makeSizeSpec(350, SizeSpec.EXACTLY),
            makeSizeSpec(200, SizeSpec.EXACTLY),
            prevLayoutState);

    // The nested root measure() was called in the first layout calculation.
    TestComponent prevMainTreeLeaf =
        (TestComponent) getRenderUnit(prevLayoutState.getMountableOutputAt(1)).getComponent();
    assertThat(prevMainTreeLeaf.wasMeasureCalled()).isTrue();
    TestComponent prevNestedLeaf1 =
        (TestComponent) getRenderUnit(prevLayoutState.getMountableOutputAt(3)).getComponent();
    assertThat(prevNestedLeaf1.wasMeasureCalled()).isTrue();
    TestComponent prevNestedLeaf2 =
        (TestComponent) getRenderUnit(prevLayoutState.getMountableOutputAt(4)).getComponent();
    assertThat(prevNestedLeaf2.wasMeasureCalled()).isTrue();

    TestComponent mainTreeLeaf =
        (TestComponent) getRenderUnit(layoutState.getMountableOutputAt(1)).getComponent();
    assertThat(mainTreeLeaf.wasMeasureCalled()).isFalse();
    TestComponent nestedLeaf1 =
        (TestComponent) getRenderUnit(layoutState.getMountableOutputAt(3)).getComponent();
    assertThat(nestedLeaf1.wasMeasureCalled()).isFalse();
    TestComponent nestedLeaf2 =
        (TestComponent) getRenderUnit(layoutState.getMountableOutputAt(4)).getComponent();
    assertThat(nestedLeaf2.wasMeasureCalled()).isFalse();
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }

  private static LayoutState calculateLayoutState(
      ComponentContext context, Component component, int widthSpec, int heightSpec) {
    return calculate(
        context,
        component,
        null,
        new TreeState(),
        -1,
        widthSpec,
        heightSpec,
        -1,
        false,
        null,
        null);
  }

  private static LayoutState calculateLayoutStateWithDiffing(
      ComponentContext context,
      Component component,
      int widthSpec,
      int heightSpec,
      LayoutState previousLayoutState) {
    return calculate(
        context,
        component,
        null,
        new TreeState(),
        -1,
        widthSpec,
        heightSpec,
        -1,
        true,
        previousLayoutState,
        null);
  }

  private static void assertOutputsState(
      LayoutState layoutState, @LithoRenderUnit.UpdateState int state) {
    assertThat(STATE_DIRTY)
        .isEqualTo(getRenderUnit(layoutState.getMountableOutputAt(0)).getUpdateState());
    for (int i = 1; i < layoutState.getMountableOutputCount(); i++) {
      LithoRenderUnit output = getRenderUnit(layoutState.getMountableOutputAt(i));
      assertThat(state).isEqualTo(output.getUpdateState());
    }
  }

  private static void assertCachedMeasurementsDefined(LithoLayoutResult node) {
    float diffHeight = node.getDiffNode() == null ? -1 : node.getDiffNode().getLastMeasuredHeight();
    float diffWidth = node.getDiffNode() == null ? -1 : node.getDiffNode().getLastMeasuredWidth();
    assertThat(diffHeight != -1).isTrue();
    assertThat(diffWidth != -1).isTrue();
    assertThat(node.areCachedMeasuresValid()).isTrue();
  }

  private static class TestLayoutSpec extends InlineLayoutSpec {
    private final boolean mAddThirdChild;

    TestLayoutSpec(boolean addThirdChild) {
      super();
      this.mAddThirdChild = addThirdChild;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return Column.create(c)
          .child(TestDrawableComponent.create(c))
          .child(Column.create(c).child(TestDrawableComponent.create(c)))
          .child(mAddThirdChild ? TestDrawableComponent.create(c) : null)
          .build();
    }
  }

  private static RenderTreeNode createNode(final Component component) {
    LithoRenderUnit unit =
        MountSpecLithoRenderUnit.create(
            0, component, null, null, null, 0, 0, LithoRenderUnit.STATE_UNKNOWN);
    return RenderTreeNodeUtils.create(unit, new Rect(), null, null);
  }

  private static class TestSimpleContainerLayout extends InlineLayoutSpec {
    private final Component mComponent;
    private final int mHorizontalPadding;

    TestSimpleContainerLayout(Component component, int horizontalPadding) {
      super();
      mComponent = component;
      mHorizontalPadding = horizontalPadding;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c).paddingPx(HORIZONTAL, mHorizontalPadding).child(mComponent).build();
    }
  }

  private static class TestSimpleContainerLayout2 extends InlineLayoutSpec {
    private final Component mComponent;

    TestSimpleContainerLayout2(Component component) {
      super();
      mComponent = component;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .alignItems(FLEX_START)
          .child(Wrapper.create(c).delegate(mComponent).heightPx(50))
          .build();
    }
  }

  private static class TestLayoutSpecInnerState extends InlineLayoutSpec {
    private final boolean mChangeChildDrawable;

    TestLayoutSpecInnerState(boolean changeChildDrawable) {
      super();
      mChangeChildDrawable = changeChildDrawable;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .background(sRedDrawable)
          .foregroundRes(btn_default)
          .child(
              TestDrawableComponent.create(c)
                  .background(mChangeChildDrawable ? sRedDrawable : sBlackDrawable))
          .child(create(c).child(TestDrawableComponent.create(c)))
          .build();
    }
  }

  private static class TestLayoutSpecBgState extends InlineLayoutSpec {
    private final boolean mChangeBg;

    TestLayoutSpecBgState(boolean changeBg) {
      super();
      mChangeBg = changeBg;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .background(mChangeBg ? sBlackDrawable : sRedDrawable)
          .foreground(sTransparentDrawable)
          .child(TestDrawableComponent.create(c))
          .child(create(c).child(TestDrawableComponent.create(c)))
          .build();
    }
  }

  private static class TestUndefinedSizeLayout extends InlineLayoutSpec {
    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .paddingPx(ALL, 2)
          .child(TestDrawableComponent.create(c, true, true, false))
          .child(
              TestSizeDependentComponent.create(c)
                  .setDelegate(false)
                  .flexShrink(0)
                  .marginPx(ALL, 11))
          .build();
    }
  }

  private static class TestNestedTreeDelegateWithUndefinedSizeLayout extends InlineLayoutSpec {
    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .paddingPx(ALL, 2)
          .child(TestSizeDependentComponent.create(c).setDelegate(true).marginPx(ALL, 11))
          .build();
    }
  }

  private static class TestLayoutWithStateIdClash extends InlineLayoutSpec {
    private final boolean mAddChild;

    TestLayoutWithStateIdClash(boolean addChild) {
      super();
      mAddChild = addChild;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .child(
              create(c)
                  .wrapInView()
                  .child(TestDrawableComponent.create(c))
                  .child(mAddChild ? TestDrawableComponent.create(c) : null))
          .child(create(c).wrapInView().child(TestDrawableComponent.create(c)))
          .build();
    }
  }
}
