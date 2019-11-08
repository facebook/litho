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

import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.specmodels.internal.ImmutableList;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/** Class for validating that the state models within a {@link SpecModel} are well-formed. */
public class CachedValueValidation {

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final ImmutableList<CachedValueParamModel> cachedValues = specModel.getCachedValues();
    for (int i = 0, size = cachedValues.size(); i < size - 1; i++) {
      for (int j = i + 1; j < size; j++) {
        if (cachedValues.get(i).getName().equals(cachedValues.get(j).getName())) {
          validationErrors.add(
              new SpecModelValidationError(
                  cachedValues.get(i).getRepresentedObject(),
                  "The cached value "
                      + cachedValues.get(i).getName()
                      + " is defined differently in different "
                      + "methods. Ensure that each instance of this cached value is declared in the same "
                      + "way (this means having the same type)."));
        }
      }
    }

    final List<SpecMethodModel<DelegateMethod, Void>> onCalculateCachedValueMethods =
        SpecModelUtils.getMethodModelsWithAnnotation(specModel, OnCalculateCachedValue.class);

    for (CachedValueParamModel cachedValue : cachedValues) {
      if (cachedValue.getTypeName().equals(ClassNames.COMPONENT)) {
        validationErrors.add(
            new SpecModelValidationError(
                cachedValue.getRepresentedObject(),
                "Cached values must not be Components, since Components are stateful. Just create the Component as normal."));
      }

      final SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod =
          getCorrespondingOnCalculateCachedValueMethod(cachedValue, onCalculateCachedValueMethods);
      if (onCalculateCachedValueMethod == null) {
        validationErrors.add(
            new SpecModelValidationError(
                cachedValue.getRepresentedObject(),
                "The cached value must have a corresponding @OnCalculateCachedValue method that "
                    + "has the same name."));
      } else if (!cachedValue
          .getTypeName()
          .box()
          .equals(onCalculateCachedValueMethod.returnType.box())) {
        validationErrors.add(
            new SpecModelValidationError(
                cachedValue.getRepresentedObject(),
                "CachedValue param types and the return type of the corresponding "
                    + "@OnCalculateCachedValue method must be the same."));
      }
    }

    for (SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod :
        onCalculateCachedValueMethods) {
      for (MethodParamModel param : onCalculateCachedValueMethod.methodParams) {
        if (!(MethodParamModelUtils.isComponentContextParam(param))
            && !(param instanceof PropModel)
            && !(param instanceof StateParamModel)
            && !(param instanceof InjectPropModel)) {
          validationErrors.add(
              new SpecModelValidationError(
                  param.getRepresentedObject(),
                  "@OnCalculateCachedValue methods may only take ComponentContext, Props, @InjectProps and State as params."));
        }
      }
    }

    return validationErrors;
  }

  @Nullable
  private static SpecMethodModel<DelegateMethod, Void> getCorrespondingOnCalculateCachedValueMethod(
      CachedValueParamModel cachedValue,
      List<SpecMethodModel<DelegateMethod, Void>> onCalculateCachedValueMethods) {
    for (SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod :
        onCalculateCachedValueMethods) {
      final String cachedValueName = getCachedValueName(onCalculateCachedValueMethod);
      if (cachedValueName != null && cachedValueName.equals(cachedValue.getName())) {
        return onCalculateCachedValueMethod;
      }
    }

    return null;
  }

  @Nullable
  private static String getCachedValueName(
      SpecMethodModel<DelegateMethod, Void> onCalculateCachedValueMethod) {
    for (Annotation annotation : onCalculateCachedValueMethod.annotations) {
      if (annotation instanceof OnCalculateCachedValue) {
        return ((OnCalculateCachedValue) annotation).name();
      }
    }

    return null;
  }
}
