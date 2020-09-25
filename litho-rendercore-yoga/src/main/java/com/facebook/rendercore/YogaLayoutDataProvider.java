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

package com.facebook.rendercore;

import androidx.annotation.Nullable;
import com.facebook.yoga.YogaConfig;
import com.facebook.yoga.YogaNode;
import java.util.List;

public interface YogaLayoutDataProvider<RenderContext> {

  @Nullable
  YogaConfig getYogaConfig();

  boolean nodeCanMeasure(Node node);

  void applyYogaPropsFromNode(
      Node node, RenderState.LayoutContext<RenderContext> context, YogaNode yogaNode);

  void applyYogaPropsFromLayoutParams(
      Node node, RenderState.LayoutContext<RenderContext> context, YogaNode yogaNode);

  @Nullable
  RenderUnit getRenderUnitForNode(
      Node node, RenderState.LayoutContext<RenderContext> layoutContext);

  List<? extends Node> getYogaChildren(Node node);

  @Nullable
  YogaRootLayoutParams getYogaRootLayoutParams(Node root);
}
