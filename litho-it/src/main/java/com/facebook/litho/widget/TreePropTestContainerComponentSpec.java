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

package com.facebook.litho.widget;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;

@LayoutSpec
public class TreePropTestContainerComponentSpec {

  public static final String KEY = "TreePropTestContainerComponent";

  public static final String EXPECTED_GLOBAL_KEY =
      "$"
          + KEY
          + ","
          + "$RootComponentWithTreeProps,"
          + "$Row,"
          + "$NestedTreeParentComponent,"
          + "$NestedTreeComponent,"
          + "$Row,"
          + "$NestedTreeChildComponent";

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return RootComponentWithTreeProps.create(c).key("RootComponentWithTreeProps").build();
  }

  public static TreePropTestContainerComponent create(ComponentContext c) {
    return TreePropTestContainerComponent.create(c).key(KEY).build();
  }
}
