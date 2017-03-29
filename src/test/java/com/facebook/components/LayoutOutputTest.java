/**
 * Copyright (c) 2014-present, Facebook, Inc.
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

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class LayoutOutputTest {

  private static final int LIFECYCLE_TEST_ID = 1;
  private static final int LEVEL_TEST = 1;
  private static final int SEQ_TEST = 1;
  private static final int MAX_LEVEL_TEST = 255;
  private static final int MAX_SEQ_TEST = 65535;

  private static class TestComponent<L extends ComponentLifecycle> extends Component<L> {
    public TestComponent(L component) {
      super(component);
    }

    @Override
    public String getSimpleName() {
      return "TestComponent";
    }
  }

  private final ComponentLifecycle mLifecycle = new ComponentLifecycle() {
    @Override
    int getId() {
      return LIFECYCLE_TEST_ID;
    }
  };
  private LayoutOutput mLayoutOutput;

  @Before
  public void setup() {
    mLayoutOutput = new LayoutOutput();
  }

  @Test
  public void testPositionAndSizeSet() {
    mLayoutOutput.setBounds(0, 1, 3, 4);
    assertEquals(0, mLayoutOutput.getBounds().left);
    assertEquals(1, mLayoutOutput.getBounds().top);
    assertEquals(3, mLayoutOutput.getBounds().right);
    assertEquals(4, mLayoutOutput.getBounds().bottom);
  }

  @Test
  public void testHostMarkerSet() {
