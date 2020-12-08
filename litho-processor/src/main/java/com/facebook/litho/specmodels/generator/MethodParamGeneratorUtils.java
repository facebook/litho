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

package com.facebook.litho.specmodels.generator;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_CONTAINER_ACCESSOR;

import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.StateParamModel;

/** Helper class for generating code relating to method params. */
public class MethodParamGeneratorUtils {

  private MethodParamGeneratorUtils() {}

  static String getImplAccessor(MethodParamModel methodParamModel) {
    if (methodParamModel instanceof StateParamModel) {
      return STATE_CONTAINER_ACCESSOR + "." + methodParamModel.getName();
    }

    return methodParamModel.getName();
  }
}
