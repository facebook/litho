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

package com.facebook.litho.specmodels.model;

/** Utility methods for {@link SpecMethodModel}s. */
public final class SpecMethodModelUtils {

  public static boolean hasLazyStateParams(SpecMethodModel<?, ?> specMethodModel) {
    for (MethodParamModel stateParamModel : specMethodModel.methodParams) {
      if (MethodParamModelUtils.isLazyStateParam(stateParamModel)) {
        return true;
      }
    }

    return false;
  }

  private SpecMethodModelUtils() {
    throw new AssertionError("No instances.");
  }
}
