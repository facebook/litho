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
import com.facebook.litho.EventHandler;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.SectionLifecycle;
import com.facebook.litho.sections.SectionLifecycleTestUtil;
import com.facebook.litho.sections.SectionTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * A helper for testing section components.
 */
public class SectionComponentTestHelper extends SectionLifecycle {

  private final SectionContext mSectionContext;

  // A set of sections that are ready to be tested (createInitialState called).
  private final Map<Section, SectionContext> preparedSections;

  public SectionComponentTestHelper(Context c) {
    final SectionContext base = new SectionContext(c);
    final SectionTree st = SectionTree.create(base, new TestTarget()).build();
    mSectionContext = SectionContext.withSectionTree(base, st);
    preparedSections = new HashMap<>();
  }

  /**
   * Return a generic {@link SectionContext} you can use to create sections/components.
   */
  public SectionContext getContext() {
    return mSectionContext;
  }

  /** Return the specific {@link SectionContext} that's been scoped to the given section. */
  @Nullable
  public SectionContext getScopedContext(Section s) {
    ensurePrepared(s);
    return preparedSections.get(s);
  }

  /**
   * prepare section for testing.  If you've prepared a section before this will reset it to
   * it's initial state.
   */
  public Section prepare(Section s) {
    final SectionContext context = SectionContext.withScope(mSectionContext, s);
    final SectionContext spyContext = spy(context);

    doAnswer(
            new Answer() {
              @Override
              @Nullable
              public Object answer(InvocationOnMock invocation) throws Throwable {
                final Section scope = ((SectionContext) invocation.getMock()).getSectionScope();
                final StateUpdate stateUpdate = (StateUpdate) invocation.getArguments()[0];
                stateUpdate.updateState(SectionLifecycleTestUtil.getStateContainer(scope), scope);
                return null;
              }
            })
        .when(spyContext)
        .updateState(any(StateUpdate.class));

    doReturn(mSectionContext.getResourceCache()).when(spyContext).getResourceCache();

    SectionLifecycleTestUtil.setScopedContext(s, spyContext);
    SectionLifecycleTestUtil.createInitialState(s.getLifecycle(), spyContext, s);
    s.setGlobalKey("globalKey");
    preparedSections.put(s, spyContext);
    return s;
  }

  /** Get child sections for the given section. */
  @Nullable
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
  @Nullable
  public static List<SubSection> getSubSections(Children children) {
    if (children == null || children.getChildren() == null) {
      return null;
    }

    final List<SubSection> subsection = new ArrayList<>(children.getChildren().size());

    for (final Section s : children.getChildren()) {
      subsection.add(SubSection.of(s));
    }
    return subsection;
  }

  /** Return the state container of the given section. */
  @SuppressWarnings("unchecked")
  public <T extends StateContainer> T getStateContainer(Section<?> section) {
    return (T)
        SectionLifecycleTestUtil.getStateContainer(getScopedContext(section).getSectionScope());
  }

  /** Dispatch the given event to a event handler. */
  public static Object dispatchEvent(
      HasEventDispatcher section, EventHandler eventHandler, Object event) {
    return section.getEventDispatcher().dispatchOnEvent(eventHandler, event);
  }

  private void ensurePrepared(Section s) {
    if (!preparedSections.containsKey(s)) {
      throw new IllegalStateException(
          "Section not prepared, did you call SectionComponentTestHelper#prepare()?");
    }
  }
}
