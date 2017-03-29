/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.model;

import java.util.List;

import com.facebook.litho.specmodels.generator.TypeSpecDataHolder;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;

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
   * Generate code required to make the Dependency Injection work.
   */
  TypeSpecDataHolder generate(SpecModel specModel);

  /**
   * Generate the source delegate that should be used for the given {@link SpecModel}
   */
  TypeName getSourceDelegateTypeName(SpecModel specModel);

  /**
   * Generate the method (if any) required to access the source delegate. Return an empty string
   * if the source delegate can be accessed directly. Otherwise, provide the given method,
   * preceded by a dot.
   */
  String getSourceDelegateAccessorMethod(SpecModel specModel);

  /**
   * Generate the constructor that should be used for the given {@link SpecModel}.
   */
  MethodSpec generateConstructor(SpecModel specModel);
}
