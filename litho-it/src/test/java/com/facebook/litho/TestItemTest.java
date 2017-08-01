/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class TestItemTest {
  private TestItem mTestItem;

  @Before
  public void setup() {
    mTestItem = new TestItem();
  }

  @Test
  public void testPositionAndSizeSet() {
    mTestItem.setBounds(0, 1, 3, 4);

    assertThat(mTestItem.getBounds().left).isEqualTo(0);
    assertThat(mTestItem.getBounds().top).isEqualTo(1);
    assertThat(mTestItem.getBounds().right).isEqualTo(3);
    assertThat(mTestItem.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testRectBoundsSet() {
    final Rect bounds = new Rect(0, 1, 3, 4);
    mTestItem.setBounds(bounds);
    assertThat(mTestItem.getBounds().left).isEqualTo(0);
    assertThat(mTestItem.getBounds().top).isEqualTo(1);
    assertThat(mTestItem.getBounds().right).isEqualTo(3);
    assertThat(mTestItem.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testRelease() {
    final Rect bounds = new Rect(0, 1, 3, 4);
    mTestItem.setBounds(bounds);
    mTestItem.setHost(mock(ComponentHost.class));

    mTestItem.release();
    assertThat(mTestItem.getTextContent()).isEmpty();
    assertThat(mTestItem.getBounds()).isEqualTo(new Rect());
    assertThat(mTestItem.getTestKey()).isNull();
    assertThat(mTestItem.getHost()).isNull();
  }

  @Test
  public void testPooling() {
    final TestItem testItem = ComponentsPools.acquireTestItem();

    final Rect bounds = new Rect(0, 1, 3, 4);
    testItem.setBounds(bounds);
    testItem.setHost(mock(ComponentHost.class));

    ComponentsPools.release(testItem);
    assertThat(testItem.getTextContent()).isEmpty();
    assertThat(testItem.getBounds()).isEqualTo(new Rect());
    assertThat(testItem.getTestKey()).isNull();
    assertThat(testItem.getHost()).isNull();
  }
}
