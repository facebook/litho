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

package com.facebook.litho.specmodels.processor;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

class ParamNameExtractor {

  static List<String> getNames(ExecutableElement method) {
    final List<? extends VariableElement> params = method.getParameters();
    final List<String> paramNames = new ArrayList<>(params.size());
    for (int i = 0, size = params.size(); i < size; i++) {
      paramNames.add(params.get(i).getSimpleName().toString());
    }
    return paramNames;
  }
}
