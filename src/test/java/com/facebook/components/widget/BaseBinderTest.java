/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Looper;
import android.view.ViewGroup;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.ComponentView;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(ComponentsTestRunner.class)
public class BaseBinderTest {

  private static final int WIDTH = 200;
  private static final int HEIGHT = 400;

  private List<String> mItems;
  private MyTestComponentBinder mBinder;
  private ShadowLooper mLayoutThreadShadowLooper;

  private ComponentView mView;
  private ComponentView mComponentView1;
  private ComponentView mComponentView2;

  @Before
  public void setup() throws Exception {
    mItems = new ArrayList<>();
    mItems.add("0");
    mItems.add("1");
    mItems.add("2");
    mItems.add("3");
    mItems.add("4");
    mItems.add("5");
    mItems.add("6");
    mItems.add("7");
    mItems.add("8");

    Context context = RuntimeEnvironment.application;
    mView = new ComponentView(context);
    mComponentView1 = mock(ComponentView.class);
    mComponentView2 = mock(ComponentView.class);

    mView.addView(mComponentView1);
    mView.addView(mComponentView2);

    mBinder = new MyTestComponentBinder(context, mItems);

    mLayoutThreadShadowLooper = Shadows.shadowOf(
        (Looper) Whitebox.invokeMethod(
            ComponentTree.class,
            "getDefaultLayoutThreadLooper"));
  }

  @Test
  public void testNotifyDataSetChanged() {
    Assert.assertEquals(0, mBinder.getComponentCount());

    mount();

    Assert.assertEquals(mItems.get(0), getAdapterInputStringAtPosition(0));
  }

  private void mount() {
    mBinder.setSize(WIDTH, HEIGHT);
    mBinder.mount(mView);
  }

