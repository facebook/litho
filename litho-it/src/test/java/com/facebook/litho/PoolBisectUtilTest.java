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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.view.View;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class PoolBisectUtilTest {

  private final Component mBarComponent =
      new Component("BarComponent") {
        @Override
        int getTypeId() {
          return 1;
        }

        @Override
        protected Object onCreateMountContent(Context context) {
          return mock(View.class);
        }
      };

  private final Component mFooComponent =
      new Component("FooComponent") {
        @Override
        int getTypeId() {
          return 2;
        }

        @Override
        protected Object onCreateMountContent(Context context) {
          return mock(View.class);
        }
      };

  @After
  public void tearDown() {
    ComponentsConfiguration.isPoolBisectEnabled = false;
    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "zzzzz";
  }

  @Test
  public void testRanges() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "zzzzz";

    assertThat(PoolBisectUtil.shouldDisablePool("FooComponent")).isTrue();

    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    assertThat(PoolBisectUtil.shouldDisablePool("BarComponent")).isTrue();

    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    assertThat(PoolBisectUtil.shouldDisablePool("BarComponent")).isTrue();
  }

  @Test
  public void testNotInRange() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "zzzzz";

    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    assertThat(PoolBisectUtil.shouldDisablePool("FooComponent")).isFalse();

    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    assertThat(PoolBisectUtil.shouldDisablePool("FooComponent")).isFalse();

    ComponentsConfiguration.disablePoolsStart = "panda";
    ComponentsConfiguration.disablePoolsEnd = "red";

    assertThat(PoolBisectUtil.shouldDisablePool("FooComponent")).isFalse();
  }

  @Test
  public void testExactMatch() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "zzzzz";

    ComponentsConfiguration.disablePoolsStart = "foocomponent";
    ComponentsConfiguration.disablePoolsEnd = "foocomponent";

    assertThat(PoolBisectUtil.shouldDisablePool("FooComponent")).isTrue();
  }

  @Test
  public void testExcludeMatch() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "zzzzz";

    ComponentsConfiguration.disablePoolsStart = "foocomponent1";
    ComponentsConfiguration.disablePoolsEnd = "zzzzz";

    assertThat(PoolBisectUtil.shouldDisablePool("FooComponent")).isFalse();

    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "foocomponen";

    assertThat(PoolBisectUtil.shouldDisablePool("FooComponent")).isFalse();
  }

  @Test
  public void testDisabled() {
    ComponentsConfiguration.isPoolBisectEnabled = false;
    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "zzzzz";

    assertThat(PoolBisectUtil.shouldDisablePool("FooComponent")).isFalse();
  }

  @Test
  public void testGetPoolInRange() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    final MountContentPool pool = PoolBisectUtil.getPoolForComponent(mBarComponent);
    assertThat(pool).isInstanceOf(DisabledMountContentPool.class);

    final View view = (View) pool.acquire(RuntimeEnvironment.application, mBarComponent);
    pool.release(view);
    assertThat(view).isNotSameAs(pool.acquire(RuntimeEnvironment.application, mBarComponent));
  }

  @Test
  public void testGetPoolNotInRange() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    final MountContentPool pool = PoolBisectUtil.getPoolForComponent(mFooComponent);
    assertThat(pool).isInstanceOf(DefaultMountContentPool.class);

    final View view = (View) pool.acquire(RuntimeEnvironment.application, mFooComponent);
    pool.release(view);
    assertThat(view).isSameAs(pool.acquire(RuntimeEnvironment.application, mFooComponent));
  }

  @Test
  public void testGetPoolDisabled() {
    ComponentsConfiguration.isPoolBisectEnabled = false;
    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    final MountContentPool pool = PoolBisectUtil.getPoolForComponent(mBarComponent);
    assertThat(pool).isInstanceOf(DefaultMountContentPool.class);

    final View view = (View) pool.acquire(RuntimeEnvironment.application, mBarComponent);
    pool.release(view);
    assertThat(view).isSameAs(pool.acquire(RuntimeEnvironment.application, mBarComponent));
  }
}
