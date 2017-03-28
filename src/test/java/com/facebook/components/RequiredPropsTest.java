/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class RequiredPropsTest {

  private ComponentContext mContext;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testWithoutRequiredProps() {
    String error = "";
    try {
      Text.create(mContext)
          .build();
    } catch (IllegalStateException e) {
      error = e.getMessage();
    }
    assertTrue(
        "Error message did not mention the missing required prop",
        error.contains("text"));
  }

  @Test
  public void testWithRequiredProps() {
    Text.create(mContext)
        .text("text")
        .build();
  }
}
