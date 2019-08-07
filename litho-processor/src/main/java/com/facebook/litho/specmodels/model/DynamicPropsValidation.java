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
package com.facebook.litho.specmodels.model;

import com.facebook.litho.annotations.OnBindDynamicValue;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.Prop;
import com.squareup.javapoet.TypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

class DynamicPropsValidation {

  static List<SpecModelValidationError> validate(SpecModel specModel) {
    if (!(specModel instanceof MountSpecModel)) {
      return validateHasNoDynamicProps(specModel);
    }

    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    final TypeName mountType =
        SpecModelUtils.getMethodModelWithAnnotation(specModel, OnCreateMountContent.class)
            .returnType;

    final Map<String, List<SpecMethodModel<BindDynamicValueMethod, Void>>> propToMethodMap =
        new HashMap<>();
    for (SpecMethodModel<BindDynamicValueMethod, Void> methodModel :
        ((MountSpecModel) specModel).getBindDynamicValueMethods()) {
      if (!validateBindDynamicValueMethod(methodModel, mountType, validationErrors)) {
        continue;
      }

      final String propName = methodModel.methodParams.get(1).getName();

      if (!propToMethodMap.containsKey(propName)) {
        propToMethodMap.put(propName, new ArrayList<>());
      }

      propToMethodMap.get(propName).add(methodModel);
    }

    for (PropModel dynamicProp : SpecModelUtils.getDynamicProps(specModel)) {
      final List<SpecMethodModel<BindDynamicValueMethod, Void>> methods =
          propToMethodMap.get(dynamicProp.getName());
      if (methods == null) {
        validationErrors.add(
            new SpecModelValidationError(
                specModel.getRepresentedObject(),
                specModel.getSpecName()
                    + " does not provide @OnBindDynamicValue method for dynamic prop "
                    + dynamicProp.getName()));
      } else if (methods.size() > 1) {
        validationErrors.add(
            new SpecModelValidationError(
                specModel.getRepresentedObject(),
                specModel.getSpecName()
                    + " provides more than one @OnBindDynamicValue method for dynamic prop "
                    + dynamicProp.getName()));
      }
    }

    return validationErrors;
  }

  private static List<SpecModelValidationError> validateHasNoDynamicProps(SpecModel specModel) {
    final List<SpecModelValidationError> validationErrors = new ArrayList<>();

    for (PropModel dynamicProp : SpecModelUtils.getDynamicProps(specModel)) {
      validationErrors.add(
          new SpecModelValidationError(
              dynamicProp.getRepresentedObject(),
              specModel.getSpecName()
                  + " declares dynamic props "
                  + dynamicProp.getName()
                  + " (only MountSpecs support dynamic props)."));
    }

    if (specModel.getRepresentedObject() instanceof TypeElement) {
      final TypeElement spec = (TypeElement) specModel.getRepresentedObject();

      for (Element enclosedElement : spec.getEnclosedElements()) {
        if (enclosedElement.getKind() != ElementKind.METHOD
            || enclosedElement.getAnnotation(OnBindDynamicValue.class) == null) {
          continue;
        }
        validationErrors.add(
            new SpecModelValidationError(
                enclosedElement,
                specModel.getSpecName()
                    + " declares "
                    + OnBindDynamicValue.class
                    + " methods "
                    + specModel.getSpecName()
                    + " (only MountSpecs support dynamic props)."));
      }
    }

    return validationErrors;
  }

  private static boolean validateBindDynamicValueMethod(
      SpecMethodModel<BindDynamicValueMethod, Void> method,
      TypeName mountType,
      List<SpecModelValidationError> out) {
    if (method.methodParams.size() == 2
        && method.methodParams.get(0).getTypeName().equals(mountType)
        && method.methodParams.get(1).getAnnotations().stream()
            .anyMatch(DynamicPropsValidation::isDynamicPropAnnotation)) {
      return true;
    }

    out.add(
        new SpecModelValidationError(
            method.representedObject,
            "A method annotated with @OnBindDynamicValue should have two parameters, the "
                + "first should have the same type as the return type of the method annotated with "
                + "@OnCreateMountContent (i.e. "
                + mountType
                + "), the second - represent a dynamic prop."));

    return false;
  }

  private static boolean isDynamicPropAnnotation(Annotation annotation) {
    return (annotation instanceof Prop) && ((Prop) annotation).dynamic();
  }
}
