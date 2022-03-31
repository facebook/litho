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
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.RenderUnit.Binder;
import java.util.List;

/**
 * This represents the rendering primitive. Every {@link Mountable} must define what content it
 * creates, and its type. It should also implement a mechanism to measure itself given arbitrary
 * width and height specs. A {@link Mountable} can also specify a collection of Binders to set and
 * unset properties on the content.
 *
 * <p>This interface is abstraction of [RenderUnit].
 *
 * <ul>
 *   <li>A {@link Mountable} must only create one type of content.
 *   <li>A {@link Mountable} must be immutable.
 *   <li>Content properties must be unset otherwise the content will not match expected behaviour
 *       when they are reused from the content pool.
 * </ul>
 *
 * <b>Tip: Implement {@link Equivalence} to improve performance.
 *
 * @param <ContentT> The type of the content.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface Mountable<ContentT> {

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
  RenderUnit.RenderType getRenderType();

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
  ContentT createContent(Context context);

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
   * framework will check if every field of the Mountable is equal using reflection. It is highly
   * recommended to implement {@link Equivalence} to avoid using the reflection based equivalence
   * check.
   *
   * <ul>
   *   <li>Must not cause side effects.
   *   <li>Is guaranteed to be called at least once.
   *   <li>Can be called more that once.
   *   <li>Can be called from any thread.
   * </ul>
   */
  @Nullable
  Object measure(
      final Context context,
      final int widthSpec,
      final int heightSpec,
      final Size size,
      final @Nullable Object previousLayoutData);

  /** A list of {@link Binder} to set and unset properties on the content. */
  @Nullable
  List<Binder<?, ContentT>> getBinders();
}
