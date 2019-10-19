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

package com.facebook.litho.testing.sections;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import com.facebook.litho.EventHandler;
import com.facebook.litho.HasEventDispatcher;
import com.facebook.litho.StateContainer;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.Section;
import com.facebook.litho.sections.SectionContext;
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
 *
 * <p>Allows testing a {@code GroupsSectionSpec}'s direct children without building the whole
 * hieararchy. Also allows simpler State updates testing by keeping trach of a scoped section
 * context.
 */
public class SectionsTestHelper extends Section {

  // Keeping a reference of the SectionTree here otherwise in some tests it gets cleared.
  private final SectionTree mSectionTree;
  private final SectionContext mSectionContext;

  // A set of sections that are ready to be tested (createInitialState called).
  private final Map<Section, SectionContext> preparedSections;

  public SectionsTestHelper(Context c) {
    this(new SectionContext(c));
  }

  public SectionsTestHelper(SectionContext base) {
    super("SectionComponentTestHelper");
    mSectionTree = SectionTree.create(base, new TestTarget()).build();
    mSectionContext = SectionContext.withSectionTree(base, mSectionTree);
    preparedSections = new HashMap<>();
  }

  @Override
  public boolean isEquivalentTo(Section other) {
    return this.equals(other);
  }

  /** Return a generic {@link SectionContext} you can use to create sections. */
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
   * Prepare section for testing.
   *
   * <p>Prepare sets up a proper scoped {@link SectionContext} we can use to test lifecycle methods
   * and state updates. It also prepares the given section for use by calling lifecycle methods like
   * {@code onCreateInitialState}.
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
                final StateContainer.StateUpdate stateUpdate =
                    (StateContainer.StateUpdate) invocation.getArguments()[0];
                SectionLifecycleTestUtil.getStateContainer(scope).applyStateUpdate(stateUpdate);
                return null;
              }
            })
        .when(spyContext)
        .updateStateSync(any(StateContainer.StateUpdate.class), any(String.class));

    doAnswer(
            new Answer() {
              @Override
              @Nullable
              public Object answer(InvocationOnMock invocation) throws Throwable {
                final Section scope = ((SectionContext) invocation.getMock()).getSectionScope();
                final StateContainer.StateUpdate stateUpdate =
                    (StateContainer.StateUpdate) invocation.getArguments()[0];
                SectionLifecycleTestUtil.getStateContainer(scope).applyStateUpdate(stateUpdate);
                return null;
              }
            })
        .when(spyContext)
        .updateStateLazy(any(StateContainer.StateUpdate.class));

    doReturn(mSectionContext.getResourceCache()).when(spyContext).getResourceCache();

    SectionLifecycleTestUtil.setScopedContext(s, spyContext);
    SectionLifecycleTestUtil.createInitialState(s, spyContext, s);
    s.setGlobalKey("globalKey");
    preparedSections.put(s, spyContext);
    return s;
  }

  /**
   * Get child sections for the given section.
   *
   * @param section The section under test.
   * @return A list of the children created by the given section.
   */
  @Nullable
  public List<SubSection> getChildren(Section section) {
    ensurePrepared(section);
    final Children children =
        SectionLifecycleTestUtil.createChildren(
            section, SectionContext.withScope(getScopedContext(section), section), section);
    return getSubSections(children);
  }

  /**
   * Get sub sections for the given Children. This is very similar to {@link #getChildren(Section)}
   * except it gets the Children intead of calling createChildren. This is useful for testing the
   * output of {@link com.facebook.litho.sections.common.RenderSectionEvent} handlers.
   *
   * @param children The Children object to extract sections from.
   * @return A list of the sections inside the given Children object.
   */
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

  /**
   * Get the state container of the given section.
   *
   * @param section The section for which you want the state values from.
   * @param <T> The section's StateContainer class
   * @return the state container.
   */
  @SuppressWarnings("unchecked")
  public <T extends StateContainer> T getStateContainer(Section section) {
    return (T)
        SectionLifecycleTestUtil.getStateContainer(getScopedContext(section).getSectionScope());
  }

  /**
   * Dispatches an event to the section
   *
   * @param section the section under test
   * @param eventHandler the event handler to execute
   * @param event the event object
   * @return the event's return value if the event expects to return anything. Otherwise null.
   */
  public static Object dispatchEvent(
      HasEventDispatcher section, EventHandler eventHandler, Object event) {
    return section.getEventDispatcher().dispatchOnEvent(eventHandler, event);
  }

  private void ensurePrepared(Section s) {
    if (!preparedSections.containsKey(s)) {
      throw new IllegalStateException(
          "Section not prepared, did you call SectionsTestHelper#prepare()?");
    }
  }
}
