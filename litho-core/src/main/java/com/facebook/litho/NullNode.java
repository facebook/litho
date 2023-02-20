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

import androidx.annotation.Nullable;
import com.facebook.yoga.YogaNode;

/**
 * This {@link LithoNode } represents a component that renders to null. This is required to support
 * reconciliation of state, and transitions on a component that conditionally renders to null.
 */
public class NullNode extends LithoNode {

  @Override
  protected @Nullable YogaLayoutProps createYogaNodeWriter() {
    return null;
  }

  @Override
  final LithoLayoutResult createLayoutResult(YogaNode node) {
    throw new UnsupportedOperationException("NullNode must not be used for layout");
  }
}
