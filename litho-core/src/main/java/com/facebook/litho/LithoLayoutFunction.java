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
import com.facebook.rendercore.Node;
import java.util.Map;

public class LithoLayoutFunction implements Node.LayoutFunction {

  static final LithoLayoutFunction INSTANCE = new LithoLayoutFunction();

  private LithoLayoutFunction() {}

  @Override
  public Node.LayoutResult calculateLayout(
      final Context context,
      final Node node,
      final int widthSpec,
      final int heightSpec,
      final Map layoutContexts) {

    DefaultInternalNode result = ((DefaultInternalNode) node);

    // TODO: pass the root diff node when Layout#create() was called
    Layout.measure(result.getContext(), result, widthSpec, heightSpec, null);

    return result;
  }
}
