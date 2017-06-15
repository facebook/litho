/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.support.v4.util.LongSparseArray;

import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.TestComponent;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.litho.testing.ComponentTestHelper.mountComponent;
import static com.facebook.litho.testing.TestDrawableComponent.create;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.powermock.reflect.Whitebox.getInternalState;

@RunWith(ComponentsTestRunner.class)
public class MountStateRemountTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testRemountSameLayoutState() {
    final TestComponent component1 = create(mContext)
        .build();
    final TestComponent component2 = create(mContext)
        .build();
    final TestComponent component3 = create(mContext)
        .build();
    final TestComponent component4 = create(mContext)
        .build();

    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(component1)
                .child(component2)
                .build();
          }
        });

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isTrue();

    mountComponent(
        mContext,
        lithoView,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(component3)
                .child(component4)
                .build();
          }
        });

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isTrue();
    assertThat(component3.isMounted()).isFalse();
    assertThat(component4.isMounted()).isFalse();

    final MountState mountState = getInternalState(lithoView, "mMountState");
    final LongSparseArray<MountItem> indexToItemMap =
        getInternalState(mountState, "mIndexToItemMap");

    final List<Component> components = new ArrayList<>();
    for (int i = 0; i < indexToItemMap.size(); i++) {
      components.add(indexToItemMap.valueAt(i).getComponent());
    }

    assertThat(containsRef(components, component1)).isFalse();
    assertThat(containsRef(components, component2)).isFalse();
    assertThat(containsRef(components, component3)).isTrue();
    assertThat(containsRef(components, component4)).isTrue();
  }

  /**
   * There was a crash when mounting a drawing in place of a view. This test is here to make sure
   * this does not regress. To reproduce this crash the pools needed to be in a specific state
   * as view layout outputs and mount items were being re-used for drawables.
   */
  @Test
  public void testRemountDifferentMountType() throws IllegalAccessException, NoSuchFieldException {
    clearPool("sLayoutOutputPool");
    clearPool("sViewNodeInfoPool");

    final LithoView lithoView = ComponentTestHelper.mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return TestViewComponent.create(c).buildWithLayout();
          }
        });

    ComponentTestHelper.mountComponent(
        mContext,
        lithoView,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return TestDrawableComponent.create(c).buildWithLayout();
          }
        });
  }

  @Test
  public void testRemountNewLayoutState() {
    final TestComponent component1 = create(mContext)
        .unique()
        .build();
    final TestComponent component2 = create(mContext)
        .unique()
        .build();
    final TestComponent component3 = create(mContext)
        .unique()
        .build();
    final TestComponent component4 = create(mContext)
        .unique()
        .build();

    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(component1)
                .child(component2)
                .build();
          }
        });

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isTrue();

    mountComponent(
        mContext,
        lithoView,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(component3)
                .child(component4)
                .build();
          }
        });

    assertThat(component1.isMounted()).isFalse();
    assertThat(component2.isMounted()).isFalse();
    assertThat(component3.isMounted()).isTrue();
    assertThat(component4.isMounted()).isTrue();
  }

  @Test
  public void testRemountPartiallyDifferentLayoutState() {
    final TestComponent component1 = create(mContext)
        .build();
    final TestComponent component2 = create(mContext)
        .build();
    final TestComponent component3 = create(mContext)
        .build();
    final TestComponent component4 = create(mContext)
        .build();

    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(component1)
                .child(component2)
                .build();
          }
        });

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isTrue();

    mountComponent(
        mContext,
        lithoView,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return Column.create(c)
                .child(component3)
                .child(
                    Column.create(c)
                        .wrapInView()
                        .child(component4))
                .build();
          }
        });

    assertThat(component1.isMounted()).isTrue();
    assertThat(component2.isMounted()).isFalse();
    assertThat(component3.isMounted()).isFalse();
    assertThat(component4.isMounted()).isTrue();
  }

  private boolean containsRef(List<?> list, Object object) {
    for (Object o : list) {
      if (o == object) {
        return true;
      }
    }
    return false;
  }

  private static void clearPool(String name) {
    final RecyclePool<?> pool =
        Whitebox.getInternalState(ComponentsPools.class, name);

    while (pool.acquire() != null) {
      // Run.
    }
  }
}
