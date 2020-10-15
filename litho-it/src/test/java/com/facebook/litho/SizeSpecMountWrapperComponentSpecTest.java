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
import static org.hamcrest.CoreMatchers.isA;

import android.graphics.Color;
import android.widget.FrameLayout;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SizeSpecMountWrapperComponent;
import com.facebook.litho.widget.SizeTreePropComponent;
import com.facebook.litho.widget.SolidColor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class SizeSpecMountWrapperComponentSpecTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext(), new StateHandler());
    ErrorBoundariesConfiguration.rootWrapperComponentFactory = null;
  }

  @Test
  public void testViewHierarchy() {
    LithoView lithoView =
        ComponentTestHelper.mountComponent(
            mContext,
            SizeSpecMountWrapperComponent.create(mContext).component(Row.create(mContext)).build());
    // This should be the FrameLayout that fixes the commons props problem.
    // TODO: T59446191
    assertThat(lithoView.getChildAt(0)).isInstanceOf(FrameLayout.class);
    // This is the LithoView that holds the new tree after the MountSpec.
    assertThat(((FrameLayout) lithoView.getChildAt(0)).getChildAt(0)).isInstanceOf(LithoView.class);
  }

  @Test
  public void testThrowErrorWithoutUsingSizeSpecMountWrapper() {
    thrown.expect(LithoMetadataExceptionWrapper.class);
    thrown.expectCause(isA(NullPointerException.class));
    // Throws an exception
    ComponentTestHelper.mountComponent(mContext, SizeTreePropComponent.create(mContext).build());

    // Does not throw an exception
    ComponentTestHelper.mountComponent(
        mContext,
        SizeSpecMountWrapperComponent.create(mContext)
            .component(SizeTreePropComponent.create(mContext))
            .build());
  }

  @Test
  public void testDifferentSizesDependingOnAvailableSpace() {
    // Test component with enough width. Throws no exception
    ComponentTestHelper.mountComponent(mContext, getTestComponent(50), 1000, 1000);

    // Mount the same component but with different width and height constraints that would throw an
    // IllegalStateException.
    thrown.expect(LithoMetadataExceptionWrapper.class);
    thrown.expectCause(isA(IllegalStateException.class));

    // WidthPercentage would make the width of the wrapped component less than 400, and for the
    // height we just pass a smaller viewport (this would test both onMeasure and onBoundsDefined)
    ComponentTestHelper.mountComponent(mContext, getTestComponent(80), 1000, 300);
  }

  private Component getTestComponent(int widthPercentage) {
    return Row.create(mContext)
        .child(SolidColor.create(mContext).color(Color.RED).widthPercent(widthPercentage))
        .child(
            SizeSpecMountWrapperComponent.create(mContext)
                .component(SizeTreePropComponent.create(mContext)))
        .build();
  }
}
