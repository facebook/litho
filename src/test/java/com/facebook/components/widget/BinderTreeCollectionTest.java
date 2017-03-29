/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import android.support.v4.util.SparseArrayCompat;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestLayoutComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class BinderTreeCollectionTest {

  private BinderTreeCollection mBinderTreeCollection;

  @Before
  public void setup() {
    mBinderTreeCollection = new BinderTreeCollection();

    for (int i = 0; i < 10; i++) {
      mBinderTreeCollection.put(i + 1, createNewComponentTree());
    }
  }

  @Test
  public void testPositionOf() {
    ComponentTree treeAtPos3 = mBinderTreeCollection.get(3);

    assertEquals(3, mBinderTreeCollection.getPositionOf(treeAtPos3));

    ComponentTree newTreeAtPos5 = createNewComponentTree();
    mBinderTreeCollection.put(5, newTreeAtPos5);

    assertEquals(5, mBinderTreeCollection.getPositionOf(newTreeAtPos5));

    ComponentTree inexistentTree = createNewComponentTree();

    assertTrue(mBinderTreeCollection.getPositionOf(inexistentTree) < 0);
  }

  @Test
  public void testPutAsReplace() {
    int originalSize = mBinderTreeCollection.size();

    assertNotNull(mBinderTreeCollection.get(1));
    assertNotNull(mBinderTreeCollection.get(2));

    ComponentTree secondItem = mBinderTreeCollection.get(2);

    ComponentTree newComponentTree = createNewComponentTree();

    assertNotEquals(newComponentTree, mBinderTreeCollection.get(1));

    mBinderTreeCollection.put(1, newComponentTree);

    assertContiguous();
    assertEquals(originalSize, mBinderTreeCollection.size());
    assertEquals(newComponentTree, mBinderTreeCollection.get(1));
    assertEquals(secondItem, mBinderTreeCollection.get(2));
  }

  @Test
  public void testPutAsNewInsertion() {
    int originalSize = mBinderTreeCollection.size();

    assertNull(mBinderTreeCollection.get(originalSize + 1));

    ComponentTree newComponentTree = createNewComponentTree();
    mBinderTreeCollection.put(originalSize + 1, newComponentTree);

    assertContiguous();
    assertEquals(originalSize + 1, mBinderTreeCollection.size());
    assertEquals(newComponentTree, mBinderTreeCollection.get(originalSize + 1));
  }

  @Test
  public void testInsertInMiddle() {
    int originalSize = mBinderTreeCollection.size();

    int insertPosition = 4;
    ComponentTree treeAtPosition3 = mBinderTreeCollection.get(3);
    ComponentTree treeAtPosition4 = mBinderTreeCollection.get(4);
    ComponentTree treeAtPosition6 = mBinderTreeCollection.get(6);
    ComponentTree treeAtLastPosition = mBinderTreeCollection.get(originalSize);

    ComponentTree newComponentTree = createNewComponentTree();
    mBinderTreeCollection.insert(insertPosition, newComponentTree);

    assertContiguous();
    assertEquals(originalSize + 1, mBinderTreeCollection.size());
    assertEquals(treeAtPosition3, mBinderTreeCollection.get(3));
    assertEquals(newComponentTree, mBinderTreeCollection.get(4));
    assertEquals(treeAtPosition4, mBinderTreeCollection.get(5));
    assertEquals(treeAtPosition6, mBinderTreeCollection.get(7));
    assertEquals(treeAtLastPosition, mBinderTreeCollection.get(originalSize + 1));
  }

  @Test
  public void testInsertAsLast() {
    int originalSize = mBinderTreeCollection.size();

    ComponentTree treeAtLastPosition = mBinderTreeCollection.get(originalSize);

    ComponentTree newComponentTree = createNewComponentTree();
    mBinderTreeCollection.insert(originalSize + 1, newComponentTree);

    assertContiguous();
    assertEquals(originalSize + 1, mBinderTreeCollection.size());
    assertEquals(treeAtLastPosition, mBinderTreeCollection.get(originalSize));
    assertEquals(newComponentTree, mBinderTreeCollection.get(originalSize + 1));
  }

  @Test
  public void testMoveFromBeforeTo() {
    int originalSize = mBinderTreeCollection.size();

    ComponentTree treeAtPosition2 = mBinderTreeCollection.get(2);
    ComponentTree treeAtPosition3 = mBinderTreeCollection.get(3);
    ComponentTree treeAtPosition4 = mBinderTreeCollection.get(4);
    ComponentTree treeAtPosition5 = mBinderTreeCollection.get(5);
    ComponentTree treeAtPosition6 = mBinderTreeCollection.get(6);
    ComponentTree treeAtPosition7 = mBinderTreeCollection.get(7);

    mBinderTreeCollection.move(3, 6);

    assertContiguous();
    assertEquals(originalSize, mBinderTreeCollection.size());
    assertEquals(treeAtPosition2, mBinderTreeCollection.get(2));
    assertEquals(treeAtPosition4, mBinderTreeCollection.get(3));
    assertEquals(treeAtPosition5, mBinderTreeCollection.get(4));
    assertEquals(treeAtPosition6, mBinderTreeCollection.get(5));
    assertEquals(treeAtPosition3, mBinderTreeCollection.get(6));
    assertEquals(treeAtPosition7, mBinderTreeCollection.get(7));
  }

  @Test
  public void testMoveFromAfterTo() {
    int originalSize = mBinderTreeCollection.size();

    ComponentTree treeAtPosition2 = mBinderTreeCollection.get(2);
    ComponentTree treeAtPosition3 = mBinderTreeCollection.get(3);
    ComponentTree treeAtPosition4 = mBinderTreeCollection.get(4);
    ComponentTree treeAtPosition5 = mBinderTreeCollection.get(5);
    ComponentTree treeAtPosition6 = mBinderTreeCollection.get(6);
    ComponentTree treeAtPosition7 = mBinderTreeCollection.get(7);

    mBinderTreeCollection.move(6, 3);

    assertContiguous();
    assertEquals(originalSize, mBinderTreeCollection.size());
    assertEquals(treeAtPosition2, mBinderTreeCollection.get(2));
    assertEquals(treeAtPosition6, mBinderTreeCollection.get(3));
    assertEquals(treeAtPosition3, mBinderTreeCollection.get(4));
    assertEquals(treeAtPosition4, mBinderTreeCollection.get(5));
    assertEquals(treeAtPosition5, mBinderTreeCollection.get(6));
    assertEquals(treeAtPosition7, mBinderTreeCollection.get(7));
  }

  @Test
  public void testRemoveRange() {
    int originalSize = mBinderTreeCollection.size();
    int removePositionStart = 2;
    int removeItemCount = 3;

    ComponentTree treeAtPosition1 = mBinderTreeCollection.get(1);
    ComponentTree treeAtPosition5 = mBinderTreeCollection.get(5);
    ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    mBinderTreeCollection.removeShiftingLeft(removePositionStart, removeItemCount);

    assertContiguous();
    assertEquals(originalSize - 3, mBinderTreeCollection.size());
    assertEquals(treeAtPosition1, mBinderTreeCollection.get(1));
    assertEquals(treeAtPosition5, mBinderTreeCollection.get(2));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(mBinderTreeCollection.size()));
  }

  @Test
  public void testRemoveRangeStartBeforeFirstPosition() {
    int originalSize = mBinderTreeCollection.size();
    int removePositionStart = 0;
    int removeItemCount = 3;

    ComponentTree treeAtPosition3 = mBinderTreeCollection.get(3);
    ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    mBinderTreeCollection.removeShiftingLeft(removePositionStart, removeItemCount);

    assertContiguous();
    assertEquals(originalSize - 2, mBinderTreeCollection.size());
    assertEquals(treeAtPosition3, mBinderTreeCollection.get(0));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(mBinderTreeCollection.size() - 1));
  }

  @Test
  public void testRemoveRangeBeforeCollection() {
    int originalSize = mBinderTreeCollection.size();
    int removePositionStart = 0;
    int removeItemCount = 1;

    ComponentTree treeAtPosition1 = mBinderTreeCollection.get(1);
    ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    mBinderTreeCollection.removeShiftingLeft(removePositionStart, removeItemCount);

    assertContiguous();
    assertEquals(originalSize, mBinderTreeCollection.size());
    assertEquals(treeAtPosition1, mBinderTreeCollection.get(0));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(originalSize - 1));
  }

  @Test
  public void testInsertShiftingLeft() {
    int originalSize = mBinderTreeCollection.size();

    ComponentTree newItem = createNewComponentTree();

    ComponentTree treeAtPosition1 = mBinderTreeCollection.get(1);
    ComponentTree treeAtPosition2 = mBinderTreeCollection.get(2);
    ComponentTree treeAtPosition3 = mBinderTreeCollection.get(3);
    ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    mBinderTreeCollection.insertShiftingLeft(3, newItem);

    assertContiguous();
    assertEquals(originalSize + 1, mBinderTreeCollection.size());
    assertEquals(treeAtPosition1, mBinderTreeCollection.get(0));
    assertEquals(treeAtPosition2, mBinderTreeCollection.get(1));
    assertEquals(treeAtPosition3, mBinderTreeCollection.get(2));
    assertEquals(newItem, mBinderTreeCollection.get(3));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(originalSize));
  }

  @Test
  public void testShiftRangeRight() throws Exception {
    int originalSize = mBinderTreeCollection.size();
    int positionStart = 2;
    int itemCount = 4;
    int shiftBy = 2;

    ComponentTree treeAtPosition1 = mBinderTreeCollection.get(1);
    ComponentTree treeAtPosition2 = mBinderTreeCollection.get(2);
    ComponentTree treeAtPosition5 = mBinderTreeCollection.get(5);
    ComponentTree treeAtPosition8 = mBinderTreeCollection.get(8);
    ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    Whitebox.invokeMethod(
        mBinderTreeCollection,
        "shiftRangeRight",
        positionStart,
        itemCount,
        shiftBy);

    assertEquals(originalSize - 2, mBinderTreeCollection.size());
    assertEquals(treeAtPosition1, mBinderTreeCollection.get(1));
    assertNull(mBinderTreeCollection.get(2));
    assertNull(mBinderTreeCollection.get(3));
    assertEquals(treeAtPosition2, mBinderTreeCollection.get(4));
    assertEquals(treeAtPosition5, mBinderTreeCollection.get(7));
    assertEquals(treeAtPosition8, mBinderTreeCollection.get(8));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(originalSize));
  }

  @Test
  public void testShiftRangeRightSmallerCount() throws Exception {
    int originalSize = mBinderTreeCollection.size();
    int positionStart = 2;
    int itemCount = 2;
    int shiftBy = 4;

    ComponentTree treeAtPosition1 = mBinderTreeCollection.get(1);
    ComponentTree treeAtPosition2 = mBinderTreeCollection.get(2);
    ComponentTree treeAtPosition3 = mBinderTreeCollection.get(3);
    ComponentTree treeAtPosition8 = mBinderTreeCollection.get(8);
    ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    Whitebox.invokeMethod(
        mBinderTreeCollection,
        "shiftRangeRight",
        positionStart,
        itemCount,
        shiftBy);

    assertEquals(originalSize - 4, mBinderTreeCollection.size());
    assertEquals(treeAtPosition1, mBinderTreeCollection.get(1));
    assertNull(mBinderTreeCollection.get(2));
    assertNull(mBinderTreeCollection.get(3));
    assertNull(mBinderTreeCollection.get(4));
    assertNull(mBinderTreeCollection.get(5));
    assertEquals(treeAtPosition2, mBinderTreeCollection.get(6));
    assertEquals(treeAtPosition3, mBinderTreeCollection.get(7));
    assertEquals(treeAtPosition8, mBinderTreeCollection.get(8));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(originalSize));
  }

  @Test
  public void testShiftRangeRightStartBeforeFirstPosition() throws Exception {
    int originalSize = mBinderTreeCollection.size();
    int positionStart = 0;
    int itemCount = 4;
    int shiftBy = 2;

    ComponentTree treeAtPosition1 = mBinderTreeCollection.get(1);
    ComponentTree treeAtPosition2 = mBinderTreeCollection.get(2);
   ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    Whitebox.invokeMethod(
        mBinderTreeCollection,
        "shiftRangeRight",
        positionStart,
        itemCount,
        shiftBy);

    assertEquals(originalSize - 2, mBinderTreeCollection.size());
    assertNull(mBinderTreeCollection.get(1));
    assertNull(mBinderTreeCollection.get(2));
    assertEquals(treeAtPosition1, mBinderTreeCollection.get(3));
    assertEquals(treeAtPosition2, mBinderTreeCollection.get(4));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(originalSize));
  }

  @Test
  public void testShiftRangeLeft() throws Exception {
    int originalSize = mBinderTreeCollection.size();
    int positionStart = 4;
    int itemCount = 4;
    int shiftBy = 2;

    ComponentTree treeAtPosition1 = mBinderTreeCollection.get(1);
    ComponentTree treeAtPosition4 = mBinderTreeCollection.get(4);
    ComponentTree treeAtPosition7 = mBinderTreeCollection.get(7);
    ComponentTree treeAtPosition8 = mBinderTreeCollection.get(8);
    ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    Whitebox.invokeMethod(
        mBinderTreeCollection,
        "shiftRangeLeft",
        positionStart,
        itemCount,
        shiftBy);

    assertEquals(originalSize - 2, mBinderTreeCollection.size());
    assertEquals(treeAtPosition1, mBinderTreeCollection.get(1));
    assertEquals(treeAtPosition4, mBinderTreeCollection.get(2));
    assertEquals(treeAtPosition7, mBinderTreeCollection.get(5));
    assertNull(mBinderTreeCollection.get(6));
    assertNull(mBinderTreeCollection.get(7));
    assertEquals(treeAtPosition8, mBinderTreeCollection.get(8));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(originalSize));
  }

  @Test
  public void testShiftRangeLeftSmallerCount() throws Exception {
    int originalSize = mBinderTreeCollection.size();
    int positionStart = 4;
    int itemCount = 2;
    int shiftBy = 3;

    ComponentTree treeAtPosition4 = mBinderTreeCollection.get(4);
    ComponentTree treeAtPosition5 = mBinderTreeCollection.get(5);
    ComponentTree treeAtPosition6 = mBinderTreeCollection.get(6);
    ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    Whitebox.invokeMethod(
        mBinderTreeCollection,
        "shiftRangeLeft",
        positionStart,
        itemCount,
        shiftBy);

    assertEquals(originalSize - 3, mBinderTreeCollection.size());
    assertEquals(treeAtPosition4, mBinderTreeCollection.get(1));
    assertEquals(treeAtPosition5, mBinderTreeCollection.get(2));
    assertNull(mBinderTreeCollection.get(4));
    assertNull(mBinderTreeCollection.get(5));
    assertEquals(treeAtPosition6, mBinderTreeCollection.get(6));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(originalSize));
  }

  @Test
  public void testShiftRangeLeftStartBeforeFirstPosition() throws Exception {
    int originalSize = mBinderTreeCollection.size();
    int positionStart = 0;
    int itemCount = 4;
    int shiftBy = 2;

    ComponentTree treeAtPosition2 = mBinderTreeCollection.get(2);
    ComponentTree treeAtPosition3 = mBinderTreeCollection.get(3);
    ComponentTree treeAtPosition4 = mBinderTreeCollection.get(4);
    ComponentTree treeAtPosition5 = mBinderTreeCollection.get(5);
    ComponentTree treeAtPositionLast = mBinderTreeCollection.get(originalSize);

    Whitebox.invokeMethod(
        mBinderTreeCollection,
        "shiftRangeLeft",
        positionStart,
        itemCount,
        shiftBy);

    assertEquals(originalSize - 1, mBinderTreeCollection.size());
    assertEquals(treeAtPosition2, mBinderTreeCollection.get(0));
    assertEquals(treeAtPosition3, mBinderTreeCollection.get(1));
    assertNull(mBinderTreeCollection.get(2));
    assertNull(mBinderTreeCollection.get(3));
    assertEquals(treeAtPosition4, mBinderTreeCollection.get(4));
    assertEquals(treeAtPosition5, mBinderTreeCollection.get(5));
    assertEquals(treeAtPositionLast, mBinderTreeCollection.get(originalSize));
  }

  @Test
  public void testShiftRangeLeftStartAfterLastPosition() throws Exception {
    int originalSize = mBinderTreeCollection.size();
    int positionStart = 11;
    int itemCount = 4;
    int shiftBy = 2;

    ComponentTree treeAtPosition1 = mBinderTreeCollection.get(1);
    ComponentTree treeAtPosition8 = mBinderTreeCollection.get(8);

    Whitebox.invokeMethod(
        mBinderTreeCollection,
        "shiftRangeLeft",
        positionStart,
        itemCount,
        shiftBy);

    assertEquals(originalSize - 2, mBinderTreeCollection.size());
    assertEquals(treeAtPosition1, mBinderTreeCollection.get(1));
    assertEquals(treeAtPosition8, mBinderTreeCollection.get(8));
    assertNull(mBinderTreeCollection.get(originalSize - 1));
    assertNull(mBinderTreeCollection.get(originalSize));
  }

  @Test
  public void testRemoveInexistentKey() throws Exception {
    SparseArrayCompat array = Whitebox.getInternalState(mBinderTreeCollection, "mItems");
    int originalSize = array.size();

    array.remove(-1);

    assertEquals(originalSize, array.size());
  }

  void assertContiguous() {
    SparseArrayCompat items = Whitebox.getInternalState(mBinderTreeCollection, "mItems");
    for (int i = 0; i < items.size() - 1; i++) {
      assertEquals(items.keyAt(i) + 1, items.keyAt(i + 1));
    }
  }

  private static ComponentTree createNewComponentTree() {
    final ComponentContext c = new ComponentContext(RuntimeEnvironment.application);
    return ComponentTree.create(
        c,
        TestLayoutComponent.create(c)
            .build())
        .incrementalMount(false)
        .build();
  }
}
