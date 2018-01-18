/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.specmodels.model;

import com.facebook.litho.sections.annotations.OnCreateService;
import com.facebook.litho.specmodels.model.DelegateMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.MethodParamModelFactory;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import com.facebook.litho.specmodels.model.SpecModel;
import com.facebook.litho.specmodels.model.SpecModelUtils;
import com.facebook.litho.specmodels.model.TypeSpec;
import javax.annotation.Nullable;

class SectionSpecModelUtils {

  @Nullable
  static MethodParamModel createServiceParam(SpecModel model) {
    final TypeSpec serviceType = extractServiceParam(model);
    if (serviceType == null) {
      return null;
    }

    return MethodParamModelFactory.createSimpleMethodParamModel(
        serviceType, "_service", model.getRepresentedObject());
  }

  @Nullable
  private static TypeSpec extractServiceParam(SpecModel specModel) {
    final SpecMethodModel<DelegateMethod, Void> onCreateService =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnCreateService.class);
    return onCreateService == null ? null : onCreateService.returnTypeSpec;
  }
}
