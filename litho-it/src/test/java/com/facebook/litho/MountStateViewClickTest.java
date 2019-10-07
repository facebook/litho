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

import static com.facebook.litho.Column.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class MountStateViewClickTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testInnerComponentHostClickable() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(
                        create(c)
                            .clickHandler(c.newEventHandler(1))
                            .child(TestViewComponent.create(c)))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.isClickable()).isFalse();

    ComponentHost innerHost = (ComponentHost) lithoView.getChildAt(0);
    assertThat(innerHost.isClickable()).isTrue();
  }

  @Test
  public void testInnerComponentHostClickableWithLongClickHandler() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(
                        create(c)
                            .longClickHandler(c.newEventHandler(1))
                            .child(TestViewComponent.create(c)))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.isClickable()).isFalse();

    ComponentHost innerHost = (ComponentHost) lithoView.getChildAt(0);
    assertThat(innerHost.isLongClickable()).isTrue();
  }

  @Test
  public void testRootHostClickable() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .clickHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    assertThat(lithoView.isClickable()).isTrue();
  }

  @Test
  public void testRootHostClickableWithLongClickHandler() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .longClickHandler(c.newEventHandler(1))
                    .child(TestDrawableComponent.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    assertThat(lithoView.isLongClickable()).isTrue();
  }

  @Test
  public void testRootHostClickableUnmount() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            Column.create(mContext)
                .clickHandler(mContext.newEventHandler(1))
                .longClickHandler(mContext.newEventHandler(2))
                .child(TestDrawableComponent.create(mContext))
                .build(),
            true);

    assertThat(lithoView.isClickable()).isTrue();
    assertThat(lithoView.isLongClickable()).isTrue();

    lithoView.unmountAllItems();

    assertThat(lithoView.isClickable()).isFalse();
    assertThat(lithoView.isLongClickable()).isFalse();
  }
}
