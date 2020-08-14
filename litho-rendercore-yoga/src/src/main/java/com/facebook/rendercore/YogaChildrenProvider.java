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
import com.facebook.rendercore.RenderState.LayoutContext;
import java.util.Collections;
import java.util.List;

/**
 * Interface to be implemented by a Node which uses Yoga's layout engine, and has with children
 * which are Yoga nodes.
 */
public interface YogaChildrenProvider<T extends Node, RenderContext> {
  /** List of Yoga node children, or {@link Collections#EMPTY_LIST}. */
  List<T> getYogaChildren();

  /** @return the RenderUnit that should be rendered by this node. */
  @Nullable
  RenderUnit getRenderUnit(LayoutContext<RenderContext> layoutContext);

  /**
   * @return true if this node implements its Node.calculateLayout() with a custom measurement
   *     logic.
   */
  boolean canMeasure();
}
