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

import android.graphics.Rect;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;
import com.facebook.rendercore.extensions.ExtensionState;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.OnItemCallbacks;
import java.util.Map;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class DynamicPropsExtension
    extends MountExtension<
        DynamicPropsExtensionInput, DynamicPropsExtension.DynamicPropsExtensionState>
    implements OnItemCallbacks<DynamicPropsExtension.DynamicPropsExtensionState> {

  private static final DynamicPropsExtension sInstance = new DynamicPropsExtension();

  @Override
  protected DynamicPropsExtensionState createState() {
    return new DynamicPropsExtensionState();
  }

  private DynamicPropsExtension() {}

  public static DynamicPropsExtension getInstance() {
    return sInstance;
  }

  @Override
  public void beforeMount(
      ExtensionState<DynamicPropsExtensionState> extensionState,
      @Nullable DynamicPropsExtensionInput dynamicPropsExtensionInput,
      @Nullable Rect localVisibleRect) {
    final DynamicPropsExtensionState state = extensionState.getState();

    state.mPreviousInput = state.mCurrentInput;
    state.mCurrentInput =
        dynamicPropsExtensionInput != null
            ? dynamicPropsExtensionInput.getDynamicValueOutputs()
            : null;
  }

  @Override
  public void onUnmount(ExtensionState<DynamicPropsExtensionState> extensionState) {
    extensionState.releaseAllAcquiredReferences();
    final DynamicPropsExtensionState state = extensionState.getState();
    state.mCurrentInput = null;
    state.mPreviousInput = null;
  }

  @Override
  public void afterMount(ExtensionState<DynamicPropsExtensionState> extensionState) {
    final DynamicPropsExtensionState state = extensionState.getState();
    state.mPreviousInput = null;
  }

  @Override
  public void onBindItem(
      final ExtensionState<DynamicPropsExtensionState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    final DynamicPropsExtensionState state = extensionState.getState();

    @Nullable
    final DynamicValueOutput dynamicValueOutput =
        state.mCurrentInput != null ? state.mCurrentInput.get(renderUnit.getId()) : null;

    if (dynamicValueOutput != null) {
      state.mDynamicPropsManager.onBindComponentToContent(
          dynamicValueOutput.getComponent(),
          dynamicValueOutput.getScopedContext(),
          dynamicValueOutput.getCommonDynamicProps(),
          content);
    }
  }

  @Override
  public void onUnbindItem(
      final ExtensionState<DynamicPropsExtensionState> extensionState,
      final RenderUnit<?> renderUnit,
      final Object content,
      final @Nullable Object layoutData) {
    final DynamicPropsExtensionState state = extensionState.getState();

    @Nullable
    final DynamicValueOutput dynamicValueOutput =
        state.mPreviousInput != null
            ? state.mPreviousInput.get(renderUnit.getId())
            : state.mCurrentInput != null ? state.mCurrentInput.get(renderUnit.getId()) : null;

    if (dynamicValueOutput != null) {
      state.mDynamicPropsManager.onUnbindComponent(
          dynamicValueOutput.getComponent(), dynamicValueOutput.getCommonDynamicProps(), content);
    }
  }

  @Override
  public boolean shouldUpdateItem(
      final ExtensionState<DynamicPropsExtensionState> extensionState,
      final RenderUnit<?> previousRenderUnit,
      final @Nullable Object previousLayoutData,
      final RenderUnit<?> nextRenderUnit,
      final @Nullable Object nextLayoutData) {
    return true;
  }

  @Override
  public void beforeMountItem(
      ExtensionState<DynamicPropsExtensionState> extensionState,
      RenderTreeNode renderTreeNode,
      int index) {}

  @Override
  public void onMountItem(
      ExtensionState<DynamicPropsExtensionState> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

  @Override
  public void onUnmountItem(
      ExtensionState<DynamicPropsExtensionState> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

  @Override
  public void onBoundsAppliedToItem(
      ExtensionState<DynamicPropsExtensionState> extensionState,
      RenderUnit<?> renderUnit,
      Object content,
      @Nullable Object layoutData) {}

  static class DynamicPropsExtensionState {
    private final DynamicPropsManager mDynamicPropsManager = new DynamicPropsManager();

    @Nullable private Map<Long, DynamicValueOutput> mCurrentInput;

    @Nullable private Map<Long, DynamicValueOutput> mPreviousInput;

    @VisibleForTesting
    public DynamicPropsManager getDynamicPropsManager() {
      return mDynamicPropsManager;
    }
  }
}
