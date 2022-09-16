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

import android.content.Context;
import androidx.annotation.Nullable;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.utils.CommonUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A RenderUnit represents a single rendering primitive for RenderCore. Every RenderUnit has to
 * define at least a createContent method to allocate the RenderUnit content (View or Drawable) via
 * the {@link ContentAllocator} it returns from getContentAllocator method. That content will be
 * automatically recycled by RenderCore based on their concrete type.
 *
 * <p>A RenderUnit should in most cases declare how it intends to bind data returning Binders from
 * its mountUnmountFunctions callback or from the attachDetachFunctions callback.
 *
 * <p>Immutability: RenderUnits should be immutable! Continuing to change them after they are built
 * and given to RenderCore (e.g. via RenderState) is not safe.
 */
public abstract class RenderUnit<MOUNT_CONTENT> {

  public enum RenderType {
    DRAWABLE,
    VIEW,
  }

  private final RenderType mRenderType;

  // These maps are used to match a binder with its Binder class.
  // Every RenderUnit should have only one Binder per type.
  private @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>> mMountBinderTypeToDelegateMap;
  private @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> mMountBinders;

  private @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>> mAttachBinderTypeToDelegateMap;
  private @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> mAttachBinders;

  public RenderUnit(RenderType renderType) {
    this(
        renderType,
        Collections.<DelegateBinder<?, ? super MOUNT_CONTENT>>emptyList(),
        Collections.<DelegateBinder<?, ? super MOUNT_CONTENT>>emptyList());
  }

  public RenderUnit(
      RenderType renderType, List<DelegateBinder<?, ? super MOUNT_CONTENT>> mountBinders) {
    this(
        renderType,
        mountBinders,
        Collections.<DelegateBinder<?, ? super MOUNT_CONTENT>>emptyList());
  }

  public RenderUnit(
      RenderType type,
      List<DelegateBinder<?, ? super MOUNT_CONTENT>> mountBinders,
      List<DelegateBinder<?, ? super MOUNT_CONTENT>> attachBinders) {
    mRenderType = type;
    for (DelegateBinder<?, ? super MOUNT_CONTENT> binder : mountBinders) {
      addMountBinder(binder);
    }
    for (DelegateBinder<?, ? super MOUNT_CONTENT> binder : attachBinders) {
      addAttachBinder(binder);
    }
  }

  public RenderType getRenderType() {
    return mRenderType;
  }

  public abstract ContentAllocator getContentAllocator();

  /** @return a unique id identifying this RenderUnit in the tree of Node it is part of. */
  public abstract long getId();

  protected @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>>
      getMountBinderTypeToDelegateMap() {
    return mMountBinderTypeToDelegateMap;
  }

  protected @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> getMountBinders() {
    return mMountBinders;
  }

  protected @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>>
      getAttachBinderTypeToDelegateMap() {
    return mAttachBinderTypeToDelegateMap;
  }

  protected @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> getAttachBinders() {
    return mAttachBinders;
  }

  public Class<?> getRenderContentType() {
    return getClass();
  }

  protected void onStartUpdateRenderUnit() {}

  protected void onEndUpdateRenderUnit() {}

  public String getDescription() {
    // This API is primarily used for tracing, and the section names have a char limit of 127.
    // If the class name exceeds that it will be replace by the simple name.
    // In a release build the class name will be minified, so it is unlikely to hit the limit.
    final String name = getClass().getName();
    return name.length() > 80 ? getClass().getSimpleName() : "<cls>" + name + "</cls>";
  }

  /**
   * Adds an {@link DelegateBinder} that will be invoked with the other mount/unmount binders. Can
   * be used to add generic functionality (e.g. accessibility) to a RenderUnit.
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  public void addMountBinder(DelegateBinder<?, ? super MOUNT_CONTENT> binder) {
    if (mMountBinders == null) {
      mMountBinders = new ArrayList<>();

      if (mMountBinderTypeToDelegateMap != null) {
        throw new IllegalStateException("Binder Map and Binder List out of sync!");
      }

      mMountBinderTypeToDelegateMap = new HashMap<>();
    }

    addBinder(mMountBinderTypeToDelegateMap, mMountBinders, binder);
  }

  /**
   * Adds {@link DelegateBinder}s that will be invoked with the other mount/unmount binders. Can be
   * used to add generic functionality (e.g. accessibility) to a RenderUnit.
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  @SafeVarargs
  public final void addMountBinders(DelegateBinder<?, ? super MOUNT_CONTENT>... binders) {
    for (int i = 0; i < binders.length; i++) {
      addMountBinder(binders[i]);
    }
  }

  /**
   * Adds an {@link DelegateBinder} that will be invoked with the other attach/detach binders. Can
   * be used to add generic functionality (e.g. Dynamic Props) to a RenderUnit
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  public void addAttachBinder(DelegateBinder<?, ? super MOUNT_CONTENT> binder) {
    if (mAttachBinders == null) {
      mAttachBinders = new ArrayList<>();

      if (mAttachBinderTypeToDelegateMap != null) {
        throw new IllegalStateException("Binder Map and Binder List out of sync!");
      }

      mAttachBinderTypeToDelegateMap = new HashMap<>();
    }

    addBinder(mAttachBinderTypeToDelegateMap, mAttachBinders, binder);
  }

  /**
   * Adds an {@link DelegateBinder}s that will be invoked with the other attach/detach binders. Can
   * be used to add generic functionality (e.g. Dynamic Props) to a RenderUnit
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  @SafeVarargs
  public final void addAttachBinders(DelegateBinder<?, ? super MOUNT_CONTENT>... binders) {
    for (int i = 0; i < binders.length; i++) {
      addAttachBinder(binders[i]);
    }
  }

  // Make sure a binder with the same Binder is not already defined in this RenderUnit.
  // If that's the case, remove the old binder and add the new one at the current list position
  // which is at the end.
  private static <MOUNT_CONTENT> void addBinder(
      Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>> binderTypeToBinderMap,
      List<DelegateBinder<?, MOUNT_CONTENT>> binders,
      DelegateBinder binder) {
    final @Nullable DelegateBinder prevBinder =
        binderTypeToBinderMap.put(binder.binder.getClass(), binder);
    if (prevBinder != null) {
      // A binder with the same type was already present and it should be removed.
      boolean found = false;
      for (int i = binders.size() - 1; i >= 0; i--) {
        if (binders.get(i).binder.getClass() == binder.binder.getClass()) {
          binders.remove(i);
          found = true;
          break;
        }
      }
      if (!found) {
        throw new IllegalStateException("Binder Map and Binder List out of sync!");
      }
    }

    binders.add(binder);
  }

  /**
   * Override this method to indicate if a {@link RenderUnit} has nested {@link RenderTreeHost}s, it
   * will ensure that they are notified when this {@link RenderUnit}'s bounds change and visibility
   * events are processed correctly for them.
   *
   * @return {@code true} to ensure nested {@link RenderTreeHost}s are notified about parent's
   *     bounds change, otherwise {@code false}
   */
  public boolean doesMountRenderTreeHosts() {
    return false;
  }

  /** Bind all mountUnmount binder functions. */
  protected void mountBinders(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mMountBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (DelegateBinder binder : mMountBinders) {
      if (isTracing) {
        tracer.beginSection("RenderUnit.mountExtension:" + binder.getSimpleName());
      }
      binder.bind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /** Unbind all mountUnmount binder functions. */
  protected void unmountBinders(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mMountBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (int i = mMountBinders.size() - 1; i >= 0; i--) {
      final DelegateBinder binder = mMountBinders.get(i);
      if (isTracing) {
        tracer.beginSection("RenderUnit.unmountExtension:" + binder.getSimpleName());
      }
      binder.unbind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /** Bind all attachDetach binder functions. */
  protected void attachBinders(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mAttachBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (DelegateBinder binder : mAttachBinders) {
      if (isTracing) {
        tracer.beginSection("RenderUnit.attachExtension:" + binder.getSimpleName());
      }
      binder.bind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /** Unbind all attachDetach binder functions. */
  protected void detachBinders(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mAttachBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (int i = mAttachBinders.size() - 1; i >= 0; i--) {
      final DelegateBinder binder = mAttachBinders.get(i);
      if (isTracing) {
        tracer.beginSection("RenderUnit.detachExtension:" + binder.getSimpleName());
      }
      binder.unbind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /**
   * Unbind and rebind all binders which should update compared to a previous (i.e. current)
   * RenderUnit.
   */
  void updateBinders(
      Context context,
      MOUNT_CONTENT content,
      RenderUnit<MOUNT_CONTENT> currentRenderUnit,
      @Nullable Object currentLayoutData,
      @Nullable Object newLayoutData,
      @Nullable MountDelegate mountDelegate,
      boolean isAttached) {

    final List<DelegateBinder> attachBindersForBind =
        new ArrayList<>(sizeOrZero(getAttachBinders()));
    final List<DelegateBinder> attachBindersForUnbind =
        new ArrayList<>(sizeOrZero(currentRenderUnit.getAttachBinders()));
    final List<DelegateBinder> mountBindersForBind = new ArrayList<>(sizeOrZero(getMountBinders()));
    final List<DelegateBinder> mountBindersForUnbind =
        new ArrayList<>(sizeOrZero(currentRenderUnit.getMountBinders()));

    // 1. Diff the binders to resolve what's to bind/unbind.
    resolveBindersToUpdate(
        currentRenderUnit.getAttachBinders(),
        getAttachBinders(),
        currentRenderUnit.getAttachBinderTypeToDelegateMap(),
        currentLayoutData,
        newLayoutData,
        attachBindersForBind,
        attachBindersForUnbind);
    resolveBindersToUpdate(
        currentRenderUnit.getMountBinders(),
        getMountBinders(),
        currentRenderUnit.getMountBinderTypeToDelegateMap(),
        currentLayoutData,
        newLayoutData,
        mountBindersForBind,
        mountBindersForUnbind);

    final @Nullable List<ExtensionState> extensionStatesToUpdate;

    if (mountDelegate != null) {
      extensionStatesToUpdate =
          mountDelegate.collateExtensionsToUpdate(
              currentRenderUnit, currentLayoutData, this, newLayoutData);
    } else {
      extensionStatesToUpdate = null;
    }

    // 2. unbind all attach binders which should update (only if currently attached).
    if (isAttached) {
      if (mountDelegate != null && extensionStatesToUpdate != null) {
        MountDelegate.onUnbindItemWhichRequiresUpdate(
            extensionStatesToUpdate,
            currentRenderUnit,
            currentLayoutData,
            this,
            newLayoutData,
            content);
      }
      for (int i = attachBindersForUnbind.size() - 1; i >= 0; i--) {
        final DelegateBinder binder = attachBindersForUnbind.get(i);
        binder.unbind(context, content, currentLayoutData);
      }
    }

    // 3. unbind all mount binders which should update.
    if (mountDelegate != null && extensionStatesToUpdate != null) {
      MountDelegate.onUnmountItemWhichRequiresUpdate(
          extensionStatesToUpdate,
          currentRenderUnit,
          currentLayoutData,
          this,
          newLayoutData,
          content);
    }
    for (int i = mountBindersForUnbind.size() - 1; i >= 0; i--) {
      final DelegateBinder binder = mountBindersForUnbind.get(i);
      binder.unbind(context, content, currentLayoutData);
    }

    // 4. rebind all mount binders which did update.
    for (DelegateBinder binder : mountBindersForBind) {
      binder.bind(context, content, newLayoutData);
    }
    if (mountDelegate != null && extensionStatesToUpdate != null) {
      MountDelegate.onMountItemWhichRequiresUpdate(
          extensionStatesToUpdate,
          currentRenderUnit,
          currentLayoutData,
          this,
          newLayoutData,
          content);
    }

    // 5. rebind all attach binders which did update.
    for (DelegateBinder binder : attachBindersForBind) {
      binder.bind(context, content, newLayoutData);
    }
    if (mountDelegate != null && extensionStatesToUpdate != null) {
      MountDelegate.onBindItemWhichRequiresUpdate(
          extensionStatesToUpdate,
          currentRenderUnit,
          currentLayoutData,
          this,
          newLayoutData,
          content);
    }
  }

  /**
   * This methods diff current and new binders, calling shouldUpdate if needed, and returning a list
   * of binders from the "current" ones to unbind, and a list of binders from the "new" ones to
   * bind.
   */
  private static <MOUNT_CONTENT> void resolveBindersToUpdate(
      @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> currentBinders,
      @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> newBinders,
      @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>> currentBinderTypeToBinderMap,
      @Nullable Object currentLayoutData,
      @Nullable Object newLayoutData,
      List<DelegateBinder> bindersToBind,
      List<DelegateBinder> bindersToUnbind) {

    // There's nothing to unbind because there aren't any current binders, we need to bind all
    // new binders.
    if (currentBinders == null || currentBinders.isEmpty()) {
      if (newBinders != null) {
        bindersToBind.addAll(newBinders);
      }
      return;
    }

    // There's no new binders. All current binders have to be unbound.
    if (newBinders == null || newBinders.isEmpty()) {
      bindersToUnbind.addAll(currentBinders);
      return;
    }

    final Map<Class<?>, Boolean> binderToShouldUpdate = new HashMap<>(newBinders.size());

    // Parse all new binders and resolve which ones are to bind.
    for (DelegateBinder newBinder : newBinders) {
      final Class<?> binderClass = newBinder.binder.getClass();
      final @Nullable DelegateBinder currentBinder = currentBinderTypeToBinderMap.get(binderClass);

      if (currentBinder == null) {
        // Found new binder, has to be bound.
        bindersToBind.add(newBinder);
        continue;
      }

      final boolean shouldUpdate =
          newBinder.shouldUpdate(currentBinder, currentLayoutData, newLayoutData);
      // Memoize the result for the next for-loop.
      binderToShouldUpdate.put(binderClass, shouldUpdate);
      if (shouldUpdate) {
        bindersToBind.add(newBinder);
      }
    }

    // Parse all current binders and resolve which ones are to unbind.
    for (DelegateBinder currentBinder : currentBinders) {
      final Class<?> binderClass = currentBinder.binder.getClass();
      if (!binderToShouldUpdate.containsKey(binderClass) || binderToShouldUpdate.get(binderClass)) {
        // Found a current binder which either is not in the new RenderUnit or shouldUpdate is
        // true, therefore we need to unbind it.
        bindersToUnbind.add(currentBinder);
      }
    }
  }

  private static int sizeOrZero(@Nullable Collection<?> collection) {
    return collection == null ? 0 : collection.size();
  }

  /**
   * A binder is a pair of data Model and {@link Binder}. The binder will bind the model to a
   * matching content type defined.
   */
  public static class DelegateBinder<MODEL, CONTENT> {
    private final MODEL model;
    private final Binder<MODEL, CONTENT> binder;

    private DelegateBinder(MODEL model, Binder<MODEL, CONTENT> binder) {
      this.model = model;
      this.binder = binder;
    }

    /**
     * Create a binder with a Model and {@link Binder} which will bind the given Model to the
     * content type which will be provided by the RenderUnit.
     */
    public static <MODEL, CONTENT> DelegateBinder<MODEL, CONTENT> createDelegateBinder(
        MODEL model, Binder<MODEL, CONTENT> binder) {
      return new DelegateBinder<>(model, binder);
    }

    boolean shouldUpdate(
        final DelegateBinder<MODEL, CONTENT> previous,
        final @Nullable Object currentLayoutData,
        final @Nullable Object nextLayoutData) {
      return binder.shouldUpdate(previous.model, model, currentLayoutData, nextLayoutData);
    }

    void bind(final Context context, final CONTENT content, final @Nullable Object layoutData) {
      binder.bind(context, content, model, layoutData);
    }

    void unbind(final Context context, final CONTENT content, final @Nullable Object layoutData) {
      binder.unbind(context, content, model, layoutData);
    }

    String getSimpleName() {
      return CommonUtils.getSectionNameForTracing(binder.getClass());
    }
  }

  /**
   * Represents a single bind function. Every bind has an equivalent unbind and a shouldUpdate
   * callback
   */
  public interface Binder<MODEL, CONTENT> {
    boolean shouldUpdate(
        final MODEL currentModel,
        final MODEL newModel,
        final @Nullable Object currentLayoutData,
        final @Nullable Object nextLayoutData);

    void bind(
        final Context context,
        final CONTENT content,
        final MODEL model,
        final @Nullable Object layoutData);

    void unbind(
        final Context context,
        final CONTENT content,
        final MODEL model,
        final @Nullable Object layoutData);
  }
}
