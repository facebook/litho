/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.litho.Column.create;
import static com.facebook.litho.testing.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(ComponentsTestRunner.class)
public class MountStateViewClickTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testInnerComponentHostClickable() {
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
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
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .longClickHandler(c.newEventHandler(1))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    assertThat(lithoView.isLongClickable()).isTrue();
  }
}
