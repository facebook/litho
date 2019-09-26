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

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.view.View;
import com.facebook.litho.LayoutState.LayoutStateReferenceWrapper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.litho.testing.util.InlineLayoutWithSizeSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class WillRenderTest {

  private final InlineLayoutSpec mNullSpec =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayout(ComponentContext c) {
          return null;
        }
      };

  private final InlineLayoutSpec mNonNullSpec =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayout(ComponentContext c) {
          return Row.create(c).build();
        }
      };

  private final InlineLayoutWithSizeSpec mLayoutWithSizeSpec =
      new InlineLayoutWithSizeSpec() {

        @Override
        protected Component onCreateLayoutWithSizeSpec(
            ComponentContext c, int widthSpec, int heightSpec) {
          return Row.create(c)
              .widthDip(View.MeasureSpec.getSize(widthSpec))
              .heightDip(View.MeasureSpec.getSize(heightSpec))
              .build();
        }
      };

  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void testWillRenderForComponentThatReturnsNull() {
    ComponentContext c = new ComponentContext(application);
    c.setLayoutStateReferenceWrapper(LayoutStateReferenceWrapper.getTestInstance(c));
    assertThat(c, Wrapper.create(c).delegate(mNullSpec).build()).wontRender();
  }

  @Test
  public void testWillRenderForComponentThatReturnsNonNull() {
    ComponentContext c = new ComponentContext(application);
    c.setLayoutStateReferenceWrapper(LayoutStateReferenceWrapper.getTestInstance(c));
    assertThat(c, Wrapper.create(c).delegate(mNonNullSpec).build()).willRender();
  }

  @Test
  public void testWillRenderForComponentWithSizeSpecThrowsException() {
    mExpectedException.expect(IllegalArgumentException.class);
    mExpectedException.expectMessage("@OnCreateLayoutWithSizeSpec");

    ComponentContext c = new ComponentContext(application);
    c.setLayoutStateReferenceWrapper(LayoutStateReferenceWrapper.getTestInstance(c));
    Component.willRender(c, Wrapper.create(c).delegate(mLayoutWithSizeSpec).build());
  }
}
