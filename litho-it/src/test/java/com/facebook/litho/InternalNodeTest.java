/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static android.graphics.Color.GREEN;
import static android.support.v4.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static com.facebook.litho.ComponentsPools.acquireInternalNode;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.yoga.YogaAlign.STRETCH;
import static com.facebook.yoga.YogaDirection.INHERIT;
import static com.facebook.yoga.YogaDirection.RTL;
import static com.facebook.yoga.YogaEdge.ALL;
import static com.facebook.yoga.YogaPositionType.ABSOLUTE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.getInternalState;
import static org.robolectric.RuntimeEnvironment.application;

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
    mNode.layoutDirection(INHERIT);
    assertThat(isFlagSet(mNode, "PFLAG_LAYOUT_DIRECTION_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_LAYOUT_DIRECTION_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testAlignSelfFlag() {
    mNode.alignSelf(STRETCH);
    assertThat(isFlagSet(mNode, "PFLAG_ALIGN_SELF_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_ALIGN_SELF_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testPositionTypeFlag() {
    mNode.positionType(ABSOLUTE);
    assertThat(isFlagSet(mNode, "PFLAG_POSITION_TYPE_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_POSITION_TYPE_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexFlag() {
    mNode.flex(1.5f);
    assertThat(isFlagSet(mNode, "PFLAG_FLEX_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_FLEX_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexGrowFlag() {
    mNode.flexGrow(1.5f);
    assertThat(isFlagSet(mNode, "PFLAG_FLEX_GROW_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_FLEX_GROW_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexShrinkFlag() {
    mNode.flexShrink(1.5f);
    assertThat(isFlagSet(mNode, "PFLAG_FLEX_SHRINK_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_FLEX_SHRINK_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexBasisFlag() {
    mNode.flexBasisPx(1);
    assertThat(isFlagSet(mNode, "PFLAG_FLEX_BASIS_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_FLEX_BASIS_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testImportantForAccessibilityFlag() {
    mNode.importantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    assertThat(isFlagSet(mNode, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testDuplicateParentStateFlag() {
    mNode.duplicateParentState(false);
    assertThat(isFlagSet(mNode, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMarginFlag() {
    mNode.marginPx(ALL, 3);
    assertThat(isFlagSet(mNode, "PFLAG_MARGIN_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_MARGIN_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testPaddingFlag() {
    mNode.paddingPx(ALL, 3);
    assertThat(isFlagSet(mNode, "PFLAG_PADDING_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_PADDING_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testBorderWidthFlag() {
    mNode.borderWidthPx(ALL, 3);
    assertThat(isFlagSet(mNode, "PFLAG_BORDER_WIDTH_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_BORDER_WIDTH_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testBorderColorFlag() {
    mNode.borderColor(GREEN);
    assertThat(isFlagSet(mNode, "PFLAG_BORDER_COLOR_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_BORDER_COLOR_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testPositionFlag() {
    mNode.positionPx(ALL, 3);
    assertThat(isFlagSet(mNode, "PFLAG_POSITION_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_POSITION_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testWidthFlag() {
    mNode.widthPx(4);
    assertThat(isFlagSet(mNode, "PFLAG_WIDTH_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_WIDTH_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMinWidthFlag() {
    mNode.minWidthPx(4);
    assertThat(isFlagSet(mNode, "PFLAG_MIN_WIDTH_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_MIN_WIDTH_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMaxWidthFlag() {
    mNode.maxWidthPx(4);
    assertThat(isFlagSet(mNode, "PFLAG_MAX_WIDTH_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_MAX_WIDTH_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testHeightFlag() {
    mNode.heightPx(4);
    assertThat(isFlagSet(mNode, "PFLAG_HEIGHT_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_HEIGHT_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMinHeightFlag() {
    mNode.minHeightPx(4);
    assertThat(isFlagSet(mNode, "PFLAG_MIN_HEIGHT_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_MIN_HEIGHT_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMaxHeightFlag() {
    mNode.maxHeightPx(4);
    assertThat(isFlagSet(mNode, "PFLAG_MAX_HEIGHT_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_MAX_HEIGHT_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testBackgroundFlag() {
    mNode.backgroundColor(0xFFFF0000);
    assertThat(isFlagSet(mNode, "PFLAG_BACKGROUND_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_BACKGROUND_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testForegroundFlag() {
    mNode.foregroundColor(0xFFFF0000);
    assertThat(isFlagSet(mNode, "PFLAG_FOREGROUND_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_FOREGROUND_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testAspectRatioFlag() {
    mNode.aspectRatio(1);
    assertThat(isFlagSet(mNode, "PFLAG_ASPECT_RATIO_IS_SET")).isTrue();
    clearFlag(mNode, "PFLAG_ASPECT_RATIO_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testTransitionKeyFlag() {
    mNode.transitionKey("key");
    assertThat(isFlagSet(mNode, "PFLAG_TRANSITION_KEY_IS_SET")).isTrue();
    assertThat(mNode.isForceViewWrapping()).isTrue();
    clearFlag(mNode, "PFLAG_TRANSITION_KEY_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void setNestedTreeDoesntTransferLayoutDirectionIfExplicitlySetOnNestedNode() {
    InternalNode holderNode =
        acquireInternalNode(
            new ComponentContext(application),
            application.getResources());
    InternalNode nestedTree =
        acquireInternalNode(
            new ComponentContext(application),
            application.getResources());

    nestedTree.layoutDirection(RTL);
    holderNode.calculateLayout();
    holderNode.setNestedTree(nestedTree);

    assertThat(isFlagSet(holderNode, "PFLAG_LAYOUT_DIRECTION_IS_SET")).isFalse();
    assertThat(holderNode.getStyleDirection()).isEqualTo(INHERIT);
    assertThat(nestedTree.getStyleDirection()).isEqualTo(RTL);
  }

  @Test
  public void testCopyIntoTrasferLayoutDirectionIfNotSetOnTheHolderOrOnTheNestedTree() {
    InternalNode holderNode =
        acquireInternalNode(
            new ComponentContext(application),
            application.getResources());
    InternalNode nestedTree =
        acquireInternalNode(
            new ComponentContext(application),
            application.getResources());

    holderNode.calculateLayout();
    holderNode.copyInto(nestedTree);

    assertThat(isFlagSet(holderNode, "PFLAG_LAYOUT_DIRECTION_IS_SET")).isFalse();
    assertThat(isFlagSet(nestedTree, "PFLAG_LAYOUT_DIRECTION_IS_SET")).isTrue();
  }

  @Test
  public void testCopyIntoNestedTreeTransferLayoutDirectionIfExplicitlySetOnHolderNode() {
    InternalNode holderNode =
        acquireInternalNode(
            new ComponentContext(application),
            application.getResources());
    InternalNode nestedTree =
        acquireInternalNode(
            new ComponentContext(application),
            application.getResources());

    holderNode.layoutDirection(RTL);
    holderNode.calculateLayout();
    holderNode.copyInto(nestedTree);

    assertThat(nestedTree.getStyleDirection()).isEqualTo(RTL);
  }

  @Test
  public void testComponentCreateAndRetrieveCachedLayout() {
    final ComponentContext c = new ComponentContext(application);
    final int unspecifiedSizeSpec = makeSizeSpec(0, UNSPECIFIED);
    final int exactSizeSpec = makeSizeSpec(50, EXACTLY);
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

    assertThat(textComponent.hasCachedLayout()).isTrue();
    InternalNode cachedLayout = textComponent.getCachedLayout();
    assertThat(cachedLayout).isNotNull();
    assertThat(cachedLayout.getLastWidthSpec()).isEqualTo(exactSizeSpec);
    assertThat(cachedLayout.getLastHeightSpec()).isEqualTo(unspecifiedSizeSpec);

    textComponent.clearCachedLayout();
    assertThat(textComponent.hasCachedLayout()).isFalse();
  }

  @Test
  public void testContextSpecificComponentAssertionPasses() {
    InternalNode.assertContextSpecificStyleNotSet(mNode);
  }

  @Test
  public void testContextSpecificComponentAssertionFailFormatting() {
    final Component testComponent = new TestComponent<>(mLifecycle);
    mNode.alignSelf(YogaAlign.AUTO);
    mNode.flex(1f);
    mNode.appendComponent(testComponent);

    String error = "";
    try {
      InternalNode.assertContextSpecificStyleNotSet(mNode);
    } catch (IllegalStateException e) {
      error = e.getMessage();
    }

    assertTrue(
        "The error message contains the attributes set",
        error.contains("alignSelf, flex"));
  }

  private static boolean isFlagSet(InternalNode internalNode, String flagName) {
    long flagPosition = Whitebox.getInternalState(InternalNode.class, flagName);
    long flags = Whitebox.getInternalState(internalNode, "mPrivateFlags");

    return ((flags & flagPosition) != 0);
  }

  private static void clearFlag(InternalNode internalNode, String flagName) {
    long flagPosition = Whitebox.getInternalState(InternalNode.class, flagName);
    long flags = Whitebox.getInternalState(internalNode, "mPrivateFlags");
    flags &= ~flagPosition;
    Whitebox.setInternalState(internalNode, "mPrivateFlags", flags);
  }

  private static void assertEmptyFlags(InternalNode internalNode) {
    assertThat(((long) getInternalState(internalNode, "mPrivateFlags")) == 0).isTrue();
  }
}
