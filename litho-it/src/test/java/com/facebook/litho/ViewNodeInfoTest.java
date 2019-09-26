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

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link ViewNodeInfo} */
@RunWith(ComponentsTestRunner.class)
public class ViewNodeInfoTest {

  private ViewNodeInfo mViewNodeInfo;

  @Before
  public void setup() {
    mViewNodeInfo = new ViewNodeInfo();
  }

  @Test
  public void testTouchBoundsNoHostTranslation() {
    final InternalNode node =
        new TouchExpansionTestInternalNode(new ComponentContext(RuntimeEnvironment.application));

    mViewNodeInfo.setExpandedTouchBounds(node, 10, 10, 20, 20);

    assertThat(mViewNodeInfo.getExpandedTouchBounds()).isEqualTo(new Rect(9, 8, 23, 24));
  }
}
