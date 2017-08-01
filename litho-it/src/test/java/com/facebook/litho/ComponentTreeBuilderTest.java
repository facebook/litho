/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.powermock.reflect.Whitebox.getInternalState;

import android.os.Handler;
import android.os.Looper;
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests for {@link ComponentTree.Builder}
 */

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeBuilderTest {
  private static final String mLogTag = "logTag";
  private final Object mLayoutLock = new Object();

  private ComponentContext mContext;
  private Component mRoot;
  private ComponentTree.Builder mComponentTreeBuilder;
  private Looper mLooper;
  private ComponentsLogger mComponentsLogger;

  @Before
  public void setup() throws Exception {
    mLooper = mock(Looper.class);
    mComponentsLogger = mock(ComponentsLogger.class);
    mContext = new ComponentContext(RuntimeEnvironment.application, mLogTag, mComponentsLogger);
    mRoot = TestLayoutComponent.create(mContext)
        .build();

    mComponentTreeBuilder = ComponentTree.create(mContext, mRoot);
  }

  @Test
  public void testDefaultCreation() {
    ComponentTree componentTree = mComponentTreeBuilder.build();

    assertSameAsInternalState(componentTree, mRoot, "mRoot");
    assertDefaults(componentTree);
  }

  @Test
  public void testCreationWithInputs() {
    ComponentTree componentTree =
        mComponentTreeBuilder
            .layoutLock(mLayoutLock)
            .layoutThreadLooper(mLooper)
            .build();

    assertSameAsInternalState(componentTree, mRoot, "mRoot");
    assertEqualToInternalState(componentTree, true, "mIsLayoutDiffingEnabled");
    assertSameAsInternalState(componentTree, mLayoutLock, "mLayoutLock");

    assertThat(componentTree.isIncrementalMountEnabled()).isTrue();
    assertThat(mContext.getLogger()).isEqualTo(mComponentsLogger);
    assertThat(mContext.getLogTag()).isEqualTo(mLogTag);

    Handler handler = getInternalState(componentTree, "mLayoutThreadHandler");
    assertThat(mLooper).isSameAs(handler.getLooper());
  }

  @Test
  public void testReleaseAndInit() {
    mComponentTreeBuilder
        .layoutLock(mLayoutLock)
        .layoutThreadLooper(mLooper);

    mComponentTreeBuilder.release();

    Component root = TestLayoutComponent.create(mContext)
        .build();

    mComponentTreeBuilder.init(mContext, root);

    ComponentTree componentTree = mComponentTreeBuilder.build();

    assertSameAsInternalState(componentTree, root, "mRoot");
    assertDefaults(componentTree);
  }

  private static void assertSameAsInternalState(
      ComponentTree componentTree,
      Object object,
      String internalName) {
    assertThat(object).isSameAs(getInternalState(componentTree, internalName));
  }

  private static void assertEqualToInternalState(
      ComponentTree componentTree,
      Object object,
      String internalName) {
    assertThat((Object) getInternalState(componentTree, internalName)).isEqualTo(object);
  }

  private static void assertDefaults(ComponentTree componentTree) {
    assertEqualToInternalState(componentTree, true, "mIsLayoutDiffingEnabled");
    assertSameAsInternalState(componentTree, null, "mLayoutLock");

    assertThat(componentTree.isIncrementalMountEnabled()).isTrue();
  }
}
