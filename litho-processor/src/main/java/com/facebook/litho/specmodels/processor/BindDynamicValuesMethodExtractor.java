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
package com.facebook.litho.specmodels.processor;

import static com.facebook.litho.specmodels.internal.ImmutableList.copyOf;
import static com.facebook.litho.specmodels.model.SpecModelUtils.generateTypeSpec;
import static com.facebook.litho.specmodels.processor.MethodExtractorUtils.getMethodParams;

import com.facebook.litho.annotations.OnBindDynamicValue;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.facebook.litho.specmodels.model.BindDynamicValueMethod;
import com.facebook.litho.specmodels.model.MethodParamModel;
import com.facebook.litho.specmodels.model.SpecMethodModel;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class BindDynamicValuesMethodExtractor {

  /** Get the delegate methods from the given {@link TypeElement}. */
  public static ImmutableList<SpecMethodModel<BindDynamicValueMethod, Void>>
      getOnBindDynamicValuesMethods(TypeElement typeElement, Messager messager) {
    final List<SpecMethodModel<BindDynamicValueMethod, Void>> methods = new ArrayList<>();

    for (Element enclosedElement : typeElement.getEnclosedElements()) {
      if (enclosedElement.getKind() != ElementKind.METHOD) {
        continue;
      }

      final Annotation annotation = enclosedElement.getAnnotation(OnBindDynamicValue.class);
      if (annotation == null) {
        continue;
      }

      final ExecutableElement method = (ExecutableElement) enclosedElement;
      final List<MethodParamModel> methodParams =
          getMethodParams(
              method,
              messager,
              Collections.singletonList(Prop.class),
              Collections.emptyList(),
              Collections.emptyList());

      final SpecMethodModel<BindDynamicValueMethod, Void> methodModel =
          SpecMethodModel.<BindDynamicValueMethod, Void>builder()
              .annotations(ImmutableList.of(annotation))
              .modifiers(copyOf(new ArrayList<>(method.getModifiers())))
              .name(method.getSimpleName())
              .returnTypeSpec(generateTypeSpec(method.getReturnType()))
              .typeVariables(ImmutableList.of())
              .methodParams(copyOf(methodParams))
              .representedObject(method)
              .typeModel(null)
              .build();

      methods.add(methodModel);
    }

    return ImmutableList.copyOf(methods);
  }

  private BindDynamicValuesMethodExtractor() {}
}
