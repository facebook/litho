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

  // RenderUnit's description is used for tracing, and according to:
  // https://developer.android.com/reference/android/os/Trace#beginSection(java.lang.String)
  // the max length of the tracing section name is 127
  public static final int MAX_DESCRIPTION_LENGTH = 127;

  private static final int MAX_FIXED_MOUNT_BINDERS_COUNT = 64;

  private final RenderType mRenderType;

  // These maps are used to match a binder with its Binder class.
  // Every RenderUnit should have only one Binder per type.
  private @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>>
      mOptionalMountBinderTypeToDelegateMap;
  private @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> mOptionalMountBinders;
  // Fixed mount binders are binders that are always there for a given RenderUnit type, and they're
  // always in the same order.
  private final List<DelegateBinder<?, ? super MOUNT_CONTENT>> mFixedMountBinders;

  private @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>> mAttachBinderTypeToDelegateMap;
  private @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> mAttachBinders;

  public RenderUnit(RenderType renderType) {
    this(
        renderType,
        Collections.<DelegateBinder<?, ? super MOUNT_CONTENT>>emptyList(),
        Collections.<DelegateBinder<?, ? super MOUNT_CONTENT>>emptyList(),
        Collections.<DelegateBinder<?, ? super MOUNT_CONTENT>>emptyList());
  }

  public RenderUnit(
      RenderType renderType, List<DelegateBinder<?, ? super MOUNT_CONTENT>> optionalMountBinders) {
    this(
        renderType,
        Collections.<DelegateBinder<?, ? super MOUNT_CONTENT>>emptyList(),
        optionalMountBinders,
        Collections.<DelegateBinder<?, ? super MOUNT_CONTENT>>emptyList());
  }

  public RenderUnit(
      RenderType type,
      List<DelegateBinder<?, ? super MOUNT_CONTENT>> optionalMountBinders,
      List<DelegateBinder<?, ? super MOUNT_CONTENT>> attachBinders) {
    this(
        type,
        Collections.<DelegateBinder<?, ? super MOUNT_CONTENT>>emptyList(),
        optionalMountBinders,
        attachBinders);
  }

  public RenderUnit(
      RenderType type,
      List<DelegateBinder<?, ? super MOUNT_CONTENT>> fixedMountBinders,
      List<DelegateBinder<?, ? super MOUNT_CONTENT>> optionalMountBinders,
      List<DelegateBinder<?, ? super MOUNT_CONTENT>> attachBinders) {
    if (fixedMountBinders != null && fixedMountBinders.size() > MAX_FIXED_MOUNT_BINDERS_COUNT) {
      throw new IllegalStateException(
          "Too many fixed mount binders. Max is " + MAX_FIXED_MOUNT_BINDERS_COUNT);
    }

    mRenderType = type;
    mFixedMountBinders = fixedMountBinders;
    for (int i = 0; i < optionalMountBinders.size(); i++) {
      final DelegateBinder<?, ? super MOUNT_CONTENT> binder = optionalMountBinders.get(i);
      addOptionalMountBinder(binder);
    }
    for (int i = 0; i < attachBinders.size(); i++) {
      final DelegateBinder<?, ? super MOUNT_CONTENT> binder = attachBinders.get(i);
      addAttachBinder(binder);
    }
  }

  public RenderType getRenderType() {
    return mRenderType;
  }

  public abstract ContentAllocator<MOUNT_CONTENT> getContentAllocator();

  /** @return a unique id identifying this RenderUnit in the tree of Node it is part of. */
  public abstract long getId();

  protected @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>>
      getOptionalMountBinderTypeToDelegateMap() {
    return mOptionalMountBinderTypeToDelegateMap;
  }

  protected @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> getOptionalMountBinders() {
    return mOptionalMountBinders;
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
  public void addOptionalMountBinder(DelegateBinder<?, ? super MOUNT_CONTENT> binder) {
    if (mOptionalMountBinders == null) {
      mOptionalMountBinders = new ArrayList<>();

      if (mOptionalMountBinderTypeToDelegateMap != null) {
        throw new IllegalStateException("Binder Map and Binder List out of sync!");
      }

      mOptionalMountBinderTypeToDelegateMap = new HashMap<>();
    }

    addBinder(mOptionalMountBinderTypeToDelegateMap, mOptionalMountBinders, binder);
  }

  /**
   * Adds {@link DelegateBinder}s that will be invoked with the other mount/unmount binders. Can be
   * used to add generic functionality (e.g. accessibility) to a RenderUnit.
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  @SafeVarargs
  public final void addOptionalMountBinders(DelegateBinder<?, ? super MOUNT_CONTENT>... binders) {
    for (int i = 0; i < binders.length; i++) {
      addOptionalMountBinder(binders[i]);
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

  /** Bind all fixed mountUnmount binder functions. */
  private void mountFixedBinders(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    final boolean isTracing = tracer.isTracing();
    for (int i = 0; i < mFixedMountBinders.size(); i++) {
      final DelegateBinder binder = mFixedMountBinders.get(i);
      if (isTracing) {
        tracer.beginSection("RenderUnit.mountFixedBinder:" + binder.getSimpleName());
      }
      binder.bind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /** Bind all mountUnmount binder functions. */
  protected void mountBinders(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    mountFixedBinders(context, content, layoutData, tracer);
    if (mOptionalMountBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (int i = 0; i < mOptionalMountBinders.size(); i++) {
      final DelegateBinder binder = mOptionalMountBinders.get(i);
      if (isTracing) {
        tracer.beginSection("RenderUnit.mountBinder:" + binder.getSimpleName());
      }
      binder.bind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /** Unbind all fixed mountUnmount binder functions. */
  private void unmountFixedBinders(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    final boolean isTracing = tracer.isTracing();
    for (int i = mFixedMountBinders.size() - 1; i >= 0; i--) {
      final DelegateBinder binder = mFixedMountBinders.get(i);
      if (isTracing) {
        tracer.beginSection("RenderUnit.unmountFixedBinder:" + binder.getSimpleName());
      }
      binder.unbind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /** Unbind all mountUnmount binder functions. */
  protected void unmountBinders(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mOptionalMountBinders != null) {
      final boolean isTracing = tracer.isTracing();
      for (int i = mOptionalMountBinders.size() - 1; i >= 0; i--) {
        final DelegateBinder binder = mOptionalMountBinders.get(i);
        if (isTracing) {
          tracer.beginSection("RenderUnit.unmountBinder:" + binder.getSimpleName());
        }
        binder.unbind(context, content, layoutData);
        if (isTracing) {
          tracer.endSection();
        }
      }
    }

    unmountFixedBinders(context, content, layoutData, tracer);
  }

  /** Bind all attachDetach binder functions. */
  protected void attachBinders(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mAttachBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (int i = 0; i < mAttachBinders.size(); i++) {
      final DelegateBinder binder = mAttachBinders.get(i);
      if (isTracing) {
        tracer.beginSection("RenderUnit.attachBinder:" + binder.getSimpleName());
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
        tracer.beginSection("RenderUnit.detachBinder:" + binder.getSimpleName());
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
  protected void updateBinders(
      Context context,
      MOUNT_CONTENT content,
      RenderUnit<MOUNT_CONTENT> currentRenderUnit,
      @Nullable Object currentLayoutData,
      @Nullable Object newLayoutData,
      @Nullable MountDelegate mountDelegate,
      boolean isAttached) {

    final List<DelegateBinder> attachBindersForBind = new ArrayList<>(sizeOrZero(mAttachBinders));
    final List<DelegateBinder> attachBindersForUnbind =
        new ArrayList<>(sizeOrZero(currentRenderUnit.mAttachBinders));
    final List<DelegateBinder> optionalMountBindersForBind =
        new ArrayList<>(sizeOrZero(mOptionalMountBinders));
    final List<DelegateBinder> optionalMountBindersForUnbind =
        new ArrayList<>(sizeOrZero(currentRenderUnit.mOptionalMountBinders));

    // 1. Resolve fixed mount binders which should update.
    long fixedMountBindersToUpdate =
        resolveFixedMountBindersToUpdate(
            currentRenderUnit.mFixedMountBinders,
            mFixedMountBinders,
            currentLayoutData,
            newLayoutData);

    // 2. Diff the binders to resolve what's to bind/unbind.
    resolveBindersToUpdate(
        currentRenderUnit.mAttachBinders,
        mAttachBinders,
        currentRenderUnit.mAttachBinderTypeToDelegateMap,
        currentLayoutData,
        newLayoutData,
        attachBindersForBind,
        attachBindersForUnbind);
    resolveBindersToUpdate(
        currentRenderUnit.mOptionalMountBinders,
        mOptionalMountBinders,
        currentRenderUnit.mOptionalMountBinderTypeToDelegateMap,
        currentLayoutData,
        newLayoutData,
        optionalMountBindersForBind,
        optionalMountBindersForUnbind);

    final @Nullable List<ExtensionState> extensionStatesToUpdate;

    if (mountDelegate != null) {
      extensionStatesToUpdate =
          mountDelegate.collateExtensionsToUpdate(
              currentRenderUnit, currentLayoutData, this, newLayoutData);
    } else {
      extensionStatesToUpdate = null;
    }

    // 3. Unbind all attach binders which should update (only if currently attached).
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

    // 4. Unbind all optional and fixed mount binders which should update.
    if (mountDelegate != null && extensionStatesToUpdate != null) {
      MountDelegate.onUnmountItemWhichRequiresUpdate(
          extensionStatesToUpdate,
          currentRenderUnit,
          currentLayoutData,
          this,
          newLayoutData,
          content);
    }
    for (int i = optionalMountBindersForUnbind.size() - 1; i >= 0; i--) {
      final DelegateBinder binder = optionalMountBindersForUnbind.get(i);
      binder.unbind(context, content, currentLayoutData);
    }
    if (fixedMountBindersToUpdate != 0) {
      for (int i = mFixedMountBinders.size() - 1; i >= 0; i--) {
        if ((fixedMountBindersToUpdate & ((long) 0x1 << i)) != 0) {
          final DelegateBinder binder = currentRenderUnit.mFixedMountBinders.get(i);
          binder.unbind(context, content, currentLayoutData);
        }
      }
    }

    // 5. Rebind all fixed and optional mount binders which did update.
    if (fixedMountBindersToUpdate != 0) {
      for (int i = 0; i < mFixedMountBinders.size(); i++) {
        if ((fixedMountBindersToUpdate & (0x1L << i)) != 0) {
          final DelegateBinder binder = mFixedMountBinders.get(i);
          binder.bind(context, content, newLayoutData);
        }
      }
    }
    for (int i = 0; i < optionalMountBindersForBind.size(); i++) {
      final DelegateBinder binder = optionalMountBindersForBind.get(i);
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

    // 6. Rebind all attach binders which did update.
    for (int i = 0; i < attachBindersForBind.size(); i++) {
      final DelegateBinder binder = attachBindersForBind.get(i);
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

  public @Nullable <T extends Binder<?, ?>> T findAttachBinderByClass(Class<T> klass) {
    if (mAttachBinderTypeToDelegateMap == null || mAttachBinderTypeToDelegateMap.isEmpty()) {
      return null;
    }

    final DelegateBinder binder = mAttachBinderTypeToDelegateMap.get(klass);
    if (binder == null) {
      return null;
    }

    return (T) binder.binder;
  }

  /**
   * This methods validates current and new fixed mount binders, calling shouldUpdate if needed, and
   * returning a long value with bits set to 1 for all fixed binders that need to be updated.
   */
  private static <MOUNT_CONTENT> long resolveFixedMountBindersToUpdate(
      List<DelegateBinder<?, ? super MOUNT_CONTENT>> currentFixedBinders,
      List<DelegateBinder<?, ? super MOUNT_CONTENT>> newFixedBinders,
      @Nullable Object currentLayoutData,
      @Nullable Object newLayoutData) {
    long fixedMountBindersToUpdate = 0;

    if (currentFixedBinders.isEmpty() && newFixedBinders.isEmpty()) {
      return fixedMountBindersToUpdate;
    }

    if (currentFixedBinders.size() != newFixedBinders.size()) {
      throw new IllegalStateException(
          "Current and new fixed Mount Binders are of sync: \n"
              + "currentFixedBinders.size() = "
              + currentFixedBinders.size()
              + "\n"
              + "newFixedBinders.size() = "
              + newFixedBinders.size());
    }

    for (int i = 0; i < currentFixedBinders.size(); i++) {
      final DelegateBinder currentBinder = currentFixedBinders.get(i);
      final DelegateBinder newBinder = newFixedBinders.get(i);

      if (newBinder.shouldUpdate(currentBinder, currentLayoutData, newLayoutData)) {
        fixedMountBindersToUpdate = fixedMountBindersToUpdate | ((long) 0x1 << i);
      }
    }

    return fixedMountBindersToUpdate;
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
    for (int i = 0; i < newBinders.size(); i++) {
      final DelegateBinder newBinder = newBinders.get(i);
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
    for (int i = 0; i < currentBinders.size(); i++) {
      final DelegateBinder currentBinder = currentBinders.get(i);
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
