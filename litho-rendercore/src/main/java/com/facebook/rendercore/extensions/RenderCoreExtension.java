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

package com.facebook.rendercore.extensions;

import android.graphics.Rect;
import android.util.Pair;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.LayoutResult;
import com.facebook.rendercore.MountDelegate;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.RenderCoreExtensionHost;
import com.facebook.rendercore.RenderCoreSystrace;
import com.facebook.rendercore.Systracer;
import java.util.List;
import java.util.Stack;

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
   * @param host The {@link Host} of the extensions.
   * @param mountDelegate The {@link MountDelegate}.
   * @param results A map of {@link RenderCoreExtension} to their results from the layout phase.
   */
  public static void beforeMount(
      final Host host,
      final @Nullable MountDelegate mountDelegate,
      final @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> results) {
    if (mountDelegate != null && results != null) {
      final Rect visibleRect = new Rect();
      host.getLocalVisibleRect(visibleRect);
      mountDelegate.beforeMount(results, visibleRect);
    }
  }

  /**
   * Calls {@link MountExtension#afterMount(ExtensionState)} for each {@link RenderCoreExtension}
   * that has a mount phase.
   *
   * @param mountDelegate The {@link MountDelegate}.
   */
  public static void afterMount(final @Nullable MountDelegate mountDelegate) {
    if (mountDelegate != null) {
      mountDelegate.afterMount();
    }
  }

  /**
   * Calls {@link VisibleBoundsCallbacks#onVisibleBoundsChanged(ExtensionState, Rect)} for each
   * {@link RenderCoreExtension} that has a mount phase.
   *
   * @param host The {@link Host} of the extensions
   */
  public static void notifyVisibleBoundsChanged(final MountDelegateTarget target, final Host host) {
    MountDelegate delegate = target.getMountDelegate();
    if (delegate != null) {
      final Rect rect = new Rect();
      host.getLocalVisibleRect(rect);
      delegate.notifyVisibleBoundsChanged(rect);
    }
  }

  public static void onRegisterForPremount(
      final MountDelegateTarget target, final @Nullable Long frameTimeMs) {
    MountDelegate delegate = target.getMountDelegate();
    if (delegate != null) {
      delegate.onRegisterForPremount(frameTimeMs);
    }
  }

  public static void onUnregisterForPremount(final MountDelegateTarget target) {
    MountDelegate delegate = target.getMountDelegate();
    if (delegate != null) {
      delegate.onUnregisterForPremount();
    }
  }

  /** returns {@code false} iff the results have the same {@link RenderCoreExtension}s. */
  public static boolean shouldUpdate(
      final @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> current,
      final @Nullable List<Pair<RenderCoreExtension<?, ?>, Object>> next) {

    if (current == next) {
      return false;
    }

    if (current == null || next == null) {
      return true;
    }

    if (current.size() != next.size()) {
      return true;
    }

    for (int i = 0, size = current.size(); i < size; i++) {
      if (!current.get(i).first.equals(next.get(i).first)) {
        return true;
      }
    }

    return false;
  }

  public static void recursivelyNotifyVisibleBoundsChanged(@Nullable Object content) {
    recursivelyNotifyVisibleBoundsChanged(content, null);
  }

  public static void recursivelyNotifyVisibleBoundsChanged(
      final @Nullable Object content, @Nullable Systracer tracer) {
    final Systracer systracer = tracer != null ? tracer : RenderCoreSystrace.getInstance();
    systracer.beginSection("recursivelyNotifyVisibleBoundsChanged");

    if (content != null) {
      final Stack<Object> contentStack = new Stack<>();
      contentStack.add(content);

      while (!contentStack.isEmpty()) {
        final Object currentContent = contentStack.pop();

        if (currentContent instanceof RenderCoreExtensionHost) {
          ((RenderCoreExtensionHost) currentContent).notifyVisibleBoundsChanged();
        } else if (currentContent instanceof ViewGroup) {
          final ViewGroup currentViewGroup = (ViewGroup) currentContent;
          for (int i = currentViewGroup.getChildCount() - 1; i >= 0; i--) {
            contentStack.push(currentViewGroup.getChildAt(i));
          }
        }
      }
    }

    systracer.endSection();
  }
}
