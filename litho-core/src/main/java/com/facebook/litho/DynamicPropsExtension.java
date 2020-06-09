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

package com.facebook.litho;

import android.content.Context;
import android.graphics.Rect;
import com.facebook.rendercore.Host;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.RenderUnit;

public class DynamicPropsExtension
    implements HostListenerExtension<DynamicPropsExtension.DynamicPropsExtensionInput>,
        RenderUnit.Binder<LithoRenderUnit, Object> {

  private final DynamicPropsManager mDynamicPropsManager = new DynamicPropsManager();

  public interface DynamicPropsExtensionInput {
    int getMountableOutputCount();

    RenderTreeNode getMountableOutputAt(int position);
  }

  @Override
  public void beforeMount(DynamicPropsExtensionInput input, Rect localVisibleRect) {
    for (int i = 0, size = input.getMountableOutputCount(); i < size; i++) {
      final RenderTreeNode renderTreeNode = input.getMountableOutputAt(i);
      renderTreeNode.getRenderUnit().addAttachDetachExtension(this);
    }
  }

  @Override
  public void afterMount() {}

  @Override
  public void onVisibleBoundsChanged(Rect localVisibleRect) {}

  @Override
  public void onUnmount() {}

  @Override
  public void onUnbind() {}

  @Override
  public boolean shouldUpdate(
      final LithoRenderUnit current,
      final LithoRenderUnit next,
      final Object currentData,
      final Object nextData) {
    return true;
  }

  @Override
  public void bind(
      final Context context,
      final com.facebook.rendercore.Host host,
      final Object content,
      final LithoRenderUnit unit,
      final Object data) {
    final LayoutOutput output = unit.output;
    mDynamicPropsManager.onBindComponentToContent(output.getComponent(), content);
  }

  @Override
  public void unbind(
      final Context context,
      final Host host,
      final Object content,
      final LithoRenderUnit unit,
      final Object data) {
    final LayoutOutput output = unit.output;
    mDynamicPropsManager.onUnbindComponent(output.getComponent());
  }
}
