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
import android.graphics.Color;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class LinearComponentBinderTest {

  private static final int WIDTH = 200;
  private static final int HEIGHT = 300;

  private List<Integer> mItems;
  private MyTestLinearComponentBinder mBinder;
  private ShadowLooper mLayoutThreadShadowLooper;

  private RecyclerView mView;
  private LinearLayoutManager mLayoutManager;

  @Before
  public void setup() throws Exception {
    mItems = new ArrayList<>();
    mItems.add(Color.BLACK);
    mItems.add(Color.BLUE);
    mItems.add(Color.CYAN);
    mItems.add(Color.DKGRAY);
    mItems.add(Color.GRAY);
    mItems.add(Color.GREEN);
    mItems.add(Color.LTGRAY);
    mItems.add(Color.RED);
    mItems.add(Color.MAGENTA);

    Context context = RuntimeEnvironment.application;
    ComponentsConfiguration.bootstrapBinderItems = true;

    mLayoutManager = new LinearLayoutManager(context);

    mView = new RecyclerView(context);
    mView.setLayoutManager(mLayoutManager);

    mBinder = Mockito.spy(new MyTestLinearComponentBinder(context, mLayoutManager, mItems));

    Mockito.doReturn(HEIGHT).when(mBinder).getHeight();
    Mockito.doReturn(WIDTH).when(mBinder).getWidth();
  }

