/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.TestNullLayoutComponent;
import com.facebook.litho.testing.TestSizeDependentComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaPositionType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAccessibilityManager;

import static android.content.Context.ACCESSIBILITY_SERVICE;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static com.facebook.litho.NodeInfo.FOCUS_SET_TRUE;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.TOP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@PrepareForTest(Component.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculateTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  @Before
  public void setup() throws Exception {
  }

  @Test
  public void testNoUnnecessaryLayoutOutputsForLayoutSpecs() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(2, layoutState.getMountableOutputCount());
  }

  @Test
  public void testLayoutOutputsForRootInteractiveLayoutSpecs() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .wrapInView()
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(2, layoutState.getMountableOutputCount());
  }

  @Test
  public void testLayoutOutputsForSpecsWithClickHandling() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .clickHandler(c.newEventHandler(1)))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(3, layoutState.getMountableOutputCount());

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(1).getNodeInfo();
    assertNotNull(nodeInfo);
    assertNotNull(nodeInfo.getClickHandler());
    assertNull(nodeInfo.getLongClickHandler());
    assertNull(nodeInfo.getTouchHandler());
  }

  @Test
  public void testLayoutOutputsForSpecsWithLongClickHandling() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .longClickHandler(c.newEventHandler(1)))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(3, layoutState.getMountableOutputCount());

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(1).getNodeInfo();
    assertNotNull(nodeInfo);
    assertNull(nodeInfo.getClickHandler());
    assertNotNull(nodeInfo.getLongClickHandler());
    assertNull(nodeInfo.getTouchHandler());
  }

  @Test
  public void testLayoutOutputsForSpecsWithTouchHandling() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .touchHandler(c.newEventHandler(1)))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(3, layoutState.getMountableOutputCount());

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(1).getNodeInfo();
    assertNotNull(nodeInfo);
    assertNotNull(nodeInfo.getTouchHandler());
    assertNull(nodeInfo.getClickHandler());
    assertNull(nodeInfo.getLongClickHandler());
  }

  @Test
  public void testLayoutOutputsForDeepLayoutSpecs() {
    final int paddingSize = 5;
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .backgroundColor(0xFFFF0000)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .justifyContent(YogaJustify.SPACE_AROUND)
                    .alignItems(YogaAlign.CENTER)
                    .positionType(YogaPositionType.ABSOLUTE)
                    .positionPx(LEFT, 50)
                    .positionPx(TOP, 50)
                    .positionPx(RIGHT, 200)
                    .positionPx(BOTTOM, 50)
                    .child(
                        Text.create(c)
                            .text("textLeft1"))
                    .child(
                        Text.create(c)
                            .text("textRight1"))
                    .paddingPx(YogaEdge.ALL, paddingSize)
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .justifyContent(YogaJustify.SPACE_AROUND)
                    .alignItems(YogaAlign.CENTER)
                    .positionType(YogaPositionType.ABSOLUTE)
                    .positionPx(LEFT, 200)
                    .positionPx(TOP, 50)
                    .positionPx(RIGHT, 50)
                    .positionPx(BOTTOM, 50)
                    .child(
                        Text.create(c)
                            .text("textLeft2")
                            .withLayout().flexShrink(0)
                            .wrapInView()
                            .paddingPx(YogaEdge.ALL, paddingSize))
                    .child(
                        TestViewComponent.create(c)
                            .withLayout().flexShrink(0)
                            .wrapInView()))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));
    // Check total layout outputs.
    assertEquals(8, layoutState.getMountableOutputCount());

    // Check quantity of HostComponents.
    int totalHosts = 0;
    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      ComponentLifecycle lifecycle = getComponentAt(layoutState, i);
      if (isHostComponent(lifecycle)) {
        totalHosts++;
      }
    }
    assertEquals(3, totalHosts);

    //Check all the Layouts are in the correct position.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 2)));
    assertTrue(getComponentAt(layoutState, 3) instanceof Text);
    assertTrue(getComponentAt(layoutState, 4) instanceof Text);
    assertTrue(isHostComponent(getComponentAt(layoutState, 5)));
    assertTrue(getComponentAt(layoutState, 6) instanceof Text);
    assertTrue(getComponentAt(layoutState, 7) instanceof TestViewComponent);

    // Check the text within the TextComponents.
    assertEquals("textLeft1", getTextFromTextComponent(layoutState, 3));
    assertEquals("textRight1", getTextFromTextComponent(layoutState, 4));
    assertEquals("textLeft2", getTextFromTextComponent(layoutState, 6));

    Rect textLayoutBounds = layoutState.getMountableOutputAt(6).getBounds();
    Rect textBackgroundBounds = layoutState.getMountableOutputAt(5).getBounds();

    assertEquals(textBackgroundBounds.left , textLayoutBounds.left - paddingSize);
    assertEquals(textBackgroundBounds.top , textLayoutBounds.top - paddingSize);
    assertEquals(textBackgroundBounds.right , textLayoutBounds.right + paddingSize);
    assertEquals(textBackgroundBounds.bottom , textLayoutBounds.bottom + paddingSize);
  }

  @Test
  public void testLayoutOutputMountBounds() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .widthPx(30)
            .heightPx(30)
            .wrapInView()
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .widthPx(10)
                    .heightPx(10)
                    .marginPx(YogaEdge.ALL, 10)
                    .wrapInView()
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .widthPx(10)
                            .heightPx(10)))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));

    Rect mountBounds = new Rect();

    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 30, 30), mountBounds);

    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(10, 10, 20, 20), mountBounds);

    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 10, 10), mountBounds);
  }

  @Test
  public void testLayoutOutputsForDeepLayoutSpecsWithBackground() {
    final int paddingSize = 5;
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .backgroundColor(0xFFFF0000)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .justifyContent(YogaJustify.SPACE_AROUND)
                    .alignItems(YogaAlign.CENTER)
                    .positionType(YogaPositionType.ABSOLUTE)
                    .positionPx(LEFT, 50)
                    .positionPx(TOP, 50)
                    .positionPx(RIGHT, 200)
                    .positionPx(BOTTOM, 50)
                    .child(
                        Text.create(c)
                            .text("textLeft1"))
                    .child(
                        Text.create(c)
                            .text("textRight1"))
                    .backgroundColor(0xFFFF0000)
                    .foregroundColor(0xFFFF0000)
                    .paddingPx(YogaEdge.ALL, paddingSize)
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .justifyContent(YogaJustify.SPACE_AROUND)
                    .alignItems(YogaAlign.CENTER)
                    .positionType(YogaPositionType.ABSOLUTE)
                    .positionPx(LEFT, 200)
                    .positionPx(TOP, 50)
                    .positionPx(RIGHT, 50)
                    .positionPx(BOTTOM, 50)
                    .child(
                        Text.create(c)
                            .text("textLeft2")
                            .withLayout().flexShrink(0)
                            .wrapInView()
                            .backgroundColor(0xFFFF0000)
                            .paddingPx(YogaEdge.ALL, paddingSize))
                    .child(
                        TestViewComponent.create(c)
                            .withLayout().flexShrink(0)
                            .backgroundColor(0xFFFF0000)
                            .foregroundColor(0x0000FFFF)
                            .paddingPx(YogaEdge.ALL, paddingSize)))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));

    // Account for Android version in the foreground. If >= M the foreground is part of the
    // ViewLayoutOutput otherwise it has its own LayoutOutput.
    boolean foregroundHasOwnOutput = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;

    // Check total layout outputs.
    assertEquals(
        foregroundHasOwnOutput ? 12 : 11,
        layoutState.getMountableOutputCount());

    // Check quantity of HostComponents.
    int totalHosts = 0;
    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      ComponentLifecycle lifecycle = getComponentAt(layoutState, i);
      if (isHostComponent(lifecycle)) {
        totalHosts++;
      }
    }
    assertEquals(3, totalHosts);

    //Check all the Layouts are in the correct position.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 2)));
    assertTrue(getComponentAt(layoutState, 3) instanceof DrawableComponent);
    assertTrue(getComponentAt(layoutState, 4) instanceof Text);
    assertTrue(getComponentAt(layoutState, 5) instanceof Text);
    assertTrue(getComponentAt(layoutState, 6) instanceof DrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 7)));
    assertTrue(getComponentAt(layoutState, 8) instanceof DrawableComponent);
    assertTrue(getComponentAt(layoutState, 9) instanceof Text);
    assertTrue(getComponentAt(layoutState, 10) instanceof TestViewComponent);
    if (foregroundHasOwnOutput) {
      assertTrue(getComponentAt(layoutState, 11) instanceof DrawableComponent);
    }

    // Check the text within the TextComponents.
    assertEquals("textLeft1", getTextFromTextComponent(layoutState, 4));
    assertEquals("textRight1", getTextFromTextComponent(layoutState, 5));
    assertEquals("textLeft2", getTextFromTextComponent(layoutState, 9));

    //Check that the backgrounds have the same size of the components to which they are associated
    assertEquals(
        layoutState.getMountableOutputAt(2).getBounds(),
        layoutState.getMountableOutputAt(3).getBounds());
    assertEquals(
        layoutState.getMountableOutputAt(2).getBounds(),
        layoutState.getMountableOutputAt(6).getBounds());

    Rect textLayoutBounds = layoutState.getMountableOutputAt(9).getBounds();
    Rect textBackgroundBounds = layoutState.getMountableOutputAt(8).getBounds();

    assertEquals(textBackgroundBounds.left , textLayoutBounds.left - paddingSize);
    assertEquals(textBackgroundBounds.top , textLayoutBounds.top - paddingSize);
    assertEquals(textBackgroundBounds.right , textLayoutBounds.right + paddingSize);
    assertEquals(textBackgroundBounds.bottom , textLayoutBounds.bottom + paddingSize);

    assertEquals(layoutState.getMountableOutputAt(7).getBounds(),layoutState.getMountableOutputAt(8).getBounds());

    ViewNodeInfo viewNodeInfo = layoutState.getMountableOutputAt(10).getViewNodeInfo();
    assertNotNull(viewNodeInfo);
    assertTrue(viewNodeInfo.getBackground() != null);
    if (foregroundHasOwnOutput) {
      assertTrue(viewNodeInfo.getForeground() == null);
    } else {
      assertTrue(viewNodeInfo.getForeground() != null);
    }
    assertTrue(viewNodeInfo.getPaddingLeft() == paddingSize);
    assertTrue(viewNodeInfo.getPaddingTop() == paddingSize);
    assertTrue(viewNodeInfo.getPaddingRight() == paddingSize);
    assertTrue(viewNodeInfo.getPaddingBottom() == paddingSize);
  }

  @Test
  public void testLayoutOutputsForMegaDeepLayoutSpecs() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .wrapInView())
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))
                            .child(TestDrawableComponent.create(c))
                            .wrapInView())
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))
                            .child(TestDrawableComponent.create(c)))
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))
                            .child(TestViewComponent.create(c)))
                    .wrapInView())
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));

    // Check total layout outputs.
    assertEquals(18, layoutState.getMountableOutputCount());

    // Check quantity of HostComponents.
    int totalHosts = 0;
    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      ComponentLifecycle lifecycle = getComponentAt(layoutState, i);
      if (isHostComponent(lifecycle)) {
        totalHosts++;
      }
    }
    assertEquals(7, totalHosts);

    // Check all the Components match the right LayoutOutput positions.
    // Tree One.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(isHostComponent(getComponentAt(layoutState, 1)));
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 3) instanceof TestDrawableComponent);

    // Tree Two.
    assertTrue(isHostComponent(getComponentAt(layoutState, 4)));
    assertTrue(isHostComponent(getComponentAt(layoutState, 5)));
    assertTrue(getComponentAt(layoutState, 6) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 7)));
    assertTrue(getComponentAt(layoutState, 8) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 9) instanceof TestDrawableComponent);

    // Tree Three.
    assertTrue(isHostComponent(getComponentAt(layoutState, 10)));
    assertTrue(getComponentAt(layoutState, 11) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 12) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 13) instanceof TestDrawableComponent);

    // Tree Four.
    assertTrue(isHostComponent(getComponentAt(layoutState, 14)));
    assertTrue(getComponentAt(layoutState, 15) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 16) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 17) instanceof TestViewComponent);
  }

  @Test
  public void testLayoutOutputStableIds() {
    final Component component1 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .contentDescription("cd0"))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .contentDescription("cd1"))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestViewComponent.create(c))
                    .contentDescription("cd2"))
            .build();
      }
    };
    final Component component2 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .contentDescription("cd0"))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .contentDescription("cd1"))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestViewComponent.create(c))
                    .contentDescription("cd2"))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component1,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY));

    LayoutState sameComponentLayoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component2,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY));

    assertEquals(
        layoutState.getMountableOutputCount(),
        sameComponentLayoutState.getMountableOutputCount());

    for (int i = 0; i < layoutState.getMountableOutputCount(); i++) {
      assertEquals(
          layoutState.getMountableOutputAt(i).getId(),
          sameComponentLayoutState.getMountableOutputAt(i).getId());
    }
  }

  @Test
  public void testLayoutOutputStableIdsForMegaDeepComponent() {
    final Component component1 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .wrapInView())
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))
                            .child(TestDrawableComponent.create(c))
                            .wrapInView())
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))
                            .child(TestDrawableComponent.create(c)))
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))
                            .child(TestViewComponent.create(c)))
                    .wrapInView())
            .build();
      }
    };

    final Component component2 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .wrapInView())
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))
                            .child(TestDrawableComponent.create(c))
                            .wrapInView())
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))
                            .child(TestDrawableComponent.create(c)))
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))
                            .child(TestViewComponent.create(c)))
                    .wrapInView())
            .build();
      }
    };

