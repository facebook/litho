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

package com.facebook.litho;

import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.ThreadSafe;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Controller which makes sure EventHandler instances are bound to the latest instance of the
 * Component/Section that created them.
 *
 * <p>To see the problem this is trying to solve, consider a Section with a button Component with a
 * click handler as one of its rows. When the section updates (e.g. performs a state update),
 * nothing about how the button renders changes, so we don't want to re-resolve it. However, if we
 * don't re-resolve it, the click handler points to an old instance of the Section (the Section
 * before the state update).
 *
 * <p>To get the best of both worlds, we don't re-render the button and use this class to update the
 * EventDispatchInfo on all EventHandlers created in this tree to their latest Component/Section
 * owners.
 *
 * <p>In order to do this, all EventHandlers created by a given component/section (identified by
 * their global key) is given a reference to the same EventDispatchInfo instance. Then on each tree
 * resolution, we update this EventDispatchInfo with the latest instance of the context and
 * component/section.
 *
 * <p>## Details of the full flow in the context of Components
 * <li>- During a resolve or layout (or resolve+layout), we record all Spec-generated EventHandlers
 *     in a list local to this computation.
 * <li>- When we create EventHandlers, we create them with the latest ComponentContext and
 *     EventDispatcher (the two together are what we call DispatchInfo) but don't update any
 *     existing committed EventHandlers.
 * <li>- The ResolveResult has just the EventHandlers from resolve, but the LayoutState (result of
 *     layout computation) has both the EventHandlers created in resolve and the EventHandlers
 *     created in layout
 * <li>- If we commit this LayoutState, we 'canonicalize' the DispatchInfos on the EventHandlers we
 *     created. This means we update the collected EventHandler's DispatchInfos to be the
 *     'canonical' DispatchInfos we already have for that global key (if we don't have one, that
 *     DispatchInfo becomes the canonical DispatchInfo for that key)
 * <li>- Finally, we update the canonical DispatchInfos for each global key to the latest
 *     ComponentContext/EventDispatcher from the newly committed LayoutState.
 *
 *     <p>By having all committed EventHandlers for a global key reference the same DispatchInfo, we
 *     can update them all at the same time when a new LayoutState is committed.
 */
@ThreadSafe
@Nullsafe(Nullsafe.Mode.LOCAL)
public class EventHandlersController {

  private final Map<String, DispatchInfoWrapper> mDispatchInfos = new HashMap<>();

  /**
   * Updates the EventDispatchInfo for this global key with the latest context and
   * component/section. All EventHandlers created by this global key have the same EventDispatchInfo
   * instance.
   */
  public synchronized void updateEventDispatchInfoForGlobalKey(
      ComponentContext c, HasEventDispatcher dispatcher, @Nullable String globalKey) {
    if (globalKey == null) {
      return;
    }

    final DispatchInfoWrapper dispatchInfoWrapper = mDispatchInfos.get(globalKey);

    if (dispatchInfoWrapper == null) {
      return;
    }

    // Mark that the list of event handlers for this component is still needed.
    dispatchInfoWrapper.mUsedInCurrentTree = true;

    // Set the latest dispatcher and context for all EventHandlers which reference this
    // EventDispatchInfo
    dispatchInfoWrapper.mDispatchInfo.hasEventDispatcher = dispatcher;
    dispatchInfoWrapper.mDispatchInfo.componentContext = c;
  }

  /**
   * If a global key doesn't appear in the current tree, remove its EventDispatchInfo from our book
   * keeping.
   */
  public synchronized void clearUnusedEventDispatchInfos() {
    final Iterator<Map.Entry<String, DispatchInfoWrapper>> iterator =
        mDispatchInfos.entrySet().iterator();
    while (iterator.hasNext()) {
      final Map.Entry<String, DispatchInfoWrapper> entry = iterator.next();
      final DispatchInfoWrapper dispatchInfoWrapper = entry.getValue();
      if (!dispatchInfoWrapper.mUsedInCurrentTree) {
        iterator.remove();
      } else {
        dispatchInfoWrapper.mUsedInCurrentTree = false;
      }
    }
  }

  /**
   * For a list of Pair(GlobalKey, EventHandler), updates the EventDispatchInfo on each EventHandler
   * to be the canonical EventDispatchInfos for the corresponding global key. This means that from
   * now on, when EventDispatchInfos are re-bound in {@link #updateEventDispatchInfoForGlobalKey},
   * these EventHandlers will also be updated.
   */
  public synchronized void canonicalizeEventDispatchInfos(
      List<Pair<String, EventHandler>> eventHandlers) {
    for (Pair<String, EventHandler> handlerEntry : eventHandlers) {
      final String globalKey = handlerEntry.first;
      final EventHandler eventHandler = handlerEntry.second;

      DispatchInfoWrapper existingDispatchInfo = mDispatchInfos.get(globalKey);
      if (existingDispatchInfo == null) {
        existingDispatchInfo = new DispatchInfoWrapper(eventHandler.dispatchInfo);
        mDispatchInfos.put(globalKey, existingDispatchInfo);
      } else {
        eventHandler.dispatchInfo = existingDispatchInfo.mDispatchInfo;
      }
    }
  }

  @VisibleForTesting
  public synchronized Map<String, DispatchInfoWrapper> getDispatchInfos() {
    return mDispatchInfos;
  }

  @VisibleForTesting
  public static class DispatchInfoWrapper {

    final EventDispatchInfo mDispatchInfo;
    boolean mUsedInCurrentTree;

    public DispatchInfoWrapper(EventDispatchInfo dispatchInfo) {
      mDispatchInfo = dispatchInfo;
    }
  }
}
