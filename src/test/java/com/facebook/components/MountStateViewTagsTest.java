/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.util.SparseArray;
import android.view.View;

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

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

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .child(
                    Container.create(c)
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

