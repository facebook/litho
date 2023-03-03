/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.sections;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.util.Pair;
import com.facebook.litho.EventHandler;
import com.facebook.litho.EventHandlersController;
import com.facebook.litho.testing.sections.TestTarget;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class SectionTreeEventHandlerTest {

  private static final String SECTION1_GLOBAL_KEY = "section1";
  private static final String SECTION2_GLOBAL_KEY = "section2";

  private final Section mSection = mock(Section.class);
  private final Section mSection2 = mock(Section.class);
  private SectionTree mSectionTree;
  private SectionContext mContext;
  private SectionTree.Target mTestTarget;

  @Before
  public void setup() {
    when(mSection.getGlobalKey()).thenReturn(SECTION1_GLOBAL_KEY);
    when(mSection2.getGlobalKey()).thenReturn(SECTION2_GLOBAL_KEY);
    mTestTarget = new TestTarget();
    mSectionTree =
        SectionTree.create(new SectionContext(getApplicationContext()), mTestTarget).build();
    mContext = mSectionTree.getContext();
  }

  @Test
  public void testNoDuplicateWhenEventHandlerIsReplacedInEventHandlerWrapper() {
    EventHandlersController eventHandlersController = mSectionTree.getEventHandlersController();

    SectionContext scopedContext = SectionContext.withScope(mContext, mSection);

    EventHandler eventHandler1 =
        Section.newEventHandler(mSection.getClass(), "TestSection", scopedContext, 1, null);

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(0);

    EventHandler eventHandler2 =
        Section.newEventHandler(mSection.getClass(), "TestSection", scopedContext, 1, null);

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(0);
    assertThat(eventHandler1.dispatchInfo).isNotSameAs(eventHandler2.dispatchInfo);

    final ArrayList<Pair<String, EventHandler<?>>> eventHandlers = new ArrayList<>();
    eventHandlers.add(new Pair<>(SECTION1_GLOBAL_KEY, eventHandler1));
    eventHandlers.add(new Pair<>(SECTION1_GLOBAL_KEY, eventHandler2));

    eventHandlersController.canonicalizeEventDispatchInfos(eventHandlers);

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(1);
    assertThat(eventHandler1.dispatchInfo).isSameAs(eventHandler2.dispatchInfo);

    Section newSection = mock(Section.class);
    when(newSection.getGlobalKey()).thenReturn(SECTION1_GLOBAL_KEY);
    SectionContext newScopedContext = SectionContext.withScope(mContext, newSection);

    eventHandlersController.updateEventDispatchInfoForGlobalKey(
        newScopedContext, newSection, mSection.getGlobalKey());
    eventHandlersController.clearUnusedEventDispatchInfos();

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(1);
    assertThat(eventHandler1.dispatchInfo.componentContext).isSameAs(newScopedContext);
    assertThat(eventHandler2.dispatchInfo.componentContext).isSameAs(newScopedContext);
    assertThat(eventHandler1.dispatchInfo.hasEventDispatcher).isSameAs(newSection);
    assertThat(eventHandler2.dispatchInfo.hasEventDispatcher).isSameAs(newSection);
  }

  @Test
  public void testClearUnusedEntries() {
    Section section = mock(Section.class);
    Section section2 = mock(Section.class);
    Section section2_2 = mock(Section.class);
    when(section.getGlobalKey()).thenReturn(SECTION1_GLOBAL_KEY);
    when(section2.getGlobalKey()).thenReturn(SECTION2_GLOBAL_KEY);
    when(section2_2.getGlobalKey()).thenReturn(SECTION2_GLOBAL_KEY);
    SectionContext scopedContext = SectionContext.withScope(mContext, section);
    SectionContext scopedContext2 = SectionContext.withScope(mContext, section2);
    SectionContext scopedContext2_2 = SectionContext.withScope(mContext, section2_2);

    EventHandlersController eventHandlersController = mSectionTree.getEventHandlersController();

    EventHandler eventHandler1 =
        Section.newEventHandler(mSection.getClass(), "TestSection", scopedContext, 1, null);
    EventHandler eventHandler2 =
        Section.newEventHandler(mSection.getClass(), "TestSection", scopedContext2, 1, null);

    final ArrayList<Pair<String, EventHandler<?>>> eventHandlers = new ArrayList<>();
    eventHandlers.add(new Pair<>(SECTION1_GLOBAL_KEY, eventHandler1));
    eventHandlers.add(new Pair<>(SECTION2_GLOBAL_KEY, eventHandler2));

    eventHandlersController.canonicalizeEventDispatchInfos(eventHandlers);

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(2);
    assertThat(eventHandler1.dispatchInfo.componentContext).isSameAs(scopedContext);
    assertThat(eventHandler2.dispatchInfo.componentContext).isSameAs(scopedContext2);
    assertThat(eventHandler1.dispatchInfo.hasEventDispatcher).isSameAs(section);
    assertThat(eventHandler2.dispatchInfo.hasEventDispatcher).isSameAs(section2);

    eventHandlersController.updateEventDispatchInfoForGlobalKey(
        scopedContext2_2, section2_2, SECTION2_GLOBAL_KEY);
    eventHandlersController.clearUnusedEventDispatchInfos();

    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(1);
    assertThat(eventHandlersController.getDispatchInfos().get(SECTION2_GLOBAL_KEY)).isNotNull();

    eventHandlersController.clearUnusedEventDispatchInfos();
    assertThat(eventHandlersController.getDispatchInfos().size()).isEqualTo(0);

    assertThat(eventHandler1.dispatchInfo.componentContext).isSameAs(scopedContext);
    assertThat(eventHandler2.dispatchInfo.componentContext).isSameAs(scopedContext2_2);
    assertThat(eventHandler1.dispatchInfo.hasEventDispatcher).isSameAs(section);
    assertThat(eventHandler2.dispatchInfo.hasEventDispatcher).isSameAs(section2_2);
  }
}
