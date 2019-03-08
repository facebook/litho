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

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class ComponentHostRecycleUtilTest {

  @After
  public void tearDown() {
    ComponentsConfiguration.isMountIndexBisectEnabled = false;
    ComponentsConfiguration.mountIndexBisectStart = 0;
    ComponentsConfiguration.mountIndexBisectEnd = 100;

    ComponentsConfiguration.isRootComponentBisectEnabled = false;
    ComponentsConfiguration.rootComponentBisectStart = "aaaaa";
    ComponentsConfiguration.rootComponentBisectEnd = "zzzzz";
  }

  @Test
  public void testBothDisabled() {
    ComponentsConfiguration.isMountIndexBisectEnabled = false;
    ComponentsConfiguration.isRootComponentBisectEnabled = false;

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isFalse();
  }

  @Test
  public void testMountIndexBisect() {
    ComponentsConfiguration.isMountIndexBisectEnabled = true;
    ComponentsConfiguration.mountIndexBisectStart = 0;
    ComponentsConfiguration.mountIndexBisectEnd = 100;

    ComponentsConfiguration.isRootComponentBisectEnabled = false;
    ComponentsConfiguration.rootComponentBisectStart = "bar";
    ComponentsConfiguration.rootComponentBisectEnd = "bar";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isTrue();

    ComponentsConfiguration.mountIndexBisectStart = 5;
    ComponentsConfiguration.mountIndexBisectEnd = 10;

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(4, "foo")).isFalse();

    ComponentsConfiguration.mountIndexBisectStart = 4;
    ComponentsConfiguration.mountIndexBisectEnd = 4;

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(4, "foo")).isTrue();
  }

  @Test
  public void testRootComponentBisect() {
    ComponentsConfiguration.isMountIndexBisectEnabled = false;
    ComponentsConfiguration.mountIndexBisectStart = 4;
    ComponentsConfiguration.mountIndexBisectEnd = 4;

    ComponentsConfiguration.isRootComponentBisectEnabled = true;
    ComponentsConfiguration.rootComponentBisectStart = "aaaaa";
    ComponentsConfiguration.rootComponentBisectEnd = "zzzzz";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isTrue();

    ComponentsConfiguration.rootComponentBisectStart = "foo";
    ComponentsConfiguration.rootComponentBisectEnd = "foo";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isTrue();

    ComponentsConfiguration.rootComponentBisectStart = "bar";
    ComponentsConfiguration.rootComponentBisectEnd = "bar";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isFalse();

    ComponentsConfiguration.rootComponentBisectStart = "FOO";
    ComponentsConfiguration.rootComponentBisectEnd = "FOO";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isTrue();

    ComponentsConfiguration.rootComponentBisectStart = "foo";
    ComponentsConfiguration.rootComponentBisectEnd = "goo";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isTrue();

    ComponentsConfiguration.rootComponentBisectStart = "bar";
    ComponentsConfiguration.rootComponentBisectEnd = "foo";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isTrue();

    ComponentsConfiguration.rootComponentBisectStart = "bar";
    ComponentsConfiguration.rootComponentBisectEnd = "baz";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isFalse();
  }

  @Test
  public void testBoth() {
    ComponentsConfiguration.isMountIndexBisectEnabled = true;
    ComponentsConfiguration.mountIndexBisectStart = 0;
    ComponentsConfiguration.mountIndexBisectEnd = 100;

    ComponentsConfiguration.isRootComponentBisectEnabled = true;
    ComponentsConfiguration.rootComponentBisectStart = "aaaaa";
    ComponentsConfiguration.rootComponentBisectEnd = "zzzzz";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isTrue();

    ComponentsConfiguration.mountIndexBisectStart = 4;
    ComponentsConfiguration.mountIndexBisectEnd = 4;

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isFalse();
    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(4, "foo")).isTrue();

    ComponentsConfiguration.rootComponentBisectStart = "foo";
    ComponentsConfiguration.rootComponentBisectEnd = "foo";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(0, "foo")).isFalse();
    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(4, "foo")).isTrue();

    ComponentsConfiguration.rootComponentBisectStart = "bar";
    ComponentsConfiguration.rootComponentBisectEnd = "bar";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(4, "foo")).isFalse();

    ComponentsConfiguration.rootComponentBisectStart = "FOO";
    ComponentsConfiguration.rootComponentBisectEnd = "FOO";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(4, "foo")).isTrue();

    ComponentsConfiguration.mountIndexBisectStart = 3;
    ComponentsConfiguration.mountIndexBisectEnd = 5;
    ComponentsConfiguration.rootComponentBisectStart = "foo";
    ComponentsConfiguration.rootComponentBisectEnd = "goo";

    assertThat(ComponentHostRecycleUtil.shouldSkipRecyclingComponentHost(4, "foo")).isTrue();
  }
}
