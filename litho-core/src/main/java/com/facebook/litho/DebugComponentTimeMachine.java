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

package com.facebook.litho;

import androidx.annotation.UiThread;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * This class provides the means to save and restore ComponentTrees for time-traveling debugging.
 */
public final class DebugComponentTimeMachine {

  private DebugComponentTimeMachine() {}

  // TODO(festevezga, T77766921) - Use LayoutState.CalculateLayoutSource for overloads too
  private static final HashSet<Integer> sShouldSkipNextSnapshot = new HashSet<>();

  /**
   * Finds the timeline corresponding to a component
   *
   * @param component the component the timeline is for
   * @return the timeline or null
   */
  public static @Nullable TreeRevisions getTimeline(DebugComponent component) {
    return component.getContext().getComponentTree().getTimeline();
  }

  /**
   * Caches a StateHandler and Component root alongside metadata for them
   *
   * @param componentTree the ComponentTree we want to save the snapshot for
   * @param root the component to save the timeline for
   * @param stateHandler the state handler for the context in the root
   * @param props the props of the component root
   * @param source the cause of the redraw that will be saved
   * @param attribution the context of the cause of the redraw
   * @return whether a snapshot was saved
   */
  public static boolean saveTimelineSnapshot(
      ComponentTree componentTree,
      Component root,
      StateHandler stateHandler,
      TreeProps props,
      @LayoutState.CalculateLayoutSource int source,
      @Nullable String attribution) {
    if (sShouldSkipNextSnapshot.remove(componentTree.mId)
        || source == LayoutState.CalculateLayoutSource.RELOAD_PREVIOUS_STATE) {
      return false;
    }

    final StateHandler frozenStateHandler = new StateHandler(stateHandler);
    componentTree.appendTimeline(root, frozenStateHandler, props, source, attribution);

    return true;
  }

  /**
   * Applies a StateHandler to a Component node from the cache
   *
   * @param node the component the timeline is for
   * @param destination the TreeRevision#key at the timeline we'll be traveling to
   * @return whether the snapshot was loaded
   */
  @UiThread
  public static boolean loadTimelineSnapshot(DebugComponent node, String destination) {
    ThreadUtils.assertMainThread();
    final ComponentTree oldTree = node.getContext().getComponentTree();
    final TreeRevisions treeRevisions = oldTree.getTimeline();
    if (treeRevisions != null) {
      final TreeRevision selected = treeRevisions.findByKey(destination);
      if (selected != null) {
        oldTree.resetState(
            selected.revisionNumber, selected.root, selected.props, selected.handler);
      }
      return true;
    }

    return false;
  }

  /**
   * Prevents the next call to saveTimelineSnapshot for this ComponentTree
   *
   * @param tree the component tree the timeline is for
   */
  public static void skipNextSnapshot(ComponentTree tree) {
    sShouldSkipNextSnapshot.add(tree.mId);
  }

  /**
   * Prevents the next call to saveTimelineSnapshot for this DebugComponent
   *
   * @param node the component the timeline is for
   * @return false if the node failed to get its ComponentTree
   */
  public static boolean maybeSkipNextSnapshot(DebugComponent node) {
    final LithoView lithoView = node.getLithoView();
    if (lithoView == null) {
      return false;
    }
    final ComponentTree componentTree = lithoView.getComponentTree();
    if (componentTree == null) {
      return false;
    }
    skipNextSnapshot(componentTree);
    return true;
  }

  public static final class TreeRevision {

    public static final DateFormat REVISION_DATE_FORMAT =
        new SimpleDateFormat("hh:mm:ss.SSS", Locale.getDefault());

    public final Component root;
    public final StateHandler handler;
    public final TreeProps props;
    public final long revisionMoment;
    public final long revisionNumber;
    public final @LayoutState.CalculateLayoutSource int source;
    public final @Nullable String attribution;
    public final String key;

    private TreeRevision(
        Component root,
        StateHandler handler,
        TreeProps props,
        long revisionMoment,
        long revisionNumber,
        @LayoutState.CalculateLayoutSource int source,
        @Nullable String attribution) {
      this.root = root;
      this.handler = handler;
      this.props = props;
      this.revisionMoment = revisionMoment;
      this.revisionNumber = revisionNumber;
      this.source = source;
      this.attribution = attribution;
      this.key = String.format("%d%d", revisionNumber, revisionMoment);
    }

    public String getKey() {
      return key;
    }
  }

  public static final class TreeRevisions {
    private long selected;
    public final List<TreeRevision> revisions = new ArrayList<>();
    public final String rootName;

    TreeRevisions(long selected, List<TreeRevision> revisions, String rootName) {
      this.selected = selected;
      this.revisions.addAll(revisions);
      this.rootName = rootName;
    }

    TreeRevisions(
        final Component root,
        final StateHandler handler,
        final TreeProps props,
        final @LayoutState.CalculateLayoutSource int source,
        final @Nullable String attribution) {
      rootName = root.getSimpleName() + " key=" + root.getGlobalKey();
      internalAdd(
          new TreeRevision(
              root, handler, props, System.currentTimeMillis(), 0, source, attribution));
    }

    public TreeRevisions shallowCopy() {
      return new TreeRevisions(selected, revisions, rootName);
    }

    private void internalAdd(TreeRevision revision) {
      revisions.add(revision);
      this.selected = revision.revisionNumber;
    }

    void setLatest(
        final Component root,
        final StateHandler handler,
        final TreeProps props,
        final @LayoutState.CalculateLayoutSource int source,
        final @Nullable String attribution) {
      long nextRevision = revisions.get(revisions.size() - 1).revisionNumber + 1;
      final TreeRevision revision =
          new TreeRevision(
              root, handler, props, System.currentTimeMillis(), nextRevision, source, attribution);
      internalAdd(revision);
    }

    void setSelected(long selected) {
      this.selected = selected;
    }

    public TreeRevision getSelected() {
      for (TreeRevision rev : revisions) {
        if (rev.revisionNumber == selected) {
          return rev;
        }
      }
      throw new IllegalStateException();
    }

    public @Nullable TreeRevision findByKey(String pick) {
      for (TreeRevision rev : revisions) {
        if (rev.getKey().equals(pick)) {
          return rev;
        }
      }
      return null;
    }
  }
}
