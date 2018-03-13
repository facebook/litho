/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.widget.Text;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class LithoViewTestHelperTest {
  @Before
  public void skipIfRelease() {
    Assume.assumeTrue("These tests cover debug functionality and can only be run " +
            "for internal builds.",
        ComponentsConfiguration.IS_INTERNAL_BUILD);
  }

  @Test
  public void testBasicViewToString() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).widthPx(100).heightPx(100).build();
          }
        };

    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final ComponentTree componentTree = ComponentTree.create(c, component)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();

    final LithoView lithoView = new LithoView(RuntimeEnvironment.application);
    lithoView.setComponentTree(componentTree);
    lithoView.measure(
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, UNSPECIFIED));

    final String string = LithoViewTestHelper.viewToString(lithoView);

    assertThat(string)
        .containsPattern(
            "litho.InlineLayout\\{\\w+ V.E..... .. 0,0-100,100\\}\n"
                + "  litho.TestDrawableComponent\\{\\w+ V.E..... .. 0,0-100,100\\}");
  }

  @Test
  public void testViewToStringWithText() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(
                    TestDrawableComponent.create(c)
                        .testKey("test-drawable")
                        .widthPx(100)
                        .heightPx(100))
                .child(Text.create(c).text("Hello, World"))
                .build();
          }
        };

    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    final ComponentTree componentTree = ComponentTree.create(c, component)
        .incrementalMount(false)
        .layoutDiffing(false)
        .build();

    final LithoView lithoView = new LithoView(RuntimeEnvironment.application);
    lithoView.setComponentTree(componentTree);
    lithoView.measure(
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, UNSPECIFIED));
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    final String string = LithoViewTestHelper.viewToString(lithoView);
    assertThat(string)
        .containsPattern(
            "litho.InlineLayout\\{\\w+ V.E..... .. 0,0-100,100\\}\n"
                + "  litho.TestDrawableComponent\\{\\w+ V.E..... .. 0,0-100,100 litho:id/test-drawable\\}\n"
                + "  litho.Text\\{\\w+ V.E..... .. 0,100-100,100 text=\"Hello, World\"\\}");
  }
}
