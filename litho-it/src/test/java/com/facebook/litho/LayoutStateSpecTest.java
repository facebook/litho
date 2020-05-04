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
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutStateSpecTest {

  private static final int COMPONENT_ID = 37;

  private int mWidthSpec;
  private int mHeightSpec;
  private LayoutState mLayoutState;
  private Component mComponent;
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    mWidthSpec = SizeSpec.makeSizeSpec(39, SizeSpec.EXACTLY);
    mHeightSpec = SizeSpec.makeSizeSpec(41, SizeSpec.EXACTLY);
    mComponent = TestLayoutComponent.create(mContext).build();
    Whitebox.setInternalState(mComponent, "mId", COMPONENT_ID);

    mLayoutState = new LayoutState(mContext);
    Whitebox.setInternalState(mLayoutState, "mComponent", mComponent);
    Whitebox.setInternalState(mLayoutState, "mWidthSpec", mWidthSpec);
    Whitebox.setInternalState(mLayoutState, "mHeightSpec", mHeightSpec);
  }

  @Test
  public void testCompatibleInputAndSpec() {
    assertThat(mLayoutState.isCompatibleComponentAndSpec(COMPONENT_ID, mWidthSpec, mHeightSpec))
        .isTrue();
  }

  @Test
  public void testIncompatibleInput() {
    assertThat(
            mLayoutState.isCompatibleComponentAndSpec(COMPONENT_ID + 1000, mWidthSpec, mHeightSpec))
        .isFalse();
  }

  @Test
  public void testIncompatibleWidthSpec() {
    assertThat(
            mLayoutState.isCompatibleComponentAndSpec(COMPONENT_ID, mWidthSpec + 1000, mHeightSpec))
        .isFalse();
  }

  @Test
  public void testIncompatibleHeightSpec() {
    assertThat(
            mLayoutState.isCompatibleComponentAndSpec(COMPONENT_ID, mWidthSpec, mHeightSpec + 1000))
        .isFalse();
  }
}
