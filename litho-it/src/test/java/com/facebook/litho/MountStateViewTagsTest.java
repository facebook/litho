/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.util.SparseArray;
import android.view.View;

import com.facebook.litho.testing.TestDrawableComponent;
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
public class MountStateViewTagsTest {
  private static final int DUMMY_ID = 0x10000000;

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testInnerComponentHostViewTags() {
    final Object tag1 = new Object();
    final SparseArray<Object> tags1 = new SparseArray<>(1);
    tags1.put(DUMMY_ID, tag1);

    final Object tag2 = new Object();
    final SparseArray<Object> tags2 = new SparseArray<>(1);
    tags2.put(DUMMY_ID, tag2);

    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .viewTags(tags1)
                        .child(TestDrawableComponent.create(c))
                        .child(TestDrawableComponent.create(c)))
                .child(TestDrawableComponent.create(c))
                .child(
                    TestDrawableComponent.create(c)
                        .withLayout()
                        .viewTags(tags2))
                .build();
          }
        });

    final View innerHost1 = lithoView.getChildAt(0);
    final View innerHost2 = lithoView.getChildAt(1);

    assertThat(innerHost1.getTag(DUMMY_ID)).isEqualTo(tag1);
    assertThat(innerHost2.getTag(DUMMY_ID)).isEqualTo(tag2);
  }

  @Test
  public void testRootHostViewTags() {
    final Object tag = new Object();
    final SparseArray<Object> tags = new SparseArray<>(1);
    tags.put(DUMMY_ID, tag);

    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .viewTags(tags)
                .child(TestDrawableComponent.create(c))
                .child(TestDrawableComponent.create(c))
                .build();
          }
        });

    assertThat(lithoView.getTag(DUMMY_ID)).isEqualTo(tag);
  }
}
