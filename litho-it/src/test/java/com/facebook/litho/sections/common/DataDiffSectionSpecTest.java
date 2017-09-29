/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.common;

import static com.facebook.litho.testing.sections.TestTarget.DELETE;
import static com.facebook.litho.testing.sections.TestTarget.INSERT;
import static com.facebook.litho.testing.sections.TestTarget.MOVE;
import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionTree;
import com.facebook.litho.testing.sections.TestGroupSection;
import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.sections.TestTarget.Operation;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link DataDiffSectionSpec} */
@RunWith(ComponentsTestRunner.class)
public class DataDiffSectionSpecTest {

  private SectionContext mSectionContext;
  private SectionTree mSectionTree;
  private TestTarget mTestTarget;

  @Before
  public void setup() throws Exception {
    mSectionContext = new SectionContext(RuntimeEnvironment.application);
    mTestTarget = new TestTarget();
    mSectionTree = SectionTree.create(mSectionContext, mTestTarget).build();
  }

  @Test
  public void testSetRoot() {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add(""+i);
    }

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    final List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);
  }

  @Test
  public void testAppendData() {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add(""+i);
    }

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    data = new ArrayList<>();
    for (int i = 0; i < 200; i++) {
      data.add(""+i);
    }

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 100, 100);
  }

  @Test
  public void testInsertData() {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add(""+i);
    }

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add(""+i);
    }

    data.add(6,"new item");
    data.add(9,"new item");
    data.add(12,"new item");

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(3);
    assertThat(executedOperations.get(0).mOp).isEqualTo(INSERT);
    assertThat(executedOperations.get(0).mIndex).isEqualTo(10);

    assertThat(executedOperations.get(1).mOp).isEqualTo(INSERT);
    assertThat(executedOperations.get(1).mIndex).isEqualTo(8);

    assertThat(executedOperations.get(2).mOp).isEqualTo(INSERT);
    assertThat(executedOperations.get(2).mIndex).isEqualTo(6);
  }

  @Test
  public void testMoveData() {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      data.add(""+i);
    }

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 3);

    mTestTarget.clear();

    data = new ArrayList<>();
    for (int i = 2; i >= 0; i--) {
      data.add(""+i);
    }

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(2);

    assertThat(executedOperations.get(0).mOp).isEqualTo(MOVE);
    assertThat(executedOperations.get(0).mIndex).isEqualTo(1);
    assertThat(executedOperations.get(0).mToIndex).isEqualTo(0);

    assertThat(executedOperations.get(1).mOp).isEqualTo(MOVE);
    assertThat(executedOperations.get(1).mIndex).isEqualTo(2);
    assertThat(executedOperations.get(1).mToIndex).isEqualTo(0);
  }

  @Test
  public void testRemoveRangeData() {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add(""+i);
    }

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    data = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      data.add(""+i);
    }
    for (int i = 90; i < 100; i++) {
      data.add(""+i);
    }
    // data = [0...49, 90...99]

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(1).isEqualTo(executedOperations.size());
    assertRangeOperation(executedOperations.get(0), TestTarget.DELETE_RANGE, 50, 40);
  }

  @Test
  public void testRemoveData() throws Exception {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add(""+i);
    }

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add(""+i);
    }
    data.remove(9);
    data.remove(91);

    mSectionTree.setRoot(TestGroupSection.create(mSectionContext).data(data).build());
    executedOperations = mTestTarget.getOperations();

    assertThat(2).isEqualTo(executedOperations.size());
    assertThat(executedOperations.get(0).mOp).isEqualTo(DELETE);
    assertThat(executedOperations.get(0).mIndex).isEqualTo(92);
    assertThat(executedOperations.get(1).mOp).isEqualTo(DELETE);
    assertThat(executedOperations.get(1).mIndex).isEqualTo(9);
  }

  @Test
  public void testUpdateData() {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add(""+i);
    }

    mSectionTree.setRoot(
        TestGroupSection.create(mSectionContext)
            .data(data)
            .build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 100);

    mTestTarget.clear();

    data = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      data.add("different "+i);
    }

    mSectionTree.setRoot(TestGroupSection
        .create(mSectionContext)
        .isSameItemComparator(new Comparator() {
          @Override
          public int compare(Object lhs, Object rhs) {
            return 0;
          }
        })
        .isSameContentComparator(new Comparator() {
          @Override
          public int compare(Object lhs, Object rhs) {
            return -1;
          }
        })
        .data(data)
        .build());
    executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.UPDATE_RANGE, 0, 100);
  }

  @Test
  public void testShuffledDataWithUpdates() {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 40; i++) {
      data.add(""+i);
    }

    Collections.shuffle(data);

    mSectionTree.setRoot(
        TestGroupSection.create(mSectionContext)
            .data(data)
            .build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 40);

    mTestTarget.clear();

    data = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      data.add(""+i);
    }

    Collections.shuffle(data);

    mSectionTree.setRoot(TestGroupSection
        .create(mSectionContext)
        .isSameItemComparator(new Comparator() {
          @Override
          public int compare(Object lhs, Object rhs) {
            String left = (String) lhs;
            String right = (String) rhs;
            return left.compareTo(right);
          }
        })
        .isSameContentComparator(new Comparator() {
          @Override
          public int compare(Object lhs, Object rhs) {
            return -1;
          }
        })
        .data(data)
        .build());

    executedOperations = mTestTarget.getOperations();
    assertBulkOperations(executedOperations,0, 20,20);
  }


  @Test
  public void testShuffledData() {
    ArrayList<String> data = new ArrayList<>();
    for (int i = 0; i < 40; i++) {
      data.add(""+i);
    }

    Collections.shuffle(data);

    mSectionTree.setRoot(
        TestGroupSection.create(mSectionContext)
            .data(data)
            .build());
    List<Operation> executedOperations = mTestTarget.getOperations();

    assertThat(executedOperations.size()).isEqualTo(1);
    assertRangeOperation(executedOperations.get(0), TestTarget.INSERT_RANGE, 0, 40);

    mTestTarget.clear();

    data = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      data.add(""+i);
    }

    Collections.shuffle(data);

    mSectionTree.setRoot(TestGroupSection
        .create(mSectionContext)
        .isSameItemComparator(new Comparator() {
          @Override
          public int compare(Object lhs, Object rhs) {
            String left = (String) lhs;
            String right = (String) rhs;
            return left.compareTo(right);
          }
        })
        .data(data)
        .build());

    executedOperations = mTestTarget.getOperations();

    assertBulkOperations(executedOperations, 0, 0, 20);
  }

  private void assertRangeOperation(
      Operation operation,
      int opType,
      int startIndex,
      int rangeCount) {
    assertEquals("opreation type", operation.mOp, opType);
    assertEquals("operation starting index", operation.mIndex, startIndex);
    assertEquals("operation range count", operation.mRangeCount, rangeCount);
  }

  private void assertBulkOperations(
      List<Operation> operations,
      int expectedInserted,
      int expectedUpdated,
      int expectedRemoved) {

    int totalRemoved = 0;
    int totalUpdated = 0;
    int totalInserted = 0;

    for (int i = 0; i < operations.size(); i++) {
      Operation operation = operations.get(i);

      switch (operation.mOp) {
        case TestTarget.DELETE:
          totalRemoved++;
          break;
        case TestTarget.DELETE_RANGE:
          totalRemoved += operation.mRangeCount;
          break;
        case TestTarget.UPDATE:
          totalUpdated++;
          break;
        case TestTarget.UPDATE_RANGE:
          totalUpdated += operation.mRangeCount;
          break;
        case TestTarget.INSERT:
          totalInserted++;
          break;
        case TestTarget.INSERT_RANGE:
          totalInserted += operation.mRangeCount;
          break;
      }
    }

    assertThat(totalInserted).isEqualTo(expectedInserted);
    assertThat(totalUpdated).isEqualTo(expectedUpdated);
    assertThat(totalRemoved).isEqualTo(expectedRemoved);
  }
}
