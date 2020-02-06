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
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A RenderUnit represents a single rendering primitive for RenderCore. Every RenderUnit has to
 * define at least a createContent method to allocate the RenderUnit content (View or Drawable).
 * RenderUnits will be automatically recycled by RenderCore based on their concrete type. A
 * RenderUnit should in most cases declare how it intends to bind data returning Binders from its
 * mountUnmountFunctions callback or from the attachDetachFunctions callback.
 */
public abstract class RenderUnit<T> implements Copyable {

  public enum RenderType {
    DRAWABLE,
    VIEW,
  }

  private final RenderType mRenderType;
  private final List<Binder<RenderUnit<T>, T>> mBaseMountUnmountFunctions;
  private final List<Binder<RenderUnit<T>, T>> mBaseAttachDetachFunctions;
  private List<Binder<RenderUnit<T>, T>> mMountUnmountFunctionsWithExtensions;

  public RenderUnit(RenderType renderType) {
    this(renderType, null, null);
  }

  public RenderUnit(
      RenderType renderType,
      List<? extends Binder<? extends RenderUnit<T>, T>> mountUnmountFunctions) {
    this(renderType, mountUnmountFunctions, null);
  }

  public RenderUnit(
      RenderType renderType,
      List<? extends Binder<? extends RenderUnit<T>, T>> mountUnmountFunctions,
      List<? extends Binder<? extends RenderUnit<T>, T>> attachDetachFunctions) {
    mRenderType = renderType;
    mBaseMountUnmountFunctions = (List<Binder<RenderUnit<T>, T>>) mountUnmountFunctions;
    mBaseAttachDetachFunctions = (List<Binder<RenderUnit<T>, T>>) attachDetachFunctions;
  }

  public RenderType getRenderType() {
    return mRenderType;
  }

  public abstract T createContent(Context c);

  /** @return a list of binding functions that will be invoked during the mount process. */
  @Nullable
  public final List<Binder<RenderUnit<T>, T>> mountUnmountFunctions() {
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
  public final List<Binder<RenderUnit<T>, T>> attachDetachFunctions() {
    return mBaseAttachDetachFunctions;
  }

  /** @return a unique id identifying this RenderUnit in the tree of Node it is part of. */
  public abstract long getId();

  /**
   * RenderUnits that can measure their content should implement this metho. Measure is not
   * guaranteed to be called as certain layout algorithms might size children based on their own
   * constraints rather than the child's measure function.
   */
  public MeasureResult measure(
      Context context, final int widthSpec, final int heightSpec, Map layoutContexts) {

    return new MeasureResult(
        this,
        widthSpec,
        heightSpec,
        View.MeasureSpec.getMode(widthSpec) == View.MeasureSpec.EXACTLY
            ? View.MeasureSpec.getSize(widthSpec)
            : 0,
        View.MeasureSpec.getMode(heightSpec) == View.MeasureSpec.EXACTLY
            ? View.MeasureSpec.getSize(heightSpec)
            : 0);
  }

  /**
   * This callback is invoked when the final size for this RenderUnit has been determined. unlike
   * measure this is guaranteed to be called at the end of the layout process.
   */
  public void onSizeDefined(Context context, Node.LayoutResult layoutResult, Map layoutContexts) {}

  @Override
  public RenderUnit makeCopy() {
    try {
      return (RenderUnit) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds an extension function that will be invoked with the other mount/unmount binders. Can be
   * used to add generic functionality (e.g. accessibility) to a RenderUnit.
   */
  public <B extends Binder<? extends RenderUnit<? extends T>, ? extends T>>
      void addMountUnmountExtension(B binder) {
    if (mMountUnmountFunctionsWithExtensions == null) {
      mMountUnmountFunctionsWithExtensions = new ArrayList<>(mBaseMountUnmountFunctions.size() + 4);
      mMountUnmountFunctionsWithExtensions.addAll(mBaseMountUnmountFunctions);
    }
    mMountUnmountFunctionsWithExtensions.add((Binder<RenderUnit<T>, T>) binder);
  }

  /**
   * Represents a single bind function. Every bind has an equivalent unbind and a shouldUpdate
   * callback
   */
  public interface Binder<RENDER_UNIT, CONTENT> {
    boolean shouldUpdate(
        RENDER_UNIT currentValue,
        RENDER_UNIT newValue,
        Map currentLayoutContexts,
        Map nextLayoutContexts);

    void bind(Context context, CONTENT content, RENDER_UNIT renderUnit, Map layoutContexts);

    void unbind(Context context, CONTENT content, RENDER_UNIT renderUnit, Map layoutContexts);
  }
}
