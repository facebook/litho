/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class LayoutTest {

  @Test
  public void testLayoutWithNullComponentReturnsNullLayout() {
    ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    ComponentLayout.Builder builder = Layout.create(c, null);
    assertEquals(builder.build(), c.NULL_LAYOUT);
  }
}
