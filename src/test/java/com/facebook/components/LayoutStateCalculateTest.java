/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.components.testing.TestComponent;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestLayoutComponent;
import com.facebook.components.testing.TestNullLayoutComponent;
import com.facebook.components.testing.TestSizeDependentComponent;
import com.facebook.components.testing.TestViewComponent;
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
import static com.facebook.components.NodeInfo.FOCUS_SET_TRUE;
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
        return Container.create(c)
            .child(
                Container.create(c)
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
        return Container.create(c)
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
        return Container.create(c)
            .child(
                Container.create(c)
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
        return Container.create(c)
            .child(
                Container.create(c)
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
        return Container.create(c)
            .child(
                Container.create(c)
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
        return Container.create(c)
            .backgroundColor(0xFFFF0000)
            .child(
                Container.create(c)
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
                Container.create(c)
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
                            .withLayout()
                            .wrapInView()
                            .paddingPx(YogaEdge.ALL, paddingSize))
                    .child(
                        TestViewComponent.create(c)
                            .withLayout()
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
        return Container.create(c)
            .widthPx(30)
            .heightPx(30)
            .wrapInView()
            .child(
                Container.create(c)
                    .widthPx(10)
                    .heightPx(10)
                    .marginPx(YogaEdge.ALL, 10)
                    .wrapInView()
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
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

