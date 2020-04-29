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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateSizeTest {
  private static final int COMPONENT_ID = 37;
  private static final int WIDTH = 49;
  private static final int HEIGHT = 51;

  private LayoutState mLayoutState;
  private Component mComponent;
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    mComponent = TestLayoutComponent.create(mContext).build();
    Whitebox.setInternalState(mComponent, "mId", COMPONENT_ID);

    mLayoutState = new LayoutState(mContext);
    Whitebox.setInternalState(mLayoutState, "mWidth", WIDTH);
    Whitebox.setInternalState(mLayoutState, "mHeight", HEIGHT);
    Whitebox.setInternalState(mLayoutState, "mComponent", mComponent);
  }

  @Test
  public void testCompatibleSize() {
    assertThat(mLayoutState.isCompatibleSize(WIDTH, HEIGHT)).isTrue();
  }

  @Test
  public void testIncompatibleWidthSpec() {
    assertThat(mLayoutState.isCompatibleSize(WIDTH + 1000, HEIGHT)).isFalse();
  }

  @Test
  public void testIncompatibleHeightSpec() {
    assertThat(mLayoutState.isCompatibleSize(WIDTH, HEIGHT + 1000)).isFalse();
  }
}
