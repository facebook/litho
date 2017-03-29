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

import static org.mockito.Mockito.mock;

@RunWith(ComponentsTestRunner.class)
public class WorkingRangesTest {

  private static final int WIDTH = 200;
  private static final int HEIGHT = 400;

  private static final int RANGE_ITEM_COUNT = 3;

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
    mBinder.setSize(WIDTH, HEIGHT);
    mBinder.mount(mView);

    mLayoutThreadShadowLooper = Shadows.shadowOf(
        (Looper) Whitebox.invokeMethod(
            ComponentTree.class,
            "getDefaultLayoutThreadLooper"));
  }

  private void performNotifyDataSetChanged() {
    mBinder.notifyDataSetChanged();
    mLayoutThreadShadowLooper.runOneTask();
  }

  @Test
  public void testWorkingRanges() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(2, 5);

    Assert.assertEquals(5, mBinder.getComponentCount());

    // The items outside of the range should not be loaded.
    Assert.assertNull(getAdapterInputStringAtPosition(1));
    Assert.assertNull(getAdapterInputStringAtPosition(8));

    for (int i = 2; i < 7; i++) {
      Assert.assertEquals(mItems.get(i), getAdapterInputStringAtPosition(i));
    }
  }

  @Test
  public void testNotifyItemChangedInsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(4);

    Assert.assertEquals("6", getAdapterInputStringAtPosition(6));
    mItems.set(6, "23");
    // Nothing changed until the notify call
    Assert.assertEquals("6", getAdapterInputStringAtPosition(6));

    mBinder.notifyItemChanged(6);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals("23", getAdapterInputStringAtPosition(6));
    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());
  }

  @Test
  public void testNotifyItemChangedOutsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(2);

    // The component should not be loaded because it is outside the working range.
    Assert.assertNull(getAdapterInputStringAtPosition(6));
    mItems.set(6, "23");

    mBinder.notifyItemChanged(6);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertNull(getAdapterInputStringAtPosition(6));
    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());

    // Move range so that it includes the changed item.
    mBinder.getRangeController().setWorkingRangeAt(5);

    Assert.assertEquals("23", getAdapterInputStringAtPosition(6));
    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());
  }

  @Test
  public void testNotifyItemRangeChangedInsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(5);

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

    // Since the changed range overlaps with the working range, the reunion of the components inside
    // the working range and inside the changed range will be loaded.
    Assert.assertEquals(RANGE_ITEM_COUNT + 1, mBinder.getComponentCount());

    for (int i = 0; i < newRange.size(); i++) {
      Assert.assertEquals(newRange.get(i), getAdapterInputStringAtPosition(positionStart + i));
    }

    int firstOriginalItemAfterRange = positionStart + newRange.size();
    Assert.assertEquals(
        mItems.get(firstOriginalItemAfterRange),
        getAdapterInputStringAtPosition(firstOriginalItemAfterRange));
  }

  @Test
  public void testNotifyItemRangeChangedOutsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(1);

    List<String> newRange = new ArrayList<>();
    newRange.add("10");
    newRange.add("11");

    int positionStart = 4;
    for (int i = positionStart; i < positionStart + newRange.size(); i++) {
      mItems.set(i, newRange.get(i - positionStart));
    }

    mBinder.notifyItemRangeChanged(positionStart, newRange.size());
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());

    for (int i = 0; i < newRange.size(); i++) {
      Assert.assertNull(getAdapterInputStringAtPosition(positionStart + i));
    }

    // Move range so that it includes the changed items.
    mBinder.getRangeController().setWorkingRangeAt(positionStart);

    for (int i = 0; i < newRange.size(); i++) {
      Assert.assertEquals(newRange.get(i), getAdapterInputStringAtPosition(positionStart + i));
    }

    int firstOriginalItemAfterRange = positionStart + newRange.size();
    Assert.assertEquals(
        mItems.get(firstOriginalItemAfterRange),
        getAdapterInputStringAtPosition(firstOriginalItemAfterRange));
  }

  @Test
  public void testNotifyItemInsertedInsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(5);

    String newItem = "20";
    int position = 6;
    String prevItemAtInsertedPosition = mItems.get(position);

    mItems.add(position, newItem);

    mBinder.notifyItemInserted(position);
    mLayoutThreadShadowLooper.runOneTask();

    // Loads the components within the working range and the newly inserted component.
    Assert.assertEquals(RANGE_ITEM_COUNT + 1, mBinder.getComponentCount());
    Assert.assertEquals(newItem, getAdapterInputStringAtPosition(position));
    Assert.assertEquals(prevItemAtInsertedPosition, getAdapterInputStringAtPosition(position + 1));
  }

  @Test
  public void testNotifyItemInsertedAfterWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(1);

    String newItem = "20";
    int position = 6;
    String prevItemAtInsertedPosition = mItems.get(position);

    mItems.add(position, newItem);

    mBinder.notifyItemInserted(position);
    mLayoutThreadShadowLooper.runOneTask();

    // Loads just the components within the working range.
    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());

    mBinder.getRangeController().setWorkingRangeAt(5);

    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());
    Assert.assertEquals(newItem, getAdapterInputStringAtPosition(position));
    Assert.assertEquals(prevItemAtInsertedPosition, getAdapterInputStringAtPosition(position + 1));
  }

  @Test
  public void testNotifyItemInsertedBeforeWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(3);

    String newItem = "20";
    int position = 2;
    String prevItemAtInsertedPosition = mItems.get(position);

    mItems.add(position, newItem);

    mBinder.notifyItemInserted(position);
    mLayoutThreadShadowLooper.runOneTask();

    // Loads just the components within the working range.
    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());

    mBinder.getRangeController().setWorkingRangeAt(2);

    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());
    Assert.assertEquals(newItem, getAdapterInputStringAtPosition(position));
    Assert.assertEquals(prevItemAtInsertedPosition, getAdapterInputStringAtPosition(position + 1));
    Assert.assertEquals(mItems.get(position + 2), getAdapterInputStringAtPosition(position + 2));
  }

  @Test
  public void testNotifyRangeInsertedInsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(5);

    List<String> newRange = new ArrayList<>();
    newRange.add("10");
    newRange.add("11");
    newRange.add("12");

    int positionStart = 6;
    mItems.addAll(positionStart, newRange);

    mBinder.notifyItemRangeInserted(positionStart, newRange.size());
    mLayoutThreadShadowLooper.runOneTask();

    // Loads the components within the working range and the newly inserted components.
    Assert.assertEquals(RANGE_ITEM_COUNT + newRange.size(), mBinder.getComponentCount());

    for (int i = 5; i < 11; i++) {
      Assert.assertEquals(mItems.get(i), getAdapterInputStringAtPosition(i));
    }

    mBinder.getRangeController().setWorkingRangeAt(1);
    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());
  }

  @Test
  public void testNotifyRangeInsertedOutsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(2);

    List<String> newRange = new ArrayList<>();
    newRange.add("10");
    newRange.add("11");

    int positionStart = 1;
    mItems.addAll(positionStart, newRange);

    mBinder.notifyItemRangeInserted(positionStart, newRange.size());
    mLayoutThreadShadowLooper.runOneTask();

    // Loads just the components within the working range.
    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());

    // Move range so that it includes the changed items.
    mBinder.getRangeController().setWorkingRangeAt(positionStart);

    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());

    for (int i = 0; i < newRange.size(); i++) {
      Assert.assertEquals(newRange.get(i), getAdapterInputStringAtPosition(positionStart + i));
    }

    int firstOriginalItemAfterRange = positionStart + newRange.size();
    Assert.assertEquals(
        mItems.get(firstOriginalItemAfterRange),
        getAdapterInputStringAtPosition(firstOriginalItemAfterRange));
  }

  @Test
  public void testNotifyItemMovedToWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(5);

    int from = 4;
    int to = 6;

    String movingItem = mItems.remove(from);
    mItems.add(to, movingItem);

    mBinder.notifyItemMoved(from, to);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(RANGE_ITEM_COUNT + 1, mBinder.getComponentCount());
    Assert.assertEquals(movingItem, getAdapterInputStringAtPosition(to));
  }

  @Test
  public void testNotifyItemMovedFromWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(3);

    int from = 4;
    int to = 6;

    String movingItem = mItems.remove(from);
    mItems.add(to, movingItem);

    mBinder.notifyItemMoved(from, to);
    mLayoutThreadShadowLooper.runOneTask();

    // All the components are loaded, except for the one that was moved.
    Assert.assertEquals(RANGE_ITEM_COUNT - 1, mBinder.getComponentCount());
    Assert.assertEquals("5", getAdapterInputStringAtPosition(from));

    // Move range so that it includes the changed item.
    mBinder.getRangeController().setWorkingRangeAt(5);

    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());
    Assert.assertEquals(movingItem, getAdapterInputStringAtPosition(to));
  }

  @Test
  public void testNotifyItemMovedOutsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(3);

    int from = 1;
    int to = 6;

    String movingItem = mItems.remove(from);
    mItems.add(to, movingItem);

    mBinder.notifyItemMoved(from, to);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());
    Assert.assertEquals(mItems.get(2), getAdapterInputStringAtPosition(2));
    Assert.assertEquals(mItems.get(3), getAdapterInputStringAtPosition(3));
    Assert.assertEquals(mItems.get(4), getAdapterInputStringAtPosition(4));
  }

  @Test
  public void testNotifyItemRemovedInsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(3);

    int position = 4;
    String itemNextToRemoved = mItems.get(position + 1);
    mItems.remove(position);

    mBinder.notifyItemRemoved(position);
    mLayoutThreadShadowLooper.runOneTask();

    // All the components within the working range are loaded except for the deleted item.
    Assert.assertEquals(RANGE_ITEM_COUNT - 1, mBinder.getComponentCount());
    Assert.assertEquals(itemNextToRemoved, getAdapterInputStringAtPosition(position));
  }

  @Test
  public void testNotifyItemRemovedBeforeWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(4);

    int position = 2;
    String itemNextToRemoved = mItems.get(position + 1);
    mItems.remove(position);

    mBinder.notifyItemRemoved(position);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());
    Assert.assertEquals(mItems.get(4), getAdapterInputStringAtPosition(4));

    // Move range so that it includes the changed item.
    mBinder.getRangeController().setWorkingRangeAt(1);

    Assert.assertEquals(itemNextToRemoved, getAdapterInputStringAtPosition(position));
  }

  @Test
  public void testNotifyItemRemovedAfterWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(1);

    int position = 4;
    String itemNextToRemoved = mItems.get(position + 1);
    mItems.remove(position);

    mBinder.notifyItemRemoved(position);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());

    // Move range so that it includes the changed item.
    mBinder.getRangeController().setWorkingRangeAt(3);

    Assert.assertEquals(itemNextToRemoved, getAdapterInputStringAtPosition(position));
  }

  @Test
  public void testNotifyItemRangeRemovedInsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(3);

    int positionStart = 4;
    int itemCount = 3;
    mItems.subList(positionStart, positionStart + itemCount).clear();

    mBinder.notifyItemRangeRemoved(positionStart, itemCount);
    mLayoutThreadShadowLooper.runOneTask();

    // All the components within the working range are loaded except for the deleted items.
    Assert.assertEquals(RANGE_ITEM_COUNT - 2, mBinder.getComponentCount());
    Assert.assertEquals("3", getAdapterInputStringAtPosition(3));
    Assert.assertNull(getAdapterInputStringAtPosition(4));
    Assert.assertNull(getAdapterInputStringAtPosition(5));
  }

  @Test
  public void testNotifyItemRangeRemovedOutsideWorkingRange() {
    // Set the first list of elements
    performNotifyDataSetChanged();

    mBinder.getRangeController().setWorkingRangeAt(5);

    int positionStart = 1;
    int itemCount = 3;
    mItems.subList(positionStart, positionStart + itemCount).clear();

    mBinder.notifyItemRangeRemoved(positionStart, itemCount);
    mLayoutThreadShadowLooper.runOneTask();

    Assert.assertEquals(RANGE_ITEM_COUNT, mBinder.getComponentCount());

    Assert.assertEquals("5", getAdapterInputStringAtPosition(2));
    Assert.assertEquals("6", getAdapterInputStringAtPosition(3));
    Assert.assertEquals("7", getAdapterInputStringAtPosition(4));

    // Move range so that it includes the changed item.
    mBinder.getRangeController().setWorkingRangeAt(0);

    Assert.assertEquals("0", getAdapterInputStringAtPosition(0));
    Assert.assertEquals("4", getAdapterInputStringAtPosition(1));
    Assert.assertEquals("5", getAdapterInputStringAtPosition(2));
  }

  private String getAdapterInputStringAtPosition(int position) {
    ComponentTree componentTree = mBinder.getComponentAt(position);

    if (componentTree == null) {
      return null;
    }

    return Whitebox.getInternalState(Whitebox.getInternalState(componentTree, "mRoot"), "text");
  }

  private static class MyTestComponentBinder extends BaseBinder<ViewGroup,
