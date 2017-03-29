/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.util.LongSparseArray;
import android.support.v4.util.Pools;

import com.facebook.components.testing.ComponentTestHelper;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestComponent;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestViewComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class MountStateRemountTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testRemountSameLayoutState() {
    final TestComponent component1 = TestDrawableComponent.create(mContext)
        .build();
    final TestComponent component2 = TestDrawableComponent.create(mContext)
        .build();
    final TestComponent component3 = TestDrawableComponent.create(mContext)
        .build();
    final TestComponent component4 = TestDrawableComponent.create(mContext)
        .build();

    final ComponentView componentView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .child(component1)
                .child(component2)
                .build();
          }
        });

    assertTrue(component1.isMounted());
    assertTrue(component2.isMounted());

    ComponentTestHelper.mountComponent(
        mContext,
        componentView,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Container.create(c)
                .child(component3)
                .child(component4)
                .build();
          }
        });

    assertTrue(component1.isMounted());
    assertTrue(component2.isMounted());
    assertFalse(component3.isMounted());
    assertFalse(component4.isMounted());

    final MountState mountState = Whitebox.getInternalState(componentView,"mMountState");
    final LongSparseArray<MountItem> indexToItemMap =
        Whitebox.getInternalState(mountState,"mIndexToItemMap");

    final List<Component> components = new ArrayList<>();
    for (int i = 0; i < indexToItemMap.size(); i++) {
      components.add(indexToItemMap.valueAt(i).getComponent());
    }

    assertFalse(containsRef(components, component1));
    assertFalse(containsRef(components, component2));
    assertTrue(containsRef(components, component3));
    assertTrue(containsRef(components, component4));
  }

  /**
   * There was a crash when mounting a drawing in place of a view. This test is here to make sure
   * this does not regress. To reproduce this crash the pools needed to be in a specific state
   * as view layout outputs and mount items were being re-used for drawables.
   */
  @Test
