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

import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class LithoViewTestHelperTest {
  @Before
  public void skipIfRelease() {
    Assume.assumeTrue(
        "These tests cover debug functionality and can only be run " + "for internal builds.",
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

    final LithoView lithoView = new LithoView(getApplicationContext());
    lithoView.setComponent(component);
    lithoView.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(0, UNSPECIFIED));

    final String string = LithoViewTestHelper.viewToString(lithoView);

    assertThat(string)
        .containsPattern(
            "litho.InlineLayout\\{\\w+ V.E..... .. 0,0-100,100\\}\n"
                + "  litho.TestDrawableComponent\\{\\w+ V.E..... .. 0,0-100,100\\}");
  }

  @Test
  public void viewToStringWithText() {
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
                .child(Text.create(c).widthPx(100).heightPx(100).text("Hello, World"))
                .build();
          }
        };

    final LithoView lithoView = new LithoView(getApplicationContext());
    lithoView.setComponent(component);
    lithoView.measure(makeMeasureSpec(0, UNSPECIFIED), makeMeasureSpec(0, UNSPECIFIED));
    lithoView.layout(0, 0, lithoView.getMeasuredWidth(), lithoView.getMeasuredHeight());

    final String string = LithoViewTestHelper.viewToString(lithoView);
    assertThat(string)
        .containsPattern(
            "litho.InlineLayout\\{\\w+ V.E..... .. 0,0-100,200\\}\n"
                + "  litho.Column\\{\\w+ V.E..... .. 0,0-100,200\\}\n"
                + "    litho.TestDrawableComponent\\{\\w+ V.E..... .. 0,0-100,100 litho:id/test-drawable\\}\n"
                + "    litho.Text\\{\\w+ V.E..... .. 0,100-100,200 text=\"Hello, World\"\\}");
  }
}
