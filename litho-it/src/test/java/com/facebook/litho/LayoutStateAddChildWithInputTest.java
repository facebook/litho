/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateAddChildWithInputTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mContext.setLayoutStateReferenceWrapperForTesting();
  }

  @Test
  public void testNewEmptyLayout() {
    Column component =
        Column.create(mContext)
            .child(TestLayoutComponent.create(mContext))
            .child(TestLayoutComponent.create(mContext))
            .build();

    InternalNode node = (InternalNode) component.resolve(mContext);

    assertThat(node.getChildCount()).isEqualTo(2);
    assertThat(node.getChildAt(0).getChildCount()).isEqualTo(0);
    assertThat(node.getChildAt(1).getChildCount()).isEqualTo(0);
  }
}
