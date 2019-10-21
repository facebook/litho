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
