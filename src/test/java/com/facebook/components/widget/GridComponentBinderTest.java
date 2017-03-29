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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;

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

    // 20 items that are spread across 10 span groups. The first item occupies two spans.
    // The last span group will only contain one element.
    for (int i = 0; i < 20; i++) {
      final int index = i % COLORS.length;
      mItems.add(COLORS[index]);
    }

    mContext = RuntimeEnvironment.application;
    ComponentsConfiguration.bootstrapBinderItems = true;

    mRecyclerView = new RecyclerView(mContext);
    mRecyclerView.setPadding(1, 2, 3, 4);

    mLayoutThreadShadowLooper = Shadows.shadowOf(
        (Looper) Whitebox.invokeMethod(
            ComponentTree.class,
            "getDefaultLayoutThreadLooper"));
  }

  @Test
  public void testMatchingWidthSpec() {
    setupBinder(GridLayoutManager.VERTICAL);

    mRecyclerView.layout(0, 0, GRID_WIDTH, 2000);

    for (int i = 0; i < mItems.size(); i++) {
      assertEquals(
          mRecyclerView.getChildAt(i).getWidth(),
          SizeSpec.getSize(mBinder.getWidthSpec(i)));
    }
  }

  @Test
  public void testGetSpanIndex() {
    setupBinder(GridLayoutManager.VERTICAL);

    Assert.assertEquals(0, mBinder.getSpanIndex(0));
    Assert.assertEquals(0, mBinder.getSpanIndex(1));
    Assert.assertEquals(1, mBinder.getSpanIndex(2));
    Assert.assertEquals(0, mBinder.getSpanIndex(3));
    Assert.assertEquals(1, mBinder.getSpanIndex(8));
    Assert.assertEquals(0, mBinder.getSpanIndex(19));
  }

  @Test
  public void testWorkingRangesBasic() throws Exception {
    setupBinder(GridLayoutManager.VERTICAL);

    // Set the range controller.
    mBinder.getRangeController().notifyOnScroll(4 * SPAN_COUNT - 1, 2 * SPAN_COUNT);

    int rangeSizeInViewPorts =
        RecyclerComponentBinder.RecyclerComponentWorkingRangeController.RANGE_SIZE;
    int rangeItemCount = 2 * SPAN_COUNT * rangeSizeInViewPorts;
    Assert.assertEquals(rangeItemCount, mBinder.getComponentCount());

    // The items outside of the range should not be loaded.
    Assert.assertEquals(null, getAdapterInputStringAtPosition(2 * SPAN_COUNT - 3));
    Assert.assertEquals(null, getAdapterInputStringAtPosition(2 * SPAN_COUNT - 2));
    Assert.assertEquals(null, getAdapterInputStringAtPosition(8 * SPAN_COUNT - 1));
    Assert.assertEquals(null, getAdapterInputStringAtPosition(8 * SPAN_COUNT));

    for (int i = 2 * SPAN_COUNT - 1; i < 8 * SPAN_COUNT - 1; i++) {
      Assert.assertEquals(mItems.get(i), getAdapterInputStringAtPosition(i));
    }
  }

  @Test
  public void testWorkingRangesTrimmedBeginning() throws Exception {
    setupBinder(GridLayoutManager.VERTICAL);

    // Set the range controller.
    mBinder.getRangeController().notifyOnScroll(2 * SPAN_COUNT - 1, 3 * SPAN_COUNT);

    int rangeSizeInViewPorts =
        RecyclerComponentBinder.RecyclerComponentWorkingRangeController.RANGE_SIZE;
    int rangeItemCount = 3 + 3 * SPAN_COUNT * (rangeSizeInViewPorts - 1);
    Assert.assertEquals(rangeItemCount, mBinder.getComponentCount());

    // The items outside of the range should not be loaded.
    Assert.assertEquals(null, getAdapterInputStringAtPosition(8 * SPAN_COUNT - 1));
    Assert.assertEquals(null, getAdapterInputStringAtPosition(8 * SPAN_COUNT));

    for (int i = 0; i < 8 * SPAN_COUNT - 1; i++) {
      Assert.assertEquals(mItems.get(i), getAdapterInputStringAtPosition(i));
    }
  }

  @Test
  public void testWorkingRangesTrimmedEnd() throws Exception {
    setupBinder(GridLayoutManager.VERTICAL);

    // Set the range controller.
    mBinder.getRangeController().notifyOnScroll(6 * SPAN_COUNT - 1, 3 * SPAN_COUNT);

    int rangeSizeInViewPorts =
        RecyclerComponentBinder.RecyclerComponentWorkingRangeController.RANGE_SIZE;
    int rangeItemCount = 1 + SPAN_COUNT + 3 * SPAN_COUNT * (rangeSizeInViewPorts - 1);
    Assert.assertEquals(rangeItemCount, mBinder.getComponentCount());

    // The items outside of the range should not be loaded.
    Assert.assertEquals(null, getAdapterInputStringAtPosition(3 * SPAN_COUNT - 2));

    for (int i = 3 * SPAN_COUNT - 1; i < mItems.size(); i++) {
      Assert.assertEquals(mItems.get(i), getAdapterInputStringAtPosition(i));
    }
  }

  @Test
  public void testWorkingRangesTrimmedBothEnds() {
    setupBinder(GridLayoutManager.VERTICAL);

    // Set the range controller.
    mBinder.getRangeController().notifyOnScroll(3 * SPAN_COUNT - 1, 4 * SPAN_COUNT);

    Assert.assertEquals(mItems.size(), mBinder.getComponentCount());

    for (int i = 0; i < mItems.size(); i++) {
      Assert.assertEquals(mItems.get(i), getAdapterInputStringAtPosition(i));
    }
  }

  @Test
  public void testWorkingRangesBasicHorizontal() throws Exception {
    setupBinder(GridLayoutManager.HORIZONTAL);

    // Set the range controller.
    mBinder.getRangeController().notifyOnScroll(3 * SPAN_COUNT - 1, 2 * SPAN_COUNT);

    int rangeSizeInViewPorts =
        RecyclerComponentBinder.RecyclerComponentWorkingRangeController.RANGE_SIZE;
    int rangeItemCount = 2 * SPAN_COUNT * rangeSizeInViewPorts;
    Assert.assertEquals(rangeItemCount, mBinder.getComponentCount());

    // The items outside of the range should not be loaded.
    Assert.assertEquals(null, getAdapterInputStringAtPosition(SPAN_COUNT - 2));
    Assert.assertEquals(null, getAdapterInputStringAtPosition(7 * SPAN_COUNT - 1));

    for (int i = SPAN_COUNT - 1; i < 7 * SPAN_COUNT - 1; i++) {
      Assert.assertEquals(mItems.get(i), getAdapterInputStringAtPosition(i));
    }
  }

  @Test
  public void testWorkingRangesTrimmedBothEndsHorizontal() {
    setupBinder(GridLayoutManager.HORIZONTAL);

    // Set the range controller.
    mBinder.getRangeController().notifyOnScroll(3 * SPAN_COUNT - 1, 4 * SPAN_COUNT);

    Assert.assertEquals(mItems.size(), mBinder.getComponentCount());

    for (int i = 0; i < mItems.size(); i++) {
      Assert.assertEquals(mItems.get(i), getAdapterInputStringAtPosition(i));
    }
  }

  @Test
  public void testBootstrapWorkingRangeVertical() {
    setupBinder(GridLayoutManager.VERTICAL);

    Assert.assertEquals(11, mBinder.getComponentCount());
    Assert.assertEquals(0, mBinder.getFirstPosition());
  }

  @Test
  public void testBootstrapWorkingRangeHorizontal() {
    setupBinder(GridLayoutManager.HORIZONTAL);

    Assert.assertEquals(15, mBinder.getComponentCount());
    Assert.assertEquals(0, mBinder.getFirstPosition());
  }

  @Test
  public void testBootstrapWithTrimming() {
    setupBinder(GridLayoutManager.VERTICAL);

    mBinder.setSize(GRID_WIDTH, 1000);

    Assert.assertEquals(mItems.size(), mBinder.getComponentCount());
    Assert.assertEquals(0, mBinder.getFirstPosition());
  }

  private void setupBinder(int orientation) {
    mGridLayoutManager = new GridLayoutManager(mContext, SPAN_COUNT, orientation, false);
    mGridLayoutManager.setSpanSizeLookup(
        new GridLayoutManager.SpanSizeLookup() {
          @Override
          public int getSpanSize(int position) {
            return
                (position == 0)
                    ? 2
                    : 1;
          }
        });
    mBinder = new MyTestGridComponentBinder(mContext, mItems, mGridLayoutManager);
    mBinder.setSize(GRID_WIDTH - 4, GRID_HEIGHT - 6);
    mBinder.mount(mRecyclerView);
    mBinder.bind(mRecyclerView);

    mLayoutThreadShadowLooper.runOneTask();
  }

  private Integer getAdapterInputStringAtPosition(int position) {
    ComponentTree componentTree = mBinder.getComponentAt(position);

    if (componentTree == null) {
      return null;
    }

    return Whitebox.getInternalState(Whitebox.getInternalState(componentTree, "mRoot"), "color");
  }

  private static class MyTestGridComponentBinder extends GridComponentBinder {

    private final List<Integer> mItems;

    MyTestGridComponentBinder(
        Context context,
        List<Integer> items,
        GridLayoutManager layoutManager) {
      super(context, layoutManager);
      mItems = items;
    }

    @Override
    public int getCount() {
      return mItems.size();
    }

    @Override
    public Component<?> createComponent(ComponentContext c, int position) {
