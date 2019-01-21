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

package com.facebook.litho.widget;

import static com.facebook.litho.testing.assertj.ComponentConditions.typeIs;
import static com.facebook.litho.testing.assertj.SubComponentDeepExtractor.deepSubComponentWith;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.assertj.ComponentAssert;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link LazySelectorComponentSpec}. */
@RunWith(ComponentsTestRunner.class)
public class LazySelectorComponentSpecTest {

  private final InlineLayoutSpec mNullSpec =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayout(ComponentContext c) {
          return null;
        }
      };

  private ComponentContext mContext;

  @Before
  public void setUp() throws Exception {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testFirstComponentSelected() throws Exception {
    ComponentAssert.assertThat(
            LazySelectorComponent.create(mContext)
                .component(
                    new ComponentCreator() {
                      @Override
                      public Component create() {
                        return Image.create(mContext).drawable(null).build();
                      }
                    })
                .component(
                    new ComponentCreator() {
                      @Override
                      public Component create() {
                        return Text.create(mContext).text("Hello World").build();
                      }
                    }))
        .has(deepSubComponentWith(mContext, typeIs(Image.class)));
  }

  @Test
  public void testSubsequentComponentNotCreated() throws Exception {
    ComponentCreator second = mock(ComponentCreator.class);

    ComponentAssert.assertThat(
            LazySelectorComponent.create(mContext)
                .component(
                    new ComponentCreator() {
                      @Override
                      public Component create() {
                        return Image.create(mContext).drawable(null).build();
                      }
                    })
                .component(second))
        .has(deepSubComponentWith(mContext, typeIs(Image.class)));

    verify(second, never()).create();
  }

  @Test
  public void testNullArgument() throws Exception {
    ComponentAssert.assertThat(
            LazySelectorComponent.create(mContext)
                .component(null)
                .component(
                    new ComponentCreator() {
                      @Override
                      public Component create() {
                        return Image.create(mContext).drawable(null).build();
                      }
                    })
                .component(
                    new ComponentCreator() {
                      @Override
                      public Component create() {
                        return Text.create(mContext).text("Hello World").build();
                      }
                    }))
        .has(deepSubComponentWith(mContext, typeIs(Image.class)));
  }

  @Test
  public void testNullLayoutSkipped() throws Exception {
    ComponentAssert.assertThat(
            LazySelectorComponent.create(mContext)
                .component(
                    new ComponentCreator() {
                      @Override
                      public Component create() {
                        return mNullSpec;
                      }
                    })
                .component(
                    new ComponentCreator() {
                      @Override
                      public Component create() {
                        return Image.create(mContext).drawable(null).build();
                      }
                    })
                .component(
                    new ComponentCreator() {
                      @Override
                      public Component create() {
                        return Text.create(mContext).text("Hello World").build();
                      }
                    }))
        .has(deepSubComponentWith(mContext, typeIs(Image.class)));
  }

  @Test
  public void testEmpty() throws Exception {
    ComponentAssert.assertThat(
            LazySelectorComponent.create(mContext)
                .component(
                    new ComponentCreator() {
                      @Override
                      public Component create() {
                        return mNullSpec;
                      }
                    })
                .component(null))
        .willNotRender();
  }

  @Test
  public void testAllNull() throws Exception {
    ComponentAssert.assertThat(
            LazySelectorComponent.create(mContext).component(null).component(null))
        .willNotRender();
  }
}
