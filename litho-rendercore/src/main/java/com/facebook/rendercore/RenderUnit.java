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

package com.facebook.rendercore;

import android.content.Context;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A RenderUnit represents a single rendering primitive for RenderCore. Every RenderUnit has to
 * define at least a createContent method to allocate the RenderUnit content (View or Drawable).
 * That content will be automatically recycled by RenderCore based on their concrete type.
 *
 * <p>A RenderUnit should in most cases declare how it intends to bind data returning Binders from
 * its mountUnmountFunctions callback or from the attachDetachFunctions callback.
 *
 * <p>Immutability: RenderUnits should be immutable! Continuing to change them after they are built
 * and given to RenderCore (e.g. via RenderState) is not safe.
 */
public abstract class RenderUnit<MOUNT_CONTENT> implements Copyable {

  public enum RenderType {
    DRAWABLE,
    VIEW,
  }

  private final RenderType mRenderType;
  // These maps are used to match an extension with its Binder class. Every renderUnit should have
  // only one Binder per type.
  private @Nullable Map<Class<?>, Extension<?, MOUNT_CONTENT>>
      mMountUnmountBinderTypeToExtensionMap;
  private @Nullable List<Extension<?, MOUNT_CONTENT>> mMountUnmountExtensions;
  private @Nullable Map<Class<?>, Extension<?, MOUNT_CONTENT>>
      mAttachDetachBinderTypeToExtensionMap;
  private @Nullable List<Extension<?, MOUNT_CONTENT>> mAttachDetachExtensions;

  public RenderUnit(RenderType renderType) {
    this(
        renderType,
        Collections.<Extension<?, ? super MOUNT_CONTENT>>emptyList(),
        Collections.<Extension<?, ? super MOUNT_CONTENT>>emptyList());
  }

  public RenderUnit(
      RenderType renderType, List<Extension<?, ? super MOUNT_CONTENT>> mountUnmountExtensions) {
    this(
        renderType,
        mountUnmountExtensions,
        Collections.<Extension<?, ? super MOUNT_CONTENT>>emptyList());
  }

  public RenderUnit(
      RenderType renderType,
      List<Extension<?, ? super MOUNT_CONTENT>> mountUnmountExtensions,
      List<Extension<?, ? super MOUNT_CONTENT>> attachDetachExtensions) {
    mRenderType = renderType;
    for (Extension<?, ? super MOUNT_CONTENT> extension : mountUnmountExtensions) {
      addMountUnmountExtension(extension);
    }
    for (Extension<?, ? super MOUNT_CONTENT> extension : attachDetachExtensions) {
      addAttachDetachExtension(extension);
    }
  }

  public RenderType getRenderType() {
    return mRenderType;
  }

  public abstract MOUNT_CONTENT createContent(Context c);

  /** @return a unique id identifying this RenderUnit in the tree of Node it is part of. */
  public abstract long getId();

  public Object getRenderContentType() {
    return getClass();
  }

  public boolean isRecyclingDisabled() {
    return false;
  }

  @Nullable
  public MountItemsPool.ItemPool getRecyclingPool() {
    return null;
  }

  protected Class getDescription() {
    return getClass();
  }

  @Override
  public RenderUnit makeCopy() {
    try {
      RenderUnit renderUnit = (RenderUnit) super.clone();
      if (mMountUnmountExtensions != null) {
        renderUnit.mMountUnmountExtensions = new ArrayList<>(mMountUnmountExtensions);
        renderUnit.mMountUnmountBinderTypeToExtensionMap =
            new HashMap<>(mMountUnmountBinderTypeToExtensionMap);
      }

      if (mAttachDetachExtensions != null) {
        renderUnit.mAttachDetachExtensions = new ArrayList<>(mAttachDetachExtensions);
        renderUnit.mAttachDetachBinderTypeToExtensionMap =
            new HashMap<>(mAttachDetachBinderTypeToExtensionMap);
      }

      return renderUnit;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds an {@link Extension} that will be invoked with the other mount/unmount binders. Can be
   * used to add generic functionality (e.g. accessibility) to a RenderUnit.
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  public void addMountUnmountExtension(Extension<?, ? super MOUNT_CONTENT> extension) {
    if (mMountUnmountExtensions == null) {
      mMountUnmountExtensions = new ArrayList<>();

      if (mMountUnmountBinderTypeToExtensionMap != null) {
        throw new IllegalStateException("Extension Map and Extension List out of sync!");
      }

      mMountUnmountBinderTypeToExtensionMap = new HashMap<>();
    }

    addExtension(mMountUnmountBinderTypeToExtensionMap, mMountUnmountExtensions, extension);
  }

  /**
   * Adds {@link Extension}s that will be invoked with the other mount/unmount binders. Can be used
   * to add generic functionality (e.g. accessibility) to a RenderUnit.
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  @SafeVarargs
  public final void addMountUnmountExtensions(Extension<?, ? super MOUNT_CONTENT>... extensions) {
    for (int i = 0; i < extensions.length; i++) {
      addMountUnmountExtension(extensions[i]);
    }
  }

  /**
   * Adds an {@link Extension} that will be invoked with the other attach/detach binders. Can be
   * used to add generic functionality (e.g. Dynamic Props) to a RenderUnit
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  public void addAttachDetachExtension(Extension<?, ? super MOUNT_CONTENT> extension) {
    if (mAttachDetachExtensions == null) {
      mAttachDetachExtensions = new ArrayList<>();

      if (mAttachDetachBinderTypeToExtensionMap != null) {
        throw new IllegalStateException("Extension Map and Extension List out of sync!");
      }

      mAttachDetachBinderTypeToExtensionMap = new HashMap<>();
    }

    addExtension(mAttachDetachBinderTypeToExtensionMap, mAttachDetachExtensions, extension);
  }

  /**
   * Adds an {@link Extension}s that will be invoked with the other attach/detach binders. Can be
   * used to add generic functionality (e.g. Dynamic Props) to a RenderUnit
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  @SafeVarargs
  public final void addAttachDetachExtensions(Extension<?, ? super MOUNT_CONTENT>... extensions) {
    for (int i = 0; i < extensions.length; i++) {
      addAttachDetachExtension(extensions[i]);
    }
  }

  // Make sure an Extension with the same Binder is not already defined in this RenderUnit.
  // If that's the case, remove the old Extension and add the new one at the current list position
  // which is at the end.
  private static <MOUNT_CONTENT> void addExtension(
      Map<Class<?>, Extension<?, MOUNT_CONTENT>> binderTypeToExtensionMap,
      List<Extension<?, MOUNT_CONTENT>> extensions,
      Extension extension) {
    final @Nullable Extension prevExtension =
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

  /** Bind all mountUnmount extension functions. */
  void mountExtensions(Context context, MOUNT_CONTENT content, @Nullable Object layoutData) {
    bind((List) mMountUnmountExtensions, context, content, layoutData);
  }

  /** Unbind all mountUnmount extension functions. Public because used from Litho's MountState. */
  public void unmountExtensions(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData) {
    unbind((List) mMountUnmountExtensions, context, content, layoutData);
  }

  /** Bind all attachDetach extension functions. */
  void attachExtensions(Context context, MOUNT_CONTENT content, @Nullable Object layoutData) {
    bind((List) mAttachDetachExtensions, context, content, layoutData);
  }

  /** Unbind all attachDetach extension functions. */
  void detachExtensions(Context context, MOUNT_CONTENT content, @Nullable Object layoutData) {
    unbind((List) mAttachDetachExtensions, context, content, layoutData);
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
      boolean isAttached) {

    final List<Extension> attachDetachExtensionsForBind =
        new ArrayList<>(sizeOrZero(mAttachDetachExtensions));
    final List<Extension> attachDetachExtensionsForUnbind =
        new ArrayList<>(sizeOrZero(currentRenderUnit.mAttachDetachExtensions));
    final List<Extension> mountUnmountExtensionsForBind =
        new ArrayList<>(sizeOrZero(mMountUnmountExtensions));
    final List<Extension> mountUnmountExtensionsForUnbind =
        new ArrayList<>(sizeOrZero(currentRenderUnit.mMountUnmountExtensions));

    // 1. Diff the extensions to resolve what's to bind/unbind.
    resolveExtensionsToUpdate(
        currentRenderUnit.mAttachDetachExtensions,
        mAttachDetachExtensions,
        currentRenderUnit.mAttachDetachBinderTypeToExtensionMap,
        currentLayoutData,
        newLayoutData,
        attachDetachExtensionsForBind,
        attachDetachExtensionsForUnbind);
    resolveExtensionsToUpdate(
        currentRenderUnit.mMountUnmountExtensions,
        mMountUnmountExtensions,
        currentRenderUnit.mMountUnmountBinderTypeToExtensionMap,
        currentLayoutData,
        newLayoutData,
        mountUnmountExtensionsForBind,
        mountUnmountExtensionsForUnbind);

    // 2. unbind all attach binders which should update (only if currently attached).
    if (isAttached) {
      unbind(attachDetachExtensionsForUnbind, context, content, currentLayoutData);
    }

    // 3. unbind all mount binders which should update.
    unbind(mountUnmountExtensionsForUnbind, context, content, currentLayoutData);

    // 4. rebind all mount binder which did update.
    bind(mountUnmountExtensionsForBind, context, content, newLayoutData);

    // 5. rebind all attach binder which did update.
    bind(attachDetachExtensionsForBind, context, content, newLayoutData);
  }

  /**
   * This methods diff current and new extensions, calling shouldUpdate if needed, and returning a
   * list of extensions from the "current" ones to unbind, and a list of extensions from the "new"
   * ones to bind.
   */
  private static <MOUNT_CONTENT> void resolveExtensionsToUpdate(
      @Nullable List<Extension<?, MOUNT_CONTENT>> currentExtensions,
      @Nullable List<Extension<?, MOUNT_CONTENT>> newExtensions,
      @Nullable Map<Class<?>, Extension<?, MOUNT_CONTENT>> currentBinderTypeToExtensionMap,
      @Nullable Object currentLayoutData,
      @Nullable Object newLayoutData,
      List<Extension> extensionsToBind,
      List<Extension> extensionsToUnbind) {

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
    for (Extension newExtension : newExtensions) {
      final Class<?> binderClass = newExtension.binder.getClass();
      final @Nullable Extension currentExtension = currentBinderTypeToExtensionMap.get(binderClass);

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
    for (Extension currentExtension : currentExtensions) {
      final Class<?> binderClass = currentExtension.binder.getClass();
      if (!binderToShouldUpdate.containsKey(binderClass) || binderToShouldUpdate.get(binderClass)) {
        // Found a currentExtension which either is not in the new RenderUnit or shouldUpdate is
        // true, therefore we need to unbind it.
        extensionsToUnbind.add(currentExtension);
      }
    }
  }

  private static <MOUNT_CONTENT> void bind(
      @Nullable List<Extension> extensions,
      Context context,
      MOUNT_CONTENT content,
      @Nullable Object layoutData) {
    if (extensions == null) {
      return;
    }

    for (Extension extension : extensions) {
      extension.bind(context, content, layoutData);
    }
  }

  private static <MOUNT_CONTENT> void unbind(
      @Nullable List<Extension> extensions,
      Context context,
      MOUNT_CONTENT content,
      @Nullable Object layoutData) {
    if (extensions == null) {
      return;
    }

    for (int i = extensions.size() - 1; i >= 0; i--) {
      final Extension extension = extensions.get(i);
      extension.unbind(context, content, layoutData);
    }
  }

  private static int sizeOrZero(@Nullable Collection<?> collection) {
    return collection == null ? 0 : collection.size();
  }

  /**
   * An Extension is a pair of data Model and {@link Binder}. The binder will bind the model to a
   * matching content type defined.
   */
  public static class Extension<MODEL, CONTENT> {
    private final MODEL model;
    private final Binder<MODEL, CONTENT> binder;

    private Extension(MODEL model, Binder<MODEL, CONTENT> binder) {
      this.model = model;
      this.binder = binder;
    }

    /**
     * Create an Extension with a Model and {@link Binder} which will bind the given Model to the
     * content type which will be provided by the RenderUnit.
     */
    public static <MODEL, CONTENT> Extension<MODEL, CONTENT> extension(
        MODEL model, Binder<MODEL, CONTENT> binder) {
      return new Extension<>(model, binder);
    }

    boolean shouldUpdate(
        final Extension<MODEL, CONTENT> prevExtension,
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
