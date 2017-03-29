/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.content.Context;
import android.graphics.Color;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(ComponentsTestRunner.class)
public class PagerBinderTest {

  private static final int WIDTH = 200;
  private static final int HEIGHT = 300;

  private ComponentContext mContext;
  private TestPagerComponentBinder mBinder;

  @Rule
  public ComponentsRule mComponentsRule = new ComponentsRule();

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testEmptyPager() {
    PagerBinder pagerBinder = new PagerBinder(mContext) {
      @Override
      protected int getCount() {
        return 0;
      }

      @Override
      public Component<?> createComponent(ComponentContext c, int position) {
        return null;
      }
    };

    pagerBinder.setSize(WIDTH, HEIGHT);
    assertEquals(
        0,
        ((BinderTreeCollection) Whitebox.getInternalState(pagerBinder, "mComponentTrees")).size());
  }

  @Test
  public void testOffscreenPageMathWithAFullPage() {
    mBinder = new TestPagerComponentBinder(mContext, 0, 1f);

    assertEquals(1, getPagerOffscreenLimit(mBinder));
  }

  @Test
  public void testOffscreenPageMathWithMoreThanHalfPage() {
    mBinder = new TestPagerComponentBinder(mContext, 0, 0.8f);

    assertEquals(2, getPagerOffscreenLimit(mBinder));
  }

  @Test
  public void testOffscreenPageMathWithHalfPage() {
    mBinder = new TestPagerComponentBinder(mContext, 0, 0.5f);

    assertEquals(2, getPagerOffscreenLimit(mBinder));
  }

  @Test
  public void testOffscreenPageMathWithLessThanHalfPage() {
    mBinder = new TestPagerComponentBinder(mContext, 0, 0.4f);

    assertEquals(3, getPagerOffscreenLimit(mBinder));
  }

  @Test
  public void testOffscreenPageMathWithLessThanAThirdPage() {
    mBinder = new TestPagerComponentBinder(mContext, 0, 0.3f);

    assertEquals(4, getPagerOffscreenLimit(mBinder));
  }

  @Test
  public void testSimpleBinderUpdate() {
    mBinder = new TestPagerComponentBinder(mContext, 0, 1f);
    mBinder.getRangeController().notifyOnPageSelected(0);
    for (int i = 0; i < 3; i++) {
      assertNotNull(mBinder.getComponentAt(i));
    }

