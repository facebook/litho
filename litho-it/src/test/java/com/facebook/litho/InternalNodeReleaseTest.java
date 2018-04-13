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

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.yoga.YogaEdge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class InternalNodeReleaseTest {
  private InternalNode mInternalNode;

  @Before
  public void setup() {
    mInternalNode =
        ComponentsPools.acquireInternalNode(new ComponentContext(RuntimeEnvironment.application));
  }

  private static void assertDefaultValues(InternalNode node) {
    assertThat(node.isForceViewWrapping()).isEqualTo(false);

    assertThat(node.getNodeInfo()).isNull();
    assertThat(node.getTouchExpansion()).isNull();
    assertThat(node.getTestKey()).isNull();
  }

  @Test
  public void testDefaultValues() {
    assertDefaultValues(mInternalNode);

    mInternalNode.touchExpansionPx(YogaEdge.ALL, 1);
    mInternalNode.touchExpansionPx(YogaEdge.LEFT, 1);
    mInternalNode.touchExpansionPx(YogaEdge.TOP, 1);
    mInternalNode.touchExpansionPx(YogaEdge.RIGHT, 1);
    mInternalNode.touchExpansionPx(YogaEdge.BOTTOM, 1);
    mInternalNode.touchExpansionPx(YogaEdge.START, 1);
    mInternalNode.touchExpansionPx(YogaEdge.END, 1);
    mInternalNode.testKey("testkey");
    mInternalNode.wrapInView();
    mInternalNode.clickHandler(new EventHandler(null, 1));
    mInternalNode.longClickHandler(new EventHandler(null, 1));
    mInternalNode.focusChangeHandler(new EventHandler(null, 1));
    mInternalNode.touchHandler(new EventHandler(null, 1));
    mInternalNode.dispatchPopulateAccessibilityEventHandler(
        new EventHandler<DispatchPopulateAccessibilityEventEvent>(null, 1));
    mInternalNode.onInitializeAccessibilityEventHandler(
        new EventHandler<OnInitializeAccessibilityEventEvent>(null, 1));
    mInternalNode.onPopulateAccessibilityEventHandler(
        new EventHandler<OnPopulateAccessibilityEventEvent>(null, 1));
    mInternalNode.onInitializeAccessibilityNodeInfoHandler(
        new EventHandler<OnInitializeAccessibilityNodeInfoEvent>(null, 1));
    mInternalNode.onRequestSendAccessibilityEventHandler(
        new EventHandler<OnRequestSendAccessibilityEventEvent>(null, 1));
    mInternalNode.performAccessibilityActionHandler(
        new EventHandler<PerformAccessibilityActionEvent>(null, 1));
    mInternalNode.sendAccessibilityEventHandler(
        new EventHandler<SendAccessibilityEventEvent>(null, 1));
    mInternalNode.sendAccessibilityEventUncheckedHandler(
        new EventHandler<SendAccessibilityEventUncheckedEvent>(null, 1));

    mInternalNode.release();
    assertDefaultValues(mInternalNode);

    setup();
    assertDefaultValues(mInternalNode);
  }

  @Test(expected=IllegalStateException.class)
  public void testAttachedNode() {
    assertDefaultValues(mInternalNode);
    InternalNode parent =
        ComponentsPools.acquireInternalNode(new ComponentContext(RuntimeEnvironment.application));
    parent.addChildAt(mInternalNode, 0);
    mInternalNode.release();
  }

  @Test(expected=IllegalStateException.class)
  public void testNodeWithChildren() {
    assertDefaultValues(mInternalNode);
    mInternalNode.addChildAt(
        ComponentsPools.acquireInternalNode(new ComponentContext(RuntimeEnvironment.application)),
        0);
    mInternalNode.release();
  }
}
