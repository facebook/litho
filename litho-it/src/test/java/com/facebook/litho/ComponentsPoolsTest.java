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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.ComponentsPools.acquireMountContent;
import static com.facebook.litho.ComponentsPools.maybePreallocateContent;
import static com.facebook.litho.ComponentsPools.release;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

@RunWith(ComponentsTestRunner.class)
public class ComponentsPoolsTest {

  private static final int POOL_SIZE = 2;
  private final Component mLifecycle =
      new Component("Lifecycle") {
        @Override
        int getTypeId() {
          return 1;
        }

        @Override
        protected int poolSize() {
          return POOL_SIZE;
        }

        @Override
        public View onCreateMountContent(Context context) {
          return mNewMountContent;
        }
      };

  private final Component mLifecycleWithEmptyPoolSize =
      new Component("LifecycleWithEmptyPoolSize") {
        @Override
        int getTypeId() {
          return 2;
        }

        @Override
        protected int poolSize() {
          return 0;
        }

        @Override
        public View onCreateMountContent(Context context) {
          return mNewMountContent;
        }
      };

  private Context mContext1;
  private Context mContext2;
  private ActivityController<Activity> mActivityController;
  private Activity mActivity;
  private ColorDrawable mMountContent;
  private View mNewMountContent;

  @Before
  public void setup() {
    mContext1 = getApplicationContext();
    mContext2 = new ContextWrapper(getApplicationContext());
    mActivityController = Robolectric.buildActivity(Activity.class).create();
    mActivity = mActivityController.get();
    mMountContent = new ColorDrawable(Color.RED);
    mNewMountContent = new View(mContext1);
  }

  @After
  public void tearDown() {
    ComponentsPools.clearActivityCallbacks();
  }

  @Test
  public void testAcquireMountContentWithSameContext() {
    assertThat(acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);

    release(mContext1, mLifecycle, mMountContent, ComponentTree.RecyclingMode.DEFAULT);

    assertThat(mMountContent)
        .isSameAs(acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT));
  }

  @Test
  public void testAcquireMountContentWithSameRootContext() {
    assertThat(acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);

    release(mContext1, mLifecycle, mMountContent, ComponentTree.RecyclingMode.DEFAULT);

    assertThat(acquireMountContent(mContext2, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);
  }

  @Test
  public void testAcquireMountContentWhenRecyclingModeIsNoViewReuse() {
    assertThat(
            acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.NO_VIEW_REUSE))
        .isSameAs(mNewMountContent);

    release(mContext1, mLifecycle, mMountContent, ComponentTree.RecyclingMode.NO_VIEW_REUSE);

    // In spite of recycling we get a new mount content.
    assertThat(
            acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.NO_VIEW_REUSE))
        .isSameAs(mNewMountContent);
  }

  @Test
  public void testNotPreallocateWhenRecyclingModeIsNoViewRecycle() {
    maybePreallocateContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.NO_VIEW_RECYCLING);
    mNewMountContent = new View(mContext1);
    // Pre allocation has no impact, we get a new mount content on acquiring.
    assertThat(
            acquireMountContent(
                mContext1, mLifecycle, ComponentTree.RecyclingMode.NO_VIEW_RECYCLING))
        .isSameAs(mNewMountContent);
  }

  @Test
  public void testReleaseMountContentForDestroyedContextDoesNothing() {
    // Assert pooling was working before
    assertThat(acquireMountContent(mActivity, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);

    release(mActivity, mLifecycle, mMountContent, ComponentTree.RecyclingMode.DEFAULT);

    assertThat(mMountContent)
        .isSameAs(acquireMountContent(mActivity, mLifecycle, ComponentTree.RecyclingMode.DEFAULT));

    // Now destroy it and assert pooling no longer works
    mActivityController.destroy();
    release(mActivity, mLifecycle, mMountContent, ComponentTree.RecyclingMode.DEFAULT);

    assertThat(acquireMountContent(mActivity, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);
  }

  @Test
  public void testDestroyingActivityDoesNotAffectPoolingOfOtherContexts() {
    mActivityController.destroy();
    ComponentsPools.onContextDestroyed(mActivity);

    release(mContext1, mLifecycle, mMountContent, ComponentTree.RecyclingMode.DEFAULT);

    assertThat(acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mMountContent);
  }

  @Test
  public void testPreallocateContent() {
    assertThat(acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);

    maybePreallocateContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT);

    // Change the content that's returned when we create new mount content to make sure we're
    // getting the one from preallocating above.
    mNewMountContent = new View(mContext1);
    assertThat(acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isNotSameAs(mNewMountContent);
  }

  @Test
  public void testDoNotPreallocateContentBeyondPoolSize() {
    for (int i = 0; i < POOL_SIZE; i++) {
      maybePreallocateContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT);
      acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT);
    }

    maybePreallocateContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT);

    mNewMountContent = new View(mContext1);
    assertThat(acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);
  }

  @Test
  public void testAllocationsCountTowardsPreallocationLimit() {
    for (int i = 0; i < POOL_SIZE - 1; i++) {
      maybePreallocateContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT);
      acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT);
    }
    acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT);

    // Allocation limit should be hit now, so we shouldn't preallocate anything
    maybePreallocateContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT);

    mNewMountContent = new View(mContext1);
    assertThat(acquireMountContent(mContext1, mLifecycle, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);
  }

  @Test
  public void testReleaseAndAcquireWithNoPoolSize() {
    release(
        mContext1, mLifecycleWithEmptyPoolSize, mMountContent, ComponentTree.RecyclingMode.DEFAULT);
    assertThat(
            acquireMountContent(
                mContext1, mLifecycleWithEmptyPoolSize, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);
  }

  @Test
  public void testPreallocateWithEmptyPoolSize() {
    maybePreallocateContent(
        mContext1, mLifecycleWithEmptyPoolSize, ComponentTree.RecyclingMode.DEFAULT);

    mNewMountContent = new View(mContext1);
    assertThat(
            acquireMountContent(
                mContext1, mLifecycleWithEmptyPoolSize, ComponentTree.RecyclingMode.DEFAULT))
        .isSameAs(mNewMountContent);
  }
}
