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

  @Before
  public void setup() {
  }

  @Test
  public void testBasicViewToString() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return TestDrawableComponent.create(c)
            .widthPx(100)
            .heightPx(100)
            .buildWithLayout();
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
        makeMeasureSpec(0, UNSPECIFIED),
        makeMeasureSpec(0, UNSPECIFIED));

    final String string = LithoViewTestHelper.viewToString(lithoView);

    assertThat(string).isEqualTo(
        "Lifecycle{0, 0 - 100, 100}\n" +
        "  TestDrawableComponent{0, 0 - 100, 100}");
  }

  @Test
  public void testViewToStringWithText() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Column.create(c)
            .child(
                TestDrawableComponent.create(c)
                    .withLayout()
                    .testKey("test-drawable")
                    .widthPx(100)
                    .heightPx(100))
            .child(
                Text.create(c)
                    .text("Hello, World")).build();
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
        makeMeasureSpec(0, UNSPECIFIED),
        makeMeasureSpec(0, UNSPECIFIED));
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    final String string = LithoViewTestHelper.viewToString(lithoView);
    assertThat(string).isEqualTo("Lifecycle{0, 0 - 100, 100}\n" +
        "  TestDrawableComponent{0, 0 - 100, 100 testKey=\"test-drawable\"}\n" +
        "  Text{0, 100 - 100, 100 text=\"Hello, World\"}");
  }
}
