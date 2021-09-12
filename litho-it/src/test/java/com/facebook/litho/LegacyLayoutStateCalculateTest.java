/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.graphics.Color.RED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.NodeInfo.SELECTED_SET_TRUE;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.yoga.YogaAlign.CENTER;
import static com.facebook.yoga.YogaAlign.FLEX_START;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.HORIZONTAL;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.TOP;
import static com.facebook.yoga.YogaJustify.SPACE_AROUND;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Color;
import android.graphics.Rect;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.Nullable;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestSizeDependentComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SolidColor;
import com.facebook.litho.widget.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAccessibilityManager;

@RunWith(LithoTestRunner.class)
public class LegacyLayoutStateCalculateTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  private ComponentContext mContext;

  @Before
  public void setup() throws Exception {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false);
    // invdalidate the cached accessibility value before each test runs so that we don't
    // have a value already cached.  If we don't do this, accessibility tests will fail when run
    // after non-accessibility tests, and vice-versa.
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled();
    mContext = mLithoViewRule.getComponentTree().getContext();
  }

  @Test
  public void testLayoutOutputsWithImportantForAccessibility() {
    enableAccessibility();

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .contentDescription("This is root view")
                .child(TestDrawableComponent.create(c).widthDip(30).heightDip(30))
                .child(
                    TestDrawableComponent.create(c, true, true, true)
                        .flex(1)
                        .flexBasisDip(0)
                        .backgroundColor(RED)
                        .marginDip(HORIZONTAL, 10)
                        .importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO))
                .child(
                    Row.create(c)
                        .alignItems(CENTER)
                        .paddingDip(ALL, 10)
                        .importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
                        .child(
                            TestDrawableComponent.create(c)
                                .widthDip(30)
                                .heightDip(30)
                                .importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
                                .contentDescription("This is an image")))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(7);

    final long hostMarkerRoot =
        getLayoutOutput(layoutState.getMountableOutputAt(1)).getHostMarker();
    final long hostMarkerOne = getLayoutOutput(layoutState.getMountableOutputAt(5)).getHostMarker();
    final long hostMarkerTwo = getLayoutOutput(layoutState.getMountableOutputAt(6)).getHostMarker();

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(1)).getHostMarker())
        .isEqualTo(hostMarkerRoot);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(2)).getHostMarker())
        .isEqualTo(hostMarkerRoot);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(3)).getHostMarker())
        .isEqualTo(hostMarkerRoot);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(4)).getHostMarker())
        .isEqualTo(hostMarkerRoot);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(5)).getHostMarker())
        .isEqualTo(hostMarkerOne);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(6)).getHostMarker())
        .isEqualTo(hostMarkerTwo);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(DrawableComponent.class);
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 4))).isTrue();
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue();
    assertThat(getComponentAt(layoutState, 6)).isInstanceOf(TestDrawableComponent.class);

    assertThat(IMPORTANT_FOR_ACCESSIBILITY_AUTO)
        .isEqualTo(
            getLayoutOutput(layoutState.getMountableOutputAt(0)).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_AUTO)
        .isEqualTo(
            getLayoutOutput(layoutState.getMountableOutputAt(1)).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_NO)
        .isEqualTo(
            getLayoutOutput(layoutState.getMountableOutputAt(2)).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_NO)
        .isEqualTo(
            getLayoutOutput(layoutState.getMountableOutputAt(3)).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
        .isEqualTo(
            getLayoutOutput(layoutState.getMountableOutputAt(4)).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_YES)
        .isEqualTo(
            getLayoutOutput(layoutState.getMountableOutputAt(5)).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_YES)
        .isEqualTo(
            getLayoutOutput(layoutState.getMountableOutputAt(6)).getImportantForAccessibility());
  }

  @Test
  public void testLayoutOutputsForLongClickHandlerAndViewTagsOnRoot() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .longClickHandler(c.newEventHandler(1))
                .viewTags(new SparseArray<>())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
    final long hostMarkerZero =
        getLayoutOutput(layoutState.getMountableOutputAt(0)).getHostMarker();

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(1)).getHostMarker())
        .isEqualTo(hostMarkerZero);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);

    final NodeInfo nodeInfo = getLayoutOutput(layoutState.getMountableOutputAt(0)).getNodeInfo();
    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getLongClickHandler()).isNotNull();
    assertThat(nodeInfo.getViewTags()).isNotNull();
  }

  @Test
  public void testComponentsLoggerCanReturnNullPerfEventsDuringLayout() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).child(TestDrawableComponent.create(c)).wrapInView().build();
          }
        };

    final ComponentsLogger logger =
        new TestComponentsLogger() {
          @Override
          public @Nullable PerfEvent newPerformanceEvent(ComponentContext c, int eventId) {
            return null;
          }
        };

    final LayoutState layoutState =
        LayoutState.calculate(
            ComponentTree.create(mContext).logger(logger, "test").build().getContext(),
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY),
            LayoutState.CalculateLayoutSource.TEST);

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
  }

  @Test
  public void testLayoutOutputRootWithPaddingOverridingDelegateNestedTreeComponent() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            final Component nestedTreeRootComponent =
                TestSizeDependentComponent.create(c).setFixSizes(true).setDelegate(false).build();

            return Wrapper.create(c).delegate(nestedTreeRootComponent).paddingPx(ALL, 10).build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);
    Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    mountBounds = layoutState.getMountableOutputAt(0).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(0)).getHostMarker()).isEqualTo(0);
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(1).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(10, 10, 60, 60));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(2).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(10, 10, 60, 60));
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent.class);
    mountBounds = layoutState.getMountableOutputAt(3).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(13, 63, 337, 83));
  }

  @Test
  public void testLayoutOutputsForDeepLayoutSpecsWithBackground() {
    final int paddingSize = 5;
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .backgroundColor(0xFFFF0000)
                .child(
                    Row.create(c)
                        .justifyContent(SPACE_AROUND)
                        .alignItems(CENTER)
                        .positionType(ABSOLUTE)
                        .positionPx(LEFT, 50)
                        .positionPx(TOP, 50)
                        .positionPx(RIGHT, 200)
                        .positionPx(BOTTOM, 50)
                        .child(Text.create(c).text("textLeft1"))
                        .child(Text.create(c).text("textRight1"))
                        .backgroundColor(0xFFFF0000)
                        .foregroundColor(0xFFFF0000)
                        .paddingPx(ALL, paddingSize)
                        .wrapInView())
                .child(
                    Row.create(c)
                        .justifyContent(SPACE_AROUND)
                        .alignItems(CENTER)
                        .positionType(ABSOLUTE)
                        .positionPx(LEFT, 200)
                        .positionPx(TOP, 50)
                        .positionPx(RIGHT, 50)
                        .positionPx(BOTTOM, 50)
                        .child(
                            Text.create(c)
                                .text("textLeft2")
                                .wrapInView()
                                .backgroundColor(0xFFFF0000)
                                .paddingPx(ALL, paddingSize))
                        .child(
                            TestViewComponent.create(c)
                                .backgroundColor(0xFFFF0000)
                                .foregroundColor(0x0000FFFF)
                                .paddingPx(ALL, paddingSize)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Account for Android version in the foreground. If >= M the foreground is part of the
    // ViewLayoutOutput otherwise it has its own LayoutOutput.
    final boolean foregroundHasOwnOutput = SDK_INT < M;

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(foregroundHasOwnOutput ? 12 : 11);

    // Check quantity of HostComponents.
    int totalHosts = 0;
    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      final Component childComponent = getComponentAt(layoutState, i);
      if (isHostComponent(childComponent)) {
        totalHosts++;
      }
    }
    assertThat(totalHosts).isEqualTo(3);

    // Check all the Layouts are in the correct position.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 2))).isTrue();
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(DrawableComponent.class);
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(Text.class);
    assertThat(getComponentAt(layoutState, 5)).isInstanceOf(Text.class);
    assertThat(getComponentAt(layoutState, 6)).isInstanceOf(DrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 7))).isTrue();
    assertThat(getComponentAt(layoutState, 8)).isInstanceOf(DrawableComponent.class);
    assertThat(getComponentAt(layoutState, 9)).isInstanceOf(Text.class);
    assertThat(getComponentAt(layoutState, 10)).isInstanceOf(TestViewComponent.class);
    if (foregroundHasOwnOutput) {
      assertThat(getComponentAt(layoutState, 11)).isInstanceOf(DrawableComponent.class);
    }

    // Check the text within the TextComponents.
    assertThat(getTextFromTextComponent(layoutState, 4)).isEqualTo("textLeft1");
    assertThat(getTextFromTextComponent(layoutState, 5)).isEqualTo("textRight1");
    assertThat(getTextFromTextComponent(layoutState, 9)).isEqualTo("textLeft2");

    // Check that the backgrounds have the same size of the components to which they are associated
    assertThat(layoutState.getMountableOutputAt(3).getAbsoluteBounds(new Rect()))
        .isEqualTo(layoutState.getMountableOutputAt(2).getAbsoluteBounds(new Rect()));
    assertThat(layoutState.getMountableOutputAt(6).getAbsoluteBounds(new Rect()))
        .isEqualTo(layoutState.getMountableOutputAt(2).getAbsoluteBounds(new Rect()));

    final Rect textLayoutBounds = layoutState.getMountableOutputAt(9).getAbsoluteBounds(new Rect());
    final Rect textBackgroundBounds =
        layoutState.getMountableOutputAt(8).getAbsoluteBounds(new Rect());

    assertThat(textLayoutBounds.left - paddingSize).isEqualTo(textBackgroundBounds.left);
    assertThat(textLayoutBounds.top - paddingSize).isEqualTo(textBackgroundBounds.top);
    assertThat(textLayoutBounds.right + paddingSize).isEqualTo(textBackgroundBounds.right);
    assertThat(textLayoutBounds.bottom + paddingSize).isEqualTo(textBackgroundBounds.bottom);

    assertThat(layoutState.getMountableOutputAt(8).getAbsoluteBounds(new Rect()))
        .isEqualTo(layoutState.getMountableOutputAt(7).getAbsoluteBounds(new Rect()));

    final ViewNodeInfo viewNodeInfo =
        getLayoutOutput(layoutState.getMountableOutputAt(10)).getViewNodeInfo();
    assertThat(viewNodeInfo).isNotNull();
    assertThat(viewNodeInfo.getBackground() != null).isTrue();
    if (foregroundHasOwnOutput) {
      assertThat(viewNodeInfo.getForeground() == null).isTrue();
    } else {
      assertThat(viewNodeInfo.getForeground() != null).isTrue();
    }
    assertThat(viewNodeInfo.getPaddingLeft() == paddingSize).isTrue();
    assertThat(viewNodeInfo.getPaddingTop() == paddingSize).isTrue();
    assertThat(viewNodeInfo.getPaddingRight() == paddingSize).isTrue();
    assertThat(viewNodeInfo.getPaddingBottom() == paddingSize).isTrue();
  }

  @Test
  public void testLayoutOutputForDelegateNestedTreeComponent() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .paddingPx(ALL, 2)
                .child(
                    TestSizeDependentComponent.create(c)
                        .setFixSizes(true)
                        .setDelegate(false)
                        .marginPx(ALL, 11))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);
    Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    mountBounds = layoutState.getMountableOutputAt(0).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(0)).getHostMarker()).isEqualTo(0);
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(1).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(18, 18, 68, 68));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(2).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(18, 18, 68, 68));
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent.class);
    mountBounds = layoutState.getMountableOutputAt(3).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(21, 71, 329, 91));
  }

  @Test
  public void testLayoutOutputForRootNestedTreeComponent() {
    final LayoutState layoutState =
        calculateLayoutState(
            mContext,
            TestSizeDependentComponent.create(new ComponentContext(getApplicationContext()))
                .setFixSizes(true)
                .setDelegate(false)
                .build(),
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);
    Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    mountBounds = layoutState.getMountableOutputAt(0).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(0)).getHostMarker()).isEqualTo(0);
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(1).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(5, 5, 55, 55));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(2).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(5, 5, 55, 55));
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent.class);
    mountBounds = layoutState.getMountableOutputAt(3).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(8, 58, 342, 78));
  }

  @Test
  public void testLayoutOutputsForRootInteractiveLayoutSpecs() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).child(TestDrawableComponent.create(c)).wrapInView().build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
  }

  @Test
  public void testLayoutOutputsForAccessibilityEnabled() {
    enableAccessibility();

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Row.create(c)
                .alignItems(CENTER)
                .paddingDip(ALL, 10)
                .contentDescription("This is root view")
                .child(TestDrawableComponent.create(c).widthDip(30).heightDip(30))
                .child(
                    TestDrawableComponent.create(c, true, true, true)
                        .flex(1)
                        .flexBasisDip(0)
                        .backgroundColor(RED)
                        .marginDip(HORIZONTAL, 10))
                .child(
                    Row.create(c)
                        .alignItems(CENTER)
                        .paddingDip(ALL, 10)
                        .contentDescription("This is a container")
                        .child(
                            TestDrawableComponent.create(c)
                                .widthDip(30)
                                .heightDip(30)
                                .contentDescription("This is an image"))
                        .child(
                            TestDrawableComponent.create(c, true, true, true)
                                .flex(1)
                                .flexBasisDip(0)
                                .marginDip(HORIZONTAL, 10)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(10);

    final long hostMarkerRoot =
        getLayoutOutput(layoutState.getMountableOutputAt(1)).getHostMarker();
    final long hostMarkerOne = getLayoutOutput(layoutState.getMountableOutputAt(3)).getHostMarker();
    final long hostMarkerTwo = getLayoutOutput(layoutState.getMountableOutputAt(6)).getHostMarker();
    final long hostMarkerThree =
        getLayoutOutput(layoutState.getMountableOutputAt(7)).getHostMarker();
    final long hostMarkerFour =
        getLayoutOutput(layoutState.getMountableOutputAt(9)).getHostMarker();

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(1)).getHostMarker())
        .isEqualTo(hostMarkerRoot);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(3)).getHostMarker())
        .isEqualTo(hostMarkerOne);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(4)).getHostMarker())
        .isEqualTo(hostMarkerOne);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(6)).getHostMarker())
        .isEqualTo(hostMarkerTwo);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(7)).getHostMarker())
        .isEqualTo(hostMarkerThree);
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(9)).getHostMarker())
        .isEqualTo(hostMarkerFour);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 2))).isTrue();
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(DrawableComponent.class);
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue();
    assertThat(isHostComponent(getComponentAt(layoutState, 6))).isTrue();
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 8))).isTrue();
    assertThat(getComponentAt(layoutState, 9)).isInstanceOf(TestDrawableComponent.class);
  }

  @Test
  public void testLayoutOutputsForSelectedOnRoot() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).child(TestDrawableComponent.create(c)).selected(true).build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
    final long hostMarkerZero =
        getLayoutOutput(layoutState.getMountableOutputAt(0)).getHostMarker();

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(1)).getHostMarker())
        .isEqualTo(hostMarkerZero);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);

    assertThat(
            getLayoutOutput(layoutState.getMountableOutputAt(0)).getNodeInfo().getSelectedState())
        .isEqualTo(SELECTED_SET_TRUE);
  }

  @Test
  public void testLayoutOutputsForFocusableOnRoot() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).child(TestDrawableComponent.create(c)).focusable(true).build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
    final long hostMarkerZero =
        getLayoutOutput(layoutState.getMountableOutputAt(0)).getHostMarker();

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(1)).getHostMarker())
        .isEqualTo(hostMarkerZero);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(0)).getNodeInfo().getFocusState())
        .isEqualTo(FOCUS_SET_TRUE);
  }

  @Test
  public void testLayoutOutputForRootNestedTreeComponentWithPercentParentSizeDefined() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .alignItems(FLEX_START)
                .widthPx(100)
                .heightPx(100)
                .child(
                    TestSizeDependentComponent.create(c)
                        .widthPercent(50)
                        .heightPercent(50)
                        .backgroundColor(0xFFFF0000))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED));

    Rect mountBounds = new Rect();
    mountBounds = layoutState.getMountableOutputAt(0).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 100, 100));

    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(1).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 50, 50));
  }

  @Test
  public void testLayoutOutputForRootWithDelegateNestedTreeComponent() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return TestSizeDependentComponent.create(c)
                .setFixSizes(true)
                .setDelegate(false)
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);
    Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    mountBounds = layoutState.getMountableOutputAt(0).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(0)).getHostMarker()).isEqualTo(0);
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(1).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(5, 5, 55, 55));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(2).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(5, 5, 55, 55));
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent.class);
    mountBounds = layoutState.getMountableOutputAt(3).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(8, 58, 342, 78));
  }

  @Test
  public void testLayoutOutputForDelegateNestedTreeComponentDelegate() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .paddingPx(ALL, 2)
                .child(
                    TestSizeDependentComponent.create(c)
                        .setFixSizes(true)
                        .setDelegate(true)
                        .marginPx(ALL, 11))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);
    Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    mountBounds = layoutState.getMountableOutputAt(0).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(1).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(13, 13, 63, 63));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(2).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(13, 13, 63, 63));
  }

  @Test
  public void testLayoutOutputsForComponentWithBackgrounds() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .backgroundColor(0xFFFF0000)
                .foregroundColor(0xFFFF0000)
                .child(TestDrawableComponent.create(c))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);

    // First and third output are the background and the foreground
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(DrawableComponent.class);
  }

  @Test
  public void onMountItemUpdatesImplementVirtualViews_ComponentHostShouldAlsoUpdate() {
    enableAccessibility();

    mLithoViewRule
        .setRoot(Text.create(mContext).text("hello world").build())
        .attachToWindow()
        .measure()
        .layout();

    assertThat(mLithoViewRule.getLithoView().implementsVirtualViews())
        .describedAs("The parent output of the Text must implement virtual views")
        .isTrue();

    mLithoViewRule
        .setRootAndSizeSpec(
            SolidColor.create(mContext).color(Color.BLACK).build(),
            SizeSpec.makeSizeSpec(100, EXACTLY),
            SizeSpec.makeSizeSpec(100, EXACTLY))
        .measure()
        .layout();

    assertThat(mLithoViewRule.getLithoView().implementsVirtualViews())
        .describedAs("The parent output of the drawable must not implement virtual views")
        .isFalse();

    mLithoViewRule
        .setRootAndSizeSpec(
            Column.create(mContext)
                .child(Text.create(mContext).text("hello world").build())
                .child(SolidColor.create(mContext).color(Color.BLACK).build())
                .build(),
            SizeSpec.makeSizeSpec(100, EXACTLY),
            SizeSpec.makeSizeSpec(200, EXACTLY))
        .attachToWindow()
        .measure()
        .layout();

    assertThat(mLithoViewRule.getLithoView().implementsVirtualViews())
        .describedAs("The root output must not implement virtual views")
        .isFalse();

    final ComponentHost host = (ComponentHost) mLithoViewRule.getLithoView().getChildAt(0);
    assertThat(host.implementsVirtualViews())
        .describedAs("The parent output of the Text must implement virtual views")
        .isTrue();
  }

  @Test
  public void testLayoutOutputMountBounds() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .widthPx(30)
                .heightPx(30)
                .wrapInView()
                .child(
                    create(c)
                        .widthPx(10)
                        .heightPx(10)
                        .marginPx(ALL, 10)
                        .wrapInView()
                        .child(TestDrawableComponent.create(c).widthPx(10).heightPx(10)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    Rect mountBounds = new Rect();

    mountBounds = layoutState.getMountableOutputAt(0).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 30, 30));

    mountBounds = layoutState.getMountableOutputAt(1).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(10, 10, 20, 20));

    mountBounds = layoutState.getMountableOutputAt(2).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 10, 10));
  }

  @Test
  public void testLayoutOutputForRootNestedTreeComponentWithPercent() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .alignItems(FLEX_START)
                .child(
                    TestSizeDependentComponent.create(c)
                        .setFixSizes(true)
                        .widthPercent(50)
                        .heightPercent(50)
                        .backgroundColor(0xFFFF0000))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED));

    Rect mountBounds = new Rect();
    mountBounds = layoutState.getMountableOutputAt(0).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 60, 86));

    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    mountBounds = layoutState.getMountableOutputAt(1).getBounds();
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 60, 86));
  }

  @Test
  public void whenAccessibleChildNodeExists_ParentNodeShouldImplementVirtualViews() {
    enableAccessibility();

    final Component component = Text.create(mContext).text("hello world").build();

    mLithoViewRule.setRoot(component).attachToWindow().measure().layout();

    assertThat(mLithoViewRule.getLithoView().implementsVirtualViews())
        .describedAs("The parent output of the Text must implement virtual views")
        .isTrue();
  }

  @Test
  public void testLayoutOutputsForDeepLayoutSpecs() {
    final int paddingSize = 5;
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .backgroundColor(0xFFFF0000)
                .child(
                    Row.create(c)
                        .justifyContent(SPACE_AROUND)
                        .alignItems(CENTER)
                        .positionType(ABSOLUTE)
                        .positionPx(LEFT, 50)
                        .positionPx(TOP, 50)
                        .positionPx(RIGHT, 200)
                        .positionPx(BOTTOM, 50)
                        .child(Text.create(c).text("textLeft1"))
                        .child(Text.create(c).text("textRight1"))
                        .paddingPx(ALL, paddingSize)
                        .wrapInView())
                .child(
                    Row.create(c)
                        .justifyContent(SPACE_AROUND)
                        .alignItems(CENTER)
                        .positionType(ABSOLUTE)
                        .positionPx(LEFT, 200)
                        .positionPx(TOP, 50)
                        .positionPx(RIGHT, 50)
                        .positionPx(BOTTOM, 50)
                        .child(
                            Text.create(c)
                                .text("textLeft2")
                                .wrapInView()
                                .paddingPx(ALL, paddingSize))
                        .child(TestViewComponent.create(c).wrapInView()))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));
    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(8);

    // Check quantity of HostComponents.
    int totalHosts = 0;
    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      final Component childComponent = getComponentAt(layoutState, i);
      if (isHostComponent(childComponent)) {
        totalHosts++;
      }
    }
    assertThat(totalHosts).isEqualTo(3);

    // Check all the Layouts are in the correct position.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 2))).isTrue();
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(Text.class);
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(Text.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue();
    assertThat(getComponentAt(layoutState, 6)).isInstanceOf(Text.class);
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestViewComponent.class);

    // Check the text within the TextComponents.
    assertThat(getTextFromTextComponent(layoutState, 3)).isEqualTo("textLeft1");
    assertThat(getTextFromTextComponent(layoutState, 4)).isEqualTo("textRight1");
    assertThat(getTextFromTextComponent(layoutState, 6)).isEqualTo("textLeft2");

    final Rect textLayoutBounds = layoutState.getMountableOutputAt(6).getAbsoluteBounds(new Rect());
    final Rect textBackgroundBounds =
        layoutState.getMountableOutputAt(5).getAbsoluteBounds(new Rect());

    assertThat(textLayoutBounds.left - paddingSize).isEqualTo(textBackgroundBounds.left);
    assertThat(textLayoutBounds.top - paddingSize).isEqualTo(textBackgroundBounds.top);
    assertThat(textLayoutBounds.right + paddingSize).isEqualTo(textBackgroundBounds.right);
    assertThat(textLayoutBounds.bottom + paddingSize).isEqualTo(textBackgroundBounds.bottom);
  }

  @Test
  public void testLayoutOutputsForClickHandlerAndViewTagsOnRoot() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .clickHandler(c.newEventHandler(1))
                .viewTags(new SparseArray<>())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            mContext, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
    final long hostMarkerZero =
        getLayoutOutput(layoutState.getMountableOutputAt(0)).getHostMarker();

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(1)).getHostMarker())
        .isEqualTo(hostMarkerZero);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);

    final NodeInfo nodeInfo = getLayoutOutput(layoutState.getMountableOutputAt(0)).getNodeInfo();

    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getClickHandler()).isNotNull();
    assertThat(nodeInfo.getViewTags()).isNotNull();
  }

  private void enableAccessibility() {
    final ShadowAccessibilityManager manager =
        Shadows.shadowOf(
            (AccessibilityManager) getApplicationContext().getSystemService(ACCESSIBILITY_SERVICE));
    manager.setEnabled(true);
    manager.setTouchExplorationEnabled(true);
  }

  private static CharSequence getTextFromTextComponent(
      final LayoutState layoutState, final int index) {
    return Whitebox.getInternalState(
        getLayoutOutput(layoutState.getMountableOutputAt(index)).getComponent(), "text");
  }

  private static boolean isHostComponent(final Component component) {
    return component instanceof HostComponent;
  }

  private static Component getComponentAt(final LayoutState layoutState, final int index) {
    return getLayoutOutput(layoutState.getMountableOutputAt(index)).getComponent();
  }

  private LayoutState calculateLayoutState(
      final ComponentContext context,
      final Component component,
      final int componentTreeId,
      final int widthSpec,
      final int heightSpec) {

    return LayoutState.calculate(
        context,
        component,
        componentTreeId,
        widthSpec,
        heightSpec,
        LayoutState.CalculateLayoutSource.TEST);
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
