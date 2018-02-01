/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.common;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.Component;
import com.facebook.litho.Diff;
import com.facebook.litho.sections.Change;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Tests {@link SingleComponentSectionSpec} */
@RunWith(ComponentsTestRunner.class)
public class SingleComponentSectionSpecTest {

  private SectionContext mSectionContext;
  private Diff<Component> mComponentDiff;
  private Diff<Boolean> mStickyDiff;
  private Diff<Integer> mSpanSizeDiff;
  private Diff<Boolean> mIsFullSpanDiff;
  private ChangeSet mChangeSet;

  @Before
  public void setup() throws Exception {
    mSectionContext = new SectionContext(RuntimeEnvironment.application);
    mComponentDiff = new Diff<>();
    mStickyDiff = new Diff<>();
    mSpanSizeDiff = new Diff<>();
    mIsFullSpanDiff = new Diff<>();
    mChangeSet = ChangeSet.acquireChangeSet();
  }

  @Test
  public void testDeleteComponent() {
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext, mChangeSet, mComponentDiff, mStickyDiff, mSpanSizeDiff, mIsFullSpanDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.DELETE);
  }

  @Test
  public void testInsertComponent() {
    Component component = mock(Component.class);
    mComponentDiff.init(null, component);
    mStickyDiff.init(null, true);
    mSpanSizeDiff.init(null, 2);
    mIsFullSpanDiff.init(null, true);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext, mChangeSet, mComponentDiff, mStickyDiff, mSpanSizeDiff, mIsFullSpanDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.INSERT);
    assertThat(change.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(change.getRenderInfo().isSticky()).isTrue();
    assertThat(change.getRenderInfo().getSpanSize()).isEqualTo(2);
    assertThat(change.getRenderInfo().isFullSpan()).isTrue();
  }

  @Test
  public void testUpdateComponent() {
    Component nextComponent = mock(Component.class);
    Component prevComponent = mock(Component.class);
    when(prevComponent.isEquivalentTo(nextComponent)).thenReturn(false);

    mComponentDiff.init(prevComponent, nextComponent);
    mStickyDiff.init(true, true);
    mSpanSizeDiff.init(2, 2);
    mIsFullSpanDiff.init(true, true);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext, mChangeSet, mComponentDiff, mStickyDiff, mSpanSizeDiff, mIsFullSpanDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.UPDATE);
    assertThat(change.getRenderInfo().getComponent()).isEqualTo(nextComponent);
    assertThat(change.getRenderInfo().isSticky()).isTrue();
    assertThat(change.getRenderInfo().getSpanSize()).isEqualTo(2);
    assertThat(change.getRenderInfo().isFullSpan()).isTrue();
  }

  @Test
  public void testUpdateSpanSize() {
    Component component = mock(Component.class);
    mComponentDiff.init(component, component);
    mStickyDiff.init(true, true);
    mSpanSizeDiff.init(1, 2);
    mIsFullSpanDiff.init(true, true);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext, mChangeSet, mComponentDiff, mStickyDiff, mSpanSizeDiff, mIsFullSpanDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.UPDATE);
    assertThat(change.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(change.getRenderInfo().isSticky()).isTrue();
    assertThat(change.getRenderInfo().getSpanSize()).isEqualTo(2);
    assertThat(change.getRenderInfo().isFullSpan()).isTrue();
  }

  @Test
  public void testUpdateSticky() {
    Component component = mock(Component.class);
    mComponentDiff.init(component, component);
    mStickyDiff.init(true, false);
    mSpanSizeDiff.init(1, 1);
    mIsFullSpanDiff.init(true, true);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext, mChangeSet, mComponentDiff, mStickyDiff, mSpanSizeDiff, mIsFullSpanDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.UPDATE);
    assertThat(change.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(change.getRenderInfo().isSticky()).isFalse();
    assertThat(change.getRenderInfo().getSpanSize()).isEqualTo(1);
    assertThat(change.getRenderInfo().isFullSpan()).isTrue();
  }

  @Test
  public void testUpdateIsFullSpan() {
    Component component = mock(Component.class);
    mComponentDiff.init(component, component);
    mStickyDiff.init(true, true);
    mSpanSizeDiff.init(1, 1);
    mIsFullSpanDiff.init(true, false);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext, mChangeSet, mComponentDiff, mStickyDiff, mSpanSizeDiff, mIsFullSpanDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.UPDATE);
    assertThat(change.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(change.getRenderInfo().isSticky()).isTrue();
    assertThat(change.getRenderInfo().getSpanSize()).isEqualTo(1);
    assertThat(change.getRenderInfo().isFullSpan()).isFalse();
  }

  @Test
  public void testNoUpdate() {
    Component nextComponent = mock(Component.class);
    Component prevComponent = mock(Component.class);
    when(prevComponent.isEquivalentTo(nextComponent)).thenReturn(true);

    mComponentDiff.init(prevComponent, nextComponent);
    mStickyDiff.init(true, true);
    mSpanSizeDiff.init(1, 1);
    mIsFullSpanDiff.init(true, true);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext, mChangeSet, mComponentDiff, mStickyDiff, mSpanSizeDiff, mIsFullSpanDiff);

    assertThat(mChangeSet.getChangeCount()).isEqualTo(0);
  }

  private static Change verifyChangeSetAndGetTheChange(ChangeSet changeSet) {
    assertThat(changeSet.getChangeCount()).isEqualTo(1);
    return changeSet.getChangeAt(changeSet.getChangeCount() - 1);
  }
}
