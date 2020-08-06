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
import java.util.Collections;
import java.util.List;

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

  /**
   * List used to temporarily record the attach {@link RenderUnit.Binder}s which should be updated
   * (unbound and re-bound).
   */
  private static final List<Binder> sTmpAttachDetachExtensionsToUpdate = new ArrayList<>();
  /**
   * List used to temporarily record the mount {@link RenderUnit.Binder}s which should be updated
   * (unbound and re-bound).
   */
  private static final List<Binder> sTmpMountUnmountExtensionsToUpdate = new ArrayList<>();

  private final RenderType mRenderType;
  private final List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>> mBaseMountUnmountFunctions;
  private final List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>> mBaseAttachDetachFunctions;
  private @Nullable List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>
      mMountUnmountFunctionsWithExtensions;
  private @Nullable List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>
      mAttachDetachFunctionsWithExtensions;

  public RenderUnit(RenderType renderType) {
    this(
        renderType,
        Collections.<Binder<? extends RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>emptyList(),
        Collections.<Binder<? extends RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>emptyList());
  }

  public RenderUnit(
      RenderType renderType,
      List<? extends Binder<? extends RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>
          mountUnmountFunctions) {
    this(
        renderType,
        mountUnmountFunctions,
        Collections.<Binder<? extends RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>emptyList());
  }

  public RenderUnit(
      RenderType renderType,
      List<? extends Binder<? extends RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>
          mountUnmountFunctions,
      List<? extends Binder<? extends RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>
          attachDetachFunctions) {
    mRenderType = renderType;
    mBaseMountUnmountFunctions =
        (List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>) mountUnmountFunctions;
    mBaseAttachDetachFunctions =
        (List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>) attachDetachFunctions;
  }

  public RenderType getRenderType() {
    return mRenderType;
  }

  public abstract MOUNT_CONTENT createContent(Context c);

  /** @return a list of binding functions that will be invoked during the mount process. */
  @Nullable
  private List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>> mountUnmountFunctions() {
    return mMountUnmountFunctionsWithExtensions != null
        ? mMountUnmountFunctionsWithExtensions
        : mBaseMountUnmountFunctions;
  }

  /**
   * @return a list of binding functions that will be invoked whenever the shouldUpdate method in
   *     them returns true or the RenderUnit gets attached/detached from the hierarchy. If both
   *     shouldUpdate returns true and this RenderUnit was attached the bind function will still be
   *     guaranteed to be called once.
   */
  @Nullable
  private List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>> attachDetachFunctions() {
    return mAttachDetachFunctionsWithExtensions != null
        ? mAttachDetachFunctionsWithExtensions
        : mBaseAttachDetachFunctions;
  }

  /** @return a unique id identifying this RenderUnit in the tree of Node it is part of. */
  public abstract long getId();

  public Object getRenderContentType() {
    return getClass();
  }

  protected Class getDescription() {
    return getClass();
  }

  @Override
  public RenderUnit makeCopy() {
    try {
      RenderUnit renderUnit = (RenderUnit) super.clone();
      if (mMountUnmountFunctionsWithExtensions != null) {
        renderUnit.mMountUnmountFunctionsWithExtensions =
            new ArrayList<>(mMountUnmountFunctionsWithExtensions);
      }

      return renderUnit;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds an extension function that will be invoked with the other mount/unmount binders. Can be
   * used to add generic functionality (e.g. accessibility) to a RenderUnit.
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  public void addMountUnmountExtension(Binder binder) {
    if (mMountUnmountFunctionsWithExtensions == null) {
      mMountUnmountFunctionsWithExtensions = new ArrayList<>(mBaseMountUnmountFunctions.size() + 4);
      mMountUnmountFunctionsWithExtensions.addAll(mBaseMountUnmountFunctions);
    }
    mMountUnmountFunctionsWithExtensions.add(
        (Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>) binder);
  }

  /**
   * Adds an extension function that will be invoked with the other attach/detach binders. Can be
   * used to add generic functionality (e.g. Dynamic Props) to a RenderUnit
   *
   * <p>NB: This method should only be called while initially configuring the RenderUnit. See the
   * class-level javadocs about immutability.
   */
  public void addAttachDetachExtension(Binder binder) {
    if (mAttachDetachFunctionsWithExtensions == null) {
      mAttachDetachFunctionsWithExtensions = new ArrayList<>(mBaseAttachDetachFunctions.size() + 4);
      mAttachDetachFunctionsWithExtensions.addAll(mBaseAttachDetachFunctions);
    }
    mAttachDetachFunctionsWithExtensions.add(
        (Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>) binder);
  }

  /** Bind all mountUnmount extension functions. */
  void mountExtensions(Context context, MOUNT_CONTENT content, @Nullable Object layoutData) {
    bind((List) mountUnmountFunctions(), context, content, this, layoutData);
  }

  /** Unbind all mountUnmount extension functions. Public because used from Litho's MountState. */
  public void unmountExtensions(
      Context context, MOUNT_CONTENT content, @Nullable Object layoutData) {
    unbind((List) mountUnmountFunctions(), context, content, this, layoutData);
  }

  /** Bind all attachDetach extension functions. */
  void attachExtensions(Context context, MOUNT_CONTENT content, @Nullable Object layoutData) {
    bind((List) attachDetachFunctions(), context, content, this, layoutData);
  }

  /** Unbind all attachDetach extension functions. */
  void detachExtensions(Context context, MOUNT_CONTENT content, @Nullable Object layoutData) {
    unbind((List) attachDetachFunctions(), context, content, this, layoutData);
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
      @Nullable Object newLayoutData) {
    final List<Binder> tmpAttachDetachExtensionsToUpdate = sTmpAttachDetachExtensionsToUpdate;
    final List<Binder> tmpMountUnmountExtensionsToUpdate = sTmpMountUnmountExtensionsToUpdate;

    // 1. Fill in the extensions to update.
    shouldUpdate(
        attachDetachFunctions(),
        tmpAttachDetachExtensionsToUpdate,
        currentRenderUnit,
        this,
        currentLayoutData,
        newLayoutData);
    shouldUpdate(
        mountUnmountFunctions(),
        tmpMountUnmountExtensionsToUpdate,
        currentRenderUnit,
        this,
        currentLayoutData,
        newLayoutData);

    // 2. unbind all attach binders which should update.
    unbind(
        tmpAttachDetachExtensionsToUpdate, context, content, currentRenderUnit, currentLayoutData);

    // 3. unbind all mount binders which should update.
    unbind(
        tmpMountUnmountExtensionsToUpdate, context, content, currentRenderUnit, currentLayoutData);

    // 4. rebind all mount binder which did update.
    bind(tmpMountUnmountExtensionsToUpdate, context, content, this, newLayoutData);

    // 5. rebind all attach binder which did update.
    bind(tmpAttachDetachExtensionsToUpdate, context, content, this, newLayoutData);

    // 6. Clear auxiliary data structures.
    tmpAttachDetachExtensionsToUpdate.clear();
    tmpMountUnmountExtensionsToUpdate.clear();
  }

  private static <MOUNT_CONTENT> void bind(
      @Nullable List<Binder> extensions,
      Context context,
      MOUNT_CONTENT content,
      RenderUnit<MOUNT_CONTENT> renderUnit,
      @Nullable Object layoutData) {
    if (extensions == null) {
      return;
    }

    for (Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT> binder : extensions) {
      binder.bind(context, content, renderUnit, layoutData);
    }
  }

  private static <MOUNT_CONTENT> void unbind(
      @Nullable List<Binder> extensions,
      Context context,
      MOUNT_CONTENT content,
      RenderUnit<MOUNT_CONTENT> renderUnit,
      @Nullable Object layoutData) {
    if (extensions == null) {
      return;
    }

    for (int i = extensions.size() - 1; i >= 0; i--) {
      final Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT> binder = extensions.get(i);
      binder.unbind(context, content, renderUnit, layoutData);
    }
  }

  private static <MOUNT_CONTENT> void shouldUpdate(
      @Nullable List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>> extensions,
      List<Binder> extensionsToUpdate,
      RenderUnit<MOUNT_CONTENT> currentRenderUnit,
      RenderUnit<MOUNT_CONTENT> newRenderUnit,
      @Nullable Object currentLayoutData,
      @Nullable Object newLayoutData) {
    if (extensions == null) {
      return;
    }

    for (Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT> binder : extensions) {
      if (binder.shouldUpdate(currentRenderUnit, newRenderUnit, currentLayoutData, newLayoutData)) {
        extensionsToUpdate.add(binder);
      }
    }
  }

  /**
   * Represents a single bind function. Every bind has an equivalent unbind and a shouldUpdate
   * callback
   */
  public interface Binder<RENDER_UNIT, CONTENT> {
    boolean shouldUpdate(
        final RENDER_UNIT currentValue,
        final RENDER_UNIT newValue,
        final @Nullable Object currentLayoutData,
        final @Nullable Object nextLayoutData);

    void bind(
        final Context context,
        final CONTENT content,
        final RENDER_UNIT renderUnit,
        final @Nullable Object layoutData);

    void unbind(
        final Context context,
        final CONTENT content,
        final RENDER_UNIT renderUnit,
        final @Nullable Object layoutData);
  }
}
