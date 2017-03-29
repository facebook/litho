/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.os.Handler;
import android.os.Looper;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestLayoutComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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
            .layoutDiffing(true)
            .layoutLock(mLayoutLock)
            .layoutThreadLooper(mLooper)
            .build();

    assertSameAsInternalState(componentTree, mRoot, "mRoot");
    assertEqualToInternalState(componentTree, true, "mIsLayoutDiffingEnabled");
    assertSameAsInternalState(componentTree, mLayoutLock, "mLayoutLock");

    assertTrue(componentTree.isIncrementalMountEnabled());
    assertEquals(mComponentsLogger, mContext.getLogger());
    assertEquals(mLogTag, mContext.getLogTag());

    Handler handler = Whitebox.getInternalState(componentTree, "mLayoutThreadHandler");
    assertSame(mLooper, handler.getLooper());
  }

  @Test
  public void testReleaseAndInit() {
    mComponentTreeBuilder
        .layoutDiffing(true)
        .layoutLock(mLayoutLock)
        .layoutThreadLooper(mLooper);

    mComponentTreeBuilder.release();

    Component root = TestLayoutComponent.create(mContext)
        .build();

