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

  private void performNotifyDataSetChanged() {
    mBinder.notifyDataSetChanged();
    mLayoutThreadShadowLooper.runOneTask();
    Assert.assertEquals(mItems.size(), mBinder.getComponentCount());
  }

  @Test
  public void testNotifyItemChanged() {
    mount();

    Assert.assertEquals("6", getAdapterInputStringAtPosition(6));
    mItems.set(6, "23");
    // Nothing changed until the notify call
    Assert.assertEquals("6", getAdapterInputStringAtPosition(6));

    mBinder.notifyItemChanged(6);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals("23", getAdapterInputStringAtPosition(6));
    Assert.assertEquals(mItems.size(), mBinder.getComponentCount());
  }

  @Test
  public void testNotifyItemRangeChanged() {
    mount();

    List<String> newRange = new ArrayList<>();
    newRange.add("10");
    newRange.add("11");
    newRange.add("12");

    int positionStart = 4;
    for (int i = positionStart; i < positionStart + newRange.size(); i++) {
      mItems.set(i, newRange.get(i - positionStart));
    }

    mBinder.notifyItemRangeChanged(positionStart, newRange.size());
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(mItems.size(), mBinder.getComponentCount());
    for (int i = 0; i < newRange.size(); i++) {
      Assert.assertEquals(newRange.get(i), getAdapterInputStringAtPosition(positionStart + i));
    }

    int firstOriginalItemAfterRange = positionStart + newRange.size();
    Assert.assertEquals(
        mItems.get(firstOriginalItemAfterRange),
        getAdapterInputStringAtPosition(firstOriginalItemAfterRange));
  }

  @Test
  public void testNotifyItemInserted() {
    mount();

    String newItem = "20";
    int position = 6;
    String prevItemAtInsertedPosition = mItems.get(position);

    mItems.add(position, newItem);

    mBinder.notifyItemInserted(position);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(mItems.size(), mBinder.getComponentCount());
    Assert.assertEquals(newItem, getAdapterInputStringAtPosition(position));
    Assert.assertEquals(prevItemAtInsertedPosition, getAdapterInputStringAtPosition(position + 1));
  }

  @Test
  public void testNotifyRangeInserted() {
    mount();

    List<String> newRange = new ArrayList<>();
    newRange.add("10");
    newRange.add("11");
    newRange.add("12");

    int positionStart = 4;
    mItems.addAll(positionStart, newRange);

    mBinder.notifyItemRangeInserted(positionStart, newRange.size());
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(mItems.size(), mBinder.getComponentCount());

    for (int i = 0; i < newRange.size(); i++) {
      Assert.assertEquals(newRange.get(i), getAdapterInputStringAtPosition(positionStart + i));
    }

    int firstOriginalItemAfterRange = positionStart + newRange.size();
    Assert.assertEquals(
        mItems.get(firstOriginalItemAfterRange),
        getAdapterInputStringAtPosition(firstOriginalItemAfterRange));
  }

  @Test
  public void testNotifyItemMoved() {
    mount();

    int from = 4;
    int to = 6;

    String movingItem = mItems.remove(from);
    mItems.add(to, movingItem);

    mBinder.notifyItemMoved(from, to);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(mItems.size(), mBinder.getComponentCount());
    Assert.assertEquals(movingItem, getAdapterInputStringAtPosition(to));
  }

  @Test
  public void testNotifyItemMovedFromBeforeCollection() {
