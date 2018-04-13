/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeDumpingHelperTest {

  @Before
  public void skipIfRelease() {
    Assume.assumeTrue(
        "These tests cover debug functionality and can only be run " + "for internal builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD);
  }

  @Test
  public void testBasicComponentTreeDumping() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).widthPx(100).heightPx(100).build();
          }
        };

    ComponentContext componentContext = new ComponentContext(RuntimeEnvironment.application);
    final ComponentTree componentTree =
        ComponentTree.create(componentContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
            .build();

    componentContext = ComponentContext.withComponentTree(componentContext, componentTree);

    final LithoView lithoView = new LithoView(RuntimeEnvironment.application);
    lithoView.setComponentTree(componentTree);
    lithoView.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(0, UNSPECIFIED));

    final String string = ComponentTreeDumpingHelper.dumpContextTree(componentContext);
    assertThat(string).containsPattern("InlineLayout\\{V}\n" + "  TestDrawableComponent\\{V}");
  }
}
