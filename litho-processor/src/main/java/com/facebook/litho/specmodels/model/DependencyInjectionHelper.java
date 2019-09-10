/*
 * Copyright 2014-present Facebook, Inc.
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

import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import java.util.List;

/**
 * An interface for generating certain methods that are required in order for Dependency Injection
 * to work.
 */
public interface DependencyInjectionHelper {

  /** Validate that the dependency injection for this spec is correctly defined. */
  List<SpecModelValidationError> validate(SpecModel specModel);

  /** Whether a Spec annotation should be moved to the generated Component. */
  boolean isValidGeneratedComponentAnnotation(AnnotationSpec annotation);

  /** Generate the constructor required for Dependency Injection. */
  MethodSpec generateConstructor(SpecModel specModel);

  /** Generate the code needed to inject a new instance of the given SpecModel called 'instance' */
  CodeBlock generateFactoryMethodsComponentInstance(SpecModel specModel);

  /**
   * Generate the necessary code to handle the {@link com.facebook.litho.annotations.InjectProp}
   * annotation. Field with the same name of the parameter should be returned to be usable.
   *
   * @param specModel the model holding the spec being generated
   * @param injectPropParams a list of the models for the injected params
   */
  TypeSpecDataHolder generateInjectedFields(
      SpecModel specModel, ImmutableList<InjectPropModel> injectPropParams);

  /**
   * Generate an accessor for each injected field. This is used to generate matchers for TestSpecs
   * and can be necessary for DI mechanisms which do not allow direct access to the generated
   * fields. For instance, when field values are wrapped in a lazy wrapper.
   */
  MethodSpec generateTestingFieldAccessor(SpecModel specModel, InjectPropModel injectPropModel);

  /**
   * Generate accessor code for each injected field. This is used to generate access from methods
   * such as onCreateLayout.
   *
   * @param specModel the model holding the spc being generated
   * @param methodParamModel the model for the method parameter being accessed
   */
  String generateImplAccessor(SpecModel specModel, MethodParamModel methodParamModel);
}
