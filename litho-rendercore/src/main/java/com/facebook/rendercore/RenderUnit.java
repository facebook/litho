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
 * RenderUnits will be automatically recycled by RenderCore based on their concrete type. A
 * RenderUnit should in most cases declare how it intends to bind data returning Binders from its
 * mountUnmountFunctions callback or from the attachDetachFunctions callback.
 */
public abstract class RenderUnit<MOUNT_CONTENT> implements Copyable {

  public enum RenderType {
    DRAWABLE,
    VIEW,
  }

  private final RenderType mRenderType;
  private final List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>> mBaseMountUnmountFunctions;
  private final List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>> mBaseAttachDetachFunctions;
  private List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>>
      mMountUnmountFunctionsWithExtensions;

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
  public final List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>> mountUnmountFunctions() {
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
  public final List<Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>> attachDetachFunctions() {
    return mBaseAttachDetachFunctions;
  }

  /** @return a unique id identifying this RenderUnit in the tree of Node it is part of. */
  public abstract long getId();

  public Object getRenderContentType() {
    return getClass();
  }

  @Override
  public RenderUnit makeCopy() {
    try {
      RenderUnit renderUnit = (RenderUnit) super.clone();
      if (mMountUnmountFunctionsWithExtensions != null) {
        renderUnit.mMountUnmountFunctionsWithExtensions =
            new ArrayList(mMountUnmountFunctionsWithExtensions);
      }

      return renderUnit;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds an extension function that will be invoked with the other mount/unmount binders. Can be
   * used to add generic functionality (e.g. accessibility) to a RenderUnit.
   */
  public void addMountUnmountExtension(Binder binder) {
    if (mMountUnmountFunctionsWithExtensions == null) {
      mMountUnmountFunctionsWithExtensions = new ArrayList<>(mBaseMountUnmountFunctions.size() + 4);
      mMountUnmountFunctionsWithExtensions.addAll(mBaseMountUnmountFunctions);
    }
    mMountUnmountFunctionsWithExtensions.add(
        (Binder<RenderUnit<MOUNT_CONTENT>, MOUNT_CONTENT>) binder);
  }

  /** removes an extension function previously added with addMountUnmountExtension */
  public void removeMountUnmountExtension(Binder binder) {
    mMountUnmountFunctionsWithExtensions.remove(binder);
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
        final Host host,
        final CONTENT content,
        final RENDER_UNIT renderUnit,
        final @Nullable Object layoutData);

    void unbind(
        final Context context,
        final Host host,
        final CONTENT content,
        final RENDER_UNIT renderUnit,
        final @Nullable Object layoutData);
  }
}
