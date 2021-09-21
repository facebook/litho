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
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.Node.LayoutResult;
import com.facebook.rendercore.RenderCoreExtensionHost;
import java.util.Map;
import java.util.Set;

/**
 * The base class for all RenderCore Extensions.
 *
 * @param <Input> the state the extension operates on.
 */
public class RenderCoreExtension<Input, State> {

  /**
   * The extension can optionally return a {@link LayoutResultVisitor} for every layout pass which
   * will visit every {@link LayoutResult}. The visitor should be functional and immutable.
   *
   * @return a {@link LayoutResultVisitor}.
   */
  public @Nullable LayoutResultVisitor<? extends Input> getLayoutVisitor() {
    return null;
  }

  /**
   * The extension can optionally return a {@link MountExtension} which can be used to augment the
   * RenderCore's mounting phase. The {@link #< Input >} collected in the latest layout pass will be
   * passed to the extension before mount.
   *
   * @return a {@link MountExtension}.
   */
  public @Nullable MountExtension<? extends Input, State> getMountExtension() {
    return null;
  }

  /**
   * Should return a new {@link #< Input >} to which the {@link LayoutResultVisitor} can write into.
   *
   * @return A new {@link #< Input >} for {@link LayoutResultVisitor} to write into.
   */
  public @Nullable Input createInput() {
    return null;
  }

  /**
   * Calls {@link MountExtension#beforeMount(ExtensionState, Object, Rect)} for each {@link
   * RenderCoreExtension} that has a mount phase.
   *
   * @param host The {@link Host} of the extensions
   * @param results A map of {@link RenderCoreExtension} to their results from the layout phase.
   */
  public static void beforeMount(
      final MountDelegateTarget mountDelegateTarget,
      final Host host,
      final @Nullable Map<RenderCoreExtension<?, ?>, Object> results) {
    if (results != null) {
      final Rect rect = new Rect();
      host.getLocalVisibleRect(rect);
      for (Map.Entry<RenderCoreExtension<?, ?>, Object> entry : results.entrySet()) {
        final Object state = entry.getValue();
        final MountExtension extension = entry.getKey().getMountExtension();
        if (extension != null) {
          extension.beforeMount(mountDelegateTarget.getExtensionState(extension), state, rect);
        }
      }
    }
  }

  /**
   * Calls {@link MountExtension#afterMount(ExtensionState)} for each {@link RenderCoreExtension}
   * that has a mount phase.
   *
   * @param results A map of {@link RenderCoreExtension} to their results from the layout phase.
   */
  public static void afterMount(
      final MountDelegateTarget mountDelegateTarget,
      final @Nullable Map<RenderCoreExtension<?, ?>, Object> results) {
    if (results != null) {
      for (Map.Entry<RenderCoreExtension<?, ?>, Object> entry : results.entrySet()) {
        final MountExtension<?, ?> extension = entry.getKey().getMountExtension();
        if (extension != null) {
          extension.afterMount(mountDelegateTarget.getExtensionState(extension));
        }
      }
    }
  }

  /**
   * Calls {@link MountExtension#onVisibleBoundsChanged(ExtensionState, Rect)} for each {@link
   * RenderCoreExtension} that has a mount phase.
   *
   * @param host The {@link Host} of the extensions
   * @param results A map of {@link RenderCoreExtension} to their results from the layout phase.
   */
  public static void notifyVisibleBoundsChanged(
      final MountDelegateTarget mountDelegateTarget,
      final Host host,
      @Nullable final Map<RenderCoreExtension<?, ?>, Object> results) {
    if (results != null) {
      final Rect rect = new Rect();
      host.getLocalVisibleRect(rect);
      for (Map.Entry<RenderCoreExtension<?, ?>, Object> e : results.entrySet()) {
        final MountExtension<?, ?> extension = e.getKey().getMountExtension();
        if (extension != null) {
          final ExtensionState state = mountDelegateTarget.getExtensionState(extension);
          if (state != null) {
            extension.onVisibleBoundsChanged(state, rect);
          }
        }
      }
    }
  }

  /** returns {@code false} iff the results have the same {@link RenderCoreExtension}s. */
  public static boolean shouldUpdate(
      final @Nullable Map<RenderCoreExtension<?, ?>, Object> currentResults,
      final @Nullable Map<RenderCoreExtension<?, ?>, Object> nextResults) {

    Set<RenderCoreExtension<?, ?>> current =
        currentResults != null ? currentResults.keySet() : null;
    Set<RenderCoreExtension<?, ?>> next = nextResults != null ? nextResults.keySet() : null;

    if (current == next) {
      return false;
    }

    if (current == null || next == null) {
      return true;
    }

    return !current.equals(next);
  }

  public static void recursivelyNotifyVisibleBoundsChanged(final @Nullable Object content) {
    if (content instanceof RenderCoreExtensionHost) {
      final RenderCoreExtensionHost host = (RenderCoreExtensionHost) content;
      host.notifyVisibleBoundsChanged();
    } else if (content instanceof ViewGroup) {
      final ViewGroup parent = (ViewGroup) content;
      for (int i = 0; i < parent.getChildCount(); i++) {
        final View child = parent.getChildAt(i);
        recursivelyNotifyVisibleBoundsChanged(child);
      }
    }
  }
}
