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

import androidx.annotation.Nullable;
import com.facebook.rendercore.Node.LayoutResult;

/**
 * The base class for all RenderCore Extensions.
 *
 * @param <State> the state the extension operates on.
 */
public class RenderCoreExtension<State> {

  /**
   * The extension can optionally return a {@link LayoutResultVisitor} for every layout pass which
   * will visit every {@link LayoutResult}. The visitor should be functional and immutable.
   *
   * @return a {@link LayoutResultVisitor}.
   */
  public @Nullable LayoutResultVisitor<State> getLayoutVisitor() {
    return null;
  }

  /**
   * The extension can optionally return a {@link MountExtension} which can be used to augment the
   * RenderCore's mounting phase. The {@link #<State>} collected in the latest layout pass will be
   * passed to the extension before mount.
   *
   * @return a {@link MountExtension}.
   */
  public @Nullable MountExtension<State> getMountExtension() {
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
}
