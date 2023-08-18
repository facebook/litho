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

package com.facebook.rendercore;

import android.graphics.Rect;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LongSparseArray;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.GapWorkerCallbacks;
import com.facebook.rendercore.extensions.InformsMountCallback;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.OnItemCallbacks;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.extensions.VisibleBoundsCallbacks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Can be passed to a MountState to override default mounting behaviour and control which items get
 * mounted or unmounted.
 */
public class MountDelegate {

  private final LongSparseArray<Integer> mReferenceCountMap = new LongSparseArray<>();
  private final MountDelegateTarget mMountDelegateTarget;
  private final List<ExtensionState> mExtensionStates = new ArrayList<>();
  private @Nullable ExtensionState mUnmountDelegateExtensionState;
  private boolean mReferenceCountingEnabled = false;
  private boolean mCollectVisibleBoundsChangedCalls = false;
  private int mNotifyVisibleBoundsChangedNestCount = 0;
  private final Set<Object> mNotifyVisibleBoundsChangedItems = new HashSet<>();
  private final Systracer mTracer;
  private @Nullable RenderTreeUpdateListener mMountStateListener;

  public MountDelegate(MountDelegateTarget mountDelegateTarget, Systracer tracer) {
    mMountDelegateTarget = mountDelegateTarget;
    mTracer = tracer;
  }

  public void setCollectVisibleBoundsChangedCalls(boolean value) {
    mCollectVisibleBoundsChangedCalls = value;
  }

  public void registerExtensions(
      @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> extensions) {
    mExtensionStates.clear();
    if (extensions != null) {
      for (Pair<RenderCoreExtension<?, ?>, Object> e : extensions) {
        final MountExtension<?, ?> extension = e.first.getMountExtension();
        if (extension != null) {
          final ExtensionState<?> extensionState = extension.createExtensionState(this);

          if (extension instanceof UnmountDelegateExtension) {
            mMountDelegateTarget.setUnmountDelegateExtension((UnmountDelegateExtension) extension);
            mUnmountDelegateExtensionState = extensionState;
          }

          mReferenceCountingEnabled =
              mReferenceCountingEnabled
                  || (extension instanceof InformsMountCallback
                      && ((InformsMountCallback) extension).canPreventMount());

          mExtensionStates.add(extensionState);
        }
      }
    }
  }

  /**
   * @param extension
   * @deprecated Only used for Litho's integration. Marked for removal.
   */
  @Deprecated
  public ExtensionState registerMountExtension(MountExtension extension) {
    final ExtensionState extensionState = extension.createExtensionState(this);
    if (extension instanceof UnmountDelegateExtension) {
      mMountDelegateTarget.setUnmountDelegateExtension((UnmountDelegateExtension) extension);
      mUnmountDelegateExtensionState = extensionState;
    }

    mReferenceCountingEnabled =
        mReferenceCountingEnabled
            || (extension instanceof InformsMountCallback
                && ((InformsMountCallback) extension).canPreventMount());

    mExtensionStates.add(extensionState);

    return extensionState;
  }

  /**
   * @param toRemove {@link MountExtension} to remove.
   * @deprecated Only used for Litho's integration. Marked for removal.
   */
  @Deprecated
  public void unregisterMountExtension(MountExtension toRemove) {
    MountExtension mountExtension = null;
    Iterator<ExtensionState> iter = mExtensionStates.iterator();
    while (iter.hasNext()) {
      final MountExtension extension = iter.next().getExtension();
      if (extension == toRemove) {
        mountExtension = extension;
        iter.remove();
        break;
      }
    }

    if (mountExtension instanceof UnmountDelegateExtension) {
      mMountDelegateTarget.removeUnmountDelegateExtension();
      mUnmountDelegateExtensionState = null;
    }

    if (mountExtension == null) {
      throw new IllegalStateException("Could not find the extension " + toRemove);
    }

    if (mountExtension instanceof InformsMountCallback
        && ((InformsMountCallback) mountExtension).canPreventMount()) {
      updateRefCountEnabled();
    }
  }

  public void unregisterAllExtensions() {
    mMountDelegateTarget.removeUnmountDelegateExtension();
    mUnmountDelegateExtensionState = null;
    mExtensionStates.clear();
    mReferenceCountingEnabled = false;
  }

  public void onRegisterForPremount(@Nullable Long frameTimeMs) {
    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      MountExtension extension = mExtensionStates.get(i).getExtension();
      if (extension instanceof GapWorkerCallbacks) {
        ((GapWorkerCallbacks) extension)
            .onRegisterForPremount(mExtensionStates.get(i), frameTimeMs);
      }
    }
  }

  public void onUnregisterForPremount() {
    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      MountExtension extension = mExtensionStates.get(i).getExtension();
      if (extension instanceof GapWorkerCallbacks) {
        ((GapWorkerCallbacks) extension).onUnregisterForPremount(mExtensionStates.get(i));
      }
    }
  }

  /**
   * Calls {@link MountExtension#beforeMount(ExtensionState, Object, Rect)} for each {@link
   * RenderCoreExtension} that has a mount phase.
   *
   * @param results A map of {@link RenderCoreExtension} to their results from the layout phase.
   */
  public void beforeMount(
      final List<Pair<RenderCoreExtension<?, ?>, Object>> results, final Rect rect) {
    int i = 0; // Use an int to get the index of the extensions state.
    for (Pair<RenderCoreExtension<?, ?>, Object> entry : results) {
      final Object input = entry.second;
      final MountExtension extension = entry.first.getMountExtension();
      if (extension != null) {
        final ExtensionState current = mExtensionStates.get(i);
        if (current.getExtension() != extension) {
          throw new IllegalStateException(
              String.format(
                  "state for %s was not found at expected index %d. Found %s at index instead.",
                  entry.first, i, current.getExtension()));
        }
        extension.beforeMount(current, input, rect);
        i++;
      }
    }
  }

  public void afterMount() {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      mExtensionStates.get(i).afterMount();
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void notifyVisibleBoundsChanged(Rect rect) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      ExtensionState state = mExtensionStates.get(i);
      MountExtension extension = state.getExtension();
      if (extension instanceof VisibleBoundsCallbacks) {
        ((VisibleBoundsCallbacks) extension).onVisibleBoundsChanged(state, rect);
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void notifyVisibleBoundsChangedForItem(Object item) {
    if (!mCollectVisibleBoundsChangedCalls) {
      RenderCoreExtension.recursivelyNotifyVisibleBoundsChanged(item, mTracer);
      return;
    }

    mNotifyVisibleBoundsChangedItems.add(item);
  }

  public void startNotifyVisibleBoundsChangedSection() {
    if (!mCollectVisibleBoundsChangedCalls) {
      return;
    }

    mNotifyVisibleBoundsChangedNestCount++;
  }

  public void endNotifyVisibleBoundsChangedSection() {
    if (!mCollectVisibleBoundsChangedCalls) {
      return;
    }

    mNotifyVisibleBoundsChangedNestCount--;
    if (mNotifyVisibleBoundsChangedNestCount < 0) {
      throw new RuntimeException(
          "mNotifyVisibleBoundsChangedNestCount should not be decremented below zero!");
    }

    if (mNotifyVisibleBoundsChangedNestCount == 0) {
      for (Object item : mNotifyVisibleBoundsChangedItems) {
        RenderCoreExtension.recursivelyNotifyVisibleBoundsChanged(item, mTracer);
      }

      mNotifyVisibleBoundsChangedItems.clear();
    }
  }

  private void updateRefCountEnabled() {
    mReferenceCountingEnabled = false;
    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      MountExtension extension = mExtensionStates.get(i).getExtension();
      if (extension instanceof InformsMountCallback) {
        mReferenceCountingEnabled = ((InformsMountCallback) extension).canPreventMount();
      }
      if (mReferenceCountingEnabled) {
        return;
      }
    }
  }

  void unBind() {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      mExtensionStates.get(i).onUnbind();
    }

    endNotifyVisibleBoundsChangedSection();
  }

  void unMount() {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      mExtensionStates.get(i).onUnmount();
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onBindItem(
      final RenderUnit renderUnit,
      final Object content,
      final Object layoutData,
      final Systracer tracer) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      ExtensionState state = mExtensionStates.get(i);
      MountExtension extension = state.getExtension();
      if (extension instanceof OnItemCallbacks) {
        final boolean isTracing = tracer.isTracing();
        if (isTracing) {
          tracer.beginSection("Extension:onBindItem " + extension.getName());
        }
        ((OnItemCallbacks) extension).onBindItem(state, renderUnit, content, layoutData);
        if (isTracing) {
          tracer.endSection();
        }
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onUnbindItem(
      final RenderUnit renderUnit,
      final Object content,
      final Object layoutData,
      final Systracer tracer) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      ExtensionState state = mExtensionStates.get(i);
      MountExtension extension = state.getExtension();
      if (extension instanceof OnItemCallbacks) {
        final boolean isTracing = tracer.isTracing();
        if (isTracing) {
          tracer.beginSection("Extension:onUnbindItem " + extension.getName());
        }
        ((OnItemCallbacks) extension).onUnbindItem(state, renderUnit, content, layoutData);
        if (isTracing) {
          tracer.endSection();
        }
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  /**
   * Collects all the {@link MountExtension} which need a callback to their mount and bind item
   * methods for {@param nextRenderUnit}. This method returns the list of those extensions.
   */
  @Nullable
  List<ExtensionState> collateExtensionsToUpdate(
      final @Nullable RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final @Nullable RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData,
      final Systracer tracer) {
    List<ExtensionState> extensionStatesToUpdate = null;
    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      final ExtensionState state = mExtensionStates.get(i);
      final MountExtension extension = state.getExtension();
      if (extension instanceof OnItemCallbacks) {
        final boolean shouldUpdate;
        final boolean isTracing = tracer.isTracing();
        if (isTracing) {
          tracer.beginSection("Extension:shouldUpdateItem " + extension.getName());
        }
        shouldUpdate =
            ((OnItemCallbacks) extension)
                .shouldUpdateItem(
                    state, previousRenderUnit, previousLayoutData, nextRenderUnit, nextLayoutData);
        if (isTracing) {
          tracer.endSection();
        }
        if (shouldUpdate) {
          if (extensionStatesToUpdate == null) {
            extensionStatesToUpdate = new ArrayList<>(mExtensionStates.size());
          }
          extensionStatesToUpdate.add(state);
        }
      }
    }

    return extensionStatesToUpdate;
  }

  static void onUnbindItemWhichRequiresUpdate(
      final List<ExtensionState> extensionStatesToUpdate,
      final @Nullable RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final @Nullable RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData,
      final Object content,
      final Systracer tracer) {
    if (!extensionStatesToUpdate.isEmpty()) {
      final int size = extensionStatesToUpdate.size();
      for (int i = 0; i < size; i++) {
        ExtensionState state = extensionStatesToUpdate.get(i);
        MountExtension extension = state.getExtension();
        if (extension instanceof OnItemCallbacks) {
          final boolean isTracing = tracer.isTracing();
          if (isTracing) {
            tracer.beginSection("Extension:onUnbindItem " + extension.getName());
          }
          ((OnItemCallbacks) extension)
              .onUnbindItem(state, previousRenderUnit, content, previousLayoutData);
          if (isTracing) {
            tracer.endSection();
          }
        }
      }
    }
  }

  static void onUnmountItemWhichRequiresUpdate(
      final List<ExtensionState> extensionStatesToUpdate,
      final @Nullable RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final @Nullable RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData,
      final Object content,
      final Systracer tracer) {
    if (!extensionStatesToUpdate.isEmpty()) {
      final int size = extensionStatesToUpdate.size();
      for (int i = 0; i < size; i++) {
        ExtensionState state = extensionStatesToUpdate.get(i);
        MountExtension extension = state.getExtension();
        if (extension instanceof OnItemCallbacks) {
          final boolean isTracing = tracer.isTracing();
          if (isTracing) {
            tracer.beginSection("Extension:onUnmountItem " + extension.getName());
          }
          ((OnItemCallbacks) extension)
              .onUnmountItem(state, previousRenderUnit, content, previousLayoutData);
          if (isTracing) {
            tracer.endSection();
          }
        }
      }
    }
  }

  static void onMountItemWhichRequiresUpdate(
      final List<ExtensionState> extensionStatesToUpdate,
      final @Nullable RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final @Nullable RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData,
      final Object content,
      final Systracer tracer) {
    if (!extensionStatesToUpdate.isEmpty()) {
      final int size = extensionStatesToUpdate.size();
      for (int i = 0; i < size; i++) {
        final ExtensionState state = extensionStatesToUpdate.get(i);
        final MountExtension extension = state.getExtension();
        if (extension instanceof OnItemCallbacks) {
          final boolean isTracing = tracer.isTracing();
          if (isTracing) {
            tracer.beginSection("Extension:onMountItem " + extension.getName());
          }
          ((OnItemCallbacks) extension).onMountItem(state, nextRenderUnit, content, nextLayoutData);
          if (isTracing) {
            tracer.endSection();
          }
        }
      }
    }
  }

  static void onBindItemWhichRequiresUpdate(
      final List<ExtensionState> extensionStatesToUpdate,
      final @Nullable RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final @Nullable RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData,
      final Object content,
      final Systracer tracer) {
    if (!extensionStatesToUpdate.isEmpty()) {
      final int size = extensionStatesToUpdate.size();
      for (int i = 0; i < size; i++) {
        ExtensionState state = extensionStatesToUpdate.get(i);
        MountExtension extension = state.getExtension();
        if (extension instanceof OnItemCallbacks) {
          final boolean isTracing = tracer.isTracing();
          if (isTracing) {
            tracer.beginSection("Extension:onBindItem " + extension.getName());
          }
          ((OnItemCallbacks) extension).onBindItem(state, nextRenderUnit, content, nextLayoutData);
          if (isTracing) {
            tracer.endSection();
          }
        }
      }
    }
  }

  public void onMountItem(
      final RenderUnit renderUnit,
      final Object content,
      final Object layoutData,
      final Systracer tracer) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      final ExtensionState state = mExtensionStates.get(i);
      final MountExtension extension = state.getExtension();
      if (extension instanceof OnItemCallbacks) {
        final boolean isTracing = tracer.isTracing();
        if (isTracing) {
          tracer.beginSection("Extension:onMountItem " + extension.getName());
        }
        ((OnItemCallbacks) extension).onMountItem(state, renderUnit, content, layoutData);
        if (isTracing) {
          tracer.endSection();
        }
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onUnmountItem(
      final RenderUnit renderUnit,
      final Object content,
      final @Nullable Object layoutData,
      final Systracer tracer) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      ExtensionState state = mExtensionStates.get(i);
      MountExtension extension = state.getExtension();
      if (extension instanceof OnItemCallbacks) {
        final boolean isTracing = tracer.isTracing();
        if (isTracing) {
          tracer.beginSection("Extension:onUnmountItem " + extension.getName());
        }
        ((OnItemCallbacks) extension).onUnmountItem(state, renderUnit, content, layoutData);
        if (isTracing) {
          tracer.endSection();
        }
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  public void onBoundsAppliedToItem(RenderTreeNode node, Object content, Systracer tracer) {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      ExtensionState state = mExtensionStates.get(i);
      MountExtension extension = state.getExtension();
      if (extension instanceof OnItemCallbacks) {
        final boolean isTracing = tracer.isTracing();
        if (isTracing) {
          tracer.beginSection("Extension:onBoundsAppliedToItem " + extension.getName());
        }
        ((OnItemCallbacks) extension)
            .onBoundsAppliedToItem(state, node.getRenderUnit(), content, node.getLayoutData());
        if (isTracing) {
          tracer.endSection();
        }
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  @Nullable
  public ExtensionState getUnmountDelegateExtensionState() {
    return mUnmountDelegateExtensionState;
  }

  public Object getContentAt(int position) {
    return mMountDelegateTarget.getContentAt(position);
  }

  public @Nullable Object getContentById(long id) {
    return mMountDelegateTarget.getContentById(id);
  }

  public boolean isRootItem(int position) {
    return mMountDelegateTarget.isRootItem(position);
  }

  /** @return true if this item needs to be mounted. */
  public boolean maybeLockForMount(RenderTreeNode renderTreeNode, int index) {
    if (!mReferenceCountingEnabled) {
      return true;
    }

    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      MountExtension extension = mExtensionStates.get(i).getExtension();
      if (extension instanceof OnItemCallbacks) {
        ((OnItemCallbacks) extension)
            .beforeMountItem(mExtensionStates.get(i), renderTreeNode, index);
      }
    }

    endNotifyVisibleBoundsChangedSection();

    return hasAcquiredRef(renderTreeNode.getRenderUnit().getId());
  }

  public boolean isLockedForMount(RenderTreeNode renderTreeNode) {
    return isLockedForMount(renderTreeNode.getRenderUnit().getId());
  }

  public boolean isLockedForMount(long id) {
    if (!mReferenceCountingEnabled) {
      return true;
    }

    return hasAcquiredRef(id);
  }

  private boolean hasAcquiredRef(long renderUnitId) {
    final Integer refCount = mReferenceCountMap.get(renderUnitId);

    return refCount != null && refCount > 0;
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void acquireMountRef(final RenderTreeNode node) {
    acquireMountRef(node.getRenderUnit().getId());
  }

  public void acquireMountRef(final long id) {
    incrementExtensionRefCount(id);
  }

  public void acquireAndMountRef(final RenderTreeNode node) {
    acquireAndMountRef(node.getRenderUnit().getId());
  }

  public void acquireAndMountRef(final long id) {
    acquireMountRef(id);

    // Only mount if we're during a mounting phase, otherwise the mounting phase will take care of
    // that.
    mMountDelegateTarget.notifyMount(id);
  }

  public boolean hasItemToMount() {
    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      MountExtension extension = mExtensionStates.get(i).getExtension();
      if (extension instanceof GapWorkerCallbacks
          && ((GapWorkerCallbacks) extension).hasItemToMount(mExtensionStates.get(i))) {
        return true;
      }
    }
    return false;
  }

  public void premountNext() {
    startNotifyVisibleBoundsChangedSection();

    for (int i = 0, size = mExtensionStates.size(); i < size; i++) {
      MountExtension extension = mExtensionStates.get(i).getExtension();
      if (extension instanceof GapWorkerCallbacks) {
        ((GapWorkerCallbacks) extension).premountNext(mExtensionStates.get(i));
      }
    }

    endNotifyVisibleBoundsChangedSection();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public void releaseMountRef(final RenderTreeNode renderTreeNode) {
    releaseMountRef(renderTreeNode.getRenderUnit().getId());
  }

  public void releaseMountRef(final long id) {
    decrementExtensionRefCount(id);
  }

  public void releaseAndUnmountRef(final RenderTreeNode renderTreeNode) {
    releaseAndUnmountRef(renderTreeNode.getRenderUnit().getId());
  }

  public void releaseAndUnmountRef(final long id) {
    final boolean wasLockedForMount = isLockedForMount(id);
    releaseMountRef(id);

    if (wasLockedForMount && !isLockedForMount(id)) {
      mMountDelegateTarget.notifyUnmount(id);
    }
  }

  public void releaseAllAcquiredReferences() {
    if (!mReferenceCountingEnabled) {
      return;
    }

    for (ExtensionState extension : mExtensionStates) {
      extension.releaseAllAcquiredReferences();
    }

    mReferenceCountMap.clear();
  }

  private void incrementExtensionRefCount(long renderUnitId) {
    if (!mReferenceCountingEnabled) {
      return;
    }

    Integer refCount = mReferenceCountMap.get(renderUnitId);

    if (refCount == null) {
      refCount = 0;
    }

    mReferenceCountMap.put(renderUnitId, refCount + 1);
  }

  private void decrementExtensionRefCount(final long renderUnitId) {
    if (!mReferenceCountingEnabled) {
      return;
    }

    Integer refCount = mReferenceCountMap.get(renderUnitId);

    if (refCount == null || refCount == 0) {
      throw new IllegalStateException(
          "Trying to decrement reference count for an item you don't own.");
    }

    mReferenceCountMap.put(renderUnitId, refCount - 1);
  }

  public MountDelegateTarget getMountDelegateTarget() {
    return mMountDelegateTarget;
  }

  public Systracer getTracer() {
    return mTracer;
  }

  @VisibleForTesting
  public int getRefCount(long id) {
    return mReferenceCountMap.get(id);
  }

  @VisibleForTesting
  public List<ExtensionState> getExtensionStates() {
    return mExtensionStates;
  }

  public void setMountStateListener(@Nullable RenderTreeUpdateListener listener) {
    mMountStateListener = listener;
  }

  public void renderTreeUpdated(RenderCoreExtensionHost host) {
    if (mMountStateListener != null) {
      mMountStateListener.onRenderTreeUpdated(host);
    }
  }
}
