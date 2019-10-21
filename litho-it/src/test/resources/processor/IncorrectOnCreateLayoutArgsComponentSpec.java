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

package com.facebook.litho.processor.integration.resources;

import android.content.Context;
import com.facebook.litho.Component;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;

@LayoutSpec
public class IncorrectOnCreateLayoutArgsComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(
      Context context /* error: Expected com.facebook.litho.ComponentContext */) {
    return null;
  }
}
