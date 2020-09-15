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

package com.facebook.rendercore.testing;

import com.facebook.rendercore.Copyable;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderState.LayoutContext;

public class SimpleWrapperNode implements Node {

  private final LayoutResult<?> mLayoutResult;

  public SimpleWrapperNode(LayoutResult<?> layoutResult) {
    mLayoutResult = layoutResult;
  }

  @Override
  public LayoutResult<?> calculateLayout(LayoutContext context, int widthSpec, int heightSpec) {
    return mLayoutResult;
  }

  @Override
  public Copyable getLayoutParams() {
    return null;
  }

  @Override
  public Copyable makeCopy() {
    return new SimpleWrapperNode(mLayoutResult);
  }
}
