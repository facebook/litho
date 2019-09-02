/*
 * Copyright 2018-present Facebook, Inc.
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

import com.facebook.litho.LayoutState.LayoutStateReferenceWrapper;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentTest {

  private ComponentContext mContext;
  private final boolean config = ComponentsConfiguration.cacheInternalNodeOnLayoutState;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    ComponentsConfiguration.cacheInternalNodeOnLayoutState = false;
  }

  @After
  public void cleanup() {
    ComponentsConfiguration.cacheInternalNodeOnLayoutState = config;
  }

  @Test
  public void testShallowCopyCachedLayoutSameThread() {
    final LayoutState layoutState = new LayoutState(mContext);
    final ComponentContext c = new ComponentContext(mContext);
    c.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState));

    Component component = TestDrawableComponent.create(mContext).build();
    component.measure(c, 100, 100, new Size());
    assertThat(component.getCachedLayout(c)).isNotNull();

    Component copyComponent = component.makeShallowCopy();
    assertThat(copyComponent.getCachedLayout(c)).isNotNull();
  }

  @Test
  public void testShallowCopyCachedLayoutSameLayoutStateCacheLayoutState() {
    boolean config = ComponentsConfiguration.cacheInternalNodeOnLayoutState;
    ComponentsConfiguration.cacheInternalNodeOnLayoutState = true;
    final LayoutState layoutState = new LayoutState(mContext);
    final ComponentContext c = new ComponentContext(mContext);
    c.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState));
    Component component = TestDrawableComponent.create(mContext).unique().build();
    component.measure(c, 100, 100, new Size());
    assertThat(component.getCachedLayout(c)).isNotNull();
    Component copyComponent = component.makeShallowCopy();
    assertThat(copyComponent.getCachedLayout(c)).isNotNull();

    assertThat(component.getCachedLayout(c)).isEqualTo(copyComponent.getCachedLayout(c));
    ComponentsConfiguration.cacheInternalNodeOnLayoutState = config;
  }

  @Test
  public void testShallowCopyCachedLayoutOtherLayoutStateCacheLayoutState() {
    boolean config = ComponentsConfiguration.cacheInternalNodeOnLayoutState;
    ComponentsConfiguration.cacheInternalNodeOnLayoutState = true;

    final LayoutState layoutState1 = new LayoutState(mContext);
    final LayoutState layoutState2 = new LayoutState(mContext);

    final ComponentContext c1 = new ComponentContext(mContext);
    c1.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState1));
    final ComponentContext c2 = new ComponentContext(mContext);
    c2.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState2));

    Component component = TestDrawableComponent.create(mContext).unique().build();
    component.measure(c1, 100, 100, new Size());
    assertThat(component.getCachedLayout(c1)).isNotNull();

    Component copyComponent = component.makeShallowCopy();
    assertThat(copyComponent.getCachedLayout(c2)).isNull();

    ComponentsConfiguration.cacheInternalNodeOnLayoutState = config;
  }

  @Test
  public void testShallowCopyCachedLayoutDifferentThreadsNoMeasureCopy() {
    final LayoutState layoutState = new LayoutState(mContext);
    final ComponentContext c = new ComponentContext(mContext);
    c.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState));

    Component component = TestDrawableComponent.create(mContext).build();
    Component[] resultCopyComponent = new Component[1];
    InternalNode[] cachedLayouts = new InternalNode[2];

    CountDownLatch lockWaitMeasure = new CountDownLatch(1);
    CountDownLatch testFinished = new CountDownLatch(1);

    Thread thread1 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                component.measure(c, 100, 100, new Size());

                cachedLayouts[0] = component.getCachedLayout(c);

                lockWaitMeasure.countDown();
              }
            });

    Thread thread2 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                // Wait for component to have a cached layout, then make copy.
                try {
                  lockWaitMeasure.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                  throw new IllegalStateException();
                }

                resultCopyComponent[0] = component.makeShallowCopy();
                cachedLayouts[1] = resultCopyComponent[0].getCachedLayout(c);

                testFinished.countDown();
              }
            });

    thread1.start();
    thread2.start();

    try {
      testFinished.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new IllegalStateException();
    }

    // On this thread, the cached layout should be null
    assertThat(component.getCachedLayout(c)).isNull();

    // The saved internal node should be not null
    assertThat(cachedLayouts[0]).isNotNull();

    // makeShallowCopy will create a thread local var for the cached layout but its value will be
    // null
    assertThat(resultCopyComponent[0].getCachedLayout(c)).isNull();

    // The saved internal node should also be null
    assertThat(cachedLayouts[1]).isNull();
  }

  @Test
  public void testShallowCopyCachedLayoutDifferentThreadsMeasureCopy() {
    final LayoutState layoutState = new LayoutState(mContext);
    final ComponentContext c = new ComponentContext(mContext);
    c.setLayoutStateReferenceWrapper(new LayoutStateReferenceWrapper(layoutState));

    Component component = TestDrawableComponent.create(mContext).build();
    Component[] resultCopyComponent = new Component[1];
    InternalNode[] cachedLayouts = new InternalNode[2];

    CountDownLatch lockWaitMeasure = new CountDownLatch(1);
    CountDownLatch testFinished = new CountDownLatch(1);

    Thread thread1 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                component.measure(c, 100, 100, new Size());

                cachedLayouts[0] = component.getCachedLayout(c);

                lockWaitMeasure.countDown();
              }
            });

    Thread thread2 =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                // Wait for component to have a cached layout, then make copy.
                try {
                  lockWaitMeasure.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                  throw new IllegalStateException();
                }

                resultCopyComponent[0] = component.makeShallowCopy();
                resultCopyComponent[0].measure(c, 100, 100, new Size());
                cachedLayouts[1] = resultCopyComponent[0].getCachedLayout(c);

                testFinished.countDown();
              }
            });

    thread1.start();
    thread2.start();

    try {
      testFinished.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new IllegalStateException();
    }

    // On this thread, the cached layout should be null
    assertThat(component.getCachedLayout(c)).isNull();

    // The saved internal node should be not null
    assertThat(cachedLayouts[0]).isNotNull();

    // On this thread, the cached layout should be null
    assertThat(resultCopyComponent[0].getCachedLayout(c)).isNull();

    // The saved internal node should be not null
    assertThat(cachedLayouts[1]).isNotNull();

    // the internal nodes should be different
    assertThat(cachedLayouts[0]).isNotEqualTo(cachedLayouts[1]);
  }
}
