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
