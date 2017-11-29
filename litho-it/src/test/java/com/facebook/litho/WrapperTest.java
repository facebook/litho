/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.ComponentContext.NULL_LAYOUT;
import static com.facebook.litho.ComponentLayout.Builder;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class WrapperTest {

  @Test
  public void testWrapperWithNullComponentReturnsNullLayout() {
    ComponentContext c = new ComponentContext(application);
    Builder builder = Wrapper.create(c).delegate(null);
    assertThat(NULL_LAYOUT).isEqualTo(builder.build());
  }
}
