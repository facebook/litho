/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections;

import static com.facebook.litho.FrameworkLogEvents.EVENT_SECTIONS_CREATE_NEW_TREE;
import static com.facebook.litho.FrameworkLogEvents.EVENT_SECTIONS_ON_CREATE_CHILDREN;
import static com.facebook.litho.FrameworkLogEvents.EVENT_SECTIONS_SET_ROOT;
import static com.facebook.litho.FrameworkLogEvents.PARAM_SECTION_SET_ROOT_SOURCE;
import static com.facebook.litho.FrameworkLogEvents.PARAM_SET_ROOT_ON_BG_THREAD;
import static com.facebook.litho.ThreadUtils.assertMainThread;
import static com.facebook.litho.ThreadUtils.isMainThread;
import static com.facebook.litho.sections.SectionLifecycle.StateUpdate;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.UiThread;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.ComponentsPools;
import com.facebook.litho.ComponentsSystrace;
import com.facebook.litho.EventHandler;
import com.facebook.litho.EventTrigger;
import com.facebook.litho.EventTriggersContainer;
import com.facebook.litho.LogEvent;
import com.facebook.litho.ThreadUtils;
import com.facebook.litho.TreeProps;
import com.facebook.litho.sections.SectionsLogEventUtils.ApplyNewChangeSet;
import com.facebook.litho.sections.config.SectionsConfiguration;
import com.facebook.litho.sections.logger.SectionsDebugLogger;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.litho.widget.ViewportInfo;
import java.util.ArrayList;
import java.util.HashMap;
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

  private static final String DEFAULT_CHANGESET_THREAD_NAME = "SectionChangeSetThread";
  private static final int DEFAULT_CHANGESET_THREAD_PRIORITY = Process.THREAD_PRIORITY_BACKGROUND;
  private final SectionsDebugLogger mSectionsDebugLogger;
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
  private static final int MESSAGE_FOCUS_REQUEST = 2;
  private static final int MESSAGE_FOCUS_DISPATCHER_LOADING_STATE_UPDATE = 3;
  private static final Handler sMainThreadHandler = new SectionsMainThreadHandler();

  @GuardedBy("SectionTree.class")
  private static volatile Looper sDefaultChangeSetThreadLooper;

  private final SectionContext mContext;
  private final BatchedTarget mTarget;
  private final FocusDispatcher mFocusDispatcher;
  private final boolean mAsyncStateUpdates;
  private final boolean mAsyncPropUpdates;
  private final String mTag;
  private final Map<String, Range> mLastRanges = new HashMap<>();
  private final boolean mForceSyncStateUpdates;

  // Holds a Pair where the first item is a section's global starting index
  // and the second is the count.
  // Guarded by UI Thread.
  private Map<String, Pair<Integer, Integer>> mSectionPositionInfo;

  private LoadEventsHandler mLoadEventsHandler;

  private final CalculateChangeSetRunnable mCalculateChangeSetOnMainThreadRunnable;
  private final CalculateChangeSetRunnable mCalculateChangeSetRunnable;

  private class CalculateChangeSetRunnable implements Runnable {

    private final Handler mHandler;

    @GuardedBy("this")
    private boolean mIsPosted;

    @GuardedBy("this")
    private @ApplyNewChangeSet int mSource;

    public CalculateChangeSetRunnable(Handler handler) {
      mHandler = handler;
    }

    public synchronized void ensurePosted(@ApplyNewChangeSet int source) {
      if (!mIsPosted) {
        mIsPosted = true;
        mHandler.post(this);
        mSource = source;
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
      @ApplyNewChangeSet int source;
      synchronized (this) {
        if (!mIsPosted) {
          return;
        }
        source = mSource;
        mSource = ApplyNewChangeSet.NONE;
        mIsPosted = false;
      }

      try {
        applyNewChangeSet(source);
      } catch (IndexOutOfBoundsException e) {
        throw new RuntimeException(getDebugInfo(SectionTree.this) + e.getMessage(), e);
      }
    }
  }

  @GuardedBy("this")
  private @Nullable Section mCurrentSection;

  @GuardedBy("this")
  private @Nullable Section mNextSection;

  @GuardedBy("this")
  private StateUpdatesHolder mPendingStateUpdates;

  @GuardedBy("this")
  private List<ChangeSet> mPendingChangeSets;

  @GuardedBy("this")
  private Map<String, List<EventHandler>> mEventHandlers = new HashMap<>();

  @GuardedBy("this")
  private final EventTriggersContainer mEventTriggersContainer = new EventTriggersContainer();

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

  private synchronized void bindTriggerHandler(Section section) {
    section.recordEventTrigger(mEventTriggersContainer);

    final List<Section> children = section.getChildren();
    if (children != null) {
      for (int i = 0, size = children.size(); i < size; i++) {
        bindTriggerHandler(children.get(i));
      }
    }
  }

  private synchronized void clearUnusedTriggerHandlers() {
    mEventTriggersContainer.clear();
  }

  @Nullable
  synchronized EventTrigger getEventTrigger(String triggerKey) {
    return mEventTriggersContainer.getEventTrigger(triggerKey);
  }

  private SectionTree(Builder builder) {
    mSectionsDebugLogger = new Logger(SectionsConfiguration.LOGGERS);
    mReleased = false;
    mAsyncStateUpdates = builder.mAsyncStateUpdates;
    mForceSyncStateUpdates = builder.mForceSyncStateUpdates;
    if (mAsyncStateUpdates && mForceSyncStateUpdates) {
      throw new RuntimeException("Cannot force both sync and async state updates at the same time");
    }

    mAsyncPropUpdates = builder.mAsyncPropUpdates;
    mTag = builder.mTag;
    mTarget = new BatchedTarget(builder.mTarget, mSectionsDebugLogger, mTag);
    mFocusDispatcher = new FocusDispatcher(mTarget);
    mContext = SectionContext.withSectionTree(builder.mContext, this);
    mPendingChangeSets = new ArrayList<>();
    mPendingStateUpdates = SectionsPools.acquireStateUpdatesHolder();
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
   * Update the root Section. This will create the new Section tree and generate a {@link ChangeSet}
   * to be applied to the UI. In response to this {@link Target#applyNewChangeSet(int)} ()} will be
   * invoked once the {@link ChangeSet} has been calculated. The generation of the ChangeSet will
   * happen synchronously in the thread calling this method.
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
      mCalculateChangeSetRunnable.ensurePosted(ApplyNewChangeSet.SET_ROOT_ASYNC);
    } else {
      applyNewChangeSet(ApplyNewChangeSet.SET_ROOT);
    }
  }

  /**
   * Update the root Section. This will create the new Section tree and generate a {@link ChangeSet}
   * to be applied to the UI. In response to this {@link Target#applyNewChangeSet(int)} will be
   * invoked once the {@link ChangeSet} has been calculated. The generation of the ChangeSet will
   * happen asynchronously in this SectionTree ChangeSetThread.
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

    mCalculateChangeSetRunnable.ensurePosted(ApplyNewChangeSet.SET_ROOT_ASYNC);
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
    section.refresh(section.getScopedContext());

    if (!section.isDiffSectionSpec()) {
      final List<Section> children = section.getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        refreshRecursive(children.get(i));
      }
    }
  }

  @UiThread
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

  @UiThread
  private void dataBoundRecursive(Section section) {

    section.dataBound(section.getScopedContext());

    if (section.isDiffSectionSpec()) {
      return;
    }

    final List<Section> children = section.getChildren();
    for (int i = 0, size = children.size(); i < size; i++) {
      dataBoundRecursive(children.get(i));
    }
  }

  /** Calculates the global starting index for each section in the hierarchy. */
  @UiThread
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

    section.viewportChanged(
        section.getScopedContext(),
        firstVisibleIndex,
        lastVisibleIndex,
        totalItemsCount,
        firstFullyVisibleIndex,
        lastFullyVisibleIndex);

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

  public void requestFocusOnRoot(int index) {
    final String sectionKey;
    synchronized (this) {
      if (mCurrentSection == null) {
        return;
      }

      sectionKey = mCurrentSection.getGlobalKey();
    }

    requestFocus(sectionKey, index);
  }

  void requestFocus(Section section, int index) {
    requestFocus(section.getGlobalKey(), index);
  }

  void requestFocusStart(String sectionKey) {
    requestFocus(sectionKey, 0);
  }

  void requestFocusEnd(final String sectionKey) {
    focusRequestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            if (mSectionPositionInfo == null) {
              throw new IllegalStateException(
                  "You cannot call requestFocusWithOffset() before dataBound() is called.");
            }

            mFocusDispatcher.requestFocus(
                getGlobalIndex(sectionKey, mSectionPositionInfo.get(sectionKey).second - 1));
          }
        });
  }

  private void requestFocus(final String sectionKey, final int index) {
    focusRequestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            checkFocusValidity(sectionKey, index);
            mFocusDispatcher.requestFocus(getGlobalIndex(sectionKey, index));
          }
        });
  }

  void requestFocusWithOffset(Section section, int index, int offset) {
    requestFocusWithOffset(section.getGlobalKey(), index, offset);
  }

  void requestFocusWithOffset(String sectionKey, int offset) {
    requestFocusWithOffset(sectionKey, 0, offset);
  }

  private void requestFocusWithOffset(final String sectionKey, final int index, final int offset) {
    focusRequestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            checkFocusValidity(sectionKey, index);
            mFocusDispatcher.requestFocusWithOffset(getGlobalIndex(sectionKey, index), offset);
          }
        });
  }

  @UiThread
  private void checkFocusValidity(String sectionKey, int index) {
    if (mSectionPositionInfo == null) {
      throw new IllegalStateException(
          "You cannot call requestFocusWithOffset() before dataBound() is called.");
    }

    if (index >= mSectionPositionInfo.get(sectionKey).second) {
      throw new IllegalStateException(
          "You are trying to request focus with offset on an index that is out of bounds: " +
              "requested " + index + " , total " + mSectionPositionInfo.get(sectionKey).second);
    }
  }

  private static void focusRequestOnUiThread(Runnable runnable) {
    if (isMainThread()) {
      runnable.run();
    } else {
      sMainThreadHandler.obtainMessage(MESSAGE_FOCUS_REQUEST, runnable).sendToTarget();
    }
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
   * Call this when to release this SectionTree and make sure that all the Services in the tree get
   * destroyed.
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
    clearUnusedTriggerHandlers();
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
      addStateUpdateInternal(key, stateUpdate, false);
      mCalculateChangeSetOnMainThreadRunnable.ensurePosted(ApplyNewChangeSet.UPDATE_STATE);
    }
  }

  /**
   * This will be called by the framework when one of the {@link Section} in the tree requests to
   * update its own state. The generation of the ChangeSet will happen asynchronously in this
   * SectionTree ChangesetThread.
   *
   * @param key The unique key of the {@link Section} in the tree.
   * @param stateUpdate An implementation of {@link StateUpdate} that knows how to transition to the
   *     new state.
   */
  synchronized void updateStateAsync(String key, StateUpdate stateUpdate) {
    if (mForceSyncStateUpdates) {
      updateState(key, stateUpdate);
    } else {
      mCalculateChangeSetRunnable.cancel();
      addStateUpdateInternal(key, stateUpdate, false);
      mCalculateChangeSetRunnable.ensurePosted(ApplyNewChangeSet.UPDATE_STATE_ASYNC);
    }
  }

  synchronized void updateStateLazy(String key, StateUpdate stateUpdate) {
    addStateUpdateInternal(key, stateUpdate, true);
  }

  private static Section copy(Section section, boolean deep) {
    return section != null ? section.makeShallowCopy(deep) : null;
  }

  private synchronized void addStateUpdateInternal(
      String key, StateUpdate stateUpdate, boolean isLazyStateUpdate) {
    if (mReleased) {
      return;
    }

    if (mCurrentSection == null && mNextSection == null) {
      throw new IllegalStateException("State set with no attached Section");
    }

    List<StateUpdate> currentPendingUpdatesForKey = mPendingStateUpdates.mAllStateUpdates.get(key);

    if (currentPendingUpdatesForKey == null) {
      currentPendingUpdatesForKey = acquireUpdatesList();
      mPendingStateUpdates.mAllStateUpdates.put(key, currentPendingUpdatesForKey);
    }

    currentPendingUpdatesForKey.add(stateUpdate);

    // If the state update is lazy, do not create a new tree root because the calculation of
    // a new tree will not happen yet.
    if (isLazyStateUpdate) {
      return;
    }

    List<StateUpdate> currentPendingNonLazyUpdatesForKey =
        mPendingStateUpdates.mNonLazyStateUpdates.get(key);

    if (currentPendingNonLazyUpdatesForKey == null) {
      currentPendingNonLazyUpdatesForKey = acquireUpdatesList();
      mPendingStateUpdates.mNonLazyStateUpdates.put(key, currentPendingNonLazyUpdatesForKey);
    }

    currentPendingNonLazyUpdatesForKey.add(stateUpdate);

    // Need to calculate a new tree since the state changed. The next tree root will be the same
    // of the current tree or a copy of the next pending root.
    if (mNextSection == null) {
      mNextSection = copy(mCurrentSection, false);
    } else {
      mNextSection = copy(mNextSection, false);
    }
  }

  private void applyNewChangeSet(@ApplyNewChangeSet int source) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection(
          "applyNewChangeSet_" + SectionsLogEventUtils.applyNewChangeSetSourceToString(source));
    }

    try {
      Section currentRoot;
      Section nextRoot;
      StateUpdatesHolder pendingStateUpdates;

      final ComponentsLogger logger;
      final String logTag;

      synchronized (this) {
        if (mReleased) {
          return;
        }

        currentRoot = copy(mCurrentSection, true);
        nextRoot = copy(mNextSection, false);
        logger = mContext.getLogger();
        logTag = mContext.getLogTag();
        pendingStateUpdates = copyPendingStateUpdates();
      }

      LogEvent logEvent = null;

      if (logger != null) {
        logEvent =
            SectionsLogEventUtils.getSectionsPerformanceEvent(
                logger, logTag, EVENT_SECTIONS_SET_ROOT, currentRoot, nextRoot);
        logEvent.addParam(
            PARAM_SECTION_SET_ROOT_SOURCE,
            SectionsLogEventUtils.applyNewChangeSetSourceToString(source));
        logEvent.addParam(PARAM_SET_ROOT_ON_BG_THREAD, !ThreadUtils.isMainThread());
      }

      clearUnusedTriggerHandlers();

      // Checking nextRoot is enough here since whenever we enqueue a new state update we also
      // re-assign nextRoot.
      while (nextRoot != null) {
        final ChangeSetState changeSetState =
            calculateNewChangeSet(
                mContext,
                currentRoot,
                nextRoot,
                pendingStateUpdates.mAllStateUpdates,
                mSectionsDebugLogger,
                mTag);

        final boolean changeSetIsValid;
        Section oldRoot = null;
        Section newRoot = null;

        synchronized (this) {
          boolean currentNotNull = currentRoot != null;
          boolean instanceCurrentNotNull = mCurrentSection != null;
          boolean currentIsSame =
              (currentNotNull
                      && instanceCurrentNotNull
                      && currentRoot.getId() == mCurrentSection.getId())
                  || (!currentNotNull && !instanceCurrentNotNull);
          boolean nextIsSame = (mNextSection != null && nextRoot.getId() == mNextSection.getId());

          changeSetIsValid =
              currentIsSame && nextIsSame && isStateUpdateCompleted(pendingStateUpdates);

          if (changeSetIsValid) {
            oldRoot = mCurrentSection;
            newRoot = nextRoot;

            mCurrentSection = newRoot;
            mNextSection = null;
            removeCompletedStateUpdatesFromInstance(pendingStateUpdates);
            mPendingChangeSets.add(changeSetState.getChangeSet());

            if (oldRoot != null) {
              unbindOldComponent(oldRoot);
              oldRoot.release();
            }

            bindNewComponent(newRoot);
            bindTriggerHandler(newRoot);
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
          SectionsPools.release(pendingStateUpdates);

          if (mReleased) {
            return;
          }

          currentRoot = copy(mCurrentSection, true);
          nextRoot = copy(mNextSection, false);
          if (nextRoot != null) {
            pendingStateUpdates = copyPendingStateUpdates();
          }
        }
      }

      if (logger != null) {
        logger.log(logEvent);
      }
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  @GuardedBy("this")
  private StateUpdatesHolder copyPendingStateUpdates() {
    StateUpdatesHolder clonedPendingStateUpdates = SectionsPools.acquireStateUpdatesHolder();

    if (mPendingStateUpdates.mAllStateUpdates.isEmpty()) {
      return clonedPendingStateUpdates;
    }

    final Set<String> keys = mPendingStateUpdates.mAllStateUpdates.keySet();
    for (String key : keys) {
      clonedPendingStateUpdates.mAllStateUpdates.put(
          key, new ArrayList<>(mPendingStateUpdates.mAllStateUpdates.get(key)));
    }

    final Set<String> keysNonLazy = mPendingStateUpdates.mNonLazyStateUpdates.keySet();
    for (String key : keysNonLazy) {
      clonedPendingStateUpdates.mNonLazyStateUpdates.put(
          key, new ArrayList<>(mPendingStateUpdates.mNonLazyStateUpdates.get(key)));
    }

    return clonedPendingStateUpdates;
  }

  /**
   * The state update is completed if there are no new non-lazy state updates enqueued.
   *
   * @return true if there are no more state updates to be processed immediately.
   */
  private synchronized boolean isStateUpdateCompleted(StateUpdatesHolder localPendingStateUpdates) {
    return localPendingStateUpdates.mNonLazyStateUpdates.equals(
        mPendingStateUpdates.mNonLazyStateUpdates);
  }

  @GuardedBy("this")
  private void removeCompletedStateUpdatesFromInstance(
      StateUpdatesHolder localPendingStateUpdates) {
    if (localPendingStateUpdates.mAllStateUpdates.isEmpty()) {
      return;
    }

    final Set<String> keys = localPendingStateUpdates.mAllStateUpdates.keySet();
    for (String key : keys) {
      if (!mPendingStateUpdates.mAllStateUpdates.containsKey(key)) {
        // instanceMap has been modified and since the localMap is created, so it's no longer valid.
        // We will exit the function early
        return;
      }

      List<StateUpdate> completed = localPendingStateUpdates.mAllStateUpdates.get(key);
      List<StateUpdate> pending = mPendingStateUpdates.mAllStateUpdates.remove(key);
      pending.removeAll(completed);
      if (!pending.isEmpty()) {
        mPendingStateUpdates.mAllStateUpdates.put(key, pending);
      }

      List<StateUpdate> completedNonLazy = localPendingStateUpdates.mNonLazyStateUpdates.get(key);
      List<StateUpdate> pendingNonLazy = mPendingStateUpdates.mNonLazyStateUpdates.remove(key);
      if (completedNonLazy != null && pendingNonLazy != null) {
        pendingNonLazy.removeAll(completedNonLazy);
      }
      if (pendingNonLazy != null && !pendingNonLazy.isEmpty()) {
        mPendingStateUpdates.mNonLazyStateUpdates.put(key, pendingNonLazy);
      }
    }
  }

  void dispatchLoadingEvent(LoadingEvent event) {
    final LoadingEvent.LoadingState loadingState = event.loadingState;
    if (mLoadEventsHandler != null) {
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

    postLoadingStateToFocusDispatch(loadingState);
  }

  private void postLoadingStateToFocusDispatch(final LoadingEvent.LoadingState loadingState) {
    if (isMainThread()) {
      setLoadingStateToFocusDispatch(loadingState);
    } else {
      sMainThreadHandler
          .obtainMessage(
              MESSAGE_FOCUS_DISPATCHER_LOADING_STATE_UPDATE,
              new Runnable() {
                @Override
                public void run() {
                  setLoadingStateToFocusDispatch(loadingState);
                }
              })
          .sendToTarget();
    }
  }

  private void setLoadingStateToFocusDispatch(final LoadingEvent.LoadingState loadingState) {
    if (loadingState == LoadingEvent.LoadingState.INITIAL_LOAD
        || loadingState == LoadingEvent.LoadingState.LOADING) {
      mFocusDispatcher.waitForDataBound(true);
    }

    if (loadingState == LoadingEvent.LoadingState.FAILED) {
      mFocusDispatcher.waitForDataBound(false);
    }

    mFocusDispatcher.setLoadingState(loadingState);
    mFocusDispatcher.maybeDispatchFocusRequests();
  }

  private void bindNewComponent(Section section) {
    section.bindService(section.getScopedContext());
    bindEventHandlers(section);

    if (!section.isDiffSectionSpec()) {
      final List<Section> children = section.getChildren();
      for (int i = 0, size = children.size(); i < size; i++) {
        bindNewComponent(children.get(i));
      }
    }
  }

  private void unbindOldComponent(Section section) {
    section.unbindService(section.getScopedContext());

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

  @UiThread
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

    if (mFocusDispatcher.isLoadingCompleted()) {
      mFocusDispatcher.waitForDataBound(false);
      mFocusDispatcher.maybeDispatchFocusRequests();
    }
  }

  private static ChangeSetState calculateNewChangeSet(
      SectionContext context,
      Section currentRoot,
      Section nextRoot,
      Map<String, List<StateUpdate>> pendingStateUpdates,
      SectionsDebugLogger sectionsDebugLogger,
      String sectionTreeTag) {
    nextRoot.setGlobalKey(nextRoot.getKey());

    final ComponentsLogger logger = context.getLogger();
    LogEvent logEvent = null;
    if (logger != null) {
      logEvent =
          SectionsLogEventUtils.getSectionsPerformanceEvent(
              logger, context.getLogTag(), EVENT_SECTIONS_CREATE_NEW_TREE, currentRoot, nextRoot);
    }

    createNewTreeAndApplyStateUpdates(
        context, currentRoot, nextRoot, pendingStateUpdates, sectionsDebugLogger, sectionTreeTag);

    if (logger != null) {
      logger.log(logEvent);
    }

    return ChangeSetState.generateChangeSet(
        context, currentRoot, nextRoot, sectionsDebugLogger, sectionTreeTag, "", "");
  }

  /**
   * Creates the new tree, transfers state/services from the current tree and applies all the state
   * updates that have been enqueued since the last tree calculation.
   */
  private static void createNewTreeAndApplyStateUpdates(
      SectionContext context,
      Section currentRoot,
      Section nextRoot,
      Map<String, List<StateUpdate>> pendingStateUpdates,
      SectionsDebugLogger sectionsDebugLogger,
      String sectionTreeTag) {
    if (nextRoot == null) {
      throw new IllegalStateException("Can't generate a subtree with a null root");
    }

    nextRoot.setScopedContext(SectionContext.withScope(context, nextRoot));
    if (currentRoot != null) {
      nextRoot.setCount(currentRoot.getCount());
    }

    final boolean shouldTransferState =
        currentRoot != null && currentRoot.getClass().equals(nextRoot.getClass());

    if (currentRoot == null || !shouldTransferState) {
      nextRoot.createInitialState(nextRoot.getScopedContext());
      nextRoot.createService(nextRoot.getScopedContext());
    } else {
      nextRoot.transferService(nextRoot.getScopedContext(), currentRoot, nextRoot);
      nextRoot.transferState(
          nextRoot.getScopedContext(),
          currentRoot.getStateContainer());
    }

    // TODO: t18544409 Make sure this is absolutely the best solution as this is an anti-pattern
    final List<StateUpdate> stateUpdates = pendingStateUpdates.get(nextRoot.getGlobalKey());
    if (stateUpdates != null) {
      for (int i = 0, size = stateUpdates.size(); i < size; i++) {
        stateUpdates.get(i).updateState(nextRoot.getStateContainer(), nextRoot);
      }

      if (nextRoot.shouldComponentUpdate(currentRoot, nextRoot)) {
        nextRoot.invalidate();
      }
    }

    if (!nextRoot.isDiffSectionSpec()) {
      final Map<String, Pair<Section, Integer>> currentComponentChildren = currentRoot == null ?
          null :
          Section.acquireChildrenMap(currentRoot);

      final TreeProps parentTreeProps = context.getTreeProps();
      nextRoot.populateTreeProps(parentTreeProps);
      context.setTreeProps(
          nextRoot.getTreePropsForChildren(context, parentTreeProps));

      final ComponentsLogger logger = context.getLogger();
      LogEvent logEvent = null;
      if (logger != null) {
        logEvent =
            SectionsLogEventUtils.getSectionsPerformanceEvent(
                logger, context.getLogTag(), EVENT_SECTIONS_ON_CREATE_CHILDREN, null, nextRoot);
      }

      nextRoot.setChildren(nextRoot.createChildren(
          nextRoot.getScopedContext()));

      if (logger != null) {
        logger.log(logEvent);
      }

      final List<Section> nextRootChildren = nextRoot.getChildren();

      for (int i = 0, size = nextRootChildren.size(); i < size; i++) {
        final Section child = nextRootChildren.get(i);
        child.setParent(nextRoot);
        final String childKey = child.getKey();
        if (TextUtils.isEmpty(childKey)) {
          final String errorMessage =
              "Your Section "
                  + child.getClass().getSimpleName()
                  + " has an empty key. Please specify a key.";
          throw new IllegalStateException(errorMessage);
        }

        final String globalKey = nextRoot.getGlobalKey() + childKey;
        child.generateKeyAndSet(nextRoot.getScopedContext(), globalKey);
        child.setScopedContext(SectionContext.withScope(context, child));

        final Pair<Section,Integer> valueAndIndex = currentComponentChildren == null ?
            null :
            currentComponentChildren.get(child.getGlobalKey());
        final Section currentChild = valueAndIndex != null ? valueAndIndex.first : null;

        createNewTreeAndApplyStateUpdates(
            context, currentChild, child, pendingStateUpdates, sectionsDebugLogger, sectionTreeTag);
      }

      final TreeProps contextTreeProps = context.getTreeProps();
      if (contextTreeProps != parentTreeProps) {
        if (contextTreeProps != null) {
          ComponentsPools.release(contextTreeProps);
        }
        context.setTreeProps(parentTreeProps);
      }
    }
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

  private static List<StateUpdate> acquireUpdatesList() {
    //TODO use pools t11953296
    return new ArrayList<>();
  }

  private static void releaseUpdatesList(List<StateUpdate> stateUpdates) {
    //TODO use pools t11953296
  }

  private static class SectionsMainThreadHandler extends Handler {

    private SectionsMainThreadHandler() {
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
            throw new RuntimeException(getDebugInfo(tree) + e.getMessage(), e);
          }
          break;
        case MESSAGE_FOCUS_REQUEST:
        case MESSAGE_FOCUS_DISPATCHER_LOADING_STATE_UPDATE:
          Runnable focusRequest = (Runnable) msg.obj;
          focusRequest.run();
          break;
        default:
          throw new IllegalArgumentException();
      }
    }
  }

  @GuardedBy("tree")
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
    sb.append(tree.mPendingStateUpdates.mAllStateUpdates.size());

    sb.append(", pendingNonLazyStateUpdates.size: ");
    sb.append(tree.mPendingStateUpdates.mNonLazyStateUpdates.size());
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
    private boolean mAsyncPropUpdates;
    private String mTag;
    private Handler mChangeSetThreadHandler;
    private boolean mForceSyncStateUpdates;

    private Builder(SectionContext componentContext, Target target) {
      mContext = componentContext;
      mTarget = target;
      mAsyncStateUpdates = SectionsConfiguration.sectionComponentsAsyncStateUpdates;
      mAsyncPropUpdates = SectionsConfiguration.sectionComponentsAsyncPropUpdates;
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
     */
    public Builder asyncStateUpdates(boolean asyncStateUpdates) {
      mAsyncStateUpdates = asyncStateUpdates;
      return this;
    }

    /**
     * If enabled, all state updates will be performed on the main thread. This may be necessary to
     * use if your data model is not thread safe (e.g. model objects can be mutated from another
     * thread), meaning changesets on the datamodel list cannot be computed in the background.
     *
     * <p>NB: This may come at significant performance cost.
     */
    public Builder forceSyncStateUpdates(boolean forceSyncStateUpdates) {
      mForceSyncStateUpdates = forceSyncStateUpdates;
      return this;
    }

    /**
     * If enabled, any prop updates done with setRoot will be on a background thread. This excludes
     * the very first setRoot where there isn't an existing section. That one will still be on the
     * main thread.
     */
    public Builder asyncPropUpdates(boolean asyncPropUpdates) {
      mAsyncPropUpdates = asyncPropUpdates;
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

  /**
   * A class to track state updates. It keeps track of both all state updates, and all non-lazy
   * state updates. We're interested in non-lazy state updates in particular since they are the ones
   * we need in order to determine whether we need to execute another state update or not.
   */
  static class StateUpdatesHolder {
    private Map<String, List<StateUpdate>> mAllStateUpdates;
    private Map<String, List<StateUpdate>> mNonLazyStateUpdates;

    StateUpdatesHolder() {
      mAllStateUpdates = new HashMap<>();
      mNonLazyStateUpdates = new HashMap<>();
    }

    void release() {
      mAllStateUpdates.clear();
      mNonLazyStateUpdates.clear();
    }
  }
}
