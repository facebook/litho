/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.generator;

import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.StateParamModel;

import static com.facebook.litho.specmodels.generator.GeneratorConstants.STATE_CONTAINER_FIELD_NAME;

/**
 * Helper class for generating code relating to method params.
 */
public class MethodParamGeneratorUtils {

  private MethodParamGeneratorUtils() {
  }

  static String getImplAccessor(MethodParamModel methodParamModel) {
    if (methodParamModel instanceof StateParamModel) {
      return STATE_CONTAINER_FIELD_NAME + "." + methodParamModel.getName();
    }

    return methodParamModel.getName();
  }
}
