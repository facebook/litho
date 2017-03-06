// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.generator;

import com.facebook.components.specmodels.model.MethodParamModel;
import com.facebook.components.specmodels.model.StateParamModel;

import static com.facebook.components.specmodels.generator.GeneratorConstants.STATE_CONTAINER_FIELD_NAME;

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
