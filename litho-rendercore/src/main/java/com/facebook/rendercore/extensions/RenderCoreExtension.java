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

package com.facebook.rendercore.extensions;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.Node.LayoutResult;
import java.util.Map;
import java.util.Set;

/**
 * The base class for all RenderCore Extensions.
 *
 * @param <State> the state the extension operates on.
 */
public class RenderCoreExtension<State> {

  /** {@link Rect} to get the current visible bounds during the mount phase. */
  private static final Rect sVisibleRect = new Rect();

  /**
   * The extension can optionally return a {@link LayoutResultVisitor} for every layout pass which
   * will visit every {@link LayoutResult}. The visitor should be functional and immutable.
   *
   * @return a {@link LayoutResultVisitor}.
   */
  public @Nullable LayoutResultVisitor<? extends State> getLayoutVisitor() {
    return null;
  }

  /**
   * The extension can optionally return a {@link MountExtension} which can be used to augment the
   * RenderCore's mounting phase. The {@link #<State>} collected in the latest layout pass will be
   * passed to the extension before mount.
   *
   * @return a {@link MountExtension}.
   */
  public @Nullable MountExtension<? extends State> getMountExtension() {
    return null;
  }

  /**
   * Should return a new {@link #<State>} to which the {@link LayoutResultVisitor} can write into.
   *
   * @return A new {@link #<State>} for {@link LayoutResultVisitor} to write into.
   */
  public @Nullable State createState() {
    return null;
  }

  /**
   * Calls {@link MountExtension#beforeMount(Object, Rect)} for each {@link RenderCoreExtension}
   * that has a mount phase.
   */
  public static void beforeMount(
      final Host host, final @Nullable Map<RenderCoreExtension<?>, Object> results) {
    if (results != null) {
      host.getLocalVisibleRect(sVisibleRect);
      for (Map.Entry<RenderCoreExtension<?>, Object> entry : results.entrySet()) {
        final Object state = entry.getValue();
        final MountExtension extension = entry.getKey().getMountExtension();
        if (extension != null) {
          extension.beforeMount(state, sVisibleRect);
        }
      }
    }
  }

  /**
   * Calls {@link MountExtension#afterMount()} for each {@link RenderCoreExtension} that has a mount
   * phase.
   */
  public static void afterMount(final @Nullable Map<RenderCoreExtension<?>, Object> results) {
    if (results != null) {
      for (Map.Entry<RenderCoreExtension<?>, Object> entry : results.entrySet()) {
        final MountExtension extension = entry.getKey().getMountExtension();
        if (extension != null) {
          extension.afterMount();
        }
      }
    }
  }

  /** returns {@code false} iff the results have the same {@link RenderCoreExtension}s. */
  public static boolean shouldUpdate(
      final @Nullable Map<RenderCoreExtension<?>, Object> currentResults,
      final @Nullable Map<RenderCoreExtension<?>, Object> nextResults) {

    Set<RenderCoreExtension<?>> current = currentResults != null ? currentResults.keySet() : null;
    Set<RenderCoreExtension<?>> next = nextResults != null ? nextResults.keySet() : null;

    if (current == next) {
      return false;
    }

    if (current == null || next == null) {
      return true;
    }

    return !current.equals(next);
  }
}
