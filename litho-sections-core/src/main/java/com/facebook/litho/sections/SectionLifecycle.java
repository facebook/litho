/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import com.facebook.litho.ComponentsPools;
import com.facebook.litho.Diff;
import com.facebook.litho.EventDispatcher;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Output;
import com.facebook.litho.TreeProps;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.sections.annotations.OnDiff;

public abstract class SectionLifecycle implements EventDispatcher {

  protected interface StateContainer {}

  /**
   * This methods will delegate to the {@link GroupSectionSpec}
   * method annotated with {@link com.facebook.litho.sections.annotations.OnCreateChildren}
   */
  protected Children createChildren(
      SectionContext c,
      Section component) {
    return null;
  }

  /**
   * This method will delegate to the {@link DiffSectionSpec}
   * method annotated with {@link OnDiff}
   */
  protected void generateChangeSet(
      SectionContext c,
      ChangeSet changeSet,
      Section previous,
      Section next) {
  }

  /**
   * This method will delegate to the {@link Section}Spec method annotated with {@link OnDataBound}
   */
  protected void dataBound(SectionContext c, Section section) {

  }

  protected void bindService(SectionContext c, Section section) {

  }

  protected void unbindService(SectionContext c, Section section) {

  }

  protected void createInitialState(SectionContext c, Section section) {

  }

  protected void createService(SectionContext c, Section section) {

  }

  public Object dispatchOnEvent(EventHandler eventHandler, Object eventState) {
    // Do nothing by default.
    return null;
  }

  protected void viewportChanged(
      SectionContext sectionContext,
      int firstVisibleItem,
      int lastVisibleItem,
      int totalItemsCount,
      int firstFullyVisibleItem,
      int lastFullyVisibleItem,
      Section section) {

  }

  protected void refresh(
      SectionContext sectionContext,
      Section section) {

  }

  /**
   * Call this to transfer the {@link com.facebook.litho.annotations.State} annotated values
   * between two {@link Section} with the same global scope.
   */
  protected void transferState(
      SectionContext c,
      StateContainer stateContainer,
      Section section) {
  }

  /**
   * Call this to transfer the Services between two
   * {@link Section} with the same global scope.
   */
  protected void transferService(
      SectionContext c,
      Section oldSection,
      Section newSection) {
  }

  protected Object getService(Section section) {
    return null;
  }

  /**
   * @return Whether it's necessary to generate a new changeSet for the sub-tree with root in next
   * @param previous the {@link Section} with the same globalScope in the previous tree.
   * @param next the {@link Section} for which we want to check whether a new changeSet is needed.
   */
  final boolean shouldComponentUpdate(Section previous, Section next) {
    boolean dirty = false;

    // If next is invalidated it means a state update was applied to it so we need to create
    // the ChangeSet regardless.
    if (!dirty && next != null) {
      dirty |= next.isInvalidated();
    }

    return dirty || shouldUpdate(previous, next);
  }

  protected final <T> Diff<T> acquireDiff(T previousValue, T nextValue) {
    Diff diff = ComponentsPools.acquireDiff(previousValue, nextValue);

    return diff;
  }

  protected void releaseDiff(Diff diff) {
    ComponentsPools.release(diff);
  }

  protected Output acquireOutput() {
    //TODO 11953296
    return new Output();
  }

  protected void releaseOutput(Output output) {
    //TODO 11953296
  }

  protected boolean shouldUpdate(Section previous, Section next) {
    return !(previous == next || (previous != null && previous.isEquivalentTo(next)));
  }

  /**
   * @return true if this lifecycle will generate a changeSet. If false this lifecycle will instead
   * implement createChildren.
   */
  protected boolean isDiffSectionSpec() {
    return false;
  }


  String getLogTag() {
    return getClass().getSimpleName();
  }

  protected static <E> EventHandler<E> newEventHandler(
      SectionContext c,
      String name,
      int id,
      Object[] params) {
    final EventHandler eventHandler = c.newEventHandler(name, id, params);
    recordEventHandler(c.getSectionScope(), eventHandler);

    return eventHandler;
  }

  protected static <E> EventHandler<E> newEventHandler(
      Section<?> c,
      String name,
      int id,
      Object[] params) {
    final EventHandler eventHandler = new EventHandler<E>(c, name, id, params);
    recordEventHandler(c, eventHandler);

    return eventHandler;
  }

  private static void recordEventHandler(Section section, EventHandler eventHandler) {
    section.getScopedContext().getSectionTree().recordEventHandler(section, eventHandler);
  }

  protected interface StateUpdate {
    void updateState(StateContainer stateContainer, Section section);
  }

  /**
   * Retrieves all of the tree props used by this Section from the TreeProps map
   * and sets the tree props as fields on the ComponentImpl.
   */
  protected void populateTreeProps(Section section, TreeProps parentTreeProps) {
  }

  /**
   * Updates the TreeProps map with outputs from all {@link OnCreateTreeProp} methods.
   */
  protected TreeProps getTreePropsForChildren(
      SectionContext c,
      Section section,
      TreeProps previousTreeProps) {
    return previousTreeProps;
  }

  public static EventHandler getLoadingEventHandler(SectionContext context) {
    Section scopedSection = context.getSectionScope();
    if (scopedSection == null) {
      return null;
    }

    while (scopedSection.getParent() != null) {
      if (scopedSection.loadingEventHandler != null) {
        return scopedSection.loadingEventHandler;
      }
      scopedSection = scopedSection.getParent();
    }

    return context.getTreeLoadingEventHandler();
  }

  public static void dispatchLoadingEvent(
      SectionContext context,
      boolean isEmpty,
      LoadingEvent.LoadingState loadingState,
      Throwable t) {
    final EventHandler<LoadingEvent> loadingEventHandler = getLoadingEventHandler(context);

    if (loadingEventHandler != null) {
      LoadingEvent loadingEvent = new LoadingEvent();
      loadingEvent.isEmpty = isEmpty;
      loadingEvent.loadingState = loadingState;
      loadingEvent.t = t;
      loadingEventHandler.dispatchEvent(loadingEvent);
    }
  }

  public static void requestFocus(SectionContext c, int index) {
    final Section scopedSection = c.getSectionScope();
    final SectionTree sectionTree = c.getSectionTree();

    if (scopedSection == null || sectionTree == null) {
      return;
    }

    sectionTree.requestFocus(scopedSection, index);
  }

  /**
   * Scroll to the index in the section with an additional offset. One notable difference between
   * this and the {@link #requestFocus(SectionContext, int)} API is that this will *always* take
   * an action, while {@link #requestFocus(SectionContext, int)} will ignore the command if the
   * item is visible on the screen.
   * @param offset distance, in pixels, from the top of the screen to scroll the requested item
   */
  public static void requestFocusWithOffset(SectionContext c, int index, int offset) {
    final Section scopedSection = c.getSectionScope();
    final SectionTree sectionTree = c.getSectionTree();

    if (scopedSection == null || sectionTree == null) {
      return;
    }

    sectionTree.requestFocusWithOffset(scopedSection, index, offset);
  }

  /**
   * Scroll to the beginning of the section with the given key.
   */
  public static void requestFocus(SectionContext c, String sectionKey) {
    requestFocus(c, sectionKey, FocusType.START);
  }

  /**
   * Scroll to the index in the section with an additional offset. One notable difference between
   * this and the {@link #requestFocus(SectionContext, String)} API is that this will *always* take
   * an action, while {@link #requestFocus(SectionContext, String)} will ignore the command if the
   * item is visible on the screen.
   * @param offset distance, in pixels, from the top of the screen to scroll the requested item
   */
  public static void requestFocusWithOffset(SectionContext c, String sectionKey, int offset) {
    final Section scopedSection = c.getSectionScope();
    final SectionTree sectionTree = c.getSectionTree();

    if (scopedSection == null || sectionTree == null) {
      return;
    }

    final String globalKey = scopedSection.getGlobalKey() + sectionKey;

    sectionTree.requestFocusWithOffset(globalKey, offset);
  }

  public static void requestFocus(SectionContext c, String sectionKey, FocusType focusType) {
    final Section scopedSection = c.getSectionScope();
    final SectionTree sectionTree = c.getSectionTree();

    if (scopedSection == null || sectionTree == null) {
      return;
    }

    final String globalKey = scopedSection.getGlobalKey() + sectionKey;

    switch (focusType) {
      case START:
        sectionTree.requestFocusStart(globalKey);
        break;
      case END:
        sectionTree.requestFocusEnd(globalKey);
        break;
    }
  }
}
