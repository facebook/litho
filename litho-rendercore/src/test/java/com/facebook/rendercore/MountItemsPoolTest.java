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

package com.facebook.rendercore;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import androidx.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public class MountItemsPoolTest {
  private final Context mContext = RuntimeEnvironment.application;
  private ActivityController<Activity> mActivityController;
  private Activity mActivity;

  @Before
  public void setup() {
    MountItemsPool.clear();
    MountItemsPool.setMountContentPoolFactory(null);
    mActivityController = Robolectric.buildActivity(Activity.class).create();
    mActivity = mActivityController.get();
  }

  @After
  public void cleanup() {
    MountItemsPool.setMountContentPoolFactory(null);
  }

  @Test
  public void testPrefillMountContentPool() {
    final int prefillCount = 4;
    final TestRenderUnit testRenderUnit =
        new TestRenderUnit(/*id*/ 0, /*customPoolSize*/ prefillCount);
    MountItemsPool.prefillMountContentPool(mContext, prefillCount, testRenderUnit);
    assertThat(testRenderUnit.getCreatedCount()).isEqualTo(prefillCount);

    final TestRenderUnit testRenderUnitToAcquire =
        new TestRenderUnit(0, /*customPoolSize*/ prefillCount);

    for (int i = 0; i < prefillCount; i++) {
      MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    }
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(0);

    MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(1);
  }

  @Test
  public void testPrefillMountContentPoolWithCustomPool() {
    final int prefillCount = 4;
    final int customPoolSize = 2;
    final TestRenderUnit testRenderUnit = new TestRenderUnit(0, customPoolSize);
    MountItemsPool.prefillMountContentPool(mContext, prefillCount, testRenderUnit);
    // it is "+ 1" because as soon as it tries to prefill a mount content that doesn't fill the
    // pool, then we stop
    assertThat(testRenderUnit.getCreatedCount()).isEqualTo(customPoolSize + 1);

    final TestRenderUnit testRenderUnitToAcquire = new TestRenderUnit(0, 2);

    for (int i = 0; i < prefillCount; i++) {
      MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    }
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(2);
  }

  @Test
  public void testPrefillMountContentPoolWithCustomPoolFactory() {
    final MountItemsPool.ItemPool customPool =
        new MountItemsPool.DefaultItemPool(MountItemsPoolTest.class, 10, false);
    MountItemsPool.setMountContentPoolFactory(() -> customPool);

    final int prefillCount = 10;
    final TestRenderUnit testRenderUnit = new TestRenderUnit(0, 5);
    MountItemsPool.prefillMountContentPool(mContext, prefillCount, testRenderUnit);
    assertThat(testRenderUnit.getCreatedCount()).isEqualTo(prefillCount);

    final TestRenderUnit testRenderUnitToAcquire = new TestRenderUnit(0, 5);

    for (int i = 0; i < prefillCount; i++) {
      MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    }
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(0);
  }

  @Test
  public void testReleaseMountContentForDestroyedContextDoesNothing() {
    final TestRenderUnit testRenderUnit = new TestRenderUnit(0);

    final Object content1 = MountItemsPool.acquireMountContent(mActivity, testRenderUnit);
    MountItemsPool.release(mActivity, testRenderUnit, content1);

    final Object content2 = MountItemsPool.acquireMountContent(mActivity, testRenderUnit);

    // Assert pooling was working before
    assertThat(content1).isSameAs(content2);

    MountItemsPool.release(mActivity, testRenderUnit, content2);

    // Now destroy the activity and assert pooling no longer works. Next acquire should produce
    // difference content.
    MountItemsPool.onContextDestroyed(mActivity);

    final Object content3 = MountItemsPool.acquireMountContent(mActivity, testRenderUnit);
    assertThat(content3).isNotSameAs(content1);
  }

  @Test
  public void testDestroyingActivityDoesNotAffectPoolingOfOtherContexts() {
    // Destroy activity context
    mActivityController.destroy();
    MountItemsPool.onContextDestroyed(mActivity);
    final TestRenderUnit testRenderUnit = new TestRenderUnit(0);

    // Create content with different context
    final Object content1 = MountItemsPool.acquireMountContent(mContext, testRenderUnit);
    MountItemsPool.release(mContext, testRenderUnit, content1);

    final Object content2 = MountItemsPool.acquireMountContent(mContext, testRenderUnit);

    // Ensure different context is unaffected by destroying activity context.
    assertThat(content1).isSameAs(content2);
  }

  @Test
  public void testAcquireAndReleaseReturnsCorrectContentInstances() {
    final TestRenderUnit testRenderUnitToAcquire =
        new TestRenderUnit(/*id*/ 0, /*customPoolSize*/ 2);

    // acquire content objects
    Object firstContent = MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    Object secondContent = MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);

    // both of them should be created and they shouldn't be the same instance
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(2);
    assertThat(firstContent).isNotNull();
    assertThat(secondContent).isNotSameAs(firstContent);

    // release the second content instance
    MountItemsPool.release(mContext, testRenderUnitToAcquire, secondContent);

    // acquire the third content instance
    Object thirdContent = MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);

    // it should be the same instance that was just released
    assertThat(thirdContent).isSameAs(secondContent);
  }

  @Test
  public void testAcquireContentWhenPoolingIsDisabledReturnsNewContentEveryTime() {
    final TestRenderUnit testRenderUnitToAcquire =
        new TestRenderUnit(/*id*/ 0, /*customPoolSize*/ 0); // disable Pooling

    // acquire content objects
    Object firstContent = MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);
    Object secondContent = MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);

    // both of them should be created and they shouldn't be the same instance
    assertThat(testRenderUnitToAcquire.getCreatedCount()).isEqualTo(2);
    assertThat(firstContent).isNotNull();
    assertThat(secondContent).isNotSameAs(firstContent);

    // release the second content instance
    MountItemsPool.release(mContext, testRenderUnitToAcquire, secondContent);

    // acquire the third content instance
    Object thirdContent = MountItemsPool.acquireMountContent(mContext, testRenderUnitToAcquire);

    // it should not be the same as just released instance because pool size is 0
    assertThat(thirdContent).isNotSameAs(secondContent);
  }

  public static final class TestRenderUnit extends RenderUnit<View>
      implements ContentAllocator<View> {

    private final long mId;
    private final int mCustomPoolSize;
    private int mCreatedCount;

    public TestRenderUnit(long id) {
      super(RenderType.VIEW);
      mId = id;
      mCreatedCount = 0;
      mCustomPoolSize = DEFAULT_MAX_PREALLOCATION;
    }

    public TestRenderUnit(long id, int customPoolSize) {
      super(RenderType.VIEW);
      mId = id;
      mCreatedCount = 0;
      mCustomPoolSize = customPoolSize;
    }

    @Override
    public View createContent(Context c) {
      mCreatedCount++;
      return new View(c);
    }

    @Nullable
    @Override
    public MountItemsPool.ItemPool createRecyclingPool() {
      return new MountItemsPool.DefaultItemPool(MountItemsPoolTest.class, mCustomPoolSize, false);
    }

    @Override
    public ContentAllocator<View> getContentAllocator() {
      return this;
    }

    @Override
    public long getId() {
      return mId;
    }

    @Override
    public int poolSize() {
      return mCustomPoolSize;
    }

    public int getCreatedCount() {
      return mCreatedCount;
    }
  }
}
