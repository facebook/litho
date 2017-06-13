/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Rect;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests {@link ViewNodeInfo}
 */
@RunWith(ComponentsTestRunner.class)
public class ViewNodeInfoTest {

  private ViewNodeInfo mViewNodeInfo;

  @Before
  public void setup() {
    mViewNodeInfo = ViewNodeInfo.acquire();
  }

  @Test
  public void testTouchBoundsNoHostTranslation() {
    final InternalNode node = new TouchExpansionTestInternalNode();

    mViewNodeInfo.setExpandedTouchBounds(node, 10, 10, 20, 20);

    assertThat(mViewNodeInfo.getExpandedTouchBounds()).isEqualTo(new Rect(9, 8, 23, 24));
  }

  @Test
  public void testRefCountAcquiringReleasedNode() {
    ViewNodeInfo viewNodeInfo = ViewNodeInfo.acquire();

    viewNodeInfo.acquireRef();
    viewNodeInfo.release();
    viewNodeInfo.release(); // Now it should be back in the pool.

    try {
      viewNodeInfo.acquireRef();
      fail("Acquiring ref of a ViewNodeInfo already released to the pool.");
    } catch (Exception e) {
      // Expected exception.
    }

    // Drain pool of bad ViewNodeInfo instances for subsequent tests.
    clearViewNodeInfoPool();
  }

  @Test
  public void testRefCountDoubleReleasingToPool() {
    ViewNodeInfo viewNodeInfo = ViewNodeInfo.acquire();

    viewNodeInfo.acquireRef();
    viewNodeInfo.release();
    viewNodeInfo.release(); // Now it should be back in the pool.

    try {
      viewNodeInfo.release();
      fail("Releasing a ViewNodeInfo that is already in the pool.");
    } catch (Exception e) {
      // Expected exception.
    }

    // Drain pool of bad ViewNodeInfo instances for subsequent tests.
    clearViewNodeInfoPool();
  }

  private static void clearViewNodeInfoPool() {
    final RecyclePool<NodeInfo> viewNodeInfoPool =
        Whitebox.getInternalState(ComponentsPools.class, "sViewNodeInfoPool");

    while (viewNodeInfoPool.acquire() != null) {
      // Run.
    }
  }
}
