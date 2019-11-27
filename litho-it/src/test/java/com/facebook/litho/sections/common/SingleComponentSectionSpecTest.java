/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.facebook.litho.specmodels.internal.ImmutableList;
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
  private ChangeSet mChangeSet;

  @Before
  public void setup() throws Exception {
    mSectionContext = new SectionContext(RuntimeEnvironment.application);
    mChangeSet = ChangeSet.acquireChangeSet(null, false);
  }

  @Test
  public void testNullComponent() {
    Diff<Component> componentDiff = new Diff<>(null, null);
    Diff<Boolean> stickyDiff = new Diff<>(null, null);
    Diff<Integer> spanSizeDiff = new Diff<>(null, null);
    Diff<Boolean> isFullSpanDiff = new Diff<>(null, null);
    Diff<Map<String, Object>> customAttributesDiff = new Diff<>(null, null);
    Diff<Object> dataDiff = new Diff<>(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

    assertThat(mChangeSet.getChangeCount()).isEqualTo(0);
  }

  @Test
  public void testNullComponentWithRenderInfo() {
    Diff<Component> componentDiff = new Diff<>(null, null);
    Diff<Boolean> stickyDiff = new Diff<>(null, true);
    Diff<Integer> spanSizeDiff = new Diff<>(null, 1);
    Diff<Boolean> isFullSpanDiff = new Diff<>(null, true);
    Diff<Map<String, Object>> customAttributesDiff =
        new Diff<>(null, new HashMap<String, Object>());
    Diff<Object> dataDiff = new Diff<>(null, 1);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

    assertThat(mChangeSet.getChangeCount()).isEqualTo(0);
  }

  @Test
  public void testNullComponentWithPrevRenderInfo() {
    Diff<Component> componentDiff = new Diff<>(null, null);
    Diff<Boolean> stickyDiff = new Diff<>(true, true);
    Diff<Integer> spanSizeDiff = new Diff<>(2, 1);
    Diff<Boolean> isFullSpanDiff = new Diff<>(false, true);
    Diff<Map<String, Object>> customAttributesDiff =
        new Diff<>(null, new HashMap<String, Object>());
    Diff<Object> dataDiff = new Diff<>(1, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

    assertThat(mChangeSet.getChangeCount()).isEqualTo(0);
  }

  @Test
  public void testDeleteComponent() {
    Diff<Component> componentDiff = new Diff<>(mock(Component.class), null);
    Diff<Boolean> stickyDiff = new Diff<>(null, null);
    Diff<Integer> spanSizeDiff = new Diff<>(null, null);
    Diff<Boolean> isFullSpanDiff = new Diff<>(null, null);
    Diff<Map<String, Object>> customAttributesDiff = new Diff<>(null, null);
    Diff<Object> dataDiff = new Diff<>(1, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.DELETE);
    assertThat(change.getPrevData()).isEqualTo(ImmutableList.of(1));
    assertThat(change.getNextData()).isNull();
  }

  @Test
  public void testInsertComponent() {
    Component component = mock(Component.class);
    Diff<Component> componentDiff = new Diff<>(null, component);
    Diff<Boolean> stickyDiff = new Diff<>(null, true);
    Diff<Integer> spanSizeDiff = new Diff<>(null, 2);
    Diff<Boolean> isFullSpanDiff = new Diff<>(null, true);
    Diff<Map<String, Object>> customAttributesDiff = new Diff<>(null, null);
    Diff<Object> dataDiff = new Diff<>(null, 1);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.INSERT);
    assertThat(change.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(change.getRenderInfo().isSticky()).isTrue();
    assertThat(change.getRenderInfo().getSpanSize()).isEqualTo(2);
    assertThat(change.getRenderInfo().isFullSpan()).isTrue();
    assertThat(change.getPrevData()).isNull();
    assertThat(change.getNextData()).isEqualTo(ImmutableList.of(1));
  }

  @Test
  public void testUpdateComponent() {
    Component nextComponent = mock(Component.class);
    Component prevComponent = mock(Component.class);
    when(prevComponent.isEquivalentTo(nextComponent)).thenReturn(false);

    Diff<Component> componentDiff = new Diff<>(prevComponent, nextComponent);
    Diff<Boolean> stickyDiff = new Diff<>(true, true);
    Diff<Integer> spanSizeDiff = new Diff<>(2, 2);
    Diff<Boolean> isFullSpanDiff = new Diff<>(true, true);
    Diff<Map<String, Object>> customAttributesDiff = new Diff<>(null, null);
    Diff<Object> dataDiff = new Diff<>(1, 2);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.UPDATE);
    assertThat(change.getRenderInfo().getComponent()).isEqualTo(nextComponent);
    assertThat(change.getRenderInfo().isSticky()).isTrue();
    assertThat(change.getRenderInfo().getSpanSize()).isEqualTo(2);
    assertThat(change.getRenderInfo().isFullSpan()).isTrue();
    assertThat(change.getPrevData()).isEqualTo(ImmutableList.of(1));
    assertThat(change.getNextData()).isEqualTo(ImmutableList.of(2));
  }

  @Test
  public void testUpdateSpanSize() {
    Component component = mock(Component.class);
    Diff<Component> componentDiff = new Diff<>(component, component);
    Diff<Boolean> stickyDiff = new Diff<>(true, true);
    Diff<Integer> spanSizeDiff = new Diff<>(1, 2);
    Diff<Boolean> isFullSpanDiff = new Diff<>(true, true);
    Diff<Map<String, Object>> customAttributesDiff = new Diff<>(null, null);
    Diff<Object> dataDiff = new Diff<>(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

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
    Diff<Component> componentDiff = new Diff<>(component, component);
    Diff<Boolean> stickyDiff = new Diff<>(true, false);
    Diff<Integer> spanSizeDiff = new Diff<>(1, 1);
    Diff<Boolean> isFullSpanDiff = new Diff<>(true, true);
    Diff<Map<String, Object>> customAttributesDiff = new Diff<>(null, null);
    Diff<Object> dataDiff = new Diff<>(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

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
    Diff<Component> componentDiff = new Diff<>(component, component);
    Diff<Boolean> stickyDiff = new Diff<>(true, true);
    Diff<Integer> spanSizeDiff = new Diff<>(1, 1);
    Diff<Boolean> isFullSpanDiff = new Diff<>(true, false);
    Diff<Map<String, Object>> customAttributesDiff = new Diff<>(null, null);
    Diff<Object> dataDiff = new Diff<>(null, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

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

    Diff<Component> componentDiff = new Diff<>(prevComponent, nextComponent);
    Diff<Boolean> stickyDiff = new Diff<>(true, true);
    Diff<Integer> spanSizeDiff = new Diff<>(1, 1);
    Diff<Boolean> isFullSpanDiff = new Diff<>(true, true);
    Diff<Map<String, Object>> customAttributesDiff = new Diff<>(null, null);
    Diff<Object> dataDiff = new Diff<>(1, 1);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

    assertThat(mChangeSet.getChangeCount()).isEqualTo(0);
  }

  @Test
  public void testUpdateCustomAttributes() {
    Component component = mock(Component.class);
    when(component.isEquivalentTo(any())).thenReturn(true);
    Diff<Component> componentDiff = new Diff<>(component, component);
    Diff<Boolean> stickyDiff = new Diff<>(true, true);
    Diff<Integer> spanSizeDiff = new Diff<>(1, 1);
    Diff<Boolean> isFullSpanDiff = new Diff<>(true, false);
    final HashMap<String, Object> attrs = new HashMap<>();
    attrs.put("test", true);
    Diff<Map<String, Object>> customAttributesDiff = new Diff<>(null, attrs);
    Diff<Object> dataDiff = new Diff<>(null, null);

    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);

    Change change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(change.getType()).isEqualTo(Change.UPDATE);
    assertThat(change.getRenderInfo().getComponent()).isEqualTo(component);
    assertThat(change.getRenderInfo().isSticky()).isTrue();
    assertThat(change.getRenderInfo().getSpanSize()).isEqualTo(1);
    assertThat(change.getRenderInfo().isFullSpan()).isFalse();
    assertThat((Boolean) change.getRenderInfo().getCustomAttribute("test")).isTrue();

    mChangeSet = ChangeSet.acquireChangeSet(null, false);
    customAttributesDiff = new Diff<>(attrs, attrs);
    isFullSpanDiff = new Diff<>(false, false);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);
    assertThat(mChangeSet.getChangeCount()).isEqualTo(0);
    assertThat((Boolean) change.getRenderInfo().getCustomAttribute("test")).isTrue();

    mChangeSet = ChangeSet.acquireChangeSet(null, false);
    customAttributesDiff = new Diff<>(attrs, null);
    SingleComponentSectionSpec.onCreateChangeSet(
        mSectionContext,
        mChangeSet,
        componentDiff,
        stickyDiff,
        spanSizeDiff,
        isFullSpanDiff,
        customAttributesDiff,
        dataDiff);
    change = verifyChangeSetAndGetTheChange(mChangeSet);
    assertThat(mChangeSet.getChangeCount()).isEqualTo(1);
    assertThat(change.getRenderInfo().getCustomAttribute("test")).isNull();
  }

  private static Change verifyChangeSetAndGetTheChange(ChangeSet changeSet) {
    assertThat(changeSet.getChangeCount()).isEqualTo(1);
    return changeSet.getChangeAt(changeSet.getChangeCount() - 1);
  }
}
