// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.specmodels.model;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * An interface for generating certain methods that are required in order for Dependency
 * Injection to work.
 */
public interface DependencyInjectionHelper {

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
