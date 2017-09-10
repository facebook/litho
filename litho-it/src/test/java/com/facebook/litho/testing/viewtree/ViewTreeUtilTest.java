/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.viewtree;

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test for {@link ViewTreeUtil} */
@RunWith(ComponentsTestRunner.class)
public class ViewTreeUtilTest {
  @Test
  public void testResourceFound() {
    final String resourceName = ViewTreeUtil.getResourceName(android.R.drawable.bottom_bar);
    assertThat(resourceName).isEqualTo("bottom_bar");
  }

  @Test
  public void testResourceNotFound() {
    final String resourceName = ViewTreeUtil.getResourceName(Integer.MAX_VALUE);
    assertThat(resourceName).isEqualTo("<undefined>");
  }
}
