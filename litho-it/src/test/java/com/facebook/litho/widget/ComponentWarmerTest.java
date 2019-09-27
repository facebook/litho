/*
 * Copyright 2019-present Facebook, Inc.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentWarmerTest {

  private ComponentContext mContext;
  private ComponentRenderInfo mComponentRenderInfo;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponentRenderInfo =
        ComponentRenderInfo.create()
            .component(TestDrawableComponent.create(mContext).build())
            .customAttribute(ComponentWarmer.COMPONENT_WARMER_TAG, "tag1")
            .build();
  }

  @Test
  public void testCreateFromRecyclerBinder() {
    final RecyclerBinder binder = new RecyclerBinder.Builder().build(mContext);

    final ComponentWarmer warmer = new ComponentWarmer(binder);

    assertThat(binder.getComponentWarmer()).isEqualTo(warmer);
    assertThat(warmer.getFactory()).isNotNull();
  }

  @Test
  public void testPrepareForRecyclerBinder() {
    final RecyclerBinder binder = new RecyclerBinder.Builder().build(mContext);

    final ComponentWarmer warmer = new ComponentWarmer(binder);
    warmer.prepare("tag1", mComponentRenderInfo);

    assertThat(warmer.get("tag1")).isNotNull();

    binder.insertItemAt(0, mComponentRenderInfo);

    assertThat(binder.getComponentTreeHolderAt(0)).isEqualTo(warmer.get("tag1"));
  }

  @Test
  public void testPrepareAsyncForRecyclerBinder() {
    final RecyclerBinder binder = new RecyclerBinder.Builder().build(mContext);

    final ComponentWarmer warmer = new ComponentWarmer(binder);
    warmer.prepareAsync("tag1", mComponentRenderInfo);

    assertThat(warmer.get("tag1")).isNotNull();

    binder.insertItemAt(0, mComponentRenderInfo);

    assertThat(binder.getComponentTreeHolderAt(0)).isEqualTo(warmer.get("tag1"));
  }
}
