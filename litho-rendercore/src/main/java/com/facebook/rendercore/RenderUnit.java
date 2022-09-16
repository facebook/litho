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
  // These maps are used to match an extension with its Binder class. Every renderUnit should have
  // only one Binder per type.
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
    for (DelegateBinder<?, ? super MOUNT_CONTENT> extension : mountBinders) {
      addMountUnmountExtension(extension);
    }
    for (DelegateBinder<?, ? super MOUNT_CONTENT> extension : attachBinders) {
      addAttachDetachExtension(extension);
    }
  }

  public RenderType getRenderType() {
    return mRenderType;
  }

  public abstract ContentAllocator getContentAllocator();

  /** @return a unique id identifying this RenderUnit in the tree of Node it is part of. */
  public abstract long getId();

  protected @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>>
      getMountUnmountBinderTypeToExtensionMap() {
    return mMountBinderTypeToDelegateMap;
  }

  protected @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> getMountUnmountExtensions() {
    return mMountBinders;
  }

  protected @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>>
      getAttachDetachBinderTypeToExtensionMap() {
    return mAttachBinderTypeToDelegateMap;
  }

  protected @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> getAttachDetachExtensions() {
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
  public void addMountUnmountExtension(DelegateBinder<?, ? super MOUNT_CONTENT> extension) {
    if (mMountBinders == null) {
      mMountBinders = new ArrayList<>();

      if (mMountBinderTypeToDelegateMap != null) {
        throw new IllegalStateException("Extension Map and Extension List out of sync!");
      }

      mMountBinderTypeToDelegateMap = new HashMap<>();
    }

    addExtension(mMountBinderTypeToDelegateMap, mMountBinders, extension);
  }

  /**
   * Adds {@link DelegateBinder}s that will be invoked with the other mount/unmount binders. Can be
   * used to add generic functionality (e.g. accessibility) to a RenderUnit.
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  @SafeVarargs
  public final void addMountUnmountExtensions(
      DelegateBinder<?, ? super MOUNT_CONTENT>... extensions) {
    for (int i = 0; i < extensions.length; i++) {
      addMountUnmountExtension(extensions[i]);
    }
  }

  /**
   * Adds an {@link DelegateBinder} that will be invoked with the other attach/detach binders. Can
   * be used to add generic functionality (e.g. Dynamic Props) to a RenderUnit
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  public void addAttachDetachExtension(DelegateBinder<?, ? super MOUNT_CONTENT> extension) {
    if (mAttachBinders == null) {
      mAttachBinders = new ArrayList<>();

      if (mAttachBinderTypeToDelegateMap != null) {
        throw new IllegalStateException("Extension Map and Extension List out of sync!");
      }

      mAttachBinderTypeToDelegateMap = new HashMap<>();
    }

    addExtension(mAttachBinderTypeToDelegateMap, mAttachBinders, extension);
  }

  /**
   * Adds an {@link DelegateBinder}s that will be invoked with the other attach/detach binders. Can
   * be used to add generic functionality (e.g. Dynamic Props) to a RenderUnit
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  @SafeVarargs
  public final void addAttachDetachExtensions(
      DelegateBinder<?, ? super MOUNT_CONTENT>... extensions) {
    for (int i = 0; i < extensions.length; i++) {
      addAttachDetachExtension(extensions[i]);
    }
  }

  // Make sure an Extension with the same Binder is not already defined in this RenderUnit.
  // If that's the case, remove the old Extension and add the new one at the current list position
  // which is at the end.
  private static <MOUNT_CONTENT> void addExtension(
      Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>> binderTypeToExtensionMap,
      List<DelegateBinder<?, MOUNT_CONTENT>> extensions,
      DelegateBinder extension) {
    final @Nullable DelegateBinder prevExtension =
        binderTypeToExtensionMap.put(extension.binder.getClass(), extension);
    if (prevExtension != null) {
      // A binder with the same type was already present and we need to clear it from the extension
      // list.
      boolean found = false;
      for (int i = extensions.size() - 1; i >= 0; i--) {
        if (extensions.get(i).binder.getClass() == extension.binder.getClass()) {
          extensions.remove(i);
          found = true;
          break;
        }
      }
      if (!found) {
        throw new IllegalStateException("Extension Map and Extension List out of sync!");
      }
    }

    extensions.add(extension);
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

  /** Bind all mountUnmount extension functions. */
  protected void mountExtensions(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mMountBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (DelegateBinder extension : mMountBinders) {
      if (isTracing) {
        tracer.beginSection("RenderUnit.mountExtension:" + getId());
      }
      extension.bind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /** Unbind all mountUnmount extension functions. */
  protected void unmountExtensions(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mMountBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (int i = mMountBinders.size() - 1; i >= 0; i--) {
      final DelegateBinder extension = mMountBinders.get(i);
      if (isTracing) {
        tracer.beginSection("RenderUnit.unmountExtension:" + getId());
      }
      extension.unbind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /** Bind all attachDetach extension functions. */
  protected void attachExtensions(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mAttachBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (DelegateBinder extension : mAttachBinders) {
      if (isTracing) {
        tracer.beginSection("RenderUnit.attachExtension:" + getId());
      }
      extension.bind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /** Unbind all attachDetach extension functions. */
  protected void detachExtensions(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData, Systracer tracer) {
    if (mAttachBinders == null) {
      return;
    }

    final boolean isTracing = tracer.isTracing();
    for (int i = mAttachBinders.size() - 1; i >= 0; i--) {
      final DelegateBinder extension = mAttachBinders.get(i);
      if (isTracing) {
        tracer.beginSection("RenderUnit.detachExtension:" + getId());
      }
      extension.unbind(context, content, layoutData);
      if (isTracing) {
        tracer.endSection();
      }
    }
  }

  /**
   * Unbind and rebind all extensions which should update compared to a previous (i.e. current)
   * RenderUnit.
   */
  void updateExtensions(
      Context context,
      MOUNT_CONTENT content,
      RenderUnit<MOUNT_CONTENT> currentRenderUnit,
      @Nullable Object currentLayoutData,
      @Nullable Object newLayoutData,
      @Nullable MountDelegate mountDelegate,
      boolean isAttached) {

    final List<DelegateBinder> attachDetachExtensionsForBind =
        new ArrayList<>(sizeOrZero(getAttachDetachExtensions()));
    final List<DelegateBinder> attachDetachExtensionsForUnbind =
        new ArrayList<>(sizeOrZero(currentRenderUnit.getAttachDetachExtensions()));
    final List<DelegateBinder> mountUnmountExtensionsForBind =
        new ArrayList<>(sizeOrZero(getMountUnmountExtensions()));
    final List<DelegateBinder> mountUnmountExtensionsForUnbind =
        new ArrayList<>(sizeOrZero(currentRenderUnit.getMountUnmountExtensions()));

    // 1. Diff the extensions to resolve what's to bind/unbind.
    resolveExtensionsToUpdate(
        currentRenderUnit.getAttachDetachExtensions(),
        getAttachDetachExtensions(),
        currentRenderUnit.getAttachDetachBinderTypeToExtensionMap(),
        currentLayoutData,
        newLayoutData,
        attachDetachExtensionsForBind,
        attachDetachExtensionsForUnbind);
    resolveExtensionsToUpdate(
        currentRenderUnit.getMountUnmountExtensions(),
        getMountUnmountExtensions(),
        currentRenderUnit.getMountUnmountBinderTypeToExtensionMap(),
        currentLayoutData,
        newLayoutData,
        mountUnmountExtensionsForBind,
        mountUnmountExtensionsForUnbind);

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
        mountDelegate.onUnbindItemWhichRequiresUpdate(
            extensionStatesToUpdate,
            currentRenderUnit,
            currentLayoutData,
            this,
            newLayoutData,
            content);
      }
      for (int i = attachDetachExtensionsForUnbind.size() - 1; i >= 0; i--) {
        final DelegateBinder extension = attachDetachExtensionsForUnbind.get(i);
        extension.unbind(context, content, currentLayoutData);
      }
    }

    // 3. unbind all mount binders which should update.
    if (mountDelegate != null && extensionStatesToUpdate != null) {
      mountDelegate.onUnmountItemWhichRequiresUpdate(
          extensionStatesToUpdate,
          currentRenderUnit,
          currentLayoutData,
          this,
          newLayoutData,
          content);
    }
    for (int i = mountUnmountExtensionsForUnbind.size() - 1; i >= 0; i--) {
      final DelegateBinder extension = mountUnmountExtensionsForUnbind.get(i);
      extension.unbind(context, content, currentLayoutData);
    }

    // 4. rebind all mount binder which did update.
    for (DelegateBinder extension : mountUnmountExtensionsForBind) {
      extension.bind(context, content, newLayoutData);
    }
    if (mountDelegate != null && extensionStatesToUpdate != null) {
      mountDelegate.onMountItemWhichRequiresUpdate(
          extensionStatesToUpdate,
          currentRenderUnit,
          currentLayoutData,
          this,
          newLayoutData,
          content);
    }

    // 5. rebind all attach binder which did update.
    for (DelegateBinder extension : attachDetachExtensionsForBind) {
      extension.bind(context, content, newLayoutData);
    }
    if (mountDelegate != null && extensionStatesToUpdate != null) {
      mountDelegate.onBindItemWhichRequiresUpdate(
          extensionStatesToUpdate,
          currentRenderUnit,
          currentLayoutData,
          this,
          newLayoutData,
          content);
    }
  }

  /**
   * This methods diff current and new extensions, calling shouldUpdate if needed, and returning a
   * list of extensions from the "current" ones to unbind, and a list of extensions from the "new"
   * ones to bind.
   */
  private static <MOUNT_CONTENT> void resolveExtensionsToUpdate(
      @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> currentExtensions,
      @Nullable List<DelegateBinder<?, MOUNT_CONTENT>> newExtensions,
      @Nullable Map<Class<?>, DelegateBinder<?, MOUNT_CONTENT>> currentBinderTypeToExtensionMap,
      @Nullable Object currentLayoutData,
      @Nullable Object newLayoutData,
      List<DelegateBinder> extensionsToBind,
      List<DelegateBinder> extensionsToUnbind) {

    // There's nothing to unbind because there aren't any current extensions, we need to bind all
    // new Extensions.
    if (currentExtensions == null || currentExtensions.isEmpty()) {
      if (newExtensions != null) {
        extensionsToBind.addAll(newExtensions);
      }
      return;
    }

    // There's no new extensions. All current extensions have to be unbound.
    if (newExtensions == null || newExtensions.isEmpty()) {
      extensionsToUnbind.addAll(currentExtensions);
      return;
    }

    final Map<Class<?>, Boolean> binderToShouldUpdate = new HashMap<>(newExtensions.size());

    // Parse all newExtensions and resolve which ones are to bind.
    for (DelegateBinder newExtension : newExtensions) {
      final Class<?> binderClass = newExtension.binder.getClass();
      final @Nullable DelegateBinder currentExtension =
          currentBinderTypeToExtensionMap.get(binderClass);

      if (currentExtension == null) {
        // Found new Extension, has to be bound.
        extensionsToBind.add(newExtension);
        continue;
      }

      final boolean shouldUpdate =
          newExtension.shouldUpdate(currentExtension, currentLayoutData, newLayoutData);
      // Memoize the result for the next for-loop.
      binderToShouldUpdate.put(binderClass, shouldUpdate);
      if (shouldUpdate) {
        extensionsToBind.add(newExtension);
      }
    }

    // Parse all currentExtensions and resolve which ones are to unbind.
    for (DelegateBinder currentExtension : currentExtensions) {
      final Class<?> binderClass = currentExtension.binder.getClass();
      if (!binderToShouldUpdate.containsKey(binderClass) || binderToShouldUpdate.get(binderClass)) {
        // Found a currentExtension which either is not in the new RenderUnit or shouldUpdate is
        // true, therefore we need to unbind it.
        extensionsToUnbind.add(currentExtension);
      }
    }
  }

  private static int sizeOrZero(@Nullable Collection<?> collection) {
    return collection == null ? 0 : collection.size();
  }

  /**
   * An Extension is a pair of data Model and {@link Binder}. The binder will bind the model to a
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
     * Create an Extension with a Model and {@link Binder} which will bind the given Model to the
     * content type which will be provided by the RenderUnit.
     */
    public static <MODEL, CONTENT> DelegateBinder<MODEL, CONTENT> createDelegateBinder(
        MODEL model, Binder<MODEL, CONTENT> binder) {
      return new DelegateBinder<>(model, binder);
    }

    boolean shouldUpdate(
        final DelegateBinder<MODEL, CONTENT> prevExtension,
        final @Nullable Object currentLayoutData,
        final @Nullable Object nextLayoutData) {
      return binder.shouldUpdate(prevExtension.model, model, currentLayoutData, nextLayoutData);
    }

    void bind(final Context context, final CONTENT content, final @Nullable Object layoutData) {
      binder.bind(context, content, model, layoutData);
    }

    void unbind(final Context context, final CONTENT content, final @Nullable Object layoutData) {
      binder.unbind(context, content, model, layoutData);
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
