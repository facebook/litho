/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.litho.ThreadUtils.isMainThread;
import static com.facebook.litho.sections.SectionLifecycle.StateUpdate;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentsPools;
import com.facebook.litho.EventHandler;
import com.facebook.litho.TreeProps;
import com.facebook.litho.sections.config.SectionComponentsConfiguration;
import com.facebook.litho.sections.logger.SectionComponentLogger;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.ViewportInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Represents a tree of {@link Section} and manages their lifecycle. {@link SectionTree} takes
 * a root {@link Section} and generates the complete tree by recursively invoking
 * OnCreateChildren.
 * {@link SectionTree} is also responsible for regenerating the tree in response to state update
 * events.
 */
public class SectionTree {

  private static final int OUT_OF_RANGE = -1;

  private static class Range {

    private int firstVisibleIndex;
    private int lastVisibleIndex;
    private int firstFullyVisibleIndex;
    private int lastFullyVisibleIndex;
    private int totalItemsCount;
  }

  private static final String DEFAULT_CHANGESET_THREAD_NAME = "ComponentChangeSetThread";
  private static final int DEFAULT_CHANGESET_THREAD_PRIORITY = Process.THREAD_PRIORITY_BACKGROUND;
  private final SectionComponentLogger mSectionComponentLogger;
  private volatile boolean mReleased;

  /**
   * The class implementing this interface will be responsible to translate the ChangeSet into
   * UI updates.
   */
  public interface Target {

    /**
     * Notify that a {@link Component} was added at index.
     */
    void insert(int index, RenderInfo renderInfo);

    void insertRange(int index, int count, List<RenderInfo> renderInfos);

    /**
     * Notify that a {@link Component} was updated at index.
     */
    void update(int index, RenderInfo renderInfo);

    void updateRange(int index, int count, List<RenderInfo> renderInfos);

    /**
     * Notify that the {@link Component} at index was deleted.
     */
    void delete(int index);

    void deleteRange(int index, int count);

    /**
     * Notify that a {@link Component} was moved fromPosition toPosition.
     */
    void move(int fromPosition, int toPosition);

    /**
     * Request focus on the item with the given index.
     */
    void requestFocus(int index);

    /**
     * Request focus on the item with the given index, plus some additional offset.
     */
    void requestFocusWithOffset(int index, int offset);
  }

  private static final int MESSAGE_WHAT_BACKGROUND_CHANGESET_STATE_UPDATED = 1;
  private static final Handler sMainThreadHandler = new SectionsComponentMainThreadHandler();

  @GuardedBy("ComponentTree.class")
  private static volatile Looper sDefaultChangeSetThreadLooper;

  private final SectionContext mContext;
  private final BatchedTarget mTarget;
  private final boolean mAsyncStateUpdates;
  private final boolean mAsyncPropUpdates;
  private final String mTag;
  private final Map<String, Range> mLastRanges = new HashMap<>();
  // Holds a Pair where the first item is a section's global starting index
  // and the second is the count.
  private Map<String, Pair<Integer, Integer>> mSectionPositionInfo;
  private LoadEventsHandler mLoadEventsHandler;

  private final CalculateChangeSetRunnable mCalculateChangeSetOnMainThreadRunnable;
  private final CalculateChangeSetRunnable mCalculateChangeSetRunnable;

  private class CalculateChangeSetRunnable implements Runnable {

    private final Handler mHandler;

    @GuardedBy("this")
    private boolean mIsPosted;

    public CalculateChangeSetRunnable(Handler handler) {
      mHandler = handler;
    }

    public synchronized void ensurePosted() {
      if (!mIsPosted) {
        mIsPosted = true;
        mHandler.post(this);
      }
    }

    public synchronized void cancel() {
      if (mIsPosted) {
        mIsPosted = false;
        mHandler.removeCallbacks(this);
      }
    }

    @Override
    public void run() {
      synchronized (this) {
        if (!mIsPosted) {
          return;
        }
        mIsPosted = false;
      }
      applyNewChangeSet();
    }
  }

  @GuardedBy("this")
  private @Nullable Section<?> mCurrentSection;

  @GuardedBy("this")
  private @Nullable Section<?> mNextSection;

  @GuardedBy("this")
  private Map<String, List<StateUpdate>> mPendingStateUpdates;

  @GuardedBy("this")
  private List<ChangeSet> mPendingChangeSets;

  private boolean mHasNonLazyUpdate;

  @GuardedBy("this")
  private Map<String, List<EventHandler>> mEventHandlers = new HashMap<>();

  private synchronized void bindEventHandlers(Section section) {
    if (!mEventHandlers.containsKey(section.getGlobalKey())) {
      return;
    }

    for (EventHandler eventHandler : mEventHandlers.get(section.getGlobalKey())) {
      eventHandler.mHasEventDispatcher = section;

      // Params should only be null for tests
      if (eventHandler.params != null) {
        eventHandler.params[0] = section.getScopedContext();
      }
    }
  }

  synchronized void recordEventHandler(Section section, EventHandler eventHandler) {
    final String key = section.getGlobalKey();

    if (!mEventHandlers.containsKey(key)) {
      List<EventHandler> list = new ArrayList<>();
      mEventHandlers.put(key, list);
    }

    mEventHandlers.get(key).add(eventHandler);
  }

  private SectionTree(Builder builder) {
    mSectionComponentLogger = new Logger(SectionComponentsConfiguration.LOGGERS);
    mReleased = false;
    mAsyncStateUpdates = builder.mAsyncStateUpdates;
    mAsyncPropUpdates = builder.mAsyngPropUpdates;
    mTag = builder.mTag;
    mTarget = new BatchedTarget(builder.mTarget, mSectionComponentLogger, mTag);
    mContext = SectionContext.withSectionTree(builder.mContext, this);
    mPendingChangeSets = new ArrayList<>();
    mPendingStateUpdates = new HashMap<>();
    mHasNonLazyUpdate = false;
    Handler changeSetThreadHandler = builder.mChangeSetThreadHandler != null ?
        builder.mChangeSetThreadHandler :
        new Handler(getDefaultChangeSetThreadLooper());
    mCalculateChangeSetRunnable = new CalculateChangeSetRunnable(changeSetThreadHandler);
    mCalculateChangeSetOnMainThreadRunnable = new CalculateChangeSetRunnable(sMainThreadHandler);
  }

  /**
   * Create a {@link Builder} that can be used to configure a {@link SectionTree}.
   *
   * @param context The {@link SectionContext} taht will be used to create the child
   * {@link com.facebook.litho.Component}s
   * @param target The {@link Target} that will be responsible to apply the
   * {@link ChangeSet} to the UI.
   */
  public static Builder create(SectionContext context, Target target) {
    //TODO use pools t11953296
    return new Builder(context, target);
  }

  /**
   * Update the root ListComponent. This will create the new ListComponent tree and generate a
   * {@link ChangeSet} to be applied to the UI. In response to this
   * {@link Target#applyNewChangeSet()} will be invoked once the {@link ChangeSet} has
   * been calculated.
   * The generation of the ChangeSet will happen synchronously in the thread calling this method.
   *
   * @param section The new root.
   */
  public void setRoot(Section section) {
    boolean isFirstSetRoot;
    synchronized (this) {
      if (mReleased) {
        throw new IllegalStateException("Setting root on a released tree");
      }

      if (mCurrentSection != null && mCurrentSection.getId() == section.getId()) {
        return;
      }

      if (mNextSection != null && mNextSection.getId() == section.getId()) {
        return;
      }

      mNextSection = copy(section, false);
      isFirstSetRoot = mCurrentSection == null;
    }

    if (mAsyncPropUpdates && !isFirstSetRoot) {
      mCalculateChangeSetRunnable.ensurePosted();
    } else {
      applyNewChangeSet();
    }
  }

  /**
   * Update the root ListComponent. This will create the new ListComponent tree and generate a
   * {@link ChangeSet} to be applied to the UI. In response to this
   * {@link Target#applyNewChangeSet()} will be invoked once the {@link ChangeSet} has
   * been calculated.
   * The generation of the ChangeSet will happen asynchronously in this ListComponentTree
   * ChangeSetThread.
   *
   * @param section The new root.
   */
  public void setRootAsync(Section section) {
    if (mReleased) {
      throw new IllegalStateException("Setting root on a released tree");
    }

    synchronized (this) {
      if (mCurrentSection != null && mCurrentSection.getId() == section.getId()) {
        return;
      }

      if (mNextSection != null && mNextSection.getId() == section.getId()) {
        return;
      }

      mNextSection = copy(section, false);
    }

    mCalculateChangeSetRunnable.ensurePosted();
  }

  /**
   * Asks all the {@link Section} in the tree to refresh themselves.
   * The refresh is by default a no-op. {@link Section}s that need a refresh behaviour should
   * implement a method annotated with {@link com.facebook.litho.sections.annotations.OnRefresh}.
   */
  public void refresh() {
    final Section section;

    synchronized (this) {
      if (mReleased) {
        throw new IllegalStateException("Calling refresh on a released tree");
      }

      section = mCurrentSection;
    }

    if (section == null) {
      return;
    }

    refreshRecursive(section);
  }

  /**
   * Sets the {@link LoadEventsHandler} that will be used to dispatch {@link LoadingEvent} outside
   * this {@link SectionTree}.
   */
  public void setLoadEventsHandler(LoadEventsHandler loadEventsHandler) {
    mLoadEventsHandler = loadEventsHandler;
  }

  private void refreshRecursive(Section section) {
    section.getLifecycle().refresh(section.getScopedContext(), section);

    if (!section.isDiffSectionSpec()) {
      final List<Section> children = section.getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        refreshRecursive(children.get(i));
      }
    }
  }

  private void dataBound() {
    final Section currentSection;
    synchronized (this) {
      currentSection = mCurrentSection;
    }

    if (currentSection != null) {
      mSectionPositionInfo = new HashMap<>();
      calculateRequestFocusDataRecursive(currentSection, 0);
    }

    if (currentSection != null) {
      dataBoundRecursive(currentSection);
    }
  }

  private void dataBoundRecursive(Section section) {

    section.getLifecycle().dataBound(section.getScopedContext(), section);

    if (section.isDiffSectionSpec()) {
      return;
    }

    final List<Section> children = section.getChildren();
    for (int i = 0, size = children.size(); i < size; i++) {
      dataBoundRecursive(children.get(i));
    }
  }

  /**
   * Calculates the global starting index for each section in the hierarchy.
   */
  private void calculateRequestFocusDataRecursive(Section root, int prevChildrenCount) {
    if (root == null) {
      return;
    }

    final List<Section> children = root.getChildren();

    // We reached a leaf, we have the count.
    if (children == null || children.isEmpty()) {
      mSectionPositionInfo.put(
          root.getGlobalKey(),
          Pair.create(prevChildrenCount, root.getCount()));
      return;
    }

    // We have to calculate the starting index for all the children.
    int currentChildrenCount = 0;
    for (int i = 0; i < children.size(); i++) {
      final Section child = children.get(i);
      calculateRequestFocusDataRecursive(child, prevChildrenCount + currentChildrenCount);
      currentChildrenCount += child.getCount();
    }

    mSectionPositionInfo.put(root.getGlobalKey(), Pair.create(prevChildrenCount, root.getCount()));
  }

  public void viewPortChangedFromScrolling(
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex) {
    viewPortChanged(
        firstVisibleIndex,
        lastVisibleIndex,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex,
        ViewportInfo.State.SCROLLING);
  }

  public void viewPortChanged(
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex,
      @ViewportInfo.State int state) {
    final Section currentSection;
    synchronized (this) {
      currentSection = mCurrentSection;
    }
    if (currentSection != null) {
      viewPortChangedRecursive(
          currentSection,
          firstVisibleIndex,
          lastVisibleIndex,
          firstFullyVisibleIndex,
          lastFullyVisibleIndex,
          state);
    }
  }

  private void viewPortChangedRecursive(
      Section section,
      int firstVisibleIndex,
      int lastVisibleIndex,
      int firstFullyVisibleIndex,
      int lastFullyVisibleIndex,
      @ViewportInfo.State int state) {
    Range currentRange = mLastRanges.get(section.getGlobalKey());
    final int totalItemsCount = section.getCount();

    if (currentRange == null) {
      currentRange = acquireRange();
      mLastRanges.put(section.getGlobalKey(), currentRange);
    } else if (currentRange.firstVisibleIndex == firstVisibleIndex &&
        currentRange.lastVisibleIndex == lastVisibleIndex &&
        currentRange.firstFullyVisibleIndex == firstFullyVisibleIndex &&
        currentRange.lastFullyVisibleIndex == lastFullyVisibleIndex &&
        currentRange.totalItemsCount == totalItemsCount) {

      if (state != ViewportInfo.State.DATA_CHANGES) {
        return;
      }
    }

    currentRange.lastVisibleIndex = lastVisibleIndex;
    currentRange.firstVisibleIndex = firstVisibleIndex;
    currentRange.firstFullyVisibleIndex = firstFullyVisibleIndex;
    currentRange.lastFullyVisibleIndex = lastFullyVisibleIndex;
    currentRange.totalItemsCount = totalItemsCount;

    section.getLifecycle().viewportChanged(
        section.getScopedContext(),
        firstVisibleIndex,
        lastVisibleIndex,
        totalItemsCount,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex,
        section);

    if (section.isDiffSectionSpec()) {
      return;
    }

    int offset = 0;
    final List<Section> children = section.getChildren();
    for (int i = 0, size = children.size(); i < size; i++) {
      final Section child = children.get(i);

      int childFirstVisibleIndex = firstVisibleIndex - offset;
      int childLastVisibleIndex = lastVisibleIndex - offset;

      int childFullyFirstVisibleIndex = firstFullyVisibleIndex - offset;
      int childFullyLastVisibleIndex = lastFullyVisibleIndex - offset;

      if (childFirstVisibleIndex >= child.getCount() || childLastVisibleIndex < 0) {
        childFirstVisibleIndex = -1;
        childLastVisibleIndex = -1;
      } else {
        childFirstVisibleIndex = Math.max(childFirstVisibleIndex, 0);
        childLastVisibleIndex = Math.min(childLastVisibleIndex, child.getCount() - 1);
      }

      if (childFullyFirstVisibleIndex >= child.getCount() || childFullyLastVisibleIndex < 0) {
        childFullyFirstVisibleIndex = -1;
        childFullyLastVisibleIndex = -1;
      } else {
        childFullyFirstVisibleIndex = Math.max(childFullyFirstVisibleIndex, 0);
        childFullyLastVisibleIndex = Math.min(childFullyLastVisibleIndex, child.getCount() - 1);
      }

      offset += child.getCount();
      viewPortChangedRecursive(
          child,
          childFirstVisibleIndex,
          childLastVisibleIndex,
          childFullyFirstVisibleIndex,
          childFullyLastVisibleIndex,
          state);
    }
  }

  void requestFocus(Section section, int index) {
    requestFocus(section.getGlobalKey(), index);
  }

  void requestFocusStart(String sectionKey) {
    requestFocus(sectionKey, 0);
  }

  void requestFocusEnd(String sectionKey) {
    requestFocus(sectionKey, mSectionPositionInfo.get(sectionKey).second -1);
  }

  private void requestFocus(String sectionKey, int index) {
    if (mSectionPositionInfo == null) {
      throw new IllegalStateException(
          "You cannot call requestFocus() before dataBound() is called.");
    }

    if (index >= mSectionPositionInfo.get(sectionKey).second) {
      throw new IllegalStateException(
          "You are trying to request focus on an index that is out of bounds: requested " +
              index + " , total " + mSectionPositionInfo.get(sectionKey).second);
    }

    mTarget.requestFocus(getGlobalIndex(sectionKey, index));
  }

  void requestFocusWithOffset(Section section, int index, int offset) {
    requestFocusWithOffset(section.getGlobalKey(), index, offset);
  }

  void requestFocusWithOffset(String sectionKey, int offset) {
    requestFocusWithOffset(sectionKey, 0, offset);
  }

  private void requestFocusWithOffset(String sectionKey, int index, int offset) {
    if (mSectionPositionInfo == null) {
      throw new IllegalStateException(
          "You cannot call requestFocusWithOffset() before dataBound() is called.");
    }

    if (index >= mSectionPositionInfo.get(sectionKey).second) {
      throw new IllegalStateException(
          "You are trying to request focus with offset on an index that is out of bounds: " +
              "requested " + index + " , total " + mSectionPositionInfo.get(sectionKey).second);
    }

    mTarget.requestFocusWithOffset(getGlobalIndex(sectionKey, index), offset);
  }

  private int getGlobalIndex(String sectionKey, int localIndex) {
    return mSectionPositionInfo.get(sectionKey).first + localIndex;
  }

  private static Range acquireRange() {
    //TODO use pools t11953296
    return new Range();
  }

  private static void releaseRange(Range range) {
    //TODO use pools t11953296
  }

  /**
   * Call this when to release this ListComponentTree and make sure that all the Services in the
   * tree get destroyed.
   */
  public void release() {
    final Section toDispose;

    synchronized (this) {
      mReleased = true;
      toDispose = mCurrentSection;
      mCurrentSection = null;
      mNextSection = null;
      mEventHandlers = null;
    }

    for (Range range : mLastRanges.values()) {
      releaseRange(range);
    }
    mLastRanges.clear();
    mSectionPositionInfo = null;
    //TODO use pools t11953296
  }

  public boolean isReleased() {
    return mReleased;
  }

  /**
   * This will be called by the framework when one of the {@link Section} in the tree
   * requests to update its own state.
   * The generation of the ChangeSet will happen synchronously in the thread calling this method.
   *
   * @param key The unique key of the {@link Section} in the tree.
   * @param stateUpdate An implementation of {@link StateUpdate} that knows how to transition to the
   *        new state.
   */
  synchronized void updateState(String key, StateUpdate stateUpdate) {
    if (mAsyncStateUpdates) {
      updateStateAsync(key, stateUpdate);
    } else {
      mCalculateChangeSetOnMainThreadRunnable.cancel();
      addStateUpdateInternal(key, stateUpdate, true);
      mCalculateChangeSetOnMainThreadRunnable.ensurePosted();
    }
  }

  /**
   * This will be called by the framework when one of the {@link Section} in the tree
   * requests to update its own state.
   * The generation of the ChangeSet will happen asynchronously in this ListComponentTree
   * ChangesetThread.
   *
   * @param key The unique key of the {@link Section} in the tree.
   * @param stateUpdate An implementation of {@link StateUpdate} that knows how to transition to the
   *        new state.
   */
  synchronized void updateStateAsync(String key, StateUpdate stateUpdate) {
    mCalculateChangeSetRunnable.cancel();
    addStateUpdateInternal(key, stateUpdate, true);
    mCalculateChangeSetRunnable.ensurePosted();
  }

  synchronized void updateStateLazy(String key, StateUpdate stateUpdate) {
    addStateUpdateInternal(key, stateUpdate, false);
  }

  private static Section copy(Section section, boolean deep) {
    return section != null ? section.makeShallowCopy(deep) : null;
  }

  private synchronized void addStateUpdateInternal(
      String key,
      StateUpdate stateUpdate,
      boolean isNonLazyStateUpdate) {
    if (mReleased) {
      return;
    }

    if (mCurrentSection == null && mNextSection == null) {
      throw new IllegalStateException("State set with no attached Section");
    }

    List<StateUpdate> currentPendingUpdatesForKey = mPendingStateUpdates.get(key);

    if (currentPendingUpdatesForKey == null) {
      currentPendingUpdatesForKey = acquireUpdatesList();
      mPendingStateUpdates.put(key, currentPendingUpdatesForKey);
    }

    mHasNonLazyUpdate = isNonLazyStateUpdate || mHasNonLazyUpdate;
    currentPendingUpdatesForKey.add(stateUpdate);

    // If the state update is lazy, do not create a new tree root because the calculation of
    // a new tree will not happen yet.
    if (!isNonLazyStateUpdate) {
      return;
    }

    // Need to calculate a new tree since the state changed. The next tree root will be the same
    // of the current tree or a copy of the next pending root.
    if (mNextSection == null) {
      mNextSection = copy(mCurrentSection, false);
    } else {
      mNextSection = copy(mNextSection, false);
    }
  }

  private void applyNewChangeSet() {
    Section<?> currentRoot;
    Section<?> nextRoot;
    Map<String, List<StateUpdate>> pendingStateUpdates = acquireUpdatesMap();

    synchronized (this) {
      if (mReleased) {
        return;
      }

      currentRoot = copy(mCurrentSection, true);
      nextRoot = copy(mNextSection, false);
      clonePendingStateUpdatesFromInstanceToLocal(mPendingStateUpdates, pendingStateUpdates);
      mHasNonLazyUpdate = false;
    }

    // Checking nextRoot is enough here since whenever we enqueue a new state update we also
    // re-assign nextRoot.
    while (nextRoot != null) {
      final ChangeSetState changeSetState =
          calculateNewChangeSet(
              mContext, currentRoot, nextRoot, pendingStateUpdates, mSectionComponentLogger, mTag);

      final boolean changeSetIsValid;
      Section oldRoot = null;
      Section newRoot = null;

      synchronized (this) {

        boolean currentNotNull = currentRoot != null;
        boolean instanceCurrentNotNull = mCurrentSection != null;
        boolean currentIsSame = (currentNotNull && instanceCurrentNotNull &&
            currentRoot.getId() == mCurrentSection.getId()) ||
            (!currentNotNull && !instanceCurrentNotNull);

        boolean nextNotNull = nextRoot != null;
        boolean instanceNextNotNull = mNextSection != null;
        boolean nextIsSame = (nextNotNull && instanceNextNotNull &&
            nextRoot.getId() == mNextSection.getId()) ||
            (!nextNotNull && !instanceNextNotNull);

        changeSetIsValid = currentIsSame &&
            nextIsSame &&
            isStateUpdateCompleted(pendingStateUpdates);

        if (changeSetIsValid) {
          oldRoot = mCurrentSection;
          newRoot = nextRoot;

          mCurrentSection = newRoot;
          mNextSection = null;
          removeCompletedStateUpdatesFromInstance(mPendingStateUpdates, pendingStateUpdates);
          mPendingChangeSets.add(changeSetState.getChangeSet());

          if (oldRoot != null) {
            unbindOldComponent(oldRoot);
            oldRoot.release();
          }

          bindNewComponent(newRoot);
        }
      }

      if (changeSetIsValid) {
        final List<Section> removedComponents = changeSetState.getRemovedComponents();
        for (int i = 0, size = removedComponents.size(); i < size; i++) {
          final Section removedComponent = removedComponents.get(i);
          releaseRange(mLastRanges.remove(removedComponent.getGlobalKey()));
        }

        postNewChangeSets();
      }

      synchronized (this) {
        if (mReleased) {
          return;
        }

        currentRoot = copy(mCurrentSection, true);
        nextRoot = copy(mNextSection, false);
        if (nextRoot != null) {
          clonePendingStateUpdatesFromInstanceToLocal(mPendingStateUpdates, pendingStateUpdates);
          mHasNonLazyUpdate = false;
        }
      }
    }
  }

  /**
   * If the completed state update map is equal to the pending state update map, state update is
   * completed.
   *
   * Otherwise, we check if there is any new non-lazy state updates enqueued.
   *
   * @return true if there is no more state update to be processed immediately
   */
  private synchronized boolean isStateUpdateCompleted(Map<String, List<StateUpdate>> localMap) {
    return localMap.equals(mPendingStateUpdates) || !mHasNonLazyUpdate;
  }

  @GuardedBy("this")
  private static void clonePendingStateUpdatesFromInstanceToLocal(
      Map<String, List<StateUpdate>> instanceMap,
      Map<String, List<StateUpdate>> localMap) {
    if (localMap == null) {
      throw new IllegalArgumentException("Provide a local variable Map for state update transfer");
    }

    localMap.clear();

    if (instanceMap.isEmpty()) {
      return;
    }

    final Set<String> keys = instanceMap.keySet();
    for (String key : keys) {
      localMap.put(key, new ArrayList<>(instanceMap.get(key)));
    }
  }

  @GuardedBy("this")
  private static void removeCompletedStateUpdatesFromInstance(
      Map<String, List<StateUpdate>> instanceMap,
      Map<String, List<StateUpdate>> localMap) {
    if (localMap.isEmpty()) {
      return;
    }

    final Set<String> keys = localMap.keySet();
    for (String key : keys) {
      List<StateUpdate> completed = localMap.get(key);
      List<StateUpdate> pending = instanceMap.remove(key);
      pending.removeAll(completed);

      if (!pending.isEmpty()) {
        instanceMap.put(key, pending);
      }
    }
  }

  void dispatchLoadingEvent(
      LoadingEvent event) {
    if (mLoadEventsHandler != null) {
      final LoadingEvent.LoadingState loadingState = event.loadingState;
      final boolean isEmpty = event.isEmpty;

      switch (loadingState) {
        case FAILED:
          mLoadEventsHandler.onLoadFailed(isEmpty);
          break;
        case INITIAL_LOAD:
          mLoadEventsHandler.onInitialLoad();
          break;
        case LOADING:
          mLoadEventsHandler.onLoadStarted(isEmpty);
          break;
        case SUCCEEDED:
          mLoadEventsHandler.onLoadSucceeded(isEmpty);
          break;
      }
    }
  }

  private void bindNewComponent(Section<?> section) {
    section.getLifecycle().bindService(section.getScopedContext(), section);
    bindEventHandlers(section);

    if (!section.isDiffSectionSpec()) {
      final List<Section> children = section.getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        bindNewComponent(children.get(i));
      }
    }
  }

  private void unbindOldComponent(Section<?> section) {
    section.getLifecycle().unbindService(section.getScopedContext(), section);

    if (!section.isDiffSectionSpec()) {
      final List<Section> children = section.getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        unbindOldComponent(children.get(i));
      }
    }
  }

  private void postNewChangeSets() {
    if (isMainThread()) {
      postChangesetsToHandler();
    } else {
      sMainThreadHandler.obtainMessage(MESSAGE_WHAT_BACKGROUND_CHANGESET_STATE_UPDATED, this)
          .sendToTarget();
    }
  }

  private void postChangesetsToHandler() {
    assertMainThread();

    final List<ChangeSet> changeSets;
    synchronized (this) {
      if (mReleased) {
        return;
      }

      changeSets = new ArrayList<>(mPendingChangeSets);
      mPendingChangeSets.clear();
    }

    boolean appliedChanges = false;
    for (int i = 0, size = changeSets.size(); i < size; i++) {
      final ChangeSet changeSet = changeSets.get(i);

      if (changeSet.getChangeCount() > 0) {
        for (int j = 0, changeSize = changeSet.getChangeCount(); j < changeSize; j++) {
          final Change change = changeSet.getChangeAt(j);
          switch (change.getType()) {
            case Change.INSERT:
              appliedChanges = true;
              mTarget.insert(change.getIndex(), change.getRenderInfo());
              break;
            case Change.INSERT_RANGE:
              appliedChanges = true;
              mTarget.insertRange(change.getIndex(), change.getCount(), change.getRenderInfos());
              break;
            case Change.UPDATE:
              appliedChanges = true;
              mTarget.update(change.getIndex(), change.getRenderInfo());
              break;
            case Change.UPDATE_RANGE:
              appliedChanges = true;
              mTarget.updateRange(change.getIndex(), change.getCount(), change.getRenderInfos());
              break;
            case Change.DELETE:
              appliedChanges = true;
              mTarget.delete(change.getIndex());
              break;
            case Change.DELETE_RANGE:
              appliedChanges = true;
              mTarget.deleteRange(change.getIndex(), change.getCount());
              break;
            case Change.MOVE:
              appliedChanges = true;
              mTarget.move(change.getIndex(), change.getToIndex());
          }
        }
        mTarget.dispatchLastEvent();
      }
    }

    if (appliedChanges) {
      dataBound();
    }
  }

  private static ChangeSetState calculateNewChangeSet(
      SectionContext context,
      Section<?> currentRoot,
      Section<?> nextRoot,
      Map<String, List<StateUpdate>> pendingStateUpdates,
      SectionComponentLogger sectionComponentLogger,
      String sectionTreeTag) {
    nextRoot.setGlobalKey(nextRoot.getKey());
    createNewTreeAndApplyStateUpdates(
        context,
        currentRoot,
        nextRoot,
        pendingStateUpdates,
        sectionComponentLogger,
        sectionTreeTag);

    return ChangeSetState.generateChangeSet(
        context,
        currentRoot,
        nextRoot,
        sectionComponentLogger,
        sectionTreeTag,
        "",
        "");
  }

  /**
   * Creates the new tree, transfers state/services from the current tree and applies all the
   * state updates that have been enqueued since the last tree calculation.
   */
  private static void createNewTreeAndApplyStateUpdates(
      SectionContext context,
      Section<?> currentRoot,
      Section<?> nextRoot,
      Map<String, List<StateUpdate>> pendingStateUpdates,
      SectionComponentLogger sectionComponentLogger,
      String sectionTreeTag) {
    if (nextRoot == null) {
      throw new IllegalStateException("Can't generate a subtree with a null root");
    }

    nextRoot.setScopedContext(SectionContext.withScope(context, nextRoot));
    if (currentRoot != null) {
      nextRoot.setCount(currentRoot.getCount());
    }

    final SectionLifecycle sectionLifecycle = nextRoot.getLifecycle();
    final boolean shouldTransferState =
        currentRoot != null
            && currentRoot.getLifecycle().getClass().equals(sectionLifecycle.getClass());

    if (currentRoot == null || !shouldTransferState) {
      sectionLifecycle.createInitialState(nextRoot.getScopedContext(), nextRoot);
      sectionLifecycle.createService(nextRoot.getScopedContext(), nextRoot);
    } else {
      sectionLifecycle.transferService(nextRoot.getScopedContext(), currentRoot, nextRoot);
      sectionLifecycle.transferState(
          nextRoot.getScopedContext(),
          currentRoot.getStateContainer(),
          nextRoot);
    }

    // TODO: t18544409 Make sure this is absolutely the best solution as this is an anti-pattern
    final List<StateUpdate> stateUpdates = pendingStateUpdates.get(nextRoot.getGlobalKey());
    if (stateUpdates != null) {
      for (int i = 0, size = stateUpdates.size(); i < size; i++) {
        stateUpdates.get(i).updateState(nextRoot.getStateContainer(), nextRoot);
      }

      if (sectionLifecycle.shouldComponentUpdate(currentRoot, nextRoot)) {
        nextRoot.invalidate();
      }
    }

    if (!sectionLifecycle.isDiffSectionSpec()) {
      final Map<String, Pair<Section, Integer>> currentComponentChildren = currentRoot == null ?
          null :
          Section.acquireChildrenMap(currentRoot);

      final TreeProps parentTreeProps = context.getTreeProps();
      sectionLifecycle.populateTreeProps(nextRoot, parentTreeProps);
      context.setTreeProps(
          sectionLifecycle.getTreePropsForChildren(context, nextRoot, parentTreeProps));

      nextRoot.setChildren(sectionLifecycle.createChildren(
          nextRoot.getScopedContext(),
          nextRoot));

      final List<Section> nextRootChildren = nextRoot.getChildren();

      Set<String> keysSet = acquireKeysSet();
      for (int i = 0, size = nextRootChildren.size(); i < size; i++) {
        final Section child = nextRootChildren.get(i);
        child.setParent(nextRoot);
        final String childKey = child.getKey();
        final String globalKey = nextRoot.getGlobalKey() + childKey;
        if (TextUtils.isEmpty(childKey) || keysSet.contains(globalKey)) {
          final String errorMessage = TextUtils.isEmpty(childKey)
              ? ("Your Section " +
              child.getLifecycle().getClass().getSimpleName() +
              " has an empty key. Please specify a key.")
              : ("You have two Sections with the same key: " + child.getKey() +
              ", as children of " +
              nextRoot.getLifecycle().getClass().getSimpleName() +
              ". Please specify different keys.");

          throw new IllegalStateException(errorMessage);
        }

        child.setGlobalKey(globalKey);
        keysSet.add(globalKey);
        child.setScopedContext(SectionContext.withScope(context, child));

        final Pair<Section,Integer> valueAndIndex = currentComponentChildren == null ?
            null :
            currentComponentChildren.get(child.getGlobalKey());
        final Section currentChild = valueAndIndex != null ? valueAndIndex.first : null;

        createNewTreeAndApplyStateUpdates(
            context,
            currentChild,
            child,
            pendingStateUpdates,
            sectionComponentLogger,
            sectionTreeTag);
      }

      releaseKeySet(keysSet);

      if (context.getTreeProps() != parentTreeProps) {
        ComponentsPools.release(context.getTreeProps());
        context.setTreeProps(parentTreeProps);
      }
    }
  }

  private static Set<String> acquireKeysSet() {
    //TODO use pools t11953296
    return new HashSet<>();
  }

  private static void releaseKeySet(Set<String> key) {
    //TODO use pools t11953296
  }

  private static synchronized Looper getDefaultChangeSetThreadLooper() {
    if (sDefaultChangeSetThreadLooper == null) {
      HandlerThread defaultThread =
          new HandlerThread(DEFAULT_CHANGESET_THREAD_NAME, DEFAULT_CHANGESET_THREAD_PRIORITY);
      defaultThread.start();
      sDefaultChangeSetThreadLooper = defaultThread.getLooper();
    }

    return sDefaultChangeSetThreadLooper;
  }

  private Map<String, List<StateUpdate>> acquireUpdatesMap() {
    return new HashMap<>();
  }

  private static List<StateUpdate> acquireUpdatesList() {
    //TODO use pools t11953296
    return new ArrayList<>();
  }

  private static void releaseUpdatesList(List<StateUpdate> stateUpdates) {
    //TODO use pools t11953296
  }

  private static class SectionsComponentMainThreadHandler extends Handler {

    private SectionsComponentMainThreadHandler() {
      super(Looper.getMainLooper());
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MESSAGE_WHAT_BACKGROUND_CHANGESET_STATE_UPDATED:
          final SectionTree tree = (SectionTree) msg.obj;
          try {
            tree.postChangesetsToHandler();
          } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(getDebugInfo(tree) + e.getMessage());
          }
          break;

        default:
          throw new IllegalArgumentException();
      }
    }
  }

  private static String getDebugInfo(SectionTree tree) {
    final StringBuilder sb = new StringBuilder();
    sb.append("tag: ");
    sb.append(tree.mTag);

    sb.append(", currentSection.size: ");
    sb.append(tree.mCurrentSection != null ? tree.mCurrentSection.getCount() : null);

    sb.append(", nextSection.size: ");
    sb.append(tree.mNextSection != null ? tree.mNextSection.getCount() : null);

    sb.append(", pendingChangeSets.size: ");
    sb.append(tree.mPendingChangeSets.size());

    sb.append(", pendingStateUpdates.size: ");
    sb.append(tree.mPendingStateUpdates.size());

    sb.append(", hasNonLazyUpdate: ");
    sb.append(tree.mHasNonLazyUpdate);
    sb.append("\n");
    return sb.toString();
  }

  /**
   * A builder class that can be used to create a {@link SectionTree}.
   */
  public static class Builder {

    private final SectionContext mContext;
    private final Target mTarget;
    private boolean mAsyncStateUpdates;
    private boolean mAsyngPropUpdates;
    private String mTag;
    private Handler mChangeSetThreadHandler;

    private Builder(SectionContext componentContext, Target target) {
      mContext = componentContext;
      mTarget = target;
      mAsyncStateUpdates = SectionComponentsConfiguration.sectionComponentsAsyncStateUpdates;
      mAsyngPropUpdates = SectionComponentsConfiguration.sectionComponentsAsyncPropUpdates;
    }

    /**
     * An optional Handler where {@link ChangeSet} calculation should happen. If not provided the
     * framework will use its default background thread.
     */
    public Builder changeSetThreadHandler(Handler changeSetThreadHandler) {
      mChangeSetThreadHandler = changeSetThreadHandler;
      return this;
    }

    /**
     * If enabled, all state updates will be performed on a background thread.
     * @return
     */
    public Builder asyncStateUpdates(boolean asyncStateUpdates) {
      mAsyncStateUpdates = asyncStateUpdates;
      return this;
    }

    /**
     * If enabled, any prop updates done with setRoot will be on a background thread. This excludes
     * the very first setRoot where there isn't an existing section. That one will still be on the
     * main thread.
     */
    public Builder asyncPropUpdates(boolean asyngPropUpdates) {
      mAsyngPropUpdates = asyngPropUpdates;
      return this;
    }

    /**
     * If enabled, a tag will define the section tree being built
     *
     * @param tag tag that labels the section tree
     * @return
     */
    public Builder tag(String tag) {
      mTag = (tag == null) ? "" : tag;
      return this;
    }

    /**
     * @return the {@link SectionTree}.
     */
    public SectionTree build() {
      return new SectionTree(this);
    }
  }
}
