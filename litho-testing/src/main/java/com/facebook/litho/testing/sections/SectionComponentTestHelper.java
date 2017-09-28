/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.testing.sections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.support.annotation.Nullable;
import com.facebook.litho.EventHandler;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionLifecycle;
import com.facebook.litho.sections.SectionLifecycleTestUtil;
import com.facebook.litho.sections.SectionTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * A helper for testing section components.
 */
public class SectionComponentTestHelper extends SectionLifecycle {

  private final SectionContext sc;
  private final SectionTree st;

  // A set of sections that are ready to be tested (createInitialState called).
  private final HashMap<Section, SectionContext> preparedSections;

  public SectionComponentTestHelper(Context c) {
    SectionContext base = new SectionContext(c);
    st = SectionTree.create(base, new TestTarget()).build();
    sc = SectionContext.withSectionTree(base, st);
    preparedSections = new HashMap<>();
  }

  /**
   * Return a generic {@link SectionContext} you can use to create sections/components.
   */
  public SectionContext getContext() {
    return sc;
  }

  /**
   * Return the specific {@link SectionContext} that's been scoped to the given section.
   */
  public SectionContext getScopedContext(Section s) {
    ensurePrepared(s);
    return preparedSections.get(s);
  }

  /**
   * prepare section for testing.  If you've prepared a section before this will reset it to
   * it's initial state.
   */
  public Section prepare(Section s) {
    SectionContext context = SectionContext.withScope(sc, s);
    SectionContext spyContext = spy(context);

    doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Section scope = ((SectionContext) invocation.getMock()).getSectionScope();
                StateUpdate stateUpdate = (StateUpdate) invocation.getArguments()[0];
                stateUpdate.updateState(SectionLifecycleTestUtil.getStateContainer(scope), scope);
                return null;
              }
            })
        .when(spyContext)
        .updateState(any(StateUpdate.class));

    doReturn(sc.getResourceCache()).when(spyContext).getResourceCache();

    SectionLifecycleTestUtil.setScopedContext(s, spyContext);
    SectionLifecycleTestUtil.createInitialState(s.getLifecycle(), spyContext, s);
    s.setGlobalKey("globalKey");
    preparedSections.put(s, spyContext);
    return s;
  }

  /**
   * Get child sections for the given section.
   */
  public List<SubSection> getChildren(Section section) {
    ensurePrepared(section);
    final Children children =
        SectionLifecycleTestUtil.createChildren(
            section.getLifecycle(),
            SectionContext.withScope(getScopedContext(section), section),
            section);
    return getSubSections(children);
  }

  /** Get subSections for the given Children. */
  public @Nullable List<SubSection> getSubSections(Children children) {
    if (children == null || children.getChildren() == null) {
      return null;
    }

    ArrayList<SubSection> subsection = new ArrayList<>(children.getChildren().size());

    for (Section s: children.getChildren()) {
      subsection.add(SubSection.of(s));
    }
    return subsection;
  }

  /**
   * Return the state container of the given section.
   */
  public <T extends StateContainer> T getStateContainer(Section<?> section) {
    return (T)
        SectionLifecycleTestUtil.getStateContainer(getScopedContext(section).getSectionScope());
  }

  /**
   * Dispatch the given event to a event handler.
   */
  public Object dispatchEvent(Section section, EventHandler eventHandler, Object event) {
    return section.getEventDispatcher().dispatchOnEvent(eventHandler, event);
  }

  private void ensurePrepared(Section s) {
    if (!preparedSections.containsKey(s)) {
      throw new IllegalStateException(
          "Section not prepared, did you call SectionComponentTestHelper#prepare()?");
    }
  }
}
