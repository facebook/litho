/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho.yoga;

import com.facebook.yoga.YogaConfig;
import com.facebook.yoga.YogaConfigFactory;
import com.facebook.yoga.YogaNode;
import com.facebook.yoga.YogaNodeFactory;

public abstract class LithoYogaFactory {
  public static YogaConfig createYogaConfig() {
    return YogaConfigFactory.create();
  }

  public static YogaNode createYogaNode(YogaConfig config) {
    return YogaNodeFactory.create(config);
  }
}
