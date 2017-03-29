/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import android.support.v4.util.Pools;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

@PrepareForTest(ComponentsPools.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class LayoutStateRecyclingTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  private int mUnspecifiedSizeSpec;

  @Before
  public void setUp() throws Exception {
    mUnspecifiedSizeSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
  }

  @Test
  public void testNodeRecycling() {
    Pools.SynchronizedPool<InternalNode> internalNodePool = mock(Pools.SynchronizedPool.class);

    Whitebox.setInternalState(
        ComponentsPools.class,
        "sInternalNodePool",
        internalNodePool);

    // We want to verify that we never recycle a node with a non-null parent, since that would
    // mean that the parent retains a dangling reference to a recycled node.
    Mockito.doAnswer(
        new Answer<Void>() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            InternalNode node = (InternalNode) invocation.getArguments()[0];
            assertNull("Internal node parent must be null before releasing", node.getParent());
            return null;
          }
        }).when(internalNodePool).release(Matchers.<InternalNode>any());

    // Create a layout state and release it.
