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

package com.facebook.rendercore.visibility;

import android.graphics.Rect;
import androidx.annotation.Nullable;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderTreeNode;
import com.facebook.rendercore.extensions.LayoutResultVisitor;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.visibility.VisibilityExtension.Results;
import java.util.LinkedList;
import java.util.List;

public class VisibilityExtension extends RenderCoreExtension<Results> {

  private final Visitor visitor;
  private final VisibilityMountExtension<Results> mountExtension;

  public VisibilityExtension(VisibilityOutput.Factory<?> factory) {
    visitor = new Visitor(factory);
    mountExtension = new VisibilityMountExtension<>();
  }

  @Override
  public LayoutResultVisitor<Results> getLayoutVisitor() {
    return visitor;
  }

  @Override
  public MountExtension<Results> getMountExtension() {
    return mountExtension;
  }

  @Override
  public Results createInput() {
    return new Results();
  }

  public static class Results implements VisibilityExtensionInput {

    private final List<VisibilityOutput> outputs = new LinkedList<>();

    @Override
    public List<VisibilityOutput> getVisibilityOutputs() {
      return outputs;
    }

    @Override
    public boolean isIncrementalVisibilityEnabled() {
      return false;
    }

    @Override
    public @Nullable VisibilityModuleInput getVisibilityModuleInput() {
      return null;
    }

    void addOutput(@Nullable VisibilityOutput output) {
      if (output != null) {
        outputs.add(output);
      }
    }
  }

  public static class Visitor implements LayoutResultVisitor<Results> {

    private final VisibilityOutput.Factory factory;

    public Visitor(VisibilityOutput.Factory factory) {
      this.factory = factory;
    }

    @Override
    public void visit(
        final @Nullable RenderTreeNode parent,
        final Node.LayoutResult<?> layoutResult,
        final Rect bounds,
        final int x,
        final int y,
        final int position,
        final @Nullable Results results) {
      if (results != null) {
        Rect absoluteBounds = new Rect(x, y, x + bounds.width(), y + bounds.height());
        results.addOutput(factory.createVisibilityOutput(layoutResult, absoluteBounds));
      }
    }
  }
}
