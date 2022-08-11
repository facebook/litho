/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import static org.assertj.core.api.Assertions.assertThat;

import android.content.Context;
import android.view.View;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.rendercore.MountItemsPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class ComponentPoolingTest {
  private static final int POOL_SIZE = 2;

  private final Context mContext = RuntimeEnvironment.application;

  @Before
  public void setup() {
    MountItemsPool.clear();
  }

  @After
  public void cleanup() {
    MountItemsPool.clear();
  }

  @Test
  public void testMaybePreallocateContent() {
    final PooledComponent component = new PooledComponent("PooledComponent");

    // Preallocate content more times than the defined pool size
    for (int i = 0; i < POOL_SIZE * 2; i++) {
      MountItemsPool.maybePreallocateContent(mContext, component);
    }

    // Ensure onCreateMountContent was called POOL_SIZE times.
    assertThat(component.getOnCreateMountContentCount()).isEqualTo(POOL_SIZE);

    // Acquire POOL_SIZE contents
    Object[] objects = new Object[POOL_SIZE + 1];
    for (int i = 0; i < POOL_SIZE; i++) {
      objects[i] = MountItemsPool.acquireMountContent(mContext, component);
    }

    // Ensure onCreateMountContent wasn't called an additional time.
    assertThat(component.getOnCreateMountContentCount()).isEqualTo(POOL_SIZE);

    // Acquire one more content
    objects[POOL_SIZE] = MountItemsPool.acquireMountContent(mContext, component);

    // Ensure onCreateMountContent was called an additional time.
    assertThat(component.getOnCreateMountContentCount()).isEqualTo(POOL_SIZE + 1);

    // Release all acquired content
    for (Object object : objects) {
      MountItemsPool.release(mContext, component, object);
    }

    // Reacquire POOL_SIZE contents
    for (int i = 0; i < POOL_SIZE; i++) {
      MountItemsPool.acquireMountContent(mContext, component);
    }

    // Ensure onCreateMountContent wasn't called an additional time.
    assertThat(component.getOnCreateMountContentCount()).isEqualTo(POOL_SIZE + 1);
  }

  private static class PooledComponent extends SpecGeneratedComponent {
    private int mOnCreateMountContentCount = 0;

    protected PooledComponent(String simpleName) {
      super(simpleName);
    }

    @Override
    public int poolSize() {
      return POOL_SIZE;
    }

    @Override
    public Object onCreateMountContent(Context context) {
      mOnCreateMountContentCount++;
      return new View(context);
    }

    public int getOnCreateMountContentCount() {
      return mOnCreateMountContentCount;
    }
  }
}
