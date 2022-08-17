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
import com.facebook.infer.annotation.Nullsafe;
import java.util.List;
import java.util.Map;

/**
 * A {@link Mountable} represents a rendering primitive.
 *
 * <p>Every {@link Mountable} must define what content it creates via the {@link ContentAllocator}
 * implementation, and its type. It must also implement a mechanism to measure itself given
 * arbitrary width and height specs. A {@link Mountable} can also specify a collection of Binders to
 * set and unset properties on the content via RenderUnit's addMountUnmountExtension and
 * addAttachDetachExtension methods.
 *
 * <p>Experimental. Currently for Litho team internal use only.
 *
 * <ul>
 *   <li>A {@link Mountable} must only create one type of content.
 *   <li>A {@link Mountable} must be immutable.
 *   <li>Content properties must be unset otherwise the content will not match expected behaviour
 *       when they are reused from the content pool.
 * </ul>
 *
 * @param <ContentT> The type of the content.
 * @see RenderUnit
 * @see Node
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class Mountable<ContentT> extends RenderUnit<ContentT> implements Node {

  private long mId;
  private boolean mIsIdSet;

  public Mountable(RenderUnit.RenderType renderType) {
    super(renderType);
  }

  public final void setId(long id) {
    if (mIsIdSet) {
      throw new RuntimeException("Id can only be set once for any Mountable");
    }
    mId = id;
    mIsIdSet = true;
  }

  @Override
  public final long getId() {
    return mId;
  }

  @Override
  public final RenderUnit.RenderType getRenderType() {
    return super.getRenderType();
  }

  @Override
  public final LayoutResult calculateLayout(
      RenderState.LayoutContext context, int widthSpec, int heightSpec) {
    final MeasureResult measureResult = measure(context, widthSpec, heightSpec);

    return new MountableLayoutResult(
        this,
        widthSpec,
        heightSpec,
        measureResult.width,
        measureResult.height,
        measureResult.layoutData);
  }

  /**
   * Given a {@param widthSpec} and {@param heightSpec} set the width and height this Mountable will
   * require in {@param size}. In addition this method can return any data that is required to set,
   * and unset properties on the content in the binders.
   *
   * <p>As a performance optimisation the framework will skip this method if this Mountable is equal
   * to the previous Mountable, and if the size specs are compatible. In order to do this the
   * framework will check if every field of the Mountable is equal using reflection.
   *
   * <ul>
   *   <li>Must not cause side effects.
   *   <li>Is guaranteed to be called at least once.
   *   <li>Can be called more that once.
   *   <li>Can be called from any thread.
   * </ul>
   *
   * @return a {@link MeasureResult} with the width, height, and optional layout data.
   */
  protected abstract MeasureResult measure(
      final RenderState.LayoutContext context, final int widthSpec, final int heightSpec);

  /** This method is an override that calls super impl to keep it protected on RenderUnit. */
  @Override
  public final void mountExtensions(Context context, Object contentT, @Nullable Object layoutData) {
    super.mountExtensions(context, (ContentT) contentT, layoutData);
  }

  /** This method is an override that calls super impl to keep it protected on RenderUnit. */
  @Override
  public final void unmountExtensions(
      Context context, Object contentT, @Nullable Object layoutData) {
    super.unmountExtensions(context, (ContentT) contentT, layoutData);
  }

  /** This method is an override that calls super impl to keep it protected on RenderUnit. */
  @Override
  public final void attachExtensions(Context context, Object content, @Nullable Object layoutData) {
    super.attachExtensions(context, (ContentT) content, layoutData);
  }

  /** This method is an override that calls super impl to keep it protected on RenderUnit. */
  @Override
  public final void detachExtensions(Context context, Object content, @Nullable Object layoutData) {
    super.detachExtensions(context, (ContentT) content, layoutData);
  }

  /** This method is an override that calls super impl to keep it protected on RenderUnit. */
  @Override
  public final @Nullable Map<Class<?>, Extension<?, ContentT>>
      getMountUnmountBinderTypeToExtensionMap() {
    return super.getMountUnmountBinderTypeToExtensionMap();
  }

  /** This method is an override that calls super impl to keep it protected on RenderUnit. */
  @Override
  public final @Nullable List<Extension<?, ContentT>> getMountUnmountExtensions() {
    return super.getMountUnmountExtensions();
  }

  /** This method is an override that calls super impl to keep it protected on RenderUnit. */
  @Override
  public final @Nullable Map<Class<?>, Extension<?, ContentT>>
      getAttachDetachBinderTypeToExtensionMap() {
    return super.getAttachDetachBinderTypeToExtensionMap();
  }

  /** This method is an override that calls super impl to keep it protected on RenderUnit. */
  @Override
  public final @Nullable List<Extension<?, ContentT>> getAttachDetachExtensions() {
    return super.getAttachDetachExtensions();
  }
}
