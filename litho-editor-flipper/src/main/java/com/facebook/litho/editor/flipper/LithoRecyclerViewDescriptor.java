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

package com.facebook.litho.editor.flipper;

import android.view.View;
import android.view.ViewGroup;
import com.facebook.flipper.core.FlipperDynamic;
import com.facebook.flipper.core.FlipperObject;
import com.facebook.flipper.plugins.inspector.Named;
import com.facebook.flipper.plugins.inspector.NodeDescriptor;
import com.facebook.flipper.plugins.inspector.SetDataOperations;
import com.facebook.flipper.plugins.inspector.Touch;
import com.facebook.litho.sections.debug.DebugSection;
import com.facebook.litho.widget.LithoRecyclerView;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class LithoRecyclerViewDescriptor extends NodeDescriptor<LithoRecyclerView> {

  @Override
  public void invalidate(final LithoRecyclerView node) {
    super.invalidate(node);

    new com.facebook.flipper.core.ErrorReportingRunnable(mConnection) {
      @Override
      protected void runOrThrow() throws Exception {
        final Object child;
        child = getChildAt(node, 0);
        if (child instanceof DebugSection) {
          DebugSection childSection = (DebugSection) child;
          final NodeDescriptor descriptor = descriptorForClass(DebugSection.class);
          descriptor.invalidate(childSection);
        }
      }
    }.run();
  }

  @Override
  public void init(final LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    descriptor.init(node);
  }

  @Override
  public String getId(LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getId(node);
  }

  @Override
  public String getName(LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getName(node);
  }

  @Override
  public int getChildCount(LithoRecyclerView node) throws Exception {
    // TODO T39526148 this might not always be true when using the RecyclerBinder manually.
    return 1;
  }

  @Override
  public int getAXChildCount(LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXChildCount(node);
  }

  @Override
  public Object getChildAt(LithoRecyclerView node, int index) throws Exception {
    // TODO T39526148 account for the case above
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    int count = descriptor.getChildCount(node);

    final List<View> childrenViews = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      childrenViews.add((View) descriptor.getChildAt(node, i));
    }

    return DebugSection.getRootInstance(childrenViews);
  }

  @Override
  public Object getAXChildAt(LithoRecyclerView node, int index) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXChildAt(node, index);
  }

  @Override
  public List<Named<FlipperObject>> getData(LithoRecyclerView node) throws Exception {
    final List<Named<FlipperObject>> props = new ArrayList<>();
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    props.addAll(descriptor.getData(node));

    return props;
  }

  @Override
  public List<Named<FlipperObject>> getAXData(LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXData(node);
  }

  @Override
  public void setValue(
      LithoRecyclerView node,
      String[] path,
      @Nullable SetDataOperations.FlipperValueHint kind,
      FlipperDynamic value)
      throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    descriptor.setValue(node, path, kind, value);
  }

  @Override
  public List<Named<String>> getAttributes(LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAttributes(node);
  }

  @Override
  public FlipperObject getExtraInfo(LithoRecyclerView node) {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getExtraInfo(node);
  }

  @Override
  public void hitTest(LithoRecyclerView node, Touch touch) throws Exception {
    touch.continueWithOffset(0, 0, 0);
  }

  @Override
  public void axHitTest(LithoRecyclerView node, Touch touch) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    descriptor.axHitTest(node, touch);
  }

  @Override
  public String getAXName(LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXName(node);
  }

  @Override
  public List<Named<String>> getAXAttributes(LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXAttributes(node);
  }

  @Override
  public void setHighlighted(LithoRecyclerView node, boolean selected, boolean isAlignmentMode)
      throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    descriptor.setHighlighted(node, selected, isAlignmentMode);
  }

  @Override
  public String getDecoration(LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getDecoration(node);
  }

  @Override
  public String getAXDecoration(LithoRecyclerView node) throws Exception {
    final NodeDescriptor descriptor = descriptorForClass(ViewGroup.class);
    return descriptor.getAXDecoration(node);
  }

  @Override
  public boolean matches(String query, LithoRecyclerView node) throws Exception {
    NodeDescriptor descriptor = descriptorForClass(Object.class);
    return descriptor.matches(query, node);
  }
}
