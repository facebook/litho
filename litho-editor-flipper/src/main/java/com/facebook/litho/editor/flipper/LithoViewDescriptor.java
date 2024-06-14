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

package com.facebook.litho.editor.flipper;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.ViewGroup;
import com.facebook.flipper.core.FlipperDynamic;
import com.facebook.flipper.core.FlipperObject;
import com.facebook.flipper.plugins.inspector.Named;
import com.facebook.flipper.plugins.inspector.NodeDescriptor;
import com.facebook.flipper.plugins.inspector.SetDataOperations;
import com.facebook.flipper.plugins.inspector.Touch;
import com.facebook.litho.BaseMountingView;
import com.facebook.litho.DebugComponent;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class LithoViewDescriptor extends NodeDescriptor<BaseMountingView> {

  @Override
  public void init(BaseMountingView node) throws Exception {
    node.setOnDirtyMountListener(
        new BaseMountingView.OnDirtyMountListener() {
          @Override
          public void onDirtyMount(BaseMountingView view) {
            invalidate(view);
            invalidateAX(view);
          }
        });
  }

  @Override
  public String getId(BaseMountingView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getId(node);
  }

  @Override
  public String getName(BaseMountingView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getName(node);
  }

  @Override
  public String getAXName(BaseMountingView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXName(node);
  }

  @Override
  public int getChildCount(BaseMountingView node) {
    return DebugComponent.getRootInstance(node) == null ? 0 : 1;
  }

  @Override
  public int getAXChildCount(BaseMountingView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXChildCount(node);
  }

  @Override
  public Object getChildAt(BaseMountingView node, int index) {
    return DebugComponent.getRootInstance(node);
  }

  @Override
  public @Nullable Object getAXChildAt(BaseMountingView node, int index) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getChildAt(node, index);
  }

  @Override
  public List<Named<FlipperObject>> getData(BaseMountingView node) throws Exception {
    final List<Named<FlipperObject>> props = new ArrayList<>();
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    final Rect mountedBounds = node.getPreviousMountBounds();

    props.add(
        0,
        new Named<>(
            "LithoView",
            new FlipperObject.Builder()
                .put(
                    "mountbounds",
                    new FlipperObject.Builder()
                        .put("left", mountedBounds.left)
                        .put("top", mountedBounds.top)
                        .put("right", mountedBounds.right)
                        .put("bottom", mountedBounds.bottom))
                .build()));

    props.addAll(descriptor.getData(node));

    return props;
  }

  @Override
  public List<Named<FlipperObject>> getAXData(BaseMountingView node) throws Exception {
    final List<Named<FlipperObject>> props = new ArrayList<>();
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    props.addAll(descriptor.getAXData(node));
    return props;
  }

  @Override
  public void setValue(
      BaseMountingView node,
      String[] path,
      @Nullable SetDataOperations.FlipperValueHint kind,
      FlipperDynamic value)
      throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    descriptor.setValue(node, path, kind, value);
  }

  @Override
  public List<Named<String>> getAttributes(BaseMountingView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAttributes(node);
  }

  @Override
  public List<Named<String>> getAXAttributes(BaseMountingView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXAttributes(node);
  }

  @Override
  public FlipperObject getExtraInfo(BaseMountingView node) {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getExtraInfo(node);
  }

  @Override
  public void setHighlighted(BaseMountingView node, boolean selected, boolean isAlignmentMode)
      throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    descriptor.setHighlighted(node, selected, isAlignmentMode);
  }

  @Override
  public @Nullable Bitmap getSnapshot(BaseMountingView node, boolean includeChildren)
      throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    if (descriptor == null) {
      return null;
    }
    return descriptor.getSnapshot(node, includeChildren);
  }

  @Override
  public void hitTest(BaseMountingView node, Touch touch) {
    touch.continueWithOffset(0, 0, 0);
  }

  @Override
  public void axHitTest(BaseMountingView node, Touch touch) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    descriptor.axHitTest(node, touch);
  }

  @Override
  public String getDecoration(BaseMountingView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getDecoration(node);
  }

  @Override
  public String getAXDecoration(BaseMountingView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXDecoration(node);
  }

  @Override
  public boolean matches(String query, BaseMountingView node) throws Exception {
    NodeDescriptor descriptor = descriptorForClass(Object.class);
    return descriptor.matches(query, node);
  }
}
