/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;
import com.facebook.litho.specmodels.internal.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import java.util.List;

/**
 * An interface for generating certain methods that are required in order for Dependency
 * Injection to work.
 */
public interface DependencyInjectionHelper {

  /**
   * Validate that the dependency injection for this spec is correctly defined.
   */
  List<SpecModelValidationError> validate(SpecModel specModel);

  /**
   * Whether a Spec annotation should be moved to the generated Component.
   */
  boolean isValidGeneratedComponentAnnotation(AnnotationSpec annotation);

  /** Generate source delegate required for Dependency Injection. */
  TypeSpecDataHolder generateSourceDelegate(SpecModel specModel);

  /** Generate the constructor required for Dependency Injection. */
  MethodSpec generateConstructor(SpecModel specModel);

  /** Generate the code needed to inject a new instance of the given SpecModel called 'instance' */
  CodeBlock generateFactoryMethodsComponentInstance(SpecModel specModel);

  /**
   * Generate the necessary code to handle the {@link com.facebook.litho.annotations.InjectProp}
   * annotation. Field with the same name of the parameter should be returned to be usable.
   *
   * @param injectPropParams
   */
  TypeSpecDataHolder generateInjectedFields(ImmutableList<InjectPropModel> injectPropParams);

  /**
   * Generate an accessor for each injected field. This is used to generate matchers for TestSpecs
   * and can be necessary for DI mechanisms which do not allow direct access to the generated
   * fields. For instance, when field values are wrapped in a lazy wrapper.
   */
  MethodSpec generateTestingFieldAccessor(InjectPropModel injectPropModel);

  /** True if the Spec has to be injected. */
  boolean hasSpecInjection();

  /**
   * False if the Component.create() method should be static. This depends on Dependency Injection.
   */
  boolean hasDIComponentCreation();
}
