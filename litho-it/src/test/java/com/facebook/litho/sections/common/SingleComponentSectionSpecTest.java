/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.sections.common;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.litho.Component;
import com.facebook.litho.Diff;
import com.facebook.litho.sections.Change;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import java.util.HashMap;
import java.util.Map;
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
  private Diff<Map<String, Object>> mCustomAttributesDiff;
  private ChangeSet mChangeSet;

  @Before
  public void setup() throws Exception {
    mSectionContext = new SectionContext(RuntimeEnvironment.application);
    mComponentDiff = new Diff<>();
    mStickyDiff = new Diff<>();
    mSpanSizeDiff = new Diff<>();
    mIsFullSpanDiff = new Diff<>();
    mCustomAttributesDiff = new Diff<>();
    mChangeSet = ChangeSet.acquireChangeSet(null);
  }

  @Test
  public void testDeleteComponent() {
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);

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
    mCustomAttributesDiff.init(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);

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
    mCustomAttributesDiff.init(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);

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
    mCustomAttributesDiff.init(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);

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
    mCustomAttributesDiff.init(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);

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
    mCustomAttributesDiff.init(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);

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
    mCustomAttributesDiff.init(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);

    assertThat(mChangeSet.getChangeCount()).isEqualTo(0);
  }

  @Test
  public void testUpdateCustomAttributes() {
    Component component = mock(Component.class);
    when(component.isEquivalentTo(any())).thenReturn(true);
    mComponentDiff.init(component, component);
    mStickyDiff.init(true, true);
    mSpanSizeDiff.init(1, 1);
    mIsFullSpanDiff.init(true, false);
    final HashMap<String, Object> attrs = new HashMap<>();
    attrs.put("test", true);
    mCustomAttributesDiff.init(null, attrs);

    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.UPDATE);
    assertThat(change.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(change.getRenderInfo().isSticky()).isTrue();
    assertThat(change.getRenderInfo().getSpanSize()).isEqualTo(1);
    assertThat(change.getRenderInfo().isFullSpan()).isFalse();
    assertThat((Boolean) change.getRenderInfo().getCustomAttribute("test")).isTrue();

    mChangeSet = ChangeSet.acquireChangeSet(null);
    mCustomAttributesDiff.init(attrs, attrs);
    mIsFullSpanDiff.init(false, false);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);
    assertThat(mChangeSet.getChangeCount()).isEqualTo(0);
    assertThat((Boolean) change.getRenderInfo().getCustomAttribute("test")).isTrue();

    mChangeSet = ChangeSet.acquireChangeSet(null);
    mCustomAttributesDiff.init(attrs, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        mComponentDiff,
        mStickyDiff,
        mSpanSizeDiff,
        mIsFullSpanDiff,
        mCustomAttributesDiff);
    change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(mChangeSet.getChangeCount()).isEqualTo(1);
    assertThat(change.getRenderInfo().getCustomAttribute("test")).isNull();
  }

  private static Change verifyChangeSetAndGetTheChange(ChangeSet changeSet) {
    assertThat(changeSet.getChangeCount()).isEqualTo(1);
    return changeSet.getChangeAt(changeSet.getChangeCount() - 1);
  }
}
