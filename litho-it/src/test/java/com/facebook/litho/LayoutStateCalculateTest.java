/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
import static androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutState.createAndMeasureTreeForComponent;
import static com.facebook.litho.NodeInfo.ENABLED_SET_FALSE;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.litho.NodeInfo.SELECTED_SET_TRUE;
import static com.facebook.litho.SizeSpec.AT_MOST;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.Nullable;
import com.facebook.litho.LayoutState.LayoutStateReferenceWrapper;
import com.facebook.litho.drawable.DefaultComparableDrawable;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.TestNullLayoutComponent;
import com.facebook.litho.testing.TestSizeDependentComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.logging.TestComponentsLogger;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAccessibilityManager;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculateTest {

  @Before
  public void setup() throws Exception {
    // invdalidate the cached accessibility value before each test runs so that we don't
    // have a value already cached.  If we don't do this, accessibility tests will fail when run
    // after non-accessibility tests, and vice-versa.
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled();
  }

  @After
  public void validate() {
    validateMockitoUsage();
  }

  @Test
  public void testNoUnnecessaryLayoutOutputsForLayoutSpecs() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Column.create(c)
                .child(Column.create(c).child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            RuntimeEnvironment.application,
            component,
            -1,
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
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
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
  }

  @Test
  public void testLayoutOutputsForSpecsWithTouchExpansion() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c).widthPx(100).heightPx(10))
                .child(
                    Row.create(c)
                        .viewTag(new Object())
                        .child(TestDrawableComponent.create(c).widthPx(20).heightPx(90))
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .clickHandler(c.newEventHandler(1))
                                .widthPx(50)
                                .heightPx(50)
                                .touchExpansionPx(YogaEdge.ALL, 5)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(6);

    final ViewNodeInfo viewNodeInfo = layoutState.getMountableOutputAt(4).getViewNodeInfo();
    assertThat(viewNodeInfo.getExpandedTouchBounds()).isEqualTo(new Rect(15, -5, 75, 55));

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(4).getNodeInfo();
    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getClickHandler()).isNotNull();
    assertThat(nodeInfo.getLongClickHandler()).isNull();
    assertThat(nodeInfo.getFocusChangeHandler()).isNull();
    assertThat(nodeInfo.getTouchHandler()).isNull();
  }

  @Test
  public void testLayoutOutputsForSpecsWithClickHandling() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .clickHandler(c.newEventHandler(1)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(1).getNodeInfo();
    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getClickHandler()).isNotNull();
    assertThat(nodeInfo.getLongClickHandler()).isNull();
    assertThat(nodeInfo.getFocusChangeHandler()).isNull();
    assertThat(nodeInfo.getTouchHandler()).isNull();
  }

  @Test
  public void testLayoutOutputsForSpecsWithLongClickHandling() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .longClickHandler(c.newEventHandler(1)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(1).getNodeInfo();
    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getClickHandler()).isNull();
    assertThat(nodeInfo.getLongClickHandler()).isNotNull();
    assertThat(nodeInfo.getFocusChangeHandler()).isNull();
    assertThat(nodeInfo.getTouchHandler()).isNull();
  }

  @Test
  public void testLayoutOutputsForSpecsWithFocusChangeHandling() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .focusChangeHandler(c.newEventHandler(1)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(1).getNodeInfo();
    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getClickHandler()).isNull();
    assertThat(nodeInfo.getLongClickHandler()).isNull();
    assertThat(nodeInfo.getFocusChangeHandler()).isNotNull();
    assertThat(nodeInfo.getTouchHandler()).isNull();
  }

  @Test
  public void testLayoutOutputsForSpecsWithInterceptTouchHandling() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .interceptTouchHandler(c.newEventHandler(1)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(1).getNodeInfo();
    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getClickHandler()).isNull();
    assertThat(nodeInfo.getLongClickHandler()).isNull();
    assertThat(nodeInfo.getInterceptTouchHandler()).isNotNull();
    assertThat(nodeInfo.getTouchHandler()).isNull();
  }

  @Test
  public void testLayoutOutputsForSpecsWithTouchHandling() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .touchHandler(c.newEventHandler(1)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(1).getNodeInfo();
    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getTouchHandler()).isNotNull();
    assertThat(nodeInfo.getClickHandler()).isNull();
    assertThat(nodeInfo.getLongClickHandler()).isNull();
    assertThat(nodeInfo.getFocusChangeHandler()).isNull();
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
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));
    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(8);

    // Check quantity of HostComponents.
    int totalHosts = 0;
    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      final ComponentLifecycle lifecycle = getComponentAt(layoutState, i);
      if (isHostComponent(lifecycle)) {
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

    final Rect textLayoutBounds = layoutState.getMountableOutputAt(6).getBounds();
    final Rect textBackgroundBounds = layoutState.getMountableOutputAt(5).getBounds();

    assertThat(textLayoutBounds.left - paddingSize).isEqualTo(textBackgroundBounds.left);
    assertThat(textLayoutBounds.top - paddingSize).isEqualTo(textBackgroundBounds.top);
    assertThat(textLayoutBounds.right + paddingSize).isEqualTo(textBackgroundBounds.right);
    assertThat(textLayoutBounds.bottom + paddingSize).isEqualTo(textBackgroundBounds.bottom);
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
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    final Rect mountBounds = new Rect();

    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 30, 30));

    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(10, 10, 20, 20));

    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 10, 10));
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
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Account for Android version in the foreground. If >= M the foreground is part of the
    // ViewLayoutOutput otherwise it has its own LayoutOutput.
    final boolean foregroundHasOwnOutput = SDK_INT < M;

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(foregroundHasOwnOutput ? 12 : 11);

    // Check quantity of HostComponents.
    int totalHosts = 0;
    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      final ComponentLifecycle lifecycle = getComponentAt(layoutState, i);
      if (isHostComponent(lifecycle)) {
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
    assertThat(layoutState.getMountableOutputAt(3).getBounds())
        .isEqualTo(layoutState.getMountableOutputAt(2).getBounds());
    assertThat(layoutState.getMountableOutputAt(6).getBounds())
        .isEqualTo(layoutState.getMountableOutputAt(2).getBounds());

    final Rect textLayoutBounds = layoutState.getMountableOutputAt(9).getBounds();
    final Rect textBackgroundBounds = layoutState.getMountableOutputAt(8).getBounds();

    assertThat(textLayoutBounds.left - paddingSize).isEqualTo(textBackgroundBounds.left);
    assertThat(textLayoutBounds.top - paddingSize).isEqualTo(textBackgroundBounds.top);
    assertThat(textLayoutBounds.right + paddingSize).isEqualTo(textBackgroundBounds.right);
    assertThat(textLayoutBounds.bottom + paddingSize).isEqualTo(textBackgroundBounds.bottom);

    assertThat(layoutState.getMountableOutputAt(8).getBounds())
        .isEqualTo(layoutState.getMountableOutputAt(7).getBounds());

    final ViewNodeInfo viewNodeInfo = layoutState.getMountableOutputAt(10).getViewNodeInfo();
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
  public void testLayoutOutputsForMegaDeepLayoutSpecs() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c))
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c).wrapInView())
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .child(TestDrawableComponent.create(c))
                                .wrapInView())
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .child(TestDrawableComponent.create(c)))
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .child(TestViewComponent.create(c)))
                        .wrapInView())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(18);

    // Check quantity of HostComponents.
    int totalHosts = 0;
    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      final ComponentLifecycle lifecycle = getComponentAt(layoutState, i);
      if (isHostComponent(lifecycle)) {
        totalHosts++;
      }
    }
    assertThat(totalHosts).isEqualTo(7);

    // Check all the Components match the right LayoutOutput positions.
    // Tree One.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue();
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestDrawableComponent.class);

    // Tree Two.
    assertThat(isHostComponent(getComponentAt(layoutState, 4))).isTrue();
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue();
    assertThat(getComponentAt(layoutState, 6)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 7))).isTrue();
    assertThat(getComponentAt(layoutState, 8)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 9)).isInstanceOf(TestDrawableComponent.class);

    // Tree Three.
    assertThat(isHostComponent(getComponentAt(layoutState, 10))).isTrue();
    assertThat(getComponentAt(layoutState, 11)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 12)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 13)).isInstanceOf(TestDrawableComponent.class);

    // Tree Four.
    assertThat(isHostComponent(getComponentAt(layoutState, 14))).isTrue();
    assertThat(getComponentAt(layoutState, 15)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 16)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 17)).isInstanceOf(TestViewComponent.class);
  }

  @Test
  public void testLayoutOutputStableIds() {
    final Component component1 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(create(c).child(TestDrawableComponent.create(c)).contentDescription("cd0"))
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c))
                        .contentDescription("cd1"))
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestViewComponent.create(c))
                        .contentDescription("cd2"))
                .build();
          }
        };
    final Component component2 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(create(c).child(TestDrawableComponent.create(c)).contentDescription("cd0"))
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c))
                        .contentDescription("cd1"))
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestViewComponent.create(c))
                        .contentDescription("cd2"))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component1, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(20, EXACTLY));

    final LayoutState sameComponentLayoutState =
        calculateLayoutState(
            application, component2, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(20, EXACTLY));

    assertThat(sameComponentLayoutState.getMountableOutputCount())
        .isEqualTo(layoutState.getMountableOutputCount());

    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      assertThat(sameComponentLayoutState.getMountableOutputAt(i).getId())
          .isEqualTo(layoutState.getMountableOutputAt(i).getId());
    }
  }

  @Test
  public void testLayoutOutputStableIdsForMegaDeepComponent() {
    final Component component1 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c))
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c).wrapInView())
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .child(TestDrawableComponent.create(c))
                                .wrapInView())
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .child(TestDrawableComponent.create(c)))
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .child(TestViewComponent.create(c)))
                        .wrapInView())
                .build();
          }
        };

    final Component component2 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c))
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c).wrapInView())
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .child(TestDrawableComponent.create(c))
                                .wrapInView())
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .child(TestDrawableComponent.create(c)))
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(
                            create(c)
                                .child(TestDrawableComponent.create(c))
                                .child(TestViewComponent.create(c)))
                        .wrapInView())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component1, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(20, EXACTLY));

    final LayoutState sameComponentLayoutState =
        calculateLayoutState(
            application, component2, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(20, EXACTLY));
    assertThat(sameComponentLayoutState.getMountableOutputCount())
        .isEqualTo(layoutState.getMountableOutputCount());

    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      assertThat(sameComponentLayoutState.getMountableOutputAt(i).getId())
          .isEqualTo(layoutState.getMountableOutputAt(i).getId());
    }
  }

  @Test
  public void testPartiallyStableIds() {
    final Component component1 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        };
    final Component component2 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    final LayoutState layoutState1 =
        calculateLayoutState(
            application, component1, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(20, EXACTLY));

    final LayoutState layoutState2 =
        calculateLayoutState(
            application, component2, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(20, EXACTLY));

    assertThat(layoutState2.getMountableOutputAt(0).getId())
        .isEqualTo(layoutState1.getMountableOutputAt(0).getId());
    assertThat(layoutState2.getMountableOutputAt(1).getId())
        .isEqualTo(layoutState1.getMountableOutputAt(1).getId());
    assertThat(layoutState1.getMountableOutputCount()).isEqualTo(3);
    assertThat(layoutState2.getMountableOutputCount()).isEqualTo(4);
  }

  @Test
  public void testDifferentIds() {
    final Component component1 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Column.create(c)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        };
    final Component component2 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Column.create(c)
                .child(TestDrawableComponent.create(c).wrapInView())
                .child(
                    Column.create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c))
                        .wrapInView())
                .build();
          }
        };

    final LayoutState layoutState1 =
        calculateLayoutState(
            RuntimeEnvironment.application,
            component1,
            -1,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY));

    final LayoutState layoutState2 =
        calculateLayoutState(
            RuntimeEnvironment.application,
            component2,
            -1,
            SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
            SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY));

    assertThat(layoutState1.getMountableOutputAt(1).getId())
        .isNotEqualTo(layoutState2.getMountableOutputAt(1).getId());
  }

  @Test
  public void testLayoutOutputsWithInteractiveLayoutSpecAsLeafs() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .child(create(c).child(TestLayoutComponent.create(c)).wrapInView())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 2))).isTrue();
  }

  private static ComponentLifecycle getComponentAt(final LayoutState layoutState, final int index) {
    return layoutState.getMountableOutputAt(index).getComponent();
  }

  private static CharSequence getTextFromTextComponent(
      final LayoutState layoutState, final int index) {
    return Whitebox.getInternalState(
        layoutState.getMountableOutputAt(index).getComponent(), "text");
  }

  private static boolean isHostComponent(final ComponentLifecycle component) {
    return component instanceof HostComponent;
  }

  @Test
  public void testNoMeasureOnNestedComponentWithSameSpecs() {
    final ComponentContext baseContext = new ComponentContext(application);
    final ComponentContext c =
        ComponentContext.withComponentTree(baseContext, ComponentTree.create(baseContext).build());
    final LayoutState layoutState = new LayoutState(c);
    c.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState));

    final Size size = new Size();
    final TestComponent innerComponent =
        TestDrawableComponent.create(c, 0, 0, false, true, true, false, false).build();
    final int widthSpec = makeSizeSpec(100, EXACTLY);
    final int heightSpec = makeSizeSpec(100, EXACTLY);
    innerComponent.measure(c, widthSpec, heightSpec, size);

    final InternalNode internalNode = layoutState.getCachedLayout(innerComponent);
    internalNode.setLastWidthSpec(widthSpec);
    internalNode.setLastHeightSpec(heightSpec);
    internalNode.setLastMeasuredWidth(internalNode.getWidth());
    internalNode.setLastMeasuredHeight(internalNode.getHeight());

    innerComponent.resetInteractions();

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(Row.create(c).child(innerComponent).widthPx(100).heightPx(100))
                .build();
          }
        };

    calculateLayoutState(
        application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(innerComponent.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testNoMeasureOnNestedComponentWithNewMeasureSpecExact() {
    final ComponentContext baseContext = new ComponentContext(application);
    final ComponentContext c =
        ComponentContext.withComponentTree(baseContext, ComponentTree.create(baseContext).build());
    final LayoutState layoutState = new LayoutState(c);
    c.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState));

    final Size size = new Size();
    final TestComponent innerComponent =
        TestDrawableComponent.create(c, 0, 0, false, true, true, false, false).build();
    final int widthSpec = makeSizeSpec(100, AT_MOST);
    final int heightSpec = makeSizeSpec(100, AT_MOST);
    innerComponent.measure(c, widthSpec, heightSpec, size);

    final InternalNode internalNode = layoutState.getCachedLayout(innerComponent);
    internalNode.setLastWidthSpec(widthSpec);
    internalNode.setLastHeightSpec(heightSpec);
    internalNode.setLastMeasuredWidth(100);
    internalNode.setLastMeasuredHeight(100);

    innerComponent.resetInteractions();

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(Row.create(c).child(innerComponent).widthPx(100).heightPx(100))
                .build();
          }
        };

    calculateLayoutState(
        application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(innerComponent.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testNoMeasureOnNestedComponentWithNewMeasureSpecOldUnspecified() {
    final ComponentContext baseContext = new ComponentContext(application);
    final ComponentContext c =
        ComponentContext.withComponentTree(baseContext, ComponentTree.create(baseContext).build());
    final LayoutState layoutState = new LayoutState(c);
    c.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState));

    final Size size = new Size();
    final TestComponent innerComponent =
        TestDrawableComponent.create(c, 0, 0, false, true, true, false, false).build();
    final int widthSpec = makeSizeSpec(0, UNSPECIFIED);
    final int heightSpec = makeSizeSpec(0, UNSPECIFIED);
    innerComponent.measure(c, widthSpec, heightSpec, size);

    final InternalNode internalNode = layoutState.getCachedLayout(innerComponent);
    internalNode.setLastWidthSpec(widthSpec);
    internalNode.setLastHeightSpec(heightSpec);
    internalNode.setLastMeasuredWidth(99);
    internalNode.setLastMeasuredHeight(99);

    innerComponent.resetInteractions();

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).child(innerComponent).build();
          }
        };

    calculateLayoutState(
        application, component, -1, makeSizeSpec(100, AT_MOST), makeSizeSpec(100, AT_MOST));

    assertThat(innerComponent.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testNoMeasureOnNestedComponentWithOldAndNewAtMost() {
    final ComponentContext baseContext = new ComponentContext(application);
    final ComponentContext c =
        ComponentContext.withComponentTree(baseContext, ComponentTree.create(baseContext).build());
    final LayoutState layoutState = new LayoutState(c);
    c.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState));

    final Size size = new Size();
    final TestComponent innerComponent =
        TestDrawableComponent.create(c, 0, 0, false, true, true, false, false).build();
    final int widthSpec = makeSizeSpec(100, AT_MOST);
    final int heightSpec = makeSizeSpec(100, AT_MOST);
    innerComponent.measure(c, widthSpec, heightSpec, size);

    final InternalNode internalNode = layoutState.getCachedLayout(innerComponent);
    internalNode.setLastWidthSpec(widthSpec);
    internalNode.setLastHeightSpec(heightSpec);
    internalNode.setLastMeasuredWidth(50);
    internalNode.setLastMeasuredHeight(50);

    innerComponent.resetInteractions();

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).child(Row.create(c).child(innerComponent).flexShrink(0)).build();
          }
        };

    calculateLayoutState(
        application, component, -1, makeSizeSpec(50, AT_MOST), makeSizeSpec(50, AT_MOST));

    assertThat(innerComponent.wasMeasureCalled()).isFalse();
  }

  @Test
  public void testLayoutOutputsForTwiceNestedComponent() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c).child(create(c).child(TestDrawableComponent.create(c))).wrapInView())
                .child(
                    create(c)
                        .child(create(c).child(TestDrawableComponent.create(c)))
                        .child(create(c).child(TestDrawableComponent.create(c))))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(5);

    final long hostMarkerRoot = layoutState.getMountableOutputAt(0).getId();
    final long hostMarkerOne = layoutState.getMountableOutputAt(1).getId();

    // First output is the inner host for the click handler
    assertThat(layoutState.getMountableOutputAt(1).getHostMarker()).isEqualTo(hostMarkerRoot);

    // Second output is the child of the inner host
    assertThat(layoutState.getMountableOutputAt(2).getHostMarker()).isEqualTo(hostMarkerOne);

    // Third and fourth outputs are children of the root view.
    assertThat(layoutState.getMountableOutputAt(3).getHostMarker()).isEqualTo(hostMarkerRoot);
    assertThat(layoutState.getMountableOutputAt(4).getHostMarker()).isEqualTo(hostMarkerRoot);
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
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);

    // First and third output are the background and the foreground
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    assertThat(((DrawableComponent) getComponentAt(layoutState,1)).isBackground()).isTrue();
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(DrawableComponent.class);
    assertThat(((DrawableComponent) getComponentAt(layoutState,3)).isBackground()).isFalse();
  }

  @Test
  public void testLayoutOutputsForComponentWithBackgroundDrawable() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .background(DefaultComparableDrawable.create
                    (new ColorDrawable(0xffff0000)))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        };

    final LayoutState layoutState = calculateLayoutState(
        application,
        component,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    // First output is the background
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    assertThat(((DrawableComponent) getComponentAt(layoutState,1)).isBackground()).isTrue();
  }

  @Test
  public void testLayoutOutputsForNonComponentClickableNode() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(create(c).child(TestDrawableComponent.create(c)).wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c))
                        .wrapInView())
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestViewComponent.create(c))
                        .wrapInView())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(9);

    final long hostMarkerRoot = layoutState.getMountableOutputAt(0).getHostMarker();
    final long hostMarkerZero = layoutState.getMountableOutputAt(1).getHostMarker();
    final long hostMarkerTwo = layoutState.getMountableOutputAt(4).getHostMarker();
    final long hostMarkerThree = layoutState.getMountableOutputAt(7).getHostMarker();

    assertThat(layoutState.getMountableOutputAt(1).getHostMarker()).isEqualTo(hostMarkerRoot);
    assertThat(layoutState.getMountableOutputAt(3).getHostMarker()).isEqualTo(hostMarkerZero);
    assertThat(layoutState.getMountableOutputAt(5).getHostMarker()).isEqualTo(hostMarkerTwo);
    assertThat(layoutState.getMountableOutputAt(6).getHostMarker()).isEqualTo(hostMarkerZero);
    assertThat(layoutState.getMountableOutputAt(8).getHostMarker()).isEqualTo(hostMarkerThree);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue();
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 3))).isTrue();
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 5)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 6))).isTrue();
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 8)).isInstanceOf(TestViewComponent.class);
  }

  @Test
  public void testLayoutOutputsForNonComponentContentDescriptionNode() {
    enableAccessibility();

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(create(c).child(TestDrawableComponent.create(c)).contentDescription("cd0"))
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c))
                        .contentDescription("cd1"))
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .child(TestViewComponent.create(c))
                        .contentDescription("cd2"))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(9);

    final long hostMarkerRoot = layoutState.getMountableOutputAt(0).getHostMarker();
    final long hostMarkerZero = layoutState.getMountableOutputAt(1).getHostMarker();
    final long hostMarkerTwo = layoutState.getMountableOutputAt(4).getHostMarker();
    final long hostMarkerThree = layoutState.getMountableOutputAt(7).getHostMarker();

    assertThat(layoutState.getMountableOutputAt(1).getHostMarker()).isEqualTo(hostMarkerRoot);
    assertThat(layoutState.getMountableOutputAt(3).getHostMarker()).isEqualTo(hostMarkerZero);
    assertThat(layoutState.getMountableOutputAt(5).getHostMarker()).isEqualTo(hostMarkerTwo);
    assertThat(layoutState.getMountableOutputAt(6).getHostMarker()).isEqualTo(hostMarkerZero);
    assertThat(layoutState.getMountableOutputAt(8).getHostMarker()).isEqualTo(hostMarkerThree);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(isHostComponent(getComponentAt(layoutState, 1))).isTrue();
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 3))).isTrue();
    assertThat(getComponentAt(layoutState, 4)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 5)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 6))).isTrue();
    assertThat(getComponentAt(layoutState, 7)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 8)).isInstanceOf(TestViewComponent.class);
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
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
    final long hostMarkerZero = layoutState.getMountableOutputAt(0).getHostMarker();

    assertThat(layoutState.getMountableOutputAt(1).getHostMarker()).isEqualTo(hostMarkerZero);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);

    assertThat(layoutState.getMountableOutputAt(0).getNodeInfo().getFocusState())
        .isEqualTo(FOCUS_SET_TRUE);
  }

  @Test
  public void testLayoutOutputsForFocusable() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(create(c).child(TestDrawableComponent.create(c)).focusable(true))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);
    assertThat(layoutState.getMountableOutputAt(0).getNodeInfo()).isNull();
    assertThat(layoutState.getMountableOutputAt(1).getNodeInfo().getFocusState())
        .isEqualTo(FOCUS_SET_TRUE);
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
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
    final long hostMarkerZero = layoutState.getMountableOutputAt(0).getHostMarker();

    assertThat(layoutState.getMountableOutputAt(1).getHostMarker()).isEqualTo(hostMarkerZero);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);

    assertThat(layoutState.getMountableOutputAt(0).getNodeInfo().getSelectedState())
        .isEqualTo(SELECTED_SET_TRUE);
  }

  @Test
  public void testLayoutOutputsForSelected() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c).child(TestDrawableComponent.create(c)).focusable(true).selected(true))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);
    assertThat(layoutState.getMountableOutputAt(0).getNodeInfo()).isNull();
    assertThat(layoutState.getMountableOutputAt(1).getNodeInfo().getSelectedState())
        .isEqualTo(SELECTED_SET_TRUE);
  }

  @Test
  public void testLayoutOutputsForEnabledFalseDoesntWrap() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(create(c).child(TestDrawableComponent.create(c).enabled(false)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);

    assertThat(layoutState.getMountableOutputAt(0).getNodeInfo()).isNull();
    assertThat(layoutState.getMountableOutputAt(0).getComponent().getSimpleName())
        .isEqualTo("HostComponent");

    assertThat(layoutState.getMountableOutputAt(1).getComponent().getSimpleName())
        .isEqualTo("TestDrawableComponent");
    assertThat(MountItem.isTouchableDisabled(layoutState.getMountableOutputAt(1).getFlags()))
        .isTrue();
  }

  @Test
  public void testLayoutOutputsForEnabledFalseInInnerWrappedComponentDrawable() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(
                            TestDrawableComponent.create(c)
                                .clickHandler(c.newEventHandler(1))
                                .enabled(false)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    // Because the TestDrawableComponent is disabled, we don't wrap it in a host.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);

    assertThat(layoutState.getMountableOutputAt(0).getNodeInfo()).isNull();
    assertThat(layoutState.getMountableOutputAt(0).getComponent())
        .isInstanceOf(HostComponent.class);

    assertThat(layoutState.getMountableOutputAt(1).getComponent())
        .isInstanceOf(TestDrawableComponent.class);
    assertThat(MountItem.isTouchableDisabled(layoutState.getMountableOutputAt(1).getFlags()))
        .isTrue();
  }

  @Test
  public void testLayoutOutputsForEnabledFalseInInnerComponentView() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(create(c).child(TestViewComponent.create(c).enabled(false)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);

    assertThat(layoutState.getMountableOutputAt(0).getNodeInfo()).isNull();
    assertThat(layoutState.getMountableOutputAt(0).getComponent())
        .isInstanceOf(HostComponent.class);

    assertThat(layoutState.getMountableOutputAt(1).getComponent())
        .isInstanceOf(TestViewComponent.class);
    assertThat(layoutState.getMountableOutputAt(1).getNodeInfo().getEnabledState())
        .isEqualTo(ENABLED_SET_FALSE);
  }

  @Test
  public void testLayoutOutputsForEnabledFalseApplyToDescendent() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .enabled(false)
                        .child(TestViewComponent.create(c).enabled(true))
                        .child(TestDrawableComponent.create(c).clickHandler(c.newEventHandler(1)))
                        .child(TestDrawableComponent.create(c).enabled(false)))
                .child(
                    create(c)
                        .child(TestViewComponent.create(c))
                        .child(TestDrawableComponent.create(c)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(6);

    assertThat(layoutState.getMountableOutputAt(0).getNodeInfo()).isNull();
    assertThat(layoutState.getMountableOutputAt(0).getComponent())
        .isInstanceOf(HostComponent.class);

    assertThat(layoutState.getMountableOutputAt(1).getComponent())
        .isInstanceOf(TestViewComponent.class);
    assertThat(layoutState.getMountableOutputAt(1).getNodeInfo().getEnabledState())
        .isEqualTo(ENABLED_SET_FALSE);

    assertThat(layoutState.getMountableOutputAt(2).getComponent())
        .isInstanceOf(TestDrawableComponent.class);
    assertThat(layoutState.getMountableOutputAt(2).getNodeInfo()).isNull();
    assertThat(MountItem.isTouchableDisabled(layoutState.getMountableOutputAt(2).getFlags()))
        .isTrue();

    assertThat(layoutState.getMountableOutputAt(3).getComponent())
        .isInstanceOf(TestDrawableComponent.class);
    assertThat(layoutState.getMountableOutputAt(3).getNodeInfo()).isNull();
    assertThat(MountItem.isTouchableDisabled(layoutState.getMountableOutputAt(3).getFlags()))
        .isTrue();

    assertThat(layoutState.getMountableOutputAt(4).getComponent())
        .isInstanceOf(TestViewComponent.class);
    assertThat(layoutState.getMountableOutputAt(4).getNodeInfo()).isNull();
    assertThat(layoutState.getMountableOutputAt(5).getComponent())
        .isInstanceOf(TestDrawableComponent.class);
    assertThat(layoutState.getMountableOutputAt(5).getNodeInfo()).isNull();
    assertThat(MountItem.isTouchableDisabled(layoutState.getMountableOutputAt(5).getFlags()))
        .isFalse();
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
                    TestDrawableComponent.create(c, true, true, true, true)
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
                            TestDrawableComponent.create(c, true, true, true, true)
                                .flex(1)
                                .flexBasisDip(0)
                                .marginDip(HORIZONTAL, 10)))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(10);

    final long hostMarkerRoot = layoutState.getMountableOutputAt(1).getHostMarker();
    final long hostMarkerOne = layoutState.getMountableOutputAt(3).getHostMarker();
    final long hostMarkerTwo = layoutState.getMountableOutputAt(6).getHostMarker();
    final long hostMarkerThree = layoutState.getMountableOutputAt(7).getHostMarker();
    final long hostMarkerFour = layoutState.getMountableOutputAt(9).getHostMarker();

    assertThat(layoutState.getMountableOutputAt(1).getHostMarker()).isEqualTo(hostMarkerRoot);
    assertThat(layoutState.getMountableOutputAt(3).getHostMarker()).isEqualTo(hostMarkerOne);
    assertThat(layoutState.getMountableOutputAt(4).getHostMarker()).isEqualTo(hostMarkerOne);
    assertThat(layoutState.getMountableOutputAt(6).getHostMarker()).isEqualTo(hostMarkerTwo);
    assertThat(layoutState.getMountableOutputAt(7).getHostMarker()).isEqualTo(hostMarkerThree);
    assertThat(layoutState.getMountableOutputAt(9).getHostMarker()).isEqualTo(hostMarkerFour);

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
                    TestDrawableComponent.create(c, true, true, true, true)
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
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(7);

    final long hostMarkerRoot = layoutState.getMountableOutputAt(1).getHostMarker();
    final long hostMarkerOne = layoutState.getMountableOutputAt(5).getHostMarker();
    final long hostMarkerTwo = layoutState.getMountableOutputAt(6).getHostMarker();

    assertThat(layoutState.getMountableOutputAt(1).getHostMarker()).isEqualTo(hostMarkerRoot);
    assertThat(layoutState.getMountableOutputAt(2).getHostMarker()).isEqualTo(hostMarkerRoot);
    assertThat(layoutState.getMountableOutputAt(3).getHostMarker()).isEqualTo(hostMarkerRoot);
    assertThat(layoutState.getMountableOutputAt(4).getHostMarker()).isEqualTo(hostMarkerRoot);
    assertThat(layoutState.getMountableOutputAt(5).getHostMarker()).isEqualTo(hostMarkerOne);
    assertThat(layoutState.getMountableOutputAt(6).getHostMarker()).isEqualTo(hostMarkerTwo);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(DrawableComponent.class);
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestDrawableComponent.class);
    assertThat(isHostComponent(getComponentAt(layoutState, 4))).isTrue();
    assertThat(isHostComponent(getComponentAt(layoutState, 5))).isTrue();
    assertThat(getComponentAt(layoutState, 6)).isInstanceOf(TestDrawableComponent.class);

    assertThat(IMPORTANT_FOR_ACCESSIBILITY_AUTO)
        .isEqualTo(layoutState.getMountableOutputAt(0).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_AUTO)
        .isEqualTo(layoutState.getMountableOutputAt(1).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_NO)
        .isEqualTo(layoutState.getMountableOutputAt(2).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_NO)
        .isEqualTo(layoutState.getMountableOutputAt(3).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
        .isEqualTo(layoutState.getMountableOutputAt(4).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_YES)
        .isEqualTo(layoutState.getMountableOutputAt(5).getImportantForAccessibility());
    assertThat(IMPORTANT_FOR_ACCESSIBILITY_YES)
        .isEqualTo(layoutState.getMountableOutputAt(6).getImportantForAccessibility());
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
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
    final long hostMarkerZero = layoutState.getMountableOutputAt(0).getHostMarker();

    assertThat(layoutState.getMountableOutputAt(1).getHostMarker()).isEqualTo(hostMarkerZero);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(0).getNodeInfo();

    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getClickHandler()).isNotNull();
    assertThat(nodeInfo.getViewTags()).isNotNull();
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
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
    final long hostMarkerZero = layoutState.getMountableOutputAt(0).getHostMarker();

    assertThat(layoutState.getMountableOutputAt(1).getHostMarker()).isEqualTo(hostMarkerZero);

    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(TestDrawableComponent.class);

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(0).getNodeInfo();
    assertThat(nodeInfo).isNotNull();
    assertThat(nodeInfo.getLongClickHandler()).isNotNull();
    assertThat(nodeInfo.getViewTags()).isNotNull();
  }

  @Test
  public void testLayoutOutputsForForceWrappedComponent() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).child(TestDrawableComponent.create(c).wrapInView()).build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);
    assertThat(getComponentAt(layoutState, 0)).isInstanceOf(HostComponent.class);
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(HostComponent.class);
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
  }

  @Test
  public void testLayoutOutputForRootNestedTreeComponent() {
    final LayoutState layoutState =
        calculateLayoutState(
            application,
            TestSizeDependentComponent.create(new ComponentContext(application))
                .setFixSizes(true)
                .setDelegate(false)
                .build(),
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);
    final Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    assertThat(layoutState.getMountableOutputAt(0).getHostMarker()).isEqualTo(0);
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(5, 5, 55, 55));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(5, 5, 55, 55));
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent.class);
    layoutState.getMountableOutputAt(3).getMountBounds(mountBounds);
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
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);
    final Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(13, 13, 63, 63));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(13, 13, 63, 63));
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
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);
    final Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    assertThat(layoutState.getMountableOutputAt(0).getHostMarker()).isEqualTo(0);
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(18, 18, 68, 68));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(18, 18, 68, 68));
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent.class);
    layoutState.getMountableOutputAt(3).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(21, 71, 329, 91));
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
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);
    final Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    assertThat(layoutState.getMountableOutputAt(0).getHostMarker()).isEqualTo(0);
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(5, 5, 55, 55));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(5, 5, 55, 55));
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent.class);
    layoutState.getMountableOutputAt(3).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(8, 58, 342, 78));
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
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    // Check total layout outputs.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(4);
    final Rect mountBounds = new Rect();
    // Check host.
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
    assertThat(layoutState.getMountableOutputAt(0).getHostMarker()).isEqualTo(0);
    // Check NestedTree
    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(10, 10, 60, 60));
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(TestDrawableComponent.class);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(10, 10, 60, 60));
    assertThat(getComponentAt(layoutState, 3)).isInstanceOf(TestViewComponent.class);
    layoutState.getMountableOutputAt(3).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(13, 63, 337, 83));
  }

  @Test
  public void testLayoutOutputForRootWithNullLayout() {
    final Component componentWithNullLayout =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return null;
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application,
            componentWithNullLayout,
            -1,
            makeSizeSpec(350, EXACTLY),
            makeSizeSpec(200, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(0);
  }

  @Test
  public void testLayoutComponentForNestedTreeChildWithNullLayout() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c).paddingPx(ALL, 2).child(new TestNullLayoutComponent()).build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(350, EXACTLY), makeSizeSpec(200, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(1);
    final Rect mountBounds = new Rect();
    assertThat(isHostComponent(getComponentAt(layoutState, 0))).isTrue();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 350, 200));
  }

  @Test
  public void testMeasure() {
    final int width = 50;
    final int height = 30;
    final ComponentContext c = new ComponentContext(application);
    c.setLayoutStateReferenceWrapperForTesting();
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c).measuredWidth(width).measuredHeight(height))
                .build();
          }
        };

    final InternalNode node =
        createAndMeasureTreeForComponent(
            c, component, makeSizeSpec(width, AT_MOST), makeSizeSpec(height, AT_MOST));

    assertThat(node.getWidth()).isEqualTo(width);
    assertThat(node.getHeight()).isEqualTo(height);
    assertThat(node.getChildCount()).isEqualTo(1);
    assertThat(((InternalNode) node.getChildAt(0)).getWidth()).isEqualTo(width);
    assertThat(((InternalNode) node.getChildAt(0)).getHeight()).isEqualTo(height);
  }

  @Test
  public void testNestedTreeComponentWithDoubleMeasurementsDoesntThrow() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return Row.create(c)
                .alignItems(YogaAlign.STRETCH)
                .paddingPx(YogaEdge.ALL, 2)
                .child(
                    TestSizeDependentComponent.create(c)
                        .setFixSizes(true)
                        .setDelegate(false)
                        .marginPx(YogaEdge.ALL, 11))
                .child(TestDrawableComponent.create(c).heightPx(200).widthPx(200))
                .build();
          }
        };

    calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED));

    // Testing that is not throwing an exception.
  }

  @Test
  public void testLayoutOutputForRootNestedTreeComponentWithAspectRatio() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestSizeDependentComponent.create(c).widthPx(100).aspectRatio(1))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED));

    final Rect mountBounds = new Rect();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 100, 100));
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
            application, component, -1, makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED));

    final Rect mountBounds = new Rect();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 100, 100));

    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 50, 50));
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
            application, component, -1, makeSizeSpec(0, UNSPECIFIED), makeSizeSpec(0, UNSPECIFIED));

    final Rect mountBounds = new Rect();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 60, 86));

    assertThat(getComponentAt(layoutState, 1)).isInstanceOf(DrawableComponent.class);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertThat(mountBounds).isEqualTo(new Rect(0, 0, 60, 86));
  }

  @Test
  public void testLayoutOutputsForComponentWithBorderColorNoBorderWidth() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .border(Border.create(c).color(ALL, GREEN).build())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    // No layout output generated related with borders
    // if borderColor is supplied but not borderWidth.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
  }

  @Test
  public void testLayoutOutputsForComponentWithBorderWidthNoBorderColor() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .border(Border.create(c).widthPx(ALL, 10).build())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    // No layout output generated related with borders
    // if borderWidth supplied but not borderColor.
    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
  }

  @Test
  public void testLayoutOutputsForComponentWithBorderWidthAllAndBorderColor() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .border(Border.create(c).widthPx(ALL, 10).color(ALL, GREEN).build())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    // Output at index 1 is BorderColorDrawable component.
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(DrawableComponent.class);
  }

  @Test
  public void testLayoutOutputsForComponentWithBorderWidthTopAndBorderColor() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(TestDrawableComponent.create(c))
                .border(Border.create(c).widthPx(TOP, 10).color(TOP, GREEN).build())
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    // Output at index 1 is BorderColorDrawable component.
    assertThat(getComponentAt(layoutState, 2)).isInstanceOf(DrawableComponent.class);
  }

  @Test
  public void testWillRenderLayoutsOnce() {
    ComponentContext c = new ComponentContext(application);
    c.setLayoutStateReferenceWrapperForTesting();

    final Component componentSpy =
        spy(TestLayoutComponent.create(c, 0, 0, true, true, true, false).build());

    Component.willRender(c, componentSpy);

    final InternalNode cachedLayout = componentSpy.getLayoutCreatedInWillRenderForTesting();
    assertThat(cachedLayout).isNotNull();

    calculateLayoutState(
        c.getAndroidContext(),
        componentSpy,
        -1,
        makeSizeSpec(100, EXACTLY),
        makeSizeSpec(100, EXACTLY));

    assertThat(componentSpy.getLayoutCreatedInWillRenderForTesting()).isNull();

    verify(componentSpy, times(1)).updateInternalChildState(any(ComponentContext.class));
  }

  @Test
  public void testResolveLayoutUsesWillRenderResult() {
    ComponentContext c = new ComponentContext(application);
    c.setLayoutStateReferenceWrapperForTesting();

    final Component component =
        TestLayoutComponent.create(c, 0, 0, true, true, true, false).build();

    Component.willRender(c, component);

    final InternalNode cachedLayout = component.getLayoutCreatedInWillRenderForTesting();
    assertThat(cachedLayout).isNotNull();

    InternalNode result = LayoutState.resolve(c, component);
    assertThat(result).isEqualTo(cachedLayout);
    assertThat(component.getLayoutCreatedInWillRenderForTesting()).isNull();
  }

  @Test
  public void testNewLayoutBuilderUsesWillRenderResult() {
    ComponentContext c = new ComponentContext(application);
    c.setLayoutStateReferenceWrapperForTesting();

    final Component component =
        TestLayoutComponent.create(c, 0, 0, true, true, true, false).build();

    Component.willRender(c, component);

    final InternalNode cachedLayout = component.getLayoutCreatedInWillRenderForTesting();
    assertThat(cachedLayout).isNotNull();

    InternalNode result = c.newLayoutBuilder(component, 0, 0);
    assertThat(result).isEqualTo(cachedLayout);
    assertThat(component.getLayoutCreatedInWillRenderForTesting()).isNull();
  }

  @Test
  public void testCreateLayoutUsesWillRenderResult() {
    ComponentContext c = new ComponentContext(application);
    c.setLayoutStateReferenceWrapperForTesting();

    final Component component =
        TestLayoutComponent.create(c, 0, 0, true, true, true, false).build();

    Component.willRender(c, component);

    final InternalNode cachedLayout = component.getLayoutCreatedInWillRenderForTesting();
    assertThat(cachedLayout).isNotNull();

    InternalNode result = LayoutState.createLayout(c, component, false);
    assertThat(result).isEqualTo(cachedLayout);
    assertThat(component.getLayoutCreatedInWillRenderForTesting()).isNull();
  }

  @Test
  public void testWillRenderLayoutsOnceInColumn() {
    ComponentContext c = new ComponentContext(application);

    final Component componentSpy =
        spy(TestLayoutComponent.create(c, 0, 0, true, true, true, false).build());

    final Component root =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            Component.willRender(c, componentSpy);

            return Column.create(c).child(componentSpy).build();
          }
        };

    calculateLayoutState(
        c.getAndroidContext(), root, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));
    verify(componentSpy, times(1)).updateInternalChildState(any(ComponentContext.class));
  }

  @Test
  public void testWillRenderTwiceDoesNotReCreateLayout() {
    ComponentContext c = new ComponentContext(application);
    c.setLayoutStateReferenceWrapperForTesting();

    final Component component =
        TestLayoutComponent.create(c, 0, 0, true, true, true, false).build();

    Component.willRender(c, component);

    final InternalNode cachedLayout = component.getLayoutCreatedInWillRenderForTesting();
    assertThat(cachedLayout).isNotNull();

    assertThat(Component.willRender(c, component)).isTrue();
    assertThat(component.getLayoutCreatedInWillRenderForTesting()).isEqualTo(cachedLayout);
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
            new ComponentContext(application, "test", logger),
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY),
            LayoutState.CalculateLayoutSource.TEST);

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(2);
  }

  private void enableAccessibility() {
    final ShadowAccessibilityManager manager =
        Shadows.shadowOf(
            (AccessibilityManager)
                RuntimeEnvironment.application.getSystemService(ACCESSIBILITY_SERVICE));
    manager.setEnabled(true);
    manager.setTouchExplorationEnabled(true);
  }

  private LayoutState calculateLayoutState(
      final Context context,
      final Component component,
      final int componentTreeId,
      final int widthSpec,
      final int heightSpec) {

    return LayoutState.calculate(
        new ComponentContext(context),
        component,
        componentTreeId,
        widthSpec,
        heightSpec,
        LayoutState.CalculateLayoutSource.TEST);
  }
}
