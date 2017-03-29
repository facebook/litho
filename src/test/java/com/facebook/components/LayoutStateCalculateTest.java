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
  public void testPartiallyStableIds() {
    final Component component1 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };
    final Component component2 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    LayoutState layoutState1 = calculateLayoutState(
        RuntimeEnvironment.application,
        component1,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY));

    LayoutState layoutState2 = calculateLayoutState(
        RuntimeEnvironment.application,
        component2,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY));

    assertEquals(
        layoutState1.getMountableOutputAt(0).getId(),
        layoutState2.getMountableOutputAt(0).getId());
    assertEquals(
        layoutState1.getMountableOutputAt(1).getId(),
        layoutState2.getMountableOutputAt(1).getId());
    assertEquals(3, layoutState1.getMountableOutputCount());
    assertEquals(4, layoutState2.getMountableOutputCount());
  }

  @Test
  public void testDifferentIds() {
    final Component component1 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };
    final Component component2 = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .wrapInView())
            .build();
      }
    };

    LayoutState layoutState1 = calculateLayoutState(
        RuntimeEnvironment.application,
        component1,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY));

    LayoutState layoutState2 = calculateLayoutState(
        RuntimeEnvironment.application,
        component2,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(20, SizeSpec.EXACTLY));

    assertNotEquals(
        layoutState1.getMountableOutputAt(1).getId(),
        layoutState2.getMountableOutputAt(1).getId());
  }

  @Test
  public void testLayoutOutputsWithInteractiveLayoutSpecAsLeafs() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestLayoutComponent.create(c))
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
    assertEquals(3, layoutState.getMountableOutputCount());

    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(getComponentAt(layoutState, 1) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 2)));
  }

  private static ComponentLifecycle getComponentAt(LayoutState layoutState, int index) {
    return layoutState.getMountableOutputAt(index).getComponent().getLifecycle();
  }

  private static CharSequence getTextFromTextComponent(LayoutState layoutState, int index) {
    return Whitebox.getInternalState(layoutState.getMountableOutputAt(index).getComponent(), "text");
  }

  private static boolean isHostComponent(ComponentLifecycle component) {
    return component instanceof HostComponent;
  }

  @Test
  public void testNoMeasureOnNestedComponentWithSameSpecs() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);

    final Size size = new Size();
    final TestComponent innerComponent =
        TestDrawableComponent.create(c, 0, 0, false, true, true, false, false).build();
    final int widthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY);
    innerComponent.measure(
        c,
        widthSpec,
        heightSpec,
        size);

    InternalNode internalNode = ((Component) innerComponent).getCachedLayout();
    internalNode.setLastWidthSpec(widthSpec);
    internalNode.setLastHeightSpec(heightSpec);
    internalNode.setLastMeasuredWidth(internalNode.getWidth());
    internalNode.setLastMeasuredHeight(internalNode.getHeight());

    innerComponent.resetInteractions();

    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Layout.create(c, innerComponent).flexShrink(0)
                    .widthPx(100)
                    .heightPx(100))
            .build();
      }
    };

    calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertFalse(innerComponent.wasMeasureCalled());
  }

  @Test
  public void testNoMeasureOnNestedComponentWithNewMeasureSpecExact() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);

    final Size size = new Size();
    final TestComponent innerComponent =
        TestDrawableComponent.create(c, 0, 0, false, true, true, false, false).build();
    final int widthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.AT_MOST);
    final int heightSpec = SizeSpec.makeSizeSpec(100, SizeSpec.AT_MOST);
    innerComponent.measure(
        c,
        widthSpec,
        heightSpec,
        size);

    InternalNode internalNode = ((Component) innerComponent).getCachedLayout();
    internalNode.setLastWidthSpec(widthSpec);
    internalNode.setLastHeightSpec(heightSpec);
    internalNode.setLastMeasuredWidth(100);
    internalNode.setLastMeasuredHeight(100);

    innerComponent.resetInteractions();

    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Layout.create(c, innerComponent).flexShrink(0)
                    .widthPx(100)
                    .heightPx(100))
            .build();
      }
    };

    calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertFalse(innerComponent.wasMeasureCalled());
  }

  @Test
  public void testNoMeasureOnNestedComponentWithNewMeasureSpecOldUnspecified() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);

    final Size size = new Size();
    final TestComponent innerComponent =
        TestDrawableComponent.create(c, 0, 0, false, true, true, false, false).build();
    final int widthSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    final int heightSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    innerComponent.measure(
        c,
        widthSpec,
        heightSpec,
        size);

    InternalNode internalNode = ((Component) innerComponent).getCachedLayout();
    internalNode.setLastWidthSpec(widthSpec);
    internalNode.setLastHeightSpec(heightSpec);
    internalNode.setLastMeasuredWidth(99);
    internalNode.setLastMeasuredHeight(99);

    innerComponent.resetInteractions();

    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(innerComponent)
            .build();
      }
    };

    calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.AT_MOST),
        SizeSpec.makeSizeSpec(100, SizeSpec.AT_MOST));

    assertFalse(innerComponent.wasMeasureCalled());
  }

  @Test
  public void testNoMeasureOnNestedComponentWithOldAndNewAtMost() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);

    final Size size = new Size();
    final TestComponent innerComponent =
        TestDrawableComponent.create(c, 0, 0, false, true, true, false, false).build();
    final int widthSpec = SizeSpec.makeSizeSpec(100, SizeSpec.AT_MOST);
    final int heightSpec = SizeSpec.makeSizeSpec(100, SizeSpec.AT_MOST);
    innerComponent.measure(
        c,
        widthSpec,
        heightSpec,
        size);

    InternalNode internalNode = ((Component) innerComponent).getCachedLayout();
    internalNode.setLastWidthSpec(widthSpec);
    internalNode.setLastHeightSpec(heightSpec);
    internalNode.setLastMeasuredWidth(50);
    internalNode.setLastMeasuredHeight(50);

    innerComponent.resetInteractions();

    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(innerComponent)
            .build();
      }
    };

    calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(50, SizeSpec.AT_MOST),
        SizeSpec.makeSizeSpec(50, SizeSpec.AT_MOST));

    assertFalse(innerComponent.wasMeasureCalled());
  }

  @Test
  public void testLayoutOutputsForTwiceNestedComponent() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c)))
                .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c)))
                    .child(
                        Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                            .child(TestDrawableComponent.create(c))))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(5, layoutState.getMountableOutputCount());

    long hostMarkerRoot = layoutState.getMountableOutputAt(0).getId();
    long hostMarkerOne = layoutState.getMountableOutputAt(1).getId();

    // First output is the inner host for the click handler
    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(1).getHostMarker());

    // Second output is the child of the inner host
    assertEquals(hostMarkerOne, layoutState.getMountableOutputAt(2).getHostMarker());

    // Third and fourth outputs are children of the root view.
    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(3).getHostMarker());
    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(4).getHostMarker());
  }

  @Test
  public void testLayoutOutputsForComponentWithBackgrounds() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .backgroundColor(0xFFFF0000)
            .foregroundColor(0xFFFF0000)
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(4, layoutState.getMountableOutputCount());

    // First and third output are the background and the foreground
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    assertTrue(getComponentAt(layoutState, 3) instanceof DrawableComponent);
  }

  @Test
  public void testLayoutOutputsForNonComponentClickableNode() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestDrawableComponent.create(c))
                    .wrapInView())
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .child(TestViewComponent.create(c))
                    .wrapInView())
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(9, layoutState.getMountableOutputCount());

    long hostMarkerRoot = layoutState.getMountableOutputAt(0).getHostMarker();
    long hostMarkerZero = layoutState.getMountableOutputAt(1).getHostMarker();
    long hostMarkerTwo = layoutState.getMountableOutputAt(4).getHostMarker();
    long hostMarkerThree = layoutState.getMountableOutputAt(7).getHostMarker();

    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(1).getHostMarker());
    assertEquals(hostMarkerZero, layoutState.getMountableOutputAt(3).getHostMarker());
    assertEquals(hostMarkerTwo, layoutState.getMountableOutputAt(5).getHostMarker());
    assertEquals(hostMarkerZero, layoutState.getMountableOutputAt(6).getHostMarker());
    assertEquals(hostMarkerThree, layoutState.getMountableOutputAt(8).getHostMarker());

    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(isHostComponent(getComponentAt(layoutState, 1)));
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 3)));
    assertTrue(getComponentAt(layoutState, 4) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 5) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 6)));
    assertTrue(getComponentAt(layoutState, 7) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 8) instanceof TestViewComponent);
  }

  @Test
  public void testLayoutOutputsForNonComponentContentDescriptionNode() {
    enableAccessibility();

    final Component component = new InlineLayoutSpec() {
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
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(9, layoutState.getMountableOutputCount());

    long hostMarkerRoot = layoutState.getMountableOutputAt(0).getHostMarker();
    long hostMarkerZero = layoutState.getMountableOutputAt(1).getHostMarker();
    long hostMarkerTwo = layoutState.getMountableOutputAt(4).getHostMarker();
    long hostMarkerThree = layoutState.getMountableOutputAt(7).getHostMarker();

    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(1).getHostMarker());
    assertEquals(hostMarkerZero, layoutState.getMountableOutputAt(3).getHostMarker());
    assertEquals(hostMarkerTwo, layoutState.getMountableOutputAt(5).getHostMarker());
    assertEquals(hostMarkerZero, layoutState.getMountableOutputAt(6).getHostMarker());
    assertEquals(hostMarkerThree, layoutState.getMountableOutputAt(8).getHostMarker());

    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(isHostComponent(getComponentAt(layoutState, 1)));
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 3)));
    assertTrue(getComponentAt(layoutState, 4) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 5) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 6)));
    assertTrue(getComponentAt(layoutState, 7) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 8) instanceof TestViewComponent);
  }

  @Test
  public void testLayoutOutputsForFocusableOnRoot() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .focusable(true)
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
    long hostMarkerZero = layoutState.getMountableOutputAt(0).getHostMarker();

    assertEquals(hostMarkerZero, layoutState.getMountableOutputAt(1).getHostMarker());

    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(getComponentAt(layoutState, 1) instanceof TestDrawableComponent);

    assertEquals(FOCUS_SET_TRUE, layoutState.getMountableOutputAt(0).getNodeInfo().getFocusState());
  }

  @Test
  public void testLayoutOutputsForFocusable() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(TestDrawableComponent.create(c))
                    .focusable(true))
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
    assertNull(layoutState.getMountableOutputAt(0).getNodeInfo());
    assertEquals(FOCUS_SET_TRUE, layoutState.getMountableOutputAt(1).getNodeInfo().getFocusState());
  }

  @Test
  public void testLayoutOutputsForAccessibilityEnabled() {
    enableAccessibility();

    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .flexDirection(YogaFlexDirection.ROW)
            .alignItems(YogaAlign.CENTER)
            .paddingDip(YogaEdge.ALL, 10)
            .contentDescription("This is root view")
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .widthDip(30)
                    .heightDip(30))
            .child(
                TestDrawableComponent.create(c, true, true, true, true, false)
                    .withLayout().flexShrink(0)
                    .flex(1).flexBasisDip(0)
                    .backgroundColor(Color.RED)
                    .marginDip(YogaEdge.HORIZONTAL, 10))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .alignItems(YogaAlign.CENTER)
                    .paddingDip(YogaEdge.ALL, 10)
                    .contentDescription("This is a container")
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .widthDip(30)
                            .heightDip(30)
                            .contentDescription("This is an image"))
                    .child(
                        TestDrawableComponent.create(c, true, true, true, true, false)
                            .withLayout().flexShrink(0)
                            .flex(1).flexBasisDip(0)
                            .marginDip(YogaEdge.HORIZONTAL, 10)))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(10, layoutState.getMountableOutputCount());

    long hostMarkerRoot = layoutState.getMountableOutputAt(1).getHostMarker();
    long hostMarkerOne = layoutState.getMountableOutputAt(3).getHostMarker();
    long hostMarkerTwo = layoutState.getMountableOutputAt(6).getHostMarker();
    long hostMarkerThree = layoutState.getMountableOutputAt(7).getHostMarker();
    long hostMarkerFour = layoutState.getMountableOutputAt(9).getHostMarker();

    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(1).getHostMarker());
    assertEquals(hostMarkerOne, layoutState.getMountableOutputAt(3).getHostMarker());
    assertEquals(hostMarkerOne, layoutState.getMountableOutputAt(4).getHostMarker());
    assertEquals(hostMarkerTwo, layoutState.getMountableOutputAt(6).getHostMarker());
    assertEquals(hostMarkerThree, layoutState.getMountableOutputAt(7).getHostMarker());
    assertEquals(hostMarkerFour, layoutState.getMountableOutputAt(9).getHostMarker());

    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(getComponentAt(layoutState, 1) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 2)));
    assertTrue(getComponentAt(layoutState, 3) instanceof DrawableComponent);
    assertTrue(getComponentAt(layoutState, 4) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 5)));
    assertTrue(isHostComponent(getComponentAt(layoutState, 6)));
    assertTrue(getComponentAt(layoutState, 7) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 8)));
    assertTrue(getComponentAt(layoutState, 9) instanceof TestDrawableComponent);
  }

  @Test
  public void testLayoutOutputsWithImportantForAccessibility() {
    enableAccessibility();

    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .contentDescription("This is root view")
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .widthDip(30)
                    .heightDip(30))
            .child(
                TestDrawableComponent.create(c, true, true, true, true, false)
                    .withLayout().flexShrink(0)
                    .flex(1).flexBasisDip(0)
                    .backgroundColor(Color.RED)
                    .marginDip(YogaEdge.HORIZONTAL, 10)
                    .importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .flexDirection(YogaFlexDirection.ROW)
                    .alignItems(YogaAlign.CENTER)
                    .paddingDip(YogaEdge.ALL, 10)
                    .importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .widthDip(30)
                            .heightDip(30)
                            .importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES)
                            .contentDescription("This is an image")))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(7, layoutState.getMountableOutputCount());

    long hostMarkerRoot = layoutState.getMountableOutputAt(1).getHostMarker();
    long hostMarkerOne = layoutState.getMountableOutputAt(5).getHostMarker();
    long hostMarkerTwo = layoutState.getMountableOutputAt(6).getHostMarker();

    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(1).getHostMarker());
    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(2).getHostMarker());
    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(3).getHostMarker());
    assertEquals(hostMarkerRoot, layoutState.getMountableOutputAt(4).getHostMarker());
    assertEquals(hostMarkerOne, layoutState.getMountableOutputAt(5).getHostMarker());
    assertEquals(hostMarkerTwo, layoutState.getMountableOutputAt(6).getHostMarker());

    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(getComponentAt(layoutState, 1) instanceof TestDrawableComponent);
    assertTrue(getComponentAt(layoutState, 2) instanceof DrawableComponent);
    assertTrue(getComponentAt(layoutState, 3) instanceof TestDrawableComponent);
    assertTrue(isHostComponent(getComponentAt(layoutState, 4)));
    assertTrue(isHostComponent(getComponentAt(layoutState, 5)));
    assertTrue(getComponentAt(layoutState, 6) instanceof TestDrawableComponent);

    assertEquals(
        layoutState.getMountableOutputAt(0).getImportantForAccessibility(),
        IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    assertEquals(
        layoutState.getMountableOutputAt(1).getImportantForAccessibility(),
        IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    assertEquals(
        layoutState.getMountableOutputAt(2).getImportantForAccessibility(),
        IMPORTANT_FOR_ACCESSIBILITY_NO);
    assertEquals(
        layoutState.getMountableOutputAt(3).getImportantForAccessibility(),
        IMPORTANT_FOR_ACCESSIBILITY_NO);
    assertEquals(
        layoutState.getMountableOutputAt(4).getImportantForAccessibility(),
        IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    assertEquals(
        layoutState.getMountableOutputAt(5).getImportantForAccessibility(),
        IMPORTANT_FOR_ACCESSIBILITY_YES);
    assertEquals(
        layoutState.getMountableOutputAt(6).getImportantForAccessibility(),
        IMPORTANT_FOR_ACCESSIBILITY_YES);
  }

  @Test
  public void testLayoutOutputsForClickHandlerAndViewTagsOnRoot() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .clickHandler(c.newEventHandler(1))
            .viewTags(new SparseArray<>())
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
    long hostMarkerZero = layoutState.getMountableOutputAt(0).getHostMarker();

    assertEquals(hostMarkerZero, layoutState.getMountableOutputAt(1).getHostMarker());

    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(getComponentAt(layoutState, 1) instanceof TestDrawableComponent);

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(0).getNodeInfo();

    assertNotNull(nodeInfo);
    assertNotNull(nodeInfo.getClickHandler());
    assertNotNull(nodeInfo.getViewTags());
  }

  @Test
  public void testLayoutOutputsForLongClickHandlerAndViewTagsOnRoot() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .longClickHandler(c.newEventHandler(1))
            .viewTags(new SparseArray<>())
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
    long hostMarkerZero = layoutState.getMountableOutputAt(0).getHostMarker();

    assertEquals(hostMarkerZero, layoutState.getMountableOutputAt(1).getHostMarker());

    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    assertTrue(getComponentAt(layoutState, 1) instanceof TestDrawableComponent);

    final NodeInfo nodeInfo = layoutState.getMountableOutputAt(0).getNodeInfo();
    assertNotNull(nodeInfo);
    assertNotNull(nodeInfo.getLongClickHandler());
    assertNotNull(nodeInfo.getViewTags());
  }

  @Test
  public void testLayoutOutputsForForceWrappedComponent() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .wrapInView())
            .build();
      }
    };

    final LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(3, layoutState.getMountableOutputCount());
    assertTrue(getComponentAt(layoutState, 0) instanceof HostComponent);
    assertTrue(getComponentAt(layoutState, 1) instanceof HostComponent);
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
  }

  @Test
  public void testLayoutOutputForRootNestedTreeComponent() {
    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        TestSizeDependentComponent.create(new ComponentContext(RuntimeEnvironment.application))
            .setFixSizes(true)
            .setDelegate(false)
            .build(),
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));

    // Check total layout outputs.
    assertEquals(4, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    // Check host.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 350, 200), mountBounds);
    assertEquals(0, layoutState.getMountableOutputAt(0).getHostMarker());
    // Check NestedTree
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(5, 5, 55, 55), mountBounds);
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertEquals(new Rect(5, 5, 55, 55), mountBounds);
    assertTrue(getComponentAt(layoutState, 3) instanceof TestViewComponent);
    layoutState.getMountableOutputAt(3).getMountBounds(mountBounds);
    assertEquals(new Rect(8, 58, 342, 78), mountBounds);
  }

  @Test
  public void testLayoutOutputForDelegateNestedTreeComponentDelegate() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .paddingPx(YogaEdge.ALL, 2)
            .child(
                TestSizeDependentComponent.create(c)
                    .setFixSizes(true)
                    .setDelegate(true)
                    .withLayout().flexShrink(0)
                    .marginPx(YogaEdge.ALL, 11))
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
    assertEquals(3, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    // Check host.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 350, 200), mountBounds);
    // Check NestedTree
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(13, 13, 63, 63), mountBounds);
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertEquals(new Rect(13, 13, 63, 63), mountBounds);
  }

  @Test
  public void testLayoutOutputForDelegateNestedTreeComponent() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .paddingPx(YogaEdge.ALL, 2)
            .child(
                TestSizeDependentComponent.create(c)
                    .setFixSizes(true)
                    .setDelegate(false)
                    .withLayout().flexShrink(0)
                    .marginPx(YogaEdge.ALL, 11))
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
    assertEquals(4, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    // Check host.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 350, 200), mountBounds);
    assertEquals(0, layoutState.getMountableOutputAt(0).getHostMarker());
    // Check NestedTree
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(18, 18, 68, 68), mountBounds);
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertEquals(new Rect(18, 18, 68, 68), mountBounds);
    assertTrue(getComponentAt(layoutState, 3) instanceof TestViewComponent);
    layoutState.getMountableOutputAt(3).getMountBounds(mountBounds);
    assertEquals(new Rect(21, 71, 329, 91), mountBounds);
  }

  @Test
  public void testLayoutOutputForRootWithDelegateNestedTreeComponent() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return TestSizeDependentComponent.create(c)
            .setFixSizes(true)
            .setDelegate(false)
            .buildWithLayout();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));

    // Check total layout outputs.
    assertEquals(4, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    // Check host.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 350, 200), mountBounds);
    assertEquals(0, layoutState.getMountableOutputAt(0).getHostMarker());
    // Check NestedTree
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(5, 5, 55, 55), mountBounds);
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertEquals(new Rect(5, 5, 55, 55), mountBounds);
    assertTrue(getComponentAt(layoutState, 3) instanceof TestViewComponent);
    layoutState.getMountableOutputAt(3).getMountBounds(mountBounds);
    assertEquals(new Rect(8, 58, 342, 78), mountBounds);
  }

  @Test
  public void testLayoutOutputRootWithPaddingOverridingDelegateNestedTreeComponent() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        final Component<TestSizeDependentComponent> nestedTreeRootComponent =
            TestSizeDependentComponent.create(c)
                .setFixSizes(true)
                .setDelegate(false)
                .build();

        return Layout.create(c, nestedTreeRootComponent).flexShrink(0)
            .paddingPx(YogaEdge.ALL, 10)
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
    assertEquals(4, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    // Check host.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 350, 200), mountBounds);
    assertEquals(0, layoutState.getMountableOutputAt(0).getHostMarker());
    // Check NestedTree
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(10, 10, 60, 60), mountBounds);
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertEquals(new Rect(10, 10, 60, 60), mountBounds);
    assertTrue(getComponentAt(layoutState, 3) instanceof TestViewComponent);
    layoutState.getMountableOutputAt(3).getMountBounds(mountBounds);
    assertEquals(new Rect(13, 63, 337, 83), mountBounds);
  }

  @Test
  public void testLayoutOutputForRootWithNullLayout() {
    final Component componentWithNullLayout = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return null;
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        componentWithNullLayout,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));

    assertEquals(0, layoutState.getMountableOutputCount());
  }

  @Test
  public void testLayoutComponentForNestedTreeChildWithNullLayout() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .paddingPx(YogaEdge.ALL, 2)
            .child(new TestNullLayoutComponent())
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY));

    assertEquals(1, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 350, 200), mountBounds);
  }

  @Test
  public void testMeasure() {
    final int width = 50;
    final int height = 30;
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                TestDrawableComponent.create(c)
                    .measuredWidth(width)
                    .measuredHeight(height))
            .build();
      }
    };

    InternalNode node = LayoutState.createAndMeasureTreeForComponent(
        c,
        component,
        SizeSpec.makeSizeSpec(width, SizeSpec.AT_MOST),
        SizeSpec.makeSizeSpec(height, SizeSpec.AT_MOST));

    assertEquals(width, node.getWidth());
    assertEquals(height, node.getHeight());
    assertEquals(1, node.getChildCount());
    assertEquals(width, ((InternalNode) node.getChildAt(0)).getWidth());
    assertEquals(height, ((InternalNode) node.getChildAt(0)).getHeight());
  }

  @Test
  public void testLayoutOutputWithCachedLayoutSpec() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final int widthSpecContainer = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    final int horizontalPadding = 20;
    final int widthMeasuredComponent = SizeSpec.makeSizeSpec(
        SizeSpec.getSize(widthSpecContainer) - horizontalPadding - horizontalPadding,
        SizeSpec.EXACTLY);

    final Component<?> componentSpy = PowerMockito.spy(
        TestLayoutComponent.create(c, 0, 0, true, true, true, false).build());
    Size sizeOutput = new Size();
    componentSpy.measure(
        c,
        widthMeasuredComponent,
        heightSpec,
        sizeOutput);

    // Check the cached measured component tree
    assertTrue(componentSpy.hasCachedLayout());
    final InternalNode cachedLayout = componentSpy.getCachedLayout();
    assertEquals(1, cachedLayout.getChildCount());
    assertTrue(((InternalNode) cachedLayout.getChildAt(0))
        .getComponent().getLifecycle() instanceof TestDrawableComponent);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .paddingPx(YogaEdge.HORIZONTAL, horizontalPadding)
            .child(componentSpy)
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        rootContainer,
        -1,
        widthSpecContainer,
        heightSpec);

    // Make sure we reused the cached layout and it wasn't released.
    verify(componentSpy, never()).releaseCachedLayout();
    verify(componentSpy, times(1)).clearCachedLayout();

    // Check total layout outputs.
    assertEquals(2, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    // Check host.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 300, sizeOutput.height), mountBounds);
    assertTrue(getComponentAt(layoutState, 1) instanceof TestDrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(20, 0, 20, 0), mountBounds);

    Mockito.validateMockitoUsage();
  }

  @Test
  public void testLayoutOutputWithCachedLayoutSpecWithMeasure() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final int widthSpecContainer = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    final int horizontalPadding = 20;
    final int widthMeasuredComponent = SizeSpec.makeSizeSpec(
        SizeSpec.getSize(widthSpecContainer) - horizontalPadding - horizontalPadding,
        SizeSpec.EXACTLY);

    final Component<?> sizeDependentComponentSpy = PowerMockito.spy(
        TestSizeDependentComponent.create(c)
            .setFixSizes(false)
            .setDelegate(false)
            .build());
    Size sizeOutput = new Size();
    sizeDependentComponentSpy.measure(
        c,
        widthMeasuredComponent,
        heightSpec,
        sizeOutput);

    // Check the cached measured component tree
    assertTrue(sizeDependentComponentSpy.hasCachedLayout());
    final InternalNode cachedLayout = sizeDependentComponentSpy.getCachedLayout();
    assertEquals(2, cachedLayout.getChildCount());
    assertTrue(((InternalNode) cachedLayout.getChildAt(0))
        .getComponent().getLifecycle() instanceof TestDrawableComponent);
    assertTrue(((InternalNode) cachedLayout.getChildAt(1))
        .getComponent().getLifecycle() instanceof TestViewComponent);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .paddingPx(YogaEdge.HORIZONTAL, horizontalPadding)
            .child(sizeDependentComponentSpy)
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        rootContainer,
        -1,
        widthSpecContainer,
        heightSpec);

    // Make sure we reused the cached layout and it wasn't released.
    verify(sizeDependentComponentSpy, never()).releaseCachedLayout();
    verify(sizeDependentComponentSpy, times(1)).clearCachedLayout();

    // Check total layout outputs.
    assertEquals(4, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    // Check host.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 300, sizeOutput.height), mountBounds);
    // Check NestedTree
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(25, 5, 275, 11), mountBounds);
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertEquals(new Rect(25, 5, 275, 11), mountBounds);
    assertTrue(getComponentAt(layoutState, 3) instanceof TestViewComponent);
    layoutState.getMountableOutputAt(3).getMountBounds(mountBounds);
    assertEquals(new Rect(28, 14, 28, 14), mountBounds);

    Mockito.validateMockitoUsage();
  }

  @Test
  public void testLayoutOutputWithCachedLayoutSpecDelegate() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final int widthSpecContainer = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    final int horizontalPadding = 20;
    final int widthMeasuredComponent = SizeSpec.makeSizeSpec(
        SizeSpec.getSize(widthSpecContainer) - horizontalPadding - horizontalPadding,
        SizeSpec.EXACTLY);

    final Component<?> componentSpy = PowerMockito.spy(
        TestLayoutComponent.create(c, 0, 0, true, true, true, true).build());
    Size sizeOutput = new Size();
    componentSpy.measure(
        c,
        widthMeasuredComponent,
        heightSpec,
        sizeOutput);

    // Check the cached measured component tree
    assertTrue(componentSpy.hasCachedLayout());
    final InternalNode cachedLayout = componentSpy.getCachedLayout();
    assertEquals(0, cachedLayout.getChildCount());
    assertTrue(cachedLayout.getComponent().getLifecycle() instanceof TestDrawableComponent);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .paddingPx(YogaEdge.HORIZONTAL, horizontalPadding)
            .child(componentSpy)
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        rootContainer,
        -1,
        widthSpecContainer,
        heightSpec);

    // Make sure we reused the cached layout and it wasn't released.
    verify(componentSpy, never()).releaseCachedLayout();
    verify(componentSpy, times(1)).clearCachedLayout();

    // Check total layout outputs.
    assertEquals(2, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    // Check host.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 300, sizeOutput.height), mountBounds);
    assertTrue(getComponentAt(layoutState, 1) instanceof TestDrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(20, 0, 280, 0), mountBounds);

    Mockito.validateMockitoUsage();
  }

  @Test
  public void testLayoutOutputWithCachedLayoutSpecWithMeasureDelegate() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final int widthSpecContainer = SizeSpec.makeSizeSpec(300, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    final int horizontalPadding = 20;
    final int widthMeasuredComponent = SizeSpec.makeSizeSpec(
        SizeSpec.getSize(widthSpecContainer) - horizontalPadding - horizontalPadding,
        SizeSpec.EXACTLY);

    final Component<?> sizeDependentComponentSpy = PowerMockito.spy(
        TestSizeDependentComponent.create(c)
            .setFixSizes(false)
            .setDelegate(true)
            .build());
    Size sizeOutput = new Size();
    sizeDependentComponentSpy.measure(
        c,
        widthMeasuredComponent,
        heightSpec,
        sizeOutput);

    // Check the cached measured component tree
    assertTrue(sizeDependentComponentSpy.hasCachedLayout());
    final InternalNode cachedLayout = sizeDependentComponentSpy.getCachedLayout();
    assertEquals(0, cachedLayout.getChildCount());
    assertTrue(cachedLayout.getComponent().getLifecycle() instanceof TestDrawableComponent);

    // Now embed the measured component in another container and calculate a layout.
    final Component rootContainer = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .paddingPx(YogaEdge.HORIZONTAL, horizontalPadding)
            .child(sizeDependentComponentSpy)
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        rootContainer,
        -1,
        widthSpecContainer,
        heightSpec);

    // Make sure we reused the cached layout and it wasn't released.
    verify(sizeDependentComponentSpy, never()).releaseCachedLayout();
    verify(sizeDependentComponentSpy, times(1)).clearCachedLayout();

    // Check total layout outputs.
    assertEquals(3, layoutState.getMountableOutputCount());
    Rect mountBounds = new Rect();
    // Check host.
    assertTrue(isHostComponent(getComponentAt(layoutState, 0)));
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 300, sizeOutput.height), mountBounds);
    // Check NestedTree
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(20, 0, 280, 0), mountBounds);
    assertTrue(getComponentAt(layoutState, 2) instanceof TestDrawableComponent);
    layoutState.getMountableOutputAt(2).getMountBounds(mountBounds);
    assertEquals(new Rect(20, 0, 280, 0), mountBounds);

    Mockito.validateMockitoUsage();
  }

  @Test
  public void testNestedTreeComponentWithDoubleMeasurementsDoesntThrow() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .flexDirection(YogaFlexDirection.ROW)
            .alignItems(YogaAlign.STRETCH)
            .paddingPx(YogaEdge.ALL, 2)
            .child(
                TestSizeDependentComponent.create(c)
                    .setFixSizes(true)
                    .setDelegate(false)
                    .withLayout().flexShrink(0)
                    .marginPx(YogaEdge.ALL, 11))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .heightPx(200)
                    .widthPx(200))
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
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                TestSizeDependentComponent.create(c)
                    .withLayout().flexShrink(0)
                    .widthPx(100)
                    .aspectRatio(1))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED));

    Rect mountBounds = new Rect();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 100, 100), mountBounds);
  }

  @Test
  public void testLayoutOutputForRootNestedTreeComponentWithPercentParentSizeDefined() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .alignItems(YogaAlign.FLEX_START)
            .widthPx(100)
            .heightPx(100)
            .child(
                TestSizeDependentComponent.create(c)
                    .withLayout().flexShrink(0)
                    .widthPercent(50)
                    .heightPercent(50)
                    .backgroundColor(0xFFFF0000))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED));

    Rect mountBounds = new Rect();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 100, 100), mountBounds);

    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 50, 50), mountBounds);
  }

  @Test
  public void testLayoutOutputForRootNestedTreeComponentWithPercent() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .alignItems(YogaAlign.FLEX_START)
            .child(
                TestSizeDependentComponent.create(c)
                    .setFixSizes(true)
                    .withLayout().flexShrink(0)
                    .widthPercent(50)
                    .heightPercent(50)
                    .backgroundColor(0xFFFF0000))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED));

    Rect mountBounds = new Rect();
    layoutState.getMountableOutputAt(0).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 60, 86), mountBounds);

    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
    layoutState.getMountableOutputAt(1).getMountBounds(mountBounds);
    assertEquals(new Rect(0, 0, 60, 86), mountBounds);
  }

  @Test
  public void testLayoutOutputForRootNestedTreeComponentWithPercentPadding() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .alignItems(YogaAlign.FLEX_START)
            .widthPx(50)
            .heightPx(50)
            .child(
                TestSizeDependentComponent.create(c)
                    .setFixSizes(true)
                    .withLayout().flexShrink(0)
                    .paddingPercent(YogaEdge.ALL, 10)
                    .backgroundColor(0xFFFF0000))
            .build();
      }
    };

    InternalNode root = LayoutState.createAndMeasureTreeForComponent(
        new ComponentContext(RuntimeEnvironment.application),
        component,
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED));

    assertEquals(5, root.getChildAt(0).getNestedTree().getPaddingLeft());
    assertEquals(5, root.getChildAt(0).getNestedTree().getPaddingTop());
    assertEquals(5, root.getChildAt(0).getNestedTree().getPaddingRight());
    assertEquals(5, root.getChildAt(0).getNestedTree().getPaddingBottom());
  }

  @Test
  public void testLayoutOutputsForComponentWithBorderColorNoBorderWidth() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .borderColor(Color.GREEN)
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    // No layout output generated related with borders
    // if borderColor is supplied but not borderWidth.
    assertEquals(2, layoutState.getMountableOutputCount());
  }

  @Test
  public void testLayoutOutputsForComponentWithBorderWidthNoBorderColor() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .borderWidthPx(YogaEdge.ALL, 10)
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    // No layout output generated related with borders
    // if borderWidth supplied but not borderColor.
    assertEquals(2, layoutState.getMountableOutputCount());
  }

  @Test
  public void testLayoutOutputsForComponentWithBorderWidthAllAndBorderColor() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .borderWidthPx(YogaEdge.ALL, 10)
            .borderColor(Color.GREEN)
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

    // Output at index 1 is BorderColorDrawable component.
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
  }

  @Test
  public void testLayoutOutputsForComponentWithBorderWidthTopAndBorderColor() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(TestDrawableComponent.create(c))
            .borderWidthPx(YogaEdge.TOP, 10)
            .borderColor(Color.GREEN)
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

    // Output at index 1 is BorderColorDrawable component.
    assertTrue(getComponentAt(layoutState, 1) instanceof DrawableComponent);
  }

  private void enableAccessibility() {
