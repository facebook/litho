/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Color;
import android.support.v4.view.ViewCompat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class InternalNodeTest {
  private static final int LIFECYCLE_TEST_ID = 1;

  private final ComponentLifecycle mLifecycle = new ComponentLifecycle() {
    @Override
    int getId() {
      return LIFECYCLE_TEST_ID;
    }
  };

  private static class TestComponent<L extends ComponentLifecycle> extends Component<L> {
    public TestComponent(L component) {
      super(component);
    }

    @Override
    public String getSimpleName() {
      return "TestComponent";
    }
  }

  private InternalNode mNode;

  @Before
  public void setup() {
    mNode = ComponentsPools.acquireInternalNode(
        new ComponentContext(RuntimeEnvironment.application),
        RuntimeEnvironment.application.getResources());
  }

  @Test
  public void testLayoutDirectionFlag() {
    mNode.layoutDirection(YogaDirection.INHERIT);
    assertTrue(isFlagSet(mNode, "PFLAG_LAYOUT_DIRECTION_IS_SET"));
    clearFlag(mNode, "PFLAG_LAYOUT_DIRECTION_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testAlignSelfFlag() {
    mNode.alignSelf(YogaAlign.STRETCH);
    assertTrue(isFlagSet(mNode, "PFLAG_ALIGN_SELF_IS_SET"));
    clearFlag(mNode, "PFLAG_ALIGN_SELF_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testPositionTypeFlag() {
    mNode.positionType(YogaPositionType.ABSOLUTE);
    assertTrue(isFlagSet(mNode, "PFLAG_POSITION_TYPE_IS_SET"));
    clearFlag(mNode, "PFLAG_POSITION_TYPE_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexFlag() {
    mNode.flex(1.5f);
    assertTrue(isFlagSet(mNode, "PFLAG_FLEX_IS_SET"));
    clearFlag(mNode, "PFLAG_FLEX_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexGrowFlag() {
    mNode.flexGrow(1.5f);
    assertTrue(isFlagSet(mNode, "PFLAG_FLEX_GROW_IS_SET"));
    clearFlag(mNode, "PFLAG_FLEX_GROW_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexShrinkFlag() {
    mNode.flexShrink(1.5f);
    assertTrue(isFlagSet(mNode, "PFLAG_FLEX_SHRINK_IS_SET"));
    clearFlag(mNode, "PFLAG_FLEX_SHRINK_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexBasisFlag() {
    mNode.flexBasisPx(1);
    assertTrue(isFlagSet(mNode, "PFLAG_FLEX_BASIS_IS_SET"));
    clearFlag(mNode, "PFLAG_FLEX_BASIS_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testImportantForAccessibilityFlag() {
    mNode.importantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    assertTrue(isFlagSet(mNode, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET"));
    clearFlag(mNode, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testDuplicateParentStateFlag() {
    mNode.duplicateParentState(false);
    assertTrue(isFlagSet(mNode, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET"));
    clearFlag(mNode, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMarginFlag() {
    mNode.marginPx(YogaEdge.ALL, 3);
    assertTrue(isFlagSet(mNode, "PFLAG_MARGIN_IS_SET"));
    clearFlag(mNode, "PFLAG_MARGIN_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testPaddingFlag() {
    mNode.paddingPx(YogaEdge.ALL, 3);
    assertTrue(isFlagSet(mNode, "PFLAG_PADDING_IS_SET"));
    clearFlag(mNode, "PFLAG_PADDING_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testBorderWidthFlag() {
    mNode.borderWidthPx(YogaEdge.ALL, 3);
    assertTrue(isFlagSet(mNode, "PFLAG_BORDER_WIDTH_IS_SET"));
    clearFlag(mNode, "PFLAG_BORDER_WIDTH_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testBorderColorFlag() {
    mNode.borderColor(Color.GREEN);
    assertTrue(isFlagSet(mNode, "PFLAG_BORDER_COLOR_IS_SET"));
    clearFlag(mNode, "PFLAG_BORDER_COLOR_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testPositionFlag() {
    mNode.positionPx(YogaEdge.ALL, 3);
    assertTrue(isFlagSet(mNode, "PFLAG_POSITION_IS_SET"));
    clearFlag(mNode, "PFLAG_POSITION_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testWidthFlag() {
    mNode.widthPx(4);
    assertTrue(isFlagSet(mNode, "PFLAG_WIDTH_IS_SET"));
    clearFlag(mNode, "PFLAG_WIDTH_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMinWidthFlag() {
    mNode.minWidthPx(4);
    assertTrue(isFlagSet(mNode, "PFLAG_MIN_WIDTH_IS_SET"));
    clearFlag(mNode, "PFLAG_MIN_WIDTH_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMaxWidthFlag() {
    mNode.maxWidthPx(4);
    assertTrue(isFlagSet(mNode, "PFLAG_MAX_WIDTH_IS_SET"));
    clearFlag(mNode, "PFLAG_MAX_WIDTH_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testHeightFlag() {
    mNode.heightPx(4);
    assertTrue(isFlagSet(mNode, "PFLAG_HEIGHT_IS_SET"));
    clearFlag(mNode, "PFLAG_HEIGHT_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMinHeightFlag() {
    mNode.minHeightPx(4);
    assertTrue(isFlagSet(mNode, "PFLAG_MIN_HEIGHT_IS_SET"));
    clearFlag(mNode, "PFLAG_MIN_HEIGHT_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMaxHeightFlag() {
    mNode.maxHeightPx(4);
    assertTrue(isFlagSet(mNode, "PFLAG_MAX_HEIGHT_IS_SET"));
    clearFlag(mNode, "PFLAG_MAX_HEIGHT_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testBackgroundFlag() {
    mNode.backgroundColor(0xFFFF0000);
    assertTrue(isFlagSet(mNode, "PFLAG_BACKGROUND_IS_SET"));
    clearFlag(mNode, "PFLAG_BACKGROUND_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testForegroundFlag() {
    mNode.foregroundColor(0xFFFF0000);
    assertTrue(isFlagSet(mNode, "PFLAG_FOREGROUND_IS_SET"));
    clearFlag(mNode, "PFLAG_FOREGROUND_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testAspectRatioFlag() {
    mNode.aspectRatio(1);
    assertTrue(isFlagSet(mNode, "PFLAG_ASPECT_RATIO_IS_SET"));
    clearFlag(mNode, "PFLAG_ASPECT_RATIO_IS_SET");
    assertEmptyFlags(mNode);
  }

  public void testTransitionKeyFlag() {
    mNode.transitionKey("key");
    assertTrue(isFlagSet(mNode, "PFLAG_TRANSITION_KEY_IS_SET"));
    assertTrue(mNode.isForceViewWrapping());
    clearFlag(mNode, "PFLAG_TRANSITION_KEY_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void setNestedTreeDoesntTransferLayoutDirectionIfExplicitlySetOnNestedNode() {
    InternalNode holderNode =
        ComponentsPools.acquireInternalNode(
            new ComponentContext(RuntimeEnvironment.application),
            RuntimeEnvironment.application.getResources());
    InternalNode nestedTree =
        ComponentsPools.acquireInternalNode(
            new ComponentContext(RuntimeEnvironment.application),
            RuntimeEnvironment.application.getResources());

    nestedTree.layoutDirection(YogaDirection.RTL);
    holderNode.calculateLayout();
    holderNode.setNestedTree(nestedTree);

    assertFalse(isFlagSet(holderNode, "PFLAG_LAYOUT_DIRECTION_IS_SET"));
    assertEquals(
        YogaDirection.INHERIT,
        holderNode.getStyleDirection());
    assertEquals(
        YogaDirection.RTL,
        nestedTree.getStyleDirection());
  }

  @Test
  public void testCopyIntoTrasferLayoutDirectionIfNotSetOnTheHolderOrOnTheNestedTree() {
    InternalNode holderNode =
        ComponentsPools.acquireInternalNode(
            new ComponentContext(RuntimeEnvironment.application),
            RuntimeEnvironment.application.getResources());
    InternalNode nestedTree =
        ComponentsPools.acquireInternalNode(
            new ComponentContext(RuntimeEnvironment.application),
            RuntimeEnvironment.application.getResources());

    holderNode.calculateLayout();
    holderNode.copyInto(nestedTree);

    assertFalse(isFlagSet(holderNode, "PFLAG_LAYOUT_DIRECTION_IS_SET"));
    assertTrue(isFlagSet(nestedTree, "PFLAG_LAYOUT_DIRECTION_IS_SET"));
  }

  @Test
  public void testCopyIntoNestedTreeTransferLayoutDirectionIfExplicitlySetOnHolderNode() {
    InternalNode holderNode =
        ComponentsPools.acquireInternalNode(
            new ComponentContext(RuntimeEnvironment.application),
            RuntimeEnvironment.application.getResources());
    InternalNode nestedTree =
        ComponentsPools.acquireInternalNode(
            new ComponentContext(RuntimeEnvironment.application),
            RuntimeEnvironment.application.getResources());

    holderNode.layoutDirection(YogaDirection.RTL);
    holderNode.calculateLayout();
    holderNode.copyInto(nestedTree);

    assertEquals(
        YogaDirection.RTL,
        nestedTree.getStyleDirection());
  }

  @Test
  public void testComponentCreateAndRetrieveCachedLayout() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final int unspecifiedSizeSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    final int exactSizeSpec = SizeSpec.makeSizeSpec(50, SizeSpec.EXACTLY);
    final Component<Text> textComponent = Text.create(c)
        .textSizePx(16)
        .text("test")
        .build();
    final Size textSize = new Size();
    textComponent.measure(
        c,
        exactSizeSpec,
        unspecifiedSizeSpec,
        textSize);

    assertTrue(textComponent.hasCachedLayout());
    InternalNode cachedLayout = textComponent.getCachedLayout();
    assertNotNull(cachedLayout);
    assertEquals(exactSizeSpec, cachedLayout.getLastWidthSpec());
    assertEquals(unspecifiedSizeSpec, cachedLayout.getLastHeightSpec());

    textComponent.clearCachedLayout();
    assertFalse(textComponent.hasCachedLayout());
  }

  @Test
  public void testContextSpecificComponentAssertionPasses() {
    InternalNode.assertContextSpecificStyleNotSet(mNode);
  }

  // @Test
  // public void testContextSpecificComponentAssertionFailFormatting() {
  //   final Component testComponent = new TestComponent<>(mLifecycle);
  //   mNode.alignSelf(YogaAlign.AUTO);
  //   mNode.flex(1f);
  //   mNode.setComponent(testComponent);
  // 
  //   String error = "";
  //   try {
  //     InternalNode.assertContextSpecificStyleNotSet(mNode);
  //   } catch (IllegalStateException e) {
  //     error = e.getMessage();
  //   }
  //
  //   assertTrue(
  //       "The error message contains the attributes set",
  //       error.contains("alignSelf, flex"));
  // }

