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

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.view.View;
import androidx.test.core.app.ApplicationProvider;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class PoolBisectUtilTest {

  private final Component mBarComponent =
      new Component() {

        @Override
        protected Object onCreateMountContent(Context context) {
          return new View(context);
        }

        @Override
        public String getSimpleName() {
          return "BarComponent";
        }
      };

  private final Component mFooComponent =
      new Component() {

        @Override
        protected Object onCreateMountContent(Context context) {
          return new View(context);
        }

        @Override
        public String getSimpleName() {
          return "FooComponent";
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

    assertThat(PoolBisectUtil.getPoolForComponent(mFooComponent))
        .isInstanceOf(DisabledMountContentPool.class);

    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    assertThat(PoolBisectUtil.getPoolForComponent(mBarComponent))
        .isInstanceOf(DisabledMountContentPool.class);
    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    assertThat(PoolBisectUtil.getPoolForComponent(mBarComponent))
        .isInstanceOf(DisabledMountContentPool.class);
  }

  @Test
  public void testNotInRange() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    assertThat(PoolBisectUtil.getPoolForComponent(mFooComponent))
        .isInstanceOf(DefaultMountContentPool.class);
    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    assertThat(PoolBisectUtil.getPoolForComponent(mFooComponent))
        .isInstanceOf(DefaultMountContentPool.class);
    ComponentsConfiguration.disablePoolsStart = "panda";
    ComponentsConfiguration.disablePoolsEnd = "red";

    assertThat(PoolBisectUtil.getPoolForComponent(mFooComponent))
        .isInstanceOf(DefaultMountContentPool.class);
  }

  @Test
  public void testExactMatch() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "foocomponent";
    ComponentsConfiguration.disablePoolsEnd = "foocomponent";

    assertThat(PoolBisectUtil.getPoolForComponent(mFooComponent))
        .isInstanceOf(DisabledMountContentPool.class);
  }

  @Test
  public void testExcludeMatch() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "foocomponent1";
    ComponentsConfiguration.disablePoolsEnd = "zzzzz";

    assertThat(PoolBisectUtil.getPoolForComponent(mFooComponent))
        .isInstanceOf(DefaultMountContentPool.class);

    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "foocomponen";

    assertThat(PoolBisectUtil.getPoolForComponent(mFooComponent))
        .isInstanceOf(DefaultMountContentPool.class);
  }

  @Test
  public void testDisabled() {
    ComponentsConfiguration.isPoolBisectEnabled = false;
    ComponentsConfiguration.disablePoolsStart = "aaaaa";
    ComponentsConfiguration.disablePoolsEnd = "zzzzz";

    assertThat(PoolBisectUtil.getPoolForComponent(mFooComponent))
        .isInstanceOf(DefaultMountContentPool.class);
  }

  @Test
  public void testGetPoolInRange() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    final MountContentPool pool = PoolBisectUtil.getPoolForComponent(mBarComponent);
    assertThat(pool).isInstanceOf(DisabledMountContentPool.class);

    final View view =
        (View) pool.acquire(ApplicationProvider.getApplicationContext(), mBarComponent);
    pool.release(view);
    assertThat(view)
        .isNotSameAs(pool.acquire(ApplicationProvider.getApplicationContext(), mBarComponent));
  }

  @Test
  public void testGetPoolNotInRange() {
    ComponentsConfiguration.isPoolBisectEnabled = true;
    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    final MountContentPool pool = PoolBisectUtil.getPoolForComponent(mFooComponent);
    assertThat(pool).isInstanceOf(DefaultMountContentPool.class);

    final View view =
        (View) pool.acquire(ApplicationProvider.getApplicationContext(), mFooComponent);
    pool.release(view);
    assertThat(view)
        .isSameAs(pool.acquire(ApplicationProvider.getApplicationContext(), mFooComponent));
  }

  @Test
  public void testGetPoolDisabled() {
    ComponentsConfiguration.isPoolBisectEnabled = false;
    ComponentsConfiguration.disablePoolsStart = "artistcomponent";
    ComponentsConfiguration.disablePoolsEnd = "bazcomponent";

    final MountContentPool pool = PoolBisectUtil.getPoolForComponent(mBarComponent);
    assertThat(pool).isInstanceOf(DefaultMountContentPool.class);

    final View view =
        (View) pool.acquire(ApplicationProvider.getApplicationContext(), mBarComponent);
    pool.release(view);
    assertThat(view)
        .isSameAs(pool.acquire(ApplicationProvider.getApplicationContext(), mBarComponent));
  }
}
