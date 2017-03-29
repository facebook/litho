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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.facebook.components.Component;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentTree;
import com.facebook.components.SizeSpec;
import com.facebook.components.config.ComponentsConfiguration;
import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class GridComponentBinderTest {

  private static final int SPAN_COUNT = 2;
  private static final int GRID_WIDTH = 200;
  private static final int GRID_HEIGHT = 300;

  private static final int[] COLORS = {
      Color.BLACK, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN, Color.RED, Color.MAGENTA
  };

  private List<Integer> mItems;
  private MyTestGridComponentBinder mBinder;
  private RecyclerView mRecyclerView;

  private Context mContext;
  private GridLayoutManager mGridLayoutManager;

  private ShadowLooper mLayoutThreadShadowLooper;

  @Before
  public void setup() throws Exception {
    mItems = new ArrayList<>();
