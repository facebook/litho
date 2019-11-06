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

package com.facebook.litho.widget;

import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LayoutThreadPoolConfigurationImpl;
import com.facebook.litho.Row;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
  public void testConsumeFromRecyclerBinder() {
    final RecyclerBinder binder = new RecyclerBinder.Builder().build(mContext);

    final ComponentWarmer warmer = new ComponentWarmer(binder);
    warmer.prepare("tag1", mComponentRenderInfo, null);

    assertThat(warmer.consume("tag1")).isNotNull();
    assertThat(warmer.consume("tag1")).isNull();
  }

  @Test
  public void testPrepareForRecyclerBinder() {
    final RecyclerBinder binder = new RecyclerBinder.Builder().build(mContext);

    final ComponentWarmer warmer = new ComponentWarmer(binder);
    ComponentWarmer.Cache cache = Whitebox.getInternalState(warmer, "mCache");

    warmer.prepare("tag1", mComponentRenderInfo, null);

    final ComponentTreeHolder cachedCTH = cache.get("tag1");

    binder.insertItemAt(0, mComponentRenderInfo);

    assertThat(binder.getComponentTreeHolderAt(0)).isEqualTo(cachedCTH);
  }

  @Test
  public void testPrepareAsyncForRecyclerBinder() {
    final RecyclerBinder binder = new RecyclerBinder.Builder().build(mContext);

    final ComponentWarmer warmer = new ComponentWarmer(binder);
    ComponentWarmer.Cache cache = Whitebox.getInternalState(warmer, "mCache");

    warmer.prepareAsync("tag1", mComponentRenderInfo);

    final ComponentTreeHolder cachedCTH = cache.get("tag1");

    binder.insertItemAt(0, mComponentRenderInfo);

    assertThat(binder.getComponentTreeHolderAt(0)).isEqualTo(cachedCTH);
  }

  @Test
  public void testCancelDuringPrepareAsync() {
    final RecyclerBinder binder =
        new RecyclerBinder.Builder()
            .useCancelableLayoutFutures(true)
            .threadPoolConfig(new LayoutThreadPoolConfigurationImpl(2, 2, 5))
            .build(mContext);

    binder.measure(
        new Size(),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        null);

    final ComponentWarmer warmer = new ComponentWarmer(binder);

    final CountDownLatch waitToResolveChild = new CountDownLatch(1);
    final CountDownLatch waitToCancel = new CountDownLatch(1);

    final boolean[] childrenResolved = {false, false};

    final Component childComponent1 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            childrenResolved[0] = true;

            waitToResolveChild.countDown();
            try {
              waitToCancel.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }

            return Row.create(c).build();
          }
        };

    final Component childComponent2 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            childrenResolved[1] = true;
            return Row.create(c).build();
          }
        };

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c).child(childComponent1).child(childComponent2).build();
          }
        };

    warmer.prepareAsync("tag1", ComponentRenderInfo.create().component(component).build());

    try {
      waitToResolveChild.await(5, TimeUnit.SECONDS);
      warmer.cancelPrepare("tag1");
      waitToCancel.countDown();

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertThat(childrenResolved[0]).isTrue();
    assertThat(childrenResolved[1]).isFalse();
    assertThat(warmer.consume("tag1")).isNull();
  }

  @Test
  public void testCancelDuringPrepare() {
    final RecyclerBinder binder =
        new RecyclerBinder.Builder().useCancelableLayoutFutures(true).build(mContext);

    binder.measure(
        new Size(),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        null);

    final ComponentWarmer warmer = new ComponentWarmer(binder);

    final CountDownLatch waitToResolveChild = new CountDownLatch(1);
    final CountDownLatch waitToCancel = new CountDownLatch(1);
    final CountDownLatch waitToAssert = new CountDownLatch(1);

    final boolean[] childrenResolved = {false, false};

    final Component childComponent1 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            childrenResolved[0] = true;
            waitToResolveChild.countDown();
            try {
              boolean wait = waitToCancel.await(7, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }

            return Row.create(c).build();
          }
        };

    final Component childComponent2 =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            childrenResolved[1] = true;
            return Row.create(c).build();
          }
        };

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return Column.create(c).child(childComponent1).child(childComponent2).build();
          }
        };

    runOnBackgroundThreadSync(
        new Runnable() {
          @Override
          public void run() {
            warmer.prepare("tag1", ComponentRenderInfo.create().component(component).build(), null);
            waitToAssert.countDown();
          }
        });

    try {
      waitToResolveChild.await(5, TimeUnit.SECONDS);
      warmer.cancelPrepare("tag1");
      waitToCancel.countDown();

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    try {
      waitToAssert.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    assertThat(childrenResolved[0]).isTrue();
    assertThat(childrenResolved[1]).isFalse();
    assertThat(warmer.consume("tag1")).isNull();
  }

  private static void runOnBackgroundThreadSync(final Runnable runnable) {
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                runnable.run();
              }
            })
        .start();
  }
}
