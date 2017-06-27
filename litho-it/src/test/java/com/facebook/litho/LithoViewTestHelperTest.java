/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(ComponentsTestRunner.class)
public class LithoViewTestHelperTest {
  private LithoView mLithoView;

  @Before
  public void skipIfRelease() {
    Assume.assumeTrue("These tests cover debug functionality and can only be run " +
            "for internal builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD);
  }

  @Before
  public void setup() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return TestDrawableComponent.create(c)
            .withLayout()
            .widthPx(100)
            .heightPx(100)
            .build();
      }
    };

    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final ComponentTree componentTree = ComponentTree.create(c, component)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();

    mLithoView = new LithoView(RuntimeEnvironment.application);
    mLithoView.setComponentTree(componentTree);
  }

  @Test
  public void testBasicViewToString() {
    mLithoView.measure(
        makeMeasureSpec(0, UNSPECIFIED),
        makeMeasureSpec(0, UNSPECIFIED));

    final String string = LithoViewTestHelper.viewToString(mLithoView);

    assertThat(string).isEqualTo(
        "Lifecycle{0, 0 - 100, 100}\n" +
        "  TestDrawableComponent{0, 0 - 100, 100}");
  }
}
