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

import static junit.framework.Assert.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

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
