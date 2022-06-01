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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.RenderUnit.Binder;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents the rendering primitive. Every {@link Mountable} must define what content it
 * creates, and its type. It should also implement a mechanism to measure itself given arbitrary
 * width and height specs. A {@link Mountable} can also specify a collection of Binders to set and
 * unset properties on the content.
 *
 * <p>This abstract class is abstraction of {@link RenderUnit}.
 *
 * <ul>
 *   <li>A {@link Mountable} must only create one type of content.
 *   <li>A {@link Mountable} must be immutable.
 *   <li>Content properties must be unset otherwise the content will not match expected behaviour
 *       when they are reused from the content pool.
 * </ul>
 *
 * <b>Tip: Override {@link #isEquivalentTo(Mountable)} to improve performance.
 *
 * @param <ContentT> The type of the content.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class Mountable<ContentT> implements Equivalence<Mountable<?>> {

  private @Nullable List<Binder<?, ContentT>> mBinders = null;
  private @Nullable Controller<ContentT> mController = null;

  /**
   * Specifies if the content type is {@link View} or a {@link Drawable}.
   *
   * <ul>
   *   <li>This must be a constant.
   *   <li>Must not cause side effects.
   *   <li>Can be called from any thread.
   * </ul>
   *
   * @return Returns {@link RenderUnit.RenderType#VIEW} or {@link RenderUnit.RenderType#DRAWABLE}.
   */
  protected abstract RenderUnit.RenderType getRenderType();

  /**
   * Creates new mountable content when called.
   *
   * <ul>
   *   <li>Must not cause side effects.
   *   <li>Called from the main thread.
   * </ul>
   *
   * @param context The Android context.
   * @return A new mountable content.
   */
  protected abstract ContentT createContent(Context context);

  /**
   * Given a {@param widthSpec} and {@param heightSpec} set the width and height this Mountable will
   * require in {@param size}. In addition this method can return any data that is required to set,
   * and unset properties on the content in the binders.
   *
   * <p>If measure is called again in the same layout pass, then {@param previousLayoutData} will be
   * the layout data returned by the previous measure call.
   *
   * <p>As a performance optimisation the framework will skip this method if this Mountable is equal
   * to the previous Mountable, and if the size specs are compatible. In order to do this the
   * framework will check if every field of the Mountable is equal using reflection. Override {@link
   * #isEquivalentTo(Mountable)} to change the implementation.
   *
   * <ul>
   *   <li>Must not cause side effects.
   *   <li>Is guaranteed to be called at least once.
   *   <li>Can be called more that once.
   *   <li>Can be called from any thread.
   * </ul>
   */
  protected abstract @Nullable Object measure(
      final ComponentContext context,
      final int widthSpec,
      final int heightSpec,
      final Size size,
      final @Nullable Object previousLayoutData);

  /** A list of {@link Binder} to set and unset properties on the content. */
  protected @Nullable List<Binder<?, ContentT>> getBinders() {
    return mBinders;
  }

  /**
   * Registers a {@link Controller} for this {@link Mountable}.
   *
   * @throws RuntimeException if a {@link Controller} was has already been registered for this
   *     {@link Mountable}.
   */
  protected final void registerController(Controller<ContentT> controller) {
    if (mController != null) {
      throw new RuntimeException(
          "A controller is already registered for this Mountable. Mountable.registerContoller() should be called at most once.");
    }
    mController = controller;

    if (mBinders == null) {
      mBinders = new ArrayList<>(2);
    }
    mBinders.add(controller);
  }

  /** A {@link Controller} registered for this Mountable. */
  @Nullable
  protected final Controller<ContentT> getController() {
    return mController;
  }

  @Override
  public boolean isEquivalentTo(Mountable<?> other) {
    return EquivalenceUtils.hasEquivalentFields(this, other);
  }

  /**
   * This API informs the framework to fill the content pool for this Mountable ahead of time. The
   * default value is {@code false}, i.e. content is not pre-allocated. Pre-allocation of the
   * content can improve performance in some circumstances where creating the content is expensive.
   *
   * @return {@code true} to preallocate the content, otherwise {@code false}
   */
  protected boolean canPreallocate() {
    return false;
  }

  /** This API informs the framework about the size of the content pool. The default is 3. */
  protected int getPoolSize() {
    return 3;
  }

  /** Creates the content pool the framework should use for this Mountable. */
  protected MountItemsPool.ItemPool<ContentT> onCreateMountContentPool() {
    return new DefaultMountContentPool(
        "<cls>" + getClass().getName() + "</cls>", getPoolSize(), true);
  }
}
